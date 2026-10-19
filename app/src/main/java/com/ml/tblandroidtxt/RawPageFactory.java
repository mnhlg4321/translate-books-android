package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Library > RAW: import many TXT files into the app, list them by chapter number, view / rename / replace / delete. */
class RawPageFactory {
    private final MainActivity a;

    RawPageFactory(MainActivity activity) { a = activity; }

    View buildRawPage() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        LinearLayout panel = a.sectionCard("📄", "RAW");
        TextView desc = a.text("Nhập nhiều TXT RAW một lần; app giữ bản sao nên không cần chọn lại tệp hay mất quyền truy cập. Sắp theo số chương ở đầu tên tệp (025_… là chương 25).", 13, a.MUTED, false);
        desc.setSingleLine(false);
        panel.addView(desc, a.marginLP(-1, -2, 0, 0, 0, 10));
        panel.addView(a.primaryButton("+ Nhập RAW", v -> a.chooseRawFiles()), a.marginLP(-1, a.dp(48), 0, 0, 0, 8));

        List<LibraryRawStore.Item> all = LibraryRawStore.list(a);
        LibrarySelectionController selection = a.bulkUi.controller(LibraryBulkUi.TAB_RAW);
        EditText search = a.input("Tìm theo tên", a.rawFilter);
        search.setSingleLine(true);
        search.setContentDescription("raw-search");
        panel.addView(search, a.marginLP(-1, a.dp(44), 0, 0, 0, 8));

        LinearLayout headerSlot = new LinearLayout(a);
        headerSlot.setOrientation(LinearLayout.VERTICAL);
        panel.addView(headerSlot, a.marginLP(-1, -2, 0, 0, 0, 8));
        TextView summary = a.text("", 13, a.GREEN, true);
        panel.addView(summary, a.marginLP(-1, -2, 0, 0, 0, 10));
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        panel.addView(list);
        root.addView(panel);

        Runnable render = () -> {
            List<LibraryRawStore.Item> shown = filter(all, a.rawFilter);
            list.removeAllViews();
            headerSlot.removeAllViews();
            List<String> ids = new ArrayList<>();
            for (LibraryRawStore.Item i : shown) ids.add(i.id);
            summary.setText(all.size() + " mục" + (shown.size() != all.size() ? " · hiện " + shown.size() : ""));
            if (selection.isActive()) headerSlot.addView(a.bulkUi.selectionHeader(LibraryBulkUi.TAB_RAW, ids));
            else if (!all.isEmpty()) headerSlot.addView(a.bulkUi.selectButton(LibraryBulkUi.TAB_RAW), new LinearLayout.LayoutParams(-2, a.dp(40)));
            if (shown.isEmpty()) {
                TextView empty = a.text(all.isEmpty() ? "Chưa có RAW. Bấm Nhập RAW để chọn nhiều tệp TXT." : "Không có RAW khớp \"" + a.rawFilter + "\".", 14, a.MUTED, false);
                empty.setSingleLine(false);
                list.addView(empty, a.marginLP(-1, -2, 0, 12, 0, 12));
                return;
            }
            for (LibraryRawStore.Item item : shown) list.addView(card(item, selection), a.marginLP(-1, -2, 0, 0, 0, 10));
        };
        render.run();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { a.rawFilter = s.toString(); render.run(); }
        });
        return scroll;
    }

    static List<LibraryRawStore.Item> filter(List<LibraryRawStore.Item> all, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return all;
        ArrayList<LibraryRawStore.Item> out = new ArrayList<>();
        for (LibraryRawStore.Item i : all) if (i.originalName.toLowerCase(Locale.ROOT).contains(q) || i.name.toLowerCase(Locale.ROOT).contains(q)) out.add(i);
        return out;
    }

    static String meta(LibraryRawStore.Item i) {
        return i.chapterLabel() + " · " + String.format(Locale.US, "%,d", i.chars) + " ký tự" + (i.encoding.isEmpty() ? "" : " · " + i.encoding);
    }

    private View card(LibraryRawStore.Item item, LibrarySelectionController selection) {
        String title = item.name.equals(LibraryRawStore.stripExtension(item.originalName)) ? item.originalName : item.name + "  (" + item.originalName + ")";
        if (selection.isActive()) return a.bulkUi.selectableCard(LibraryBulkUi.TAB_RAW, item.id, title, meta(item), a.BORDER);
        LinearLayout box = a.card(14, a.FIELD, a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        TextView t = a.text("📄 " + title, 15, a.TEXT, true);
        t.setSingleLine(false);
        box.addView(t);
        TextView m = a.text(meta(item), 12, item.chapterNo >= 0 ? a.MUTED : a.AMBER, false);
        m.setSingleLine(false);
        box.addView(m, a.marginLP(-1, -2, 0, 4, 0, 10));
        LinearLayout buttons = a.rowContainer();
        buttons.addView(a.tinyButton("Xem", a.BLUE, v -> view(item)), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        buttons.addView(a.space(6, 1));
        buttons.addView(a.tinyButton("Đổi tên", a.BLUE, v -> rename(item)), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        buttons.addView(a.space(6, 1));
        buttons.addView(a.tinyButton("Thay tệp", a.BLUE, v -> a.chooseRawReplacement(item.id)), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        buttons.addView(a.space(6, 1));
        buttons.addView(a.tinyButton("Xóa", a.RED, v -> a.bulkUi.deleteIds(LibraryBulkUi.TAB_RAW, java.util.Collections.singletonList(item.id))), new LinearLayout.LayoutParams(0, a.dp(42), 1));
        box.addView(buttons);
        a.bulkUi.attachLongPress(box, LibraryBulkUi.TAB_RAW, item.id);
        return box;
    }

    private void view(LibraryRawStore.Item item) {
        String text;
        try { text = LibraryRawStore.files(a).read(item.id); }
        catch (Exception e) { a.toast("Không đọc được RAW: " + AppValidator.readableError(e)); return; }
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(20, lines.length); i++) sb.append(lines[i]).append('\n');
        if (lines.length > 20) sb.append("…(").append(lines.length - 20).append(" dòng nữa)");
        TextView body = a.text(sb.toString(), 13, a.TEXT, false);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextIsSelectable(true);
        body.setPadding(a.dp(16), a.dp(8), a.dp(16), a.dp(8));
        ScrollView sv = new ScrollView(a);
        sv.addView(body);
        new AlertDialog.Builder(a).setTitle(item.originalName + " — " + item.chapterLabel()).setView(sv).setPositiveButton("Đóng", null).show();
    }

    private void rename(LibraryRawStore.Item item) {
        EditText input = a.input("Tên RAW", item.name);
        new AlertDialog.Builder(a).setTitle("Đổi tên RAW").setMessage("Chỉ đổi tên hiển thị; số chương và tên tệp gốc giữ nguyên.").setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    if (!LibraryRawStore.rename(a, item.id, input.getText().toString())) { a.toast("Tên không hợp lệ"); return; }
                    a.refreshPagePreservingScroll(LibraryBulkUi.TAB_RAW);
                }).setNegativeButton("Hủy", null).show();
    }
}
