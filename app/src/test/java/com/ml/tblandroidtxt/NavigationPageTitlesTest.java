package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NavigationPageTitlesTest {
    @Test public void topTitleNamesEveryPrimaryDestination() {
        assertEquals("Dịch", NavigationPageTitles.title("Translate"));
        assertEquals("Công việc", NavigationPageTitles.title("Jobs"));
        assertEquals("Biên tập", NavigationPageTitles.title("Editorial"));
        assertEquals("Thư viện", NavigationPageTitles.title("Files"));
        assertEquals("Cài đặt", NavigationPageTitles.title("Settings"));
    }

    @Test public void libraryChildTitleKeepsTheParentContext() {
        assertEquals("Thư viện · RAW", NavigationPageTitles.title("RAW"));
        assertEquals("Thư viện · Glossary", NavigationPageTitles.title("Glossaries"));
        assertEquals("Thư viện · Pronoun", NavigationPageTitles.title("Pronouns"));
        assertEquals("Thư viện · Tập", NavigationPageTitles.title("ChapterSets"));
        assertEquals("Thư viện · Mẫu", NavigationPageTitles.title("Sample"));
    }
}
