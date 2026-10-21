package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Merges the capped general job cache with the jobs belonging to chapter-set sessions. */
final class JobListMerge {
    private JobListMerge() {}

    /**
     * The workspace snapshot is the second source on purpose: when timestamps tie, it wins.
     * This keeps the snapshot's richer session-backed row for the same job id.
     */
    static List<TranslationRepository.JobSummary> all(
            List<TranslationRepository.JobSummary> cached,
            List<TranslationRepository.JobSummary> workspace) {
        Map<Long, TranslationRepository.JobSummary> byId = new HashMap<>();
        add(byId, cached, false);
        add(byId, workspace, true);
        ArrayList<TranslationRepository.JobSummary> out = new ArrayList<>(byId.values());
        Collections.sort(out, new Comparator<TranslationRepository.JobSummary>() {
            @Override public int compare(TranslationRepository.JobSummary left,
                                          TranslationRepository.JobSummary right) {
                int updated = Long.compare(right.updatedAt, left.updatedAt);
                return updated != 0 ? updated : Long.compare(right.id, left.id);
            }
        });
        return out;
    }

    private static void add(Map<Long, TranslationRepository.JobSummary> byId,
                            List<TranslationRepository.JobSummary> source,
                            boolean preferOnTie) {
        if (source == null) return;
        for (TranslationRepository.JobSummary candidate : source) {
            if (candidate == null) continue;
            TranslationRepository.JobSummary current = byId.get(candidate.id);
            if (current == null || candidate.updatedAt > current.updatedAt
                    || (preferOnTie && candidate.updatedAt == current.updatedAt)) {
                byId.put(candidate.id, candidate);
            }
        }
    }
}
