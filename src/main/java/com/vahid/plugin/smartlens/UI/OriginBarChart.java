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

import com.intellij.util.ui.JBUI;
import com.vahid.plugin.smartlens.dto.ReviewItemDto;
import com.vahid.plugin.smartlens.value.ChangeOrigin;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Part-to-whole: pending changed lines by origin as one stacked horizontal bar, with a legend that also carries
 * the values (so identity never relies on color alone).
 */
class OriginBarChart extends ChartComponent<ChangeOrigin> {
    private static final List<ChangeOrigin> ORDER =
            List.of(ChangeOrigin.AGENT, ChangeOrigin.MIXED, ChangeOrigin.DEVELOPER, ChangeOrigin.UNKNOWN);

    private final Map<ChangeOrigin, Integer> lines = new EnumMap<>(ChangeOrigin.class);
    private final Map<ChangeOrigin, Integer> files = new EnumMap<>(ChangeOrigin.class);

    OriginBarChart() {
        super(origin -> {
        });
    }

    void setItems(List<ReviewItemDto> items) {
        lines.clear();
        files.clear();
        for (ReviewItemDto item : items) {
            lines.merge(item.origin(), item.linesAdded() + item.linesDeleted(), Integer::sum);
            files.merge(item.origin(), 1, Integer::sum);
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(JBUI.scale(300), JBUI.scale(58));
    }

    @Override
    protected void paintChart(Graphics2D g, int width, int height) {
        int total = lines.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            paintEmpty(g, width, height, "No pending changes");
            return;
        }
        int barHeight = JBUI.scale(16);
        int gap = JBUI.scale(2);
        int top = JBUI.scale(4);

        // Clip to one rounded bar: only the two outer data-ends are rounded, segments are split by a surface gap
        Shape previousClip = g.getClip();
        g.clip(new RoundRectangle2D.Double(0, top, width, barHeight, JBUI.scale(8), JBUI.scale(8)));
        double x = 0;
        for (ChangeOrigin origin : ORDER) {
            int value = lines.getOrDefault(origin, 0);
            if (value == 0 && !files.containsKey(origin)) {
                continue;
            }
            double segment = Math.max(JBUI.scale(3), (double) width * value / total);
            Rectangle2D rectangle = new Rectangle2D.Double(x, top, Math.min(segment, width - x), barHeight);
            g.setColor(ChartColors.forOrigin(origin));
            g.fill(new Rectangle2D.Double(rectangle.getX(), rectangle.getY(), Math.max(1, rectangle.getWidth() - gap), barHeight));
            addHit(rectangle, origin);
            x += segment;
        }
        g.setClip(previousClip);

        // Legend with values
        FontMetrics metrics = g.getFontMetrics();
        int legendX = 0;
        int legendY = top + barHeight + JBUI.scale(16);
        int swatch = JBUI.scale(8);
        for (ChangeOrigin origin : ORDER) {
            if (!files.containsKey(origin)) {
                continue;
            }
            String text = origin.getDisplayName() + "  " + files.get(origin) + (files.get(origin) == 1 ? " file" : " files")
                    + " · " + lines.getOrDefault(origin, 0) + (lines.getOrDefault(origin, 0) == 1 ? " line" : " lines");
            int itemWidth = swatch + JBUI.scale(5) + metrics.stringWidth(text) + JBUI.scale(14);
            if (legendX > 0 && legendX + itemWidth > width) {
                legendX = 0;
                legendY += metrics.getHeight();
            }
            g.setColor(ChartColors.forOrigin(origin));
            g.fillRoundRect(legendX, legendY - swatch, swatch, swatch, JBUI.scale(3), JBUI.scale(3));
            g.setColor(ChartColors.ink());
            g.drawString(text, legendX + swatch + JBUI.scale(5), legendY);
            legendX += itemWidth;
        }
    }

    @Override
    protected String tooltip(ChangeOrigin origin) {
        return "<html><b>" + origin.getDisplayName() + "</b><br>" + files.getOrDefault(origin, 0) + " files, "
                + lines.getOrDefault(origin, 0) + " changed lines</html>";
    }
}
