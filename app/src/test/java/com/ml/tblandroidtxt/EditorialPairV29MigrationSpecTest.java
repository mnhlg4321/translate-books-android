package com.ml.tblandroidtxt;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** V29 only adds the chunk plan column to the pair runs; nothing is rebuilt and old runs keep an empty plan. */
public final class EditorialPairV29MigrationSpecTest {
    @Test public void v29AddsOneColumnWithAnEmptyDefault() {
        assertEquals(1, EditorialPairV29MigrationSpec.from28To29().size());
        String sql = EditorialPairV29MigrationSpec.from28To29().get(0).toLowerCase(java.util.Locale.ROOT);
        assertTrue(sql.startsWith("alter table editorial_pair_runs add column chunk_plan_json text not null default ''"));
    }

    @Test public void theRepositoryRunsItOnCreateAndOnUpgradeFrom28() throws Exception {
        Path path = Paths.get("src/main/java/com/ml/tblandroidtxt/TranslationRepository.java");
        String source = new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("VER = 29"));
        assertTrue(source.contains("if (oldVersion < 29) migrateEditorialPairRunsToV29(db);"));
        assertTrue(source.indexOf("if (oldVersion < 28) migrateEditorialApiRunsToV28(db);") < source.indexOf("if (oldVersion < 29) migrateEditorialPairRunsToV29(db);"));
        assertTrue(source.contains("migrateEditorialApiRunsToV28(db);\n        migrateEditorialPairRunsToV29(db);\n    }"));
    }
}
