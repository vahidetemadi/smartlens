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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.vahid.plugin.smartlens.config.SmartLensState;
import com.vahid.plugin.smartlens.value.ChangeOrigin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of who edited each file since its last commit: the developer (typing in the IDE)
 * or an AI agent (file changed on disk from outside the IDE, or reported by an agent hook).
 * Stored in the workspace file, so it stays local to the developer.
 */
@State(
        name = "SmartLensAttribution",
        storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)}
)
@Service(Service.Level.PROJECT)
public final class AttributionService implements PersistentStateComponent<AttributionService.AttributionState> {
    private static final long EXTERNAL_RELOAD_WINDOW_MILLIS = 3_000L;

    private AttributionState state = new AttributionState();
    private final Map<String, Long> recentExternalChanges = new ConcurrentHashMap<>();
    private volatile long lastVcsOperationMillis;

    public static AttributionService getInstance(@NotNull Project project) {
        return project.getService(AttributionService.class);
    }

    public synchronized void recordAgentEdit(String path) {
        EditRecord record = state.records.computeIfAbsent(path, k -> new EditRecord());
        record.agentEdits++;
        record.lastAgentEditMillis = System.currentTimeMillis();
    }

    public synchronized void recordDeveloperEdit(String path) {
        EditRecord record = state.records.computeIfAbsent(path, k -> new EditRecord());
        record.developerEdits++;
        record.lastDeveloperEditMillis = System.currentTimeMillis();
    }

    /**
     * Remembers that the file is about to be reloaded from disk, so that the resulting
     * document change is not mistaken for developer typing.
     */
    public void markExternalChange(String path) {
        recentExternalChanges.put(path, System.currentTimeMillis());
    }

    public boolean isRecentlyExternallyChanged(String path) {
        Long changedAt = recentExternalChanges.get(path);
        if (changedAt == null) {
            return false;
        }
        if (System.currentTimeMillis() - changedAt > EXTERNAL_RELOAD_WINDOW_MILLIS) {
            recentExternalChanges.remove(path);
            return false;
        }
        return true;
    }

    /**
     * Opens a quiet period during which disk changes are attributed to Git (checkout, rebase, ...) and not to agents.
     */
    public void markVcsOperation() {
        lastVcsOperationMillis = System.currentTimeMillis();
    }

    public boolean isInVcsQuietPeriod() {
        long quietMillis = SmartLensState.getInstance().vcsQuietSeconds * 1_000L;
        return System.currentTimeMillis() - lastVcsOperationMillis < quietMillis;
    }

    /**
     * @param path        file path
     * @param hookReported whether an agent hook reported an edit of this file since the last commit
     * @return the origin of the file's pending changes
     */
    public synchronized ChangeOrigin origin(String path, boolean hookReported) {
        EditRecord record = state.records.get(path);
        int agentEdits = (record == null ? 0 : record.agentEdits) + (hookReported ? 1 : 0);
        int developerEdits = record == null ? 0 : record.developerEdits;
        return ChangeOrigin.classify(agentEdits, developerEdits);
    }

    /**
     * Drops the records of files that no longer have pending changes (committed or reverted).
     */
    public synchronized void retainOnly(Set<String> changedPaths) {
        state.records.keySet().retainAll(changedPaths);
    }

    public synchronized void clear() {
        state.records.clear();
        recentExternalChanges.clear();
    }

    @Override
    public synchronized @NotNull AttributionState getState() {
        return state;
    }

    @Override
    public synchronized void loadState(@NotNull AttributionState loadedState) {
        this.state = loadedState;
    }

    public static final class AttributionState {
        public Map<String, EditRecord> records = new HashMap<>();
    }

    public static final class EditRecord {
        public int agentEdits;
        public int developerEdits;
        public long lastAgentEditMillis;
        public long lastDeveloperEditMillis;
    }
}
