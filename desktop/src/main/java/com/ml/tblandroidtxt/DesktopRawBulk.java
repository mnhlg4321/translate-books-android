package com.ml.tblandroidtxt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Bulk delete, undo and export of ticked RAW chapters on the PC. The plan and its messages come from {@link DesktopLibraryBulk}, the same
 * as for the other Library lists. A delete removes the index rows at once; the files are removed only when the ten second undo window
 * ends ({@link #expire}), so {@link #undo} can bring everything back.
 */
public final class DesktopRawBulk {
    public static final long UNDO_MS = DesktopLibraryBulk.UNDO_MS;

    /** A finished delete that can still be undone: the removed items, kept until their files are purged. */
    public static final class Pending {
        private final List<DesktopRawStore.Item> items;
        private boolean settled;

        private Pending(List<DesktopRawStore.Item> items) { this.items = items; }

        public int count() { return items.size(); }
    }

    private final DesktopRawStore raws;

    DesktopRawBulk(DesktopRawStore raws) { this.raws = raws; }

    /** The plan for the ticked RAW ids: each chapter with the chapter sets that list it, and the jobs that still need it. */
    public DesktopLibraryBulk.Plan plan(Collection<String> ids) {
        ArrayList<LibraryDeletePlan.Target> targets = new ArrayList<>();
        for (String id : new LinkedHashSet<>(ids)) {
            DesktopRawStore.Item item = raws.find(id);
            if (item == null) continue;
            LibraryDeletePlan.Target t = new LibraryDeletePlan.Target(item.id, item.name);
            t.chapterSets.addAll(raws.chapterSetNames(id));
            t.unfinishedJobs.addAll(raws.unfinishedJobNames(id));
            targets.add(t);
        }
        return DesktopLibraryBulk.Plan.of(targets);
    }

    /** Removes the planned chapters from the library list. Nothing is removed from disk yet. */
    public Pending delete(DesktopLibraryBulk.Plan plan) {
        return removeRows(plan.deletableIds());
    }

    /** Removes the index rows of these RAW but keeps their files until {@link #expire}; used by the Tập delete that takes RAW with it. */
    public Pending removeRows(Collection<String> ids) {
        List<DesktopRawStore.Item> items = new ArrayList<>();
        for (String id : ids) {
            DesktopRawStore.Item item = raws.find(id);
            if (item != null) items.add(item);
        }
        List<String> rows = new ArrayList<>();
        for (DesktopRawStore.Item item : items) rows.add(item.id);
        raws.removeRows(rows);
        return new Pending(items);
    }

    /** Puts every removed chapter back. Returns how many came back; zero when the window already ended. */
    public int undo(Pending pending) {
        if (pending == null || pending.settled) return 0;
        pending.settled = true;
        return raws.restoreRows(pending.items);
    }

    /** Ends the undo window: the files of the removed chapters are deleted for good. */
    public void expire(Pending pending) {
        if (pending == null || pending.settled) return;
        pending.settled = true;
        List<String> ids = new ArrayList<>();
        for (DesktopRawStore.Item item : pending.items) ids.add(item.id);
        raws.purgeFiles(ids);
    }

    /** Writes the ticked chapters as .txt files into a folder (the app's normalised text), never over an existing file. Returns how many were written. */
    public int export(Collection<String> ids, Path folder) throws IOException {
        int n = 0;
        for (String id : ids) {
            DesktopRawStore.Item item = raws.find(id);
            if (item == null) continue;
            String name = item.originalName.isEmpty() ? item.name + ".txt" : item.originalName;
            DesktopLibraryBulk.writeNew(folder, name, raws.text(id).getBytes(StandardCharsets.UTF_8));
            n++;
        }
        return n;
    }
}
