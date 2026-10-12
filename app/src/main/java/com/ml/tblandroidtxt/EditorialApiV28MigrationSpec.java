package com.ml.tblandroidtxt;

import java.util.Arrays;
import java.util.List;

/** Additive-preserving v28 migration: allow V5_CHAT and snapshot its original attachments with each API run. */
public final class EditorialApiV28MigrationSpec {
    public static List<String> from27To28() {
        return Arrays.asList(
                "DROP INDEX IF EXISTS idx_editorial_api_runs_combo",
                "ALTER TABLE editorial_api_runs RENAME TO editorial_api_runs_v27",
                "CREATE TABLE editorial_api_runs (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "combo_id INTEGER NOT NULL REFERENCES editorial_combos(id) ON DELETE CASCADE, "
                        + "contract_revision TEXT NOT NULL, quality_core_sha256 TEXT NOT NULL DEFAULT '', model TEXT NOT NULL DEFAULT '', "
                        + "mode TEXT NOT NULL CHECK(mode IN ('QUICK','THOROUGH','V5_CHAT')), "
                        + "state TEXT NOT NULL CHECK(state IN ('RUNNING','RETRY_REQUIRED','WRONG_PAIR','FINAL_OK','FINAL_NOTES','CANCELLED')), "
                        + "raw_sha256 TEXT NOT NULL, raw_text TEXT NOT NULL, draft_sha256 TEXT NOT NULL, draft_text TEXT NOT NULL, "
                        + "glossary_sha256 TEXT NOT NULL DEFAULT '', glossary_text TEXT NOT NULL DEFAULT '', "
                        + "pronoun_sha256 TEXT NOT NULL DEFAULT '', pronoun_text TEXT NOT NULL DEFAULT '', "
                        + "original_source_files_json TEXT NOT NULL DEFAULT '[]', "
                        + "edited_text TEXT NOT NULL DEFAULT '', final_text TEXT NOT NULL DEFAULT '', notes_json TEXT NOT NULL DEFAULT '[]', "
                        + "issues_json TEXT NOT NULL DEFAULT '[]', guards_json TEXT NOT NULL DEFAULT '{}', steps_json TEXT NOT NULL DEFAULT '[]', "
                        + "wrong_pair_evidence TEXT NOT NULL DEFAULT '', calls INTEGER NOT NULL DEFAULT 0, "
                        + "input_tokens INTEGER NOT NULL DEFAULT 0, output_tokens INTEGER NOT NULL DEFAULT 0, usd TEXT NOT NULL DEFAULT '0', "
                        + "cost_known INTEGER NOT NULL DEFAULT 1, glossary_entries INTEGER NOT NULL DEFAULT 0, pronoun_rows INTEGER NOT NULL DEFAULT 0, "
                        + "error TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)",
                "INSERT INTO editorial_api_runs (id,combo_id,contract_revision,quality_core_sha256,model,mode,state,raw_sha256,raw_text,"
                        + "draft_sha256,draft_text,glossary_sha256,glossary_text,pronoun_sha256,pronoun_text,original_source_files_json,"
                        + "edited_text,final_text,notes_json,issues_json,guards_json,steps_json,wrong_pair_evidence,calls,input_tokens,"
                        + "output_tokens,usd,cost_known,glossary_entries,pronoun_rows,error,created_at,updated_at) "
                        + "SELECT id,combo_id,contract_revision,quality_core_sha256,model,mode,state,raw_sha256,raw_text,draft_sha256,draft_text,"
                        + "glossary_sha256,glossary_text,pronoun_sha256,pronoun_text,'[]',edited_text,final_text,notes_json,issues_json,"
                        + "guards_json,steps_json,wrong_pair_evidence,calls,input_tokens,output_tokens,usd,cost_known,glossary_entries,"
                        + "pronoun_rows,error,created_at,updated_at FROM editorial_api_runs_v27",
                "DROP TABLE editorial_api_runs_v27",
                "CREATE INDEX IF NOT EXISTS idx_editorial_api_runs_combo ON editorial_api_runs(combo_id,created_at)"
        );
    }

    private EditorialApiV28MigrationSpec() { }
}
