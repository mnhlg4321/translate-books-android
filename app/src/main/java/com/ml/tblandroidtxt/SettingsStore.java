package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public class SettingsStore {
    private static final String PREF = "settings";

    public static void save(Context c, AppSettings s) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString("provider", s.provider)
                .putString("baseUrl", s.baseUrl)
                .putString("apiKey", s.apiKey)
                .putString("model", s.model)
                .putString("sourceLanguage", s.sourceLanguage)
                .putString("targetLanguage", s.targetLanguage)
                .putString("outputFilenamePattern", s.outputFilenamePattern)
                .putString("chunkMode", s.chunkMode)
                .putInt("maxTokensPerChunk", s.maxTokensPerChunk)
                .putInt("maxCharsPerChunk", s.maxCharsPerChunk)
                .putFloat("softLimitRatio", s.softLimitRatio)
                .putInt("timeoutSeconds", s.timeoutSeconds)
                .putInt("maxAttempts", s.maxAttempts)
                .putInt("initialRetryDelayMs",s.initialRetryDelayMs).putInt("maxRetryDelayMs",s.maxRetryDelayMs)
                .putBoolean("retryOnEmpty",s.retryOnEmpty).putBoolean("retryOnTruncation",s.retryOnTruncation).putBoolean("retryOnValidationFailure",s.retryOnValidationFailure)
                .putFloat("temperature", s.temperature)
                .putInt("maxOutputTokens", s.maxOutputTokens)
                .putInt("contextChars", s.contextChars)
                .putBoolean("contextOverlapEnabled",s.contextOverlapEnabled)
                .putString("optimizationPreset", s.optimizationPreset)
                .putBoolean("refineAfter", s.refineAfter)
                .putBoolean("bilingualOutput", s.bilingualOutput)
                .putBoolean("savePartialOutput", s.savePartialOutput)
                .putBoolean("stopOnCostLimit", s.stopOnCostLimit)
                .putFloat("costLimitUsd", (float)s.costLimitUsd)
                .putFloat("costWarningUsd",(float)s.costWarningUsd).putFloat("maxRetryCostUsd",(float)s.maxRetryCostUsd).putInt("maxPaidRetries",s.maxPaidRetries).putBoolean("stopWhenPricingUnknown",s.stopWhenPricingUnknown)
                .putString("translationInstructions", s.translationInstructions)
                .putString("refinementInstructions", s.refinementInstructions)
                .putString("instructionUri", s.instructionUri)
                .putString("instructionName", s.instructionName)
                .putString("envUri", s.envUri)
                .putString("envName", s.envName)
                .putString("glossaryText", s.glossaryText)
                .putString("pronounText", s.pronounText)
                .putString("pronounUri", s.pronounUri)
                .putString("pronounName", s.pronounName)
                .putString("selectedPronounId",s.selectedPronounId)
                .putString("selectedPronounName",s.selectedPronounName)
                .putInt("glossaryInjectLimit", s.glossaryInjectLimit)
                .putInt("pronounInjectLimit", s.pronounInjectLimit)
                .putString("languageProfile", s.languageProfile)
                .putString("selectedGlossaryId", s.selectedGlossaryId)
                .putString("selectedGlossaryName", s.selectedGlossaryName)
                .putString("outputUri", s.outputUri)
                .putString("outputName", s.outputName)
                .putString("outputTreeUri", s.outputTreeUri)
                .putString("outputTreeName", s.outputTreeName)
                .apply();
    }

    public static AppSettings load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        AppSettings s = new AppSettings();
        s.provider = p.getString("provider", s.provider);
        s.baseUrl = p.getString("baseUrl", s.baseUrl);
        s.apiKey = p.getString("apiKey", s.apiKey);
        s.model = p.getString("model", s.model);
        s.sourceLanguage = p.getString("sourceLanguage", s.sourceLanguage);
        s.targetLanguage = p.getString("targetLanguage", s.targetLanguage);
        s.outputFilenamePattern = p.getString("outputFilenamePattern", s.outputFilenamePattern);
        s.chunkMode = p.getString("chunkMode", s.chunkMode);
        s.maxTokensPerChunk = p.getInt("maxTokensPerChunk", s.maxTokensPerChunk);
        s.maxCharsPerChunk = p.getInt("maxCharsPerChunk", s.maxCharsPerChunk);
        s.softLimitRatio = p.getFloat("softLimitRatio", s.softLimitRatio);
        s.timeoutSeconds = p.getInt("timeoutSeconds", s.timeoutSeconds);
        s.maxAttempts = p.getInt("maxAttempts", s.maxAttempts);
        s.initialRetryDelayMs=p.getInt("initialRetryDelayMs",s.initialRetryDelayMs);s.maxRetryDelayMs=p.getInt("maxRetryDelayMs",s.maxRetryDelayMs);s.retryOnEmpty=p.getBoolean("retryOnEmpty",s.retryOnEmpty);s.retryOnTruncation=p.getBoolean("retryOnTruncation",s.retryOnTruncation);s.retryOnValidationFailure=p.getBoolean("retryOnValidationFailure",s.retryOnValidationFailure);
        s.temperature = p.getFloat("temperature", s.temperature);
        s.maxOutputTokens = p.getInt("maxOutputTokens", s.maxOutputTokens);
        s.contextChars = p.getInt("contextChars", s.contextChars);
        s.contextOverlapEnabled=p.getBoolean("contextOverlapEnabled",s.contextOverlapEnabled);
        s.optimizationPreset = p.getString("optimizationPreset", s.optimizationPreset);
        s.refineAfter = p.getBoolean("refineAfter", s.refineAfter);
        s.bilingualOutput = p.getBoolean("bilingualOutput", s.bilingualOutput);
        s.savePartialOutput = p.getBoolean("savePartialOutput", s.savePartialOutput);
        s.stopOnCostLimit = p.getBoolean("stopOnCostLimit", s.stopOnCostLimit);
        s.costLimitUsd = p.getFloat("costLimitUsd", (float)s.costLimitUsd);
        s.costWarningUsd=p.getFloat("costWarningUsd",(float)s.costWarningUsd);s.maxRetryCostUsd=p.getFloat("maxRetryCostUsd",(float)s.maxRetryCostUsd);s.maxPaidRetries=p.getInt("maxPaidRetries",s.maxPaidRetries);s.stopWhenPricingUnknown=p.getBoolean("stopWhenPricingUnknown",s.stopWhenPricingUnknown);
        s.translationInstructions = p.getString("translationInstructions", s.translationInstructions);
        s.refinementInstructions = p.getString("refinementInstructions", s.refinementInstructions);
        s.instructionUri = p.getString("instructionUri", s.instructionUri);
        s.instructionName = p.getString("instructionName", s.instructionName);
        s.envUri = p.getString("envUri", s.envUri);
        s.envName = p.getString("envName", s.envName);
        s.glossaryText = p.getString("glossaryText", s.glossaryText);
        s.pronounText = p.getString("pronounText", s.pronounText);
        s.pronounUri = p.getString("pronounUri", s.pronounUri);
        s.pronounName = p.getString("pronounName", s.pronounName);
        s.selectedPronounId=p.getString("selectedPronounId",s.selectedPronounId);s.selectedPronounName=p.getString("selectedPronounName",s.selectedPronounName);
        s.glossaryInjectLimit = p.getInt("glossaryInjectLimit", s.glossaryInjectLimit);
        s.pronounInjectLimit = p.getInt("pronounInjectLimit", s.pronounInjectLimit);
        s.languageProfile = p.getString("languageProfile", s.languageProfile);
        s.selectedGlossaryId = p.getString("selectedGlossaryId", s.selectedGlossaryId);
        s.selectedGlossaryName = p.getString("selectedGlossaryName", s.selectedGlossaryName);
        s.outputUri = p.getString("outputUri", s.outputUri);
        s.outputName = p.getString("outputName", s.outputName);
        s.outputTreeUri = p.getString("outputTreeUri", s.outputTreeUri);
        s.outputTreeName = p.getString("outputTreeName", s.outputTreeName);
        return s;
    }

    public static String toJson(AppSettings s) {
        try {
            JSONObject o = new JSONObject();
            o.put("provider", s.provider); o.put("baseUrl", s.baseUrl); o.put("apiKey", s.apiKey); o.put("model", s.model);
            o.put("sourceLanguage", s.sourceLanguage); o.put("targetLanguage", s.targetLanguage); o.put("outputFilenamePattern", s.outputFilenamePattern);
            o.put("chunkMode", s.chunkMode); o.put("maxTokensPerChunk", s.maxTokensPerChunk); o.put("maxCharsPerChunk", s.maxCharsPerChunk);
            o.put("softLimitRatio", s.softLimitRatio); o.put("timeoutSeconds", s.timeoutSeconds); o.put("maxAttempts", s.maxAttempts);
            o.put("initialRetryDelayMs",s.initialRetryDelayMs);o.put("maxRetryDelayMs",s.maxRetryDelayMs);o.put("retryOnEmpty",s.retryOnEmpty);o.put("retryOnTruncation",s.retryOnTruncation);o.put("retryOnValidationFailure",s.retryOnValidationFailure);
            o.put("temperature", s.temperature); o.put("maxOutputTokens", s.maxOutputTokens); o.put("contextChars", s.contextChars);
            o.put("contextOverlapEnabled",s.contextOverlapEnabled);
            o.put("optimizationPreset", s.optimizationPreset);
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

    public static AppSettings fromJson(String json) {
        AppSettings s = new AppSettings();
        try {
            JSONObject o = new JSONObject(json == null ? "{}" : json);
            s.provider = o.optString("provider", s.provider); s.baseUrl = o.optString("baseUrl", s.baseUrl); s.apiKey = o.optString("apiKey", s.apiKey); s.model = o.optString("model", s.model);
            s.sourceLanguage = o.optString("sourceLanguage", s.sourceLanguage); s.targetLanguage = o.optString("targetLanguage", s.targetLanguage); s.outputFilenamePattern = o.optString("outputFilenamePattern", s.outputFilenamePattern);
            s.chunkMode = o.optString("chunkMode", s.chunkMode); s.maxTokensPerChunk = o.optInt("maxTokensPerChunk", s.maxTokensPerChunk); s.maxCharsPerChunk = o.optInt("maxCharsPerChunk", s.maxCharsPerChunk);
            s.softLimitRatio = (float)o.optDouble("softLimitRatio", s.softLimitRatio); s.timeoutSeconds = o.optInt("timeoutSeconds", s.timeoutSeconds); s.maxAttempts = o.optInt("maxAttempts", s.maxAttempts);
            s.initialRetryDelayMs=o.optInt("initialRetryDelayMs",s.initialRetryDelayMs);s.maxRetryDelayMs=o.optInt("maxRetryDelayMs",s.maxRetryDelayMs);s.retryOnEmpty=o.optBoolean("retryOnEmpty",s.retryOnEmpty);s.retryOnTruncation=o.optBoolean("retryOnTruncation",s.retryOnTruncation);s.retryOnValidationFailure=o.optBoolean("retryOnValidationFailure",s.retryOnValidationFailure);
            s.temperature = (float)o.optDouble("temperature", s.temperature); s.maxOutputTokens = o.optInt("maxOutputTokens", s.maxOutputTokens); s.contextChars = o.optInt("contextChars", s.contextChars);
            s.contextOverlapEnabled=o.optBoolean("contextOverlapEnabled",s.contextOverlapEnabled);
            s.optimizationPreset = o.optString("optimizationPreset", s.optimizationPreset);
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

    public static String toEnv(AppSettings s) {
        StringBuilder b = new StringBuilder();
        b.append("# Exported from Translate Books with LLMs\n");
        b.append("LLM_PROVIDER=").append(nz(s.provider)).append('\n');
        b.append("BASE_URL=").append(nz(s.baseUrl)).append('\n');
        if (s.apiKey != null && !s.apiKey.trim().isEmpty()) b.append("API_KEY=").append(s.apiKey.trim()).append('\n');
        b.append("DEFAULT_MODEL=").append(nz(s.model)).append('\n');
        b.append("DEFAULT_SOURCE_LANGUAGE=").append(nz(s.sourceLanguage)).append('\n');
        b.append("DEFAULT_TARGET_LANGUAGE=").append(nz(s.targetLanguage)).append('\n');
        b.append("LANGUAGE_PROFILE=").append(nz(s.languageProfile)).append('\n');
        b.append("OUTPUT_FILENAME_PATTERN=").append(nz(s.outputFilenamePattern)).append('\n');
        b.append("CHUNK_MODE=").append(nz(s.chunkMode)).append('\n');
        b.append("MAX_TOKENS_PER_CHUNK=").append(s.maxTokensPerChunk).append('\n');
        b.append("MAX_CHARS_PER_CHUNK=").append(s.maxCharsPerChunk).append('\n');
        b.append("SOFT_LIMIT_RATIO=").append(s.softLimitRatio).append('\n');
        b.append("CONTEXT_CHARS=").append(s.contextChars).append('\n');
        b.append("OPTIMIZATION_PRESET=").append(nz(s.optimizationPreset)).append('\n');
        b.append("REQUEST_TIMEOUT=").append(s.timeoutSeconds).append('\n');
        b.append("MAX_TRANSLATION_ATTEMPTS=").append(s.maxAttempts).append('\n');
        b.append("TEMPERATURE=").append(s.temperature).append('\n');
        b.append("MAX_OUTPUT_TOKENS=").append(s.maxOutputTokens).append('\n');
        b.append("REFINE_AFTER_TRANSLATION=").append(s.refineAfter).append('\n');
        b.append("BILINGUAL_OUTPUT=").append(s.bilingualOutput).append('\n');
        b.append("SAVE_PARTIAL_OUTPUT=").append(s.savePartialOutput).append('\n');
        b.append("STOP_ON_COST_LIMIT=").append(s.stopOnCostLimit).append('\n');
        b.append("COST_LIMIT_USD=").append(s.costLimitUsd).append('\n');
        b.append("GLOSSARY_INJECT_LIMIT=").append(s.glossaryInjectLimit).append('\n');
        b.append("PRONOUN_INJECT_LIMIT=").append(s.pronounInjectLimit).append('\n');
        return b.toString();
    }

    private static String nz(String v) { return v == null ? "" : v; }

}
