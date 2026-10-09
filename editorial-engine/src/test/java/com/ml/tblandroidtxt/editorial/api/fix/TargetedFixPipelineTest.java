package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;
import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan;
import com.ml.tblandroidtxt.editorial.api.fix.FixPoint.Type;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class TargetedFixPipelineTest {
    private static final String PRONOUN_HEADER = "from,speaker,target,self,call,scope,note\n";

    private static PairMap map(String raw, String draft) {
        return PairMaps.fromJobRows("CH006", "raw", "draft", List.of(raw), List.of(draft));
    }

    private static List<EditInputs.GlossaryEntry> glossary() {
        return List.of(new EditInputs.GlossaryEntry("竜王", "Long Vương", "name", ""));
    }

    private static List<FixPoint> find(String raw, String draft, String pronoun) {
        PairMap map = map(raw, draft);
        return FixPointFinder.find(map, map.entries().get(0), glossary(), PRONOUN_HEADER + (pronoun == null ? "" : pronoun), null, null).points();
    }

    private static FixPoint point(int id, Type type, int draftLine, String before, String raw) {
        return new FixPoint(id, Set.of(type), draftLine, before, raw, List.of(type.name() + " synthetic rule"));
    }

    @Test public void oneDraftLineCombinesIndependentAddressGlossaryAndKanaRules() {
        List<FixPoint> points = find("「竜王は来た。」", "「Cậu đã đến かな。」",
                "竜王,A,B,tôi,em,*,address\n");
        assertEquals(1, points.size());
        assertEquals(Set.of(Type.ADDRESS, Type.GLOSSARY, Type.KANA), points.get(0).types());
        assertEquals("「Cậu đã đến かな。」", points.get(0).draftText());
        assertTrue(points.get(0).rules().size() >= 3);
    }

    @Test public void narrationAndOutOfScopePronounsDoNotBecomeAddressPoints() {
        PairMap narrationMap = map("AがBに言った。", "Cậu đang kể chuyện.");
        List<FixPoint> narration = FixPointFinder.find(narrationMap, narrationMap.entries().get(0), List.of(),
                PRONOUN_HEADER + "A,A,B,tôi,em,p002,narrow\n", null, null).points();
        assertFalse(narration.stream().anyMatch(p -> p.types().contains(Type.ADDRESS)));
        List<FixPoint> outOfScope = find("「AはBに言った。」", "「Cậu sẽ đi。」", "A,A,B,tôi,em,p2,narrow\n");
        assertFalse(outOfScope.stream().anyMatch(p -> p.types().contains(Type.ADDRESS)));
    }

    @Test public void alignedRawDialogueStillScopesAddressWhenDraftDropsQuoteMarks() {
        List<FixPoint> points = find("「AはBに言った。」", "Cậu sẽ đi.", "A,A,B,tôi,em,*,address\n");
        assertTrue(points.stream().anyMatch(p -> p.types().contains(Type.ADDRESS)));
    }

    @Test public void conflictingPronounRowsPreserveTheDraftAddress() {
        List<FixPoint> points = find("「AはBに言った。」", "「Cậu sẽ đi。」",
                "A,A,B,tôi,em,*,one\nA,A,B,mình,bạn,*,two\n");
        assertFalse(points.stream().anyMatch(p -> p.types().contains(Type.ADDRESS)));
    }

    @Test public void missingRawBeadBecomesAnInsertionPointAfterItsDraftAnchor() {
        String rawText = "RAW one\nRAW missing\nRAW three\n";
        String draftText = "DRAFT one\nDRAFT three\n";
        PairMap map = map(rawText, draftText);
        ChunkPlan plan = new ChunkPlan("r", "d", "g", "p", new ChunkPlan.Limits("char", 10, 20, 50, 0), 2.5,
                3, 2, List.of("11:1", "10:1", "11:1"), List.of(new ChunkPlan.Chunk(0, 3, 0, 2, List.of(), false)),
                "OK", List.of(), 0, 0, 1, 0, 0);
        List<FixPoint> points = FixPointFinder.find(map, map.entries().get(0), List.of(), "", null, plan).points();
        assertEquals(1, points.size());
        assertTrue(points.get(0).insertion());
        assertEquals(0, points.get(0).draftLineIndex());
        assertEquals("RAW missing", points.get(0).rawText());
    }

    @Test public void plannedSplitGlossaryWithoutAUniqueDraftLineIsNotAssignedArbitrarily() {
        PairMap map = map("竜王が現れた。", "The dragon\nking appeared.");
        ChunkPlan plan = new ChunkPlan("r", "d", "g", "p", new ChunkPlan.Limits("char", 10, 20, 50, 0), 2.5,
                1, 2, List.of("12:1"), List.of(new ChunkPlan.Chunk(0, 1, 0, 2, List.of(), false)),
                "OK", List.of(), 0, 0, 0, 0, 0);
        FixPointFinder.Result result = FixPointFinder.find(map, map.entries().get(0), glossary(), "", null, plan);
        assertEquals(0, result.points().stream().filter(p -> p.types().contains(Type.GLOSSARY)).count());
        assertEquals(1, result.ambiguousGlossarySplits());
    }

    @Test public void promptIncludesEachPointRuleAndNeverAnOwnerFinal() {
        FixPoint p = point(1, Type.ADDRESS, 0, "DRAFT sentence", "RAW sentence");
        var prompt = TargetedFixPrompt.build("Vietnamese", "", "RAW chunk", "", "", "DRAFT chunk", "", List.of(p));
        assertTrue(prompt.user().contains("[1] TYPES: ADDRESS"));
        assertTrue(prompt.user().contains("RAW: RAW sentence"));
        assertTrue(prompt.user().contains("RULES: ADDRESS synthetic rule"));
        assertFalse(prompt.system().contains("FINAL"));
        assertFalse(prompt.user().contains("FINAL"));
    }

    @Test public void parserAcceptsWrappersMarkdownAlternativeIdsAndQuotesButIgnoresUnknownAndDuplicates() {
        List<FixPoint> points = List.of(point(1, Type.KANA, 0, "old", "RAW"), point(2, Type.KANA, 1, "older", "RAW"));
        String fence = String.valueOf((char) 96).repeat(3);
        String response = fence + "text\n</EDIT>\n**1. \"new one\"**\n(2) =\n[2] duplicate\n[9] unknown\n" + fence;
        TargetedFixParser.Parsed parsed = TargetedFixParser.parse(response, points);
        assertEquals("new one", parsed.answers().get(1).text());
        assertTrue(parsed.answers().get(2).keep());
        assertTrue(parsed.ignoredLines() >= 2);
    }

    @Test public void applierChangesOnlyTargetLineAndPreservesCrLf() {
        String source = "first\r\nsecond\r\nthird";
        FixPoint p = point(1, Type.GLOSSARY, 1, "second", "源");
        var parsed = TargetedFixParser.parse("[1] corrected", List.of(p));
        var result = TargetedFixApplier.apply(source, List.of(p), parsed, List.of());
        assertEquals("first\r\ncorrected\r\nthird", result.text());
        assertEquals(TargetedFixApplier.Status.FIXED, result.outcomes().get(0).status());
    }

    @Test public void applierInsertsAtTheDeclaredAnchorAndPreservesExistingText() {
        String source = "first\r\nthird";
        FixPoint p = new FixPoint(1, Set.of(Type.MISSING), 0, "", "RAW missing", List.of("translate"));
        var parsed = TargetedFixParser.parse("[1] second", List.of(p));
        var result = TargetedFixApplier.apply(source, List.of(p), parsed, List.of());
        assertEquals("first\r\nsecond\r\nthird", result.text());
        assertEquals(TargetedFixApplier.Status.FIXED, result.outcomes().get(0).status());
    }

    @Test public void applierRejectsMetaScriptGrowthAndMultilineAnswersWithoutChangingSource() {
        String source = "normal line";
        for (String answer : List.of("日本語", "<EDITED>x</EDITED>", "x\ny", "x".repeat(30))) {
            FixPoint p = point(1, Type.GLOSSARY, 0, source, "RAW");
            var result = TargetedFixApplier.apply(source, List.of(p), TargetedFixParser.parse("[1] " + answer, List.of(p)), List.of());
            assertEquals(source, result.text());
            assertEquals(TargetedFixApplier.Status.REJECTED, result.outcomes().get(0).status());
        }
    }

    @Test public void missingAnswerKeepsExistingAndDoesNotInventAnInsertion() {
        FixPoint old = point(1, Type.KANA, 0, "DRAFT", "RAW");
        FixPoint missing = new FixPoint(2, Set.of(Type.MISSING), -1, "", "RAW missing", List.of("translate"));
        var result = TargetedFixApplier.apply("DRAFT", List.of(old, missing), TargetedFixParser.parse("[1] =", List.of(old, missing)), List.of());
        assertEquals("DRAFT", result.text());
        assertEquals(TargetedFixApplier.Status.KEPT, result.outcomes().get(0).status());
        assertEquals(TargetedFixApplier.Status.NO_ANSWER, result.outcomes().get(1).status());
    }

    @Test public void fixPointCannotExistWithoutAnAppliedRule() {
        try {
            new FixPoint(1, Set.of(Type.ADDRESS), 0, "DRAFT", "RAW", List.of());
            fail("point without a rule must fail closed");
        } catch (IllegalArgumentException expected) {
            assertEquals("FIX_POINT_WITHOUT_RULE", expected.getMessage());
        }
    }

    @Test public void outputReserveScalesWithTheTargetNotTheWholeChapterAndIsBounded() {
        FixPoint small = point(1, Type.KANA, 0, "line", "RAW");
        int smallLimit = TargetedFixPrompt.maxOutputTokens(List.of(small));
        FixPoint large = point(2, Type.KANA, 0, "x".repeat(20_000), "RAW".repeat(5_000));
        int largeLimit = TargetedFixPrompt.maxOutputTokens(List.of(large));
        assertEquals(128, smallLimit);
        assertEquals(4096, largeLimit);
        try {
            TargetedFixPrompt.maxOutputTokens(List.of());
            fail("a no-point chunk has no output reserve");
        } catch (IllegalArgumentException expected) {
            assertEquals("TARGETED_PROMPT_NO_POINTS", expected.getMessage());
        }
    }
}
