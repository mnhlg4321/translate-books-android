package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** End-to-end Q1 retry check using only invented QA_ content and the local fake provider seam. */
@RunWith(AndroidJUnit4.class)
public class V425RetainedSourceRepairInstrumentedTest {
    private static final String TAG = "QA_V425_REPAIR_";
    private static final String SOURCE_PREFIX = String.join("\n",
            "QA_彼は店へ行きます。",
            "QA_町は静かです。",
            "QA_彼女は笑います。",
            "QA_風は穏やかです。",
            "QA_鳥が飛びます。");

    @Test public void retainedJapaneseGetsAppHintAndNextFakeResponseCompletesJob() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String marker = "QA_MARK_" + System.nanoTime();
        String source = SOURCE_PREFIX + "\nQA_空が青いです。" + marker;
        String rejected = String.join("\n",
                "QA_He goes to the store.",
                "QA_The town is quiet.",
                "QA_「テストの音です！」",
                "QA_The wind is gentle.",
                "QA_The bird flies.",
                "QA_The sky is blue." + marker);
        String accepted = String.join("\n",
                "QA_He goes to the store.",
                "QA_The town is quiet.",
                "QA_A cheerful shout.",
                "QA_The wind is gentle.",
                "QA_The bird flies.",
                "QA_The sky is blue." + marker);

        LibraryRawStore.Item raw = null;
        long jobId = -1;
        File outputDir = new File(c.getCacheDir(), "v425-retained-source-repair");
        outputDir.mkdirs();
        List<PromptPair> prompts = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger calls = new AtomicInteger();
        try {
            RawImportPlanner.Plan plan = RawImportPlanner.plan(
                    Collections.singletonList(new RawImportPlanner.Candidate("025_" + TAG + marker + ".txt", source, "UTF-8")),
                    LibraryRawStore.hashes(c));
            List<LibraryRawStore.Item> added = LibraryRawStore.add(c, plan);
            assertEquals(1, added.size());
            raw = added.get(0);

            TranslateTestSeam.setOutputDir(outputDir);
            TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
                if (observer != null) {
                    observer.onRequestBodyStarted();
                    observer.onRequestBodySent(100);
                    observer.onResponseHeaders(200);
                }
                prompts.add(prompt);
                int attempt = calls.incrementAndGet();
                assertTrue("only the two planned fake responses are allowed", attempt <= 2);
                return response(attempt == 1 ? rejected : accepted, attempt);
            });

            AppSettings settings = new AppSettings();
            settings.apiKey = "fixture-key";
            settings.provider = "fake";
            settings.model = "fake/model";
            settings.maxAttempts = 2;
            settings.initialRetryDelayMs = 100;
            settings.maxRetryDelayMs = 100;
            settings.reasoningEffort = "off";
            settings.refineAfter = false;
            settings.stopOnCostLimit = false;
            settings.maxTokensPerChunk = 450;

            PreparedBatch batch = prepare(c, Uri.parse(raw.uri()), settings);
            assertEquals("fixture is a single translation chunk", 1, batch.exactChunkCount());
            File output = new File(outputDir, "repair-output.txt");
            Intent start = new Intent(c, TranslatorService.class);
            start.setAction(TranslatorService.ACTION_START);
            start.putExtra("outputUri", Uri.fromFile(output).toString());
            start.putExtra("outputTreeUri", "");
            start.putExtra("settings", SettingsStore.toJson(settings));
            start.putExtra("preparedBatchId", batch.id);
            start.putExtra("startSessionId", java.util.UUID.randomUUID().toString());
            c.startForegroundService(start);

            TranslationRepository.JobSummary job = waitForTerminal(c, raw.uri(), 120_000L);
            assertNotNull("translation job was created", job);
            jobId = job.id;
            assertTrue("job completes after the second fake response; status=" + job.status,
                    job.status != null && job.status.toUpperCase().startsWith("COMPLETED"));
            assertEquals("one retained-source rejection followed by one accepted response", 2, calls.get());
            assertEquals(2, prompts.size());
            assertFalse("first request is unchanged", prompts.get(0).user.contains("APP-GENERATED RETRY NOTE"));
            assertTrue("retry has the app-generated repair note", prompts.get(1).user.startsWith("# APP-GENERATED RETRY NOTE\n"));
            assertTrue(prompts.get(1).user.contains("Line 3: QA_「テストの音です！」"));
            assertEquals("system instruction is unchanged", prompts.get(0).system, prompts.get(1).system);
            String result = new String(readAll(output), StandardCharsets.UTF_8);
            assertTrue(result.contains("QA_A cheerful shout."));
            assertFalse(result.contains("QA_「テストの音です！」"));
        } finally {
            TranslateTestSeam.clear();
            PreparationCoordinator.get(c).cancel();
            if (raw != null) {
                if (jobId > 0) {
                    TranslationRepository repo = new TranslationRepository(c);
                    try { repo.deleteJob(jobId); } finally { repo.close(); }
                }
                LibraryRawStore.removeRows(c, Collections.singletonList(raw.id));
                LibraryRawStore.purgeFiles(c, Collections.singletonList(raw.id));
            }
            for (File file : files(outputDir)) file.delete();
        }
    }

    private static PreparedBatch prepare(Context c, Uri raw, AppSettings settings) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<PreparedBatch> result = new AtomicReference<>();
        PreparationCoordinator.get(c).request(Collections.singletonList(raw), settings, batch -> {
            if (batch.ready() || batch.status == PreparedBatch.Status.ERROR) {
                result.set(batch);
                ready.countDown();
            }
        });
        assertTrue("plan prepared in time", ready.await(60, TimeUnit.SECONDS));
        assertTrue(String.valueOf(result.get().error), result.get().ready());
        return result.get();
    }

    private static TranslationRepository.JobSummary waitForTerminal(Context c, String rawUri, long timeoutMs) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        TranslationRepository.JobSummary latest = null;
        while (System.currentTimeMillis() < end) {
            latest = jobFor(c, rawUri);
            if (latest != null && latest.status != null) {
                String status = latest.status.toUpperCase();
                if (status.startsWith("COMPLETED") || status.startsWith("FAILED") || status.startsWith("CANCELED")) return latest;
            }
            Thread.sleep(250L);
        }
        return latest;
    }

    private static TranslationRepository.JobSummary jobFor(Context c, String rawUri) {
        TranslationRepository repo = new TranslationRepository(c);
        try {
            for (TranslationRepository.JobSummary job : repo.getJobSummaries(50)) {
                if (rawUri.equals(job.inputUri)) return job;
            }
        } finally { repo.close(); }
        return null;
    }

    private static OpenAICompatibleClient.ChatResult response(String body, int number) {
        OpenAICompatibleClient.ChatResult result = new OpenAICompatibleClient.ChatResult();
        result.content = "<TRANSLATION>\n" + body + "\n</TRANSLATION>";
        result.promptTokens = 110;
        result.completionTokens = 30;
        result.totalTokens = 140;
        result.usageReported = true;
        result.providerResponseId = "qa-fixture-" + number;
        result.finishReason = "stop";
        return result;
    }

    private static byte[] readAll(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file)) {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) > 0) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static File[] files(File directory) {
        File[] files = directory.listFiles();
        return files == null ? new File[0] : files;
    }
}
