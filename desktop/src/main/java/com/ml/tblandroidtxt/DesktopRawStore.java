package com.ml.tblandroidtxt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Imported RAW chapters on the PC: an index row in {@code library_raw} plus the text as a file under {@code <data>/library_raw}. */
public final class DesktopRawStore {
    public static final String DIR = "library_raw";

    public static final class Item {
        public String id = UUID.randomUUID().toString();
        public String name = "";
        public String originalName = "";
        public int chapterNo = ChapterNumber.NONE;
        public int chars;
        public String inputHash = "";
        public String encoding = "";
        public long createdAt = System.currentTimeMillis();
        public long updatedAt = createdAt;

        public String uri() { return RawUri.of(id); }
        public String chapterLabel() { return chapterNo >= 0 ? "Chương " + chapterNo : "Không có số chương"; }
    }

    /** A picked TXT file, already decoded. */
    public static final class Picked {
        public final String name;
        public final String text;
        public final String encoding;
        public Picked(String name, String text, String encoding) { this.name = name; this.text = text; this.encoding = encoding; }
    }

    private static final String COLUMNS = "id,name,original_name,chapter_no,chars,input_hash,encoding,created_at,updated_at";
    private final SqlDb db;
    private final RawFileStore files;

    public DesktopRawStore(SqlDb db, File dir) {
        this.db = db;
        this.files = new RawFileStore(dir);
    }

    public List<Item> list() {
        ArrayList<Item> out = new ArrayList<>();
        try (DbCursor cur = db.rawQuery("SELECT " + COLUMNS + " FROM library_raw", null)) {
            while (cur.moveToNext()) out.add(read(cur));
        }
        Collections.sort(out, (a, b) -> ChapterNumber.compare(a.chapterNo, a.originalName, b.chapterNo, b.originalName));
        return out;
    }

    public Item find(String id) {
        if (id == null || id.isEmpty()) return null;
        try (DbCursor cur = db.rawQuery("SELECT " + COLUMNS + " FROM library_raw WHERE id=?", new String[]{id})) {
            return cur.moveToFirst() ? read(cur) : null;
        }
    }

    public Set<String> hashes() {
        HashSet<String> out = new HashSet<>();
        try (DbCursor cur = db.rawQuery("SELECT input_hash FROM library_raw", null)) {
            while (cur.moveToNext()) out.add(cur.getString(0));
        }
        return out;
    }

    public String text(String id) throws IOException { return files.read(id); }

    /** Plans and writes the picked files: duplicates (same normalised hash) and invalid ones are skipped. Returns the plan. */
    public RawImportPlanner.Plan add(List<Picked> picked) throws IOException {
        ArrayList<RawImportPlanner.Candidate> candidates = new ArrayList<>();
        for (Picked p : picked) candidates.add(new RawImportPlanner.Candidate(p.name, p.text, p.encoding));
        RawImportPlanner.Plan plan = RawImportPlanner.plan(candidates, hashes());
        for (RawImportPlanner.Entry e : plan.entries) {
            if (e.verdict != RawImportPlanner.Verdict.ADD) continue;
            Item item = new Item();
            item.name = stripExtension(e.candidate.name);
            item.originalName = e.candidate.name;
            item.chapterNo = e.chapterNo;
            item.chars = e.normalized.length();
            item.inputHash = e.hash;
            item.encoding = e.candidate.encoding;
            files.write(item.id, e.normalized);
            db.insertOrThrow("library_raw", null, values(item));
        }
        return plan;
    }

    /** Adds RAW chapters read from a library ZIP (ids and metadata kept); the caller has already removed duplicates. */
    void addEntries(List<LibraryArchive.RawEntry> entries) throws IOException {
        for (LibraryArchive.RawEntry e : entries) {
            Item item = new Item();
            item.id = e.id; item.name = e.name; item.originalName = e.originalName; item.chapterNo = e.chapterNo;
            item.chars = e.text.length(); item.inputHash = e.inputHash; item.encoding = e.encoding;
            item.createdAt = e.createdAt > 0 ? e.createdAt : item.createdAt;
            item.updatedAt = e.updatedAt > 0 ? e.updatedAt : item.updatedAt;
            files.write(item.id, e.text);
            db.insertWithOnConflict("library_raw", null, values(item), SqlDb.CONFLICT_REPLACE);
        }
    }

    /** Removes the index rows and then the files. */
    public void delete(List<String> ids) {
        db.beginTransaction();
        try {
            for (String id : ids) db.delete("library_raw", "id=?", new String[]{id});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        for (String id : ids) files.delete(id);
    }

    /** Deletes only the index rows; the files stay until {@link #purgeFiles}, so a delete can still be undone. */
    void removeRows(List<String> ids) {
        db.beginTransaction();
        try {
            for (String id : ids) db.delete("library_raw", "id=?", new String[]{id});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    /** Puts index rows back (the files were kept). Rows whose file is gone, or whose id is present again, are skipped. */
    int restoreRows(List<Item> items) {
        int n = 0;
        for (Item item : items) {
            if (find(item.id) != null || !files.exists(item.id)) continue;
            db.insertOrThrow("library_raw", null, values(item));
            n++;
        }
        return n;
    }

    void purgeFiles(List<String> ids) { for (String id : ids) files.delete(id); }

    /** Replaces the text of one RAW, keeping its id so that chapter sets still point at it. The name stays as the person set it. */
    public void replace(String id, RawImportPlanner.Entry e) throws IOException {
        files.write(id, e.normalized);
        DbValues v = new DbValues();
        v.put("original_name", e.candidate.name);
        if (e.chapterNo >= 0) v.put("chapter_no", Integer.valueOf(e.chapterNo)); else v.putNull("chapter_no");
        v.put("chars", Integer.valueOf(e.normalized.length()));
        v.put("input_hash", e.hash);
        v.put("encoding", e.candidate.encoding);
        v.put("updated_at", Long.valueOf(System.currentTimeMillis()));
        db.update("library_raw", v, "id=?", new String[]{id});
    }

    /** Names of the chapter sets that list this RAW. */
    public List<String> chapterSetNames(String id) {
        ArrayList<String> out = new ArrayList<>();
        try (DbCursor cur = db.rawQuery("SELECT DISTINCT s.name FROM chapter_set_rows r JOIN chapter_sets s ON s.id=r.set_id WHERE r.raw_id=?", new String[]{id})) {
            while (cur.moveToNext()) out.add(cur.getString(0));
        }
        return out;
    }

    public boolean rename(String id, String newName) {
        String name = newName == null ? "" : newName.trim();
        if (name.isEmpty()) return false;
        DbValues v = new DbValues();
        v.put("name", name);
        v.put("updated_at", Long.valueOf(System.currentTimeMillis()));
        return db.update("library_raw", v, "id=?", new String[]{id}) == 1;
    }

    /** File names of jobs that still need this RAW (a resume reads it again). */
    public List<String> unfinishedJobNames(String id) {
        ArrayList<String> out = new ArrayList<>();
        try (DbCursor cur = db.rawQuery("SELECT id,file_name,status FROM jobs WHERE input_uri=? AND UPPER(COALESCE(status,'')) NOT IN ("
                + JobStatus.sqlIn(JobStatus.Kind.FINISHED, JobStatus.Kind.CANCELLED, JobStatus.Kind.DISMISSED) + ")", new String[]{RawUri.of(id)})) {
            while (cur.moveToNext()) out.add(cur.getString(1));
        }
        return out;
    }

    /** Input hashes that already have a finished translation job. */
    public Set<String> completedHashes() {
        HashSet<String> out = new HashSet<>();
        try (DbCursor cur = db.rawQuery("SELECT DISTINCT input_hash FROM jobs WHERE input_hash<>'' AND UPPER(COALESCE(status,'')) IN (" + JobStatus.sqlFinished() + ")", null)) {
            while (cur.moveToNext()) out.add(cur.getString(0));
        }
        return out;
    }

    List<LibraryArchive.RawEntry> entries() throws IOException {
        ArrayList<LibraryArchive.RawEntry> out = new ArrayList<>();
        for (Item item : list()) {
            LibraryArchive.RawEntry e = new LibraryArchive.RawEntry();
            e.id = item.id; e.name = item.name; e.originalName = item.originalName; e.encoding = item.encoding;
            e.inputHash = item.inputHash; e.chapterNo = item.chapterNo; e.createdAt = item.createdAt; e.updatedAt = item.updatedAt;
            e.text = files.read(item.id);
            e.chars = e.text.length();
            out.add(e);
        }
        return out;
    }

    public static String stripExtension(String name) {
        String n = name == null ? "" : name.trim();
        return n.toLowerCase(java.util.Locale.ROOT).endsWith(".txt") && n.length() > 4 ? n.substring(0, n.length() - 4) : n;
    }

    private static DbValues values(Item item) {
        DbValues v = new DbValues();
        v.put("id", item.id);
        v.put("name", item.name);
        v.put("original_name", item.originalName);
        if (item.chapterNo >= 0) v.put("chapter_no", Integer.valueOf(item.chapterNo)); else v.putNull("chapter_no");
        v.put("chars", Integer.valueOf(item.chars));
        v.put("input_hash", item.inputHash);
        v.put("encoding", item.encoding);
        v.put("created_at", Long.valueOf(item.createdAt));
        v.put("updated_at", Long.valueOf(item.updatedAt));
        return v;
    }

    private static Item read(DbCursor cur) {
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
