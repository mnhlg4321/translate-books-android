package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

/** The chain DesktopLiveVolume uses (folder import, newSet, slots, save, volumePlanFromSet, bindings) must bind each chapter to its own Glossary and Pronoun. */
public class SetGlossaryBindingTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private Path raw, glossary, pronoun;

    @Before public void setUp() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        Path root = tmp.getRoot().toPath();
        raw = Files.createDirectories(root.resolve("raw"));
        glossary = Files.createDirectories(root.resolve("glossary"));
        pronoun = Files.createDirectories(root.resolve("pronoun"));
        for (int n = 1; n <= 2; n++) write(raw.resolve("00" + n + "_QA_VOL_chuong.txt"), ("魔王甲" + n + "は歩いた。" + "日".repeat(120) + "\n\n").repeat(3));
        write(glossary.resolve("001_QA_VOL_glossary.csv"), "source,target,category,note\n魔王甲,Ma Vương Giáp,character,\n");
        write(glossary.resolve("002_QA_VOL_glossary.csv"), "source,target,category,note\n魔王甲,Ma Vương Giáp,character,\n銀の森,Rừng Bạc,place,\n");
        write(pronoun.resolve("001_QA_VOL_pronoun.csv"), "from,speaker,target,self,call,scope,note\n勇者乙,勇者乙,魔王甲,俺,お前,*,QA\n");
        write(pronoun.resolve("002_QA_VOL_pronoun.csv"), "from,speaker,target,self,call,scope,note\n魔王甲,魔王甲,勇者乙,我,貴様,*,QA\n");
    }

    private static void write(Path f, String text) throws Exception { Files.write(f, text.getBytes(StandardCharsets.UTF_8)); }

    private DesktopRuntime load() throws Exception {
        DesktopRuntime rt = DesktopRuntime.open(tmp.getRoot().toPath().resolve("d"), null);
        rt.importRawFolder(raw);
        rt.importGlossaryFolder(glossary);
        rt.importPronounFolder(pronoun);
        return rt;
    }

    private static List<String> rawIds(DesktopRuntime rt) {
        List<String> out = new ArrayList<>();
        for (DesktopRawStore.Item i : rt.raws().list()) out.add(i.id);
        return out;
    }

    private static List<String> ids(List<DesktopSetEditor.Choice> c) {
        List<String> out = new ArrayList<>();
        for (DesktopSetEditor.Choice x : c) out.add(x.id);
        return out;
    }

    @Test public void theLiveCheckChainBindsEachChapterToItsOwnFiles() throws Exception {
        try (DesktopRuntime rt = load()) {
            DesktopSetEditor ed = rt.newSet(rawIds(rt));
            ed.setGlossarySlot(ids(ed.glossaryChoices()));
            ed.setPronounSlot(ids(ed.pronounChoices()));
            assertEquals("001_QA_VOL_glossary", ed.rows().get(0).glossaryLabel);
            assertEquals("002_QA_VOL_glossary", ed.rows().get(1).glossaryLabel);
            ed.setName("QA_VOL_tap");
            String id = ed.save();
            assertTrue(id != null);
            VolumePlan plan = rt.volumePlanFromSet(id);
            assertEquals(2, plan.bindings().size());
            ChapterBinding b1 = plan.bindings().get(0), b2 = plan.bindings().get(1);
            assertFalse("chapter 1 has Glossary text", b1.glossaryText.isEmpty());
            assertFalse("chapter 2 has Glossary text", b2.glossaryText.isEmpty());
            assertFalse(b1.pronounText.isEmpty());
            assertFalse(b2.pronounText.isEmpty());
            assertFalse("different Glossary files, different hashes", b1.glossaryHash.equals(b2.glossaryHash));
        }
    }

    @Test public void anEmptySlotIsReportedAndUseMatchingBindsEveryChapterToItsOwnFiles() throws Exception {
        try (DesktopRuntime rt = load()) {
            DesktopSetEditor ed = rt.newSet(rawIds(rt));
            assertEquals("both slots left on Không dùng: still a valid set", 2, ed.included());
            assertNull(ed.blockReason());
            assertEquals(2, ed.emptySlotHints().size());
            String id = ed.save();
            for (VolumePlan.BindingInfo b : rt.volumePlanFromSet(id).bindingSummary(new AppSettings())) {
                assertTrue(b.glossaryEmpty());
                assertTrue(b.pronounEmpty());
            }
            ed.useMatching(true);
            ed.useMatching(false);
            assertTrue(ed.emptySlotHints().isEmpty());
            assertNotNull(ed.save());
            java.util.List<VolumePlan.BindingInfo> infos = rt.volumePlanFromSet(id).bindingSummary(new AppSettings());
            assertEquals(2, infos.size());
            for (VolumePlan.BindingInfo b : infos) {
                assertFalse(b.glossaryEmpty());
                assertFalse(b.pronounEmpty());
                assertTrue(b.glossaryEntries > 0);
                assertTrue(b.pronounRules > 0);
            }
            assertEquals(1, infos.get(0).glossaryEntries);
            assertEquals(2, infos.get(1).glossaryEntries);
            assertFalse("different files give different settings hashes", infos.get(0).settingsHash.equals(infos.get(1).settingsHash));
        }
    }

    @Test public void rowChoicesNoneOnPurposeAndReopenThenSaveKeepTheBindings() throws Exception {
        try (DesktopRuntime rt = load()) {
            DesktopSetEditor ed = rt.newSet(rawIds(rt));
            ed.setGlossarySlot(ids(ed.glossaryChoices()));
            ed.setPronounSlot(ids(ed.pronounChoices()));
            // chapter 1 takes the Glossary of chapter 2 by choice; chapter 2 is set to Không dùng on purpose
            String g2 = null;
            for (DesktopSetEditor.Choice c : ed.rowChoices(0, true)) if (c.label.startsWith("002")) g2 = c.id;
            ed.chooseGlossary(0, g2);
            ed.chooseGlossary(1, "");
            ed.choosePronoun(1, "");
            assertEquals("Không dùng", ed.rows().get(1).glossaryLabel);
            assertEquals("a deliberate Không dùng is green, not yellow", "GREEN", ed.rows().get(1).status);
            String id = ed.save();
            assertNotNull(id);
            DesktopSetEditor reopened = rt.openSet(id);
            assertEquals("002_QA_VOL_glossary", reopened.rows().get(0).glossaryLabel);
            assertEquals("001_QA_VOL_pronoun", reopened.rows().get(0).pronounLabel);
            assertEquals("Không dùng", reopened.rows().get(1).glossaryLabel);
            assertNotNull("saving again changes nothing", reopened.save());
            java.util.List<VolumePlan.BindingInfo> infos = rt.volumePlanFromSet(id).bindingSummary(new AppSettings());
            assertEquals(2, infos.get(0).glossaryEntries);
            assertFalse(infos.get(0).glossaryEmpty());
            assertTrue(infos.get(1).glossaryEmpty());
            assertTrue(infos.get(1).pronounEmpty());
        }
    }

    @Test public void aSetMadeFromOneRawOfTheRawPageBindsItToo() throws Exception {
        try (DesktopRuntime rt = load()) {
            List<String> one = new ArrayList<>();
            one.add(rawIds(rt).get(0));
            DesktopSetEditor ed = rt.newSet(one);
            ed.setGlossarySlot(ed.matching(true));
            ed.setPronounSlot(ed.matching(false));
            String id = ed.save();
            VolumePlan.BindingInfo b = rt.volumePlanFromSet(id).bindingSummary(new AppSettings()).get(0);
            assertFalse(b.glossaryEmpty());
            assertTrue(b.glossaryEntries > 0);
            assertTrue(b.pronounRules > 0);
        }
    }
}
