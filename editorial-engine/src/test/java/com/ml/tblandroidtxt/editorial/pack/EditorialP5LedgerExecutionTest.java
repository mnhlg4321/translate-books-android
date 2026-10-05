package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The ledger contract through the real P5 engine with in-memory fakes: identity, inventory, wire judgement,
 * persisted report, RAW to RECONCILE hand-over and refusals. Text is synthetic.
 */
public final class EditorialP5LedgerExecutionTest {
    private static final byte[] PROJECT = bytes("project authority");
    private static final byte[] PROMPT = bytes("prompt authority");
    private static final byte[] WORKFLOW = bytes("workflow authority");
    private static final String RAW = String.join("\n", "王は城に入った。", "騎士が言った。", "「踏破した。」", "「今回は無理だ。」",
            "彼女は笑った。", "「踏破だ。」", "雨が降る。", "彼は歩いた。", "「踏破完了。」", "空は暗い。");
    private static final String DRAFT = String.join("\n", "Vua vao thanh.", "Hiep si noi.", "\"Da chinh phuc.\"",
            "\"今回 khong the.\"", "Co ay cuoi.", "\"Chinh phuc roi.\"", "Troi mua.", "Anh di.", "\"Chinh phuc xong.\"", "Troi toi.");

    // ---- helpers over the wire ----

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static byte[] json(Map<String, Object> m) { return EditorialCanonicalJson.canonicalize(wireView(m)).getBytes(StandardCharsets.UTF_8); }

    private static EditorialRawInventory.Inventory inv() { return EditorialRawInventory.build(bytes(RAW)); }

    private static String id(int line) { return inv().units().get(line - 1).id(); }

    private static List<Object> fullCoverage() {
        return new ArrayList<>(List.of(map("from", id(1), "to", id(10), "status", "PROCESSED")));
    }

    private static byte[] rawWire(String attempt, List<Object> candidates) {
        return json(map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE, "attemptIdentity", attempt,
                "coverage", fullCoverage(), "candidates", candidates));
    }

    private static Map<String, Object> finding(String errorId, int rawLine, int draftLine, String rawQuote, String draftQuote,
                                               String type, List<Object> candidateIds, List<Object> occurrences) {
        return map("errorId", errorId, "type", type, "severity", "MAJOR", "rawUnits", new ArrayList<Object>(List.of(id(rawLine))),
                "draft", map("kind", "LINES", "start", BigDecimal.valueOf(draftLine), "end", BigDecimal.valueOf(draftLine), "after", BigDecimal.ZERO),
                "rawQuote", rawQuote, "draftQuote", draftQuote, "observation", "obs", "expectedMeaning", "exp",
                "evidenceRefs", new ArrayList<Object>(), "candidateIds", candidateIds, "occurrenceUnits", occurrences,
                "disposition", "OPEN", "evidenceLimit", "");
    }

    private static byte[] reconcileWire(String attempt, List<Object> findings, List<Object> resolutions, Map<String, Object> disposition) {
        return json(map("wireSchemaVersion", EditorialL1Ledger.RECONCILE_WIRE, "attemptIdentity", attempt, "coverage", fullCoverage(),
                "resolutions", resolutions, "findings", findings, "speakerRecords", new ArrayList<Object>(),
                "protectedSpans", new ArrayList<Object>(), "disposition", disposition));
    }

    private static Map<String, Object> cont() { return map("disposition", "CONTINUE", "reasonCode", "L1_OK", "stopClass", "NONE"); }

    private static List<Object> candidates() {
        return new ArrayList<>(List.of(
                map("candidateId", "c1", "ledger", "TG", "unitId", id(3), "note", "contrast"),
                map("candidateId", "c2", "ledger", "UNIT", "unitId", id(4), "note", "left")));
    }

    // ---- flow ----

    private static EditorialP5PilotRequest ledgerRaw(Fixture f) {
        return f.request.withContractRevision(EditorialContractRevision.CURRENT_LEDGER);
    }

    private static EditorialP5PilotRequest ledgerReconcile(Fixture f, EditorialP5PilotRequest raw, byte[] rawReport) {
        return raw.withPhase("L1_RECONCILE").withPredecessorIdentity(raw.attemptIdentity()).withPredecessorReport(rawReport);
    }

    @Test
    public void rawPassThenReconcileCommitLedgerReportsAndMetrics() {
        Fixture f = fixture(RAW, DRAFT);
        EditorialP5PilotRequest raw = ledgerRaw(f);
        Store store = new Store();
        EditorialP5PilotProvider.Response rawResponse = ok(rawWire(raw.attemptIdentity(), candidates()));
        FakeProvider provider = new FakeProvider(rawResponse);
        EditorialP5PilotResult rawResult = run(raw, authorization(raw, "auth-raw"), provider, store);
        assertEquals(rawResult.reasonCode(), EditorialP5PilotResult.Outcome.COMMITTED, rawResult.outcome());
        byte[] rawReport = rawResult.committedResult().reportBytes();
        assertEquals(EditorialContractRevision.CURRENT_LEDGER, EditorialContractRevision.ofReportBytes(rawReport));
        EditorialL1Ledger.Body rawBody = EditorialL1Ledger.parseBody(EditorialCanonicalJson.parseObject(rawReport));
        assertEquals(2, rawBody.candidates().size());
        assertEquals(0, rawBody.metrics().uniqueFindingCount());
        assertEquals(EditorialL1Ledger.RAW_WIRE,
                provider.requests.get(0).outputSchemaId());
        // the RAW prompt does not see the candidates block (there is none yet)
        assertFalse(provider.requests.get(0).visibleSources().containsKey(EditorialL1LedgerRun.RAW_CANDIDATES_ROLE));

        EditorialP5PilotRequest reconcile = ledgerReconcile(f, raw, rawReport);
        List<Object> occ = new ArrayList<>(List.of(id(6), id(9)));
        List<Object> findings = new ArrayList<>(List.of(
                finding("e1", 3, 3, "踏破", "chinh phuc", "MEANING", new ArrayList<>(List.of("c1")), occ),
                finding("e2", 4, 4, "今回", "今回", "UNTRANSLATED", new ArrayList<>(List.of("c2")), new ArrayList<>())));
        List<Object> resolutions = new ArrayList<>(List.of(
                map("candidateId", "c1", "status", "PROCESSED", "findingRef", "e1"),
                map("candidateId", "c2", "status", "PROCESSED", "findingRef", "e2")));
        FakeProvider provider2 = new FakeProvider(ok(reconcileWire(reconcile.attemptIdentity(), findings, resolutions, cont())));
        EditorialP5PilotResult result = run(reconcile, authorization(reconcile, "auth-rec"), provider2, store);
        assertEquals(result.reasonCode(), EditorialP5PilotResult.Outcome.COMMITTED, result.outcome());
        assertEquals(2, result.metrics().findingCount());
        assertEquals(EditorialL1Ledger.RECONCILE_WIRE, provider2.requests.get(0).outputSchemaId());
        byte[] block = provider2.requests.get(0).visibleSources().get(EditorialL1LedgerRun.RAW_CANDIDATES_ROLE);
        assertNotNull(block);
        String blockText = new String(block, StandardCharsets.UTF_8);
        assertTrue(blockText.contains("c1\tTG\tL3"));
        assertTrue(blockText.contains("c2\tUNIT\tL4"));

        byte[] report = store.committed.get(reconcile.attemptIdentity()).reportBytes();
        Map<String, Object> parsed = EditorialCanonicalJson.parseObject(report);
        // v1 reader keys survive
        assertEquals("REPORT_L1", parsed.get("artifactType"));
        assertEquals("L1_RECONCILE", parsed.get("phase"));
        assertEquals("CONTINUE", parsed.get("disposition"));
        assertEquals(EditorialContractRevision.REPORT_SCHEMA_V2, parsed.get("schemaVersion"));
        EditorialL1Ledger.Body body = EditorialL1Ledger.parseBody(parsed);
        assertEquals(2, body.findings().size());
        assertEquals(4, body.metrics().occurrenceCount());
        assertEquals(10, body.metrics().unitCount());
        assertEquals(2, body.candidates().size());
        // exactly the persisted bytes are what a later reader (committedL1 → L2) gets
        assertArrayEquals(report, store.findCommitted(reconcile.attemptIdentity()).get().reportBytes());
    }

    @Test
    public void legacyAndLedgerAttemptsNeverCollide() {
        Fixture f = fixture(RAW, DRAFT);
        assertFalse(f.request.attemptIdentity().equals(ledgerRaw(f).attemptIdentity()));
    }

    @Test
    public void reconcileRefusesLegacyPredecessorReportAndMissingOrStaleOnes() {
        Fixture f = fixture(RAW, DRAFT);
        EditorialP5PilotRequest raw = ledgerRaw(f);
        byte[] legacy = json(map("phase", "L1_RAW_DISCOVERY", "artifactType", "REPORT_L1"));
        assertRefused("INPUT_REPORT_L1_LEGACY_CONTRACT:root", ledgerReconcile(f, raw, legacy));
        assertRefused("INPUT_RAW_LEDGER_REPORT_MISSING:root", raw.withPhase("L1_RECONCILE"));
        // a ledger report of a different RAW is stale
        Fixture other = fixture(RAW + "\n追加。", DRAFT);
        EditorialP5PilotRequest otherRaw = ledgerRaw(other);
        Store s = new Store();
        EditorialP5PilotResult r = run(otherRaw, authorization(otherRaw, "a"), new FakeProvider(ok(
                json(map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE, "attemptIdentity", otherRaw.attemptIdentity(),
                        "coverage", new ArrayList<Object>(List.of(map("from", EditorialRawInventory.build(bytes(RAW + "\n追加。")).units().get(0).id(),
                                "to", EditorialRawInventory.build(bytes(RAW + "\n追加。")).units().get(10).id(), "status", "PROCESSED"))),
                        "candidates", new ArrayList<Object>())))), s);
        assertEquals(r.reasonCode(), EditorialP5PilotResult.Outcome.COMMITTED, r.outcome());
        assertRefused("INPUT_RAW_LEDGER_STALE:root", ledgerReconcile(f, raw, r.committedResult().reportBytes()));
    }

    private void assertRefused(String code, EditorialP5PilotRequest request) {
        FakeProvider provider = new FakeProvider();
        EditorialP5PilotResult r = run(request, authorization(request, "a"), provider, new Store());
        assertEquals(EditorialP5PilotResult.Outcome.STOP, r.outcome());
        assertEquals(code, r.reasonCode());
        assertEquals(0, provider.calls);
    }

    @Test
    public void invalidWireStopsTypedWithoutRepairCallAndMarksRecovery() {
        Fixture f = fixture(RAW, DRAFT);
        EditorialP5PilotRequest raw = ledgerRaw(f);
        Store store = new Store();
        // a coverage gap: only lines 1..5 covered
        byte[] gap = json(map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE, "attemptIdentity", raw.attemptIdentity(),
                "coverage", new ArrayList<Object>(List.of(map("from", id(1), "to", id(5), "status", "PROCESSED"))),
                "candidates", new ArrayList<Object>()));
        FakeProvider provider = new FakeProvider(ok(gap));
        EditorialP5PilotResult r = run(raw, authorization(raw, "a"), provider, store);
        assertEquals(EditorialP5PilotResult.Outcome.STOP, r.outcome());
        assertEquals("REPAIR_L1_LEDGER_INVALID", r.reasonCode());
        assertEquals(List.of("L1_COVERAGE_GAP:coverage"), r.stopReceipt().evidenceRefs());
        assertEquals(1, provider.calls);
        assertTrue(store.committed.isEmpty());
        assertTrue(store.inFlight.contains(raw.attemptIdentity()));
    }

    @Test
    public void unresolvedCandidateAndForgedQuoteAreRefused() {
        Fixture f = fixture(RAW, DRAFT);
        EditorialP5PilotRequest raw = ledgerRaw(f);
        Store store = new Store();
        EditorialP5PilotResult rawResult = run(raw, authorization(raw, "a"),
                new FakeProvider(ok(rawWire(raw.attemptIdentity(), candidates()))), store);
        byte[] rawReport = rawResult.committedResult().reportBytes();
        EditorialP5PilotRequest reconcile = ledgerReconcile(f, raw, rawReport);
        // c2 not resolved
        EditorialP5PilotResult r = run(reconcile, authorization(reconcile, "b"), new FakeProvider(ok(reconcileWire(
                reconcile.attemptIdentity(), new ArrayList<>(), new ArrayList<>(List.of(
                        map("candidateId", "c1", "status", "PRESERVED", "findingRef", ""))), cont()))), new Store());
        assertEquals("REPAIR_L1_LEDGER_INVALID", r.reasonCode());
        assertEquals(List.of("L1_CANDIDATE_UNRESOLVED:resolutions"), r.stopReceipt().evidenceRefs());
        // forged raw quote
        List<Object> findings = new ArrayList<>(List.of(finding("e1", 3, 3, "攻略", "chinh phuc", "MEANING",
                new ArrayList<>(), new ArrayList<>())));
        EditorialP5PilotResult q = run(reconcile, authorization(reconcile, "c"), new FakeProvider(ok(reconcileWire(
                reconcile.attemptIdentity(), findings, new ArrayList<>(List.of(
                        map("candidateId", "c1", "status", "PRESERVED", "findingRef", ""),
                        map("candidateId", "c2", "status", "PRESERVED", "findingRef", ""))), cont()))), new Store());
        assertEquals(List.of("L1_RAW_QUOTE_NOT_IN_ANCHOR:findings.0.rawQuote"), q.stopReceipt().evidenceRefs());
    }

    @Test
    public void typedStopFromTheModelIsHonouredAndPreserveDraftCommits() {
        Fixture f = fixture(RAW, DRAFT);
        EditorialP5PilotRequest raw = ledgerRaw(f);
        Store store = new Store();
        byte[] rawReport = run(raw, authorization(raw, "a"),
                new FakeProvider(ok(rawWire(raw.attemptIdentity(), new ArrayList<>()))), store).committedResult().reportBytes();
        EditorialP5PilotRequest reconcile = ledgerReconcile(f, raw, rawReport);
        Map<String, Object> preserve = map("disposition", "PRESERVE_DRAFT", "reasonCode", "L1_NO_EVIDENCE", "stopClass", "NONE");
        EditorialP5PilotResult p = run(reconcile, authorization(reconcile, "b"), new FakeProvider(ok(reconcileWire(
                reconcile.attemptIdentity(), new ArrayList<>(), new ArrayList<>(), preserve))), store);
        assertEquals(p.reasonCode(), EditorialP5PilotResult.Outcome.PRESERVE_DRAFT, p.outcome());
        Map<String, Object> stop = map("disposition", "STOP", "reasonCode", "L1_BLOCKED", "stopClass", "CONTENT_BLOCKED");
        EditorialP5PilotResult s = run(reconcile, authorization(reconcile, "c"), new FakeProvider(ok(reconcileWire(
                reconcile.attemptIdentity(), new ArrayList<>(), new ArrayList<>(), stop))), new Store());
        assertEquals(EditorialP5PilotResult.Outcome.STOP, s.outcome());
        assertEquals(EditorialP5PilotResult.StopClass.CONTENT_BLOCKED, s.stopReceipt().stopClass());
    }

    @Test
    public void tooManyUnitsStopsTypedBeforeAnyCall() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i <= EditorialL1LedgerRun.MAX_UNITS_SINGLE_CALL; i++) big.append("行").append(i).append('\n');
        Fixture f = fixture(big.toString(), DRAFT);
        assertRefused("L1_UNIT_LIMIT_EXCEEDED:root", ledgerRaw(f));
    }

    // ---- fixture ----

    private EditorialP5PilotResult run(EditorialP5PilotRequest request, EditorialP5PilotAuthorization authorization,
                                       FakeProvider provider, Store store) {
        return new EditorialP5PilotExecution(() -> 1_000L).execute(request, authorization, provider, store);
    }

    /**
     * Z2: parsing is not the whole path. Every bookkeeping variant of a RECONCILE wire must be committed by the real
     * engine (decision, report, readback), and a refusal must never be the generic parse failure that hides a bug.
     */
    @Test
    public void everyBookkeepingVariantIsCommittedEndToEndAndRefusalsCarryTypedDetails() {
        Fixture f = fixture(RAW, DRAFT);
        EditorialP5PilotRequest raw = ledgerRaw(f);
        Store base = new Store();
        byte[] rawReport = run(raw, authorization(raw, "auth-raw"),
                new FakeProvider(ok(rawWire(raw.attemptIdentity(), candidates()))), base).committedResult().reportBytes();
        EditorialP5PilotRequest reconcile = ledgerReconcile(f, raw, rawReport);
        List<Object> findings = new ArrayList<>(List.of(
                finding("e1", 3, 3, "踏破", "chinh phuc", "MEANING", new ArrayList<>(List.of("c1")), new ArrayList<>(List.of(id(6), id(9)))),
                finding("e2", 4, 4, "今回", "今回", "UNTRANSLATED", new ArrayList<>(List.of("c2")), new ArrayList<>())));
        List<Object> resolutions = new ArrayList<>(List.of(
                map("candidateId", "c1", "status", "PROCESSED", "findingRef", "e1"),
                map("candidateId", "c2", "status", "PROCESSED", "findingRef", "e2")));
        byte[] baseline = reconcileWire(reconcile.attemptIdentity(), findings, resolutions, cont());
        List<EditorialWireMutationEngine.Mutation> mutations = EditorialWireMutationEngine.l1Reconcile(
                EditorialWireMutationEngine.copy(baseline), 10, 0, 10, "L3", java.util.Set.of(3, 4), quote -> {
                    int hits = 0;
                    for (EditorialRawInventory.Unit unit : inv().units()) {
                        if (EditorialQuoteMatcher.containsRaw(unit.text(), quote)) hits++;
                    }
                    return hits;
                });
        int committed = 0;
        int refused = 0;
        for (EditorialWireMutationEngine.Mutation mutation : mutations) {
            Map<String, Object> variant = EditorialWireMutationEngine.copy(baseline);
            mutation.apply().accept(variant);
            Store store = new Store();
            run(raw, authorization(raw, "auth-raw"), new FakeProvider(ok(rawWire(raw.attemptIdentity(), candidates()))), store);
            EditorialP5PilotResult result = run(reconcile, authorization(reconcile, "auth-rec"),
                    new FakeProvider(ok(EditorialWireMutationEngine.bytes(variant))), store);
            if (mutation.expect() == EditorialWireMutationEngine.Expect.NORMALIZED) {
                assertEquals(mutation.name() + ": " + result.reasonCode()
                                + (result.stopReceipt() == null ? "" : result.stopReceipt().evidenceRefs()),
                        EditorialP5PilotResult.Outcome.COMMITTED, result.outcome());
                committed++;
            } else {
                assertEquals(mutation.name(), EditorialP5PilotResult.Outcome.STOP, result.outcome());
                assertFalse(mutation.name() + " was refused with the generic parse failure",
                        result.stopReceipt().evidenceRefs().toString().contains("L1_WIRE_PARSE_FAILED"));
                refused++;
            }
        }
        assertTrue("committed variants " + committed, committed >= 25);
        assertTrue("refused variants " + refused, refused >= 8);
    }

    @Test
    public void aContinuingDispositionWithAnEmptyReasonIsCommitted() {
        Fixture f = fixture(RAW, DRAFT);
        EditorialP5PilotRequest raw = ledgerRaw(f);
        Store store = new Store();
        byte[] rawReport = run(raw, authorization(raw, "auth-raw"),
                new FakeProvider(ok(rawWire(raw.attemptIdentity(), candidates()))), store).committedResult().reportBytes();
        EditorialP5PilotRequest reconcile = ledgerReconcile(f, raw, rawReport);
        List<Object> resolutions = new ArrayList<>(List.of(
                map("candidateId", "c1", "status", "PRESERVED", "findingRef", ""),
                map("candidateId", "c2", "status", "PRESERVED", "findingRef", "")));
        for (String kind : List.of("CONTINUE", "PRESERVE_DRAFT")) {
            Store fresh = new Store();
            run(raw, authorization(raw, "auth-raw"), new FakeProvider(ok(rawWire(raw.attemptIdentity(), candidates()))), fresh);
            EditorialP5PilotResult result = run(reconcile, authorization(reconcile, "auth-rec"), new FakeProvider(ok(reconcileWire(
                    reconcile.attemptIdentity(), new ArrayList<>(), resolutions,
                    map("disposition", kind, "reasonCode", "", "stopClass", "NONE")))), fresh);
            assertEquals(kind + ": " + result.reasonCode(), EditorialP5PilotResult.Outcome.valueOf(
                    "CONTINUE".equals(kind) ? "COMMITTED" : "PRESERVE_DRAFT"), result.outcome());
        }
    }

    private static EditorialP5PilotProvider.Response ok(byte[] wire) {
        return new EditorialP5PilotProvider.Response("response-success", wire, "stop", true, 80, 30, 110,
                BigDecimal.ZERO, null, true);
    }

    private static EditorialP5PilotAuthorization authorization(EditorialP5PilotRequest request, String id) {
        return new EditorialP5PilotAuthorization(id, request.binding().bindingIdentity(),
                request.binding().runDeclarationIdentity(), request.binding().canonicalPackHash(),
                request.binding().canonicalProfileHash(), request.binding().compatibilityEvaluationId(),
                request.chapterKey(), "L1", "FAKE_PROVIDER", "fake/model", "fake-endpoint-account",
                1, 0, 0, 10_000, 2_000, 12_000, BigDecimal.ONE, 60_000L,
                true, false, false, "HASH_ONLY", "TEST_STOP_AUTHORITY", 0L, Long.MAX_VALUE, true);
    }

    private static Fixture fixture(String raw, String draft) {
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
        EditorialP5PilotRequest request = new EditorialP5PilotRequest(binding, manifest,
                new EditorialP5PilotRequest.PackAuthority(authority), "chapter-1",
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY, sources, "root-predecessor",
                List.of("anchor-1"), List.of("raw-1"), true, 500);
        return new Fixture(request);
    }

    private static EditorialP5PilotRequest.SourceBytes source(String role, String id, String value) {
        return new EditorialP5PilotRequest.SourceBytes(role, "content://p5/" + id,
                bytes(value), "UTF-8", schema(role), "VALID", 0L);
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
                    : "prompt.txt".equals(path) ? EditorialPackFileRole.TURN_PROMPT
                    : EditorialPackFileRole.WORKFLOW;
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
        root.put("canonicalPackHash", EditorialCanonicalJson.sha256Hex(
                (domain + without).getBytes(StandardCharsets.UTF_8)));
        return EditorialPackManifest.parse(EditorialCanonicalJson.canonicalize(root)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static EditorialPackManifest loadManifest() {
        try (java.io.InputStream input = EditorialP5LedgerExecutionTest.class
                .getResourceAsStream("/editorial-p2/editorial-pack.json")) {
            if (input == null) throw new AssertionError("P2 manifest test resource missing");
            return EditorialPackManifest.parse(input.readAllBytes());
        } catch (java.io.IOException error) {
            throw new AssertionError(error);
        }
    }

    private static EditorialP4Binding binding(EditorialPackManifest manifest,
                                              List<EditorialP5PilotRequest.SourceBytes> sources) {
        List<EditorialP4SourceIdentity> identities = new ArrayList<>();
        for (EditorialP5PilotRequest.SourceBytes source : sources) identities.add(
                new EditorialP4SourceIdentity(source.role(), source.sourceReference(), source.bytes().length,
                        EditorialCanonicalJson.sha256Hex(source.bytes()), source.encoding(),
                        source.schemaStatus(), source.ordinal()));
        String manifestFingerprint = EditorialCanonicalJson.sha256Hex(
                manifest.canonicalJson().getBytes(StandardCharsets.UTF_8));
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

    private record Fixture(EditorialP5PilotRequest request) { }

    private static final class FakeProvider implements EditorialP5PilotProvider {
        private final Deque<Response> responses = new ArrayDeque<>();
        private final List<Request> requests = new ArrayList<>();
        private int calls;

        FakeProvider(Response... responses) { this.responses.addAll(Arrays.asList(responses)); }

        @Override public Response call(Request request) {
            calls++;
            requests.add(request);
            if (responses.isEmpty()) throw new AssertionError("unexpected fake-provider call");
            return responses.removeFirst();
        }
    }

    private static final class Store implements EditorialP5PilotExecution.AttemptStore {
        private final Map<String, EditorialP5PilotResult.CommittedResult> committed = new LinkedHashMap<>();
        private final Set<String> inFlight = new java.util.HashSet<>();

        @Override public Claim claim(String attemptIdentity) {
            if (committed.containsKey(attemptIdentity)) return Claim.ALREADY_COMMITTED;
            if (!inFlight.add(attemptIdentity)) return Claim.IN_FLIGHT;
            return Claim.ACQUIRED;
        }

        @Override public void commit(EditorialP5PilotResult.CommittedResult value) {
            committed.put(value.attemptIdentity(), value);
            inFlight.remove(value.attemptIdentity());
        }

        @Override public Optional<EditorialP5PilotResult.CommittedResult> findCommitted(String attemptIdentity) {
            return Optional.ofNullable(committed.get(attemptIdentity));
        }

        @Override public void markRecoveryRequired(String attemptIdentity, String reasonCode) {
            inFlight.add(attemptIdentity);
        }
    }
    private static Object wireView(Map<String, Object> value) {
        Object schema = value.get("wireSchemaVersion");
        return schema instanceof String && ((String) schema).endsWith(".v3")
                ? com.ml.tblandroidtxt.editorial.pack.EditorialUnitReference.wireView(value) : value;
    }

}
