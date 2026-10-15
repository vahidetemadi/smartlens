/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.service;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.vahid.plugin.smartlens.value.ChangeOrigin;

import java.util.Set;

public class AttributionServiceTest extends BasePlatformTestCase {

    @Override
    protected void tearDown() throws Exception {
        try {
            AttributionService.getInstance(getProject()).clear();
        } finally {
            super.tearDown();
        }
    }

    public void test_given_agentAndDeveloperEdits_when_origin_then_classifiedPerFile() {
        AttributionService service = AttributionService.getInstance(getProject());
        service.recordAgentEdit("/p/Agent.java");
        service.recordAgentEdit("/p/Mixed.java");
        service.recordDeveloperEdit("/p/Mixed.java");
        service.recordDeveloperEdit("/p/Dev.java");

        assertEquals(ChangeOrigin.AGENT, service.origin("/p/Agent.java", false));
        assertEquals(ChangeOrigin.MIXED, service.origin("/p/Mixed.java", false));
        assertEquals(ChangeOrigin.DEVELOPER, service.origin("/p/Dev.java", false));
        assertEquals(ChangeOrigin.MIXED, service.origin("/p/Dev.java", true));
        assertEquals(ChangeOrigin.UNKNOWN, service.origin("/p/Other.java", false));
    }

    public void test_given_committedFiles_when_retainOnly_then_theirRecordsAreDropped() {
        AttributionService service = AttributionService.getInstance(getProject());
        service.recordAgentEdit("/p/Committed.java");
        service.recordAgentEdit("/p/Pending.java");

        service.retainOnly(Set.of("/p/Pending.java"));

        assertEquals(ChangeOrigin.UNKNOWN, service.origin("/p/Committed.java", false));
        assertEquals(ChangeOrigin.AGENT, service.origin("/p/Pending.java", false));
        assertEquals(Set.of("/p/Pending.java"), service.getState().records.keySet());
    }

    public void test_given_externalChange_when_checkedImmediately_then_documentReloadIsNotDeveloperTyping() {
        AttributionService service = AttributionService.getInstance(getProject());
        service.markExternalChange("/p/A.java");

        assertTrue(service.isRecentlyExternallyChanged("/p/A.java"));
        assertFalse(service.isRecentlyExternallyChanged("/p/B.java"));
    }

    public void test_given_vcsOperation_when_marked_then_quietPeriodIsActive() {
        AttributionService service = AttributionService.getInstance(getProject());
        service.markVcsOperation();

        assertTrue(service.isInVcsQuietPeriod());
    }
}
