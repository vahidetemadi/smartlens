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
package com.vahid.plugin.smartlens.UI;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.actions.diff.ShowDiffAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.ColumnInfo;
import com.vahid.plugin.smartlens.dto.ReviewItemDto;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;

import java.util.List;

/**
 * "Understand" tab: pending changes ranked by review priority; double-click opens the diff.
 */
public class UnderstandPanel extends ReportTablePanel<ReviewItemDto> {

    @SuppressWarnings("unchecked")
    public UnderstandPanel(Project project) {
        super(project, new ColumnInfo[]{
                TableColumns.<ReviewItemDto>score("Priority", ReviewItemDto::priority),
                TableColumns.<ReviewItemDto>text("File", item -> displayPath(project, item.path())),
                TableColumns.<ReviewItemDto>text("Changed by", item -> item.origin().getDisplayName()),
                TableColumns.<ReviewItemDto>number("+", ReviewItemDto::linesAdded),
                TableColumns.<ReviewItemDto>number("−", ReviewItemDto::linesDeleted),
                TableColumns.<ReviewItemDto>percent("Your familiarity", ReviewItemDto::familiarity)
        }, "No pending changes.");
    }

    @Override
    protected List<ReviewItemDto> rows(SmartLensReportDto report) {
        return report.reviewItems();
    }

    @Override
    protected String summary(SmartLensReportDto report) {
        long agentFiles = report.reviewItems().stream().filter(item -> item.origin().involvesAgent()).count();
        return String.format("%d pending files, %d touched by AI agents. Review from the top.",
                report.reviewItems().size(), agentFiles);
    }

    @Override
    protected void open(ReviewItemDto item) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(item.path());
        Change change = file == null ? null : ChangeListManager.getInstance(project).getChange(file);
        if (change != null) {
            ShowDiffAction.showDiffForChange(project, List.of(change));
        } else {
            openFile(item.path());
        }
    }
}
