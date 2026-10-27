package com.ml.tblandroidtxt;

/** Document access port for the run loop. A ref is the URI string as stored in the job row. */
public interface DocumentIO {
    String readText(String ref) throws Exception;

    String displayName(String ref);

    /** Writes the text and verifies what was written. */
    void writeTextVerified(String ref, String text) throws Exception;

    void writeText(String ref, String text) throws Exception;

    /**
     * Turns the job's output reference into a concrete writable one (a pending folder output becomes a created file and
     * the job row is updated); returns the ref to write to.
     */
    String resolveOutput(long jobId, String ref) throws Exception;
}
