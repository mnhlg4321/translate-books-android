package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** v4.23 J3: the numbers and lines of the one-screen progress panel. */
public class V423RunPanelTest {
    @Test public void titleIsVietnameseAndNamesTheSetOrTheFile() {
        assertEquals("\u0110ang d\u1ecbch \u00b7 T\u1eadp ATE_JP_VOL6", RunPanelFormatter.title("RUNNING", true, "ATE_JP_VOL6", "004.txt"));
        assertEquals("\u0110ang d\u1ecbch \u00b7 004.txt", RunPanelFormatter.title("RUNNING", false, "", "004.txt"));
        assertEquals("T\u1ea1m d\u1eebng \u00b7 T\u1eadp X", RunPanelFormatter.title("PAUSED", true, "X", ""));
        assertEquals("\u0110\u00e3 d\u1eebng \u00b7 T\u1eadp", RunPanelFormatter.title("IDLE", true, "", ""));
        assertEquals("Xong \u00b7 T\u1eadp X", RunPanelFormatter.title("COMPLETED", true, "X", "a"));
        assertEquals("L\u1ed7i \u00b7 T\u1eadp X", RunPanelFormatter.title("FAILED", true, "X", "a"));
        assertEquals("\u0110ang chu\u1ea9n b\u1ecb", RunPanelFormatter.title("VALIDATING", false, "", ""));
    }

    @Test public void chipAndTitleAreDrawnFromTheSameState() {
        // A run in progress: the chip follows the broadcast state, never a stale saved COMPLETED.
        assertEquals("RUNNING", RunPanelFormatter.chipState(true, "RUNNING", "COMPLETED"));
        assertEquals("PAUSED", RunPanelFormatter.chipState(true, "PAUSED", "RUNNING"));
        assertEquals("RUNNING", RunPanelFormatter.chipState(true, "", "COMPLETED"));
        assertEquals("RUNNING", RunPanelFormatter.chipState(true, "COMPLETED", ""));
        // Nothing running: the last saved state.
        assertEquals("COMPLETED", RunPanelFormatter.chipState(false, "RUNNING", "COMPLETED"));
        assertEquals("FAILED", RunPanelFormatter.chipState(false, "", "failed"));
        assertEquals("IDLE", RunPanelFormatter.chipState(false, "", ""));
        for (String st : new String[]{"RUNNING", "PAUSED", "VALIDATING"}) {
            String chip = RunPanelFormatter.chipState(true, st, "COMPLETED");
            assertEquals(RunPanelFormatter.stateLabel(chip.equals("RUNNING") ? "RUNNING" : chip), RunPanelFormatter.stateLabel(st));
        }
    }

    @Test public void setAndChapterLines() {
        assertEquals("Chương 4 (4/14) · 3 xong · 0 lỗi", RunPanelFormatter.setLine(4, 14, 3, 0, 4));
        assertEquals("Chương 2/3 · 1 xong · 1 lỗi", RunPanelFormatter.setLine(2, 3, 1, 1, -1));
        assertEquals("chunk 5/6 · 004.txt", RunPanelFormatter.chapterLine(5, 6, "004.txt"));
        assertEquals("chunk —", RunPanelFormatter.chapterLine(0, 0, ""));
        assertEquals("chunk 6/6", RunPanelFormatter.chapterLine(9, 6, null));
    }

    @Test public void setProgressCountsFinishedAndFailedChaptersPlusTheCurrentOne() {
        assertEquals(0, RunPanelFormatter.setPermille(0, 0, 0, 0, 0));
        assertEquals(0, RunPanelFormatter.setPermille(14, 0, 0, 0, 6));
        assertEquals(Math.round(1000 * (3 + 4 / 6d) / 14), RunPanelFormatter.setPermille(14, 3, 0, 4, 6), 1);
        assertEquals("a failed chapter counts as passed", Math.round(1000 * (3 + 0.5) / 14), RunPanelFormatter.setPermille(14, 2, 1, 1, 2), 1);
        assertEquals(1000, RunPanelFormatter.setPermille(2, 1, 1, 6, 6));
        assertEquals(500, RunPanelFormatter.chunkPermille(3, 6));
    }

    @Test public void remainingTimeNeedsEnoughProgress() {
        assertEquals(-1L, RunPanelFormatter.remainingMs(60_000, 10));
        assertEquals(60_000L, RunPanelFormatter.remainingMs(60_000, 500));
        assertEquals(0L, RunPanelFormatter.remainingMs(60_000, 1000));
        assertEquals("…", RunPanelFormatter.remaining(-1));
        assertEquals("~31:00", RunPanelFormatter.remaining(31 * 60_000L));
    }

    @Test public void clockAndNumbers() {
        assertEquals("26:11", RunPanelFormatter.clock((26 * 60 + 11) * 1000L));
        assertEquals("1:02:03", RunPanelFormatter.clock(3723_000L));
        assertEquals("00:00", RunPanelFormatter.clock(-5));
        assertEquals("308.0k tok", RunPanelFormatter.tokens(308_000));
        assertEquals("$0.2602", RunPanelFormatter.money(0.2602));
        assertEquals("0 · 0", RunPanelFormatter.failures(0, 0));
        assertEquals("G10 · P4", RunPanelFormatter.rules(10, 4));
        assertEquals("—", RunPanelFormatter.rules(-1, -1));
    }

    @Test public void estimateAndCollapsedLines() {
        assertEquals("~$0.43 dự kiến cả tập · chương này 11.6k tok · $0.006", RunPanelFormatter.estimateLine(true, 0.43, 11_600, 0.006));
        assertEquals("1.0k tok · $0.001", RunPanelFormatter.estimateLine(false, 0, 1000, 0.0014));
        assertTrue(RunPanelFormatter.collapsedSource("ATE_JP_VOL6", 14, true, "", 0).startsWith("Tập ATE_JP_VOL6 · 14 chương"));
        assertEquals("a.txt · ▸", RunPanelFormatter.collapsedSource("", 0, false, "a.txt", 1));
    }
}
