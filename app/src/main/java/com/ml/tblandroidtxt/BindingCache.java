package com.ml.tblandroidtxt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Chapter bindings already built, found again by a cheap fingerprint of (chapter, Glossary content, Pronoun content). Building a binding
 * turns the Glossary into prompt text and hashes both texts; ticking a chapter used to do that for every ticked chapter each time.
 */
final class BindingCache {
    private final LinkedHashMap<String, ChapterBinding> entries = new LinkedHashMap<String, ChapterBinding>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, ChapterBinding> eldest) { return size() > capacity; }
    };
    private final int capacity;
    /** How many bindings were built (cache misses); read by tests. */
    int built;

    BindingCache(int capacity) { this.capacity = Math.max(1, capacity); }

    synchronized ChapterBinding get(String rawId, String rawName, int chapterNo, GlossaryStore.Glossary glossary, PronounStore.Profile pronoun) {
        String key = ChapterBinding.fingerprint(rawId, rawName, chapterNo, glossary, pronoun);
        ChapterBinding known = entries.get(key);
        if (known != null) return known;
        ChapterBinding fresh = ChapterBinding.of(rawId, rawName, chapterNo, glossary, pronoun);
        entries.put(key, fresh);
        built++;
        return fresh;
    }

    synchronized int size() { return entries.size(); }
}
