package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The edit-answer parser, the check-answer parser and the fix applier. Synthetic text only. */
public final class EditorialApiParsersTest {
    // ---- edit answer ----

    @Test public void fullAnswerIsReadWithNotes() {
        EditResponseParser.Parsed p = EditResponseParser.parse("<EDITED>\n  Dòng một.\nDòng hai.  \n</EDITED>\n<NOTES>\nDòng hai | MEANING | giữ nguyên\n</NOTES>", "stop");
        assertEquals(EditResponseParser.Status.OK, p.status());
        assertEquals("Dòng một.\nDòng hai.", p.edited());
        assertEquals(1, p.notes().size());
        assertEquals("Dòng hai", p.notes().get(0).quote());
        assertEquals("MEANING", p.notes().get(0).kind());
        assertEquals(0, p.notesDropped());
    }

    @Test public void missingNotesIsNotAnError() {
        EditResponseParser.Parsed p = EditResponseParser.parse("<EDITED>Chỉ có văn bản.</EDITED>", "stop");
        assertEquals(EditResponseParser.Status.OK, p.status());
        assertTrue(p.notes().isEmpty());
        assertEquals(0, p.notesDropped());
    }

    @Test public void brokenNoteLinesAreSkippedAndCounted() {
        EditResponseParser.Parsed p = EditResponseParser.parse("<EDITED>x</EDITED><NOTES>\nthiếu cột | MEANING\n\n | | \nđoạn | NUMBER | lý do | thừa cột\nổn | GLOSSARY | rõ\n</NOTES>", "stop");
        assertEquals(EditResponseParser.Status.OK, p.status());
        assertEquals(2, p.notes().size());
        assertEquals("lý do | thừa cột", p.notes().get(0).reason());
        assertEquals(2, p.notesDropped());
    }

    @Test public void unclosedNotesStillYieldTheNotesBeforeTheEnd() {
        EditResponseParser.Parsed p = EditResponseParser.parse("<EDITED>x</EDITED><NOTES>\na | b | c\n", "stop");
        assertEquals(1, p.notes().size());
    }

    @Test public void textBeforeTheTagIsIgnoredButReported() {
        EditResponseParser.Parsed p = EditResponseParser.parse("Đây là bản đã sửa:\n<EDITED>Văn bản.</EDITED>", "stop");
        assertEquals(EditResponseParser.Status.OK, p.status());
        assertEquals("Văn bản.", p.edited());
        assertTrue(p.textBeforeTag());
    }

    @Test public void nestedEditedTagsKeepTheOuterText() {
        EditResponseParser.Parsed p = EditResponseParser.parse("<EDITED>trước <EDITED>trong</EDITED> sau</EDITED>", "stop");
        assertEquals(EditResponseParser.Status.OK, p.status());
        assertEquals("trước <EDITED>trong</EDITED> sau", p.edited());
    }

    @Test public void missingCloseIsTruncatedWhenTheModelRanOutAndFormatOtherwise() {
        assertEquals(EditResponseParser.Status.TRUNCATED, EditResponseParser.parse("<EDITED>nửa chừng", "length").status());
        assertEquals(EditResponseParser.Status.FORMAT, EditResponseParser.parse("<EDITED>nửa chừng", "stop").status());
        assertEquals(EditResponseParser.Status.TRUNCATED, EditResponseParser.parse("không có thẻ", "length").status());
    }

    @Test public void missingOpenTagOrEmptyEditedIsFormat() {
        assertEquals(EditResponseParser.Status.FORMAT, EditResponseParser.parse("chỉ có chữ", "stop").status());
        assertEquals(EditResponseParser.Status.FORMAT, EditResponseParser.parse("</EDITED>", "stop").status());
        assertEquals(EditResponseParser.Status.FORMAT, EditResponseParser.parse("<EDITED>   \n </EDITED>", "stop").status());
        assertEquals(EditResponseParser.Status.FORMAT, EditResponseParser.parse(null, null).status());
    }

    @Test public void wrongPairCarriesItsEvidence() {
        EditResponseParser.Parsed p = EditResponseParser.parse("<WRONG_PAIR>RAW kể về cuộc thi, DRAFT kể về bữa tiệc.</WRONG_PAIR>", "stop");
        assertEquals(EditResponseParser.Status.WRONG_PAIR, p.status());
        assertTrue(p.wrongPairEvidence().contains("cuộc thi"));
        assertEquals("", p.edited());
        // an empty claim is not a wrong pair
        assertEquals(EditResponseParser.Status.FORMAT, EditResponseParser.parse("<WRONG_PAIR> </WRONG_PAIR>", "stop").status());
    }

    @Test public void frameSymbolsAndFullWidthCharactersSurviveByteForByte() {
        String text = "「Ừ.」\n『Đúng vậy』\n◇◇◇\n＊＊＊\n── …… 【Hệ thống】\nＡＢＣ　全角";
        EditResponseParser.Parsed p = EditResponseParser.parse("<EDITED>\n" + text + "\n</EDITED>", "stop");
        assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), p.edited().getBytes(StandardCharsets.UTF_8));
    }

    // ---- check answer ----

    @Test public void passAnswerParses() {
        CheckResponseParser.Parsed p = CheckResponseParser.parse("{\"verdict\":\"PASS\",\"wrong_pair_evidence\":\"\",\"issues\":[]}");
        assertEquals(CheckResponseParser.Status.OK, p.status());
        assertEquals("PASS", p.verdict());
        assertTrue(p.issues().isEmpty());
        assertEquals(0, p.counters().total());
    }

    @Test public void emptyFixIsValidAndExtraKeysAreCounted() {
        CheckResponseParser.Parsed p = CheckResponseParser.parse("{\"verdict\":\"ISSUES\",\"wrong_pair_evidence\":\"\",\"confidence\":0.9,"
                + "\"issues\":[{\"edited_quote\":\"năm trăm\",\"raw_quote\":\"五百\",\"kind\":\"NUMBER\",\"fix\":\"\",\"why\":\"x\"}]}");
        assertEquals(1, p.issues().size());
        assertEquals("", p.issues().get(0).fix());
        assertEquals(EditorialApiContract.IssueKind.NUMBER, p.issues().get(0).kind());
        assertEquals(2, p.counters().unknownKeys());
    }

    @Test public void issueWithoutAQuoteIsDroppedAndCountedNotRefused() {
        CheckResponseParser.Parsed p = CheckResponseParser.parse("{\"verdict\":\"ISSUES\",\"wrong_pair_evidence\":\"\",\"issues\":["
                + "{\"edited_quote\":\"\",\"raw_quote\":\"a\",\"kind\":\"MEANING\",\"fix\":\"b\"},"
                + "\"not an object\","
                + "{\"edited_quote\":\"ổn\",\"raw_quote\":\"\",\"kind\":\"NOT_A_KIND\",\"fix\":\"tốt\"}]}");
        assertEquals(CheckResponseParser.Status.OK, p.status());
        assertEquals(1, p.issues().size());
        assertEquals(EditorialApiContract.IssueKind.TECHNICAL, p.issues().get(0).kind());
        assertEquals(2, p.counters().issuesDropped());
        assertEquals(1, p.counters().unknownKinds());
    }

    @Test public void missingKeysAndOversizeValuesAreCounted() {
        StringBuilder longQuote = new StringBuilder();
        for (int i = 0; i < 200; i++) longQuote.append('x');
        CheckResponseParser.Parsed p = CheckResponseParser.parse("{\"issues\":[{\"edited_quote\":\"" + longQuote + "\",\"kind\":\"MEANING\"}]}");
        assertEquals(CheckResponseParser.Status.OK, p.status());
        assertEquals("ISSUES", p.verdict());
        assertEquals(1, p.issues().size());
        assertTrue(p.counters().missingKeys() >= 2);
        assertEquals(1, p.counters().oversizeValues());
    }

    @Test public void answerWrappedInAFenceOrSentenceIsStillRead() {
        CheckResponseParser.Parsed p = CheckResponseParser.parse("Kết quả:\n```json\n{\"verdict\":\"PASS\",\"wrong_pair_evidence\":\"\",\"issues\":[]}\n```");
        assertEquals(CheckResponseParser.Status.OK, p.status());
    }

    @Test public void unreadableAnswerIsTheOnlyFailure() {
        assertEquals(CheckResponseParser.Status.UNREADABLE, CheckResponseParser.parse("không phải JSON").status());
        assertEquals(CheckResponseParser.Status.UNREADABLE, CheckResponseParser.parse("{\"verdict\":").status());
        assertEquals(CheckResponseParser.Status.UNREADABLE, CheckResponseParser.parse(null).status());
        assertEquals(CheckResponseParser.Status.UNREADABLE, CheckResponseParser.parse("").status());
    }

    @Test public void wrongPairVerdictKeepsEvidence() {
        CheckResponseParser.Parsed p = CheckResponseParser.parse("{\"verdict\":\"WRONG_PAIR\",\"wrong_pair_evidence\":\"khác chương\",\"issues\":[]}");
        assertEquals("WRONG_PAIR", p.verdict());
        assertEquals("khác chương", p.wrongPairEvidence());
    }

    // ---- fix applier ----

    private static CheckResponseParser.Issue issue(String quote, String fix) {
        return new CheckResponseParser.Issue(quote, "", EditorialApiContract.IssueKind.MEANING, fix);
    }

    @Test public void fixIsAppliedWhenTheQuoteMatchesExactlyOnce() {
        FixApplier.Result r = FixApplier.apply("Cô ấy đi chợ.\nAnh ấy ở nhà.", List.of(issue("Anh ấy ở nhà", "Anh ấy ra ngoài")));
        assertEquals("Cô ấy đi chợ.\nAnh ấy ra ngoài.", r.text());
        assertEquals(1, r.applied().size());
        assertTrue(r.review().isEmpty());
    }

    @Test public void noMatchLeavesTheTextAndMarksReview() {
        FixApplier.Result r = FixApplier.apply("Cô ấy đi chợ.", List.of(issue("không có trong văn bản", "x")));
        assertEquals("Cô ấy đi chợ.", r.text());
        assertEquals(FixApplier.ReviewReason.NO_MATCH, r.review().get(0).reason());
    }

    @Test public void twoMatchesAreAmbiguousAndNothingChanges() {
        FixApplier.Result r = FixApplier.apply("Có. Có.", List.of(issue("Có", "Không")));
        assertEquals("Có. Có.", r.text());
        assertEquals(FixApplier.ReviewReason.AMBIGUOUS, r.review().get(0).reason());
        // overlapping occurrences are ambiguous too
        assertEquals(FixApplier.ReviewReason.AMBIGUOUS, FixApplier.apply("aaa", List.of(issue("aa", "b"))).review().get(0).reason());
    }

    @Test public void whitespaceDifferencesStillMatchAndOnlyTheQuoteIsReplaced() {
        FixApplier.Result r = FixApplier.apply("Anh  ấy\tđi\u3000chợ rồi.", List.of(issue("  ấy đi chợ ", "ấy về nhà")));
        assertEquals("Anh  ấy về nhà rồi.", r.text());
        assertEquals(1, r.applied().size());
    }

    @Test public void emptyFixIsOnlyReported() {
        FixApplier.Result r = FixApplier.apply("Một câu.", List.of(issue("Một câu", "")));
        assertEquals("Một câu.", r.text());
        assertEquals(FixApplier.ReviewReason.EMPTY_FIX, r.review().get(0).reason());
    }

    @Test public void fixesApplyInOrderAndASecondFixOnTheSamePassageCannotLand() {
        List<CheckResponseParser.Issue> issues = new ArrayList<>();
        issues.add(issue("hai", "ba"));
        issues.add(issue("hai", "bốn"));
        FixApplier.Result r = FixApplier.apply("một hai", issues);
        assertEquals("một ba", r.text());
        assertEquals(1, r.applied().size());
        assertEquals(FixApplier.ReviewReason.NO_MATCH, r.review().get(0).reason());
    }

    @Test public void frameSymbolsInsideTheQuoteAreKept() {
        FixApplier.Result r = FixApplier.apply("「Đi thôi.」", List.of(issue("Đi thôi", "Về thôi")));
        assertEquals("「Về thôi.」", r.text());
    }
}
