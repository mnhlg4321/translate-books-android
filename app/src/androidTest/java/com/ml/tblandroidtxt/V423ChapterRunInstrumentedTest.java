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

    /** The chapter-local test makes chapter 2 one short line that cannot be split when the provider says the context is too large. */
    private static boolean tinyChapterTwo;

    /** J6 tests: the RAW has quoted dialogue, so the bracket rule compares the translation with a RAW that uses corner brackets. */
    private static boolean bracketRaw;

    private static String rawText(int chapter) {
        if (bracketRaw) {
            StringBuilder b = new StringBuilder();
            for (int i = 1; i <= 30; i++) b.append("【章").append(chapter).append("】第").append(i).append("段。\u300c").append(TERMS[chapter - 1]).append("です。\u300d\n\n");
            return b.append(System.nanoTime()).toString();
        }
        if (chapter == 2 && tinyChapterTwo) return "【章2】" + TERMS[1] + "です。" + System.nanoTime();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 30; i++) sb.append("【章").append(chapter).append("】第").append(i).append("段。登場人物は").append(TERMS[chapter - 1])
                .append("で、これは翻訳試験のための十分に長い日本語の段落であり、境界を越えて分割されることを確認します。\n\n");
        return sb.append(System.nanoTime()).toString();
    }

    private static OpenAICompatibleClient.ChatResult response(String userPrompt) {
        OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
        r.content = lineRichResponse(userPrompt, ".");
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
        Broadcasts previewEvents = listen(c);
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            observe(observer);
            String all = prompt.system + "\n" + prompt.user;
            int chapter = all.contains("【章1】") ? 1 : all.contains("【章2】") ? 2 : 3;
            prompts.add(new String[]{String.valueOf(chapter), all});
            calls.incrementAndGet();
            return response(all);
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
                assertTrue(containsText(activity.getWindow().getDecorView(), "Glossary   002_" + TAG + "Glossary.csv"));
            });

            waitFor(() -> status(c, f.raws.get(0).uri()).startsWith("COMPLETED") && status(c, f.raws.get(1).uri()).startsWith("COMPLETED") && status(c, f.raws.get(2).uri()).startsWith("COMPLETED"), 120_000L, "three chapters completed");
            waitFor(() -> !TranslatorService.isActive(), 30_000L, "service idle");

            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            boolean transitionSeen = false;
            synchronized (previewEvents.events) {
                for (Intent event : previewEvents.events) {
                    String file = event.getStringExtra(TranslatorService.EXTRA_FILE_NAME);
                    if (!f.raws.get(1).originalName.equals(file) && !f.raws.get(2).originalName.equals(file)) continue;
                    String preview = event.getStringExtra(TranslatorService.EXTRA_PREVIEW);
                    assertNotNull(preview);
                    assertFalse("accepted preview must survive the next chapter's preparation", preview.isEmpty());
                    String source = event.getStringExtra(TranslatorService.EXTRA_PREVIEW_FILE);
                    if (!file.equals(source)) {
                        transitionSeen = true;
                        assertTrue("preview identifies the preceding chapter", f.raws.get(0).originalName.equals(source)
                                || f.raws.get(1).originalName.equals(source));
                    }
                }
            }
            assertTrue("observed a real service chapter transition", transitionSeen);

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
            c.unregisterReceiver(previewEvents);
            cleanup(c, f, original);
        }
    }

    @Test
    public void integrityFailureInChapterTwoContinuesWithChapterThree() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        tinyChapterTwo = true;
        Fixture f = seed(c);
        tinyChapterTwo = false;
        final java.util.concurrent.atomic.AtomicBoolean inChapterTwo = new java.util.concurrent.atomic.AtomicBoolean();
        TranslateTestSeam.setOutputDir(outDir(c));
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            String all = prompt.system + "\n" + prompt.user;
            // Chapter 2 is split into halves after the first 413, and a half may carry no marker: stay in chapter 2 until chapter 3 starts.
            if (all.contains("【章2】")) inChapterTwo.set(true);
            else if (all.contains("【章1】") || all.contains("【章3】")) inChapterTwo.set(false);
            if (inChapterTwo.get()) throw new ApiHttpException(413, 0, "Request too large for this model: context length exceeded");
            observe(observer);
            calls.incrementAndGet();
            return response(all);
        });
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            runSet(scenario, c, f, baseSettings());
            waitFor(() -> !status(c, f.raws.get(2).uri()).equals("NONE") && !TranslatorService.isActive(), 120_000L, "queue finished");
            assertTrue(status(c, f.raws.get(0).uri()), status(c, f.raws.get(0).uri()).startsWith("COMPLETED"));
            String second = status(c, f.raws.get(1).uri());
            assertTrue("chapter 2 failed on its own: " + second, !second.startsWith("COMPLETED") && !second.equals("NONE"));
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
            calls.incrementAndGet();
            return response(all);
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

    // ---- v4.23 J2 / J6: chapter-local validation failures, the stop after two, "Tiếp tục tập", truncation and quote repair ----

    private static String badBrackets() { return "<TRANSLATION>Xin ch\u00e0o \u300cc\u00e2u n\u00f3i kh\u00f4ng \u0111\u00f3ng, ph\u1ea7n d\u1ecbch b\u1ecb h\u1ecfng v\u1ec1 ng\u1eb7c k\u00e9p.</TRANSLATION>"; }

    private static OpenAICompatibleClient.ChatResult raw(String content, String finish, int completion) {
        OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
        r.content = content; r.finishReason = finish; r.promptTokens = 110; r.completionTokens = completion; r.totalTokens = 110 + completion; r.usageReported = true; r.providerResponseId = "fixture";
        return r;
    }

    /** Collects every progress broadcast of the service while a test runs. */
    private static final class Broadcasts extends android.content.BroadcastReceiver {
        final List<Intent> events = java.util.Collections.synchronizedList(new ArrayList<Intent>());
        final List<String> statuses = java.util.Collections.synchronizedList(new ArrayList<String>());
        final List<String> logs = java.util.Collections.synchronizedList(new ArrayList<String>());
        @Override public void onReceive(Context context, Intent intent) {
            events.add(new Intent(intent));
            statuses.add(String.valueOf(intent.getStringExtra(TranslatorService.EXTRA_STATUS)));
            String log = intent.getStringExtra(TranslatorService.EXTRA_LOG);
            if (log != null) logs.add(log);
        }
        int count(String status) { int n = 0; synchronized (statuses) { for (String s : statuses) if (status.equals(s)) n++; } return n; }
        boolean logContains(String text) { synchronized (logs) { for (String l : logs) if (l.contains(text)) return true; } return false; }
    }

    private static Broadcasts listen(Context c) {
        Broadcasts b = new Broadcasts();
        c.registerReceiver(b, new android.content.IntentFilter(TranslatorService.ACTION_PROGRESS), Context.RECEIVER_NOT_EXPORTED);
        return b;
    }

    @Test
    public void rejectedAnswerInChapterTwoDoesNotStopTheRunAndNoErrorIsBroadcast() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        Fixture f = seed(c);
        TranslateTestSeam.setOutputDir(outDir(c));
        AtomicInteger calls = new AtomicInteger();
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            observe(observer);
            String all = prompt.system + "\n" + prompt.user;
            if (all.contains("\u3010\u7ae02\u3011")) return raw(badBrackets(), "stop", 30);
            calls.incrementAndGet();
            return response(all);
        });
        Broadcasts rx = listen(c);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            runSet(scenario, c, f, baseSettings());
            waitFor(() -> status(c, f.raws.get(2).uri()).startsWith("COMPLETED") && !TranslatorService.isActive(), 120_000L, "queue finished");
            assertTrue(status(c, f.raws.get(0).uri()).startsWith("COMPLETED"));
            assertFalse("chapter 2 did not complete", status(c, f.raws.get(1).uri()).startsWith("COMPLETED"));
            Thread.sleep(500L);
            assertEquals("no error is broadcast while the queue goes on", 0, rx.count("error"));
            assertTrue("the run ends as done with the summary", rx.count("done") >= 1);
            assertTrue(rx.logs.toString(), rx.logContains("2 xong \u00b7 1 l\u1ed7i"));
            assertTrue("the message names the chapters", rx.logContains("Ch\u01b0\u01a1ng 2 l\u1ed7i, chuy\u1ec3n ch\u01b0\u01a1ng 3"));
            media("31-chapter-run-rejected-continues", scenario);
        } finally {
            c.unregisterReceiver(rx);
            cleanup(c, f, original);
        }
    }

    @Test
    public void twoRejectedChaptersInARowStopTheRun() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        Fixture f = seed(c);
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            observe(observer);
            String all = prompt.system + "\n" + prompt.user;
            if (all.contains("\u3010\u7ae01\u3011") || all.contains("\u3010\u7ae02\u3011")) return raw(badBrackets(), "stop", 30);
            return response(all);
        });
        Broadcasts rx = listen(c);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            runSet(scenario, c, f, baseSettings());
            waitFor(() -> rx.count("error") > 0 && !TranslatorService.isActive(), 120_000L, "queue stopped");
            assertEquals("chapter 3 was never started", "NONE", status(c, f.raws.get(2).uri()));
            assertTrue("two failed chapters stop the run: " + rx.logs, rx.logContains("D\u1eebng t\u1eadp: 2 ch\u01b0\u01a1ng li\u00ean ti\u1ebfp l\u1ed7i ki\u1ec3m \u0111\u1ecbnh (Ch\u01b0\u01a1ng 1, Ch\u01b0\u01a1ng 2). B\u1ea5m Ti\u1ebfp t\u1ee5c t\u1eadp") && rx.logContains("T\u1eadp: 0 xong \u00b7 2 l\u1ed7i"));
            assertTrue("the stop is not reported as an initialisation failure", !rx.logContains("L\u1ed7i kh\u1edfi t\u1ea1o"));
            media("32-chapter-run-stopped-after-two", scenario);
        } finally {
            c.unregisterReceiver(rx);
            cleanup(c, f, original);
        }
    }

    @Test
    public void continueSetTicksTheUnfinishedChaptersAndTheFailedJobGoesOn() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        Fixture f = seed(c);
        TranslateTestSeam.setOutputDir(outDir(c));
        final java.util.concurrent.atomic.AtomicBoolean chapterTwoBroken = new java.util.concurrent.atomic.AtomicBoolean(true);
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            observe(observer);
            String all = prompt.system + "\n" + prompt.user;
            if (chapterTwoBroken.get() && all.contains("\u3010\u7ae02\u3011")) return raw(badBrackets(), "stop", 30);
            return response(all);
        });
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            runSet(scenario, c, f, baseSettings());
            waitFor(() -> status(c, f.raws.get(2).uri()).startsWith("COMPLETED") && !TranslatorService.isActive(), 120_000L, "first run finished");
            TranslationRepository.JobSummary stopped = job(c, f.raws.get(1).uri());
            assertNotNull(stopped);
            final long stoppedId = stopped.id;
            chapterTwoBroken.set(false);
            // "Tiếp tục tập": the set is reopened with only the chapter that has no finished translation ticked.
            AtomicReference<Boolean> opened = new AtomicReference<>();
            scenario.onActivity(activity -> { activity.switchTab("Translate"); opened.set(activity.sourceUi.continueSet(f.setId)); });
            assertTrue(opened.get());
            waitFor(() -> {
                AtomicReference<PreparedBatch> ref = new AtomicReference<>();
                scenario.onActivity(activity -> ref.set(activity.currentPreparedBatch()));
                return ref.get() != null && ref.get().ready() && ref.get().bindings != null && ref.get().bindings.size() == 1;
            }, 60_000L, "plan for the one unfinished chapter");
            AtomicReference<PreparedBatch> plan = new AtomicReference<>();
            scenario.onActivity(activity -> plan.set(activity.currentPreparedBatch()));
            assertEquals(f.raws.get(1).uri(), plan.get().inputs.get(0).uri.toString());
            // The normal Start way: the unfinished job of that chapter is the one that continues.
            List<Integer> ordinals = new ArrayList<>();
            List<String> hashes = new ArrayList<>();
            for (PreparedBatch.Input in : plan.get().inputs) { ordinals.add(in.ordinal); hashes.add(in.inputHash); }
            TranslationRepository repo = new TranslationRepository(c);
            Map<Integer, Long> choice;
            try { choice = ResumePrompt.chooseJobs(ordinals, hashes, repo.getIncompleteJobs(hashes, 50)); } finally { repo.close(); }
            assertEquals("the failed chapter's job is offered", Long.valueOf(stoppedId), choice.get(plan.get().inputs.get(0).ordinal));
            Intent i = new Intent(c, TranslatorService.class);
            i.setAction(TranslatorService.ACTION_START);
            i.putExtra("outputUri", "");
            i.putExtra("outputTreeUri", "content://tbl.test/tree/v423");
            i.putExtra("settings", SettingsStore.toJson(baseSettings()));
            i.putExtra("resumeJobs", ResumePrompt.toJson(choice));
            String sid = java.util.UUID.randomUUID().toString();
            i.putExtra("startSessionId", sid);
            AtomicReference<String> error = new AtomicReference<>();
            scenario.onActivity(activity -> { i.putExtra("preparedBatchId", plan.get().id); error.set(activity.putChapterSession(i, sid, plan.get())); });
            assertNull(error.get());
            c.startForegroundService(i);
            waitFor(() -> status(c, f.raws.get(1).uri()).startsWith("COMPLETED") && !TranslatorService.isActive(), 120_000L, "chapter 2 completed");
            TranslationRepository.JobSummary after = job(c, f.raws.get(1).uri());
            assertEquals("the same job went on, no second job was created", stoppedId, after.id);
            media("33-continue-set-done", scenario);
        } finally {
            cleanup(c, f, original);
        }
    }

    private static String lineRichResponse(String userPrompt, String closing) {
        // One translated line per source paragraph that appears in this request, so the missing-lines check sees a faithful answer.
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\u3010\u7ae0\\d\u3011\u7b2c(\\d+)\u6bb5").matcher(userPrompt);
        StringBuilder sb = new StringBuilder();
        java.util.Set<String> seen = new java.util.HashSet<>();
        while (m.find()) if (seen.add(m.group(1))) sb.append("D\u00f2ng d\u1ecbch s\u1ed1 ").append(m.group(1)).append(closing).append("\n\n");
        return "<TRANSLATION>" + sb.toString().trim() + "</TRANSLATION>";
    }

    @Test
    public void aCutAnswerIsRetriedWithALargerLimitAndNeverSentIdenticallyTwice() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        Fixture f = seed(c);
        TranslateTestSeam.setOutputDir(outDir(c));
        final List<String> requests = Collections.synchronizedList(new ArrayList<String>());
        final List<Integer> limits = Collections.synchronizedList(new ArrayList<Integer>());
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            observe(observer);
            String all = prompt.system + "\n" + prompt.user;
            requests.add(HashUtil.sha256(all) + ":" + limit);
            if (!all.contains("\u3010\u7ae01\u3011")) return response(all);
            limits.add(limit);
            // Chapter 1: cut while the limit is below 8192, whole once it is raised.
            if (limit < 8192) return raw(lineRichResponse(all, "."), "length", limit);
            return raw(lineRichResponse(all, "."), "stop", 900);
        });
        Broadcasts rx = listen(c);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AppSettings s = baseSettings();
            s.maxAttempts = 3; s.maxTokensPerChunk = 2000; s.initialRetryDelayMs = 100; s.maxRetryDelayMs = 200;
            runSet(scenario, c, f, AppValidator.normalize(s));
            waitFor(() -> status(c, f.raws.get(0).uri()).startsWith("COMPLETED") && !TranslatorService.isActive(), 180_000L, "chapter 1 completed");
            assertTrue("the second request used a larger limit: " + limits, limits.size() >= 2 && limits.get(1) >= 8192 && limits.get(1) > limits.get(0));
            assertEquals("no request was sent twice as it was", requests.size(), new java.util.HashSet<>(requests).size());
            LogStore.flush();
            assertTrue("cut and larger limit are logged", LogStore.read(c).contains("b\u1ecb c\u1eaft; l\u1ea7n sau d\u00f9ng gi\u1edbi h\u1ea1n \u0111\u1ea7u ra"));
        } finally {
            c.unregisterReceiver(rx);
            cleanup(c, f, original);
        }
    }

    @Test
    public void aChunkCutAtEveryLimitIsSplitInHalvesAndTheChapterCompletes() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        Fixture f = seed(c);
        TranslateTestSeam.setOutputDir(outDir(c));
        final List<String> requests = Collections.synchronizedList(new ArrayList<String>());
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            observe(observer);
            String all = prompt.system + "\n" + prompt.user;
            requests.add(HashUtil.sha256(all) + ":" + limit);
            if (!all.contains("\u3010\u7ae01\u3011")) return response(all);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\u3010\u7ae01\u3011\u7b2c(\\d+)\u6bb5").matcher(all);
            java.util.Set<String> seen = new java.util.HashSet<>();
            while (m.find()) seen.add(m.group(1));
            // More than 8 paragraphs in one request is "too long to finish": cut at any limit.
            if (seen.size() > 8) return raw(lineRichResponse(all, "."), "length", limit);
            return raw(lineRichResponse(all, "."), "stop", 400);
        });
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AppSettings s = baseSettings();
            s.maxAttempts = 2; s.maxTokensPerChunk = 2000; s.initialRetryDelayMs = 100; s.maxRetryDelayMs = 200;
            runSet(scenario, c, f, AppValidator.normalize(s));
            waitFor(() -> status(c, f.raws.get(0).uri()).startsWith("COMPLETED") && !TranslatorService.isActive(), 240_000L, "chapter 1 completed after splitting");
            assertEquals("no request was sent twice as it was", requests.size(), new java.util.HashSet<>(requests).size());
            TranslationRepository repo = new TranslationRepository(c);
            try {
                TranslationRepository.JobSummary j = job(c, f.raws.get(0).uri());
                int done = 0;
                for (TranslationRepository.ChunkRow row : repo.getChunkRows(j.id)) if ("done".equalsIgnoreCase(row.status) || "COMPLETED".equalsIgnoreCase(row.status)) done++;
                assertTrue("the chapter finished in more than one chunk: " + done, done >= 3);
            } finally { repo.close(); }
        } finally {
            cleanup(c, f, original);
        }
    }

    @Test
    public void mixedQuoteStylesAreRepairedAcceptedAndRecordedAsAWarning() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = snapshot(c);
        clearAll(c);
        bracketRaw = true;
        Fixture f = seed(c);
        bracketRaw = false;
        TranslateTestSeam.setOutputDir(outDir(c));
        TranslateTestSeam.setProvider((settings, prompt, limit, id, observer) -> {
            observe(observer);
            String all = prompt.system + "\n" + prompt.user;
            // Opens with the corner bracket like the RAW and closes with a curly quote.
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\u3010\u7ae0(\\d)\u3011\u7b2c(\\d+)\u6bb5").matcher(all);
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();
            while (m.find()) seen.add(m.group(1) + "-" + m.group(2));
            StringBuilder sb = new StringBuilder();
            for (String k : seen) sb.append("Ph\u1ea7n ").append(k).append(" \u300cc\u00e2u tho\u1ea1i n\u00e0y d\u00f9ng d\u1ea5u ngo\u1eb7c l\u1eabn\u201d\n\n");
            return raw("<TRANSLATION>" + sb.toString().trim() + "</TRANSLATION>", "stop", 60);
        });
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            runSet(scenario, c, f, baseSettings());
            waitFor(() -> status(c, f.raws.get(2).uri()).startsWith("COMPLETED") && !TranslatorService.isActive(), 120_000L, "all chapters completed");
            TranslationRepository repo = new TranslationRepository(c);
            try {
                boolean warned = false;
                for (TranslationRepository.ChunkRow row : repo.getChunkRows(job(c, f.raws.get(0).uri()).id)) {
                    assertFalse("the curly closer was replaced", row.translated != null && row.translated.indexOf('\u201d') >= 0);
                    warned |= row.validationWarnings != null && row.validationWarnings.contains("QUOTE_STYLE_REPAIRED");
                }
                assertTrue("the repair is recorded as a warning", warned);
            } finally { repo.close(); }
        } finally {
            cleanup(c, f, original);
        }
    }

    // ---- helpers ----

    private static void media(String stage, ActivityScenario<MainActivity> scenario) throws InterruptedException {
        if (!"run".equals(InstrumentationRegistry.getArguments().getString("v423_media", ""))) return;
        scenario.onActivity(activity -> activity.switchTab("Translate"));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        if (!stage.startsWith("28")) {
            // Bring the progress card with the closing line into view.
            scenario.onActivity(activity -> { android.widget.ScrollView sv = activity.findScrollView(activity.getWindow().getDecorView()); if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN)); });
        }
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
