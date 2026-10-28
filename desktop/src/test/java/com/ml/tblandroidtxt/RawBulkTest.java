package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

/** T5: bulk delete of RAW with the second confirmation for chapters that belong to a set, and an undo that restores everything. */
public class RawBulkTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static String chapter(int n) {
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < 3; p++) sb.append("QA chương ").append(n).append(" đoạn ").append(p).append(" nội dung riêng.\n");
        return sb.toString().repeat(4);
    }

    private DesktopRuntime open() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        return DesktopRuntime.open(tmp.getRoot().toPath().resolve("data"), null);
    }

    private List<String> add(DesktopRuntime rt, int... chapters) throws Exception {
        List<DesktopRawStore.Picked> picked = new ArrayList<>();
        for (int n : chapters) picked.add(new DesktopRawStore.Picked(String.format("%03d_QA_raw.txt", n), chapter(n), "UTF-8"));
        rt.raws().add(picked);
        List<String> ids = new ArrayList<>();
        for (DesktopRawStore.Item i : rt.raws().list()) ids.add(i.id);
        return ids;
    }

    @Test public void aRawInsideASetNeedsTheSecondConfirmationAndUndoBringsEverythingBack() throws Exception {
        try (DesktopRuntime rt = open()) {
            List<String> ids = add(rt, 1, 2, 3);
            ChapterSetStoreCore.SetData set = new ChapterSetStoreCore.SetData();
            set.name = "QA_tap";
            set.rows.add(new ChapterSetStoreCore.Row(2, ids.get(1), "", ""));
            ChapterSetStoreCore.save(rt.db(), set);

            DesktopRawBulk bulk = rt.rawBulk();
            DesktopRawBulk.Plan plain = bulk.plan(Arrays.asList(ids.get(0), ids.get(2)));
            assertEquals(2, plain.deletableCount());
            assertFalse("no chapter of these two is in a set", plain.needsSecondConfirm());

            DesktopRawBulk.Plan risky = bulk.plan(ids);
            assertEquals(3, risky.deletableCount());
            assertTrue(risky.needsSecondConfirm());
            assertTrue(risky.secondMessage().contains("QA_tap"));

            String hash = rt.raws().find(ids.get(1)).inputHash;
            DesktopRawBulk.Pending pending = bulk.delete(risky);
            assertEquals(3, pending.count());
            assertTrue("the list is empty at once", rt.raws().list().isEmpty());

            assertEquals(3, bulk.undo(pending));
            List<DesktopRawStore.Item> back = rt.raws().list();
            assertEquals(3, back.size());
            assertEquals(1, back.get(0).chapterNo);
            assertEquals(hash, back.get(1).inputHash);
            assertEquals(chapter(2), rt.raws().text(ids.get(1)).replace("\r", ""));
            assertEquals("a second undo does nothing", 0, bulk.undo(pending));
            assertEquals("the set still points at the restored RAW", Arrays.asList("QA_tap"), rt.raws().chapterSetNames(ids.get(1)));
        }
    }

    @Test public void filesAreRemovedOnlyWhenTheUndoWindowEnds() throws Exception {
        try (DesktopRuntime rt = open()) {
            List<String> ids = add(rt, 5, 6);
            Path dir = rt.dataDir.resolve(DesktopRawStore.DIR);
            assertEquals(2, Files.list(dir).filter(f -> !f.getFileName().toString().endsWith(".tmp")).count());
            DesktopRawBulk.Pending pending = rt.rawBulk().delete(rt.rawBulk().plan(ids));
            assertEquals("files are kept while undo is possible", 2, Files.list(dir).count());
            rt.rawBulk().expire(pending);
            assertEquals(0, Files.list(dir).count());
            assertEquals("after the window ended undo restores nothing", 0, rt.rawBulk().undo(pending));
            assertTrue(rt.raws().list().isEmpty());
        }
    }

    @Test public void aRawNeededByAnUnfinishedJobIsLeftOutOfTheDelete() throws Exception {
        try (DesktopRuntime rt = open()) {
            List<String> ids = add(rt, 7, 8);
            DbValues v = new DbValues();
            v.put("input_uri", RawUri.of(ids.get(0)));
            v.put("file_name", "007_QA_raw.txt");
            v.put("status", "paused");
            v.put("created_at", Long.valueOf(1));
            v.put("updated_at", Long.valueOf(1));
            rt.db().insertOrThrow("jobs", null, v);
            DesktopRawBulk.Plan plan = rt.rawBulk().plan(ids);
            assertEquals(1, plan.deletableCount());
            assertEquals(1, plan.blockedCount());
            assertTrue(plan.firstMessage().contains("job"));
        }
    }

    @Test public void replaceKeepsIdAndRejectsDuplicatesAndExportWritesFiles() throws Exception {
        try (DesktopRuntime rt = open()) {
            List<String> ids = add(rt, 9, 10);
            Path other = tmp.getRoot().toPath().resolve("011_QA_new.txt");
            Files.write(other, chapter(11).getBytes(StandardCharsets.UTF_8));
            assertNull(rt.replaceRaw(ids.get(0), other));
            DesktopRawStore.Item item = rt.raws().find(ids.get(0));
            assertEquals(11, item.chapterNo);
            assertEquals(Chunker.normalizeSource(chapter(11)), rt.raws().text(ids.get(0)));
            Path same = tmp.getRoot().toPath().resolve("010_QA_same.txt");
            Files.write(same, chapter(10).getBytes(StandardCharsets.UTF_8));
            assertTrue("same text as another RAW is refused", rt.replaceRaw(ids.get(0), same).contains("trùng"));

            Path out = tmp.getRoot().toPath().resolve("exported");
            assertEquals(2, rt.rawBulk().export(ids, out));
            assertEquals(2, Files.list(out).count());
            assertFalse(rt.raws().rename(ids.get(0), "   "));
            assertTrue(rt.raws().rename(ids.get(0), "QA_ten_moi"));
        }
    }
}
