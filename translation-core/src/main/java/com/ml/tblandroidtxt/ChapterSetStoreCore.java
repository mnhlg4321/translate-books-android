package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Chapter sets: a named list of rows, each row pairing one RAW with at most one Glossary and one Pronoun profile, confirmed by the owner. */
class ChapterSetStoreCore {
    static final class Row {
        int chapterNo = ChapterNumber.NONE;
        String rawId = "";
        String glossaryId = "";
        String pronounId = "";

        Row() { }
        Row(int chapterNo, String rawId, String glossaryId, String pronounId) {
            this.chapterNo = chapterNo; this.rawId = rawId == null ? "" : rawId;
            this.glossaryId = glossaryId == null ? "" : glossaryId; this.pronounId = pronounId == null ? "" : pronounId;
        }
    }

    static final class SetData {
        String id = UUID.randomUUID().toString();
        String name = "";
        long createdAt = System.currentTimeMillis();
        long updatedAt = createdAt;
        final List<Row> rows = new ArrayList<>();
    }

    static final class SetInfo {
        String id = "";
        String name = "";
        int chapters;
        long updatedAt;
    }

    ChapterSetStoreCore() {}

    static List<SetInfo> listSets(SqlDb db) {
        ArrayList<SetInfo> out = new ArrayList<>();
        try (DbCursor cur = db.rawQuery("SELECT s.id,s.name,s.updated_at,(SELECT COUNT(*) FROM chapter_set_rows r WHERE r.set_id=s.id) FROM chapter_sets s ORDER BY s.updated_at DESC", null)) {
            while (cur.moveToNext()) {
                SetInfo i = new SetInfo();
                i.id = cur.getString(0); i.name = cur.getString(1); i.updatedAt = cur.getLong(2); i.chapters = cur.getInt(3);
                out.add(i);
            }
        }
        return out;
    }

    static SetData get(SqlDb db, String id) {
        SetData data;
        try (DbCursor cur = db.rawQuery("SELECT id,name,created_at,updated_at FROM chapter_sets WHERE id=?", new String[]{id})) {
            if (!cur.moveToFirst()) return null;
            data = new SetData();
            data.id = cur.getString(0); data.name = cur.getString(1); data.createdAt = cur.getLong(2); data.updatedAt = cur.getLong(3);
        }
        try (DbCursor cur = db.rawQuery("SELECT chapter_no,raw_id,glossary_id,pronoun_id FROM chapter_set_rows WHERE set_id=? ORDER BY ordinal", new String[]{id})) {
            while (cur.moveToNext()) data.rows.add(new Row(cur.isNull(0) ? ChapterNumber.NONE : cur.getInt(0), cur.getString(1), cur.getString(2), cur.getString(3)));
        }
        return data;
    }

    /** Saves (insert or replace) a whole set in one transaction. */
    static void save(SqlDb db, SetData data) {
        db.beginTransaction();
        try {
            data.updatedAt = System.currentTimeMillis();
            DbValues s = new DbValues();
            s.put("id", data.id); s.put("name", data.name); s.put("created_at", data.createdAt); s.put("updated_at", data.updatedAt);
            db.insertWithOnConflict("chapter_sets", null, s, SqlDb.CONFLICT_REPLACE);
            db.delete("chapter_set_rows", "set_id=?", new String[]{data.id});
            for (int i = 0; i < data.rows.size(); i++) {
                Row r = data.rows.get(i);
                DbValues v = new DbValues();
                v.put("set_id", data.id); v.put("ordinal", i);
                if (r.chapterNo >= 0) v.put("chapter_no", r.chapterNo); else v.putNull("chapter_no");
                v.put("raw_id", r.rawId); v.put("glossary_id", r.glossaryId); v.put("pronoun_id", r.pronounId);
                db.insertOrThrow("chapter_set_rows", null, v);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    static void delete(SqlDb db, List<String> ids) {
        db.beginTransaction();
        try {
            for (String id : ids) {
                db.delete("chapter_set_rows", "set_id=?", new String[]{id});
                db.delete("chapter_sets", "id=?", new String[]{id});
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

}
