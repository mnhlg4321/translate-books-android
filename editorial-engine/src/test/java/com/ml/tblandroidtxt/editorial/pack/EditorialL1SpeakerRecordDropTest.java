package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Synthetic matrix for W5: speakerRecords is an optional side note, so a record whose unit reference is not a unit
 * line is dropped and counted with its path, while everything else in the ledger stays strict.
 */
public final class EditorialL1SpeakerRecordDropTest {
    // physical lines: 1 text, 2 blank, 3 text, 4 blank, 5 text
    private static final String RAW = "raw 1\n\nraw 3\n\nraw 5";
    private static final String DRAFT = "draft 1\n\ndraft 3\n\ndraft 5";

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }

    private static Map<String, Object> speaker(Object unitId, String speaker) {
        return map("unitId", unitId, "speaker", speaker, "listener", "", "basis", "synthetic");
    }

    private static Map<String, Object> finding(int rawLine, String draftQuote) {
        return map("errorId", "E" + rawLine, "type", "MEANING", "severity", "MAJOR",
                "rawUnits", List.of("L" + rawLine),
                "draft", map("kind", "LINES", "start", BigDecimal.valueOf(rawLine), "end", BigDecimal.valueOf(rawLine),
                        "after", BigDecimal.ZERO),
                "rawQuote", "raw " + rawLine, "draftQuote", draftQuote,
                "observation", "observed", "expectedMeaning", "expected",
                "evidenceRefs", List.of(), "candidateIds", List.of(), "occurrenceUnits", List.of(),
                "disposition", "OPEN", "evidenceLimit", "");
    }

    private static EditorialRawInventory.Inventory inventory() {
        return EditorialRawInventory.build(RAW.getBytes(StandardCharsets.UTF_8));
    }

    private static EditorialL1Ledger.ReconcilePass parse(List<Object> findings, List<Object> speakers) {
        EditorialRawInventory.Inventory inventory = inventory();
        Map<String, Object> rawWire = map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE, "attemptIdentity", "raw",
                "coverage", List.of(map("from", "L1", "to", "L5", "status", "PROCESSED")), "candidates", List.of());
        EditorialL1Ledger.RawPass rawPass = EditorialL1Ledger.parseRawPass(json(rawWire), "raw", inventory);
        Map<String, Object> reconcile = map("wireSchemaVersion", EditorialL1Ledger.RECONCILE_WIRE,
                "attemptIdentity", "reconcile", "coverage", List.of(map("from", "L1", "to", "L5", "status", "PROCESSED")),
                "resolutions", List.of(), "findings", findings, "speakerRecords", speakers, "protectedSpans", List.of(),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE"));
        return EditorialL1Ledger.parseReconcile(json(reconcile), "reconcile", inventory,
                EditorialL1Ledger.draftLines(DRAFT.getBytes(StandardCharsets.UTF_8)), rawPass.candidates());
    }

    private static byte[] json(Map<String, Object> value) {
        return EditorialCanonicalJson.canonicalize(value).getBytes(StandardCharsets.UTF_8);
    }

    private static void expectCode(String code, String path, Runnable action) {
        try {
            action.run();
            fail("expected " + code + ":" + path);
        } catch (WireViolation invalid) {
            assertEquals(code, invalid.code());
            assertEquals(path, invalid.path());
        }
    }

    @Test public void validRecordsAreKeptAndNothingIsCounted() {
        EditorialL1Ledger.ReconcilePass pass = parse(List.of(), List.of(speaker("L1", "A"), speaker("L3", "B")));
        assertEquals(2, pass.speakerRecords().size());
        assertTrue(pass.speakerRecordsDropped().isEmpty());
    }

    @Test public void blankOutOfRangeAndMalformedReferencesAreDroppedWithTheirPaths() {
        EditorialL1Ledger.ReconcilePass pass = parse(List.of(), List.of(
                speaker("L1", "A"),      // 0 kept
                speaker("L2", "B"),      // 1 blank line
                speaker("L3", "C"),      // 2 kept
                speaker("L99", "D"),     // 3 beyond the text
                speaker("X7", "E"),      // 4 malformed
                speaker("L4", "F")));    // 5 blank line
        assertEquals(2, pass.speakerRecords().size());
        assertEquals("A", pass.speakerRecords().get(0).speaker());
        assertEquals("C", pass.speakerRecords().get(1).speaker());
        assertEquals(List.of("speakerRecords.1.unitId", "speakerRecords.3.unitId", "speakerRecords.4.unitId",
                "speakerRecords.5.unitId"), pass.speakerRecordsDropped());
    }

    @Test public void nonStringAndEmptyReferencesAreDroppedToo() {
        EditorialL1Ledger.ReconcilePass pass = parse(List.of(), List.of(speaker(BigDecimal.ONE, "A"), speaker("", "B")));
        assertTrue(pass.speakerRecords().isEmpty());
        assertEquals(2, pass.speakerRecordsDropped().size());
    }

    @Test public void aMissingUnitKeyIsStillAStructuralError() {
        Map<String, Object> missing = speaker("L1", "A");
        missing.remove("unitId");
        expectCode("L1_MISSING_KEY", "speakerRecords.0.unitId", () -> parse(List.of(), List.of(missing)));
    }

    @Test public void otherDefectsOfARecordStayFatal() {
        // a valid reference does not excuse an empty required field, an unknown key or a missing basis
        Map<String, Object> blankSpeaker = speaker("L1", "");
        expectCode("L1_TEXT_REQUIRED", "speakerRecords.0.speaker", () -> parse(List.of(), List.of(blankSpeaker)));
        Map<String, Object> extra = speaker("L1", "A");
        extra.put("surprise", "x");
        try {
            parse(List.of(), List.of(extra));
            fail("unknown key must be rejected");
        } catch (WireViolation invalid) {
            assertTrue(invalid.path(), invalid.path().startsWith("speakerRecords.0"));
        }
    }

    @Test public void findingsStayStrictWhileRecordsAreDropped() {
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", "findings.0.draftQuote",
                () -> parse(List.of(finding(1, "absent")), List.of(speaker("L2", "A"))));
        assertEquals(1, parse(List.of(finding(1, "draft 1")), List.of(speaker("L2", "A"))).findings().size());
    }

    @Test public void dropIsPersistedInTheReportBodyAndRestored() {
        EditorialL1Ledger.ReconcilePass pass = parse(List.of(), List.of(speaker("L1", "A"), speaker("L2", "B")));
        EditorialL1Ledger.RawPass rawPass = new EditorialL1Ledger.RawPass(
                List.of(new EditorialRawInventory.Range("L1", "L5", "PROCESSED")), List.of());
        Map<String, Object> artifact = EditorialL1Ledger.bodyToMap(EditorialL1Ledger.bodyOfReconcile(inventory(), rawPass, pass));
        @SuppressWarnings("unchecked") Map<String, Object> normalizations = (Map<String, Object>) artifact.get("normalizations");
        assertEquals(1, ((BigDecimal) normalizations.get("speakerRecordsDropped")).intValueExact());
        assertEquals(List.of("speakerRecords.1.unitId"), normalizations.get("speakerRecordsDroppedPaths"));
        EditorialL1Ledger.Body restored = EditorialL1Ledger.parseBody(artifact);
        assertEquals(List.of("speakerRecords.1.unitId"), restored.speakerRecordsDropped());
        assertEquals(1, restored.speakerRecords().size());
        assertEquals(EditorialContractRevision.L1_LEDGER_V9, artifact.get("contractRevision"));
    }

    @Test public void promptStatesThatSpeakerRecordsAreOptional() {
        String rules = EditorialFieldSpec.promptRules(EditorialFieldSpec.L1_RECONCILE);
        assertTrue(rules, rules.contains("speakerRecords array; items 0.."));
        assertTrue(rules, rules.contains("MAY be empty; an optional side note nothing depends on"));
    }
}
