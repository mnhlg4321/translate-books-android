package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** A glossary source inside a longer glossary source belongs to the longer entry. Synthetic terms only. */
public final class FixPointGlossaryOverlapTest {
    private static final List<EditInputs.GlossaryEntry> GLOSSARY = List.of(
            new EditInputs.GlossaryEntry("竜王隊", "Đội Rồng Vàng", "name", ""),
            new EditInputs.GlossaryEntry("竜王", "Long Vương", "name", ""),
            new EditInputs.GlossaryEntry("王", "vua", "word", ""));

    private static List<FixPoint> points(String raw, String draft) {
        PairMap map = PairMaps.fromJobRows("CH001", "raw", "draft", List.of(raw), List.of(draft));
        return FixPointFinder.find(map, map.entries().get(0), GLOSSARY, "", null, null).points();
    }

    private static long glossaryRules(List<FixPoint> points, String source) {
        long n = 0;
        for (FixPoint p : points) for (String rule : p.rules()) if (rule.startsWith("GLOSSARY") && rule.contains("'" + source + "'")) n++;
        return n;
    }

    @Test public void aShortEntryInsideALongerOneWhoseTargetTheDraftHasMakesNoPoint() {
        List<FixPoint> found = points("竜王隊が現れた。", "Đội Rồng Vàng đã xuất hiện.");
        assertTrue("no point at all: " + found, found.isEmpty());
    }

    @Test public void theLongerEntryIsStillCheckedAndTheShorterOnesStayQuiet() {
        List<FixPoint> found = points("竜王隊が現れた。", "Đội rồng đã xuất hiện.");
        assertEquals(1, glossaryRules(found, "竜王隊"));
        assertEquals(0, glossaryRules(found, "竜王"));
        assertEquals(0, glossaryRules(found, "王"));
    }

    @Test public void aStandaloneOccurrenceOfTheShortEntryIsStillChecked() {
        List<FixPoint> found = points("竜王隊の隣に竜王がいた。", "Đội Rồng Vàng đứng cạnh con rồng.");
        assertEquals(0, glossaryRules(found, "竜王隊"));
        assertEquals(1, glossaryRules(found, "竜王"));
        assertEquals("王 only occurs inside the two longer entries", 0, glossaryRules(found, "王"));
    }

    @Test public void aBareShortEntryOutsideEveryLongerOneIsChecked() {
        List<FixPoint> found = points("竜王隊の王が笑った。", "Đội Rồng Vàng cười.");
        assertEquals(1, glossaryRules(found, "王"));
        assertEquals(0, glossaryRules(found, "竜王"));
    }

    @Test public void aShortEntryAloneIsCheckedAsBefore() {
        List<FixPoint> found = points("竜王が現れた。", "Rồng đã xuất hiện.");
        assertEquals(1, glossaryRules(found, "竜王"));
        assertEquals(0, glossaryRules(found, "竜王隊"));
    }
}
