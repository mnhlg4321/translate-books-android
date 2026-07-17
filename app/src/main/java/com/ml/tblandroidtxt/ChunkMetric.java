package com.ml.tblandroidtxt;

/** Privacy-safe local telemetry: counts and hashes only, never book or prompt text. */
public class ChunkMetric {
    public long jobId, durationMs;
    public int chunkIndex, attempt, sourceChars, rawTokens, systemTokens, instructionTokens;
    public int glossaryTokens, pronounTokens, historyTokens, contextTokens, estimatedInputTokens;
    public int actualInputTokens, outputTokens, retryTokens, maxOutputTokens, baselineInputTokens, cachedInputTokens;
    public double estimatedCost, usageCost;
    public String phase = "", preset = "", provider = "", model = "", outcome = "", retryReason = "";
    public String usageSource = "UNKNOWN", costStatus = "UNKNOWN_USAGE", logicalRequestId = "";
}
