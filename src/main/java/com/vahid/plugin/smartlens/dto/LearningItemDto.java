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

import com.vahid.plugin.smartlens.value.FeatureCategory;

/**
 * A skill used by an AI agent that never appeared in the developer's own commits.
 */
public record LearningItemDto(String feature,
                              FeatureCategory category,
                              String description,
                              int occurrences,
                              String examplePath,
                              String source) {
}
