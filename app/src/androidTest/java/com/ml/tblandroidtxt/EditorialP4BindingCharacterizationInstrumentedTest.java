package com.ml.tblandroidtxt;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** P4 characterization of the legacy project owner and v18 storage boundary. */
@RunWith(AndroidJUnit4.class)
public final class EditorialP4BindingCharacterizationInstrumentedTest {
    private Context context;
    private String databaseName;
    private TranslationRepository database;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-p4-characterization-" + UUID.randomUUID() + ".db";
        database = new TranslationRepository(context, databaseName);
    }

    @After public void tearDown() {
        if (database != null) database.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void legacyRepositoryDoesNotAcceptImportedPackWithoutP4Service() {
        EditorialRepository.Project project = new EditorialRepository.Project();
        project.seriesName = "P4 characterization " + UUID.randomUUID();
        project.volumeName = "Imported 4.1.3";
        project.workflowVersion = "V5-SAFE.4.1.3-FULL";
        project.workflowHash = "497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d";

        EditorialRepository repository = new EditorialRepository(database);
        try {
            repository.createProject(project);
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("immutable V5-SAFE.4"));
            return;
        }
        throw new AssertionError("legacy repository must not silently accept an imported P4 pack");
    }

    @Test public void v18ScopeEntryStoreMustRetainP4SourceIdentityFacts() {
        SQLiteDatabase db = database.editorialReadableDatabase();
        assertEquals(24, db.getVersion());
        assertEquals(1, scalarInt(db,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='editorial_p4_bindings'"));
        assertEquals(1, scalarInt(db,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='editorial_p4_binding_inputs'"));
    }

    private static int scalarInt(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }
}
