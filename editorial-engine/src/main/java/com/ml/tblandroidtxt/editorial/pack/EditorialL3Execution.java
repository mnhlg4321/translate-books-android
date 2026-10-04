package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Bounded L3 boundary: blind RAW-first re-audit (RAW, GLOSSARY, VI_L2 only),
 * then a reconcile call that sees the full chain, resolves every re-audit
 * candidate, declares QA change rows and records the two adversarial passes.
 *
 * <p>The app reconstructs VI_L2 to FINAL, checks that every DRAFT to FINAL
 * changed line is owned by an L2 or L3 change, computes the five release
 * numbers itself and commits FINAL + QA_RECEIPT in one store commit. A model
 * PASS label is never read as evidence; it only supplies accountable rows.</p>
 */
public final class EditorialL3Execution {
    public static final String REAUDIT_PHASE = "L3_RAW_FIRST_REAUDIT";
    public static final String RECONCILE_PHASE = "L3_RECONCILE";
    public static final String REAUDIT_WIRE = "safe4.l3.reaudit.wire.v1";
    public static final String RECONCILE_WIRE = "safe4.l3.reconcile.wire.v1";
    public static final String QA_RECEIPT_SCHEMA = "safe4.full.qa-receipt.v1";
    /** Receipt of the ledger contract: anchored probes, carried defects and a real final read of FINAL. */
    public static final String QA_RECEIPT_SCHEMA_V2 = "safe4.full.qa-receipt.v2";
    public static final String REAUDIT_WIRE_V3 = EditorialL3Ledger.REAUDIT_WIRE_V3;
    public static final String RECONCILE_WIRE_V3 = EditorialL3Ledger.RECONCILE_WIRE_V3;
    public static final int MAX_CANDIDATES = 300;
    public static final int MAX_PROBES = 40;
    static final Set<String> LEDGERS = Set.of("UNIT", "TG", "SR", "RC");
    private static final String ATTEMPT_DOMAIN = "EDITORIAL_L3_ATTEMPT_IDENTITY_V1\n";

    public enum Outcome { COMMITTED, ALREADY_COMMITTED, STOPPED }

    public record Request(EditorialP5PilotRequest context, String reportL1AttemptIdentity,
                          byte[] reportL1Bytes, EditorialL2Execution.Committed l2,
                          Set<Integer> protectedLineNumbers) {
        public Request {
            reportL1Bytes = reportL1Bytes == null ? null : reportL1Bytes.clone();
            protectedLineNumbers = protectedLineNumbers == null ? Set.of() : Set.copyOf(protectedLineNumbers);
        }

        @Override public byte[] reportL1Bytes() { return reportL1Bytes == null ? null : reportL1Bytes.clone(); }

        public String attemptIdentity() {
            EditorialP4Binding binding = context.binding();
            String value = binding.bindingIdentity() + "\n" + binding.runDeclarationIdentity() + "\n"
                    + binding.canonicalPackHash() + "\n" + context.chapterKey() + "\nL3\n"
                    + reportL1AttemptIdentity + "\n" + (l2 == null ? "" : l2.attemptIdentity() + "\n"
                    + l2.viL2Sha256() + "\n" + l2.changeMapSha256()) + "\n" + context.bundleIdentity()
                    + EditorialContractRevision.identitySuffix(context.contractRevision());
            return EditorialCanonicalJson.sha256Hex((ATTEMPT_DOMAIN + value).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** The five release numbers, computed by the app from accountable rows and the actual diff. */
    public record ReleaseNumbers(int unprocessedRawUnits, int unprocessedCandidates,
                                 int provenUnresolvedConflicts, int unaccountedChangedAnchors,
                                 int protectedSpanRegressions, int preserved) {
        public boolean releasable() {
            return unprocessedRawUnits == 0 && unprocessedCandidates == 0 && provenUnresolvedConflicts == 0
                    && unaccountedChangedAnchors == 0 && protectedSpanRegressions == 0;
        }
    }

    public record Result(Outcome outcome, EditorialL2Execution.StopClass stopClass, String reasonCode,
                         List<String> issues, EditorialL2Execution.Committed committed,
                         ReleaseNumbers releaseNumbers, int providerCalls) {
        public Result {
            issues = List.copyOf(issues == null ? List.of() : issues);
        }

        public boolean accepted() {
            return outcome == Outcome.COMMITTED || outcome == Outcome.ALREADY_COMMITTED;
        }
    }

    record Candidate(String candidateId, String ledger, int line, String status) { }

    record Probe(String probeId, String finding, String verdict) { }

    record ReconcileWire(Map<String, String> resolutions, List<EditorialChangeMapReconstructor.ChangeRow> changes,
                         List<EditorialChangeMapReconstructor.PreservedRow> preserved,
                         List<Probe> coverage, List<Probe> regression,
                         EditorialL2Execution.StopClass stopClass, String reasonCode) { }

    /**
     * @param budget caps for each of the two calls; total cost is checked against
     *               {@code maximumCost} across both calls
     */
    public Result execute(Request request, EditorialL2Execution.Budget budget,
                          EditorialL2Execution.Provider provider, EditorialL2Execution.Store store) {
        return execute(request, budget, budget, budget == null ? null : budget.maximumCost(), provider, store);
    }

    /**
     * Separate caps per call (re-audit and reconcile); the summed cost may not exceed the sum of both caps.
     */
    public Result execute(Request request, EditorialL2Execution.Budget reauditBudget,
                          EditorialL2Execution.Budget reconcileBudget,
                          EditorialL2Execution.Provider provider, EditorialL2Execution.Store store) {
        java.math.BigDecimal total = reauditBudget == null || reconcileBudget == null || reauditBudget.maximumCost() == null
                || reconcileBudget.maximumCost() == null ? null
                : reauditBudget.maximumCost().add(reconcileBudget.maximumCost());
        return execute(request, reauditBudget, reconcileBudget, total, provider, store);
    }

    /**
     * Ledger contract: a third call, {@value EditorialFinalRead#L3_PHASE}, reads the exact FINAL bytes the app
     * built before they are committed. The total cap is the sum of the three caps.
     */
    public Result execute(Request request, EditorialL2Execution.Budget reauditBudget,
                          EditorialL2Execution.Budget reconcileBudget, EditorialL2Execution.Budget finalReadBudget,
                          EditorialL2Execution.Provider provider, EditorialL2Execution.Store store) {
        if (finalReadBudget == null) return execute(request, reauditBudget, reconcileBudget, provider, store);
        BigDecimal total = reauditBudget == null || reconcileBudget == null || reauditBudget.maximumCost() == null
                || reconcileBudget.maximumCost() == null || finalReadBudget.maximumCost() == null ? null
                : reauditBudget.maximumCost().add(reconcileBudget.maximumCost()).add(finalReadBudget.maximumCost());
        return execute(request, reauditBudget, reconcileBudget, total, finalReadBudget, provider, store);
    }

    private Result execute(Request request, EditorialL2Execution.Budget reauditBudget,
                           EditorialL2Execution.Budget reconcileBudget, BigDecimal totalCap,
                           EditorialL2Execution.Provider provider, EditorialL2Execution.Store store) {
        return execute(request, reauditBudget, reconcileBudget, totalCap, null, provider, store);
    }

    private Result execute(Request request, EditorialL2Execution.Budget reauditBudget,
                           EditorialL2Execution.Budget reconcileBudget, BigDecimal totalCap,
                           EditorialL2Execution.Budget finalReadBudget,
                           EditorialL2Execution.Provider provider, EditorialL2Execution.Store store) {
        if (request == null || request.context() == null) {
            return stop(EditorialL2Execution.StopClass.INPUT_REQUIRED, "INPUT_L3_REQUEST_MISSING", List.of(), 0);
        }
        if (!validBudget(reauditBudget) || !validBudget(reconcileBudget) || totalCap == null) {
            return stop(EditorialL2Execution.StopClass.AUTHORIZATION_REQUIRED, "L3_BUDGET_REQUIRED", List.of(), 0);
        }
        String issue = predecessorIssue(request);
        if (issue != null) return stop(EditorialL2Execution.StopClass.INPUT_REQUIRED, issue, List.of(), 0);
        final boolean ledger = EditorialContractRevision.isLedger(request.context().contractRevision());
        if (ledger && !validBudget(finalReadBudget)) {
            return stop(EditorialL2Execution.StopClass.AUTHORIZATION_REQUIRED, "L3_FINAL_READ_BUDGET_REQUIRED", List.of(), 0);
        }

        EditorialPhaseContextProjector.PhaseProjection reaudit;
        EditorialPhaseContextProjector.PhaseProjection reconcile;
        try {
            reaudit = project(request, REAUDIT_PHASE);
            reconcile = project(request, RECONCILE_PHASE);
        } catch (RuntimeException invalid) {
            return stop(EditorialL2Execution.StopClass.INPUT_REQUIRED, "INPUT_L3_PROJECTION_INVALID", List.of(), 0);
        }
        Map<String, byte[]> reauditSources = visible(reaudit);
        Map<String, byte[]> reconcileSources = visible(reconcile);
        if (size(reauditSources) > reauditBudget.maximumInputBytes() || size(reconcileSources) > reconcileBudget.maximumInputBytes()) {
            return stop(EditorialL2Execution.StopClass.BUDGET_EXCEEDED, "L3_INPUT_BUDGET_EXCEEDED", List.of(), 0);
        }
        if (provider == null || store == null) {
            return stop(EditorialL2Execution.StopClass.EXECUTION_DISABLED, "L3_PROVIDER_OR_STORE_NOT_CONFIGURED", List.of(), 0);
        }

        String attemptIdentity = request.attemptIdentity();
        EditorialL2Execution.Store.Claim claim;
        try {
            claim = store.claim(attemptIdentity, request.l2().attemptIdentity(), request.context().bundleIdentity());
        } catch (RuntimeException error) {
            return stop(EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L3_CLAIM_FAILED", List.of(), 0);
        }
        switch (claim) {
            case ALREADY_COMMITTED -> {
                Optional<EditorialL2Execution.Committed> existing = store.findCommitted(attemptIdentity);
                return existing.map(value -> new Result(Outcome.ALREADY_COMMITTED, null, "L3_ALREADY_COMMITTED",
                        List.of(), value, null, 0)).orElseGet(() -> stop(EditorialL2Execution.StopClass.RETRY_REQUIRED,
                        "RETRY_L3_COMMITTED_RESULT_UNAVAILABLE", List.of(), 0));
            }
            case IN_FLIGHT -> {
                return stop(EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L3_CALL_STATE_UNKNOWN", List.of(), 0);
            }
            case RECOVERY_REQUIRED -> {
                return stop(EditorialL2Execution.StopClass.RETRY_REQUIRED, "STOP_L3_EXTERNAL_CALL_STATE_UNRESOLVED", List.of(), 0);
            }
            case ACQUIRED -> { }
        }

        if (ledger) {
            return executeLedger(request, reauditBudget, reconcileBudget, finalReadBudget, totalCap, provider, store,
                    attemptIdentity, reauditSources, reconcileSources);
        }
        BigDecimal spent = BigDecimal.ZERO;
        // Call 1: blind RAW-first re-audit. It never sees DRAFT, REPORT_L1 or CHANGE_MAP_L2.
        CallOutcome first = call(provider, store, attemptIdentity, REAUDIT_PHASE, REAUDIT_WIRE,
                reauditSources, request, reauditBudget, totalCap, spent, 0);
        if (first.stop != null) return first.stop;
        spent = first.cost;
        List<Candidate> candidates;
        try {
            candidates = parseReaudit(first.bytes, attemptIdentity, viL2LineCount(request));
        } catch (RuntimeException invalid) {
            recover(store, attemptIdentity, "REPAIR_L3_REAUDIT_SCHEMA_INVALID");
            return stop(EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L3_REAUDIT_SCHEMA_INVALID",
                    List.of(EditorialL2Execution.safeMessage(invalid)), 1);
        }

        // Call 2: reconcile sees the full chain; the app-owned candidate list travels in the envelope.
        Map<String, byte[]> secondSources = new TreeMap<>(reconcileSources);
        CallOutcome second = call(provider, store, attemptIdentity, RECONCILE_PHASE, RECONCILE_WIRE,
                secondSources, request, reconcileBudget, totalCap, spent, 1, candidateEnvelope(candidates));
        if (second.stop != null) return second.stop;
        ReconcileWire wire;
        try {
            wire = parseReconcile(second.bytes, attemptIdentity, candidates);
        } catch (RuntimeException invalid) {
            recover(store, attemptIdentity, "REPAIR_L3_RECONCILE_SCHEMA_INVALID");
            return stop(EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L3_RECONCILE_SCHEMA_INVALID",
                    List.of(EditorialL2Execution.safeMessage(invalid)), 2);
        }
        if (wire.stopClass() != null) {
            recover(store, attemptIdentity, wire.reasonCode());
            return stop(wire.stopClass(), wire.reasonCode(), List.of(), 2);
        }

        EditorialChangeMapReconstructor.Result qa = new EditorialChangeMapReconstructor().reconstruct(
                new EditorialChangeMapReconstructor.Request(EditorialChangeMapReconstructor.Kind.L3_VI_L2_TO_FINAL,
                        request.l2().viL2Bytes(), request.l2().attemptIdentity(), wire.changes(), wire.preserved(),
                        protectedForViL2(request)));
        if (!qa.accepted()) {
            recover(store, attemptIdentity, "REPAIR_L3_QA_CHANGE_MAP_INVALID");
            return stop(EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L3_QA_CHANGE_MAP_INVALID", qa.issues(), 2);
        }
        ReleaseNumbers numbers = releaseNumbers(request, candidates, wire, qa);
        if (numbers.provenUnresolvedConflicts() > 0) {
            recover(store, attemptIdentity, "CONTENT_L3_PROVEN_CONFLICT_UNRESOLVED");
            return new Result(Outcome.STOPPED, EditorialL2Execution.StopClass.CONTENT_BLOCKED,
                    "CONTENT_L3_PROVEN_CONFLICT_UNRESOLVED", List.of(), null, numbers, 2);
        }
        if (!numbers.releasable()) {
            recover(store, attemptIdentity, "REPAIR_L3_RELEASE_NUMBERS_NOT_ZERO");
            return new Result(Outcome.STOPPED, EditorialL2Execution.StopClass.REPAIR_REQUIRED,
                    "REPAIR_L3_RELEASE_NUMBERS_NOT_ZERO", List.of(), null, numbers, 2);
        }

        byte[] finalBytes = qa.outputBytes();
        byte[] receipt = receipt(request, candidates, wire, qa, numbers, attemptIdentity);
        EditorialL2Execution.Committed committed = new EditorialL2Execution.Committed(attemptIdentity,
                request.l2().attemptIdentity(), request.context().bundleIdentity(), finalBytes,
                EditorialCanonicalJson.sha256Hex(finalBytes), receipt, EditorialCanonicalJson.sha256Hex(receipt));
        try {
            store.commit(committed);
        } catch (RuntimeException error) {
            recover(store, attemptIdentity, "RETRY_L3_ATOMIC_COMMIT_FAILED");
            return stop(EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L3_ATOMIC_COMMIT_FAILED", List.of(), 2);
        }
        Optional<EditorialL2Execution.Committed> readback = store.findCommitted(attemptIdentity);
        if (readback.isEmpty() || !readback.get().viL2Sha256().equals(committed.viL2Sha256())
                || !readback.get().changeMapSha256().equals(committed.changeMapSha256())) {
            return stop(EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L3_READBACK_MISMATCH", List.of(), 2);
        }
        return new Result(Outcome.COMMITTED, null, "L3_FINAL_COMMITTED", List.of(), readback.get(), numbers, 2);
    }

    /** The ledger-contract flow: three calls, anchored probes, carried defects, a real read of FINAL. */
    private Result executeLedger(Request request, EditorialL2Execution.Budget reauditBudget,
                                 EditorialL2Execution.Budget reconcileBudget, EditorialL2Execution.Budget finalReadBudget,
                                 BigDecimal totalCap, EditorialL2Execution.Provider provider,
                                 EditorialL2Execution.Store store, String attemptIdentity,
                                 Map<String, byte[]> reauditSources, Map<String, byte[]> reconcileSources) {
        EditorialRawInventory.Inventory inventory;
        EditorialL1Ledger.Body l1;
        List<String> viLines;
        List<EditorialL3Ledger.Carried> carried;
        try {
            inventory = EditorialRawInventory.build(request.context().source(EditorialSafe4Contract.RAW).bytes());
            l1 = EditorialL1Ledger.parseBody(EditorialCanonicalJson.parseObject(request.reportL1Bytes()));
            viLines = EditorialFinalRead.lines(request.l2().viL2Bytes());
            carried = EditorialL3Ledger.carriedFrom(request.l2().changeMapBytes());
        } catch (RuntimeException invalid) {
            recover(store, attemptIdentity, "INPUT_L3_LEDGER_CHAIN_INVALID");
            return stop(EditorialL2Execution.StopClass.INPUT_REQUIRED, "INPUT_L3_LEDGER_CHAIN_INVALID", List.of(), 0);
        }

        BigDecimal spent = BigDecimal.ZERO;
        CallOutcome first = call(provider, store, attemptIdentity, REAUDIT_PHASE, REAUDIT_WIRE_V3, reauditSources, request,
                reauditBudget, totalCap, spent, 0);
        if (first.stop != null) return first.stop;
        spent = first.cost;
        EditorialL3Ledger.ReauditPass pass;
        try {
            pass = EditorialL3Ledger.parseReaudit(first.bytes, attemptIdentity, inventory, viLines.size());
        } catch (RuntimeException invalid) {
            recover(store, attemptIdentity, "REPAIR_L3_REAUDIT_SCHEMA_INVALID");
            return stop(EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L3_REAUDIT_SCHEMA_INVALID",
                    List.of(EditorialL1Ledger.safeMessage(invalid)), 1);
        }

        Map<String, byte[]> secondSources = new TreeMap<>(reconcileSources);
        secondSources.put(EditorialL3Ledger.CARRIED_ROLE, EditorialL3Ledger.carriedBlock(carried));
        CallOutcome second = call(provider, store, attemptIdentity, RECONCILE_PHASE, RECONCILE_WIRE_V3, secondSources, request,
                reconcileBudget, totalCap, spent, 1, EditorialL3Ledger.candidateBlock(pass.candidates()));
        if (second.stop != null) return second.stop;
        spent = second.cost;
        EditorialL3Ledger.ReconcileWire wire;
        try {
            wire = EditorialL3Ledger.parseReconcile(second.bytes, attemptIdentity, pass.candidates(), carried.size(), inventory,
                    request.l2().viL2Bytes());
        } catch (RuntimeException invalid) {
            recover(store, attemptIdentity, "REPAIR_L3_RECONCILE_SCHEMA_INVALID");
            return stop(EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L3_RECONCILE_SCHEMA_INVALID",
                    List.of(EditorialL1Ledger.safeMessage(invalid)), 2);
        }
        if (wire.rows().stopClass() != null) {
            recover(store, attemptIdentity, wire.rows().reasonCode());
            return stop(wire.rows().stopClass(), wire.rows().reasonCode(), List.of(), 2);
        }

        EditorialChangeMapReconstructor.Result qa = new EditorialChangeMapReconstructor().reconstruct(
                new EditorialChangeMapReconstructor.Request(EditorialChangeMapReconstructor.Kind.L3_VI_L2_TO_FINAL,
                        request.l2().viL2Bytes(), request.l2().attemptIdentity(), wire.rows().changes(),
                        wire.rows().preserved(), protectedForViL2(request)));
        if (!qa.accepted()) {
            recover(store, attemptIdentity, "REPAIR_L3_QA_CHANGE_MAP_INVALID");
            return stop(EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L3_QA_CHANGE_MAP_INVALID", qa.issues(), 2);
        }

        EditorialL3Ledger.CarriedVerdict carriedVerdict = EditorialL3Ledger.verifyCarried(carried, wire.carried(), viLines,
                wire.rows().changes(), wire.rows().preserved(), qa);
        if (!carriedVerdict.issues().isEmpty()) {
            recover(store, attemptIdentity, "REPAIR_L3_CARRIED_RESOLUTION_INVALID");
            return stop(EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L3_CARRIED_RESOLUTION_INVALID",
                    carriedVerdict.issues(), 2);
        }
        if (!carriedVerdict.unresolved().isEmpty()) {
            recover(store, attemptIdentity, "CONTENT_L3_CARRIED_DEFECT_UNRESOLVED");
            List<String> open = new ArrayList<>();
            for (String index : carriedVerdict.unresolved()) open.add("L3_UNRESOLVED_CARRIED_DEFECT:" + index);
            return stop(EditorialL2Execution.StopClass.CONTENT_BLOCKED, "CONTENT_L3_CARRIED_DEFECT_UNRESOLVED", open, 2);
        }
        int[] probeConflicts = new int[1];
        List<String> probeIssues = EditorialL3Ledger.verifyProbes(wire.probes(), inventory, viLines, wire.rows().changes(),
                wire.rows().preserved(), qa, probeConflicts);
        if (!probeIssues.isEmpty()) {
            recover(store, attemptIdentity, "REPAIR_L3_PROBES_INVALID");
            return stop(EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L3_PROBES_INVALID", probeIssues, 2);
        }

        int unprocessed = 0;
        int conflicts = probeConflicts[0];
        int preserved = qa.preserved().size();
        for (EditorialL3Ledger.Candidate candidate : pass.candidates()) {
            // Reconcile may resolve a re-audit candidate; an unresolved one keeps its re-audit status.
            String status = wire.resolutions().getOrDefault(candidate.candidateId(), candidate.status());
            switch (status) {
                case "UNPROCESSED" -> unprocessed++;
                case "CONFLICT" -> conflicts++;
                case "PRESERVED" -> preserved++;
                default -> { }
            }
        }
        // coverage over the inventory was verified closed, so no RAW unit is unprocessed by construction
        int[] accounted = ledgerAccounting(request, qa);
        ReleaseNumbers numbers = new ReleaseNumbers(0, unprocessed, conflicts, accounted[0], accounted[1], preserved);
        if (numbers.provenUnresolvedConflicts() > 0) {
            recover(store, attemptIdentity, "CONTENT_L3_PROVEN_CONFLICT_UNRESOLVED");
            return new Result(Outcome.STOPPED, EditorialL2Execution.StopClass.CONTENT_BLOCKED,
                    "CONTENT_L3_PROVEN_CONFLICT_UNRESOLVED", List.of(), null, numbers, 2);
        }
        if (!numbers.releasable()) {
            recover(store, attemptIdentity, "REPAIR_L3_RELEASE_NUMBERS_NOT_ZERO");
            return new Result(Outcome.STOPPED, EditorialL2Execution.StopClass.REPAIR_REQUIRED,
                    "REPAIR_L3_RELEASE_NUMBERS_NOT_ZERO", List.of(), null, numbers, 2);
        }

        // Call 3: the model reads the exact FINAL bytes; the app checks the echoed hash and probe tails.
        byte[] finalBytes = qa.outputBytes();
        Map<String, byte[]> readSources = new TreeMap<>();
        readSources.put(EditorialSafe4Contract.RAW, request.context().source(EditorialSafe4Contract.RAW).bytes());
        if (request.context().source(EditorialSafe4Contract.GLOSSARY) != null
                && request.context().source(EditorialSafe4Contract.GLOSSARY).bytes() != null) {
            readSources.put(EditorialSafe4Contract.GLOSSARY, request.context().source(EditorialSafe4Contract.GLOSSARY).bytes());
        }
        readSources.put(EditorialFinalRead.TARGET_ROLE, finalBytes);
        readSources.put(EditorialFinalRead.PROBES_ROLE, EditorialFinalRead.probeBlock(finalBytes));
        if (size(readSources) > finalReadBudget.maximumInputBytes()) {
            recover(store, attemptIdentity, "L3_INPUT_BUDGET_EXCEEDED");
            return new Result(Outcome.STOPPED, EditorialL2Execution.StopClass.BUDGET_EXCEEDED, "L3_INPUT_BUDGET_EXCEEDED",
                    List.of(EditorialFinalRead.L3_PHASE), null, numbers, 2);
        }
        CallOutcome third = call(provider, store, attemptIdentity, EditorialFinalRead.L3_PHASE, EditorialFinalRead.WIRE,
                readSources, request, finalReadBudget, totalCap, spent, 2);
        if (third.stop != null) return third.stop;
        EditorialFinalRead.Result read;
        try {
            read = EditorialFinalRead.parse(third.bytes, attemptIdentity, finalBytes, EditorialFinalRead.L3_PHASE);
        } catch (RuntimeException invalid) {
            recover(store, attemptIdentity, "REPAIR_L3_FINAL_READ_INVALID");
            return stop(EditorialL2Execution.StopClass.REPAIR_REQUIRED, "REPAIR_L3_FINAL_READ_INVALID",
                    List.of(EditorialL1Ledger.safeMessage(invalid)), 3);
        }
        if (!read.defects().isEmpty()) {
            // No fix-after-read round is spent in this contract revision: a FINAL that still reads wrong is not released.
            recover(store, attemptIdentity, "CONTENT_L3_FINAL_READ_DEFECTS");
            List<String> open = new ArrayList<>();
            for (EditorialFinalRead.Defect d : read.defects()) open.add("L3_FINAL_READ_DEFECT:" + d.line() + ":" + d.type());
            return new Result(Outcome.STOPPED, EditorialL2Execution.StopClass.CONTENT_BLOCKED,
                    "CONTENT_L3_FINAL_READ_DEFECTS", open, null, numbers, 3);
        }

        byte[] receipt = receiptV2(request, l1, inventory, pass, carried, carriedVerdict, wire, qa, numbers, read, attemptIdentity);
        EditorialL2Execution.Committed committed = new EditorialL2Execution.Committed(attemptIdentity,
                request.l2().attemptIdentity(), request.context().bundleIdentity(), finalBytes,
                EditorialCanonicalJson.sha256Hex(finalBytes), receipt, EditorialCanonicalJson.sha256Hex(receipt));
        try {
            store.commit(committed);
        } catch (RuntimeException error) {
            recover(store, attemptIdentity, "RETRY_L3_ATOMIC_COMMIT_FAILED");
            return stop(EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L3_ATOMIC_COMMIT_FAILED", List.of(), 3);
        }
        Optional<EditorialL2Execution.Committed> readback = store.findCommitted(attemptIdentity);
        if (readback.isEmpty() || !readback.get().viL2Sha256().equals(committed.viL2Sha256())
                || !readback.get().changeMapSha256().equals(committed.changeMapSha256())) {
            return stop(EditorialL2Execution.StopClass.RETRY_REQUIRED, "RETRY_L3_READBACK_MISMATCH", List.of(), 3);
        }
        return new Result(Outcome.COMMITTED, null, "L3_FINAL_COMMITTED", List.of(), readback.get(), numbers, 3);
    }

    /** QA receipt of the ledger contract. Every claim in it is backed by an operation recorded in the same file. */
    private static byte[] receiptV2(Request request, EditorialL1Ledger.Body l1, EditorialRawInventory.Inventory inventory,
                                    EditorialL3Ledger.ReauditPass pass, List<EditorialL3Ledger.Carried> carried,
                                    EditorialL3Ledger.CarriedVerdict carriedVerdict, EditorialL3Ledger.ReconcileWire wire,
                                    EditorialChangeMapReconstructor.Result qa, ReleaseNumbers numbers,
                                    EditorialFinalRead.Result read, String attemptIdentity) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", QA_RECEIPT_SCHEMA_V2);
        root.put("artifactType", "QA_RECEIPT");
        root.put("contractRevision", EditorialContractRevision.CURRENT_LEDGER);
        root.put("attemptIdentity", attemptIdentity);
        root.put("bindingIdentity", request.context().binding().bindingIdentity());
        root.put("chapterKey", request.context().chapterKey());
        root.put("bundleIdentity", request.context().bundleIdentity());
        root.put("reportL1AttemptIdentity", request.reportL1AttemptIdentity());
        root.put("reportL1Sha256", EditorialCanonicalJson.sha256Hex(request.reportL1Bytes()));
        root.put("viL2AttemptIdentity", request.l2().attemptIdentity());
        root.put("viL2Sha256", request.l2().viL2Sha256());
        root.put("changeMapL2Sha256", request.l2().changeMapSha256());
        root.put("finalSha256", qa.outputSha256());
        root.put("finalByteCount", BigDecimal.valueOf(qa.outputBytes().length));
        root.put("finalLineCount", BigDecimal.valueOf(read.lineCount()));
        root.put("qaChangeMap", EditorialCanonicalJson.parse(qa.changeMapBytes()));
        if (!wire.rows().warnings().isEmpty()) root.put("wireWarnings", new ArrayList<>(wire.rows().warnings()));
        root.put("reauditCoverage", EditorialL3Ledger.coverageEvidence(inventory, pass));
        Map<String, Object> ledgers = new TreeMap<>();
        for (EditorialL3Ledger.Candidate candidate : pass.candidates()) {
            String status = wire.resolutions().getOrDefault(candidate.candidateId(), candidate.status());
            @SuppressWarnings("unchecked") Map<String, Object> counts =
                    (Map<String, Object>) ledgers.computeIfAbsent(candidate.ledger(), k -> new TreeMap<String, Object>());
            counts.merge(status, BigDecimal.ONE, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
        }
        root.put("ledgers", ledgers);
        root.put("probes", EditorialL3Ledger.probeRows(wire.probes()));
        root.put("carriedDefects", EditorialL3Ledger.carriedEvidence(carried, carriedVerdict, wire.carried()));
        Set<String> flagged = new TreeSet<>();
        for (EditorialL1Ledger.Candidate c : l1.candidates()) flagged.add(c.unitId());
        for (EditorialL1Ledger.Finding f : l1.findings()) {
            flagged.addAll(f.rawUnits());
            flagged.addAll(f.occurrenceUnits());
        }
        for (EditorialL3Ledger.Candidate c : pass.candidates()) flagged.add(c.unitId());
        Map<String, Object> reconciliation = new LinkedHashMap<>();
        reconciliation.put("l1Candidates", BigDecimal.valueOf(l1.candidates().size()));
        reconciliation.put("l1Findings", BigDecimal.valueOf(l1.findings().size()));
        reconciliation.put("l3Candidates", BigDecimal.valueOf(pass.candidates().size()));
        reconciliation.put("distinctUnitsFlagged", BigDecimal.valueOf(flagged.size()));
        reconciliation.put("inventoryUnits", BigDecimal.valueOf(inventory.units().size()));
        root.put("reconciliation", reconciliation);
        Map<String, Object> release = new LinkedHashMap<>();
        release.put("unprocessedRawUnits", BigDecimal.valueOf(numbers.unprocessedRawUnits()));
        release.put("unprocessedCandidates", BigDecimal.valueOf(numbers.unprocessedCandidates()));
        release.put("provenUnresolvedConflicts", BigDecimal.valueOf(numbers.provenUnresolvedConflicts()));
        release.put("unaccountedChangedAnchors", BigDecimal.valueOf(numbers.unaccountedChangedAnchors()));
        release.put("protectedSpanRegressions", BigDecimal.valueOf(numbers.protectedSpanRegressions()));
        release.put("preserved", BigDecimal.valueOf(numbers.preserved()));
        root.put("releaseNumbers", release);
        root.put("finalRead", EditorialFinalRead.evidence(EditorialFinalRead.L3_PHASE, read));
        // one entry per operation that really ran, in order; nothing is claimed without one
        List<Object> operations = new ArrayList<>();
        operations.add(operation(1, REAUDIT_PHASE, null));
        operations.add(operation(2, RECONCILE_PHASE, null));
        operations.add(operation(3, EditorialFinalRead.L3_PHASE, read.targetSha256()));
        root.put("operations", operations);
        root.put("pairDeltaQa", "NONE");
        root.put("stopReceipt", "NONE");
        return EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, Object> operation(int order, String phase, String targetSha256) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("order", BigDecimal.valueOf(order));
        row.put("phase", phase);
        if (targetSha256 != null) row.put("targetSha256", targetSha256);
        return row;
    }

    /** L2 must be the committed result of this chain: same REPORT_L1, bundle and intact bytes. */
    static String predecessorIssue(Request request) {
        String l1 = EditorialL2Execution.predecessorIssue(new EditorialL2Execution.Request(request.context(),
                request.reportL1AttemptIdentity(), request.reportL1Bytes(), request.protectedLineNumbers()));
        if (l1 != null) return l1;
        EditorialL2Execution.Committed l2 = request.l2();
        if (l2 == null) return "INPUT_VI_L2_REQUIRED";
        if (!EditorialCanonicalJson.sha256Hex(l2.viL2Bytes()).equals(l2.viL2Sha256())
                || !EditorialCanonicalJson.sha256Hex(l2.changeMapBytes()).equals(l2.changeMapSha256())) {
            return "INPUT_VI_L2_INTEGRITY_INVALID";
        }
        if (!request.reportL1AttemptIdentity().equals(l2.predecessorIdentity())
                || !request.context().bundleIdentity().equals(l2.bundleIdentity())) {
            return "INPUT_VI_L2_CHAIN_MISMATCH";
        }
        Map<String, Object> map;
        try {
            map = EditorialCanonicalJson.parseObject(l2.changeMapBytes());
        } catch (RuntimeException invalid) {
            return "INPUT_CHANGE_MAP_L2_INVALID";
        }
        String draftSha = EditorialCanonicalJson.sha256Hex(request.context().source(EditorialSafe4Contract.DRAFT).bytes());
        if (!"CHANGE_MAP_L2".equals(map.get("artifactType")) || !draftSha.equals(map.get("baseSha256"))
                || !l2.viL2Sha256().equals(map.get("outputSha256"))
                || !request.reportL1AttemptIdentity().equals(map.get("predecessorIdentity"))) {
            return "INPUT_CHANGE_MAP_L2_MISMATCH";
        }
        return null;
    }

    private static EditorialPhaseContextProjector.PhaseProjection project(Request request, String phase) {
        EditorialPhaseContextProjector.Bundle l1 = request.context().bundleForExecution();
        List<EditorialPhaseContextProjector.BundleAsset> assets = new ArrayList<>(l1.assets());
        assets.add(new EditorialPhaseContextProjector.BundleAsset("REPORT_L1",
                "report-l1:" + request.reportL1AttemptIdentity(), request.reportL1Bytes(), true,
                EditorialContractRevision.isLedger(request.context().contractRevision())
                        ? EditorialContractRevision.REPORT_SCHEMA_V2 : EditorialP5RawWireContract.FINAL_REPORT_SCHEMA));
        assets.add(new EditorialPhaseContextProjector.BundleAsset("VI_L2",
                "vi-l2:" + request.l2().attemptIdentity(), request.l2().viL2Bytes(), true,
                EditorialL2Execution.VI_L2_SCHEMA));
        assets.add(new EditorialPhaseContextProjector.BundleAsset("CHANGE_MAP_L2",
                "change-map-l2:" + request.l2().attemptIdentity(), request.l2().changeMapBytes(), true,
                EditorialChangeMapReconstructor.Kind.L2_DRAFT_TO_VI_L2.schemaVersion()));
        return new EditorialPhaseContextProjector().project(
                new EditorialPhaseContextProjector.Bundle(assets, l1.pronounStatus()), phase);
    }

    private record CallOutcome(byte[] bytes, BigDecimal cost, Result stop) { }

    private static CallOutcome call(EditorialL2Execution.Provider provider, EditorialL2Execution.Store store,
                                    String attemptIdentity, String phase, String schema,
                                    Map<String, byte[]> sources, Request request,
                                    EditorialL2Execution.Budget budget, BigDecimal totalCap, BigDecimal spent,
                                    int priorCalls) {
        return call(provider, store, attemptIdentity, phase, schema, sources, request, budget, totalCap, spent,
                priorCalls, null);
    }

    private static CallOutcome call(EditorialL2Execution.Provider provider, EditorialL2Execution.Store store,
                                    String attemptIdentity, String phase, String schema,
                                    Map<String, byte[]> sources, Request request,
                                    EditorialL2Execution.Budget budget, BigDecimal totalCap, BigDecimal spent,
                                    int priorCalls, byte[] candidateEnvelope) {
        int calls = priorCalls + 1;
        Map<String, byte[]> visible = new TreeMap<>(sources);
        if (candidateEnvelope != null) visible.put("L3_REAUDIT_CANDIDATES", candidateEnvelope);
        EditorialL2Execution.Provider.Response response;
        try {
            response = provider.call(new EditorialL2Execution.Provider.Request(attemptIdentity, phase, schema,
                    Collections.unmodifiableMap(visible), request.context().authority(), request.context().chapterKey(),
                    budget.maximumOutputTokens(), budget.maximumExecutionTimeMillis()));
        } catch (Exception error) {
            recover(store, attemptIdentity, "RETRY_L3_PROVIDER_CALL_FAILED");
            return new CallOutcome(null, spent, stop(EditorialL2Execution.StopClass.RETRY_REQUIRED,
                    "RETRY_L3_PROVIDER_CALL_FAILED", List.of(phase), calls));
        }
        if (response == null || response.responseBytes() == null) {
            recover(store, attemptIdentity, "RETRY_L3_PROVIDER_EMPTY_RESPONSE");
            return new CallOutcome(null, spent, stop(EditorialL2Execution.StopClass.RETRY_REQUIRED,
                    "RETRY_L3_PROVIDER_EMPTY_RESPONSE", List.of(phase), calls));
        }
        String finish = response.finishReason() == null ? "" : response.finishReason().toLowerCase(java.util.Locale.ROOT);
        if (!response.transportComplete() || "length".equals(finish) || "max_tokens".equals(finish)
                || "max_output_tokens".equals(finish)) {
            recover(store, attemptIdentity, "RETRY_L3_OUTPUT_TRUNCATED");
            return new CallOutcome(null, spent, stop(EditorialL2Execution.StopClass.RETRY_REQUIRED,
                    "RETRY_L3_OUTPUT_TRUNCATED", List.of(phase), calls));
        }
        if (!response.costKnown()) {
            recover(store, attemptIdentity, "RETRY_L3_PROVIDER_COST_UNAVAILABLE");
            return new CallOutcome(null, spent, stop(EditorialL2Execution.StopClass.RETRY_REQUIRED,
                    "RETRY_L3_PROVIDER_COST_UNAVAILABLE", List.of(phase), calls));
        }
        BigDecimal total = spent.add(response.cost());
        if (response.outputTokens() > budget.maximumOutputTokens()
                || response.cost().compareTo(budget.maximumCost()) > 0 || total.compareTo(totalCap) > 0) {
            recover(store, attemptIdentity, "L3_TOKEN_OR_COST_BUDGET_EXCEEDED");
            return new CallOutcome(null, total, stop(EditorialL2Execution.StopClass.BUDGET_EXCEEDED,
                    "L3_TOKEN_OR_COST_BUDGET_EXCEEDED", List.of(phase), calls));
        }
        return new CallOutcome(response.responseBytes(), total, null);
    }

    static List<Candidate> parseReaudit(byte[] bytes, String attemptIdentity, int viL2Lines) {
        if (bytes.length > EditorialL2Execution.MAX_WIRE_BYTES) throw new IllegalArgumentException("L3_WIRE_BYTE_LIMIT_EXCEEDED");
        Map<String, Object> root = EditorialCanonicalJson.parseObject(bytes);
        EditorialL2Execution.keys(root, Set.of("wireSchemaVersion", "attemptIdentity", "candidates"), "root");
        if (!REAUDIT_WIRE.equals(root.get("wireSchemaVersion"))) throw new IllegalArgumentException("L3_WIRE_SCHEMA_INVALID");
        if (!attemptIdentity.equals(root.get("attemptIdentity"))) throw new IllegalArgumentException("L3_WIRE_ATTEMPT_ECHO_MISMATCH");
        List<Object> values = EditorialCanonicalJson.array(root.get("candidates"), "candidates");
        if (values.size() > MAX_CANDIDATES) throw new IllegalArgumentException("L3_WIRE_ROW_LIMIT_EXCEEDED");
        List<Candidate> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        boolean unit = false;
        for (Object value : values) {
            Map<String, Object> row = EditorialL2Execution.object(value, "candidate");
            EditorialL2Execution.keys(row, Set.of("candidateId", "ledger", "line", "status"), "candidate");
            String id = EditorialL2Execution.text(row, "candidateId");
            if (!EditorialP5RawWireContract.token(id, EditorialP5RawWireContract.MAX_ID_LENGTH) || !ids.add(id)) {
                throw new IllegalArgumentException("L3_WIRE_CANDIDATE_ID_INVALID");
            }
            String ledger = EditorialL2Execution.text(row, "ledger");
            if (!LEDGERS.contains(ledger)) throw new IllegalArgumentException("L3_WIRE_LEDGER_INVALID");
            int line = EditorialL2Execution.line(row);
            if (line < 0 || line > viL2Lines) throw new IllegalArgumentException("L3_WIRE_LINE_OUT_OF_RANGE");
            String status = EditorialL2Execution.text(row, "status");
            if (!Set.of("PROCESSED", "PRESERVED", "UNPROCESSED", "CONFLICT").contains(status)) {
                throw new IllegalArgumentException("L3_WIRE_STATUS_INVALID");
            }
            unit |= "UNIT".equals(ledger);
            result.add(new Candidate(id, ledger, line, status));
        }
        // An exhaustive re-audit always enumerates raw units; an empty unit ledger is not evidence.
        if (!unit) throw new IllegalArgumentException("L3_WIRE_RAW_UNITS_MISSING");
        return List.copyOf(result);
    }

    static ReconcileWire parseReconcile(byte[] bytes, String attemptIdentity, List<Candidate> candidates) {
        if (bytes.length > EditorialL2Execution.MAX_WIRE_BYTES) throw new IllegalArgumentException("L3_WIRE_BYTE_LIMIT_EXCEEDED");
        Map<String, Object> root = EditorialCanonicalJson.parseObject(bytes);
        EditorialL2Execution.keys(root, Set.of("wireSchemaVersion", "attemptIdentity", "resolutions", "changes",
                "preserved", "adversarialCoverage", "adversarialRegression", "disposition"), "root");
        if (!RECONCILE_WIRE.equals(root.get("wireSchemaVersion"))) throw new IllegalArgumentException("L3_WIRE_SCHEMA_INVALID");
        if (!attemptIdentity.equals(root.get("attemptIdentity"))) throw new IllegalArgumentException("L3_WIRE_ATTEMPT_ECHO_MISMATCH");

        // Reuse the strict L2 row grammar for changes, preserved rows and disposition.
        Map<String, Object> l2Shape = new LinkedHashMap<>();
        l2Shape.put("wireSchemaVersion", EditorialL2Execution.WIRE_SCHEMA_VERSION);
        l2Shape.put("attemptIdentity", attemptIdentity);
        l2Shape.put("changes", root.get("changes"));
        l2Shape.put("preserved", root.get("preserved"));
        l2Shape.put("disposition", root.get("disposition"));
        EditorialL2Execution.Wire rows = EditorialL2Execution.parseWire(
                EditorialCanonicalJson.canonicalize(l2Shape).getBytes(StandardCharsets.UTF_8), attemptIdentity);

        Map<String, String> resolutions = new TreeMap<>();
        Set<String> known = new HashSet<>();
        for (Candidate candidate : candidates) known.add(candidate.candidateId());
        for (Object value : EditorialCanonicalJson.array(root.get("resolutions"), "resolutions")) {
            Map<String, Object> row = EditorialL2Execution.object(value, "resolution");
            EditorialL2Execution.keys(row, Set.of("candidateId", "status"), "resolution");
            String id = EditorialL2Execution.text(row, "candidateId");
            String status = EditorialL2Execution.text(row, "status");
            if (!known.contains(id) || resolutions.containsKey(id)) throw new IllegalArgumentException("L3_WIRE_RESOLUTION_ID_INVALID");
            if (!Set.of("PROCESSED", "PRESERVED", "UNPROCESSED", "CONFLICT").contains(status)) {
                throw new IllegalArgumentException("L3_WIRE_STATUS_INVALID");
            }
            resolutions.put(id, status);
        }
        List<Probe> coverage = probes(root.get("adversarialCoverage"), "adversarialCoverage");
        List<Probe> regression = probes(root.get("adversarialRegression"), "adversarialRegression");
        return new ReconcileWire(resolutions, rows.changes(), rows.preserved(), coverage, regression,
                rows.stopClass(), rows.reasonCode());
    }

    private static List<Probe> probes(Object value, String path) {
        List<Object> values = EditorialCanonicalJson.array(value, path);
        // Each adversarial pass must record what it tried; an empty record is not a pass.
        if (values.isEmpty() || values.size() > MAX_PROBES) throw new IllegalArgumentException("L3_WIRE_PROBES_INVALID:" + path);
        List<Probe> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Object item : values) {
            Map<String, Object> row = EditorialL2Execution.object(item, path);
            EditorialL2Execution.keys(row, Set.of("probeId", "finding", "verdict"), path);
            String id = EditorialL2Execution.text(row, "probeId");
            String verdict = EditorialL2Execution.text(row, "verdict");
            if (!EditorialP5RawWireContract.token(id, EditorialP5RawWireContract.MAX_ID_LENGTH) || !ids.add(id)
                    || !Set.of("NO_DEFECT", "FIXED", "PRESERVED", "CONFLICT").contains(verdict)) {
                throw new IllegalArgumentException("L3_WIRE_PROBE_INVALID:" + path);
            }
            result.add(new Probe(id, EditorialL2Execution.text(row, "finding"), verdict));
        }
        return List.copyOf(result);
    }

    static ReleaseNumbers releaseNumbers(Request request, List<Candidate> candidates, ReconcileWire wire,
                                         EditorialChangeMapReconstructor.Result qa) {
        int unprocessedUnits = 0;
        int unprocessed = 0;
        int conflicts = 0;
        int preserved = 0;
        for (Candidate candidate : candidates) {
            // Reconcile may resolve a re-audit candidate; an unresolved one keeps its re-audit status.
            String status = wire.resolutions().getOrDefault(candidate.candidateId(), candidate.status());
            switch (status) {
                case "UNPROCESSED" -> {
                    if ("UNIT".equals(candidate.ledger())) unprocessedUnits++; else unprocessed++;
                }
                case "CONFLICT" -> conflicts++;
                case "PRESERVED" -> preserved++;
                default -> { }
            }
        }
        for (Probe probe : wire.coverage()) if ("CONFLICT".equals(probe.verdict())) conflicts++;
        for (Probe probe : wire.regression()) if ("CONFLICT".equals(probe.verdict())) conflicts++;
        preserved += qa.preserved().size();

        if (EditorialContractRevision.isLedger(request.context().contractRevision())) {
            int[] accounted = ledgerAccounting(request, qa);
            return new ReleaseNumbers(unprocessedUnits, unprocessed, conflicts, accounted[0], accounted[1], preserved);
        }
        // DRAFT -> FINAL: every actual changed line must be owned by an applied L2 or L3 change.
        Set<Integer> owned = new TreeSet<>(appliedLines(request.l2().changeMapBytes()));
        for (EditorialChangeMapReconstructor.AppliedChange change : qa.applied()) owned.add(change.row().lineNumber());
        String draft = new String(request.context().source(EditorialSafe4Contract.DRAFT).bytes(), StandardCharsets.UTF_8);
        String finalText = new String(qa.outputBytes(), StandardCharsets.UTF_8);
        int unaccounted = 0;
        int protectedRegressions = 0;
        for (EditorialDiffValidator.ChangedSpan span : new EditorialDiffValidator().compute(draft, finalText)) {
            if (!owned.contains(span.lineNumber())) unaccounted++;
            if (request.protectedLineNumbers().contains(span.lineNumber())) protectedRegressions++;
        }
        return new ReleaseNumbers(unprocessedUnits, unprocessed, conflicts, unaccounted, protectedRegressions, preserved);
    }

    /**
     * Protected DRAFT lines of a ledger chain: the caller's set plus the spans persisted in REPORT_L1 v2. For
     * the legacy contract only the caller's set exists.
     */
    static Set<Integer> protectedDraftLines(Request request) {
        Set<Integer> lines = new TreeSet<>(request.protectedLineNumbers());
        if (EditorialContractRevision.isLedger(request.context().contractRevision())) {
            try {
                lines.addAll(EditorialL1Ledger.protectedLines(EditorialL1Ledger.parseBody(
                        EditorialCanonicalJson.parseObject(request.reportL1Bytes())).protectedSpans()));
            } catch (RuntimeException invalid) {
                // L2 refused such a report before it could commit; nothing more to add here
            }
        }
        return lines;
    }

    /** Protected lines in VI_L2 numbering: DRAFT numbers moved through the L2 line map; removed lines drop out. */
    private static Set<Integer> protectedForViL2(Request request) {
        if (!EditorialContractRevision.isLedger(request.context().contractRevision())) return request.protectedLineNumbers();
        List<Integer> map = EditorialChangeMapReconstructor.lineMapFromChangeMap(
                EditorialFinalRead.lines(request.context().source(EditorialSafe4Contract.DRAFT).bytes()).size(),
                request.l2().changeMapBytes());
        Set<Integer> result = new TreeSet<>();
        for (Integer line : protectedDraftLines(request)) {
            if (line >= 1 && line <= map.size() && map.get(line - 1) > 0) result.add(map.get(line - 1));
        }
        return result;
    }

    /**
     * Ledger chains account for every line by replaying the declared operations of each stage on line hashes
     * (a stage that does not reproduce its bytes counts as one unaccounted change) and check that each protected
     * DRAFT line reaches FINAL with identical content.
     *
     * @return {unaccountedChangedAnchors, protectedSpanRegressions}
     */
    private static int[] ledgerAccounting(Request request, EditorialChangeMapReconstructor.Result qa) {
        byte[] draft = request.context().source(EditorialSafe4Contract.DRAFT).bytes();
        byte[] vi = request.l2().viL2Bytes();
        byte[] fin = qa.outputBytes();
        int unaccounted = 0;
        if (!EditorialChangeMapReconstructor.replayMatches(draft, vi, request.l2().changeMapBytes())) unaccounted++;
        if (!EditorialChangeMapReconstructor.replayMatches(vi, fin, qa.changeMapBytes())) unaccounted++;
        List<String> draftHashes = EditorialChangeMapReconstructor.lineHashes(draft);
        List<String> finalHashes = EditorialChangeMapReconstructor.lineHashes(fin);
        List<Integer> toVi = EditorialChangeMapReconstructor.lineMapFromChangeMap(draftHashes.size(), request.l2().changeMapBytes());
        List<Integer> toFinal = EditorialChangeMapReconstructor.lineMapFromChangeMap(
                EditorialChangeMapReconstructor.lineHashes(vi).size(), qa.changeMapBytes());
        int regressions = 0;
        for (Integer line : protectedDraftLines(request)) {
            if (line < 1 || line > draftHashes.size()) continue;
            int viLine = toVi.get(line - 1);
            int finalLine = viLine == 0 || viLine > toFinal.size() ? 0 : toFinal.get(viLine - 1);
            if (finalLine == 0 || finalLine > finalHashes.size()
                    || !finalHashes.get(finalLine - 1).equals(draftHashes.get(line - 1))) {
                regressions++;
            }
        }
        return new int[] {unaccounted, regressions};
    }

    private static List<Integer> appliedLines(byte[] changeMap) {
        List<Integer> lines = new ArrayList<>();
        Map<String, Object> map = EditorialCanonicalJson.parseObject(changeMap);
        for (Object value : EditorialCanonicalJson.array(map.get("changes"), "changes")) {
            Map<String, Object> row = EditorialL2Execution.object(value, "change");
            if ("CLOSED".equals(row.get("status"))) lines.add(EditorialL2Execution.line(Map.of("line", row.get("lineNumber"))));
        }
        return lines;
    }

    private static byte[] candidateEnvelope(List<Candidate> candidates) {
        List<Object> rows = new ArrayList<>();
        for (Candidate candidate : candidates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("candidateId", candidate.candidateId());
            row.put("ledger", candidate.ledger());
            row.put("line", BigDecimal.valueOf(candidate.line()));
            row.put("status", candidate.status());
            rows.add(row);
        }
        return EditorialCanonicalJson.canonicalize(Map.of("candidates", rows)).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] receipt(Request request, List<Candidate> candidates, ReconcileWire wire,
                                  EditorialChangeMapReconstructor.Result qa, ReleaseNumbers numbers,
                                  String attemptIdentity) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", QA_RECEIPT_SCHEMA);
        root.put("artifactType", "QA_RECEIPT");
        root.put("attemptIdentity", attemptIdentity);
        root.put("bindingIdentity", request.context().binding().bindingIdentity());
        root.put("chapterKey", request.context().chapterKey());
        root.put("bundleIdentity", request.context().bundleIdentity());
        root.put("reportL1AttemptIdentity", request.reportL1AttemptIdentity());
        root.put("reportL1Sha256", EditorialCanonicalJson.sha256Hex(request.reportL1Bytes()));
        root.put("viL2AttemptIdentity", request.l2().attemptIdentity());
        root.put("viL2Sha256", request.l2().viL2Sha256());
        root.put("changeMapL2Sha256", request.l2().changeMapSha256());
        root.put("finalSha256", qa.outputSha256());
        root.put("finalByteCount", BigDecimal.valueOf(qa.outputBytes().length));
        root.put("finalLineCount", BigDecimal.valueOf(new String(qa.outputBytes(), StandardCharsets.UTF_8)
                .split("\\r?\\n", -1).length));
        root.put("qaChangeMap", EditorialCanonicalJson.parse(qa.changeMapBytes()));
        Map<String, Object> ledgers = new TreeMap<>();
        for (String ledger : LEDGERS) {
            Map<String, Object> counts = new TreeMap<>();
            for (Candidate candidate : candidates) {
                if (!ledger.equals(candidate.ledger())) continue;
                String status = wire.resolutions().getOrDefault(candidate.candidateId(), candidate.status());
                counts.merge(status, BigDecimal.ONE, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
            }
            ledgers.put(ledger, counts);
        }
        root.put("ledgers", ledgers);
        root.put("adversarialCoverage", probeRows(wire.coverage()));
        root.put("adversarialRegression", probeRows(wire.regression()));
        Map<String, Object> release = new LinkedHashMap<>();
        release.put("unprocessedRawUnits", BigDecimal.valueOf(numbers.unprocessedRawUnits()));
        release.put("unprocessedCandidates", BigDecimal.valueOf(numbers.unprocessedCandidates()));
        release.put("provenUnresolvedConflicts", BigDecimal.valueOf(numbers.provenUnresolvedConflicts()));
        release.put("unaccountedChangedAnchors", BigDecimal.valueOf(numbers.unaccountedChangedAnchors()));
        release.put("protectedSpanRegressions", BigDecimal.valueOf(numbers.protectedSpanRegressions()));
        release.put("preserved", BigDecimal.valueOf(numbers.preserved()));
        root.put("releaseNumbers", release);
        root.put("finalReadOrder", List.of(REAUDIT_PHASE, RECONCILE_PHASE, "ADVERSARIAL_COVERAGE",
                "ADVERSARIAL_REGRESSION", "APP_RELEASE_NUMBERS"));
        root.put("pairDeltaQa", "NONE");
        root.put("stopReceipt", "NONE");
        return EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
    }

    private static List<Object> probeRows(List<Probe> probes) {
        List<Object> rows = new ArrayList<>();
        for (Probe probe : probes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("probeId", probe.probeId());
            row.put("finding", probe.finding());
            row.put("verdict", probe.verdict());
            rows.add(row);
        }
        return rows;
    }

    private static int viL2LineCount(Request request) {
        return new String(request.l2().viL2Bytes(), StandardCharsets.UTF_8).split("\\r?\\n", -1).length;
    }

    private static Map<String, byte[]> visible(EditorialPhaseContextProjector.PhaseProjection projection) {
        Map<String, byte[]> result = new TreeMap<>();
        for (Map.Entry<String, EditorialPhaseContextProjector.BundleAsset> entry : projection.visibleAssets().entrySet()) {
            result.put(entry.getKey(), entry.getValue().bytes());
        }
        return result;
    }

    private static long size(Map<String, byte[]> sources) {
        long total = 0L;
        for (byte[] bytes : sources.values()) total += bytes == null ? 0 : bytes.length;
        return total;
    }

    private static boolean validBudget(EditorialL2Execution.Budget budget) {
        return budget != null && budget.maximumInputBytes() > 0 && budget.maximumOutputTokens() > 0
                && budget.maximumCost().signum() >= 0 && budget.maximumExecutionTimeMillis() > 0;
    }

    private static void recover(EditorialL2Execution.Store store, String attemptIdentity, String reason) {
        try {
            store.markRecoveryRequired(attemptIdentity, reason);
        } catch (RuntimeException ignored) {
            // The claim stays IN_FLIGHT and is never re-dispatched automatically.
        }
    }

    private static Result stop(EditorialL2Execution.StopClass stopClass, String reason, List<String> issues, int calls) {
        return new Result(Outcome.STOPPED, stopClass, reason, issues, null, null, calls);
    }
}
