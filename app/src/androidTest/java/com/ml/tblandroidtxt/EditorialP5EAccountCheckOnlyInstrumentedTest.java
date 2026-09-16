package com.ml.tblandroidtxt;

import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;

/**
 * Account-only boundary for the owner-approved P5E verification.
 *
 * <p>This method deliberately has no repository, database, authorization,
 * provider or RAW runner dependency. It emits only the redacted result
 * {@code MATCH} or {@code MISMATCH}; the credential, endpoint and both
 * fingerprint values stay in process memory.</p>
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5EAccountCheckOnlyInstrumentedTest {
    private static final String OPT_IN = "p5e_account_check";
    private static final String EXPECTED = "p5e_expected_endpoint_account_fingerprint";
    private static final String RESULT = "p5e.account.result";

    @Test
    public void ownerApprovedAccountCheckOnlyReturnsMatchOrMismatch() {
        Bundle arguments = InstrumentationRegistry.getArguments();
        if (!"YES".equalsIgnoreCase(arguments.getString(OPT_IN, ""))) {
            return;
        }

        String result = "MISMATCH";
        try {
            String expected = arguments.getString(EXPECTED, "");
            if (isSha256(expected)) {
                Context target = ApplicationProvider.getApplicationContext();
                AppSettings settings = SettingsStore.load(target).copy();
                if (EditorialP5EFreshRawRoutingPolicy.matches(settings)
                        && settings.apiKey != null
                        && !settings.apiKey.trim().isEmpty()) {
                    String actual = endpointAccountFingerprint(settings);
                    if (expected.equalsIgnoreCase(actual)) {
                        result = "MATCH";
                    }
                }
            }
        } catch (Throwable ignored) {
            // An unavailable or malformed account is a redacted MISMATCH;
            // never expose an exception that could contain sensitive values.
            result = "MISMATCH";
        }

        Bundle status = new Bundle();
        status.putString(RESULT, result);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, status);
    }

    private static boolean isSha256(String value) {
        return value != null && value.matches("[0-9a-fA-F]{64}");
    }

    private static String endpointAccountFingerprint(AppSettings settings) {
        String endpoint = AppSettings.normalizeEndpoint(settings.baseUrl);
        return EditorialCanonicalJson.sha256Hex(
                (endpoint + "\n" + settings.apiKey).getBytes(StandardCharsets.UTF_8));
    }
}
