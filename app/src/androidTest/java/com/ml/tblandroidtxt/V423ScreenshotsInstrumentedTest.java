package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * v4.23 screenshots on the emulator: a driver script reads the "V423SHOT stage=" log lines and takes a screenshot at each one. Not an
 * assertion test; it walks the Translate, Library, Settings > Appearance, Jobs and Biên tập screens in both palettes, at 130 percent with the
 * compact density, with a custom accent and with dialogs open, and puts the appearance preferences back at the end.
 */
@RunWith(AndroidJUnit4.class)
public class V423ScreenshotsInstrumentedTest {
    /** Scrolls the page so that the section header with this title is at the top (the open section is drawn below its header). */
    private static void scrollToText(MainActivity a, String title) {
        final android.widget.ScrollView sv = a.findScrollView(a.getWindow().getDecorView());
        if (sv == null) return;
        sv.post(() -> {
            android.view.View found = findText(sv, title);
            if (found == null) return;
            int y = 0;
            android.view.View v = found;
            while (v != null && v != sv) { y += v.getTop(); v = v.getParent() instanceof android.view.View ? (android.view.View) v.getParent() : null; }
            sv.scrollTo(0, Math.max(0, y - 20));
        });
    }

    private static android.view.View findText(android.view.View v, String title) {
        if (v instanceof android.widget.TextView && ((android.widget.TextView) v).getText().toString().trim().equals(title)) return v;
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) { android.view.View f = findText(g.getChildAt(i), title); if (f != null) return f; }
        }
        return null;
    }

    private static void stage(String name) throws InterruptedException {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(900L);
        android.util.Log.i("V423SHOT", "stage=" + name);
        Thread.sleep(3500L);
    }

    private static void waitFor(java.util.concurrent.Callable<Boolean> condition, long timeoutMs) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try { if (condition.call()) return; } catch (RuntimeException ignored) { }
            Thread.sleep(200L);
        }
        throw new AssertionError("timed out");
    }

    private static <T> T onMain(ActivityScenario<MainActivity> s, java.util.function.Function<MainActivity, T> f) {
        AtomicReference<T> out = new AtomicReference<>();
        s.onActivity(a -> out.set(f.apply(a)));
        return out.get();
    }

    private static void apply(ActivityScenario<MainActivity> s, AppearanceSettings next, String expectedPalette, float expectedFont) throws Exception {
        s.onActivity(a -> a.commitAppearance(next));
        waitFor(() -> expectedPalette.equals(onMain(s, a -> a.appearance.id)) && Math.abs(onMain(s, a -> a.fontScale) - expectedFont) < 0.01f, 20_000L);
        Thread.sleep(600L);
    }

    private static Intent running(boolean set) {
        Intent i = new Intent(TranslatorService.ACTION_PROGRESS);
        i.putExtra(TranslatorService.EXTRA_STATE, "RUNNING");
        i.putExtra(TranslatorService.EXTRA_FILE_NAME, "004_ATE_JP_VOL6.txt");
        i.putExtra(TranslatorService.EXTRA_TOTAL_CHUNKS, 6);
        i.putExtra(TranslatorService.EXTRA_COMPLETED, 4);
        i.putExtra(TranslatorService.EXTRA_CURRENT_CHUNK, 5);
        i.putExtra(TranslatorService.EXTRA_TOTAL_TOKENS, 11_600);
        i.putExtra(TranslatorService.EXTRA_TOTAL_COST, 0.006);
        i.putExtra(TranslatorService.EXTRA_SESSION_TOKENS, 308_000L);
        i.putExtra(TranslatorService.EXTRA_SESSION_COST, 0.2602);
        i.putExtra(TranslatorService.EXTRA_SESSION_START_MS, System.currentTimeMillis() - (26 * 60 + 11) * 1000L);
        i.putExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_COST, 0.43);
        i.putExtra(TranslatorService.EXTRA_GLOSSARY_LOCKS, 10);
        i.putExtra(TranslatorService.EXTRA_PRONOUN_LOCKS, 4);
        i.putExtra(TranslatorService.EXTRA_PREVIEW, "Bản dịch gần nhất của chunk 4 sẽ hiện ở đây.");
        i.putExtra(TranslatorService.EXTRA_PREVIEW_CHUNK, 4);
        if (set) {
            i.putExtra(TranslatorService.EXTRA_SET_TOTAL, 14);
            i.putExtra(TranslatorService.EXTRA_SET_DONE, 3);
            i.putExtra(TranslatorService.EXTRA_SET_FAILED, 0);
            i.putExtra(TranslatorService.EXTRA_SET_INDEX, 4);
        }
        return i;
    }

    /** Panel scenes: a set running, the same set paused, a single file running and a run stopped after two failed chapters; chip and title together. */
    private static void panelScenes(ActivityScenario<MainActivity> s, String prefix, Context c) throws Exception {
        s.onActivity(a -> a.switchTab("Translate"));
        s.onActivity(a -> { a.translationActive = true; a.updateTrackingFromIntent(running(true)); });
        stage(prefix + "-panel-set-running");
        s.onActivity(a -> { Intent i = running(true); i.putExtra(TranslatorService.EXTRA_STATE, "PAUSED"); a.updateTrackingFromIntent(i); });
        stage(prefix + "-panel-set-paused");
        s.onActivity(a -> { Intent i = running(false); a.updateTrackingFromIntent(i); });
        stage(prefix + "-panel-single-running");
        RuntimeStateStore.save(c, "FAILED", "error", -1, "", "Failed", "005.txt", 6, 2, 0, 0, 0, 0, 0, 0, false, 0, 0, "", 0, 0, 0, 0, "", 0, 1);
        s.onActivity(a -> {
            a.translationActive = false;
            Intent i = running(true);
            i.putExtra(TranslatorService.EXTRA_STATE, "FAILED");
            i.putExtra(TranslatorService.EXTRA_SET_DONE, 3);
            i.putExtra(TranslatorService.EXTRA_SET_FAILED, 2);
            i.putExtra(TranslatorService.EXTRA_SET_INDEX, 5);
            a.updateTrackingFromIntent(i);
            ChapterRunOutcome o = new ChapterRunOutcome(14);
            o.done("1"); o.done("2"); o.done("3"); o.failed("4", "x", "Ch\u01b0\u01a1ng 4"); o.failed("5", "x", "Ch\u01b0\u01a1ng 5");
            a.statusBanner.setText("⚠ " + o.stopSentence() + (char) 10 + o.summary());
            a.statusBanner.setVisibility(android.view.View.VISIBLE);
            a.refreshTopStatusChip();
        });
        s.onActivity(a -> {
            final android.widget.ScrollView sv = a.findScrollView(a.getWindow().getDecorView());
            if (sv == null) return;
            sv.post(() -> {
                int y = 0;
                android.view.View v = a.statusBanner;
                while (v != null && v != sv) { y += v.getTop(); v = v.getParent() instanceof android.view.View ? (android.view.View) v.getParent() : null; }
                sv.scrollTo(0, Math.max(0, y - 30));
            });
        });
        stage(prefix + "-panel-stopped-after-two");
        s.onActivity(a -> a.statusBanner.setVisibility(android.view.View.GONE));
    }

    @Test
    public void walkThroughTheScreens() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = c.getSharedPreferences("appearance", Context.MODE_PRIVATE);
        java.util.Map<String, ?> original = new java.util.HashMap<>(prefs.getAll());
        prefs.edit().clear().commit();
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            // 1. dark, idle Translate, panel not running
            s.onActivity(a -> a.switchTab("Translate"));
            stage("01-dark-translate");
            s.onActivity(a -> { a.updateTrackingFromIntent(running(true)); a.trackingCard.setVisibility(android.view.View.VISIBLE); });
            stage("02-dark-translate-running-set");
            panelScenes(s, "d", c);
            s.onActivity(a -> a.switchTab("Library"));
            stage("03-dark-library");
            s.onActivity(a -> a.switchTab("Jobs"));
            stage("04-dark-jobs");
            s.onActivity(a -> a.switchTab("Settings"));
            stage("05-dark-settings");
            s.onActivity(a -> { a.switchTab("Settings"); a.settingsCategory = "Appearance"; a.settingsSectionExpanded = true; a.invalidatePage("Settings"); a.switchTab("Settings"); scrollToText(a, "Appearance"); });
            stage("06-dark-appearance");
            s.onActivity(a -> new AlertDialog.Builder(a).setTitle("Hộp thoại mẫu").setMessage("Nội dung hộp thoại, chữ phụ và nút.").setPositiveButton("Đồng ý", null).setNegativeButton("Hủy", null).show());
            stage("07-dark-dialog");
            s.onActivity(a -> a.getWindow().getDecorView().cancelPendingInputEvents());
            android.app.Instrumentation inst = InstrumentationRegistry.getInstrumentation();
            inst.sendKeySync(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BACK));
            inst.sendKeySync(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_BACK));

            // 2. Light
            AppearanceSettings light = new AppearanceSettings();
            light.palette = "light";
            apply(s, light, "light", 1f);
            s.onActivity(a -> a.switchTab("Translate"));
            stage("10-light-translate");
            s.onActivity(a -> { a.updateTrackingFromIntent(running(true)); });
            stage("11-light-translate-running-set");
            panelScenes(s, "l", c);
            s.onActivity(a -> a.switchTab("Library"));
            stage("12-light-library");
            s.onActivity(a -> a.switchTab("Jobs"));
            stage("13-light-jobs");
            s.onActivity(a -> a.switchTab("Editorial"));
            stage("14-light-editorial");
            s.onActivity(a -> { a.switchTab("Settings"); a.settingsCategory = "Appearance"; a.settingsSectionExpanded = true; a.invalidatePage("Settings"); a.switchTab("Settings"); scrollToText(a, "Appearance"); });
            stage("15-light-appearance");
            s.onActivity(a -> new AlertDialog.Builder(a).setTitle("Hộp thoại mẫu").setMessage("Nội dung hộp thoại, chữ phụ và nút.").setPositiveButton("Đồng ý", null).setNegativeButton("Hủy", null).show());
            stage("16-light-dialog");
            inst.sendKeySync(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BACK));
            inst.sendKeySync(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_BACK));

            // 3. Paper
            AppearanceSettings paper = new AppearanceSettings();
            paper.palette = "paper";
            apply(s, paper, "paper", 1f);
            s.onActivity(a -> a.switchTab("Translate"));
            stage("20-paper-translate");
            s.onActivity(a -> a.switchTab("Editorial"));
            stage("21-paper-editorial");

            // 4. 130 percent, compact, custom accent, round corners (dark)
            AppearanceSettings big = new AppearanceSettings();
            big.palette = "default"; big.fontPercent = 130; big.density = AppearanceSettings.DENSITY_COMPACT; big.accent = "#2F8FD8"; big.corners = AppearanceSettings.CORNERS_ROUND;
            apply(s, big, "default", 1.3f);
            s.onActivity(a -> a.switchTab("Translate"));
            stage("30-130-compact-translate");
            s.onActivity(a -> { a.updateTrackingFromIntent(running(true)); });
            stage("31-130-compact-running-set");
            panelScenes(s, "b", c);
            s.onActivity(a -> { a.switchTab("Library"); });
            stage("32-130-compact-library");
            s.onActivity(a -> { a.switchTab("Settings"); a.settingsCategory = "Appearance"; a.settingsSectionExpanded = true; a.invalidatePage("Settings"); a.switchTab("Settings"); scrollToText(a, "Appearance"); });
            stage("33-130-compact-appearance");
            s.onActivity(a -> a.switchTab("Library"));
            s.onActivity(a -> { android.widget.ScrollView sv = a.findScrollView(a.getWindow().getDecorView()); if (sv != null) sv.post(() -> sv.fullScroll(android.view.View.FOCUS_DOWN)); });
            stage("35-130-files-bottom");

            // 5. light + 130 + compact
            AppearanceSettings lightBig = new AppearanceSettings();
            lightBig.palette = "light"; lightBig.fontPercent = 130; lightBig.density = AppearanceSettings.DENSITY_COMPACT;
            apply(s, lightBig, "light", 1.3f);
            s.onActivity(a -> a.switchTab("Library"));
            stage("40-light-130-library");
            s.onActivity(a -> a.switchTab("Translate"));
            stage("41-light-130-translate");

            apply(s, AppearanceSettings.defaults(), "default", 1f);
        } finally {
            SharedPreferences.Editor e = prefs.edit().clear();
            for (java.util.Map.Entry<String, ?> en : original.entrySet()) {
                Object v = en.getValue();
                if (v instanceof String) e.putString(en.getKey(), (String) v);
                else if (v instanceof Integer) e.putInt(en.getKey(), (Integer) v);
            }
            e.commit();
        }
    }
}
