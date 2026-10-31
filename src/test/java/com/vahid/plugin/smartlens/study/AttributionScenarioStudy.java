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
package com.vahid.plugin.smartlens.study;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.util.TimeoutUtil;
import com.vahid.plugin.smartlens.SmartLensRepositoryTestCase;
import com.vahid.plugin.smartlens.dto.ReviewItemDto;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scripted attribution scenarios in a real headless IDE, including known weak spots. Each scenario touches its own
 * file; the observed origin is compared to the ground truth and written to {@code build/study/attribution-*.md}.
 * Run with {@code ./gradlew test --tests '*AttributionScenario*' -Psmartlens.study.attribution=true}.
 */
public class AttributionScenarioStudy extends SmartLensRepositoryTestCase {
    private static final long AFTER_RELOAD_WINDOW_MILLIS = 4_000;
    private final Map<String, Scenario> scenarios = new LinkedHashMap<>();

    private record Scenario(String file, String description, String expected) {
    }

    @Override
    protected int quietSeconds() {
        return 5;
    }

    protected String variant() {
        return "default";
    }

    @Override
    protected void createRepository() throws Exception {
        git("init", "-q", "-b", "master");
        git("config", "user.email", "dev@example.com");
        git("config", "user.name", "Dev");
        git("config", "commit.gpgsign", "false");
        write(".gitignore", ".idea/\n*.iml\n*.ipr\n*.iws\nbuild/\n");
        for (int i = 1; i <= 12; i++) {
            write("C" + i + ".java", "public class C" + i + " {\n}\n");
        }
        git("add", "-A");
        git("commit", "-q", "-m", "initial");
        git("checkout", "-q", "-b", "feature");
        write("C12.java", "public class C12 {\n    int onFeatureBranch;\n}\n");
        git("commit", "-q", "-am", "feature work");
        git("checkout", "-q", "master");
    }

    public void test_scenarios() throws Exception {
        scenario("C1.java", "Agent edits a file that is not open", "AGENT",
                () -> agentEdit("C1.java"));
        scenario("C2.java", "Agent edits a file open in the editor (IDE reloads it)", "AGENT", () -> {
            assertNotNull(FileDocumentManager.getInstance().getDocument(root.findChild("C2.java")));
            agentEdit("C2.java");
        });
        scenario("New.java", "Agent creates a new file", "AGENT",
                () -> agentEdit("New.java"));
        scenario("C3.java", "Developer types in the editor", "DEVELOPER",
                () -> typeAtEnd("C3.java", "// dev\n"));
        scenario("C4.java", "Developer edits, then agent edits", "MIXED", () -> {
            typeAtEnd("C4.java", "// dev\n");
            agentEdit("C4.java");
        });
        scenario("C5.java", "Agent edits, developer edits 4 s later", "MIXED", () -> {
            agentEdit("C5.java");
            TimeoutUtil.sleep(AFTER_RELOAD_WINDOW_MILLIS);
            typeAtEnd("C5.java", "// dev\n");
        });
        scenario("C6.java", "Agent edits, developer edits immediately (<1 s)", "MIXED", () -> {
            agentEdit("C6.java");
            typeAtEnd("C6.java", "// dev\n");
        });
        scenario("C7.java", "In-IDE assistant edits through the editor, no hook", "AGENT",
                () -> typeAtEnd("C7.java", "// assistant\n"));
        scenario("C8.java", "In-IDE assistant edits through the editor, with hook entry", "AGENT", () -> {
            typeAtEnd("C8.java", "// assistant\n");
            appendHookEntry("C8.java");
        });
        scenario("C9.java", "Developer runs an external formatter", "DEVELOPER",
                () -> agentEdit("C9.java"));
        scenario("build/Gen.java", "Agent edits a git-ignored file", "not reported",
                () -> agentEdit("build/Gen.java"));
        // Git operations last: the quiet period must not hide the agent edits above
        TimeoutUtil.sleep(quietSeconds() * 1_000L + 1_000);
        scenario("C12.java", "Git checkout changes a file, developer edits it 4 s later", "DEVELOPER", () -> {
            git("checkout", "-q", "feature");
            refreshFromDisk();
            TimeoutUtil.sleep(AFTER_RELOAD_WINDOW_MILLIS);
            typeAtEnd("C12.java", "// dev\n");
        });

        SmartLensReportDto report = analyzeViaBackgroundTask();
        Map<String, String> observed = report.reviewItems().stream().collect(Collectors.toMap(
                item -> base.relativize(Path.of(item.path())).toString().replace('\\', '/'),
                item -> item.origin().name(),
                (a, b) -> a));

        StringBuilder table = new StringBuilder("| Scenario | Expected | Observed (" + variant() + ") | Correct |\n|---|---|---|---|\n");
        int correct = 0;
        for (Scenario scenario : scenarios.values()) {
            String actual = observed.getOrDefault(scenario.file(), "not reported");
            boolean ok = actual.equals(scenario.expected());
            correct += ok ? 1 : 0;
            table.append(String.format("| %s | %s | %s | %s |%n", scenario.description(), scenario.expected(), actual, ok ? "yes" : "no"));
        }
        table.append(String.format("%n%d of %d scenarios correct.%n", correct, scenarios.size()));
        Path output = Path.of(System.getProperty("user.dir"), "build/study/attribution-" + variant() + ".md");
        Files.createDirectories(output.getParent());
        Files.writeString(output, table.toString(), StandardCharsets.UTF_8);
        System.out.println(table);
    }

    private void scenario(String file, String description, String expected, ThrowingRunnable action) throws Exception {
        scenarios.put(file, new Scenario(file, description, expected));
        action.run();
    }

    private void agentEdit(String relativePath) throws Exception {
        Path path = base.resolve(relativePath);
        String previous = Files.exists(path) ? Files.readString(path) : "";
        writeOnDisk(relativePath, previous + "// changed outside the IDE\n");
        refreshFromDisk();
    }

    private void appendHookEntry(String relativePath) throws Exception {
        Path log = base.resolve(".smartlens/agent-edits.jsonl");
        String previous = Files.exists(log) ? Files.readString(log) : "";
        writeOnDisk(".smartlens/agent-edits.jsonl", previous
                + "{\"file\": \"" + relativePath + "\", \"agent\": \"assistant\", \"ts\": " + (System.currentTimeMillis() / 1000) + "}\n");
        refreshFromDisk();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
