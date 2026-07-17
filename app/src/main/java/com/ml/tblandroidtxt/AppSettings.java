package com.ml.tblandroidtxt;

import java.util.Locale;
import java.util.Map;

public class AppSettings {
    public String provider = "openrouter";
    public String baseUrl = "https://openrouter.ai/api/v1/chat/completions";
    public String apiKey = "";
    public String model = "anthropic/claude-sonnet-4.6";
    public String sourceLanguage = "Japanese";
    public String targetLanguage = "Vietnamese";
    public String outputFilenamePattern = "{originalName}_translated.{ext}";
    public String chunkMode = "token"; // token | char
    public int maxTokensPerChunk = 450;
    public int maxCharsPerChunk = 0;
    public float softLimitRatio = 0.8f;
    public int timeoutSeconds = 300;
    public int maxAttempts = 3;
    public int initialRetryDelayMs = 1000;
    public int maxRetryDelayMs = 60000;
    public boolean retryOnEmpty = true;
    public boolean retryOnTruncation = true;
    public boolean retryOnValidationFailure = true;
    public float temperature = 0.3f;
    public int maxOutputTokens = 4096;
    public int contextChars = 400;
    public boolean contextOverlapEnabled = false;
    public String optimizationPreset = "balanced"; // quality | balanced | economy | full baseline
    public boolean refineAfter = false;
    public boolean bilingualOutput = false;
    public boolean savePartialOutput = true;
    public boolean stopOnCostLimit = false;
    public double costLimitUsd = 0.0;
    public double costWarningUsd = 0.0;
    public double maxRetryCostUsd = 0.0;
    public int maxPaidRetries = 0;
    public boolean stopWhenPricingUnknown = false;
    public String translationInstructions = "";
    public String refinementInstructions = "";
    public String instructionUri = "";
    public String instructionName = "";
    public String envUri = "";
    public String envName = "";
    public String glossaryText = "";
    public String pronounText = "";
    public String pronounUri = "";
    public String pronounName = "";
    public String selectedPronounId = "";
    public String selectedPronounName = "";
    public int glossaryInjectLimit = 80;
    public int pronounInjectLimit = 40;
    public String languageProfile = "Custom";
    public String selectedGlossaryId = "";
    public String selectedGlossaryName = "";
    public String outputUri = "";
    public String outputName = "";
    public String outputTreeUri = "";
    public String outputTreeName = "";

    public static AppSettings fromEnv(Map<String, String> env, AppSettings fallback) {
        AppSettings s = fallback.copy();
        String provider = first(env, "LLM_PROVIDER", "PROVIDER");
        if (notBlank(provider)) s.provider = provider.trim().toLowerCase(Locale.ROOT);

        String base = first(env, "BASE_URL", "API_BASE_URL", "OPENAI_BASE_URL", "OPENROUTER_BASE_URL", "API_ENDPOINT");
        if (notBlank(base)) s.baseUrl = normalizeEndpoint(base.trim());
        else s.baseUrl = defaultBaseUrl(s.provider);

        String key = first(env, "API_KEY", "OPENAI_API_KEY", "OPENROUTER_API_KEY", "DEEPSEEK_API_KEY", "GEMINI_API_KEY");
        if (notBlank(key)) s.apiKey = key.trim();

        String model = first(env, "DEFAULT_MODEL", "MODEL", "OPENAI_MODEL", "OPENROUTER_MODEL", "DEEPSEEK_MODEL");
        if (notBlank(model)) s.model = model.trim();

        s.sourceLanguage = getString(env, s.sourceLanguage, "DEFAULT_SOURCE_LANGUAGE", "SOURCE_LANGUAGE");
        s.targetLanguage = getString(env, s.targetLanguage, "DEFAULT_TARGET_LANGUAGE", "TARGET_LANGUAGE");
        s.languageProfile = getString(env, s.languageProfile, "LANGUAGE_PROFILE", "TRANSLATION_PROFILE");
        s.glossaryInjectLimit = getInt(env, s.glossaryInjectLimit, "GLOSSARY_INJECT_LIMIT");
        s.pronounInjectLimit = getInt(env, s.pronounInjectLimit, "PRONOUN_INJECT_LIMIT");
        s.outputFilenamePattern = getString(env, s.outputFilenamePattern, "OUTPUT_FILENAME_PATTERN", "NAMING_CONVENTION");
        s.chunkMode = getString(env, s.chunkMode, "CHUNK_MODE").toLowerCase(Locale.ROOT);
        s.maxTokensPerChunk = getInt(env, s.maxTokensPerChunk, "MAX_TOKENS_PER_CHUNK");
        s.maxCharsPerChunk = getInt(env, s.maxCharsPerChunk, "MAX_CHARS_PER_CHUNK");
        s.softLimitRatio = getFloat(env, s.softLimitRatio, "SOFT_LIMIT_RATIO");
        s.timeoutSeconds = getInt(env, s.timeoutSeconds, "REQUEST_TIMEOUT", "TIMEOUT_SECONDS");
        s.maxAttempts = getInt(env, s.maxAttempts, "MAX_TRANSLATION_ATTEMPTS", "MAX_ATTEMPTS", "MAX_RETRIES");
        s.initialRetryDelayMs=getInt(env,s.initialRetryDelayMs,"INITIAL_RETRY_DELAY_MS");s.maxRetryDelayMs=getInt(env,s.maxRetryDelayMs,"MAX_RETRY_DELAY_MS");
        s.retryOnEmpty=getBool(env,s.retryOnEmpty,"RETRY_ON_EMPTY");s.retryOnTruncation=getBool(env,s.retryOnTruncation,"RETRY_ON_TRUNCATION");s.retryOnValidationFailure=getBool(env,s.retryOnValidationFailure,"RETRY_ON_VALIDATION_FAILURE");
        s.temperature = getFloat(env, s.temperature, "TEMPERATURE");
        s.maxOutputTokens = getInt(env, s.maxOutputTokens, "MAX_OUTPUT_TOKENS", "MAX_TOKENS");
        s.contextChars = getInt(env, s.contextChars, "CONTEXT_CHARS", "CONTEXT_BEFORE_AFTER");
        s.contextOverlapEnabled=getBool(env,s.contextOverlapEnabled,"CONTEXT_OVERLAP_ENABLED");
        s.optimizationPreset = getString(env, s.optimizationPreset, "OPTIMIZATION_PRESET", "COST_PRESET").toLowerCase(Locale.ROOT);
        s.refineAfter = getBool(env, s.refineAfter, "REFINE_AFTER_TRANSLATION", "REFINE_AFTER");
        s.bilingualOutput = getBool(env, s.bilingualOutput, "BILINGUAL_OUTPUT");
        s.savePartialOutput = getBool(env, s.savePartialOutput, "ANDROID_SAVE_PARTIAL_OUTPUT", "SAVE_PARTIAL_OUTPUT");
        s.stopOnCostLimit = getBool(env, s.stopOnCostLimit, "STOP_ON_COST_LIMIT", "COST_LIMIT_ENABLED");
        s.costLimitUsd = getDouble(env, s.costLimitUsd, "COST_LIMIT_USD", "MAX_COST_USD", "COST_LIMIT");
        s.costWarningUsd=getDouble(env,s.costWarningUsd,"COST_WARNING_USD");s.maxRetryCostUsd=getDouble(env,s.maxRetryCostUsd,"MAX_RETRY_COST_USD");s.maxPaidRetries=getInt(env,s.maxPaidRetries,"MAX_PAID_RETRIES");s.stopWhenPricingUnknown=getBool(env,s.stopWhenPricingUnknown,"STOP_WHEN_PRICING_UNKNOWN");
        return s;
    }

    public AppSettings copy() {
        AppSettings s = new AppSettings();
        s.provider = provider; s.baseUrl = baseUrl; s.apiKey = apiKey; s.model = model;
        s.sourceLanguage = sourceLanguage; s.targetLanguage = targetLanguage; s.outputFilenamePattern = outputFilenamePattern;
        s.chunkMode = chunkMode; s.maxTokensPerChunk = maxTokensPerChunk; s.maxCharsPerChunk = maxCharsPerChunk;
        s.softLimitRatio = softLimitRatio; s.timeoutSeconds = timeoutSeconds; s.maxAttempts = maxAttempts;
        s.initialRetryDelayMs=initialRetryDelayMs;s.maxRetryDelayMs=maxRetryDelayMs;s.retryOnEmpty=retryOnEmpty;s.retryOnTruncation=retryOnTruncation;s.retryOnValidationFailure=retryOnValidationFailure;
        s.temperature = temperature; s.maxOutputTokens = maxOutputTokens; s.contextChars = contextChars;
        s.contextOverlapEnabled=contextOverlapEnabled;
        s.optimizationPreset = optimizationPreset;
        s.refineAfter = refineAfter; s.bilingualOutput = bilingualOutput; s.savePartialOutput = savePartialOutput;
        s.stopOnCostLimit = stopOnCostLimit; s.costLimitUsd = costLimitUsd;
        s.costWarningUsd=costWarningUsd;s.maxRetryCostUsd=maxRetryCostUsd;s.maxPaidRetries=maxPaidRetries;s.stopWhenPricingUnknown=stopWhenPricingUnknown;
        s.translationInstructions = translationInstructions; s.refinementInstructions = refinementInstructions;
        s.instructionUri = instructionUri; s.instructionName = instructionName; s.envUri = envUri; s.envName = envName;
        s.glossaryText = glossaryText;
        s.pronounText = pronounText; s.pronounUri = pronounUri; s.pronounName = pronounName; s.selectedPronounId=selectedPronounId; s.selectedPronounName=selectedPronounName;
        s.glossaryInjectLimit = glossaryInjectLimit; s.pronounInjectLimit = pronounInjectLimit; s.languageProfile = languageProfile;
        s.selectedGlossaryId = selectedGlossaryId; s.selectedGlossaryName = selectedGlossaryName;
        s.outputUri = outputUri; s.outputName = outputName; s.outputTreeUri = outputTreeUri; s.outputTreeName = outputTreeName;
        return s;
    }

    public int effectiveHardLimit() {
        if ("char".equalsIgnoreCase(chunkMode)) return maxCharsPerChunk > 0 ? maxCharsPerChunk : 3500;
        return maxTokensPerChunk > 0 ? maxTokensPerChunk : 450;
    }

    public int effectiveSoftLimit() {
        return Math.max(1, Math.round(effectiveHardLimit() * Math.max(0.1f, Math.min(1.0f, softLimitRatio))));
    }

    public String buildOutputName(String inputName) {
        String name = inputName == null || inputName.trim().isEmpty() ? "translated.txt" : inputName.trim();
        String ext = "txt";
        String base = name;
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            base = name.substring(0, dot);
            ext = name.substring(dot + 1);
        }
        String pattern = outputFilenamePattern == null || outputFilenamePattern.trim().isEmpty()
                ? "{originalName}_translated.{ext}" : outputFilenamePattern.trim();
        String out = pattern
                .replace("{originalName}", base)
                .replace("{sourceLang}", sourceLanguage == null ? "" : sourceLanguage)
                .replace("{targetLang}", targetLanguage == null ? "" : targetLanguage)
                .replace("{model}", safeModelName(model))
                .replace("{ext}", ext);
        return FileUtil.sanitizeOutputName(out);
    }

    private static String safeModelName(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "model";
        return raw.replace('/', '-').replace(':', '-').replace(' ', '_');
    }

    public static String defaultBaseUrl(String provider) {
        String p = provider == null ? "" : provider.toLowerCase(Locale.ROOT);
        if (p.contains("openrouter")) return "https://openrouter.ai/api/v1/chat/completions";
        if (p.contains("deepseek")) return "https://api.deepseek.com/v1/chat/completions";
        return "https://api.openai.com/v1/chat/completions";
    }

    public static String normalizeEndpoint(String raw) {
        if (raw == null || raw.trim().isEmpty()) return raw;
        String url = raw.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (!url.endsWith("/chat/completions")) {
            if (url.endsWith("/v1")) url = url + "/chat/completions";
            else if (url.contains("/api/generate")) url = url; // Ollama native is not implemented in this MVP.
        }
        return url;
    }

    private static boolean notBlank(String v) { return v != null && !v.trim().isEmpty(); }
    private static String first(Map<String, String> env, String... keys) {
        for (String k : keys) if (env.containsKey(k) && notBlank(env.get(k))) return env.get(k);
        return null;
    }
    private static String getString(Map<String, String> env, String def, String... keys) {
        String v = first(env, keys); return notBlank(v) ? v.trim() : def;
    }
    private static int getInt(Map<String, String> env, int def, String... keys) {
        String v = first(env, keys); if (!notBlank(v)) return def;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }
    private static float getFloat(Map<String, String> env, float def, String... keys) {
        String v = first(env, keys); if (!notBlank(v)) return def;
        try { return Float.parseFloat(v.trim()); } catch (Exception e) { return def; }
    }
    private static double getDouble(Map<String, String> env, double def, String... keys) {
        String v = first(env, keys); if (!notBlank(v)) return def;
        try { return Double.parseDouble(v.trim()); } catch (Exception e) { return def; }
    }
    private static boolean getBool(Map<String, String> env, boolean def, String... keys) {
        String v = first(env, keys); if (!notBlank(v)) return def;
        String n = v.trim().toLowerCase(Locale.ROOT);
        return n.equals("1") || n.equals("true") || n.equals("yes") || n.equals("on");
    }
}
