package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;

/** Lightweight user-facing Jobs page. Developer and QA tools live in Settings. */
final class JobsMainPageFactory {
    private final MainActivity a;
    /** Runs the owner opened; kept across page rebuilds so a refresh does not fold them again. */
    private static final Set<String> EXPANDED = new HashSet<>();

    JobsMainPageFactory(MainActivity activity) { a = activity; }

    View build() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        EditorialJobCard.addJobs(a, root); // Editorial runs next to the translation jobs (4.22)
        root.addView(workspaceFilter(), a.marginLP(-1, -2, 0, 0, 0, 12));
        List<TranslationRepository.JobSummary> jobs = a.filteredWorkspaceJobsSnapshot();

        root.addView(totalsCard());

        boolean serviceActive = TranslatorService.isActive();
        long activeJobId = -1L;
        if (serviceActive) {
            Intent runtime = RuntimeStateStore.toIntent(a);
            if (runtime != null) activeJobId = runtime.getLongExtra(TranslatorService.EXTRA_ACTIVE_JOB_ID, -1L);
        }
        TranslationRepository.JobSummary current = JobPicker.current(jobs, serviceActive, activeJobId);

        LinearLayout currentSection = a.sectionCard("▶", "Current job");
        if (current == null) currentSection.addView(a.text("Không có job đang chạy hay đang dở.", 13, a.MUTED, false));
        else currentSection.addView(currentCard(current, serviceActive && current.id == activeJobId));
        LinearLayout refresh = a.rowContainer();
        refresh.addView(a.primaryButton("↻ Refresh", v -> a.refreshJobCacheAsync(true)), new LinearLayout.LayoutParams(0, -2, 1));
        refresh.addView(a.space(8, 1));
        refresh.addView(a.secondaryButton("Resume last", v -> a.resumeCheckpoint()), new LinearLayout.LayoutParams(0, -2, 1));
        currentSection.addView(refresh, a.marginLP(-1, -2, 0, 10, 0, 0));
        root.addView(currentSection);

        LinearLayout recent = a.sectionCard("◷", "Recent jobs");
        List<RunGroups.Item> items = RunGroups.build(jobs, a.sessionInfosSnapshot());
        int count = 0;
        for (RunGroups.Item item : items) {
            recent.addView(item.run != null ? runCard(item.run) : recentRow(item.single, false), a.marginLP(-1, -2, 0, 0, 0, 8));
            if (++count == 30) break;
        }
        if (count == 0) recent.addView(a.text("No recent jobs.", 13, a.MUTED, false));
        root.addView(recent);
        return scroll;
    }

    private View workspaceFilter() {
        LinearLayout box = a.card(14, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        TextView label = a.text("Lọc Công việc theo Tập", 13, a.TEXT, true);
        box.addView(label, a.marginLP(-1, -2, 0, 0, 0, 6));
        Button pick = a.secondaryButton("Tập: " + a.workspaceFilterLabel(), v -> chooseWorkspaceFilter());
        pick.setContentDescription("Lọc Công việc theo Tập");
        box.addView(pick, new LinearLayout.LayoutParams(-1, -2));
        return box;
    }

    private void chooseWorkspaceFilter() {
        List<WorkspaceSetModel.Entry> sets = a.workspaceSetsSnapshot();
        ArrayList<String> labels = new ArrayList<>();
        labels.add("Tất cả");
        for (WorkspaceSetModel.Entry e : sets) labels.add(e.label());
        new AlertDialog.Builder(a).setTitle("Lọc Công việc theo Tập")
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    if (which == 0) a.setJobsSetFilter("");
                    else if (which - 1 < sets.size()) a.setJobsSetFilter(sets.get(which - 1).id);
                }).setNegativeButton("Hủy", null).show();
    }

    /** "Hôm nay / 7 ngày": chapters finished, tokens and money, every provider call (retries too) counted once. */
    private View totalsCard() {
        LinearLayout section = a.sectionCard("Σ", "Token và chi phí");
        section.addView(totalsLine("Hôm nay", a.cachedUsageToday));
        section.addView(totalsLine("7 ngày", a.cachedUsageWeek), a.marginLP(-1, -2, 0, 8, 0, 0));
        return section;
    }

    private View totalsLine(String label, UsageTotals t) {
        TextView v = a.text(label + ": " + t.chaptersDone + " chương xong\n" + t.line(), 13, a.TEXT, false);
        v.setSingleLine(false);
        return v;
    }

    private View currentCard(TranslationRepository.JobSummary job, boolean runningNow) {
        LinearLayout card = a.card(14, a.FIELD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        card.addView(a.text(safeName(job.fileName), 15, a.TEXT, true));

        // The live counters belong to the running job only; any other job shows what its own rows say.
        Intent runtime = runningNow ? RuntimeStateStore.toIntent(a) : null;
        int done = runtime == null ? job.doneChunks : runtime.getIntExtra(TranslatorService.EXTRA_COMPLETED, job.doneChunks);
        int total = runtime == null ? job.totalChunks : runtime.getIntExtra(TranslatorService.EXTRA_TOTAL_CHUNKS, job.totalChunks);
        int failed = runtime == null ? job.failedChunks : runtime.getIntExtra(TranslatorService.EXTRA_FAILED, job.failedChunks);
        int fallbacks = runtime == null ? 0 : runtime.getIntExtra(TranslatorService.EXTRA_FALLBACKS, 0);
        long elapsed = runtime == null ? 0L : runtime.getLongExtra(TranslatorService.EXTRA_ELAPSED_MS, 0L);
        long remaining = runtime == null ? -1L : runtime.getLongExtra(TranslatorService.EXTRA_REMAINING_MS, -1L);
        String state = runtime == null ? statusText(job.status) : safe(runtime.getStringExtra(TranslatorService.EXTRA_STATE));
        String usage = runtime != null
                ? TranslationDashboardFormatter.compactTokens(runtime.getIntExtra(TranslatorService.EXTRA_TOTAL_TOKENS, 0)) + " tokens · " + TranslationDashboardFormatter.money3(runtime.getDoubleExtra(TranslatorService.EXTRA_TOTAL_COST, 0d))
                : (job.usage == null ? "Chưa có token/chi phí" : job.usage.line());
        String body = state + " · " + done + "/" + total + " chunks\n" + usage
                + (runtime != null ? "\nElapsed " + CostEstimator.duration(elapsed) + (remaining >= 0 ? " · About " + CostEstimator.duration(remaining) + " remaining" : "") : "")
                + "\n" + failed + " failed" + (runtime != null ? " · " + fallbacks + " fallbacks" : "");
        TextView metrics = a.text(body, 13, a.MUTED, false);
        metrics.setSingleLine(false);
        card.addView(metrics, a.marginLP(-1, -2, 0, 4, 0, 8));

        LinearLayout actions = a.rowContainer();
        Button resume = a.secondaryButton("Resume", v -> a.resumeJob(job.id));
        Button retry = a.secondaryButton("Retry", v -> a.retryJobFailedChunks(job.id));
        resume.setEnabled(JobStatus.classify(job.status) != JobStatus.Kind.FINISHED);
        retry.setEnabled(job.failedChunks > 0);
        actions.addView(resume, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(a.space(8, 1));
        actions.addView(retry, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(a.space(8, 1));
        if (runningNow) actions.addView(a.dangerButton("Cancel", v -> a.sendSvc(TranslatorService.ACTION_CANCEL)), new LinearLayout.LayoutParams(0, -2, 1));
        else actions.addView(a.secondaryButton("Bỏ", v -> confirmDismiss(job)), new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(actions);
        return card;
    }

    private View runCard(RunGroups.Run run) {
        LinearLayout card = a.card(12, a.FIELD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        boolean open = EXPANDED.contains(run.sessionId);
        TextView title = a.text((open ? "▾ " : "▸ ") + "📚 " + (run.setName == null || run.setName.isEmpty() ? "Lượt chạy tổ hợp" : run.setName), 14, a.TEXT, true);
        title.setSingleLine(false);
        card.addView(title);
        StringBuilder meta = new StringBuilder(dateTime(run.createdAt)).append(" · ").append(run.done).append("/").append(run.chapterCount).append(" chương xong");
        if (run.failed > 0) meta.append(" · ").append(run.failed).append(" lỗi");
        if (run.notRun() > 0) meta.append(" · ").append(run.notRun()).append(" chưa chạy");
        meta.append("\n").append(run.usage.line()).append(" · ").append(CostEstimator.duration(run.durationMs()));
        TextView m = a.text(meta.toString(), 12, a.MUTED, false);
        m.setSingleLine(false);
        card.addView(m);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("run-card");
        card.setOnClickListener(v -> {
            if (!EXPANDED.remove(run.sessionId)) EXPANDED.add(run.sessionId);
            a.invalidatePage("Jobs");
            a.switchTab("Jobs");
        });
        if (open) {
            LinearLayout list = new LinearLayout(a);
            list.setOrientation(LinearLayout.VERTICAL);
            for (RunGroups.Entry e : run.chapters) {
                list.addView(recentRow(e.job, true, e.continuedFromSession.isEmpty() ? null : "tiếp tục từ lượt " + sessionTime(e.continuedFromSession, e.job)), a.marginLP(-1, -2, 0, 8, 0, 0));
            }
            if (run.chapters.isEmpty()) list.addView(a.text("Chưa có chương nào chạy trong lượt này.", 12, a.MUTED, false));
            card.addView(list);
        }
        if (run.unfinished() > 0 && run.setId != null && !run.setId.isEmpty() && !a.isDeletedWorkspaceSet(run.setId)) {
            Button go = a.primaryButton("Tiếp tục tổ hợp (" + run.unfinished() + " chương)", v -> a.continueChapterSet(run.setId));
            go.setContentDescription("run-continue-set");
            card.addView(go, a.marginLP(-1, -2, 0, 8, 0, 0));
        }
        return card;
    }

    private String sessionTime(String sessionId, TranslationRepository.JobSummary job) {
        for (RunGroups.SessionInfo s : a.sessionInfosSnapshot()) if (s.sessionId.equals(sessionId)) return dateTime(s.createdAt);
        return dateTime(job.createdAt);
    }

    private View recentRow(TranslationRepository.JobSummary job, boolean inRun) { return recentRow(job, inRun, null); }

    private View recentRow(TranslationRepository.JobSummary job, boolean inRun, String note) {
        LinearLayout card = a.card(12, inRun ? a.CARD : a.FIELD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        TextView title = a.text(safeName(job.fileName), 14, a.TEXT, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(title);
        String lineTwo = statusText(job.status) + " · " + job.doneChunks + "/" + job.totalChunks + " chunks · " + dateTime(job.updatedAt)
                + (job.startSessionId == null || job.startSessionId.isEmpty() ? (inRun ? "" : " (lượt cũ)") : "")
                + (note == null ? "" : " · " + note)
                + (job.usage == null ? "" : "\n" + job.usage.line());
        TextView meta = a.text(lineTwo, 12, a.MUTED, false);
        meta.setSingleLine(false);
        card.addView(meta);
        if (inRun && JobStatus.classify(job.status) == JobStatus.Kind.FINISHED) {
            Button open = a.secondaryButton("Mở kết quả", v -> a.openJobOutput(job.outputUri));
            card.addView(open, a.marginLP(-1, -2, 0, 6, 0, 0));
        }
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> a.openJobDetails(job.id));
        card.setOnLongClickListener(v -> { showJobMenu(job); return true; });
        return card;
    }

    /** Long press on a row: "Bỏ" hides it from the unfinished lists (nothing deleted); "Xóa" removes the job and its token and cost history. */
    private void showJobMenu(TranslationRepository.JobSummary job) {
        boolean unfinished = JobStatus.is(job.status, JobStatus.Kind.CONTINUABLE, JobStatus.Kind.NEEDS_REVIEW, JobStatus.Kind.CONFLICT);
        final String[] labels = unfinished ? new String[]{"Bỏ (giữ lịch sử)", "Xóa job"} : new String[]{"Xóa job"};
        new AlertDialog.Builder(a).setTitle(safeName(job.fileName))
                .setItems(labels, (d, which) -> {
                    if (unfinished && which == 0) confirmDismiss(job);
                    else confirmDelete(job);
                }).setNegativeButton("Đóng", null).show();
    }

    private void confirmDismiss(TranslationRepository.JobSummary job) {
        new AlertDialog.Builder(a).setTitle("Bỏ job này?")
                .setMessage("Job sẽ không hiện ở đây nữa; chunk đã dịch, token và chi phí vẫn giữ trong Jobs.\n\n" + safeName(job.fileName))
                .setPositiveButton("Bỏ job", (d, w) -> a.dismissJobs(java.util.Collections.singletonList(job.id)))
                .setNegativeButton("Hủy", null).show();
    }

    private void confirmDelete(TranslationRepository.JobSummary job) {
        new AlertDialog.Builder(a).setTitle("Xóa job #" + job.id + "?")
                .setMessage("Xóa luôn lịch sử token và chi phí của job này, và không hoàn tác được. Nếu một tổ hợp Biên tập dùng job này làm bản DRAFT thì tổ hợp đó sẽ không đọc được nữa.\n\nThường nên dùng \"Bỏ\" để giữ lịch sử.\n\n" + safeName(job.fileName))
                .setPositiveButton("Xóa", (d, w) -> a.deleteJobs(java.util.Collections.singletonList(job.id)))
                .setNegativeButton("Hủy", null).show();
    }

    private String statusText(String status) {
        switch (JobStatus.classify(status)) {
            case FINISHED: return "Completed";
            case CANCELLED: return "Cancelled";
            case DISMISSED: return "Đã bỏ";
            case NEEDS_REVIEW: return "Cần xem lại";
            case CONFLICT: return "Input đã đổi";
            case CONTINUABLE: return "error".equalsIgnoreCase(status) ? "Failed" : "Paused";
            default: return safe(status);
        }
    }

    private String safe(String value) { return value == null || value.trim().isEmpty() ? "Unknown" : value.trim(); }
    private String safeName(String value) { return value == null || value.trim().isEmpty() ? "Unknown file" : value.trim(); }
    private String dateTime(long time) { return time <= 0 ? "—" : new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(time)); }
}
