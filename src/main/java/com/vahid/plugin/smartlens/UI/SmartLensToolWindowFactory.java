/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.UI;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.vahid.plugin.smartlens.listener.SmartLensReportListener;
import com.vahid.plugin.smartlens.service.SmartLensAnalysisService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SmartLensToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String TOOL_WINDOW_ID = "SmartLens";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        List<ReportTablePanel<?>> panels = List.of(new LearnPanel(project), new UnderstandPanel(project), new RiskPanel(project));
        List<String> titles = List.of("Learn", "Understand", "Risk");

        ContentManager contentManager = toolWindow.getContentManager();
        ContentFactory contentFactory = ContentFactory.getInstance();
        for (int i = 0; i < panels.size(); i++) {
            contentManager.addContent(contentFactory.createContent(panels.get(i), titles.get(i), false));
        }

        project.getMessageBus().connect(toolWindow.getDisposable()).subscribe(SmartLensReportListener.TOPIC,
                (SmartLensReportListener) report -> panels.forEach(panel -> panel.showReport(report)));

        SmartLensAnalysisService analysisService = SmartLensAnalysisService.getInstance(project);
        analysisService.getLastReport()
                .ifPresentOrElse(report -> panels.forEach(panel -> panel.showReport(report)), analysisService::runInBackground);
    }
}
