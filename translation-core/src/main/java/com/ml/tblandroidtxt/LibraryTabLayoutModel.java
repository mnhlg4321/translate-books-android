package com.ml.tblandroidtxt;

/** Pure sizing rules for the four Library tabs and their fixed overflow menu. */
final class LibraryTabLayoutModel {
    static final int MENU_WIDTH_DP = 48;
    static final int TAB_MIN_WIDTH_DP = 48;
    static final int TAB_HORIZONTAL_PADDING_DP = 8;
    static final int TAB_GAP_DP = 4;
    static final int ROW_HEIGHT_DP = 48;
    static final int NAV_TOP_PADDING_DP = 4;
    static final int NAV_BOTTOM_PADDING_DP = 6;
    static final float TAB_TEXT_SP = 13f;
    static final float MENU_TEXT_SP = 20f;

    private static final String[] LABELS = {"RAW", "Glossary", "Pronoun", "Tập"};
    // Measured on the owner's phone at 13 sp bold: "Glossary" 0.48 sp per character, "RAW" 0.69; 0.50 keeps the estimate just above the widest label ("Glossary").
    private static final float BOLD_TEXT_WIDTH_FACTOR = 0.50f;

    private LibraryTabLayoutModel() {}

    static int tabWidthDp(String label, float appScale, float systemScale) {
        return Math.max(TAB_MIN_WIDTH_DP, (int) Math.ceil(estimatedTextWidthDp(label, appScale, systemScale)
                + 2 * TAB_HORIZONTAL_PADDING_DP));
    }

    /** The one width every tab gets: the widest label with its padding. */
    static int equalTabWidthDp(float appScale, float systemScale) {
        int widest = TAB_MIN_WIDTH_DP;
        for (String label : LABELS) widest = Math.max(widest, tabWidthDp(label, appScale, systemScale));
        return widest;
    }

    static int totalTabWidthDp(float appScale, float systemScale) {
        return LABELS.length * equalTabWidthDp(appScale, systemScale) + (LABELS.length - 1) * TAB_GAP_DP;
    }

    /** Height of the row that holds the tabs and the menu: the tabs and the menu button must fit inside it with the padding. */
    static int navHeightDp() { return NAV_TOP_PADDING_DP + ROW_HEIGHT_DP + NAV_BOTTOM_PADDING_DP; }

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
