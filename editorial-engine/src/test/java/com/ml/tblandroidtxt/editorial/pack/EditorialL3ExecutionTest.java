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
import java.util.TreeSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** L3 (blind re-audit + reconcile) boundary tests; provider and store are in-memory fakes. */
public final class EditorialL3ExecutionTest {
    private static final byte[] PROJECT = bytes("project authority");
    private static final byte[] PROMPT = bytes("prompt authority");
    private static final byte[] WORKFLOW = bytes("workflow authority");
    private static final String DRAFT = "dong mot\ndong hai\ndong ba\n";
    private static final String VI_L2 = "dong mot\ndong hai sua\ndong ba\n";
    private static final String L1_ID = "l1-attempt-identity-1";
    private static final EditorialL2Execution.Budget BUDGET = new EditorialL2Execution.Budget(
            100_000, 4_000, new BigDecimal("0.50"), 60_000L);

    // ---- cases ----

    @Test public void legacyL3IdentityIsFrozen() {
        assertEquals("d5719d09f853221866d1eee8bace62b38b92979988b3d22ee14cba9bef661bea", fx(Set.of()).l3().attemptIdentity());
    }
    @Test public void legacyIdentitiesAreFrozen() {
        EditorialP5PilotRequest req = ctx();
        assertEquals("6bb51cceb4e38b5d7db80b281070f97dbf783cd3a9344e29c13545c284abc73e", req.attemptIdentity());
        assertEquals("d84a70ce260e4aff12fc009081452f751b70ed1ecb2c1d397df68555810c0888", req.requestIdentity());
    }

    @Test public void v2IdentityIsFrozenAndV3DoesNotReuseIt() {
        EditorialP5PilotRequest v2 = ctx().withContractRevision(EditorialContractRevision.L1_LEDGER_V2);
        assertEquals("5a770ec2a0b85ff02a80590e892cab52e68f9741761b6500584296cb0d828b52", v2.attemptIdentity());
        assertEquals("7b8a1a10bb2a804badeeeebc7d239b8e0a3084d6fb8be0d19ddfc3f17342cc0e", v2.requestIdentity());
        EditorialP5PilotRequest v3 = v2.withContractRevision(EditorialContractRevision.L1_LEDGER_V3);
        assertFalse(v2.attemptIdentity().equals(v3.attemptIdentity()));
        assertFalse(v2.requestIdentity().equals(v3.requestIdentity()));
    }

    @Test public void currentLedgerRevisionChangesIdentitiesAndIsDeterministic() {
        EditorialP5PilotRequest legacy = ctx();
        EditorialP5PilotRequest ledger = legacy.withContractRevision(EditorialContractRevision.CURRENT_LEDGER);
        assertEquals(EditorialContractRevision.LEGACY_V1, legacy.contractRevision());
        assertFalse(legacy.attemptIdentity().equals(ledger.attemptIdentity()));
        assertFalse(legacy.requestIdentity().equals(ledger.requestIdentity()));
        assertEquals(ledger.attemptIdentity(), ctx().withContractRevision(EditorialContractRevision.CURRENT_LEDGER).attemptIdentity());
        // the legacy revision given explicitly is the identity from before the revision existed
        assertEquals(legacy.attemptIdentity(), legacy.withContractRevision(EditorialContractRevision.LEGACY_V1).attemptIdentity());
        // phase and predecessor report copies keep the revision
        assertEquals(EditorialContractRevision.CURRENT_LEDGER, ledger.withPhase("L1_RECONCILE").contractRevision());
        assertArrayEquals(new byte[] {1}, ledger.withPredecessorReport(new byte[] {1}).predecessorReport());
        assertEquals(EditorialContractRevision.CURRENT_LEDGER, ledger.withPredecessorReport(new byte[] {1}).contractRevision());
        try {
            legacy.withContractRevision("L1_LEDGER_V10");
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("unknown contract revision", expected.getMessage());
        }
    }

    @Test public void happyPathCommitsFinalAndReceipt() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        ScriptProvider provider = new ScriptProvider(
                reaudit(id, defaultCandidates()),
                reconcile(id, defaultResolutions(), List.of(qaChange("q1", 3, "dong ba", "dong ba sua", false, null)),
                        List.of(preserved("p1", 1, "dong mot")), probes("cov", "NO_DEFECT"), probes("reg", "FIXED")));
        FakeStore store = new FakeStore();

        EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, BUDGET, provider, store);

        assertEquals(r.reasonCode() + r.issues(), EditorialL3Execution.Outcome.COMMITTED, r.outcome());
        assertEquals(2, r.providerCalls());
        assertEquals(2, provider.calls);
        assertEquals(EditorialL2Execution.Provider.class, EditorialL2Execution.Provider.class);
        EditorialL2Execution.Committed stored = store.committed.get(id);
        assertArrayEquals(bytes("dong mot\ndong hai sua\ndong ba sua\n"), stored.viL2Bytes());
        EditorialL3Execution.ReleaseNumbers n = r.releaseNumbers();
        assertEquals(0, n.unprocessedRawUnits());
        assertEquals(0, n.unprocessedCandidates());
        assertEquals(0, n.provenUnresolvedConflicts());
        assertEquals(0, n.unaccountedChangedAnchors());
        assertEquals(0, n.protectedSpanRegressions());
        assertEquals(2, n.preserved()); // t1 resolved PRESERVED + one QA preserved row
        assertTrue(n.releasable());

        Map<String, Object> receipt = EditorialCanonicalJson.parseObject(stored.changeMapBytes());
        assertEquals("QA_RECEIPT", receipt.get("artifactType"));
        assertEquals(EditorialL3Execution.QA_RECEIPT_SCHEMA, receipt.get("schemaVersion"));
        assertEquals(EditorialCanonicalJson.sha256Hex(stored.viL2Bytes()), receipt.get("finalSha256"));
        assertEquals(stored.viL2Sha256(), receipt.get("finalSha256"));
        assertEquals(EditorialCanonicalJson.sha256Hex(stored.changeMapBytes()), stored.changeMapSha256());
        assertEquals("NONE", receipt.get("stopReceipt"));
        assertEquals(f.l2.viL2Sha256(), receipt.get("viL2Sha256"));
        assertEquals(f.l2.attemptIdentity(), receipt.get("viL2AttemptIdentity"));
        assertEquals(L1_ID, receipt.get("reportL1AttemptIdentity"));
        assertEquals(id, receipt.get("attemptIdentity"));
        Map<String, Object> rel = obj(receipt.get("releaseNumbers"));
        for (String key : List.of("unprocessedRawUnits", "unprocessedCandidates", "provenUnresolvedConflicts",
                "unaccountedChangedAnchors", "protectedSpanRegressions")) {
            assertEquals(key, 0, ((BigDecimal) rel.get(key)).intValueExact());
        }
        assertEquals(2, ((BigDecimal) rel.get("preserved")).intValueExact());
        Map<String, Object> ledgers = obj(receipt.get("ledgers"));
        assertEquals(2, ((BigDecimal) obj(ledgers.get("UNIT")).get("PROCESSED")).intValueExact());
        assertEquals(1, ((BigDecimal) obj(ledgers.get("TG")).get("PRESERVED")).intValueExact());
        assertEquals(1, ((BigDecimal) obj(ledgers.get("SR")).get("PROCESSED")).intValueExact());
        assertTrue(obj(ledgers.get("RC")).isEmpty());
        Map<String, Object> qaMap = obj(receipt.get("qaChangeMap"));
        assertEquals("QA_CHANGE_MAP", qaMap.get("artifactType"));
        assertEquals(f.l2.viL2Sha256(), qaMap.get("baseSha256"));
        assertEquals(stored.viL2Sha256(), qaMap.get("outputSha256"));
        assertEquals(1, ((List<?>) receipt.get("adversarialCoverage")).size());
        assertEquals(1, ((List<?>) receipt.get("adversarialRegression")).size());
        assertEquals(r.committed().viL2Sha256(), stored.viL2Sha256());
    }

    @Test public void visibleSourcesPerPhase() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        ScriptProvider provider = new ScriptProvider(reaudit(id, defaultCandidates()),
                reconcile(id, defaultResolutions(), List.of(), List.of(), probes("cov", "NO_DEFECT"),
                        probes("reg", "NO_DEFECT")));
        EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, BUDGET, provider, new FakeStore());
        assertEquals(r.reasonCode() + r.issues(), EditorialL3Execution.Outcome.COMMITTED, r.outcome());
        assertEquals(2, provider.requests.size());

        EditorialL2Execution.Provider.Request first = provider.requests.get(0);
        assertEquals(EditorialL3Execution.REAUDIT_PHASE, first.phase());
        assertEquals(EditorialL3Execution.REAUDIT_WIRE, first.outputSchemaId());
        assertEquals(new TreeSet<>(Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY, "VI_L2")),
                new TreeSet<>(first.visibleSources().keySet()));
        assertFalse(first.visibleSources().containsKey(EditorialSafe4Contract.DRAFT));
        assertFalse(first.visibleSources().containsKey("REPORT_L1"));
        assertFalse(first.visibleSources().containsKey("CHANGE_MAP_L2"));
        assertFalse(first.visibleSources().containsKey(EditorialSafe4Contract.PRONOUN));

        EditorialL2Execution.Provider.Request second = provider.requests.get(1);
        assertEquals(EditorialL3Execution.RECONCILE_PHASE, second.phase());
        assertEquals(EditorialL3Execution.RECONCILE_WIRE, second.outputSchemaId());
        Set<String> keys = second.visibleSources().keySet();
        for (String role : List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                EditorialSafe4Contract.GLOSSARY, "REPORT_L1", "VI_L2", "CHANGE_MAP_L2", "L3_REAUDIT_CANDIDATES")) {
            assertTrue(role + " in " + keys, keys.contains(role));
        }
        boolean pronounAuthoritative = req.context().bundleForExecution().assets().stream()
                .anyMatch(a -> EditorialSafe4Contract.PRONOUN.equals(a.role()) && a.authoritative());
        assertEquals(keys.toString(), pronounAuthoritative, keys.contains(EditorialSafe4Contract.PRONOUN));
        assertEquals(keys.toString(), pronounAuthoritative ? 8 : 7, keys.size());
        // The envelope the model reconciles against is app-owned and lists every candidate.
        Map<String, Object> env = EditorialCanonicalJson.parseObject(second.visibleSources().get("L3_REAUDIT_CANDIDATES"));
        assertEquals(4, ((List<?>) env.get("candidates")).size());
    }

    @Test public void secondExecuteIsAlreadyCommitted() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        ScriptProvider provider = new ScriptProvider(reaudit(id, defaultCandidates()),
                reconcile(id, defaultResolutions(), List.of(), List.of(), probes("cov", "NO_DEFECT"),
                        probes("reg", "NO_DEFECT")));
        FakeStore store = new FakeStore();
        EditorialL3Execution exec = new EditorialL3Execution();
        assertEquals(EditorialL3Execution.Outcome.COMMITTED, exec.execute(req, BUDGET, provider, store).outcome());
        EditorialL3Execution.Result again = exec.execute(req, BUDGET, provider, store);
        assertEquals(EditorialL3Execution.Outcome.ALREADY_COMMITTED, again.outcome());
        assertTrue(again.accepted());
        assertEquals(0, again.providerCalls());
        assertEquals(2, provider.calls);
    }

    @Test public void predecessorGatesStopBeforeProvider() {
        Fx f = fx(Set.of());
        EditorialP5PilotRequest c = f.context;
        byte[] report = f.reportBytes;

        gate(new EditorialL3Execution.Request(c, L1_ID, report, null, Set.of()), "INPUT_VI_L2_REQUIRED");

        EditorialL2Execution.Committed l2 = f.l2;
        EditorialL2Execution.Committed tampered = new EditorialL2Execution.Committed(l2.attemptIdentity(),
                l2.predecessorIdentity(), l2.bundleIdentity(), bytes(VI_L2 + "tamper\n"), l2.viL2Sha256(),
                l2.changeMapBytes(), l2.changeMapSha256());
        gate(new EditorialL3Execution.Request(c, L1_ID, report, tampered, Set.of()),
                "INPUT_VI_L2_INTEGRITY_INVALID");

        EditorialL2Execution.Committed otherChain = new EditorialL2Execution.Committed(l2.attemptIdentity(),
                "some-other-report-attempt", l2.bundleIdentity(), l2.viL2Bytes(), l2.viL2Sha256(),
                l2.changeMapBytes(), l2.changeMapSha256());
        gate(new EditorialL3Execution.Request(c, L1_ID, report, otherChain, Set.of()),
                "INPUT_VI_L2_CHAIN_MISMATCH");

        gate(new EditorialL3Execution.Request(c, L1_ID, report(c, "L1_RAW_DISCOVERY"), l2, Set.of()),
                "INPUT_REPORT_L1_PHASE_INVALID");
    }

    @Test public void unprocessedCandidateBlocksRelease() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        // t1 (TG) and u1 (UNIT) both stay UNPROCESSED after reconcile.
        Map<String, String> res = new LinkedHashMap<>();
        res.put("s1", "PROCESSED");
        ScriptProvider provider = new ScriptProvider(reaudit(id, defaultCandidates()),
                reconcile(id, res, List.of(), List.of(), probes("cov", "NO_DEFECT"), probes("reg", "NO_DEFECT")));
        FakeStore store = new FakeStore();
        EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, BUDGET, provider, store);
        assertEquals(EditorialL3Execution.Outcome.STOPPED, r.outcome());
        assertEquals(EditorialL2Execution.StopClass.REPAIR_REQUIRED, r.stopClass());
        assertEquals("REPAIR_L3_RELEASE_NUMBERS_NOT_ZERO", r.reasonCode());
        assertEquals(1, r.releaseNumbers().unprocessedRawUnits());
        assertEquals(1, r.releaseNumbers().unprocessedCandidates());
        assertFalse(r.releaseNumbers().releasable());
        assertNull(r.committed());
        assertEquals(2, r.providerCalls());
        assertTrue(store.committed.isEmpty());
        assertTrue(store.recovery.containsKey(id));
    }

    @Test public void provenConflictIsContentBlocked() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        // Case A: a candidate resolved CONFLICT.
        Map<String, String> res = defaultResolutions();
        res.put("u2", "CONFLICT");
        FakeStore storeA = new FakeStore();
        EditorialL3Execution.Result a = new EditorialL3Execution().execute(req, BUDGET,
                new ScriptProvider(reaudit(id, defaultCandidates()),
                        reconcile(id, res, List.of(), List.of(), probes("cov", "NO_DEFECT"), probes("reg", "NO_DEFECT"))),
                storeA);
        assertConflict(a, storeA);
        assertEquals(1, a.releaseNumbers().provenUnresolvedConflicts());

        // Case B: an adversarial probe verdict CONFLICT.
        FakeStore storeB = new FakeStore();
        EditorialL3Execution.Result b = new EditorialL3Execution().execute(req, BUDGET,
                new ScriptProvider(reaudit(id, defaultCandidates()),
                        reconcile(id, defaultResolutions(), List.of(), List.of(), probes("cov", "NO_DEFECT"),
                                probes("reg", "CONFLICT"))), storeB);
        assertConflict(b, storeB);
        assertEquals(1, b.releaseNumbers().provenUnresolvedConflicts());

        // Case C: coverage probe CONFLICT.
        FakeStore storeC = new FakeStore();
        EditorialL3Execution.Result c = new EditorialL3Execution().execute(req, BUDGET,
                new ScriptProvider(reaudit(id, defaultCandidates()),
                        reconcile(id, defaultResolutions(), List.of(), List.of(), probes("cov", "CONFLICT"),
                                probes("reg", "NO_DEFECT"))), storeC);
        assertConflict(c, storeC);
    }

    @Test public void malformedReauditAndReconcileAreRepairRequired() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        List<byte[]> badReaudit = new ArrayList<>();
        // No UNIT row at all.
        badReaudit.add(reaudit(id, List.of(cand("t1", "TG", 1, "PROCESSED"))));
        // Wrong attempt echo.
        badReaudit.add(reaudit("0".repeat(64), defaultCandidates()));
        // Unknown key.
        Map<String, Object> unknown = reauditMap(id, defaultCandidates());
        unknown.put("extra", "x");
        badReaudit.add(canon(unknown));
        // Line beyond VI_L2 line count (VI_L2 splits into 4 entries: 3 lines + trailing empty).
        badReaudit.add(reaudit(id, List.of(cand("u1", "UNIT", 5, "PROCESSED"))));
        // Unknown ledger / status / duplicate id / not json.
        badReaudit.add(reaudit(id, List.of(cand("u1", "XX", 1, "PROCESSED"))));
        badReaudit.add(reaudit(id, List.of(cand("u1", "UNIT", 1, "DONE"))));
        badReaudit.add(reaudit(id, List.of(cand("u1", "UNIT", 1, "PROCESSED"), cand("u1", "UNIT", 2, "PROCESSED"))));
        badReaudit.add(bytes("not json"));
        for (byte[] wire : badReaudit) {
            ScriptProvider provider = new ScriptProvider(wire);
            FakeStore store = new FakeStore();
            EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, BUDGET, provider, store);
            assertEquals(r.reasonCode(), EditorialL2Execution.StopClass.REPAIR_REQUIRED, r.stopClass());
            assertEquals("REPAIR_L3_REAUDIT_SCHEMA_INVALID", r.reasonCode());
            assertEquals(1, r.providerCalls());
            assertEquals(1, provider.calls);
            assertTrue(store.committed.isEmpty());
        }

        List<byte[]> badReconcile = new ArrayList<>();
        badReconcile.add(reconcile(id, defaultResolutions(), List.of(), List.of(), probes("cov", "NO_DEFECT"),
                List.of()));
        badReconcile.add(reconcile(id, defaultResolutions(), List.of(), List.of(), List.of(),
                probes("reg", "NO_DEFECT")));
        Map<String, String> unknownId = defaultResolutions();
        unknownId.put("ghost", "PROCESSED");
        badReconcile.add(reconcile(id, unknownId, List.of(), List.of(), probes("cov", "NO_DEFECT"),
                probes("reg", "NO_DEFECT")));
        Map<String, String> badStatus = defaultResolutions();
        badStatus.put("u1", "DONE");
        badReconcile.add(reconcile(id, badStatus, List.of(), List.of(), probes("cov", "NO_DEFECT"),
                probes("reg", "NO_DEFECT")));
        Map<String, Object> extra = reconcileMap(id, defaultResolutions(), List.of(), List.of(),
                probes("cov", "NO_DEFECT"), probes("reg", "NO_DEFECT"));
        extra.put("extra", "x");
        badReconcile.add(canon(extra));
        Map<String, Object> badProbe = reconcileMap(id, defaultResolutions(), List.of(), List.of(),
                probes("cov", "MAYBE"), probes("reg", "NO_DEFECT"));
        badReconcile.add(canon(badProbe));
        badReconcile.add(bytes("not json"));
        for (byte[] wire : badReconcile) {
            ScriptProvider provider = new ScriptProvider(reaudit(id, defaultCandidates()), wire);
            FakeStore store = new FakeStore();
            EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, BUDGET, provider, store);
            assertEquals(r.reasonCode(), EditorialL2Execution.StopClass.REPAIR_REQUIRED, r.stopClass());
            assertEquals("REPAIR_L3_RECONCILE_SCHEMA_INVALID", r.reasonCode());
            assertEquals(2, r.providerCalls());
            assertTrue(store.committed.isEmpty());
        }
    }

    @Test public void staleQaAnchorIsRepairRequired() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        FakeStore store = new FakeStore();
        EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, BUDGET,
                new ScriptProvider(reaudit(id, defaultCandidates()),
                        reconcile(id, defaultResolutions(),
                                List.of(qaChange("q1", 3, "WRONG BEFORE", "x", false, null)), List.of(),
                                probes("cov", "NO_DEFECT"), probes("reg", "NO_DEFECT"))), store);
        assertEquals(EditorialL3Execution.Outcome.STOPPED, r.outcome());
        assertEquals(EditorialL2Execution.StopClass.REPAIR_REQUIRED, r.stopClass());
        assertEquals("REPAIR_L3_QA_CHANGE_MAP_INVALID", r.reasonCode());
        boolean found = false;
        for (String issue : r.issues()) found |= issue.startsWith("CHANGE_ANCHOR_MISMATCH:");
        assertTrue(r.issues().toString(), found);
        assertTrue(store.committed.isEmpty());
    }

    @Test public void truncatedFirstCallMarksRecoveryAndNeverRedispatches() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        ScriptProvider provider = new ScriptProvider(reaudit(id, defaultCandidates()),
                reconcile(id, defaultResolutions(), List.of(), List.of(), probes("cov", "NO_DEFECT"),
                        probes("reg", "NO_DEFECT")));
        provider.finishes.put(0, "length");
        FakeStore store = new FakeStore();
        EditorialL3Execution exec = new EditorialL3Execution();
        EditorialL3Execution.Result r = exec.execute(req, BUDGET, provider, store);
        assertEquals(EditorialL2Execution.StopClass.RETRY_REQUIRED, r.stopClass());
        assertEquals("RETRY_L3_OUTPUT_TRUNCATED", r.reasonCode());
        assertEquals(1, r.providerCalls());
        assertEquals(1, provider.calls);
        assertTrue(store.recovery.containsKey(id));
        assertTrue(store.committed.isEmpty());

        EditorialL3Execution.Result later = exec.execute(req, BUDGET, provider, store);
        assertEquals(EditorialL2Execution.StopClass.RETRY_REQUIRED, later.stopClass());
        assertEquals("STOP_L3_EXTERNAL_CALL_STATE_UNRESOLVED", later.reasonCode());
        assertEquals(0, later.providerCalls());
        assertEquals(1, provider.calls);
    }

    @Test public void eachL3CallCarriesItsOwnOutputCapAndCostCap() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        EditorialL2Execution.Budget reaudit = new EditorialL2Execution.Budget(200_000, 8_192, new BigDecimal("0.05"), 180_000L);
        EditorialL2Execution.Budget reconcile = new EditorialL2Execution.Budget(200_000, 16_384, new BigDecimal("0.10"), 180_000L);
        ScriptProvider provider = new ScriptProvider(reaudit(id, defaultCandidates()),
                reconcile(id, defaultResolutions(), List.of(), List.of(), probes("cov", "NO_DEFECT"),
                        probes("reg", "NO_DEFECT")));
        EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, reaudit, reconcile, provider, new FakeStore());
        assertEquals(r.reasonCode() + r.issues(), EditorialL3Execution.Outcome.COMMITTED, r.outcome());
        assertEquals(8_192, provider.requests.get(0).maximumOutputTokens());
        assertEquals(16_384, provider.requests.get(1).maximumOutputTokens());

        // A call above its own cap is a budget stop even when the other cap would have allowed it.
        ScriptProvider pricey = new ScriptProvider(reaudit(id, defaultCandidates()),
                reconcile(id, defaultResolutions(), List.of(), List.of(), probes("cov", "NO_DEFECT"),
                        probes("reg", "NO_DEFECT")));
        pricey.cost = new BigDecimal("0.07");
        FakeStore store = new FakeStore();
        EditorialL3Execution.Result over = new EditorialL3Execution().execute(f.l3(), reaudit, reconcile, pricey, store);
        assertEquals(EditorialL2Execution.StopClass.BUDGET_EXCEEDED, over.stopClass());
        assertEquals(1, over.providerCalls());
        assertTrue(store.committed.isEmpty());

        EditorialL3Execution.Result noBudget = new EditorialL3Execution().execute(f.l3(), reaudit, null,
                new ScriptProvider(), new FakeStore());
        assertEquals(EditorialL2Execution.StopClass.AUTHORIZATION_REQUIRED, noBudget.stopClass());
    }

    @Test public void summedCostAcrossCallsExceedsMaximum() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        ScriptProvider provider = new ScriptProvider(reaudit(id, defaultCandidates()),
                reconcile(id, defaultResolutions(), List.of(), List.of(), probes("cov", "NO_DEFECT"),
                        probes("reg", "NO_DEFECT")));
        provider.cost = new BigDecimal("0.30"); // each under 0.50, sum 0.60 over it
        FakeStore store = new FakeStore();
        EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, BUDGET, provider, store);
        assertEquals(EditorialL2Execution.StopClass.BUDGET_EXCEEDED, r.stopClass());
        assertEquals("L3_TOKEN_OR_COST_BUDGET_EXCEEDED", r.reasonCode());
        assertEquals(2, r.providerCalls());
        assertEquals(2, provider.calls);
        assertTrue(store.committed.isEmpty());
    }

    @Test public void protectedLineAndDialogueWithoutProofAreReverted() {
        // Protected line 3: QA change reverted, FINAL keeps the VI_L2 line, release numbers zero.
        Fx f = fx(Set.of(3));
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        FakeStore store = new FakeStore();
        EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, BUDGET,
                new ScriptProvider(reaudit(id, defaultCandidates()),
                        reconcile(id, defaultResolutions(),
                                List.of(qaChange("q1", 3, "dong ba", "dong ba sua", false, null)), List.of(),
                                probes("cov", "NO_DEFECT"), probes("reg", "NO_DEFECT"))), store);
        assertEquals(r.reasonCode() + r.issues(), EditorialL3Execution.Outcome.COMMITTED, r.outcome());
        EditorialL2Execution.Committed stored = store.committed.get(id);
        assertArrayEquals(bytes(VI_L2), stored.viL2Bytes());
        assertTrue(r.releaseNumbers().releasable());
        assertEquals(0, r.releaseNumbers().protectedSpanRegressions());
        assertEquals(0, r.releaseNumbers().unaccountedChangedAnchors());
        assertEquals("PROTECTED_SPAN_TOUCHED", revertReason(stored));

        // Dialogue change without complete Speaker Proof is reverted too.
        Fx g = fx(Set.of());
        EditorialL3Execution.Request req2 = g.l3();
        String id2 = req2.attemptIdentity();
        FakeStore store2 = new FakeStore();
        EditorialL3Execution.Result r2 = new EditorialL3Execution().execute(req2, BUDGET,
                new ScriptProvider(reaudit(id2, defaultCandidates()),
                        reconcile(id2, defaultResolutions(),
                                List.of(qaChange("q1", 3, "dong ba", "dong ba sua", true, null)), List.of(),
                                probes("cov", "NO_DEFECT"), probes("reg", "NO_DEFECT"))), store2);
        assertEquals(r2.reasonCode() + r2.issues(), EditorialL3Execution.Outcome.COMMITTED, r2.outcome());
        EditorialL2Execution.Committed stored2 = store2.committed.get(id2);
        assertArrayEquals(bytes(VI_L2), stored2.viL2Bytes());
        assertEquals("SPEAKER_PROOF_MISSING", revertReason(stored2));
    }

    @Test public void commitFailureIsRetryRequiredAndNothingCommitted() {
        Fx f = fx(Set.of());
        EditorialL3Execution.Request req = f.l3();
        String id = req.attemptIdentity();
        FakeStore store = new FakeStore();
        store.failCommit = true;
        EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, BUDGET,
                new ScriptProvider(reaudit(id, defaultCandidates()),
                        reconcile(id, defaultResolutions(), List.of(), List.of(), probes("cov", "NO_DEFECT"),
                                probes("reg", "NO_DEFECT"))), store);
        assertEquals(EditorialL2Execution.StopClass.RETRY_REQUIRED, r.stopClass());
        assertEquals("RETRY_L3_ATOMIC_COMMIT_FAILED", r.reasonCode());
        assertTrue(store.findCommitted(id).isEmpty());
        assertTrue(store.recovery.containsKey(id));
    }

    // ---- helpers ----

    private static void gate(EditorialL3Execution.Request req, String reason) {
        ScriptProvider provider = new ScriptProvider(bytes("{}"));
        FakeStore store = new FakeStore();
        EditorialL3Execution.Result r = new EditorialL3Execution().execute(req, BUDGET, provider, store);
        assertEquals(r.reasonCode(), EditorialL3Execution.Outcome.STOPPED, r.outcome());
        assertEquals(EditorialL2Execution.StopClass.INPUT_REQUIRED, r.stopClass());
        assertEquals(reason, r.reasonCode());
        assertEquals(0, r.providerCalls());
        assertEquals(0, provider.calls);
        assertTrue(store.committed.isEmpty());
    }

    private static void assertConflict(EditorialL3Execution.Result r, FakeStore store) {
        assertEquals(EditorialL3Execution.Outcome.STOPPED, r.outcome());
        assertEquals(EditorialL2Execution.StopClass.CONTENT_BLOCKED, r.stopClass());
        assertEquals("CONTENT_L3_PROVEN_CONFLICT_UNRESOLVED", r.reasonCode());
        assertNull(r.committed());
        assertTrue(store.committed.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static String revertReason(EditorialL2Execution.Committed stored) {
        Map<String, Object> receipt = EditorialCanonicalJson.parseObject(stored.changeMapBytes());
        Map<String, Object> qa = (Map<String, Object>) receipt.get("qaChangeMap");
        List<Object> rows = (List<Object>) qa.get("changes");
        assertEquals(1, rows.size());
        Map<String, Object> row = (Map<String, Object>) rows.get(0);
        assertEquals("REVERTED", row.get("status"));
        return (String) row.get("revertReason");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> obj(Object value) { return (Map<String, Object>) value; }

    private record Cand(String id, String ledger, int line, String status) { }

    private static Cand cand(String id, String ledger, int line, String status) {
        return new Cand(id, ledger, line, status);
    }

    private static List<Cand> defaultCandidates() {
        return List.of(cand("u1", "UNIT", 2, "UNPROCESSED"), cand("u2", "UNIT", 3, "PROCESSED"),
                cand("t1", "TG", 1, "UNPROCESSED"), cand("s1", "SR", 2, "UNPROCESSED"));
    }

    private static Map<String, String> defaultResolutions() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("u1", "PROCESSED");
        m.put("t1", "PRESERVED");
        m.put("s1", "PROCESSED");
        return m;
    }

    private static Map<String, Object> reauditMap(String attempt, List<Cand> candidates) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("wireSchemaVersion", EditorialL3Execution.REAUDIT_WIRE);
        root.put("attemptIdentity", attempt);
        List<Object> rows = new ArrayList<>();
        for (Cand c : candidates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("candidateId", c.id());
            row.put("ledger", c.ledger());
            row.put("line", BigDecimal.valueOf(c.line()));
            row.put("status", c.status());
            rows.add(row);
        }
        root.put("candidates", rows);
        return root;
    }

    private static byte[] reaudit(String attempt, List<Cand> candidates) {
        return canon(reauditMap(attempt, candidates));
    }

    private static Map<String, Object> reconcileMap(String attempt, Map<String, String> resolutions,
                                                    List<Map<String, Object>> changes,
                                                    List<Map<String, Object>> preserved,
                                                    List<Map<String, Object>> coverage,
                                                    List<Map<String, Object>> regression) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("wireSchemaVersion", EditorialL3Execution.RECONCILE_WIRE);
        root.put("attemptIdentity", attempt);
        List<Object> res = new ArrayList<>();
        for (Map.Entry<String, String> e : resolutions.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("candidateId", e.getKey());
            row.put("status", e.getValue());
            res.add(row);
        }
        root.put("resolutions", res);
        root.put("changes", new ArrayList<Object>(changes));
        root.put("preserved", new ArrayList<Object>(preserved));
        root.put("adversarialCoverage", new ArrayList<Object>(coverage));
        root.put("adversarialRegression", new ArrayList<Object>(regression));
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("disposition", "CONTINUE");
        d.put("reasonCode", "OK");
        d.put("stopClass", "NONE");
        root.put("disposition", d);
        return root;
    }

    private static byte[] reconcile(String attempt, Map<String, String> resolutions,
                                    List<Map<String, Object>> changes, List<Map<String, Object>> preserved,
                                    List<Map<String, Object>> coverage, List<Map<String, Object>> regression) {
        return canon(reconcileMap(attempt, resolutions, changes, preserved, coverage, regression));
    }

    private static List<Map<String, Object>> probes(String id, String verdict) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("probeId", id + "-1");
        m.put("finding", "tried an adversarial reading");
        m.put("verdict", verdict);
        return List.of(m);
    }

    private static Map<String, Object> qaChange(String id, int line, String before, String after,
                                                boolean dialogue, Map<String, Object> proof) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("changeId", id);
        m.put("errorId", "err-" + id);
        m.put("line", BigDecimal.valueOf(line));
        m.put("before", before);
        m.put("after", after);
        m.put("reason", "qa wording");
        m.put("dialogue", dialogue);
        if (proof != null) m.put("speakerProof", proof);
        m.put("status", "CLOSED");
        return m;
    }

    private static Map<String, Object> preserved(String id, int line, String before) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("preserveId", id);
        m.put("line", BigDecimal.valueOf(line));
        m.put("before", before);
        m.put("evidenceLimit", "kept: no raw evidence to change");
        return m;
    }

    private static byte[] canon(Map<String, Object> m) {
        return EditorialCanonicalJson.canonicalize(m).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] report(EditorialP5PilotRequest context, String phase) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("artifactType", "REPORT_L1");
        m.put("schemaVersion", EditorialP5RawWireContract.FINAL_REPORT_SCHEMA);
        m.put("phase", phase);
        m.put("bindingIdentity", context.binding().bindingIdentity());
        m.put("canonicalPackHash", context.binding().canonicalPackHash());
        m.put("manifestFingerprint", context.manifestFingerprint());
        m.put("chapterKey", context.chapterKey());
        m.put("bundleIdentity", context.bundleIdentity());
        m.put("disposition", "CONTINUE");
        return canon(m);
    }

    /** Valid L1 context, REPORT_L1 and a real app-generated committed L2 (one CLOSED change on line 2). */
    private static final class Fx {
        final EditorialP5PilotRequest context;
        final byte[] reportBytes;
        final EditorialL2Execution.Committed l2;
        final Set<Integer> protectedLines;

        Fx(Set<Integer> protectedLines) {
            this.protectedLines = protectedLines;
            this.context = ctx();
            this.reportBytes = report(context, "L1_RECONCILE");
            EditorialL2Execution.Request l2Request = new EditorialL2Execution.Request(context, L1_ID, reportBytes,
                    Set.of());
            Map<String, Object> change = new LinkedHashMap<>();
            change.put("changeId", "c1");
            change.put("errorId", "err-c1");
            change.put("line", BigDecimal.valueOf(2));
            change.put("before", "dong hai");
            change.put("after", "dong hai sua");
            change.put("reason", "fix wording");
            change.put("dialogue", false);
            change.put("status", "CLOSED");
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("wireSchemaVersion", EditorialL2Execution.WIRE_SCHEMA_VERSION);
            root.put("attemptIdentity", l2Request.attemptIdentity());
            List<Object> l2Resolutions = new ArrayList<>();
            List<Object> l2Candidates = new ArrayList<>();
            for (String[] c : new String[][]{{"U001", "UNIT"}, {"TG001", "TG"}}) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("candidateId", c[0]);
                r.put("status", "PROCESSED");
                l2Resolutions.add(r);
                Map<String, Object> cand = new LinkedHashMap<>();
                cand.put("candidateId", c[0]);
                cand.put("ledger", c[1]);
                cand.put("line", BigDecimal.ONE);
                l2Candidates.add(cand);
            }
            root.put("resolutions", l2Resolutions);
            root.put("changes", new ArrayList<Object>(List.of(change)));
            root.put("preserved", new ArrayList<Object>());
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("disposition", "CONTINUE");
            d.put("reasonCode", "OK");
            d.put("stopClass", "NONE");
            root.put("disposition", d);
            final byte[] l2Wire = canon(root);
            Map<String, Object> discovery = new LinkedHashMap<>();
            discovery.put("wireSchemaVersion", EditorialL2Execution.DISCOVERY_WIRE);
            discovery.put("attemptIdentity", l2Request.attemptIdentity());
            discovery.put("candidates", l2Candidates);
            final byte[] l2Discovery = canon(discovery);
            EditorialL2Execution.Provider l2Provider = request ->
                    new EditorialL2Execution.Provider.Response(
                            EditorialL2Execution.DISCOVERY_PHASE.equals(request.phase()) ? l2Discovery : l2Wire,
                            "stop", true, 100, 50, new BigDecimal("0.01"), true);
            EditorialL2Execution.Result l2Result = new EditorialL2Execution().execute(l2Request,
                    BUDGET, BUDGET, l2Provider, new FakeStore());
            if (l2Result.outcome() != EditorialL2Execution.Outcome.COMMITTED) {
                throw new AssertionError("L2 fixture failed: " + l2Result.reasonCode() + l2Result.issues());
            }
            this.l2 = l2Result.committed();
            assertArrayEquals(bytes(VI_L2), l2.viL2Bytes());
        }

        EditorialL3Execution.Request l3() {
            return new EditorialL3Execution.Request(context, L1_ID, reportBytes, l2, protectedLines);
        }
    }

    private static Fx fx(Set<Integer> protectedLines) { return new Fx(protectedLines); }

    /** Scripted per-call responses (index 0 = re-audit, 1 = reconcile); records every request. */
    private static final class ScriptProvider implements EditorialL2Execution.Provider {
        final List<byte[]> script;
        final List<Request> requests = new ArrayList<>();
        final Map<Integer, String> finishes = new HashMap<>();
        BigDecimal cost = new BigDecimal("0.01");
        int calls;

        ScriptProvider(byte[]... script) { this.script = List.of(script); }

        @Override public Response call(Request request) {
            int index = calls++;
            requests.add(request);
            byte[] wire = script.get(Math.min(index, script.size() - 1));
            return new Response(wire, finishes.getOrDefault(index, "stop"), true, 100, 50, cost, true);
        }
    }

    private static final class FakeStore implements EditorialL2Execution.Store {
        final Map<String, EditorialL2Execution.Committed> committed = new HashMap<>();
        final Map<String, String> recovery = new HashMap<>();
        final Map<String, Boolean> inFlight = new HashMap<>();
        boolean failCommit;
        @Override public Claim claim(String attempt, String predecessor, String bundle) {
            if (committed.containsKey(attempt)) return Claim.ALREADY_COMMITTED;
            if (recovery.containsKey(attempt)) return Claim.RECOVERY_REQUIRED;
            if (inFlight.containsKey(attempt)) return Claim.IN_FLIGHT;
            inFlight.put(attempt, true);
            return Claim.ACQUIRED;
        }
        @Override public void commit(EditorialL2Execution.Committed value) {
            if (failCommit) throw new IllegalStateException("commit failed");
            committed.put(value.attemptIdentity(), value);
        }
        @Override public Optional<EditorialL2Execution.Committed> findCommitted(String attempt) {
            return Optional.ofNullable(committed.get(attempt));
        }
        @Override public void markRecoveryRequired(String attempt, String reason) {
            recovery.put(attempt, reason);
        }
    }

    // ---- fixture (copied minimal helpers from EditorialL2ExecutionTest) ----

    private static EditorialP5PilotRequest ctx() {
        Map<EditorialPackFileRole, byte[]> authority = new LinkedHashMap<>();
        authority.put(EditorialPackFileRole.PROJECT_INSTRUCTION, PROJECT);
        authority.put(EditorialPackFileRole.TURN_PROMPT, PROMPT);
        authority.put(EditorialPackFileRole.WORKFLOW, WORKFLOW);
        EditorialPackManifest manifest = manifest(authority);
        List<EditorialP5PilotRequest.SourceBytes> sources = List.of(
                source(EditorialSafe4Contract.RAW, "raw", "raw chapter"),
                source(EditorialSafe4Contract.DRAFT, "draft", DRAFT),
                source(EditorialSafe4Contract.GLOSSARY, "glossary", "term\ttarget"),
                source(EditorialSafe4Contract.PRONOUN, "pronoun", "from\ttarget"));
        EditorialP4Binding binding = binding(manifest, sources);
        return new EditorialP5PilotRequest(binding, manifest,
                new EditorialP5PilotRequest.PackAuthority(authority), "chapter-1",
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY, sources, "root-predecessor",
                List.of("anchor-1"), List.of("raw-1"), true, 500);
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
        try (java.io.InputStream input = EditorialL3ExecutionTest.class
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
}
