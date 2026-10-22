package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** Presentation-only synthetic evidence. Does not start a service or make provider requests. */
@RunWith(AndroidJUnit4.class)
public class V423RunReadabilityInstrumentedTest {
    private Context context;
    private SharedPreferences appearance;
    private Map<String, ?> original;

    @Before public void setup() {
        assertFalse(TranslatorService.isActive());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        appearance = context.getSharedPreferences("appearance", Context.MODE_PRIVATE);
        original = new HashMap<>(appearance.getAll());
        String palette = InstrumentationRegistry.getArguments().getString("ui_palette", "paper");
        int font = Integer.parseInt(InstrumentationRegistry.getArguments().getString("ui_font", "100"));
        assertTrue(appearance.edit().putString("mode", "fixed").putString("palette", palette).putInt("fontPercent", font).commit());
    }

    @After public void cleanup() {
        RuntimeStateStore.clear(context);
        SharedPreferences.Editor e = appearance.edit().clear();
        for (Map.Entry<String, ?> item : original.entrySet()) {
            Object v = item.getValue();
            if (v instanceof String) e.putString(item.getKey(), (String) v);
            else if (v instanceof Integer) e.putInt(item.getKey(), (Integer) v);
            else if (v instanceof Float) e.putFloat(item.getKey(), (Float) v);
            else if (v instanceof Long) e.putLong(item.getKey(), (Long) v);
            else if (v instanceof Boolean) e.putBoolean(item.getKey(), (Boolean) v);
        }
        assertTrue(e.commit());
    }

    private void seed(String state, String preview) {
        RuntimeStateSnapshot s = new RuntimeStateSnapshot();
        s.jobState = state; s.status = "PAUSED".equals(state) ? "paused" : "running";
        s.fileName = "007_synthetic-chapter.txt"; s.totalChunks = 4; s.currentChunk = 2; s.completed = 1;
        s.totalTokens = 7300; s.totalCost = 0.004; s.estimatedTotalCost = 0.26; s.elapsedMs = 338_000;
        s.glossaryLocks = 9; s.pronounLocks = 5;
        s.preview = preview; s.previewChunk = 1; s.previewTotal = 4; s.previewFile = "007_synthetic-chapter.txt";
        RuntimeStateStore.save(context, s);
        RuntimeStateStore.saveSession(context, 91_100, 0.0496, 0, 23, 6, 0, 7);
    }

    private static void idle() { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); }

    @Test public void readableMetricsAndAdaptiveScrollablePreview() throws Exception {
        seed("PAUSED", "Đã dịch.");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> a.switchTab("Translate")); idle();
            final int[] shortHeight = new int[1];
            scenario.onActivity(a -> {
                shortHeight[0] = a.previewScroll.getHeight();
                assertTrue("Short preview has no empty fixed viewport", shortHeight[0] > 0 && shortHeight[0] < a.dp(120));
                assertEquals("91.100 token", a.metricTokens.getText().toString());
                assertEquals("5 phút 38 s", a.metricElapsed.getText().toString());
                assertTrue(a.metricRemaining.getText().toString().endsWith(" s"));
                assertEquals("0 chunk lỗi", a.metricFailed.getText().toString());
                assertEquals("Glossaries: 9 luật · Pronoun: 5 luật", a.metricGlossaryLocks.getText().toString());
            });
            String longText = ("Đây là nội dung tổng hợp để kiểm tra vùng đọc và thao tác cuộn; không phải văn bản sách.\n\n").repeat(18);
            seed("PAUSED", longText);
            scenario.onActivity(MainActivity::restoreRuntimeStateToUi); idle();
            scenario.onActivity(a -> {
                assertTrue("Long preview gets more room than the old 150dp viewport", a.previewScroll.getHeight() >= a.dp(160));
                assertTrue(a.previewScroll.getHeight() > shortHeight[0]);
                assertTrue(a.previewText.isTextSelectable());
                for (TextView text : new TextView[]{a.metricTokens, a.metricElapsed, a.metricRemaining, a.metricGlossaryLocks, a.previewMeta, a.topStatusChip}) {
                    assertEquals(TextView.AUTO_SIZE_TEXT_TYPE_NONE, text.getAutoSizeTextType());
                    assertNotNull(text.getLayout());
                    for (int line = 0; line < text.getLayout().getLineCount(); line++) assertEquals("No clipped label: " + text.getText(), 0, text.getLayout().getEllipsisCount(line));
                }
                int[] meta = new int[2], body = new int[2];
                a.previewMeta.getLocationOnScreen(meta); a.previewText.getLocationOnScreen(body);
                assertTrue("No large gap between heading and preview", body[1] - meta[1] - a.previewMeta.getHeight() <= a.dp(10));
                a.previewScroll.scrollTo(0, a.previewText.getHeight());
            }); idle();
            scenario.onActivity(a -> {
                assertTrue("Long preview can actually scroll", a.previewScroll.getScrollY() > 0);
                a.previewToggle.performClick(); assertEquals(View.GONE, a.previewScroll.getVisibility());
                a.previewToggle.performClick(); assertEquals(View.VISIBLE, a.previewScroll.getVisibility());
                a.previewScroll.scrollTo(0, 0);
            }); idle();
            capture("run-panel");
            scenario.onActivity(a -> {
                android.view.ViewParent parent = a.previewScroll.getParent();
                while (parent != null && !(parent instanceof android.widget.ScrollView)) parent = parent.getParent();
                assertNotNull("Preview has an outer scroll container", parent);
                android.widget.ScrollView outer = (android.widget.ScrollView) parent;
                int[] heading = new int[2], viewport = new int[2];
                a.previewMeta.getLocationOnScreen(heading); outer.getLocationOnScreen(viewport);
                outer.scrollBy(0, Math.max(0, heading[1] - viewport[1]));
                assertTrue("The outer page can bring the preview into view", outer.getScrollY() > 0);
            }); idle();
            final java.util.concurrent.CountDownLatch frame = new java.util.concurrent.CountDownLatch(1);
            scenario.onActivity(a -> a.previewMeta.postOnAnimation(frame::countDown));
            assertTrue(frame.await(3, java.util.concurrent.TimeUnit.SECONDS)); idle();
            capture("preview-area");
        }
    }

    @Test public void visibleStateLabelsKeepJobsShortcutAndEmptyPreviewMeaning() {
        seed("IDLE", "");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            for (String state : new String[]{"IDLE", "RUNNING", "PAUSED", "COMPLETED", "FAILED", "VALIDATING"}) {
                seed(state, "");
                scenario.onActivity(a -> { a.switchTab("Translate"); a.restoreRuntimeStateToUi(); }); idle();
                scenario.onActivity(a -> {
                    assertEquals(RunPanelFormatter.chipLabel(state), a.topStatusChip.getText().toString());
                    assertTrue(a.topStatusChip.getContentDescription().toString().contains("Mở Công việc"));
                    assertTrue(a.previewMeta.getText().toString().contains("Chưa có chunk được chấp nhận"));
                    assertTrue(a.topStatusChip.getWidth() >= a.dp(48) && a.topStatusChip.getHeight() >= a.dp(48));
                });
            }
            scenario.onActivity(a -> { a.topStatusChip.performClick(); assertEquals("Jobs", a.currentTab); });
        }
    }

    private void capture(String name) throws Exception {
        // Idle main-thread work does not guarantee that SurfaceFlinger has presented the scroll.
        InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(200, 5000);
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        android.content.res.Configuration cfg = context.getResources().getConfiguration();
        String suffix = InstrumentationRegistry.getArguments().getString("ui_palette", "paper") + "-"
                + InstrumentationRegistry.getArguments().getString("ui_font", "100") + "-" + cfg.screenWidthDp + "x" + cfg.screenHeightDp;
        try (FileOutputStream out = new FileOutputStream(new File(context.getExternalFilesDir(null), name + "-" + suffix + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }
}
