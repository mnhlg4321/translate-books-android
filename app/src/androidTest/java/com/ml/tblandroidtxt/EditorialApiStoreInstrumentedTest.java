package com.ml.tblandroidtxt;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Real SQLite: Editorial API persistence, V5 attachment snapshots and additive upgrades without losing old data. */
@RunWith(AndroidJUnit4.class)
public final class EditorialApiStoreInstrumentedTest {
    private Context context;
    private String databaseName;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-api-v1-" + UUID.randomUUID() + ".db";
    }

    @After public void tearDown() { context.deleteDatabase(databaseName); }

    private SqliteEditorialApiStore open() { return new SqliteEditorialApiStore(new TranslationRepository(context, databaseName)); }

    private static EditorialApiCombo combo() {
        EditorialApiCombo combo = new EditorialApiCombo();
        combo.name = "Chương 1 (test tổng hợp)";
        combo.rawUri = "content://synthetic/raw";
        combo.rawName = "raw.txt";
        combo.draftUri = "content://synthetic/draft";
        combo.draftName = "draft.txt";
        combo.glossaryId = "g-1";
        combo.pronounId = "";
        combo.settingsJson = new EditorialApiCombo.Settings().toJson();
        return combo;
    }

    private static EditorialApiRun run(long comboId) {
        EditorialApiRun run = new EditorialApiRun();
        run.comboId = comboId;
        run.model = "fake-model";
        run.mode = EditorialApiContract.Mode.THOROUGH;
        run.state = EditorialApiContract.RunState.FINAL_NOTES;
        run.rawText = "RAW dòng một\nRAW dòng hai";
        run.draftText = "Bản nháp một\nBản nháp hai";
        run.finalText = "Bản cuối một\nBản cuối hai";
        run.notesJson = "[{\"quote\":\"một\",\"kind\":\"TONE\",\"reason\":\"x\"}]";
        run.usd = new BigDecimal("0.0123456");
        run.calls = 3;
        run.inputTokens = 1234;
        run.outputTokens = 567;
        run.glossaryEntries = 2;
        return run;
    }

    @Test public void freshDatabaseIsV28WithV5SnapshotColumn() {
        try (SqliteEditorialApiStore store = open()) {
            SQLiteDatabase db = store.databaseForTest();
            assertEquals(29, db.getVersion());
            assertTrue(tableExists(db, "editorial_combos"));
            assertTrue(tableExists(db, "editorial_api_runs"));
            assertTrue(columnExists(db, "editorial_api_runs", "original_source_files_json"));
        }
    }

    @Test public void v5ModeAndOriginalSourceSnapshotSurviveAStoreReopen() {
        long comboId;
        long runId;
        String sourceSnapshot = "[{\"name\":\"RAW.txt\",\"content\":\"synthetic raw\"}]";
        try (SqliteEditorialApiStore store = open()) {
            comboId = store.insertCombo(combo());
            EditorialApiRun run = run(comboId);
            run.mode = EditorialApiContract.Mode.V5_CHAT;
            run.originalSourceFilesJson = sourceSnapshot;
            runId = store.insertRun(run);
        }
        try (SqliteEditorialApiStore store = open()) {
            EditorialApiRun restored = store.getRun(runId);
            assertEquals(EditorialApiContract.Mode.V5_CHAT, restored.mode);
            assertEquals(sourceSnapshot, restored.originalSourceFilesJson);
        }
    }

    @Test public void comboAndRunSurviveClosingAndReopeningTheDatabase() {
        long comboId;
        long runId;
        try (SqliteEditorialApiStore store = open()) {
            EditorialApiCombo combo = combo();
            comboId = store.insertCombo(combo);
            EditorialApiRun run = run(comboId);
            runId = store.insertRun(run);
        }
        try (SqliteEditorialApiStore store = open()) {
            EditorialApiCombo combo = store.getCombo(comboId);
            assertNotNull(combo);
            assertEquals("Chương 1 (test tổng hợp)", combo.name);
            assertEquals("g-1", combo.glossaryId);
            assertEquals("", combo.pronounId);
            EditorialApiRun run = store.getRun(runId);
            assertNotNull(run);
            assertEquals("Bản cuối một\nBản cuối hai", run.finalText);
            assertEquals(EditorialApiContract.RunState.FINAL_NOTES, run.state);
            assertEquals(EditorialApiContract.Mode.THOROUGH, run.mode);
            assertEquals(new BigDecimal("0.0123456"), run.usd);
            assertEquals(3, run.calls);
            assertEquals(runId, store.latestRun(comboId).id);
            assertEquals(1, store.listCombos().size());
        }
    }

    @Test public void updateRunReplacesTheRowAndDeletingTheComboCascadesToItsRuns() {
        try (SqliteEditorialApiStore store = open()) {
            long comboId = store.insertCombo(combo());
            EditorialApiRun run = run(comboId);
            long runId = store.insertRun(run);
            run.state = EditorialApiContract.RunState.CANCELLED;
            run.finalText = "";
            store.updateRun(run);
            assertEquals(EditorialApiContract.RunState.CANCELLED, store.getRun(runId).state);
            assertEquals(1, store.runsOf(comboId).size());
            store.deleteCombo(comboId);
            assertNull(store.getCombo(comboId));
            assertNull(store.getRun(runId));
        }
    }

    @Test public void upgradeFromVersion25KeepsEveryOldTableAndRowAndAddsOnlyTheTwoNewTables() {
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null);
        old.execSQL("CREATE TABLE jobs (id INTEGER PRIMARY KEY, title TEXT)");
        old.execSQL("CREATE TABLE editorial_runs (id TEXT PRIMARY KEY, payload TEXT)");
        old.execSQL("INSERT INTO jobs(id,title) VALUES (7,'sách cũ')");
        old.execSQL("INSERT INTO editorial_runs(id,payload) VALUES ('r1','giữ nguyên')");
        old.setVersion(25);
        old.close();

        try (SqliteEditorialApiStore store = open()) {
            SQLiteDatabase db = store.databaseForTest();
            assertEquals(29, db.getVersion());
            assertTrue(tableExists(db, "editorial_combos"));
            assertTrue(tableExists(db, "editorial_api_runs"));
            try (Cursor c = db.rawQuery("SELECT title FROM jobs WHERE id=7", null)) {
                assertTrue(c.moveToFirst());
                assertEquals("sách cũ", c.getString(0));
            }
            try (Cursor c = db.rawQuery("SELECT payload FROM editorial_runs WHERE id='r1'", null)) {
                assertTrue(c.moveToFirst());
                assertEquals("giữ nguyên", c.getString(0));
            }
            long comboId = store.insertCombo(combo());
            assertTrue(comboId > 0);
        }
    }

    @Test public void upgradeFromVersion27PreservesApiRunDataAndAddsTheV5Mode() {
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null);
        for (String sql : EditorialApiMigrationSpec.from25To26()) old.execSQL(sql);
        for (String sql : EditorialPairMigrationSpec.from26To27()) old.execSQL(sql);
        old.execSQL("INSERT INTO editorial_combos(name,created_at,updated_at) VALUES ('synthetic',1,1)");
        old.execSQL("INSERT INTO editorial_api_runs(combo_id,contract_revision,mode,state,raw_sha256,raw_text,draft_sha256,draft_text,final_text,created_at,updated_at) "
                + "VALUES (1,'EDITORIAL_API_V1.3','QUICK','FINAL_OK','raw-hash','raw','draft-hash','draft','kept-final',2,2)");
        old.setVersion(27);
        old.close();

        try (SqliteEditorialApiStore store = open()) {
            EditorialApiRun restored = store.getRun(1);
            assertEquals(29, store.databaseForTest().getVersion());
            assertNotNull(restored);
            assertEquals("kept-final", restored.finalText);
            assertEquals("raw-hash", restored.rawSha256);
            assertEquals("[]", restored.originalSourceFilesJson);
            restored.mode = EditorialApiContract.Mode.V5_CHAT;
            store.updateRun(restored);
            assertEquals(EditorialApiContract.Mode.V5_CHAT, store.getRun(1).mode);
        }
    }

    private static boolean tableExists(SQLiteDatabase db, String name) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[] {name})) {
            return c.moveToFirst();
        }
    }

    private static boolean columnExists(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) if (column.equals(c.getString(c.getColumnIndexOrThrow("name")))) return true;
            return false;
        }
    }
}
