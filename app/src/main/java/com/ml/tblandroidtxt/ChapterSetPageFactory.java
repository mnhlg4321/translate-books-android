package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Library > Bộ chương: the list of saved sets and the confirmation table. The app only suggests the Glossary / Pronoun that start
 * with the same chapter number; the owner sees every row, changes what is wrong and saves. A saved set is never paired again.
 */
class ChapterSetPageFactory {
    /** The table being edited: a new set built from ticked RAW items, or a saved set that was opened. */
    static final class Draft {
        String id = java.util.UUID.randomUUID().toString();
        String name = "";
        long createdAt = System.currentTimeMillis();
        boolean existing;
        final List<ChapterSetMatcher.Row> rows = new ArrayList<>();
    }

    private final MainActivity a;

    ChapterSetPageFactory(MainActivity activity) { a = activity; }

    View buildPage() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        root.addView(a.chapterSetDraft == null ? listPanel() : editorPanel());
        return scroll;
    }

    // ---- building a draft ----

    /** Draft from the ticked RAW ids: suggestions only, nothing is saved yet. */
    static Draft newDraft(MainActivity a, List<String> rawIds) {
        List<LibraryRawStore.Item> picked = new ArrayList<>();
        java.util.Set<String> wanted = new java.util.HashSet<>(rawIds);
        for (LibraryRawStore.Item i : LibraryRawStore.list(a)) if (wanted.contains(i.id)) picked.add(i);
        LibraryRawStore.sort(picked);
        List<ChapterSetMatcher.Named> raws = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (LibraryRawStore.Item i : picked) { raws.add(new ChapterSetMatcher.Named(i.id, i.originalName)); numbers.add(i.chapterNo); names.add(i.originalName); }
        Draft d = new Draft();
        d.name = ChapterSetMatcher.defaultName(names);
        d.rows.addAll(ChapterSetMatcher.suggest(raws, numbers, glossaryNames(a), pronounNames(a)));
        return d;
    }

    /** Draft from a saved set: choices stay exactly as the owner confirmed them; deleted items show as missing. */
    static Draft openDraft(MainActivity a, String setId) {
        ChapterSetStore.SetData data = ChapterSetStore.get(a, setId);
        if (data == null) return null;
        Draft d = new Draft();
        d.id = data.id; d.name = data.name; d.createdAt = data.createdAt; d.existing = true;
        Map<String, String> glossaries = new HashMap<>(), pronouns = new HashMap<>();
        for (ChapterSetMatcher.Named n : glossaryNames(a)) glossaries.put(n.id, n.name);
        for (ChapterSetMatcher.Named n : pronounNames(a)) pronouns.put(n.id, n.name);
        for (ChapterSetStore.Row r : data.rows) {
            ChapterSetMatcher.Row row = new ChapterSetMatcher.Row();
            LibraryRawStore.Item raw = LibraryRawStore.find(a, r.rawId);
            row.rawId = r.rawId;
            row.rawName = raw == null ? "(RAW đã bị xóa)" : raw.originalName;
            row.rawMissing = raw == null;
            row.chapterNo = r.chapterNo;
            row.glossaryId = r.glossaryId; row.glossaryChosen = true; row.glossaryMissing = !r.glossaryId.isEmpty() && !glossaries.containsKey(r.glossaryId);
            row.pronounId = r.pronounId; row.pronounChosen = true; row.pronounMissing = !r.pronounId.isEmpty() && !pronouns.containsKey(r.pronounId);
            d.rows.add(row);
        }
        ChapterSetMatcher.resuggest(d.rows, glossaryNames(a), pronounNames(a));
        for (ChapterSetMatcher.Row row : d.rows) { row.glossaryChosen = true; row.pronounChosen = true; }
        ChapterSetMatcher.rate(d.rows);
        return d;
    }

    static List<ChapterSetMatcher.Named> glossaryNames(MainActivity a) {
        ArrayList<ChapterSetMatcher.Named> out = new ArrayList<>();
        for (GlossaryStore.Glossary g : GlossaryStore.loadAll(a)) out.add(new ChapterSetMatcher.Named(g.id, g.name));
        return out;
    }

    static List<ChapterSetMatcher.Named> pronounNames(MainActivity a) {
        ArrayList<ChapterSetMatcher.Named> out = new ArrayList<>();
        for (PronounStore.Profile p : PronounStore.loadAll(a)) out.add(new ChapterSetMatcher.Named(p.id, p.name));
        return out;
    }

    // ---- list of saved sets ----

    private View listPanel() {
        LinearLayout panel = a.sectionCard("📚", "Bộ chương");
        TextView desc = a.text("Mỗi bộ ghép RAW – Glossary – Pronoun theo chương. Tạo ở tab RAW: bấm Chọn, tick các chương, rồi Tạo bộ chương. Ở màn Dịch chọn nguồn \"Bộ chương\" là dịch được nhiều chương, mỗi chương một Glossary/Pronoun riêng.", 13, a.MUTED, false);
        desc.setSingleLine(false);
        panel.addView(desc, a.marginLP(-1, -2, 0, 0, 0, 10));
        List<ChapterSetStore.SetInfo> sets = ChapterSetStore.listSets(a);
        LibrarySelectionController selection = a.bulkUi.controller(LibraryBulkUi.TAB_SETS);
        if (selection.isActive()) {
            List<String> ids = new ArrayList<>();
            for (ChapterSetStore.SetInfo s : sets) ids.add(s.id);
            panel.addView(a.bulkUi.selectionHeader(LibraryBulkUi.TAB_SETS, ids), a.marginLP(-1, -2, 0, 0, 0, 10));
        } else if (!sets.isEmpty()) {
            panel.addView(a.bulkUi.selectButton(LibraryBulkUi.TAB_SETS), a.marginLP(-2, a.dp(40), 0, 0, 0, 10));
        }
        if (sets.isEmpty()) {
            TextView empty = a.text("Chưa có bộ chương.", 14, a.MUTED, false);
            empty.setGravity(Gravity.CENTER);
            panel.addView(empty, new LinearLayout.LayoutParams(-1, a.dp(90)));
        }
        for (ChapterSetStore.SetInfo info : sets) {
            Summary sum = summarize(openDraft(a, info.id));
            String meta = info.chapters + " chương · xanh " + sum.green + " · vàng " + sum.yellow + " · đỏ " + sum.red;
            if (selection.isActive()) { panel.addView(a.bulkUi.selectableCard(LibraryBulkUi.TAB_SETS, info.id, "📚 " + info.name, meta, a.BORDER), a.marginLP(-1, -2, 0, 0, 0, 10)); continue; }
            LinearLayout box = a.card(14, a.FIELD, a.BORDER);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
            TextView t = a.text("📚 " + info.name, 15, a.TEXT, true);
            t.setSingleLine(false);
            box.addView(t);
            TextView m = a.text(meta, 12, sum.red > 0 ? a.RED : sum.yellow > 0 ? a.AMBER : a.MUTED, false);
            m.setSingleLine(false);
            box.addView(m, a.marginLP(-1, -2, 0, 4, 0, 10));
            LinearLayout buttons = a.rowContainer();
            buttons.addView(a.tinyButton("Mở", a.BLUE, v -> { a.chapterSetDraft = openDraft(a, info.id); a.refreshPagePreservingScroll(LibraryBulkUi.TAB_SETS); }), new LinearLayout.LayoutParams(0, a.dp(42), 1));
            buttons.addView(a.space(6, 1));
            buttons.addView(a.tinyButton("Xóa", a.RED, v -> a.bulkUi.deleteIds(LibraryBulkUi.TAB_SETS, Collections.singletonList(info.id))), new LinearLayout.LayoutParams(0, a.dp(42), 1));
            box.addView(buttons);
            a.bulkUi.attachLongPress(box, LibraryBulkUi.TAB_SETS, info.id);
            panel.addView(box, a.marginLP(-1, -2, 0, 0, 0, 10));
        }
        return panel;
    }

    static final class Summary { int green, yellow, red; }

    static Summary summarize(Draft d) {
        Summary s = new Summary();
        if (d == null) return s;
        s.green = ChapterSetMatcher.count(d.rows, ChapterSetMatcher.Status.GREEN);
        s.yellow = ChapterSetMatcher.count(d.rows, ChapterSetMatcher.Status.YELLOW);
        s.red = ChapterSetMatcher.count(d.rows, ChapterSetMatcher.Status.RED);
        return s;
    }

    // ---- confirmation table ----

    private View editorPanel() {
        Draft d = a.chapterSetDraft;
        LinearLayout panel = a.sectionCard("📚", d.existing ? "Sửa bộ chương" : "Bộ chương mới");
        TextView desc = a.text("App chỉ gợi ý Glossary/Pronoun có cùng số chương ở đầu tên. Bạn xem từng dòng, sửa chỗ sai rồi Lưu; bộ đã lưu sẽ không bị ghép lại.", 13, a.MUTED, false);
        desc.setSingleLine(false);
        panel.addView(desc, a.marginLP(-1, -2, 0, 0, 0, 10));

        LinearLayout top = a.rowContainer();
        top.addView(a.secondaryButton("← Quay lại", v -> a.closeChapterSetDraft(false)), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        top.addView(a.space(8, 1));
        top.addView(a.primaryButton("Lưu bộ chương", v -> save()), new LinearLayout.LayoutParams(0, a.dp(44), 1));
        panel.addView(top, a.marginLP(-1, -2, 0, 0, 0, 10));

        EditText name = a.input("Tên bộ", d.name);
        name.setSingleLine(true);
        name.setContentDescription("chapter-set-name");
        name.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) { d.name = s.toString(); }
        });
        panel.addView(a.fieldBlock("TÊN BỘ", name));

        Summary sum = summarize(d);
        TextView summary = a.text("● xanh " + sum.green + "   ● vàng " + sum.yellow + "   ● đỏ " + sum.red, 14, sum.red > 0 ? a.RED : sum.yellow > 0 ? a.AMBER : a.GREEN, true);
        summary.setContentDescription("chapter-set-summary");
        panel.addView(summary, a.marginLP(-1, -2, 0, 4, 0, 4));
        TextView legend = a.text("Xanh: đủ.  Vàng: thiếu Glossary hoặc Pronoun (vẫn lưu được).  Đỏ: không có số chương, trùng số chương hoặc nhiều ứng viên — phải sửa hoặc bỏ dòng trước khi lưu.", 12, a.MUTED, false);
        legend.setSingleLine(false);
        panel.addView(legend, a.marginLP(-1, -2, 0, 0, 0, 10));

        for (ChapterSetMatcher.Row row : d.rows) panel.addView(rowCard(d, row), a.marginLP(-1, -2, 0, 0, 0, 10));
        return panel;
    }

    private View rowCard(Draft d, ChapterSetMatcher.Row row) {
        int color = row.excluded ? a.MUTED : row.status == ChapterSetMatcher.Status.RED ? a.RED : row.status == ChapterSetMatcher.Status.YELLOW ? a.AMBER : a.GREEN;
        LinearLayout box = a.card(14, a.FIELD, color);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        String number = row.chapterNo >= 0 ? "Chương " + row.chapterNo : "Chương ?";
        TextView title = a.text(number + " · " + row.rawName, 15, a.TEXT, true);
        title.setSingleLine(false);
        title.setContentDescription("row:" + row.rawName);
        box.addView(title);
        String statusWord = row.excluded ? "Bỏ khỏi bộ" : row.status == ChapterSetMatcher.Status.RED ? "ĐỎ" : row.status == ChapterSetMatcher.Status.YELLOW ? "VÀNG" : "XANH";
        TextView status = a.text("● " + statusWord + (row.excluded ? "" : " — " + row.reason), 13, color, true);
        status.setSingleLine(false);
        box.addView(status, a.marginLP(-1, -2, 0, 2, 0, 6));

        LinearLayout numberRow = a.rowContainer();
        numberRow.addView(a.text("Số chương", 12, a.MUTED, false), new LinearLayout.LayoutParams(a.dp(86), -2));
        EditText no = a.input("số", row.chapterNo >= 0 ? String.valueOf(row.chapterNo) : "");
        no.setInputType(InputType.TYPE_CLASS_NUMBER);
        no.setSingleLine(true);
        no.setImeOptions(EditorInfo.IME_ACTION_DONE);
        no.setContentDescription("chapter-no:" + row.rawName);
        Runnable apply = () -> {
            String text = no.getText().toString().trim();
            int value = ChapterNumber.NONE;
            try { if (!text.isEmpty()) value = Integer.parseInt(text); } catch (NumberFormatException ignored) { }
            if (value == row.chapterNo) return;
            row.chapterNo = value;
            ChapterSetMatcher.resuggest(d.rows, glossaryNames(a), pronounNames(a));
            ChapterSetMatcher.rate(d.rows);
            a.refreshPagePreservingScroll(LibraryBulkUi.TAB_SETS);
        };
        no.setOnEditorActionListener((v, actionId, event) -> { apply.run(); return false; });
        no.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) apply.run(); });
        numberRow.addView(no, new LinearLayout.LayoutParams(0, a.dp(40), 1));
        box.addView(numberRow, a.marginLP(-1, -2, 0, 0, 0, 6));

        box.addView(pickerButton("Glossary", row, true, d), a.marginLP(-1, a.dp(42), 0, 0, 0, 6));
        box.addView(pickerButton("Pronoun", row, false, d), a.marginLP(-1, a.dp(42), 0, 0, 0, 6));

        CheckBox skip = new CheckBox(a);
        skip.setText("Bỏ dòng này khỏi bộ");
        skip.setTextColor(a.TEXT);
        skip.setTextSize(13);
        skip.setChecked(row.excluded);
        skip.setOnClickListener(v -> {
            row.excluded = skip.isChecked();
            ChapterSetMatcher.rate(d.rows);
            a.refreshPagePreservingScroll(LibraryBulkUi.TAB_SETS);
        });
        box.addView(skip);
        return box;
    }

    private View pickerButton(String label, ChapterSetMatcher.Row row, boolean glossary, Draft d) {
        String id = glossary ? row.glossaryId : row.pronounId;
        boolean missing = glossary ? row.glossaryMissing : row.pronounMissing;
        boolean chosen = glossary ? row.glossaryChosen : row.pronounChosen;
        String shown;
        if (missing) shown = "(đã bị xóa)";
        else if (!id.isEmpty()) shown = nameOf(glossary ? glossaryNames(a) : pronounNames(a), id);
        else shown = chosen ? "Không dùng" : "(chưa chọn)";
        android.widget.Button b = a.secondaryButton(label + ": " + shown + "  ▾", v -> chooseProfile(label, row, glossary, d));
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setContentDescription(label.toLowerCase(Locale.ROOT) + ":" + row.rawName);
        b.setEnabled(!row.excluded);
        return b;
    }

    private static String nameOf(List<ChapterSetMatcher.Named> list, String id) {
        for (ChapterSetMatcher.Named n : list) if (n.id.equals(id)) return n.name;
        return id;
    }

    private void chooseProfile(String label, ChapterSetMatcher.Row row, boolean glossary, Draft d) {
        List<ChapterSetMatcher.Named> all = glossary ? glossaryNames(a) : pronounNames(a);
        List<String> candidates = glossary ? row.glossaryCandidates : row.pronounCandidates;
        List<ChapterSetMatcher.Named> ordered = new ArrayList<>();
        for (ChapterSetMatcher.Named n : all) if (candidates.contains(n.id)) ordered.add(n);
        for (ChapterSetMatcher.Named n : all) if (!candidates.contains(n.id)) ordered.add(n);
        String[] labels = new String[ordered.size() + 1];
        labels[0] = "Không dùng";
        for (int i = 0; i < ordered.size(); i++) labels[i + 1] = (candidates.contains(ordered.get(i).id) ? "★ " : "") + ordered.get(i).name;
        new AlertDialog.Builder(a).setTitle(label + " cho " + row.rawName).setItems(labels, (dlg, which) -> {
            String picked = which == 0 ? "" : ordered.get(which - 1).id;
            if (glossary) { row.glossaryId = picked; row.glossaryChosen = true; row.glossaryMissing = false; }
            else { row.pronounId = picked; row.pronounChosen = true; row.pronounMissing = false; }
            ChapterSetMatcher.rate(d.rows);
            a.refreshPagePreservingScroll(LibraryBulkUi.TAB_SETS);
        }).setNegativeButton("Hủy", null).show();
    }

    void save() {
        Draft d = a.chapterSetDraft;
        if (d == null) return;
        ChapterSetMatcher.rate(d.rows);
        if (ChapterSetMatcher.hasRed(d.rows)) {
            StringBuilder sb = new StringBuilder("Còn dòng đỏ, sửa hoặc bỏ dòng trước khi lưu:");
            for (ChapterSetMatcher.Row r : d.rows) if (!r.excluded && r.status == ChapterSetMatcher.Status.RED) sb.append("\n• ").append(r.rawName).append(" — ").append(r.reason);
            new AlertDialog.Builder(a).setTitle("Chưa lưu được").setMessage(sb.toString()).setPositiveButton("Đóng", null).show();
            return;
        }
        String name = d.name == null || d.name.trim().isEmpty() ? "Bộ chương" : d.name.trim();
        ChapterSetStore.SetData data = new ChapterSetStore.SetData();
        data.id = d.id; data.name = name; data.createdAt = d.createdAt;
        List<ChapterSetMatcher.Row> kept = new ArrayList<>();
        for (ChapterSetMatcher.Row r : d.rows) if (!r.excluded) kept.add(r);
        Collections.sort(kept, (x, y) -> ChapterNumber.compare(x.chapterNo, x.rawName, y.chapterNo, y.rawName));
        for (ChapterSetMatcher.Row r : kept) data.rows.add(new ChapterSetStore.Row(r.chapterNo, r.rawId, r.glossaryMissing ? "" : r.glossaryId, r.pronounMissing ? "" : r.pronounId));
        if (data.rows.isEmpty()) { a.toast("Bộ chưa có chương nào"); return; }
        ChapterSetStore.save(a, data);
        a.toast("Đã lưu bộ chương \"" + name + "\" (" + data.rows.size() + " chương)");
        a.closeChapterSetDraft(true);
    }
}
