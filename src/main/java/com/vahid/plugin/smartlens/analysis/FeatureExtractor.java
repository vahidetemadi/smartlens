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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexical, language-light extraction of "skills" (imported APIs, annotations and language features)
 * from source lines. It works on diff fragments, where a full parse (PSI) is not possible.
 */
public final class FeatureExtractor {
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "^\\s*import\\s+(?:static\\s+)?([\\w.]+?)(?:\\.\\*)?(?:\\s+as\\s+\\w+)?\\s*;?\\s*$");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;?\\s*$");
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("(?<![\\w.@\"])@([A-Z]\\w*)");
    private static final Set<String> TRIVIAL_ANNOTATIONS = Set.of("Override", "Deprecated", "SuppressWarnings");
    private static final Map<String, LanguageFeature> LANGUAGE_FEATURES = new LinkedHashMap<>();

    static {
        register("Record", "\\brecord\\s+[A-Z]\\w*\\s*[(<]",
                "Immutable data carrier with generated constructor, accessors, equals/hashCode and toString.");
        register("Sealed type", "\\bsealed\\s+(?:abstract\\s+)?(?:class|interface)\\b|\\bpermits\\b",
                "Restricts which classes may extend a type, enabling exhaustive switches.");
        register("Switch arrow labels", "\\bcase\\s+[^:]+->",
                "Arrow-form switch (no fall-through), usable as an expression.");
        register("Pattern matching for instanceof", "\\binstanceof\\s+(?:final\\s+)?[A-Z][\\w.<>?, ]*\\s+[a-z]\\w*",
                "Tests a type and binds a typed variable in one step, removing the explicit cast.");
        register("Text block", "\"\"\"",
                "Multi-line string literal that keeps formatting readable.");
        register("Local variable type inference (var)", "\\bvar\\s+[a-z]\\w*\\s*[=:]",
                "Lets the compiler infer a local variable's type.");
        register("Method reference", "\\w::\\w|\\)::\\w",
                "Compact lambda that points to an existing method.");
        register("Lambda expression", "(?:\\(\\s*[\\w\\s,]*\\)|\\b[a-z]\\w*)\\s*->(?!\\s*\\{?\\s*$)|\\{\\s*[a-z]\\w*\\s*->",
                "Anonymous function passed as a value.");
        register("Stream API", "\\.stream\\(\\)|\\bStream\\.(?:of|iterate|generate)\\(|\\.collect\\(",
                "Declarative pipeline (map/filter/collect) over collections.");
        register("Optional", "\\bOptional\\.(?:of|ofNullable|empty)\\(",
                "Explicit container for a value that may be absent, instead of null.");
        register("Try-with-resources", "\\btry\\s*\\(",
                "Closes AutoCloseable resources automatically, even on exceptions.");
        register("Bounded wildcard generics", "<\\s*\\?\\s+(?:extends|super)\\b",
                "Makes generic APIs flexible for producers (extends) and consumers (super).");
        register("Default interface method", "\\bdefault\\s+(?:<[^>]+>\\s*)?[\\w<>\\[\\],? ]+\\s+\\w+\\s*\\(",
                "Interface method with a body, evolving APIs without breaking implementors.");
        register("Varargs", "\\w\\.\\.\\.\\s+\\w",
                "Accepts a variable number of arguments.");
        register("Synchronized block", "\\bsynchronized\\s*\\(",
                "Guards a critical section with a monitor lock.");
        register("Volatile field", "\\bvolatile\\b",
                "Guarantees visibility of writes across threads.");
        register("Kotlin data class", "\\bdata\\s+class\\b",
                "Kotlin class with generated equals/hashCode/toString/copy.");
        register("Kotlin coroutine", "\\bsuspend\\s+fun\\b|\\b(?:launch|async|runBlocking|withContext)\\s*[({]",
                "Asynchronous code written sequentially with suspend functions.");
        register("Kotlin extension function", "\\bfun\\s+(?:<[^>]+>\\s*)?[A-Z][\\w<>?, ]*\\.\\w+\\s*\\(",
                "Adds a function to an existing type without inheritance.");
    }

    private final Set<String> codeExtensions;

    public FeatureExtractor(Set<String> codeExtensions) {
        this.codeExtensions = codeExtensions.stream()
                .map(extension -> extension.trim().toLowerCase(Locale.ROOT))
                .filter(extension -> !extension.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }

    public boolean isCodeFile(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 && codeExtensions.contains(path.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public Set<FeatureDto> extract(List<String> lines) {
        Set<FeatureDto> features = new HashSet<>();
        lines.forEach(line -> features.addAll(extract(line)));
        return features;
    }

    /**
     * Extracts the features used in a single source line; comment lines are ignored.
     *
     * @param line raw source line
     * @return features found in the line
     */
    public Set<FeatureDto> extract(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
            return Set.of();
        }

        Set<FeatureDto> features = new HashSet<>();
        Matcher importMatcher = IMPORT_PATTERN.matcher(trimmed);
        if (importMatcher.matches()) {
            features.add(new FeatureDto(importMatcher.group(1), FeatureCategory.LIBRARY_API));
            return features;
        }

        Matcher annotationMatcher = ANNOTATION_PATTERN.matcher(trimmed);
        while (annotationMatcher.find()) {
            String annotation = annotationMatcher.group(1);
            if (!TRIVIAL_ANNOTATIONS.contains(annotation)) {
                features.add(new FeatureDto("@" + annotation, FeatureCategory.ANNOTATION));
            }
        }

        LANGUAGE_FEATURES.forEach((name, feature) -> {
            if (feature.pattern().matcher(trimmed).find()) {
                features.add(new FeatureDto(name, FeatureCategory.LANGUAGE_FEATURE));
            }
        });
        return features;
    }

    /**
     * @return the package declared by the line ({@code package a.b;}), if any
     */
    public Optional<String> declaredPackage(String line) {
        Matcher matcher = PACKAGE_PATTERN.matcher(line);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public String describe(FeatureDto feature) {
        return switch (feature.category()) {
            case LANGUAGE_FEATURE -> {
                LanguageFeature languageFeature = LANGUAGE_FEATURES.get(feature.name());
                yield languageFeature != null ? languageFeature.description() : feature.name();
            }
            case LIBRARY_API -> {
                int lastDot = feature.name().lastIndexOf('.');
                String owner = lastDot > 0 ? feature.name().substring(0, lastDot) : feature.name();
                yield "API from " + owner + " that you have not imported in your own commits.";
            }
            case ANNOTATION -> "Annotation you have not used in your own commits.";
        };
    }

    private static void register(String name, String regex, String description) {
        LANGUAGE_FEATURES.put(name, new LanguageFeature(Pattern.compile(regex), description));
    }

    private record LanguageFeature(Pattern pattern, String description) {
    }
}
