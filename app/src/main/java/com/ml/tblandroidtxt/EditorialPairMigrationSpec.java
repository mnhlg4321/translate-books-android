package com.ml.tblandroidtxt;

import java.util.Arrays;
import java.util.List;

/**
 * Database version 27: the chunk-pair run tables and two columns on the combo (which kind of source and which job). Additive
 * only. An existing combo keeps source_kind FILES and its whole-chapter runs stay in editorial_api_runs; nothing is converted,
 * guessed or backfilled, and no old run becomes a chunk run.
 */
public final class EditorialPairMigrationSpec {
    public static final String RUNS = "editorial_pair_runs";
    public static final String ITEMS = "editorial_pair_items";
    public static final String RESERVATIONS = "editorial_pair_reservations";
    public static final String JOURNAL = "editorial_pair_journal";

    private static final String PAIR_STATES = "'IMPORTED','RESERVE_FAILED','E_RESERVED','E_SENT','E_RECEIVED','STRUCTURE_BLOCKED','WARN_REVIEW',"
            + "'CHECK_PENDING','ACCEPTED','REJECTED','UNKNOWN','MISSING'";
    private static final String RUN_STATES = "'PREPARED','RUNNING','PAUSED','INCOMPLETE','UNKNOWN','FINAL_BLOCKED','FINAL_ELIGIBLE'";

    public static List<String> from26To27() {
        return Arrays.asList(
                "CREATE TABLE IF NOT EXISTS editorial_pair_runs (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "combo_id INTEGER NOT NULL REFERENCES editorial_combos(id) ON DELETE CASCADE, "
                        + "arm TEXT NOT NULL CHECK(arm IN ('C','W')), contract_revision TEXT NOT NULL, model TEXT NOT NULL DEFAULT '', "
                        + "target_language TEXT NOT NULL DEFAULT 'Vietnamese', "
                        + "state TEXT NOT NULL CHECK(state IN (" + RUN_STATES + ")), "
                        + "source_kind TEXT NOT NULL, source_ref TEXT NOT NULL DEFAULT '', source_label TEXT NOT NULL DEFAULT '', chapter_id TEXT NOT NULL DEFAULT '', "
                        + "raw_rows_json TEXT NOT NULL, draft_rows_json TEXT NOT NULL, "
                        + "glossary_text TEXT NOT NULL DEFAULT '', glossary_sha256 TEXT NOT NULL DEFAULT '', "
                        + "pronoun_text TEXT NOT NULL DEFAULT '', pronoun_sha256 TEXT NOT NULL DEFAULT '', cue_fields TEXT NOT NULL DEFAULT 'from,speaker,target', "
                        + "map_revision TEXT NOT NULL DEFAULT '', map_hash TEXT NOT NULL DEFAULT '', cap_usd TEXT NOT NULL DEFAULT '0.10', "
                        + "started INTEGER NOT NULL DEFAULT 0, paused INTEGER NOT NULL DEFAULT 0, cost_overrun INTEGER NOT NULL DEFAULT 0, "
                        + "merged_text TEXT NOT NULL DEFAULT '', merge_receipt_json TEXT NOT NULL DEFAULT '', warnings INTEGER NOT NULL DEFAULT 0, "
                        + "calls INTEGER NOT NULL DEFAULT 0, input_tokens INTEGER NOT NULL DEFAULT 0, output_tokens INTEGER NOT NULL DEFAULT 0, "
                        + "usd TEXT NOT NULL DEFAULT '0', cost_known INTEGER NOT NULL DEFAULT 1, error TEXT NOT NULL DEFAULT '', "
                        + "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS editorial_pair_items (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "run_id INTEGER NOT NULL REFERENCES editorial_pair_runs(id) ON DELETE CASCADE, pair_id TEXT NOT NULL, ordinal INTEGER NOT NULL, "
                        + "state TEXT NOT NULL CHECK(state IN (" + PAIR_STATES + ")), attempt INTEGER NOT NULL DEFAULT 0, generation INTEGER NOT NULL DEFAULT 0, "
                        + "request_id TEXT NOT NULL DEFAULT '', candidate_text TEXT NOT NULL DEFAULT '', response_text TEXT NOT NULL DEFAULT '', "
                        + "gate_json TEXT NOT NULL DEFAULT '', response_hash TEXT NOT NULL DEFAULT '', calls INTEGER NOT NULL DEFAULT 0, "
                        + "input_tokens INTEGER NOT NULL DEFAULT 0, output_tokens INTEGER NOT NULL DEFAULT 0, usd TEXT NOT NULL DEFAULT '0', "
                        + "cost_known INTEGER NOT NULL DEFAULT 1, error TEXT NOT NULL DEFAULT '', updated_at INTEGER NOT NULL, UNIQUE(run_id, pair_id))",
                "CREATE TABLE IF NOT EXISTS editorial_pair_reservations (call_id TEXT PRIMARY KEY, "
                        + "run_id INTEGER NOT NULL REFERENCES editorial_pair_runs(id) ON DELETE CASCADE, pair_id TEXT NOT NULL, "
                        + "worst_usd TEXT NOT NULL, state TEXT NOT NULL CHECK(state IN ('RESERVED','SETTLED','SETTLED_ZERO')), "
                        + "settled_usd TEXT NOT NULL DEFAULT '0', reason TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS editorial_pair_journal (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "run_id INTEGER NOT NULL REFERENCES editorial_pair_runs(id) ON DELETE CASCADE, pair_id TEXT NOT NULL DEFAULT '', "
                        + "event TEXT NOT NULL, created_at INTEGER NOT NULL)",
                "CREATE INDEX IF NOT EXISTS idx_editorial_pair_runs_combo ON editorial_pair_runs(combo_id,created_at)",
                "CREATE INDEX IF NOT EXISTS idx_editorial_pair_items_run ON editorial_pair_items(run_id,ordinal)",
                "CREATE INDEX IF NOT EXISTS idx_editorial_pair_reservations_run ON editorial_pair_reservations(run_id,state)",
                "ALTER TABLE editorial_combos ADD COLUMN source_kind TEXT NOT NULL DEFAULT 'FILES'",
                "ALTER TABLE editorial_combos ADD COLUMN job_id INTEGER NOT NULL DEFAULT 0"
        );
    }

    private EditorialPairMigrationSpec() { }
}
