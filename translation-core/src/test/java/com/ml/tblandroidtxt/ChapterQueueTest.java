package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.List;

/** The chapter loop shared by the phone service and the PC, driven with scripted steps (no provider calls except the resume case). */
public class ChapterQueueTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private RunnerHarness h;

    @Before public void open() throws Exception { h = new RunnerHarness(tmp.getRoot().toPath()); }

    @After public void close() throws Exception { h.close(); }

    private static ChapterBinding binding(int chapterNo, String glossaryText) {
        ChapterBinding b = new ChapterBinding();
        b.chapterNo = chapterNo;
        b.glossaryText = glossaryText;
        return b;
    }

    private List<ChapterQueue.Task> tasks(int n) {
        ArrayList<ChapterQueue.Task> out = new ArrayList<>();
        for (int i = 1; i <= n; i++) out.add(new ChapterQueue.Task("QA_" + i + ".txt", binding(i, "G" + i), null, null));
        return out;
    }

    private ChapterRunOutcome begin(int n) {
        h.runner.beginRun(true);
        ChapterRunOutcome outcome = new ChapterRunOutcome(n);
        h.runner.setChapterOutcome(outcome);
        return outcome;
    }

    private static class Recorder implements ChapterQueue.Step {
        final List<String> calls = new ArrayList<>();
        final List<String> glossaries = new ArrayList<>();
        @Override public void startNew(ChapterQueue.Task input, AppSettings s, int fileNo, int fileTotal, String label) throws Exception {
            calls.add(input.displayName + "|" + fileNo + "/" + fileTotal + "|" + label);
            glossaries.add(s.glossaryText);
            act(input);
        }
        void act(ChapterQueue.Task input) throws Exception { }
    }

    @Test public void everyChapterRunsWithItsOwnBindingAndTheRunEndsCompleted() throws Exception {
        ChapterRunOutcome outcome = begin(3);
        Recorder step = new Recorder();
        AppSettings base = RunnerHarness.settings();
        base.glossaryText = "BASE";
        assertTrue(ChapterQueue.run(h.runner, h.repo, tasks(3), base, step));
        assertEquals(3, step.calls.size());
        assertEquals("QA_2.txt|2/3|Chương 2 (2/3) · QA_2.txt", step.calls.get(1));
        assertEquals(java.util.Arrays.asList("G1", "G2", "G3"), step.glossaries);
        assertEquals("the shared settings are never replaced by a chapter's copy", "BASE", base.glossaryText);
        assertEquals(3, outcome.doneCount());
        assertEquals(0, outcome.failedCount());
        assertTrue(h.statuses().contains("done"));
    }

    @Test public void aChapterLocalFailureMovesOnToTheNextChapter() throws Exception {
        ChapterRunOutcome outcome = begin(3);
        Recorder step = new Recorder() {
            @Override void act(ChapterQueue.Task input) {
                if (input.displayName.equals("QA_2.txt")) throw new ChapterLocalException("kiểm định chương 2 lỗi");
            }
        };
        assertTrue(ChapterQueue.run(h.runner, h.repo, tasks(3), RunnerHarness.settings(), step));
        assertEquals("chapter 3 still ran", 3, step.calls.size());
        assertEquals(2, outcome.doneCount());
        assertEquals(1, outcome.failedCount());
        assertEquals(0, outcome.notRun());
        assertTrue(outcome.summary(), outcome.summary().contains("2"));
        assertTrue(h.statuses().contains("done"));
        boolean announced = false;
        for (RunSnapshot s : h.snapshots) if (s.log != null && s.log.contains("Chương 2 lỗi, chuyển chương 3")) announced = true;
        assertTrue("the failure line names the next chapter", announced);
    }

    @Test public void twoFailuresInARowStopTheQueueWithItsOwnSentence() throws Exception {
        ChapterRunOutcome outcome = begin(4);
        Recorder step = new Recorder() {
            @Override void act(ChapterQueue.Task input) {
                if (!input.displayName.equals("QA_1.txt")) throw new ChapterLocalException("lỗi " + input.displayName);
            }
        };
        try {
            ChapterQueue.run(h.runner, h.repo, tasks(4), RunnerHarness.settings(), step);
            fail("expected the queue to stop");
        } catch (ChapterQueue.QueueStoppedException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Dừng tập: 2 chương liên tiếp lỗi kiểm định (Chương 2, Chương 3)"));
        }
        assertEquals("chapter 4 never started", 3, step.calls.size());
        assertEquals(1, outcome.doneCount());
        assertEquals(2, outcome.failedCount());
        assertEquals(1, outcome.notRun());
    }

    @Test public void aFailureThatIsNotLocalToTheChapterStopsEverything() throws Exception {
        begin(3);
        Recorder step = new Recorder() {
            @Override void act(ChapterQueue.Task input) {
                if (input.displayName.equals("QA_2.txt")) throw new IllegalStateException("quota hết");
            }
        };
        try {
            ChapterQueue.run(h.runner, h.repo, tasks(3), RunnerHarness.settings(), step);
            fail("expected the failure to propagate");
        } catch (IllegalStateException e) {
            assertEquals("quota hết", e.getMessage());
        }
        assertEquals(2, step.calls.size());
    }

    @Test public void cancelDuringAChapterEndsTheQueueWithThatFailureRethrown() throws Exception {
        ChapterRunOutcome outcome = begin(3);
        Recorder step = new Recorder() {
            @Override void act(ChapterQueue.Task input) {
                if (input.displayName.equals("QA_2.txt")) {
                    h.runner.cancel();
                    throw new ChapterLocalException("bị hủy giữa chương");
                }
            }
        };
        try {
            ChapterQueue.run(h.runner, h.repo, tasks(3), RunnerHarness.settings(), step);
            fail("expected the failure to propagate");
        } catch (ChapterLocalException e) {
            assertEquals("bị hủy giữa chương", e.getMessage());
        }
        assertEquals(2, step.calls.size());
        assertEquals("a cancelled chapter is not counted as failed", 1, outcome.doneCount());
        assertEquals(0, outcome.failedCount());
    }

    @Test public void cancelBetweenChaptersReturnsFalseSoTheCallerFinishesTheCancellation() throws Exception {
        ChapterRunOutcome outcome = begin(3);
        Recorder step = new Recorder() {
            @Override void act(ChapterQueue.Task input) {
                if (input.displayName.equals("QA_1.txt")) h.runner.cancel();
            }
        };
        assertFalse(ChapterQueue.run(h.runner, h.repo, tasks(3), RunnerHarness.settings(), step));
        assertEquals(1, step.calls.size());
        assertEquals("the chapter that was cancelling is not counted as done", 0, outcome.doneCount());
        assertFalse("no completion line after a cancel", h.statuses().contains("done"));
    }

    @Test public void anUnfinishedChapterIsContinuedInsteadOfStartedAgain() throws Exception {
        AppSettings s = RunnerHarness.settings();
        long jobId = h.startJob(s, RunnerHarness.chunks("A1.", "A2.", "A3."));
        h.runner.setActiveJobId(0);
        ChapterRunOutcome outcome = begin(2);
        h.script.add(RunnerHarness.ok());
        h.script.add(RunnerHarness.ok());
        h.script.add(RunnerHarness.ok());
        ArrayList<ChapterQueue.Task> list = new ArrayList<>();
        list.add(new ChapterQueue.Task("QA_1.txt", binding(1, "G1"), Long.valueOf(jobId), null));
        list.add(new ChapterQueue.Task("QA_2.txt", binding(2, "G2"), null, null));
        Recorder step = new Recorder();
        assertTrue(ChapterQueue.run(h.runner, h.repo, list, s, step));
        assertEquals("only the second chapter was started fresh", 1, step.calls.size());
        assertTrue(step.calls.get(0).startsWith("QA_2.txt|2/2|"));
        assertEquals(3, h.calls.size());
        assertEquals("completed", String.valueOf(h.jobStatus(jobId)).toLowerCase());
        assertEquals(2, outcome.doneCount());
    }

    @Test public void withoutAChapterOutcomeTheQueueRunsPlainInputsWithTheSharedSettings() throws Exception {
        h.runner.beginRun(true);
        h.runner.setChapterOutcome(null);
        Recorder step = new Recorder();
        ArrayList<ChapterQueue.Task> list = new ArrayList<>();
        list.add(new ChapterQueue.Task("QA_a.txt", null, null, null));
        list.add(new ChapterQueue.Task("QA_b.txt", null, null, null));
        AppSettings base = RunnerHarness.settings();
        base.glossaryText = "BASE";
        assertTrue(ChapterQueue.run(h.runner, h.repo, list, base, step));
        assertEquals(java.util.Arrays.asList("BASE", "BASE"), step.glossaries);
        assertEquals("QA_b.txt|2/2|QA_b.txt", step.calls.get(1));
    }
}
