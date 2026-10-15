/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.action;

import com.intellij.openapi.project.Project;
import com.vahid.plugin.smartlens.service.SmartLensAnalysisService;
import org.jetbrains.annotations.NotNull;

public class RefreshSmartLensAction extends SmartLensAction {

    @Override
    protected boolean isEnabled(@NotNull Project project) {
        return !SmartLensAnalysisService.getInstance(project).isRunning();
    }

    @Override
    protected void perform(@NotNull Project project) {
        SmartLensAnalysisService.getInstance(project).runInBackground();
    }
}
