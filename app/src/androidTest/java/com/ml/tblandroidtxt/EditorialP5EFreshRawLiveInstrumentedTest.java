package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * One-shot fresh RAW harness. Both entry points are opt-in by distinct
 * instrumentation arguments. The default invocation therefore performs no
 * database, settings, authorization or provider work: both tests are skipped
 * by the first executable statement in their respective methods.
 *
 * <p>The preflight method is the only A2 entry point. The live method is a
 * future, separately approved path and is intentionally never invoked by A1.
 * It reuses the existing exact-preflight fixture and the production fresh
 * runner; it does not copy lineage SQL or routing policy into the harness.</p>
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5EFreshRawLiveInstrumentedTest {
    private static final String PRODUCTION_PACKAGE = "com.ml.tblandroidtxt";
    private static final String TEST_PACKAGE = "com.ml.tblandroidtxt.test";
    private static final String PREFLIGHT_OPT_IN = "p5e_fresh_raw_preflight";
    private static final String LIVE_OPT_IN = "p5e_fresh_raw_live";

    private static final String PRODUCTION_VERSION = "v4.17-p5e.11";
    private static final long PRODUCTION_VERSION_CODE = 207L;
    private static final String PRODUCTION_APK_SHA256 =
            "2ccbb844c629132bb534b0d6aba516055c410bf96d20b14b3f80f91b962800fd";
    private static final String CERTIFICATE_SHA256 =
            "47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155";
    private static final int DATABASE_SCHEMA_VERSION = 24;
    private static final int CONTEXT_SIZE_BYTES = 80_317;
    private static final String EXPECTED_DB_SHA256 =
            "3563f44bce9e529955b6c39142243f59af8f2f0d0095303f5c7a66be07219391";

    private static final String AUTHORIZATION_ID =
            "P5E-FRESH-MERCEDES-VOL5-RAW-20260911-01";
    private static final String AUTHORIZATION_STOP_AUTHORITY =
            "OWNER_CONTROLLED|RAW_ONLY|NO_SCHEMA_REPAIR|NO_AUTOMATIC_RETRY|"
                    + "NO_RECONCILE|NO_RESPONSE_HEALING|NO_FALLBACK|"
                    + "PRESERVE_DURABLE_RECOVERY_STATE";

    private static final String[] VISIBLE_ROLES = {
            EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY
    };

    @Test
    public void freshRawExactPreflightRunsOnlyWhenExplicitlyOptedIn() throws Exception {
        Assume.assumeTrue("A2 preflight opt-in is required",
                "YES".equals(InstrumentationRegistry.getArguments()
                        .getString(PREFLIGHT_OPT_IN)));

        Bundle arguments = InstrumentationRegistry.getArguments();
        requireExactSha256(arguments, "p5e_expected_production_apk_sha256",
                PRODUCTION_APK_SHA256);
        requireExact(arguments, "p5e_expected_production_version", PRODUCTION_VERSION);
        requireExact(arguments, "p5e_expected_production_version_code",
                Long.toString(PRODUCTION_VERSION_CODE));
        requireExactSha256(arguments, "p5e_expected_certificate_sha256", CERTIFICATE_SHA256);
        requireExact(arguments, "p5e_expected_schema_version",
                Integer.toString(DATABASE_SCHEMA_VERSION));
        requireExactSha256(arguments, "p5e_expected_db_sha256", EXPECTED_DB_SHA256);
        requireExact(arguments, "p5e_expected_device_signature_token", "abebea4b");
        requireExact(arguments, "p5e_expected_selector", EditorialP5EFreshRawLiveRunner.SELECTOR);
        requireExact(arguments, "p5e_expected_chapter_key",
                EditorialP5EFreshRawLiveRunner.CHAPTER_KEY);
        requireExact(arguments, "p5e_expected_binding",
                EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY);
        requireExact(arguments, "p5e_expected_run",
                EditorialP5EFreshRawLiveRunner.RUN_DECLARATION_IDENTITY);
        requireExact(arguments, "p5e_expected_evaluation",
                EditorialP5EFreshRawLiveRunner.COMPATIBILITY_EVALUATION_ID);
        requireExact(arguments, "p5e_expected_pack_hash",
                EditorialP5EFreshRawLiveRunner.CANONICAL_PACK_HASH);
        requireExact(arguments, "p5e_expected_profile_hash",
                EditorialP5EFreshRawLiveRunner.CANONICAL_PROFILE_HASH);
        String testApkSha256 = required(arguments, "p5e_test_apk_sha256");
        requireSha256(testApkSha256, "p5e_test_apk_sha256");
        String testSourceCommit = required(arguments, "p5e_test_source_commit");
        requireCommit(testSourceCommit, "p5e_test_source_commit");

        Context target = ApplicationProvider.getApplicationContext();
        verifyPackage(target, PRODUCTION_PACKAGE, PRODUCTION_VERSION_CODE,
                PRODUCTION_APK_SHA256, CERTIFICATE_SHA256);
        Context instrumentation = InstrumentationRegistry.getInstrumentation().getContext();
        assertEquals(TEST_PACKAGE, instrumentation.getPackageName());
        verifyPackage(instrumentation, TEST_PACKAGE, -1L, testApkSha256, CERTIFICATE_SHA256);

        Path databasePath = target.getDatabasePath("tbl_android_txt.db").toPath();
        assertTrue("current database must exist", Files.isRegularFile(databasePath));
        String dbBefore = sha256File(databasePath);
        assertEquals(EXPECTED_DB_SHA256, dbBefore);

        try (TranslationRepository database = new TranslationRepository(target)) {
            EditorialP5EFreshRawBoundaryInstrumentedTest.RowCounts before =
                    EditorialP5EFreshRawBoundaryInstrumentedTest.rowCounts(
                            database.editorialReadableDatabase());
            EditorialP5EFreshRawBoundaryInstrumentedTest.PreflightFixture fixture =
                    EditorialP5EFreshRawBoundaryInstrumentedTest.loadFixture(database);
            assertEquals(CONTEXT_SIZE_BYTES, fixture.contextBytes());

            long projectId = EditorialP5EFreshRawBoundaryInstrumentedTest.projectId(
                    database, EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY);
            AppSettings settings = SettingsStore.load(target).copy();
            assertTrue("current settings must select the fresh RAW route",
                    EditorialP5EFreshRawRoutingPolicy.matches(settings));
            // The renderer does not need a credential; clearing the copy makes
            // the zero-call renderer incapable of carrying a key into a body.
            settings.apiKey = "";

            JSONObject body = new EditorialP5EFreshRawLiveRunner(database,
                    new EditorialPackStorageLayout(target.getFilesDir().toPath()))
                    .preflightOnly(projectId, EditorialP5EFreshRawLiveRunner.SELECTOR,
                            EditorialP5EFreshRawLiveRunner.CHAPTER_KEY, settings,
                            fixture.providerRequest());
            byte[] canonicalBody = canonicalBytes(body);
            JSONObject schemaObject = body.getJSONObject("response_format")
                    .getJSONObject("json_schema").getJSONObject("schema");
            byte[] schemaBytes = canonicalBytes(schemaObject);
            assertRouteBody(body);

            assertEquals(EditorialP5RawWireContract.SCHEMA_NAME,
                    body.getJSONObject("response_format").getJSONObject("json_schema")
                            .getString("name"));
            assertEquals(EditorialP5RawWireContract.worstCaseWireBytes(), 2_785);
            assertEquals(EditorialP5RawWireContract.MAX_WIRE_BYTES, 3_584);
            assertTrue("request body must not be stored in the manifest",
                    canonicalBody.length > 0);

            EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck lineage =
                    EditorialP5EFreshRawLiveRunner.inspectLineage(database,
                            EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY);
            assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.UNUSED,
                    lineage.status());
            assertLineageZero(lineage);

            EditorialP5EFreshRawBoundaryInstrumentedTest.RowCounts after =
                    EditorialP5EFreshRawBoundaryInstrumentedTest.rowCounts(
                            database.editorialReadableDatabase());
            assertEquals(before, after);
            String dbAfter = sha256File(databasePath);
            assertEquals(dbBefore, dbAfter);

            JSONObject manifest = redactedPreflightManifest(arguments, fixture, projectId,
                    canonicalBody, schemaBytes, dbBefore, before, after, lineage, testSourceCommit);
            // This is metadata only: source content, prompt, body, key and
            // provider response are deliberately absent.
            Log.i("P5E_9B_A2_PREFLIGHT", manifest.toString());
        }
    }

    @Test
    public void authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn() throws Exception {
        Assume.assumeTrue("live opt-in is required and is forbidden during A1",
                "YES".equals(InstrumentationRegistry.getArguments()
                        .getString(LIVE_OPT_IN)));

        Bundle arguments = InstrumentationRegistry.getArguments();
        // Every live fact is required. There are no empty/default live values.
        String authorizationId = required(arguments, "p5e_authorization_id");
        String authorizationIdHash = required(arguments, "p5e_authorization_id_hash");
        String ownerManifestHash = required(arguments, "p5e_owner_approval_manifest_sha256");
        String expectedAttemptIdentity = required(arguments, "p5e_expected_attempt_identity");
        String expectedRequestIdentity = required(arguments, "p5e_expected_request_identity");
        String expectedEnvelopeHash = required(arguments, "p5e_expected_request_envelope_hash");
        String expectedBodyHash = required(arguments, "p5e_expected_canonical_request_body_sha256");
        String expectedRouteFingerprint = required(arguments, "p5e_expected_route_fingerprint");
        String expectedEndpointAccountFingerprint = required(arguments,
                "p5e_expected_endpoint_account_fingerprint");
        String expectedProjectId = required(arguments, "p5e_expected_project_row_id");
        String expectedDeviceSignatureToken = required(arguments,
                "p5e_expected_device_signature_token");

        requireSha256(authorizationIdHash, "p5e_authorization_id_hash");
        requireSha256(ownerManifestHash, "p5e_owner_approval_manifest_sha256");
        requireSha256(expectedAttemptIdentity, "p5e_expected_attempt_identity");
        requireSha256(expectedRequestIdentity, "p5e_expected_request_identity");
        requireSha256(expectedEnvelopeHash, "p5e_expected_request_envelope_hash");
        requireSha256(expectedBodyHash, "p5e_expected_canonical_request_body_sha256");
        requireSha256(expectedRouteFingerprint, "p5e_expected_route_fingerprint");
        requireSha256(expectedEndpointAccountFingerprint,
                "p5e_expected_endpoint_account_fingerprint");
        requireExact(arguments, "p5e_authorization_id", AUTHORIZATION_ID);
        assertEquals(sha256(authorizationId.getBytes(StandardCharsets.UTF_8)),
                authorizationIdHash.toLowerCase(Locale.ROOT));
        requireExact(arguments, "p5e_expected_device_signature_token", "abebea4b");
        requireExact(arguments, "p5e_expected_provider", EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        requireExact(arguments, "p5e_expected_model", EditorialP5EFreshRawRoutingPolicy.MODEL);
        requireExact(arguments, "p5e_expected_upstream_provider",
                EditorialP5EFreshRawRoutingPolicy.UPSTREAM_PROVIDER);
        requireExactSha256(arguments, "p5e_expected_route_fingerprint",
                EditorialP5EFreshRawRoutingPolicy.fingerprint());
        requireExact(arguments, "p5e_expected_phase", "L1_RAW_DISCOVERY");
        requireExact(arguments, "p5e_expected_selector", EditorialP5EFreshRawLiveRunner.SELECTOR);
        requireExact(arguments, "p5e_expected_chapter_key", EditorialP5EFreshRawLiveRunner.CHAPTER_KEY);
        requireExact(arguments, "p5e_expected_binding", EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY);
        requireExact(arguments, "p5e_expected_run", EditorialP5EFreshRawLiveRunner.RUN_DECLARATION_IDENTITY);
        requireExact(arguments, "p5e_expected_evaluation",
                EditorialP5EFreshRawLiveRunner.COMPATIBILITY_EVALUATION_ID);
        requireExact(arguments, "p5e_expected_pack_hash",
                EditorialP5EFreshRawLiveRunner.CANONICAL_PACK_HASH);
        requireExact(arguments, "p5e_expected_profile_hash",
                EditorialP5EFreshRawLiveRunner.CANONICAL_PROFILE_HASH);
        requireExact(arguments, "p5e_expected_schema_version",
                Integer.toString(DATABASE_SCHEMA_VERSION));
        requireExact(arguments, "p5e_expected_production_version_code",
                Long.toString(PRODUCTION_VERSION_CODE));
        requireExactSha256(arguments, "p5e_expected_production_apk_sha256",
                PRODUCTION_APK_SHA256);
        requireExactSha256(arguments, "p5e_expected_certificate_sha256", CERTIFICATE_SHA256);
        requireExactSha256(arguments, "p5e_expected_db_sha256", EXPECTED_DB_SHA256);

        int maximumPrimaryCalls = requiredInt(arguments, "p5e_maximum_primary_semantic_calls");
        int maximumSchemaRepairs = requiredInt(arguments, "p5e_maximum_schema_repair_calls");
        int maximumNetworkRetries = requiredInt(arguments, "p5e_maximum_network_retries");
        int maximumInputTokens = requiredInt(arguments, "p5e_maximum_input_tokens");
        int maximumOutputTokens = requiredInt(arguments, "p5e_maximum_output_tokens");
        int maximumTotalTokens = requiredInt(arguments, "p5e_maximum_total_tokens");
        BigDecimal maximumCost = requiredDecimal(arguments, "p5e_maximum_total_cost_usd");
        long maximumExecutionMillis = requiredLong(arguments, "p5e_maximum_execution_time_ms");
        long issuedAtMillis = requiredLong(arguments, "p5e_authorization_issued_at_ms");
        long expiresAtMillis = requiredLong(arguments, "p5e_authorization_expires_at_ms");
        String evidenceRedactionPolicy = required(arguments, "p5e_evidence_redaction_policy");
        String cancellationStopAuthority = required(arguments, "p5e_cancellation_stop_authority");
        requireExact(arguments, "p5e_evidence_redaction_policy", "HASH_ONLY");
        requireExact(arguments, "p5e_cancellation_stop_authority", AUTHORIZATION_STOP_AUTHORITY);
        assertEquals(1, maximumPrimaryCalls);
        assertEquals(0, maximumSchemaRepairs);
        assertEquals(0, maximumNetworkRetries);
        assertEquals(100_000, maximumInputTokens);
        assertEquals(EditorialP5RawWireContract.OUTPUT_TOKEN_CAP, maximumOutputTokens);
        assertEquals(104_096, maximumTotalTokens);
        assertEquals(new BigDecimal("0.05"), maximumCost);
        assertEquals(120_000L, maximumExecutionMillis);

        Context target = ApplicationProvider.getApplicationContext();
        verifyPackage(target, PRODUCTION_PACKAGE, PRODUCTION_VERSION_CODE,
                PRODUCTION_APK_SHA256, CERTIFICATE_SHA256);
        assertEquals("abebea4b", expectedDeviceSignatureToken);
        Path databasePath = target.getDatabasePath("tbl_android_txt.db").toPath();
        assertEquals(EXPECTED_DB_SHA256, sha256File(databasePath));

        // This is the first point at which a future explicitly approved live
        // run may read the credential. It is never logged or put in evidence.
        AppSettings settings = SettingsStore.load(target).copy();
        assertTrue(EditorialP5EFreshRawRoutingPolicy.matches(settings));
        assertTrue(settings.apiKey != null && !settings.apiKey.trim().isEmpty());
        String endpointAccountFingerprint = endpointAccountFingerprint(settings);
        assertEquals(expectedEndpointAccountFingerprint, endpointAccountFingerprint);

        try (TranslationRepository database = new TranslationRepository(target)) {
            EditorialP5EFreshRawBoundaryInstrumentedTest.RowCounts before =
                    EditorialP5EFreshRawBoundaryInstrumentedTest.rowCounts(
                            database.editorialReadableDatabase());
            assertEquals(0L, before.attempts());
            assertEquals(0L, before.authorizations());
            assertEquals(0L, before.reconciliation());
            assertEquals(0L, before.reconciliationHistory());
            assertEquals(0L, before.lifecycle());
            assertEquals(0L, before.reportOrReceipt());

            EditorialP5EFreshRawBoundaryInstrumentedTest.PreflightFixture fixture =
                    EditorialP5EFreshRawBoundaryInstrumentedTest.loadFixture(database);
            long projectId = EditorialP5EFreshRawBoundaryInstrumentedTest.projectId(
                    database, EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY);
            assertEquals(Long.parseLong(expectedProjectId), projectId);

            JSONObject body = new EditorialP5EFreshRawLiveRunner(database,
                    new EditorialPackStorageLayout(target.getFilesDir().toPath()))
                    .preflightOnly(projectId, EditorialP5EFreshRawLiveRunner.SELECTOR,
                            EditorialP5EFreshRawLiveRunner.CHAPTER_KEY, settings,
                            fixture.providerRequest());
            byte[] canonicalBody = canonicalBytes(body);
            assertEquals(expectedBodyHash.toLowerCase(Locale.ROOT), sha256(canonicalBody));
            assertEquals(expectedAttemptIdentity, fixture.request().attemptIdentity());
            assertEquals(expectedRequestIdentity, fixture.request().requestIdentity());
            assertEquals(expectedEnvelopeHash, fixture.providerRequest().requestEnvelopeHash());
            EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck lineage =
                    EditorialP5EFreshRawLiveRunner.inspectLineage(database,
                            EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY);
            assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.UNUSED,
                    lineage.status());
            assertLineageZero(lineage);

            // Authorization is constructed only after all exact checks above.
            EditorialP5PilotAuthorization authorization = new EditorialP5PilotAuthorization(
                    authorizationId,
                    EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY,
                    EditorialP5EFreshRawLiveRunner.RUN_DECLARATION_IDENTITY,
                    EditorialP5EFreshRawLiveRunner.CANONICAL_PACK_HASH,
                    EditorialP5EFreshRawLiveRunner.CANONICAL_PROFILE_HASH,
                    EditorialP5EFreshRawLiveRunner.COMPATIBILITY_EVALUATION_ID,
                    EditorialP5EFreshRawLiveRunner.CHAPTER_KEY,
                    "L1_RAW_DISCOVERY",
                    EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                    EditorialP5EFreshRawRoutingPolicy.MODEL,
                    endpointAccountFingerprint,
                    maximumPrimaryCalls,
                    maximumSchemaRepairs,
                    maximumNetworkRetries,
                    maximumInputTokens,
                    maximumOutputTokens,
                    maximumTotalTokens,
                    maximumCost,
                    maximumExecutionMillis,
                    true,
                    false,
                    false,
                    evidenceRedactionPolicy,
                    cancellationStopAuthority,
                    issuedAtMillis,
                    expiresAtMillis,
                    true);
            // The only dispatch in this future path is the fresh RAW runner.
            EditorialP5CExactBindingExecution.Result result =
                    new EditorialP5EFreshRawLiveRunner(database,
                            new EditorialPackStorageLayout(target.getFilesDir().toPath()))
                            .dispatchRaw(projectId, EditorialP5EFreshRawLiveRunner.SELECTOR,
                                    EditorialP5EFreshRawLiveRunner.CHAPTER_KEY, authorization,
                                    settings);
            assertTrue("fresh harness may make at most one provider call",
                    result.providerCalls() <= 1);
            assertFalse(result.executionAllowed());
            assertEquals("NOT_CERTIFIED", result.certificationState());
            if (result.status() == EditorialP5CExactBindingExecution.Status.COMMITTED) {
                assertCommittedRawResult(result, fixture.request());
            }
        }
    }

    private static void assertCommittedRawResult(
            EditorialP5CExactBindingExecution.Result result,
            EditorialP5PilotRequest request) {
        assertEquals(1, result.providerCalls());
        assertNotNull(result.rawResult());
        EditorialP5PilotResult.Metrics metrics = result.rawResult().metrics();
        assertEquals(1, metrics.primaryCalls());
        assertEquals(0, metrics.repairCalls());
        assertEquals(0, metrics.networkRetries());
        assertFalse(metrics.truncated());
        assertTrue(metrics.schemaValidationPassed());
        assertTrue(metrics.receiptValidationPassed());
        assertTrue(metrics.costAccountingComplete());
        assertTrue(metrics.finishReason() != null && !"length".equalsIgnoreCase(
                metrics.finishReason()));
        EditorialP5PilotResult.CommittedResult committed =
                result.rawResult().committedResult();
        assertNotNull(committed);
        assertEquals(request.attemptIdentity(), committed.attemptIdentity());
        assertEquals(request.requestIdentity(), committed.requestIdentity());
        assertTrue(committed.reportBytes().length > 0);
        assertTrue(committed.receiptBytes().length > 0);
        assertNotNull(committed.output());
        assertEquals(new String(request.source(EditorialSafe4Contract.RAW).bytes(),
                StandardCharsets.UTF_8), committed.output().beforeText());
        assertEquals(committed.output().beforeText(), committed.output().afterText());
        assertTrue(committed.output().declaredChanges().isEmpty());
    }

    private static JSONObject redactedPreflightManifest(
            Bundle arguments,
            EditorialP5EFreshRawBoundaryInstrumentedTest.PreflightFixture fixture,
            long projectId,
            byte[] canonicalBody,
            byte[] schemaBytes,
            String dbSha256,
            EditorialP5EFreshRawBoundaryInstrumentedTest.RowCounts before,
            EditorialP5EFreshRawBoundaryInstrumentedTest.RowCounts after,
            EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck lineage,
            String testSourceCommit) throws JSONException {
        JSONObject manifest = new JSONObject();
        manifest.put("manifestVersion", "p5e.9b.a2.redacted-preflight.v1");
        manifest.put("execution", "HOST_TEMPLATE_ONLY_UNTIL_A2");
        manifest.put("productionPackage", PRODUCTION_PACKAGE);
        manifest.put("productionVersion", PRODUCTION_VERSION);
        manifest.put("productionVersionCode", PRODUCTION_VERSION_CODE);
        manifest.put("productionApkSha256", PRODUCTION_APK_SHA256);
        manifest.put("productionCertificateSha256", CERTIFICATE_SHA256);
        manifest.put("testPackage", TEST_PACKAGE);
        manifest.put("testApkSha256", required(arguments, "p5e_test_apk_sha256"));
        manifest.put("testSourceCommit", testSourceCommit);
        manifest.put("testRunner", "androidx.test.runner.AndroidJUnitRunner");
        manifest.put("dbSha256", dbSha256);
        manifest.put("schemaVersion", DATABASE_SCHEMA_VERSION);
        manifest.put("projectRowId", projectId);
        manifest.put("selector", EditorialP5EFreshRawLiveRunner.SELECTOR);
        manifest.put("chapterKey", EditorialP5EFreshRawLiveRunner.CHAPTER_KEY);
        manifest.put("bindingIdentity", EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY);
        manifest.put("runDeclarationIdentity", EditorialP5EFreshRawLiveRunner.RUN_DECLARATION_IDENTITY);
        manifest.put("compatibilityEvaluationId",
                EditorialP5EFreshRawLiveRunner.COMPATIBILITY_EVALUATION_ID);
        manifest.put("canonicalPackHash", EditorialP5EFreshRawLiveRunner.CANONICAL_PACK_HASH);
        manifest.put("canonicalProfileHash", EditorialP5EFreshRawLiveRunner.CANONICAL_PROFILE_HASH);
        manifest.put("attemptIdentity", fixture.request().attemptIdentity());
        manifest.put("requestIdentity", fixture.request().requestIdentity());
        manifest.put("requestEnvelopeHash", fixture.providerRequest().requestEnvelopeHash());
        manifest.put("canonicalHttpRequestBodySha256", sha256(canonicalBody));
        manifest.put("canonicalHttpRequestBodyBytes", canonicalBody.length);
        manifest.put("jsonSchemaSha256", sha256(schemaBytes));
        manifest.put("jsonSchemaBytes", schemaBytes.length);
        manifest.put("wireSchemaVersion", EditorialP5RawWireContract.SCHEMA_VERSION);
        manifest.put("worstCaseWireBytes", EditorialP5RawWireContract.worstCaseWireBytes());
        manifest.put("maximumWireBytes", EditorialP5RawWireContract.MAX_WIRE_BYTES);
        manifest.put("outputTokenCap", EditorialP5RawWireContract.OUTPUT_TOKEN_CAP);
        manifest.put("contextSizeBytes", fixture.contextBytes());
        manifest.put("sourceProjection", "RAW_AND_GLOSSARY_VISIBLE_DRAFT_AND_PRONOUN_HIDDEN");
        manifest.put("sources", sourceInventory(fixture.request()));
        manifest.put("packAuthorityRequired", true);
        manifest.put("routeFingerprint", EditorialP5EFreshRawRoutingPolicy.fingerprint());
        manifest.put("provider", EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        manifest.put("model", EditorialP5EFreshRawRoutingPolicy.MODEL);
        manifest.put("upstreamProvider", EditorialP5EFreshRawRoutingPolicy.UPSTREAM_PROVIDER);
        manifest.put("stream", false);
        manifest.put("responseFormat", "json_schema");
        manifest.put("strict", true);
        manifest.put("reasoningEffort", EditorialP5RawWireContract.REASONING_POLICY);
        manifest.put("requireParameters", true);
        manifest.put("allowFallbacks", false);
        manifest.put("only", new JSONArray().put(
                EditorialP5EFreshRawRoutingPolicy.UPSTREAM_PROVIDER));
        manifest.put("dataCollection", EditorialP5EFreshRawRoutingPolicy.DATA_COLLECTION_POLICY);
        manifest.put("plugins", "ABSENT");
        manifest.put("providerCalls", 0);
        manifest.put("dbCountsBefore", counts(before));
        manifest.put("dbCountsAfter", counts(after));
        manifest.put("lineageCounts", lineageCounts(lineage));
        manifest.put("authorizationCreated", false);
        manifest.put("attemptCreated", false);
        manifest.put("requestBodyStored", false);
        manifest.put("fullModelResponseStored", false);
        return manifest;
    }

    private static JSONArray sourceInventory(EditorialP5PilotRequest request)
            throws JSONException {
        ArrayList<EditorialP5PilotRequest.SourceBytes> sources =
                new ArrayList<>(request.sources());
        sources.sort(Comparator.comparing(EditorialP5PilotRequest.SourceBytes::role));
        JSONArray result = new JSONArray();
        for (EditorialP5PilotRequest.SourceBytes source : sources) {
            JSONObject value = new JSONObject();
            value.put("role", source.role());
            value.put("visibility", isVisible(source.role()) ? "VISIBLE" : "HIDDEN");
            value.put("byteLength", source.actualByteLength());
            value.put("sha256", source.actualSha256());
            value.put("schemaId", source.schemaId());
            value.put("ordinal", source.ordinal());
            result.put(value);
        }
        return result;
    }

    private static boolean isVisible(String role) {
        for (String value : VISIBLE_ROLES) if (value.equals(role)) return true;
        return false;
    }

    private static JSONObject counts(
            EditorialP5EFreshRawBoundaryInstrumentedTest.RowCounts counts)
            throws JSONException {
        return new JSONObject()
                .put("attempts", counts.attempts())
                .put("authorizationReceipts", counts.authorizations())
                .put("reconciliation", counts.reconciliation())
                .put("reconciliationHistory", counts.reconciliationHistory())
                .put("lifecycle", counts.lifecycle())
                .put("reportOrReceipt", counts.reportOrReceipt());
    }

    private static JSONObject lineageCounts(
            EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck lineage)
            throws JSONException {
        return new JSONObject()
                .put("status", lineage.status().name())
                .put("attempts", lineage.attempts())
                .put("authorizationReceipts", lineage.authorizationReceipts())
                .put("reconciliation", lineage.reconciliation())
                .put("reconciliationHistory", lineage.reconciliationHistory())
                .put("lifecycle", lineage.lifecycle());
    }

    private static void assertRouteBody(JSONObject body) throws JSONException {
        assertEquals(EditorialP5EFreshRawRoutingPolicy.MODEL, body.getString("model"));
        assertFalse(body.getBoolean("stream"));
        assertEquals(EditorialP5RawWireContract.OUTPUT_TOKEN_CAP,
                body.getInt("max_tokens"));
        assertEquals("json_schema", body.getJSONObject("response_format").getString("type"));
        assertTrue(body.getJSONObject("response_format").getJSONObject("json_schema")
                .getBoolean("strict"));
        JSONObject provider = body.getJSONObject("provider");
        assertTrue(provider.getBoolean("require_parameters"));
        assertFalse(provider.getBoolean("allow_fallbacks"));
        assertEquals(EditorialP5EFreshRawRoutingPolicy.UPSTREAM_PROVIDER,
                provider.getJSONArray("only").getString(0));
        assertEquals(EditorialP5EFreshRawRoutingPolicy.DATA_COLLECTION_POLICY,
                provider.getString("data_collection"));
        assertEquals(EditorialP5RawWireContract.REASONING_POLICY,
                body.getString("reasoning_effort"));
        assertFalse(body.has("plugins"));
    }

    private static void assertLineageZero(
            EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck lineage) {
        assertEquals(0L, lineage.attempts());
        assertEquals(0L, lineage.authorizationReceipts());
        assertEquals(0L, lineage.reconciliation());
        assertEquals(0L, lineage.reconciliationHistory());
        assertEquals(0L, lineage.lifecycle());
    }

    private static String endpointAccountFingerprint(AppSettings settings) {
        String endpoint = AppSettings.normalizeEndpoint(settings.baseUrl);
        return sha256((endpoint + "\n" + settings.apiKey)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static PackageInfo verifyPackage(Context context, String packageName,
                                              long expectedVersionCode,
                                              String expectedApkSha256,
                                              String expectedCertificateSha256)
            throws Exception {
        int flags = Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo info = context.getPackageManager().getPackageInfo(packageName, flags);
        assertEquals(packageName, info.packageName);
        if (expectedVersionCode >= 0) {
            long actualVersionCode = Build.VERSION.SDK_INT >= 28
                    ? info.getLongVersionCode() : info.versionCode;
            assertEquals(expectedVersionCode, actualVersionCode);
        }
        ApplicationInfo applicationInfo = info.applicationInfo;
        assertNotNull(applicationInfo);
        assertNotNull(applicationInfo.sourceDir);
        assertEquals(expectedApkSha256.toLowerCase(Locale.ROOT),
                sha256File(Paths.get(applicationInfo.sourceDir)));
        assertEquals(expectedCertificateSha256.toLowerCase(Locale.ROOT),
                certificateSha256(info));
        return info;
    }

    private static String certificateSha256(PackageInfo info) {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= 28) {
            SigningInfo signingInfo = info.signingInfo;
            assertNotNull(signingInfo);
            signatures = signingInfo.hasMultipleSigners()
                    ? signingInfo.getApkContentsSigners()
                    : signingInfo.getSigningCertificateHistory();
        } else {
            signatures = info.signatures;
        }
        assertNotNull(signatures);
        assertTrue(signatures.length > 0);
        return sha256(signatures[0].toByteArray());
    }

    private static byte[] canonicalBytes(JSONObject object) {
        return EditorialCanonicalJson.canonicalize(
                EditorialCanonicalJson.parse(object.toString().getBytes(StandardCharsets.UTF_8)))
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256File(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = messageDigest();
            byte[] buffer = new byte[16_384];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
            return hex(digest.digest());
        }
    }

    private static String sha256(byte[] bytes) {
        MessageDigest digest = messageDigest();
        digest.update(bytes);
        return hex(digest.digest());
    }

    private static MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return result.toString();
    }

    private static String required(Bundle arguments, String key) {
        String value = arguments == null ? null : arguments.getString(key);
        if (value == null || value.trim().isEmpty()) {
            throw new AssertionError("missing required live/preflight argument: " + key);
        }
        return value.trim();
    }

    private static void requireExact(Bundle arguments, String key, String expected) {
        assertEquals(key, expected, required(arguments, key));
    }

    private static void requireExactSha256(Bundle arguments, String key, String expected) {
        String actual = required(arguments, key);
        requireSha256(actual, key);
        assertEquals(key, expected.toLowerCase(Locale.ROOT), actual.toLowerCase(Locale.ROOT));
    }

    private static void requireSha256(String value, String key) {
        assertTrue(key + " must be a full SHA-256", value.matches("(?i)[0-9a-f]{64}"));
    }

    private static void requireCommit(String value, String key) {
        assertTrue(key + " must be a full Git commit", value.matches("[0-9a-fA-F]{40}"));
    }

    private static int requiredInt(Bundle arguments, String key) {
        try {
            return Integer.parseInt(required(arguments, key));
        } catch (NumberFormatException error) {
            throw new AssertionError(key + " must be an integer", error);
        }
    }

    private static long requiredLong(Bundle arguments, String key) {
        try {
            return Long.parseLong(required(arguments, key));
        } catch (NumberFormatException error) {
            throw new AssertionError(key + " must be a long", error);
        }
    }

    private static BigDecimal requiredDecimal(Bundle arguments, String key) {
        try {
            return new BigDecimal(required(arguments, key));
        } catch (NumberFormatException error) {
            throw new AssertionError(key + " must be a decimal", error);
        }
    }
}
