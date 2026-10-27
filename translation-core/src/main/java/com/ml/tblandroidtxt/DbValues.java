package com.ml.tblandroidtxt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Column values for insert/update, a subset of android.content.ContentValues that keeps each value's Java type. */
public final class DbValues {
    private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

    public DbValues() {}

    public void put(String key, String value) { values.put(key, value); }
    public void put(String key, Integer value) { values.put(key, value); }
    public void put(String key, Long value) { values.put(key, value); }
    public void put(String key, Double value) { values.put(key, value); }
    public void put(String key, Float value) { values.put(key, value); }
    public void put(String key, Boolean value) { values.put(key, value); }
    public void put(String key, byte[] value) { values.put(key, value); }
    public void putNull(String key) { values.put(key, null); }

    public Object get(String key) { return values.get(key); }
    public boolean containsKey(String key) { return values.containsKey(key); }
    public int size() { return values.size(); }

    /** Insertion-ordered view for port implementations. */
    public Map<String, Object> entries() { return Collections.unmodifiableMap(values); }
}
