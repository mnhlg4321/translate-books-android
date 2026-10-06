package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** GATE-01…09 and SEP-02 of the chunk-pair package: the structural gate is a suspicion fence, integer based, lines never block. */
public final class StructuralGateTest {
    private static StructuralGate.Result gate(String draft, String candidate) {
        return StructuralGate.check(new StructuralGate.Input(draft, candidate, EditResponseParser.Status.OK, "stop", "req-1", "req-1", 0));
    }

    private static String lines(int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to; i++) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("Dòng ").append(i).append(" của bản nháp có đủ chữ để đếm.");
        }
        return sb.toString();
    }

    private static String letters(char c, int n) { return String.valueOf(c).repeat(n); }

    // ---- GATE-01

    @Test public void envelopeAndIdentityFailuresBlock() {
        StructuralGate.Result format = StructuralGate.check(new StructuralGate.Input("Văn bản", null, EditResponseParser.Status.FORMAT, "stop", "r", "r", 0));
        assertEquals(StructuralGate.Status.BLOCK, format.status());
        assertTrue(format.has("FORMAT"));
        StructuralGate.Result length = StructuralGate.check(new StructuralGate.Input("Văn bản", "Văn bản sửa", EditResponseParser.Status.OK, "length", "r", "r", 0));
        assertTrue(length.has("FINISH_REASON"));
        assertEquals(StructuralGate.Status.BLOCK, length.status());
        StructuralGate.Result wrong = StructuralGate.check(new StructuralGate.Input("Văn bản", "Văn bản sửa", EditResponseParser.Status.OK, "stop", "r-1", "r-2", 0));
        assertTrue(wrong.has("PAIR_ID_MISMATCH"));
        StructuralGate.Result empty = gate("Văn bản", "  \n ");
        assertTrue(empty.has("CANDIDATE_EMPTY"));
        assertEquals(StructuralGate.Status.BLOCK, empty.status());
    }

    // ---- GATE-04 / verbatim

    @Test public void aValidPassageEditPassesWithoutAnyCode() {
        String draft = lines(1, 12);
        String candidate = draft.replace("Dòng 5 của bản nháp có đủ chữ để đếm.", "Dòng 5 của bản nháp được sửa lại cho đúng nghĩa hơn.");
        StructuralGate.Result r = gate(draft, candidate);
        assertEquals(r.codes().toString(), StructuralGate.Status.PASS, r.status());
        assertTrue(r.codes().isEmpty());
        assertFalse(r.verbatim());
    }

    @Test public void aVerbatimCandidateGivesNoReflowOrLineWarning() {
        String draft = lines(1, 12);
        StructuralGate.Result r = gate(draft, draft);
        assertEquals(StructuralGate.Status.PASS, r.status());
        assertTrue(r.verbatim());
        assertFalse(r.reflowOnly());
        assertTrue(r.codes().isEmpty());
        // the same text with edge whitespace is still verbatim after the edge is cut, and the cut is reported once
        StructuralGate.Result padded = gate(draft, "\n\n" + draft + "\n");
        assertTrue(padded.verbatim());
        assertEquals(List.of("BOUNDARY_WS_TRIMMED"), padded.warnCodes());
        assertFalse(padded.has("REFLOW_ONLY"));
    }

    // ---- GATE-07: lines never block

    @Test public void mergingLinesThatKeepTheLettersIsAWarningNeverABlock() {
        String draft = lines(1, 40);
        String[] rows = draft.split("\n");
        List<String> merged = new ArrayList<>();
        for (int i = 0; i < rows.length; i += 4) merged.add(rows[i] + " " + rows[i + 1] + " " + rows[i + 2] + " " + rows[i + 3]);
        // 40 -> 10 lines: far past the old 20 % line threshold
        StructuralGate.Result tenPer = gate(draft, String.join("\n", merged));
        assertEquals(tenPer.codes().toString(), StructuralGate.Status.WARN, tenPer.status());
        assertTrue(tenPer.has("REFLOW_ONLY"));
        assertTrue(tenPer.has("LINE_DELTA"));
        assertTrue(tenPer.blockCodes().isEmpty());
        // 40 -> 30 (ten pairs joined)
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < 20; i++) pairs.add(rows[i]);
        for (int i = 20; i < 40; i += 2) pairs.add(rows[i] + " " + rows[i + 1]);
        StructuralGate.Result thirty = gate(draft, String.join("\n", pairs));
        assertEquals(StructuralGate.Status.WARN, thirty.status());
        assertTrue(thirty.reflowOnly());
        // everything on one line
        StructuralGate.Result one = gate(draft, String.join(" ", rows));
        assertEquals(StructuralGate.Status.WARN, one.status());
        assertTrue(one.blockCodes().isEmpty());
        // the contrast: thirty lines but half of the letters gone -> block on the letters, not on the lines
        String d = letters('a', 500) + "\n" + letters('a', 500);
        StructuralGate.Result half = gate(d, letters('b', 450));
        assertEquals(StructuralGate.Status.BLOCK, half.status());
        assertEquals(List.of("CHARS_LOSS"), half.blockCodes());
    }

    // ---- GATE-08: restoring a missing passage

    @Test public void restoringAMissingPassageIsALineWarningNotABlock() {
        String draft = lines(1, 40);
        StringBuilder restored = new StringBuilder(draft);
        for (int i = 41; i <= 48; i++) restored.append('\n').append("Dòng ").append(i).append(" của bản nháp có đủ chữ để đếm.");
        StructuralGate.Result r = gate(draft, restored.toString());
        assertEquals(r.codes().toString(), StructuralGate.Status.WARN, r.status());
        assertEquals(List.of("LINE_DELTA"), r.warnCodes());
        assertTrue(r.blockCodes().isEmpty());
        // one sentence restored inside an existing line: no line change, nothing to report
        String inLine = draft.replace("Dòng 7 của bản nháp có đủ chữ để đếm.", "Dòng 7 của bản nháp có đủ chữ để đếm. Câu được khôi phục từ bản gốc.");
        assertEquals(StructuralGate.Status.PASS, gate(draft, inLine).status());
        // padding that more than doubles the text cannot be told from a restoration: blocked as growth
        String padded = draft + "\n" + (lines(41, 80) + "\n" + lines(81, 120));
        StructuralGate.Result growth = gate(draft, padded);
        assertEquals(StructuralGate.Status.BLOCK, growth.status());
        assertTrue(growth.has("CHARS_GROWTH"));
    }

    // ---- GATE-05: short chunks

    @Test public void shortChunksUseLineCountsNotPercentagesAndSplitOrMergeIsOnlyAWarning() {
        String three = "Dòng một có chữ.\nDòng hai có chữ.\nDòng ba có chữ.";
        assertEquals(StructuralGate.Status.PASS, gate(three, three.replace("hai", "2")).status());
        StructuralGate.Result toTwo = gate(three, "Dòng một có chữ.\nDòng hai có chữ. Dòng ba có chữ.");
        assertEquals(StructuralGate.Status.WARN, toTwo.status());
        assertTrue(toTwo.has("LINE_DELTA"));
        StructuralGate.Result toOne = gate(three, "Dòng một có chữ. Dòng hai có chữ. Dòng ba có chữ.");
        assertEquals(StructuralGate.Status.WARN, toOne.status());
        assertTrue(toOne.reflowOnly());
        assertTrue(toOne.blockCodes().isEmpty());
        StructuralGate.Result split = gate("Một dòng thoại ngắn.", "Một dòng\nthoại ngắn.");
        assertEquals(StructuralGate.Status.WARN, split.status());
        assertEquals(StructuralGate.Status.BLOCK, gate(three, " ").status());
        // under 80 letters the ratio is not used: a very short answer to a short line is fine
        assertEquals(StructuralGate.Status.PASS, gate("「はい、わかりました。」と彼は言った。", "“Vâng.”").status());
    }

    // ---- GATE-09: signs at the edges

    @Test public void characterThresholdsAreIntegerComparisonsWithTheMilderLevelOnTheEdge() {
        String draft = letters('a', 1000);
        int[][] cases = {
                {500, 1}, {499, 2}, // 2c = d is WARN, 2c = d - 1 is BLOCK
                {2000, 1}, {2001, 2}, // c = 2d is WARN, c = 2d + 1 is BLOCK
                {800, 0}, {799, 1}, // 5c = 4d is clean, 5c = 4d - 1 is WARN
                {1250, 0}, {1251, 1}, // 4c = 5d is clean, 4c = 5d + 1 is WARN
        };
        for (int[] c : cases) {
            StructuralGate.Result r = gate(draft, letters('b', c[0]));
            StructuralGate.Status expected = c[1] == 2 ? StructuralGate.Status.BLOCK : c[1] == 1 ? StructuralGate.Status.WARN : StructuralGate.Status.PASS;
            assertEquals("c = " + c[0] + " " + r.codes(), expected, r.status());
        }
        assertTrue(gate(draft, letters('b', 499)).has("CHARS_LOSS"));
        assertTrue(gate(draft, letters('b', 2001)).has("CHARS_GROWTH"));
        assertTrue(gate(draft, letters('b', 799)).has("CHARS_LOSS_WARN"));
        assertTrue(gate(draft, letters('b', 1251)).has("CHARS_GROWTH_WARN"));
        // below 80 letters: no ratio, only an empty answer or more than 320 letters blocks
        String shortDraft = letters('a', 50);
        assertEquals(StructuralGate.Status.BLOCK, gate(shortDraft, "").status());
        assertEquals(StructuralGate.Status.PASS, gate(shortDraft, letters('b', 320)).status());
        assertEquals(StructuralGate.Status.BLOCK, gate(shortDraft, letters('b', 321)).status());
        assertEquals(StructuralGate.Status.PASS, gate(shortDraft, letters('b', 3)).status());
        // exactly 80 letters is already in the ratio regime
        assertEquals(StructuralGate.Status.BLOCK, gate(letters('a', 80), letters('b', 39)).status());
        assertEquals(StructuralGate.Status.WARN, gate(letters('a', 80), letters('b', 40)).status());
    }

    @Test public void lettersIgnoreWhitespaceSoReflowDoesNotMoveTheRatio() {
        String draft = "a b c d e f g h i j ".repeat(10);
        assertEquals(100, PairText.letters(draft));
        assertEquals(100, PairText.letters(draft.replace(" ", "\n")));
    }

    // ---- GATE-02: N5 replay (192 -> 37) and the other hard cases

    private static String n5Twin() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 192; i++) {
            if (i > 1) sb.append('\n');
            if (i == 60 || i == 190) sb.append("◆");
            else sb.append("Dòng ").append(i).append(" của chương dài, có đủ chữ để chặn đúng khi bị cụt mất phần lớn.");
        }
        return sb.toString();
    }

    @Test public void theN5TruncationStaysBlockedForTheLettersAndTheMarkerNotForTheLineCount() {
        String draft = n5Twin();
        String[] rows = draft.split("\n");
        StringBuilder cut = new StringBuilder();
        for (int i = 0; i < 36; i++) cut.append(rows[i]).append('\n');
        cut.append("◆");
        StructuralGate.Result r = gate(draft, cut.toString());
        assertEquals(StructuralGate.Status.BLOCK, r.status());
        assertEquals(192, r.draftLines());
        assertEquals(37, r.candidateLines());
        assertTrue(r.has("CHARS_LOSS"));
        assertTrue(r.has("MARKER_MISMATCH"));
        assertTrue(r.has("LINE_DELTA"));
        assertEquals(StructuralGate.Severity.WARN, r.codes().stream().filter(c -> c.code().equals("LINE_DELTA")).findFirst().orElseThrow().severity());
        // remove the line gate from the picture: the block stands on the other two codes
        List<String> blockers = r.blockCodes();
        assertTrue(blockers.contains("CHARS_LOSS") && blockers.contains("MARKER_MISMATCH"));
        assertFalse(blockers.contains("LINE_DELTA"));
    }

    @Test public void explanationsAndLabelsInsideTheAnswerAreMetaFindings() {
        String draft = lines(1, 10);
        StructuralGate.Result tail = gate(draft, draft.replace("Dòng 3", "Dòng ba") + "\nGhi chú: đã sửa một chỗ.");
        assertTrue(tail.has("META_LEAK"));
        assertEquals(StructuralGate.Status.WARN, tail.status());
        assertFalse(tail.candidate().contains("Ghi chú"));
        StructuralGate.Result fence = gate(draft, "```\n" + draft + "\n```\nNotes: x");
        assertTrue(fence.has("META_LEAK") || fence.has("META_UNSEPARABLE"));
        StructuralGate.Result inside = gate(draft, draft + "\n```\nmã\n```");
        assertEquals(StructuralGate.Status.BLOCK, inside.status());
        StructuralGate.Result label = gate(draft, PairContract.paragraphLabel(4) + " " + draft);
        assertTrue(label.has("PARAGRAPH_LABEL_LEAK"));
        assertEquals(StructuralGate.Status.BLOCK, label.status());
    }

    // ---- SEP-02: boundary whitespace and markers

    @Test public void edgeWhitespaceIsCutAndReportedAndMarkersKeepTheirOrder() {
        String draft = "Mở đầu có chữ.\n\n◆\n\nHết đoạn có chữ.\n\n◇\n\nKết thúc có chữ.";
        StructuralGate.Result ok = gate(draft, "\n" + draft.replace("Mở đầu", "Bắt đầu") + "\n\n");
        assertEquals(StructuralGate.Status.WARN, ok.status());
        assertEquals(List.of("BOUNDARY_WS_TRIMMED"), ok.warnCodes());
        // markers swapped: blocked
        StructuralGate.Result swapped = gate(draft, "Mở đầu có chữ.\n\n◇\n\nHết đoạn có chữ.\n\n◆\n\nKết thúc có chữ.");
        assertTrue(swapped.has("MARKER_MISMATCH"));
        assertEquals(StructuralGate.Status.BLOCK, swapped.status());
        // a marker added where the draft has none: blocked
        StructuralGate.Result added = gate("Một đoạn có chữ.\n\nĐoạn hai có chữ.", "Một đoạn có chữ.\n\n◆\n\nĐoạn hai có chữ.");
        assertTrue(added.has("MARKER_MISMATCH"));
        // markers still present and in order but joined to a text line: warning only
        StructuralGate.Result joined = gate(draft, "Mở đầu có chữ.\n\n◆ Hết đoạn có chữ.\n\n◇ Kết thúc có chữ.");
        assertTrue(joined.codes().toString(), joined.has("MARKER_NOT_OWN_LINE"));
        assertEquals(StructuralGate.Status.WARN, joined.status());
        // the layout fingerprint is a hash of shape only
        assertEquals(StructuralGate.layoutFingerprint(draft), gate(draft, draft).internalLayoutFingerprint());
        assertFalse(StructuralGate.layoutFingerprint(draft).contains("Mở"));
    }

    @Test public void blankRunChangesAtTheSameLineCountAreAWarning() {
        String draft = "Một dòng có chữ.\nHai dòng có chữ.\n\nBa dòng có chữ.";
        StructuralGate.Result r = gate(draft, "Một dòng có chữ.\n\nHai dòng có chữ.\nBa dòng có chữ. (sửa)");
        assertEquals(StructuralGate.Status.WARN, r.status());
        assertTrue(r.has("BLANK_RUN_CHANGED"));
    }

    @Test public void theGateOnlyReadsTheEdgeOfTheRawBodyThroughTheParserAccessor() {
        String content = "<EDITED>\n\n  Nội dung đã sửa.  \n</EDITED><NOTES>a | b | c</NOTES>";
        assertEquals("\n\n  Nội dung đã sửa.  \n", EditResponseParser.rawEditedBody(content));
        assertEquals(null, EditResponseParser.rawEditedBody("không có thẻ"));
        EditResponseParser.Parsed parsed = EditResponseParser.parse(content, "stop");
        StructuralGate.Result r = StructuralGate.check(new StructuralGate.Input("Nội dung nháp.", EditResponseParser.rawEditedBody(content),
                parsed.status(), "stop", "r", "r", 0));
        assertTrue(r.has("BOUNDARY_WS_TRIMMED"));
        assertEquals("Nội dung đã sửa.", r.candidate());
    }
}
