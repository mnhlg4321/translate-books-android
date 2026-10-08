package com.ml.tblandroidtxt.editorial.api.chunk;

import com.ml.tblandroidtxt.editorial.api.EditInputs;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** S4: every threshold is tested on both sides, built from hand-made alignments so each signal moves alone. */
public final class ChapterVerdictTest {
    private static final List<Anchors.Term> TERMS = Anchors.terms(List.of(new EditInputs.GlossaryEntry("魔王", "Ma Vương", "", "")));

    /** n 1-1 beads. {@code agree} of the first {@code withTerm} carry the term on both sides, the others only on the RAW side. */
    private static Fixture oneToOne(int n, int withTerm, int agree, int sameEdges, int rawChars, int draftChars) {
        Fixture f = new Fixture();
        int rawPer = Math.max(1, rawChars / n);
        int draftPer = Math.max(1, draftChars / n);
        for (int i = 0; i < n; i++) {
            boolean same = i < sameEdges;
            f.raw.add("「" + "あ".repeat(rawPer - 2) + "」");
            f.draft.add(same ? "「" + "a".repeat(Math.max(0, draftPer - 2)) + "」" : "a".repeat(draftPer));
            Set<String> ra = new HashSet<>();
            Set<String> da = new HashSet<>();
            if (i < withTerm) { ra.add("ma vương"); if (i < agree) da.add("ma vương"); }
            f.rawAnchors.add(ra);
            f.draftAnchors.add(da);
            f.beads.add(new LineAligner.Bead(i, i + 1, i, i + 1));
        }
        return f;
    }

    private static final class Fixture {
        final List<String> raw = new ArrayList<>();
        final List<String> draft = new ArrayList<>();
        final List<Set<String>> rawAnchors = new ArrayList<>();
        final List<Set<String>> draftAnchors = new ArrayList<>();
        final List<LineAligner.Bead> beads = new ArrayList<>();

        ChapterVerdict.Result verdict(List<Anchors.Term> terms) {
            double ratio = 2.54;
            return ChapterVerdict.evaluate(raw, draft, new LineAligner.Alignment(beads, rawAnchors, draftAnchors, ratio), terms, ChapterVerdict.DEFAULT_SERIES_RATIO);
        }
    }

    private static ChapterVerdict.Level nameLevel(int withTerm, int agree) {
        // 100 lines, all edges equal, ratio exactly the series ratio
        Fixture f = oneToOne(100, withTerm, agree, 100, 100 * 20, (int) Math.round(100 * 20 * 2.54));
        ChapterVerdict.Result r = f.verdict(TERMS);
        ChapterVerdict.Reason name = r.reasons().stream().filter(x -> x.code().equals("NAME_MATCH")).findFirst().orElse(null);
        return name == null ? ChapterVerdict.Level.OK : name.level();
    }

    @Test public void nameMatchThresholdsAreSixtyAndThirtyFivePercent() {
        assertEquals(ChapterVerdict.Level.OK, nameLevel(20, 12));    // 0.60
        assertEquals(ChapterVerdict.Level.WARN, nameLevel(20, 11));  // 0.55
        assertEquals(ChapterVerdict.Level.WARN, nameLevel(20, 7));   // 0.35
        assertEquals(ChapterVerdict.Level.BLOCK, nameLevel(20, 6));  // 0.30
    }

    @Test public void nameMatchCountsOnlyWithAGlossaryAndAtLeastFifteenPairsWithTerms() {
        assertEquals("14 pairs with terms is too few to judge", ChapterVerdict.Level.OK, nameLevel(14, 0));
        assertEquals("15 pairs with terms is enough", ChapterVerdict.Level.BLOCK, nameLevel(15, 0));
        Fixture f = oneToOne(100, 50, 0, 100, 2000, 5080);
        assertEquals("no glossary, no name signal", ChapterVerdict.Level.OK, f.verdict(List.of()).level());
    }

    private static ChapterVerdict.Level edgeLevel(int sameEdges) {
        Fixture f = oneToOne(100, 0, 0, sameEdges, 2000, 5080);
        ChapterVerdict.Result r = f.verdict(List.of());
        ChapterVerdict.Reason edge = r.reasons().stream().filter(x -> x.code().equals("EDGE_SYMBOLS")).findFirst().orElse(null);
        return edge == null ? ChapterVerdict.Level.OK : edge.level();
    }

    @Test public void edgeSymbolThresholdsAreNinetySevenAndNinetyFivePercent() {
        assertEquals(ChapterVerdict.Level.OK, edgeLevel(97));
        assertEquals(ChapterVerdict.Level.WARN, edgeLevel(96));
        assertEquals(ChapterVerdict.Level.WARN, edgeLevel(95));
        assertEquals(ChapterVerdict.Level.BLOCK, edgeLevel(94));
    }

    private static ChapterVerdict.Level unpairedLevel(int unpaired) {
        Fixture f = oneToOne(200 - unpaired, 0, 0, 200, 4000, 10160);
        for (int i = 0; i < unpaired; i++) {
            int k = f.beads.size();
            f.raw.add("「あ」");
            f.rawAnchors.add(new HashSet<>());
            f.beads.add(new LineAligner.Bead(k, k + 1, f.draft.size(), f.draft.size()));
        }
        ChapterVerdict.Result r = f.verdict(List.of());
        ChapterVerdict.Reason skips = r.reasons().stream().filter(x -> x.code().equals("UNPAIRED_LINES")).findFirst().orElse(null);
        return skips == null ? ChapterVerdict.Level.OK : skips.level();
    }

    @Test public void unpairedLineThresholdsAreOnePointFiveAndFivePercentOfBeads() {
        assertEquals("3/200 = 1.5 %", ChapterVerdict.Level.OK, unpairedLevel(3));
        assertEquals("4/200 = 2 %", ChapterVerdict.Level.WARN, unpairedLevel(4));
        assertEquals("10/200 = 5 %", ChapterVerdict.Level.WARN, unpairedLevel(10));
        assertEquals("11/200 = 5.5 %", ChapterVerdict.Level.BLOCK, unpairedLevel(11));
    }

    private static ChapterVerdict.Level lengthLevel(double deviation) {
        int rawChars = 4000;
        Fixture f = oneToOne(100, 0, 0, 100, rawChars, (int) Math.round(rawChars * 2.54 * (1 + deviation)));
        ChapterVerdict.Result r = f.verdict(List.of());
        ChapterVerdict.Reason length = r.reasons().stream().filter(x -> x.code().equals("LENGTH_RATIO")).findFirst().orElse(null);
        return length == null ? ChapterVerdict.Level.OK : length.level();
    }

    @Test public void lengthRatioThresholdsAreTwentyAndThirtyFivePercentEitherWay() {
        assertEquals(ChapterVerdict.Level.OK, lengthLevel(0.19));
        assertEquals(ChapterVerdict.Level.WARN, lengthLevel(0.22));
        assertEquals(ChapterVerdict.Level.WARN, lengthLevel(0.34));
        assertEquals(ChapterVerdict.Level.BLOCK, lengthLevel(0.37));
        assertEquals("a DRAFT far shorter than usual is as suspicious", ChapterVerdict.Level.BLOCK, lengthLevel(-0.4));
        assertEquals(ChapterVerdict.Level.WARN, lengthLevel(-0.25));
    }

    @Test public void theChapterLevelIsTheWorstReasonAndEveryReasonCarriesItsNumber() {
        Fixture f = oneToOne(100, 20, 0, 94, 4000, (int) Math.round(4000 * 2.54 * 1.25));
        ChapterVerdict.Result r = f.verdict(TERMS);
        assertEquals(ChapterVerdict.Level.BLOCK, r.level());
        assertTrue(r.has("NAME_MATCH") && r.has("EDGE_SYMBOLS") && r.has("LENGTH_RATIO"));
        assertFalse(r.has("UNPAIRED_LINES"));
        ChapterVerdict.Reason name = r.reasons().stream().filter(x -> x.code().equals("NAME_MATCH")).findFirst().orElseThrow();
        assertEquals(0.0, name.value(), 0.0);
        ChapterVerdict.Reason edge = r.reasons().stream().filter(x -> x.code().equals("EDGE_SYMBOLS")).findFirst().orElseThrow();
        assertEquals(0.94, edge.value(), 1e-9);
    }

    @Test public void aCleanAlignmentIsOkWithNoReason() {
        Fixture f = oneToOne(100, 30, 30, 100, 4000, (int) Math.round(4000 * 2.54));
        ChapterVerdict.Result r = f.verdict(TERMS);
        assertEquals(ChapterVerdict.Level.OK, r.level());
        assertTrue(r.reasons().isEmpty());
        assertEquals(100, r.oneToOne());
    }

    @Test public void anEmptySideIsBlockedAndGroupsAreCounted() {
        ChapterVerdict.Result empty = ChapterVerdict.evaluate(List.of(), List.of("a"), new LineAligner.Alignment(List.of(), List.of(), List.of(), 0), TERMS, 2.54);
        assertEquals(ChapterVerdict.Level.BLOCK, empty.level());
        assertTrue(empty.has("EMPTY_SOURCE"));
        List<String> raw = List.of("あ", "い", "う", "え", "お");
        List<String> draft = List.of("a", "b", "c");
        List<LineAligner.Bead> beads = List.of(new LineAligner.Bead(0, 1, 0, 2), new LineAligner.Bead(1, 3, 2, 3), new LineAligner.Bead(3, 4, 3, 3), new LineAligner.Bead(4, 5, 3, 3));
        List<Set<String>> sets = List.of(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
        ChapterVerdict.Result r = ChapterVerdict.evaluate(raw, draft, new LineAligner.Alignment(beads, sets, sets, 1.0), List.of(), 1.0);
        assertEquals(1, r.splitGroups());
        assertEquals(1, r.mergeGroups());
        assertEquals(2, r.rawOnly());
        assertEquals(0, r.draftOnly());
        assertNotNull(r.level());
    }

    @Test public void aRealAlignmentOfAWrongChapterIsBlockedAndOfARightOneIsNot() {
        List<String> raw = new ArrayList<>();
        List<String> right = new ArrayList<>();
        List<String> other = new ArrayList<>();
        for (int k = 1; k <= 40; k++) {
            raw.add("「これは" + k + "番目の台詞です、魔王。」");
            right.add("「Đây là lời thoại thứ " + k + ", Ma Vương.」");
            other.add(k % 3 == 0 ? "Một đoạn kể hoàn toàn khác không liên quan gì đến chương này, số " + (900 + k) + " và nữa" : "「Lời thoại của một chương khác " + (500 + k) + ".」");
        }
        LineAligner.Alignment ok = LineAligner.align(raw, right, TERMS);
        assertEquals(ChapterVerdict.Level.OK, ChapterVerdict.evaluate(raw, right, ok, TERMS, ok.ratio()).level());
        LineAligner.Alignment bad = LineAligner.align(raw, other, TERMS);
        assertEquals(ChapterVerdict.Level.BLOCK, ChapterVerdict.evaluate(raw, other, bad, TERMS, ok.ratio()).level());
    }
}
