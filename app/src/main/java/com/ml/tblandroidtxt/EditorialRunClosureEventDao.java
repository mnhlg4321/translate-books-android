package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ml.tblandroidtxt.editorial.pack.EditorialAuthoritativeRunAllocationScope;
import com.ml.tblandroidtxt.editorial.pack.EditorialAuthoritativeRunDeclaration;
import com.ml.tblandroidtxt.editorial.pack.EditorialClosureEventEligibility;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialRunClosureEvent;
import com.ml.tblandroidtxt.editorial.pack.EditorialRunClosureEventDraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/** Append/read-only v18 closure-event DAO; lifecycle/trust orchestration remains outside A2. */
public final class EditorialRunClosureEventDao {
    private static final String TABLE = "editorial_run_closure_events";
    private static final String SELECT = "SELECT closure_event_identity,closure_event_fingerprint,"
            + "closure_event_contract_version,authoritative_run_identity,project_revision_identity,"
            + "input_scope_snapshot_identity,compatibility_evaluation_id,run_kind,phase_identity,"
            + "frozen_manifest_fingerprint,frozen_manifest_reference,node_kind,parent_record_identity,"
            + "run_attempt_ordinal,closure_eligibility,closure_attestation_version,"
            + "closure_attestation_fingerprint,appended_at FROM " + TABLE;

    private final TranslationRepository database;
    private final EditorialAuthoritativeRunDeclarationDao declarations;
    private final LongSupplier clock;

    public EditorialRunClosureEventDao(TranslationRepository database) {
        this(database, System::currentTimeMillis);
    }

    EditorialRunClosureEventDao(TranslationRepository database, LongSupplier clock) {
        this.database = Objects.requireNonNull(database, "database");
        this.declarations = new EditorialAuthoritativeRunDeclarationDao(database, clock);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Package-private by design: only the future lifecycle owner may obtain the
     * trusted append permit. A2 exposes no public fabricated-eligibility path.
     */
    EditorialIdentityAppendResult<EditorialRunClosureEvent> append(
            EditorialRunClosureEventDraft draft,
            EditorialClosureEventAppendPermit permit) {
        if (draft == null) return result(EditorialIdentityPersistenceCode.INVALID_IMMUTABLE_RECORD,
                null, "", "closure-event-draft-required");
        if (permit == null || !permit.trustedDataCompatible
                || draft.closureEligibility() != EditorialClosureEventEligibility.ELIGIBLE) {
            return result(EditorialIdentityPersistenceCode.TRUSTED_CONTEXT_MISMATCH,
                    null, draft.authoritativeRunIdentity(), "trusted-data-compatible-permit-required");
        }
        if (!draft.compatibilityEvaluationId().equals(permit.compatibilityEvaluationId)
                || !draft.frozenManifestFingerprint().equals(permit.frozenManifestFingerprint)
                || !draft.closureAttestationVersion().equals(permit.closureAttestationVersion)
                || !draft.closureAttestationFingerprint().equals(permit.closureAttestationFingerprint)) {
            return result(EditorialIdentityPersistenceCode.TRUSTED_CONTEXT_MISMATCH,
                    null, draft.authoritativeRunIdentity(), "trusted-facts-do-not-match-event");
        }
        final long appendedAt;
        try {
            appendedAt = clock.getAsLong();
        } catch (RuntimeException error) {
            return result(EditorialIdentityPersistenceCode.PERSISTENCE_FAILURE,
                    null, "", "closure-event-clock-failure");
        }
        if (appendedAt < 0) return result(EditorialIdentityPersistenceCode.INVALID_IMMUTABLE_RECORD,
                null, "", "closure-event-clock-invalid");

        SQLiteDatabase db = database.editorialWritableDatabase();
        try {
            db.beginTransaction();
            try {
                EditorialAuthoritativeRunDeclaration declaration =
                        declarations.findByIdentityInTransaction(db, draft.authoritativeRunIdentity());
                if (declaration == null) return result(EditorialIdentityPersistenceCode.FOREIGN_REFERENCE_MISSING,
                        null, draft.authoritativeRunIdentity(), "authoritative-declaration-not-found");
                EditorialIdentityPersistenceCode mismatch = crossCheck(declaration, draft);
                if (mismatch != null) return result(mismatch, null,
                        draft.authoritativeRunIdentity(), "declaration-event-cross-check-failed");
                if (draft.nodeKind() == EditorialLineageNodeKind.CHILD
                        && !EditorialIdentityDaoSupport.hasRow(db, "editorial_lineage_records",
                        "record_identity", draft.parentRecordIdentity())) {
                    return result(EditorialIdentityPersistenceCode.PARENT_NOT_FOUND, null,
                            draft.authoritativeRunIdentity(), "exact-parent-not-found");
                }
                EditorialRunClosureEvent event = EditorialRunClosureEvent.allocate(draft, appendedAt);
                EditorialRunClosureEvent existing = findByAuthoritativeRunIdentity(db,
                        draft.authoritativeRunIdentity());
                if (existing != null) {
                    db.setTransactionSuccessful();
                    return classifyExisting(existing, event);
                }
                insert(db, event);
                EditorialRunClosureEvent readback = findByIdentity(db, event.closureEventIdentity());
                if (readback == null || !sameStoredBytes(readback, event)) {
                    return result(EditorialIdentityPersistenceCode.PERSISTENCE_FAILURE, null,
                            event.closureEventIdentity(), "closure-event-exact-readback-failed");
                }
                db.setTransactionSuccessful();
                return result(EditorialIdentityPersistenceCode.APPENDED, readback,
                        event.closureEventIdentity(), "appended");
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteConstraintException error) {
            EditorialRunClosureEvent raced = findByAuthoritativeRunIdentity(
                    draft.authoritativeRunIdentity()).orElse(null);
            if (raced != null) return classifyExisting(raced,
                    EditorialRunClosureEvent.allocate(draft, appendedAt));
            return result(EditorialIdentityPersistenceCode.PERSISTENCE_FAILURE,
                    null, draft.authoritativeRunIdentity(), "closure-event-insert-rolled-back");
        } catch (SQLiteException | IllegalStateException error) {
            return result(EditorialIdentityPersistenceCode.PERSISTENCE_FAILURE,
                    null, draft.authoritativeRunIdentity(), "closure-event-transaction-rolled-back");
        }
    }

    public Optional<EditorialRunClosureEvent> findByClosureEventIdentity(String identity) {
        if (!isHash(identity)) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE closure_event_identity=?", new String[]{identity})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(read(cursor));
        } catch (RuntimeException invalidStoredRow) {
            return Optional.empty();
        }
    }

    public Optional<EditorialRunClosureEvent> findByAuthoritativeRunIdentity(String identity) {
        if (!isHash(identity)) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE authoritative_run_identity=?", new String[]{identity})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(read(cursor));
        } catch (RuntimeException invalidStoredRow) {
            return Optional.empty();
        }
    }

    public List<EditorialRunClosureEvent> listByContext(EditorialAuthoritativeRunAllocationScope scope) {
        if (scope == null) return List.of();
        String sql = SELECT + " WHERE project_revision_identity=? AND input_scope_snapshot_identity=?"
                + " AND compatibility_evaluation_id=? AND run_kind=? AND phase_identity=?"
                + " AND frozen_manifest_fingerprint=? ORDER BY run_attempt_ordinal ASC,closure_event_identity ASC";
        ArrayList<EditorialRunClosureEvent> result = new ArrayList<>();
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

    private EditorialIdentityPersistenceCode crossCheck(
            EditorialAuthoritativeRunDeclaration declaration,
            EditorialRunClosureEventDraft event) {
        if (!declaration.projectRevisionIdentity().equals(event.projectRevisionIdentity())
                || !declaration.inputScopeSnapshotIdentity().equals(event.inputScopeSnapshotIdentity())
                || !declaration.compatibilityEvaluationId().equals(event.compatibilityEvaluationId())
                || !declaration.runKind().equals(event.runKind())
                || !declaration.phaseIdentity().equals(event.phaseIdentity())
                || !declaration.frozenManifestFingerprint().equals(event.frozenManifestFingerprint())
                || !declaration.frozenManifestReference().equals(event.frozenManifestReference())
                || declaration.runAttemptOrdinal() != event.runAttemptOrdinal()) {
            if (!declaration.frozenManifestFingerprint().equals(event.frozenManifestFingerprint())) {
                return EditorialIdentityPersistenceCode.FROZEN_MANIFEST_MISMATCH;
            }
            if (declaration.runAttemptOrdinal() != event.runAttemptOrdinal()) {
                return EditorialIdentityPersistenceCode.ORDINAL_MISMATCH;
            }
            return EditorialIdentityPersistenceCode.EVENT_DECLARATION_MISMATCH;
        }
        if (declaration.nodeKind() != event.nodeKind()) {
            return EditorialIdentityPersistenceCode.EVENT_INTENT_COLLISION;
        }
        if (!Objects.equals(declaration.parentRecordIdentity(), event.parentRecordIdentity())) {
            return EditorialIdentityPersistenceCode.EVENT_PARENT_COLLISION;
        }
        return null;
    }

    private void insert(SQLiteDatabase db, EditorialRunClosureEvent event) {
        ContentValues row = new ContentValues();
        row.put("closure_event_identity", event.closureEventIdentity());
        row.put("closure_event_fingerprint", event.closureEventFingerprint());
        row.put("closure_event_contract_version", event.closureEventContractVersion());
        row.put("authoritative_run_identity", event.authoritativeRunIdentity());
        row.put("project_revision_identity", event.projectRevisionIdentity());
        row.put("input_scope_snapshot_identity", event.inputScopeSnapshotIdentity());
        row.put("compatibility_evaluation_id", event.compatibilityEvaluationId());
        row.put("run_kind", event.runKind());
        row.put("phase_identity", event.phaseIdentity());
        row.put("frozen_manifest_fingerprint", event.frozenManifestFingerprint());
        row.put("frozen_manifest_reference", event.frozenManifestReference());
        row.put("node_kind", event.nodeKind().name());
        if (event.parentRecordIdentity() == null) row.putNull("parent_record_identity");
        else row.put("parent_record_identity", event.parentRecordIdentity());
        row.put("run_attempt_ordinal", event.runAttemptOrdinal());
        row.put("closure_eligibility", event.closureEligibility().name());
        row.put("closure_attestation_version", event.closureAttestationVersion());
        row.put("closure_attestation_fingerprint", event.closureAttestationFingerprint());
        row.put("appended_at", event.appendedAt());
        db.insertOrThrow(TABLE, null, row);
    }

    private EditorialRunClosureEvent read(Cursor cursor) {
        if (!EditorialRunClosureEventDraft.CONTRACT_VERSION.equals(cursor.getString(2))) {
            throw new IllegalArgumentException("stored closure-event contract version mismatch");
        }
        EditorialRunClosureEventDraft draft = new EditorialRunClosureEventDraft(
                cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6),
                cursor.getString(7), cursor.getString(8), cursor.getString(9), cursor.getString(10),
                EditorialLineageNodeKind.valueOf(cursor.getString(11)),
                cursor.isNull(12) ? null : cursor.getString(12), cursor.getLong(13),
                EditorialClosureEventEligibility.valueOf(cursor.getString(14)), cursor.getString(15),
                cursor.getString(16));
        return EditorialRunClosureEvent.fromStored(cursor.getString(0), cursor.getString(1),
                draft, cursor.getLong(17));
    }

    private EditorialRunClosureEvent findByIdentity(SQLiteDatabase db, String identity) {
        try (Cursor cursor = db.rawQuery(SELECT + " WHERE closure_event_identity=?",
                new String[]{identity})) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    private EditorialRunClosureEvent findByAuthoritativeRunIdentity(SQLiteDatabase db, String identity) {
        try (Cursor cursor = db.rawQuery(SELECT + " WHERE authoritative_run_identity=?",
                new String[]{identity})) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    private EditorialIdentityAppendResult<EditorialRunClosureEvent> classifyExisting(
            EditorialRunClosureEvent existing, EditorialRunClosureEvent incoming) {
        if (existing.closureEventIdentity().equals(incoming.closureEventIdentity())
                && existing.closureEventFingerprint().equals(incoming.closureEventFingerprint())
                && existing.identityProjection().equals(incoming.identityProjection())
                && existing.fingerprintProjection().equals(incoming.fingerprintProjection())) {
            return result(EditorialIdentityPersistenceCode.ALREADY_EXISTS, existing,
                    existing.closureEventIdentity(), "same-immutable-event");
        }
        if (existing.nodeKind() != incoming.nodeKind()) {
            return result(EditorialIdentityPersistenceCode.EVENT_INTENT_COLLISION, null,
                    existing.closureEventIdentity(), "same-declaration-root-child-change");
        }
        if (!Objects.equals(existing.parentRecordIdentity(), incoming.parentRecordIdentity())) {
            return result(EditorialIdentityPersistenceCode.EVENT_PARENT_COLLISION, null,
                    existing.closureEventIdentity(), "same-declaration-parent-change");
        }
        return result(EditorialIdentityPersistenceCode.IMMUTABLE_COLLISION, null,
                existing.closureEventIdentity(), "same-declaration-different-event-bytes");
    }

    private boolean sameStoredBytes(EditorialRunClosureEvent a, EditorialRunClosureEvent b) {
        return a.closureEventIdentity().equals(b.closureEventIdentity())
                && a.closureEventFingerprint().equals(b.closureEventFingerprint())
                && a.identityProjection().equals(b.identityProjection())
                && a.fingerprintProjection().equals(b.fingerprintProjection())
                && a.appendedAt() == b.appendedAt();
    }

    private static boolean isHash(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static <T> EditorialIdentityAppendResult<T> result(
            EditorialIdentityPersistenceCode code, T value, String identity, String detail) {
        return EditorialIdentityDaoSupport.result(code, value, identity, detail);
    }
}
