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

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.vahid.plugin.smartlens.listener.SmartLensReportListener;
import com.vahid.plugin.smartlens.service.SmartLensAnalysisService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class SmartLensToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String TOOL_WINDOW_ID = "SmartLens";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Overview (charts) first; the table tabs keep exact values, sorting and open-on-double-click
        List<ReportView> panels = List.of(new OverviewPanel(project), new LearnPanel(project),
                new UnderstandPanel(project), new RiskPanel(project));
        List<String> titles = List.of("Overview", "Learn", "Understand", "Risk");

        ContentManager contentManager = toolWindow.getContentManager();
        ContentFactory contentFactory = ContentFactory.getInstance();
        for (int i = 0; i < panels.size(); i++) {
            contentManager.addContent(contentFactory.createContent((JComponent) panels.get(i), titles.get(i), false));
        }

        project.getMessageBus().connect(toolWindow.getDisposable()).subscribe(SmartLensReportListener.TOPIC,
                (SmartLensReportListener) report -> panels.forEach(panel -> panel.showReport(report)));

        SmartLensAnalysisService analysisService = SmartLensAnalysisService.getInstance(project);
        analysisService.getLastReport()
                .ifPresentOrElse(report -> panels.forEach(panel -> panel.showReport(report)), analysisService::runInBackground);
    }
}
