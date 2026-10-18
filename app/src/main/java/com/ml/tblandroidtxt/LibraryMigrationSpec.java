package com.ml.tblandroidtxt;

import java.util.List;

/**
 * Version 30 adds the Translate library tables: imported RAW chapters (text kept as files under filesDir/library_raw) and the
 * chapter sets that pair a RAW with a Glossary and a Pronoun profile. Nothing existing is changed; no book text is stored in SQLite.
 */
public final class LibraryMigrationSpec {
    public static List<String> from29To30() {
        return List.of(
                "CREATE TABLE IF NOT EXISTS library_raw (id TEXT PRIMARY KEY, name TEXT NOT NULL, original_name TEXT NOT NULL, chapter_no INTEGER, chars INTEGER NOT NULL DEFAULT 0, input_hash TEXT NOT NULL, encoding TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)",
                "CREATE INDEX IF NOT EXISTS idx_library_raw_hash ON library_raw(input_hash)",
                "CREATE TABLE IF NOT EXISTS chapter_sets (id TEXT PRIMARY KEY, name TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS chapter_set_rows (set_id TEXT NOT NULL, ordinal INTEGER NOT NULL, chapter_no INTEGER, raw_id TEXT NOT NULL DEFAULT '', glossary_id TEXT NOT NULL DEFAULT '', pronoun_id TEXT NOT NULL DEFAULT '', PRIMARY KEY(set_id, ordinal))");
    }

    private LibraryMigrationSpec() {}
}
