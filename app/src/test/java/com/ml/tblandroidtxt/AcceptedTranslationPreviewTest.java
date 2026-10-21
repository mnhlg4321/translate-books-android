package com.ml.tblandroidtxt;

import static org.junit.Assert.*;
import org.junit.Test;

public class AcceptedTranslationPreviewTest {
    @Test public void chapterTransitionKeepsAcceptedChunkWithItsOriginalIdentity() {
        AcceptedTranslationPreview p = new AcceptedTranslationPreview();
        p.beginRun();
        p.beginFile("001.txt", 5);
        p.accept("Bản dịch chương một", 5, 5);
        p.beginFile("002.txt", 2);
        assertEquals("Bản dịch chương một", p.text);
        assertEquals("001.txt", p.file);
        assertEquals(5, p.chunk);
        assertEquals(5, p.total);
        assertEquals("Bản dịch gần nhất · Chunk 5/5\n001.txt",
                TranslationDashboardFormatter.previewMeta(p.chunk, p.total, p.text, p.file));
        p.accept("Bản dịch chương hai", 1, 2);
        assertEquals("002.txt", p.file);
        assertEquals(1, p.chunk);
        assertEquals(2, p.total);
        assertEquals("Bản dịch chương hai", p.text);
    }

    @Test public void failureBeforeAcceptanceKeepsLastGoodTextAndNewRunClearsIt() {
        AcceptedTranslationPreview p = new AcceptedTranslationPreview();
        p.beginFile("a.txt", 6);
        p.accept("Đã nhận", 4, 6);
        p.beginFile("b.txt", 2);
        p.accept("   ", 1, 2);
        p.accept(null, 1, 2);
        p.beginFile("c.txt", 3);
        assertEquals("a.txt", p.file);
        assertEquals("Đã nhận", p.text);
        p.beginRun();
        p.beginFile("new.txt", 4);
        assertEquals("", p.text);
        assertEquals("", p.file);
        assertEquals(0, p.chunk);
        assertEquals(0, p.total);
    }

    @Test public void retryAndSplitUseAcceptedOrdinalAndUpdatedTotal() {
        AcceptedTranslationPreview p = new AcceptedTranslationPreview();
        p.beginFile("a.txt", 2);
        p.accept("Checkpoint", 2, 2);
        p.accept("Retry chunk đầu", 1, 2);
        assertEquals(1, p.chunk);
        assertEquals("Retry chunk đầu", p.text);
        p.accept("Sau chia nhỏ", 3, 4);
        assertEquals(4, p.total);
    }

    @Test public void tailIsBoundedWithoutSplittingASurrogatePair() {
        AcceptedTranslationPreview p = new AcceptedTranslationPreview();
        p.beginFile("a.txt", 1);
        p.accept("x" + "\uD83D\uDE00" + "z".repeat(1199), 1, 1);
        assertEquals(1201, p.text.length());
        assertTrue(p.text.startsWith("\uD83D\uDE00"));
    }

    @Test public void missingLegacyIdentityDoesNotClaimCurrentChapter() {
        assertEquals("Bản dịch gần nhất · Chunk 5",
                TranslationDashboardFormatter.previewMeta(5, 0, "text", null));
        assertEquals("Bản dịch gần nhất\nChưa có chunk được chấp nhận",
                TranslationDashboardFormatter.previewMeta(0, 0, "", ""));
    }
}
