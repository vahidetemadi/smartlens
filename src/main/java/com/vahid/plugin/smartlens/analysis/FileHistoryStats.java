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
import com.vahid.plugin.smartlens.dto.FileChangeDto;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Accumulates the git history of one file: churn, authorship and the share written by agents or the developer.
 */
public final class FileHistoryStats {
    private final String path;
    private final Set<String> authors = new HashSet<>();
    private int commits;
    private int developerCommits;
    private int linesAdded;
    private int agentLinesAdded;
    private int developerLinesAdded;

    public FileHistoryStats(String path) {
        this.path = path;
    }

    public void record(CommitInfoDto commit, FileChangeDto change, boolean agentCommit, boolean developerCommit) {
        commits++;
        authors.add(commit.authorEmail().toLowerCase(Locale.ROOT));
        linesAdded += change.linesAdded();
        if (agentCommit) {
            agentLinesAdded += change.linesAdded();
        }
        if (developerCommit) {
            developerCommits++;
            developerLinesAdded += change.linesAdded();
        }
    }

    public String getPath() {
        return path;
    }

    public int getCommits() {
        return commits;
    }

    public int getAuthors() {
        return authors.size();
    }

    /**
     * @return share (0..1) of the added lines that came from agent commits
     */
    public double agentShare() {
        return linesAdded == 0 ? 0.0 : (double) agentLinesAdded / linesAdded;
    }

    /**
     * Familiarity of the developer with this file, approximated by the share of lines they wrote
     * (falls back to the share of commits when the history only deleted lines).
     *
     * @return familiarity between 0 (never touched) and 1 (sole author)
     */
    public double familiarity() {
        if (linesAdded > 0) {
            return (double) developerLinesAdded / linesAdded;
        }
        return commits == 0 ? 0.0 : (double) developerCommits / commits;
    }
}
