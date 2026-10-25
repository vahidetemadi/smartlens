/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.study;

import com.vahid.plugin.smartlens.analysis.AgentMatcher;
import com.vahid.plugin.smartlens.analysis.FeatureExtractor;
import com.vahid.plugin.smartlens.analysis.FileHistoryStats;
import com.vahid.plugin.smartlens.analysis.GitLogParser;
import com.vahid.plugin.smartlens.analysis.LearningAnalyzer;
import com.vahid.plugin.smartlens.analysis.RiskScorer;
import com.vahid.plugin.smartlens.config.SmartLensState;
import com.vahid.plugin.smartlens.dto.CommitInfoDto;
import com.vahid.plugin.smartlens.dto.FileChangeDto;
import com.vahid.plugin.smartlens.dto.LearningItemDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Preliminary mining study: runs SmartLens' history analysis (the same classes the plugin uses) over local clones
 * of open-source repositories and writes the results to {@code build/study/mining.md}.
 * Run with {@code ./gradlew test --tests '*MiningStudy*' -Psmartlens.study.repos=/path/a,/path/b}.
 */
@EnabledIfSystemProperty(named = "smartlens.study.repos", matches = ".+")
class MiningStudy {
    private static final int MAX_COMMITS = 2_000;
    private static final double TRAIN_SHARE = 0.8;
    private static final int TOP_K = 10;
    private static final Pattern FIX_PATTERN = Pattern.compile("(?i)\\b(fix(e[sd])?|bug|regression|revert)\\b");
    private static final Pattern BOT_PATTERN = Pattern.compile("(?i)\\[bot]|\\bbot\\b|github-actions|renovate|dependabot");

    private final AgentMatcher agentMatcher = AgentMatcher.fromCommaSeparated(SmartLensState.DEFAULT_AGENT_PATTERNS);
    private final FeatureExtractor featureExtractor =
            new FeatureExtractor(Set.of(SmartLensState.DEFAULT_CODE_EXTENSIONS.split(",")));

    @Test
    void run() throws Exception {
        StringBuilder report = new StringBuilder("# SmartLens mining study\n\n");
        report.append("| Repository | Commits | Agent commits | Agent share of added code lines | Developer vocabulary | Agent features | New to developer | Top new skills |\n");
        report.append("|---|---|---|---|---|---|---|---|\n");
        StringBuilder risk = new StringBuilder("\n## Risk ranking\n\n");
        risk.append("| Repository | Candidate files | Overlap@10 with churn | Top-10 with >50% AI | Test-window fix commits | Base rate | P@10 churn | P@10 recent churn | P@10 SmartLens |\n");
        risk.append("|---|---|---|---|---|---|---|---|---|\n");

        for (String repository : System.getProperty("smartlens.study.repos").split(",")) {
            analyze(Path.of(repository.trim()), report, risk);
        }

        Path output = Path.of("build/study/mining.md");
        Files.createDirectories(output.getParent());
        Files.writeString(output, report.append(risk).toString(), StandardCharsets.UTF_8);
        System.out.println(report);
    }

    private void analyze(Path repository, StringBuilder report, StringBuilder risk) throws Exception {
        String name = repository.getFileName().toString().replaceFirst("_", "/");
        List<CommitInfoDto> commits = new ArrayList<>(GitLogParser.parseLog(git(repository, "log", "--no-merges", "--no-renames",
                "--no-color", "--no-ext-diff", "--no-textconv", "--max-count=" + MAX_COMMITS, "-p", "--unified=0",
                "--format=" + GitLogParser.LOG_FORMAT), ""));
        commits.sort(Comparator.comparingLong(CommitInfoDto::authorEpochSeconds));
        Map<String, String> subjects = git(repository, "log", "--no-merges", "--format=%H%x1f%s").stream()
                .map(line -> line.split("\u001F", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1], (a, b) -> a));
        Set<String> headFiles = git(repository, "ls-tree", "-r", "--name-only", "HEAD").stream()
                .map(path -> "/" + path)
                .collect(Collectors.toSet());

        // The developer persona: the most active human author (all e-mails used under that name)
        String persona = commits.stream()
                .filter(commit -> !BOT_PATTERN.matcher(commit.authorName() + " " + commit.authorEmail()).find())
                .filter(commit -> !agentMatcher.matches(commit.authorName()) && !agentMatcher.matches(commit.authorEmail()))
                .collect(Collectors.groupingBy(commit -> commit.authorName().toLowerCase(Locale.ROOT), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
        Set<String> personaEmails = commits.stream()
                .filter(commit -> commit.authorName().toLowerCase(Locale.ROOT).equals(persona))
                .map(commit -> commit.authorEmail().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        // Descriptive statistics and Learn
        LearningAnalyzer learningAnalyzer = new LearningAnalyzer(featureExtractor);
        Set<String> agentFeatures = new HashSet<>();
        long agentCommits = 0;
        long addedCodeLines = 0;
        long agentCodeLines = 0;
        for (CommitInfoDto commit : commits) {
            boolean agent = agentMatcher.isAgentCommit(commit);
            boolean developer = !agent && personaEmails.contains(commit.authorEmail().toLowerCase(Locale.ROOT));
            agentCommits += agent ? 1 : 0;
            for (FileChangeDto change : commit.files()) {
                if (!featureExtractor.isCodeFile(change.path())) {
                    continue;
                }
                addedCodeLines += change.linesAdded();
                if (agent) {
                    agentCodeLines += change.linesAdded();
                    learningAnalyzer.addAgentLines(change.path(), change.addedLines(), "commit " + commit.shortHash());
                    featureExtractor.extract(change.addedLines()).forEach(feature -> agentFeatures.add(feature.name()));
                } else if (developer) {
                    learningAnalyzer.addDeveloperLines(change.path(), change.addedLines());
                }
            }
        }
        List<LearningItemDto> novel = learningAnalyzer.novelItems(Integer.MAX_VALUE);
        String topSkills = novel.stream().limit(4)
                .map(item -> item.feature() + " (" + item.occurrences() + ")")
                .collect(Collectors.joining(", "));
        report.append(String.format(Locale.ROOT, "| %s | %d | %d (%.0f%%) | %.0f%% | %d | %d | %d | %s |%n",
                name, commits.size(), agentCommits, percent(agentCommits, commits.size()), percent(agentCodeLines, addedCodeLines),
                learningAnalyzer.developerVocabularySize(), agentFeatures.size(), novel.size(), topSkills));

        // Risk: rank on the first 80% of the history, check against fix commits in the remaining 20%
        int split = (int) Math.round(commits.size() * TRAIN_SHARE);
        Map<String, FileHistoryStats> stats = new HashMap<>();
        for (CommitInfoDto commit : commits.subList(0, split)) {
            boolean agent = agentMatcher.isAgentCommit(commit);
            boolean developer = !agent && personaEmails.contains(commit.authorEmail().toLowerCase(Locale.ROOT));
            for (FileChangeDto change : commit.files()) {
                stats.computeIfAbsent(change.path(), FileHistoryStats::new).record(commit, change, agent, developer);
            }
        }
        List<FileHistoryStats> candidates = stats.values().stream()
                .filter(file -> file.getCommits() >= 2 && headFiles.contains(file.getPath()) && RiskScorer.isRiskRelevant(file.getPath()))
                .toList();
        int maxCommits = candidates.stream().mapToInt(FileHistoryStats::getCommits).max().orElse(0);
        List<String> byChurn = candidates.stream()
                .sorted(Comparator.comparingInt(FileHistoryStats::getCommits).reversed().thenComparing(FileHistoryStats::getPath))
                .map(FileHistoryStats::getPath).toList();
        List<FileHistoryStats> byRisk = candidates.stream()
                .sorted(Comparator.comparingDouble((FileHistoryStats file) -> RiskScorer.risk(file, maxCommits)).reversed()
                        .thenComparing(FileHistoryStats::getPath))
                .toList();

        List<CommitInfoDto> fixCommits = commits.subList(split, commits.size()).stream()
                .filter(commit -> FIX_PATTERN.matcher(subjects.getOrDefault(commit.hash(), "")).find())
                .toList();
        Set<String> fixedFiles = fixCommits.stream()
                .flatMap(commit -> commit.files().stream().map(FileChangeDto::path))
                .collect(Collectors.toSet());

        // Recency baseline: commits in the last quarter of the training period
        long trainStart = commits.isEmpty() ? 0 : commits.get(0).authorEpochSeconds();
        long trainEnd = split == 0 ? 0 : commits.get(split - 1).authorEpochSeconds();
        long recentFrom = trainEnd - (trainEnd - trainStart) / 4;
        Map<String, Long> recentCommits = commits.subList(0, split).stream()
                .filter(commit -> commit.authorEpochSeconds() >= recentFrom)
                .flatMap(commit -> commit.files().stream().map(FileChangeDto::path))
                .collect(Collectors.groupingBy(path -> path, Collectors.counting()));
        List<String> byRecentChurn = candidates.stream()
                .sorted(Comparator.comparingLong((FileHistoryStats file) -> recentCommits.getOrDefault(file.getPath(), 0L)).reversed()
                        .thenComparing(Comparator.comparingInt(FileHistoryStats::getCommits).reversed())
                        .thenComparing(FileHistoryStats::getPath))
                .map(FileHistoryStats::getPath).toList();

        List<String> topRisk = byRisk.stream().limit(TOP_K).map(FileHistoryStats::getPath).toList();
        List<String> topChurn = byChurn.stream().limit(TOP_K).toList();
        long overlap = topRisk.stream().filter(topChurn::contains).count();
        long aiHeavy = byRisk.stream().limit(TOP_K).filter(file -> file.agentShare() > 0.5).count();
        double baseRate = candidates.isEmpty() ? 0 : (double) candidates.stream().filter(file -> fixedFiles.contains(file.getPath())).count() / candidates.size();
        risk.append(String.format(Locale.ROOT, "| %s | %d | %d/%d | %d | %d | %.2f | %.2f | %.2f | %.2f |%n",
                name, candidates.size(), overlap, Math.min(TOP_K, candidates.size()), aiHeavy, fixCommits.size(), baseRate,
                precision(topChurn, fixedFiles), precision(byRecentChurn.stream().limit(TOP_K).toList(), fixedFiles),
                precision(topRisk, fixedFiles)));
        System.out.printf("%s: persona=%s emails=%s split=%s%n", name, persona, personaEmails,
                commits.isEmpty() ? "-" : java.time.Instant.ofEpochSecond(commits.get(Math.min(split, commits.size() - 1)).authorEpochSeconds()));
    }

    private static double precision(List<String> ranking, Set<String> relevant) {
        return ranking.isEmpty() ? 0 : (double) ranking.stream().filter(relevant::contains).count() / ranking.size();
    }

    private static double percent(long part, long total) {
        return total == 0 ? 0 : 100.0 * part / total;
    }

    private static List<String> git(Path repository, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("git", "-c", "core.quotePath=false"));
        command.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(command).directory(repository.toFile()).redirectErrorStream(false).start();
        List<String> lines = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        if (!process.waitFor(5, TimeUnit.MINUTES) || process.exitValue() != 0) {
            throw new IllegalStateException("git failed in " + repository + ": " + command);
        }
        return lines;
    }
}
