package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Line oriented difference of DRAFT against EDITED. The changed passages are what the check call sees (before -> after),
 * and the share of changed characters is the rewrite measure. Pure and deterministic; no I/O.
 */
public final class LineDiff {
    /** A run of changed lines: {@code before} (DRAFT side, may be empty) and {@code after} (EDITED side, may be empty). */
    public record Segment(int draftStartLine, int editedStartLine, String before, String after) {
        /** Characters that really differ: the common start and end of the two sides are not a change. */
        public int changedChars() {
            int limit = Math.min(before.length(), after.length());
            int prefix = 0;
            while (prefix < limit && before.charAt(prefix) == after.charAt(prefix)) prefix++;
            int suffix = 0;
            while (suffix < limit - prefix && before.charAt(before.length() - 1 - suffix) == after.charAt(after.length() - 1 - suffix)) suffix++;
            return Math.max(before.length(), after.length()) - prefix - suffix;
        }
    }

    /** Above this many cells the middle block is reported as one segment instead of being aligned line by line. */
    static final long MAX_DP_CELLS = 16_000_000L;

    private LineDiff() { }

    public static String[] lines(String text) {
        return (text == null ? "" : text).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    public static List<Segment> segments(String draft, String edited) {
        String[] a = lines(draft);
        String[] b = lines(edited);
        int prefix = 0;
        while (prefix < a.length && prefix < b.length && a[prefix].equals(b[prefix])) prefix++;
        int suffix = 0;
        while (suffix < a.length - prefix && suffix < b.length - prefix
                && a[a.length - 1 - suffix].equals(b[b.length - 1 - suffix])) suffix++;
        int an = a.length - prefix - suffix;
        int bn = b.length - prefix - suffix;
        List<Segment> out = new ArrayList<>();
        if (an == 0 && bn == 0) return out;
        if ((long) an * bn > MAX_DP_CELLS) {
            out.add(segment(a, b, prefix, prefix + an, prefix, prefix + bn));
            return out;
        }
        int[][] lcs = new int[an + 1][bn + 1];
        for (int i = an - 1; i >= 0; i--) {
            for (int j = bn - 1; j >= 0; j--) {
                lcs[i][j] = a[prefix + i].equals(b[prefix + j]) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        int i = 0;
        int j = 0;
        int startA = -1;
        int startB = -1;
        while (i < an || j < bn) {
            if (i < an && j < bn && a[prefix + i].equals(b[prefix + j])) {
                if (startA >= 0) {
                    out.add(segment(a, b, prefix + startA, prefix + i, prefix + startB, prefix + j));
                    startA = -1;
                }
                i++;
                j++;
                continue;
            }
            if (startA < 0) { startA = i; startB = j; }
            if (j >= bn || (i < an && lcs[i + 1][j] >= lcs[i][j + 1])) i++;
            else j++;
        }
        if (startA >= 0) out.add(segment(a, b, prefix + startA, prefix + an, prefix + startB, prefix + bn));
        return out;
    }

    private static Segment segment(String[] a, String[] b, int aFrom, int aTo, int bFrom, int bTo) {
        return new Segment(aFrom + 1, bFrom + 1, join(a, aFrom, aTo), join(b, bFrom, bTo));
    }

    private static String join(String[] lines, int from, int to) {
        StringBuilder out = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from) out.append('\n');
            out.append(lines[i]);
        }
        return out.toString();
    }

    public static long changedChars(List<Segment> segments) {
        long total = 0;
        for (Segment segment : segments) total += segment.changedChars();
        return total;
    }
}
