package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A final read is one capped call that receives the exact bytes the app built (VI_L2 or FINAL), the hash of
 * those bytes and a few app-chosen probe lines. The response must echo the hash and the tail of every probe
 * line; the app checks both, so a receipt can only say "read" when the bytes really were returned to the
 * model. What the model concludes stays a judgement: defects it lists are evidence for the next stage, a
 * clean verdict is never a certificate. Any change to the target afterwards invalidates the read.
 */
public final class EditorialFinalRead {
    public static final String WIRE = "safe4.final-read.wire.v1";
    public static final String L2_PHASE = "L2_FINAL_READ";
    public static final String L3_PHASE = "L3_FINAL_READ";
    public static final String TARGET_ROLE = "READ_TARGET";
    public static final String PROBES_ROLE = "READ_PROBE_LINES";
    public static final int PROBE_COUNT = EditorialFieldSpec.MAX_FINAL_READ_PROBES;
    public static final int TAIL_LENGTH = EditorialFieldSpec.MAX_FINAL_READ_TAIL;
    public static final int MAX_DEFECTS = EditorialFieldSpec.MAX_FINAL_READ_DEFECTS;
    public static final int MAX_WIRE_BYTES = 16_384;
    static final Set<String> TYPES = EditorialL1Ledger.FINDING_TYPES;

    /** A line the model reports as still wrong in the built text. */
    public record Defect(int line, String quote, String type, String note) { }

    /** What the app verified about one read. */
    public record Result(String targetSha256, int lineCount, List<Integer> probeLines, String verdict,
                         List<Defect> defects) {
        public Result {
            probeLines = List.copyOf(probeLines);
            defects = List.copyOf(defects);
        }
    }

    private EditorialFinalRead() { }

    /** Defect types a reader may use (the finding types of the ledger contract). */
    public static Set<String> jsonSchemaTypes() { return TYPES; }

    /** Lines of the built text, split exactly like the reconstruction and the viewer do. */
    public static List<String> lines(byte[] target) {
        String text = strict(target);
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n", -1)) lines.add(line.endsWith("\r") ? line.substring(0, line.length() - 1) : line);
        return lines;
    }

    /**
     * Deterministic probe lines: {@link #PROBE_COUNT} distinct non-empty line numbers derived from the hash of
     * the target, spread over the text. Fewer lines than probes returns all non-empty lines.
     */
    public static List<Integer> probeLines(byte[] target) {
        List<String> lines = lines(target);
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) if (!lines.get(i).isBlank()) candidates.add(i + 1);
        if (candidates.size() <= PROBE_COUNT) return candidates;
        String hash = EditorialCanonicalJson.sha256Hex(target);
        TreeSet<Integer> chosen = new TreeSet<>();
        int seed = 0;
        while (chosen.size() < PROBE_COUNT) {
            // 8 hex digits per probe; skip collisions deterministically
            int offset = (seed * 8) % (hash.length() - 8 + 1);
            long value = Long.parseLong(hash.substring(offset, offset + 8), 16) + seed;
            chosen.add(candidates.get((int) (value % candidates.size())));
            seed++;
        }
        return new ArrayList<>(chosen);
    }

    /** App-owned block carrying the probe lines to the model. */
    public static byte[] probeBlock(byte[] target) {
        List<Object> lines = new ArrayList<>();
        for (Integer line : probeLines(target)) lines.add(BigDecimal.valueOf(line));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("targetSha256", EditorialCanonicalJson.sha256Hex(target));
        root.put("tailLength", BigDecimal.valueOf(TAIL_LENGTH));
        root.put("probeLines", lines);
        return EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
    }

    /** Strict parse of the response against the exact target bytes; IllegalArgumentException with a typed code. */
    public static Result parse(byte[] response, String attemptIdentity, byte[] target) {
        return parse(response, attemptIdentity, target, L2_PHASE);
    }

    public static Result parse(byte[] response, String attemptIdentity, byte[] target, String phase) {
        if (response == null || response.length > MAX_WIRE_BYTES) throw bad("FINAL_READ_WIRE_BYTE_LIMIT_EXCEEDED", "root");
        List<String> targetLines = lines(target);
        String sha = EditorialCanonicalJson.sha256Hex(target);
        Map<String, Object> root = EditorialCanonicalJson.parseObject(response);
        keys(root, Set.of("wireSchemaVersion", "attemptIdentity", "readSha256", "probeTails", "verdict", "defects"), "root");
        if (!WIRE.equals(root.get("wireSchemaVersion"))) throw bad("FINAL_READ_WIRE_SCHEMA_INVALID", "wireSchemaVersion");
        if (!attemptIdentity.equals(root.get("attemptIdentity"))) throw bad("FINAL_READ_ATTEMPT_ECHO_MISMATCH", "attemptIdentity");
        if (!sha.equals(root.get("readSha256"))) throw bad("FINAL_READ_HASH_ECHO_MISMATCH", "readSha256");

        List<Integer> probes = probeLines(target);
        fieldText(phase, "readSha256", root.get("readSha256"));
        Map<Integer, String> tails = new LinkedHashMap<>();
        List<Object> probeRows = EditorialCanonicalJson.array(root.get("probeTails"), "probeTails");
        for (int index = 0; index < probeRows.size(); index++) {
            String path = "probeTails." + index;
            Map<String, Object> row = object(probeRows.get(index), path);
            keys(row, Set.of("line", "tail"), path);
            int line = integer(row.get("line"), path + ".line");
            Object tail = row.get("tail");
            if (!(tail instanceof String) || tails.put(line, (String) tail) != null) throw bad("FINAL_READ_PROBE_INVALID", path + ".tail");
            fieldText(phase, path + ".tail", tail);
        }
        if (!tails.keySet().equals(new HashSet<>(probes))) throw bad("FINAL_READ_PROBE_SET_MISMATCH", "probeTails");
        for (Integer line : probes) {
            String text = targetLines.get(line - 1);
            String expected = text.length() <= TAIL_LENGTH ? text : text.substring(text.length() - TAIL_LENGTH);
            if (!expected.equals(tails.get(line))) throw bad("FINAL_READ_PROBE_TAIL_MISMATCH", "probeTails");
        }

        String verdict = fieldText(phase, "verdict", root.get("verdict"));
        if (!"CLEAN".equals(verdict) && !"DEFECTS".equals(verdict)) throw bad("FINAL_READ_VERDICT_INVALID", "verdict");
        List<Object> rows = EditorialCanonicalJson.array(root.get("defects"), "defects");
        if (rows.size() > MAX_DEFECTS) throw bad("FINAL_READ_DEFECT_LIMIT_EXCEEDED", "defects");
        if (("CLEAN".equals(verdict)) != rows.isEmpty()) throw bad("FINAL_READ_VERDICT_DEFECT_MISMATCH", "defects");
        List<Defect> defects = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            String path = "defects." + index;
            Map<String, Object> row = object(rows.get(index), path);
            keys(row, Set.of("line", "quote", "type", "note"), path);
            int line = integer(row.get("line"), path + ".line");
            if (line < 1 || line > targetLines.size()) throw bad("FINAL_READ_DEFECT_LINE_OUT_OF_RANGE", path + ".line");
            String quote = fieldText(phase, path + ".quote", row.get("quote"));
            String note = fieldText(phase, path + ".note", row.get("note"));
            Object type = row.get("type");
            String typeValue = fieldText(phase, path + ".type", type);
            if (!TYPES.contains(typeValue)) throw bad("FINAL_READ_DEFECT_TYPE_INVALID", path + ".type");
            if (quote.isEmpty() || !EditorialQuoteMatcher.contains(targetLines.get(line - 1), quote)) throw bad("FINAL_READ_DEFECT_QUOTE_NOT_IN_LINE", path + ".quote");
            defects.add(new Defect(line, quote, typeValue, note));
        }
        return new Result(sha, targetLines.size(), probes, verdict, defects);
    }

    /** Canonical evidence block stored with the artifact; round-trips through {@link #parseEvidence}. */
    public static Map<String, Object> evidence(String phase, Result result) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("phase", phase);
        m.put("targetSha256", result.targetSha256());
        m.put("lineCount", BigDecimal.valueOf(result.lineCount()));
        List<Object> probes = new ArrayList<>();
        for (Integer line : result.probeLines()) probes.add(BigDecimal.valueOf(line));
        m.put("probeLines", probes);
        m.put("probesVerified", Boolean.TRUE);
        m.put("verdict", result.verdict());
        List<Object> defects = new ArrayList<>();
        for (Defect defect : result.defects()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("line", BigDecimal.valueOf(defect.line()));
            d.put("quote", defect.quote());
            d.put("type", defect.type());
            d.put("note", defect.note());
            defects.add(d);
        }
        m.put("defects", defects);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static Result parseEvidence(Map<String, Object> block) {
        List<Integer> probes = new ArrayList<>();
        for (Object o : (List<Object>) block.get("probeLines")) probes.add(((BigDecimal) o).intValueExact());
        List<Defect> defects = new ArrayList<>();
        for (Object o : (List<Object>) block.get("defects")) {
            Map<String, Object> d = (Map<String, Object>) o;
            defects.add(new Defect(((BigDecimal) d.get("line")).intValueExact(), (String) d.get("quote"),
                    (String) d.get("type"), (String) d.get("note")));
        }
        return new Result((String) block.get("targetSha256"), ((BigDecimal) block.get("lineCount")).intValueExact(),
                probes, (String) block.get("verdict"), defects);
    }

    private static String strict(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            return !text.isEmpty() && text.charAt(0) == '﻿' ? text.substring(1) : text;
        } catch (CharacterCodingException invalid) {
            throw bad("FINAL_READ_TARGET_NOT_UTF8");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map<?, ?>)) throw bad("FINAL_READ_OBJECT_EXPECTED", path);
        return (Map<String, Object>) value;
    }

    private static void keys(Map<String, Object> value, Set<String> allowed, String path) {
        if (!value.keySet().equals(allowed)) throw bad("FINAL_READ_KEYS_INVALID", path);
    }

    private static int integer(Object value, String path) {
        if (!(value instanceof BigDecimal)) throw bad("FINAL_READ_INT_INVALID", path);
        try {
            return ((BigDecimal) value).intValueExact();
        } catch (ArithmeticException invalid) {
            throw bad("FINAL_READ_INT_INVALID", path);
        }
    }

    private static String fieldText(String phase, String path, Object value) {
        String text = EditorialFieldSpec.validateString(phase, path, value, "FINAL_READ_TEXT_INVALID",
                "FINAL_READ_TEXT_REQUIRED", "FINAL_READ_TEXT_INVALID", "FINAL_READ_TEXT_INVALID");
        for (char c : text.toCharArray()) if (Character.isISOControl(c)) throw bad("FINAL_READ_TEXT_CONTROL_CHARACTER", path);
        return text;
    }

    private static IllegalArgumentException bad(String code) { return bad(code, "root"); }
    private static IllegalArgumentException bad(String code, String path) { return WireViolation.at(code, path); }

    /** Strict response schema for the provider; the parser above stays the authority. */
    public static Map<String, Object> jsonSchema() {
        String phase = EditorialFieldSpec.L2_FINAL_READ;
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("wireSchemaVersion", EditorialFieldSpec.schema(phase, "wireSchemaVersion"));
        props.put("attemptIdentity", EditorialFieldSpec.schema(phase, "attemptIdentity"));
        props.put("readSha256", EditorialFieldSpec.schema(phase, "readSha256"));
        props.put("probeTails", fieldArray(phase, "probeTails", object(Map.of(
                "line", Map.of("type", "integer"),
                "tail", EditorialFieldSpec.schema(phase, "probeTails[].tail")))));
        props.put("verdict", EditorialFieldSpec.schema(phase, "verdict"));
        props.put("defects", fieldArray(phase, "defects", object(Map.of(
                "line", Map.of("type", "integer"),
                "quote", EditorialFieldSpec.schema(phase, "defects[].quote"),
                "type", EditorialFieldSpec.schema(phase, "defects[].type"),
                "note", EditorialFieldSpec.schema(phase, "defects[].note")))));
        return object(props);
    }

    private static Map<String, Object> fieldArray(String phase, String path, Map<String, Object> itemSchema) {
        EditorialFieldSpec.Field spec = EditorialFieldSpec.find(phase, path);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "array");
        if (spec.minItems() > 0) result.put("minItems", BigDecimal.valueOf(spec.minItems()));
        result.put("maxItems", BigDecimal.valueOf(spec.maxItems()));
        result.put("items", itemSchema);
        return result;
    }

    private static Map<String, Object> object(Map<String, ?> props) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "object");
        m.put("additionalProperties", Boolean.FALSE);
        m.put("required", new ArrayList<Object>(new TreeSet<>(props.keySet())));
        m.put("properties", new LinkedHashMap<String, Object>(props));
        return m;
    }
}
