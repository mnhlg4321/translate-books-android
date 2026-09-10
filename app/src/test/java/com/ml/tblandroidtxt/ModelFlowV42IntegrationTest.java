package com.ml.tblandroidtxt;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/** Reproducible model-selection/estimate flow without requiring a live provider account. */
public class ModelFlowV42IntegrationTest {
    @After public void resetCatalog() {
        ModelCatalog.installForTests(Collections.emptyList(), ModelCatalog.LoadingState.IDLE);
    }

    @Test public void newlyFetchedModelRecalculatesWhenAsyncPricingArrives() {
        AppSettings settings = new AppSettings();
        settings.provider = "openrouter";
        settings.model = "openai/gpt-luna.preview-2026-07-15";
        String source = "A newly fetched model must show a token estimate before its pricing arrives.";

        ModelCatalog.installForTests(Collections.emptyList(), ModelCatalog.LoadingState.LOADING);
        CostEstimator.Estimate before = CostEstimator.estimate(source, settings);
        assertTrue(before.totalTokensLow > 0);
        assertFalse(before.pricingAvailable);

        String response = "{\"data\":[{\"id\":\"openai/gpt-luna.preview-2026-07-15\","
                + "\"name\":\"GPT Luna\",\"context_length\":200000,"
                + "\"pricing\":{\"prompt\":\"8e-7\",\"completion\":\"4e-6\",\"input_cache_read\":\"8e-8\"}}]}";
        List<ModelCatalog.ModelInfo> fetched = ModelCatalog.parseOpenRouterResponse(response, "openrouter-live", 777);
        ModelCatalog.installForTests(fetched, ModelCatalog.LoadingState.READY);

        CostEstimator.Estimate after = CostEstimator.estimate(source, settings);
        assertTrue(after.pricingAvailable);
        assertTrue(after.costHigh >= after.costLow);
        assertEquals("openrouter-live", after.pricingSource);
        assertEquals(settings.model, ModelCatalog.findModelInfo(settings.provider, settings.model).id);
    }

    @Test public void selectedExactModelSurvivesSettingsSerialization() {
        AppSettings selected = new AppSettings();
        selected.provider = "openrouter";
        selected.model = "openai/gpt-terra-2.1-preview:exact";
        AppSettings restored = SettingsStore.fromJson(SettingsStore.toJson(selected));
        assertEquals(selected.provider, restored.provider);
        assertEquals(selected.model, restored.model);
    }

    @Test public void actualUsageParserUsesProviderFieldsAndProviderCost() throws Exception {
        String response = "{\"choices\":[{\"message\":{\"content\":\"translated\"},\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"input_tokens\":1200,\"output_tokens\":300,\"total_tokens\":1500,"
                + "\"completion_tokens_details\":{\"reasoning_tokens\":40},"
                + "\"input_tokens_details\":{\"cache_read_input_tokens\":400},\"cost\":\"0.0042\"}}";
        OpenAICompatibleClient.ChatResult result = OpenAICompatibleClient.parseChatResponse(response);
        assertTrue(result.usageReported);
        assertEquals(1200, result.promptTokens);
        assertEquals(300, result.completionTokens);
        assertEquals(40, result.reasoningTokens);
        assertEquals(1500, result.totalTokens);
        assertEquals(400, result.cachedPromptTokens);
        assertTrue(result.providerCostReported);
        assertEquals(0.0042, result.providerCost, 0.0000001);
    }

    @Test public void missingUsageIsNotPresentedAsProviderUsage() throws Exception {
        OpenAICompatibleClient.ChatResult result = OpenAICompatibleClient.parseChatResponse(
                "{\"choices\":[{\"message\":{\"content\":\"translated\"}}]}");
        assertFalse(result.usageReported);
        assertFalse(result.providerCostReported);
    }
}
