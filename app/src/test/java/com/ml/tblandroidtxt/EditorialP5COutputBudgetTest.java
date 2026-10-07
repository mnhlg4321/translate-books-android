package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;

import org.json.JSONObject;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

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

    @Test public void multiTurnEnvelopeKeepsSystemAndHistoryOrder() throws Exception {
        AppSettings settings = new AppSettings();
        JSONObject body = OpenAICompatibleClient.buildChatRequestBody(settings,
                List.of(new OpenAICompatibleClient.ChatMessage("system", "project"),
                        new OpenAICompatibleClient.ChatMessage("user", "L1"),
                        new OpenAICompatibleClient.ChatMessage("assistant", "REPORT"),
                        new OpenAICompatibleClient.ChatMessage("user", "L2")),
                512, null, null, "medium");
        org.json.JSONArray messages = body.getJSONArray("messages");
        assertEquals(4, messages.length());
        assertEquals("system", messages.getJSONObject(0).getString("role"));
        assertEquals("L1", messages.getJSONObject(1).getString("content"));
        assertEquals("REPORT", messages.getJSONObject(2).getString("content"));
        assertEquals("L2", messages.getJSONObject(3).getString("content"));
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

    @Test public void freshRawRoutePinsProviderAndDisablesFallbackWithoutHealing()
            throws Exception {
        AppSettings settings = new AppSettings();
        settings.provider = EditorialP5EFreshRawRoutingPolicy.PROVIDER;
        settings.model = EditorialP5EFreshRawRoutingPolicy.MODEL;
        JSONObject format = new JSONObject().put("type", "json_schema")
                .put("json_schema", new JSONObject()
                        .put("name", EditorialP5RawWireContract.SCHEMA_NAME)
                        .put("strict", true)
                        .put("schema", new JSONObject(EditorialCanonicalJson.canonicalize(
                                EditorialP5RawWireContract.jsonSchema()))));

        JSONObject body = OpenAICompatibleClient.buildChatRequestBody(settings,
                new PromptPair("system", "raw"), EditorialP5RawWireContract.OUTPUT_TOKEN_CAP,
                format, EditorialP5EFreshRawRoutingPolicy.providerPreferences(),
                EditorialP5RawWireContract.REASONING_POLICY);

        JSONObject provider = body.getJSONObject("provider");
        assertTrue(provider.getBoolean("require_parameters"));
        assertFalse(provider.getBoolean("allow_fallbacks"));
        assertEquals(EditorialP5EFreshRawRoutingPolicy.UPSTREAM_PROVIDER,
                provider.getJSONArray("only").getString(0));
        assertEquals(EditorialP5EFreshRawRoutingPolicy.DATA_COLLECTION_POLICY,
                provider.getString("data_collection"));
        assertFalse(body.has("plugins"));
        // Reasoning-model route with require_parameters: temperature is unsupported by every
        // endpoint, so it must not be sent (live HTTP 404 on 2026-10-01).
        assertFalse(body.has("temperature"));
        assertEquals(EditorialP5EFreshRawRoutingPolicy.MODEL, body.getString("model"));
        assertEquals(4096, body.getInt("max_tokens"));
        assertFalse(body.getBoolean("stream"));
    }

    @Test public void legacyRouteDoesNotInheritFreshProviderRestrictions() throws Exception {
        JSONObject body = OpenAICompatibleClient.buildChatRequestBody(new AppSettings(),
                new PromptPair("system", "legacy"), 512, null, true, "");

        assertTrue(body.getJSONObject("provider").getBoolean("require_parameters"));
        assertFalse(body.getJSONObject("provider").has("allow_fallbacks"));
        assertFalse(body.getJSONObject("provider").has("only"));
        assertFalse(body.getJSONObject("provider").has("data_collection"));
        // No reasoning request on this route, so the configured temperature is still sent.
        assertTrue(body.has("temperature"));
    }

    @Test public void temperatureIsOnlyDroppedForRequireParametersWithReasoning() throws Exception {
        AppSettings settings = new AppSettings();
        PromptPair prompt = new PromptPair("system", "synthetic");
        assertTrue(OpenAICompatibleClient.buildChatRequestBody(settings, prompt, 512)
                .has("temperature"));
        JSONObject required = new JSONObject().put("require_parameters", true);
        JSONObject notRequired = new JSONObject().put("require_parameters", false);
        assertFalse(OpenAICompatibleClient.buildChatRequestBody(settings, prompt, 512, null,
                required, "minimal").has("temperature"));
        assertTrue(OpenAICompatibleClient.buildChatRequestBody(settings, prompt, 512, null,
                required, "").has("temperature"));
        assertTrue(OpenAICompatibleClient.buildChatRequestBody(settings, prompt, 512, null,
                notRequired, "minimal").has("temperature"));
        assertTrue(OpenAICompatibleClient.buildChatRequestBody(settings, prompt, 512, null,
                (JSONObject) null, "minimal").has("temperature"));
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
