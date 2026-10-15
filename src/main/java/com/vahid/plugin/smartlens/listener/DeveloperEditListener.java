/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.listener;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.vfs.VirtualFile;
import com.vahid.plugin.smartlens.service.AttributionService;
import org.jetbrains.annotations.NotNull;

/**
 * Attributes edits made through the IDE editor to the developer.
 * Document reloads triggered by an external (agent) change are skipped.
 */
public class DeveloperEditListener implements DocumentListener {

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        Document document = event.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null || !file.isInLocalFileSystem()) {
            return;
        }
        for (Project project : ProjectLocator.getInstance().getProjectsForFile(file)) {
            if (project == null || project.isDisposed()) {
                continue;
            }
            AttributionService attributionService = AttributionService.getInstance(project);
            if (!attributionService.isRecentlyExternallyChanged(file.getPath()) && ProjectFiles.isTrackable(project, file)) {
                attributionService.recordDeveloperEdit(file.getPath());
            }
        }
    }
}
