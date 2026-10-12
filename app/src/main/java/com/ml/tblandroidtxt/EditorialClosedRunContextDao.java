package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunCompatibilityOutcome;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContextDraft;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosedRunContractFacts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Append/read-only DAO; authoritative attempt ordinals are allocated inside SQLite transactions. */
public final class EditorialClosedRunContextDao {
    private static final String TABLE = "editorial_closed_run_contexts";
    private static final String SELECT = "SELECT closed_run_identity,closed_run_fingerprint,closed_context_version,"
            + "project_revision_identity,scope_snapshot_identity,canonical_pack_hash,compatibility_evaluation_id,"
            + "compatibility_outcome,trusted_profile_id,trusted_profile_version,canonical_profile_hash,machine_fingerprint,"
            + "pack_contract_id,pack_contract_version,pack_schema_id,pack_schema_version,evaluator_contract_version,"
            + "adapter_set_fingerprint,capability_fingerprint,compatibility_context_fingerprint,engine_version_used,"
            + "run_kind,phase_identity,run_attempt_ordinal,input_manifest_fingerprint,source_run_row_id,closed_at FROM " + TABLE;
    private final TranslationRepository database;

    public EditorialClosedRunContextDao(TranslationRepository database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /** The caller supplies no attempt ordinal; the DAO allocates it transactionally. */
    public EditorialIdentityAppendResult<EditorialClosedRunContext> append(
            EditorialClosedRunContextDraft draft, Long sourceRunRowId, long closedAt) {
        if (draft == null || closedAt < 0) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.INVALID_IMMUTABLE_RECORD,
                    null, "", "closed-run-draft-or-timestamp-invalid");
        }
        SQLiteDatabase db = database.editorialWritableDatabase();
        EditorialIdentityPersistenceCode precondition = validatePreconditions(db, draft, sourceRunRowId);
        if (precondition != null) {
            return EditorialIdentityDaoSupport.result(precondition, null, "", "closed-run-precondition-failed");
        }
        try {
            db.beginTransaction();
            try {
                Optional<EditorialClosedRunContext> existing = findExactDraft(db, draft);
                if (existing.isPresent()) {
                    db.setTransactionSuccessful();
                    return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                            existing.get(), existing.get().closedRunIdentity(), "same-closed-context");
                }
                long ordinal = nextOrdinal(db, draft);
                EditorialClosedRunContext context;
                try {
                    context = EditorialClosedRunContext.allocate(draft, ordinal);
                } catch (IllegalArgumentException invalid) {
                    return EditorialIdentityDaoSupport.result(
                            EditorialIdentityPersistenceCode.RUN_CONTEXT_NOT_CLOSED, null, "",
                            "closed-run-gate-rejected");
                }
                insert(db, context, sourceRunRowId, closedAt);
                db.setTransactionSuccessful();
                return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.APPENDED,
                        context, context.closedRunIdentity(), "appended");
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteConstraintException error) {
            Optional<EditorialClosedRunContext> raced = findExactDraft(db, draft);
            if (raced.isPresent()) {
                return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                        raced.get(), raced.get().closedRunIdentity(), "same-closed-context-after-race");
            }
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.ATTEMPT_ALLOCATION_FAILURE,
                    null, "", "closed-run-allocation-rollback");
        } catch (SQLiteException | IllegalStateException error) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                    null, "", "closed-run-transaction-rollback");
        }
    }

    public Optional<EditorialClosedRunContext> findByIdentity(String identity) {
        if (identity == null || identity.isBlank()) return Optional.empty();
        return findByIdentity(database.editorialReadableDatabase(), identity);
    }

    public List<EditorialClosedRunContext> listByScope(String scopeSnapshotIdentity) {
        if (scopeSnapshotIdentity == null || scopeSnapshotIdentity.isBlank()) return List.of();
        ArrayList<EditorialClosedRunContext> result = new ArrayList<>();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE scope_snapshot_identity=? ORDER BY run_attempt_ordinal ASC,closed_run_identity ASC",
                new String[]{scopeSnapshotIdentity})) {
            while (cursor.moveToNext()) {
                try { result.add(read(cursor)); } catch (RuntimeException ignored) { return List.of(); }
            }
        }
        return List.copyOf(result);
    }

    private EditorialIdentityPersistenceCode validatePreconditions(
            SQLiteDatabase db, EditorialClosedRunContextDraft draft, Long sourceRunRowId) {
        if (draft.compatibilityOutcome() != EditorialClosedRunCompatibilityOutcome.DATA_COMPATIBLE) {
            return EditorialIdentityPersistenceCode.RUN_CONTEXT_NOT_CLOSED;
        }
        if (!EditorialIdentityDaoSupport.hasRow(db, "editorial_project_revisions", "revision_identity",
                draft.projectRevisionIdentity())) return EditorialIdentityPersistenceCode.PROJECT_REVISION_REQUIRED;
        try (Cursor scope = db.rawQuery(
                "SELECT project_revision_identity,manifest_fingerprint FROM editorial_input_scope_snapshots "
                        + "WHERE scope_snapshot_identity=?", new String[]{draft.scopeSnapshotIdentity()})) {
            if (!scope.moveToFirst()) return EditorialIdentityPersistenceCode.INPUT_SCOPE_REQUIRED;
            if (!draft.projectRevisionIdentity().equals(scope.getString(0))
                    || !draft.inputManifestFingerprint().equals(scope.getString(1))) {
                return EditorialIdentityPersistenceCode.COMPATIBILITY_CONTEXT_MISMATCH;
            }
        }
        if (sourceRunRowId != null && !EditorialIdentityDaoSupport.hasRow(
                db, "editorial_runs", "id", String.valueOf(sourceRunRowId))) {
            return EditorialIdentityPersistenceCode.FOREIGN_KEY_RESTRICTED;
        }
        try (Cursor evaluation = db.rawQuery(
                "SELECT canonical_pack_hash,trusted_profile_id,trusted_profile_version,canonical_profile_hash,"
                        + "engine_version_used,machine_contract_fingerprint,evaluator_contract_version,"
                        + "adapter_set_fingerprint,capability_fingerprint,context_fingerprint,compatibility_outcome "
                        + "FROM editorial_pack_compatibility_evaluations WHERE evaluation_id=?",
                new String[]{draft.compatibilityEvaluationId()})) {
            if (!evaluation.moveToFirst()) return EditorialIdentityPersistenceCode.COMPATIBILITY_CONTEXT_MISMATCH;
            if (!draft.canonicalPackHash().equals(evaluation.getString(0))
                    || !draft.trustedProfileId().equals(evaluation.getString(1))
                    || !draft.trustedProfileVersion().equals(evaluation.getString(2))
                    || !draft.canonicalProfileHash().equals(evaluation.getString(3))
                    || !draft.contractFacts().engineVersionUsed().equals(evaluation.getString(4))
                    || !draft.machineFingerprint().equals(evaluation.getString(5))
                    || !draft.contractFacts().evaluatorContractVersion().equals(evaluation.getString(6))
                    || !draft.contractFacts().adapterSetFingerprint().equals(evaluation.getString(7))
                    || !draft.contractFacts().capabilityFingerprint().equals(evaluation.getString(8))
                    || !draft.contractFacts().compatibilityContextFingerprint().equals(evaluation.getString(9))
                    || !EditorialClosedRunCompatibilityOutcome.DATA_COMPATIBLE.name().equals(evaluation.getString(10))) {
                return EditorialIdentityPersistenceCode.TRUSTED_PROFILE_CONTEXT_MISMATCH;
            }
        }
        try (Cursor pack = db.rawQuery(
                "SELECT contract_version,schema_version,engine_version_used,canonical_pack_hash FROM editorial_packs "
                        + "WHERE canonical_pack_hash=?", new String[]{draft.canonicalPackHash()})) {
            if (!pack.moveToFirst()) return EditorialIdentityPersistenceCode.COMPATIBILITY_CONTEXT_MISMATCH;
            if (!draft.contractFacts().packContractVersion().equals(pack.getString(0))
                    || !draft.contractFacts().packSchemaVersion().equals(pack.getString(1))
                    || !draft.contractFacts().engineVersionUsed().equals(pack.getString(2))
                    || !draft.canonicalPackHash().equals(pack.getString(3))) {
                return EditorialIdentityPersistenceCode.COMPATIBILITY_CONTEXT_MISMATCH;
            }
        }
        return null;
    }

    private Optional<EditorialClosedRunContext> findExactDraft(
            SQLiteDatabase db, EditorialClosedRunContextDraft draft) {
        try (Cursor cursor = db.rawQuery(SELECT
                + " WHERE scope_snapshot_identity=? AND run_kind=? AND phase_identity=? AND input_manifest_fingerprint=?",
                new String[]{draft.scopeSnapshotIdentity(), draft.runKind(), draft.phaseIdentity(),
                        draft.inputManifestFingerprint()})) {
            while (cursor.moveToNext()) {
                EditorialClosedRunContext existing = read(cursor);
                if (sameDraft(existing, draft)) return Optional.of(existing);
            }
        } catch (RuntimeException invalidStoredRow) { return Optional.empty(); }
        return Optional.empty();
    }

    private long nextOrdinal(SQLiteDatabase db, EditorialClosedRunContextDraft draft) {
        long next = 0;
        try (Cursor cursor = db.rawQuery(SELECT
                + " WHERE scope_snapshot_identity=? AND run_kind=? AND phase_identity=?",
                new String[]{draft.scopeSnapshotIdentity(), draft.runKind(), draft.phaseIdentity()})) {
            while (cursor.moveToNext()) {
                EditorialClosedRunContext existing = read(cursor);
                if (sameAllocationKey(existing, draft)) {
                    next = Math.max(next, existing.runAttemptOrdinal() + 1);
                }
            }
        }
        return next;
    }

    private Optional<EditorialClosedRunContext> findByIdentity(SQLiteDatabase db, String identity) {
        try (Cursor cursor = db.rawQuery(SELECT + " WHERE closed_run_identity=?", new String[]{identity})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(read(cursor));
        } catch (RuntimeException invalidStoredRow) { return Optional.empty(); }
    }

    private void insert(SQLiteDatabase db, EditorialClosedRunContext context,
                        Long sourceRunRowId, long closedAt) {
        ContentValues row = new ContentValues();
        row.put("closed_run_identity", context.closedRunIdentity());
        row.put("closed_run_fingerprint", context.closedRunFingerprint());
        row.put("closed_context_version", context.closedContextVersion());
        row.put("project_revision_identity", context.projectRevisionIdentity());
        row.put("scope_snapshot_identity", context.scopeSnapshotIdentity());
        row.put("canonical_pack_hash", context.canonicalPackHash());
        row.put("compatibility_evaluation_id", context.compatibilityEvaluationId());
        row.put("compatibility_outcome", context.compatibilityOutcome().name());
        row.put("trusted_profile_id", context.trustedProfileId());
        row.put("trusted_profile_version", context.trustedProfileVersion());
        row.put("canonical_profile_hash", context.canonicalProfileHash());
        row.put("machine_fingerprint", context.machineFingerprint());
        row.put("pack_contract_id", context.contractFacts().packContractId());
        row.put("pack_contract_version", context.contractFacts().packContractVersion());
        row.put("pack_schema_id", context.contractFacts().packSchemaId());
        row.put("pack_schema_version", context.contractFacts().packSchemaVersion());
        row.put("evaluator_contract_version", context.contractFacts().evaluatorContractVersion());
        row.put("adapter_set_fingerprint", context.contractFacts().adapterSetFingerprint());
        row.put("capability_fingerprint", context.contractFacts().capabilityFingerprint());
        row.put("compatibility_context_fingerprint", context.contractFacts().compatibilityContextFingerprint());
        row.put("engine_version_used", context.contractFacts().engineVersionUsed());
        row.put("run_kind", context.runKind());
        row.put("phase_identity", context.phaseIdentity());
        row.put("run_attempt_ordinal", context.runAttemptOrdinal());
        row.put("input_manifest_fingerprint", context.inputManifestFingerprint());
        if (sourceRunRowId == null) row.putNull("source_run_row_id");
        else row.put("source_run_row_id", sourceRunRowId);
        row.put("closed_at", closedAt);
        db.insertOrThrow(TABLE, null, row);
    }

    private EditorialClosedRunContext read(Cursor cursor) {
        EditorialClosedRunCompatibilityOutcome outcome = EditorialClosedRunCompatibilityOutcome.valueOf(cursor.getString(7));
        EditorialClosedRunContractFacts facts = new EditorialClosedRunContractFacts(
                cursor.getString(12), cursor.getString(13), cursor.getString(14), cursor.getString(15),
                cursor.getString(16), cursor.getString(17), cursor.getString(18), cursor.getString(19),
                cursor.getString(20));
        EditorialClosedRunContextDraft draft = new EditorialClosedRunContextDraft(
                cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5),
                cursor.getString(6), outcome, cursor.getString(8), cursor.getString(9), cursor.getString(10),
                cursor.getString(11), facts, cursor.getString(21), cursor.getString(22), cursor.getString(24));
        EditorialClosedRunContext context = EditorialClosedRunContext.allocate(draft, cursor.getLong(23));
        if (!context.closedRunIdentity().equals(cursor.getString(0))
                || !context.closedRunFingerprint().equals(cursor.getString(1))) {
            throw new IllegalArgumentException("stored closed-run canonical bytes mismatch");
        }
        return context;
    }

    private boolean sameAllocationKey(EditorialClosedRunContext existing,
                                      EditorialClosedRunContextDraft draft) {
        return sameDraft(existing, draft);
    }

    private boolean sameDraft(EditorialClosedRunContext existing,
                              EditorialClosedRunContextDraft draft) {
        EditorialClosedRunContractFacts left = existing.contractFacts();
        EditorialClosedRunContractFacts right = draft.contractFacts();
        return existing.closedContextVersion().equals(draft.closedContextVersion())
                && existing.projectRevisionIdentity().equals(draft.projectRevisionIdentity())
                && existing.scopeSnapshotIdentity().equals(draft.scopeSnapshotIdentity())
                && existing.canonicalPackHash().equals(draft.canonicalPackHash())
                && existing.compatibilityEvaluationId().equals(draft.compatibilityEvaluationId())
                && existing.compatibilityOutcome() == draft.compatibilityOutcome()
                && existing.trustedProfileId().equals(draft.trustedProfileId())
                && existing.trustedProfileVersion().equals(draft.trustedProfileVersion())
                && existing.canonicalProfileHash().equals(draft.canonicalProfileHash())
                && existing.machineFingerprint().equals(draft.machineFingerprint())
                && left.equals(right)
                && existing.runKind().equals(draft.runKind())
                && existing.phaseIdentity().equals(draft.phaseIdentity())
                && existing.inputManifestFingerprint().equals(draft.inputManifestFingerprint());
    }
}
