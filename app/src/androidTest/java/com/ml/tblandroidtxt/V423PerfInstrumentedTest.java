package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * v4.23 J1 on the emulator, synthetic data only (USD 0): 19 RAW, 19 Glossary, 18 Pronoun and a 14-chapter set. Ticking a chapter
 * must not ask for a real SAF write probe (the persisted tree grant, when the emulator has one, makes every probe a create + write +
 * delete) and the tick must reach the next frame quickly. Runs against an older build too: the probe counter is read by reflection
 * and reported as -1 when absent, so the same file gives the before/after numbers in the log (tag V423PERF).
 */
@RunWith(AndroidJUnit4.class)
public class V423PerfInstrumentedTest {
    private static final String[] PREFS = {"glossary_store", "pronoun_store", "settings", "translate_source"};
    private static final String TAG = "v423p-";
    private static final int CHAPTERS = 14;

    private static String rawText(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 160; i++) sb.append("【章").append(n).append("】第").append(i).append("段。登場人物は花子と太郎で、これは性能試験のための十分に長い日本語の段落です。\n\n");
        return sb.append(System.nanoTime()).toString();
    }

    private static int probeCount() {
        try {
            Field f = FileUtil.class.getField("WRITE_PROBE_COUNT");
            return ((java.util.concurrent.atomic.AtomicInteger) f.get(null)).get();
        } catch (Exception e) { return -1; }
    }

    private static Uri grantedTree(Context c) {
        for (android.content.UriPermission p : c.getContentResolver().getPersistedUriPermissions()) {
            if (p.isWritePermission() && android.provider.DocumentsContract.isTreeUri(p.getUri())) return p.getUri();
        }
        return null;
    }

    private static CheckBox findCheck(View v, String description) {
        if (v instanceof CheckBox && description.equals(String.valueOf(v.getContentDescription()))) return (CheckBox) v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) { CheckBox c = findCheck(g.getChildAt(i), description); if (c != null) return c; }
        }
        return null;
    }

    private static void waitFor(java.util.concurrent.Callable<Boolean> condition, long timeoutMs, String what) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (condition.call()) return;
            Thread.sleep(100L);
        }
        throw new AssertionError("timed out: " + what);
    }

    @Test
    public void tickingChaptersAsksForNoSafProbeAndReachesTheNextFrameFast() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, Map<String, ?>> original = new HashMap<>();
        for (String name : PREFS) original.put(name, new HashMap<>(c.getSharedPreferences(name, Context.MODE_PRIVATE).getAll()));
        c.getSharedPreferences("glossary_store", Context.MODE_PRIVATE).edit().clear().commit();
        c.getSharedPreferences("pronoun_store", Context.MODE_PRIVATE).edit().clear().putBoolean("legacy_migrated", true).commit();
        c.getSharedPreferences("translate_source", Context.MODE_PRIVATE).edit().clear().commit();
        clear(GlossaryStore.class, "cachedJson", "cachedGlossaries");
        clear(PronounStore.class, "cachedJson", "cachedProfiles");

        ArrayList<GlossaryStore.Glossary> glossaries = new ArrayList<>();
        for (int i = 1; i <= 19; i++) {
            GlossaryStore.Glossary g = new GlossaryStore.Glossary();
            g.id = "v423p-g" + i; g.name = String.format("%03d_%sGlossary.csv", i, TAG);
            for (int t = 0; t < 40; t++) g.terms.add(new GlossaryStore.Term("名前" + t, "Ten" + t, "character"));
            glossaries.add(g);
        }
        assertTrue(GlossaryStore.saveAll(c, glossaries));
        ArrayList<PronounStore.Profile> pronouns = new ArrayList<>();
        for (int i = 1; i <= 18; i++) {
            PronounStore.Profile p = new PronounStore.Profile();
            p.id = "v423p-p" + i; p.name = String.format("%03d_%sPronoun.csv", i, TAG);
            StringBuilder sb = new StringBuilder("from,speaker,target,self,call,scope,note\n");
            for (int t = 0; t < 60; t++) sb.append("Ten").append(t).append(",,Nguoi").append(t).append(",tôi,cậu,,\n");
            p.text = sb.toString();
            pronouns.add(p);
        }
        assertTrue(PronounStore.saveAll(c, pronouns));
        ChapterSetStore.SetData set = new ChapterSetStore.SetData();
        set.name = TAG + "set";
        List<String> names = new ArrayList<>();
        ArrayList<String> rawIds = new ArrayList<>();
        for (int i = 1; i <= 19; i++) {
            String name = String.format("%03d_%sRAW.txt", i, TAG);
            RawImportPlanner.Plan plan = RawImportPlanner.plan(Collections.singletonList(new RawImportPlanner.Candidate(name, rawText(i), "UTF-8")), LibraryRawStore.hashes(c));
            LibraryRawStore.Item raw = LibraryRawStore.add(c, plan).get(0);
            rawIds.add(raw.id);
            if (i <= CHAPTERS) {
                names.add(name);
                set.rows.add(new ChapterSetStore.Row(i, raw.id, "v423p-g" + i, i <= 18 ? "v423p-p" + i : ""));
            }
        }
        ChapterSetStore.save(c, set);
        List<Long> ticks = new ArrayList<>();
        int probesBefore;
        int probes = 0;
        Uri tree = grantedTree(c);
        Uri used = tree != null ? tree : Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Fv423perf");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> { a.outputTreeUri = used; a.switchTab("Translate"); a.sourceUi.selectSet(set.id); });
            waitFor(() -> {
                AtomicReference<PreparedBatch> ref = new AtomicReference<>();
                scenario.onActivity(a -> ref.set(a.currentPreparedBatch()));
                return ref.get() != null && ref.get().ready() && ref.get().bindings != null && ref.get().bindings.size() == CHAPTERS;
            }, 120_000L, "plan for " + CHAPTERS + " chapters");
            Thread.sleep(500L);
            probesBefore = probeCount();
            int expected = CHAPTERS;
            for (int round = 0; round < 12; round++) {
                final String description = "chapter:" + names.get(round % CHAPTERS);
                final CountDownLatch frame = new CountDownLatch(1);
                final long[] elapsed = new long[1];
                scenario.onActivity(a -> {
                    CheckBox box = findCheck(a.getWindow().getDecorView(), description);
                    if (box == null) throw new AssertionError("no checkbox " + description);
                    long t0 = System.nanoTime();
                    box.performClick();
                    Choreographer.getInstance().postFrameCallback(f -> { elapsed[0] = System.nanoTime() - t0; frame.countDown(); });
                });
                assertTrue(frame.await(30, TimeUnit.SECONDS));
                ticks.add(elapsed[0] / 1_000_000L);
                expected += round < CHAPTERS ? -1 : 1;
                final int want = expected;
                waitFor(() -> {
                    AtomicReference<PreparedBatch> ref = new AtomicReference<>();
                    scenario.onActivity(a -> ref.set(a.currentPreparedBatch()));
                    return ref.get() != null && ref.get().ready() && ref.get().bindings != null && ref.get().bindings.size() == want;
                }, 120_000L, "plan after tick " + round);
            }
            probes = probeCount() - probesBefore;
        } finally {
            ChapterSetStore.delete(c, Collections.singletonList(set.id));
            LibraryRawStore.removeRows(c, rawIds);
            LibraryRawStore.purgeFiles(c, rawIds);
            for (String name : PREFS) {
                SharedPreferences.Editor e = c.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear();
                for (Map.Entry<String, ?> en : original.get(name).entrySet()) {
                    Object v = en.getValue();
                    if (v instanceof String) e.putString(en.getKey(), (String) v);
                    else if (v instanceof Boolean) e.putBoolean(en.getKey(), (Boolean) v);
                    else if (v instanceof Integer) e.putInt(en.getKey(), (Integer) v);
                    else if (v instanceof Long) e.putLong(en.getKey(), (Long) v);
                    else if (v instanceof Float) e.putFloat(en.getKey(), (Float) v);
                }
                e.commit();
            }
            clear(GlossaryStore.class, "cachedJson", "cachedGlossaries");
            clear(PronounStore.class, "cachedJson", "cachedProfiles");
            clear(GlossaryStore.class, "corruptJson", null);
            clear(PronounStore.class, "corruptJson", null);
        }
        long[] sorted = new long[ticks.size()];
        for (int i = 0; i < sorted.length; i++) sorted[i] = ticks.get(i);
        Arrays.sort(sorted);
        long p50 = sorted[sorted.length / 2], p95 = sorted[(int) Math.ceil(sorted.length * 0.95) - 1], max = sorted[sorted.length - 1];
        android.util.Log.i("V423PERF", "grant=" + (tree != null) + " ticks_ms=" + ticks + " p50=" + p50 + " p95=" + p95 + " max=" + max + " probeCalls=" + probes
                + " (-1 means this build has no counter; " + ticks.size() + " ticks, " + CHAPTERS + " chapters)");
        if (probes >= 0 && probesBefore >= 0) assertEquals("SAF probe requests during ticks", 0, probes);
        assertTrue("tick -> next frame p95 " + p95 + " ms", p95 < 50);
    }

    private static void clear(Class<?> type, String jsonField, String itemsField) throws Exception {
        Field json = type.getDeclaredField(jsonField);
        json.setAccessible(true);
        json.set(null, null);
        if (itemsField == null) return;
        Field items = type.getDeclaredField(itemsField);
        items.setAccessible(true);
        items.set(null, null);
    }
}
