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
import com.vahid.plugin.smartlens.dto.FileChangeDto;
import com.vahid.plugin.smartlens.value.ChangeOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ScoringTest {

    @Test
    void test_given_editCounts_when_classified_then_agentOnlyRanksAboveMixed() {
        assertThat(ChangeOrigin.classify(2, 0)).isEqualTo(ChangeOrigin.AGENT);
        assertThat(ChangeOrigin.classify(1, 5)).isEqualTo(ChangeOrigin.MIXED);
        assertThat(ChangeOrigin.classify(0, 3)).isEqualTo(ChangeOrigin.DEVELOPER);
        assertThat(ChangeOrigin.classify(0, 0)).isEqualTo(ChangeOrigin.UNKNOWN);
        assertThat(ChangeOrigin.AGENT.getScore()).isGreaterThan(ChangeOrigin.MIXED.getScore());
        assertThat(ChangeOrigin.MIXED.getScore()).isGreaterThan(ChangeOrigin.DEVELOPER.getScore());
    }

    @Test
    void test_given_sameChange_when_prioritized_then_agentChangeComesBeforeDeveloperChange() {
        double agent = ReviewPrioritizer.priority(ChangeOrigin.AGENT, 20, 5, 0.5);
        double mixed = ReviewPrioritizer.priority(ChangeOrigin.MIXED, 20, 5, 0.5);
        double developer = ReviewPrioritizer.priority(ChangeOrigin.DEVELOPER, 20, 5, 0.5);

        assertThat(agent).isGreaterThan(mixed).isGreaterThan(developer);
        assertThat(ReviewPrioritizer.priority(ChangeOrigin.AGENT, 10_000, 0, 0.0)).isEqualTo(100.0);
    }

    @Test
    void test_given_fileHistory_when_scored_then_agentShareAndUnfamiliarityRaiseRisk() {
        FileHistoryStats stats = new FileHistoryStats("/r/A.java");
        stats.record(commit("a@x"), change(30), true, false);
        stats.record(commit("me@x"), change(10), false, true);

        assertThat(stats.getCommits()).isEqualTo(2);
        assertThat(stats.getAuthors()).isEqualTo(2);
        assertThat(stats.agentShare()).isCloseTo(0.75, within(1e-9));
        assertThat(stats.familiarity()).isCloseTo(0.25, within(1e-9));

        double churn = RiskScorer.churn(2, 2);
        assertThat(churn).isEqualTo(1.0);
        assertThat(RiskScorer.risk(churn, 1.0, 0.0, 5)).isEqualTo(100.0);
        assertThat(RiskScorer.risk(churn, 0.0, 1.0, 1)).isEqualTo(34.0);
        assertThat(RiskScorer.risk(stats, 2)).isBetween(34.0, 100.0);
        assertThat(RiskScorer.churn(0, 10)).isZero();
    }

    @Test
    void test_given_identities_when_matched_then_trailersAndAuthorsAreDetectedCaseInsensitively() {
        AgentMatcher matcher = AgentMatcher.fromCommaSeparated(" Claude , copilot,,");

        assertThat(matcher.isAgentCommit(new CommitInfoDto("h", "me@x", "Me", 0, List.of("Claude Opus <noreply@anthropic.com>"), List.of())))
                .isTrue();
        assertThat(matcher.isAgentCommit(new CommitInfoDto("h", "copilot@github.com", "GitHub", 0, List.of(), List.of())))
                .isTrue();
        assertThat(matcher.isAgentCommit(new CommitInfoDto("h", "me@x", "Me", 0, List.of(), List.of()))).isFalse();
        assertThat(matcher.matches(null)).isFalse();
    }

    private static CommitInfoDto commit(String email) {
        return new CommitInfoDto("h", email, email, 0, List.of(), List.of());
    }

    private static FileChangeDto change(int added) {
        return new FileChangeDto("/r/A.java", java.util.Collections.nCopies(added, "x"), 0);
    }
}
