package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class LibraryImportPlannerTest {
    @Test public void eachGlossaryFileBecomesAnIndependentProfile() {
        LibraryImportPlanner.Result<GlossaryStore.Glossary> result =
                LibraryImportPlanner.glossaries(Arrays.asList(
                        new LibraryImportPlanner.Source(
                                "characters.csv", "source,target\nAlice,Alicia"),
                        new LibraryImportPlanner.Source(
                                "places.csv", "source,target\nLondon,Luân Đôn")));

        assertEquals(2, result.imported.size());
        assertTrue(result.failed.isEmpty());
        assertEquals("characters.csv", result.imported.get(0).name);
        assertEquals("places.csv", result.imported.get(1).name);
        assertEquals(1, result.imported.get(0).count());
        assertEquals(1, result.imported.get(1).count());
        assertNotEquals(result.imported.get(0).id, result.imported.get(1).id);
    }

    @Test public void badGlossaryDoesNotDiscardValidSibling() {
        LibraryImportPlanner.Result<GlossaryStore.Glossary> result =
                LibraryImportPlanner.glossaries(Arrays.asList(
                        new LibraryImportPlanner.Source("valid.csv", "Magic,Phép thuật"),
                        new LibraryImportPlanner.Source("invalid.txt", "not a mapping")));

        assertEquals(1, result.imported.size());
        assertEquals("valid.csv", result.imported.get(0).name);
        assertEquals(1, result.failed.size());
        assertTrue(result.failed.get(0).startsWith("invalid.txt:"));
    }

    @Test public void eachPronounFileBecomesAnIndependentProfile() {
        LibraryImportPlanner.Result<PronounStore.Profile> result =
                LibraryImportPlanner.pronouns(Arrays.asList(
                        new LibraryImportPlanner.Source(
                                "family.csv", "from,to,pronoun\nAlice,Bob,chị/em"),
                        new LibraryImportPlanner.Source(
                                "work.csv", "from,to,pronoun\nAlice,Bob,sếp/tôi")));

        assertEquals(2, result.imported.size());
        assertTrue(result.failed.isEmpty());
        assertEquals("family.csv", result.imported.get(0).name);
        assertEquals("work.csv", result.imported.get(1).name);
        assertNotEquals(result.imported.get(0).id, result.imported.get(1).id);
        assertNotEquals(result.imported.get(0).text, result.imported.get(1).text);
    }

    @Test public void badPronounDoesNotDiscardValidSibling() {
        LibraryImportPlanner.Result<PronounStore.Profile> result =
                LibraryImportPlanner.pronouns(Arrays.asList(
                        new LibraryImportPlanner.Source(
                                "valid.csv", "from,to,pronoun\nAlice,Bob,chị/em"),
                        new LibraryImportPlanner.Source("invalid.txt", "")));

        assertEquals(1, result.imported.size());
        assertEquals("valid.csv", result.imported.get(0).name);
        assertEquals(1, result.failed.size());
        assertTrue(result.failed.get(0).startsWith("invalid.txt:"));
    }
}
