package com.ml.tblandroidtxt;

import android.content.Intent;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Lightweight user-facing Jobs page. Developer and QA tools live in Settings. */
final class JobsMainPageFactory {
    private final MainActivity a;

    JobsMainPageFactory(MainActivity activity) { a = activity; }

    View build() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        List<TranslationRepository.JobSummary> jobs = a.jobSummariesSnapshot();
        TranslationRepository.JobSummary current = current(jobs);

        LinearLayout currentSection = a.sectionCard("▶", "Current job");
        if (current == null) currentSection.addView(a.text("No active or paused job.", 13, a.MUTED, false));
        else currentSection.addView(currentCard(current));
        LinearLayout refresh = a.rowContainer();
        refresh.addView(a.primaryButton("↻ Refresh", v -> a.refreshJobCacheAsync(true)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        refresh.addView(a.space(8, 1));
        refresh.addView(a.secondaryButton("Resume last", v -> a.resumeCheckpoint()), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        currentSection.addView(refresh, a.marginLP(-1, -2, 0, 10, 0, 0));
        root.addView(currentSection);

        LinearLayout recent = a.sectionCard("◷", "Recent jobs");
        int count = 0;
        for (TranslationRepository.JobSummary job : jobs) {
            if (current != null && job.id == current.id) continue;
            recent.addView(recentRow(job), a.marginLP(-1, -2, 0, 0, 0, 8));
            if (++count == 30) break;
        }
        if (count == 0) recent.addView(a.text("No recent jobs.", 13, a.MUTED, false));
        root.addView(recent);
        return scroll;
    }

    private TranslationRepository.JobSummary current(List<TranslationRepository.JobSummary> jobs) {
        for (TranslationRepository.JobSummary job : jobs) {
            String state = safe(job.status).toLowerCase(Locale.US);
            if (!"done".equals(state) && !"cancelled".equals(state)) return job;
        }
        return null;
    }

    private View currentCard(TranslationRepository.JobSummary job) {
        LinearLayout card = a.card(14, a.FIELD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        card.addView(a.text(safeName(job.fileName), 15, a.TEXT, true));

        Intent runtime = RuntimeStateStore.toIntent(a);
        int done = runtime == null ? job.doneChunks : runtime.getIntExtra(TranslatorService.EXTRA_COMPLETED, job.doneChunks);
        int total = runtime == null ? job.totalChunks : runtime.getIntExtra(TranslatorService.EXTRA_TOTAL_CHUNKS, job.totalChunks);
        int failed = runtime == null ? job.failedChunks : runtime.getIntExtra(TranslatorService.EXTRA_FAILED, job.failedChunks);
        int fallbacks = runtime == null ? 0 : runtime.getIntExtra(TranslatorService.EXTRA_FALLBACKS, 0);
        int tokens = runtime == null ? 0 : runtime.getIntExtra(TranslatorService.EXTRA_TOTAL_TOKENS, 0);
        double cost = runtime == null ? 0d : runtime.getDoubleExtra(TranslatorService.EXTRA_TOTAL_COST, 0d);
        long elapsed = runtime == null ? 0L : runtime.getLongExtra(TranslatorService.EXTRA_ELAPSED_MS, 0L);
        long remaining = runtime == null ? -1L : runtime.getLongExtra(TranslatorService.EXTRA_REMAINING_MS, -1L);
        String state = runtime == null ? safe(job.status) : safe(runtime.getStringExtra(TranslatorService.EXTRA_STATE));
        String body = state + " · " + done + "/" + total + " chunks\n"
                + TranslationDashboardFormatter.compactTokens(tokens) + " tokens · " + TranslationDashboardFormatter.money3(cost) + "\n"
                + "Elapsed " + CostEstimator.duration(elapsed)
                + (remaining >= 0 ? " · About " + CostEstimator.duration(remaining) + " remaining" : "") + "\n"
                + failed + " failed · " + fallbacks + " fallbacks";
        TextView metrics = a.text(body, 13, a.MUTED, false);
        metrics.setSingleLine(false);
        card.addView(metrics, a.marginLP(-1, -2, 0, 4, 0, 8));

        LinearLayout actions = a.rowContainer();
        Button resume = a.secondaryButton("Resume", v -> a.resumeJob(job.id));
        Button retry = a.secondaryButton("Retry", v -> a.retryJobFailedChunks(job.id));
        resume.setEnabled(!"done".equalsIgnoreCase(job.status));
        retry.setEnabled(job.failedChunks > 0);
        actions.addView(resume, new LinearLayout.LayoutParams(0, a.dp(42), 1));
        actions.addView(a.space(8, 1));
        actions.addView(retry, new LinearLayout.LayoutParams(0, a.dp(42), 1));
        actions.addView(a.space(8, 1));
        actions.addView(a.dangerButton("Cancel", v -> a.sendSvc(TranslatorService.ACTION_CANCEL)), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        card.addView(actions);
        return card;
    }

    private View recentRow(TranslationRepository.JobSummary job) {
        LinearLayout card = a.card(12, a.FIELD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        TextView title = a.text(safeName(job.fileName), 14, a.TEXT, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(title);
        String status = "done".equalsIgnoreCase(job.status) ? "Completed"
                : ("error".equalsIgnoreCase(job.status) ? "Failed"
                : ("paused".equalsIgnoreCase(job.status) ? "Paused" : safe(job.status)));
        TextView meta = a.text(status + " · " + job.doneChunks + "/" + job.totalChunks + " chunks · " + dateTime(job.updatedAt), 12, a.MUTED, false);
        meta.setSingleLine(false);
        card.addView(meta);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> a.openJobDetails(job.id));
        return card;
    }

    private String safe(String value) { return value == null || value.trim().isEmpty() ? "Unknown" : value.trim(); }
    private String safeName(String value) { return value == null || value.trim().isEmpty() ? "Unknown file" : value.trim(); }
    private String dateTime(long time) { return time <= 0 ? "—" : new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(time)); }
}
