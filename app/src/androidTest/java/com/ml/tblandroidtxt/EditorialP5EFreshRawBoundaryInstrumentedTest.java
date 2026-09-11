package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4SourceIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Zero-call boundary checks for the fresh P5E RAW lineage. This class only
 * opens the current DB for readback and renders an in-memory request body;
 * it never invokes executeRaw, an attempt store or a network provider.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5EFreshRawBoundaryInstrumentedTest {
    private static final String PACKAGE = "com.ml.tblandroidtxt";
    private static final long EXPECTED_CANDIDATE_VERSION_CODE = 206L;
    private static final String FRESH_SELECTOR = "p5e-fresh-mercedes-vol5-20260911-01";
    private static final String OLD_SELECTOR = "p5d-raw-mercedes-vol5-001";
    private static final String FRESH_BINDING =
            "845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf";
    private static final String FRESH_RUN =
            "8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc";
    private static final String FRESH_CHAPTER = "001";
    private static final String COMPATIBILITY_EVALUATION =
            "3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1";
    private static final String HISTORICAL_COMPATIBILITY_EVALUATION =
            "f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1";
    private static final String PACK_HASH =
            "497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d";
    private static final String PROFILE_HASH =
            "beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21";
    private static final Map<String, Integer> EXPECTED_SOURCE_BYTES = Map.of(
            "RAW", 23_814, "DRAFT", 26_462, "GLOSSARY", 3_249, "PRONOUN", 452);
    private static final Map<String, String> EXPECTED_SOURCE_HASHES = Map.of(
            "RAW", "a308210eca80557cfa9fec7ed55b2ee3de5c1c4776e59b2b5edbf0efb04504be",
            "DRAFT", "64adecd8ceccbb13446ef14c494ca9bb1987117c428c5758e7442270ec7f62b5",
            "GLOSSARY", "4bc3e2dd05542aa5ca6b7e5fcac43ed53e9af57060eb69c6fa71e9d0a2ea0314",
            "PRONOUN", "4947ff9184995be5f850f2323fbe0a04c67302fb8d5afb63cf12202b44720686");

    @Test public void readOnlyFreshBaselineMatchesTupleAndZeroRows() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(PACKAGE, 0);
        long versionCode = Build.VERSION.SDK_INT >= 28
                ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
        assertEquals(EXPECTED_CANDIDATE_VERSION_CODE, versionCode);

        java.io.File databaseFile = context.getDatabasePath("tbl_android_txt.db");
        assertTrue("current pilot DB must exist", databaseFile.isFile());
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(databaseFile.getPath(), null,
                SQLiteDatabase.OPEN_READONLY)) {
            assertEquals(24, db.getVersion());
            assertEquals("delete", scalar(db, "PRAGMA journal_mode"));

            try (Cursor cursor = db.rawQuery(
                    "SELECT binding_identity,run_declaration_identity,canonical_pack_hash,"
                            + "canonical_profile_hash,compatibility_evaluation_id,project_row_id,"
                            + "source_mode,phase_identity,execution_allowed,certification_state "
                            + "FROM editorial_p4_bindings WHERE attempt_request_selector=?",
                    new String[]{FRESH_SELECTOR})) {
                assertTrue("fresh selector must be present", cursor.moveToFirst());
                assertEquals(FRESH_BINDING, cursor.getString(0));
                assertEquals(FRESH_RUN, cursor.getString(1));
                assertEquals(PACK_HASH, cursor.getString(2));
                assertEquals(PROFILE_HASH, cursor.getString(3));
                assertEquals(COMPATIBILITY_EVALUATION, cursor.getString(4));
                assertEquals("NORMAL_FOUR_SOURCE", cursor.getString(6));
                assertEquals("L1_SOURCE_PREFLIGHT", cursor.getString(7));
                assertEquals(0, cursor.getInt(8));
                assertEquals("NOT_CERTIFIED", cursor.getString(9));
                long projectId = cursor.getLong(5);
                assertEquals(1L, count(db,
                        "SELECT COUNT(*) FROM editorial_chapters WHERE project_id=? AND chapter_key=?",
                        String.valueOf(projectId), FRESH_CHAPTER));
            }

            assertEquals(0L, count(db, "SELECT COUNT(*) FROM editorial_p5c_attempts"));
            assertEquals(0L, count(db,
                    "SELECT COUNT(*) FROM editorial_p5d_authorization_receipts"));
            assertEquals(0L, count(db, "SELECT COUNT(*) FROM editorial_p5d_reconciliation"));
            assertEquals(0L, count(db,
                    "SELECT COUNT(*) FROM editorial_p5d_reconciliation_history"));
            assertEquals(0L, count(db,
                    "SELECT COUNT(*) FROM editorial_p5d_network_lifecycle"));
            assertEquals(0L, count(db,
                    "SELECT COUNT(*) FROM editorial_p5c_attempts "
                            + "WHERE report_bytes IS NOT NULL OR receipt_bytes IS NOT NULL"));

            try (Cursor cursor = db.rawQuery(
                    "SELECT role,byte_length,sha256 FROM editorial_p4_binding_inputs "
                            + "WHERE binding_identity=? ORDER BY role",
                    new String[]{FRESH_BINDING})) {
                int rows = 0;
                while (cursor.moveToNext()) {
                    String role = cursor.getString(0);
                    assertEquals(EXPECTED_SOURCE_BYTES.get(role).longValue(), cursor.getLong(1));
                    assertEquals(EXPECTED_SOURCE_HASHES.get(role), cursor.getString(2));
                    rows++;
                }
                assertEquals(4, rows);
            }
        }
    }

    @Test public void historicalP5dRunnerRejectsFreshSelectorBeforeProvider() throws Exception {
        assertNotEquals(FRESH_SELECTOR, OLD_SELECTOR);
        assertEquals(OLD_SELECTOR, privateStaticString(
                EditorialP5DVol5RawPilotInstrumentedTest.class, "SELECTOR"));
        String historicalAttempt = privateStaticString(
                EditorialP5DVol5RawPilotInstrumentedTest.class, "ORIGINAL_ATTEMPT_ID");
        assertEquals(64, historicalAttempt.length());
        try {
            EditorialP5DVol5RawPilotInstrumentedTest.requireHistoricalSelector(FRESH_SELECTOR);
            fail("historical P5D runner must reject the fresh selector");
        } catch (IllegalArgumentException expected) {
            assertEquals("P5D_HISTORICAL_RUNNER_SELECTOR_MISMATCH", expected.getMessage());
        }

        Context context = ApplicationProvider.getApplicationContext();
        java.io.File databaseFile = context.getDatabasePath("tbl_android_txt.db");
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(databaseFile.getPath(), null,
                SQLiteDatabase.OPEN_READONLY)) {
            assertEquals(0L, count(db,
                    "SELECT COUNT(*) FROM editorial_p5c_attempts WHERE attempt_identity=?",
                    historicalAttempt));
            assertEquals(0L, count(db,
                    "SELECT COUNT(*) FROM editorial_p5c_attempts WHERE binding_identity=?",
                    FRESH_BINDING));
            assertEquals(0L, count(db,
                    "SELECT COUNT(*) FROM editorial_p5d_authorization_receipts "
                            + "WHERE binding_identity=?", FRESH_BINDING));
            assertEquals(0L, count(db,
                    "SELECT COUNT(*) FROM editorial_p5d_reconciliation AS r "
                            + "JOIN editorial_p5c_attempts AS a "
                            + "ON a.attempt_identity = r.attempt_identity "
                            + "WHERE a.binding_identity=?", FRESH_BINDING));
        }
    }

    @Test public void freshRawPreflightRendersStrictRouteWithoutDispatchOrMutation()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        try (TranslationRepository database = new TranslationRepository(context)) {
            RowCounts before = rowCounts(database.editorialReadableDatabase());
            PreflightFixture fixture = loadFixture(database);

            AppSettings settings = new AppSettings();
            settings.provider = EditorialP5EFreshRawRoutingPolicy.PROVIDER;
            settings.model = EditorialP5EFreshRawRoutingPolicy.MODEL;
            settings.baseUrl = AppSettings.defaultBaseUrl(
                    EditorialP5EFreshRawRoutingPolicy.PROVIDER);
            settings.apiKey = "";
            JSONObject body = new EditorialP5EFreshRawLiveRunner(database,
                    new EditorialPackStorageLayout(context.getFilesDir().toPath()))
                    .preflightOnly(projectId(database, FRESH_BINDING), FRESH_SELECTOR,
                            FRESH_CHAPTER, settings, fixture.providerRequest);

            JSONObject provider = body.getJSONObject("provider");
            assertTrue(provider.getBoolean("require_parameters"));
            assertFalse(provider.getBoolean("allow_fallbacks"));
            assertEquals(EditorialP5EFreshRawRoutingPolicy.UPSTREAM_PROVIDER,
                    provider.getJSONArray("only").getString(0));
            assertEquals(EditorialP5EFreshRawRoutingPolicy.DATA_COLLECTION_POLICY,
                    provider.getString("data_collection"));
            assertFalse(body.has("plugins"));
            assertFalse(body.getBoolean("stream"));
            assertEquals(EditorialP5RawWireContract.OUTPUT_TOKEN_CAP,
                    body.getInt("max_tokens"));
            assertEquals(EditorialP5EFreshRawRoutingPolicy.MODEL,
                    body.getString("model"));
            assertEquals("json_schema",
                    body.getJSONObject("response_format").getString("type"));
            assertTrue(body.getJSONObject("response_format").getJSONObject("json_schema")
                    .getBoolean("strict"));
            assertEquals(EditorialP5RawWireContract.REASONING_POLICY,
                    body.getString("reasoning_effort"));

            byte[] canonicalBody = EditorialCanonicalJson.canonicalize(
                    EditorialCanonicalJson.parse(body.toString().getBytes(StandardCharsets.UTF_8)))
                    .getBytes(StandardCharsets.UTF_8);
            byte[] schema = EditorialCanonicalJson.canonicalize(
                    EditorialCanonicalJson.parse(body.getJSONObject("response_format")
                            .getJSONObject("json_schema").getJSONObject("schema")
                            .toString().getBytes(StandardCharsets.UTF_8)))
                    .getBytes(StandardCharsets.UTF_8);
            assertTrue(canonicalBody.length > schema.length);
            assertEquals(80_317, fixture.contextBytes);
            assertTrue(canonicalBody.length > fixture.contextBytes);

            // A preflight has no provider object and no execution/store call.
            assertEquals(0, fixture.providerCalls);
            RowCounts after = rowCounts(database.editorialReadableDatabase());
            assertEquals(before, after);
            Log.i("P5E_9A_PREFLIGHT", "selector=" + FRESH_SELECTOR
                    + " binding=" + FRESH_BINDING
                    + " requestBytes=" + canonicalBody.length
                    + " schemaBytes=" + schema.length
                    + " contextBytes=" + fixture.contextBytes
                    + " requestHash=" + EditorialCanonicalJson.sha256Hex(canonicalBody)
                    + " providerCalls=" + fixture.providerCalls
                    + " attempts=" + after.attempts
                    + " authorizations=" + after.authorizations
                    + " reconciliation=" + after.reconciliation);
        }
    }

    @Test public void freshLiveDispatchRequiresAuthorizationAndDoesNotMutate() {
        Context context = ApplicationProvider.getApplicationContext();
        try (TranslationRepository database = new TranslationRepository(context)) {
            RowCounts before = rowCounts(database.editorialReadableDatabase());
            AppSettings settings = new AppSettings();
            settings.provider = EditorialP5EFreshRawRoutingPolicy.PROVIDER;
            settings.model = EditorialP5EFreshRawRoutingPolicy.MODEL;
            settings.baseUrl = AppSettings.defaultBaseUrl(
                    EditorialP5EFreshRawRoutingPolicy.PROVIDER);
            EditorialP5CExactBindingExecution.Result result =
                    new EditorialP5EFreshRawLiveRunner(database,
                            new EditorialPackStorageLayout(context.getFilesDir().toPath()))
                            .dispatchRaw(1L, FRESH_SELECTOR, FRESH_CHAPTER, null, settings);
            assertEquals(EditorialP5CExactBindingExecution.Status.STOP, result.status());
            assertEquals("P5E_FRESH_RAW_AUTHORIZATION_REQUIRED", result.reasonCode());
            assertEquals(0, result.providerCalls());
            assertEquals(before, rowCounts(database.editorialReadableDatabase()));
        }
    }

    @Test public void historicalEvaluationAuthorizationIsRejectedBeforeProvider() {
        Context context = ApplicationProvider.getApplicationContext();
        try (TranslationRepository database = new TranslationRepository(context)) {
            RowCounts before = rowCounts(database.editorialReadableDatabase());
            AppSettings settings = new AppSettings();
            settings.provider = EditorialP5EFreshRawRoutingPolicy.PROVIDER;
            settings.model = EditorialP5EFreshRawRoutingPolicy.MODEL;
            settings.baseUrl = AppSettings.defaultBaseUrl(
                    EditorialP5EFreshRawRoutingPolicy.PROVIDER);
            EditorialP5CExactBindingExecution.Result result =
                    new EditorialP5EFreshRawLiveRunner(database,
                            new EditorialPackStorageLayout(context.getFilesDir().toPath()))
                            .dispatchRaw(1L, FRESH_SELECTOR, FRESH_CHAPTER,
                                    authorization(HISTORICAL_COMPATIBILITY_EVALUATION), settings);
            assertEquals(EditorialP5CExactBindingExecution.Status.STOP, result.status());
            assertEquals("P5E_FRESH_RAW_AUTHORIZATION_MISMATCH", result.reasonCode());
            assertEquals(0, result.providerCalls());
            assertEquals(before, rowCounts(database.editorialReadableDatabase()));
        }
    }

    private static PreflightFixture loadFixture(TranslationRepository database) throws Exception {
        EditorialP4Binding binding = new EditorialP4BindingDao(database)
                .findByAttemptRequestSelector(FRESH_SELECTOR)
                .orElseThrow(() -> new AssertionError("fresh binding is missing"));
        assertEquals(FRESH_BINDING, binding.bindingIdentity());
        assertEquals(FRESH_RUN, binding.runDeclarationIdentity());

        EditorialRepository repository = new EditorialRepository(database);
        long projectId = projectId(database, binding.bindingIdentity());
        EditorialRepository.Chapter chapter = repository.listChapters(projectId).stream()
                .filter(value -> FRESH_CHAPTER.equals(value.chapterKey))
                .findFirst().orElseThrow(() -> new AssertionError("fresh chapter is missing"));
        Map<String, EditorialRepository.AssetSnapshot> assets = new HashMap<>();
        for (EditorialRepository.AssetSnapshot asset : repository.chapterAssets(chapter.id)) {
            assets.put(asset.role.name(), asset);
        }

        List<EditorialP5PilotRequest.SourceBytes> sources = new ArrayList<>();
        for (EditorialP4SourceIdentity identity : binding.inputs()) {
            EditorialRepository.AssetSnapshot asset = assets.get(identity.role());
            assertNotNull("source asset missing: " + identity.role(), asset);
            byte[] bytes = asset.content.getBytes(StandardCharsets.UTF_8);
            assertEquals(identity.byteLength(), bytes.length);
            assertEquals(identity.sha256(), EditorialCanonicalJson.sha256Hex(bytes));
            assertEquals(EXPECTED_SOURCE_BYTES.get(identity.role()).intValue(), bytes.length);
            assertEquals(EXPECTED_SOURCE_HASHES.get(identity.role()), identity.sha256());
            sources.add(new EditorialP5PilotRequest.SourceBytes(identity.role(),
                    identity.sourceReference(), bytes, identity.encoding(), schemaId(identity.role()),
                    identity.schemaStatus(), identity.ordinal()));
        }

        EditorialPackStorageLayout storage = new EditorialPackStorageLayout(
                ApplicationProvider.getApplicationContext().getFilesDir().toPath());
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                .resolve(binding.packId(), binding.packVersion())
                .orElseThrow(() -> new AssertionError("fresh pack is not selectable"));
        assertEquals(PACK_HASH, candidate.canonicalPackHash());
        assertEquals(PROFILE_HASH, candidate.canonicalProfileHash());
        assertEquals(COMPATIBILITY_EVALUATION, candidate.evaluation().evaluationId());
        EditorialPackManifest manifest = candidate.manifest();
        EnumMap<EditorialPackFileRole, byte[]> authority =
                new EnumMap<>(EditorialPackFileRole.class);
        for (EditorialPackManifest.FileEntry file : manifest.fileEntries()) {
            Path path = storage.immutableEntry(binding.canonicalPackHash(), file.path());
            assertTrue(Files.isRegularFile(path));
            byte[] bytes = Files.readAllBytes(path);
            assertEquals(file.byteLength(), bytes.length);
            assertEquals(file.sha256(), EditorialCanonicalJson.sha256Hex(bytes));
            authority.put(file.role(), bytes);
        }

        EditorialP5PilotRequest request = new EditorialP5PilotRequest(binding, manifest,
                new EditorialP5PilotRequest.PackAuthority(authority), FRESH_CHAPTER,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY, sources,
                binding.runDeclarationIdentity(), List.of("chapter:" + FRESH_CHAPTER),
                List.of("population:" + FRESH_CHAPTER), true,
                EditorialP5RawWireContract.OUTPUT_TOKEN_CAP);
        Map<String, byte[]> visible = new TreeMap<>();
        visible.put(EditorialSafe4Contract.RAW, request.source(EditorialSafe4Contract.RAW).bytes());
        visible.put(EditorialSafe4Contract.GLOSSARY,
                request.source(EditorialSafe4Contract.GLOSSARY).bytes());
        EditorialP5PilotProvider.Request.Context context =
                new EditorialP5PilotProvider.Request.Context(binding.bindingIdentity(),
                        binding.runDeclarationIdentity(), request.manifestFingerprint(),
                        request.bundleIdentity(), request.predecessorIdentity(),
                        request.stableAnchors(), request.populationIds());
        String envelopeHash = requestEnvelopeHash(request, visible);
        EditorialP5PilotProvider.Request providerRequest = new EditorialP5PilotProvider.Request(
                request.attemptIdentity(), EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC,
                EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                EditorialP5EFreshRawRoutingPolicy.MODEL, request.phase(), envelopeHash, visible,
                request.authority(), EditorialP5RawWireContract.SCHEMA_VERSION, FRESH_CHAPTER,
                "", context);
        int contextBytes = 0;
        for (byte[] bytes : visible.values()) contextBytes += bytes.length;
        for (byte[] bytes : request.authority().entries().values()) contextBytes += bytes.length;
        return new PreflightFixture(providerRequest, contextBytes, 0);
    }

    private static String requestEnvelopeHash(EditorialP5PilotRequest request,
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
        ArrayList<Object> sourceFingerprints = new ArrayList<>();
        for (String role : new ArrayList<>(visible.keySet())) {
            EditorialP5PilotRequest.SourceBytes source = request.source(role);
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("role", role);
            value.put("sourceId", source.sourceReference());
            value.put("byteLength", BigDecimal.valueOf(source.bytes().length));
            value.put("sha256", source.actualSha256());
            value.put("schemaId", source.schemaId());
            sourceFingerprints.add(value);
        }
        sourceFingerprints.sort((left, right) -> ((String) ((Map<?, ?>) left).get("role"))
                .compareTo((String) ((Map<?, ?>) right).get("role")));
        root.put("visibleSources", sourceFingerprints);
        return EditorialCanonicalJson.sha256Hex(EditorialCanonicalJson.canonicalize(root)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String schemaId(String role) {
        return EditorialSafe4Contract.GLOSSARY.equals(role) ? "safe4.full.glossary.v1"
                : EditorialSafe4Contract.PRONOUN.equals(role) ? "safe4.full.pronoun.v1" : "text.v1";
    }

    private static long projectId(TranslationRepository database, String bindingIdentity) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT project_row_id FROM editorial_p4_bindings WHERE binding_identity=?",
                new String[]{bindingIdentity})) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private static EditorialP5PilotAuthorization authorization(String evaluationId) {
        long issuedAt = 1_000L;
        return new EditorialP5PilotAuthorization("p5e-evaluation-mismatch-test",
                FRESH_BINDING, FRESH_RUN, PACK_HASH, PROFILE_HASH, evaluationId,
                FRESH_CHAPTER, "L1_RAW_DISCOVERY",
                EditorialP5EFreshRawRoutingPolicy.PROVIDER,
                EditorialP5EFreshRawRoutingPolicy.MODEL, "test-account-fingerprint",
                1, 0, 0, 40_000, EditorialP5RawWireContract.OUTPUT_TOKEN_CAP,
                44_096, BigDecimal.ONE, 60_000L, true, false, false,
                "redacted", "app-owned", issuedAt, issuedAt + 60_000L, true);
    }

    private static RowCounts rowCounts(SQLiteDatabase db) {
        return new RowCounts(
                count(db, "SELECT COUNT(*) FROM editorial_p5c_attempts"),
                count(db, "SELECT COUNT(*) FROM editorial_p5d_authorization_receipts"),
                count(db, "SELECT COUNT(*) FROM editorial_p5d_reconciliation"),
                count(db, "SELECT COUNT(*) FROM editorial_p5d_reconciliation_history"),
                count(db, "SELECT COUNT(*) FROM editorial_p5d_network_lifecycle"),
                count(db, "SELECT COUNT(*) FROM editorial_p5c_attempts "
                        + "WHERE report_bytes IS NOT NULL OR receipt_bytes IS NOT NULL"));
    }

    private static long count(SQLiteDatabase db, String sql, String... args) {
        try (Cursor cursor = db.rawQuery(sql, args.length == 0 ? null : args)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private static String scalar(SQLiteDatabase db, String sql, String... args) {
        try (Cursor cursor = db.rawQuery(sql, args.length == 0 ? null : args)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getString(0);
        }
    }

    private static String privateStaticString(Class<?> owner, String fieldName)
            throws Exception {
        java.lang.reflect.Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private record PreflightFixture(EditorialP5PilotProvider.Request providerRequest,
                                    int contextBytes, int providerCalls) { }

    private record RowCounts(long attempts, long authorizations, long reconciliation,
                             long reconciliationHistory, long lifecycle, long reportOrReceipt) { }
}
