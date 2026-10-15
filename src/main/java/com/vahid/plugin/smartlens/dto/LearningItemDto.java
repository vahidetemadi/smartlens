/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.dto;

import com.vahid.plugin.smartlens.value.FeatureCategory;

/**
 * A skill used by an AI agent that never appeared in the developer's own commits.
 */
public record LearningItemDto(String feature,
                              FeatureCategory category,
                              String description,
                              int occurrences,
                              String examplePath,
                              String source) {
}
