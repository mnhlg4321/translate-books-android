package com.ml.tblandroidtxt.desktop;

/** The language profiles of the phone's "Chọn profile ngôn ngữ" list (the phone keeps them inline in its main screen). */
final class LanguageProfiles {
    static final String[] LABELS = {
            "Japanese → Vietnamese Light Novel",
            "English → Vietnamese",
            "Chinese → Vietnamese Web Novel",
            "Korean → Vietnamese",
            "Vietnamese → English",
            "Tùy chỉnh"
    };

    private static final String[][] PAIRS = {
            {"Japanese", "Vietnamese"}, {"English", "Vietnamese"}, {"Chinese", "Vietnamese"}, {"Korean", "Vietnamese"}, {"Vietnamese", "English"}
    };

    private LanguageProfiles() { }

    /** Source and target language of a profile; null for "Tùy chỉnh" (the fields stay as they are). */
    static String[] languages(String label) {
        for (int i = 0; i < PAIRS.length; i++) if (LABELS[i].equals(label)) return PAIRS[i];
        return null;
    }
}
