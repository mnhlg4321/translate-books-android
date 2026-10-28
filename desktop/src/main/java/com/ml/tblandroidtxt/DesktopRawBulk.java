package com.ml.tblandroidtxt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bulk delete, undo and export of ticked RAW chapters on the PC, built on the same list arithmetic ({@link LibraryBulk}) and the same
 * messages ({@link LibraryDeletePlan}) as the phone. A delete removes the index rows at once; the files are removed only when the ten
 * second undo window ends ({@link #expire}), so {@link #undo} can bring everything back.
 */
public final class DesktopRawBulk {
    public static final long UNDO_MS = 10_000L;

    /** What a delete would do with the ticked chapters. */
    public static final class Plan {
        private final LibraryDeletePlan inner;
        private Plan(LibraryDeletePlan inner) { this.inner = inner; }
        public int deletableCount() { return inner.deletable.size(); }
        public int blockedCount() { return inner.blocked.size(); }
        public List<String> deletableIds() { return inner.deletableIds(); }
        public boolean needsSecondConfirm() { return inner.needsSecondConfirm(); }
        public String firstMessage() { return inner.firstMessage(); }
        public String secondMessage() { return inner.secondMessage(); }
    }

    /** A finished delete that can still be undone. */
    public static final class Pending {
        private final List<LibraryBulk.Removed<DesktopRawStore.Item>> removed;
        private boolean settled;
        private Pending(List<LibraryBulk.Removed<DesktopRawStore.Item>> removed) { this.removed = removed; }
        public int count() { return removed.size(); }
    }

    private final DesktopRawStore raws;

    DesktopRawBulk(DesktopRawStore raws) { this.raws = raws; }

    public Plan plan(Collection<String> ids) {
        ArrayList<LibraryDeletePlan.Target> targets = new ArrayList<>();
        for (String id : new LinkedHashSet<>(ids)) {
            DesktopRawStore.Item item = raws.find(id);
            if (item == null) continue;
            LibraryDeletePlan.Target t = new LibraryDeletePlan.Target(item.id, item.originalName.isEmpty() ? item.name : item.name);
            t.chapterSets.addAll(raws.chapterSetNames(id));
            t.unfinishedJobs.addAll(raws.unfinishedJobNames(id));
            targets.add(t);
        }
        return new Plan(LibraryDeletePlan.plan(targets));
    }

    /** Removes the planned chapters from the library list. Nothing is removed from disk yet. */
    public Pending delete(Plan plan) {
        Set<String> ids = new HashSet<>(plan.deletableIds());
        LibraryBulk.Result<DesktopRawStore.Item> result = LibraryBulk.remove(raws.list(), ids, item -> item.id);
        raws.removeRows(new ArrayList<>(ids));
        return new Pending(result.removed);
    }

    /** Puts every removed chapter back. Returns how many came back; zero when the window already ended. */
    public int undo(Pending pending) {
        if (pending == null || pending.settled) return 0;
        pending.settled = true;
        List<DesktopRawStore.Item> back = LibraryBulk.reinsert(new ArrayList<DesktopRawStore.Item>(), pending.removed, item -> item.id);
        return raws.restoreRows(back);
    }

    /** Ends the undo window: the files of the removed chapters are deleted for good. */
    public void expire(Pending pending) {
        if (pending == null || pending.settled) return;
        pending.settled = true;
        List<String> ids = new ArrayList<>();
        for (LibraryBulk.Removed<DesktopRawStore.Item> r : pending.removed) ids.add(r.item.id);
        raws.purgeFiles(ids);
    }

    /** Writes the ticked chapters as .txt files into a folder (the app's normalised text). Returns how many were written. */
    public int export(Collection<String> ids, Path folder) throws IOException {
        Files.createDirectories(folder);
        int n = 0;
        for (String id : ids) {
            DesktopRawStore.Item item = raws.find(id);
            if (item == null) continue;
            String name = item.originalName.isEmpty() ? item.name + ".txt" : item.originalName;
            Path target = folder.resolve(name.replaceAll("[\\\\/:*?\"<>|]", "_"));
            Files.write(target, raws.text(id).getBytes(StandardCharsets.UTF_8));
            n++;
        }
        return n;
    }
}
