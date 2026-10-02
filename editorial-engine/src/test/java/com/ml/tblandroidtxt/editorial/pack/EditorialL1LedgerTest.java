package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Ledger v2 wire, validation, metrics and report round-trip. Text is synthetic (no book content). */
public final class EditorialL1LedgerTest {
    private static final String ATT = "att-1";
    // 12 lines; lines 3, 6, 9 contain 踏破 (the same defect repeated); line 4 has 今回
    private static final String RAW = String.join("\n",
            "王は城に入った。", "騎士が言った。", "「踏破した。」", "「今回は無理だ。」", "彼女は笑った。", "「踏破だ。」",
            "雨が降る。", "彼は歩いた。", "「踏破完了。」", "空は暗い。", "三人が来た。", "終わり。");
    private static final String DRAFT = String.join("\n",
            "Vua vao thanh.", "Hiep si noi.", "\"Da chinh phuc.\"", "\"今回 khong the.\"", "Co ay cuoi.", "\"Chinh phuc roi.\"",
            "Troi mua.", "Anh di.", "\"Chinh phuc xong.\"", "Troi toi.", "Ba nguoi toi.", "Het.");

    private static EditorialRawInventory.Inventory inv() { return EditorialRawInventory.build(RAW.getBytes(StandardCharsets.UTF_8)); }
    private static List<String> draft() { return EditorialL1Ledger.draftLines(DRAFT.getBytes(StandardCharsets.UTF_8)); }
    private static String id(int line) { return inv().units().get(line - 1).id(); }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> range(int from, int to, String status) {
        return map("from", id(from), "to", id(to), "status", status);
    }

    private static List<Object> fullCoverage() { return new ArrayList<>(List.of(range(1, 12, "PROCESSED"))); }

    private static byte[] json(Map<String, Object> m) { return EditorialCanonicalJson.canonicalize(m).getBytes(StandardCharsets.UTF_8); }

    private static Map<String, Object> rawWire(List<Object> coverage, List<Object> candidates) {
        return map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE, "attemptIdentity", ATT, "coverage", coverage, "candidates", candidates);
    }

    private static Map<String, Object> candidate(String id, String ledger, int line) {
        return map("candidateId", id, "ledger", ledger, "unitId", id(line), "note", "n");
    }

    private static Map<String, Object> finding(String errorId, String type, int rawLine, int draftStart, int draftEnd,
                                               String rawQuote, String draftQuote) {
        return map("errorId", errorId, "type", type, "severity", "MAJOR", "rawUnits", new ArrayList<Object>(List.of(id(rawLine))),
                "draft", map("kind", "LINES", "start", BigDecimal.valueOf(draftStart), "end", BigDecimal.valueOf(draftEnd)),
                "rawQuote", rawQuote, "draftQuote", draftQuote, "observation", "obs", "expectedMeaning", "exp",
                "evidenceRefs", new ArrayList<Object>(), "candidateIds", new ArrayList<Object>(),
                "occurrenceUnits", new ArrayList<Object>(), "disposition", "OPEN", "evidenceLimit", "");
    }

    private static Map<String, Object> continueDisposition() {
        return map("disposition", "CONTINUE", "reasonCode", "L1_OK", "stopClass", "NONE");
    }

    private static Map<String, Object> reconcileWire(List<Object> findings, List<Object> resolutions) {
        return map("wireSchemaVersion", EditorialL1Ledger.RECONCILE_WIRE, "attemptIdentity", ATT, "coverage", fullCoverage(),
                "resolutions", resolutions, "findings", findings, "speakerRecords", new ArrayList<Object>(),
                "protectedSpans", new ArrayList<Object>(), "disposition", continueDisposition());
    }

    private static EditorialL1Ledger.ReconcilePass reconcile(Map<String, Object> wire, List<EditorialL1Ledger.Candidate> cands) {
        return EditorialL1Ledger.parseReconcile(json(wire), ATT, inv(), draft(), cands);
    }

    private static void expectCode(String code, Runnable r) {
        try {
            r.run();
            fail("expected " + code);
        } catch (IllegalArgumentException e) {
            assertEquals(code, e.getMessage());
        }
    }

    // ---- RAW pass ----

    @Test
    public void rawPassAcceptsCoverageAndSparseCandidates() {
        EditorialL1Ledger.RawPass pass = EditorialL1Ledger.parseRawPass(
                json(rawWire(fullCoverage(), new ArrayList<>(List.of(candidate("c1", "TG", 3), candidate("c2", "UNIT", 4))))), ATT, inv());
        assertEquals(2, pass.candidates().size());
        assertEquals(1, pass.coverage().size());
    }

    @Test
    public void rawPassRefusesGapOverlapUnknownAndForgedUnits() {
        expectCode("L1_COVERAGE_GAP", () -> EditorialL1Ledger.parseRawPass(
                json(rawWire(new ArrayList<>(List.of(range(1, 5, "PROCESSED"), range(7, 12, "PROCESSED"))), new ArrayList<>())), ATT, inv()));
        expectCode("L1_COVERAGE_OVERLAP", () -> EditorialL1Ledger.parseRawPass(
                json(rawWire(new ArrayList<>(List.of(range(1, 7, "PROCESSED"), range(7, 12, "PROCESSED"))), new ArrayList<>())), ATT, inv()));
        expectCode("L1_COVERAGE_EMPTY", () -> EditorialL1Ledger.parseRawPass(json(rawWire(new ArrayList<>(), new ArrayList<>())), ATT, inv()));
        Map<String, Object> forged = candidate("c1", "TG", 3);
        forged.put("unitId", "u:3:00000000");
        expectCode("L1_UNIT_UNKNOWN", () -> EditorialL1Ledger.parseRawPass(
                json(rawWire(fullCoverage(), new ArrayList<>(List.of(forged)))), ATT, inv()));
    }

    @Test
    public void rawPassRefusesDuplicateCandidateIdsWrongEchoAndOversize() {
        expectCode("L1_CANDIDATE_ID_DUPLICATE", () -> EditorialL1Ledger.parseRawPass(json(rawWire(fullCoverage(),
                new ArrayList<>(List.of(candidate("c1", "TG", 3), candidate("c1", "TG", 4))))), ATT, inv()));
        expectCode("L1_WIRE_ATTEMPT_ECHO_MISMATCH", () -> EditorialL1Ledger.parseRawPass(
                json(rawWire(fullCoverage(), new ArrayList<>())), "other", inv()));
        expectCode("L1_WIRE_BYTE_LIMIT_EXCEEDED", () -> EditorialL1Ledger.parseRawPass(
                new byte[EditorialL1Ledger.MAX_WIRE_BYTES + 1], ATT, inv()));
        List<Object> many = new ArrayList<>();
        for (int i = 0; i <= EditorialL1Ledger.MAX_CANDIDATES_PER_CALL; i++) many.add(candidate("c" + i, "UNIT", 1));
        expectCode("L1_CANDIDATE_LIMIT_EXCEEDED", () -> EditorialL1Ledger.parseRawPass(json(rawWire(fullCoverage(), many)), ATT, inv()));
    }

    // ---- RECONCILE: the R0 shapes ----

    @Test
    public void moreThanFourFindingsAreAllKept() {
        List<Object> findings = new ArrayList<>();
        findings.add(finding("e1", "UNTRANSLATED", 4, 4, 4, "今回", "今回"));
        findings.add(finding("e2", "MEANING", 3, 3, 3, "踏破", "chinh phuc"));
        findings.add(finding("e3", "MEANING", 6, 6, 6, "踏破", "Chinh phuc"));
        findings.add(finding("e4", "MEANING", 9, 9, 9, "踏破", "Chinh phuc"));
        findings.add(finding("e5", "STRUCTURE", 5, 5, 5, "笑った", "cuoi"));
        findings.add(finding("e6", "NUMBER", 11, 11, 11, "三人", "Ba nguoi"));
        EditorialL1Ledger.ReconcilePass pass = reconcile(reconcileWire(findings, new ArrayList<>()), List.of());
        assertEquals(6, pass.findings().size());
        assertEquals(6, EditorialL1Ledger.metrics(inv(), List.of(), pass).uniqueFindingCount());
    }

    @Test
    public void oneFindingWithManyOccurrencesCountsOnceAndOccurrencesSeparately() {
        Map<String, Object> f = finding("e1", "MEANING", 3, 3, 3, "踏破", "chinh phuc");
        f.put("occurrenceUnits", new ArrayList<Object>(List.of(id(6), id(9))));
        EditorialL1Ledger.ReconcilePass pass = reconcile(reconcileWire(new ArrayList<>(List.of(f)), new ArrayList<>()), List.of());
        EditorialL1Ledger.Metrics m = EditorialL1Ledger.metrics(inv(), List.of(), pass);
        assertEquals(1, m.uniqueFindingCount());
        assertEquals(3, m.occurrenceCount());
        assertEquals(1, m.openFindingCount());
        assertEquals(0, m.preservedFindingCount());
        assertEquals(12, m.unitCount());
    }

    @Test
    public void missingTargetUsesAMissingAnchorWithoutDraftQuote() {
        Map<String, Object> f = finding("e1", "OMISSION", 7, 6, 6, "雨が降る", "");
        f.put("draft", map("kind", "MISSING", "after", BigDecimal.valueOf(6)));
        EditorialL1Ledger.ReconcilePass pass = reconcile(reconcileWire(new ArrayList<>(List.of(f)), new ArrayList<>()), List.of());
        assertEquals("MISSING", pass.findings().get(0).draft().kind());
        f.put("draftQuote", "Troi mua");
        expectCode("L1_DRAFT_QUOTE_FORBIDDEN_FOR_MISSING", () -> reconcile(reconcileWire(new ArrayList<>(List.of(f)), new ArrayList<>()), List.of()));
    }

    @Test
    public void quotesMustOccurInTheAnchoredText() {
        expectCode("L1_RAW_QUOTE_NOT_IN_ANCHOR", () -> reconcile(reconcileWire(new ArrayList<>(List.of(
                finding("e1", "MEANING", 3, 3, 3, "攻略", "chinh phuc"))), new ArrayList<>()), List.of()));
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", () -> reconcile(reconcileWire(new ArrayList<>(List.of(
                finding("e1", "MEANING", 3, 3, 3, "踏破", "khong co"))), new ArrayList<>()), List.of()));
        // a draft quote that sits on another line than the anchor is not accepted
        expectCode("L1_DRAFT_QUOTE_NOT_IN_ANCHOR", () -> reconcile(reconcileWire(new ArrayList<>(List.of(
                finding("e1", "MEANING", 3, 4, 4, "踏破", "chinh phuc"))), new ArrayList<>()), List.of()));
        expectCode("L1_DRAFT_QUOTE_REQUIRED", () -> reconcile(reconcileWire(new ArrayList<>(List.of(
                finding("e1", "MEANING", 3, 3, 3, "踏破", ""))), new ArrayList<>()), List.of()));
    }

    @Test
    public void anchorsAndReferencesMustResolve() {
        expectCode("L1_DRAFT_ANCHOR_OUT_OF_RANGE", () -> reconcile(reconcileWire(new ArrayList<>(List.of(
                finding("e1", "MEANING", 3, 3, 13, "踏破", "chinh phuc"))), new ArrayList<>()), List.of()));
        Map<String, Object> forgedUnit = finding("e1", "MEANING", 3, 3, 3, "踏破", "chinh phuc");
        forgedUnit.put("rawUnits", new ArrayList<Object>(List.of("u:3:00000000")));
        expectCode("L1_UNIT_UNKNOWN", () -> reconcile(reconcileWire(new ArrayList<>(List.of(forgedUnit)), new ArrayList<>()), List.of()));
        Map<String, Object> forgedCandidate = finding("e1", "MEANING", 3, 3, 3, "踏破", "chinh phuc");
        forgedCandidate.put("candidateIds", new ArrayList<Object>(List.of("nope")));
        expectCode("L1_CANDIDATE_REF_UNKNOWN", () -> reconcile(reconcileWire(new ArrayList<>(List.of(forgedCandidate)), new ArrayList<>()), List.of()));
        expectCode("L1_ERROR_ID_DUPLICATE", () -> reconcile(reconcileWire(new ArrayList<>(List.of(
                finding("e1", "MEANING", 3, 3, 3, "踏破", "chinh phuc"),
                finding("e1", "MEANING", 6, 6, 6, "踏破", "Chinh phuc"))), new ArrayList<>()), List.of()));
        Map<String, Object> dupOcc = finding("e1", "MEANING", 3, 3, 3, "踏破", "chinh phuc");
        dupOcc.put("occurrenceUnits", new ArrayList<Object>(List.of(id(3))));
        expectCode("L1_OCCURRENCE_DUPLICATE", () -> reconcile(reconcileWire(new ArrayList<>(List.of(dupOcc)), new ArrayList<>()), List.of()));
    }

    @Test
    public void preservedFindingNeedsAnEvidenceLimitAndTypesAreClosed() {
        Map<String, Object> f = finding("e1", "MEANING", 3, 3, 3, "踏破", "chinh phuc");
        f.put("disposition", "PRESERVED");
        expectCode("L1_PRESERVED_NEEDS_EVIDENCE_LIMIT", () -> reconcile(reconcileWire(new ArrayList<>(List.of(f)), new ArrayList<>()), List.of()));
        f.put("evidenceLimit", "glossary silent");
        EditorialL1Ledger.ReconcilePass pass = reconcile(reconcileWire(new ArrayList<>(List.of(f)), new ArrayList<>()), List.of());
        assertEquals(1, EditorialL1Ledger.metrics(inv(), List.of(), pass).preservedFindingCount());
        Map<String, Object> badType = finding("e2", "VIBES", 3, 3, 3, "踏破", "chinh phuc");
        expectCode("L1_ENUM_INVALID", () -> reconcile(reconcileWire(new ArrayList<>(List.of(badType)), new ArrayList<>()), List.of()));
    }

    @Test
    public void everyCandidateMustBeResolvedAndResolutionsMustReferToKnownIds() {
        List<EditorialL1Ledger.Candidate> cands = List.of(new EditorialL1Ledger.Candidate("c1", "TG", id(3), "n"),
                new EditorialL1Ledger.Candidate("c2", "UNIT", id(4), "n"));
        Map<String, Object> f = finding("e1", "MEANING", 3, 3, 3, "踏破", "chinh phuc");
        f.put("candidateIds", new ArrayList<Object>(List.of("c1")));
        expectCode("L1_CANDIDATE_UNRESOLVED", () -> reconcile(reconcileWire(new ArrayList<>(List.of(f)), new ArrayList<>(List.of(
                map("candidateId", "c1", "status", "PROCESSED", "findingRef", "e1")))), cands));
        EditorialL1Ledger.ReconcilePass pass = reconcile(reconcileWire(new ArrayList<>(List.of(f)), new ArrayList<>(List.of(
                map("candidateId", "c1", "status", "PROCESSED", "findingRef", "e1"),
                map("candidateId", "c2", "status", "UNPROCESSED", "findingRef", "")))), cands);
        EditorialL1Ledger.Metrics m = EditorialL1Ledger.metrics(inv(), cands, pass);
        assertEquals(2, m.candidateCount());
        assertEquals(1, m.unprocessedCandidateCount());
        expectCode("L1_FINDING_REF_UNKNOWN", () -> reconcile(reconcileWire(new ArrayList<>(List.of(f)), new ArrayList<>(List.of(
                map("candidateId", "c1", "status", "PROCESSED", "findingRef", "zzz"),
                map("candidateId", "c2", "status", "PROCESSED", "findingRef", "")))), cands));
        expectCode("L1_RESOLUTION_ID_INVALID", () -> reconcile(reconcileWire(new ArrayList<>(List.of(f)), new ArrayList<>(List.of(
                map("candidateId", "c1", "status", "PROCESSED", "findingRef", "e1"),
                map("candidateId", "c1", "status", "PROCESSED", "findingRef", "e1"),
                map("candidateId", "c2", "status", "PROCESSED", "findingRef", "")))), cands));
    }

    @Test
    public void protectedSpansAndSpeakerRecordsAreRangeChecked() {
        Map<String, Object> wire = reconcileWire(new ArrayList<>(), new ArrayList<>());
        wire.put("protectedSpans", new ArrayList<Object>(List.of(map("spanId", "p1", "start", BigDecimal.valueOf(2),
                "end", BigDecimal.valueOf(3), "source", "PRONOUN_ROW", "reason", "profile row"))));
        wire.put("speakerRecords", new ArrayList<Object>(List.of(map("unitId", id(2), "speaker", "kỵ sĩ", "listener", "vua", "basis", "text"))));
        EditorialL1Ledger.ReconcilePass pass = reconcile(wire, List.of());
        assertEquals(1, pass.protectedSpans().size());
        assertEquals(1, pass.speakerRecords().size());
        wire.put("protectedSpans", new ArrayList<Object>(List.of(map("spanId", "p1", "start", BigDecimal.valueOf(2),
                "end", BigDecimal.valueOf(13), "source", "PRONOUN_ROW", "reason", "r"))));
        expectCode("L1_PROTECTED_RANGE_INVALID", () -> reconcile(wire, List.of()));
    }

    @Test
    public void unknownKeysAndForgedDispositionsAreRefused() {
        Map<String, Object> wire = reconcileWire(new ArrayList<>(), new ArrayList<>());
        wire.put("extra", "x");
        expectCode("L1_UNKNOWN_KEY", () -> reconcile(wire, List.of()));
        Map<String, Object> stop = reconcileWire(new ArrayList<>(), new ArrayList<>());
        stop.put("disposition", map("disposition", "CONTINUE", "reasonCode", "L1_OK", "stopClass", "CONTENT_BLOCKED"));
        expectCode("L1_DISPOSITION_STOP_CLASS_INVALID", () -> reconcile(stop, List.of()));
        Map<String, Object> pass = reconcileWire(new ArrayList<>(), new ArrayList<>());
        pass.put("disposition", map("disposition", "PASS", "reasonCode", "L1_OK", "stopClass", "NONE"));
        expectCode("L1_DISPOSITION_INVALID", () -> reconcile(pass, List.of()));
    }

    @Test
    public void typedStopMayCarryEmptyCoverage() {
        Map<String, Object> wire = reconcileWire(new ArrayList<>(), new ArrayList<>());
        wire.put("coverage", new ArrayList<Object>());
        wire.put("disposition", map("disposition", "STOP", "reasonCode", "L1_INPUT_UNUSABLE", "stopClass", "INPUT_REQUIRED"));
        assertEquals("STOP", reconcile(wire, List.of()).disposition().kind());
    }

    // ---- report body round trip ----

    @Test
    public void reportBodyRoundTripsThroughCanonicalJsonWithoutLoss() {
        List<EditorialL1Ledger.Candidate> cands = List.of(new EditorialL1Ledger.Candidate("c1", "TG", id(3), "n"));
        Map<String, Object> f = finding("e1", "MEANING", 3, 3, 3, "踏破", "chinh phuc");
        f.put("candidateIds", new ArrayList<Object>(List.of("c1")));
        f.put("occurrenceUnits", new ArrayList<Object>(List.of(id(6), id(9))));
        f.put("evidenceRefs", new ArrayList<Object>(List.of("gl-1")));
        Map<String, Object> wire = reconcileWire(new ArrayList<>(List.of(f)), new ArrayList<>(List.of(
                map("candidateId", "c1", "status", "PROCESSED", "findingRef", "e1"))));
        wire.put("protectedSpans", new ArrayList<Object>(List.of(map("spanId", "p1", "start", BigDecimal.valueOf(3),
                "end", BigDecimal.valueOf(3), "source", "L1_PROOF", "reason", "proof"))));
        EditorialL1Ledger.ReconcilePass pass = reconcile(wire, cands);
        EditorialL1Ledger.RawPass raw = new EditorialL1Ledger.RawPass(List.of(new EditorialRawInventory.Range(id(1), id(12), "PROCESSED")), cands);
        EditorialL1Ledger.Body body = EditorialL1Ledger.bodyOfReconcile(inv(), raw, pass);
        byte[] bytes = EditorialCanonicalJson.canonicalize(EditorialL1Ledger.bodyToMap(body)).getBytes(StandardCharsets.UTF_8);
        EditorialL1Ledger.Body back = EditorialL1Ledger.parseBody(EditorialCanonicalJson.parseObject(bytes));
        assertEquals(body, back);
        byte[] again = EditorialCanonicalJson.canonicalize(EditorialL1Ledger.bodyToMap(back)).getBytes(StandardCharsets.UTF_8);
        assertTrue(java.util.Arrays.equals(bytes, again));
        assertEquals(EditorialContractRevision.L1_LEDGER_V2, EditorialContractRevision.ofReportBytes(bytes));
        assertEquals(1, back.metrics().uniqueFindingCount());
        assertEquals(3, back.metrics().occurrenceCount());
    }

    @Test
    public void legacyReportIsNotALedgerBody() {
        Map<String, Object> legacy = map("phase", "L1_RECONCILE", "artifactType", "REPORT_L1");
        assertEquals(EditorialContractRevision.LEGACY_V1, EditorialContractRevision.ofReport(legacy));
        expectCode("L1_REPORT_NOT_LEDGER_V2", () -> EditorialL1Ledger.parseBody(legacy));
        assertFalse(EditorialContractRevision.eligiblePredecessor(EditorialContractRevision.LEGACY_V1, EditorialContractRevision.L1_LEDGER_V2));
        assertTrue(EditorialContractRevision.eligiblePredecessor(EditorialContractRevision.L1_LEDGER_V2, EditorialContractRevision.L1_LEDGER_V2));
        assertEquals(EditorialContractRevision.LEGACY_V1, EditorialContractRevision.ofReportBytes("not json".getBytes(StandardCharsets.UTF_8)));
        assertEquals("", EditorialContractRevision.identitySuffix(EditorialContractRevision.LEGACY_V1));
    }

    // ---- sizing ----

    @Test
    public void heavyRealisticCallFitsTheByteCapAndTheCapIsBoundedByTheTokenCap() {
        // the maximum number of findings, each with long two-byte-per-character text
        String text = "á".repeat(160);
        int total = 0;
        for (int i = 0; i < EditorialL1Ledger.MAX_FINDINGS_PER_CALL; i++) {
            Map<String, Object> f = finding("e" + i, "MEANING", 3, 3, 3, "踏破", "chinh phuc");
            f.put("observation", text);
            f.put("expectedMeaning", text);
            total += EditorialCanonicalJson.canonicalize(f).getBytes(StandardCharsets.UTF_8).length;
        }
        assertTrue("findings " + total, total < EditorialL1Ledger.MAX_WIRE_BYTES - 8_192);
        // 16,384 output tokens at <= 4 bytes per token can never exceed the wire cap
        assertTrue(EditorialL1Ledger.MAX_WIRE_BYTES >= 16_384 * 4);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void responseSchemaRequiresExactlyTheKeysTheParserAccepts() {
        Map<String, Object> raw = EditorialL1Ledger.jsonSchema(true);
        assertEquals(java.util.Set.of("wireSchemaVersion", "attemptIdentity", "coverage", "candidates"),
                new java.util.HashSet<>((List<Object>) raw.get("required")));
        Map<String, Object> rec = EditorialL1Ledger.jsonSchema(false);
        Map<String, Object> props = (Map<String, Object>) rec.get("properties");
        assertEquals(new java.util.HashSet<>(reconcileWire(new ArrayList<>(), new ArrayList<>()).keySet()),
                new java.util.HashSet<>((List<Object>) rec.get("required")));
        assertEquals(props.keySet(), new java.util.LinkedHashSet<>((List<Object>) rec.get("required")));
        Map<String, Object> findingItem = (Map<String, Object>) ((Map<String, Object>) props.get("findings")).get("items");
        assertEquals(new java.util.HashSet<>(finding("e1", "MEANING", 3, 3, 3, "踏破", "chinh phuc").keySet()),
                new java.util.HashSet<>((List<Object>) findingItem.get("required")));
        // no union types: a strict response schema cannot express them
        assertFalse(EditorialCanonicalJson.canonicalize(rec).contains("oneOf"));
        assertFalse(EditorialCanonicalJson.canonicalize(rec).contains("anyOf"));
    }

    @Test
    public void anchorShapeIsUniformAndUnusedNumbersMustBeZero() {
        Map<String, Object> lines = finding("e1", "MEANING", 3, 3, 3, "踏破", "chinh phuc");
        lines.put("draft", map("kind", "LINES", "start", BigDecimal.valueOf(3), "end", BigDecimal.valueOf(3), "after", BigDecimal.ZERO));
        assertEquals(1, reconcile(reconcileWire(new ArrayList<>(List.of(lines)), new ArrayList<>()), List.of()).findings().size());
        lines.put("draft", map("kind", "LINES", "start", BigDecimal.valueOf(3), "end", BigDecimal.valueOf(3), "after", BigDecimal.valueOf(2)));
        expectCode("L1_DRAFT_ANCHOR_UNUSED_FIELD", () -> reconcile(reconcileWire(new ArrayList<>(List.of(lines)), new ArrayList<>()), List.of()));
        Map<String, Object> missing = finding("e1", "OMISSION", 7, 6, 6, "雨が降る", "");
        missing.put("draft", map("kind", "MISSING", "start", BigDecimal.ZERO, "end", BigDecimal.ZERO, "after", BigDecimal.valueOf(6)));
        assertEquals("MISSING", reconcile(reconcileWire(new ArrayList<>(List.of(missing)), new ArrayList<>()), List.of()).findings().get(0).draft().kind());
        missing.put("draft", map("kind", "MISSING", "start", BigDecimal.valueOf(2), "end", BigDecimal.ZERO, "after", BigDecimal.valueOf(6)));
        expectCode("L1_DRAFT_ANCHOR_UNUSED_FIELD", () -> reconcile(reconcileWire(new ArrayList<>(List.of(missing)), new ArrayList<>()), List.of()));
    }

    @Test
    public void typedParseFailureMessageIsAllowListed() {
        assertEquals("L1_UNIT_UNKNOWN", EditorialL1Ledger.safeMessage(new IllegalArgumentException("L1_UNIT_UNKNOWN")));
        assertEquals("L1_WIRE_PARSE_FAILED", EditorialL1Ledger.safeMessage(new IllegalArgumentException("contains 王は城 text")));
        assertEquals("L1_WIRE_PARSE_FAILED", EditorialL1Ledger.safeMessage(new IllegalArgumentException((String) null)));
    }
}
