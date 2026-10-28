package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Settings profile and library ZIP moving between phone and PC, with self-written QA_ data only. */
public class ProfileAndLibraryTransferTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static boolean windows() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }

    private DesktopRuntime open(String name) throws Exception {
        return DesktopRuntime.open(tmp.getRoot().toPath().resolve(name), null);
    }

    private Path write(String name, String text) throws Exception {
        Path f = tmp.getRoot().toPath().resolve(name);
        Files.write(f, text.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test public void exportedProfileImportsBackIdenticalAndNeverCarriesTheKeyOrPrivateFields() throws Exception {
        assumeTrue(windows());
        try (DesktopRuntime a = open("a"); DesktopRuntime b = open("b")) {
            AppSettings s = a.loadSettings();
            s.model = "QA_model"; s.targetLanguage = "Vietnamese"; s.reasoningEffort = "medium"; s.costLimitUsd = 0.5; s.maxAttempts = 4;
            s.apiKey = "QA_FAKE_KEY";
            s.outputTreeUri = "content://phone/tree"; s.envUri = "content://phone/env";
            a.saveSettings(s);
            String json = a.exportProfileJson();
            JSONObject o = new JSONObject(json);
            assertFalse(o.has("apiKey"));
            for (String forbidden : new String[]{"editorial", "instructionUri", "envUri", "outputUri", "outputTreeUri", "outputName", "outputTreeName"}) {
                assertFalse(forbidden, o.has(forbidden));
            }
            assertFalse(json.contains("QA_FAKE_KEY"));
            DesktopRuntime.ProfilePreview preview = b.previewProfile(json);
            assertFalse("some shared setting differs", preview.differences.isEmpty());
            b.applyProfile(preview);
            assertEquals(json, b.exportProfileJson());
            assertTrue(b.previewProfile(json).differences.isEmpty());
        }
    }

    @Test public void applyingAProfileCanBeUndone() throws Exception {
        assumeTrue(windows());
        try (DesktopRuntime rt = open("u")) {
            AppSettings s = rt.loadSettings();
            s.model = "QA_before";
            rt.saveSettings(s);
            AppSettings other = s.copy();
            other.model = "QA_after";
            DesktopRuntime.ProfilePreview p = rt.previewProfile(SettingsJson.toExportJson(other));
            assertEquals(1, p.differences.size());
            assertEquals("model", p.differences.get(0)[0]);
            rt.applyProfile(p);
            assertEquals("QA_after", rt.loadSettings().model);
            assertTrue(rt.canUndoProfile());
            rt.undoProfile();
            assertEquals("QA_before", rt.loadSettings().model);
            assertFalse(rt.canUndoProfile());
        }
    }

    @Test public void aProfileFromThePcLeavesPhoneOnlyFieldsAndTheKeyUntouched() throws Exception {
        AppSettings phone = new AppSettings();
        phone.apiKey = "QA_PHONE_KEY";
        phone.outputTreeUri = "content://phone/tree";
        phone.outputTreeName = "Truyen";
        phone.envUri = "content://phone/env";
        phone.instructionUri = "content://phone/instr";
        phone.editorial.model = "QA_editorial_phone";
        phone.model = "QA_old";
        AppSettings pc = new AppSettings();
        pc.model = "QA_pc_model";
        pc.outputTreeUri = "file:///C:/pc/out";
        pc.editorial.model = "QA_editorial_pc";
        pc.apiKey = "QA_PC_KEY";
        AppSettings merged = SettingsJson.fromImportedProfile(SettingsJson.toExportJson(pc), phone);
        assertEquals("QA_pc_model", merged.model);
        assertEquals("QA_PHONE_KEY", merged.apiKey);
        assertEquals("content://phone/tree", merged.outputTreeUri);
        assertEquals("Truyen", merged.outputTreeName);
        assertEquals("content://phone/env", merged.envUri);
        assertEquals("content://phone/instr", merged.instructionUri);
        assertEquals("QA_editorial_phone", merged.editorial.model);
        assertEquals("an older profile that still carries a key imports it, as the phone always did", "QA_OTHER",
                SettingsJson.fromImportedProfile("{\"apiKey\":\"QA_OTHER\",\"model\":\"m\"}", phone).apiKey);
    }

    @Test public void aSelfBuiltArchiveImportsAndAnExportReadsBackTheSame() throws Exception {
        assumeTrue(windows());
        try (DesktopRuntime src = open("src"); DesktopRuntime dst = open("dst")) {
            src.importGlossaries(Arrays.asList(write("QA_gl.csv", "source,target,category,note\n魔王,Ma vương,character,\n城,thành,term,\n")));
            src.importPronouns(Arrays.asList(write("QA_pr.txt", "from,speaker,target,self,call,scope,note\nA,A,B,tao,mày,*,\n")));
            src.raws().add(Arrays.asList(
                    new DesktopRawStore.Picked("001_QA_a.txt", "QA dòng một.\nQA dòng hai.\n".repeat(5), "UTF-8"),
                    new DesktopRawStore.Picked("002_QA_b.txt", "QA nội dung khác.\n".repeat(7), "UTF-8")));
            Path zip = tmp.getRoot().toPath().resolve("lib.zip");
            org.junit.Assert.assertArrayEquals(new int[]{1, 1, 2}, src.exportLibrary(zip));

            DesktopRuntime.LibraryPreview preview = dst.previewLibrary(zip);
            assertEquals(1, preview.addGlossaries());
            assertEquals(1, preview.addPronouns());
            assertEquals(2, preview.addRaws());
            assertEquals(0, preview.bad());
            assertTrue(dst.applyLibrary(preview));
            assertEquals(1, dst.glossaries().size());
            assertEquals(1, dst.pronouns().size());
            assertEquals(2, dst.raws().list().size());
            assertEquals(src.raws().list().get(0).inputHash, dst.raws().list().get(0).inputHash);
            assertEquals(1, dst.raws().list().get(0).chapterNo);

            DesktopRuntime.LibraryPreview again = dst.previewLibrary(zip);
            assertEquals("importing the same file twice adds nothing", 0, again.addedCount());
            assertEquals(4, again.existing());

            Path zip2 = tmp.getRoot().toPath().resolve("lib2.zip");
            dst.exportLibrary(zip2);
            assertEquals("same content, same library: nothing to add from the PC export either", 0, src.previewLibrary(zip2).addedCount());
        }
    }

    @Test public void anArchiveWhoseIntegrityCodeDoesNotMatchIsRejected() throws Exception {
        assumeTrue(windows());
        try (DesktopRuntime src = open("src2"); DesktopRuntime dst = open("dst2")) {
            src.importGlossaries(Arrays.asList(write("QA_gl2.csv", "source,target,category,note\n魔王,Ma vương,character,\n")));
            Path zip = tmp.getRoot().toPath().resolve("good.zip");
            src.exportLibrary(zip);
            byte[] tampered = tamper(Files.readAllBytes(zip), "Ma vương", "Ma vuong");
            Path bad = tmp.getRoot().toPath().resolve("bad.zip");
            Files.write(bad, tampered);
            DesktopRuntime.LibraryPreview preview = dst.previewLibrary(bad);
            assertEquals(1, preview.bad());
            assertEquals(0, preview.addedCount());
            assertTrue(dst.applyLibrary(preview));
            assertTrue("nothing from the tampered file was stored", dst.glossaries().isEmpty());
        }
    }

    @Test public void aFileThatIsNotALibraryZipIsRefusedWithAReadableReason() throws Exception {
        assumeTrue(windows());
        try (DesktopRuntime rt = open("n")) {
            Path notZip = write("notes.zip", "plain text");
            try {
                rt.previewLibrary(notZip);
                throw new AssertionError("expected a refusal");
            } catch (IllegalArgumentException expected) {
                assertFalse(expected.getMessage().isEmpty());
            }
        }
    }

    @Test public void rawLibraryRefsReadThroughTheDocumentPort() throws Exception {
        assumeTrue(windows());
        try (DesktopRuntime rt = open("r")) {
            String text = "QA một chương tự viết.\nDòng hai.\n".repeat(4);
            int[] counts = rt.importRawFiles(Arrays.asList(write("012_QA_chuong.txt", text)));
            assertEquals(1, counts[0]);
            assertEquals(0, rt.importRawFiles(Arrays.asList(write("012_QA_chuong_copy.txt", text)))[0]);
            DesktopRawStore.Item item = rt.raws().list().get(0);
            assertEquals(12, item.chapterNo);
            PathDocumentIO io = new PathDocumentIO(null, rt.raws());
            assertEquals(Chunker.normalizeSource(text), io.readText(item.uri()));
            assertEquals("012_QA_chuong.txt", io.displayName(item.uri()));
            rt.raws().delete(new ArrayList<>(Arrays.asList(item.id)));
            assertTrue(rt.raws().list().isEmpty());
        }
    }

    @Test public void aKeyInsideAnOlderProfileNeverReachesTheSavedSettingsOrTheDpapiKey() throws Exception {
        assumeTrue(windows());
        try (DesktopRuntime rt = open("k")) {
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            DesktopRuntime.ProfilePreview p = rt.previewProfile("{\"apiKey\":\"QA_OTHER_KEY_VALUE\",\"model\":\"QA_m\"}");
            rt.applyProfile(p);
            assertEquals("QA_m", rt.loadSettings().model);
            assertEquals("", rt.loadSettings().apiKey);
            assertEquals("TEST_KEY_NOT_SECRET", rt.secrets().load());
            try (java.util.stream.Stream<Path> walk = Files.walk(rt.dataDir)) {
                for (Path f : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                    if (f.getFileName().toString().equals("app.lock")) continue;
                    assertFalse(f + " holds the key", new String(Files.readAllBytes(f), StandardCharsets.ISO_8859_1).contains("QA_OTHER_KEY_VALUE"));
                }
            }
        }
    }

    /** Replaces text inside the zipped files without recomputing any integrity code. */
    private static byte[] tamper(byte[] zip, String from, String to) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8);
             ZipOutputStream o = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                String name = e.getName();
                String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (name.equals("library.json")) body = body.replace(from, to);
                o.putNextEntry(new ZipEntry(name));
                o.write(body.getBytes(StandardCharsets.UTF_8));
                o.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
