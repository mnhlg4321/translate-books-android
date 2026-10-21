package com.ml.tblandroidtxt;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * v4.23 J5 / J3 on the emulator: choosing a palette, accent, font size, density or corners rebuilds the Activity with the matching theme and
 * comes back on the same tab with the same selection; following the system mode switches with Android's night mode; the progress panel keeps
 * its size while its numbers change. The appearance preferences are put back as they were.
 */
@RunWith(AndroidJUnit4.class)
public class V423AppearanceInstrumentedTest {
    private SharedPreferences prefs;
    private java.util.Map<String, ?> original;
    private String originalNight = "auto";

    @Before public void remember() {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = c.getSharedPreferences("appearance", Context.MODE_PRIVATE);
        original = new java.util.HashMap<>(prefs.getAll());
        originalNight = shell("cmd uimode night").toLowerCase();
        prefs.edit().clear().commit();
    }

    @After public void restore() {
        SharedPreferences.Editor e = prefs.edit().clear();
        for (java.util.Map.Entry<String, ?> en : original.entrySet()) {
            Object v = en.getValue();
            if (v instanceof String) e.putString(en.getKey(), (String) v);
            else if (v instanceof Integer) e.putInt(en.getKey(), (Integer) v);
        }
        e.commit();
        shell("cmd uimode night " + (originalNight.contains("yes") ? "yes" : originalNight.contains("no") ? "no" : "auto"));
    }

    private static String shell(String command) {
        UiAutomation ui = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        try (ParcelFileDescriptor pfd = ui.executeShellCommand(command);
             BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(pfd.getFileDescriptor())))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) sb.append(line).append('\n');
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private static void waitFor(java.util.concurrent.Callable<Boolean> condition, long timeoutMs, String what) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try { if (condition.call()) return; } catch (RuntimeException ignored) { }
            Thread.sleep(200L);
        }
        throw new AssertionError("timed out: " + what);
    }

    private static <T> T onMain(ActivityScenario<MainActivity> scenario, java.util.function.Function<MainActivity, T> f) {
        AtomicReference<T> out = new AtomicReference<>();
        scenario.onActivity(a -> out.set(f.apply(a)));
        return out.get();
    }

    @Test
    public void lightPaletteRebuildsTheActivityOnTheSameTabWithTheSameSelection() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            final java.util.List<String> ids = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                String name = "v423a-" + i + "-RAW.txt";
                RawImportPlanner.Plan plan = RawImportPlanner.plan(java.util.Collections.singletonList(new RawImportPlanner.Candidate(name, name + " body " + System.nanoTime(), "UTF-8")), LibraryRawStore.hashes(InstrumentationRegistry.getInstrumentation().getTargetContext()));
                ids.add(LibraryRawStore.add(InstrumentationRegistry.getInstrumentation().getTargetContext(), plan).get(0).id);
            }
            scenario.onActivity(a -> {
                a.switchTab("RAW");
                a.bulkUi.controller(LibraryBulkUi.TAB_RAW).enter(ids.get(0));
                a.bulkUi.controller(LibraryBulkUi.TAB_RAW).toggle(ids.get(1));
            });
            assertFalse(onMain(scenario, a -> a.appearance.light));
            final String tabBefore = onMain(scenario, a -> a.currentTab);
            scenario.onActivity(a -> {
                AppearanceSettings next = AppearanceSettings.load(a.getSharedPreferences("appearance", Context.MODE_PRIVATE));
                next.palette = "light";
                a.commitAppearance(next);
            });
            waitFor(() -> onMain(scenario, a -> a.appearance.light), 20_000L, "light palette applied after recreate");
            assertEquals(tabBefore, onMain(scenario, a -> a.currentTab));
            assertEquals("the ticked cards came back", 2, (int) onMain(scenario, a -> a.bulkUi.controller(LibraryBulkUi.TAB_RAW).count()));
            assertTrue(onMain(scenario, a -> a.bulkUi.controller(LibraryBulkUi.TAB_RAW).isSelected(ids.get(1))));
            assertEquals("light surfaces", ColorMath.luminance(onMain(scenario, a -> a.BG)) > 0.5, true);
            assertTrue("status colours are the light palette's", onMain(scenario, a -> a.GREEN == UiAppearance.find("light").ok));
            assertEquals(UiAppearance.find("light").onAccent, (int) onMain(scenario, a -> a.ONACCENT));
            scenario.onActivity(a -> a.bulkUi.controller(LibraryBulkUi.TAB_RAW).exit());
            Context cc = InstrumentationRegistry.getInstrumentation().getTargetContext();
            LibraryRawStore.removeRows(cc, ids);
            LibraryRawStore.purgeFiles(cc, ids);
        }
    }

    @Test
    public void everySettingIsAppliedAfterTheRebuild() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> a.switchTab("Settings"));
            scenario.onActivity(a -> {
                AppearanceSettings next = new AppearanceSettings();
                next.palette = "paper"; next.accent = "#2F8FD8"; next.fontPercent = 130;
                next.density = AppearanceSettings.DENSITY_COMPACT; next.corners = AppearanceSettings.CORNERS_ROUND;
                a.commitAppearance(next);
            });
            waitFor(() -> onMain(scenario, a -> a.fontScale) > 1.29f, 20_000L, "font scale applied");
            assertEquals("Settings", onMain(scenario, a -> a.currentTab));
            assertEquals(0.75f, onMain(scenario, a -> a.densityScale), 0);
            assertEquals(1.8f, onMain(scenario, a -> a.cornerFactor), 0);
            assertEquals(0xFF2F8FD8, (int) onMain(scenario, a -> a.BLUE));
            assertEquals(ColorMath.onColor(0xFF2F8FD8), (int) onMain(scenario, a -> a.ONACCENT));
            // A button never drops below 44 dp or gets a fixed height, even at 130 percent and compact density.
            float density = InstrumentationRegistry.getInstrumentation().getTargetContext().getResources().getDisplayMetrics().density;
            assertTrue(onMain(scenario, a -> { android.widget.Button b = a.secondaryButton("Một nhãn nút khá dài để thử việc xuống dòng", v -> { }); return b.getMinimumHeight() >= Math.round(48 * density) - 1; }));
            // Reset puts everything back.
            scenario.onActivity(a -> a.commitAppearance(AppearanceSettings.defaults()));
            waitFor(() -> onMain(scenario, a -> a.fontScale) == 1f && !onMain(scenario, a -> a.appearance.light), 20_000L, "defaults back");
            assertEquals("default", onMain(scenario, a -> a.appearance.id));
            assertEquals(1f, onMain(scenario, a -> a.cornerFactor), 0);
        }
    }

    @Test
    public void followingTheSystemSwitchesWithNightMode() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                AppearanceSettings next = new AppearanceSettings();
                next.mode = AppearanceSettings.MODE_SYSTEM; next.palette = "slate"; next.paletteLight = "paper";
                a.commitAppearance(next);
            });
            shell("cmd uimode night yes");
            waitFor(() -> "slate".equals(onMain(scenario, a -> a.appearance.id)), 30_000L, "dark palette at night");
            shell("cmd uimode night no");
            waitFor(() -> "paper".equals(onMain(scenario, a -> a.appearance.id)), 30_000L, "light palette by day");
            assertTrue(onMain(scenario, a -> a.appearance.light));
        }
    }

    @Test
    public void theTopChipAndThePanelTitleNeverDisagree() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> a.switchTab("Translate"));
            String[][] cases = {{"RUNNING", "%s", "Đang dịch"}, {"PAUSED", "%s", "Tạm dừng"}};
            for (String[] k : cases) {
                scenario.onActivity(a -> { a.translationActive = true; a.updateTrackingFromIntent(progress(k[0], 14, 3, 0, 4, 6, 5, 1000, 0.01, "004.txt")); });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                String chip = onMain(scenario, a -> a.topStatusChip.getText().toString());
                String title = onMain(scenario, a -> a.trackingTitle.getText().toString());
                assertEquals(k[2], chip);
                assertTrue(title, title.startsWith(RunPanelFormatter.stateLabel(k[0])));
            }
            // The run ended: the broadcast and the saved state agree on COMPLETED.
            RuntimeStateStore.save(c, "COMPLETED", "done", 100, "", "Completed", "a.txt", 1, 1, 0, 0, 0, 0, 0, 0, false, 0, 0, "", 0, 0, 0, 0, "", 0, 1);
            scenario.onActivity(a -> { a.translationActive = false; a.updateTrackingFromIntent(progress("COMPLETED", 14, 14, 0, 14, 6, 6, 1000, 0.01, "014.txt")); });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals("Hoàn tất", onMain(scenario, a -> a.topStatusChip.getText().toString()));
            assertTrue(onMain(scenario, a -> a.trackingTitle.getText().toString()).startsWith("Xong"));
        }
    }

    private static int[] sizeOf(View v) { return new int[]{v.getWidth(), v.getHeight()}; }

    private static Intent progress(String state, int total, int done, int failed, int index, int chunks, int chunkDone, long tokens, double cost, String file) {
        Intent i = new Intent(TranslatorService.ACTION_PROGRESS);
        i.putExtra(TranslatorService.EXTRA_STATE, state);
        i.putExtra(TranslatorService.EXTRA_FILE_NAME, file);
        i.putExtra(TranslatorService.EXTRA_TOTAL_CHUNKS, chunks);
        i.putExtra(TranslatorService.EXTRA_COMPLETED, chunkDone);
        i.putExtra(TranslatorService.EXTRA_FAILED, failed);
        i.putExtra(TranslatorService.EXTRA_TOTAL_TOKENS, (int) Math.min(Integer.MAX_VALUE, tokens));
        i.putExtra(TranslatorService.EXTRA_TOTAL_COST, cost);
        i.putExtra(TranslatorService.EXTRA_SESSION_TOKENS, tokens);
        i.putExtra(TranslatorService.EXTRA_SESSION_COST, cost);
        i.putExtra(TranslatorService.EXTRA_SESSION_START_MS, System.currentTimeMillis() - 26 * 60_000L);
        i.putExtra(TranslatorService.EXTRA_SET_TOTAL, total);
        i.putExtra(TranslatorService.EXTRA_SET_DONE, done);
        i.putExtra(TranslatorService.EXTRA_SET_FAILED, failed);
        i.putExtra(TranslatorService.EXTRA_SET_INDEX, index);
        i.putExtra(TranslatorService.EXTRA_GLOSSARY_LOCKS, 10);
        i.putExtra(TranslatorService.EXTRA_PRONOUN_LOCKS, 4);
        i.putExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_COST, 0.43);
        return i;
    }

    @Test
    public void theProgressPanelWrapsNamedMetricsWithoutShrinkingText() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> a.switchTab("Translate"));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Intent[] samples = {
                    progress("RUNNING", 14, 0, 0, 1, 6, 0, 0, 0, "001.txt"),
                    progress("RUNNING", 14, 3, 0, 4, 6, 5, 308_123, 0.2602, "004_ATE_JP_VOL6.txt"),
                    progress("RUNNING", 14, 9, 2, 12, 120, 99, 12_345_678, 12.3456, "012_a_very_long_chapter_file_name_that_would_not_fit_in_one_line.txt"),
            };
            for (int k = 0; k < samples.length; k++) {
                final Intent sample = samples[k];
                scenario.onActivity(a -> a.updateTrackingFromIntent(sample));
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                Thread.sleep(300L);
                scenario.onActivity(a -> {
                    for (TextView value : new TextView[]{a.metricTokens, a.metricCost, a.metricElapsed, a.metricRemaining, a.metricGlossaryLocks}) {
                        assertEquals("Never shrink meaningful text to fit", TextView.AUTO_SIZE_TEXT_TYPE_NONE, value.getAutoSizeTextType());
                        assertTrue(value.getText().toString(), value.getWidth() > 0 && value.getHeight() > 0);
                        assertNotNull(value.getLayout());
                        for (int line = 0; line < value.getLayout().getLineCount(); line++) assertEquals(0, value.getLayout().getEllipsisCount(line));
                    }
                });
            }
            scenario.onActivity(a -> {
                assertTrue("set totals are shown", a.setLine.getText().toString().contains("xong") && a.setBarRow.getVisibility() == View.VISIBLE);
                assertEquals("12.3456 is the whole run's cost", "$12.3456", a.metricCost.getText().toString());
                assertEquals("Glossaries: 10 luật · Pronoun: 4 luật", a.metricGlossaryLocks.getText().toString());
                assertEquals("Token cả lượt", a.tokensCellLabel.getText().toString());
            });
        }
    }
}
