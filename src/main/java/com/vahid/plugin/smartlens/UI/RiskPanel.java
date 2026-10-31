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
                report.hotspots().size(), report.analyzedCommits(), developer(report));
    }

    private static String developer(SmartLensReportDto report) {
        List<String> emails = report.developerEmails().stream().sorted().toList();
        if (emails.isEmpty()) {
            return "unknown";
        }
        return emails.size() == 1 ? emails.get(0) : emails.get(0) + " +" + (emails.size() - 1) + " aliases";
    }

    @Override
    protected void open(HotspotDto item) {
        openFile(item.path());
    }
}
