package com.ml.tblandroidtxt;

import com.sun.jna.platform.win32.Crypt32Util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The API key, encrypted for the current Windows user with DPAPI and kept in secrets/api_key.bin. The key is never written
 * to prefs, logs, the database or the persisted settings JSON.
 */
public final class DpapiSecrets implements SecretsResolver {
    private final Path file;

    public DpapiSecrets(Path secretsDir) { this.file = secretsDir.resolve("api_key.bin"); }

    public boolean hasKey() { return Files.exists(file); }

    public void save(String apiKey) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Files.deleteIfExists(file);
            return;
        }
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.write(file, Crypt32Util.cryptProtectData(apiKey.trim().getBytes(StandardCharsets.UTF_8)));
    }

    /** The key, or an empty string when none is saved or it cannot be decrypted (another Windows user). */
    public String load() {
        try {
            if (!Files.exists(file)) return "";
            return new String(Crypt32Util.cryptUnprotectData(Files.readAllBytes(file)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /** "&bull;&bull;&bull;&bull;" plus the last four characters, never the key. */
    public String masked() {
        String key = load();
        if (key.isEmpty()) return "";
        return "••••" + key.substring(Math.max(0, key.length() - 4));
    }

    @Override public AppSettings withCurrentSecrets(AppSettings snapshot) {
        AppSettings hydrated = snapshot == null ? new AppSettings() : snapshot.copy();
        hydrated.apiKey = load();
        return hydrated;
    }

    /**
     * Checks the shape of an API key before it is saved; returns null when it looks right, else a Vietnamese message that never
     * contains the key. OpenRouter keys are exactly sk-or-v1- plus 64 hex characters; other providers need one token without
     * whitespace of a plausible length. A key pasted twice is called out.
     */
    public static String validate(String provider, String key) {
        String k = key == null ? "" : key.trim();
        if (k.isEmpty()) return "API key đang trống.";
        for (int i = 0; i < k.length(); i++) {
            if (Character.isWhitespace(k.charAt(i))) return "API key có khoảng trắng hoặc xuống dòng ở giữa; hãy dán lại đúng một key.";
        }
        int prefixes = 0;
        for (int at = k.indexOf("sk-"); at >= 0; at = k.indexOf("sk-", at + 3)) prefixes++;
        if (prefixes > 1) return "API key có vẻ bị dán hai lần (có " + prefixes + " lần \"sk-\"). Xóa ô, dán một lần rồi lưu.";
        if ("openrouter".equalsIgnoreCase(provider == null ? "" : provider.trim())) {
            if (!k.matches("^sk-or-v1-[0-9a-f]{64}$")) {
                return "API key OpenRouter phải có dạng sk-or-v1- cùng 64 ký tự hex (73 ký tự); key vừa nhập dài " + k.length() + " ký tự.";
            }
            return null;
        }
        if (k.length() < 16 || k.length() > 200) return "Độ dài API key bất thường (" + k.length() + " ký tự); kiểm tra lại.";
        return null;
    }

    /** True when the saved key has the OpenRouter shape (checked without exposing it). */
    public boolean savedKeyHasOpenRouterShape() {
        return validate("openrouter", load()) == null;
    }

    /** Length of the saved key, 0 when none; used to report "key still invalid (length N)". */
    public int savedKeyLength() { return load().length(); }
}
