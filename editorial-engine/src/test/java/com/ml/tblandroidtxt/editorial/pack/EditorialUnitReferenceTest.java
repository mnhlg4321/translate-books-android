package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public final class EditorialUnitReferenceTest {
    private static final EditorialRawInventory.Inventory INV = EditorialRawInventory.build(
            "\uFEFFfirst\r\n\r\n[IMG_001]\n　\nlast".getBytes(StandardCharsets.UTF_8));

    private static void code(String code, Runnable action) {
        try { action.run(); fail("expected " + code); }
        catch (IllegalArgumentException invalid) { assertEquals(code, invalid.getMessage()); }
    }

    private static byte[] wire(String label, Object coverage, Object candidates) {
        return EditorialCanonicalJson.canonicalize(Map.of("wireSchemaVersion", label, "attemptIdentity", "att",
                "coverage", coverage, "candidates", candidates)).getBytes(StandardCharsets.UTF_8);
    }

    @Test public void validPhysicalLineResolvesToExactAppHash() {
        assertEquals(INV.units().get(0).id(), EditorialUnitReference.resolve("L1", INV));
        assertEquals(INV.units().get(1).id(), EditorialUnitReference.resolve("L5", INV));
        assertEquals("L5", EditorialUnitReference.fromId(INV.units().get(1).id()));
    }

    @Test public void blankUnicodeBlankAndImageMarkerAreNotUnits() {
        for (String ref : List.of("L2", "L3", "L4")) {
            code("L1_UNIT_LINE_NOT_A_UNIT", () -> EditorialUnitReference.resolve(ref, INV));
        }
    }

    @Test public void outOfRangeAndOverflowAreUnknown() {
        for (String ref : List.of("L6", "L2147483648", "L9999999999999999999999999")) {
            code("L1_UNIT_UNKNOWN", () -> EditorialUnitReference.resolve(ref, INV));
        }
    }

    @Test public void malformedAndFullIdsAreRejectedAtTheWireBoundary() {
        for (Object ref : List.of("L0", "L01", "l1", "L-1", "L1 ", " L1", "L1\n", "1", 1, INV.units().get(0).id())) {
            code("L1_UNIT_REF_INVALID", () -> EditorialUnitReference.resolve(ref, INV));
        }
        code("L1_UNIT_REF_INVALID", () -> EditorialUnitReference.resolve(null, INV));
        code("L1_UNIT_REF_INVALID", () -> EditorialUnitReference.resolveList(List.of(""), "rawUnits", 6, INV));
    }

    @Test public void rawAndL2DiscoveryCoverageSkipExcludedLinesAndPersistFullIds() {
        List<Object> ranges = List.of(Map.of("from", "L1", "to", "L1", "status", "PROCESSED"),
                Map.of("from", "L5", "to", "L5", "status", "PRESERVED"));
        for (String label : List.of(EditorialL1Ledger.RAW_WIRE, EditorialL2Execution.DISCOVERY_WIRE_V3)) {
            var raw = EditorialL1Ledger.parseRawPass(wire(label, ranges,
                    List.of(Map.of("candidateId", "c1", "ledger", "UNIT", "unitId", "L5", "note", "n"))), "att", INV, label);
            assertEquals(INV.units().get(1).id(), raw.candidates().get(0).unitId());
            assertEquals(INV.units().get(1).id(), raw.coverage().get(1).fromId());
            var body = EditorialL1Ledger.bodyOfRawPass(INV, raw);
            Map<String, Object> report = EditorialL1Ledger.bodyToMap(body);
            assertEquals(body, EditorialL1Ledger.parseBody(EditorialCanonicalJson.parseObject(
                    EditorialCanonicalJson.canonicalize(report).getBytes(StandardCharsets.UTF_8))));
            assertTrue(EditorialCanonicalJson.canonicalize(report).contains(INV.units().get(1).id()));
            report.put("contractRevision", EditorialContractRevision.L1_LEDGER_V2);
            assertEquals(body, EditorialL1Ledger.parseBody(report)); // historical display/readback
        }
    }

    @Test public void coverageStillRefusesGapsOverlapsAndNonUnitEndpoints() {
        code("L1_COVERAGE_GAP", () -> EditorialL1Ledger.parseRawPass(wire(EditorialL1Ledger.RAW_WIRE,
                List.of(Map.of("from", "L5", "to", "L5", "status", "PROCESSED")), List.of()), "att", INV));
        code("L1_COVERAGE_OVERLAP", () -> EditorialL1Ledger.parseRawPass(wire(EditorialL1Ledger.RAW_WIRE,
                List.of(Map.of("from", "L1", "to", "L5", "status", "PROCESSED"),
                        Map.of("from", "L5", "to", "L5", "status", "PROCESSED")), List.of()), "att", INV));
        code("L1_UNIT_LINE_NOT_A_UNIT", () -> EditorialL1Ledger.parseRawPass(wire(EditorialL1Ledger.RAW_WIRE,
                List.of(Map.of("from", "L2", "to", "L5", "status", "PROCESSED")), List.of()), "att", INV));
    }

    @Test public void v2ResponseCannotAnswerAV3Attempt() {
        code("L1_WIRE_SCHEMA_INVALID", () -> EditorialL1Ledger.parseRawPass(wire("safe4.l1.raw-ledger.wire.v2",
                List.of(), List.of()), "att", INV));
        assertFalse(EditorialContractRevision.eligiblePredecessor(EditorialContractRevision.L1_LEDGER_V2,
                EditorialContractRevision.L1_LEDGER_V3));
    }

    @Test public void l3CandidatesAndProbesResolveBeforeVerificationAndReceiptStorage() {
        var coverage = List.of(Map.of("from", "L1", "to", "L5", "status", "PROCESSED"));
        var candidates = List.of(Map.of("candidateId", "c1", "ledger", "UNIT", "unitId", "L5",
                "viLine", java.math.BigDecimal.ONE, "status", "PROCESSED", "note", "n"));
        var pass = EditorialL3Ledger.parseReaudit(wire(EditorialL3Execution.REAUDIT_WIRE_V3, coverage, candidates),
                "att", INV, 1);
        assertEquals(INV.units().get(1).id(), pass.candidates().get(0).unitId());
        assertTrue(new String(EditorialL3Ledger.candidateBlock(pass.candidates()), StandardCharsets.UTF_8)
                .contains("\"unitId\":\"L5\""));
        Map<String, Object> probe = new java.util.LinkedHashMap<>();
        probe.put("probeId", "p1"); probe.put("kind", "COVERAGE"); probe.put("rawUnits", List.of("L5"));
        probe.put("viStart", java.math.BigDecimal.ONE); probe.put("viEnd", java.math.BigDecimal.ONE); probe.put("scope", "s"); probe.put("contrast", "c");
        probe.put("rawQuote", "last"); probe.put("viQuote", "last"); probe.put("verdict", "NO_DEFECT"); probe.put("action", "NONE");
        Map<String, Object> root = Map.of("wireSchemaVersion", EditorialL3Execution.RECONCILE_WIRE_V3,
                "attemptIdentity", "att", "resolutions", List.of(Map.of("candidateId", "c1", "status", "PROCESSED")),
                "carriedResolutions", List.of(), "changes", List.of(), "preserved", List.of(), "probes", List.of(probe),
                "disposition", Map.of("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE"));
        var parsed = EditorialL3Ledger.parseReconcile(EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8),
                "att", pass.candidates(), 0, INV);
        assertEquals(List.of(INV.units().get(1).id()), parsed.probes().get(0).rawUnits());
        probe.put("rawUnits", List.of("L3"));
        code("L1_UNIT_LINE_NOT_A_UNIT", () -> EditorialL3Ledger.parseReconcile(
                EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8), "att", pass.candidates(), 0, INV));
    }

    @Test public void l3CandidateFailureReportsItsIndexedField() {
        var coverage = List.of(Map.of("from", "L1", "to", "L5", "status", "PROCESSED"));
        var badCandidate = Map.of("candidateId", "c1", "ledger", "UNIT", "unitId", "L5",
                "viLine", "one", "status", "PROCESSED", "note", "n");
        try {
            EditorialL3Ledger.parseReaudit(wire(EditorialL3Execution.REAUDIT_WIRE_V3, coverage, List.of(badCandidate)),
                    "att", INV, 1);
            fail("expected candidate line type failure");
        } catch (RuntimeException invalid) {
            assertEquals("L1_INT_INVALID:candidates.0.viLine", WireViolation.safeMessage(invalid, "L3_PARSE_FAILED"));
        }
    }

    @Test public void modelViewChangesOnlyReferenceFieldsAndKeepsDurableInputIntact() {
        String id = INV.units().get(0).id();
        Map<String, Object> report = Map.of("unitId", id, "rawUnits", List.of(id), "occurrenceUnits", List.of(id),
                "rawQuote", id, "candidateId", id, "coverage", List.of(Map.of("from", id, "to", id)));
        String view = EditorialCanonicalJson.canonicalize(EditorialUnitReference.wireView(report));
        assertTrue(view.contains("\"unitId\":\"L1\""));
        assertTrue(view.contains("\"rawUnits\":[\"L1\"]"));
        assertTrue(view.contains("\"from\":\"L1\""));
        assertTrue(view.contains("\"rawQuote\":\"" + id + "\""));
        assertEquals(id, report.get("unitId"));
    }

    @Test public void strictSchemaPinsLineSyntaxWithoutRelaxingLimits() {
        String raw = EditorialCanonicalJson.canonicalize(EditorialL1Ledger.jsonSchema(true));
        String reconcile = EditorialCanonicalJson.canonicalize(EditorialL1Ledger.jsonSchema(false));
        assertEquals(3, raw.split("\\Q^L[1-9][0-9]*$\\E", -1).length - 1);
        assertEquals(5, reconcile.split("\\Q^L[1-9][0-9]*$\\E", -1).length - 1);
        assertTrue(raw.contains("\"maxLength\":80"));
        assertTrue(raw.contains("\"maxItems\":400"));
    }
}
