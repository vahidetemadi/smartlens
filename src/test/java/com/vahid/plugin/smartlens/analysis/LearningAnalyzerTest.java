/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.analysis;

import com.vahid.plugin.smartlens.dto.LearningItemDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LearningAnalyzerTest {

    @Test
    void test_given_agentAndDeveloperLines_when_novelItemsRequested_then_onlyUnknownSkillsAreRankedByUse() {
        LearningAnalyzer analyzer = new LearningAnalyzer(new FeatureExtractor(Set.of("java")));
        analyzer.addDeveloperLines("/r/Dev.java", List.of("return Optional.of(x);", "import java.util.List;"));
        analyzer.addDeveloperLines("/r/notes.md", List.of("public record Ignored(int a) {}"));
        analyzer.addAgentLines("/r/Ai.java", List.of(
                "return Optional.empty();",
                "public record Point(int x, int y) {}",
                "record Pair<A, B>(A a, B b) {}",
                "import java.util.concurrent.CompletableFuture;"), "commit abc");
        analyzer.addAgentLines("/r/ai.md", List.of("sealed interface Shape permits A {}"), "commit abc");

        List<LearningItemDto> items = analyzer.novelItems(10);

        assertThat(items).extracting(LearningItemDto::feature)
                .containsExactly("Record", "java.util.concurrent.CompletableFuture");
        assertThat(items.get(0).occurrences()).isEqualTo(2);
        assertThat(items.get(0).examplePath()).isEqualTo("/r/Ai.java");
        assertThat(items.get(0).source()).isEqualTo("commit abc");
        assertThat(analyzer.novelItems(1)).hasSize(1);
    }
}
