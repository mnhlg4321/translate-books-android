package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

public final class BenchmarkReadiness {
    public static class Result {
        public final List<String> blockers = new ArrayList<>();
        public double estimatedUpperCostUsd;
        public boolean ready() { return blockers.isEmpty(); }
    }
    public static Result check(String source, AppSettings settings, double budgetUsd) {
        Result r = new Result(); AppSettings s = settings == null ? new AppSettings() : settings;
        if (source == null || source.trim().isEmpty()) r.blockers.add("SOURCE_MISSING");
        if (s.apiKey == null || s.apiKey.trim().isEmpty()) r.blockers.add("API_KEY_MISSING");
        if (s.model == null || s.model.trim().isEmpty()) r.blockers.add("MODEL_MISSING");
        ModelCatalog.ModelInfo pricing = ModelCatalog.findModelInfo(s.provider, s.model);
        if (pricing == null || !pricing.hasPricing()) r.blockers.add("PRICING_UNKNOWN");
        if (source != null && !source.trim().isEmpty()) {
            CostEstimator.Estimate estimate = CostEstimator.estimate(source, s);
            r.estimatedUpperCostUsd = Math.max(0, estimate.noCacheCostHigh) * 2.0; // Full + optimized; budget gate stays conservative.
            if (budgetUsd > 0 && r.estimatedUpperCostUsd > budgetUsd) r.blockers.add("BUDGET_EXCEEDED");
        }
        return r;
    }
    private BenchmarkReadiness() {}
}
