package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Narrow append/read boundary for v15 compatibility evidence; there are no mutation APIs. */
public final class EditorialPackCompatibilityEvaluationDao {
    private static final String TABLE = "editorial_pack_compatibility_evaluations";
    private final TranslationRepository database;

    public EditorialPackCompatibilityEvaluationDao(TranslationRepository database) {
        if (database == null) throw new IllegalArgumentException("database is required");
        this.database = database;
    }

    /** Appends a trusted-profile snapshot. Duplicate evaluation identities are rejected. */
    public long append(EditorialPackCompatibilityEvaluation evaluation) {
        if (evaluation == null || evaluation.attestation() != EditorialPackCompatibilityEvaluation.Attestation.TRUSTED_PROFILE) {
            throw new IllegalArgumentException("Only trusted-profile evaluations may be appended");
        }
        ContentValues values = new ContentValues();
        values.put("evaluation_id", evaluation.evaluationId());
        values.put("import_id", evaluation.importId());
        putNullable(values, "pack_row_id", evaluation.packRowId());
        putNullable(values, "compatibility_result_id", evaluation.compatibilityResultId());
        values.put("canonical_pack_hash", evaluation.canonicalPackHash());
        values.put("trusted_profile_id", evaluation.trustedProfileId().orElseThrow());
        values.put("trusted_profile_version", evaluation.trustedProfileVersion().orElseThrow());
        values.put("canonical_profile_hash", evaluation.canonicalProfileHash().orElseThrow());
        values.put("engine_version_used", evaluation.engineVersionUsed());
        values.put("machine_contract_fingerprint", evaluation.machineContractFingerprint());
        values.put("evaluator_contract_version", evaluation.evaluatorContractVersion().orElseThrow());
        values.put("adapter_set_fingerprint", evaluation.adapterSetFingerprint().orElseThrow());
        values.put("capability_fingerprint", evaluation.capabilityFingerprint().orElseThrow());
        values.put("context_fingerprint", evaluation.contextFingerprint().orElseThrow());
        values.put("compatibility_outcome", evaluation.compatibilityOutcome().name());
        values.put("reason_code", evaluation.reasonCode());
        values.put("blocker_details", evaluation.blockerDetails());
        values.put("evaluated_at", evaluation.evaluatedAt());
        return database.editorialWritableDatabase().insertOrThrow(TABLE, null, values);
    }

    public Optional<EditorialPackCompatibilityEvaluation> findByEvaluationId(String evaluationId) {
        if (evaluationId == null || evaluationId.isBlank()) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT id,evaluation_id,import_id,pack_row_id,compatibility_result_id,canonical_pack_hash,trusted_profile_id,trusted_profile_version,canonical_profile_hash,engine_version_used,machine_contract_fingerprint,evaluator_contract_version,adapter_set_fingerprint,capability_fingerprint,context_fingerprint,compatibility_outcome,reason_code,blocker_details,evaluated_at FROM " + TABLE + " WHERE evaluation_id=?",
                new String[]{evaluationId})) {
            return cursor.moveToFirst() ? Optional.of(read(cursor)) : Optional.empty();
        }
    }

    public List<EditorialPackCompatibilityEvaluation> listByPackHash(String canonicalPackHash) {
        ArrayList<EditorialPackCompatibilityEvaluation> result = new ArrayList<>();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT id,evaluation_id,import_id,pack_row_id,compatibility_result_id,canonical_pack_hash,trusted_profile_id,trusted_profile_version,canonical_profile_hash,engine_version_used,machine_contract_fingerprint,evaluator_contract_version,adapter_set_fingerprint,capability_fingerprint,context_fingerprint,compatibility_outcome,reason_code,blocker_details,evaluated_at FROM " + TABLE + " WHERE canonical_pack_hash=? ORDER BY evaluated_at ASC,id ASC",
                new String[]{canonicalPackHash})) {
            while (cursor.moveToNext()) result.add(read(cursor));
        }
        return List.copyOf(result);
    }

    public List<EditorialPackCompatibilityEvaluation> listByImportId(String importId) {
        ArrayList<EditorialPackCompatibilityEvaluation> result = new ArrayList<>();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT id,evaluation_id,import_id,pack_row_id,compatibility_result_id,canonical_pack_hash,trusted_profile_id,trusted_profile_version,canonical_profile_hash,engine_version_used,machine_contract_fingerprint,evaluator_contract_version,adapter_set_fingerprint,capability_fingerprint,context_fingerprint,compatibility_outcome,reason_code,blocker_details,evaluated_at FROM " + TABLE + " WHERE import_id=? ORDER BY evaluated_at ASC,id ASC",
                new String[]{importId})) {
            while (cursor.moveToNext()) result.add(read(cursor));
        }
        return List.copyOf(result);
    }

    /** Explicit legacy view: v14 rows are readable but never attested retroactively. */
    public List<EditorialPackCompatibilityEvaluation> listByPackHashIncludingLegacy(String canonicalPackHash) {
        ArrayList<EditorialPackCompatibilityEvaluation> result = new ArrayList<>(listByPackHash(canonicalPackHash));
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT id,import_id,pack_row_id,canonical_pack_hash,engine_version_used,machine_contract_fingerprint,compatibility_class,blocked_reason,evaluated_at FROM editorial_pack_compatibility_results WHERE canonical_pack_hash=? ORDER BY evaluated_at ASC,id ASC",
                new String[]{canonicalPackHash})) {
            while (cursor.moveToNext()) result.add(EditorialPackCompatibilityEvaluation.legacy(
                    cursor.getLong(0), cursor.getString(1), nullableLong(cursor, 2), cursor.getString(3),
                    cursor.getString(4), cursor.getString(5), parseOutcome(cursor.getString(6)),
                    cursor.getString(7), cursor.getLong(8)));
        }
        result.sort(Comparator.comparingLong(EditorialPackCompatibilityEvaluation::evaluatedAt)
                .thenComparing(EditorialPackCompatibilityEvaluation::evaluationId));
        return List.copyOf(result);
    }

    private EditorialPackCompatibilityEvaluation read(Cursor cursor) {
        return EditorialPackCompatibilityEvaluation.persisted(
                cursor.getLong(0), cursor.getString(1), cursor.getString(2), nullableLong(cursor, 3), nullableLong(cursor, 4),
                cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9),
                cursor.getString(10), cursor.getString(11), cursor.getString(12), cursor.getString(13), cursor.getString(14),
                parseOutcome(cursor.getString(15)), cursor.getString(16), cursor.getString(17), cursor.getLong(18));
    }

    private static EditorialPackCompatibilityClass parseOutcome(String value) {
        try { return EditorialPackCompatibilityClass.valueOf(value); }
        catch (RuntimeException error) { throw new IllegalStateException("Unknown compatibility outcome", error); }
    }

    private static Long nullableLong(Cursor cursor, int index) { return cursor.isNull(index) ? null : cursor.getLong(index); }
    private static void putNullable(ContentValues values, String key, Optional<Long> value) {
        if (value.isPresent()) values.put(key, value.get()); else values.putNull(key);
    }
}
