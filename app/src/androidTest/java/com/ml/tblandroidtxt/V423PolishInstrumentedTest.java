package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * v4.23 polish on the emulator with a fake provider (USD 0): resume of a CRLF file on both paths, the one-time repair of wrongly
 * conflicted jobs, dismissing a job, the recovery that must not touch finished/cancelled/dismissed jobs, the hide rule for a leftover
 * job, the Jobs tab (totals and run cards), the Translate screen in chapter-set mode, nested scrolling and the Library notices.
 * Every test removes the jobs, files and library items it made.
 */
@RunWith(AndroidJUnit4.class)
public class V423PolishInstrumentedTest {
    private static final String TAG = V423ResumeInstrumentedTest.TAG + "polish-";

    private static Context ctx() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }

    private static String crlfText(String marker) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 30; i++) sb.append("【").append(marker).append("】第").append(i).append("段。再開試験のための十分に長い日本語の段落であり、境界を越えて分割されることを確認します。登場人物は花子です。\r\n\r\n");
        return sb.toString();
    }

    private static File writeFile(Context c, String name, String text) throws Exception {
        File f = new File(V423ResumeInstrumentedTest.outDir(c), name);
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private static TranslationRepository.JobSummary jobByUri(Context c, Uri uri) {
        return V423ResumeInstrumentedTest.job(c, uri.toString());
    }

    private static void deleteJobsFor(Context c, Uri... uris) {
        TranslationRepository repo = new TranslationRepository(c);
        try {
            for (Uri u : uris) { TranslationRepository.JobSummary j; while ((j = V423ResumeInstrumentedTest.job(c, u.toString())) != null) repo.deleteJob(j.id); }
        } finally { repo.close(); }
    }

    private static void startWithResume(Context c, PreparedBatch batch, AppSettings s, File output, long resumeJobId) {
        Intent i = new Intent(c, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_START);
        i.putExtra("outputUri", Uri.fromFile(output).toString());
        i.putExtra("outputTreeUri", "");
        i.putExtra("settings", SettingsStore.toJson(s));
        i.putExtra("preparedBatchId", batch.id);
        i.putExtra("startSessionId", java.util.UUID.randomUUID().toString());
        i.putExtra("resumeJobs", "{\"0\":" + resumeJobId + "}");
        c.startForegroundService(i);
    }

    /** Runs a CRLF file until chunk 2 fails and returns its job id, leaving the provider seam installed with {@code failing} switched off. */
    private static long stopAtFailedChunk(Context c, Uri uri, File out, AtomicInteger calls, AtomicInteger failAt) throws Exception {
        TranslateTestSeam.setOutputDir(V423ResumeInstrumentedTest.outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            int n = calls.incrementAndGet();
            if (n == failAt.get()) throw new ApiHttpException(401, 0, "Unauthorized: invalid credentials");
            V423ResumeInstrumentedTest.observe(observer);
            return V423ResumeInstrumentedTest.response(n);
        });
        AppSettings s = V423ResumeInstrumentedTest.settings();
        PreparedBatch batch = V423ResumeInstrumentedTest.prepare(c, uri, s);
        V423ResumeInstrumentedTest.startNew(c, batch, s, out);
        V423ResumeInstrumentedTest.waitFor(() -> jobByUri(c, uri) != null && V423ResumeInstrumentedTest.idle() && V423ResumeInstrumentedTest.counts(c, jobByUri(c, uri).id)[1] >= 1, 90_000L, "stopped on the failed chunk");
        return jobByUri(c, uri).id;
    }

    /** A file with Windows line endings resumes on both paths: "Tiếp tục job cũ" from Start (continueExistingJob) and the Resume button (resumeJobById). */
    @Test
    public void a_crlfFileResumesOnBothPaths() throws Exception {
        Context c = ctx();
        File one = writeFile(c, TAG + "crlf-resume-button-" + System.nanoTime() + ".txt", crlfText("A" + System.nanoTime()));
        File two = writeFile(c, TAG + "crlf-start-continue-" + System.nanoTime() + ".txt", crlfText("B" + System.nanoTime()));
        Uri u1 = Uri.fromFile(one), u2 = Uri.fromFile(two);
        try {
            // path 2: the Resume button (ACTION_RESUME_JOB)
            AtomicInteger calls = new AtomicInteger(), failAt = new AtomicInteger(2);
            long job1 = stopAtFailedChunk(c, u1, new File(V423ResumeInstrumentedTest.outDir(c), "crlf1-out.txt"), calls, failAt);
            failAt.set(-1);
            {
                TranslationRepository probe = new TranslationRepository(c);
                try {
                    String stored = probe.getInputHash(job1), text = FileUtil.readText(c, u1);
                    assertTrue("stored=" + stored + " raw=" + HashUtil.sha256(text) + " normalized=" + HashUtil.sha256(Chunker.normalizeSource(text)) + " chars=" + text.length() + " cr=" + text.indexOf('\r') + " status=" + probe.getJob(job1).status + " uri=" + probe.getJob(job1).inputUri + " expected=" + u1, InputIdentity.matches(stored, text));
                } finally { probe.close(); }
            }
            V423ResumeInstrumentedTest.sendResume(c, job1);
            V423ResumeInstrumentedTest.waitFor(() -> V423ResumeInstrumentedTest.idle() && String.valueOf(jobByUri(c, u1).status).toUpperCase().startsWith("COMPLETED"), 120_000L, "resume button finished the CRLF job");
            assertFalse("not marked as a conflict", "INPUT_CONFLICT".equalsIgnoreCase(jobByUri(c, u1).status));

            // path 1: Start with "Tiếp tục job cũ" (resumeJobs extra, continueExistingJob)
            calls.set(0); failAt.set(2);
            long job2 = stopAtFailedChunk(c, u2, new File(V423ResumeInstrumentedTest.outDir(c), "crlf2-out.txt"), calls, failAt);
            failAt.set(-1);
            AppSettings s = V423ResumeInstrumentedTest.settings();
            PreparedBatch batch = V423ResumeInstrumentedTest.prepare(c, u2, s);
            startWithResume(c, batch, s, new File(V423ResumeInstrumentedTest.outDir(c), "crlf2-out.txt"), job2);
            V423ResumeInstrumentedTest.waitFor(() -> V423ResumeInstrumentedTest.idle() && String.valueOf(jobByUri(c, u2).status).toUpperCase().startsWith("COMPLETED"), 120_000L, "start-continue finished the CRLF job");
            assertEquals("the same job was continued, no new job", job2, jobByUri(c, u2).id);
        } finally {
            TranslateTestSeam.clear();
            deleteJobsFor(c, u1, u2);
            one.delete(); two.delete();
        }
    }

    /** Jobs wrongly marked INPUT_CONFLICT by the line-ending mismatch come back as paused, once; a file that really changed stays in conflict. */
    @Test
    public void b_oneTimeRepairRestoresOnlyLineEndingConflicts() throws Exception {
        Context c = ctx();
        File same = writeFile(c, TAG + "repair-same-" + System.nanoTime() + ".txt", crlfText("C" + System.nanoTime()));
        File changed = writeFile(c, TAG + "repair-changed-" + System.nanoTime() + ".txt", crlfText("D" + System.nanoTime()));
        Uri us = Uri.fromFile(same), uc = Uri.fromFile(changed);
        android.content.SharedPreferences prefs = c.getSharedPreferences("input_conflict_repair", Context.MODE_PRIVATE);
        try {
            AtomicInteger calls = new AtomicInteger(), failAt = new AtomicInteger(2);
            long jobSame = stopAtFailedChunk(c, us, new File(V423ResumeInstrumentedTest.outDir(c), "rep1-out.txt"), calls, failAt);
            calls.set(0);
            long jobChanged = stopAtFailedChunk(c, uc, new File(V423ResumeInstrumentedTest.outDir(c), "rep2-out.txt"), calls, failAt);
            Files.write(changed.toPath(), (crlfText("D2") + "追加").getBytes(StandardCharsets.UTF_8));
            TranslationRepository repo = new TranslationRepository(c);
            try { repo.touchJob(jobSame, "INPUT_CONFLICT"); repo.touchJob(jobChanged, "INPUT_CONFLICT"); } finally { repo.close(); }

            prefs.edit().remove("done_v423_crlf").commit();
            int repaired = InputConflictRepair.runOnce(c);
            assertEquals(1, repaired);
            assertEquals("paused", jobByUri(c, us).status);
            assertEquals("a file that really changed stays in conflict", "INPUT_CONFLICT", jobByUri(c, uc).status);
            assertEquals("the repair runs once", -1, InputConflictRepair.runOnce(c));
        } finally {
            TranslateTestSeam.clear();
            prefs.edit().putBoolean("done_v423_crlf", true).commit();
            deleteJobsFor(c, us, uc);
            same.delete(); changed.delete();
        }
    }

    private static List<Chunk> chunks(int n) {
        ArrayList<Chunk> out = new ArrayList<>();
        for (int i = 0; i < n; i++) { String src = "古い再開試験の原文その" + i + "です。"; out.add(new Chunk(i, i * src.length(), (i + 1) * src.length(), "", src, "", "")); }
        return out;
    }

    private static long makeJob(TranslationRepository repo, String name, String hash, int chunkCount, int completed, String status) {
        long id = repo.createJob("content://fake/" + name, "content://fake/out-" + name, name, V423ResumeInstrumentedTest.settings(), chunks(chunkCount), hash, "polish-settings");
        for (int i = 0; i < completed; i++) repo.commitValidatedChunk(id, i, "bản dịch " + i, "", "FAKE_PROVIDER");
        repo.touchJob(id, status);
        return id;
    }

    private static String statusOf(TranslationRepository repo, long id) { return repo.getJob(id).status; }

    private static long updatedAt(TranslationRepository repo, long id) { return repo.getJobSummary(id).updatedAt; }

    /** The start-up recovery marks only unfinished jobs; dismissed, finished and cancelled ones keep their status and their time. */
    @Test
    public void c_recoveryDoesNotTouchDismissedFinishedOrCancelledJobs() throws Exception {
        Context c = ctx();
        TranslationRepository repo = new TranslationRepository(c);
        ArrayList<Long> made = new ArrayList<>();
        try {
            long dismissed = makeJob(repo, TAG + "rec-dismissed", "h-rec-1", 2, 0, "x");
            long finished = makeJob(repo, TAG + "rec-finished", "h-rec-2", 2, 0, "x");
            long cancelled = makeJob(repo, TAG + "rec-cancelled", "h-rec-3", 2, 0, "x");
            long paused = makeJob(repo, TAG + "rec-paused", "h-rec-4", 2, 0, "x");
            long review = makeJob(repo, TAG + "rec-review", "h-rec-5", 2, 0, "x");
            made.addAll(Arrays.asList(dismissed, finished, cancelled, paused, review));
            for (long id : made) repo.markDeliveryUnknown(id, 0, 1, "SEND_STARTED", "simulated loss");
            repo.dismissJob(dismissed);
            repo.touchJob(finished, "COMPLETED");
            repo.touchJob(cancelled, "cancelled");
            repo.touchJob(paused, "paused");
            repo.touchJob(review, "NEEDS_REVIEW");
            long tDismissed = updatedAt(repo, dismissed), tFinished = updatedAt(repo, finished), tCancelled = updatedAt(repo, cancelled), tReview = updatedAt(repo, review);
            Thread.sleep(30L);

            repo.recoverInterruptedState();

            assertEquals("dismissed", statusOf(repo, dismissed));
            assertEquals("COMPLETED", statusOf(repo, finished));
            assertEquals("cancelled", statusOf(repo, cancelled));
            assertEquals(tDismissed, updatedAt(repo, dismissed));
            assertEquals(tFinished, updatedAt(repo, finished));
            assertEquals(tCancelled, updatedAt(repo, cancelled));
            assertEquals("an unfinished job with an unknown delivery needs review", "NEEDS_REVIEW", statusOf(repo, paused));
            assertEquals("a job already in review is not touched again", tReview, updatedAt(repo, review));
        } finally {
            for (long id : made) repo.deleteJob(id);
            repo.close();
        }
    }

    /** A leftover job is hidden once a newer job of the same input is finished; an older finished job hides nothing. Nothing is written. */
    @Test
    public void d_aNewerFinishedJobHidesTheLeftover() throws Exception {
        Context c = ctx();
        TranslationRepository repo = new TranslationRepository(c);
        ArrayList<Long> made = new ArrayList<>();
        try {
            String same = "h-hide-" + System.nanoTime(), other = "h-hide-other-" + System.nanoTime(), older = "h-hide-old-" + System.nanoTime();
            long leftover = makeJob(repo, TAG + "hide-leftover", same, 3, 1, "paused");
            long keeps = makeJob(repo, TAG + "hide-keeps", other, 3, 1, "paused");
            long oldFinished = makeJob(repo, TAG + "hide-old-finished", older, 2, 2, "COMPLETED");
            long newerLeft = makeJob(repo, TAG + "hide-newer-left", older, 3, 1, "paused");
            made.addAll(Arrays.asList(leftover, keeps, oldFinished, newerLeft));
            assertEquals(1, repo.getIncompleteJobs(Collections.singletonList(same), 10).size());

            long newer = makeJob(repo, TAG + "hide-newer-done", same, 3, 3, "COMPLETED");
            made.add(newer);
            assertTrue("hidden", repo.getIncompleteJobs(Collections.singletonList(same), 10).isEmpty());
            assertEquals("not rewritten: the leftover keeps its status", "paused", statusOf(repo, leftover));
            assertEquals(1, repo.getIncompleteJobs(Collections.singletonList(other), 10).size());
            assertEquals("an older finished job hides nothing", 1, repo.getIncompleteJobs(Collections.singletonList(older), 10).size());

            repo.deleteJob(newer); made.remove(Long.valueOf(newer));
            assertEquals("the leftover is back once the newer job is gone", 1, repo.getIncompleteJobs(Collections.singletonList(same), 10).size());
        } finally {
            for (long id : made) repo.deleteJob(id);
            repo.close();
        }
    }

    private static void staleCleanup(Context c) {
        TranslationRepository cleaner = new TranslationRepository(c);
        try { for (TranslationRepository.JobProgress stale : cleaner.getIncompleteJobs(null, 200)) cleaner.deleteJob(stale.id); } finally { cleaner.close(); }
    }

    /** "Bỏ job này": the card goes away, nothing is deleted, and it does not come back after the service start-up recovery. */
    @Test
    public void e_dismissingAJobHidesItForGood() throws Exception {
        Context c = ctx();
        staleCleanup(c);
        TranslationRepository repo = new TranslationRepository(c);
        long job = makeJob(repo, TAG + "dismiss-card.txt", "h-dismiss-" + System.nanoTime(), 4, 1, "paused");
        repo.close();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> { a.translationActive = false; a.switchTab("Translate"); a.refreshPendingJobCard(); });
            V423ResumeInstrumentedTest.waitFor(() -> {
                AtomicReference<Boolean> shown = new AtomicReference<>(false);
                scenario.onActivity(a -> shown.set(a.pendingJobCard.getVisibility() == View.VISIBLE && V423ResumeInstrumentedTest.containsText(a.pendingJobCard, TAG + "dismiss-card.txt") && V423ResumeInstrumentedTest.containsText(a.pendingJobCard, "Bỏ job này")));
                return shown.get();
            }, 15_000L, "the card shows the job with a dismiss button");
            scrollPageTo(scenario, a -> a.pendingJobCard, 300);
            V423ResumeInstrumentedTest.media("43-pending-card-dismiss");

            scenario.onActivity(a -> a.dismissJobs(Collections.singletonList(job)));
            V423ResumeInstrumentedTest.waitFor(() -> {
                AtomicReference<Boolean> hidden = new AtomicReference<>(false);
                scenario.onActivity(a -> hidden.set(a.pendingJobCard.getVisibility() != View.VISIBLE));
                return hidden.get();
            }, 15_000L, "the card is gone after dismissing");

            TranslationRepository r2 = new TranslationRepository(c);
            try {
                assertEquals("dismissed", statusOf(r2, job));
                assertEquals("the translated chunk is kept", 1, V423ResumeInstrumentedTest.counts(c, job)[0]);
                r2.recoverInterruptedState();
                assertEquals("dismissed", statusOf(r2, job));
                assertTrue(r2.getIncompleteJobs(null, 50).isEmpty());
            } finally { r2.close(); }
            scenario.onActivity(a -> a.refreshPendingJobCard());
            Thread.sleep(800L);
            scenario.onActivity(a -> assertTrue("still hidden after a refresh", a.pendingJobCard.getVisibility() != View.VISIBLE));
        } finally {
            TranslationRepository r3 = new TranslationRepository(c);
            try { r3.deleteJob(job); } finally { r3.close(); }
        }
    }

    private static void insertMetric(TranslationRepository repo, long jobId, int idx, String costStatus, double cost, int in, int cached, int out) {
        ChunkMetric m = new ChunkMetric();
        m.jobId = jobId; m.chunkIndex = idx; m.phase = "translate"; m.attempt = 1; m.costStatus = costStatus; m.usageCost = cost;
        m.actualInputTokens = in; m.cachedInputTokens = cached; m.outputTokens = out; m.usageSource = "PROVIDER"; m.outcome = "success";
        repo.recordMetric(m);
    }

    private static File sessionFile(Context c, String sessionId, String setName, int chapters) throws Exception {
        File dir = new File(c.getFilesDir(), ChapterSession.DIR);
        dir.mkdirs();
        org.json.JSONArray rows = new org.json.JSONArray();
        for (int i = 0; i < chapters; i++) rows.put(new org.json.JSONObject());
        org.json.JSONObject root = new org.json.JSONObject().put("sessionId", sessionId).put("setId", "polish-set").put("setName", setName).put("createdAt", System.currentTimeMillis() - 60_000L).put("chapters", rows).put("resumedJobs", new org.json.JSONArray());
        File f = new File(dir, sessionId + ".json");
        Files.write(f.toPath(), root.toString().getBytes(StandardCharsets.UTF_8));
        return f;
    }

    /** Jobs tab: token and cost totals for today / 7 days, a run card for a chapter-set run, and the Current job never a finished job. */
    @Test
    public void f_jobsTabShowsTotalsAndGroupsARunByCombo() throws Exception {
        Context c = ctx();
        staleCleanup(c);
        String sessionId = "polish-" + Long.toHexString(System.nanoTime());
        String setName = "Tổ hợp thử v423 " + (System.nanoTime() % 1000);
        File snapshot = sessionFile(c, sessionId, setName, 3);
        TranslationRepository repo = new TranslationRepository(c);
        long j0 = makeJob(repo, TAG + "run-ch0.txt", "h-run-0-" + System.nanoTime(), 2, 2, "COMPLETED");
        long j1 = makeJob(repo, TAG + "run-ch1.txt", "h-run-1-" + System.nanoTime(), 2, 1, "error");
        try {
            android.database.sqlite.SQLiteDatabase db = repo.getWritableDatabase();
            db.execSQL("UPDATE jobs SET start_session_id=?, prepared_input_ordinal=0 WHERE id=?", new Object[]{sessionId, j0});
            db.execSQL("UPDATE jobs SET start_session_id=?, prepared_input_ordinal=1 WHERE id=?", new Object[]{sessionId, j1});
            insertMetric(repo, j0, 0, "PROVIDER_REPORTED", 0.0100, 2000, 500, 400);
            insertMetric(repo, j0, 1, "PROVIDER_REPORTED", 0.0050, 1000, 0, 300);
            insertMetric(repo, j1, 0, "UNKNOWN_USAGE", 0, 700, 0, 100);
        } finally { repo.close(); }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> { a.refreshJobCacheAsync(false); a.switchTab("Jobs"); });
            V423ResumeInstrumentedTest.waitFor(() -> {
                AtomicReference<Boolean> ok = new AtomicReference<>(false);
                scenario.onActivity(a -> ok.set(V423ResumeInstrumentedTest.containsText(a.getWindow().getDecorView(), setName)));
                return ok.get();
            }, 20_000L, "the run card is on the Jobs tab");
            scrollPageTo(scenario, a -> findByText(a.getWindow().getDecorView(), "Token và chi phí"), 250);
            V423ResumeInstrumentedTest.media("44a-jobs-top-totals-and-current");
            scenario.onActivity(a -> {
                View root = a.getWindow().getDecorView();
                assertTrue(V423ResumeInstrumentedTest.containsText(root, "Token và chi phí"));
                assertTrue(V423ResumeInstrumentedTest.containsText(root, "Hôm nay"));
                assertTrue(V423ResumeInstrumentedTest.containsText(root, "7 ngày"));
                assertFalse("a chapter finished today is counted (the since value was compared as text and always lost)", V423ResumeInstrumentedTest.containsText(root, "Hôm nay: 0 chương xong"));
                assertFalse(V423ResumeInstrumentedTest.containsText(root, "7 ngày: 0 chương xong"));
                assertTrue("1 of 3 chapters done, 1 failed, 1 not run", V423ResumeInstrumentedTest.containsText(root, "1/3 chương xong · 1 lỗi · 1 chưa chạy"));
                assertTrue("the run's cost is the sum of its chapters (0.0150)", V423ResumeInstrumentedTest.containsText(root, "USD 0.0150"));
                assertTrue("a call without a known price is counted apart", V423ResumeInstrumentedTest.containsText(root, "1 lượt chưa rõ giá"));
                assertFalse("the finished chapter is not the Current job", containsInCurrentCard(root, TAG + "run-ch0.txt"));
            });
            scrollPageTo(scenario, a -> findByDescription(a.getWindow().getDecorView(), "run-card"), 300);
            V423ResumeInstrumentedTest.media("44-jobs-run-card");
            scenario.onActivity(a -> { View card = findByDescription(a.getWindow().getDecorView(), "run-card"); assertNotNull(card); card.performClick(); });
            V423ResumeInstrumentedTest.waitFor(() -> {
                AtomicReference<Boolean> ok = new AtomicReference<>(false);
                scenario.onActivity(a -> ok.set(V423ResumeInstrumentedTest.containsText(a.getWindow().getDecorView(), "Mở kết quả")));
                return ok.get();
            }, 10_000L, "opening the run lists its chapters with Mở kết quả");
            scrollPageTo(scenario, a -> findByDescription(a.getWindow().getDecorView(), "run-card"), 250);
            V423ResumeInstrumentedTest.media("45-jobs-run-expanded");
        } finally {
            TranslationRepository r2 = new TranslationRepository(c);
            try { r2.deleteJob(j0); r2.deleteJob(j1); } finally { r2.close(); }
            snapshot.delete();
        }
    }

    /** Scrolls the page so {@code target} sits {@code offsetPx} below the top of the window (for the screenshots). */
    private static void scrollPageTo(ActivityScenario<MainActivity> scenario, java.util.function.Function<MainActivity, View> target, int offsetPx) throws Exception {
        scenario.onActivity(a -> {
            View v = target.apply(a);
            if (v == null) return;
            int[] at = new int[2];
            v.getLocationInWindow(at);
            ScrollView outer = null;   // the page's own ScrollView: the outermost one above the target
            for (android.view.ViewParent p = v.getParent(); p instanceof View; p = p.getParent()) if (p instanceof ScrollView) outer = (ScrollView) p;
            if (outer != null) outer.scrollBy(0, at[1] - offsetPx);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(400L);
    }

    private static boolean containsInCurrentCard(View root, String text) {
        // The "Current job" section is the card that holds the "Resume" / "Refresh" buttons; a finished job must not appear in it.
        View refresh = findByText(root, "↻ Refresh");
        if (refresh == null) return false;
        View parent = (View) refresh.getParent();
        while (parent != null && !(parent.getParent() instanceof ScrollView) && parent.getParent() instanceof View) {
            if (V423ResumeInstrumentedTest.containsText(parent, "Current job")) return V423ResumeInstrumentedTest.containsText(parent, text);
            parent = (View) parent.getParent();
        }
        return false;
    }

    private static View findByText(View v, String text) {
        if (v instanceof android.widget.TextView && ((android.widget.TextView) v).getText().toString().contains(text)) return v;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { View f = findByText(((ViewGroup) v).getChildAt(i), text); if (f != null) return f; }
        return null;
    }

    private static View findByDescription(View v, String description) {
        if (description.contentEquals(v.getContentDescription() == null ? "" : v.getContentDescription())) return v;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { View f = findByDescription(((ViewGroup) v).getChildAt(i), description); if (f != null) return f; }
        return null;
    }

    /** Chapter-set mode hides the Glossaries and Pronoun rows (each chapter has its own); Tệp TXT shows them again. */
    @Test
    public void g_comboModeHidesTheGlossaryAndPronounRows() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<String> original = new AtomicReference<>("");
            scenario.onActivity(a -> {
                a.switchTab("Translate");
                original.set(a.sourceUi.isSetMode() ? TranslateSourceFactory.MODE_SET : TranslateSourceFactory.MODE_FILES);
                a.sourceUi.setMode(TranslateSourceFactory.MODE_FILES);
                assertEquals(View.VISIBLE, a.translateGlossaryRow.getVisibility());
                assertEquals(View.VISIBLE, a.translatePronounRow.getVisibility());
                assertEquals(View.GONE, a.translateComboNote.getVisibility());
                a.sourceUi.setMode(TranslateSourceFactory.MODE_SET);
                assertEquals(View.GONE, a.translateGlossaryRow.getVisibility());
                assertEquals(View.GONE, a.translatePronounRow.getVisibility());
                assertEquals(View.VISIBLE, a.translateComboNote.getVisibility());
                assertTrue(a.translateComboNote.getText().toString().contains("theo từng chương"));
            });
            scenario.onActivity(a -> {
                int[] at = new int[2];
                a.translateComboNote.getLocationInWindow(at);
                a.findScrollView(a.getWindow().getDecorView()).scrollBy(0, at[1] - 700);
            });
            V423ResumeInstrumentedTest.media("46-combo-mode-compact");
            scenario.onActivity(a -> {
                a.sourceUi.setMode(TranslateSourceFactory.MODE_FILES);
                assertEquals(View.VISIBLE, a.translateGlossaryRow.getVisibility());
                assertEquals(View.VISIBLE, a.translatePronounRow.getVisibility());
                a.sourceUi.setMode(original.get());
            });
        }
    }

    /** A finger drag delivered through the activity's touch dispatch, exactly where the finger would be (window coordinates). */
    private static void swipe(ActivityScenario<MainActivity> scenario, View v, int fromDy, int toDy) {
        int[] xy = new int[2];
        v.getLocationInWindow(xy);
        final float x = xy[0] + v.getWidth() / 2f, y0 = xy[1] + v.getHeight() / 2f + fromDy, y1 = xy[1] + v.getHeight() / 2f + toDy;
        final long t = SystemClock.uptimeMillis();
        scenario.onActivity(a -> a.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y0, 0)));
        int steps = 14;
        for (int i = 1; i <= steps; i++) {
            final float y = y0 + (y1 - y0) * i / steps;
            scenario.onActivity(a -> a.dispatchTouchEvent(MotionEvent.obtain(t, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, x, y, 0)));
            SystemClock.sleep(16L);
        }
        scenario.onActivity(a -> a.dispatchTouchEvent(MotionEvent.obtain(t, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y1, 0)));
    }

    /** A finger drag inside the Activity Log scrolls the log itself (it used to scroll only with a mouse wheel). */
    @Test
    public void h_fingerDragScrollsTheActivityLog() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                a.switchTab("Translate");
                for (int i = 0; i < 120; i++) a.appendLog("polish scroll line " + i + " – một dòng log đủ dài để cuộn thử", false);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Thread.sleep(600L);
            AtomicReference<ScrollView> logRef = new AtomicReference<>();
            scenario.onActivity(a -> {
                logRef.set(a.logScroll);
                assertTrue("nested scrolling is on for the log", a.logScroll.isNestedScrollingEnabled());
                int[] at = new int[2];
                a.logScroll.getLocationInWindow(at);
                ScrollView page = a.findScrollView(a.getWindow().getDecorView());
                page.scrollBy(0, at[1] - 400);   // bring the log frame into the visible part of the page
            });
            Thread.sleep(800L);
            AtomicInteger before = new AtomicInteger(), after = new AtomicInteger();
            scenario.onActivity(a -> { a.logScroll.scrollTo(0, a.logScroll.getChildAt(0).getHeight()); before.set(a.logScroll.getScrollY()); });
            assertTrue("the log is longer than its frame", before.get() > 0);
            V423ResumeInstrumentedTest.media("47-activity-log-scrolled");
            AtomicReference<View> view = new AtomicReference<>();
            scenario.onActivity(a -> view.set(a.logScroll));
            // control: with nested scrolling off (the old behaviour) the same drag is recorded, not asserted
            scenario.onActivity(a -> a.logScroll.setNestedScrollingEnabled(false));
            swipe(scenario, view.get(), -100, 160);
            Thread.sleep(500L);
            AtomicInteger control = new AtomicInteger();
            scenario.onActivity(a -> control.set(a.logScroll.getScrollY()));
            android.util.Log.i("V423POLISH", "control (nested off): scrollY " + before.get() + " -> " + control.get());
            scenario.onActivity(a -> { a.logScroll.setNestedScrollingEnabled(true); a.logScroll.scrollTo(0, a.logScroll.getChildAt(0).getHeight()); before.set(a.logScroll.getScrollY()); });
            scenario.onActivity(a -> { int[] w = new int[2]; a.logScroll.getLocationInWindow(w); int[] sc = new int[2]; a.logScroll.getLocationOnScreen(sc); android.util.Log.i("V423POLISH", "log view window=" + w[0] + "," + w[1] + " screen=" + sc[0] + "," + sc[1] + " size=" + a.logScroll.getWidth() + "x" + a.logScroll.getHeight() + " shown=" + a.logScroll.isShown() + " decor=" + a.getWindow().getDecorView().getHeight()); });
            swipe(scenario, view.get(), -100, 160);   // finger moves down: the content moves down, scrollY gets smaller
            Thread.sleep(500L);
            scenario.onActivity(a -> after.set(a.logScroll.getScrollY()));
            android.util.Log.i("V423POLISH", "nested on: scrollY " + before.get() + " -> " + after.get());
            assertTrue("scrollY " + before.get() + " -> " + after.get(), after.get() < before.get());
        }
    }

    /** Library: "N mục" on each list, and an import notice bar with details; a Pronoun delete goes through the undo bar. */
    @Test
    public void i_libraryShowsCountsNoticeAndUndoForAPronounDelete() throws Exception {
        Context c = ctx();
        PronounStore.Profile p = PronounStore.importProfile(c, TAG + "pronoun-del", "", "from,speaker,target,self,call,scope,note\n花子,太郎,花子,私,君,all,test\n");
        assertNotNull(p);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> a.switchTab("Pronouns"));
            V423ResumeInstrumentedTest.waitFor(() -> {
                AtomicReference<Boolean> ok = new AtomicReference<>(false);
                scenario.onActivity(a -> ok.set(V423ResumeInstrumentedTest.containsText(a.getWindow().getDecorView(), " mục")));
                return ok.get();
            }, 10_000L, "Pronouns shows N mục");
            scenario.onActivity(a -> a.switchTab("RAW"));
            V423ResumeInstrumentedTest.media("48a-library-raw-count");
            scenario.onActivity(a -> a.switchTab("Glossaries"));
            V423ResumeInstrumentedTest.media("48b-library-glossaries-count");
            scenario.onActivity(a -> a.switchTab("Pronouns"));
            scenario.onActivity(a -> a.bulkUi.notice(LibraryBulkUi.TAB_PRONOUNS, "Đã thêm 2 · bỏ qua 1 (trùng/lỗi)", "• x.csv: trùng nội dung"));
            V423ResumeInstrumentedTest.waitFor(() -> {
                AtomicReference<Boolean> ok = new AtomicReference<>(false);
                scenario.onActivity(a -> ok.set(V423ResumeInstrumentedTest.containsText(a.getWindow().getDecorView(), "Đã thêm 2 · bỏ qua 1 (trùng/lỗi)") && V423ResumeInstrumentedTest.containsText(a.getWindow().getDecorView(), "Xem chi tiết")));
                return ok.get();
            }, 10_000L, "the notice bar shows with its detail button");
            scenario.onActivity(a -> {
                LibraryDeletePlan.Target t = new LibraryDeletePlan.Target(p.id, p.name);
                a.bulkUi.executePlan(LibraryBulkUi.TAB_PRONOUNS, LibraryDeletePlan.plan(Collections.singletonList(t)));
            });
            V423ResumeInstrumentedTest.waitFor(() -> {
                AtomicReference<Boolean> ok = new AtomicReference<>(false);
                scenario.onActivity(a -> ok.set(a.bulkUi.undoBar() != null));
                return ok.get();
            }, 10_000L, "the delete shows the undo bar");
            V423ResumeInstrumentedTest.media("48-library-notice-and-undo");
            scenario.onActivity(a -> a.bulkUi.undoNow());
            assertNotNull("undo brought the profile back", PronounStore.find(c, p.id));
            scenario.onActivity(a -> a.bulkUi.clearNotice());
        } finally {
            PronounStore.delete(c, p.id);
        }
    }
}
