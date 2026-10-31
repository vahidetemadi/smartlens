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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.intellij.openapi.diagnostic.Logger;
import com.vahid.plugin.smartlens.dto.AgentHookEntryDto;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads the JSON-lines log that AI agents append to after editing a file, e.g. via a Claude Code PostToolUse hook:
 * <pre>{"file": "/abs/or/relative/path", "agent": "claude-code", "ts": 1760000000}</pre>
 */
public final class HookLogReader {
    private static final Logger LOG = Logger.getInstance(HookLogReader.class);

    private HookLogReader() {
    }

    /**
     * @param logFile     the hook log file; a missing file yields an empty list
     * @param projectBase base directory used to resolve relative paths
     * @return valid entries, malformed lines are skipped
     */
    public static List<AgentHookEntryDto> read(Path logFile, Path projectBase) {
        if (!Files.isRegularFile(logFile)) {
            return List.of();
        }
        try {
            List<AgentHookEntryDto> entries = new ArrayList<>();
            for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
                parseLine(line, projectBase).ifPresent(entries::add);
            }
            return entries;
        } catch (IOException e) {
            LOG.warn("Cannot read agent hook log " + logFile, e);
            return List.of();
        }
    }

    static Optional<AgentHookEntryDto> parseLine(String line, Path projectBase) {
        if (line.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonElement element = JsonParser.parseString(line);
            if (!element.isJsonObject()) {
                return Optional.empty();
            }
            JsonObject json = element.getAsJsonObject();
            String file = Optional.ofNullable(string(json, "file")).orElse(string(json, "path"));
            if (file == null || file.isBlank()) {
                return Optional.empty();
            }
            String path = projectBase.resolve(file).normalize().toString().replace('\\', '/');
            String agent = Optional.ofNullable(string(json, "agent")).orElse("agent");
            long epochSeconds = json.has("ts") && json.get("ts").isJsonPrimitive() ? json.get("ts").getAsLong() : 0L;
            return Optional.of(new AgentHookEntryDto(path, agent, epochSeconds));
        } catch (JsonParseException | IllegalStateException | NumberFormatException | java.nio.file.InvalidPathException e) {
            return Optional.empty();
        }
    }

    @Nullable
    private static String string(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }
}
