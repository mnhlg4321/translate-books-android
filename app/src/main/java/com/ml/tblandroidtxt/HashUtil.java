package com.ml.tblandroidtxt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HashUtil {
    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return String.valueOf((text == null ? "" : text).hashCode()); }
    }

    public static String settingsHash(AppSettings s) {
        if (s == null) return "";
        String key = safe(s.provider) + "\n" + safe(s.baseUrl) + "\n" + safe(s.model) + "\n" +
                safe(s.sourceLanguage) + "\n" + safe(s.targetLanguage) + "\n" + safe(s.translationInstructions) + "\n" +
                safe(s.refinementInstructions) + "\n" + safe(s.glossaryText) + "\n" + safe(s.pronounText) + "\n" +
                s.refineAfter + "\n" + s.bilingualOutput + "\n" + s.maxTokensPerChunk + "\n" + s.maxCharsPerChunk + "\n" + s.chunkMode + "\n" +
                s.contextOverlapEnabled+"\n"+s.contextChars+"\n"+s.temperature+"\n"+s.maxOutputTokens+"\n"+s.maxAttempts+"\n"+s.initialRetryDelayMs+"\n"+s.maxRetryDelayMs+"\n"+s.optimizationPreset;
        return sha256(key);
    }
    private static String safe(String s) { return s == null ? "" : s; }
}
