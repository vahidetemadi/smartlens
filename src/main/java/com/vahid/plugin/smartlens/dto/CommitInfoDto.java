/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.dto;

import java.util.List;

public record CommitInfoDto(String hash,
                            String authorEmail,
                            String authorName,
                            long authorEpochSeconds,
                            List<String> coAuthors,
                            List<FileChangeDto> files) {
    public String shortHash() {
        return hash.length() > 8 ? hash.substring(0, 8) : hash;
    }
}
