package com.ml.tblandroidtxt;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;

import java.util.List;

/** Draws the chunk-pair screens: the pairs before the run, the run, and the result with its per-pair decisions. */
final class EditorialPairPageFactory {
    private final MainActivity a;
    private final EditorialApiUiController c;
    private final EditorialPairUiController p;

    EditorialPairPageFactory(MainActivity activity, EditorialApiUiController controller) {
        a = activity;
        c = controller;
        p = controller.pair;
    }

    // ---- before the run

    void confirm(LinearLayout root) {
        LinearLayout card = a.sectionCard("✓", "Xem trước các cặp");
        EditorialPairPreview v = p.preview;
        if (v == null) { card.addView(text("Không còn gì để xem.", 14, a.MUTED, false)); root.addView(card); return; }
        card.addView(text(c.combo.name, 15, a.TEXT, true));
        card.addView(text("Nguồn: " + (EditorialApiCombo.sourceIsJob(c.combo) ? "job Dịch “" + c.combo.rawName + "”" : c.combo.rawName + " + " + c.combo.draftName), 13, a.MUTED, false),
                a.marginLP(-1, -2, 0, 2, 0, 6));
        card.addView(text(EditorialPairPresenter.previewTotals(v), 14, v.runnable() ? a.TEXT : a.AMBER, true));
        EditorialPairSourceLoader.References refs = p.references;
        String glossary = refs == null || refs.glossaryName.isEmpty() ? "Không dùng Glossary" : "Glossary: " + refs.glossaryName;
        String pronoun = refs == null || refs.pronounName.isEmpty() ? "Không dùng Pronoun" : "Pronoun: " + refs.pronounName;
        card.addView(text(glossary, 14, a.TEXT, false), a.marginLP(-1, -2, 0, 6, 0, 0));
        card.addView(text(pronoun, 14, a.TEXT, false));
        EditorialApiCombo.Settings settings = c.combo.settings();
        card.addView(text("Mỗi cặp một lượt gọi; không có bước kiểm riêng. Mô hình: " + EditorialApiUiController.effectiveModel(c.combo, SettingsStore.load(a)), 13, a.MUTED, false),
                a.marginLP(-1, -2, 0, 6, 0, 0));
        EditorialApiModelPricing pricing = new EditorialApiModelPricing(SettingsStore.load(a).provider);
        String model = EditorialApiUiController.effectiveModel(c.combo, SettingsStore.load(a));
        if (!v.rows.isEmpty()) {
            card.addView(text(EditorialPairPresenter.costLine(v, pricing.inputPerToken(model), pricing.outputPerToken(model), settings.maxUsdPerChapter), 13, a.TEXT, false),
                    a.marginLP(-1, -2, 0, 4, 0, 8));
        }
        for (String blocker : EditorialPairPresenter.blockers(v)) card.addView(text("✖ " + blocker, 13, a.RED, false), a.marginLP(-1, -2, 0, 2, 0, 2));
        for (String warning : EditorialPairPresenter.warnings(v)) card.addView(text("⚠ " + warning, 13, a.AMBER, false), a.marginLP(-1, -2, 0, 2, 0, 2));
        if (!c.error.isEmpty()) card.addView(text(c.error, 13, a.RED, false), a.marginLP(-1, -2, 0, 4, 0, 4));
        LinearLayout buttons = a.rowContainer();
        buttons.addView(a.secondaryButton("Đổi", x -> c.handleBack()), new LinearLayout.LayoutParams(0, a.dp(48), 1));
        buttons.addView(a.space(8, 1));
        Button run = a.primaryButton("Chạy", x -> p.startRun());
        run.setEnabled(v.runnable());
        run.setAlpha(v.runnable() ? 1f : 0.45f);
        buttons.addView(run, new LinearLayout.LayoutParams(0, a.dp(48), 1));
        card.addView(buttons, a.marginLP(-1, -2, 0, 12, 0, 0));
        root.addView(card, a.marginLP(-1, -2, 0, 0, 0, 10));

        if (!v.rows.isEmpty()) {
            LinearLayout pairs = a.sectionCard("▦", "Các cặp (" + v.rows.size() + ")");
            int shown = 0;
            for (EditorialPairPreview.Row row : v.rows) {
                if (shown++ >= 200) { pairs.addView(text("… và " + (v.rows.size() - 200) + " cặp nữa", 12, a.MUTED, false)); break; }
                pairs.addView(text(EditorialPairPresenter.previewRow(row), 13, row.missing || row.tooLong ? a.AMBER : a.TEXT, false), a.marginLP(-1, -2, 0, 0, 0, 2));
                if (!row.rawHead.isEmpty()) pairs.addView(text("RAW: " + row.rawHead, 12, a.MUTED, false), a.marginLP(-1, -2, 8, 0, 0, 0));
                if (!row.draftHead.isEmpty()) pairs.addView(text("DRAFT: " + row.draftHead, 12, a.MUTED, false), a.marginLP(-1, -2, 8, 0, 0, 4));
            }
            root.addView(pairs, a.marginLP(-1, -2, 0, 0, 0, 10));
        }
    }

    // ---- during the run

    void run(LinearLayout root) {
        LinearLayout card = a.sectionCard("…", "Đang biên tập từng cặp");
        card.addView(text(p.progress.isEmpty() ? "Đang chạy…" : p.progress, 15, a.TEXT, false));
        PairRun run = readRun();
        if (run != null) {
            card.addView(text(EditorialApiPresenter.usd(run.usd) + " • " + run.calls + " lượt gọi", 13, a.MUTED, false), a.marginLP(-1, -2, 0, 6, 0, 0));
            for (PairItem item : readItems(run.id)) {
                card.addView(text("Cặp " + item.ordinal + ": " + EditorialPairPresenter.pairState(item.state), 13, color(item.state), false), a.marginLP(-1, -2, 0, 0, 0, 2));
            }
        }
        card.addView(text("Có thể rời màn này; tiến độ được lưu sau từng cặp và không gửi lại cặp đã xong.", 12, a.MUTED, false), a.marginLP(-1, -2, 0, 8, 0, 0));
        card.addView(a.dangerButton("Hủy sau cặp hiện tại", v -> p.cancelRun()), a.marginLP(-1, a.dp(46), 0, 12, 0, 0));
        root.addView(card);
    }

    // ---- the result

    void result(LinearLayout root) {
        PairRun run = readRun();
        LinearLayout card = a.sectionCard("✎", "Kết quả theo cặp");
        if (run == null) {
            card.addView(text(c.error.isEmpty() ? "Không có kết quả để hiển thị." : c.error, 14, a.MUTED, false));
            card.addView(a.secondaryButton("Về danh sách", v -> c.showList()), a.marginLP(-1, a.dp(44), 0, 12, 0, 0));
            root.addView(card);
            return;
        }
        List<PairItem> items = readItems(run.id);
        boolean interrupted = EditorialPairPresenter.interrupted(run, items);
        card.addView(text(EditorialPairPresenter.runHeadline(run, EditorialPairRunService.isActive(run.id), interrupted), 17, a.TEXT, true));
        for (String line : EditorialPairPresenter.statusLines(run, items)) card.addView(text(line, 13, a.MUTED, false), a.marginLP(-1, -2, 0, 4, 0, 0));
        if (interrupted) card.addView(text("⚠ Ứng dụng đã đóng giữa chừng. Không có gì được gửi lại tự động; cặp đang gửi dở được coi là không rõ kết quả.", 13, a.AMBER, false), a.marginLP(-1, -2, 0, 6, 0, 0));
        if (!c.error.isEmpty()) card.addView(text(c.error, 13, a.RED, false), a.marginLP(-1, -2, 0, 4, 0, 0));

        EditorialPairRunService.ExportPlan plan = exportPlan(run.id);
        if (plan != null) card.addView(text("Xuất: " + plan.label, 13, plan.complete ? a.TEXT : a.AMBER, false), a.marginLP(-1, -2, 0, 8, 0, 0));
        LinearLayout actions = a.rowContainer();
        Button export = a.primaryButton(plan != null && plan.complete ? "Xuất bản ghép" : "Xuất bản tạm", v -> p.exportNow());
        export.setEnabled(plan != null);
        export.setAlpha(plan != null ? 1f : 0.45f);
        actions.addView(export, new LinearLayout.LayoutParams(0, a.dp(44), 1));
        boolean pending = false;
        for (PairItem i : items) if (i.state == PairState.IMPORTED) pending = true;
        if (pending && !EditorialPairRunService.isActive(run.id)) {
            actions.addView(a.space(8, 1));
            actions.addView(a.secondaryButton("Chạy tiếp", v -> p.resumeRun()), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        }
        card.addView(actions, a.marginLP(-1, -2, 0, 8, 0, 8));
        card.addView(a.secondaryButton("Về danh sách", v -> c.showList()), new LinearLayout.LayoutParams(-1, a.dp(44)));
        root.addView(card, a.marginLP(-1, -2, 0, 0, 0, 10));

        EditorialPairSnapshot snapshot = snapshot(run);
        LinearLayout pairs = a.sectionCard("▦", "Các cặp (" + items.size() + ")");
        for (PairItem item : items) pairs.addView(pairCard(run, item, snapshot), a.marginLP(-1, -2, 0, 0, 0, 8));
        root.addView(pairs, a.marginLP(-1, -2, 0, 0, 0, 10));

        LinearLayout tech = new LinearLayout(a);
        tech.setOrientation(LinearLayout.VERTICAL);
        tech.addView(a.secondaryButton((p.detailsOpen ? "▾ " : "▸ ") + EditorialPairPresenter.DIAGNOSTICS_TITLE, v -> p.toggleDetails()), new LinearLayout.LayoutParams(-1, a.dp(44)));
        if (p.detailsOpen) {
            TextView details = text(EditorialPairPresenter.detailsText(run, items), 11, a.MUTED, false);
            details.setTextIsSelectable(true);
            tech.addView(details, a.marginLP(-1, -2, 0, 8, 0, 0));
        }
        root.addView(tech);
    }

    private View pairCard(PairRun run, PairItem item, EditorialPairSnapshot snapshot) {
        LinearLayout box = a.card(12, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(10), a.dp(10), a.dp(10), a.dp(10));
        box.addView(text("Cặp " + item.ordinal + ": " + EditorialPairPresenter.pairState(item.state), 13, color(item.state), true));
        for (String line : EditorialPairPresenter.gateLines(item)) box.addView(text(line, 12, line.startsWith("Chặn") ? a.RED : a.AMBER, false), a.marginLP(-1, -2, 0, 2, 0, 0));
        if (item.state == PairState.WARN_REVIEW) {
            LinearLayout decide = a.rowContainer();
            decide.addView(a.primaryButton("Chấp nhận", v -> p.resolve(item.pairId, true)), new LinearLayout.LayoutParams(0, a.dp(40), 1));
            decide.addView(a.space(8, 1));
            decide.addView(a.secondaryButton("Loại", v -> p.resolve(item.pairId, false)), new LinearLayout.LayoutParams(0, a.dp(40), 1));
            box.addView(decide, a.marginLP(-1, -2, 0, 6, 0, 0));
        }
        String draft = snapshot == null || EditorialPairModels.ARM_WHOLE.equals(run.arm) ? null : draftOf(snapshot, item);
        if (draft != null && !item.candidateText.isEmpty()) {
            boolean open = p.expanded.contains(item.pairId);
            box.addView(a.secondaryButton(open ? "Ẩn thay đổi" : "Xem thay đổi so với DRAFT", v -> p.toggle(item.pairId)), a.marginLP(-1, a.dp(40), 0, 6, 0, 0));
            if (open) {
                box.addView(text(EditorialApiPresenter.diffSummary(draft, item.candidateText), 12, a.MUTED, false), a.marginLP(-1, -2, 0, 6, 0, 2));
                for (String[] row : EditorialApiPresenter.diffRows(draft, item.candidateText)) {
                    box.addView(text(row[0], 11, a.MUTED, true));
                    box.addView(text("DRAFT: " + (row[1].isEmpty() ? "(không có)" : row[1]), 12, a.RED), a.marginLP(-1, -2, 0, 2, 0, 0));
                    box.addView(text("Bản sửa: " + (row[2].isEmpty() ? "(đã bỏ)" : row[2]), 12, a.GREEN), a.marginLP(-1, -2, 0, 2, 0, 6));
                }
            }
        }
        if (!item.error.isEmpty() && item.state == PairState.UNKNOWN) box.addView(text("Lý do kỹ thuật: " + item.error, 11, a.MUTED, false), a.marginLP(-1, -2, 0, 4, 0, 0));
        return box;
    }

    // ---- reads (the page is built on the UI thread from the stored rows; nothing here can send)

    private PairRun readRun() {
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(a)) { return store.getRun(p.runId); }
    }

    private List<PairItem> readItems(long runId) {
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(a)) { return store.items(runId); }
    }

    private EditorialPairRunService.ExportPlan exportPlan(long runId) {
        try (SqliteEditorialPairRunStore store = new SqliteEditorialPairRunStore(a)) {
            return new EditorialPairRunService(store, new EditorialApiProvider() {
                @Override public com.ml.tblandroidtxt.editorial.api.EditorialApiFlow.StepResponse call(
                        com.ml.tblandroidtxt.editorial.api.EditorialApiFlow.Request r, String m, int o, long t) { throw new IllegalStateException("read only"); }

                @Override public void cancel() { }
            }, null, 1L).exportPlan(runId);
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    private static EditorialPairSnapshot snapshot(PairRun run) {
        try { return EditorialPairSnapshot.of(run); } catch (RuntimeException broken) { return null; }
    }

    private static String draftOf(EditorialPairSnapshot snapshot, PairItem item) {
        PairMap.Entry e = snapshot.map.entry(item.pairId);
        return e == null ? null : snapshot.map.draftText(e);
    }

    private int color(PairState s) {
        switch (s) {
            case ACCEPTED: return a.GREEN;
            case WARN_REVIEW: case RESERVE_FAILED: return a.AMBER;
            case STRUCTURE_BLOCKED: case REJECTED: case UNKNOWN: return a.RED;
            default: return a.TEXT;
        }
    }

    private TextView text(String value, int sp, int color) { return text(value, sp, color, false); }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = a.text(value, sp, color, bold);
        view.setSingleLine(false);
        return view;
    }

}
