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
