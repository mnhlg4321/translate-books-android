package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Deterministic dry-run A/B over one locked chunk list. Never calls a provider. */
public final class BenchmarkReport {
    public static class Row {
        public int chunkIndex, sourceChars, rawTokens, baselineInputTokens, optimizedInputTokens;
        public int baselineMaxOutput, optimizedMaxOutput;
        public double savingPercent() { return baselineInputTokens <= 0 ? 0 : (baselineInputTokens - optimizedInputTokens) * 100d / baselineInputTokens; }
    }
    public final List<Row> rows = new ArrayList<>();
    public long baselineInputTokens, optimizedInputTokens, rawTokens;
    public double savingPercent() { return baselineInputTokens <= 0 ? 0 : (baselineInputTokens - optimizedInputTokens) * 100d / baselineInputTokens; }

    public static BenchmarkReport dryRun(List<Chunk> lockedChunks, AppSettings settings) {
        BenchmarkReport report = new BenchmarkReport(); if (lockedChunks == null) return report;
        AppSettings baseline = settings.copy(); baseline.optimizationPreset = "full";
        AppSettings optimized = settings.copy(); if ("full".equalsIgnoreCase(optimized.optimizationPreset)) optimized.optimizationPreset = "balanced";
        String previous = "";
        for (Chunk chunk : lockedChunks) {
            PromptPlan a = PromptPlan.forTranslation(chunk, previous, baseline);
            PromptPlan b = PromptPlan.forTranslation(chunk, previous, optimized);
            Row row = new Row(); row.chunkIndex = chunk.index; row.sourceChars = chunk.mainContent.length(); row.rawTokens = b.rawTokens;
            row.baselineInputTokens = a.totalInputTokens; row.optimizedInputTokens = b.totalInputTokens;
            // A fair dry run reports one shared configured max-output policy.
            row.baselineMaxOutput = settings.maxOutputTokens; row.optimizedMaxOutput = settings.maxOutputTokens;
            report.rows.add(row); report.baselineInputTokens += row.baselineInputTokens;
            report.optimizedInputTokens += row.optimizedInputTokens; report.rawTokens += row.rawTokens;
        }
        return report;
    }

    public String asText() {
        StringBuilder out = new StringBuilder("DRY-RUN A/B (same chunks; no API calls)\n");
        out.append("Baseline input: ").append(baselineInputTokens).append("\nOptimized input: ").append(optimizedInputTokens)
                .append("\nEstimated saving: ").append(String.format(Locale.US, "%.1f%%", savingPercent())).append("\n");
        for (Row r : rows) out.append("Chunk ").append(r.chunkIndex + 1).append(": ").append(r.baselineInputTokens)
                .append(" -> ").append(r.optimizedInputTokens).append(" (").append(String.format(Locale.US, "%.1f%%", r.savingPercent())).append(")\n");
        return out.toString();
    }
    private BenchmarkReport() {}
}
