package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** B2: blank or redundant icon labels do not create an empty leading gutter. */
public class SectionCardHeaderModelTest {
    @Test public void blankIconKeepsTitleWithoutIconGap() {
        assertFalse(SectionCardHeaderModel.showIcon("", "Dịch"));
        assertEquals(0, SectionCardHeaderModel.iconGapDp("", "Dịch"));
        assertTrue(SectionCardHeaderModel.showTitle("Dịch"));
    }

    @Test public void repeatedIconAndTitleAreNotDrawnTwice() {
        assertFalse(SectionCardHeaderModel.showIcon("RAW", "RAW"));
        assertEquals(0, SectionCardHeaderModel.iconGapDp("RAW", "RAW"));
        assertTrue(SectionCardHeaderModel.showTitle("RAW"));
    }

    @Test public void distinctIconAndTitleKeepTheHeaderGutter() {
        assertTrue(SectionCardHeaderModel.showIcon("✎", "Sửa"));
        assertEquals(10, SectionCardHeaderModel.iconGapDp("✎", "Sửa"));
    }
}
