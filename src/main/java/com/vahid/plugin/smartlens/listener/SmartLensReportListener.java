/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.listener;

import com.intellij.util.messages.Topic;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;

/**
 * Project-level topic notified (on the EDT) whenever a new SmartLens report is available.
 */
public interface SmartLensReportListener {
    @Topic.ProjectLevel
    Topic<SmartLensReportListener> TOPIC = Topic.create("SmartLens report", SmartLensReportListener.class);

    void reportUpdated(SmartLensReportDto report);
}
