package com.ml.tblandroidtxt;

/** Persists the run state shown after the UI was away; same parameters as RuntimeStateStore.save/saveSession. */
public interface RunStatePersister {
    void save(String jobState, String status, int progress, String log, String phase, String fileName,
              int totalChunks, int completed, int failed, int fallbacks,
              int totalTokens, double totalCost, int estimatedTotalTokens, double estimatedTotalCost,
              boolean providerUsageComplete, long elapsedMs, long remainingMs, String preview,
              int currentChunk, int glossaryLocks, int pronounLocks, int lockChunk, String lockPhase,
              int previewChunk, long activeJobId, String previewFile, int previewTotal);

    void saveSession(long sessionTokens, double sessionCost, long sessionStartMs,
                     long sessionEndMs, long pausedAtMs, long pausedTotalMs,
                     int setTotal, int setDone, int setFailed, int setIndex);
}
