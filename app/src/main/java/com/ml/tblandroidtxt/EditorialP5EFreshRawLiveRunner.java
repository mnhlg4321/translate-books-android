package com.ml.tblandroidtxt;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4SourceIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
            "3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1";
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
     * Read-only current-binding preflight. It reconstructs the app-owned
     * request facts from the persisted binding and immutable pack storage,
     * then compares the caller's prepared request before rendering JSON.
     * This closes the gap where a fixture could otherwise supply a correct
     * selector while carrying different source or authority bytes.
     */
    public JSONObject preflightOnly(long projectId, String selector, String chapterKey,
                                    AppSettings settings,
                                    EditorialP5PilotProvider.Request request) throws Exception {
        if (projectId <= 0) throw preflightMismatch();
        requireFreshRequest(selector, chapterKey, request);
        EditorialP4Binding binding = new EditorialP4BindingDao(database)
                .findByAttemptRequestSelector(SELECTOR).orElse(null);
        if (binding == null || projectIdFor(binding) != projectId) throw preflightMismatch();
        requireExactBinding(binding);
        if (!chapterExists(projectId, CHAPTER_KEY)) throw preflightMismatch();
        EditorialP5PilotRequest expected = loadExactRequest(projectId, binding);
        verifyPreparedRequest(expected, request);
        return preflightOnly(selector, chapterKey, settings, request);
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
        EditorialP4Binding binding;
        try {
            binding = new EditorialP4BindingDao(database)
                    .findByAttemptRequestSelector(SELECTOR).orElse(null);
        } catch (RuntimeException error) {
            return stop("P5E_FRESH_RAW_LINEAGE_CHECK_FAILED");
        }
        long persistedProjectId;
        try {
            persistedProjectId = binding == null ? -1L : projectIdFor(binding);
        } catch (RuntimeException error) {
            return stop("P5E_FRESH_RAW_LINEAGE_CHECK_FAILED");
        }
        if (binding == null || !BINDING_IDENTITY.equals(binding.bindingIdentity())
                || !RUN_DECLARATION_IDENTITY.equals(binding.runDeclarationIdentity())
                || !CANONICAL_PACK_HASH.equals(binding.canonicalPackHash())
                || !CANONICAL_PROFILE_HASH.equals(binding.canonicalProfileHash())
                || !COMPATIBILITY_EVALUATION_ID.equals(binding.compatibilityEvaluationId())
                || persistedProjectId != projectId) {
            return stop("P5E_FRESH_RAW_BINDING_MISMATCH");
        }
        FreshRawLineageCheck lineage = inspectLineage(database, BINDING_IDENTITY);
        if (lineage.status() == FreshRawLineageCheck.Status.CHECK_FAILED) {
            return stop("P5E_FRESH_RAW_LINEAGE_CHECK_FAILED");
        }
        if (lineage.status() == FreshRawLineageCheck.Status.ALREADY_USED) {
            return stop("P5E_FRESH_RAW_LINEAGE_ALREADY_USED");
        }
        try {
            OpenRouterEditorialP5PilotProvider provider =
                    OpenRouterEditorialP5PilotProvider.withFreshRawLifecyclePersistence(
                            settings, authorization.maximumOutputTokens(), database);
            EditorialP5CExactBindingExecution.Result result =
                    new EditorialP5CExactBindingExecution(database, storage)
                            .executeRaw(projectId, SELECTOR, CHAPTER_KEY, authorization, provider);
            P5ERawDiagnostics.dispatch(result);
            return result;
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

    private static void requireExactBinding(EditorialP4Binding binding) {
        if (!BINDING_IDENTITY.equals(binding.bindingIdentity())
                || !RUN_DECLARATION_IDENTITY.equals(binding.runDeclarationIdentity())
                || !CANONICAL_PACK_HASH.equals(binding.canonicalPackHash())
                || !CANONICAL_PROFILE_HASH.equals(binding.canonicalProfileHash())
                || !COMPATIBILITY_EVALUATION_ID.equals(binding.compatibilityEvaluationId())
                || !EditorialSafe4Contract.NORMAL_MODE.equals(binding.sourceMode())) {
            throw preflightMismatch();
        }
    }

    private EditorialP5PilotRequest loadExactRequest(long projectId,
                                                       EditorialP4Binding binding)
            throws IOException {
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                .resolve(binding.packId(), binding.packVersion()).orElse(null);
        if (candidate == null
                || !CANONICAL_PACK_HASH.equals(candidate.canonicalPackHash())
                || !binding.manifestFingerprint().equals(
                EditorialPackSelectionPolicy.manifestFingerprint(candidate.manifest()))
                || !CANONICAL_PROFILE_HASH.equals(candidate.canonicalProfileHash())
                || !COMPATIBILITY_EVALUATION_ID.equals(candidate.evaluation().evaluationId())) {
            throw preflightMismatch();
        }
        EditorialPackManifest manifest = candidate.manifest();
        EditorialP5PilotRequest.PackAuthority authority = resolveAuthority(binding, manifest);

        EditorialRepository repository = new EditorialRepository(database);
        EditorialRepository.Chapter chapter = repository.listChapters(projectId).stream()
                .filter(value -> CHAPTER_KEY.equals(value.chapterKey)).findFirst().orElse(null);
        if (chapter == null) throw preflightMismatch();
        Map<String, EditorialRepository.AssetSnapshot> assets = new HashMap<>();
        for (EditorialRepository.AssetSnapshot asset : repository.chapterAssets(chapter.id)) {
            assets.put(asset.role.name(), asset);
        }
        ArrayList<EditorialP5PilotRequest.SourceBytes> sources = new ArrayList<>();
        for (EditorialP4SourceIdentity identity : binding.inputs()) {
            EditorialRepository.AssetSnapshot asset = assets.get(identity.role());
            if (asset == null || asset.content == null
                    || !identity.sourceReference().equals(asset.sourceUri)) {
                throw preflightMismatch();
            }
            byte[] bytes = asset.content.getBytes(StandardCharsets.UTF_8);
            if (identity.byteLength() != bytes.length
                    || !identity.sha256().equals(EditorialCanonicalJson.sha256Hex(bytes))) {
                throw preflightMismatch();
            }
            sources.add(new EditorialP5PilotRequest.SourceBytes(identity.role(),
                    identity.sourceReference(), bytes, identity.encoding(), schemaId(identity.role()),
                    identity.schemaStatus(), identity.ordinal()));
        }
        return new EditorialP5PilotRequest(binding, manifest, authority, CHAPTER_KEY,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY, sources,
                binding.runDeclarationIdentity(), List.of("chapter:" + CHAPTER_KEY),
                List.of("population:" + CHAPTER_KEY), true,
                EditorialP5RawWireContract.OUTPUT_TOKEN_CAP);
    }

    private EditorialP5PilotRequest.PackAuthority resolveAuthority(
            EditorialP4Binding binding, EditorialPackManifest manifest) throws IOException {
        if (!storage.hasImmutableMarker(binding.canonicalPackHash())) throw preflightMismatch();
        EnumMap<EditorialPackFileRole, byte[]> values = new EnumMap<>(EditorialPackFileRole.class);
        for (EditorialPackManifest.FileEntry file : manifest.fileEntries()) {
            Path path = storage.immutableEntry(binding.canonicalPackHash(), file.path());
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw preflightMismatch();
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length != file.byteLength()
                    || !file.sha256().equals(EditorialCanonicalJson.sha256Hex(bytes))) {
                throw preflightMismatch();
            }
            values.put(file.role(), bytes);
        }
        return new EditorialP5PilotRequest.PackAuthority(values);
    }

    private static void verifyPreparedRequest(EditorialP5PilotRequest expected,
                                               EditorialP5PilotProvider.Request actual) {
        EditorialP5PilotProvider.Request.Context expectedContext =
                new EditorialP5PilotProvider.Request.Context(
                        expected.binding().bindingIdentity(), expected.binding().runDeclarationIdentity(),
                        expected.manifestFingerprint(), expected.bundleIdentity(),
                        expected.predecessorIdentity(), expected.stableAnchors(),
                        expected.populationIds());
        if (!expected.attemptIdentity().equals(actual.attemptIdentity())
                || actual.callKind() != EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC
                || !EditorialP5EFreshRawRoutingPolicy.PROVIDER.equals(actual.provider())
                || !EditorialP5EFreshRawRoutingPolicy.MODEL.equals(actual.model())
                || !"L1_RAW_DISCOVERY".equals(actual.phase())
                || !EditorialP5RawWireContract.SCHEMA_VERSION.equals(actual.outputSchemaId())
                || !CHAPTER_KEY.equals(actual.chapterKey())
                || !expectedContext.equals(actual.context())) {
            throw preflightMismatch();
        }
        Map<String, byte[]> visible = new TreeMap<>();
        visible.put(EditorialSafe4Contract.RAW,
                expected.source(EditorialSafe4Contract.RAW).bytes());
        visible.put(EditorialSafe4Contract.GLOSSARY,
                expected.source(EditorialSafe4Contract.GLOSSARY).bytes());
        if (!sameBytes(visible, actual.visibleSources())
                || !sameAuthority(expected.authority(), actual.authority())
                || !expectedEnvelopeHash(expected, visible).equals(actual.requestEnvelopeHash())) {
            throw preflightMismatch();
        }
    }

    private static String expectedEnvelopeHash(EditorialP5PilotRequest request,
                                                Map<String, byte[]> visible) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("contractVersion", EditorialSafe4Contract.CONTRACT_VERSION);
        root.put("outputSchema", EditorialP5RawWireContract.SCHEMA_VERSION);
        root.put("attemptIdentity", request.attemptIdentity());
        root.put("requestIdentity", request.requestIdentity());
        root.put("bindingIdentity", request.binding().bindingIdentity());
        root.put("runDeclarationIdentity", request.binding().runDeclarationIdentity());
        root.put("canonicalPackHash", request.binding().canonicalPackHash());
        root.put("canonicalProfileHash", request.binding().canonicalProfileHash());
        root.put("compatibilityEvaluationId", request.binding().compatibilityEvaluationId());
        root.put("manifestFingerprint", request.manifestFingerprint());
        root.put("chapterKey", request.chapterKey());
        root.put("phase", request.phase());
        root.put("provider", EditorialP5EFreshRawRoutingPolicy.PROVIDER);
        root.put("model", EditorialP5EFreshRawRoutingPolicy.MODEL);
        root.put("stableAnchors", request.stableAnchors());
        root.put("populationIds", request.populationIds());
        ArrayList<Object> fingerprints = new ArrayList<>();
        for (String role : new ArrayList<>(visible.keySet())) {
            EditorialP5PilotRequest.SourceBytes source = request.source(role);
            if (source == null || source.bytes() == null) throw preflightMismatch();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("role", role);
            value.put("sourceId", source.sourceReference());
            value.put("byteLength", BigDecimal.valueOf(source.bytes().length));
            value.put("sha256", source.actualSha256());
            value.put("schemaId", source.schemaId());
            fingerprints.add(value);
        }
        root.put("visibleSources", fingerprints);
        return EditorialCanonicalJson.sha256Hex(EditorialCanonicalJson.canonicalize(root)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static boolean sameBytes(Map<String, byte[]> expected, Map<String, byte[]> actual) {
        if (actual == null || expected.size() != actual.size()
                || !expected.keySet().equals(actual.keySet())) return false;
        for (String key : expected.keySet()) {
            if (!Arrays.equals(expected.get(key), actual.get(key))) return false;
        }
        return true;
    }

    private static boolean sameAuthority(EditorialP5PilotRequest.PackAuthority expected,
                                         EditorialP5PilotRequest.PackAuthority actual) {
        if (actual == null) return false;
        Map<EditorialPackFileRole, byte[]> expectedEntries = expected.entries();
        Map<EditorialPackFileRole, byte[]> actualEntries = actual.entries();
        if (expectedEntries.size() != actualEntries.size()
                || !expectedEntries.keySet().equals(actualEntries.keySet())) return false;
        for (EditorialPackFileRole role : expectedEntries.keySet()) {
            if (!Arrays.equals(expectedEntries.get(role), actualEntries.get(role))) return false;
        }
        return true;
    }

    private static String schemaId(String role) {
        return EditorialSafe4Contract.GLOSSARY.equals(role) ? "safe4.full.glossary.v1"
                : EditorialSafe4Contract.PRONOUN.equals(role) ? "safe4.full.pronoun.v1" : "text.v1";
    }

    private boolean chapterExists(long projectId, String chapterKey) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT 1 FROM editorial_chapters WHERE project_id=? AND chapter_key=? LIMIT 1",
                new String[]{String.valueOf(projectId), chapterKey})) {
            return cursor.moveToFirst();
        }
    }

    private static IllegalArgumentException preflightMismatch() {
        return new IllegalArgumentException("P5E_FRESH_RAW_PREFLIGHT_BINDING_MISMATCH");
    }

    private long projectIdFor(EditorialP4Binding binding) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT project_row_id FROM editorial_p4_bindings WHERE binding_identity=?",
                new String[]{binding.bindingIdentity()})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    /**
     * Reads the complete fresh-lineage guard on the schema that owns each
     * identity. Reconciliation, history and lifecycle rows are related to a
     * binding through the durable attempt identity; they do not own a
     * binding_identity column. All reads happen before the aggregate status is
     * calculated so a schema drift in any constituent query fails closed.
     */
    static FreshRawLineageCheck inspectLineage(TranslationRepository database,
                                                String bindingIdentity) {
        try {
            return inspectLineage(database == null ? null : database.editorialReadableDatabase(),
                    bindingIdentity);
        } catch (RuntimeException error) {
            return FreshRawLineageCheck.checkFailed();
        }
    }

    /** Package-private overload used by read-only instrumentation against a database handle. */
    static FreshRawLineageCheck inspectLineage(SQLiteDatabase db, String bindingIdentity) {
        if (db == null || bindingIdentity == null || bindingIdentity.isEmpty()) {
            return FreshRawLineageCheck.checkFailed();
        }
        try {
            long attempts = count(db,
                    "SELECT COUNT(*) FROM editorial_p5c_attempts WHERE binding_identity=?",
                    bindingIdentity);
            long authorizationReceipts = count(db,
                    "SELECT COUNT(*) FROM editorial_p5d_authorization_receipts "
                            + "WHERE binding_identity=?", bindingIdentity);
            long reconciliation = count(db,
                    "SELECT COUNT(*) FROM editorial_p5d_reconciliation AS r "
                            + "JOIN editorial_p5c_attempts AS a "
                            + "ON a.attempt_identity = r.attempt_identity "
                            + "WHERE a.binding_identity=?", bindingIdentity);
            long reconciliationHistory = count(db,
                    "SELECT COUNT(*) FROM editorial_p5d_reconciliation_history AS h "
                            + "JOIN editorial_p5c_attempts AS a "
                            + "ON a.attempt_identity = h.attempt_identity "
                            + "WHERE a.binding_identity=?", bindingIdentity);
            long lifecycle = count(db,
                    "SELECT COUNT(*) FROM editorial_p5d_network_lifecycle AS l "
                            + "JOIN editorial_p5c_attempts AS a "
                            + "ON a.attempt_identity = l.attempt_identity "
                            + "WHERE a.binding_identity=?", bindingIdentity);
            if (attempts < 0L || authorizationReceipts < 0L || reconciliation < 0L
                    || reconciliationHistory < 0L || lifecycle < 0L) {
                return FreshRawLineageCheck.checkFailed();
            }
            boolean used = attempts != 0L || authorizationReceipts != 0L
                    || reconciliation != 0L || reconciliationHistory != 0L
                    || lifecycle != 0L;
            return new FreshRawLineageCheck(
                    used ? FreshRawLineageCheck.Status.ALREADY_USED
                            : FreshRawLineageCheck.Status.UNUSED,
                    attempts, authorizationReceipts, reconciliation,
                    reconciliationHistory, lifecycle);
        } catch (RuntimeException error) {
            return FreshRawLineageCheck.checkFailed();
        }
    }

    static final class FreshRawLineageCheck {
        enum Status { UNUSED, ALREADY_USED, CHECK_FAILED }

        private final Status status;
        private final long attempts;
        private final long authorizationReceipts;
        private final long reconciliation;
        private final long reconciliationHistory;
        private final long lifecycle;

        private FreshRawLineageCheck(Status status, long attempts,
                                     long authorizationReceipts, long reconciliation,
                                     long reconciliationHistory, long lifecycle) {
            this.status = status;
            this.attempts = attempts;
            this.authorizationReceipts = authorizationReceipts;
            this.reconciliation = reconciliation;
            this.reconciliationHistory = reconciliationHistory;
            this.lifecycle = lifecycle;
        }

        static FreshRawLineageCheck checkFailed() {
            return new FreshRawLineageCheck(Status.CHECK_FAILED,
                    -1L, -1L, -1L, -1L, -1L);
        }

        Status status() { return status; }
        long attempts() { return attempts; }
        long authorizationReceipts() { return authorizationReceipts; }
        long reconciliation() { return reconciliation; }
        long reconciliationHistory() { return reconciliationHistory; }
        long lifecycle() { return lifecycle; }
    }

    private static long count(SQLiteDatabase db, String sql, String... args) {
        try (Cursor cursor = db.rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    private static EditorialP5CExactBindingExecution.Result stop(String reason) {
        P5ERawDiagnostics.stop(reason);
        return new EditorialP5CExactBindingExecution.Result(
                EditorialP5CExactBindingExecution.Status.STOP, reason,
                null, null, 0, false, "NOT_CERTIFIED");
    }
}
