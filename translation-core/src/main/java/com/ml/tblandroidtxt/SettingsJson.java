package com.ml.tblandroidtxt;

import org.json.JSONObject;

/** Pure settings serialization shared by the app and the core; SettingsStore delegates here. */
public final class SettingsJson {
    private SettingsJson() {}

    public static String toJson(AppSettings s) {
        try {
            JSONObject o = new JSONObject();
            o.put("editorial", new JSONObject(s.editorial.toJson()));
            o.put("provider", s.provider); o.put("baseUrl", s.baseUrl); o.put("apiKey", s.apiKey); o.put("model", s.model);
            o.put("sourceLanguage", s.sourceLanguage); o.put("targetLanguage", s.targetLanguage); o.put("outputFilenamePattern", s.outputFilenamePattern);
            o.put("chunkMode", s.chunkMode); o.put("maxTokensPerChunk", s.maxTokensPerChunk); o.put("maxCharsPerChunk", s.maxCharsPerChunk);
            o.put("softLimitRatio", s.softLimitRatio); o.put("timeoutSeconds", s.timeoutSeconds); o.put("maxAttempts", s.maxAttempts);
            o.put("initialRetryDelayMs",s.initialRetryDelayMs);o.put("maxRetryDelayMs",s.maxRetryDelayMs);o.put("retryOnEmpty",s.retryOnEmpty);o.put("retryOnTruncation",s.retryOnTruncation);o.put("retryOnValidationFailure",s.retryOnValidationFailure);
            o.put("temperature", s.temperature); o.put("maxOutputTokens", s.maxOutputTokens); o.put("contextChars", s.contextChars);
            o.put("contextOverlapEnabled",s.contextOverlapEnabled);
            o.put("optimizationPreset", s.optimizationPreset);
            o.put("reasoningEffort", s.reasoningEffort);
            o.put("refineAfter", s.refineAfter); o.put("bilingualOutput", s.bilingualOutput); o.put("savePartialOutput", s.savePartialOutput);
            o.put("stopOnCostLimit", s.stopOnCostLimit); o.put("costLimitUsd", s.costLimitUsd);
            o.put("costWarningUsd",s.costWarningUsd);o.put("maxRetryCostUsd",s.maxRetryCostUsd);o.put("maxPaidRetries",s.maxPaidRetries);o.put("stopWhenPricingUnknown",s.stopWhenPricingUnknown);
            o.put("translationInstructions", s.translationInstructions); o.put("refinementInstructions", s.refinementInstructions);
            o.put("instructionUri", s.instructionUri); o.put("instructionName", s.instructionName); o.put("envUri", s.envUri); o.put("envName", s.envName);
            o.put("glossaryText", s.glossaryText);
            o.put("pronounText", s.pronounText); o.put("pronounUri", s.pronounUri); o.put("pronounName", s.pronounName);
            o.put("selectedPronounId",s.selectedPronounId);o.put("selectedPronounName",s.selectedPronounName);
            o.put("glossaryInjectLimit", s.glossaryInjectLimit); o.put("pronounInjectLimit", s.pronounInjectLimit); o.put("languageProfile", s.languageProfile);
            o.put("selectedGlossaryId", s.selectedGlossaryId); o.put("selectedGlossaryName", s.selectedGlossaryName);
            o.put("outputUri", s.outputUri); o.put("outputName", s.outputName); o.put("outputTreeUri", s.outputTreeUri); o.put("outputTreeName", s.outputTreeName);
            return o.toString();
        } catch (Exception e) { return "{}"; }
    }

    /**
     * Durable job/config snapshots deliberately omit the device secret.  The full
     * {@link #toJson(AppSettings)} form remains the in-process form used by the
     * settings screen and explicit internal intents.
     */
    public static String toPersistedJson(AppSettings s) {
        try {
            return stripPersistedSecrets(toJson(s));
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Removes only the top-level API key from an already serialized settings snapshot. */
    public static String stripPersistedSecrets(String json) throws Exception {
        JSONObject o = new JSONObject(json == null ? "{}" : json);
        o.remove("apiKey");
        return o.toString();
    }
}
