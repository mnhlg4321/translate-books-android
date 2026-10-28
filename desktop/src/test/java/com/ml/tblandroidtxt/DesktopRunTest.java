package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** A fake-provider translation on the PC wiring: real SQLite file, real files, DPAPI key, pause/resume and reopening the database. */
public class DesktopRunTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private Path data;
    private Path input;
    private Path outDir;
    private final List<RunSnapshot> snapshots = new ArrayList<>();
    private final AtomicInteger ended = new AtomicInteger();

    @Before public void setUp() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        data = tmp.getRoot().toPath().resolve("data");
        outDir = tmp.getRoot().toPath().resolve("out");
        Files.createDirectories(outDir);
        input = tmp.getRoot().toPath().resolve("QA_raw.txt");
        StringBuilder sb = new StringBuilder();
        String[] lead = {"一", "二", "三", "四"};
        for (int p = 0; p < 4; p++) {
            sb.append(lead[p]);
            for (int i = 0; i < 84; i++) sb.append('日');
            sb.append("。\n\n");
        }
        Files.write(input, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private DesktopRuntime.UiEvents events() {
        return new DesktopRuntime.UiEvents() {
            @Override public void onSnapshot(RunSnapshot s) { synchronized (snapshots) { snapshots.add(s); } }
            @Override public void onNotification(String text, int done, int total) { }
            @Override public void onRunEnded() { ended.incrementAndGet(); }
            @Override public void onCancelledEnd(String message) { ended.incrementAndGet(); }
        };
    }

    private static AppSettings settings() {
        AppSettings s = new AppSettings();
        s.model = "fixture-model";
        s.reasoningEffort = "off";
        s.optimizationPreset = "full";
        s.maxTokensPerChunk = 80;
        return s;
    }

    private static TranslationEngineCore.ProviderClient provider(AtomicInteger calls, Runnable onCall) {
        return (settings, prompt, limit, id, observer) -> {
            int n = calls.incrementAndGet();
            if (onCall != null) onCall.run();
            OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
            r.content = "<TRANSLATION>T" + n + ".</TRANSLATION>";
            r.finishReason = "stop";
            r.usageReported = true;
            r.promptTokens = 10;
            r.completionTokens = 5;
            r.totalTokens = 15;
            return r;
        };
    }

    private void waitUntilIdle(DesktopRuntime rt) throws Exception {
        long deadline = System.currentTimeMillis() + 60_000;
        while (rt.isRunning() && System.currentTimeMillis() < deadline) Thread.sleep(20);
        assertFalse("run finished", rt.isRunning());
    }

    @Test public void aFullRunWritesTheOutputAndTheDatabaseRows() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        DesktopRuntime rt = DesktopRuntime.open(data, events(), provider(calls, null));
        try {
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            AppSettings s = settings();
            int chunks = Chunker.chunkText(new String(Files.readAllBytes(input), StandardCharsets.UTF_8), rt.settingsForRun(s)).size();
            assertTrue("fixture splits into several chunks: " + chunks, chunks >= 3);
            Path out = outDir.resolve("QA_out.txt");
            assertTrue(rt.startNew(input, out, s, null, null));
            assertFalse("only one run at a time", rt.startNew(input, outDir.resolve("other.txt"), s, null, null));
            waitUntilIdle(rt);
            assertEquals(chunks, calls.get());
            StringBuilder expected = new StringBuilder();
            for (int i = 1; i <= chunks; i++) expected.append("T").append(i).append(".\n");
            assertEquals(expected.toString(), new String(Files.readAllBytes(out), StandardCharsets.UTF_8));
            List<TranslationRepositoryCore.JobSummary> jobs = rt.jobs();
            assertEquals(1, jobs.size());
            assertEquals("COMPLETED", jobs.get(0).status);
            assertEquals(chunks, jobs.get(0).doneChunks);
            assertEquals(1, ended.get());
            synchronized (snapshots) { assertEquals("done", snapshots.get(snapshots.size() - 1).status.equals("done") ? "done" : snapshots.get(snapshots.size() - 1).status); }
        } finally {
            rt.close();
        }
    }

    @Test public void pauseThenResumeContinuesTheSameRun() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        final DesktopRuntime[] holder = new DesktopRuntime[1];
        DesktopRuntime rt = DesktopRuntime.open(data, events(), provider(calls, () -> { if (calls.get() == 2) holder[0].pause(); }));
        holder[0] = rt;
        try {
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            AppSettings s = settings();
            Path out = outDir.resolve("QA_out.txt");
            assertTrue(rt.startNew(input, out, s, null, null));
            long deadline = System.currentTimeMillis() + 30_000;
            while (!rt.runner().isPaused() && System.currentTimeMillis() < deadline) Thread.sleep(20);
            assertTrue(rt.runner().isPaused());
            Thread.sleep(700);
            assertTrue("still running while paused", rt.isRunning());
            int during = calls.get();
            Thread.sleep(700);
            assertEquals("no provider call while paused", during, calls.get());
            rt.resume();
            waitUntilIdle(rt);
            assertEquals("COMPLETED", rt.jobs().get(0).status);
        } finally {
            rt.close();
        }
    }

    @Test public void anUnfinishedJobContinuesAfterTheDatabaseIsReopened() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        final DesktopRuntime[] holder = new DesktopRuntime[1];
        DesktopRuntime first = DesktopRuntime.open(data, events(), provider(calls, () -> { if (calls.get() == 2) holder[0].cancel(); }));
        holder[0] = first;
        long jobId;
        int total;
        try {
            first.secrets().save("TEST_KEY_NOT_SECRET");
            AppSettings s = settings();
            Path out = outDir.resolve("QA_out.txt");
            assertTrue(first.startNew(input, out, s, null, null));
            waitUntilIdle(first);
            TranslationRepositoryCore.JobSummary j = first.jobs().get(0);
            jobId = j.id;
            total = j.totalChunks;
            assertTrue("the job is unfinished", j.doneChunks < j.totalChunks);
        } finally {
            first.close();
        }
        AtomicInteger more = new AtomicInteger(100);
        DesktopRuntime second = DesktopRuntime.open(data, events(), provider(more, null));
        try {
            TranslationRepositoryCore.JobSummary before = second.jobs().get(0);
            assertEquals("the job survived 'closing the app'", jobId, before.id);
            assertTrue(second.resumeJob(jobId));
            waitUntilIdle(second);
            TranslationRepositoryCore.JobSummary after = second.jobs().get(0);
            assertEquals("COMPLETED", after.status);
            assertEquals(total, after.doneChunks);
            assertTrue(Files.exists(outDir.resolve("QA_out.txt")));
            assertFalse(new String(Files.readAllBytes(outDir.resolve("QA_out.txt")), StandardCharsets.UTF_8).isEmpty());
        } finally {
            second.close();
        }
    }
}
