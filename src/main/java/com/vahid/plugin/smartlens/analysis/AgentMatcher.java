/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
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
