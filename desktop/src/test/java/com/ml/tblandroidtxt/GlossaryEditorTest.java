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
import java.util.Arrays;

/** T1: edit a Glossary (add, edit, delete rows, save, reopen) and move the library to another PC data folder through the library ZIP. */
public class GlossaryEditorTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private DesktopRuntime open(String name) throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        return DesktopRuntime.open(tmp.getRoot().toPath().resolve(name), null);
    }

    private Path write(String name, String text) throws Exception {
        Path f = tmp.getRoot().toPath().resolve(name);
        Files.write(f, text.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test public void addEditDeleteSaveReopenGivesTheSameTerms() throws Exception {
        try (DesktopRuntime rt = open("a")) {
            GlossaryStoreCore.Glossary g = rt.createGlossary("QA_gl");
            DesktopGlossaryEditor ed = DesktopGlossaryEditor.open(g);
            assertFalse(ed.dirty());
            ed.addTerm("魔王", "Ma vương", "character", "QA ghi chú");
            ed.addTerm("城", "thành", "term", "");
            ed.addTerm("剣", "kiếm", "term", "");
            assertTrue(ed.dirty());
            assertEquals("rows are added at the top", "剣", ed.field(0, DesktopGlossaryEditor.SOURCE));
            ed.setField(1, DesktopGlossaryEditor.TARGET, "tường thành");
            ed.removeRows(Arrays.asList(0));
            ed.setName("QA_gl_doi_ten");
            ed.setSourceLang("Chinese");
            ed.setTargetLang("Vietnamese");
            assertTrue(ed.save(rt));
            assertFalse("saving clears the unsaved mark", ed.dirty());

            GlossaryStoreCore.Glossary again = rt.glossary(g.id);
            assertEquals("QA_gl_doi_ten", again.name);
            assertEquals("Chinese", again.sourceLang);
            assertEquals(2, again.count());
            assertEquals("城", again.terms.get(0).source);
            assertEquals("tường thành", again.terms.get(0).target);
            assertEquals("魔王", again.terms.get(1).source);
            assertEquals("QA ghi chú", again.terms.get(1).note);
            assertFalse(DesktopGlossaryEditor.open(again).dirty());
        }
    }

    @Test public void filterMatchesSourceTargetAndCategory() throws Exception {
        try (DesktopRuntime rt = open("f")) {
            DesktopGlossaryEditor ed = DesktopGlossaryEditor.open(rt.createGlossary("QA_f"));
            ed.addTerm("魔王", "Ma vương", "character", "");
            ed.addTerm("城", "thành Ma", "term", "");
            ed.addTerm("剣", "kiếm", "item", "");
            assertEquals(3, ed.visible("", "").size());
            assertEquals("target also matches, case-insensitive", 2, ed.visible("MA", "").size());
            assertEquals(1, ed.visible("ma", "character").size());
            assertEquals(1, ed.visible("", "item").size());
            assertEquals(Arrays.asList("character", "item", "term"), ed.categories());
        }
    }

    @Test public void saveDropsOnlyFullyEmptyRowsAndKeepsPriorityColumnOutOfTheStoredFormat() throws Exception {
        try (DesktopRuntime rt = open("e")) {
            GlossaryStoreCore.Glossary g = rt.createGlossary("QA_e");
            DesktopGlossaryEditor ed = DesktopGlossaryEditor.open(g);
            ed.addTerm("", "", "term", "");
            ed.addTerm("甲", "", "term", "");
            assertTrue(ed.save(rt));
            assertEquals(1, rt.glossary(g.id).count());
            Path csv = write("QA_prio.csv", "source,target,category,note,priority\n乙,Ất,term,,9\n");
            int[] r = ed.mergeFiles(Arrays.asList(csv));
            assertEquals(1, r[1]);
            assertTrue(ed.save(rt));
            org.json.JSONObject o = GlossaryStoreCore.toJson(rt.glossary(g.id));
            assertFalse("priority is not stored", o.getJSONArray("terms").getJSONObject(0).has("priority"));
        }
    }

    @Test public void healthAndPreviewUseTheSharedParserAndPromptText() throws Exception {
        try (DesktopRuntime rt = open("h")) {
            DesktopGlossaryEditor ed = DesktopGlossaryEditor.open(rt.createGlossary("QA_h"));
            ed.addTerm("魔王", "Ma vương", "character", "");
            ed.addTerm("魔王", "Quỷ vương", "character", "");
            String health = ed.healthText("");
            assertTrue(health.startsWith("Glossary terms: 2"));
            assertTrue("a source with two targets is reported", health.contains("Conflicts"));
            assertTrue(health.contains("Priority rule:"));
            assertTrue(ed.previewText().startsWith("# Glossary: QA_h"));
            assertTrue(ed.previewText().contains("魔王 => Ma vương [character]"));
        }
    }

    @Test public void exportingTheLibraryAndImportingItElsewhereKeepsEveryCount() throws Exception {
        try (DesktopRuntime a = open("src"); DesktopRuntime b = open("dst")) {
            GlossaryStoreCore.Glossary g = a.createGlossary("QA_gl1");
            DesktopGlossaryEditor ed = DesktopGlossaryEditor.open(g);
            ed.addTerm("魔王", "Ma vương", "character", "");
            ed.addTerm("城", "thành", "term", "ghi chú");
            ed.save(a);
            a.importPronouns(Arrays.asList(write("QA_pr.txt", "from,speaker,target,self,call,scope,note\nA,A,B,tao,mày,*,\n")));
            Path zip = tmp.getRoot().toPath().resolve("lib.zip");
            assertEquals(1, a.exportLibrary(zip)[0]);
            DesktopRuntime.LibraryPreview p = b.previewLibrary(zip);
            assertTrue(b.applyLibrary(p));
            assertEquals(1, b.glossaries().size());
            assertEquals(2, b.glossaries().get(0).count());
            assertEquals(1, b.pronouns().size());
            assertEquals(1, b.pronouns().get(0).count());
        }
    }
}
