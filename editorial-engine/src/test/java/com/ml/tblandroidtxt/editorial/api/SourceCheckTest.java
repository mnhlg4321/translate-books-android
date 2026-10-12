package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SourceCheckTest {
    private static List<SourceCheck.Problem> check(String raw, String draft, boolean glossary, boolean pronoun) {
        return SourceCheck.check(raw, draft, glossary, pronoun, 0);
    }

    @Test public void aNormalPairWithBothReferencesHasNoProblem() {
        assertTrue(check("日本語の文章です。", "Đây là một câu tiếng Việt.", true, true).isEmpty());
    }

    @Test public void emptySourcesBlock() {
        List<SourceCheck.Problem> problems = check(" \n", "", true, true);
        assertTrue(problems.contains(SourceCheck.Problem.RAW_EMPTY));
        assertTrue(problems.contains(SourceCheck.Problem.DRAFT_EMPTY));
        assertTrue(SourceCheck.blocked(problems));
    }

    @Test public void brokenEncodingBlocks() {
        String broken = "���� abc";
        assertTrue(check(broken, "ok text here", true, true).contains(SourceCheck.Problem.RAW_BROKEN_ENCODING));
        assertTrue(check("ok text here", broken, true, true).contains(SourceCheck.Problem.DRAFT_BROKEN_ENCODING));
    }

    @Test public void aMissingReferenceOrOddRatioOnlyWarns() {
        List<SourceCheck.Problem> problems = check("a".repeat(100), "b".repeat(10), false, false);
        assertTrue(problems.contains(SourceCheck.Problem.LENGTH_RATIO));
        assertTrue(problems.contains(SourceCheck.Problem.NO_GLOSSARY));
        assertTrue(problems.contains(SourceCheck.Problem.NO_PRONOUN));
        assertFalse(SourceCheck.blocked(problems));
    }

    @Test public void identicalTextsWarn() {
        List<SourceCheck.Problem> problems = check("same text", "same text", true, true);
        assertEquals(List.of(SourceCheck.Problem.SAME_TEXT), problems);
    }

    @Test public void aChapterTooLongForOneCallBlocks() {
        List<SourceCheck.Problem> problems = check("x".repeat(40_000), "y".repeat(40_000), true, true);
        assertTrue(problems.contains(SourceCheck.Problem.TOO_LONG));
        assertTrue(SourceCheck.blocked(problems));
        assertFalse(check("x".repeat(20_000), "y".repeat(20_000), true, true).contains(SourceCheck.Problem.TOO_LONG));
    }
}
