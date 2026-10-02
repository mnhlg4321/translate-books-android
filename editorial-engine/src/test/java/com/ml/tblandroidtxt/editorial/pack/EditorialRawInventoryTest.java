package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Inventory and chunk rules (RAW_LINE_INVENTORY_V1). All text here is synthetic. */
public final class EditorialRawInventoryTest {
    private static byte[] b(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    @Test
    public void idsAreLineBasedAndStable() {
        EditorialRawInventory.Inventory inv = EditorialRawInventory.build(b("一行目\n\n二行目\n三行目"));
        assertEquals(3, inv.units().size());
        assertEquals(1, inv.excluded().size());
        assertEquals(2, inv.excluded().get(0).line());
        assertEquals("BLANK", inv.excluded().get(0).reason());
        assertEquals(3, inv.units().get(1).line());
        assertTrue(inv.units().get(1).id().startsWith("u:3:"));
        assertEquals("u:3:".length() + 8, inv.units().get(1).id().length());
        assertEquals(4, inv.physicalLines());
    }

    @Test
    public void crlfLfAndBomGiveTheSameIds() {
        EditorialRawInventory.Inventory lf = EditorialRawInventory.build(b("甲\n乙\n丙\n"));
        EditorialRawInventory.Inventory crlf = EditorialRawInventory.build(b("甲\r\n乙\r\n丙\r\n"));
        EditorialRawInventory.Inventory bom = EditorialRawInventory.build(b("﻿甲\n乙\n丙\n"));
        assertEquals(lf.inventorySha256(), crlf.inventorySha256());
        assertEquals(lf.inventorySha256(), bom.inventorySha256());
        assertEquals(lf.units().get(0).id(), crlf.units().get(0).id());
        // the raw file hash still differs: bytes are bytes
        assertNotEquals(lf.rawSha256(), crlf.rawSha256());
    }

    @Test
    public void noTrailingNewlineKeepsTheLastLine() {
        EditorialRawInventory.Inventory inv = EditorialRawInventory.build(b("甲\n乙"));
        assertEquals(2, inv.units().size());
        assertEquals("乙", inv.units().get(1).text());
    }

    @Test
    public void whitespaceOnlyAndImageMarkerLinesAreExcludedWithReason() {
        EditorialRawInventory.Inventory inv = EditorialRawInventory.build(
                b("甲\n 　 \n[IMG_001]\n［挿絵］\n﻿\n乙\n"));
        assertEquals(2, inv.units().size());
        List<String> reasons = new ArrayList<>();
        for (EditorialRawInventory.Excluded e : inv.excluded()) reasons.add(e.line() + ":" + e.reason());
        assertEquals(List.of("2:BLANK", "3:IMAGE_MARKER", "4:IMAGE_MARKER", "5:BLANK", "7:BLANK"), reasons);
    }

    @Test
    public void lineWithImageMarkerAndTextIsAUnit() {
        EditorialRawInventory.Inventory inv = EditorialRawInventory.build(b("[IMG_001] 本文\n"));
        assertEquals(1, inv.units().size());
    }

    @Test
    public void loneCarriageReturnIsNotASeparator() {
        EditorialRawInventory.Inventory inv = EditorialRawInventory.build(b("甲\r乙\n丙"));
        assertEquals(2, inv.units().size());
    }

    @Test
    public void identicalTextOnDifferentLinesGetsDifferentIds() {
        EditorialRawInventory.Inventory inv = EditorialRawInventory.build(b("同じ\n同じ\n"));
        assertNotEquals(inv.units().get(0).id(), inv.units().get(1).id());
        assertEquals(inv.units().get(0).textSha256(), inv.units().get(1).textSha256());
    }

    @Test
    public void changedTextChangesTheId() {
        String a = EditorialRawInventory.build(b("甲\n乙\n")).units().get(1).id();
        String c = EditorialRawInventory.build(b("甲\n乙！\n")).units().get(1).id();
        assertNotEquals(a, c);
    }

    @Test
    public void invalidUtf8IsRefused() {
        try {
            EditorialRawInventory.build(new byte[] {(byte) 0xff, (byte) 0xfe, 'a'});
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("INPUT_RAW_NOT_UTF8", expected.getMessage());
        }
    }

    @Test
    public void coverageClosesOverEveryUnitExactlyOnce() {
        EditorialRawInventory.Inventory inv = EditorialRawInventory.build(b("1\n2\n3\n4\n5\n"));
        String id1 = inv.units().get(0).id(), id2 = inv.units().get(1).id(), id3 = inv.units().get(2).id();
        String id4 = inv.units().get(3).id(), id5 = inv.units().get(4).id();
        assertTrue(EditorialRawInventory.coverageIssues(inv, List.of(
                new EditorialRawInventory.Range(id1, id3, "PROCESSED"),
                new EditorialRawInventory.Range(id4, id5, "PRESERVED"))).isEmpty());
        assertEquals(List.of("COVERAGE_EMPTY"), EditorialRawInventory.coverageIssues(inv, List.of()));
        assertHas(EditorialRawInventory.coverageIssues(inv, List.of(
                new EditorialRawInventory.Range(id1, id2, "PROCESSED"),
                new EditorialRawInventory.Range(id4, id5, "PROCESSED"))), "COVERAGE_GAP");
        assertHas(EditorialRawInventory.coverageIssues(inv, List.of(
                new EditorialRawInventory.Range(id1, id3, "PROCESSED"))), "COVERAGE_GAP");
        assertHas(EditorialRawInventory.coverageIssues(inv, List.of(
                new EditorialRawInventory.Range(id1, id3, "PROCESSED"),
                new EditorialRawInventory.Range(id3, id5, "PROCESSED"))), "COVERAGE_OVERLAP");
        assertHas(EditorialRawInventory.coverageIssues(inv, List.of(
                new EditorialRawInventory.Range(id3, id1, "PROCESSED"))), "COVERAGE_REVERSED");
        assertHas(EditorialRawInventory.coverageIssues(inv, List.of(
                new EditorialRawInventory.Range("u:99:deadbeef", id5, "PROCESSED"))), "COVERAGE_UNKNOWN_ID");
        // out of order is a gap or an overlap, never silently accepted
        assertFalse(EditorialRawInventory.coverageIssues(inv, List.of(
                new EditorialRawInventory.Range(id4, id5, "PROCESSED"),
                new EditorialRawInventory.Range(id1, id3, "PROCESSED"))).isEmpty());
    }

    private static void assertHas(List<String> issues, String prefix) {
        for (String issue : issues) if (issue.startsWith(prefix)) return;
        fail(prefix + " missing in " + issues);
    }

    @Test
    public void chunkPlanOwnsEveryUnitExactlyOnceForManySizes() {
        for (int max : new int[] {1, 2, 7, 50, 200}) {
            for (int overlap : new int[] {0, 3, 6}) {
                for (int n = 0; n <= 520; n += (n < 30 ? 1 : 37)) {
                    List<EditorialRawInventory.Chunk> chunks = EditorialRawInventory.planChunks(n, max, overlap);
                    int expectedNext = 0;
                    for (EditorialRawInventory.Chunk c : chunks) {
                        assertEquals(expectedNext, c.ownedFrom());
                        assertTrue(c.ownedCount() >= 1 && c.ownedCount() <= max);
                        assertTrue(c.contextFrom() <= c.ownedFrom() && c.contextTo() >= c.ownedTo());
                        assertTrue(c.contextFrom() >= 0 && c.contextTo() <= n - 1);
                        assertTrue(c.ownedFrom() - c.contextFrom() <= overlap);
                        assertTrue(c.contextTo() - c.ownedTo() <= overlap);
                        expectedNext = c.ownedTo() + 1;
                    }
                    assertEquals(n, expectedNext);
                    for (int u = 0; u < n; u += Math.max(1, n / 9)) assertTrue(EditorialRawInventory.ownerOf(chunks, u) >= 0);
                }
            }
        }
    }

    @Test
    public void chunkPlanIsBalancedAndDeterministic() {
        List<EditorialRawInventory.Chunk> a = EditorialRawInventory.planChunks(401, 200, 6);
        assertEquals(3, a.size());
        assertEquals(a, EditorialRawInventory.planChunks(401, 200, 6));
        int min = Integer.MAX_VALUE, max = 0;
        for (EditorialRawInventory.Chunk c : a) { min = Math.min(min, c.ownedCount()); max = Math.max(max, c.ownedCount()); }
        assertTrue(max - min <= 1);
        assertTrue(EditorialRawInventory.planChunks(0, 200, 6).isEmpty());
        assertEquals(1, EditorialRawInventory.planChunks(200, 200, 6).size());
    }

    @Test
    public void invalidChunkParametersAreRefused() {
        try {
            EditorialRawInventory.planChunks(10, 0, 0);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("CHUNK_PARAMETERS_INVALID", expected.getMessage());
        }
    }
}
