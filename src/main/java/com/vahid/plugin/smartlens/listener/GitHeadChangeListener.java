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
