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
package com.vahid.plugin.smartlens.analysis;

import com.vahid.plugin.smartlens.dto.CommitInfoDto;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Decides whether a commit was produced (or co-produced) by an AI agent,
 * based on the author identity and the Co-authored-by trailers.
 */
public final class AgentMatcher {
    private final List<String> patterns;

    public AgentMatcher(List<String> patterns) {
        this.patterns = patterns.stream()
                .map(pattern -> pattern.trim().toLowerCase(Locale.ROOT))
                .filter(pattern -> !pattern.isEmpty())
                .toList();
    }

    public static AgentMatcher fromCommaSeparated(String patterns) {
        return new AgentMatcher(Arrays.asList(patterns.split(",")));
    }

    public boolean isAgentCommit(CommitInfoDto commit) {
        return Stream.concat(Stream.of(commit.authorEmail(), commit.authorName()), commit.coAuthors().stream())
                .anyMatch(this::matches);
    }

    public boolean matches(String identity) {
        if (identity == null || identity.isBlank()) {
            return false;
        }
        String normalized = identity.toLowerCase(Locale.ROOT);
        return patterns.stream().anyMatch(normalized::contains);
    }
}
