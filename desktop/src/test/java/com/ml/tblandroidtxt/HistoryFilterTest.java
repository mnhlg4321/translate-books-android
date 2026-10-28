package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

/** T7: history search by file name and filter by chapter set on a QA_ database, plus the phone's status labels. */
public class HistoryFilterTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void filterBySetAndSearchByNameReturnTheRightJobs() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        Path root = tmp.getRoot().toPath();
        Path raw = Files.createDirectories(root.resolve("raw"));
        for (int n = 1; n <= 2; n++) {
            StringBuilder sb = new StringBuilder();
            for (int p = 0; p < 3; p++) { sb.append("甲").append(n).append(p); for (int i = 0; i < 84; i++) sb.append('日'); sb.append("。\n\n"); }
            Files.write(raw.resolve("00" + n + "_QA_" + (n == 1 ? "alpha" : "beta") + ".txt"), sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        DesktopRuntime.UiEvents quiet = new DesktopRuntime.UiEvents() {
            @Override public void onSnapshot(RunSnapshot s) { }
            @Override public void onNotification(String text, int done, int total) { }
            @Override public void onRunEnded() { }
            @Override public void onCancelledEnd(String message) { }
        };
        try (DesktopRuntime rt = DesktopRuntime.open(root.resolve("d"), quiet, (settings, prompt, limit, id, observer) -> {
            OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
            r.content = "<TRANSLATION>T.</TRANSLATION>";
            r.finishReason = "stop"; r.usageReported = true; r.promptTokens = 10; r.completionTokens = 5; r.totalTokens = 15;
            return r;
        })) {
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            rt.importRawFolder(raw);
            DesktopSetEditor ed = rt.newSet(java.util.Arrays.asList(rt.raws().list().get(0).id, rt.raws().list().get(1).id));
            ed.setName("QA_tap");
            String setId = ed.save();
            AppSettings s = new AppSettings();
            s.model = "fixture-model"; s.reasoningEffort = "off"; s.optimizationPreset = "full"; s.maxTokensPerChunk = 80;
            assertTrue(rt.startVolume(rt.volumePlanFromSet(setId), s, null, null, root.resolve("out")));
            Thread.sleep(200);
            long deadline = System.currentTimeMillis() + 60_000;
            while (rt.isRunning() && System.currentTimeMillis() < deadline) Thread.sleep(20);

            DesktopHistory h = rt.history();
            assertEquals(2, h.jobIdsOfSet(setId).size());
            assertTrue(h.jobIdsOfSet("no-such-set").isEmpty());
            DesktopHistory.Page inSet = h.page(-1, DesktopHistory.ALL, new HashSet<String>(), "", setId);
            assertEquals("the two chapters fold into one run", 1, inSet.entries.size());
            assertTrue(inSet.entries.get(0).run);
            assertEquals(2, inSet.entries.get(0).jobs.size());
            assertTrue(h.page(-1, DesktopHistory.ALL, new HashSet<String>(), "", "no-such-set").entries.isEmpty());

            DesktopHistory.Page byName = h.page(-1, DesktopHistory.ALL, new HashSet<String>(), "beta", "");
            assertEquals(1, byName.entries.size());
            assertTrue(h.page(-1, DesktopHistory.ALL, new HashSet<String>(), "khong-co-ten-nay", "").entries.isEmpty());

            long jobId = inSet.entries.get(0).jobs.get(0).id;
            DesktopHistory.Detail d = h.detail(jobId);
            assertFalse(d.chunks.isEmpty());
            assertTrue(d.chunks.get(0).attempts >= 1);
            assertTrue(d.chunks.get(0).inputTokens > 0);
            assertEquals(2, h.usageLines().length);
            assertTrue(h.usageLines()[0].startsWith("Hôm nay: 2 chương xong"));
        }
    }

    @Test public void statusLabelsAreThePhoneLabels() {
        assertEquals("Xong", DesktopHistory.statusText("done"));
        assertEquals("Lỗi", DesktopHistory.statusText("error"));
        assertEquals("Tạm dừng", DesktopHistory.statusText("paused"));
        assertEquals("Đang dịch", DesktopHistory.statusText("running"));
        assertEquals("Đã hủy", DesktopHistory.statusText("cancelled"));
        assertEquals("Đã bỏ", DesktopHistory.statusText("dismissed"));
        assertEquals("Cần xem lại", DesktopHistory.statusText("NEEDS_REVIEW"));
        assertEquals("Input đã đổi", DesktopHistory.statusText("INPUT_CONFLICT"));
    }
}
