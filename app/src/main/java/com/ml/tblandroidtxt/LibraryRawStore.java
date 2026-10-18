package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Imported RAW chapters: an index row in SQLite ({@code library_raw}) plus the text as a file under filesDir/library_raw. */
final class LibraryRawStore {
    static final String DIR = "library_raw";

    static final class Item {
        String id = UUID.randomUUID().toString();
        String name = "";
        String originalName = "";
        int chapterNo = ChapterNumber.NONE;
        int chars;
        String inputHash = "";
        String encoding = "";
        long createdAt = System.currentTimeMillis();
        long updatedAt = createdAt;

        String uri() { return RawUri.of(id); }
        String chapterLabel() { return chapterNo >= 0 ? "Chương " + chapterNo : "Không có số chương"; }
    }

    /** A running or unfinished job that reads this RAW. */
    static final class Usage {
        final long jobId;
        final String fileName;
        final String status;
        Usage(long jobId, String fileName, String status) { this.jobId = jobId; this.fileName = fileName; this.status = status; }
    }

    private LibraryRawStore() {}

    static RawFileStore files(Context c) { return new RawFileStore(new File(c.getFilesDir(), DIR)); }

    static List<Item> list(Context c) {
        ArrayList<Item> out = new ArrayList<>();
        TranslationRepository repo = new TranslationRepository(c);
        try (Cursor cur = repo.getReadableDatabase().rawQuery("SELECT id,name,original_name,chapter_no,chars,input_hash,encoding,created_at,updated_at FROM library_raw", null)) {
            while (cur.moveToNext()) out.add(read(cur));
        } finally { repo.close(); }
        sort(out);
        return out;
    }

    static void sort(List<Item> items) {
        Collections.sort(items, (a, b) -> ChapterNumber.compare(a.chapterNo, a.originalName, b.chapterNo, b.originalName));
    }

    static Item find(Context c, String id) {
        if (id == null || id.isEmpty()) return null;
        TranslationRepository repo = new TranslationRepository(c);
        try (Cursor cur = repo.getReadableDatabase().rawQuery("SELECT id,name,original_name,chapter_no,chars,input_hash,encoding,created_at,updated_at FROM library_raw WHERE id=?", new String[]{id})) {
            return cur.moveToFirst() ? read(cur) : null;
        } finally { repo.close(); }
    }

    static Set<String> hashes(Context c) {
        HashSet<String> out = new HashSet<>();
        TranslationRepository repo = new TranslationRepository(c);
        try (Cursor cur = repo.getReadableDatabase().rawQuery("SELECT input_hash FROM library_raw", null)) {
            while (cur.moveToNext()) out.add(cur.getString(0));
        } finally { repo.close(); }
        return out;
    }

    /** Writes the planned additions (file first, then the index row); returns the items actually added. */
    static List<Item> add(Context c, RawImportPlanner.Plan plan) throws IOException {
        ArrayList<Item> added = new ArrayList<>();
        RawFileStore store = files(c);
        TranslationRepository repo = new TranslationRepository(c);
        try {
            SQLiteDatabase db = repo.getWritableDatabase();
            for (RawImportPlanner.Entry e : plan.entries) {
                if (e.verdict != RawImportPlanner.Verdict.ADD) continue;
                Item item = new Item();
                item.name = stripExtension(e.candidate.name);
                item.originalName = e.candidate.name;
                item.chapterNo = e.chapterNo;
                item.chars = e.normalized.length();
                item.inputHash = e.hash;
                item.encoding = e.candidate.encoding;
                store.write(item.id, e.normalized);
                db.insertOrThrow("library_raw", null, values(item));
                added.add(item);
            }
        } finally { repo.close(); }
        return added;
    }

    /** Puts items back with their old ids (undo of a delete); their files must still exist. */
    static void restore(Context c, List<Item> items) {
        TranslationRepository repo = new TranslationRepository(c);
        try {
            SQLiteDatabase db = repo.getWritableDatabase();
            db.beginTransaction();
            try {
                for (Item item : items) if (files(c).exists(item.id)) db.insertWithOnConflict("library_raw", null, values(item), SQLiteDatabase.CONFLICT_REPLACE);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        } finally { repo.close(); }
    }

    /** Removes the index rows only; the files stay until {@link #purgeFiles} so a delete can be undone. */
    static void removeRows(Context c, List<String> ids) {
        TranslationRepository repo = new TranslationRepository(c);
        try {
            SQLiteDatabase db = repo.getWritableDatabase();
            db.beginTransaction();
            try {
                for (String id : ids) db.delete("library_raw", "id=?", new String[]{id});
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        } finally { repo.close(); }
    }

    static void purgeFiles(Context c, List<String> ids) {
        RawFileStore store = files(c);
        for (String id : ids) store.delete(id);
    }

    /** Startup sweep: files with no index row and half-written leftovers. */
    static void sweepOrphans(Context c) {
        try {
            HashSet<String> keep = new HashSet<>();
            for (Item i : list(c)) keep.add(i.id);
            files(c).purgeExcept(keep);
        } catch (Exception ignored) { }
    }

    static boolean rename(Context c, String id, String newName) {
        String name = newName == null ? "" : newName.trim();
        if (name.isEmpty()) return false;
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("updated_at", System.currentTimeMillis());
        TranslationRepository repo = new TranslationRepository(c);
        try { return repo.getWritableDatabase().update("library_raw", v, "id=?", new String[]{id}) == 1; }
        finally { repo.close(); }
    }

    /** Replaces the text of one RAW (same id, so chapter sets keep pointing at it). */
    static void replace(Context c, String id, RawImportPlanner.Entry e) throws IOException {
        files(c).write(id, e.normalized);
        ContentValues v = new ContentValues();
        v.put("original_name", e.candidate.name);
        v.put("chapter_no", e.chapterNo >= 0 ? e.chapterNo : null);
        v.put("chars", e.normalized.length());
        v.put("input_hash", e.hash);
        v.put("encoding", e.candidate.encoding);
        v.put("updated_at", System.currentTimeMillis());
        TranslationRepository repo = new TranslationRepository(c);
        try { repo.getWritableDatabase().update("library_raw", v, "id=?", new String[]{id}); }
        finally { repo.close(); }
    }

    /** Jobs that still need this RAW (resume reads it again), i.e. not finished. */
    static List<Usage> unfinishedJobs(Context c, String id) {
        ArrayList<Usage> out = new ArrayList<>();
        TranslationRepository repo = new TranslationRepository(c);
        try (Cursor cur = repo.getReadableDatabase().rawQuery("SELECT id,file_name,status FROM jobs WHERE input_uri=? AND UPPER(COALESCE(status,'')) NOT IN ('DONE','COMPLETED','COMPLETED_WITH_WARNINGS','PASSED','CANCELLED')", new String[]{RawUri.of(id)})) {
            while (cur.moveToNext()) out.add(new Usage(cur.getLong(0), cur.getString(1), cur.getString(2)));
        } finally { repo.close(); }
        return out;
    }

    /** Text and name for {@link FileUtil}; null when the RAW is unknown. */
    static String displayNameOf(Context c, String id) {
        Item item = find(c, id);
        return item == null ? null : item.originalName;
    }

    static String stripExtension(String name) {
        String n = name == null ? "" : name.trim();
        return n.toLowerCase(java.util.Locale.ROOT).endsWith(".txt") && n.length() > 4 ? n.substring(0, n.length() - 4) : n;
    }

    private static ContentValues values(Item item) {
        ContentValues v = new ContentValues();
        v.put("id", item.id);
        v.put("name", item.name);
        v.put("original_name", item.originalName);
        if (item.chapterNo >= 0) v.put("chapter_no", item.chapterNo); else v.putNull("chapter_no");
        v.put("chars", item.chars);
        v.put("input_hash", item.inputHash);
        v.put("encoding", item.encoding);
        v.put("created_at", item.createdAt);
        v.put("updated_at", item.updatedAt);
        return v;
    }

    private static Item read(Cursor cur) {
        Item i = new Item();
        i.id = cur.getString(0);
        i.name = cur.getString(1);
        i.originalName = cur.getString(2);
        i.chapterNo = cur.isNull(3) ? ChapterNumber.NONE : cur.getInt(3);
        i.chars = cur.getInt(4);
        i.inputHash = cur.getString(5);
        i.encoding = cur.getString(6);
        i.createdAt = cur.getLong(7);
        i.updatedAt = cur.getLong(8);
        return i;
    }
}
