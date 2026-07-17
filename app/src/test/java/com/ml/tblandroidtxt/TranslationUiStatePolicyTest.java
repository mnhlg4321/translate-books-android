package com.ml.tblandroidtxt;

import org.junit.Test;

import static org.junit.Assert.*;

public class TranslationUiStatePolicyTest {
    @Test public void changingTabsDoesNotChangeServiceOwnedJobState() {
        assertTrue(TranslationUiStatePolicy.isTranslationActive(true, "running"));
        assertTrue(TranslationUiStatePolicy.isTranslationActive(true, "paused"));
    }

    @Test public void returningToTabCannotStartDuplicateJob() {
        assertFalse(TranslationUiStatePolicy.mayStartNewJob(true, false));
        assertFalse(TranslationUiStatePolicy.mayStartNewJob(true, true));
        assertTrue(TranslationUiStatePolicy.mayStartNewJob(false, false));
    }

    @Test public void staleRunningSnapshotDoesNotPretendServiceIsAliveAfterRecreation() {
        assertFalse(TranslationUiStatePolicy.isTranslationActive(false, "running"));
    }

    @Test public void tabOrActivityRestorationNeverDispatchesStart() {
        assertFalse(TranslationUiStatePolicy.mayDispatchStart(false, false, false));
        assertTrue(TranslationUiStatePolicy.mayDispatchStart(true, false, false));
    }
}
