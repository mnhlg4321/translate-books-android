package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

/**
 * v4.23 J2/J6: the validator compares brackets, repeats and line counts with the RAW, names a cut answer as such, repairs mixed quote
 * styles by a fixed rule, and a rejected answer is a failure of one chapter (never of the whole run).
 */
public class V423ValidationTest {
    private static final String OPEN = "「", CLOSE = "」", OPEN2 = "『", CLOSE2 = "』", CURLY_CLOSE = "”", CURLY_SINGLE_CLOSE = "’";

    private static String lines(int n, String each) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(each).append(i).append('\n');
        return sb.toString().trim();
    }

    private static ResponseValidator.Result check(String raw, String body) {
        return ResponseValidator.validate(body, body, raw, "", "stop", 100, 4096, false);
    }

    // ---- brackets against the RAW ----

    @Test public void balancedRawAndUnclosedTranslationIsRefusedWithTheKindAndCounts() {
        String raw = OPEN + "a" + CLOSE;
        ResponseValidator.Result r = check(raw, OPEN + "a" + OPEN + "b");
        assertFalse(r.accepted);
        assertTrue(r.errors.contains("UNCLOSED_BRACKET_OR_DIALOGUE"));
        assertEquals(OPEN + CLOSE, r.bracketKind);
        assertEquals(2, r.bracketDelta);
        assertEquals(0, r.rawBracketDelta);
        assertTrue(r.readable().contains("2"));
    }

    @Test public void rawThatIsUnbalancedItselfDoesNotRefuseAFaithfulTranslation() {
        String raw = OPEN + "a" + CLOSE + OPEN + "b";           // the RAW opens one quote it never closes
        assertTrue(check(raw, OPEN + "x" + CLOSE + OPEN + "y").accepted);
    }

    @Test public void translationThatClosesTheRawsOpenQuoteIsAccepted() {
        String raw = OPEN + "a" + CLOSE + OPEN + "b";
        assertTrue(check(raw, OPEN + "x" + CLOSE + OPEN + "y" + CLOSE).accepted);
    }

    @Test public void opposingOrLargerImbalanceIsRefusedEvenWhenTheRawIsUnbalanced() {
        String raw = OPEN + "a" + CLOSE + OPEN + "b";           // delta +1
        assertFalse("larger (+2)", check(raw, OPEN + "x" + OPEN + "y" + CLOSE + OPEN + "z").accepted);
        assertFalse("opposite (-1)", check(raw, OPEN + "x" + CLOSE + CLOSE).accepted);
    }

    @Test public void asciiParenthesisSurplusIsRefusedOnlyWhenTheRawDoesNotHaveIt() {
        assertFalse(check("plain", "plain (unclosed").accepted);
        assertTrue(check("plain (unclosed", "plain (unclosed").accepted);
        assertTrue("a surplus of closers was never refused", check("plain", "plain) done").accepted);
    }

    @Test public void doubleCornerBracketsAreComparedToo() {
        assertFalse(check(OPEN2 + "a" + CLOSE2, OPEN2 + "a").accepted);
        assertTrue(check(OPEN2 + "a" + CLOSE2, OPEN2 + "a" + CLOSE2).accepted);
    }

    // ---- cut answers are named first ----

    @Test public void finishLengthIsReportedBeforeAnyBracketError() {
        String raw = OPEN + "a" + CLOSE;
        ResponseValidator.Result r = ResponseValidator.validate(OPEN + "a", OPEN + "a", raw, "", "length", 4096, 4096, false);
        assertFalse(r.accepted);
        assertTrue(r.truncated);
        assertEquals("FINISH_REASON_LENGTH", r.errors.get(0));
        assertFalse("the quote is a symptom, not a second verdict", r.errors.contains("UNCLOSED_BRACKET_OR_DIALOGUE"));
        assertTrue(r.readable().startsWith("Bản dịch bị cắt"));
        assertTrue(new ResponseValidator.RejectedResponseException(r).getMessage().contains("bị cắt"));
    }

    @Test public void tokenLimitReachedCountsAsCutEvenWithFinishStop() {
        ResponseValidator.Result r = ResponseValidator.validate("ok.", "ok.", "raw", "", "stop", 4096, 4096, false);
        assertTrue(r.truncated);
        assertTrue(r.errors.contains("OUTPUT_TOKEN_LIMIT_REACHED"));
    }

    @Test public void unknownFinishFarFromTheLimitIsTreatedAsStop() {
        ResponseValidator.Result far = ResponseValidator.validate("unfinished words", "unfinished words", "raw", "", "", 500, 4096, false);
        assertTrue(far.accepted);
        assertTrue(far.warnings.contains("UNKNOWN_FINISH_REASON"));
        ResponseValidator.Result near = ResponseValidator.validate("unfinished words", "unfinished words", "raw", "", "", 4000, 4096, false);
        assertFalse(near.accepted);
        assertTrue(near.errors.contains("LIKELY_TRUNCATED_END"));
        assertTrue(near.truncated);
    }

    @Test public void tokenCountsInTheMessageNeverChangeTheRetryKind() {
        ResponseValidator.Result r = ResponseValidator.validate(OPEN + "a", OPEN + "a", OPEN + "a" + CLOSE, "", "length", 4013, 4013, false);
        assertTrue(new ResponseValidator.RejectedResponseException(r).getMessage().contains("401"));
        assertEquals(RetryPolicy.Kind.TRUNCATION, RetryPolicy.classify(new ResponseValidator.RejectedResponseException(r)));
        ResponseValidator.Result bracket = check(OPEN + "a" + CLOSE, OPEN + "a" + OPEN);
        assertEquals(RetryPolicy.Kind.VALIDATION, RetryPolicy.classify(new ResponseValidator.RejectedResponseException(bracket)));
    }

    // ---- missing lines ----

    @Test public void translationWithFewerThanEightyPercentOfTheLinesIsRefused() {
        String raw = lines(10, "raw line ");
        assertTrue(check(raw, lines(8, "dong ")).accepted);
        ResponseValidator.Result r = check(raw, lines(7, "dong "));
        assertFalse(r.accepted);
        assertTrue(r.errors.contains("OUTPUT_MISSING_LINES"));
        assertEquals(0.7, r.lineRatio, 1e-9);
        assertTrue(r.readable().contains("70%"));
    }

    @Test public void blankLinesDoNotCountAndShortChunksAreNotJudged() {
        String raw = lines(10, "raw line ").replace("\n", "\n\n");
        assertTrue(check(raw, lines(9, "dong ")).accepted);
        assertTrue("fewer than five RAW lines: no ratio", check(lines(4, "raw "), "one line").accepted);
    }

    // ---- repetition ----

    @Test public void repetitionTheRawAlsoHasIsNotAModelLoop() {
        String chorus = "la la la\nla la la\nla la la\nla la la\nla la la";
        assertTrue(check(chorus, chorus).accepted);
        assertFalse(check("one\ntwo\nthree\nfour\nfive", chorus).accepted);
    }

    // ---- mixed quote styles ----

    @Test public void cornerOpenAndCurlyCloseIsRepairedByAFixedRule() {
        String raw = OPEN + "a" + CLOSE + OPEN + "b" + CLOSE;
        String mixed = OPEN + "x" + CURLY_CLOSE + " " + OPEN + "y" + CURLY_CLOSE;
        ResponseValidator.Repaired repaired = ResponseValidator.repairQuoteStyle(mixed, raw);
        assertTrue(repaired.changed);
        assertEquals(OPEN + "x" + CLOSE + " " + OPEN + "y" + CLOSE, repaired.text);
        assertFalse("refused before the repair", check(raw, mixed).accepted);
        assertTrue("accepted after it", check(raw, repaired.text).accepted);
    }

    @Test public void singleCornerAndCurlyAreRepairedAndNothingElseIsTouched() {
        String raw = OPEN2 + "a" + CLOSE2;
        ResponseValidator.Repaired ok = ResponseValidator.repairQuoteStyle(OPEN2 + "x" + CURLY_SINGLE_CLOSE, raw);
        assertTrue(ok.changed);
        assertEquals(OPEN2 + "x" + CLOSE2, ok.text);
        // A stray apostrophe with nothing open is left alone.
        ResponseValidator.Repaired stray = ResponseValidator.repairQuoteStyle(OPEN2 + "x" + CURLY_SINGLE_CLOSE + " it" + CURLY_SINGLE_CLOSE + "s", raw);
        assertEquals(OPEN2 + "x" + CLOSE2 + " it" + CURLY_SINGLE_CLOSE + "s", stray.text);
    }

    @Test public void curlyPairsInsideTheSameTextAreKeptAndOnlyTheOnesClosingACornerOpenerAreChanged() {
        String raw = OPEN + "a" + CLOSE + OPEN + "b" + CLOSE;
        String mixed = OPEN + "x" + CURLY_CLOSE + " “quoted" + CURLY_CLOSE + " " + OPEN + "y" + CURLY_CLOSE;
        ResponseValidator.Repaired repaired = ResponseValidator.repairQuoteStyle(mixed, raw);
        assertTrue(repaired.changed);
        assertEquals(OPEN + "x" + CLOSE + " “quoted" + CURLY_CLOSE + " " + OPEN + "y" + CLOSE, repaired.text);
    }

    @Test public void aRepairThatDoesNotReachTheRawsBalanceIsNotKept() {
        String raw = OPEN + "a" + CLOSE;
        String missing = OPEN + "x" + OPEN + "y" + CURLY_CLOSE;           // one closer short even after the repair
        ResponseValidator.Repaired repaired = ResponseValidator.repairQuoteStyle(missing, raw);
        assertFalse(repaired.changed);
        assertEquals(missing, repaired.text);
    }

    @Test public void repairNeverRunsWhenTheRawDoesNotUseCornerBracketsOrWhenACornerCloserExists() {
        assertFalse(ResponseValidator.repairQuoteStyle(OPEN + "x" + CURLY_CLOSE, "plain text").changed);
        assertFalse(ResponseValidator.repairQuoteStyle(OPEN + "x" + CLOSE + " " + CURLY_CLOSE, OPEN + "a" + CLOSE).changed);
        assertFalse(ResponseValidator.repairQuoteStyle("", OPEN + "a" + CLOSE).changed);
    }

    // ---- a rejected answer belongs to its chapter ----

    @Test public void aRejectedAnswerContinuesTheQueueOtherFailuresDoNot() {
        ResponseValidator.Result r = check(OPEN + "a" + CLOSE, OPEN + "a");
        Exception rejected = new ResponseValidator.RejectedResponseException(r);
        assertTrue(ChapterRunOutcome.continuesQueue(rejected));
        assertTrue(ChapterRunOutcome.continuesQueue(new ChapterLocalException("x", rejected)));
        assertTrue(ChapterRunOutcome.continuesQueue(new RuntimeException("wrapped", rejected)));
        assertFalse(ChapterRunOutcome.continuesQueue(new TranslationEngine.DeliveryUnknownException("unknown", null)));
        assertFalse(ChapterRunOutcome.continuesQueue(new RuntimeException("HTTP 401")));
        assertFalse(ChapterRunOutcome.continuesQueue(new java.io.IOException("network")));
        assertFalse(ChapterRunOutcome.continuesQueue(new RuntimeException("Cost limit reached: $1")));
    }

    @Test public void twoFailedChaptersInARowStopTheQueueButADoneChapterResetsTheCount() {
        ChapterRunOutcome o = new ChapterRunOutcome(6);
        o.failed("1", "x");
        assertFalse(o.shouldStopQueue());
        o.done("2");
        o.failed("3", "x");
        assertFalse("the done chapter in between reset the streak", o.shouldStopQueue());
        o.failed("4", "x");
        assertTrue(o.shouldStopQueue());
        assertEquals(2, o.failedInARow());
        assertEquals("Tổ hợp: 1 xong · 3 lỗi (1: x; 3: x; 4: x) · 2 chưa chạy", o.summary());
    }

    // ---- truncation handling ----

    @Test public void aCutAnswerIsRetriedWithALargerLimitThenCappedAndRecognisedThroughTheCauseChain() {
        assertEquals(8192, TranslationEngine.nextLimitAfterTruncation(4096));
        assertEquals(16384, TranslationEngine.nextLimitAfterTruncation(8192));
        assertEquals(16384, TranslationEngine.nextLimitAfterTruncation(16384));
        assertEquals(8192, TranslationEngine.nextLimitAfterTruncation(1000));
        ResponseValidator.Result cut = ResponseValidator.validate("x", "x", "raw", "", "length", 10, 100, false);
        assertTrue(TranslationEngine.isTruncation(new ResponseValidator.RejectedResponseException(cut)));
        assertTrue(TranslationEngine.isTruncation(new RuntimeException("w", new ResponseValidator.RejectedResponseException(cut))));
        assertFalse(TranslationEngine.isTruncation(new ResponseValidator.RejectedResponseException(check("raw", ""))));
        assertFalse(TranslationEngine.isTruncation(new RuntimeException("none")));
    }

    // ---- reasoning effort ----

    @Test public void reasoningEffortIsLowOnlyForKnownReasoningModelsUnlessChosen() {
        AppSettings s = new AppSettings();
        s.model = "openai/gpt-5.6-luna";
        assertEquals("low", ReasoningEffort.forTranslation(s));
        s.model = "anthropic/claude-sonnet-4.5";
        assertEquals("", ReasoningEffort.forTranslation(s));
        s.reasoningEffort = "high";
        assertEquals("high", ReasoningEffort.forTranslation(s));
        s.reasoningEffort = "off"; s.model = "openai/gpt-5.6-luna";
        assertEquals("", ReasoningEffort.forTranslation(s));
        s.reasoningEffort = "nonsense";
        assertEquals("auto", ReasoningEffort.normalize(s.reasoningEffort));
        assertTrue(ReasoningEffort.looksLikeReasoningModel("openai/o3-mini"));
        assertTrue(ReasoningEffort.looksLikeReasoningModel("deepseek/deepseek-r1"));
        assertFalse(ReasoningEffort.looksLikeReasoningModel("google/gemma-3"));
    }

    @Test public void reasoningEffortSurvivesSettingsJsonAndDefaultsToAuto() {
        AppSettings s = new AppSettings();
        assertEquals("auto", s.reasoningEffort);
        s.reasoningEffort = "medium";
        assertEquals("medium", SettingsStore.fromJson(SettingsStore.toJson(s)).reasoningEffort);
        assertEquals("medium", AppValidator.normalize(s).reasoningEffort);
        assertEquals("medium", s.copy().reasoningEffort);
        assertTrue(Arrays.asList("auto", "off", "low", "medium", "high").contains(AppValidator.normalize(new AppSettings()).reasoningEffort));
    }
}
