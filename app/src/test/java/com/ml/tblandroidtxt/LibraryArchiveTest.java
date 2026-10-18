package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** v4.22 S0.4/S0.5: library ZIP round trip, merge rules and refusal of foreign or tampered files. */
public class LibraryArchiveTest {
    private static final String SECRET_KEY = "sk-or-v1-SYNTHETIC-KEY-1234567890";

    @Test public void roundTripKeepsEveryField() throws Exception {
        GlossaryStore.Glossary g = glossary("g-1", "001_Glossary, \"Synthetic\".csv", term("花子", "Hanako", "character", "gọi, \"chị\""));
        g.sourceLang = "Japanese"; g.createdAt = 11; g.updatedAt = 12;
        PronounStore.Profile p = pronoun("p-1", "001_Pronoun.csv", "from,speaker,target,self,call,scope,note\nHanako,,Taro,chị,em,,");
        p.uri = "content://synthetic/pronoun"; p.createdAt = 21; p.updatedAt = 22;

        byte[] zip = LibraryArchive.build(Collections.singletonList(g), Collections.singletonList(p), "g-1", "p-1", "4.22-dev.3", 1000L);
        LibraryArchive.Contents back = LibraryArchive.read(new ByteArrayInputStream(zip));

        assertTrue(back.bad.isEmpty());
        assertEquals("4.22-dev.3", back.appVersion);
        assertEquals("g-1", back.activeGlossaryId);
        assertEquals("p-1", back.activePronounId);
        GlossaryStore.Glossary g2 = back.glossaries.get(0);
        assertEquals(g.id, g2.id); assertEquals(g.name, g2.name); assertEquals("Japanese", g2.sourceLang);
        assertEquals(11, g2.createdAt); assertEquals(12, g2.updatedAt);
        assertEquals("gọi, \"chị\"", g2.terms.get(0).note);
        assertEquals(LibraryArchive.integrity(g), LibraryArchive.integrity(g2));
        PronounStore.Profile p2 = back.pronouns.get(0);
        assertEquals(p.text, p2.text); assertEquals(p.uri, p2.uri); assertEquals(LibraryArchive.integrity(p), LibraryArchive.integrity(p2));
    }

    @Test public void zipHoldsReadableCopiesAndNoSettings() throws Exception {
        GlossaryStore.Glossary g = glossary("g-1", "a/b:c", term("太郎", "Taro", "character", "x,y"));
        byte[] zip = LibraryArchive.build(Arrays.asList(g, glossary("g-2", "a/b:c", term("Z", "z", "term", ""))),
                Collections.singletonList(pronoun("p-1", "Pronoun", "Hanako -> Taro: chị/em")), "", "", "v", 1L);
        List<String> names = new ArrayList<>();
        String all = unzipAll(zip, names);
        assertTrue(names.contains("library.json"));
        assertTrue(names.contains("README.txt"));
        assertTrue(names.contains("glossaries/001_a_b_c.csv"));
        assertTrue(names.contains("glossaries/002_a_b_c.csv"));
        assertTrue(names.contains("pronouns/001_Pronoun.txt"));
        assertTrue(all.contains("source,target,category,note\n太郎,Taro,character,\"x,y\"\n"));
        assertFalse(all.contains("apiKey"));
        assertFalse(all.contains(SECRET_KEY));
    }

    @Test public void planAddsNewKeepsExistingAndNeverOverwrites() throws Exception {
        GlossaryStore.Glossary same = glossary("g-same", "Same", term("A", "a", "term", ""));
        GlossaryStore.Glossary changed = glossary("g-changed", "Changed", term("B", "b2", "term", ""));
        GlossaryStore.Glossary duplicateContent = glossary("g-other-id", "Same", term("A", "a", "term", ""));
        GlossaryStore.Glossary fresh = glossary("g-new", "New", term("C", "c", "term", ""));
        PronounStore.Profile pSame = pronoun("p-1", "P", "Hanako -> Taro: chị/em");
        PronounStore.Profile pNew = pronoun("p-2", "Q", "Taro -> Hanako: anh/em");
        byte[] zip = LibraryArchive.build(Arrays.asList(same, changed, duplicateContent, fresh), Arrays.asList(pSame, pNew), "", "", "v", 1L);

        List<GlossaryStore.Glossary> current = Arrays.asList(glossary("g-same", "Same", term("A", "a", "term", "")),
                glossary("g-changed", "Changed", term("B", "b", "term", "")));
        List<PronounStore.Profile> currentPronouns = Collections.singletonList(pronoun("p-1", "P", "Hanako -> Taro: chị/em"));
        LibraryArchive.MergePlan plan = LibraryArchive.plan(LibraryArchive.read(new ByteArrayInputStream(zip)), current, currentPronouns, "2026-10-18");

        assertEquals(2, plan.existing);
        assertEquals(1, plan.duplicate);
        assertEquals(1, plan.renamed);
        assertEquals(2, plan.addGlossaries.size());
        GlossaryStore.Glossary copy = plan.addGlossaries.get(0);
        assertNotEquals("g-changed", copy.id);
        assertEquals("Changed (nhập 2026-10-18)", copy.name);
        assertEquals("g-new", plan.addGlossaries.get(1).id);
        assertEquals(1, plan.addPronouns.size());
        assertEquals("p-2", plan.addPronouns.get(0).id);
        assertEquals("b", current.get(1).terms.get(0).target);
    }

    @Test public void importingTheSameBackupTwiceAddsNothing() throws Exception {
        List<GlossaryStore.Glossary> library = Arrays.asList(glossary("g-1", "One", term("A", "a", "term", "")));
        List<PronounStore.Profile> pronouns = Collections.singletonList(pronoun("p-1", "P", "x"));
        byte[] zip = LibraryArchive.build(library, pronouns, "", "", "v", 1L);
        LibraryArchive.MergePlan plan = LibraryArchive.plan(LibraryArchive.read(new ByteArrayInputStream(zip)), library, pronouns, "d");
        assertEquals(0, plan.addedCount());
        assertEquals(2, plan.existing);
    }

    @Test public void deletedProfileComesBackWithItsId() throws Exception {
        GlossaryStore.Glossary g = glossary("g-keep-id", "One", term("A", "a", "term", ""));
        byte[] zip = LibraryArchive.build(Collections.singletonList(g), Collections.<PronounStore.Profile>emptyList(), "", "", "v", 1L);
        LibraryArchive.MergePlan plan = LibraryArchive.plan(LibraryArchive.read(new ByteArrayInputStream(zip)),
                Collections.<GlossaryStore.Glossary>emptyList(), Collections.<PronounStore.Profile>emptyList(), "d");
        assertEquals("g-keep-id", plan.addGlossaries.get(0).id);
        assertEquals("One", plan.addGlossaries.get(0).name);
    }

    @Test public void tamperedItemIsSkippedAndListed() throws Exception {
        byte[] zip = LibraryArchive.build(Collections.singletonList(glossary("g-1", "One", term("A", "a", "term", ""))),
                Collections.<PronounStore.Profile>emptyList(), "", "", "v", 1L);
        String manifest = manifest(zip).replace("\"target\": \"a\"", "\"target\": \"changed\"");
        LibraryArchive.Contents c = LibraryArchive.read(new ByteArrayInputStream(zipOf("library.json", manifest)));
        assertTrue(c.glossaries.isEmpty());
        assertEquals(1, c.bad.size());
    }

    @Test public void foreignFilesAreRefusedWithAReason() throws Exception {
        expectRefusal(zipOf("other.json", "{}"), "không có library.json");
        expectRefusal(zipOf("library.json", "not json"), "không đọc được");
        expectRefusal(zipOf("library.json", new JSONObject().put("format", "x").put("formatVersion", 1).toString()), "định dạng");
        expectRefusal(zipOf("library.json", new JSONObject().put("format", LibraryArchive.FORMAT).put("formatVersion", 2).toString()), "mới hơn");
        expectRefusal("plain text".getBytes(StandardCharsets.UTF_8), "không có library.json");
    }

    @Test public void onlyTheManifestIsRead() throws Exception {
        byte[] good = LibraryArchive.build(Collections.singletonList(glossary("g-1", "One", term("A", "a", "term", ""))),
                Collections.<PronounStore.Profile>emptyList(), "", "", "v", 1L);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("../../escape.txt")); zip.write(1); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("library.json")); zip.write(manifest(good).getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
        }
        assertEquals(1, LibraryArchive.read(new ByteArrayInputStream(bytes.toByteArray())).glossaries.size());
    }

    @Test public void fileNamesAreSafe() {
        assertEquals("unnamed", LibraryArchive.safeFileName(" ../ "));
        assertEquals("013_Glossary_蜘蛛_Ex2.csv", LibraryArchive.safeFileName("013_Glossary_蜘蛛 Ex2.csv"));
        assertEquals(60, LibraryArchive.safeFileName(new String(new char[100]).replace('\0', 'a')).length());
        assertEquals("plain", LibraryArchive.csvCell("plain"));
        assertEquals("\"a\"\"b\"", LibraryArchive.csvCell("a\"b"));
    }

    private static void expectRefusal(byte[] zip, String reasonPart) throws Exception {
        try {
            LibraryArchive.read(new ByteArrayInputStream(zip));
            fail("expected refusal: " + reasonPart);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(reasonPart));
        }
    }

    private static GlossaryStore.Glossary glossary(String id, String name, GlossaryStore.Term... terms) {
        GlossaryStore.Glossary g = new GlossaryStore.Glossary();
        g.id = id; g.name = name; g.createdAt = 1; g.updatedAt = 2;
        g.terms.addAll(Arrays.asList(terms));
        return g;
    }

    private static GlossaryStore.Term term(String s, String t, String c, String n) { return new GlossaryStore.Term(s, t, c, n); }

    private static PronounStore.Profile pronoun(String id, String name, String text) {
        PronounStore.Profile p = new PronounStore.Profile();
        p.id = id; p.name = name; p.text = text; p.createdAt = 1; p.updatedAt = 2;
        return p;
    }

    private static String manifest(byte[] zip) throws Exception {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) if ("library.json".equals(e.getName())) return new String(readAll(in), StandardCharsets.UTF_8);
        }
        throw new AssertionError("no manifest");
    }

    private static String unzipAll(byte[] zip, List<String> names) throws Exception {
        StringBuilder all = new StringBuilder();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) { names.add(e.getName()); all.append(new String(readAll(in), StandardCharsets.UTF_8)).append('\n'); }
        }
        return all.toString();
    }

    private static byte[] zipOf(String name, String text) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(name)); zip.write(text.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static byte[] readAll(java.io.InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[4096]; int n;
        while ((n = in.read(b)) > 0) out.write(b, 0, n);
        return out.toByteArray();
    }
}
