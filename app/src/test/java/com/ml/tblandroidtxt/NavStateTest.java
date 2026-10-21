package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NavStateTest {
    @Test public void missingStateUsesSafeDefaults() {
        NavState state = NavState.from(null);
        assertEquals("Translate", state.page);
    }

    @Test public void removedVolumePagesNormalizeToTranslate() {
        assertEquals("Translate", NavState.normalizePage("Vol" + "ume"));
        assertEquals("Translate", NavState.normalizePage("All" + "Sets"));
        assertEquals("Jobs", NavState.normalizePage("Jobs"));
    }
}
