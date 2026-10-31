/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vahid.plugin.smartlens;

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
import com.intellij.util.TimeoutUtil;
import com.vahid.plugin.smartlens.config.SmartLensState;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;
import com.vahid.plugin.smartlens.listener.SmartLensReportListener;
import com.vahid.plugin.smartlens.service.SmartLensAnalysisService;
import git4idea.GitVcs;
import git4idea.repo.GitRepositoryManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Base of the tests that need a real IDE project backed by a real Git repository on disk.
 * Subclasses fill the repository in {@link #createRepository()}; the helpers simulate agents (disk writes
 * outside the IDE) and the developer (typing in the editor).
 */
public abstract class SmartLensRepositoryTestCase extends HeavyPlatformTestCase {
    protected Path base;
    protected VirtualFile root;
    private int originalQuietSeconds;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalQuietSeconds = SmartLensState.getInstance().vcsQuietSeconds;
        SmartLensState.getInstance().vcsQuietSeconds = quietSeconds();

        base = Files.createDirectories(Path.of(getProject().getBasePath()));
        createRepository();

        root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(base);
        assertNotNull(root);
        VfsUtil.markDirtyAndRefresh(false, true, true, root);
        PsiTestUtil.addContentRoot(getModule(), root);

        ProjectLevelVcsManagerImpl.getInstanceImpl(getProject()).waitForInitialized();
        ProjectLevelVcsManager.getInstance(getProject())
                .setDirectoryMappings(List.of(new VcsDirectoryMapping(root.getPath(), GitVcs.NAME)));
        waitUntil("Git repository is detected",
                () -> GitRepositoryManager.getInstance(getProject()).getRepositoryForRoot(root) != null);
        // Let the HEAD moves of the setup commits fall out of the Git quiet period
        TimeoutUtil.sleep(quietSeconds() * 1_000L);
        refreshFromDisk();
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

    protected abstract void createRepository() throws Exception;

    /**
     * Git quiet period used during the test; 0 keeps tests fast when no Git operation happens after setup.
     */
    protected int quietSeconds() {
        return 0;
    }

    protected SmartLensReportDto analyzeViaBackgroundTask() {
        VcsDirtyScopeManager.getInstance(getProject()).markEverythingDirty();
        ChangeListManagerImpl.getInstanceImpl(getProject()).waitUntilRefreshed();

        AtomicReference<SmartLensReportDto> published = new AtomicReference<>();
        getProject().getMessageBus().connect(getTestRootDisposable())
                .subscribe(SmartLensReportListener.TOPIC, (SmartLensReportListener) published::set);
        SmartLensAnalysisService.getInstance(getProject()).runInBackground();
        waitUntil("report is published", () -> published.get() != null);
        assertSame(published.get(), SmartLensAnalysisService.getInstance(getProject()).getLastReport().orElseThrow());
        return published.get();
    }

    /**
     * Simulates the developer typing at the end of a file in the IDE editor, then saving it.
     */
    protected void typeAtEnd(String relativePath, String text) {
        VirtualFile file = root.findFileByRelativePath(relativePath);
        assertNotNull(relativePath, file);
        Document document = FileDocumentManager.getInstance().getDocument(file);
        assertNotNull(document);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(document.getTextLength(), text));
        FileDocumentManager.getInstance().saveDocument(document);
    }

    /**
     * Simulates a tool outside the IDE (an AI agent, a formatter, ...) writing a file on disk.
     */
    protected void writeOnDisk(String relativePath, String content) throws IOException {
        Path path = write(relativePath, content);
        // Ensure the IDE sees a newer timestamp than its cached one
        Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis() + 2_000));
    }

    protected void refreshFromDisk() {
        VfsUtil.markDirtyAndRefresh(false, true, true, root);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }

    protected Path write(String relativePath, String content) throws IOException {
        Path path = base.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    protected void git(String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).directory(base.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue("git timed out", process.waitFor(120, TimeUnit.SECONDS));
        assertEquals("git " + command + ": " + output, 0, process.exitValue());
    }

    protected static void waitUntil(String what, BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 120_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting until " + what);
            }
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            TimeoutUtil.sleep(50);
        }
    }
}
