package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.CheckSpec;
import com.ml.tblandroidtxt.editorial.api.EditPromptBuilder;
import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;

import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** JVM-only: nothing here reaches a transport. */
public final class OpenRouterEditorialApiProviderTest {
    @Test public void editOutputCeilingFollowsTheDraftWithAFloorAndACap() {
        assertEquals(4096, OpenRouterEditorialApiProvider.editMaxOutputTokens(100));
        assertEquals(8000, OpenRouterEditorialApiProvider.editMaxOutputTokens(10_000));
        assertEquals(32_000, OpenRouterEditorialApiProvider.editMaxOutputTokens(1_000_000));
    }

    @Test public void theCheckResponseFormatIsTheStrictSchemaOfTheSpec() throws Exception {
        JSONObject format = OpenRouterEditorialApiProvider.checkResponseFormat();
        assertEquals("json_schema", format.getString("type"));
        JSONObject schema = format.getJSONObject("json_schema");
        assertEquals("editorial_api_check_v1", schema.getString("name"));
        assertTrue(schema.getBoolean("strict"));
        JSONObject root = schema.getJSONObject("schema");
        assertFalse(root.getBoolean("additionalProperties"));
        assertEquals(CheckSpec.topLevel().size() + 1, root.getJSONArray("required").length());
        JSONObject items = root.getJSONObject("properties").getJSONObject("issues").getJSONObject("items");
        assertEquals(CheckSpec.issueFields().size(), items.getJSONArray("required").length());
    }

    @Test public void aMissingKeyIsAFailureAnswerNotAnExceptionAndNothingIsSent() {
        AppSettings settings = new AppSettings();
        settings.apiKey = "";
        OpenRouterEditorialApiProvider provider = new OpenRouterEditorialApiProvider(settings);
        EditorialApiFlow flow = new EditorialApiFlow(new EditInputs("a", "b", "Vietnamese", List.of(), ""),
                EditorialApiFlow.Config.of(EditorialApiContract.Mode.QUICK));
        EditorialApiFlow.StepResponse response = provider.call(flow.nextRequest(), "", 4096, 1000);
        assertEquals("API_KEY_MISSING", response.error());
    }

    @Test public void failureTextCarriesTheExceptionTypeAndNeverMoreThanOneShortLine() {
        String text = OpenRouterEditorialApiProvider.describe(new java.io.IOException("first line\nsecond line " + "x".repeat(500)));
        assertTrue(text.startsWith("IOException: first line second line"));
        assertTrue(text.length() < 200);
        assertEquals("IllegalStateException", OpenRouterEditorialApiProvider.describe(new IllegalStateException()));
    }

    @Test public void failureTextNeverCarriesTheKeyOrABearerToken() {
        String text = OpenRouterEditorialApiProvider.describe(
                new java.io.IOException("401 for key my-secret-key-123 header Bearer abc.def and sk-or-v1-abcdef123456"), "my-secret-key-123");
        assertFalse(text, text.contains("my-secret-key-123"));
        assertFalse(text, text.contains("abc.def"));
        assertFalse(text, text.contains("sk-or-v1"));
        assertTrue(text, text.contains("[key]"));
    }

    @Test public void onlyProvenPreDispatchFailuresAreDefinite() {
        assertTrue(OpenRouterEditorialApiProvider.notDispatched(new IllegalArgumentException("API key is empty")));
        assertTrue(OpenRouterEditorialApiProvider.notDispatched(new java.net.UnknownHostException("openrouter.ai")));
        assertTrue(OpenRouterEditorialApiProvider.notDispatched(new java.net.ConnectException("refused")));
        assertTrue(OpenRouterEditorialApiProvider.notDispatched(new ApiHttpException(401, 0, "unauthorized")));
        assertTrue(OpenRouterEditorialApiProvider.notDispatched(new ApiHttpException(429, 1000, "rate limited")));
        // after sending, the charge cannot be told
        assertFalse(OpenRouterEditorialApiProvider.notDispatched(new java.net.SocketTimeoutException("read timed out")));
        assertFalse(OpenRouterEditorialApiProvider.notDispatched(new java.io.IOException("connection reset")));
        assertFalse(OpenRouterEditorialApiProvider.notDispatched(new ApiHttpException(503, 0, "unavailable")));
        assertFalse(OpenRouterEditorialApiProvider.notDispatched(new ApiHttpException(408, 0, "timeout")));
        assertFalse(OpenRouterEditorialApiProvider.notDispatched(new RuntimeException("No choices returned")));
    }

    @Test public void theEditPromptStaysSeparateFromTheLegacyPromptPlan() {
        assertTrue(EditPromptBuilder.OUTPUT_CONTRACT.contains("<EDITED>"));
        assertFalse(EditPromptBuilder.OUTPUT_CONTRACT.contains("<TRANSLATION>"));
    }
}
