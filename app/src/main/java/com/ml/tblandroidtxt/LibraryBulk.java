package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** List arithmetic for bulk delete and its undo: remove many items once, put exactly those items back at their old places. */
final class LibraryBulk {
    interface IdOf<T> { String id(T item); }

    static final class Removed<T> {
        final int index;
        final T item;
        Removed(int index, T item) { this.index = index; this.item = item; }
    }

    static final class Result<T> {
        final List<T> remaining = new ArrayList<>();
        final List<Removed<T>> removed = new ArrayList<>();
    }

    static <T> Result<T> remove(List<T> list, Set<String> ids, IdOf<T> idOf) {
        Result<T> out = new Result<>();
        for (int i = 0; i < list.size(); i++) {
            T item = list.get(i);
            if (ids.contains(idOf.id(item))) out.removed.add(new Removed<>(i, item));
            else out.remaining.add(item);
        }
        return out;
    }

    /**
     * Undo: puts the removed items back with their old ids at their old positions (clamped), skipping any id that
     * is in the list again; items added meanwhile are kept.
     */
    static <T> List<T> reinsert(List<T> current, List<Removed<T>> removed, IdOf<T> idOf) {
        ArrayList<T> out = new ArrayList<>(current);
        Set<String> present = new HashSet<>();
        for (T item : current) present.add(idOf.id(item));
        for (Removed<T> r : removed) {
            if (!present.add(idOf.id(r.item))) continue;
            out.add(Math.min(Math.max(0, r.index), out.size()), r.item);
        }
        return out;
    }

    private LibraryBulk() {}
}
