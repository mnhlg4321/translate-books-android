package com.ml.tblandroidtxt;

/** Pure lifecycle policy used by the Activity and covered by local unit tests. */
public final class TranslationUiStatePolicy {
    private TranslationUiStatePolicy() {}

    public static boolean isTranslationActive(boolean serviceActive, String savedStatus) {
        if (!serviceActive) return false;
        return !"done".equals(savedStatus) && !"error".equals(savedStatus) && !"cancelled".equals(savedStatus);
    }

    public static boolean mayStartNewJob(boolean serviceActive, boolean uiActive) {
        return !serviceActive && !uiActive;
    }

    public static boolean mayDispatchStart(boolean userInitiated, boolean serviceActive, boolean uiActive) {
        return userInitiated && mayStartNewJob(serviceActive, uiActive);
    }
}
