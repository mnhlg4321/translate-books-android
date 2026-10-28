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
}
