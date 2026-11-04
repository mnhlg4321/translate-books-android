package com.ml.tblandroidtxt;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * PC side of the Library multi-select (RAW, Glossary, Pronoun, Tập). Ticks, the delete plan and its usage text, the undo bookkeeping
 * and collision-safe export names. It sits on the same {@link LibrarySelectionController} and {@link LibraryDeletePlan} as the phone,
 * so the two platforms ask the same questions and show the same messages.
 */
public final class DesktopLibraryBulk {
    public static final long UNDO_MS = 10_000L;

    /** Tick boxes of one list. Always in selection mode on the PC; the ticks are the source of truth for bulk actions. */
    public static final class Selection {
        private final LibrarySelectionController inner = new LibrarySelectionController();

        public Selection() { inner.enter(null); }

        public boolean isSelected(String id) { return inner.isSelected(id); }

        public void toggle(String id) { inner.toggle(id); }

        public int count() { return inner.count(); }

        public List<String> ids() { return inner.ids(); }

        /** "Chọn tất cả" over the rows that are shown now; a second press on a full set unticks them. */
        public void selectAll(Collection<String> visibleIds) { inner.selectAll(visibleIds); }

        /** Drops ticks of rows that no longer exist. */
        public void retainOnly(Collection<String> existingIds) { inner.retainOnly(existingIds); }

        public void clear() {
            inner.exit();
            inner.enter(null);
        }

        public String headline() { return inner.headline(); }
    }

    /** What a bulk delete would do: how many go, which are left out, and which chapter sets or RAW are affected. */
    public static final class Plan {
        private final LibraryDeletePlan inner;

        private Plan(LibraryDeletePlan inner) { this.inner = inner; }

        public static Plan of(List<LibraryDeletePlan.Target> targets) { return new Plan(LibraryDeletePlan.plan(targets)); }

        public Plan withRawOnly(List<String> rawIds) {
            inner.setRawOnlyIds(rawIds);
            return this;
        }

        public int deletableCount() { return inner.deletable.size(); }

        public int blockedCount() { return inner.blocked.size(); }

        public List<String> deletableIds() { return inner.deletableIds(); }

        public boolean needsSecondConfirm() { return inner.needsSecondConfirm(); }

        public String firstMessage() { return inner.firstMessage(); }

        public String secondMessage() { return inner.secondMessage(); }

        public List<String> rawOnlyIds() { return new ArrayList<>(inner.rawOnlyIds); }
    }

    /** One item taken out of a list, with the place it held so that undo can put it back there. */
    public static final class Removed<T> {
        public final T item;
        public final int index;

        Removed(T item, int index) {
            this.item = item;
            this.index = index;
        }
    }

    /** Removes every item whose id is in {@code ids}; the result is in ascending index order. */
    public static <T> List<Removed<T>> take(List<T> list, Collection<String> ids, Function<T, String> idOf) {
        Set<String> wanted = new HashSet<>(ids);
        List<Removed<T>> out = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (wanted.contains(idOf.apply(list.get(i)))) out.add(0, new Removed<>(list.remove(i), i));
        }
        return out;
    }

    /** Puts removed items back at their old places, skipping any whose id is already present again. */
    public static <T> void putBack(List<T> list, List<Removed<T>> removed, Function<T, String> idOf) {
        Set<String> present = new HashSet<>();
        for (T t : list) present.add(idOf.apply(t));
        for (Removed<T> r : removed) {
            if (!present.add(idOf.apply(r.item))) continue;
            list.add(Math.min(r.index, list.size()), r.item);
        }
    }

    /** Everything one delete took out, kept until the undo window ends. Glossary/Pronoun/Set fields are restored as stored objects. */
    public static final class Undo {
        final List<Removed<GlossaryStoreCore.Glossary>> glossaries = new ArrayList<>();
        final List<Removed<PronounStoreCore.Profile>> pronouns = new ArrayList<>();
        final List<ChapterSetStoreCore.SetData> sets = new ArrayList<>();
        /** The Glossary / Pronoun that was in use before the delete ("" when none was affected). */
        String glossarySelected = "";
        String pronounSelected = "";
        DesktopRawBulk.Pending raws;

        public int count() {
            return glossaries.size() + pronouns.size() + sets.size() + (raws == null ? 0 : raws.count());
        }
    }

    /**
     * Writes {@code bytes} into {@code folder} under {@code fileName}, never over an existing file: a clash gets " (2)", " (3)"…
     * before the extension. Returns the path written.
     */
    public static Path writeNew(Path folder, String fileName, byte[] bytes) throws IOException {
        Files.createDirectories(folder);
        String clean = fileName.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (clean.isEmpty() || clean.equals(".") || clean.equals("..")) clean = "tep";
        int dot = clean.lastIndexOf('.');
        String stem = dot > 0 ? clean.substring(0, dot) : clean;
        String ext = dot > 0 ? clean.substring(dot) : "";
        Path target = folder.resolve(clean);
        for (int n = 2; ; n++) {
            try {
                Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return target;
            } catch (FileAlreadyExistsException taken) {
                target = folder.resolve(stem + " (" + n + ")" + ext);
            }
        }
    }

    /** A Glossary as the CSV the import reads: {@code source,target,category,note}. */
    public static String glossaryCsv(GlossaryStoreCore.Glossary g) {
        StringBuilder sb = new StringBuilder("source,target,category,note\n");
        for (GlossaryStoreCore.Term t : g.terms) {
            sb.append(csv(t.source)).append(',').append(csv(t.target)).append(',').append(csv(t.category)).append(',').append(csv(t.note)).append('\n');
        }
        return sb.toString();
    }

    static String csv(String value) {
        String v = value == null ? "" : value;
        if (v.indexOf(',') < 0 && v.indexOf('"') < 0 && v.indexOf('\n') < 0 && v.indexOf('\r') < 0) return v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private DesktopLibraryBulk() {}
}
