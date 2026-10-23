package com.ml.tblandroidtxt;

import android.os.Bundle;

/** Navigation state that must survive Activity recreation for the current page. */
final class NavState {
    private static final String PAGE = "nav.page";

    String page = "Translate";

    static NavState from(Bundle state) {
        NavState out = new NavState();
        if (state == null) return out;
        out.page = safe(state.getString(PAGE, state.getString("currentTab", out.page)));
        out.page = normalizePage(out.page);
        return out;
    }

    void save(Bundle state) {
        state.putString(PAGE, safe(page));
    }

    static String normalizePage(String page) {
        if ("Files".equals(page) || "Sample".equals(page)) return "RAW";
        if (!isValidPage(page)) return "Translate";
        return page;
    }

    private static boolean isValidPage(String page) {
        return "Translate".equals(page) || "Jobs".equals(page) || "JobsHistory".equals(page) || "Editorial".equals(page)
                || "Settings".equals(page) || "RAW".equals(page)
                || "Glossaries".equals(page) || "Pronouns".equals(page)
                || "ChapterSets".equals(page);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
