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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBUI;
import com.vahid.plugin.smartlens.dto.HotspotDto;
import com.vahid.plugin.smartlens.value.ChangeOrigin;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Hotspot map: churn (commits, log scale) against the share written by AI; bubble area is the risk score.
 * Bubbles are neutral context unless the file has pending changes, in which case they take the origin's color
 * (emphasis): an agent currently editing a hotspot stands out.
 */
class HotspotMap extends ChartComponent<HotspotDto> {
    static final int MAX_BUBBLES = 40;
    private static final int LABELED_BY_RISK = 3;
    private static final int MIN_RADIUS = 4;
    private static final int MAX_RADIUS = 13;
    private static final java.util.Map<ChangeOrigin, String> SHORT_NAMES = java.util.Map.of(
            ChangeOrigin.AGENT, "AI agent", ChangeOrigin.MIXED, "Mixed", ChangeOrigin.DEVELOPER, "Developer",
            ChangeOrigin.UNKNOWN, "Unknown");
    private static final List<ChangeOrigin> LEGEND =
            List.of(ChangeOrigin.AGENT, ChangeOrigin.MIXED, ChangeOrigin.DEVELOPER);

    private final Function<String, String> fileName;
    private List<HotspotDto> hotspots = List.of();

    HotspotMap(Consumer<HotspotDto> onOpen, Function<String, String> fileName) {
        super(onOpen);
        this.fileName = fileName;
    }

    void setItems(List<HotspotDto> items) {
        hotspots = items.stream().limit(MAX_BUBBLES).toList();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(JBUI.scale(360), JBUI.scale(240));
    }

    @Override
    protected void paintChart(Graphics2D g, int width, int height) {
        if (hotspots.isEmpty()) {
            paintEmpty(g, width, height, "Not enough history to find hotspots");
            return;
        }
        FontMetrics metrics = g.getFontMetrics();
        int left = metrics.stringWidth("100%") + JBUI.scale(8);
        int top = metrics.getHeight() + JBUI.scale(8);
        int bottom = 2 * metrics.getHeight() + JBUI.scale(6);
        int right = JBUI.scale(8);
        Rectangle plot = new Rectangle(left, top, Math.max(1, width - left - right), Math.max(1, height - top - bottom));
        // Data area inset by the largest bubble radius, so no bubble leaves the plot or covers the tick labels
        int inset = JBUI.scale(MAX_RADIUS);
        Rectangle data = new Rectangle(plot.x + inset, plot.y + inset,
                Math.max(1, plot.width - 2 * inset), Math.max(1, plot.height - 2 * inset));

        int maxCommits = hotspots.stream().mapToInt(HotspotDto::commits).max().orElse(1);
        double maxRisk = hotspots.stream().mapToDouble(HotspotDto::risk).max().orElse(1);

        paintAxes(g, plot, data, maxCommits, metrics);
        paintLegend(g, width, metrics);

        // Context first, then emphasized bubbles; larger under smaller so every bubble stays visible
        List<HotspotDto> ordered = new ArrayList<>(hotspots);
        ordered.sort(Comparator.comparing((HotspotDto hotspot) -> hotspot.pendingOrigin() != null)
                .thenComparing(Comparator.comparingDouble(HotspotDto::risk).reversed()));
        List<Rectangle> labels = new ArrayList<>();
        // Emphasized bubbles (pending changes) are labeled first, then the top risks while space allows
        List<HotspotDto> toLabel = new ArrayList<>(hotspots.stream().filter(hotspot -> hotspot.pendingOrigin() != null).toList());
        hotspots.stream().limit(LABELED_BY_RISK).filter(hotspot -> !toLabel.contains(hotspot)).forEach(toLabel::add);

        for (HotspotDto hotspot : ordered) {
            double radius = ChartScales.bubbleRadius(hotspot.risk(), maxRisk, JBUI.scale(MIN_RADIUS), JBUI.scale(MAX_RADIUS));
            double x = data.x + ChartScales.logPosition(hotspot.commits(), maxCommits) * data.width;
            double y = data.y + (1.0 - hotspot.agentShare()) * data.height;
            Ellipse2D bubble = new Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius);
            g.setColor(hotspot.pendingOrigin() == null ? ChartColors.NEUTRAL : ChartColors.forOrigin(hotspot.pendingOrigin()));
            g.fill(bubble);
            // 2px surface ring separates overlapping bubbles
            g.setColor(ChartColors.CARD_SURFACE);
            g.setStroke(new BasicStroke(JBUI.scale(2)));
            g.draw(bubble);
            addHit(bubble, hotspot);
        }

        // Selective direct labels: top risks and every hotspot with pending changes, skipped when they collide
        for (HotspotDto hotspot : toLabel) {
            double radius = ChartScales.bubbleRadius(hotspot.risk(), maxRisk, JBUI.scale(MIN_RADIUS), JBUI.scale(MAX_RADIUS));
            int x = (int) (data.x + ChartScales.logPosition(hotspot.commits(), maxCommits) * data.width);
            int y = (int) (data.y + (1.0 - hotspot.agentShare()) * data.height);
            String text = fileName.apply(hotspot.path());
            int textWidth = metrics.stringWidth(text);
            int textX = x + (int) radius + JBUI.scale(3);
            if (textX + textWidth > plot.x + plot.width) {
                textX = x - (int) radius - JBUI.scale(3) - textWidth;
            }
            Rectangle bounds = new Rectangle(textX, y - metrics.getAscent() / 2 - 1, textWidth, metrics.getHeight());
            if (textX < 0 || labels.stream().anyMatch(bounds::intersects)) {
                continue;
            }
            labels.add(bounds);
            int baseline = y + metrics.getAscent() / 2 - 1;
            // Surface-colored halo keeps the label legible where it crosses other bubbles
            g.setColor(ChartColors.CARD_SURFACE);
            int halo = JBUI.scale(1);
            for (int dx = -halo; dx <= halo; dx++) {
                for (int dy = -halo; dy <= halo; dy++) {
                    g.drawString(text, textX + dx, baseline + dy);
                }
            }
            g.setColor(ChartColors.ink());
            g.drawString(text, textX, baseline);
        }
    }

    private static void paintAxes(Graphics2D g, Rectangle plot, Rectangle data, int maxCommits, FontMetrics metrics) {
        g.setStroke(new BasicStroke(1));
        for (int percent = 0; percent <= 100; percent += 25) {
            int y = data.y + (int) Math.round((1.0 - percent / 100.0) * data.height);
            g.setColor(ChartColors.HAIRLINE);
            g.drawLine(plot.x, y, plot.x + plot.width, y);
            if (percent % 50 == 0) {
                String label = percent + "%";
                g.setColor(ChartColors.mutedInk());
                g.drawString(label, plot.x - metrics.stringWidth(label) - JBUI.scale(5), y + metrics.getAscent() / 2 - 1);
            }
        }
        // Baseline below the inset data area
        g.setColor(ChartColors.AXIS);
        g.drawLine(plot.x, plot.y + plot.height, plot.x + plot.width, plot.y + plot.height);
        int labelY = plot.y + plot.height + metrics.getAscent() + JBUI.scale(4);
        for (int tick : ChartScales.logTicks(maxCommits)) {
            int x = data.x + (int) Math.round(ChartScales.logPosition(tick, maxCommits) * data.width);
            String label = String.valueOf(tick);
            g.setColor(ChartColors.mutedInk());
            g.drawString(label, x - metrics.stringWidth(label) / 2, labelY);
        }
        String xTitle = "Commits (log scale)";
        g.drawString(xTitle, plot.x + (plot.width - metrics.stringWidth(xTitle)) / 2, labelY + metrics.getHeight());
        g.drawString("Written by AI", 0, metrics.getAscent());
    }

    /**
     * Legend of the bubble colors, right-aligned on the first line (wraps to the left when it does not fit).
     */
    private static void paintLegend(Graphics2D g, int width, FontMetrics metrics) {
        int swatch = JBUI.scale(8);
        List<String> texts = new ArrayList<>(List.of("Pending:"));
        List<Color> colors = new ArrayList<>(java.util.Collections.singletonList(null));
        for (ChangeOrigin origin : LEGEND) {
            texts.add(SHORT_NAMES.get(origin));
            colors.add(ChartColors.forOrigin(origin));
        }
        texts.add("None");
        colors.add(ChartColors.NEUTRAL);
        int legendWidth = 0;
        for (int i = 0; i < texts.size(); i++) {
            legendWidth += (colors.get(i) == null ? 0 : swatch + JBUI.scale(4)) + metrics.stringWidth(texts.get(i)) + JBUI.scale(10);
        }
        int minX = metrics.stringWidth("Written by AI") + JBUI.scale(16);
        int x = Math.max(minX, width - legendWidth);
        int y = metrics.getAscent();
        for (int i = 0; i < texts.size(); i++) {
            int itemWidth = (colors.get(i) == null ? 0 : swatch + JBUI.scale(4)) + metrics.stringWidth(texts.get(i));
            if (x + itemWidth > width) {
                x = minX;
                y += metrics.getHeight();
            }
            if (colors.get(i) != null) {
                g.setColor(colors.get(i));
                g.fillOval(x, y - swatch, swatch, swatch);
                x += swatch + JBUI.scale(4);
            }
            g.setColor(colors.get(i) == null ? ChartColors.mutedInk() : ChartColors.ink());
            g.drawString(texts.get(i), x, y);
            x += metrics.stringWidth(texts.get(i)) + JBUI.scale(10);
        }
    }

    @Override
    protected String tooltip(HotspotDto hotspot) {
        return "<html><b>" + StringUtil.escapeXmlEntities(fileName.apply(hotspot.path())) + "</b><br>"
                + String.format("Risk %.1f · %d commits · %d authors<br>Written by AI %d%% · your familiarity %d%%",
                hotspot.risk(), hotspot.commits(), hotspot.authors(),
                Math.round(hotspot.agentShare() * 100), Math.round(hotspot.familiarity() * 100))
                + (hotspot.pendingOrigin() == null ? "" : "<br>Pending changes: " + hotspot.pendingOrigin().getDisplayName())
                + "<br><i>Click to open</i></html>";
    }
}
