package com.ml.tblandroidtxt;

/** Read-only row cursor, a subset of android.database.Cursor. Positions start before the first row. */
public interface DbCursor extends AutoCloseable {
    boolean moveToNext();

    boolean moveToFirst();

    String getString(int columnIndex);

    int getInt(int columnIndex);

    long getLong(int columnIndex);

    double getDouble(int columnIndex);

    boolean isNull(int columnIndex);

    int getColumnCount();

    @Override void close();
}
