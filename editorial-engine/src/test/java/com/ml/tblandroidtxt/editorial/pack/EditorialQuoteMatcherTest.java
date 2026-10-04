package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Shared evidence comparison used by the ledger validators. */
public final class EditorialQuoteMatcherTest {
    @Test public void normalizesNfcAndOuterWhitespaceAndRemovesOnlyRawRubyReadings() {
        String raw = "  café and 揃《そろ》えても  ";
        assertTrue(EditorialQuoteMatcher.containsRaw(raw, " 揃えても "));
        assertTrue(EditorialQuoteMatcher.containsRaw(raw, " 揃《そろ》えても "));
        assertTrue(EditorialQuoteMatcher.contains(raw, " cafe\u0301 and 揃《そろ》えても "));
        assertFalse(EditorialQuoteMatcher.containsRaw(raw, "そろ"));
        assertFalse(EditorialQuoteMatcher.containsRaw(raw, "not present"));
    }

    @Test public void citationsThatBecomeEmptyAfterNormalizationNeverMatch() {
        for (String emptyCitation : new String[] {"", " \t\n ", "《x》", " 《ふりがな》 "}) {
            assertFalse("contains should reject: " + emptyCitation,
                    EditorialQuoteMatcher.contains("synthetic anchor", emptyCitation));
            assertFalse("containsRaw should reject: " + emptyCitation,
                    EditorialQuoteMatcher.containsRaw("synthetic anchor", emptyCitation));
        }
    }

    @Test public void nonemptySyntheticFuriganaCitationStillMatchesVisibleRawText() {
        String anchor = "試験例《しけんれい》を表示する";
        assertTrue(EditorialQuoteMatcher.containsRaw(anchor, "試験例を表示"));
        assertTrue(EditorialQuoteMatcher.containsRaw(anchor, " 試験例《しけんれい》を表示 "));
    }
}
