package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EditorialSafe4ProfileGeneratorTest {
    @Test public void sameInputsProduceSameCanonicalProfileBytes() {
        String first = EditorialSafe4TrustedProfileFactory.canonicalJson(
                "b5589bf5f2b3b60942841950c5877e6f8281f7ff", "2026-09-03T00:00:00+07:00");
        String second = EditorialSafe4TrustedProfileFactory.canonicalJson(
                "b5589bf5f2b3b60942841950c5877e6f8281f7ff", "2026-09-03T00:00:00+07:00");
        assertEquals(first, second);
        assertTrue(first.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 0);
        assertTrue(!first.endsWith("\n"));
    }
}
