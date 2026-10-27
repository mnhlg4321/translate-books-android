package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Map;

/**
 * SqlDb over the app's SQLiteOpenHelper. The helper is asked for its database on every call and the SQLiteDatabase
 * is never kept, because after helper.close() a new instance is returned. SQLiteException passes through unchanged.
 */
final class AndroidSqlDb implements SqlDb {
    private final SQLiteOpenHelper helper;

    AndroidSqlDb(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    @Override public DbCursor rawQuery(String sql, String[] selectionArgs) {
        return new AndroidDbCursor(helper.getReadableDatabase().rawQuery(sql, selectionArgs));
    }

    @Override public void execSQL(String sql) { helper.getWritableDatabase().execSQL(sql); }

    @Override public void execSQL(String sql, Object[] bindArgs) { helper.getWritableDatabase().execSQL(sql, bindArgs); }

    @Override public long insert(String table, String nullColumnHack, DbValues values) {
        return helper.getWritableDatabase().insert(table, nullColumnHack, toContentValues(values));
    }

    @Override public long insertOrThrow(String table, String nullColumnHack, DbValues values) {
        return helper.getWritableDatabase().insertOrThrow(table, nullColumnHack, toContentValues(values));
    }

    @Override public long insertWithOnConflict(String table, String nullColumnHack, DbValues values, int conflictAlgorithm) {
        return helper.getWritableDatabase().insertWithOnConflict(table, nullColumnHack, toContentValues(values), conflictAlgorithm);
    }

    @Override public int update(String table, DbValues values, String whereClause, String[] whereArgs) {
        return helper.getWritableDatabase().update(table, toContentValues(values), whereClause, whereArgs);
    }

    @Override public int delete(String table, String whereClause, String[] whereArgs) {
        return helper.getWritableDatabase().delete(table, whereClause, whereArgs);
    }

    @Override public void beginTransaction() { helper.getWritableDatabase().beginTransaction(); }

    @Override public void setTransactionSuccessful() { helper.getWritableDatabase().setTransactionSuccessful(); }

    @Override public void endTransaction() { helper.getWritableDatabase().endTransaction(); }

    @Override public void setForeignKeyConstraintsEnabled(boolean enable) {
        helper.getWritableDatabase().setForeignKeyConstraintsEnabled(enable);
    }

    static ContentValues toContentValues(DbValues values) {
        ContentValues out = new ContentValues();
        for (Map.Entry<String, Object> e : values.entries().entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            if (v == null) out.putNull(key);
            else if (v instanceof String) out.put(key, (String) v);
            else if (v instanceof Integer) out.put(key, (Integer) v);
            else if (v instanceof Long) out.put(key, (Long) v);
            else if (v instanceof Double) out.put(key, (Double) v);
            else if (v instanceof Float) out.put(key, (Float) v);
            else if (v instanceof Boolean) out.put(key, (Boolean) v);
            else if (v instanceof byte[]) out.put(key, (byte[]) v);
            else throw new IllegalArgumentException("Unsupported value type for " + key);
        }
        return out;
    }

    private static final class AndroidDbCursor implements DbCursor {
        private final Cursor cursor;

        AndroidDbCursor(Cursor cursor) { this.cursor = cursor; }

        @Override public boolean moveToNext() { return cursor.moveToNext(); }
        @Override public boolean moveToFirst() { return cursor.moveToFirst(); }
        @Override public String getString(int i) { return cursor.getString(i); }
        @Override public int getInt(int i) { return cursor.getInt(i); }
        @Override public long getLong(int i) { return cursor.getLong(i); }
        @Override public double getDouble(int i) { return cursor.getDouble(i); }
        @Override public boolean isNull(int i) { return cursor.isNull(i); }
        @Override public int getColumnCount() { return cursor.getColumnCount(); }
        @Override public void close() { cursor.close(); }
    }
}
