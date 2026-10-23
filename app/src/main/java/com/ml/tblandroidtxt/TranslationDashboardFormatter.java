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

    /** One concise Vietnamese line for the not-running Translate screen. */
    public static String preparedCompact(CostEstimator.Estimate estimate, int chunks) {
        String chunkLine = Math.max(0, chunks) + " chunk";
        if (estimate == null) return chunkLine + " · token chưa có · Chi phí ước tính chưa có";
        String tokens = "≈ " + compactTokensVi(estimate.totalTokensHigh) + " token";
        String cost = estimate.pricingAvailable && !Double.isNaN(estimate.costHigh)
                ? "Chi phí ước tính ≈ " + money3Vi(estimate.costHigh)
                : "Chi phí ước tính chưa có";
        return chunkLine + " · " + tokens + " · " + cost;
    }

    public static String progress(int completed, int total) {
        return Math.max(0, completed) + "/" + Math.max(0, total) + " chunks";
    }

    public static String currentChunk(int current, int total) {
        if (current <= 0 || total <= 0) return "—";
        return Math.min(current, total) + "/" + total;
    }

    public static String lockCount(int count) {
        if (count < 0) return "—";
        return count + (count == 1 ? " rule" : " rules");
    }

    public static String lockMeta(String phase, int lockChunk, int total) {
        if (lockChunk <= 0) return "Rule usage appears when a chunk request is prepared.";
        String label = phase == null || phase.trim().isEmpty() ? "Current prompt" : phase.trim();
        return label + " • chunk " + lockChunk + (total > 0 ? "/" + total : "") + " • exact prompt usage";
    }

    public static String previewMeta(int previewChunk, int total, String preview) {
        return previewMeta(previewChunk, total, preview, "");
    }

    public static String previewMeta(int previewChunk, int total, String preview, String file) {
        if (preview == null || preview.trim().isEmpty()) return "Bản dịch gần nhất\nChưa có chunk được chấp nhận";
        String source = file == null || file.trim().isEmpty() ? "" : "\n" + file;
        return "Bản dịch gần nhất" + (previewChunk > 0 ? " · Chunk " + previewChunk + (total > 0 ? "/" + total : "") : "") + source;
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

    static String compactTokensVi(long value) {
        long safe = Math.max(0, value);
        if (safe < 1000) return String.valueOf(safe);
        return String.format(java.util.Locale.forLanguageTag("vi-VN"), "%.1fk", safe / 1000d);
    }

    static String money3(double value) {
        return String.format(java.util.Locale.US, "$%.3f", Math.max(0d, value));
    }

    static String money3Vi(double value) {
        return String.format(java.util.Locale.US, "$%.3f", Math.max(0d, value)).replace('.', ',');
    }
}
