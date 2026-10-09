package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairReservation;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import com.ml.tblandroidtxt.editorial.api.PairStates.RunState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Real SQLite for the chunk-pair tables: the store, the journal windows (the process "dies" between operations and a new
 * service opens the same database), the migration from version 26 without losing old data, and the read-only use of a
 * translation job. These classes are written and compiled in the offline package; they have NOT been run on a device.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialPairStoreInstrumentedTest {
    private static final EditorialApiRunService.Pricing PRICING = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.00000025"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };

    private Context context;
    private String databaseName;
    private final List<TranslationRepository> open = new ArrayList<>();

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-pair-" + UUID.randomUUID() + ".db";
    }

    @After public void tearDown() {
        for (TranslationRepository r : open) r.close();
        context.deleteDatabase(databaseName);
    }

    private SqliteEditorialPairRunStore store() {
        TranslationRepository repository = new TranslationRepository(context, databaseName);
        open.add(repository);
        return new SqliteEditorialPairRunStore(repository);
    }

    private long combo(SqliteEditorialPairRunStore pairs) {
        // a combo row is the parent of every run: the same repository owns both tables
        SQLiteDatabase db = pairs.databaseForTest();
        ContentValues v = new ContentValues();
        v.put("name", "ghép thử");
        v.put("created_at", 1L);
        v.put("updated_at", 1L);
        v.put("source_kind", "JOB");
        v.put("job_id", 5L);
        return db.insertOrThrow("editorial_combos", null, v);
    }

    private EditorialPairSource source(int rows) {
        EditorialPairSource base = EditorialPairTestData.source(rows);
        List<String> flagged = new ArrayList<>();
        for (String row : base.draftRows) flagged.add(row.replace("dòng 3:", "dòng 3: かな"));
        return new EditorialPairSource(base.kind, base.ref, base.label, base.chapterId,
                base.rawRows, flagged, base.lineageIssues);
    }

    private FakeEditorialApiProvider identity() {
        return new FakeEditorialApiProvider((request, index) -> FakeEditorialApiProvider.text(
                EditorialPairTestData.targetedAnswer(request, index, (callIndex, draft) -> "="), "stop"));
    }

    // ---- the store and its transactions

    @Test public void aRunSurvivesClosingTheDatabaseWithEveryPairItsCandidateAndItsGate() throws Exception {
        long runId;
        long comboId;
        FakeEditorialApiProvider fake = identity();
        try (SqliteEditorialPairRunStore store = store()) {
            comboId = combo(store);
            EditorialPairRunService service = new EditorialPairRunService(store, fake, PRICING, 60_000L);
            PairRun run = service.prepare(comboId, source(3), "", "", null, "m", "Vietnamese", new BigDecimal("0.05"), "C");
            runId = run.id;
            PairRun done = service.execute(runId, null);
            assertEquals(RunState.FINAL_ELIGIBLE, done.state);
        }
        int requests = fake.requests.size();
        try (SqliteEditorialPairRunStore store = store()) {
            PairRun stored = store.getRun(runId);
            assertNotNull(stored);
            assertEquals(RunState.FINAL_ELIGIBLE, stored.state);
            assertEquals(3, store.items(runId).size());
            for (PairItem i : store.items(runId)) {
                assertEquals(PairState.ACCEPTED, i.state);
                assertTrue(i.gateJson.contains("\"status\":\"PASS\""));
                assertFalse(i.responseText.isEmpty());
            }
            assertTrue(store.openReservations(runId).isEmpty());
            assertEquals(0, new BigDecimal("0.012").compareTo(store.exposure(runId)));
            assertEquals(stored.mergedText, EditorialPairSnapshot.of(stored).map.draft.text);
            assertEquals(runId, store.latestRun(comboId).id);
            // reopening reads and never sends
            EditorialPairRunService reopened = new EditorialPairRunService(store, new FakeEditorialApiProvider((r, i) -> { throw new AssertionError("no call on reopen"); }), PRICING, 1L);
            assertTrue(reopened.exportPlan(runId).complete);
        }
        assertEquals(requests, fake.requests.size());
    }

    @Test public void deletingTheComboRemovesItsPairRunsItemsReservationsAndJournal() throws Exception {
        try (SqliteEditorialPairRunStore store = store()) {
            long comboId = combo(store);
            EditorialPairRunService service = new EditorialPairRunService(store, identity(), PRICING, 60_000L);
            PairRun run = service.prepare(comboId, source(2), "", "", null, "m", "Vietnamese", new BigDecimal("0.05"), "C");
            service.execute(run.id, null);
            store.databaseForTest().delete("editorial_combos", "id=?", new String[] {String.valueOf(comboId)});
            assertNull(store.getRun(run.id));
            assertTrue(store.items(run.id).isEmpty());
            assertTrue(store.journal(run.id).isEmpty());
            assertEquals(0, store.exposure(run.id).signum());
        }
    }

    @Test public void aReservationIsSettledOnceAndAMoveThatTheContractForbidsIsRefusedByTheStore() throws Exception {
        try (SqliteEditorialPairRunStore store = store()) {
            long comboId = combo(store);
            PairRun run = new PairRun();
            run.comboId = comboId;
            PairItem item = new PairItem();
            item.pairId = "p1";
            item.ordinal = 1;
            long runId = store.insertRun(run, List.of(item));
            PairReservation r = new PairReservation();
            r.callId = "c1";
            r.runId = runId;
            r.pairId = "p1";
            r.worstUsd = new BigDecimal("0.01");
            assertTrue(store.reserve(r, new BigDecimal("0.02")));
            PairReservation second = new PairReservation();
            second.callId = "c2";
            second.runId = runId;
            second.pairId = "p1";
            second.worstUsd = new BigDecimal("0.02");
            assertFalse("settled + open + worst above the cap is refused and writes nothing", store.reserve(second, new BigDecimal("0.02")));
            assertEquals(1, store.openReservations(runId).size());
            try {
                store.setItemState(runId, "p1", PairState.IMPORTED, PairState.E_SENT, 1, "c1", "SENT");
                fail("IMPORTED cannot jump to E_SENT");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("MOVE_NOT_ALLOWED"));
            }
            store.setItemState(runId, "p1", PairState.IMPORTED, PairState.E_RESERVED, 1, "c1", "RESERVED");
            store.setItemState(runId, "p1", PairState.E_RESERVED, PairState.E_SENT, 1, "c1", "SENT");
            store.settleZero("c1", "NOT_DISPATCHED");
            try {
                store.settleZero("c1", "again");
                fail("a reservation is settled once");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("SETTLE_TWICE"));
            }
        }
    }

    // ---- journal windows on the real database

    @Test public void deathBetweenLedgerJournalAnswerAndCommitIsReadFromTheDatabaseOnTheNextOpen() throws Exception {
        String[][] windows = {{"after", "reserve", "UNKNOWN", "1"}, {"after", "state:E_RESERVED", "IMPORTED", "0"}, {"before", "commitReceived", "UNKNOWN", "1"}};
        for (String[] w : windows) {
            context.deleteDatabase(databaseName);
            long runId;
            FakeEditorialApiProvider fake = identity();
            try (SqliteEditorialPairRunStore real = store()) {
                long comboId = combo(real);
                FaultyPairRunStore faulty = new FaultyPairRunStore(real);
                if (w[0].equals("after")) faulty.crashAfter(w[1]); else faulty.crashBefore(w[1]);
                EditorialPairRunService service = new EditorialPairRunService(faulty, fake, PRICING, 60_000L);
                PairRun run = service.prepare(comboId, source(2), "", "", null, "m", "Vietnamese", new BigDecimal("0.05"), "C");
                runId = run.id;
                try {
                    service.execute(runId, null);
                    fail("the simulated death did not happen at " + w[1]);
                } catch (FaultyPairRunStore.SimulatedCrash expected) {
                    // the process is gone
                }
            }
            int sentBefore = fake.requests.size();
            try (SqliteEditorialPairRunStore reopened = store()) {
                EditorialPairRunService restarted = new EditorialPairRunService(reopened, fake, PRICING, 60_000L);
                assertTrue(w[1], restarted.needsRecovery(runId));
                restarted.recover(runId);
                assertEquals(w[1], PairState.valueOf(w[2]), reopened.items(runId).get(0).state);
                assertEquals(w[1], Integer.parseInt(w[3]), reopened.openReservations(runId).size());
                assertEquals("recovery never sends", sentBefore, fake.requests.size());
            }
        }
    }

    // ---- migration

    @Test public void upgradeFromVersion26KeepsEveryOldRowAndAddsOnlyThePairTablesAndTheTwoComboColumns() throws Exception {
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null);
        for (String sql : EditorialApiMigrationSpec.from25To26()) old.execSQL(sql);
        old.execSQL("CREATE TABLE jobs (id INTEGER PRIMARY KEY, title TEXT)");
        old.execSQL("INSERT INTO jobs(id,title) VALUES (3,'sách cũ')");
        ContentValues combo = new ContentValues();
        combo.put("name", "tổ hợp cũ");
        combo.put("raw_uri", "content://x/raw");
        combo.put("created_at", 1L);
        combo.put("updated_at", 1L);
        long comboId = old.insertOrThrow("editorial_combos", null, combo);
        ContentValues run = new ContentValues();
        run.put("combo_id", comboId);
        run.put("contract_revision", "EDITORIAL_API_V1.1");
        run.put("mode", "QUICK");
        run.put("state", "FINAL_OK");
        run.put("raw_sha256", "a");
        run.put("raw_text", "RAW cũ");
        run.put("draft_sha256", "b");
        run.put("draft_text", "DRAFT cũ");
        run.put("final_text", "bản cuối cũ");
        run.put("created_at", 1L);
        run.put("updated_at", 1L);
        old.insertOrThrow("editorial_api_runs", null, run);
        old.setVersion(26);
        old.close();

        try (SqliteEditorialPairRunStore store = store()) {
            SQLiteDatabase db = store.databaseForTest();
            assertEquals(29, db.getVersion());
            for (String table : new String[] {"editorial_pair_runs", "editorial_pair_items", "editorial_pair_reservations", "editorial_pair_journal"}) {
                try (Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[] {table})) { assertTrue(table, c.moveToFirst()); }
            }
            try (Cursor c = db.rawQuery("SELECT title FROM jobs WHERE id=3", null)) { assertTrue(c.moveToFirst()); assertEquals("sách cũ", c.getString(0)); }
            try (Cursor c = db.rawQuery("SELECT final_text, state FROM editorial_api_runs WHERE combo_id=?", new String[] {String.valueOf(comboId)})) {
                assertTrue(c.moveToFirst());
                assertEquals("bản cuối cũ", c.getString(0));
                assertEquals("FINAL_OK", c.getString(1));
            }
            try (Cursor c = db.rawQuery("SELECT source_kind, job_id FROM editorial_combos WHERE id=?", new String[] {String.valueOf(comboId)})) {
                assertTrue(c.moveToFirst());
                assertEquals("an old combo is a two-file combo, never converted", "FILES", c.getString(0));
                assertEquals(0L, c.getLong(1));
            }
            // the old whole-chapter run is not a chunk run: no pair run, item, reservation or journal row appeared
            for (String table : new String[] {"editorial_pair_runs", "editorial_pair_items", "editorial_pair_reservations", "editorial_pair_journal"}) {
                try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) { assertTrue(c.moveToFirst()); assertEquals(table, 0, c.getInt(0)); }
            }
        }
    }

    // ---- a translation job is only read

    @Test public void aJobIsReadWithoutChangingARowAStatusOrAnOutputAndTheRunStaysInItsOwnTables() throws Exception {
        TranslationRepository repository = new TranslationRepository(context, databaseName);
        open.add(repository);
        AppSettings settings = new AppSettings();
        List<Chunk> chunks = new ArrayList<>();
        String[] sources = {"一行目の原文です。花子が言った。\n\n", "二行目の原文です。太郎が頷いた。\n\n", "三行目の原文です。夜が更けた。\n"};
        int offset = 0;
        for (int i = 0; i < sources.length; i++) {
            chunks.add(new Chunk(i, offset, offset + sources[i].length(), "", sources[i], "", ""));
            offset += sources[i].length();
        }
        long jobId = repository.createJob("content://x/raw", "content://x/out", "truyen.txt", settings, chunks);
        repository.markChunkDone(jobId, 0, "Dòng một của bản dịch có đủ chữ để cổng cấu trúc đo được kết quả.\n\n");
        repository.markChunkDone(jobId, 1, "Dòng hai của bản dịch có đủ chữ để cổng cấu trúc đo được kết quả.\n\n");
        String before = describe(repository, jobId);
        String statusBefore = repository.getJob(jobId).status;

        EditorialPairSource source = EditorialPairSource.fromJobRows(jobId, "truyen.txt", repository.getChunkRows(jobId));
        assertEquals(3, source.rawRows.size());
        assertEquals("row 3 has no completed translation: a missing pair, not a guess", 1, java.util.Collections.frequency(source.draftRows, null));
        assertTrue(source.lineageIssues.isEmpty());
        EditorialPairPreview preview = EditorialPairPreview.of(source, List.of(), "", null);
        assertFalse(preview.runnable());
        assertTrue(preview.blockers.contains("MISSING_PAIRS:1"));

        repository.markChunkDone(jobId, 2, "Dòng ba của bản dịch có đủ chữ để cổng cấu trúc đo được kết quả.\n");
        String completeBefore = describe(repository, jobId);
        EditorialPairSource full = EditorialPairSource.fromJobRows(jobId, "truyen.txt", repository.getChunkRows(jobId));
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(repository)) {
            long comboId = combo(store);
            EditorialPairRunService service = new EditorialPairRunService(store, identity2(), PRICING, 60_000L);
            PairRun run = service.prepare(comboId, full, "", "", null, "m", "Vietnamese", new BigDecimal("0.05"), "C");
            service.execute(run.id, null);
        }
        assertEquals("not a row, a status or an output of the job changed", completeBefore, describe(repository, jobId));
        assertNotNull(statusBefore);
        assertFalse(before.equals(completeBefore)); // the only change is the one this test made itself
    }

    private FakeEditorialApiProvider identity2() {
        return new FakeEditorialApiProvider((request, index) -> FakeEditorialApiProvider.text(
                "<EDITED>" + EditorialPairTestData.draftPart(request) + "</EDITED>", "stop"));
    }

    private static String describe(TranslationRepository repository, long jobId) {
        StringBuilder sb = new StringBuilder();
        TranslationRepository.Job job = repository.getJob(jobId);
        sb.append(job.status).append('|').append(job.outputUri).append('|').append(job.fileName).append('\n');
        for (TranslationRepository.ChunkRow r : repository.getChunkRows(jobId)) {
            sb.append(r.index).append('|').append(r.status).append('|').append(r.source).append('|').append(r.translated).append('|').append(r.stableId)
                    .append('|').append(r.responseHash).append('\n');
        }
        return sb.toString();
    }
}
