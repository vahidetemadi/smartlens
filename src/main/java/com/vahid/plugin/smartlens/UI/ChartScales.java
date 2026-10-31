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

import java.util.ArrayList;
import java.util.List;

/**
 * Pure scale functions used by the Overview charts.
 */
final class ChartScales {
    private static final int[] NICE_STEPS = {1, 2, 5};

    private ChartScales() {
    }

    /**
     * @return position of {@code value} on a logarithmic axis from 1 to {@code max}, between 0 and 1
     */
    static double logPosition(double value, double max) {
        if (max <= 1 || value <= 1) {
            return 0.0;
        }
        return Math.min(1.0, Math.log(value) / Math.log(max));
    }

    /**
     * Bubble radius whose <em>area</em> is proportional to the value (radius grows with the square root).
     */
    static double bubbleRadius(double value, double max, double minRadius, double maxRadius) {
        if (max <= 0 || value <= 0) {
            return minRadius;
        }
        return Math.max(minRadius, maxRadius * Math.sqrt(Math.min(1.0, value / max)));
    }

    /**
     * @return 1, 2, 5, 10, 20, 50, ... up to {@code max} (always contains 1)
     */
    static List<Integer> logTicks(int max) {
        List<Integer> ticks = new ArrayList<>();
        for (int magnitude = 1; magnitude <= Math.max(1, max); magnitude *= 10) {
            for (int step : NICE_STEPS) {
                int tick = step * magnitude;
                if (tick <= Math.max(1, max)) {
                    ticks.add(tick);
                }
            }
        }
        return ticks;
    }
}
