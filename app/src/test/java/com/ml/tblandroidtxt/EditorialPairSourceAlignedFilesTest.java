package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Chunk pairs from two line-aligned chapter files, cut like the translation flow's chunks. */
public final class EditorialPairSourceAlignedFilesTest {
    private static final String RAW = "\n一行目です。\n\n二行目です。\n\n三行目です。\n四行目です。\n\n";
    private static final String DRAFT = "Dòng một.\n\nDòng hai.\n\nDòng ba.\nDòng bốn.\n";

    @Test public void rowsConcatenateBackToEachFileAndKeepLineRangesTogether() {
        EditorialPairSource s = EditorialPairSource.fromAlignedFiles("007", RAW, DRAFT, 12);
        assertTrue(s.lineageIssues.isEmpty());
        assertEquals(EditorialPairModels.SOURCE_FILES, s.kind);
        assertEquals(String.join("", s.rawRows), RAW);
        assertEquals(String.join("", s.draftRows), DRAFT);
        assertEquals(2, s.rawRows.size());
        assertEquals("\n一行目です。\n\n二行目です。\n\n", s.rawRows.get(0));
        assertEquals("Dòng một.\n\nDòng hai.\n\n", s.draftRows.get(0));
        assertEquals("Dòng ba.\nDòng bốn.\n", s.draftRows.get(1));
    }

    @Test public void aLineLongerThanTheBudgetIsAChunkOfItsOwn() {
        EditorialPairSource s = EditorialPairSource.fromAlignedFiles("x", "長い長い一行。\n短。\n", "Một dòng dài.\nNgắn.\n", 3);
        assertEquals(List.of("長い長い一行。\n", "短。\n"), s.rawRows);
        assertEquals(List.of("Một dòng dài.\n", "Ngắn.\n"), s.draftRows);
    }

    @Test public void aWholeChapterWithinTheBudgetIsOneChunk() {
        EditorialPairSource s = EditorialPairSource.fromAlignedFiles("x", RAW, DRAFT, 10_000);
        assertEquals(1, s.rawRows.size());
        assertEquals(DRAFT, s.draftRows.get(0));
    }

    @Test public void differentLineCountsGiveNoRowsAndATypedIssue() {
        EditorialPairSource s = EditorialPairSource.fromAlignedFiles("x", RAW, "Dòng một.\nDòng hai.\n", 12);
        assertTrue(s.rawRows.isEmpty());
        assertEquals(List.of("LINE_COUNT_MISMATCH:4/2"), s.lineageIssues);
        assertEquals(List.of("LINE_COUNT_MISMATCH:0/0"), EditorialPairSource.fromAlignedFiles("x", "", "\n", 12).lineageIssues);
    }

    @Test public void theRowsBuildAPairMapWithOnePairPerChunk() {
        EditorialPairSource s = EditorialPairSource.fromAlignedFiles("007", RAW, DRAFT, 12);
        PairMap map = PairMaps.fromJobRows("007", "raw", "draft", s.rawRows, s.draftRows);
        assertEquals(2, map.entries().size());
    }
}
