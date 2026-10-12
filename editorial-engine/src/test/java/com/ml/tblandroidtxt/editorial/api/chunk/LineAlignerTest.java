package com.ml.tblandroidtxt.editorial.api.chunk;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** S3: every kind of step, compensating split + merge, runs of merges, empty inputs. Synthetic text only. */
public final class LineAlignerTest {
    private static final List<Anchors.Term> NONE = List.of();

    /** RAW line k: distinct Japanese text with the number k; DRAFT line: Vietnamese-length text with the same number. */
    private static String raw(int k) {
        String body = "これは" + k + "番目の文章であり、それなりの長さをもっています。";
        return k % 2 == 0 ? "「" + body + "」" : body;
    }

    private static String draft(int k) {
        String body = "Đây là câu văn thứ " + k + ", và nó có độ dài tương ứng với câu gốc trong bản tiếng Nhật.";
        return k % 2 == 0 ? "「" + body + "」" : body;
    }

    private static List<String> raws(int n) { List<String> l = new ArrayList<>(); for (int k = 1; k <= n; k++) l.add(raw(k)); return l; }

    private static List<String> drafts(int n) { List<String> l = new ArrayList<>(); for (int k = 1; k <= n; k++) l.add(draft(k)); return l; }

    private static String kinds(LineAligner.Alignment a) {
        StringBuilder sb = new StringBuilder();
        for (LineAligner.Bead b : a.beads()) sb.append(b.rawCount()).append(b.draftCount()).append(' ');
        return sb.toString().trim();
    }

    private static void assertCovers(LineAligner.Alignment a, int r, int d) {
        int ri = 0;
        int di = 0;
        for (LineAligner.Bead b : a.beads()) {
            assertEquals(ri, b.rawStart());
            assertEquals(di, b.draftStart());
            ri = b.rawEnd();
            di = b.draftEnd();
        }
        assertEquals(r, ri);
        assertEquals(d, di);
    }

    @Test public void equalChaptersAlignOneToOne() {
        LineAligner.Alignment a = LineAligner.align(raws(12), drafts(12), NONE);
        assertEquals("11 11 11 11 11 11 11 11 11 11 11 11", kinds(a));
        assertCovers(a, 12, 12);
    }

    @Test public void aDraftThatSplitsOneRawLineIsAOneToTwoStep() {
        List<String> d = drafts(10);
        String line = d.remove(4);
        int cut = line.indexOf(',') + 1;
        d.add(4, line.substring(0, cut).trim());
        d.add(5, line.substring(cut).trim());
        LineAligner.Alignment a = LineAligner.align(raws(10), d, NONE);
        assertEquals("11 11 11 11 12 11 11 11 11 11", kinds(a));
        assertCovers(a, 10, 11);
    }

    @Test public void aDraftThatMergesTwoRawLinesIsATwoToOneStep() {
        List<String> d = drafts(10);
        String merged = d.get(4) + " " + d.get(5);
        d.remove(5);
        d.set(4, merged);
        LineAligner.Alignment a = LineAligner.align(raws(10), d, NONE);
        assertEquals("11 11 11 11 21 11 11 11 11", kinds(a));
        assertCovers(a, 10, 9);
    }

    @Test public void aRawLineWithNoDraftLineIsAOneToZeroStep() {
        List<String> d = drafts(10);
        d.remove(5);
        List<String> r = raws(10);
        // RAW line 6 is a long dialogue line with its own number: no neighbour absorbs it for less than the skip penalty
        r.set(5, "「これは6番目の文章であり、" + "あ".repeat(400) + "」");
        LineAligner.Alignment a = LineAligner.align(r, d, NONE);
        assertEquals(1, a.beads().stream().filter(b -> b.draftCount() == 0).count());
        assertEquals(0, a.beads().stream().filter(b -> b.rawCount() == 0).count());
        assertEquals(5, a.beads().stream().filter(b -> b.draftCount() == 0).findFirst().orElseThrow().rawStart());
        assertCovers(a, 10, 9);
    }

    @Test public void aDraftLineWithNoRawLineIsAZeroToOneStep() {
        List<String> d = drafts(10);
        d.add(3, "◇◇◇ 1001 1002 1003 1004 1005 1006 1007 1008 1009 1010 " + "x".repeat(100)); // other marks and ten numbers nothing in RAW has: no split explains it
        LineAligner.Alignment a = LineAligner.align(raws(10), d, NONE);
        assertEquals(1, a.beads().stream().filter(b -> b.rawCount() == 0).count());
        assertEquals(3, a.beads().stream().filter(b -> b.rawCount() == 0).findFirst().orElseThrow().draftStart());
        assertCovers(a, 10, 11);
    }

    @Test public void aSplitAndAMergeThatCancelOutAreBothFoundEvenWithEqualLineCounts() {
        List<String> d = drafts(14);
        // split line 3 into two, merge lines 9 and 10 into one: 14 draft lines again
        String line = d.get(2);
        int cut = line.indexOf(',') + 1;
        d.set(2, line.substring(0, cut).trim());
        d.add(3, line.substring(cut).trim());
        int mergeAt = 9; // 0-based index of draft line for RAW 9 after the split shifted everything by one
        String merged = d.get(mergeAt) + " " + d.get(mergeAt + 1);
        d.remove(mergeAt + 1);
        d.set(mergeAt, merged);
        assertEquals(14, d.size());
        LineAligner.Alignment a = LineAligner.align(raws(14), d, NONE);
        long split = a.beads().stream().filter(b -> b.rawCount() == 1 && b.draftCount() == 2).count();
        long merge = a.beads().stream().filter(b -> b.rawCount() == 2 && b.draftCount() == 1).count();
        assertEquals(1, split);
        assertEquals(1, merge);
        assertCovers(a, 14, 14);
    }

    @Test public void aRunOfMergesIsAlignedWithoutLosingTheLinesAround() {
        List<String> d = new ArrayList<>();
        for (int k = 1; k <= 20; k++) {
            if (k >= 8 && k <= 13) {
                if (k % 2 == 0) d.add(draft(k) + " " + draft(k + 1)); // RAW 8+9, 10+11 and 12+13 each become one DRAFT line
            } else {
                d.add(draft(k));
            }
        }
        LineAligner.Alignment a = LineAligner.align(raws(20), d, NONE);
        assertEquals(3, a.beads().stream().filter(b -> b.rawCount() == 2 && b.draftCount() == 1).count());
        assertEquals(14, a.beads().stream().filter(LineAligner.Bead::oneToOne).count());
        assertCovers(a, 20, d.size());
    }

    @Test public void glossaryAnchorsDecideWhichRawLineAMergedDraftLineBelongsTo() {
        List<Anchors.Term> terms = Anchors.terms(List.of(new com.ml.tblandroidtxt.editorial.api.EditInputs.GlossaryEntry("魔王", "Ma Vương", "", "")));
        List<String> raw = List.of("勇者は旅に出た。", "魔王が待っていた。", "二人は戦った。", "世界は救われた。");
        List<String> draft = List.of("Người anh hùng lên đường.", "Ma Vương đang chờ.", "Hai người giao chiến.", "Thế giới được cứu.");
        LineAligner.Alignment a = LineAligner.align(raw, draft, terms);
        assertEquals("11 11 11 11", kinds(a));
        assertTrue(a.rawAnchors().get(1).contains("ma vương"));
        assertTrue(a.draftAnchors().get(1).contains("ma vương"));
    }

    @Test public void emptyInputsGiveNoBeadsOrOnlyUnpairedBeads() {
        assertEquals(0, LineAligner.align(List.of(), List.of(), NONE).beads().size());
        LineAligner.Alignment onlyRaw = LineAligner.align(raws(3), List.of(), NONE);
        assertEquals("10 10 10", kinds(onlyRaw));
        LineAligner.Alignment onlyDraft = LineAligner.align(List.of(), drafts(2), NONE);
        assertEquals("01 01", kinds(onlyDraft));
    }

    @Test public void aLongChapterStaysInsideTheBandAndStillCoversEveryLine() {
        long start = System.nanoTime();
        LineAligner.Alignment a = LineAligner.align(raws(1200), drafts(1190), NONE);
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertCovers(a, 1200, 1190);
        assertTrue("1200 lines should align in well under 5 s on a JVM, took " + millis, millis < 5000);
    }

    @Test public void beadsSurviveRunLengthEncoding() {
        List<String> d = drafts(10);
        d.remove(5);
        LineAligner.Alignment a = LineAligner.align(raws(10), d, NONE);
        assertEquals(a.beads(), ChunkPlan.decodeBeads(ChunkPlan.encodeBeads(a.beads())));
    }
}
