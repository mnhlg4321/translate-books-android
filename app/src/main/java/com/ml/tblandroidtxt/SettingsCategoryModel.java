package com.ml.tblandroidtxt;

/** Stable five-category map for Settings; old saved navigation ids are accepted for compatibility. */
final class SettingsCategoryModel {
    static final String GENERAL = "General";
    static final String PROVIDER = "Provider";
    static final String EDITORIAL = "Biên tập";
    static final String APPEARANCE = "Appearance";
    static final String ADVANCED = "Advanced";
    private static final String[] IDS = {GENERAL, PROVIDER, EDITORIAL, APPEARANCE, ADVANCED};

    private SettingsCategoryModel() { }

    static String[] ids() { return IDS.clone(); }

    static String normalize(String id) {
        if (GENERAL.equals(id) || PROVIDER.equals(id) || EDITORIAL.equals(id) || APPEARANCE.equals(id) || ADVANCED.equals(id)) return id;
        if ("Translation".equals(id) || "Prompt".equals(id)) return GENERAL;
        if ("Performance".equals(id) || "Logging".equals(id) || "About".equals(id)) return ADVANCED;
        return GENERAL;
    }

    static String label(String id) {
        String n = normalize(id);
        if (GENERAL.equals(n)) return "Dịch";
        if (PROVIDER.equals(n)) return "Provider & chi phí";
        if (EDITORIAL.equals(n)) return "Biên tập";
        if (APPEARANCE.equals(n)) return "Giao diện";
        return "Nâng cao & thông tin";
    }
}
