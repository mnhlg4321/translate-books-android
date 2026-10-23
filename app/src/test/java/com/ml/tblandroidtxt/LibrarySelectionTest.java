package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/** v4.23 L2: selection state, delete plan with every kind of reference, and the undo list arithmetic. */
public class LibrarySelectionTest {
    @Test public void selectionModeTicksTogglesAndExits() {
        LibrarySelectionController c = new LibrarySelectionController();
        assertFalse(c.isActive());
        c.toggle("a");
        assertEquals("a tap outside selection mode does nothing", 0, c.count());
        c.enter("a");
        assertTrue(c.isActive());
        assertEquals(1, c.count());
        c.toggle("b");
        c.toggle("a");
        assertEquals(Arrays.asList("b"), c.ids());
        assertEquals("1 đã chọn", c.headline());
        c.exit();
        assertFalse(c.isActive());
        assertEquals(0, c.count());
    }

    @Test public void selectAllTicksEveryVisibleThenUnticks() {
        LibrarySelectionController c = new LibrarySelectionController();
        c.enter(null);
        List<String> visible = Arrays.asList("a", "b", "c");
        c.selectAll(visible);
        assertEquals(3, c.count());
        c.selectAll(visible);
        assertEquals(0, c.count());
        c.toggle("b");
        c.selectAll(visible);
        assertEquals("a partial selection becomes a full one", 3, c.count());
        c.retainOnly(Arrays.asList("a", "z"));
        assertEquals(Arrays.asList("a"), c.ids());
    }

    private static LibraryDeletePlan.Target target(String id, String name) { return new LibraryDeletePlan.Target(id, name); }

    @Test public void planLeavesOutItemsWithUnfinishedJobsAndAsksTwiceForUsedOnes() {
        LibraryDeletePlan.Target plain = target("1", "plain");
        LibraryDeletePlan.Target active = target("2", "active"); active.active = true;
        LibraryDeletePlan.Target inSet = target("3", "inSet"); inSet.chapterSets.add("Kumo 7");
        LibraryDeletePlan.Target combo = target("4", "combo"); combo.editorialCombos.add("Tổ hợp A");
        LibraryDeletePlan.Target job = target("5", "job"); job.unfinishedJobs.add("#12 025_RAW.txt");
        LibraryDeletePlan plan = LibraryDeletePlan.plan(Arrays.asList(plain, active, inSet, combo, job));
        assertEquals(Arrays.asList("1", "2", "3", "4"), plan.deletableIds());
        assertEquals(1, plan.blocked.size());
        assertTrue(plan.needsSecondConfirm());
        assertTrue(plan.firstMessage().contains("Xóa 4 mục"));
        assertTrue(plan.firstMessage().contains("Bỏ qua 1 mục"));
        assertTrue(plan.firstMessage().contains("#12 025_RAW.txt"));
        String second = plan.secondMessage();
        assertTrue(second.contains("active — đang Active"));
        assertTrue(second.contains("inSet — nằm trong bộ chương Kumo 7"));
        assertTrue(second.contains("combo — tổ hợp Biên tập Tổ hợp A"));
        assertFalse("plain items are not listed as risky", second.contains("plain"));
    }

    @Test public void planWithOnlyPlainItemsNeedsNoSecondConfirmAndAllBlockedDeletesNothing() {
        LibraryDeletePlan ok = LibraryDeletePlan.plan(Arrays.asList(target("1", "a"), target("2", "b")));
        assertFalse(ok.needsSecondConfirm());
        LibraryDeletePlan.Target job = target("1", "a"); job.unfinishedJobs.add("#1");
        LibraryDeletePlan none = LibraryDeletePlan.plan(Arrays.asList(job));
        assertTrue(none.deletable.isEmpty());
        assertFalse(none.needsSecondConfirm());
        assertTrue(none.firstMessage().startsWith("Không có mục nào xóa được."));
    }

    private static final LibraryBulk.IdOf<String> SELF = item -> item;

    @Test public void removeThenUndoRestoresTheSameOrderAndKeepsNewItems() {
        List<String> list = new ArrayList<>(Arrays.asList("g0", "g1", "g2", "g3", "g4"));
        LibraryBulk.Result<String> r = LibraryBulk.remove(list, new HashSet<>(Arrays.asList("g1", "g3", "missing")), SELF);
        assertEquals(Arrays.asList("g0", "g2", "g4"), r.remaining);
        assertEquals(2, r.removed.size());
        assertEquals(Arrays.asList("g0", "g1", "g2", "g3", "g4"), LibraryBulk.reinsert(r.remaining, r.removed, SELF));
        List<String> changedMeanwhile = new ArrayList<>(Arrays.asList("new", "g0", "g2", "g4"));
        assertEquals("a new item stays; old ones go back next to their neighbours",
                Arrays.asList("new", "g1", "g0", "g2", "g3", "g4").size(), LibraryBulk.reinsert(changedMeanwhile, r.removed, SELF).size());
        List<String> alreadyBack = new ArrayList<>(Arrays.asList("g0", "g1", "g2", "g4"));
        List<String> again = LibraryBulk.reinsert(alreadyBack, r.removed, SELF);
        assertEquals("an id that is back already is not duplicated", 1, java.util.Collections.frequency(again, "g1"));
        assertEquals(5, again.size());
    }

    @Test public void undoAtTheEndAndOnAnEmptyListClampsTheIndex() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        LibraryBulk.Result<String> all = LibraryBulk.remove(list, new HashSet<>(list), SELF);
        assertTrue(all.remaining.isEmpty());
        assertEquals(Arrays.asList("a", "b", "c"), LibraryBulk.reinsert(all.remaining, all.removed, SELF));
        LibraryBulk.Result<String> last = LibraryBulk.remove(list, new HashSet<>(Arrays.asList("c")), SELF);
        assertEquals(Arrays.asList("a", "b", "c"), LibraryBulk.reinsert(last.remaining, last.removed, SELF));
    }

    @Test public void backFromSelectionModeComesBeforeLeavingThePage() {
        assertEquals(AppBackNavigationPolicy.Action.EXIT_SELECTION, AppBackNavigationPolicy.resolve("Glossaries", false, false, "General", true));
        assertEquals(AppBackNavigationPolicy.Action.EXIT_SELECTION, AppBackNavigationPolicy.resolve("RAW", false, false, "General", true));
        assertEquals(AppBackNavigationPolicy.Action.OPEN_TRANSLATE, AppBackNavigationPolicy.resolve("RAW", false, false, "General", false));
        assertEquals(AppBackNavigationPolicy.Action.OPEN_LIBRARY_HOME, AppBackNavigationPolicy.resolve("ChapterSets", false, false, "General", false));
        assertEquals(AppBackNavigationPolicy.Action.OPEN_TRANSLATE, AppBackNavigationPolicy.resolve("Files", false, false, "General", false));
        assertEquals(AppBackNavigationPolicy.Action.SAVE_AND_CLOSE_GLOSSARY, AppBackNavigationPolicy.resolve("Glossaries", true, false, "General", false));
    }
}
