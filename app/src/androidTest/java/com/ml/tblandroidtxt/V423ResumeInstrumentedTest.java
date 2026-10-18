package com.ml.tblandroidtxt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
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

/**
 * v4.23 resume fix on the emulator with a fake provider (USD 0): a chunk fails, its retry succeeds, and the rest of the job must be
 * translated too instead of the job being marked finished with chunks still pending. T1 reproduces the owner's report.
 */
@RunWith(AndroidJUnit4.class)
public class V423ResumeInstrumentedTest {
    private static final String TAG = "v423x-";

    private static String rawText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 30; i++) sb.append("第").append(i).append("段。再開試験のための十分に長い日本語の段落であり、境界を越えて分割されることを確認します。登場人物は花子です。\n\n");
        return sb.append(System.nanoTime()).toString();
    }

    private static OpenAICompatibleClient.ChatResult response(int n) {
        OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
        r.content = "<TRANSLATION>Bản dịch thử nghiệm số " + n + " với đủ độ dài để vượt qua kiểm định của ứng dụng.</TRANSLATION>";
        r.promptTokens = 110; r.completionTokens = 30; r.totalTokens = 140; r.usageReported = true; r.providerResponseId = "fx"; r.finishReason = "stop";
        return r;
    }

    private static void observe(OpenAICompatibleClient.NetworkObserver o) { o.onRequestBodyStarted(); o.onRequestBodySent(100); o.onResponseHeaders(200); }

    private static AppSettings settings() {
        AppSettings s = new AppSettings();
        s.apiKey = "fixture-key"; s.provider = "fake"; s.model = "fake/model"; s.maxTokensPerChunk = 80; s.maxAttempts = 1; s.refineAfter = false; s.stopOnCostLimit = false;
        return AppValidator.normalize(s);
    }

    private static File outDir(Context c) { File d = new File(c.getCacheDir(), "v423-resume-out"); d.mkdirs(); return d; }

    private static LibraryRawStore.Item importRaw(Context c, String name) throws Exception {
        RawImportPlanner.Plan plan = RawImportPlanner.plan(Collections.singletonList(new RawImportPlanner.Candidate(name, rawText(), "UTF-8")), LibraryRawStore.hashes(c));
        return LibraryRawStore.add(c, plan).get(0);
    }

    private static PreparedBatch prepare(Context c, Uri raw, AppSettings s) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<PreparedBatch> out = new AtomicReference<>();
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(raw);
        PreparationCoordinator.get(c).request(uris, s, batch -> { if (batch.ready() || batch.status == PreparedBatch.Status.ERROR) { out.set(batch); ready.countDown(); } });
        assertTrue(ready.await(60, TimeUnit.SECONDS));
        assertTrue(String.valueOf(out.get().error), out.get().ready());
        return out.get();
    }

    private static void startNew(Context c, PreparedBatch batch, AppSettings s, File output) {
        Intent i = new Intent(c, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_START);
        i.putExtra("outputUri", Uri.fromFile(output).toString());
        i.putExtra("outputTreeUri", "");
        i.putExtra("settings", SettingsStore.toJson(s));
        i.putExtra("preparedBatchId", batch.id);
        i.putExtra("startSessionId", java.util.UUID.randomUUID().toString());
        c.startForegroundService(i);
    }

    private static TranslationRepository.JobSummary job(Context c, String rawUri) {
        TranslationRepository repo = new TranslationRepository(c);
        try { for (TranslationRepository.JobSummary j : repo.getJobSummaries(100)) if (rawUri.equals(j.inputUri)) return j; }
        finally { repo.close(); }
        return null;
    }

    private static void waitFor(java.util.concurrent.Callable<Boolean> cond, long ms, String what) throws Exception {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) { if (cond.call()) return; Thread.sleep(250L); }
        throw new AssertionError("timed out: " + what);
    }

    private static boolean idle() { return !TranslatorService.isActive(); }

    private static int[] counts(Context c, long jobId) {
        TranslationRepository repo = new TranslationRepository(c);
        try {
            int done = 0, error = 0, pending = 0;
            for (TranslationRepository.ChunkRow r : repo.getChunkRows(jobId)) {
                String st = String.valueOf(r.status).toUpperCase();
                if (st.equals("COMPLETED") || st.equals("DONE")) done++; else if (st.contains("ERROR")) error++; else pending++;
            }
            return new int[]{done, error, pending};
        } finally { repo.close(); }
    }

    private static void cleanup(Context c, LibraryRawStore.Item raw) {
        TranslateTestSeam.clear();
        TranslationRepository repo = new TranslationRepository(c);
        try { TranslationRepository.JobSummary j; while ((j = job(c, raw.uri())) != null) repo.deleteJob(j.id); } finally { repo.close(); }
        LibraryRawStore.removeRows(c, Collections.singletonList(raw.id));
        LibraryRawStore.purgeFiles(c, Collections.singletonList(raw.id));
        File[] outs = outDir(c).listFiles();
        if (outs != null) for (File o : outs) o.delete();
    }

    /** T1: chunk 2 fails (authentication), chunks 3.. stay pending; Retry must translate chunk 2 and everything after it, once each. */
    @Test
    public void t1_retryAfterAFailedChunkTranslatesTheRestOfTheJob() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LibraryRawStore.Item raw = importRaw(c, "007_" + TAG + "retry.txt");
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger failAt = new AtomicInteger(2);
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            int n = calls.incrementAndGet();
            if (n == failAt.get()) throw new ApiHttpException(401, 0, "Unauthorized: invalid credentials");
            observe(observer);
            return response(n);
        });
        List<String> doneSnapshots = Collections.synchronizedList(new ArrayList<String>());
        AtomicReference<Long> jobIdRef = new AtomicReference<>(-1L);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (!"done".equals(intent.getStringExtra(TranslatorService.EXTRA_STATUS))) return;
                long id = jobIdRef.get();
                if (id > 0) { int[] c3 = counts(context, id); doneSnapshots.add(c3[0] + "/" + c3[1] + "/" + c3[2]); }
            }
        };
        IntentFilter filter = new IntentFilter(TranslatorService.ACTION_PROGRESS);
        c.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        try {
            AppSettings s = settings();
            PreparedBatch batch = prepare(c, Uri.parse(raw.uri()), s);
            int total = batch.exactChunkCount();
            assertTrue("a long job", total >= 5);
            startNew(c, batch, s, new File(outDir(c), "retry-out.txt"));
            waitFor(() -> job(c, raw.uri()) != null && idle() && counts(c, job(c, raw.uri()).id)[1] >= 1, 90_000L, "first run stopped on the failed chunk");
            long jobId = job(c, raw.uri()).id;
            jobIdRef.set(jobId);
            int[] before = counts(c, jobId);
            assertEquals(1, before[0]);
            assertEquals(1, before[1]);
            assertEquals("chunks after the failed one are still pending", total - 2, before[2]);
            doneSnapshots.clear();

            Intent retry = new Intent(c, TranslatorService.class);
            retry.setAction(TranslatorService.ACTION_RETRY_JOB_FAILED);
            retry.putExtra("jobId", jobId);
            c.startForegroundService(retry);
            waitFor(() -> idle() && calls.get() > 2, 120_000L, "retry finished");
            Thread.sleep(500L);

            int[] after = counts(c, jobId);
            assertEquals("every chunk is translated after the retry (done/error/pending=" + after[0] + "/" + after[1] + "/" + after[2] + ")", total, after[0]);
            assertEquals("chunk 1 was not sent again: one failed call + one call per chunk", total + 1, calls.get());
            String status = String.valueOf(job(c, raw.uri()).status).toUpperCase();
            assertTrue("job status " + status, status.startsWith("COMPLETED"));
            for (String snap : doneSnapshots) assertTrue("'done' was announced while chunks were unfinished (" + snap + ")", snap.equals(total + "/0/0"));
            assertFalse("a 'done' event was sent", doneSnapshots.isEmpty());
        } finally {
            c.unregisterReceiver(receiver);
            cleanup(c, raw);
        }
    }
}
