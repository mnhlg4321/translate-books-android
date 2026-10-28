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

/** T2: Pronoun files in the 7-column and the old 3-column format are opened, edited as text, saved, and keep their format. */
public class PronounEditorTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static final String SEVEN = "from,speaker,target,self,call,scope,note\nA,A,B,tao,mày,*,QA ghi chú\nB,B,A,tôi,anh,*,\n";
    private static final String THREE = "Alice,Bob,gọi nhau là anh em\nBob,Alice,gọi nhau là anh em\n";

    private DesktopRuntime open(String name) throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        return DesktopRuntime.open(tmp.getRoot().toPath().resolve(name), null);
    }

    private Path write(String name, String text) throws Exception {
        Path f = tmp.getRoot().toPath().resolve(name);
        Files.write(f, text.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test public void sevenColumnFileShowsSevenColumnsAndSavingAnEditKeepsTheFormat() throws Exception {
        try (DesktopRuntime rt = open("a")) {
            assertEquals(1, rt.importPronounFiles(Arrays.asList(write("QA_p7.csv", SEVEN))).imported);
            PronounStoreCore.Profile p = rt.pronouns().get(0);
            DesktopPronounEditor ed = DesktopPronounEditor.open(p);
            assertEquals(7, ed.columns().length);
            assertFalse(ed.legacyOnly());
            assertEquals(2, ed.rows().size());
            assertEquals("tao", ed.rows().get(0)[3]);
            assertEquals("QA ghi chú", ed.rows().get(0)[6]);
            assertEquals("2 quy tắc Pronoun • Hợp lệ", ed.statusLine());

            String edited = SEVEN.replace("tao,mày", "tớ,cậu");
            ed.setText(edited);
            assertTrue(ed.dirty());
            assertTrue(ed.save(rt));
            assertFalse(ed.dirty());
            PronounStoreCore.Profile again = rt.pronoun(p.id);
            assertEquals("the text is stored exactly as typed", edited, again.text);
            assertEquals("tớ", DesktopPronounEditor.open(again).rows().get(0)[3]);
        }
    }

    @Test public void threeColumnFileStaysThreeColumnsAfterAnEdit() throws Exception {
        try (DesktopRuntime rt = open("b")) {
            rt.importPronounFiles(Arrays.asList(write("QA_p3.csv", THREE)));
            PronounStoreCore.Profile p = rt.pronouns().get(0);
            DesktopPronounEditor ed = DesktopPronounEditor.open(p);
            assertTrue(ed.legacyOnly());
            assertEquals(3, ed.columns().length);
            assertEquals("Alice", ed.rows().get(0)[0]);
            String edited = THREE.replace("anh em", "chị em");
            ed.setText(edited);
            assertTrue(ed.save(rt));
            PronounStoreCore.Profile again = rt.pronoun(p.id);
            assertEquals(edited, again.text);
            DesktopPronounEditor reopened = DesktopPronounEditor.open(again);
            assertTrue("still the old 3-column format", reopened.legacyOnly());
            assertEquals(3, reopened.columns().length);
            assertEquals("gọi nhau là chị em", reopened.rows().get(0)[2]);
        }
    }

    @Test public void malformedRowsShowAsWarningsAndAFileWithNoValidRuleIsNotImported() throws Exception {
        try (DesktopRuntime rt = open("c")) {
            DesktopRuntime.PronounImport r = rt.importPronounFiles(Arrays.asList(write("QA_bad.csv", "chỉ có chữ tự do, không có quy tắc\n")));
            assertEquals(0, r.imported);
            assertEquals(1, r.failed.size());
            assertTrue(r.failed.get(0).contains("No valid pronoun rules"));
            assertTrue(rt.pronouns().isEmpty());

            rt.importPronounFiles(Arrays.asList(write("QA_ok.csv", SEVEN)));
            DesktopPronounEditor ed = DesktopPronounEditor.open(rt.pronouns().get(0));
            ed.setText(SEVEN + "X,Y\n");
            assertTrue("a short row is reported in the editor", ed.warningCount() >= 1);
            assertTrue(ed.statusLine().contains("Cảnh báo"));
            String health = ed.healthText("");
            assertTrue(health.contains("Pronoun rules:"));
            assertTrue(health.contains("Priority rule:"));
        }
    }

    @Test public void replaceFileKeepsIdSelectsTheProfileAndRefusesAnInvalidFile() throws Exception {
        try (DesktopRuntime rt = open("d")) {
            rt.importPronounFiles(Arrays.asList(write("QA_p.csv", SEVEN)));
            String id = rt.pronouns().get(0).id;
            assertEquals(null, rt.replacePronounFile(id, write("QA_new.csv", THREE)));
            assertEquals(id, rt.selectedPronounId());
            assertEquals(THREE, rt.pronoun(id).text);
            assertEquals("the name stays as it was", "QA_p", rt.pronoun(id).name);
            assertTrue(rt.replacePronounFile(id, write("QA_junk.csv", "chỉ có chữ tự do, không có quy tắc\n")).contains("No valid pronoun rules"));
            assertEquals(THREE, rt.pronoun(id).text);
            assertTrue(rt.renamePronoun(id, "QA_doi_ten"));
            assertFalse(rt.renamePronoun(id, "  "));
            assertEquals("QA_doi_ten", rt.pronoun(id).name);
        }
    }
}
