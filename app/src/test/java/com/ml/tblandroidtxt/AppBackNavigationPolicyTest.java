package com.ml.tblandroidtxt;

import org.junit.Test;

import static com.ml.tblandroidtxt.AppBackNavigationPolicy.Action.CLOSE_PRONOUN;
import static com.ml.tblandroidtxt.AppBackNavigationPolicy.Action.OPEN_LIBRARY_HOME;
import static com.ml.tblandroidtxt.AppBackNavigationPolicy.Action.OPEN_SETTINGS_GENERAL;
import static com.ml.tblandroidtxt.AppBackNavigationPolicy.Action.OPEN_TRANSLATE;
import static com.ml.tblandroidtxt.AppBackNavigationPolicy.Action.SAVE_AND_CLOSE_GLOSSARY;
import static com.ml.tblandroidtxt.AppBackNavigationPolicy.Action.SYSTEM;
import static org.junit.Assert.assertEquals;

public class AppBackNavigationPolicyTest {
    @Test public void editorsReturnToTheirLists() {
        assertEquals(SAVE_AND_CLOSE_GLOSSARY, AppBackNavigationPolicy.resolve("Glossaries", true, false, "General"));
        assertEquals(CLOSE_PRONOUN, AppBackNavigationPolicy.resolve("Pronouns", false, true, "General"));
    }

    @Test public void libraryBackWalksToLibraryHomeThenTranslate() {
        assertEquals(OPEN_LIBRARY_HOME, AppBackNavigationPolicy.resolve("Glossaries", false, false, "General"));
        assertEquals(OPEN_LIBRARY_HOME, AppBackNavigationPolicy.resolve("Pronouns", false, false, "General"));
        assertEquals(OPEN_LIBRARY_HOME, AppBackNavigationPolicy.resolve("Sample", false, false, "General"));
        assertEquals(OPEN_TRANSLATE, AppBackNavigationPolicy.resolve("Files", false, false, "General"));
    }

    @Test public void settingsBackWalksToGeneralThenTranslate() {
        assertEquals(OPEN_SETTINGS_GENERAL, AppBackNavigationPolicy.resolve("Settings", false, false, "Provider"));
        assertEquals(OPEN_TRANSLATE, AppBackNavigationPolicy.resolve("Settings", false, false, "General"));
    }

    @Test public void unrelatedTabsRetainSystemBack() {
        assertEquals(SYSTEM, AppBackNavigationPolicy.resolve("Translate", false, false, "General"));
        assertEquals(SYSTEM, AppBackNavigationPolicy.resolve("Jobs", false, false, "General"));
    }
}
