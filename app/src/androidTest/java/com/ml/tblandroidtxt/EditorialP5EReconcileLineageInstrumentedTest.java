package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Disposable-database coverage of EditorialP5EFreshRawLiveRunner.inspectReconcileLineage.
 * Foreign keys are switched OFF on this test connection so rows can be inserted
 * without the full binding/run-declaration parent chain; only the lineage query is under test.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5EReconcileLineageInstrumentedTest {
    private static final String BINDING = EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY;
    private static final String RUN = EditorialP5EFreshRawLiveRunner.RUN_DECLARATION_IDENTITY;
    private static final String RAW_ATTEMPT =
            EditorialP5EFreshRawLiveRunner.RAW_PREDECESSOR_ATTEMPT_IDENTITY;
    private static final String RECONCILE_ATTEMPT = "9".repeat(64);
    private static final String REQUEST_ID = "c".repeat(64);
    private static final String PREDECESSOR_ID = "d".repeat(64);
    private static final String ENVELOPE_HASH = "e".repeat(64);
    private static final String AUTHORIZATION_HASH = "f".repeat(64);

    private Context context;
    private String databaseName;
    private TranslationRepository database;
    private SQLiteDatabase db;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "p5e-rl-" + UUID.randomUUID() + ".db";
        database = new TranslationRepository(context, databaseName);
        db = database.editorialWritableDatabase();
        db.execSQL("PRAGMA foreign_keys=OFF");
    }

    @After public void tearDown() {
        if (database != null) database.close();
        if (context != null && databaseName != null) context.deleteDatabase(databaseName);
    }

    @Test public void emptyDatabaseIsNotReady() {
        assertEquals(EditorialP5EFreshRawLiveRunner.ReconcileLineage.NOT_READY, inspect());
    }

    @Test public void exactEventSevenStateIsReady() {
        seedEventSeven("COMMITTED", true);
        assertEquals(EditorialP5EFreshRawLiveRunner.ReconcileLineage.READY, inspect());
    }

    @Test public void reconcileAttemptMakesLineageAlreadyUsed() {
        seedEventSeven("COMMITTED", true);
        seedAttempt(RECONCILE_ATTEMPT, "L1_RECONCILE", "CLAIMED", false);
        assertEquals(EditorialP5EFreshRawLiveRunner.ReconcileLineage.ALREADY_USED, inspect());
    }

    @Test public void recoveryRequiredRawIsNotReady() {
        seedEventSeven("RECOVERY_REQUIRED", false);
        assertEquals(EditorialP5EFreshRawLiveRunner.ReconcileLineage.NOT_READY, inspect());
    }

    @Test public void missingLifecycleRowIsNotReady() {
        seedAttempt(RAW_ATTEMPT, "L1_RAW_DISCOVERY", "COMMITTED", true);
        seedAuthorizationReceipt();
        assertEquals(EditorialP5EFreshRawLiveRunner.ReconcileLineage.NOT_READY, inspect());
    }

    private EditorialP5EFreshRawLiveRunner.ReconcileLineage inspect() {
        return EditorialP5EFreshRawLiveRunner.inspectReconcileLineage(
                database.editorialReadableDatabase());
    }

    private void seedEventSeven(String status, boolean withBlobs) {
        seedAttempt(RAW_ATTEMPT, "L1_RAW_DISCOVERY", status, withBlobs);
        seedAuthorizationReceipt();
        seedLifecycle(RAW_ATTEMPT);
    }

    private void seedAttempt(String attemptIdentity, String phase, String status,
                             boolean withBlobs) {
        ContentValues row = new ContentValues();
        row.put("attempt_identity", attemptIdentity);
        row.put("request_identity", String.valueOf(attemptIdentity.charAt(0)).repeat(64));
        row.put("binding_identity", BINDING);
        row.put("run_declaration_identity", RUN);
        row.put("chapter_key", "001");
        row.put("phase", phase);
        row.put("predecessor_identity", PREDECESSOR_ID);
        row.put("request_envelope_hash", ENVELOPE_HASH);
        row.put("provider", EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        row.put("model", EditorialP5EFreshRawRoutingPolicy.MODEL);
        row.put("status", status);
        row.put("response_identity", "");
        if (withBlobs) {
            row.put("report_bytes", new byte[]{1, 2, 3});
            row.put("receipt_bytes", new byte[]{4, 5, 6});
        } else {
            row.putNull("report_bytes");
            row.putNull("receipt_bytes");
        }
        row.put("metrics_json", "");
        row.put("recovery_reason_code", "");
        row.put("created_at", 1L);
        row.put("updated_at", 1L);
        db.insertOrThrow("editorial_p5c_attempts", null, row);
    }

    private void seedAuthorizationReceipt() {
        ContentValues row = new ContentValues();
        row.put("authorization_id_hash", AUTHORIZATION_HASH);
        row.put("exact_phase", "L1_RAW_DISCOVERY");
        row.put("attempt_identity", RAW_ATTEMPT);
        row.put("request_identity", REQUEST_ID);
        row.put("binding_identity", BINDING);
        row.put("run_declaration_identity", RUN);
        row.put("chapter_key", "001");
        row.put("provider", EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        row.put("model", EditorialP5EFreshRawRoutingPolicy.MODEL);
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
        row.put("consumed_attempt_identity", RAW_ATTEMPT);
        row.put("created_at", 1L);
        db.insertOrThrow("editorial_p5d_authorization_receipts", null, row);
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
        db.insertOrThrow("editorial_p5d_network_lifecycle", null, row);
    }
}
