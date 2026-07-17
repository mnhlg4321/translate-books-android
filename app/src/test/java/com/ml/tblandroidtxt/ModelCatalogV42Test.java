package com.ml.tblandroidtxt;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ModelCatalogV42Test {
    @After public void resetCatalog() {
        ModelCatalog.installForTests(Collections.emptyList(), ModelCatalog.LoadingState.IDLE);
    }

    private ModelCatalog.ModelInfo parseOne(String id, String pricing) {
        String json = "{\"data\":[{\"id\":\"" + id + "\",\"name\":\"New model\","
                + "\"context_length\":131072,\"pricing\":" + pricing + "}]}";
        List<ModelCatalog.ModelInfo> models = ModelCatalog.parseOpenRouterResponse(json, "test-live", 1234);
        assertEquals(1, models.size());
        return models.get(0);
    }

    @Test public void parsesStandardDecimalPricingOncePerMillion() {
        ModelCatalog.ModelInfo model = parseOne("openai/gpt-luna", "{\"prompt\":\"0.00000125\",\"completion\":\"0.00001\"}");
        assertEquals(1.25, model.inputPerMillion, 0.0000001);
        assertEquals(10.0, model.outputPerMillion, 0.0000001);
        assertTrue(model.hasPricing());
    }

    @Test public void parsesScientificNotationPricing() {
        ModelCatalog.ModelInfo model = parseOne("openai/gpt-terra-preview-2026-07-01", "{\"prompt\":\"7.5e-7\",\"completion\":4.5e-6}");
        assertEquals(0.75, model.inputPerMillion, 0.0000001);
        assertEquals(4.5, model.outputPerMillion, 0.0000001);
    }

    @Test public void nullPricingIsUnknownNotZero() {
        ModelCatalog.ModelInfo model = parseOne("provider/model.null", "{\"prompt\":null,\"completion\":null}");
        assertFalse(model.inputPriceKnown);
        assertFalse(model.outputPriceKnown);
        assertFalse(model.hasPricing());
    }

    @Test public void zeroPricingIsKnownAndFree() {
        ModelCatalog.ModelInfo model = parseOne("provider/free-model", "{\"prompt\":\"0\",\"completion\":0}");
        assertTrue(model.hasPricing());
        assertEquals(0, ModelCatalog.usageCost(model, 1000, 0, 500), 0);
        assertTrue(model.priceSuffix().contains("$0"));
    }

    @Test public void parsesCachedInputPricing() {
        ModelCatalog.ModelInfo model = parseOne("provider/cached-model", "{\"prompt\":\"1e-6\",\"completion\":\"4e-6\",\"input_cache_read\":\"2.5e-7\"}");
        assertTrue(model.cachedInputPriceKnown);
        assertEquals(0.25, model.cachedInputPerMillion, 0.0000001);
        assertEquals(0.0027, ModelCatalog.usageCost(model, 1000, 400, 500), 0.0000001);
    }

    @Test public void preservesUnusualExactProviderModelIds() {
        String id = "vendor.alpha/model-x.2-preview-2026-07-15:free/router-beta";
        ModelCatalog.ModelInfo model = parseOne(id, "{\"prompt\":\"1e-6\",\"completion\":\"2e-6\"}");
        assertEquals(id, model.id);
        assertEquals(id, ModelCatalog.normalizeModelId("  vendor.alpha / model-x.2-preview-2026-07-15:free/router-beta  "));
    }

    @Test public void unknownModelNeverBorrowsAnotherModelsPricing() {
        ModelCatalog.ModelInfo unknown = ModelCatalog.findModelInfo("openrouter", "openai/gpt-luna-not-catalogued");
        assertEquals("openai/gpt-luna-not-catalogued", unknown.id);
        assertFalse(unknown.hasPricing());
        assertTrue(Double.isNaN(ModelCatalog.usageCost(unknown, 1000, 0, 1000)));
    }

    @Test public void unknownPricingStillReturnsTokenRangeAndNonBlankUiLine() {
        AppSettings settings = new AppSettings();
        settings.provider = "openrouter"; settings.model = "new/provider-model.preview";
        CostEstimator.Estimate estimate = CostEstimator.estimate("A source paragraph long enough to estimate tokens.", settings);
        assertTrue(estimate.inputTokensLow > 0);
        assertTrue(estimate.inputTokensHigh >= estimate.inputTokensLow);
        assertFalse(estimate.pricingAvailable);
        assertTrue(CostEstimator.estimateLine(estimate, settings).contains("Pricing unavailable"));
        assertTrue(CostEstimator.estimateLine(estimate, settings).contains("generic approximation"));
    }
}
