package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** F1 + F6: reserve reasoning output, bound retries, and protect each root chunk. */
public class RuntimeFixF1F6Test {
    private static String cjk(int count) {
        StringBuilder out = new StringBuilder(count);
        for (int i = 0; i < count; i++) out.append('\u65e5');
        return out.toString();
    }

    @Test public void requestCapAddsOnlyTheReasoningReserveToVisibleAdaptiveOutput() {
        Chunk chunk = new Chunk(0, "", cjk(333), "");
        PromptPlan plan = PromptPlan.forTranslation(chunk, "", new AppSettings());
        assertEquals(333, plan.rawTokens);
        assertEquals(629, plan.visibleMaxOutputTokens(new AppSettings()));

        AppSettings s = new AppSettings();
        s.reasoningEffort = "medium";
        assertEquals(2677, plan.requestMaxOutputTokens(s));
        s.reasoningEffort = "high";
        assertEquals(8821, plan.requestMaxOutputTokens(s));
        s.reasoningEffort = "off";
        s.model = "fixture-model";
        assertEquals(629, plan.requestMaxOutputTokens(s));
        s.model = "openai/gpt-5.6-luna";
        assertEquals(2677, plan.requestMaxOutputTokens(s));
    }

    @Test public void reasoningBoundRequiresLengthAndConsumesTheOutputBudget() {
        OpenAICompatibleClient.ChatResult result = new OpenAICompatibleClient.ChatResult();
        result.finishReason = "length";
        result.completionTokens = 100;
        result.reasoningTokens = 90;
        result.content = "<TRANSLATION>x</TRANSLATION>";
        assertTrue(TranslationEngine.isReasoningBound(result, "x"));

        result.reasoningTokens = 50;
        assertFalse(TranslationEngine.isReasoningBound(result, "x"));
        result.reasoningTokens = 0;
        assertTrue(TranslationEngine.isReasoningBound(result, "x"));
        assertFalse(TranslationEngine.isReasoningBound(result, "A visible answer with more than thirty two chars."));
    }

    @Test public void onlyExplicitHighOrMediumEffortGetsOneLowerEffortRetry() {
        assertEquals("medium", ReasoningEffort.lowerForRetry("high"));
        assertEquals("low", ReasoningEffort.lowerForRetry("medium"));
        assertEquals("", ReasoningEffort.lowerForRetry("low"));
        assertEquals("", ReasoningEffort.lowerForRetry("auto"));
        assertEquals("", ReasoningEffort.lowerForRetry("off"));
    }

    @Test public void rootChunkAttemptBudgetStopsTheSeventhRequest() {
        RootChunkAttemptBudget budget = new RootChunkAttemptBudget(6);
        for (int i = 1; i <= 6; i++) assertEquals(i, budget.claim("job:root"));
        assertEquals(-1, budget.claim("job:root"));
        assertEquals(1, budget.claim("job:other-root"));
    }

    @Test public void defaultSafeCapIsThreeTimesThePlanButNeverBelowTenCents() {
        assertEquals(0.10, CostLimitPolicy.defaultSafeCapUsd(0), 0.000001);
        assertEquals(0.10, CostLimitPolicy.defaultSafeCapUsd(0.02), 0.000001);
        assertEquals(0.12, CostLimitPolicy.defaultSafeCapUsd(0.04), 0.000001);
        AppSettings s = new AppSettings();
        s.stopOnCostLimit = true;
        s.costLimitUsd = 0.25;
        assertEquals(0.25, CostLimitPolicy.effectiveCapUsd(s, 0.04, false), 0.000001);
        assertEquals(-1.0, CostLimitPolicy.effectiveCapUsd(s, 0.04, true), 0.000001);
    }

    @Test public void costGuardBlocksBeforeTheFirstPaidRequest() {
        AppSettings s = new AppSettings();
        s.stopOnCostLimit = true;
        s.costLimitUsd = 0.10;
        assertFalse(BudgetPolicy.beforePaidRequest(s, 0.10, 0, 0, true).allowed);
    }

    @Test public void offlineReasoningBoundRetriesOnceAtLowerEffortAndNeverRequestsAThirdCall() throws Exception {
        final List<String> efforts = new ArrayList<>();
        final List<Integer> caps = new ArrayList<>();
        TranslationEngine engine = new TranslationEngine((settings, prompt, outputLimit, requestId, observer) -> {
            efforts.add(settings.reasoningEffort);
            caps.add(outputLimit);
            OpenAICompatibleClient.ChatResult answer = new OpenAICompatibleClient.ChatResult();
            answer.content = "<TRANSLATION>x</TRANSLATION>";
            answer.finishReason = "length";
            answer.completionTokens = 100;
            answer.reasoningTokens = 100;
            return answer;
        });
        AppSettings s = new AppSettings();
        s.reasoningEffort = "high";
        s.maxAttempts = 3;
        s.retryOnTruncation = true;
        s.initialRetryDelayMs = 100;
        s.maxRetryDelayMs = 100;
        try {
            engine.translateWithRetry(new Chunk(0, "", "短い", ""), "", s, () -> false, null, null);
        } catch (Exception failure) {
            assertTrue(TranslationEngine.isReasoningBound(failure));
            assertFalse(TranslationEngine.shouldSplitAfterFailure(failure));
        }
        assertEquals(2, efforts.size());
        assertEquals("high", efforts.get(0));
        assertEquals("medium", efforts.get(1));
        assertTrue(caps.get(1) < caps.get(0));
    }
}
