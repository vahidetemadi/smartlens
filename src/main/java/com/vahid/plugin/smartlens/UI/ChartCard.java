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
 * Rounded card with a title, hosting one chart or stat of the Overview dashboard.
 */
class ChartCard extends JPanel {

    ChartCard(String title, JComponent body) {
        super(new BorderLayout(0, JBUI.scale(6)));
        setOpaque(false);
        setBorder(JBUI.Borders.empty(10, 12));
        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(JBFont.label().asBold());
        add(titleLabel, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = JBUI.scale(12);
            g.setColor(ChartColors.CARD_SURFACE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g.setColor(ChartColors.HAIRLINE);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        } finally {
            g.dispose();
        }
    }
}
