package com.ml.tblandroidtxt;

import java.util.HashMap;
import java.util.Map;

/** In-memory per-run request budget keyed by the stable identity of a root chunk. */
final class RootChunkAttemptBudget {
    private final int maximum;
    private final Map<String, Integer> counts = new HashMap<>();

    RootChunkAttemptBudget(int maximum) { this.maximum = Math.max(1, maximum); }

    synchronized int claim(String rootStableId) {
        String key = rootStableId == null ? "" : rootStableId;
        int current = counts.containsKey(key) ? counts.get(key) : 0;
        if (current >= maximum) return -1;
        int next = current + 1;
        counts.put(key, next);
        return next;
    }

    synchronized void reset() { counts.clear(); }
}
