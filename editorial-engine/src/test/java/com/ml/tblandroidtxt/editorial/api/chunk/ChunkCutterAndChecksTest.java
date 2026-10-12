package com.ml.tblandroidtxt.editorial.api.chunk;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** S5 (cuts never fall inside a group, sizes come from the caller) and S6 (merge, uncertain, WARN). Synthetic lines only. */
public final class ChunkCutterAndChecksTest {
    private static final ToIntFunction<String> CHARS = s -> s.replace("\n", "").codePointCount(0, s.replace("\n", "").length());
    /** A different measure: one unit per three characters, to show the caller's measure drives the count. */
    private static final ToIntFunction<String> THIRDS = s -> (CHARS.applyAsInt(s) + 2) / 3;

    private static final class Chapter {
        final List<String> raw = new ArrayList<>();
        final List<String> draft = new ArrayList<>();
        final List<LineAligner.Bead> beads = new ArrayList<>();

        /** Appends a bead of {@code r} RAW lines and {@code d} DRAFT lines, each of {@code len} characters. */
        Chapter bead(int r, int d, int len) {
            int rs = raw.size();
            int ds = draft.size();
            for (int i = 0; i < r; i++) raw.add("あ".repeat(len));
            for (int i = 0; i < d; i++) draft.add("a".repeat(len));
            beads.add(new LineAligner.Bead(rs, raw.size(), ds, draft.size()));
            return this;
        }

        Chapter ones(int n, int len) { for (int i = 0; i < n; i++) bead(1, 1, len); return this; }

        LineAligner.Alignment alignment() {
            List<Set<String>> ra = new ArrayList<>();
            List<Set<String>> da = new ArrayList<>();
            for (String s : raw) ra.add(new HashSet<>());
            for (String s : draft) da.add(new HashSet<>());
            return new LineAligner.Alignment(beads, ra, da, 1.0);
        }

        List<ChunkCutter.Cut> cut(int soft, int hard, int maxOutput, ToIntFunction<String> measure) {
            return ChunkCutter.cut(alignment(), raw, draft, new ChunkCutter.Limits(soft, hard, maxOutput, 1.3, measure, measure));
        }
    }

    private static List<Integer> ends(List<ChunkCutter.Cut> cuts) {
        List<Integer> out = new ArrayList<>();
        for (ChunkCutter.Cut c : cuts) out.add(c.endBead());
        return out;
    }

    @Test public void cutsFallAtTheFirstSafePointOnceTheSoftLimitIsReached() {
        Chapter c = new Chapter().ones(40, 100);
        List<ChunkCutter.Cut> cuts = c.cut(900, Integer.MAX_VALUE, 0, CHARS);
        // 9 lines of 100 reach 900: cut after bead index 8, then every 9 beads; the last chunk is the remainder
        assertEquals(List.of(8, 17, 26, 35, 39), ends(cuts));
    }

    @Test public void aCutNeverFallsInsideAGroupOrNextToAnUnpairedLine() {
        Chapter c = new Chapter().ones(8, 100).bead(1, 2, 100).ones(1, 100).bead(2, 1, 100).ones(1, 100).bead(1, 0, 100).ones(30, 100);
        List<ChunkCutter.Cut> cuts = c.cut(900, Integer.MAX_VALUE, 0, CHARS);
        List<LineAligner.Bead> b = c.beads;
        for (ChunkCutter.Cut cut : cuts) {
            int k = cut.endBead();
            if (k == b.size() - 1) continue;
            for (int j = Math.max(0, k - 1); j <= Math.min(b.size() - 1, k + 2); j++) {
                assertTrue("bead " + j + " around cut " + k + " must be 1-1", b.get(j).oneToOne());
            }
        }
        assertTrue(cuts.size() >= 3);
    }

    @Test public void chunksRebuildTheWholeChapterInOrder() {
        Chapter c = new Chapter().ones(6, 100).bead(1, 2, 100).ones(12, 100).bead(2, 1, 100).ones(20, 100);
        List<ChunkCutter.Cut> cuts = c.cut(700, 1400, 0, CHARS);
        int rawNext = 0;
        int draftNext = 0;
        int from = 0;
        for (ChunkCutter.Cut cut : cuts) {
            assertEquals(rawNext, c.beads.get(from).rawStart());
            assertEquals(draftNext, c.beads.get(from).draftStart());
            rawNext = c.beads.get(cut.endBead()).rawEnd();
            draftNext = c.beads.get(cut.endBead()).draftEnd();
            from = cut.endBead() + 1;
        }
        assertEquals(c.raw.size(), rawNext);
        assertEquals(c.draft.size(), draftNext);
    }

    @Test public void theCallersMeasureChangesTheNumberOfChunksLikeTokenAndCharModesDoInTranslate() {
        Chapter c = new Chapter().ones(60, 100);
        int byChars = c.cut(900, Integer.MAX_VALUE, 0, CHARS).size();
        int byThirds = c.cut(900, Integer.MAX_VALUE, 0, THIRDS).size();
        assertTrue(byChars > byThirds);
        assertEquals(7, byChars);
        assertEquals(3, byThirds);
    }

    @Test public void theHardLimitWinsOverTheSoftLimitWhenASafePointExistsInsideIt() {
        // 100-character lines; only bead 1 (200 chars) is a safe point before the hard limit of 700: bead 2 has a group two beads on
        Chapter c = new Chapter().ones(4, 100).bead(1, 2, 100).bead(2, 1, 100).bead(1, 2, 100).ones(40, 100);
        List<ChunkCutter.Cut> cuts = c.cut(600, 700, 0, CHARS);
        assertEquals(1, cuts.get(0).endBead());
        assertTrue(cuts.get(0).flags().isEmpty());
    }

    @Test public void aLineLongerThanTheHardLimitIsAChunkOfItsOwn() {
        Chapter c = new Chapter().ones(6, 100).bead(1, 1, 2000).ones(30, 100);
        List<ChunkCutter.Cut> cuts = c.cut(600, 900, 0, CHARS);
        int longEnd = -1;
        for (ChunkCutter.Cut cut : cuts) if (cut.flags().contains("LONG_LINE")) longEnd = cut.endBead();
        assertEquals("the long bead (index 6) closes its own chunk", 6, longEnd);
        int previous = -1;
        for (ChunkCutter.Cut cut : cuts) { if (cut.endBead() == 6) assertEquals(5, previous + 0 == 5 ? 5 : previous); previous = cut.endBead(); }
    }

    @Test public void theEstimatedOutputMustFitMaxOutputSoAChunkIsCutSmaller() {
        Chapter c = new Chapter().ones(60, 100);
        List<ChunkCutter.Cut> unlimited = c.cut(2000, 4000, 0, CHARS);
        List<ChunkCutter.Cut> limited = c.cut(2000, 4000, 1000, CHARS); // 100-char DRAFT lines x 1.3 -> at most 7 lines per chunk
        assertTrue(limited.size() > unlimited.size());
        int from = 0;
        for (ChunkCutter.Cut cut : limited) {
            int lines = cut.endBead() - from + 1;
            assertTrue("chunk of " + lines + " lines", lines * 100 * 1.3 <= 1000 || cut.endBead() == c.beads.size() - 1);
            from = cut.endBead() + 1;
        }
    }

    @Test public void withoutAnySafePointInsideHardThePointWithinThreeTimesHardIsUsedAndFlagged() {
        // 12 beads of 100 chars, all 1-1, then an unsafe stretch: groups every other bead for 30 beads, then 1-1 again
        Chapter c = new Chapter();
        for (int i = 0; i < 30; i++) { c.bead(1, 2, 100); c.bead(1, 1, 100); }
        c.ones(10, 100);
        List<ChunkCutter.Cut> cuts = c.cut(300, 500, 0, CHARS);
        assertTrue(cuts.stream().anyMatch(x -> x.flags().contains("OVERSIZE") || x.flags().contains("WEAK_CUT") || x.flags().contains("NO_SAFE_CUT")));
        int last = cuts.get(cuts.size() - 1).endBead();
        assertEquals(c.beads.size() - 1, last);
    }

    @Test public void noSafePointAnywhereKeepsTheRestAsOneFlaggedChunk() {
        Chapter c = new Chapter();
        for (int i = 0; i < 12; i++) c.bead(1, 2, 100);
        List<ChunkCutter.Cut> cuts = c.cut(300, 500, 0, CHARS);
        assertEquals(1, cuts.size());
        assertTrue(cuts.get(0).flags().contains("NO_SAFE_CUT"));
    }

    // ---- S6

    private static ChunkChecks.Result check(Chapter c, double ratio, List<ChunkCutter.Cut> cuts) {
        LineAligner.Alignment a = new LineAligner.Alignment(c.beads, c.alignment().rawAnchors(), c.alignment().draftAnchors(), ratio);
        return ChunkChecks.apply(a, c.raw, c.draft, List.of(), cuts);
    }

    private static List<ChunkCutter.Cut> cutsAt(int... ends) {
        List<ChunkCutter.Cut> out = new ArrayList<>();
        for (int e : ends) out.add(new ChunkCutter.Cut(e, Set.of()));
        return out;
    }

    @Test public void chunksWithMatchingLengthAndDialoguePassUntouched() {
        Chapter c = new Chapter().ones(30, 100);
        ChunkChecks.Result r = check(c, 1.0, cutsAt(9, 19, 29));
        assertEquals(3, r.chunks().size());
        assertEquals(0, r.initialFailures());
        assertFalse(r.raisesWarn());
        assertTrue(r.chunks().stream().noneMatch(ChunkChecks.Checked::uncertain));
    }

    @Test public void aChunkWhoseDraftIsTooShortIsMergedWithTheNextOneWhenThatFixesIt() {
        Chapter c = new Chapter();
        for (int i = 0; i < 10; i++) c.bead(1, 1, 100);
        for (int i = 0; i < 10; i++) { c.raw.add("あ".repeat(100)); c.draft.add("a".repeat(40)); c.beads.add(new LineAligner.Bead(10 + i, 11 + i, 10 + i, 11 + i)); }
        for (int i = 0; i < 10; i++) { c.raw.add("あ".repeat(100)); c.draft.add("a".repeat(160)); c.beads.add(new LineAligner.Bead(20 + i, 21 + i, 20 + i, 21 + i)); }
        ChunkChecks.Result r = check(c, 1.0, cutsAt(9, 19, 29));
        assertEquals("both the short and the long chunk fail at first", 2, r.initialFailures());
        assertEquals(2, r.chunks().size());
        assertEquals(10, r.chunks().get(1).beadFrom());
        assertEquals(29, r.chunks().get(1).beadTo());
        assertTrue(r.chunks().get(1).flags().contains("MERGED"));
        assertFalse(r.chunks().get(1).uncertain());
    }

    @Test public void aChunkThatStillFailsAfterTheMergeIsMarkedUncertain() {
        Chapter c = new Chapter().ones(10, 100);
        for (int i = 0; i < 20; i++) { c.raw.add("あ".repeat(100)); c.draft.add("a".repeat(10)); c.beads.add(new LineAligner.Bead(10 + i, 11 + i, 10 + i, 11 + i)); }
        ChunkChecks.Result r = check(c, 1.0, cutsAt(9, 19, 29));
        assertTrue(r.chunks().stream().anyMatch(ChunkChecks.Checked::uncertain));
        assertTrue(r.chunks().stream().filter(ChunkChecks.Checked::uncertain).allMatch(x -> x.failures().contains("LENGTH")));
    }

    @Test public void dialogueCountsMayDifferByOneButNotByTwo() {
        Chapter c = new Chapter().ones(10, 100);
        c.raw.set(0, "「" + c.raw.get(0).substring(1));
        c.raw.set(1, "『" + c.raw.get(1).substring(1));
        ChunkChecks.Result two = check(c, 1.0, cutsAt(9));
        assertTrue(two.chunks().get(0).uncertain() && two.chunks().get(0).failures().contains("DIALOGUE"));
        c.draft.set(0, "「" + c.draft.get(0).substring(1));
        ChunkChecks.Result one = check(c, 1.0, cutsAt(9));
        assertFalse(one.chunks().get(0).uncertain());
    }

    @Test public void moreThanTwentyPercentFailingChunksRaisesTheChapterToWarn() {
        Chapter c = new Chapter();
        for (int part = 0; part < 5; part++) {
            for (int i = 0; i < 10; i++) {
                int k = part * 10 + i;
                c.raw.add("あ".repeat(100));
                c.draft.add("a".repeat(part < 2 ? 5 : 100)); // chunks 0 and 1 fail: 2 of 5 = 40 %
                c.beads.add(new LineAligner.Bead(k, k + 1, k, k + 1));
            }
        }
        ChunkChecks.Result r = check(c, 1.0, cutsAt(9, 19, 29, 39, 49));
        assertEquals(2, r.initialFailures());
        assertTrue(r.raisesWarn());
        Chapter ok = new Chapter().ones(50, 100);
        assertFalse(check(ok, 1.0, cutsAt(9, 19, 29, 39, 49)).raisesWarn());
    }
}
