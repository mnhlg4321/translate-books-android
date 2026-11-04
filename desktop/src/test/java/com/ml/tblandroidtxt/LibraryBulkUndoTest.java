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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * v4.25 PC Library lists: tick selection, the delete plan with its usage text, the undo that puts Glossary / Pronoun / Tập back exactly
 * as stored, the fallback of the active Glossary, and export names that never overwrite.
 */
public class LibraryBulkUndoTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private DesktopRuntime open() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        return DesktopRuntime.open(tmp.getRoot().toPath().resolve("data"), null);
    }

    private static GlossaryStoreCore.Glossary glossary(DesktopRuntime rt, String name, String source) {
        GlossaryStoreCore.Glossary g = rt.createGlossary(name);
        g.terms.add(new GlossaryStoreCore.Term(source, "đích " + source, "term", "ghi chú, có dấu phẩy"));
        g.terms.add(new GlossaryStoreCore.Term("sa\"o", "\"sao\"", "character", ""));
        rt.saveGlossary(g);
        return g;
    }

    private static PronounStoreCore.Profile pronoun(DesktopRuntime rt, String name) {
        PronounStoreCore.Profile p = new PronounStoreCore.Profile();
        p.name = name;
        p.text = "QA_xưng_hô_" + name + "\n";
        rt.savePronoun(p);
        return p;
    }

    private static void setWith(DesktopRuntime rt, String name, String glossaryId, String pronounId) {
        ChapterSetStoreCore.SetData set = new ChapterSetStoreCore.SetData();
        set.name = name;
        set.rows.add(new ChapterSetStoreCore.Row(1, "", glossaryId, pronounId));
        ChapterSetStoreCore.save(rt.db(), set);
    }

    @Test public void selectionTicksOnlyWhatIsShownAndClearsEverything() {
        DesktopLibraryBulk.Selection sel = new DesktopLibraryBulk.Selection();
        sel.toggle("hidden");
        sel.selectAll(Arrays.asList("a", "b"));
        assertEquals(3, sel.count());
        assertTrue(sel.isSelected("hidden"));
        sel.retainOnly(Arrays.asList("a", "hidden"));
        assertEquals(2, sel.count());
        sel.clear();
        assertEquals(0, sel.count());
        sel.toggle("a");
        assertEquals("1 đã chọn", sel.headline());
    }

    @Test public void takeAndPutBackKeepTheOriginalPlaces() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<DesktopLibraryBulk.Removed<String>> gone = DesktopLibraryBulk.take(list, Arrays.asList("b", "d"), s -> s);
        assertEquals(Arrays.asList("a", "c", "e"), list);
        DesktopLibraryBulk.putBack(list, gone, s -> s);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), list);
        DesktopLibraryBulk.putBack(list, gone, s -> s);
        assertEquals("putting back twice adds nothing", 5, list.size());
    }

    @Test public void exportNeverOverwritesAndAddsANumber() throws Exception {
        Path dir = tmp.getRoot().toPath().resolve("out");
        Path first = DesktopLibraryBulk.writeNew(dir, "QA_bo_thuat_ngu.csv", "một".getBytes(StandardCharsets.UTF_8));
        Path second = DesktopLibraryBulk.writeNew(dir, "QA_bo_thuat_ngu.csv", "hai".getBytes(StandardCharsets.UTF_8));
        assertEquals("QA_bo_thuat_ngu.csv", first.getFileName().toString());
        assertEquals("QA_bo_thuat_ngu (2).csv", second.getFileName().toString());
        assertEquals("một", new String(Files.readAllBytes(first), StandardCharsets.UTF_8));
        assertEquals("hai", new String(Files.readAllBytes(second), StandardCharsets.UTF_8));
        Path odd = DesktopLibraryBulk.writeNew(dir, "a/b:c*.txt", new byte[0]);
        assertEquals("a_b_c_.txt", odd.getFileName().toString());
    }

    @Test public void glossaryCsvQuotesWhatNeedsQuotes() throws Exception {
        GlossaryStoreCore.Glossary g = new GlossaryStoreCore.Glossary();
        g.terms.add(new GlossaryStoreCore.Term("a,b", "x\"y", "term", "n"));
        assertEquals("source,target,category,note\n\"a,b\",\"x\"\"y\",term,n\n", DesktopLibraryBulk.glossaryCsv(g));
    }

    @Test public void glossaryPlanNamesTheSetsThatUseItAndTheActiveOne() throws Exception {
        try (DesktopRuntime rt = open()) {
            GlossaryStoreCore.Glossary a = glossary(rt, "QA_A", "a");
            GlossaryStoreCore.Glossary b = glossary(rt, "QA_B", "b");
            setWith(rt, "QA_tap_dung_B", b.id, "");

            DesktopLibraryBulk.Plan plain = rt.glossaryPlan(Arrays.asList(a.id));
            assertEquals(1, plain.deletableCount());
            assertFalse("A is not in a set and not active", plain.needsSecondConfirm());

            DesktopLibraryBulk.Plan risky = rt.glossaryPlan(Arrays.asList(b.id));
            assertTrue(risky.needsSecondConfirm());
            assertTrue(risky.secondMessage().contains("QA_tap_dung_B"));

            rt.selectGlossary(a.id);
            assertTrue(rt.glossaryPlan(Arrays.asList(a.id)).secondMessage().contains("Active"));
        }
    }

    @Test public void undoBringsGlossariesBackByteForByteAndTheActiveOneToo() throws Exception {
        try (DesktopRuntime rt = open()) {
            GlossaryStoreCore.Glossary a = glossary(rt, "QA_A", "a");
            GlossaryStoreCore.Glossary b = glossary(rt, "QA_B", "b");
            glossary(rt, "QA_C", "c");
            rt.selectGlossary(b.id);
            String before = GlossaryStoreCore.serialize(rt.glossaries());

            DesktopLibraryBulk.Undo undo = rt.deleteGlossaries(Arrays.asList(a.id, b.id));
            assertEquals(2, undo.count());
            assertEquals("the active Glossary falls back to none", "", rt.selectedGlossaryId());
            assertEquals(1, rt.glossaries().size());

            assertEquals(2, rt.restoreDeleted(undo));
            assertEquals("ids, names, terms and places come back unchanged", before, GlossaryStoreCore.serialize(rt.glossaries()));
            assertEquals("the active Glossary is selected again", b.id, rt.selectedGlossaryId());
        }
    }

    @Test public void deletingAnUnknownGlossaryRemovesNothing() throws Exception {
        try (DesktopRuntime rt = open()) {
            glossary(rt, "QA_A", "a");
            DesktopLibraryBulk.Undo undo = rt.deleteGlossaries(Arrays.asList("khong-co"));
            assertEquals(0, undo.count());
            assertEquals(1, rt.glossaries().size());
        }
    }

    @Test public void undoBringsPronounsBackByteForByteAndTheActiveOneToo() throws Exception {
        try (DesktopRuntime rt = open()) {
            PronounStoreCore.Profile p1 = pronoun(rt, "QA_P1");
            PronounStoreCore.Profile p2 = pronoun(rt, "QA_P2");
            rt.selectPronoun(p1.id);
            String before = PronounStoreCore.serialize(rt.pronouns());

            DesktopLibraryBulk.Undo undo = rt.deletePronouns(Arrays.asList(p1.id, p2.id));
            assertEquals("", rt.selectedPronounId());
            assertEquals(0, rt.pronouns().size());
            assertEquals(2, rt.restoreDeleted(undo));
            assertEquals(before, PronounStoreCore.serialize(rt.pronouns()));
            assertEquals(p1.id, rt.selectedPronounId());
        }
    }

    @Test public void undoBringsASetBackUnchanged() throws Exception {
        try (DesktopRuntime rt = open()) {
            GlossaryStoreCore.Glossary g = glossary(rt, "QA_G", "g");
            PronounStoreCore.Profile p = pronoun(rt, "QA_P");
            setWith(rt, "QA_tap", g.id, p.id);
            ChapterSetStoreCore.SetData stored = ChapterSetStoreCore.get(rt.db(), rt.setCards().get(0).id);

            DesktopLibraryBulk.Undo undo = rt.deleteSetsUndoable(Arrays.asList(stored.id), false);
            assertTrue(rt.setCards().isEmpty());
            assertEquals(1, rt.restoreDeleted(undo));

            ChapterSetStoreCore.SetData back = ChapterSetStoreCore.get(rt.db(), stored.id);
            assertEquals(stored.name, back.name);
            assertEquals("updated_at is kept, not stamped again", stored.updatedAt, back.updatedAt);
            assertEquals(1, back.rows.size());
            assertEquals(g.id, back.rows.get(0).glossaryId);
            assertEquals(p.id, back.rows.get(0).pronounId);
            assertEquals(1, back.rows.get(0).chapterNo);
        }
    }

    @Test public void setPlanListsRawThatOnlyTheSetUses() throws Exception {
        try (DesktopRuntime rt = open()) {
            setWith(rt, "QA_tap", "", "");
            String setId = rt.setCards().get(0).id;
            assertEquals(Arrays.asList(), rt.setPlan(Arrays.asList(setId)).rawOnlyIds());
            assertEquals(1, rt.setPlan(Arrays.asList(setId)).deletableCount());
        }
    }
}
