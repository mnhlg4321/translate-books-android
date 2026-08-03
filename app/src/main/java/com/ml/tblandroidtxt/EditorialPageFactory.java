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

/** SAFE4 project/import page. Legacy V5 chapters are intentionally read-only. */
final class EditorialPageFactory {
    private final MainActivity a;

    EditorialPageFactory(MainActivity activity) { a = activity; }

    View build() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        LinearLayout intro = a.sectionCard("✎", "Biên tập V5-SAFE.4");
        TextView help = a.text("Bộ 3 chỉ dẫn SAFE4 đã được đóng gói bất biến. Lõi V5 cũ đã ngừng hoạt động; chạy model và phát hành đang bị khóa cho đến khi đủ schema, lineage và Golden Replay G1–G10.", 14, a.TEXT, false);
        help.setSingleLine(false);
        intro.addView(help);
        intro.addView(a.text("Pack sha256 " + shortHash(EditorialSafe4Pack.PACK_HASH), 11, a.MUTED, false), a.marginLP(-1, -2, 0, 6, 0, 0));
        intro.addView(a.primaryButton("+ Tạo project SAFE4", v -> showCreateProject()), a.marginLP(-1, a.dp(48), 0, 12, 0, 0));
        root.addView(intro);

        List<EditorialRepository.Project> projects;
        try (EditorialRepository repo = new EditorialRepository(a)) { projects = repo.listProjects(); }
        if (projects.isEmpty()) {
            TextView empty = a.text("Chưa có project. Bạn có thể chuẩn bị input SAFE4 ngay; phần thực thi vẫn khóa an toàn.", 14, a.MUTED, false);
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
            glossary = repo.projectReference(project.id, EditorialSafe4Workflow.AssetRole.GLOSSARY);
            pronoun = repo.projectReference(project.id, EditorialSafe4Workflow.AssetRole.PRONOUN);
            glossaries = repo.projectReferenceProfiles(project.id, EditorialSafe4Workflow.AssetRole.GLOSSARY);
            pronouns = repo.projectReferenceProfiles(project.id, EditorialSafe4Workflow.AssetRole.PRONOUN);
        }
        boolean safe4Project = EditorialSafe4Pack.VERSION.equals(project.workflowVersion)
                && EditorialSafe4Pack.PACK_HASH.equals(project.workflowHash);
        String identity = safe4Project
                ? chapters.size() + " chapter • V5-SAFE.4 • " + shortHash(project.workflowHash)
                : chapters.size() + " chapter • LEGACY V5 • chỉ đọc lịch sử";
        box.addView(a.text(identity, 12, safe4Project ? a.GREEN : a.AMBER, false), a.marginLP(-1, -2, 0, 4, 0, 10));

        if (safe4Project) {
            addSafe4Inputs(box, project, glossary, pronoun, glossaries, pronouns);
            box.addView(a.secondaryButton("Sửa project", v -> showEditProject(project)), a.marginLP(-1, a.dp(46), 0, 8, 0, 8));
        } else {
            TextView legacy = a.text("Project này không được chạy, retry, phát hành hay nhận chapter mới. Snapshot và bảng SQLite cũ vẫn được giữ để đối chiếu/migrate sau.", 12, a.MUTED, false);
            legacy.setSingleLine(false);
            box.addView(legacy, a.marginLP(-1, -2, 0, 4, 0, 10));
        }

        for (EditorialRepository.Chapter chapter : chapters) {
            box.addView(chapterCard(chapter), a.marginLP(-1, -2, 0, 0, 0, 8));
        }
        return box;
    }

    private void addSafe4Inputs(LinearLayout box, EditorialRepository.Project project,
                                EditorialRepository.AssetSnapshot glossary,
                                EditorialRepository.AssetSnapshot pronoun,
                                List<EditorialRepository.ReferenceProfile> glossaries,
                                List<EditorialRepository.ReferenceProfile> pronouns) {
        int rawCount = a.editorialSelectionCount(project.id, true);
        int draftCount = a.editorialSelectionCount(project.id, false);
        LinearLayout actions = a.rowContainer();
        actions.addView(a.primaryButton(rawCount == 0 ? "RAW: chọn file" : "RAW: " + rawCount + " file", v -> a.chooseEditorialBatch(project.id)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        actions.addView(a.space(8, 1));
        actions.addView(a.primaryButton(draftCount == 0 ? "DRAFT: chọn file" : "DRAFT: " + draftCount + " file", v -> a.chooseEditorialDraft(project.id)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        box.addView(actions);
        box.addView(a.secondaryButton("Nhập gói SAFE4: 3 file + Pronoun tùy chọn", v -> a.chooseEditorialBundle(project.id)), a.marginLP(-1, a.dp(42), 0, 6, 0, 0));
        box.addView(a.text("RAW: " + a.editorialSelectionSummary(project.id, true), 12, rawCount == 0 ? a.MUTED : a.GREEN, false));
        box.addView(a.text("DRAFT: " + a.editorialSelectionSummary(project.id, false), 12, draftCount == 0 ? a.MUTED : a.GREEN, false), a.marginLP(-1, -2, 0, 2, 0, 6));

        LinearLayout references = a.rowContainer();
        references.addView(a.secondaryButton(glossary == null ? "Glossary: Import" : "Glossary: ACTIVE (" + glossaries.size() + ")", v -> a.manageEditorialReferences(project.id, EditorialSafe4Workflow.AssetRole.GLOSSARY)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        references.addView(a.space(8, 1));
        references.addView(a.secondaryButton(pronoun == null ? "Pronoun: NONE" : "Pronoun: AVAILABLE (" + pronouns.size() + ")", v -> a.manageEditorialReferences(project.id, EditorialSafe4Workflow.AssetRole.PRONOUN)), new LinearLayout.LayoutParams(0, a.dp(46), 1));
        box.addView(references, a.marginLP(-1, -2, 0, 8, 0, 0));
        if (glossary != null) box.addView(a.text("Active Glossary: " + glossary.displayName, 12, a.GREEN, false));
        if (pronoun != null) box.addView(a.text("PRONOUN_STATUS=AVAILABLE • " + pronoun.displayName, 12, a.GREEN, false), a.marginLP(-1, -2, 0, 2, 0, 4));
        else box.addView(a.text("PRONOUN_STATUS=NONE • hợp lệ theo SAFE4", 12, a.GREEN, false), a.marginLP(-1, -2, 0, 2, 0, 4));
        if (rawCount > 0 || draftCount > 0) box.addView(a.secondaryButton("Xem trước mapping và lưu snapshot", v -> a.previewEditorialSelection(project.id)), a.marginLP(-1, a.dp(46), 0, 8, 0, 0));
    }

    private View chapterCard(EditorialRepository.Chapter chapter) {
        LinearLayout card = a.card(12, a.PANEL, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
        boolean legacy = chapter.state == EditorialSafe4Workflow.ChapterState.LEGACY_V5_READ_ONLY;
        card.addView(a.text(chapter.chapterKey + " • " + chapter.state.name(), 12, legacy ? a.MUTED : a.AMBER, false));
        String status = legacy
                ? "LEGACY V5 • chỉ đọc; không dùng evidence/gate cũ để phát hành SAFE4"
                : "SAFE4 • execution blocked: " + EditorialSafe4Pack.blockedReason();
        TextView state = a.text(status, 11, legacy ? a.MUTED : a.AMBER, false);
        state.setSingleLine(false);
        card.addView(state, a.marginLP(-1, -2, 0, 2, 0, 6));
        card.addView(chapterSnapshot(chapter.id));
        return card;
    }

    private View chapterSnapshot(long chapterId) {
        LinearLayout snapshot = a.card(10, a.FIELD, a.BORDER);
        snapshot.setOrientation(LinearLayout.VERTICAL);
        snapshot.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
        snapshot.addView(a.text("Input Snapshot • immutable", 11, a.MUTED, true));
        Map<EditorialSafe4Workflow.AssetRole, EditorialRepository.AssetSnapshot> assets = new EnumMap<>(EditorialSafe4Workflow.AssetRole.class);
        try (EditorialRepository repo = new EditorialRepository(a)) {
            for (EditorialRepository.AssetSnapshot asset : repo.chapterAssets(chapterId)) assets.put(asset.role, asset);
        }
        EditorialSafe4Workflow.AssetRole[] roles = { EditorialSafe4Workflow.AssetRole.RAW,
                EditorialSafe4Workflow.AssetRole.DRAFT, EditorialSafe4Workflow.AssetRole.GLOSSARY,
                EditorialSafe4Workflow.AssetRole.PRONOUN };
        for (EditorialSafe4Workflow.AssetRole role : roles) {
            EditorialRepository.AssetSnapshot asset = assets.get(role);
            String value;
            if (asset == null && role == EditorialSafe4Workflow.AssetRole.PRONOUN) value = "PRONOUN_STATUS=NONE";
            else value = asset == null ? "chưa có snapshot" : asset.displayName + " • " + asset.content.length() + " ký tự • sha256 " + shortHash(asset.sha256);
            TextView line = a.text(snapshotRole(role) + "  " + value, 11,
                    asset == null && role != EditorialSafe4Workflow.AssetRole.PRONOUN ? a.MUTED : a.TEXT, false);
            line.setSingleLine(false);
            snapshot.addView(line, a.marginLP(-1, -2, 0, 2, 0, 0));
        }
        return snapshot;
    }

    private String snapshotRole(EditorialSafe4Workflow.AssetRole role) {
        if (role == EditorialSafe4Workflow.AssetRole.RAW) return "RAW";
        if (role == EditorialSafe4Workflow.AssetRole.DRAFT) return "DRAFT";
        if (role == EditorialSafe4Workflow.AssetRole.GLOSSARY) return "Glossary";
        return "Pronoun";
    }

    private static String shortHash(String hash) {
        String value = hash == null ? "" : hash;
        return value.length() <= 12 ? value : value.substring(0, 12) + "…";
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
        new AlertDialog.Builder(a).setTitle("Tạo project V5-SAFE.4")
                .setMessage("Project sẽ khóa vào đúng hash của bộ 3 chỉ dẫn SAFE4. Có thể chuẩn bị snapshot, nhưng chưa thể chạy model/phát hành.")
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
        new AlertDialog.Builder(a).setTitle("Sửa project SAFE4").setView(form)
                .setPositiveButton("Lưu", (d, w) -> a.updateEditorialProject(project.id, series.getText().toString(), volume.getText().toString()))
                .setNegativeButton("Hủy", null).show();
    }
}
