package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONObject;

/** One-time, transactional removal of legacy API keys from durable runtime snapshots. */
public final class PersistedSettingsCleanup {
    private static final String PREF = "persisted-settings-cleanup";
    private static final String DONE = "v1_done";
    private static final Object LOCK = new Object();

    public static final class TableResult {
        public int cleaned;
        public int skipped;
    }

    public static final class Result {
        public boolean alreadyDone;
        public final TableResult jobs = new TableResult();
        public final TableResult configRevisions = new TableResult();
        public final TableResult preparedBatches = new TableResult();
    }

    private PersistedSettingsCleanup() {}

    public static Result runOnce(Context context) {
        if (context == null) throw new IllegalArgumentException("Context is required");
        synchronized (LOCK) {
            SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            if (prefs.getBoolean(DONE, false)) {
                Result result = new Result();
                result.alreadyDone = true;
                return result;
            }
            TranslationRepository repo = new TranslationRepository(context.getApplicationContext());
            SQLiteDatabase db = repo.getWritableDatabase();
            Result result = new Result();
            boolean committed = false;
            db.beginTransaction();
            try {
                clean(db, "jobs", "id", "settings", "id", result.jobs);
                clean(db, "config_revisions", "job_id,revision", "settings_json", "job_id=? AND revision=?", result.configRevisions);
                clean(db, "prepared_batches", "id", "settings_json", "id", result.preparedBatches);
                db.setTransactionSuccessful();
                committed = true;
            } finally {
                db.endTransaction();
                repo.close();
            }
            // The completion flag is written only after the SQLite transaction has committed.
            if (!committed || !prefs.edit().putBoolean(DONE, true).commit()) {
                throw new IllegalStateException("Persisted settings cleanup completion could not be recorded");
            }
            Log.i("TranslateBooks", "settings_cleanup jobs_cleaned=" + result.jobs.cleaned
                    + " jobs_skipped=" + result.jobs.skipped
                    + " config_revisions_cleaned=" + result.configRevisions.cleaned
                    + " config_revisions_skipped=" + result.configRevisions.skipped
                    + " prepared_batches_cleaned=" + result.preparedBatches.cleaned
                    + " prepared_batches_skipped=" + result.preparedBatches.skipped);
            return result;
        }
    }

    private static void clean(SQLiteDatabase db, String table, String keyColumns, String jsonColumn,
                              String where, TableResult result) {
        String[] projection = keyColumns.split(",");
        String[] columns = new String[projection.length + 1];
        System.arraycopy(projection, 0, columns, 0, projection.length);
        columns[projection.length] = jsonColumn;
        try (Cursor cursor = db.query(table, columns, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                String raw = cursor.getString(projection.length);
                try {
                    JSONObject parsed = new JSONObject(raw == null ? "" : raw);
                    if (!parsed.has("apiKey")) continue;
                    String cleaned = SettingsStore.stripPersistedSecrets(parsed.toString());
                    ContentValues values = new ContentValues();
                    values.put(jsonColumn, cleaned);
                    String selection;
                    String[] args = new String[projection.length];
                    if (projection.length == 1) {
                        selection = where + "=?";
                        args[0] = cursor.getString(0);
                    } else {
                        selection = "job_id=? AND revision=?";
                        for (int i = 0; i < projection.length; i++) args[i] = cursor.getString(i);
                    }
                    db.update(table, values, selection, args);
                    result.cleaned++;
                } catch (Exception invalidJson) {
                    result.skipped++;
                }
            }
        }
    }
}
