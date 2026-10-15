/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.dto;

import com.vahid.plugin.smartlens.value.ChangeOrigin;

/**
 * A file with pending changes, ranked by how much it deserves to be reviewed.
 */
public record ReviewItemDto(String path,
                            ChangeOrigin origin,
                            int linesAdded,
                            int linesDeleted,
                            double familiarity,
                            double priority) {
}
