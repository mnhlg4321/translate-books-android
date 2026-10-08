package com.ml.tblandroidtxt.editorial.api.chunk;

import com.ml.tblandroidtxt.editorial.api.EditInputs;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** S2 (units that rebuild the file byte for byte) and the alignment anchors. */
public final class LineUnitsAndAnchorsTest {
    private static String rebuilt(LineUnits u) { return u.slice(0, u.size()); }

    @Test public void unitsRebuildTheFileForLfCrlfMixedAndLoneCr() {
        String[] inputs = {
                "一行目\n\n二行目\n三行目\n",
                "一行目\r\n\r\n二行目\r\n三行目\r\n",
                "一行目\r\n\n二行目\n\r三行目\r\n\r\n\r\n",
                "一行目\r\r二行目\r",
                "\n\n  \n一行目\n",
                "一行目",
                "一行目\n   \n　\n二行目",
        };
        for (String in : inputs) {
            LineUnits u = LineUnits.parse(in);
            assertEquals(in, rebuilt(u));
            // any split between two units also rebuilds
            for (int cut = 0; cut <= u.size(); cut++) assertEquals(in, u.slice(0, cut) + u.slice(cut, u.size()));
        }
    }

    @Test public void blankLinesStayWithTheUnitBeforeThemAndLeadingBlanksWithTheFirstUnit() {
        LineUnits u = LineUnits.parse("\n \n一\n　\n\n二\n");
        assertEquals(2, u.size());
        assertEquals("\n \n一\n　\n\n", u.slice(0, 1));
        assertEquals("二\n", u.slice(1, 2));
        assertEquals("一", u.units().get(0).text());
        assertEquals(3, u.units().get(0).physicalLine());
        assertEquals(6, u.units().get(1).physicalLine());
    }

    @Test public void aLineOfOnlyIdeographicSpaceOrNbspIsBlankButAnyOtherCharIsNot() {
        assertEquals(1, LineUnits.parse("　　\n本文\n \n").size());
        assertEquals(2, LineUnits.parse("・\n本文\n").size());
        assertEquals("本文", LineUnits.strip("　  本文 "));
        assertFalse(LineUnits.isSpace('﻿'));
        assertEquals(0, LineUnits.parse("").size());
        assertEquals(0, LineUnits.parse(null).size());
    }

    @Test public void lfTextMapsBackToOriginalOffsets() {
        String in = "あ\r\nい\rう\nえ";
        LineUnits u = LineUnits.parse(in);
        assertEquals("あ\nい\nう\nえ", u.lfText());
        String lf = u.lfText();
        for (int i = 0; i < lf.length(); i++) {
            if (lf.charAt(i) == '\n') continue;
            assertEquals(lf.charAt(i), in.charAt(u.originalOffset(i)));
        }
    }

    @Test public void unitTextIsStrippedAndAnalysisJoinsWithLf() {
        LineUnits u = LineUnits.parse("　　「台詞」  \n地の文\n");
        assertEquals(List.of("「台詞」", "地の文"), u.texts());
        assertEquals("「台詞」\n地の文", u.analysis(0, 2));
        assertEquals(4, u.units().get(0).codePoints());
    }

    // ---- anchors

    private static final List<Anchors.Term> TERMS = Anchors.terms(List.of(
            new EditInputs.GlossaryEntry("魔王", " Ma Vương ", "", ""),
            new EditInputs.GlossaryEntry("勇者", "Dũng giả", "", ""),
            new EditInputs.GlossaryEntry("", "bỏ", "", ""),
            new EditInputs.GlossaryEntry("空", "", "", "")));

    @Test public void termsKeepOnlyCompleteEntriesAndLowerCaseTheTarget() {
        assertEquals(2, TERMS.size());
        assertEquals("ma vương", TERMS.get(0).targetLower());
        assertEquals(Set.of("ma vương", "dũng giả"), Anchors.targets(TERMS));
    }

    @Test public void rawAnchorsAreTargetsOfSourcesThatOccurPlusDigits() {
        assertEquals(Set.of("ma vương", "3"), Anchors.raw("魔王は３人いた", TERMS));
        assertEquals(Set.of(), Anchors.raw("何もない", TERMS));
    }

    @Test public void draftAnchorsMatchTargetsIgnoringCaseAndDigitsAfterFullWidthFolding() {
        assertEquals(Set.of("ma vương", "3"), Anchors.draft("MA VƯƠNG có ３ người", TERMS));
        assertEquals(Set.of("dũng giả", "12"), Anchors.draft("dũng giả 12", TERMS));
    }

    @Test public void cjkNumeralsAreNotDigits() {
        assertEquals(Set.of(), Anchors.numbers("三人と十二匹"));
        assertEquals(Set.of("2025", "7"), Anchors.numbers("２０２５年7月"));
    }

    @Test public void aSharedAnchorIsInTheIntersectionAndADifferentOneIsNot() {
        Set<String> a = Anchors.raw("勇者が来た", TERMS);
        assertTrue(Anchors.draft("Dũng giả đến", TERMS).containsAll(a));
        assertFalse(Anchors.draft("Ma Vương đến", TERMS).containsAll(a));
    }
}
