/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
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
