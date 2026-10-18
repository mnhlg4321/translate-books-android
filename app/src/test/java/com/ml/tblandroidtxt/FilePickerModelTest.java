package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** v4.23 U1: the picker's rows, search, order, single / multiple choice and cancel, without any view. */
public class FilePickerModelTest {
    private static FilePickerModel.Item item(String id, String name, boolean suggested) {
        return new FilePickerModel.Item(id, name, "", ChapterNumber.parse(name), suggested, suggested ? "Gợi ý" : "");
    }

    private static List<FilePickerModel.Item> sample() {
        return Arrays.asList(item("a", "013_Glossary.csv", false), item("b", "9_Glossary.csv", false), item("c", "100_Glossary.csv", true),
                item("d", "Ghi chú nhân vật.csv", false), item("e", "KUMO_日本語の名前.csv", false), item("f", "002_Glossary.csv", true));
    }

    private static List<String> ids(List<FilePickerModel.Item> l) { List<String> o = new ArrayList<>(); for (FilePickerModel.Item i : l) o.add(i.id); return o; }

    @Test public void orderIsSuggestedFirstThenChapterAsNumberThenName() {
        FilePickerModel m = new FilePickerModel(true, false, sample(), Collections.<String>emptyList());
        assertEquals(Arrays.asList("f", "c", "b", "a", "d", "e"), ids(m.visible()));
    }

    @Test public void searchIgnoresCaseAndVietnameseMarksAndKeepsJapanese() {
        FilePickerModel m = new FilePickerModel(true, false, sample(), Collections.<String>emptyList());
        m.setQuery("GHI CHU");
        assertEquals(Arrays.asList("d"), ids(m.visible()));
        m.setQuery("nhân");
        assertEquals(Arrays.asList("d"), ids(m.visible()));
        m.setQuery("日本語");
        assertEquals(Arrays.asList("e"), ids(m.visible()));
        m.setQuery("日本語x");
        assertTrue(m.visible().isEmpty());
        assertEquals("d", FilePickerModel.fold("Đ"));
    }

    @Test public void quickFiltersShowSuggestedOrSelected() {
        FilePickerModel m = new FilePickerModel(true, false, sample(), Arrays.asList("a"));
        m.setFilter(FilePickerModel.Filter.SUGGESTED);
        assertEquals(Arrays.asList("f", "c"), ids(m.visible()));
        m.setFilter(FilePickerModel.Filter.SELECTED);
        assertEquals(Arrays.asList("a"), ids(m.visible()));
        m.toggle("e");
        assertEquals(Arrays.asList("a", "e"), ids(m.visible()));
        assertTrue(m.hasSuggestions());
    }

    @Test public void multipleChoiceTogglesAndSelectAllUsesTheFilter() {
        FilePickerModel m = new FilePickerModel(true, false, sample(), Collections.<String>emptyList());
        m.toggle("a"); m.toggle("b"); m.toggle("a");
        assertEquals(Arrays.asList("b"), m.selectedIds());
        assertEquals("Đã chọn 1", m.headline());
        m.setQuery("glossary");
        m.selectAllVisible();
        assertEquals(4, m.selectedCount());
        assertFalse("rows hidden by the filter are not ticked", m.isSelected("e"));
        m.clearAll();
        assertEquals(0, m.selectedCount());
        assertTrue("a multiple picker may confirm an empty choice", m.canConfirm());
    }

    @Test public void singleChoiceKeepsOneRowAndNeedsAChoiceToConfirm() {
        FilePickerModel m = new FilePickerModel(false, true, sample(), Collections.<String>emptyList());
        assertTrue(m.allowsNone());
        assertFalse(m.canConfirm());
        assertEquals("Chưa chọn", m.headline());
        m.toggle("a"); m.toggle("c");
        assertEquals(Arrays.asList("c"), m.selectedIds());
        assertEquals("Đã chọn: 100_Glossary.csv", m.headline());
        assertTrue(m.canConfirm());
        m.toggle(FilePickerModel.NONE_ID);
        assertEquals("Đã chọn: Không dùng", m.headline());
        m.selectAllVisible();
        assertEquals("select all does nothing in single choice", 1, m.selectedCount());
    }

    @Test public void cancelGivesBackTheSelectionTheSheetOpenedWith() {
        FilePickerModel m = new FilePickerModel(true, false, sample(), Arrays.asList("a", "b"));
        m.toggle("a"); m.toggle("c");
        assertEquals(Arrays.asList("b", "c"), m.selectedIds());
        m.cancel();
        assertEquals(Arrays.asList("a", "b"), m.selectedIds());
        FilePickerModel single = new FilePickerModel(false, true, sample(), Arrays.asList("d"));
        single.toggle("e");
        single.cancel();
        assertEquals(Arrays.asList("d"), single.selectedIds());
        assertEquals("a reopened sheet shows what was chosen", "d", single.selectedIds().get(0));
    }

    @Test public void singleChoiceOpenedWithSeveralKeepsOnlyTheFirst() {
        FilePickerModel m = new FilePickerModel(false, false, sample(), Arrays.asList("b", "a"));
        assertEquals(Arrays.asList("b"), m.selectedIds());
    }
}
