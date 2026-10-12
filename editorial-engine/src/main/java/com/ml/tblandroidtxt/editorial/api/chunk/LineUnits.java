package com.ml.tblandroidtxt.editorial.api.chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CS-1 step S2: a text as a list of non-blank lines ("units"). Each unit keeps the exact original characters of its line,
 * its line terminator and the blank lines that follow it (leading blank lines belong to the first unit), so any run of units
 * is a slice of the original and the slices of consecutive runs concatenate back to the file byte for byte. CRLF, LF and a
 * lone CR all end a line; the analysis text of a unit is the line stripped the way the reference measurements strip it.
 */
public final class LineUnits {
    /** One non-blank line: {@code text} is the stripped line, [{@code start}, {@code end}) its slice of the source. */
    public record Unit(int index, String text, int start, int end, int physicalLine, int codePoints) { }

    private final String source;
    private final List<Unit> units;

    private LineUnits(String source, List<Unit> units) {
        this.source = source;
        this.units = Collections.unmodifiableList(units);
    }

    public static LineUnits parse(String source) {
        String text = source == null ? "" : source;
        List<int[]> lines = new ArrayList<>(); // start, contentEnd
        int i = 0;
        while (i < text.length()) {
            int contentEnd = i;
            while (contentEnd < text.length() && text.charAt(contentEnd) != '\n' && text.charAt(contentEnd) != '\r') contentEnd++;
            int next = contentEnd;
            if (next < text.length()) {
                if (text.charAt(next) == '\r' && next + 1 < text.length() && text.charAt(next + 1) == '\n') next += 2;
                else next += 1;
            }
            lines.add(new int[] {i, contentEnd});
            i = next;
        }
        List<String> stripped = new ArrayList<>();
        List<int[]> starts = new ArrayList<>(); // line start offset, physical line number
        for (int n = 0; n < lines.size(); n++) {
            String s = strip(text.substring(lines.get(n)[0], lines.get(n)[1]));
            if (s.isEmpty()) continue;
            stripped.add(s);
            starts.add(new int[] {lines.get(n)[0], n + 1});
        }
        List<Unit> out = new ArrayList<>();
        for (int k = 0; k < stripped.size(); k++) {
            int start = k == 0 ? 0 : starts.get(k)[0];
            int end = k + 1 < stripped.size() ? starts.get(k + 1)[0] : text.length();
            String s = stripped.get(k);
            out.add(new Unit(k, s, start, end, starts.get(k)[1], s.codePointCount(0, s.length())));
        }
        return new LineUnits(text, out);
    }

    public String source() { return source; }

    public List<Unit> units() { return units; }

    public int size() { return units.size(); }

    /** Exact original text of units [from, to). Empty for an empty range. */
    public String slice(int from, int to) {
        if (from >= to) return "";
        return source.substring(units.get(from).start(), units.get(to - 1).end());
    }

    /** The stripped lines of units [from, to) joined by LF: what measures and anchors look at. */
    public String analysis(int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from) sb.append('\n');
            sb.append(units.get(i).text());
        }
        return sb.toString();
    }

    public List<String> texts() {
        List<String> out = new ArrayList<>(units.size());
        for (Unit u : units) out.add(u.text());
        return out;
    }

    /** The text with CRLF and lone CR turned into LF, which is what analysis code sees. */
    public String lfText() { return source.replace("\r\n", "\n").replace('\r', '\n'); }

    /** Maps an offset in {@link #lfText()} back to the offset of the same character in the source. */
    public int originalOffset(int lfOffset) {
        int original = 0;
        int lf = 0;
        while (original < source.length() && lf < lfOffset) {
            char c = source.charAt(original);
            if (c == '\r' && original + 1 < source.length() && source.charAt(original + 1) == '\n') original += 2;
            else original += 1;
            lf++;
        }
        return original;
    }

    /** Whitespace as the reference tooling understands it: ASCII controls 9-13 and 28-31, space, NEL, NBSP, U+1680, U+2000-200A, U+2028/9, U+202F, U+205F, U+3000. */
    public static boolean isSpace(int cp) {
        return (cp >= 9 && cp <= 13) || (cp >= 28 && cp <= 32) || cp == 0x85 || cp == 0xA0 || cp == 0x1680
                || (cp >= 0x2000 && cp <= 0x200A) || cp == 0x2028 || cp == 0x2029 || cp == 0x202F || cp == 0x205F || cp == 0x3000;
    }

    public static String strip(String s) {
        int a = 0;
        int b = s.length();
        while (a < b && isSpace(s.charAt(a))) a++;
        while (b > a && isSpace(s.charAt(b - 1))) b--;
        return s.substring(a, b);
    }
}
