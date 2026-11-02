package com.ml.tblandroidtxt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TranslateInputLabelTest {
    @Test public void emptyLibraryLabelNamesRaw() {
        assertEquals("Chưa chọn RAW", TranslateInputLabel.empty(true));
    }

    @Test public void emptyDeviceLabelKeepsTxtWording() {
        assertEquals("Chưa chọn file TXT", TranslateInputLabel.empty(false));
    }

    @Test public void selectedLibraryLabelIncludesNameAndChapter() {
        assertEquals("📄 025_QA_RAW.txt · Chương 025",
                TranslateInputLabel.librarySelection("025_QA_RAW.txt", "Chương 025"));
    }
}
