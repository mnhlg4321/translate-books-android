package com.ml.tblandroidtxt;

import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Redacted characterization of the persisted fresh RAW route.
 *
 * <p>The opt-in test is deliberately separate from the A2 preflight and live
 * harness. Its only application-owned read is one settings-store load;
 * it does not open the database, create a client/request, or perform a
 * production preflight or reach a provider. The synthetic contract tests use
 * in-memory settings
 * only and are not evidence about the current device.</p>
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5EFreshRawRouteDiagnosticInstrumentedTest {
    private static final String DIAGNOSTIC_OPT_IN = "p5e_fresh_raw_route_diagnostic";
    private static final String STATUS_PROVIDER_MATCH = "p5e.route.providerMatch";
    private static final String STATUS_MODEL_MATCH = "p5e.route.modelMatch";
    private static final String STATUS_ENDPOINT_MATCH = "p5e.route.endpointMatch";
    private static final String STATUS_ROUTE_MATCH = "p5e.route.routeMatch";

    @Test
    public void persistedRawRouteDiagnosticRunsOnlyWhenExplicitlyOptedIn() {
        Assume.assumeTrue("A3.2 diagnostic opt-in is required",
                "YES".equals(InstrumentationRegistry.getArguments()
                        .getString(DIAGNOSTIC_OPT_IN)));

        Context target = ApplicationProvider.getApplicationContext();
        AppSettings settings = SettingsStore.load(target);
        RouteMatch result = evaluate(settings);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, redactedStatus(result));
    }

    @Test
    public void exactRouteProducesAllTrue() {
        RouteMatch result = evaluate(exactSettings());
        assertTrue(result.providerMatch);
        assertTrue(result.modelMatch);
        assertTrue(result.endpointMatch);
        assertTrue(result.routeMatch);
    }

    @Test
    public void providerMismatchIsRedactedAndRoutesFalse() {
        RouteMatch result = evaluate(settings("other-provider",
                EditorialP5EFreshRawRoutingPolicy.MODEL,
                AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER)));
        assertFalse(result.providerMatch);
        assertTrue(result.modelMatch);
        assertTrue(result.endpointMatch);
        assertFalse(result.routeMatch);
    }

    @Test
    public void modelMismatchIsRedactedAndRoutesFalse() {
        RouteMatch result = evaluate(settings(
                EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                "openai/gpt-5.6-luna-variant",
                AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER)));
        assertTrue(result.providerMatch);
        assertFalse(result.modelMatch);
        assertTrue(result.endpointMatch);
        assertFalse(result.routeMatch);
    }

    @Test
    public void endpointMismatchIsRedactedAndRoutesFalse() {
        RouteMatch result = evaluate(settings(
                EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                EditorialP5EFreshRawRoutingPolicy.MODEL,
                "https://example.invalid/v1/chat/completions"));
        assertTrue(result.providerMatch);
        assertTrue(result.modelMatch);
        assertFalse(result.endpointMatch);
        assertFalse(result.routeMatch);
    }

    @Test
    public void providerComparisonIsCaseInsensitive() {
        RouteMatch result = evaluate(settings("OpenRouter",
                EditorialP5EFreshRawRoutingPolicy.MODEL,
                AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER)));
        assertTrue(result.providerMatch);
        assertTrue(result.routeMatch);
    }

    @Test
    public void modelComparisonRemainsCaseSensitive() {
        RouteMatch result = evaluate(settings(
                EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                "OPENAI/GPT-5.6-LUNA",
                AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER)));
        assertFalse(result.modelMatch);
        assertFalse(result.routeMatch);
    }

    @Test
    public void endpointNormalizationFollowsPersistedRoutePolicy() {
        String endpoint = AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        RouteMatch trailingSlash = evaluate(settings(
                EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                EditorialP5EFreshRawRoutingPolicy.MODEL, endpoint + "/"));
        RouteMatch surroundingWhitespace = evaluate(settings(
                EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                EditorialP5EFreshRawRoutingPolicy.MODEL, "  " + endpoint + "  "));
        assertTrue(trailingSlash.endpointMatch);
        assertTrue(trailingSlash.routeMatch);
        assertTrue(surroundingWhitespace.endpointMatch);
        assertTrue(surroundingWhitespace.routeMatch);
    }

    @Test
    public void providerAndModelWhitespaceIsNotSilentlyAccepted() {
        String endpoint = AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        RouteMatch providerWhitespace = evaluate(settings(
                " " + EditorialP5EFreshRawRoutingPolicy.PROVIDER + " ",
                EditorialP5EFreshRawRoutingPolicy.MODEL, endpoint));
        RouteMatch modelWhitespace = evaluate(settings(
                EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                " " + EditorialP5EFreshRawRoutingPolicy.MODEL + " ", endpoint));
        assertFalse(providerWhitespace.providerMatch);
        assertFalse(providerWhitespace.routeMatch);
        assertFalse(modelWhitespace.modelMatch);
        assertFalse(modelWhitespace.routeMatch);
    }

    @Test
    public void routeMatchIsExactlyTheConjunctionOfComponentFlags() {
        RouteMatch[] cases = {
                evaluate(exactSettings()),
                evaluate(settings("other-provider", EditorialP5EFreshRawRoutingPolicy.MODEL,
                        AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER))),
                evaluate(settings(EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                        "other-model",
                        AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER))),
                evaluate(settings(EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                        EditorialP5EFreshRawRoutingPolicy.MODEL,
                        "https://example.invalid/v1/chat/completions"))
        };
        for (RouteMatch result : cases) {
            assertEquals(result.providerMatch && result.modelMatch && result.endpointMatch,
                    result.routeMatch);
        }
    }

    @Test
    public void diagnosticStatusBundleUsesOnlyTheBooleanAllowlist() {
        Bundle status = redactedStatus(evaluate(exactSettings()));
        assertEquals(4, status.keySet().size());
        assertTrue(status.keySet().contains(STATUS_PROVIDER_MATCH));
        assertTrue(status.keySet().contains(STATUS_MODEL_MATCH));
        assertTrue(status.keySet().contains(STATUS_ENDPOINT_MATCH));
        assertTrue(status.keySet().contains(STATUS_ROUTE_MATCH));
        assertBooleanString(status, STATUS_PROVIDER_MATCH);
        assertBooleanString(status, STATUS_MODEL_MATCH);
        assertBooleanString(status, STATUS_ENDPOINT_MATCH);
        assertBooleanString(status, STATUS_ROUTE_MATCH);

        String output = status.toString().toLowerCase(Locale.ROOT);
        assertFalse(output.contains("openrouter"));
        assertFalse(output.contains("gpt-5.6-luna"));
        assertFalse(output.contains("https://"));
        assertFalse(output.contains("apikey"));
        assertFalse(output.contains("secret"));
        assertFalse(output.contains("source"));
        assertFalse(output.contains("prompt"));
        assertFalse(output.contains("request"));
        assertFalse(output.contains("response"));
    }

    static Bundle redactedStatus(RouteMatch result) {
        Bundle status = new Bundle();
        status.putString(STATUS_PROVIDER_MATCH, Boolean.toString(result.providerMatch));
        status.putString(STATUS_MODEL_MATCH, Boolean.toString(result.modelMatch));
        status.putString(STATUS_ENDPOINT_MATCH, Boolean.toString(result.endpointMatch));
        status.putString(STATUS_ROUTE_MATCH, Boolean.toString(result.routeMatch));
        return status;
    }

    private static void assertBooleanString(Bundle status, String key) {
        String value = status.getString(key);
        assertTrue(value != null && value.matches("true|false"));
    }

    static RouteMatch evaluate(AppSettings settings) {
        boolean providerMatch = settings != null
                && EditorialP5EFreshRawRoutingPolicy.PROVIDER.equalsIgnoreCase(settings.provider);
        boolean modelMatch = settings != null
                && EditorialP5EFreshRawRoutingPolicy.MODEL.equals(settings.model);
        String normalizedEndpoint = settings == null
                ? null : AppSettings.normalizeEndpoint(settings.baseUrl);
        boolean endpointMatch = AppSettings.defaultBaseUrl(
                EditorialP5EFreshRawRoutingPolicy.PROVIDER).equals(normalizedEndpoint);
        boolean routeMatch = providerMatch && modelMatch && endpointMatch;

        // Keep the redacted diagnostic tied to the production-owned predicate.
        // A future policy change must fail this characterization instead of
        // silently producing a different boolean contract.
        if (routeMatch != EditorialP5EFreshRawRoutingPolicy.matches(settings)) {
            throw new AssertionError("route policy/component predicate diverged");
        }
        return new RouteMatch(providerMatch, modelMatch, endpointMatch, routeMatch);
    }

    private static AppSettings exactSettings() {
        return settings(EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                EditorialP5EFreshRawRoutingPolicy.MODEL,
                AppSettings.defaultBaseUrl(EditorialP5EFreshRawRoutingPolicy.PROVIDER));
    }

    private static AppSettings settings(String provider, String model, String endpoint) {
        AppSettings settings = new AppSettings();
        settings.provider = provider;
        settings.model = model;
        settings.baseUrl = endpoint;
        return settings;
    }

    static final class RouteMatch {
        final boolean providerMatch;
        final boolean modelMatch;
        final boolean endpointMatch;
        final boolean routeMatch;

        RouteMatch(boolean providerMatch, boolean modelMatch, boolean endpointMatch,
                boolean routeMatch) {
            this.providerMatch = providerMatch;
            this.modelMatch = modelMatch;
            this.endpointMatch = endpointMatch;
            this.routeMatch = routeMatch;
        }

    }
}
