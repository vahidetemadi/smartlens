/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.listener;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.vahid.plugin.smartlens.service.AttributionService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Attributes files that changed on disk from outside the IDE (a VFS refresh) to an AI agent.
 * Changes made by Git itself (checkout, rebase, merge, ...) are skipped.
 */
public class ExternalChangeListener implements BulkFileListener {
    private static final Set<String> GIT_STATE_FILES =
            Set.of("HEAD", "ORIG_HEAD", "MERGE_HEAD", "REBASE_HEAD", "CHERRY_PICK_HEAD", "REVERT_HEAD");

    private final Project project;

    public ExternalChangeListener(Project project) {
        this.project = project;
    }

    @Override
    public void before(@NotNull List<? extends @NotNull VFileEvent> events) {
        if (project.isDisposed()) {
            return;
        }
        AttributionService attributionService = AttributionService.getInstance(project);
        for (VFileEvent event : events) {
            if (!event.isFromRefresh()) {
                continue;
            }
            if (isGitStateChange(event.getPath())) {
                attributionService.markVcsOperation();
            } else if (event instanceof VFileContentChangeEvent) {
                // The document of this file is about to be reloaded; that is not developer typing
                attributionService.markExternalChange(event.getPath());
            }
        }
    }

    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        if (project.isDisposed()) {
            return;
        }
        AttributionService attributionService = AttributionService.getInstance(project);
        if (attributionService.isInVcsQuietPeriod()) {
            return;
        }
        for (VFileEvent event : events) {
            if (!event.isFromRefresh() || !(event instanceof VFileContentChangeEvent || event instanceof VFileCreateEvent)) {
                continue;
            }
            VirtualFile file = event.getFile();
            if (file != null && ProjectFiles.isTrackable(project, file)) {
                attributionService.recordAgentEdit(file.getPath());
            }
        }
    }

    static boolean isGitStateChange(String path) {
        int gitDir = path.lastIndexOf("/.git/");
        if (gitDir < 0) {
            return false;
        }
        String insideGit = path.substring(gitDir + "/.git/".length());
        return GIT_STATE_FILES.contains(insideGit) || insideGit.startsWith("rebase-merge") || insideGit.startsWith("rebase-apply");
    }
}
