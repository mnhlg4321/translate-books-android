package com.ml.tblandroidtxt.editorial.api.chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CS-1 step S3: aligns the non-blank lines of RAW with those of DRAFT, Gale-Church style. Moves are 1-1, 1-2 (DRAFT split a
 * RAW line), 2-1 (DRAFT merged two RAW lines), and the penalised 1-0 and 0-1 (a RAW line with no DRAFT line, or the reverse).
 * The cost of a pair is the log distance between its DRAFT length and the length the chapter's own VI/JP ratio predicts, plus
 * 1.5 when the opening mark differs, 1.0 when the closing mark differs, plus the anchor term (glossary targets and numbers
 * that disagree add cost, shared ones remove cost). The dynamic programme runs inside a diagonal band of |R-D|+50 so a long
 * chapter stays light. Ties resolve exactly as the reference implementation (forward relaxation in row-major order, strict
 * improvement), because the measured cut and verdict figures were taken with it.
 */
public final class LineAligner {
    public static final double SPLIT_PENALTY = 3.0;
    public static final double SKIP_PENALTY = 6.0;
    public static final int BAND_SLACK = 50;
    private static final String OPEN = "「『【（(〝《◇◆＊*─―";
    private static final String CLOSE = "」』】）)〟》";
    private static final int[] DI = {1, 1, 2, 1, 0};
    private static final int[] DJ = {1, 2, 1, 0, 1};
    private static final double[] PEN = {0, SPLIT_PENALTY, SPLIT_PENALTY, SKIP_PENALTY, SKIP_PENALTY};

    /** Lines [rawStart, rawEnd) of RAW face lines [draftStart, draftEnd) of DRAFT. */
    public record Bead(int rawStart, int rawEnd, int draftStart, int draftEnd) {
        public int rawCount() { return rawEnd - rawStart; }

        public int draftCount() { return draftEnd - draftStart; }

        public boolean oneToOne() { return rawCount() == 1 && draftCount() == 1; }

        public boolean oneSideEmpty() { return rawCount() == 0 || draftCount() == 0; }
    }

    public record Alignment(List<Bead> beads, List<Set<String>> rawAnchors, List<Set<String>> draftAnchors, double ratio) { }

    private LineAligner() { }

    /** The mark that opens a line, or {@code '.'}. */
    public static char openMark(String line) {
        char c = line.charAt(0);
        return OPEN.indexOf(c) >= 0 ? c : '.';
    }

    /** The mark that closes a line, or {@code '.'}. */
    public static char closeMark(String line) {
        char c = line.charAt(line.length() - 1);
        return CLOSE.indexOf(c) >= 0 ? c : '.';
    }

    /** @param raw stripped non-blank RAW lines; @param draft stripped non-blank DRAFT lines */
    public static Alignment align(List<String> raw, List<String> draft, List<Anchors.Term> terms) {
        final int r = raw.size();
        final int d = draft.size();
        long rawTotal = 0;
        long draftTotal = 0;
        int[] rawLen = new int[r];
        int[] draftLen = new int[d];
        for (int i = 0; i < r; i++) { rawLen[i] = raw.get(i).codePointCount(0, raw.get(i).length()); rawTotal += rawLen[i]; }
        for (int j = 0; j < d; j++) { draftLen[j] = draft.get(j).codePointCount(0, draft.get(j).length()); draftTotal += draftLen[j]; }
        final double ratio = (double) draftTotal / Math.max(1L, rawTotal);
        List<Set<String>> ar = new ArrayList<>(r);
        List<Set<String>> ad = new ArrayList<>(d);
        for (String line : raw) ar.add(Anchors.raw(line, terms));
        for (String line : draft) ad.add(Anchors.draft(line, terms));
        long[] rawPrefix = new long[r + 1];
        long[] draftPrefix = new long[d + 1];
        for (int i = 0; i < r; i++) rawPrefix[i + 1] = rawPrefix[i] + rawLen[i];
        for (int j = 0; j < d; j++) draftPrefix[j + 1] = draftPrefix[j] + draftLen[j];

        final int width = d + 1;
        final int band = Math.abs(r - d) + BAND_SLACK;
        double[] cost = new double[(r + 1) * width];
        java.util.Arrays.fill(cost, Double.POSITIVE_INFINITY);
        byte[] back = new byte[(r + 1) * width];
        cost[0] = 0;
        for (int i = 0; i <= r; i++) {
            int jLo = Math.max(0, i - band);
            int jHi = Math.min(d, i + band);
            for (int j = jLo; j <= jHi; j++) {
                double here = cost[i * width + j];
                if (here == Double.POSITIVE_INFINITY) continue;
                for (int m = 0; m < 5; m++) {
                    int a = i + DI[m];
                    int b = j + DJ[m];
                    if (a > r || b > d || Math.abs(a - b) > band) continue;
                    double c = here + PEN[m];
                    if (DI[m] != 0 && DJ[m] != 0) {
                        c = c + pairCost(raw, draft, ar, ad, rawPrefix, draftPrefix, ratio, i, a, j, b);
                    }
                    int cell = a * width + b;
                    if (c < cost[cell]) { cost[cell] = c; back[cell] = (byte) (m + 1); }
                }
            }
        }
        List<Bead> beads = new ArrayList<>();
        int i = r;
        int j = d;
        while (i != 0 || j != 0) {
            int m = back[i * width + j] - 1;
            if (m < 0) throw new IllegalStateException("ALIGNMENT_UNREACHABLE");
            int pi = i - DI[m];
            int pj = j - DJ[m];
            beads.add(new Bead(pi, i, pj, j));
            i = pi;
            j = pj;
        }
        Collections.reverse(beads);
        return new Alignment(Collections.unmodifiableList(beads), ar, ad, ratio);
    }

    private static double pairCost(List<String> raw, List<String> draft, List<Set<String>> ar, List<Set<String>> ad,
                                   long[] rawPrefix, long[] draftPrefix, double ratio, int i, int a, int j, int b) {
        double lr = rawPrefix[a] - rawPrefix[i];
        double ld = draftPrefix[b] - draftPrefix[j];
        double c = Math.abs(Math.log((ld + 1) / (ratio * lr + 1)));
        if (openMark(raw.get(i)) != openMark(draft.get(j))) c += 1.5;
        if (closeMark(raw.get(a - 1)) != closeMark(draft.get(b - 1))) c += 1.0;
        Set<String> sa;
        Set<String> sd;
        if (a - i == 1) sa = ar.get(i); else { sa = new HashSet<>(); for (int k = i; k < a; k++) sa.addAll(ar.get(k)); }
        if (b - j == 1) sd = ad.get(j); else { sd = new HashSet<>(); for (int k = j; k < b; k++) sd.addAll(ad.get(k)); }
        if (!sa.isEmpty() || !sd.isEmpty()) {
            int inter = 0;
            Set<String> small = sa.size() <= sd.size() ? sa : sd;
            Set<String> large = small == sa ? sd : sa;
            for (String s : small) if (large.contains(s)) inter++;
            int union = sa.size() + sd.size() - inter;
            int sym = union - inter;
            int denominator = Math.max(1, union);
            c += (2.0 * sym) / denominator - (1.0 * inter) / denominator;
        }
        return c;
    }
}
