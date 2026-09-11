package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4SourceIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Explicit P5D device path for the separately authorized VOL5/001 RAW call.
 * Source bytes are staged into the debuggable validation package by the host;
 * this test never logs source text, a request body or a provider response.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5DVol5RawPilotInstrumentedTest {
    private static final String CANONICAL_ASSET =
            "editorial-p2/v5-safe-4.1.3-full-canonical.zip";
    private static final String PACK_ID = "com.ml.tblandroidtxt.editorial.safe4.full";
    private static final String PACK_VERSION = "4.1.3";
    private static final String SELECTOR = "p5d-raw-mercedes-vol5-001";
    private static final String CHAPTER_KEY = "001";
    private static final String PROVIDER = "openrouter";
    private static final String MODEL = "openai/gpt-5.6-luna";
    private static final String ORIGINAL_ATTEMPT_ID =
            "157e3517b4b98535392db95f0c93285f08a0ac6b07341a82508ffc36aea9a5f0";
    private static final String ORIGINAL_AUTHORIZATION_HASH =
            "f39ff4fd7503aa6746fc2f61f4ea75a06ec5989f187da240334822587eeaa212";
    private static final String DIAGNOSTIC_AUTHORIZATION_ID =
            "P5D-VOL5-RAW-DIAGNOSTIC-20260909-01";
    private static final String DIAGNOSTIC_AUTHORIZATION_HASH =
            "a28d70c9f1e9b33160daa6d1614abae98f5a921621fbea49f92279f06bcdc00d";
    private static final String ACCEPTANCE_AUTHORIZATION_ID =
            "P5D-VOL5-RAW-ACCEPTANCE-20260909-01";
    private static final String ACCEPTANCE_AUTHORIZATION_HASH =
            "d116a03f995480c19187ea6531dbf4fc4e91690178f13841854297b62cf01693";
    private static final String TRUNCATED_GENERATION_ID =
            "gen-1788959113-A9RinufLgb63vTqkAEwE";
    private static final int RAW_ACCEPTANCE_OUTPUT_TOKENS = 4_096;
    // Retained only to describe the already-consumed historical diagnostic;
    // it must never be reused for a new acceptance authorization.
    private static final int HISTORICAL_DIAGNOSTIC_OUTPUT_TOKENS = 2_048;
    private static final long PILOT_WINDOW_MILLIS = 5 * 60 * 1000L;

    /**
     * Test-only guard: this class is historical P5D wiring and cannot accept
     * a fresh-pilot selector. The fresh boundary has its own runner.
     */
    static String requireHistoricalSelector(String selector) {
        if (!SELECTOR.equals(selector)) {
            throw new IllegalArgumentException("P5D_HISTORICAL_RUNNER_SELECTOR_MISMATCH");
        }
        return selector;
    }

    @Test public void v23UpgradePreservesVol5RecoveryIdentityAndHistory() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        java.io.File databaseFile = context.getDatabasePath("tbl_android_txt.db");
        assertTrue("VOL5 pilot database must already exist", databaseFile.isFile());

        String beforeStatus;
        String beforeReason;
        int beforeAuthCount;
        String beforeReconciliation;
        SQLiteDatabase before = SQLiteDatabase.openDatabase(databaseFile.getPath(), null,
                SQLiteDatabase.OPEN_READONLY);
        try {
            Assume.assumeTrue("v22/v23 migration assertion is historical; current schema is v24",
                    before.getVersion() == 22 || before.getVersion() == 23);
            beforeStatus = scalar(before, "SELECT status FROM editorial_p5c_attempts WHERE attempt_identity=?",
                    ORIGINAL_ATTEMPT_ID);
            beforeReason = scalar(before,
                    "SELECT recovery_reason_code FROM editorial_p5c_attempts WHERE attempt_identity=?",
                    ORIGINAL_ATTEMPT_ID);
            beforeAuthCount = count(before,
                    "SELECT COUNT(*) FROM editorial_p5d_authorization_receipts WHERE attempt_identity=?",
                    ORIGINAL_ATTEMPT_ID);
            beforeReconciliation = scalar(before,
                    "SELECT classification FROM editorial_p5d_reconciliation WHERE attempt_identity=?",
                    ORIGINAL_ATTEMPT_ID);
        } finally {
            before.close();
        }

        try (TranslationRepository upgraded = new TranslationRepository(context)) {
            SQLiteDatabase after = upgraded.editorialReadableDatabase();
            assertEquals(23, after.getVersion());
            assertEquals("RECOVERY_REQUIRED", scalar(after,
                    "SELECT status FROM editorial_p5c_attempts WHERE attempt_identity=?",
                    ORIGINAL_ATTEMPT_ID));
            assertEquals(beforeStatus, scalar(after,
                    "SELECT status FROM editorial_p5c_attempts WHERE attempt_identity=?",
                    ORIGINAL_ATTEMPT_ID));
            assertEquals(beforeReason, scalar(after,
                    "SELECT recovery_reason_code FROM editorial_p5c_attempts WHERE attempt_identity=?",
                    ORIGINAL_ATTEMPT_ID));
            assertEquals(beforeAuthCount, count(after,
                    "SELECT COUNT(*) FROM editorial_p5d_authorization_receipts WHERE attempt_identity=?",
                    ORIGINAL_ATTEMPT_ID));
            assertEquals(beforeReconciliation, scalar(after,
                    "SELECT classification FROM editorial_p5d_reconciliation WHERE attempt_identity=?",
                    ORIGINAL_ATTEMPT_ID));
            assertEquals(0, count(after,
                    "SELECT COUNT(*) FROM editorial_p5d_reconciliation_history WHERE attempt_identity=?",
                    ORIGINAL_ATTEMPT_ID));
        }
    }

    @Test public void p5eCode198ReconstructedDbSchemaAndVol5DataReadback() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        java.io.File databaseFile = context.getDatabasePath("tbl_android_txt.db");
        assertTrue("VOL5 pilot database must already exist", databaseFile.isFile());
        String rawHash;
        int rawBytes;
        int assetCount;
        SQLiteDatabase before = SQLiteDatabase.openDatabase(databaseFile.getPath(), null,
                SQLiteDatabase.OPEN_READONLY);
        try {
            assertEquals("P5E current schema must remain v24", 24, before.getVersion());
            rawHash = scalar(before, "SELECT sha256 FROM editorial_assets WHERE source_uri=?",
                    "p5d-real://mercedes-vol5/001/raw");
            rawBytes = intScalar(before, "SELECT size_bytes FROM editorial_assets WHERE source_uri=?",
                    "p5d-real://mercedes-vol5/001/raw");
            assetCount = count(before, "SELECT COUNT(*) FROM editorial_assets WHERE source_uri LIKE ?",
                    "p5d-real://mercedes-vol5/001/%");
        } finally {
            before.close();
        }

        try (TranslationRepository after = new TranslationRepository(context)) {
            SQLiteDatabase db = after.editorialReadableDatabase();
            assertEquals(24, db.getVersion());
            assertEquals(rawHash, scalar(db, "SELECT sha256 FROM editorial_assets WHERE source_uri=?",
                    "p5d-real://mercedes-vol5/001/raw"));
            assertEquals(rawBytes, intScalar(db,
                    "SELECT size_bytes FROM editorial_assets WHERE source_uri=?",
                    "p5d-real://mercedes-vol5/001/raw"));
            assertEquals(assetCount, count(db,
                    "SELECT COUNT(*) FROM editorial_assets WHERE source_uri LIKE ?",
                    "p5d-real://mercedes-vol5/001/%"));
        }
    }

    @Test public void readBackVol5AcceptanceAfterDeadlineCleanup() {
        Context context = ApplicationProvider.getApplicationContext();
        try (TranslationRepository database = new TranslationRepository(context)) {
            EditorialP5CAttemptStore attemptStore = new EditorialP5CAttemptStore(database);
            EditorialP5CAttemptStore.AttemptRecord attempt = attemptStore
                    .findRecord(ORIGINAL_ATTEMPT_ID).orElseThrow(
                            () -> new AssertionError("VOL5 attempt is missing after deadline cleanup"));
            Assume.assumeTrue("P5D deadline readback is historical after P5E acceptance",
                    "RECOVERY_REQUIRED".equals(attempt.status()));
            java.util.Optional<EditorialP5CAttemptStore.AuthorizationReceipt> authorization =
                    attemptStore.findAuthorizationReceipt(ACCEPTANCE_AUTHORIZATION_HASH);
            java.util.Optional<EditorialP5CAttemptStore.NetworkLifecycleEvent> lifecycle =
                    attemptStore.findNetworkLifecycle(ORIGINAL_ATTEMPT_ID);
            List<EditorialP5CAttemptStore.ReconciliationRecord> history = attemptStore
                    .findReconciliationHistory(ORIGINAL_ATTEMPT_ID);
            Log.i("P5D_RAW_READBACK", "status=" + attempt.status()
                    + " reason=" + attempt.recoveryReasonCode()
                    + " responseIdentityPresent=" + (attempt.responseIdentity() != null
                    && !attempt.responseIdentity().isBlank())
                    + " reportBytes=" + attempt.reportBytes().length
                    + " receiptBytes=" + attempt.receiptBytes().length
                    + " acceptanceAuthorizationPresent=" + authorization.isPresent()
                    + " acceptanceConsumption=" + (authorization.isPresent()
                    ? authorization.get().consumptionResult() : "ABSENT")
                    + " lifecyclePresent=" + lifecycle.isPresent()
                    + " lifecycleStage=" + (lifecycle.isPresent()
                    ? lifecycle.get().stage() : "ABSENT")
                    + " requestBodyBytes=" + (lifecycle.isPresent()
                    ? lifecycle.get().requestBodyBytes() : 0)
                    + " httpStatus=" + (lifecycle.isPresent()
                    ? lifecycle.get().httpStatus() : -1)
                    + " elapsedMs=" + (lifecycle.isPresent()
                    ? lifecycle.get().elapsedMillis() : 0)
                    + " contentType=" + (lifecycle.isPresent()
                    ? lifecycle.get().responseContentType() : "")
                    + " generationPresent=" + (lifecycle.isPresent()
                    && !lifecycle.get().generationId().isBlank())
                    + " generationId=" + (lifecycle.isPresent()
                    ? lifecycle.get().generationId() : "")
                    + " responseIdPresent=" + (lifecycle.isPresent()
                    && !lifecycle.get().providerResponseId().isBlank())
                    + " cancellationSource=" + (lifecycle.isPresent()
                    ? lifecycle.get().cancellationSource() : "")
                    + " recoveryHistoryCount=" + history.size());
            assertTrue("acceptance readback must not contain a partial report",
                    attempt.reportBytes().length == 0 || attempt.receiptBytes().length > 0);
            assertTrue("acceptance readback must not contain a partial receipt",
                    attempt.receiptBytes().length == 0 || attempt.reportBytes().length > 0);
        }
    }

    @Test public void closeInterruptedVol5AcceptanceAsTypedRecovery() {
        Context context = ApplicationProvider.getApplicationContext();
        try (TranslationRepository database = new TranslationRepository(context)) {
            EditorialP5CAttemptStore attemptStore = new EditorialP5CAttemptStore(database);
            EditorialP5CAttemptStore.AttemptRecord before = attemptStore
                    .findRecord(ORIGINAL_ATTEMPT_ID).orElseThrow(
                            () -> new AssertionError("VOL5 attempt is missing before recovery closure"));
            Assume.assumeTrue("P5D claimed-row closure is historical after P5E acceptance",
                    "CLAIMED".equals(before.status()));
            assertEquals("CLAIMED", before.status());
            attemptStore.markRecoveryRequired(ORIGINAL_ATTEMPT_ID,
                    "RETRY_PROVIDER_CALL_TIMEOUT");
            EditorialP5CAttemptStore.AttemptRecord after = attemptStore
                    .findRecord(ORIGINAL_ATTEMPT_ID).orElseThrow();
            assertEquals("RECOVERY_REQUIRED", after.status());
            assertEquals("RETRY_PROVIDER_CALL_TIMEOUT", after.recoveryReasonCode());
            assertEquals(0, after.reportBytes().length);
            assertEquals(0, after.receiptBytes().length);
            Log.i("P5D_RAW_RECOVERY", "status=" + after.status()
                    + " reason=" + after.recoveryReasonCode()
                    + " reportBytes=" + after.reportBytes().length
                    + " receiptBytes=" + after.receiptBytes().length
                    + " providerCallsAfterCleanup=0");
        }
    }

    @Test public void preparePersistedVol5Chapter001Binding() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        try (TranslationRepository database = new TranslationRepository(context)) {
            EditorialPackStorageLayout storage = new EditorialPackStorageLayout(
                    context.getFilesDir().toPath());
            EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                    .resolve(PACK_ID, PACK_VERSION).orElse(null);
            if (candidate == null) {
                byte[] packBytes;
                try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                        .getAssets().open(CANONICAL_ASSET)) {
                    packBytes = input.readAllBytes();
                }
                EditorialPackImportResult imported = new EditorialPackImportService(database, storage,
                        new EditorialEngineProfileResolver(
                                BundledEditorialEngineContractProfileRegistry.load()))
                        .importZip(new ByteArrayInputStream(packBytes));
                assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION,
                        imported.state());
                candidate = new EditorialPackSelectionPolicy(database, storage)
                        .resolve(PACK_ID, PACK_VERSION).orElse(null);
            }
            Assume.assumeTrue("canonical 4.1.3 pack is not imported in validation package",
                    candidate != null);
            if (candidate == null) return;
            List<EditorialP4InputSource> sources = readSources(context);
            EditorialP4SetupRequest request = new EditorialP4SetupRequest(
                    SELECTOR, "MERCEDES", "VOL 5", candidate.packId(), candidate.packVersion(),
                    "p5d/mercedes/vol5", "p5d/mercedes/vol5/001", sources,
                    EditorialSafe4Contract.NORMAL_MODE, "AVAILABLE", "AVAILABLE", "NONE",
                    "USER_CONFIRMED_NORMAL_P5D_RAW_VOL5", "EDITORIAL_PILOT",
                    "L1_SOURCE_PREFLIGHT", "manifest-attestation-v1", EditorialLineageNodeKind.ROOT,
                    null, System.currentTimeMillis());
            EditorialP4BindingResult result = new EditorialP4BindingTransactionService(database, storage)
                    .createSetup(request);
            assertTrue("VOL5 P4 setup was not accepted: " + result.detail(), result.accepted());
            assertNotNull(result.binding());
            long projectId = result.projectId();
            if (listChapterIds(database, projectId).isEmpty()) insertChapter(database, projectId, sources);

            EditorialP4ResumeResult resumed = new EditorialP4BindingTransactionService(database, storage)
                    .resumeProject(projectId, SELECTOR, sources);
            assertEquals(EditorialP4ResumeResult.Code.RESTORED, resumed.code());
            assertEquals(result.binding().bindingIdentity(), resumed.binding().bindingIdentity());
            logBinding(result.binding(), projectId, sources);
        }
    }

    @Test public void authorizedVol5RawRunsOnlyWhenExplicitlyOptedIn() throws Exception {
        Assume.assumeTrue("P5D acceptance is historical and permanently disabled", false);
        String optIn = InstrumentationRegistry.getArguments().getString("p5d_raw_live", "");
        Assume.assumeTrue("new VOL5 RAW call is explicit opt-in", "YES".equalsIgnoreCase(optIn));

        Context context = ApplicationProvider.getApplicationContext();
        AppSettings settings = SettingsStore.load(context);
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: provider must be OpenRouter",
                PROVIDER.equalsIgnoreCase(nullToEmpty(settings.provider)));
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: OpenRouter endpoint required",
                AppSettings.defaultBaseUrl(PROVIDER).equals(AppSettings.normalizeEndpoint(settings.baseUrl)));
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: OpenRouter API key is absent after validation restore",
                settings.apiKey != null && !settings.apiKey.trim().isEmpty());

        settings.provider = PROVIDER;
        settings.baseUrl = AppSettings.defaultBaseUrl(PROVIDER);
        settings.model = MODEL;
        settings.timeoutSeconds = 300;

        try (TranslationRepository database = new TranslationRepository(context)) {
            EditorialP4Binding binding = new EditorialP4BindingDao(database)
                    .findByAttemptRequestSelector(SELECTOR).orElseThrow(
                            () -> new AssertionError("VOL5 P4 binding is not persisted"));
            long projectId = projectId(database, binding.bindingIdentity());
            assertTrue("VOL5 persisted project id is required", projectId > 0);
            String endpointAccountFingerprint = EditorialCanonicalJson.sha256Hex(
                    (settings.baseUrl + "|" + EditorialCanonicalJson.sha256Hex(
                            settings.apiKey.getBytes(StandardCharsets.UTF_8)))
                            .getBytes(StandardCharsets.UTF_8));
            long issuedAt = System.currentTimeMillis();
            long expiresAt = issuedAt + PILOT_WINDOW_MILLIS;
            EditorialP5PilotAuthorization raw = new EditorialP5PilotAuthorization(
                    ACCEPTANCE_AUTHORIZATION_ID,
                    binding.bindingIdentity(), binding.runDeclarationIdentity(),
                    binding.canonicalPackHash(), binding.canonicalProfileHash(),
                    binding.compatibilityEvaluationId(), CHAPTER_KEY, "L1_RAW_DISCOVERY",
                    PROVIDER, MODEL, endpointAccountFingerprint,
                    1, 0, 0, 100_000, RAW_ACCEPTANCE_OUTPUT_TOKENS, 100_000,
                    BigDecimal.valueOf(0.10),
                    PILOT_WINDOW_MILLIS, true, false, false, "HASH_ONLY",
                    "USER_AUTHORIZED_NEW_RAW_VOL5;RECONCILE_NOT_AUTHORIZED",
                    issuedAt, expiresAt, true);
            assertEquals(ACCEPTANCE_AUTHORIZATION_HASH,
                    EditorialP5CAttemptStore.authorizationIdHash(ACCEPTANCE_AUTHORIZATION_ID));

            EditorialP5CAttemptStore attemptStore = new EditorialP5CAttemptStore(database);
            EditorialP5CAttemptStore.ReconciliationRecord primary = attemptStore
                    .findReconciliation(ORIGINAL_ATTEMPT_ID).orElseThrow(
                            () -> new AssertionError("VOL5 primary cancellation decision is missing"));
            assertEquals(EditorialP5CAttemptStore.ExternalStateClassification
                    .EXTERNAL_CONFIRMED_CANCELLED, primary.classification());
            EditorialP5CAttemptStore.ReconciliationDecision recoveryDecision =
                    new EditorialP5CAttemptStore.ReconciliationDecision(
                            ORIGINAL_ATTEMPT_ID,
                            EditorialP5CAttemptStore.ExternalStateClassification
                                    .EXTERNAL_COMPLETED_BILLED_OUTPUT_UNAVAILABLE,
                            "docs/P5D_RAW_DIAGNOSTIC_ATTEMPT_REPORT.md",
                            endpointAccountFingerprint,
                            "TRUNCATED_OUTPUT_COST_0.0075392_" + TRUNCATED_GENERATION_ID,
                            "P5D_USER_AUTHORIZED_RAW_ACCEPTANCE", true, true,
                            ACCEPTANCE_AUTHORIZATION_HASH, System.currentTimeMillis());
            // The first cancelled decision remains immutable in the primary
            // row; this later decision is appended to v23 history.
            attemptStore.recordRecoveryDecision(recoveryDecision);
            assertTrue("new recovery decision must be readable before dispatch",
                    attemptStore.findReconciliationHistory(ORIGINAL_ATTEMPT_ID).stream()
                            .anyMatch(value -> ACCEPTANCE_AUTHORIZATION_HASH
                                    .equals(value.newAuthorizationIdHash())));

            Log.i("P5D_RAW_PREFLIGHT", "selector=" + SELECTOR
                    + " projectId=" + projectId
                    + " bindingIdentity=" + binding.bindingIdentity()
                    + " runDeclarationIdentity=" + binding.runDeclarationIdentity()
                    + " compatibilityEvaluationId=" + binding.compatibilityEvaluationId()
                    + " canonicalPackHash=" + binding.canonicalPackHash()
                    + " canonicalProfileHash=" + binding.canonicalProfileHash()
                    + " provider=" + PROVIDER + " model=" + MODEL
                    + " endpointAccountFingerprint=" + endpointAccountFingerprint
                    + " primaryCalls=1 schemaRepairCalls=0 networkRetries=0"
                    + " maxTotalCost=0.10 maxExecutionTimeMs=" + PILOT_WINDOW_MILLIS);
            for (EditorialP4SourceIdentity input : binding.inputs()) {
                Log.i("P5D_RAW_PREFLIGHT", "source role=" + input.role()
                        + " length=" + input.byteLength() + " sha256=" + input.sha256());
            }

            OpenRouterEditorialP5PilotProvider provider =
                    OpenRouterEditorialP5PilotProvider.withLifecyclePersistence(
                     settings, RAW_ACCEPTANCE_OUTPUT_TOKENS, database);
            assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: provider configuration is incomplete",
                    provider.configured());
            EditorialP5CExactBindingExecution.Result result =
                    new EditorialP5CExactBindingExecution(database,
                            new EditorialPackStorageLayout(context.getFilesDir().toPath()))
                            .executeRaw(projectId, SELECTOR, CHAPTER_KEY, raw, provider);
            logResult(result);
            assertNotNull(result);
            assertTrue("RAW result exceeded the authorized primary call",
                    result.providerCalls() <= 1);
            org.junit.Assert.assertNull("RECONCILE must not be called by RAW-only authorization",
                    result.reconcileResult());
            assertTrue("execution must remain disabled", !result.executionAllowed());
            assertEquals("NOT_CERTIFIED", result.certificationState());
            if (result.accepted()) {
                assertNotNull(result.rawResult());
                assertTrue(result.rawResult().outcome() == EditorialP5PilotResult.Outcome.COMMITTED
                        || result.rawResult().outcome() == EditorialP5PilotResult.Outcome.ALREADY_COMMITTED);
            }
        }
    }

    @Test public void p5eAuthorizedVol5RawRunsOnlyWhenExplicitlyOptedIn() throws Exception {
        String optIn = InstrumentationRegistry.getArguments().getString("p5e_raw_live", "");
        Assume.assumeTrue("P5E VOL5 RAW acceptance is explicit opt-in",
                "YES".equalsIgnoreCase(optIn));

        Context context = ApplicationProvider.getApplicationContext();
        AppSettings settings = SettingsStore.load(context);
        assertTrue("P5E_LIVE_AUTHORIZATION_INCOMPLETE: provider must be OpenRouter",
                PROVIDER.equalsIgnoreCase(nullToEmpty(settings.provider)));
        assertTrue("P5E_LIVE_AUTHORIZATION_INCOMPLETE: OpenRouter endpoint required",
                AppSettings.defaultBaseUrl(PROVIDER).equals(AppSettings.normalizeEndpoint(settings.baseUrl)));
        assertTrue("P5E_LIVE_AUTHORIZATION_INCOMPLETE: OpenRouter API key is absent",
                settings.apiKey != null && !settings.apiKey.trim().isEmpty());
        settings.provider = PROVIDER;
        settings.baseUrl = AppSettings.defaultBaseUrl(PROVIDER);
        settings.model = MODEL;
        settings.timeoutSeconds = 300;

        final String authorizationId = "P5E-VOL5-RAW-ACCEPTANCE-20260910-01";
        try (TranslationRepository database = new TranslationRepository(context)) {
            EditorialP4Binding binding = new EditorialP4BindingDao(database)
                    .findByAttemptRequestSelector(SELECTOR).orElseThrow(
                            () -> new AssertionError("VOL5 P4 binding is not persisted"));
            long projectId = projectId(database, binding.bindingIdentity());
            assertTrue("VOL5 persisted project id is required", projectId > 0);

            EditorialP5CAttemptStore attemptStore = new EditorialP5CAttemptStore(database);
            EditorialP5CAttemptStore.AttemptRecord prior = attemptStore
                    .findRecord(ORIGINAL_ATTEMPT_ID).orElseThrow(
                            () -> new AssertionError("VOL5 recovery attempt is missing"));
            assertEquals("RECOVERY_REQUIRED", prior.status());
            EditorialP5CAttemptStore.ReconciliationRecord primary = attemptStore
                    .findReconciliation(ORIGINAL_ATTEMPT_ID).orElseThrow(
                            () -> new AssertionError("VOL5 primary reconciliation is missing"));
            assertEquals(EditorialP5CAttemptStore.ExternalStateClassification
                    .EXTERNAL_CONFIRMED_CANCELLED, primary.classification());

            String endpointAccountFingerprint = EditorialCanonicalJson.sha256Hex(
                    (settings.baseUrl + "|" + EditorialCanonicalJson.sha256Hex(
                            settings.apiKey.getBytes(StandardCharsets.UTF_8)))
                            .getBytes(StandardCharsets.UTF_8));
            String authorizationHash = EditorialP5CAttemptStore.authorizationIdHash(authorizationId);

            // Reconcile the exact old generation before constructing or
            // dispatching the new single-use authorization. The decision is
            // append-only and names the new authorization hash without
            // storing request/response/source bodies.
            EditorialP5CAttemptStore.ReconciliationDecision recoveryDecision =
                    new EditorialP5CAttemptStore.ReconciliationDecision(
                            ORIGINAL_ATTEMPT_ID,
                            EditorialP5CAttemptStore.ExternalStateClassification
                                    .EXTERNAL_CONFIRMED_CANCELLED,
                            "docs/P5E_RECONCILIATION_RECORD.md",
                            endpointAccountFingerprint,
                            "GENERATION_CANCELLED_INPUT_23674_OUTPUT_0_REASONING_0_"
                                    + "UPSTREAM_USAGE_0.0047348_BILLING_FLAG_ABSENT",
                            "P5E_EXTERNAL_METADATA_RECONCILIATION", true, true,
                            authorizationHash, System.currentTimeMillis());
            attemptStore.recordRecoveryDecision(recoveryDecision);
            assertTrue("P5E recovery decision must be readable before authorization dispatch",
                    attemptStore.findReconciliationHistory(ORIGINAL_ATTEMPT_ID).stream()
                            .anyMatch(value -> authorizationHash.equals(value.newAuthorizationIdHash())));

            long issuedAt = System.currentTimeMillis();
            long expiresAt = issuedAt + PILOT_WINDOW_MILLIS;
            EditorialP5PilotAuthorization raw = new EditorialP5PilotAuthorization(
                    authorizationId, binding.bindingIdentity(), binding.runDeclarationIdentity(),
                    binding.canonicalPackHash(), binding.canonicalProfileHash(),
                    binding.compatibilityEvaluationId(), CHAPTER_KEY, "L1_RAW_DISCOVERY",
                    PROVIDER, MODEL, endpointAccountFingerprint,
                    1, 0, 0, 100_000, EditorialP5RawWireContract.OUTPUT_TOKEN_CAP, 100_000,
                    BigDecimal.valueOf(0.10), PILOT_WINDOW_MILLIS, true, false, false,
                    "HASH_ONLY",
                    "P5E_COMPACT_RAW_WIRE_V1;REASONING_MINIMAL;RAW_ONLY;"
                            + "NO_SCHEMA_REPAIR;NO_AUTOMATIC_RETRY;RECONCILE_NOT_AUTHORIZED",
                    issuedAt, expiresAt, true);
            assertEquals(authorizationHash,
                    EditorialP5CAttemptStore.authorizationIdHash(raw.authorizationId()));

            Log.i("P5E_RAW_PREFLIGHT", "oldGeneration=" + "gen-1788967700-RgJDCWrZsNZ4VAAWmlj8"
                    + " externalClassification=EXTERNAL_CONFIRMED_CANCELLED"
                    + " priorKnownCosts=gen-1788910936-DHfTNOyDlU3f3PJOAvqb:0.00484;"
                    + "gen-1788959113-A9RinufLgb63vTqkAEwE:0.0075392;"
                    + "gen-1788967700-RgJDCWrZsNZ4VAAWmlj8:ACCOUNT_USAGE_ZERO_UPSTREAM_USAGE_NONZERO"
                    + " binding=" + binding.bindingIdentity()
                    + " sourceWireSchema=" + EditorialP5RawWireContract.SCHEMA_VERSION
                    + " outputCap=" + EditorialP5RawWireContract.OUTPUT_TOKEN_CAP
                    + " reasoningPolicy=" + EditorialP5RawWireContract.REASONING_POLICY
                    + " primaryCalls=1 schemaRepairCalls=0 networkRetries=0"
                    + " maximumCost=0.10 maximumExecutionMs=" + PILOT_WINDOW_MILLIS
                    + " rawOnly=true reconcileAuthorization=false");
            logBinding(binding, projectId, readSources(context));

            OpenRouterEditorialP5PilotProvider provider =
                    OpenRouterEditorialP5PilotProvider.withLifecyclePersistence(
                            settings, EditorialP5RawWireContract.OUTPUT_TOKEN_CAP, database);
            assertTrue("P5E_LIVE_AUTHORIZATION_INCOMPLETE: provider configuration is incomplete",
                    provider.configured());
            keepTargetForegroundForDiagnostic();
            EditorialP5CExactBindingExecution.Result result =
                    new EditorialP5CExactBindingExecution(database,
                            new EditorialPackStorageLayout(context.getFilesDir().toPath()))
                            .executeRaw(projectId, SELECTOR, CHAPTER_KEY, raw, provider);
            logP5eResult(result);
            assertNotNull(result);
            assertTrue("P5E RAW result exceeded one primary call", result.providerCalls() <= 1);
            org.junit.Assert.assertNull("P5E RAW-only acceptance must not call RECONCILE",
                    result.reconcileResult());
            assertTrue("execution must remain disabled", !result.executionAllowed());
            assertEquals("NOT_CERTIFIED", result.certificationState());
            assertEquals(EditorialP5CExactBindingExecution.Status.COMMITTED, result.status());
            assertNotNull(result.rawResult());
            assertEquals(EditorialP5PilotResult.Outcome.COMMITTED,
                    result.rawResult().outcome());
            assertTrue(result.rawResult().metrics().schemaValidationPassed());
            assertTrue(result.rawResult().metrics().receiptValidationPassed());
            assertTrue(result.rawResult().metrics().costAccountingComplete());
            assertEquals(0, result.rawResult().committedResult().output().declaredChanges().size());

            byte[] exactRaw = readSources(context).stream()
                    .filter(value -> EditorialSafe4Contract.RAW.equals(value.role()))
                    .findFirst().orElseThrow().bytes();
            assertTrue("app-owned RAW beforeText must be exact pinned bytes",
                    Arrays.equals(exactRaw, result.rawResult().committedResult().output()
                            .beforeText().getBytes(StandardCharsets.UTF_8)));
            assertEquals(result.rawResult().committedResult().output().beforeText(),
                    result.rawResult().committedResult().output().afterText());

            EditorialP5CAttemptStore.AttemptRecord after = attemptStore
                    .findRecord(ORIGINAL_ATTEMPT_ID).orElseThrow();
            assertEquals("COMMITTED", after.status());
            assertTrue("RAW predecessor report must be readable after commit",
                    after.reportBytes().length > 0);
            assertTrue("RAW predecessor receipt must be readable after commit",
                    after.receiptBytes().length > 0);
            assertEquals("CONSUMED", attemptStore.findAuthorizationReceipt(authorizationHash)
                    .orElseThrow().consumptionResult());
            Log.i("P5E_RAW_ACCEPTANCE", "P5E_LIVE_RAW_ACCEPTANCE_PASS"
                    + " RAW_PREDECESSOR_COMMITTED RAW_PREDECESSOR_READBACK_PASS"
                    + " READY_FOR_P5F_CONTROLLED_RECONCILE RECONCILE_AUTHORIZATION_REQUIRED"
                    + " EXECUTION_DISABLED NOT_CERTIFIED");
        }
    }

    @Test public void authorizedVol5RawDiagnosticRecoveryRunsOnlyWhenExplicitlyOptedIn()
            throws Exception {
        String optIn = InstrumentationRegistry.getArguments().getString("p5d_raw_diagnostic", "");
        Assume.assumeTrue("VOL5 RAW diagnostic call is explicit opt-in",
                "YES".equalsIgnoreCase(optIn));

        Context context = ApplicationProvider.getApplicationContext();
        AppSettings settings = SettingsStore.load(context);
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: provider must be OpenRouter",
                PROVIDER.equalsIgnoreCase(nullToEmpty(settings.provider)));
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: OpenRouter endpoint required",
                AppSettings.defaultBaseUrl(PROVIDER).equals(AppSettings.normalizeEndpoint(settings.baseUrl)));
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: OpenRouter API key is absent",
                settings.apiKey != null && !settings.apiKey.trim().isEmpty());
        settings.provider = PROVIDER;
        settings.baseUrl = AppSettings.defaultBaseUrl(PROVIDER);
        settings.model = MODEL;
        settings.timeoutSeconds = 300;

        try (TranslationRepository database = new TranslationRepository(context)) {
            EditorialP4Binding binding = new EditorialP4BindingDao(database)
                    .findByAttemptRequestSelector(SELECTOR).orElseThrow(
                            () -> new AssertionError("VOL5 P4 binding is not persisted"));
            long projectId = projectId(database, binding.bindingIdentity());
            assertTrue("VOL5 persisted project id is required", projectId > 0);
            List<EditorialP4InputSource> sources = readSources(context);
            EditorialP4ResumeResult resumed = new EditorialP4BindingTransactionService(database,
                    new EditorialPackStorageLayout(context.getFilesDir().toPath()))
                    .resumeProject(projectId, SELECTOR, sources);
            assertEquals(EditorialP4ResumeResult.Code.RESTORED, resumed.code());
            assertEquals(binding.bindingIdentity(), resumed.binding().bindingIdentity());

            EditorialP5CAttemptStore attemptStore = new EditorialP5CAttemptStore(database);
            EditorialP5CAttemptStore.AttemptRecord oldAttempt = attemptStore
                    .findRecord(ORIGINAL_ATTEMPT_ID).orElseThrow(
                            () -> new AssertionError("VOL5 recovery attempt is missing"));
            assertEquals("RECOVERY_REQUIRED", oldAttempt.status());
            assertEquals("RETRY_PROVIDER_CALL_FAILED_UNKNOWN", oldAttempt.recoveryReasonCode());
            assertEquals("L1_RAW_DISCOVERY", oldAttempt.phase());
            assertEquals("CONSUMED", attemptStore.findAuthorizationReceipt(
                    ORIGINAL_AUTHORIZATION_HASH).orElseThrow().consumptionResult());

            String endpointAccountFingerprint = EditorialCanonicalJson.sha256Hex(
                    (settings.baseUrl + "|" + EditorialCanonicalJson.sha256Hex(
                            settings.apiKey.getBytes(StandardCharsets.UTF_8)))
                            .getBytes(StandardCharsets.UTF_8));
            long issuedAt = System.currentTimeMillis();
            long expiresAt = issuedAt + PILOT_WINDOW_MILLIS;
            EditorialP5PilotAuthorization raw = new EditorialP5PilotAuthorization(
                    DIAGNOSTIC_AUTHORIZATION_ID, binding.bindingIdentity(),
                    binding.runDeclarationIdentity(), binding.canonicalPackHash(),
                    binding.canonicalProfileHash(), binding.compatibilityEvaluationId(),
                     CHAPTER_KEY, "L1_RAW_DISCOVERY", PROVIDER, MODEL,
                     endpointAccountFingerprint, 1, 0, 0, 100_000,
                     HISTORICAL_DIAGNOSTIC_OUTPUT_TOKENS, 100_000,
                    BigDecimal.valueOf(0.10), PILOT_WINDOW_MILLIS, true, false, false,
                    "HASH_ONLY", "USER_AUTHORIZED_P5D_RAW_DIAGNOSTIC;PRIOR_CANCELLED_GENERATION;"
                            + "DUPLICATE_BILLING_RISK_ACKNOWLEDGED", issuedAt, expiresAt, true);
            assertEquals(DIAGNOSTIC_AUTHORIZATION_HASH,
                    EditorialP5CAttemptStore.authorizationIdHash(DIAGNOSTIC_AUTHORIZATION_ID));

            EditorialP5CAttemptStore.ReconciliationDecision decision =
                    new EditorialP5CAttemptStore.ReconciliationDecision(
                            ORIGINAL_ATTEMPT_ID,
                            EditorialP5CAttemptStore.ExternalStateClassification
                                    .EXTERNAL_CONFIRMED_CANCELLED,
                            "docs/P5D_VOL5_RAW_PROVIDER_RECONCILIATION.md",
                            endpointAccountFingerprint,
                            "CANCELLED_DISPLAYED_COST_0.00484",
                            "P5D_USER_AUTHORIZATION_20260909", true, true,
                            DIAGNOSTIC_AUTHORIZATION_HASH, 1788955200000L);
            java.util.Optional<EditorialP5CAttemptStore.ReconciliationRecord> existing =
                    attemptStore.findReconciliation(ORIGINAL_ATTEMPT_ID);
            if (existing.isEmpty()) {
                // The recovery decision is written through the durable owner,
                // never by a test-side SQL state mutation.
                attemptStore.recordReconciliation(decision);
            }
            EditorialP5CAttemptStore.ReconciliationRecord reconciliation = attemptStore
                    .findReconciliation(ORIGINAL_ATTEMPT_ID).orElseThrow();
            assertEquals(EditorialP5CAttemptStore.ExternalStateClassification
                    .EXTERNAL_CONFIRMED_CANCELLED, reconciliation.classification());
            assertEquals(DIAGNOSTIC_AUTHORIZATION_HASH,
                    reconciliation.newAuthorizationIdHash());
            assertTrue(reconciliation.retryEligible());
            assertTrue(reconciliation.duplicateRiskAcknowledged());

            keepTargetForegroundForDiagnostic();
            OpenRouterEditorialP5PilotProvider provider =
                    OpenRouterEditorialP5PilotProvider.withLifecyclePersistence(
                             settings, HISTORICAL_DIAGNOSTIC_OUTPUT_TOKENS, database);
            assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: provider configuration is incomplete",
                    provider.configured());
            EditorialP5CExactBindingExecution.Result result =
                    new EditorialP5CExactBindingExecution(database,
                            new EditorialPackStorageLayout(context.getFilesDir().toPath()))
                            .executeRaw(projectId, SELECTOR, CHAPTER_KEY, raw, provider);
            logResult(result);
            assertNotNull(result);
            assertTrue("RAW diagnostic exceeded one primary call", result.providerCalls() <= 1);
            org.junit.Assert.assertNull("RAW-only diagnostic must not call RECONCILE",
                    result.reconcileResult());
            assertTrue("execution must remain disabled", !result.executionAllowed());
            assertEquals("NOT_CERTIFIED", result.certificationState());
            Log.i("P5D_RAW_DIAGNOSTIC", "authorizationHash=" + DIAGNOSTIC_AUTHORIZATION_HASH
                    + " originalAttempt=" + ORIGINAL_ATTEMPT_ID
                    + " providerCalls=" + result.providerCalls()
                    + " status=" + result.status() + " reason=" + result.reasonCode());
        }
    }

    private static void keepTargetForegroundForDiagnostic() throws Exception {
        try (android.os.ParcelFileDescriptor command = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().executeShellCommand("am start -n com.ml.tblandroidtxt/.MainActivity")) {
            // The test-only foreground keepalive prevents a quiet socket from
            // being freezer-suspended on the validation device. It changes no
            // production timeout or cancellation policy.
        }
    }

    private static List<EditorialP4InputSource> readSources(Context context) throws Exception {
        return List.of(
                source(context, "RAW", "raw", "001_RAW_MERCEDES_VOL5.TXT"),
                source(context, "DRAFT", "draft", "001_DRAFT_MERCEDES_VOL5.TXT"),
                source(context, "GLOSSARY", "glossary", "001_GLOSSARY_MERCEDES_VOL5.CSV"),
                source(context, "PRONOUN", "pronoun", "001_PRONOUN_MERCEDES_VOL5.CSV"));
    }

    private static EditorialP4InputSource source(Context context, String role, String id,
                                                  String originalName) throws Exception {
        byte[] bytes;
        try (InputStream input = context.openFileInput("p5d-vol5-" + id + ".bin")) {
            bytes = stripUtf8Bom(input.readAllBytes());
        }
        if (bytes.length == 0) throw new AssertionError("empty VOL5 source: " + originalName);
        return new EditorialP4InputSource(role, "p5d-real://mercedes-vol5/001/" + id,
                bytes, "UTF-8", "VALID", 0L);
    }

    private static byte[] stripUtf8Bom(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xef && (bytes[1] & 0xff) == 0xbb
                && (bytes[2] & 0xff) == 0xbf) return Arrays.copyOfRange(bytes, 3, bytes.length);
        return bytes;
    }

    private static void insertChapter(TranslationRepository database, long projectId,
                                      List<EditorialP4InputSource> sources) {
        android.database.sqlite.SQLiteDatabase db = database.editorialWritableDatabase();
        long now = System.currentTimeMillis();
        db.beginTransaction();
        try {
            ContentValues chapter = new ContentValues();
            chapter.put("project_id", projectId);
            chapter.put("chapter_key", CHAPTER_KEY);
            chapter.put("title", "MERCEDES VOL 5 • 001");
            chapter.put("state", EditorialSafe4Workflow.ChapterState.SAFE4_BLOCKED.name());
            chapter.put("raw_hash", EditorialCanonicalJson.sha256Hex(sources.get(0).bytes()));
            chapter.put("created_at", now);
            chapter.put("updated_at", now);
            long chapterId = db.insertOrThrow("editorial_chapters", null, chapter);
            for (EditorialP4InputSource source : sources) {
                ContentValues asset = new ContentValues();
                asset.put("chapter_id", chapterId);
                asset.put("role", source.role());
                asset.put("source_uri", source.sourceReference());
                asset.put("display_name", source.role().toLowerCase() + ".source");
                asset.put("sha256", EditorialCanonicalJson.sha256Hex(source.bytes()));
                asset.put("size_bytes", source.bytes().length);
                asset.put("content", new String(source.bytes(), StandardCharsets.UTF_8));
                asset.put("created_at", now);
                db.insertOrThrow("editorial_assets", null, asset);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static List<Long> listChapterIds(TranslationRepository database, long projectId) {
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        try (android.database.Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT id FROM editorial_chapters WHERE project_id=? AND chapter_key=?",
                new String[]{String.valueOf(projectId), CHAPTER_KEY})) {
            while (cursor.moveToNext()) ids.add(cursor.getLong(0));
        }
        return ids;
    }

    private static long projectId(TranslationRepository database, String bindingIdentity) {
        try (android.database.Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT project_row_id FROM editorial_p4_bindings WHERE binding_identity=?",
                new String[]{bindingIdentity})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    private static void logBinding(EditorialP4Binding binding, long projectId,
                                   List<EditorialP4InputSource> sources) {
        Log.i("P5D_VOL5_BINDING", "projectId=" + projectId + " selector=" + SELECTOR
                + " bindingIdentity=" + binding.bindingIdentity()
                + " runDeclarationIdentity=" + binding.runDeclarationIdentity()
                + " canonicalPackHash=" + binding.canonicalPackHash()
                + " canonicalProfileHash=" + binding.canonicalProfileHash()
                + " sourceManifestFingerprint=" + binding.inputManifestFingerprint());
        for (EditorialP4InputSource source : sources) {
            Log.i("P5D_VOL5_SOURCE", "role=" + source.role() + " length=" + source.bytes().length
                    + " sha256=" + EditorialCanonicalJson.sha256Hex(source.bytes()));
        }
    }

    private static void logResult(EditorialP5CExactBindingExecution.Result result) {
        Log.i("P5D_RAW_RESULT", "status=" + result.status() + " reason=" + result.reasonCode()
                + " providerCalls=" + result.providerCalls()
                + " executionAllowed=" + result.executionAllowed()
                + " certificationState=" + result.certificationState());
        EditorialP5PilotResult raw = result.rawResult();
        if (raw == null) {
            Log.i("P5D_RAW_METRICS", "result=NOT_AVAILABLE");
            return;
        }
        EditorialP5PilotResult.Metrics metrics = raw.metrics();
        Log.i("P5D_RAW_METRICS", "outcome=" + raw.outcome() + " reason=" + raw.reasonCode()
                + " primaryCalls=" + metrics.primaryCalls()
                + " repairCalls=" + metrics.repairCalls()
                + " networkRetries=" + metrics.networkRetries()
                + " inputTokens=" + metrics.inputTokens()
                + " outputTokens=" + metrics.outputTokens()
                + " totalTokens=" + metrics.totalTokens()
                + " actualReportedCost=" + metrics.actualReportedCost()
                + " finishReason=" + metrics.finishReason()
                + " truncated=" + metrics.truncated()
                + " schemaValid=" + metrics.schemaValidationPassed()
                + " receiptValid=" + metrics.receiptValidationPassed()
                 + " latencyMs=" + metrics.latencyMillis());
    }

    private static void logP5eResult(EditorialP5CExactBindingExecution.Result result) {
        Log.i("P5E_RAW_RESULT", "status=" + result.status() + " reason=" + result.reasonCode()
                + " providerCalls=" + result.providerCalls()
                + " executionAllowed=" + result.executionAllowed()
                + " certificationState=" + result.certificationState());
        EditorialP5PilotResult raw = result.rawResult();
        if (raw == null) {
            Log.i("P5E_RAW_METRICS", "result=NOT_AVAILABLE");
            return;
        }
        EditorialP5PilotResult.Metrics metrics = raw.metrics();
        Log.i("P5E_RAW_METRICS", "outcome=" + raw.outcome() + " reason=" + raw.reasonCode()
                + " primaryCalls=" + metrics.primaryCalls()
                + " repairCalls=" + metrics.repairCalls()
                + " networkRetries=" + metrics.networkRetries()
                + " inputTokens=" + metrics.inputTokens()
                + " outputTokens=" + metrics.outputTokens()
                + " reasoningTokens=" + metrics.reasoningTokens()
                + " totalTokens=" + metrics.totalTokens()
                + " actualReportedCost=" + metrics.actualReportedCost()
                + " estimatedCost=" + metrics.estimatedCost()
                + " costAccountingComplete=" + metrics.costAccountingComplete()
                + " finishReason=" + metrics.finishReason()
                + " truncated=" + metrics.truncated()
                + " schemaValid=" + metrics.schemaValidationPassed()
                + " receiptValid=" + metrics.receiptValidationPassed()
                + " latencyMs=" + metrics.latencyMillis());
    }

    private static String scalar(SQLiteDatabase db, String sql, String argument) {
        try (Cursor cursor = db.rawQuery(sql, new String[]{argument})) {
            assertTrue("expected persisted VOL5 row", cursor.moveToFirst());
            return cursor.getString(0);
        }
    }

    private static int count(SQLiteDatabase db, String sql, String argument) {
        try (Cursor cursor = db.rawQuery(sql, new String[]{argument})) {
            assertTrue("expected count result", cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private static int intScalar(SQLiteDatabase db, String sql, String argument) {
        try (Cursor cursor = db.rawQuery(sql, new String[]{argument})) {
            assertTrue("expected integer result", cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
