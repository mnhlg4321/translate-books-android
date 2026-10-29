package com.ml.tblandroidtxt.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.DesktopPromptPreview;
import com.ml.tblandroidtxt.DesktopRuntime;

import javafx.application.Platform;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** The "Xem trước prompt" body shows its content the moment it is built (real controls, real toolkit, QA_ text). */
public class PromptPreviewViewTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static void onFx(Runnable r) throws Exception {
        try { Platform.startup(() -> { }); } catch (IllegalStateException alreadyRunning) { /* toolkit is up */ }
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try { r.run(); } catch (Throwable t) { error.set(t); } finally { done.countDown(); }
        });
        assertTrue(done.await(60, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError(error.get());
    }

    @Test public void systemAndUserPromptAreFilledAndEqualToThePreviewOfTheCore() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        Path raw = tmp.getRoot().toPath().resolve("001_QA_raw.txt");
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < 4; p++) { sb.append("甲").append(p); for (int i = 0; i < 84; i++) sb.append('日'); sb.append("。\n\n"); }
        Files.write(raw, sb.toString().getBytes(StandardCharsets.UTF_8));
        try (DesktopRuntime rt = DesktopRuntime.open(tmp.getRoot().toPath().resolve("d"), null)) {
            AppSettings s = rt.loadSettings();
            s.maxTokensPerChunk = 80;
            s.optimizationPreset = "full";
            rt.saveSettings(s);
            DesktopPromptPreview.Result want = rt.previewPrompt(raw, rt.loadSettings(), 0, "");
            AtomicReference<PromptPreviewView> holder = new AtomicReference<>();
            onFx(() -> {
                try { holder.set(new PromptPreviewView(rt, raw, rt::loadSettings)); } catch (Exception e) { throw new RuntimeException(e); }
            });
            PromptPreviewView v = holder.get();
            assertNotNull(v);
            assertFalse(v.system.getText().isEmpty());
            assertFalse(v.user.getText().isEmpty());
            assertFalse(v.info.getText().isEmpty());
            assertEquals(want.system, v.system.getText());
            assertEquals(want.user, v.user.getText());
            assertTrue(v.info.getText().startsWith("Chunk 1/"));
            assertEquals(want.copyText(), v.copyText());
            assertTrue(v.pick.getItems().size() > 1);
            onFx(() -> v.pick.setValue(1));
            assertTrue("picking another chunk reloads", v.info.getText().startsWith("Chunk 2/"));
        }
    }
}
