package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Durable project/chapter/evidence store. It does not execute models or render UI. */
public final class EditorialRepository implements AutoCloseable {
    public static final class Project {
        public long id;
        public String seriesName = "";
        public String volumeName = "";
        public String workflowVersion = EditorialEvidenceSchema.VERSION;
        public String workflowHash = "";
        public String outputTreeUri = "";
    }
    public static final class Chapter {
        public long id;
        public long projectId;
        public String chapterKey = "";
        public String title = "";
        public EditorialWorkflowV5.ChapterState state = EditorialWorkflowV5.ChapterState.DRAFT_INPUT;
        public String rawHash = "";
    }
    public static final class AssetSnapshot extends EditorialAssetManifest.Asset {
        public final String sourceUri;
        public final String displayName;
        public final String content;
        public AssetSnapshot(EditorialWorkflowV5.AssetRole role, String sourceUri, String displayName, String content) {
            super(role, HashUtil.sha256(content == null ? "" : content));
            this.sourceUri = safe(sourceUri);
            this.displayName = safe(displayName);
            this.content = safe(content);
        }
    }
    public static final class Run {
        public long id;
        public long chapterId;
        public String runKind = "";
        public String state = "";
    }

    private final TranslationRepository database;
    public EditorialRepository(Context context) { this(new TranslationRepository(context.getApplicationContext())); }
    EditorialRepository(TranslationRepository database) { this.database = database; }

    public long createProject(Project project) {
        if (project == null || blank(project.seriesName) || blank(project.volumeName) || blank(project.workflowHash)) throw new IllegalArgumentException("Project identity and workflow hash are required");
        ContentValues v = new ContentValues(); long now = System.currentTimeMillis();
        v.put("series_name", project.seriesName.trim()); v.put("volume_name", project.volumeName.trim());
        v.put("workflow_version", safe(project.workflowVersion)); v.put("workflow_hash", project.workflowHash.trim());
        v.put("output_tree_uri", safe(project.outputTreeUri)); v.put("created_at", now); v.put("updated_at", now);
        return database.editorialWritableDatabase().insertOrThrow("editorial_projects", null, v);
    }

    public long createChapter(long projectId, String chapterKey, String title, List<AssetSnapshot> assets) {
        if (projectId <= 0 || blank(chapterKey)) throw new IllegalArgumentException("Project and chapter key are required");
        String assetError = validateChapterAssets(assets); if (assetError != null) throw new IllegalArgumentException(assetError);
        AssetSnapshot raw = find(assets, EditorialWorkflowV5.AssetRole.RAW);
        SQLiteDatabase db = database.editorialWritableDatabase(); long now = System.currentTimeMillis(); db.beginTransaction();
        try {
            if (!exists(db, "editorial_projects", projectId)) throw new IllegalArgumentException("Editorial project not found");
            ContentValues chapter = new ContentValues(); chapter.put("project_id", projectId); chapter.put("chapter_key", chapterKey.trim()); chapter.put("title", safe(title));
            chapter.put("state", EditorialWorkflowV5.ChapterState.L1_READY.name()); chapter.put("raw_hash", raw.sha256); chapter.put("created_at", now); chapter.put("updated_at", now);
            long id = db.insertOrThrow("editorial_chapters", null, chapter);
            for (AssetSnapshot asset : assets) {
                ContentValues v = new ContentValues(); v.put("chapter_id", id); v.put("role", asset.role.name()); v.put("source_uri", asset.sourceUri);
                v.put("display_name", asset.displayName); v.put("sha256", asset.sha256); v.put("size_bytes", asset.content.length()); v.put("content", asset.content); v.put("created_at", now);
                db.insertOrThrow("editorial_assets", null, v);
            }
            db.setTransactionSuccessful(); return id;
        } finally { db.endTransaction(); }
    }

    public long createRun(long chapterId, String runKind, String provider, String model, String promptHash, String workflowHash, String inputManifestJson) {
        if (chapterId <= 0 || !("L1".equals(runKind) || "L2".equals(runKind) || "L3".equals(runKind))) throw new IllegalArgumentException("Invalid editorial run");
        if (blank(promptHash) || blank(workflowHash) || blank(inputManifestJson)) throw new IllegalArgumentException("Run hashes and input manifest are required");
        SQLiteDatabase db = database.editorialWritableDatabase(); if (!exists(db, "editorial_chapters", chapterId)) throw new IllegalArgumentException("Editorial chapter not found");
        ContentValues v = new ContentValues(); long now = System.currentTimeMillis(); v.put("chapter_id", chapterId); v.put("run_kind", runKind); v.put("state", "QUEUED");
        v.put("provider", safe(provider)); v.put("model", safe(model)); v.put("prompt_hash", promptHash); v.put("workflow_hash", workflowHash); v.put("input_manifest_json", inputManifestJson); v.put("created_at", now); v.put("updated_at", now);
        return db.insertOrThrow("editorial_runs", null, v);
    }

    public void saveScene(long runId, String sceneKey, String rawStart, String rawEnd, String status, String ledgerJson) {
        if (runId <= 0 || blank(sceneKey) || blank(rawStart) || blank(rawEnd) || blank(status) || blank(ledgerJson)) throw new IllegalArgumentException("Scene evidence is incomplete");
        ContentValues v = new ContentValues(); v.put("run_id", runId); v.put("scene_key", sceneKey); v.put("raw_start_anchor", rawStart); v.put("raw_end_anchor", rawEnd); v.put("status", status); v.put("ledger_json", ledgerJson);
        database.editorialWritableDatabase().insertWithOnConflict("editorial_scenes", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void saveGate(long runId, EditorialWorkflowV5.Gate gate, EditorialWorkflowV5.GateStatus status, String evidenceJson) {
        if (runId <= 0 || gate == null || status == null || blank(evidenceJson)) throw new IllegalArgumentException("Gate evidence is incomplete");
        ContentValues v = new ContentValues(); v.put("run_id", runId); v.put("gate_name", gate.name()); v.put("status", status.name()); v.put("evidence_json", evidenceJson); v.put("updated_at", System.currentTimeMillis());
        database.editorialWritableDatabase().insertWithOnConflict("editorial_gates", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long saveEvidence(long runId, String evidenceType, String payload) {
        if (runId <= 0 || blank(evidenceType) || blank(payload)) throw new IllegalArgumentException("Evidence type and payload are required");
        ContentValues v = new ContentValues(); v.put("run_id", runId); v.put("evidence_type", evidenceType); v.put("payload", payload); v.put("payload_hash", HashUtil.sha256(payload)); v.put("created_at", System.currentTimeMillis());
        return database.editorialWritableDatabase().insertOrThrow("editorial_evidence", null, v);
    }

    public Chapter getChapter(long id) {
        try (Cursor c = database.editorialReadableDatabase().rawQuery("SELECT id,project_id,chapter_key,title,state,raw_hash FROM editorial_chapters WHERE id=?", new String[]{String.valueOf(id)})) {
            if (!c.moveToFirst()) return null; Chapter chapter = new Chapter(); chapter.id=c.getLong(0); chapter.projectId=c.getLong(1); chapter.chapterKey=safe(c.getString(2)); chapter.title=safe(c.getString(3));
            try { chapter.state=EditorialWorkflowV5.ChapterState.valueOf(c.getString(4)); } catch (Exception ignored) {} chapter.rawHash=safe(c.getString(5)); return chapter;
        }
    }

    public List<Project> listProjects() {
        ArrayList<Project> projects=new ArrayList<>();
        try (Cursor c=database.editorialReadableDatabase().rawQuery("SELECT id,series_name,volume_name,workflow_version,workflow_hash,output_tree_uri FROM editorial_projects ORDER BY updated_at DESC,id DESC",null)) {
            while(c.moveToNext()){Project p=new Project();p.id=c.getLong(0);p.seriesName=safe(c.getString(1));p.volumeName=safe(c.getString(2));p.workflowVersion=safe(c.getString(3));p.workflowHash=safe(c.getString(4));p.outputTreeUri=safe(c.getString(5));projects.add(p);}
        } return projects;
    }

    public List<Chapter> listChapters(long projectId) {
        ArrayList<Chapter> chapters=new ArrayList<>();
        try (Cursor c=database.editorialReadableDatabase().rawQuery("SELECT id,project_id,chapter_key,title,state,raw_hash FROM editorial_chapters WHERE project_id=? ORDER BY chapter_key COLLATE NOCASE",new String[]{String.valueOf(projectId)})) {
            while(c.moveToNext()){Chapter chapter=new Chapter();chapter.id=c.getLong(0);chapter.projectId=c.getLong(1);chapter.chapterKey=safe(c.getString(2));chapter.title=safe(c.getString(3));try{chapter.state=EditorialWorkflowV5.ChapterState.valueOf(c.getString(4));}catch(Exception ignored){}chapter.rawHash=safe(c.getString(5));chapters.add(chapter);}
        } return chapters;
    }

    public void markStale(long chapterId) {
        ContentValues v = new ContentValues(); v.put("state", EditorialWorkflowV5.ChapterState.STALE.name()); v.put("updated_at", System.currentTimeMillis());
        database.editorialWritableDatabase().update("editorial_chapters", v, "id=?", new String[]{String.valueOf(chapterId)});
    }

    public boolean assetsMatchSnapshot(long chapterId, List<AssetSnapshot> current) {
        ArrayList<EditorialAssetManifest.Asset> stored = new ArrayList<>();
        try (Cursor c = database.editorialReadableDatabase().rawQuery("SELECT role,sha256 FROM editorial_assets WHERE chapter_id=?", new String[]{String.valueOf(chapterId)})) {
            while (c.moveToNext()) { try { stored.add(new EditorialAssetManifest.Asset(EditorialWorkflowV5.AssetRole.valueOf(c.getString(0)), c.getString(1))); } catch (Exception ignored) {} }
        }
        return EditorialAssetManifest.unchanged(stored, new ArrayList<EditorialAssetManifest.Asset>(current));
    }

    public int evidenceCount(long runId) {
        try (Cursor c = database.editorialReadableDatabase().rawQuery("SELECT COUNT(*) FROM editorial_evidence WHERE run_id=?", new String[]{String.valueOf(runId)})) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }

    public void deleteProject(long projectId) {
        SQLiteDatabase db = database.editorialWritableDatabase(); db.beginTransaction();
        try {
            ArrayList<Long> chapters = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT id FROM editorial_chapters WHERE project_id=?", new String[]{String.valueOf(projectId)})) { while (c.moveToNext()) chapters.add(c.getLong(0)); }
            for (Long chapterId : chapters) {
                ArrayList<Long> runs = new ArrayList<>();
                try (Cursor c = db.rawQuery("SELECT id FROM editorial_runs WHERE chapter_id=?", new String[]{String.valueOf(chapterId)})) { while (c.moveToNext()) runs.add(c.getLong(0)); }
                for (Long runId : runs) { db.delete("editorial_evidence", "run_id=?", new String[]{String.valueOf(runId)}); db.delete("editorial_gates", "run_id=?", new String[]{String.valueOf(runId)}); db.delete("editorial_scenes", "run_id=?", new String[]{String.valueOf(runId)}); }
                db.delete("editorial_runs", "chapter_id=?", new String[]{String.valueOf(chapterId)});
                db.delete("editorial_assets", "chapter_id=?", new String[]{String.valueOf(chapterId)});
            }
            db.delete("editorial_chapters", "project_id=?", new String[]{String.valueOf(projectId)});
            db.delete("editorial_projects", "id=?", new String[]{String.valueOf(projectId)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static String validateChapterAssets(List<AssetSnapshot> assets) {
        ArrayList<EditorialAssetManifest.Asset> manifest = new ArrayList<>(); if (assets != null) manifest.addAll(assets);
        String error = EditorialAssetManifest.validateUniqueRequired(manifest, EnumSet.of(EditorialWorkflowV5.AssetRole.RAW, EditorialWorkflowV5.AssetRole.DRAFT, EditorialWorkflowV5.AssetRole.GLOSSARY, EditorialWorkflowV5.AssetRole.PRONOUN));
        if (error != null) return error;
        for (AssetSnapshot asset : assets) if (blank(asset.content)) return "Asset content is empty: " + asset.role;
        return null;
    }
    private static AssetSnapshot find(List<AssetSnapshot> assets, EditorialWorkflowV5.AssetRole role) { for (AssetSnapshot asset : assets) if (asset.role == role) return asset; throw new IllegalArgumentException("Missing " + role); }
    private static boolean exists(SQLiteDatabase db, String table, long id) { try (Cursor c = db.rawQuery("SELECT id FROM " + table + " WHERE id=?", new String[]{String.valueOf(id)})) { return c.moveToFirst(); } }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static String safe(String value) { return value == null ? "" : value; }
    @Override public void close() { database.close(); }
}
