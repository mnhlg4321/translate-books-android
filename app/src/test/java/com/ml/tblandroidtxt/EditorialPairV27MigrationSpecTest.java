package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Version 27 adds the pair-run tables and two combo columns, and nothing else: no old table is touched, no row is converted. */
public final class EditorialPairV27MigrationSpecTest {
    private static String sql() { return String.join("\n", EditorialPairMigrationSpec.from26To27()).toLowerCase(Locale.ROOT); }

    @Test public void v27AddsFourTablesThreeIndexesAndTwoColumns() {
        List<String> statements = EditorialPairMigrationSpec.from26To27();
        assertEquals(9, statements.size());
        String sql = sql();
        assertEquals(4, sql.split("create table if not exists", -1).length - 1);
        assertEquals(3, sql.split("create index if not exists", -1).length - 1);
        assertEquals(2, sql.split("alter table editorial_combos add column", -1).length - 1);
        for (String table : new String[] {"editorial_pair_runs", "editorial_pair_items", "editorial_pair_reservations", "editorial_pair_journal"}) {
            assertTrue(table, sql.contains("create table if not exists " + table));
        }
        assertTrue(sql.contains("source_kind text not null default 'files'"));
        assertTrue(sql.contains("job_id integer not null default 0"));
    }

    @Test public void v27IsAdditiveOnlyAndNeverTouchesTheOldRunsOrTheJobTables() {
        String sql = sql();
        for (String forbidden : new String[] {"drop ", "delete from", "insert into", "insert or replace", "update ", "alter table editorial_api_runs", "alter table jobs", "alter table chunks"}) {
            assertFalse(forbidden, sql.contains(forbidden));
        }
        // the job tables are never named: a pair run copies its rows and keeps no foreign key into the job
        for (String job : new String[] {" jobs", " chunks", "chunk_attempts", "prepared_"}) assertFalse(job, sql.contains(job));
        // the only existing table named is the combo: by foreign key and by the two ALTERs
        assertTrue(sql.contains("references editorial_combos(id) on delete cascade"));
    }

    @Test public void statesArmsAndReservationStatesAreClosedSetsMatchingTheEngine() {
        String sql = sql();
        assertTrue(sql.contains("arm in ('c','w')"));
        for (com.ml.tblandroidtxt.editorial.api.PairStates.PairState s : com.ml.tblandroidtxt.editorial.api.PairStates.PairState.values()) {
            assertTrue(s.name(), sql.contains("'" + s.name().toLowerCase(Locale.ROOT) + "'"));
        }
        for (com.ml.tblandroidtxt.editorial.api.PairStates.RunState s : com.ml.tblandroidtxt.editorial.api.PairStates.RunState.values()) {
            assertTrue(s.name(), sql.contains("'" + s.name().toLowerCase(Locale.ROOT) + "'"));
        }
        assertTrue(sql.contains("state in ('reserved','settled','settled_zero')"));
        assertTrue(sql.contains("unique(run_id, pair_id)"));
        // the ledger keeps every settlement once: the call id is the key
        assertTrue(sql.contains("call_id text primary key"));
    }

    @Test public void theRepositoryRunsTheV27StepOnCreateAndFromEveryOlderVersion() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/com/ml/tblandroidtxt/TranslationRepository.java")),
                java.nio.charset.StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertTrue(source.contains("VER = 27"));
        assertTrue(source.contains("if (oldVersion < 27) createEditorialPairTables(db);"));
        assertTrue(source.indexOf("if (oldVersion < 26) createEditorialApiTables(db);") < source.indexOf("if (oldVersion < 27) createEditorialPairTables(db);"));
    }
}
