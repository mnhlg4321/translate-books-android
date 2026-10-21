package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JobListMergeTest {
    private static TranslationRepository.JobSummary job(long id, long updated, String name) {
        TranslationRepository.JobSummary out = new TranslationRepository.JobSummary();
        out.id = id;
        out.updatedAt = updated;
        out.fileName = name;
        return out;
    }

    @Test public void allIncludesSingleFileAndWorkspaceJobsWithoutDuplicates() {
        TranslationRepository.JobSummary single = job(101, 300, "single.txt");
        TranslationRepository.JobSummary workspace = job(100, 200, "set.txt");
        TranslationRepository.JobSummary duplicateFromCache = job(99, 100, "old.txt");
        TranslationRepository.JobSummary duplicateFromWorkspace = job(99, 200, "new.txt");

        List<TranslationRepository.JobSummary> merged = JobListMerge.all(
                Arrays.asList(single, duplicateFromCache),
                Arrays.asList(workspace, duplicateFromWorkspace));

        assertEquals(Arrays.asList(101L, 100L, 99L), ids(merged));
        assertEquals("new.txt", merged.get(2).fileName);
    }

    @Test public void equalUpdatedAtKeepsWorkspaceSnapshot() {
        TranslationRepository.JobSummary cached = job(7, 500, "cached.txt");
        TranslationRepository.JobSummary workspace = job(7, 500, "workspace.txt");

        List<TranslationRepository.JobSummary> merged = JobListMerge.all(
                Collections.singletonList(cached), Collections.singletonList(workspace));

        assertEquals(1, merged.size());
        assertSame(workspace, merged.get(0));
    }

    @Test public void currentJobCanUseMergedSingleFileList() {
        TranslationRepository.JobSummary single = job(55, 800, "single.txt");
        single.status = "paused";
        single.inputHash = "single";
        TranslationRepository.JobSummary set = job(54, 700, "set.txt");
        set.status = "COMPLETED";
        set.inputHash = "set";

        List<TranslationRepository.JobSummary> merged = JobListMerge.all(
                Collections.singletonList(single), Collections.singletonList(set));

        assertEquals(55L, JobPicker.current(merged, false, -1L).id);
    }

    private static List<Long> ids(List<TranslationRepository.JobSummary> jobs) {
        java.util.ArrayList<Long> out = new java.util.ArrayList<>();
        for (TranslationRepository.JobSummary job : jobs) out.add(job.id);
        return out;
    }
}
