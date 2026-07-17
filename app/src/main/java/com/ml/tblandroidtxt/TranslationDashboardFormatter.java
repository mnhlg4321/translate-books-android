package com.ml.tblandroidtxt;

/** Pure presentation rules for the compact Translate metrics dashboard. */
public final class TranslationDashboardFormatter {
    private TranslationDashboardFormatter() {}

    public static String prepared(CostEstimator.Estimate estimate, int chunks) {
        String chunkLine = chunks + (chunks == 1 ? " chunk" : " chunks");
        if (estimate == null) return chunkLine + "\nEstimated tokens unavailable\nEstimated cost unavailable";
        String tokens = "≈ " + compactTokens(estimate.totalTokensHigh) + " estimated tokens";
        String cost = estimate.pricingAvailable && !Double.isNaN(estimate.costHigh)
                ? "Estimated cost ≈ " + money3(estimate.costHigh)
                : "Estimated cost unavailable";
        return chunkLine + "\n" + tokens + "\n" + cost;
    }

    public static String progress(int completed, int total) {
        return Math.max(0, completed) + "/" + Math.max(0, total) + " chunks";
    }

    public static String remaining(long remainingMs) {
        return remainingMs < 0 ? "Calculating…" : "About " + CostEstimator.duration(remainingMs) + " remaining";
    }

    public static String tokens(String state, int current, int estimatedTotal, boolean providerUsageComplete) {
        boolean completed = "COMPLETED".equalsIgnoreCase(state);
        if (providerUsageComplete) return compactTokens(Math.max(0, current)) + (completed ? " actual tokens" : " tokens used");
        if (current > 0) return "≈ " + compactTokens(current) + (completed ? " estimated tokens" : " tokens used");
        return estimatedTotal > 0 ? "≈ " + compactTokens(estimatedTotal) + " estimated tokens" : "—";
    }

    public static String cost(String state, double current, double estimatedTotal, boolean providerUsageComplete) {
        boolean completed = "COMPLETED".equalsIgnoreCase(state);
        if (providerUsageComplete) return money3(Math.max(0, current)) + (completed ? " actual cost" : " spent");
        if (current > 0) return "≈ " + money3(current) + (completed ? " estimated cost" : " spent");
        return estimatedTotal > 0 ? "Estimated total ≈ " + money3(estimatedTotal) : "—";
    }

    static String compactTokens(long value) {
        long safe = Math.max(0, value);
        return safe < 1000 ? String.valueOf(safe) : String.format(java.util.Locale.US, "%.1fk", safe / 1000d);
    }

    static String money3(double value) {
        return String.format(java.util.Locale.US, "$%.3f", Math.max(0d, value));
    }
}
