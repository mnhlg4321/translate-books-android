package com.ml.tblandroidtxt;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only model of the translation-side workspaces. A workspace is keyed by
 * the persisted set id, not by its current Library row: a saved run keeps a
 * deleted set visible.
 */
final class WorkspaceSetModel {
    static final String PREF_LAST_OPENED = "workspace_last_opened";

    static final class Entry {
        String id = "";
        String name = "";
        boolean deleted;
        int chapterCount;
        int done;
        int failed;
        long updatedAt;
        long lastRunAt;
        long lastOpenedAt;
        String latestSessionId = "";
        final List<String> sessionIds = new ArrayList<>();

        long sortKey() { return Math.max(lastOpenedAt, Math.max(updatedAt, lastRunAt)); }
        int unfinished() { return Math.max(0, chapterCount - done); }
        String displayName() { return name == null || name.trim().isEmpty() ? "Tập không tên" : name.trim(); }
        String label() { return deleted ? displayName() + " (đã xóa)" : displayName(); }
    }

    private WorkspaceSetModel() {}

    /** Merges live Library rows with every saved run, without duplicating a set id. */
    static List<Entry> merge(List<ChapterSetStore.SetInfo> liveSets,
                             List<RunGroups.SessionInfo> sessions,
                             Map<String, Long> lastOpened) {
        Map<String, Entry> byId = new HashMap<>();
        if (liveSets != null) for (ChapterSetStore.SetInfo live : liveSets) {
            if (live == null || blank(live.id)) continue;
            Entry e = byId.get(live.id);
            if (e == null) { e = new Entry(); e.id = live.id; byId.put(e.id, e); }
            e.name = nonEmpty(live.name, e.name);
            e.deleted = false;
            e.chapterCount = Math.max(e.chapterCount, live.chapters);
            e.updatedAt = Math.max(e.updatedAt, live.updatedAt);
        }
        if (sessions != null) for (RunGroups.SessionInfo session : sessions) {
            if (session == null || blank(session.setId)) continue;
            Entry e = byId.get(session.setId);
            if (e == null) {
                e = new Entry();
                e.id = session.setId;
                e.deleted = true;
                byId.put(e.id, e);
            }
            // A live Library row is the current name; a saved run supplies the
            // name only for a deleted set (or when the live row has no name).
            if (e.deleted || blank(e.name)) e.name = nonEmpty(session.setName, e.name);
            e.chapterCount = Math.max(e.chapterCount, session.chapterCount);
            if (!e.sessionIds.contains(session.sessionId)) e.sessionIds.add(session.sessionId);
            if (session.createdAt >= e.lastRunAt) {
                e.lastRunAt = session.createdAt;
                e.latestSessionId = session.sessionId;
            }
        }
        if (lastOpened != null) for (Map.Entry<String, Long> opened : lastOpened.entrySet()) {
            Entry e = byId.get(opened.getKey());
            if (e != null && opened.getValue() != null) e.lastOpenedAt = Math.max(0L, opened.getValue());
        }
        ArrayList<Entry> out = new ArrayList<>(byId.values());
        Collections.sort(out, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) {
                int byName = a.displayName().compareToIgnoreCase(b.displayName());
                return byName != 0 ? byName : a.id.compareTo(b.id);
            }
        });
        return out;
    }

    /** Most recently used workspaces, with one entry per set and a hard cap. */
    static List<Entry> recent(List<Entry> all, int limit) {
        ArrayList<Entry> out = new ArrayList<>();
        if (all != null) out.addAll(all);
        Collections.sort(out, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) {
                int byTime = Long.compare(b.sortKey(), a.sortKey());
                return byTime != 0 ? byTime : a.displayName().compareToIgnoreCase(b.displayName());
            }
        });
        if (out.size() > Math.max(0, limit)) return new ArrayList<>(out.subList(0, Math.max(0, limit)));
        return out;
    }

    /** Jobs are owned by the newest run that resumed them, matching the Jobs page grouping rule. */
    static List<TranslationRepository.JobSummary> jobsForSet(List<TranslationRepository.JobSummary> jobs,
                                                               List<RunGroups.SessionInfo> sessions,
                                                               String setId) {
        ArrayList<TranslationRepository.JobSummary> out = new ArrayList<>();
        if (jobs == null || blank(setId)) return out;
        for (TranslationRepository.JobSummary job : jobs) {
            if (job == null) continue;
            RunGroups.SessionInfo owner = RunGroups.sessionOf(job.id, job.startSessionId, sessions == null ? Collections.<RunGroups.SessionInfo>emptyList() : sessions);
            if (owner != null && setId.equals(owner.setId)) out.add(job);
        }
        return out;
    }

    static Map<String, Long> readLastOpened(Context context) {
        Map<String, ?> raw = context.getSharedPreferences(PREF_LAST_OPENED, Context.MODE_PRIVATE).getAll();
        Map<String, Long> out = new HashMap<>();
        for (Map.Entry<String, ?> e : raw.entrySet()) if (e.getValue() instanceof Long) out.put(e.getKey(), (Long)e.getValue());
        return out;
    }

    static void recordOpened(Context context, String setId) {
        if (blank(setId)) return;
        context.getSharedPreferences(PREF_LAST_OPENED, Context.MODE_PRIVATE).edit()
                .putLong(setId, System.currentTimeMillis()).apply();
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static String nonEmpty(String value, String fallback) { return blank(value) ? (fallback == null ? "" : fallback) : value.trim(); }
}
