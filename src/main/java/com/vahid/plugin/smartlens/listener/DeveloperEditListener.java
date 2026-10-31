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
