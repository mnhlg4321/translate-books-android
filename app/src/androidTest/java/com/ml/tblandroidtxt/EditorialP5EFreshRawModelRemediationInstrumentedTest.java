package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A4 test-only persisted model correction. It is a distinct opt-in method,
 * never part of the exact-preflight invocation, and never calls SettingsStore
 * save, a database, a provider or the live runner.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5EFreshRawModelRemediationInstrumentedTest {
    private static final String OPT_IN = "p5e_fresh_raw_model_remediation";
    private static final String PRODUCTION_PACKAGE = "com.ml.tblandroidtxt";
    private static final String TEST_PACKAGE = "com.ml.tblandroidtxt.test";
    private static final long EXPECTED_VERSION_CODE = 207L;
    private static final String EXPECTED_PRODUCTION_APK_SHA256 =
            "2ccbb844c629132bb534b0d6aba516055c410bf96d20b14b3f80f91b962800fd";
    private static final String EXPECTED_CERTIFICATE_SHA256 =
            "47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155";
    private static final String STATUS_PREFIX = "p5e.model.v1.";

    @Test
    public void freshRawModelRemediationRunsOnlyWhenExplicitlyOptedIn() throws Exception {
        Assume.assumeTrue("A4 model-remediation opt-in is required",
                "YES".equals(InstrumentationRegistry.getArguments().getString(OPT_IN)));

        Bundle arguments = InstrumentationRegistry.getArguments();
        requireExact(arguments, "p5e_expected_production_package", PRODUCTION_PACKAGE);
        requireExact(arguments, "p5e_expected_production_version_code",
                Long.toString(EXPECTED_VERSION_CODE));
        requireExactSha256(arguments, "p5e_expected_production_apk_sha256",
                EXPECTED_PRODUCTION_APK_SHA256);
        requireExactSha256(arguments, "p5e_expected_certificate_sha256",
                EXPECTED_CERTIFICATE_SHA256);
        String expectedTestApkSha256 = required(arguments, "p5e_test_apk_sha256");
        requireSha256(expectedTestApkSha256, "p5e_test_apk_sha256");

        Context target = ApplicationProvider.getApplicationContext();
        verifyPackage(target, PRODUCTION_PACKAGE, EXPECTED_VERSION_CODE,
                EXPECTED_PRODUCTION_APK_SHA256, EXPECTED_CERTIFICATE_SHA256);
        Context instrumentation = InstrumentationRegistry.getInstrumentation().getContext();
        assertEquals(TEST_PACKAGE, instrumentation.getPackageName());
        verifyPackage(instrumentation, TEST_PACKAGE, -1L, expectedTestApkSha256,
                EXPECTED_CERTIFICATE_SHA256);

        SharedPreferences preferences = target.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Map<String, ?> before = new HashMap<>(preferences.getAll());
        P5EFreshRawModelRemediation.Plan plan = P5EFreshRawModelRemediation.plan(
                before, true, true);
        assertTrue(plan.status == P5EFreshRawModelRemediation.Status.WRITE_REQUIRED
                || plan.status == P5EFreshRawModelRemediation.Status.NO_OP);

        boolean writeAttempted = plan.writeRequired;
        boolean writeCommitted = !writeAttempted;
        if (writeAttempted) {
            // Deliberately write only the persisted model key. SettingsStore.save
            // writes the entire settings object and is forbidden in this path.
            writeCommitted = preferences.edit()
                    .putString(P5EFreshRawModelRemediation.MODEL_KEY,
                            P5EFreshRawModelRemediation.EXPECTED_MODEL)
                    .commit();
        }

        Map<String, ?> after = new HashMap<>(preferences.getAll());
        P5EFreshRawModelRemediation.Result result = P5EFreshRawModelRemediation.complete(
                plan, writeCommitted, after);
        assertTrue(result.success);
        assertEquals(P5EFreshRawModelRemediation.Status.APPLIED, result.status);
        assertEquals(P5EFreshRawModelRemediation.EXPECTED_MODEL,
                after.get(P5EFreshRawModelRemediation.MODEL_KEY));

        AppSettings settings = SettingsStore.load(target);
        assertTrue(EditorialP5EFreshRawRoutingPolicy.matches(settings));
        Bundle status = new Bundle();
        status.putString(STATUS_PREFIX + "modelAlreadyCorrect",
                Boolean.toString(plan.status == P5EFreshRawModelRemediation.Status.NO_OP));
        status.putString(STATUS_PREFIX + "writeAttempted", Boolean.toString(writeAttempted));
        status.putString(STATUS_PREFIX + "writeCommitted", Boolean.toString(writeCommitted));
        status.putString(STATUS_PREFIX + "readbackMatch", "true");
        status.putString(STATUS_PREFIX + "otherSettingsUnchanged",
                Boolean.toString(result.otherSettingsUnchanged));
        status.putString(STATUS_PREFIX + "routeMatch", "true");
        status.putString(STATUS_PREFIX + "providerCalls", "0");
        status.putString(STATUS_PREFIX + "authorizationCreated", "false");
        status.putString(STATUS_PREFIX + "attemptCreated", "false");
        status.putString(STATUS_PREFIX + "reconciliationCreated", "false");
        InstrumentationRegistry.getInstrumentation().sendStatus(0, status);
    }

    @Test
    public void emptyPreferencesPlanWritesOnlyTheModelKey() {
        Map<String, Object> before = new HashMap<>();
        P5EFreshRawModelRemediation.Plan plan = P5EFreshRawModelRemediation.plan(
                before, true, true);
        assertEquals(P5EFreshRawModelRemediation.Status.WRITE_REQUIRED, plan.status);
        Map<String, Object> after = new HashMap<>();
        after.put("model", P5EFreshRawModelRemediation.EXPECTED_MODEL);
        P5EFreshRawModelRemediation.Result result = P5EFreshRawModelRemediation.complete(
                plan, true, after);
        assertTrue(result.success);
        assertTrue(result.writeAttempted);
    }

    @Test
    public void wrongModelRequiresWriteAndPreservesOtherKeys() {
        Map<String, Object> before = new HashMap<>();
        before.put("model", "anthropic/claude-sonnet-4.6");
        before.put("timeoutSeconds", 300);
        P5EFreshRawModelRemediation.Plan plan = P5EFreshRawModelRemediation.plan(
                before, true, true);
        Map<String, Object> after = new HashMap<>(before);
        after.put("model", P5EFreshRawModelRemediation.EXPECTED_MODEL);
        P5EFreshRawModelRemediation.Result result = P5EFreshRawModelRemediation.complete(
                plan, true, after);
        assertTrue(result.success);
        assertTrue(result.otherSettingsUnchanged);
    }

    @Test
    public void alreadyCorrectModelIsNoOp() {
        Map<String, Object> values = new HashMap<>();
        values.put("model", P5EFreshRawModelRemediation.EXPECTED_MODEL);
        P5EFreshRawModelRemediation.Plan plan = P5EFreshRawModelRemediation.plan(
                values, true, true);
        assertEquals(P5EFreshRawModelRemediation.Status.NO_OP, plan.status);
        P5EFreshRawModelRemediation.Result result = P5EFreshRawModelRemediation.complete(
                plan, true, values);
        assertTrue(result.success);
        assertFalse(result.writeAttempted);
    }

    @Test
    public void missingOptInFailsClosedBeforeAnyWrite() {
        P5EFreshRawModelRemediation.Plan plan = P5EFreshRawModelRemediation.plan(
                new HashMap<>(), false, true);
        assertEquals(P5EFreshRawModelRemediation.Status.OPT_IN_REQUIRED, plan.status);
        assertFalse(plan.writeRequired);
    }

    @Test
    public void identityMismatchFailsClosedBeforeAnyWrite() {
        P5EFreshRawModelRemediation.Plan plan = P5EFreshRawModelRemediation.plan(
                new HashMap<>(), true, false);
        assertEquals(P5EFreshRawModelRemediation.Status.IDENTITY_MISMATCH, plan.status);
        assertFalse(plan.writeRequired);
    }

    @Test
    public void writeFailureDoesNotBecomeSuccess() {
        Map<String, Object> before = new HashMap<>();
        before.put("model", "old-model");
        P5EFreshRawModelRemediation.Plan plan = P5EFreshRawModelRemediation.plan(
                before, true, true);
        P5EFreshRawModelRemediation.Result result = P5EFreshRawModelRemediation.complete(
                plan, false, before);
        assertEquals(P5EFreshRawModelRemediation.Status.WRITE_FAILED, result.status);
        assertFalse(result.success);
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
        assertTrue(applicationInfo != null && applicationInfo.sourceDir != null);
        assertEquals(expectedApkSha256.toLowerCase(Locale.ROOT),
                sha256File(Paths.get(applicationInfo.sourceDir)));
        assertEquals(expectedCertificateSha256.toLowerCase(Locale.ROOT),
                certificateSha256(info));
        return info;
    }

    private static String certificateSha256(PackageInfo info) throws Exception {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= 28) {
            SigningInfo signingInfo = info.signingInfo;
            assertTrue(signingInfo != null);
            signatures = signingInfo.hasMultipleSigners()
                    ? signingInfo.getApkContentsSigners()
                    : signingInfo.getSigningCertificateHistory();
        } else {
            signatures = info.signatures;
        }
        assertTrue(signatures != null && signatures.length > 0);
        return sha256(signatures[0].toByteArray());
    }

    private static String sha256File(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[16_384];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return hex(digest.digest());
    }

    private static String sha256(byte[] bytes) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return result.toString();
    }

    private static String required(Bundle arguments, String key) {
        String value = arguments == null ? null : arguments.getString(key);
        if (value == null || value.trim().isEmpty()) {
            throw new AssertionError("missing required A4 argument: " + key);
        }
        return value.trim();
    }

    private static void requireExact(Bundle arguments, String key, String expected) {
        assertEquals(key, expected, required(arguments, key));
    }

    private static void requireExactSha256(Bundle arguments, String key, String expected) {
        String value = required(arguments, key);
        requireSha256(value, key);
        assertEquals(expected.toLowerCase(Locale.ROOT), value.toLowerCase(Locale.ROOT));
    }

    private static void requireSha256(String value, String key) {
        assertTrue(key + " must be full SHA-256", value.matches("(?i)[0-9a-f]{64}"));
    }
}
