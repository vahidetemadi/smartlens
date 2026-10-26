/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.UI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ChartScalesTest {

    @Test
    void test_given_commits_when_placedOnLogAxis_then_endsAndMiddleAreCorrect() {
        assertThat(ChartScales.logPosition(1, 100)).isZero();
        assertThat(ChartScales.logPosition(100, 100)).isEqualTo(1.0);
        assertThat(ChartScales.logPosition(10, 100)).isCloseTo(0.5, within(1e-9));
        assertThat(ChartScales.logPosition(500, 100)).isEqualTo(1.0);
        assertThat(ChartScales.logPosition(5, 1)).isZero();
    }

    @Test
    void test_given_risk_when_sizingBubbles_then_areaIsProportionalAndMinimumIsKept() {
        double full = ChartScales.bubbleRadius(100, 100, 4, 12);
        double quarter = ChartScales.bubbleRadius(25, 100, 4, 12);
        assertThat(full).isEqualTo(12.0);
        // A quarter of the value gives a quarter of the area, i.e. half the radius
        assertThat(quarter).isCloseTo(6.0, within(1e-9));
        assertThat(ChartScales.bubbleRadius(1, 100, 4, 12)).isEqualTo(4.0);
        assertThat(ChartScales.bubbleRadius(0, 0, 4, 12)).isEqualTo(4.0);
    }

    @Test
    void test_given_maximum_when_ticksRequested_then_niceLogTicksUpToMaximum() {
        assertThat(ChartScales.logTicks(46)).containsExactly(1, 2, 5, 10, 20);
        assertThat(ChartScales.logTicks(1)).containsExactly(1);
        assertThat(ChartScales.logTicks(100)).containsExactly(1, 2, 5, 10, 20, 50, 100);
    }
}
