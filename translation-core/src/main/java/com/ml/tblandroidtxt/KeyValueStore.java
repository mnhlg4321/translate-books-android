package com.ml.tblandroidtxt;

import java.util.Map;

/** Key-value port mirroring the SharedPreferences calls the library stores use. */
public interface KeyValueStore {
    String getString(String key, String defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    Map<String, ?> getAll();

    Editor edit();

    interface Editor {
        Editor putString(String key, String value);

        Editor putBoolean(String key, boolean value);

        Editor putLong(String key, long value);

        Editor remove(String key);

        /** Synchronous write; false when the store could not persist. */
        boolean commit();

        void apply();
    }
}
