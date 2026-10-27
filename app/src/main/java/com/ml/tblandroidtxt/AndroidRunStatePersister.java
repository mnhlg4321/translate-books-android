package com.ml.tblandroidtxt;

import android.content.Context;

/** Forwards to RuntimeStateStore with the service context. */
final class AndroidRunStatePersister implements RunStatePersister {
    private final Context context;

    AndroidRunStatePersister(Context context) { this.context = context; }

    @Override public void save(String jobState, String status, int progress, String log, String phase, String fileName,
                               int totalChunks, int completed, int failed, int fallbacks,
                               int totalTokens, double totalCost, int estimatedTotalTokens, double estimatedTotalCost,
                               boolean providerUsageComplete, long elapsedMs, long remainingMs, String preview,
                               int currentChunk, int glossaryLocks, int pronounLocks, int lockChunk, String lockPhase,
                               int previewChunk, long activeJobId, String previewFile, int previewTotal) {
        RuntimeStateStore.save(context, jobState, status, progress, log, phase, fileName,
                totalChunks, completed, failed, fallbacks, totalTokens, totalCost, estimatedTotalTokens, estimatedTotalCost,
                providerUsageComplete, elapsedMs, remainingMs, preview, currentChunk, glossaryLocks, pronounLocks, lockChunk,
                lockPhase, previewChunk, activeJobId, previewFile, previewTotal);
    }

    @Override public void saveSession(long sessionTokens, double sessionCost, long sessionStartMs,
                                      long sessionEndMs, long pausedAtMs, long pausedTotalMs,
                                      int setTotal, int setDone, int setFailed, int setIndex) {
        RuntimeStateStore.saveSession(context, sessionTokens, sessionCost, sessionStartMs,
                sessionEndMs, pausedAtMs, pausedTotalMs, setTotal, setDone, setFailed, setIndex);
    }
}
