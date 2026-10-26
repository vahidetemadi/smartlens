/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.UI;

import com.vahid.plugin.smartlens.dto.SmartLensReportDto;

/**
 * A tab of the SmartLens tool window that renders the latest report (as a table or as charts).
 */
public interface ReportView {
    void showReport(SmartLensReportDto report);
}
