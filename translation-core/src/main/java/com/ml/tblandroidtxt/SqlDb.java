package com.ml.tblandroidtxt;

/**
 * Database port for the translation storage code. It mirrors the part of android.database.sqlite.SQLiteDatabase
 * the app uses, with the same argument order and conflict constants, so moved code keeps its text.
 */
public interface SqlDb {
    int CONFLICT_NONE = 0;
    int CONFLICT_ROLLBACK = 1;
    int CONFLICT_ABORT = 2;
    int CONFLICT_FAIL = 3;
    int CONFLICT_IGNORE = 4;
    int CONFLICT_REPLACE = 5;

    DbCursor rawQuery(String sql, String[] selectionArgs);

    void execSQL(String sql);

    void execSQL(String sql, Object[] bindArgs);

    /** nullColumnHack is accepted for signature parity and is always null in this code base. */
    long insert(String table, String nullColumnHack, DbValues values);

    long insertOrThrow(String table, String nullColumnHack, DbValues values);

    long insertWithOnConflict(String table, String nullColumnHack, DbValues values, int conflictAlgorithm);

    int update(String table, DbValues values, String whereClause, String[] whereArgs);

    int delete(String table, String whereClause, String[] whereArgs);

    void beginTransaction();

    void setTransactionSuccessful();

    void endTransaction();

    void setForeignKeyConstraintsEnabled(boolean enable);
}
