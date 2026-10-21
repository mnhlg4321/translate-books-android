package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** v4.23 J2: the cost limit and the totals belong to the whole run; "Tiếp tục tập" finds the run a job belongs to. */
public class V423RunMeterTest {
    @Test public void costAddsUpAcrossChaptersSoTheLimitStopsTheRunNotTheChapter() {
        RunCostMeter m = new RunCostMeter();
        // Three chapters of USD 0.40 each: no single chapter reaches USD 1, the run does at the third.
        for (int chapter = 1; chapter <= 3; chapter++) {
            double chapterCost = 0;
            for (int call = 0; call < 4; call++) { m.addUsage(1000, 0.10); chapterCost += 0.10; }
            assertTrue(chapterCost < 1.0);
            if (chapter < 3) assertFalse("after chapter " + chapter, m.limitReached(true, 1.0));
        }
        assertEquals(12_000, m.tokens());
        assertEquals(1.2, m.cost(), 1e-9);
        assertTrue(m.limitReached(true, 1.0));
        assertFalse("limit off", m.limitReached(false, 1.0));
        assertFalse("limit zero means none", m.limitReached(true, 0));
    }

    @Test public void aContinuedJobBringsItsEarlierSpendOnlyOnce() {
        RunCostMeter m = new RunCostMeter();
        assertTrue(m.preloadContinuedJob(297, 0.30));
        assertFalse("the same job twice in one run", m.preloadContinuedJob(297, 0.30));
        assertTrue(m.preloadContinuedJob(298, 0.20));
        m.addUsage(500, 0.05);
        assertEquals(0.55, m.cost(), 1e-9);
        assertEquals(500, m.tokens());
        assertTrue(m.limitReached(true, 0.5));
        m.reset();
        assertEquals(0, m.cost(), 0);
        assertTrue("a new run may preload the job again", m.preloadContinuedJob(297, 0.30));
    }

    @Test public void unknownPriceAndNegativeNumbersNeverCorruptTheTotals() {
        RunCostMeter m = new RunCostMeter();
        m.addUsage(-5, Double.NaN);
        m.addUsage(100, -1);
        assertFalse(m.preloadContinuedJob(0, 1));
        assertEquals(100, m.tokens());
        assertEquals(0, m.cost(), 0);
    }

    private static RunGroups.SessionInfo session(String id, String setId, long createdAt, int chapters, long... resumed) {
        RunGroups.SessionInfo s = new RunGroups.SessionInfo();
        s.sessionId = id; s.setId = setId; s.setName = "set"; s.createdAt = createdAt; s.chapterCount = chapters;
        for (long j : resumed) s.resumedJobs.add(j);
        return s;
    }

    private static TranslationRepository.JobSummary job(long id, String startSession, int ordinal, String status, int failedChunks) {
        TranslationRepository.JobSummary j = new TranslationRepository.JobSummary();
        j.id = id; j.startSessionId = startSession; j.preparedOrdinal = ordinal; j.status = status; j.failedChunks = failedChunks; j.updatedAt = id;
        return j;
    }

    @Test public void aJobBelongsToTheNewestRunThatContinuedItElseToTheRunThatCreatedIt() {
        List<RunGroups.SessionInfo> sessions = new ArrayList<>();
        sessions.add(session("a", "set-1", 100, 4));
        sessions.add(session("b", "set-1", 200, 4, 7L));
        assertEquals("b", RunGroups.sessionOf(7, "a", sessions).sessionId);
        assertEquals("a", RunGroups.sessionOf(8, "a", sessions).sessionId);
        assertNull(RunGroups.sessionOf(9, "unknown", sessions));
        assertNull(RunGroups.sessionOf(9, null, sessions));
    }

    @Test public void aRunKnowsItsSetAndHowManyChaptersAreUnfinished() {
        List<RunGroups.SessionInfo> sessions = new ArrayList<>();
        sessions.add(session("a", "set-1", 100, 4));
        List<TranslationRepository.JobSummary> jobs = new ArrayList<>();
        jobs.add(job(3, "a", 2, "error", 1));
        jobs.add(job(2, "a", 1, "COMPLETED", 0));
        jobs.add(job(1, "a", 0, "COMPLETED", 0));
        RunGroups.Run run = null;
        for (RunGroups.Item item : RunGroups.build(jobs, sessions)) if (item.run != null) run = item.run;
        assertNotNull(run);
        assertEquals("set-1", run.setId);
        assertEquals(2, run.done);
        assertEquals(1, run.failed);
        assertEquals("the failed chapter and the one that never started", 2, run.unfinished());
    }
}
