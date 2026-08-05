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
    public static List<String> from13To14(){return Arrays.asList(
            "CREATE TABLE IF NOT EXISTS editorial_packs (id INTEGER PRIMARY KEY AUTOINCREMENT, pack_id TEXT NOT NULL, version TEXT NOT NULL, canonical_pack_hash TEXT NOT NULL UNIQUE, contract_version TEXT NOT NULL, schema_version TEXT NOT NULL, minimum_engine_version TEXT NOT NULL, compatibility_class TEXT NOT NULL CHECK(compatibility_class IN ('DATA_COMPATIBLE','ADAPTER_REQUIRED','ENGINE_UPGRADE_REQUIRED','INVALID','BLOCKED')), state TEXT NOT NULL CHECK(state IN ('STORED_BLOCKED','STORED_READY_FOR_CERTIFICATION')), storage_key TEXT NOT NULL UNIQUE, manifest_canonical_json TEXT NOT NULL, created_at INTEGER NOT NULL, validated_at INTEGER NOT NULL, engine_version_used TEXT NOT NULL, blocked_reason TEXT NOT NULL DEFAULT '', UNIQUE(pack_id,version))",
            "CREATE TABLE IF NOT EXISTS editorial_pack_imports (import_id TEXT PRIMARY KEY, state TEXT NOT NULL CHECK(state IN ('STAGING','SNAPSHOTTED','INTEGRITY_VALIDATED','COMPATIBILITY_EVALUATED','STORED_BLOCKED','STORED_READY_FOR_CERTIFICATION')), pack_id TEXT DEFAULT '', pack_version TEXT DEFAULT '', canonical_pack_hash TEXT DEFAULT '', staging_path TEXT NOT NULL, storage_key TEXT DEFAULT '', storage_moved INTEGER NOT NULL DEFAULT 0 CHECK(storage_moved IN (0,1)), pack_row_id INTEGER, blocked_reason TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(pack_row_id) REFERENCES editorial_packs(id) ON DELETE RESTRICT)",
            "CREATE TABLE IF NOT EXISTS editorial_pack_files (id INTEGER PRIMARY KEY AUTOINCREMENT, pack_row_id INTEGER NOT NULL, declared_role TEXT NOT NULL CHECK(declared_role IN ('PROJECT_INSTRUCTION','TURN_PROMPT','WORKFLOW')), normalized_relative_path TEXT NOT NULL, byte_length INTEGER NOT NULL CHECK(byte_length >= 0), sha256 TEXT NOT NULL, storage_key TEXT NOT NULL, FOREIGN KEY(pack_row_id) REFERENCES editorial_packs(id) ON DELETE RESTRICT, UNIQUE(pack_row_id,declared_role), UNIQUE(pack_row_id,normalized_relative_path))",
            "CREATE TABLE IF NOT EXISTS editorial_pack_compatibility_results (id INTEGER PRIMARY KEY AUTOINCREMENT, import_id TEXT NOT NULL, pack_row_id INTEGER, canonical_pack_hash TEXT NOT NULL, engine_version_used TEXT NOT NULL, machine_contract_fingerprint TEXT NOT NULL, compatibility_class TEXT NOT NULL CHECK(compatibility_class IN ('DATA_COMPATIBLE','ADAPTER_REQUIRED','ENGINE_UPGRADE_REQUIRED','INVALID','BLOCKED')), required_class TEXT NOT NULL CHECK(required_class IN ('DATA_COMPATIBLE','ADAPTER_REQUIRED','ENGINE_UPGRADE_REQUIRED','INVALID','BLOCKED')), blocked_reason TEXT NOT NULL DEFAULT '', evaluated_at INTEGER NOT NULL, FOREIGN KEY(import_id) REFERENCES editorial_pack_imports(import_id) ON DELETE RESTRICT, FOREIGN KEY(pack_row_id) REFERENCES editorial_packs(id) ON DELETE RESTRICT, UNIQUE(canonical_pack_hash,engine_version_used,machine_contract_fingerprint))",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_editorial_pack_import_hash ON editorial_pack_imports(canonical_pack_hash) WHERE canonical_pack_hash<>''",
            "CREATE INDEX IF NOT EXISTS idx_editorial_pack_import_state ON editorial_pack_imports(state,updated_at)",
            "CREATE INDEX IF NOT EXISTS idx_editorial_pack_files_pack ON editorial_pack_files(pack_row_id,declared_role)",
            "CREATE INDEX IF NOT EXISTS idx_editorial_pack_compatibility_hash ON editorial_pack_compatibility_results(canonical_pack_hash,evaluated_at)",
            "CREATE TRIGGER IF NOT EXISTS trg_editorial_packs_no_update BEFORE UPDATE ON editorial_packs BEGIN SELECT RAISE(ABORT,'editorial_packs are immutable'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_editorial_packs_no_delete BEFORE DELETE ON editorial_packs BEGIN SELECT RAISE(ABORT,'editorial_packs are immutable'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_editorial_pack_files_no_update BEFORE UPDATE ON editorial_pack_files BEGIN SELECT RAISE(ABORT,'editorial_pack_files are immutable'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_editorial_pack_files_no_delete BEFORE DELETE ON editorial_pack_files BEGIN SELECT RAISE(ABORT,'editorial_pack_files are immutable'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_editorial_pack_compatibility_no_update BEFORE UPDATE ON editorial_pack_compatibility_results BEGIN SELECT RAISE(ABORT,'editorial_pack_compatibility_results are immutable'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_editorial_pack_compatibility_no_delete BEFORE DELETE ON editorial_pack_compatibility_results BEGIN SELECT RAISE(ABORT,'editorial_pack_compatibility_results are immutable'); END"
    );}
    /**
     * v15 is a new immutable evaluation history. It deliberately does not alter
     * or backfill the v14 compatibility-result row. SQLiteOpenHelper runs this
     * list inside its upgrade transaction.
     */
    public static List<String> from14To15(){return Arrays.asList(
            "CREATE TABLE IF NOT EXISTS editorial_pack_compatibility_evaluations (id INTEGER PRIMARY KEY AUTOINCREMENT, evaluation_id TEXT NOT NULL UNIQUE, import_id TEXT NOT NULL, pack_row_id INTEGER, compatibility_result_id INTEGER, canonical_pack_hash TEXT NOT NULL, trusted_profile_id TEXT NOT NULL, trusted_profile_version TEXT NOT NULL, canonical_profile_hash TEXT NOT NULL, engine_version_used TEXT NOT NULL, machine_contract_fingerprint TEXT NOT NULL, evaluator_contract_version TEXT NOT NULL, adapter_set_fingerprint TEXT NOT NULL, capability_fingerprint TEXT NOT NULL, context_fingerprint TEXT NOT NULL, compatibility_outcome TEXT NOT NULL CHECK(compatibility_outcome IN ('DATA_COMPATIBLE','ADAPTER_REQUIRED','ENGINE_UPGRADE_REQUIRED','INVALID','BLOCKED')), reason_code TEXT NOT NULL, blocker_details TEXT NOT NULL DEFAULT '', evaluated_at INTEGER NOT NULL, FOREIGN KEY(import_id) REFERENCES editorial_pack_imports(import_id) ON DELETE RESTRICT, FOREIGN KEY(pack_row_id) REFERENCES editorial_packs(id) ON DELETE RESTRICT, FOREIGN KEY(compatibility_result_id) REFERENCES editorial_pack_compatibility_results(id) ON DELETE RESTRICT)",
            "CREATE INDEX IF NOT EXISTS idx_editorial_pack_compatibility_evaluations_pack ON editorial_pack_compatibility_evaluations(canonical_pack_hash,evaluated_at,id)",
            "CREATE INDEX IF NOT EXISTS idx_editorial_pack_compatibility_evaluations_import ON editorial_pack_compatibility_evaluations(import_id,evaluated_at)",
            "CREATE TRIGGER IF NOT EXISTS trg_editorial_pack_compatibility_evaluations_no_update BEFORE UPDATE ON editorial_pack_compatibility_evaluations BEGIN SELECT RAISE(ABORT,'editorial_pack_compatibility_evaluations are immutable'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_editorial_pack_compatibility_evaluations_no_delete BEFORE DELETE ON editorial_pack_compatibility_evaluations BEGIN SELECT RAISE(ABORT,'editorial_pack_compatibility_evaluations are immutable'); END"
    );}
    private EditorialMigrationSpec() {}
}
