package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Editorial project/chapter page. Chapter inputs are immutable snapshots once saved. */
final class EditorialPageFactory {
    private final MainActivity a;

    EditorialPageFactory(MainActivity activity) { a = activity; }

    View build() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        LinearLayout intro = a.sectionCard("✎", "Biên tập V5");
        TextView help = a.text("Tạo project một lần, rồi chọn độc lập RAW, DRAFT, Glossary và Pronoun. App sẽ xem trước mapping trước khi lưu snapshot.", 14, a.TEXT, false);
        help.setSingleLine(false);
        intro.addView(help);
        intro.addView(a.primaryButton("+ Tạo project biên tập", v -> showCreateProject()), a.marginLP(-1, a.dp(48), 0, 12, 0, 0));
        root.addView(intro);

        List<EditorialRepository.Project> projects;
        try (EditorialRepository repo = new EditorialRepository(a)) { projects = repo.listProjects(); }
        if (projects.isEmpty()) {
            TextView empty = a.text("Chưa có project. Project chứa series, volume và các chapter đã snapshot.", 14, a.MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setSingleLine(false);
            root.addView(empty, new LinearLayout.LayoutParams(-1, a.dp(120)));
        } else {
            for (EditorialRepository.Project project : projects) root.addView(projectCard(project), a.marginLP(-1, -2, 0, 0, 0, 10));
        }
        return scroll;
    }

    private View projectCard(EditorialRepository.Project project) {
        LinearLayout box = a.card(14, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        box.addView(a.text("✎ " + project.seriesName + " • " + project.volumeName, 16, a.TEXT, true));

        List<EditorialRepository.Chapter> chapters;
        List<EditorialRepository.ReferenceProfile> glossaries, pronouns;
        EditorialRepository.AssetSnapshot glossary, pronoun;
        try (EditorialRepository repo = new EditorialRepository(a)) {
            chapters = repo.listChapters(project.id);
            glossary = repo.projectReference(project.id, EditorialWorkflowV5.AssetRole.GLOSSARY);
            pronoun = repo.projectReference(project.id, EditorialWorkflowV5.AssetRole.PRONOUN);
            glossaries = repo.projectReferenceProfiles(project.id, EditorialWorkflowV5.AssetRole.GLOSSARY);
            pronouns = repo.projectReferenceProfiles(project.id, EditorialWorkflowV5.AssetRole.PRONOUN);
        }
        box.addView(a.text(chapters.size() + " chapter • V5 workflow snapshot", 12, a.MUTED, false), a.marginLP(-1, -2, 0, 4, 0, 10));

        int rawCount = a.editorialSelectionCount(project.id, true);
        int draftCount = a.editorialSelectionCount(project.id, false);
        LinearLayout actions = a.rowContainer();
        actions.addView(a.primaryButton(rawCount == 0 ? "RAW: chọn file" : "RAW: " + rawCount + " file", v -> a.chooseEditorialBatch(project.id)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        actions.addView(a.space(8, 1));
        actions.addView(a.primaryButton(draftCount == 0 ? "DRAFT: chọn file" : "DRAFT: " + draftCount + " file", v -> a.chooseEditorialDraft(project.id)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        box.addView(actions);
        box.addView(a.secondaryButton("Nhập gói 4 file cho từng chapter", v -> a.chooseEditorialBundle(project.id)), a.marginLP(-1, a.dp(42), 0, 6, 0, 0));
        box.addView(a.text("RAW: " + a.editorialSelectionSummary(project.id, true), 12, rawCount == 0 ? a.MUTED : a.GREEN, false));
        box.addView(a.text("DRAFT: " + a.editorialSelectionSummary(project.id, false), 12, draftCount == 0 ? a.MUTED : a.GREEN, false), a.marginLP(-1, -2, 0, 2, 0, 6));

        LinearLayout references = a.rowContainer();
        references.addView(a.secondaryButton(glossary == null ? "Glossary: Import" : "Glossary: ACTIVE (" + glossaries.size() + ")", v -> a.manageEditorialReferences(project.id, EditorialWorkflowV5.AssetRole.GLOSSARY)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        references.addView(a.space(8, 1));
        references.addView(a.secondaryButton(pronoun == null ? "Pronoun: Import" : "Pronoun: ACTIVE (" + pronouns.size() + ")", v -> a.manageEditorialReferences(project.id, EditorialWorkflowV5.AssetRole.PRONOUN)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        box.addView(references, a.marginLP(-1, -2, 0, 8, 0, 0));
        if (glossary != null) box.addView(a.text("Active Glossary: " + glossary.displayName, 12, a.GREEN, false));
        if (pronoun != null) box.addView(a.text("Active Pronoun: " + pronoun.displayName, 12, a.GREEN, false), a.marginLP(-1, -2, 0, 2, 0, 4));
        if (rawCount > 0 || draftCount > 0) box.addView(a.secondaryButton("Xem trước mapping và lưu chapter", v -> a.previewEditorialSelection(project.id)), a.marginLP(-1, a.dp(46), 0, 8, 0, 0));

        LinearLayout projectActions = a.rowContainer();
        projectActions.addView(a.secondaryButton("Sửa project", v -> showEditProject(project)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        projectActions.addView(a.space(8, 1));
        String folder = project.outputTreeUri.isEmpty() ? "Release folder: chưa chọn" : "Release: " + FileUtil.treeName(android.net.Uri.parse(project.outputTreeUri));
        projectActions.addView(a.secondaryButton(folder, v -> a.chooseEditorialReleaseFolder(project.id)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        box.addView(projectActions, a.marginLP(-1, -2, 0, 8, 0, 8));

        for (EditorialRepository.Chapter chapter : chapters) {
            box.addView(chapterCard(chapter), a.marginLP(-1, -2, 0, 0, 0, 8));
        }
        return box;
    }

    private View chapterCard(EditorialRepository.Chapter chapter) {
        LinearLayout card = a.card(12, a.PANEL, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
        LinearLayout row = a.rowContainer();
        boolean ready = chapter.state == EditorialWorkflowV5.ChapterState.L1_READY
                || chapter.state == EditorialWorkflowV5.ChapterState.L1_CLOSED
                || chapter.state == EditorialWorkflowV5.ChapterState.L2_CLOSED
                || chapter.state == EditorialWorkflowV5.ChapterState.RELEASE_READY
                || chapter.state == EditorialWorkflowV5.ChapterState.RELEASED;
        TextView label = a.text(chapter.chapterKey + " • " + chapter.state.name(), 12, ready ? a.GREEN : chapter.state == EditorialWorkflowV5.ChapterState.FAILED ? a.RED : a.MUTED, false);
        row.addView(label, new LinearLayout.LayoutParams(0, a.dp(42), 1));
        if (chapter.state == EditorialWorkflowV5.ChapterState.L1_CLOSED) {
            row.addView(a.tinyButton("Report", a.BLUE, v -> a.showEditorialL1Report(chapter.id, chapter.chapterKey)), new LinearLayout.LayoutParams(a.dp(72), a.dp(42)));
            row.addView(a.tinyButton("Run L2", a.GREEN, v -> a.confirmRunEditorialL2(chapter.id)), new LinearLayout.LayoutParams(a.dp(72), a.dp(42)));
        } else if (chapter.state == EditorialWorkflowV5.ChapterState.L2_CLOSED) {
            row.addView(a.tinyButton("VI_L2", a.BLUE, v -> a.showEditorialL2(chapter.id, chapter.chapterKey)), new LinearLayout.LayoutParams(a.dp(72), a.dp(42)));
            row.addView(a.tinyButton("Run L3", a.GREEN, v -> a.confirmRunEditorialL3(chapter.id)), new LinearLayout.LayoutParams(a.dp(72), a.dp(42)));
        } else if (chapter.state == EditorialWorkflowV5.ChapterState.RELEASE_READY) {
            row.addView(a.tinyButton("FINAL", a.BLUE, v -> a.showEditorialL3(chapter.id, chapter.chapterKey)), new LinearLayout.LayoutParams(a.dp(68), a.dp(42)));
            row.addView(a.tinyButton("Release", a.GREEN, v -> a.confirmReleaseEditorial(chapter.id, chapter.chapterKey)), new LinearLayout.LayoutParams(a.dp(76), a.dp(42)));
        } else if (chapter.state == EditorialWorkflowV5.ChapterState.RELEASED) {
            row.addView(a.tinyButton("FINAL", a.BLUE, v -> a.showEditorialL3(chapter.id, chapter.chapterKey)), new LinearLayout.LayoutParams(a.dp(84), a.dp(42)));
        } else if (chapter.state == EditorialWorkflowV5.ChapterState.FAILED) {
            String l1Failure, l2Failure, l3Failure;
            try (EditorialRepository repo = new EditorialRepository(a)) {
                l1Failure = repo.latestEvidenceForChapter(chapter.id, "L1_FAILURE");
                l2Failure = repo.latestEvidenceForChapter(chapter.id, "L2_FAILURE");
                l3Failure = repo.latestEvidenceForChapter(chapter.id, "L3_FAILURE");
            }
            if (!l3Failure.isEmpty()) row.addView(a.tinyButton("Retry L3", a.AMBER, v -> a.confirmRetryEditorialL3(chapter.id, l3Failure)), new LinearLayout.LayoutParams(a.dp(84), a.dp(42)));
            else if (!l2Failure.isEmpty()) row.addView(a.tinyButton("Retry L2", a.AMBER, v -> a.confirmRetryEditorialL2(chapter.id, l2Failure)), new LinearLayout.LayoutParams(a.dp(84), a.dp(42)));
            else row.addView(a.tinyButton("Retry", a.AMBER, v -> a.confirmRetryEditorialL1(chapter.id, l1Failure)), new LinearLayout.LayoutParams(a.dp(84), a.dp(42)));
        } else {
            android.widget.Button run = a.tinyButton("Run L1", a.BLUE, v -> a.confirmRunEditorialL1(chapter.id));
            run.setEnabled(chapter.state == EditorialWorkflowV5.ChapterState.L1_READY);
            run.setAlpha(run.isEnabled() ? 1f : .45f);
            row.addView(run, new LinearLayout.LayoutParams(a.dp(84), a.dp(42)));
        }
        card.addView(row);
        card.addView(chapterSnapshot(chapter.id));
        return card;
    }

    private View chapterSnapshot(long chapterId) {
        LinearLayout snapshot = a.card(10, a.FIELD, a.BORDER);
        snapshot.setOrientation(LinearLayout.VERTICAL);
        snapshot.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
        snapshot.addView(a.text("Input Snapshot • immutable", 11, a.MUTED, true));
        Map<EditorialWorkflowV5.AssetRole, EditorialRepository.AssetSnapshot> assets = new EnumMap<>(EditorialWorkflowV5.AssetRole.class);
        try (EditorialRepository repo = new EditorialRepository(a)) {
            for (EditorialRepository.AssetSnapshot asset : repo.chapterAssets(chapterId)) assets.put(asset.role, asset);
        }
        EditorialWorkflowV5.AssetRole[] roles = {
                EditorialWorkflowV5.AssetRole.RAW,
                EditorialWorkflowV5.AssetRole.DRAFT,
                EditorialWorkflowV5.AssetRole.GLOSSARY,
                EditorialWorkflowV5.AssetRole.PRONOUN
        };
        for (EditorialWorkflowV5.AssetRole role : roles) {
            EditorialRepository.AssetSnapshot asset = assets.get(role);
            String value = asset == null ? "chưa có snapshot" : asset.displayName + " • " + asset.content.length() + " ký tự • sha256 " + shortHash(asset.sha256);
            TextView line = a.text(snapshotRole(role) + "  " + value, 11, asset == null ? a.MUTED : a.TEXT, false);
            line.setSingleLine(false);
            snapshot.addView(line, a.marginLP(-1, -2, 0, 2, 0, 0));
        }
        return snapshot;
    }

    private String snapshotRole(EditorialWorkflowV5.AssetRole role) {
        if (role == EditorialWorkflowV5.AssetRole.RAW) return "RAW";
        if (role == EditorialWorkflowV5.AssetRole.DRAFT) return "DRAFT";
        if (role == EditorialWorkflowV5.AssetRole.GLOSSARY) return "Glossary";
        return "Pronoun";
    }

    private String shortHash(String hash) {
        String value = hash == null ? "" : hash;
        return value.length() <= 12 ? value : value.substring(0, 12) + "…";
    }

    private String chapterSummary(List<EditorialRepository.Chapter> chapters) {
        StringBuilder b = new StringBuilder();
        int max = Math.min(4, chapters.size());
        for (int i = 0; i < max; i++) {
            EditorialRepository.Chapter c = chapters.get(i);
            if (i > 0) b.append('\n');
            b.append("• ").append(c.chapterKey).append(" — ").append(c.state.name());
        }
        if (chapters.size() > max) b.append("\n+").append(chapters.size() - max).append(" chapter khác");
        return b.toString();
    }

    private void showChapters(EditorialRepository.Project project, List<EditorialRepository.Chapter> chapters) {
        new AlertDialog.Builder(a).setTitle(project.seriesName + " • " + project.volumeName)
                .setMessage(chapters.isEmpty() ? "Chưa có chapter." : chapterSummary(chapters))
                .setPositiveButton("Đóng", null).show();
    }

    private void showCreateProject() {
        LinearLayout form = new LinearLayout(a);
        form.setOrientation(LinearLayout.VERTICAL);
        int p = a.dp(22);
        form.setPadding(p, 0, p, 0);
        EditText series = a.input("Series", "");
        EditText volume = a.input("Volume", "");
        form.addView(series);
        form.addView(volume, a.marginLP(-1, -2, 0, 8, 0, 0));
        new AlertDialog.Builder(a).setTitle("Tạo project biên tập")
                .setMessage("Glossary và Pronoun của Biên tập được lưu riêng trong project.")
                .setView(form).setPositiveButton("Tạo", (d, w) -> a.createEditorialProject(series.getText().toString(), volume.getText().toString()))
                .setNegativeButton("Hủy", null).show();
    }

    private void showEditProject(EditorialRepository.Project project) {
        LinearLayout form = new LinearLayout(a);
        form.setOrientation(LinearLayout.VERTICAL);
        int p = a.dp(22);
        form.setPadding(p, 0, p, 0);
        EditText series = a.input("Series", project.seriesName);
        EditText volume = a.input("Volume", project.volumeName);
        form.addView(series);
        form.addView(volume, a.marginLP(-1, -2, 0, 8, 0, 0));
        new AlertDialog.Builder(a).setTitle("Sửa project biên tập").setView(form)
                .setPositiveButton("Lưu", (d, w) -> a.updateEditorialProject(project.id, series.getText().toString(), volume.getText().toString()))
                .setNegativeButton("Hủy", null).show();
    }
}
