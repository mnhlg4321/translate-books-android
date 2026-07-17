package com.ml.tblandroidtxt;

import org.junit.Test;
import static org.junit.Assert.*;

public class V46DashboardTest {
    @Test public void preparedSummaryContainsOnlyChunksAndExplicitEstimates() {
        CostEstimator.Estimate e = new CostEstimator.Estimate();
        e.totalTokensHigh = 59500; e.costHigh = 0.171; e.pricingAvailable = true;
        String text = TranslationDashboardFormatter.prepared(e, 11);
        assertEquals("11 chunks\n≈ 59.5k estimated tokens\nEstimated cost ≈ $0.171", text);
        assertFalse(text.contains("exact"));
        assertFalse(text.contains("Ready to validate"));
        assertEquals("1 chunk\n≈ 59.5k estimated tokens\nEstimated cost ≈ $0.171", TranslationDashboardFormatter.prepared(e, 1));
    }

    @Test public void runningDashboardHasClearUnitsAndEstimateLabels() {
        assertEquals("0/11 chunks", TranslationDashboardFormatter.progress(0, 11));
        assertEquals("Calculating…", TranslationDashboardFormatter.remaining(-1));
        assertEquals("≈ 120.0k estimated tokens", TranslationDashboardFormatter.tokens("RUNNING", 0, 120000, false));
        assertEquals("Estimated total ≈ $0.150", TranslationDashboardFormatter.cost("RUNNING", 0, 0.15, false));
        assertEquals("140 tokens used", TranslationDashboardFormatter.tokens("RUNNING", 140, 120000, true));
        assertEquals("$0.010 spent", TranslationDashboardFormatter.cost("RUNNING", 0.01, 0.15, true));
    }

    @Test public void completedProviderUsageReplacesEstimatesWithActuals() {
        assertEquals("122.1k actual tokens", TranslationDashboardFormatter.tokens("COMPLETED", 122127, 140000, true));
        assertEquals("$0.150 actual cost", TranslationDashboardFormatter.cost("COMPLETED", 0.15012075, 0.2, true));
    }

    @Test public void missingProviderUsageNeverClaimsActual() {
        assertEquals("≈ 80.0k estimated tokens", TranslationDashboardFormatter.tokens("COMPLETED", 80000, 120000, false));
        assertEquals("≈ $0.120 estimated cost", TranslationDashboardFormatter.cost("COMPLETED", 0.12, 0.15, false));
    }
}
