package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** B3: recommendation undo is disabled before Apply, enabled after it, then disabled after Undo. */
public class RecommendationUndoModelTest {
    @Test public void notAppliedAppliedUndone() {
        boolean hasSnapshot = false;
        assertFalse(RecommendationUndoModel.isEnabled(hasSnapshot));
        assertEquals("Hoàn tác dùng được sau khi bấm Áp dụng", RecommendationUndoModel.helperText(hasSnapshot));

        hasSnapshot = true;
        assertTrue(RecommendationUndoModel.isEnabled(hasSnapshot));
        assertEquals("", RecommendationUndoModel.helperText(hasSnapshot));

        hasSnapshot = false;
        assertFalse(RecommendationUndoModel.isEnabled(hasSnapshot));
        assertEquals("Hoàn tác dùng được sau khi bấm Áp dụng", RecommendationUndoModel.helperText(hasSnapshot));
    }

    @Test public void disabledUndoHasActionableAccessibilityDescription() {
        assertEquals("Hoàn tác cài đặt khuyến nghị, chưa dùng được: bấm Áp dụng trước",
                RecommendationUndoModel.contentDescription(false));
        assertEquals("Hoàn tác cài đặt khuyến nghị", RecommendationUndoModel.contentDescription(true));
    }
}
