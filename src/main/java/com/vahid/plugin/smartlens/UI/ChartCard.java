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
