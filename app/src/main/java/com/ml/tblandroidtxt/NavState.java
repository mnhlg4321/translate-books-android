package com.ml.tblandroidtxt;

import android.os.Bundle;

/** Navigation state that must survive Activity recreation for the Volume page. */
final class NavState {
    private static final String PAGE = "nav.page";
    private static final String SET_ID = "nav.setId";
    private static final String ORIGIN = "nav.origin";

    String page = "Translate";
    String setId = "";
    String originTab = "Translate";

    static NavState from(Bundle state) {
        NavState out = new NavState();
        if (state == null) return out;
        out.page = safe(state.getString(PAGE, state.getString("currentTab", out.page)));
        out.setId = safe(state.getString(SET_ID, ""));
        out.originTab = safe(state.getString(ORIGIN, out.originTab));
        if (out.page.isEmpty()) out.page = "Translate";
        if (out.originTab.isEmpty()) out.originTab = "Translate";
        return out;
    }

    void save(Bundle state) {
        state.putString(PAGE, safe(page));
        state.putString(SET_ID, safe(setId));
        state.putString(ORIGIN, safe(originTab));
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
