package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/**
 * Which jobs the Jobs tab shows as unfinished and which one is the "Current job". The rules mirror the SQL in
 * {@code TranslationRepository.getIncompleteJobs} (the card on the Translate screen): a finished job never counts as unfinished, a
 * dismissed or cancelled one is out, and an unfinished job is hidden once a newer job for the same input has finished (the owner
 * translated that file again, so the old leftover is no longer something to continue). Nothing is written when hiding.
 */
final class JobPicker {
    private JobPicker() {}

    /** {@code jobs} newest first. */
    static List<TranslationRepository.JobSummary> unfinished(List<TranslationRepository.JobSummary> jobs) {
        ArrayList<TranslationRepository.JobSummary> out = new ArrayList<>();
        for (TranslationRepository.JobSummary j : jobs) {
            if (!JobStatus.is(j.status, JobStatus.Kind.ACTIVE, JobStatus.Kind.CONTINUABLE, JobStatus.Kind.NEEDS_REVIEW)) continue;
            if (supersededByFinished(j, jobs)) continue;
            out.add(j);
        }
        return out;
    }

    static boolean supersededByFinished(TranslationRepository.JobSummary job, List<TranslationRepository.JobSummary> all) {
        if (job.inputHash == null || job.inputHash.isEmpty()) return false;
        for (TranslationRepository.JobSummary f : all) {
            if (f.id > job.id && job.inputHash.equals(f.inputHash) && JobStatus.classify(f.status) == JobStatus.Kind.FINISHED) return true;
        }
        return false;
    }

    /**
     * The job running now when the service is working, otherwise the newest unfinished one. Never a finished, cancelled or dismissed job.
     */
    static TranslationRepository.JobSummary current(List<TranslationRepository.JobSummary> jobs, boolean serviceActive, long activeJobId) {
        List<TranslationRepository.JobSummary> open = unfinished(jobs);
        if (serviceActive) {
            for (TranslationRepository.JobSummary j : open) if (j.id == activeJobId) return j;
            for (TranslationRepository.JobSummary j : open) if (JobStatus.classify(j.status) == JobStatus.Kind.ACTIVE) return j;
        }
        // A row still marked running while no service works (the process died) is shown like any other unfinished job.
        return open.isEmpty() ? null : open.get(0);
    }
}
