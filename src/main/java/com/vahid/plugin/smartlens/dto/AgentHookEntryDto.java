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
package com.vahid.plugin.smartlens.dto;

/**
 * One line of the agent hook log, written by an AI agent after it edited a file.
 *
 * @param path         absolute, system-independent path of the edited file
 * @param agent        name of the agent, e.g. "claude-code"
 * @param epochSeconds time of the edit, 0 when the agent did not report one
 */
public record AgentHookEntryDto(String path, String agent, long epochSeconds) {
}
