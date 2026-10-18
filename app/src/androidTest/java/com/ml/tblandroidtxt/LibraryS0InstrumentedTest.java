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
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * v4.22 S0.7 on the emulator, synthetic data only: bulk import keeps the Active profiles, the library ZIP round trip
 * restores a deleted glossary with its id, an unreadable store is kept and locked, and imported profiles survive a
 * process kill (seed/verify phases with {@code -e s0_restart seed|verify}, force-stop in between). The system file
 * picker is not exercised: the test writes and reads file URIs directly.
 */
@RunWith(AndroidJUnit4.class)
public class LibraryS0InstrumentedTest {
    private static final String[] PREFS = {"glossary_store", "pronoun_store", "settings", MainActivity.LIBRARY_BACKUP_PREF};
    private static final String RESTART_MARKER = "s0-restart-ids.txt";

    @Test
    public void importKeepsActive_libraryRoundTrip_corruptStoreLocked() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(target);
        File runtimeLog = new File(target.getFilesDir(), "tbl_runtime.log");
        boolean runtimeLogExisted = runtimeLog.isFile();
        String originalRuntimeLog = LogStore.read(target);
        ArrayList<File> fixtures = new ArrayList<>();
        try {
            clearAll(target);
            GlossaryStore.Glossary active = new GlossaryStore.Glossary();
            active.id = "s0-active-glossary"; active.name = "s0-active";
            active.terms.add(new GlossaryStore.Term("花子", "Hanako", "character"));
            assertTrue(GlossaryStore.saveAll(target, new ArrayList<>(java.util.Collections.singletonList(active))));
            assertTrue(GlossaryStore.setSelectedId(target, active.id));

            Uri g1 = Uri.fromFile(fixture(target, fixtures, "s0-g1-", ".csv", "source,target\n太郎,Taro"));
            Uri g2 = Uri.fromFile(fixture(target, fixtures, "s0-g2-", ".csv", "source,target\n森,rừng"));
            Uri g3 = Uri.fromFile(fixture(target, fixtures, "s0-g3-", ".csv", "source,target\n魔法,ma pháp"));
            Uri p1 = Uri.fromFile(fixture(target, fixtures, "s0-p1-", ".csv", "from,to,pronoun\nHanako,Taro,chị/em"));
            Uri p2 = Uri.fromFile(fixture(target, fixtures, "s0-p2-", ".csv", "from,to,pronoun\nTaro,Hanako,anh/em"));
            File zip = new File(target.getCacheDir(), "s0-library-" + System.nanoTime() + ".zip");
            fixtures.add(zip);

            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(activity -> {
                    activity.importGlossaryProfiles(selection(g1, g2, g3), 0);
                    activity.importPronounProfiles(selection(p1, p2), 0);
                    assertEquals(4, GlossaryStore.loadAll(activity).size());
                    assertEquals(2, PronounStore.loadAll(activity).size());
                    assertEquals("Active glossary unchanged", active.id, GlossaryStore.getSelectedId(activity));
                    assertEquals("no Active pronoun chosen by import", "", PronounStore.getSelectedId(activity));

                    activity.writeLibraryExport(Uri.fromFile(zip));
                    assertTrue(zip.length() > 0);
                    assertTrue(activity.lastLibraryExportLine().contains("4 glossary, 2 pronoun"));
                    activity.switchTab("Files");
                });
                media("14-library-backup-card");

                GlossaryStore.Glossary removed = GlossaryStore.loadAll(target).get(0);
                assertTrue(GlossaryStore.delete(target, removed.id));
                LibraryArchive.Contents contents;
                try (FileInputStream in = new FileInputStream(zip)) { contents = LibraryArchive.read(in); }
                LibraryArchive.MergePlan plan = LibraryArchive.plan(contents, GlossaryStore.loadAll(target), PronounStore.loadAll(target), "test");
                assertEquals(1, plan.addGlossaries.size());
                assertEquals(removed.id, plan.addGlossaries.get(0).id);
                assertEquals(5, plan.existing);
                scenario.onActivity(activity -> activity.previewLibraryImport(Uri.fromFile(zip)));
                media("15-library-import-preview");
                dismissDialogs();
                scenario.onActivity(activity -> activity.applyLibraryImport(plan));
                assertNotNull(GlossaryStore.find(target, removed.id));
                assertEquals(4, GlossaryStore.loadAll(target).size());
                assertEquals(active.id, GlossaryStore.getSelectedId(target));

                SharedPreferences glossaryPrefs = target.getSharedPreferences("glossary_store", Context.MODE_PRIVATE);
                glossaryPrefs.edit().putString("glossaries_json", "[{\"id\":").commit();
                clearStoreCache(GlossaryStore.class, "cachedJson", "cachedGlossaries");
                assertTrue(GlossaryStore.isCorrupt(target));
                assertFalse(GlossaryStore.saveAll(target, new ArrayList<GlossaryStore.Glossary>()));
                assertEquals("[{\"id\":", glossaryPrefs.getString("glossaries_json", ""));
                boolean copied = false;
                for (Map.Entry<String, ?> e : glossaryPrefs.getAll().entrySet()) copied |= e.getKey().startsWith("glossaries_json_corrupt_") && "[{\"id\":".equals(e.getValue());
                assertTrue("unreadable text preserved", copied);
                scenario.onActivity(activity -> {
                    activity.switchTab("Glossaries");
                    assertTrue("locked card shown", containsText(activity.getWindow().getDecorView(), "Dữ liệu Glossary đọc lỗi"));
                });
                media("16-library-locked-card");
            }
        } finally {
            restoreAll(target, original);
            restoreRuntimeLog(runtimeLog, runtimeLogExisted, originalRuntimeLog);
            for (File f : fixtures) if (f.isFile()) assertTrue(f.delete());
            assertEquals(original, snapshot(target));
        }
    }

    /** Phase 1 of the restart check: import, record ids, then the runner force-stops the app. Original prefs saved to a marker. */
    @Test
    public void restartSeed() throws Exception {
        Assume.assumeTrue("seed".equals(InstrumentationRegistry.getArguments().getString("s0_restart", "")));
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ArrayList<File> fixtures = new ArrayList<>();
        Uri g1 = Uri.fromFile(fixture(target, fixtures, "s0-restart-", ".csv", "source,target\n花子,Hanako"));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.importGlossaryProfiles(selection(g1), 0));
        }
        String id = GlossaryStore.loadAll(target).get(0).id;
        try (FileOutputStream out = new FileOutputStream(new File(target.getFilesDir(), RESTART_MARKER))) {
            out.write(id.getBytes(StandardCharsets.UTF_8));
        }
        for (File f : fixtures) f.delete();
    }

    /** Phase 2 after force-stop: the imported glossary is still in the store; it is then removed again. */
    @Test
    public void restartVerify() throws Exception {
        Assume.assumeTrue("verify".equals(InstrumentationRegistry.getArguments().getString("s0_restart", "")));
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File marker = new File(target.getFilesDir(), RESTART_MARKER);
        assertTrue(marker.isFile());
        String id;
        try (FileInputStream in = new FileInputStream(marker)) { id = new String(readAll(in), StandardCharsets.UTF_8); }
        assertNotNull("imported glossary survived the kill", GlossaryStore.find(target, id));
        assertTrue(GlossaryStore.delete(target, id));
        assertTrue(marker.delete());
    }

    /** With {@code -e v422_media run} the screen is held so the shell can take a picture. */
    private static void media(String stage) throws InterruptedException {
        if (!"run".equals(InstrumentationRegistry.getArguments().getString("v422_media", ""))) return;
        Thread.sleep(700L);
        android.util.Log.i("V422MEDIA", "stage=" + stage);
        Thread.sleep(5_000L);
    }

    /** Closes the import preview dialog the activity just showed. */
    private static void dismissDialogs() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static Intent selection(Uri... uris) {
        ClipData clip = new ClipData("profiles", new String[]{"text/plain"}, new ClipData.Item(uris[0]));
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
        c.getSharedPreferences(MainActivity.LIBRARY_BACKUP_PREF, Context.MODE_PRIVATE).edit().clear().commit();
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

    private static File fixture(Context context, List<File> fixtures, String prefix, String suffix, String text) throws Exception {
        File file = File.createTempFile(prefix, suffix, context.getCacheDir());
        try (FileOutputStream output = new FileOutputStream(file)) { output.write(text.getBytes(StandardCharsets.UTF_8)); }
        fixtures.add(file);
        return file;
    }

    private static byte[] readAll(java.io.InputStream in) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[8192]; int n;
        while ((n = in.read(b)) > 0) out.write(b, 0, n);
        return out.toByteArray();
    }

    private static void restoreRuntimeLog(File file, boolean existed, String text) throws Exception {
        if (!existed) { if (file.isFile()) assertTrue(file.delete()); return; }
        try (FileOutputStream output = new FileOutputStream(file, false)) { output.write(text.getBytes(StandardCharsets.UTF_8)); }
    }
}
