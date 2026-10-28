package com.ml.tblandroidtxt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Job history of the PC: pages by job id (newest first), chapters of one run folded into one entry, a status filter, and a detail with
 * numbers only (chunk counts, cost, request attempts). No chunk text is ever read here.
 */
public final class DesktopHistory {
    public static final String ALL = "all", DONE = "done", FAILED = "failed", UNFINISHED = "unfinished";

    /** One line of the list: a single job, or a run with its chapters. */
    public static final class Entry {
        public final boolean run;
        public final String sessionId;
        public final String title;
        public final String statusText;
        public final String progressText;
        public final String costText;
        public final long updatedAt;
        public final List<TranslationRepositoryCore.JobSummary> jobs;

        Entry(boolean run, String sessionId, String title, String statusText, String progressText, String costText, long updatedAt,
              List<TranslationRepositoryCore.JobSummary> jobs) {
            this.run = run; this.sessionId = sessionId; this.title = title; this.statusText = statusText; this.progressText = progressText;
            this.costText = costText; this.updatedAt = updatedAt; this.jobs = jobs;
        }
    }

    public static final class Page {
        public final List<Entry> entries;
        /** Pass as {@code beforeId} for the next page; -1 when there is none. */
        public final long nextCursor;
        public final boolean hasMore;
        Page(List<Entry> entries, long nextCursor, boolean hasMore) { this.entries = entries; this.nextCursor = nextCursor; this.hasMore = hasMore; }
    }

    public static final class Detail {
        public final TranslationRepositoryCore.JobSummary job;
        public final Map<String, Integer> chunkStatuses;
        public final Map<String, Integer> finishReasons;
        public final List<TranslationRepositoryCore.JobAttemptRow> attempts;
        public final Path outputFile;
        Detail(TranslationRepositoryCore.JobSummary job, Map<String, Integer> chunkStatuses, Map<String, Integer> finishReasons,
               List<TranslationRepositoryCore.JobAttemptRow> attempts, Path outputFile) {
            this.job = job; this.chunkStatuses = chunkStatuses; this.finishReasons = finishReasons; this.attempts = attempts; this.outputFile = outputFile;
        }
    }

    private final DesktopRepository repo;
    private final Path sessionsDir;

    DesktopHistory(DesktopRepository repo, Path sessionsDir) {
        this.repo = repo;
        this.sessionsDir = sessionsDir;
    }

    public static String statusText(String raw) {
        switch (JobStatus.classify(raw)) {
            case FINISHED: return "Hoàn tất";
            case ACTIVE: return "Đang chạy";
            case CONTINUABLE: return "Dừng, có thể tiếp tục";
            case NEEDS_REVIEW: return "Cần xem";
            case CONFLICT: return "Tệp nguồn đã đổi";
            case CANCELLED: return "Đã hủy";
            case DISMISSED: return "Đã ẩn";
            default: return raw == null ? "" : raw;
        }
    }

    private static boolean failed(TranslationRepositoryCore.JobSummary j) {
        JobStatus.Kind k = JobStatus.classify(j.status);
        if (k == JobStatus.Kind.FINISHED) return false;
        return j.failedChunks > 0 || "error".equalsIgnoreCase(j.status) || k == JobStatus.Kind.NEEDS_REVIEW || k == JobStatus.Kind.CONFLICT;
    }

    /** Jobs the "Đang làm" tab lists: running, stoppable-and-continuable, or needing a look. */
    public static boolean isUnfinished(TranslationRepositoryCore.JobSummary j) { return unfinished(j); }

    private static boolean unfinished(TranslationRepositoryCore.JobSummary j) {
        return JobStatus.is(j.status, JobStatus.Kind.ACTIVE, JobStatus.Kind.CONTINUABLE, JobStatus.Kind.NEEDS_REVIEW);
    }

    private static String cost(UsageTotals u) {
        if (u == null || u.calls == 0) return "";
        return u.unknownPriceCalls > 0 && u.costUsd == 0 ? "chưa có giá" : CostEstimator.money(u.costUsd);
    }

    /**
     * One page of history. {@code shownSessions} are the runs already shown by earlier pages (and are added to when this page shows
     * more), so loading more never lists the same run twice.
     */
    public Page page(long beforeId, String filter, Set<String> shownSessions) {
        List<TranslationRepositoryCore.JobSummary> raw = repo.getJobSummariesPage(beforeId, JobHistoryPageModel.PAGE_SIZE, null, null);
        long next = JobHistoryPageModel.nextCursor(raw);
        boolean more = JobHistoryPageModel.hasMore(raw);
        List<RunGroups.SessionInfo> sessions = ChapterSession.readInfos(sessionsDir.toFile());
        Set<String> known = new HashSet<>();
        for (RunGroups.SessionInfo s : sessions) known.add(s.sessionId);
        List<String> wanted = new ArrayList<>();
        List<TranslationRepositoryCore.JobSummary> jobs = new ArrayList<>(raw);
        Set<Long> have = new HashSet<>();
        for (TranslationRepositoryCore.JobSummary j : raw) have.add(j.id);
        for (TranslationRepositoryCore.JobSummary j : raw) {
            if (j.startSessionId != null && known.contains(j.startSessionId) && !wanted.contains(j.startSessionId)) wanted.add(j.startSessionId);
        }
        if (!wanted.isEmpty()) {
            List<Long> resumed = new ArrayList<>();
            for (RunGroups.SessionInfo s : sessions) if (wanted.contains(s.sessionId)) resumed.addAll(s.resumedJobs);
            for (TranslationRepositoryCore.JobSummary j : repo.getJobSummariesForSessions(wanted, resumed)) if (have.add(j.id)) jobs.add(j);
        }
        List<Entry> out = new ArrayList<>();
        for (RunGroups.Item item : RunGroups.build(jobs, sessions)) {
            if (item.run != null) {
                RunGroups.Run run = item.run;
                if (shownSessions.contains(run.sessionId)) continue;
                boolean isDone = run.unfinished() == 0;
                if (!matches(filter, isDone, run.failed > 0, !isDone)) continue;
                shownSessions.add(run.sessionId);
                List<TranslationRepositoryCore.JobSummary> chapters = new ArrayList<>();
                for (RunGroups.Entry e : run.chapters) chapters.add(e.job);
                String status = isDone ? "Hoàn tất" : run.failed > 0 ? "Có chương lỗi" : "Chưa xong";
                String progress = run.done + "/" + run.chapterCount + " chương xong" + (run.failed > 0 ? " · " + run.failed + " lỗi" : "");
                String title = (run.setName == null || run.setName.isEmpty() ? "Lượt dịch cả tập" : run.setName) + " · " + run.chapterCount + " chương";
                out.add(new Entry(true, run.sessionId, title, status, progress, cost(run.usage), run.lastUpdated, chapters));
            } else {
                TranslationRepositoryCore.JobSummary j = item.single;
                if (!matches(filter, JobStatus.is(j.status, JobStatus.Kind.FINISHED), failed(j), unfinished(j))) continue;
                List<TranslationRepositoryCore.JobSummary> one = new ArrayList<>();
                one.add(j);
                out.add(new Entry(false, "", j.fileName, statusText(j.status), j.doneChunks + "/" + j.totalChunks + " chunk" + (j.failedChunks > 0 ? " · " + j.failedChunks + " lỗi" : ""),
                        cost(j.usage), j.updatedAt, one));
            }
        }
        return new Page(out, next, more);
    }

    private static boolean matches(String filter, boolean isDone, boolean isFailed, boolean isUnfinished) {
        if (filter == null || ALL.equals(filter)) return true;
        if (DONE.equals(filter)) return isDone;
        if (FAILED.equals(filter)) return isFailed;
        return UNFINISHED.equals(filter) && isUnfinished;
    }

    /** Numbers for one job; null when it does not exist. */
    public Detail detail(long jobId) {
        TranslationRepositoryCore.JobSummary job = repo.getJobSummary(jobId);
        if (job == null) return null;
        Map<String, Integer> statuses = new java.util.TreeMap<>();
        Map<String, Integer> reasons = new java.util.TreeMap<>();
        for (TranslationRepositoryCore.ChunkRow r : repo.getChunkRows(jobId)) {
            statuses.merge(String.valueOf(r.status), 1, Integer::sum);
            for (TranslationRepositoryCore.AttemptRow a : repo.getAttemptRows(jobId, r.index)) {
                reasons.merge(a.finishReason == null || a.finishReason.isEmpty() ? "(none)" : a.finishReason, 1, Integer::sum);
            }
        }
        Path output = null;
        try {
            if (job.outputUri != null && job.outputUri.startsWith("file:")) output = PathDocumentIO.pathOf(job.outputUri);
        } catch (RuntimeException ignored) { }
        return new Detail(job, statuses, reasons, repo.getJobAttempts(jobId), output);
    }
}
