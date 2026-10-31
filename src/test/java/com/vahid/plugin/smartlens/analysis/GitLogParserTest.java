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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitLogParserTest {
    private static final String M = GitLogParser.COMMIT_MARKER;

    @Test
    void test_given_logWithTwoCommits_when_parsed_then_headersTrailersAndAddedLinesAreExtracted() {
        List<String> lines = List.of(
                M + "abc123456789\u001Fdev@mail.com\u001FDev\u001F1700000000\u001FClaude <noreply@anthropic.com>\u001DOther <o@x>",
                "",
                "diff --git a/src/A.java b/src/A.java",
                "index 1..2 100644",
                "--- a/src/A.java",
                "+++ b/src/A.java",
                "@@ -1,0 +2,2 @@",
                "+import java.util.Optional;",
                "++++ weird content starting with plus",
                "@@ -10 +11,0 @@",
                "-removed",
                "\\ No newline at end of file",
                M + "def\u001Fme@mail.com\u001FMe\u001F1690000000\u001F",
                "",
                "diff --git a/old.txt b/old.txt",
                "deleted file mode 100644",
                "--- a/old.txt",
                "+++ /dev/null",
                "@@ -1,2 +0,0 @@",
                "-a",
                "-b");

        List<CommitInfoDto> commits = GitLogParser.parseLog(lines, "/repo");

        assertThat(commits).hasSize(2);
        CommitInfoDto first = commits.get(0);
        assertThat(first.authorEmail()).isEqualTo("dev@mail.com");
        assertThat(first.authorEpochSeconds()).isEqualTo(1700000000L);
        assertThat(first.coAuthors()).containsExactly("Claude <noreply@anthropic.com>", "Other <o@x>");
        assertThat(first.shortHash()).isEqualTo("abc12345");
        FileChangeDto change = first.files().get(0);
        assertThat(change.path()).isEqualTo("/repo/src/A.java");
        assertThat(change.addedLines()).containsExactly("import java.util.Optional;", "+++ weird content starting with plus");
        assertThat(change.linesDeleted()).isEqualTo(1);

        CommitInfoDto second = commits.get(1);
        assertThat(second.coAuthors()).isEmpty();
        assertThat(second.files()).singleElement().satisfies(deleted -> {
            assertThat(deleted.path()).isEqualTo("/repo/old.txt");
            assertThat(deleted.linesAdded()).isZero();
            assertThat(deleted.linesDeleted()).isEqualTo(2);
        });
    }

    @Test
    void test_given_binaryDiff_when_parsed_then_fileHasNoLines() {
        List<FileChangeDto> files = GitLogParser.parseDiff(List.of(
                "diff --git a/img.png b/img.png",
                "Binary files a/img.png and b/img.png differ"), "/repo/");

        assertThat(files).singleElement().satisfies(file -> {
            assertThat(file.path()).isEqualTo("/repo/img.png");
            assertThat(file.linesAdded()).isZero();
        });
    }
}
