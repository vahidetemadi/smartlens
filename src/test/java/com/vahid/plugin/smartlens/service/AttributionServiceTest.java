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
