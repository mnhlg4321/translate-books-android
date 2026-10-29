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

/** R0: after importing a phone library into a PC with nothing chosen, the Glossary and Pronoun the phone used become the chosen ones; a choice already made is kept. */
public class LibraryActiveAdoptTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static final String GL = "source,target,category,note\n";
    private static final String PR = "from,speaker,target,self,call,scope,note\n";

    private Path write(String name, String text) throws Exception {
        Path f = tmp.getRoot().toPath().resolve(name);
        Files.write(f, text.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test public void theRecordedActiveFilesAreChosenOnlyWhenNothingIsChosen() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        try (DesktopRuntime phone = DesktopRuntime.open(tmp.getRoot().toPath().resolve("phone"), null);
             DesktopRuntime fresh = DesktopRuntime.open(tmp.getRoot().toPath().resolve("fresh"), null);
             DesktopRuntime chosen = DesktopRuntime.open(tmp.getRoot().toPath().resolve("chosen"), null)) {
            phone.importGlossaries(Arrays.asList(write("001_QA_gl.csv", GL + "魔王,Ma vương,character,\n"), write("002_QA_gl.csv", GL + "城,thành,term,\n")));
            phone.importPronouns(Arrays.asList(write("001_QA_pr.csv", PR + "A,A,B,tao,mày,*,\n")));
            String activeG = null;
            for (GlossaryStoreCore.Glossary g : phone.glossaries()) if (g.name.equals("002_QA_gl")) activeG = g.id;
            phone.selectGlossary(activeG);
            phone.selectPronoun(phone.pronouns().get(0).id);
            Path zip = tmp.getRoot().toPath().resolve("lib.zip");
            phone.exportLibrary(zip);

            assertTrue(fresh.selectedGlossaryId().isEmpty());
            assertTrue(fresh.applyLibrary(fresh.previewLibrary(zip)));
            assertEquals(activeG, fresh.selectedGlossaryId());
            assertEquals(phone.pronouns().get(0).id, fresh.selectedPronounId());

            chosen.importGlossaries(Arrays.asList(write("003_QA_own.csv", GL + "剣,kiếm,term,\n")));
            String own = chosen.glossaries().get(0).id;
            chosen.selectGlossary(own);
            assertTrue(chosen.applyLibrary(chosen.previewLibrary(zip)));
            assertEquals("a choice already made is kept", own, chosen.selectedGlossaryId());
            assertFalse("the Pronoun was empty, so it is adopted", chosen.selectedPronounId().isEmpty());
        }
    }
}
