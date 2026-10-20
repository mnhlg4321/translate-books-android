package com.ml.tblandroidtxt;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * The one picker for RAW, Glossary, Pronoun and combination choices: a near full-screen sheet with a search box, quick filters, a
 * circle on every row, the full file name (up to three lines, never cut mid-line) and a bottom bar with Hủy / Chọn (n).
 * Tapping a row only changes that row; the sheet closes on the main button, ✕ or Back, and ✕ / Back never change anything.
 */
final class FilePickerSheet {
    interface OnConfirm { void onConfirm(List<String> ids); }

    private final MainActivity a;
    private final FilePickerModel model;
    private final String confirmLabel;
    private final OnConfirm callback;
    private final Dialog dialog;
    /** Test hook: the sheet opened last. */
    static volatile FilePickerSheet last;
    private LinearLayout list;
    private TextView headline;
    private Button confirm;
    private final String title;

    private FilePickerSheet(MainActivity activity, String title, FilePickerModel model, String confirmLabel, OnConfirm callback) {
        this.a = activity;
        this.title = title;
        this.model = model;
        this.confirmLabel = confirmLabel;
        this.callback = callback;
        this.dialog = new Dialog(activity, activity.appearance != null && activity.appearance.light ? android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar : android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
    }

    /** Opens the sheet; {@code callback} runs only when the owner confirms. */
    static FilePickerSheet show(MainActivity a, String title, String subtitle, FilePickerModel model, String confirmLabel, OnConfirm callback) {
        FilePickerSheet s = new FilePickerSheet(a, title, model, confirmLabel, callback);
        s.build(subtitle);
        s.dialog.show();
        last = s;
        Window w = s.dialog.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (int) (a.getResources().getDisplayMetrics().heightPixels * 0.94f));
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return s;
    }

    private void build(String subtitle) {
        LinearLayout root = a.card(18, a.PANEL, a.BORDER);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(a.dp(14), a.dp(12), a.dp(14), a.dp(10));

        LinearLayout top = a.rowContainer();
        TextView t = a.text(title, 18, a.TEXT, true);
        t.setSingleLine(false);
        top.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = a.secondaryButton("✕", v -> dialog.cancel());
        close.setContentDescription("picker-close");
        top.addView(close, new LinearLayout.LayoutParams(a.dp(48), a.dp(44)));
        root.addView(top);

        headline = a.text("", 13, a.GREEN, true);
        headline.setSingleLine(false);
        headline.setContentDescription("picker-headline");
        root.addView(headline, a.marginLP(-1, -2, 0, 2, 0, 0));
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = a.text(subtitle, 12, a.MUTED, false);
            sub.setSingleLine(false);
            root.addView(sub, a.marginLP(-1, -2, 0, 0, 0, 0));
        }

        EditText search = a.input("Tìm theo tên…", "");
        search.setSingleLine(true);
        search.setContentDescription("picker-search");
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { model.setQuery(s.toString()); renderList(); }
        });
        root.addView(search, a.marginLP(-1, a.dp(44), 0, 8, 0, 0));

        LinearLayout filters = a.rowContainer();
        filters.addView(filterButton("Tất cả", FilePickerModel.Filter.ALL), new LinearLayout.LayoutParams(0, -2, 1));
        if (model.hasSuggestions()) { filters.addView(a.space(6, 1)); filters.addView(filterButton("Gợi ý", FilePickerModel.Filter.SUGGESTED), new LinearLayout.LayoutParams(0, -2, 1)); }
        filters.addView(a.space(6, 1));
        filters.addView(filterButton("Đã chọn", FilePickerModel.Filter.SELECTED), new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(filters, a.marginLP(-1, -2, 0, 6, 0, 0));

        if (model.isMultiple()) {
            LinearLayout bulk = a.rowContainer();
            bulk.addView(a.secondaryButton("Chọn tất cả (đang lọc)", v -> { model.selectAllVisible(); renderList(); }), new LinearLayout.LayoutParams(0, -2, 1));
            bulk.addView(a.space(6, 1));
            bulk.addView(a.secondaryButton("Bỏ chọn hết", v -> { model.clearAll(); renderList(); }), new LinearLayout.LayoutParams(0, -2, 1));
            root.addView(bulk, a.marginLP(-1, -2, 0, 6, 0, 0));
        }

        ScrollView scroll = a.scroll();
        list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, a.marginLP(-1, 0, 0, 8, 0, 0));
        ((LinearLayout.LayoutParams) scroll.getLayoutParams()).weight = 1;

        LinearLayout bar = a.rowContainer();
        bar.addView(a.secondaryButton("Hủy", v -> dialog.cancel()), new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(a.space(8, 1));
        confirm = a.primaryButton(confirmLabel, v -> {
            List<String> ids = model.selectedIds();
            dialog.dismiss();
            callback.onConfirm(ids);
        });
        confirm.setContentDescription("picker-confirm");
        bar.addView(confirm, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(bar, a.marginLP(-1, -2, 0, 8, 0, 0));

        dialog.setContentView(root);
        dialog.setOnCancelListener(d -> model.cancel());
        renderList();
    }

    private Button filterButton(String label, FilePickerModel.Filter f) {
        Button b = a.secondaryButton(label, v -> { model.setFilter(f); renderList(); });
        b.setTextSize(12);
        b.setContentDescription("filter:" + label);
        return b;
    }

    private void renderList() {
        headline.setText(model.headline());
        confirm.setText(model.isMultiple() ? confirmLabel + " (" + model.selectedCount() + ")" : confirmLabel);
        confirm.setEnabled(model.canConfirm());
        confirm.setAlpha(model.canConfirm() ? 1f : 0.45f);
        list.removeAllViews();
        if (model.allowsNone()) list.addView(row(FilePickerModel.NONE_ID, "⊘  Không dùng", "", false, ""), a.marginLP(-1, -2, 0, 0, 0, 6));
        List<FilePickerModel.Item> rows = model.visible();
        for (FilePickerModel.Item i : rows) list.addView(row(i.id, i.name, i.meta, i.suggested, i.suggestedLabel), a.marginLP(-1, -2, 0, 0, 0, 6));
        if (rows.isEmpty() && !model.allowsNone()) {
            TextView empty = a.text("Không có mục nào khớp.", 14, a.MUTED, false);
            list.addView(empty, a.marginLP(-1, -2, 0, 12, 0, 12));
        }
    }

    private View row(String id, String name, String meta, boolean suggested, String suggestedLabel) {
        boolean on = model.isSelected(id);
        LinearLayout row = a.card(12, on ? a.CARD : a.FIELD, on ? a.BLUE : a.BORDER);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(a.dp(12), a.dp(10), a.dp(10), a.dp(10));
        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView n = a.text(name, 15, a.TEXT, on);
        n.setSingleLine(false);
        n.setMaxLines(6);
        n.setEllipsize(TextUtils.TruncateAt.END);
        n.setContentDescription("name:" + name);
        texts.addView(n);
        String second = (suggested && !suggestedLabel.isEmpty() ? suggestedLabel + (meta.isEmpty() ? "" : " · ") : "") + meta;
        if (!second.isEmpty()) {
            TextView m = a.text(second, 12, suggested ? a.GREEN : a.MUTED, false);
            m.setSingleLine(false);
            texts.addView(m, a.marginLP(-1, -2, 0, 2, 0, 0));
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        TextView mark = a.text(model.isMultiple() ? (on ? "✓" : "○") : (on ? "◉" : "○"), 22, on ? a.BLUE : a.MUTED, true);
        mark.setGravity(Gravity.CENTER);
        row.addView(mark, new LinearLayout.LayoutParams(a.dp(40), -2));
        row.setContentDescription((on ? "đã chọn: " : "chưa chọn: ") + name);
        row.setOnClickListener(v -> { model.toggle(id); renderList(); });
        row.setOnLongClickListener(v -> { Toast.makeText(a, name, Toast.LENGTH_LONG).show(); return true; });
        return row;
    }

    /** Test hook: the dialog that was opened. */
    Dialog dialog() { return dialog; }
}
