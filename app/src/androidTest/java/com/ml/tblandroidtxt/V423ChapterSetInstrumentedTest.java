package com.ml.tblandroidtxt;

import android.app.Dialog;
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
import java.util.Collections;
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
 * v4.23 U1-U3 on the emulator with synthetic data and very long file names: the picker sheet stays open when a row is tapped,
 * marks it as chosen and only changes the slot on the main button; Hủy changes nothing; no file name is cut (also at font scale 1.3);
 * a combination is created from the three slots, rated, saved and shown on the Translate screen with all three names per chapter.
 */
@RunWith(AndroidJUnit4.class)
public class V423ChapterSetInstrumentedTest {
    private static final String[] PREFS = {"glossary_store", "pronoun_store", "settings", "translate_source"};
    private static final String TAG = "v423c-";
    private static final String LONG = "_KUMO_VOL08_QA_STATE_CANON_CANDIDATE_ADD_WIP_R256_EXTRA_LONG_NAME_FOR_WRAP_TEST_" + TAG;

    private static String rawName(int n) { return String.format("%03d%sRAW.txt", n, LONG); }
    private static String glossaryName(int n, String sfx) { return String.format("%03d%sGlossary%s.csv", n, LONG, sfx); }
    private static String pronounName(int n) { return String.format("%03d%sPronoun_PARTIAL.csv", n, LONG); }

    private static final class Seed {
        final List<String> rawIds = new ArrayList<>();
        final List<String> setIds = new ArrayList<>();
    }

    private Seed seed(Context c) throws Exception {
        Seed s = new Seed();
        ArrayList<GlossaryStore.Glossary> g = new ArrayList<>();
        for (String[] spec : new String[][]{{"v423c-g1", glossaryName(1, "")}, {"v423c-g2a", glossaryName(2, "_old")}, {"v423c-g2b", glossaryName(2, "_new")}, {"v423c-g3", glossaryName(3, "")}}) {
            GlossaryStore.Glossary x = new GlossaryStore.Glossary();
            x.id = spec[0]; x.name = spec[1]; x.terms.add(new GlossaryStore.Term("花子", "Hanako", "character"));
            g.add(x);
        }
        assertTrue(GlossaryStore.saveAll(c, g));
        ArrayList<PronounStore.Profile> p = new ArrayList<>();
        for (int n : new int[]{1, 3}) {
            PronounStore.Profile x = new PronounStore.Profile();
            x.id = "v423c-p" + n; x.name = pronounName(n); x.text = "from,speaker,target,self,call,scope,note\nHanako,,Taro,tôi,cậu,,";
            p.add(x);
        }
        assertTrue(PronounStore.saveAll(c, p));
        for (int n = 1; n <= 3; n++) {
            String name = rawName(n);
            RawImportPlanner.Plan plan = RawImportPlanner.plan(Collections.singletonList(new RawImportPlanner.Candidate(name, name + " 本文です。" + System.nanoTime(), "UTF-8")), LibraryRawStore.hashes(c));
            for (LibraryRawStore.Item i : LibraryRawStore.add(c, plan)) s.rawIds.add(i.id);
        }
        return s;
    }

    private void cleanup(Context c, Seed s, Map<String, Map<String, ?>> original) throws Exception {
        ChapterSetStore.delete(c, s.setIds);
        for (ChapterSetStore.SetInfo i : ChapterSetStore.listSets(c)) if (i.name.contains(TAG)) ChapterSetStore.delete(c, Collections.singletonList(i.id));
        LibraryRawStore.removeRows(c, s.rawIds);
        LibraryRawStore.purgeFiles(c, s.rawIds);
        for (LibraryRawStore.Item i : LibraryRawStore.list(c)) if (i.originalName.contains("v423")) { LibraryRawStore.removeRows(c, Collections.singletonList(i.id)); LibraryRawStore.purgeFiles(c, Collections.singletonList(i.id)); }
        restoreAll(c, original);
    }

    @Test
    public void comboFromThreeSlots_sheetStaysOpen_noClippedNames_saveAndTranslateScreen() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        Seed seed = seed(c);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> { a.chapterSetDraft = ChapterSetPageFactory.newDraft(a, Collections.<String>emptyList()); a.invalidatePage("ChapterSets"); a.switchTab("ChapterSets"); });
            idle();
            scenario.onActivity(a -> assertTrue(containsText(a.getWindow().getDecorView(), "Tập mới")));
            media("31-combo-new-empty", scenario);

            // RAW sheet: tapping a row keeps the sheet open and marks the row; Hủy leaves the slot as it was.
            scenario.onActivity(a -> new ChapterSetPageFactory(a).pickRaw());
            idle();
            Dialog sheet = FilePickerSheet.last.dialog();
            assertTrue(sheet.isShowing());
            tapRow(scenario, sheet, rawName(1));
            assertTrue("the sheet stays open after a tap", sheet.isShowing());
            assertNotNull(findByDescription(sheet.getWindow().getDecorView(), "đã chọn: " + rawName(1)));
            assertTrue(textOf(sheet, "picker-headline").contains("Đã chọn 1"));
            assertNoEllipsis(sheet.getWindow().getDecorView(), "name:");
            media("32-picker-raw-long-names", scenario);
            scenario.onActivity(a -> sheet.cancel());
            idle();
            assertTrue("Hủy changes nothing", draftOf(scenario).rawIds.isEmpty());

            // Reopen, choose all three, confirm.
            scenario.onActivity(a -> new ChapterSetPageFactory(a).pickRaw());
            idle();
            Dialog sheet2 = FilePickerSheet.last.dialog();
            for (int n = 1; n <= 3; n++) tapRow(scenario, sheet2, rawName(n));
            assertTrue(sheet2.isShowing());
            assertTrue(textOf(sheet2, "picker-headline").contains("Đã chọn 3"));
            clickByDescription(scenario, sheet2, "picker-confirm");
            idle();
            assertFalse(sheet2.isShowing());
            assertEquals(3, draftOf(scenario).rawIds.size());
            scenario.onActivity(a -> {
                assertTrue(containsText(a.getWindow().getDecorView(), "✓ Đã chọn 3"));
                assertNoEllipsis(a.getWindow().getDecorView(), "slot-name:");
                assertTrue("the slot shows the full file name", containsText(a.getWindow().getDecorView(), rawName(2)));
            });

            // Glossary slot: 1, 2 (old and new) and 3; Pronoun slot: 1 and 3.
            scenario.onActivity(a -> new ChapterSetPageFactory(a).pickProfiles(true));
            idle();
            Dialog gs = FilePickerSheet.last.dialog();
            for (String n : new String[]{glossaryName(1, ""), glossaryName(2, "_old"), glossaryName(2, "_new"), glossaryName(3, "")}) tapRow(scenario, gs, n);
            media("33-picker-glossary-chosen", scenario);
            clickByDescription(scenario, gs, "picker-confirm");
            idle();
            scenario.onActivity(a -> new ChapterSetPageFactory(a).pickProfiles(false));
            idle();
            Dialog ps = FilePickerSheet.last.dialog();
            tapRow(scenario, ps, pronounName(1));
            tapRow(scenario, ps, pronounName(3));
            clickByDescription(scenario, ps, "picker-confirm");
            idle();
            ComboDraft d = draftOf(scenario);
            assertEquals(3, d.rows.size());
            assertEquals(ChapterSetMatcher.Status.GREEN, d.rows.get(0).status);
            assertEquals("two Glossary files share chapter 2", ChapterSetMatcher.Status.RED, d.rows.get(1).status);
            assertEquals("v423c-p3", d.rows.get(2).pronounId);
            scenario.onActivity(a -> {
                assertTrue(containsText(a.getWindow().getDecorView(), "✕"));
                assertTrue(containsText(a.getWindow().getDecorView(), glossaryName(1, "")));
                assertTrue(containsText(a.getWindow().getDecorView(), pronounName(1)));
            });
            media("34-combo-rows-ok-warn-error", scenario);

            // Saving is blocked while a row is ✕.
            scenario.onActivity(a -> new ChapterSetPageFactory(a).save());
            idle();
            assertTrue(ChapterSetStore.listSets(c).isEmpty() || !hasSet(c, d.name));
            assertNotNull(draftOf(scenario));

            // Change chapter 2's Glossary through the single-choice sheet.
            ChapterSetMatcher.Row second = d.rows.get(1);
            scenario.onActivity(a -> new ChapterSetPageFactory(a).chooseForRow(draftOf(scenario), second, true));
            idle();
            Dialog one = FilePickerSheet.last.dialog();
            tapRow(scenario, one, glossaryName(2, "_new"));
            tapRow(scenario, one, glossaryName(2, "_old"));
            assertEquals("single choice keeps one row", 1, countSelectedRows(one));
            clickByDescription(scenario, one, "picker-confirm");
            idle();
            assertEquals("v423c-g2a", draftOf(scenario).rows.get(1).glossaryId);
            scenario.onActivity(a -> {
                draftOf(scenario).name = TAG + "combo";
                new ChapterSetPageFactory(a).save();
                assertNull("saving closes the screen", a.chapterSetDraft);
            });
            idle();
            ChapterSetStore.SetInfo mine = null;
            for (ChapterSetStore.SetInfo i : ChapterSetStore.listSets(c)) if ((TAG + "combo").equals(i.name)) mine = i;
            assertNotNull(mine);
            seed.setIds.add(mine.id);
            ChapterSetStore.SetData saved = ChapterSetStore.get(c, mine.id);
            assertEquals(3, saved.rows.size());
            assertEquals("", saved.rows.get(1).pronounId);
            media("35-combo-list", scenario);

            // The Translate screen shows all three names per chapter, unclipped, also at font scale 1.3.
            final String setId = mine.id;
            scenario.onActivity(a -> { a.switchTab("Translate"); a.sourceUi.selectSet(setId); });
            idle();
            scenario.onActivity(a -> {
                assertTrue(containsText(a.getWindow().getDecorView(), "Glossary   " + glossaryName(2, "_old")));
                assertTrue(containsText(a.getWindow().getDecorView(), "Pronoun   " + pronounName(1)));
                assertNoEllipsis(a.getWindow().getDecorView(), "slot-name:");
            });
            media("36-translate-source-combo", scenario);
        } finally {
            cleanup(c, seed, original);
        }
    }

    @Test
    public void noNameIsClippedAtFontScale130() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        Seed seed = seed(c);
        shell("settings put system font_scale 1.3");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> { a.chapterSetDraft = ChapterSetPageFactory.newDraft(a, seed.rawIds); a.invalidatePage("ChapterSets"); a.switchTab("ChapterSets"); });
            idle();
            scenario.onActivity(a -> { assertNoEllipsis(a.getWindow().getDecorView(), "slot-name:"); assertNoEllipsis(a.getWindow().getDecorView(), "glossary:"); });
            scenario.onActivity(a -> new ChapterSetPageFactory(a).pickRaw());
            idle();
            Dialog sheet = FilePickerSheet.last.dialog();
            assertNoEllipsis(sheet.getWindow().getDecorView(), "name:");
            media("37-picker-font-130", scenario);
            scenario.onActivity(a -> sheet.cancel());
            // The RAW tab shortcut fills the RAW slot with the ticked files.
            assertEquals(3, draftOf(scenario).rawIds.size());
        } finally {
            shell("settings put system font_scale 1.0");
            cleanup(c, seed, original);
        }
    }

    // ---- helpers ----

    private static ComboDraft draftOf(ActivityScenario<MainActivity> s) {
        AtomicReference<ComboDraft> r = new AtomicReference<>();
        s.onActivity(a -> r.set(a.chapterSetDraft));
        return r.get();
    }

    private static boolean hasSet(Context c, String name) {
        for (ChapterSetStore.SetInfo s : ChapterSetStore.listSets(c)) if (name.equals(s.name)) return true;
        return false;
    }

    private static void idle() { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); }

    private static void shell(String command) throws Exception {
        android.os.ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
        try (java.io.InputStream in = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) { while (in.read() >= 0) { } }
        Thread.sleep(500L);
    }

    private static void tapRow(ActivityScenario<MainActivity> scenario, Dialog d, String name) {
        View row = findByDescription(d.getWindow().getDecorView(), "chưa chọn: " + name);
        if (row == null) row = findByDescription(d.getWindow().getDecorView(), "đã chọn: " + name);
        assertNotNull("row for " + name, row);
        final View target = row;
        InstrumentationRegistry.getInstrumentation().runOnMainSync(target::performClick);
        idle();
    }

    private static void clickByDescription(ActivityScenario<MainActivity> scenario, Dialog d, String desc) {
        final View v = findByDescription(d.getWindow().getDecorView(), desc);
        assertNotNull(desc, v);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(v::performClick);
    }

    private static String textOf(Dialog d, String desc) {
        View v = findByDescription(d.getWindow().getDecorView(), desc);
        return v instanceof TextView ? ((TextView) v).getText().toString() : "";
    }

    private static int countSelectedRows(Dialog d) { return countDesc(d.getWindow().getDecorView(), "đã chọn: "); }

    private static int countDesc(View v, String prefix) {
        int n = v.getContentDescription() != null && v.getContentDescription().toString().startsWith(prefix) ? 1 : 0;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) n += countDesc(((ViewGroup) v).getChildAt(i), prefix);
        return n;
    }

    private static View findByDescription(View v, String desc) {
        if (v.getContentDescription() != null && desc.equals(v.getContentDescription().toString())) return v;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { View f = findByDescription(((ViewGroup) v).getChildAt(i), desc); if (f != null) return f; }
        return null;
    }

    /** Every TextView whose description starts with the prefix is fully laid out: no ellipsis and its height covers its text. */
    private static void assertNoEllipsis(View root, String prefix) {
        int[] seen = {0};
        check(root, prefix, seen);
        assertTrue("some '" + prefix + "' views exist", seen[0] > 0);
    }

    private static void check(View v, String prefix, int[] seen) {
        if (v instanceof TextView && v.getContentDescription() != null && v.getContentDescription().toString().startsWith(prefix)) {
            TextView t = (TextView) v;
            assertNotNull(t.getLayout());
            seen[0]++;
            for (int i = 0; i < t.getLayout().getLineCount(); i++) assertEquals("ellipsis in: " + t.getText(), 0, t.getLayout().getEllipsisCount(i));
            assertTrue("height covers the text of: " + t.getText(), t.getHeight() >= t.getLayout().getHeight());
        }
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) check(((ViewGroup) v).getChildAt(i), prefix, seen);
    }

    private static void media(String stage, ActivityScenario<MainActivity> scenario) throws InterruptedException {
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
        c.getSharedPreferences("translate_source", Context.MODE_PRIVATE).edit().clear().commit();
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
        Field json = StoreCacheFields.find(type, jsonField);
        json.setAccessible(true);
        json.set(null, null);
        if (itemsField == null) return;
        Field items = StoreCacheFields.find(type, itemsField);
        items.setAccessible(true);
        items.set(null, null);
    }
}
