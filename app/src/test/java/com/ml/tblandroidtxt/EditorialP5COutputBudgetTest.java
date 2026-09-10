package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;

import org.json.JSONObject;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertFalse(body.getBoolean("stream"));
    }

    @Test public void rawStructuredRequestUsesStrictSchemaProviderRoutingAndMinimalReasoning()
            throws Exception {
        AppSettings settings = new AppSettings();
        JSONObject format = new JSONObject().put("type", "json_schema")
                .put("json_schema", new JSONObject()
                        .put("name", EditorialP5RawWireContract.SCHEMA_NAME)
                        .put("strict", true)
                        .put("schema", new JSONObject(EditorialCanonicalJson.canonicalize(
                                EditorialP5RawWireContract.jsonSchema()))));
        JSONObject body = OpenAICompatibleClient.buildChatRequestBody(settings,
                new PromptPair("system", "raw"), EditorialP5RawWireContract.OUTPUT_TOKEN_CAP,
                format, true, EditorialP5RawWireContract.REASONING_POLICY);

        assertFalse(body.getBoolean("stream"));
        assertEquals("json_schema", body.getJSONObject("response_format").getString("type"));
        assertEquals(EditorialP5RawWireContract.SCHEMA_NAME,
                body.getJSONObject("response_format").getJSONObject("json_schema")
                        .getString("name"));
        assertTrue(body.getJSONObject("response_format").getJSONObject("json_schema")
                .getBoolean("strict"));
        assertTrue(body.getJSONObject("provider").getBoolean("require_parameters"));
        assertEquals(EditorialP5RawWireContract.REASONING_POLICY,
                body.getString("reasoning_effort"));
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
