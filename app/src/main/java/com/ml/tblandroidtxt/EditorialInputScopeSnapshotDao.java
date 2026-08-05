package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshot;
import com.ml.tblandroidtxt.editorial.pack.EditorialInputScopeSnapshotEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialRequiredInputRoleContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Append/read-only DAO for complete project-bound input scope snapshots. */
public final class EditorialInputScopeSnapshotDao {
    private static final String TABLE = "editorial_input_scope_snapshots";
    private static final String SELECT = "SELECT scope_snapshot_identity,project_revision_identity,"
            + "scope_canonical_version,canonical_scope_key,required_roles_canonical,required_roles_fingerprint,"
            + "manifest_version,manifest_fingerprint,source_chapter_row_id,created_at FROM " + TABLE;
    private final TranslationRepository database;

    public EditorialInputScopeSnapshotDao(TranslationRepository database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /** Appends the snapshot and all entries atomically; no partial snapshot is observable. */
    public EditorialIdentityAppendResult<EditorialInputScopeSnapshot> appendWithEntries(
            EditorialInputScopeSnapshot snapshot, Long sourceChapterRowId, long createdAt) {
        if (snapshot == null || createdAt < 0) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.INVALID_IMMUTABLE_RECORD,
                    null, snapshot == null ? "" : snapshot.scopeSnapshotIdentity(), "snapshot-or-timestamp-invalid");
        }
        Optional<EditorialInputScopeSnapshot> existing = findByIdentity(snapshot.scopeSnapshotIdentity());
        if (existing.isPresent()) return classify(existing.get(), snapshot);
        SQLiteDatabase db = database.editorialWritableDatabase();
        EditorialIdentityPersistenceCode precondition = validatePreconditions(db, snapshot, sourceChapterRowId);
        if (precondition != null) {
            return EditorialIdentityDaoSupport.result(precondition, null,
                    snapshot.scopeSnapshotIdentity(), "snapshot-precondition-failed");
        }
        try {
            db.beginTransaction();
            try {
                Optional<EditorialInputScopeSnapshot> raced = findByIdentity(snapshot.scopeSnapshotIdentity());
                if (raced.isPresent()) {
                    db.setTransactionSuccessful();
                    return classify(raced.get(), snapshot);
                }
                insertSnapshot(db, snapshot, sourceChapterRowId, createdAt);
                for (EditorialInputScopeSnapshotEntry entry : snapshot.entries()) {
                    ContentValues row = new ContentValues();
                    row.put("scope_snapshot_identity", snapshot.scopeSnapshotIdentity());
                    row.put("role", entry.role());
                    row.put("ordinal", entry.ordinal());
                    row.put("input_sha256", entry.inputSha256());
                    row.put("byte_count", entry.byteCount());
                    row.put("item_count", entry.itemCount());
                    db.insertOrThrow("editorial_input_scope_snapshot_entries", null, row);
                }
                db.setTransactionSuccessful();
                return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.APPENDED,
                        snapshot, snapshot.scopeSnapshotIdentity(), "appended");
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteConstraintException error) {
            Optional<EditorialInputScopeSnapshot> raced = findByIdentity(snapshot.scopeSnapshotIdentity());
            if (raced.isPresent()) return classify(raced.get(), snapshot);
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                    null, snapshot.scopeSnapshotIdentity(), "snapshot-transaction-rollback");
        } catch (SQLiteException | IllegalStateException error) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.LINEAGE_PERSISTENCE_FAILURE,
                    null, snapshot.scopeSnapshotIdentity(), "snapshot-transaction-rollback");
        }
    }

    public Optional<EditorialInputScopeSnapshot> findByIdentity(String identity) {
        if (identity == null || identity.isBlank()) return Optional.empty();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE scope_snapshot_identity=?", new String[]{identity})) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(read(cursor));
        } catch (RuntimeException invalidStoredRow) {
            return Optional.empty();
        }
    }

    public List<EditorialInputScopeSnapshot> listByProjectRevision(String revisionIdentity) {
        if (revisionIdentity == null || revisionIdentity.isBlank()) return List.of();
        ArrayList<EditorialInputScopeSnapshot> result = new ArrayList<>();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                SELECT + " WHERE project_revision_identity=? ORDER BY canonical_scope_key ASC,scope_snapshot_identity ASC",
                new String[]{revisionIdentity})) {
            while (cursor.moveToNext()) {
                try { result.add(read(cursor)); } catch (RuntimeException ignored) { return List.of(); }
            }
        }
        return List.copyOf(result);
    }

    private EditorialIdentityPersistenceCode validatePreconditions(
            SQLiteDatabase db, EditorialInputScopeSnapshot snapshot, Long sourceChapterRowId) {
        if (!EditorialIdentityDaoSupport.hasRow(db, "editorial_project_revisions", "revision_identity",
                snapshot.projectRevisionIdentity())) return EditorialIdentityPersistenceCode.PROJECT_REVISION_REQUIRED;
        if (sourceChapterRowId != null && !EditorialIdentityDaoSupport.hasRow(
                db, "editorial_chapters", "id", String.valueOf(sourceChapterRowId))) {
            return EditorialIdentityPersistenceCode.FOREIGN_KEY_RESTRICTED;
        }
        return null;
    }

    private void insertSnapshot(SQLiteDatabase db, EditorialInputScopeSnapshot snapshot,
                                Long sourceChapterRowId, long createdAt) {
        ContentValues row = new ContentValues();
        row.put("scope_snapshot_identity", snapshot.scopeSnapshotIdentity());
        row.put("project_revision_identity", snapshot.projectRevisionIdentity());
        row.put("scope_canonical_version", snapshot.scopeCanonicalVersion());
        row.put("canonical_scope_key", snapshot.canonicalScopeKey());
        row.put("required_roles_canonical", snapshot.requiredRolesCanonical());
        row.put("required_roles_fingerprint", snapshot.requiredRolesFingerprint());
        row.put("manifest_version", snapshot.manifestVersion());
        row.put("manifest_fingerprint", snapshot.manifestFingerprint());
        if (sourceChapterRowId == null) row.putNull("source_chapter_row_id");
        else row.put("source_chapter_row_id", sourceChapterRowId);
        row.put("created_at", createdAt);
        db.insertOrThrow(TABLE, null, row);
    }

    private EditorialIdentityAppendResult<EditorialInputScopeSnapshot> classify(
            EditorialInputScopeSnapshot existing, EditorialInputScopeSnapshot incoming) {
        if (existing.equals(incoming)) {
            return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.ALREADY_EXISTS,
                    existing, incoming.scopeSnapshotIdentity(), "same-immutable-record");
        }
        return EditorialIdentityDaoSupport.result(EditorialIdentityPersistenceCode.DUPLICATE_IMMUTABLE_RECORD,
                null, incoming.scopeSnapshotIdentity(), "identity-content-changed");
    }

    private EditorialInputScopeSnapshot read(Cursor cursor) {
        EditorialRequiredInputRoleContract contract =
                EditorialIdentityDaoSupport.readRoleContract(cursor.getString(4));
        ArrayList<EditorialInputScopeSnapshotEntry> entries = new ArrayList<>();
        try (Cursor entryCursor = database.editorialReadableDatabase().rawQuery(
                "SELECT role,ordinal,input_sha256,byte_count,item_count FROM editorial_input_scope_snapshot_entries "
                        + "WHERE scope_snapshot_identity=? ORDER BY role ASC,ordinal ASC",
                new String[]{cursor.getString(0)})) {
            while (entryCursor.moveToNext()) {
                entries.add(new EditorialInputScopeSnapshotEntry(entryCursor.getString(0), entryCursor.getLong(1),
                        entryCursor.getString(2), entryCursor.getLong(3), entryCursor.getLong(4)));
            }
        }
        EditorialInputScopeSnapshot snapshot = new EditorialInputScopeSnapshot(
                cursor.getString(1), cursor.getString(2), cursor.getString(3), contract,
                cursor.getString(6), entries);
        if (!snapshot.scopeSnapshotIdentity().equals(cursor.getString(0))
                || !snapshot.requiredRolesFingerprint().equals(cursor.getString(5))
                || !snapshot.manifestFingerprint().equals(cursor.getString(7))) {
            throw new IllegalArgumentException("stored scope snapshot canonical bytes mismatch");
        }
        return snapshot;
    }
}
