package com.ml.tblandroidtxt;

/** Pure header decision used by ordinary section cards. */
final class SectionCardHeaderModel {
    private SectionCardHeaderModel() {}

    static boolean showIcon(String icon, String title) {
        String candidate = icon == null ? "" : icon.trim();
        String heading = title == null ? "" : title.trim();
        return !candidate.isEmpty() && !candidate.equals(heading);
    }

    static int iconGapDp(String icon, String title) {
        return showIcon(icon, title) ? 10 : 0;
    }

    static boolean showTitle(String title) {
        return title != null && !title.trim().isEmpty();
    }
}
