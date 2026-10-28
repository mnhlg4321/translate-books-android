package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A chapter set being created or edited on the PC. The rules are the phone's: {@link ComboDraft} keeps the three slots and the rows,
 * {@link ChapterSetMatcher} suggests and rates, and the set is stored with {@link ChapterSetStoreCore} in the same tables. This class
 * only feeds it the PC library (RAW from {@link DesktopRawStore}, Glossary and Pronoun from the prefs stores).
 */
public final class DesktopSetEditor {
    /** A library item to pick in a slot. */
    public static final class Choice {
        public final String id;
        public final String label;
        public final String meta;
        public final int chapterNo;
        Choice(String id, String label, String meta, int chapterNo) { this.id = id; this.label = label; this.meta = meta; this.chapterNo = chapterNo; }
    }

    private final DesktopRuntime rt;
    private final ComboDraft draft;
    private String baseline;

    private DesktopSetEditor(DesktopRuntime rt, ComboDraft draft) {
        this.rt = rt;
        this.draft = draft;
        this.baseline = signature();
    }

    /** A new set; {@code rawIds} pre-fills the RAW slot (the "Tạo tập" shortcut of the RAW page), may be empty. */
    public static DesktopSetEditor create(DesktopRuntime rt, Collection<String> rawIds) {
        ComboDraft d = new ComboDraft();
        d.rawIds.addAll(rawIds);
        d.dirty = true;
        d.rebuild(rt.rawRefs(), rt.glossaryNames(), rt.pronounNames());
        DesktopSetEditor e = new DesktopSetEditor(rt, d);
        e.baseline = "";
        return e;
    }

    /** A saved set opens with exactly its saved choices; items deleted since then show as missing. Null when the set is gone. */
    public static DesktopSetEditor open(DesktopRuntime rt, String setId) {
        ChapterSetStoreCore.SetData data = ChapterSetStoreCore.get(rt.db(), setId);
        if (data == null) return null;
        ComboDraft d = new ComboDraft();
        d.id = data.id; d.name = data.name; d.nameAuto = false; d.createdAt = data.createdAt; d.existing = true;
        Set<String> glossaries = new HashSet<>(), pronouns = new HashSet<>();
        for (ChapterSetMatcher.Named n : rt.glossaryNames()) glossaries.add(n.id);
        for (ChapterSetMatcher.Named n : rt.pronounNames()) pronouns.add(n.id);
        List<ComboDraft.RawRef> raws = rt.rawRefs();
        Set<String> knownRaw = new HashSet<>();
        for (ComboDraft.RawRef r : raws) knownRaw.add(r.id);
        for (ChapterSetStoreCore.Row r : data.rows) {
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
        d.rebuild(raws, rt.glossaryNames(), rt.pronounNames());
        for (ChapterSetMatcher.Row row : d.rows) {
            row.rawMissing = !knownRaw.contains(row.rawId);
            row.glossaryMissing = !row.glossaryId.isEmpty() && !glossaries.contains(row.glossaryId);
            row.pronounMissing = !row.pronounId.isEmpty() && !pronouns.contains(row.pronounId);
            row.glossaryChosen = true; row.pronounChosen = true;
        }
        ChapterSetMatcher.rate(d.rows);
        return new DesktopSetEditor(rt, d);
    }

    /** The rows a run would translate: every row that is not left out, as saved. */
    List<ChapterSetMatcher.Row> rowsForRun() {
        List<ChapterSetMatcher.Row> out = new ArrayList<>();
        for (ChapterSetMatcher.Row r : draft.rows) if (!r.excluded) out.add(r);
        return out;
    }

    public String id() { return draft.id; }
    public boolean existing() { return draft.existing; }
    public String name() { return draft.name; }

    public void setName(String v) {
        String text = v == null ? "" : v;
        if (text.equals(draft.name)) return;
        draft.name = text;
        draft.nameAuto = text.trim().isEmpty();
        draft.dirty = true;
    }

    public boolean dirty() { return draft.dirty && !signature().equals(baseline); }

    // ---- slots ----

    public List<Choice> rawChoices() {
        List<Choice> out = new ArrayList<>();
        for (DesktopRawStore.Item i : rt.raws().list()) out.add(new Choice(i.id, i.originalName, i.chapterLabel() + " · " + i.chars + " ký tự", i.chapterNo));
        return out;
    }

    public List<Choice> glossaryChoices() {
        List<Choice> out = new ArrayList<>();
        for (GlossaryStoreCore.Glossary g : rt.glossaries()) { int ch = ChapterNumber.parse(g.name); out.add(new Choice(g.id, g.name, (ch >= 0 ? "Chương " + ch + " · " : "") + g.count() + " mục", ch)); }
        return out;
    }

    public List<Choice> pronounChoices() {
        List<Choice> out = new ArrayList<>();
        for (PronounStoreCore.Profile p : rt.pronouns()) { int ch = ChapterNumber.parse(p.name); out.add(new Choice(p.id, p.name, (ch >= 0 ? "Chương " + ch + " · " : "") + p.count() + " quy tắc", ch)); }
        return out;
    }

    public Set<String> chosenRaw() { return new LinkedHashSet<>(draft.rawIds); }
    public Set<String> chosenGlossaries() { return new LinkedHashSet<>(draft.glossaryIds); }
    public Set<String> chosenPronouns() { return new LinkedHashSet<>(draft.pronounIds); }

    public void setRawSlot(Collection<String> ids) { replace(draft.rawIds, ids); }
    public void setGlossarySlot(Collection<String> ids) { replace(draft.glossaryIds, ids); }
    public void setPronounSlot(Collection<String> ids) { replace(draft.pronounIds, ids); }

    private void replace(Set<String> target, Collection<String> ids) {
        target.clear();
        target.addAll(ids);
        changed();
    }

    /** Takes the Glossary / Pronoun files that match no chapter out of their slot ("Bỏ khỏi ô"). */
    public void dropUnused(boolean glossary) {
        Set<String> unused = new HashSet<>(glossary ? draft.unusedGlossaries : draft.unusedPronouns);
        for (ChapterSetMatcher.Named n : glossary ? rt.glossaryNames() : rt.pronounNames()) if (unused.contains(n.name)) (glossary ? draft.glossaryIds : draft.pronounIds).remove(n.id);
        changed();
    }

    public List<String> unusedGlossaries() { return new ArrayList<>(draft.unusedGlossaries); }
    public List<String> unusedPronouns() { return new ArrayList<>(draft.unusedPronouns); }

    // ---- rows ----

    public List<VolumePlan.RowView> rows() {
        List<VolumePlan.RowView> out = new ArrayList<>();
        for (int i = 0; i < draft.rows.size(); i++) {
            ChapterSetMatcher.Row r = draft.rows.get(i);
            out.add(new VolumePlan.RowView(i, r, label(r, true), label(r, false)));
        }
        return out;
    }

    private String label(ChapterSetMatcher.Row r, boolean glossary) {
        String id = glossary ? r.glossaryId : r.pronounId;
        boolean missing = glossary ? r.glossaryMissing : r.pronounMissing;
        if (missing) return "(đã bị xóa)";
        if (!id.isEmpty()) {
            for (ChapterSetMatcher.Named n : glossary ? rt.glossaryNames() : rt.pronounNames()) if (n.id.equals(id)) return n.name;
            return id;
        }
        boolean chosen = glossary ? r.glossaryChosen : r.pronounChosen;
        boolean off = glossary ? r.glossaryOff : r.pronounOff;
        return chosen || off ? "Không dùng" : "(chưa có)";
    }

    /** The files a row may take: the ones chosen in the slot first (suggestions), then the rest of the library. */
    public List<Choice> rowChoices(int index, boolean glossary) {
        ChapterSetMatcher.Row row = draft.rows.get(index);
        Set<String> inSlot = glossary ? draft.glossaryIds : draft.pronounIds;
        List<Choice> first = new ArrayList<>(), rest = new ArrayList<>();
        for (Choice c : glossary ? glossaryChoices() : pronounChoices()) {
            if (inSlot.contains(c.id)) first.add(new Choice(c.id, c.label, c.chapterNo == row.chapterNo && c.chapterNo >= 0 ? "Gợi ý · cùng chương " + c.chapterNo : "Trong ô đã chọn", c.chapterNo));
            else rest.add(c);
        }
        first.addAll(rest);
        return first;
    }

    public void setChapterText(int index, String text) {
        String t = text == null ? "" : text.trim();
        int value = ChapterNumber.NONE;
        if (t.matches("[0-9]{1,6}")) value = Integer.parseInt(t);
        ChapterSetMatcher.Row row = draft.rows.get(index);
        if (value == row.chapterNo) return;
        row.chapterNo = value;
        changed();
    }

    /** An empty id means "Không dùng". */
    public void chooseGlossary(int index, String id) {
        ChapterSetMatcher.Row row = draft.rows.get(index);
        row.glossaryId = id == null ? "" : id; row.glossaryChosen = true; row.glossaryMissing = false;
        if (!row.glossaryId.isEmpty()) draft.glossaryIds.add(row.glossaryId);
        changed();
    }

    public void choosePronoun(int index, String id) {
        ChapterSetMatcher.Row row = draft.rows.get(index);
        row.pronounId = id == null ? "" : id; row.pronounChosen = true; row.pronounMissing = false;
        if (!row.pronounId.isEmpty()) draft.pronounIds.add(row.pronounId);
        changed();
    }

    public void setExcluded(int index, boolean excluded) {
        draft.rows.get(index).excluded = excluded;
        ChapterSetMatcher.rate(draft.rows);
        draft.dirty = true;
    }

    private void changed() {
        draft.dirty = true;
        List<ComboDraft.RawRef> raws = rt.rawRefs();
        Set<String> known = new HashSet<>();
        for (ComboDraft.RawRef r : raws) known.add(r.id);
        for (ChapterSetMatcher.Row row : draft.rows) if (!known.contains(row.rawId) && draft.rawIds.contains(row.rawId)) raws.add(new ComboDraft.RawRef(row.rawId, row.rawName, row.chapterNo));
        draft.rebuild(raws, rt.glossaryNames(), rt.pronounNames());
    }

    // ---- state and save ----

    public int included() { return draft.included(); }
    public boolean canSave() { return draft.canSave(); }
    public int firstRedIndex() { return draft.firstRedIndex(); }

    public String summary() {
        if (draft.rows.isEmpty()) return "Chưa có RAW nào: chọn RAW ở ô trên.";
        return ComboDraft.summaryOf(draft.included(), ChapterSetMatcher.count(draft.rows, ChapterSetMatcher.Status.GREEN),
                ChapterSetMatcher.count(draft.rows, ChapterSetMatcher.Status.YELLOW), ChapterSetMatcher.count(draft.rows, ChapterSetMatcher.Status.RED));
    }

    /** The reason the set cannot be saved yet, or null when it can. */
    public String blockReason() {
        ChapterSetMatcher.rate(draft.rows);
        if (draft.included() == 0) return "Chưa có chương nào: chọn RAW ở ô RAW";
        if (!draft.canSave()) return "Còn dòng ✕: " + draft.rows.get(draft.firstRedIndex()).rawName;
        return null;
    }

    /** Saves the set in the shared tables. Returns the saved set's id, or null when {@link #blockReason()} says no. */
    public String save() {
        if (blockReason() != null) return null;
        String name = draft.name == null || draft.name.trim().isEmpty() ? "Tổ hợp" : draft.name.trim();
        ChapterSetStoreCore.SetData data = new ChapterSetStoreCore.SetData();
        data.id = draft.id; data.name = name; data.createdAt = draft.createdAt;
        List<ChapterSetMatcher.Row> kept = new ArrayList<>();
        for (ChapterSetMatcher.Row r : draft.rows) if (!r.excluded) kept.add(r);
        Collections.sort(kept, (x, y) -> ChapterNumber.compare(x.chapterNo, x.rawName, y.chapterNo, y.rawName));
        for (ChapterSetMatcher.Row r : kept) data.rows.add(new ChapterSetStoreCore.Row(r.chapterNo, r.rawId, r.glossaryMissing ? "" : r.glossaryId, r.pronounMissing ? "" : r.pronounId));
        ChapterSetStoreCore.save(rt.db(), data);
        draft.existing = true;
        draft.name = name;
        draft.nameAuto = false;
        draft.dirty = false;
        baseline = signature();
        return data.id;
    }

    private String signature() {
        StringBuilder sb = new StringBuilder(draft.name == null ? "" : draft.name);
        for (ChapterSetMatcher.Row r : draft.rows) {
            sb.append('|').append(r.rawId).append(',').append(r.chapterNo).append(',').append(r.glossaryId).append(',').append(r.pronounId).append(',').append(r.excluded);
        }
        return sb.toString();
    }
}
