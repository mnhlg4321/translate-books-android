package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RawAlignedNormalizerTest {
    @Test public void repairsOnlySymbolsProvenByTheAlignedRawLine() {
        RawAlignedNormalizer.Result result = RawAlignedNormalizer.normalize(
                "〝原文……〟 《かな》 ? 12",
                "“bản… ” 【かな】 ？１２");
        assertTrue(result.alignmentStrong());
        assertTrue(result.text().contains("〝"));
        assertTrue(result.text().contains("……"));
        assertTrue(result.text().contains("?12"));
        assertFalse(result.text().contains("《かな》"));
        assertTrue(result.repairs().stream().anyMatch(r -> r.kind().equals("REMOVE_COPIED_RUBY")));
    }

    @Test public void doesNotApplyAFrameWhenRawHasNoFrame() {
        RawAlignedNormalizer.Result result = RawAlignedNormalizer.normalize("Plain source", "【plain】");
        assertEquals("【plain】", result.text());
        assertTrue(result.repairs().isEmpty());
    }

    @Test public void uncertainLineAlignmentReportsOffsetAndDoesNotRepair() {
        RawAlignedNormalizer.Result result = RawAlignedNormalizer.normalize("one\ntwo\nthree\nfour\nfive\nsix\nseven\neight\nnine\nten",
                "một\nhai\nba");
        assertFalse(result.alignmentStrong());
        assertTrue(result.repairs().isEmpty());
        assertTrue(result.detections().stream().anyMatch(d -> d.kind().equals("LINE_OFFSET")));
    }

    @Test public void remainingJapaneseIsADetectionForTheModel() {
        RawAlignedNormalizer.Result result = RawAlignedNormalizer.normalize("日本語", "Xin chào あはっ");
        assertTrue(result.detections().stream().anyMatch(d -> d.kind().equals("UNTRANSLATED")));
    }
}
