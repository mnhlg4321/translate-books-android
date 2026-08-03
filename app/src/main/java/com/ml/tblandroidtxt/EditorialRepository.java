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
        public String workflowVersion = EditorialSafe4Pack.VERSION;
        public String workflowHash = EditorialSafe4Pack.PACK_HASH;
        public String outputTreeUri = "";
    }
    public static final class Chapter {
        public long id;
        public long projectId;
        public String chapterKey = "";
        public String title = "";
        public EditorialSafe4Workflow.ChapterState state = EditorialSafe4Workflow.ChapterState.SAFE4_BLOCKED;
        public String rawHash = "";
    }
    public static final class AssetSnapshot extends EditorialAssetManifest.Asset {
        public final String sourceUri;
        public final String displayName;
        public final String content;
        public AssetSnapshot(EditorialSafe4Workflow.AssetRole role, String sourceUri, String displayName, String content) {
            super(role, HashUtil.sha256(content == null ? "" : content));
            this.sourceUri = safe(sourceUri);
            this.displayName = safe(displayName);
            this.content = safe(content);
        }
    }
    public static final class ReferenceProfile {
        public long id; public long projectId; public EditorialSafe4Workflow.AssetRole role;
        public String sourceUri="",displayName="",content="",sha256=""; public boolean active;
        AssetSnapshot snapshot(){return new AssetSnapshot(role,sourceUri,displayName,content);}
    }
    private final TranslationRepository database;
    public EditorialRepository(Context context) { this(new TranslationRepository(context.getApplicationContext())); }
    EditorialRepository(TranslationRepository database) { this.database = database; }

    public long createProject(Project project) {
        if (project == null || blank(project.seriesName) || blank(project.volumeName)) throw new IllegalArgumentException("Project identity is required");
        if (!EditorialSafe4Pack.VERSION.equals(project.workflowVersion)
                || !EditorialSafe4Pack.PACK_HASH.equals(project.workflowHash)) {
            throw new IllegalArgumentException("New editorial projects must bind the immutable V5-SAFE.4 pack");
        }
        ContentValues v = new ContentValues(); long now = System.currentTimeMillis();
        v.put("series_name", project.seriesName.trim()); v.put("volume_name", project.volumeName.trim());
        v.put("workflow_version", safe(project.workflowVersion)); v.put("workflow_hash", project.workflowHash.trim());
        v.put("output_tree_uri", safe(project.outputTreeUri)); v.put("created_at", now); v.put("updated_at", now);
        return database.editorialWritableDatabase().insertOrThrow("editorial_projects", null, v);
    }

    public long createChapter(long projectId, String chapterKey, String title, List<AssetSnapshot> assets) {
        if (projectId <= 0 || blank(chapterKey)) throw new IllegalArgumentException("Project and chapter key are required");
        String assetError = validateChapterAssets(assets); if (assetError != null) throw new IllegalArgumentException(assetError);
        AssetSnapshot raw = find(assets, EditorialSafe4Workflow.AssetRole.RAW);
        SQLiteDatabase db = database.editorialWritableDatabase(); long now = System.currentTimeMillis(); db.beginTransaction();
        try {
            try (Cursor project = db.rawQuery("SELECT workflow_version,workflow_hash FROM editorial_projects WHERE id=?", new String[]{String.valueOf(projectId)})) {
                if (!project.moveToFirst()) throw new IllegalArgumentException("Editorial project not found");
                if (!EditorialSafe4Pack.VERSION.equals(safe(project.getString(0)))
                        || !EditorialSafe4Pack.PACK_HASH.equals(safe(project.getString(1)))) {
                    throw new IllegalStateException("Legacy editorial projects are read-only");
                }
            }
            ContentValues chapter = new ContentValues(); chapter.put("project_id", projectId); chapter.put("chapter_key", chapterKey.trim()); chapter.put("title", safe(title));
            chapter.put("state", EditorialSafe4Workflow.ChapterState.SAFE4_BLOCKED.name()); chapter.put("raw_hash", raw.sha256); chapter.put("created_at", now); chapter.put("updated_at", now);
            long id = db.insertOrThrow("editorial_chapters", null, chapter);
            for (AssetSnapshot asset : assets) {
                ContentValues v = new ContentValues(); v.put("chapter_id", id); v.put("role", asset.role.name()); v.put("source_uri", asset.sourceUri);
                v.put("display_name", asset.displayName); v.put("sha256", asset.sha256); v.put("size_bytes", asset.content.length()); v.put("content", asset.content); v.put("created_at", now);
                db.insertOrThrow("editorial_assets", null, v);
            }
            db.setTransactionSuccessful(); return id;
        } finally { db.endTransaction(); }
    }

    public Chapter getChapter(long id) {
        try (Cursor c = database.editorialReadableDatabase().rawQuery("SELECT c.id,c.project_id,c.chapter_key,c.title,c.state,c.raw_hash,p.workflow_version FROM editorial_chapters c JOIN editorial_projects p ON p.id=c.project_id WHERE c.id=?", new String[]{String.valueOf(id)})) {
            if (!c.moveToFirst()) return null; Chapter chapter = new Chapter(); chapter.id=c.getLong(0); chapter.projectId=c.getLong(1); chapter.chapterKey=safe(c.getString(2)); chapter.title=safe(c.getString(3));
            chapter.state = EditorialSafe4Pack.VERSION.equals(safe(c.getString(6)))
                    ? EditorialSafe4Workflow.persistedState(c.getString(4))
                    : EditorialSafe4Workflow.ChapterState.LEGACY_V5_READ_ONLY;
            chapter.rawHash=safe(c.getString(5)); return chapter;
        }
    }

    public Project getProject(long id) {
        try (Cursor c=database.editorialReadableDatabase().rawQuery("SELECT id,series_name,volume_name,workflow_version,workflow_hash,output_tree_uri FROM editorial_projects WHERE id=?",new String[]{String.valueOf(id)})) {
            if(!c.moveToFirst())return null; Project p=new Project();p.id=c.getLong(0);p.seriesName=safe(c.getString(1));p.volumeName=safe(c.getString(2));p.workflowVersion=safe(c.getString(3));p.workflowHash=safe(c.getString(4));p.outputTreeUri=safe(c.getString(5));return p;
        }
    }

    public void updateProjectIdentity(long projectId,String series,String volume){if(projectId<=0||blank(series)||blank(volume))throw new IllegalArgumentException("Series and Volume are required");SQLiteDatabase db=database.editorialWritableDatabase();requireSafe4Project(db,projectId);ContentValues v=new ContentValues();v.put("series_name",series.trim());v.put("volume_name",volume.trim());v.put("updated_at",System.currentTimeMillis());try{if(db.update("editorial_projects",v,"id=?",new String[]{String.valueOf(projectId)})!=1)throw new IllegalStateException("Editorial project not found");}catch(android.database.sqlite.SQLiteConstraintException e){throw new IllegalArgumentException("A project with the same Series and Volume already exists");}}

    public void saveProjectReference(long projectId,AssetSnapshot asset){if(projectId<=0||asset==null||(asset.role!=EditorialSafe4Workflow.AssetRole.GLOSSARY&&asset.role!=EditorialSafe4Workflow.AssetRole.PRONOUN)||blank(asset.content))throw new IllegalArgumentException("Editorial project reference is invalid");SQLiteDatabase db=database.editorialWritableDatabase();long now=System.currentTimeMillis();db.beginTransaction();try{requireSafe4Project(db,projectId);ContentValues off=new ContentValues();off.put("is_active",0);db.update("editorial_project_reference_profiles",off,"project_id=? AND role=?",new String[]{String.valueOf(projectId),asset.role.name()});ContentValues p=new ContentValues();p.put("project_id",projectId);p.put("role",asset.role.name());p.put("source_uri",asset.sourceUri);p.put("display_name",asset.displayName);p.put("sha256",asset.sha256);p.put("size_bytes",asset.content.length());p.put("content",asset.content);p.put("is_active",1);p.put("updated_at",now);db.insertWithOnConflict("editorial_project_reference_profiles",null,p,SQLiteDatabase.CONFLICT_REPLACE);ContentValues v=new ContentValues(p);v.remove("is_active");db.insertWithOnConflict("editorial_project_assets",null,v,SQLiteDatabase.CONFLICT_REPLACE);db.setTransactionSuccessful();}finally{db.endTransaction();}}

    public AssetSnapshot projectReference(long projectId,EditorialSafe4Workflow.AssetRole role){if(role!=EditorialSafe4Workflow.AssetRole.GLOSSARY&&role!=EditorialSafe4Workflow.AssetRole.PRONOUN)return null;try(Cursor c=database.editorialReadableDatabase().rawQuery("SELECT source_uri,display_name,content FROM editorial_project_reference_profiles WHERE project_id=? AND role=? AND is_active=1 ORDER BY updated_at DESC,id DESC LIMIT 1",new String[]{String.valueOf(projectId),role.name()})){if(c.moveToFirst())return new AssetSnapshot(role,safe(c.getString(0)),safe(c.getString(1)),safe(c.getString(2)));}try(Cursor c=database.editorialReadableDatabase().rawQuery("SELECT source_uri,display_name,content FROM editorial_project_assets WHERE project_id=? AND role=?",new String[]{String.valueOf(projectId),role.name()})){return c.moveToFirst()?new AssetSnapshot(role,safe(c.getString(0)),safe(c.getString(1)),safe(c.getString(2))):null;}}

    public List<ReferenceProfile> projectReferenceProfiles(long projectId,EditorialSafe4Workflow.AssetRole role){ArrayList<ReferenceProfile> out=new ArrayList<>();try(Cursor c=database.editorialReadableDatabase().rawQuery("SELECT id,source_uri,display_name,content,sha256,is_active FROM editorial_project_reference_profiles WHERE project_id=? AND role=? ORDER BY is_active DESC,updated_at DESC,id DESC",new String[]{String.valueOf(projectId),role.name()})){while(c.moveToNext()){ReferenceProfile p=new ReferenceProfile();p.id=c.getLong(0);p.projectId=projectId;p.role=role;p.sourceUri=safe(c.getString(1));p.displayName=safe(c.getString(2));p.content=safe(c.getString(3));p.sha256=safe(c.getString(4));p.active=c.getInt(5)!=0;out.add(p);}}return out;}

    public void selectProjectReference(long projectId,long profileId,EditorialSafe4Workflow.AssetRole role){SQLiteDatabase db=database.editorialWritableDatabase();db.beginTransaction();try{requireSafe4Project(db,projectId);ContentValues off=new ContentValues();off.put("is_active",0);db.update("editorial_project_reference_profiles",off,"project_id=? AND role=?",new String[]{String.valueOf(projectId),role.name()});ContentValues on=new ContentValues();on.put("is_active",1);on.put("updated_at",System.currentTimeMillis());if(db.update("editorial_project_reference_profiles",on,"id=? AND project_id=? AND role=?",new String[]{String.valueOf(profileId),String.valueOf(projectId),role.name()})!=1)throw new IllegalArgumentException("Reference profile not found");ReferenceProfile active=null;for(ReferenceProfile p:projectReferenceProfiles(projectId,role))if(p.id==profileId){active=p;break;}if(active==null)throw new IllegalStateException("Reference profile unavailable");ContentValues legacy=new ContentValues();legacy.put("project_id",projectId);legacy.put("role",role.name());legacy.put("source_uri",active.sourceUri);legacy.put("display_name",active.displayName);legacy.put("sha256",active.sha256);legacy.put("size_bytes",active.content.length());legacy.put("content",active.content);legacy.put("updated_at",System.currentTimeMillis());db.insertWithOnConflict("editorial_project_assets",null,legacy,SQLiteDatabase.CONFLICT_REPLACE);db.setTransactionSuccessful();}finally{db.endTransaction();}}

    public void deleteProjectReference(long projectId,long profileId,EditorialSafe4Workflow.AssetRole role){SQLiteDatabase db=database.editorialWritableDatabase();requireSafe4Project(db,projectId);boolean active=false;try(Cursor c=db.rawQuery("SELECT is_active FROM editorial_project_reference_profiles WHERE id=? AND project_id=? AND role=?",new String[]{String.valueOf(profileId),String.valueOf(projectId),role.name()})){active=c.moveToFirst()&&c.getInt(0)!=0;}db.delete("editorial_project_reference_profiles","id=? AND project_id=? AND role=?",new String[]{String.valueOf(profileId),String.valueOf(projectId),role.name()});if(active){List<ReferenceProfile> left=projectReferenceProfiles(projectId,role);if(left.isEmpty())db.delete("editorial_project_assets","project_id=? AND role=?",new String[]{String.valueOf(projectId),role.name()});else selectProjectReference(projectId,left.get(0).id,role);}}

    public List<AssetSnapshot> chapterAssets(long chapterId) {
        ArrayList<AssetSnapshot> assets=new ArrayList<>();
        try(Cursor c=database.editorialReadableDatabase().rawQuery("SELECT role,source_uri,display_name,content FROM editorial_assets WHERE chapter_id=? ORDER BY role",new String[]{String.valueOf(chapterId)})) {
            while(c.moveToNext())try{assets.add(new AssetSnapshot(EditorialSafe4Workflow.AssetRole.valueOf(c.getString(0)),safe(c.getString(1)),safe(c.getString(2)),safe(c.getString(3))));}catch(Exception ignored){}
        } return assets;
    }

    public List<Project> listProjects() {
        ArrayList<Project> projects=new ArrayList<>();
        try (Cursor c=database.editorialReadableDatabase().rawQuery("SELECT id,series_name,volume_name,workflow_version,workflow_hash,output_tree_uri FROM editorial_projects ORDER BY updated_at DESC,id DESC",null)) {
            while(c.moveToNext()){Project p=new Project();p.id=c.getLong(0);p.seriesName=safe(c.getString(1));p.volumeName=safe(c.getString(2));p.workflowVersion=safe(c.getString(3));p.workflowHash=safe(c.getString(4));p.outputTreeUri=safe(c.getString(5));projects.add(p);}
        } return projects;
    }

    public List<Chapter> listChapters(long projectId) {
        ArrayList<Chapter> chapters=new ArrayList<>();
        try (Cursor c=database.editorialReadableDatabase().rawQuery("SELECT c.id,c.project_id,c.chapter_key,c.title,c.state,c.raw_hash,p.workflow_version FROM editorial_chapters c JOIN editorial_projects p ON p.id=c.project_id WHERE c.project_id=? ORDER BY c.chapter_key COLLATE NOCASE",new String[]{String.valueOf(projectId)})) {
            while(c.moveToNext()){Chapter chapter=new Chapter();chapter.id=c.getLong(0);chapter.projectId=c.getLong(1);chapter.chapterKey=safe(c.getString(2));chapter.title=safe(c.getString(3));chapter.state=EditorialSafe4Pack.VERSION.equals(safe(c.getString(6)))?EditorialSafe4Workflow.persistedState(c.getString(4)):EditorialSafe4Workflow.ChapterState.LEGACY_V5_READ_ONLY;chapter.rawHash=safe(c.getString(5));chapters.add(chapter);}
        } return chapters;
    }

    public boolean assetsMatchSnapshot(long chapterId, List<AssetSnapshot> current) {
        ArrayList<EditorialAssetManifest.Asset> stored = new ArrayList<>();
        try (Cursor c = database.editorialReadableDatabase().rawQuery("SELECT role,sha256 FROM editorial_assets WHERE chapter_id=?", new String[]{String.valueOf(chapterId)})) {
            while (c.moveToNext()) { try { stored.add(new EditorialAssetManifest.Asset(EditorialSafe4Workflow.AssetRole.valueOf(c.getString(0)), c.getString(1))); } catch (Exception ignored) {} }
        }
        return EditorialAssetManifest.unchanged(stored, new ArrayList<EditorialAssetManifest.Asset>(current));
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
            db.delete("editorial_project_assets","project_id=?",new String[]{String.valueOf(projectId)});
            db.delete("editorial_project_reference_profiles","project_id=?",new String[]{String.valueOf(projectId)});
            db.delete("editorial_projects", "id=?", new String[]{String.valueOf(projectId)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static String validateChapterAssets(List<AssetSnapshot> assets) {
        ArrayList<EditorialAssetManifest.Asset> manifest = new ArrayList<>(); if (assets != null) manifest.addAll(assets);
        String error = EditorialAssetManifest.validateUniqueRequired(manifest, EnumSet.of(EditorialSafe4Workflow.AssetRole.RAW, EditorialSafe4Workflow.AssetRole.DRAFT, EditorialSafe4Workflow.AssetRole.GLOSSARY));
        if (error != null) return error;
        EnumSet<EditorialSafe4Workflow.AssetRole> allowed = EnumSet.of(EditorialSafe4Workflow.AssetRole.RAW,
                EditorialSafe4Workflow.AssetRole.DRAFT, EditorialSafe4Workflow.AssetRole.GLOSSARY,
                EditorialSafe4Workflow.AssetRole.PRONOUN, EditorialSafe4Workflow.AssetRole.PAIR_CONTEXT);
        for (AssetSnapshot asset : assets) {
            if (!allowed.contains(asset.role)) return "Unexpected SAFE4 input role: " + asset.role;
            if (blank(asset.content)) return "Asset content is empty: " + asset.role;
        }
        return null;
    }
    private static AssetSnapshot find(List<AssetSnapshot> assets, EditorialSafe4Workflow.AssetRole role) { for (AssetSnapshot asset : assets) if (asset.role == role) return asset; throw new IllegalArgumentException("Missing " + role); }
    private static void requireSafe4Project(SQLiteDatabase db,long projectId){try(Cursor c=db.rawQuery("SELECT workflow_version,workflow_hash FROM editorial_projects WHERE id=?",new String[]{String.valueOf(projectId)})){if(!c.moveToFirst())throw new IllegalArgumentException("Editorial project not found");if(!EditorialSafe4Pack.VERSION.equals(safe(c.getString(0)))||!EditorialSafe4Pack.PACK_HASH.equals(safe(c.getString(1))))throw new IllegalStateException("Legacy editorial projects are read-only");}}
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static String safe(String value) { return value == null ? "" : value; }
    @Override public void close() { database.close(); }
}
