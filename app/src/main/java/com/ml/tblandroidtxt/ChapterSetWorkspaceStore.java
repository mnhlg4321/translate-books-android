package com.ml.tblandroidtxt;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only, off-UI-thread source for the workspace navigator. It deliberately
 * does not use MainActivity.cachedJobSummaries: that cache is capped at 150.
 */
final class ChapterSetWorkspaceStore {
    static final class Snapshot {
        final List<ChapterSetStore.SetInfo> liveSets;
        final Map<String, ChapterSetStore.SetData> liveData;
        final List<ChapterSession> sessions;
        final List<RunGroups.SessionInfo> sessionInfos;
        final List<TranslationRepository.JobSummary> jobs;
        final List<WorkspaceSetModel.Entry> sets;
        final Map<String, String> rawNames;

        Snapshot(List<ChapterSetStore.SetInfo> liveSets,
                 Map<String, ChapterSetStore.SetData> liveData,
                 List<ChapterSession> sessions,
                 List<RunGroups.SessionInfo> sessionInfos,
                 List<TranslationRepository.JobSummary> jobs,
                 List<WorkspaceSetModel.Entry> sets,
                 Map<String, String> rawNames) {
            this.liveSets = Collections.unmodifiableList(new ArrayList<>(liveSets));
            this.liveData = Collections.unmodifiableMap(new HashMap<>(liveData));
            this.sessions = Collections.unmodifiableList(new ArrayList<>(sessions));
            this.sessionInfos = Collections.unmodifiableList(new ArrayList<>(sessionInfos));
            this.jobs = Collections.unmodifiableList(new ArrayList<>(jobs));
            this.sets = Collections.unmodifiableList(new ArrayList<>(sets));
            this.rawNames = Collections.unmodifiableMap(new HashMap<>(rawNames));
        }

        List<TranslationRepository.JobSummary> jobsForSet(String setId) {
            return WorkspaceSetModel.jobsForSet(jobs, sessionInfos, setId);
        }

        WorkspaceSetModel.Entry set(String setId) {
            if (setId == null) return null;
            for (WorkspaceSetModel.Entry e : sets) if (setId.equals(e.id)) return e;
            return null;
        }

        String setIdOfJob(long jobId) {
            for (TranslationRepository.JobSummary job : jobs) if (job.id == jobId) {
                RunGroups.SessionInfo owner = RunGroups.sessionOf(job.id, job.startSessionId, sessionInfos);
                return owner == null ? "" : safe(owner.setId);
            }
            return "";
        }
    }

    private ChapterSetWorkspaceStore() {}

    static Snapshot read(Context context) {
        List<ChapterSetStore.SetInfo> live = ChapterSetStore.listSets(context);
        Map<String, ChapterSetStore.SetData> liveData = new HashMap<>();
        ArrayList<String> liveIds = new ArrayList<>();
        for (ChapterSetStore.SetInfo info : live) {
            if (info == null || blank(info.id)) continue;
            liveIds.add(info.id);
            ChapterSetStore.SetData data = ChapterSetStore.get(context, info.id);
            if (data != null) liveData.put(info.id, data);
        }

        File dir = new File(context.getFilesDir(), ChapterSession.DIR);
        List<RunGroups.SessionInfo> infos = ChapterSession.readInfos(dir);
        List<ChapterSession> sessions = readValidSessions(dir);

        Set<String> sessionIds = new HashSet<>();
        Set<Long> resumedJobs = new HashSet<>();
        for (RunGroups.SessionInfo info : infos) {
            if (info == null || blank(info.sessionId)) continue;
            sessionIds.add(info.sessionId);
            resumedJobs.addAll(info.resumedJobs);
        }
        TranslationRepository repo = new TranslationRepository(context);
        List<TranslationRepository.JobSummary> jobs;
        try {
            jobs = repo.getJobSummariesForSessions(new ArrayList<>(sessionIds), new ArrayList<>(resumedJobs));
        } finally {
            repo.close();
        }
        List<WorkspaceSetModel.Entry> sets = WorkspaceSetModel.merge(live, infos, WorkspaceSetModel.readLastOpened(context));
        Map<String, String> rawNames = new HashMap<>();
        for (LibraryRawStore.Item item : LibraryRawStore.list(context)) {
            if (item == null || blank(item.id)) continue;
            rawNames.put(item.id, blank(item.name) ? item.originalName : item.name);
        }
        applyVolumeStats(sets, liveData, sessions, infos, jobs, rawNames);
        return new Snapshot(live, liveData, sessions, infos, jobs, sets, rawNames);
    }

    private static List<ChapterSession> readValidSessions(File dir) {
        ArrayList<ChapterSession> out = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return out;
        for (File file : files) {
            String name = file.getName();
            if (!name.endsWith(".json")) continue;
            String id = name.substring(0, name.length() - 5);
            try { out.add(ChapterSession.read(dir, id)); } catch (Exception ignored) { }
        }
        return out;
    }

    private static void applyVolumeStats(List<WorkspaceSetModel.Entry> sets,
                                          Map<String, ChapterSetStore.SetData> liveData,
                                          List<ChapterSession> sessions,
                                          List<RunGroups.SessionInfo> infos,
                                          List<TranslationRepository.JobSummary> jobs,
                                          Map<String, String> rawNames) {
        for (WorkspaceSetModel.Entry entry : sets) {
            List<TranslationRepository.JobSummary> owned = WorkspaceSetModel.jobsForSet(jobs, infos, entry.id);
            ChapterSetVolumeModel.Volume volume = ChapterSetVolumeModel.build(
                    entry.id, liveData.get(entry.id), sessions, infos, owned, rawNames);
            entry.chapterCount = volume.chapters.size();
            entry.done = volume.doneCount();
            entry.failed = volume.errorCount();
        }
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static String safe(String value) { return value == null ? "" : value; }
}
