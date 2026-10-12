package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Test-first size, materialization and replay evidence for the P5E wire DTO. */
public final class EditorialP5RawWireContractTest {
    @Test public void currentFullShapeWithDuplicatedRawTextExceedsByteBudget() {
        String raw = "R".repeat(23_814);
        Map<String, Object> full = new LinkedHashMap<>();
        full.put("reportSchemaVersion", EditorialP5RawWireContract.FINAL_REPORT_SCHEMA);
        full.put("receiptSchemaVersion", EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION);
        full.put("bindingIdentity", "a".repeat(64));
        full.put("manifestFingerprint", "b".repeat(64));
        full.put("chapterKey", "001");
        full.put("phase", "L1");
        full.put("bundleIdentity", "c".repeat(64));
        full.put("predecessorIdentity", "d".repeat(64));
        full.put("stableAnchors", List.of("chapter:001"));
        full.put("ledger", Map.of("populationIds", List.of("population:001"),
                "entries", List.of(Map.of("itemId", "population:001", "disposition", "PROCESSED",
                        "evidenceRefs", List.of("evidence:raw"), "modelDeclaredPass", false))));
        Map<String, String> gates = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) gates.put(gate, "PASS");
        full.put("gates", gates);
        full.put("preservedInventory", List.of());
        full.put("declaredChanges", List.of());
        full.put("beforeText", raw);
        full.put("afterText", raw);
        full.put("releaseAttemptCount", BigDecimal.ZERO);
        full.put("disposition", Map.of("disposition", "CONTINUE", "reasonCode", "LOCAL_REVIEW",
                "phase", "L1", "blockingGate", "COVERAGE", "evidenceRefs", List.of(),
                "affectedScope", "NONE", "recoveryAction", "No action", "resumeFrom", "L1"));
        full.put("evidenceRefs", List.of("evidence:raw"));
        full.put("modelDeclaredPass", false);

        int serializedBytes = EditorialCanonicalJson.canonicalize(full)
                .getBytes(StandardCharsets.UTF_8).length;
        assertEquals(47_628, raw.getBytes(StandardCharsets.UTF_8).length * 2);
        assertTrue("duplicated current-schema source must be rejected before provider dispatch: "
                + serializedBytes, serializedBytes > EditorialP5RawWireContract.MAX_WIRE_BYTES);
        // Exact GPT tokenization is not bundled in the engine module. The
        // conservative 4-byte/token estimate is recorded as characterization
        // only; the hard local bound is the measured UTF-8 byte size.
        int estimatedTokens = (serializedBytes + 3) / 4;
        assertTrue("4-byte/token estimate must exceed 4096: " + estimatedTokens,
                estimatedTokens > EditorialP5RawWireContract.OUTPUT_TOKEN_CAP);
    }

    @Test public void declaredWorstCaseWireIsBelowCapWithMargin() {
        int worstCase = EditorialP5RawWireContract.worstCaseWireBytes();
        assertEquals(2_785, worstCase);
        assertTrue("worst-case wire bytes=" + worstCase,
                worstCase <= EditorialP5RawWireContract.MAX_WIRE_BYTES);
        assertTrue(EditorialP5RawWireContract.MAX_WIRE_BYTES
                < EditorialP5RawWireContract.OUTPUT_TOKEN_CAP);
    }

    @Test public void providerSchemaIsClosedAndCarriesAllCompactHardLimits() {
        String schema = EditorialCanonicalJson.canonicalize(EditorialP5RawWireContract.jsonSchema());

        assertTrue(schema.contains("\"additionalProperties\":false"));
        assertTrue(schema.contains("\"maxItems\":4"));
        assertTrue(schema.contains("\"maxItems\":8"));
        assertTrue(schema.contains("\"maxItems\":0"));
        assertEquals("safe4_raw_discovery_v1", EditorialP5RawWireContract.SCHEMA_NAME);
    }

    @Test public void compactWireContainsNoSourceAndMaterializesExactRawBytes() {
        byte[] raw = "RAW exact bytes — no model echo\n".getBytes(StandardCharsets.UTF_8);
        String attempt = "a".repeat(64);
        String envelope = "b".repeat(64);
        EditorialP5RawWireResponse wire = new EditorialP5RawWireResponse(
                EditorialP5RawWireContract.SCHEMA_VERSION, attempt, envelope,
                List.of(new EditorialLedgerValidator.Entry("population:001", "PROCESSED",
                        List.of("evidence:raw"), true)), gates(), List.of("evidence:raw"),
                List.of(), 0, EditorialStopDecision.continueWithoutStop(
                        "L1", "COVERAGE", "LOCAL_REVIEW"), true);

        String wireJson = new String(wire.wireBytes(), StandardCharsets.UTF_8);
        assertFalse(wireJson.contains("RAW exact bytes"));
        EditorialP5L1Output output = wire.materialize(providerRequest(attempt, envelope, raw));
        assertEquals(new String(raw, StandardCharsets.UTF_8), output.beforeText());
        assertEquals(output.beforeText(), output.afterText());
        assertTrue(Arrays.equals(raw, output.beforeText().getBytes(StandardCharsets.UTF_8)));
        assertTrue(output.declaredChanges().isEmpty());
        assertEquals(EditorialP5RawWireContract.FINAL_REPORT_SCHEMA, output.reportSchemaVersion());
        assertEquals("binding-from-app", output.bindingIdentity());
        assertTrue(output.modelDeclaredPass());
    }

    @Test public void replayAcrossAttemptOrRequestBindingIsRejected() {
        EditorialP5RawWireResponse wire = new EditorialP5RawWireResponse(
                EditorialP5RawWireContract.SCHEMA_VERSION, "a".repeat(64), "b".repeat(64),
                List.of(), gates(), List.of(), List.of(), 0,
                EditorialStopDecision.continueWithoutStop("L1", "COVERAGE", "LOCAL_REVIEW"), false);
        try {
            wire.materialize(providerRequest("c".repeat(64), "b".repeat(64), bytes("raw")));
            throw new AssertionError("attempt replay must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("attempt identity"));
        }
        try {
            wire.materialize(providerRequest("a".repeat(64), "d".repeat(64), bytes("raw")));
            throw new AssertionError("request replay must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("request envelope"));
        }
    }

    @Test public void nonEmptyDeclaredChangesAreRejectedAtWireConstruction() {
        try {
            new EditorialP5RawWireResponse(EditorialP5RawWireContract.SCHEMA_VERSION,
                    "a".repeat(64), "b".repeat(64), List.of(), gates(), List.of(), List.of(), 1,
                    EditorialStopDecision.continueWithoutStop("L1", "COVERAGE", "LOCAL_REVIEW"), false);
            throw new AssertionError("RAW wire edits must be forbidden");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_DECLARED_CHANGES_FORBIDDEN"));
        }
    }

    @Test public void compactWireRejectsDuplicateFindingAndEvidenceReferences() {
        List<EditorialLedgerValidator.Entry> findings = List.of(
                new EditorialLedgerValidator.Entry("population:001", "PROCESSED",
                        List.of("evidence:raw"), true),
                new EditorialLedgerValidator.Entry("population:001", "NOT_EVALUATED",
                        List.of("evidence:raw"), false));
        try {
            new EditorialP5RawWireResponse(EditorialP5RawWireContract.SCHEMA_VERSION,
                    "a".repeat(64), "b".repeat(64), findings, gates(),
                    List.of("evidence:raw", "evidence:raw"), List.of(), 0,
                    EditorialStopDecision.continueWithoutStop("L1", "COVERAGE", "LOCAL_REVIEW"), false);
            throw new AssertionError("duplicate finding/evidence references must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_WIRE_DUPLICATE_FINDING_ID"));
            assertTrue(expected.getMessage().contains("RAW_WIRE_DUPLICATE_EVIDENCE_REF"));
        }
    }

    @Test public void compactWireRejectsEvidenceReferenceOutsideWireInventory() {
        try {
            new EditorialP5RawWireResponse(EditorialP5RawWireContract.SCHEMA_VERSION,
                    "a".repeat(64), "b".repeat(64),
                    List.of(new EditorialLedgerValidator.Entry("population:001", "PROCESSED",
                            List.of("evidence:missing"), true)), gates(),
                    List.of("evidence:raw"), List.of(), 0,
                    EditorialStopDecision.continueWithoutStop("L1", "COVERAGE", "LOCAL_REVIEW"), false);
            throw new AssertionError("orphan evidence reference must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_WIRE_EVIDENCE_REF_NOT_DECLARED"));
        }
    }

    @Test public void compactWireEnforcesFindingsPreservedAndDispositionLimits() {
        List<EditorialLedgerValidator.Entry> findings = List.of(
                new EditorialLedgerValidator.Entry("population:001", "NOT_EVALUATED",
                        List.of("evidence:raw"), false),
                new EditorialLedgerValidator.Entry("population:002", "NOT_EVALUATED",
                        List.of("evidence:raw"), false),
                new EditorialLedgerValidator.Entry("population:003", "NOT_EVALUATED",
                        List.of("evidence:raw"), false),
                new EditorialLedgerValidator.Entry("population:004", "NOT_EVALUATED",
                        List.of("evidence:raw"), false),
                new EditorialLedgerValidator.Entry("population:005", "NOT_EVALUATED",
                        List.of("evidence:raw"), false));
        try {
            new EditorialP5RawWireResponse(EditorialP5RawWireContract.SCHEMA_VERSION,
                    "a".repeat(64), "b".repeat(64), findings, gates(), List.of("evidence:raw"),
                    List.of(), 0, EditorialStopDecision.continueWithoutStop(
                            "L1", "COVERAGE", "LOCAL_REVIEW"), false);
            throw new AssertionError("finding limit must be enforced");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_WIRE_FINDINGS_LIMIT_EXCEEDED"));
        }

        try {
            new EditorialP5RawWireResponse(EditorialP5RawWireContract.SCHEMA_VERSION,
                    "a".repeat(64), "b".repeat(64), List.of(), gates(), List.of(),
                    List.of("preserved:1", "preserved:2", "preserved:3", "preserved:4", "preserved:5"),
                    0, EditorialStopDecision.continueWithoutStop(
                            "L1", "COVERAGE", "LOCAL_REVIEW"), false);
            throw new AssertionError("preserved inventory limit must be enforced");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_WIRE_PRESERVED_LIMIT_EXCEEDED"));
        }

        try {
            new EditorialP5RawWireResponse(EditorialP5RawWireContract.SCHEMA_VERSION,
                    "a".repeat(64), "b".repeat(64), List.of(), gates(), List.of(),
                    List.of("preserved:1", "preserved:1"), 0,
                    EditorialStopDecision.continueWithoutStop(
                            "L1", "COVERAGE", "LOCAL_REVIEW"), false);
            throw new AssertionError("duplicate preserved IDs must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_WIRE_DUPLICATE_PRESERVED_ITEM"));
        }

        List<String> fiveEvidenceRefs = List.of(
                "evidence:1", "evidence:2", "evidence:3", "evidence:4", "evidence:5");
        try {
            new EditorialP5RawWireResponse(EditorialP5RawWireContract.SCHEMA_VERSION,
                    "a".repeat(64), "b".repeat(64), List.of(), gates(), fiveEvidenceRefs,
                    List.of(), 0, EditorialStopDecision.contentBlocked(
                            "CONTENT_CONFLICT_PROVEN", "L1", EditorialSafe4Contract.GATE_IDS.get(0),
                            fiveEvidenceRefs, "scope", "review", "L1"), false);
            throw new AssertionError("disposition evidence limit must be enforced");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("RAW_WIRE_DISPOSITION_EVIDENCE_LIMIT_EXCEEDED"));
        }
    }

    @Test public void rawUtf8BomRoundTripsWithoutChangingAppOwnedBytes() {
        byte[] raw = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'R', 'A', 'W', '\n'};
        String attempt = "a".repeat(64);
        String envelope = "b".repeat(64);
        EditorialP5RawWireResponse wire = new EditorialP5RawWireResponse(
                EditorialP5RawWireContract.SCHEMA_VERSION, attempt, envelope,
                List.of(new EditorialLedgerValidator.Entry("population:001", "PROCESSED",
                        List.of("evidence:raw"), false)), gates(), List.of("evidence:raw"),
                List.of(), 0, EditorialStopDecision.continueWithoutStop(
                        "L1", "COVERAGE", "LOCAL_REVIEW"), false);

        EditorialP5L1Output output = wire.materialize(providerRequest(attempt, envelope, raw));

        assertTrue(Arrays.equals(raw, output.beforeText().getBytes(StandardCharsets.UTF_8)));
        assertEquals(output.beforeText(), output.afterText());
        assertTrue(output.declaredChanges().isEmpty());
    }

    @Test public void materializationRejectsMissingPopulationCoverage() {
        String attempt = "a".repeat(64);
        String envelope = "b".repeat(64);
        EditorialP5RawWireResponse wire = new EditorialP5RawWireResponse(
                EditorialP5RawWireContract.SCHEMA_VERSION, attempt, envelope,
                List.of(), gates(), List.of(), List.of(), 0,
                EditorialStopDecision.continueWithoutStop("L1", "COVERAGE", "LOCAL_REVIEW"), false);
        try {
            wire.materialize(providerRequest(attempt, envelope, bytes("raw")));
            throw new AssertionError("missing app-owned population coverage must stop materialization");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("LEDGER_UNACCOUNTED_ITEM"));
        }
    }

    @Test public void reconcileMaterializesExactDraftBytesNotRaw() {
        byte[] draft = "DRAFT exact bytes\n".getBytes(StandardCharsets.UTF_8);
        String attempt = "a".repeat(64);
        String envelope = "b".repeat(64);
        EditorialP5RawWireResponse wire = wire(attempt, envelope);
        EditorialP5PilotProvider.Request request = providerRequest(attempt, envelope,
                "L1_RECONCILE", Map.of(EditorialSafe4Contract.RAW, bytes("RAW other"),
                        EditorialSafe4Contract.DRAFT, draft));
        EditorialP5L1Output output = wire.materialize(request);
        assertEquals("DRAFT exact bytes\n", output.beforeText());
        assertEquals(output.beforeText(), output.afterText());
        assertTrue(Arrays.equals(draft, output.afterText().getBytes(StandardCharsets.UTF_8)));
        assertTrue(output.declaredChanges().isEmpty());
    }

    @Test public void materializeRejectsNonL1Phase() {
        String attempt = "a".repeat(64);
        String envelope = "b".repeat(64);
        try {
            wire(attempt, envelope).materialize(providerRequest(attempt, envelope, "L2_AUDIT",
                    Map.of(EditorialSafe4Contract.RAW, bytes("raw"))));
            throw new AssertionError("L2 phase must not materialize");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("L1 phase"));
        }
    }

    private static EditorialP5RawWireResponse wire(String attempt, String envelope) {
        return new EditorialP5RawWireResponse(
                EditorialP5RawWireContract.SCHEMA_VERSION, attempt, envelope,
                List.of(new EditorialLedgerValidator.Entry("population:001", "PROCESSED",
                        List.of("evidence:raw"), true)), gates(), List.of("evidence:raw"),
                List.of(), 0, EditorialStopDecision.continueWithoutStop(
                        "L1", "COVERAGE", "LOCAL_REVIEW"), true);
    }

    private static EditorialP5PilotProvider.Request providerRequest(String attempt,
            String envelope, String phase, Map<String, byte[]> sources) {
        return new EditorialP5PilotProvider.Request(attempt,
                EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC, "openrouter",
                "openai/gpt-5.6-luna", phase, envelope, sources,
                new EditorialP5PilotRequest.PackAuthority(Map.of()),
                EditorialP5RawWireContract.SCHEMA_VERSION, "001", "",
                new EditorialP5PilotProvider.Request.Context(
                        "binding-from-app", "run-from-app", "manifest-from-app", "bundle-from-app",
                        "predecessor-from-app", List.of("chapter:001"), List.of("population:001")));
    }

    private static EditorialP5PilotProvider.Request providerRequest(String attempt,
                                                                      String envelope,
                                                                      byte[] raw) {
        return new EditorialP5PilotProvider.Request(attempt,
                EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC, "openrouter",
                "openai/gpt-5.6-luna", "L1_RAW_DISCOVERY", envelope,
                Map.of(EditorialSafe4Contract.RAW, raw),
                new EditorialP5PilotRequest.PackAuthority(Map.of()),
                EditorialP5RawWireContract.SCHEMA_VERSION, "001", "",
                new EditorialP5PilotProvider.Request.Context(
                        "binding-from-app", "run-from-app", "manifest-from-app", "bundle-from-app",
                        "predecessor-from-app", List.of("chapter:001"), List.of("population:001")));
    }

    private static Map<String, String> gates() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) result.put(gate, "PASS");
        return result;
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
