package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.TreeSet;

public class DesktopSchemaTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static TreeSet<String> namesInFile() throws Exception {
        TreeSet<String> names = new TreeSet<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("^CREATE\\s+(?:UNIQUE\\s+)?(TABLE|INDEX|TRIGGER|VIEW)\\s+(?:IF NOT EXISTS\\s+)?(\\S+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        for (String sql : DesktopSchema.statements()) {
            if (DesktopSchema.isReserved(sql)) continue;
            java.util.regex.Matcher m = p.matcher(sql);
            assertTrue(sql.substring(0, Math.min(60, sql.length())), m.find());
            String name = m.group(2);
            int paren = name.indexOf('(');
            names.add((paren > 0 ? name.substring(0, paren) : name).toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private static TreeSet<String> namesInDatabase(Path file) throws Exception {
        TreeSet<String> names = new TreeSet<>();
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE sql IS NOT NULL")) {
            while (rs.next()) {
                String n = rs.getString(1).toLowerCase(Locale.ROOT);
                if (!n.equals("sqlite_sequence")) names.add(n);
            }
        }
        return names;
    }

    private static int userVersion(Path file) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
             Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.getInt(1);
        }
    }

    @Test public void newDatabaseHasEveryObjectOfTheSchemaFileAndVersion30() throws Exception {
        Path file = tmp.getRoot().toPath().resolve("db").resolve("tbl_android_txt.db");
        JdbcSqlDb db = DesktopSchema.open(file);
        try (DbCursor c = db.rawQuery("PRAGMA foreign_keys", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("foreign keys are on", 1, c.getInt(0));
        }
        assertEquals(30, userVersion(file));
        TreeSet<String> expected = namesInFile();
        TreeSet<String> actual = namesInDatabase(file);
        assertEquals("names with SQL in sqlite_master equal the schema file (without sqlite_sequence)", expected, actual);
        assertTrue(expected.contains("jobs") && expected.contains("chunks") && expected.contains("library_raw"));
    }

    @Test public void existingVersion30DatabaseIsReopenedUntouched() throws Exception {
        Path file = tmp.getRoot().toPath().resolve("a.db");
        JdbcSqlDb first = DesktopSchema.open(file);
        DbValues v = new DbValues();
        v.put("id", "set-1");
        v.put("name", "n");
        v.put("created_at", 1L);
        v.put("updated_at", 2L);
        first.insertOrThrow("chapter_sets", null, v);
        JdbcSqlDb second = DesktopSchema.open(file);
        try (DbCursor c = second.rawQuery("SELECT COUNT(*) FROM chapter_sets", null)) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getInt(0));
        }
    }

    @Test public void otherVersionsAreRefused() throws Exception {
        Path file = tmp.getRoot().toPath().resolve("old.db");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath()); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE jobs (id INTEGER)");
            st.execute("PRAGMA user_version=29");
        }
        try {
            DesktopSchema.open(file);
            fail("a version 29 database must not be opened");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("29"));
        }
        assertEquals("the refused file is left as it was", 29, userVersion(file));
    }
}
