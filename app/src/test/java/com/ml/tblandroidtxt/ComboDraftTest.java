package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v4.23 U2: pairing from the three slots, only inside the files chosen in each slot, and never re-pairing a saved combination. */
public class ComboDraftTest {
    private static ChapterSetMatcher.Named n(String id, String name) { return new ChapterSetMatcher.Named(id, name); }
    private static ComboDraft.RawRef raw(String id, String name) { return new ComboDraft.RawRef(id, name, ChapterNumber.parse(name)); }

    private static final List<ComboDraft.RawRef> RAWS = Arrays.asList(raw("r1", "001_ATE_RAW.txt"), raw("r2", "002_ATE_RAW.txt"), raw("r3", "003_ATE_RAW.txt"), raw("rx", "Ex2_RAW.txt"), raw("one", "story.txt"));
    private static final List<ChapterSetMatcher.Named> GLOSS = Arrays.asList(n("g1", "001_G.csv"), n("g2", "002_G.csv"), n("g3", "003_G.csv"), n("g3b", "3_G_alt.csv"), n("g9", "009_G.csv"), n("gx", "notes.csv"));
    private static final List<ChapterSetMatcher.Named> PRON = Arrays.asList(n("p1", "001_P.csv"), n("p2", "002_P.csv"), n("px", "misc.csv"));

    private static ComboDraft draft(String[] raws, String[] g, String[] p) {
        ComboDraft d = new ComboDraft();
        d.rawIds.addAll(Arrays.asList(raws)); d.glossaryIds.addAll(Arrays.asList(g)); d.pronounIds.addAll(Arrays.asList(p));
        d.rebuild(RAWS, GLOSS, PRON);
        return d;
    }

    @Test public void oneRawOneGlossaryOnePronounWithoutChapterNumbersIsOneGreenRow() {
        ComboDraft d = draft(new String[]{"one"}, new String[]{"gx"}, new String[]{"px"});
        assertEquals(1, d.rows.size());
        assertEquals("gx", d.rows.get(0).glossaryId);
        assertEquals("px", d.rows.get(0).pronounId);
        assertEquals(ChapterSetMatcher.Status.GREEN, d.rows.get(0).status);
        assertTrue(d.canSave());
    }

    @Test public void threeThreeThreeWithTheSameNumbersAreThreeGreenRows() {
        ComboDraft d = draft(new String[]{"r1", "r2", "r3"}, new String[]{"g1", "g2", "g3"}, new String[]{"p1", "p2"});
        assertEquals(3, d.rows.size());
        assertEquals(Arrays.asList("g1", "g2", "g3"), Arrays.asList(d.rows.get(0).glossaryId, d.rows.get(1).glossaryId, d.rows.get(2).glossaryId));
        assertEquals(ChapterSetMatcher.Status.GREEN, d.rows.get(0).status);
        assertEquals(ChapterSetMatcher.Status.GREEN, d.rows.get(1).status);
        assertEquals("row 3 has no Pronoun among the chosen files", ChapterSetMatcher.Status.YELLOW, d.rows.get(2).status);
        assertEquals("Thiếu Pronoun", d.rows.get(2).reason);
        assertTrue("a yellow row can still be saved", d.canSave());
    }

    @Test public void pairingStaysInsideTheFilesChosenInEachSlot() {
        ComboDraft d = draft(new String[]{"r1", "r2"}, new String[]{"g1"}, new String[]{"p1", "p2"});
        assertEquals("g2 exists in the library but was not chosen in the Glossary slot", "", d.rows.get(1).glossaryId);
        assertEquals(ChapterSetMatcher.Status.YELLOW, d.rows.get(1).status);
        assertEquals("p2", d.rows.get(1).pronounId);
    }

    @Test public void twoGlossariesOfTheSameChapterAreRedUntilOneIsChosen() {
        ComboDraft d = draft(new String[]{"r3"}, new String[]{"g3", "g3b"}, new String[]{});
        ChapterSetMatcher.Row r = d.rows.get(0);
        assertEquals(ChapterSetMatcher.Status.RED, r.status);
        assertTrue(r.reason, r.reason.contains("2 Glossary cùng chương 3"));
        assertFalse(d.canSave());
        assertEquals(0, d.firstRedIndex());
        r.glossaryId = "g3b"; r.glossaryChosen = true;
        d.rebuild(RAWS, GLOSS, PRON);
        assertEquals(ChapterSetMatcher.Status.GREEN, d.rows.get(0).status);
        assertTrue("an empty Pronoun slot means no Pronoun on purpose, not a warning", d.rows.get(0).pronounOff);
    }

    @Test public void filesThatMatchNoChapterAreReported() {
        ComboDraft d = draft(new String[]{"r1", "r2"}, new String[]{"g1", "g2", "g9"}, new String[]{"p1", "p2", "px"});
        assertEquals(Arrays.asList("009_G.csv"), d.unusedGlossaries);
        assertEquals(Arrays.asList("misc.csv"), d.unusedPronouns);
    }

    @Test public void aRawWithoutNumberNeedsAPairOrIsRed() {
        ComboDraft d = draft(new String[]{"r1", "rx"}, new String[]{"g1"}, new String[]{"p1"});
        ChapterSetMatcher.Row ex = d.rows.get(1);
        assertEquals("Ex2_RAW.txt", ex.rawName);
        assertEquals(ChapterSetMatcher.Status.RED, ex.status);
        ex.chapterNo = 5;
        d.rebuild(RAWS, GLOSS, PRON);
        assertEquals(ChapterSetMatcher.Status.YELLOW, d.rows.get(1).status);
    }

    @Test public void aSavedChoiceIsNotPairedAgainWhenASlotChanges() {
        ComboDraft d = draft(new String[]{"r1", "r2"}, new String[]{"g1", "g2"}, new String[]{"p1", "p2"});
        ChapterSetMatcher.Row second = d.rows.get(1);
        second.glossaryId = ""; second.glossaryChosen = true; // the owner picked "Không dùng"
        d.glossaryIds.add("g9");
        d.rebuild(RAWS, GLOSS, PRON);
        assertEquals("", d.rows.get(1).glossaryId);
        assertEquals("g1", d.rows.get(0).glossaryId);
        assertEquals(2, d.rows.size());
        d.rawIds.add("r3");
        d.rebuild(RAWS, GLOSS, PRON);
        assertEquals("only the new row is suggested", 3, d.rows.size());
        assertEquals("", d.rows.get(1).glossaryId);
        d.rawIds.remove("r1");
        d.rebuild(RAWS, GLOSS, PRON);
        assertEquals(Arrays.asList("r2", "r3"), Arrays.asList(d.rows.get(0).rawId, d.rows.get(1).rawId));
    }

    @Test public void nameFollowsTheRawNamesUntilTheOwnerTypesOne() {
        ComboDraft d = draft(new String[]{"r1", "r2"}, new String[]{}, new String[]{});
        assertEquals("ATE_RAW", d.name);
        d.name = "My combo"; d.nameAuto = false;
        d.rawIds.add("r3");
        d.rebuild(RAWS, GLOSS, PRON);
        assertEquals("My combo", d.name);
    }

    @Test public void listLinesSummariseChaptersAndStatus() {
        assertEquals("3 chương · ✓ 3 đủ", ComboDraft.summaryOf(3, 3, 0, 0));
        assertEquals("3 chương · ✓ 2 đủ · ⚠ 1 thiếu", ComboDraft.summaryOf(3, 2, 1, 0));
        assertEquals("Ch.1–3", ComboDraft.chapterRange(Arrays.asList(3, 1, 2)));
        assertEquals("Ch.1, 3, 7", ComboDraft.chapterRange(Arrays.asList(7, 1, 3)));
        assertEquals("Ch.5", ComboDraft.chapterRange(Arrays.asList(5)));
        assertEquals("", ComboDraft.chapterRange(new ArrayList<Integer>()));
    }
}
