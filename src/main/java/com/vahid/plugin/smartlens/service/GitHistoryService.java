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

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.vahid.plugin.smartlens.analysis.GitLogParser;
import com.vahid.plugin.smartlens.dto.CommitInfoDto;
import com.vahid.plugin.smartlens.dto.FileChangeDto;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Thin layer over Git4Idea that runs the git commands SmartLens needs. Must not be called on the EDT.
 */
@Service(Service.Level.PROJECT)
public final class GitHistoryService {
    private final Project project;

    public GitHistoryService(Project project) {
        this.project = project;
    }

    public static GitHistoryService getInstance(@NotNull Project project) {
        return project.getService(GitHistoryService.class);
    }

    public List<GitRepository> getRepositories() {
        return GitRepositoryManager.getInstance(project).getRepositories();
    }

    /**
     * Loads the latest non-merge commits of the repository together with their added lines.
     *
     * @param repository git repository
     * @param depth      maximum number of commits
     * @return commits, newest first
     * @throws VcsException when git fails
     */
    public List<CommitInfoDto> loadHistory(GitRepository repository, int depth) throws VcsException {
        if (repository.isFresh()) {
            return List.of();
        }
        GitLineHandler handler = handler(repository, GitCommand.LOG);
        handler.addParameters("--no-merges", "--no-renames", "--no-color", "--no-ext-diff",
                "--max-count=" + depth, "-p", "--unified=0", "--format=" + GitLogParser.LOG_FORMAT);
        return GitLogParser.parseLog(run(handler), repository.getRoot().getPath());
    }

    /**
     * @return uncommitted changes of tracked files (staged and unstaged) compared to HEAD
     */
    public List<FileChangeDto> loadWorkingTreeChanges(GitRepository repository) throws VcsException {
        if (repository.isFresh()) {
            return List.of();
        }
        GitLineHandler handler = handler(repository, GitCommand.DIFF);
        handler.addParameters("HEAD", "--no-renames", "--no-color", "--no-ext-diff", "--unified=0");
        return GitLogParser.parseDiff(run(handler), repository.getRoot().getPath());
    }

    /**
     * @return absolute paths of untracked, non-ignored files (new files git diff cannot see)
     */
    public List<String> loadUntrackedFiles(GitRepository repository) throws VcsException {
        GitLineHandler handler = handler(repository, GitCommand.LS_FILES);
        handler.addParameters("--others", "--exclude-standard");
        String root = repository.getRoot().getPath();
        return run(handler).stream()
                .filter(line -> !line.isBlank())
                .map(line -> line.length() >= 2 && line.startsWith("\"") && line.endsWith("\"") ? line.substring(1, line.length() - 1) : line)
                .map(relativePath -> root + "/" + relativePath)
                .toList();
    }

    public long headCommitEpochSeconds(GitRepository repository) throws VcsException {
        if (repository.isFresh()) {
            return 0L;
        }
        GitLineHandler handler = handler(repository, GitCommand.LOG);
        handler.addParameters("-1", "--format=%ct", "HEAD");
        return run(handler).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .map(Long::parseLong)
                .orElse(0L);
    }

    public Optional<String> currentUserEmail(GitRepository repository) throws VcsException {
        return Optional.ofNullable(GitConfigUtil.getValue(project, repository.getRoot(), "user.email"))
                .map(String::trim)
                .filter(email -> !email.isEmpty());
    }

    private GitLineHandler handler(GitRepository repository, GitCommand command) {
        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), command);
        handler.setSilent(true);
        handler.setStdoutSuppressed(true);
        return handler;
    }

    private static List<String> run(GitLineHandler handler) throws VcsException {
        GitCommandResult result = Git.getInstance().runCommand(handler);
        if (!result.success()) {
            throw new VcsException(result.getErrorOutputAsJoinedString());
        }
        return result.getOutput();
    }
}
