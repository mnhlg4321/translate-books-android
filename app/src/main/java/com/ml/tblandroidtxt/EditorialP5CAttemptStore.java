package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotExecution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
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
                authorization.model());
        Pending old = pending.putIfAbsent(request.attemptIdentity(), value);
        if (old != null && !old.equals(value)) {
            throw new IllegalStateException("P5C attempt identity collision");
        }
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
                SQLiteDatabase db = database.editorialWritableDatabase();
                ContentValues update = new ContentValues();
                update.put("status", "CLAIMED");
                update.put("recovery_reason_code", "");
                update.put("updated_at", System.currentTimeMillis());
                if (db.update(TABLE, update, "attempt_identity=? AND status='RECOVERY_REQUIRED'",
                        new String[]{attemptIdentity}) != 1) {
                    return Claim.IN_FLIGHT;
                }
                return Claim.ACQUIRED;
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
        try {
            db.insertOrThrow(TABLE, null, values);
            return Claim.ACQUIRED;
        } catch (SQLiteConstraintException race) {
            Row raced = findRow(attemptIdentity);
            if (raced != null && facts.matches(raced)) {
                if ("COMMITTED".equals(raced.status)) return Claim.ALREADY_COMMITTED;
                if ("CLAIMED".equals(raced.status)) return Claim.IN_FLIGHT;
            }
            throw new IllegalStateException("P5C attempt claim collision", race);
        }
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

    private record Pending(String requestIdentity, String bindingIdentity,
                           String runDeclarationIdentity, String chapterKey, String phase,
                           String predecessorIdentity, String requestEnvelopeHash,
                           String provider, String model) {
        boolean matches(Row row) {
            return requestIdentity.equals(row.requestIdentity)
                    && bindingIdentity.equals(row.bindingIdentity)
                    && runDeclarationIdentity.equals(row.runDeclarationIdentity)
                    && chapterKey.equals(row.chapterKey) && phase.equals(row.phase)
                    && predecessorIdentity.equals(row.predecessorIdentity)
                    && requestEnvelopeHash.equals(row.requestEnvelopeHash)
                    && provider.equals(row.provider) && model.equals(row.model);
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
