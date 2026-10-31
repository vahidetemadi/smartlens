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
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;

final class ProjectFiles {
    private ProjectFiles() {
    }

    /**
     * @return true when the file is a regular, non-ignored source file of the project (read access required)
     */
    static boolean isTrackable(Project project, VirtualFile file) {
        if (project.isDisposed() || !file.isValid() || file.isDirectory() || !file.isInLocalFileSystem()) {
            return false;
        }
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        return fileIndex.isInContent(file)
                && !fileIndex.isExcluded(file)
                && !ChangeListManager.getInstance(project).isIgnoredFile(file);
    }
}
