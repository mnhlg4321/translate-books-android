package com.ml.tblandroidtxt;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;

import java.util.List;

/** Draws the Biên tập tab from {@link EditorialApiUiController} state, with the wording of {@link EditorialApiPresenter}. */
final class EditorialApiPageFactory {
    private final MainActivity a;
    private final EditorialApiUiController c;

    EditorialApiPageFactory(MainActivity activity, EditorialApiUiController controller) {
        a = activity;
        c = controller;
    }

    View build() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        switch (c.screen) {
            case COMBO: comboScreen(root); break;
            case CONFIRM: confirmScreen(root); break;
            case PROGRESS: progressScreen(root); break;
            case RESULT: resultScreen(root); break;
            default: listScreen(root);
        }
        return scroll;
    }

    // ---- list ----

    private void listScreen(LinearLayout root) {
        LinearLayout intro = a.sectionCard("✎", EditorialApiPresenter.TAB_TITLE);
        intro.addView(wrapped("Chọn bản gốc (RAW) và bản dịch nháp (DRAFT), kèm Glossary và Pronoun nếu có. Ứng dụng nhờ model sửa bản nháp cho đúng và hay hơn; chế độ Kỹ kiểm thêm một lượt.", 14, a.TEXT));
        intro.addView(a.primaryButton("+ Tổ hợp mới", v -> c.newCombo()), a.marginLP(-1, a.dp(48), 0, 12, 0, 0));
        root.addView(intro);

        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(a)) {
            List<EditorialApiCombo> combos = store.listCombos();
            if (combos.isEmpty()) {
                TextView empty = wrapped("Chưa có tổ hợp nào. Bấm “+ Tổ hợp mới” để bắt đầu.", 14, a.MUTED);
                empty.setGravity(android.view.Gravity.CENTER);
                root.addView(empty, new LinearLayout.LayoutParams(-1, a.dp(100)));
            }
            for (EditorialApiCombo combo : combos) {
                EditorialApiPresenter.ComboRow row = EditorialApiPresenter.row(combo, store.latestRun(combo.id));
                root.addView(comboCard(row), a.marginLP(-1, -2, 0, 0, 0, 10));
            }
        }
        root.addView(legacySection(), a.marginLP(-1, -2, 0, 6, 0, 0));
    }

    private View comboCard(EditorialApiPresenter.ComboRow row) {
        LinearLayout box = a.card(14, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        box.addView(wrapped(row.title, 16, a.TEXT, true));
        box.addView(wrapped(row.status, 12, a.MUTED), a.marginLP(-1, -2, 0, 4, 0, 8));
        LinearLayout main = a.rowContainer();
        main.addView(a.primaryButton("Chạy", v -> c.runFromList(row.comboId)), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        if (row.hasResult) {
            main.addView(a.space(8, 1));
            main.addView(a.secondaryButton("Xem kết quả", v -> c.openResult(row.comboId)), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        }
        box.addView(main);
        LinearLayout more = a.rowContainer();
        more.addView(a.secondaryButton("Sửa", v -> c.openCombo(row.comboId)), new LinearLayout.LayoutParams(0, a.dp(40), 1));
        more.addView(a.space(8, 1));
        more.addView(a.secondaryButton("Nhân bản", v -> c.duplicate(row.comboId)), new LinearLayout.LayoutParams(0, a.dp(40), 1));
        more.addView(a.space(8, 1));
        more.addView(a.secondaryButton("Xóa", v -> c.confirmDelete(row.comboId, row.title)), new LinearLayout.LayoutParams(0, a.dp(40), 1));
        box.addView(more, a.marginLP(-1, -2, 0, 8, 0, 0));
        return box;
    }

    private View legacySection() {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(a.secondaryButton((c.legacyOpen ? "▾ " : "▸ ") + EditorialApiPresenter.LEGACY_SECTION_TITLE, v -> c.toggleLegacy()),
                new LinearLayout.LayoutParams(-1, a.dp(44)));
        if (c.legacyOpen) {
            LinearLayout inner = new LinearLayout(a);
            inner.setOrientation(LinearLayout.VERTICAL);
            new EditorialPageFactory(a).populate(inner);
            box.addView(inner, a.marginLP(-1, -2, 0, 8, 0, 0));
        }
        return box;
    }

    // ---- combo ----

    private void comboScreen(LinearLayout root) {
        EditorialApiCombo combo = c.combo;
        EditorialApiCombo.Settings settings = combo.settings();
        LinearLayout card = a.sectionCard("✎", combo.id > 0 ? "Sửa tổ hợp" : "Tổ hợp mới");

        EditText name = a.input("Tên (để trống: tự đặt theo các file)", combo.name);
        card.addView(a.fieldBlock("TÊN", name));

        card.addView(fileRow("RAW — bản gốc", combo.rawName, true));
        card.addView(fileRow("DRAFT — bản dịch nháp", combo.draftName, false));

        card.addView(referenceRow("GLOSSARY", combo.glossaryId.isEmpty() ? "Không dùng" : c.glossaryName().isEmpty() ? "Không còn trong thư viện" : c.glossaryName(),
                v -> c.chooseGlossary()));
        card.addView(referenceRow("PRONOUN", combo.pronounId.isEmpty() ? "Không dùng" : c.pronounName().isEmpty() ? "Không còn trong thư viện" : c.pronounName(),
                v -> c.choosePronoun()));

        TextView modeLabel = a.text("CHẾ ĐỘ", 11, a.MUTED, true);
        card.addView(modeLabel, a.marginLP(-1, -2, 0, 4, 0, 4));
        RadioGroup modes = new RadioGroup(a);
        modes.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton quick = radio("Nhanh", settings.mode == EditorialApiContract.Mode.QUICK);
        RadioButton thorough = radio("Kỹ", settings.mode == EditorialApiContract.Mode.THOROUGH);
        modes.addView(quick);
        modes.addView(thorough);
        card.addView(modes);
        card.addView(wrapped("Nhanh: chỉ biên tập (1 lượt gọi). Kỹ: biên tập, kiểm, và kiểm lại nếu có sửa.", 12, a.MUTED), a.marginLP(-1, -2, 0, 2, 0, 10));

        EditText model = a.input("Mặc định theo Cài đặt", settings.model);
        card.addView(a.fieldBlock("MODEL (không bắt buộc)", model));
        EditText cap = a.decimal("0.10", settings.maxUsdPerChapter.toPlainString());
        card.addView(a.fieldBlock("TRẦN CHI PHÍ MỖI CHƯƠNG (USD)", cap));

        if (!c.error.isEmpty()) card.addView(wrapped(c.error, 13, a.RED), a.marginLP(-1, -2, 0, 0, 0, 8));

        card.addView(a.primaryButton("Tiếp tục", v -> c.saveAndContinue(name.getText().toString(),
                thorough.isChecked() ? EditorialApiContract.Mode.THOROUGH : EditorialApiContract.Mode.QUICK,
                model.getText().toString(), cap.getText().toString())), new LinearLayout.LayoutParams(-1, a.dp(48)));
        card.addView(a.secondaryButton("Quay lại", v -> c.showList()), a.marginLP(-1, a.dp(44), 0, 8, 0, 0));
        root.addView(card);
    }

    private RadioButton radio(String text, boolean checked) {
        RadioButton button = new RadioButton(a);
        button.setText(text);
        button.setTextColor(a.TEXT);
        button.setChecked(checked);
        button.setId(View.generateViewId());
        return button;
    }

    private View fileRow(String label, String fileName, boolean raw) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, a.dp(10));
        box.addView(a.text(label, 11, a.MUTED, true));
        box.addView(wrapped(fileName.isEmpty() ? "Chưa chọn" : fileName, 14, fileName.isEmpty() ? a.MUTED : a.TEXT), a.marginLP(-1, -2, 0, 3, 0, 6));
        LinearLayout buttons = a.rowContainer();
        buttons.addView(a.secondaryButton("Chọn file", v -> c.pickFile(raw)), new LinearLayout.LayoutParams(0, a.dp(40), 1));
        buttons.addView(a.space(8, 1));
        buttons.addView(a.secondaryButton("Từ bản dịch gần đây", v -> c.pickRecent(raw)), new LinearLayout.LayoutParams(0, a.dp(40), 1));
        box.addView(buttons);
        return box;
    }

    private View referenceRow(String label, String value, View.OnClickListener change) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, a.dp(10));
        box.addView(a.text(label, 11, a.MUTED, true));
        LinearLayout row = a.rowContainer();
        row.addView(wrapped(value, 14, a.TEXT), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(a.secondaryButton("Đổi", change), new LinearLayout.LayoutParams(a.dp(90), a.dp(40)));
        box.addView(row, a.marginLP(-1, -2, 0, 3, 0, 0));
        return box;
    }

    // ---- confirmation ----

    private void confirmScreen(LinearLayout root) {
        EditorialApiPresenter.Confirmation v = c.confirmation;
        LinearLayout card = a.sectionCard("✓", "Xác nhận trước khi chạy");
        if (v == null) { card.addView(wrapped("Không còn gì để xác nhận.", 14, a.MUTED)); root.addView(card); return; }
        card.addView(wrapped(v.comboName, 15, a.TEXT, true));
        card.addView(fileBlock("RAW", v.rawName, v.rawChars, v.rawHead));
        card.addView(fileBlock("DRAFT", v.draftName, v.draftChars, v.draftHead));
        card.addView(wrapped(v.glossaryLine, 14, a.TEXT), a.marginLP(-1, -2, 0, 4, 0, 0));
        card.addView(wrapped(v.pronounLine, 14, a.TEXT), a.marginLP(-1, -2, 0, 2, 0, 0));
        card.addView(wrapped(v.modeLine, 14, a.TEXT), a.marginLP(-1, -2, 0, 8, 0, 0));
        card.addView(wrapped(v.modelLine, 14, a.TEXT), a.marginLP(-1, -2, 0, 2, 0, 0));
        card.addView(wrapped(v.costLine, 14, a.TEXT), a.marginLP(-1, -2, 0, 2, 0, 8));
        for (String warning : v.warnings) card.addView(wrapped("⚠ " + warning, 13, a.AMBER), a.marginLP(-1, -2, 0, 2, 0, 2));
        for (String blocker : v.blockers) card.addView(wrapped("✖ " + blocker, 13, a.RED), a.marginLP(-1, -2, 0, 2, 0, 2));
        if (!c.error.isEmpty()) card.addView(wrapped(c.error, 13, a.RED), a.marginLP(-1, -2, 0, 4, 0, 4));
        LinearLayout buttons = a.rowContainer();
        buttons.addView(a.secondaryButton("Đổi", x -> c.handleBack()), new LinearLayout.LayoutParams(0, a.dp(48), 1));
        buttons.addView(a.space(8, 1));
        android.widget.Button run = a.primaryButton("Chạy", x -> c.startRun());
        run.setEnabled(v.canRun());
        run.setAlpha(v.canRun() ? 1f : 0.45f);
        buttons.addView(run, new LinearLayout.LayoutParams(0, a.dp(48), 1));
        card.addView(buttons, a.marginLP(-1, -2, 0, 12, 0, 0));
        root.addView(card);
    }

    private View fileBlock(String role, String name, int chars, String head) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, a.dp(8), 0, 0);
        box.addView(wrapped(role + ": " + name + " • " + chars + " ký tự", 13, a.TEXT, true));
        box.addView(wrapped(head.isEmpty() ? "(trống)" : head, 12, a.MUTED), a.marginLP(-1, -2, 0, 2, 0, 0));
        return box;
    }

    // ---- progress ----

    private void progressScreen(LinearLayout root) {
        LinearLayout card = a.sectionCard("…", "Đang biên tập");
        card.addView(wrapped(c.progress.isEmpty() ? "Đang chạy…" : c.progress, 15, a.TEXT));
        card.addView(wrapped("Có thể rời màn này; kết quả được lưu sau từng bước.", 12, a.MUTED), a.marginLP(-1, -2, 0, 8, 0, 0));
        card.addView(a.dangerButton("Hủy", v -> c.cancelRun()), a.marginLP(-1, a.dp(46), 0, 12, 0, 0));
        root.addView(card);
    }

    // ---- result ----

    private void resultScreen(LinearLayout root) {
        EditorialApiRun run;
        try (SqliteEditorialApiStore store = new SqliteEditorialApiStore(a)) { run = store.getRun(c.runId); }
        LinearLayout card = a.sectionCard("✎", "Kết quả");
        if (run == null) {
            card.addView(wrapped(c.error.isEmpty() ? "Không có kết quả để hiển thị." : c.error, 14, a.MUTED));
            card.addView(a.secondaryButton("Về danh sách", v -> c.showList()), a.marginLP(-1, a.dp(44), 0, 12, 0, 0));
            root.addView(card);
            return;
        }
        if (run.finished()) c.checkStale(run);
        EditorialApiPresenter.Result r = EditorialApiPresenter.result(run, c.staleParts);
        card.addView(wrapped(r.headline, 17, a.TEXT, true));
        if (!r.detail.isEmpty()) card.addView(wrapped(r.detail, 14, a.MUTED), a.marginLP(-1, -2, 0, 4, 0, 4));
        if (!c.error.isEmpty()) card.addView(wrapped(c.error, 13, a.RED));
        if (!r.staleParts.isEmpty()) card.addView(wrapped("⚠ " + EditorialApiPresenter.staleMessage(r.staleParts), 13, a.AMBER), a.marginLP(-1, -2, 0, 4, 0, 4));

        LinearLayout actions = a.rowContainer();
        android.widget.Button export = a.primaryButton("Xuất TXT", v -> c.exportFinal());
        export.setEnabled(r.canExport);
        export.setAlpha(r.canExport ? 1f : 0.45f);
        actions.addView(export, new LinearLayout.LayoutParams(0, a.dp(44), 1));
        actions.addView(a.space(8, 1));
        actions.addView(a.secondaryButton("Chạy lại", v -> c.rerun()), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        card.addView(actions, a.marginLP(-1, -2, 0, 8, 0, 8));
        root.addView(card, a.marginLP(-1, -2, 0, 0, 0, 10));

        if (!r.review.isEmpty()) {
            LinearLayout review = a.sectionCard("!", "Cần xem (" + r.review.size() + ")");
            for (EditorialApiPresenter.Review item : r.review) review.addView(reviewItem(item), a.marginLP(-1, -2, 0, 0, 0, 8));
            root.addView(review, a.marginLP(-1, -2, 0, 0, 0, 10));
        }
        if (!r.flags.isEmpty() || !r.notes.isEmpty()) {
            LinearLayout flags = a.sectionCard("⚑", "Lưu ý");
            for (String flag : r.flags) flags.addView(wrapped("• " + flag, 13, a.TEXT), a.marginLP(-1, -2, 0, 0, 0, 4));
            for (String note : r.notes) flags.addView(wrapped("• " + note, 13, a.MUTED), a.marginLP(-1, -2, 0, 0, 0, 4));
            root.addView(flags, a.marginLP(-1, -2, 0, 0, 0, 10));
        }

        LinearLayout text = a.sectionCard("¶", "Bản cuối");
        TextView body = wrapped(r.finalText.isEmpty() ? "(trống)" : r.finalText, 14, a.TEXT);
        body.setTextIsSelectable(true);
        text.addView(body);
        if (r.canCompare) {
            text.addView(a.secondaryButton(c.showDiff ? "Ẩn so sánh với DRAFT" : "So sánh với DRAFT", v -> c.toggleDiff()),
                    a.marginLP(-1, a.dp(44), 0, 10, 0, 0));
            if (c.showDiff) {
                text.addView(wrapped(EditorialApiPresenter.diffSummary(run.draftText, run.finalText), 12, a.MUTED), a.marginLP(-1, -2, 0, 8, 0, 4));
                for (String[] row : EditorialApiPresenter.diffRows(run.draftText, run.finalText)) text.addView(diffRow(row), a.marginLP(-1, -2, 0, 0, 0, 8));
            }
        }
        root.addView(text, a.marginLP(-1, -2, 0, 0, 0, 10));

        LinearLayout tech = new LinearLayout(a);
        tech.setOrientation(LinearLayout.VERTICAL);
        tech.addView(a.secondaryButton((c.technicalOpen ? "▾ " : "▸ ") + "Chi tiết kỹ thuật", v -> c.toggleTechnical()), new LinearLayout.LayoutParams(-1, a.dp(44)));
        if (c.technicalOpen) {
            TextView details = wrapped(r.technical, 11, a.MUTED);
            details.setTextIsSelectable(true);
            tech.addView(details, a.marginLP(-1, -2, 0, 8, 0, 0));
        }
        root.addView(tech, a.marginLP(-1, -2, 0, 0, 0, 10));
        root.addView(a.secondaryButton("Về danh sách", v -> c.showList()), new LinearLayout.LayoutParams(-1, a.dp(44)));
    }

    private View reviewItem(EditorialApiPresenter.Review item) {
        LinearLayout box = a.card(12, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(10), a.dp(10), a.dp(10), a.dp(10));
        box.addView(wrapped(item.kind, 13, a.AMBER, true));
        if (!item.rawQuote.isEmpty()) box.addView(wrapped("RAW: " + item.rawQuote, 13, a.MUTED), a.marginLP(-1, -2, 0, 4, 0, 0));
        if (!item.viQuote.isEmpty()) box.addView(wrapped("Bản cuối: " + item.viQuote, 13, a.TEXT), a.marginLP(-1, -2, 0, 2, 0, 0));
        if (!item.fix.isEmpty()) box.addView(wrapped("Gợi ý: " + item.fix, 13, a.TEXT), a.marginLP(-1, -2, 0, 2, 0, 0));
        return box;
    }

    private View diffRow(String[] row) {
        LinearLayout box = a.card(10, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8));
        box.addView(wrapped(row[0], 11, a.MUTED, true));
        box.addView(wrapped("DRAFT: " + (row[1].isEmpty() ? "(không có)" : row[1]), 12, a.RED), a.marginLP(-1, -2, 0, 3, 0, 0));
        box.addView(wrapped("Bản cuối: " + (row[2].isEmpty() ? "(đã bỏ)" : row[2]), 12, a.GREEN), a.marginLP(-1, -2, 0, 3, 0, 0));
        return box;
    }

    // ---- small helpers ----

    private TextView wrapped(String text, int sp, int color) { return wrapped(text, sp, color, false); }

    private TextView wrapped(String text, int sp, int color, boolean bold) {
        TextView view = a.text(text, sp, color, bold);
        view.setSingleLine(false);
        return view;
    }
}
