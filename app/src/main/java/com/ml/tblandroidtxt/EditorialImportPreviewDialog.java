package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Card-based import preview. Reference ownership is explicit before any snapshot is saved. */
final class EditorialImportPreviewDialog {
    private EditorialImportPreviewDialog() { }

    static void show(MainActivity a, long projectId, EditorialImportPlanner.Result plan, List<String> failures) {
        boolean canSave = plan != null && plan.readyCount() > 0;
        ScrollView scroll = a.scroll();
        scroll.setFillViewport(true);
        LinearLayout root = a.pageRoot();
        scroll.addView(root);

        LinearLayout summary = a.card(14, a.PANEL, a.BORDER);
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        summary.addView(a.text("Mapping preview", 17, a.TEXT, true));
        summary.addView(a.text(plan.readyCount() + " READY • " + plan.blockedCount() + " NEEDS REVIEW", 13,
                plan.blockedCount() == 0 ? a.GREEN : a.AMBER, true));
        TextView note = a.text("SAFE4 yêu cầu RAW, DRAFT và Glossary. Pronoun là tùy chọn và luôn được ghi rõ AVAILABLE hoặc NONE trước khi snapshot.", 12, a.MUTED, false);
        note.setSingleLine(false);
        summary.addView(note, a.marginLP(-1, -2, 0, 4, 0, 0));
        root.addView(summary, a.marginLP(-1, -2, 0, 0, 0, 8));

        for (EditorialImportPlanner.ChapterPlan chapter : plan.chapters) {
            LinearLayout card = a.card(12, a.FIELD, a.BORDER);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
            LinearLayout header = a.rowContainer();
            header.addView(a.text("Chapter " + chapter.key, 15, a.TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
            boolean ready = chapter.status() == EditorialImportPlanner.ChapterPlan.Status.READY;
            header.addView(a.chip(ready ? "READY" : "NEEDS REVIEW", ready ? a.GREEN : a.AMBER, ready));
            card.addView(header);
            card.addView(sourceRow(a, "RAW", chapter.raw));
            card.addView(sourceRow(a, "DRAFT", chapter.draft));
            card.addView(referenceRow(a, "Glossary", chapter.glossary, chapter.glossaryOrigin));
            card.addView(referenceRow(a, "Pronoun • " + chapter.pronounStatus().name(), chapter.pronoun, chapter.pronounOrigin));
            if (!chapter.problem.isEmpty()) {
                TextView issue = a.text("Chặn: " + chapter.problem, 12, a.RED, true);
                issue.setSingleLine(false);
                card.addView(issue, a.marginLP(-1, -2, 0, 5, 0, 0));
            }
            root.addView(card, a.marginLP(-1, -2, 0, 0, 0, 8));
        }

        if (!plan.unassigned.isEmpty() || !plan.warnings.isEmpty() || (failures != null && !failures.isEmpty())) {
            LinearLayout warnings = a.card(12, a.FIELD, a.BORDER);
            warnings.setOrientation(LinearLayout.VERTICAL);
            warnings.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
            warnings.addView(a.text("Import warnings", 13, a.AMBER, true));
            for (String warning : plan.warnings) addWarning(a, warnings, warning);
            for (EditorialImportPlanner.Source source : plan.unassigned) addWarning(a, warnings, "Chưa gán: " + source.name);
            if (failures != null) for (String failure : failures) addWarning(a, warnings, failure);
            root.addView(warnings, a.marginLP(-1, -2, 0, 0, 0, 8));
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(a).setTitle("Preview mapping").setView(scroll);
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

    private static View referenceRow(MainActivity a, String role, EditorialImportPlanner.Source source,
                                     EditorialImportPlanner.ChapterPlan.ReferenceOrigin origin) {
        if (source == null) return a.text(role + "  — chưa có nguồn", 12, a.MUTED, false);
        String sourceLabel = origin == EditorialImportPlanner.ChapterPlan.ReferenceOrigin.CHAPTER_OVERRIDE
                ? "Nguồn: Chapter override\nFile: " + source.name
                : "Nguồn: Inherited project default\nProfile: " + source.name;
        TextView row = a.text(role + "\n" + sourceLabel + " • " + source.content.length() + " ký tự • sha256 " + shortHash(HashUtil.sha256(source.content)), 12, a.TEXT, false);
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
