package com.ml.tblandroidtxt;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** In-memory preferences for host tests; {@link #failCommits} makes every commit report failure without writing. */
final class FakeSharedPreferences implements SharedPreferences {
    final Map<String, Object> values = new HashMap<>();
    boolean failCommits;
    int commits;

    @Override public Map<String, ?> getAll() { return new HashMap<>(values); }
    @Override public String getString(String key, String defValue) { Object v = values.get(key); return v instanceof String ? (String) v : defValue; }
    @SuppressWarnings("unchecked")
    @Override public Set<String> getStringSet(String key, Set<String> defValues) { Object v = values.get(key); return v instanceof Set ? (Set<String>) v : defValues; }
    @Override public int getInt(String key, int defValue) { Object v = values.get(key); return v instanceof Integer ? (Integer) v : defValue; }
    @Override public long getLong(String key, long defValue) { Object v = values.get(key); return v instanceof Long ? (Long) v : defValue; }
    @Override public float getFloat(String key, float defValue) { Object v = values.get(key); return v instanceof Float ? (Float) v : defValue; }
    @Override public boolean getBoolean(String key, boolean defValue) { Object v = values.get(key); return v instanceof Boolean ? (Boolean) v : defValue; }
    @Override public boolean contains(String key) { return values.containsKey(key); }
    @Override public Editor edit() { return new FakeEditor(); }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }

    private final class FakeEditor implements Editor {
        private final Map<String, Object> pending = new HashMap<>();
        private boolean clear;

        @Override public Editor putString(String key, String value) { pending.put(key, value); return this; }
        @Override public Editor putStringSet(String key, Set<String> values) { pending.put(key, values); return this; }
        @Override public Editor putInt(String key, int value) { pending.put(key, value); return this; }
        @Override public Editor putLong(String key, long value) { pending.put(key, value); return this; }
        @Override public Editor putFloat(String key, float value) { pending.put(key, value); return this; }
        @Override public Editor putBoolean(String key, boolean value) { pending.put(key, value); return this; }
        @Override public Editor remove(String key) { pending.put(key, null); return this; }
        @Override public Editor clear() { clear = true; return this; }
        @Override public boolean commit() {
            if (failCommits) return false;
            if (clear) values.clear();
            for (Map.Entry<String, Object> e : pending.entrySet()) {
                if (e.getValue() == null) values.remove(e.getKey()); else values.put(e.getKey(), e.getValue());
            }
            commits++;
            return true;
        }
        @Override public void apply() { commit(); }
    }
}
