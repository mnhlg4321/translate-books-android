package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ml.tblandroidtxt.editorial.pack.EditorialAuthoritativeRunAllocationScope;
import com.ml.tblandroidtxt.editorial.pack.EditorialAuthoritativeRunDeclaration;
import com.ml.tblandroidtxt.editorial.pack.EditorialAuthoritativeRunDeclarationDraft;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/** Append/read-only DAO; the only owner of authoritative declaration ordinals and identities. */
public final class EditorialAuthoritativeRunDeclarationDao {
    private static final String TABLE = "editorial_authoritative_run_declarations";
    private static final String SELECT = "SELECT declaration_identity,declaration_fingerprint,"
            + "run_declaration_contract_version,attempt_request_selector,project_revision_identity,"
            + "input_scope_snapshot_identity,compatibility_evaluation_id,run_kind,phase_identity,"
            + "frozen_manifest_fingerprint,frozen_manifest_reference,node_kind,parent_record_identity,"
            + "run_attempt_ordinal,created_at FROM " + TABLE;

    private final TranslationRepository database;
    private final LongSupplier clock;

    public EditorialAuthoritativeRunDeclarationDao(TranslationRepository database) {
        this(database, System::currentTimeMillis);
    }

    EditorialAuthoritativeRunDeclarationDao(TranslationRepository database, LongSupplier clock) {
        this.database = Objects.requireNonNull(database, "database");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Appends one NEW_AUTHORIZED_ATTEMPT. The caller supplies only the semantic
     * draft; the DAO owns time, ordinal, identity and fingerprint.
     */
    public EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration>
    appendNewAuthorizedAttempt(EditorialAuthoritativeRunDeclarationDraft draft) {
        if (draft == null) return result(EditorialIdentityPersistenceCode.INVALID_IMMUTABLE_RECORD,
                null, "", "declaration-draft-required");
        final long createdAt;
        try {
            createdAt = clock.getAsLong();
        } catch (RuntimeException error) {
            return result(EditorialIdentityPersistenceCode.PERSISTENCE_FAILURE, null, "",
                    "declaration-clock-failure");
        }
        if (createdAt < 0) return result(EditorialIdentityPersistenceCode.INVALID_IMMUTABLE_RECORD,
                null, "", "declaration-clock-invalid");

        SQLiteDatabase db = database.editorialWritableDatabase();
        try {
            db.beginTransaction();
            try {
                EditorialAuthoritativeRunDeclaration existing =
                        findByAttemptRequestSelector(db, draft.attemptRequestSelector());
                if (existing != null) {
                    db.setTransactionSuccessful();
                    return classifyRequest(existing, draft);
                }
                EditorialIdentityPersistenceCode precondition = validatePreconditions(db, draft);
                if (precondition != null) return result(precondition, null, "",
                        "declaration-reference-precondition-failed");
                long ordinal = allocateNextOrdinal(db, draft.allocationScope());
                if (ordinal < 0) return result(EditorialIdentityPersistenceCode.ATTEMPT_ALLOCATION_CONFLICT,
                        null, "", "declaration-ordinal-overflow");
                EditorialAuthoritativeRunDeclaration declaration =
                        EditorialAuthoritativeRunDeclaration.allocate(draft, ordinal, createdAt);
                insert(db, declaration);
                EditorialAuthoritativeRunDeclaration readback =
                        findByIdentity(db, declaration.declarationIdentity());
                if (readback == null || !sameStoredBytes(readback, declaration)) {
                    return result(EditorialIdentityPersistenceCode.PERSISTENCE_FAILURE, null,
                            declaration.declarationIdentity(), "declaration-exact-readback-failed");
                }
                db.setTransactionSuccessful();
                return result(EditorialIdentityPersistenceCode.APPENDED, readback,
                        declaration.declarationIdentity(), "appended");
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteConstraintException error) {
            EditorialAuthoritativeRunDeclaration raced = findByAttemptRequestSelector(
                    draft.attemptRequestSelector()).orElse(null);
            if (raced != null) return classifyRequest(raced, draft);
            return result(EditorialIdentityPersistenceCode.ATTEMPT_ALLOCATION_CONFLICT,
                    null, "", "declaration-insert-rolled-back");
        } catch (SQLiteException | IllegalStateException error) {
            return result(EditorialIdentityPersistenceCode.PERSISTENCE_FAILURE,
                    null, "", "declaration-transaction-rolled-back");
        }
    }

    /** Exact retry/read operation; it never allocates or writes. */
    public EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration>
    retrySameAttempt(String declarationIdentity) {
        if (!isHash(declarationIdentity)) return result(EditorialIdentityPersistenceCode.NOT_FOUND,
                null, "", "declaration-selector-invalid");
        Optional<EditorialAuthoritativeRunDeclaration> found = findByDeclarationIdentity(declarationIdentity);
        return found.map(value -> result(EditorialIdentityPersistenceCode.ALREADY_EXISTS, value,
                        value.declarationIdentity(), "same-declaration-retry"))
                .orElseGet(() -> result(EditorialIdentityPersistenceCode.NOT_FOUND,
                        null, declarationIdentity, "declaration-not-found"));
    }

    public Optional<EditorialAuthoritativeRunDeclaration> findByDeclarationIdentity(String identity) {
        if (!isHash(identity)) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE declaration_identity=?", new String[]{identity})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(read(cursor));
        } catch (RuntimeException invalidStoredRow) {
            return Optional.empty();
        }
    }

    /** Package boundary for a caller that already owns the surrounding SQLite transaction. */
    EditorialAuthoritativeRunDeclaration findByIdentityInTransaction(
            SQLiteDatabase db, String identity) {
        if (!isHash(identity)) return null;
        return findByIdentity(db, identity);
    }

    public Optional<EditorialAuthoritativeRunDeclaration> findByAttemptRequestSelector(String selector) {
        if (selector == null || selector.isBlank()) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE attempt_request_selector=?", new String[]{selector})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(read(cursor));
        } catch (RuntimeException invalidStoredRow) {
            return Optional.empty();
        }
    }

    public List<EditorialAuthoritativeRunDeclaration> listByAllocationScope(
            EditorialAuthoritativeRunAllocationScope scope) {
        if (scope == null) return List.of();
        ArrayList<EditorialAuthoritativeRunDeclaration> result = new ArrayList<>();
        String sql = SELECT + " WHERE project_revision_identity=? AND input_scope_snapshot_identity=?"
                + " AND compatibility_evaluation_id=? AND run_kind=? AND phase_identity=?"
                + " AND frozen_manifest_fingerprint=? ORDER BY run_attempt_ordinal ASC,declaration_identity ASC";
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(sql, new String[]{
                scope.projectRevisionIdentity(), scope.inputScopeSnapshotIdentity(),
                scope.compatibilityEvaluationId(), scope.runKind(), scope.phaseIdentity(),
                scope.frozenManifestFingerprint()})) {
            while (cursor.moveToNext()) result.add(read(cursor));
            return List.copyOf(result);
        } catch (RuntimeException invalidStoredRow) {
            return List.of();
        }
    }

    private EditorialIdentityPersistenceCode validatePreconditions(
            SQLiteDatabase db, EditorialAuthoritativeRunDeclarationDraft draft) {
        if (!EditorialIdentityDaoSupport.hasRow(db, "editorial_project_revisions",
                "revision_identity", draft.projectRevisionIdentity())) {
            return EditorialIdentityPersistenceCode.FOREIGN_REFERENCE_MISSING;
        }
        if (!EditorialIdentityDaoSupport.hasRow(db, "editorial_input_scope_snapshots",
                "scope_snapshot_identity", draft.inputScopeSnapshotIdentity())) {
            return EditorialIdentityPersistenceCode.FOREIGN_REFERENCE_MISSING;
        }
        if (!EditorialIdentityDaoSupport.hasRow(db, "editorial_pack_compatibility_evaluations",
                "evaluation_id", draft.compatibilityEvaluationId())) {
            return EditorialIdentityPersistenceCode.FOREIGN_REFERENCE_MISSING;
        }
        if (draft.nodeKind() == EditorialLineageNodeKind.CHILD
                && !EditorialIdentityDaoSupport.hasRow(db, "editorial_lineage_records",
                "record_identity", draft.parentRecordIdentity())) {
            return EditorialIdentityPersistenceCode.PARENT_NOT_FOUND;
        }
        return null;
    }

    private long allocateNextOrdinal(SQLiteDatabase db, EditorialAuthoritativeRunAllocationScope scope) {
        String sql = "SELECT COALESCE(MAX(run_attempt_ordinal),-1)+1 FROM " + TABLE
                + " WHERE project_revision_identity=? AND input_scope_snapshot_identity=?"
                + " AND compatibility_evaluation_id=? AND run_kind=? AND phase_identity=?"
                + " AND frozen_manifest_fingerprint=?";
        try (Cursor cursor = db.rawQuery(sql, new String[]{scope.projectRevisionIdentity(),
                scope.inputScopeSnapshotIdentity(), scope.compatibilityEvaluationId(),
                scope.runKind(), scope.phaseIdentity(), scope.frozenManifestFingerprint()})) {
            if (!cursor.moveToFirst()) return -1L;
            long next = cursor.getLong(0);
            return next < 0 ? -1L : next;
        }
    }

    private void insert(SQLiteDatabase db, EditorialAuthoritativeRunDeclaration declaration) {
        ContentValues row = new ContentValues();
        row.put("declaration_identity", declaration.declarationIdentity());
        row.put("declaration_fingerprint", declaration.declarationFingerprint());
        row.put("run_declaration_contract_version", declaration.runDeclarationContractVersion());
        row.put("attempt_request_selector", declaration.attemptRequestSelector());
        row.put("project_revision_identity", declaration.projectRevisionIdentity());
        row.put("input_scope_snapshot_identity", declaration.inputScopeSnapshotIdentity());
        row.put("compatibility_evaluation_id", declaration.compatibilityEvaluationId());
        row.put("run_kind", declaration.runKind());
        row.put("phase_identity", declaration.phaseIdentity());
        row.put("frozen_manifest_fingerprint", declaration.frozenManifestFingerprint());
        row.put("frozen_manifest_reference", declaration.frozenManifestReference());
        row.put("node_kind", declaration.nodeKind().name());
        if (declaration.parentRecordIdentity() == null) row.putNull("parent_record_identity");
        else row.put("parent_record_identity", declaration.parentRecordIdentity());
        row.put("run_attempt_ordinal", declaration.runAttemptOrdinal());
        row.put("created_at", declaration.createdAt());
        db.insertOrThrow(TABLE, null, row);
    }

    private EditorialAuthoritativeRunDeclaration read(Cursor cursor) {
        if (!EditorialAuthoritativeRunDeclarationDraft.CONTRACT_VERSION.equals(cursor.getString(2))) {
            throw new IllegalArgumentException("stored declaration contract version mismatch");
        }
        EditorialLineageNodeKind node = EditorialLineageNodeKind.valueOf(cursor.getString(11));
        EditorialAuthoritativeRunDeclarationDraft draft = new EditorialAuthoritativeRunDeclarationDraft(
                cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6),
                cursor.getString(7), cursor.getString(8), cursor.getString(9), cursor.getString(10),
                node, cursor.isNull(12) ? null : cursor.getString(12));
        return EditorialAuthoritativeRunDeclaration.fromStored(cursor.getString(0), cursor.getString(1),
                draft, cursor.getLong(13), cursor.getLong(14));
    }

    private EditorialAuthoritativeRunDeclaration findByIdentity(SQLiteDatabase db, String identity) {
        try (Cursor cursor = db.rawQuery(SELECT + " WHERE declaration_identity=?",
                new String[]{identity})) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    private EditorialAuthoritativeRunDeclaration findByAttemptRequestSelector(
            SQLiteDatabase db, String selector) {
        try (Cursor cursor = db.rawQuery(SELECT + " WHERE attempt_request_selector=?",
                new String[]{selector})) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    private EditorialIdentityAppendResult<EditorialAuthoritativeRunDeclaration> classifyRequest(
            EditorialAuthoritativeRunDeclaration existing,
            EditorialAuthoritativeRunDeclarationDraft incoming) {
        if (sameSemanticFields(existing, incoming)) {
            return result(EditorialIdentityPersistenceCode.ALREADY_EXISTS, existing,
                    existing.declarationIdentity(), "same-attempt-request");
        }
        return result(EditorialIdentityPersistenceCode.ATTEMPT_REQUEST_COLLISION, null,
                existing.declarationIdentity(), "same-selector-different-semantic-facts");
    }

    private boolean sameSemanticFields(EditorialAuthoritativeRunDeclaration existing,
                                       EditorialAuthoritativeRunDeclarationDraft incoming) {
        return existing.attemptRequestSelector().equals(incoming.attemptRequestSelector())
                && existing.projectRevisionIdentity().equals(incoming.projectRevisionIdentity())
                && existing.inputScopeSnapshotIdentity().equals(incoming.inputScopeSnapshotIdentity())
                && existing.compatibilityEvaluationId().equals(incoming.compatibilityEvaluationId())
                && existing.runKind().equals(incoming.runKind())
                && existing.phaseIdentity().equals(incoming.phaseIdentity())
                && existing.frozenManifestFingerprint().equals(incoming.frozenManifestFingerprint())
                && existing.frozenManifestReference().equals(incoming.frozenManifestReference())
                && existing.nodeKind() == incoming.nodeKind()
                && Objects.equals(existing.parentRecordIdentity(), incoming.parentRecordIdentity());
    }

    private boolean sameStoredBytes(EditorialAuthoritativeRunDeclaration a,
                                    EditorialAuthoritativeRunDeclaration b) {
        return a.declarationIdentity().equals(b.declarationIdentity())
                && a.declarationFingerprint().equals(b.declarationFingerprint())
                && a.identityProjection().equals(b.identityProjection())
                && a.fingerprintProjection().equals(b.fingerprintProjection())
                && a.runAttemptOrdinal() == b.runAttemptOrdinal()
                && a.createdAt() == b.createdAt();
    }

    private static boolean isHash(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static <T> EditorialIdentityAppendResult<T> result(
            EditorialIdentityPersistenceCode code, T value, String identity, String detail) {
        return EditorialIdentityDaoSupport.result(code, value, identity, detail);
    }
}
