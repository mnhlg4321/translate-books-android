package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Exercises the production-owned P5E lineage query only on disposable v24
 * databases. No test in this class opens the current pilot DB.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5EFreshRawLineageInstrumentedTest {
    private static final String CANONICAL_ASSET =
            "editorial-p2/v5-safe-4.1.3-full-canonical.zip";
    private static final String OTHER_BINDING_SELECTOR_PREFIX = "p5e-lq-other-";
    private static final String ATTEMPT_ID = "a".repeat(64);
    private static final String OTHER_ATTEMPT_ID = "b".repeat(64);
    private static final String REQUEST_ID = "c".repeat(64);
    private static final String PREDECESSOR_ID = "d".repeat(64);
    private static final String ENVELOPE_HASH = "e".repeat(64);
    private static final String AUTHORIZATION_HASH = "f".repeat(64);
    private static final String DECISION_ID = "1".repeat(64);
    private static final String OTHER_AUTHORIZATION_HASH = "2".repeat(64);
    private static final String OTHER_DECISION_ID = "3".repeat(64);
    private static final String HISTORICAL_EVALUATION =
            "f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1";

    private Context context;
    private String databaseName;
    private Path storageRoot;
    private TranslationRepository database;
    private Fixture fixture;

    @Before public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "p5e-lq-" + UUID.randomUUID() + ".db";
        storageRoot = context.getCacheDir().toPath()
                .resolve("p5e-lq-" + UUID.randomUUID());
        database = new TranslationRepository(context, databaseName);
        fixture = createFixture("p5e-lq-primary-" + UUID.randomUUID());
    }

    @After public void tearDown() throws Exception {
        if (database != null) database.close();
        if (context != null && databaseName != null) context.deleteDatabase(databaseName);
        deleteTree(storageRoot);
    }

    @Test public void schemaV24EmptyFreshLineageIsUnusedAndAllReadsComplete() {
        assertEquals(24, database.editorialReadableDatabase().getVersion());
        EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check = inspectPrimary();

        assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.UNUSED,
                check.status());
        assertZero(check);
    }

    @Test public void historicalPredicateIsRedOnSchemaV24WithoutBindingColumn() {
        SQLiteDatabase db = database.editorialReadableDatabase();
        assertFalse(hasColumn(db, "editorial_p5d_reconciliation", "binding_identity"));
        try (android.database.Cursor ignored = db.rawQuery(
                "SELECT COUNT(*) FROM editorial_p5d_reconciliation WHERE binding_identity=?",
                new String[]{fixture.binding().bindingIdentity()})) {
            fail("the historical predicate must not execute on schema v24");
        } catch (SQLiteException expected) {
            assertTrue(expected.getMessage().contains("binding_identity"));
        }
    }

    @Test public void attemptForBindingMakesLineageAlreadyUsed() {
        seedAttempt(fixture.binding(), ATTEMPT_ID);

        EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check = inspectPrimary();
        assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.ALREADY_USED,
                check.status());
        assertEquals(1L, check.attempts());
        assertEquals(0L, check.authorizationReceipts());
        assertEquals(0L, check.reconciliation());
        assertEquals(0L, check.reconciliationHistory());
        assertEquals(0L, check.lifecycle());
    }

    @Test public void authorizationReceiptForBindingMakesLineageAlreadyUsed() {
        seedAttempt(fixture.binding(), ATTEMPT_ID);
        seedAuthorizationReceipt(fixture.binding(), ATTEMPT_ID, AUTHORIZATION_HASH);

        EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check = inspectPrimary();
        assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.ALREADY_USED,
                check.status());
        assertEquals(1L, check.attempts());
        assertEquals(1L, check.authorizationReceipts());
    }

    @Test public void primaryReconciliationUsesAttemptIdentityJoin() {
        seedAttempt(fixture.binding(), ATTEMPT_ID);
        seedReconciliation(ATTEMPT_ID);

        EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check = inspectPrimary();
        assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.ALREADY_USED,
                check.status());
        assertEquals(1L, check.reconciliation());
        assertEquals(0L, check.reconciliationHistory());
    }

    @Test public void reconciliationHistoryUsesAttemptIdentityJoin() {
        seedAttempt(fixture.binding(), ATTEMPT_ID);
        seedHistory(ATTEMPT_ID, DECISION_ID);

        EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check = inspectPrimary();
        assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.ALREADY_USED,
                check.status());
        assertEquals(1L, check.reconciliationHistory());
    }

    @Test public void lifecycleUsesAttemptIdentityJoin() {
        seedAttempt(fixture.binding(), ATTEMPT_ID);
        seedLifecycle(ATTEMPT_ID);

        EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check = inspectPrimary();
        assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.ALREADY_USED,
                check.status());
        assertEquals(1L, check.lifecycle());
    }

    @Test public void evidenceForOtherBindingDoesNotConsumeFreshBinding() throws Exception {
        EditorialP4Binding other = createAdditionalBinding(
                OTHER_BINDING_SELECTOR_PREFIX + UUID.randomUUID());
        seedAttempt(other, OTHER_ATTEMPT_ID);
        seedAuthorizationReceipt(other, OTHER_ATTEMPT_ID, OTHER_AUTHORIZATION_HASH);
        seedReconciliation(OTHER_ATTEMPT_ID);
        seedHistory(OTHER_ATTEMPT_ID, OTHER_DECISION_ID);
        seedLifecycle(OTHER_ATTEMPT_ID);

        EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check = inspectPrimary();
        assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.UNUSED,
                check.status());
        assertZero(check);
    }

    @Test public void missingTableReturnsTypedCheckFailure() {
        database.editorialWritableDatabase().execSQL(
                "DROP TABLE editorial_p5d_reconciliation");

        EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check = inspectPrimary();
        assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.CHECK_FAILED,
                check.status());
    }

    @Test public void missingAttemptIdentityColumnReturnsTypedCheckFailure() {
        SQLiteDatabase db = database.editorialWritableDatabase();
        db.execSQL("DROP TABLE editorial_p5d_reconciliation");
        db.execSQL("CREATE TABLE editorial_p5d_reconciliation (binding_identity TEXT)");

        EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check = inspectPrimary();
        assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.CHECK_FAILED,
                check.status());
    }

    @Test public void canonicalChapterKeyIsAcceptedAndFriendlyAliasIsRejectedBeforeProvider() {
        EditorialP5EFreshRawLiveRunner runner = new EditorialP5EFreshRawLiveRunner(
                database, new EditorialPackStorageLayout(storageRoot));
        AppSettings settings = routeSettings();

        EditorialP5CExactBindingExecution.Result canonical = runner.dispatchRaw(
                fixture.projectId(), EditorialP5EFreshRawLiveRunner.SELECTOR, "001", null, settings);
        assertEquals(EditorialP5CExactBindingExecution.Status.STOP, canonical.status());
        assertEquals("P5E_FRESH_RAW_AUTHORIZATION_REQUIRED", canonical.reasonCode());
        assertEquals(0, canonical.providerCalls());

        EditorialP5CExactBindingExecution.Result friendly = runner.dispatchRaw(
                fixture.projectId(), EditorialP5EFreshRawLiveRunner.SELECTOR, "chapter001",
                null, settings);
        assertEquals(EditorialP5CExactBindingExecution.Status.STOP, friendly.status());
        assertEquals("P5E_FRESH_RAW_SELECTOR_OR_CHAPTER_MISMATCH", friendly.reasonCode());
        assertEquals(0, friendly.providerCalls());
        assertZero(inspectPrimary());
    }

    @Test public void nullAndWrongAuthorizationRemainTypedAndMutationFree() {
        EditorialP5EFreshRawLiveRunner runner = new EditorialP5EFreshRawLiveRunner(
                database, new EditorialPackStorageLayout(storageRoot));
        AppSettings settings = routeSettings();

        EditorialP5CExactBindingExecution.Result missing = runner.dispatchRaw(
                fixture.projectId(), EditorialP5EFreshRawLiveRunner.SELECTOR, "001", null, settings);
        assertEquals("P5E_FRESH_RAW_AUTHORIZATION_REQUIRED", missing.reasonCode());
        assertEquals(0, missing.providerCalls());

        EditorialP5CExactBindingExecution.Result wrong = runner.dispatchRaw(
                fixture.projectId(), EditorialP5EFreshRawLiveRunner.SELECTOR, "001",
                authorization(HISTORICAL_EVALUATION), settings);
        assertEquals("P5E_FRESH_RAW_AUTHORIZATION_MISMATCH", wrong.reasonCode());
        assertEquals(0, wrong.providerCalls());
        assertZero(inspectPrimary());
    }

    @Test public void schemaFailureReturnsCheckFailedAndDoesNotMutate() {
        database.editorialWritableDatabase().execSQL(
                "DROP TABLE editorial_p5d_reconciliation");
        EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check = inspectPrimary();
        assertEquals(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck.Status.CHECK_FAILED,
                check.status());
        assertEquals(0L, count("editorial_p5c_attempts"));
        assertEquals(0L, count("editorial_p5d_authorization_receipts"));
    }

    private EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck inspectPrimary() {
        return EditorialP5EFreshRawLiveRunner.inspectLineage(database,
                fixture.binding().bindingIdentity());
    }

    private void assertZero(EditorialP5EFreshRawLiveRunner.FreshRawLineageCheck check) {
        assertEquals(0L, check.attempts());
        assertEquals(0L, check.authorizationReceipts());
        assertEquals(0L, check.reconciliation());
        assertEquals(0L, check.reconciliationHistory());
        assertEquals(0L, check.lifecycle());
    }

    private Fixture createFixture(String selector) throws Exception {
        EditorialPackStorageLayout storage = new EditorialPackStorageLayout(storageRoot);
        byte[] zip;
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open(CANONICAL_ASSET)) {
            zip = input.readAllBytes();
        }
        EditorialPackImportResult imported = new EditorialPackImportService(
                database, storage, new EditorialEngineProfileResolver(
                BundledEditorialEngineContractProfileRegistry.load()))
                .importZip(new ByteArrayInputStream(zip));
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, imported.state());
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(
                database, storage).resolve(imported.packId(), imported.version()).orElseThrow();
        List<EditorialP4InputSource> sources = sources(selector);
        EditorialP4BindingResult result = new EditorialP4BindingTransactionService(
                database, storage).createSetup(new EditorialP4SetupRequest(
                selector, "P5E LQ", "Schema v24", candidate.packId(), candidate.packVersion(),
                "p5e-lq/project/" + selector, "p5e-lq/scope/" + selector, sources,
                EditorialSafe4Contract.NORMAL_MODE, "AVAILABLE", "AVAILABLE", "NONE",
                "USER_CONFIRMED_P5E_LQ", "EDITORIAL_PILOT", "L1_SOURCE_PREFLIGHT",
                "manifest-attestation-v1", EditorialLineageNodeKind.ROOT, null,
                System.currentTimeMillis()));
        assertEquals(EditorialP4BindingResult.Code.APPENDED, result.code());
        return new Fixture(result.projectId(), result.binding(), imported.packId(),
                imported.version(), sources);
    }

    private EditorialP4Binding createAdditionalBinding(String selector) {
        EditorialPackStorageLayout storage = new EditorialPackStorageLayout(storageRoot);
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(
                database, storage).resolve(fixture.packId(), fixture.packVersion()).orElseThrow();
        List<EditorialP4InputSource> sources = sources(selector);
        EditorialP4BindingResult result = new EditorialP4BindingTransactionService(
                database, storage).createSetup(new EditorialP4SetupRequest(
                selector, "P5E LQ other", "Schema v24", candidate.packId(), candidate.packVersion(),
                "p5e-lq/project/" + selector, "p5e-lq/scope/" + selector, sources,
                EditorialSafe4Contract.NORMAL_MODE, "AVAILABLE", "AVAILABLE", "NONE",
                "USER_CONFIRMED_P5E_LQ", "EDITORIAL_PILOT", "L1_SOURCE_PREFLIGHT",
                "manifest-attestation-v1", EditorialLineageNodeKind.ROOT, null,
                System.currentTimeMillis()));
        assertEquals(EditorialP4BindingResult.Code.APPENDED, result.code());
        return result.binding();
    }

    private List<EditorialP4InputSource> sources(String selector) {
        return List.of(
                source("RAW", selector + "/raw", "raw fixture"),
                source("DRAFT", selector + "/draft", "draft fixture"),
                source("GLOSSARY", selector + "/glossary", "term\ttarget"),
                source("PRONOUN", selector + "/pronoun", "from\ttarget"));
    }

    private static EditorialP4InputSource source(String role, String reference, String text) {
        return new EditorialP4InputSource(role, "p5e-lq://" + reference,
                text.getBytes(java.nio.charset.StandardCharsets.UTF_8), "UTF-8", "VALID", 0L);
    }

    private void seedAttempt(EditorialP4Binding binding, String attemptIdentity) {
        ContentValues row = new ContentValues();
        row.put("attempt_identity", attemptIdentity);
        row.put("request_identity", REQUEST_ID);
        row.put("binding_identity", binding.bindingIdentity());
        row.put("run_declaration_identity", binding.runDeclarationIdentity());
        row.put("chapter_key", "001");
        row.put("phase", "L1_RAW_DISCOVERY");
        row.put("predecessor_identity", PREDECESSOR_ID);
        row.put("request_envelope_hash", ENVELOPE_HASH);
        row.put("provider", "fake-provider");
        row.put("model", "fake/model");
        row.put("status", "CLAIMED");
        row.put("response_identity", "");
        row.putNull("report_bytes");
        row.putNull("receipt_bytes");
        row.put("metrics_json", "");
        row.put("recovery_reason_code", "");
        row.put("created_at", 1L);
        row.put("updated_at", 1L);
        database.editorialWritableDatabase().insertOrThrow(
                "editorial_p5c_attempts", null, row);
    }

    private void seedAuthorizationReceipt(EditorialP4Binding binding, String attemptIdentity,
                                          String authorizationHash) {
        ContentValues row = new ContentValues();
        row.put("authorization_id_hash", authorizationHash);
        row.put("exact_phase", "L1_RAW_DISCOVERY");
        row.put("attempt_identity", attemptIdentity);
        row.put("request_identity", REQUEST_ID);
        row.put("binding_identity", binding.bindingIdentity());
        row.put("run_declaration_identity", binding.runDeclarationIdentity());
        row.put("chapter_key", "001");
        row.put("provider", "fake-provider");
        row.put("model", "fake/model");
        row.put("endpoint_account_fingerprint", "fake-account");
        row.put("issued_at", 1L);
        row.put("expires_at", 2L);
        row.put("maximum_primary_calls", 1);
        row.put("maximum_schema_repair_calls", 0);
        row.put("maximum_network_retries", 0);
        row.put("maximum_input_tokens", 1);
        row.put("maximum_output_tokens", 1);
        row.put("maximum_total_tokens", 1);
        row.put("maximum_total_cost", "1");
        row.put("maximum_execution_time_ms", 1L);
        row.put("consumed_at", 1L);
        row.put("consumption_result", "CONSUMED");
        row.put("consumed_attempt_identity", attemptIdentity);
        row.put("created_at", 1L);
        database.editorialWritableDatabase().insertOrThrow(
                "editorial_p5d_authorization_receipts", null, row);
    }

    private void seedReconciliation(String attemptIdentity) {
        ContentValues row = new ContentValues();
        row.put("attempt_identity", attemptIdentity);
        row.put("classification", "EXTERNAL_CONFIRMED_FAILED");
        row.put("evidence_ref", "isolated-evidence");
        row.put("endpoint_account_fingerprint", "fake-account");
        row.put("billing_state", "UNKNOWN");
        row.put("decided_by", "isolated-test");
        row.put("retry_eligible", 0);
        row.put("duplicate_risk_acknowledged", 1);
        row.put("new_authorization_id_hash", "");
        row.put("decided_at", 1L);
        database.editorialWritableDatabase().insertOrThrow(
                "editorial_p5d_reconciliation", null, row);
    }

    private void seedHistory(String attemptIdentity, String decisionIdentity) {
        ContentValues row = new ContentValues();
        row.put("decision_identity", decisionIdentity);
        row.put("attempt_identity", attemptIdentity);
        row.put("classification", "EXTERNAL_CONFIRMED_FAILED");
        row.put("evidence_ref", "isolated-history");
        row.put("endpoint_account_fingerprint", "fake-account");
        row.put("billing_state", "UNKNOWN");
        row.put("decided_by", "isolated-test");
        row.put("retry_eligible", 0);
        row.put("duplicate_risk_acknowledged", 1);
        row.put("new_authorization_id_hash", "");
        row.put("decided_at", 1L);
        database.editorialWritableDatabase().insertOrThrow(
                "editorial_p5d_reconciliation_history", null, row);
    }

    private void seedLifecycle(String attemptIdentity) {
        ContentValues row = new ContentValues();
        row.put("attempt_identity", attemptIdentity);
        row.put("stage", "CALL_CREATED");
        row.put("request_body_bytes", 0L);
        row.put("http_status", -1);
        row.put("exception_class", "");
        row.put("elapsed_ms", 0L);
        row.put("generation_id", "");
        row.put("provider_response_id", "");
        row.put("response_content_type", "");
        row.put("cancellation_source", "");
        row.put("response_body_bytes", 0L);
        row.put("updated_at", 1L);
        database.editorialWritableDatabase().insertOrThrow(
                "editorial_p5d_network_lifecycle", null, row);
    }

    private AppSettings routeSettings() {
        AppSettings settings = new AppSettings();
        settings.provider = EditorialP5EFreshRawRoutingPolicy.PROVIDER;
        settings.model = EditorialP5EFreshRawRoutingPolicy.MODEL;
        settings.baseUrl = AppSettings.defaultBaseUrl(
                EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        settings.apiKey = "";
        return settings;
    }

    private static EditorialP5PilotAuthorization authorization(String evaluationId) {
        return new EditorialP5PilotAuthorization("p5e-lq-auth",
                EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY,
                EditorialP5EFreshRawLiveRunner.RUN_DECLARATION_IDENTITY,
                EditorialP5EFreshRawLiveRunner.CANONICAL_PACK_HASH,
                EditorialP5EFreshRawLiveRunner.CANONICAL_PROFILE_HASH, evaluationId, "001",
                "L1_RAW_DISCOVERY", EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                EditorialP5EFreshRawRoutingPolicy.MODEL, "test-account-fingerprint", 1, 0, 0,
                40_000, EditorialP5RawWireContract.OUTPUT_TOKEN_CAP, 44_096, BigDecimal.ONE,
                60_000L, true, false, false, "redacted", "isolated-test", 1_000L,
                61_000L, true);
    }

    private long count(String table) {
        try (android.database.Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private static boolean hasColumn(SQLiteDatabase db, String table, String expectedColumn) {
        try (android.database.Cursor cursor = db.rawQuery(
                "PRAGMA table_info(" + table + ")", null)) {
            int nameColumn = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (expectedColumn.equals(cursor.getString(nameColumn))) return true;
            }
            return false;
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException error) {
                    throw new RuntimeException(error);
                }
            });
        }
    }

    private record Fixture(long projectId, EditorialP4Binding binding, String packId,
                           String packVersion, List<EditorialP4InputSource> sources) { }
}
