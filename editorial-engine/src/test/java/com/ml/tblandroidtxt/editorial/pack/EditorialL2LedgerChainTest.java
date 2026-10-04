package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** L2 under the ledger contract: every L1 finding is answered and checked by the app. Text is synthetic. */
public final class EditorialL2LedgerChainTest {
    private static final byte[] PROJECT = bytes("project authority");
    private static final byte[] PROMPT = bytes("prompt authority");
    private static final byte[] WORKFLOW = bytes("workflow authority");
    private static final String L1_ID = "l1-attempt-identity-1";
    private static final String RAW = String.join("\n", "王は城に入った。", "騎士が言った。", "「踏《とう》破した。」", "「今回は無理だ。」",
            "彼女は笑った。", "「踏破だ。」", "雨が降る。", "彼は歩いた。", "「踏破完了。」", "空は暗い。");
    private static final String DRAFT = String.join("\n", "Vua vao thanh.", "Hiep si noi.", "\"Da chinh phuc.\"",
            "\"今回 khong the.\"", "Co ay cuoi.", "\"Chinh phuc roi.\"", "Troi mua.", "Anh di.", "\"Chinh phuc xong.\"", "Troi toi.");
    private static final EditorialL2Execution.Budget BUDGET = new EditorialL2Execution.Budget(
            100_000, 4_000, new BigDecimal("0.50"), 60_000L);

    // ---- builders ----

    private static EditorialRawInventory.Inventory inv() { return EditorialRawInventory.build(bytes(RAW)); }

    private static String unit(int line) { return inv().units().get(line - 1).id(); }

    private static EditorialL1Ledger.Finding finding(String id, int rawLine, int draftStart, int draftEnd, String rawQuote,
                                                     String draftQuote, List<String> occurrences, String disposition) {
        return new EditorialL1Ledger.Finding(id, "MEANING", "MAJOR", List.of(unit(rawLine)),
                EditorialL1Ledger.DraftAnchor.lines(draftStart, draftEnd), rawQuote, draftQuote, "obs", "exp", List.of(),
                List.of(), occurrences, disposition, "PRESERVED".equals(disposition) ? "limit" : "");
    }

    private static EditorialL1Ledger.Finding missing(String id, int rawLine, int after, String rawQuote) {
        return new EditorialL1Ledger.Finding(id, "OMISSION", "MAJOR", List.of(unit(rawLine)),
                EditorialL1Ledger.DraftAnchor.missingAfter(after), rawQuote, "", "obs", "exp", List.of(), List.of(), List.of(),
                "OPEN", "");
    }

    private static byte[] reportOf(EditorialP5PilotRequest context, String raw, List<EditorialL1Ledger.Finding> findings,
                                   List<EditorialL1Ledger.ProtectedSpan> spans) {
        EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(bytes(raw));
        List<EditorialRawInventory.Range> coverage = List.of(new EditorialRawInventory.Range(
                inventory.units().get(0).id(), inventory.units().get(inventory.units().size() - 1).id(), "PROCESSED"));
        EditorialL1Ledger.RawPass rawPass = new EditorialL1Ledger.RawPass(coverage, List.of());
        EditorialL1Ledger.ReconcilePass pass = new EditorialL1Ledger.ReconcilePass(coverage, List.of(), findings, List.of(), spans,
                new EditorialL1Ledger.Disposition("CONTINUE", "L1_OK", "NONE"));
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", EditorialContractRevision.REPORT_SCHEMA_V2);
        report.put("artifactType", "REPORT_L1");
        report.put("bindingIdentity", context.binding().bindingIdentity());
        report.put("canonicalPackHash", context.binding().canonicalPackHash());
        report.put("manifestFingerprint", context.manifestFingerprint());
        report.put("chapterKey", context.chapterKey());
        report.put("bundleIdentity", context.bundleIdentity());
        report.put("disposition", "CONTINUE");
        for (Map.Entry<String, Object> e : EditorialL1Ledger.bodyToMap(EditorialL1Ledger.bodyOfReconcile(inventory, rawPass, pass)).entrySet()) {
            report.put(e.getKey(), e.getValue());
        }
        report.put("phase", "L1_RECONCILE");
        return json(report);
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static byte[] json(Map<String, Object> m) { return EditorialCanonicalJson.canonicalize(wireView(m)).getBytes(StandardCharsets.UTF_8); }

    private static Map<String, Object> change(String id, String errorId, String op, int line, String before, String after) {
        Map<String, Object> m = map("changeId", id, "errorId", errorId, "line", BigDecimal.valueOf(line), "before", before,
                "after", after, "reason", "fix", "dialogue", Boolean.FALSE, "status", "CLOSED");
        if (op != null) m.put("op", op);
        return m;
    }

    private static Map<String, Object> resolution(String errorId, String status, List<Object> changeIds, List<Object> preserveIds,
                                                  List<Object> occurrences, String quote, String reason) {
        return map("errorId", errorId, "status", status, "changeIds", changeIds, "preserveIds", preserveIds,
                "occurrences", occurrences, "evidenceQuote", quote, "reason", reason);
    }

    private static Map<String, Object> occ(int rawLine, String ref) { return map("unitId", unit(rawLine), "ref", ref); }

    private static List<Object> list(Object... v) { return new ArrayList<>(List.of(v)); }

    /** Scripted provider: discovery, edit and final read by phase; the read echoes whatever it was handed. */
    private static final class Script implements EditorialL2Execution.Provider {
        final Function<String, byte[]> editFor;
        final List<Request> requests = new ArrayList<>();
        final Map<String, String> readOverride = new HashMap<>();
        BigDecimal cost = new BigDecimal("0.01");
        List<Map<String, Object>> readDefects = new ArrayList<>();

        Script(Function<String, byte[]> editFor) { this.editFor = editFor; }

        @Override public Response call(Request request) {
            requests.add(request);
            byte[] body;
            switch (request.phase()) {
                case EditorialL2Execution.DISCOVERY_PHASE -> body = json(map(
                        "wireSchemaVersion", EditorialL2Execution.DISCOVERY_WIRE_V3, "attemptIdentity", request.attemptIdentity(),
                        "coverage", list(map("from", unit(1), "to", unit(10), "status", "PROCESSED")),
                        "candidates", list(map("candidateId", "U001", "ledger", "UNIT", "unitId", unit(1), "note", "n"))));
                case EditorialL2Execution.PHASE -> body = editFor.apply(request.attemptIdentity());
                default -> body = readWire(request);
            }
            return new Response(body, "stop", true, 100, 50, cost, true);
        }

        private byte[] readWire(Request request) {
            byte[] target = request.visibleSources().get(EditorialFinalRead.TARGET_ROLE);
            List<String> lines = EditorialFinalRead.lines(target);
            List<Object> tails = new ArrayList<>();
            for (Integer line : EditorialFinalRead.probeLines(target)) {
                String text = lines.get(line - 1);
                tails.add(map("line", BigDecimal.valueOf(line), "tail", text.length() <= 12 ? text : text.substring(text.length() - 12)));
            }
            Map<String, Object> wire = map("wireSchemaVersion", EditorialFinalRead.WIRE, "attemptIdentity", request.attemptIdentity(),
                    "readSha256", EditorialCanonicalJson.sha256Hex(target), "probeTails", tails,
                    "verdict", readDefects.isEmpty() ? "CLEAN" : "DEFECTS", "defects", new ArrayList<Object>(readDefects));
            if (readOverride.containsKey("hash")) wire.put("readSha256", readOverride.get("hash"));
            return json(wire);
        }
    }

    private static byte[] editWire(String attempt, List<Object> changes, List<Object> preserved, List<Object> findingResolutions) {
        return json(map("wireSchemaVersion", EditorialL2Execution.WIRE_SCHEMA_VERSION_V3, "attemptIdentity", attempt,
                "resolutions", list(map("candidateId", "U001", "status", "PROCESSED")),
                "findingResolutions", findingResolutions, "changes", changes, "preserved", preserved,
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE")));
    }

    private static final class Ledger {
        final EditorialP5PilotRequest context;
        final byte[] report;

        Ledger(List<EditorialL1Ledger.Finding> findings, List<EditorialL1Ledger.ProtectedSpan> spans) {
            this.context = fixture(RAW, DRAFT).withContractRevision(EditorialContractRevision.CURRENT_LEDGER);
            this.report = reportOf(context, RAW, findings, spans);
        }

        EditorialL2Execution.Request request() {
            return new EditorialL2Execution.Request(context, L1_ID, report, Set.of());
        }
    }

    private static EditorialL2Execution.Result run(Ledger l, Script provider, Store store) {
        return new EditorialL2Execution().execute(l.request(), BUDGET, BUDGET, BUDGET, provider, store);
    }

    private static List<EditorialL1Ledger.Finding> threeFindings() {
        return List.of(
                finding("e1", 3, 3, 3, "踏破", "chinh phuc", List.of(unit(6), unit(9)), "OPEN"),
                finding("e2", 4, 4, 4, "今回", "今回", List.of(), "OPEN"),
                missing("e3", 7, 6, "雨が降る"));
    }

    private static Script happyScript(Ledger[] holder) {
        return new Script(attempt -> editWire(attempt,
                list(change("C1", "e1", null, 3, "\"Da chinh phuc.\"", "\"Da vuot qua.\""),
                        change("C2", "e1", null, 6, "\"Chinh phuc roi.\"", "\"Vuot qua roi.\""),
                        change("C3", "e1", null, 9, "\"Chinh phuc xong.\"", "\"Vuot qua xong.\""),
                        change("C4", "e2", null, 4, "\"今回 khong the.\"", "\"Lan nay khong the.\""),
                        change("C5", "e3", "INSERT_AFTER", 6, "\"Chinh phuc roi.\"", "Troi bat dau mua.")),
                list(),
                list(resolution("e1", "FIXED", list("C1", "C2", "C3"), list(), list(occ(6, "C2"), occ(9, "C3")), "", ""),
                        resolution("e2", "FIXED", list("C4"), list(), list(), "", ""),
                        resolution("e3", "FIXED", list("C5"), list(), list(), "", ""))));
    }

    // ---- tests ----

    @Test public void everyFindingFixedWithAllOccurrencesCommitsWithThreeCallsAndEvidence() {
        Ledger l = new Ledger(threeFindings(), List.of());
        Script provider = happyScript(null);
        Store store = new Store();
        EditorialL2Execution.Result r = run(l, provider, store);
        assertEquals(r.reasonCode() + r.issues(), EditorialL2Execution.Outcome.COMMITTED, r.outcome());
        assertEquals(3, r.providerCalls());
        assertEquals(3, provider.requests.size());
        assertEquals(EditorialFinalRead.L2_PHASE, provider.requests.get(2).phase());
        assertEquals(EditorialL2Execution.DISCOVERY_WIRE_V3, provider.requests.get(0).outputSchemaId());
        String vi = new String(r.committed().viL2Bytes(), StandardCharsets.UTF_8);
        assertTrue(vi, vi.contains("\"Da vuot qua.\"\n"));
        assertTrue(vi.contains("Troi bat dau mua."));
        assertFalse(vi.contains("Chinh phuc"));
        Map<String, Object> map = EditorialCanonicalJson.parseObject(r.committed().changeMapBytes());
        @SuppressWarnings("unchecked") Map<String, Object> discovery = (Map<String, Object>) map.get("discoveryCoverage");
        assertEquals(10, ((Number) discovery.get("unitCount")).intValue());
        assertEquals(1, ((List<?>) discovery.get("ranges")).size());
        @SuppressWarnings("unchecked") Map<String, Object> l1 = (Map<String, Object>) map.get("l1Resolution");
        @SuppressWarnings("unchecked") Map<String, Object> counts = (Map<String, Object>) l1.get("counts");
        assertEquals(3, ((Number) counts.get("FIXED")).intValue());
        @SuppressWarnings("unchecked") Map<String, Object> read = (Map<String, Object>) map.get("finalRead");
        assertEquals("CLEAN", read.get("verdict"));
        assertEquals(EditorialCanonicalJson.sha256Hex(r.committed().viL2Bytes()), read.get("targetSha256"));
        // the read call received exactly the built bytes and only RAW, GLOSSARY, the target and the probe block
        EditorialL2Execution.Provider.Request readRequest = provider.requests.get(2);
        assertArrayEquals(r.committed().viL2Bytes(), readRequest.visibleSources().get(EditorialFinalRead.TARGET_ROLE));
        assertEquals(Set.of("RAW", "GLOSSARY", EditorialFinalRead.TARGET_ROLE, EditorialFinalRead.PROBES_ROLE),
                readRequest.visibleSources().keySet());
        // the edit call sees the ledger report and the v2 schema
        assertEquals(EditorialL2Execution.WIRE_SCHEMA_VERSION_V3, provider.requests.get(1).outputSchemaId());
        assertTrue(provider.requests.get(1).visibleSources().containsKey("REPORT_L1"));
        // the contract revision is part of the attempt identity
        EditorialP5PilotRequest legacyContext = l.context.withContractRevision(EditorialContractRevision.LEGACY_V1);
        assertFalse(l.request().attemptIdentity().equals(
                new EditorialL2Execution.Request(legacyContext, L1_ID, l.report, Set.of()).attemptIdentity()));
    }

    @Test public void duplicateFindingReferenceIdsAreNormalizedAndCountedInChangeMap() {
        Ledger l = new Ledger(threeFindings(), List.of());
        Script provider = new Script(attempt -> editWire(attempt,
                list(change("C1", "e1", null, 3, "\"Da chinh phuc.\"", "\"Da vuot qua.\""),
                        change("C2", "e1", null, 6, "\"Chinh phuc roi.\"", "\"Vuot qua roi.\""),
                        change("C3", "e1", null, 9, "\"Chinh phuc xong.\"", "\"Vuot qua xong.\""),
                        change("C4", "e2", null, 4, "\"今回 khong the.\"", "\"Lan nay khong the.\""),
                        change("C5", "e3", "INSERT_AFTER", 6, "\"Chinh phuc roi.\"", "Troi bat dau mua.")),
                list(),
                list(resolution("e1", "FIXED", list("C1", "C1", "C2", "C3"), list(), list(occ(6, "C2"), occ(9, "C3")), "", ""),
                        resolution("e2", "FIXED", list("C4"), list(), list(), "", ""),
                        resolution("e3", "FIXED", list("C5"), list(), list(), "", ""))));
        EditorialL2Execution.Result r = run(l, provider, new Store());
        assertEquals(r.reasonCode() + r.issues(), EditorialL2Execution.Outcome.COMMITTED, r.outcome());
        Map<String, Object> map = EditorialCanonicalJson.parseObject(r.committed().changeMapBytes());
        @SuppressWarnings("unchecked") Map<String, Object> normalizations = (Map<String, Object>) map.get("normalizations");
        assertEquals(1, ((BigDecimal) normalizations.get("duplicateReferencesRemoved")).intValueExact());
        @SuppressWarnings("unchecked") Map<String, Object> l1Resolution = (Map<String, Object>) map.get("l1Resolution");
        @SuppressWarnings("unchecked") List<Map<String, Object>> findings = (List<Map<String, Object>>) l1Resolution.get("findings");
        assertEquals(List.of("C1", "C2", "C3"), findings.get(0).get("changeIds"));
    }

    @Test public void ledgerEditDerivesBeforeAndPersistsOnlyMismatchWarnings() {
        Ledger l = new Ledger(List.of(finding("e1", 3, 3, 3, "踏破", "\"Da chinh phuc.\"", List.of(), "OPEN")), List.of());
        Map<String, Object> omitted = change("C1", "e1", null, 3, "ignored", "\"Da vuot qua.\"");
        omitted.remove("before");
        Script omittedProvider = new Script(attempt -> editWire(attempt, list(omitted), list(),
                list(resolution("e1", "FIXED", list("C1"), list(), list(), "", ""))));
        EditorialL2Execution.Result derived = run(l, omittedProvider, new Store());
        assertEquals(derived.reasonCode() + derived.issues(), EditorialL2Execution.Outcome.COMMITTED, derived.outcome());
        assertTrue(new String(derived.committed().viL2Bytes(), StandardCharsets.UTF_8).contains("\"Da vuot qua.\""));
        assertFalse(EditorialCanonicalJson.parseObject(derived.committed().changeMapBytes()).containsKey("wireWarnings"));

        Map<String, Object> mismatch = change("C1", "e1", null, 3, "not a source substring", "\"Da vuot qua.\"");
        Script mismatchProvider = new Script(attempt -> editWire(attempt, list(mismatch), list(),
                list(resolution("e1", "FIXED", list("C1"), list(), list(), "", ""))));
        EditorialL2Execution.Result warned = run(l, mismatchProvider, new Store());
        assertEquals(warned.reasonCode() + warned.issues(), EditorialL2Execution.Outcome.COMMITTED, warned.outcome());
        assertEquals(List.of("CHANGE_BEFORE_SUBSTRING_MISMATCH:C1"),
                EditorialCanonicalJson.parseObject(warned.committed().changeMapBytes()).get("wireWarnings"));
    }

    @Test public void anOccurrenceWithoutAChangeOrPreservedRowIsRefused() {
        Ledger l = new Ledger(List.of(finding("e1", 3, 3, 3, "踏破", "chinh phuc", List.of(unit(6), unit(9)), "OPEN")), List.of());
        Script provider = new Script(a -> editWire(a,
                list(change("C1", "e1", null, 3, "\"Da chinh phuc.\"", "\"Da vuot qua.\"")), list(),
                list(resolution("e1", "FIXED", list("C1"), list(), list(occ(6, "C1")), "", ""))));
        EditorialL2Execution.Result r = run(l, provider, new Store());
        assertEquals("REPAIR_L2_FINDING_RESOLUTION_INVALID", r.reasonCode());
        assertTrue(r.issues().toString(), r.issues().contains("L2_FINDING_OCCURRENCE_MISSING:e1:" + unit(9)));
        assertEquals(2, r.providerCalls());
    }

    @Test public void aClaimedFixThatIsNotThereIsRefused() {
        Ledger l = new Ledger(List.of(finding("e1", 3, 3, 3, "踏破", "chinh phuc", List.of(), "OPEN")), List.of());
        // FIXED without a change
        EditorialL2Execution.Result none = run(l, new Script(a -> editWire(a, list(), list(),
                list(resolution("e1", "FIXED", list(), list(), list(), "", "")))), new Store());
        assertTrue(none.issues().toString(), none.issues().contains("L2_FINDING_FIXED_WITHOUT_CHANGE:e1"));
        // the change belongs to another finding id
        EditorialL2Execution.Result wrongId = run(l, new Script(a -> editWire(a,
                list(change("C1", "L2-x", null, 3, "\"Da chinh phuc.\"", "\"Da vuot qua.\"")), list(),
                list(resolution("e1", "FIXED", list("C1"), list(), list(), "", "")))), new Store());
        assertTrue(wrongId.issues().toString(), wrongId.issues().contains("L2_FINDING_CHANGE_ERROR_MISMATCH:e1:C1"));
        // the change is far from the anchor
        EditorialL2Execution.Result off = run(l, new Script(a -> editWire(a,
                list(change("C1", "e1", null, 8, "Anh di.", "Anh chay.")), list(),
                list(resolution("e1", "FIXED", list("C1"), list(), list(), "", "")))), new Store());
        assertTrue(off.issues().toString(), off.issues().contains("L2_FINDING_FIX_OFF_ANCHOR:e1"));
        // a change with an unknown error id
        EditorialL2Execution.Result unknown = run(l, new Script(a -> editWire(a,
                list(change("C1", "zzz", null, 3, "\"Da chinh phuc.\"", "\"Da vuot qua.\"")), list(),
                list(resolution("e1", "REJECTED", list(), list(), list(), "踏破", "raw says so")))), new Store());
        assertTrue(unknown.issues().toString(), unknown.issues().contains("L2_CHANGE_ERROR_ID_UNKNOWN:C1"));
        // a finding left out
        EditorialL2Execution.Result left = run(l, new Script(a -> editWire(a, list(), list(), list())), new Store());
        assertTrue(left.issues().toString(), left.issues().contains("L2_FINDING_NOT_RESOLVED:e1"));
    }

    @Test public void rejectionNeedsARawQuoteAndNoChangeAndPreservationNeedsARowOnTheAnchor() {
        Ledger l = new Ledger(List.of(finding("e1", 3, 3, 3, "踏破", "chinh phuc", List.of(), "OPEN")), List.of());
        EditorialL2Execution.Result noQuote = run(l, new Script(a -> editWire(a, list(), list(),
                list(resolution("e1", "REJECTED", list(), list(), list(), "", "fine as is")))), new Store());
        assertTrue(noQuote.issues().toString(), noQuote.issues().contains("L2_FINDING_REJECTED_WITHOUT_RAW_EVIDENCE:e1"));
        EditorialL2Execution.Result forged = run(l, new Script(a -> editWire(a, list(), list(),
                list(resolution("e1", "REJECTED", list(), list(), list(), "not in raw", "fine as is")))), new Store());
        assertTrue(forged.issues().toString(), forged.issues().contains("L2_FINDING_REJECTED_WITHOUT_RAW_EVIDENCE:e1"));
        Store ok = new Store();
        EditorialL2Execution.Result rejected = run(l, new Script(a -> editWire(a, list(), list(),
                list(resolution("e1", "REJECTED", list(), list(), list(), " 踏破 ", "the draft verb matches the RAW sense")))), ok);
        assertEquals(rejected.reasonCode() + rejected.issues(), EditorialL2Execution.Outcome.COMMITTED, rejected.outcome());
        assertArrayEquals(bytes(DRAFT), rejected.committed().viL2Bytes());

        Map<String, Object> keep = map("preserveId", "P1", "line", BigDecimal.valueOf(3), "before", "\"Da chinh phuc.\"", "evidenceLimit", "glossary silent");
        EditorialL2Execution.Result preserved = run(l, new Script(a -> editWire(a, list(), list(keep),
                list(resolution("e1", "PRESERVED", list(), list("P1"), list(), "", "")))), new Store());
        assertEquals(preserved.reasonCode() + preserved.issues(), EditorialL2Execution.Outcome.COMMITTED, preserved.outcome());
        Map<String, Object> offKeep = map("preserveId", "P1", "line", BigDecimal.valueOf(8), "before", "Anh di.", "evidenceLimit", "x");
        EditorialL2Execution.Result off = run(l, new Script(a -> editWire(a, list(), list(offKeep),
                list(resolution("e1", "PRESERVED", list(), list("P1"), list(), "", "")))), new Store());
        assertTrue(off.issues().toString(), off.issues().contains("L2_FINDING_PRESERVE_OFF_ANCHOR:e1:P1"));
        EditorialL2Execution.Result missing = run(l, new Script(a -> editWire(a, list(), list(),
                list(resolution("e1", "PRESERVED", list(), list(), list(), "", "")))), new Store());
        assertTrue(missing.issues().toString(), missing.issues().contains("L2_FINDING_PRESERVED_WITHOUT_ROW:e1"));
    }

    @Test public void anUnresolvedFindingIsATypedContentStopWithoutCommit() {
        Ledger l = new Ledger(List.of(finding("e1", 3, 3, 3, "踏破", "chinh phuc", List.of(), "OPEN")), List.of());
        Store store = new Store();
        EditorialL2Execution.Result r = run(l, new Script(a -> editWire(a, list(), list(),
                list(resolution("e1", "UNRESOLVED", list(), list(), list(), "", "cannot tell")))), store);
        assertEquals(EditorialL2Execution.StopClass.CONTENT_BLOCKED, r.stopClass());
        assertEquals("CONTENT_L2_FINDING_UNRESOLVED", r.reasonCode());
        assertEquals(List.of("L2_UNRESOLVED_FINDING:e1"), r.issues());
        assertTrue(store.committed.isEmpty());
        assertEquals(2, r.providerCalls());
    }

    @Test public void protectedLinesFromTheReportRevertAChangeAndLeaveTheFindingOpen() {
        // line 3 is protected by the report; a PRESERVED-disposition finding may sit there, but a fix may not touch it
        EditorialL1Ledger.Finding f = finding("e1", 3, 3, 3, "踏破", "chinh phuc", List.of(), "PRESERVED");
        Ledger l = new Ledger(List.of(f), List.of(new EditorialL1Ledger.ProtectedSpan("p1", 3, 3, "PRONOUN_ROW", "row")));
        Store store = new Store();
        EditorialL2Execution.Result r = run(l, new Script(a -> editWire(a,
                list(change("C1", "e1", null, 3, "\"Da chinh phuc.\"", "\"Da vuot qua.\"")), list(),
                list(resolution("e1", "FIXED", list("C1"), list(), list(), "", "")))), store);
        assertEquals("CONTENT_L2_FINDING_UNRESOLVED", r.reasonCode());
        assertTrue(store.committed.isEmpty());
    }

    @Test public void legacyAndLedgerReportsAreNeverMixedAcrossContracts() {
        Ledger l = new Ledger(threeFindings(), List.of());
        byte[] legacyReport = json(map("artifactType", "REPORT_L1", "schemaVersion", EditorialP5RawWireContract.FINAL_REPORT_SCHEMA,
                "phase", "L1_RECONCILE", "bindingIdentity", l.context.binding().bindingIdentity(),
                "canonicalPackHash", l.context.binding().canonicalPackHash(), "manifestFingerprint", l.context.manifestFingerprint(),
                "chapterKey", l.context.chapterKey(), "bundleIdentity", l.context.bundleIdentity(), "disposition", "CONTINUE"));
        Script provider = happyScript(null);
        EditorialL2Execution.Result legacyInLedgerChain = new EditorialL2Execution().execute(
                new EditorialL2Execution.Request(l.context, L1_ID, legacyReport, Set.of()), BUDGET, BUDGET, BUDGET, provider, new Store());
        assertEquals("INPUT_REPORT_L1_LEGACY_CONTRACT", legacyInLedgerChain.reasonCode());
        EditorialP5PilotRequest legacyContext = l.context.withContractRevision(EditorialContractRevision.LEGACY_V1);
        EditorialL2Execution.Result ledgerInLegacyChain = new EditorialL2Execution().execute(
                new EditorialL2Execution.Request(legacyContext, L1_ID, l.report, Set.of()), BUDGET, BUDGET, BUDGET, provider, new Store());
        assertEquals("INPUT_REPORT_L1_CONTRACT_MISMATCH", ledgerInLegacyChain.reasonCode());
        assertTrue(provider.requests.isEmpty());
    }

    @Test public void aReportBuiltForAnotherRawIsStale() {
        Ledger l = new Ledger(threeFindings(), List.of());
        byte[] stale = reportOf(l.context, RAW + "\n追加。", List.of(), List.of());
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(
                new EditorialL2Execution.Request(l.context, L1_ID, stale, Set.of()), BUDGET, BUDGET, BUDGET, new Script(a -> new byte[0]), new Store());
        assertEquals("INPUT_REPORT_L1_LEDGER_STALE", r.reasonCode());
    }

    @Test public void theLedgerChainNeedsAFinalReadBudgetBeforeAnyCall() {
        Ledger l = new Ledger(threeFindings(), List.of());
        Script provider = happyScript(null);
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(l.request(), BUDGET, BUDGET, provider, new Store());
        assertEquals(EditorialL2Execution.StopClass.AUTHORIZATION_REQUIRED, r.stopClass());
        assertEquals("L2_FINAL_READ_BUDGET_REQUIRED", r.reasonCode());
        assertTrue(provider.requests.isEmpty());
    }

    @Test public void aFinalReadThatDoesNotEchoTheBytesIsRefusedAndDefectsAreCarriedAsEvidence() {
        Ledger l = new Ledger(threeFindings(), List.of());
        Script bad = happyScript(null);
        bad.readOverride.put("hash", "0".repeat(64));
        Store store = new Store();
        EditorialL2Execution.Result r = run(l, bad, store);
        assertEquals("REPAIR_L2_FINAL_READ_INVALID", r.reasonCode());
        assertEquals(List.of("FINAL_READ_HASH_ECHO_MISMATCH:readSha256"), r.issues());
        assertEquals(3, r.providerCalls());
        assertTrue(store.committed.isEmpty());

        Script defects = happyScript(null);
        defects.readDefects.add(map("line", BigDecimal.valueOf(5), "quote", "Co ay", "type", "ADDRESS_PROFILE", "note", "check call"));
        EditorialL2Execution.Result carried = run(l, defects, new Store());
        assertEquals(carried.reasonCode() + carried.issues(), EditorialL2Execution.Outcome.COMMITTED, carried.outcome());
        Map<String, Object> map = EditorialCanonicalJson.parseObject(carried.committed().changeMapBytes());
        @SuppressWarnings("unchecked") Map<String, Object> read = (Map<String, Object>) map.get("finalRead");
        assertEquals("DEFECTS", read.get("verdict"));
        assertEquals(1, ((List<?>) read.get("defects")).size());
    }

    @Test public void restartIsIdempotentAndTheLegacyChainIsUntouched() {
        Ledger l = new Ledger(threeFindings(), List.of());
        Store store = new Store();
        EditorialL2Execution.Result first = run(l, happyScript(null), store);
        Script none = new Script(a -> new byte[0]);
        EditorialL2Execution.Result again = run(l, none, store);
        assertEquals(EditorialL2Execution.Outcome.ALREADY_COMMITTED, again.outcome());
        assertEquals(0, none.requests.size());
        assertArrayEquals(first.committed().viL2Bytes(), again.committed().viL2Bytes());
    }

    // ---- L3 on a ledger chain (v2 wires, anchored probes, carried defects, final read of FINAL) ----

    private static final EditorialL2Execution.Budget READ_BUDGET = new EditorialL2Execution.Budget(
            100_000, 4_000, new BigDecimal("0.50"), 60_000L);

    private static Map<String, Object> probe(String id, String kind, int rawLine, int viLine, String rawQuote, String viQuote,
                                             String verdict, String action) {
        return map("probeId", id, "kind", kind, "rawUnits", list(unit(rawLine)), "viStart", BigDecimal.valueOf(viLine),
                "viEnd", BigDecimal.valueOf(viLine), "scope", "checked this unit", "contrast", "compared with the glossary",
                "rawQuote", rawQuote, "viQuote", viQuote, "verdict", verdict, "action", action);
    }

    /** Six anchored probes over six different units of the happy-path VI_L2. */
    private static List<Object> goodProbes() {
        return list(
                probe("P1", "COVERAGE", 1, 1, "王は城", "Vua vao", "NO_DEFECT", "NONE"),
                probe("P2", "COVERAGE", 2, 2, "騎士が", "Hiep si", "NO_DEFECT", "NONE"),
                probe("P3", "COVERAGE", 3, 3, "踏破", " vuot qua ", "NO_DEFECT", "NONE"),
                probe("P4", "REGRESSION", 4, 4, "今回", "Lan nay", "NO_DEFECT", "NONE"),
                probe("P5", "REGRESSION", 6, 6, "踏破", "Vuot qua roi", "NO_DEFECT", "NONE"),
                probe("P6", "REGRESSION", 7, 7, "雨が降る", "bat dau mua", "NO_DEFECT", "NONE"));
    }

    private static byte[] reauditWire(String attempt) {
        return json(map("wireSchemaVersion", EditorialL3Execution.REAUDIT_WIRE_V3, "attemptIdentity", attempt,
                "coverage", list(map("from", unit(1), "to", unit(10), "status", "PROCESSED")),
                "candidates", list(map("candidateId", "R001", "ledger", "TG", "unitId", unit(3), "viLine", BigDecimal.valueOf(3),
                        "status", "PROCESSED", "note", "contrast"))));
    }

    private static Map<String, Object> reconcileV2(List<Object> changes, List<Object> carried, List<Object> preserved, List<Object> probes) {
        return map("wireSchemaVersion", EditorialL3Execution.RECONCILE_WIRE_V3, "attemptIdentity", "x",
                "resolutions", list(map("candidateId", "R001", "status", "PROCESSED")), "carriedResolutions", carried,
                "changes", changes, "preserved", preserved, "probes", probes,
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE"));
    }

    private static byte[] readEcho(EditorialL2Execution.Provider.Request r, String hashOverride, List<Map<String, Object>> defects) {
        byte[] target = r.visibleSources().get(EditorialFinalRead.TARGET_ROLE);
        List<String> lines = EditorialFinalRead.lines(target);
        List<Object> tails = new ArrayList<>();
        for (Integer line : EditorialFinalRead.probeLines(target)) {
            String text = lines.get(line - 1);
            tails.add(map("line", BigDecimal.valueOf(line), "tail", text.length() <= 12 ? text : text.substring(text.length() - 12)));
        }
        return json(map("wireSchemaVersion", EditorialFinalRead.WIRE, "attemptIdentity", r.attemptIdentity(),
                "readSha256", hashOverride != null ? hashOverride : EditorialCanonicalJson.sha256Hex(target), "probeTails", tails,
                "verdict", defects.isEmpty() ? "CLEAN" : "DEFECTS", "defects", new ArrayList<Object>(defects)));
    }

    private static EditorialL3Execution.Result runL3(Ledger l, EditorialL2Execution.Committed l2, Map<String, Object> reconcileWire,
                                                     String readHash, List<Map<String, Object>> readDefects, Store store) {
        EditorialL3Execution.Request request = new EditorialL3Execution.Request(l.context, L1_ID, l.report, l2, Set.of());
        EditorialL2Execution.Provider provider = r -> {
            byte[] body;
            if (EditorialL3Execution.REAUDIT_PHASE.equals(r.phase())) {
                body = reauditWire(r.attemptIdentity());
            } else if (EditorialL3Execution.RECONCILE_PHASE.equals(r.phase())) {
                Map<String, Object> wire = new LinkedHashMap<>(reconcileWire);
                wire.put("attemptIdentity", r.attemptIdentity());
                body = json(wire);
            } else {
                body = readEcho(r, readHash, readDefects);
            }
            return new EditorialL2Execution.Provider.Response(body, "stop", true, 100, 50, new BigDecimal("0.01"), true);
        };
        return new EditorialL3Execution().execute(request, BUDGET, BUDGET, READ_BUDGET, provider, store);
    }

    private static EditorialL3Execution.Result runL3(Ledger l, EditorialL2Execution.Committed l2, Map<String, Object> reconcileWire) {
        return runL3(l, l2, reconcileWire, null, List.of(), new Store());
    }

    private static EditorialL2Execution.Committed committedL2(Ledger l, Script script) {
        EditorialL2Execution.Result l2 = run(l, script, new Store());
        assertEquals(l2.reasonCode() + l2.issues(), EditorialL2Execution.Outcome.COMMITTED, l2.outcome());
        return l2.committed();
    }

    @Test public void l3AccountsForStructuralL2StagesAndCarriesProtectedLinesThroughTheLineMap() {
        // DRAFT line 7 is protected; L2 inserts a line after line 6, so it becomes VI_L2 line 8
        Ledger l = new Ledger(threeFindings(), List.of(new EditorialL1Ledger.ProtectedSpan("p1", 7, 7, "PRONOUN_ROW", "row")));
        EditorialL2Execution.Committed l2 = committedL2(l, happyScript(null));
        String vi = new String(l2.viL2Bytes(), StandardCharsets.UTF_8);
        assertEquals("Troi mua.", vi.split("\n", -1)[7]);

        // no L3 change: FINAL is VI_L2, every stage replays on hashes, the protected line arrived unchanged
        Store cleanStore = new Store();
        EditorialL3Execution.Result clean = runL3(l, l2, reconcileV2(list(), list(), list(), goodProbes()), null, List.of(), cleanStore);
        assertEquals(clean.reasonCode() + clean.issues(), EditorialL3Execution.Outcome.COMMITTED, clean.outcome());
        assertEquals(3, clean.providerCalls());
        assertEquals(0, clean.releaseNumbers().unaccountedChangedAnchors());
        assertEquals(0, clean.releaseNumbers().protectedSpanRegressions());
        assertArrayEquals(l2.viL2Bytes(), clean.committed().viL2Bytes());
        // the receipt is backed by recorded operations and passes the validator on the exact FINAL bytes
        EditorialQaReceiptValidator.Result receipt = EditorialQaReceiptValidator.validate(
                clean.committed().changeMapBytes(), clean.committed().viL2Bytes());
        assertTrue(receipt.issues().toString(), receipt.valid());
        assertFalse(receipt.legacy());

        // an L3 edit of the remapped protected line (VI_L2 line 8) is reverted; the inserted line 7 may change
        EditorialL3Execution.Result guarded = runL3(l, l2, reconcileV2(list(
                change("Q1", "L3-1", null, 8, "Troi mua.", "Troi mua to."),
                change("Q2", "L3-2", null, 7, "Troi bat dau mua.", "Troi bat dau mua nho.")), list(), list(), goodProbes()));
        assertEquals(guarded.reasonCode() + guarded.issues(), EditorialL3Execution.Outcome.COMMITTED, guarded.outcome());
        String fin = new String(guarded.committed().viL2Bytes(), StandardCharsets.UTF_8);
        assertEquals("Troi mua.", fin.split("\n", -1)[7]);
        assertEquals("Troi bat dau mua nho.", fin.split("\n", -1)[6]);
        assertEquals(0, guarded.releaseNumbers().protectedSpanRegressions());
        assertEquals(0, guarded.releaseNumbers().unaccountedChangedAnchors());
        assertTrue(new String(guarded.committed().changeMapBytes(), StandardCharsets.UTF_8).contains("PROTECTED_SPAN_TOUCHED"));
    }

    @Test public void l3DerivesBeforeAndStoresMismatchWarningsInTheQaReceipt() {
        Ledger l = new Ledger(threeFindings(), List.of());
        EditorialL2Execution.Committed l2 = committedL2(l, happyScript(null));

        Map<String, Object> omitted = change("Q1", "L3-1", null, 1, "ignored", "Vua buoc vao thanh.");
        omitted.remove("before");
        List<Object> derivedProbes = goodProbes();
        derivedProbes.set(0, probe("P1", "COVERAGE", 1, 1, "王は城", "Vua vao", "DEFECT_FOUND", "CHANGE:Q1"));
        EditorialL3Execution.Result derived = runL3(l, l2, reconcileV2(list(omitted), list(), list(), derivedProbes));
        assertEquals(derived.reasonCode() + derived.issues(), EditorialL3Execution.Outcome.COMMITTED, derived.outcome());
        assertFalse(EditorialCanonicalJson.parseObject(derived.committed().changeMapBytes()).containsKey("wireWarnings"));

        Map<String, Object> mismatch = change("Q1", "L3-1", null, 1, "not a source substring", "Vua buoc vao thanh.");
        List<Object> mismatchProbes = goodProbes();
        mismatchProbes.set(0, probe("P1", "COVERAGE", 1, 1, "王は城", "Vua vao", "DEFECT_FOUND", "CHANGE:Q1"));
        EditorialL3Execution.Result warned = runL3(l, l2, reconcileV2(list(mismatch), list(), list(), mismatchProbes));
        assertEquals(warned.reasonCode() + warned.issues(), EditorialL3Execution.Outcome.COMMITTED, warned.outcome());
        Map<String, Object> receipt = EditorialCanonicalJson.parseObject(warned.committed().changeMapBytes());
        assertEquals(List.of("CHANGE_BEFORE_SUBSTRING_MISMATCH:Q1"), receipt.get("wireWarnings"));
    }

    @Test public void probesMustBeAnchoredQuotedDistinctAndBroad() {
        Ledger l = new Ledger(threeFindings(), List.of());
        EditorialL2Execution.Committed l2 = committedL2(l, happyScript(null));
        // a generic "no defect" without an anchor
        Map<String, Object> unanchored = probe("P1", "COVERAGE", 1, 1, "王は城", "Vua vao", "NO_DEFECT", "NONE");
        unanchored.put("rawUnits", list());
        List<Object> probes = goodProbes();
        probes.set(0, unanchored);
        EditorialL3Execution.Result r = runL3(l, l2, reconcileV2(list(), list(), list(), probes));
        assertEquals("REPAIR_L3_PROBES_INVALID", r.reasonCode());
        assertTrue(r.issues().toString(), r.issues().contains("L3_PROBE_RAW_ANCHOR_REQUIRED:P1"));

        probes = goodProbes();
        probes.set(1, probe("P2", "COVERAGE", 2, 2, "not in raw", "Hiep si", "NO_DEFECT", "NONE"));
        assertTrue(runL3(l, l2, reconcileV2(list(), list(), list(), probes)).issues().contains("L3_PROBE_RAW_QUOTE_NOT_IN_ANCHOR:P2"));

        probes = goodProbes();
        probes.set(2, probe("P3", "COVERAGE", 3, 3, "踏破", "not in vi", "NO_DEFECT", "NONE"));
        assertTrue(runL3(l, l2, reconcileV2(list(), list(), list(), probes)).issues().contains("L3_PROBE_VI_QUOTE_NOT_IN_ANCHOR:P3"));

        probes = goodProbes();
        probes.set(3, probe("P4", "REGRESSION", 4, 99, "今回", "Lan nay", "NO_DEFECT", "NONE"));
        assertTrue(runL3(l, l2, reconcileV2(list(), list(), list(), probes)).issues().contains("L3_PROBE_VI_ANCHOR_OUT_OF_RANGE:P4"));

        probes = goodProbes();
        probes.set(4, probe("P5", "REGRESSION", 1, 1, "王は城", "Vua vao", "NO_DEFECT", "NONE"));   // same anchor as P1
        assertTrue(runL3(l, l2, reconcileV2(list(), list(), list(), probes)).issues().contains("L3_PROBE_ANCHOR_DUPLICATE:P5"));

        EditorialL3Execution.Result few = runL3(l, l2, reconcileV2(list(), list(), list(), goodProbes().subList(0, 4)));
        assertTrue(few.issues().toString(), few.issues().contains("L3_PROBES_REGRESSION_TOO_FEW"));

        // an action must match the verdict and name a real applied change near the anchor
        probes = goodProbes();
        probes.set(0, probe("P1", "COVERAGE", 1, 1, "王は城", "Vua vao", "DEFECT_FOUND", "NONE"));
        assertTrue(runL3(l, l2, reconcileV2(list(), list(), list(), probes)).issues().contains("L3_PROBE_ACTION_CHANGE_UNKNOWN:P1"));
        probes = goodProbes();
        probes.set(0, probe("P1", "COVERAGE", 1, 1, "王は城", "Vua vao", "NO_DEFECT", "CHANGE:Q1"));
        assertTrue(runL3(l, l2, reconcileV2(list(), list(), list(), probes)).issues().contains("L3_PROBE_ACTION_INVALID:P1"));

        // a probe that found a defect and fixed it on its anchor is accepted
        probes = goodProbes();
        probes.set(0, probe("P1", "COVERAGE", 1, 1, "王は城", "Vua vao", "DEFECT_FOUND", "CHANGE:Q1"));
        EditorialL3Execution.Result fixed = runL3(l, l2, reconcileV2(list(change("Q1", "L3-1", null, 1, "Vua vao thanh.", "Vua buoc vao thanh.")),
                list(), list(), probes));
        assertEquals(fixed.reasonCode() + fixed.issues(), EditorialL3Execution.Outcome.COMMITTED, fixed.outcome());
        assertTrue(new String(fixed.committed().viL2Bytes(), StandardCharsets.UTF_8).startsWith("Vua buoc vao thanh."));
    }

    @Test public void aConflictProbeIsATypedContentStop() {
        Ledger l = new Ledger(threeFindings(), List.of());
        EditorialL2Execution.Committed l2 = committedL2(l, happyScript(null));
        List<Object> probes = goodProbes();
        probes.set(0, probe("P1", "COVERAGE", 1, 1, "王は城", "Vua vao", "CONFLICT", "NONE"));
        EditorialL3Execution.Result r = runL3(l, l2, reconcileV2(list(), list(), list(), probes));
        assertEquals("CONTENT_L3_PROVEN_CONFLICT_UNRESOLVED", r.reasonCode());
        assertEquals(EditorialL2Execution.StopClass.CONTENT_BLOCKED, r.stopClass());
    }

    @Test public void l2FinalReadDefectsAreCarriedAndEveryOneMustBeAnswered() {
        Ledger l = new Ledger(threeFindings(), List.of());
        Script script = happyScript(null);
        script.readDefects.add(map("line", BigDecimal.valueOf(5), "quote", "Co ay", "type", "ADDRESS_PROFILE", "note", "check call"));
        EditorialL2Execution.Committed l2 = committedL2(l, script);
        // not answered at all
        EditorialL3Execution.Result none = runL3(l, l2, reconcileV2(list(), list(), list(), goodProbes()));
        assertEquals("REPAIR_L3_CARRIED_RESOLUTION_INVALID", none.reasonCode());
        assertTrue(none.issues().toString(), none.issues().contains("L3_CARRIED_NOT_RESOLVED:0"));
        // the model asks to see the carried block
        // fixed on the line
        EditorialL3Execution.Result fixed = runL3(l, l2, reconcileV2(list(change("Q1", "L3-1", null, 5, "Co ay cuoi.", "Co gai cuoi.")),
                list(map("index", BigDecimal.ZERO, "status", "FIXED", "changeIds", list("Q1"), "preserveIds", list(),
                        "evidenceQuote", "", "reason", "")), list(), goodProbes()));
        assertEquals(fixed.reasonCode() + fixed.issues(), EditorialL3Execution.Outcome.COMMITTED, fixed.outcome());
        // rejected needs a quote of that very line
        EditorialL3Execution.Result badReject = runL3(l, l2, reconcileV2(list(), list(map("index", BigDecimal.ZERO, "status", "REJECTED",
                "changeIds", list(), "preserveIds", list(), "evidenceQuote", "not there", "reason", "fine")), list(), goodProbes()));
        assertTrue(badReject.issues().toString(), badReject.issues().contains("L3_CARRIED_REJECTED_WITHOUT_EVIDENCE:0"));
        EditorialL3Execution.Result reject = runL3(l, l2, reconcileV2(list(), list(map("index", BigDecimal.ZERO, "status", "REJECTED",
                "changeIds", list(), "preserveIds", list(), "evidenceQuote", " Co ay ", "reason", "profile row says so")), list(), goodProbes()));
        assertEquals(reject.reasonCode() + reject.issues(), EditorialL3Execution.Outcome.COMMITTED, reject.outcome());
        // unresolved stops the chapter
        EditorialL3Execution.Result open = runL3(l, l2, reconcileV2(list(), list(map("index", BigDecimal.ZERO, "status", "UNRESOLVED",
                "changeIds", list(), "preserveIds", list(), "evidenceQuote", "", "reason", "cannot tell")), list(), goodProbes()));
        assertEquals("CONTENT_L3_CARRIED_DEFECT_UNRESOLVED", open.reasonCode());
        // an unknown index is a schema error
        EditorialL3Execution.Result unknownIndex = runL3(l, l2, reconcileV2(list(), list(map("index", BigDecimal.valueOf(7), "status", "UNRESOLVED",
                "changeIds", list(), "preserveIds", list(), "evidenceQuote", "", "reason", "x")), list(), goodProbes()));
        assertEquals("REPAIR_L3_RECONCILE_SCHEMA_INVALID", unknownIndex.reasonCode());
    }

    @Test public void theFinalReadOfFinalMustEchoTheBytesAndAReadWithDefectsIsNotReleased() {
        Ledger l = new Ledger(threeFindings(), List.of());
        EditorialL2Execution.Committed l2 = committedL2(l, happyScript(null));
        Store store = new Store();
        EditorialL3Execution.Result bad = runL3(l, l2, reconcileV2(list(), list(), list(), goodProbes()), "0".repeat(64), List.of(), store);
        assertEquals("REPAIR_L3_FINAL_READ_INVALID", bad.reasonCode());
        assertEquals(List.of("FINAL_READ_HASH_ECHO_MISMATCH:readSha256"), bad.issues());
        assertTrue(store.committed.isEmpty());

        Store store2 = new Store();
        EditorialL3Execution.Result defects = runL3(l, l2, reconcileV2(list(), list(), list(), goodProbes()), null,
                List.of(map("line", BigDecimal.valueOf(9), "quote", "Anh di", "type", "MEANING", "note", "n")), store2);
        assertEquals("CONTENT_L3_FINAL_READ_DEFECTS", defects.reasonCode());
        assertEquals(List.of("L3_FINAL_READ_DEFECT:9:MEANING"), defects.issues());
        assertEquals(3, defects.providerCalls());
        assertTrue(store2.committed.isEmpty());
    }

    @Test public void theLedgerChainNeedsAFinalReadBudgetOnL3Too() {
        Ledger l = new Ledger(threeFindings(), List.of());
        EditorialL2Execution.Committed l2 = committedL2(l, happyScript(null));
        EditorialL3Execution.Result r = new EditorialL3Execution().execute(
                new EditorialL3Execution.Request(l.context, L1_ID, l.report, l2, Set.of()), BUDGET, BUDGET,
                x -> { throw new AssertionError("no call expected"); }, new Store());
        assertEquals(EditorialL2Execution.StopClass.AUTHORIZATION_REQUIRED, r.stopClass());
        assertEquals("L3_FINAL_READ_BUDGET_REQUIRED", r.reasonCode());
    }

    @Test public void theReceiptValidatorRefusesClaimsWithoutOperationsAndAcceptsLegacyAsUnverified() {
        Ledger l = new Ledger(threeFindings(), List.of());
        EditorialL2Execution.Committed l2 = committedL2(l, happyScript(null));
        EditorialL3Execution.Result ok = runL3(l, l2, reconcileV2(list(), list(), list(), goodProbes()));
        byte[] fin = ok.committed().viL2Bytes();
        Map<String, Object> receipt = EditorialCanonicalJson.parseObject(ok.committed().changeMapBytes());
        assertTrue(EditorialQaReceiptValidator.validate(json(receipt), fin).valid());

        Map<String, Object> noOps = new LinkedHashMap<>(receipt);
        noOps.put("operations", list());
        assertTrue(EditorialQaReceiptValidator.validate(json(noOps), fin).issues().contains("RECEIPT_FINAL_READ_WITHOUT_OPERATION"));
        Map<String, Object> staticMarker = new LinkedHashMap<>(receipt);
        staticMarker.put("finalReadOrder", list("L3_RAW_FIRST_REAUDIT", "APP_RELEASE_NUMBERS"));
        assertTrue(EditorialQaReceiptValidator.validate(json(staticMarker), fin).issues().contains("RECEIPT_STATIC_FINAL_READ_MARKER"));
        assertTrue(EditorialQaReceiptValidator.validate(json(receipt), bytes("another text")).issues().contains("RECEIPT_FINAL_HASH_MISMATCH"));
        Map<String, Object> wrongTarget = new LinkedHashMap<>(receipt);
        @SuppressWarnings("unchecked") Map<String, Object> read = new LinkedHashMap<>((Map<String, Object>) receipt.get("finalRead"));
        read.put("targetSha256", "0".repeat(64));
        wrongTarget.put("finalRead", read);
        assertTrue(EditorialQaReceiptValidator.validate(json(wrongTarget), fin).issues().contains("RECEIPT_FINAL_READ_TARGET_MISMATCH"));
        Map<String, Object> unanchored = new LinkedHashMap<>(receipt);
        unanchored.put("probes", list(map("probeId", "P1", "kind", "COVERAGE", "rawUnits", list(), "viStart", BigDecimal.ZERO,
                "viEnd", BigDecimal.ZERO, "scope", "", "contrast", "", "rawQuote", "", "viQuote", "", "verdict", "NO_DEFECT", "action", "NONE")));
        EditorialQaReceiptValidator.Result thin = EditorialQaReceiptValidator.validate(json(unanchored), fin);
        assertTrue(thin.issues().toString(), thin.issues().contains("RECEIPT_PROBE_UNANCHORED"));
        assertTrue(thin.issues().contains("RECEIPT_PROBES_TOO_FEW"));
        Map<String, Object> badNumbers = new LinkedHashMap<>(receipt);
        @SuppressWarnings("unchecked") Map<String, Object> numbers = new LinkedHashMap<>((Map<String, Object>) receipt.get("releaseNumbers"));
        numbers.put("unaccountedChangedAnchors", BigDecimal.ONE);
        badNumbers.put("releaseNumbers", numbers);
        assertTrue(EditorialQaReceiptValidator.validate(json(badNumbers), fin).issues().contains("RECEIPT_RELEASE_NUMBER_NOT_ZERO:unaccountedChangedAnchors"));

        // a receipt written before the revision existed is readable but never evidence of a read
        EditorialQaReceiptValidator.Result legacy = EditorialQaReceiptValidator.validate(
                json(map("schemaVersion", EditorialL3Execution.QA_RECEIPT_SCHEMA, "artifactType", "QA_RECEIPT",
                        "finalReadOrder", list("L3_RAW_FIRST_REAUDIT"))), fin);
        assertTrue(legacy.valid());
        assertTrue(legacy.legacy());
        assertFalse(EditorialQaReceiptValidator.validate(bytes("not json"), fin).valid());
    }

    @Test public void l3RefusesALegacyReportOnALedgerChainAndBindsTheRevisionIntoItsIdentity() {
        Ledger l = new Ledger(threeFindings(), List.of());
        EditorialL2Execution.Committed l2 = committedL2(l, happyScript(null));
        byte[] legacyReport = json(map("artifactType", "REPORT_L1", "schemaVersion", EditorialP5RawWireContract.FINAL_REPORT_SCHEMA,
                "phase", "L1_RECONCILE", "bindingIdentity", l.context.binding().bindingIdentity(),
                "canonicalPackHash", l.context.binding().canonicalPackHash(), "manifestFingerprint", l.context.manifestFingerprint(),
                "chapterKey", l.context.chapterKey(), "bundleIdentity", l.context.bundleIdentity(), "disposition", "CONTINUE"));
        EditorialL3Execution.Result refused = new EditorialL3Execution().execute(
                new EditorialL3Execution.Request(l.context, L1_ID, legacyReport, l2, Set.of()), BUDGET, BUDGET, READ_BUDGET,
                r -> { throw new AssertionError("no call expected"); }, new Store());
        assertEquals("INPUT_REPORT_L1_LEGACY_CONTRACT", refused.reasonCode());
        EditorialP5PilotRequest legacyContext = l.context.withContractRevision(EditorialContractRevision.LEGACY_V1);
        assertFalse(new EditorialL3Execution.Request(l.context, L1_ID, l.report, l2, Set.of()).attemptIdentity()
                .equals(new EditorialL3Execution.Request(legacyContext, L1_ID, l.report, l2, Set.of()).attemptIdentity()));
    }

    // ---- fixture (same minimal helpers as the other engine tests) ----

    private static EditorialP5PilotRequest fixture(String raw, String draft) {
        Map<EditorialPackFileRole, byte[]> authority = new LinkedHashMap<>();
        authority.put(EditorialPackFileRole.PROJECT_INSTRUCTION, PROJECT);
        authority.put(EditorialPackFileRole.TURN_PROMPT, PROMPT);
        authority.put(EditorialPackFileRole.WORKFLOW, WORKFLOW);
        EditorialPackManifest manifest = manifest(authority);
        List<EditorialP5PilotRequest.SourceBytes> sources = List.of(
                source(EditorialSafe4Contract.RAW, "raw", raw),
                source(EditorialSafe4Contract.DRAFT, "draft", draft),
                source(EditorialSafe4Contract.GLOSSARY, "glossary", "term\ttarget"),
                source(EditorialSafe4Contract.PRONOUN, "pronoun", "from\ttarget"));
        EditorialP4Binding binding = binding(manifest, sources);
        return new EditorialP5PilotRequest(binding, manifest, new EditorialP5PilotRequest.PackAuthority(authority), "chapter-1",
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY, sources, "root-predecessor",
                List.of("anchor-1"), List.of("raw-1"), true, 500);
    }

    private static EditorialP5PilotRequest.SourceBytes source(String role, String id, String value) {
        return new EditorialP5PilotRequest.SourceBytes(role, "content://p5/" + id, bytes(value), "UTF-8", schema(role), "VALID", 0L);
    }

    private static String schema(String role) {
        return EditorialSafe4Contract.GLOSSARY.equals(role) ? "safe4.full.glossary.v1"
                : EditorialSafe4Contract.PRONOUN.equals(role) ? "safe4.full.pronoun.v1" : "text.v1";
    }

    private static EditorialPackManifest manifest(Map<EditorialPackFileRole, byte[]> authority) {
        Map<String, Object> root = mutableMap(loadManifest().declarations());
        @SuppressWarnings("unchecked") List<Object> files = (List<Object>) root.get("fileRoles");
        for (Object value : files) {
            @SuppressWarnings("unchecked") Map<String, Object> file = (Map<String, Object>) value;
            String path = String.valueOf(file.get("path"));
            EditorialPackFileRole role = "project.txt".equals(path) ? EditorialPackFileRole.PROJECT_INSTRUCTION
                    : "prompt.txt".equals(path) ? EditorialPackFileRole.TURN_PROMPT : EditorialPackFileRole.WORKFLOW;
            byte[] bytes = authority.get(role);
            file.put("byteLength", BigDecimal.valueOf(bytes.length));
            file.put("sha256", EditorialCanonicalJson.sha256Hex(bytes));
        }
        root.put("packId", "com.example.p5.pack");
        root.put("version", "5.0.0-test");
        root.put("displayName", "P5 test pack");
        root.put("createdAt", "2026-09-04T00:00:00+07:00");
        root.put("canonicalPackHash", "0".repeat(64));
        String without = EditorialCanonicalJson.canonicalize(rootWithout(root, "canonicalPackHash"));
        String domain = "EDITORIAL_PACK_CANONICAL_HASH_V1\n";
        root.put("canonicalPackHash", EditorialCanonicalJson.sha256Hex((domain + without).getBytes(StandardCharsets.UTF_8)));
        return EditorialPackManifest.parse(EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8));
    }

    private static EditorialPackManifest loadManifest() {
        try (java.io.InputStream input = EditorialL2LedgerChainTest.class.getResourceAsStream("/editorial-p2/editorial-pack.json")) {
            if (input == null) throw new AssertionError("P2 manifest test resource missing");
            return EditorialPackManifest.parse(input.readAllBytes());
        } catch (java.io.IOException error) {
            throw new AssertionError(error);
        }
    }

    private static EditorialP4Binding binding(EditorialPackManifest manifest, List<EditorialP5PilotRequest.SourceBytes> sources) {
        List<EditorialP4SourceIdentity> identities = new ArrayList<>();
        for (EditorialP5PilotRequest.SourceBytes source : sources) identities.add(
                new EditorialP4SourceIdentity(source.role(), source.sourceReference(), source.bytes().length,
                        EditorialCanonicalJson.sha256Hex(source.bytes()), source.encoding(), source.schemaStatus(), source.ordinal()));
        String manifestFingerprint = EditorialCanonicalJson.sha256Hex(manifest.canonicalJson().getBytes(StandardCharsets.UTF_8));
        return new EditorialP4Binding("1".repeat(64), "2".repeat(64), "3".repeat(64),
                manifest.packId(), manifest.version(), manifest.canonicalPackHash(), manifestFingerprint,
                "com.example.p5.profile", "2.0.0", "4".repeat(64), "5".repeat(64),
                "p5-evaluation", "DATA_COMPATIBLE", "6".repeat(64),
                EditorialSafe4Contract.CONTRACT_VERSION, EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION,
                "7".repeat(64), "8".repeat(64), EditorialSafe4Contract.NORMAL_MODE,
                "AVAILABLE", "AVAILABLE", "NONE", "USER_CONFIRMED_NORMAL",
                "9".repeat(64), 0L, "EDITORIAL_PILOT", "L1_SOURCE_PREFLIGHT", identities);
    }

    private static Map<String, Object> mutableMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) result.put(entry.getKey(), mutable(entry.getValue()));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Object mutable(Object value) {
        if (value instanceof Map) return mutableMap((Map<String, Object>) value);
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<Object>) value) result.add(mutable(item));
            return result;
        }
        return value;
    }

    private static Map<String, Object> rootWithout(Map<String, Object> source, String key) {
        Map<String, Object> result = new LinkedHashMap<>(source);
        result.remove(key);
        return result;
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    private static final class Store implements EditorialL2Execution.Store {
        final Map<String, EditorialL2Execution.Committed> committed = new HashMap<>();
        final Map<String, Boolean> inFlight = new HashMap<>();

        @Override public Claim claim(String attempt, String predecessor, String bundle) {
            if (committed.containsKey(attempt)) return Claim.ALREADY_COMMITTED;
            if (inFlight.containsKey(attempt)) return Claim.IN_FLIGHT;
            inFlight.put(attempt, true);
            return Claim.ACQUIRED;
        }

        @Override public void commit(EditorialL2Execution.Committed value) { committed.put(value.attemptIdentity(), value); }

        @Override public Optional<EditorialL2Execution.Committed> findCommitted(String attempt) {
            return Optional.ofNullable(committed.get(attempt));
        }

        @Override public void markRecoveryRequired(String attempt, String reason) { }
    }
    private static Object wireView(Map<String, Object> value) {
        Object schema = value.get("wireSchemaVersion");
        return schema instanceof String && ((String) schema).endsWith(".v3")
                ? com.ml.tblandroidtxt.editorial.pack.EditorialUnitReference.wireView(value) : value;
    }

}
