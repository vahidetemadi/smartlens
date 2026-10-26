/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.UI;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * Headline number of the Overview: title, large value and a short caption.
 */
class StatTile extends ChartCard {
    private final JBLabel valueLabel = new JBLabel("–");
    private final JBLabel captionLabel = new JBLabel(" ");

    StatTile(String title) {
        super(title, new JPanel());
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        valueLabel.setFont(JBFont.h1().asBold());
        captionLabel.setFont(JBFont.small());
        captionLabel.setForeground(ChartColors.mutedInk());
        body.add(valueLabel, BorderLayout.CENTER);
        body.add(captionLabel, BorderLayout.SOUTH);
        remove(1);
        add(body, BorderLayout.CENTER);
    }

    void setValue(String value, String caption) {
        valueLabel.setText(value);
        captionLabel.setText(caption);
    }
}
