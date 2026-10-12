package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Pure JVM tests for the FINAL .txt export and its readback verification. */
public final class EditorialChapterFinalExportTest {
    private static final String VIETNAMESE = "Xin chào, thế giới!\nĐây là chương một — “đã sửa”.\nKết thúc";

    private static EditorialL2Execution.Committed committed(byte[] bytes, String sha) {
        byte[] map = "{\"qa\":true}".getBytes(StandardCharsets.UTF_8);
        return new EditorialL2Execution.Committed("a".repeat(64), "b".repeat(64), "c".repeat(64),
                bytes, sha, map, EditorialCanonicalJson.sha256Hex(map));
    }

    private static EditorialL2Execution.Committed valid(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return committed(bytes, EditorialCanonicalJson.sha256Hex(bytes));
    }

    @Test public void happyPathWritesAndVerifiesExactBytes() {
        EditorialL2Execution.Committed fin = valid(VIETNAMESE);
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        EditorialChapterFinalCoordinator.ExportResult result = EditorialChapterFinalCoordinator.exportTxt(
                fin, () -> sink, () -> new ByteArrayInputStream(sink.toByteArray()));
        assertTrue(result.reasonCode(), result.verified());
        assertEquals("EXPORT_VERIFIED", result.reasonCode());
        assertEquals(fin.viL2Sha256(), result.sha256());
        assertEquals(fin.viL2Bytes().length, result.byteCount());
        assertArrayEquals(fin.viL2Bytes(), sink.toByteArray());
    }

    @Test public void exportedBytesAreExactlyFinalWithoutBomOrExtraNewline() {
        EditorialL2Execution.Committed fin = valid(VIETNAMESE);
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        EditorialChapterFinalCoordinator.exportTxt(fin, () -> sink, () -> new ByteArrayInputStream(sink.toByteArray()));
        byte[] written = sink.toByteArray();
        assertArrayEquals(VIETNAMESE.getBytes(StandardCharsets.UTF_8), written);
        assertFalse(written.length >= 3 && (written[0] & 0xff) == 0xEF && (written[1] & 0xff) == 0xBB
                && (written[2] & 0xff) == 0xBF);
        assertTrue(written[written.length - 1] != '\n');
        assertEquals(VIETNAMESE, new String(written, StandardCharsets.UTF_8));

        // A FINAL that ends with a newline keeps exactly that one newline.
        EditorialL2Execution.Committed withNewline = valid("Dòng một\n");
        ByteArrayOutputStream sink2 = new ByteArrayOutputStream();
        EditorialChapterFinalCoordinator.exportTxt(withNewline, () -> sink2,
                () -> new ByteArrayInputStream(sink2.toByteArray()));
        assertArrayEquals("Dòng một\n".getBytes(StandardCharsets.UTF_8), sink2.toByteArray());
    }

    @Test public void missingFinalIsReported() {
        AtomicInteger opened = new AtomicInteger();
        EditorialChapterFinalCoordinator.ExportResult result = EditorialChapterFinalCoordinator.exportTxt(null,
                () -> { opened.incrementAndGet(); return new ByteArrayOutputStream(); },
                () -> { opened.incrementAndGet(); return new ByteArrayInputStream(new byte[0]); });
        assertFalse(result.verified());
        assertEquals("EXPORT_FINAL_MISSING", result.reasonCode());
        assertEquals(0, opened.get());
    }

    @Test public void integrityMismatchNeverOpensOutput() {
        byte[] bytes = VIETNAMESE.getBytes(StandardCharsets.UTF_8);
        EditorialL2Execution.Committed wrongSha = committed(bytes, EditorialCanonicalJson.sha256Hex("other".getBytes(StandardCharsets.UTF_8)));
        EditorialL2Execution.Committed wrongBytes = committed("tampered".getBytes(StandardCharsets.UTF_8),
                EditorialCanonicalJson.sha256Hex(bytes));
        for (EditorialL2Execution.Committed bad : new EditorialL2Execution.Committed[]{wrongSha, wrongBytes}) {
            AtomicInteger opened = new AtomicInteger();
            EditorialChapterFinalCoordinator.ExportResult result = EditorialChapterFinalCoordinator.exportTxt(bad,
                    () -> { opened.incrementAndGet(); return new ByteArrayOutputStream(); },
                    () -> { opened.incrementAndGet(); return new ByteArrayInputStream(new byte[0]); });
            assertFalse(result.verified());
            assertEquals("EXPORT_FINAL_INTEGRITY_INVALID", result.reasonCode());
            assertEquals(0, opened.get());
        }
    }

    @Test public void outputOpenFailureIsWriteFailed() {
        AtomicInteger readbacks = new AtomicInteger();
        EditorialChapterFinalCoordinator.ExportResult result = EditorialChapterFinalCoordinator.exportTxt(
                valid(VIETNAMESE), () -> { throw new IOException("denied"); },
                () -> { readbacks.incrementAndGet(); return new ByteArrayInputStream(new byte[0]); });
        assertFalse(result.verified());
        assertEquals("EXPORT_WRITE_FAILED", result.reasonCode());
        assertEquals(0, readbacks.get());
    }

    @Test public void readbackWithDifferentBytesIsMismatch() {
        EditorialL2Execution.Committed fin = valid(VIETNAMESE);
        EditorialChapterFinalCoordinator.ExportResult result = EditorialChapterFinalCoordinator.exportTxt(fin,
                ByteArrayOutputStream::new,
                () -> new ByteArrayInputStream("Xin chao".getBytes(StandardCharsets.UTF_8)));
        assertFalse(result.verified());
        assertEquals("EXPORT_READBACK_MISMATCH", result.reasonCode());
        assertFalse(fin.viL2Sha256().equals(result.sha256()));
    }

    @Test public void readbackFailureIsReported() {
        EditorialChapterFinalCoordinator.ExportResult openFails = EditorialChapterFinalCoordinator.exportTxt(
                valid(VIETNAMESE), ByteArrayOutputStream::new, () -> { throw new IOException("gone"); });
        assertFalse(openFails.verified());
        assertEquals("EXPORT_READBACK_FAILED", openFails.reasonCode());

        EditorialChapterFinalCoordinator.ExportResult readFails = EditorialChapterFinalCoordinator.exportTxt(
                valid(VIETNAMESE), ByteArrayOutputStream::new, () -> new InputStream() {
                    @Override public int read() throws IOException { throw new IOException("io"); }
                });
        assertFalse(readFails.verified());
        assertEquals("EXPORT_READBACK_FAILED", readFails.reasonCode());
    }
}
