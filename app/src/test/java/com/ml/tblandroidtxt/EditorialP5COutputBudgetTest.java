package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;

import org.json.JSONObject;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public final class EditorialP5COutputBudgetTest {
    @Test public void liveL1EnvelopeGetsEnoughRoomWithoutIgnoringAuthorization() {
        assertEquals(4096, EditorialP5CExactBindingExecution.boundedOutputTokens(auth(4096)));
        assertEquals(512, EditorialP5CExactBindingExecution.boundedOutputTokens(auth(512)));
    }

    @Test public void httpPayloadUsesTheExactAuthorizedCap() throws Exception {
        AppSettings settings = new AppSettings();
        PromptPair prompt = new PromptPair("system", "synthetic raw discovery");
        JSONObject body = OpenAICompatibleClient.buildChatRequestBody(settings, prompt, 4096);

        assertEquals(4096, body.getInt("max_tokens"));
        assertEquals(8192,
                OpenAICompatibleClient.buildChatRequestBody(settings, prompt, 8192)
                        .getInt("max_tokens"));
        assertEquals(512,
                OpenAICompatibleClient.buildChatRequestBody(settings, prompt, 512)
                        .getInt("max_tokens"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonPositiveHttpCapIsRejectedInsteadOfClamped() throws Exception {
        OpenAICompatibleClient.buildChatRequestBody(new AppSettings(),
                new PromptPair("system", "synthetic"), 0);
    }

    private static EditorialP5PilotAuthorization auth(int outputTokens) {
        return new EditorialP5PilotAuthorization("auth", "a".repeat(64), "b".repeat(64),
                "c".repeat(64), "d".repeat(64), "evaluation", "001", "L1_RAW_DISCOVERY",
                "openrouter", "google/gemini-2.5-flash", "endpoint-account", 1, 1, 0,
                50000, outputTokens, 50000, BigDecimal.valueOf(0.10), 300000L,
                true, false, false, "HASH_ONLY", "USER", 1L, 2L, true);
    }
}
