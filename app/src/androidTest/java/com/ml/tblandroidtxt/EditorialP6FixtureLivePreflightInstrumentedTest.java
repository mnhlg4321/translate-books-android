package com.ml.tblandroidtxt;

import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/** Pure preflight cases and one opt-in zero-call check against the emulator's saved Settings. */
@RunWith(AndroidJUnit4.class)
public final class EditorialP6FixtureLivePreflightInstrumentedTest {
    private static AppSettings valid(String key) {
        AppSettings settings = new AppSettings();
        settings.provider = EditorialP5EFreshRawRoutingPolicy.PROVIDER;
        settings.model = EditorialP5EFreshRawRoutingPolicy.MODEL;
        settings.baseUrl = AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        settings.apiKey = key;
        return settings;
    }

    private static String fingerprint(AppSettings settings) {
        return EditorialP6FixtureLivePreflight.check(settings, "0".repeat(64))
                .equals(EditorialP6FixtureLivePreflight.FINGERPRINT_MISMATCH)
                ? com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson.sha256Hex(
                (AppSettings.normalizeEndpoint(settings.baseUrl) + "\n" + settings.apiKey)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                : "";
    }

    @Test public void missingExpectedFingerprintIsRejectedBeforeProviderConstruction() {
        assertEquals(EditorialP6FixtureLivePreflight.EXPECTED_FINGERPRINT_REQUIRED,
                EditorialP6FixtureLivePreflight.check(valid("synthetic-key"), ""));
    }

    @Test public void wrongRouteIsRejectedBeforeProviderConstruction() {
        AppSettings settings = valid("synthetic-key");
        settings.model = "openai/gpt-5.6-luna";
        assertEquals(EditorialP6FixtureLivePreflight.ROUTE_MISMATCH,
                EditorialP6FixtureLivePreflight.check(settings, "0".repeat(64)));
    }

    @Test public void missingKeyIsRejectedBeforeProviderConstruction() {
        assertEquals(EditorialP6FixtureLivePreflight.KEY_MISSING,
                EditorialP6FixtureLivePreflight.check(valid(""), "0".repeat(64)));
    }

    @Test public void wrongFingerprintIsRejectedAndMatchingFingerprintPasses() {
        AppSettings settings = valid("synthetic-key");
        String actual = fingerprint(settings);
        assertEquals(EditorialP6FixtureLivePreflight.FINGERPRINT_MISMATCH,
                EditorialP6FixtureLivePreflight.check(settings, "0".repeat(64)));
        assertEquals(EditorialP6FixtureLivePreflight.MATCH,
                EditorialP6FixtureLivePreflight.check(settings, actual));
    }

    @Test public void liveModeRejectsTheDeliberatelyWrongEmulatorFingerprintBeforeAnyDispatch() {
        Bundle arguments = InstrumentationRegistry.getArguments();
        Assume.assumeTrue("zero-call live preflight is opt-in",
                "YES".equalsIgnoreCase(arguments.getString("p6_fixture_live", ""))
                        && "YES".equalsIgnoreCase(arguments.getString("p6_live_preflight_only", "")));
        String expected = arguments.getString("p6_expected_endpoint_account_fingerprint", "");
        Assume.assumeTrue("the harness must supply a valid deliberately wrong SHA-256",
                expected.matches("[0-9a-fA-F]{64}") && !expected.matches("0{64}"));
        AppSettings settings = SettingsStore.load(ApplicationProvider.getApplicationContext()).copy();
        assertEquals(EditorialP6FixtureLivePreflight.FINGERPRINT_MISMATCH,
                EditorialP6FixtureLivePreflight.check(settings, expected));
        Bundle status = new Bundle();
        status.putString("p6.live.preflight.result", "FINGERPRINT_MISMATCH");
        status.putInt("p6.live.preflight.providerCalls", 0);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, status);
    }
}
