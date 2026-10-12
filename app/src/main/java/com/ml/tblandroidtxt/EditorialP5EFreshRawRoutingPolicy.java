package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Explicit provider route for the fresh P5E RAW boundary.
 *
 * <p>The historical P5D adapter keeps its legacy route. This policy is only
 * selected by the fresh RAW adapter, so a later live call cannot silently
 * inherit fallback routing or a different upstream.</p>
 */
public final class EditorialP5EFreshRawRoutingPolicy {
    public static final String PROVIDER = "openrouter";
    public static final String MODEL = "openai/gpt-5.6-luna";
    /** OpenRouter provider slug; the display name is "OpenAI". */
    public static final String UPSTREAM_PROVIDER = "openai";
    public static final String DATA_COLLECTION_POLICY = "deny";

    private EditorialP5EFreshRawRoutingPolicy() { }

    /** Exact OpenRouter provider preferences for the fresh RAW request. */
    public static JSONObject providerPreferences() {
        try {
            return new JSONObject()
                    .put("require_parameters", true)
                    .put("allow_fallbacks", false)
                    .put("only", new JSONArray().put(UPSTREAM_PROVIDER))
                    .put("data_collection", DATA_COLLECTION_POLICY);
        } catch (JSONException error) {
            throw new IllegalStateException("P5E fresh RAW route cannot be serialized", error);
        }
    }

    /** Stable fingerprint for the route facts included in a future manifest. */
    public static String fingerprint() {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("provider", PROVIDER);
        facts.put("model", MODEL);
        facts.put("require_parameters", true);
        facts.put("allow_fallbacks", false);
        facts.put("only", List.of(UPSTREAM_PROVIDER));
        facts.put("data_collection", DATA_COLLECTION_POLICY);
        return EditorialCanonicalJson.sha256Hex(EditorialCanonicalJson.canonicalize(facts)
                .getBytes(StandardCharsets.UTF_8));
    }

    public static boolean matches(AppSettings settings) {
        if (settings == null || !PROVIDER.equalsIgnoreCase(settings.provider)
                || !MODEL.equals(settings.model)) return false;
        String endpoint = AppSettings.normalizeEndpoint(settings.baseUrl);
        return AppSettings.defaultBaseUrl(PROVIDER).equals(endpoint);
    }
}
