package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NavStateTest {
    @Test public void missingStateUsesSafeDefaults() {
        NavState state = NavState.from(null);
        assertEquals("Translate", state.page);
        assertEquals("", state.setId);
        assertEquals("Translate", state.originTab);
    }
}
