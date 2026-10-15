/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.value;

/**
 * Who produced the pending (uncommitted) changes of a file.
 * The score reflects how much the change deserves the developer's attention:
 * agent-only changes score highest, mixed changes lower, developer-only changes lowest.
 */
public enum ChangeOrigin {
    AGENT("AI agent", 1.0),
    MIXED("AI agent + developer", 0.6),
    UNKNOWN("Unknown", 0.3),
    DEVELOPER("Developer", 0.0);

    private final String displayName;
    private final double score;

    ChangeOrigin(String displayName, double score) {
        this.displayName = displayName;
        this.score = score;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getScore() {
        return score;
    }

    public boolean involvesAgent() {
        return this == AGENT || this == MIXED;
    }

    /**
     * Classifies a file based on the edits observed for it since the last commit.
     *
     * @param agentEdits     number of edits attributed to an AI agent
     * @param developerEdits number of edits typed by the developer inside the IDE
     * @return the resulting origin, {@link #UNKNOWN} when no edit has been observed
     */
    public static ChangeOrigin classify(int agentEdits, int developerEdits) {
        if (agentEdits > 0 && developerEdits > 0) {
            return MIXED;
        } else if (agentEdits > 0) {
            return AGENT;
        } else if (developerEdits > 0) {
            return DEVELOPER;
        }
        return UNKNOWN;
    }
}
