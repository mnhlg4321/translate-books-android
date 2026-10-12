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

/** Synthetic characters only: Aki, Rina, Ken, Mori, Sora, Mio. No chapter text. */
public final class AddressEditGuardTest {
    private static final List<AddressRow> ROWS = List.of(
            new AddressRow("森先生", "Aki", "Mori", "em", "cô"),
            new AddressRow("ケン先輩", "Aki", "Ken", "em", "anh Ken"),
            new AddressRow("リナ", "Aki", "Rina", "mình", "Rina"),
            new AddressRow("アキ", "Rina", "Aki", "mình", "Aki"),
            new AddressRow("アキさん", "Ken", "Aki", "tôi", "cô"),
            new AddressRow("秋", "Sora", "Aki", "tôi", "cậu"),
            new AddressRow("私", "@NARRATOR", "@AUDIENCE", "tôi", ""),
            new AddressRow("ミオ", "Mio", "*", "Mio", ""));

    private static FixPoint address(String draft, String raw) {
        return new FixPoint(1, Set.of(FixPoint.Type.ADDRESS), 0, draft, raw, List.of("ADDRESS synthetic rule"), ROWS);
    }

    private static String reason(String draft, String raw, String candidate) {
        return AddressEditGuard.rejection(address(draft, raw), candidate);
    }

    @Test public void aRepairWithOnlySceneEvidenceStillApplies() {
        assertNull(reason("「Tớ muốn học cái đó」", "「それ、覚えたい」", "「Mình muốn học cái đó」"));
    }

    @Test public void aVocativeFromTermPinsTheRowSoOtherRowsFormsAreRejected() {
        String raw = "「リナ……お願い、覚えさせて」";
        String draft = "「Rina…… làm ơn, giúp tớ nhé」";
        assertNull(reason(draft, raw, "「Rina…… làm ơn, giúp mình nhé」"));
        assertEquals("ADDRESS_FORM_NOT_IN_APPLICABLE_ROW", reason(draft, raw, "「Rina…… làm ơn, giúp em nhé」"));
    }

    @Test public void aFromTermUsedAsAThirdPersonRemovesThatPersonAsSpeakerAndAddressee() {
        String raw = "「リナが助けてくれたから、あたしは生きてる」";
        String draft = "「Rina đã cứu tớ nên tớ còn sống」";
        assertEquals("ADDRESS_FORM_NOT_IN_APPLICABLE_ROW", reason(draft, raw, "「Rina đã cứu mình nên mình còn sống」"));
        // the same words are fine once the person is addressed instead of mentioned
        assertNull(reason(draft, "「リナ、助けてくれたから、あたしは生きてる」", "「Rina đã cứu mình nên mình còn sống」"));
    }

    @Test public void aTopicParticleIsAlsoAThirdPersonUse() {
        assertEquals("ADDRESS_FORM_NOT_IN_APPLICABLE_ROW",
                reason("「Tớ nói rồi, Rina giỏi lắm」", "「だからリナは凄いって言ったのに」", "「Mình nói rồi, Rina giỏi lắm」"));
    }

    @Test public void aPluralAudienceHasNoRowSoAddressWordsStay() {
        assertEquals("ADDRESS_AMBIGUOUS_AUDIENCE", reason("「Tôi sẽ bảo vệ các tiền bối」", "「先輩たちのことも守る」", "「Em sẽ bảo vệ các tiền bối」"));
        assertEquals("ADDRESS_AMBIGUOUS_AUDIENCE", reason("「Tôi sẽ bảo vệ mọi người」", "「みんなを守る」", "「Em sẽ bảo vệ mọi người」"));
    }

    @Test public void aWordAlreadyValidForThePinnedRowIsNotChangedAndAForeignOneIsNotAdded() {
        String raw = "「アキさん、戦えるでしょう？」";
        String draft = "「Aki, cô chiến đấu được không?」";
        assertEquals("ADDRESS_VALID_FORM_CHANGED", reason(draft, raw, "「Aki, tôi chiến đấu được không?」"));
        assertEquals("ADDRESS_FORM_NOT_IN_APPLICABLE_ROW", reason(draft, raw, "「Aki, cậu chiến đấu được không?」"));
    }

    @Test public void theLongerFromTermWinsWhereTwoTermsOverlap() {
        // アキ is inside アキさん; only the longer term is evidence, so the アキ row (mình/Aki) is not pinned
        assertEquals("ADDRESS_FORM_NOT_IN_APPLICABLE_ROW",
                reason("「Aki, tớ đang ở đây」", "「アキさん、あたしはここ」", "「Aki, mình đang ở đây」"));
    }

    @Test public void aLineThatAlreadyFitsOneRowMustStillFitOneRow() {
        String raw = "「提案したのはあなたの方です、後悔しても知りませんよ」";
        String draft = "「Chính cô là người đề nghị, đừng trách tôi」";
        assertEquals("ADDRESS_BREAKS_CONSISTENT_ROW", reason(draft, raw, "「Chính em là người đề nghị, đừng trách tôi」"));
        assertNull(reason(draft, raw, "「Chính cô là người đề nghị, đừng trách em」"));
    }

    @Test public void aSingleAddressWordIsNotEnoughToCallTheLineConsistent() {
        // "tôi" alone fits several rows, so the partial repair of the call word (tiền bối -> anh Ken) is not undone by the lock
        assertNull(reason("Chuyện tiền bối có tin không, tôi không quan tâm", "「先輩が納得するかは関係ない」", "Chuyện anh Ken có tin không, tôi không quan tâm"));
    }

    @Test public void aNameIsNeverSwappedForAnotherName() {
        assertEquals("ADDRESS_NAME_SWAPPED", reason("「Mio, em hơi sợ người đó」", "「ミオ、あの人が怖い」", "「Rina, em hơi sợ người đó」"));
    }

    @Test public void onlyAddressWordsMayChangeInAnAddressOnlyPoint() {
        assertEquals("ADDRESS_SCOPE_EXCEEDED", reason("「Tớ nghĩ giống cậu ấy」", "「あたしも同感」", "「Mình đồng ý với cậu ấy」"));
        assertEquals("ADDRESS_SCOPE_EXCEEDED", reason("「Tớ đến rồi.」", "「来たよ」", "「Mình đến rồi!」"));
        assertEquals("ADDRESS_SCOPE_EXCEEDED", reason("「Tớ đã nói rồi」", "「言ったよ」", "「Mình đã bảo rồi」"));
    }

    @Test public void aTitleMayMoveWithItsAddressWordAndAPronounMayBeDropped() {
        assertNull(reason("Chuyện tiền bối có tin hay không không liên quan đến tôi", "「先輩が納得するかは関係ない」",
                "Chuyện anh có tin hay không không liên quan đến em"));
        assertNull(reason("「Tớ đã nói rồi」", "「言ったよ」", "「Đã nói rồi」"));
    }

    @Test public void aMixedPointKeepsTheDirectionChecksButNotTheWordingLock() {
        FixPoint mixed = new FixPoint(1, Set.of(FixPoint.Type.ADDRESS, FixPoint.Type.GLOSSARY), 0, "「Cậu thấy Rồng vương chưa」", "「竜王は？」",
                List.of("ADDRESS rule", "GLOSSARY rule"), ROWS);
        assertNull(AddressEditGuard.rejection(mixed, "「Cậu thấy Long Vương chưa」"));
        assertNull(AddressEditGuard.rejection(mixed, "「Thấy Long Vương chưa」"));
        FixPoint plural = new FixPoint(1, Set.of(FixPoint.Type.ADDRESS, FixPoint.Type.GLOSSARY), 0, "「Tôi thấy Rồng vương」", "「先輩たち、竜王を見た」",
                List.of("ADDRESS rule", "GLOSSARY rule"), ROWS);
        assertEquals("ADDRESS_AMBIGUOUS_AUDIENCE", AddressEditGuard.rejection(plural, "「Em thấy Long Vương」"));
    }

    @Test public void pointsWithoutRowsOrWithoutAddressAreNotChecked() {
        FixPoint noRows = new FixPoint(1, Set.of(FixPoint.Type.ADDRESS), 0, "「Tớ đến」", "「来た」", List.of("rule"));
        assertNull(AddressEditGuard.rejection(noRows, "「Hoàn toàn khác」"));
        FixPoint glossary = new FixPoint(1, Set.of(FixPoint.Type.GLOSSARY), 0, "Rồng vương", "竜王", List.of("rule"), ROWS);
        assertNull(AddressEditGuard.rejection(glossary, "Long Vương"));
    }

    @Test public void theApplierKeepsTheDraftLineAndReportsTheLockReasonWhileAnIndependentRepairStillApplies() {
        String before = "「Tớ nghĩ giống cậu ấy」\nRồng vương đã về.";
        FixPoint address = address("「Tớ nghĩ giống cậu ấy」", "「あたしも同感」");
        FixPoint term = new FixPoint(2, Set.of(FixPoint.Type.GLOSSARY), 1, "Rồng vương đã về.", "竜王", List.of("GLOSSARY rule"));
        var points = List.of(address, term);
        var result = TargetedFixApplier.apply(before, points,
                TargetedFixParser.parse("[1] 「Mình đồng ý với cậu ấy」\n[2] Long Vương đã về.", points),
                List.of(new EditInputs.GlossaryEntry("竜王", "Long Vương", "name", "")));
        assertEquals("「Tớ nghĩ giống cậu ấy」\nLong Vương đã về.", result.text());
        assertEquals(TargetedFixApplier.Status.KEPT, result.outcomes().get(0).status());
        assertEquals("ADDRESS_SCOPE_EXCEEDED", result.outcomes().get(0).reason());
        assertEquals(TargetedFixApplier.Status.FIXED, result.outcomes().get(1).status());
    }

    @Test public void theApplierKeepsAnAddressOnlyRepairByteForByteWithItsQuotesAndSymbols() {
        String before = "「Rina…… làm ơn, giúp tớ nhé?!」";
        FixPoint p = address(before, "「リナ……お願い、覚えさせて？！」");
        var result = TargetedFixApplier.apply(before, List.of(p), TargetedFixParser.parse("[1] 「Rina…… làm ơn, giúp mình nhé?!」", List.of(p)), List.of());
        assertEquals("「Rina…… làm ơn, giúp mình nhé?!」", result.text());
        assertEquals(TargetedFixApplier.Status.FIXED, result.outcomes().get(0).status());
    }

    @Test public void theFinderAttachesEveryTableRowInScopeAndSurvivesARowWhoseSelfEqualsItsCall() {
        String header = "from,speaker,target,self,call,scope,note\n";
        PairMap map = PairMaps.fromJobRows("CH001", "raw", "draft", List.of("「AはEに言った。」"), List.of("「Cậu sẽ đi。」"));
        List<FixPoint> points = FixPointFinder.find(map, map.entries().get(0), List.of(),
                header + "A,A,B,tôi,em,*,in scope\nC,C,D,mình,bạn,p9,out of scope\nE,E,F,em,em,*,same word\n", null, null).points();
        assertEquals(1, points.size());
        List<AddressRow> rows = points.get(0).addressRows();
        assertTrue(rows.contains(new AddressRow("A", "A", "B", "tôi", "em")));
        assertTrue("a row whose cue is not in the chunk still counts as evidence", rows.contains(new AddressRow("E", "E", "F", "em", "em")));
        assertFalse(rows.stream().anyMatch(r -> r.from().equals("C")));
    }
}
