/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.analysis;

/**
 * Regression-risk score of a file, inspired by hotspot analysis (change frequency as the main driver),
 * weighted by how much of it was written by agents and how little the developer knows it.
 * <pre>
 * churn    = log(1 + commits) / log(1 + maxCommits)
 * exposure = 0.4 + 0.3 * agentShare + 0.3 * (1 - familiarity)
 * authors  = 0.85 + 0.15 * min(1, (authorCount - 1) / 4)
 * risk     = 100 * churn * exposure * authors
 * </pre>
 */
public final class RiskScorer {
    static final double BASE_EXPOSURE = 0.4;
    static final double AGENT_WEIGHT = 0.3;
    static final double UNFAMILIARITY_WEIGHT = 0.3;
    static final double BASE_AUTHOR_FACTOR = 0.85;
    static final int AUTHOR_SATURATION = 5;

    private RiskScorer() {
    }

    public static double churn(int commits, int maxCommits) {
        if (commits <= 0 || maxCommits <= 0) {
            return 0.0;
        }
        return Math.min(1.0, Math.log1p(commits) / Math.log1p(maxCommits));
    }

    public static double risk(FileHistoryStats stats, int maxCommits) {
        return risk(churn(stats.getCommits(), maxCommits), stats.agentShare(), stats.familiarity(), stats.getAuthors());
    }

    public static double risk(double churn, double agentShare, double familiarity, int authors) {
        double exposure = BASE_EXPOSURE + AGENT_WEIGHT * agentShare + UNFAMILIARITY_WEIGHT * (1.0 - familiarity);
        double authorFactor = BASE_AUTHOR_FACTOR
                + (1.0 - BASE_AUTHOR_FACTOR) * Math.min(1.0, Math.max(0, authors - 1) / (double) (AUTHOR_SATURATION - 1));
        return round(100.0 * churn * exposure * authorFactor);
    }

    static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
