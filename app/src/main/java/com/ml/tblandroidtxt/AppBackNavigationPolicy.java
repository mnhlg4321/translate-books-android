package com.ml.tblandroidtxt;

final class AppBackNavigationPolicy {
    enum Action {
        SAVE_AND_CLOSE_GLOSSARY,
        CLOSE_PRONOUN,
        OPEN_LIBRARY_HOME,
        EXIT_SELECTION,
        OPEN_SETTINGS_GENERAL,
        OPEN_TRANSLATE,
        OPEN_ORIGIN,
        SYSTEM
    }

    static Action resolve(String tab, boolean editingGlossary, boolean editingPronoun, String settingsCategory) {
        return resolve(tab, editingGlossary, editingPronoun, settingsCategory, false);
    }

    /** {@code selecting}: the current Library list is in multi-select mode; Back leaves that mode first. */
    static Action resolve(String tab, boolean editingGlossary, boolean editingPronoun, String settingsCategory, boolean selecting) {
        if (selecting && isLibraryList(tab)) return Action.EXIT_SELECTION;
        if ("Glossaries".equals(tab) && editingGlossary) return Action.SAVE_AND_CLOSE_GLOSSARY;
        if ("Pronouns".equals(tab) && editingPronoun) return Action.CLOSE_PRONOUN;
        if ("Volume".equals(tab) || "AllSets".equals(tab)) return Action.OPEN_ORIGIN;
        if ("Glossaries".equals(tab) || "Pronouns".equals(tab) || "Sample".equals(tab) || "RAW".equals(tab) || "ChapterSets".equals(tab)) return Action.OPEN_LIBRARY_HOME;
        if ("Files".equals(tab)) return Action.OPEN_TRANSLATE;
        if ("Settings".equals(tab) && !"General".equals(settingsCategory)) return Action.OPEN_SETTINGS_GENERAL;
        if ("Settings".equals(tab)) return Action.OPEN_TRANSLATE;
        return Action.SYSTEM;
    }

    static boolean isLibraryList(String tab) {
        return "Glossaries".equals(tab) || "Pronouns".equals(tab) || "RAW".equals(tab) || "ChapterSets".equals(tab);
    }

    private AppBackNavigationPolicy() {}
}
