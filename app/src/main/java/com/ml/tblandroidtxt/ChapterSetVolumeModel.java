package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only model for one translation workspace. The chapter list comes from
 * the live set, or from the union of every valid run snapshot when the set was
 * deleted. Job state is attached to a raw id from the creating snapshot first
 * and to the stored RAW URI only as a fallback.
 */
final class ChapterSetVolumeModel {
    static final class Chapter {
        final String key;
        final String rawId;
        final String rawName;
        final int chapterNo;
        final long sourceTime;
        TranslationRepository.JobSummary job;

        Chapter(String key, String rawId, String rawName, int chapterNo, long sourceTime) {
            this.key = key;
            this.rawId = rawId;
            this.rawName = rawName;
            this.chapterNo = chapterNo;
            this.sourceTime = sourceTime;
        }

        boolean finished() { return job != null && JobStatus.classify(job.status) == JobStatus.Kind.FINISHED; }

        boolean failed() {
            if (job == null) return false;
            return job.failedChunks > 0 || "error".equalsIgnoreCase(job.status)
                    || JobStatus.is(job.status, JobStatus.Kind.NEEDS_REVIEW, JobStatus.Kind.CONFLICT);
        }

        boolean active() { return job != null && JobStatus.classify(job.status) == JobStatus.Kind.ACTIVE; }

        String statusLabel() {
            if (job == null) return "Chưa dịch";
            if (finished()) return "Xong";
            if (failed()) return "Lỗi";
            if (active()) return "Đang chạy";
            return "Đang dở";
        }
    }

    static final class Volume {
        final String setId;
        final String name;
        final boolean deleted;
        final List<Chapter> chapters;

        Volume(String setId, String name, boolean deleted, List<Chapter> chapters) {
            this.setId = setId == null ? "" : setId;
            this.name = name == null ? "" : name;
            this.deleted = deleted;
            this.chapters = Collections.unmodifiableList(new ArrayList<>(chapters));
        }

        int doneCount() {
            int count = 0;
            for (Chapter chapter : chapters) if (chapter.finished()) count++;
            return count;
        }

        int errorCount() {
            int count = 0;
            for (Chapter chapter : chapters) if (chapter.failed()) count++;
            return count;
        }

        int unfinishedCount() { return Math.max(0, chapters.size() - doneCount()); }

        boolean hasActiveJob() {
            for (Chapter chapter : chapters) if (chapter.active()) return true;
            return false;
        }

        boolean hasUsableRawForUnfinished(java.util.Set<String> availableRawIds) {
            if (availableRawIds == null || availableRawIds.isEmpty()) return false;
            for (Chapter chapter : chapters) {
                if (!chapter.finished() && availableRawIds.contains(chapter.rawId)) return true;
            }
            return false;
        }
    }

    private ChapterSetVolumeModel() {}

    static Volume build(ChapterSetStore.SetData live,
                        List<ChapterSession> sessions,
                        List<RunGroups.SessionInfo> infos,
                        List<TranslationRepository.JobSummary> jobs,
                        Map<String, String> rawNames) {
        String setId = live == null ? "" : safe(live.id);
        if (setId.isEmpty() && sessions != null) {
            for (ChapterSession session : sessions) {
                if (session != null && !safe(session.setId).isEmpty()) {
                    setId = safe(session.setId);
                    break;
                }
            }
        }
        return build(setId, live, sessions, infos, jobs, rawNames);
    }

    static Volume build(String setId,
                        ChapterSetStore.SetData live,
                        List<ChapterSession> sessions,
                        List<RunGroups.SessionInfo> infos,
                        List<TranslationRepository.JobSummary> jobs,
                        Map<String, String> rawNames) {
        String id = safe(setId);
        if (id.isEmpty() && live != null) id = safe(live.id);
        LinkedHashMap<String, Chapter> byKey = new LinkedHashMap<>();

        if (live != null) {
            for (ChapterSetStore.Row row : live.rows) {
                if (row == null) continue;
                String rawId = safe(row.rawId);
                String name = rawName(rawId, "", rawNames);
                String key = chapterKey(rawId, row.chapterNo);
                byKey.put(key, new Chapter(key, rawId, name, row.chapterNo, live.updatedAt));
            }
        } else if (sessions != null) {
            for (ChapterSession session : sessions) {
                if (session == null || !id.equals(safe(session.setId))) continue;
                for (ChapterBinding binding : session.chapters) {
                    if (binding == null) continue;
                    String rawId = safe(binding.rawId);
                    String key = chapterKey(rawId, binding.chapterNo);
                    Chapter existing = byKey.get(key);
                    if (existing == null || session.createdAt >= existing.sourceTime) {
                        String name = rawName(rawId, binding.rawName, rawNames);
                        byKey.put(key, new Chapter(key, rawId, name, binding.chapterNo, session.createdAt));
                    }
                }
            }
        }

        attachJobs(id, byKey, sessions, infos, jobs);
        ArrayList<Chapter> ordered = new ArrayList<>(byKey.values());
        Collections.sort(ordered, new Comparator<Chapter>() {
            @Override public int compare(Chapter a, Chapter b) {
                int number = ChapterNumber.compare(a.chapterNo, a.rawName, b.chapterNo, b.rawName);
                return number != 0 ? number : a.key.compareTo(b.key);
            }
        });
        String name = live == null ? newestSessionName(id, sessions) : live.name;
        return new Volume(id, name, live == null, ordered);
    }

    private static void attachJobs(String setId,
                                   Map<String, Chapter> chapters,
                                   List<ChapterSession> sessions,
                                   List<RunGroups.SessionInfo> infos,
                                   List<TranslationRepository.JobSummary> jobs) {
        if (jobs == null) return;
        for (TranslationRepository.JobSummary job : jobs) {
            if (job == null) continue;
            RunGroups.SessionInfo owner = RunGroups.sessionOf(job.id, job.startSessionId,
                    infos == null ? Collections.<RunGroups.SessionInfo>emptyList() : infos);
            // The W0 query normally already gives this set's jobs. Keep this
            // guard for callers that hand the model a wider diagnostic list;
            // a resumed job belongs to the newer run, not its old start run.
            if (owner != null && !setId.equals(safe(owner.setId))) continue;

            String rawId = snapshotRawId(job, sessions);
            if (rawId.isEmpty()) rawId = RawUri.idOf(job.inputUri);
            if (rawId.isEmpty()) continue;
            Chapter chapter = chapters.get(chapterKey(rawId, ChapterNumber.NONE));
            if (chapter == null) {
                for (Chapter candidate : chapters.values()) {
                    if (rawId.equals(candidate.rawId)) { chapter = candidate; break; }
                }
            }
            if (chapter == null) continue;
            if (chapter.job == null || job.id > chapter.job.id) chapter.job = job;
        }
    }

    private static String snapshotRawId(TranslationRepository.JobSummary job, List<ChapterSession> sessions) {
        if (sessions == null || safe(job.startSessionId).isEmpty() || job.preparedOrdinal < 0) return "";
        for (ChapterSession session : sessions) {
            if (session == null || !safe(job.startSessionId).equals(safe(session.sessionId))) continue;
            if (job.preparedOrdinal >= session.chapters.size()) return "";
            ChapterBinding binding = session.chapters.get(job.preparedOrdinal);
            return binding == null ? "" : safe(binding.rawId);
        }
        return "";
    }

    private static String newestSessionName(String setId, List<ChapterSession> sessions) {
        String name = "";
        long newest = Long.MIN_VALUE;
        if (sessions != null) for (ChapterSession session : sessions) {
            if (session == null || !setId.equals(safe(session.setId))) continue;
            if (session.createdAt >= newest) { newest = session.createdAt; name = safe(session.setName); }
        }
        return name;
    }

    private static String rawName(String rawId, String fallback, Map<String, String> rawNames) {
        if (rawNames != null) {
            String name = rawNames.get(rawId);
            if (!safe(name).isEmpty()) return safe(name);
        }
        return !safe(fallback).isEmpty() ? safe(fallback) : rawId;
    }

    private static String chapterKey(String rawId, int chapterNo) {
        return !safe(rawId).isEmpty() ? "raw:" + rawId : "chapter:" + chapterNo;
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
