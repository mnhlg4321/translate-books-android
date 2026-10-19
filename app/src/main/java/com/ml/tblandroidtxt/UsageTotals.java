package com.ml.tblandroidtxt;

import java.util.Locale;

/**
 * Tokens and money spent, added up from {@code chunk_metrics} rows (one row per call to the provider, so a retry is counted once, as the
 * real money it cost). Money counts only rows whose price is known (provider reported or derived from usage); the others are counted
 * separately so the owner sees "n calls without a known price" instead of a silently low total.
 */
final class UsageTotals {
    long inputTokens;
    long cachedInputTokens;
    long outputTokens;
    double costUsd;
    int calls;
    int unknownPriceCalls;
    /** Finished chapters in the same window (only the period totals fill this). */
    int chaptersDone;

    static boolean priceKnown(String costStatus) {
        return "PROVIDER_REPORTED".equals(costStatus) || "USAGE_DERIVED".equals(costStatus);
    }

    void add(String costStatus, double usageCost, long inputTokens, long cachedInputTokens, long outputTokens) {
        calls++;
        this.inputTokens += Math.max(0, inputTokens);
        this.cachedInputTokens += Math.max(0, cachedInputTokens);
        this.outputTokens += Math.max(0, outputTokens);
        if (priceKnown(costStatus)) costUsd += Math.max(0d, usageCost);
        else unknownPriceCalls++;
    }

    void add(UsageTotals other) {
        inputTokens += other.inputTokens; cachedInputTokens += other.cachedInputTokens; outputTokens += other.outputTokens;
        costUsd += other.costUsd; calls += other.calls; unknownPriceCalls += other.unknownPriceCalls; chaptersDone += other.chaptersDone;
    }

    /** "12.3k vào (4.1k cached) · 2.0k ra · USD 0.0123" with a note for calls of unknown price. */
    String line() {
        StringBuilder sb = new StringBuilder();
        sb.append(compact(inputTokens)).append(" vào");
        if (cachedInputTokens > 0) sb.append(" (").append(compact(cachedInputTokens)).append(" cached)");
        sb.append(" · ").append(compact(outputTokens)).append(" ra · USD ").append(money(costUsd));
        if (unknownPriceCalls > 0) sb.append(" · ").append(unknownPriceCalls).append(" lượt chưa rõ giá");
        return sb.toString();
    }

    static String compact(long n) {
        if (n >= 1_000_000) return String.format(Locale.US, "%.1fM", n / 1_000_000d);
        if (n >= 1_000) return String.format(Locale.US, "%.1fk", n / 1_000d);
        return String.valueOf(n);
    }

    static String money(double usd) {
        return usd >= 1 ? String.format(Locale.US, "%.2f", usd) : String.format(Locale.US, "%.4f", usd);
    }
}
