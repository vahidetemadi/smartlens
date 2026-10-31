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
