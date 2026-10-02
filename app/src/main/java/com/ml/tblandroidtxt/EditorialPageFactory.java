package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.EnumMap;
import java.util.ArrayList;
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

        LinearLayout packs = a.sectionCard("▦", "Editorial Packs");
        TextView packHelp = a.text("Kiểm tra các pack đã được lưu trong persistent registry. Khu vực này chỉ đọc; built-in SAFE4 và candidate bên ngoài không được seed tự động.", 12, a.MUTED, false);
        packHelp.setSingleLine(false);
        packs.addView(packHelp, a.marginLP(-1, -2, 0, 5, 0, 0));
        packs.addView(a.secondaryButton("Import Editorial Pack ZIP", v -> a.openEditorialPackZipImport()), a.marginLP(-1, a.dp(44), 0, 6, 0, 0));
        packs.addView(a.secondaryButton("Xem các pack đã lưu", v -> new EditorialPackManagementPageFactory(a).show()), a.marginLP(-1, a.dp(44), 0, 6, 0, 0));
        packs.addView(a.text("READ ONLY • không import / certify / activate / delete / replace", 11, a.CYAN, true));
        root.addView(packs, a.marginLP(-1, -2, 0, 0, 0, 10));

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
        boolean p4Project = project.bindingIdentity != null && !project.bindingIdentity.isEmpty();
        boolean safe4Project = !p4Project && EditorialSafe4Pack.VERSION.equals(project.workflowVersion)
                && EditorialSafe4Pack.PACK_HASH.equals(project.workflowHash);
        String identity = p4Project
                ? chapters.size() + " chapter • " + project.boundPackId + " v" + project.boundPackVersion
                + " • hash " + shortHash(project.boundCanonicalPackHash)
                + "\nTương thích contract • Đã lưu • Chờ chứng nhận • Execution đang khóa"
                : safe4Project
                ? chapters.size() + " chapter • V5-SAFE.4 • " + shortHash(project.workflowHash)
                : chapters.size() + " chapter • LEGACY V5 • chỉ đọc lịch sử";
        TextView identityView = a.text(identity, 12, p4Project ? a.AMBER : safe4Project ? a.GREEN : a.AMBER, false);
        identityView.setSingleLine(false);
        box.addView(identityView, a.marginLP(-1, -2, 0, 4, 0, 10));

        if (p4Project) {
            TextView locked = a.text("Binding immutable • chọn pack khác cần tạo project mới. Không auto-rebind.", 12, a.MUTED, false);
            locked.setSingleLine(false);
            box.addView(locked, a.marginLP(-1, -2, 0, 4, 0, 10));
        } else if (safe4Project) {
            addSafe4Inputs(box, project, glossary, pronoun, glossaries, pronouns);
            box.addView(a.secondaryButton("Sửa project", v -> showEditProject(project)), a.marginLP(-1, a.dp(46), 0, 8, 0, 8));
        } else {
            TextView legacy = a.text("Project này không được chạy, retry, phát hành hay nhận chapter mới. Snapshot và bảng SQLite cũ vẫn được giữ để đối chiếu/migrate sau.", 12, a.MUTED, false);
            legacy.setSingleLine(false);
            box.addView(legacy, a.marginLP(-1, -2, 0, 4, 0, 10));
        }

        for (EditorialRepository.Chapter chapter : chapters) {
            View chapterView = chapterCard(chapter);
            if (p4Project && chapterView instanceof LinearLayout) {
                ((LinearLayout) chapterView).addView(new EditorialChapterFinalPanel(a).build(project, chapter),
                        a.marginLP(-1, -2, 0, 6, 0, 0));
            }
            box.addView(chapterView, a.marginLP(-1, -2, 0, 0, 0, 8));
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
        List<EditorialPackSelectionCandidate> candidates = loadSelectablePacks();
        if (candidates.isEmpty()) {
            new AlertDialog.Builder(a).setTitle("Chưa có pack có thể chọn")
                    .setMessage("Chỉ pack đã lưu immutable, integrity hợp lệ và DATA_COMPATIBLE qua trusted profile mới được chọn. Hãy import pack trước; pack bị khóa vẫn chỉ xem được trong màn hình quản lý.")
                    .setPositiveButton("Xem pack đã lưu", (d, w) -> new EditorialPackManagementPageFactory(a).show())
                    .setNegativeButton("Đóng", null).show();
            return;
        }
        LinearLayout form = new LinearLayout(a);
        form.setOrientation(LinearLayout.VERTICAL);
        int p = a.dp(22);
        form.setPadding(p, 0, p, 0);
        EditText series = a.input("Series", "");
        EditText volume = a.input("Volume", "");
        EditText raw = a.input("RAW bytes / nội dung ban đầu", "");
        EditText draft = a.input("DRAFT bytes / nội dung ban đầu", "");
        EditText glossary = a.input("GLOSSARY bytes / nội dung ban đầu", "");
        EditText pronoun = a.input("PRONOUN tùy chọn", "");
        form.addView(a.fieldBlock("SERIES", series));
        form.addView(a.fieldBlock("VOLUME", volume));
        TextView packTitle = a.text("Chọn pack cụ thể (bắt buộc)", 12, a.TEXT, true);
        form.addView(packTitle, a.marginLP(-1, -2, 0, 4, 0, 0));
        RadioGroup packChoices = new RadioGroup(a);
        packChoices.setOrientation(LinearLayout.VERTICAL);
        final EditorialPackSelectionCandidate[] selected = {candidates.get(0)};
        for (int index = 0; index < candidates.size(); index++) {
            EditorialPackSelectionCandidate candidate = candidates.get(index);
            RadioButton choice = new RadioButton(a);
            choice.setText(candidate.selectionLabel() + "\nTương thích: DATA_COMPATIBLE • Integrity: VALID"
                    + " • Profile: " + candidate.trustedProfileVersion()
                    + " • Chờ chứng nhận • Execution đang khóa");
            choice.setTextColor(a.TEXT);
            choice.setSingleLine(false);
            choice.setTag(candidate);
            choice.setId(View.generateViewId());
            if (index == 0) choice.setChecked(true);
            packChoices.addView(choice, new LinearLayout.LayoutParams(-1, a.dp(68)));
        }
        packChoices.setOnCheckedChangeListener((group, checkedId) -> {
            View checked = group.findViewById(checkedId);
            if (checked != null && checked.getTag() instanceof EditorialPackSelectionCandidate candidate) selected[0] = candidate;
        });
        form.addView(packChoices, a.marginLP(-1, -2, 0, 8, 0, 0));
        form.addView(a.text("Setup chỉ lưu immutable metadata và input identity. Không mở model, không certify, không chạy.", 11, a.AMBER, false), a.marginLP(-1, -2, 0, 7, 0, 4));
        form.addView(a.fieldBlock("RAW (bắt buộc)", raw));
        form.addView(a.fieldBlock("DRAFT (bắt buộc)", draft));
        form.addView(a.fieldBlock("GLOSSARY (bắt buộc)", glossary));
        form.addView(a.fieldBlock("PRONOUN (tùy chọn; để trống = NONE)", pronoun));
        new AlertDialog.Builder(a).setTitle("Tạo project V5-SAFE.4")
                .setView(form).setPositiveButton("Xác nhận & lưu setup", (d, w) -> a.createEditorialProjectWithP4Binding(
                        series.getText().toString(), volume.getText().toString(), selected[0].packId(),
                        selected[0].packVersion(), raw.getText().toString(), draft.getText().toString(),
                        glossary.getText().toString(), pronoun.getText().toString()))
                .setNegativeButton("Hủy", null).show();
    }

    private List<EditorialPackSelectionCandidate> loadSelectablePacks() {
        try (TranslationRepository database = new TranslationRepository(a)) {
            EditorialPackStorageLayout storage = new EditorialPackStorageLayout(a.getFilesDir().toPath());
            return new EditorialPackSelectionPolicy(database, storage).listSelectable();
        } catch (RuntimeException error) {
            return List.of();
        }
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
