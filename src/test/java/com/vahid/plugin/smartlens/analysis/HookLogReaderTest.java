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
