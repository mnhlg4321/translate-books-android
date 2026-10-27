package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MemoryKeyValueStoreTest {
    @Test public void defaultsAndTypes() {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        assertEquals("d", store.getString("a", "d"));
        assertFalse(store.getBoolean("b", false));
        assertTrue(store.edit().putString("a", "x").putBoolean("b", true).putLong("n", 5L).commit());
        assertEquals("x", store.getString("a", "d"));
        assertTrue(store.getBoolean("b", false));
        assertEquals(5L, store.getAll().get("n"));
        assertEquals("d", store.getString("b", "d"));
    }

    @Test public void editsApplyOnCommitAndRemoveWorks() {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        KeyValueStore.Editor pending = store.edit().putString("a", "1");
        assertEquals("not visible before commit", "d", store.getString("a", "d"));
        pending.apply();
        assertEquals("1", store.getString("a", "d"));
        store.edit().remove("a").commit();
        assertEquals("d", store.getString("a", "d"));
        store.edit().putString("k", "v").commit();
        store.edit().putString("k", null).commit();
        assertEquals("a null value removes the key, as SharedPreferences does", "d", store.getString("k", "d"));
    }

    @Test public void getAllIsACopy() {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        store.edit().putString("a", "1").commit();
        java.util.Map<String, ?> snapshot = store.getAll();
        store.edit().putString("b", "2").commit();
        assertEquals(1, snapshot.size());
    }
}
