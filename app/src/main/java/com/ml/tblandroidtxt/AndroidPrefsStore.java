package com.ml.tblandroidtxt;

import android.content.SharedPreferences;

import java.util.Map;

/** KeyValueStore over SharedPreferences. */
final class AndroidPrefsStore implements KeyValueStore {
    private final SharedPreferences prefs;

    AndroidPrefsStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    @Override public String getString(String key, String defaultValue) { return prefs.getString(key, defaultValue); }

    @Override public boolean getBoolean(String key, boolean defaultValue) { return prefs.getBoolean(key, defaultValue); }

    @Override public Map<String, ?> getAll() { return prefs.getAll(); }

    @Override public Editor edit() { return new AndroidEditor(prefs.edit()); }

    private static final class AndroidEditor implements Editor {
        private final SharedPreferences.Editor editor;

        AndroidEditor(SharedPreferences.Editor editor) { this.editor = editor; }

        @Override public Editor putString(String key, String value) { editor.putString(key, value); return this; }
        @Override public Editor putBoolean(String key, boolean value) { editor.putBoolean(key, value); return this; }
        @Override public Editor putLong(String key, long value) { editor.putLong(key, value); return this; }
        @Override public Editor remove(String key) { editor.remove(key); return this; }
        @Override public boolean commit() { return editor.commit(); }
        @Override public void apply() { editor.apply(); }
    }
}
