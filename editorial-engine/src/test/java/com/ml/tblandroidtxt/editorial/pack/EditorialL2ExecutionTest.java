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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** L2_EDIT boundary tests; provider and store are in-memory fakes. */
public final class EditorialL2ExecutionTest {
    private static final byte[] PROJECT = bytes("project authority");
    private static final byte[] PROMPT = bytes("prompt authority");
    private static final byte[] WORKFLOW = bytes("workflow authority");
    private static final String DRAFT = "dong mot\ndong hai\ndong ba\n";
    private static final String L1_ID = "l1-attempt-identity-1";
    private static final EditorialL2Execution.Budget BUDGET = new EditorialL2Execution.Budget(
            100_000, 4_000, new BigDecimal("0.50"), 60_000L);

    // ---- cases ----

    @Test public void validClosedChangeCommitsReconstructedViL2() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeProvider provider = new FakeProvider(wire(req, List.of(change("c1", 2, "dong hai", "dong hai sua",
                false, null, "CLOSED")), "CONTINUE", "NONE"));
        FakeStore store = new FakeStore();

        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET, provider, store);

        assertEquals(r.reasonCode() + r.issues(), EditorialL2Execution.Outcome.COMMITTED, r.outcome());
        assertEquals(2, r.providerCalls());
        assertEquals(2, provider.calls);
        EditorialL2Execution.Committed stored = store.committed.get(req.attemptIdentity());
        assertArrayEquals(bytes("dong mot\ndong hai sua\ndong ba\n"), stored.viL2Bytes());
        Map<String, Object> map = EditorialCanonicalJson.parseObject(stored.changeMapBytes());
        assertEquals("CHANGE_MAP_L2", map.get("artifactType"));
        assertEquals(L1_ID, map.get("predecessorIdentity"));
        assertEquals(L1_ID, stored.predecessorIdentity());
        assertEquals(EditorialCanonicalJson.sha256Hex(stored.viL2Bytes()), stored.viL2Sha256());
        assertEquals(stored.viL2Sha256(), store.findCommitted(req.attemptIdentity()).get().viL2Sha256());
        assertEquals(r.committed().viL2Sha256(), stored.viL2Sha256());
    }

    @Test public void noEditWireCommitsViL2EqualToDraft() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeStore store = new FakeStore();
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET,
                new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE")), store);
        assertEquals(r.reasonCode(), EditorialL2Execution.Outcome.COMMITTED, r.outcome());
        assertArrayEquals(bytes(DRAFT), store.committed.get(req.attemptIdentity()).viL2Bytes());
    }

    @Test public void secondExecuteIsAlreadyCommittedWithoutProviderCall() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeProvider provider = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
        FakeStore store = new FakeStore();
        EditorialL2Execution exec = new EditorialL2Execution();
        assertEquals(EditorialL2Execution.Outcome.COMMITTED, exec.execute(req, BUDGET, BUDGET, provider, store).outcome());
        EditorialL2Execution.Result again = exec.execute(req, BUDGET, BUDGET, provider, store);
        assertEquals(EditorialL2Execution.Outcome.ALREADY_COMMITTED, again.outcome());
        assertEquals(0, again.providerCalls());
        assertEquals(2, provider.calls);
    }

    @Test public void predecessorGatesStopBeforeProvider() {
        Ctx c = ctx();
        EditorialP5PilotRequest ctxReq = c.request;
        assertGate(ctxReq, null, "INPUT_REPORT_L1_REQUIRED");
        assertGate(ctxReq, report(ctxReq, "L1_RAW_DISCOVERY", null, null, "CONTINUE"),
                "INPUT_REPORT_L1_PHASE_INVALID");
        assertGate(ctxReq, report(ctxReq, "L1_RECONCILE", "f".repeat(64), null, "CONTINUE"),
                "INPUT_REPORT_L1_IDENTITY_MISMATCH");
        assertGate(ctxReq, report(ctxReq, "L1_RECONCILE", null, "e".repeat(64), "CONTINUE"),
                "INPUT_REPORT_L1_SOURCE_DRIFT");
        assertGate(ctxReq, report(ctxReq, "L1_RECONCILE", null, null, "STOP"),
                "INPUT_REPORT_L1_DISPOSITION_INVALID");
    }

    @Test public void wireFailuresCarrySafeIndexedPaths() {
        Map<String, Object> badChange = new LinkedHashMap<>();
        badChange.put("changeId", "c1"); badChange.put("errorId", "e1"); badChange.put("line", BigDecimal.ONE);
        badChange.put("before", "before"); badChange.put("after", "after"); badChange.put("reason", BigDecimal.ONE);
        badChange.put("dialogue", false); badChange.put("status", "CLOSED");
        Map<String, Object> disposition = new LinkedHashMap<>();
        disposition.put("disposition", "CONTINUE"); disposition.put("reasonCode", "OK"); disposition.put("stopClass", "NONE");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("wireSchemaVersion", EditorialL2Execution.WIRE_SCHEMA_VERSION); root.put("attemptIdentity", L1_ID);
        root.put("changes", new ArrayList<>(List.of(badChange))); root.put("preserved", new ArrayList<>());
        root.put("disposition", disposition);
        try {
            EditorialL2Execution.parseWire(canon(root), L1_ID);
            throw new AssertionError("expected reason text violation");
        } catch (RuntimeException invalid) {
            assertEquals("L2_WIRE_TEXT_INVALID:changes.0.reason", WireViolation.safeMessage(invalid, "L2_WIRE_PARSE_FAILED"));
        }

        badChange.put("untrustedModelKey", "secret-value");
        try {
            EditorialL2Execution.parseWire(canon(root), L1_ID);
            throw new AssertionError("expected unknown key violation");
        } catch (RuntimeException invalid) {
            String safe = WireViolation.safeMessage(invalid, "L2_WIRE_PARSE_FAILED");
            assertEquals("L2_WIRE_UNKNOWN_KEY:changes.0", safe);
            assertFalse(safe.contains("untrustedModelKey"));
            assertFalse(safe.contains("secret-value"));
        }
    }

    @Test public void truncatedOutputMarksRecoveryAndNeverRedispatches() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeProvider provider = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
        provider.finish = "length";
        FakeStore store = new FakeStore();
        EditorialL2Execution exec = new EditorialL2Execution();
        EditorialL2Execution.Result r = exec.execute(req, BUDGET, BUDGET, provider, store);
        assertStop(r, EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L2_OUTPUT_TRUNCATED");
        assertEquals(1, r.providerCalls());
        assertTrue(store.recovery.containsKey(req.attemptIdentity()));
        assertTrue(store.committed.isEmpty());
        provider.finish = "stop";
        EditorialL2Execution.Result later = exec.execute(req, BUDGET, BUDGET, provider, store);
        assertStop(later, EditorialL2Execution.StopClass.RETRY_REQUIRED, "STOP_L2_EXTERNAL_CALL_STATE_UNRESOLVED");
        assertEquals(0, later.providerCalls());
        assertEquals(1, provider.calls);
    }

    @Test public void costAboveBudgetAndUnknownCost() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeProvider expensive = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
        expensive.cost = new BigDecimal("9.99");
        FakeStore store = new FakeStore();
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET, expensive, store);
        assertEquals(EditorialL2Execution.StopClass.BUDGET_EXCEEDED, r.stopClass());
        assertTrue(store.committed.isEmpty());

        FakeProvider unknown = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
        unknown.costKnown = false;
        FakeStore store2 = new FakeStore();
        EditorialL2Execution.Result u = new EditorialL2Execution().execute(req, BUDGET, BUDGET, unknown, store2);
        assertStop(u, EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L2_PROVIDER_COST_UNAVAILABLE");
        assertTrue(store2.committed.isEmpty());
    }

    @Test public void malformedWiresAreRepairRequiredAndNothingCommitted() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        List<byte[]> bad = new ArrayList<>();
        Map<String, Object> unknownKey = wireMap(req, List.of(), "CONTINUE", "NONE");
        unknownKey.put("extra", "x");
        bad.add(canon(unknownKey));
        Map<String, Object> wrongEcho = wireMap(req, List.of(), "CONTINUE", "NONE");
        wrongEcho.put("attemptIdentity", "0".repeat(64));
        bad.add(canon(wrongEcho));
        Map<String, Object> wrongSchema = wireMap(req, List.of(), "CONTINUE", "NONE");
        wrongSchema.put("wireSchemaVersion", "safe4.l2.edit.wire.v0");
        bad.add(canon(wrongSchema));
        Map<String, Object> badStatus = wireMap(req, List.of(change("c1", 2, "dong hai", "x", false, null, "OPEN")),
                "CONTINUE", "NONE");
        bad.add(canon(badStatus));
        bad.add(bytes("not json"));
        for (byte[] wireBytes : bad) {
            FakeStore store = new FakeStore();
            EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET,
                    new FakeProvider(wireBytes), store);
            assertStop(r, EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L2_OUTPUT_SCHEMA_INVALID");
            assertTrue(store.committed.isEmpty());
        }
    }

    @Test public void anchorMismatchIsRepairRequired() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeStore store = new FakeStore();
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET,
                new FakeProvider(wire(req, List.of(change("c1", 2, "WRONG BEFORE", "x", false, null, "CLOSED")),
                        "CONTINUE", "NONE")), store);
        assertStop(r, EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L2_CHANGE_MAP_INVALID");
        boolean found = false;
        for (String issue : r.issues()) found |= issue.startsWith("CHANGE_ANCHOR_MISMATCH:");
        assertTrue(r.issues().toString(), found);
        assertTrue(store.committed.isEmpty());
    }

    @Test public void dialogueWithoutSpeakerProofIsRevertedAndRecorded() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeStore store = new FakeStore();
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET,
                new FakeProvider(wire(req, List.of(change("c1", 2, "dong hai", "sua", true, null, "CLOSED")),
                        "CONTINUE", "NONE")), store);
        assertEquals(r.reasonCode() + r.issues(), EditorialL2Execution.Outcome.COMMITTED, r.outcome());
        EditorialL2Execution.Committed stored = store.committed.get(req.attemptIdentity());
        assertArrayEquals(bytes(DRAFT), stored.viL2Bytes());
        Map<String, Object> map = EditorialCanonicalJson.parseObject(stored.changeMapBytes());
        @SuppressWarnings("unchecked") List<Object> rows = (List<Object>) map.get("changes");
        assertEquals(1, rows.size());
        @SuppressWarnings("unchecked") Map<String, Object> row = (Map<String, Object>) rows.get(0);
        assertEquals("REVERTED", row.get("status"));
        assertEquals("SPEAKER_PROOF_MISSING", row.get("revertReason"));
    }

    @Test public void modelStopContentBlockedAndRejectedStopClass() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeStore store = new FakeStore();
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET,
                new FakeProvider(wire(req, List.of(), "STOP", "CONTENT_BLOCKED")), store);
        assertEquals(EditorialL2Execution.Outcome.STOPPED, r.outcome());
        assertEquals(EditorialL2Execution.StopClass.CONTENT_BLOCKED, r.stopClass());
        assertTrue(store.committed.isEmpty());

        FakeStore store2 = new FakeStore();
        EditorialL2Execution.Result bad = new EditorialL2Execution().execute(req, BUDGET, BUDGET,
                new FakeProvider(wire(req, List.of(), "STOP", "REPAIR_REQUIRED")), store2);
        assertStop(bad, EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L2_OUTPUT_SCHEMA_INVALID");
        assertTrue(store2.committed.isEmpty());
    }

    @Test public void commitFailureAndReadbackMismatch() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeStore failing = new FakeStore();
        failing.failCommit = true;
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET,
                new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE")), failing);
        assertStop(r, EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L2_ATOMIC_COMMIT_FAILED");
        assertTrue(failing.findCommitted(req.attemptIdentity()).isEmpty());

        FakeStore mismatch = new FakeStore();
        mismatch.corruptReadback = true;
        EditorialL2Execution.Result m = new EditorialL2Execution().execute(req, BUDGET, BUDGET,
                new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE")), mismatch);
        assertStop(m, EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L2_READBACK_MISMATCH");
    }

    @Test public void inputBudgetAndVisibleSources() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeProvider tiny = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req,
                new EditorialL2Execution.Budget(10, 4_000, BigDecimal.ONE, 60_000L),
                new EditorialL2Execution.Budget(10, 4_000, BigDecimal.ONE, 60_000L), tiny, new FakeStore());
        assertStop(r, EditorialL2Execution.StopClass.BUDGET_EXCEEDED, "L2_INPUT_BUDGET_EXCEEDED");
        assertEquals(0, r.providerCalls());
        assertEquals(0, tiny.calls);

        FakeProvider ok = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
        new EditorialL2Execution().execute(req, BUDGET, BUDGET, ok, new FakeStore());
        Set<String> keys = ok.lastRequest.visibleSources().keySet();
        assertFalse(keys.contains("VI_L2"));
        assertTrue(keys.contains(EditorialSafe4Contract.RAW));
        assertTrue(keys.contains(EditorialSafe4Contract.DRAFT));
        assertTrue(keys.contains(EditorialSafe4Contract.GLOSSARY));
        assertTrue(keys.contains("REPORT_L1"));
        // Fixture binding declares pronounStatus AVAILABLE; PRONOUN is visible only if authoritative.
        boolean pronounAuthoritative = req.context().bundleForExecution().assets().stream()
                .anyMatch(a -> EditorialSafe4Contract.PRONOUN.equals(a.role()) && a.authoritative());
        assertEquals(keys.toString(), pronounAuthoritative, keys.contains(EditorialSafe4Contract.PRONOUN));
        assertTrue(keys.toString(), keys.contains(EditorialL2Execution.CANDIDATES_BLOCK));
        assertEquals(keys.toString(), pronounAuthoritative ? 6 : 5, keys.size());
    }

    @Test public void discoveryIsBlindAndEditReceivesTheAppOwnedCandidateBlock() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeProvider provider = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET, provider, new FakeStore());
        assertEquals(r.reasonCode(), EditorialL2Execution.Outcome.COMMITTED, r.outcome());
        assertEquals(2, provider.requests.size());

        EditorialL2Execution.Provider.Request first = provider.requests.get(0);
        assertEquals(EditorialL2Execution.DISCOVERY_PHASE, first.phase());
        assertEquals(EditorialL2Execution.DISCOVERY_WIRE, first.outputSchemaId());
        assertEquals(Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY), first.visibleSources().keySet());

        EditorialL2Execution.Provider.Request second = provider.requests.get(1);
        assertEquals(EditorialL2Execution.PHASE, second.phase());
        assertTrue(second.visibleSources().containsKey(EditorialL2Execution.CANDIDATES_BLOCK));
        Map<String, Object> block = EditorialCanonicalJson.parseObject(second.visibleSources().get(EditorialL2Execution.CANDIDATES_BLOCK));
        assertEquals(2, ((List<?>) block.get("candidates")).size());
        assertFalse(second.visibleSources().containsKey("VI_L2"));
        assertEquals(first.attemptIdentity(), second.attemptIdentity());
    }

    @Test public void committedChangeMapRecordsTheDiscoveryAndKeepsTheL3Fields() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeStore store = new FakeStore();
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET,
                new FakeProvider(wire(req, List.of(change("c1", 2, "dong hai", "dong hai sua", false, null, "CLOSED")),
                        "CONTINUE", "NONE")), store);
        assertEquals(r.reasonCode(), EditorialL2Execution.Outcome.COMMITTED, r.outcome());
        Map<String, Object> map = EditorialCanonicalJson.parseObject(store.committed.get(req.attemptIdentity()).changeMapBytes());
        assertEquals("CHANGE_MAP_L2", map.get("artifactType"));
        assertEquals(EditorialCanonicalJson.sha256Hex(bytes(DRAFT)), map.get("baseSha256"));
        assertEquals(store.committed.get(req.attemptIdentity()).viL2Sha256(), map.get("outputSha256"));
        @SuppressWarnings("unchecked") Map<String, Object> discovery = (Map<String, Object>) map.get("rawDiscovery");
        assertEquals(EditorialL2Execution.DISCOVERY_PHASE, discovery.get("phase"));
        assertEquals(new BigDecimal(2), discovery.get("candidateCount"));
        @SuppressWarnings("unchecked") Map<String, Object> ledgers = (Map<String, Object>) discovery.get("ledgers");
        @SuppressWarnings("unchecked") Map<String, Object> unit = (Map<String, Object>) ledgers.get("UNIT");
        assertEquals(new BigDecimal(1), unit.get("PROCESSED"));
        assertEquals(64, String.valueOf(discovery.get("candidatesSha256")).length());
    }

    @Test public void discoveryWithoutRawUnitsOrWithBadRowsIsRepairRequiredAfterOneCall() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        String attempt = req.attemptIdentity();
        List<byte[]> bad = new ArrayList<>();
        bad.add(discovery(attempt, List.of(candidate("TG001", "TG", 1))));
        bad.add(discovery(attempt, List.of(candidate("U001", "UNIT", 1), candidate("U001", "TG", 1))));
        bad.add(discovery(attempt, List.of(candidate("U001", "UNIT", 99))));
        bad.add(discovery(attempt, List.of(candidate("U001", "WRONG", 1))));
        bad.add(discovery("0".repeat(64), List.of(candidate("U001", "UNIT", 1))));
        Map<String, Object> extra = EditorialCanonicalJson.parseObject(discovery(attempt, List.of(candidate("U001", "UNIT", 1))));
        extra = new LinkedHashMap<>(extra);
        extra.put("note", "x");
        bad.add(canon(extra));
        bad.add(bytes("not json"));
        for (byte[] discoveryBytes : bad) {
            FakeProvider provider = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
            provider.discoveryWire = discoveryBytes;
            FakeStore store = new FakeStore();
            EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET, provider, store);
            assertStop(r, EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L2_DISCOVERY_SCHEMA_INVALID");
            assertEquals(1, r.providerCalls());
            assertEquals(1, provider.calls);
            assertTrue(store.committed.isEmpty());
            assertTrue(store.recovery.containsKey(attempt));
        }
    }

    @Test public void candidateResolutionsAreCountedByTheApp() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();

        Map<String, Object> missing = wireMap(req, List.of(), "CONTINUE", "NONE");
        missing.put("resolutions", new ArrayList<Object>(resolutions("PROCESSED").subList(0, 1)));
        assertStop(run(req, canon(missing)), EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L2_RESOLUTIONS_INCOMPLETE");

        Map<String, Object> unknown = wireMap(req, List.of(), "CONTINUE", "NONE");
        List<Object> withUnknown = resolutions("PROCESSED");
        Map<String, Object> ghost = new LinkedHashMap<>();
        ghost.put("candidateId", "NOPE");
        ghost.put("status", "PROCESSED");
        withUnknown.add(ghost);
        unknown.put("resolutions", withUnknown);
        assertStop(run(req, canon(unknown)), EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L2_OUTPUT_SCHEMA_INVALID");

        Map<String, Object> duplicate = wireMap(req, List.of(), "CONTINUE", "NONE");
        List<Object> twice = resolutions("PROCESSED");
        twice.add(resolutions("PROCESSED").get(0));
        duplicate.put("resolutions", twice);
        assertStop(run(req, canon(duplicate)), EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L2_OUTPUT_SCHEMA_INVALID");

        Map<String, Object> badStatus = wireMap(req, List.of(), "CONTINUE", "NONE");
        badStatus.put("resolutions", resolutions("DONE"));
        assertStop(run(req, canon(badStatus)), EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L2_OUTPUT_SCHEMA_INVALID");

        Map<String, Object> unprocessed = wireMap(req, List.of(), "CONTINUE", "NONE");
        unprocessed.put("resolutions", resolutions("UNPROCESSED"));
        EditorialL2Execution.Result u = run(req, canon(unprocessed));
        assertStop(u, EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L2_CANDIDATES_UNPROCESSED");
        assertEquals(2, u.providerCalls());

        Map<String, Object> conflict = wireMap(req, List.of(), "CONTINUE", "NONE");
        conflict.put("resolutions", resolutions("CONFLICT"));
        assertStop(run(req, canon(conflict)), EditorialL2Execution.StopClass.CONTENT_BLOCKED, "CONTENT_L2_CANDIDATE_CONFLICT");

        // A proven conflict outranks an unprocessed candidate.
        Map<String, Object> mixed = wireMap(req, List.of(), "CONTINUE", "NONE");
        List<Object> mixedRows = resolutions("UNPROCESSED");
        ((Map<String, Object>) mixedRows.get(1)).put("status", "CONFLICT");
        mixed.put("resolutions", mixedRows);
        assertStop(run(req, canon(mixed)), EditorialL2Execution.StopClass.CONTENT_BLOCKED, "CONTENT_L2_CANDIDATE_CONFLICT");

        // PRESERVED is a resolved state, not a failure.
        Map<String, Object> preserved = wireMap(req, List.of(), "CONTINUE", "NONE");
        preserved.put("resolutions", resolutions("PRESERVED"));
        assertEquals(EditorialL2Execution.Outcome.COMMITTED, run(req, canon(preserved)).outcome());
    }

    @Test public void modelStopMayOmitResolutions() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        Map<String, Object> stop = wireMap(req, List.of(), "STOP", "CONTENT_BLOCKED");
        stop.put("resolutions", new ArrayList<Object>());
        EditorialL2Execution.Result r = run(req, canon(stop));
        assertEquals(EditorialL2Execution.StopClass.CONTENT_BLOCKED, r.stopClass());
        assertEquals(2, r.providerCalls());
    }

    @Test public void editCallFailureLeavesRecoveryAndResumeNeverRedispatches() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        FakeProvider provider = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
        provider.failPhase = EditorialL2Execution.PHASE;
        FakeStore store = new FakeStore();
        EditorialL2Execution exec = new EditorialL2Execution();
        EditorialL2Execution.Result r = exec.execute(req, BUDGET, BUDGET, provider, store);
        assertStop(r, EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L2_PROVIDER_CALL_FAILED");
        assertEquals(2, r.providerCalls());
        provider.failPhase = null;
        EditorialL2Execution.Result later = exec.execute(req, BUDGET, BUDGET, provider, store);
        assertStop(later, EditorialL2Execution.StopClass.RETRY_REQUIRED, "STOP_L2_EXTERNAL_CALL_STATE_UNRESOLVED");
        assertEquals(0, later.providerCalls());
        assertEquals(2, provider.calls);
    }

    @Test public void eachCallHasItsOwnBudget() {
        Ctx c = ctx();
        EditorialL2Execution.Request req = c.request();
        EditorialL2Execution.Budget cheap = new EditorialL2Execution.Budget(100_000, 4_000, new BigDecimal("0.005"), 60_000L);
        FakeProvider provider = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
        provider.cost = new BigDecimal("0.01");
        EditorialL2Execution.Result discoveryOver = new EditorialL2Execution().execute(req, cheap, BUDGET, provider, new FakeStore());
        assertStop(discoveryOver, EditorialL2Execution.StopClass.BUDGET_EXCEEDED, "L2_TOKEN_OR_COST_BUDGET_EXCEEDED");
        assertEquals(1, discoveryOver.providerCalls());

        FakeProvider second = new FakeProvider(wire(req, List.of(), "CONTINUE", "NONE"));
        second.cost = new BigDecimal("0.01");
        EditorialL2Execution.Result editOver = new EditorialL2Execution().execute(req, BUDGET, cheap, second, new FakeStore());
        assertStop(editOver, EditorialL2Execution.StopClass.BUDGET_EXCEEDED, "L2_TOKEN_OR_COST_BUDGET_EXCEEDED");
        assertEquals(2, editOver.providerCalls());

        assertStop(new EditorialL2Execution().execute(req, BUDGET, null, new FakeProvider(bytes("{}")), new FakeStore()),
                EditorialL2Execution.StopClass.AUTHORIZATION_REQUIRED, "L2_BUDGET_REQUIRED");
        assertEquals(EditorialL2Execution.StopClass.AUTHORIZATION_REQUIRED,
                new EditorialL2Execution().execute(req, null, BUDGET, new FakeProvider(bytes("{}")), new FakeStore()).stopClass());
    }

    @Test public void legacyL2IdentityIsFrozen() {
        assertEquals("1d7385390705b56ec826458ec17b2c78ac81d61f8518b8de847b4c16d5832dec", ctx().request().attemptIdentity());
    }
    private EditorialL2Execution.Result run(EditorialL2Execution.Request req, byte[] editWire) {
        return new EditorialL2Execution().execute(req, BUDGET, BUDGET, new FakeProvider(editWire), new FakeStore());
    }

    // ---- helpers ----

    private static void assertGate(EditorialP5PilotRequest context, byte[] reportBytes, String reason) {
        EditorialL2Execution.Request req = new EditorialL2Execution.Request(context, L1_ID, reportBytes, Set.of());
        FakeProvider provider = new FakeProvider(bytes("{}"));
        FakeStore store = new FakeStore();
        EditorialL2Execution.Result r = new EditorialL2Execution().execute(req, BUDGET, BUDGET, provider, store);
        assertStop(r, EditorialL2Execution.StopClass.INPUT_REQUIRED, reason);
        assertEquals(0, r.providerCalls());
        assertEquals(0, provider.calls);
        assertTrue(store.committed.isEmpty());
    }

    private static void assertStop(EditorialL2Execution.Result r, EditorialL2Execution.StopClass cls, String reason) {
        assertEquals(r.reasonCode() + r.issues(), EditorialL2Execution.Outcome.STOPPED, r.outcome());
        assertEquals(cls, r.stopClass());
        assertEquals(reason, r.reasonCode());
        assertNull(r.committed());
    }

    private static byte[] report(EditorialP5PilotRequest context, String phase, String bindingOverride,
                                 String bundleOverride, String disposition) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("artifactType", "REPORT_L1");
        m.put("schemaVersion", EditorialP5RawWireContract.FINAL_REPORT_SCHEMA);
        m.put("phase", phase);
        m.put("bindingIdentity", bindingOverride != null ? bindingOverride : context.binding().bindingIdentity());
        m.put("canonicalPackHash", context.binding().canonicalPackHash());
        m.put("manifestFingerprint", context.manifestFingerprint());
        m.put("chapterKey", context.chapterKey());
        m.put("bundleIdentity", bundleOverride != null ? bundleOverride : context.bundleIdentity());
        m.put("disposition", disposition);
        return canon(m);
    }

    private static Map<String, Object> change(String id, int line, String before, String after,
                                              boolean dialogue, Map<String, Object> proof, String status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("changeId", id);
        m.put("errorId", "err-" + id);
        m.put("line", BigDecimal.valueOf(line));
        m.put("before", before);
        m.put("after", after);
        m.put("reason", "fix wording");
        m.put("dialogue", dialogue);
        if (proof != null) m.put("speakerProof", proof);
        m.put("status", status);
        return m;
    }

    private static Map<String, Object> wireMap(EditorialL2Execution.Request req, List<Map<String, Object>> changes,
                                               String disposition, String stopClass) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("wireSchemaVersion", EditorialL2Execution.WIRE_SCHEMA_VERSION);
        root.put("attemptIdentity", req.attemptIdentity());
        root.put("resolutions", resolutions("PROCESSED"));
        root.put("changes", new ArrayList<Object>(changes));
        root.put("preserved", new ArrayList<Object>());
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("disposition", disposition);
        d.put("reasonCode", "OK");
        d.put("stopClass", stopClass);
        root.put("disposition", d);
        return root;
    }

    private static byte[] wire(EditorialL2Execution.Request req, List<Map<String, Object>> changes,
                               String disposition, String stopClass) {
        return canon(wireMap(req, changes, disposition, stopClass));
    }

    private static byte[] canon(Map<String, Object> m) {
        return EditorialCanonicalJson.canonicalize(m).getBytes(StandardCharsets.UTF_8);
    }

    private static List<Object> resolutions(String status) {
        List<Object> rows = new ArrayList<>();
        for (String id : List.of("U001", "TG001")) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("candidateId", id);
            row.put("status", status);
            rows.add(row);
        }
        return rows;
    }

    private static Map<String, Object> candidate(String id, String ledger, int line) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("candidateId", id);
        row.put("ledger", ledger);
        row.put("line", BigDecimal.valueOf(line));
        return row;
    }

    private static byte[] discovery(String attempt, List<Map<String, Object>> rows) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("wireSchemaVersion", EditorialL2Execution.DISCOVERY_WIRE);
        root.put("attemptIdentity", attempt);
        root.put("candidates", new ArrayList<Object>(rows));
        return canon(root);
    }

    private static final class FakeProvider implements EditorialL2Execution.Provider {
        final byte[] wire;
        byte[] discoveryWire;
        String finish = "stop";
        boolean costKnown = true;
        BigDecimal cost = new BigDecimal("0.01");
        String failPhase;
        int calls;
        Request lastRequest;
        final List<Request> requests = new ArrayList<>();
        FakeProvider(byte[] wire) { this.wire = wire; }
        @Override public Response call(Request request) throws Exception {
            calls++;
            lastRequest = request;
            requests.add(request);
            if (request.phase().equals(failPhase)) throw new java.io.IOException("transport down");
            byte[] out = wire;
            if (EditorialL2Execution.DISCOVERY_PHASE.equals(request.phase())) {
                out = discoveryWire != null ? discoveryWire : discovery(request.attemptIdentity(),
                        List.of(candidate("U001", "UNIT", 1), candidate("TG001", "TG", 1)));
            }
            return new Response(out, finish, true, 100, 50, cost, costKnown);
        }
    }

    private static final class FakeStore implements EditorialL2Execution.Store {
        final Map<String, EditorialL2Execution.Committed> committed = new HashMap<>();
        final Map<String, String> recovery = new HashMap<>();
        final Map<String, Boolean> inFlight = new HashMap<>();
        boolean failCommit;
        boolean corruptReadback;
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
            EditorialL2Execution.Committed value = committed.get(attempt);
            if (value == null || !corruptReadback) return Optional.ofNullable(value);
            return Optional.of(new EditorialL2Execution.Committed(value.attemptIdentity(),
                    value.predecessorIdentity(), value.bundleIdentity(), value.viL2Bytes(), "0".repeat(64),
                    value.changeMapBytes(), value.changeMapSha256()));
        }
        @Override public void markRecoveryRequired(String attempt, String reason) {
            recovery.put(attempt, reason);
        }
    }

    // ---- fixture (copied minimal helpers from EditorialP5PilotExecutionBoundaryTest) ----

    private static final class Ctx {
        final EditorialP5PilotRequest request;
        Ctx(EditorialP5PilotRequest request) { this.request = request; }
        EditorialL2Execution.Request request() {
            return new EditorialL2Execution.Request(request, L1_ID, report(request, "L1_RECONCILE", null, null,
                    "CONTINUE"), Set.of());
        }
    }

    private static Ctx ctx() {
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
        return new Ctx(new EditorialP5PilotRequest(binding, manifest,
                new EditorialP5PilotRequest.PackAuthority(authority), "chapter-1",
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY, sources, "root-predecessor",
                List.of("anchor-1"), List.of("raw-1"), true, 500));
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
        try (java.io.InputStream input = EditorialL2ExecutionTest.class
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
