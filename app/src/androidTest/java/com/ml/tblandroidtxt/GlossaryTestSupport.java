package com.ml.tblandroidtxt;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Glossary store writes for tests. {@link GlossaryStore#saveAll} persists with {@code apply()}, which is asynchronous: when an
 * instrumentation run ends right after a test the target process is killed and a pending write can be lost, which leaves the
 * fixture glossaries a test just "removed" in the device's store (the residue seen on the 4.18.0 release candidate). An empty
 * {@code commit()} is queued behind every pending write of the same preferences file and returns only when it is on disk.
 */
final class GlossaryTestSupport {
    private static final String PREFS_FILE = "glossary_store";

    private GlossaryTestSupport() { }

    static void saveAndFlush(Context context, List<GlossaryStore.Glossary> glossaries) {
        GlossaryStore.saveAll(context, glossaries);
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit().commit();
    }

    /** Removes exactly these ids (a test's own fixtures) and nothing else, then waits until the change is on disk. */
    static void removeAndFlush(Context context, Collection<String> ids) {
        if (ids.isEmpty()) return;
        List<GlossaryStore.Glossary> all = new ArrayList<>(GlossaryStore.loadAll(context));
        all.removeIf(g -> ids.contains(g.id));
        saveAndFlush(context, all);
    }
}
