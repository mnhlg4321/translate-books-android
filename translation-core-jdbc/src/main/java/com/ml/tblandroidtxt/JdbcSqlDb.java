package com.ml.tblandroidtxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SqlDb over sqlite-jdbc (tests and the desktop app). It reproduces the Android behaviours the storage code relies on: string-bound
 * selection arguments, nested transactions where only the outermost commits, insert returning -1, insertOrThrow
 * throwing, and NULL columns reading as 0.
 */
public final class JdbcSqlDb implements SqlDb {
    /** Unchecked wrapper: the Android API throws unchecked SQLiteException. */
    public static final class SqlFailure extends RuntimeException {
        SqlFailure(SQLException cause) { super(cause.getMessage(), cause); }
    }

    private final Connection connection;
    private final ArrayList<boolean[]> transactions = new ArrayList<>();
    private boolean innerFailed;

    public JdbcSqlDb(Connection connection) {
        this.connection = connection;
        setForeignKeyConstraintsEnabled(true);
    }

    public void close() {
        try { connection.close(); } catch (SQLException ignored) { }
    }

    @Override public DbCursor rawQuery(String sql, String[] selectionArgs) {
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            if (selectionArgs != null) {
                for (int i = 0; i < selectionArgs.length; i++) st.setString(i + 1, selectionArgs[i]);
            }
            try (ResultSet rs = st.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columns = meta.getColumnCount();
                ArrayList<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[columns];
                    for (int i = 0; i < columns; i++) row[i] = rs.getObject(i + 1);
                    rows.add(row);
                }
                return new Rows(rows, columns);
            }
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    @Override public void execSQL(String sql) {
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    @Override public void execSQL(String sql, Object[] bindArgs) {
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            if (bindArgs != null) {
                for (int i = 0; i < bindArgs.length; i++) bind(st, i + 1, bindArgs[i]);
            }
            st.execute();
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    @Override public long insert(String table, String nullColumnHack, DbValues values) {
        try {
            return insertImpl(table, values, CONFLICT_NONE);
        } catch (SqlFailure e) {
            return -1;
        }
    }

    @Override public long insertOrThrow(String table, String nullColumnHack, DbValues values) {
        return insertImpl(table, values, CONFLICT_NONE);
    }

    @Override public long insertWithOnConflict(String table, String nullColumnHack, DbValues values, int conflictAlgorithm) {
        try {
            return insertImpl(table, values, conflictAlgorithm);
        } catch (SqlFailure e) {
            return -1;
        }
    }

    private long insertImpl(String table, DbValues values, int conflict) {
        StringBuilder sql = new StringBuilder("INSERT");
        sql.append(conflictClause(conflict)).append(" INTO ").append(table).append(" (");
        StringBuilder marks = new StringBuilder();
        List<Object> args = new ArrayList<>();
        for (Map.Entry<String, Object> e : values.entries().entrySet()) {
            if (!args.isEmpty()) { sql.append(','); marks.append(','); }
            sql.append(e.getKey());
            marks.append('?');
            args.add(e.getValue());
        }
        sql.append(") VALUES (").append(marks).append(')');
        try (PreparedStatement st = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) bind(st, i + 1, args.get(i));
            int changed = st.executeUpdate();
            if (changed <= 0) return -1;
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getLong(1) : -1;
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    private static String conflictClause(int conflict) {
        switch (conflict) {
            case CONFLICT_ROLLBACK: return " OR ROLLBACK";
            case CONFLICT_ABORT: return " OR ABORT";
            case CONFLICT_FAIL: return " OR FAIL";
            case CONFLICT_IGNORE: return " OR IGNORE";
            case CONFLICT_REPLACE: return " OR REPLACE";
            default: return "";
        }
    }

    @Override public int update(String table, DbValues values, String whereClause, String[] whereArgs) {
        if (values == null || values.size() == 0) throw new IllegalArgumentException("Empty values");
        StringBuilder sql = new StringBuilder("UPDATE ").append(table).append(" SET ");
        List<Object> args = new ArrayList<>();
        for (Map.Entry<String, Object> e : values.entries().entrySet()) {
            if (!args.isEmpty()) sql.append(',');
            sql.append(e.getKey()).append("=?");
            args.add(e.getValue());
        }
        if (whereClause != null && !whereClause.isEmpty()) sql.append(" WHERE ").append(whereClause);
        if (whereArgs != null) for (String a : whereArgs) args.add(a);
        return executeUpdate(sql.toString(), args);
    }

    @Override public int delete(String table, String whereClause, String[] whereArgs) {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(table);
        if (whereClause != null && !whereClause.isEmpty()) sql.append(" WHERE ").append(whereClause);
        List<Object> args = new ArrayList<>();
        if (whereArgs != null) for (String a : whereArgs) args.add(a);
        return executeUpdate(sql.toString(), args);
    }

    private int executeUpdate(String sql, List<Object> args) {
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.size(); i++) bind(st, i + 1, args.get(i));
            return st.executeUpdate();
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    /** Same binding rules as SQLiteProgram.bindAllArgsAsStrings/bindObject: integers as long, reals as double, Boolean as 1/0. */
    private static void bind(PreparedStatement st, int index, Object value) throws SQLException {
        if (value == null) st.setNull(index, java.sql.Types.NULL);
        else if (value instanceof Boolean) st.setLong(index, ((Boolean) value) ? 1L : 0L);
        else if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) st.setLong(index, ((Number) value).longValue());
        else if (value instanceof Float || value instanceof Double) st.setDouble(index, ((Number) value).doubleValue());
        else if (value instanceof byte[]) st.setBytes(index, (byte[]) value);
        else st.setString(index, value.toString());
    }

    @Override public void beginTransaction() {
        try {
            if (transactions.isEmpty()) {
                connection.setAutoCommit(false);
                innerFailed = false;
            }
            transactions.add(new boolean[]{false});
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    @Override public void setTransactionSuccessful() {
        if (transactions.isEmpty()) throw new IllegalStateException("no transaction is active");
        transactions.get(transactions.size() - 1)[0] = true;
    }

    @Override public void endTransaction() {
        if (transactions.isEmpty()) throw new IllegalStateException("no transaction is active");
        boolean successful = transactions.remove(transactions.size() - 1)[0];
        if (!successful) innerFailed = true;
        if (!transactions.isEmpty()) return;
        try {
            if (successful && !innerFailed) connection.commit();
            else connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    @Override public void setForeignKeyConstraintsEnabled(boolean enable) {
        execSQL("PRAGMA foreign_keys=" + (enable ? "ON" : "OFF"));
    }

    /** Fully read rows, so statements never outlive the call. */
    private static final class Rows implements DbCursor {
        private final List<Object[]> rows;
        private final int columns;
        private int position = -1;

        Rows(List<Object[]> rows, int columns) { this.rows = rows; this.columns = columns; }

        @Override public boolean moveToNext() { return ++position < rows.size(); }
        @Override public boolean moveToFirst() { position = 0; return !rows.isEmpty(); }
        @Override public String getString(int i) {
            Object v = cell(i);
            if (v == null) return null;
            if (v instanceof byte[]) return new String((byte[]) v, java.nio.charset.StandardCharsets.UTF_8);
            if (v instanceof Double) return String.valueOf(((Double) v).doubleValue());
            return v.toString();
        }
        @Override public int getInt(int i) { return (int) getLong(i); }
        @Override public long getLong(int i) {
            Object v = cell(i);
            if (v == null) return 0L;
            if (v instanceof Number) return ((Number) v).longValue();
            try { return Long.parseLong(v.toString().trim()); }
            catch (NumberFormatException e) { return (long) getDouble(i); }
        }
        @Override public double getDouble(int i) {
            Object v = cell(i);
            if (v == null) return 0d;
            if (v instanceof Number) return ((Number) v).doubleValue();
            try { return Double.parseDouble(v.toString().trim()); }
            catch (NumberFormatException e) { return 0d; }
        }
        @Override public boolean isNull(int i) { return cell(i) == null; }
        @Override public int getColumnCount() { return columns; }
        @Override public void close() {}

        private Object cell(int i) {
            if (position < 0 || position >= rows.size()) throw new IllegalStateException("cursor is not on a row");
            return rows.get(position)[i];
        }
    }
}
