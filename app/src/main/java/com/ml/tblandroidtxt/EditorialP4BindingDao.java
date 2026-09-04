package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4SourceIdentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Append/read-only owner for the complete P4 project/run binding tuple. */
public final class EditorialP4BindingDao {
    private static final String TABLE = "editorial_p4_bindings";
    private static final String SELECT = "SELECT binding_identity,binding_fingerprint,binding_contract_version,"
            + "attempt_request_selector,project_row_id,project_revision_identity,input_scope_snapshot_identity,"
            + "run_declaration_identity,pack_id,pack_version,canonical_pack_hash,manifest_fingerprint,"
            + "trusted_profile_id,trusted_profile_version,canonical_profile_hash,machine_contract_fingerprint,"
            + "compatibility_evaluation_id,compatibility_outcome,evaluation_context_fingerprint,contract_version,"
            + "schema_version,phase_graph_fingerprint,context_allow_list_fingerprint,source_mode,glossary_status,"
            + "pronoun_status,pair_context_status,explicit_user_decision_provenance,input_manifest_fingerprint,"
            + "run_attempt_ordinal,run_kind,phase_identity,execution_allowed,certification_state,created_at FROM "
            + TABLE;

    private final TranslationRepository database;

    public EditorialP4BindingDao(TranslationRepository database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /** Public convenience boundary; the P4 transaction service uses the scoped variant. */
    public EditorialP4BindingResult append(String attemptRequestSelector, long projectId,
                                            EditorialP4Binding binding, long createdAt) {
        if (createdAt < 0) return failure(projectId, "binding-timestamp-invalid");
        SQLiteDatabase db = database.editorialWritableDatabase();
        try {
            db.beginTransaction();
            EditorialP4BindingResult result = appendInTransaction(db, attemptRequestSelector,
                    projectId, binding, createdAt);
            if (result.code() == EditorialP4BindingResult.Code.APPENDED
                    || result.code() == EditorialP4BindingResult.Code.ALREADY_EXISTS) {
                db.setTransactionSuccessful();
            }
            return result;
        } catch (SQLiteException | IllegalStateException error) {
            return failure(projectId, "binding-transaction-rolled-back");
        } finally {
            db.endTransaction();
        }
    }

    /** Package boundary for the surrounding atomic project/setup transaction. */
    EditorialP4BindingResult appendInTransaction(SQLiteDatabase db, String attemptRequestSelector,
                                                  long projectId, EditorialP4Binding binding,
                                                  long createdAt) {
        if (db == null || binding == null || attemptRequestSelector == null
                || attemptRequestSelector.isBlank() || projectId <= 0 || createdAt < 0) {
            return failure(projectId, "binding-request-invalid");
        }
        EditorialP4Binding existingBySelector = findBySelector(db, attemptRequestSelector);
        if (existingBySelector != null) {
            return classify(existingBySelector, binding, projectId, "attempt-selector");
        }
        EditorialP4Binding existingByIdentity = findByIdentity(db, binding.bindingIdentity());
        if (existingByIdentity != null) {
            return classify(existingByIdentity, binding, projectId, "binding-identity");
        }
        if (!has(db, "editorial_projects", "id", String.valueOf(projectId))
                || !has(db, "editorial_project_revisions", "revision_identity",
                binding.projectRevisionIdentity())
                || !has(db, "editorial_input_scope_snapshots", "scope_snapshot_identity",
                binding.inputScopeSnapshotIdentity())
                || !has(db, "editorial_authoritative_run_declarations", "declaration_identity",
                binding.runDeclarationIdentity())
                || !has(db, "editorial_pack_compatibility_evaluations", "evaluation_id",
                binding.compatibilityEvaluationId())
                || !has(db, "editorial_packs", "canonical_pack_hash", binding.canonicalPackHash())) {
            return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.FOREIGN_REFERENCE_MISSING,
                    projectId, null, "binding-reference-precondition-failed");
        }
        try {
            insert(db, attemptRequestSelector, projectId, binding, createdAt);
            for (EditorialP4SourceIdentity input : binding.inputs()) insertInput(db, binding.bindingIdentity(), input);
            EditorialP4Binding readback = findByIdentity(db, binding.bindingIdentity());
            if (readback == null || !sameStoredBytes(readback, binding)) {
                return failure(projectId, "binding-exact-readback-failed");
            }
            return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.APPENDED,
                    projectId, readback, "appended");
        } catch (SQLiteConstraintException error) {
            EditorialP4Binding raced = findBySelector(db, attemptRequestSelector);
            if (raced != null) return classify(raced, binding, projectId, "attempt-selector-race");
            return failure(projectId, "binding-insert-rollback");
        } catch (SQLiteException | IllegalArgumentException error) {
            return failure(projectId, "binding-insert-rollback");
        }
    }

    public Optional<EditorialP4Binding> findByIdentity(String identity) {
        if (identity == null || identity.isBlank()) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE binding_identity=?", new String[]{identity})) {
            return cursor.moveToFirst() ? Optional.of(read(cursor)) : Optional.empty();
        } catch (RuntimeException invalidStoredRow) {
            return Optional.empty();
        }
    }

    public Optional<EditorialP4Binding> findByAttemptRequestSelector(String selector) {
        if (selector == null || selector.isBlank()) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE attempt_request_selector=?", new String[]{selector})) {
            return cursor.moveToFirst() ? Optional.of(read(cursor)) : Optional.empty();
        } catch (RuntimeException invalidStoredRow) {
            return Optional.empty();
        }
    }

    public List<EditorialP4Binding> listByProject(long projectId) {
        if (projectId <= 0) return List.of();
        ArrayList<EditorialP4Binding> result = new ArrayList<>();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE project_row_id=? ORDER BY run_attempt_ordinal ASC,binding_identity ASC",
                new String[]{String.valueOf(projectId)})) {
            while (cursor.moveToNext()) result.add(read(cursor));
            return List.copyOf(result);
        } catch (RuntimeException invalidStoredRow) {
            return List.of();
        }
    }

    private EditorialP4Binding findBySelector(SQLiteDatabase db, String selector) {
        try (Cursor cursor = db.rawQuery(SELECT + " WHERE attempt_request_selector=?",
                new String[]{selector})) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    private EditorialP4Binding findByIdentity(SQLiteDatabase db, String identity) {
        try (Cursor cursor = db.rawQuery(SELECT + " WHERE binding_identity=?",
                new String[]{identity})) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    private void insert(SQLiteDatabase db, String selector, long projectId,
                        EditorialP4Binding binding, long createdAt) {
        ContentValues row = new ContentValues();
        row.put("binding_identity", binding.bindingIdentity());
        row.put("binding_fingerprint", binding.bindingFingerprint());
        row.put("binding_contract_version", EditorialP4Binding.CONTRACT_VERSION);
        row.put("attempt_request_selector", selector);
        row.put("project_row_id", projectId);
        row.put("project_revision_identity", binding.projectRevisionIdentity());
        row.put("input_scope_snapshot_identity", binding.inputScopeSnapshotIdentity());
        row.put("run_declaration_identity", binding.runDeclarationIdentity());
        row.put("pack_id", binding.packId());
        row.put("pack_version", binding.packVersion());
        row.put("canonical_pack_hash", binding.canonicalPackHash());
        row.put("manifest_fingerprint", binding.manifestFingerprint());
        row.put("trusted_profile_id", binding.trustedProfileId());
        row.put("trusted_profile_version", binding.trustedProfileVersion());
        row.put("canonical_profile_hash", binding.canonicalProfileHash());
        row.put("machine_contract_fingerprint", binding.machineContractFingerprint());
        row.put("compatibility_evaluation_id", binding.compatibilityEvaluationId());
        row.put("compatibility_outcome", binding.compatibilityOutcome());
        row.put("evaluation_context_fingerprint", binding.evaluationContextFingerprint());
        row.put("contract_version", binding.contractVersion());
        row.put("schema_version", binding.schemaVersion());
        row.put("phase_graph_fingerprint", binding.phaseGraphFingerprint());
        row.put("context_allow_list_fingerprint", binding.contextAllowListFingerprint());
        row.put("source_mode", binding.sourceMode());
        row.put("glossary_status", binding.glossaryStatus());
        row.put("pronoun_status", binding.pronounStatus());
        row.put("pair_context_status", binding.pairContextStatus());
        row.put("explicit_user_decision_provenance", binding.explicitUserDecisionProvenance());
        row.put("input_manifest_fingerprint", binding.inputManifestFingerprint());
        row.put("run_attempt_ordinal", binding.runAttemptOrdinal());
        row.put("run_kind", binding.runKind());
        row.put("phase_identity", binding.phaseIdentity());
        row.put("execution_allowed", 0);
        row.put("certification_state", binding.certificationState());
        row.put("created_at", createdAt);
        db.insertOrThrow(TABLE, null, row);
    }

    private void insertInput(SQLiteDatabase db, String bindingIdentity, EditorialP4SourceIdentity input) {
        ContentValues row = new ContentValues();
        row.put("binding_identity", bindingIdentity);
        row.put("role", input.role());
        row.put("ordinal", input.ordinal());
        row.put("source_reference", input.sourceReference());
        row.put("byte_length", input.byteLength());
        row.put("sha256", input.sha256());
        row.put("encoding", input.encoding());
        row.put("schema_status", input.schemaStatus());
        db.insertOrThrow("editorial_p4_binding_inputs", null, row);
    }

    private EditorialP4Binding read(Cursor cursor) {
        ArrayList<EditorialP4SourceIdentity> inputs = new ArrayList<>();
        try (Cursor inputCursor = database.editorialReadableDatabase().rawQuery(
                "SELECT role,source_reference,byte_length,sha256,encoding,schema_status,ordinal "
                        + "FROM editorial_p4_binding_inputs WHERE binding_identity=? ORDER BY role,ordinal",
                new String[]{cursor.getString(0)})) {
            while (inputCursor.moveToNext()) inputs.add(new EditorialP4SourceIdentity(
                    inputCursor.getString(0), inputCursor.getString(1), inputCursor.getLong(2),
                    inputCursor.getString(3), inputCursor.getString(4), inputCursor.getString(5),
                    inputCursor.getLong(6)));
        }
        if (!EditorialP4Binding.CONTRACT_VERSION.equals(cursor.getString(2))
                || cursor.getInt(32) != 0
                || !EditorialP4Binding.NOT_CERTIFIED.equals(cursor.getString(33))) {
            throw new IllegalArgumentException("stored P4 binding execution state is invalid");
        }
        return EditorialP4Binding.fromStored(cursor.getString(0), cursor.getString(1),
                cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8),
                cursor.getString(9), cursor.getString(10), cursor.getString(11), cursor.getString(12),
                cursor.getString(13), cursor.getString(14), cursor.getString(15), cursor.getString(16),
                cursor.getString(17), cursor.getString(18), cursor.getString(19), cursor.getString(20),
                cursor.getString(21), cursor.getString(22), cursor.getString(23), cursor.getString(24),
                cursor.getString(25), cursor.getString(26), cursor.getString(27), cursor.getString(28),
                cursor.getLong(29), cursor.getString(30), cursor.getString(31), inputs);
    }

    private EditorialP4BindingResult classify(EditorialP4Binding existing,
                                               EditorialP4Binding incoming, long projectId, String detail) {
        if (existing.canonicalProjection().equals(incoming.canonicalProjection())
                && existing.bindingFingerprint().equals(incoming.bindingFingerprint())) {
            return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.ALREADY_EXISTS,
                    projectId, existing, "same-immutable-record:" + detail);
        }
        return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.REQUEST_COLLISION,
                projectId, null, "immutable-binding-collision:" + detail);
    }

    private boolean has(SQLiteDatabase db, String table, String column, String value) {
        return EditorialIdentityDaoSupport.hasRow(db, table, column, value);
    }

    private boolean sameStoredBytes(EditorialP4Binding left, EditorialP4Binding right) {
        return left.bindingIdentity().equals(right.bindingIdentity())
                && left.bindingFingerprint().equals(right.bindingFingerprint())
                && left.canonicalProjection().equals(right.canonicalProjection())
                && left.executionAllowed() == right.executionAllowed()
                && left.certificationState().equals(right.certificationState());
    }

    private static EditorialP4BindingResult failure(long projectId, String detail) {
        return EditorialP4BindingResult.of(EditorialP4BindingResult.Code.PERSISTENCE_FAILURE,
                projectId, null, detail);
    }
}
