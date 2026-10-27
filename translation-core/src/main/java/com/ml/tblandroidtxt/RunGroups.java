package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups jobs by the chapter-set run (one press of Start) that worked on them. A run is known from its snapshot file
 * ({@code translate_sessions/<id>.json}): its name, time, number of chapters and the ids of the earlier jobs it continued. A job belongs to
 * the newest run that continued it, otherwise to the run that created it (its {@code start_session_id}); that way tokens and money are
 * counted once. Jobs with no known run stay single rows. Read only: nothing is written and no schema changes.
 */
final class RunGroups {
    private RunGroups() {}

    static final class SessionInfo {
        String sessionId = "";
        String setId = "";
        String setName = "";
        long createdAt;
        int chapterCount;
        final List<Long> resumedJobs = new ArrayList<>();
    }

    static final class Entry {
        final TranslationRepositoryCore.JobSummary job;
        /** Session the job was created in when this run only continued it; empty otherwise. */
        final String continuedFromSession;
        Entry(TranslationRepositoryCore.JobSummary job, String continuedFromSession) { this.job = job; this.continuedFromSession = continuedFromSession; }
    }

    static final class Run {
        String sessionId = "";
        String setId = "";
        String setName = "";
        long createdAt;
        long lastUpdated;
        int chapterCount;
        int done;
        int failed;
        final List<Entry> chapters = new ArrayList<>();
        final UsageTotals usage = new UsageTotals();

        int notRun() { return Math.max(0, chapterCount - chapters.size()); }
        /** Chapters of this run that still need work: not finished (failed, paused or never started). */
        int unfinished() { return Math.max(0, chapterCount - done); }
        long durationMs() { return Math.max(0, lastUpdated - createdAt); }
    }

    /** Either a run or a job that belongs to no known run. */
    static final class Item {
        final Run run;
        final TranslationRepositoryCore.JobSummary single;
        Item(Run run, TranslationRepositoryCore.JobSummary single) { this.run = run; this.single = single; }
        long sortKey() { return run != null ? run.lastUpdated : single.updatedAt; }
    }

    private static boolean failed(TranslationRepositoryCore.JobSummary j) {
        JobStatus.Kind k = JobStatus.classify(j.status);
        if (k == JobStatus.Kind.FINISHED) return false;
        return j.failedChunks > 0 || "error".equalsIgnoreCase(j.status) || k == JobStatus.Kind.NEEDS_REVIEW || k == JobStatus.Kind.CONFLICT;
    }

    /** The run (Start press) a job belongs to: the newest run that continued it, else the run that created it; null when it has none. */
    static SessionInfo sessionOf(long jobId, String startSessionId, List<SessionInfo> sessions) {
        List<SessionInfo> newestFirst = new ArrayList<>(sessions);
        Collections.sort(newestFirst, new Comparator<SessionInfo>() {
            @Override public int compare(SessionInfo a, SessionInfo b) { return Long.compare(b.createdAt, a.createdAt); }
        });
        for (SessionInfo s : newestFirst) if (s.resumedJobs.contains(jobId)) return s;
        if (startSessionId != null) for (SessionInfo s : newestFirst) if (s.sessionId.equals(startSessionId)) return s;
        return null;
    }

    /** {@code jobs} newest first. Result is newest first by last activity. */
    static List<Item> build(List<TranslationRepositoryCore.JobSummary> jobs, List<SessionInfo> sessions) {
        Map<String, SessionInfo> byId = new HashMap<>();
        for (SessionInfo s : sessions) byId.put(s.sessionId, s);
        List<SessionInfo> newestFirst = new ArrayList<>(sessions);
        Collections.sort(newestFirst, new Comparator<SessionInfo>() {
            @Override public int compare(SessionInfo a, SessionInfo b) { return Long.compare(b.createdAt, a.createdAt); }
        });
        Map<String, Run> runs = new LinkedHashMap<>();
        List<Item> items = new ArrayList<>();
        for (TranslationRepositoryCore.JobSummary j : jobs) {
            String owner = null;
            for (SessionInfo s : newestFirst) if (s.resumedJobs.contains(j.id)) { owner = s.sessionId; break; }
            if (owner == null && j.startSessionId != null && byId.containsKey(j.startSessionId)) owner = j.startSessionId;
            if (owner == null) { items.add(new Item(null, j)); continue; }
            Run run = runs.get(owner);
            if (run == null) {
                SessionInfo s = byId.get(owner);
                run = new Run();
                run.sessionId = owner; run.setId = s.setId; run.setName = s.setName; run.createdAt = s.createdAt; run.chapterCount = s.chapterCount; run.lastUpdated = s.createdAt;
                runs.put(owner, run);
            }
            boolean continued = j.startSessionId != null && !j.startSessionId.isEmpty() && !owner.equals(j.startSessionId);
            run.chapters.add(new Entry(j, continued ? j.startSessionId : ""));
            run.lastUpdated = Math.max(run.lastUpdated, j.updatedAt);
            if (JobStatus.classify(j.status) == JobStatus.Kind.FINISHED) run.done++;
            else if (failed(j)) run.failed++;
            if (j.usage != null) run.usage.add(j.usage);
        }
        for (Run r : runs.values()) {
            Collections.sort(r.chapters, new Comparator<Entry>() {
                @Override public int compare(Entry a, Entry b) { return Integer.compare(a.job.preparedOrdinal, b.job.preparedOrdinal); }
            });
            items.add(new Item(r, null));
        }
        Collections.sort(items, new Comparator<Item>() {
            @Override public int compare(Item a, Item b) { return Long.compare(b.sortKey(), a.sortKey()); }
        });
        return items;
    }
}
