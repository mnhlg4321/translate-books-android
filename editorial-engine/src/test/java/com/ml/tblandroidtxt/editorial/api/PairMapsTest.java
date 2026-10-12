package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** JOB-01 (engine side): the row of a job is the link; a row without a translation stays a visible missing pair. */
public final class PairMapsTest {
    private static final List<String> RAW = Arrays.asList("第一。\n\n", "第二。\n\n", "第三。\n");
    private static final List<String> DRAFT = Arrays.asList("Một.\n\n", "Hai.\n\n", "Ba.\n");

    @Test public void everyRowIsOnePairAndTheMapVerifiesAndMergesToTheDraft() {
        PairMap map = PairMaps.fromJobRows("CH001", "job-7-raw", "job-7-draft", RAW, DRAFT);
        assertEquals(3, map.entries().size());
        assertTrue(map.verify().toString(), map.verify().isEmpty());
        assertEquals("Một.", map.draftText(map.entries().get(0)));
        assertEquals("第二。", map.rawText(map.entries().get(1)));
        Map<String, String> own = new HashMap<>();
        for (PairMap.Entry e : map.entries()) own.put(e.pairId(), map.draftText(e));
        ChunkMerge.Result merged = ChunkMerge.merge(map, own, false);
        assertTrue(merged.ok());
        assertEquals(map.draft.text, merged.text());
    }

    @Test public void aRowWithoutATranslationIsAMissingPairAndBlocksTheMerge() {
        PairMap map = PairMaps.fromJobRows("CH001", "r", "d", RAW, Arrays.asList("Một.\n\n", null, "Ba.\n"));
        assertEquals(1, map.missingPairs().size());
        assertEquals(1, map.missingPairs().get(0).displayOrdinal() - 1);
        assertFalse(map.verify().isEmpty());
        assertFalse(ChunkMerge.merge(map, new HashMap<>(), true).ok());
    }

    @Test public void theRowCountsMustMatchAndAnOrphanTranslationIsNotLinkedToAnything() {
        try {
            PairMaps.fromJobRows("", "r", "d", RAW, DRAFT.subList(0, 2));
            throw new AssertionError("different row counts must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("rows differ"));
        }
        PairMap orphan = PairMaps.fromJobRows("", "r", "d", Arrays.asList("第一。", " ", "第三。"), Arrays.asList("Một.", "Hai không có nguồn.", "Ba."));
        assertEquals(2, orphan.entries().size());
        // the translation of a row that has no source keeps its own slot and is reported, the next pair keeps its own translation
        assertEquals("Ba.", orphan.draftText(orphan.entries().get(1)));
        boolean reported = false;
        for (DocManifest.Issue i : orphan.verify()) if (i.code().equals("DRAFT_UNIT_UNMAPPED")) reported = true;
        assertTrue(reported);
    }
}
