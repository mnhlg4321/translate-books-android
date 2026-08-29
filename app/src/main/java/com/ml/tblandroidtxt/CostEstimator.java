package com.ml.tblandroidtxt;

import java.util.List;
import java.util.Locale;

public class CostEstimator {
    public static class Estimate {
        public int sourceTokens, chunks, inputTokensLow, inputTokensHigh, outputTokensLow, outputTokensHigh;
        public int totalTokensLow, totalTokensHigh;
        public int breakdownSource, breakdownInstruction, breakdownGlossary, breakdownPronoun, breakdownContext;
        public int expectedCachedInputTokens, expectedUncachedInputTokens;
        public double costLow, costHigh, noCacheCostLow, noCacheCostHigh;
        public boolean pricingAvailable, cachePricingAvailable, genericTokenizer;
        public String tokenizerState = "heuristic approximation";
        public String pricingSource = "pricing-unavailable";
    }

    public static Estimate estimate(String text, AppSettings settings) {
        AppSettings s = settings == null ? new AppSettings() : settings;
        String src = text == null ? "" : text;
        List<Chunk> chunks = Chunker.chunkText(src, s);
        return estimatePreparedChunks(chunks, s);
    }

    /** Estimates from the production chunks already prepared for the job; never rechunks source text. */
    public static Estimate estimatePreparedChunks(List<Chunk> chunks, AppSettings settings) {
        AppSettings s = settings == null ? new AppSettings() : settings;
        Estimate e = new Estimate();
        if (chunks == null) chunks = java.util.Collections.emptyList();
        e.chunks = chunks.size();
        for (Chunk chunk : chunks) e.sourceTokens = safeAdd(e.sourceTokens, Chunker.approxTokens(chunk.mainContent));
        java.util.ArrayList<PromptPlan> plans = new java.util.ArrayList<>();
        String previousPlaceholder = "";
        for (Chunk chunk : chunks) {
            PromptPlan plan = PromptPlan.forTranslation(chunk, previousPlaceholder, s);
            plans.add(plan);
            addBreakdown(e, plan);
            previousPlaceholder = tail(chunk.mainContent, Math.max(0, s.contextChars));
        }

        ModelCatalog.ModelInfo model = ModelCatalog.findModelInfo(s.provider, s.model);
        e.genericTokenizer = model == null || (!ModelCatalog.existsInActiveCatalog(s.model) && !model.hasPricing());
        e.tokenizerState = e.genericTokenizer ? "generic approximation" : "catalog model approximation";
        e.inputTokensLow = sumInputTokens(plans);
        e.inputTokensHigh = e.genericTokenizer ? rangeHigh(e.inputTokensLow) : e.inputTokensLow;
        e.outputTokensLow = Math.round(e.sourceTokens * 1.05f);
        e.outputTokensHigh = Math.round(e.sourceTokens * 1.45f);

        if (s.refineAfter) {
            for (Chunk chunk : chunks) {
                PromptPlan plan = PromptPlan.forRefinement(chunk, chunk.mainContent, s);
                plans.add(plan);
                addBreakdown(e, plan);
            }
            e.inputTokensLow = sumInputTokens(plans);
            e.inputTokensHigh = e.genericTokenizer ? rangeHigh(e.inputTokensLow) : e.inputTokensLow;
            e.outputTokensLow += Math.round(e.outputTokensLow * 0.95f);
            e.outputTokensHigh += Math.round(e.outputTokensHigh * 1.10f);
        }

        int requestCount = Math.max(1, e.chunks * (s.refineAfter ? 2 : 1));
        e.outputTokensHigh = Math.min(e.outputTokensHigh, safeMultiply(requestCount, Math.max(128, s.maxOutputTokens)));
        e.outputTokensLow = Math.min(e.outputTokensLow, e.outputTokensHigh);
        e.totalTokensLow = safeAdd(e.inputTokensLow, e.outputTokensLow);
        e.totalTokensHigh = safeAdd(e.inputTokensHigh, e.outputTokensHigh);
        e.cachePricingAvailable = model != null && model.hasPricing() && model.cachedInputPriceKnown
                && ProviderCapabilities.forSettings(s).promptCaching;
        e.expectedCachedInputTokens = e.cachePricingAvailable ? estimateExpectedCachedTokens(plans) : 0;
        e.expectedCachedInputTokens = Math.min(e.inputTokensLow, Math.max(0, e.expectedCachedInputTokens));
        e.expectedUncachedInputTokens = Math.max(0, e.inputTokensLow - e.expectedCachedInputTokens);
        applyPricing(e, s);

        ObservabilityLog.event("estimator_input", "provider", s.provider, "model", s.model,
                "sourceTokens", e.sourceTokens, "chunks", e.chunks, "maxOutput", s.maxOutputTokens,
                "preset", s.optimizationPreset);
        ObservabilityLog.event("estimator_output", "inputLow", e.inputTokensLow, "inputHigh", e.inputTokensHigh,
                "outputLow", e.outputTokensLow, "outputHigh", e.outputTokensHigh,
                "pricing", e.pricingAvailable, "costLow", e.costLow, "costHigh", e.costHigh,
                "tokenizer", e.tokenizerState);
        return e;
    }

    /** Runtime preflight estimate that never builds prompts or reparses glossary/pronoun data. */
    public static Estimate estimatePreparedChunksLightweight(List<Chunk> chunks, AppSettings settings) {
        AppSettings s = settings == null ? new AppSettings() : settings;
        Estimate e = new Estimate();
        if (chunks == null) chunks = java.util.Collections.emptyList();
        e.chunks = chunks.size();
        for (Chunk chunk : chunks) e.sourceTokens = safeAdd(e.sourceTokens, Chunker.approxTokens(chunk.mainContent));
        int requests = Math.max(1, e.chunks * (s.refineAfter ? 2 : 1));
        e.inputTokensLow = e.sourceTokens;
        e.inputTokensHigh = rangeHigh(e.sourceTokens);
        e.outputTokensLow = Math.round(e.sourceTokens * (s.refineAfter ? 1.8f : 0.9f));
        e.outputTokensHigh = Math.min(safeMultiply(requests, Math.max(128, s.maxOutputTokens)),
                Math.round(e.sourceTokens * (s.refineAfter ? 2.8f : 1.45f)));
        e.outputTokensLow = Math.min(e.outputTokensLow, e.outputTokensHigh);
        e.totalTokensLow = safeAdd(e.inputTokensLow, e.outputTokensLow);
        e.totalTokensHigh = safeAdd(e.inputTokensHigh, e.outputTokensHigh);
        e.breakdownSource = e.sourceTokens;
        e.genericTokenizer = true;
        e.tokenizerState = "lightweight source-only approximation";
        applyPricing(e, s);
        return e;
    }

    /** Applies current model prices to stable token counts without touching source or chunks. */
    public static void applyPricing(Estimate e, AppSettings settings) {
        if (e == null) return;
        AppSettings s = settings == null ? new AppSettings() : settings;
        ModelCatalog.ModelInfo model = ModelCatalog.findModelInfo(s.provider, s.model);
        e.pricingAvailable = model != null && model.hasPricing();
        e.pricingSource = model == null ? "pricing-unavailable" : model.pricingSource;
        e.cachePricingAvailable = e.pricingAvailable && model.cachedInputPriceKnown
                && ProviderCapabilities.forSettings(s).promptCaching;
        if (!e.cachePricingAvailable) e.expectedCachedInputTokens = 0;
        e.expectedCachedInputTokens = Math.min(e.inputTokensLow, Math.max(0, e.expectedCachedInputTokens));
        e.expectedUncachedInputTokens = Math.max(0, e.inputTokensLow - e.expectedCachedInputTokens);
        e.costLow = usageCost(e.inputTokensLow, e.expectedCachedInputTokens, e.outputTokensLow, model);
        e.costHigh = usageCost(e.inputTokensHigh, e.expectedCachedInputTokens, e.outputTokensHigh, model);
        e.noCacheCostLow = cost(e.inputTokensLow, e.outputTokensLow, model);
        e.noCacheCostHigh = cost(e.inputTokensHigh, e.outputTokensHigh, model);
    }

    /** Aggregates per-file estimates for the batch card and durable prepared batch. */
    public static void addInto(Estimate total, Estimate e) {
        if (total == null || e == null) return;
        total.sourceTokens=safeAdd(total.sourceTokens,e.sourceTokens); total.chunks=safeAdd(total.chunks,e.chunks);
        total.inputTokensLow=safeAdd(total.inputTokensLow,e.inputTokensLow); total.inputTokensHigh=safeAdd(total.inputTokensHigh,e.inputTokensHigh);
        total.outputTokensLow=safeAdd(total.outputTokensLow,e.outputTokensLow); total.outputTokensHigh=safeAdd(total.outputTokensHigh,e.outputTokensHigh);
        total.totalTokensLow=safeAdd(total.totalTokensLow,e.totalTokensLow); total.totalTokensHigh=safeAdd(total.totalTokensHigh,e.totalTokensHigh);
        total.breakdownSource=safeAdd(total.breakdownSource,e.breakdownSource); total.breakdownInstruction=safeAdd(total.breakdownInstruction,e.breakdownInstruction);
        total.breakdownGlossary=safeAdd(total.breakdownGlossary,e.breakdownGlossary); total.breakdownPronoun=safeAdd(total.breakdownPronoun,e.breakdownPronoun);
        total.breakdownContext=safeAdd(total.breakdownContext,e.breakdownContext);
        total.expectedCachedInputTokens=safeAdd(total.expectedCachedInputTokens,e.expectedCachedInputTokens);
        total.expectedUncachedInputTokens=safeAdd(total.expectedUncachedInputTokens,e.expectedUncachedInputTokens);
        total.pricingAvailable = total.chunks == e.chunks ? e.pricingAvailable : total.pricingAvailable && e.pricingAvailable;
        total.cachePricingAvailable |= e.cachePricingAvailable; total.genericTokenizer |= e.genericTokenizer;
        total.tokenizerState=e.tokenizerState; total.pricingSource=e.pricingSource;
        if (total.pricingAvailable) {
            total.costLow += e.costLow; total.costHigh += e.costHigh;
            total.noCacheCostLow += e.noCacheCostLow; total.noCacheCostHigh += e.noCacheCostHigh;
        } else {
            total.costLow=Double.NaN; total.costHigh=Double.NaN; total.noCacheCostLow=Double.NaN; total.noCacheCostHigh=Double.NaN;
        }
    }

    private static int rangeHigh(int value) {
        return (int)Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, value) * 1.10d));
    }

    private static int safeAdd(int a, int b) {
        return (int)Math.min(Integer.MAX_VALUE, (long)Math.max(0, a) + Math.max(0, b));
    }

    private static int safeMultiply(int a, int b) {
        return (int)Math.min(Integer.MAX_VALUE, (long)Math.max(0, a) * Math.max(0, b));
    }

    private static void addBreakdown(Estimate e, PromptPlan p) {
        e.breakdownSource += p.rawTokens;
        e.breakdownInstruction += p.instructionTokens;
        e.breakdownGlossary += p.glossaryTokens;
        e.breakdownPronoun += p.pronounTokens;
        e.breakdownContext += p.contextTokens + p.historyTokens;
    }

    private static int sumInputTokens(List<PromptPlan> plans) {
        long total = 0;
        for (PromptPlan p : plans) total += Math.max(0, p.totalInputTokens);
        return (int)Math.min(Integer.MAX_VALUE, total);
    }

    /** Cold-batch expectation: request one warms cache; later requests reuse exact shared system prefixes. */
    private static int estimateExpectedCachedTokens(List<PromptPlan> plans) {
        if (plans == null || plans.size() < 2) return 0;
        long cached = 0;
        PromptPlan previous = plans.get(0);
        for (int i = 1; i < plans.size(); i++) {
            PromptPlan current = plans.get(i);
            String prefix = commonPrefix(previous.prompt.system, current.prompt.system);
            int tokens = Chunker.approxTokens(prefix);
            if (tokens >= 1024) cached += (tokens / 128) * 128;
            previous = current;
        }
        return (int)Math.min(Integer.MAX_VALUE, cached);
    }

    private static String commonPrefix(String a, String b) {
        String left = a == null ? "" : a, right = b == null ? "" : b;
        int max = Math.min(left.length(), right.length()), i = 0;
        while (i < max && left.charAt(i) == right.charAt(i)) i++;
        return left.substring(0, i);
    }

    private static String tail(String value, int maxChars) {
        String v = value == null ? "" : value.trim();
        if (maxChars <= 0 || v.isEmpty()) return "";
        return v.length() <= maxChars ? v : v.substring(v.length() - maxChars);
    }

    public static double cost(int inputTokens, int outputTokens, AppSettings s) {
        return cost(inputTokens, outputTokens, ModelCatalog.findModelInfo(s.provider, s.model));
    }

    public static double cost(int inputTokens, int outputTokens, ModelCatalog.ModelInfo model) {
        if (model == null || !model.hasPricing()) return Double.NaN;
        return inputTokens / 1_000_000d * model.inputPerMillion
                + outputTokens / 1_000_000d * model.outputPerMillion;
    }

    private static double usageCost(int inputTokens, int cachedInputTokens, int outputTokens,
                                    ModelCatalog.ModelInfo model) {
        return ModelCatalog.usageCost(model, inputTokens, cachedInputTokens, outputTokens);
    }

    public static String estimateLine(Estimate e, AppSettings s) {
        String cost = e.pricingAvailable ? money(e.costLow) + " - " + money(e.costHigh) : "Pricing unavailable";
        return "Estimated input: " + tokenRange(e.inputTokensLow, e.inputTokensHigh)
                + "\nEstimated output: " + tokenRange(e.outputTokensLow, e.outputTokensHigh)
                + "\nEstimated total: " + tokenRange(e.totalTokensLow, e.totalTokensHigh)
                + "\nEstimated cost: " + cost
                + "\nTokenizer: " + e.tokenizerState
                + "\nPricing source: " + e.pricingSource;
    }

    public static String money(double value) {
        if (Double.isNaN(value)) return "—";
        if (value == 0) return "$0";
        if (value < 0.01) return String.format(Locale.US, "$%.4f", value);
        if (value < 1) return String.format(Locale.US, "$%.3f", value);
        return String.format(Locale.US, "$%.2f", value);
    }

    public static String tokenRange(int low, int high) {
        return low == high ? fmt(low) + " tokens" : fmt(low) + "–" + fmt(high) + " tokens";
    }

    public static String fmt(long n) { return String.format(Locale.US, "%,d", n); }

    public static String duration(long ms) {
        if (ms < 0) ms = 0;
        long sec = ms / 1000, h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
