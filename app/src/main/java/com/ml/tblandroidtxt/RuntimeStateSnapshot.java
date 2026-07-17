package com.ml.tblandroidtxt;

import java.util.HashMap;
import java.util.Map;

/** Immutable, Android-free runtime snapshot schema shared by service, store and tests. */
public final class RuntimeStateSnapshot {
    public String jobState = "IDLE", status = "", log = "", phase = "", fileName = "", preview = "";
    public int progress = -1, totalChunks, completed, failed, fallbacks, totalTokens, estimatedTotalTokens, currentChunk;
    public double totalCost, estimatedTotalCost;
    public boolean providerUsageComplete;
    public long elapsedMs, remainingMs, activeJobId = -1L;

    public Map<String, Object> toMap() {
        HashMap<String, Object> m = new HashMap<>();
        m.put("jobState",jobState);m.put("status", status); m.put("log", log); m.put("phase", phase); m.put("fileName", fileName);
        m.put("preview", preview); m.put("progress", progress); m.put("totalChunks", totalChunks);
        m.put("completed", completed); m.put("failed", failed); m.put("fallbacks", fallbacks);
        m.put("totalTokens", totalTokens); m.put("estimatedTotalTokens", estimatedTotalTokens); m.put("currentChunk", currentChunk); m.put("totalCost", totalCost);
        m.put("estimatedTotalCost", estimatedTotalCost); m.put("providerUsageComplete", providerUsageComplete);
        m.put("elapsedMs", elapsedMs); m.put("remainingMs", remainingMs); m.put("activeJobId", activeJobId);
        return m;
    }

    public static RuntimeStateSnapshot fromMap(Map<String, Object> m) {
        RuntimeStateSnapshot s = new RuntimeStateSnapshot();
        if (m == null) return s;
        s.jobState=string(m.get("jobState"));if(s.jobState.isEmpty())s.jobState="IDLE";s.status = string(m.get("status")); s.log = string(m.get("log")); s.phase = string(m.get("phase"));
        s.fileName = string(m.get("fileName")); s.preview = string(m.get("preview"));
        s.progress = integer(m.get("progress"), -1); s.totalChunks = integer(m.get("totalChunks"), 0);
        s.completed = integer(m.get("completed"), 0); s.failed = integer(m.get("failed"), 0);
        s.fallbacks = integer(m.get("fallbacks"), 0); s.totalTokens = integer(m.get("totalTokens"), 0);
        s.estimatedTotalTokens = integer(m.get("estimatedTotalTokens"), 0);
        s.currentChunk = integer(m.get("currentChunk"), 0); s.totalCost = decimal(m.get("totalCost"), 0);
        s.estimatedTotalCost = decimal(m.get("estimatedTotalCost"), 0);
        s.providerUsageComplete = bool(m.get("providerUsageComplete"));
        s.elapsedMs = longValue(m.get("elapsedMs"), 0); s.remainingMs = longValue(m.get("remainingMs"), 0);
        s.activeJobId = longValue(m.get("activeJobId"), -1);
        return s;
    }

    private static String string(Object v) { return v == null ? "" : String.valueOf(v); }
    private static int integer(Object v, int d) { return v instanceof Number ? ((Number) v).intValue() : d; }
    private static long longValue(Object v, long d) { return v instanceof Number ? ((Number) v).longValue() : d; }
    private static double decimal(Object v, double d) { return v instanceof Number ? ((Number) v).doubleValue() : d; }
    private static boolean bool(Object v) { return v instanceof Boolean && (Boolean) v; }
}
