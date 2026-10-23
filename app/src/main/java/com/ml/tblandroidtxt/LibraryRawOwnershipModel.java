package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure ownership calculation for the optional RAW cleanup when chapter sets are deleted. */
final class LibraryRawOwnershipModel {
    private LibraryRawOwnershipModel() { }

    static List<String> rawOnlyIds(Map<String, ? extends Collection<String>> rawIdsBySet,
                                   Collection<String> deletingSetIds,
                                   Collection<String> availableRawIds) {
        Set<String> deleting = new HashSet<>();
        if (deletingSetIds != null) deleting.addAll(deletingSetIds);
        Set<String> candidates = new HashSet<>();
        Set<String> remaining = new HashSet<>();
        if (rawIdsBySet != null) {
            for (Map.Entry<String, ? extends Collection<String>> entry : rawIdsBySet.entrySet()) {
                Collection<String> ids = entry.getValue();
                if (ids == null) continue;
                if (deleting.contains(entry.getKey())) candidates.addAll(ids);
                else remaining.addAll(ids);
            }
        }
        candidates.removeAll(remaining);
        if (availableRawIds != null) candidates.retainAll(new HashSet<>(availableRawIds));
        ArrayList<String> out = new ArrayList<>(candidates);
        java.util.Collections.sort(out);
        return out;
    }
}
