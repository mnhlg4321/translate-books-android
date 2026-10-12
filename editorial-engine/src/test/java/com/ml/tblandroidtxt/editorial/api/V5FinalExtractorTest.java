package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class V5FinalExtractorTest {
    @Test public void extractsOnlyTheDelimitedFinal() {
        assertEquals("bản cuối", V5FinalExtractor.extract("REPORT\n<FINAL>\n bản cuối \n</FINAL>\nQA"));
    }

    @Test public void extractsTheNamedFinalQaFileBlockFromTheThirdTurn() {
        String reply = "QA report\n=== FILE: [007]_FINAL_QA_JAKUAKU_MONSTER_VOL1.txt ===\nBản cuối dòng một.\nDòng hai.\n"
                + "=== END FILE ===\n=== FILE: [007]_QA_RECEIPT_JAKUAKU_MONSTER_VOL1.txt ===\nReceipt";
        assertEquals("Bản cuối dòng một.\nDòng hai.", V5FinalExtractor.extract(reply));
        assertEquals("Bản dịch.\nDòng hai.", V5FinalExtractor.extract("QA\n[007]_FINAL_QA_JAKUAKU_MONSTER_VOL1.txt\n```text\nBản dịch.\nDòng hai.\n```\nQA receipt"));
    }

    @Test public void missingOrEmptyFinalIsNotAccepted() {
        assertTrue(V5FinalExtractor.extract("REPORT only").isEmpty());
        assertTrue(V5FinalExtractor.extract("<FINAL>  </FINAL>").isEmpty());
        assertTrue(V5FinalExtractor.extract("[007]_FINAL_QA_JAKUAKU_MONSTER_VOL1.txt\n```text\npartial").isEmpty());
    }
}
