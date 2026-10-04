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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Z2: strict schemas for the L2/L3 phases are generated from the field table. They must describe exactly the keys of
 * the table, every golden wire (inapplicable values empty) must satisfy both the schema and the production parser, and
 * the legacy and L1 wires must keep their previous response formats. Synthetic text only.
 */
public final class EditorialStrictSchemaTest {
    private static final String ATT = "att-strict";
    private static final String RAW = "raw 1\n\nraw 3\nraw 4\nraw 5";
    private static final String DRAFT = "draft 1\n\ndraft 3\ndraft 4\ndraft 5";

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }

    private static List<Object> list(Object... values) { return new ArrayList<>(List.of(values)); }

    private static byte[] json(Map<String, Object> value) { return EditorialCanonicalJson.canonicalize(value).getBytes(StandardCharsets.UTF_8); }

    // ---- a small schema checker (same stand-in as the L1 golden test) ----

    @SuppressWarnings("unchecked")
    private static void check(Object schema, Object value, String path, List<String> errors) {
        Map<String, Object> s = (Map<String, Object>) schema;
        Object type = s.get("type");
        if ("object".equals(type)) {
            if (!(value instanceof Map)) { errors.add("TYPE_OBJECT:" + path); return; }
            Map<String, Object> m = (Map<String, Object>) value;
            Map<String, Object> props = (Map<String, Object>) s.get("properties");
            for (Object key : (List<Object>) s.get("required")) if (!m.containsKey(key)) errors.add("REQUIRED:" + path + "." + key);
            for (String key : m.keySet()) if (!props.containsKey(key)) errors.add("EXTRA:" + path + "." + key);
            for (Map.Entry<String, Object> entry : m.entrySet()) {
                if (props.containsKey(entry.getKey())) check(props.get(entry.getKey()), entry.getValue(), path + "." + entry.getKey(), errors);
            }
        } else if ("array".equals(type)) {
            if (!(value instanceof List)) { errors.add("TYPE_ARRAY:" + path); return; }
            List<Object> items = (List<Object>) value;
            if (s.containsKey("minItems") && items.size() < ((BigDecimal) s.get("minItems")).intValue()) errors.add("MIN_ITEMS:" + path);
            if (s.containsKey("maxItems") && items.size() > ((BigDecimal) s.get("maxItems")).intValue()) errors.add("MAX_ITEMS:" + path);
            for (int i = 0; i < items.size(); i++) check(s.get("items"), items.get(i), path + "." + i, errors);
        } else if ("string".equals(type)) {
            if (!(value instanceof String text)) { errors.add("TYPE_STRING:" + path); return; }
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

    private static List<String> errorsOf(Map<String, Object> schema, Map<String, Object> wire) {
        List<String> errors = new ArrayList<>();
        check(schema, EditorialCanonicalJson.parseObject(json(wire)), "$", errors);
        return errors;
    }

    // ---- structure ----

    @SuppressWarnings("unchecked")
    private static void assertStrictShape(Object schema, String where) {
        Map<String, Object> s = (Map<String, Object>) schema;
        if ("object".equals(s.get("type"))) {
            assertEquals(where, Boolean.FALSE, s.get("additionalProperties"));
            Map<String, Object> props = (Map<String, Object>) s.get("properties");
            assertEquals(where + " required == properties", new ArrayList<Object>(props.keySet()), s.get("required"));
            for (Map.Entry<String, Object> entry : props.entrySet()) assertStrictShape(entry.getValue(), where + "." + entry.getKey());
        } else if ("array".equals(s.get("type"))) {
            assertNotNull(where + " items", s.get("items"));
            assertStrictShape(s.get("items"), where + "[]");
        }
        assertFalse(where + " uses a union", s.containsKey("anyOf") || s.containsKey("oneOf") || s.containsKey("allOf"));
    }

    @SuppressWarnings("unchecked")
    private static Object walk(Object schema, String path) {
        Object current = schema;
        for (String raw : path.split("\\.")) {
            boolean array = raw.endsWith("[]");
            String name = array ? raw.substring(0, raw.length() - 2) : raw;
            Map<String, Object> s = (Map<String, Object>) current;
            if ("array".equals(s.get("type"))) s = (Map<String, Object>) s.get("items");
            Map<String, Object> props = (Map<String, Object>) s.get("properties");
            if (props == null || !props.containsKey(name)) return null;
            current = props.get(name);
            if (array) current = ((Map<String, Object>) current).get("items");
        }
        return current;
    }

    @Test public void everyGeneratedPhaseIsStrictAndCoversEveryFieldOfTheTable() {
        for (String phase : List.of(EditorialFieldSpec.L2_RAW_DISCOVERY, EditorialFieldSpec.L2_EDIT, EditorialFieldSpec.L2_FINAL_READ,
                EditorialFieldSpec.L3_RAW_FIRST_REAUDIT, EditorialFieldSpec.L3_RECONCILE, EditorialFieldSpec.L3_FINAL_READ)) {
            Map<String, Object> schema = EditorialStrictSchema.forPhase(phase);
            assertStrictShape(schema, phase);
            for (EditorialFieldSpec.Field field : EditorialFieldSpec.fields(phase)) {
                assertNotNull(phase + ": " + field.path() + " missing from the schema", walk(schema, field.path()));
            }
        }
    }

    @Test public void forRequestSelectsOnlyTheGeneratedWires() {
        assertEquals("safe4_l2_edit_strict", EditorialStrictSchema.forRequest("safe4.l2.edit.wire.v3", "L2_EDIT").name());
        assertEquals("safe4_l3_reconcile_strict", EditorialStrictSchema.forRequest("safe4.l3.reconcile.wire.v3", "L3_RECONCILE").name());
        assertEquals("safe4_l3_final_read_strict", EditorialStrictSchema.forRequest(EditorialFinalRead.WIRE, "L3_FINAL_READ").name());
        assertEquals("safe4_l2_final_read_strict", EditorialStrictSchema.forRequest(EditorialFinalRead.WIRE, "L2_FINAL_READ").name());
        assertNotNull(EditorialStrictSchema.forRequest("safe4.l2.raw-discovery.wire.v3", "L2_RAW_DISCOVERY"));
        assertNotNull(EditorialStrictSchema.forRequest("safe4.l3.reaudit.wire.v3", "L3_RAW_FIRST_REAUDIT"));
        assertNull("legacy L2 edit wire keeps json_object", EditorialStrictSchema.forRequest(EditorialL2Execution.WIRE_SCHEMA_VERSION, "L2_EDIT"));
        assertNull("L1 has its own generator", EditorialStrictSchema.forRequest(EditorialL1Ledger.RECONCILE_WIRE, "L1_RECONCILE"));
        assertNull(EditorialStrictSchema.forRequest("unknown", "L2_EDIT"));
        assertNull(EditorialStrictSchema.forRequest(null, "L2_EDIT"));
    }

    // ---- golden wires: inapplicable values empty, accepted by the schema and by the production parser ----

    private static EditorialRawInventory.Inventory inventory() {
        return EditorialRawInventory.build(RAW.getBytes(StandardCharsets.UTF_8));
    }

    @Test public void discoveryAndReauditGoldenWiresPassSchemaAndParser() {
        EditorialRawInventory.Inventory inv = inventory();
        Map<String, Object> discovery = map("wireSchemaVersion", "safe4.l2.raw-discovery.wire.v3", "attemptIdentity", ATT,
                "coverage", list(map("from", "L1", "to", "L5", "status", "PROCESSED")),
                "candidates", list(map("candidateId", "c1", "ledger", "UNIT", "unitId", "L3", "note", "")));
        assertEquals(List.of(), errorsOf(EditorialStrictSchema.forPhase(EditorialFieldSpec.L2_RAW_DISCOVERY), discovery));
        assertEquals(1, EditorialL1Ledger.parseRawPass(json(discovery), ATT, inv, "safe4.l2.raw-discovery.wire.v3").candidates().size());

        Map<String, Object> reaudit = map("wireSchemaVersion", "safe4.l3.reaudit.wire.v3", "attemptIdentity", ATT,
                "coverage", list(map("from", "L1", "to", "L5", "status", "PROCESSED")),
                "candidates", list(map("candidateId", "c1", "ledger", "UNIT", "unitId", "L3", "viLine", BigDecimal.valueOf(3),
                        "status", "PROCESSED", "note", "")));
        assertEquals(List.of(), errorsOf(EditorialStrictSchema.forPhase(EditorialFieldSpec.L3_RAW_FIRST_REAUDIT), reaudit));
        assertEquals(1, EditorialL3Ledger.parseReaudit(json(reaudit), ATT, inv, 5).candidates().size());
    }

    private static Map<String, Object> goldenChange() {
        return map("changeId", "C1", "errorId", "E1", "line", BigDecimal.valueOf(3), "before", "", "after", "draft three",
                "reason", "synthetic reason", "dialogue", Boolean.FALSE, "status", "CLOSED", "op", "REPLACE",
                "speakerProof", map("speaker", "", "listener", "", "anchorBefore", "", "anchorAfter", ""));
    }

    @Test public void l3ReconcileGoldenWirePassesSchemaAndParserAndLeavesNoNoise() {
        EditorialRawInventory.Inventory inv = inventory();
        Map<String, Object> wire = map("wireSchemaVersion", "safe4.l3.reconcile.wire.v3", "attemptIdentity", ATT,
                "resolutions", list(map("candidateId", "t1", "status", "PROCESSED")),
                "carriedResolutions", list(),
                "changes", list(goldenChange()),
                "preserved", list(map("preserveId", "P1", "line", BigDecimal.valueOf(4), "before", "draft 4", "evidenceLimit", "synthetic")),
                "probes", list(map("probeId", "pr-1", "kind", "COVERAGE", "rawUnits", list("L3"), "viStart", BigDecimal.valueOf(3),
                        "viEnd", BigDecimal.valueOf(3), "scope", "scope", "contrast", "contrast", "rawQuote", "raw 3",
                        "viQuote", "draft 3", "verdict", "NO_DEFECT", "action", "NONE")),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "", "stopClass", "NONE"));
        assertEquals(List.of(), errorsOf(EditorialStrictSchema.forPhase(EditorialFieldSpec.L3_RECONCILE), wire));
        List<EditorialL3Ledger.Candidate> candidates = List.of(new EditorialL3Ledger.Candidate("t1", "UNIT",
                inv.units().get(0).id(), 1, "PROCESSED", ""));
        EditorialL3Ledger.ReconcileWire parsed = EditorialL3Ledger.parseReconcile(json(wire), ATT, candidates, 0, inv,
                DRAFT.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, parsed.rows().changes().size());
        // an empty `before` and an empty speakerProof of a non-dialogue change are how a strict wire says "not supplied"
        assertEquals(List.of(), parsed.rows().warnings());
    }

    @Test public void l2EditGoldenWirePassesSchemaAndParser() {
        EditorialRawInventory.Inventory inv = inventory();
        EditorialL1Ledger.RawPass rawPass = new EditorialL1Ledger.RawPass(
                List.of(new EditorialRawInventory.Range(inv.units().get(0).id(), inv.units().get(inv.units().size() - 1).id(), "PROCESSED")),
                List.of());
        Map<String, Object> l1Finding = map("errorId", "E1", "type", "MEANING", "severity", "MAJOR", "rawUnits", list("L3"),
                "draft", map("kind", "LINES", "start", BigDecimal.valueOf(3), "end", BigDecimal.valueOf(3), "after", BigDecimal.ZERO),
                "rawQuote", "raw 3", "draftQuote", "draft 3", "observation", "o", "expectedMeaning", "e",
                "evidenceRefs", list(), "candidateIds", list(), "occurrenceUnits", list(), "disposition", "OPEN", "evidenceLimit", "");
        Map<String, Object> reconcile = map("wireSchemaVersion", EditorialL1Ledger.RECONCILE_WIRE, "attemptIdentity", "r",
                "coverage", list(map("from", "L1", "to", "L5", "status", "PROCESSED")), "resolutions", list(),
                "findings", list(l1Finding), "speakerRecords", list(), "protectedSpans", list(),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "", "stopClass", "NONE"));
        EditorialL1Ledger.ReconcilePass pass = EditorialL1Ledger.parseReconcile(json(reconcile), "r", inv,
                EditorialL1Ledger.draftLines(DRAFT.getBytes(StandardCharsets.UTF_8)), List.of());
        EditorialL1Ledger.Body body = EditorialL1Ledger.bodyOfReconcile(inv, rawPass, pass);

        Map<String, Object> wire = map("wireSchemaVersion", "safe4.l2.edit.wire.v3", "attemptIdentity", ATT,
                "resolutions", list(),
                "findingResolutions", list(map("errorId", "E1", "status", "FIXED", "changeIds", list("C1"), "preserveIds", list(),
                        "evidenceQuote", "", "reason", "", "occurrences", list())),
                "changes", list(goldenChange()), "preserved", list(),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "", "stopClass", "NONE"));
        assertEquals(List.of(), errorsOf(EditorialStrictSchema.forPhase(EditorialFieldSpec.L2_EDIT), wire));
        EditorialL2Execution.EditWire parsed = EditorialL2Execution.parseEditWireV3(json(wire), ATT, List.of(), body, inv,
                DRAFT.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, parsed.rows().changes().size());
        assertEquals(List.of(), parsed.rows().warnings());
        assertEquals(1, parsed.findingResolutions().size());
    }

    @Test public void theSchemaRefusesWhatTheTableForbids() {
        Map<String, Object> schema = EditorialStrictSchema.forPhase(EditorialFieldSpec.L2_EDIT);
        Map<String, Object> wire = map("wireSchemaVersion", "safe4.l2.edit.wire.v3", "attemptIdentity", ATT,
                "resolutions", list(), "findingResolutions", list(), "changes", list(goldenChange()), "preserved", list(),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "", "stopClass", "NONE"));
        assertEquals(List.of(), errorsOf(schema, wire));
        @SuppressWarnings("unchecked") Map<String, Object> change = (Map<String, Object>) ((List<Object>) wire.get("changes")).get(0);
        change.remove("speakerProof");
        assertFalse(errorsOf(schema, wire).isEmpty());
        change.put("speakerProof", map("speaker", "", "listener", "", "anchorBefore", "", "anchorAfter", ""));
        change.put("status", "OPEN");
        assertTrue(errorsOf(schema, wire).stream().anyMatch(e -> e.startsWith("ENUM:")));
        change.put("status", "CLOSED");
        wire.put("zz", "x");
        assertTrue(errorsOf(schema, wire).stream().anyMatch(e -> e.startsWith("EXTRA:")));
    }
}
