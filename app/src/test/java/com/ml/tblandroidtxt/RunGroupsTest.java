package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunGroupsTest {
    private static TranslationRepository.JobSummary job(long id, String status, String session, int ordinal, long updated, double cost) {
        TranslationRepository.JobSummary j = new TranslationRepository.JobSummary();
        j.id = id; j.status = status; j.startSessionId = session; j.preparedOrdinal = ordinal; j.updatedAt = updated; j.createdAt = updated - 10; j.fileName = "ch" + ordinal;
        if (cost >= 0) { j.usage = new UsageTotals(); j.usage.add("PROVIDER_REPORTED", cost, 1000, 100, 200); }
        return j;
    }

    private static RunGroups.SessionInfo session(String id, String name, long created, int chapters, Long... resumed) {
        RunGroups.SessionInfo s = new RunGroups.SessionInfo();
        s.sessionId = id; s.setName = name; s.createdAt = created; s.chapterCount = chapters;
        s.resumedJobs.addAll(Arrays.asList(resumed));
        return s;
    }

    private static RunGroups.Run onlyRun(List<RunGroups.Item> items) {
        RunGroups.Run found = null;
        for (RunGroups.Item i : items) if (i.run != null) { assertNull("one run expected", found); found = i.run; }
        assertNotNull(found);
        return found;
    }

    @Test public void aRunCountsDoneFailedAndNotRunChaptersAndAddsItsMoney() {
        List<TranslationRepository.JobSummary> jobs = Arrays.asList(
                job(13, "COMPLETED", "s1", 2, 3000, 0.003), job(12, "error", "s1", 1, 2000, 0.002), job(11, "COMPLETED", "s1", 0, 1000, 0.001));
        RunGroups.Run run = onlyRun(RunGroups.build(jobs, Arrays.asList(session("s1", "Tập 7", 900, 5))));
        assertEquals("Tập 7", run.setName);
        assertEquals(2, run.done);
        assertEquals(1, run.failed);
        assertEquals(5, run.chapterCount);
        assertEquals(2, run.notRun());
        assertEquals(0.006, run.usage.costUsd, 1e-9);
        assertEquals(3000 - 900, run.durationMs());
        assertEquals("chapters in order", Arrays.asList(11L, 12L, 13L), ids(run));
    }

    private static List<Long> ids(RunGroups.Run run) {
        ArrayList<Long> out = new ArrayList<>();
        for (RunGroups.Entry e : run.chapters) out.add(e.job.id);
        return out;
    }

    @Test public void aContinuedChapterShowsUnderTheNewRunAndIsNotCountedTwice() {
        // job 11 was made by run s1 (chapter 2 failed) and continued by the later run s2
        List<TranslationRepository.JobSummary> jobs = Arrays.asList(
                job(11, "COMPLETED", "s1", 1, 5000, 0.010), job(10, "COMPLETED", "s1", 0, 1000, 0.001));
        List<RunGroups.SessionInfo> sessions = Arrays.asList(session("s1", "Tập 7", 900, 2), session("s2", "Tập 7", 4000, 1, 11L));
        List<RunGroups.Item> items = RunGroups.build(jobs, sessions);
        RunGroups.Run s1 = null, s2 = null;
        for (RunGroups.Item i : items) if (i.run != null) { if ("s1".equals(i.run.sessionId)) s1 = i.run; else s2 = i.run; }
        assertEquals(Arrays.asList(10L), ids(s1));
        assertEquals(Arrays.asList(11L), ids(s2));
        assertEquals("tiếp tục từ lượt", "s1", s2.chapters.get(0).continuedFromSession);
        assertEquals(0.010, s2.usage.costUsd, 1e-9);
        assertEquals(0.001, s1.usage.costUsd, 1e-9);
    }

    @Test public void jobsWithoutAKnownRunStayAsSingleRows() {
        List<TranslationRepository.JobSummary> jobs = Arrays.asList(job(5, "COMPLETED", "", 0, 100, -1), job(4, "paused", "gone-session", 0, 50, -1));
        List<RunGroups.Item> items = RunGroups.build(jobs, new ArrayList<RunGroups.SessionInfo>());
        assertEquals(2, items.size());
        for (RunGroups.Item i : items) { assertNull(i.run); assertNotNull(i.single); }
    }

    @Test public void itemsComeNewestFirst() {
        List<TranslationRepository.JobSummary> jobs = Arrays.asList(job(8, "COMPLETED", "", 0, 9000, -1), job(7, "COMPLETED", "s1", 0, 2000, 0.001));
        List<RunGroups.Item> items = RunGroups.build(jobs, Arrays.asList(session("s1", "T", 1000, 1)));
        assertEquals(2, items.size());
        assertTrue(items.get(0).single != null && items.get(1).run != null);
    }

    @Test public void aRunWithNoJobIsNotShown() {
        List<RunGroups.Item> items = RunGroups.build(new ArrayList<TranslationRepository.JobSummary>(), Arrays.asList(session("s1", "T", 1000, 3)));
        assertTrue(items.isEmpty());
    }
}
