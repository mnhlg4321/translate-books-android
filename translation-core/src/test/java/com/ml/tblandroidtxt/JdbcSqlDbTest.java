package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;

/** Pins the Android-equivalent behaviours of the test SqlDb that the storage tests rely on. */
public class JdbcSqlDbTest {
    private Connection connection;
    private JdbcSqlDb db;

    @Before public void open() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new JdbcSqlDb(connection);
        db.execSQL("CREATE TABLE parent (id INTEGER PRIMARY KEY, name TEXT UNIQUE, n INTEGER, r REAL, flag INTEGER, b BLOB)");
        db.execSQL("CREATE TABLE child (id INTEGER PRIMARY KEY, parent_id INTEGER REFERENCES parent(id))");
    }

    @After public void close() throws Exception { connection.close(); }

    private static DbValues row(String name, Integer n) {
        DbValues v = new DbValues();
        v.put("name", name);
        v.put("n", n);
        return v;
    }

    private long count(String sql, String... args) {
        try (DbCursor c = db.rawQuery(sql, args)) { return c.moveToFirst() ? c.getLong(0) : -1; }
    }

    @Test public void valuesKeepTheirTypes() {
        DbValues v = new DbValues();
        v.put("name", "a");
        v.put("n", 7);
        v.put("r", 1.5d);
        v.put("flag", true);
        v.put("b", new byte[]{1, 2});
        long id = db.insertOrThrow("parent", null, v);
        try (DbCursor c = db.rawQuery("SELECT name,n,r,flag,typeof(n),typeof(r),typeof(flag),typeof(b) FROM parent WHERE id=?", new String[]{String.valueOf(id)})) {
            assertTrue(c.moveToFirst());
            assertEquals("a", c.getString(0));
            assertEquals(7, c.getInt(1));
            assertEquals(1.5d, c.getDouble(2), 0d);
            assertEquals(1, c.getInt(3));
            assertEquals("integer", c.getString(4));
            assertEquals("real", c.getString(5));
            assertEquals("integer", c.getString(6));
            assertEquals("blob", c.getString(7));
            assertEquals(8, c.getColumnCount());
        }
    }

    @Test public void nullColumnsReadAsZeroAndNull() {
        DbValues v = new DbValues();
        v.put("name", "a");
        v.putNull("n");
        db.insertOrThrow("parent", null, v);
        try (DbCursor c = db.rawQuery("SELECT n,name FROM parent", null)) {
            assertTrue(c.moveToNext());
            assertTrue(c.isNull(0));
            assertEquals(0, c.getInt(0));
            assertEquals(0L, c.getLong(0));
            assertNull(c.getString(0));
            assertFalse(c.moveToNext());
        }
    }

    @Test public void selectionArgsBindAsStringsAndNumericColumnsStillMatch() {
        db.insertOrThrow("parent", null, row("a", 5));
        assertEquals(1, count("SELECT COUNT(*) FROM parent WHERE n=?", "5"));
        assertEquals(1, db.update("parent", row("b", 6), "name=?", new String[]{"a"}));
        assertEquals(1, db.delete("parent", "n=?", new String[]{"6"}));
        assertEquals(0, count("SELECT COUNT(*) FROM parent"));
    }

    @Test public void insertReturnsMinusOneOnConflictButInsertOrThrowThrows() {
        db.insertOrThrow("parent", null, row("a", 1));
        assertEquals(-1L, db.insert("parent", null, row("a", 2)));
        try {
            db.insertOrThrow("parent", null, row("a", 3));
            fail("unique violation must throw");
        } catch (RuntimeException expected) {
            assertTrue(expected instanceof JdbcSqlDb.SqlFailure);
        }
        assertEquals(1, count("SELECT COUNT(*) FROM parent"));
    }

    @Test public void conflictAlgorithms() {
        long first = db.insertOrThrow("parent", null, row("a", 1));
        assertEquals(-1L, db.insertWithOnConflict("parent", null, row("a", 9), SqlDb.CONFLICT_IGNORE));
        assertEquals(1, count("SELECT n FROM parent WHERE name='a'"));
        long replaced = db.insertWithOnConflict("parent", null, row("a", 9), SqlDb.CONFLICT_REPLACE);
        assertTrue(replaced > 0 && replaced != first);
        assertEquals(9, count("SELECT n FROM parent WHERE name='a'"));
        assertEquals(1, count("SELECT COUNT(*) FROM parent"));
    }

    @Test public void foreignKeysAreEnforced() {
        DbValues v = new DbValues();
        v.put("parent_id", 99L);
        try {
            db.insertOrThrow("child", null, v);
            fail("foreign key violation must throw");
        } catch (RuntimeException expected) {
            // like SQLiteConstraintException on Android
        }
        db.setForeignKeyConstraintsEnabled(false);
        assertTrue(db.insertOrThrow("child", null, v) > 0);
    }

    @Test public void execSqlWithBindArgs() {
        db.execSQL("INSERT INTO parent (name,n,flag) VALUES (?,?,?)", new Object[]{"x", 3L, true});
        assertEquals(1, count("SELECT COUNT(*) FROM parent WHERE name='x' AND n=3 AND flag=1"));
    }

    @Test public void transactionCommitsOnlyWhenOutermostSucceeds() {
        db.beginTransaction();
        try {
            db.insertOrThrow("parent", null, row("a", 1));
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        assertEquals(1, count("SELECT COUNT(*) FROM parent"));

        db.beginTransaction();
        try {
            db.insertOrThrow("parent", null, row("b", 2));
        } finally { db.endTransaction(); }
        assertEquals("no setTransactionSuccessful rolls back", 1, count("SELECT COUNT(*) FROM parent"));
    }

    @Test public void nestedTransactions() {
        db.beginTransaction();
        try {
            db.beginTransaction();
            try {
                db.insertOrThrow("parent", null, row("a", 1));
                // inner not marked successful
            } finally { db.endTransaction(); }
            db.insertOrThrow("parent", null, row("b", 2));
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        assertEquals("an inner rollback poisons the outer transaction", 0, count("SELECT COUNT(*) FROM parent"));

        db.beginTransaction();
        try {
            db.beginTransaction();
            try {
                db.insertOrThrow("parent", null, row("c", 3));
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        assertEquals(1, count("SELECT COUNT(*) FROM parent"));
    }

    @Test public void failedStatementInsideTransactionRollsEverythingBack() {
        db.beginTransaction();
        try {
            db.insertOrThrow("parent", null, row("a", 1));
            db.insertOrThrow("parent", null, row("a", 2));
            db.setTransactionSuccessful();
            fail("second insert must throw before success is marked");
        } catch (RuntimeException expected) {
            // fall through to endTransaction
        } finally { db.endTransaction(); }
        assertEquals(0, count("SELECT COUNT(*) FROM parent"));
    }
}
