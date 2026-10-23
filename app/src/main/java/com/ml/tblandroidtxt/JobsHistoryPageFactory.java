package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** User-facing, paginated translation history. All data is supplied by MainActivity's background loader. */
final class JobsHistoryPageFactory {
    private final MainActivity a;

    JobsHistoryPageFactory(MainActivity activity) { a = activity; }

    View build() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);

        LinearLayout header = a.sectionCard("", "Lịch sử");
        EditText search = a.input("Tìm theo tên tệp", a.historyQuery);
        search.setSingleLine(true);
        search.setContentDescription("Tìm theo tên tệp");
        header.addView(search, a.marginLP(-1, a.dp(48), 0, 0, 0, 8));
        search.addTextChangedListener(new TextWatcher() {
            private Runnable pending;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pending != null) search.removeCallbacks(pending);
                final String value = s == null ? "" : s.toString();
                pending = () -> a.setHistoryQuery(value);
                search.postDelayed(pending, 350L);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        LinearLayout filters = a.rowContainer();
        String setName = historySetName();
        Button setChip = a.secondaryButton(WorkspaceFilterModel.chipText(a.historySetFilterId, setName), v -> {
            if (WorkspaceFilterModel.active(a.historySetFilterId)) a.setHistorySetFilter("");
            else chooseSet();
        });
        setChip.setContentDescription(WorkspaceFilterModel.active(a.historySetFilterId)
                ? "Bỏ lọc theo Tập" : "Lọc lịch sử theo Tập");
        filters.addView(setChip, new LinearLayout.LayoutParams(0, -2, 1));
        if (!WorkspaceFilterModel.active(a.historySetFilterId)) {
            filters.addView(a.space(8, 1));
            Button choose = a.secondaryButton("Chọn Tập", v -> chooseSet());
            filters.addView(choose, new LinearLayout.LayoutParams(0, -2, 1));
        }
        header.addView(filters);
        root.addView(header);

        List<TranslationRepository.JobSummary> jobs = a.historyJobsSnapshot();
        if (a.historyLoading && jobs.isEmpty()) root.addView(a.text("Đang tải lịch sử…", 14, a.MUTED, false));
        if (jobs.isEmpty() && !a.historyLoading) root.addView(a.text("Không có kết quả", 14, a.MUTED, false));
        for (TranslationRepository.JobSummary job : jobs) root.addView(historyRow(job), a.marginLP(-1, -2, 0, 0, 0, 8));

        if (a.historyHasMore()) {
            Button more = a.secondaryButton(a.historyLoading ? "Đang tải…" : "Tải thêm", v -> a.loadHistoryPage(false));
            more.setEnabled(!a.historyLoading);
            root.addView(more, a.marginLP(-1, -2, 0, 8, 0, 0));
        }
        Button back = a.secondaryButton("Quay lại Công việc", v -> a.switchTab("Jobs"));
        root.addView(back, a.marginLP(-1, -2, 0, 12, 0, 0));
        return scroll;
    }

    private View historyRow(TranslationRepository.JobSummary job) {
        LinearLayout card = a.card(12, a.FIELD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        TextView name = a.text(safeName(job.fileName), 14, a.TEXT, true);
        name.setSingleLine(true);
        card.addView(name);
        TextView meta = a.text(status(job.status) + " · " + job.doneChunks + "/" + job.totalChunks
                + " chunk · " + (job.usage == null ? "Chưa có token/chi phí" : job.usage.line()), 12, a.MUTED, false);
        meta.setSingleLine(false);
        card.addView(meta, a.marginLP(-1, -2, 0, 4, 0, 0));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("Mở chi tiết job");
        card.setOnClickListener(v -> a.openJobDetails(job.id));
        return card;
    }

    private void chooseSet() {
        List<WorkspaceSetModel.Entry> sets = a.workspaceSetsSnapshot();
        ArrayList<String> labels = new ArrayList<>();
        labels.add("Tất cả");
        for (WorkspaceSetModel.Entry entry : sets) labels.add(entry.label());
        new AlertDialog.Builder(a).setTitle("Lọc lịch sử theo Tập")
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    if (which == 0) a.setHistorySetFilter("");
                    else if (which - 1 < sets.size()) a.setHistorySetFilter(sets.get(which - 1).id);
                }).setNegativeButton("Hủy", null).show();
    }

    private String historySetName() {
        for (WorkspaceSetModel.Entry entry : a.workspaceSetsSnapshot()) {
            if (a.historySetFilterId != null && a.historySetFilterId.equals(entry.id)) return entry.label();
        }
        return "";
    }

    private String status(String raw) {
        switch (JobStatus.classify(raw)) {
            case FINISHED: return "Xong";
            case NEEDS_REVIEW: return "Cần xem lại";
            case CONFLICT: return "Input đã đổi";
            case CONTINUABLE: return "Tạm dừng";
            case ACTIVE: return "Đang dịch";
            case CANCELLED: return "Đã hủy";
            case DISMISSED: return "Đã bỏ";
            default: return raw == null || raw.trim().isEmpty() ? "Không rõ" : raw;
        }
    }

    private String safeName(String value) { return value == null || value.trim().isEmpty() ? "Tệp TXT" : value.trim(); }
}
