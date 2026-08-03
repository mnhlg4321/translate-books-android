package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistryMetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Read-only persisted-pack list/detail surface. It has no importer or mutation callback. */
final class EditorialPackManagementPageFactory {
    private final MainActivity a;

    EditorialPackManagementPageFactory(MainActivity activity) { a = activity; }

    void show() {
        AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle("Editorial Packs • chỉ đọc")
                .setView(build())
                .setNegativeButton("Đóng", null)
                .create();
        dialog.show();
    }

    View build() {
        ScrollView scroll = a.scroll();
        scroll.setFillViewport(true);
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        LinearLayout intro = a.card(14, a.PANEL, a.BORDER);
        intro.setOrientation(LinearLayout.VERTICAL);
        intro.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        intro.addView(a.text("Editorial Packs • persistent imported packs", 17, a.TEXT, true));
        TextView note = a.text("Màn hình này chỉ đọc registry. Built-in SAFE4 không được seed vào persistent registry và không xuất hiện ở đây.", 12, a.MUTED, false);
        note.setSingleLine(false);
        intro.addView(note, a.marginLP(-1, -2, 0, 4, 0, 0));
        intro.addView(a.text("READ ONLY • không import, certify, activate, delete hoặc replace", 11, a.CYAN, true));
        root.addView(intro, a.marginLP(-1, -2, 0, 0, 0, 10));

        TextView loading = a.text("Đang tải registry…", 13, a.MUTED, false);
        loading.setGravity(Gravity.CENTER);
        root.addView(loading, new LinearLayout.LayoutParams(-1, a.dp(64)));
        EditorialPackListPresenter.Result result = loadRegistry();
        root.removeView(loading);
        if (result.state() == EditorialPackListPresenter.State.ERROR) {
            TextView error = a.text("Không đọc được registry: " + result.errorMessage(), 13, a.RED, false);
            error.setSingleLine(false);
            root.addView(error, a.marginLP(-1, -2, 0, 0, 0, 10));
            return scroll;
        }
        if (result.state() == EditorialPackListPresenter.State.EMPTY) {
            TextView empty = a.text("Registry đang trống. Chưa có persisted imported pack nào; không có thao tác nào được thực hiện.", 13, a.MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setSingleLine(false);
            root.addView(empty, new LinearLayout.LayoutParams(-1, a.dp(120)));
            return scroll;
        }
        String previousPackId = "";
        for (EditorialPackUiModel model : result.packs()) {
            if (!model.packId().equals(previousPackId)) {
                TextView group = a.text("PACK ID  " + model.packId(), 11, a.MUTED, true);
                root.addView(group, a.marginLP(-1, a.dp(2), 0, 4, 0, 0));
                previousPackId = model.packId();
            }
            root.addView(packCard(model), a.marginLP(-1, -2, 0, 0, 0, 8));
        }
        return scroll;
    }

    private EditorialPackListPresenter.Result loadRegistry() {
        try (TranslationRepository database = new TranslationRepository(a)) {
            EditorialPackStorageLayout storage = new EditorialPackStorageLayout(a.getFilesDir().toPath());
            EditorialPackRegistry registry = new SqliteEditorialPackRegistry(database, storage);
            return new EditorialPackListPresenter(registry).load();
        } catch (RuntimeException error) {
            return new EditorialPackListPresenter(new FailingRegistry(error)).load();
        }
    }

    private View packCard(EditorialPackUiModel model) {
        LinearLayout card = a.card(12, a.FIELD, a.BORDER);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(a.dp(11), a.dp(9), a.dp(11), a.dp(9));
        TextView title = a.text(model.displayName(), 15, a.TEXT, true);
        title.setSingleLine(false);
        card.addView(title);
        card.addView(a.text(model.packId() + " • v" + model.version(), 12, a.MUTED, false));
        card.addView(a.text("hash " + model.shortHash() + " • READ ONLY", 11, a.CYAN, true));
        TextView compatibility = a.text(model.compatibilityLabel(), 12, a.AMBER, true);
        compatibility.setSingleLine(false);
        card.addView(compatibility, a.marginLP(-1, -2, 0, 2, 0, 0));
        TextView storage = a.text(model.storageLabel(), 12, a.MUTED, true);
        storage.setSingleLine(false);
        card.addView(storage);
        card.addView(a.text("contract " + model.contractVersion() + " • schema " + model.schemaVersion(), 11, a.MUTED, false));
        card.addView(a.text("storedAt " + model.createdAt(), 11, a.MUTED, false));
        card.setOnClickListener(v -> showDetail(model));
        return card;
    }

    private void showDetail(EditorialPackUiModel model) {
        ScrollView scroll = a.scroll();
        scroll.setFillViewport(true);
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        addDetail(root, "Manifest identity", model.packId() + " • v" + model.version() + "\n" + model.displayName());
        addDetail(root, "Canonical hash", model.canonicalHash());
        addDetail(root, "Contract / schema", model.contractVersion() + " / " + model.schemaVersion());
        addDetail(root, "Minimum engine", model.minimumEngineVersion());
        addDetail(root, "Storage", model.storageLabel() + "\n" + model.storageState().name());
        addDetail(root, "Compatibility", model.compatibilityClass().name() + "\n" + model.compatibilityLabel());
        addDetail(root, "Machine-contract fingerprint", model.machineContractFingerprint());
        addDetail(root, "Required capabilities", join(model.requiredCapabilities()));
        addDetail(root, "Missing capabilities", join(model.missingCapabilities()));
        addDetail(root, "Integrity", model.integrityLabel() + (model.integrityReason().isEmpty() ? "" : "\n" + model.integrityReason()));
        addDetail(root, "Latest compatibility result", model.compatibilityEvaluatedAt() == 0 ? "Chưa có kết quả" : "evaluatedAt " + model.compatibilityEvaluatedAt() + " • engine " + model.engineVersionUsed());
        addDetail(root, "Import / validation", "createdAt " + model.createdAt() + "\nvalidatedAt " + model.validatedAt());
        if (!model.blockedReason().isEmpty()) addDetail(root, "Blocked reason", model.blockedReason());
        LinearLayout files = a.card(10, a.FIELD, a.BORDER);
        files.setOrientation(LinearLayout.VERTICAL);
        files.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
        files.addView(a.text("Three file roles", 13, a.TEXT, true));
        for (EditorialPackUiModel.FileRow file : model.files()) {
            TextView line = a.text(file.role() + " • " + file.path() + " • " + file.byteLength() + " bytes\nsha256 " + file.sha256(), 11, a.TEXT, false);
            line.setSingleLine(false);
            files.addView(line, a.marginLP(-1, -2, 0, 5, 0, 0));
        }
        root.addView(files);
        new AlertDialog.Builder(a).setTitle("Pack detail • READ ONLY").setView(scroll).setNegativeButton("Đóng", null).show();
    }

    private void addDetail(LinearLayout root, String title, String value) {
        LinearLayout block = a.card(10, a.PANEL, a.BORDER);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(a.dp(10), a.dp(7), a.dp(10), a.dp(7));
        block.addView(a.text(title, 11, a.MUTED, true));
        TextView text = a.text(value == null || value.isEmpty() ? "—" : value, 12, a.TEXT, false);
        text.setSingleLine(false);
        block.addView(text);
        root.addView(block, a.marginLP(-1, -2, 0, 0, 0, 6));
    }

    private static String join(java.util.Set<String> values) {
        if (values == null || values.isEmpty()) return "Không có";
        ArrayList<String> sorted = new ArrayList<>(values); sorted.sort(Comparator.naturalOrder());
        return String.join(", ", sorted);
    }

    private static final class FailingRegistry implements EditorialPackRegistry {
        private final RuntimeException error;
        FailingRegistry(RuntimeException error) { this.error = error; }
        public java.util.Optional<com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest> findByHash(String hash) { throw error; }
        public java.util.Optional<com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest> findByIdentity(String id, String version) { throw error; }
        public List<com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest> list() { throw error; }
    }
}
