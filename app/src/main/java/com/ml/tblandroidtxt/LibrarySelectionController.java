package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Multi-select state of one Library list: whether selection mode is on and which ids are ticked, in the order they were ticked. */
final class LibrarySelectionController {
    private boolean active;
    private final Set<String> selected = new LinkedHashSet<>();

    boolean isActive() { return active; }

    /** Enters selection mode, optionally ticking the item that was long-pressed. */
    void enter(String firstId) {
        active = true;
        if (firstId != null && !firstId.isEmpty()) selected.add(firstId);
    }

    /** Leaves selection mode and forgets every tick. */
    void exit() {
        active = false;
        selected.clear();
    }

    /** Ticks or unticks one item; ignored outside selection mode. */
    void toggle(String id) {
        if (!active || id == null) return;
        if (!selected.remove(id)) selected.add(id);
    }

    boolean isSelected(String id) { return selected.contains(id); }

    int count() { return selected.size(); }

    List<String> ids() { return new ArrayList<>(selected); }

    /** "Chọn tất cả": ticks every visible id; when all are already ticked it unticks them. */
    void selectAll(Collection<String> visibleIds) {
        if (!active) return;
        if (selected.containsAll(visibleIds)) selected.removeAll(visibleIds);
        else selected.addAll(visibleIds);
    }

    /** Drops ticks of items that no longer exist (after a delete or a filter). */
    void retainOnly(Collection<String> existingIds) {
        selected.retainAll(existingIds);
    }

    String headline() { return count() + " đã chọn"; }
}
