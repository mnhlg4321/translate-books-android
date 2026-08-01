package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Card-based, read-only import preview. Editing the selected mapping is a later step. */
final class EditorialImportPreviewDialog {
    private EditorialImportPreviewDialog() { }

    static void show(MainActivity a, long projectId, EditorialImportPlanner.Result plan, List<String> failures) {
        EditorialRepository.AssetSnapshot glossary;
        EditorialRepository.AssetSnapshot pronoun;
        try (EditorialRepository repo = new EditorialRepository(a)) {
            glossary = repo.projectReference(projectId, EditorialWorkflowV5.AssetRole.GLOSSARY);
            pronoun = repo.projectReference(projectId, EditorialWorkflowV5.AssetRole.PRONOUN);
        }
        boolean canSave = plan.readyCount() > 0 && glossary != null && pronoun != null;
        ScrollView scroll = a.scroll();
        scroll.setFillViewport(true);
        LinearLayout root = a.pageRoot();
        scroll.addView(root);

        LinearLayout summary = a.card(14, a.PANEL, a.BORDER);
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        summary.addView(a.text("Mapping preview", 17, a.TEXT, true));
        summary.addView(a.text(plan.readyCount() + " READY • " + plan.blockedCount() + " NEEDS REVIEW", 13, plan.blockedCount() == 0 ? a.GREEN : a.AMBER, true));
        TextView note = a.text("Bốn input sẽ được snapshot khi bạn lưu chapter. Mapping hiện chỉ đọc; các hàng BLOCKED không được lưu.", 12, a.MUTED, false);
        note.setSingleLine(false);
        summary.addView(note, a.marginLP(-1, -2, 0, 4, 0, 0));
        root.addView(summary, a.marginLP(-1, -2, 0, 0, 0, 8));

        if (glossary == null || pronoun == null) {
            TextView missing = a.text("Cần chọn Glossary và Pronoun ACTIVE của project trước khi lưu chapter.", 12, a.AMBER, true);
            missing.setSingleLine(false);
            root.addView(missing, a.marginLP(-1, -2, 0, 0, 0, 8));
        }

        for (EditorialImportPlanner.ChapterPlan chapter : plan.chapters) {
            LinearLayout card = a.card(12, a.FIELD, a.BORDER);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
            LinearLayout header = a.rowContainer();
            header.addView(a.text("Chapter " + chapter.key, 15, a.TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
            header.addView(a.chip(chapter.ready() ? "READY" : "BLOCKED", chapter.ready() ? a.GREEN : a.AMBER, chapter.ready()));
            card.addView(header);
            card.addView(sourceRow(a, "RAW", chapter.raw));
            card.addView(sourceRow(a, "DRAFT", chapter.draft));
            card.addView(referenceRow(a, "Glossary", glossary));
            card.addView(referenceRow(a, "Pronoun", pronoun));
            if (!chapter.problem.isEmpty()) {
                TextView issue = a.text("Chặn: " + chapter.problem, 12, a.RED, true);
                issue.setSingleLine(false);
                card.addView(issue, a.marginLP(-1, -2, 0, 5, 0, 0));
            }
            root.addView(card, a.marginLP(-1, -2, 0, 0, 0, 8));
        }

        if (!plan.warnings.isEmpty() || (failures != null && !failures.isEmpty())) {
            LinearLayout warnings = a.card(12, a.FIELD, a.BORDER);
            warnings.setOrientation(LinearLayout.VERTICAL);
            warnings.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
            warnings.addView(a.text("Import warnings", 13, a.AMBER, true));
            if (plan.warnings != null) for (String warning : plan.warnings) addWarning(a, warnings, warning);
            if (failures != null) for (String failure : failures) addWarning(a, warnings, failure);
            root.addView(warnings, a.marginLP(-1, -2, 0, 0, 0, 8));
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(a).setTitle("Preview RAW + DRAFT").setView(scroll);
        if (canSave) dialog.setPositiveButton("Lưu " + plan.readyCount() + " chapter", (d, which) -> a.persistEditorialBatch(projectId, plan));
        else dialog.setPositiveButton("Đóng", null);
        dialog.setNegativeButton(canSave ? "Hủy" : null, null).show();
    }

    private static View sourceRow(MainActivity a, String role, EditorialImportPlanner.Source source) {
        if (source == null) return a.text(role + "  — chưa ghép", 12, a.MUTED, false);
        String hash = HashUtil.sha256(source.content);
        TextView row = a.text(role + "  " + source.name + " • " + source.content.length() + " ký tự • sha256 " + shortHash(hash), 12, a.TEXT, false);
        row.setSingleLine(false);
        return row;
    }

    private static View referenceRow(MainActivity a, String role, EditorialRepository.AssetSnapshot source) {
        if (source == null) return a.text(role + "  — chưa có ACTIVE snapshot", 12, a.MUTED, false);
        TextView row = a.text(role + "  " + source.displayName + " • snapshot mới • sha256 " + shortHash(source.sha256), 12, a.TEXT, false);
        row.setSingleLine(false);
        return row;
    }

    private static void addWarning(MainActivity a, LinearLayout parent, String warning) {
        TextView row = a.text("• " + warning, 12, a.AMBER, false);
        row.setSingleLine(false);
        parent.addView(row, a.marginLP(-1, -2, 0, 2, 0, 0));
    }

    private static String shortHash(String hash) {
        String value = hash == null ? "" : hash;
        return value.length() <= 12 ? value : value.substring(0, 12) + "…";
    }
}
