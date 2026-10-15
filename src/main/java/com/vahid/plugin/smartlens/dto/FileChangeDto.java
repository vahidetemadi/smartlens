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

/**
 * A single file touched by a commit or by the working tree.
 *
 * @param path         absolute, system-independent path of the file
 * @param addedLines   content of the added lines (without the leading '+')
 * @param linesDeleted number of deleted lines
 */
public record FileChangeDto(String path, List<String> addedLines, int linesDeleted) {
    public int linesAdded() {
        return addedLines.size();
    }
}
