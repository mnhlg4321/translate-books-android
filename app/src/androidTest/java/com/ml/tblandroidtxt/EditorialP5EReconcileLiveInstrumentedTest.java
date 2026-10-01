package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;

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
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * M4 live RECONCILE entry. It is opt-in by the instrumentation argument
 * {@code p5e_reconcile_live=YES}; without it the first statement skips the test
 * and no database, settings, authorization or provider work happens.
 *
 * <p>Unlike the RAW harness, this class hard-codes no production APK hash,
 * version, version code or database hash. The M4 production APK is not built
 * yet, so those pins are bound at event time by the host packet and passed as
 * required arguments; they are verified here against the device (installed APK
 * file hash, PackageInfo, certificate, and the database file hash before
 * dispatch). A missing or malformed argument fails the test. A typed STOP from
 * the runner is a valid recorded outcome; there is no retry.</p>
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5EReconcileLiveInstrumentedTest {
    private static final String PRODUCTION_PACKAGE = "com.ml.tblandroidtxt";
    private static final String TEST_PACKAGE = "com.ml.tblandroidtxt.test";
    private static final String LIVE_OPT_IN = "p5e_reconcile_live";
    private static final String STATUS_PREFIX = "p5e.reconcile.v1.";
    private static final String PHASE = "L1_RECONCILE";
    private static final String AUTHORIZATION_STOP_AUTHORITY =
            "OWNER_CONTROLLED|RECONCILE_ONLY|NO_SCHEMA_REPAIR|NO_AUTOMATIC_RETRY|"
                    + "NO_RESPONSE_HEALING|NO_FALLBACK|PRESERVE_DURABLE_RECOVERY_STATE";

    @Test
    public void authorizedReconcileRunsOnlyWhenExplicitlyOptedIn() throws Exception {
        Assume.assumeTrue("reconcile live opt-in is required",
                "YES".equals(InstrumentationRegistry.getArguments().getString(LIVE_OPT_IN)));

        Bundle arguments = InstrumentationRegistry.getArguments();
        // Every fact is required; there are no defaults.
        String expectedApkSha256 = required(arguments, "p5e_expected_production_apk_sha256");
        String expectedVersion = required(arguments, "p5e_expected_production_version");
        long expectedVersionCode = requiredLong(arguments, "p5e_expected_production_version_code");
        String expectedCertificateSha256 = required(arguments, "p5e_expected_certificate_sha256");
        String expectedDbSha256 = required(arguments, "p5e_expected_db_sha256");
        String expectedDeviceSignatureToken = required(arguments,
                "p5e_expected_device_signature_token");
        String testApkSha256 = required(arguments, "p5e_test_apk_sha256");
        String testSourceCommit = required(arguments, "p5e_test_source_commit");
        String authorizationId = required(arguments, "p5e_authorization_id");
        String authorizationIdHash = required(arguments, "p5e_authorization_id_hash");
        String ownerManifestHash = required(arguments, "p5e_owner_approval_manifest_sha256");
        String expectedEndpointAccountFingerprint = required(arguments,
                "p5e_expected_endpoint_account_fingerprint");
        long expectedProjectId = requiredLong(arguments, "p5e_expected_project_row_id");
        long issuedAtMillis = requiredLong(arguments, "p5e_authorization_issued_at_ms");
        long expiresAtMillis = requiredLong(arguments, "p5e_authorization_expires_at_ms");
        int expectedSchemaVersion = requiredInt(arguments, "p5e_expected_schema_version");

        requireSha256(expectedApkSha256, "p5e_expected_production_apk_sha256");
        requireSha256(expectedCertificateSha256, "p5e_expected_certificate_sha256");
        requireSha256(expectedDbSha256, "p5e_expected_db_sha256");
        requireSha256(testApkSha256, "p5e_test_apk_sha256");
        requireSha256(authorizationIdHash, "p5e_authorization_id_hash");
        requireSha256(ownerManifestHash, "p5e_owner_approval_manifest_sha256");
        requireSha256(expectedEndpointAccountFingerprint,
                "p5e_expected_endpoint_account_fingerprint");
        assertTrue("p5e_test_source_commit must be a full Git commit",
                testSourceCommit.matches("[0-9a-fA-F]{40}"));
        assertTrue("p5e_expected_production_version_code must be positive",
                expectedVersionCode > 0L);
        assertTrue("p5e_expected_device_signature_token must be 8 hex chars",
                expectedDeviceSignatureToken.matches("(?i)[0-9a-f]{8}"));
        assertEquals(sha256(authorizationId.getBytes(StandardCharsets.UTF_8)),
                authorizationIdHash.toLowerCase(Locale.ROOT));

        requireExact(arguments, "p5e_expected_phase", PHASE);
        requireExact(arguments, "p5e_expected_provider", EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        requireExact(arguments, "p5e_expected_model", EditorialP5EFreshRawRoutingPolicy.MODEL);
        requireExact(arguments, "p5e_expected_upstream_provider",
                EditorialP5EFreshRawRoutingPolicy.UPSTREAM_PROVIDER);
        requireExactSha256(arguments, "p5e_expected_route_fingerprint",
                EditorialP5EFreshRawRoutingPolicy.fingerprint());
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
        requireExact(arguments, "p5e_expected_raw_predecessor_attempt_identity",
                EditorialP5EFreshRawLiveRunner.RAW_PREDECESSOR_ATTEMPT_IDENTITY);

        int maximumPrimaryCalls = requiredInt(arguments, "p5e_maximum_primary_semantic_calls");
        int maximumSchemaRepairs = requiredInt(arguments, "p5e_maximum_schema_repair_calls");
        int maximumNetworkRetries = requiredInt(arguments, "p5e_maximum_network_retries");
        int maximumInputTokens = requiredInt(arguments, "p5e_maximum_input_tokens");
        int maximumOutputTokens = requiredInt(arguments, "p5e_maximum_output_tokens");
        int maximumTotalTokens = requiredInt(arguments, "p5e_maximum_total_tokens");
        BigDecimal maximumCost = requiredDecimal(arguments, "p5e_maximum_total_cost_usd");
        long maximumExecutionMillis = requiredLong(arguments, "p5e_maximum_execution_time_ms");
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

        // Device facts are checked against the arguments before any dispatch.
        Context target = ApplicationProvider.getApplicationContext();
        PackageInfo production = verifyPackage(target, PRODUCTION_PACKAGE, expectedVersionCode,
                expectedApkSha256, expectedCertificateSha256);
        assertEquals(expectedVersion, production.versionName);
        Context instrumentation = InstrumentationRegistry.getInstrumentation().getContext();
        assertEquals(TEST_PACKAGE, instrumentation.getPackageName());
        verifyPackage(instrumentation, TEST_PACKAGE, -1L, testApkSha256, expectedCertificateSha256);
        Path databasePath = target.getDatabasePath("tbl_android_txt.db").toPath();
        assertTrue("current database must exist", Files.isRegularFile(databasePath));
        assertEquals(expectedDbSha256.toLowerCase(Locale.ROOT), sha256File(databasePath));

        // First point at which the credential is read; it is never logged.
        AppSettings settings = SettingsStore.load(target).copy();
        assertTrue(EditorialP5EFreshRawRoutingPolicy.matches(settings));
        assertTrue(settings.apiKey != null && !settings.apiKey.trim().isEmpty());
        String endpointAccountFingerprint = endpointAccountFingerprint(settings);
        assertEquals(expectedEndpointAccountFingerprint, endpointAccountFingerprint);

        try (TranslationRepository database = new TranslationRepository(target)) {
            SQLiteDatabase readable = database.editorialReadableDatabase();
            assertEquals(expectedSchemaVersion, readable.getVersion());
            long projectId = EditorialP5EFreshRawBoundaryInstrumentedTest.projectId(
                    database, EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY);
            assertEquals(expectedProjectId, projectId);
            assertEquals(EditorialP5EFreshRawLiveRunner.ReconcileLineage.READY,
                    EditorialP5EFreshRawLiveRunner.inspectReconcileLineage(readable));

            EditorialP5PilotAuthorization authorization = new EditorialP5PilotAuthorization(
                    authorizationId,
                    EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY,
                    EditorialP5EFreshRawLiveRunner.RUN_DECLARATION_IDENTITY,
                    EditorialP5EFreshRawLiveRunner.CANONICAL_PACK_HASH,
                    EditorialP5EFreshRawLiveRunner.CANONICAL_PROFILE_HASH,
                    EditorialP5EFreshRawLiveRunner.COMPATIBILITY_EVALUATION_ID,
                    EditorialP5EFreshRawLiveRunner.CHAPTER_KEY,
                    PHASE,
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
            // The authorization receipt is persisted/consumed by the production
            // executor, exactly as for the RAW entry; the harness inserts nothing.
            EditorialP5CExactBindingExecution.Result result =
                    new EditorialP5EFreshRawLiveRunner(database,
                            new EditorialPackStorageLayout(target.getFilesDir().toPath()))
                            .dispatchReconcile(projectId, EditorialP5EFreshRawLiveRunner.SELECTOR,
                                    EditorialP5EFreshRawLiveRunner.CHAPTER_KEY, authorization,
                                    settings);

            Bundle status = new Bundle();
            status.putString(STATUS_PREFIX + "status", result.status().name());
            status.putString(STATUS_PREFIX + "reasonCode", result.reasonCode());
            status.putString(STATUS_PREFIX + "providerCalls", Integer.toString(result.providerCalls()));
            status.putString(STATUS_PREFIX + "testSourceCommit", testSourceCommit);
            status.putString(STATUS_PREFIX + "ownerApprovalManifestSha256",
                    ownerManifestHash.toLowerCase(Locale.ROOT));
            InstrumentationRegistry.getInstrumentation().sendStatus(0, status);

            assertTrue("reconcile harness may make at most one provider call",
                    result.providerCalls() <= 1);
            assertFalse(result.executionAllowed());
            assertEquals("NOT_CERTIFIED", result.certificationState());

            long reconcileAttempts = scalar(readable,
                    "SELECT COUNT(*) FROM editorial_p5c_attempts WHERE binding_identity=? AND phase=?",
                    EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY, PHASE);
            assertEquals("exactly one L1_RECONCILE attempt must exist now", 1L, reconcileAttempts);
            if (result.status() == EditorialP5CExactBindingExecution.Status.COMMITTED) {
                assertCommittedReconcile(readable);
            }
        }
    }

    private static void assertCommittedReconcile(SQLiteDatabase db) {
        byte[] reportBytes;
        String predecessorColumn;
        try (Cursor cursor = db.rawQuery("SELECT report_bytes,predecessor_identity "
                + "FROM editorial_p5c_attempts WHERE binding_identity=? AND phase=? "
                + "AND status='COMMITTED'", new String[]{
                EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY, PHASE})) {
            assertEquals(1, cursor.getCount());
            assertTrue(cursor.moveToFirst());
            reportBytes = cursor.getBlob(0);
            predecessorColumn = cursor.getString(1);
        }
        assertNotNull(reportBytes);
        assertTrue(reportBytes.length > 0);
        Map<String, Object> report = EditorialCanonicalJson.parseObject(reportBytes);
        assertEquals(PHASE, report.get("phase"));
        assertEquals(EditorialP5EFreshRawLiveRunner.RAW_PREDECESSOR_ATTEMPT_IDENTITY,
                report.get("predecessorIdentity"));
        assertEquals(EditorialP5EFreshRawLiveRunner.RAW_PREDECESSOR_ATTEMPT_IDENTITY,
                predecessorColumn);
    }

    private static long scalar(SQLiteDatabase db, String sql, String... args) {
        try (Cursor cursor = db.rawQuery(sql, args)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private static String endpointAccountFingerprint(AppSettings settings) {
        String endpoint = AppSettings.normalizeEndpoint(settings.baseUrl);
        return sha256((endpoint + "\n" + settings.apiKey).getBytes(StandardCharsets.UTF_8));
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
            throw new AssertionError("missing required live argument: " + key);
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
