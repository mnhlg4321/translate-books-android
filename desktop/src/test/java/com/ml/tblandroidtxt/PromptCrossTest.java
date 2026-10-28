package com.ml.tblandroidtxt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * "The PC and the phone send the same prompt": the P0a fixtures are read through the PC document port and pushed through
 * AppValidator, Chunker, PromptPlan and buildChatRequestBody on the PC classpath (org.json library); everything is compared
 * with the golden files of balanced_default. Prompts and chunks.json byte for byte, the request after JSON normalisation.
 */
public class PromptCrossTest {
    private static final Path FIX = Paths.get("../app/src/test/resources/fixtures/golden-prompt");

    private static String read(PathDocumentIO io, String name) throws Exception { return io.readText(PathDocumentIO.refOf(FIX.resolve(name))); }

    @Test public void balancedDefaultMatchesTheGoldenFilesThroughThePcPath() throws Exception {
        PathDocumentIO io = new PathDocumentIO(null);
        JSONObject spec = null;
        JSONArray cases = new JSONObject(read(io, "cases.json")).getJSONArray("cases");
        for (int i = 0; i < cases.length(); i++) if ("balanced_default".equals(cases.getJSONObject(i).getString("name"))) spec = cases.getJSONObject(i);
        JSONObject c = spec.getJSONObject("settings");
        AppSettings s = new AppSettings();
        s.provider = c.getString("provider"); s.model = c.getString("model");
        s.sourceLanguage = c.getString("sourceLanguage"); s.targetLanguage = c.getString("targetLanguage");
        s.reasoningEffort = c.getString("reasoning"); s.temperature = (float) c.getDouble("temperature");
        s.optimizationPreset = c.getString("optimizationPreset"); s.chunkMode = c.getString("chunkMode");
        s.maxTokensPerChunk = c.getInt("maxTokensPerChunk"); s.maxCharsPerChunk = c.getInt("maxCharsPerChunk");
        s.softLimitRatio = (float) c.getDouble("softLimitRatio"); s.maxOutputTokens = c.getInt("maxOutputTokens");
        s.contextChars = c.getInt("contextChars"); s.contextOverlapEnabled = c.getBoolean("contextOverlapEnabled");
        s.glossaryInjectLimit = c.getInt("glossaryInjectLimit"); s.pronounInjectLimit = c.getInt("pronounInjectLimit");
        s.refineAfter = c.getBoolean("refineAfter");
        s.glossaryText = read(io, spec.getString("glossaryFile"));
        s.pronounText = read(io, spec.getString("pronounFile"));
        CustomInstructions ci = YamlInstructionParser.parse("instruction.yaml", read(io, "instruction.yaml"));
        if (ci.hasTranslation()) s.translationInstructions = ci.translation;
        if (ci.hasRefinement()) s.refinementInstructions = ci.refinement;
        String previous = read(io, "prev_translation.txt");

        s = AppValidator.normalize(s);
        List<Chunk> chunks = Chunker.chunkText(read(io, "raw_ja.txt"), s);
        Path expected = FIX.resolve("expected/balanced_default");
        assertEquals(new String(Files.readAllBytes(expected.resolve("chunks.json")), StandardCharsets.UTF_8), chunksJson(chunks));
        assertEquals(3, chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            PromptPlan plan = PromptPlan.forTranslation(chunks.get(i), i == 0 ? "" : previous, s);
            JSONObject body = OpenAICompatibleClient.buildChatRequestBody(s, plan.prompt, plan.requestMaxOutputTokens(s), null, false,
                    ReasoningEffort.forTranslation(s));
            assertArrayEquals("system " + i, Files.readAllBytes(expected.resolve("chunk-" + i + ".system.txt")), plan.prompt.system.getBytes(StandardCharsets.UTF_8));
            assertArrayEquals("user " + i, Files.readAllBytes(expected.resolve("chunk-" + i + ".user.txt")), plan.prompt.user.getBytes(StandardCharsets.UTF_8));
            String want = canonical(new JSONObject(new String(Files.readAllBytes(expected.resolve("chunk-" + i + ".request.json")), StandardCharsets.UTF_8)));
            assertEquals("request " + i, want, canonical(body));
            Object temperature = body.get("temperature");
            assertTrue("temperature is numeric on the PC library: " + temperature.getClass(), temperature instanceof Number);
            assertEquals(0.3d, ((Number) temperature).doubleValue(), 1e-6);
            assertTrue(canonical(body).contains("\"temperature\": \"0.300000\""));
        }
    }

    // ---- same canonical form as the golden writer: sorted keys, 2-space indent, floating point as 6-decimal strings ----

    private static String chunksJson(List<Chunk> chunks) throws Exception {
        JSONArray list = new JSONArray();
        for (Chunk k : chunks) {
            list.put(new JSONObject().put("index", k.index).put("startOffset", k.startOffset).put("endOffset", k.endOffset)
                    .put("paragraphStart", k.paragraphStart).put("paragraphEnd", k.paragraphEnd)
                    .put("main", lenHash(k.mainContent)).put("contextBefore", lenHash(k.contextBefore)).put("contextAfter", lenHash(k.contextAfter)));
        }
        return canonical(new JSONObject().put("chunkCount", chunks.size()).put("chunks", list));
    }

    private static JSONObject lenHash(String text) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return new JSONObject().put("length", text.length()).put("sha256", sb.toString());
    }

    private static String canonical(Object v) {
        StringBuilder sb = new StringBuilder();
        write(sb, v, 0);
        return sb.append('\n').toString();
    }

    private static void write(StringBuilder sb, Object v, int depth) {
        if (v == null || v == JSONObject.NULL) { sb.append("null"); return; }
        if (v instanceof JSONObject) {
            JSONObject o = (JSONObject) v;
            TreeSet<String> keys = new TreeSet<>(o.keySet());
            if (keys.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            int n = 0;
            for (String k : keys) {
                indent(sb, depth + 1); quote(sb, k); sb.append(": "); write(sb, o.get(k), depth + 1);
                sb.append(++n < keys.size() ? ",\n" : "\n");
            }
            indent(sb, depth); sb.append('}');
        } else if (v instanceof JSONArray) {
            JSONArray a = (JSONArray) v;
            if (a.length() == 0) { sb.append("[]"); return; }
            sb.append("[\n");
            for (int i = 0; i < a.length(); i++) {
                indent(sb, depth + 1); write(sb, a.get(i), depth + 1); sb.append(i + 1 < a.length() ? ",\n" : "\n");
            }
            indent(sb, depth); sb.append(']');
        } else if (v instanceof Float || v instanceof Double || v instanceof java.math.BigDecimal) {
            quote(sb, String.format(Locale.ROOT, "%.6f", ((Number) v).doubleValue()));
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else {
            quote(sb, String.valueOf(v));
        }
    }

    private static void indent(StringBuilder sb, int depth) { for (int i = 0; i < depth; i++) sb.append("  "); }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) sb.append(String.format(Locale.ROOT, "\\u%04x", (int) ch)); else sb.append(ch);
            }
        }
        sb.append('"');
    }
}
