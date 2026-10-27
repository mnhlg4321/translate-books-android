package com.ml.tblandroidtxt;

/**
 * Everything the old TranslatorService.broadcast() put into the progress Intent, captured at one moment. The app turns it
 * into an Intent with the same keys, types and values; {@link #FIELDS} lists (Intent key, type, field) for every extra.
 */
public final class RunSnapshot {
    public final String status;
    public final int progress;
    /** The log text given to publish (may be null); the Intent carries it as "log". */
    public final String log;
    /** The last non-blank log text; RuntimeStateStore keeps this one. */
    public final String metricLastLog;
    public final String phase;
    public final String fileName;
    public final int totalChunks;
    public final int completed;
    public final int failed;
    public final int fallbacks;
    public final int totalTokens;
    public final double totalCost;
    public final int estimatedTotalTokens;
    public final double estimatedTotalCost;
    public final boolean providerUsageComplete;
    public final long elapsedMs;
    public final long remainingMs;
    public final String preview;
    public final int currentChunk;
    public final int glossaryLocks;
    public final int pronounLocks;
    public final int lockChunk;
    public final String lockPhase;
    public final int previewChunk;
    public final String previewFile;
    public final int previewTotal;
    public final long activeJobId;
    public final String state;
    public final long sessionTokens;
    public final double sessionCost;
    public final long sessionStartMs;
    public final long sessionEndMs;
    public final long pausedAtMs;
    public final long pausedTotalMs;
    public final int setTotal;
    public final int setDone;
    public final int setFailed;
    public final int setIndex;

    /** {Intent extra key, Java type, RunSnapshot field}, in the order the old broadcast() added them. */
    public static final String[][] FIELDS = {
            {"status", "String", "status"},
            {"progress", "int", "progress"},
            {"log", "String", "log"},
            {"phase", "String", "phase"},
            {"fileName", "String", "fileName"},
            {"totalChunks", "int", "totalChunks"},
            {"completed", "int", "completed"},
            {"failed", "int", "failed"},
            {"fallbacks", "int", "fallbacks"},
            {"totalTokens", "int", "totalTokens"},
            {"totalCost", "double", "totalCost"},
            {"estimatedTotalTokens", "int", "estimatedTotalTokens"},
            {"estimatedTotalCost", "double", "estimatedTotalCost"},
            {"providerUsageComplete", "boolean", "providerUsageComplete"},
            {"elapsedMs", "long", "elapsedMs"},
            {"remainingMs", "long", "remainingMs"},
            {"preview", "String", "preview"},
            {"currentChunk", "int", "currentChunk"},
            {"glossaryLocks", "int", "glossaryLocks"},
            {"pronounLocks", "int", "pronounLocks"},
            {"lockChunk", "int", "lockChunk"},
            {"lockPhase", "String", "lockPhase"},
            {"previewChunk", "int", "previewChunk"},
            {"previewFile", "String", "previewFile"},
            {"previewTotal", "int", "previewTotal"},
            {"activeJobId", "long", "activeJobId"},
            {"jobState", "String", "state"},
            {"sessionTokens", "long", "sessionTokens"},
            {"sessionCost", "double", "sessionCost"},
            {"sessionStartMs", "long", "sessionStartMs"},
            {"sessionEndMs", "long", "sessionEndMs"},
            {"pausedAtMs", "long", "pausedAtMs"},
            {"pausedTotalMs", "long", "pausedTotalMs"},
            {"setTotal", "int", "setTotal"},
            {"setDone", "int", "setDone"},
            {"setFailed", "int", "setFailed"},
            {"setIndex", "int", "setIndex"},
    };

    @SuppressWarnings("checkstyle:ParameterNumber")
    RunSnapshot(String status, int progress, String log, String metricLastLog, String phase, String fileName,
                int totalChunks, int completed, int failed, int fallbacks, int totalTokens, double totalCost,
                int estimatedTotalTokens, double estimatedTotalCost, boolean providerUsageComplete,
                long elapsedMs, long remainingMs, String preview, int currentChunk, int glossaryLocks,
                int pronounLocks, int lockChunk, String lockPhase, int previewChunk, String previewFile,
                int previewTotal, long activeJobId, String state, long sessionTokens, double sessionCost,
                long sessionStartMs, long sessionEndMs, long pausedAtMs, long pausedTotalMs,
                int setTotal, int setDone, int setFailed, int setIndex) {
        this.status = status;
        this.progress = progress;
        this.log = log;
        this.metricLastLog = metricLastLog;
        this.phase = phase;
        this.fileName = fileName;
        this.totalChunks = totalChunks;
        this.completed = completed;
        this.failed = failed;
        this.fallbacks = fallbacks;
        this.totalTokens = totalTokens;
        this.totalCost = totalCost;
        this.estimatedTotalTokens = estimatedTotalTokens;
        this.estimatedTotalCost = estimatedTotalCost;
        this.providerUsageComplete = providerUsageComplete;
        this.elapsedMs = elapsedMs;
        this.remainingMs = remainingMs;
        this.preview = preview;
        this.currentChunk = currentChunk;
        this.glossaryLocks = glossaryLocks;
        this.pronounLocks = pronounLocks;
        this.lockChunk = lockChunk;
        this.lockPhase = lockPhase;
        this.previewChunk = previewChunk;
        this.previewFile = previewFile;
        this.previewTotal = previewTotal;
        this.activeJobId = activeJobId;
        this.state = state;
        this.sessionTokens = sessionTokens;
        this.sessionCost = sessionCost;
        this.sessionStartMs = sessionStartMs;
        this.sessionEndMs = sessionEndMs;
        this.pausedAtMs = pausedAtMs;
        this.pausedTotalMs = pausedTotalMs;
        this.setTotal = setTotal;
        this.setDone = setDone;
        this.setFailed = setFailed;
        this.setIndex = setIndex;
    }
}
