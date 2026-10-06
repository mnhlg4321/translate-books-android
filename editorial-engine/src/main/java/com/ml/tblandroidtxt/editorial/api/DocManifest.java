package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;

/**
 * One document (RAW or DRAFT) with its own coordinates: the normalized text, its hash, and the ordered units that cover it.
 * A unit is the trimmed text of one row; everything between two units (and before the first / after the last) is separator,
 * so the document is exactly {@code edgeLead + unit0 + boundary0 + unit1 + ... + edgeTrail}. RAW and DRAFT never share
 * offsets or ordinals; the PairMap links them explicitly. Pure.
 */
public final class DocManifest {
    public enum Kind { RAW, DRAFT }

    /** A trimmed main range [start, end) of the document with its paragraph numbers (whole-chapter, 1-based, inclusive). */
    public record Unit(String id, int start, int end, int paragraphStart, int paragraphEnd, String mainHash) { }

    public record Issue(String code, String detail) { }

    public final Kind kind;
    public final String docId;
    public final String chapterId;
    public final String normalizationRevision;
    public final String text;
    public final String textSha256;
    private final List<Unit> units;
    private final int[][] paragraphs; // [start, end) of every paragraph

    private DocManifest(Kind kind, String docId, String chapterId, String text, List<int[]> ranges) {
        this.kind = kind;
        this.docId = docId == null ? "" : docId;
        this.chapterId = chapterId == null ? "" : chapterId;
        this.normalizationRevision = PairContract.NORMALIZATION_REVISION;
        this.text = text;
        this.textSha256 = PairText.sha256(text);
        this.paragraphs = paragraphsOf(text);
        List<Unit> built = new ArrayList<>();
        for (int[] r : ranges) built.add(unitOf(r[0], r[1]));
        this.units = List.copyOf(built);
    }

    /**
     * Builds the document from row texts in order. A {@code null} or blank row has no unit (a missing row). The separator
     * between two units is the trailing whitespace of the first plus the leading whitespace of the second;
     * {@code appendNewline} reproduces the job output contract, which adds a newline after a row that does not end with one.
     */
    public static DocManifest fromRows(Kind kind, String docId, String chapterId, List<String> rows, boolean appendNewline) {
        StringBuilder doc = new StringBuilder();
        List<int[]> ranges = new ArrayList<>();
        String pendingTrail = null;
        for (String row : rows) {
            String normalized = PairText.normalize(row);
            if (PairText.isBlank(normalized)) continue;
            int a = PairText.leadingWhitespace(normalized);
            int b = PairText.trailingWhitespaceStart(normalized);
            String lead = normalized.substring(0, a);
            String main = normalized.substring(a, b);
            String trail = normalized.substring(b);
            if (pendingTrail != null) doc.append(pendingTrail);
            doc.append(lead);
            int start = doc.length();
            doc.append(main);
            ranges.add(new int[] {start, doc.length()});
            pendingTrail = appendNewline && !trail.endsWith("\n") ? trail + "\n" : trail;
        }
        if (pendingTrail != null) doc.append(pendingTrail);
        return new DocManifest(kind, docId, chapterId, doc.toString(), ranges);
    }

    /** For fixtures and imports that already carry ranges over the normalized text. */
    public static DocManifest fromText(Kind kind, String docId, String chapterId, String text, int[][] ranges) {
        List<int[]> list = new ArrayList<>();
        for (int[] r : ranges) list.add(new int[] {r[0], r[1]});
        return new DocManifest(kind, docId, chapterId, PairText.normalize(text), list);
    }

    private DocManifest(DocManifest base, List<Unit> replacement) {
        this.kind = base.kind;
        this.docId = base.docId;
        this.chapterId = base.chapterId;
        this.normalizationRevision = base.normalizationRevision;
        this.text = base.text;
        this.textSha256 = base.textSha256;
        this.paragraphs = base.paragraphs;
        this.units = List.copyOf(replacement);
    }

    /** Copy with other units (tests and deserialization); the units are not re-derived, so a wrong hash stays wrong. */
    DocManifest withUnits(List<Unit> replacement) { return new DocManifest(this, replacement); }

    private Unit unitOf(int start, int end) {
        int s = Math.max(0, Math.min(start, text.length()));
        int e = Math.max(s, Math.min(end, text.length()));
        String main = text.substring(s, e);
        String hash = PairText.sha256(main);
        String id = PairText.sha256(kind + ":" + s + ":" + e + ":" + hash).substring(0, 24);
        return new Unit(id, start, end, paragraphAt(start, false), paragraphAt(end - 1, true), hash);
    }

    public List<Unit> units() { return units; }

    public Unit unit(String id) {
        for (Unit u : units) if (u.id().equals(id)) return u;
        return null;
    }

    public int indexOf(String id) {
        for (int i = 0; i < units.size(); i++) if (units.get(i).id().equals(id)) return i;
        return -1;
    }

    public String main(Unit unit) { return text.substring(unit.start(), unit.end()); }

    public int paragraphCount() { return paragraphs.length; }

    public String edgeLead() { return units.isEmpty() ? text : text.substring(0, units.get(0).start()); }

    public String edgeTrail() { return units.isEmpty() ? "" : text.substring(units.get(units.size() - 1).end()); }

    /** Separator between unit {@code index} and the next one. */
    public String boundaryAfter(int index) {
        return text.substring(units.get(index).end(), units.get(index + 1).start());
    }

    /** Reference-only text before {@code offset}, bounded; never part of a main range's output. */
    public String contextBefore(int offset, int maxChars) {
        return text.substring(Math.max(0, offset - maxChars), Math.max(0, offset));
    }

    public String contextAfter(int offset, int maxChars) {
        return text.substring(Math.min(text.length(), offset), Math.min(text.length(), offset + maxChars));
    }

    private int paragraphAt(int offset, boolean preferPrevious) {
        if (paragraphs.length == 0) return 0;
        for (int i = 0; i < paragraphs.length; i++) {
            if (offset < paragraphs[i][0]) return preferPrevious ? Math.max(1, i) : i + 1;
            if (offset < paragraphs[i][1]) return i + 1;
        }
        return paragraphs.length;
    }

    /** Paragraph = block of non-blank lines separated by blank lines; numbered from 1 over the whole document. */
    private static int[][] paragraphsOf(String text) {
        List<int[]> out = new ArrayList<>();
        int lineStart = 0;
        int blockStart = -1;
        int blockEnd = -1;
        while (lineStart <= text.length()) {
            int nl = text.indexOf('\n', lineStart);
            int lineEnd = nl < 0 ? text.length() : nl;
            boolean blank = PairText.isBlank(text.substring(lineStart, lineEnd));
            if (!blank) {
                if (blockStart < 0) blockStart = lineStart;
                blockEnd = lineEnd;
            } else if (blockStart >= 0) {
                out.add(new int[] {blockStart, blockEnd});
                blockStart = -1;
            }
            if (nl < 0) break;
            lineStart = nl + 1;
        }
        if (blockStart >= 0) out.add(new int[] {blockStart, blockEnd});
        return out.toArray(new int[0][]);
    }

    /** Structural checks of the document itself; empty = the units cover the text exactly with separators in between. */
    public List<Issue> verify() {
        List<Issue> issues = new ArrayList<>();
        if (!PairText.sha256(text).equals(textSha256)) issues.add(new Issue("DOC_HASH_MISMATCH", docId));
        int cursor = 0;
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (u.start() < 0 || u.end() > text.length() || u.end() <= u.start()) {
                issues.add(new Issue("UNIT_BOUNDS", u.id()));
                continue;
            }
            if (u.start() < cursor) issues.add(new Issue(i > 0 && u.start() < units.get(i - 1).start() ? "UNIT_ORDER" : "UNIT_OVERLAP", u.id()));
            String main = text.substring(u.start(), u.end());
            if (!PairText.sha256(main).equals(u.mainHash())) issues.add(new Issue("UNIT_HASH_MISMATCH", u.id()));
            if (!PairText.trim(main).equals(main)) issues.add(new Issue("UNIT_NOT_TRIMMED", u.id()));
            if (u.start() > cursor && !PairText.isBlank(text.substring(cursor, u.start()))) issues.add(new Issue("COVERAGE_GAP", "before " + u.id()));
            cursor = Math.max(cursor, u.end());
        }
        if (cursor < text.length() && !PairText.isBlank(text.substring(cursor))) issues.add(new Issue("COVERAGE_GAP", "after last unit"));
        return issues;
    }

    /** Hash of the coverage shape: unit ranges and separator lengths, without any text. */
    public String coverageHash() {
        StringBuilder sb = new StringBuilder(kind.name()).append('|').append(textSha256);
        for (Unit u : units) sb.append('|').append(u.start()).append('-').append(u.end()).append(':').append(u.mainHash());
        return PairText.sha256(sb.toString());
    }
}
