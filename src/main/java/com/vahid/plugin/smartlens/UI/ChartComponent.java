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

import com.intellij.ide.ui.UISettings;
import com.intellij.util.ui.JBFont;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base of the Overview charts: anti-aliased painting, a per-mark tooltip and click-to-open on the marks.
 * Subclasses register the shape of every mark they paint with {@link #addHit(Shape, Object)}.
 *
 * @param <T> item behind a mark
 */
abstract class ChartComponent<T> extends JComponent {
    private final List<Hit<T>> hits = new ArrayList<>();
    private final Consumer<T> onOpen;

    private record Hit<T>(Shape shape, T item) {
    }

    protected ChartComponent(Consumer<T> onOpen) {
        this.onOpen = onOpen;
        setOpaque(false);
        // Registers the component with the tooltip manager; the text comes from getToolTipText(MouseEvent)
        setToolTipText("");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                T item = itemAt(event.getPoint());
                if (item != null) {
                    onOpen.accept(item);
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                setCursor(itemAt(event.getPoint()) != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    @Override
    protected final void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            UISettings.setupAntialiasing(g);
            g.setFont(JBFont.small());
            hits.clear();
            paintChart(g, getWidth(), getHeight());
        } finally {
            g.dispose();
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        T item = itemAt(event.getPoint());
        return item == null ? null : tooltip(item);
    }

    protected abstract void paintChart(Graphics2D g, int width, int height);

    protected abstract String tooltip(T item);

    protected void addHit(Shape shape, T item) {
        hits.add(new Hit<>(shape, item));
    }

    protected static void paintEmpty(Graphics2D g, int width, int height, String text) {
        g.setColor(ChartColors.mutedInk());
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (width - metrics.stringWidth(text)) / 2, height / 2);
    }

    private T itemAt(Point point) {
        // Last painted mark wins (it is drawn on top)
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).shape().contains(point)) {
                return hits.get(i).item();
            }
        }
        return null;
    }
}
