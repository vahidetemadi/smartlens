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
