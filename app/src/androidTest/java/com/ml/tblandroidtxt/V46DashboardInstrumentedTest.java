package com.ml.tblandroidtxt;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class V46DashboardInstrumentedTest {
    private static Chunk chunk(int index, String source) {
        return new Chunk(index, index * source.length(), (index + 1) * source.length(), "", source, "", "");
    }

    private static AppSettings settings() {
        AppSettings s = new AppSettings();
        s.apiKey = "fixture-key"; s.provider = "fake"; s.model = "fake/model";
        s.maxAttempts = 1; s.refineAfter = false;
        return AppValidator.normalize(s);
    }

    private static OpenAICompatibleClient.ChatResult response(int number) {
        OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
        r.content = "<TRANSLATION>Bản dịch thử nghiệm số " + number + " có đủ độ dài để kiểm định.</TRANSLATION>";
        r.promptTokens = 110; r.completionTokens = 30; r.totalTokens = 140;
        r.usageReported = true; r.providerCostReported = true; r.providerCost = 0.01;
        r.providerResponseId = "fixture-response-" + number; r.finishReason = "stop";
        return r;
    }

    private static List<Chunk> chunks() {
        return Arrays.asList(
                chunk(0, "第一の十分に長い原文段落を使って偽プロバイダーを検証します。"),
                chunk(1, "第二の十分に長い原文段落を使って偽プロバイダーを検証します。"),
                chunk(2, "第三の十分に長い原文段落を使って偽プロバイダーを検証します。"));
    }

    @Test public void seedPreparedDashboardForScreenshot() throws Exception {
        PreparedBatch batch = new PreparedBatch();
        batch.id = "v47-prepared-fixture";
        batch.status = PreparedBatch.Status.READY;
        PreparedBatch.Input input = new PreparedBatch.Input();
        input.uri = Uri.parse("content://fixture/prepared-input");
        input.displayName = "fixture-eleven-chunks.txt";
        for (int i = 0; i < 11; i++) input.chunks.add(chunk(i, "Prepared fixture source paragraph " + i));
        batch.inputs.add(input);
        batch.estimate = new CostEstimator.Estimate();
        batch.estimate.totalTokensHigh = 59500;
        batch.estimate.costHigh = 0.171;
        batch.estimate.pricingAvailable = true;
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> {
            PreparationCoordinator.get(activity).cancel();
            activity.inputUris.clear();
            activity.inputUris.add(input.uri);
            activity.inputUri = input.uri;
            activity.preparedBatch = batch;
            if (activity.inputFileLabel != null) activity.inputFileLabel.setText("📄 fixture-eleven-chunks.txt");
            activity.renderPreparedBatch(batch);
        });
        Thread.sleep(1000);
        scenario.onActivity(activity -> {
            activity.preparedBatch = batch;
            activity.renderPreparedBatch(batch);
        });
        Thread.sleep(7000);
        assertEquals("11 chunks\n≈ 59.5k estimated tokens\nEstimated cost ≈ $0.171", TranslationDashboardFormatter.prepared(batch.estimate, 11));
    }

    @Test public void seedRunningFakeProviderMultiChunkDashboard() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        TranslationRepository repo = new TranslationRepository(context); AppSettings s = settings();
        List<Chunk> plan = chunks();
        long job = repo.createJob("content://fixture/running-input", "content://fixture/running-output",
                "fake-provider-3-chunks.txt", s, plan, "v46-running-input", "v46-settings");
        AtomicInteger calls = new AtomicInteger();
        TranslationEngine engine = new TranslationEngine(context, repo, (settings, prompt, limit, id, observer) -> {
            observer.onRequestBodyStarted(); observer.onRequestBodySent(100); observer.onResponseHeaders(200);
            return response(calls.incrementAndGet());
        });
        String translated = engine.translateWithRetry(job, plan.get(0), "", s, () -> false, (r, x) -> {}, m -> {});
        repo.commitValidatedChunk(job, 0, translated, "", "FAKE_PROVIDER");
        assertEquals(1, calls.get()); assertEquals("COMPLETED", repo.getChunkRows(job).get(0).status);
        repo.close();

        RuntimeStateSnapshot snapshot = new RuntimeStateSnapshot();
        snapshot.jobState = "RUNNING"; snapshot.status = "running"; snapshot.phase = "Running";
        snapshot.fileName = "fake-provider-3-chunks.txt"; snapshot.progress = 33;
        snapshot.totalChunks = 3; snapshot.completed = 1; snapshot.failed = 0; snapshot.fallbacks = 0;
        snapshot.totalTokens = 140; snapshot.totalCost = 0.01; snapshot.providerUsageComplete = true;
        snapshot.estimatedTotalTokens = 600; snapshot.estimatedTotalCost = 0.05;
        snapshot.elapsedMs = 65000; snapshot.remainingMs = 120000; snapshot.currentChunk = 2; snapshot.activeJobId = job;
        snapshot.glossaryLocks = 4; snapshot.pronounLocks = 2; snapshot.lockChunk = 2; snapshot.lockPhase = "Translating";
        snapshot.preview = translated; snapshot.previewChunk = 1;
        RuntimeStateStore.save(context, snapshot);
        assertTrue(context.getSharedPreferences("runtime_state", Context.MODE_PRIVATE).edit().putBoolean("v46Fixture", true).commit());
        assertEquals("RUNNING", RuntimeStateStore.toIntent(context).getStringExtra(TranslatorService.EXTRA_STATE));
    }

    @Test public void seedCompletedFakeProviderMultiChunkDashboard() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        TranslationRepository repo = new TranslationRepository(context); AppSettings s = settings();
        List<Chunk> plan = chunks();
        long job = repo.createJob("content://fixture/completed-input", "content://fixture/completed-output",
                "fake-provider-3-chunks.txt", s, plan, "v46-completed-input", "v46-settings");
        AtomicInteger calls = new AtomicInteger();
        TranslationEngine engine = new TranslationEngine(context, repo, (settings, prompt, limit, id, observer) -> {
            observer.onRequestBodyStarted(); observer.onRequestBodySent(100); observer.onResponseHeaders(200);
            return response(calls.incrementAndGet());
        });
        String previous = "";
        for (Chunk chunk : plan) {
            String translated = engine.translateWithRetry(job, chunk, previous, s, () -> false, (r, x) -> {}, m -> {});
            repo.commitValidatedChunk(job, chunk.index, translated, "", "FAKE_PROVIDER");
            previous = translated;
        }
        assertEquals(3, calls.get());
        for (TranslationRepository.ChunkRow row : repo.getChunkRows(job)) assertEquals("COMPLETED", row.status);
        repo.close();

        RuntimeStateSnapshot snapshot = new RuntimeStateSnapshot();
        snapshot.jobState = "COMPLETED"; snapshot.status = "done"; snapshot.phase = "Completed";
        snapshot.fileName = "fake-provider-3-chunks.txt"; snapshot.progress = 100;
        snapshot.totalChunks = 3; snapshot.completed = 3; snapshot.failed = 0; snapshot.fallbacks = 0;
        snapshot.totalTokens = 420; snapshot.totalCost = 0.03; snapshot.providerUsageComplete = true;
        snapshot.estimatedTotalTokens = 600; snapshot.estimatedTotalCost = 0.05;
        snapshot.elapsedMs = 195000; snapshot.remainingMs = 0; snapshot.currentChunk = 3; snapshot.activeJobId = job;
        snapshot.glossaryLocks = 3; snapshot.pronounLocks = 1; snapshot.lockChunk = 3; snapshot.lockPhase = "Translating";
        snapshot.preview = previous; snapshot.previewChunk = 3;
        RuntimeStateStore.save(context, snapshot);
        assertTrue(context.getSharedPreferences("runtime_state", Context.MODE_PRIVATE).edit().putBoolean("v46Fixture", true).commit());
        assertEquals("COMPLETED", RuntimeStateStore.toIntent(context).getStringExtra(TranslatorService.EXTRA_STATE));
    }
}
