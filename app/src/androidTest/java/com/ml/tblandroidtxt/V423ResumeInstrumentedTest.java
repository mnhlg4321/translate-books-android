package com.ml.tblandroidtxt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.widget.Button;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
    static final String TAG = "v423x-";

    static String rawText(String marker) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 30; i++) sb.append("【").append(marker).append("】第").append(i).append("段。再開試験のための十分に長い日本語の段落であり、境界を越えて分割されることを確認します。登場人物は花子です。\n\n");
        return sb.append(System.nanoTime()).toString();
    }

    static OpenAICompatibleClient.ChatResult response(int n) {
        OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
        r.content = "<TRANSLATION>Bản dịch thử nghiệm số " + n + " với đủ độ dài để vượt qua kiểm định của ứng dụng.</TRANSLATION>";
        r.promptTokens = 110; r.completionTokens = 30; r.totalTokens = 140; r.usageReported = true; r.providerResponseId = "fx"; r.finishReason = "stop";
        return r;
    }

    static void observe(OpenAICompatibleClient.NetworkObserver o) { o.onRequestBodyStarted(); o.onRequestBodySent(100); o.onResponseHeaders(200); }

    static AppSettings settings() {
        AppSettings s = new AppSettings();
        s.apiKey = "fixture-key"; s.provider = "fake"; s.model = "fake/model"; s.maxTokensPerChunk = 80; s.maxAttempts = 1; s.refineAfter = false; s.stopOnCostLimit = false;
        return AppValidator.normalize(s);
    }

    static File outDir(Context c) { File d = new File(c.getCacheDir(), "v423-resume-out"); d.mkdirs(); return d; }

    static LibraryRawStore.Item importRaw(Context c, String name) throws Exception {
        RawImportPlanner.Plan plan = RawImportPlanner.plan(Collections.singletonList(new RawImportPlanner.Candidate(name, rawText(name), "UTF-8")), LibraryRawStore.hashes(c));
        return LibraryRawStore.add(c, plan).get(0);
    }

    static PreparedBatch prepare(Context c, Uri raw, AppSettings s) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<PreparedBatch> out = new AtomicReference<>();
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(raw);
        PreparationCoordinator.get(c).request(uris, s, batch -> { if (batch.ready() || batch.status == PreparedBatch.Status.ERROR) { out.set(batch); ready.countDown(); } });
        assertTrue(ready.await(60, TimeUnit.SECONDS));
        assertTrue(String.valueOf(out.get().error), out.get().ready());
        return out.get();
    }

    static void startNew(Context c, PreparedBatch batch, AppSettings s, File output) {
        Intent i = new Intent(c, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_START);
        i.putExtra("outputUri", Uri.fromFile(output).toString());
        i.putExtra("outputTreeUri", "");
        i.putExtra("settings", SettingsStore.toJson(s));
        i.putExtra("preparedBatchId", batch.id);
        i.putExtra("startSessionId", java.util.UUID.randomUUID().toString());
        c.startForegroundService(i);
    }

    static TranslationRepository.JobSummary job(Context c, String rawUri) {
        TranslationRepository repo = new TranslationRepository(c);
        try { for (TranslationRepository.JobSummary j : repo.getJobSummaries(100)) if (rawUri.equals(j.inputUri)) return j; }
        finally { repo.close(); }
        return null;
    }

    static void waitFor(java.util.concurrent.Callable<Boolean> cond, long ms, String what) throws Exception {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) { if (cond.call()) return; Thread.sleep(250L); }
        throw new AssertionError("timed out: " + what);
    }

    static boolean idle() { return !TranslatorService.isActive(); }

    static int[] counts(Context c, long jobId) {
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

    static void cleanup(Context c, LibraryRawStore.Item raw) {
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

    private static OpenAICompatibleClient.ChatResult costed(int n) {
        OpenAICompatibleClient.ChatResult r = response(n);
        r.providerCostReported = true; r.providerCost = 0.01;
        return r;
    }

    static void sendResume(Context c, long jobId) {
        Intent i = new Intent(c, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_RESUME_JOB);
        i.putExtra("jobId", jobId);
        c.startForegroundService(i);
    }

    /** T2: the cost limit stops the job part-way (paused, chunks left); "Tiếp tục dịch" keeps going until everything is translated. */
    @Test
    public void t2_costLimitPausesAndContinueFinishesTheJob() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LibraryRawStore.Item raw = importRaw(c, "008_" + TAG + "cost.txt");
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> { observe(observer); return costed(calls.incrementAndGet()); });
        try {
            AppSettings s = settings();
            s.stopOnCostLimit = true; s.costLimitUsd = 0.035;
            s = AppValidator.normalize(s);
            PreparedBatch batch = prepare(c, Uri.parse(raw.uri()), s);
            int total = batch.exactChunkCount();
            startNew(c, batch, s, new File(outDir(c), "cost-out.txt"));
            waitFor(() -> job(c, raw.uri()) != null && idle(), 90_000L, "first run stopped at the cost limit");
            long jobId = job(c, raw.uri()).id;
            int[] first = counts(c, jobId);
            assertTrue("some chunks are done, some are left (" + first[0] + "/" + first[1] + "/" + first[2] + ")", first[0] >= 3 && first[0] < total);
            assertEquals("paused", String.valueOf(job(c, raw.uri()).status).toLowerCase());
            int previous = first[0];
            for (int round = 0; round < 12; round++) {
                sendResume(c, jobId);
                int prev = previous;
                waitFor(() -> idle() && counts(c, jobId)[0] > prev || counts(c, jobId)[0] == total, 90_000L, "resume round " + round);
                waitFor(TranslatorServiceIdle::now, 30_000L, "idle");
                previous = counts(c, jobId)[0];
                if (previous == total) break;
            }
            assertEquals(total, counts(c, jobId)[0]);
            assertTrue(String.valueOf(job(c, raw.uri()).status).toUpperCase().startsWith("COMPLETED"));
        } finally { cleanup(c, raw); }
    }

    /** T3: an unfinished job is found by its input hash; continuing it creates no new job and sends only the missing chunks; "Dịch lại" starts a new job. */
    @Test
    public void t3_startWithAnUnfinishedJobContinuesItOrRestarts() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LibraryRawStore.Item raw = importRaw(c, "009_" + TAG + "start.txt");
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger failAt = new AtomicInteger(2);
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            int n = calls.incrementAndGet();
            if (n == failAt.get()) throw new ApiHttpException(401, 0, "Unauthorized: invalid credentials");
            observe(observer);
            return response(n);
        });
        try {
            AppSettings s = settings();
            PreparedBatch batch = prepare(c, Uri.parse(raw.uri()), s);
            int total = batch.exactChunkCount();
            startNew(c, batch, s, new File(outDir(c), "start-out.txt"));
            waitFor(() -> job(c, raw.uri()) != null && idle() && counts(c, job(c, raw.uri()).id)[1] >= 1, 90_000L, "first run stopped");
            long jobId = job(c, raw.uri()).id;

            TranslationRepository repo = new TranslationRepository(c);
            List<TranslationRepository.JobProgress> partial;
            try { partial = repo.getIncompleteJobs(Collections.singletonList(batch.inputs.get(0).inputHash), 10); } finally { repo.close(); }
            assertEquals(1, partial.size());
            assertEquals(jobId, partial.get(0).id);
            assertEquals(total, partial.get(0).total);
            assertEquals(1, partial.get(0).done);
            assertEquals(1, partial.get(0).error);
            assertEquals(total - 2, partial.get(0).pending);
            assertEquals(Collections.singletonMap(0, jobId), ResumePrompt.chooseJobs(Collections.singletonList(0), Collections.singletonList(batch.inputs.get(0).inputHash), partial));

            // "Tiếp tục job cũ": same job, only chunks 2.. are sent.
            calls.set(0); failAt.set(-1);
            Intent cont = startIntent(c, batch, s, new File(outDir(c), "start-out.txt"));
            cont.putExtra("resumeJobs", ResumePrompt.toJson(Collections.singletonMap(0, jobId)));
            c.startForegroundService(cont);
            waitFor(() -> idle() && calls.get() > 0, 120_000L, "continued run finished");
            Thread.sleep(500L);
            assertEquals("no new job was created", jobId, job(c, raw.uri()).id);
            assertEquals(1, jobsFor(c, raw.uri()));
            assertEquals("only the missing chunks were sent", total - 1, calls.get());
            assertEquals(total, counts(c, jobId)[0]);
            assertTrue(String.valueOf(job(c, raw.uri()).status).toUpperCase().startsWith("COMPLETED"));

            // "Dịch lại từ đầu": the old behaviour, a second job.
            calls.set(0);
            c.startForegroundService(startIntent(c, batch, s, new File(outDir(c), "start-out2.txt")));
            waitFor(() -> idle() && calls.get() > 0 && jobsFor(c, raw.uri()) == 2, 120_000L, "restart created a new job");
            assertEquals(total, calls.get());
        } finally { cleanup(c, raw); }
    }

    private static Intent startIntent(Context c, PreparedBatch batch, AppSettings s, File output) {
        Intent i = new Intent(c, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_START);
        i.putExtra("outputUri", Uri.fromFile(output).toString());
        i.putExtra("outputTreeUri", "");
        i.putExtra("settings", SettingsStore.toJson(s));
        i.putExtra("preparedBatchId", batch.id);
        i.putExtra("startSessionId", java.util.UUID.randomUUID().toString());
        return i;
    }

    private static int jobsFor(Context c, String rawUri) {
        TranslationRepository repo = new TranslationRepository(c);
        try { int n = 0; for (TranslationRepository.JobSummary j : repo.getJobSummaries(200)) if (rawUri.equals(j.inputUri)) n++; return n; }
        finally { repo.close(); }
    }

    /** T4: a combination run stopped inside chapter 2; starting again continues that job, chapter 3 is new, chapter 1 stays as it was. */
    @Test
    public void t4_combinationContinuesTheStoppedChapterAndStartsTheNext() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LibraryRawStore.Item r1 = importRaw(c, "001_" + TAG + "c1.txt"), r2 = importRaw(c, "002_" + TAG + "c2.txt"), r3 = importRaw(c, "003_" + TAG + "c3.txt");
        AtomicInteger calls = new AtomicInteger();
        java.util.concurrent.atomic.AtomicBoolean failTwo = new java.util.concurrent.atomic.AtomicBoolean(true);
        AtomicInteger twoCalls = new AtomicInteger();
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            String all = prompt.system + "\n" + prompt.user;
            boolean chapterTwo = all.contains("【" + r2.originalName + "】");
            calls.incrementAndGet();
            if (chapterTwo && failTwo.get() && twoCalls.incrementAndGet() == 2) throw new ApiHttpException(401, 0, "Unauthorized: invalid credentials");
            observe(observer);
            return response(calls.get());
        });
        try {
            AppSettings s = settings();
            // Run 1: chapters 1-3 as one combination; chapter 2 stops after its first chunk and the queue stops with it.
            runChapters(c, s, Arrays.asList(r1, r2, r3), null, () -> { });
            long j1 = job(c, r1.uri()).id;
            assertTrue(String.valueOf(job(c, r1.uri()).status).toUpperCase().startsWith("COMPLETED"));
            assertEquals("chapter 3 never started", null, job(c, r3.uri()));
            long j2 = job(c, r2.uri()).id;
            int[] two = counts(c, j2);
            assertEquals(1, two[0]);
            failTwo.set(false);
            calls.set(0);

            // Run 2: the owner starts the combination again with chapters 2 and 3 ticked (chapter 1 is finished); chapter 2 continues.
            runChapters(c, s, Arrays.asList(r2, r3), Collections.singletonMap(0, j2), () -> { });
            assertEquals("chapter 1 was not translated again", j1, job(c, r1.uri()).id);
            assertEquals(1, jobsFor(c, r1.uri()));
            assertEquals("chapter 2 continued its own job", 1, jobsFor(c, r2.uri()));
            assertEquals(j2, job(c, r2.uri()).id);
            assertTrue(String.valueOf(job(c, r2.uri()).status).toUpperCase().startsWith("COMPLETED"));
            assertTrue("chapter 3 ran as a new job", String.valueOf(job(c, r3.uri()).status).toUpperCase().startsWith("COMPLETED"));
        } finally {
            cleanup(c, r1); cleanup(c, r2); cleanup(c, r3);
        }
    }

    /** Runs the chosen RAWs as a chapter-set run (no Glossary/Pronoun) and waits until the service is idle. */
    private static void runChapters(Context c, AppSettings s, List<LibraryRawStore.Item> raws, java.util.Map<Integer, Long> resume, Runnable after) throws Exception {
        ArrayList<Uri> uris = new ArrayList<>();
        ArrayList<ChapterBinding> bindings = new ArrayList<>();
        for (LibraryRawStore.Item r : raws) { uris.add(Uri.parse(r.uri())); bindings.add(ChapterBinding.of(r.id, r.originalName, r.chapterNo, null, null)); }
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<PreparedBatch> out = new AtomicReference<>();
        PreparationCoordinator.get(c).request(uris, s, bindings, batch -> { if (batch.ready() || batch.status == PreparedBatch.Status.ERROR) { out.set(batch); ready.countDown(); } });
        assertTrue(ready.await(60, TimeUnit.SECONDS));
        PreparedBatch batch = out.get();
        assertTrue(String.valueOf(batch.error), batch.ready());
        String sid = java.util.UUID.randomUUID().toString();
        ChapterSession session = new ChapterSession(sid);
        session.chapters.addAll(bindings);
        session.write(new File(c.getFilesDir(), ChapterSession.DIR));
        Intent i = new Intent(c, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_START);
        i.putExtra("outputUri", "");
        i.putExtra("outputTreeUri", "content://tbl.test/tree/v423x");
        i.putExtra("settings", SettingsStore.toJson(s));
        i.putExtra("preparedBatchId", batch.id);
        i.putExtra("startSessionId", sid);
        i.putExtra("chapterMode", true);
        if (resume != null && !resume.isEmpty()) i.putExtra("resumeJobs", ResumePrompt.toJson(resume));
        c.startForegroundService(i);
        Thread.sleep(1500L);
        waitFor(() -> !TranslatorService.isActive(), 180_000L, "chapter run finished");
        Thread.sleep(500L);
        after.run();
    }

    /** T5: a chunk is split after a context error and the first piece then fails; Retry and the rest of the job complete with every chunk in order. */
    @Test
    public void t5_splitChunkThenRetryCompletesInOrder() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LibraryRawStore.Item raw = importRaw(c, "010_" + TAG + "split.txt");
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            int n = calls.incrementAndGet();
            if (n == 2) throw new ApiHttpException(413, 0, "Request too large for this model: context length exceeded");
            if (n == 3) throw new ApiHttpException(401, 0, "Unauthorized: invalid credentials");
            observe(observer);
            return response(n);
        });
        try {
            AppSettings s = settings();
            PreparedBatch batch = prepare(c, Uri.parse(raw.uri()), s);
            int total = batch.exactChunkCount();
            startNew(c, batch, s, new File(outDir(c), "split-out.txt"));
            waitFor(() -> job(c, raw.uri()) != null && idle() && counts(c, job(c, raw.uri()).id)[1] >= 1, 90_000L, "stopped on the split chunk");
            long jobId = job(c, raw.uri()).id;
            int[] before = counts(c, jobId);
            assertEquals("chunk 2 was replaced by more chunks", total + 1, before[0] + before[1] + before[2]);

            Intent retry = new Intent(c, TranslatorService.class);
            retry.setAction(TranslatorService.ACTION_RETRY_JOB_FAILED);
            retry.putExtra("jobId", jobId);
            c.startForegroundService(retry);
            waitFor(() -> idle() && calls.get() > 3, 120_000L, "retry finished");
            Thread.sleep(500L);
            int[] after = counts(c, jobId);
            assertEquals(total + 1, after[0]);
            assertTrue(String.valueOf(job(c, raw.uri()).status).toUpperCase().startsWith("COMPLETED"));
            TranslationRepository repo = new TranslationRepository(c);
            try {
                List<TranslationRepository.ChunkRow> rows = repo.getChunkRows(jobId);
                for (int k = 0; k < rows.size(); k++) assertEquals("chunk order", k, rows.get(k).index);
            } finally { repo.close(); }
        } finally { cleanup(c, raw); }
    }

    /** T6: "Resume" while nothing runs says so (status info) and never looks like a pause or a running job. */
    @Test
    public void t6_resumeWithoutARunningJobAnnouncesIt() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        List<String> statuses = Collections.synchronizedList(new ArrayList<String>());
        List<String> logs = Collections.synchronizedList(new ArrayList<String>());
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                statuses.add(String.valueOf(intent.getStringExtra(TranslatorService.EXTRA_STATUS)));
                logs.add(String.valueOf(intent.getStringExtra(TranslatorService.EXTRA_LOG)));
            }
        };
        c.registerReceiver(receiver, new IntentFilter(TranslatorService.ACTION_PROGRESS), Context.RECEIVER_NOT_EXPORTED);
        try {
            Intent i = new Intent(c, TranslatorService.class);
            i.setAction(TranslatorService.ACTION_RESUME);
            c.startService(i);
            waitFor(() -> !statuses.isEmpty(), 10_000L, "a message is announced");
            assertEquals("info", statuses.get(0));
            assertFalse(statuses.contains("paused"));
            assertFalse(statuses.contains("running"));
            assertTrue(logs.get(0), logs.get(0).contains("Không có job nào đang tạm dừng"));
            assertFalse(TranslatorService.isActive());
        } finally { c.unregisterReceiver(receiver); }
    }

    static void media(String stage) throws InterruptedException {
        if (!"run".equals(InstrumentationRegistry.getArguments().getString("v423_media", ""))) return;
        Thread.sleep(700L);
        android.util.Log.i("V423MEDIA", "stage=" + stage);
        Thread.sleep(5_000L);
    }

    static boolean containsText(android.view.View view, String text) {
        if (view instanceof android.widget.TextView && ((android.widget.TextView) view).getText().toString().contains(text)) return true;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) view;
            for (int k = 0; k < g.getChildCount(); k++) if (containsText(g.getChildAt(k), text)) return true;
        }
        return false;
    }

    /** Screens: the "Job đang dở" card after a stop, the question on Start, and the Translate screen after Retry has finished the job. */
    @Test
    public void u_screens_pendingCardStartQuestionAndStateAfterRetry() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Emulator data only: older unfinished jobs left by other test classes would hide this job in the card.
        TranslationRepository cleaner = new TranslationRepository(c);
        try { for (TranslationRepository.JobProgress stale : cleaner.getIncompleteJobs(null, 200)) cleaner.deleteJob(stale.id); } finally { cleaner.close(); }
        LibraryRawStore.Item raw = importRaw(c, "011_" + TAG + "screens.txt");
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            int n = calls.incrementAndGet();
            if (n == 3) throw new ApiHttpException(401, 0, "Unauthorized: invalid credentials");
            observe(observer);
            return response(n);
        });
        try (androidx.test.core.app.ActivityScenario<MainActivity> scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity.class)) {
            AppSettings s = settings();
            PreparedBatch batch = prepare(c, Uri.parse(raw.uri()), s);
            int total = batch.exactChunkCount();
            startNew(c, batch, s, new File(outDir(c), "screens-out.txt"));
            waitFor(() -> job(c, raw.uri()) != null && idle() && counts(c, job(c, raw.uri()).id)[1] >= 1, 90_000L, "stopped on a failed chunk");
            long jobId = job(c, raw.uri()).id;
            // A stale job whose request has unknown delivery and whose updated_at is the newest must not hide this job in the card.
            TranslationRepository seed = new TranslationRepository(c);
            long staleId;
            try {
                staleId = seed.createJob("content://fake/stale-unknown", "content://fake/stale-out", "stale-unknown.txt", settings(),
                        Collections.singletonList(new Chunk(0, 0, 10, "", "古い未確定の原文です。", "", "")), "stale-unknown-input", "stale-unknown-settings");
                seed.markDeliveryUnknown(staleId, 0, 1, "SENT", "simulated loss");
                seed.touchJob(staleId, "NEEDS_REVIEW");
            } finally { seed.close(); }
            scenario.onActivity(a -> { a.translationActive = false; a.switchTab("Translate"); a.refreshPendingJobCard(); });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a -> {
                assertTrue(containsText(a.getWindow().getDecorView(), "Job đang dở"));
                assertTrue(containsText(a.getWindow().getDecorView(), "chunk xong"));
                assertTrue(containsText(a.getWindow().getDecorView(), "Tiếp tục dịch"));
                assertTrue("the card shows the job that can continue", containsText(a.pendingJobCard, raw.originalName));
                assertFalse("not the stale unknown-delivery job", containsText(a.pendingJobCard, "stale-unknown.txt"));
                a.findScrollView(a.getWindow().getDecorView()).post(() -> { View card = a.pendingJobCard; a.findScrollView(a.getWindow().getDecorView()).scrollTo(0, Math.max(0, card.getTop() - 200)); });
            });
            media("38-pending-job-card");
            TranslationRepository repo = new TranslationRepository(c);
            List<TranslationRepository.JobProgress> partial;
            try { partial = repo.getIncompleteJobs(Collections.singletonList(batch.inputs.get(0).inputHash), 1); } finally { repo.close(); }
            scenario.onActivity(a -> a.askResumeOrRestart(partial, () -> { }, () -> { }));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            media("39-start-question");
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);

            Intent retry = new Intent(c, TranslatorService.class);
            retry.setAction(TranslatorService.ACTION_RETRY_JOB_FAILED);
            retry.putExtra("jobId", jobId);
            c.startForegroundService(retry);
            waitFor(() -> idle() && counts(c, jobId)[0] == total, 120_000L, "retry continued to the end");
            Thread.sleep(1500L);
            scenario.onActivity(a -> { a.switchTab("Translate"); a.refreshPendingJobCard(); });
            media("40-state-after-retry");
            scenario.onActivity(a -> assertFalse("the card no longer shows the finished job", containsText(a.pendingJobCard, raw.originalName)));
        } finally {
            TranslationRepository del = new TranslationRepository(c);
            try { for (TranslationRepository.JobProgress s2 : del.getIncompleteJobs(null, 200)) if ("stale-unknown.txt".equals(s2.fileName)) del.deleteJob(s2.id); } finally { del.close(); }
            cleanup(c, raw);
        }
    }

    /** The action buttons wrap their label instead of clipping it: a fixed height cut "Retry failed chunks" on a phone with a large font. */
    @Test
    public void v_actionButtonsGrowWithTheirLabel() throws Exception {
        try (androidx.test.core.app.ActivityScenario<MainActivity> scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                a.switchTab("Translate");
                for (Button b : new Button[]{a.startButton, a.pauseButton, a.resumeButton, a.cancelButton, a.retryButton}) {
                    assertEquals(b.getText().toString(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT, b.getLayoutParams().height);
                }
            });
        }
    }

    private static final class TranslatorServiceIdle {
        static boolean now() { return !TranslatorService.isActive(); }
    }
}
