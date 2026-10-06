package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Version 26 adds the two EDITORIAL_API_V1 tables and changes nothing else. */
public final class EditorialApiV26MigrationSpecTest {
    private static String sql() { return String.join("\n", EditorialApiMigrationSpec.from25To26()).toLowerCase(); }

    @Test public void v26AddsExactlyTwoTablesAndOneIndex() {
        List<String> statements = EditorialApiMigrationSpec.from25To26();
        assertEquals(3, statements.size());
        String sql = sql();
        assertEquals(2, sql.split("create table if not exists", -1).length - 1);
        assertEquals(1, sql.split("create index if not exists", -1).length - 1);
        assertTrue(sql.contains("create table if not exists editorial_combos"));
        assertTrue(sql.contains("create table if not exists editorial_api_runs"));
        assertEquals(0, sql.split("create trigger", -1).length - 1);
        assertEquals(0, sql.split("create unique index", -1).length - 1);
    }

    @Test public void v26IsAdditiveOnly() {
        String sql = sql();
        for (String forbidden : new String[] {"alter table", "drop ", "delete from", "insert into", "insert or replace", "update "}) {
            assertFalse(forbidden, sql.contains(forbidden));
        }
        // the only existing table named is the new combo table, by foreign key
        for (String old : new String[] {"jobs", "chunks", "editorial_projects", "editorial_chapters", "editorial_runs", "editorial_phase_artifacts"}) {
            assertFalse(old, sql.matches("(?s).*\\b" + old + "\\b.*"));
        }
    }

    @Test public void stateAndModeAreClosedSetsAndRunsCascadeWithTheirCombo() {
        String sql = sql();
        assertTrue(sql.contains("mode in ('quick','thorough')"));
        assertTrue(sql.contains("state in ('running','retry_required','wrong_pair','final_ok','final_notes','cancelled')"));
        assertTrue(sql.contains("references editorial_combos(id) on delete cascade"));
        for (String column : new String[] {"raw_sha256", "draft_sha256", "glossary_sha256", "pronoun_sha256", "final_text", "edited_text",
                "notes_json", "issues_json", "guards_json", "steps_json", "quality_core_sha256", "contract_revision", "model"}) {
            assertTrue(column, sql.contains(column));
        }
    }

    @Test public void theRepositoryKeepsTheV26StepAndCreatesItOnAFreshDatabase() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/com/ml/tblandroidtxt/TranslationRepository.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("VER = 27")); // the v26 step below stays in the chain; v27 is covered by EditorialPairV27MigrationSpecTest
        assertTrue(source.contains("if (oldVersion < 26) createEditorialApiTables(db);"));
        String created = source.replace("\r\n", "\n");
        assertTrue(created.contains("createEditorialApiTables(db);\n        createEditorialPairTables(db);\n    }"));
    }
}
