package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v4.23 L4: each chapter translates with its own Glossary/Pronoun from a per-Start snapshot; the shared settings never change. */
public class ChapterRunTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private static GlossaryStore.Glossary glossary(String id, String name, String source, String target) {
        GlossaryStore.Glossary g = new GlossaryStore.Glossary();
        g.id = id; g.name = name;
        g.terms.add(new GlossaryStore.Term(source, target, "character"));
        return g;
    }

    private static PronounStore.Profile pronoun(String id, String name, String text) {
        PronounStore.Profile p = new PronounStore.Profile();
        p.id = id; p.name = name; p.text = text;
        return p;
    }

    private static AppSettings base() {
        AppSettings s = new AppSettings();
        s.glossaryText = "ACTIVE GLOSSARY TEXT";
        s.selectedGlossaryId = "active-g"; s.selectedGlossaryName = "active";
        s.pronounText = "ACTIVE PRONOUN TEXT"; s.pronounName = "active-p"; s.selectedPronounId = "active-p-id"; s.pronounUri = "content://x";
        return s;
    }

    private static List<ChapterBinding> three() {
        return Arrays.asList(
                ChapterBinding.of("r1", "001.txt", 1, glossary("g1", "001_g", "花子", "Hanako"), pronoun("p1", "001_p", "from,speaker,target,self,call,scope,note\nA,,B,tôi,cậu,,")),
                ChapterBinding.of("r2", "002.txt", 2, glossary("g2", "002_g", "太郎", "Taro"), null),
                ChapterBinding.of("r3", "003.txt", 3, null, null));
    }

    @Test public void eachChapterGetsItsOwnTextsAndTheBaseIsUntouched() {
        AppSettings base = base();
        List<ChapterBinding> b = three();
        AppSettings s1 = b.get(0).applyTo(base), s2 = b.get(1).applyTo(base), s3 = b.get(2).applyTo(base);
        assertTrue(s1.glossaryText.contains("花子") && s1.glossaryText.contains("Hanako"));
        assertTrue(s2.glossaryText.contains("太郎"));
        assertFalse("chapter 2 does not see chapter 1's term", s2.glossaryText.contains("花子"));
        assertEquals("no profile means no text, never the Active one", "", s3.glossaryText);
        assertEquals("", s3.pronounText);
        assertEquals("g1", s1.selectedGlossaryId);
        assertEquals("p1", s1.selectedPronounId);
        assertEquals("", s2.pronounText);
        assertEquals("", s1.pronounUri);
        assertNotEquals("jobs.settings_hash differs per chapter", HashUtil.settingsHash(s1), HashUtil.settingsHash(s2));
        assertEquals("ACTIVE GLOSSARY TEXT", base.glossaryText);
        assertEquals("active-g", base.selectedGlossaryId);
        assertEquals("ACTIVE PRONOUN TEXT", base.pronounText);
        assertEquals("content://x", base.pronounUri);
        assertEquals("the saved settings JSON of the base is unchanged", SettingsStore.toJson(base()), SettingsStore.toJson(base));
    }

    @Test public void sessionSnapshotRoundTripsAndChecksItself() throws Exception {
        File dir = new File(temp.getRoot(), ChapterSession.DIR);
        ChapterSession s = new ChapterSession("11111111-2222");
        s.setId = "set-1"; s.setName = "Kumo";
        s.chapters.addAll(three());
        s.write(dir);
        ChapterSession back = ChapterSession.read(dir, "11111111-2222");
        assertEquals(3, back.chapters.size());
        assertEquals("Kumo", back.setName);
        assertEquals(s.chapters.get(0).glossaryText, back.chapters.get(0).glossaryText);
        assertEquals(s.chapters.get(1).keyPart(), back.chapters.get(1).keyPart());
        back.requireMatches(Arrays.asList("tblraw://r1", "tblraw://r2", "tblraw://r3"));
        try { back.requireMatches(Arrays.asList("tblraw://r1", "tblraw://r3", "tblraw://r2")); fail("order must match"); } catch (IllegalStateException expected) { }
        try { back.requireMatches(Arrays.asList("tblraw://r1")); fail("count must match"); } catch (IllegalStateException expected) { }
        try { ChapterSession.read(dir, "no-such-session"); fail("missing snapshot"); } catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("Thiếu ảnh chụp")); }
        try { ChapterSession.read(dir, "../escape"); fail("path escape"); } catch (java.io.IOException expected) { }
        File file = ChapterSession.file(dir, "11111111-2222");
        String damaged = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).replace("Hanako", "Hanaco");
        Files.write(file.toPath(), damaged.getBytes(StandardCharsets.UTF_8));
        try { ChapterSession.read(dir, "11111111-2222"); fail("damaged text must be refused"); } catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("hỏng")); }
    }

    @Test public void snapshotKeepsItsTextsEvenIfTheLibraryChangesAfterStart() throws Exception {
        GlossaryStore.Glossary g = glossary("g1", "001_g", "花子", "Hanako");
        ChapterBinding at = ChapterBinding.of("r1", "001.txt", 1, g, null);
        g.terms.add(new GlossaryStore.Term("次郎", "Jiro", "character"));
        assertFalse("a later edit of the profile is not in the snapshot", at.glossaryText.contains("Jiro"));
        assertNotEquals("but a fresh binding differs, so the plan key changes", at.glossaryHash, ChapterBinding.of("r1", "001.txt", 1, g, null).glossaryHash);
    }

    @Test public void planKeyIncludesEachInputsGlossaryAndPronounHashes() {
        List<String> identities = Arrays.asList("tblraw://r1:h1:10", "tblraw://r2:h2:20");
        String construction = "construct";
        String plain = PreparationCoordinator.selectionKeyOf(construction, identities, null);
        assertEquals("without bindings the key is the one-profile key", HashUtil.sha256(construction + "\n" + identities.get(0) + "\n" + identities.get(1)), plain);
        List<ChapterBinding> a = Arrays.asList(
                ChapterBinding.of("r1", "1", 1, glossary("g", "g", "A", "a"), null), ChapterBinding.of("r2", "2", 2, null, null));
        List<ChapterBinding> sameRawOtherGlossary = Arrays.asList(
                ChapterBinding.of("r1", "1", 1, glossary("g", "g", "A", "CHANGED"), null), ChapterBinding.of("r2", "2", 2, null, null));
        String ka = PreparationCoordinator.selectionKeyOf(construction, identities, a);
        assertNotEquals("two sets that share RAW but not the glossary never share a plan", ka, PreparationCoordinator.selectionKeyOf(construction, identities, sameRawOtherGlossary));
        assertNotEquals(plain, ka);
        assertEquals(ka, PreparationCoordinator.selectionKeyOf(construction, identities, a));
        AppSettings settings = base();
        assertNotEquals(PreparationCoordinator.estimateKey(settings, construction, null), PreparationCoordinator.estimateKey(settings, construction, a));
        assertEquals("a chapter input is estimated with its own texts", a.get(0).glossaryText, PreparationCoordinator.settingsFor(settings, a, 0).glossaryText);
        assertEquals("out-of-range falls back to the shared settings", settings.glossaryText, PreparationCoordinator.settingsFor(settings, a, 5).glossaryText);
    }

    @Test public void onlyChapterLocalFailuresLetTheQueueContinue() {
        assertTrue(ChapterRunOutcome.continuesQueue(new ChapterLocalException("Final integrity audit failed")));
        assertTrue(ChapterRunOutcome.continuesQueue(new ChapterLocalException("split", new RuntimeException("context"))));
        assertFalse(ChapterRunOutcome.continuesQueue(new IllegalStateException("Final integrity audit failed")));
        assertFalse(ChapterRunOutcome.continuesQueue(new java.io.IOException("network")));
        assertFalse(ChapterRunOutcome.continuesQueue(new TranslationEngine.DeliveryUnknownException("unknown", null)));
        assertFalse(ChapterRunOutcome.continuesQueue(new RuntimeException("cost limit reached")));
        assertFalse(ChapterRunOutcome.continuesQueue(new RuntimeException("HTTP 401")));
    }

    @Test public void closingLineCountsDoneFailedAndNotRun() {
        ChapterRunOutcome o = new ChapterRunOutcome(5);
        o.done("001.txt");
        o.failed("002.txt", "Final integrity audit failed: line count");
        o.done("003.txt");
        assertEquals(2, o.doneCount());
        assertEquals(1, o.failedCount());
        assertEquals(2, o.notRun());
        assertEquals("Tổ hợp: 2 xong · 1 lỗi (002.txt: Final integrity audit failed: line count) · 2 chưa chạy", o.summary());
        ChapterRunOutcome all = new ChapterRunOutcome(2);
        all.done("a"); all.done("b");
        assertEquals("Tổ hợp: 2 xong · 0 lỗi · 0 chưa chạy", all.summary());
    }
}
