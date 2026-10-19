package com.ml.tblandroidtxt;

import java.util.Locale;

/**
 * One place that says what a job's stored status means. The status column holds several spellings written over the releases
 * ("done" from the service, "COMPLETED" from the integrity audit, "paused"/"PAUSED", ...); every list that decides whether a job is
 * finished, can continue or must be hidden reads it from here, so the lists cannot drift apart.
 */
final class JobStatus {
    private JobStatus() {}

    enum Kind {
        /** The service is working on it. */
        ACTIVE("RUNNING", "READY"),
        /** Stopped with chunks left; the owner can continue it. */
        CONTINUABLE("PAUSED", "ERROR", "RETRYABLE_ERROR", "FAILED_INTEGRITY_CHECK"),
        /** A chunk has a request of unknown delivery; the owner must look before anything is sent again. */
        NEEDS_REVIEW("NEEDS_REVIEW"),
        /** The input no longer matches what the job was made from. */
        CONFLICT("INPUT_CONFLICT"),
        /** Finished and verified. */
        FINISHED("DONE", "COMPLETED", "COMPLETED_WITH_WARNINGS", "PASSED", "INTEGRITY_PASSED"),
        CANCELLED("CANCELLED"),
        /** The owner dismissed it from the unfinished-job card; nothing was deleted. */
        DISMISSED("DISMISSED"),
        /** Empty or a spelling this version does not know. */
        OTHER();

        private final String[] upper;

        Kind(String... upper) { this.upper = upper; }

        String[] spellings() { return upper.clone(); }
    }

    static final String DISMISSED_VALUE = "dismissed";

    static Kind classify(String raw) {
        if (raw == null) return Kind.OTHER;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) return Kind.OTHER;
        for (Kind k : Kind.values()) for (String v : k.upper) if (v.equals(s)) return k;
        return Kind.OTHER;
    }

    static boolean is(String raw, Kind... kinds) {
        Kind k = classify(raw);
        for (Kind x : kinds) if (x == k) return true;
        return false;
    }

    /** Quoted, comma separated upper-case spellings for {@code UPPER(status) IN (...)}. */
    static String sqlIn(Kind... kinds) {
        StringBuilder sb = new StringBuilder();
        for (Kind k : kinds) for (String v : k.upper) { if (sb.length() > 0) sb.append(','); sb.append('\'').append(v).append('\''); }
        return sb.toString();
    }

    /** Jobs that can still be worked on from the unfinished-job card or the Current job panel. */
    static String sqlUnfinishedShown() { return sqlIn(Kind.ACTIVE, Kind.CONTINUABLE, Kind.NEEDS_REVIEW); }

    static String sqlFinished() { return sqlIn(Kind.FINISHED); }
}
