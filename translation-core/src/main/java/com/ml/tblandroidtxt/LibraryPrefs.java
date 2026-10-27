package com.ml.tblandroidtxt;

 
import java.util.Map;
import java.util.Objects;

/** Keeps an unreadable library value under its own key so a locked store can still be exported and inspected. */
final class LibraryPrefs {
    static final String CORRUPT_SUFFIX = "_corrupt_";

    /** Copies {@code raw} to {@code key_corrupt_<epochMillis>} once; returns the key holding the copy. */
    static synchronized String preserveCorrupt(KeyValueStore p, String key, String raw) {
        String prefix = key + CORRUPT_SUFFIX;
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            if (e.getKey().startsWith(prefix) && Objects.equals(e.getValue(), raw)) return e.getKey();
        }
        String copyKey = prefix + System.currentTimeMillis();
        p.edit().putString(copyKey, raw == null ? "" : raw).commit();
        return copyKey;
    }

    private LibraryPrefs() {}
}
