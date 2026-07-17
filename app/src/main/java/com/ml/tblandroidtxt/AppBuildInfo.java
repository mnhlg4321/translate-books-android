package com.ml.tblandroidtxt;

/** Centralized release metadata so UI/log exports do not drift across release patches. */
public final class AppBuildInfo {
    public static final int VERSION_CODE = 47;
    public static final String VERSION_NAME = "4.8";
    public static final String RELEASE_LABEL = "v4.8 Import navigation reliability";
    public static final String APP_TITLE = "Translate Books";

    private AppBuildInfo() {}

    public static String exportVersionLine() {
        return VERSION_NAME + " / versionCode " + VERSION_CODE;
    }

    public static String safeFileSuffix() {
        return VERSION_NAME.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
