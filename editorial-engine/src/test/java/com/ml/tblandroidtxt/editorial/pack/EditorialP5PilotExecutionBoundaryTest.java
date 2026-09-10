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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Test-first P5 boundary; all provider implementations in this class are in-memory fakes. */
public final class EditorialP5PilotExecutionBoundaryTest {
    private static final byte[] PROJECT = bytes("project authority");
    private static final byte[] PROMPT = bytes("prompt authority");
    private static final byte[] WORKFLOW = bytes("workflow authority");

    @Test public void missingAuthorizationIsFailClosedBeforeProviderCall() {
        Fixture fixture = fixture();
        FakeProvider provider = new FakeProvider(response(fixture.request, true));

        EditorialP5PilotResult result = execute(fixture, null, provider, new Store());

        assertEquals(EditorialP5PilotResult.Outcome.STOP, result.outcome());
        assertEquals(EditorialP5PilotResult.StopClass.AUTHORIZATION_REQUIRED,
                result.stopReceipt().stopClass());
        assertEquals(0, provider.calls);
        assertEquals(0, result.metrics().providerCallsBeforePreflight());
    }

    @Test public void unresolvedExternalRecoveryCannotReachProviderOrReclaimAttempt() {
        Fixture fixture = fixture();
        FakeProvider provider = new FakeProvider(response(fixture.request, true));
        Store store = new Store();
        store.recoveryRequired = true;

        EditorialP5PilotResult result = execute(fixture,
                authorization(fixture.request, "auth-recovery-gate"), provider, store);

        assertEquals(EditorialP5PilotResult.Outcome.STOP, result.outcome());
        assertEquals(EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                result.stopReceipt().stopClass());
        assertEquals("STOP_EXTERNAL_CALL_STATE_UNRESOLVED", result.stopReceipt().reasonCode());
        assertEquals(0, provider.calls);
        assertEquals(0, result.metrics().primaryCalls());
    }

    @Test public void wrongBindingAndWrongPhaseNeverReachProvider() {
        Fixture fixture = fixture();
        FakeProvider provider = new FakeProvider(response(fixture.request, true));
        EditorialP5PilotAuthorization wrongBinding = authorizationVariant(fixture.request,
                "wrong-binding", "wrong-binding", 1, 10_000, 2_000, 12_000,
                BigDecimal.ONE, 60_000L, true, 0L, Long.MAX_VALUE);

        EditorialP5PilotResult bindingResult = execute(fixture, wrongBinding, provider, new Store());
        assertEquals(EditorialP5PilotResult.StopClass.BINDING_MISMATCH,
                bindingResult.stopReceipt().stopClass());
        assertEquals(0, provider.calls);

        EditorialP5PilotRequest wrongPhase = fixture.request.withPhase("L2_EDIT");
        Fixture phaseFixture = fixture.withRequest(wrongPhase);
        EditorialP5PilotResult phaseResult = execute(phaseFixture,
                authorization(wrongPhase, "auth-phase"), provider, new Store());
        assertEquals(EditorialP5PilotResult.StopClass.PHASE_NOT_L1,
                phaseResult.stopReceipt().stopClass());
        assertEquals(0, provider.calls);
    }

    @Test public void sourceDriftAndPreflightFailureNeverReachProvider() {
        Fixture fixture = fixture();
        FakeProvider provider = new FakeProvider(response(fixture.request, true));
        EditorialP5PilotRequest drift = fixture.request.withSourceBytes(
                EditorialSafe4Contract.RAW, bytes("changed raw"));
        EditorialP5PilotResult driftResult = execute(fixture.withRequest(drift),
                authorization(drift, "auth-drift"), provider, new Store());
        assertEquals(EditorialP5PilotResult.StopClass.STALE_CHAIN,
                driftResult.stopReceipt().stopClass());
        assertEquals(0, provider.calls);

        EditorialP5PilotRequest missingRaw = fixture.request.withoutSource(EditorialSafe4Contract.RAW);
        EditorialP5PilotResult preflightResult = execute(fixture.withRequest(missingRaw),
                authorization(missingRaw, "auth-preflight"), provider, new Store());
        assertEquals(EditorialP5PilotResult.StopClass.INPUT_REQUIRED,
                preflightResult.stopReceipt().stopClass());
        assertEquals("INPUT_RAW_FILE_MISSING", preflightResult.stopReceipt().reasonCode());
        assertEquals(0, provider.calls);
    }

    @Test public void rawDiscoverySendsOnlyVisibleSourcesAndCommitsValidatedResult() {
        Fixture fixture = fixture();
        FakeProvider provider = new FakeProvider(response(fixture.request, true));
        Store store = new Store();

        EditorialP5PilotResult result = execute(fixture,
                authorization(fixture.request, "auth-success"), provider, store);

        assertEquals(EditorialP5PilotResult.Outcome.COMMITTED, result.outcome());
        assertEquals(1, provider.calls);
        assertEquals(EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC,
                provider.requests.get(0).callKind());
        assertEquals(Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY),
                provider.requests.get(0).visibleSources().keySet());
        assertFalse(provider.requests.get(0).visibleSources().containsKey(EditorialSafe4Contract.DRAFT));
        assertFalse(provider.requests.get(0).visibleSources().containsKey(EditorialSafe4Contract.PRONOUN));
        assertNotNull(provider.requests.get(0).requestEnvelopeHash());
        assertNotNull(result.committedResult());
        assertEquals(result.requestIdentity(), result.committedResult().requestIdentity());
        assertEquals(1, store.committed.size());
        assertEquals(0, result.metrics().networkRetries());
        assertTrue(result.metrics().schemaValidationPassed());
        assertTrue(result.metrics().receiptValidationPassed());
    }

    @Test public void insufficientSemanticEvidencePreservesWithoutProviderCall() {
        Fixture fixture = fixture();
        fixture = fixture.withRequest(fixture.request.withEvidenceContentSufficient(false));
        FakeProvider provider = new FakeProvider(response(fixture.request, true));

        EditorialP5PilotResult result = execute(fixture,
                authorization(fixture.request, "auth-preserve"), provider, new Store());

        assertEquals(EditorialP5PilotResult.Outcome.PRESERVE_DRAFT, result.outcome());
        assertEquals("PRESERVE_DRAFT_EVIDENCE_INSUFFICIENT", result.reasonCode());
        assertEquals(0, provider.calls);
        assertEquals(0, result.metrics().preserveDraftCount());
    }

    @Test public void truncatedOutputIsTypedRetryAndDoesNotCommit() {
        Fixture fixture = fixture();
        FakeProvider provider = new FakeProvider(new EditorialP5PilotProvider.Response(
                "response-truncated", bytes("partial"), "length", false, 20, 10, 30,
                BigDecimal.ZERO, null, true));
        Store store = new Store();

        EditorialP5PilotResult result = execute(fixture,
                authorization(fixture.request, "auth-truncated"), provider, store);

        assertEquals(EditorialP5PilotResult.Outcome.STOP, result.outcome());
        assertEquals(EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                result.stopReceipt().stopClass());
        assertEquals("RETRY_OUTPUT_TRUNCATED", result.stopReceipt().reasonCode());
        assertEquals(0, store.committed.size());
        assertFalse(result.metrics().receiptValidationPassed());
    }

    @Test public void schemaRepairIsAtMostOneCallAndCannotChangeSemanticOutput() {
        Fixture fixture = fixture();
        EditorialP5L1Output output = output(fixture.request);
        FakeProvider provider = new FakeProvider(
                new EditorialP5PilotProvider.Response("response-primary", bytes("bad-schema"), "stop",
                        true, 30, 12, 42, BigDecimal.ZERO, output, false),
                new EditorialP5PilotProvider.Response("response-repair", bytes("repaired"), "stop",
                        true, 12, 14, 26, BigDecimal.ZERO, output, true));

        EditorialP5PilotResult result = execute(fixture,
                authorization(fixture.request, "auth-repair"), provider, new Store());

        assertEquals(EditorialP5PilotResult.Outcome.COMMITTED, result.outcome());
        assertEquals(2, provider.calls);
        assertEquals(EditorialP5PilotProvider.CallKind.SCHEMA_REPAIR,
                provider.requests.get(1).callKind());
        assertTrue(provider.requests.get(1).visibleSources().isEmpty());
        assertNotNull(provider.requests.get(1).priorSemanticFingerprint());
        assertEquals(1, result.metrics().repairCalls());
    }

    @Test public void modelDeclaredPassCannotOverrideLocalDiffValidation() {
        Fixture fixture = fixture();
        EditorialP5L1Output valid = output(fixture.request);
        EditorialP5L1Output invalid = new EditorialP5L1Output(valid.reportSchemaVersion(),
                valid.receiptSchemaVersion(), valid.bindingIdentity(), valid.manifestFingerprint(),
                valid.chapterKey(), valid.phase(), valid.bundleIdentity(), valid.predecessorIdentity(),
                valid.stableAnchors(), valid.ledger(), valid.gates(), valid.preservedInventory(),
                List.of(new EditorialDiffValidator.DeclaredChange(1, "wrong-before", "wrong-after",
                        "model-error")), valid.beforeText(), valid.afterText(), valid.releaseAttemptCount(),
                valid.disposition(), valid.evidenceRefs(), true);
        FakeProvider provider = new FakeProvider(new EditorialP5PilotProvider.Response(
                "response-invalid", bytes("invalid"), "stop", true, 30, 12, 42,
                BigDecimal.ZERO, invalid, true));
        EditorialP5PilotAuthorization auth = authorizationVariant(fixture.request, "auth-model-pass",
                fixture.request.binding().bindingIdentity(), 0, 10_000, 2_000, 12_000,
                BigDecimal.ONE, 60_000L, true, 0L, Long.MAX_VALUE);

        EditorialP5PilotResult result = execute(fixture, auth, provider, new Store());

        assertEquals(EditorialP5PilotResult.Outcome.STOP, result.outcome());
        assertEquals(EditorialP5PilotResult.StopClass.REPAIR_REQUIRED,
                result.stopReceipt().stopClass());
        assertEquals("REPAIR_OUTPUT_SCHEMA_INVALID", result.stopReceipt().reasonCode());
        assertTrue(result.committedResult() == null);
        assertEquals(1, provider.calls);
    }

    @Test public void outputIdentityMismatchCannotBeAcceptedOrRebound() {
        Fixture fixture = fixture();
        EditorialP5L1Output valid = output(fixture.request);
        EditorialP5L1Output mismatched = new EditorialP5L1Output(valid.reportSchemaVersion(),
                valid.receiptSchemaVersion(), "wrong-binding", valid.manifestFingerprint(),
                valid.chapterKey(), valid.phase(), valid.bundleIdentity(),
                valid.predecessorIdentity(), valid.stableAnchors(), valid.ledger(), valid.gates(),
                valid.preservedInventory(), valid.declaredChanges(), valid.beforeText(),
                valid.afterText(), valid.releaseAttemptCount(), valid.disposition(),
                valid.evidenceRefs(), true);
        FakeProvider provider = new FakeProvider(new EditorialP5PilotProvider.Response(
                "response-identity-mismatch", bytes("identity-mismatch"), "stop", true,
                30, 12, 42, BigDecimal.ZERO, mismatched, true));

        EditorialP5PilotResult result = execute(fixture,
                authorization(fixture.request, "auth-identity-mismatch"), provider, new Store());

        assertEquals(EditorialP5PilotResult.Outcome.STOP, result.outcome());
        assertEquals(EditorialP5PilotResult.StopClass.REPAIR_REQUIRED,
                result.stopReceipt().stopClass());
        assertEquals("REPAIR_OUTPUT_SCHEMA_INVALID", result.stopReceipt().reasonCode());
        assertEquals(1, provider.calls);
        assertNull(result.committedResult());
    }

    @Test public void estimatedCostIsNotReportedAsActualCost() {
        Fixture fixture = fixture();
        EditorialP5PilotProvider.Response response = new EditorialP5PilotProvider.Response(
                "response-estimated-cost", bytes("response"), "stop", true,
                80, 30, 110, BigDecimal.valueOf(0.25), output(fixture.request), true, false);
        FakeProvider provider = new FakeProvider(response);

        EditorialP5PilotResult result = execute(fixture,
                authorizationVariant(fixture.request, "auth-estimated-cost",
                        fixture.request.binding().bindingIdentity(), 0, 2_000, 2_000, 12_000,
                        BigDecimal.valueOf(0.10), 60_000L, true, 0L, Long.MAX_VALUE),
                provider, new Store());

        assertEquals(EditorialP5PilotResult.StopClass.BUDGET_EXCEEDED,
                result.stopReceipt().stopClass());
        assertEquals(0, result.metrics().actualReportedCost().signum());
        assertEquals(0, result.metrics().estimatedCost().compareTo(BigDecimal.valueOf(0.25)));
    }

    @Test public void reportedAndEstimatedCostsRemainSeparateAcrossRepair() {
        Fixture fixture = fixture();
        EditorialP5L1Output output = output(fixture.request);
        FakeProvider provider = new FakeProvider(
                new EditorialP5PilotProvider.Response("response-reported", bytes("primary"), "stop",
                        true, 80, 30, 110, BigDecimal.valueOf(0.20), output, false, true),
                new EditorialP5PilotProvider.Response("response-estimated", bytes("repair"), "stop",
                        true, 12, 14, 26, BigDecimal.valueOf(0.30), output, true, false));

        EditorialP5PilotResult result = execute(fixture,
                authorizationVariant(fixture.request, "auth-cost-accounting",
                        fixture.request.binding().bindingIdentity(), 1, 2_000, 2_000, 12_000,
                        BigDecimal.valueOf(0.60), 60_000L, true, 0L, Long.MAX_VALUE),
                provider, new Store());

        assertEquals(EditorialP5PilotResult.Outcome.COMMITTED, result.outcome());
        assertEquals(0, result.metrics().actualReportedCost().compareTo(BigDecimal.valueOf(0.20)));
        assertEquals(0, result.metrics().estimatedCost().compareTo(BigDecimal.valueOf(0.30)));
        assertEquals(2, provider.calls);
    }

    @Test public void unknownProviderCostStopsWithoutTreatingItAsZero() {
        Fixture fixture = fixture();
        EditorialP5PilotProvider.Response response = new EditorialP5PilotProvider.Response(
                "response-unknown-cost", bytes("response"), "stop", true,
                80, 30, 0, 110, BigDecimal.ZERO, output(fixture.request), true, false, false);
        FakeProvider provider = new FakeProvider(response);
        Store store = new Store();

        EditorialP5PilotResult result = execute(fixture,
                authorizationVariant(fixture.request, "auth-unknown-cost",
                        fixture.request.binding().bindingIdentity(), 0, 2_000, 2_000, 12_000,
                        BigDecimal.ONE, 60_000L, true, 0L, Long.MAX_VALUE),
                provider, store);

        assertEquals(EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                result.stopReceipt().stopClass());
        assertEquals("RETRY_PROVIDER_COST_UNAVAILABLE", result.stopReceipt().reasonCode());
        assertFalse(result.metrics().costAccountingComplete());
        assertEquals(0, result.metrics().actualReportedCost().signum());
        assertEquals(0, result.metrics().estimatedCost().signum());
        assertEquals(0, store.committed.size());
    }

    @Test public void reasoningTokensCountAgainstOutputBudget() {
        Fixture fixture = fixture();
        EditorialP5PilotProvider.Response response = new EditorialP5PilotProvider.Response(
                "response-reasoning-budget", bytes("response"), "stop", true,
                80, 300, 250, 380, BigDecimal.ZERO, output(fixture.request), true, true, true);
        FakeProvider provider = new FakeProvider(response);
        Store store = new Store();

        EditorialP5PilotResult result = execute(fixture,
                authorizationVariant(fixture.request, "auth-reasoning-budget",
                        fixture.request.binding().bindingIdentity(), 0, 2_000, 500, 2_000,
                        BigDecimal.ONE, 60_000L, true, 0L, Long.MAX_VALUE),
                provider, store);

        assertEquals(EditorialP5PilotResult.Outcome.COMMITTED, result.outcome());
        assertEquals(250, result.metrics().reasoningTokens());
        assertEquals(300, result.metrics().outputTokens());
        assertEquals(380, result.metrics().totalTokens());
        assertEquals(1, store.committed.size());

        Store rejectedStore = new Store();
        EditorialP5PilotResult rejected = execute(fixture,
                authorizationVariant(fixture.request, "auth-reasoning-budget-rejected",
                        fixture.request.binding().bindingIdentity(), 0, 2_000, 500, 379,
                        BigDecimal.ONE, 60_000L, true, 0L, Long.MAX_VALUE),
                new FakeProvider(response), rejectedStore);
        assertEquals(EditorialP5PilotResult.StopClass.BUDGET_EXCEEDED,
                rejected.stopReceipt().stopClass());
        assertEquals("P5_TOKEN_OR_COST_BUDGET_EXCEEDED", rejected.stopReceipt().reasonCode());
        assertEquals(0, rejectedStore.committed.size());
    }

    @Test public void deadlineExpiryBeforeCommitNeverPersistsLateResult() {
        Fixture fixture = fixture();
        FakeProvider provider = new FakeProvider(response(fixture.request, true));
        Store store = new Store();
        java.util.concurrent.atomic.AtomicInteger clockReads =
                new java.util.concurrent.atomic.AtomicInteger();
        EditorialP5PilotResult result = new EditorialP5PilotExecution(() ->
                clockReads.incrementAndGet() >= 6 ? 2_001L : 1_000L)
                .execute(fixture.request,
                        authorizationVariant(fixture.request, "auth-deadline-before-commit",
                                fixture.request.binding().bindingIdentity(), 0, 2_000, 2_000,
                                12_000, BigDecimal.ONE, 1_000L, true, 0L, Long.MAX_VALUE),
                        provider, store);

        assertEquals(EditorialP5PilotResult.Outcome.STOP, result.outcome());
        assertEquals(EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                result.stopReceipt().stopClass());
        assertEquals("RETRY_ATTEMPT_DEADLINE_EXPIRED_BEFORE_COMMIT",
                result.stopReceipt().reasonCode());
        assertEquals(1, provider.calls);
        assertEquals(0, store.committed.size());
    }

    @Test public void inFlightAttemptBlocksSecondExternalCall() {
        Fixture fixture = fixture();
        Store store = new Store();
        store.inFlight.add(fixture.request.attemptIdentity());
        FakeProvider provider = new FakeProvider(response(fixture.request, true));

        EditorialP5PilotResult result = execute(fixture,
                authorization(fixture.request, "auth-in-flight"), provider, store);

        assertEquals(EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                result.stopReceipt().stopClass());
        assertEquals("RETRY_PROVIDER_CALL_STATE_UNKNOWN", result.stopReceipt().reasonCode());
        assertEquals(0, provider.calls);
    }

    @Test public void reconcileSendsOnlyPhaseAllowedSources() {
        Fixture fixture = fixture();
        EditorialP5PilotRequest request = fixture.request.withPhase("L1_RECONCILE");
        fixture = fixture.withRequest(request);
        FakeProvider provider = new FakeProvider(response(request, true));

        EditorialP5PilotResult result = execute(fixture,
                authorization(request, "auth-reconcile"), provider, new Store());

        assertEquals(EditorialP5PilotResult.Outcome.COMMITTED, result.outcome());
        assertEquals(Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                        EditorialSafe4Contract.GLOSSARY, EditorialSafe4Contract.PRONOUN),
                provider.requests.get(0).visibleSources().keySet());
        assertFalse(provider.requests.get(0).visibleSources().containsKey(EditorialSafe4Contract.PAIR_CONTEXT));
    }

    @Test public void providerConsentAndExpiredAuthorizationAreFailClosed() {
        Fixture fixture = fixture();
        FakeProvider provider = new FakeProvider(response(fixture.request, true));

        EditorialP5PilotResult noConsent = execute(fixture,
                authorizationVariant(fixture.request, "auth-no-consent",
                        fixture.request.binding().bindingIdentity(), 1, 10_000, 2_000, 12_000,
                        BigDecimal.ONE, 60_000L, false, 0L, Long.MAX_VALUE),
                provider, new Store());
        assertEquals(EditorialP5PilotResult.StopClass.AUTHORIZATION_REQUIRED,
                noConsent.stopReceipt().stopClass());
        assertEquals(0, provider.calls);

        EditorialP5PilotResult expired = execute(fixture,
                authorizationVariant(fixture.request, "auth-expired",
                        fixture.request.binding().bindingIdentity(), 1, 10_000, 2_000, 12_000,
                        BigDecimal.ONE, 60_000L, true, 0L, 500L),
                provider, new Store());
        assertEquals(EditorialP5PilotResult.StopClass.AUTHORIZATION_EXPIRED,
                expired.stopReceipt().stopClass());
        assertEquals(0, provider.calls);
    }

    @Test public void unreadablePinnedHandleReturnsRetryBeforeProvider() {
        Fixture fixture = fixture();
        EditorialP5PilotRequest unreadable = fixture.request.withSourceBytes(
                EditorialSafe4Contract.RAW, null);
        FakeProvider provider = new FakeProvider(response(unreadable, true));

        EditorialP5PilotResult result = execute(fixture.withRequest(unreadable),
                authorization(unreadable, "auth-unreadable"), provider, new Store());

        assertEquals(EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                result.stopReceipt().stopClass());
        assertEquals("RETRY_SOURCE_BYTES_UNAVAILABLE", result.stopReceipt().reasonCode());
        assertEquals(0, provider.calls);
    }

    @Test public void responseUsageBeyondBudgetDoesNotCommit() {
        Fixture fixture = fixture();
        EditorialP5PilotProvider.Response response = new EditorialP5PilotProvider.Response(
                "response-over-budget", bytes("response"), "stop", true,
                80, 30, 110, BigDecimal.ZERO, output(fixture.request), true);
        FakeProvider provider = new FakeProvider(response);
        Store store = new Store();

        EditorialP5PilotResult result = execute(fixture,
                authorizationVariant(fixture.request, "auth-budget",
                        fixture.request.binding().bindingIdentity(), 1, 10_000, 2_000, 100,
                        BigDecimal.ONE, 60_000L, true, 0L, Long.MAX_VALUE), provider, store);

        assertEquals(EditorialP5PilotResult.StopClass.BUDGET_EXCEEDED,
                result.stopReceipt().stopClass());
        assertEquals("P5_TOKEN_OR_COST_BUDGET_EXCEEDED", result.stopReceipt().reasonCode());
        assertEquals(0, store.committed.size());
        assertEquals(1, provider.calls);
    }

    @Test public void invalidOutputTokenCapStopsBeforeProvider() {
        Fixture fixture = fixture();
        FakeProvider provider = new FakeProvider(response(fixture.request, true));

        EditorialP5PilotResult result = execute(fixture,
                authorizationVariant(fixture.request, "auth-invalid-output-cap",
                        fixture.request.binding().bindingIdentity(), 0, 10_000, 0, 12_000,
                        BigDecimal.ONE, 60_000L, true, 0L, Long.MAX_VALUE),
                provider, new Store());

        assertEquals(EditorialP5PilotResult.StopClass.BUDGET_EXCEEDED,
                result.stopReceipt().stopClass());
        assertEquals("P5_TOKEN_BUDGET_INVALID", result.stopReceipt().reasonCode());
        assertEquals(0, provider.calls);
    }

    @Test public void exactRetryAfterCommitIsIdempotentWithoutSecondProviderCall() {
        Fixture fixture = fixture();
        Store store = new Store();
        FakeProvider first = new FakeProvider(response(fixture.request, true));
        EditorialP5PilotAuthorization auth = authorization(fixture.request, "auth-idempotent");

        EditorialP5PilotResult committed = execute(fixture, auth, first, store);
        FakeProvider second = new FakeProvider();
        EditorialP5PilotResult retry = execute(fixture, auth, second, store);

        assertEquals(EditorialP5PilotResult.Outcome.COMMITTED, committed.outcome());
        assertEquals(EditorialP5PilotResult.Outcome.ALREADY_COMMITTED, retry.outcome());
        assertEquals(committed.requestIdentity(), retry.requestIdentity());
        assertEquals(committed.committedResult().attemptIdentity(),
                retry.committedResult().attemptIdentity());
        assertEquals(0, second.calls);
        assertNotNull(retry.committedResult().reportBytes());
        assertTrue(retry.committedResult().output().modelDeclaredPass());
    }

    @Test public void atomicCommitFailureReturnsRetryAndNoCommittedResult() {
        Fixture fixture = fixture();
        FakeProvider provider = new FakeProvider(response(fixture.request, true));
        Store store = new Store();
        store.failCommit = true;

        EditorialP5PilotResult result = execute(fixture,
                authorization(fixture.request, "auth-commit-failure"), provider, store);

        assertEquals(EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                result.stopReceipt().stopClass());
        assertEquals("RETRY_ATOMIC_RESULT_COMMIT_FAILED", result.stopReceipt().reasonCode());
        assertNull(result.committedResult());
        assertEquals(0, store.committed.size());
    }

    private EditorialP5PilotResult execute(Fixture fixture,
                                            EditorialP5PilotAuthorization authorization,
                                            FakeProvider provider, Store store) {
        return new EditorialP5PilotExecution(() -> 1_000L)
                .execute(fixture.request, authorization, provider, store);
    }

    private static EditorialP5PilotAuthorization authorization(EditorialP5PilotRequest request,
                                                                String id) {
        return authorizationVariant(request, id, request.binding().bindingIdentity(), 1,
                10_000, 2_000, 12_000, BigDecimal.ONE, 60_000L, true,
                0L, Long.MAX_VALUE);
    }

    private static EditorialP5PilotAuthorization authorizationVariant(
            EditorialP5PilotRequest request, String id, String bindingIdentity,
            int maximumSchemaRepairCalls, int maximumInputTokens, int maximumOutputTokens,
            int maximumTotalTokens, BigDecimal maximumTotalCost,
            long maximumExecutionTimeMillis, boolean allowChapterToProvider,
            long issuedAtMillis, long expiresAtMillis) {
        return new EditorialP5PilotAuthorization(id, bindingIdentity,
                request.binding().runDeclarationIdentity(), request.binding().canonicalPackHash(),
                request.binding().canonicalProfileHash(), request.binding().compatibilityEvaluationId(),
                request.chapterKey(), "L1", "FAKE_PROVIDER", "fake/model", "fake-endpoint-account",
                1, maximumSchemaRepairCalls, 0, maximumInputTokens, maximumOutputTokens,
                maximumTotalTokens, maximumTotalCost, maximumExecutionTimeMillis,
                allowChapterToProvider, false, false, "HASH_ONLY", "TEST_STOP_AUTHORITY",
                issuedAtMillis, expiresAtMillis, true);
    }

    private static EditorialP5PilotProvider.Response response(EditorialP5PilotRequest request,
                                                               boolean schemaValid) {
        return new EditorialP5PilotProvider.Response("response-success", bytes("response"), "stop",
                true, 80, 30, 110, BigDecimal.ZERO,
                schemaValid ? output(request) : null, schemaValid);
    }

    private static EditorialP5L1Output output(EditorialP5PilotRequest request) {
        EditorialLedgerValidator.Request ledger = new EditorialLedgerValidator.Request(
                request.populationIds(), List.of(new EditorialLedgerValidator.Entry(
                        request.populationIds().get(0), "PROCESSED", List.of("evidence-1"), false)));
        Map<String, String> gates = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) gates.put(gate, "PASS");
        EditorialP5PilotRequest.SourceBytes rawSource =
                request.source(EditorialSafe4Contract.RAW);
        String rawText = request.phase().equals("L1_RAW_DISCOVERY")
                ? rawSource == null || rawSource.bytes() == null ? ""
                : new String(rawSource.bytes(), StandardCharsets.UTF_8)
                : "draft";
        return new EditorialP5L1Output("safe4.full.report-l1.v1",
                EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION, request.binding().bindingIdentity(),
                request.manifestFingerprint(), request.chapterKey(), "L1", request.bundleIdentity(),
                request.predecessorIdentity(), request.stableAnchors(), ledger, gates,
                List.of(), List.of(), rawText, rawText, 0,
                EditorialStopDecision.continueWithoutStop("L1", "COVERAGE", "LOCAL_VALIDATED"),
                Set.of("evidence-1"), true);
    }

    private static Fixture fixture() {
        Map<EditorialPackFileRole, byte[]> authority = new LinkedHashMap<>();
        authority.put(EditorialPackFileRole.PROJECT_INSTRUCTION, PROJECT);
        authority.put(EditorialPackFileRole.TURN_PROMPT, PROMPT);
        authority.put(EditorialPackFileRole.WORKFLOW, WORKFLOW);
        EditorialPackManifest manifest = manifest(authority);
        List<EditorialP5PilotRequest.SourceBytes> sources = List.of(
                source(EditorialSafe4Contract.RAW, "raw", "raw chapter"),
                source(EditorialSafe4Contract.DRAFT, "draft", "draft"),
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
        try (java.io.InputStream input = EditorialP5PilotExecutionBoundaryTest.class
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

    private record Fixture(EditorialP5PilotRequest request) {
        Fixture withRequest(EditorialP5PilotRequest value) { return new Fixture(value); }
    }

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
            if (recoveryRequired) return Claim.RECOVERY_REQUIRED;
            if (committed.containsKey(attemptIdentity)) return Claim.ALREADY_COMMITTED;
            if (!inFlight.add(attemptIdentity)) return Claim.IN_FLIGHT;
            return Claim.ACQUIRED;
        }

        @Override public void commit(EditorialP5PilotResult.CommittedResult value) {
            if (failCommit) throw new IllegalStateException("test commit failure");
            committed.put(value.attemptIdentity(), value);
            inFlight.remove(value.attemptIdentity());
        }

        @Override public Optional<EditorialP5PilotResult.CommittedResult> findCommitted(String attemptIdentity) {
            return Optional.ofNullable(committed.get(attemptIdentity));
        }

        @Override public void markRecoveryRequired(String attemptIdentity, String reasonCode) {
            inFlight.add(attemptIdentity);
        }

        private boolean failCommit;
        private boolean recoveryRequired;
    }
}
