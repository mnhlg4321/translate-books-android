package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which glossary entries and pronoun rows a request sees, and where each row applies (chunk-pair package, section 5).
 * The order is fixed: (A) parse, (B) the scope of the row against the main range, (C) the cue in MAIN or CONTEXT, (D) dedupe
 * that keeps the scope. Scope is never inferred, never ranked and never lost: two rows of the same pair of characters with
 * different forms of address that share a paragraph are a conflict, both rows stay. The same code serves the whole chapter
 * (main = every paragraph) and one pair. Pure.
 */
public final class ReferenceProjector {
    /** Cue fields of the API V1 filter at N5; the choice is pinned per run and must be the same for whole and chunk. */
    public static final Set<String> DEFAULT_CUE_FIELDS = Set.of("from", "speaker", "target");

    private static final Pattern RANGE = Pattern.compile("(?i)^p([0-9]+)(?:-p([0-9]+))?$");
    private static final Pattern CHAPTER_PREFIX = Pattern.compile("(?i)CH([0-9]+)");

    /**
     * @param chapterId     the chapter the RAW belongs to ({@code ""} when unknown: a {@code CHnnn:} scope then fails closed)
     * @param mainParaStart first whole-chapter paragraph of the main range (1-based, inclusive)
     * @param mainParaEnd   last whole-chapter paragraph of the main range
     * @param cueFields     which pronoun columns may name a cue: from, speaker, target
     */
    public record Params(String chapterId, int mainParaStart, int mainParaEnd, String mainText, String contextBefore, String contextAfter,
                         Set<String> cueFields) {
        public Params {
            chapterId = chapterId == null ? "" : chapterId;
            mainText = PairText.normalize(mainText);
            contextBefore = PairText.normalize(contextBefore);
            contextAfter = PairText.normalize(contextAfter);
            cueFields = cueFields == null || cueFields.isEmpty() ? DEFAULT_CUE_FIELDS : Set.copyOf(cueFields);
        }
    }

    /** A pronoun row after dedupe. {@code effective} is the merged scope inside the main range; {@code partial} = not all of it. */
    public record Row(String line, String from, String speaker, String target, String self, String call, String note, boolean p3,
                      List<int[]> effective, boolean partial, Set<String> regions) {
        public Row {
            effective = List.copyOf(effective);
            regions = Set.copyOf(regions);
        }

        public String key() { return String.join("\u0001", from, speaker, target, self, call, note); }

        public boolean covers(int paragraph) {
            for (int[] r : effective) if (paragraph >= r[0] && paragraph <= r[1]) return true;
            return false;
        }
    }

    public record Dropped(String line, String reason) { }

    public record Conflict(String rowA, String rowB, List<int[]> overlap) { }

    public record Projection(List<EditInputs.GlossaryEntry> glossary, List<Row> pronouns, List<Dropped> dropped, List<Conflict> conflicts) {
        public Projection {
            glossary = List.copyOf(glossary);
            pronouns = List.copyOf(pronouns);
            dropped = List.copyOf(dropped);
            conflicts = List.copyOf(conflicts);
        }

        public boolean hasPartial() {
            for (Row r : pronouns) if (r.partial()) return true;
            return false;
        }

        public int count(String reason) {
            int n = 0;
            for (Dropped d : dropped) if (d.reason().equals(reason)) n++;
            return n;
        }
    }

    private ReferenceProjector() { }

    public static Projection project(Params p, List<EditInputs.GlossaryEntry> glossary, String pronounCsv) {
        return project(p, glossary, pronounCsv, true);
    }

    /** {@code fromInKey=false} exists only so a test can show what a key without {@code from} would lose. */
    static Projection project(Params p, List<EditInputs.GlossaryEntry> glossary, String pronounCsv, boolean fromInKey) {
        List<EditInputs.GlossaryEntry> keptGlossary = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (EditInputs.GlossaryEntry g : glossary == null ? List.<EditInputs.GlossaryEntry>of() : glossary) {
            String source = PairText.normalize(g.source()).trim();
            if (source.isEmpty() || g.target().isBlank() || !p.mainText().contains(source)) continue;
            if (seen.add(source + "\u0000" + g.target().trim())) keptGlossary.add(g);
        }
        List<Dropped> dropped = new ArrayList<>();
        Map<String, Row> byKey = new LinkedHashMap<>();
        String csv = PairText.normalize(pronounCsv);
        for (String rawLine : csv.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            List<String> cells = ReferenceFilter.cells(line);
            if (isHeader(cells)) continue;
            boolean p3 = cells.size() >= ReferenceFilter.PRONOUN_COLUMNS;
            String from = cell(cells, 0);
            String speaker = p3 ? cell(cells, 1) : "";
            String target = p3 ? cell(cells, 2) : cell(cells, 1);
            String self = p3 ? cell(cells, 3) : "";
            String call = p3 ? cell(cells, 4) : "";
            String scope = p3 ? cell(cells, 5) : "";
            String note = p3 ? cell(cells, 6) : cell(cells, 2);

            // (B) scope against the main range
            Scope s = parseScope(scope);
            if (!s.valid) { dropped.add(new Dropped(line, "SCOPE_INVALID")); continue; }
            if (!s.prefix.isEmpty() && !sameChapter(s.prefix, p.chapterId())) { dropped.add(new Dropped(line, "SCOPE_CHAPTER_MISMATCH")); continue; }
            int lo = s.chapterWide ? p.mainParaStart() : Math.max(s.start, p.mainParaStart());
            int hi = s.chapterWide ? p.mainParaEnd() : Math.min(s.end, p.mainParaEnd());
            if (lo > hi) { dropped.add(new Dropped(line, "SCOPE_OUTSIDE_MAIN")); continue; }

            // (C) the cue in MAIN or CONTEXT
            List<String> cues = new ArrayList<>();
            addCue(cues, p.cueFields().contains("from") ? from : "");
            if (p3) {
                addCue(cues, p.cueFields().contains("speaker") ? speaker : "");
                addCue(cues, p.cueFields().contains("target") ? target : "");
            }
            Set<String> regions = new LinkedHashSet<>();
            if (cues.isEmpty()) {
                regions.add("GLOBAL");
            } else {
                boolean main = false;
                boolean context = false;
                for (String cue : cues) {
                    if (p.mainText().contains(cue)) main = true;
                    else if (p.contextBefore().contains(cue) || p.contextAfter().contains(cue)) context = true;
                }
                if (main) regions.add("MAIN");
                else if (context) regions.add("CONTEXT_ONLY");
                else { dropped.add(new Dropped(line, "NO_CUE_IN_REGION")); continue; }
            }
            List<int[]> effective = new ArrayList<>();
            effective.add(new int[] {lo, hi});

            // (D) dedupe, scope accumulated
            String key = (fromInKey ? from : "") + "\u0001" + speaker + "\u0001" + target + "\u0001" + self + "\u0001" + call + "\u0001" + note;
            Row existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, row(line, from, speaker, target, self, call, note, p3, effective, regions, p));
            } else {
                List<int[]> merged = new ArrayList<>(existing.effective());
                merged.addAll(effective);
                Set<String> unioned = new LinkedHashSet<>(existing.regions());
                unioned.addAll(regions);
                byKey.put(key, row(existing.line(), existing.from(), speaker, target, self, call, note, p3, merge(merged), unioned, p));
            }
        }
        List<Row> rows = new ArrayList<>(byKey.values());
        return new Projection(keptGlossary, rows, dropped, conflicts(rows));
    }

    private static Row row(String line, String from, String speaker, String target, String self, String call, String note, boolean p3,
                           List<int[]> effective, Set<String> regions, Params p) {
        List<int[]> merged = merge(effective);
        boolean full = merged.size() == 1 && merged.get(0)[0] <= p.mainParaStart() && merged.get(0)[1] >= p.mainParaEnd();
        return new Row(line, from, speaker, target, self, call, note, p3, merged, !full, regions);
    }

    /** Two rows for the same speaker and target with different self/call that share a paragraph: kept, reported, never ranked. */
    private static List<Conflict> conflicts(List<Row> rows) {
        List<Conflict> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            for (int j = i + 1; j < rows.size(); j++) {
                Row a = rows.get(i);
                Row b = rows.get(j);
                if (!a.p3() || !b.p3()) continue;
                if (!a.speaker().equals(b.speaker()) || !a.target().equals(b.target())) continue;
                if (a.self().equals(b.self()) && a.call().equals(b.call())) continue;
                List<int[]> overlap = intersect(a.effective(), b.effective());
                if (!overlap.isEmpty()) out.add(new Conflict(a.key(), b.key(), overlap));
            }
        }
        return out;
    }

    static List<int[]> merge(List<int[]> ranges) {
        List<int[]> sorted = new ArrayList<>(ranges);
        sorted.sort((x, y) -> Integer.compare(x[0], y[0]));
        List<int[]> out = new ArrayList<>();
        for (int[] r : sorted) {
            if (!out.isEmpty() && r[0] <= out.get(out.size() - 1)[1] + 1) {
                out.get(out.size() - 1)[1] = Math.max(out.get(out.size() - 1)[1], r[1]);
            } else {
                out.add(new int[] {r[0], r[1]});
            }
        }
        return out;
    }

    static List<int[]> intersect(List<int[]> a, List<int[]> b) {
        List<int[]> out = new ArrayList<>();
        for (int[] x : a) for (int[] y : b) {
            int lo = Math.max(x[0], y[0]);
            int hi = Math.min(x[1], y[1]);
            if (lo <= hi) out.add(new int[] {lo, hi});
        }
        return merge(out);
    }

    private record Scope(boolean valid, boolean chapterWide, String prefix, int start, int end) { }

    private static Scope parseScope(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty() || s.equals("*")) return new Scope(true, true, "", -1, -1);
        String prefix = "";
        String value = s;
        int colon = value.indexOf(':');
        if (colon >= 0) {
            prefix = value.substring(0, colon).trim();
            if (!CHAPTER_PREFIX.matcher(prefix).matches()) return new Scope(false, false, "", -1, -1);
            value = value.substring(colon + 1).trim();
        } else if (value.regionMatches(true, 0, "CH", 0, 2)) {
            return new Scope(false, false, "", -1, -1);
        }
        Matcher m = RANGE.matcher(value);
        if (!m.matches()) return new Scope(false, false, "", -1, -1);
        try {
            int start = Integer.parseInt(m.group(1));
            int end = m.group(2) == null ? start : Integer.parseInt(m.group(2));
            if (start < 1 || end < start) return new Scope(false, false, "", -1, -1);
            return new Scope(true, false, prefix, start, end);
        } catch (NumberFormatException tooBig) {
            return new Scope(false, false, "", -1, -1);
        }
    }

    private static boolean sameChapter(String prefix, String chapterId) {
        if (chapterId == null || chapterId.isBlank()) return false;
        Matcher a = CHAPTER_PREFIX.matcher(prefix.trim());
        Matcher b = CHAPTER_PREFIX.matcher(chapterId.trim());
        if (!a.matches() || !b.matches()) return false;
        try {
            return Integer.parseInt(a.group(1)) == Integer.parseInt(b.group(1));
        } catch (NumberFormatException tooBig) {
            return false;
        }
    }

    private static boolean isHeader(List<String> cells) {
        if (cells.isEmpty()) return true;
        String first = cells.get(0).trim().toLowerCase(Locale.ROOT);
        return first.equals("from") || first.equals("source") || first.equals("nguồn");
    }

    private static String cell(List<String> cells, int index) { return index < cells.size() ? cells.get(index).trim() : ""; }

    private static void addCue(List<String> cues, String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || v.equals("*") || v.equals("-")) return;
        cues.add(v);
    }

    // ---- rendering

    /** The paragraph ranges of a row as the model reads them, matching the labels in the RAW section: {@code P003–P007, P009}. */
    public static String scopeText(List<int[]> ranges) {
        StringBuilder sb = new StringBuilder();
        for (int[] r : ranges) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(label(r[0]));
            if (r[1] != r[0]) sb.append('–').append(label(r[1]));
        }
        return sb.toString();
    }

    private static String label(int paragraph) { return String.format(Locale.ROOT, "P%03d", paragraph); }

    /** Rows as they enter the prompt: never the raw scope cell; a partial row carries where it applies. */
    public static List<String> renderPronounRows(Projection projection) {
        List<String> out = new ArrayList<>();
        for (Row r : projection.pronouns()) {
            String body = r.p3() ? r.from() + " | " + r.speaker() + " | " + r.target() + " | " + r.self() + " | " + r.call() + " | " + r.note() : r.line();
            out.add(r.partial() ? body + " [áp dụng đoạn " + scopeText(r.effective()) + "]" : body);
        }
        return out;
    }

    public static List<String> renderConflicts(Projection projection) {
        List<String> out = new ArrayList<>();
        for (Conflict c : projection.conflicts()) out.add("REFERENCE_CONFLICT đoạn " + scopeText(c.overlap()));
        return out;
    }
}
