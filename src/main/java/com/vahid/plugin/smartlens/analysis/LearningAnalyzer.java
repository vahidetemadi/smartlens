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

import com.vahid.plugin.smartlens.dto.FeatureDto;
import com.vahid.plugin.smartlens.dto.LearningItemDto;
import com.vahid.plugin.smartlens.value.FeatureCategory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compares the skills an AI agent used with the developer's own vocabulary (built from their commits)
 * and reports the ones that are new to the developer. Imports of the project's own packages are not skills.
 */
public final class LearningAnalyzer {
    private final FeatureExtractor featureExtractor;
    private final Set<FeatureDto> developerVocabulary = new HashSet<>();
    private final Map<FeatureDto, AgentUsage> agentUsages = new HashMap<>();
    private final Set<String> projectPackages = new HashSet<>();

    public LearningAnalyzer(FeatureExtractor featureExtractor) {
        this.featureExtractor = featureExtractor;
    }

    public void addDeveloperLines(String path, List<String> lines) {
        if (featureExtractor.isCodeFile(path)) {
            collectPackages(lines);
            developerVocabulary.addAll(featureExtractor.extract(lines));
        }
    }

    public void addAgentLines(String path, List<String> lines, String source) {
        if (!featureExtractor.isCodeFile(path)) {
            return;
        }
        collectPackages(lines);
        for (String line : lines) {
            for (FeatureDto feature : featureExtractor.extract(line)) {
                // Keep the first example found; history is processed newest first
                agentUsages.computeIfAbsent(feature, k -> new AgentUsage(path, source)).occurrences++;
            }
        }
    }

    public int developerVocabularySize() {
        return developerVocabulary.size();
    }

    /**
     * @param limit maximum number of items to return
     * @return agent features missing from the developer vocabulary, most used first
     */
    public List<LearningItemDto> novelItems(int limit) {
        return agentUsages.entrySet().stream()
                .filter(entry -> !developerVocabulary.contains(entry.getKey()))
                .filter(entry -> !isProjectImport(entry.getKey()))
                .sorted(Comparator.<Map.Entry<FeatureDto, AgentUsage>>comparingInt(entry -> entry.getValue().occurrences)
                        .reversed()
                        .thenComparing(entry -> entry.getKey().name()))
                .limit(limit)
                .map(entry -> new LearningItemDto(
                        entry.getKey().name(),
                        entry.getKey().category(),
                        featureExtractor.describe(entry.getKey()),
                        entry.getValue().occurrences,
                        entry.getValue().examplePath,
                        entry.getValue().source))
                .toList();
    }

    private void collectPackages(List<String> lines) {
        lines.forEach(line -> featureExtractor.declaredPackage(line).ifPresent(projectPackages::add));
    }

    private boolean isProjectImport(FeatureDto feature) {
        return feature.category() == FeatureCategory.LIBRARY_API
                && projectPackages.stream().anyMatch(projectPackage -> feature.name().startsWith(projectPackage + "."));
    }

    private static final class AgentUsage {
        private final String examplePath;
        private final String source;
        private int occurrences;

        private AgentUsage(String examplePath, String source) {
            this.examplePath = examplePath;
            this.source = source;
        }
    }
}
