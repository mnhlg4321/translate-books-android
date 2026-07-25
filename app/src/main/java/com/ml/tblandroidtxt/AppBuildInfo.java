package com.ml.tblandroidtxt;

/** Centralized release metadata so UI/log exports do not drift across release patches. */
public final class AppBuildInfo {
    public static final int VERSION_CODE = BuildConfig.VERSION_CODE;
    public static final String VERSION_NAME = BuildConfig.VERSION_NAME;
    public static final String RELEASE_LABEL = "P0 translation-core hotfix in progress";
    public static final String COMMIT_SHA = BuildConfig.BUILD_GIT_COMMIT;
    public static final String BUILD_TIME = BuildConfig.BUILD_TIMESTAMP;
    public static final String BUILD_EVENT_ID = BuildConfig.BUILD_EVENT_ID;
    public static final String APP_TITLE = "Translate Books";

    private AppBuildInfo() {}

    public static String exportVersionLine() {
        return VERSION_NAME + " / versionCode " + VERSION_CODE + " / "
                + BUILD_EVENT_ID + " / " + COMMIT_SHA + " / " + BUILD_TIME;
    }

    public static String safeFileSuffix() {
        return VERSION_NAME.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
