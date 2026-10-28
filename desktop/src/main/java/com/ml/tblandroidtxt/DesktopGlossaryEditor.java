package com.ml.tblandroidtxt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * The open Glossary of the PC editor: a draft copy that the table edits, the filter, the unsaved-changes check, the health text and
 * the prompt preview. Saving goes through {@link GlossaryStoreCore#upsert} (same JSON as the phone); the draft never touches the store
 * before that.
 */
public final class DesktopGlossaryEditor {
    /** The three priority lines the phone adds under "Kiểm tra Glossary / Pronoun". */
    public static final String PRIORITY_RULE = "Priority rule:\n"
            + "- File Pronoun riêng ưu tiên hơn ghi chú xưng hô trong glossary.\n"
            + "- Ghi chú xưng hô trong glossary vẫn được giữ làm name/character lock nếu có file Pronoun riêng.\n"
            + "- Prompt chỉ inject rule match với chunk, không nhét toàn bộ glossary/pronoun.";

    public static final int SOURCE = 0, TARGET = 1, CATEGORY = 2, NOTE = 3;

    private final GlossaryStoreCore.Glossary draft;
    private String baseline;

    private DesktopGlossaryEditor(GlossaryStoreCore.Glossary draft) {
        this.draft = draft;
        this.baseline = signature();
    }

    public static DesktopGlossaryEditor open(GlossaryStoreCore.Glossary stored) {
        GlossaryStoreCore.Glossary copy = new GlossaryStoreCore.Glossary();
        copy.id = stored.id; copy.name = stored.name; copy.sourceLang = stored.sourceLang; copy.targetLang = stored.targetLang;
        copy.createdAt = stored.createdAt; copy.updatedAt = stored.updatedAt;
        for (GlossaryStoreCore.Term t : stored.terms) copy.terms.add(new GlossaryStoreCore.Term(t.source, t.target, t.category, t.note));
        return new DesktopGlossaryEditor(copy);
    }

    public String id() { return draft.id; }
    public String name() { return draft.name; }
    public String sourceLang() { return draft.sourceLang; }
    public String targetLang() { return draft.targetLang; }
    public void setName(String v) { draft.name = v == null ? "" : v.trim(); }
    public void setSourceLang(String v) { draft.sourceLang = v == null ? "" : v.trim(); }
    public void setTargetLang(String v) { draft.targetLang = v == null ? "" : v.trim(); }

    public int size() { return draft.terms.size(); }

    public String field(int row, int column) {
        GlossaryStoreCore.Term t = draft.terms.get(row);
        switch (column) {
            case SOURCE: return t.source;
            case TARGET: return t.target;
            case CATEGORY: return t.category;
            default: return t.note;
        }
    }

    public void setField(int row, int column, String value) {
        GlossaryStoreCore.Term t = draft.terms.get(row);
        String v = value == null ? "" : value;
        switch (column) {
            case SOURCE: t.source = v; break;
            case TARGET: t.target = v; break;
            case CATEGORY: t.category = v.trim().isEmpty() ? "term" : v; break;
            default: t.note = v; break;
        }
    }

    /** Adds a row at the top; returns its index (0). */
    public int addTerm(String source, String target, String category, String note) {
        draft.terms.add(0, new GlossaryStoreCore.Term(source, target, category, note));
        return 0;
    }

    public void removeRows(Collection<Integer> rows) {
        List<Integer> sorted = new ArrayList<>(new TreeSet<>(rows));
        for (int i = sorted.size() - 1; i >= 0; i--) {
            int r = sorted.get(i);
            if (r >= 0 && r < draft.terms.size()) draft.terms.remove(r);
        }
    }

    /** Distinct categories in use, sorted. */
    public List<String> categories() {
        TreeSet<String> out = new TreeSet<>();
        for (GlossaryStoreCore.Term t : draft.terms) out.add(t.category);
        return new ArrayList<>(out);
    }

    /** Row indices matching the search (source or target contains it) and the category ("" = every category). */
    public List<Integer> visible(String query, String category) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String c = category == null ? "" : category;
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < draft.terms.size(); i++) {
            GlossaryStoreCore.Term t = draft.terms.get(i);
            if (!c.isEmpty() && !c.equals(t.category)) continue;
            if (!q.isEmpty() && !t.source.toLowerCase(Locale.ROOT).contains(q) && !t.target.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(i);
        }
        return out;
    }

    public boolean dirty() { return !signature().equals(baseline); }

    /** Saves the draft; a row with all four fields empty is dropped. Returns false when the store refused the write. */
    public boolean save(DesktopRuntime runtime) {
        draft.terms.removeIf(t -> t.source.trim().isEmpty() && t.target.trim().isEmpty() && t.note.trim().isEmpty());
        if (draft.name.trim().isEmpty()) draft.name = "Glossary";
        if (!runtime.saveGlossary(draft)) return false;
        baseline = signature();
        return true;
    }

    /** Reads CSV/TXT files into the draft; returns {files read, terms added}. A file that cannot be read stops the merge with its error. */
    public int[] mergeFiles(List<Path> files) throws Exception {
        int read = 0, before = draft.terms.size();
        List<String> names = new ArrayList<>();
        for (Path f : files) {
            String text = TextDecoding.decode(Files.readAllBytes(f)).text;
            GlossaryStoreCore.mergeTerms(draft, GlossaryStoreCore.parseTerms(f.getFileName().toString(), text));
            names.add(DesktopRawStore.stripExtension(f.getFileName().toString()));
            read++;
        }
        draft.name = GlossaryStoreCore.suggestedImportName(draft.name, names);
        return new int[]{read, draft.terms.size() - before};
    }

    /** "Kiểm tra dữ liệu": the shared parser's report for this draft and the chosen Pronoun, then the priority rule, as on the phone. */
    public String healthText(String pronounText) {
        PromptContextBuilder.ParseReport r = PromptContextBuilder.validate(GlossaryStoreCore.toPromptText(draft), pronounText == null ? "" : pronounText);
        return r.summaryText() + "\n\n" + PRIORITY_RULE;
    }

    /** "Xem trước": exactly the Glossary text that goes into a prompt. */
    public String previewText() {
        String text = GlossaryStoreCore.toPromptText(draft);
        return text.isEmpty() ? "(Glossary chưa có thuật ngữ nào có cả source và target)" : text;
    }

    private String signature() {
        StringBuilder sb = new StringBuilder();
        sb.append(draft.name).append('\u0001').append(draft.sourceLang).append('\u0001').append(draft.targetLang);
        for (GlossaryStoreCore.Term t : draft.terms) {
            sb.append('\u0002').append(t.source).append('\u0001').append(t.target).append('\u0001').append(t.category).append('\u0001').append(t.note);
        }
        return sb.toString();
    }
}
