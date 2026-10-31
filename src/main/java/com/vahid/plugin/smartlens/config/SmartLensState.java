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
package com.vahid.plugin.smartlens.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@State(
        name = "SmartLensState",
        storages = {@Storage("SmartLensState.xml")}
)
@Service(Service.Level.APP)
public final class SmartLensState implements PersistentStateComponent<SmartLensState> {
    public static final String DEFAULT_AGENT_PATTERNS = "claude,anthropic,copilot,openai,codex,cursor,aider,devin,gemini,junie";
    public static final String DEFAULT_HOOK_LOG_PATH = ".smartlens/agent-edits.jsonl";
    public static final String DEFAULT_CODE_EXTENSIONS = "java,kt,kts";

    public String agentPatterns = DEFAULT_AGENT_PATTERNS;
    public String hookLogPath = DEFAULT_HOOK_LOG_PATH;
    public String codeExtensions = DEFAULT_CODE_EXTENSIONS;
    public String developerAliases = "";
    public int historyDepth = 300;
    public int vcsQuietSeconds = 5;

    public static SmartLensState getInstance() {
        return ApplicationManager.getApplication().getService(SmartLensState.class);
    }

    @Override
    public @NotNull SmartLensState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SmartLensState smartLensState) {
        XmlSerializerUtil.copyBean(smartLensState, this);
    }

    public Set<String> codeExtensionSet() {
        return commaSeparated(codeExtensions);
    }

    /**
     * @return other Git e-mail addresses of the developer, lower-cased (commits made from other machines or accounts)
     */
    public Set<String> developerAliasSet() {
        return commaSeparated(developerAliases.toLowerCase(java.util.Locale.ROOT));
    }

    private static Set<String> commaSeparated(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toSet());
    }
}
