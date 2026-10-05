package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** SQLite implementation over the two tables of database version 26. */
public final class SqliteEditorialApiStore implements EditorialApiStore, AutoCloseable {
    private final TranslationRepository database;

    public SqliteEditorialApiStore(Context context) { this(new TranslationRepository(context.getApplicationContext())); }

    SqliteEditorialApiStore(TranslationRepository database) { this.database = database; }

    @Override public void close() { database.close(); }

    private SQLiteDatabase db() { return database.getWritableDatabase(); }

    /** For instrumented tests that inspect the schema. */
    SQLiteDatabase databaseForTest() { return db(); }

    // ---- combos ----

    @Override public long insertCombo(EditorialApiCombo combo) {
        long now = System.currentTimeMillis();
        combo.createdAt = now;
        combo.updatedAt = now;
        long id = db().insertOrThrow(EditorialApiMigrationSpec.COMBOS, null, comboValues(combo));
        combo.id = id;
        return id;
    }

    @Override public void updateCombo(EditorialApiCombo combo) {
        combo.updatedAt = System.currentTimeMillis();
        db().update(EditorialApiMigrationSpec.COMBOS, comboValues(combo), "id=?", new String[] {String.valueOf(combo.id)});
    }

    @Override public void deleteCombo(long id) {
        db().delete(EditorialApiMigrationSpec.COMBOS, "id=?", new String[] {String.valueOf(id)});
    }

    @Override public List<EditorialApiCombo> listCombos() {
        List<EditorialApiCombo> out = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT * FROM editorial_combos ORDER BY updated_at DESC, id DESC", null)) {
            while (c.moveToNext()) out.add(combo(c));
        }
        return out;
    }

    @Override public EditorialApiCombo getCombo(long id) {
        try (Cursor c = db().rawQuery("SELECT * FROM editorial_combos WHERE id=?", new String[] {String.valueOf(id)})) {
            return c.moveToFirst() ? combo(c) : null;
        }
    }

    private static ContentValues comboValues(EditorialApiCombo combo) {
        ContentValues v = new ContentValues();
        v.put("name", combo.name);
        v.put("raw_uri", combo.rawUri);
        v.put("raw_name", combo.rawName);
        v.put("draft_uri", combo.draftUri);
        v.put("draft_name", combo.draftName);
        v.put("glossary_id", combo.glossaryId.isEmpty() ? null : combo.glossaryId);
        v.put("pronoun_id", combo.pronounId.isEmpty() ? null : combo.pronounId);
        v.put("settings_json", combo.settingsJson);
        v.put("created_at", combo.createdAt);
        v.put("updated_at", combo.updatedAt);
        return v;
    }

    private static EditorialApiCombo combo(Cursor c) {
        EditorialApiCombo combo = new EditorialApiCombo();
        combo.id = c.getLong(c.getColumnIndexOrThrow("id"));
        combo.name = text(c, "name");
        combo.rawUri = text(c, "raw_uri");
        combo.rawName = text(c, "raw_name");
        combo.draftUri = text(c, "draft_uri");
        combo.draftName = text(c, "draft_name");
        combo.glossaryId = text(c, "glossary_id");
        combo.pronounId = text(c, "pronoun_id");
        combo.settingsJson = text(c, "settings_json");
        combo.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        combo.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        return combo;
    }

    // ---- runs ----

    @Override public long insertRun(EditorialApiRun run) {
        long now = System.currentTimeMillis();
        run.createdAt = now;
        run.updatedAt = now;
        long id = db().insertOrThrow(EditorialApiMigrationSpec.RUNS, null, runValues(run));
        run.id = id;
        return id;
    }

    @Override public void updateRun(EditorialApiRun run) {
        run.updatedAt = System.currentTimeMillis();
        db().update(EditorialApiMigrationSpec.RUNS, runValues(run), "id=?", new String[] {String.valueOf(run.id)});
    }

    @Override public EditorialApiRun getRun(long id) {
        try (Cursor c = db().rawQuery("SELECT * FROM editorial_api_runs WHERE id=?", new String[] {String.valueOf(id)})) {
            return c.moveToFirst() ? run(c) : null;
        }
    }

    @Override public EditorialApiRun latestRun(long comboId) {
        try (Cursor c = db().rawQuery("SELECT * FROM editorial_api_runs WHERE combo_id=? ORDER BY created_at DESC, id DESC LIMIT 1",
                new String[] {String.valueOf(comboId)})) {
            return c.moveToFirst() ? run(c) : null;
        }
    }

    @Override public List<EditorialApiRun> runsOf(long comboId) {
        List<EditorialApiRun> out = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT * FROM editorial_api_runs WHERE combo_id=? ORDER BY created_at DESC, id DESC",
                new String[] {String.valueOf(comboId)})) {
            while (c.moveToNext()) out.add(run(c));
        }
        return out;
    }

    private static ContentValues runValues(EditorialApiRun r) {
        ContentValues v = new ContentValues();
        v.put("combo_id", r.comboId);
        v.put("contract_revision", r.contractRevision);
        v.put("quality_core_sha256", r.qualityCoreSha256);
        v.put("model", r.model);
        v.put("mode", r.mode.name());
        v.put("state", r.state.name());
        v.put("raw_sha256", r.rawSha256);
        v.put("raw_text", r.rawText);
        v.put("draft_sha256", r.draftSha256);
        v.put("draft_text", r.draftText);
        v.put("glossary_sha256", r.glossarySha256);
        v.put("glossary_text", r.glossaryText);
        v.put("pronoun_sha256", r.pronounSha256);
        v.put("pronoun_text", r.pronounText);
        v.put("edited_text", r.editedText);
        v.put("final_text", r.finalText);
        v.put("notes_json", r.notesJson);
        v.put("issues_json", r.issuesJson);
        v.put("guards_json", r.guardsJson);
        v.put("steps_json", r.stepsJson);
        v.put("wrong_pair_evidence", r.wrongPairEvidence);
        v.put("calls", r.calls);
        v.put("input_tokens", r.inputTokens);
        v.put("output_tokens", r.outputTokens);
        v.put("usd", r.usd.toPlainString());
        v.put("cost_known", r.costKnown ? 1 : 0);
        v.put("glossary_entries", r.glossaryEntries);
        v.put("pronoun_rows", r.pronounRows);
        v.put("error", r.error);
        v.put("created_at", r.createdAt);
        v.put("updated_at", r.updatedAt);
        return v;
    }

    private static EditorialApiRun run(Cursor c) {
        EditorialApiRun r = new EditorialApiRun();
        r.id = c.getLong(c.getColumnIndexOrThrow("id"));
        r.comboId = c.getLong(c.getColumnIndexOrThrow("combo_id"));
        r.contractRevision = text(c, "contract_revision");
        r.qualityCoreSha256 = text(c, "quality_core_sha256");
        r.model = text(c, "model");
        r.mode = EditorialApiContract.Mode.valueOf(text(c, "mode"));
        r.state = EditorialApiContract.RunState.valueOf(text(c, "state"));
        r.rawSha256 = text(c, "raw_sha256");
        r.rawText = text(c, "raw_text");
        r.draftSha256 = text(c, "draft_sha256");
        r.draftText = text(c, "draft_text");
        r.glossarySha256 = text(c, "glossary_sha256");
        r.glossaryText = text(c, "glossary_text");
        r.pronounSha256 = text(c, "pronoun_sha256");
        r.pronounText = text(c, "pronoun_text");
        r.editedText = text(c, "edited_text");
        r.finalText = text(c, "final_text");
        r.notesJson = text(c, "notes_json");
        r.issuesJson = text(c, "issues_json");
        r.guardsJson = text(c, "guards_json");
        r.stepsJson = text(c, "steps_json");
        r.wrongPairEvidence = text(c, "wrong_pair_evidence");
        r.calls = c.getInt(c.getColumnIndexOrThrow("calls"));
        r.inputTokens = c.getLong(c.getColumnIndexOrThrow("input_tokens"));
        r.outputTokens = c.getLong(c.getColumnIndexOrThrow("output_tokens"));
        try { r.usd = new BigDecimal(text(c, "usd")); } catch (NumberFormatException e) { r.usd = BigDecimal.ZERO; }
        r.costKnown = c.getInt(c.getColumnIndexOrThrow("cost_known")) != 0;
        r.glossaryEntries = c.getInt(c.getColumnIndexOrThrow("glossary_entries"));
        r.pronounRows = c.getInt(c.getColumnIndexOrThrow("pronoun_rows"));
        r.error = text(c, "error");
        r.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        r.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        return r;
    }

    private static String text(Cursor c, String column) {
        int index = c.getColumnIndexOrThrow(column);
        return c.isNull(index) ? "" : c.getString(index);
    }
}
