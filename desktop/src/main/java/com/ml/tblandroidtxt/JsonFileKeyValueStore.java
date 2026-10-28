package com.ml.tblandroidtxt;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** KeyValueStore kept in one JSON file per prefs name (prefs/&lt;name&gt;.json), written atomically. */
public final class JsonFileKeyValueStore implements KeyValueStore {
    private final Path file;
    private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

    public JsonFileKeyValueStore(Path prefsDir, String name) {
        this.file = prefsDir.resolve(name + ".json");
        load();
    }

    private synchronized void load() {
        values.clear();
        try {
            if (!Files.exists(file)) return;
            JSONObject o = new JSONObject(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            for (Iterator<String> it = o.keys(); it.hasNext(); ) {
                String key = it.next();
                Object v = o.get(key);
                if (v instanceof Integer) v = ((Integer) v).longValue();
                values.put(key, v);
            }
        } catch (Exception unreadable) {
            values.clear();
        }
    }

    @Override public synchronized String getString(String key, String defaultValue) {
        Object v = values.get(key);
        return v instanceof String ? (String) v : defaultValue;
    }

    @Override public synchronized boolean getBoolean(String key, boolean defaultValue) {
        Object v = values.get(key);
        return v instanceof Boolean ? (Boolean) v : defaultValue;
    }

    @Override public synchronized Map<String, ?> getAll() { return new LinkedHashMap<>(values); }

    @Override public Editor edit() { return new FileEditor(); }

    /** Writes the whole map to a temp file and moves it over the target. */
    private boolean persist(Map<String, Object> next) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, new JSONObject(next).toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private final class FileEditor implements Editor {
        private final LinkedHashMap<String, Object> pending = new LinkedHashMap<>();
        private final java.util.HashSet<String> removed = new java.util.HashSet<>();

        @Override public Editor putString(String key, String value) {
            if (value == null) return remove(key);
            removed.remove(key);
            pending.put(key, value);
            return this;
        }

        @Override public Editor putBoolean(String key, boolean value) { removed.remove(key); pending.put(key, value); return this; }

        @Override public Editor putLong(String key, long value) { removed.remove(key); pending.put(key, value); return this; }

        @Override public Editor remove(String key) { pending.remove(key); removed.add(key); return this; }

        @Override public boolean commit() {
            synchronized (JsonFileKeyValueStore.this) {
                LinkedHashMap<String, Object> next = new LinkedHashMap<>(values);
                for (String key : removed) next.remove(key);
                next.putAll(pending);
                if (!persist(next)) return false;
                values.clear();
                values.putAll(next);
                return true;
            }
        }

        @Override public void apply() { commit(); }
    }
}
