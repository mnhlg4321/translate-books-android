package com.ml.tblandroidtxt;

import java.util.Arrays;
import java.util.List;

/**
 * Database version 26: the two tables of EDITORIAL_API_V1 (docs/EDITORIAL_API_V1_PLAN_20261005.md section 4.4). Additive
 * only - no existing table is touched, nothing is inserted, updated or dropped - and kept inspectable like the earlier specs.
 */
public final class EditorialApiMigrationSpec {
    public static final String COMBOS = "editorial_combos";
    public static final String RUNS = "editorial_api_runs";

    public static List<String> from25To26() {
        return Arrays.asList(
                "CREATE TABLE IF NOT EXISTS editorial_combos (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, "
                        + "raw_uri TEXT NOT NULL DEFAULT '', raw_name TEXT NOT NULL DEFAULT '', draft_uri TEXT NOT NULL DEFAULT '', "
                        + "draft_name TEXT NOT NULL DEFAULT '', glossary_id TEXT, pronoun_id TEXT, settings_json TEXT NOT NULL DEFAULT '{}', "
                        + "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS editorial_api_runs (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "combo_id INTEGER NOT NULL REFERENCES editorial_combos(id) ON DELETE CASCADE, "
                        + "contract_revision TEXT NOT NULL, quality_core_sha256 TEXT NOT NULL DEFAULT '', model TEXT NOT NULL DEFAULT '', "
                        + "mode TEXT NOT NULL CHECK(mode IN ('QUICK','THOROUGH')), "
                        + "state TEXT NOT NULL CHECK(state IN ('RUNNING','RETRY_REQUIRED','WRONG_PAIR','FINAL_OK','FINAL_NOTES','CANCELLED')), "
                        + "raw_sha256 TEXT NOT NULL, raw_text TEXT NOT NULL, draft_sha256 TEXT NOT NULL, draft_text TEXT NOT NULL, "
                        + "glossary_sha256 TEXT NOT NULL DEFAULT '', glossary_text TEXT NOT NULL DEFAULT '', "
                        + "pronoun_sha256 TEXT NOT NULL DEFAULT '', pronoun_text TEXT NOT NULL DEFAULT '', "
                        + "edited_text TEXT NOT NULL DEFAULT '', final_text TEXT NOT NULL DEFAULT '', notes_json TEXT NOT NULL DEFAULT '[]', "
                        + "issues_json TEXT NOT NULL DEFAULT '[]', guards_json TEXT NOT NULL DEFAULT '{}', steps_json TEXT NOT NULL DEFAULT '[]', "
                        + "wrong_pair_evidence TEXT NOT NULL DEFAULT '', calls INTEGER NOT NULL DEFAULT 0, "
                        + "input_tokens INTEGER NOT NULL DEFAULT 0, output_tokens INTEGER NOT NULL DEFAULT 0, usd TEXT NOT NULL DEFAULT '0', "
                        + "cost_known INTEGER NOT NULL DEFAULT 1, glossary_entries INTEGER NOT NULL DEFAULT 0, pronoun_rows INTEGER NOT NULL DEFAULT 0, "
                        + "error TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)",
                "CREATE INDEX IF NOT EXISTS idx_editorial_api_runs_combo ON editorial_api_runs(combo_id,created_at)"
        );
    }

    private EditorialApiMigrationSpec() { }
}
