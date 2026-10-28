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

    public static AppSettings fromJson(String json) {
        AppSettings s = new AppSettings();
        try {
            JSONObject o = new JSONObject(json == null ? "{}" : json);
            s.editorial = EditorialPreferences.fromJson(o.optJSONObject("editorial") == null ? "{}" : o.getJSONObject("editorial").toString());
            s.provider = o.optString("provider", s.provider); s.baseUrl = o.optString("baseUrl", s.baseUrl); s.apiKey = o.optString("apiKey", s.apiKey); s.model = o.optString("model", s.model);
            s.sourceLanguage = o.optString("sourceLanguage", s.sourceLanguage); s.targetLanguage = o.optString("targetLanguage", s.targetLanguage); s.outputFilenamePattern = o.optString("outputFilenamePattern", s.outputFilenamePattern);
            s.chunkMode = o.optString("chunkMode", s.chunkMode); s.maxTokensPerChunk = o.optInt("maxTokensPerChunk", s.maxTokensPerChunk); s.maxCharsPerChunk = o.optInt("maxCharsPerChunk", s.maxCharsPerChunk);
            s.softLimitRatio = (float)o.optDouble("softLimitRatio", s.softLimitRatio); s.timeoutSeconds = o.optInt("timeoutSeconds", s.timeoutSeconds); s.maxAttempts = o.optInt("maxAttempts", s.maxAttempts);
            s.initialRetryDelayMs=o.optInt("initialRetryDelayMs",s.initialRetryDelayMs);s.maxRetryDelayMs=o.optInt("maxRetryDelayMs",s.maxRetryDelayMs);s.retryOnEmpty=o.optBoolean("retryOnEmpty",s.retryOnEmpty);s.retryOnTruncation=o.optBoolean("retryOnTruncation",s.retryOnTruncation);s.retryOnValidationFailure=o.optBoolean("retryOnValidationFailure",s.retryOnValidationFailure);
            s.temperature = (float)o.optDouble("temperature", s.temperature); s.maxOutputTokens = o.optInt("maxOutputTokens", s.maxOutputTokens); s.contextChars = o.optInt("contextChars", s.contextChars);
            s.contextOverlapEnabled=o.optBoolean("contextOverlapEnabled",s.contextOverlapEnabled);
            s.optimizationPreset = o.optString("optimizationPreset", s.optimizationPreset);
            s.reasoningEffort = o.optString("reasoningEffort", s.reasoningEffort);
            s.refineAfter = o.optBoolean("refineAfter", s.refineAfter); s.bilingualOutput = o.optBoolean("bilingualOutput", s.bilingualOutput); s.savePartialOutput = o.optBoolean("savePartialOutput", s.savePartialOutput);
            s.stopOnCostLimit = o.optBoolean("stopOnCostLimit", s.stopOnCostLimit); s.costLimitUsd = o.optDouble("costLimitUsd", s.costLimitUsd);
            s.costWarningUsd=o.optDouble("costWarningUsd",s.costWarningUsd);s.maxRetryCostUsd=o.optDouble("maxRetryCostUsd",s.maxRetryCostUsd);s.maxPaidRetries=o.optInt("maxPaidRetries",s.maxPaidRetries);s.stopWhenPricingUnknown=o.optBoolean("stopWhenPricingUnknown",s.stopWhenPricingUnknown);
            s.translationInstructions = o.optString("translationInstructions", s.translationInstructions); s.refinementInstructions = o.optString("refinementInstructions", s.refinementInstructions);
            s.instructionUri = o.optString("instructionUri", s.instructionUri); s.instructionName = o.optString("instructionName", s.instructionName); s.envUri = o.optString("envUri", s.envUri); s.envName = o.optString("envName", s.envName);
            s.glossaryText = o.optString("glossaryText", s.glossaryText);
            s.pronounText = o.optString("pronounText", s.pronounText); s.pronounUri = o.optString("pronounUri", s.pronounUri); s.pronounName = o.optString("pronounName", s.pronounName);
            s.selectedPronounId=o.optString("selectedPronounId",s.selectedPronounId);s.selectedPronounName=o.optString("selectedPronounName",s.selectedPronounName);
            s.glossaryInjectLimit = o.optInt("glossaryInjectLimit", s.glossaryInjectLimit); s.pronounInjectLimit = o.optInt("pronounInjectLimit", s.pronounInjectLimit); s.languageProfile = o.optString("languageProfile", s.languageProfile);
            s.selectedGlossaryId = o.optString("selectedGlossaryId", s.selectedGlossaryId); s.selectedGlossaryName = o.optString("selectedGlossaryName", s.selectedGlossaryName);
            s.outputUri = o.optString("outputUri", s.outputUri); s.outputName = o.optString("outputName", s.outputName); s.outputTreeUri = o.optString("outputTreeUri", s.outputTreeUri); s.outputTreeName = o.optString("outputTreeName", s.outputTreeName);
        } catch (Exception ignored) {}
        return s;
    }

    /**
     * Settings keys shared between the phone and the PC profile files. Everything else (editorial, instructionUri, envUri,
     * outputUri, outputTreeUri, the *Name fields next to those, the Glossary/Pronoun texts and ids) is neither written nor read.
     */
    public static final String[] SHARED_KEYS = {
            "provider", "baseUrl", "model", "sourceLanguage", "targetLanguage", "outputFilenamePattern", "chunkMode",
            "maxTokensPerChunk", "maxCharsPerChunk", "softLimitRatio", "timeoutSeconds", "maxAttempts", "initialRetryDelayMs",
            "maxRetryDelayMs", "retryOnEmpty", "retryOnTruncation", "retryOnValidationFailure", "temperature", "maxOutputTokens",
            "contextChars", "contextOverlapEnabled", "reasoningEffort", "optimizationPreset", "refineAfter", "bilingualOutput",
            "savePartialOutput", "stopOnCostLimit", "costLimitUsd", "costWarningUsd", "maxRetryCostUsd", "maxPaidRetries",
            "stopWhenPricingUnknown", "translationInstructions", "refinementInstructions", "glossaryInjectLimit",
            "pronounInjectLimit", "languageProfile"};

    /** The profile file a person exports: the shared keys only, never the API key. */
    public static String toExportJson(AppSettings s) throws Exception {
        JSONObject full = new JSONObject(toJson(s));
        JSONObject out = new JSONObject();
        for (String key : SHARED_KEYS) if (full.has(key)) out.put(key, full.get(key));
        return out.toString(2);
    }

    /**
     * Reads an exported profile onto {@code current}: only shared keys present in the file are taken; every other value
     * (editorial, the output locations, and the API key unless an older file carries one) stays what {@code current} has.
     */
    public static AppSettings fromImportedProfile(String json, AppSettings current) throws Exception {
        AppSettings base = current == null ? new AppSettings() : current.copy();
        JSONObject o = new JSONObject(json == null ? "{}" : json);
        AppSettings p = fromJson(json);
        for (String key : SHARED_KEYS) if (o.has(key)) copyShared(key, p, base);
        // Profiles written before v4.22 carried the key; a file that still has one keeps importing it (as the phone always did).
        if (o.has("apiKey")) base.apiKey = p.apiKey;
        return base;
    }

    private static void copyShared(String key, AppSettings from, AppSettings to) {
        switch (key) {
            case "provider": to.provider = from.provider; break;
            case "baseUrl": to.baseUrl = from.baseUrl; break;
            case "model": to.model = from.model; break;
            case "sourceLanguage": to.sourceLanguage = from.sourceLanguage; break;
            case "targetLanguage": to.targetLanguage = from.targetLanguage; break;
            case "outputFilenamePattern": to.outputFilenamePattern = from.outputFilenamePattern; break;
            case "chunkMode": to.chunkMode = from.chunkMode; break;
            case "maxTokensPerChunk": to.maxTokensPerChunk = from.maxTokensPerChunk; break;
            case "maxCharsPerChunk": to.maxCharsPerChunk = from.maxCharsPerChunk; break;
            case "softLimitRatio": to.softLimitRatio = from.softLimitRatio; break;
            case "timeoutSeconds": to.timeoutSeconds = from.timeoutSeconds; break;
            case "maxAttempts": to.maxAttempts = from.maxAttempts; break;
            case "initialRetryDelayMs": to.initialRetryDelayMs = from.initialRetryDelayMs; break;
            case "maxRetryDelayMs": to.maxRetryDelayMs = from.maxRetryDelayMs; break;
            case "retryOnEmpty": to.retryOnEmpty = from.retryOnEmpty; break;
            case "retryOnTruncation": to.retryOnTruncation = from.retryOnTruncation; break;
            case "retryOnValidationFailure": to.retryOnValidationFailure = from.retryOnValidationFailure; break;
            case "temperature": to.temperature = from.temperature; break;
            case "maxOutputTokens": to.maxOutputTokens = from.maxOutputTokens; break;
            case "contextChars": to.contextChars = from.contextChars; break;
            case "contextOverlapEnabled": to.contextOverlapEnabled = from.contextOverlapEnabled; break;
            case "reasoningEffort": to.reasoningEffort = from.reasoningEffort; break;
            case "optimizationPreset": to.optimizationPreset = from.optimizationPreset; break;
            case "refineAfter": to.refineAfter = from.refineAfter; break;
            case "bilingualOutput": to.bilingualOutput = from.bilingualOutput; break;
            case "savePartialOutput": to.savePartialOutput = from.savePartialOutput; break;
            case "stopOnCostLimit": to.stopOnCostLimit = from.stopOnCostLimit; break;
            case "costLimitUsd": to.costLimitUsd = from.costLimitUsd; break;
            case "costWarningUsd": to.costWarningUsd = from.costWarningUsd; break;
            case "maxRetryCostUsd": to.maxRetryCostUsd = from.maxRetryCostUsd; break;
            case "maxPaidRetries": to.maxPaidRetries = from.maxPaidRetries; break;
            case "stopWhenPricingUnknown": to.stopWhenPricingUnknown = from.stopWhenPricingUnknown; break;
            case "translationInstructions": to.translationInstructions = from.translationInstructions; break;
            case "refinementInstructions": to.refinementInstructions = from.refinementInstructions; break;
            case "glossaryInjectLimit": to.glossaryInjectLimit = from.glossaryInjectLimit; break;
            case "pronounInjectLimit": to.pronounInjectLimit = from.pronounInjectLimit; break;
            case "languageProfile": to.languageProfile = from.languageProfile; break;
            default: break;
        }
    }

    /** Shared keys whose value differs between two settings, as {key, current, imported} rows (for the preview table). */
    public static java.util.List<String[]> differences(AppSettings current, AppSettings imported) throws Exception {
        JSONObject a = new JSONObject(toJson(current));
        JSONObject b = new JSONObject(toJson(imported));
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
        for (String key : SHARED_KEYS) {
            String x = String.valueOf(a.opt(key)), y = String.valueOf(b.opt(key));
            if (!x.equals(y)) rows.add(new String[]{key, x, y});
        }
        return rows;
    }
}
