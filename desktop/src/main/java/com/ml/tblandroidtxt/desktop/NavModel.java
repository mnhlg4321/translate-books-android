package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.KeyValueStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The left-hand navigation as plain data (no JavaFX): pages, their titles, the Ctrl+1..5 targets and the page that is reopened.
 * Titles are copied from the phone's NavigationPageTitles (which cannot move to the core because it pulls in the Editorial tab);
 * "Dịch cả tập" sits under Dịch because it is the same job done for many chapters.
 */
final class NavModel {
    static final String TRANSLATE = "translate", VOLUME = "volume", JOBS = "jobs", RAW = "raw", GLOSSARY = "glossary",
            PRONOUN = "pronoun", SETS = "sets", SETTINGS = "settings", APPEARANCE = "appearance", LIBRARY = "library";

    static final class Entry {
        final String key;
        final String label;
        final String title;
        /** Group the entry sits under in the menu ("" for a top-level entry). */
        final String parent;

        Entry(String key, String label, String title, String parent) {
            this.key = key; this.label = label; this.title = title; this.parent = parent;
        }
    }

    private static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            new Entry(TRANSLATE, "Dịch", "Dịch", ""),
            new Entry(VOLUME, "Dịch cả tập", "Dịch · Cả tập", TRANSLATE),
            new Entry(JOBS, "Công việc", "Công việc", ""),
            new Entry(RAW, "RAW", "Thư viện · RAW", LIBRARY),
            new Entry(GLOSSARY, "Glossary", "Thư viện · Glossary", LIBRARY),
            new Entry(PRONOUN, "Pronoun", "Thư viện · Pronoun", LIBRARY),
            new Entry(SETS, "Tập", "Thư viện · Tập", LIBRARY),
            new Entry(SETTINGS, "Cài đặt", "Cài đặt", ""),
            new Entry(APPEARANCE, "Giao diện", "Cài đặt · Giao diện", SETTINGS)));

    private static final String LAST = "nav_last", LAST_LIBRARY = "nav_last_library";

    private NavModel() { }

    static List<Entry> entries() { return ENTRIES; }

    static Entry entry(String key) {
        for (Entry e : ENTRIES) if (e.key.equals(key)) return e;
        return null;
    }

    static boolean isPage(String key) { return entry(key) != null; }

    static String title(String key) {
        Entry e = entry(key);
        return e == null ? "Dịch" : e.title;
    }

    /** The menu button that stays highlighted for a page: itself, or the Thư viện heading for a library page. */
    static boolean inLibrary(String key) {
        Entry e = entry(key);
        return e != null && LIBRARY.equals(e.parent);
    }

    static List<String> libraryKeys() {
        List<String> out = new ArrayList<>();
        for (Entry e : ENTRIES) if (LIBRARY.equals(e.parent)) out.add(e.key);
        return out;
    }

    /** Ctrl+1..5: Dịch, Dịch cả tập, Công việc, Thư viện (the sub-page used last), Cài đặt. Null for any other number. */
    static String shortcutTarget(int number, KeyValueStore prefs) {
        switch (number) {
            case 1: return TRANSLATE;
            case 2: return VOLUME;
            case 3: return JOBS;
            case 4: return lastLibrary(prefs);
            case 5: return SETTINGS;
            default: return null;
        }
    }

    static String lastLibrary(KeyValueStore prefs) {
        String saved = prefs.getString(LAST_LIBRARY, RAW);
        return libraryKeys().contains(saved) ? saved : RAW;
    }

    static void remember(KeyValueStore prefs, String key) {
        if (!isPage(key)) return;
        KeyValueStore.Editor ed = prefs.edit().putString(LAST, key);
        if (inLibrary(key)) ed.putString(LAST_LIBRARY, key);
        ed.commit();
    }

    /** The page the app reopens on: the one used last, or Dịch when nothing valid is stored. */
    static String restore(KeyValueStore prefs) {
        String saved = prefs.getString(LAST, TRANSLATE);
        return isPage(saved) ? saved : TRANSLATE;
    }
}
