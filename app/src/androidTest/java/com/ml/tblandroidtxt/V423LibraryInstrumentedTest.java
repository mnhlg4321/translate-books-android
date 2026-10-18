package com.ml.tblandroidtxt;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * v4.23 L1/L2 on the emulator with synthetic data only: the RAW tab (bulk import, chapter order, internal reads, delete with undo,
 * jobs that still need a RAW), and multi-select delete/undo on Glossaries and Pronouns including the Active profile. The system
 * picker is not driven: the activity receives the same Intent a picker returns, with file URIs.
 */
@RunWith(AndroidJUnit4.class)
public class V423LibraryInstrumentedTest {
    private static final String[] PREFS = {"glossary_store", "pronoun_store", "settings"};
    private static final String TAG = "v423-";

    @Test
    public void rawTab_importOrderReadDeleteUndoAndBlockedByJob() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(target);
        HashSet<String> existingRaw = new HashSet<>();
        for (LibraryRawStore.Item i : LibraryRawStore.list(target)) existingRaw.add(i.id);
        ArrayList<File> fixtures = new ArrayList<>();
        ArrayList<Long> jobs = new ArrayList<>();
        try {
            Uri a9 = Uri.fromFile(fixture(target, fixtures, "9_" + TAG + "a.txt", "第九章。\n九番目の本文です。"));
            Uri a13 = Uri.fromFile(fixture(target, fixtures, "013_" + TAG + "b.txt", "第十三章。\n十三番目の本文です。\r\n二行目。"));
            Uri a25 = Uri.fromFile(fixture(target, fixtures, "025_" + TAG + "c.txt", "第二十五章。\n二十五番目の本文です。"));
            Uri a100 = Uri.fromFile(fixture(target, fixtures, "100_" + TAG + "d.txt", "第百章。\n百番目の本文です。"));
            Uri vol = Uri.fromFile(fixture(target, fixtures, "VOL08_" + TAG + "e.txt", "巻外の章。\n番号のない本文です。"));
            Uri dup = Uri.fromFile(fixture(target, fixtures, "999_" + TAG + "dup.txt", "第九章。\n九番目の本文です。"));
            Uri blank = Uri.fromFile(fixture(target, fixtures, "998_" + TAG + "blank.txt", "  \n"));

            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(activity -> activity.importRawFiles(selection(a100, a25, a9, vol, a13, dup, blank)));
                long deadline = System.currentTimeMillis() + 15_000L;
                while (System.currentTimeMillis() < deadline && mine(target).size() < 5) Thread.sleep(200L);
                List<LibraryRawStore.Item> items = mine(target);
                assertEquals("5 imported, duplicate and blank skipped", 5, items.size());
                assertEquals(Arrays.asList(9, 13, 25, 100, ChapterNumber.NONE), chapters(items));
                LibraryRawStore.Item i13 = items.get(1);
                assertEquals("013_" + TAG + "b.txt", i13.originalName);

                Uri internal = Uri.parse(i13.uri());
                assertEquals("第十三章。\n十三番目の本文です。\n二行目。", FileUtil.readText(target, internal));
                assertEquals(i13.originalName, FileUtil.displayName(target, internal));
                assertNull("no persisted SAF grant is needed", FileUtil.validateReadable(target, internal, "Input"));
                assertTrue(FileUtil.sizeOf(target, internal) > 0);
                assertTrue(FileUtil.identityOf(target, internal).contains(i13.inputHash));
                assertEquals(HashUtil.sha256("第十三章。\n十三番目の本文です。\n二行目。"), i13.inputHash);

                scenario.onActivity(activity -> activity.switchTab("RAW"));
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> {
                    assertTrue(containsText(activity.getWindow().getDecorView(), "Chương 25"));
                    assertTrue(containsText(activity.getWindow().getDecorView(), "Không có số chương"));
                });
                media("17-raw-tab");

                scenario.onActivity(activity -> {
                    LibrarySelectionController c = activity.bulkUi.controller(LibraryBulkUi.TAB_RAW);
                    c.enter(i13.id);
                    c.toggle(items.get(2).id);
                    c.toggle(items.get(3).id);
                    activity.refreshPagePreservingScroll("RAW");
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> {
                    assertTrue(containsText(activity.getWindow().getDecorView(), "3 đã chọn"));
                    assertTrue(containsText(activity.getWindow().getDecorView(), "Xóa (3)"));
                    assertTrue("Back leaves selection mode first", activity.bulkUi.selecting("RAW"));
                });
                media("18-raw-selection");

                // A job that still needs 100_… blocks only that item.
                TranslationRepository repo = new TranslationRepository(target);
                AppSettings s = AppValidator.normalize(new AppSettings());
                Chunk chunk = new Chunk(0, 0, 10, "", "本文です。", "", "");
                long job = repo.createJob(items.get(3).uri(), "content://fake/out", items.get(3).originalName, s, Arrays.asList(chunk), items.get(3).inputHash, "s");
                jobs.add(job);
                repo.touchJob(job, "RUNNING");
                repo.close();

                AtomicReference<LibraryDeletePlan> planRef = new AtomicReference<>();
                scenario.onActivity(activity -> {
                    List<LibraryDeletePlan.Target> targets = new ArrayList<>();
                    for (LibraryRawStore.Item i : items.subList(1, 4)) {
                        LibraryDeletePlan.Target t = new LibraryDeletePlan.Target(i.id, i.originalName);
                        for (LibraryRawStore.Usage u : LibraryRawStore.unfinishedJobs(activity, i.id)) t.unfinishedJobs.add("#" + u.jobId);
                        targets.add(t);
                    }
                    planRef.set(LibraryDeletePlan.plan(targets));
                    activity.bulkUi.deleteIds(LibraryBulkUi.TAB_RAW, Arrays.asList(i13.id, items.get(2).id, items.get(3).id));
                });
                LibraryDeletePlan plan = planRef.get();
                assertEquals(2, plan.deletable.size());
                assertEquals(1, plan.blocked.size());
                assertEquals(items.get(3).id, plan.blocked.get(0).id);
                media("19-raw-delete-dialog");
                dismissDialogs();

                scenario.onActivity(activity -> activity.bulkUi.executePlan(LibraryBulkUi.TAB_RAW, plan));
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                assertEquals(3, mine(target).size());
                assertFalse(LibraryRawStore.find(target, i13.id) != null);
                assertTrue("files stay during the undo window", LibraryRawStore.files(target).exists(i13.id));
                scenario.onActivity(activity -> {
                    assertTrue(containsText(activity.getWindow().getDecorView(), "Đã xóa 2 mục"));
                    assertTrue(containsText(activity.getWindow().getDecorView(), "Hoàn tác"));
                });
                media("20-raw-undo-bar");

                scenario.onActivity(activity -> activity.bulkUi.undoNow());
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                List<LibraryRawStore.Item> back = mine(target);
                assertEquals("undo restores the same items with the same ids", 5, back.size());
                assertEquals(chapters(items), chapters(back));
                assertEquals(i13.id, back.get(1).id);
                assertEquals("第十三章。\n十三番目の本文です。\n二行目。", FileUtil.readText(target, Uri.parse(back.get(1).uri())));

                scenario.onActivity(activity -> activity.bulkUi.executePlan(LibraryBulkUi.TAB_RAW, plan));
                scenario.onActivity(activity -> activity.bulkUi.finishUndo(true));
                assertFalse("after the window the files are gone", LibraryRawStore.files(target).exists(i13.id));
                assertEquals(3, mine(target).size());
            }
        } finally {
            TranslationRepository repo = new TranslationRepository(target);
            for (long job : jobs) repo.deleteJob(job);
            repo.close();
            List<String> leftover = new ArrayList<>();
            for (LibraryRawStore.Item i : LibraryRawStore.list(target)) if (!existingRaw.contains(i.id)) leftover.add(i.id);
            LibraryRawStore.removeRows(target, leftover);
            LibraryRawStore.purgeFiles(target, leftover);
            for (File f : fixtures) f.delete();
            restoreAll(target, original);
        }
        HashSet<String> after = new HashSet<>();
        for (LibraryRawStore.Item i : LibraryRawStore.list(target)) after.add(i.id);
        assertEquals("pre-existing RAW untouched", existingRaw, after);
    }

    @Test
    public void glossaryAndPronounSelection_deleteUndoAndActiveCleared() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(target);
        try {
            clearAll(target);
            ArrayList<GlossaryStore.Glossary> list = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                GlossaryStore.Glossary g = new GlossaryStore.Glossary();
                g.id = "v423-g" + i; g.name = "v423-glossary-" + i;
                g.terms.add(new GlossaryStore.Term("語" + i, "từ" + i, "term"));
                list.add(g);
            }
            assertTrue(GlossaryStore.saveAll(target, list));
            assertTrue(GlossaryStore.setSelectedId(target, "v423-g1"));
            ArrayList<PronounStore.Profile> pronouns = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                PronounStore.Profile p = new PronounStore.Profile();
                p.id = "v423-p" + i; p.name = "v423-pronoun-" + i;
                p.text = "from,speaker,target,self,call,scope,note\nA" + i + ",,B,tôi,cậu,,";
                pronouns.add(p);
            }
            assertTrue(PronounStore.saveAll(target, pronouns));

            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(activity -> {
                    activity.bulkUi.controller(LibraryBulkUi.TAB_GLOSSARIES).enter("v423-g0");
                    activity.bulkUi.controller(LibraryBulkUi.TAB_GLOSSARIES).toggle("v423-g1");
                    activity.bulkUi.controller(LibraryBulkUi.TAB_GLOSSARIES).toggle("v423-g2");
                    activity.switchTab("Glossaries");
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> assertTrue(containsText(activity.getWindow().getDecorView(), "3 đã chọn")));

                AtomicReference<LibraryDeletePlan> planRef = new AtomicReference<>();
                scenario.onActivity(activity -> {
                    activity.bulkUi.deleteSelected(LibraryBulkUi.TAB_GLOSSARIES);
                    LibraryDeletePlan.Target active = new LibraryDeletePlan.Target("v423-g1", "v423-glossary-1");
                    active.active = GlossaryStore.getSelectedId(activity).equals("v423-g1");
                    planRef.set(LibraryDeletePlan.plan(Arrays.asList(new LibraryDeletePlan.Target("v423-g0", "v423-glossary-0"), active, new LibraryDeletePlan.Target("v423-g2", "v423-glossary-2"))));
                });
                LibraryDeletePlan plan = planRef.get();
                assertTrue("deleting the Active glossary needs the second confirmation", plan.needsSecondConfirm());
                assertTrue(plan.secondMessage().contains("đang Active"));
                media("21-glossary-delete-dialog");
                dismissDialogs();
                scenario.onActivity(activity -> activity.bulkUi.executePlan(LibraryBulkUi.TAB_GLOSSARIES, plan));
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                assertEquals(1, GlossaryStore.loadAll(target).size());
                assertEquals("v423-g3", GlossaryStore.loadAll(target).get(0).id);
                assertEquals("Active glossary is cleared when it is deleted", "", GlossaryStore.getSelectedId(target));
                scenario.onActivity(activity -> assertTrue(containsText(activity.getWindow().getDecorView(), "Hoàn tác")));
                media("22-glossary-undo-bar");

                scenario.onActivity(activity -> activity.bulkUi.undoNow());
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                List<GlossaryStore.Glossary> back = GlossaryStore.loadAll(target);
                assertEquals(4, back.size());
                assertEquals(Arrays.asList("v423-g0", "v423-g1", "v423-g2", "v423-g3"), ids(back));
                assertEquals("Active glossary restored by undo", "v423-g1", GlossaryStore.getSelectedId(target));

                scenario.onActivity(activity -> {
                    activity.bulkUi.controller(LibraryBulkUi.TAB_PRONOUNS).enter("v423-p0");
                    activity.bulkUi.controller(LibraryBulkUi.TAB_PRONOUNS).toggle("v423-p2");
                    LibraryDeletePlan p = LibraryDeletePlan.plan(Arrays.asList(new LibraryDeletePlan.Target("v423-p0", "a"), new LibraryDeletePlan.Target("v423-p2", "c")));
                    assertFalse(p.needsSecondConfirm());
                    activity.bulkUi.executePlan(LibraryBulkUi.TAB_PRONOUNS, p);
                    assertEquals(Arrays.asList("v423-p1"), pronounIds(activity));
                    activity.bulkUi.undoNow();
                    assertEquals(Arrays.asList("v423-p0", "v423-p1", "v423-p2"), pronounIds(activity));
                });

                scenario.onActivity(activity -> {
                    activity.bulkUi.controller(LibraryBulkUi.TAB_GLOSSARIES).exit();
                    activity.bulkUi.controller(LibraryBulkUi.TAB_GLOSSARIES).enter("v423-g2");
                    activity.bulkUi.useSelected(LibraryBulkUi.TAB_GLOSSARIES);
                    assertEquals("Use on exactly one selected glossary makes it Active", "v423-g2", GlossaryStore.getSelectedId(activity));
                });
            }
        } finally {
            restoreAll(target, original);
        }
        assertEquals(original, snapshot(target));
    }

    // ---- helpers ----

    private static List<LibraryRawStore.Item> mine(Context c) {
        ArrayList<LibraryRawStore.Item> out = new ArrayList<>();
        for (LibraryRawStore.Item i : LibraryRawStore.list(c)) if (i.originalName.contains(TAG)) out.add(i);
        return out;
    }

    private static List<Integer> chapters(List<LibraryRawStore.Item> items) {
        ArrayList<Integer> out = new ArrayList<>();
        for (LibraryRawStore.Item i : items) out.add(i.chapterNo);
        return out;
    }

    private static List<String> ids(List<GlossaryStore.Glossary> list) {
        ArrayList<String> out = new ArrayList<>();
        for (GlossaryStore.Glossary g : list) out.add(g.id);
        return out;
    }

    private static List<String> pronounIds(Context c) {
        ArrayList<String> out = new ArrayList<>();
        for (PronounStore.Profile p : PronounStore.loadAll(c)) out.add(p.id);
        return out;
    }

    /** With {@code -e v423_media run} the screen is held so the shell can take a picture. */
    private static void media(String stage) throws InterruptedException {
        if (!"run".equals(InstrumentationRegistry.getArguments().getString("v423_media", ""))) return;
        Thread.sleep(700L);
        android.util.Log.i("V423MEDIA", "stage=" + stage);
        Thread.sleep(5_000L);
    }

    private static void dismissDialogs() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static Intent selection(Uri... uris) {
        ClipData clip = new ClipData("raw", new String[]{"text/plain"}, new ClipData.Item(uris[0]));
        for (int i = 1; i < uris.length; i++) clip.addItem(new ClipData.Item(uris[i]));
        Intent result = new Intent();
        result.setClipData(clip);
        return result;
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

    /** A file whose name starts with the wanted display name (the name is kept exactly: it lives in its own cache folder). */
    private static File fixture(Context context, List<File> fixtures, String name, String text) throws Exception {
        File dir = new File(context.getCacheDir(), "v423-fixtures-" + System.nanoTime());
        assertTrue(dir.mkdirs());
        File file = new File(dir, name);
        try (FileOutputStream output = new FileOutputStream(file)) { output.write(text.getBytes(StandardCharsets.UTF_8)); }
        fixtures.add(file);
        fixtures.add(dir);
        return file;
    }
}
