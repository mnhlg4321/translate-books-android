package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multi-select for the Library lists (RAW, Glossaries, Pronouns, chapter sets): selection header, selectable cards,
 * bottom action bar, bulk delete with a second confirmation and a 10 second undo, and export of the ticked items.
 */
final class LibraryBulkUi {
    static final long UNDO_MS = 10_000L;
    static final String TAB_RAW = "RAW", TAB_GLOSSARIES = "Glossaries", TAB_PRONOUNS = "Pronouns", TAB_SETS = "ChapterSets";

    private final MainActivity a;
    private final Map<String, LibrarySelectionController> controllers = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Undo undo;

    private static final class Undo {
        final String tab;
        final String message;
        final Runnable restore;
        final Runnable expire;
        Runnable timer;
        Undo(String tab, String message, Runnable restore, Runnable expire) { this.tab = tab; this.message = message; this.restore = restore; this.expire = expire; }
    }

    LibraryBulkUi(MainActivity activity) { a = activity; }

    LibrarySelectionController controller(String tab) {
        LibrarySelectionController c = controllers.get(tab);
        if (c == null) { c = new LibrarySelectionController(); controllers.put(tab, c); }
        return c;
    }

    boolean selecting(String tab) { return AppBackNavigationPolicy.isLibraryList(tab) && controller(tab).isActive(); }

    void exitSelection(String tab) {
        controller(tab).exit();
        refresh(tab);
    }

    /** Leaves selection mode on every list except {@code keepTab}; called when the tab changes. */
    void exitOthers(String keepTab) {
        for (Map.Entry<String, LibrarySelectionController> e : controllers.entrySet()) if (!e.getKey().equals(keepTab)) e.getValue().exit();
    }

    void refresh(String tab) { a.refreshPagePreservingScroll(tab); }

    /** Parts of the open list that show the selection count (header, bottom bar), updated in place when one card is ticked. */
    private final Map<String, Runnable> headerUpdater = new java.util.HashMap<>();
    private final Map<String, java.util.List<Runnable>> barUpdaters = new java.util.HashMap<>();

    private void updateCounts(String tab) {
        Runnable header = headerUpdater.get(tab);
        if (header != null) header.run();
        java.util.List<Runnable> list = barUpdaters.get(tab);
        if (list != null) for (Runnable r : list) r.run();
    }

    // ---- list header, cards, bottom bar ----

    View selectButton(String tab) {
        return a.secondaryButton("Chọn", v -> { controller(tab).enter(null); refresh(tab); });
    }

    /** "n đã chọn · Chọn tất cả · Hủy" at the top of a list in selection mode. */
    View selectionHeader(String tab, Collection<String> visibleIds) {
        LibrarySelectionController c = controller(tab);
        c.retainOnly(visibleIds);
        LinearLayout row = a.rowContainer();
        TextView count = a.text(c.headline(), 14, a.TEXT, true);
        count.setContentDescription("selection-count");
        headerUpdater.put(tab, () -> count.setText(c.headline()));
        row.addView(count, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(a.secondaryButton("Chọn tất cả", v -> { c.selectAll(visibleIds); refresh(tab); }), new LinearLayout.LayoutParams(-2, a.dp(40)));
        row.addView(a.space(6, 1));
        row.addView(a.secondaryButton("Hủy", v -> exitSelection(tab)), new LinearLayout.LayoutParams(-2, a.dp(40)));
        return row;
    }

    /** A card with a tick box; tapping anywhere on it toggles the selection. */
    View selectableCard(String tab, String id, String title, String meta, int borderColor) {
        LibrarySelectionController c = controller(tab);
        boolean on = c.isSelected(id);
        LinearLayout box = a.card(14, a.FIELD, on ? a.BLUE : borderColor);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
        CheckBox check = new CheckBox(a);
        check.setChecked(on);
        check.setClickable(false);
        check.setFocusable(false);
        box.addView(check, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = a.text(title, 15, a.TEXT, true);
        t.setSingleLine(false);
        texts.addView(t);
        TextView m = a.text(meta, 12, a.MUTED, false);
        m.setSingleLine(false);
        texts.addView(m, a.marginLP(-1, -2, 0, 3, 0, 0));
        box.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        box.setContentDescription((on ? "selected:" : "unselected:") + title);
        // Only this card and the counts change: rebuilding the page for every tick made long lists stutter.
        box.setOnClickListener(v -> {
            c.toggle(id);
            boolean now = c.isSelected(id);
            check.setChecked(now);
            a.tint(box, a.FIELD, now ? a.BLUE : borderColor, 1, 14);
            box.setContentDescription((now ? "selected:" : "unselected:") + title);
            updateCounts(tab);
        });
        return box;
    }

    /** Long-press on a normal card starts selection with that card ticked. */
    void attachLongPress(View card, String tab, String id) {
        card.setOnLongClickListener(v -> { controller(tab).enter(id); refresh(tab); return true; });
    }

    /** Bottom action bar, or null outside selection mode. */
    View bottomBar(String tab) {
        LibrarySelectionController c = controller(tab);
        if (!selecting(tab)) return null;
        int n = c.count();
        LinearLayout bar = a.card(14, a.PANEL, a.BORDER);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(a.dp(8), a.dp(8), a.dp(8), a.dp(8));
        android.widget.Button delete = a.dangerButton("Xóa (" + n + ")", v -> deleteSelected(tab));
        delete.setEnabled(n > 0);
        bar.addView(delete, new LinearLayout.LayoutParams(0, a.dp(44), 1));
        final java.util.List<Runnable> barList = new java.util.ArrayList<>();
        barUpdaters.put(tab, barList);
        barList.add(() -> { int k = c.count(); delete.setText("Xóa (" + k + ")"); a.setButtonEnabled(delete, k > 0); });
        if (!TAB_SETS.equals(tab)) {
            bar.addView(a.space(6, 1));
            android.widget.Button export = a.secondaryButton("Xuất", v -> exportSelected(tab));
            export.setEnabled(n > 0);
            bar.addView(export, new LinearLayout.LayoutParams(0, a.dp(44), 1));
            barList.add(() -> a.setButtonEnabled(export, c.count() > 0));
        }
        if (TAB_GLOSSARIES.equals(tab) || TAB_PRONOUNS.equals(tab)) {
            bar.addView(a.space(6, 1));
            android.widget.Button use = a.primaryButton("Dùng", v -> useSelected(tab));
            use.setEnabled(n == 1);
            bar.addView(use, new LinearLayout.LayoutParams(0, a.dp(44), 1));
            barList.add(() -> a.setButtonEnabled(use, c.count() == 1));
        }
        if (TAB_RAW.equals(tab)) {
            bar.addView(a.space(6, 1));
            android.widget.Button sets = a.primaryButton("Tạo tổ hợp", v -> a.createChapterSetFromSelection());
            sets.setEnabled(n > 0);
            bar.addView(sets, new LinearLayout.LayoutParams(0, a.dp(44), 1));
            barList.add(() -> a.setButtonEnabled(sets, c.count() > 0));
        }
        bar.setLayoutParams(a.marginLP(-1, -2, 0, 6, 0, 0));
        return bar;
    }

    // ---- first row of a list and the shared notice bar ----

    /** First row of every Library list: how many items there are, and the "Chọn" button that enters selection. */
    View headerRow(String tab, int count) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView n = a.text(count + " mục", 13, a.GREEN, true);
        n.setContentDescription("library-count");
        row.addView(n, new LinearLayout.LayoutParams(0, -2, 1));
        if (count > 0) row.addView(selectButton(tab), new LinearLayout.LayoutParams(-2, -2));
        return row;
    }

    private String noticeSummary = "";
    private String noticeDetail = "";
    private Runnable noticeTimer;

    /**
     * One message for every Library import, in the same bar above the list as the undo bar: "Đã thêm n · bỏ qua m (trùng/lỗi)", with the
     * detail behind "Xem chi tiết". It stays for the same ten seconds as an undo and replaces the older message.
     */
    void notice(String tab, String summary, String detail) {
        noticeSummary = summary == null ? "" : summary;
        noticeDetail = detail == null ? "" : detail;
        if (noticeTimer != null) handler.removeCallbacks(noticeTimer);
        noticeTimer = () -> { noticeSummary = ""; noticeDetail = ""; a.invalidateLibraryPages(); refresh(tab); };
        handler.postDelayed(noticeTimer, UNDO_MS + 2_000L);
        refresh(tab);
    }

    void clearNotice() {
        if (noticeTimer != null) handler.removeCallbacks(noticeTimer);
        noticeSummary = ""; noticeDetail = "";
    }

    View noticeBar() {
        if (noticeSummary.isEmpty()) return null;
        LinearLayout bar = a.card(14, a.PANEL, a.BLUE);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(a.dp(12), a.dp(8), a.dp(8), a.dp(8));
        TextView msg = a.text(noticeSummary, 14, a.TEXT, true);
        msg.setSingleLine(false);
        msg.setContentDescription("library-notice");
        bar.addView(msg);
        LinearLayout actions = new LinearLayout(a);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        if (!noticeDetail.isEmpty()) {
            final String detail = noticeDetail;
            actions.addView(a.secondaryButton("Xem chi tiết", v -> new AlertDialog.Builder(a).setTitle("Chi tiết").setMessage(detail).setPositiveButton("Đóng", null).show()), new LinearLayout.LayoutParams(0, -2, 1));
            actions.addView(a.space(6, 1));
        }
        actions.addView(a.secondaryButton("Đóng", v -> { clearNotice(); a.invalidateLibraryPages(); a.refreshPagePreservingScroll(a.currentTab); }), new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(actions, a.marginLP(-1, -2, 0, 6, 0, 0));
        bar.setLayoutParams(a.marginLP(-1, -2, 0, 6, 0, 0));
        return bar;
    }

    // ---- delete with undo ----

    View undoBar() {
        if (undo == null) return null;
        Undo u = undo;
        LinearLayout bar = a.card(14, a.PANEL, a.GREEN);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(a.dp(12), a.dp(8), a.dp(8), a.dp(8));
        TextView msg = a.text(u.message, 14, a.TEXT, true);
        msg.setSingleLine(false);
        msg.setContentDescription("undo-message");
        bar.addView(msg, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(a.primaryButton("Hoàn tác", v -> undoNow()), new LinearLayout.LayoutParams(-2, a.dp(44)));
        bar.setLayoutParams(a.marginLP(-1, -2, 0, 6, 0, 0));
        return bar;
    }

    private void startUndo(Undo next) {
        finishUndo(true);
        undo = next;
        next.timer = () -> { if (undo == next) { finishUndo(true); a.invalidateLibraryPages(); } };
        handler.postDelayed(next.timer, UNDO_MS);
        refresh(next.tab);
    }

    /** Ends a pending undo window; {@code expire} true makes the delete final (RAW files are removed). */
    void finishUndo(boolean expire) {
        Undo u = undo;
        if (u == null) return;
        undo = null;
        if (u.timer != null) handler.removeCallbacks(u.timer);
        if (expire) u.expire.run();
    }

    void undoNow() {
        Undo u = undo;
        if (u == null) return;
        undo = null;
        if (u.timer != null) handler.removeCallbacks(u.timer);
        u.restore.run();
        a.invalidateLibraryPages();
        refresh(u.tab);
    }

    void deleteSelected(String tab) { deleteIds(tab, controller(tab).ids()); }

    /** Bulk or single delete of the given ids (a card's own Del button passes one id without entering selection mode). */
    void deleteIds(String tab, List<String> ids) {
        if (ids.isEmpty()) return;
        if (!a.ensureConfigMutable()) return;
        if ((TAB_GLOSSARIES.equals(tab) && GlossaryStore.isCorrupt(a)) || (TAB_PRONOUNS.equals(tab) && PronounStore.isCorrupt(a))) { a.toast(MainActivity.LIBRARY_WRITE_REFUSED); return; }
        List<LibraryDeletePlan.Target> targets = targetsFor(tab, ids);
        LibraryDeletePlan plan = LibraryDeletePlan.plan(targets);
        AlertDialog.Builder first = new AlertDialog.Builder(a).setTitle(plan.deletable.isEmpty() ? "Không xóa được" : "Xóa " + plan.deletable.size() + " mục?").setMessage(plan.firstMessage()).setNegativeButton("Hủy", null);
        if (!plan.deletable.isEmpty()) {
            first.setPositiveButton("Xóa", (d, w) -> {
                if (!plan.needsSecondConfirm()) { executePlan(tab, plan); return; }
                new AlertDialog.Builder(a).setTitle("Xác nhận lần hai").setMessage(plan.secondMessage())
                        .setNegativeButton("Hủy", null).setPositiveButton("Vẫn xóa", (d2, w2) -> executePlan(tab, plan)).show();
            });
        }
        first.show();
    }

    private List<LibraryDeletePlan.Target> targetsFor(String tab, List<String> ids) {
        ArrayList<LibraryDeletePlan.Target> out = new ArrayList<>();
        if (TAB_RAW.equals(tab)) {
            Map<String, LibraryRawStore.Item> byId = new HashMap<>();
            for (LibraryRawStore.Item i : LibraryRawStore.list(a)) byId.put(i.id, i);
            for (String id : ids) {
                LibraryRawStore.Item item = byId.get(id);
                if (item == null) continue;
                LibraryDeletePlan.Target t = new LibraryDeletePlan.Target(id, item.originalName);
                for (LibraryRawStore.Usage u : LibraryRawStore.unfinishedJobs(a, id)) t.unfinishedJobs.add("#" + u.jobId + " " + u.fileName);
                t.chapterSets.addAll(LibraryUsage.chapterSets(a, LibraryUsage.Kind.RAW, id));
                out.add(t);
            }
        } else if (TAB_GLOSSARIES.equals(tab)) {
            String active = GlossaryStore.getSelectedId(a);
            for (GlossaryStore.Glossary g : GlossaryStore.loadAll(a)) if (ids.contains(g.id)) {
                LibraryDeletePlan.Target t = new LibraryDeletePlan.Target(g.id, g.name);
                t.active = g.id.equals(active);
                t.chapterSets.addAll(LibraryUsage.chapterSets(a, LibraryUsage.Kind.GLOSSARY, g.id));
                t.editorialCombos.addAll(LibraryUsage.editorialCombos(a, LibraryUsage.Kind.GLOSSARY, g.id));
                out.add(t);
            }
        } else if (TAB_PRONOUNS.equals(tab)) {
            String active = PronounStore.getSelectedId(a);
            for (PronounStore.Profile p : PronounStore.loadAll(a)) if (ids.contains(p.id)) {
                LibraryDeletePlan.Target t = new LibraryDeletePlan.Target(p.id, p.name);
                t.active = p.id.equals(active);
                t.chapterSets.addAll(LibraryUsage.chapterSets(a, LibraryUsage.Kind.PRONOUN, p.id));
                t.editorialCombos.addAll(LibraryUsage.editorialCombos(a, LibraryUsage.Kind.PRONOUN, p.id));
                out.add(t);
            }
        } else if (TAB_SETS.equals(tab)) {
            for (ChapterSetStore.SetInfo s : ChapterSetStore.listSets(a)) if (ids.contains(s.id)) out.add(new LibraryDeletePlan.Target(s.id, s.name));
        }
        return out;
    }

    void executePlan(String tab, LibraryDeletePlan plan) {
        if (!a.ensureConfigMutable()) return;
        Set<String> ids = new HashSet<>(plan.deletableIds());
        int n = ids.size();
        String message = "Đã xóa " + n + " mục · Hoàn tác";
        controller(tab).exit();
        if (TAB_GLOSSARIES.equals(tab)) {
            List<GlossaryStore.Glossary> all = GlossaryStore.loadAll(a);
            LibraryBulk.Result<GlossaryStore.Glossary> r = LibraryBulk.remove(all, ids, g -> g.id);
            if (!GlossaryStore.saveAll(a, r.remaining)) { a.toast(MainActivity.LIBRARY_WRITE_REFUSED); refresh(tab); return; }
            String active = GlossaryStore.getSelectedId(a);
            final boolean wasActive = ids.contains(active);
            if (wasActive) a.clearGlossarySelection();
            startUndo(new Undo(tab, message, () -> {
                List<GlossaryStore.Glossary> back = LibraryBulk.reinsert(GlossaryStore.loadAll(a), r.removed, g -> g.id);
                if (!GlossaryStore.saveAll(a, back)) { a.toast(MainActivity.LIBRARY_WRITE_REFUSED); return; }
                if (wasActive && GlossaryStore.getSelectedId(a).isEmpty()) a.selectGlossary(GlossaryStore.find(a, active), false);
                a.toast("Đã hoàn tác " + n + " mục");
            }, () -> { }));
        } else if (TAB_PRONOUNS.equals(tab)) {
            List<PronounStore.Profile> all = PronounStore.loadAll(a);
            LibraryBulk.Result<PronounStore.Profile> r = LibraryBulk.remove(all, ids, p -> p.id);
            if (!PronounStore.saveAll(a, r.remaining)) { a.toast(MainActivity.LIBRARY_WRITE_REFUSED); refresh(tab); return; }
            String active = PronounStore.getSelectedId(a);
            final boolean wasActive = ids.contains(active);
            if (wasActive) { PronounStore.setSelectedId(a, ""); a.applyActivePronoun(null); }
            startUndo(new Undo(tab, message, () -> {
                List<PronounStore.Profile> back = LibraryBulk.reinsert(PronounStore.loadAll(a), r.removed, p -> p.id);
                if (!PronounStore.saveAll(a, back)) { a.toast(MainActivity.LIBRARY_WRITE_REFUSED); return; }
                if (wasActive && PronounStore.getSelectedId(a).isEmpty()) a.selectPronoun(PronounStore.find(a, active), false);
                a.toast("Đã hoàn tác " + n + " mục");
            }, () -> { }));
        } else if (TAB_RAW.equals(tab)) {
            List<LibraryRawStore.Item> removed = new ArrayList<>();
            for (LibraryRawStore.Item i : LibraryRawStore.list(a)) if (ids.contains(i.id)) removed.add(i);
            List<String> removedIds = new ArrayList<>(ids);
            LibraryRawStore.removeRows(a, removedIds);
            startUndo(new Undo(tab, message, () -> { LibraryRawStore.restore(a, removed); a.toast("Đã hoàn tác " + n + " mục"); },
                    () -> LibraryRawStore.purgeFiles(a, removedIds)));
        } else if (TAB_SETS.equals(tab)) {
            List<ChapterSetStore.SetData> removed = ChapterSetStore.snapshot(a, new ArrayList<>(ids));
            ChapterSetStore.delete(a, new ArrayList<>(ids));
            startUndo(new Undo(tab, message, () -> { ChapterSetStore.restore(a, removed); a.toast("Đã hoàn tác " + n + " mục"); }, () -> { }));
        }
    }

    // ---- export / use ----

    void exportSelected(String tab) {
        LibrarySelectionController c = controller(tab);
        if (c.count() == 0) return;
        try {
            Set<String> ids = new HashSet<>(c.ids());
            List<GlossaryStore.Glossary> glossaries = new ArrayList<>();
            List<PronounStore.Profile> pronouns = new ArrayList<>();
            List<LibraryArchive.RawEntry> raws = new ArrayList<>();
            if (TAB_GLOSSARIES.equals(tab)) { for (GlossaryStore.Glossary g : GlossaryStore.loadAll(a)) if (ids.contains(g.id)) glossaries.add(g); }
            else if (TAB_PRONOUNS.equals(tab)) { for (PronounStore.Profile p : PronounStore.loadAll(a)) if (ids.contains(p.id)) pronouns.add(p); }
            else if (TAB_RAW.equals(tab)) {
                List<LibraryRawStore.Item> items = new ArrayList<>();
                for (LibraryRawStore.Item i : LibraryRawStore.list(a)) if (ids.contains(i.id)) items.add(i);
                raws = LibraryRawStore.entries(a, items);
            }
            byte[] zip = LibraryArchive.build(glossaries, pronouns, raws, "", "", AppBuildInfo.VERSION_NAME, System.currentTimeMillis());
            a.startSelectedLibraryExport(zip, glossaries.size() + " glossary, " + pronouns.size() + " pronoun, " + raws.size() + " RAW");
        } catch (Exception e) {
            a.toast("Không xuất được: " + AppValidator.readableError(e));
        }
    }

    void useSelected(String tab) {
        LibrarySelectionController c = controller(tab);
        if (c.count() != 1) return;
        String id = c.ids().get(0);
        c.exit();
        if (TAB_GLOSSARIES.equals(tab)) a.selectGlossary(GlossaryStore.find(a, id), false);
        else if (TAB_PRONOUNS.equals(tab)) a.selectPronoun(PronounStore.find(a, id), false);
    }
}
