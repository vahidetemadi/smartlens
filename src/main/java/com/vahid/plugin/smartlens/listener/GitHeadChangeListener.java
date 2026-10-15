/*
 * Copyright (c) 2026 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartlens.listener;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.project.Project;
import com.vahid.plugin.smartlens.service.AttributionService;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opens a quiet period whenever HEAD moves or a Git operation is in progress, so that the
 * resulting disk changes are not attributed to an AI agent.
 */
public class GitHeadChangeListener implements GitRepositoryChangeListener {
    private final Project project;
    private final Map<String, String> lastRevisions = new ConcurrentHashMap<>();

    public GitHeadChangeListener(Project project) {
        this.project = project;
    }

    @Override
    public void repositoryChanged(@NotNull GitRepository repository) {
        if (project.isDisposed()) {
            return;
        }
        String root = repository.getRoot().getPath();
        String revision = Objects.requireNonNullElse(repository.getCurrentRevision(), "");
        String previous = lastRevisions.put(root, revision);

        boolean headMoved = previous != null && !previous.equals(revision);
        if (headMoved || repository.getState() != Repository.State.NORMAL) {
            AttributionService.getInstance(project).markVcsOperation();
        }
    }
}
