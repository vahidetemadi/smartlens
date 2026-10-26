/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.UI;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBUI;
import com.vahid.plugin.smartlens.dto.LearningItemDto;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

/**
 * Magnitude: the skills agents used most that are new to the developer, as horizontal bars (one hue).
 */
class SkillBarChart extends ChartComponent<LearningItemDto> {
    static final int MAX_BARS = 8;

    private List<LearningItemDto> items = List.of();

    SkillBarChart(Consumer<LearningItemDto> onOpen) {
        super(onOpen);
    }

    void setItems(List<LearningItemDto> learningItems) {
        items = learningItems.stream().limit(MAX_BARS).toList();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(JBUI.scale(300), JBUI.scale(20) * MAX_BARS + JBUI.scale(6));
    }

    @Override
    protected void paintChart(Graphics2D g, int width, int height) {
        if (items.isEmpty()) {
            paintEmpty(g, width, height, "No new skills found");
            return;
        }
        FontMetrics metrics = g.getFontMetrics();
        int rowHeight = JBUI.scale(20);
        int barHeight = JBUI.scale(10);
        int labelWidth = (int) Math.min(width * 0.45, items.stream().mapToInt(item -> metrics.stringWidth(item.feature())).max().orElse(0));
        int valueWidth = metrics.stringWidth("0000") + JBUI.scale(6);
        int barX = labelWidth + JBUI.scale(8);
        int barSpace = Math.max(1, width - barX - valueWidth);
        int max = items.stream().mapToInt(LearningItemDto::occurrences).max().orElse(1);
        int radius = JBUI.scale(4);

        int y = JBUI.scale(2);
        // Only whole rows: the list is ranked, so the rows that fit are the most used skills
        int visibleRows = Math.max(1, Math.min(items.size(), (height - y) / rowHeight));
        for (LearningItemDto item : items.subList(0, visibleRows)) {
            int baseline = y + (rowHeight + metrics.getAscent()) / 2 - JBUI.scale(1);
            g.setColor(ChartColors.ink());
            g.drawString(truncate(item.feature(), labelWidth, metrics), 0, baseline);

            double barWidth = Math.max(JBUI.scale(3), (double) barSpace * item.occurrences() / max);
            double barY = y + (rowHeight - barHeight) / 2.0;
            // Rounded data-end on the right, square end on the baseline (left)
            Area bar = new Area(new RoundRectangle2D.Double(barX, barY, barWidth, barHeight, 2 * radius, 2 * radius));
            bar.add(new Area(new Rectangle2D.Double(barX, barY, Math.min(radius, barWidth), barHeight)));
            g.setColor(ChartColors.AGENT);
            g.fill(bar);

            g.setColor(ChartColors.mutedInk());
            g.drawString(String.valueOf(item.occurrences()), (int) (barX + barWidth + JBUI.scale(5)), baseline);
            addHit(new Rectangle2D.Double(0, y, width, rowHeight), item);
            y += rowHeight;
        }
    }

    @Override
    protected String tooltip(LearningItemDto item) {
        return "<html><b>" + StringUtil.escapeXmlEntities(item.feature()) + "</b> (" + item.category().getDisplayName() + ")<br>"
                + StringUtil.escapeXmlEntities(item.description()) + "<br>Used " + item.occurrences() + " times by agents; e.g. "
                + StringUtil.escapeXmlEntities(item.source()) + "<br><i>Click to open the example</i></html>";
    }

    private static String truncate(String text, int width, FontMetrics metrics) {
        if (metrics.stringWidth(text) <= width) {
            return text;
        }
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end) + ellipsis) > width) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }
}
