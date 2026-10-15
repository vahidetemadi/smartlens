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
import com.intellij.util.ui.ColumnInfo;
import com.vahid.plugin.smartlens.dto.LearningItemDto;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;

import java.util.List;

/**
 * "Learn" tab: skills the AI agents used that never appear in the developer's own commits.
 */
public class LearnPanel extends ReportTablePanel<LearningItemDto> {

    @SuppressWarnings("unchecked")
    public LearnPanel(Project project) {
        super(project, new ColumnInfo[]{
                TableColumns.<LearningItemDto>text("Skill", LearningItemDto::feature),
                TableColumns.<LearningItemDto>text("Category", item -> item.category().getDisplayName()),
                TableColumns.<LearningItemDto>number("Uses by agent", LearningItemDto::occurrences),
                TableColumns.<LearningItemDto>text("What it is", LearningItemDto::description),
                TableColumns.<LearningItemDto>text("Example", item -> item.source() + " · " + displayPath(project, item.examplePath()))
        }, "No new skills found yet. Let an AI agent work on the project, then refresh.");
    }

    @Override
    protected List<LearningItemDto> rows(SmartLensReportDto report) {
        return report.learningItems();
    }

    @Override
    protected String summary(SmartLensReportDto report) {
        return String.format("%d skills used by AI agents are new to you (%d of %d commits were agent-assisted)",
                report.learningItems().size(), report.agentCommits(), report.analyzedCommits());
    }

    @Override
    protected void open(LearningItemDto item) {
        openFile(item.examplePath());
    }
}
