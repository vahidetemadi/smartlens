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
import org.jetbrains.annotations.Nullable;

/**
 * A file that changed often in the history and therefore has a higher regression potential.
 *
 * @param pendingOrigin origin of the file's uncommitted changes, null when it has none
 */
public record HotspotDto(String path,
                         int commits,
                         int authors,
                         double agentShare,
                         double familiarity,
                         double risk,
                         @Nullable ChangeOrigin pendingOrigin) {
}
