package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * v4.23 L1 on the emulator: a library RAW ({@code tblraw://}) goes through the whole Translate service with a fake provider
 * (USD 0). {@code -e v423_phase seed|verify} runs the force-stop check: seed translates chunk 1 and leaves the process while
 * chunk 2 is not yet sent; after the shell force-stops the app, verify resumes the job from the same RAW and must not send chunk 1 again.
 */
@RunWith(AndroidJUnit4.class)
public class V423TranslateRawInstrumentedTest {
    private static final String MARKER = "v423-translate-marker.txt";
    private static final String TAG = "v423t-";

    static String rawText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 12; i++) sb.append("第").append(i).append("段。これは翻訳試験のための十分に長い日本語の段落であり、境界を越えて分割されることを確認します。登場人物は花子と太郎です。\n\n");
        return sb.toString();
    }

    static AppSettings settings() {
        AppSettings s = new AppSettings();
        s.apiKey = "fixture-key"; s.provider = "fake"; s.model = "fake/model";
        s.maxTokensPerChunk = 80; s.maxAttempts = 1; s.refineAfter = false; s.stopOnCostLimit = false;
        return AppValidator.normalize(s);
    }

    static OpenAICompatibleClient.ChatResult response(int number) {
        OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
        r.content = "<TRANSLATION>Bản dịch thử nghiệm số " + number + " với đủ độ dài để vượt qua kiểm định của ứng dụng.</TRANSLATION>";
        r.promptTokens = 110; r.completionTokens = 30; r.totalTokens = 140; r.usageReported = true;
        r.providerResponseId = "fixture-" + number; r.finishReason = "stop";
        return r;
    }

    private static void observe(OpenAICompatibleClient.NetworkObserver o) {
        o.onRequestBodyStarted(); o.onRequestBodySent(100); o.onResponseHeaders(200);
    }

    private static LibraryRawStore.Item importRaw(Context c, String name, String text) throws Exception {
        RawImportPlanner.Plan plan = RawImportPlanner.plan(java.util.Collections.singletonList(new RawImportPlanner.Candidate(name, text, "UTF-8")), LibraryRawStore.hashes(c));
        assertEquals(1, plan.added());
        return LibraryRawStore.add(c, plan).get(0);
    }

    private static PreparedBatch prepare(Context c, Uri raw, AppSettings s) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<PreparedBatch> out = new AtomicReference<>();
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(raw);
        PreparationCoordinator.get(c).request(uris, s, batch -> { if (batch.ready() || batch.status == PreparedBatch.Status.ERROR) { out.set(batch); ready.countDown(); } });
        assertTrue("plan prepared in time", ready.await(60, TimeUnit.SECONDS));
        assertTrue(String.valueOf(out.get().error), out.get().ready());
        return out.get();
    }

    private static void startService(Context c, PreparedBatch batch, AppSettings s, File output) {
        Intent i = new Intent(c, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_START);
        i.putExtra("outputUri", Uri.fromFile(output).toString());
        i.putExtra("outputTreeUri", "");
        i.putExtra("settings", SettingsStore.toJson(s));
        i.putExtra("preparedBatchId", batch.id);
        i.putExtra("startSessionId", java.util.UUID.randomUUID().toString());
        c.startForegroundService(i);
    }

    private static TranslationRepository.JobSummary jobFor(Context c, String rawUri) {
        TranslationRepository repo = new TranslationRepository(c);
        try {
            for (TranslationRepository.JobSummary j : repo.getJobSummaries(50)) if (rawUri.equals(j.inputUri)) return j;
        } finally { repo.close(); }
        return null;
    }

    private static boolean waitDone(Context c, String rawUri, long timeoutMs) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            TranslationRepository.JobSummary j = jobFor(c, rawUri);
            if (j != null && j.status != null && j.status.toUpperCase().startsWith("COMPLETED")) return true;
            Thread.sleep(300L);
        }
        return false;
    }

    private static File outDir(Context c) {
        File dir = new File(c.getCacheDir(), "v423-out");
        dir.mkdirs();
        return dir;
    }

    private static void cleanRaw(Context c, LibraryRawStore.Item raw, long jobId) {
        if (jobId > 0) { TranslationRepository repo = new TranslationRepository(c); repo.deleteJob(jobId); repo.close(); }
        if (raw != null) { LibraryRawStore.removeRows(c, java.util.Collections.singletonList(raw.id)); LibraryRawStore.purgeFiles(c, java.util.Collections.singletonList(raw.id)); }
    }

    @Test
    public void internalRawTranslatesEndToEnd() throws Exception {
        Assume.assumeTrue("full".equals(InstrumentationRegistry.getArguments().getString("v423_phase", "full")));
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LibraryRawStore.Item raw = importRaw(c, "005_" + TAG + "full.txt", rawText() + System.nanoTime());
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> { observe(observer); return response(calls.incrementAndGet()); });
        long jobId = -1;
        try {
            AppSettings s = settings();
            PreparedBatch batch = prepare(c, Uri.parse(raw.uri()), s);
            assertTrue("several chunks", batch.exactChunkCount() >= 3);
            assertEquals("plan input is the library RAW", raw.uri(), batch.inputs.get(0).uri.toString());
            assertEquals(raw.inputHash, batch.inputs.get(0).inputHash);
            assertEquals(raw.originalName, batch.inputs.get(0).displayName);
            File out = new File(outDir(c), "full-out.txt");
            startService(c, batch, s, out);
            assertTrue("job completed", waitDone(c, raw.uri(), 120_000L));
            TranslationRepository.JobSummary job = jobFor(c, raw.uri());
            jobId = job.id;
            assertEquals(batch.exactChunkCount(), calls.get());
            assertEquals(raw.originalName, job.fileName);
            assertTrue(out.length() > 0);
            String text = new String(readAll(out), StandardCharsets.UTF_8);
            assertTrue(text.contains("Bản dịch thử nghiệm số 1 "));
            assertTrue(text.contains("số " + batch.exactChunkCount() + " "));
        } finally {
            TranslateTestSeam.clear();
            cleanRaw(c, raw, jobId);
        }
    }

    @Test
    public void restartSeed() throws Exception {
        Assume.assumeTrue("seed".equals(InstrumentationRegistry.getArguments().getString("v423_phase", "")));
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LibraryRawStore.Item raw = importRaw(c, "006_" + TAG + "restart.txt", rawText() + System.nanoTime());
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            int n = calls.incrementAndGet();
            if (n >= 2) { try { Thread.sleep(300_000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } throw new java.io.IOException("process is being killed"); }
            observe(observer);
            return response(n);
        });
        AppSettings s = settings();
        PreparedBatch batch = prepare(c, Uri.parse(raw.uri()), s);
        File out = new File(outDir(c), "restart-out.txt");
        startService(c, batch, s, out);
        long end = System.currentTimeMillis() + 90_000L;
        long jobId = -1;
        int completed = 0;
        while (System.currentTimeMillis() < end && completed < 1) {
            TranslationRepository.JobSummary j = jobFor(c, raw.uri());
            if (j != null) { jobId = j.id; completed = j.doneChunks; }
            Thread.sleep(200L);
        }
        assertEquals("chunk 1 done, chunk 2 not yet sent", 1, completed);
        assertEquals(2, calls.get());
        try (FileOutputStream o = new FileOutputStream(new File(c.getFilesDir(), MARKER))) {
            o.write((raw.id + "\n" + jobId + "\n" + batch.exactChunkCount() + "\n").getBytes(StandardCharsets.UTF_8));
        }
        // The shell force-stops the app now; nothing is cleaned up here on purpose.
    }

    @Test
    public void restartVerify() throws Exception {
        Assume.assumeTrue("verify".equals(InstrumentationRegistry.getArguments().getString("v423_phase", "")));
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File marker = new File(c.getFilesDir(), MARKER);
        assertTrue(marker.isFile());
        String[] parts = new String(readAll(marker), StandardCharsets.UTF_8).split("\n");
        String rawId = parts[0];
        long jobId = Long.parseLong(parts[1]);
        int chunkCount = Integer.parseInt(parts[2]);
        LibraryRawStore.Item raw = LibraryRawStore.find(c, rawId);
        assertNotNull("RAW survived the kill", raw);
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> { observe(observer); return response(100 + calls.incrementAndGet()); });
        try {
            Intent i = new Intent(c, TranslatorService.class);
            i.setAction(TranslatorService.ACTION_RESUME_JOB);
            i.putExtra("jobId", jobId);
            c.startForegroundService(i);
            assertTrue("resumed job completed", waitDone(c, raw.uri(), 120_000L));
            assertEquals("chunk 1 is not sent again", chunkCount - 1, calls.get());
            File out = new File(outDir(c), "restart-out.txt");
            String text = new String(readAll(out), StandardCharsets.UTF_8);
            assertTrue("chunk 1 kept from before the kill", text.contains("Bản dịch thử nghiệm số 1 "));
            assertTrue("later chunks translated after the restart", text.contains("số 101 "));
            assertFalse(text.isEmpty());
        } finally {
            TranslateTestSeam.clear();
            cleanRaw(c, raw, jobId);
            marker.delete();
            new File(outDir(c), "restart-out.txt").delete();
        }
    }

    private static byte[] readAll(File f) throws Exception {
        try (FileInputStream in = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return out.toByteArray();
        }
    }
}
