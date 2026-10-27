package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Byte-exact snapshot of what the translation path produces for fixed, invented inputs: chunks, prompts,
 * request body, plan token counts and cost estimates. Later refactors (shared core extraction) must reproduce
 * these files. Read-only by default; the golden files are rewritten only when Gradle is started with
 * {@code -Dgolden.update=true} (forwarded to the test JVM by app/build.gradle).
 */
public class GoldenPromptSnapshotTest {
    private static final String RES = "fixtures/golden-prompt/";
    private static final String SRC_DIR = "src/test/resources/fixtures/golden-prompt";
    private static final boolean UPDATE = "true".equals(System.getProperty("golden.update", "false"));
    private static final List<String> EXPECTED_CASES = Arrays.asList("balanced_default", "no_overlap", "economy_char",
            "quality", "full_no_context", "reasoning_high", "reasoning_off", "tight_inject_limits",
            "no_glossary_no_pronoun", "legacy_pronoun", "refine_on_estimate", "catalog_known");

    private ModelCatalog.CatalogState savedCatalog;
    private boolean catalogSwapped;

    /** ModelCatalog is process-wide static state; start from the empty (bundled fallback) catalog and put it back after. */
    @Before public void isolateCatalog() {
        savedCatalog = ModelCatalog.state();
        if (!savedCatalog.models.isEmpty()) {
            ModelCatalog.installForTests(Collections.emptyList(), ModelCatalog.LoadingState.IDLE);
            catalogSwapped = true;
        }
    }

    @After public void restoreCatalog() {
        if (catalogSwapped) ModelCatalog.installForTests(savedCatalog.models, savedCatalog.loadingState);
        catalogSwapped = false;
    }

    @Test public void balancedDefault() throws Exception { runCase("balanced_default"); }
    @Test public void noOverlap() throws Exception { runCase("no_overlap"); }
    @Test public void economyChar() throws Exception { runCase("economy_char"); }
    @Test public void quality() throws Exception { runCase("quality"); }
    @Test public void fullNoContext() throws Exception { runCase("full_no_context"); }
    @Test public void reasoningHigh() throws Exception { runCase("reasoning_high"); }
    @Test public void reasoningOff() throws Exception { runCase("reasoning_off"); }
    @Test public void tightInjectLimits() throws Exception { runCase("tight_inject_limits"); }
    @Test public void noGlossaryNoPronoun() throws Exception { runCase("no_glossary_no_pronoun"); }
    @Test public void legacyPronoun() throws Exception { runCase("legacy_pronoun"); }
    @Test public void refineOnEstimate() throws Exception { runCase("refine_on_estimate"); }
    @Test public void catalogKnown() throws Exception { runCase("catalog_known"); }

    @Test public void casesFileMatchesTestMethods() throws Exception {
        JSONArray cases = new JSONObject(readResource("cases.json")).getJSONArray("cases");
        List<String> names = new ArrayList<>();
        for (int i = 0; i < cases.length(); i++) names.add(cases.getJSONObject(i).getString("name"));
        assertEquals("cases.json and the test methods must list the same cases", EXPECTED_CASES, names);
    }

    @Test public void goldenFilesContainNoSecrets() throws Exception {
        Path root = resourceDir();
        Pattern sk = Pattern.compile("\\bsk-[A-Za-z0-9]{8,}");
        Pattern bearer = Pattern.compile("\\bBearer\\s+\\S+");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) { files = walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList()); }
        assertFalse("no golden files found under " + root, files.isEmpty());
        for (Path file : files) {
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            String rel = root.relativize(file).toString().replace('\\', '/');
            assertFalse(rel + " contains the test key marker", text.contains("TEST_KEY_NOT_SECRET"));
            assertFalse(rel + " matches sk- key pattern", sk.matcher(text).find());
            assertFalse(rel + " matches Bearer token pattern", bearer.matcher(text).find());
        }
    }

    // ---- case runner -------------------------------------------------------------------------------------------

    private void runCase(String name) throws Exception {
        JSONObject spec = findCase(name);
        AppSettings settings = settingsFrom(spec);
        String raw = readResource("raw_ja.txt");
        String previous = readResource("prev_translation.txt");

        // Same order as TranslationEngine: normalize, chunk, plan per chunk, request body.
        AppSettings s = AppValidator.normalize(settings);
        String neutralChunks = chunksJson(Chunker.chunkText(raw, s));
        if (spec.has("catalog")) {
            JSONObject cat = spec.getJSONObject("catalog");
            ModelCatalog.installForTests(Collections.singletonList(new ModelCatalog.ModelInfo(s.model,
                    cat.getDouble("inputPerMillion"), cat.getDouble("outputPerMillion"),
                    cat.getDouble("cachedInputPerMillion"), cat.getInt("contextLength"), s.provider,
                    cat.getString("pricingSource"))), ModelCatalog.LoadingState.READY);
            catalogSwapped = true; // restored in @After
        } else {
            assertFalse("test catalog must be the neutral one", ModelCatalog.existsInActiveCatalog(s.model));
        }
        List<Chunk> chunks = Chunker.chunkText(raw, s);
        assertTrue(name + ": fixture must split into at least 3 chunks, got " + chunks.size(), chunks.size() >= 3);

        TreeMap<String, byte[]> actual = new TreeMap<>();
        String chunkJson = chunksJson(chunks);
        if (spec.has("catalog")) {
            assertFalse(name + ": the catalog must change the chunking, otherwise the case proves nothing",
                    neutralChunks.equals(chunkJson));
        }
        actual.put("chunks.json", utf8(chunkJson));
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            PromptPlan plan = PromptPlan.forTranslation(chunk, i == 0 ? "" : previous, s);
            int requestCap = plan.requestMaxOutputTokens(s);
            String effort = ReasoningEffort.forTranslation(s);
            JSONObject body = OpenAICompatibleClient.buildChatRequestBody(s, plan.prompt, requestCap, null, false, effort);
            actual.put("chunk-" + i + ".system.txt", utf8(plan.prompt.system));
            actual.put("chunk-" + i + ".user.txt", utf8(plan.prompt.user));
            actual.put("chunk-" + i + ".plan.json", utf8(canonical(planJson(plan, s, requestCap, effort))));
            actual.put("chunk-" + i + ".request.json", utf8(canonical(body)));
        }
        actual.put("estimate.json", utf8(canonical(estimateJson(CostEstimator.estimatePreparedChunks(chunks, s)))));
        actual.put("estimate-lightweight.json",
                utf8(canonical(estimateJson(CostEstimator.estimatePreparedChunksLightweight(chunks, s)))));

        if (UPDATE) { writeGolden(name, actual); return; }
        compareGolden(name, actual);
    }

    private static AppSettings settingsFrom(JSONObject spec) throws Exception {
        JSONObject c = spec.getJSONObject("settings");
        AppSettings s = new AppSettings();
        // getString/getInt/... throw when a field is missing, so a case can never lean on AppSettings defaults.
        s.provider = c.getString("provider");
        s.model = c.getString("model");
        s.sourceLanguage = c.getString("sourceLanguage");
        s.targetLanguage = c.getString("targetLanguage");
        s.reasoningEffort = c.getString("reasoning");
        s.temperature = (float) c.getDouble("temperature");
        s.optimizationPreset = c.getString("optimizationPreset");
        s.chunkMode = c.getString("chunkMode");
        s.maxTokensPerChunk = c.getInt("maxTokensPerChunk");
        s.maxCharsPerChunk = c.getInt("maxCharsPerChunk");
        s.softLimitRatio = (float) c.getDouble("softLimitRatio");
        s.maxOutputTokens = c.getInt("maxOutputTokens");
        s.contextChars = c.getInt("contextChars");
        s.contextOverlapEnabled = c.getBoolean("contextOverlapEnabled");
        s.glossaryInjectLimit = c.getInt("glossaryInjectLimit");
        s.pronounInjectLimit = c.getInt("pronounInjectLimit");
        s.refineAfter = c.getBoolean("refineAfter");
        s.glossaryText = spec.isNull("glossaryFile") ? "" : readResource(spec.getString("glossaryFile"));
        s.pronounText = spec.isNull("pronounFile") ? "" : readResource(spec.getString("pronounFile"));
        // Same parser call as TranslationEngine.loadExternalConfig.
        CustomInstructions ci = YamlInstructionParser.parse("instruction.yaml", readResource("instruction.yaml"));
        if (ci.hasTranslation()) s.translationInstructions = ci.translation;
        if (ci.hasRefinement()) s.refinementInstructions = ci.refinement;
        return s;
    }

    private static JSONObject findCase(String name) throws Exception {
        JSONArray cases = new JSONObject(readResource("cases.json")).getJSONArray("cases");
        for (int i = 0; i < cases.length(); i++) {
            if (name.equals(cases.getJSONObject(i).getString("name"))) return cases.getJSONObject(i);
        }
        throw new AssertionError("case not in cases.json: " + name);
    }

    // ---- golden content ----------------------------------------------------------------------------------------

    private static String chunksJson(List<Chunk> chunks) throws Exception {
        JSONArray list = new JSONArray();
        for (Chunk c : chunks) {
            list.put(new JSONObject()
                    .put("index", c.index)
                    .put("startOffset", c.startOffset).put("endOffset", c.endOffset)
                    .put("paragraphStart", c.paragraphStart).put("paragraphEnd", c.paragraphEnd)
                    .put("main", lenHash(c.mainContent))
                    .put("contextBefore", lenHash(c.contextBefore))
                    .put("contextAfter", lenHash(c.contextAfter)));
        }
        return canonical(new JSONObject().put("chunkCount", chunks.size()).put("chunks", list));
    }

    /** length is in UTF-16 code units (Java String.length); sha256 is over the UTF-8 bytes. */
    private static JSONObject lenHash(String text) throws Exception {
        String t = text == null ? "" : text;
        return new JSONObject().put("length", t.length()).put("sha256", sha256(utf8(t)));
    }

    private static JSONObject planJson(PromptPlan p, AppSettings s, int requestCap, String effort) throws Exception {
        return new JSONObject()
                .put("rawTokens", p.rawTokens).put("systemTokens", p.systemTokens)
                .put("instructionTokens", p.instructionTokens).put("glossaryTokens", p.glossaryTokens)
                .put("pronounTokens", p.pronounTokens).put("historyTokens", p.historyTokens)
                .put("contextTokens", p.contextTokens).put("totalInputTokens", p.totalInputTokens)
                .put("glossaryLockCount", p.glossaryLockCount).put("pronounLockCount", p.pronounLockCount)
                .put("visibleMaxOutputTokens", p.visibleMaxOutputTokens(s))
                .put("requestMaxOutputTokens", requestCap)
                .put("reasoningEffortSent", effort);
    }

    private static JSONObject estimateJson(CostEstimator.Estimate e) throws Exception {
        return new JSONObject()
                .put("sourceTokens", e.sourceTokens).put("chunks", e.chunks)
                .put("inputTokensLow", e.inputTokensLow).put("inputTokensHigh", e.inputTokensHigh)
                .put("outputTokensLow", e.outputTokensLow).put("outputTokensHigh", e.outputTokensHigh)
                .put("totalTokensLow", e.totalTokensLow).put("totalTokensHigh", e.totalTokensHigh)
                .put("breakdownSource", e.breakdownSource).put("breakdownInstruction", e.breakdownInstruction)
                .put("breakdownGlossary", e.breakdownGlossary).put("breakdownPronoun", e.breakdownPronoun)
                .put("breakdownContext", e.breakdownContext)
                .put("expectedCachedInputTokens", e.expectedCachedInputTokens)
                .put("expectedUncachedInputTokens", e.expectedUncachedInputTokens)
                .put("costLow", d(e.costLow)).put("costHigh", d(e.costHigh))
                .put("noCacheCostLow", d(e.noCacheCostLow)).put("noCacheCostHigh", d(e.noCacheCostHigh))
                .put("pricingAvailable", e.pricingAvailable).put("cachePricingAvailable", e.cachePricingAvailable)
                .put("genericTokenizer", e.genericTokenizer)
                .put("tokenizerState", e.tokenizerState).put("pricingSource", e.pricingSource);
    }

    /** Doubles go in as 6-decimal strings; also keeps NaN/Infinity (unknown pricing) representable. */
    private static String d(double v) { return String.format(Locale.ROOT, "%.6f", v); }

    // ---- canonical JSON: sorted keys, 2-space indent, every floating-point value as a 6-decimal string ----------

    static String canonical(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, 0);
        return sb.append('\n').toString();
    }

    private static void writeValue(StringBuilder sb, Object v, int depth) {
        if (v == null || v == JSONObject.NULL) { sb.append("null"); return; }
        if (v instanceof JSONObject) {
            JSONObject o = (JSONObject) v;
            TreeSet<String> keys = new TreeSet<>();
            for (java.util.Iterator<String> it = o.keys(); it.hasNext(); ) keys.add(it.next());
            if (keys.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            int n = 0;
            for (String k : keys) {
                indent(sb, depth + 1);
                quote(sb, k);
                sb.append(": ");
                writeValue(sb, o.opt(k), depth + 1);
                sb.append(++n < keys.size() ? ",\n" : "\n");
            }
            indent(sb, depth);
            sb.append('}');
        } else if (v instanceof JSONArray) {
            JSONArray a = (JSONArray) v;
            if (a.length() == 0) { sb.append("[]"); return; }
            sb.append("[\n");
            for (int i = 0; i < a.length(); i++) {
                indent(sb, depth + 1);
                writeValue(sb, a.opt(i), depth + 1);
                sb.append(i + 1 < a.length() ? ",\n" : "\n");
            }
            indent(sb, depth);
            sb.append(']');
        } else if (v instanceof Float || v instanceof Double || v instanceof java.math.BigDecimal) {
            quote(sb, String.format(Locale.ROOT, "%.6f", ((Number) v).doubleValue()));
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else {
            quote(sb, String.valueOf(v));
        }
    }

    private static void indent(StringBuilder sb, int depth) { for (int i = 0; i < depth; i++) sb.append("  "); }

    /** Own escaper so output never depends on the org.json flavour: only quote, backslash and C0 controls are escaped. */
    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    // ---- compare / update --------------------------------------------------------------------------------------

    private void compareGolden(String name, TreeMap<String, byte[]> actual) throws Exception {
        Path dir = resourceDir().resolve("expected").resolve(name);
        List<String> problems = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            fail(name + ": golden directory missing (" + dir + "); run with -Dgolden.update=true to create it");
        }
        TreeSet<String> onDisk = new TreeSet<>();
        try (Stream<Path> list = Files.list(dir)) { list.forEach(p -> onDisk.add(p.getFileName().toString())); }
        for (String missing : diff(actual.keySet(), onDisk)) problems.add(name + ": golden file missing: " + missing);
        for (String extra : diff(onDisk, actual.keySet())) problems.add(name + ": unexpected golden file: " + extra);
        for (String file : actual.keySet()) {
            if (!onDisk.contains(file)) continue;
            byte[] want = Files.readAllBytes(dir.resolve(file));
            byte[] got = actual.get(file);
            if (Arrays.equals(want, got)) continue;
            String wantText = new String(want, StandardCharsets.UTF_8);
            String gotText = new String(got, StandardCharsets.UTF_8);
            boolean byteExact = file.endsWith(".txt") || file.equals("chunks.json");
            if (!byteExact) {
                // Other JSON files compare by content after canonicalisation, so whitespace-only edits are not a diff.
                try {
                    String normalized = canonical(new JSONObject(wantText));
                    if (normalized.equals(gotText)) continue;
                    wantText = normalized;
                } catch (Exception notJson) { /* fall through to the text diff */ }
            }
            problems.add(name + " / " + file + ": " + firstDifference(wantText, gotText));
        }
        if (!problems.isEmpty()) fail(String.join("\n", problems));
    }

    private static TreeSet<String> diff(java.util.Collection<String> a, java.util.Collection<String> b) {
        TreeSet<String> out = new TreeSet<>(a);
        out.removeAll(b);
        return out;
    }

    /** Line/column (1-based, in chars) of the first difference with at most 80 chars of context; never the whole text. */
    static String firstDifference(String want, String got) {
        int n = Math.min(want.length(), got.length());
        int at = 0;
        while (at < n && want.charAt(at) == got.charAt(at)) at++;
        int line = 1, col = 1;
        for (int i = 0; i < at; i++) {
            if (want.charAt(i) == '\n') { line++; col = 1; } else col++;
        }
        return "first difference at line " + line + ", column " + col + " (char " + at + "; expected length "
                + want.length() + ", actual length " + got.length() + ")\n  expected: " + excerpt(want, at)
                + "\n  actual:   " + excerpt(got, at);
    }

    private static String excerpt(String text, int at) {
        int from = Math.max(0, at - 40), to = Math.min(text.length(), at + 40);
        if (from > 0 && Character.isLowSurrogate(text.charAt(from))) from--;
        if (to < text.length() && to > from && Character.isHighSurrogate(text.charAt(to - 1))) to++;
        return "[" + text.substring(from, Math.min(to, text.length())).replace("\n", "\\n").replace("\r", "\\r") + "]";
    }

    private void writeGolden(String name, TreeMap<String, byte[]> actual) throws IOException {
        Path root = Paths.get(SRC_DIR);
        assertTrue("golden.update needs the app module as working directory: " + root.toAbsolutePath(),
                Files.isDirectory(root));
        Path dir = root.resolve("expected").resolve(name);
        if (Files.isDirectory(dir)) {
            try (Stream<Path> list = Files.list(dir)) {
                for (Path p : list.collect(Collectors.toList())) Files.delete(p);
            }
        }
        Files.createDirectories(dir);
        for (java.util.Map.Entry<String, byte[]> e : actual.entrySet()) Files.write(dir.resolve(e.getKey()), e.getValue());
    }

    // ---- io helpers ---------------------------------------------------------------------------------------------

    private static Path resourceDir() throws Exception {
        URL url = GoldenPromptSnapshotTest.class.getClassLoader().getResource(RES + "cases.json");
        assertNotNull("fixture resources missing from the test classpath", url);
        return Paths.get(url.toURI()).getParent();
    }

    private static String readResource(String file) throws Exception {
        return new String(Files.readAllBytes(resourceDir().resolve(file)), StandardCharsets.UTF_8);
    }

    private static byte[] utf8(String text) { return text.getBytes(StandardCharsets.UTF_8); }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return sb.toString();
    }
}
