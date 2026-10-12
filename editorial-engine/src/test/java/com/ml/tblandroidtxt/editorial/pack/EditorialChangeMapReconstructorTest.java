package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EditorialChangeMapReconstructorTest {
    private static final String PRED = "pred-1";
    private static final EditorialChangeMapReconstructor.SpeakerProof FULL_PROOF =
            new EditorialChangeMapReconstructor.SpeakerProof("Anh", "Em", "truoc", "sau");

    private static byte[] utf8(String text) { return text.getBytes(StandardCharsets.UTF_8); }

    private static String sha(String text) { return EditorialCanonicalJson.sha256Hex(utf8(text)); }

    private static EditorialChangeMapReconstructor.ChangeRow row(String id, int line, String before,
                                                                  String after) {
        return new EditorialChangeMapReconstructor.ChangeRow(id, "E-" + id, line, before, after,
                "reason", false, null, EditorialChangeMapReconstructor.DeclaredStatus.CLOSED);
    }

    private static EditorialChangeMapReconstructor.ChangeRow rowFull(
            String id, String errorId, int line, String before, String after, String reason,
            boolean dialogue, EditorialChangeMapReconstructor.SpeakerProof proof,
            EditorialChangeMapReconstructor.DeclaredStatus status) {
        return new EditorialChangeMapReconstructor.ChangeRow(id, errorId, line, before, after,
                reason, dialogue, proof, status);
    }

    private static EditorialChangeMapReconstructor.PreservedRow keep(String id, int line, String before) {
        return new EditorialChangeMapReconstructor.PreservedRow(id, line, before, "limit");
    }

    private static EditorialChangeMapReconstructor.Request request(
            byte[] base, List<EditorialChangeMapReconstructor.ChangeRow> changes,
            List<EditorialChangeMapReconstructor.PreservedRow> preserved, Set<Integer> protectedLines) {
        return new EditorialChangeMapReconstructor.Request(
                EditorialChangeMapReconstructor.Kind.L2_DRAFT_TO_VI_L2, base, PRED, changes, preserved,
                protectedLines);
    }

    private static EditorialChangeMapReconstructor.Result run(
            String base, List<EditorialChangeMapReconstructor.ChangeRow> changes) {
        return new EditorialChangeMapReconstructor().reconstruct(
                request(utf8(base), changes, List.of(), Set.of()));
    }

    private static void assertRepair(EditorialChangeMapReconstructor.Result result, String issue) {
        assertEquals(result.issues().toString(), EditorialChangeMapReconstructor.Status.REPAIR_REQUIRED,
                result.status());
        assertTrue(result.issues().toString(), result.issues().contains(issue));
        assertNull(result.outputBytes());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Map<String, Object> map, String key) {
        return (List<Map<String, Object>>) map.get(key);
    }

    private static int num(Object value) { return ((Number) value).intValue(); }

    @Test public void noEditKeepsBaseBytesAndZeroCounts() {
        byte[] base = utf8("a\nb\nc");
        EditorialChangeMapReconstructor.Result result = new EditorialChangeMapReconstructor()
                .reconstruct(request(base, List.of(), List.of(), Set.of()));
        assertTrue(result.accepted());
        assertArrayEquals(base, result.outputBytes());
        assertTrue(result.actualChangedLines().isEmpty());
        assertTrue(result.applied().isEmpty());
        Map<String, Object> map = EditorialCanonicalJson.parseObject(result.changeMapBytes());
        @SuppressWarnings("unchecked")
        Map<String, Object> counts = (Map<String, Object>) map.get("counts");
        for (String key : List.of("applied", "reverted", "preserved", "actualChangedLines",
                "unaccountedChangedLines", "protectedSpanRegressions", "dialogueChangesWithoutProof")) {
            assertEquals(key, 0, num(counts.get(key)));
        }
        assertEquals(EditorialCanonicalJson.sha256Hex(base), map.get("baseSha256"));
        assertEquals(map.get("baseSha256"), map.get("outputSha256"));
    }

    @Test public void validSingleChangeProducesExactOutputAndChangeMap() {
        byte[] base = utf8("one\ntwo\nthree");
        EditorialChangeMapReconstructor.Result result = new EditorialChangeMapReconstructor().reconstruct(
                request(base, List.of(row("c1", 2, "two", "TWO")), List.of(), Set.of()));
        assertTrue(result.issues().toString(), result.accepted());
        assertEquals("one\nTWO\nthree", new String(result.outputBytes(), StandardCharsets.UTF_8));
        assertEquals(List.of(2), result.actualChangedLines());
        assertEquals(1, result.applied().size());
        assertEquals(sha("two"), result.applied().get(0).beforeHash());
        assertEquals(sha("TWO"), result.applied().get(0).afterHash());
        Map<String, Object> map = EditorialCanonicalJson.parseObject(result.changeMapBytes());
        assertEquals("safe4.full.change-map-l2.v1", map.get("schemaVersion"));
        assertEquals("CHANGE_MAP_L2", map.get("artifactType"));
        assertEquals(EditorialCanonicalJson.sha256Hex(base), map.get("baseSha256"));
        assertEquals(EditorialCanonicalJson.sha256Hex(result.outputBytes()), map.get("outputSha256"));
        assertEquals(PRED, map.get("predecessorIdentity"));
        assertEquals("DRAFT", map.get("baseRole"));
        assertEquals("VI_L2", map.get("outputRole"));
        List<Map<String, Object>> changes = rows(map, "changes");
        assertEquals(1, changes.size());
        assertEquals("CLOSED", changes.get(0).get("status"));
        assertEquals(sha("two"), changes.get(0).get("beforeHash"));
        assertEquals(sha("TWO"), changes.get(0).get("afterHash"));
    }

    @Test public void crlfAndMissingFinalTerminatorAreReproducedExactly() {
        String base = "one\r\ntwo\r\nthree";
        EditorialChangeMapReconstructor.Result result = run(base, List.of(row("c1", 2, "two", "TWO")));
        assertTrue(result.issues().toString(), result.accepted());
        assertEquals("one\r\nTWO\r\nthree", new String(result.outputBytes(), StandardCharsets.UTF_8));
        assertEquals(List.of(2), result.actualChangedLines());
        // Change the unterminated final line too.
        EditorialChangeMapReconstructor.Result last = run(base, List.of(row("c2", 3, "three", "THREE")));
        assertTrue(last.issues().toString(), last.accepted());
        assertEquals("one\r\ntwo\r\nTHREE", new String(last.outputBytes(), StandardCharsets.UTF_8));
    }

    @Test public void unicodeVietnameseTextIsPreserved() {
        String base = "Dòng một\nAnh ấy nói: “Đi thôi.”\nKết thúc\n";
        EditorialChangeMapReconstructor.Result result = run(base, List.of(
                row("c1", 2, "Anh ấy nói: “Đi thôi.”", "Anh ấy nói: “Đi nào.”")));
        assertTrue(result.issues().toString(), result.accepted());
        assertEquals("Dòng một\nAnh ấy nói: “Đi nào.”\nKết thúc\n",
                new String(result.outputBytes(), StandardCharsets.UTF_8));
        assertEquals(List.of(2), result.actualChangedLines());
        assertEquals(sha("Anh ấy nói: “Đi thôi.”"), result.applied().get(0).beforeHash());
    }

    @Test public void anchorMismatchRequiresRepair() {
        assertRepair(run("a\nb\nc", List.of(row("c1", 2, "WRONG", "x"))), "CHANGE_ANCHOR_MISMATCH:c1");
    }

    @Test public void malformedDeclarationsRequireRepair() {
        assertRepair(run("a\nb\nc", List.of(row("c1", 4, "b", "x"))), "CHANGE_LINE_OUT_OF_RANGE:c1");
        assertRepair(run("a\nb\nc", List.of(row("c1", 0, "b", "x"))), "CHANGE_LINE_OUT_OF_RANGE:c1");
        assertRepair(run("a\nb\nc", List.of(row("c1", 1, "a", "x"), row("c1", 2, "b", "y"))),
                "CHANGE_ID_DUPLICATE:c1");
        assertRepair(run("a\nb\nc", List.of(row("bad id", 2, "b", "x"))), "CHANGE_ID_INVALID");
        assertRepair(run("a\nb\nc", List.of(rowFull("c1", null, 2, "b", "x", "r", false, null,
                EditorialChangeMapReconstructor.DeclaredStatus.CLOSED))), "CHANGE_ERROR_ID_INVALID:c1");
        assertRepair(run("a\nb\nc", List.of(row("c1", 2, "b", "x\ny"))), "CHANGE_AFTER_INVALID:c1");
        assertRepair(run("a\nb\nc", List.of(row("c1", 2, "b", "b"))), "CHANGE_NO_OP:c1");
        assertRepair(run("a\nb\nc", List.of(row("a", 2, "b", "x"), row("b", 2, "b", "y"))),
                "CHANGE_LINE_CONFLICT:a,b");
    }

    @Test public void dialogueWithoutProofIsRevertedButCompleteProofIsApplied() {
        String base = "a\n\"hi\"\nc";
        EditorialChangeMapReconstructor.Result missing = run(base, List.of(rowFull("c1", "E-1", 2, "\"hi\"",
                "\"chào\"", "r", true, null, EditorialChangeMapReconstructor.DeclaredStatus.CLOSED)));
        assertTrue(missing.issues().toString(), missing.accepted());
        assertEquals(base, new String(missing.outputBytes(), StandardCharsets.UTF_8));
        assertEquals(1, missing.reverted().size());
        assertEquals(EditorialChangeMapReconstructor.REVERT_SPEAKER_PROOF_MISSING,
                missing.reverted().get(0).revertReason());
        assertTrue(missing.applied().isEmpty());
        assertTrue(missing.actualChangedLines().isEmpty());

        EditorialChangeMapReconstructor.SpeakerProof partial =
                new EditorialChangeMapReconstructor.SpeakerProof("Anh", " ", "a", "b");
        EditorialChangeMapReconstructor.Result incomplete = run(base, List.of(rowFull("c1", "E-1", 2,
                "\"hi\"", "\"chào\"", "r", true, partial,
                EditorialChangeMapReconstructor.DeclaredStatus.CLOSED)));
        assertEquals(EditorialChangeMapReconstructor.REVERT_SPEAKER_PROOF_MISSING,
                incomplete.reverted().get(0).revertReason());

        EditorialChangeMapReconstructor.Result proven = run(base, List.of(rowFull("c1", "E-1", 2, "\"hi\"",
                "\"chào\"", "r", true, FULL_PROOF, EditorialChangeMapReconstructor.DeclaredStatus.CLOSED)));
        assertTrue(proven.issues().toString(), proven.accepted());
        assertEquals("a\n\"chào\"\nc", new String(proven.outputBytes(), StandardCharsets.UTF_8));
        Map<String, Object> map = EditorialCanonicalJson.parseObject(proven.changeMapBytes());
        Map<String, Object> changeRow = rows(map, "changes").get(0);
        assertEquals(true, changeRow.get("dialogue"));
        @SuppressWarnings("unchecked")
        Map<String, Object> proof = (Map<String, Object>) changeRow.get("speakerProof");
        assertEquals("Anh", proof.get("speaker"));
        assertEquals("Em", proof.get("listener"));
        assertEquals("truoc", proof.get("anchorBefore"));
        assertEquals("sau", proof.get("anchorAfter"));
    }

    @Test public void protectedLineChangeIsReverted() {
        EditorialChangeMapReconstructor.Result result = new EditorialChangeMapReconstructor().reconstruct(
                request(utf8("a\nb\nc"), List.of(row("c1", 2, "b", "x")), List.of(), Set.of(2)));
        assertTrue(result.issues().toString(), result.accepted());
        assertEquals("a\nb\nc", new String(result.outputBytes(), StandardCharsets.UTF_8));
        assertEquals(EditorialChangeMapReconstructor.REVERT_PROTECTED_SPAN_TOUCHED,
                result.reverted().get(0).revertReason());
        assertTrue(result.applied().isEmpty());
    }

    @Test public void declaredRevertedRowIsNotAppliedButAnchorIsStillChecked() {
        EditorialChangeMapReconstructor.ChangeRow declared = rowFull("c1", "E-1", 2, "b", "x", "r", false,
                null, EditorialChangeMapReconstructor.DeclaredStatus.REVERTED);
        EditorialChangeMapReconstructor.Result result = run("a\nb\nc", List.of(declared));
        assertTrue(result.issues().toString(), result.accepted());
        assertEquals("a\nb\nc", new String(result.outputBytes(), StandardCharsets.UTF_8));
        assertEquals(EditorialChangeMapReconstructor.REVERT_DECLARED, result.reverted().get(0).revertReason());
        assertTrue(result.applied().isEmpty());
        Map<String, Object> map = EditorialCanonicalJson.parseObject(result.changeMapBytes());
        assertEquals("REVERTED", rows(map, "changes").get(0).get("status"));
        assertEquals("DECLARED_REVERTED", rows(map, "changes").get(0).get("revertReason"));

        EditorialChangeMapReconstructor.ChangeRow stale = rowFull("c1", "E-1", 2, "WRONG", "x", "r", false,
                null, EditorialChangeMapReconstructor.DeclaredStatus.REVERTED);
        assertRepair(run("a\nb\nc", List.of(stale)), "CHANGE_ANCHOR_MISMATCH:c1");
    }

    @Test public void preservedRowsAreListedAndValidated() {
        byte[] base = utf8("a\nb\nc");
        EditorialChangeMapReconstructor.Result ok = new EditorialChangeMapReconstructor().reconstruct(
                request(base, List.of(row("c1", 2, "b", "x")), List.of(keep("p1", 3, "c")), Set.of()));
        assertTrue(ok.issues().toString(), ok.accepted());
        assertEquals(1, ok.preserved().size());
        Map<String, Object> map = EditorialCanonicalJson.parseObject(ok.changeMapBytes());
        List<Map<String, Object>> preserved = rows(map, "preserved");
        assertEquals(1, preserved.size());
        assertEquals("p1", preserved.get(0).get("preserveId"));
        assertEquals(3, num(preserved.get(0).get("lineNumber")));
        assertEquals(sha("c"), preserved.get(0).get("beforeHash"));

        assertRepair(new EditorialChangeMapReconstructor().reconstruct(
                request(base, List.of(), List.of(keep("p1", 3, "WRONG")), Set.of())),
                "PRESERVE_ANCHOR_MISMATCH:p1");
        assertRepair(new EditorialChangeMapReconstructor().reconstruct(
                request(base, List.of(row("c1", 2, "b", "x")), List.of(keep("p1", 2, "b")), Set.of())),
                "PRESERVE_LINE_CHANGED:p1");
        assertRepair(new EditorialChangeMapReconstructor().reconstruct(
                request(base, List.of(row("c1", 2, "b", "x")), List.of(keep("c1", 3, "c")), Set.of())),
                "PRESERVE_ID_DUPLICATE:c1");
    }

    @Test public void invalidInputsRequireInput() {
        EditorialChangeMapReconstructor reconstructor = new EditorialChangeMapReconstructor();
        EditorialChangeMapReconstructor.Result notUtf8 = reconstructor.reconstruct(
                request(new byte[]{'a', (byte) 0xC3, (byte) 0x28, '\n'}, List.of(), List.of(), Set.of()));
        assertEquals(EditorialChangeMapReconstructor.Status.INPUT_REQUIRED, notUtf8.status());
        assertEquals(List.of("BASE_TEXT_NOT_UTF8"), notUtf8.issues());
        assertNull(notUtf8.outputBytes());

        EditorialChangeMapReconstructor.Result missing = reconstructor.reconstruct(
                request(null, List.of(), List.of(), Set.of()));
        assertEquals(EditorialChangeMapReconstructor.Status.INPUT_REQUIRED, missing.status());
        assertEquals(List.of("BASE_TEXT_MISSING"), missing.issues());

        EditorialChangeMapReconstructor.Result blank = reconstructor.reconstruct(
                new EditorialChangeMapReconstructor.Request(
                        EditorialChangeMapReconstructor.Kind.L2_DRAFT_TO_VI_L2, utf8("a"), "  ",
                        List.of(), List.of(), Set.of()));
        assertEquals(EditorialChangeMapReconstructor.Status.INPUT_REQUIRED, blank.status());
        assertEquals(List.of("PREDECESSOR_IDENTITY_INVALID"), blank.issues());
    }

    @Test public void reconstructionIsDeterministicAndRowsSortedByChangeId() {
        byte[] base = utf8("a\nb\nc\nd");
        List<EditorialChangeMapReconstructor.ChangeRow> forward = List.of(
                row("a1", 1, "a", "A"), row("b2", 2, "b", "B"), row("c3", 3, "c", "C"));
        List<EditorialChangeMapReconstructor.ChangeRow> shuffled = new ArrayList<>(
                Arrays.asList(forward.get(2), forward.get(0), forward.get(1)));
        EditorialChangeMapReconstructor.Result first = new EditorialChangeMapReconstructor()
                .reconstruct(request(base, forward, List.of(), Set.of()));
        EditorialChangeMapReconstructor.Result again = new EditorialChangeMapReconstructor()
                .reconstruct(request(base, forward, List.of(), Set.of()));
        EditorialChangeMapReconstructor.Result other = new EditorialChangeMapReconstructor()
                .reconstruct(request(base, shuffled, List.of(), Set.of()));
        assertTrue(first.issues().toString(), first.accepted());
        assertArrayEquals(first.changeMapBytes(), again.changeMapBytes());
        assertArrayEquals(first.changeMapBytes(), other.changeMapBytes());
        assertArrayEquals(first.outputBytes(), other.outputBytes());
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> changeRow : rows(EditorialCanonicalJson.parseObject(other.changeMapBytes()),
                "changes")) {
            ids.add((String) changeRow.get("changeId"));
        }
        assertEquals(List.of("a1", "b2", "c3"), ids);
        assertEquals(List.of(1, 2, 3), other.actualChangedLines());
    }

    @Test public void l3KindUsesQaChangeMapSchemaAndRoles() {
        EditorialChangeMapReconstructor.Result result = new EditorialChangeMapReconstructor().reconstruct(
                new EditorialChangeMapReconstructor.Request(
                        EditorialChangeMapReconstructor.Kind.L3_VI_L2_TO_FINAL, utf8("a\nb"), PRED,
                        List.of(row("c1", 2, "b", "B")), List.of(), Set.of()));
        assertTrue(result.issues().toString(), result.accepted());
        Map<String, Object> map = EditorialCanonicalJson.parseObject(result.changeMapBytes());
        assertEquals("safe4.full.qa-change-map.v1", map.get("schemaVersion"));
        assertEquals("QA_CHANGE_MAP", map.get("artifactType"));
        assertEquals("VI_L2", map.get("baseRole"));
        assertEquals("FINAL", map.get("outputRole"));
    }
}
