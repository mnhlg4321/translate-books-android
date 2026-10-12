package com.ml.tblandroidtxt;

import android.database.Cursor;

import com.ml.tblandroidtxt.editorial.pack.EditorialLineageFingerprint;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputEntry;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageInputManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageParentReference;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageRecord;

import java.util.List;

/** Maps immutable SQLite rows back to the pure-JVM lineage source of truth. */
final class EditorialLineageRecordRowMapper {
    private EditorialLineageRecordRowMapper() { }

    static EditorialLineageRecord readRecord(Cursor cursor, List<EditorialLineageInputEntry> entries) {
        EditorialLineageInputManifest manifest = new EditorialLineageInputManifest(
                cursor.getString(12), entries);
        EditorialLineageIdentity identity = new EditorialLineageIdentity(
                cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5),
                cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9),
                cursor.getString(10), cursor.getString(11), manifest,
                EditorialLineageNodeKind.valueOf(cursor.getString(14)));
        EditorialLineageParentReference parent = null;
        if (!cursor.isNull(15) || !cursor.isNull(16)) {
            parent = new EditorialLineageParentReference(cursor.getString(15), cursor.getString(16));
        }
        EditorialLineageFingerprint fingerprint = new EditorialLineageFingerprint(
                cursor.getString(13), cursor.getString(0), cursor.getString(1));
        return EditorialLineageRecord.fromDeclared(identity, parent, fingerprint);
    }

    static EditorialLineageInputEntry readInputEntry(Cursor cursor) {
        return new EditorialLineageInputEntry(cursor.getString(1), cursor.getInt(2), cursor.getString(3),
                cursor.getLong(4), cursor.getLong(5));
    }
}
