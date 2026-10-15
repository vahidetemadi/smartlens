/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.analysis;

import com.vahid.plugin.smartlens.dto.FeatureDto;
import com.vahid.plugin.smartlens.value.FeatureCategory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureExtractorTest {
    private final FeatureExtractor extractor = new FeatureExtractor(Set.of("java", " KT "));

    @Test
    void test_given_importLine_when_extracted_then_onlyLibraryApiIsReturned() {
        assertThat(extractor.extract("import java.util.concurrent.CompletableFuture;"))
                .containsExactly(new FeatureDto("java.util.concurrent.CompletableFuture", FeatureCategory.LIBRARY_API));
        assertThat(extractor.extract("import static java.util.stream.Collectors.toList;"))
                .containsExactly(new FeatureDto("java.util.stream.Collectors.toList", FeatureCategory.LIBRARY_API));
        assertThat(extractor.extract("import kotlinx.coroutines.flow.Flow as F"))
                .containsExactly(new FeatureDto("kotlinx.coroutines.flow.Flow", FeatureCategory.LIBRARY_API));
    }

    @Test
    void test_given_modernJavaLines_when_extracted_then_languageFeaturesAreFound() {
        assertThat(names("public record Point(int x, int y) {}")).contains("Record");
        assertThat(names("if (shape instanceof Circle c) {")).contains("Pattern matching for instanceof");
        assertThat(names("case CIRCLE -> 1;")).contains("Switch arrow labels");
        assertThat(names("list.stream().map(String::trim).toList();")).contains("Stream API", "Method reference");
        assertThat(names("try (var in = Files.newInputStream(p)) {"))
                .contains("Try-with-resources", "Local variable type inference (var)");
        assertThat(names("return Optional.ofNullable(value);")).contains("Optional");
        assertThat(names("items.forEach(item -> log(item));")).contains("Lambda expression");
    }

    @Test
    void test_given_annotationsAndComments_when_extracted_then_trivialAndCommentedAreIgnored() {
        assertThat(extractor.extract("@Override @Service(Service.Level.APP)"))
                .containsExactly(new FeatureDto("@Service", FeatureCategory.ANNOTATION));
        assertThat(extractor.extract("// Optional.of(x) is great")).isEmpty();
        assertThat(extractor.extract(" * @param value the value")).isEmpty();
    }

    @Test
    void test_given_paths_when_checkingCodeFile_then_configuredExtensionsAreCaseInsensitive() {
        assertThat(extractor.isCodeFile("/a/B.java")).isTrue();
        assertThat(extractor.isCodeFile("/a/B.KT")).isTrue();
        assertThat(extractor.isCodeFile("/a/README.md")).isFalse();
        assertThat(extractor.isCodeFile("/a/Makefile")).isFalse();
    }

    private Set<String> names(String line) {
        return extractor.extract(line).stream().map(FeatureDto::name).collect(java.util.stream.Collectors.toSet());
    }
}
