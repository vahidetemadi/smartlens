/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.study;

/**
 * Ablation of {@link AttributionScenarioStudy} without the Git quiet period.
 */
public class AttributionScenarioNoQuietStudy extends AttributionScenarioStudy {

    @Override
    protected int quietSeconds() {
        return 0;
    }

    @Override
    protected String variant() {
        return "no quiet period";
    }
}
