package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public final class EditorialP5COutputBudgetTest {
    @Test public void liveL1EnvelopeGetsEnoughRoomWithoutIgnoringAuthorization() {
        assertEquals(2048, EditorialP5CExactBindingExecution.boundedOutputTokens(auth(4096)));
        assertEquals(512, EditorialP5CExactBindingExecution.boundedOutputTokens(auth(512)));
    }

    private static EditorialP5PilotAuthorization auth(int outputTokens) {
        return new EditorialP5PilotAuthorization("auth", "a".repeat(64), "b".repeat(64),
                "c".repeat(64), "d".repeat(64), "evaluation", "001", "L1_RAW_DISCOVERY",
                "openrouter", "google/gemini-2.5-flash", "endpoint-account", 1, 1, 0,
                50000, outputTokens, 50000, BigDecimal.valueOf(0.10), 300000L,
                true, false, false, "HASH_ONLY", "USER", 1L, 2L, true);
    }
}
