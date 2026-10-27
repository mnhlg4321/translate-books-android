package com.ml.tblandroidtxt;

/** Puts the device's API key back into a settings snapshot that was stored without it. */
public interface SecretsResolver {
    AppSettings withCurrentSecrets(AppSettings snapshot);
}
