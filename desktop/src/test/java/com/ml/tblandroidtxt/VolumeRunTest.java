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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** A whole volume on the PC wiring with a scripted provider: matching, per-chapter profiles, session file, cost cap, folder rules. */
public class VolumeRunTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private Path root;
    private Path rawDir, glossaryDir, pronounDir, outDir;
    private final List<String> prompts = new ArrayList<>();
    private final AtomicInteger ended = new AtomicInteger();

    @Before public void setUp() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        root = tmp.getRoot().toPath();
        rawDir = Files.createDirectories(root.resolve("raw"));
        glossaryDir = Files.createDirectories(root.resolve("glossary"));
        pronounDir = Files.createDirectories(root.resolve("pronoun"));
        outDir = root.resolve("out");
        for (int n = 1; n <= 3; n++) {
            StringBuilder sb = new StringBuilder();
            for (int p = 0; p < 4; p++) {
                sb.append("甲").append(n).append(p == 0 ? "" : "x");
                for (int i = 0; i < 84; i++) sb.append('日');
                sb.append("。\n\n");
            }
            write(rawDir.resolve("00" + n + "_QA_raw.txt"), sb.toString());
        }
        write(glossaryDir.resolve("001_QA_gl.csv"), "source,target,category,note\n甲1,QA_TERM_ONE,term,\n");
        write(glossaryDir.resolve("002_QA_gl.csv"), "source,target,category,note\n甲2,QA_TERM_TWO,term,\n");
        write(glossaryDir.resolve("QA_GLOSSARY_WORKING.csv"), "source,target,category,note\n甲3,QA_TERM_SHARED,term,\n");
        write(pronounDir.resolve("001_QA_pr.csv"), "from,speaker,target,self,call,scope,note\nA,A,B,tao,mày,*,\n");
        write(pronounDir.resolve("002_QA_pr.csv"), "from,speaker,target,self,call,scope,note\nB,B,A,tôi,anh,*,\n");
    }

    private static void write(Path f, String text) throws Exception { Files.write(f, text.getBytes(StandardCharsets.UTF_8)); }

    private DesktopRuntime.UiEvents events() {
        return new DesktopRuntime.UiEvents() {
            @Override public void onSnapshot(RunSnapshot s) { }
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

    private TranslationEngineCore.ProviderClient provider(double cost) {
        AtomicInteger calls = new AtomicInteger();
        return (settings, prompt, limit, id, observer) -> {
            int n = calls.incrementAndGet();
            synchronized (prompts) { prompts.add(prompt.system + "\n" + prompt.user); }
            OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
            r.content = "<TRANSLATION>T" + n + ".</TRANSLATION>";
            r.finishReason = "stop";
            r.usageReported = true;
            r.promptTokens = 10; r.completionTokens = 5; r.totalTokens = 15;
            if (cost > 0) { r.providerCostReported = true; r.providerCost = cost; }
            return r;
        };
    }

    private void waitUntilIdle(DesktopRuntime rt) throws Exception {
        long deadline = System.currentTimeMillis() + 60_000;
        while (rt.isRunning() && System.currentTimeMillis() < deadline) Thread.sleep(20);
        assertFalse("run finished", rt.isRunning());
    }

    private void loadLibrary(DesktopRuntime rt) throws Exception {
        assertEquals(3, rt.importRawFolder(rawDir).added);
        assertEquals(3, rt.importGlossaryFolder(glossaryDir).added);
        assertEquals(2, rt.importPronounFolder(pronounDir).added);
    }

    @Test public void foldersMatchByChapterNumberAndASharedFileLeavesTheCellEmpty() throws Exception {
        try (DesktopRuntime rt = DesktopRuntime.open(root.resolve("d1"), events(), provider(0))) {
            loadLibrary(rt);
            DesktopRuntime.FolderImport again = rt.importGlossaryFolder(glossaryDir);
            assertEquals("a second scan adds nothing", 0, again.added);
            assertEquals(3, again.skipped);
            VolumePlan plan = rt.newVolumePlan(null);
            assertEquals(3, plan.size());
            VolumePlan.RowView one = plan.row(0), three = plan.row(2);
            assertEquals(1, one.chapterNo);
            assertEquals("001_QA_gl", one.glossaryLabel);
            assertEquals("001_QA_pr", one.pronounLabel);
            assertEquals("GREEN", one.status);
            assertEquals("chapter 3 has neither a numbered Glossary nor a Pronoun", "YELLOW", three.status);
            assertEquals("", three.glossaryLabel);
            assertFalse(plan.hasRed());
            assertTrue(plan.hasMissingProfiles());
            String shared = null;
            for (String[] g : plan.glossaryChoices()) if (g[1].equals("QA_GLOSSARY_WORKING")) shared = g[0];
            plan.useGlossaryForAll(shared);
            assertEquals("QA_GLOSSARY_WORKING", plan.row(1).glossaryLabel);
            plan.setGlossary(0, "");
            assertEquals("Không dùng", plan.row(0).glossaryLabel);
            plan.setExcluded(2, true);
            assertEquals(2, plan.included());
            plan.setChapterNo(1, 1);
            assertEquals("two RAW with the same number are red", "RED", plan.row(1).status);
            assertTrue(plan.hasRed());
            assertFalse("a plan with a red row never starts", rt.startVolume(plan, settings(), null, null, outDir));
        }
    }

    @Test public void aFolderWithOnlyAChaptersSubfolderSaysWhatWasScanned() throws Exception {
        try (DesktopRuntime rt = DesktopRuntime.open(root.resolve("d2"), events(), provider(0))) {
            Path book = Files.createDirectories(root.resolve("book"));
            Path chapters = Files.createDirectories(book.resolve("chapters"));
            write(chapters.resolve("001_QA_raw.txt"), Files.readString(rawDir.resolve("001_QA_raw.txt")));
            DesktopRuntime.FolderImport top = rt.importRawFolder(book);
            assertEquals(0, top.added);
            assertTrue(top.note, top.note.contains("chapters") && top.note.contains("không quét thư mục con"));
            assertEquals(1, rt.importRawFolder(chapters).added);
        }
    }

    @Test public void eachChapterIsTranslatedWithItsOwnProfilesAndTheRunIsRecordedAsOneSession() throws Exception {
        try (DesktopRuntime rt = DesktopRuntime.open(root.resolve("d3"), events(), provider(0))) {
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            loadLibrary(rt);
            VolumePlan plan = rt.newVolumePlan(null);
            String shared = null;
            for (String[] g : plan.glossaryChoices()) if (g[1].equals("QA_GLOSSARY_WORKING")) shared = g[0];
            plan.setGlossary(2, shared);
            plan.resolveMissing("", "");
            assertEquals(0, plan.yellow());
            AppSettings s = settings();
            CostEstimator.Estimate estimate = rt.estimateVolume(plan, s);
            assertTrue("three chapters, several chunks each: " + estimate.chunks, estimate.chunks >= 9);
            assertTrue(rt.startVolume(plan, s, null, estimate, outDir));
            waitUntilIdle(rt);
            assertEquals(1, ended.get());
            for (int n = 1; n <= 3; n++) {
                Path out = outDir.resolve(s.buildOutputName("00" + n + "_QA_raw.txt"));
                assertTrue(out + " exists", Files.isRegularFile(out));
            }
            List<TranslationRepositoryCore.JobSummary> jobs = rt.jobs();
            assertEquals(3, jobs.size());
            String session = jobs.get(0).startSessionId;
            assertFalse(session.isEmpty());
            for (TranslationRepositoryCore.JobSummary j : jobs) {
                assertEquals("COMPLETED", j.status);
                assertEquals("all three chapters belong to one run", session, j.startSessionId);
            }
            assertTrue(Files.isRegularFile(rt.dataDir.resolve(ChapterSession.DIR).resolve(session + ".json")));
            synchronized (prompts) {
                boolean one = false, two = false, three = false, wrong = false;
                for (String p : prompts) {
                    if (p.contains("QA_TERM_ONE")) one = true;
                    if (p.contains("QA_TERM_TWO")) two = true;
                    if (p.contains("QA_TERM_SHARED")) three = true;
                    if (p.contains("甲1") && (p.contains("QA_TERM_TWO") || p.contains("QA_TERM_SHARED"))) wrong = true;
                }
                assertTrue("chapter 1 used its Glossary", one);
                assertTrue("chapter 2 used its Glossary", two);
                assertTrue("chapter 3 used the one picked by hand", three);
                assertFalse("no chapter used another chapter's Glossary", wrong);
            }
            List<RunGroups.SessionInfo> infos = ChapterSession.readInfos(rt.dataDir.resolve(ChapterSession.DIR).toFile());
            List<RunGroups.Item> items = RunGroups.build(jobs, infos);
            assertEquals(1, items.size());
            assertEquals(3, items.get(0).run.chapters.size());
            assertEquals(3, items.get(0).run.done);
        }
    }

    @Test public void theCostCapBelongsToTheWholeRunAndStopsBeforeTheNextChapter() throws Exception {
        try (DesktopRuntime rt = DesktopRuntime.open(root.resolve("d4"), events(), provider(0.04))) {
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            loadLibrary(rt);
            VolumePlan plan = rt.newVolumePlan(null);
            plan.resolveMissing("", "");
            AppSettings s = settings();
            assertTrue(rt.startVolume(plan, s, 0.05, null, outDir));
            waitUntilIdle(rt);
            List<TranslationRepositoryCore.JobSummary> jobs = rt.jobs();
            assertEquals("chapter 2 never started", 1, jobs.size());
            assertEquals("paused", jobs.get(0).status);
            assertTrue("the first chapter stopped before it finished", jobs.get(0).doneChunks < jobs.get(0).totalChunks);
            assertEquals(2, prompts.size());
        }
    }

    @Test public void aRawDeletedAfterMatchingFailsTheRunBeforeAnyCall() throws Exception {
        try (DesktopRuntime rt = DesktopRuntime.open(root.resolve("d5"), events(), provider(0))) {
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            loadLibrary(rt);
            VolumePlan plan = rt.newVolumePlan(null);
            plan.resolveMissing("", "");
            rt.raws().delete(new ArrayList<>(Arrays.asList(rt.raws().list().get(1).id)));
            assertTrue(rt.startVolume(plan, settings(), null, null, outDir));
            waitUntilIdle(rt);
            assertEquals(0, prompts.size());
            assertTrue(rt.jobs().isEmpty());
        }
    }

    @Test public void historyFoldsTheRunIntoOneLineFiltersByStatusAndShowsNumbersOnly() throws Exception {
        try (DesktopRuntime rt = DesktopRuntime.open(root.resolve("d6"), events(), provider(0))) {
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            loadLibrary(rt);
            VolumePlan plan = rt.newVolumePlan(null);
            plan.resolveMissing("", "");
            assertTrue(rt.startVolume(plan, settings(), null, null, outDir));
            waitUntilIdle(rt);
            // one more plain single-file job in the same database
            assertTrue(rt.startNew(rawDir.resolve("001_QA_raw.txt"), outDir.resolve("single.txt"), settings(), null, null));
            waitUntilIdle(rt);
            java.util.Set<String> shown = new java.util.HashSet<>();
            DesktopHistory.Page page = rt.history().page(-1, DesktopHistory.ALL, shown);
            assertEquals("one run line and one single job", 2, page.entries.size());
            DesktopHistory.Entry run = null, single = null;
            for (DesktopHistory.Entry e : page.entries) { if (e.run) run = e; else single = e; }
            assertEquals(3, run.jobs.size());
            assertEquals("Xong", run.statusText);
            assertTrue(run.progressText, run.progressText.startsWith("3/3 chương xong"));
            assertEquals(1, single.jobs.size());
            assertEquals(1, shown.size());
            assertEquals("a later page never lists the run again", 0, rt.history().page(-1, DesktopHistory.ALL, shown).entries.stream().filter(e -> e.run).count());
            assertEquals(2, rt.history().page(-1, DesktopHistory.DONE, new java.util.HashSet<>()).entries.size());
            assertEquals(0, rt.history().page(-1, DesktopHistory.FAILED, new java.util.HashSet<>()).entries.size());
            assertEquals(0, rt.history().page(-1, DesktopHistory.UNFINISHED, new java.util.HashSet<>()).entries.size());
            DesktopHistory.Detail d = rt.history().detail(run.jobs.get(0).id);
            assertFalse(d.attempts.isEmpty());
            assertEquals(d.job.doneChunks, d.attempts.size());
            assertTrue(d.chunkStatuses.containsKey("COMPLETED") || d.chunkStatuses.containsKey("DONE"));
            assertEquals("the output of a file: ref resolves to a path", true, d.outputFile != null && d.outputFile.getParent() != null);
            for (TranslationRepositoryCore.JobAttemptRow a : d.attempts) assertEquals("fixture-model", a.model);
        }
    }
}
