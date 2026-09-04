package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Deterministic, bounded L1 pilot boundary.  The class accepts an injected
 * provider and persistence owner so unit tests can use fakes; it has no global
 * activation path and it never treats model text as authoritative state.
 */
public final class EditorialP5PilotExecution {
    private static final String L1_OUTPUT_SCHEMA = "safe4.full.report-l1.v1";

    @FunctionalInterface
    public interface Clock {
        long nowMillis();
    }

    /** Atomic attempt owner.  A real app implementation must back these calls by one transaction. */
    public interface AttemptStore {
        enum Claim { ACQUIRED, ALREADY_COMMITTED, IN_FLIGHT }

        /**
         * Gives a durable store the exact request facts before claim. The
         * default keeps existing unit-test stores source-compatible.
         */
        default void prepare(EditorialP5PilotRequest request,
                             EditorialP5PilotAuthorization authorization,
                             String requestEnvelopeHash) { }

        Claim claim(String attemptIdentity);

        void commit(EditorialP5PilotResult.CommittedResult value);

        Optional<EditorialP5PilotResult.CommittedResult> findCommitted(String attemptIdentity);

        void markRecoveryRequired(String attemptIdentity, String reasonCode);
    }

    private final Clock clock;

    public EditorialP5PilotExecution(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public EditorialP5PilotResult execute(EditorialP5PilotRequest request,
                                          EditorialP5PilotAuthorization authorization,
                                          EditorialP5PilotProvider provider,
                                          AttemptStore store) {
        long startedAt = clock.nowMillis();
        MetricState metrics = new MetricState(startedAt, clock);
        if (request == null) {
            return stopped("", EditorialP5PilotResult.StopClass.INPUT_REQUIRED,
                    "INPUT_PILOT_REQUEST_MISSING", "L1", "EXACT_BINDING",
                    List.of(), "P5_REQUEST", "Create an exact persisted pilot request",
                    "L1_SOURCE_PREFLIGHT", true, metrics);
        }
        String requestIdentity = safeRequestIdentity(request);
        if (authorization == null) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.AUTHORIZATION_REQUIRED,
                    "P5_AUTHORIZATION_REQUIRED", request.phase(), "PILOT_AUTHORIZATION",
                    List.of(), request.chapterKey(), "Obtain explicit scoped pilot authorization",
                    request.phase(), true, metrics);
        }
        if (authorization.expiredAt(clock.nowMillis())) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.AUTHORIZATION_EXPIRED,
                    "P5_AUTHORIZATION_EXPIRED", request.phase(), "PILOT_AUTHORIZATION",
                    List.of(), request.chapterKey(), "Issue a new bounded authorization",
                    request.phase(), false, metrics);
        }
        if (!"L1".equals(authorization.phase())
                && !authorization.phase().equals(request.phase())) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.PHASE_NOT_L1,
                    "P5_AUTHORIZATION_PHASE_NOT_L1", request.phase(), "PILOT_PHASE",
                    List.of(), request.chapterKey(), "Create an L1-only authorization",
                    "L1_SOURCE_PREFLIGHT", false, metrics);
        }
        if (!isL1ExecutionPhase(request.phase())) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.PHASE_NOT_L1,
                    "P5_PHASE_NOT_L1", request.phase(), "PILOT_PHASE",
                    List.of(), request.chapterKey(), "Select L1_RAW_DISCOVERY or L1_RECONCILE",
                    "L1_SOURCE_PREFLIGHT", false, metrics);
        }
        if (!bindingMatches(request, authorization)) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.BINDING_MISMATCH,
                    "P5_AUTHORIZATION_BINDING_MISMATCH", request.phase(), "EXACT_BINDING",
                    List.of(), request.chapterKey(), "Resolve the exact persisted P4 binding",
                    request.phase(), false, metrics);
        }
        if (!authorization.allowChapterToProvider()) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.AUTHORIZATION_REQUIRED,
                    "P5_PROVIDER_CONSENT_REQUIRED", request.phase(), "PILOT_AUTHORIZATION",
                    List.of(), request.chapterKey(), "Obtain explicit consent for chapter transmission",
                    request.phase(), false, metrics);
        }
        if (authorization.maximumPrimarySemanticCalls() < 1) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.BUDGET_EXCEEDED,
                    "P5_CALL_BUDGET_INVALID", request.phase(), "CALL_POLICY",
                    List.of(), request.chapterKey(), "Use one primary call and zero automatic retries",
                    request.phase(), false, metrics);
        }

        String envelopeIssue = validatePackEnvelope(request);
        if (envelopeIssue != null) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.BINDING_MISMATCH,
                    envelopeIssue, request.phase(), "ARTIFACT_IDENTITY", List.of(), request.chapterKey(),
                    "Restore the exact persisted pack/profile facts", request.phase(), false, metrics);
        }
        String sourceIssue = validatePinnedSources(request);
        if (sourceIssue != null) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.STALE_CHAIN,
                    sourceIssue, request.phase(), "SOURCE_IDENTITY", List.of(), request.chapterKey(),
                    "Restore the exact pinned source bytes or create a new binding",
                    "L1_SOURCE_PREFLIGHT", false, metrics);
        }

        EditorialSourcePreflight.Result preflight = runPreflight(request);
        if (preflight.outcome() == EditorialSourcePreflight.Outcome.PRESERVE_DRAFT) {
            // No provider call and no partial report: evidence insufficiency is a
            // non-stop disposition at this boundary, not a content failure.
            return EditorialP5PilotResult.preserveWithoutProvider(requestIdentity,
                    preflight.reasonCode(), metrics.snapshot());
        }
        if (preflight.outcome() != EditorialSourcePreflight.Outcome.PASS) {
            EditorialP5PilotResult.StopClass stopClass = preflight.outcome()
                    == EditorialSourcePreflight.Outcome.RETRY_REQUIRED
                    ? EditorialP5PilotResult.StopClass.RETRY_REQUIRED
                    : EditorialP5PilotResult.StopClass.INPUT_REQUIRED;
            return stopped(requestIdentity, stopClass, preflight.reasonCode(), request.phase(),
                    "SOURCE_PREFLIGHT", List.of(), request.chapterKey(),
                    recoveryFor(preflight.reasonCode()), "L1_SOURCE_PREFLIGHT", true, metrics);
        }

        EditorialPhaseContextProjector.PhaseProjection projection;
        try {
            EditorialPhaseContextProjector projector = new EditorialPhaseContextProjector();
            EditorialPhaseContextProjector.Bundle bundle = request.bundleForExecution();
            EditorialSourceStatusResolver.Result pronounStatus = bundle.pronounStatus();
            if (!pronounStatus.accepted()) {
                return stopped(requestIdentity, EditorialP5PilotResult.StopClass.INPUT_REQUIRED,
                        pronounStatus.reasonCode(), request.phase(), "SOURCE_STATUS",
                        List.of(), request.chapterKey(), "Resolve explicit Pronoun source status",
                        "L1_SOURCE_PREFLIGHT", true, metrics);
            }
            EditorialPhaseContextProjector.FullBundleValidation full = projector.validateFullBundle(bundle);
            if (!full.valid()) {
                return stopped(requestIdentity, EditorialP5PilotResult.StopClass.INPUT_REQUIRED,
                        "INPUT_BUNDLE_INVALID", request.phase(), "BUNDLE_IDENTITY", full.issues(),
                        request.chapterKey(), "Restore a complete, pinned source bundle",
                        "L1_SOURCE_PREFLIGHT", false, metrics);
            }
            projection = projector.project(bundle, request.phase());
        } catch (RuntimeException error) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.INPUT_REQUIRED,
                    "INPUT_PHASE_PROJECTION_INVALID", request.phase(), "PHASE_VISIBILITY",
                    List.of(), request.chapterKey(), "Restore the exact phase-eligible bundle",
                    "L1_SOURCE_PREFLIGHT", false, metrics);
        }

        int contextSize = contextSize(request, projection);
        metrics.requestContextSize = contextSize;
        if (contextSize > authorization.maximumInputTokens()
                || request.requestedOutputTokens() > authorization.maximumOutputTokens()) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.BUDGET_EXCEEDED,
                    "P5_TOKEN_BUDGET_EXCEEDED", request.phase(), "TOKEN_BUDGET", List.of(),
                    request.chapterKey(), "Reduce the bounded pilot request or obtain a new budget",
                    request.phase(), false, metrics);
        }

        if (provider == null || store == null) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.EXECUTION_DISABLED,
                    "P5_PROVIDER_OR_STORE_NOT_CONFIGURED", request.phase(), "EXECUTION_GATE",
                    List.of(), request.chapterKey(), "Configure a scoped fake/provider and atomic store",
                    request.phase(), false, metrics);
        }

        String requestEnvelopeHash = requestEnvelopeHash(request, projection, authorization);
        AttemptStore.Claim claim;
        try {
            store.prepare(request, authorization, requestEnvelopeHash);
            claim = store.claim(request.attemptIdentity());
        } catch (RuntimeException error) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                    "RETRY_ATTEMPT_CLAIM_FAILED", request.phase(), "ATOMIC_ATTEMPT",
                    List.of(), request.chapterKey(), "Retry the exact attempt after persistence recovery",
                    request.phase(), true, metrics);
        }
        if (claim == AttemptStore.Claim.ALREADY_COMMITTED) {
            Optional<EditorialP5PilotResult.CommittedResult> existing = store.findCommitted(
                    request.attemptIdentity());
            if (existing.isPresent()) return EditorialP5PilotResult.alreadyCommitted(
                    requestIdentity, existing.get());
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                    "RETRY_COMMITTED_RESULT_UNAVAILABLE", request.phase(), "ATOMIC_ATTEMPT",
                    List.of(), request.chapterKey(), "Recover the committed result before retrying",
                    request.phase(), true, metrics);
        }
        if (claim == AttemptStore.Claim.IN_FLIGHT) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                    "RETRY_PROVIDER_CALL_STATE_UNKNOWN", request.phase(), "ATOMIC_ATTEMPT",
                    List.of(), request.chapterKey(), "Resolve the existing external call state explicitly",
                    request.phase(), true, metrics);
        }
        if (claim != AttemptStore.Claim.ACQUIRED) {
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                    "RETRY_ATTEMPT_CLAIM_UNKNOWN", request.phase(), "ATOMIC_ATTEMPT",
                    List.of(), request.chapterKey(), "Recover the attempt store before retrying",
                    request.phase(), true, metrics);
        }

        EditorialP5PilotProvider.Request providerRequest = new EditorialP5PilotProvider.Request(
                request.attemptIdentity(), EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC,
                authorization.provider(), authorization.model(), request.phase(), requestEnvelopeHash,
                visibleSourceBytes(projection), request.authority(), L1_OUTPUT_SCHEMA,
                request.chapterKey(), "");

        EditorialP5PilotProvider.Response primary;
        try {
            metrics.primaryCalls++;
            primary = provider.call(providerRequest);
            metrics.record(primary);
        } catch (Exception error) {
            recover(store, request.attemptIdentity(), "RETRY_PROVIDER_CALL_FAILED");
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                    "RETRY_PROVIDER_CALL_FAILED", request.phase(), "PROVIDER_CALL",
                    List.of(), request.chapterKey(), "Inspect external call state before an explicit retry",
                    request.phase(), true, metrics);
        }

        if (primary == null) {
            recover(store, request.attemptIdentity(), "RETRY_PROVIDER_EMPTY_RESPONSE");
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                    "RETRY_PROVIDER_EMPTY_RESPONSE", request.phase(), "PROVIDER_CALL",
                    List.of(), request.chapterKey(), "Inspect external call state before retrying",
                    request.phase(), true, metrics);
        }
        if (isTruncated(primary)) {
            recover(store, request.attemptIdentity(), "RETRY_OUTPUT_TRUNCATED");
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                    "RETRY_OUTPUT_TRUNCATED", request.phase(), "OUTPUT_COMPLETENESS",
                    List.of(), request.chapterKey(), "Retry only after explicit pilot decision",
                    request.phase(), true, metrics);
        }
        if (overBudget(authorization, metrics)) {
            recover(store, request.attemptIdentity(), "P5_TOKEN_OR_COST_BUDGET_EXCEEDED");
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.BUDGET_EXCEEDED,
                    "P5_TOKEN_OR_COST_BUDGET_EXCEEDED", request.phase(), "TOKEN_BUDGET",
                    List.of(), request.chapterKey(), "Stop the pilot and obtain a new bounded budget",
                    request.phase(), false, metrics);
        }

        EditorialP5L1Output output = primary.output();
        if (!primary.schemaValid() || output == null) {
            if (output == null || authorization.maximumSchemaRepairCalls() < 1) {
                recover(store, request.attemptIdentity(), "REPAIR_OUTPUT_SCHEMA_INVALID");
                return stopped(requestIdentity, EditorialP5PilotResult.StopClass.REPAIR_REQUIRED,
                        "REPAIR_OUTPUT_SCHEMA_INVALID", request.phase(), "REPORT_L1_SCHEMA",
                        List.of(), request.chapterKey(), "Perform at most one schema-only repair",
                        request.phase(), true, metrics);
            }
            String semanticFingerprint = output.semanticFingerprint();
            EditorialP5PilotProvider.Request repairRequest = new EditorialP5PilotProvider.Request(
                    request.attemptIdentity(), EditorialP5PilotProvider.CallKind.SCHEMA_REPAIR,
                    authorization.provider(), authorization.model(), request.phase(),
                    requestEnvelopeHash + ":SCHEMA_REPAIR", Map.of(), request.authority(),
                    L1_OUTPUT_SCHEMA, request.chapterKey(), semanticFingerprint);
            EditorialP5PilotProvider.Response repair;
            try {
                metrics.repairCalls++;
                repair = provider.call(repairRequest);
                metrics.record(repair);
            } catch (Exception error) {
                recover(store, request.attemptIdentity(), "RETRY_SCHEMA_REPAIR_FAILED");
                return stopped(requestIdentity, EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                        "RETRY_SCHEMA_REPAIR_FAILED", request.phase(), "REPORT_L1_SCHEMA",
                        List.of(), request.chapterKey(), "Inspect repair call state before retrying",
                        request.phase(), true, metrics);
            }
            if (repair == null || isTruncated(repair)) {
                recover(store, request.attemptIdentity(), "RETRY_OUTPUT_TRUNCATED");
                return stopped(requestIdentity, EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                        "RETRY_OUTPUT_TRUNCATED", request.phase(), "OUTPUT_COMPLETENESS",
                        List.of(), request.chapterKey(), "Retry only after explicit pilot decision",
                        request.phase(), true, metrics);
            }
            if (overBudget(authorization, metrics)) {
                recover(store, request.attemptIdentity(), "P5_TOKEN_OR_COST_BUDGET_EXCEEDED");
                return stopped(requestIdentity, EditorialP5PilotResult.StopClass.BUDGET_EXCEEDED,
                        "P5_TOKEN_OR_COST_BUDGET_EXCEEDED", request.phase(), "TOKEN_BUDGET",
                        List.of(), request.chapterKey(), "Stop the pilot and obtain a new bounded budget",
                        request.phase(), false, metrics);
            }
            if (!repair.schemaValid() || repair.output() == null) {
                recover(store, request.attemptIdentity(), "REPAIR_OUTPUT_SCHEMA_INVALID");
                return stopped(requestIdentity, EditorialP5PilotResult.StopClass.REPAIR_REQUIRED,
                        "REPAIR_OUTPUT_SCHEMA_INVALID", request.phase(), "REPORT_L1_SCHEMA",
                        List.of(), request.chapterKey(), "Do not issue a third call; repair locally or stop",
                        request.phase(), true, metrics);
            }
            if (!semanticFingerprint.equals(repair.output().semanticFingerprint())) {
                recover(store, request.attemptIdentity(), "REPAIR_SEMANTICS_CHANGED");
                return stopped(requestIdentity, EditorialP5PilotResult.StopClass.REPAIR_REQUIRED,
                        "REPAIR_SEMANTICS_CHANGED", request.phase(), "REPAIR_BOUNDARY",
                        List.of(), request.chapterKey(), "Reject semantic changes in a schema-only repair",
                        request.phase(), false, metrics);
            }
            output = repair.output();
        }
        metrics.schemaValidationPassed = true;

        Validation validation = validateOutput(request, projection, output);
        if (!validation.valid) {
            recover(store, request.attemptIdentity(), "REPAIR_OUTPUT_SCHEMA_INVALID");
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.REPAIR_REQUIRED,
                    "REPAIR_OUTPUT_SCHEMA_INVALID", request.phase(), "LOCAL_OUTPUT_VALIDATION",
                    validation.issues, request.chapterKey(), "Repair schema only once or stop",
                    request.phase(), true, metrics);
        }
        metrics.receiptValidationPassed = true;
        if (output.disposition().isStop()) {
            EditorialP5PilotResult.StopClass stopClass = output.disposition().stopReceipt().stopClass()
                    == EditorialStopDecision.StopClass.CONTENT_BLOCKED
                    ? EditorialP5PilotResult.StopClass.CONTENT_BLOCKED
                    : EditorialP5PilotResult.StopClass.VALIDATION_FAILED;
            recover(store, request.attemptIdentity(), output.disposition().reasonCode());
            return stopped(requestIdentity, stopClass, output.disposition().reasonCode(), request.phase(),
                    output.disposition().blockingGate(), output.disposition().evidenceRefs(),
                    output.disposition().affectedScope(), output.disposition().recoveryAction(),
                    output.disposition().resumeFrom(),
                    output.disposition().stopReceipt().retryable(), metrics);
        }

        byte[] reportBytes = reportBytes(request, projection, output, validation);
        byte[] receiptBytes = receiptBytes(request, projection, output, validation);
        String responseIdentity = EditorialCanonicalJson.sha256Hex(primary.responseBytes());
        if (metrics.repairCalls > 0) {
            // The repair response is the final response identity when a repair was used.
            // The response bytes are not retained; only this redacted fingerprint is.
            responseIdentity = EditorialCanonicalJson.sha256Hex(
                    (responseIdentity + "\n" + output.semanticFingerprint()).getBytes(StandardCharsets.UTF_8));
        }
        EditorialP5PilotResult.Metrics finalMetrics = metrics.snapshot();
        EditorialP5PilotResult.CommittedResult committed = new EditorialP5PilotResult.CommittedResult(
                request.attemptIdentity(), requestIdentity, responseIdentity, reportBytes, receiptBytes,
                finalMetrics, output);
        try {
            store.commit(committed);
        } catch (RuntimeException error) {
            recover(store, request.attemptIdentity(), "RETRY_ATOMIC_RESULT_COMMIT_FAILED");
            return stopped(requestIdentity, EditorialP5PilotResult.StopClass.RETRY_REQUIRED,
                    "RETRY_ATOMIC_RESULT_COMMIT_FAILED", request.phase(), "ATOMIC_RESULT",
                    List.of(), request.chapterKey(), "Recover the attempt before an explicit retry",
                    request.phase(), true, metrics);
        }
        if (output.disposition().isPreserveDraft()) {
            metrics.preserveDraftCount = 1;
            finalMetrics = metrics.snapshot();
            // The store already contains the immutable result with the metrics
            // captured immediately before commit; preserve is still never canon.
            return EditorialP5PilotResult.preserved(requestIdentity, committed,
                    output.disposition().reasonCode(), finalMetrics);
        }
        return EditorialP5PilotResult.committed(requestIdentity, committed, finalMetrics);
    }

    private EditorialSourcePreflight.Result runPreflight(EditorialP5PilotRequest request) {
        Map<String, EditorialSourcePreflight.SourceInput> inputs = new LinkedHashMap<>();
        for (EditorialP5PilotRequest.SourceBytes source : request.sources()) {
            inputs.put(source.role(), new EditorialSourcePreflight.SourceInput(
                    source.role(), source.sourceReference(), source.bytes(), true,
                    source.actualByteLength(), source.bytes() == null ? null : source.actualSha256(),
                    source.schemaId()));
        }
        EditorialSourcePreflight.Request preflightRequest;
        if (EditorialSafe4Contract.NORMAL_MODE.equals(request.binding().sourceMode())) {
            preflightRequest = EditorialSourcePreflight.Request.normal(inputs,
                    request.evidenceContentSufficient());
        } else if (EditorialSafe4Contract.ALTERNATE_MODE.equals(request.binding().sourceMode())) {
            preflightRequest = new EditorialSourcePreflight.Request(
                    EditorialSourcePreflight.Mode.ALTERNATE_EXPLICIT, true, inputs,
                    request.evidenceContentSufficient());
        } else {
            return new EditorialSourcePreflight.Result(
                    EditorialSourcePreflight.Outcome.INPUT_REQUIRED,
                    "INPUT_SOURCE_MODE_INVALID", EditorialSourcePreflight.Stage.EXACT_INVENTORY,
                    List.of(), Map.of(), true, 0, 0);
        }
        return new EditorialSourcePreflight().evaluate(preflightRequest);
    }

    private static String validatePackEnvelope(EditorialP5PilotRequest request) {
        EditorialP4Binding binding = request.binding();
        EditorialPackManifest manifest = request.manifest();
        String fingerprint = request.manifestFingerprint();
        if (!binding.manifestFingerprint().equals(fingerprint)) return "P5_MANIFEST_FINGERPRINT_MISMATCH";
        if (!binding.canonicalPackHash().equals(manifest.canonicalPackHash())
                || !manifest.canonicalPackHash().equals(manifest.calculatedCanonicalPackHash())) {
            return "P5_CANONICAL_PACK_HASH_MISMATCH";
        }
        if (!EditorialSafe4Contract.CONTRACT_VERSION.equals(manifest.contractVersion())
                || !EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION.equals(manifest.schemaVersion())) {
            return "P5_CONTRACT_SCHEMA_MISMATCH";
        }
        for (EditorialPackManifest.FileEntry file : manifest.fileEntries()) {
            byte[] authority = request.authority().bytes(file.role());
            if (authority == null || authority.length != file.byteLength()
                    || !file.sha256().equals(EditorialCanonicalJson.sha256Hex(authority))) {
                return "P5_AUTHORITY_BYTES_MISMATCH:" + file.role().name();
            }
        }
        return null;
    }

    private static String validatePinnedSources(EditorialP5PilotRequest request) {
        Map<String, EditorialP5PilotRequest.SourceBytes> byKey = new LinkedHashMap<>();
        Set<String> known = Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                EditorialSafe4Contract.GLOSSARY, EditorialSafe4Contract.PRONOUN,
                EditorialSafe4Contract.PAIR_CONTEXT);
        for (EditorialP5PilotRequest.SourceBytes source : request.sources()) {
            if (!known.contains(source.role())) return "INPUT_SOURCE_ROLE_INVALID";
            String key = source.role() + "\u0000" + source.ordinal();
            if (byKey.put(key, source) != null) return "INPUT_SOURCE_IDENTITY_AMBIGUOUS";
        }
        Map<String, EditorialP4SourceIdentity> bound = new LinkedHashMap<>();
        for (EditorialP4SourceIdentity identity : request.binding().inputs()) {
            String key = identity.role() + "\u0000" + identity.ordinal();
            if (bound.put(key, identity) != null) return "P5_BOUND_SOURCE_IDENTITY_AMBIGUOUS";
        }
        for (Map.Entry<String, EditorialP5PilotRequest.SourceBytes> entry : byKey.entrySet()) {
            EditorialP4SourceIdentity expected = bound.get(entry.getKey());
            if (expected == null) return "STALE_SOURCE_NOT_IN_BINDING";
            EditorialP5PilotRequest.SourceBytes actual = entry.getValue();
            if (!expected.sourceReference().equals(actual.sourceReference())
                    || !expected.encoding().equals(actual.encoding())
                    || !expected.schemaStatus().equals(actual.schemaStatus())) {
                return "STALE_SOURCE_METADATA:" + actual.role();
            }
            // A null byte value models a present handle whose bytes cannot yet
            // be read; let P3B preflight return RETRY_SOURCE_BYTES_UNAVAILABLE.
            if (actual.bytes() != null && (expected.byteLength() != actual.actualByteLength()
                    || !expected.sha256().equalsIgnoreCase(actual.actualSha256()))) {
                return "STALE_SOURCE_BYTES:" + actual.role();
            }
        }
        return null;
    }

    private static boolean bindingMatches(EditorialP5PilotRequest request,
                                          EditorialP5PilotAuthorization authorization) {
        EditorialP4Binding binding = request.binding();
        return authorization.projectBindingIdentity().equals(binding.bindingIdentity())
                && authorization.runDeclarationIdentity().equals(binding.runDeclarationIdentity())
                && authorization.canonicalPackHash().equals(binding.canonicalPackHash())
                && authorization.canonicalProfileHash().equals(binding.canonicalProfileHash())
                && authorization.compatibilityEvaluationId().equals(binding.compatibilityEvaluationId())
                && authorization.chapterKey().equals(request.chapterKey());
    }

    private static boolean isL1ExecutionPhase(String phase) {
        return "L1_RAW_DISCOVERY".equals(phase) || "L1_RECONCILE".equals(phase);
    }

    private static Map<String, byte[]> visibleSourceBytes(
            EditorialPhaseContextProjector.PhaseProjection projection) {
        TreeMap<String, byte[]> result = new TreeMap<>();
        for (Map.Entry<String, EditorialPhaseContextProjector.BundleAsset> entry
                : projection.visibleAssets().entrySet()) {
            result.put(entry.getKey(), entry.getValue().bytes());
        }
        return Collections.unmodifiableMap(result);
    }

    private static int contextSize(EditorialP5PilotRequest request,
                                   EditorialPhaseContextProjector.PhaseProjection projection) {
        long total = 0L;
        for (EditorialPhaseContextProjector.BundleAsset asset : projection.visibleAssets().values()) {
            if (asset.bytes() != null) total += asset.bytes().length;
        }
        for (EditorialPackFileRole role : EditorialPackFileRole.values()) {
            byte[] authority = request.authority().bytes(role);
            if (authority != null) total += authority.length;
        }
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private static String requestEnvelopeHash(EditorialP5PilotRequest request,
                                              EditorialPhaseContextProjector.PhaseProjection projection,
                                              EditorialP5PilotAuthorization authorization) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("contractVersion", EditorialSafe4Contract.CONTRACT_VERSION);
        root.put("outputSchema", L1_OUTPUT_SCHEMA);
        root.put("attemptIdentity", request.attemptIdentity());
        root.put("requestIdentity", request.requestIdentity());
        root.put("bindingIdentity", request.binding().bindingIdentity());
        root.put("runDeclarationIdentity", request.binding().runDeclarationIdentity());
        root.put("canonicalPackHash", request.binding().canonicalPackHash());
        root.put("canonicalProfileHash", request.binding().canonicalProfileHash());
        root.put("compatibilityEvaluationId", request.binding().compatibilityEvaluationId());
        root.put("manifestFingerprint", request.manifestFingerprint());
        root.put("chapterKey", request.chapterKey());
        root.put("phase", request.phase());
        root.put("provider", authorization.provider());
        root.put("model", authorization.model());
        root.put("stableAnchors", request.stableAnchors());
        root.put("populationIds", request.populationIds());
        root.put("visibleSources", sourceFingerprints(projection));
        return EditorialCanonicalJson.sha256Hex(EditorialCanonicalJson.canonicalize(root)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static List<Object> sourceFingerprints(
            EditorialPhaseContextProjector.PhaseProjection projection) {
        ArrayList<Object> result = new ArrayList<>();
        ArrayList<EditorialPhaseContextProjector.BundleAsset> assets = new ArrayList<>(
                projection.visibleAssets().values());
        assets.sort(Comparator.comparing(EditorialPhaseContextProjector.BundleAsset::role));
        for (EditorialPhaseContextProjector.BundleAsset asset : assets) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("role", asset.role());
            value.put("sourceId", asset.sourceId());
            value.put("byteLength", BigDecimal.valueOf(asset.bytes() == null ? -1 : asset.bytes().length));
            value.put("sha256", asset.sha256());
            value.put("schemaId", asset.schemaId());
            result.add(value);
        }
        return result;
    }

    private static boolean isTruncated(EditorialP5PilotProvider.Response response) {
        if (!response.transportComplete()) return true;
        String finish = response.finishReason().toLowerCase(java.util.Locale.ROOT);
        return "length".equals(finish) || "max_tokens".equals(finish)
                || "max_output_tokens".equals(finish);
    }

    private boolean overBudget(EditorialP5PilotAuthorization authorization, MetricState metrics) {
        return metrics.totalTokens > authorization.maximumTotalTokens()
                || metrics.inputTokens > authorization.maximumInputTokens()
                || metrics.outputTokens > authorization.maximumOutputTokens()
                || metrics.actualReportedCost.compareTo(authorization.maximumTotalCost()) > 0
                || Math.max(0L, clock.nowMillis() - metrics.startedAt)
                > authorization.maximumExecutionTimeMillis();
    }

    private static Validation validateOutput(EditorialP5PilotRequest request,
                                             EditorialPhaseContextProjector.PhaseProjection projection,
                                             EditorialP5L1Output output) {
        ArrayList<String> issues = new ArrayList<>();
        if (!L1_OUTPUT_SCHEMA.equals(output.reportSchemaVersion())) issues.add("REPORT_L1_SCHEMA_INVALID");
        if (!EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION.equals(output.receiptSchemaVersion())) {
            issues.add("RECEIPT_SCHEMA_INVALID");
        }
        if (!request.binding().bindingIdentity().equals(output.bindingIdentity())) issues.add("OUTPUT_BINDING_IDENTITY_MISMATCH");
        if (!request.manifestFingerprint().equals(output.manifestFingerprint())) issues.add("OUTPUT_MANIFEST_IDENTITY_MISMATCH");
        if (!request.chapterKey().equals(output.chapterKey())) issues.add("OUTPUT_CHAPTER_IDENTITY_MISMATCH");
        if (!"L1".equals(output.phase())) issues.add("OUTPUT_PHASE_INVALID");
        if (!projection.bundleIdentity().equals(output.bundleIdentity())) issues.add("OUTPUT_BUNDLE_IDENTITY_MISMATCH");
        if (!request.predecessorIdentity().equals(output.predecessorIdentity())) issues.add("OUTPUT_PREDECESSOR_MISMATCH");
        if (!request.stableAnchors().equals(output.stableAnchors())) issues.add("OUTPUT_ANCHORS_MISMATCH");
        if (!request.populationIds().equals(output.ledger().populationIds())) issues.add("OUTPUT_POPULATION_MISMATCH");
        EditorialLedgerValidator.Result ledger = new EditorialLedgerValidator().validate(output.ledger());
        if (!ledger.valid()) issues.addAll(ledger.issues().stream().map(
                issue -> issue.code() + ":" + issue.itemId()).toList());
        EditorialReceiptValidator.ReceiptDocument document = new EditorialReceiptValidator.ReceiptDocument(
                output.receiptSchemaVersion(), "REPORT_L1", output.bundleIdentity(),
                output.predecessorIdentity(), output.stableAnchors(), output.ledger(),
                output.evidenceRefs(), output.gates(), output.preservedInventory(),
                output.declaredChanges(), output.beforeText(), output.afterText(),
                output.releaseAttemptCount(), output.disposition(), output.modelDeclaredPass());
        EditorialReceiptValidator.Result receipt = new EditorialReceiptValidator().validate(document);
        if (!receipt.valid()) issues.addAll(receipt.issues());
        return new Validation(issues.isEmpty(), List.copyOf(issues), receipt, ledger,
                new EditorialDiffValidator().validate(output.beforeText(), output.afterText(),
                        output.declaredChanges(), output.modelDeclaredPass()));
    }

    private static byte[] reportBytes(EditorialP5PilotRequest request,
                                      EditorialPhaseContextProjector.PhaseProjection projection,
                                      EditorialP5L1Output output, Validation validation) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", output.reportSchemaVersion());
        report.put("artifactType", "REPORT_L1");
        report.put("bindingIdentity", request.binding().bindingIdentity());
        report.put("manifestFingerprint", request.manifestFingerprint());
        report.put("canonicalPackHash", request.binding().canonicalPackHash());
        report.put("canonicalProfileHash", request.binding().canonicalProfileHash());
        report.put("compatibilityEvaluationId", request.binding().compatibilityEvaluationId());
        report.put("chapterKey", request.chapterKey());
        report.put("phase", request.phase());
        report.put("bundleIdentity", projection.bundleIdentity());
        report.put("predecessorIdentity", output.predecessorIdentity());
        report.put("stableAnchors", output.stableAnchors());
        report.put("populationTotal", BigDecimal.valueOf(validation.ledger.populationTotal()));
        report.put("accountedTotal", BigDecimal.valueOf(validation.ledger.accountedTotal()));
        report.put("actualChangedSpans", BigDecimal.valueOf(validation.diff.actualChangedSpans().size()));
        report.put("gates", new TreeMap<>(output.gates()));
        report.put("evidenceRefs", output.evidenceRefs().stream().sorted().toList());
        report.put("preservedInventory", output.preservedInventory());
        report.put("disposition", output.disposition().disposition().name());
        report.put("modelDeclaredPassRecordedOnly", output.modelDeclaredPass());
        return EditorialCanonicalJson.canonicalize(report).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] receiptBytes(EditorialP5PilotRequest request,
                                       EditorialPhaseContextProjector.PhaseProjection projection,
                                       EditorialP5L1Output output, Validation validation) {
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("schemaVersion", output.receiptSchemaVersion());
        receipt.put("artifactType", "REPORT_L1");
        receipt.put("manifestRef", request.manifestFingerprint());
        receipt.put("packRef", request.binding().canonicalPackHash());
        receipt.put("profileRef", request.binding().canonicalProfileHash());
        receipt.put("bindingRef", request.binding().bindingIdentity());
        receipt.put("phase", request.phase());
        receipt.put("bundleIdentity", projection.bundleIdentity());
        receipt.put("predecessorIdentity", output.predecessorIdentity());
        receipt.put("populationTotal", BigDecimal.valueOf(validation.ledger.populationTotal()));
        receipt.put("accountedTotal", BigDecimal.valueOf(validation.ledger.accountedTotal()));
        receipt.put("changedSpanTotal", BigDecimal.valueOf(validation.diff.actualChangedSpans().size()));
        receipt.put("gates", new TreeMap<>(output.gates()));
        receipt.put("evidenceRefs", output.evidenceRefs().stream().sorted().toList());
        receipt.put("canonAllowed", validation.receipt.canonAllowed());
        receipt.put("propagationAllowed", validation.receipt.propagationAllowed());
        receipt.put("disposition", output.disposition().disposition().name());
        receipt.put("preservedInventory", output.preservedInventory());
        receipt.put("releaseAttemptCount", BigDecimal.valueOf(output.releaseAttemptCount()));
        return EditorialCanonicalJson.canonicalize(receipt).getBytes(StandardCharsets.UTF_8);
    }

    private static String safeRequestIdentity(EditorialP5PilotRequest request) {
        try {
            return request.requestIdentity();
        } catch (RuntimeException error) {
            return "";
        }
    }

    private EditorialP5PilotResult stopped(String requestIdentity,
                                            EditorialP5PilotResult.StopClass stopClass,
                                            String reasonCode, String phase,
                                            String gate, List<String> evidenceRefs,
                                            String affectedScope, String recoveryAction,
                                            String resumeFrom, boolean retryable,
                                            MetricState metrics) {
        return EditorialP5PilotResult.stopped(requestIdentity, stopClass, reasonCode, phase,
                gate, evidenceRefs, affectedScope, recoveryAction, resumeFrom, retryable,
                metrics.snapshot());
    }

    private static String recoveryFor(String reasonCode) {
        if (reasonCode != null && reasonCode.startsWith("INPUT_")) {
            return "Restore or explicitly select the required input before retrying";
        }
        return "Resolve the source access issue before retrying";
    }

    private static void recover(AttemptStore store, String attemptIdentity, String reasonCode) {
        try {
            store.markRecoveryRequired(attemptIdentity, reasonCode);
        } catch (RuntimeException ignored) {
            // The typed result still reports the original failure.  A real
            // store must surface this through its own durable recovery monitor.
        }
    }

    private record Validation(boolean valid, List<String> issues,
                              EditorialReceiptValidator.Result receipt,
                              EditorialLedgerValidator.Result ledger,
                              EditorialDiffValidator.Result diff) { }

    private static final class MetricState {
        private final long startedAt;
        private final Clock clock;
        private int providerCallsBeforePreflight;
        private int primaryCalls;
        private int repairCalls;
        private int networkRetries;
        private int inputTokens;
        private int outputTokens;
        private int totalTokens;
        private BigDecimal estimatedCost = BigDecimal.ZERO;
        private BigDecimal actualReportedCost = BigDecimal.ZERO;
        private int requestContextSize;
        private String finishReason = "NOT_CALLED";
        private boolean truncated;
        private boolean schemaValidationPassed;
        private boolean receiptValidationPassed;
        private int preserveDraftCount;
        private int findingCount;
        private int falseStopCount;

        private MetricState(long startedAt, Clock clock) {
            this.startedAt = startedAt;
            this.clock = clock;
        }

        private void record(EditorialP5PilotProvider.Response response) {
            if (response == null) return;
            inputTokens = safeAdd(inputTokens, response.inputTokens());
            outputTokens = safeAdd(outputTokens, response.outputTokens());
            totalTokens = safeAdd(totalTokens, response.totalTokens());
            actualReportedCost = actualReportedCost.add(response.reportedCost());
            finishReason = response.finishReason();
            truncated = isTruncated(response);
        }

        private EditorialP5PilotResult.Metrics snapshot() {
            long elapsed = Math.max(0L, clock.nowMillis() - startedAt);
            return new EditorialP5PilotResult.Metrics(providerCallsBeforePreflight,
                    primaryCalls, repairCalls, networkRetries, inputTokens, outputTokens,
                    totalTokens, estimatedCost, actualReportedCost, requestContextSize,
                    finishReason, truncated, schemaValidationPassed, receiptValidationPassed,
                    preserveDraftCount, findingCount, falseStopCount, elapsed);
        }

        private static int safeAdd(int left, int right) {
            long value = (long) left + right;
            return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
        }
    }
}
