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

import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import com.intellij.ui.content.Content;
import com.vahid.plugin.smartlens.SmartLensRepositoryTestCase;
import com.vahid.plugin.smartlens.UI.SmartLensToolWindowFactory;
import com.vahid.plugin.smartlens.dto.SmartLensReportDto;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders the real SmartLens tool window (headless) for the paper figure: a clone of an open-source repository,
 * a short scripted agent session, the analysis, and one PNG per tab in {@code build/study/}.
 * Run with {@code ./gradlew test --tests '*ScreenshotStudy*' -Psmartlens.study.screenshotRepo=/path/to/clone}.
 */
public class ScreenshotStudy extends SmartLensRepositoryTestCase {
    private static final String IMPL = "src/main/java/com/github/gilbertotcc/lifx/impl/";
    private static final java.util.regex.Pattern BOT = java.util.regex.Pattern.compile("(?i)\\[bot]|\\bbot\\b|circleci|github-actions");
    private static final int WIDTH = 1000;
    private static final java.util.Map<String, Integer> HEIGHTS = java.util.Map.of("Overview", 390, "Learn", 262, "Understand", 100, "Risk", 228);
    private static final int SCALE = 2;

    @Override
    protected void createRepository() throws Exception {
        git("clone", "-q", System.getProperty("smartlens.study.screenshotRepo"), ".");
        // The developer persona is the most active human author of the clone (no e-mail addresses in the code)
        developerEmails = personaEmails();
        git("config", "user.email", developerEmails.get(0));
        write(".git/info/exclude", ".idea/\n*.iml\n*.ipr\n*.iws\n");
    }

    private java.util.List<String> developerEmails;

    /**
     * @return e-mails of the most active human author, most used first (the first one acts as git user.email)
     */
    private java.util.List<String> personaEmails() throws Exception {
        Process process = new ProcessBuilder("git", "log", "--no-merges", "--format=%an%x1f%ae")
                .directory(base.toFile()).redirectErrorStream(true).start();
        java.util.List<String[]> authors = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                .lines().map(line -> line.split("\u001F", 2)).filter(parts -> parts.length == 2)
                .filter(parts -> !BOT.matcher(parts[0] + " " + parts[1]).find())
                .toList();
        assertTrue("git log timed out", process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS));
        String persona = authors.stream()
                .collect(java.util.stream.Collectors.groupingBy(parts -> parts[0].toLowerCase(java.util.Locale.ROOT), java.util.stream.Collectors.counting()))
                .entrySet().stream().max(java.util.Map.Entry.comparingByValue()).orElseThrow().getKey();
        return authors.stream()
                .filter(parts -> parts[0].toLowerCase(java.util.Locale.ROOT).equals(persona))
                .collect(java.util.stream.Collectors.groupingBy(parts -> parts[1].toLowerCase(java.util.Locale.ROOT), java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .map(java.util.Map.Entry::getKey)
                .toList();
    }

    public void test_renderToolWindow() throws Exception {
        com.vahid.plugin.smartlens.config.SmartLensState settings = com.vahid.plugin.smartlens.config.SmartLensState.getInstance();
        String previousAliases = settings.developerAliases;
        settings.developerAliases = String.join(",", developerEmails.subList(1, developerEmails.size()));
        try {
            renderToolWindow();
        } finally {
            settings.developerAliases = previousAliases;
        }
    }

    private void renderToolWindow() throws Exception {
        // Scripted agent session (outside the IDE): a new helper and a change to an existing class
        writeOnDisk(IMPL + "RetryPolicy.java", """
                package com.github.gilbertotcc.lifx.impl;

                import java.time.Duration;
                import java.util.Optional;
                import java.util.concurrent.ThreadLocalRandom;

                public sealed interface RetryPolicy permits RetryPolicy.None, RetryPolicy.Exponential {

                    Optional<Duration> delayBeforeAttempt(int attempt);

                    record None() implements RetryPolicy {
                        public Optional<Duration> delayBeforeAttempt(int attempt) {
                            return Optional.empty();
                        }
                    }

                    record Exponential(Duration base, int maxAttempts) implements RetryPolicy {
                        public Optional<Duration> delayBeforeAttempt(int attempt) {
                            if (attempt >= maxAttempts) {
                                return Optional.empty();
                            }
                            long jitter = ThreadLocalRandom.current().nextLong(base.toMillis() + 1);
                            return Optional.of(base.multipliedBy(1L << attempt).plusMillis(jitter));
                        }
                    }

                    static String describe(RetryPolicy policy) {
                        return switch (policy) {
                            case None none -> "no retry";
                            case Exponential e -> "exponential, max " + e.maxAttempts();
                        };
                    }
                }
                """);
        insertBeforeLastBrace(IMPL + "CallExecutor.java", """

                    private static boolean isRetryable(Throwable error) {
                        return error instanceof java.io.IOException io && !(io instanceof java.io.FileNotFoundException);
                    }
                """);
        refreshFromDisk();
        // The developer adds a small change in the editor
        typeAtEnd("src/main/java/com/github/gilbertotcc/lifx/LifxClient.java", "\n");

        SmartLensReportDto report = analyzeViaBackgroundTask();
        assertFalse(report.reviewItems().isEmpty());

        ToolWindowHeadlessManagerImpl.MockToolWindow toolWindow = new ToolWindowHeadlessManagerImpl.MockToolWindow(getProject());
        new SmartLensToolWindowFactory().createToolWindowContent(getProject(), toolWindow);
        Path output = Files.createDirectories(Path.of(System.getProperty("user.dir"), "build/study"));
        for (Content content : toolWindow.getContentManager().getContents()) {
            // The toolbar only fills itself once shown on screen; render the content below it
            Component panel = content.getComponent() instanceof com.intellij.openapi.ui.SimpleToolWindowPanel toolWindowPanel
                    ? toolWindowPanel.getContent() : content.getComponent();
            BufferedImage image = render(panel, HEIGHTS.get(content.getDisplayName()));
            ImageIO.write(image, "png", output.resolve("fig-" + content.getDisplayName().toLowerCase() + ".png").toFile());
        }
    }

    private void insertBeforeLastBrace(String relativePath, String code) throws Exception {
        String text = Files.readString(base.resolve(relativePath));
        int lastBrace = text.lastIndexOf('}');
        writeOnDisk(relativePath, text.substring(0, lastBrace) + code + text.substring(lastBrace));
    }

    private static BufferedImage render(Component component, int height) {
        // Behave as if shown: creates the table header and lets the toolbar fill itself
        component.addNotify();
        component.setSize(WIDTH, height);
        layoutRecursively(component);
        com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        com.intellij.util.TimeoutUtil.sleep(500);
        com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        layoutRecursively(component);
        BufferedImage image = new BufferedImage(WIDTH * SCALE, height * SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.scale(SCALE, SCALE);
        graphics.setColor(component.getBackground() != null ? component.getBackground() : Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, height);
        component.printAll(graphics);
        graphics.dispose();
        return image;
    }

    private static void layoutRecursively(Component component) {
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }
}
