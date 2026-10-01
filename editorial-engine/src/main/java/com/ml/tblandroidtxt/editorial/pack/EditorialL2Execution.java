package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
    public static final String WIRE_SCHEMA_VERSION = "safe4.l2.edit.wire.v1";
    public static final String VI_L2_SCHEMA = "safe4.full.vi-l2.v1";
    public static final int MAX_WIRE_BYTES = 65_536;
    public static final int MAX_CHANGES = 200;
    public static final int MAX_PRESERVED = 100;
    public static final int MAX_TEXT_FIELD = 2_000;
    private static final String ATTEMPT_DOMAIN = "EDITORIAL_L2_EDIT_ATTEMPT_IDENTITY_V1\n";

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
                    + EditorialCanonicalJson.sha256Hex(reportL1Bytes) + "\n" + context.bundleIdentity();
            return EditorialCanonicalJson.sha256Hex((ATTEMPT_DOMAIN + value).getBytes(StandardCharsets.UTF_8));
        }
    }

    public Result execute(Request request, Budget budget, Provider provider, Store store) {
        if (request == null || request.context() == null) {
            return stop(StopClass.INPUT_REQUIRED, "INPUT_L2_REQUEST_MISSING", List.of(), 0);
        }
        if (budget == null || !budget.valid()) {
            return stop(StopClass.AUTHORIZATION_REQUIRED, "L2_BUDGET_REQUIRED", List.of(), 0);
        }
        EditorialP5PilotRequest context = request.context();
        String predecessorIssue = predecessorIssue(request);
        if (predecessorIssue != null) {
            return stop(StopClass.INPUT_REQUIRED, predecessorIssue, List.of(), 0);
        }
        EditorialPhaseContextProjector.PhaseProjection projection;
        try {
            projection = project(request);
        } catch (RuntimeException invalid) {
            return stop(StopClass.INPUT_REQUIRED, "INPUT_L2_PROJECTION_INVALID", List.of(), 0);
        }
        long inputBytes = 0L;
        Map<String, byte[]> visible = new TreeMap<>();
        for (Map.Entry<String, EditorialPhaseContextProjector.BundleAsset> entry
                : projection.visibleAssets().entrySet()) {
            byte[] bytes = entry.getValue().bytes();
            visible.put(entry.getKey(), bytes);
            inputBytes += bytes == null ? 0 : bytes.length;
        }
        if (inputBytes > budget.maximumInputBytes()) {
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

        Provider.Response response;
        try {
            response = provider.call(new Provider.Request(attemptIdentity, PHASE, WIRE_SCHEMA_VERSION,
                    Collections.unmodifiableMap(visible), context.authority(), context.chapterKey(),
                    budget.maximumOutputTokens(), budget.maximumExecutionTimeMillis()));
        } catch (Exception error) {
            recover(store, attemptIdentity, "RETRY_L2_PROVIDER_CALL_FAILED");
            return stop(StopClass.RETRY_REQUIRED, "RETRY_L2_PROVIDER_CALL_FAILED", List.of(), 1);
        }
        if (response == null || response.responseBytes() == null) {
            recover(store, attemptIdentity, "RETRY_L2_PROVIDER_EMPTY_RESPONSE");
            return stop(StopClass.RETRY_REQUIRED, "RETRY_L2_PROVIDER_EMPTY_RESPONSE", List.of(), 1);
        }
        String finish = response.finishReason() == null ? "" : response.finishReason().toLowerCase(java.util.Locale.ROOT);
        if (!response.transportComplete() || "length".equals(finish) || "max_tokens".equals(finish)
                || "max_output_tokens".equals(finish)) {
            recover(store, attemptIdentity, "RETRY_L2_OUTPUT_TRUNCATED");
            return stop(StopClass.RETRY_REQUIRED, "RETRY_L2_OUTPUT_TRUNCATED", List.of(), 1);
        }
        if (!response.costKnown()) {
            recover(store, attemptIdentity, "RETRY_L2_PROVIDER_COST_UNAVAILABLE");
            return stop(StopClass.RETRY_REQUIRED, "RETRY_L2_PROVIDER_COST_UNAVAILABLE", List.of(), 1);
        }
        if (response.outputTokens() > budget.maximumOutputTokens()
                || response.cost().compareTo(budget.maximumCost()) > 0) {
            recover(store, attemptIdentity, "L2_TOKEN_OR_COST_BUDGET_EXCEEDED");
            return stop(StopClass.BUDGET_EXCEEDED, "L2_TOKEN_OR_COST_BUDGET_EXCEEDED", List.of(), 1);
        }

        Wire wire;
        try {
            wire = parseWire(response.responseBytes(), attemptIdentity);
        } catch (RuntimeException invalid) {
            recover(store, attemptIdentity, "REPAIR_L2_OUTPUT_SCHEMA_INVALID");
            return stop(StopClass.REPAIR_REQUIRED, "REPAIR_L2_OUTPUT_SCHEMA_INVALID",
                    List.of(safeMessage(invalid)), 1);
        }
        if (wire.stopClass() != null) {
            recover(store, attemptIdentity, wire.reasonCode());
            return stop(wire.stopClass(), wire.reasonCode(), List.of(), 1);
        }

        EditorialChangeMapReconstructor.Result reconstruction = new EditorialChangeMapReconstructor()
                .reconstruct(new EditorialChangeMapReconstructor.Request(
                        EditorialChangeMapReconstructor.Kind.L2_DRAFT_TO_VI_L2,
                        context.source(EditorialSafe4Contract.DRAFT).bytes(),
                        request.reportL1AttemptIdentity(), wire.changes(), wire.preserved(),
                        request.protectedLineNumbers()));
        if (!reconstruction.accepted()) {
            StopClass stopClass = reconstruction.status() == EditorialChangeMapReconstructor.Status.INPUT_REQUIRED
                    ? StopClass.INPUT_REQUIRED : StopClass.REPAIR_REQUIRED;
            String reason = stopClass == StopClass.INPUT_REQUIRED
                    ? "INPUT_L2_BASE_INVALID" : "REPAIR_L2_CHANGE_MAP_INVALID";
            recover(store, attemptIdentity, reason);
            return stop(stopClass, reason, reconstruction.issues(), 1);
        }

        byte[] viL2 = reconstruction.outputBytes();
        byte[] changeMap = reconstruction.changeMapBytes();
        Committed committed = new Committed(attemptIdentity, request.reportL1AttemptIdentity(),
                bundleIdentity, viL2, EditorialCanonicalJson.sha256Hex(viL2), changeMap,
                EditorialCanonicalJson.sha256Hex(changeMap));
        try {
            store.commit(committed);
        } catch (RuntimeException error) {
            recover(store, attemptIdentity, "RETRY_L2_ATOMIC_COMMIT_FAILED");
            return stop(StopClass.RETRY_REQUIRED, "RETRY_L2_ATOMIC_COMMIT_FAILED", List.of(), 1);
        }
        Optional<Committed> readback = store.findCommitted(attemptIdentity);
        if (readback.isEmpty() || !readback.get().viL2Sha256().equals(committed.viL2Sha256())
                || !readback.get().changeMapSha256().equals(committed.changeMapSha256())) {
            return stop(StopClass.RETRY_REQUIRED, "RETRY_L2_READBACK_MISMATCH", List.of(), 1);
        }
        return new Result(Outcome.COMMITTED, null, "L2_COMMITTED", List.of(), readback.get(), 1);
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
        if (!"REPORT_L1".equals(report.get("artifactType"))
                || !EditorialP5RawWireContract.FINAL_REPORT_SCHEMA.equals(report.get("schemaVersion"))
                || !"L1_RECONCILE".equals(report.get("phase"))) {
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

    private static EditorialPhaseContextProjector.PhaseProjection project(Request request) {
        EditorialPhaseContextProjector.Bundle l1 = request.context().bundleForExecution();
        List<EditorialPhaseContextProjector.BundleAsset> assets = new ArrayList<>(l1.assets());
        assets.add(new EditorialPhaseContextProjector.BundleAsset("REPORT_L1",
                "report-l1:" + request.reportL1AttemptIdentity(), request.reportL1Bytes(), true,
                EditorialP5RawWireContract.FINAL_REPORT_SCHEMA));
        return new EditorialPhaseContextProjector().project(
                new EditorialPhaseContextProjector.Bundle(assets, l1.pronounStatus()), PHASE);
    }

    record Wire(List<EditorialChangeMapReconstructor.ChangeRow> changes,
                List<EditorialChangeMapReconstructor.PreservedRow> preserved,
                StopClass stopClass, String reasonCode) { }

    /** Strict compact wire parser; unknown keys, oversize values and wrong echoes are rejected. */
    static Wire parseWire(byte[] bytes, String attemptIdentity) {
        if (bytes.length > MAX_WIRE_BYTES) throw new IllegalArgumentException("L2_WIRE_BYTE_LIMIT_EXCEEDED");
        Map<String, Object> root = EditorialCanonicalJson.parseObject(bytes);
        keys(root, Set.of("wireSchemaVersion", "attemptIdentity", "changes", "preserved", "disposition"), "root");
        if (!WIRE_SCHEMA_VERSION.equals(root.get("wireSchemaVersion"))) {
            throw new IllegalArgumentException("L2_WIRE_SCHEMA_INVALID");
        }
        if (!attemptIdentity.equals(root.get("attemptIdentity"))) {
            throw new IllegalArgumentException("L2_WIRE_ATTEMPT_ECHO_MISMATCH");
        }
        List<Object> changeValues = EditorialCanonicalJson.array(root.get("changes"), "changes");
        List<Object> preservedValues = EditorialCanonicalJson.array(root.get("preserved"), "preserved");
        if (changeValues.size() > MAX_CHANGES || preservedValues.size() > MAX_PRESERVED) {
            throw new IllegalArgumentException("L2_WIRE_ROW_LIMIT_EXCEEDED");
        }
        List<EditorialChangeMapReconstructor.ChangeRow> changes = new ArrayList<>();
        for (Object value : changeValues) {
            Map<String, Object> row = object(value, "change");
            keys(row, Set.of("changeId", "errorId", "line", "before", "after", "reason", "dialogue",
                    "speakerProof", "status"), "change", Set.of("speakerProof"));
            EditorialChangeMapReconstructor.SpeakerProof proof = null;
            if (row.containsKey("speakerProof")) {
                Map<String, Object> p = object(row.get("speakerProof"), "speakerProof");
                keys(p, Set.of("speaker", "listener", "anchorBefore", "anchorAfter"), "speakerProof");
                proof = new EditorialChangeMapReconstructor.SpeakerProof(text(p, "speaker"),
                        text(p, "listener"), text(p, "anchorBefore"), text(p, "anchorAfter"));
            }
            EditorialChangeMapReconstructor.DeclaredStatus status;
            try {
                status = EditorialChangeMapReconstructor.DeclaredStatus.valueOf(text(row, "status"));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("L2_WIRE_STATUS_INVALID");
            }
            changes.add(new EditorialChangeMapReconstructor.ChangeRow(text(row, "changeId"),
                    text(row, "errorId"), line(row), text(row, "before"), text(row, "after"),
                    text(row, "reason"), bool(row, "dialogue"), proof, status));
        }
        List<EditorialChangeMapReconstructor.PreservedRow> preserved = new ArrayList<>();
        for (Object value : preservedValues) {
            Map<String, Object> row = object(value, "preserved");
            keys(row, Set.of("preserveId", "line", "before", "evidenceLimit"), "preserved");
            preserved.add(new EditorialChangeMapReconstructor.PreservedRow(text(row, "preserveId"),
                    line(row), text(row, "before"), text(row, "evidenceLimit")));
        }
        Map<String, Object> disposition = object(root.get("disposition"), "disposition");
        keys(disposition, Set.of("disposition", "reasonCode", "stopClass"), "disposition");
        String kind = text(disposition, "disposition");
        String reason = text(disposition, "reasonCode");
        if (!EditorialP5RawWireContract.safeText(reason)) throw new IllegalArgumentException("L2_WIRE_REASON_INVALID");
        String stopClass = text(disposition, "stopClass");
        if ("CONTINUE".equals(kind) || "PRESERVE_DRAFT".equals(kind)) {
            if (!"NONE".equals(stopClass)) throw new IllegalArgumentException("L2_WIRE_STOP_CLASS_INVALID");
            return new Wire(List.copyOf(changes), List.copyOf(preserved), null, reason);
        }
        if (!"STOP".equals(kind)) throw new IllegalArgumentException("L2_WIRE_DISPOSITION_INVALID");
        // A model may only request a content stop or input; it cannot claim PASS or a technical class.
        return switch (stopClass) {
            case "CONTENT_BLOCKED" -> new Wire(List.of(), List.of(), StopClass.CONTENT_BLOCKED, reason);
            case "INPUT_REQUIRED" -> new Wire(List.of(), List.of(), StopClass.INPUT_REQUIRED, reason);
            default -> throw new IllegalArgumentException("L2_WIRE_STOP_CLASS_INVALID");
        };
    }

    private static void keys(Map<String, Object> value, Set<String> allowed, String path) {
        keys(value, allowed, path, Set.of());
    }

    private static void keys(Map<String, Object> value, Set<String> allowed, String path, Set<String> optional) {
        for (String key : value.keySet()) {
            if (!allowed.contains(key)) throw new IllegalArgumentException("L2_WIRE_UNKNOWN_KEY:" + path);
        }
        for (String key : allowed) {
            if (!optional.contains(key) && !value.containsKey(key)) {
                throw new IllegalArgumentException("L2_WIRE_MISSING_KEY:" + path);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map)) throw new IllegalArgumentException("L2_WIRE_OBJECT_EXPECTED:" + path);
        return (Map<String, Object>) value;
    }

    private static String text(Map<String, Object> value, String key) {
        Object text = value.get(key);
        if (!(text instanceof String) || ((String) text).length() > MAX_TEXT_FIELD) {
            throw new IllegalArgumentException("L2_WIRE_TEXT_INVALID:" + key);
        }
        return (String) text;
    }

    private static boolean bool(Map<String, Object> value, String key) {
        Object flag = value.get(key);
        if (!(flag instanceof Boolean)) throw new IllegalArgumentException("L2_WIRE_BOOLEAN_INVALID:" + key);
        return (Boolean) flag;
    }

    private static int line(Map<String, Object> value) {
        Object number = value.get("line");
        if (!(number instanceof BigDecimal)) throw new IllegalArgumentException("L2_WIRE_LINE_INVALID");
        try {
            return ((BigDecimal) number).intValueExact();
        } catch (ArithmeticException invalid) {
            throw new IllegalArgumentException("L2_WIRE_LINE_INVALID");
        }
    }

    private static void recover(Store store, String attemptIdentity, String reason) {
        try {
            store.markRecoveryRequired(attemptIdentity, reason);
        } catch (RuntimeException ignored) {
            // The claim remains IN_FLIGHT and is never re-dispatched automatically.
        }
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message != null && message.matches("[A-Z0-9_:./-]{1,96}") ? message : "L2_WIRE_PARSE_FAILED";
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
