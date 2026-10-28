package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** T3: matching by chapter number, saving a set, seeing it in "Dịch cả tập", and the run recording its set id. */
public class ChapterSetTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private Path root, rawDir, glossaryDir, pronounDir, outDir;

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
        write(glossaryDir.resolve("001_QA_gl.csv"), "source,target,category,note\n甲1,QA_ONE,term,\n");
        write(glossaryDir.resolve("002_QA_gl.csv"), "source,target,category,note\n甲2,QA_TWO,term,\n");
        write(glossaryDir.resolve("002_QA_gl_bis.csv"), "source,target,category,note\n甲2,QA_TWO_B,term,\n");
        write(pronounDir.resolve("001_QA_pr.csv"), "from,speaker,target,self,call,scope,note\nA,A,B,tao,mày,*,\n");
    }

    private static void write(Path f, String text) throws Exception { Files.write(f, text.getBytes(StandardCharsets.UTF_8)); }

    private DesktopRuntime.UiEvents quiet() {
        return new DesktopRuntime.UiEvents() {
            @Override public void onSnapshot(RunSnapshot s) { }
            @Override public void onNotification(String text, int done, int total) { }
            @Override public void onRunEnded() { }
            @Override public void onCancelledEnd(String message) { }
        };
    }

    private DesktopRuntime load(String name) throws Exception {
        DesktopRuntime rt = DesktopRuntime.open(root.resolve(name), quiet(), (settings, prompt, limit, id, observer) -> {
            OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
            r.content = "<TRANSLATION>T.</TRANSLATION>";
            r.finishReason = "stop";
            r.usageReported = true;
            r.promptTokens = 10; r.completionTokens = 5; r.totalTokens = 15;
            return r;
        });
        rt.importRawFolder(rawDir);
        rt.importGlossaryFolder(glossaryDir);
        rt.importPronounFolder(pronounDir);
        return rt;
    }

    private static List<String> ids(DesktopSetEditor.Choice... c) {
        List<String> out = new ArrayList<>();
        for (DesktopSetEditor.Choice x : c) out.add(x.id);
        return out;
    }

    @Test public void suggestionsAreTheMatcherSuggestionsOnTheSameInput() throws Exception {
        try (DesktopRuntime rt = load("a")) {
            DesktopSetEditor ed = rt.newSet(new ArrayList<String>());
            for (DesktopSetEditor.Choice c : ed.rawChoices()) ed.setRawSlot(append(ed.chosenRaw(), c.id));
            ed.setGlossarySlot(allIds(ed.glossaryChoices()));
            ed.setPronounSlot(allIds(ed.pronounChoices()));

            List<ChapterSetMatcher.Named> raws = new ArrayList<>(), gl = new ArrayList<>(), pr = new ArrayList<>();
            List<Integer> numbers = new ArrayList<>();
            for (DesktopRawStore.Item i : rt.raws().list()) { raws.add(new ChapterSetMatcher.Named(i.id, i.originalName)); numbers.add(i.chapterNo); }
            for (GlossaryStoreCore.Glossary g : rt.glossaries()) gl.add(new ChapterSetMatcher.Named(g.id, g.name));
            for (PronounStoreCore.Profile p : rt.pronouns()) pr.add(new ChapterSetMatcher.Named(p.id, p.name));
            List<ChapterSetMatcher.Row> expected = ChapterSetMatcher.suggest(raws, numbers, gl, pr);

            List<VolumePlan.RowView> rows = ed.rows();
            assertEquals(expected.size(), rows.size());
            for (int i = 0; i < rows.size(); i++) {
                assertEquals(expected.get(i).rawId, rows.get(i).rawId);
                assertEquals(expected.get(i).glossaryId, rows.get(i).glossaryId);
                assertEquals(expected.get(i).pronounId, rows.get(i).pronounId);
                assertEquals(expected.get(i).status.name(), rows.get(i).status);
            }
            assertEquals("chapter 1 is complete", "GREEN", rows.get(0).status);
            assertEquals("chapter 2 has two Glossary candidates and no pick", "RED", rows.get(1).status);
            assertEquals("chapter 3 lacks both", "YELLOW", rows.get(2).status);
            assertNotNull("a red row blocks saving", ed.blockReason());
            assertNull(ed.save());

            DesktopSetEditor.Choice two = null;
            for (DesktopSetEditor.Choice c : ed.rowChoices(1, true)) if (c.label.equals("002_QA_gl")) two = c;
            ed.chooseGlossary(1, two.id);
            assertEquals("YELLOW", ed.rows().get(1).status);
            assertNull(ed.blockReason());
        }
    }

    @Test public void aSavedSetOpensAgainAndShowsInDichCaTap() throws Exception {
        try (DesktopRuntime rt = load("b")) {
            DesktopSetEditor ed = rt.newSet(allRaw(rt));
            ed.setGlossarySlot(allIds(ed.glossaryChoices()));
            ed.setPronounSlot(allIds(ed.pronounChoices()));
            ed.chooseGlossary(1, firstByLabel(ed.rowChoices(1, true), "002_QA_gl"));
            ed.setName("QA_tap_ba_chuong");
            ed.setExcluded(2, true);
            assertEquals(2, ed.included());
            String id = ed.save();
            assertNotNull(id);
            assertFalse(ed.dirty());

            List<DesktopRuntime.SetCard> cards = rt.setCards();
            assertEquals(1, cards.size());
            assertEquals("QA_tap_ba_chuong", cards.get(0).name);
            assertEquals(2, cards.get(0).chapters);

            DesktopSetEditor reopened = rt.openSet(id);
            assertEquals(2, reopened.rows().size());
            assertEquals("002_QA_gl", reopened.rows().get(1).glossaryLabel);
            assertEquals("001_QA_pr", reopened.rows().get(0).pronounLabel);

            VolumePlan plan = rt.volumePlanFromSet(id);
            assertEquals("QA_tap_ba_chuong", plan.name());
            assertEquals(id, plan.setId());
            assertEquals(2, plan.included());
            assertEquals("001_QA_gl", plan.row(0).glossaryLabel);
        }
    }

    @Test public void runningASavedSetWritesItsIdToTheSessionSoHistoryCanFilterByIt() throws Exception {
        try (DesktopRuntime rt = load("c")) {
            DesktopSetEditor ed = rt.newSet(allRaw(rt));
            ed.setGlossarySlot(allIds(ed.glossaryChoices()));
            ed.setPronounSlot(allIds(ed.pronounChoices()));
            ed.chooseGlossary(1, firstByLabel(ed.rowChoices(1, true), "002_QA_gl"));
            ed.setExcluded(2, true);
            String id = ed.save();
            AppSettings s = new AppSettings();
            s.model = "fixture-model"; s.reasoningEffort = "off"; s.optimizationPreset = "full"; s.maxTokensPerChunk = 80;
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            VolumePlan runPlan = rt.volumePlanFromSet(id);
            assertTrue("red=" + runPlan.red() + " included=" + runPlan.included() + " rows=" + runPlan.views().stream().map(v -> v.status + ":" + v.reason).collect(java.util.stream.Collectors.toList()), rt.startVolume(runPlan, s, null, null, outDir));
            long deadline = System.currentTimeMillis() + 60_000;
            Thread.sleep(200);
            while (rt.isRunning() && System.currentTimeMillis() < deadline) Thread.sleep(20);
            File[] sessions = rt.dataDir.resolve(ChapterSession.DIR).toFile().listFiles();
            assertNotNull(sessions);
            assertEquals(1, sessions.length);
            String json = new String(Files.readAllBytes(sessions[0].toPath()), StandardCharsets.UTF_8);
            assertTrue("session file carries the set id", json.contains(id));
        }
    }

    @Test public void deletingASetCanTakeTheRawThatOnlyBelongedToItAndSetsDoNotTravelInTheLibraryZip() throws Exception {
        try (DesktopRuntime rt = load("d")) {
            List<String> all = new ArrayList<>(allRaw(rt));
            DesktopSetEditor one = rt.newSet(all.subList(0, 2));
            one.setName("QA_a");
            String id1 = one.save();
            DesktopSetEditor two = rt.newSet(all.subList(1, 3));
            two.setName("QA_b");
            String id2 = two.save();
            assertNotNull(id1);
            assertNotNull(id2);
            assertEquals("only chapter 1 is in no other set", 1, rt.rawOnlyInSets(Arrays.asList(id1)).size());
            assertEquals(1, rt.deleteSets(Arrays.asList(id1), true));
            assertEquals(2, rt.raws().list().size());
            assertEquals(1, rt.setCards().size());
            assertEquals(0, rt.deleteSets(Arrays.asList(id2), false));
            assertEquals(2, rt.raws().list().size());
            assertTrue(rt.setCards().isEmpty());
            assertFalse("a set that was deleted opens as null", rt.openSet(id1) != null);
            Path zip = tmp.getRoot().toPath().resolve("lib.zip");
            rt.exportLibrary(zip);
            assertFalse("the library ZIP has no chapter sets", new String(Files.readAllBytes(zip), StandardCharsets.ISO_8859_1).contains("chapter_set"));
        }
    }

    private static List<String> allRaw(DesktopRuntime rt) {
        List<String> out = new ArrayList<>();
        for (DesktopRawStore.Item i : rt.raws().list()) out.add(i.id);
        return out;
    }

    private static List<String> allIds(List<DesktopSetEditor.Choice> choices) {
        List<String> out = new ArrayList<>();
        for (DesktopSetEditor.Choice c : choices) out.add(c.id);
        return out;
    }

    private static List<String> append(java.util.Set<String> ids, String id) {
        List<String> out = new ArrayList<>(ids);
        out.add(id);
        return out;
    }

    private static String firstByLabel(List<DesktopSetEditor.Choice> choices, String label) {
        for (DesktopSetEditor.Choice c : choices) if (c.label.equals(label)) return c.id;
        throw new AssertionError("no choice " + label);
    }
}
