package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class V5FinalExtractorTest {
    @Test public void extractsOnlyTheDelimitedFinal() {
        assertEquals("bản cuối", V5FinalExtractor.extract("REPORT\n<FINAL>\n bản cuối \n</FINAL>\nQA"));
    }

    @Test public void missingOrEmptyFinalIsNotAccepted() {
        assertTrue(V5FinalExtractor.extract("REPORT only").isEmpty());
        assertTrue(V5FinalExtractor.extract("<FINAL>  </FINAL>").isEmpty());
    }
}
