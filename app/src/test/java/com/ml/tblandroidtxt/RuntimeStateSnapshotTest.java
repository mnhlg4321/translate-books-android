package com.ml.tblandroidtxt;

import org.junit.Test;

import static org.junit.Assert.*;

public class RuntimeStateSnapshotTest {
    @Test public void runtimeStateRoundTripsAllVisibleJobFields() {
        RuntimeStateSnapshot before = new RuntimeStateSnapshot();
        before.jobState="PAUSED";before.status = "paused"; before.phase = "Refining"; before.fileName = "book.txt";
        before.progress = 43; before.currentChunk = 7; before.totalChunks = 16;
        before.glossaryLocks = 12; before.pronounLocks = 3;
        before.lockChunk = 7; before.lockPhase = "Refining"; before.previewChunk = 6;
        before.completed = 6; before.failed = 1; before.fallbacks = 2;
        before.log = "Paused by user"; before.preview = "Bản dịch gần nhất";
        before.totalTokens = 12345; before.totalCost = 0.4321;
        before.estimatedTotalTokens = 20000; before.estimatedTotalCost = 0.7; before.providerUsageComplete = true;
        before.elapsedMs = 90000; before.remainingMs = 120000; before.activeJobId = 99;

        RuntimeStateSnapshot after = RuntimeStateSnapshot.fromMap(before.toMap());

        assertEquals(before.jobState,after.jobState);assertEquals(before.status, after.status); assertEquals(before.phase, after.phase);
        assertEquals(before.fileName, after.fileName); assertEquals(before.progress, after.progress);
        assertEquals(before.currentChunk, after.currentChunk); assertEquals(before.totalChunks, after.totalChunks);
        assertEquals(before.glossaryLocks, after.glossaryLocks); assertEquals(before.pronounLocks, after.pronounLocks);
        assertEquals(before.lockChunk, after.lockChunk); assertEquals(before.lockPhase, after.lockPhase);
        assertEquals(before.previewChunk, after.previewChunk);
        assertEquals(before.completed, after.completed); assertEquals(before.failed, after.failed);
        assertEquals(before.fallbacks, after.fallbacks); assertEquals(before.log, after.log);
        assertEquals(before.preview, after.preview); assertEquals(before.totalTokens, after.totalTokens);
        assertEquals(before.totalCost, after.totalCost, 0.000001);
        assertEquals(before.estimatedTotalTokens, after.estimatedTotalTokens);
        assertEquals(before.estimatedTotalCost, after.estimatedTotalCost, 0.000001);
        assertEquals(before.providerUsageComplete, after.providerUsageComplete);
        assertEquals(before.elapsedMs, after.elapsedMs); assertEquals(before.remainingMs, after.remainingMs);
        assertEquals(before.activeJobId, after.activeJobId);
    }
}
