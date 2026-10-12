package com.ml.tblandroidtxt;

import java.util.List;

/**
 * Version 29 adds one column to the pair runs: the chunk plan (CS-1) that cut the two files into chunks, stored as JSON without
 * any book text. Old runs keep an empty plan; nothing else changes.
 */
public final class EditorialPairV29MigrationSpec {
    public static List<String> from28To29() {
        return List.of("ALTER TABLE editorial_pair_runs ADD COLUMN chunk_plan_json TEXT NOT NULL DEFAULT ''");
    }

    private EditorialPairV29MigrationSpec() { }
}
