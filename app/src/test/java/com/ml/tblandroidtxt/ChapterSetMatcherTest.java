package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** v4.23 L3: the matcher only suggests; every row is rated GREEN / YELLOW / RED and the owner can still change it. */
public class ChapterSetMatcherTest {
    private static ChapterSetMatcher.Named n(String id, String name) { return new ChapterSetMatcher.Named(id, name); }

    private static List<ChapterSetMatcher.Row> suggest(List<ChapterSetMatcher.Named> raws, List<ChapterSetMatcher.Named> g, List<ChapterSetMatcher.Named> p) {
        List<Integer> numbers = new ArrayList<>();
        for (ChapterSetMatcher.Named r : raws) numbers.add(ChapterNumber.parse(r.name));
        return ChapterSetMatcher.suggest(raws, numbers, g, p);
    }

    @Test public void zeroPaddedAndPlainNumbersMatchTheSameChapter() {
        List<ChapterSetMatcher.Row> rows = suggest(
                Arrays.asList(n("r25", "025_RAW_Kumo.txt"), n("r26", "26_RAW_Kumo.txt")),
                Arrays.asList(n("g25", "25_Glossary_Kumo.csv"), n("g26", "026_Glossary_Kumo.csv")),
                Arrays.asList(n("p25", "025_Pronoun_Kumo.csv"), n("p26", "026-Pronoun.csv")));
        assertEquals("g25", rows.get(0).glossaryId);
        assertEquals("p25", rows.get(0).pronounId);
        assertEquals("g26", rows.get(1).glossaryId);
        assertEquals("p26", rows.get(1).pronounId);
        assertEquals(ChapterSetMatcher.Status.GREEN, rows.get(0).status);
        assertEquals(ChapterSetMatcher.Status.GREEN, rows.get(1).status);
        assertFalse(ChapterSetMatcher.hasRed(rows));
    }

    @Test public void volumeAndExtraNumbersInTheMiddleAreNotChapters() {
        List<ChapterSetMatcher.Row> rows = suggest(
                Arrays.asList(n("a", "VOL08_013_RAW.txt"), n("b", "Ex2_RAW.txt")),
                Arrays.asList(n("g13", "013_Glossary.csv"), n("g2", "2_Glossary.csv")),
                Arrays.asList(n("p13", "013_Pronoun.csv")));
        for (ChapterSetMatcher.Row r : rows) {
            assertEquals(ChapterSetMatcher.Status.RED, r.status);
            assertEquals("", r.glossaryId);
            assertTrue(r.reason.contains("không có số chương"));
        }
        assertTrue(ChapterSetMatcher.hasRed(rows));
    }

    @Test public void missingGlossaryOrPronounIsYellowAndBothMissingSaysSo() {
        List<ChapterSetMatcher.Row> rows = suggest(
                Arrays.asList(n("a", "001_a.txt"), n("b", "002_b.txt"), n("c", "003_c.txt")),
                Arrays.asList(n("g1", "001_g.csv"), n("g2", "002_g.csv")),
                Arrays.asList(n("p1", "001_p.csv")));
        assertEquals(ChapterSetMatcher.Status.GREEN, rows.get(0).status);
        assertEquals(ChapterSetMatcher.Status.YELLOW, rows.get(1).status);
        assertEquals("Thiếu Pronoun", rows.get(1).reason);
        assertEquals(ChapterSetMatcher.Status.YELLOW, rows.get(2).status);
        assertEquals("Thiếu Glossary và Pronoun", rows.get(2).reason);
        assertFalse("yellow rows can be saved", ChapterSetMatcher.hasRed(rows));
        assertEquals(1, ChapterSetMatcher.count(rows, ChapterSetMatcher.Status.GREEN));
        assertEquals(2, ChapterSetMatcher.count(rows, ChapterSetMatcher.Status.YELLOW));
    }

    @Test public void severalCandidatesAreRedUntilTheOwnerPicksOne() {
        List<ChapterSetMatcher.Named> glossaries = Arrays.asList(n("g-a", "010_Glossary_old.csv"), n("g-b", "10_Glossary_new.csv"));
        List<ChapterSetMatcher.Row> rows = suggest(Arrays.asList(n("r", "010_RAW.txt")), glossaries, Arrays.asList(n("p", "010_P.csv")));
        ChapterSetMatcher.Row row = rows.get(0);
        assertEquals("nothing is chosen silently", "", row.glossaryId);
        assertEquals(2, row.glossaryCandidates.size());
        assertEquals(ChapterSetMatcher.Status.RED, row.status);
        row.glossaryId = "g-b";
        row.glossaryChosen = true;
        ChapterSetMatcher.rate(rows);
        assertEquals(ChapterSetMatcher.Status.GREEN, row.status);
        row.glossaryId = "";
        ChapterSetMatcher.rate(rows);
        assertEquals("choosing Không dùng on purpose is allowed", ChapterSetMatcher.Status.GREEN, row.status);
    }

    @Test public void twoRawsWithTheSameNumberAreBothRedUntilOneIsExcludedOrRenumbered() {
        List<ChapterSetMatcher.Row> rows = suggest(
                Arrays.asList(n("a", "007_first.txt"), n("b", "7_second.txt"), n("c", "008.txt")),
                Collections.singletonList(n("g", "007_g.csv")), Collections.singletonList(n("p", "007_p.csv")));
        assertEquals(ChapterSetMatcher.Status.RED, rows.get(0).status);
        assertEquals(ChapterSetMatcher.Status.RED, rows.get(1).status);
        assertTrue(rows.get(0).reason.contains("Hai RAW cùng số chương 7"));
        rows.get(1).excluded = true;
        ChapterSetMatcher.rate(rows);
        assertEquals(ChapterSetMatcher.Status.GREEN, rows.get(0).status);
        rows.get(1).excluded = false;
        rows.get(1).chapterNo = 70;
        ChapterSetMatcher.resuggest(rows, Collections.singletonList(n("g", "007_g.csv")), Collections.singletonList(n("p", "007_p.csv")));
        ChapterSetMatcher.rate(rows);
        assertEquals("a renumbered row is rated again", ChapterSetMatcher.Status.YELLOW, rows.get(1).status);
    }

    @Test public void renumberingKeepsFieldsTheOwnerChoseAndResuggestsTheRest() {
        List<ChapterSetMatcher.Named> g = Arrays.asList(n("g5", "5_g.csv"), n("g6", "6_g.csv"));
        List<ChapterSetMatcher.Named> p = Arrays.asList(n("p5", "5_p.csv"), n("p6", "6_p.csv"));
        List<ChapterSetMatcher.Row> rows = suggest(Collections.singletonList(n("x", "5_x.txt")), g, p);
        ChapterSetMatcher.Row row = rows.get(0);
        row.pronounId = "p6";
        row.pronounChosen = true;
        row.chapterNo = 6;
        ChapterSetMatcher.resuggest(rows, g, p);
        ChapterSetMatcher.rate(rows);
        assertEquals("g6", row.glossaryId);
        assertEquals("the owner's pronoun choice stays", "p6", row.pronounId);
    }

    @Test public void missingItemsInAnOpenedSetAreShownNotFatal() {
        ChapterSetMatcher.Row row = new ChapterSetMatcher.Row();
        row.rawId = "r"; row.rawName = "012_x.txt"; row.chapterNo = 12;
        row.glossaryId = "gone"; row.glossaryChosen = true; row.glossaryMissing = true;
        row.pronounId = "p"; row.pronounChosen = true;
        ChapterSetMatcher.rate(Collections.singletonList(row));
        assertEquals(ChapterSetMatcher.Status.YELLOW, row.status);
        assertEquals("Glossary đã bị xóa", row.reason);
        row.rawMissing = true;
        ChapterSetMatcher.rate(Collections.singletonList(row));
        assertEquals(ChapterSetMatcher.Status.RED, row.status);
        assertTrue(row.reason.contains("RAW đã bị xóa"));
    }

    @Test public void defaultNameIsTheSharedPartAfterTheNumber() {
        assertEquals("Kumo7_RAW", ChapterSetMatcher.defaultName(Arrays.asList("025_Kumo7_RAW.txt", "026_Kumo7_RAW.txt")));
        assertEquals("Kumo7", ChapterSetMatcher.defaultName(Arrays.asList("025_Kumo7_a.txt", "026_Kumo7_b.txt")));
        assertEquals("only", ChapterSetMatcher.defaultName(Collections.singletonList("9-only.txt")));
        assertEquals("Bộ chương", ChapterSetMatcher.defaultName(new ArrayList<String>()));
        assertEquals("names with nothing in common fall back to the first one", "a", ChapterSetMatcher.defaultName(Arrays.asList("1_a.txt", "2_b.txt")));
    }
}
