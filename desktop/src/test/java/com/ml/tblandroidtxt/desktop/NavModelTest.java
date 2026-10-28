package com.ml.tblandroidtxt.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.ml.tblandroidtxt.MemoryKeyValueStore;

import org.junit.Test;

public class NavModelTest {
    @Test public void titlesAreThePhoneTitles() {
        assertEquals("Dịch", NavModel.title(NavModel.TRANSLATE));
        assertEquals("Công việc", NavModel.title(NavModel.JOBS));
        assertEquals("Thư viện · RAW", NavModel.title(NavModel.RAW));
        assertEquals("Thư viện · Glossary", NavModel.title(NavModel.GLOSSARY));
        assertEquals("Thư viện · Pronoun", NavModel.title(NavModel.PRONOUN));
        assertEquals("Thư viện · Tập", NavModel.title(NavModel.SETS));
        assertEquals("Cài đặt", NavModel.title(NavModel.SETTINGS));
        assertEquals("Cài đặt · Giao diện", NavModel.title(NavModel.APPEARANCE));
    }

    @Test public void theAppReopensOnTheLastPageAndFallsBackToDich() {
        MemoryKeyValueStore prefs = new MemoryKeyValueStore();
        assertEquals(NavModel.TRANSLATE, NavModel.restore(prefs));
        NavModel.remember(prefs, NavModel.PRONOUN);
        assertEquals(NavModel.PRONOUN, NavModel.restore(prefs));
        prefs.edit().putString("nav_last", "gone").commit();
        assertEquals(NavModel.TRANSLATE, NavModel.restore(prefs));
        NavModel.remember(prefs, "not-a-page");
        assertEquals("an unknown page is not stored", NavModel.TRANSLATE, NavModel.restore(prefs));
    }

    @Test public void ctrlDigitsReachFivePagesAndLibraryUsesTheLastSubPage() {
        MemoryKeyValueStore prefs = new MemoryKeyValueStore();
        assertEquals(NavModel.TRANSLATE, NavModel.shortcutTarget(1, prefs));
        assertEquals(NavModel.VOLUME, NavModel.shortcutTarget(2, prefs));
        assertEquals(NavModel.JOBS, NavModel.shortcutTarget(3, prefs));
        assertEquals(NavModel.RAW, NavModel.shortcutTarget(4, prefs));
        assertEquals(NavModel.SETTINGS, NavModel.shortcutTarget(5, prefs));
        assertNull(NavModel.shortcutTarget(6, prefs));
        assertNull(NavModel.shortcutTarget(0, prefs));
        NavModel.remember(prefs, NavModel.SETS);
        NavModel.remember(prefs, NavModel.JOBS);
        assertEquals("the library remembers its own last page", NavModel.SETS, NavModel.shortcutTarget(4, prefs));
    }
}
