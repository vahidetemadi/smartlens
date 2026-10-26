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
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import com.intellij.ui.content.Content;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.UIUtil;
import com.vahid.plugin.smartlens.UI.SmartLensToolWindowFactory;
import com.vahid.plugin.smartlens.config.SmartLensConfig;
import com.vahid.plugin.smartlens.dto.HotspotDto;
import com.vahid.plugin.smartlens.dto.LearningItemDto;
import com.vahid.plugin.smartlens.dto.ReviewItemDto;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;
import com.vahid.plugin.smartlens.value.ChangeOrigin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * End-to-end check of the plugin inside a real (headless) IDE: a real Git repository, the listeners registered
 * in plugin.xml, Git4Idea commands, the background analysis, the report topic and the tool window.
 */
public class SmartLensEndToEndTest extends SmartLensRepositoryTestCase {
    private static final String DEV_EMAIL = "dev@example.com";

    @Override
    protected void createRepository() throws Exception {
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
        // Developer edits a file that an agent hook also reports -> mixed; the (not ignored) hook log is no change to review
        typeAtEnd("Hooked.java", "\n// developer edit\n");
        writeOnDisk(".smartlens/agent-edits.jsonl",
                "{\"file\": \"Hooked.java\", \"agent\": \"claude-code\", \"ts\": " + (System.currentTimeMillis() / 1000) + "}\n");
        refreshFromDisk();

        SmartLensReportDto report = analyzeViaBackgroundTask();

        // History: 3 commits, one co-authored by an agent; developer recognized by git user.email
        assertEquals(Set.of(DEV_EMAIL), report.developerEmails());
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
        assertEquals(ChangeOrigin.AGENT, report.reviewItems().get(0).origin());
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
        assertEquals(List.of("Overview", "Learn", "Understand", "Risk"), Arrays.stream(toolWindow.getContentManager().getContents())
                .map(Content::getDisplayName).toList());
        assertEquals(report.learningItems().size(), rowCount(toolWindow, 1));
        assertEquals(report.reviewItems().size(), rowCount(toolWindow, 2));
        assertEquals(report.hotspots().size(), rowCount(toolWindow, 3));
        // The Overview dashboard paints its charts from the same report without errors
        javax.swing.JComponent overview = toolWindow.getContentManager().getContents()[0].getComponent();
        overview.setSize(1000, 480);
        layoutRecursively(overview);
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(1000, 480, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        overview.printAll(graphics);
        graphics.dispose();
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

    private static void layoutRecursively(java.awt.Component component) {
        component.doLayout();
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }

    private static int rowCount(@NotNull ToolWindowHeadlessManagerImpl.MockToolWindow toolWindow, int tab) {
        TableView<?> table = UIUtil.findComponentOfType(toolWindow.getContentManager().getContents()[tab].getComponent(), TableView.class);
        assertNotNull(table);
        return table.getRowCount();
    }
}
