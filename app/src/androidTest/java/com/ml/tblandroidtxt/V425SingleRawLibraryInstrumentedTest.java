package com.ml.tblandroidtxt;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Verifies the one-chapter library picker feeds the existing RAW preparation path without activating suggestions. */
@RunWith(AndroidJUnit4.class)
public class V425SingleRawLibraryInstrumentedTest {
    private static final String TAG = "QA_V425_SINGLE_RAW_";
    private static final String[] PREFS = {"glossary_store", "pronoun_store", "settings", "translate_source", "ui_session"};

    @Test public void librarySelectionBuildsEstimateAndOnlySuggestsMatchingProfiles() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        LibraryRawStore.Item raw = null;
        File outputDir = new File(c.getCacheDir(), "v425-single-raw");
        outputDir.mkdirs();
        try {
            seedProfiles(c);
            raw = importRaw(c);
            final LibraryRawStore.Item selectedRaw = raw;
            TranslateTestSeam.setOutputDir(outputDir);
            TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
                throw new AssertionError("estimate and selection must not call a provider");
            });

            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(a -> {
                    a.switchTab("Translate");
                    a.sourceUi.setMode(TranslateSourceFactory.MODE_FILES);
                    View button = findDescription(a.getWindow().getDecorView(), "input-library");
                    assertNotNull("library button exists in one-chapter mode", button);
                    a.inputUris.clear();
                    a.setLabel(a.inputFileLabel, TranslateInputLabel.empty(true));
                    assertEquals("Chưa chọn RAW", a.inputFileLabel.getText().toString());
                    button.performClick();

                    FilePickerSheet picker = FilePickerSheet.last;
                    assertNotNull(picker);
                    Dialog dialog = picker.dialog();
                    View rawRow = findDescription(dialog.getWindow().getDecorView(), "chưa chọn: " + selectedRaw.originalName);
                    assertNotNull("the RAW is listed in FilePickerSheet", rawRow);
                    rawRow.performClick();
                    View confirm = findDescription(dialog.getWindow().getDecorView(), "picker-confirm");
                    assertNotNull(confirm);
                    confirm.performClick();
                });

                AtomicReference<PreparedBatch> prepared = new AtomicReference<>();
                long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60);
                while (System.currentTimeMillis() < deadline) {
                    scenario.onActivity(a -> prepared.set(a.currentPreparedBatch()));
                    if (prepared.get() != null && prepared.get().ready()) break;
                    Thread.sleep(150L);
                }
                assertNotNull("estimate plan prepared", prepared.get());
                assertTrue("estimate plan ready", prepared.get().ready());
                assertEquals(1, prepared.get().inputs.size());
                assertEquals(selectedRaw.uri(), prepared.get().inputs.get(0).uri.toString());
                assertNull("one RAW uses the standard single-file path, without chapter bindings", prepared.get().bindings);
                assertEquals(selectedRaw.uri(), UiSessionStore.loadInputUris(c).get(0));
                assertNull(FileUtil.validateReadable(c, Uri.parse(selectedRaw.uri()), "QA input"));
                assertEquals("📄 " + selectedRaw.originalName + " · " + selectedRaw.chapterLabel(),
                        aInputLabel(scenario));
                assertEquals("025_QA_Glossary.csv", GlossaryStore.selected(c).name);
                assertEquals("025_QA_Pronoun.csv", PronounStore.selected(c).name);

                AtomicReference<String> suggestion = new AtomicReference<>();
                scenario.onActivity(a -> suggestion.set(a.singleRawSuggestion.getText().toString()));
                assertTrue(suggestion.get().contains("025_QA_Glossary.csv"));
                assertTrue(suggestion.get().contains("025_QA_Pronoun.csv"));
                assertTrue(suggestion.get().contains("Cấu hình đang chọn được giữ nguyên."));
                assertTrue(!suggestion.get().contains("126_QA_WRONG.csv"));
                assertTrue(!suggestion.get().contains("026_QA_WRONG.csv"));
            }
        } finally {
            TranslateTestSeam.clear();
            if (raw != null) {
                LibraryRawStore.removeRows(c, java.util.Collections.singletonList(raw.id));
                LibraryRawStore.purgeFiles(c, java.util.Collections.singletonList(raw.id));
            }
            for (File file : files(outputDir)) file.delete();
            restore(c, original);
            GlossaryStoreCore.resetForTest();
            PronounStoreCore.resetForTest();
            PreparationCoordinator.get(c).cancel();
        }
    }

    private static String aInputLabel(ActivityScenario<MainActivity> scenario) {
        AtomicReference<String> label = new AtomicReference<>();
        scenario.onActivity(a -> label.set(a.inputFileLabel.getText().toString()));
        return label.get();
    }

    private static LibraryRawStore.Item importRaw(Context c) throws Exception {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 8; i++) text.append("QA_本文 ").append(i).append("。これはローカルの推定確認用テスト文です。\n\n");
        RawImportPlanner.Plan plan = RawImportPlanner.plan(
                java.util.Collections.singletonList(new RawImportPlanner.Candidate("025_" + TAG + "RAW.txt", text.toString(), "UTF-8")),
                LibraryRawStore.hashes(c));
        List<LibraryRawStore.Item> added = LibraryRawStore.add(c, plan);
        assertEquals(1, added.size());
        return added.get(0);
    }

    private static void seedProfiles(Context c) {
        GlossaryStore.Glossary matchingGlossary = new GlossaryStore.Glossary();
        matchingGlossary.name = "025_QA_Glossary.csv";
        matchingGlossary.terms.add(new GlossaryStore.Term("QA_ source", "QA_ target", "term"));
        GlossaryStore.Glossary otherGlossary = new GlossaryStore.Glossary();
        otherGlossary.name = "126_QA_WRONG.csv";
        assertTrue(GlossaryStore.saveAll(c, Arrays.asList(matchingGlossary, otherGlossary)));
        assertTrue(GlossaryStore.setSelectedId(c, matchingGlossary.id));

        PronounStore.Profile matchingPronoun = new PronounStore.Profile();
        matchingPronoun.name = "025_QA_Pronoun.csv";
        matchingPronoun.text = "from,to,pronoun\nQA_A,QA_B,QA_C";
        PronounStore.Profile otherPronoun = new PronounStore.Profile();
        otherPronoun.name = "026_QA_WRONG.csv";
        otherPronoun.text = "from,to,pronoun\nQA_A,QA_B,QA_C";
        assertTrue(PronounStore.saveAll(c, Arrays.asList(matchingPronoun, otherPronoun)));
        assertTrue(PronounStore.setSelectedId(c, matchingPronoun.id));
    }

    private static View findDescription(View root, String description) {
        if (root == null) return null;
        if (description.equals(root.getContentDescription())) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findDescription(group.getChildAt(i), description);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Map<String, Map<String, ?>> snapshot(Context c) {
        HashMap<String, Map<String, ?>> out = new HashMap<>();
        for (String name : PREFS) out.put(name, new HashMap<>(c.getSharedPreferences(name, Context.MODE_PRIVATE).getAll()));
        return out;
    }

    private static void restore(Context c, Map<String, Map<String, ?>> snapshot) {
        for (Map.Entry<String, Map<String, ?>> entry : snapshot.entrySet()) {
            SharedPreferences preferences = c.getSharedPreferences(entry.getKey(), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit().clear();
            for (Map.Entry<String, ?> value : entry.getValue().entrySet()) {
                Object v = value.getValue();
                if (v instanceof String) editor.putString(value.getKey(), (String) v);
                else if (v instanceof Boolean) editor.putBoolean(value.getKey(), (Boolean) v);
                else if (v instanceof Integer) editor.putInt(value.getKey(), (Integer) v);
                else if (v instanceof Long) editor.putLong(value.getKey(), (Long) v);
                else if (v instanceof Float) editor.putFloat(value.getKey(), (Float) v);
                else if (v instanceof java.util.Set) {
                    @SuppressWarnings("unchecked") java.util.Set<String> values = (java.util.Set<String>) v;
                    editor.putStringSet(value.getKey(), values);
                }
            }
            editor.commit();
        }
    }

    private static File[] files(File directory) {
        File[] files = directory.listFiles();
        return files == null ? new File[0] : files;
    }
}
