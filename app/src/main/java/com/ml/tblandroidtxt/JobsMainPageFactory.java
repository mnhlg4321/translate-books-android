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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** User-facing Công việc page: two stable workspaces, Dịch and Biên tập. */
final class JobsMainPageFactory {
    private final MainActivity a;
    private static final Set<String> EXPANDED = new HashSet<>();

    JobsMainPageFactory(MainActivity activity) { a = activity; }

    View build() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        root.addView(sectionTabs(), a.marginLP(-1, -2, 0, 0, 0, 10));
        if ("EDITORIAL".equals(a.jobsSection)) buildEditorial(root);
        else buildTranslation(root);
        return scroll;
    }

    private View sectionTabs() {
        LinearLayout row = a.rowContainer();
        boolean translation = !"EDITORIAL".equals(a.jobsSection);
        List<TranslationRepository.JobSummary> jobs = a.workspaceJobsSnapshot();
        int editorialCount = a.editorialJobsSnapshot().size();
        Button translate = a.button("Dịch · " + jobs.size(), translation ? a.CARD : a.FIELD,
                translation ? a.TEXT : a.MUTED, v -> a.selectJobsSection("TRANSLATION"));
        Button editorial = a.button("Biên tập" + (hasActiveEditorial() ? " · đang chạy" : "") + " · " + editorialCount,
                !translation ? a.CARD : a.FIELD, !translation ? a.TEXT : a.MUTED,
                v -> a.selectJobsSection("EDITORIAL"));
        a.tint(translate, translation ? a.CARD : a.FIELD, translation ? a.BLUE : a.BORDER, 1, 10);
        a.tint(editorial, !translation ? a.CARD : a.FIELD, !translation ? a.BLUE : a.BORDER, 1, 10);
        row.addView(translate, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(a.space(8, 1));
        row.addView(editorial, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private boolean hasActiveEditorial() {
        for (JobsEditorialCardData item : a.editorialJobsSnapshot()) if (item.active) return true;
        return false;
    }

    private void buildTranslation(LinearLayout root) {
        root.addView(workspaceFilter(), a.marginLP(-1, -2, 0, 0, 0, 12));
        List<TranslationRepository.JobSummary> jobs = a.filteredWorkspaceJobsSnapshot();
        List<TranslationRepository.JobSummary> allJobs = a.workspaceJobsSnapshot();
        root.addView(totalsCard());

        boolean serviceActive = TranslatorService.isActive();
        long activeJobId = -1L;
        if (serviceActive) {
            Intent runtime = RuntimeStateStore.toIntent(a);
            if (runtime != null) activeJobId = runtime.getLongExtra(TranslatorService.EXTRA_ACTIVE_JOB_ID, -1L);
        }
        TranslationRepository.JobSummary current = JobPicker.current(allJobs, serviceActive, activeJobId);
        LinearLayout currentSection = a.sectionCard("", "Đang làm");
        if (current == null) currentSection.addView(a.text("Không có job đang chạy hay đang dở.", 13, a.MUTED, false));
        else currentSection.addView(currentCard(current, serviceActive && current.id == activeJobId));
        LinearLayout refresh = a.rowContainer();
        refresh.addView(a.primaryButton("Làm mới", v -> a.refreshJobCacheAsync(true)), new LinearLayout.LayoutParams(0, -2, 1));
        refresh.addView(a.space(8, 1));
        refresh.addView(a.secondaryButton("Tiếp tục gần nhất", v -> a.resumeCheckpoint()), new LinearLayout.LayoutParams(0, -2, 1));
        currentSection.addView(refresh, a.marginLP(-1, -2, 0, 10, 0, 0));
        Button history = a.secondaryButton("Lịch sử · Xem tất cả", v -> a.openHistory());
        currentSection.addView(history, a.marginLP(-1, -2, 0, 0, 0, 0));
        root.addView(currentSection);

        LinearLayout recent = a.sectionCard("", "Gần đây");
        List<RunGroups.Item> items = RunGroups.build(jobs, a.sessionInfosSnapshot());
        int count = 0;
        for (RunGroups.Item item : items) {
            recent.addView(item.run != null ? runCard(item.run) : recentRow(item.single, false), a.marginLP(-1, -2, 0, 0, 0, 8));
            if (++count == 30) break;
        }
        if (count == 0) recent.addView(a.text("Chưa có công việc gần đây.", 13, a.MUTED, false));
        root.addView(recent);
    }

    private View workspaceFilter() {
        LinearLayout box = a.card(14, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        TextView label = a.text("Lọc theo Tập", 13, a.TEXT, true);
        box.addView(label, a.marginLP(-1, -2, 0, 0, 0, 6));
        String filter = WorkspaceFilterModel.chipText(a.jobsSetFilterId, a.workspaceFilterLabel());
        Button chip = a.secondaryButton(filter, v -> {
            if (WorkspaceFilterModel.active(a.jobsSetFilterId)) a.setJobsSetFilter("");
            else chooseWorkspaceFilter();
        });
        chip.setContentDescription(WorkspaceFilterModel.active(a.jobsSetFilterId) ? "Bỏ lọc theo Tập" : "Lọc Công việc theo Tập");
        box.addView(chip, new LinearLayout.LayoutParams(-1, -2));
        if (!WorkspaceFilterModel.active(a.jobsSetFilterId)) {
            Button choose = a.secondaryButton("Chọn Tập", v -> chooseWorkspaceFilter());
            box.addView(choose, a.marginLP(-1, -2, 0, 6, 0, 0));
        }
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

    private View totalsCard() {
        LinearLayout section = a.sectionCard("", "Token và chi phí");
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
        Intent runtime = runningNow ? RuntimeStateStore.toIntent(a) : null;
        int done = runtime == null ? job.doneChunks : runtime.getIntExtra(TranslatorService.EXTRA_COMPLETED, job.doneChunks);
        int total = runtime == null ? job.totalChunks : runtime.getIntExtra(TranslatorService.EXTRA_TOTAL_CHUNKS, job.totalChunks);
        int failed = runtime == null ? job.failedChunks : runtime.getIntExtra(TranslatorService.EXTRA_FAILED, job.failedChunks);
        String state = runtime == null ? statusText(job.status) : safe(runtime.getStringExtra(TranslatorService.EXTRA_STATE));
        String usage = runtime != null
                ? TranslationDashboardFormatter.compactTokens(runtime.getIntExtra(TranslatorService.EXTRA_TOTAL_TOKENS, 0)) + " token · " + TranslationDashboardFormatter.money3(runtime.getDoubleExtra(TranslatorService.EXTRA_TOTAL_COST, 0d))
                : (job.usage == null ? "Chưa có token/chi phí" : job.usage.line());
        TextView metrics = a.text(state + " · " + done + "/" + total + " chunk\n" + usage + "\n" + failed + " chunk lỗi", 13, a.MUTED, false);
        metrics.setSingleLine(false);
        card.addView(metrics, a.marginLP(-1, -2, 0, 4, 0, 8));

        LinearLayout actions = a.rowContainer();
        boolean needsReview = JobStatus.classify(job.status) == JobStatus.Kind.NEEDS_REVIEW || job.unknownChunks > 0;
        Button resume = a.secondaryButton("Tiếp tục", v -> a.resumeJob(job.id));
        Button retry = a.secondaryButton("Dịch lại lỗi", v -> a.retryJobFailedChunks(job.id));
        resume.setEnabled(!needsReview && JobStatus.classify(job.status) != JobStatus.Kind.FINISHED);
        retry.setEnabled(!needsReview && job.failedChunks > 0);
        actions.addView(resume, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(a.space(8, 1));
        actions.addView(retry, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(a.space(8, 1));
        if (runningNow) actions.addView(a.secondaryButton("Dừng", v -> a.confirmStopTranslation()), new LinearLayout.LayoutParams(0, -2, 1));
        else actions.addView(a.secondaryButton("Bỏ", v -> confirmDismiss(job)), new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(actions);
        if (needsReview) card.addView(a.text("Cần xem lại trạng thái gửi trước khi chạy lại.", 12, a.AMBER, false), a.marginLP(-1, -2, 0, 6, 0, 0));
        return card;
    }

    private void buildEditorial(LinearLayout root) {
        if (!a.editorialJobsLoaded) {
            root.addView(a.text("Đang tải công việc Biên tập…", 14, a.MUTED, false));
            a.refreshEditorialJobsAsync();
            return;
        }
        List<JobsEditorialCardData> data = a.editorialJobsSnapshot();
        if (data.isEmpty()) root.addView(a.text("Chưa có công việc Biên tập.", 14, a.MUTED, false));
        for (JobsEditorialCardData item : data) {
            // The existing read-only Editorial card is the sole renderer; DB loading happened above the UI thread.
            root.addView(EditorialJobCard.build(a, item.combo, item.run, item.items), a.marginLP(-1, -2, 0, 0, 0, 10));
        }
    }

    private View runCard(RunGroups.Run run) {
        LinearLayout card = a.card(12, a.FIELD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        boolean open = EXPANDED.contains(run.sessionId);
        TextView title = a.text((open ? "Thu gọn · " : "Mở · ") + (run.setName == null || run.setName.isEmpty() ? "Lượt chạy tập" : run.setName), 14, a.TEXT, true);
        title.setSingleLine(false);
        card.addView(title);
        StringBuilder meta = new StringBuilder(dateTime(run.createdAt)).append(" · ").append(run.done).append("/").append(run.chapterCount).append(" chương xong");
        if (run.failed > 0) meta.append(" · ").append(run.failed).append(" lỗi");
        if (run.notRun() > 0) meta.append(" · ").append(run.notRun()).append(" chưa chạy");
        meta.append("\n").append(run.usage.line()).append(" · ").append(CostEstimator.duration(run.durationMs()));
        card.addView(a.text(meta.toString(), 12, a.MUTED, false));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("Mở lượt chạy tập");
        card.setOnClickListener(v -> { if (!EXPANDED.remove(run.sessionId)) EXPANDED.add(run.sessionId); a.invalidatePage("Jobs"); a.switchTab("Jobs"); });
        if (open) {
            LinearLayout list = new LinearLayout(a);
            list.setOrientation(LinearLayout.VERTICAL);
            for (RunGroups.Entry e : run.chapters) list.addView(recentRow(e.job, true), a.marginLP(-1, -2, 0, 0, 0, 8));
            card.addView(list);
        }
        return card;
    }

    private View recentRow(TranslationRepository.JobSummary job, boolean inRun) {
        LinearLayout card = a.card(12, inRun ? a.CARD : a.FIELD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        TextView title = a.text(safeName(job.fileName), 14, a.TEXT, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(title);
        TextView meta = a.text(statusText(job.status) + " · " + job.doneChunks + "/" + job.totalChunks + " chunk · " + dateTime(job.updatedAt)
                + (job.usage == null ? "" : "\n" + job.usage.line()), 12, a.MUTED, false);
        meta.setSingleLine(false);
        card.addView(meta);
        if (inRun && JobStatus.classify(job.status) == JobStatus.Kind.FINISHED) card.addView(a.secondaryButton("Mở kết quả", v -> a.openJobOutput(job.outputUri)));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("Mở chi tiết công việc");
        card.setOnClickListener(v -> a.openJobDetails(job.id));
        return card;
    }

    private void confirmDismiss(TranslationRepository.JobSummary job) {
        new AlertDialog.Builder(a).setTitle("Bỏ job này?")
                .setMessage("Job sẽ không hiện ở đây nữa; lịch sử token và chi phí vẫn giữ trong Jobs.")
                .setPositiveButton("Bỏ job", (d, w) -> a.dismissJobs(java.util.Collections.singletonList(job.id)))
                .setNegativeButton("Hủy", null).show();
    }

    private String statusText(String raw) {
        switch (JobStatus.classify(raw)) {
            case FINISHED: return "Xong";
            case CANCELLED: return "Đã hủy";
            case DISMISSED: return "Đã bỏ";
            case NEEDS_REVIEW: return "Cần xem lại";
            case CONFLICT: return "Input đã đổi";
            case CONTINUABLE: return "error".equalsIgnoreCase(raw) ? "Lỗi" : "Tạm dừng";
            case ACTIVE: return "Đang dịch";
            default: return safe(raw);
        }
    }

    private String safe(String value) { return value == null || value.trim().isEmpty() ? "Không rõ" : value.trim(); }
    private String safeName(String value) { return value == null || value.trim().isEmpty() ? "Tệp TXT" : value.trim(); }
    private String dateTime(long time) { return time <= 0 ? "—" : new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(time)); }
}
