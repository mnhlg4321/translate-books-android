package com.ml.tblandroidtxt;

/** Keeps the latest run state in prefs/runtime_state.json so a reopened app still shows the unfinished job. */
public final class JsonRunStatePersister implements RunStatePersister {
    private final KeyValueStore store;

    public JsonRunStatePersister(KeyValueStore store) { this.store = store; }

    @Override public void save(String jobState, String status, int progress, String log, String phase, String fileName,
                               int totalChunks, int completed, int failed, int fallbacks,
                               int totalTokens, double totalCost, int estimatedTotalTokens, double estimatedTotalCost,
                               boolean providerUsageComplete, long elapsedMs, long remainingMs, String preview,
                               int currentChunk, int glossaryLocks, int pronounLocks, int lockChunk, String lockPhase,
                               int previewChunk, long activeJobId, String previewFile, int previewTotal) {
        store.edit().putString("jobState", jobState).putString("status", status).putString("phase", phase)
                .putString("fileName", fileName).putLong("progress", progress).putLong("totalChunks", totalChunks)
                .putLong("completed", completed).putLong("failed", failed).putLong("activeJobId", activeJobId)
                .putString("totalCost", String.valueOf(totalCost)).apply();
    }

    @Override public void saveSession(long sessionTokens, double sessionCost, long sessionStartMs, long sessionEndMs,
                                      long pausedAtMs, long pausedTotalMs, int setTotal, int setDone, int setFailed, int setIndex) {
        store.edit().putLong("sessionTokens", sessionTokens).putString("sessionCost", String.valueOf(sessionCost))
                .putLong("sessionStartMs", sessionStartMs).putLong("sessionEndMs", sessionEndMs)
                .putLong("pausedAtMs", pausedAtMs).putLong("pausedTotalMs", pausedTotalMs).apply();
    }
}
