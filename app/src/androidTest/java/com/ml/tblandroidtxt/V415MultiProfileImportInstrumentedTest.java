package com.ml.tblandroidtxt;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class V415MultiProfileImportInstrumentedTest {
    @Test public void libraryPickerAllowsMultipleAndCollectsEveryUniqueFile() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Intent listImport = activity.configImportIntent(true);
                Intent replacement = activity.configImportIntent(false);
                assertEquals(Intent.ACTION_OPEN_DOCUMENT, listImport.getAction());
                assertTrue(listImport.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false));
                assertFalse(replacement.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, true));

                Uri first = Uri.parse("content://v415-import/characters.csv");
                Uri second = Uri.parse("content://v415-import/places.csv");
                ClipData selected = new ClipData(
                        "profiles", new String[]{"text/plain"}, new ClipData.Item(first));
                selected.addItem(new ClipData.Item(second));
                selected.addItem(new ClipData.Item(first));
                Intent result = new Intent();
                result.setClipData(selected);
                result.setData(second);

                ArrayList<Uri> uris = activity.selectedDocumentUris(result);
                assertEquals(2, uris.size());
                assertEquals(first, uris.get(0));
                assertEquals(second, uris.get(1));
            });
        }
    }

    @Test public void importingTwoFilesPersistsTwoIndependentProfilesPerLibrary() throws Exception {
        Context target = androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().getTargetContext();
        SharedPreferences glossaryPreferences =
                target.getSharedPreferences("glossary_store", Context.MODE_PRIVATE);
        SharedPreferences pronounPreferences =
                target.getSharedPreferences("pronoun_store", Context.MODE_PRIVATE);
        SharedPreferences settingsPreferences =
                target.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Map<String, ?> originalGlossaries = new HashMap<>(glossaryPreferences.getAll());
        Map<String, ?> originalPronouns = new HashMap<>(pronounPreferences.getAll());
        Map<String, ?> originalSettings = new HashMap<>(settingsPreferences.getAll());
        File runtimeLog = new File(target.getFilesDir(), "tbl_runtime.log");
        boolean runtimeLogExisted = runtimeLog.isFile();
        String originalRuntimeLog = LogStore.read(target);
        ArrayList<File> fixtures = new ArrayList<>();
        try {
            glossaryPreferences.edit().clear().commit();
            pronounPreferences.edit().clear().putBoolean("legacy_migrated", true).commit();
            clearStoreCache(GlossaryStore.class, "cachedJson", "cachedGlossaries");
            clearStoreCache(PronounStore.class, "cachedJson", "cachedProfiles");

            File glossaryOne = fixture(target, fixtures, "characters-", ".csv",
                    "source,target\nAlice,Alicia");
            File glossaryTwo = fixture(target, fixtures, "places-", ".csv",
                    "source,target\nLondon,Luân Đôn");
            File pronounOne = fixture(target, fixtures, "family-", ".csv",
                    "from,to,pronoun\nAlice,Bob,chị/em");
            File pronounTwo = fixture(target, fixtures, "work-", ".csv",
                    "from,to,pronoun\nAlice,Bob,sếp/tôi");

            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(activity -> {
                    activity.importGlossaryProfiles(
                            selectionIntent(Uri.fromFile(glossaryOne), Uri.fromFile(glossaryTwo)), 0);
                    List<GlossaryStore.Glossary> glossaries = GlossaryStore.loadAll(activity);
                    assertEquals(2, glossaries.size());
                    assertEquals(1, glossaries.get(0).count());
                    assertEquals(1, glossaries.get(1).count());
                    assertNotEquals(glossaries.get(0).id, glossaries.get(1).id);
                    assertEquals(glossaries.get(0).id, GlossaryStore.getSelectedId(activity));

                    activity.importPronounProfiles(
                            selectionIntent(Uri.fromFile(pronounOne), Uri.fromFile(pronounTwo)), 0);
                    List<PronounStore.Profile> pronouns = PronounStore.loadAll(activity);
                    assertEquals(2, pronouns.size());
                    assertNotEquals(pronouns.get(0).id, pronouns.get(1).id);
                    assertNotEquals(pronouns.get(0).text, pronouns.get(1).text);
                    assertEquals(pronouns.get(0).id, PronounStore.getSelectedId(activity));
                });
            }
        } finally {
            restore(glossaryPreferences, originalGlossaries);
            restore(pronounPreferences, originalPronouns);
            restore(settingsPreferences, originalSettings);
            clearStoreCache(GlossaryStore.class, "cachedJson", "cachedGlossaries");
            clearStoreCache(PronounStore.class, "cachedJson", "cachedProfiles");
            restoreRuntimeLog(runtimeLog, runtimeLogExisted, originalRuntimeLog);
            for (File fixture : fixtures) if (fixture.isFile()) assertTrue(fixture.delete());
            assertEquals(originalGlossaries, glossaryPreferences.getAll());
            assertEquals(originalPronouns, pronounPreferences.getAll());
            assertEquals(originalSettings, settingsPreferences.getAll());
            assertEquals(originalRuntimeLog, LogStore.read(target));
        }
    }

    private static Intent selectionIntent(Uri first, Uri second) {
        ClipData clip = new ClipData(
                "profiles", new String[]{"text/plain"}, new ClipData.Item(first));
        clip.addItem(new ClipData.Item(second));
        Intent result = new Intent();
        result.setClipData(clip);
        return result;
    }

    private static File fixture(
            Context context, List<File> fixtures, String prefix, String suffix, String text)
            throws Exception {
        File file = File.createTempFile(prefix, suffix, context.getCacheDir());
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }
        fixtures.add(file);
        return file;
    }

    private static void clearStoreCache(Class<?> type, String jsonField, String itemsField)
            throws Exception {
        Field json = type.getDeclaredField(jsonField);
        Field items = type.getDeclaredField(itemsField);
        json.setAccessible(true);
        items.setAccessible(true);
        json.set(null, null);
        items.set(null, null);
    }

    private static void restore(SharedPreferences preferences, Map<String, ?> values) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) editor.putString(entry.getKey(), (String) value);
            else if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
            else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
            else if (value instanceof Float) editor.putFloat(entry.getKey(), (Float) value);
        }
        editor.commit();
    }

    private static void restoreRuntimeLog(File file, boolean existed, String text) throws Exception {
        if (!existed) {
            if (file.isFile()) assertTrue(file.delete());
            return;
        }
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
