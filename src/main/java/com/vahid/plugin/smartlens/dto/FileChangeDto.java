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
