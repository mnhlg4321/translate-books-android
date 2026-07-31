package com.ml.tblandroidtxt;

import java.util.Arrays;
import java.util.List;

/** SQL for the additive v4.16 editorial migration; kept inspectable and testable. */
public final class EditorialMigrationSpec {
    public static List<String> from10To11() {
        return Arrays.asList(
                "CREATE TABLE IF NOT EXISTS editorial_projects (id INTEGER PRIMARY KEY AUTOINCREMENT, series_name TEXT NOT NULL, volume_name TEXT NOT NULL, workflow_version TEXT NOT NULL, workflow_hash TEXT NOT NULL, output_tree_uri TEXT DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)",
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_editorial_project_identity ON editorial_projects(series_name,volume_name)",
                "CREATE TABLE IF NOT EXISTS editorial_chapters (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, chapter_key TEXT NOT NULL, title TEXT DEFAULT '', state TEXT NOT NULL, raw_hash TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(project_id) REFERENCES editorial_projects(id))",
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_editorial_chapter_identity ON editorial_chapters(project_id,chapter_key)",
                "CREATE TABLE IF NOT EXISTS editorial_assets (id INTEGER PRIMARY KEY AUTOINCREMENT, chapter_id INTEGER NOT NULL, role TEXT NOT NULL, source_uri TEXT DEFAULT '', display_name TEXT DEFAULT '', sha256 TEXT NOT NULL, size_bytes INTEGER NOT NULL, content TEXT NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(chapter_id) REFERENCES editorial_chapters(id))",
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_editorial_asset_role ON editorial_assets(chapter_id,role)",
                "CREATE TABLE IF NOT EXISTS editorial_runs (id INTEGER PRIMARY KEY AUTOINCREMENT, chapter_id INTEGER NOT NULL, run_kind TEXT NOT NULL, state TEXT NOT NULL, provider TEXT DEFAULT '', model TEXT DEFAULT '', prompt_hash TEXT NOT NULL, workflow_hash TEXT NOT NULL, input_manifest_json TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(chapter_id) REFERENCES editorial_chapters(id))",
                "CREATE INDEX IF NOT EXISTS idx_editorial_run_chapter ON editorial_runs(chapter_id,created_at)",
                "CREATE TABLE IF NOT EXISTS editorial_scenes (id INTEGER PRIMARY KEY AUTOINCREMENT, run_id INTEGER NOT NULL, scene_key TEXT NOT NULL, raw_start_anchor TEXT NOT NULL, raw_end_anchor TEXT NOT NULL, status TEXT NOT NULL, ledger_json TEXT NOT NULL, FOREIGN KEY(run_id) REFERENCES editorial_runs(id))",
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_editorial_scene_identity ON editorial_scenes(run_id,scene_key)",
                "CREATE TABLE IF NOT EXISTS editorial_gates (run_id INTEGER NOT NULL, gate_name TEXT NOT NULL, status TEXT NOT NULL, evidence_json TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(run_id,gate_name), FOREIGN KEY(run_id) REFERENCES editorial_runs(id))",
                "CREATE TABLE IF NOT EXISTS editorial_evidence (id INTEGER PRIMARY KEY AUTOINCREMENT, run_id INTEGER NOT NULL, evidence_type TEXT NOT NULL, payload TEXT NOT NULL, payload_hash TEXT NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(run_id) REFERENCES editorial_runs(id))",
                "CREATE INDEX IF NOT EXISTS idx_editorial_evidence_run ON editorial_evidence(run_id,evidence_type,created_at)"
        );
    }
    public static List<String> from11To12(){return Arrays.asList(
            "CREATE TABLE IF NOT EXISTS editorial_project_assets (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, role TEXT NOT NULL, source_uri TEXT DEFAULT '', display_name TEXT DEFAULT '', sha256 TEXT NOT NULL, size_bytes INTEGER NOT NULL, content TEXT NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(project_id) REFERENCES editorial_projects(id))",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_editorial_project_asset_role ON editorial_project_assets(project_id,role)"
    );}
    public static List<String> from12To13(){return Arrays.asList(
            "CREATE TABLE IF NOT EXISTS editorial_project_reference_profiles (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, role TEXT NOT NULL, source_uri TEXT DEFAULT '', display_name TEXT NOT NULL, sha256 TEXT NOT NULL, size_bytes INTEGER NOT NULL, content TEXT NOT NULL, is_active INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL, FOREIGN KEY(project_id) REFERENCES editorial_projects(id))",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_editorial_reference_profile_name ON editorial_project_reference_profiles(project_id,role,display_name)",
            "CREATE INDEX IF NOT EXISTS idx_editorial_reference_profile_active ON editorial_project_reference_profiles(project_id,role,is_active)",
            "INSERT OR IGNORE INTO editorial_project_reference_profiles(project_id,role,source_uri,display_name,sha256,size_bytes,content,is_active,updated_at) SELECT project_id,role,source_uri,display_name,sha256,size_bytes,content,1,updated_at FROM editorial_project_assets"
    );}
    private EditorialMigrationSpec() {}
}
