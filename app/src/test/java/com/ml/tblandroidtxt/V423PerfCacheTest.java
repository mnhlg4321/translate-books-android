package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/** v4.23 J1: chapter bindings and prompt texts are built once per content, not once per tap. */
public class V423PerfCacheTest {
    private static GlossaryStore.Glossary glossary(String id, String target) {
        GlossaryStore.Glossary g = new GlossaryStore.Glossary();
        g.id = id; g.name = "g-" + id;
        g.terms.add(new GlossaryStore.Term("花子", target, "character"));
        return g;
    }

    private static PronounStore.Profile pronoun(String id, String text) {
        PronounStore.Profile p = new PronounStore.Profile();
        p.id = id; p.name = "p-" + id; p.text = text;
        return p;
    }

    @Test public void theSameChapterContentReusesTheBinding() {
        BindingCache cache = new BindingCache(8);
        GlossaryStore.Glossary g = glossary("g1", "Hanako");
        PronounStore.Profile p = pronoun("p1", "from,speaker,target,self,call,scope,note\nA,,B,tôi,cậu,,");
        ChapterBinding first = cache.get("r1", "001.txt", 1, g, p);
        for (int i = 0; i < 20; i++) assertSame(first, cache.get("r1", "001.txt", 1, g, p));
        assertEquals(1, cache.built);
        // An equal copy (the Library re-read the stores) is the same content.
        assertSame(first, cache.get("r1", "001.txt", 1, glossary("g1", "Hanako"), pronoun("p1", p.text)));
        assertEquals(1, cache.built);
    }

    @Test public void editedGlossaryOrPronounOrAnotherChapterBuildsANewBinding() {
        BindingCache cache = new BindingCache(8);
        ChapterBinding base = cache.get("r1", "001.txt", 1, glossary("g1", "Hanako"), pronoun("p1", "a"));
        assertNotSame(base, cache.get("r1", "001.txt", 1, glossary("g1", "Hana"), pronoun("p1", "a")));
        assertNotSame(base, cache.get("r1", "001.txt", 1, glossary("g1", "Hanako"), pronoun("p1", "b")));
        assertNotSame(base, cache.get("r2", "002.txt", 2, glossary("g1", "Hanako"), pronoun("p1", "a")));
        assertNotSame(base, cache.get("r1", "001.txt", 1, null, null));
        assertEquals(5, cache.built);
        cache.get("r1", "001.txt", 1, glossary("g1", "Hana"), pronoun("p1", "a"));
        assertEquals("a repeat of an edited content is cached too", 5, cache.built);
    }

    @Test public void theCacheIsBounded() {
        BindingCache cache = new BindingCache(3);
        for (int i = 0; i < 10; i++) cache.get("r" + i, "n", i, null, null);
        assertEquals(3, cache.size());
    }

    @Test public void promptTextIsBuiltOncePerGlossaryContent() {
        GlossaryStore.Glossary g = glossary("g9", "Hanako");
        String first = GlossaryStore.toPromptText(g);
        assertSame(first, GlossaryStore.toPromptText(g));
        g.terms.add(new GlossaryStore.Term("太郎", "Taro", "character"));
        String changed = GlossaryStore.toPromptText(g);
        assertNotSame(first, changed);
        assertEquals(true, changed.contains("Taro"));
    }

    @Test public void pronounRuleCountIsAnalysedOncePerText() {
        String text = "from,speaker,target,self,call,scope,note\nAn,,Binh,tôi,cậu,,\nCa,,Dung,tôi,cậu,,";
        int before = PronounStore.RuleCounts.calls;
        int n = pronoun("p", text).count();
        for (int i = 0; i < 10; i++) assertEquals(n, pronoun("q" + i, text).count());
        assertEquals(1, PronounStore.RuleCounts.calls - before);
    }
}
