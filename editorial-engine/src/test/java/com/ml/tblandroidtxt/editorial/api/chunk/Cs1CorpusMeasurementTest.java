package com.ml.tblandroidtxt.editorial.api.chunk;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * Opt-in measurement of CS-1 on the owner's private library; skipped unless {@code CS1_CORPUS} names the folder that holds
 * {@code JAKUAKU MONSTER} and {@code JAKUAKU MONSTER WN}. It writes only counts to the JSON file named by {@code CS1_OUT}
 * (default: build/cs1-measure.json); no book text leaves this test. Driven by {@code scripts/chunk/measure_cs1.py}.
 *
 * <p>The cut answer is derived from the owner's FINAL exactly as the reference tooling did: FINAL lines are matched to DRAFT
 * lines with a difflib-style longest-matching-block alignment (ported below), only chapters where FINAL keeps RAW's non-blank
 * line count are used, and a cut is wrong when a DRAFT line before it maps to a FINAL line at or after the RAW cut, or a DRAFT
 * line after it maps to a FINAL line before the RAW cut.
 */
public final class Cs1CorpusMeasurementTest {
    private static final ToIntFunction<String> CHARS = s -> {
        int n = 0;
        for (int i = 0; i < s.length(); ) { int cp = s.codePointAt(i); i += Character.charCount(cp); if (cp != '\n') n++; }
        return n;
    };

    private File ln;
    private File wn;

    static String decode(byte[] b) throws IOException {
        if (b.length >= 2 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xfe) return new String(b, 2, b.length - 2, StandardCharsets.UTF_16LE);
        if (b.length >= 2 && (b[0] & 0xff) == 0xfe && (b[1] & 0xff) == 0xff) return new String(b, 2, b.length - 2, StandardCharsets.UTF_16BE);
        if (b.length >= 3 && (b[0] & 0xff) == 0xef && (b[1] & 0xff) == 0xbb && (b[2] & 0xff) == 0xbf) return new String(b, 3, b.length - 3, StandardCharsets.UTF_8);
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(b)).toString();
        } catch (CharacterCodingException e) {
            return new String(b, Charset.forName("windows-31j"));
        }
    }

    static String read(File f) throws IOException { return decode(Files.readAllBytes(f.toPath())); }

    private static File first(File dir, java.util.function.Predicate<String> match) {
        String[] names = dir.list();
        if (names == null) return null;
        Arrays.sort(names);
        for (String n : names) if (match.test(n)) return new File(dir, n);
        return null;
    }

    private File lnFile(String sub, String n) { return first(new File(ln, sub), x -> x.startsWith(n + "_") && x.endsWith(".txt")); }

    private File lnGlossary(String n) {
        return first(new File(ln, "1.GLOSSARIES" + File.separator + "JAKUAKU_MONSTER_VOL1_GLOSSARY_FINAL_v1.0.0" + File.separator + "chapter_glossaries"),
                x -> x.startsWith(n + "_") && x.endsWith(".csv"));
    }

    private File wnFile(String key, String c) {
        Map<String, String> dirs = Map.of("raw", "3. RAW_JAKUAKU MONSTER WEBNOVEL", "draft", "4. DRAFT", "final", "5. FINAL", "gl", "1. JAKUAKU_MONSTER_VOL1_GLOSSARY_FINAL_v2.0.1");
        Pattern p = Pattern.compile("^\\d" + c + "-.*");
        return first(new File(wn, dirs.get(key)), x -> p.matcher(x).matches() || x.startsWith(c + "_"));
    }

    private static List<String> lines(String text) { return LineUnits.parse(text).texts(); }

    /** Python's csv.DictReader over the glossary: header row, then rows; returns (source, target) pairs as glossary entries. */
    static List<EditInputs.GlossaryEntry> glossary(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (quoted) {
                if (c == '"') { if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') { cell.append('"'); i++; } else quoted = false; }
                else cell.append(c);
            } else if (c == '"') { quoted = true; any = true; }
            else if (c == ',') { row.add(cell.toString()); cell.setLength(0); any = true; }
            else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                if (any || cell.length() > 0) { row.add(cell.toString()); rows.add(row); }
                row = new ArrayList<>();
                cell.setLength(0);
                any = false;
            } else { cell.append(c); any = true; }
        }
        if (any || cell.length() > 0) { row.add(cell.toString()); rows.add(row); }
        List<EditInputs.GlossaryEntry> out = new ArrayList<>();
        if (rows.isEmpty()) return out;
        List<String> header = rows.get(0);
        int s = header.indexOf("source");
        int t = header.indexOf("target");
        if (s < 0 || t < 0) return out;
        for (int r = 1; r < rows.size(); r++) {
            List<String> x = rows.get(r);
            String src = s < x.size() ? x.get(s) : "";
            String tgt = t < x.size() ? x.get(t) : "";
            out.add(new EditInputs.GlossaryEntry(src, tgt, "", ""));
        }
        return out;
    }

    // ---- difflib.SequenceMatcher(None, a, b, autojunk=False) opcodes, for the FINAL-derived answer

    private static int[] longest(List<String> a, Map<String, List<Integer>> b2j, int alo, int ahi, int blo, int bhi) {
        int besti = alo;
        int bestj = blo;
        int bestsize = 0;
        Map<Integer, Integer> j2len = new HashMap<>();
        for (int i = alo; i < ahi; i++) {
            Map<Integer, Integer> newj2len = new HashMap<>();
            List<Integer> js = b2j.get(a.get(i));
            if (js != null) {
                for (int j : js) {
                    if (j < blo) continue;
                    if (j >= bhi) break;
                    int k = j2len.getOrDefault(j - 1, 0) + 1;
                    newj2len.put(j, k);
                    if (k > bestsize) { besti = i - k + 1; bestj = j - k + 1; bestsize = k; }
                }
            }
            j2len = newj2len;
        }
        return new int[] {besti, bestj, bestsize};
    }

    static List<int[]> opcodes(List<String> a, List<String> b) {
        Map<String, List<Integer>> b2j = new HashMap<>();
        for (int j = 0; j < b.size(); j++) b2j.computeIfAbsent(b.get(j), k -> new ArrayList<>()).add(j);
        List<int[]> blocks = new ArrayList<>();
        List<int[]> queue = new ArrayList<>();
        queue.add(new int[] {0, a.size(), 0, b.size()});
        while (!queue.isEmpty()) {
            int[] q = queue.remove(queue.size() - 1);
            int[] m = longest(a, b2j, q[0], q[1], q[2], q[3]);
            int i = m[0];
            int j = m[1];
            int k = m[2];
            if (k > 0) {
                blocks.add(m);
                if (q[0] < i && q[2] < j) queue.add(new int[] {q[0], i, q[2], j});
                if (i + k < q[1] && j + k < q[3]) queue.add(new int[] {i + k, q[1], j + k, q[3]});
            }
        }
        blocks.sort((x, y) -> x[0] != y[0] ? Integer.compare(x[0], y[0]) : Integer.compare(x[1], y[1]));
        List<int[]> merged = new ArrayList<>();
        for (int[] bl : blocks) {
            if (!merged.isEmpty()) {
                int[] last = merged.get(merged.size() - 1);
                if (last[0] + last[2] == bl[0] && last[1] + last[2] == bl[1]) { last[2] += bl[2]; continue; }
            }
            merged.add(new int[] {bl[0], bl[1], bl[2]});
        }
        List<int[]> ops = new ArrayList<>();
        int i = 0;
        int j = 0;
        for (int[] bl : merged) {
            if (i < bl[0] || j < bl[1]) ops.add(new int[] {i, bl[0], j, bl[1]});
            i = bl[0] + bl[2];
            j = bl[1] + bl[2];
        }
        if (i < a.size() || j < b.size()) ops.add(new int[] {i, a.size(), j, b.size()});
        // equal blocks are mapped one to one by the same formula, so they need no separate entry
        for (int[] bl : merged) ops.add(new int[] {bl[0], bl[0] + bl[2], bl[1], bl[1] + bl[2]});
        return ops;
    }

    static Map<Integer, Integer> truth(List<String> draft, List<String> fin) {
        Map<Integer, Integer> m = new HashMap<>();
        for (int[] op : opcodes(draft, fin)) {
            int i1 = op[0], i2 = op[1], j1 = op[2], j2 = op[3];
            for (int k = i1; k < i2; k++) {
                if (j2 > j1) m.put(k, j1 + Math.min(j2 - j1 - 1, (k - i1) * (j2 - j1) / Math.max(1, i2 - i1)));
            }
        }
        return m;
    }

    private record Chapter(String name, List<String> raw, List<String> draft, List<String> fin, List<Anchors.Term> terms) { }

    private static int[] cutScore(Chapter c) {
        LineAligner.Alignment al = LineAligner.align(c.raw(), c.draft(), c.terms());
        Map<Integer, Integer> t = truth(c.draft(), c.fin());
        List<ChunkCutter.Cut> cuts = ChunkCutter.cut(al, c.raw(), c.draft(), new ChunkCutter.Limits(900, Integer.MAX_VALUE, 0, 1.3, CHARS, CHARS));
        int total = 0;
        int bad = 0;
        for (ChunkCutter.Cut cut : cuts) {
            if (cut.endBead() == al.beads().size() - 1) continue; // the last chunk ends the chapter, it is not a cut
            LineAligner.Bead b = al.beads().get(cut.endBead());
            int rc = b.rawEnd();
            int dc = b.draftEnd();
            int maxBefore = -1;
            int minAfter = Integer.MAX_VALUE;
            for (int k = 0; k < dc; k++) if (t.containsKey(k)) maxBefore = Math.max(maxBefore, t.get(k));
            for (int k = dc; k < c.draft().size(); k++) if (t.containsKey(k)) minAfter = Math.min(minAfter, t.get(k));
            total++;
            if (maxBefore >= rc || (minAfter != Integer.MAX_VALUE && minAfter < rc)) bad++;
        }
        return new int[] {total, bad};
    }

    private static ChapterVerdict.Level verdict(List<String> raw, List<String> draft, List<Anchors.Term> terms) {
        LineAligner.Alignment al = LineAligner.align(raw, draft, terms);
        return ChapterVerdict.evaluate(raw, draft, al, terms, ChapterVerdict.DEFAULT_SERIES_RATIO).level();
    }

    private static void tally(Map<String, Map<String, Integer>> out, String cls, ChapterVerdict.Level level) {
        out.computeIfAbsent(cls, k -> new TreeMap<>()).merge(level.name(), 1, Integer::sum);
    }

    @Test public void measureOnTheOwnersLibrary() throws Exception {
        String corpus = System.getenv("CS1_CORPUS");
        Assume.assumeTrue("set CS1_CORPUS to measure CS-1 on the private library", corpus != null && !corpus.isEmpty());
        ln = new File(corpus, "JAKUAKU MONSTER");
        wn = new File(corpus, "JAKUAKU MONSTER WN");
        Map<String, Object> out = new LinkedHashMap<>();

        // ---- cut correctness on the chapters that have an answer
        List<Chapter> answer = new ArrayList<>();
        Map<String, List<EditInputs.GlossaryEntry>> glossaries = new HashMap<>();
        Map<String, List<String>> lnRaw = new HashMap<>();
        Map<String, List<String>> lnDraft = new HashMap<>();
        Map<String, List<String>> wnRaw = new HashMap<>();
        Map<String, List<String>> wnDraft = new HashMap<>();
        Map<String, List<Anchors.Term>> lnTerms = new HashMap<>();
        Map<String, List<Anchors.Term>> wnTerms = new HashMap<>();
        for (int i = 1; i <= 28; i++) {
            String n = String.format("%03d", i);
            List<String> r = lines(read(lnFile("3.RAW", n)));
            List<String> d = lines(read(lnFile("4.DRAFT", n)));
            List<String> f = lines(read(lnFile("5.FINAL", n)));
            List<Anchors.Term> terms = Anchors.terms(glossary(read(lnGlossary(n))));
            lnRaw.put(n, r); lnDraft.put(n, d); lnTerms.put(n, terms);
            if (r.size() != d.size() && f.size() == r.size()) answer.add(new Chapter("LN" + n, r, d, f, terms));
        }
        for (int i = 1; i <= 85; i++) {
            String c = String.format("%03d", i);
            List<String> r = lines(read(wnFile("raw", c)));
            List<String> d = lines(read(wnFile("draft", c)));
            List<String> f = lines(read(wnFile("final", c)));
            List<Anchors.Term> terms = Anchors.terms(glossary(read(wnFile("gl", c))));
            wnRaw.put(c, r); wnDraft.put(c, d); wnTerms.put(c, terms);
            if (f.size() == r.size()) answer.add(new Chapter("WN" + c, r, d, f, terms));
        }
        int cuts = 0;
        int wrong = 0;
        List<String> wrongChapters = new ArrayList<>();
        for (Chapter c : answer) {
            int[] s = cutScore(c);
            cuts += s[0];
            wrong += s[1];
            if (s[1] > 0) wrongChapters.add(c.name());
        }
        Map<String, Object> cutResult = new LinkedHashMap<>();
        cutResult.put("chapters", BigDecimal.valueOf(answer.size()));
        cutResult.put("cuts", BigDecimal.valueOf(cuts));
        cutResult.put("wrong", BigDecimal.valueOf(wrong));
        cutResult.put("chaptersWithAWrongCut", new ArrayList<Object>(wrongChapters));
        out.put("cuts", cutResult);

        // ---- verdict classes, with and without the glossary
        Map<String, Map<String, Integer>> v = new TreeMap<>();
        for (int i = 1; i <= 28; i++) {
            String n = String.format("%03d", i);
            tally(v, "correct+gl", verdict(lnRaw.get(n), lnDraft.get(n), lnTerms.get(n)));
            tally(v, "correct no-gl", verdict(lnRaw.get(n), lnDraft.get(n), List.of()));
            if (i < 28) {
                String next = String.format("%03d", i + 1);
                tally(v, "wrong_chapter+gl", verdict(lnRaw.get(n), lnDraft.get(next), lnTerms.get(n)));
                tally(v, "wrong_chapter no-gl", verdict(lnRaw.get(n), lnDraft.get(next), List.of()));
            }
        }
        long slowest = 0;
        String slowestName = "";
        for (int i = 1; i <= 85; i++) {
            String c = String.format("%03d", i);
            long t0 = System.nanoTime();
            tally(v, "correct+gl", verdict(wnRaw.get(c), wnDraft.get(c), wnTerms.get(c)));
            long ms = (System.nanoTime() - t0) / 1_000_000;
            if (ms > slowest) { slowest = ms; slowestName = "WN" + c + " " + wnRaw.get(c).size() + "x" + wnDraft.get(c).size(); }
            tally(v, "correct no-gl", verdict(wnRaw.get(c), wnDraft.get(c), List.of()));
            if (i < 85 && i % 3 == 0) {
                String next = String.format("%03d", i + 1);
                tally(v, "wrong_chapter+gl", verdict(wnRaw.get(c), wnDraft.get(next), wnTerms.get(c)));
                tally(v, "wrong_chapter no-gl", verdict(wnRaw.get(c), wnDraft.get(next), List.of()));
            }
        }
        for (int i = 1; i <= 11; i++) {
            String n = String.format("%03d", i);
            tally(v, "other_edition+gl", verdict(lnRaw.get(n), wnDraft.get(n), lnTerms.get(n)));
            tally(v, "other_edition no-gl", verdict(lnRaw.get(n), wnDraft.get(n), List.of()));
        }
        Map<String, Object> verdicts = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : v.entrySet()) {
            Map<String, Object> counts = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> x : e.getValue().entrySet()) counts.put(x.getKey(), BigDecimal.valueOf(x.getValue()));
            verdicts.put(e.getKey(), counts);
        }
        out.put("verdicts", verdicts);
        Map<String, Object> timing = new LinkedHashMap<>();
        timing.put("slowestVerdictMs", BigDecimal.valueOf(slowest));
        timing.put("slowestChapter", slowestName);
        out.put("timing", timing);

        String path = System.getenv("CS1_OUT");
        File dest = new File(path == null || path.isEmpty() ? "build/cs1-measure.json" : path);
        if (dest.getParentFile() != null) dest.getParentFile().mkdirs();
        Files.write(dest.toPath(), EditorialCanonicalJson.canonicalize(out).getBytes(StandardCharsets.UTF_8));
        assertTrue(dest.exists());
    }
}
