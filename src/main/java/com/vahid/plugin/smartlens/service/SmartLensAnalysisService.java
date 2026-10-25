/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.service;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.vahid.plugin.smartlens.UI.SmartLensNotifier;
import com.vahid.plugin.smartlens.analysis.AgentMatcher;
import com.vahid.plugin.smartlens.analysis.FeatureExtractor;
import com.vahid.plugin.smartlens.analysis.FileHistoryStats;
import com.vahid.plugin.smartlens.analysis.HookLogReader;
import com.vahid.plugin.smartlens.analysis.LearningAnalyzer;
import com.vahid.plugin.smartlens.analysis.ReviewPrioritizer;
import com.vahid.plugin.smartlens.analysis.RiskScorer;
import com.vahid.plugin.smartlens.config.SmartLensState;
import com.vahid.plugin.smartlens.dto.AgentHookEntryDto;
import com.vahid.plugin.smartlens.dto.CommitInfoDto;
import com.vahid.plugin.smartlens.dto.FileChangeDto;
import com.vahid.plugin.smartlens.dto.HotspotDto;
import com.vahid.plugin.smartlens.dto.ReviewItemDto;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;
import com.vahid.plugin.smartlens.listener.SmartLensReportListener;
import com.vahid.plugin.smartlens.value.ChangeOrigin;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Builds the SmartLens report (Learn, Understand and Risk) from the git history,
 * the pending changes and the attribution collected by the listeners.
 */
@Service(Service.Level.PROJECT)
public final class SmartLensAnalysisService {
    private static final Logger LOG = Logger.getInstance(SmartLensAnalysisService.class);
    private static final int MAX_LEARNING_ITEMS = 200;
    private static final int MAX_HOTSPOTS = 100;
    private static final int MIN_HOTSPOT_COMMITS = 2;
    private static final long MAX_UNTRACKED_FILE_BYTES = 512 * 1024;
    private static final int BINARY_PROBE_BYTES = 8 * 1024;

    private final Project project;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile SmartLensReportDto lastReport;

    public SmartLensAnalysisService(Project project) {
        this.project = project;
    }

    public static SmartLensAnalysisService getInstance(@NotNull Project project) {
        return project.getService(SmartLensAnalysisService.class);
    }

    public Optional<SmartLensReportDto> getLastReport() {
        return Optional.ofNullable(lastReport);
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Runs the analysis in a cancellable background task and publishes the result on {@link SmartLensReportListener#TOPIC}.
     */
    public void runInBackground() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        new Task.Backgroundable(project, "SmartLens: analyzing changes", true) {
            private SmartLensReportDto report;

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                report = analyze(progressIndicator);
            }

            @Override
            public void onSuccess() {
                lastReport = report;
                project.getMessageBus().syncPublisher(SmartLensReportListener.TOPIC).reportUpdated(report);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                LOG.warn("SmartLens analysis failed", error);
                SmartLensNotifier.notify(project, "Analysis failed: " + error.getMessage(), NotificationType.ERROR);
            }

            @Override
            public void onFinished() {
                running.set(false);
            }
        }.queue();
    }

    /**
     * Builds the report; runs git commands, so it must be called from a background thread.
     *
     * @param progressIndicator progress of the enclosing task
     * @return the report
     */
    public SmartLensReportDto analyze(ProgressIndicator progressIndicator) {
        SmartLensState settings = SmartLensState.getInstance();
        GitHistoryService gitHistoryService = GitHistoryService.getInstance(project);
        AttributionService attributionService = AttributionService.getInstance(project);
        AgentMatcher agentMatcher = AgentMatcher.fromCommaSeparated(settings.agentPatterns);
        LearningAnalyzer learningAnalyzer = new LearningAnalyzer(new FeatureExtractor(settings.codeExtensionSet()));

        List<GitRepository> repositories = gitHistoryService.getRepositories();
        if (repositories.isEmpty()) {
            throw new IllegalStateException("No Git repository found in this project.");
        }

        Set<String> developerEmails = new HashSet<>(settings.developerAliasSet());
        List<CommitInfoDto> history = new ArrayList<>();
        List<FileChangeDto> pendingChanges = new ArrayList<>();
        Map<String, Long> headTimeByRoot = new HashMap<>();

        // Collect the raw data from every repository of the project
        for (GitRepository repository : repositories) {
            progressIndicator.checkCanceled();
            String root = repository.getRoot().getPath();
            progressIndicator.setText("Reading Git history of " + repository.getRoot().getName());
            try {
                gitHistoryService.currentUserEmail(repository)
                        .ifPresent(email -> developerEmails.add(email.toLowerCase(Locale.ROOT)));
                history.addAll(gitHistoryService.loadHistory(repository, settings.historyDepth));
                pendingChanges.addAll(gitHistoryService.loadWorkingTreeChanges(repository));
                pendingChanges.addAll(readUntrackedFiles(gitHistoryService.loadUntrackedFiles(repository)));
                headTimeByRoot.put(root, gitHistoryService.headCommitEpochSeconds(repository));
            } catch (VcsException e) {
                LOG.warn("Cannot read Git data of " + root, e);
            }
        }
        progressIndicator.checkCanceled();
        // The hook log is written by agents but is SmartLens data, not a change to review
        hookLogFile(settings).ifPresent(hookLog -> pendingChanges.removeIf(change -> change.path().equals(hookLog)));

        // Attribution of pending changes: listeners + agent hook log
        progressIndicator.setText("Attributing pending changes");
        Set<String> hookReportedPaths = hookReportedPaths(settings, headTimeByRoot);
        Set<String> pendingPaths = new HashSet<>();
        pendingChanges.forEach(change -> pendingPaths.add(change.path()));
        attributionService.retainOnly(pendingPaths);

        Map<String, ChangeOrigin> pendingOrigins = new HashMap<>();
        for (FileChangeDto change : pendingChanges) {
            pendingOrigins.put(change.path(), attributionService.origin(change.path(), hookReportedPaths.contains(change.path())));
        }

        // History: per-file stats and the two vocabularies (developer vs. agent)
        progressIndicator.setText("Analyzing history");
        Map<String, FileHistoryStats> statsByPath = new HashMap<>();
        int agentCommits = 0;
        for (CommitInfoDto commit : history) {
            progressIndicator.checkCanceled();
            boolean agentCommit = agentMatcher.isAgentCommit(commit);
            boolean developerCommit = !agentCommit && developerEmails.contains(commit.authorEmail().toLowerCase(Locale.ROOT));
            if (agentCommit) {
                agentCommits++;
            }
            for (FileChangeDto change : commit.files()) {
                statsByPath.computeIfAbsent(change.path(), FileHistoryStats::new)
                        .record(commit, change, agentCommit, developerCommit);
                if (developerCommit) {
                    learningAnalyzer.addDeveloperLines(change.path(), change.addedLines());
                } else if (agentCommit) {
                    learningAnalyzer.addAgentLines(change.path(), change.addedLines(), "commit " + commit.shortHash());
                }
            }
        }
        for (FileChangeDto change : pendingChanges) {
            ChangeOrigin origin = pendingOrigins.get(change.path());
            if (origin.involvesAgent()) {
                learningAnalyzer.addAgentLines(change.path(), change.addedLines(), "uncommitted");
            } else if (origin == ChangeOrigin.DEVELOPER) {
                learningAnalyzer.addDeveloperLines(change.path(), change.addedLines());
            }
        }

        return new SmartLensReportDto(
                Set.copyOf(developerEmails),
                history.size(),
                agentCommits,
                learningAnalyzer.novelItems(MAX_LEARNING_ITEMS),
                reviewItems(pendingChanges, pendingOrigins, statsByPath),
                hotspots(statsByPath, pendingOrigins),
                Instant.now());
    }

    private List<ReviewItemDto> reviewItems(List<FileChangeDto> pendingChanges,
                                            Map<String, ChangeOrigin> pendingOrigins,
                                            Map<String, FileHistoryStats> statsByPath) {
        return pendingChanges.stream()
                .map(change -> {
                    ChangeOrigin origin = pendingOrigins.get(change.path());
                    // A file without history is fully known by the developer only if they wrote it
                    double familiarity = Optional.ofNullable(statsByPath.get(change.path()))
                            .map(FileHistoryStats::familiarity)
                            .orElse(origin == ChangeOrigin.DEVELOPER ? 1.0 : 0.0);
                    double priority = ReviewPrioritizer.priority(origin, change.linesAdded(), change.linesDeleted(), familiarity);
                    return new ReviewItemDto(change.path(), origin, change.linesAdded(), change.linesDeleted(), familiarity, priority);
                })
                .sorted(Comparator.comparingDouble(ReviewItemDto::priority).reversed())
                .toList();
    }

    private List<HotspotDto> hotspots(Map<String, FileHistoryStats> statsByPath, Map<String, ChangeOrigin> pendingOrigins) {
        List<FileHistoryStats> candidates = statsByPath.values().stream()
                .filter(stats -> stats.getCommits() >= MIN_HOTSPOT_COMMITS)
                .filter(stats -> RiskScorer.isRiskRelevant(stats.getPath()))
                .filter(stats -> Files.isRegularFile(Path.of(stats.getPath())))
                .toList();
        int maxCommits = candidates.stream().mapToInt(FileHistoryStats::getCommits).max().orElse(0);
        return candidates.stream()
                .map(stats -> new HotspotDto(
                        stats.getPath(),
                        stats.getCommits(),
                        stats.getAuthors(),
                        stats.agentShare(),
                        stats.familiarity(),
                        RiskScorer.risk(stats, maxCommits),
                        pendingOrigins.get(stats.getPath())))
                .sorted(Comparator.comparingDouble(HotspotDto::risk).reversed())
                .limit(MAX_HOTSPOTS)
                .toList();
    }

    private Set<String> hookReportedPaths(SmartLensState settings, Map<String, Long> headTimeByRoot) {
        String basePath = project.getBasePath();
        if (basePath == null || settings.hookLogPath.isBlank()) {
            return Set.of();
        }
        Path base = Path.of(basePath);
        Set<String> paths = new HashSet<>();
        for (AgentHookEntryDto entry : HookLogReader.read(base.resolve(settings.hookLogPath), base)) {
            // Only edits newer than the last commit of the entry's repository concern pending changes
            long headTime = headTimeByRoot.entrySet().stream()
                    .filter(root -> entry.path().startsWith(root.getKey() + "/"))
                    .mapToLong(Map.Entry::getValue)
                    .findFirst()
                    .orElse(0L);
            if (entry.epochSeconds() == 0L || entry.epochSeconds() >= headTime) {
                paths.add(entry.path());
            }
        }
        return paths;
    }

    /**
     * New files are invisible to {@code git diff HEAD}; treat their whole content as added.
     */
    private static List<FileChangeDto> readUntrackedFiles(List<String> paths) {
        List<FileChangeDto> changes = new ArrayList<>();
        for (String path : paths) {
            Path file = Path.of(path);
            try {
                if (!Files.isRegularFile(file) || Files.size(file) > MAX_UNTRACKED_FILE_BYTES) {
                    continue;
                }
                byte[] content = Files.readAllBytes(file);
                if (isBinary(content)) {
                    continue;
                }
                changes.add(new FileChangeDto(path, new String(content, StandardCharsets.UTF_8).lines().toList(), 0));
            } catch (IOException e) {
                LOG.debug("Cannot read untracked file " + path, e);
            }
        }
        return changes;
    }

    private static boolean isBinary(byte[] content) {
        int checked = Math.min(content.length, BINARY_PROBE_BYTES);
        for (int i = 0; i < checked; i++) {
            if (content[i] == 0) {
                return true;
            }
        }
        return false;
    }

    private Optional<String> hookLogFile(SmartLensState settings) {
        String basePath = project.getBasePath();
        if (basePath == null || settings.hookLogPath.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Path.of(basePath).resolve(settings.hookLogPath).normalize().toString().replace('\\', '/'));
    }

    /**
     * Forgets every attribution collected so far (e.g. to start a clean experiment session).
     */
    public void resetAttribution() {
        AttributionService.getInstance(project).clear();
        ApplicationManager.getApplication().invokeLater(this::runInBackground, project.getDisposed());
    }
}
