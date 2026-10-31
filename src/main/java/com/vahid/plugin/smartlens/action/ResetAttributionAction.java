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
