package com.ml.tblandroidtxt;

import java.util.Arrays;
import java.util.List;

/** Versioned SQL kept separate so migration behavior is unit-testable. */
public final class DatabaseMigrationSpec {
    public static List<String> from42To43() { return Arrays.asList(
            "ALTER TABLE chunks ADD COLUMN stable_id TEXT DEFAULT ''",
            "ALTER TABLE chunks ADD COLUMN start_offset INTEGER DEFAULT -1",
            "ALTER TABLE chunks ADD COLUMN end_offset INTEGER DEFAULT -1",
            "ALTER TABLE chunks ADD COLUMN context_start_offset INTEGER DEFAULT -1",
            "ALTER TABLE chunks ADD COLUMN context_end_offset INTEGER DEFAULT -1",
            "ALTER TABLE chunks ADD COLUMN source_hash TEXT DEFAULT ''",
            "ALTER TABLE chunks ADD COLUMN normalized_source_hash TEXT DEFAULT ''",
            "ALTER TABLE chunks ADD COLUMN request_hash TEXT DEFAULT ''",
            "ALTER TABLE chunks ADD COLUMN response_hash TEXT DEFAULT ''",
            "ALTER TABLE chunks ADD COLUMN attempt INTEGER DEFAULT 0",
            "ALTER TABLE chunks ADD COLUMN created_at INTEGER DEFAULT 0",
            "ALTER TABLE chunks ADD COLUMN persisted_response TEXT DEFAULT ''",
            "ALTER TABLE chunks ADD COLUMN rejected_response TEXT DEFAULT ''",
            "ALTER TABLE chunks ADD COLUMN finish_reason TEXT DEFAULT ''",
            "ALTER TABLE chunks ADD COLUMN validation_warnings TEXT DEFAULT ''",
            "ALTER TABLE chunks ADD COLUMN qa_json TEXT DEFAULT ''",
            "ALTER TABLE chunks ADD COLUMN result_source TEXT DEFAULT 'PROVIDER'",
            "ALTER TABLE chunks ADD COLUMN parent_stable_id TEXT DEFAULT ''",
            "ALTER TABLE jobs ADD COLUMN integrity_status TEXT DEFAULT 'UNKNOWN'",
            "ALTER TABLE jobs ADD COLUMN output_hash TEXT DEFAULT ''",
            "ALTER TABLE jobs ADD COLUMN config_revision INTEGER DEFAULT 1",
            "ALTER TABLE jobs ADD COLUMN app_version TEXT DEFAULT '4.2-migrated'",
            "ALTER TABLE jobs ADD COLUMN completed_at INTEGER DEFAULT 0",
            "CREATE TABLE IF NOT EXISTS chunk_attempts (id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER, stable_id TEXT, chunk_idx INTEGER, attempt INTEGER, status TEXT, request_hash TEXT, response_hash TEXT, response_preview TEXT, finish_reason TEXT, validation_result TEXT, error TEXT, model TEXT, provider TEXT, input_tokens INTEGER DEFAULT 0, output_tokens INTEGER DEFAULT 0, cached_tokens INTEGER DEFAULT 0, actual_cost REAL DEFAULT 0, usage_source TEXT DEFAULT 'UNKNOWN', created_at INTEGER, updated_at INTEGER)",
            "CREATE INDEX IF NOT EXISTS idx_chunk_attempts_job ON chunk_attempts(job_id,chunk_idx,attempt)",
            "CREATE TABLE IF NOT EXISTS config_revisions (job_id INTEGER, revision INTEGER, first_chunk_idx INTEGER, settings_json TEXT, settings_hash TEXT, created_at INTEGER, PRIMARY KEY(job_id,revision))"
    ); }
    private DatabaseMigrationSpec() {}
}
