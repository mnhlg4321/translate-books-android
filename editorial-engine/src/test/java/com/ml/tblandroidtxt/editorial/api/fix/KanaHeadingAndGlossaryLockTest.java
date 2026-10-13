package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * v4.20 E, chapter 017: the heading "○一七" made a KANA point and the answer replaced the correct glossary term of the heading.
 * Invented heading and term only (アークナイト → Arknight).
 */
public final class KanaHeadingAndGlossaryLockTest {
    private static final String HEADER = "from,speaker,target,self,call,scope,note\n";
    private static final List<EditInputs.GlossaryEntry> GLOSSARY = List.of(new EditInputs.GlossaryEntry("アークナイト", "Arknight", "title", ""));

    private static List<FixPoint> find(String raw, String draft) {
        PairMap map = PairMaps.fromJobRows("CH017", "raw", "draft", List.of(raw), List.of(draft));
        return FixPointFinder.find(map, map.entries().get(0), GLOSSARY, HEADER, null, null).points();
    }

    @Test public void aChapterHeadingNumberCopiedFromRawIsNotUntranslatedText() {
        assertTrue(find("○一七　孤独なアークナイト", "○一七　Arknight cô độc").isEmpty());
    }

    @Test public void kanaLeftInTheLineIsStillAKanaPoint() {
        List<FixPoint> points = find("○一七　孤独なアークナイト", "○一七　Arknight こどく");
        assertEquals(1, points.size());
        assertTrue(points.get(0).types().contains(FixPoint.Type.KANA));
        assertFalse("the rule is written in the target language", points.get(0).rules().get(0).matches(".*[\\u0400-\\u04FF].*"));
    }

    @Test public void hanNumeralsThatAreNotInTheRawAreStillFlagged() {
        List<FixPoint> points = find("孤独なアークナイト", "一七 Arknight cô độc");
        assertEquals(1, points.size());
        assertTrue(points.get(0).types().contains(FixPoint.Type.KANA));
    }

    @Test public void anAnswerThatDropsACorrectGlossaryTermKeepsTheDraftLineWithoutBlockingTheChunk() {
        String before = "○一七　Arknight cô độc\nĐã về.";
        FixPoint heading = new FixPoint(1, Set.of(FixPoint.Type.KANA), 0, "○一七　Arknight cô độc", "○一七　孤独なアークナイト", List.of("KANA rule"));
        FixPoint other = new FixPoint(2, Set.of(FixPoint.Type.KANA), 1, "Đã về.", "帰った。", List.of("KANA rule"));
        var points = List.of(heading, other);
        var result = TargetedFixApplier.apply(before, points,
                TargetedFixParser.parse("[1] ○一七　Hiệp sĩ cô độc\n[2] Đã về rồi.", points), GLOSSARY);
        assertEquals(TargetedFixApplier.Status.KEPT, result.outcomes().get(0).status());
        assertEquals(TargetedFixApplier.GLOSSARY_TARGET_REMOVED, result.outcomes().get(0).reason());
        assertEquals(TargetedFixApplier.Status.FIXED, result.outcomes().get(1).status());
        assertEquals("○一七　Arknight cô độc\nĐã về rồi.", result.text());
    }

    @Test public void twoGlossaryLockKeepsAreNotAMalformedResponse() {
        String before = "Arknight đến.\nArknight đi.";
        FixPoint a = new FixPoint(1, Set.of(FixPoint.Type.KANA), 0, "Arknight đến.", "アークナイトが来た。", List.of("KANA rule"));
        FixPoint b = new FixPoint(2, Set.of(FixPoint.Type.KANA), 1, "Arknight đi.", "アークナイトが去った。", List.of("KANA rule"));
        var points = List.of(a, b);
        var result = TargetedFixApplier.apply(before, points, TargetedFixParser.parse("[1] Hiệp sĩ đến.\n[2] Hiệp sĩ đi.", points), GLOSSARY);
        assertEquals(TargetedFixApplier.Status.KEPT, result.outcomes().get(0).status());
        assertEquals(TargetedFixApplier.Status.KEPT, result.outcomes().get(1).status());
        assertEquals(before, result.text());
    }

    @Test public void keepingTheTermWhileRepairingTheRestStillApplies() {
        FixPoint p = new FixPoint(1, Set.of(FixPoint.Type.KANA), 0, "Arknight こどく", "孤独なアークナイト", List.of("KANA rule"));
        var result = TargetedFixApplier.apply("Arknight こどく", List.of(p), TargetedFixParser.parse("[1] Arknight cô độc", List.of(p)), GLOSSARY);
        assertEquals(TargetedFixApplier.Status.FIXED, result.outcomes().get(0).status());
        assertEquals("Arknight cô độc", result.text());
    }
}
