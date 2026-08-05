package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ml.tblandroidtxt.editorial.pack.EditorialProjectRevision;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Append/read-only DAO for explicit canonical project revisions. */
public final class EditorialProjectRevisionDao {
    private static final String TABLE = "editorial_project_revisions";
    private static final String SELECT = "SELECT revision_identity,revision_canonical_version,project_semantic_key,"
            + "project_definition_contract_version,semantic_project_type,scope_policy_fingerprint,"
            + "workflow_policy_fingerprint,project_definition_fingerprint,project_definition_canonical,"
            + "source_project_row_id,created_at FROM " + TABLE;
    private final TranslationRepository database;

    public EditorialProjectRevisionDao(TranslationRepository database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public EditorialIdentityAppendResult<EditorialProjectRevision> append(
            EditorialProjectRevision revision, Long sourceProjectRowId, long createdAt) {
        if (revision == null || createdAt < 0) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.INVALID_IMMUTABLE_RECORD,
                    null, revision == null ? "" : revision.revisionIdentity(), "revision-or-timestamp-invalid");
        }
        Optional<EditorialProjectRevision> existing = findByIdentity(revision.revisionIdentity());
        if (existing.isPresent()) return classify(existing.get(), revision);
        SQLiteDatabase db = database.editorialWritableDatabase();
        if (sourceProjectRowId != null && !hasSourceProject(db, sourceProjectRowId)) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.FOREIGN_KEY_RESTRICTED,
                    null, revision.revisionIdentity(), "source-project-row-missing");
        }
        try {
            db.beginTransaction();
            try {
                Optional<EditorialProjectRevision> raced = findByIdentity(revision.revisionIdentity());
                if (raced.isPresent()) {
                    db.setTransactionSuccessful();
                    return classify(raced.get(), revision);
                }
                ContentValues row = new ContentValues();
                row.put("revision_identity", revision.revisionIdentity());
                row.put("revision_canonical_version", revision.canonicalProjectionVersion());
                row.put("project_semantic_key", revision.projectSemanticKey());
                row.put("project_definition_contract_version", revision.projectDefinitionContractVersion());
                if (revision.semanticProjectType() == null) row.putNull("semantic_project_type");
                else row.put("semantic_project_type", revision.semanticProjectType());
                row.put("scope_policy_fingerprint", revision.scopePolicyFingerprint());
                row.put("workflow_policy_fingerprint", revision.workflowPolicyFingerprint());
                row.put("project_definition_fingerprint", revision.projectDefinitionFingerprint());
                row.put("project_definition_canonical", revision.canonicalProjection());
                if (sourceProjectRowId == null) row.putNull("source_project_row_id");
                else row.put("source_project_row_id", sourceProjectRowId);
                row.put("created_at", createdAt);
                db.insertOrThrow(TABLE, null, row);
                db.setTransactionSuccessful();
                return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.APPENDED,
                        revision, revision.revisionIdentity(), "appended");
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteConstraintException error) {
            Optional<EditorialProjectRevision> raced = findByIdentity(revision.revisionIdentity());
            if (raced.isPresent()) return classify(raced.get(), revision);
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                    null, revision.revisionIdentity(), "revision-insert-rollback");
        } catch (SQLiteException | IllegalStateException error) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                    null, revision.revisionIdentity(), "revision-transaction-rollback");
        }
    }

    public Optional<EditorialProjectRevision> findByIdentity(String identity) {
        if (identity == null || identity.isBlank()) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE revision_identity=?", new String[]{identity})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(read(cursor));
        } catch (RuntimeException invalidStoredRow) {
            return Optional.empty();
        }
    }

    public List<EditorialProjectRevision> listBySemanticKey(String semanticKey) {
        if (semanticKey == null || semanticKey.isBlank()) return List.of();
        ArrayList<EditorialProjectRevision> result = new ArrayList<>();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE project_semantic_key=? ORDER BY revision_identity ASC",
                new String[]{semanticKey})) {
            while (cursor.moveToNext()) {
                try { result.add(read(cursor)); } catch (RuntimeException ignored) { return List.of(); }
            }
        }
        return List.copyOf(result);
    }

    private boolean hasSourceProject(SQLiteDatabase db, long rowId) {
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM editorial_projects WHERE id=? LIMIT 1",
                new String[]{String.valueOf(rowId)})) { return cursor.moveToFirst(); }
    }

    private EditorialIdentityAppendResult<EditorialProjectRevision> classify(
            EditorialProjectRevision existing, EditorialProjectRevision incoming) {
        if (existing.canonicalProjection().equals(incoming.canonicalProjection())) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                    existing, incoming.revisionIdentity(), "same-immutable-record");
        }
        return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.DUPLICATE_IMMUTABLE_RECORD,
                null, incoming.revisionIdentity(), "identity-content-changed");
    }

    private EditorialProjectRevision read(Cursor cursor) {
        return new EditorialProjectRevision(cursor.getString(1), cursor.getString(2), cursor.getString(3),
                cursor.isNull(4) ? null : cursor.getString(4), cursor.getString(5), cursor.getString(6));
    }
}
