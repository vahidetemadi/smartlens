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
