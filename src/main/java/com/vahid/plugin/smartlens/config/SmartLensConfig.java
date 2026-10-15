/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
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
        historyDepthSpinner = new JBIntSpinner(300, 10, 10_000, 50);
        vcsQuietSecondsSpinner = new JBIntSpinner(5, 0, 120);

        jPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Agent identities (comma separated):", agentPatternsField)
                .addComponentToRightColumn(comment("Matched against commit author and Co-authored-by trailers."))
                .addLabeledComponent("Agent hook log (relative to project):", hookLogPathField)
                .addComponentToRightColumn(comment("JSON lines {\"file\", \"agent\", \"ts\"} appended by agent hooks."))
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
                || historyDepthSpinner.getNumber() != state.historyDepth
                || vcsQuietSecondsSpinner.getNumber() != state.vcsQuietSeconds;
    }

    @Override
    public void apply() {
        SmartLensState state = SmartLensState.getInstance();
        state.agentPatterns = agentPatternsField.getText().trim();
        state.hookLogPath = hookLogPathField.getText().trim();
        state.codeExtensions = codeExtensionsField.getText().trim();
        state.historyDepth = historyDepthSpinner.getNumber();
        state.vcsQuietSeconds = vcsQuietSecondsSpinner.getNumber();
    }

    @Override
    public void reset() {
        SmartLensState state = SmartLensState.getInstance();
        agentPatternsField.setText(state.agentPatterns);
        hookLogPathField.setText(state.hookLogPath);
        codeExtensionsField.setText(state.codeExtensions);
        historyDepthSpinner.setNumber(state.historyDepth);
        vcsQuietSecondsSpinner.setNumber(state.vcsQuietSeconds);
    }

    @Override
    public void disposeUIResources() {
        jPanel = null;
        agentPatternsField = null;
        hookLogPathField = null;
        codeExtensionsField = null;
        historyDepthSpinner = null;
        vcsQuietSecondsSpinner = null;
    }

    private static JBLabel comment(String text) {
        JBLabel label = new JBLabel(text, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
        label.setCopyable(false);
        return label;
    }
}
