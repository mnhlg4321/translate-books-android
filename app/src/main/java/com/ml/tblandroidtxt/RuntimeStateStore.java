package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class RuntimeStateStore {
    private static final String PREF = "runtime_state";

    public static void clear(Context c) {
        if (c != null) c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static void save(Context c, String jobState, String status, int progress, String log, String phase, String fileName,
                            int totalChunks, int completed, int failed, int fallbacks,
                            int totalTokens, double totalCost, int estimatedTotalTokens, double estimatedTotalCost,
                            boolean providerUsageComplete, long elapsedMs, long remainingMs, String preview,
                            int currentChunk, long activeJobId) {
        RuntimeStateSnapshot s = new RuntimeStateSnapshot();
        s.jobState=nz(jobState);s.status = nz(status); s.progress = progress; s.log = nz(log); s.phase = nz(phase); s.fileName = nz(fileName);
        s.totalChunks = totalChunks; s.completed = completed; s.failed = failed; s.fallbacks = fallbacks;
        s.totalTokens = totalTokens; s.totalCost = totalCost; s.estimatedTotalTokens = estimatedTotalTokens;
        s.estimatedTotalCost = estimatedTotalCost; s.providerUsageComplete = providerUsageComplete;
        s.elapsedMs = elapsedMs; s.remainingMs = remainingMs;
        s.preview = nz(preview); s.currentChunk = currentChunk; s.activeJobId = activeJobId;
        save(c, s);
    }

    public static void save(Context c, RuntimeStateSnapshot s) {
        if (c == null || s == null) return;
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(TranslatorService.EXTRA_STATE,nz(s.jobState))
                .putString("appVersion",AppBuildInfo.VERSION_NAME)
                .putString(TranslatorService.EXTRA_STATUS, nz(s.status))
                .putInt(TranslatorService.EXTRA_PROGRESS, s.progress)
                .putString(TranslatorService.EXTRA_LOG, nz(s.log))
                .putString(TranslatorService.EXTRA_PHASE, nz(s.phase))
                .putString(TranslatorService.EXTRA_FILE_NAME, nz(s.fileName))
                .putInt(TranslatorService.EXTRA_TOTAL_CHUNKS, s.totalChunks)
                .putInt(TranslatorService.EXTRA_COMPLETED, s.completed)
                .putInt(TranslatorService.EXTRA_FAILED, s.failed)
                .putInt(TranslatorService.EXTRA_FALLBACKS, s.fallbacks)
                .putInt(TranslatorService.EXTRA_TOTAL_TOKENS, s.totalTokens)
                .putFloat(TranslatorService.EXTRA_TOTAL_COST, (float) s.totalCost)
                .putInt(TranslatorService.EXTRA_ESTIMATED_TOTAL_TOKENS, s.estimatedTotalTokens)
                .putFloat(TranslatorService.EXTRA_ESTIMATED_TOTAL_COST, (float) s.estimatedTotalCost)
                .putBoolean(TranslatorService.EXTRA_PROVIDER_USAGE_COMPLETE, s.providerUsageComplete)
                .putLong(TranslatorService.EXTRA_ELAPSED_MS, s.elapsedMs)
                .putLong(TranslatorService.EXTRA_REMAINING_MS, s.remainingMs)
                .putString(TranslatorService.EXTRA_PREVIEW, nz(s.preview))
                .putInt(TranslatorService.EXTRA_CURRENT_CHUNK, s.currentChunk)
                .putLong(TranslatorService.EXTRA_ACTIVE_JOB_ID, s.activeJobId)
                .putLong("updatedAt", System.currentTimeMillis())
                .apply();
    }

    public static Intent toIntent(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!p.contains("updatedAt")) return null;
        if (!AppBuildInfo.VERSION_NAME.equals(p.getString("appVersion", ""))) return null;
        Intent i = new Intent(TranslatorService.ACTION_PROGRESS);
        i.putExtra(TranslatorService.EXTRA_STATE,p.getString(TranslatorService.EXTRA_STATE,"IDLE"));
        i.putExtra(TranslatorService.EXTRA_STATUS, p.getString(TranslatorService.EXTRA_STATUS, ""));
        i.putExtra(TranslatorService.EXTRA_PROGRESS, p.getInt(TranslatorService.EXTRA_PROGRESS, -1));
        i.putExtra(TranslatorService.EXTRA_LOG, p.getString(TranslatorService.EXTRA_LOG, ""));
        i.putExtra(TranslatorService.EXTRA_PHASE, p.getString(TranslatorService.EXTRA_PHASE, ""));
        i.putExtra(TranslatorService.EXTRA_FILE_NAME, p.getString(TranslatorService.EXTRA_FILE_NAME, ""));
        i.putExtra(TranslatorService.EXTRA_TOTAL_CHUNKS, p.getInt(TranslatorService.EXTRA_TOTAL_CHUNKS, 0));
        i.putExtra(TranslatorService.EXTRA_COMPLETED, p.getInt(TranslatorService.EXTRA_COMPLETED, 0));
        i.putExtra(TranslatorService.EXTRA_FAILED, p.getInt(TranslatorService.EXTRA_FAILED, 0));
        i.putExtra(TranslatorService.EXTRA_FALLBACKS, p.getInt(TranslatorService.EXTRA_FALLBACKS, 0));
        i.putExtra(TranslatorService.EXTRA_TOTAL_TOKENS, p.getInt(TranslatorService.EXTRA_TOTAL_TOKENS, 0));
        i.putExtra(TranslatorService.EXTRA_TOTAL_COST, (double) p.getFloat(TranslatorService.EXTRA_TOTAL_COST, 0f));
        i.putExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_TOKENS, p.getInt(TranslatorService.EXTRA_ESTIMATED_TOTAL_TOKENS, 0));
        i.putExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_COST, (double) p.getFloat(TranslatorService.EXTRA_ESTIMATED_TOTAL_COST, 0f));
        i.putExtra(TranslatorService.EXTRA_PROVIDER_USAGE_COMPLETE, p.getBoolean(TranslatorService.EXTRA_PROVIDER_USAGE_COMPLETE, false));
        i.putExtra(TranslatorService.EXTRA_ELAPSED_MS, p.getLong(TranslatorService.EXTRA_ELAPSED_MS, 0L));
        i.putExtra(TranslatorService.EXTRA_REMAINING_MS, p.getLong(TranslatorService.EXTRA_REMAINING_MS, 0L));
        i.putExtra(TranslatorService.EXTRA_PREVIEW, p.getString(TranslatorService.EXTRA_PREVIEW, ""));
        i.putExtra(TranslatorService.EXTRA_CURRENT_CHUNK, p.getInt(TranslatorService.EXTRA_CURRENT_CHUNK, 0));
        i.putExtra(TranslatorService.EXTRA_ACTIVE_JOB_ID, p.getLong(TranslatorService.EXTRA_ACTIVE_JOB_ID, -1L));
        return i;
    }

    public static String describe(Context c) {
        Intent i = toIntent(c);
        if (i == null) return "No runtime state saved.";
        StringBuilder sb = new StringBuilder();
        sb.append("status=").append(i.getStringExtra(TranslatorService.EXTRA_STATUS)).append('\n');
        sb.append("progress=").append(i.getIntExtra(TranslatorService.EXTRA_PROGRESS, -1)).append('\n');
        sb.append("phase=").append(i.getStringExtra(TranslatorService.EXTRA_PHASE)).append('\n');
        sb.append("file=").append(i.getStringExtra(TranslatorService.EXTRA_FILE_NAME)).append('\n');
        sb.append("chunks=").append(i.getIntExtra(TranslatorService.EXTRA_COMPLETED, 0)).append('/')
                .append(i.getIntExtra(TranslatorService.EXTRA_TOTAL_CHUNKS, 0)).append('\n');
        sb.append("failed=").append(i.getIntExtra(TranslatorService.EXTRA_FAILED, 0)).append('\n');
        sb.append("fallbacks=").append(i.getIntExtra(TranslatorService.EXTRA_FALLBACKS, 0)).append('\n');
        sb.append("tokens=").append(i.getIntExtra(TranslatorService.EXTRA_TOTAL_TOKENS, 0)).append('\n');
        sb.append("currentChunk=").append(i.getIntExtra(TranslatorService.EXTRA_CURRENT_CHUNK, 0)).append('\n');
        sb.append("activeJobId=").append(i.getLongExtra(TranslatorService.EXTRA_ACTIVE_JOB_ID, -1L)).append('\n');
        sb.append("cost=").append(i.getDoubleExtra(TranslatorService.EXTRA_TOTAL_COST, 0d)).append('\n');
        sb.append("lastLog=").append(i.getStringExtra(TranslatorService.EXTRA_LOG)).append('\n');
        String preview = i.getStringExtra(TranslatorService.EXTRA_PREVIEW);
        if (preview != null && !preview.trim().isEmpty()) {
            sb.append("lastPreviewChars=").append(preview.length()).append('\n');
            sb.append("lastPreviewHash=").append(HashUtil.sha256(preview)).append('\n');
            sb.append("lastPreviewContent=omitted_for_privacy\n");
        }
        return sb.toString();
    }

    private static String nz(String v) { return v == null ? "" : v; }
}
