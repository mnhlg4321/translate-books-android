package com.ml.tblandroidtxt;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;

/** Opens an in-memory SQLite database with the v30 schema captured from the phone (structure only). */
final class TestDb {
    static final String SCHEMA_RESOURCE = "/schema/tbl_android_txt_v30.sql";
    static final String SEPARATOR = "-- @@@";

    final Connection connection;
    final JdbcSqlDb db;

    private TestDb(Connection connection, JdbcSqlDb db) {
        this.connection = connection;
        this.db = db;
    }

    static TestDb openWithSchema() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        JdbcSqlDb db = new JdbcSqlDb(connection);
        for (String statement : schemaStatements()) {
            String lower = statement.toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("create table sqlite_sequence")) continue; // created by SQLite itself
            db.execSQL(statement);
        }
        return new TestDb(connection, db);
    }

    static java.util.List<String> schemaStatements() throws Exception {
        try (InputStream in = TestDb.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) throw new IllegalStateException("missing schema fixture " + SCHEMA_RESOURCE);
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            java.util.ArrayList<String> out = new java.util.ArrayList<>();
            for (String part : text.split("\n" + SEPARATOR + "\n")) {
                String sql = part.trim();
                // the leading comment block belongs to the first statement's chunk
                while (sql.startsWith("--")) {
                    int nl = sql.indexOf('\n');
                    sql = nl < 0 ? "" : sql.substring(nl + 1).trim();
                }
                if (!sql.isEmpty()) out.add(sql);
            }
            return out;
        }
    }

    void close() throws Exception { connection.close(); }
}
