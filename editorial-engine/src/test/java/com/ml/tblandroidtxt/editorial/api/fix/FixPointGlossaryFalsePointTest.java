package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;
import com.ml.tblandroidtxt.editorial.api.fix.FixPoint.Type;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** v4.22 step G: glossary points that are not real misses. Invented text only. */
public final class FixPointGlossaryFalsePointTest {
    private static final String HEADER = "from,speaker,target,self,call,scope,note\n";

    private static List<FixPoint> find(String raw, String draft, List<EditInputs.GlossaryEntry> glossary, String pronounRows) {
        PairMap map = PairMaps.fromJobRows("CH901", "raw", "draft", List.of(raw), List.of(draft));
        return FixPointFinder.find(map, map.entries().get(0), glossary, HEADER + pronounRows, null, null).points();
    }

    private static boolean glossaryPoint(List<FixPoint> points) {
        return points.stream().anyMatch(p -> p.types().contains(Type.GLOSSARY));
    }

    private static List<EditInputs.GlossaryEntry> one(String source, String target, String category) {
        return List.of(new EditInputs.GlossaryEntry(source, target, category, ""));
    }

    // ---- (a) a one-character source that is only part of a longer word ----

    @Test public void aOneCharacterSourceInsideAnAdjectiveOrCompoundDoesNotAskForTheName() {
        List<EditInputs.GlossaryEntry> glossary = one("白", "Shiro", "character");
        assertFalse(glossaryPoint(find("白い悪魔がいる。", "Có một con ác quỷ trắng.", glossary, "")));
        assertFalse(glossaryPoint(find("純白の部屋だ。", "Căn phòng trắng tinh.", glossary, "")));
        assertFalse(glossaryPoint(find("白魔法を使う。", "Dùng ma pháp ánh sáng.", glossary, "")));
        assertFalse(glossaryPoint(find("空は白く光った。", "Bầu trời sáng lên.", glossary, "")));
    }

    @Test public void controlTheSameSourceUsedAsAStandaloneNameStillAsksForTheName() {
        List<EditInputs.GlossaryEntry> glossary = one("白", "Shiro", "character");
        assertTrue(glossaryPoint(find("白は笑った。", "Bé con cười.", glossary, "")));
        assertTrue(glossaryPoint(find("「白、行くぞ」", "「Đi thôi」", glossary, "")));
        assertTrue(glossaryPoint(find("白ちゃんが来た。", "Bé đã đến.", glossary, "")));
        assertTrue(glossaryPoint(find("彼は白に言った。", "Người ấy nói với cô bé.", glossary, "")));
    }

    @Test public void controlALongerSourceIsNeverAffectedByTheOneCharacterRule() {
        assertTrue(glossaryPoint(find("白い悪魔がいる。", "Có một con ác quỷ.", one("白い悪魔", "Bạch Quỷ", "title"), "")));
    }

    // ---- (b) a pronoun row's RAW address wording decides that stretch ----

    @Test public void aGlossarySourceInsideAPronounRowFromIsLeftToTheRow() {
        String rows = "眷属たち,A,B,tôi,các ngươi,*,x\n";
        assertFalse(glossaryPoint(find("「眷属たちよ」", "「Hỡi các ngươi」", one("眷属", "Quyến thuộc", "concept"), rows)));
    }

    @Test public void controlWithoutThatRowTheSameSourceStillAsksForTheTerm() {
        assertTrue(glossaryPoint(find("「眷属たちよ」", "「Hỡi các ngươi」", one("眷属", "Quyến thuộc", "concept"), "")));
        // a row whose from does not contain the source does not hide it
        assertTrue(glossaryPoint(find("「眷属たちよ」", "「Hỡi các ngươi」", one("眷属", "Quyến thuộc", "concept"), "貴様,A,B,tôi,ngươi,*,x\n")));
    }

    // ---- (c) a character already referred to by a third-person pronoun ----

    @Test public void aCharacterReferredToByAThirdPersonPronounIsNotForcedIntoTheName() {
        List<EditInputs.GlossaryEntry> glossary = one("ポティマス", "Potimas", "character");
        for (String draft : List.of("Hắn hoàn toàn không chối bỏ.", "Anh ta nói vậy.", "Cô ấy đã đến.", "Lão ta cười.", "Gã đứng dậy.")) {
            assertFalse(draft, glossaryPoint(find("ポティマスは言った。", draft, glossary, "")));
        }
    }

    @Test public void controlAMissingNameStillCreatesAPoint() {
        List<EditInputs.GlossaryEntry> glossary = one("ポティマス", "Potimas", "character");
        assertTrue(glossaryPoint(find("ポティマスは言った。", "Ông đã nói.", glossary, "")));
        assertTrue(glossaryPoint(find("ポティマスは言った。", "Tên đó đã nói.", glossary, "")));
        assertFalse(glossaryPoint(find("ポティマスは言った。", "Potimas đã nói.", glossary, "")));
    }

    @Test public void controlAWrongNameBesideAPronounForAnotherPersonStillCreatesAPoint() {
        List<EditInputs.GlossaryEntry> glossary = one("巳剣", "Mitsurugi", "character");
        assertTrue(glossaryPoint(find("巳剣が言った。", "Khi đó, rất có thể Fuyu sẽ nói với cậu ấy.", glossary, "")));
    }

    @Test public void controlARowFromInNarrationDoesNotHideTheGlossaryName() {
        String rows = "日屋見さん,A,B,tôi,Ruka,*,x\n";
        assertTrue(glossaryPoint(find("日屋見さんは黙った。", "Ruka im lặng.", one("日屋見", "Hiyami", "character"), rows)));
    }

    @Test public void controlThePronounExceptionIsOnlyForCharacterEntries() {
        assertTrue(glossaryPoint(find("魔王は言った。", "Hắn đã nói.", one("魔王", "Ma Vương", "title"), "")));
        assertTrue(glossaryPoint(find("魔王は言った。", "Hắn đã nói.", one("魔王", "Ma Vương", ""), "")));
    }
}
