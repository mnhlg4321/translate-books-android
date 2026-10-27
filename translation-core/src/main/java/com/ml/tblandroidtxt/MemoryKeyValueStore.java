package com.ml.tblandroidtxt;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory KeyValueStore; edits become visible on commit/apply, as with SharedPreferences. */
public final class MemoryKeyValueStore implements KeyValueStore {
    private final HashMap<String, Object> values = new HashMap<>();

    @Override public synchronized String getString(String key, String defaultValue) {
        Object v = values.get(key);
        return v instanceof String ? (String) v : defaultValue;
    }

    @Override public synchronized boolean getBoolean(String key, boolean defaultValue) {
        Object v = values.get(key);
        return v instanceof Boolean ? (Boolean) v : defaultValue;
    }

    @Override public synchronized Map<String, ?> getAll() { return new LinkedHashMap<>(values); }

    @Override public Editor edit() { return new MemoryEditor(); }

    private final class MemoryEditor implements Editor {
        private final LinkedHashMap<String, Object> pending = new LinkedHashMap<>();
        private static final Object REMOVED = new Object();

        @Override public Editor putString(String key, String value) { pending.put(key, value == null ? REMOVED : value); return this; }
        @Override public Editor putBoolean(String key, boolean value) { pending.put(key, value); return this; }
        @Override public Editor putLong(String key, long value) { pending.put(key, value); return this; }
        @Override public Editor remove(String key) { pending.put(key, REMOVED); return this; }

        @Override public boolean commit() {
            synchronized (MemoryKeyValueStore.this) {
                for (Map.Entry<String, Object> e : pending.entrySet()) {
                    if (e.getValue() == REMOVED) values.remove(e.getKey());
                    else values.put(e.getKey(), e.getValue());
                }
            }
            return true;
        }

        @Override public void apply() { commit(); }
    }
}
