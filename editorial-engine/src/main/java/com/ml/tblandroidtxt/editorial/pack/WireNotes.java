package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.List;

/**
 * Z1: BOOKKEEPING rules do not reject a response; they normalize a value the app derives or never uses and leave a
 * note. A note is {@code kind:path} with an app-authored kind and path only (never model text), so it can be stored
 * in the artifact next to the other normalizations. One parse runs on one thread: the parser entry point calls
 * {@link #begin()}, rule code calls {@link #note}, and the entry point stores {@link #drain()} in its artifact.
 */
final class WireNotes {
    /** Notes beyond this many are counted by the artifact's own totals but not listed. */
    static final int MAX_NOTES = 128;
    private static final ThreadLocal<List<String>> NOTES = ThreadLocal.withInitial(ArrayList::new);

    private WireNotes() { }

    static void begin() { NOTES.get().clear(); }

    static void note(String kind, String path) {
        List<String> notes = NOTES.get();
        if (notes.size() >= MAX_NOTES) return;
        String entry = kind + ":" + (path == null || path.isEmpty() ? "root" : path);
        if (!notes.contains(entry)) notes.add(entry);
    }

    static List<String> drain() {
        List<String> copy = List.copyOf(NOTES.get());
        NOTES.get().clear();
        return copy;
    }
}
