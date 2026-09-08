package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotExecution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite owner for one bounded P5C attempt. It persists identities, redacted
 * report/receipt bytes, metrics and recovery disposition; it never persists
 * chapter bytes, request bodies or provider responses.
 */
public final class EditorialP5CAttemptStore implements EditorialP5PilotExecution.AttemptStore {
    private static final String TABLE = "editorial_p5c_attempts";
    private static final String LIFECYCLE_TABLE = "editorial_p5d_network_lifecycle";
    private static final String AUTH_TABLE = "editorial_p5d_authorization_receipts";
    private static final String RECONCILIATION_TABLE = "editorial_p5d_reconciliation";

    private final TranslationRepository database;
    private final Map<String, Pending> pending = new HashMap<>();

    public EditorialP5CAttemptStore(TranslationRepository database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /**
     * Supplies the exact request facts before the engine claims the attempt.
     * This is deliberately not a general-purpose request cache.
     */
    @Override public synchronized void prepare(EditorialP5PilotRequest request,
                                                EditorialP5PilotAuthorization authorization,
                                                String requestEnvelopeHash) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(authorization, "authorization");
        requireHash(requestEnvelopeHash, "request envelope hash");
        Pending value = new Pending(request.requestIdentity(), request.binding().bindingIdentity(),
                request.binding().runDeclarationIdentity(), request.chapterKey(), request.phase(),
                request.predecessorIdentity(), requestEnvelopeHash, authorization.provider(),
                authorization.model(), authorization);
        Pending old = pending.get(request.attemptIdentity());
        if (old != null && !old.matches(value)) {
            throw new IllegalStateException("P5C attempt identity collision");
        }
        // A new authorization is allowed to replace only the in-memory auth
        // facts for an exact recovery retry; durable request facts remain fixed.
        pending.put(request.attemptIdentity(), value);
    }

    @Override public synchronized Claim claim(String attemptIdentity) {
        requireHash(attemptIdentity, "attempt identity");
        Row existing = findRow(attemptIdentity);
        if (existing != null) {
            Pending facts = pending.get(attemptIdentity);
            if (facts != null && !facts.matches(existing)) {
                throw new IllegalStateException("P5C attempt identity facts changed");
            }
            if ("COMMITTED".equals(existing.status)) return Claim.ALREADY_COMMITTED;
            if ("CLAIMED".equals(existing.status)) return Claim.IN_FLIGHT;
            if ("RECOVERY_REQUIRED".equals(existing.status)) {
                return reclaimOnlyAfterReconciliation(attemptIdentity, facts);
            }
            throw new IllegalStateException("P5C attempt status is invalid");
        }

        Pending facts = pending.get(attemptIdentity);
        if (facts == null) throw new IllegalStateException("P5C attempt facts were not prepared");
        SQLiteDatabase db = database.editorialWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("attempt_identity", attemptIdentity);
        values.put("request_identity", facts.requestIdentity);
        values.put("binding_identity", facts.bindingIdentity);
        values.put("run_declaration_identity", facts.runDeclarationIdentity);
        values.put("chapter_key", facts.chapterKey);
        values.put("phase", facts.phase);
        values.put("predecessor_identity", facts.predecessorIdentity);
        values.put("request_envelope_hash", facts.requestEnvelopeHash);
        values.put("provider", facts.provider);
        values.put("model", facts.model);
        values.put("status", "CLAIMED");
        values.put("response_identity", "");
        values.putNull("report_bytes");
        values.putNull("receipt_bytes");
        values.put("metrics_json", "");
        values.put("recovery_reason_code", "");
        values.put("created_at", now);
        values.put("updated_at", now);
        boolean inserted = false;
        boolean authorizationUsed = false;
        db.beginTransaction();
        try {
            db.insertOrThrow(TABLE, null, values);
            insertConsumedAuthorizationReceipt(db, attemptIdentity, facts);
            db.setTransactionSuccessful();
            inserted = true;
        } catch (AuthorizationAlreadyConsumed used) {
            authorizationUsed = true;
        } catch (SQLiteConstraintException race) {
            // The transaction is rolled back below. The durable row is
            // classified after the insert race without consuming an auth.
        } finally {
            db.endTransaction();
        }
        if (authorizationUsed) return Claim.AUTHORIZATION_USED;
        if (inserted) return Claim.ACQUIRED;
        Row raced = findRow(attemptIdentity);
        if (raced != null && facts.matches(raced)) {
            if ("COMMITTED".equals(raced.status)) return Claim.ALREADY_COMMITTED;
            if ("CLAIMED".equals(raced.status)) return Claim.IN_FLIGHT;
            if ("RECOVERY_REQUIRED".equals(raced.status)) return Claim.RECOVERY_REQUIRED;
        }
        throw new IllegalStateException("P5C attempt claim collision");
    }

    @Override public synchronized void commit(EditorialP5PilotResult.CommittedResult value) {
        Objects.requireNonNull(value, "committed result");
        if (value.output() == null) throw new IllegalArgumentException(
                "fresh P5C commit requires semantic output");
        if (value.reportBytes().length == 0 || value.receiptBytes().length == 0) {
            throw new IllegalArgumentException("P5C commit requires report and receipt");
        }
        Row current = findRow(value.attemptIdentity());
        Pending facts = pending.get(value.attemptIdentity());
        if (current == null || facts == null || !facts.matches(current)
                || !"CLAIMED".equals(current.status)) {
            throw new IllegalStateException("P5C attempt is not the claimed exact attempt");
        }
        if (!value.requestIdentity().equals(current.requestIdentity)) {
            throw new IllegalStateException("P5C request identity changed before commit");
        }
        SQLiteDatabase db = database.editorialWritableDatabase();
        ContentValues update = new ContentValues();
        update.put("status", "COMMITTED");
        update.put("response_identity", value.responseIdentity());
        update.put("report_bytes", value.reportBytes());
        update.put("receipt_bytes", value.receiptBytes());
        update.put("metrics_json", encodeMetrics(value.metrics()));
        update.put("recovery_reason_code", "");
        update.put("updated_at", System.currentTimeMillis());
        if (db.update(TABLE, update, "attempt_identity=? AND status='CLAIMED'",
                new String[]{value.attemptIdentity()}) != 1) {
            throw new IllegalStateException("P5C atomic result commit lost the claim");
        }
        pending.remove(value.attemptIdentity());
    }

    @Override public synchronized Optional<EditorialP5PilotResult.CommittedResult> findCommitted(
            String attemptIdentity) {
        Row row = findRow(attemptIdentity);
        if (row == null || !"COMMITTED".equals(row.status)) return Optional.empty();
        if (row.responseIdentity.isBlank() || row.reportBytes.length == 0
                || row.receiptBytes.length == 0 || row.metricsJson.isBlank()) return Optional.empty();
        try {
            return Optional.of(EditorialP5PilotResult.CommittedResult.persisted(
                    row.attemptIdentity, row.requestIdentity, row.responseIdentity,
                    row.reportBytes, row.receiptBytes, decodeMetrics(row.metricsJson)));
        } catch (RuntimeException invalid) {
            return Optional.empty();
        }
    }

    @Override public synchronized void markRecoveryRequired(String attemptIdentity,
                                                              String reasonCode) {
        requireHash(attemptIdentity, "attempt identity");
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("recovery reason code is required");
        }
        SQLiteDatabase db = database.editorialWritableDatabase();
        ContentValues update = new ContentValues();
        update.put("status", "RECOVERY_REQUIRED");
        update.put("recovery_reason_code", reasonCode);
        update.put("updated_at", System.currentTimeMillis());
        db.update(TABLE, update, "attempt_identity=? AND status<>'COMMITTED'",
                new String[]{attemptIdentity});
    }

    /** Redacted transport lifecycle stages; no request/response content is accepted. */
    public enum LifecycleStage {
        CALL_CREATED,
        REQUEST_BODY_STARTED,
        REQUEST_BODY_SENT,
        RESPONSE_HEADERS_RECEIVED,
        RESPONSE_BODY_COMPLETE,
        CALL_CANCELLED,
        CALL_FAILED
    }

    public record NetworkLifecycleEvent(LifecycleStage stage, long requestBodyBytes,
                                        int httpStatus, String exceptionClass,
                                        long elapsedMillis, String generationId,
                                        String providerResponseId) {
        public NetworkLifecycleEvent {
            Objects.requireNonNull(stage, "lifecycle stage");
            if (requestBodyBytes < 0) throw new IllegalArgumentException("request bytes cannot be negative");
            if (httpStatus < -1 || httpStatus > 599) throw new IllegalArgumentException("invalid HTTP status");
            if (elapsedMillis < 0) throw new IllegalArgumentException("elapsed time cannot be negative");
            exceptionClass = bounded(exceptionClass, "exception class");
            generationId = bounded(generationId, "generation id");
            providerResponseId = bounded(providerResponseId, "provider response id");
            if (!allowedExceptionClass(exceptionClass)) {
                throw new IllegalArgumentException("exception class is not allowlisted");
            }
        }

        public static NetworkLifecycleEvent stage(LifecycleStage stage) {
            return new NetworkLifecycleEvent(stage, 0L, -1, "", 0L, "", "");
        }
    }

    public record AuthorizationReceipt(String authorizationIdHash, String exactPhase,
                                       String attemptIdentity, String requestIdentity,
                                       String bindingIdentity, String runDeclarationIdentity,
                                       String chapterKey, String provider, String model,
                                       String endpointAccountFingerprint, long issuedAt,
                                       long expiresAt, int maximumPrimaryCalls,
                                       int maximumSchemaRepairCalls, int maximumNetworkRetries,
                                       int maximumInputTokens, int maximumOutputTokens,
                                       int maximumTotalTokens, BigDecimal maximumTotalCost,
                                       long maximumExecutionTimeMillis, long consumedAt,
                                       String consumptionResult,
                                       String consumedAttemptIdentity) {
        public AuthorizationReceipt {
            requireHash(authorizationIdHash, "authorization id hash");
            requireHash(attemptIdentity, "attempt identity");
            requireHash(requestIdentity, "request identity");
            requireHash(bindingIdentity, "binding identity");
            requireHash(runDeclarationIdentity, "run declaration identity");
            exactPhase = text(exactPhase, "exact phase");
            chapterKey = text(chapterKey, "chapter key");
            provider = text(provider, "provider");
            model = text(model, "model");
            endpointAccountFingerprint = text(endpointAccountFingerprint,
                    "endpoint/account fingerprint");
            maximumTotalCost = Objects.requireNonNull(maximumTotalCost, "maximum total cost");
            consumptionResult = text(consumptionResult, "consumption result");
            consumedAttemptIdentity = consumedAttemptIdentity == null ? "" : consumedAttemptIdentity;
        }
    }

    public enum ExternalStateClassification {
        EXTERNAL_CONFIRMED_FAILED,
        EXTERNAL_CONFIRMED_CANCELLED,
        EXTERNAL_COMPLETED_BILLED_OUTPUT_UNAVAILABLE,
        EXTERNAL_COMPLETED_METADATA_AVAILABLE,
        EXTERNAL_NOT_FOUND_AFTER_BOUNDED_AUDIT,
        EXTERNAL_STATE_REMAINS_UNKNOWN
    }

    public record ReconciliationDecision(String attemptIdentity,
                                         ExternalStateClassification classification,
                                         String evidenceRef,
                                         String endpointAccountFingerprint,
                                         String billingState,
                                         String decidedBy,
                                         boolean retryEligible,
                                         boolean duplicateRiskAcknowledged,
                                         String newAuthorizationIdHash,
                                         long decidedAt) {
        public ReconciliationDecision {
            requireHash(attemptIdentity, "attempt identity");
            Objects.requireNonNull(classification, "external state classification");
            evidenceRef = text(evidenceRef, "evidence reference");
            endpointAccountFingerprint = text(endpointAccountFingerprint,
                    "endpoint/account fingerprint");
            billingState = text(billingState, "billing state");
            decidedBy = text(decidedBy, "decision authority");
            newAuthorizationIdHash = newAuthorizationIdHash == null ? "" : newAuthorizationIdHash;
            if (!newAuthorizationIdHash.isEmpty()) {
                requireHash(newAuthorizationIdHash, "new authorization id hash");
            }
            if (decidedAt < 0) throw new IllegalArgumentException("decision time cannot be negative");
            if (retryEligible && newAuthorizationIdHash.isEmpty()) {
                throw new IllegalArgumentException("retry requires a new authorization hash");
            }
            if (retryEligible && !duplicateRiskAcknowledged) {
                throw new IllegalArgumentException("retry requires duplicate-risk acknowledgement");
            }
        }
    }

    public record ReconciliationRecord(String attemptIdentity,
                                       ExternalStateClassification classification,
                                       String evidenceRef,
                                       String endpointAccountFingerprint,
                                       String billingState, String decidedBy,
                                       boolean retryEligible,
                                       boolean duplicateRiskAcknowledged,
                                       String newAuthorizationIdHash, long decidedAt) { }

    public static String authorizationIdHash(String authorizationId) {
        if (authorizationId == null || authorizationId.isBlank()) {
            throw new IllegalArgumentException("authorization id is required");
        }
        return EditorialCanonicalJson.sha256Hex(authorizationId.getBytes(StandardCharsets.UTF_8));
    }

    /** Upserts only the latest allowlisted lifecycle metadata for an existing attempt. */
    public synchronized void recordNetworkLifecycle(String attemptIdentity,
                                                     NetworkLifecycleEvent event) {
        requireHash(attemptIdentity, "attempt identity");
        Objects.requireNonNull(event, "network lifecycle event");
        if (findRow(attemptIdentity) == null) throw new IllegalStateException(
                "P5D lifecycle requires a durable attempt");
        SQLiteDatabase db = database.editorialWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("attempt_identity", attemptIdentity);
        values.put("stage", event.stage().name());
        values.put("request_body_bytes", event.requestBodyBytes());
        values.put("http_status", event.httpStatus());
        values.put("exception_class", event.exceptionClass());
        values.put("elapsed_ms", event.elapsedMillis());
        values.put("generation_id", event.generationId());
        values.put("provider_response_id", event.providerResponseId());
        values.put("updated_at", System.currentTimeMillis());
        if (db.update(LIFECYCLE_TABLE, values, "attempt_identity=?",
                new String[]{attemptIdentity}) == 0) {
            db.insertOrThrow(LIFECYCLE_TABLE, null, values);
        }
    }

    public synchronized Optional<NetworkLifecycleEvent> findNetworkLifecycle(String attemptIdentity) {
        requireHash(attemptIdentity, "attempt identity");
        String sql = "SELECT stage,request_body_bytes,http_status,exception_class,elapsed_ms,"
                + "generation_id,provider_response_id FROM " + LIFECYCLE_TABLE
                + " WHERE attempt_identity=?";
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(sql,
                new String[]{attemptIdentity})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(new NetworkLifecycleEvent(
                    LifecycleStage.valueOf(cursor.getString(0)), cursor.getLong(1), cursor.getInt(2),
                    safe(cursor.getString(3)), cursor.getLong(4), safe(cursor.getString(5)),
                    safe(cursor.getString(6))));
        }
    }

    /** Records exactly one immutable external-state decision for an attempt. */
    public synchronized void recordReconciliation(ReconciliationDecision decision) {
        Objects.requireNonNull(decision, "reconciliation decision");
        if (findRow(decision.attemptIdentity()) == null) throw new IllegalStateException(
                "P5D reconciliation requires a durable attempt");
        Optional<ReconciliationRecord> existing = findReconciliation(decision.attemptIdentity());
        if (existing.isPresent()) {
            ReconciliationRecord value = existing.get();
            if (value.classification() == decision.classification()
                    && value.evidenceRef().equals(decision.evidenceRef())
                    && value.endpointAccountFingerprint().equals(decision.endpointAccountFingerprint())
                    && value.billingState().equals(decision.billingState())
                    && value.decidedBy().equals(decision.decidedBy())
                    && value.retryEligible() == decision.retryEligible()
                    && value.duplicateRiskAcknowledged() == decision.duplicateRiskAcknowledged()
                    && value.newAuthorizationIdHash().equals(decision.newAuthorizationIdHash())
                    && value.decidedAt() == decision.decidedAt()) return;
            throw new IllegalStateException("P5D reconciliation decision is immutable");
        }
        SQLiteDatabase db = database.editorialWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("attempt_identity", decision.attemptIdentity());
        values.put("classification", decision.classification().name());
        values.put("evidence_ref", decision.evidenceRef());
        values.put("endpoint_account_fingerprint", decision.endpointAccountFingerprint());
        values.put("billing_state", decision.billingState());
        values.put("decided_by", decision.decidedBy());
        values.put("retry_eligible", decision.retryEligible() ? 1 : 0);
        values.put("duplicate_risk_acknowledged", decision.duplicateRiskAcknowledged() ? 1 : 0);
        values.put("new_authorization_id_hash", decision.newAuthorizationIdHash());
        values.put("decided_at", decision.decidedAt());
        db.insertOrThrow(RECONCILIATION_TABLE, null, values);
    }

    public synchronized Optional<ReconciliationRecord> findReconciliation(String attemptIdentity) {
        requireHash(attemptIdentity, "attempt identity");
        String sql = "SELECT classification,evidence_ref,endpoint_account_fingerprint,"
                + "billing_state,decided_by,retry_eligible,duplicate_risk_acknowledged,"
                + "new_authorization_id_hash,decided_at FROM " + RECONCILIATION_TABLE
                + " WHERE attempt_identity=?";
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(sql,
                new String[]{attemptIdentity})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(new ReconciliationRecord(attemptIdentity,
                    ExternalStateClassification.valueOf(cursor.getString(0)),
                    cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4),
                    cursor.getInt(5) != 0, cursor.getInt(6) != 0, cursor.getString(7),
                    cursor.getLong(8)));
        }
    }

    public synchronized Optional<AuthorizationReceipt> findAuthorizationReceipt(
            String authorizationIdHash) {
        requireHash(authorizationIdHash, "authorization id hash");
        return readAuthorizationReceipt(database.editorialReadableDatabase(), authorizationIdHash);
    }

    private Claim reclaimOnlyAfterReconciliation(String attemptIdentity, Pending facts) {
        if (facts == null) return Claim.RECOVERY_REQUIRED;
        Optional<ReconciliationRecord> decision = findReconciliation(attemptIdentity);
        String newAuthorizationHash = authorizationIdHash(facts.authorization.authorizationId());
        if (decision.isEmpty() || !decision.get().retryEligible()
                || !newAuthorizationHash.equals(decision.get().newAuthorizationIdHash())) {
            return Claim.RECOVERY_REQUIRED;
        }
        SQLiteDatabase db = database.editorialWritableDatabase();
        db.beginTransaction();
        try {
            if (readAuthorizationReceipt(db, newAuthorizationHash).isPresent()) {
                return Claim.AUTHORIZATION_USED;
            }
            insertConsumedAuthorizationReceipt(db, attemptIdentity, facts);
            ContentValues update = new ContentValues();
            update.put("status", "CLAIMED");
            update.put("recovery_reason_code", "");
            update.put("updated_at", System.currentTimeMillis());
            if (db.update(TABLE, update, "attempt_identity=? AND status='RECOVERY_REQUIRED'",
                    new String[]{attemptIdentity}) != 1) {
                return Claim.RECOVERY_REQUIRED;
            }
            db.setTransactionSuccessful();
            return Claim.ACQUIRED;
        } catch (AuthorizationAlreadyConsumed error) {
            return Claim.AUTHORIZATION_USED;
        } finally {
            db.endTransaction();
        }
    }

    private static void insertConsumedAuthorizationReceipt(SQLiteDatabase db,
                                                            String attemptIdentity,
                                                            Pending facts) {
        EditorialP5PilotAuthorization authorization = facts.authorization;
        String authorizationHash = authorizationIdHash(authorization.authorizationId());
        if (readAuthorizationReceipt(db, authorizationHash).isPresent()) {
            throw new AuthorizationAlreadyConsumed();
        }
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("authorization_id_hash", authorizationHash);
        values.put("exact_phase", facts.phase);
        values.put("attempt_identity", attemptIdentity);
        values.put("request_identity", facts.requestIdentity);
        values.put("binding_identity", facts.bindingIdentity);
        values.put("run_declaration_identity", facts.runDeclarationIdentity);
        values.put("chapter_key", facts.chapterKey);
        values.put("provider", authorization.provider());
        values.put("model", authorization.model());
        values.put("endpoint_account_fingerprint", authorization.endpointAccountFingerprint());
        values.put("issued_at", authorization.issuedAtMillis());
        values.put("expires_at", authorization.expiresAtMillis());
        values.put("maximum_primary_calls", authorization.maximumPrimarySemanticCalls());
        values.put("maximum_schema_repair_calls", authorization.maximumSchemaRepairCalls());
        values.put("maximum_network_retries", authorization.maximumNetworkRetries());
        values.put("maximum_input_tokens", authorization.maximumInputTokens());
        values.put("maximum_output_tokens", authorization.maximumOutputTokens());
        values.put("maximum_total_tokens", authorization.maximumTotalTokens());
        values.put("maximum_total_cost", authorization.maximumTotalCost().toPlainString());
        values.put("maximum_execution_time_ms", authorization.maximumExecutionTimeMillis());
        values.put("consumed_at", now);
        values.put("consumption_result", "CONSUMED");
        values.put("consumed_attempt_identity", attemptIdentity);
        values.put("created_at", now);
        try {
            db.insertOrThrow(AUTH_TABLE, null, values);
        } catch (SQLiteConstraintException error) {
            throw new AuthorizationAlreadyConsumed();
        }
    }

    private static Optional<AuthorizationReceipt> readAuthorizationReceipt(SQLiteDatabase db,
                                                                              String hash) {
        String sql = "SELECT authorization_id_hash,exact_phase,attempt_identity,request_identity,"
                + "binding_identity,run_declaration_identity,chapter_key,provider,model,"
                + "endpoint_account_fingerprint,issued_at,expires_at,maximum_primary_calls,"
                + "maximum_schema_repair_calls,maximum_network_retries,maximum_input_tokens,"
                + "maximum_output_tokens,maximum_total_tokens,maximum_total_cost,"
                + "maximum_execution_time_ms,consumed_at,consumption_result,"
                + "consumed_attempt_identity FROM " + AUTH_TABLE
                + " WHERE authorization_id_hash=?";
        try (Cursor cursor = db.rawQuery(sql, new String[]{hash})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(new AuthorizationReceipt(cursor.getString(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5),
                    cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9),
                    cursor.getLong(10), cursor.getLong(11), cursor.getInt(12), cursor.getInt(13),
                    cursor.getInt(14), cursor.getInt(15), cursor.getInt(16), cursor.getInt(17),
                    new BigDecimal(cursor.getString(18)), cursor.getLong(19), cursor.getLong(20),
                    cursor.getString(21), cursor.getString(22)));
        }
    }

    /** Redacted readback used by acceptance tests and local evidence. */
    public synchronized Optional<AttemptRecord> findRecord(String attemptIdentity) {
        Row row = findRow(attemptIdentity);
        if (row == null) return Optional.empty();
        return Optional.of(new AttemptRecord(row.attemptIdentity, row.requestIdentity,
                row.bindingIdentity, row.runDeclarationIdentity, row.chapterKey, row.phase,
                row.predecessorIdentity, row.requestEnvelopeHash, row.provider, row.model,
                row.status, row.responseIdentity, row.reportBytes, row.receiptBytes,
                row.metricsJson, row.recoveryReasonCode, row.createdAt, row.updatedAt));
    }

    public synchronized int count() {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + TABLE, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public record AttemptRecord(String attemptIdentity, String requestIdentity,
                                String bindingIdentity, String runDeclarationIdentity,
                                String chapterKey, String phase, String predecessorIdentity,
                                String requestEnvelopeHash, String provider, String model,
                                String status, String responseIdentity, byte[] reportBytes,
                                byte[] receiptBytes, String metricsJson,
                                String recoveryReasonCode, long createdAt, long updatedAt) {
        public AttemptRecord {
            reportBytes = reportBytes == null ? new byte[0] : reportBytes.clone();
            receiptBytes = receiptBytes == null ? new byte[0] : receiptBytes.clone();
        }

        @Override public byte[] reportBytes() { return reportBytes.clone(); }
        @Override public byte[] receiptBytes() { return receiptBytes.clone(); }
    }

    private Row findRow(String attemptIdentity) {
        if (attemptIdentity == null || attemptIdentity.isBlank()) return null;
        String sql = "SELECT attempt_identity,request_identity,binding_identity,"
                + "run_declaration_identity,chapter_key,phase,predecessor_identity,"
                + "request_envelope_hash,provider,model,status,response_identity,report_bytes,"
                + "receipt_bytes,metrics_json,recovery_reason_code,created_at,updated_at "
                + "FROM " + TABLE + " WHERE attempt_identity=?";
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(sql,
                new String[]{attemptIdentity})) {
            if (!cursor.moveToFirst()) return null;
            return new Row(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getString(5),
                    cursor.getString(6), cursor.getString(7), cursor.getString(8),
                    cursor.getString(9), cursor.getString(10), cursor.getString(11),
                    safeBlob(cursor, 12), safeBlob(cursor, 13), safe(cursor.getString(14)),
                    safe(cursor.getString(15)), cursor.getLong(16), cursor.getLong(17));
        }
    }

    private static byte[] safeBlob(Cursor cursor, int index) {
        byte[] value = cursor.isNull(index) ? null : cursor.getBlob(index);
        return value == null ? new byte[0] : value.clone();
    }

    private static String encodeMetrics(EditorialP5PilotResult.Metrics metrics) {
        try {
            JSONObject json = new JSONObject();
            json.put("providerCallsBeforePreflight", metrics.providerCallsBeforePreflight());
            json.put("primaryCalls", metrics.primaryCalls());
            json.put("repairCalls", metrics.repairCalls());
            json.put("networkRetries", metrics.networkRetries());
            json.put("inputTokens", metrics.inputTokens());
            json.put("outputTokens", metrics.outputTokens());
            json.put("totalTokens", metrics.totalTokens());
            json.put("estimatedCost", metrics.estimatedCost().toPlainString());
            json.put("actualReportedCost", metrics.actualReportedCost().toPlainString());
            json.put("requestContextSize", metrics.requestContextSize());
            json.put("finishReason", metrics.finishReason());
            json.put("truncated", metrics.truncated());
            json.put("schemaValidationPassed", metrics.schemaValidationPassed());
            json.put("receiptValidationPassed", metrics.receiptValidationPassed());
            json.put("preserveDraftCount", metrics.preserveDraftCount());
            json.put("findingCount", metrics.findingCount());
            json.put("falseStopCount", metrics.falseStopCount());
            json.put("latencyMillis", metrics.latencyMillis());
            return json.toString();
        } catch (JSONException error) {
            throw new IllegalStateException("P5C metrics encoding failed", error);
        }
    }

    private static EditorialP5PilotResult.Metrics decodeMetrics(String value) {
        try {
            JSONObject json = new JSONObject(value);
            return new EditorialP5PilotResult.Metrics(
                    json.getInt("providerCallsBeforePreflight"), json.getInt("primaryCalls"),
                    json.getInt("repairCalls"), json.getInt("networkRetries"),
                    json.getInt("inputTokens"), json.getInt("outputTokens"),
                    json.getInt("totalTokens"), decimal(json, "estimatedCost"),
                    decimal(json, "actualReportedCost"), json.getInt("requestContextSize"),
                    json.getString("finishReason"), json.getBoolean("truncated"),
                    json.getBoolean("schemaValidationPassed"),
                    json.getBoolean("receiptValidationPassed"), json.getInt("preserveDraftCount"),
                    json.getInt("findingCount"), json.getInt("falseStopCount"),
                    json.getLong("latencyMillis"));
        } catch (JSONException | NumberFormatException error) {
            throw new IllegalArgumentException("P5C persisted metrics are invalid", error);
        }
    }

    private static BigDecimal decimal(JSONObject json, String key) throws JSONException {
        return new BigDecimal(json.getString(key));
    }

    private static void requireHash(String value, String label) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(label + " must be lowercase SHA-256");
        }
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private static String text(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    private static String bounded(String value, String label) {
        String normalized = value == null ? "" : value;
        if (normalized.length() > 256 || normalized.indexOf('\n') >= 0
                || normalized.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " is too large or contains a line break");
        }
        return normalized;
    }

    private static boolean allowedExceptionClass(String value) {
        return value.isEmpty() || switch (value) {
            case "UnknownHostException", "ConnectException", "SSLException",
                    "SSLHandshakeException", "SocketTimeoutException", "InterruptedIOException",
                    "ApiHttpException", "JSONException", "IOException", "CancellationException",
                    "RuntimeException" -> true;
            default -> false;
        };
    }

    private static final class AuthorizationAlreadyConsumed extends RuntimeException { }

    private record Pending(String requestIdentity, String bindingIdentity,
                           String runDeclarationIdentity, String chapterKey, String phase,
                           String predecessorIdentity, String requestEnvelopeHash,
                           String provider, String model,
                           EditorialP5PilotAuthorization authorization) {
        boolean matches(Row row) {
            return requestIdentity.equals(row.requestIdentity)
                    && bindingIdentity.equals(row.bindingIdentity)
                    && runDeclarationIdentity.equals(row.runDeclarationIdentity)
                    && chapterKey.equals(row.chapterKey) && phase.equals(row.phase)
                    && predecessorIdentity.equals(row.predecessorIdentity)
                    && requestEnvelopeHash.equals(row.requestEnvelopeHash)
                    && provider.equals(row.provider) && model.equals(row.model);
        }

        boolean matches(Pending other) {
            return requestIdentity.equals(other.requestIdentity)
                    && bindingIdentity.equals(other.bindingIdentity)
                    && runDeclarationIdentity.equals(other.runDeclarationIdentity)
                    && chapterKey.equals(other.chapterKey) && phase.equals(other.phase)
                    && predecessorIdentity.equals(other.predecessorIdentity)
                    && requestEnvelopeHash.equals(other.requestEnvelopeHash)
                    && provider.equals(other.provider) && model.equals(other.model);
        }
    }

    private record Row(String attemptIdentity, String requestIdentity, String bindingIdentity,
                       String runDeclarationIdentity, String chapterKey, String phase,
                       String predecessorIdentity, String requestEnvelopeHash, String provider,
                       String model, String status, String responseIdentity, byte[] reportBytes,
                       byte[] receiptBytes, String metricsJson, String recoveryReasonCode,
                       long createdAt, long updatedAt) {
        Row {
            reportBytes = reportBytes == null ? new byte[0] : reportBytes.clone();
            receiptBytes = receiptBytes == null ? new byte[0] : receiptBytes.clone();
        }
    }
}
