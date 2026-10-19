package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Every spelling of a job status seen in the stored data maps to exactly one kind, and the SQL lists come from the same table. */
public class JobStatusTest {
    private static void kind(JobStatus.Kind expected, String... raws) {
        for (String raw : raws) assertEquals(raw, expected, JobStatus.classify(raw));
    }

    @Test public void everyRawSpellingHasOneKind() {
        kind(JobStatus.Kind.ACTIVE, "RUNNING", "running", "READY", " Running ");
        kind(JobStatus.Kind.CONTINUABLE, "paused", "PAUSED", "error", "ERROR", "RETRYABLE_ERROR", "FAILED_INTEGRITY_CHECK");
        kind(JobStatus.Kind.NEEDS_REVIEW, "NEEDS_REVIEW", "needs_review");
        kind(JobStatus.Kind.CONFLICT, "INPUT_CONFLICT");
        kind(JobStatus.Kind.FINISHED, "done", "DONE", "COMPLETED", "COMPLETED_WITH_WARNINGS", "PASSED", "INTEGRITY_PASSED");
        kind(JobStatus.Kind.CANCELLED, "cancelled", "CANCELLED");
        kind(JobStatus.Kind.DISMISSED, "dismissed", JobStatus.DISMISSED_VALUE, "DISMISSED");
        kind(JobStatus.Kind.OTHER, null, "", "   ", "SUPERSEDED", "something-new");
    }

    @Test public void aFinishedJobIsNeverShownAsUnfinished() {
        for (String raw : new String[]{"done", "COMPLETED", "COMPLETED_WITH_WARNINGS", "PASSED", "cancelled", "dismissed", "INPUT_CONFLICT"}) {
            assertFalse(raw, JobStatus.is(raw, JobStatus.Kind.ACTIVE, JobStatus.Kind.CONTINUABLE, JobStatus.Kind.NEEDS_REVIEW));
        }
        for (String raw : new String[]{"running", "paused", "error", "NEEDS_REVIEW", "FAILED_INTEGRITY_CHECK"}) {
            assertTrue(raw, JobStatus.is(raw, JobStatus.Kind.ACTIVE, JobStatus.Kind.CONTINUABLE, JobStatus.Kind.NEEDS_REVIEW));
        }
    }

    @Test public void sqlListsAreBuiltFromTheTable() {
        String shown = JobStatus.sqlUnfinishedShown();
        for (String v : new String[]{"RUNNING", "READY", "PAUSED", "ERROR", "RETRYABLE_ERROR", "FAILED_INTEGRITY_CHECK", "NEEDS_REVIEW"}) assertTrue(v, shown.contains("'" + v + "'"));
        for (String v : new String[]{"DONE", "COMPLETED", "DISMISSED", "CANCELLED", "INPUT_CONFLICT"}) assertFalse(v, shown.contains("'" + v + "'"));
        String fin = JobStatus.sqlFinished();
        assertTrue(fin.contains("'DONE'") && fin.contains("'COMPLETED'") && fin.contains("'COMPLETED_WITH_WARNINGS'"));
        assertFalse(fin.contains("'PAUSED'"));
    }
}
