package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan;
import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlanner;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * Opt-in (CS1_CORPUS names the folder that holds the two libraries): plans every chapter of the owner's library with the app's
 * default Performance settings and the app's own Chunker, and writes only counts (chunks, verdict, milliseconds) to
 * CS1_PERF_OUT. Driven by {@code scripts/chunk/measure_cs1.py --performance}.
 */
public final class Cs1PerformanceCountTest {
    private static String decode(byte[] b) {
        if (b.length >= 2 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xfe) return new String(b, 2, b.length - 2, StandardCharsets.UTF_16LE);
        if (b.length >= 2 && (b[0] & 0xff) == 0xfe && (b[1] & 0xff) == 0xff) return new String(b, 2, b.length - 2, StandardCharsets.UTF_16BE);
        if (b.length >= 3 && (b[0] & 0xff) == 0xef && (b[1] & 0xff) == 0xbb && (b[2] & 0xff) == 0xbf) return new String(b, 3, b.length - 3, StandardCharsets.UTF_8);
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(b)).toString();
        } catch (java.nio.charset.CharacterCodingException e) {
            return new String(b, Charset.forName("windows-31j"));
        }
    }

    private static File first(File dir, java.util.function.Predicate<String> match) {
        String[] names = dir.list();
        if (names == null) return null;
        Arrays.sort(names);
        for (String n : names) if (match.test(n)) return new File(dir, n);
        return null;
    }

    private static List<EditInputs.GlossaryEntry> glossary(String csv) {
        List<EditInputs.GlossaryEntry> out = new ArrayList<>();
        List<String[]> rows = new ArrayList<>();
        for (String line : csv.replace("\r\n", "\n").split("\n")) if (!line.isBlank()) rows.add(line.split(",", -1));
        if (rows.isEmpty()) return out;
        List<String> header = Arrays.asList(rows.get(0));
        int s = header.indexOf("source");
        int t = header.indexOf("target");
        for (int i = 1; i < rows.size(); i++) {
            String[] r = rows.get(i);
            if (s >= 0 && t >= 0 && s < r.length && t < r.length) out.add(new EditInputs.GlossaryEntry(r[s], r[t], "", ""));
        }
        return out;
    }

    @Test public void countChunksWithTheDefaultPerformanceSettings() throws Exception {
        String corpus = System.getenv("CS1_CORPUS");
        Assume.assumeTrue("set CS1_CORPUS to count chunks on the private library", corpus != null && !corpus.isEmpty());
        AppSettings settings = new AppSettings();
        ChunkPlanner.Settings planner = EditorialPairSourceLoader.plannerSettings(settings);
        File ln = new File(corpus, "JAKUAKU MONSTER");
        File wn = new File(corpus, "JAKUAKU MONSTER WN");
        Map<String, Object> chapters = new LinkedHashMap<>();
        long slowest = 0;
        String slowestName = "";
        for (int i = 1; i <= 28; i++) {
            String n = String.format("%03d", i);
            File raw = first(new File(ln, "3.RAW"), x -> x.startsWith(n + "_") && x.endsWith(".txt"));
            File draft = first(new File(ln, "4.DRAFT"), x -> x.startsWith(n + "_") && x.endsWith(".txt"));
            File gl = first(new File(ln, "1.GLOSSARIES/JAKUAKU_MONSTER_VOL1_GLOSSARY_FINAL_v1.0.0/chapter_glossaries"), x -> x.startsWith(n + "_"));
            long[] timing = new long[1];
            chapters.put("LN" + n, plan(raw, draft, gl, planner, timing));
            if (timing[0] > slowest) { slowest = timing[0]; slowestName = "LN" + n; }
        }
        for (int i = 1; i <= 85; i++) {
            String c = String.format("%03d", i);
            Pattern p = Pattern.compile("^\\d" + c + "-.*");
            File raw = first(new File(wn, "3. RAW_JAKUAKU MONSTER WEBNOVEL"), x -> p.matcher(x).matches() || x.startsWith(c + "_"));
            File draft = first(new File(wn, "4. DRAFT"), x -> p.matcher(x).matches() || x.startsWith(c + "_"));
            File gl = first(new File(wn, "1. JAKUAKU_MONSTER_VOL1_GLOSSARY_FINAL_v2.0.1"), x -> x.startsWith(c + "_"));
            long[] timing = new long[1];
            chapters.put("WN" + c, plan(raw, draft, gl, planner, timing));
            if (timing[0] > slowest) { slowest = timing[0]; slowestName = "WN" + c; }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> used = new LinkedHashMap<>();
        used.put("mode", planner.mode());
        used.put("soft", BigDecimal.valueOf(planner.soft()));
        used.put("hard", BigDecimal.valueOf(planner.hard()));
        used.put("maxOutput", BigDecimal.valueOf(planner.maxOutputTokens()));
        used.put("contextChars", BigDecimal.valueOf(planner.contextChars()));
        used.put("preset", settings.optimizationPreset == null ? "" : settings.optimizationPreset);
        out.put("settings", used);
        out.put("chapters", chapters);
        out.put("slowestMs", BigDecimal.valueOf(slowest));
        out.put("slowestChapter", slowestName);
        String path = System.getenv("CS1_PERF_OUT");
        File dest = new File(path == null || path.isEmpty() ? "build/cs1-performance.json" : path);
        if (dest.getParentFile() != null) dest.getParentFile().mkdirs();
        Files.write(dest.toPath(), EditorialCanonicalJson.canonicalize(out).getBytes(StandardCharsets.UTF_8));
        assertTrue(dest.exists());
    }

    private static Map<String, Object> plan(File raw, File draft, File gl, ChunkPlanner.Settings planner, long[] timing) throws Exception {
        String r = decode(Files.readAllBytes(raw.toPath()));
        String d = decode(Files.readAllBytes(draft.toPath()));
        String g = gl == null ? "" : decode(Files.readAllBytes(gl.toPath()));
        long t0 = System.nanoTime();
        ChunkPlanner.Planned planned = ChunkPlanner.plan(r, d, glossary(g), g, "", planner);
        timing[0] = (System.nanoTime() - t0) / 1_000_000L;
        ChunkPlan plan = planned.plan();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("chunks", BigDecimal.valueOf(plan.chunks.size()));
        row.put("verdict", plan.verdict);
        row.put("uncertain", BigDecimal.valueOf(plan.uncertainChunks));
        row.put("rawUnits", BigDecimal.valueOf(plan.rawUnits));
        row.put("ms", BigDecimal.valueOf(timing[0]));
        return row;
    }
}
