package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure drawer model that mixes chapter-set runs and single-chapter jobs. */
final class DrawerRecentModel {
    static final class Item {
        final boolean set;
        final String id;
        final String label;
        final long jobId;
        final long sortKey;
        final boolean active;

        Item(boolean set, String id, String label, long jobId, long sortKey, boolean active) {
            this.set = set;
            this.id = id == null ? "" : id;
            this.label = label == null ? "" : label;
            this.jobId = jobId;
            this.sortKey = sortKey;
            this.active = active;
        }
    }

    private DrawerRecentModel() {}

    static List<Item> build(List<WorkspaceSetModel.Entry> sets,
                            List<TranslationRepository.JobSummary> jobs,
                            List<RunGroups.SessionInfo> sessions,
                            String activeSetId, long activeJobId, int limit) {
        Map<String, Item> bySet = new HashMap<>();
        if (sets != null) for (WorkspaceSetModel.Entry entry : sets) {
            if (entry == null || entry.id == null || entry.id.isEmpty()) continue;
            bySet.put(entry.id, new Item(true, entry.id, entry.label(), -1L,
                    entry.sortKey(), entry.id.equals(activeSetId)));
        }
        Map<Long, Item> singles = new HashMap<>();
        for (RunGroups.Item group : RunGroups.build(jobs, sessions == null
                ? Collections.<RunGroups.SessionInfo>emptyList() : sessions)) {
            if (group.run != null && group.run.setId != null && !group.run.setId.isEmpty()) {
                Item old = bySet.get(group.run.setId);
                long key = Math.max(group.run.lastUpdated, old == null ? 0L : old.sortKey);
                String label = group.run.setName == null || group.run.setName.isEmpty()
                        ? (old == null ? "Tập không tên" : old.label) : group.run.setName;
                bySet.put(group.run.setId, new Item(true, group.run.setId, label, -1L,
                        key, group.run.setId.equals(activeSetId)));
            } else if (group.single != null) {
                TranslationRepository.JobSummary job = group.single;
                singles.put(job.id, new Item(false, String.valueOf(job.id),
                        safeName(job.fileName), job.id, job.updatedAt, job.id == activeJobId));
            }
        }
        ArrayList<Item> out = new ArrayList<>();
        out.addAll(bySet.values());
        out.addAll(singles.values());
        Collections.sort(out, new Comparator<Item>() {
            @Override public int compare(Item left, Item right) {
                if (left.active != right.active) return left.active ? -1 : 1;
                int byTime = Long.compare(right.sortKey, left.sortKey);
                if (byTime != 0) return byTime;
                return left.label.compareToIgnoreCase(right.label);
            }
        });
        int keep = Math.max(0, limit);
        return out.size() <= keep ? out : new ArrayList<>(out.subList(0, keep));
    }

    private static String safeName(String value) {
        return value == null || value.trim().isEmpty() ? "Tệp TXT" : value.trim();
    }
}
