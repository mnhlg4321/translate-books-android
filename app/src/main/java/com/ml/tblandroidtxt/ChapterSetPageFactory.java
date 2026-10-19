package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Library > Tổ hợp: the list of saved combinations and the screen that creates or edits one from three slots (RAW, Glossary, Pronoun).
 * The app only suggests the pairing by chapter number inside the files chosen in the slots; every row names all three files, the owner
 * can change any row, and a saved combination is never paired again.
 */
class ChapterSetPageFactory {
    private final MainActivity a;

    ChapterSetPageFactory(MainActivity activity) { a = activity; }

    View buildPage() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        View panel = a.chapterSetDraft == null ? listPanel() : editorPanel();
        root.addView(panel);
        final String targetTag = a.comboScrollTarget;
        a.comboScrollTarget = "";
        if (!targetTag.isEmpty()) scroll.post(() -> {
            View v = findByTag(root, targetTag);
            if (v == null) return;
            int y = 0;
            View cur = v;
            while (cur != null && cur != root) { y += cur.getTop(); cur = cur.getParent() instanceof View ? (View) cur.getParent() : null; }
            scroll.smoothScrollTo(0, Math.max(0, y - a.dp(12)));
        });
        return scroll;
    }

    private static View findByTag(View v, String tag) {
        if (tag.equals(v.getTag())) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) { View f = findByTag(g.getChildAt(i), tag); if (f != null) return f; }
        }
        return null;
    }

    // ---- data helpers ----

    static List<ComboDraft.RawRef> rawRefs(MainActivity a) {
        ArrayList<ComboDraft.RawRef> out = new ArrayList<>();
        for (LibraryRawStore.Item i : LibraryRawStore.list(a)) out.add(new ComboDraft.RawRef(i.id, i.originalName, i.chapterNo));
        return out;
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

    static void rebuild(MainActivity a, ComboDraft d) { d.rebuild(rawRefs(a), glossaryNames(a), pronounNames(a)); }

    /** New combination; {@code rawIds} pre-fills the RAW slot (the shortcut from the RAW tab), may be empty. */
    static ComboDraft newDraft(MainActivity a, List<String> rawIds) {
        ComboDraft d = new ComboDraft();
        d.rawIds.addAll(rawIds);
        d.dirty = true;
        rebuild(a, d);
        return d;
    }

    /** A saved combination: the slots hold exactly its files, every row keeps what the owner saved, deleted items show as missing. */
    static ComboDraft openDraft(MainActivity a, String setId) {
        ChapterSetStore.SetData data = ChapterSetStore.get(a, setId);
        if (data == null) return null;
        ComboDraft d = new ComboDraft();
        d.id = data.id; d.name = data.name; d.nameAuto = false; d.createdAt = data.createdAt; d.existing = true;
        Set<String> glossaries = new HashSet<>(), pronouns = new HashSet<>();
        for (ChapterSetMatcher.Named n : glossaryNames(a)) glossaries.add(n.id);
        for (ChapterSetMatcher.Named n : pronounNames(a)) pronouns.add(n.id);
        List<ComboDraft.RawRef> raws = rawRefs(a);
        Set<String> knownRaw = new HashSet<>();
        for (ComboDraft.RawRef r : raws) knownRaw.add(r.id);
        for (ChapterSetStore.Row r : data.rows) {
            ChapterSetMatcher.Row row = new ChapterSetMatcher.Row();
            row.rawId = r.rawId; row.chapterNo = r.chapterNo;
            row.glossaryId = r.glossaryId; row.glossaryChosen = true;
            row.pronounId = r.pronounId; row.pronounChosen = true;
            d.rows.add(row);
            d.rawIds.add(r.rawId);
            if (!r.glossaryId.isEmpty()) d.glossaryIds.add(r.glossaryId);
            if (!r.pronounId.isEmpty()) d.pronounIds.add(r.pronounId);
            if (!knownRaw.contains(r.rawId)) raws.add(new ComboDraft.RawRef(r.rawId, "(RAW đã bị xóa)", r.chapterNo));
        }
        d.rebuild(raws, glossaryNames(a), pronounNames(a));
        for (ChapterSetMatcher.Row row : d.rows) {
            row.rawMissing = !knownRaw.contains(row.rawId);
            row.glossaryMissing = !row.glossaryId.isEmpty() && !glossaries.contains(row.glossaryId);
            row.pronounMissing = !row.pronounId.isEmpty() && !pronouns.contains(row.pronounId);
            row.glossaryChosen = true; row.pronounChosen = true;
            row.chapterNo = row.chapterNo; // kept as saved, not re-read from the file name
        }
        ChapterSetMatcher.rate(d.rows);
        return d;
    }

    private void change() {
        ComboDraft d = a.chapterSetDraft;
        if (d == null) return;
        d.dirty = true;
        List<ComboDraft.RawRef> raws = rawRefs(a);
        Set<String> known = new HashSet<>();
        for (ComboDraft.RawRef r : raws) known.add(r.id);
        for (ChapterSetMatcher.Row row : d.rows) if (!known.contains(row.rawId) && d.rawIds.contains(row.rawId)) raws.add(new ComboDraft.RawRef(row.rawId, row.rawName, row.chapterNo));
        d.rebuild(raws, glossaryNames(a), pronounNames(a));
        a.refreshPagePreservingScroll(LibraryBulkUi.TAB_SETS);
    }

    // ---- list of saved combinations ----

    private View listPanel() {
        LinearLayout panel = a.sectionCard("📚", "Tổ hợp");
        TextView desc = a.text("Một tổ hợp gồm một hoặc nhiều chương; mỗi chương là một bộ RAW – Glossary – Pronoun. Ở màn Dịch chọn nguồn \"Tổ hợp\" để dịch nhiều chương, mỗi chương dùng đúng Glossary/Pronoun của nó.", 13, a.MUTED, false);
        desc.setSingleLine(false);
        panel.addView(desc, a.marginLP(-1, -2, 0, 0, 0, 10));
        android.widget.Button create = a.primaryButton("+ Tạo tổ hợp mới", v -> { a.chapterSetDraft = newDraft(a, Collections.<String>emptyList()); a.refreshPagePreservingScroll(LibraryBulkUi.TAB_SETS); });
        create.setContentDescription("combo-create");
        panel.addView(create, a.marginLP(-1, a.dp(48), 0, 0, 0, 8));
        List<ChapterSetStore.SetInfo> sets = ChapterSetStore.listSets(a);
        LibrarySelectionController selection = a.bulkUi.controller(LibraryBulkUi.TAB_SETS);
        if (selection.isActive()) {
            List<String> ids = new ArrayList<>();
            for (ChapterSetStore.SetInfo s : sets) ids.add(s.id);
            panel.addView(a.bulkUi.selectionHeader(LibraryBulkUi.TAB_SETS, ids), a.marginLP(-1, -2, 0, 0, 0, 10));
        } else if (!sets.isEmpty()) {
            panel.addView(a.bulkUi.headerRow(LibraryBulkUi.TAB_SETS, sets.size()), a.marginLP(-1, -2, 0, 0, 0, 10));
        }
        if (sets.isEmpty()) {
            TextView empty = a.text("Chưa có tổ hợp. Bấm \"+ Tạo tổ hợp mới\".", 14, a.MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setSingleLine(false);
            panel.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        }
        for (ChapterSetStore.SetInfo info : sets) {
            ComboDraft d = openDraft(a, info.id);
            int green = 0, yellow = 0, red = 0;
            List<Integer> chapters = new ArrayList<>();
            if (d != null) for (ChapterSetMatcher.Row r : d.rows) {
                chapters.add(r.chapterNo);
                if (r.status == ChapterSetMatcher.Status.GREEN) green++; else if (r.status == ChapterSetMatcher.Status.YELLOW) yellow++; else red++;
            }
            String meta = ComboDraft.summaryOf(info.chapters, green, yellow, red) + (chapters.isEmpty() ? "" : "\n" + ComboDraft.chapterRange(chapters));
            View card;
            if (selection.isActive()) {
                card = a.bulkUi.selectableCard(LibraryBulkUi.TAB_SETS, info.id, "📚 " + info.name, meta, a.BORDER);
            } else {
                LinearLayout box = a.card(14, a.FIELD, red > 0 ? a.RED : yellow > 0 ? a.AMBER : a.BORDER);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
                TextView t = a.text("📚 " + info.name, 15, a.TEXT, true);
                t.setSingleLine(false);
                box.addView(t);
                TextView m = a.text(meta, 12, red > 0 ? a.RED : yellow > 0 ? a.AMBER : a.MUTED, false);
                m.setSingleLine(false);
                box.addView(m, a.marginLP(-1, -2, 0, 4, 0, 6));
                LinearLayout buttons = a.rowContainer();
                buttons.addView(a.tinyButton("Mở", a.BLUE, v -> openCombo(info.id)), new LinearLayout.LayoutParams(0, -2, 1));
                buttons.addView(a.space(6, 1));
                buttons.addView(a.tinyButton("Xóa", a.RED, v -> a.bulkUi.deleteIds(LibraryBulkUi.TAB_SETS, Collections.singletonList(info.id))), new LinearLayout.LayoutParams(0, -2, 1));
                box.addView(buttons);
                box.setOnClickListener(v -> openCombo(info.id));
                a.bulkUi.attachLongPress(box, LibraryBulkUi.TAB_SETS, info.id);
                card = box;
            }
            card.setTag("combo:" + info.id);
            panel.addView(card, a.marginLP(-1, -2, 0, 0, 0, 10));
        }
        return panel;
    }

    private void openCombo(String id) {
        a.chapterSetDraft = openDraft(a, id);
        a.refreshPagePreservingScroll(LibraryBulkUi.TAB_SETS);
    }

    // ---- create / edit screen ----

    private View editorPanel() {
        ComboDraft d = a.chapterSetDraft;
        LinearLayout panel = a.sectionCard("📚", d.existing ? "Sửa tổ hợp" : "Tổ hợp mới");
        TextView desc = a.text("Chọn tệp ở ba ô; app gợi ý ghép theo số chương ở đầu tên, chỉ trong các tệp bạn đã chọn. Bạn xem từng dòng và đổi chỗ sai.", 13, a.MUTED, false);
        desc.setSingleLine(false);
        panel.addView(desc, a.marginLP(-1, -2, 0, 0, 0, 10));

        EditText name = a.input("Tên tổ hợp", d.name);
        name.setSingleLine(true);
        name.setContentDescription("combo-name");
        name.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                if (s.toString().equals(d.name)) return;
                d.name = s.toString();
                d.nameAuto = s.toString().trim().isEmpty();
                d.dirty = true;
            }
        });
        panel.addView(a.fieldBlock("TÊN TỔ HỢP", name));

        panel.addView(rawSlot(d), a.marginLP(-1, -2, 0, 4, 0, 8));
        panel.addView(profileSlot(d, true), a.marginLP(-1, -2, 0, 0, 0, 8));
        panel.addView(profileSlot(d, false), a.marginLP(-1, -2, 0, 0, 0, 12));

        panel.addView(a.text("Ghép theo chương", 16, a.TEXT, true));
        int green = ChapterSetMatcher.count(d.rows, ChapterSetMatcher.Status.GREEN), yellow = ChapterSetMatcher.count(d.rows, ChapterSetMatcher.Status.YELLOW), red = ChapterSetMatcher.count(d.rows, ChapterSetMatcher.Status.RED);
        TextView summary = a.text(d.rows.isEmpty() ? "Chưa có RAW nào: chọn RAW ở ô trên." : ComboDraft.summaryOf(d.included(), green, yellow, red), 14, red > 0 ? a.RED : yellow > 0 ? a.AMBER : a.GREEN, true);
        summary.setSingleLine(false);
        summary.setContentDescription("combo-summary");
        panel.addView(summary, a.marginLP(-1, -2, 0, 2, 0, 6));
        if (!d.unusedGlossaries.isEmpty()) panel.addView(warning(d.unusedGlossaries.size() + " Glossary không khớp chương nào: " + String.join(", ", d.unusedGlossaries), true), a.marginLP(-1, -2, 0, 0, 0, 6));
        if (!d.unusedPronouns.isEmpty()) panel.addView(warning(d.unusedPronouns.size() + " Pronoun không khớp chương nào: " + String.join(", ", d.unusedPronouns), false), a.marginLP(-1, -2, 0, 0, 0, 6));
        for (ChapterSetMatcher.Row row : d.rows) panel.addView(rowCard(d, row), a.marginLP(-1, -2, 0, 0, 0, 10));
        return panel;
    }

    private View warning(String text, boolean glossary) {
        LinearLayout box = a.rowContainer();
        TextView t = a.text("⚠ " + text, 12, a.AMBER, false);
        t.setSingleLine(false);
        box.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(a.secondaryButton("Bỏ khỏi ô", v -> {
            ComboDraft d = a.chapterSetDraft;
            java.util.Set<String> unusedNames = new HashSet<>(glossary ? d.unusedGlossaries : d.unusedPronouns);
            List<ChapterSetMatcher.Named> all = glossary ? glossaryNames(a) : pronounNames(a);
            for (ChapterSetMatcher.Named n : all) if (unusedNames.contains(n.name)) (glossary ? d.glossaryIds : d.pronounIds).remove(n.id);
            change();
        }), new LinearLayout.LayoutParams(-2, -2));
        return box;
    }

    private View rawSlot(ComboDraft d) {
        List<String> names = new ArrayList<>(), metas = new ArrayList<>();
        Map<String, LibraryRawStore.Item> byId = new HashMap<>();
        for (LibraryRawStore.Item i : LibraryRawStore.list(a)) byId.put(i.id, i);
        List<LibraryRawStore.Item> chosen = new ArrayList<>();
        for (String id : d.rawIds) if (byId.containsKey(id)) chosen.add(byId.get(id));
        LibraryRawStore.sort(chosen);
        for (LibraryRawStore.Item i : chosen) { names.add(i.originalName); metas.add(i.chapterLabel()); }
        boolean none = names.isEmpty();
        return SelectedFilesSlot.build(a, "RAW", "📄", names, metas, none ? SelectedFilesSlot.Status.NOT_CHOSEN : SelectedFilesSlot.Status.CHOSEN, "", none ? "Chọn" : "Đổi", this::pickRaw);
    }

    private View profileSlot(ComboDraft d, boolean glossary) {
        List<String> names = new ArrayList<>(), metas = new ArrayList<>();
        Map<String, String> nameById = new HashMap<>();
        for (ChapterSetMatcher.Named n : glossary ? glossaryNames(a) : pronounNames(a)) nameById.put(n.id, n.name);
        List<ChapterSetMatcher.Named> chosen = new ArrayList<>();
        for (String id : glossary ? d.glossaryIds : d.pronounIds) if (nameById.containsKey(id)) chosen.add(new ChapterSetMatcher.Named(id, nameById.get(id)));
        Collections.sort(chosen, (x, y) -> ChapterNumber.compare(ChapterNumber.parse(x.name), x.name, ChapterNumber.parse(y.name), y.name));
        for (ChapterSetMatcher.Named n : chosen) { names.add(n.name); int ch = ChapterNumber.parse(n.name); metas.add(ch >= 0 ? "Chương " + ch : ""); }
        boolean none = names.isEmpty();
        return SelectedFilesSlot.build(a, glossary ? "Glossary" : "Pronoun", glossary ? "📘" : "↔", names, metas,
                none ? SelectedFilesSlot.Status.OFF : SelectedFilesSlot.Status.CHOSEN, "Không dùng", none ? "Chọn" : "Đổi", () -> pickProfiles(glossary));
    }

    void pickRaw() {
        ComboDraft d = a.chapterSetDraft;
        List<FilePickerModel.Item> items = new ArrayList<>();
        for (LibraryRawStore.Item i : LibraryRawStore.list(a)) items.add(new FilePickerModel.Item(i.id, i.originalName, i.chapterLabel() + " · " + String.format(Locale.US, "%,d", i.chars) + " ký tự", i.chapterNo, false, ""));
        FilePickerModel model = new FilePickerModel(true, false, items, d.rawIds);
        FilePickerSheet.show(a, "Chọn RAW", items.isEmpty() ? "Chưa có RAW trong thư viện: nhập ở Library > RAW." : null, model, "Xong", ids -> {
            d.rawIds.clear();
            d.rawIds.addAll(ids);
            a.toast("Đã chọn " + ids.size() + " RAW");
            change();
        });
    }

    void pickProfiles(boolean glossary) {
        ComboDraft d = a.chapterSetDraft;
        Set<Integer> chapters = new HashSet<>();
        for (ChapterSetMatcher.Row r : d.rows) if (r.chapterNo >= 0) chapters.add(r.chapterNo);
        List<FilePickerModel.Item> items = new ArrayList<>();
        if (glossary) {
            for (GlossaryStore.Glossary g : GlossaryStore.loadAll(a)) { int ch = ChapterNumber.parse(g.name); items.add(profileItem(g.id, g.name, ch, g.count() + " mục", chapters)); }
        } else {
            for (PronounStore.Profile p : PronounStore.loadAll(a)) { int ch = ChapterNumber.parse(p.name); items.add(profileItem(p.id, p.name, ch, p.count() + " hàng", chapters)); }
        }
        String label = glossary ? "Glossary" : "Pronoun";
        FilePickerModel model = new FilePickerModel(true, false, items, glossary ? d.glossaryIds : d.pronounIds);
        FilePickerSheet.show(a, "Chọn " + label, chapters.isEmpty() ? null : "Gợi ý: tệp cùng số chương với các RAW đã chọn", model, "Xong", ids -> {
            LinkedHashSetHelper.replace(glossary ? d.glossaryIds : d.pronounIds, ids);
            a.toast(ids.isEmpty() ? label + ": Không dùng" : "Đã chọn " + ids.size() + " " + label);
            change();
        });
    }

    private static FilePickerModel.Item profileItem(String id, String name, int chapter, String count, Set<Integer> rawChapters) {
        boolean suggested = chapter >= 0 && rawChapters.contains(chapter);
        return new FilePickerModel.Item(id, name, (chapter >= 0 ? "Chương " + chapter + " · " : "") + count, chapter, suggested, suggested ? "Gợi ý · cùng chương " + chapter : "");
    }

    private View rowCard(ComboDraft d, ChapterSetMatcher.Row row) {
        int color = row.excluded ? a.MUTED : row.status == ChapterSetMatcher.Status.RED ? a.RED : row.status == ChapterSetMatcher.Status.YELLOW ? a.AMBER : a.GREEN;
        LinearLayout box = a.card(14, a.FIELD, color);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        box.setTag("row:" + row.rawId);
        String number = row.chapterNo >= 0 ? "Chương " + row.chapterNo : "Chương ?";
        String icon = row.excluded ? "—" : row.status == ChapterSetMatcher.Status.RED ? "✕" : row.status == ChapterSetMatcher.Status.YELLOW ? "⚠" : "✓";
        String word = row.excluded ? "Bỏ khỏi tổ hợp" : row.status == ChapterSetMatcher.Status.GREEN ? "Đủ" : row.reason;
        TextView title = a.text(number + "   " + icon + " " + word, 15, color, true);
        title.setSingleLine(false);
        title.setContentDescription("row-status:" + row.rawName);
        box.addView(title);
        box.addView(line("📄 RAW", row.rawName, a.TEXT), a.marginLP(-1, -2, 0, 4, 0, 0));
        box.addView(profileLine(d, row, true), a.marginLP(-1, -2, 0, 4, 0, 0));
        box.addView(profileLine(d, row, false), a.marginLP(-1, -2, 0, 4, 0, 0));

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
            change();
        };
        no.setOnEditorActionListener((v, actionId, event) -> { apply.run(); return false; });
        no.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) apply.run(); });
        numberRow.addView(no, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(numberRow, a.marginLP(-1, -2, 0, 6, 0, 4));

        CheckBox skip = new CheckBox(a);
        skip.setText("Bỏ dòng này khỏi tổ hợp");
        skip.setTextColor(a.TEXT);
        skip.setTextSize(13);
        skip.setChecked(row.excluded);
        skip.setOnClickListener(v -> { row.excluded = skip.isChecked(); ChapterSetMatcher.rate(d.rows); d.dirty = true; a.refreshPagePreservingScroll(LibraryBulkUi.TAB_SETS); });
        box.addView(skip);
        return box;
    }

    private View line(String label, String value, int color) {
        TextView t = a.text(label + "   " + value, 13, color, false);
        t.setSingleLine(false);
        return t;
    }

    private View profileLine(ComboDraft d, ChapterSetMatcher.Row row, boolean glossary) {
        String id = glossary ? row.glossaryId : row.pronounId;
        boolean missing = glossary ? row.glossaryMissing : row.pronounMissing;
        String shown;
        if (missing) shown = "(đã bị xóa)";
        else if (!id.isEmpty()) shown = nameOf(glossary ? glossaryNames(a) : pronounNames(a), id);
        else shown = (glossary ? row.glossaryChosen : row.pronounChosen) || (glossary ? row.glossaryOff : row.pronounOff) ? "Không dùng" : "(chưa có)";
        LinearLayout line = a.rowContainer();
        TextView t = a.text((glossary ? "📘 Glossary   " : "↔ Pronoun   ") + shown, 13, missing ? a.AMBER : a.TEXT, false);
        t.setSingleLine(false);
        t.setContentDescription((glossary ? "glossary:" : "pronoun:") + row.rawName);
        line.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        android.widget.Button change = a.secondaryButton("Đổi", v -> chooseForRow(d, row, glossary));
        change.setEnabled(!row.excluded);
        change.setContentDescription((glossary ? "change-glossary:" : "change-pronoun:") + row.rawName);
        line.addView(change, new LinearLayout.LayoutParams(-2, -2));
        return line;
    }

    private static String nameOf(List<ChapterSetMatcher.Named> list, String id) {
        for (ChapterSetMatcher.Named n : list) if (n.id.equals(id)) return n.name;
        return id;
    }

    /** Single-choice sheet for one row: the files chosen in the slot come first as suggestions, the rest of the library is below. */
    void chooseForRow(ComboDraft d, ChapterSetMatcher.Row row, boolean glossary) {
        List<ChapterSetMatcher.Named> all = glossary ? glossaryNames(a) : pronounNames(a);
        Set<String> inSlot = glossary ? d.glossaryIds : d.pronounIds;
        List<FilePickerModel.Item> items = new ArrayList<>();
        for (ChapterSetMatcher.Named n : all) {
            int ch = ChapterNumber.parse(n.name);
            boolean suggested = inSlot.contains(n.id);
            String label = suggested ? (ch == row.chapterNo && ch >= 0 ? "Gợi ý · cùng chương " + ch : "Trong ô đã chọn") : "";
            items.add(new FilePickerModel.Item(n.id, n.name, ch >= 0 ? "Chương " + ch : "", ch, suggested, label));
        }
        String current = glossary ? row.glossaryId : row.pronounId;
        FilePickerModel model = new FilePickerModel(false, true, items, Collections.singletonList(current));
        String label = glossary ? "Glossary" : "Pronoun";
        FilePickerSheet.show(a, label + " cho " + (row.chapterNo >= 0 ? "Chương " + row.chapterNo : row.rawName), row.rawName, model, "Chọn", ids -> {
            String picked = ids.isEmpty() ? "" : ids.get(0);
            if (glossary) { row.glossaryId = picked; row.glossaryChosen = true; row.glossaryMissing = false; }
            else { row.pronounId = picked; row.pronounChosen = true; row.pronounMissing = false; }
            a.toast(picked.isEmpty() ? label + ": Không dùng" : "Đã chọn: " + nameOf(all, picked));
            if (!picked.isEmpty()) (glossary ? d.glossaryIds : d.pronounIds).add(picked);
            change();
        });
    }

    // ---- fixed bottom bar and saving ----

    /** "Hủy" and "Lưu tổ hợp (n chương)", outside the scrolling page; null when no combination is open. */
    View bottomBar() {
        ComboDraft d = a.chapterSetDraft;
        if (d == null) return null;
        LinearLayout bar = a.card(14, a.PANEL, a.BORDER);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(a.dp(8), a.dp(8), a.dp(8), a.dp(8));
        bar.addView(a.secondaryButton("Hủy", v -> a.closeChapterSetDraft(false)), new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(a.space(8, 1));
        android.widget.Button save = a.primaryButton("Lưu tổ hợp (" + d.included() + " chương)", v -> save());
        save.setContentDescription("combo-save");
        bar.addView(save, new LinearLayout.LayoutParams(0, -2, 2));
        bar.setLayoutParams(a.marginLP(-1, -2, 0, 6, 0, 0));
        return bar;
    }

    void save() {
        ComboDraft d = a.chapterSetDraft;
        if (d == null) return;
        ChapterSetMatcher.rate(d.rows);
        if (d.included() == 0) { a.toast("Chưa có chương nào: chọn RAW ở ô RAW"); return; }
        if (!d.canSave()) {
            int idx = d.firstRedIndex();
            ChapterSetMatcher.Row bad = d.rows.get(idx);
            a.comboScrollTarget = "row:" + bad.rawId;
            a.toast("Còn dòng ✕: " + bad.rawName);
            a.refreshPagePreservingScroll(LibraryBulkUi.TAB_SETS);
            return;
        }
        String name = d.name == null || d.name.trim().isEmpty() ? "Tổ hợp" : d.name.trim();
        ChapterSetStore.SetData data = new ChapterSetStore.SetData();
        data.id = d.id; data.name = name; data.createdAt = d.createdAt;
        List<ChapterSetMatcher.Row> kept = new ArrayList<>();
        for (ChapterSetMatcher.Row r : d.rows) if (!r.excluded) kept.add(r);
        Collections.sort(kept, (x, y) -> ChapterNumber.compare(x.chapterNo, x.rawName, y.chapterNo, y.rawName));
        for (ChapterSetMatcher.Row r : kept) data.rows.add(new ChapterSetStore.Row(r.chapterNo, r.rawId, r.glossaryMissing ? "" : r.glossaryId, r.pronounMissing ? "" : r.pronounId));
        ChapterSetStore.save(a, data);
        a.toast("Đã lưu tổ hợp \"" + name + "\" (" + data.rows.size() + " chương)");
        a.comboScrollTarget = "combo:" + data.id;
        a.closeChapterSetDraft(true);
    }

    /** Small helper kept here so the slot code reads the same for both profile kinds. */
    private static final class LinkedHashSetHelper {
        static void replace(Set<String> target, List<String> ids) { target.clear(); target.addAll(ids); }
    }
}
