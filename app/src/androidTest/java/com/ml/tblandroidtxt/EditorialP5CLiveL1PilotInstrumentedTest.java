package com.ml.tblandroidtxt;

import android.content.Context;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Explicitly opt-in live P5C runner. The ordinary instrumentation suite
 * skips this class; a caller must pass p5c_live=YES after the final live-call
 * confirmation. It reads the persisted P4 binding and never logs secrets or
 * request/response content.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5CLiveL1PilotInstrumentedTest {
    private static final String SELECTOR = "p5c-real-mercedes-vol4-001";
    private static final String CHAPTER_KEY = "001";
    private static final String PROVIDER = "openrouter";
    private static final String MODEL = "openai/gpt-5.6-luna";
    private static final String RAW_PHASE = "L1_RAW_DISCOVERY";
    private static final String RECONCILE_PHASE = "L1_RECONCILE";
    private static final BigDecimal TOTAL_COST_PER_PHASE = BigDecimal.valueOf(0.05);
    private static final long PILOT_WINDOW_MILLIS = 5 * 60 * 1000L;

    @Test public void authorizedBindingRunsOneLiveL1Pair() {
        String optIn = InstrumentationRegistry.getArguments().getString("p5c_live", "");
        Assume.assumeTrue("live P5C is opt-in", "YES".equalsIgnoreCase(optIn));

        Context context = ApplicationProvider.getApplicationContext();
        AppSettings settings = SettingsStore.load(context);
        // The provider test suite deliberately writes a fake provider. Refuse
        // to reinterpret it as a live OpenRouter configuration.
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: provider must be OpenRouter",
                PROVIDER.equalsIgnoreCase(nullToEmpty(settings.provider)));
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: OpenRouter endpoint required",
                AppSettings.defaultBaseUrl(PROVIDER).equals(AppSettings.normalizeEndpoint(settings.baseUrl)));
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: OpenRouter API key must be entered in app Settings",
                settings.apiKey != null && !settings.apiKey.trim().isEmpty());

        // Model selection is an in-memory pilot decision; it is not persisted
        // and is intentionally the low-cost model chosen in the preflight.
        settings.provider = PROVIDER;
        settings.baseUrl = AppSettings.defaultBaseUrl(PROVIDER);
        settings.model = MODEL;
        settings.timeoutSeconds = 300;

        try (TranslationRepository database = new TranslationRepository(context)) {
            EditorialP4Binding binding = new EditorialP4BindingDao(database)
                    .findByAttemptRequestSelector(SELECTOR)
                    .orElseThrow(() -> new AssertionError("P4 binding is not persisted"));
            long projectId = projectId(database, binding.bindingIdentity());
            assertTrue("persisted P4 project id is required", projectId > 0);

            String endpointAccountFingerprint = EditorialCanonicalJson.sha256Hex(
                    (settings.baseUrl + "|" + EditorialCanonicalJson.sha256Hex(
                            settings.apiKey.getBytes(StandardCharsets.UTF_8)))
                            .getBytes(StandardCharsets.UTF_8));
            long issuedAt = System.currentTimeMillis();
            long expiresAt = issuedAt + PILOT_WINDOW_MILLIS;
            EditorialP5PilotAuthorization raw = authorization(
                    "p5c-live-raw", binding, endpointAccountFingerprint, RAW_PHASE,
                    issuedAt, expiresAt);
            EditorialP5PilotAuthorization reconcile = authorization(
                    "p5c-live-reconcile", binding, endpointAccountFingerprint, RECONCILE_PHASE,
                    issuedAt, expiresAt);

            OpenRouterEditorialP5PilotProvider provider =
                    new OpenRouterEditorialP5PilotProvider(settings, 2048);
            assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: provider configuration is incomplete",
                    provider.configured());
            EditorialP5CExactBindingExecution.Result result =
                    new EditorialP5CExactBindingExecution(database,
                            new EditorialPackStorageLayout(context.getFilesDir().toPath()))
                            .execute(projectId, SELECTOR, CHAPTER_KEY, raw, reconcile, provider);
            logResult(result);
            assertNotNull(result);
            assertTrue("live L1 did not commit: " + result.reasonCode(), result.accepted());
            assertTrue("P5C must keep execution disabled", !result.executionAllowed());
            assertTrue("P5C must remain uncertified", "NOT_CERTIFIED".equals(result.certificationState()));
        }
    }

    private static EditorialP5PilotAuthorization authorization(
            String prefix, EditorialP4Binding binding, String endpointAccountFingerprint,
            String phase, long issuedAt, long expiresAt) {
        return new EditorialP5PilotAuthorization(prefix + "-" + issuedAt,
                binding.bindingIdentity(), binding.runDeclarationIdentity(),
                binding.canonicalPackHash(), binding.canonicalProfileHash(),
                binding.compatibilityEvaluationId(), CHAPTER_KEY, phase, PROVIDER, MODEL,
                endpointAccountFingerprint, 1, 1, 0, 100000, 2048, 100000,
                TOTAL_COST_PER_PHASE, PILOT_WINDOW_MILLIS, true, false, false,
                "HASH_ONLY", "USER", issuedAt, expiresAt, true);
    }

    private static long projectId(TranslationRepository database, String bindingIdentity) {
        try (android.database.Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT project_row_id FROM editorial_p4_bindings WHERE binding_identity=?",
                new String[]{bindingIdentity})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    private static void logResult(EditorialP5CExactBindingExecution.Result result) {
        Log.i("P5C_LIVE_RESULT", "status=" + result.status()
                + " reason=" + result.reasonCode()
                + " providerCalls=" + result.providerCalls()
                + " executionAllowed=" + result.executionAllowed()
                + " certificationState=" + result.certificationState());
        logPhase("raw", result.rawResult());
        logPhase("reconcile", result.reconcileResult());
    }

    private static void logPhase(String name, EditorialP5PilotResult result) {
        if (result == null) {
            Log.i("P5C_LIVE_PHASE", "phase=" + name + " result=NOT_RUN");
            return;
        }
        EditorialP5PilotResult.Metrics metrics = result.metrics();
        Log.i("P5C_LIVE_PHASE", "phase=" + name
                + " outcome=" + result.outcome()
                + " reason=" + result.reasonCode()
                + " primaryCalls=" + metrics.primaryCalls()
                + " repairCalls=" + metrics.repairCalls()
                + " networkRetries=" + metrics.networkRetries()
                + " inputTokens=" + metrics.inputTokens()
                + " outputTokens=" + metrics.outputTokens()
                + " totalTokens=" + metrics.totalTokens()
                + " estimatedCost=" + metrics.estimatedCost()
                + " actualReportedCost=" + metrics.actualReportedCost()
                + " finishReason=" + metrics.finishReason()
                + " truncated=" + metrics.truncated()
                + " schemaValid=" + metrics.schemaValidationPassed()
                + " receiptValid=" + metrics.receiptValidationPassed()
                + " preserveDraft=" + metrics.preserveDraftCount()
                + " findings=" + metrics.findingCount()
                + " falseStops=" + metrics.falseStopCount()
                + " latencyMs=" + metrics.latencyMillis());
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
