/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
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
