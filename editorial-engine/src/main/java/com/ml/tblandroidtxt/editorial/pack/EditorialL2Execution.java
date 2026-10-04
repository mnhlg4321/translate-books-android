package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Bounded L2_EDIT boundary: exact REPORT_L1 predecessor gate, one provider
 * call, compact change-row wire, app-owned DRAFT to VI_L2 reconstruction and
 * one atomic VI_L2 + CHANGE_MAP_L2 commit.
 *
 * <p>The model never returns the edited text, hashes or identities. The app
 * decides identity, diff, coverage of declared rows and the commit. A typed
 * stop never leaves a half-valid pair: the store receives both artifacts in a
 * single commit or nothing.</p>
 */
public final class EditorialL2Execution {
    public static final String PHASE = "L2_EDIT";
    public static final String DISCOVERY_PHASE = "L2_RAW_DISCOVERY";
    public static final String DISCOVERY_WIRE = "safe4.l2.raw-discovery.wire.v1";
    /** Ledger-contract discovery: coverage ranges and sparse candidates over the app's RAW inventory. */
    public static final String DISCOVERY_WIRE_V3 = "safe4.l2.raw-discovery.wire.v3";
    public static final String CANDIDATES_BLOCK = "L2_RAW_CANDIDATES";
    public static final int MAX_CANDIDATES = EditorialFieldSpec.MAX_L2_CANDIDATES;
    static final Set<String> LEDGERS = Set.of("UNIT", "TG", "SR", "RC");
    static final Set<String> STATUSES = Set.of("PROCESSED", "PRESERVED", "UNPROCESSED", "CONFLICT");
    public static final String WIRE_SCHEMA_VERSION = "safe4.l2.edit.wire.v1";
    /** Edit wire of the ledger contract: change operations and one resolution per L1 finding. */
    public static final String WIRE_SCHEMA_VERSION_V3 = "safe4.l2.edit.wire.v3";
    public static final String VI_L2_SCHEMA = "safe4.full.vi-l2.v1";
    public static final int MAX_WIRE_BYTES = 65_536;
    public static final int MAX_CHANGES = EditorialFieldSpec.MAX_L2_CHANGES;
    public static final int MAX_PRESERVED = EditorialFieldSpec.MAX_L2_PRESERVED;
    public static final int MAX_TEXT_FIELD = EditorialFieldSpec.MAX_MODEL_TEXT;
    private static final String ATTEMPT_DOMAIN = "EDITORIAL_L2_EDIT_ATTEMPT_IDENTITY_V2\n";

    public enum Outcome { COMMITTED, ALREADY_COMMITTED, STOPPED }

    public enum StopClass {
        INPUT_REQUIRED, REPAIR_REQUIRED, RETRY_REQUIRED, CONTENT_BLOCKED,
        BUDGET_EXCEEDED, AUTHORIZATION_REQUIRED, EXECUTION_DISABLED
    }

    /** Caps for exactly one L2_EDIT call; no automatic retry or repair call. */
    public record Budget(int maximumInputBytes, int maximumOutputTokens, BigDecimal maximumCost,
                         long maximumExecutionTimeMillis) {
        public Budget {
            Objects.requireNonNull(maximumCost, "maximumCost");
        }

        boolean valid() {
            return maximumInputBytes > 0 && maximumOutputTokens > 0 && maximumCost.signum() >= 0
                    && maximumExecutionTimeMillis > 0;
        }
    }

    public interface Provider {
        record Request(String attemptIdentity, String phase, String outputSchemaId,
                       Map<String, byte[]> visibleSources,
                       EditorialP5PilotRequest.PackAuthority authority, String chapterKey,
                       int maximumOutputTokens, long maximumExecutionTimeMillis) { }

        record Response(byte[] responseBytes, String finishReason, boolean transportComplete,
                        long inputTokens, long outputTokens, BigDecimal cost, boolean costKnown) { }

        Response call(Request request) throws Exception;
    }

    public record Committed(String attemptIdentity, String predecessorIdentity, String bundleIdentity,
                            byte[] viL2Bytes, String viL2Sha256, byte[] changeMapBytes,
                            String changeMapSha256) {
        public Committed {
            viL2Bytes = viL2Bytes.clone();
            changeMapBytes = changeMapBytes.clone();
        }

        @Override public byte[] viL2Bytes() { return viL2Bytes.clone(); }
        @Override public byte[] changeMapBytes() { return changeMapBytes.clone(); }
    }

    /** One writer per chapter; a real implementation backs commit by one transaction/row. */
    public interface Store {
        enum Claim { ACQUIRED, ALREADY_COMMITTED, IN_FLIGHT, RECOVERY_REQUIRED }

        Claim claim(String attemptIdentity, String predecessorIdentity, String bundleIdentity);

        void commit(Committed value);

        Optional<Committed> findCommitted(String attemptIdentity);

        void markRecoveryRequired(String attemptIdentity, String reasonCode);
    }

    public record Result(Outcome outcome, StopClass stopClass, String reasonCode, List<String> issues,
                         Committed committed, int providerCalls) {
        public Result {
            issues = List.copyOf(issues == null ? List.of() : issues);
        }

        public boolean accepted() {
            return outcome == Outcome.COMMITTED || outcome == Outcome.ALREADY_COMMITTED;
        }
    }

    /**
     * @param context the exact L1 request facts (binding, pinned sources, authority); its phase is ignored
     * @param reportL1AttemptIdentity the committed L1_RECONCILE attempt that is the predecessor
     * @param reportL1Bytes the persisted REPORT_L1 bytes read back from the store
     */
    public record Request(EditorialP5PilotRequest context, String reportL1AttemptIdentity,
                          byte[] reportL1Bytes, Set<Integer> protectedLineNumbers) {
        public Request {
            reportL1Bytes = reportL1Bytes == null ? null : reportL1Bytes.clone();
            protectedLineNumbers = protectedLineNumbers == null ? Set.of() : Set.copyOf(protectedLineNumbers);
        }

        @Override public byte[] reportL1Bytes() { return reportL1Bytes == null ? null : reportL1Bytes.clone(); }

        public String attemptIdentity() {
            EditorialP4Binding binding = context.binding();
            String value = binding.bindingIdentity() + "\n" + binding.runDeclarationIdentity() + "\n"
                    + binding.canonicalPackHash() + "\n" + context.chapterKey() + "\n" + PHASE + "\n"
                    + reportL1AttemptIdentity + "\n"
                    + EditorialCanonicalJson.sha256Hex(reportL1Bytes) + "\n" + context.bundleIdentity()
                    + EditorialContractRevision.identitySuffix(context.contractRevision());
            return EditorialCanonicalJson.sha256Hex((ATTEMPT_DOMAIN + value).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** One blind-discovery candidate: the app-owned unit the edit call must resolve. */
    public record Candidate(String candidateId, String ledger, int line) { }

    /**
     * Two calls under one claim: a blind {@code L2_RAW_DISCOVERY} (RAW and GLOSSARY only) that lists the
     * candidates, then {@code L2_EDIT} which receives that list as the app-owned block
     * {@value #CANDIDATES_BLOCK} and must resolve every candidate. The app counts the resolutions.
     *
     * @param discoveryBudget caps for the discovery call
     * @param editBudget caps for the edit call
     */
    public Result execute(Request request, Budget discoveryBudget, Budget editBudget, Provider provider, Store store) {
        return execute(request, discoveryBudget, editBudget, null, provider, store);
    }

    /**
     * Ledger contract: a third call, {@value EditorialFinalRead#L2_PHASE}, reads the exact VI_L2 bytes the app
     * built before they are committed. The legacy contract ignores {@code finalReadBudget}.
     */
    public Result execute(Request request, Budget discoveryBudget, Budget editBudget, Budget finalReadBudget,
                          Provider provider, Store store) {
        if (request == null || request.context() == null) {
            return stop(StopClass.INPUT_REQUIRED, "INPUT_L2_REQUEST_MISSING", List.of(), 0);
        }
        if (discoveryBudget == null || !discoveryBudget.valid() || editBudget == null || !editBudget.valid()) {
            return stop(StopClass.AUTHORIZATION_REQUIRED, "L2_BUDGET_REQUIRED", List.of(), 0);
        }
        EditorialP5PilotRequest context = request.context();
        String predecessorIssue = predecessorIssue(request);
        if (predecessorIssue != null) {
            return stop(StopClass.INPUT_REQUIRED, predecessorIssue, List.of(), 0);
        }
        final boolean ledger = EditorialContractRevision.isLedger(context.contractRevision());
        EditorialL1Ledger.Body l1 = null;
        EditorialRawInventory.Inventory inventory = null;
        if (ledger) {
            if (finalReadBudget == null || !finalReadBudget.valid()) {
                return stop(StopClass.AUTHORIZATION_REQUIRED, "L2_FINAL_READ_BUDGET_REQUIRED", List.of(), 0);
            }
            try {
                l1 = EditorialL1Ledger.parseBody(EditorialCanonicalJson.parseObject(request.reportL1Bytes()));
                inventory = EditorialRawInventory.build(context.source(EditorialSafe4Contract.RAW).bytes());
            } catch (RuntimeException invalid) {
                return stop(StopClass.INPUT_REQUIRED, "INPUT_REPORT_L1_LEDGER_INVALID", List.of(), 0);
            }
            if (!inventory.inventorySha256().equals(l1.inventorySha256()) || !inventory.rawSha256().equals(l1.rawSha256())) {
                return stop(StopClass.INPUT_REQUIRED, "INPUT_REPORT_L1_LEDGER_STALE", List.of(), 0);
            }
        }
        Map<String, byte[]> discoverySources;
        Map<String, byte[]> editSources;
        try {
            discoverySources = visible(project(request, DISCOVERY_PHASE));
            editSources = visible(project(request, PHASE));
        } catch (RuntimeException invalid) {
            return stop(StopClass.INPUT_REQUIRED, "INPUT_L2_PROJECTION_INVALID", List.of(), 0);
        }
        if (size(discoverySources) > discoveryBudget.maximumInputBytes()
                || size(editSources) > editBudget.maximumInputBytes()) {
            return stop(StopClass.BUDGET_EXCEEDED, "L2_INPUT_BUDGET_EXCEEDED", List.of(), 0);
        }
        if (provider == null || store == null) {
            return stop(StopClass.EXECUTION_DISABLED, "L2_PROVIDER_OR_STORE_NOT_CONFIGURED", List.of(), 0);
        }

        String attemptIdentity = request.attemptIdentity();
        String bundleIdentity = context.bundleIdentity();
        Store.Claim claim;
        try {
            claim = store.claim(attemptIdentity, request.reportL1AttemptIdentity(), bundleIdentity);
        } catch (RuntimeException error) {
            return stop(StopClass.RETRY_REQUIRED, "RETRY_L2_CLAIM_FAILED", List.of(), 0);
        }
        switch (claim) {
            case ALREADY_COMMITTED -> {
                Optional<Committed> existing = store.findCommitted(attemptIdentity);
                return existing.map(value -> new Result(Outcome.ALREADY_COMMITTED, null,
                        "L2_ALREADY_COMMITTED", List.of(), value, 0))
                        .orElseGet(() -> stop(StopClass.RETRY_REQUIRED,
                                "RETRY_L2_COMMITTED_RESULT_UNAVAILABLE", List.of(), 0));
            }
            case IN_FLIGHT -> {
                return stop(StopClass.RETRY_REQUIRED, "RETRY_L2_CALL_STATE_UNKNOWN", List.of(), 0);
            }
            case RECOVERY_REQUIRED -> {
                return stop(StopClass.RETRY_REQUIRED, "STOP_L2_EXTERNAL_CALL_STATE_UNRESOLVED", List.of(), 0);
            }
            case ACQUIRED -> { }
        }

        // Call 1: blind discovery. It never sees DRAFT, REPORT_L1 or PRONOUN.
        CallOutcome first = call(provider, store, attemptIdentity, DISCOVERY_PHASE, ledger ? DISCOVERY_WIRE_V3 : DISCOVERY_WIRE,
                discoverySources, context, discoveryBudget, 0);
        if (first.stop != null) return first.stop;
        List<Candidate> candidates;
        List<EditorialRawInventory.Range> discoveryCoverage = List.of();
        try {
            if (ledger) {
                EditorialL1Ledger.RawPass pass = EditorialL1Ledger.parseRawPass(first.bytes, attemptIdentity, inventory,
                        DISCOVERY_WIRE_V3);
                List<Candidate> converted = new ArrayList<>();
                for (EditorialL1Ledger.Candidate c : pass.candidates()) {
                    converted.add(new Candidate(c.candidateId(), c.ledger(), inventory.unit(c.unitId()).line()));
                }
                candidates = List.copyOf(converted);
                discoveryCoverage = pass.coverage();
            } else {
                candidates = parseDiscovery(first.bytes, attemptIdentity, rawLineCount(context));
            }
        } catch (RuntimeException invalid) {
            recover(store, attemptIdentity, "REPAIR_L2_DISCOVERY_SCHEMA_INVALID");
            return stop(StopClass.REPAIR_REQUIRED, "REPAIR_L2_DISCOVERY_SCHEMA_INVALID",
                    List.of(safeMessage(invalid)), 1);
        }

        // Call 2: edit, with the app-owned candidate list as an extra visible block.
        byte[] block = candidateBlock(candidates);
        Map<String, byte[]> secondSources = new TreeMap<>(editSources);
        secondSources.put(CANDIDATES_BLOCK, block);
        if (size(secondSources) > editBudget.maximumInputBytes()) {
            recover(store, attemptIdentity, "L2_INPUT_BUDGET_EXCEEDED");
            return stop(StopClass.BUDGET_EXCEEDED, "L2_INPUT_BUDGET_EXCEEDED", List.of(), 1);
        }
        CallOutcome second = call(provider, store, attemptIdentity, PHASE, ledger ? WIRE_SCHEMA_VERSION_V3 : WIRE_SCHEMA_VERSION,
                secondSources, context, editBudget, 1);
        if (second.stop != null) return second.stop;

        EditWire wire;
        try {
            wire = ledger ? parseEditWireV3(second.bytes, attemptIdentity, candidates, l1, inventory)
                    : parseEditWire(second.bytes, attemptIdentity, candidates);
        } catch (RuntimeException invalid) {
            recover(store, attemptIdentity, "REPAIR_L2_OUTPUT_SCHEMA_INVALID");
            return stop(StopClass.REPAIR_REQUIRED, "REPAIR_L2_OUTPUT_SCHEMA_INVALID",
                    List.of(safeMessage(invalid)), 2);
        }
        if (wire.rows().stopClass() != null) {
            recover(store, attemptIdentity, wire.rows().reasonCode());
            return stop(wire.rows().stopClass(), wire.rows().reasonCode(), List.of(), 2);
        }
        // A model-declared PASS is not evidence: the app counts every candidate itself.
        int unresolved = 0;
        int unprocessed = 0;
        int conflicts = 0;
        for (Candidate candidate : candidates) {
            String status = wire.resolutions().get(candidate.candidateId());
            if (status == null) unresolved++;
            else if ("UNPROCESSED".equals(status)) unprocessed++;
            else if ("CONFLICT".equals(status)) conflicts++;
        }
        if (unresolved > 0) {
            recover(store, attemptIdentity, "REPAIR_L2_RESOLUTIONS_INCOMPLETE");
            return stop(StopClass.REPAIR_REQUIRED, "REPAIR_L2_RESOLUTIONS_INCOMPLETE",
                    List.of("L2_UNRESOLVED_CANDIDATES:" + unresolved), 2);
        }
        if (conflicts > 0) {
            recover(store, attemptIdentity, "CONTENT_L2_CANDIDATE_CONFLICT");
            return stop(StopClass.CONTENT_BLOCKED, "CONTENT_L2_CANDIDATE_CONFLICT",
                    List.of("L2_CONFLICT_CANDIDATES:" + conflicts), 2);
        }
        if (unprocessed > 0) {
            recover(store, attemptIdentity, "REPAIR_L2_CANDIDATES_UNPROCESSED");
            return stop(StopClass.REPAIR_REQUIRED, "REPAIR_L2_CANDIDATES_UNPROCESSED",
                    List.of("L2_UNPROCESSED_CANDIDATES:" + unprocessed), 2);
        }

        Set<Integer> protectedLines = request.protectedLineNumbers();
        if (ledger) {
            Set<Integer> merged = new java.util.TreeSet<>(protectedLines);
            merged.addAll(EditorialL1Ledger.protectedLines(l1.protectedSpans()));
            protectedLines = merged;
        }
        EditorialChangeMapReconstructor.Result reconstruction = new EditorialChangeMapReconstructor()
                .reconstruct(new EditorialChangeMapReconstructor.Request(
                        EditorialChangeMapReconstructor.Kind.L2_DRAFT_TO_VI_L2,
                        context.source(EditorialSafe4Contract.DRAFT).bytes(),
                        request.reportL1AttemptIdentity(), wire.rows().changes(), wire.rows().preserved(),
                        protectedLines));
        if (!reconstruction.accepted()) {
            StopClass stopClass = reconstruction.status() == EditorialChangeMapReconstructor.Status.INPUT_REQUIRED
                    ? StopClass.INPUT_REQUIRED : StopClass.REPAIR_REQUIRED;
            String reason = stopClass == StopClass.INPUT_REQUIRED
                    ? "INPUT_L2_BASE_INVALID" : "REPAIR_L2_CHANGE_MAP_INVALID";
            recover(store, attemptIdentity, reason);
            return stop(stopClass, reason, reconstruction.issues(), 2);
        }

        Map<String, Object> l1Evidence = null;
        Map<String, Object> readEvidence = null;
        if (ledger) {
            EditorialL2Findings.Verdict verdict = EditorialL2Findings.verify(l1, inventory, wire.findingResolutions(),
                    wire.rows().changes(), wire.rows().preserved(), reconstruction);
            if (!verdict.issues().isEmpty()) {
                recover(store, attemptIdentity, "REPAIR_L2_FINDING_RESOLUTION_INVALID");
                return stop(StopClass.REPAIR_REQUIRED, "REPAIR_L2_FINDING_RESOLUTION_INVALID", verdict.issues(), 2);
            }
            if (!verdict.unresolved().isEmpty()) {
                recover(store, attemptIdentity, "CONTENT_L2_FINDING_UNRESOLVED");
                List<String> open = new ArrayList<>();
                for (String id : verdict.unresolved()) open.add("L2_UNRESOLVED_FINDING:" + id);
                return stop(StopClass.CONTENT_BLOCKED, "CONTENT_L2_FINDING_UNRESOLVED", open, 2);
            }
            l1Evidence = EditorialL2Findings.evidence(l1, verdict, wire.findingResolutions(), request.reportL1Bytes());
        }

        byte[] viL2 = reconstruction.outputBytes();
        if (ledger) {
            // Third call: the model reads the exact built bytes; the app checks the echoed hash and probe tails.
            Map<String, byte[]> readSources = new TreeMap<>();
            readSources.put(EditorialSafe4Contract.RAW, context.source(EditorialSafe4Contract.RAW).bytes());
            if (context.source(EditorialSafe4Contract.GLOSSARY) != null && context.source(EditorialSafe4Contract.GLOSSARY).bytes() != null) {
                readSources.put(EditorialSafe4Contract.GLOSSARY, context.source(EditorialSafe4Contract.GLOSSARY).bytes());
            }
            readSources.put(EditorialFinalRead.TARGET_ROLE, viL2);
            readSources.put(EditorialFinalRead.PROBES_ROLE, EditorialFinalRead.probeBlock(viL2));
            if (size(readSources) > finalReadBudget.maximumInputBytes()) {
                recover(store, attemptIdentity, "L2_INPUT_BUDGET_EXCEEDED");
                return stop(StopClass.BUDGET_EXCEEDED, "L2_INPUT_BUDGET_EXCEEDED", List.of(EditorialFinalRead.L2_PHASE), 2);
            }
            CallOutcome third = call(provider, store, attemptIdentity, EditorialFinalRead.L2_PHASE, EditorialFinalRead.WIRE,
                    readSources, context, finalReadBudget, 2);
            if (third.stop != null) return third.stop;
            try {
                readEvidence = EditorialFinalRead.evidence(EditorialFinalRead.L2_PHASE,
                        EditorialFinalRead.parse(third.bytes, attemptIdentity, viL2));
            } catch (RuntimeException invalid) {
                recover(store, attemptIdentity, "REPAIR_L2_FINAL_READ_INVALID");
                return stop(StopClass.REPAIR_REQUIRED, "REPAIR_L2_FINAL_READ_INVALID", List.of(safeMessage(invalid)), 3);
            }
        }
        byte[] changeMap = withDiscoveryEvidence(reconstruction.changeMapBytes(), candidates, wire.resolutions(), block);
        if (ledger) changeMap = withLedgerEvidence(changeMap, l1Evidence, readEvidence, inventory, discoveryCoverage);
        final int totalCalls = ledger ? 3 : 2;
        Committed committed = new Committed(attemptIdentity, request.reportL1AttemptIdentity(),
                bundleIdentity, viL2, EditorialCanonicalJson.sha256Hex(viL2), changeMap,
                EditorialCanonicalJson.sha256Hex(changeMap));
        try {
            store.commit(committed);
        } catch (RuntimeException error) {
            recover(store, attemptIdentity, "RETRY_L2_ATOMIC_COMMIT_FAILED");
            return stop(StopClass.RETRY_REQUIRED, "RETRY_L2_ATOMIC_COMMIT_FAILED", List.of(), totalCalls);
        }
        Optional<Committed> readback = store.findCommitted(attemptIdentity);
        if (readback.isEmpty() || !readback.get().viL2Sha256().equals(committed.viL2Sha256())
                || !readback.get().changeMapSha256().equals(committed.changeMapSha256())) {
            return stop(StopClass.RETRY_REQUIRED, "RETRY_L2_READBACK_MISMATCH", List.of(), totalCalls);
        }
        return new Result(Outcome.COMMITTED, null, "L2_COMMITTED", List.of(), readback.get(), totalCalls);
    }

    private record CallOutcome(byte[] bytes, Result stop) { }

    private static CallOutcome call(Provider provider, Store store, String attemptIdentity, String phase,
                                    String schema, Map<String, byte[]> sources, EditorialP5PilotRequest context,
                                    Budget budget, int priorCalls) {
        int calls = priorCalls + 1;
        Provider.Response response;
        try {
            response = provider.call(new Provider.Request(attemptIdentity, phase, schema,
                    Collections.unmodifiableMap(new TreeMap<>(sources)), context.authority(), context.chapterKey(),
                    budget.maximumOutputTokens(), budget.maximumExecutionTimeMillis()));
        } catch (Exception error) {
            recover(store, attemptIdentity, "RETRY_L2_PROVIDER_CALL_FAILED");
            return new CallOutcome(null, stop(StopClass.RETRY_REQUIRED, "RETRY_L2_PROVIDER_CALL_FAILED",
                    List.of(phase), calls));
        }
        if (response == null || response.responseBytes() == null) {
            recover(store, attemptIdentity, "RETRY_L2_PROVIDER_EMPTY_RESPONSE");
            return new CallOutcome(null, stop(StopClass.RETRY_REQUIRED, "RETRY_L2_PROVIDER_EMPTY_RESPONSE",
                    List.of(phase), calls));
        }
        String finish = response.finishReason() == null ? "" : response.finishReason().toLowerCase(java.util.Locale.ROOT);
        if (!response.transportComplete() || "length".equals(finish) || "max_tokens".equals(finish)
                || "max_output_tokens".equals(finish)) {
            recover(store, attemptIdentity, "RETRY_L2_OUTPUT_TRUNCATED");
            return new CallOutcome(null, stop(StopClass.RETRY_REQUIRED, "RETRY_L2_OUTPUT_TRUNCATED",
                    List.of(phase), calls));
        }
        if (!response.costKnown()) {
            recover(store, attemptIdentity, "RETRY_L2_PROVIDER_COST_UNAVAILABLE");
            return new CallOutcome(null, stop(StopClass.RETRY_REQUIRED, "RETRY_L2_PROVIDER_COST_UNAVAILABLE",
                    List.of(phase), calls));
        }
        if (response.outputTokens() > budget.maximumOutputTokens()
                || response.cost().compareTo(budget.maximumCost()) > 0) {
            recover(store, attemptIdentity, "L2_TOKEN_OR_COST_BUDGET_EXCEEDED");
            return new CallOutcome(null, stop(StopClass.BUDGET_EXCEEDED, "L2_TOKEN_OR_COST_BUDGET_EXCEEDED",
                    List.of(phase), calls));
        }
        return new CallOutcome(response.responseBytes(), null);
    }

    /** Strict blind-discovery wire: unknown keys, bad ids/ledgers and out-of-range RAW lines are rejected. */
    static List<Candidate> parseDiscovery(byte[] bytes, String attemptIdentity, int rawLines) {
        if (bytes == null || bytes.length > MAX_WIRE_BYTES) throw WireViolation.at("L2_WIRE_BYTE_LIMIT_EXCEEDED", "root");
        Map<String, Object> root = EditorialCanonicalJson.parseObject(bytes);
        keys(root, Set.of("wireSchemaVersion", "attemptIdentity", "candidates"), "root");
        if (!DISCOVERY_WIRE.equals(root.get("wireSchemaVersion"))) throw WireViolation.at("L2_WIRE_SCHEMA_INVALID", "wireSchemaVersion");
        if (!attemptIdentity.equals(root.get("attemptIdentity"))) throw WireViolation.at("L2_WIRE_ATTEMPT_ECHO_MISMATCH", "attemptIdentity");
        List<Object> values = EditorialCanonicalJson.array(root.get("candidates"), "candidates");
        if (values.size() > MAX_CANDIDATES) throw WireViolation.at("L2_WIRE_ROW_LIMIT_EXCEEDED", "candidates");
        List<Candidate> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        boolean unit = false;
        for (int index = 0; index < values.size(); index++) {
            String path = "candidates." + index;
            Map<String, Object> row = object(values.get(index), path);
            keys(row, Set.of("candidateId", "ledger", "line"), path);
            String id = text(row, "candidateId", path + ".candidateId", EditorialFieldSpec.L2_RAW_DISCOVERY);
            if (!EditorialP5RawWireContract.token(id, EditorialP5RawWireContract.MAX_ID_LENGTH) || !ids.add(id)) {
                throw WireViolation.at("L2_WIRE_CANDIDATE_ID_INVALID", path + ".candidateId");
            }
            String ledger = text(row, "ledger", path + ".ledger", EditorialFieldSpec.L2_RAW_DISCOVERY);
            if (!LEDGERS.contains(ledger)) throw WireViolation.at("L2_WIRE_LEDGER_INVALID", path + ".ledger");
            int line = line(row, path + ".line");
            if (line < 0 || line > rawLines) throw WireViolation.at("L2_WIRE_LINE_OUT_OF_RANGE", path + ".line");
            unit |= "UNIT".equals(ledger);
            result.add(new Candidate(id, ledger, line));
        }
        // An exhaustive discovery always enumerates raw units; an empty unit ledger is not evidence.
        if (!unit) throw WireViolation.at("L2_WIRE_RAW_UNITS_MISSING", "candidates");
        return List.copyOf(result);
    }

    record EditWire(Wire rows, Map<String, String> resolutions,
                    List<EditorialL2Findings.Resolution> findingResolutions) {
        EditWire(Wire rows, Map<String, String> resolutions) { this(rows, resolutions, List.of()); }
    }

    /**
     * Ledger-contract edit wire: the v1 row grammar (with the optional {@code op}) plus
     * {@code findingResolutions}. The set of L1 error ids comes from the persisted report, never from the model.
     */
    static EditWire parseEditWireV3(byte[] bytes, String attemptIdentity, List<Candidate> candidates,
                                    EditorialL1Ledger.Body l1, EditorialRawInventory.Inventory inventory) {
        if (bytes == null || bytes.length > MAX_WIRE_BYTES) throw WireViolation.at("L2_WIRE_BYTE_LIMIT_EXCEEDED", "root");
        Map<String, Object> root = EditorialCanonicalJson.parseObject(bytes);
        keys(root, Set.of("wireSchemaVersion", "attemptIdentity", "resolutions", "findingResolutions", "changes",
                "preserved", "disposition"), "root");
        if (!WIRE_SCHEMA_VERSION_V3.equals(root.get("wireSchemaVersion"))) {
            throw WireViolation.at("L2_WIRE_SCHEMA_INVALID", "wireSchemaVersion");
        }
        Map<String, Object> rowsShape = new LinkedHashMap<>(root);
        rowsShape.remove("resolutions");
        rowsShape.remove("findingResolutions");
        rowsShape.put("wireSchemaVersion", WIRE_SCHEMA_VERSION);
        Wire rows = parseWire(EditorialCanonicalJson.canonicalize(rowsShape).getBytes(StandardCharsets.UTF_8),
                attemptIdentity, true, EditorialFieldSpec.L2_EDIT);
        Set<String> known = new HashSet<>();
        for (Candidate candidate : candidates) known.add(candidate.candidateId());
        Map<String, String> resolutions = new TreeMap<>();
        List<Object> resolutionRows = EditorialCanonicalJson.array(root.get("resolutions"), "resolutions");
        for (int index = 0; index < resolutionRows.size(); index++) {
            String path = "resolutions." + index;
            Map<String, Object> row = object(resolutionRows.get(index), path);
            keys(row, Set.of("candidateId", "status"), path);
            String id = text(row, "candidateId", path + ".candidateId", EditorialFieldSpec.L2_EDIT);
            String status = text(row, "status", path + ".status", EditorialFieldSpec.L2_EDIT);
            if (!known.contains(id) || resolutions.containsKey(id)) {
                throw WireViolation.at("L2_WIRE_RESOLUTION_ID_INVALID", path + ".candidateId");
            }
            if (!STATUSES.contains(status)) throw WireViolation.at("L2_WIRE_STATUS_INVALID", path + ".status");
            resolutions.put(id, status);
        }
        Set<String> errorIds = new HashSet<>();
        for (EditorialL1Ledger.Finding finding : l1.findings()) errorIds.add(finding.errorId());
        List<EditorialL2Findings.Resolution> findingResolutions =
                EditorialL2Findings.parse(root.get("findingResolutions"), errorIds, inventory);
        return new EditWire(rows, Map.copyOf(resolutions), findingResolutions);
    }

    /** Adds what became of every L1 finding and the final read of the built VI_L2 to the CHANGE_MAP_L2. */
    private static byte[] withLedgerEvidence(byte[] changeMap, Map<String, Object> l1Resolution,
                                             Map<String, Object> finalRead, EditorialRawInventory.Inventory inventory,
                                             List<EditorialRawInventory.Range> discoveryCoverage) {
        Map<String, Object> root = new LinkedHashMap<>(EditorialCanonicalJson.parseObject(changeMap));
        root.put("l1Resolution", l1Resolution);
        root.put("finalRead", finalRead);
        // the blind pass is judged on the app's inventory: closed ranges over every unit, not on a raw count
        Map<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("inventorySha256", inventory.inventorySha256());
        coverage.put("unitCount", BigDecimal.valueOf(inventory.units().size()));
        List<Object> ranges = new ArrayList<>();
        for (EditorialRawInventory.Range range : discoveryCoverage) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("from", range.fromId());
            row.put("to", range.toId());
            row.put("status", range.status());
            ranges.add(row);
        }
        coverage.put("ranges", ranges);
        root.put("discoveryCoverage", coverage);
        return EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * L2_EDIT wire = the strict row grammar of {@link #parseWire} plus {@code resolutions}. Every known
     * candidate id may appear at most once; completeness is judged by the app after STOP wires are handled.
     */
    static EditWire parseEditWire(byte[] bytes, String attemptIdentity, List<Candidate> candidates) {
        if (bytes == null || bytes.length > MAX_WIRE_BYTES) throw WireViolation.at("L2_WIRE_BYTE_LIMIT_EXCEEDED", "root");
        Map<String, Object> root = EditorialCanonicalJson.parseObject(bytes);
        keys(root, Set.of("wireSchemaVersion", "attemptIdentity", "resolutions", "changes", "preserved", "disposition"),
                "root");
        Map<String, Object> rowsShape = new LinkedHashMap<>(root);
        rowsShape.remove("resolutions");
        Wire rows = parseWire(EditorialCanonicalJson.canonicalize(rowsShape).getBytes(StandardCharsets.UTF_8),
                attemptIdentity, false, EditorialFieldSpec.L2_EDIT);
        Set<String> known = new HashSet<>();
        for (Candidate candidate : candidates) known.add(candidate.candidateId());
        Map<String, String> resolutions = new TreeMap<>();
        List<Object> resolutionRows = EditorialCanonicalJson.array(root.get("resolutions"), "resolutions");
        for (int index = 0; index < resolutionRows.size(); index++) {
            String path = "resolutions." + index;
            Map<String, Object> row = object(resolutionRows.get(index), path);
            keys(row, Set.of("candidateId", "status"), path);
            String id = text(row, "candidateId", path + ".candidateId", EditorialFieldSpec.L2_EDIT);
            String status = text(row, "status", path + ".status", EditorialFieldSpec.L2_EDIT);
            if (!known.contains(id) || resolutions.containsKey(id)) {
                throw WireViolation.at("L2_WIRE_RESOLUTION_ID_INVALID", path + ".candidateId");
            }
            if (!STATUSES.contains(status)) throw WireViolation.at("L2_WIRE_STATUS_INVALID", path + ".status");
            resolutions.put(id, status);
        }
        return new EditWire(rows, Map.copyOf(resolutions));
    }

    static byte[] candidateBlock(List<Candidate> candidates) {
        List<Object> rows = new ArrayList<>();
        for (Candidate candidate : candidates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("candidateId", candidate.candidateId());
            row.put("ledger", candidate.ledger());
            row.put("line", BigDecimal.valueOf(candidate.line()));
            rows.add(row);
        }
        return EditorialCanonicalJson.canonicalize(Map.of("candidates", rows)).getBytes(StandardCharsets.UTF_8);
    }

    /** Adds the discovery candidates and their resolutions to the canonical CHANGE_MAP_L2 (one extra key). */
    private static byte[] withDiscoveryEvidence(byte[] changeMap, List<Candidate> candidates,
                                                Map<String, String> resolutions, byte[] block) {
        Map<String, Object> root = new LinkedHashMap<>(EditorialCanonicalJson.parseObject(changeMap));
        Map<String, Object> discovery = new LinkedHashMap<>();
        discovery.put("phase", DISCOVERY_PHASE);
        discovery.put("candidatesSha256", EditorialCanonicalJson.sha256Hex(block));
        discovery.put("candidateCount", BigDecimal.valueOf(candidates.size()));
        Map<String, Object> ledgers = new TreeMap<>();
        List<Object> rows = new ArrayList<>();
        for (Candidate candidate : candidates) {
            String status = resolutions.get(candidate.candidateId());
            Map<String, Object> counts = castMap(ledgers.computeIfAbsent(candidate.ledger(), k -> new TreeMap<String, Object>()));
            counts.merge(status, BigDecimal.ONE, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("candidateId", candidate.candidateId());
            row.put("ledger", candidate.ledger());
            row.put("line", BigDecimal.valueOf(candidate.line()));
            row.put("status", status);
            rows.add(row);
        }
        discovery.put("ledgers", ledgers);
        discovery.put("candidates", rows);
        root.put("rawDiscovery", discovery);
        return EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) { return (Map<String, Object>) value; }

    private static int rawLineCount(EditorialP5PilotRequest context) {
        byte[] raw = context.source(EditorialSafe4Contract.RAW) == null ? null : context.source(EditorialSafe4Contract.RAW).bytes();
        return raw == null ? 0 : new String(raw, StandardCharsets.UTF_8).split("\\r?\\n", -1).length;
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

    /** REPORT_L1 must be the persisted, non-stopped RECONCILE artifact of this exact chain. */
    static String predecessorIssue(Request request) {
        if (request.reportL1Bytes() == null
                || !EditorialP5RawWireContract.token(request.reportL1AttemptIdentity(), 128)) {
            return "INPUT_REPORT_L1_REQUIRED";
        }
        Map<String, Object> report;
        try {
            report = EditorialCanonicalJson.parseObject(request.reportL1Bytes());
        } catch (RuntimeException invalid) {
            return "INPUT_REPORT_L1_INVALID";
        }
        EditorialP5PilotRequest context = request.context();
        boolean ledgerChain = EditorialContractRevision.isLedger(context.contractRevision());
        String reportRevision = EditorialContractRevision.ofReport(report);
        if (!"REPORT_L1".equals(report.get("artifactType")) || !"L1_RECONCILE".equals(report.get("phase"))) {
            return "INPUT_REPORT_L1_PHASE_INVALID";
        }
        // a report of the other contract is never an eligible predecessor; legacy reports stay readable elsewhere
        if (ledgerChain && !EditorialContractRevision.isLedger(reportRevision)) return "INPUT_REPORT_L1_LEGACY_CONTRACT";
        if (ledgerChain && !EditorialContractRevision.eligiblePredecessor(reportRevision, context.contractRevision())) return "INPUT_REPORT_L1_CONTRACT_MISMATCH";
        if (!ledgerChain && EditorialContractRevision.isLedger(reportRevision)) return "INPUT_REPORT_L1_CONTRACT_MISMATCH";
        String expectedSchema = ledgerChain ? EditorialContractRevision.REPORT_SCHEMA_V2
                : EditorialP5RawWireContract.FINAL_REPORT_SCHEMA;
        if (!expectedSchema.equals(report.get("schemaVersion"))) {
            return "INPUT_REPORT_L1_PHASE_INVALID";
        }
        if (!context.binding().bindingIdentity().equals(report.get("bindingIdentity"))
                || !context.binding().canonicalPackHash().equals(report.get("canonicalPackHash"))
                || !context.manifestFingerprint().equals(report.get("manifestFingerprint"))
                || !context.chapterKey().equals(report.get("chapterKey"))) {
            return "INPUT_REPORT_L1_IDENTITY_MISMATCH";
        }
        // The L1 bundle pins RAW/DRAFT/GLOSSARY/PRONOUN bytes; any drift invalidates the chain.
        if (!context.bundleIdentity().equals(report.get("bundleIdentity"))) {
            return "INPUT_REPORT_L1_SOURCE_DRIFT";
        }
        Object disposition = report.get("disposition");
        if (!"CONTINUE".equals(disposition) && !"PRESERVE_DRAFT".equals(disposition)) {
            return "INPUT_REPORT_L1_DISPOSITION_INVALID";
        }
        if (context.source(EditorialSafe4Contract.DRAFT) == null
                || context.source(EditorialSafe4Contract.DRAFT).bytes() == null) {
            return "INPUT_DRAFT_REQUIRED";
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
        return new EditorialPhaseContextProjector().project(
                new EditorialPhaseContextProjector.Bundle(assets, l1.pronounStatus()), phase);
    }

    record Wire(List<EditorialChangeMapReconstructor.ChangeRow> changes,
                List<EditorialChangeMapReconstructor.PreservedRow> preserved,
                StopClass stopClass, String reasonCode) { }

    /** Strict compact wire parser; unknown keys, oversize values and wrong echoes are rejected. */
    static Wire parseWire(byte[] bytes, String attemptIdentity) {
        return parseWire(bytes, attemptIdentity, false, EditorialFieldSpec.L2_EDIT);
    }

    /** {@code allowOps} (ledger contract) admits the optional {@code op} of a change row. */
    static Wire parseWire(byte[] bytes, String attemptIdentity, boolean allowOps) {
        return parseWire(bytes, attemptIdentity, allowOps, EditorialFieldSpec.L2_EDIT);
    }

    static Wire parseWire(byte[] bytes, String attemptIdentity, boolean allowOps, String phase) {
        if (bytes == null || bytes.length > MAX_WIRE_BYTES) throw WireViolation.at("L2_WIRE_BYTE_LIMIT_EXCEEDED", "root");
        Map<String, Object> root = EditorialCanonicalJson.parseObject(bytes);
        keys(root, Set.of("wireSchemaVersion", "attemptIdentity", "changes", "preserved", "disposition"), "root");
        if (!WIRE_SCHEMA_VERSION.equals(root.get("wireSchemaVersion"))) {
            throw WireViolation.at("L2_WIRE_SCHEMA_INVALID", "wireSchemaVersion");
        }
        if (!attemptIdentity.equals(root.get("attemptIdentity"))) {
            throw WireViolation.at("L2_WIRE_ATTEMPT_ECHO_MISMATCH", "attemptIdentity");
        }
        List<Object> changeValues = EditorialCanonicalJson.array(root.get("changes"), "changes");
        List<Object> preservedValues = EditorialCanonicalJson.array(root.get("preserved"), "preserved");
        if (changeValues.size() > MAX_CHANGES || preservedValues.size() > MAX_PRESERVED) {
            throw new IllegalArgumentException("L2_WIRE_ROW_LIMIT_EXCEEDED");
        }
        List<EditorialChangeMapReconstructor.ChangeRow> changes = new ArrayList<>();
        for (int index = 0; index < changeValues.size(); index++) {
            String path = "changes." + index;
            Map<String, Object> row = object(changeValues.get(index), path);
            if (allowOps) {
                keys(row, Set.of("changeId", "errorId", "line", "before", "after", "reason", "dialogue",
                        "speakerProof", "status", "op"), path, Set.of("speakerProof", "op"));
            } else {
                keys(row, Set.of("changeId", "errorId", "line", "before", "after", "reason", "dialogue",
                        "speakerProof", "status"), path, Set.of("speakerProof"));
            }
            EditorialChangeMapReconstructor.Op changeOp = EditorialChangeMapReconstructor.Op.REPLACE;
            if (row.containsKey("op")) {
                try {
                    changeOp = EditorialChangeMapReconstructor.Op.valueOf(text(row, "op", path + ".op", phase));
                } catch (IllegalArgumentException invalid) {
                    throw WireViolation.at("L2_WIRE_OP_INVALID", path + ".op");
                }
            }
            EditorialChangeMapReconstructor.SpeakerProof proof = null;
            if (row.containsKey("speakerProof")) {
                String proofPath = path + ".speakerProof";
                Map<String, Object> p = object(row.get("speakerProof"), proofPath);
                keys(p, Set.of("speaker", "listener", "anchorBefore", "anchorAfter"), proofPath);
                proof = new EditorialChangeMapReconstructor.SpeakerProof(text(p, "speaker", proofPath + ".speaker", phase),
                        text(p, "listener", proofPath + ".listener", phase), text(p, "anchorBefore", proofPath + ".anchorBefore", phase),
                        text(p, "anchorAfter", proofPath + ".anchorAfter", phase));
            }
            EditorialChangeMapReconstructor.DeclaredStatus status;
            try {
                status = EditorialChangeMapReconstructor.DeclaredStatus.valueOf(text(row, "status", path + ".status", phase));
            } catch (IllegalArgumentException invalid) {
                throw WireViolation.at("L2_WIRE_STATUS_INVALID", path + ".status");
            }
            changes.add(new EditorialChangeMapReconstructor.ChangeRow(text(row, "changeId", path + ".changeId", phase),
                    text(row, "errorId", path + ".errorId", phase), line(row, path + ".line"),
                    text(row, "before", path + ".before", phase), text(row, "after", path + ".after", phase),
                    text(row, "reason", path + ".reason", phase), bool(row, "dialogue", path + ".dialogue"), proof, status, changeOp));
        }
        List<EditorialChangeMapReconstructor.PreservedRow> preserved = new ArrayList<>();
        for (int index = 0; index < preservedValues.size(); index++) {
            String path = "preserved." + index;
            Map<String, Object> row = object(preservedValues.get(index), path);
            keys(row, Set.of("preserveId", "line", "before", "evidenceLimit"), path);
            preserved.add(new EditorialChangeMapReconstructor.PreservedRow(text(row, "preserveId", path + ".preserveId", phase),
                    line(row, path + ".line"), text(row, "before", path + ".before", phase),
                    text(row, "evidenceLimit", path + ".evidenceLimit", phase)));
        }
        Map<String, Object> disposition = object(root.get("disposition"), "disposition");
        keys(disposition, Set.of("disposition", "reasonCode", "stopClass"), "disposition");
        String kind = text(disposition, "disposition", "disposition.disposition", phase);
        String reason = text(disposition, "reasonCode", "disposition.reasonCode", phase);
        if (!EditorialP5RawWireContract.safeText(reason)) throw WireViolation.at("L2_WIRE_REASON_INVALID", "disposition.reasonCode");
        String stopClass = text(disposition, "stopClass", "disposition.stopClass", phase);
        if ("CONTINUE".equals(kind) || "PRESERVE_DRAFT".equals(kind)) {
            if (!"NONE".equals(stopClass)) throw WireViolation.at("L2_WIRE_STOP_CLASS_INVALID", "disposition.stopClass");
            return new Wire(List.copyOf(changes), List.copyOf(preserved), null, reason);
        }
        if (!"STOP".equals(kind)) throw WireViolation.at("L2_WIRE_DISPOSITION_INVALID", "disposition.disposition");
        // A model may only request a content stop or input; it cannot claim PASS or a technical class.
        return switch (stopClass) {
            case "CONTENT_BLOCKED" -> new Wire(List.of(), List.of(), StopClass.CONTENT_BLOCKED, reason);
            case "INPUT_REQUIRED" -> new Wire(List.of(), List.of(), StopClass.INPUT_REQUIRED, reason);
            default -> throw WireViolation.at("L2_WIRE_STOP_CLASS_INVALID", "disposition.stopClass");
        };
    }

    static void keys(Map<String, Object> value, Set<String> allowed, String path) {
        keys(value, allowed, path, Set.of());
    }

    static void keys(Map<String, Object> value, Set<String> allowed, String path, Set<String> optional) {
        for (String key : value.keySet()) {
            if (!allowed.contains(key)) throw WireViolation.at("L2_WIRE_UNKNOWN_KEY", path);
        }
        for (String key : allowed) {
            if (!optional.contains(key) && !value.containsKey(key)) {
                throw WireViolation.at("L2_WIRE_MISSING_KEY", path + "." + key);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map)) throw WireViolation.at("L2_WIRE_OBJECT_EXPECTED", path);
        return (Map<String, Object>) value;
    }

    static String text(Map<String, Object> value, String key) {
        return text(value, key, key);
    }

    static String text(Map<String, Object> value, String key, String path) {
        return text(value, key, path, EditorialFieldSpec.L2_EDIT);
    }

    static String text(Map<String, Object> value, String key, String path, String phase) {
        EditorialFieldSpec.Field spec = EditorialFieldSpec.find(phase, path);
        if (spec == null) {
            Object raw = value.get(key);
            if (!(raw instanceof String) || ((String) raw).length() > MAX_TEXT_FIELD) {
                throw WireViolation.at("L2_WIRE_TEXT_INVALID", path);
            }
            return (String) raw;
        }
        return EditorialFieldSpec.validateString(phase, path, value.get(key), "L2_WIRE_TEXT_INVALID",
                "L2_WIRE_TEXT_REQUIRED", "L2_WIRE_TEXT_INVALID", "L2_WIRE_TEXT_INVALID");
    }

    static boolean bool(Map<String, Object> value, String key) {
        return bool(value, key, key);
    }

    static boolean bool(Map<String, Object> value, String key, String path) {
        Object flag = value.get(key);
        if (!(flag instanceof Boolean)) throw WireViolation.at("L2_WIRE_BOOLEAN_INVALID", path);
        return (Boolean) flag;
    }

    static int line(Map<String, Object> value) {
        return line(value, "line");
    }

    static int line(Map<String, Object> value, String path) {
        Object number = value.get("line");
        if (!(number instanceof BigDecimal)) throw WireViolation.at("L2_WIRE_LINE_INVALID", path);
        try {
            return ((BigDecimal) number).intValueExact();
        } catch (ArithmeticException invalid) {
            throw WireViolation.at("L2_WIRE_LINE_INVALID", path);
        }
    }

    private static void recover(Store store, String attemptIdentity, String reason) {
        try {
            store.markRecoveryRequired(attemptIdentity, reason);
        } catch (RuntimeException ignored) {
            // The claim remains IN_FLIGHT and is never re-dispatched automatically.
        }
    }

    static String safeMessage(RuntimeException error) {
        return WireViolation.safeMessage(error, "L2_WIRE_PARSE_FAILED");
    }

    private static Result stop(StopClass stopClass, String reason, List<String> issues, int calls) {
        return new Result(Outcome.STOPPED, stopClass, reason, issues, null, calls);
    }

    /** Canonical map for diagnostics/tests; never sent to the provider. */
    static Map<String, Object> describe(Result result) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("outcome", result.outcome().name());
        value.put("reasonCode", result.reasonCode());
        value.put("providerCalls", BigDecimal.valueOf(result.providerCalls()));
        return value;
    }
}
