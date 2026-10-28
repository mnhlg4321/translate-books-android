package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.List;

/**
 * The nine run-loop scenarios of docs/V424_P1B_RUN_LOOP_CORE_WORK_REQUEST.md section 3. Expectations are derived from the
 * original TranslatorService/TranslationEngine code (read at the B0 baseline) and the F1-F6 rules, not from the moved code.
 * The first snapshots the service emits itself (for example "Đã chia ... chunk") are not part of these runs: the tests call
 * the runner after creating the job, as the service does.
 */
public class RunnerScenariosTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private RunnerHarness h;

    @Before public void open() throws Exception { h = new RunnerHarness(tmp.getRoot().toPath()); }

    @After public void close() throws Exception { h.close(); }

    private static final List<String> SOURCES = Arrays.asList("A1.", "A2.", "A3.");

    private long start(AppSettings s) { return h.startJob(s, RunnerHarness.chunks("A1.", "A2.", "A3.")); }

    private void translate(long jobId, AppSettings s) throws Exception {
        h.runner.translateJob(jobId, "mem://out.txt", s, RunnerHarness.chunks("A1.", "A2.", "A3."), 1, 1, "book.txt");
    }

    /** 1. Every chunk succeeds: job completes and the output joins the answers in order. */
    @Test public void threeChunksSucceed() throws Exception {
        AppSettings s = RunnerHarness.settings();
        long id = start(s);
        for (int i = 0; i < 3; i++) h.script.add(RunnerHarness.ok());
        translate(id, s);

        assertEquals(3, h.calls.size());
        for (int i = 0; i < 3; i++) assertEquals("COMPLETED", h.chunkStatus(id, i));
        assertEquals("B1.\nB2.\nB3.\n", h.output());
        assertEquals("COMPLETED", h.jobStatus(id));
        // Per chunk the loop broadcasts: start (progress i/3), the lock-usage ping at the same progress, "done";
        // the end adds one more at 100. Progress uses percent(i, total) with round-half-up.
        assertEquals(Arrays.asList(0, 0, 0, 33, 33, 33, 67, 67, 67, 100), h.progresses());
        for (String st : h.statuses()) assertEquals("running", st);
        assertEquals(3, h.runner.getMetricCompleted());
        assertEquals(0, h.runEnded);
    }

    /** 2. A network failure on chunk 2 is retried once after the RetryPolicy backoff. */
    @Test public void networkFailureOnChunkTwoIsRetriedPerPolicy() throws Exception {
        AppSettings s = RunnerHarness.settings();
        long id = start(s);
        h.script.addAll(Arrays.asList(RunnerHarness.ok(),
                RunnerHarness.fail(new java.net.UnknownHostException("host")),
                RunnerHarness.ok(), RunnerHarness.ok()));
        translate(id, s);

        assertEquals("one extra call for the retried chunk", 4, h.calls.size());
        long total = 0;
        for (long[] one : h.slept) total += one[0];
        assertEquals(RetryPolicy.backoffMs(RetryPolicy.Kind.NETWORK, 1, s.initialRetryDelayMs, s.maxRetryDelayMs), total);
        for (int i = 0; i < 3; i++) assertEquals("COMPLETED", h.chunkStatus(id, i));
        assertEquals("B1.\nB2.\nB3.\n", h.output());
    }

    /** 3. A chunk that fails every attempt stops the job; retryFailedChunksForJob then finishes it. */
    @Test public void failedChunkThenRetryFailedFinishesTheJob() throws Exception {
        AppSettings s = RunnerHarness.settings();
        long id = start(s);
        h.script.add(RunnerHarness.ok());
        // A network error is retryable (RetryPolicy NETWORK), so the engine spends all maxAttempts=3 calls on chunk 2.
        for (int i = 0; i < 3; i++) h.script.add(RunnerHarness.fail(new java.net.UnknownHostException("host")));
        try {
            translate(id, s);
            fail("chunk 2 must fail after maxAttempts=3");
        } catch (java.net.UnknownHostException expected) {
            // the last failure is rethrown unchanged
        }
        assertEquals(4, h.calls.size());
        assertEquals("COMPLETED", h.chunkStatus(id, 0));
        assertEquals("RETRYABLE_ERROR", h.chunkStatus(id, 1));
        assertEquals("PENDING", h.chunkStatus(id, 2));
        assertEquals("only finished chunks are in the partial output", "B1.\n", h.output());
        assertTrue(h.statuses().contains("error"));

        h.script.add(RunnerHarness.ok());
        h.script.add(RunnerHarness.ok());
        h.runner.beginRun(true);
        h.runner.retryFailedChunksForJob(id);

        assertEquals(6, h.calls.size());
        assertTrue("the retried chunk sees the previous accepted translation", h.calls.get(4).user.contains("B1."));
        for (int i = 0; i < 3; i++) assertEquals("COMPLETED", h.chunkStatus(id, i));
        assertEquals("B1.\nB2.\nB3.\n", h.output());
        assertEquals("done", h.statuses().get(h.statuses().size() - 1));
        assertEquals(1, h.runEnded);
    }

    /** 4. A cut answer is resent with the next larger output limit (nextLimitAfterTruncation). */
    @Test public void truncationRaisesTheOutputLimit() throws Exception {
        AppSettings s = RunnerHarness.settings();
        long id = start(s);
        StringBuilder cut = new StringBuilder();
        for (int i = 0; i < 200; i++) cut.append('日');
        h.script.addAll(Arrays.asList(
                RunnerHarness.content("<TRANSLATION>" + cut + "</TRANSLATION>", "length", 200, 0),
                RunnerHarness.ok(), RunnerHarness.ok(), RunnerHarness.ok()));
        translate(id, s);

        // Visible budget 256 (rawTokens small, 256 floor), no reasoning reserve for this model: first cap 256, then 8192.
        assertEquals(256, h.calls.get(0).outputLimit);
        assertEquals(TranslationEngineCore.nextLimitAfterTruncation(256), h.calls.get(1).outputLimit);
        assertEquals(8192, h.calls.get(1).outputLimit);
        assertEquals(4, h.calls.size());
        assertEquals("B1.\nB2.\nB3.\n", h.output());
    }

    /** 5a. Reasoning-bound (F6): one retry one effort lower, never a third call; the chunk stops at chapter level. */
    @Test public void reasoningBoundRetriesOnceThenStopsTheChunk() throws Exception {
        AppSettings s = RunnerHarness.settings();
        s.reasoningEffort = "high";
        long id = start(s);
        for (int i = 0; i < 3; i++) h.script.add(RunnerHarness.content("<TRANSLATION>x</TRANSLATION>", "length", 100, 100));
        try {
            translate(id, s);
            fail("reasoning-bound chunk must end the translation");
        } catch (ChapterLocalException expected) {
            assertTrue(expected.getMessage().startsWith("Reasoning "));
        }
        assertEquals(2, h.calls.size());
        assertEquals("high", h.calls.get(0).effort);
        assertEquals("medium", h.calls.get(1).effort);
        assertTrue(h.calls.get(1).outputLimit < h.calls.get(0).outputLimit);
        assertEquals("RETRYABLE_ERROR", h.chunkStatus(id, 0));
    }

    /**
     * 5b. maxAttempts (10) does not decide the number of calls; RootChunkAttemptBudget(6) does. Reading B0 rootStableKey():
     * the first request of a chunk is claimed under "jobId:stable" but the value cached for later requests lacks the
     * "jobId:" prefix, so attempt 1 is counted on its own key and attempts 2..7 on the cached key (ordinals 1..6): the
     * budget stops the eighth attempt, i.e. SEVEN provider calls. This off-by-one is existing behaviour, kept as it is.
     */
    @Test public void rootAttemptBudgetCapsAtSixCalls() throws Exception {
        AppSettings s = RunnerHarness.settings();
        s.maxAttempts = 10;
        long id = start(s);
        for (int i = 0; i < 10; i++) h.script.add(RunnerHarness.fail(new java.net.UnknownHostException("host")));
        try {
            translate(id, s);
            fail("the seventh call is refused");
        } catch (ChapterLocalException expected) {
            assertTrue(expected.getMessage().contains("6"));
        }
        assertEquals(String.valueOf(h.logLines), 7, h.calls.size());
    }

    /** 6. The run-wide cost cap stops the loop before chunk 3 and pauses the job. */
    @Test public void costLimitPausesBeforeTheNextChunk() throws Exception {
        AppSettings s = RunnerHarness.settings();
        s.stopOnCostLimit = true;
        s.costLimitUsd = 0.05;
        long id = start(s);
        h.script.addAll(Arrays.asList(RunnerHarness.okWithCost(0.04), RunnerHarness.okWithCost(0.04)));
        try {
            translate(id, s);
            fail("the cap must stop the run");
        } catch (TranslationRunner.CostLimitReachedException expected) {
            assertEquals("Cost limit reached: " + CostEstimator.money(0.05), expected.getMessage());
        }
        assertEquals(2, h.calls.size());
        assertEquals("COMPLETED", h.chunkStatus(id, 0));
        assertEquals("COMPLETED", h.chunkStatus(id, 1));
        assertEquals("PENDING", h.chunkStatus(id, 2));
        assertEquals("paused", h.jobStatus(id));
        RunSnapshot last = h.snapshots.get(h.snapshots.size() - 1);
        assertEquals("paused", last.status);
        assertEquals("Cost limit reached", last.phase);
        assertEquals(0.08, h.runner.sessionCost(), 1e-9);
        assertEquals("savePartialOutput is on: the finished chunks are written", "B1.\nB2.\n", h.output());
    }

    @Test public void costLimitWithoutPartialOutputWritesNothing() throws Exception {
        AppSettings s = RunnerHarness.settings();
        s.stopOnCostLimit = true;
        s.costLimitUsd = 0.05;
        s.savePartialOutput = false;
        long id = start(s);
        h.script.addAll(Arrays.asList(RunnerHarness.okWithCost(0.04), RunnerHarness.okWithCost(0.04)));
        try {
            translate(id, s);
            fail("the cap must stop the run");
        } catch (TranslationRunner.CostLimitReachedException expected) {
            // expected
        }
        assertNull("no partial write when savePartialOutput is off", h.output());
    }

    /** 7a. pause() between chunks and resume(): the clock does not count the pause (F5). */
    @Test public void pauseAndResumeDoNotCountPausedTime() throws Exception {
        final AppSettings s = RunnerHarness.settings();
        final long id = start(s);
        h.runner.setState(TranslationJobState.State.READY, "Ready");
        h.runner.setState(TranslationJobState.State.RUNNING, "Running");
        h.script.add(RunnerHarness.ok());
        h.script.add((st, p, limit) -> {
            h.time.addAndGet(200);
            h.runner.pause();
            return RunnerHarness.answer(RunnerHarness.translate(p));
        });
        h.script.add((st, p, limit) -> {
            h.time.addAndGet(300);
            return RunnerHarness.answer(RunnerHarness.translate(p));
        });
        final Throwable[] failure = new Throwable[1];
        Thread worker = new Thread(() -> {
            try { translate(id, s); } catch (Throwable t) { failure[0] = t; }
        }, "runner");
        worker.start();
        long deadline = System.currentTimeMillis() + 10_000;
        while (worker.getState() != Thread.State.TIMED_WAITING && System.currentTimeMillis() < deadline) Thread.sleep(10);
        assertEquals("the loop waits in waitIfPaused", Thread.State.TIMED_WAITING, worker.getState());
        assertTrue(h.runner.isPaused());
        // The loop still publishes "Chunk 2 hoàn tất" after pause(), so the "paused" snapshot is found by status, not by position.
        RunSnapshot paused = null;
        synchronized (h) { for (RunSnapshot snap : h.snapshots) if (paused == null && "paused".equals(snap.status)) paused = snap; }
        assertEquals("paused", paused.status);
        assertEquals("elapsed stops at the pause marker: 200 ms of work", 200L, paused.elapsedMs);

        h.time.addAndGet(5000);
        h.runner.resume();
        RunSnapshot resumed = null;
        synchronized (h) { for (RunSnapshot snap : h.snapshots) if ("Tiếp tục dịch".equals(snap.log)) resumed = snap; }
        assertEquals("running", resumed.status);
        assertEquals(5000L, resumed.pausedTotalMs);
        assertEquals("the 5 s pause is not counted", 200L, resumed.elapsedMs);

        worker.join(20_000);
        assertFalse(worker.isAlive());
        assertNull(failure[0]);
        RunSnapshot last;
        synchronized (h) { last = h.snapshots.get(h.snapshots.size() - 1); }
        assertEquals(500L, last.elapsedMs);
        assertEquals(5000L, last.pausedTotalMs);
        for (int i = 0; i < 3; i++) assertEquals("COMPLETED", h.chunkStatus(id, i));
    }

    /** 7b. cancel() during chunk 2: the answer is stored but not committed, and the cancelled ending is signalled. */
    @Test public void cancelStopsTheRunAndSignalsTheCancelledEnding() throws Exception {
        AppSettings s = RunnerHarness.settings();
        long id = start(s);
        h.script.add(RunnerHarness.ok());
        h.script.add((st, p, limit) -> {
            h.runner.cancel();
            return RunnerHarness.answer(RunnerHarness.translate(p));
        });
        h.runner.beginRun(true);
        h.runner.resumeJobById(id);

        assertEquals(2, h.calls.size());
        assertEquals("COMPLETED", h.chunkStatus(id, 0));
        // The engine stores the received answer (RESPONSE_RECEIVED) and then marks it VALIDATING; cancelled before the commit.
        assertEquals("VALIDATING", h.chunkStatus(id, 1));
        assertEquals("PENDING", h.chunkStatus(id, 2));
        assertEquals(Arrays.asList("Đã hủy resume checkpoint"), h.cancelledEnds);
        assertEquals(0, h.runEnded);
        assertEquals("cancelled", h.jobStatus(id));
        assertTrue(h.statuses().contains("stopping"));
    }

    /** 8. Resuming skips the finished chunk and carries its translation into the next prompt (previousBefore/tail). */
    @Test public void resumeSkipsFinishedChunksAndCarriesContext() throws Exception {
        AppSettings s = RunnerHarness.settings();
        long id = h.repo.createJob("mem://in.txt", "mem://out.txt", "book.txt", s, RunnerHarness.chunks("A1.", "A2.", "A3."), "", "");
        h.repo.markChunkDone(id, 0, "B1.");
        h.script.add(RunnerHarness.ok());
        h.script.add(RunnerHarness.ok());
        h.runner.beginRun(true);
        h.runner.resumeJobById(id);

        assertEquals(2, h.calls.size());
        assertTrue(h.calls.get(0).user.contains("<INPUT>\nA2.\n</INPUT>"));
        assertTrue(h.calls.get(0).user.contains("PREVIOUS TRANSLATION CONTEXT"));
        assertTrue(h.calls.get(0).user.contains("B1."));
        for (int i = 0; i < 3; i++) assertEquals("COMPLETED", h.chunkStatus(id, i));
        assertEquals("B1.\nB2.\nB3.\n", h.output());
        assertEquals("done", h.statuses().get(h.statuses().size() - 1));
        assertEquals(1, h.runEnded);
    }

    /** 9. A Japanese line left in the answer (F2 RETAINED_SOURCE_LINE) is rejected and retried like any validation failure. */
    @Test public void retainedSourceLineIsRejectedAndRetried() throws Exception {
        AppSettings s = RunnerHarness.settings();
        long id = h.startJob(s, RunnerHarness.chunks("これは日本語の文章です。"));
        String kept = "<TRANSLATION>これは日本語の文章がそのまま残っています。</TRANSLATION>";
        for (int i = 0; i < 3; i++) h.script.add(RunnerHarness.content(kept, "stop", 20, 0));
        try {
            h.runner.translateJob(id, "mem://out.txt", s,
                    RunnerHarness.chunks("これは日本語の文章です。"), 1, 1, "book.txt");
            fail("the answer keeps Japanese text");
        } catch (ResponseValidator.RejectedResponseException expected) {
            assertTrue(expected.validation.errors.contains("RETAINED_SOURCE_LINE"));
        }
        assertEquals("maxAttempts = 3 calls", 3, h.calls.size());
        assertEquals("RETRYABLE_ERROR", h.chunkStatus(id, 0));
        assertTrue("outside a chapter set the error is broadcast", h.statuses().contains("error"));
    }

    @Test public void retainedSourceLineInAChapterSetBecomesAChapterLocalFailure() throws Exception {
        AppSettings s = RunnerHarness.settings();
        String source = "これは日本語の文章です。";
        long id = h.startJob(s, RunnerHarness.chunks(source));
        h.runner.setChapterOutcome(new ChapterRunOutcome(1));
        String kept = "<TRANSLATION>これは日本語の文章がそのまま残っています。</TRANSLATION>";
        for (int i = 0; i < 3; i++) h.script.add(RunnerHarness.content(kept, "stop", 20, 0));
        try {
            h.runner.translateJob(id, "mem://out.txt", s, RunnerHarness.chunks(source), 1, 1, "book.txt");
            fail("the answer keeps Japanese text");
        } catch (ChapterLocalException expected) {
            // the queue would go on with the next chapter
        }
        assertFalse("no error broadcast: the chapter set continues", h.statuses().contains("error"));
    }

    /** startNewJob (moved from TranslatorService.runSingleJob): reads the input through DocumentIO, creates the job and translates it. */
    @Test public void startNewJobReadsCreatesAndTranslates() throws Exception {
        java.nio.file.Files.write(tmp.getRoot().toPath().resolve("in.txt"), "A1.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        AppSettings s = RunnerHarness.settings();
        h.script.add(RunnerHarness.ok());
        h.runner.beginRun(true);
        h.runner.startNewJob("mem://in.txt", "mem://out.txt", s, 1, 1);

        assertEquals(1, h.calls.size());
        assertEquals("B1.\n", h.output());
        assertEquals(1, h.repo.getJobSummaries(10).size());
        long id = h.repo.getJobSummaries(10).get(0).id;
        assertEquals("in.txt", h.repo.getJobSummaries(10).get(0).fileName);
        assertEquals("COMPLETED", h.jobStatus(id));
        assertEquals("mem://in.txt", h.repo.getJob(id).inputUri);
        assertEquals(id, h.runner.getActiveJobId());
        assertEquals("Translation completed: in.txt", h.snapshots.get(h.snapshots.size() - 1).log);
    }
}
