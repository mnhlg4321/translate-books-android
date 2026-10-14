package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** P2 (owner 2026-10-13): a DRAFT line follows its RAW for 〝〟 「」 『』, ： and ……. Invented sentences only. */
public final class RawPunctuationTest {
    @Test public void quotesTakeTheRawBracketsInOrder() {
        assertEquals("「Đi thôi」", RawPunctuation.restore("“Đi thôi”", "「行こう」"));
        assertEquals("〝Ghi chú〟", RawPunctuation.restore("“Ghi chú”", "〝メモ〟"));
        assertEquals("「Một」 và 『Hai』", RawPunctuation.restore("“Một” và “Hai”", "「一」と『二』"));
        // nested quotes cannot be matched position by position: the line is left as it is
        assertEquals("“A nói “B” rồi”", RawPunctuation.restore("“A nói “B” rồi”", "「Aが『B』と言った」"));
        assertEquals("「Thẳng」", RawPunctuation.restore("\"Thẳng\"", "「まっすぐ」"));
    }

    @Test public void nothingChangesWhenTheMarksCannotBeMatchedOneToOne() {
        assertEquals("“Một” và “Hai”", RawPunctuation.restore("“Một” và “Hai”", "「一と二」"));
        assertEquals("Không có ngoặc", RawPunctuation.restore("Không có ngoặc", "「括弧」"));
        assertEquals("“Đã có” 「một」", RawPunctuation.restore("“Đã có” 「một」", "「ある」「一」"));
        assertEquals("“Lẻ", RawPunctuation.restore("“Lẻ", "「奇数」"));
        assertEquals("Giờ 10:30 và: thêm", RawPunctuation.restore("Giờ 10:30 và: thêm", "時刻：十時半"));
    }

    @Test public void colonAndEllipsisFollowTheRaw() {
        assertEquals("Cấp độ：5", RawPunctuation.restore("Cấp độ: 5", "レベル：5"));
        assertEquals("「Ừm…… được」", RawPunctuation.restore("“Ừm... được”", "「うーん……いいよ」"));
        assertEquals("Ừm... được", RawPunctuation.restore("Ừm... được", "うーん。いいよ"));
    }

    @Test public void lettersNeverChange() {
        String draft = "“Một: hai... ba”";
        String restored = RawPunctuation.restore(draft, "「一：二……三」");
        assertEquals("「Một：hai…… ba」", restored);
        assertEquals(draft.replaceAll("[^\\p{L}]", ""), restored.replaceAll("[^\\p{L}]", ""));
    }

    private static final String HEADER = "from,speaker,target,self,call,scope,note\n";

    @Test public void theFinderListsAMarkRepairWithoutAPointForTheModel() {
        PairMap map = PairMaps.fromJobRows("CH021", "raw", "draft", List.of("「行こう」"), List.of("“Đi thôi”"));
        FixPointFinder.Result result = FixPointFinder.find(map, map.entries().get(0), List.of(), HEADER, null, null);
        assertTrue(result.points().isEmpty());
        assertEquals(1, result.punctuation().size());
        assertEquals("「Đi thôi」", TargetedFixApplier.apply("“Đi thôi”", List.of(), null, List.of(), result.punctuation()).text());
    }

    @Test public void theMarksAreRestoredOnTheModelsAnswerAndOnLinesWithoutAPoint() {
        String draft = "“Rồng vương đến”\n“Đi thôi”";
        FixPoint term = new FixPoint(1, Set.of(FixPoint.Type.GLOSSARY), 0, "“Rồng vương đến”", "「竜王が来た」", List.of("GLOSSARY rule"));
        var repairs = List.of(new FixPointFinder.PunctuationRepair(0, "「竜王が来た」"), new FixPointFinder.PunctuationRepair(1, "「行こう」"));
        var result = TargetedFixApplier.apply(draft, List.of(term), TargetedFixParser.parse("[1] “Long Vương đến”", List.of(term)),
                List.of(new EditInputs.GlossaryEntry("竜王", "Long Vương", "name", "")), repairs);
        assertEquals("「Long Vương đến」\n「Đi thôi」", result.text());
        assertEquals(2, result.punctuationLines());
        // the model itself may not change the marks: its answer is checked against the DRAFT's own quotes
        var changed = TargetedFixApplier.apply(draft, List.of(term), TargetedFixParser.parse("[1] 「Long Vương đến」", List.of(term)),
                List.of(new EditInputs.GlossaryEntry("竜王", "Long Vương", "name", "")), repairs);
        assertEquals("PROTECTED_SYMBOL_CHANGE", changed.outcomes().get(0).reason());
        assertEquals("「Rồng vương đến」\n「Đi thôi」", changed.text());
    }
}
