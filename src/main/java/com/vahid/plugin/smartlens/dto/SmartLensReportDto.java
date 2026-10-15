/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record SmartLensReportDto(Set<String> developerEmails,
                                 int analyzedCommits,
                                 int agentCommits,
                                 List<LearningItemDto> learningItems,
                                 List<ReviewItemDto> reviewItems,
                                 List<HotspotDto> hotspots,
                                 Instant generatedAt) {
}
