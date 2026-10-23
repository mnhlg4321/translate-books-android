package com.ml.tblandroidtxt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** B1: the tab row keeps labels intact and exposes overflow at the two largest checks. */
public class LibraryTabLayoutModelTest {
    @Test public void app130System100FitsWithCompactPadding() {
        assertFalse(LibraryTabLayoutModel.needsHorizontalScroll(1.3f, 1.0f, 360));
    }

    @Test public void app130System130EnablesHorizontalScroll() {
        assertTrue(LibraryTabLayoutModel.needsHorizontalScroll(1.3f, 1.3f, 360));
        assertTrue(LibraryTabLayoutModel.shouldUseHorizontalScroll(1.3f, 1.3f, 360));
    }

    @Test public void everyTabKeepsTheMinimumTouchWidth() {
        for (String label : new String[]{"RAW", "Glossary", "Pronoun", "Tập"}) {
            assertTrue(LibraryTabLayoutModel.tabWidthDp(label, 1.3f, 1.3f)
                    >= LibraryTabLayoutModel.TAB_MIN_WIDTH_DP);
        }
    }
}
