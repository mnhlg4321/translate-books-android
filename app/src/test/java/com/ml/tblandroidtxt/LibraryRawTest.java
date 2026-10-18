package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** v4.23 L1: chapter numbers, internal RAW addresses and files, import decisions, migration 30 and library ZIP format 2. */
public class LibraryRawTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test public void chapterNumberReadsOnlyTheStartOfTheName() {
        assertEquals(25, ChapterNumber.parse("025_RAW_Kumo.txt"));
        assertEquals(25, ChapterNumber.parse("25_x"));
        assertEquals(25, ChapterNumber.parse("025-x"));
        assertEquals(25, ChapterNumber.parse("  25 chapter"));
        assertEquals(0, ChapterNumber.parse("000_prologue"));
        assertEquals(ChapterNumber.NONE, ChapterNumber.parse("VOL08_013_RAW.txt"));
        assertEquals(ChapterNumber.NONE, ChapterNumber.parse("Ex2_RAW.txt"));
        assertEquals(ChapterNumber.NONE, ChapterNumber.parse("第3章.txt"));
        assertEquals(ChapterNumber.NONE, ChapterNumber.parse("RAW_025.txt"));
        assertEquals(ChapterNumber.NONE, ChapterNumber.parse(""));
        assertEquals(ChapterNumber.NONE, ChapterNumber.parse(null));
        assertEquals(ChapterNumber.NONE, ChapterNumber.parse("12345678901_too_long"));
    }

    @Test public void chapterOrderIsNumericAndNamelessGoLast() {
        List<String> names = new ArrayList<>(Arrays.asList("100_c", "9_a", "Ex2_z", "13_b", "VOL08_x", "013_dup"));
        Collections.sort(names, (a, b) -> ChapterNumber.compare(ChapterNumber.parse(a), a, ChapterNumber.parse(b), b));
        assertEquals(Arrays.asList("9_a", "013_dup", "13_b", "100_c", "Ex2_z", "VOL08_x"), names);
    }

    @Test public void rawUriAcceptsOnlySafeIds() {
        assertTrue(RawUri.isRaw("tblraw://3f2a-91"));
        assertEquals("3f2a-91", RawUri.idOf("tblraw://3f2a-91"));
        assertEquals("tblraw://abc", RawUri.of("abc"));
        assertEquals("", RawUri.idOf("content://media/1"));
        assertEquals("", RawUri.idOf("tblraw://"));
        assertEquals("", RawUri.idOf("tblraw://../../x"));
        assertEquals("", RawUri.idOf("tblraw://a/b"));
        assertFalse(RawUri.isRaw(null));
    }

    @Test public void rawFilesAreWrittenWholeAndSweptWhenOrphaned() throws Exception {
        File dir = new File(temp.getRoot(), "library_raw");
        RawFileStore store = new RawFileStore(dir);
        store.write("a1", "第一章\n本文");
        store.write("b2", "第二章");
        assertEquals("第一章\n本文", store.read("a1"));
        store.write("a1", "改訂版");
        assertEquals("改訂版", store.read("a1"));
        assertFalse(new File(dir, "a1.txt.recovery").exists());
        Files.write(new File(dir, "zz.txt123.pending").toPath(), new byte[]{1});
        int removed = store.purgeExcept(new HashSet<>(Collections.singletonList("a1")));
        assertEquals(2, removed);
        assertTrue(store.exists("a1"));
        assertFalse(store.exists("b2"));
        assertFalse(store.delete("../x") && false);
        try { store.write("a1", ""); throw new AssertionError("empty RAW must be refused"); } catch (java.io.IOException expected) { }
        try { store.fileFor("../escape"); throw new AssertionError("path escape must be refused"); } catch (java.io.IOException expected) { }
        assertEquals("改訂版", store.read("a1"));
    }

    @Test public void importPlanSkipsDuplicatesEmptyAndNonText() {
        RawImportPlanner.Candidate ok = new RawImportPlanner.Candidate("025_RAW.txt", "本文です。\r\n二行目", "UTF-8");
        RawImportPlanner.Candidate sameText = new RawImportPlanner.Candidate("copy.txt", "本文です。\n二行目", "UTF-8");
        RawImportPlanner.Candidate empty = new RawImportPlanner.Candidate("026.txt", "  \n", "UTF-8");
        RawImportPlanner.Candidate binary = new RawImportPlanner.Candidate("027.txt", "ab\u0000cd", "UTF-8");
        RawImportPlanner.Candidate known = new RawImportPlanner.Candidate("028.txt", "既にある", "UTF-8");
        HashSet<String> existing = new HashSet<>(Collections.singletonList(HashUtil.sha256("既にある")));
        RawImportPlanner.Plan plan = RawImportPlanner.plan(Arrays.asList(ok, sameText, empty, binary, known), existing);
        assertEquals(1, plan.added());
        assertEquals(4, plan.skipped());
        assertEquals(RawImportPlanner.Verdict.ADD, plan.entries.get(0).verdict);
        assertEquals(25, plan.entries.get(0).chapterNo);
        assertEquals("本文です。\n二行目", plan.entries.get(0).normalized);
        assertEquals(HashUtil.sha256(Chunker.normalizeSource(ok.text)), plan.entries.get(0).hash);
        assertEquals(RawImportPlanner.Verdict.DUPLICATE, plan.entries.get(1).verdict);
        assertEquals(RawImportPlanner.Verdict.INVALID, plan.entries.get(2).verdict);
        assertEquals(RawImportPlanner.Verdict.INVALID, plan.entries.get(3).verdict);
        assertEquals(RawImportPlanner.Verdict.DUPLICATE, plan.entries.get(4).verdict);
        assertEquals("Đã thêm 1 RAW, bỏ qua 4 (trùng/lỗi)", plan.summary());
    }

    @Test public void schema30CreatesLibraryTablesOnCreateAndUpgrade() throws Exception {
        List<String> sql = LibraryMigrationSpec.from29To30();
        String all = String.join("\n", sql).toLowerCase(Locale.ROOT);
        assertTrue(all.contains("create table if not exists library_raw"));
        assertTrue(all.contains("chapter_no integer"));
        assertTrue(all.contains("input_hash text not null"));
        assertTrue(all.contains("create table if not exists chapter_sets"));
        assertTrue(all.contains("create table if not exists chapter_set_rows"));
        assertFalse("no existing table may be altered", all.contains("alter table"));
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/ml/tblandroidtxt/TranslationRepository.java")), StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertTrue(source.contains("VER = 30"));
        assertTrue(source.contains("if (oldVersion < 30) createLibraryTables(db);"));
        assertTrue(source.indexOf("if (oldVersion < 29) migrateEditorialPairRunsToV29(db);") < source.indexOf("if (oldVersion < 30) createLibraryTables(db);"));
        assertTrue(source.contains("createPreparedPlanTables(db);\n        createLibraryTables(db);"));
    }

    private static LibraryArchive.RawEntry raw(String id, String originalName, String text) {
        LibraryArchive.RawEntry e = new LibraryArchive.RawEntry();
        e.id = id; e.name = LibraryRawStore.stripExtension(originalName); e.originalName = originalName; e.encoding = "UTF-8";
        e.text = text; e.chars = text.length(); e.inputHash = HashUtil.sha256(text); e.chapterNo = ChapterNumber.parse(originalName);
        e.createdAt = 5; e.updatedAt = 6;
        return e;
    }

    @Test public void archiveFormat2RoundTripsRawWithTextAndMetadata() throws Exception {
        LibraryArchive.RawEntry a = raw("r-1", "025_RAW_Kumo.txt", "第一章\n本文");
        LibraryArchive.RawEntry b = raw("r-2", "VOL08_x.txt", "別の章");
        byte[] zip = LibraryArchive.build(new ArrayList<GlossaryStore.Glossary>(), new ArrayList<PronounStore.Profile>(), Arrays.asList(a, b), "", "", "4.23", 1L);
        LibraryArchive.Contents back = LibraryArchive.read(new ByteArrayInputStream(zip));
        assertTrue(back.bad.toString(), back.bad.isEmpty());
        assertEquals(2, back.raws.size());
        LibraryArchive.RawEntry a2 = back.raws.get(0);
        assertEquals("r-1", a2.id); assertEquals("025_RAW_Kumo.txt", a2.originalName); assertEquals(25, a2.chapterNo);
        assertEquals("第一章\n本文", a2.text); assertEquals(a.inputHash, a2.inputHash); assertEquals(5, a2.createdAt);
        assertEquals(ChapterNumber.NONE, back.raws.get(1).chapterNo);
        java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8);
        List<String> names = new ArrayList<>();
        for (java.util.zip.ZipEntry e; (e = in.getNextEntry()) != null; ) names.add(e.getName());
        assertTrue(names.toString(), names.contains("raw/001_025_RAW_Kumo.txt"));
        assertTrue(names.contains("raw/002_VOL08_x.txt"));
    }

    @Test public void archiveFormat1StillImportsAndHasNoRaw() throws Exception {
        JSONObject root = new JSONObject().put("format", "tbl-library").put("formatVersion", 1).put("glossaries", new org.json.JSONArray()).put("pronouns", new org.json.JSONArray());
        LibraryArchive.Contents c = LibraryArchive.parseManifest(root.toString());
        assertTrue(c.raws.isEmpty());
        assertTrue(c.bad.isEmpty());
        root.put("formatVersion", 3);
        try { LibraryArchive.parseManifest(root.toString()); throw new AssertionError("newer format must be refused"); } catch (IllegalArgumentException expected) { }
    }

    @Test public void tamperedOrMissingRawTextIsReportedAndNotImported() throws Exception {
        LibraryArchive.RawEntry a = raw("r-1", "001.txt", "原文");
        byte[] zip = LibraryArchive.build(new ArrayList<GlossaryStore.Glossary>(), new ArrayList<PronounStore.Profile>(), Collections.singletonList(a), "", "", "v", 1L);
        // Rewrite the zip with the RAW text changed but the manifest untouched.
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8);
             java.util.zip.ZipOutputStream zo = new java.util.zip.ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (java.util.zip.ZipEntry e; (e = in.getNextEntry()) != null; ) {
                byte[] data = in.readAllBytes();
                if (e.getName().startsWith("raw/")) data = "sửa".getBytes(StandardCharsets.UTF_8);
                zo.putNextEntry(new java.util.zip.ZipEntry(e.getName())); zo.write(data); zo.closeEntry();
            }
        }
        LibraryArchive.Contents tampered = LibraryArchive.read(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(tampered.raws.isEmpty());
        assertEquals(1, tampered.bad.size());
        LibraryArchive.Contents missing = LibraryArchive.parseManifest(new String(unzipManifest(zip), StandardCharsets.UTF_8));
        assertTrue(missing.raws.isEmpty());
        assertEquals(1, missing.bad.size());
    }

    private static byte[] unzipManifest(byte[] zip) throws Exception {
        try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            for (java.util.zip.ZipEntry e; (e = in.getNextEntry()) != null; ) if (e.getName().equals("library.json")) return in.readAllBytes();
        }
        throw new AssertionError("no manifest");
    }

    @Test public void mergePlanAddsRawOnlyWhenNewAndKeepsIdsOtherwiseCopies() throws Exception {
        LibraryArchive.RawEntry same = raw("r-1", "001.txt", "同じ");
        LibraryArchive.RawEntry sameHashOtherId = raw("r-9", "009.txt", "同じ");
        LibraryArchive.RawEntry fresh = raw("r-2", "002.txt", "新しい");
        LibraryArchive.RawEntry idClash = raw("r-3", "003.txt", "衝突");
        byte[] zip = LibraryArchive.build(new ArrayList<GlossaryStore.Glossary>(), new ArrayList<PronounStore.Profile>(), Arrays.asList(same, sameHashOtherId, fresh, idClash), "", "", "v", 1L);
        LibraryArchive.Contents in = LibraryArchive.read(new ByteArrayInputStream(zip));
        LibraryArchive.RawEntry curSame = raw("r-1", "001.txt", "同じ");
        LibraryArchive.RawEntry curClash = raw("r-3", "003.txt", "別内容");
        LibraryArchive.MergePlan plan = LibraryArchive.plan(in, new ArrayList<GlossaryStore.Glossary>(), new ArrayList<PronounStore.Profile>(), Arrays.asList(curSame, curClash), "2026-10-18");
        assertEquals(1, plan.existing);
        assertEquals(1, plan.duplicate);
        assertEquals(2, plan.addRaws.size());
        assertEquals(1, plan.renamed);
        assertEquals("r-2", plan.addRaws.get(0).id);
        assertNotEquals("r-3", plan.addRaws.get(1).id);
        assertEquals(2, plan.addedCount());
    }
}
