package com.ml.tblandroidtxt;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * State of the shared file picker sheet, without any view: the rows, the search text and quick filter, single or multiple choice,
 * and the selection that is only kept when the owner confirms. Cancelling gives back the selection the sheet was opened with.
 */
final class FilePickerModel {
    enum Filter { ALL, SUGGESTED, SELECTED }

    /** The id used for the "Không dùng" row of a single-choice picker. */
    static final String NONE_ID = "";

    static final class Item {
        final String id;
        final String name;
        final String meta;
        final int chapterNo;
        final boolean suggested;
        final String suggestedLabel;

        Item(String id, String name, String meta, int chapterNo, boolean suggested, String suggestedLabel) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.meta = meta == null ? "" : meta;
            this.chapterNo = chapterNo;
            this.suggested = suggested;
            this.suggestedLabel = suggestedLabel == null ? "" : suggestedLabel;
        }
    }

    private final boolean multiple;
    private final boolean allowNone;
    private final List<Item> items = new ArrayList<>();
    private final LinkedHashSet<String> original = new LinkedHashSet<>();
    private final LinkedHashSet<String> selected = new LinkedHashSet<>();
    private String query = "";
    private Filter filter = Filter.ALL;

    /** {@code allowNone}: single choice only, adds a "Không dùng" row whose id is {@link #NONE_ID}. */
    FilePickerModel(boolean multiple, boolean allowNone, List<Item> items, Collection<String> initiallySelected) {
        this.multiple = multiple;
        this.allowNone = allowNone && !multiple;
        this.items.addAll(items);
        for (String id : initiallySelected) { original.add(id); selected.add(id); }
        if (!multiple && selected.size() > 1) { String first = selected.iterator().next(); selected.clear(); selected.add(first); }
    }

    boolean isMultiple() { return multiple; }

    boolean allowsNone() { return allowNone; }

    void setQuery(String text) { query = text == null ? "" : text; }

    void setFilter(Filter f) { filter = f == null ? Filter.ALL : f; }

    Filter filter() { return filter; }

    boolean hasSuggestions() { for (Item i : items) if (i.suggested) return true; return false; }

    /** Lower case with Vietnamese marks removed (đ becomes d); Japanese text is left as it is. */
    static String fold(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{Mn}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }

    /** Rows to show: filtered, then suggested first, chapter number as a number, then name. */
    List<Item> visible() {
        String q = fold(query.trim());
        ArrayList<Item> out = new ArrayList<>();
        for (Item i : items) {
            if (!q.isEmpty() && !fold(i.name).contains(q)) continue;
            if (filter == Filter.SUGGESTED && !i.suggested) continue;
            if (filter == Filter.SELECTED && !selected.contains(i.id)) continue;
            out.add(i);
        }
        Collections.sort(out, (a, b) -> {
            if (a.suggested != b.suggested) return a.suggested ? -1 : 1;
            return ChapterNumber.compare(a.chapterNo, a.name, b.chapterNo, b.name);
        });
        return out;
    }

    boolean isSelected(String id) { return selected.contains(id); }

    /** Single choice: the row becomes the only selected one. Multiple choice: the row is ticked or unticked. */
    void toggle(String id) {
        if (multiple) {
            if (!selected.remove(id)) selected.add(id);
        } else {
            selected.clear();
            selected.add(id);
        }
    }

    /** "Chọn tất cả (đang lọc)": ticks every visible row; multiple choice only. */
    void selectAllVisible() {
        if (!multiple) return;
        for (Item i : visible()) selected.add(i.id);
    }

    void clearAll() { selected.clear(); }

    List<String> selectedIds() { return new ArrayList<>(selected); }

    int selectedCount() { return selected.size(); }

    /** The confirm button is off in a single-choice picker until something (even "Không dùng") is chosen. */
    boolean canConfirm() { return multiple || !selected.isEmpty(); }

    /** Header line: "Đã chọn n" for multiple choice, the chosen name for single choice. */
    String headline() {
        if (multiple) return "Đã chọn " + selected.size();
        if (selected.isEmpty()) return "Chưa chọn";
        String id = selected.iterator().next();
        if (id.equals(NONE_ID)) return "Đã chọn: Không dùng";
        for (Item i : items) if (i.id.equals(id)) return "Đã chọn: " + i.name;
        return "Đã chọn 1";
    }

    /** Cancel: the selection goes back to what it was when the picker opened. */
    void cancel() {
        selected.clear();
        selected.addAll(original);
    }

    Set<String> originalIds() { return new LinkedHashSet<>(original); }

    String nameOf(String id) {
        for (Item i : items) if (i.id.equals(id)) return i.name;
        return id;
    }
}
