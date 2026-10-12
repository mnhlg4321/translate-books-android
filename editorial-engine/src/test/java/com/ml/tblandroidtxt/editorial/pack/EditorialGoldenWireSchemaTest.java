package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Golden valid wires must pass the schema the provider sends AND the production parser. A fake run cannot
 * show a schema/parser disagreement (the fake never sees the schema), which is how a {@code minLength} of 3
 * on the unit reference {@code "L1"} survived to a live call. A small JSON-schema checker (type, required,
 * additionalProperties, min/max length and items, pattern, enum) stands in for the provider's decoder.
 * Synthetic text only.
 */
public final class EditorialGoldenWireSchemaTest {
    private static final String ATT = "att-golden";
    private static final int RAW_LINES = 400;
    private static final int DRAFT_LINES = 120;

    // ---- the schema checker ----

    @SuppressWarnings("unchecked")
    private static void check(Object schema, Object value, String path, List<String> errors) {
        Map<String, Object> s = (Map<String, Object>) schema;
        Object type = s.get("type");
        if ("object".equals(type)) {
            if (!(value instanceof Map)) { errors.add("TYPE_OBJECT:" + path); return; }
            Map<String, Object> m = (Map<String, Object>) value;
            Map<String, Object> props = (Map<String, Object>) s.get("properties");
            for (Object key : (List<Object>) s.get("required")) if (!m.containsKey(key)) errors.add("REQUIRED:" + path + "." + key);
            if (Boolean.FALSE.equals(s.get("additionalProperties"))) {
                for (String key : m.keySet()) if (!props.containsKey(key)) errors.add("EXTRA:" + path + "." + key);
            }
            for (Map.Entry<String, Object> entry : m.entrySet()) {
                if (props.containsKey(entry.getKey())) check(props.get(entry.getKey()), entry.getValue(), path + "." + entry.getKey(), errors);
            }
        } else if ("array".equals(type)) {
            if (!(value instanceof List)) { errors.add("TYPE_ARRAY:" + path); return; }
            List<Object> list = (List<Object>) value;
            if (s.containsKey("minItems") && list.size() < ((BigDecimal) s.get("minItems")).intValue()) errors.add("MIN_ITEMS:" + path);
            if (s.containsKey("maxItems") && list.size() > ((BigDecimal) s.get("maxItems")).intValue()) errors.add("MAX_ITEMS:" + path);
            for (int i = 0; i < list.size(); i++) check(s.get("items"), list.get(i), path + "." + i, errors);
        } else if ("string".equals(type)) {
            if (!(value instanceof String)) { errors.add("TYPE_STRING:" + path); return; }
            String text = (String) value;
            int length = text.codePointCount(0, text.length());
            if (s.containsKey("minLength") && length < ((BigDecimal) s.get("minLength")).intValue()) errors.add("MIN_LENGTH:" + path);
            if (s.containsKey("maxLength") && length > ((BigDecimal) s.get("maxLength")).intValue()) errors.add("MAX_LENGTH:" + path);
            if (s.containsKey("pattern") && !Pattern.compile((String) s.get("pattern")).matcher(text).find()) errors.add("PATTERN:" + path);
            if (s.containsKey("enum") && !((List<Object>) s.get("enum")).contains(text)) errors.add("ENUM:" + path);
        } else if ("integer".equals(type)) {
            if (!(value instanceof BigDecimal) || ((BigDecimal) value).stripTrailingZeros().scale() > 0) errors.add("TYPE_INTEGER:" + path);
        } else if ("boolean".equals(type)) {
            if (!(value instanceof Boolean)) errors.add("TYPE_BOOLEAN:" + path);
        } else {
            errors.add("SCHEMA_TYPE_UNSUPPORTED:" + path);
        }
    }

    private static List<String> errorsOf(Object schema, byte[] wire) {
        List<String> errors = new ArrayList<>();
        check(schema, EditorialCanonicalJson.parseObject(wire), "$", errors);
        return errors;
    }

    private static byte[] json(Map<String, Object> m) { return EditorialCanonicalJson.canonicalize(m).getBytes(StandardCharsets.UTF_8); }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static List<Object> list(Object... v) { return new ArrayList<>(List.of(v)); }

    private static BigDecimal n(int v) { return BigDecimal.valueOf(v); }

    // ---- fixtures ----

    private static EditorialRawInventory.Inventory inventory() {
        StringBuilder raw = new StringBuilder();
        for (int i = 1; i <= RAW_LINES; i++) {
            if (i == 99) raw.append("試験用《しけんよう》行99です。\n");
            else raw.append("行").append(i).append("です。\n");
        }
        return EditorialRawInventory.build(raw.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> draftLines() {
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= DRAFT_LINES; i++) lines.add("dòng " + i + " của bản nháp");
        return lines;
    }

    private static List<Object> fullCoverage() {
        return list(map("from", "L1", "to", "L9", "status", "PROCESSED"),
                map("from", "L10", "to", "L99", "status", "PRESERVED"),
                map("from", "L100", "to", "L" + RAW_LINES, "status", "PROCESSED"));
    }

    private static Map<String, Object> rawWire(List<Object> candidates) {
        return map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE, "attemptIdentity", ATT,
                "coverage", fullCoverage(), "candidates", candidates);
    }

    private static List<Object> goldenCandidates() {
        return list(map("candidateId", "c", "ledger", "UNIT", "unitId", "L1", "note", ""),
                map("candidateId", "c2", "ledger", "TG", "unitId", "L9", "note", "term"),
                map("candidateId", "c3", "ledger", "SR", "unitId", "L10", "note", "address"),
                map("candidateId", "a:b/c.d_e-9", "ledger", "PAIR", "unitId", "L" + RAW_LINES, "note", "x".repeat(80)));
    }

    private static Map<String, Object> finding(String id, String kind, int start, int end, int after, String draftQuote,
                                               List<Object> units, List<Object> occurrences, String disposition, String limit) {
        return map("errorId", id, "type", "MEANING", "severity", "MAJOR", "rawUnits", units,
                "draft", map("kind", kind, "start", n(start), "end", n(end), "after", n(after)),
                "rawQuote", "行", "draftQuote", draftQuote, "observation", "obs", "expectedMeaning", "exp",
                "evidenceRefs", list("gl:1"), "candidateIds", list("c"), "occurrenceUnits", occurrences,
                "disposition", disposition, "evidenceLimit", limit);
    }

    private static Map<String, Object> reconcileWire(Map<String, Object> disposition) {
        return map("wireSchemaVersion", EditorialL1Ledger.RECONCILE_WIRE, "attemptIdentity", ATT, "coverage", fullCoverage(),
                "resolutions", list(map("candidateId", "c", "status", "PROCESSED", "findingRef", "f1"),
                        map("candidateId", "c2", "status", "PROCESSED", "findingRef", ""),
                        map("candidateId", "c3", "status", "PRESERVED", "findingRef", ""),
                        map("candidateId", "a:b/c.d_e-9", "status", "PROCESSED", "findingRef", "")),
                "findings", list(
                        finding("f1", "LINES", 3, 3, 0, "dòng 3", list("L1", "L9", "L10"), list("L100", "L" + RAW_LINES), "OPEN", ""),
                        finding("f2", "MISSING", 0, 0, 5, "", list("L99"), list(), "PRESERVED", "profile row decides")),
                "speakerRecords", list(map("unitId", "L10", "speaker", "A", "listener", "", "basis", "narration"),
                        map("unitId", "L9", "speaker", "B", "listener", "UNKNOWN", "basis", "monologue")),
                "protectedSpans", list(map("spanId", "p", "start", n(100), "end", n(101), "source", "GLOSSARY_ROW", "reason", "row")),
                "disposition", disposition);
    }

    private static Map<String, Object> continueDisposition() {
        // reasonCode is empty on purpose: CONTINUE does not need one (S3b)
        return map("disposition", "CONTINUE", "reasonCode", "", "stopClass", "NONE");
    }

    private static List<EditorialL1Ledger.Candidate> rawCandidates(EditorialRawInventory.Inventory inv) {
        return EditorialL1Ledger.parseRawPass(json(rawWire(goldenCandidates())), ATT, inv).candidates();
    }

    // ---- L1 RAW ----

    @Test public void l1RawGoldenWirePassesTheSchemaAndTheParserAcrossTheReferenceBoundaries() {
        EditorialRawInventory.Inventory inv = inventory();
        byte[] wire = json(rawWire(goldenCandidates()));
        assertEquals(List.of(), errorsOf(EditorialL1Ledger.jsonSchema(true), wire));
        EditorialL1Ledger.RawPass pass = EditorialL1Ledger.parseRawPass(wire, ATT, inv);
        assertEquals(4, pass.candidates().size());
        assertEquals(inv.units().get(0).id(), pass.candidates().get(0).unitId());      // L1
        assertEquals(inv.units().get(8).id(), pass.candidates().get(1).unitId());      // L9
        assertEquals(inv.units().get(9).id(), pass.candidates().get(2).unitId());      // L10
        assertEquals(inv.units().get(RAW_LINES - 1).id(), pass.candidates().get(3).unitId());
        assertEquals(3, pass.coverage().size());
    }

    @Test public void l1RawSchemaRefusesWhatTheParserRefuses() {
        Object schema = EditorialL1Ledger.jsonSchema(true);
        for (String bad : List.of("L0", "L", "l1", "L01", "L1 ", "u:1:abcdef12", "", "1")) {
            List<Object> candidates = goldenCandidates();
            ((Map<String, Object>) candidates.get(0)).put("unitId", bad);
            assertFalse("schema accepted unit reference '" + bad + "'", errorsOf(schema, json(rawWire(candidates))).isEmpty());
        }
        for (String bad : List.of("", "-a", " a", "a b", "x".repeat(49))) {
            List<Object> candidates = goldenCandidates();
            ((Map<String, Object>) candidates.get(0)).put("candidateId", bad);
            assertFalse("schema accepted candidateId '" + bad + "'", errorsOf(schema, json(rawWire(candidates))).isEmpty());
        }
        List<Object> longNote = goldenCandidates();
        ((Map<String, Object>) longNote.get(0)).put("note", "x".repeat(81));
        assertFalse(errorsOf(schema, json(rawWire(longNote))).isEmpty());
        Map<String, Object> missingKey = rawWire(goldenCandidates());
        missingKey.remove("candidates");
        assertFalse(errorsOf(schema, json(missingKey)).isEmpty());
        Map<String, Object> extra = rawWire(goldenCandidates());
        extra.put("surprise", "x");
        assertFalse(errorsOf(schema, json(extra)).isEmpty());
    }

    @Test public void aReferenceBeyondTheInventoryPassesTheSchemaButIsAParserRefusalWithItsPath() {
        List<Object> candidates = goldenCandidates();
        ((Map<String, Object>) candidates.get(0)).put("unitId", "L" + (RAW_LINES + 2));
        byte[] wire = json(rawWire(candidates));
        assertEquals(List.of(), errorsOf(EditorialL1Ledger.jsonSchema(true), wire));
        try {
            EditorialL1Ledger.parseRawPass(wire, ATT, inventory());
            org.junit.Assert.fail("expected a refusal");
        } catch (RuntimeException refusal) {
            assertEquals("L1_UNIT_UNKNOWN:candidates.0.unitId", WireViolation.safeMessage(refusal, "L1_WIRE_PARSE_FAILED"));
        }
    }

    // ---- L1 RECONCILE ----

    @Test public void l1ReconcileGoldenWirePassesTheSchemaAndTheParserWithEveryOptionalFieldEmpty() {
        EditorialRawInventory.Inventory inv = inventory();
        byte[] wire = json(reconcileWire(continueDisposition()));
        assertEquals(List.of(), errorsOf(EditorialL1Ledger.jsonSchema(false), wire));
        EditorialL1Ledger.ReconcilePass pass = EditorialL1Ledger.parseReconcile(wire, ATT, inv, draftLines(), rawCandidates(inv));
        assertEquals(2, pass.findings().size());
        assertEquals(2, pass.speakerRecords().size());
        assertEquals(1, pass.protectedSpans().size());
        assertEquals("CONTINUE", pass.disposition().kind());
        assertEquals("", pass.disposition().reasonCode());
        assertEquals(List.of(inv.units().get(0).id(), inv.units().get(8).id(), inv.units().get(9).id()), pass.findings().get(0).rawUnits());
        assertEquals(List.of(inv.units().get(99).id(), inv.units().get(RAW_LINES - 1).id()), pass.findings().get(0).occurrenceUnits());
    }

    @Test public void l1ReconcileRubyQuoteOmittingReadingPassesSchemaAndProductionParser() {
        EditorialRawInventory.Inventory inv = inventory();
        Map<String, Object> wire = reconcileWire(continueDisposition());
        @SuppressWarnings("unchecked") List<Object> findings = (List<Object>) wire.get("findings");
        @SuppressWarnings("unchecked") Map<String, Object> rubyFinding = (Map<String, Object>) findings.get(1);
        rubyFinding.put("rawQuote", "試験用行");
        byte[] bytes = json(wire);
        assertEquals(List.of(), errorsOf(EditorialL1Ledger.jsonSchema(false), bytes));
        EditorialL1Ledger.ReconcilePass pass = EditorialL1Ledger.parseReconcile(
                bytes, ATT, inv, draftLines(), rawCandidates(inv));
        assertEquals("試験用行", pass.findings().get(1).rawQuote());
    }

    @Test public void l1ReconcileStopAndPreserveDispositionsPassBothSides() {
        EditorialRawInventory.Inventory inv = inventory();
        Map<String, Object> stop = map("disposition", "STOP", "reasonCode", "L1_BLOCKED", "stopClass", "CONTENT_BLOCKED");
        byte[] wire = json(reconcileWire(stop));
        assertEquals(List.of(), errorsOf(EditorialL1Ledger.jsonSchema(false), wire));
        assertEquals("STOP", EditorialL1Ledger.parseReconcile(wire, ATT, inv, draftLines(), rawCandidates(inv)).disposition().kind());
        Map<String, Object> preserve = map("disposition", "PRESERVE_DRAFT", "reasonCode", "", "stopClass", "NONE");
        byte[] preserved = json(reconcileWire(preserve));
        assertEquals(List.of(), errorsOf(EditorialL1Ledger.jsonSchema(false), preserved));
        assertEquals("PRESERVE_DRAFT", EditorialL1Ledger.parseReconcile(preserved, ATT, inv, draftLines(), rawCandidates(inv)).disposition().kind());
    }

    @Test public void l1ReconcileSchemaRefusesBadReferencesAndOverlongTextButAcceptsTheShortestOnes() {
        Object schema = EditorialL1Ledger.jsonSchema(false);
        for (String good : List.of("L1", "L9", "L10", "L99", "L100", "L" + RAW_LINES)) {
            Map<String, Object> wire = reconcileWire(continueDisposition());
            @SuppressWarnings("unchecked") Map<String, Object> first = (Map<String, Object>) ((List<Object>) wire.get("findings")).get(0);
            first.put("rawUnits", list(good));
            first.put("occurrenceUnits", list(good));
            ((Map<String, Object>) ((List<Object>) wire.get("speakerRecords")).get(0)).put("unitId", good);
            ((Map<String, Object>) ((List<Object>) wire.get("coverage")).get(0)).put("from", good);
            assertEquals(good, List.of(), errorsOf(schema, json(wire)));
        }
        Map<String, Object> overlong = reconcileWire(continueDisposition());
        @SuppressWarnings("unchecked") Map<String, Object> f = (Map<String, Object>) ((List<Object>) overlong.get("findings")).get(0);
        f.put("observation", "x".repeat(241));
        assertFalse(errorsOf(schema, json(overlong)).isEmpty());
        Map<String, Object> blankObservation = reconcileWire(continueDisposition());
        @SuppressWarnings("unchecked") Map<String, Object> g = (Map<String, Object>) ((List<Object>) blankObservation.get("findings")).get(0);
        g.put("observation", "   ");
        assertFalse("a blank MUST text must be refused by the schema", errorsOf(schema, json(blankObservation)).isEmpty());
        Map<String, Object> badReason = reconcileWire(map("disposition", "STOP", "reasonCode", "bad reason with * symbol", "stopClass", "NONE"));
        assertFalse(errorsOf(schema, json(badReason)).isEmpty());
    }

    // ---- final read ----

    @Test public void finalReadGoldenWirePassesTheSchemaAndTheParser() {
        StringBuilder target = new StringBuilder();
        for (int i = 1; i <= 30; i++) target.append("Dong so ").append(i).append(" co noi dung\n");
        byte[] bytes = target.toString().getBytes(StandardCharsets.UTF_8);
        List<String> lines = EditorialFinalRead.lines(bytes);
        List<Object> tails = new ArrayList<>();
        for (Integer line : EditorialFinalRead.probeLines(bytes)) {
            String text = lines.get(line - 1);
            tails.add(map("line", n(line), "tail", text.length() <= 12 ? text : text.substring(text.length() - 12)));
        }
        Map<String, Object> wire = map("wireSchemaVersion", EditorialFinalRead.WIRE, "attemptIdentity", ATT,
                "readSha256", EditorialCanonicalJson.sha256Hex(bytes), "probeTails", tails, "verdict", "DEFECTS",
                "defects", list(map("line", n(5), "quote", "co noi dung", "type", "MEANING", "note", "")));
        assertEquals(List.of(), errorsOf(EditorialFinalRead.jsonSchema(), json(wire)));
        assertEquals("DEFECTS", EditorialFinalRead.parse(json(wire), ATT, bytes).verdict());
        wire.put("readSha256", "A".repeat(64));
        assertFalse(errorsOf(EditorialFinalRead.jsonSchema(), json(wire)).isEmpty());
    }

    // ---- every phase of the field table ----

    @Test public void everyStringFieldOfEveryPhaseHasASatisfiableLeafSchemaThatAdmitsItsShortestLegalValue() {
        List<String> unitRefs = List.of("L1", "L9", "L10", "L99", "L100", "L" + RAW_LINES, "L2147483647");
        List<String> ids = List.of("c", "c1", "E-1", "a:b/c.d_e");
        for (String phase : EditorialFieldSpec.phases()) {
            for (EditorialFieldSpec.Field field : EditorialFieldSpec.fields(phase)) {
                if (field.kind() != EditorialFieldSpec.Kind.STRING && field.kind() != EditorialFieldSpec.Kind.ENUM) continue;
                Map<String, Object> leaf = EditorialFieldSpec.schema(phase, field.path());
                String where = phase + ":" + field.path();
                List<String> samples = new ArrayList<>(List.of("", "a", "NONE", "OK", "CHANGE:c", "PRESERVE:p", "x y",
                        "a".repeat(64), "a".repeat(Math.max(1, field.minLength()))));
                samples.addAll(field.values());
                samples.addAll(unitRefs);
                if (EditorialFieldSpec.UNIT_REFERENCE_PATTERN.equals(field.pattern())) {
                    for (String ref : unitRefs) {
                        List<String> errors = new ArrayList<>();
                        check(leaf, ref, "$", errors);
                        assertEquals(where + " refuses " + ref, List.of(), errors);
                    }
                    for (String bad : List.of("L0", "L", "l1", "L01", "u:1:abcdef12", "")) {
                        List<String> errors = new ArrayList<>();
                        check(leaf, bad, "$", errors);
                        assertFalse(where + " accepts " + bad, errors.isEmpty());
                    }
                } else if (field.pattern().startsWith("^[A-Za-z0-9][A-Za-z0-9._:/-]*$") && field.minLength() == 1) {
                    for (String id : ids) {
                        List<String> errors = new ArrayList<>();
                        check(leaf, id, "$", errors);
                        assertEquals(where + " refuses id " + id, List.of(), errors);
                    }
                }
                boolean satisfiable = false;
                for (String sample : samples) {
                    List<String> errors = new ArrayList<>();
                    check(leaf, sample, "$", errors);
                    satisfiable |= errors.isEmpty();
                }
                assertTrue(where + " has no satisfiable sample among the probes", satisfiable);
                List<String> tooLong = new ArrayList<>();
                check(leaf, "a".repeat(field.maxLength() + 1), "$", tooLong);
                assertFalse(where + " accepts an overlong value", tooLong.isEmpty());
            }
        }
    }
}
