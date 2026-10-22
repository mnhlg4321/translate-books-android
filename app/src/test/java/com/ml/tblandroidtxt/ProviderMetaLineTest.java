package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProviderMetaLineTest {
    @Test public void languagePairComesBeforeProviderSoTheTargetIsNeverCutFirst() {
        String line = MainActivity.providerMetaLine("openrouter", "Japanese", "Vietnamese");
        assertEquals("Japanese → Vietnamese • openrouter", line);
        assertTrue(line.indexOf("Vietnamese") < line.indexOf("openrouter"));
    }
}
