package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** INSERT_AFTER, DELETE and MERGE_WITH_NEXT on the app-owned reconstruction. Text is synthetic. */
public final class EditorialChangeMapStructuralOpsTest {
    private static final String PRED = "pred-1";
    private static final EditorialChangeMapReconstructor.Op REPLACE = EditorialChangeMapReconstructor.Op.REPLACE;
    private static final EditorialChangeMapReconstructor.Op INSERT = EditorialChangeMapReconstructor.Op.INSERT_AFTER;
    private static final EditorialChangeMapReconstructor.Op DELETE = EditorialChangeMapReconstructor.Op.DELETE;
    private static final EditorialChangeMapReconstructor.Op MERGE = EditorialChangeMapReconstructor.Op.MERGE_WITH_NEXT;

    private static byte[] utf8(String text) { return text.getBytes(StandardCharsets.UTF_8); }

    private static EditorialChangeMapReconstructor.ChangeRow op(String id, EditorialChangeMapReconstructor.Op op, int line,
                                                                String before, String after) {
        return new EditorialChangeMapReconstructor.ChangeRow(id, "E-" + id, line, before, after, "reason", false, null,
                EditorialChangeMapReconstructor.DeclaredStatus.CLOSED, op);
    }

    private static EditorialChangeMapReconstructor.Result run(String base, Set<Integer> protectedLines,
                                                              EditorialChangeMapReconstructor.ChangeRow... rows) {
        return new EditorialChangeMapReconstructor().reconstruct(new EditorialChangeMapReconstructor.Request(
                EditorialChangeMapReconstructor.Kind.L2_DRAFT_TO_VI_L2, utf8(base), PRED, List.of(rows), List.of(),
                protectedLines));
    }

    private static EditorialChangeMapReconstructor.Result run(String base, EditorialChangeMapReconstructor.ChangeRow... rows) {
        return run(base, Set.of(), rows);
    }

    private static String text(EditorialChangeMapReconstructor.Result r) {
        assertEquals(r.issues().toString(), EditorialChangeMapReconstructor.Status.ACCEPTED, r.status());
        return new String(r.outputBytes(), StandardCharsets.UTF_8);
    }

    private static void assertRepair(EditorialChangeMapReconstructor.Result r, String issuePrefix) {
        assertEquals(r.issues().toString(), EditorialChangeMapReconstructor.Status.REPAIR_REQUIRED, r.status());
        assertTrue(r.issues().toString(), r.issues().stream().anyMatch(i -> i.startsWith(issuePrefix)));
        assertNull(r.outputBytes());
    }

    @Test public void insertAfterAddsALineWithTheAnchorsTerminator() {
        assertEquals("a\nNEW\nb\nc\n", text(run("a\nb\nc\n", op("C1", INSERT, 1, "a", "NEW"))));
        assertEquals("a\r\nNEW\r\nb\r\n", text(run("a\r\nb\r\n", op("C1", INSERT, 1, "a", "NEW"))));
    }

    @Test public void insertAfterTheLastUnterminatedLineAddsATerminatorToTheAnchor() {
        assertEquals("a\nb\nNEW", text(run("a\nb", op("C1", INSERT, 2, "b", "NEW"))));
        assertEquals("a\r\nb\r\nNEW", text(run("a\r\nb", op("C1", INSERT, 2, "b", "NEW"))));
    }

    @Test public void insertAtTheTopUsesLineZeroAndAnEmptyAnchor() {
        assertEquals("NEW\na\nb", text(run("a\nb", op("C1", INSERT, 0, "", "NEW"))));
        assertRepair(run("a\nb", op("C1", INSERT, 0, "a", "NEW")), "CHANGE_ANCHOR_MISMATCH");
    }

    @Test public void deleteRemovesTheLineAndItsTerminator() {
        assertEquals("a\nc\n", text(run("a\nb\nc\n", op("C1", DELETE, 2, "b", ""))));
        assertRepair(run("a\nb\nc\n", op("C1", DELETE, 2, "b", "x")), "CHANGE_DELETE_AFTER_NOT_EMPTY");
        assertRepair(run("a\nb\nc\n", op("C1", DELETE, 2, "wrong", "")), "CHANGE_ANCHOR_MISMATCH");
    }

    @Test public void mergeWithNextJoinsTwoLinesIntoTheGivenText() {
        assertEquals("a\nb c\nd\n", text(run("a\nb\nc\nd\n", op("C1", MERGE, 2, "b", "b c"))));
        // the merged line takes the terminator of the second line
        assertEquals("a\nb c", text(run("a\nb\nc", op("C1", MERGE, 2, "b", "b c"))));
        assertRepair(run("a\nb", op("C1", MERGE, 2, "b", "x")), "CHANGE_LINE_OUT_OF_RANGE");
        assertRepair(run("a\nb\nc", op("C1", MERGE, 1, "a", " ")), "CHANGE_AFTER_REQUIRED");
    }

    @Test public void rangeAndAnchorErrorsAreRejectedAsRepair() {
        assertRepair(run("a\nb", op("C1", INSERT, 3, "b", "x")), "CHANGE_LINE_OUT_OF_RANGE");
        assertRepair(run("a\nb", op("C1", INSERT, -1, "", "x")), "CHANGE_LINE_OUT_OF_RANGE");
        assertRepair(run("a\nb", op("C1", INSERT, 1, "stale", "x")), "CHANGE_ANCHOR_MISMATCH");
        assertRepair(run("a\nb", op("C1", INSERT, 1, "a", "has\nbreak")), "CHANGE_AFTER_INVALID");
    }

    @Test public void conflictingOperationsOnTheSameLineOrSlotAreRejected() {
        assertRepair(run("a\nb\nc", op("C1", DELETE, 2, "b", ""), op("C2", REPLACE, 2, "b", "x")), "CHANGE_LINE_CONFLICT");
        assertRepair(run("a\nb\nc", op("C1", MERGE, 1, "a", "ab"), op("C2", REPLACE, 2, "b", "x")), "CHANGE_LINE_CONFLICT");
        assertRepair(run("a\nb\nc", op("C1", INSERT, 1, "a", "x"), op("C2", INSERT, 1, "a", "y")), "CHANGE_LINE_CONFLICT");
        assertRepair(run("a\nb\nc", op("C1", MERGE, 1, "a", "ab"), op("C2", INSERT, 1, "a", "x")), "CHANGE_LINE_CONFLICT");
        // an insert after a line that is also deleted or replaced is a different slot and is fine
        assertEquals("a\nx\nc", text(run("a\nb\nc", op("C1", DELETE, 2, "b", ""), op("C2", INSERT, 2, "b", "x"))));
        assertEquals("a\nB\nx\nc", text(run("a\nb\nc", op("C1", REPLACE, 2, "b", "B"), op("C2", INSERT, 2, "b", "x"))));
    }

    @Test public void severalOperationsComposeInBaseNumbering() {
        String base = "l1\nl2\nl3\nl4\nl5\nl6\n";
        EditorialChangeMapReconstructor.Result r = run(base,
                op("C1", INSERT, 0, "", "top"),
                op("C2", REPLACE, 2, "l2", "L2"),
                op("C3", DELETE, 3, "l3", ""),
                op("C4", MERGE, 4, "l4", "l4+l5"),
                op("C5", INSERT, 5, "l5", "after5"),
                op("C6", INSERT, 6, "l6", "end"));
        assertEquals("top\nl1\nL2\nl4+l5\nafter5\nl6\nend\n", text(r));
        // base line -> output line (the empty last line after the final newline counts); deleted maps to 0, merged lines share one
        assertEquals(List.of(2, 3, 0, 4, 4, 6, 8), r.lineMap());
        assertEquals(List.of(1, 2, 3, 4, 5, 6), r.actualChangedLines());
        // the change map carries the operation and the structural counts, and gives the same line map back
        Map<String, Object> map = EditorialCanonicalJson.parseObject(r.changeMapBytes());
        assertEquals(r.lineMap(), EditorialChangeMapReconstructor.lineMapFromChangeMap(7, r.changeMapBytes()));
        @SuppressWarnings("unchecked") Map<String, Object> counts = (Map<String, Object>) map.get("counts");
        @SuppressWarnings("unchecked") Map<String, Object> structure = (Map<String, Object>) counts.get("structuralOps");
        assertEquals(3, ((Number) structure.get("insertAfter")).intValue());
        assertEquals(1, ((Number) structure.get("delete")).intValue());
        assertEquals(1, ((Number) structure.get("mergeWithNext")).intValue());
    }

    @Test public void insertAfterTheSecondLineOfAMergeLandsAfterTheMergedLine() {
        EditorialChangeMapReconstructor.Result r = run("a\nb\nc\n", op("C1", MERGE, 1, "a", "ab"), op("C2", INSERT, 2, "b", "x"));
        assertEquals("ab\nx\nc\n", text(r));
        assertEquals(List.of(1, 1, 3, 4), r.lineMap());
    }

    @Test public void replaceOnlySetsKeepTheOriginalBytesAndNoStructuralKeys() {
        EditorialChangeMapReconstructor.Result r = run("a\nb\nc\n", op("C1", REPLACE, 2, "b", "B"));
        assertEquals("a\nB\nc\n", text(r));
        assertEquals(List.of(1, 2, 3, 4), r.lineMap());
        String map = new String(r.changeMapBytes(), StandardCharsets.UTF_8);
        assertFalse(map.contains("structuralOps"));
        assertFalse(map.contains("\"op\""));
        // the nine-argument row is the same as an explicit REPLACE
        EditorialChangeMapReconstructor.Result legacy = new EditorialChangeMapReconstructor().reconstruct(
                new EditorialChangeMapReconstructor.Request(EditorialChangeMapReconstructor.Kind.L2_DRAFT_TO_VI_L2,
                        utf8("a\nb\nc\n"), PRED, List.of(new EditorialChangeMapReconstructor.ChangeRow("C1", "E-C1", 2, "b", "B",
                        "reason", false, null, EditorialChangeMapReconstructor.DeclaredStatus.CLOSED)), List.of(), Set.of()));
        assertArrayEquals(r.changeMapBytes(), legacy.changeMapBytes());
    }

    @Test public void protectedLinesRevertStructuralOperationsThatTouchThem() {
        EditorialChangeMapReconstructor.Result del = run("a\nb\nc", Set.of(2), op("C1", DELETE, 2, "b", ""));
        assertEquals("a\nb\nc", text(del));
        assertEquals(EditorialChangeMapReconstructor.REVERT_PROTECTED_SPAN_TOUCHED, del.reverted().get(0).revertReason());
        EditorialChangeMapReconstructor.Result merge = run("a\nb\nc", Set.of(2), op("C1", MERGE, 1, "a", "ab"));
        assertEquals("a\nb\nc", text(merge));
        // an insert next to one protected line is allowed; inside a protected span (both neighbours) it is reverted
        assertEquals("a\nx\nb\nc", text(run("a\nb\nc", Set.of(2), op("C1", INSERT, 1, "a", "x"))));
        EditorialChangeMapReconstructor.Result inside = run("a\nb\nc", Set.of(1, 2), op("C1", INSERT, 1, "a", "x"));
        assertEquals("a\nb\nc", text(inside));
        assertEquals(1, inside.reverted().size());
    }

    @Test public void dialogueOperationsStillNeedASpeakerProofAndPreservedLinesCannotBeRemoved() {
        EditorialChangeMapReconstructor.ChangeRow dialogue = new EditorialChangeMapReconstructor.ChangeRow("C1", "E1", 2,
                "b", "", "reason", true, null, EditorialChangeMapReconstructor.DeclaredStatus.CLOSED, DELETE);
        EditorialChangeMapReconstructor.Result r = run("a\nb\nc", dialogue);
        assertEquals("a\nb\nc", text(r));
        assertEquals(EditorialChangeMapReconstructor.REVERT_SPEAKER_PROOF_MISSING, r.reverted().get(0).revertReason());
        EditorialChangeMapReconstructor.Result kept = new EditorialChangeMapReconstructor().reconstruct(
                new EditorialChangeMapReconstructor.Request(EditorialChangeMapReconstructor.Kind.L2_DRAFT_TO_VI_L2,
                        utf8("a\nb\nc"), PRED, List.of(op("C1", DELETE, 2, "b", "")),
                        List.of(new EditorialChangeMapReconstructor.PreservedRow("P1", 2, "b", "limit")), Set.of()));
        assertRepair(kept, "PRESERVE_LINE_CHANGED");
    }

    @Test public void lineMapFromAnEmptyOrReplaceOnlyMapIsTheIdentity() {
        EditorialChangeMapReconstructor.Result r = run("a\nb\nc");
        assertEquals(List.of(1, 2, 3), EditorialChangeMapReconstructor.lineMapFromChangeMap(3, r.changeMapBytes()));
    }

    @Test public void lineHashesFollowTheSameSplittingRules() {
        List<String> hashes = EditorialChangeMapReconstructor.lineHashes(utf8("a\r\nb\nc"));
        assertEquals(3, hashes.size());
        assertEquals(EditorialCanonicalJson.sha256Hex(utf8("b")), hashes.get(1));
    }
}
