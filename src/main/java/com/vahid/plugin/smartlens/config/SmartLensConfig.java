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
package com.vahid.plugin.smartlens.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SmartLensConfig implements Configurable {
    private JPanel jPanel;
    private JBTextField agentPatternsField;
    private JBTextField hookLogPathField;
    private JBTextField codeExtensionsField;
    private JBTextField developerAliasesField;
    private JBIntSpinner historyDepthSpinner;
    private JBIntSpinner vcsQuietSecondsSpinner;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "SmartLens";
    }

    @Override
    public @Nullable JComponent createComponent() {
        agentPatternsField = new JBTextField();
        hookLogPathField = new JBTextField();
        codeExtensionsField = new JBTextField();
        developerAliasesField = new JBTextField();
        historyDepthSpinner = new JBIntSpinner(300, 10, 10_000, 50);
        vcsQuietSecondsSpinner = new JBIntSpinner(5, 0, 120);

        jPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Agent identities (comma separated):", agentPatternsField)
                .addComponentToRightColumn(comment("Matched against commit author and Co-authored-by trailers."))
                .addLabeledComponent("Agent hook log (relative to project):", hookLogPathField)
                .addComponentToRightColumn(comment("JSON lines {\"file\", \"agent\", \"ts\"} appended by agent hooks."))
                .addLabeledComponent("Your other Git e-mails (comma separated):", developerAliasesField)
                .addComponentToRightColumn(comment("Besides git config user.email; used to recognize your own commits."))
                .addLabeledComponent("Code file extensions:", codeExtensionsField)
                .addLabeledComponent("Commits to analyze:", historyDepthSpinner)
                .addLabeledComponent("Ignore disk changes after Git operations (seconds):", vcsQuietSecondsSpinner)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        reset();
        return jPanel;
    }

    @Override
    public boolean isModified() {
        SmartLensState state = SmartLensState.getInstance();
        return !agentPatternsField.getText().equals(state.agentPatterns)
                || !hookLogPathField.getText().equals(state.hookLogPath)
                || !codeExtensionsField.getText().equals(state.codeExtensions)
                || !developerAliasesField.getText().equals(state.developerAliases)
                || historyDepthSpinner.getNumber() != state.historyDepth
                || vcsQuietSecondsSpinner.getNumber() != state.vcsQuietSeconds;
    }

    @Override
    public void apply() {
        SmartLensState state = SmartLensState.getInstance();
        state.agentPatterns = agentPatternsField.getText().trim();
        state.hookLogPath = hookLogPathField.getText().trim();
        state.codeExtensions = codeExtensionsField.getText().trim();
        state.developerAliases = developerAliasesField.getText().trim();
        state.historyDepth = historyDepthSpinner.getNumber();
        state.vcsQuietSeconds = vcsQuietSecondsSpinner.getNumber();
    }

    @Override
    public void reset() {
        SmartLensState state = SmartLensState.getInstance();
        agentPatternsField.setText(state.agentPatterns);
        hookLogPathField.setText(state.hookLogPath);
        codeExtensionsField.setText(state.codeExtensions);
        developerAliasesField.setText(state.developerAliases);
        historyDepthSpinner.setNumber(state.historyDepth);
        vcsQuietSecondsSpinner.setNumber(state.vcsQuietSeconds);
    }

    @Override
    public void disposeUIResources() {
        jPanel = null;
        agentPatternsField = null;
        hookLogPathField = null;
        codeExtensionsField = null;
        developerAliasesField = null;
        historyDepthSpinner = null;
        vcsQuietSecondsSpinner = null;
    }

    private static JBLabel comment(String text) {
        JBLabel label = new JBLabel(text, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
        label.setCopyable(false);
        return label;
    }
}
