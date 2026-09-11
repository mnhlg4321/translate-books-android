package com.ml.tblandroidtxt;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;

import org.json.JSONObject;

import java.util.Objects;

/**
 * Fresh-lineage P5E boundary. Preflight and dispatch are separate methods so
 * a boolean or a shared runner flag cannot accidentally turn a zero-call
 * check into an HTTP call or a RECONCILE execution.
 */
public final class EditorialP5EFreshRawLiveRunner {
    public static final String SELECTOR = "p5e-fresh-mercedes-vol5-20260911-01";
    public static final String CHAPTER_KEY = "001";
    public static final String BINDING_IDENTITY =
            "845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf";
    public static final String RUN_DECLARATION_IDENTITY =
            "8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc";
    public static final String COMPATIBILITY_EVALUATION_ID =
            "f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1";
    public static final String CANONICAL_PACK_HASH =
            "497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d";
    public static final String CANONICAL_PROFILE_HASH =
            "beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21";

    private final TranslationRepository database;
    private final EditorialPackStorageLayout storage;

    public EditorialP5EFreshRawLiveRunner(TranslationRepository database,
                                           EditorialPackStorageLayout storage) {
        this.database = Objects.requireNonNull(database, "database");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    /**
     * Pure request rendering for P5E.9A. It has no database, attempt-store or
     * provider side effect and intentionally accepts no authorization object.
     */
    public static JSONObject preflightOnly(String selector, String chapterKey,
                                           AppSettings settings,
                                           EditorialP5PilotProvider.Request request)
            throws Exception {
        requireFreshRequest(selector, chapterKey, request);
        return OpenRouterEditorialP5PilotProvider.buildFreshRawRequestBodyForPreflight(
                settings, request, EditorialP5RawWireContract.OUTPUT_TOKEN_CAP);
    }

    /**
     * The only live entry point for this fresh runner. It is intentionally
     * RAW-only and refuses repair/retry policy or any authorization for a
     * different lineage. The method is not called by P5E.9A tests.
     */
    public EditorialP5CExactBindingExecution.Result dispatchRaw(
            long projectId, String selector, String chapterKey,
            EditorialP5PilotAuthorization authorization, AppSettings settings) {
        if (projectId <= 0 || !SELECTOR.equals(selector) || !CHAPTER_KEY.equals(chapterKey)) {
            return stop("P5E_FRESH_RAW_SELECTOR_OR_CHAPTER_MISMATCH");
        }
        if (authorization == null) return stop("P5E_FRESH_RAW_AUTHORIZATION_REQUIRED");
        if (!EditorialP5EFreshRawRoutingPolicy.matches(settings)) {
            return stop("P5E_FRESH_RAW_ROUTE_SETTINGS_MISMATCH");
        }
        if (!"L1_RAW_DISCOVERY".equals(authorization.phase())
                || !authorization.singleUse()
                || !authorization.allowChapterToProvider()
                || authorization.maximumPrimarySemanticCalls() != 1
                || authorization.maximumSchemaRepairCalls() != 0
                || authorization.maximumNetworkRetries() != 0
                || authorization.maximumOutputTokens() != EditorialP5RawWireContract.OUTPUT_TOKEN_CAP
                || authorization.allowFullModelResponseStorage()
                || authorization.allowRequestBodyStorage()
                || !EditorialP5EFreshRawRoutingPolicy.PROVIDER.equalsIgnoreCase(
                authorization.provider())
                || !EditorialP5EFreshRawRoutingPolicy.MODEL.equals(authorization.model())
                || !BINDING_IDENTITY.equals(authorization.projectBindingIdentity())
                || !RUN_DECLARATION_IDENTITY.equals(authorization.runDeclarationIdentity())
                || !CANONICAL_PACK_HASH.equals(authorization.canonicalPackHash())
                || !CANONICAL_PROFILE_HASH.equals(authorization.canonicalProfileHash())
                || !COMPATIBILITY_EVALUATION_ID.equals(authorization.compatibilityEvaluationId())
                || !CHAPTER_KEY.equals(authorization.chapterKey())) {
            return stop("P5E_FRESH_RAW_AUTHORIZATION_MISMATCH");
        }
        EditorialP4Binding binding = new EditorialP4BindingDao(database)
                .findByAttemptRequestSelector(SELECTOR).orElse(null);
        if (binding == null || !BINDING_IDENTITY.equals(binding.bindingIdentity())
                || !RUN_DECLARATION_IDENTITY.equals(binding.runDeclarationIdentity())
                || !CANONICAL_PACK_HASH.equals(binding.canonicalPackHash())
                || !CANONICAL_PROFILE_HASH.equals(binding.canonicalProfileHash())
                || !COMPATIBILITY_EVALUATION_ID.equals(binding.compatibilityEvaluationId())
                || projectIdFor(binding) != projectId) {
            return stop("P5E_FRESH_RAW_BINDING_MISMATCH");
        }
        SQLiteDatabase db = database.editorialReadableDatabase();
        if (count(db, "SELECT COUNT(*) FROM editorial_p5c_attempts WHERE binding_identity=?",
                BINDING_IDENTITY) != 0L
                || count(db, "SELECT COUNT(*) FROM editorial_p5d_authorization_receipts "
                + "WHERE binding_identity=?", BINDING_IDENTITY) != 0L
                || count(db, "SELECT COUNT(*) FROM editorial_p5d_reconciliation "
                + "WHERE binding_identity=?", BINDING_IDENTITY) != 0L
                || count(db, "SELECT COUNT(*) FROM editorial_p5d_reconciliation_history "
                + "WHERE attempt_identity IN (SELECT attempt_identity FROM editorial_p5c_attempts "
                + "WHERE binding_identity=?)", BINDING_IDENTITY) != 0L) {
            return stop("P5E_FRESH_RAW_LINEAGE_ALREADY_USED");
        }
        try {
            OpenRouterEditorialP5PilotProvider provider =
                    OpenRouterEditorialP5PilotProvider.withFreshRawLifecyclePersistence(
                            settings, authorization.maximumOutputTokens(), database);
            return new EditorialP5CExactBindingExecution(database, storage)
                    .executeRaw(projectId, SELECTOR, CHAPTER_KEY, authorization, provider);
        } catch (RuntimeException error) {
            return stop("P5E_FRESH_RAW_DISPATCH_SETUP_FAILED");
        }
    }

    private static void requireFreshRequest(String selector, String chapterKey,
                                             EditorialP5PilotProvider.Request request) {
        if (!SELECTOR.equals(selector) || !CHAPTER_KEY.equals(chapterKey)
                || request == null || request.context() == null
                || !BINDING_IDENTITY.equals(request.context().bindingIdentity())
                || !RUN_DECLARATION_IDENTITY.equals(request.context().runDeclarationIdentity())
                || !CHAPTER_KEY.equals(request.chapterKey())
                || !"L1_RAW_DISCOVERY".equals(request.phase())) {
            throw new IllegalArgumentException("P5E_FRESH_RAW_PREFLIGHT_BINDING_MISMATCH");
        }
    }

    private long projectIdFor(EditorialP4Binding binding) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT project_row_id FROM editorial_p4_bindings WHERE binding_identity=?",
                new String[]{binding.bindingIdentity()})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    private static long count(SQLiteDatabase db, String sql, String... args) {
        try (Cursor cursor = db.rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    private static EditorialP5CExactBindingExecution.Result stop(String reason) {
        return new EditorialP5CExactBindingExecution.Result(
                EditorialP5CExactBindingExecution.Status.STOP, reason,
                null, null, 0, false, "NOT_CERTIFIED");
    }
}
