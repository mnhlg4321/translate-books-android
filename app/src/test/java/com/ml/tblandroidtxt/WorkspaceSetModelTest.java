package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkspaceSetModelTest {
    private static ChapterSetStore.SetInfo live(String id, String name, int chapters, long updated) {
        ChapterSetStore.SetInfo s = new ChapterSetStore.SetInfo();
        s.id = id; s.name = name; s.chapters = chapters; s.updatedAt = updated;
        return s;
    }

    private static RunGroups.SessionInfo run(String id, String setId, String name, long created, int chapters) {
        RunGroups.SessionInfo s = new RunGroups.SessionInfo();
        s.sessionId = id; s.setId = setId; s.setName = name; s.createdAt = created; s.chapterCount = chapters;
        return s;
    }

    @Test public void liveAndDeletedRunsMergeOnceBySetId() {
        RunGroups.SessionInfo deleted = run("r-old", "gone", "Old volume", 10, 31);
        RunGroups.SessionInfo liveRun = run("r-live", "live", "Run name", 20, 4);
        List<WorkspaceSetModel.Entry> out = WorkspaceSetModel.merge(
                Arrays.asList(live("live", "Current volume", 5, 30)),
                Arrays.asList(deleted, liveRun), Collections.<String, Long>emptyMap());
        assertEquals(2, out.size());
        WorkspaceSetModel.Entry gone = find(out, "gone");
        assertTrue(gone.deleted);
        assertEquals(31, gone.chapterCount);
        assertEquals("Old volume", gone.name);
        WorkspaceSetModel.Entry live = find(out, "live");
        assertFalse(live.deleted);
        assertEquals("Current volume", live.name);
        assertEquals(5, live.chapterCount);
    }

    @Test public void recentIsCappedAndUsesNewestRunUpdateOrOpenTime() {
        RunGroups.SessionInfo old = run("r", "old", "Old", 100, 1);
        Map<String, Long> opened = new HashMap<>();
        opened.put("old", 10_000L);
        List<WorkspaceSetModel.Entry> all = WorkspaceSetModel.merge(
                Arrays.asList(live("old", "Old", 1, 1), live("new", "New", 1, 9_000)),
                Collections.singletonList(old), opened);
        for (int i = 0; i < 8; i++) all.add(liveEntry("x" + i, 1_000L + i));
        assertEquals("old", WorkspaceSetModel.recent(all, 1).get(0).id);
        assertEquals(6, WorkspaceSetModel.recent(all, 6).size());
    }

    @Test public void continuedJobIsFilteredIntoTheRunThatResumedIt() {
        TranslationRepository.JobSummary oldJob = new TranslationRepository.JobSummary();
        oldJob.id = 41; oldJob.startSessionId = "old-run"; oldJob.status = "COMPLETED";
        TranslationRepository.JobSummary newJob = new TranslationRepository.JobSummary();
        newJob.id = 42; newJob.startSessionId = "new-run"; newJob.status = "error";
        RunGroups.SessionInfo oldRun = run("old-run", "set-old", "Old", 10, 1);
        RunGroups.SessionInfo newRun = run("new-run", "set-new", "New", 20, 1);
        newRun.resumedJobs.add(41L);
        List<TranslationRepository.JobSummary> jobs = WorkspaceSetModel.jobsForSet(
                Arrays.asList(oldJob, newJob), Arrays.asList(oldRun, newRun), "set-new");
        assertEquals(2, jobs.size());
        assertEquals(41L, jobs.get(0).id);
        assertEquals(42L, jobs.get(1).id);
    }

    private static WorkspaceSetModel.Entry liveEntry(String id, long opened) {
        WorkspaceSetModel.Entry e = new WorkspaceSetModel.Entry(); e.id = id; e.name = id; e.lastOpenedAt = opened; return e;
    }

    private static WorkspaceSetModel.Entry find(List<WorkspaceSetModel.Entry> list, String id) {
        for (WorkspaceSetModel.Entry e : list) if (id.equals(e.id)) return e;
        throw new AssertionError("missing " + id);
    }
}
