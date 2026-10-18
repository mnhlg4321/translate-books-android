package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** v4.22 S0.1/S0.2: synchronous library writes, and an unreadable store is kept and locked instead of read as empty. */
public class LibraryStoreGuardTest {
    private static final String GLOSSARY_KEY = "glossaries_json";
    private static final String PRONOUN_KEY = "profiles_json";
    /** Same shape as the stored 4.21/4.22 data, synthetic content. */
    private static final String STORED_GLOSSARIES = "[{\"id\":\"g-1\",\"name\":\"001_Glossary_Synthetic.csv\",\"sourceLang\":\"—\",\"targetLang\":\"—\","
            + "\"createdAt\":1,\"updatedAt\":2,\"terms\":[{\"source\":\"花子\",\"target\":\"Hanako\",\"category\":\"character\",\"note\":\"\"}]}]";
    private static final String STORED_PRONOUNS = "[{\"id\":\"p-1\",\"name\":\"001_Pronoun_Synthetic.csv\",\"uri\":\"\",\"text\":\"from,to,pronoun\\nHanako,Taro,chị/em\",\"createdAt\":1,\"updatedAt\":2}]";

    @Test public void readableGlossaryStoreLoadsUnchanged() {
        FakeSharedPreferences p = new FakeSharedPreferences();
        p.values.put(GLOSSARY_KEY, STORED_GLOSSARIES);
        List<GlossaryStore.Glossary> list = GlossaryStore.loadAll(p);
        assertEquals(1, list.size());
        assertEquals("g-1", list.get(0).id);
        assertEquals("Hanako", list.get(0).terms.get(0).target);
        assertEquals(STORED_GLOSSARIES, p.values.get(GLOSSARY_KEY));
        assertEquals(1, p.values.size());
    }

    @Test public void emptyAndMissingGlossaryStoresAreNotCorrupt() {
        FakeSharedPreferences missing = new FakeSharedPreferences();
        assertTrue(GlossaryStore.loadAll(missing).isEmpty());
        assertTrue(GlossaryStore.saveAll(missing, Collections.<GlossaryStore.Glossary>emptyList()));
        FakeSharedPreferences empty = new FakeSharedPreferences();
        empty.values.put(GLOSSARY_KEY, "[]");
        assertTrue(GlossaryStore.loadAll(empty).isEmpty());
        assertTrue(GlossaryStore.saveAll(empty, new ArrayList<>(Collections.singletonList(glossary("new")))));
    }

    @Test public void corruptGlossaryStoreIsPreservedAndLocked() {
        FakeSharedPreferences p = new FakeSharedPreferences();
        String broken = STORED_GLOSSARIES.substring(0, STORED_GLOSSARIES.length() - 10);
        p.values.put(GLOSSARY_KEY, broken);
        assertTrue(GlossaryStore.loadAll(p).isEmpty());
        assertEquals(broken, onlyCorruptCopy(p, GLOSSARY_KEY));
        assertFalse(GlossaryStore.saveAll(p, new ArrayList<>(Collections.singletonList(glossary("x")))));
        assertFalse(GlossaryStore.saveAll(p, Collections.<GlossaryStore.Glossary>emptyList()));
        assertEquals(broken, p.values.get(GLOSSARY_KEY));
        GlossaryStore.loadAll(p);
        assertEquals("a second read keeps one copy", 1, corruptCopies(p, GLOSSARY_KEY));
    }

    @Test public void oneUnreadableGlossaryLocksTheWholeStore() {
        FakeSharedPreferences p = new FakeSharedPreferences();
        String mixed = "[" + STORED_GLOSSARIES.substring(1, STORED_GLOSSARIES.length() - 1) + ",\"not an object\"]";
        p.values.put(GLOSSARY_KEY, mixed);
        assertTrue(GlossaryStore.loadAll(p).isEmpty());
        assertFalse(GlossaryStore.saveAll(p, Collections.<GlossaryStore.Glossary>emptyList()));
        assertEquals(mixed, p.values.get(GLOSSARY_KEY));
    }

    @Test public void failedGlossaryCommitLeavesStoreAndCacheUnchanged() {
        FakeSharedPreferences p = new FakeSharedPreferences();
        p.values.put(GLOSSARY_KEY, STORED_GLOSSARIES);
        GlossaryStore.loadAll(p);
        p.failCommits = true;
        List<GlossaryStore.Glossary> next = new ArrayList<>(GlossaryStore.loadAll(p));
        next.add(glossary("added"));
        assertFalse(GlossaryStore.saveAll(p, next));
        assertEquals(1, GlossaryStore.loadAll(p).size());
        assertEquals(STORED_GLOSSARIES, p.values.get(GLOSSARY_KEY));
    }

    @Test public void glossarySaveCommitsSynchronously() {
        FakeSharedPreferences p = new FakeSharedPreferences();
        assertTrue(GlossaryStore.saveAll(p, new ArrayList<>(Collections.singletonList(glossary("one")))));
        assertEquals(1, p.commits);
        assertNotNull(GlossaryStore.parse((String) p.values.get(GLOSSARY_KEY)));
        assertEquals(1, GlossaryStore.parse((String) p.values.get(GLOSSARY_KEY)).size());
    }

    @Test public void readablePronounStoreLoadsUnchanged() {
        FakeSharedPreferences p = new FakeSharedPreferences();
        p.values.put(PRONOUN_KEY, STORED_PRONOUNS);
        List<PronounStore.Profile> list = PronounStore.loadAll(p);
        assertEquals(1, list.size());
        assertEquals("p-1", list.get(0).id);
        assertEquals(STORED_PRONOUNS, p.values.get(PRONOUN_KEY));
    }

    @Test public void corruptPronounStoreIsPreservedAndLocked() {
        FakeSharedPreferences p = new FakeSharedPreferences();
        p.values.put(PRONOUN_KEY, "{broken");
        assertTrue(PronounStore.loadAll(p).isEmpty());
        assertEquals("{broken", onlyCorruptCopy(p, PRONOUN_KEY));
        assertFalse(PronounStore.saveAll(p, Collections.<PronounStore.Profile>emptyList()));
        assertEquals("{broken", p.values.get(PRONOUN_KEY));
    }

    @Test public void failedPronounCommitLeavesStoreUnchanged() {
        FakeSharedPreferences p = new FakeSharedPreferences();
        p.values.put(PRONOUN_KEY, STORED_PRONOUNS);
        p.failCommits = true;
        assertFalse(PronounStore.saveAll(p, Collections.<PronounStore.Profile>emptyList()));
        assertEquals(1, PronounStore.loadAll(p).size());
    }

    @Test public void parseReturnsNullOnlyForUnreadableText() {
        assertNotNull(GlossaryStore.parse(null));
        assertNotNull(GlossaryStore.parse("[]"));
        assertNull(GlossaryStore.parse("[{"));
        assertNotNull(PronounStore.parse("[]"));
        assertNull(PronounStore.parse("nope"));
    }

    private static GlossaryStore.Glossary glossary(String name) {
        GlossaryStore.Glossary g = new GlossaryStore.Glossary();
        g.name = name;
        g.terms.add(new GlossaryStore.Term("太郎", "Taro", "character"));
        return g;
    }

    private static int corruptCopies(FakeSharedPreferences p, String key) {
        int n = 0;
        for (String k : p.values.keySet()) if (k.startsWith(key + LibraryPrefs.CORRUPT_SUFFIX)) n++;
        return n;
    }

    private static Object onlyCorruptCopy(FakeSharedPreferences p, String key) {
        assertEquals(1, corruptCopies(p, key));
        for (Map.Entry<String, Object> e : p.values.entrySet()) if (e.getKey().startsWith(key + LibraryPrefs.CORRUPT_SUFFIX)) return e.getValue();
        return null;
    }
}
