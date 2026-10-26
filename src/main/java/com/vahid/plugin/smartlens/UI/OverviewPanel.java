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
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.util.ui.JBUI;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;

import javax.swing.*;
import java.awt.*;

/**
 * "Overview" tab: a dashboard over the same report as the table tabs (headline numbers, pending changes by
 * origin, the most used new skills and the hotspot map). Charts open the underlying file on click.
 */
public class OverviewPanel extends SimpleToolWindowPanel implements ReportView {
    private final StatTile pendingTile = new StatTile("Pending files");
    private final StatTile agentCommitsTile = new StatTile("Agent-assisted commits");
    private final StatTile skillsTile = new StatTile("New skills to learn");
    private final StatTile hotspotTile = new StatTile("Hotspots under AI edit");
    private final OriginBarChart originChart = new OriginBarChart();
    private final SkillBarChart skillChart;
    private final HotspotMap hotspotMap;

    public OverviewPanel(Project project) {
        super(true, true);
        skillChart = new SkillBarChart(item -> ReportTablePanel.openFile(project, item.examplePath()));
        hotspotMap = new HotspotMap(hotspot -> ReportTablePanel.openFile(project, hotspot.path()),
                path -> path.substring(path.lastIndexOf('/') + 1));

        int gap = JBUI.scale(8);
        JPanel tiles = new JPanel(new GridLayout(1, 4, gap, 0));
        tiles.setOpaque(false);
        tiles.add(pendingTile);
        tiles.add(agentCommitsTile);
        tiles.add(skillsTile);
        tiles.add(hotspotTile);

        JPanel leftColumn = new JPanel(new BorderLayout(0, gap));
        leftColumn.setOpaque(false);
        leftColumn.add(new ChartCard("Pending changes by origin", originChart), BorderLayout.NORTH);
        leftColumn.add(new ChartCard("New skills most used by agents", skillChart), BorderLayout.CENTER);

        JPanel charts = new JPanel(new GridLayout(1, 2, gap, 0));
        charts.setOpaque(false);
        charts.add(leftColumn);
        charts.add(new ChartCard("Hotspot map (bubble size = risk)", hotspotMap));

        JPanel content = new JPanel(new BorderLayout(0, gap));
        content.setBorder(JBUI.Borders.empty(gap));
        content.add(tiles, BorderLayout.NORTH);
        content.add(charts, BorderLayout.CENTER);
        setContent(content);
        setToolbar(ReportTablePanel.createToolbar(this));
    }

    @Override
    public void showReport(SmartLensReportDto report) {
        long agentFiles = report.reviewItems().stream().filter(item -> item.origin().involvesAgent()).count();
        pendingTile.setValue(String.valueOf(report.reviewItems().size()), agentFiles + " touched by AI agents");
        long share = report.analyzedCommits() == 0 ? 0 : Math.round(100.0 * report.agentCommits() / report.analyzedCommits());
        agentCommitsTile.setValue(share + "%", report.agentCommits() + " of " + report.analyzedCommits() + " commits");
        skillsTile.setValue(String.valueOf(report.learningItems().size()), "used by agents, not yet by you");
        long hotspotsUnderAgent = report.hotspots().stream()
                .filter(hotspot -> hotspot.pendingOrigin() != null && hotspot.pendingOrigin().involvesAgent())
                .count();
        hotspotTile.setValue(String.valueOf(hotspotsUnderAgent), "with pending AI agent changes");

        originChart.setItems(report.reviewItems());
        skillChart.setItems(report.learningItems());
        hotspotMap.setItems(report.hotspots());
    }
}
