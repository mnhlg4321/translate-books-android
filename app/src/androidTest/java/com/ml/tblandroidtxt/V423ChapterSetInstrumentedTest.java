package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * v4.23 L3 on the emulator, synthetic data only: from three ticked RAW the app suggests Glossary/Pronoun by chapter number, rates
 * the rows green / yellow / red, refuses to save while a row is red, saves what the owner confirmed, and an opened set shows a
 * deleted profile as missing without breaking.
 */
@RunWith(AndroidJUnit4.class)
public class V423ChapterSetInstrumentedTest {
    private static final String[] PREFS = {"glossary_store", "pronoun_store", "settings"};
    private static final String TAG = "v423c-";

    @Test
    public void chapterSet_suggestRateConfirmSaveAndOpenWithMissingProfile() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(target);
        List<String> rawIds = new ArrayList<>();
        List<String> setIds = new ArrayList<>();
        try {
            clearAll(target);
            ArrayList<GlossaryStore.Glossary> glossaries = new ArrayList<>();
            glossaries.add(glossary("v423c-g1", "001_" + TAG + "Glossary.csv"));
            glossaries.add(glossary("v423c-g2a", "002_" + TAG + "Glossary_old.csv"));
            glossaries.add(glossary("v423c-g2b", "2_" + TAG + "Glossary_new.csv"));
            assertTrue(GlossaryStore.saveAll(target, glossaries));
            ArrayList<PronounStore.Profile> pronouns = new ArrayList<>();
            pronouns.add(pronoun("v423c-p1", "001_" + TAG + "Pronoun.csv"));
            pronouns.add(pronoun("v423c-p3", "003_" + TAG + "Pronoun.csv"));
            assertTrue(PronounStore.saveAll(target, pronouns));

            for (String name : new String[]{"001_" + TAG + "RAW.txt", "002_" + TAG + "RAW.txt", "003_" + TAG + "RAW.txt"}) {
                RawImportPlanner.Plan plan = RawImportPlanner.plan(java.util.Collections.singletonList(
                        new RawImportPlanner.Candidate(name, name + " 本文です。" + System.nanoTime(), "UTF-8")), LibraryRawStore.hashes(target));
                for (LibraryRawStore.Item i : LibraryRawStore.add(target, plan)) rawIds.add(i.id);
            }
            assertEquals(3, rawIds.size());

            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(activity -> {
                    LibrarySelectionController c = activity.bulkUi.controller(LibraryBulkUi.TAB_RAW);
                    c.enter(null);
                    for (String id : rawIds) c.toggle(id);
                    activity.switchTab("RAW");
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> assertTrue("the bottom bar offers Tạo bộ chương", containsText(activity.getWindow().getDecorView(), "Tạo bộ chương")));
                media("23-raw-selection-create-set");

                scenario.onActivity(activity -> activity.createChapterSetFromSelection());
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                AtomicReference<ChapterSetPageFactory.Draft> draftRef = new AtomicReference<>();
                scenario.onActivity(activity -> draftRef.set(activity.chapterSetDraft));
                ChapterSetPageFactory.Draft draft = draftRef.get();
                assertNotNull(draft);
                assertEquals(3, draft.rows.size());
                assertEquals("v423c-g1", draft.rows.get(0).glossaryId);
                assertEquals("v423c-p1", draft.rows.get(0).pronounId);
                assertEquals(ChapterSetMatcher.Status.GREEN, draft.rows.get(0).status);
                assertEquals("two glossaries share chapter 2: nothing is chosen for the owner", "", draft.rows.get(1).glossaryId);
                assertEquals(ChapterSetMatcher.Status.RED, draft.rows.get(1).status);
                assertEquals(ChapterSetMatcher.Status.YELLOW, draft.rows.get(2).status);
                assertEquals("v423c-p3", draft.rows.get(2).pronounId);
                scenario.onActivity(activity -> {
                    assertTrue(containsText(activity.getWindow().getDecorView(), "xanh 1"));
                    assertTrue(containsText(activity.getWindow().getDecorView(), "đỏ 1"));
                    assertTrue(containsText(activity.getWindow().getDecorView(), "2 Glossary cùng số chương"));
                });
                media("24-chapter-set-table");

                scenario.onActivity(activity -> new ChapterSetPageFactory(activity).save());
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                assertTrue("a red row blocks the save", ChapterSetStore.listSets(target).isEmpty() || !hasSetNamed(target, draft.name));
                media("25-chapter-set-red-blocks-save");
                InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();

                scenario.onActivity(activity -> {
                    ChapterSetMatcher.Row row = activity.chapterSetDraft.rows.get(1);
                    row.glossaryId = "v423c-g2b";
                    row.glossaryChosen = true;
                    row.pronounId = "";
                    row.pronounChosen = true;
                    ChapterSetMatcher.rate(activity.chapterSetDraft.rows);
                    activity.chapterSetDraft.name = TAG + "set";
                    assertEquals(ChapterSetMatcher.Status.GREEN, row.status);
                    new ChapterSetPageFactory(activity).save();
                    assertNull("saving closes the table", activity.chapterSetDraft);
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                List<ChapterSetStore.SetInfo> sets = ChapterSetStore.listSets(target);
                ChapterSetStore.SetInfo mine = null;
                for (ChapterSetStore.SetInfo s : sets) if ((TAG + "set").equals(s.name)) mine = s;
                assertNotNull(mine);
                setIds.add(mine.id);
                assertEquals(3, mine.chapters);
                ChapterSetStore.SetData saved = ChapterSetStore.get(target, mine.id);
                assertEquals(Arrays.asList(1, 2, 3), Arrays.asList(saved.rows.get(0).chapterNo, saved.rows.get(1).chapterNo, saved.rows.get(2).chapterNo));
                assertEquals("v423c-g2b", saved.rows.get(1).glossaryId);
                assertEquals("", saved.rows.get(1).pronounId);
                assertEquals(rawIds.get(2), saved.rows.get(2).rawId);
                scenario.onActivity(activity -> activity.switchTab("ChapterSets"));
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> assertTrue(containsText(activity.getWindow().getDecorView(), TAG + "set")));
                media("26-chapter-set-list");

                // Deleting a profile that is in the set: the set still opens and shows the row as missing.
                scenario.onActivity(activity -> {
                    LibraryDeletePlan.Target t = new LibraryDeletePlan.Target("v423c-g1", "g1");
                    t.chapterSets.addAll(LibraryUsage.chapterSets(activity, LibraryUsage.Kind.GLOSSARY, "v423c-g1"));
                    assertEquals(Arrays.asList(TAG + "set"), t.chapterSets);
                    LibraryDeletePlan plan = LibraryDeletePlan.plan(Arrays.asList(t));
                    assertTrue(plan.needsSecondConfirm());
                    assertTrue(plan.secondMessage().contains(TAG + "set"));
                    activity.bulkUi.executePlan(LibraryBulkUi.TAB_GLOSSARIES, plan);
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                assertNull(GlossaryStore.find(target, "v423c-g1"));
                AtomicReference<ChapterSetPageFactory.Draft> reopened = new AtomicReference<>();
                scenario.onActivity(activity -> {
                    ChapterSetPageFactory.Draft d = ChapterSetPageFactory.openDraft(activity, saved.id);
                    reopened.set(d);
                    activity.chapterSetDraft = d;
                    activity.invalidatePage("ChapterSets");
                    activity.switchTab("ChapterSets");
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                ChapterSetPageFactory.Draft d = reopened.get();
                assertTrue(d.rows.get(0).glossaryMissing);
                assertEquals(ChapterSetMatcher.Status.YELLOW, d.rows.get(0).status);
                assertEquals("v423c-g2b", d.rows.get(1).glossaryId);
                assertEquals("an opened set is not paired again", "v423c-g2b", d.rows.get(1).glossaryId);
                scenario.onActivity(activity -> assertTrue(containsText(activity.getWindow().getDecorView(), "Glossary đã bị xóa")));
                media("27-chapter-set-missing-profile");
                scenario.onActivity(activity -> { activity.chapterSetDraft = null; activity.bulkUi.undoNow(); });
                assertNotNull("undo brings the glossary back", GlossaryStore.find(target, "v423c-g1"));

                // Deleting the whole set (and undoing) keeps rows intact.
                scenario.onActivity(activity -> {
                    LibraryDeletePlan plan = LibraryDeletePlan.plan(Arrays.asList(new LibraryDeletePlan.Target(saved.id, TAG + "set")));
                    activity.bulkUi.executePlan(LibraryBulkUi.TAB_SETS, plan);
                });
                assertNull(ChapterSetStore.get(target, saved.id));
                scenario.onActivity(activity -> activity.bulkUi.undoNow());
                ChapterSetStore.SetData back = ChapterSetStore.get(target, saved.id);
                assertNotNull(back);
                assertEquals(3, back.rows.size());
                assertEquals("v423c-g2b", back.rows.get(1).glossaryId);
            }
        } finally {
            ChapterSetStore.delete(target, setIds);
            LibraryRawStore.removeRows(target, rawIds);
            LibraryRawStore.purgeFiles(target, rawIds);
            restoreAll(target, original);
        }
    }

    private static boolean hasSetNamed(Context c, String name) {
        for (ChapterSetStore.SetInfo s : ChapterSetStore.listSets(c)) if (name.equals(s.name)) return true;
        return false;
    }

    private static GlossaryStore.Glossary glossary(String id, String name) {
        GlossaryStore.Glossary g = new GlossaryStore.Glossary();
        g.id = id; g.name = name;
        g.terms.add(new GlossaryStore.Term("花子", "Hanako", "character"));
        return g;
    }

    private static PronounStore.Profile pronoun(String id, String name) {
        PronounStore.Profile p = new PronounStore.Profile();
        p.id = id; p.name = name;
        p.text = "from,speaker,target,self,call,scope,note\nHanako,,Taro,tôi,cậu,,";
        return p;
    }

    private static void media(String stage) throws InterruptedException {
        if (!"run".equals(InstrumentationRegistry.getArguments().getString("v423_media", ""))) return;
        Thread.sleep(700L);
        android.util.Log.i("V423MEDIA", "stage=" + stage);
        Thread.sleep(5_000L);
    }

    private static boolean containsText(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().contains(text)) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (containsText(group.getChildAt(i), text)) return true;
        }
        return false;
    }

    private static Map<String, Map<String, ?>> snapshot(Context c) {
        Map<String, Map<String, ?>> out = new HashMap<>();
        for (String name : PREFS) out.put(name, new HashMap<>(c.getSharedPreferences(name, Context.MODE_PRIVATE).getAll()));
        return out;
    }

    private static void clearAll(Context c) throws Exception {
        c.getSharedPreferences("glossary_store", Context.MODE_PRIVATE).edit().clear().commit();
        c.getSharedPreferences("pronoun_store", Context.MODE_PRIVATE).edit().clear().putBoolean("legacy_migrated", true).commit();
        clearStoreCache(GlossaryStore.class, "cachedJson", "cachedGlossaries");
        clearStoreCache(PronounStore.class, "cachedJson", "cachedProfiles");
    }

    private static void restoreAll(Context c, Map<String, Map<String, ?>> values) throws Exception {
        for (Map.Entry<String, Map<String, ?>> prefs : values.entrySet()) {
            SharedPreferences.Editor editor = c.getSharedPreferences(prefs.getKey(), Context.MODE_PRIVATE).edit().clear();
            for (Map.Entry<String, ?> entry : prefs.getValue().entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) editor.putString(entry.getKey(), (String) value);
                else if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
                else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
                else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
                else if (value instanceof Float) editor.putFloat(entry.getKey(), (Float) value);
            }
            editor.commit();
        }
        clearStoreCache(GlossaryStore.class, "cachedJson", "cachedGlossaries");
        clearStoreCache(PronounStore.class, "cachedJson", "cachedProfiles");
        clearStoreCache(GlossaryStore.class, "corruptJson", null);
        clearStoreCache(PronounStore.class, "corruptJson", null);
    }

    private static void clearStoreCache(Class<?> type, String jsonField, String itemsField) throws Exception {
        Field json = type.getDeclaredField(jsonField);
        json.setAccessible(true);
        json.set(null, null);
        if (itemsField == null) return;
        Field items = type.getDeclaredField(itemsField);
        items.setAccessible(true);
        items.set(null, null);
    }
}
