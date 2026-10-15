/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.analysis;

import com.vahid.plugin.smartlens.value.ChangeOrigin;

/**
 * Orders pending changes so that the developer verifies the most valuable ones first.
 * <pre>
 * size     = min(1, log(1 + added + deleted) / log(1 + 300))
 * priority = 100 * (0.5 * originScore + 0.3 * size + 0.2 * (1 - familiarity))
 * </pre>
 */
public final class ReviewPrioritizer {
    static final double ORIGIN_WEIGHT = 0.5;
    static final double SIZE_WEIGHT = 0.3;
    static final double UNFAMILIARITY_WEIGHT = 0.2;
    static final int SIZE_SATURATION = 300;

    private ReviewPrioritizer() {
    }

    public static double priority(ChangeOrigin origin, int linesAdded, int linesDeleted, double familiarity) {
        double size = Math.min(1.0, Math.log1p(linesAdded + linesDeleted) / Math.log1p(SIZE_SATURATION));
        double priority = ORIGIN_WEIGHT * origin.getScore() + SIZE_WEIGHT * size + UNFAMILIARITY_WEIGHT * (1.0 - familiarity);
        return RiskScorer.round(100.0 * priority);
    }
}
