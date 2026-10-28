package com.ml.tblandroidtxt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opens (or creates) the desktop copy of tbl_android_txt.db. A new database is built from the v30 schema captured from the
 * phone (resource schema/tbl_android_txt_v30.sql, statements separated by "-- @@@"); an existing one must already be at
 * PRAGMA user_version=30, anything else is refused. When the phone schema changes this resource must be regenerated.
 */
public final class DesktopSchema {
    public static final int VERSION = 30;
    public static final String RESOURCE = "/schema/tbl_android_txt_v30.sql";
    private static final String SEPARATOR = "-- @@@";

    private DesktopSchema() {}

    /** Every statement of the schema file, in file order (comment header removed, sqlite_sequence kept: callers skip it). */
    public static List<String> statements() throws IOException {
        try (InputStream in = DesktopSchema.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IOException("missing schema resource " + RESOURCE);
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            ArrayList<String> out = new ArrayList<>();
            for (String part : text.split("\n" + SEPARATOR + "\n")) {
                String sql = part.trim();
                while (sql.startsWith("--")) {
                    int nl = sql.indexOf('\n');
                    sql = nl < 0 ? "" : sql.substring(nl + 1).trim();
                }
                if (!sql.isEmpty()) out.add(sql);
            }
            return out;
        }
    }

    /** True for the one statement SQLite refuses to run because the name is reserved (it appears by itself with AUTOINCREMENT). */
    public static boolean isReserved(String statement) {
        return statement.toLowerCase(Locale.ROOT).startsWith("create table sqlite_sequence");
    }

    /** Opens the database file, creating it from the schema when it does not exist yet; foreign keys are on. */
    public static JdbcSqlDb open(Path file) throws IOException, SQLException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try {
            int version;
            int objects;
            try (Statement st = connection.createStatement()) {
                try (ResultSet rs = st.executeQuery("PRAGMA user_version")) { version = rs.next() ? rs.getInt(1) : 0; }
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sqlite_master")) { objects = rs.next() ? rs.getInt(1) : 0; }
            }
            if (version == VERSION) return new JdbcSqlDb(connection);
            if (version != 0 || objects != 0) {
                throw new IllegalStateException("The database " + file.getFileName() + " is at schema version " + version
                        + " but this app supports " + VERSION + " only; it was not opened.");
            }
            JdbcSqlDb db = new JdbcSqlDb(connection);
            db.beginTransaction();
            try {
                for (String statement : statements()) if (!isReserved(statement)) db.execSQL(statement);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            try (Statement st = connection.createStatement()) { st.execute("PRAGMA user_version=" + VERSION); }
            return db;
        } catch (IOException | SQLException | RuntimeException e) {
            try { connection.close(); } catch (SQLException ignored) { }
            throw e;
        }
    }

    /** The connection behind a JdbcSqlDb is owned by the caller; this closes it. */
    public static void close(Connection connection) {
        try { connection.close(); } catch (SQLException ignored) { }
    }
}
