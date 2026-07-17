package com.ml.tblandroidtxt;

final class AppBackNavigationPolicy {
    enum Action {
        SAVE_AND_CLOSE_GLOSSARY,
        CLOSE_PRONOUN,
        OPEN_LIBRARY_HOME,
        OPEN_SETTINGS_GENERAL,
        OPEN_TRANSLATE,
        SYSTEM
    }

    static Action resolve(String tab, boolean editingGlossary, boolean editingPronoun, String settingsCategory) {
        if ("Glossaries".equals(tab) && editingGlossary) return Action.SAVE_AND_CLOSE_GLOSSARY;
        if ("Pronouns".equals(tab) && editingPronoun) return Action.CLOSE_PRONOUN;
        if ("Glossaries".equals(tab) || "Pronouns".equals(tab) || "Sample".equals(tab)) return Action.OPEN_LIBRARY_HOME;
        if ("Files".equals(tab)) return Action.OPEN_TRANSLATE;
        if ("Settings".equals(tab) && !"General".equals(settingsCategory)) return Action.OPEN_SETTINGS_GENERAL;
        if ("Settings".equals(tab)) return Action.OPEN_TRANSLATE;
        return Action.SYSTEM;
    }

    private AppBackNavigationPolicy() {}
}
