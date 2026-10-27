package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SchemaFixtureTest {
    @Test public void fixtureIsTheV30SchemaAndLoadsIntoSqlite() throws Exception {
        assertEquals("52 tables + 52 indexes + 38 triggers/views", 142, TestDb.schemaStatements().size());
        TestDb t = TestDb.openWithSchema();
        try (DbCursor c = t.db.rawQuery("SELECT type,COUNT(*) FROM sqlite_master WHERE sql IS NOT NULL GROUP BY type ORDER BY type", null)) {
            java.util.HashMap<String, Integer> counts = new java.util.HashMap<>();
            while (c.moveToNext()) counts.put(c.getString(0), c.getInt(1));
            // sqlite_sequence is auto-created by AUTOINCREMENT tables and is not part of the fixture statements
            assertEquals(52, (int) counts.get("table"));
            assertEquals("explicit indexes only: automatic ones have no SQL", 52, (int) counts.get("index"));
            assertEquals(38, (int) counts.get("trigger"));
        }
        for (String table : new String[]{"jobs", "chunks", "config_revisions", "chunk_attempts", "chunk_metrics",
                "chapter_sets", "chapter_set_rows", "library_raw"}) {
            try (DbCursor c = t.db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
                assertTrue(table, c.moveToFirst());
                assertEquals(table, 0, c.getInt(0));
            }
        }
        t.close();
    }
}
