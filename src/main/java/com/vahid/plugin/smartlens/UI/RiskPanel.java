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
import com.vahid.plugin.smartlens.dto.HotspotDto;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;

import java.util.List;

/**
 * "Risk" tab: frequently changed files weighted by agent share and the developer's (un)familiarity.
 */
public class RiskPanel extends ReportTablePanel<HotspotDto> {

    @SuppressWarnings("unchecked")
    public RiskPanel(Project project) {
        super(project, new ColumnInfo[]{
                TableColumns.<HotspotDto>score("Risk", HotspotDto::risk),
                TableColumns.<HotspotDto>text("File", item -> displayPath(project, item.path())),
                TableColumns.<HotspotDto>number("Commits", HotspotDto::commits),
                TableColumns.<HotspotDto>number("Authors", HotspotDto::authors),
                TableColumns.<HotspotDto>percent("Written by AI", HotspotDto::agentShare),
                TableColumns.<HotspotDto>percent("Your familiarity", HotspotDto::familiarity),
                TableColumns.<HotspotDto>text("Pending changes",
                        item -> item.pendingOrigin() == null ? "" : item.pendingOrigin().getDisplayName())
        }, "Not enough history to find hotspots.");
    }

    @Override
    protected List<HotspotDto> rows(SmartLensReportDto report) {
        return report.hotspots();
    }

    @Override
    protected String summary(SmartLensReportDto report) {
        return String.format("Top %d hotspots from the last %d commits (developer: %s)",
                report.hotspots().size(), report.analyzedCommits(),
                report.developerEmails().isEmpty() ? "unknown" : String.join(", ", report.developerEmails()));
    }

    @Override
    protected void open(HotspotDto item) {
        openFile(item.path());
    }
}
