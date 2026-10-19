package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JobPickerTest {
    private static TranslationRepository.JobSummary job(long id, String status, String hash) {
        TranslationRepository.JobSummary j = new TranslationRepository.JobSummary();
        j.id = id; j.status = status; j.inputHash = hash; j.fileName = "f" + id;
        return j;
    }

    private static List<Long> ids(List<TranslationRepository.JobSummary> l) {
        ArrayList<Long> out = new ArrayList<>();
        for (TranslationRepository.JobSummary j : l) out.add(j.id);
        return out;
    }

    @Test public void currentIsNeverAFinishedCancelledOrDismissedJob() {
        // newest first, like the cache
        List<TranslationRepository.JobSummary> jobs = Arrays.asList(
                job(9, "COMPLETED", "a"), job(8, "done", "b"), job(7, "COMPLETED_WITH_WARNINGS", "c"), job(6, "cancelled", "d"), job(5, "dismissed", "e"), job(4, "INPUT_CONFLICT", "f"));
        assertNull(JobPicker.current(jobs, false, -1));
        assertTrue(JobPicker.unfinished(jobs).isEmpty());
    }

    @Test public void theRunningJobWinsWhileTheServiceWorks() {
        List<TranslationRepository.JobSummary> jobs = Arrays.asList(job(9, "NEEDS_REVIEW", "a"), job(8, "running", "b"), job(7, "paused", "c"));
        assertEquals(8L, JobPicker.current(jobs, true, 8).id);
        assertEquals(8L, JobPicker.current(jobs, true, 99).id);
        assertEquals("service idle: the newest unfinished job", 9L, JobPicker.current(jobs, false, -1).id);
    }

    @Test public void aNewerFinishedJobOfTheSameInputHidesTheLeftover() {
        List<TranslationRepository.JobSummary> jobs = Arrays.asList(
                job(12, "COMPLETED", "same"), job(10, "paused", "other"), job(3, "NEEDS_REVIEW", "same"), job(2, "error", "same"));
        assertEquals(Arrays.asList(10L), ids(JobPicker.unfinished(jobs)));
        assertEquals(10L, JobPicker.current(jobs, false, -1).id);
    }

    @Test public void anOlderFinishedJobDoesNotHideANewerLeftover() {
        List<TranslationRepository.JobSummary> jobs = Arrays.asList(job(9, "paused", "same"), job(4, "COMPLETED", "same"));
        assertEquals(Arrays.asList(9L), ids(JobPicker.unfinished(jobs)));
    }

    @Test public void jobsWithoutAHashAreNeverHidden() {
        List<TranslationRepository.JobSummary> jobs = Arrays.asList(job(9, "COMPLETED", ""), job(4, "paused", ""));
        assertEquals(Arrays.asList(4L), ids(JobPicker.unfinished(jobs)));
    }

    @Test public void aRowLeftRunningAfterACrashStillShows() {
        List<TranslationRepository.JobSummary> jobs = Arrays.asList(job(5, "RUNNING", "a"));
        assertEquals(5L, JobPicker.current(jobs, false, -1).id);
    }
}
