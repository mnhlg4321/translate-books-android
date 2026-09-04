package com.ml.tblandroidtxt;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

/**
 * P5C integration characterization. The first production E2E slice needs a
 * durable attempt owner; this test intentionally proves whether the current
 * P4-only schema already provides one before any production change.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5CExactBindingFakeE2EInstrumentedTest {
    private Context context;
    private String databaseName;
    private TranslationRepository database;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "p5c-characterization-" + UUID.randomUUID() + ".db";
        database = new TranslationRepository(context, databaseName);
    }

    @After public void tearDown() {
        if (database != null) database.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void currentP4SchemaMustExposeDurableP5CAttemptOwner() {
        assertTrue("P5C requires a durable attempt owner before exact-binding E2E",
                tableExists("editorial_p5c_attempts"));
    }

    private boolean tableExists(String table) {
        SQLiteDatabase db = database.editorialReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{table})) {
            return cursor.moveToFirst();
        }
    }
}
