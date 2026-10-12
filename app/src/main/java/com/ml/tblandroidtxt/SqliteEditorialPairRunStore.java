package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairReservation;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.PairStates;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite implementation over the version 27 tables. The ledger (reservations), the item state and the journal share one
 * database, and each operation that must be atomic is one transaction: a restart sees either the whole step or none of it.
 */
public final class SqliteEditorialPairRunStore implements EditorialPairRunStore, AutoCloseable {
    private final TranslationRepository database;

    public SqliteEditorialPairRunStore(Context context) { this(new TranslationRepository(context.getApplicationContext())); }

    SqliteEditorialPairRunStore(TranslationRepository database) { this.database = database; }

    @Override public void close() { database.close(); }

    private SQLiteDatabase db() { return database.getWritableDatabase(); }

    /** For instrumented tests that inspect the schema or remove a parent row. */
    SQLiteDatabase databaseForTest() { return db(); }

    // ---- runs

    @Override public long insertRun(PairRun run, List<PairItem> items) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            run.createdAt = now;
            run.updatedAt = now;
            long id = db.insertOrThrow(EditorialPairMigrationSpec.RUNS, null, runValues(run));
            run.id = id;
            for (PairItem item : items) {
                ContentValues v = itemValues(item);
                v.put("run_id", id);
                item.runId = id;
                item.id = db.insertOrThrow(EditorialPairMigrationSpec.ITEMS, null, v);
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    @Override public PairRun getRun(long id) {
        try (Cursor c = db().rawQuery("SELECT * FROM editorial_pair_runs WHERE id=?", new String[] {String.valueOf(id)})) {
            return c.moveToFirst() ? run(c) : null;
        }
    }

    @Override public void updateRun(PairRun run) {
        run.updatedAt = System.currentTimeMillis();
        db().update(EditorialPairMigrationSpec.RUNS, runValues(run), "id=?", new String[] {String.valueOf(run.id)});
    }

    @Override public PairRun latestRun(long comboId) {
        try (Cursor c = db().rawQuery("SELECT * FROM editorial_pair_runs WHERE combo_id=? ORDER BY created_at DESC, id DESC LIMIT 1",
                new String[] {String.valueOf(comboId)})) {
            return c.moveToFirst() ? run(c) : null;
        }
    }

    @Override public List<PairRun> runsOf(long comboId) {
        List<PairRun> out = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT * FROM editorial_pair_runs WHERE combo_id=? ORDER BY created_at DESC, id DESC", new String[] {String.valueOf(comboId)})) {
            while (c.moveToNext()) out.add(run(c));
        }
        return out;
    }

    @Override public void deleteRun(long id) {
        db().delete(EditorialPairMigrationSpec.RUNS, "id=?", new String[] {String.valueOf(id)});
    }

    private static ContentValues runValues(PairRun r) {
        ContentValues v = new ContentValues();
        v.put("combo_id", r.comboId);
        v.put("arm", r.arm);
        v.put("contract_revision", r.contractRevision);
        v.put("model", r.model);
        v.put("target_language", r.targetLanguage);
        v.put("state", r.state.name());
        v.put("source_kind", r.sourceKind);
        v.put("source_ref", r.sourceRef);
        v.put("source_label", r.sourceLabel);
        v.put("chapter_id", r.chapterId);
        v.put("raw_rows_json", r.rawRowsJson);
        v.put("draft_rows_json", r.draftRowsJson);
        v.put("glossary_text", r.glossaryText);
        v.put("glossary_sha256", r.glossarySha256);
        v.put("pronoun_text", r.pronounText);
        v.put("pronoun_sha256", r.pronounSha256);
        v.put("cue_fields", r.cueFields);
        v.put("map_revision", r.mapRevision);
        v.put("map_hash", r.mapHash);
        v.put("cap_usd", r.capUsd.toPlainString());
        v.put("started", r.started ? 1 : 0);
        v.put("paused", r.paused ? 1 : 0);
        v.put("cost_overrun", r.costOverrun ? 1 : 0);
        v.put("merged_text", r.mergedText);
        v.put("merge_receipt_json", r.mergeReceiptJson);
        v.put("chunk_plan_json", r.chunkPlanJson);
        v.put("warnings", r.warnings);
        v.put("calls", r.calls);
        v.put("input_tokens", r.inputTokens);
        v.put("output_tokens", r.outputTokens);
        v.put("usd", r.usd.toPlainString());
        v.put("cost_known", r.costKnown ? 1 : 0);
        v.put("error", r.error);
        v.put("created_at", r.createdAt);
        v.put("updated_at", r.updatedAt);
        return v;
    }

    private static PairRun run(Cursor c) {
        PairRun r = new PairRun();
        r.id = c.getLong(c.getColumnIndexOrThrow("id"));
        r.comboId = c.getLong(c.getColumnIndexOrThrow("combo_id"));
        r.arm = text(c, "arm");
        r.contractRevision = text(c, "contract_revision");
        r.model = text(c, "model");
        r.targetLanguage = text(c, "target_language");
        r.state = PairStates.RunState.valueOf(text(c, "state"));
        r.sourceKind = text(c, "source_kind");
        r.sourceRef = text(c, "source_ref");
        r.sourceLabel = text(c, "source_label");
        r.chapterId = text(c, "chapter_id");
        r.rawRowsJson = text(c, "raw_rows_json");
        r.draftRowsJson = text(c, "draft_rows_json");
        r.glossaryText = text(c, "glossary_text");
        r.glossarySha256 = text(c, "glossary_sha256");
        r.pronounText = text(c, "pronoun_text");
        r.pronounSha256 = text(c, "pronoun_sha256");
        r.cueFields = text(c, "cue_fields");
        r.mapRevision = text(c, "map_revision");
        r.mapHash = text(c, "map_hash");
        r.capUsd = decimal(text(c, "cap_usd"));
        r.started = c.getInt(c.getColumnIndexOrThrow("started")) != 0;
        r.paused = c.getInt(c.getColumnIndexOrThrow("paused")) != 0;
        r.costOverrun = c.getInt(c.getColumnIndexOrThrow("cost_overrun")) != 0;
        r.mergedText = text(c, "merged_text");
        r.mergeReceiptJson = text(c, "merge_receipt_json");
        r.chunkPlanJson = text(c, "chunk_plan_json");
        r.warnings = c.getInt(c.getColumnIndexOrThrow("warnings"));
        r.calls = c.getInt(c.getColumnIndexOrThrow("calls"));
        r.inputTokens = c.getLong(c.getColumnIndexOrThrow("input_tokens"));
        r.outputTokens = c.getLong(c.getColumnIndexOrThrow("output_tokens"));
        r.usd = decimal(text(c, "usd"));
        r.costKnown = c.getInt(c.getColumnIndexOrThrow("cost_known")) != 0;
        r.error = text(c, "error");
        r.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        r.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        return r;
    }

    // ---- items

    @Override public List<PairItem> items(long runId) {
        List<PairItem> out = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT * FROM editorial_pair_items WHERE run_id=? ORDER BY ordinal, id", new String[] {String.valueOf(runId)})) {
            while (c.moveToNext()) out.add(item(c));
        }
        return out;
    }

    @Override public PairItem item(long runId, String pairId) {
        try (Cursor c = db().rawQuery("SELECT * FROM editorial_pair_items WHERE run_id=? AND pair_id=?", new String[] {String.valueOf(runId), pairId})) {
            return c.moveToFirst() ? item(c) : null;
        }
    }

    private static ContentValues itemValues(PairItem i) {
        ContentValues v = new ContentValues();
        v.put("pair_id", i.pairId);
        v.put("ordinal", i.ordinal);
        v.put("state", i.state.name());
        v.put("attempt", i.attempt);
        v.put("generation", i.generation);
        v.put("request_id", i.requestId);
        v.put("candidate_text", i.candidateText);
        v.put("response_text", i.responseText);
        v.put("gate_json", i.gateJson);
        v.put("response_hash", i.responseHash);
        v.put("calls", i.calls);
        v.put("input_tokens", i.inputTokens);
        v.put("output_tokens", i.outputTokens);
        v.put("usd", i.usd.toPlainString());
        v.put("cost_known", i.costKnown ? 1 : 0);
        v.put("error", i.error);
        v.put("updated_at", System.currentTimeMillis());
        return v;
    }

    private static PairItem item(Cursor c) {
        PairItem i = new PairItem();
        i.id = c.getLong(c.getColumnIndexOrThrow("id"));
        i.runId = c.getLong(c.getColumnIndexOrThrow("run_id"));
        i.pairId = text(c, "pair_id");
        i.ordinal = c.getInt(c.getColumnIndexOrThrow("ordinal"));
        i.state = PairState.valueOf(text(c, "state"));
        i.attempt = c.getInt(c.getColumnIndexOrThrow("attempt"));
        i.generation = c.getInt(c.getColumnIndexOrThrow("generation"));
        i.requestId = text(c, "request_id");
        i.candidateText = text(c, "candidate_text");
        i.responseText = text(c, "response_text");
        i.gateJson = text(c, "gate_json");
        i.responseHash = text(c, "response_hash");
        i.calls = c.getInt(c.getColumnIndexOrThrow("calls"));
        i.inputTokens = c.getLong(c.getColumnIndexOrThrow("input_tokens"));
        i.outputTokens = c.getLong(c.getColumnIndexOrThrow("output_tokens"));
        i.usd = decimal(text(c, "usd"));
        i.costKnown = c.getInt(c.getColumnIndexOrThrow("cost_known")) != 0;
        i.error = text(c, "error");
        i.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        return i;
    }

    private PairState stateOf(SQLiteDatabase db, long runId, String pairId) {
        try (Cursor c = db.rawQuery("SELECT state FROM editorial_pair_items WHERE run_id=? AND pair_id=?", new String[] {String.valueOf(runId), pairId})) {
            if (!c.moveToFirst()) throw new IllegalStateException("no item " + pairId);
            return PairState.valueOf(c.getString(0));
        }
    }

    private static void journal(SQLiteDatabase db, long runId, String pairId, String event) {
        ContentValues v = new ContentValues();
        v.put("run_id", runId);
        v.put("pair_id", pairId);
        v.put("event", event);
        v.put("created_at", System.currentTimeMillis());
        db.insertOrThrow(EditorialPairMigrationSpec.JOURNAL, null, v);
    }

    // ---- ledger and journal

    @Override public boolean reserve(PairReservation r, BigDecimal capUsd) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            if (exposure(db, r.runId).add(r.worstUsd).compareTo(capUsd) > 0) return false;
            long now = System.currentTimeMillis();
            ContentValues v = new ContentValues();
            v.put("call_id", r.callId);
            v.put("run_id", r.runId);
            v.put("pair_id", r.pairId);
            v.put("worst_usd", r.worstUsd.toPlainString());
            v.put("state", PairReservation.RESERVED);
            v.put("created_at", now);
            v.put("updated_at", now);
            db.insertOrThrow(EditorialPairMigrationSpec.RESERVATIONS, null, v);
            journal(db, r.runId, r.pairId, "RESERVE");
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    @Override public void setItemState(long runId, String pairId, PairState from, PairState to, int attempt, String requestId, String journalEvent) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            PairState current = stateOf(db, runId, pairId);
            if (current != from) throw new IllegalStateException("EXPECTED " + from + " BUT " + current);
            if (!PairStates.canMove(from, to)) throw new IllegalStateException("MOVE_NOT_ALLOWED " + from + "->" + to);
            ContentValues v = new ContentValues();
            v.put("state", to.name());
            v.put("attempt", attempt);
            if (requestId != null && !requestId.isEmpty()) v.put("request_id", requestId);
            v.put("updated_at", System.currentTimeMillis());
            db.update(EditorialPairMigrationSpec.ITEMS, v, "run_id=? AND pair_id=?", new String[] {String.valueOf(runId), pairId});
            journal(db, runId, pairId, journalEvent);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public void commitNoFixPoints(long runId, String pairId, String unchangedText, String detailsJson) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            if (stateOf(db, runId, pairId) != PairState.IMPORTED) throw new IllegalStateException("NO_FIX_ONLY_WHEN_IMPORTED");
            if (!PairStates.canMove(PairState.IMPORTED, PairState.ACCEPTED)) throw new IllegalStateException("NO_FIX_MOVE_NOT_ALLOWED");
            ContentValues v = new ContentValues();
            v.put("state", PairState.ACCEPTED.name());
            v.put("candidate_text", unchangedText == null ? "" : unchangedText);
            v.put("response_text", "");
            v.put("gate_json", detailsJson == null ? "{}" : detailsJson);
            v.put("response_hash", "");
            v.put("calls", 0);
            v.put("input_tokens", 0);
            v.put("output_tokens", 0);
            v.put("usd", "0");
            v.put("cost_known", 1);
            v.put("error", "");
            v.put("updated_at", System.currentTimeMillis());
            db.update(EditorialPairMigrationSpec.ITEMS, v, "run_id=? AND pair_id=?", new String[] {String.valueOf(runId), pairId});
            journal(db, runId, pairId, "NO_FIX_POINTS");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public void bumpAttempt(long runId, String pairId, int attempt, String requestId) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            if (stateOf(db, runId, pairId) != PairState.E_SENT) throw new IllegalStateException("BUMP_ONLY_WHEN_SENT");
            ContentValues v = new ContentValues();
            v.put("attempt", attempt);
            v.put("request_id", requestId);
            v.put("updated_at", System.currentTimeMillis());
            db.update(EditorialPairMigrationSpec.ITEMS, v, "run_id=? AND pair_id=?", new String[] {String.valueOf(runId), pairId});
            journal(db, runId, pairId, "RETRY_" + attempt);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public void commitReceived(long runId, PairItem received, String callId, BigDecimal settledUsd, String settleReason) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            if (stateOf(db, runId, received.pairId) != PairState.E_SENT) throw new IllegalStateException("RECEIVE_ONLY_WHEN_SENT");
            if (!PairStates.canMove(PairState.E_RECEIVED, received.state)) throw new IllegalStateException("BAD_TARGET " + received.state);
            settle(db, callId, settledUsd, settleReason);
            ContentValues v = itemValues(received);
            v.remove("pair_id");
            v.remove("ordinal");
            v.remove("generation");
            db.update(EditorialPairMigrationSpec.ITEMS, v, "run_id=? AND pair_id=?", new String[] {String.valueOf(runId), received.pairId});
            journal(db, runId, received.pairId, "RECEIVED " + received.state);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public void commitFailedNotBilled(long runId, PairItem failed) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            if (stateOf(db, runId, failed.pairId) != PairState.E_SENT) throw new IllegalStateException("FAIL_ONLY_WHEN_SENT");
            ContentValues v = itemValues(failed);
            v.remove("pair_id");
            v.remove("ordinal");
            v.remove("generation");
            v.put("state", PairState.STRUCTURE_BLOCKED.name());
            v.put("usd", "0");
            v.put("cost_known", 1);
            db.update(EditorialPairMigrationSpec.ITEMS, v, "run_id=? AND pair_id=?", new String[] {String.valueOf(runId), failed.pairId});
            journal(db, runId, failed.pairId, "FAILED_NOT_BILLED");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public void commitUnknown(long runId, String pairId, String error) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("state", PairState.UNKNOWN.name());
            v.put("error", error);
            v.put("cost_known", 0);
            v.put("updated_at", System.currentTimeMillis());
            db.update(EditorialPairMigrationSpec.ITEMS, v, "run_id=? AND pair_id=?", new String[] {String.valueOf(runId), pairId});
            journal(db, runId, pairId, "UNKNOWN");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public void settleZero(String callId, String reason) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            settle(db, callId, BigDecimal.ZERO, reason);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** One settlement per reservation, never two. A zero settlement is a different state from a real charge. */
    private static void settle(SQLiteDatabase db, String callId, BigDecimal usd, String reason) {
        try (Cursor c = db.rawQuery("SELECT state FROM editorial_pair_reservations WHERE call_id=?", new String[] {callId})) {
            if (!c.moveToFirst() || !PairReservation.RESERVED.equals(c.getString(0))) throw new IllegalStateException("SETTLE_TWICE_OR_MISSING " + callId);
        }
        ContentValues v = new ContentValues();
        v.put("state", usd.signum() == 0 ? PairReservation.SETTLED_ZERO : PairReservation.SETTLED);
        v.put("settled_usd", usd.toPlainString());
        v.put("reason", reason);
        v.put("updated_at", System.currentTimeMillis());
        db.update(EditorialPairMigrationSpec.RESERVATIONS, v, "call_id=?", new String[] {callId});
    }

    @Override public void revertToImported(long runId, String pairId, String reason) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            ContentValues z = new ContentValues();
            z.put("state", PairReservation.SETTLED_ZERO);
            z.put("settled_usd", "0");
            z.put("reason", reason);
            z.put("updated_at", System.currentTimeMillis());
            db.update(EditorialPairMigrationSpec.RESERVATIONS, z, "run_id=? AND pair_id=? AND state=?",
                    new String[] {String.valueOf(runId), pairId, PairReservation.RESERVED});
            db.execSQL("UPDATE editorial_pair_items SET state=?, generation=generation+1, updated_at=? WHERE run_id=? AND pair_id=?",
                    new Object[] {PairState.IMPORTED.name(), System.currentTimeMillis(), runId, pairId});
            journal(db, runId, pairId, "REVERTED " + reason);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public void resolveWarning(long runId, String pairId, PairState to, String note) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            if (stateOf(db, runId, pairId) != PairState.WARN_REVIEW || !PairStates.canMove(PairState.WARN_REVIEW, to)) throw new IllegalStateException("NOT_A_WARNING");
            ContentValues v = new ContentValues();
            v.put("state", to.name());
            v.put("updated_at", System.currentTimeMillis());
            db.update(EditorialPairMigrationSpec.ITEMS, v, "run_id=? AND pair_id=?", new String[] {String.valueOf(runId), pairId});
            journal(db, runId, pairId, "WARNING_" + to + " " + note);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public List<PairReservation> openReservations(long runId) {
        List<PairReservation> out = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT call_id,pair_id,worst_usd FROM editorial_pair_reservations WHERE run_id=? AND state=?",
                new String[] {String.valueOf(runId), PairReservation.RESERVED})) {
            while (c.moveToNext()) {
                PairReservation r = new PairReservation();
                r.callId = c.getString(0);
                r.runId = runId;
                r.pairId = c.getString(1);
                r.worstUsd = decimal(c.getString(2));
                out.add(r);
            }
        }
        return out;
    }

    @Override public BigDecimal exposure(long runId) { return exposure(db(), runId); }

    private static BigDecimal exposure(SQLiteDatabase db, long runId) {
        BigDecimal sum = BigDecimal.ZERO;
        try (Cursor c = db.rawQuery("SELECT state,worst_usd,settled_usd FROM editorial_pair_reservations WHERE run_id=?", new String[] {String.valueOf(runId)})) {
            while (c.moveToNext()) sum = sum.add(PairReservation.RESERVED.equals(c.getString(0)) ? decimal(c.getString(1)) : decimal(c.getString(2)));
        }
        return sum;
    }

    @Override public List<String> journal(long runId) {
        List<String> out = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT pair_id,event FROM editorial_pair_journal WHERE run_id=? ORDER BY id", new String[] {String.valueOf(runId)})) {
            while (c.moveToNext()) out.add(runId + " " + c.getString(0) + " " + c.getString(1));
        }
        return out;
    }

    // ---- helpers

    private static String text(Cursor c, String column) {
        String s = c.getString(c.getColumnIndexOrThrow(column));
        return s == null ? "" : s;
    }

    private static BigDecimal decimal(String s) {
        try { return new BigDecimal(s == null || s.isEmpty() ? "0" : s); } catch (NumberFormatException broken) { return BigDecimal.ZERO; }
    }
}
