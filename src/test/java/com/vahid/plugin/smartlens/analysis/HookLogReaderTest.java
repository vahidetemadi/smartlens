/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.analysis;

import com.vahid.plugin.smartlens.dto.AgentHookEntryDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HookLogReaderTest {

    @Test
    void test_given_hookLog_when_read_then_validLinesAreResolvedAndMalformedSkipped(@TempDir Path base) throws IOException {
        Path log = base.resolve(".smartlens/agent-edits.jsonl");
        Files.createDirectories(log.getParent());
        Files.writeString(log, String.join("\n",
                "{\"file\": \"src/A.java\", \"agent\": \"claude-code\", \"ts\": 1700000000.7}",
                "{\"path\": \"" + base.resolve("B.java").toString().replace('\\', '/') + "\"}",
                "not json",
                "[1, 2]",
                "{\"agent\": \"no-file\"}",
                ""));

        List<AgentHookEntryDto> entries = HookLogReader.read(log, base);

        String root = base.toString().replace('\\', '/');
        assertThat(entries).containsExactly(
                new AgentHookEntryDto(root + "/src/A.java", "claude-code", 1700000000L),
                new AgentHookEntryDto(root + "/B.java", "agent", 0L));
    }

    @Test
    void test_given_missingLog_when_read_then_emptyList(@TempDir Path base) {
        assertThat(HookLogReader.read(base.resolve("missing.jsonl"), base)).isEmpty();
    }
}
