package com.ml.tblandroidtxt;

/** Pure sizing rules for the four Library tabs and their fixed overflow menu. */
final class LibraryTabLayoutModel {
    static final int MENU_WIDTH_DP = 48;
    static final int TAB_MIN_WIDTH_DP = 48;
    static final int TAB_HORIZONTAL_PADDING_DP = 4;
    static final float TAB_TEXT_SP = 13f;

    private static final String[] LABELS = {"RAW", "Glossary", "Pronoun", "Tập"};
    private static final float BOLD_TEXT_WIDTH_FACTOR = 0.68f;

    private LibraryTabLayoutModel() {}

    static int tabWidthDp(String label, float appScale, float systemScale) {
        return Math.max(TAB_MIN_WIDTH_DP, (int) Math.ceil(estimatedTextWidthDp(label, appScale, systemScale)
                + 2 * TAB_HORIZONTAL_PADDING_DP));
    }

    static int totalTabWidthDp(float appScale, float systemScale) {
        int total = 0;
        for (String label : LABELS) total += tabWidthDp(label, appScale, systemScale);
        return total;
    }

    static boolean needsHorizontalScroll(float appScale, float systemScale, int screenWidthDp) {
        if (screenWidthDp <= MENU_WIDTH_DP) return true;
        return totalTabWidthDp(appScale, systemScale) > screenWidthDp - MENU_WIDTH_DP;
    }

    static boolean shouldUseHorizontalScroll(float appScale, float systemScale, int screenWidthDp) {
        return needsHorizontalScroll(appScale, systemScale, screenWidthDp);
    }

    static float estimatedTextWidthDp(String label, float appScale, float systemScale) {
        if (label == null || label.isEmpty()) return 0f;
        float scale = Math.max(0f, appScale) * Math.max(0f, systemScale);
        return label.length() * TAB_TEXT_SP * BOLD_TEXT_WIDTH_FACTOR * scale;
    }
}
