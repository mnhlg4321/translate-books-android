package com.ml.tblandroidtxt;

/** Centralized release metadata so UI/log exports do not drift across release patches. */
public final class AppBuildInfo {
    public static final int VERSION_CODE = 48;
    public static final String VERSION_NAME = "4.13-p0-dev";
    public static final String RELEASE_LABEL = "P0 translation-core hotfix in progress";
    public static final String COMMIT_SHA = "52fb24c (P0 translation-core hotfix)";
    public static final String BUILD_TIME = "2026-07-17 +07:00";
    public static final String APP_TITLE = "Translate Books";

    private AppBuildInfo() {}

    public static String exportVersionLine() {
        return VERSION_NAME + " / versionCode " + VERSION_CODE + " / " + COMMIT_SHA + " / " + BUILD_TIME;
    }

    public static String safeFileSuffix() {
        return VERSION_NAME.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
