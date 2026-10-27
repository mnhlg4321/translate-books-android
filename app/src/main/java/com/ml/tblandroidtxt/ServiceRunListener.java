package com.ml.tblandroidtxt;

import android.app.NotificationManager;
import android.content.Intent;

/** Turns run-loop events back into the former service behaviour: progress broadcast, notification and teardown. */
final class ServiceRunListener implements RunListener {
    private final TranslatorService service;

    ServiceRunListener(TranslatorService service) { this.service = service; }

    @Override public void onSnapshot(RunSnapshot s) {
        Intent i = new Intent(TranslatorService.ACTION_PROGRESS);
        i.setPackage(service.getPackageName());
        i.putExtra(TranslatorService.EXTRA_STATUS, s.status);
        i.putExtra(TranslatorService.EXTRA_PROGRESS, s.progress);
        i.putExtra(TranslatorService.EXTRA_LOG, s.log);
        i.putExtra(TranslatorService.EXTRA_PHASE, s.phase);
        i.putExtra(TranslatorService.EXTRA_FILE_NAME, s.fileName);
        i.putExtra(TranslatorService.EXTRA_TOTAL_CHUNKS, s.totalChunks);
        i.putExtra(TranslatorService.EXTRA_COMPLETED, s.completed);
        i.putExtra(TranslatorService.EXTRA_FAILED, s.failed);
        i.putExtra(TranslatorService.EXTRA_FALLBACKS, s.fallbacks);
        i.putExtra(TranslatorService.EXTRA_TOTAL_TOKENS, s.totalTokens);
        i.putExtra(TranslatorService.EXTRA_TOTAL_COST, s.totalCost);
        i.putExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_TOKENS, s.estimatedTotalTokens);
        i.putExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_COST, s.estimatedTotalCost);
        i.putExtra(TranslatorService.EXTRA_PROVIDER_USAGE_COMPLETE, s.providerUsageComplete);
        i.putExtra(TranslatorService.EXTRA_ELAPSED_MS, s.elapsedMs);
        i.putExtra(TranslatorService.EXTRA_REMAINING_MS, s.remainingMs);
        i.putExtra(TranslatorService.EXTRA_PREVIEW, s.preview);
        i.putExtra(TranslatorService.EXTRA_CURRENT_CHUNK, s.currentChunk);
        i.putExtra(TranslatorService.EXTRA_GLOSSARY_LOCKS, s.glossaryLocks);
        i.putExtra(TranslatorService.EXTRA_PRONOUN_LOCKS, s.pronounLocks);
        i.putExtra(TranslatorService.EXTRA_LOCK_CHUNK, s.lockChunk);
        i.putExtra(TranslatorService.EXTRA_LOCK_PHASE, s.lockPhase);
        i.putExtra(TranslatorService.EXTRA_PREVIEW_CHUNK, s.previewChunk);
        i.putExtra(TranslatorService.EXTRA_PREVIEW_FILE, s.previewFile);
        i.putExtra(TranslatorService.EXTRA_PREVIEW_TOTAL, s.previewTotal);
        i.putExtra(TranslatorService.EXTRA_ACTIVE_JOB_ID, s.activeJobId);
        i.putExtra(TranslatorService.EXTRA_STATE, s.state);
        i.putExtra(TranslatorService.EXTRA_SESSION_TOKENS, s.sessionTokens);
        i.putExtra(TranslatorService.EXTRA_SESSION_COST, s.sessionCost);
        i.putExtra(TranslatorService.EXTRA_SESSION_START_MS, s.sessionStartMs);
        i.putExtra(TranslatorService.EXTRA_SESSION_END_MS, s.sessionEndMs);
        i.putExtra(TranslatorService.EXTRA_PAUSED_AT_MS, s.pausedAtMs);
        i.putExtra(TranslatorService.EXTRA_PAUSED_TOTAL_MS, s.pausedTotalMs);
        i.putExtra(TranslatorService.EXTRA_SET_TOTAL, s.setTotal);
        i.putExtra(TranslatorService.EXTRA_SET_DONE, s.setDone);
        i.putExtra(TranslatorService.EXTRA_SET_FAILED, s.setFailed);
        i.putExtra(TranslatorService.EXTRA_SET_INDEX, s.setIndex);
        service.sendBroadcast(i);
    }

    @Override public void onNotification(String text, int done, int total) { service.updateNotification(text, done, total); }

    @Override public void onRunEnded() {
        service.releaseRun();
        service.stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE);
        NotificationManager nm = service.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(TranslatorService.NOTIFY_ID);
        service.stopSelf();
    }

    @Override public void onCancelledEnd(String message) { service.finishCancelled(message); }
}
