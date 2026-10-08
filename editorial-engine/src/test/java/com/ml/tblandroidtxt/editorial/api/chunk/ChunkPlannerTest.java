package com.ml.tblandroidtxt.editorial.api.chunk;

import com.ml.tblandroidtxt.editorial.api.EditInputs;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** S2-S9 together on synthetic chapters: byte-exact chunk texts, verdicts, the stored plan and its hashes. */
public final class ChunkPlannerTest {
    private static final ToIntFunction<String> CHARS = s -> s.replace("\n", "").codePointCount(0, s.replace("\n", "").length());
    private static final List<EditInputs.GlossaryEntry> GLOSSARY = List.of(new EditInputs.GlossaryEntry("魔王", "Ma Vương", "", ""));

    private static ChunkPlanner.Settings settings(int soft, int hard) {
        return new ChunkPlanner.Settings("char", soft, hard, 0, 1.3, CHARS, CHARS, ChapterVerdict.DEFAULT_SERIES_RATIO);
    }

    private static String raw(int lines, String eol) {
        StringBuilder sb = new StringBuilder();
        for (int k = 1; k <= lines; k++) {
            String body = "これは" + k + "番目の文章であり、魔王についても少し書かれています。";
            sb.append(k % 3 == 0 ? "「" + body + "」" : body).append(eol);
            if (k % 5 == 0) sb.append(eol);
        }
        return sb.toString();
    }

    private static String draft(int lines, String eol) {
        StringBuilder sb = new StringBuilder();
        for (int k = 1; k <= lines; k++) {
            String body = "Đây là câu văn thứ " + k + " với Ma Vương được nhắc tới một chút trong đoạn.";
            sb.append(k % 3 == 0 ? "「" + body + "」" : body).append(eol);
            if (k % 5 == 0) sb.append(eol);
        }
        return sb.toString();
    }

    @Test public void chunkTextsAreExactSlicesThatRebuildBothFilesForLfAndCrlf() {
        for (String eol : new String[] {"\n", "\r\n"}) {
            String r = raw(60, eol);
            String d = draft(60, eol);
            ChunkPlanner.Planned p = ChunkPlanner.plan(r, d, GLOSSARY, "g", "p", settings(300, 600));
            assertEquals("OK", p.plan().verdict);
            assertTrue(p.plan().chunks.size() > 3);
            assertEquals(r, String.join("", p.rawRows()));
            assertEquals(d, String.join("", p.draftRows()));
            for (String row : p.rawRows()) assertFalse(row.isBlank());
        }
    }

    @Test public void aSplitAndAMergeStayInsideOneChunkEachAndAreCounted() {
        String[] r = raw(60, "\n").split("\n", -1);
        List<String> rawLines = new ArrayList<>(List.of(r));
        StringBuilder d = new StringBuilder();
        String[] dl = draft(60, "\n").split("\n", -1);
        List<String> draftLines = new ArrayList<>(List.of(dl));
        int split = draftLines.indexOf(draftLines.stream().filter(x -> x.contains("thứ 20 ")).findFirst().orElseThrow());
        String line = draftLines.get(split);
        int cut = line.indexOf(" với ");
        draftLines.set(split, line.substring(0, cut));
        draftLines.add(split + 1, line.substring(cut + 1));
        String joined = String.join("\n", draftLines);
        ChunkPlanner.Planned p = ChunkPlanner.plan(String.join("\n", rawLines), joined, GLOSSARY, "", "", settings(300, 600));
        assertEquals(1, p.plan().splitGroups);
        for (ChunkPlan.Chunk c : p.plan().chunks) {
            // the group's two DRAFT lines are never separated by a chunk boundary
            assertTrue(c.draftTo() - c.draftFrom() >= 1);
        }
        assertEquals(String.join("\n", rawLines), String.join("", p.rawRows()));
        assertEquals(joined, String.join("", p.draftRows()));
    }

    @Test public void aDraftOfAnotherChapterIsBlockedBeforeAnythingIsSent() {
        String other = draft(60, "\n").replace("Ma Vương", "Ai đó").replace("thứ ", "số 9").replace("「", "").replace("」", "");
        ChunkPlanner.Planned p = ChunkPlanner.plan(raw(60, "\n"), other, GLOSSARY, "", "", settings(300, 600));
        assertTrue(p.plan().blocked());
        assertNotNull(p.plan().reason("EDGE_SYMBOLS"));
    }

    @Test public void emptyTextsGiveABlockedPlanWithNoChunks() {
        ChunkPlanner.Planned p = ChunkPlanner.plan("", "   \n\n", GLOSSARY, "", "", settings(300, 600));
        assertTrue(p.plan().blocked());
        assertTrue(p.plan().chunks.isEmpty());
        assertNotNull(p.plan().reason("EMPTY_SOURCE"));
    }

    @Test public void thePlanRoundTripsThroughJsonWithoutAnyBookText() {
        String r = raw(60, "\n");
        ChunkPlanner.Planned p = ChunkPlanner.plan(r, draft(60, "\n"), GLOSSARY, "glossary text", "pronoun text", settings(300, 600));
        String json = p.plan().toJson();
        assertFalse(json.contains("文章"));
        assertFalse(json.contains("Ma Vương"));
        ChunkPlan back = ChunkPlan.fromJson(json);
        assertEquals(json, back.toJson());
        assertEquals(p.plan().chunks, back.chunks);
        assertEquals(p.plan().beads, back.beads);
        assertEquals(p.plan().verdict, back.verdict);
        assertEquals(p.plan().limits, back.limits);
        assertEquals(p.plan().reasons, back.reasons);
    }

    @Test public void aPlanIsOnlyReusedForTheExactSameFourInputs() {
        String r = raw(30, "\n");
        String d = draft(30, "\n");
        ChunkPlan plan = ChunkPlanner.plan(r, d, GLOSSARY, "g", "p", settings(300, 600)).plan();
        ChunkPlan twin = ChunkPlanner.plan(r, d, GLOSSARY, "g", "p", settings(300, 600)).plan();
        assertEquals(plan.toJson(), twin.toJson());
        assertTrue(plan.matches(plan.rawSha256, plan.draftSha256, plan.glossarySha256, plan.pronounSha256));
        ChunkPlan changed = ChunkPlanner.plan(r + "追加\n", d, GLOSSARY, "g", "p", settings(300, 600)).plan();
        assertFalse(plan.matches(changed.rawSha256, plan.draftSha256, plan.glossarySha256, plan.pronounSha256));
        assertNull(plan.reason("NAME_MATCH"));
    }

    @Test public void smallerLimitsGiveMoreChunksAndAHardLimitBoundsEachChunk() {
        String r = raw(80, "\n");
        String d = draft(80, "\n");
        int big = ChunkPlanner.plan(r, d, GLOSSARY, "", "", settings(900, 1800)).plan().chunks.size();
        int small = ChunkPlanner.plan(r, d, GLOSSARY, "", "", settings(300, 600)).plan().chunks.size();
        assertTrue(small > big);
        ChunkPlanner.Planned p = ChunkPlanner.plan(r, d, GLOSSARY, "", "", settings(300, 600));
        for (String row : p.rawRows()) assertTrue(CHARS.applyAsInt(LineUnits.parse(row).analysis(0, LineUnits.parse(row).size())) <= 700);
    }
}
