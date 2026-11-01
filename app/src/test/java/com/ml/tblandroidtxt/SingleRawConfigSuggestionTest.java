package com.ml.tblandroidtxt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class SingleRawConfigSuggestionTest {
    @Test public void sameLeadingChapterIsOnlySuggestedAndSelectionIsExplicitlyPreserved() {
        String summary = SingleRawConfigSuggestion.format(25,
                Arrays.asList("025_Glossary.csv", "126_wrong-chapter.csv", "Glossary_025.csv"),
                Arrays.asList("025_Pronoun.csv", "026_wrong-chapter.csv"));

        assertTrue(summary.contains("Gợi ý cùng chương 25"));
        assertTrue(summary.contains("025_Glossary.csv"));
        assertTrue(summary.contains("025_Pronoun.csv"));
        assertFalse(summary.contains("126_wrong-chapter.csv"));
        assertFalse(summary.contains("026_wrong-chapter.csv"));
        assertFalse(summary.contains("Glossary_025.csv"));
        assertTrue(summary.contains("Cấu hình đang chọn được giữ nguyên."));
    }

    @Test public void unnumberedRawDoesNotSuggestProfilesFromDigitsInTheMiddle() {
        String summary = SingleRawConfigSuggestion.format(-1,
                Collections.singletonList("Glossary_025.csv"), Collections.<String>emptyList());

        assertTrue(summary.contains("Không xác định số chương"));
        assertTrue(summary.contains("Cấu hình đang chọn được giữ nguyên."));
        assertFalse(summary.contains("Glossary_025.csv"));
    }
}
