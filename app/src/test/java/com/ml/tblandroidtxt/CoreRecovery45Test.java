package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class CoreRecovery45Test {
    @Test public void stateMachineRejectsCompletionBeforeRunning() {
        assertTrue(TranslationJobState.canTransition(TranslationJobState.State.IDLE, TranslationJobState.State.VALIDATING));
        assertTrue(TranslationJobState.canTransition(TranslationJobState.State.VALIDATING, TranslationJobState.State.READY));
        assertTrue(TranslationJobState.canTransition(TranslationJobState.State.READY, TranslationJobState.State.RUNNING));
        assertFalse(TranslationJobState.canTransition(TranslationJobState.State.VALIDATING, TranslationJobState.State.COMPLETED));
        assertFalse(TranslationJobState.canTransition(TranslationJobState.State.FAILED, TranslationJobState.State.COMPLETED));
    }

    @Test public void remainingTimeIsUnknownBeforeFirstCompletedChunk() {
        assertEquals(-1L, TranslationJobState.remainingMs(TranslationJobState.State.RUNNING, 0, 10, 5000));
        assertEquals(-1L, TranslationJobState.remainingMs(TranslationJobState.State.FAILED, 0, 10, 5000));
        assertEquals(0L, TranslationJobState.remainingMs(TranslationJobState.State.COMPLETED, 10, 10, 5000));
    }

    @Test public void runtimePreparationUsesLightweightSourceOnlyEstimate() {
        AppSettings s=new AppSettings();
        Chunk c=new Chunk(0,"", "source text long enough for a deterministic estimate", "");
        CostEstimator.Estimate e=CostEstimator.estimatePreparedChunksLightweight(Arrays.asList(c),s);
        assertEquals(1,e.chunks);
        assertTrue(e.totalTokensHigh>0);
        assertEquals("lightweight source-only approximation",e.tokenizerState);
    }
}
