package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialLedgerValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5L1Output;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotExecution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;
import com.ml.tblandroidtxt.editorial.pack.EditorialStopDecision;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * P5C app-bound fake E2E. It exercises the persisted P4 binding and the
 * production attempt owner without sending chapter data to a provider.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5CExactBindingFakeE2EInstrumentedTest {
    private static final String CANONICAL_ASSET =
            "editorial-p2/v5-safe-4.1.3-full-canonical.zip";
    private static final String CHAPTER_KEY = "p5c-fake-chapter-1";
    private static final String SELECTOR = "p5c-fake-binding-selector";
    private static final String CANONICAL_ZIP_SHA256 =
            "b9c65dbeb9d4c4ed46b67d5ec28ff6252cc2bdc4b63bc902904612987ec58987";

    private Context context;
    private String databaseName;
    private TranslationRepository database;
    private Path storageRoot;
    private EditorialPackStorageLayout storage;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "p5c-fake-e2e-" + UUID.randomUUID() + ".db";
        database = new TranslationRepository(context, databaseName);
        storageRoot = context.getCacheDir().toPath().resolve("p5c-fake-e2e-" + UUID.randomUUID());
        storage = new EditorialPackStorageLayout(storageRoot);
    }

    @After public void tearDown() throws Exception {
        if (database != null) database.close();
        context.deleteDatabase(databaseName);
        deleteTree(storageRoot);
    }

    @Test public void v20ProvidesDurableAttemptOwnerAfterCharacterization() {
        assertTrue(tableExists("editorial_p5c_attempts"));
        assertTrue(columnExists("editorial_p5c_attempts", "report_bytes"));
        assertTrue(columnExists("editorial_p5c_attempts", "receipt_bytes"));
        assertTrue(columnExists("editorial_p5c_attempts", "recovery_reason_code"));
    }

    @Test public void p5dRecoveryOwnersExistBeforeRetryIsAllowed() {
        assertTrue(tableExists("editorial_p5d_network_lifecycle"));
        assertTrue(tableExists("editorial_p5d_authorization_receipts"));
        assertTrue(tableExists("editorial_p5d_reconciliation"));
        assertTrue(tableExists("editorial_p5d_reconciliation_history"));
        assertTrue(columnExists("editorial_p5d_network_lifecycle", "stage"));
        assertTrue(columnExists("editorial_p5d_network_lifecycle", "response_body_bytes"));
        assertTrue(columnExists("editorial_p5d_authorization_receipts", "authorization_id_hash"));
        assertTrue(columnExists("editorial_p5d_reconciliation", "classification"));
    }

    @Test public void exactBindingRunsRawThenReconcileAndReloadsIdempotently() throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5PilotRequest reconcileRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RECONCILE, rawRequest.attemptIdentity());
        FakeProvider provider = new FakeProvider(Map.of(
                "L1_RAW_DISCOVERY", output(rawRequest),
                "L1_RECONCILE", output(reconcileRequest)));

        EditorialP5CExactBindingExecution.Result result = new EditorialP5CExactBindingExecution(
                database, storage).execute(fixture.projectId, SELECTOR, CHAPTER_KEY,
                authorization(fixture.binding, "auth-raw", "L1_RAW_DISCOVERY"),
                authorization(fixture.binding, "auth-reconcile", "L1_RECONCILE"), provider);

        assertEquals(EditorialP5CExactBindingExecution.Status.COMMITTED, result.status());
        assertEquals("P5C_L1_PAIR_COMMITTED", result.reasonCode());
        assertEquals(EditorialP5PilotResult.Outcome.COMMITTED, result.rawResult().outcome());
        assertEquals(EditorialP5PilotResult.Outcome.COMMITTED, result.reconcileResult().outcome());
        assertEquals(2, result.providerCalls());
        assertEquals(2, provider.calls);
        assertEquals("L1_RAW_DISCOVERY", provider.requests.get(0).phase());
        assertEquals(Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY),
                provider.requests.get(0).visibleSources().keySet());
        assertEquals("L1_RECONCILE", provider.requests.get(1).phase());
        assertEquals(Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                        EditorialSafe4Contract.GLOSSARY, EditorialSafe4Contract.PRONOUN),
                provider.requests.get(1).visibleSources().keySet());
        assertFalse(provider.requests.get(0).visibleSources().containsKey(EditorialSafe4Contract.DRAFT));
        assertFalse(provider.requests.get(0).visibleSources().containsKey(EditorialSafe4Contract.PRONOUN));

        EditorialP5CAttemptStore attemptStore = new EditorialP5CAttemptStore(database);
        EditorialP5CAttemptStore.AttemptRecord rawRecord = attemptStore.findRecord(
                result.rawResult().committedResult().attemptIdentity()).orElseThrow();
        EditorialP5CAttemptStore.AttemptRecord reconcileRecord = attemptStore.findRecord(
                result.reconcileResult().committedResult().attemptIdentity()).orElseThrow();
        assertEquals("COMMITTED", rawRecord.status());
        assertEquals("COMMITTED", reconcileRecord.status());
        assertEquals(rawRequest.attemptIdentity(), rawRecord.attemptIdentity());
        assertEquals(reconcileRequest.predecessorIdentity(), reconcileRecord.predecessorIdentity());
        assertArrayEquals(result.rawResult().committedResult().reportBytes(), rawRecord.reportBytes());
        assertArrayEquals(result.rawResult().committedResult().receiptBytes(), rawRecord.receiptBytes());
        assertArrayEquals(result.reconcileResult().committedResult().reportBytes(), reconcileRecord.reportBytes());
        assertArrayEquals(result.reconcileResult().committedResult().receiptBytes(), reconcileRecord.receiptBytes());
        assertFalse(rawRecord.metricsJson().contains("fake-response"));
        assertEquals(2, attemptStore.count());

        database.close();
        database = new TranslationRepository(context, databaseName);
        FakeProvider noCallProvider = new FakeProvider(Map.of());
        EditorialP5CExactBindingExecution.Result afterRestart = new EditorialP5CExactBindingExecution(
                database, storage).execute(fixture.projectId, SELECTOR, CHAPTER_KEY,
                authorization(fixture.binding, "auth-raw-replay", "L1_RAW_DISCOVERY"),
                authorization(fixture.binding, "auth-reconcile-replay", "L1_RECONCILE"), noCallProvider);

        assertEquals(EditorialP5CExactBindingExecution.Status.ALREADY_COMMITTED,
                afterRestart.status());
        assertEquals("P5C_ALREADY_COMMITTED", afterRestart.reasonCode());
        assertEquals(0, afterRestart.providerCalls());
        assertEquals(0, noCallProvider.calls);
        assertEquals(2, new EditorialP5CAttemptStore(database).count());
    }

    @Test public void exactBindingRunsRawOnlyAndStopsBeforeReconcile() throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        FakeProvider provider = new FakeProvider(Map.of(
                "L1_RAW_DISCOVERY", output(rawRequest)));

        EditorialP5CExactBindingExecution.Result result = new EditorialP5CExactBindingExecution(
                database, storage).executeRaw(fixture.projectId, SELECTOR, CHAPTER_KEY,
                authorization(fixture.binding, "auth-raw-only", "L1_RAW_DISCOVERY"), provider);

        assertEquals(EditorialP5CExactBindingExecution.Status.COMMITTED, result.status());
        assertEquals("P5C_RAW_COMMITTED", result.reasonCode());
        assertEquals(EditorialP5PilotResult.Outcome.COMMITTED, result.rawResult().outcome());
        assertNull(result.reconcileResult());
        assertEquals(1, result.providerCalls());
        assertEquals(1, provider.calls);
        assertEquals("L1_RAW_DISCOVERY", provider.requests.get(0).phase());
        assertEquals(Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY),
                provider.requests.get(0).visibleSources().keySet());
        EditorialP5CAttemptStore.AttemptRecord record = new EditorialP5CAttemptStore(database)
                .findRecord(result.rawResult().committedResult().attemptIdentity()).orElseThrow();
        assertEquals("COMMITTED", record.status());
        assertArrayEquals(result.rawResult().committedResult().reportBytes(), record.reportBytes());
        assertArrayEquals(result.rawResult().committedResult().receiptBytes(), record.receiptBytes());

        database.close();
        database = new TranslationRepository(context, databaseName);
        FakeProvider noCallProvider = new FakeProvider(Map.of());
        EditorialP5CExactBindingExecution.Result afterRestart = new EditorialP5CExactBindingExecution(
                database, storage).executeRaw(fixture.projectId, SELECTOR, CHAPTER_KEY,
                authorization(fixture.binding, "auth-raw-only-replay", "L1_RAW_DISCOVERY"), noCallProvider);
        assertEquals(EditorialP5CExactBindingExecution.Status.ALREADY_COMMITTED,
                afterRestart.status());
        assertEquals("P5C_RAW_ALREADY_COMMITTED", afterRestart.reasonCode());
        assertNull(afterRestart.reconcileResult());
        assertEquals(0, afterRestart.providerCalls());
        assertEquals(0, noCallProvider.calls);
        assertEquals(1, new EditorialP5CAttemptStore(database).count());
    }

    @Test public void incompleteAuthorizationStopsBeforeProvider() throws Exception {
        BindingFixture fixture = createBoundChapter();
        CountingProvider provider = new CountingProvider();
        EditorialP5CExactBindingExecution.Result result = new EditorialP5CExactBindingExecution(
                database, storage).execute(fixture.projectId, SELECTOR, CHAPTER_KEY,
                null, null, provider);
        assertEquals(EditorialP5CExactBindingExecution.Status.STOP, result.status());
        assertEquals("LIVE_AUTHORIZATION_INCOMPLETE", result.reasonCode());
        assertEquals(0, result.providerCalls());
        assertEquals(0, provider.calls);
        assertEquals(0, new EditorialP5CAttemptStore(database).count());
    }

    @Test public void sourceDriftStopsBeforeProviderAndLeavesNoAttempt() throws Exception {
        BindingFixture fixture = createBoundChapter();
        database.editorialWritableDatabase().execSQL(
                "UPDATE editorial_assets SET content=? WHERE chapter_id=? AND role='RAW'",
                new Object[]{"changed raw bytes", String.valueOf(fixture.chapterId)});
        CountingProvider provider = new CountingProvider();
        EditorialP5CExactBindingExecution.Result result = new EditorialP5CExactBindingExecution(
                database, storage).execute(fixture.projectId, SELECTOR, CHAPTER_KEY,
                authorization(fixture.binding, "auth-raw-drift", "L1_RAW_DISCOVERY"),
                authorization(fixture.binding, "auth-reconcile-drift", "L1_RECONCILE"), provider);
        assertEquals(EditorialP5CExactBindingExecution.Status.STOP, result.status());
        assertEquals("STOP_SOURCE_DRIFT", result.reasonCode());
        assertEquals(0, result.providerCalls());
        assertEquals(0, provider.calls);
        assertEquals(0, new EditorialP5CAttemptStore(database).count());
    }

    @Test public void recoveryRequiredCannotBeReclaimedWithoutReconciliation() throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5PilotAuthorization authorization = authorization(fixture.binding,
                "auth-recovery", "L1_RAW_DISCOVERY");
        EditorialP5CAttemptStore firstStore = new EditorialP5CAttemptStore(database);
        firstStore.prepare(rawRequest, authorization, "a".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                firstStore.claim(rawRequest.attemptIdentity()));

        EditorialP5CAttemptStore reopenedStore = new EditorialP5CAttemptStore(database);
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.IN_FLIGHT,
                reopenedStore.claim(rawRequest.attemptIdentity()));
        assertEquals("CLAIMED", reopenedStore.findRecord(rawRequest.attemptIdentity())
                .orElseThrow().status());

        reopenedStore.markRecoveryRequired(rawRequest.attemptIdentity(), "RETRY_TEST_RECOVERY");
        EditorialP5CAttemptStore retryStore = new EditorialP5CAttemptStore(database);
        retryStore.prepare(rawRequest, authorization, "a".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.RECOVERY_REQUIRED,
                retryStore.claim(rawRequest.attemptIdentity()));
        assertEquals(1, retryStore.count());
    }

    @Test public void p5dPersistsRedactedLifecycleAndSingleUseAuthorizationAcrossReopen()
            throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5PilotAuthorization authorization = authorization(fixture.binding,
                "p5d-auth-lifecycle", "L1_RAW_DISCOVERY");
        EditorialP5CAttemptStore store = new EditorialP5CAttemptStore(database);
        store.prepare(rawRequest, authorization, "b".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                store.claim(rawRequest.attemptIdentity()));

        String authorizationHash = EditorialP5CAttemptStore.authorizationIdHash(
                authorization.authorizationId());
        EditorialP5CAttemptStore.AuthorizationReceipt receipt = store
                .findAuthorizationReceipt(authorizationHash).orElseThrow();
        assertEquals(authorizationHash, receipt.authorizationIdHash());
        assertFalse(receipt.authorizationIdHash().equals(authorization.authorizationId()));
        assertEquals("CONSUMED", receipt.consumptionResult());

        store.recordNetworkLifecycle(rawRequest.attemptIdentity(),
                EditorialP5CAttemptStore.NetworkLifecycleEvent.stage(
                        EditorialP5CAttemptStore.LifecycleStage.CALL_CREATED));
        store.recordNetworkLifecycle(rawRequest.attemptIdentity(),
                new EditorialP5CAttemptStore.NetworkLifecycleEvent(
                        EditorialP5CAttemptStore.LifecycleStage.REQUEST_BODY_SENT,
                        321L, -1, "", 10L, "", ""));
        store.recordNetworkLifecycle(rawRequest.attemptIdentity(),
                new EditorialP5CAttemptStore.NetworkLifecycleEvent(
                        EditorialP5CAttemptStore.LifecycleStage.RESPONSE_HEADERS_RECEIVED,
                        321L, 200, "", 20L, "generation-redacted", ""));
        store.recordNetworkLifecycle(rawRequest.attemptIdentity(),
                new EditorialP5CAttemptStore.NetworkLifecycleEvent(
                        EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_COMPLETE,
                        321L, 200, "", 30L, "generation-redacted", "provider-response"));

        EditorialP5CAttemptStore.NetworkLifecycleEvent lifecycle = store
                .findNetworkLifecycle(rawRequest.attemptIdentity()).orElseThrow();
        assertEquals(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_COMPLETE,
                lifecycle.stage());
        assertEquals(321L, lifecycle.requestBodyBytes());
        assertEquals(200, lifecycle.httpStatus());
        assertEquals(30L, lifecycle.elapsedMillis());
        assertEquals("generation-redacted", lifecycle.generationId());
        assertEquals("provider-response", lifecycle.providerResponseId());
        assertFalse(columnExists("editorial_p5d_network_lifecycle", "response_body"));

        database.close();
        database = new TranslationRepository(context, databaseName);
        EditorialP5CAttemptStore reopened = new EditorialP5CAttemptStore(database);
        assertEquals("CONSUMED", reopened.findAuthorizationReceipt(authorizationHash)
                .orElseThrow().consumptionResult());
        assertEquals(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_COMPLETE,
                reopened.findNetworkLifecycle(rawRequest.attemptIdentity()).orElseThrow().stage());
    }

    @Test public void p5dLocalInstantResponseCompletesAndPersistsLifecycle() throws Exception {
        runLocalResponse(0L, false, "p5d-local-instant");
    }

    @Test public void p5dPersistsLatestResponseBodyProgressWithoutBodyContent()
            throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5PilotAuthorization authorization = authorization(fixture.binding,
                "p5d-response-progress", "L1_RAW_DISCOVERY");
        EditorialP5CAttemptStore store = new EditorialP5CAttemptStore(database);
        store.prepare(rawRequest, authorization, "f".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                store.claim(rawRequest.attemptIdentity()));
        store.recordNetworkLifecycle(rawRequest.attemptIdentity(),
                new EditorialP5CAttemptStore.NetworkLifecycleEvent(
                        EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_PROGRESS,
                        321L, 123L, 200, "application/json", "", 250L,
                        "generation-redacted", "", ""));

        EditorialP5CAttemptStore.NetworkLifecycleEvent lifecycle = store
                .findNetworkLifecycle(rawRequest.attemptIdentity()).orElseThrow();
        assertEquals(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_PROGRESS,
                lifecycle.stage());
        assertEquals(123L, lifecycle.responseBodyBytes());
        assertEquals(250L, lifecycle.elapsedMillis());
        assertFalse(lifecycle.responseContentType().contains("raw"));

        database.close();
        database = new TranslationRepository(context, databaseName);
        EditorialP5CAttemptStore.NetworkLifecycleEvent reopened = new EditorialP5CAttemptStore(
                database).findNetworkLifecycle(rawRequest.attemptIdentity()).orElseThrow();
        assertEquals(123L, reopened.responseBodyBytes());
        assertEquals(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_PROGRESS,
                reopened.stage());
    }

    @Test public void p5dProcessRestartReclassifiesExpiredClaimWithoutRedispatch()
            throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5PilotAuthorization authorization = authorization(fixture.binding,
                "p5d-stale-claim", "L1_RAW_DISCOVERY", 1L);
        EditorialP5CAttemptStore store = new EditorialP5CAttemptStore(database);
        store.prepare(rawRequest, authorization, "a".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                store.claim(rawRequest.attemptIdentity()));
        database.editorialWritableDatabase().execSQL(
                "UPDATE editorial_p5c_attempts SET created_at=0,updated_at=0 WHERE attempt_identity=?",
                new Object[]{rawRequest.attemptIdentity()});

        database.close();
        database = new TranslationRepository(context, databaseName);
        EditorialP5CAttemptStore reopened = new EditorialP5CAttemptStore(database);
        reopened.recoverStaleClaims(System.currentTimeMillis());
        assertEquals("RECOVERY_REQUIRED", reopened.findRecord(rawRequest.attemptIdentity())
                .orElseThrow().status());
        assertEquals("RETRY_PROVIDER_CALL_TIMEOUT", reopened.findRecord(rawRequest.attemptIdentity())
                .orElseThrow().recoveryReasonCode());
        reopened.prepare(rawRequest, authorization, "a".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.RECOVERY_REQUIRED,
                reopened.claim(rawRequest.attemptIdentity()));
    }

    @Test public void p5dLocalSlowResponseSurvivesLegacyCancelAndPersistsLifecycle() throws Exception {
        runLocalResponse(11_000L, true, "p5d-local-slow");
    }

    @Test public void p5dLocalSlowResponseCompletesWithoutLegacyCancel() throws Exception {
        runLocalResponse(11_000L, false, "p5d-local-slow-no-cancel");
    }

    @Test public void p5dExecuteRawStalledBodyStopsAtAttemptDeadlineAndPersistsRecovery()
            throws Exception {
        runStalledBodyAttempt(1_500L, "p5d-stalled-body-deadline", 4_000L);
    }

    @Test public void p5dExecuteRawStalledBodyStopsAtFiveMinuteDeadlineWithoutHostForceStop()
            throws Exception {
        runStalledBodyAttempt(300_000L, "p5d-stalled-body-five-minute", 305_000L);
    }

    private void runStalledBodyAttempt(long deadlineMillis, String authorizationId,
                                       long maximumElapsedMillis) throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotAuthorization authorization = authorization(fixture.binding,
                authorizationId, "L1_RAW_DISCOVERY", deadlineMillis);
        AppSettings settings = new AppSettings();
        settings.provider = "openrouter";
        settings.model = "openai/gpt-5.6-luna";
        settings.apiKey = "local-test-key";

        if (deadlineMillis >= 60_000L) keepTargetForegroundForTransport();
        try (StalledBodyServer server = new StalledBodyServer()) {
            settings.baseUrl = "http://127.0.0.1:" + server.port() + "/v1/chat/completions";
            OpenRouterEditorialP5PilotProvider provider =
                    OpenRouterEditorialP5PilotProvider.withLifecyclePersistence(settings, 4_096,
                            database);
            EditorialP5CExactBindingExecution execution =
                    new EditorialP5CExactBindingExecution(database, storage);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            long startedNanos = System.nanoTime();
            Future<EditorialP5CExactBindingExecution.Result> resultFuture = executor.submit(
                    () -> execution.executeRaw(fixture.projectId, SELECTOR, CHAPTER_KEY,
                            authorization, provider));
            try {
                assertTrue("local server did not receive the RAW request",
                        server.awaitRequest(5, TimeUnit.SECONDS));
                EditorialP5CExactBindingExecution.Result result = resultFuture.get(
                        deadlineMillis + 10_000L, TimeUnit.MILLISECONDS);
                long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startedNanos);
                assertEquals(EditorialP5CExactBindingExecution.Status.STOP, result.status());
                assertEquals("RETRY_PROVIDER_CALL_TIMEOUT", result.reasonCode());
                assertEquals(1, result.providerCalls());
                assertEquals(1, server.requestCount());
                assertTrue("app did not stop within the bounded deadline: " + elapsedMillis,
                        elapsedMillis < maximumElapsedMillis);
                String attemptIdentity;
                try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                        "SELECT attempt_identity FROM editorial_p5c_attempts LIMIT 1", null)) {
                    assertTrue(cursor.moveToFirst());
                    attemptIdentity = cursor.getString(0);
                }
                assertEquals("RECOVERY_REQUIRED", new EditorialP5CAttemptStore(database)
                        .findRecord(attemptIdentity).orElseThrow().status());
            } catch (TimeoutException timeout) {
                server.captureThreadStacks("execute_raw_deadline");
                throw new AssertionError("executeRaw outlived its attempt deadline: "
                        + server.failureSummary(), timeout);
            } finally {
                resultFuture.cancel(true);
                executor.shutdownNow();
                assertTrue("executeRaw worker cleanup incomplete",
                        executor.awaitTermination(2, TimeUnit.SECONDS));
            }
        }
    }

    private void runLocalResponse(long delayMillis, boolean cancelLegacy, String authorizationId)
            throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5CAttemptStore store = new EditorialP5CAttemptStore(database);
        EditorialP5PilotAuthorization authorization = authorization(fixture.binding,
                authorizationId, "L1_RAW_DISCOVERY");
        store.prepare(rawRequest, authorization, "d".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                store.claim(rawRequest.attemptIdentity()));

        try (SlowJsonServer server = new SlowJsonServer(delayMillis)) {
            if (delayMillis > 0L) keepTargetForegroundForTransport();
            AppSettings settings = new AppSettings();
            settings.provider = "openrouter";
            settings.model = "openai/gpt-5.6-luna";
            settings.apiKey = "test-key-not-a-secret";
            settings.baseUrl = "http://127.0.0.1:" + server.port() + "/v1/chat/completions";
            // The device may freezer-suspend an instrumentation process while the
            // socket is intentionally quiet. Keep this test deadline explicit and
            // bounded, but leave enough room to observe the >10s transport delay
            // after the process is resumed. This is test-only; it does not change
            // the production pilot deadline.
            settings.timeoutSeconds = 60;
            OpenRouterEditorialP5PilotProvider provider =
                    OpenRouterEditorialP5PilotProvider.withLifecyclePersistence(settings, 4_096, database);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<EditorialP5PilotProvider.Response> response = executor.submit(
                        () -> provider.call(transportRequest(rawRequest)));
                if (!server.awaitRequest(5, TimeUnit.SECONDS)) {
                    // Surface a client failure instead of misclassifying a broken test server as a cancellation.
                    try {
                        response.get(1, TimeUnit.SECONDS);
                    } catch (ExecutionException | TimeoutException ignored) {
                        server.captureThreadStacks("request_deadline");
                    }
                    throw new AssertionError("local server did not receive the single request: "
                            + server.failureSummary());
                }
                if (cancelLegacy) {
                    // This legacy translation cancellation must not own or cancel P5D RAW.
                    Log.i("P5D_LOCAL_HTTP", "legacy_cancel_before");
                    OpenAICompatibleClient.cancelActiveRequests();
                    Log.i("P5D_LOCAL_HTTP", "legacy_cancel_after");
                }
                try {
                    assertNotNull(response.get(delayMillis + 70_000L, TimeUnit.MILLISECONDS));
                } catch (TimeoutException timeout) {
                    server.captureThreadStacks("client_deadline");
                    throw new AssertionError("local client did not finish: "
                            + server.failureSummary(), timeout);
                } catch (ExecutionException failure) {
                    throw new AssertionError("local client failed: "
                            + server.failureSummary(), failure.getCause());
                }
                server.awaitComplete(5, TimeUnit.SECONDS);
                assertEquals(1, server.requestCount());
                Log.i("P5D_LOCAL_HTTP", "test_finished");
            } finally {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("local client cleanup incomplete");
                }
            }
        }

        EditorialP5CAttemptStore.NetworkLifecycleEvent lifecycle = store
                .findNetworkLifecycle(rawRequest.attemptIdentity()).orElseThrow();
        assertEquals(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_COMPLETE,
                lifecycle.stage());
        assertEquals(200, lifecycle.httpStatus());
        assertEquals("application/json", lifecycle.responseContentType());
        assertEquals("", lifecycle.cancellationSource());
        assertEquals("generation-local-test", lifecycle.generationId());
        assertTrue(lifecycle.requestBodyBytes() > 0);
        assertTrue(lifecycle.elapsedMillis() >= delayMillis);

        database.close();
        database = new TranslationRepository(context, databaseName);
        EditorialP5CAttemptStore.NetworkLifecycleEvent reopened = new EditorialP5CAttemptStore(database)
                .findNetworkLifecycle(rawRequest.attemptIdentity()).orElseThrow();
        assertEquals(EditorialP5CAttemptStore.LifecycleStage.RESPONSE_BODY_COMPLETE,
                reopened.stage());
        assertEquals("application/json", reopened.responseContentType());
    }

    /**
     * Some validation devices freezer-suspend an instrumentation-only target
     * while its socket is intentionally quiet. Keep this test-only transport
     * process foreground without changing production code or its deadlines.
     */
    private static void keepTargetForegroundForTransport() throws IOException {
        ParcelFileDescriptor command = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .executeShellCommand("am start -n com.ml.tblandroidtxt/.MainActivity");
        if (command != null) command.close();
        Log.i("P5D_LOCAL_HTTP", "foreground_keepalive");
    }

    @Test public void p5dRecoveryNeedsImmutableDecisionNewAuthAndDuplicateRiskAcknowledgement()
            throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5PilotAuthorization original = authorization(fixture.binding,
                "p5d-auth-original", "L1_RAW_DISCOVERY");
        EditorialP5CAttemptStore first = new EditorialP5CAttemptStore(database);
        first.prepare(rawRequest, original, "c".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                first.claim(rawRequest.attemptIdentity()));
        first.markRecoveryRequired(rawRequest.attemptIdentity(), "RETRY_PROVIDER_READ_TIMEOUT");

        EditorialP5PilotAuthorization retry = authorization(fixture.binding,
                "p5d-auth-retry", "L1_RAW_DISCOVERY");
        EditorialP5CAttemptStore retryStore = new EditorialP5CAttemptStore(database);
        retryStore.prepare(rawRequest, retry, "c".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.RECOVERY_REQUIRED,
                retryStore.claim(rawRequest.attemptIdentity()));

        String retryHash = EditorialP5CAttemptStore.authorizationIdHash(retry.authorizationId());
        retryStore.recordReconciliation(new EditorialP5CAttemptStore.ReconciliationDecision(
                rawRequest.attemptIdentity(),
                EditorialP5CAttemptStore.ExternalStateClassification.EXTERNAL_STATE_REMAINS_UNKNOWN,
                "local/p5d-activity-unknown", "fake-account-fingerprint", "UNKNOWN",
                "P5D_TEST", true, true, retryHash, 2000L));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                retryStore.claim(rawRequest.attemptIdentity()));
        assertEquals("CONSUMED", retryStore.findAuthorizationReceipt(retryHash)
                .orElseThrow().consumptionResult());
        assertEquals("CLAIMED", retryStore.findRecord(rawRequest.attemptIdentity())
                .orElseThrow().status());
        assertEquals(EditorialP5CAttemptStore.ExternalStateClassification.EXTERNAL_STATE_REMAINS_UNKNOWN,
                retryStore.findReconciliation(rawRequest.attemptIdentity()).orElseThrow().classification());
    }

    @Test public void p5eWireContractEvolutionReclaimsOnlyAfterDecision() throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5CAttemptStore firstStore = new EditorialP5CAttemptStore(database);
        firstStore.prepare(rawRequest, authorization(fixture.binding, "p5e-old-envelope",
                "L1_RAW_DISCOVERY"), "c".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                firstStore.claim(rawRequest.attemptIdentity()));
        firstStore.markRecoveryRequired(rawRequest.attemptIdentity(), "RETRY_OUTPUT_TRUNCATED");

        EditorialP5PilotAuthorization retry = authorization(fixture.binding,
                "p5e-new-wire-envelope", "L1_RAW_DISCOVERY");
        String retryHash = EditorialP5CAttemptStore.authorizationIdHash(retry.authorizationId());
        EditorialP5CAttemptStore retryStore = new EditorialP5CAttemptStore(database);
        retryStore.prepare(rawRequest, retry, "d".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.RECOVERY_REQUIRED,
                retryStore.claim(rawRequest.attemptIdentity()));
        retryStore.recordRecoveryDecision(new EditorialP5CAttemptStore.ReconciliationDecision(
                rawRequest.attemptIdentity(),
                EditorialP5CAttemptStore.ExternalStateClassification.EXTERNAL_CONFIRMED_CANCELLED,
                "local/p5e-wire-contract-evolution", "fake-account-fingerprint",
                "CANCELLED_BILLING_UNKNOWN", "P5E_TEST", true, true, retryHash, 2000L));

        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                retryStore.claim(rawRequest.attemptIdentity()));
        // The durable attempt envelope is historical immutable identity. The
        // new compact envelope is authorized in memory and must not rewrite it.
        assertEquals("c".repeat(64), retryStore.findRecord(rawRequest.attemptIdentity())
                .orElseThrow().requestEnvelopeHash());
        assertEquals("CONSUMED", retryStore.findAuthorizationReceipt(retryHash)
                .orElseThrow().consumptionResult());
        retryStore.commit(new EditorialP5PilotResult.CommittedResult(
                rawRequest.attemptIdentity(), rawRequest.requestIdentity(), "p5e-compact-response",
                new byte[]{1}, new byte[]{2}, EditorialP5PilotResult.Metrics.empty(),
                output(rawRequest)));
        assertEquals("COMMITTED", retryStore.findRecord(rawRequest.attemptIdentity())
                .orElseThrow().status());
        assertEquals("c".repeat(64), retryStore.findRecord(rawRequest.attemptIdentity())
                .orElseThrow().requestEnvelopeHash());
    }

    @Test public void p5dRejectsUnallowlistedExceptionMetadata() throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5CAttemptStore store = new EditorialP5CAttemptStore(database);
        EditorialP5PilotAuthorization authorization = authorization(fixture.binding,
                "p5d-auth-exception", "L1_RAW_DISCOVERY");
        store.prepare(rawRequest, authorization, "d".repeat(64));
        store.claim(rawRequest.attemptIdentity());
        store.recordNetworkLifecycle(rawRequest.attemptIdentity(),
                EditorialP5CAttemptStore.NetworkLifecycleEvent.stage(
                        EditorialP5CAttemptStore.LifecycleStage.CALL_CREATED));
        try {
            store.recordNetworkLifecycle(rawRequest.attemptIdentity(),
                    new EditorialP5CAttemptStore.NetworkLifecycleEvent(
                            EditorialP5CAttemptStore.LifecycleStage.CALL_FAILED, 1L, -1,
                            "secret=provider-body", 1L, "", ""));
            throw new AssertionError("unallowlisted exception metadata must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals(EditorialP5CAttemptStore.LifecycleStage.CALL_CREATED,
                    store.findNetworkLifecycle(rawRequest.attemptIdentity()).orElseThrow().stage());
        }
    }

    @Test public void p5dPreservesCancelledAndTruncatedRecoveryDecisionsAsHistory()
            throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialP5PilotRequest rawRequest = request(fixture,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5CAttemptStore store = new EditorialP5CAttemptStore(database);

        EditorialP5PilotAuthorization firstAuthorization = authorization(fixture.binding,
                "p5d-auth-history-first", "L1_RAW_DISCOVERY");
        store.prepare(rawRequest, firstAuthorization, "e".repeat(64));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                store.claim(rawRequest.attemptIdentity()));
        store.markRecoveryRequired(rawRequest.attemptIdentity(), "RETRY_OUTPUT_TRUNCATED");

        EditorialP5PilotAuthorization firstRetry = authorization(fixture.binding,
                "p5d-auth-history-retry-one", "L1_RAW_DISCOVERY");
        String firstRetryHash = EditorialP5CAttemptStore.authorizationIdHash(firstRetry.authorizationId());
        store.prepare(rawRequest, firstRetry, "e".repeat(64));
        store.recordReconciliation(new EditorialP5CAttemptStore.ReconciliationDecision(
                rawRequest.attemptIdentity(),
                EditorialP5CAttemptStore.ExternalStateClassification.EXTERNAL_CONFIRMED_CANCELLED,
                "local/p5d-cancelled-generation", "fake-account-fingerprint", "CANCELLED",
                "P5D_TEST", true, true, firstRetryHash, 2000L));
        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                store.claim(rawRequest.attemptIdentity()));
        store.markRecoveryRequired(rawRequest.attemptIdentity(), "RETRY_OUTPUT_TRUNCATED");

        EditorialP5PilotAuthorization secondRetry = authorization(fixture.binding,
                "p5d-auth-history-retry-two", "L1_RAW_DISCOVERY");
        String secondRetryHash = EditorialP5CAttemptStore.authorizationIdHash(secondRetry.authorizationId());
        store.prepare(rawRequest, secondRetry, "e".repeat(64));
        store.recordRecoveryDecision(new EditorialP5CAttemptStore.ReconciliationDecision(
                rawRequest.attemptIdentity(),
                EditorialP5CAttemptStore.ExternalStateClassification.EXTERNAL_COMPLETED_BILLED_OUTPUT_UNAVAILABLE,
                "local/p5d-truncated-generation", "fake-account-fingerprint", "BILLED_OUTPUT_UNAVAILABLE",
                "P5D_TEST", true, true, secondRetryHash, 3000L));

        assertEquals(EditorialP5PilotExecution.AttemptStore.Claim.ACQUIRED,
                store.claim(rawRequest.attemptIdentity()));
        List<EditorialP5CAttemptStore.ReconciliationRecord> history =
                store.findReconciliationHistory(rawRequest.attemptIdentity());
        assertEquals(2, history.size());
        assertEquals(firstRetryHash, history.get(0).newAuthorizationIdHash());
        assertEquals(secondRetryHash, history.get(1).newAuthorizationIdHash());
        assertEquals(firstRetryHash, store.findReconciliation(rawRequest.attemptIdentity())
                .orElseThrow().newAuthorizationIdHash());
    }

    private BindingFixture createBoundChapter() throws Exception {
        PackFixture pack = readPack(CANONICAL_ASSET);
        assertEquals(CANONICAL_ZIP_SHA256, EditorialCanonicalJson.sha256Hex(pack.zipBytes));
        EditorialPackImportResult imported = new EditorialPackImportService(database, storage,
                new EditorialEngineProfileResolver(BundledEditorialEngineContractProfileRegistry.load()))
                .importZip(new ByteArrayInputStream(pack.zipBytes));
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, imported.state());
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                .resolve(pack.manifest.packId(), pack.manifest.version()).orElseThrow();
        List<EditorialP4InputSource> sources = sourceInputs();
        EditorialP4BindingResult bindingResult = new EditorialP4BindingTransactionService(database, storage)
                .createSetup(new EditorialP4SetupRequest(SELECTOR, "P5C fake", "4.1.3",
                        candidate.packId(), candidate.packVersion(), "p5c/fake", "p5c/scope",
                        sources, EditorialSafe4Contract.NORMAL_MODE, "AVAILABLE", "AVAILABLE", "NONE",
                        "USER_CONFIRMED_NORMAL", "EDITORIAL_PILOT", "L1_SOURCE_PREFLIGHT",
                        "manifest-attestation-v1", EditorialLineageNodeKind.ROOT, null, 1000L));
        assertEquals(EditorialP4BindingResult.Code.APPENDED, bindingResult.code());
        long chapterId = insertChapter(bindingResult.projectId(), sources);
        return new BindingFixture(bindingResult.projectId(), chapterId, bindingResult.binding(),
                pack.manifest, sources);
    }

    private long insertChapter(long projectId, List<EditorialP4InputSource> sources) {
        SQLiteDatabase db = database.editorialWritableDatabase();
        long now = 1001L;
        db.beginTransaction();
        try {
            ContentValues chapter = new ContentValues();
            chapter.put("project_id", projectId);
            chapter.put("chapter_key", CHAPTER_KEY);
            chapter.put("title", "P5C fake chapter");
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
                asset.put("display_name", source.role().toLowerCase() + ".txt");
                asset.put("sha256", EditorialCanonicalJson.sha256Hex(source.bytes()));
                asset.put("size_bytes", source.bytes().length);
                asset.put("content", new String(source.bytes(), StandardCharsets.UTF_8));
                asset.put("created_at", now);
                db.insertOrThrow("editorial_assets", null, asset);
            }
            db.setTransactionSuccessful();
            return chapterId;
        } finally {
            db.endTransaction();
        }
    }

    private List<EditorialP4InputSource> sourceInputs() {
        return List.of(
                input("RAW", "raw", "raw chapter bytes"),
                input("DRAFT", "draft", "original draft bytes"),
                input("GLOSSARY", "glossary", "term\ttarget\tPOS\tnote"),
                input("PRONOUN", "pronoun", "from\ttarget\tgender\tnumber"));
    }

    private static EditorialP4InputSource input(String role, String id, String text) {
        return new EditorialP4InputSource(role, "content://p5c/" + id,
                text.getBytes(StandardCharsets.UTF_8), "UTF-8", "VALID", 0L);
    }

    private EditorialP5PilotRequest request(BindingFixture fixture,
                                             EditorialP5PilotRequest.Phase phase,
                                             String predecessor) {
        EnumMap<EditorialPackFileRole, byte[]> authority = new EnumMap<>(EditorialPackFileRole.class);
        for (EditorialPackManifest.FileEntry file : fixture.manifest.fileEntries()) {
            try {
                authority.put(file.role(), Files.readAllBytes(
                        storage.immutableEntry(fixture.binding.canonicalPackHash(), file.path())));
            } catch (IOException error) {
                throw new AssertionError(error);
            }
        }
        List<EditorialP5PilotRequest.SourceBytes> sources = new ArrayList<>();
        for (EditorialP4InputSource source : fixture.sources) {
            sources.add(new EditorialP5PilotRequest.SourceBytes(source.role(), source.sourceReference(),
                    source.bytes(), source.encoding(), schemaId(source.role()), source.schemaStatus(),
                    source.ordinal()));
        }
        return new EditorialP5PilotRequest(fixture.binding, fixture.manifest,
                new EditorialP5PilotRequest.PackAuthority(authority), CHAPTER_KEY, phase, sources,
                predecessor, List.of("chapter:" + CHAPTER_KEY), List.of("population:" + CHAPTER_KEY),
                true, 256);
    }

    private static EditorialP5PilotAuthorization authorization(EditorialP4Binding binding,
                                                                 String id, String phase) {
        return authorization(binding, id, phase, 60_000L);
    }

    private static EditorialP5PilotAuthorization authorization(EditorialP4Binding binding,
                                                                 String id, String phase,
                                                                 long executionMillis) {
        return new EditorialP5PilotAuthorization(id, binding.bindingIdentity(),
                binding.runDeclarationIdentity(), binding.canonicalPackHash(),
                binding.canonicalProfileHash(), binding.compatibilityEvaluationId(), CHAPTER_KEY,
                phase, "FAKE_PROVIDER", "fake/model", "fake-account-fingerprint", 1, 1, 0,
                200_000, 2_000, 200_000, BigDecimal.ONE, executionMillis, true, false, false,
                "HASH_ONLY", "TEST_STOP_AUTHORITY", 0L, Long.MAX_VALUE, true);
    }

    private static EditorialP5L1Output output(EditorialP5PilotRequest request) {
        EditorialLedgerValidator.Request ledger = new EditorialLedgerValidator.Request(
                request.populationIds(), List.of(new EditorialLedgerValidator.Entry(
                        request.populationIds().get(0), "PROCESSED", List.of("fake-evidence"), false)));
        Map<String, String> gates = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) gates.put(gate, "PASS");
        return new EditorialP5L1Output("safe4.full.report-l1.v1",
                EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION, request.binding().bindingIdentity(),
                request.manifestFingerprint(), CHAPTER_KEY, "L1", request.bundleIdentity(),
                request.predecessorIdentity(), request.stableAnchors(), ledger, gates,
                List.of(), List.of(), "draft", "draft", 0,
                EditorialStopDecision.continueWithoutStop("L1", "COVERAGE", "FAKE_LOCAL_VALIDATED"),
                Set.of("fake-evidence"), true);
    }

    private static EditorialP5PilotProvider.Request transportRequest(EditorialP5PilotRequest raw) {
        Map<String, byte[]> visible = new HashMap<>();
        for (EditorialP5PilotRequest.SourceBytes source : raw.sources()) {
            if ("RAW".equals(source.role()) || "GLOSSARY".equals(source.role())) {
                visible.put(source.role(), source.bytes());
            }
        }
        return new EditorialP5PilotProvider.Request(raw.attemptIdentity(),
                EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC, "openrouter",
                "openai/gpt-5.6-luna", raw.phase(), "d".repeat(64), visible,
                raw.authority(), "safe4.full.report-l1.v1", CHAPTER_KEY, "",
                new EditorialP5PilotProvider.Request.Context(raw.binding().bindingIdentity(),
                        raw.binding().runDeclarationIdentity(), raw.manifestFingerprint(),
                        raw.bundleIdentity(), raw.predecessorIdentity(), raw.stableAnchors(),
                        raw.populationIds()));
    }

    private static String schemaId(String role) {
        return EditorialSafe4Contract.GLOSSARY.equals(role) ? "safe4.full.glossary.v1"
                : EditorialSafe4Contract.PRONOUN.equals(role) ? "safe4.full.pronoun.v1" : "text.v1";
    }

    private PackFixture readPack(String assetName) throws IOException {
        byte[] zipBytes;
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open(assetName)) {
            zipBytes = input.readAllBytes();
        }
        byte[] manifestBytes = null;
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] bytes = input.readAllBytes();
                if ("editorial-pack.json".equals(entry.getName())) manifestBytes = bytes;
            }
        }
        if (manifestBytes == null) throw new IOException("canonical manifest missing");
        return new PackFixture(zipBytes, EditorialPackManifest.parse(manifestBytes));
    }

    private boolean tableExists(String table) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[]{table})) {
            return cursor.moveToFirst();
        }
    }

    private boolean columnExists(String table, String column) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "PRAGMA table_info(" + table + ")", null)) {
            while (cursor.moveToNext()) if (column.equals(cursor.getString(1))) return true;
            return false;
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (IOException error) { throw new RuntimeException(error); }
            });
        }
    }

    private record PackFixture(byte[] zipBytes, EditorialPackManifest manifest) {
        PackFixture { zipBytes = zipBytes.clone(); }
    }

    private record BindingFixture(long projectId, long chapterId, EditorialP4Binding binding,
                                  EditorialPackManifest manifest,
                                  List<EditorialP4InputSource> sources) { }

    private static final class FakeProvider implements EditorialP5PilotProvider {
        private final Map<String, EditorialP5L1Output> outputs;
        private final List<Request> requests = new ArrayList<>();
        private int calls;

        FakeProvider(Map<String, EditorialP5L1Output> outputs) {
            this.outputs = new HashMap<>(outputs);
        }

        @Override public Response call(Request request) {
            calls++;
            requests.add(request);
            EditorialP5L1Output output = outputs.get(request.phase());
            if (output == null) throw new AssertionError("unexpected fake-provider call");
            return new Response("fake-response-" + calls,
                    ("redacted-response-" + calls).getBytes(StandardCharsets.UTF_8), "stop",
                    true, 40, 20, 60, BigDecimal.ZERO, output, true);
        }
    }

    private static final class CountingProvider implements EditorialP5PilotProvider {
        private int calls;

        @Override public Response call(Request request) {
            calls++;
            throw new AssertionError("provider must not be called");
        }
    }

    /** Local, content-free OpenAI-compatible transport used only for P5D cancellation tests. */
    private static final class StalledBodyServer implements AutoCloseable {
        private final ServerSocket server;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final CountDownLatch requestReceived = new CountDownLatch(1);
        private final AtomicInteger requests = new AtomicInteger();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicReference<Socket> accepted = new AtomicReference<>();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private volatile Future<?> future;

        StalledBodyServer() throws IOException {
            server = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"));
            server.setSoTimeout(2_000);
            future = executor.submit(this::serve);
        }

        int port() { return server.getLocalPort(); }

        boolean awaitRequest(long timeout, TimeUnit unit) throws InterruptedException {
            return requestReceived.await(timeout, unit) && failure.get() == null;
        }

        int requestCount() { return requests.get(); }

        private void serve() {
            Socket socket = null;
            try {
                socket = server.accept();
                accepted.set(socket);
                socket.setSoTimeout(2_000);
                requests.incrementAndGet();
                readRequest(socket);
                requestReceived.countDown();
                OutputStream output = socket.getOutputStream();
                byte[] header = ("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n"
                        + "Content-Length: 65536\r\nConnection: close\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII);
                output.write(header);
                output.write("{\"".getBytes(StandardCharsets.UTF_8));
                output.flush();
                for (int i = 0; i < 65534; i++) {
                    Thread.sleep(100L);
                    output.write(' ');
                    output.flush();
                }
            } catch (Throwable thrown) {
                if (!closed.get()) failure.compareAndSet(null, thrown);
            } finally {
                requestReceived.countDown();
                if (socket != null) {
                    accepted.compareAndSet(socket, null);
                    try { socket.close(); } catch (IOException ignored) { }
                }
            }
        }

        private static void readRequest(Socket socket) throws IOException {
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            ByteArrayOutputStream headers = new ByteArrayOutputStream();
            int previous = -1;
            int current;
            int contentLength = 0;
            while ((current = input.read()) != -1) {
                headers.write(current);
                if (headers.size() > 32 * 1024) throw new IOException("request headers too large");
                String text = headers.toString(StandardCharsets.US_ASCII);
                if (previous == '\r' && current == '\n' && text.endsWith("\r\n\r\n")) {
                    for (String line : text.split("\\r?\\n")) {
                        if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) {
                            contentLength = Integer.parseInt(line.substring(15).trim());
                        }
                    }
                    break;
                }
                previous = current;
            }
            while (contentLength-- > 0) {
                if (input.read() < 0) throw new IOException("request body truncated");
            }
        }

        String failureSummary() {
            Throwable thrown = failure.get();
            return thrown == null ? "none" : thrown.getClass().getSimpleName();
        }

        void captureThreadStacks(String reason) {
            Log.i("P5D_LOCAL_HTTP", "thread_stacks=" + reason);
            for (Map.Entry<Thread, StackTraceElement[]> entry
                    : Thread.getAllStackTraces().entrySet()) {
                Thread thread = entry.getKey();
                StringBuilder line = new StringBuilder("thread=")
                        .append(thread.getName()).append(" state=").append(thread.getState());
                StackTraceElement[] stack = entry.getValue();
                for (int i = 0; i < Math.min(stack.length, 6); i++) {
                    line.append(' ').append(stack[i].getClassName()).append('#')
                            .append(stack[i].getMethodName());
                }
                Log.i("P5D_LOCAL_HTTP", line.toString());
            }
        }

        @Override public void close() throws Exception {
            closed.set(true);
            try { server.close(); } catch (IOException ignored) { }
            Socket socket = accepted.getAndSet(null);
            if (socket != null) try { socket.close(); } catch (IOException ignored) { }
            Future<?> running = future;
            if (running != null) running.cancel(true);
            executor.shutdownNow();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                throw new AssertionError("stalled body server cleanup incomplete");
            }
        }
    }

    /** Local, content-free OpenAI-compatible transport used only for P5D cancellation tests. */
    private static final class SlowJsonServer implements AutoCloseable {
        private final ServerSocket server;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final ScheduledExecutorService delayExecutor =
                Executors.newSingleThreadScheduledExecutor();
        private final CountDownLatch requestReceived = new CountDownLatch(1);
        private final CountDownLatch requestComplete = new CountDownLatch(1);
        private final CountDownLatch serverComplete = new CountDownLatch(1);
        private final AtomicInteger requests = new AtomicInteger();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicReference<Socket> acceptedSocket = new AtomicReference<>();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final long delayMillis;
        private volatile Future<?> serverFuture;
        private volatile ScheduledFuture<?> delayFuture;

        SlowJsonServer(long delayMillis) throws IOException {
            this.server = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"));
            this.server.setSoTimeout(5_000);
            this.delayMillis = delayMillis;
            this.serverFuture = executor.submit(this::serveOne);
        }

        int port() { return server.getLocalPort(); }
        int requestCount() { return requests.get(); }
        boolean awaitRequest(long timeout, TimeUnit unit) throws InterruptedException {
            boolean received = requestReceived.await(timeout, unit);
            return received && failure.get() == null;
        }

        private void serveOne() {
            Socket socket = null;
            try {
                socket = server.accept();
                socket.setSoTimeout(5_000);
                acceptedSocket.set(socket);
                requests.incrementAndGet();
                mark("SERVER_ACCEPTED");
                readRequest(socket);
                requestReceived.countDown();
                mark("DELAY_STARTED");
                delayFuture = delayExecutor.schedule(() -> {
                    mark("DELAY_FINISHED");
                    requestComplete.countDown();
                }, delayMillis, TimeUnit.MILLISECONDS);
                if (!requestComplete.await(delayMillis + 5_000L, TimeUnit.MILLISECONDS)) {
                    throw new IOException("local response delay exceeded bounded wait");
                }
                byte[] body = ("{\"id\":\"provider-local-test\",\"choices\":[{\"message\":{\"content\":\"{}\"},\"finish_reason\":\"stop\"}],"
                        + "\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}")
                        .getBytes(StandardCharsets.UTF_8);
                OutputStream output = socket.getOutputStream();
                mark("RESPONSE_WRITE_STARTED");
                output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\n"
                        + "X-Generation-Id: generation-local-test\r\nContent-Length: " + body.length
                        + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                output.write(body);
                output.flush();
                mark("RESPONSE_FLUSHED");
            } catch (Throwable thrown) {
                if (!closed.get()) {
                    failure.compareAndSet(null, thrown);
                    Log.i("P5D_LOCAL_HTTP", "server_failure="
                            + thrown.getClass().getSimpleName());
                }
            } finally {
                requestReceived.countDown();
                requestComplete.countDown();
                serverComplete.countDown();
                if (socket != null) {
                    acceptedSocket.compareAndSet(socket, null);
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                        // Cleanup reports the server failure/timeout if this is material.
                    }
                }
            }
        }

        private void readRequest(Socket socket) throws IOException {
            BufferedInputStream buffered = new BufferedInputStream(socket.getInputStream());
            ByteArrayOutputStream headers = new ByteArrayOutputStream();
            int a = -1, b = -1, c = -1, current;
            while ((current = buffered.read()) != -1) {
                headers.write(current);
                if (headers.size() > 32 * 1024) throw new IOException("local request headers too large");
                if (a == '\r' && b == '\n' && c == '\r' && current == '\n') {
                    mark("REQUEST_HEADERS_COMPLETE");
                    String text = headers.toString(StandardCharsets.US_ASCII);
                    int contentLength = 0;
                    for (String line : text.split("\\r?\\n")) {
                        if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) {
                            contentLength = Integer.parseInt(line.substring(15).trim());
                        }
                    }
                    byte[] discard = new byte[8 * 1024];
                    while (contentLength > 0) {
                        int read = buffered.read(discard, 0, Math.min(discard.length, contentLength));
                        if (read < 0) throw new IOException("local request ended before body");
                        contentLength -= read;
                    }
                    mark("REQUEST_BODY_COMPLETE");
                    return;
                }
                a = b;
                b = c;
                c = current;
            }
            throw new IOException("local request ended before headers");
        }

        void awaitComplete(long timeout, TimeUnit unit) throws Exception {
            if (!serverComplete.await(timeout, unit)) {
                captureThreadStacks("server_deadline");
                throw new AssertionError("local server did not complete: " + failureSummary());
            }
            Throwable thrown = failure.get();
            if (thrown != null) {
                throw new AssertionError("local server failed: " + failureSummary(), thrown);
            }
            Future<?> future = serverFuture;
            if (future != null) future.get(1, TimeUnit.SECONDS);
        }

        String failureSummary() {
            Throwable thrown = failure.get();
            return thrown == null ? "none" : thrown.getClass().getSimpleName();
        }

        void captureThreadStacks(String reason) {
            Log.i("P5D_LOCAL_HTTP", "thread_stacks=" + reason);
            for (Map.Entry<Thread, StackTraceElement[]> entry
                    : Thread.getAllStackTraces().entrySet()) {
                Thread thread = entry.getKey();
                StringBuilder line = new StringBuilder("thread=")
                        .append(thread.getName()).append(" state=").append(thread.getState());
                StackTraceElement[] stack = entry.getValue();
                for (int i = 0; i < Math.min(stack.length, 6); i++) {
                    line.append(" ").append(stack[i].getClassName()).append("#")
                            .append(stack[i].getMethodName());
                }
                Log.i("P5D_LOCAL_HTTP", line.toString());
            }
        }

        private static void mark(String marker) {
            Log.i("P5D_LOCAL_HTTP", "timeline=" + marker);
        }

        @Override public void close() throws Exception {
            closed.set(true);
            IOException closeFailure = null;
            try {
                server.close();
            } catch (IOException thrown) {
                closeFailure = thrown;
            }
            Socket socket = acceptedSocket.getAndSet(null);
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException thrown) {
                    if (closeFailure == null) closeFailure = thrown;
                }
            }
            ScheduledFuture<?> delayed = delayFuture;
            if (delayed != null) delayed.cancel(true);
            Future<?> future = serverFuture;
            if (future != null && !future.isDone()) future.cancel(true);
            executor.shutdownNow();
            delayExecutor.shutdownNow();
            boolean serverStopped = executor.awaitTermination(5, TimeUnit.SECONDS);
            boolean delayStopped = delayExecutor.awaitTermination(5, TimeUnit.SECONDS);
            if (!serverStopped || !delayStopped) {
                throw new AssertionError("local server cleanup incomplete server="
                        + serverStopped + " delay=" + delayStopped);
            }
            Log.i("P5D_LOCAL_HTTP", "cleanup_finished");
            if (closeFailure != null) throw closeFailure;
        }
    }
}
