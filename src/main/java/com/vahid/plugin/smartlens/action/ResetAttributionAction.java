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
import com.intellij.openapi.ui.Messages;
import com.vahid.plugin.smartlens.service.SmartLensAnalysisService;
import org.jetbrains.annotations.NotNull;

public class ResetAttributionAction extends SmartLensAction {

    @Override
    protected void perform(@NotNull Project project) {
        int answer = Messages.showYesNoDialog(project,
                "Forget which pending changes were made by AI agents and which by you?",
                "Reset SmartLens Attribution",
                Messages.getQuestionIcon());
        if (answer == Messages.YES) {
            SmartLensAnalysisService.getInstance(project).resetAttribution();
        }
    }
}
