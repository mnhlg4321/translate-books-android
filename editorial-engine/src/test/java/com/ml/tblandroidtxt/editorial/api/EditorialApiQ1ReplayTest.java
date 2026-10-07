package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Saved-N6-shaped responses are checked by the production guard without changing acceptance rules. */
public final class EditorialApiQ1ReplayTest {
    @Test public void n6AddedHanAndFullwidthQuestionAreBlockedOffline() {
        String raw = "原文 ?";
        String draft = "Bản dịch ?";
        String savedN6Candidate = "Bản dịch？ 三";
        EditGuards.Report report = EditGuards.check(raw, draft, savedN6Candidate, List.of(), EditGuards.Config.defaults());
        assertEquals(draft, report.cleaned());
        assertTrue(report.has(EditGuards.Code.CONTENT_LEAK));
        assertTrue(report.has(EditGuards.Code.NORMALIZATION_APPLIED));
    }

    @Test public void aCleanSavedCandidateRemainsUnchanged() {
        String draft = "Bản dịch ?";
        EditGuards.Report report = EditGuards.check("原文 ?", draft, draft, List.of(), EditGuards.Config.defaults());
        assertEquals(draft, report.cleaned());
        assertTrue(report.flags().isEmpty());
    }
}
