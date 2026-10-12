package com.ml.tblandroidtxt;

import android.database.Cursor;

import java.util.Objects;

/** Read-only guard for future deletion UX; it exposes no delete or detach operation. */
public final class EditorialIdentityRetentionPreflight {
    private final TranslationRepository database;

    public EditorialIdentityRetentionPreflight(TranslationRepository database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public EditorialIdentityRetentionCode projectDeletion(long sourceProjectRowId) {
        return referenced(sourceProjectRowId, "editorial_project_revisions", "source_project_row_id");
    }

    public EditorialIdentityRetentionCode runDeletion(long sourceRunRowId) {
        return referenced(sourceRunRowId, "editorial_closed_run_contexts", "source_run_row_id");
    }

    private EditorialIdentityRetentionCode referenced(long rowId, String table, String column) {
        if (rowId < 0) return EditorialIdentityRetentionCode.INVALID_SELECTION;
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT 1 FROM " + table + " WHERE " + column + "=? LIMIT 1",
                new String[]{String.valueOf(rowId)})) {
            return cursor.moveToFirst() ? EditorialIdentityRetentionCode.AUTHORITATIVE_REFERENCE_PRESENT
                    : EditorialIdentityRetentionCode.CLEAR_TO_DELETE;
        } catch (RuntimeException ignored) {
            return EditorialIdentityRetentionCode.INVALID_SELECTION;
        }
    }
}
