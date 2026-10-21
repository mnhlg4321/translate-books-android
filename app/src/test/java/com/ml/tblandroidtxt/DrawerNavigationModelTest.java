package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class DrawerNavigationModelTest {
    @Test public void drawerHasExactlyFourTopLevelDestinationsInOrder() {
        List<DrawerNavigationModel.Entry> entries = DrawerNavigationModel.destinations();

        assertEquals(4, entries.size());
        assertEquals("Translate", entries.get(0).target);
        assertEquals("Jobs", entries.get(1).target);
        assertEquals("Editorial", entries.get(2).target);
        assertEquals("Library", entries.get(3).target);
        assertEquals("Dịch", entries.get(0).label);
        assertEquals("Công việc", entries.get(1).label);
        assertEquals("Biên tập", entries.get(2).label);
        assertEquals("Thư viện", entries.get(3).label);
    }

    @Test public void libraryDestinationStaysSelectedOnEveryLibraryPage() {
        assertTrue(DrawerNavigationModel.isSelected("Library", "Files"));
        assertTrue(DrawerNavigationModel.isSelected("Library", "RAW"));
        assertTrue(DrawerNavigationModel.isSelected("Library", "Glossaries"));
        assertTrue(DrawerNavigationModel.isSelected("Library", "Pronouns"));
        assertTrue(DrawerNavigationModel.isSelected("Library", "ChapterSets"));
        assertTrue(DrawerNavigationModel.isSelected("Library", "Sample"));
        assertFalse(DrawerNavigationModel.isSelected("Library", "Jobs"));
        assertTrue(DrawerNavigationModel.isSelected("Jobs", "Jobs"));
    }
}
