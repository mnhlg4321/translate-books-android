package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Assume;
import org.junit.Test;
import org.json.JSONObject;
import org.sqlite.SQLiteConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Q1 retry coverage. All committed text in this test is invented and QA_-marked. */
public class V425RetainedSourceRepairHintTest {
    private static final String QA_SOURCE = String.join("\n",
            "QA_彼は店へ行きます。",
            "QA_町は静かです。",
            "QA_彼女は笑います。",
            "QA_風は穏やかです。",
            "QA_鳥が飛びます。",
            "QA_空が青いです。");

    private static final String QA_REJECTED = String.join("\n",
            "QA_He goes to the store.",
            "QA_The town is quiet.",
            "QA_「テストの音です！」",
            "QA_The wind is gentle.",
            "QA_The bird flies.",
            "QA_The sky is blue.");

    private static final String QA_ACCEPTED = String.join("\n",
            "QA_He goes to the store.",
            "QA_The town is quiet.",
            "QA_A cheerful shout.",
            "QA_The wind is gentle.",
            "QA_The bird flies.",
            "QA_The sky is blue.");

    @Test public void rejectedRetainedJapaneseAddsOnlyUserMessageHintOnNextAttempt() throws Exception {
        AppSettings settings = new AppSettings();
        settings.maxAttempts = 2;
        settings.initialRetryDelayMs = 100;
        settings.maxRetryDelayMs = 100;
        settings.reasoningEffort = "off";
        Chunk chunk = new Chunk(0, 0, QA_SOURCE.length(), "", QA_SOURCE, "", "");
        List<PromptPair> calls = new ArrayList<>();
        List<String> events = new ArrayList<>();

        TranslationEngineCore engine = new TranslationEngineCore(null, (s, prompt, outputLimit, requestId, observer) -> {
            calls.add(prompt);
            return answer(calls.size() == 1 ? QA_REJECTED : QA_ACCEPTED);
        });
        engine.setSleeper(ignored -> {});

        String output = engine.translateWithRetry(chunk, "", settings, () -> false, null,
                new TranslationEngineCore.EventSink() {
                    @Override public void onEvent(String message) { events.add(message); }
                });

        assertEquals(QA_ACCEPTED, output);
        assertEquals(2, calls.size());
        PromptPair initial = PromptPlan.forTranslation(chunk, "", AppValidator.normalize(settings)).prompt;
        assertEquals(initial.system, calls.get(0).system);
        assertEquals(initial.user, calls.get(0).user);
        assertFalse(calls.get(0).user.contains("APP-GENERATED RETRY NOTE"));

        assertEquals(initial.system, calls.get(1).system);
        assertTrue(calls.get(1).user.startsWith("# APP-GENERATED RETRY NOTE\n"));
        assertTrue(calls.get(1).user.contains("Line 3: QA_「テストの音です！」"));
        assertTrue(calls.get(1).user.indexOf("APP-GENERATED RETRY NOTE")
                < calls.get(1).user.indexOf("# SOURCE"));
        String noteSection = calls.get(1).user.substring(0, calls.get(1).user.indexOf("# SOURCE"));
        assertFalse(noteSection.contains("QA_彼は店へ行きます。"));
        assertTrue(events.contains("repairHint=RETAINED_SOURCE_LINE lines=1"));
        for (String event : events) assertFalse("event contains QA fixture text", event.contains("QA_"));
    }

    @Test public void hintRequiresARejectedJapaneseSourceLineAndClipsLongExcerpt() {
        RetainedSourceRepairHint.Generated absent =
                RetainedSourceRepairHint.fromTranslation(QA_ACCEPTED, Collections.emptyList());
        assertTrue(absent == null);

        String longLine = "QA_" + "あ".repeat(120);
        RetainedSourceRepairHint.Generated generated =
                RetainedSourceRepairHint.fromTranslation(longLine, Collections.emptyList());
        assertNotNull(generated);
        assertEquals(1, generated.lineCount);
        assertTrue(generated.note.contains("Line 1: " + "QA_" + "あ".repeat(93) + "…"));
        assertTrue(generated.note.codePointCount(0, generated.note.length()) < 400);
    }

    /** Uses only the owner's read-only local copy; no book text is printed or committed. */
    @Test public void replayJob354RejectedResponsesFromLocalDatabase() throws Exception {
        String database = System.getenv("V425_PHONE_DB");
        Assume.assumeTrue(database != null && !database.trim().isEmpty());
        Path path = Paths.get(database);
        Assume.assumeTrue(Files.isRegularFile(path));

        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path, config.toProperties())) {
            String settingsJson;
            try (PreparedStatement statement = connection.prepareStatement("SELECT settings FROM jobs WHERE id=?")) {
                statement.setLong(1, 354L);
                try (ResultSet rows = statement.executeQuery()) {
                    assertTrue("job 354 settings are available", rows.next());
                    settingsJson = rows.getString(1);
                }
            }
            String source;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT source FROM chunks WHERE job_id=? AND idx=?")) {
                statement.setLong(1, 354L);
                statement.setInt(2, 0);
                try (ResultSet rows = statement.executeQuery()) {
                    assertTrue("job 354 chunk 0 source is available", rows.next());
                    source = rows.getString(1);
                }
            }
            AppSettings validationSettings = validationSettings(settingsJson);
            Chunk sourceChunk = new Chunk(0, 0, source.length(), "", source, "", "");
            List<String> keptTargets = PromptPlan.forTranslation(sourceChunk, "", validationSettings).glossaryTargets;

            int responseCount = 0;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT attempt, response_content, finish_reason, output_tokens, " +
                            "(SELECT max_output_tokens FROM chunk_metrics WHERE job_id=chunk_attempts.job_id " +
                            "AND chunk_idx=chunk_attempts.chunk_idx AND phase='translate' " +
                            "AND attempt=chunk_attempts.attempt LIMIT 1) FROM chunk_attempts " +
                            "WHERE job_id=? AND chunk_idx=? AND phase='translate' AND status='REJECTED' " +
                            "AND response_content IS NOT NULL ORDER BY attempt")) {
                statement.setLong(1, 354L);
                statement.setInt(2, 0);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        int attempt = rows.getInt(1);
                        String raw = rows.getString(2);
                        String finish = rows.getString(3);
                        int outputTokens = rows.getInt(4);
                        int outputLimit = rows.getInt(5);
                        assertFalse("job 354 attempt " + attempt + " has a recorded output limit", rows.wasNull());
                        String translated;
                        try {
                            translated = PromptBuilder.extractTranslationStrict(raw);
                        } catch (RuntimeException parseFailure) {
                            fail("job 354 attempt " + attempt + " response parse failed: " + parseFailure.getClass().getSimpleName());
                            return;
                        }
                        ResponseValidator.Result verdict = ResponseValidator.validate(raw, translated, source, "",
                                finish, outputTokens, outputLimit, keptTargets, true);
                        assertTrue("job 354 attempt " + attempt + " must replay RETAINED_SOURCE_LINE; verdict=" + verdict.summary(),
                                verdict.errors.contains("RETAINED_SOURCE_LINE"));
                        List<ResponseValidator.RetainedSourceLine> lines =
                                ResponseValidator.retainedSourceLineDetails(translated, keptTargets);
                        List<Integer> lineNumbers = new ArrayList<>();
                        for (ResponseValidator.RetainedSourceLine line : lines) lineNumbers.add(line.lineNumber);
                        // These are response line positions; attempt 2 has one fewer blank line than the other responses.
                        List<Integer> expectedResponseLines = attempt == 2
                                ? Arrays.asList(3, 19, 53) : Arrays.asList(3, 19, 55);
                        assertEquals("attempt " + attempt + " retained response lines", expectedResponseLines, lineNumbers);
                        assertEquals(3, verdict.retainedSourceLineCount);
                        responseCount++;
                    }
                }
            }
            assertEquals("three rejected local responses replayed", 3, responseCount);
        }
    }

    /** Extracts only settings needed for glossary-aware validation; credentials and endpoints are never loaded. */
    private static AppSettings validationSettings(String settingsJson) throws Exception {
        JSONObject saved = new JSONObject(settingsJson);
        AppSettings settings = new AppSettings();
        settings.sourceLanguage = saved.optString("sourceLanguage", settings.sourceLanguage);
        settings.targetLanguage = saved.optString("targetLanguage", settings.targetLanguage);
        settings.optimizationPreset = saved.optString("optimizationPreset", settings.optimizationPreset);
        settings.maxOutputTokens = saved.optInt("maxOutputTokens", settings.maxOutputTokens);
        settings.glossaryText = saved.optString("glossaryText", "");
        settings.pronounText = saved.optString("pronounText", "");
        settings.glossaryInjectLimit = saved.optInt("glossaryInjectLimit", settings.glossaryInjectLimit);
        settings.pronounInjectLimit = saved.optInt("pronounInjectLimit", settings.pronounInjectLimit);
        settings.languageProfile = saved.optString("languageProfile", settings.languageProfile);
        return AppValidator.normalize(settings);
    }

    private static OpenAICompatibleClient.ChatResult answer(String body) {
        OpenAICompatibleClient.ChatResult result = new OpenAICompatibleClient.ChatResult();
        result.content = "<TRANSLATION>\n" + body + "\n</TRANSLATION>";
        result.finishReason = "stop";
        result.completionTokens = 20;
        result.totalTokens = 20;
        return result;
    }
}
