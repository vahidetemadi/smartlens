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
