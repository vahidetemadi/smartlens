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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses the output of {@code git log -p --unified=0 --format=}{@link #LOG_FORMAT}
 * and of {@code git diff --unified=0} into DTOs.
 */
public final class GitLogParser {
    static final String COMMIT_MARKER = "\u001ESMARTLENS\u001F";
    private static final String FIELD_SEPARATOR = "\u001F";
    private static final String TRAILER_SEPARATOR = "\u001D";
    private static final String DEV_NULL = "/dev/null";

    /**
     * One header line per commit: marker, hash, author email, author name, author time and co-author trailers.
     */
    public static final String LOG_FORMAT =
            "%x1eSMARTLENS%x1f%H%x1f%ae%x1f%an%x1f%at%x1f%(trailers:key=Co-authored-by,valueonly,separator=%x1d)";

    private GitLogParser() {
    }

    /**
     * Parses a git log with patches.
     *
     * @param lines    raw output lines of git log
     * @param rootPath system-independent path of the repository root, used to make file paths absolute
     * @return commits in the order git printed them (newest first)
     */
    public static List<CommitInfoDto> parseLog(List<String> lines, String rootPath) {
        List<CommitInfoDto> commits = new ArrayList<>();
        String[] header = null;
        List<String> commitLines = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith(COMMIT_MARKER)) {
                if (header != null) {
                    commits.add(toCommit(header, parseDiff(commitLines, rootPath)));
                }
                header = line.substring(COMMIT_MARKER.length()).split(FIELD_SEPARATOR, -1);
                commitLines = new ArrayList<>();
            } else if (header != null) {
                commitLines.add(line);
            }
        }
        if (header != null) {
            commits.add(toCommit(header, parseDiff(commitLines, rootPath)));
        }
        return commits;
    }

    /**
     * Parses a unified diff (any context size) into per-file changes.
     *
     * @param lines    raw diff lines
     * @param rootPath system-independent path of the repository root
     * @return changed files with their added lines and deleted line count
     */
    public static List<FileChangeDto> parseDiff(List<String> lines, String rootPath) {
        List<FileChangeDto> files = new ArrayList<>();
        String path = null;
        List<String> added = new ArrayList<>();
        int deleted = 0;
        boolean inHunk = false;

        for (String line : lines) {
            if (line.startsWith("diff --git ")) {
                if (path != null) {
                    files.add(new FileChangeDto(absolute(rootPath, path), added, deleted));
                }
                // Fallback path; refined by the '+++'/'---' lines when present
                path = pathFromDiffHeader(line);
                added = new ArrayList<>();
                deleted = 0;
                inHunk = false;
                continue;
            }
            if (path == null) {
                continue;
            }
            if (!inHunk) {
                if (line.startsWith("+++ ")) {
                    String newPath = stripPrefix(line.substring(4));
                    if (newPath != null) {
                        path = newPath;
                    }
                } else if (line.startsWith("@@")) {
                    inHunk = true;
                }
                continue;
            }
            if (line.startsWith("+")) {
                added.add(line.substring(1));
            } else if (line.startsWith("-")) {
                deleted++;
            }
            // '@@' starts another hunk, ' ' is context and '\' is "no newline at end of file"; none of them counts
        }
        if (path != null) {
            files.add(new FileChangeDto(absolute(rootPath, path), added, deleted));
        }
        return files;
    }

    private static CommitInfoDto toCommit(String[] header, List<FileChangeDto> files) {
        String trailers = field(header, 4);
        List<String> coAuthors = trailers.isBlank()
                ? List.of()
                : Arrays.stream(trailers.split(TRAILER_SEPARATOR)).map(String::trim).filter(s -> !s.isEmpty()).toList();
        long time;
        try {
            time = Long.parseLong(field(header, 3).trim());
        } catch (NumberFormatException e) {
            time = 0L;
        }
        return new CommitInfoDto(field(header, 0), field(header, 1), field(header, 2), time, coAuthors, files);
    }

    private static String field(String[] header, int index) {
        return index < header.length ? header[index] : "";
    }

    private static String pathFromDiffHeader(String line) {
        int index = line.lastIndexOf(" b/");
        return index >= 0 ? unquote(line.substring(index + 3)) : unquote(line.substring("diff --git ".length()));
    }

    @Nullable
    private static String stripPrefix(String path) {
        String unquoted = unquote(path.trim());
        if (DEV_NULL.equals(unquoted)) {
            return null;
        }
        return unquoted.startsWith("b/") || unquoted.startsWith("a/") ? unquoted.substring(2) : unquoted;
    }

    private static String unquote(String path) {
        return path.length() >= 2 && path.startsWith("\"") && path.endsWith("\"")
                ? path.substring(1, path.length() - 1)
                : path;
    }

    private static String absolute(String rootPath, String relativePath) {
        return rootPath.endsWith("/") ? rootPath + relativePath : rootPath + "/" + relativePath;
    }
}
