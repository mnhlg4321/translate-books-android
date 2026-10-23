package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WorkspaceFilterModelTest {
    @Test public void activeFilterNeverFallsBackToAll() {
        assertTrue(WorkspaceFilterModel.active("set-1"));
        assertEquals("Đang lọc", WorkspaceFilterModel.label("set-1", ""));
        assertEquals("Tập: Đang lọc  ×", WorkspaceFilterModel.chipText("set-1", ""));
    }

    @Test public void emptyFilterUsesAllWithoutClearAffordance() {
        assertEquals("Tất cả", WorkspaceFilterModel.label("", "Tập A"));
        assertEquals("Tập: Tất cả", WorkspaceFilterModel.chipText("", "Tập A"));
    }
}
