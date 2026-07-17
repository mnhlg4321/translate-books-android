package com.ml.tblandroidtxt;

import org.junit.Test;
import java.net.SocketTimeoutException;
import static org.junit.Assert.*;

public class CostOptimizationTest {
    private AppSettings settings() {
        AppSettings s = new AppSettings();
        s.optimizationPreset = "balanced"; s.contextChars = 100; s.maxOutputTokens = 4096;
        s.translationInstructions = "Keep every fact.";
        s.glossaryText = "Alice => A-lix [character]\nUnused => Khong dung [term]";
        s.pronounText = "Alice -> Bob: chị / em";
        return s;
    }

    @Test public void promptPlanMeasuresEveryComponentAndFiltersLocks() {
        PromptPlan p = PromptPlan.translation(new Chunk(0, "before", "Alice speaks.", "after"), "Previous paragraph", settings());
        assertTrue(p.rawTokens > 0); assertTrue(p.systemTokens > 0); assertTrue(p.instructionTokens > 0);
        assertTrue(p.glossary.contains("Alice")); assertFalse(p.glossary.contains("Unused"));
        assertTrue(p.pronounTokens > 0); assertEquals(Chunker.approxTokens(p.prompt.system) + Chunker.approxTokens(p.prompt.user), p.totalInputTokens);
    }

    @Test public void adaptiveOutputIsBoundedAndSmallerForShortChunk() {
        PromptPlan p = PromptPlan.translation(new Chunk(0, "", "Short source.", ""), "", settings());
        assertTrue(p.adaptiveMaxOutputTokens(settings()) >= 128);
        assertTrue(p.adaptiveMaxOutputTokens(settings()) < 4096);
    }

    @Test public void balancedPromptUsesFewerTokensThanFullBaseline() {
        AppSettings balanced = settings();
        Chunk c = new Chunk(0, "context before", "Alice speaks in the room.", "context after");
        PromptPlan optimized = PromptPlan.forTranslation(c, "previous translation", balanced);
        AppSettings full = balanced.copy(); full.optimizationPreset = "full";
        PromptPlan baseline = PromptPlan.forTranslation(c, "previous translation", full);
        assertTrue(optimized.totalInputTokens < baseline.totalInputTokens);
    }

    @Test public void presetsChangeChunkBudgetWithoutChangingFullBaseline() {
        AppSettings s = settings(); s.maxTokensPerChunk = 400;
        s.optimizationPreset = "full"; assertEquals(400, Chunker.adaptiveLimit(s, 400));
        s.optimizationPreset = "balanced"; assertEquals(600, Chunker.adaptiveLimit(s, 400));
        s.optimizationPreset = "economy"; assertEquals(800, Chunker.adaptiveLimit(s, 400));
    }

    @Test public void retryClassificationAvoidsPermanentFailures() {
        assertEquals(RetryPolicy.Kind.RATE_LIMIT, RetryPolicy.classify(new RuntimeException("HTTP 429 rate limit")));
        assertEquals(RetryPolicy.Kind.TIMEOUT, RetryPolicy.classify(new SocketTimeoutException("timeout")));
        assertEquals(RetryPolicy.Kind.NON_RETRYABLE, RetryPolicy.classify(new RuntimeException("HTTP 401 API key")));
        assertFalse(RetryPolicy.mayRetry(RetryPolicy.Kind.NON_RETRYABLE, 1, 3));
        assertTrue(RetryPolicy.mayRetry(RetryPolicy.Kind.NETWORK, 1, 3));
        assertFalse(RetryPolicy.mayRetry(RetryPolicy.Kind.CONTEXT_LENGTH, 1, 3));
        ApiHttpException rate = new ApiHttpException(429, 7000, "limited");
        assertEquals(RetryPolicy.Kind.RATE_LIMIT, RetryPolicy.classify(rate));
        assertEquals(7000, RetryPolicy.retryAfterMs(rate));
    }

    @Test public void invalidPresetNormalizesToBalanced() {
        AppSettings s = settings(); s.optimizationPreset = "unknown";
        assertEquals("balanced", AppValidator.normalize(s).optimizationPreset);
    }

    @Test public void estimatorUsesExactPlansAndDiscountsOnlyExpectedSharedPrefix() {
        AppSettings s = settings(); s.provider="openrouter"; s.model="openai/gpt-5.4-mini";
        s.maxTokensPerChunk=40; s.softLimitRatio=.8f;
        s.translationInstructions="Keep names and style stable. ".repeat(900);
        String source=("Alice speaks in one paragraph with enough text to form chunks.\n\n").repeat(20);
        CostEstimator.Estimate e=CostEstimator.estimate(source,s);
        assertTrue(e.chunks>1);
        assertEquals(e.inputTokensLow,e.inputTokensHigh);
        assertEquals(e.inputTokensLow,e.expectedCachedInputTokens+e.expectedUncachedInputTokens);
        assertTrue(e.expectedCachedInputTokens>0);
        assertTrue(e.costLow<e.noCacheCostLow);
        assertTrue(e.costHigh<e.noCacheCostHigh);
    }

    @Test public void unknownCachedPriceNeverInventsCacheSavings() {
        AppSettings s=settings(); s.provider="openrouter"; s.model="openai/gpt-5.5-thinking";
        CostEstimator.Estimate e=CostEstimator.estimate("Alice speaks.",s);
        assertFalse(e.cachePricingAvailable);
        assertEquals(0,e.expectedCachedInputTokens);
        assertEquals(e.noCacheCostLow,e.costLow,0.0000001);
    }
}
