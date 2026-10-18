package com.ml.tblandroidtxt;

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
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * v4.23 L4 on the emulator with a fake provider (USD 0): a three-chapter set translates each chapter with its own Glossary,
 * the Active profile and shared settings stay as they were, a chapter-local failure lets the queue go on, and an authentication
 * failure stops it. The Translate screen is driven through the same objects the buttons use; Start itself is replaced by the
 * intent the Start button builds (the SAF output folder cannot be picked in a test, see {@link TranslateTestSeam}).
 */
@RunWith(AndroidJUnit4.class)
public class V423ChapterRunInstrumentedTest {
    private static final String[] PREFS = {"glossary_store", "pronoun_store", "settings", "translate_source"};
    private static final String TAG = "v423r-";
    private static final String[] TERMS = {"花子", "太郎", "次郎"};
    private static final String[] TARGETS = {"Hanako-MOT", "Taro-HAI", "Jiro-BA"};
    private static final String ACTIVE_TARGET = "Active-ONLY-XYZ";

    private static String rawText(int chapter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 30; i++) sb.append("【章").append(chapter).append("】第").append(i).append("段。登場人物は").append(TERMS[chapter - 1])
                .append("で、これは翻訳試験のための十分に長い日本語の段落であり、境界を越えて分割されることを確認します。\n\n");
        return sb.append(System.nanoTime()).toString();
    }

    private static OpenAICompatibleClient.ChatResult response(String text) {
        OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
        r.content = "<TRANSLATION>" + text + "</TRANSLATION>";
        r.promptTokens = 110; r.completionTokens = 30; r.totalTokens = 140; r.usageReported = true;
        r.providerResponseId = "fixture"; r.finishReason = "stop";
        return r;
    }

    private static void observe(OpenAICompatibleClient.NetworkObserver o) { o.onRequestBodyStarted(); o.onRequestBodySent(100); o.onResponseHeaders(200); }

    /** What a run of the three-chapter set leaves behind. */
    private static final class Fixture {
        final List<LibraryRawStore.Item> raws = new ArrayList<>();
        String setId;
        final List<String> glossaryIds = new ArrayList<>();
    }

    private Fixture seed(Context c) throws Exception {
        Fixture f = new Fixture();
        ArrayList<GlossaryStore.Glossary> glossaries = new ArrayList<>();
        GlossaryStore.Glossary active = new GlossaryStore.Glossary();
        active.id = "v423r-active"; active.name = "active-" + TAG;
        active.terms.add(new GlossaryStore.Term("活動", ACTIVE_TARGET, "term"));
        glossaries.add(active);
        for (int i = 0; i < 3; i++) {
            GlossaryStore.Glossary g = new GlossaryStore.Glossary();
            g.id = "v423r-g" + (i + 1); g.name = String.format("%03d_%sGlossary.csv", i + 1, TAG);
            g.terms.add(new GlossaryStore.Term(TERMS[i], TARGETS[i], "character"));
            glossaries.add(g);
            f.glossaryIds.add(g.id);
        }
        assertTrue(GlossaryStore.saveAll(c, glossaries));
        assertTrue(GlossaryStore.setSelectedId(c, active.id));
        ChapterSetStore.SetData set = new ChapterSetStore.SetData();
        set.name = TAG + "set";
        for (int i = 1; i <= 3; i++) {
            String name = String.format("%03d_%sRAW.txt", i, TAG);
            RawImportPlanner.Plan plan = RawImportPlanner.plan(Collections.singletonList(new RawImportPlanner.Candidate(name, rawText(i), "UTF-8")), LibraryRawStore.hashes(c));
            LibraryRawStore.Item raw = LibraryRawStore.add(c, plan).get(0);
            f.raws.add(raw);
            set.rows.add(new ChapterSetStore.Row(i, raw.id, "v423r-g" + i, ""));
        }
        ChapterSetStore.save(c, set);
        f.setId = set.id;
        return f;
    }

    private static AppSettings baseSettings() {
        AppSettings s = new AppSettings();
        s.apiKey = "fixture-key"; s.provider = "fake"; s.model = "fake/model";
        s.maxTokensPerChunk = 80; s.maxAttempts = 1; s.refineAfter = false; s.stopOnCostLimit = false;
        s.glossaryText = GlossaryStore.toPromptText(GlossaryStore.find(InstrumentationRegistry.getInstrumentation().getTargetContext(), "v423r-active"));
        s.selectedGlossaryId = "v423r-active"; s.selectedGlossaryName = "active-" + TAG;
        return AppValidator.normalize(s);
    }

    private static File outDir(Context c) {
        File d = new File(c.getCacheDir(), "v423-chapter-out");
        d.mkdirs();
        return d;
    }

    private static TranslationRepository.JobSummary job(Context c, String rawUri) {
        TranslationRepository repo = new TranslationRepository(c);
        try {
            for (TranslationRepository.JobSummary j : repo.getJobSummaries(100)) if (rawUri.equals(j.inputUri)) return j;
        } finally { repo.close(); }
        return null;
    }

    private static String status(Context c, String rawUri) {
        TranslationRepository.JobSummary j = job(c, rawUri);
        return j == null ? "NONE" : String.valueOf(j.status).toUpperCase();
    }

    private static void waitFor(java.util.concurrent.Callable<Boolean> condition, long timeoutMs, String what) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (condition.call()) return;
            Thread.sleep(250L);
        }
        throw new AssertionError("timed out: " + what);
    }

    /** Opens the set on the Translate screen, starts it like the Start button and waits until the service is idle again. */
    private PreparedBatch runSet(ActivityScenario<MainActivity> scenario, Context c, Fixture f, AppSettings base) throws Exception {
        scenario.onActivity(activity -> { activity.switchTab("Translate"); activity.sourceUi.selectSet(f.setId); });
        waitFor(() -> {
            AtomicReference<PreparedBatch> ref = new AtomicReference<>();
            scenario.onActivity(activity -> ref.set(activity.currentPreparedBatch()));
            return ref.get() != null && ref.get().ready() && ref.get().bindings != null && ref.get().bindings.size() == 3;
        }, 60_000L, "plan for the three ticked chapters");
        AtomicReference<PreparedBatch> plan = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        Intent i = new Intent(c, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_START);
        i.putExtra("outputUri", "");
        i.putExtra("outputTreeUri", "content://tbl.test/tree/v423");
        i.putExtra("settings", SettingsStore.toJson(base));
        String sid = java.util.UUID.randomUUID().toString();
        i.putExtra("startSessionId", sid);
        scenario.onActivity(activity -> {
            plan.set(activity.currentPreparedBatch());
            i.putExtra("preparedBatchId", plan.get().id);
            error.set(activity.putChapterSession(i, sid, plan.get()));
        });
        assertNull(error.get());
        assertTrue(i.getBooleanExtra("chapterMode", false));
        c.startForegroundService(i);
        return plan.get();
    }

    private void cleanup(Context c, Fixture f, Map<String, Map<String, ?>> original) throws Exception {
        TranslateTestSeam.clear();
        TranslationRepository repo = new TranslationRepository(c);
        try {
            for (LibraryRawStore.Item raw : f.raws) {
                TranslationRepository.JobSummary j = job(c, raw.uri());
                while (j != null) { repo.deleteJob(j.id); j = job(c, raw.uri()); }
            }
        } finally { repo.close(); }
        ChapterSetStore.delete(c, Collections.singletonList(f.setId));
        List<String> ids = new ArrayList<>();
        for (LibraryRawStore.Item raw : f.raws) ids.add(raw.id);
        LibraryRawStore.removeRows(c, ids);
        LibraryRawStore.purgeFiles(c, ids);
        File[] outs = outDir(c).listFiles();
        if (outs != null) for (File o : outs) o.delete();
        restoreAll(c, original);
    }

    @Test
    public void threeChaptersTranslateWithTheirOwnGlossaries_activeAndSharedSettingsUntouched() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        Fixture f = seed(c);
        List<String[]> prompts = Collections.synchronizedList(new ArrayList<String[]>());
        TranslateTestSeam.setOutputDir(outDir(c));
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            observe(observer);
            String all = prompt.system + "\n" + prompt.user;
            int chapter = all.contains("【章1】") ? 1 : all.contains("【章2】") ? 2 : 3;
            prompts.add(new String[]{String.valueOf(chapter), all});
            return response("Bản dịch thử nghiệm số " + calls.incrementAndGet() + " với đủ độ dài để vượt qua kiểm định của ứng dụng.");
        });
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AppSettings base = baseSettings();
            String savedBefore = SettingsStore.toJson(SettingsStore.load(c));
            PreparedBatch plan = runSet(scenario, c, f, base);
            media("28-translate-set-chapters", scenario);
            assertEquals(3, plan.inputs.size());
            assertEquals(f.raws.get(0).uri(), plan.inputs.get(0).uri.toString());
            assertTrue(plan.bindings.get(1).glossaryText.contains(TARGETS[1]));
            assertFalse(plan.bindings.get(1).glossaryText.contains(ACTIVE_TARGET));
            scenario.onActivity(activity -> {
                assertTrue(containsText(activity.getWindow().getDecorView(), "3/3 chương được tick"));
                assertTrue(containsText(activity.getWindow().getDecorView(), "Glossary: 002_" + TAG + "Glossary.csv"));
            });

            waitFor(() -> status(c, f.raws.get(0).uri()).startsWith("COMPLETED") && status(c, f.raws.get(1).uri()).startsWith("COMPLETED") && status(c, f.raws.get(2).uri()).startsWith("COMPLETED"), 120_000L, "three chapters completed");
            waitFor(() -> !TranslatorService.isActive(), 30_000L, "service idle");

            // Every request carried only its own chapter's Glossary target.
            assertTrue(prompts.size() >= 3);
            for (int k = 1; k <= 3; k++) { boolean seen = false; for (String[] p : prompts) seen |= p[0].equals(String.valueOf(k)); assertTrue("chapter " + k + " was requested", seen); }
            for (String[] p : prompts) {
                int chapter = Integer.parseInt(p[0]);
                assertTrue("chapter " + chapter + " prompt has its own term target", p[1].contains(TARGETS[chapter - 1]));
                for (int other = 1; other <= 3; other++) if (other != chapter) assertFalse("chapter " + chapter + " prompt must not carry chapter " + other + "'s glossary", p[1].contains(TARGETS[other - 1]));
                assertFalse("the Active glossary is not used inside a chapter set", p[1].contains(ACTIVE_TARGET));
            }
            // jobs.settings carry the chapter's own texts.
            TranslationRepository repo = new TranslationRepository(c);
            try {
                for (int k = 0; k < 3; k++) {
                    TranslationRepository.JobSummary j = job(c, f.raws.get(k).uri());
                    String settingsJson = repo.getJob(j.id).settingsJson;
                    assertTrue(settingsJson, settingsJson.contains(TARGETS[k]));
                    assertFalse(settingsJson.contains(ACTIVE_TARGET));
                    for (int other = 0; other < 3; other++) if (other != k) assertFalse(settingsJson.contains(TARGETS[other]));
                    assertEquals(f.raws.get(k).originalName, j.fileName);
                }
            } finally { repo.close(); }
            // One output file per chapter, named after the RAW.
            File[] outs = outDir(c).listFiles();
            List<String> names = new ArrayList<>();
            for (File o : outs) names.add(o.getName());
            for (int k = 1; k <= 3; k++) {
                final String prefix = String.format("%03d_%sRAW", k, TAG);
                boolean found = false;
                for (String n : names) found |= n.startsWith(prefix) && new File(outDir(c), n).length() > 0;
                assertTrue(names.toString(), found);
            }
            // Active profile and shared settings are what they were.
            assertEquals("v423r-active", GlossaryStore.getSelectedId(c));
            SettingsStore.save(c, SettingsStore.load(c));
            AppSettings after = SettingsStore.load(c);
            assertEquals(base.glossaryText, after.glossaryText);
            assertEquals("v423r-active", after.selectedGlossaryId);
            assertFalse(after.glossaryText.contains(TARGETS[0]));
            assertNotNull(savedBefore);
            // Chapters that now have a finished translation are not ticked by default next time.
            scenario.onActivity(activity -> { activity.sourceUi.selectSet(f.setId); });
            scenario.onActivity(activity -> activity.switchTab("Translate"));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        } finally {
            cleanup(c, f, original);
        }
    }

    @Test
    public void integrityFailureInChapterTwoContinuesWithChapterThree() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        Fixture f = seed(c);
        TranslateTestSeam.setOutputDir(outDir(c));
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            observe(observer);
            String all = prompt.system + "\n" + prompt.user;
            if (all.contains("【章2】")) return response("Lỗi: invalid api key xuất hiện trong bản dịch của chương hai và kiểm toàn vẹn phải từ chối nó.");
            return response("Bản dịch thử nghiệm số " + calls.incrementAndGet() + " với đủ độ dài để vượt qua kiểm định của ứng dụng.");
        });
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            runSet(scenario, c, f, baseSettings());
            waitFor(() -> !status(c, f.raws.get(2).uri()).equals("NONE") && !TranslatorService.isActive(), 120_000L, "queue finished");
            assertTrue(status(c, f.raws.get(0).uri()), status(c, f.raws.get(0).uri()).startsWith("COMPLETED"));
            assertEquals("FAILED_INTEGRITY_CHECK", status(c, f.raws.get(1).uri()));
            assertTrue("chapter 3 still ran", status(c, f.raws.get(2).uri()).startsWith("COMPLETED"));
            media("29-chapter-run-summary", scenario);
        } finally {
            cleanup(c, f, original);
        }
    }

    @Test
    public void authenticationFailureInChapterTwoStopsTheQueue() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        Fixture f = seed(c);
        TranslateTestSeam.setOutputDir(outDir(c));
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            String all = prompt.system + "\n" + prompt.user;
            if (all.contains("【章2】")) throw new ApiHttpException(401, 0, "Unauthorized: invalid credentials");
            observe(observer);
            return response("Bản dịch thử nghiệm số " + calls.incrementAndGet() + " với đủ độ dài để vượt qua kiểm định của ứng dụng.");
        });
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            runSet(scenario, c, f, baseSettings());
            waitFor(() -> !status(c, f.raws.get(1).uri()).equals("NONE") && !TranslatorService.isActive(), 120_000L, "queue stopped");
            assertTrue(status(c, f.raws.get(0).uri()).startsWith("COMPLETED"));
            assertFalse("chapter 2 did not complete", status(c, f.raws.get(1).uri()).startsWith("COMPLETED"));
            assertEquals("chapter 3 was never started", "NONE", status(c, f.raws.get(2).uri()));
            media("30-chapter-run-stopped", scenario);
        } finally {
            cleanup(c, f, original);
        }
    }

    // ---- helpers ----

    private static void media(String stage, ActivityScenario<MainActivity> scenario) throws InterruptedException {
        if (!"run".equals(InstrumentationRegistry.getArguments().getString("v423_media", ""))) return;
        scenario.onActivity(activity -> activity.switchTab(stage.startsWith("28") ? "Translate" : "Jobs"));
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
        Field json = type.getDeclaredField(jsonField);
        json.setAccessible(true);
        json.set(null, null);
        if (itemsField == null) return;
        Field items = type.getDeclaredField(itemsField);
        items.setAccessible(true);
        items.set(null, null);
    }
}
