/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.UIUtil;
import com.vahid.plugin.smartlens.UI.SmartLensToolWindowFactory;
import com.vahid.plugin.smartlens.config.SmartLensConfig;
import com.vahid.plugin.smartlens.config.SmartLensState;
import com.vahid.plugin.smartlens.dto.HotspotDto;
import com.vahid.plugin.smartlens.dto.LearningItemDto;
import com.vahid.plugin.smartlens.dto.ReviewItemDto;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;
import com.vahid.plugin.smartlens.listener.SmartLensReportListener;
import com.vahid.plugin.smartlens.service.SmartLensAnalysisService;
import com.vahid.plugin.smartlens.value.ChangeOrigin;
import git4idea.GitVcs;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * End-to-end check of the plugin inside a real (headless) IDE: a real Git repository, the listeners registered
 * in plugin.xml, Git4Idea commands, the background analysis, the report topic and the tool window.
 */
public class SmartLensEndToEndTest extends HeavyPlatformTestCase {
    private static final String DEV_EMAIL = "dev@example.com";

    private Path base;
    private VirtualFile root;
    private int originalQuietSeconds;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // The commits made during setup move HEAD; don't let the Git quiet period hide the simulated agent edits
        originalQuietSeconds = SmartLensState.getInstance().vcsQuietSeconds;
        SmartLensState.getInstance().vcsQuietSeconds = 0;

        base = Files.createDirectories(Path.of(getProject().getBasePath()));
        createRepositoryWithHistory();

        root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(base);
        assertNotNull(root);
        VfsUtil.markDirtyAndRefresh(false, true, true, root);
        PsiTestUtil.addContentRoot(getModule(), root);

        ProjectLevelVcsManagerImpl.getInstanceImpl(getProject()).waitForInitialized();
        ProjectLevelVcsManager.getInstance(getProject())
                .setDirectoryMappings(List.of(new VcsDirectoryMapping(root.getPath(), GitVcs.NAME)));
        waitUntil("Git repository is detected",
                () -> GitRepositoryManager.getInstance(getProject()).getRepositoryForRoot(root) != null);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            SmartLensState.getInstance().vcsQuietSeconds = originalQuietSeconds;
            ChangeListManagerImpl.getInstanceImpl(getProject()).waitEverythingDoneInTestMode();
        } catch (Throwable e) {
            addSuppressedException(e);
        } finally {
            super.tearDown();
        }
    }

    public void test_given_agentAndDeveloperEdits_when_analyzed_then_reportAndToolWindowReflectThem() throws Exception {
        // Agent (outside the IDE) rewrites a file whose document is loaded, so the IDE reloads it
        VirtualFile agentFile = root.findChild("Agent.java");
        assertNotNull(FileDocumentManager.getInstance().getDocument(agentFile));
        writeOnDisk("Agent.java", """
                import java.util.concurrent.CompletableFuture;
                public class Agent {
                    record Point(int x, int y) {}
                    sealed interface Shape permits Circle {}
                    record Circle(double r) implements Shape {}
                }
                """);
        // Agent creates a brand-new (untracked) file
        writeOnDisk("NewAgent.java", """
                public class NewAgent {
                    int area(Object o) { return o instanceof String s ? s.length() : 0; }
                }
                """);
        refreshFromDisk();

        // Developer types in the editor
        typeAtEnd("Dev.java", "\n// developer note\n");
        // Developer edits a file that the agent then rewrites -> mixed
        typeAtEnd("Shared.java", "\n// developer tweak\n");
        writeOnDisk("Shared.java", "public class Shared {\n    int v = 2;\n}\n// developer tweak\n// agent tweak\n");
        refreshFromDisk();
        // Developer edits a file that an agent hook also reports -> mixed
        typeAtEnd("Hooked.java", "\n// developer edit\n");
        writeOnDisk(".smartlens/agent-edits.jsonl",
                "{\"file\": \"Hooked.java\", \"agent\": \"claude-code\", \"ts\": " + (System.currentTimeMillis() / 1000) + "}\n");
        refreshFromDisk();

        VcsDirtyScopeManager.getInstance(getProject()).markEverythingDirty();
        ChangeListManagerImpl.getInstanceImpl(getProject()).waitUntilRefreshed();

        SmartLensReportDto report = analyzeViaBackgroundTask();

        // History: 3 commits, one co-authored by an agent; developer recognized by git user.email
        assertEquals(Set(DEV_EMAIL), report.developerEmails());
        assertEquals(3, report.analyzedCommits());
        assertEquals(1, report.agentCommits());

        // Understand: attribution of every pending change
        Map<String, ChangeOrigin> origins = report.reviewItems().stream()
                .collect(Collectors.toMap(item -> base.relativize(Path.of(item.path())).toString(), ReviewItemDto::origin));
        assertEquals(Map.of(
                "Agent.java", ChangeOrigin.AGENT,
                "NewAgent.java", ChangeOrigin.AGENT,
                "Dev.java", ChangeOrigin.DEVELOPER,
                "Shared.java", ChangeOrigin.MIXED,
                "Hooked.java", ChangeOrigin.MIXED), origins);
        ReviewItemDto first = report.reviewItems().get(0);
        assertEquals(ChangeOrigin.AGENT, first.origin());
        assertEquals(ChangeOrigin.DEVELOPER, report.reviewItems().get(report.reviewItems().size() - 1).origin());

        // Learn: agent skills that the developer never used; the developer's own Optional is not reported
        List<String> skills = report.learningItems().stream().map(LearningItemDto::feature).toList();
        assertTrue(skills.toString(), skills.containsAll(List.of(
                "Record", "Sealed type", "Pattern matching for instanceof", "java.util.concurrent.CompletableFuture")));
        assertFalse(skills.toString(), skills.contains("Optional"));
        assertFalse(skills.toString(), skills.contains("java.util.Optional"));

        // Risk: Dev.java changed in two commits, written by the developer only
        HotspotDto devHotspot = report.hotspots().stream()
                .filter(hotspot -> hotspot.path().endsWith("/Dev.java")).findFirst().orElseThrow();
        assertEquals(2, devHotspot.commits());
        assertEquals(1.0, devHotspot.familiarity(), 1e-9);
        assertEquals(0.0, devHotspot.agentShare(), 1e-9);
        assertEquals(ChangeOrigin.DEVELOPER, devHotspot.pendingOrigin());

        // Tool window: the factory registered in plugin.xml builds three tabs and renders the last report
        ToolWindowHeadlessManagerImpl.MockToolWindow toolWindow = new ToolWindowHeadlessManagerImpl.MockToolWindow(getProject());
        new SmartLensToolWindowFactory().createToolWindowContent(getProject(), toolWindow);
        assertEquals(List.of("Learn", "Understand", "Risk"), java.util.Arrays.stream(toolWindow.getContentManager().getContents())
                .map(content -> content.getDisplayName()).toList());
        assertEquals(report.learningItems().size(), rowCount(toolWindow, 0));
        assertEquals(report.reviewItems().size(), rowCount(toolWindow, 1));
        assertEquals(report.hotspots().size(), rowCount(toolWindow, 2));
    }

    public void test_given_pluginXml_when_loaded_then_actionsAndSettingsAreRegistered() {
        ActionManager actionManager = ActionManager.getInstance();
        assertNotNull(actionManager.getAction("com.vahid.plugin.smartlens.action.OpenSmartLensAction"));
        assertNotNull(actionManager.getAction("com.vahid.plugin.smartlens.action.RefreshSmartLensAction"));
        assertNotNull(actionManager.getAction("com.vahid.plugin.smartlens.action.ResetAttributionAction"));
        assertNotNull(actionManager.getAction("com.vahid.plugin.smartlens.group.Toolbar"));

        SmartLensConfig config = new SmartLensConfig();
        assertNotNull(config.createComponent());
        assertFalse(config.isModified());
        config.apply();
        config.disposeUIResources();
    }

    private SmartLensReportDto analyzeViaBackgroundTask() {
        AtomicReference<SmartLensReportDto> published = new AtomicReference<>();
        getProject().getMessageBus().connect(getTestRootDisposable())
                .subscribe(SmartLensReportListener.TOPIC, (SmartLensReportListener) published::set);
        SmartLensAnalysisService.getInstance(getProject()).runInBackground();
        waitUntil("report is published", () -> published.get() != null);
        assertSame(published.get(), SmartLensAnalysisService.getInstance(getProject()).getLastReport().orElseThrow());
        return published.get();
    }

    private void createRepositoryWithHistory() throws Exception {
        git("init", "-q", "-b", "master");
        git("config", "user.email", DEV_EMAIL);
        git("config", "user.name", "Dev");
        git("config", "commit.gpgsign", "false");
        write(".gitignore", ".idea/\n*.iml\n*.ipr\n*.iws\n");
        write("Dev.java", """
                import java.util.Optional;
                public class Dev {
                    Optional<String> name() { return Optional.of("x"); }
                }
                """);
        write("Shared.java", "public class Shared {\n    int v = 1;\n}\n");
        write("Hooked.java", "public class Hooked {\n}\n");
        git("add", "-A");
        git("commit", "-q", "-m", "feat: developer code");

        write("Agent.java", """
                import java.util.concurrent.CompletableFuture;
                public class Agent {
                    record Point(int x, int y) {}
                }
                """);
        git("add", "-A");
        git("commit", "-q", "-m", "feat: agent code", "-m", "Co-Authored-By: Claude <noreply@anthropic.com>");

        write("Dev.java", """
                import java.util.Optional;
                public class Dev {
                    Optional<String> name() { return Optional.of("x"); }
                    int size() { return 1; }
                }
                """);
        git("commit", "-q", "-am", "feat: developer again");
    }

    private void typeAtEnd(String fileName, String text) {
        VirtualFile file = root.findChild(fileName);
        assertNotNull(fileName, file);
        Document document = FileDocumentManager.getInstance().getDocument(file);
        assertNotNull(document);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(document.getTextLength(), text));
        FileDocumentManager.getInstance().saveDocument(document);
    }

    private void writeOnDisk(String relativePath, String content) throws IOException {
        // Ensure the IDE sees a newer timestamp than its cached one
        Path path = base.resolve(relativePath);
        write(relativePath, content);
        Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 2_000));
    }

    private void refreshFromDisk() {
        VfsUtil.markDirtyAndRefresh(false, true, true, root);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }

    private void write(String relativePath, String content) throws IOException {
        Path path = base.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void git(String... args) throws Exception {
        List<String> command = new java.util.ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).directory(base.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue("git timed out", process.waitFor(30, TimeUnit.SECONDS));
        assertEquals("git " + command + ": " + output, 0, process.exitValue());
    }

    private static void waitUntil(String what, BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 60_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting until " + what);
            }
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            com.intellij.util.TimeoutUtil.sleep(50);
        }
    }

    private static int rowCount(@NotNull ToolWindowHeadlessManagerImpl.MockToolWindow toolWindow, int tab) {
        TableView<?> table = UIUtil.findComponentOfType(toolWindow.getContentManager().getContents()[tab].getComponent(), TableView.class);
        assertNotNull(table);
        return table.getRowCount();
    }

    private static java.util.Set<String> Set(String... values) {
        return java.util.Set.of(values);
    }
}
