package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/** Synthetic service/store/UI regression. No translation request or book input. */
@RunWith(AndroidJUnit4.class)
public class V423PreviewInstrumentedTest {
    private static class PreviewService extends TranslatorService {
        void attach(Context context) { attachBaseContext(context); }
    }

    private static Object call(TranslatorService service, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = TranslatorService.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(service, args);
    }

    @Test public void chapterResetBroadcastAndStoreKeepOriginalAcceptedIdentity() throws Exception {
        assertFalse("Do not interrupt an active translation", TranslatorService.isActive());
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PreviewService service = new PreviewService();
        service.attach(c);
        try {
            assertEquals(true, call(service, "acquireRun", new Class<?>[]{String.class}, "synthetic preview"));
            call(service, "resetMetrics", new Class<?>[]{String.class, int.class}, "001.txt", 5);
            Field field = TranslatorService.class.getDeclaredField("acceptedPreview");
            field.setAccessible(true);
            AcceptedTranslationPreview p = (AcceptedTranslationPreview) field.get(service);
            p.accept("Đây là bản dịch tổng hợp của chương một.", 5, 5);
            call(service, "resetMetrics", new Class<?>[]{String.class, int.class}, "002.txt", 2);
            call(service, "broadcast", new Class<?>[]{String.class, int.class, String.class}, "running", 25, null);
            Intent saved = RuntimeStateStore.toIntent(c);
            assertEquals("002.txt", saved.getStringExtra(TranslatorService.EXTRA_FILE_NAME));
            assertEquals("001.txt", saved.getStringExtra(TranslatorService.EXTRA_PREVIEW_FILE));
            assertEquals(5, saved.getIntExtra(TranslatorService.EXTRA_PREVIEW_TOTAL, 0));
            assertEquals(p.text, saved.getStringExtra(TranslatorService.EXTRA_PREVIEW));
            call(service, "releaseRun", new Class<?>[]{});
            assertEquals(true, call(service, "acquireRun", new Class<?>[]{String.class}, "new synthetic run"));
            assertEquals("", p.text);
        } finally {
            call(service, "releaseRun", new Class<?>[]{});
            service.onDestroy();
            RuntimeStateStore.clear(c);
        }
    }

    @Test public void lazyTranslatePageRestoresPreviewAfterRecreateOnJobs() {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RuntimeStateSnapshot s = new RuntimeStateSnapshot();
        s.jobState = "PAUSED"; s.status = "paused"; s.fileName = "002.txt";
        s.totalChunks = 2; s.currentChunk = 1;
        s.preview = "Đây là bản dịch tổng hợp đã được chấp nhận.";
        s.previewFile = "001.txt"; s.previewChunk = 5; s.previewTotal = 5;
        RuntimeStateStore.save(c, s);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> a.switchTab("Jobs"));
            scenario.recreate();
            scenario.onActivity(a -> {
                assertEquals("Jobs", a.currentTab);
                a.switchTab("Translate");
                assertEquals(s.preview, a.previewText.getText().toString());
                assertEquals("Bản dịch gần nhất · Chunk 5/5\n001.txt", a.previewMeta.getText().toString());
                assertEquals(android.view.View.VISIBLE, a.previewScroll.getVisibility());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.graphics.Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            assertNotNull(screenshot);
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(
                    new java.io.File(c.getExternalFilesDir(null), "preview-recreated.png"))) {
                assertTrue(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
            } catch (java.io.IOException e) { throw new AssertionError(e); }
            finally { screenshot.recycle(); }
        } finally {
            RuntimeStateStore.clear(c);
        }
    }

    @Test public void lateRefreshAfterActivityDestroyDoesNotCrashOrStartWork() {
        java.util.concurrent.atomic.AtomicReference<MainActivity> old = new java.util.concurrent.atomic.AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> { a.switchTab("Translate"); old.set(a); });
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            MainActivity a = old.get();
            assertTrue(a.isDestroyed());
            a.translationActive = false;
            a.refreshPendingJobCard();
            a.refreshJobCacheAsync(false);
            a.refreshFailedChunksAsync();
            a.dismissJobs(java.util.Collections.emptyList());
            a.deleteJobs(java.util.Collections.emptyList());
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
