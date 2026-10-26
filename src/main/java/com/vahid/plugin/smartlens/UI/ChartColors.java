/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.UI;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.vahid.plugin.smartlens.value.ChangeOrigin;

import java.awt.*;

/**
 * Colors of the Overview charts, light and dark steps chosen and validated separately (CVD separation and
 * contrast on the card surfaces). A color always means the same origin in every chart.
 */
final class ChartColors {
    static final JBColor AGENT = new JBColor(0x2a78d6, 0x3987e5);
    static final JBColor MIXED = new JBColor(0xeb6834, 0xd95926);
    static final JBColor DEVELOPER = new JBColor(0x1baf7a, 0x199e70);
    /** Unknown origin, and hotspots without pending changes (context, de-emphasized). */
    static final JBColor NEUTRAL = new JBColor(0xc3c2b7, 0x5a5d63);
    static final JBColor CARD_SURFACE = new JBColor(0xffffff, 0x1e1f22);
    static final JBColor HAIRLINE = new JBColor(0xe1e0d9, 0x393b40);
    static final JBColor AXIS = new JBColor(0xc3c2b7, 0x4e5157);

    private ChartColors() {
    }

    static Color forOrigin(ChangeOrigin origin) {
        return switch (origin) {
            case AGENT -> AGENT;
            case MIXED -> MIXED;
            case DEVELOPER -> DEVELOPER;
            case UNKNOWN -> NEUTRAL;
        };
    }

    static Color ink() {
        return UIUtil.getLabelForeground();
    }

    static Color mutedInk() {
        return UIUtil.getContextHelpForeground();
    }
}
