package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * v4.20 C1: an ADDRESS answer names the pronoun row it applies ({@code [id] R<n> <line>}). Synthetic characters only (Aki, Mori, Ken,
 * Rina); the cases mirror the first-person errors of the owner's chapters: a self word taken from another speaker's row.
 */
public final class AddressRowCitationTest {
    private static final List<AddressRow> ROWS = List.of(
            new AddressRow("森先生", "Aki", "Mori", "em", "cô", "Aki nói với giáo viên"),
            new AddressRow("アキさん", "Ken", "Aki", "tôi", "cô"),
            new AddressRow("リナ", "Aki", "Rina", "mình", "Rina"));

    private static FixPoint address(String draft, String raw) {
        return new FixPoint(1, Set.of(FixPoint.Type.ADDRESS), 0, draft, raw, List.of("ADDRESS synthetic rule"), ROWS);
    }

    private static TargetedFixApplier.Outcome apply(FixPoint point, String answer) {
        var result = TargetedFixApplier.apply(point.draftText(), List.of(point), TargetedFixParser.parse(answer, List.of(point)), List.of());
        return result.outcomes().get(0);
    }

    @Test public void theParserReadsARowNumberOnlyForAPointWithRows() {
        FixPoint rows = address("「Tôi muốn học」", "「覚えたい」");
        FixPoint kana = new FixPoint(2, Set.of(FixPoint.Type.KANA), 1, "old", "RAW", List.of("KANA rule"));
        var points = List.of(rows, kana);
        var cited = TargetedFixParser.parse("[1] R2 「Em muốn học」\n[2] R2 là chữ của sách", points).answers();
        assertEquals(2, cited.get(1).row());
        assertEquals("「Em muốn học」", cited.get(1).text());
        assertEquals("a leading R2 of a point without rows is book text", "R2 là chữ của sách", cited.get(2).text());
        assertEquals(TargetedFixParser.Answer.NOT_CITED, cited.get(2).row());
        assertEquals(TargetedFixParser.Answer.INVALID_ROW, TargetedFixParser.parse("[1] R9 「Em muốn học」", points).answers().get(1).row());
        assertTrue(TargetedFixParser.parse("[1] R1 =", points).answers().get(1).keep());
        assertEquals(TargetedFixParser.Answer.NOT_CITED, TargetedFixParser.parse("[1] 「Em muốn học」", points).answers().get(1).row());
    }

    @Test public void aSelfTakenFromAnotherSpeakersRowIsRepairedWhenTheAnswerCitesTheSpeakersRow() {
        // Aki speaks to her teacher (vocative 森先生); the DRAFT used Ken's self "tôi"
        FixPoint point = address("「Cô ơi, tôi muốn học cái đó」", "「森先生、それ、覚えたいです」");
        TargetedFixApplier.Outcome outcome = apply(point, "[1] R1 「Cô ơi, em muốn học cái đó」");
        assertEquals(TargetedFixApplier.Status.FIXED, outcome.status());
        assertEquals("「Cô ơi, em muốn học cái đó」", outcome.after());
    }

    @Test public void anAddressChangeWithoutARowNumberKeepsTheDraft() {
        FixPoint point = address("「Cô ơi, tôi muốn học cái đó」", "「森先生、それ、覚えたいです」");
        TargetedFixApplier.Outcome outcome = apply(point, "[1] 「Cô ơi, em muốn học cái đó」");
        assertEquals(TargetedFixApplier.Status.KEPT, outcome.status());
        assertEquals("ADDRESS_ROW_NOT_CITED", outcome.reason());
        assertEquals("ADDRESS_ROW_CITATION_INVALID", apply(point, "[1] R9 「Cô ơi, em muốn học cái đó」").reason());
    }

    @Test public void onlyTheCitedRowsFormsMayBeAddedAndNoneOfThemRemoved() {
        FixPoint plain = address("「Tôi muốn học cái đó」", "「それ、覚えたいです」");
        assertNull(AddressEditGuard.rejection(plain, "「Em muốn học cái đó」", 1));
        // Ken's row has no "em": citing it cannot justify the change
        assertEquals("ADDRESS_FORM_NOT_IN_CITED_ROW", AddressEditGuard.rejection(plain, "「Em muốn học cái đó」", 2));
        // a form of Aki's row to Rina while citing her row to the teacher
        assertEquals("ADDRESS_FORM_NOT_IN_CITED_ROW", AddressEditGuard.rejection(plain, "「Mình muốn học cái đó」", 1));
        FixPoint swapped = address("「Em nhờ cô」", "「先生に頼む」");
        assertEquals("ADDRESS_VALID_FORM_CHANGED", AddressEditGuard.rejection(swapped, "「Cô nhờ em」", 1));
    }

    @Test public void theCitedRowMustAgreeWithTheRawEvidence() {
        // the vocative 森先生 names the teacher as listener: Aki's row to Rina contradicts it
        FixPoint vocative = address("「Cô ơi, tôi muốn học cái đó」", "「森先生、それ、覚えたいです」");
        assertEquals("ADDRESS_CITED_ROW_NOT_ADDRESSED", AddressEditGuard.rejection(vocative, "「Cô ơi, mình muốn học cái đó」", 3));
        // 森先生が: the teacher is talked about, not to, and Aki is not otherwise in the scene
        FixPoint third = address("「Cô ấy đến rồi, tôi về đây」", "「森先生が来た、私は帰る」");
        assertEquals("ADDRESS_CITED_ROW_EXCLUDED", AddressEditGuard.rejection(third, "「Cô ấy đến rồi, em về đây」", 1));
    }

    @Test public void aGlossaryRepairInAMixedPointNeedsNoRowNumber() {
        FixPoint mixed = new FixPoint(1, Set.of(FixPoint.Type.ADDRESS, FixPoint.Type.GLOSSARY), 0, "「Cậu thấy Rồng vương chưa」", "「竜王は？」",
                List.of("ADDRESS rule", "GLOSSARY rule"), ROWS);
        var result = TargetedFixApplier.apply(mixed.draftText(), List.of(mixed),
                TargetedFixParser.parse("[1] 「Cậu thấy Long Vương chưa」", List.of(mixed)), List.of(new EditInputs.GlossaryEntry("竜王", "Long Vương", "name", "")));
        assertEquals(TargetedFixApplier.Status.FIXED, result.outcomes().get(0).status());
    }

    @Test public void thePromptNumbersTheRowsWithTheOwnersNoteAndFoldsTheRepeatedAddressRules() {
        String header = "from,speaker,target,self,call,scope,note\n";
        PairMap map = PairMaps.fromJobRows("CH001", "raw", "draft", List.of("「AはEに言った。」"), List.of("「Cậu sẽ đi。」"));
        List<FixPoint> points = FixPointFinder.find(map, map.entries().get(0), List.of(),
                header + "A,A,B,tôi,em,*,giữ khoảng cách\nE,E,F,em,em,*,same word\n", null, null).points();
        String user = TargetedFixPrompt.build("Vietnamese", "", "RAW", "", "", "DRAFT", "", points).user();
        assertTrue(user, user.contains("ROWS:\nR1 | A | A → B | self=tôi | call=em | giữ khoảng cách\nR2 | E | E → F | self=em | call=em | same word\n"));
        assertTrue(user, user.contains("RULES: ADDRESS: DRAFT address words to check against the speaker's row: cậu\n"));
        assertFalse("the per-row rule text is not repeated", user.contains("Pronoun row"));
    }

    @Test public void aPointWithoutRowsKeepsItsRulesAsWritten() {
        FixPoint noRows = new FixPoint(1, Set.of(FixPoint.Type.ADDRESS), 0, "「Tớ đến」", "「来た」", List.of("ADDRESS legacy rule"));
        String user = TargetedFixPrompt.build("Vietnamese", "", "RAW", "", "", "DRAFT", "", List.of(noRows)).user();
        assertTrue(user.contains("RULES: ADDRESS legacy rule"));
        assertFalse(user.contains("ROWS:"));
    }
}
