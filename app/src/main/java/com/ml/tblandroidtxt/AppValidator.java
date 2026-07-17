package com.ml.tblandroidtxt;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

public class AppValidator {
    public static AppSettings normalize(AppSettings s) {
        if (s == null) s = new AppSettings();
        s.provider = clean(s.provider, "custom").toLowerCase(Locale.ROOT);
        s.baseUrl = AppSettings.normalizeEndpoint(clean(s.baseUrl, AppSettings.defaultBaseUrl(s.provider)));
        s.model = ModelCatalog.normalizeModelId(clean(s.model, ""));
        s.sourceLanguage = clean(s.sourceLanguage, "Japanese");
        s.targetLanguage = clean(s.targetLanguage, "Vietnamese");
        s.outputFilenamePattern = clean(s.outputFilenamePattern, "{originalName}_translated.{ext}");
        s.chunkMode = clean(s.chunkMode, "token").toLowerCase(Locale.ROOT);
        if (!"token".equals(s.chunkMode) && !"char".equals(s.chunkMode)) s.chunkMode = "token";
        s.maxTokensPerChunk = clamp(s.maxTokensPerChunk, 80, 20000, 450);
        s.maxCharsPerChunk = s.maxCharsPerChunk <= 0 ? 0 : clamp(s.maxCharsPerChunk, 500, 200000, 0);
        s.softLimitRatio = clamp(s.softLimitRatio, 0.25f, 1.0f, 0.8f);
        s.timeoutSeconds = clamp(s.timeoutSeconds, 10, 1800, 300);
        s.maxAttempts = clamp(s.maxAttempts, 1, 10, 3);
        s.initialRetryDelayMs=clamp(s.initialRetryDelayMs,100,60000,1000);s.maxRetryDelayMs=clamp(s.maxRetryDelayMs,s.initialRetryDelayMs,300000,60000);
        s.temperature = clamp(s.temperature, 0f, 2f, 0.3f);
        s.maxOutputTokens = clamp(s.maxOutputTokens, 128, 32000, 4096);
        s.contextChars = s.contextChars <= 0 ? 0 : clamp(s.contextChars, 1, 5000, 400);
        s.optimizationPreset = clean(s.optimizationPreset, "balanced").toLowerCase(Locale.ROOT);
        if (!s.optimizationPreset.equals("quality") && !s.optimizationPreset.equals("balanced")
                && !s.optimizationPreset.equals("economy") && !s.optimizationPreset.equals("full")) {
            s.optimizationPreset = "balanced";
        }
        s.glossaryInjectLimit = clamp(s.glossaryInjectLimit, 0, 1000, 80);
        s.pronounInjectLimit = clamp(s.pronounInjectLimit, 0, 500, 40);
        if (s.costLimitUsd < 0) s.costLimitUsd = 0;
        if(s.costWarningUsd<0)s.costWarningUsd=0;if(s.maxRetryCostUsd<0)s.maxRetryCostUsd=0;s.maxPaidRetries=Math.max(0,Math.min(100,s.maxPaidRetries));
        return s;
    }

    public static String validateForTranslation(AppSettings s) {
        s = normalize(s);
        if (isBlank(s.sourceLanguage)) return "Chưa nhập source language";
        if (isBlank(s.targetLanguage)) return "Chưa nhập target language";
        if (s.sourceLanguage.trim().equalsIgnoreCase(s.targetLanguage.trim())) return "Source và target language đang giống nhau";
        if (isBlank(s.apiKey)) return "Chưa nhập API key";
        if (isBlank(s.model)) return "Chưa chọn model";
        if (isBlank(s.baseUrl)) return "Chưa nhập Base URL";
        if (!s.baseUrl.startsWith("https://") && !s.baseUrl.startsWith("http://")) return "Base URL phải bắt đầu bằng http:// hoặc https://";
        if (s.baseUrl.contains("/api/generate")) return "Ollama /api/generate chưa được hỗ trợ; hãy dùng endpoint OpenAI-compatible /v1/chat/completions";
        return null;
    }

    public static String validateSourceText(String text, String fileName) {
        String name = isBlank(fileName) ? "input" : fileName;
        if (text == null) return name + ": không đọc được nội dung";
        if (text.trim().isEmpty()) return name + ": file TXT rỗng";
        if (text.indexOf('\u0000') >= 0) return name + ": có vẻ không phải TXT UTF-8 hợp lệ";
        return null;
    }

    public static String readableError(Throwable t) {
        return ApiErrorParser.describe(t);
    }

    private static String clean(String v, String def) { return isBlank(v) ? def : v.trim(); }
    private static boolean isBlank(String v) { return v == null || v.trim().isEmpty(); }
    private static int clamp(int v, int min, int max, int def) { if (v <= 0 && def > 0) v = def; return Math.max(min, Math.min(max, v)); }
    private static float clamp(float v, float min, float max, float def) { if (Float.isNaN(v)) v = def; return Math.max(min, Math.min(max, v)); }
}
