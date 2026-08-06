package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageCallerSelection;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageContextResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageCreationCode;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageCreationRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageCreationResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRuntimeService;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageValidationCode;
import com.ml.tblandroidtxt.editorial.pack.EditorialRunLineageBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Option-B coordinator for one explicit immutable closure event.
 *
 * <p>The coordinator owns the application clock and orchestration boundary,
 * but not production event creation. The default event resolver is deliberately
 * unavailable until a real RUN_CONTEXT_CLOSED owner is approved.</p>
 */
public final class EditorialRunContextCoordinator {
    public static final String BINDING_CONTRACT_VERSION = "editorial-run-lineage-binding-v1";

    private final EditorialRunClosureEventResolver eventResolver;
    private final EditorialAuthoritativeIdentityCreator identityCreator;
    private final EditorialLineageContextResolver contextResolver;
    private final EditorialLineageRuntimeService lineageRuntime;
    private final EditorialLineageAndBindingTransactionService transaction;
    private final EditorialClosedRunContextDao closedRuns;
    private final EditorialLineageDao lineages;
    private final EditorialRunLineageBindingDao bindings;
    private final EditorialCoordinatorClock clock;

    /** Production boundary; it performs no work while the closure event is absent. */
    public EditorialRunContextCoordinator(TranslationRepository database) {
        this(database,
                new EditorialProductionRunClosureEventResolver(),
                new EditorialAuthoritativeIdentityCreator(database),
                new SqliteEditorialLineageContextResolver(database),
                new EditorialLineageRuntimeService(new SqliteEditorialLineageContextResolver(database)),
                new EditorialLineageAndBindingTransactionService(database),
                new EditorialCoordinatorClock() {
                    @Override public long now() { return System.currentTimeMillis(); }
                });
    }

    /** Injectable only inside the app package for isolated test-only fixtures. */
    EditorialRunContextCoordinator(
            TranslationRepository database,
            EditorialRunClosureEventResolver eventResolver,
            EditorialAuthoritativeIdentityCreator identityCreator,
            EditorialLineageContextResolver contextResolver,
            EditorialLineageRuntimeService lineageRuntime,
            EditorialLineageAndBindingTransactionService transaction,
            EditorialCoordinatorClock clock) {
        Objects.requireNonNull(database, "database");
        this.eventResolver = Objects.requireNonNull(eventResolver, "event-resolver");
        this.identityCreator = Objects.requireNonNull(identityCreator, "identity-creator");
        this.contextResolver = Objects.requireNonNull(contextResolver, "context-resolver");
        this.lineageRuntime = Objects.requireNonNull(lineageRuntime, "lineage-runtime");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.closedRuns = new EditorialClosedRunContextDao(database);
        this.lineages = new EditorialLineageDao(database);
        this.bindings = new EditorialRunLineageBindingDao(database);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Coordinates one explicit command; retrying requires the same event selector. */
    public synchronized EditorialRunContextCoordinatorResult coordinate(
            EditorialRunContextCommand command) {
        if (command == null || blank(command.closureEventSelector())) {
            return result(EditorialRunContextCoordinatorStage.COMMAND_VALIDATION,
                    EditorialRunContextCoordinatorCode.COMMAND_INVALID, false,
                    "", "", "command-selector-required", null, List.of(), null);
        }

        EditorialClosureEventResolutionResult resolved = resolveEvent(command.closureEventSelector());
        if (resolved.code() != EditorialClosureEventResolutionCode.RESOLVED) {
            return result(EditorialRunContextCoordinatorStage.CLOSURE_EVENT_RESOLUTION,
                    mapResolution(resolved.code()), resolved.code()
                            == EditorialClosureEventResolutionCode.CLOSURE_EVENT_UNAVAILABLE,
                    "", "", resolved.detail(), null, List.of(), null);
        }
        EditorialRunClosureEvent event = resolved.event().orElse(null);
        if (event == null || !event.eventIdentity().equals(command.closureEventSelector())) {
            return result(EditorialRunContextCoordinatorStage.CLOSURE_EVENT_RESOLUTION,
                    EditorialRunContextCoordinatorCode.CLOSURE_EVENT_COLLISION, false,
                    "", "", "selector-event-identity-mismatch", null, List.of(), null);
        }
        if (!event.isCanonical()) {
            return result(EditorialRunContextCoordinatorStage.CLOSURE_EVENT_RESOLUTION,
                    EditorialRunContextCoordinatorCode.CLOSURE_EVENT_COLLISION, false,
                    "", "", "closure-event-canonical-bytes-mismatch", null, List.of(), null);
        }
        if (event.eligibility() != EditorialClosureEventEligibility.ELIGIBLE) {
            return result(EditorialRunContextCoordinatorStage.CLOSURE_EVENT_RESOLUTION,
                    EditorialRunContextCoordinatorCode.CLOSURE_EVENT_INVALID, false,
                    "", "", "closure-event-not-eligible", null, List.of(), null);
        }

        long operationTimestamp;
        try {
            operationTimestamp = clock.now();
        } catch (RuntimeException ignored) {
            return result(EditorialRunContextCoordinatorStage.CLOSED_CONTEXT,
                    EditorialRunContextCoordinatorCode.CLOSE_REJECTED, true,
                    "", "", "trusted-clock-failure", null, List.of(), null);
        }
        if (operationTimestamp < 0) {
            return result(EditorialRunContextCoordinatorStage.CLOSED_CONTEXT,
                    EditorialRunContextCoordinatorCode.CLOSE_REJECTED, false,
                    "", "", "trusted-clock-invalid", null, List.of(), null);
        }

        EditorialIdentityAppendResult<EditorialClosedRunContext> close = close(event, operationTimestamp);
        if (!accepted(close)) {
            return result(EditorialRunContextCoordinatorStage.CLOSED_CONTEXT,
                    EditorialRunContextCoordinatorCode.CLOSE_REJECTED, retryable(close.code()),
                    "", "", "closed-context-rejected:" + close.detail(), null, List.of(), close.code());
        }
        EditorialClosedRunContext closed = close.value();
        Optional<EditorialClosedRunContext> closedReadback = closedRuns.findByIdentity(close.identity());
        if (closed == null || closedReadback.isEmpty() || !closed.equals(closedReadback.get())) {
            return result(EditorialRunContextCoordinatorStage.CLOSED_CONTEXT,
                    EditorialRunContextCoordinatorCode.CLOSE_REJECTED, true,
                    close.identity(), "", "closed-context-readback-mismatch", null, List.of(),
                    EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE);
        }
        if (!matchesEvent(event, closed)) {
            return result(EditorialRunContextCoordinatorStage.CLOSED_CONTEXT,
                    EditorialRunContextCoordinatorCode.CLOSURE_EVENT_COLLISION, false,
                    closed.closedRunIdentity(), "", "closure-event-closed-context-mismatch", null,
                    List.of(), EditorialIdentityPersistenceCode.COMPATIBILITY_CONTEXT_MISMATCH);
        }

        EditorialLineageCallerSelection selection = new EditorialLineageCallerSelection(
                closed.projectRevisionIdentity(), closed.scopeSnapshotIdentity(), closed.closedRunIdentity(),
                event.nodeKind(), event.parentRecordIdentity());
        Optional<EditorialRunLineageBinding> existingBinding = bindings.findByClosedRunIdentity(
                closed.closedRunIdentity());
        if (existingBinding.isPresent()) {
            Optional<EditorialLineageRecord> existingLineage = lineages.findByRecordIdentity(
                    existingBinding.get().lineageRecordIdentity());
            if (existingLineage.isEmpty() || !existingBinding.get().lineageRecordFingerprint()
                    .equals(existingLineage.get().recordFingerprint())
                    || !recordMatchesEvent(existingLineage.get(), event, closed)) {
                return resultWithBinding(EditorialRunContextCoordinatorStage.READBACK,
                        EditorialRunContextCoordinatorCode.BINDING_CONFLICT, false,
                        closed.closedRunIdentity(), existingBinding.get().lineageRecordIdentity(),
                        existingBinding.get().bindingIdentity(), "existing-binding-conflict",
                        null, List.of(), EditorialIdentityPersistenceCode.BINDING_MISMATCH);
            }
            return resultWithBinding(EditorialRunContextCoordinatorStage.READBACK,
                    EditorialRunContextCoordinatorCode.ALREADY_BOUND, false,
                    closed.closedRunIdentity(), existingLineage.get().recordIdentity(),
                    existingBinding.get().bindingIdentity(), "exact-replay",
                    EditorialLineageCreationCode.READY_TO_APPEND, List.of(),
                    EditorialIdentityPersistenceCode.ALREADY_EXISTS);
        }

        EditorialLineageCreationResult preparation;
        EditorialLineageRecord record = existingUnboundRecord(event, closed, selection);
        if (record != null) {
            preparation = EditorialLineageCreationResult.ready(record);
        } else {
            List<EditorialLineageRecord> sameRun = lineages.listByRunEvaluationIdentity(
                    closed.closedRunIdentity());
            if (!sameRun.isEmpty()) {
                return result(EditorialRunContextCoordinatorStage.LINEAGE_VALIDATION,
                        EditorialRunContextCoordinatorCode.BINDING_CONFLICT, false,
                        closed.closedRunIdentity(), sameRun.get(0).recordIdentity(),
                        "existing-lineage-does-not-match-event", null, List.of(),
                        EditorialIdentityPersistenceCode.BINDING_MISMATCH);
            }
            try {
                preparation = lineageRuntime.validateAndPrepare(new EditorialLineageCreationRequest(selection));
            } catch (RuntimeException ignored) {
                return result(EditorialRunContextCoordinatorStage.LINEAGE_VALIDATION,
                        EditorialRunContextCoordinatorCode.LINEAGE_VALIDATION_REJECTED, false,
                        closed.closedRunIdentity(), "", "lineage-validation-exception", null, List.of(), null);
            }
        }
        if (preparation == null || !preparation.isReadyToAppend()) {
            return result(EditorialRunContextCoordinatorStage.LINEAGE_VALIDATION,
                    EditorialRunContextCoordinatorCode.LINEAGE_VALIDATION_REJECTED, false,
                    closed.closedRunIdentity(), "", "closed-unbound:lineage-validation-rejected",
                    preparation == null ? null : preparation.code(),
                    preparation == null ? List.of() : preparation.validationCodes(), null);
        }
        record = preparation.record().orElse(null);
        if (record == null) {
            return result(EditorialRunContextCoordinatorStage.LINEAGE_VALIDATION,
                    EditorialRunContextCoordinatorCode.LINEAGE_VALIDATION_REJECTED, false,
                    closed.closedRunIdentity(), "", "ready-result-without-record",
                    EditorialLineageCreationCode.VALIDATION_FAILED, List.of(), null);
        }

        EditorialRunLineageBinding binding;
        try {
            binding = new EditorialRunLineageBinding(BINDING_CONTRACT_VERSION,
                    closed.closedRunIdentity(), record.recordIdentity(), record.recordFingerprint());
        } catch (RuntimeException ignored) {
            return result(EditorialRunContextCoordinatorStage.LINEAGE_PERSISTENCE,
                    EditorialRunContextCoordinatorCode.LINEAGE_PERSISTENCE_FAILED, false,
                    closed.closedRunIdentity(), record.recordIdentity(), "binding-construction-failed",
                    preparation.code(), preparation.validationCodes(),
                    EditorialIdentityPersistenceCode.INVALID_IMMUTABLE_RECORD);
        }
        EditorialLineageAndBindingResult persisted;
        try {
            persisted = transaction.appendAndBind(record, binding, operationTimestamp, operationTimestamp);
        } catch (RuntimeException ignored) {
            return result(EditorialRunContextCoordinatorStage.LINEAGE_PERSISTENCE,
                    EditorialRunContextCoordinatorCode.LINEAGE_PERSISTENCE_FAILED, true,
                    closed.closedRunIdentity(), record.recordIdentity(), "shared-transaction-failure",
                    preparation.code(), preparation.validationCodes(),
                    EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE);
        }
        if (persisted == null || !persisted.committed()) {
            EditorialIdentityPersistenceCode persistenceCode = persisted == null
                    ? EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE : persisted.code();
            EditorialRunContextCoordinatorCode coordinatorCode = isBindingConflict(persistenceCode)
                    ? EditorialRunContextCoordinatorCode.BINDING_CONFLICT
                    : EditorialRunContextCoordinatorCode.LINEAGE_PERSISTENCE_FAILED;
            return result(EditorialRunContextCoordinatorStage.LINEAGE_PERSISTENCE,
                    coordinatorCode, retryable(persistenceCode), closed.closedRunIdentity(),
                    record.recordIdentity(), "closed-unbound:" + (persisted == null ? "null" : persisted.detail()),
                    preparation.code(), preparation.validationCodes(), persistenceCode);
        }

        Optional<EditorialLineageRecord> lineageReadback = lineages.findByRecordIdentity(record.recordIdentity());
        Optional<EditorialRunLineageBinding> bindingReadback =
                bindings.findByClosedRunIdentity(closed.closedRunIdentity());
        if (lineageReadback.isEmpty() || !record.equals(lineageReadback.get())
                || bindingReadback.isEmpty() || !binding.equals(bindingReadback.get())) {
            return resultWithBinding(EditorialRunContextCoordinatorStage.READBACK,
                    bindingReadback.isPresent()
                            ? EditorialRunContextCoordinatorCode.BINDING_CONFLICT
                            : EditorialRunContextCoordinatorCode.LINEAGE_PERSISTENCE_FAILED,
                    true, closed.closedRunIdentity(), record.recordIdentity(),
                    bindingReadback.map(EditorialRunLineageBinding::bindingIdentity).orElse(binding.bindingIdentity()),
                    "exact-lineage-binding-readback-mismatch", preparation.code(),
                    preparation.validationCodes(), EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE);
        }
        boolean alreadyBound = persisted.bindingResult() != null
                && persisted.bindingResult().code() == EditorialIdentityPersistenceCode.ALREADY_EXISTS;
        return resultWithBinding(EditorialRunContextCoordinatorStage.READBACK,
                alreadyBound ? EditorialRunContextCoordinatorCode.ALREADY_BOUND
                        : EditorialRunContextCoordinatorCode.LINEAGE_BOUND,
                false, closed.closedRunIdentity(), record.recordIdentity(), binding.bindingIdentity(),
                alreadyBound ? "exact-replay" : "lineage-and-binding-committed",
                preparation.code(), preparation.validationCodes(), persisted.code());
    }

    /** Read-only projection. It never appends, retries, chooses latest, or mutates state. */
    public synchronized EditorialRunContextStatus inspect(EditorialRunContextCommand command) {
        if (command == null || blank(command.closureEventSelector())) {
            return EditorialRunContextStatus.of(EditorialRunContextStatusCode.EVENT_INVALID,
                    null, "", "", "", "command-selector-required");
        }
        EditorialClosureEventResolutionResult resolved = resolveEvent(command.closureEventSelector());
        if (resolved.code() == EditorialClosureEventResolutionCode.CLOSURE_EVENT_UNAVAILABLE) {
            return EditorialRunContextStatus.of(EditorialRunContextStatusCode.EVENT_UNAVAILABLE,
                    null, "", "", "", resolved.detail());
        }
        if (resolved.code() == EditorialClosureEventResolutionCode.CLOSURE_EVENT_COLLISION) {
            return EditorialRunContextStatus.of(EditorialRunContextStatusCode.EVENT_COLLISION,
                    null, "", "", "", resolved.detail());
        }
        EditorialRunClosureEvent event = resolved.event().orElse(null);
        if (event == null || !event.isCanonical()
                || !event.eventIdentity().equals(command.closureEventSelector())
                || event.eligibility() != EditorialClosureEventEligibility.ELIGIBLE) {
            return EditorialRunContextStatus.of(EditorialRunContextStatusCode.EVENT_INVALID,
                    event, "", "", "", "closure-event-invalid-or-ineligible");
        }
        List<EditorialClosedRunContext> candidates = new ArrayList<>();
        for (EditorialClosedRunContext candidate : closedRuns.listByScope(event.inputScopeSelector())) {
            if (matchesEvent(event, candidate)) candidates.add(candidate);
        }
        if (candidates.isEmpty()) {
            return EditorialRunContextStatus.of(EditorialRunContextStatusCode.NO_CLOSED_CONTEXT,
                    event, "", "", "", "closed-context-not-found");
        }
        if (candidates.size() != 1) {
            return EditorialRunContextStatus.of(EditorialRunContextStatusCode.AMBIGUOUS,
                    event, "", "", "", "multiple-closed-context-matches");
        }
        EditorialClosedRunContext closed = candidates.get(0);
        Optional<EditorialRunLineageBinding> binding = bindings.findByClosedRunIdentity(
                closed.closedRunIdentity());
        if (binding.isPresent()) {
            EditorialRunLineageBinding storedBinding = binding.get();
            Optional<EditorialLineageRecord> storedLineage = lineages.findByRecordIdentity(
                    storedBinding.lineageRecordIdentity());
            if (storedLineage.isEmpty()
                    || !storedBinding.closedRunIdentity().equals(closed.closedRunIdentity())
                    || !storedBinding.lineageRecordFingerprint().equals(storedLineage.get().recordFingerprint())
                    || !recordMatchesEvent(storedLineage.orElse(null), event, closed)) {
                return EditorialRunContextStatus.of(EditorialRunContextStatusCode.CORRUPT_MISMATCH,
                        event, closed.closedRunIdentity(), storedBinding.lineageRecordIdentity(),
                        storedBinding.bindingIdentity(), "binding-lineage-readback-mismatch");
            }
            return EditorialRunContextStatus.of(EditorialRunContextStatusCode.LINEAGE_BOUND,
                    event, closed.closedRunIdentity(), storedLineage.get().recordIdentity(),
                    storedBinding.bindingIdentity(), "exact-lineage-binding-readback");
        }

        EditorialLineageCreationResult preparation;
        try {
            preparation = lineageRuntime.validateAndPrepare(new EditorialLineageCreationRequest(
                    new EditorialLineageCallerSelection(closed.projectRevisionIdentity(),
                            closed.scopeSnapshotIdentity(), closed.closedRunIdentity(),
                            event.nodeKind(), event.parentRecordIdentity())));
        } catch (RuntimeException ignored) {
            return EditorialRunContextStatus.of(EditorialRunContextStatusCode.CLOSED_UNBOUND,
                    event, closed.closedRunIdentity(), "", "", "closed-context-exists");
        }
        if (preparation != null && preparation.isReadyToAppend()
                && preparation.record().isPresent()) {
            EditorialLineageRecord expected = preparation.record().get();
            Optional<EditorialLineageRecord> existing = lineages.findByRecordIdentity(expected.recordIdentity());
            if (existing.isPresent()) {
                if (!expected.equals(existing.get())) {
                    return EditorialRunContextStatus.of(EditorialRunContextStatusCode.CORRUPT_MISMATCH,
                            event, closed.closedRunIdentity(), expected.recordIdentity(), "",
                            "lineage-canonical-readback-mismatch");
                }
                return EditorialRunContextStatus.of(EditorialRunContextStatusCode.LINEAGE_EXISTS_UNBOUND,
                        event, closed.closedRunIdentity(), expected.recordIdentity(), "",
                        "lineage-exists-without-binding");
            }
        }
        return EditorialRunContextStatus.of(EditorialRunContextStatusCode.CLOSED_UNBOUND,
                event, closed.closedRunIdentity(), "", "", "closed-context-exists-without-binding");
    }

    private EditorialClosureEventResolutionResult resolveEvent(String selector) {
        try {
            EditorialClosureEventResolutionResult result = eventResolver.resolve(selector);
            return result == null ? EditorialClosureEventResolutionResult.failure(
                    EditorialClosureEventResolutionCode.CLOSURE_EVENT_UNAVAILABLE,
                    "closure-event-resolution-empty") : result;
        } catch (RuntimeException ignored) {
            return EditorialClosureEventResolutionResult.failure(
                    EditorialClosureEventResolutionCode.CLOSURE_EVENT_UNAVAILABLE,
                    "closure-event-resolution-failure");
        }
    }

    private EditorialIdentityAppendResult<EditorialClosedRunContext> close(
            EditorialRunClosureEvent event, long closedAt) {
        return identityCreator.closeRunContext(new EditorialClosedRunContextClosureRequest(
                event.projectRevisionSelector(), event.inputScopeSelector(),
                event.compatibilityEvaluationSelector(), event.runKind(), event.phaseIdentity(),
                event.sourceRunRowIdSelector(), closedAt));
    }

    private static boolean accepted(EditorialIdentityAppendResult<EditorialClosedRunContext> result) {
        return result != null && (result.code() == EditorialIdentityPersistenceCode.APPENDED
                || result.code() == EditorialIdentityPersistenceCode.ALREADY_EXISTS)
                && result.value() != null && !blank(result.identity());
    }

    private static boolean matchesEvent(EditorialRunClosureEvent event, EditorialClosedRunContext closed) {
        return event.projectRevisionSelector().equals(closed.projectRevisionIdentity())
                && event.inputScopeSelector().equals(closed.scopeSnapshotIdentity())
                && event.compatibilityEvaluationSelector().equals(closed.compatibilityEvaluationId())
                && event.runKind().equals(closed.runKind())
                && event.phaseIdentity().equals(closed.phaseIdentity())
                && event.frozenManifestFingerprint().equals(closed.inputManifestFingerprint());
    }

    private EditorialLineageRecord existingUnboundRecord(EditorialRunClosureEvent event,
                                                         EditorialClosedRunContext closed,
                                                         EditorialLineageCallerSelection selection) {
        List<EditorialLineageRecord> sameRun = lineages.listByRunEvaluationIdentity(
                closed.closedRunIdentity());
        if (sameRun.size() != 1) return null;
        EditorialLineageRecord existing = sameRun.get(0);
        if (!recordMatchesEvent(existing, event, closed)) return null;
        try {
            return contextResolver.resolve(selection) == null ? null : existing;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean recordMatchesEvent(EditorialLineageRecord record,
                                              EditorialRunClosureEvent event,
                                              EditorialClosedRunContext closed) {
        if (record == null || !record.computedFingerprint().equals(record.fingerprint())) return false;
        if (record.nodeKind() != event.nodeKind()) return false;
        if (!closed.closedRunIdentity().equals(record.identity().runEvaluationIdentity())
                || !closed.projectRevisionIdentity().equals(record.identity().projectIdentity())
                || !closed.scopeSnapshotIdentity().equals(record.identity().inputScopeIdentity())) return false;
        if (event.parentRecordIdentity() == null) return record.parentReference() == null;
        return record.parentReference() != null
                && event.parentRecordIdentity().equals(record.parentReference().parentRecordIdentity());
    }

    private static EditorialRunContextCoordinatorCode mapResolution(
            EditorialClosureEventResolutionCode code) {
        return switch (code) {
            case CLOSURE_EVENT_UNAVAILABLE -> EditorialRunContextCoordinatorCode.CLOSURE_EVENT_UNAVAILABLE;
            case CLOSURE_EVENT_INVALID -> EditorialRunContextCoordinatorCode.CLOSURE_EVENT_INVALID;
            case CLOSURE_EVENT_COLLISION -> EditorialRunContextCoordinatorCode.CLOSURE_EVENT_COLLISION;
            case RESOLVED -> throw new IllegalArgumentException("resolved-is-not-failure");
        };
    }

    private static boolean isBindingConflict(EditorialIdentityPersistenceCode code) {
        return code == EditorialIdentityPersistenceCode.BINDING_MISMATCH
                || code == EditorialIdentityPersistenceCode.DUPLICATE_IMMUTABLE_RECORD;
    }

    private static boolean retryable(EditorialIdentityPersistenceCode code) {
        return code == EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE
                || code == EditorialIdentityPersistenceCode.ATTEMPT_ALLOCATION_FAILURE
                || code == EditorialIdentityPersistenceCode.FOREIGN_KEY_RESTRICTED;
    }

    private static EditorialRunContextCoordinatorResult result(
            EditorialRunContextCoordinatorStage stage,
            EditorialRunContextCoordinatorCode code,
            boolean retryable,
            String closedRunIdentity,
            String lineageIdentity,
            String detail,
            EditorialLineageCreationCode creationCode,
            List<EditorialLineageValidationCode> validationCodes,
            EditorialIdentityPersistenceCode persistenceCode) {
        return resultWithBinding(stage, code, retryable, closedRunIdentity, lineageIdentity, "",
                detail, creationCode, validationCodes, persistenceCode);
    }

    private static EditorialRunContextCoordinatorResult resultWithBinding(
            EditorialRunContextCoordinatorStage stage,
            EditorialRunContextCoordinatorCode code,
            boolean retryable,
            String closedRunIdentity,
            String lineageIdentity,
            String bindingIdentity,
            String detail,
            EditorialLineageCreationCode creationCode,
            List<EditorialLineageValidationCode> validationCodes,
            EditorialIdentityPersistenceCode persistenceCode) {
        return EditorialRunContextCoordinatorResult.of(stage, code, retryable, closedRunIdentity,
                lineageIdentity, bindingIdentity, detail, creationCode, validationCodes, persistenceCode);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
