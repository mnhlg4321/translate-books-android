package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireResponse;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;
import com.ml.tblandroidtxt.editorial.pack.EditorialStopDecision;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Candidate-aligned P5E local verification. The first test mutates only the
 * owner-approved reconstructed pilot DB by adding a fresh setup lineage. All
 * provider and fault tests use a disposable isolated DB and storage root.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5EFreshPilotInstrumentedTest {
    private static final String CANONICAL_ASSET =
            "editorial-p2/v5-safe-4.1.3-full-canonical.zip";
    private static final String OLD_SELECTOR = "p5d-raw-mercedes-vol5-001";
    private static final String FRESH_SELECTOR = "p5e-fresh-mercedes-vol5-20260911-01";
    private static final String FRESH_CHAPTER_KEY = "001";
    private static final String FRESH_PROJECT_KEY = "p5e/fresh/20260911/vol5";
    private static final String FRESH_SCOPE_KEY = "p5e/fresh/20260911/vol5/001";
    private static final String CANONICAL_ZIP_SHA256 =
            "b9c65dbeb9d4c4ed46b67d5ec28ff6252cc2bdc4b63bc902904612987ec58987";
    private static final long FRESH_CREATED_AT = 20260911060059L;

    private Context context;
    private TranslationRepository database;
    private EditorialPackStorageLayout storage;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        database = new TranslationRepository(context);
        storage = new EditorialPackStorageLayout(context.getFilesDir().toPath());
    }

    @After public void tearDown() {
        if (database != null) database.close();
    }

    @Test public void freshPilotSetupPersistsNewBindingAndPreservesReconstructedData()
            throws Exception {
        long beforeProjects = count(database, "editorial_projects");
        long beforeBindings = count(database, "editorial_p4_bindings");
        long beforeAttempts = count(database, "editorial_p5c_attempts");
        long beforeReceipts = count(database, "editorial_p5d_authorization_receipts");
        long beforeReconciliation = count(database, "editorial_p5d_reconciliation");

        FreshFixture fixture = ensureFreshPilot();
        EditorialP4Binding oldBinding = new EditorialP4BindingDao(database)
                .findByAttemptRequestSelector(OLD_SELECTOR).orElseThrow();

        assertNotEquals("fresh selector must not reuse the old selector",
                OLD_SELECTOR, FRESH_SELECTOR);
        assertNotEquals("fresh binding must not reuse the old binding",
                oldBinding.bindingIdentity(), fixture.binding.bindingIdentity());
        assertNotEquals("fresh run declaration must not reuse the old run",
                oldBinding.runDeclarationIdentity(), fixture.binding.runDeclarationIdentity());
        assertNotEquals("fresh project must not reuse the old project",
                projectId(oldBinding), fixture.projectId);
        assertEquals(1, new EditorialRepository(database).listChapters(fixture.projectId).size());
        assertEquals(EditorialP4ResumeResult.Code.RESTORED, fixture.resume.code());
        assertEquals(FRESH_SELECTOR, fixture.bindingSelector);
        assertEquals(0L, count(database, "editorial_p5c_attempts"));
        assertEquals(0L, count(database, "editorial_p5d_authorization_receipts"));
        assertEquals(0L, count(database, "editorial_p5d_reconciliation"));

        // Setup is idempotent at the test boundary and cannot create duplicate
        // P4 rows when this class is invoked again on the same approved DB.
        assertTrue(count(database, "editorial_projects") == beforeProjects
                || count(database, "editorial_projects") == beforeProjects + 1);
        assertTrue(count(database, "editorial_p4_bindings") == beforeBindings
                || count(database, "editorial_p4_bindings") == beforeBindings + 1);
        assertEquals(beforeAttempts, count(database, "editorial_p5c_attempts"));
        assertEquals(beforeReceipts, count(database, "editorial_p5d_authorization_receipts"));
        assertEquals(beforeReconciliation, count(database, "editorial_p5d_reconciliation"));

        logFixture("fresh-setup", fixture);
    }

    @Test public void compactRawFakeE2eCommitsOnlyInIsolatedDbAndReplaysWithoutProvider()
            throws Exception {
        FreshFixture current = ensureFreshPilot();
        long currentAttempts = count(database, "editorial_p5c_attempts");

        try (IsolatedFixture isolated = createIsolatedFixture(current)) {
            EditorialP5PilotAuthorization authorization = authorization(
                    isolated.binding, "fake-local-fresh-raw", EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY);
            CompactProvider provider = new CompactProvider(false);
            EditorialP5CExactBindingExecution.Result result = new EditorialP5CExactBindingExecution(
                    isolated.database, isolated.storage).executeRaw(isolated.projectId,
                    FRESH_SELECTOR, FRESH_CHAPTER_KEY, authorization, provider);

            assertEquals(EditorialP5CExactBindingExecution.Status.COMMITTED, result.status());
            assertEquals("P5C_RAW_COMMITTED", result.reasonCode());
            assertNullReconcile(result);
            assertEquals(1, result.providerCalls());
            assertEquals(1, provider.calls);
            assertEquals(1L, count(isolated.database, "editorial_p5c_attempts"));
            assertEquals(1L, count(isolated.database, "editorial_p5d_authorization_receipts"));
            assertEquals(0L, count(isolated.database, "editorial_p5d_reconciliation"));

            EditorialP5L1Output output = result.rawResult().committedResult().output();
            byte[] rawBytes = isolated.sources.stream()
                    .filter(source -> EditorialSafe4Contract.RAW.equals(source.role()))
                    .findFirst().orElseThrow().bytes();
            assertEquals(new String(rawBytes, StandardCharsets.UTF_8), output.beforeText());
            assertEquals(output.beforeText(), output.afterText());
            assertTrue(output.declaredChanges().isEmpty());
            assertTrue(provider.payloads.get(0).contains("wireSchemaVersion"));
            assertFalse(provider.payloads.get(0).contains("beforeText"));
            assertFalse(provider.payloads.get(0).contains(new String(rawBytes, StandardCharsets.UTF_8)));

            CompactProvider noCall = new CompactProvider(true);
            EditorialP5CExactBindingExecution.Result replay = new EditorialP5CExactBindingExecution(
                    isolated.database, isolated.storage).executeRaw(isolated.projectId,
                    FRESH_SELECTOR, FRESH_CHAPTER_KEY,
                    authorization(isolated.binding, "fake-local-fresh-replay",
                            EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY), noCall);
            assertEquals(EditorialP5CExactBindingExecution.Status.ALREADY_COMMITTED, replay.status());
            assertEquals("P5C_RAW_ALREADY_COMMITTED", replay.reasonCode());
            assertEquals(0, replay.providerCalls());
            assertEquals(0, noCall.calls);
            assertEquals(1L, count(isolated.database, "editorial_p5c_attempts"));
        }

        // The fake predecessor is isolated and must not appear in the pilot DB.
        assertEquals(currentAttempts, count(database, "editorial_p5c_attempts"));
        assertEquals(0L, count(database, "editorial_p5d_authorization_receipts"));
        assertEquals(0L, count(database, "editorial_p5d_reconciliation"));
    }

    @Test public void compactRawFaultStopsWithoutPartialCommitOrRepair() throws Exception {
        FreshFixture current = ensureFreshPilot();
        try (IsolatedFixture isolated = createIsolatedFixture(current)) {
            CompactProvider provider = new CompactProvider(false, true);
            EditorialP5PilotAuthorization authorization = authorization(
                    isolated.binding, "fake-local-fresh-invalid", EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY);
            EditorialP5CExactBindingExecution.Result result = new EditorialP5CExactBindingExecution(
                    isolated.database, isolated.storage).executeRaw(isolated.projectId,
                    FRESH_SELECTOR, FRESH_CHAPTER_KEY, authorization, provider);

            assertEquals(EditorialP5CExactBindingExecution.Status.STOP, result.status());
            assertEquals("REPAIR_OUTPUT_SCHEMA_INVALID", result.reasonCode());
            assertEquals(1, result.providerCalls());
            assertEquals(1, provider.calls);
            EditorialP5CAttemptStore.AttemptRecord record = new EditorialP5CAttemptStore(
                    isolated.database).findRecord(provider.attemptIdentity).orElseThrow();
            assertEquals("RECOVERY_REQUIRED", record.status());
            assertEquals(0, record.reportBytes().length);
            assertEquals(0, record.receiptBytes().length);
            assertEquals(0L, count(isolated.database, "editorial_p5d_reconciliation"));
        }
    }

    @Test public void compactRawParserRejectsUnknownTruncatedOversizedChangesAndReplay()
            throws Exception {
        FreshFixture current = ensureFreshPilot();
        try (IsolatedFixture isolated = createIsolatedFixture(current)) {
            EditorialP5PilotProvider.Request request = parserRequest(isolated);
            EditorialP5RawWireResponse valid = validWire(request);
            EditorialP5RawWireResponse parsed = OpenRouterEditorialP5PilotProvider.parseRawOutput(
                    new String(valid.wireBytes(), StandardCharsets.UTF_8), request);
            assertNotNull(parsed);
            assertArrayEquals(valid.wireBytes(), parsed.wireBytes());

            assertThrows("malformed JSON", () -> OpenRouterEditorialP5PilotProvider.parseRawOutput(
                    "{not-json", request));
            String truncated = new String(valid.wireBytes(), StandardCharsets.UTF_8)
                    .substring(0, valid.wireBytes().length - 1);
            assertThrows("truncated JSON", () -> OpenRouterEditorialP5PilotProvider.parseRawOutput(
                    truncated, request));
            assertThrows("oversized wire", () -> OpenRouterEditorialP5PilotProvider.parseRawOutput(
                    "x".repeat(EditorialP5RawWireContract.MAX_WIRE_BYTES + 1), request));

            Map<String, Object> unknown = new LinkedHashMap<>(valid.wireMap());
            unknown.put("sourceText", "forbidden");
            assertThrows("unknown field", () -> OpenRouterEditorialP5PilotProvider.parseRawOutput(
                    canonical(unknown), request));

            Map<String, Object> changes = new LinkedHashMap<>(valid.wireMap());
            changes.put("declaredChanges", List.of(Map.of("lineNumber", 1)));
            assertThrows("declared changes", () -> OpenRouterEditorialP5PilotProvider.parseRawOutput(
                    canonical(changes), request));

            Map<String, Object> wrongAttempt = new LinkedHashMap<>(valid.wireMap());
            wrongAttempt.put("attemptIdentity", "c".repeat(64));
            assertThrows("replay attempt", () -> OpenRouterEditorialP5PilotProvider.parseRawOutput(
                    canonical(wrongAttempt), request));
            assertEquals(0L, count(isolated.database, "editorial_p5c_attempts"));
        }
    }

    private FreshFixture ensureFreshPilot() throws Exception {
        EditorialP4BindingDao bindings = new EditorialP4BindingDao(database);
        EditorialP4Binding oldBinding = bindings.findByAttemptRequestSelector(OLD_SELECTOR)
                .orElseThrow(() -> new AssertionError("historical reconstructed binding is missing"));
        long oldProjectId = projectId(oldBinding);
        EditorialRepository repository = new EditorialRepository(database);
        EditorialRepository.Chapter oldChapter = repository.listChapters(oldProjectId).stream()
                .filter(chapter -> FRESH_CHAPTER_KEY.equals(chapter.chapterKey))
                .findFirst().orElseThrow(() -> new AssertionError("VOL5 chapter 001 is missing"));
        List<EditorialP4InputSource> sources = freshSources(oldBinding, repository, oldChapter);

        EditorialP4Binding binding;
        long projectId;
        EditorialP4BindingResult result = null;
        java.util.Optional<EditorialP4Binding> existing = bindings
                .findByAttemptRequestSelector(FRESH_SELECTOR);
        if (existing.isPresent()) {
            binding = existing.get();
            projectId = projectId(binding);
            assertEquals("fresh selector must be a new project lineage", FRESH_PROJECT_KEY,
                    projectSemanticKey(projectId));
        } else {
            EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(
                    database, storage).resolve(oldBinding.packId(), oldBinding.packVersion())
                    .orElseThrow(() -> new AssertionError("trusted candidate pack is not selectable"));
            result = new EditorialP4BindingTransactionService(database, storage).createSetup(
                    setupRequest(candidate.packId(), candidate.packVersion(), sources, oldBinding));
            assertEquals(EditorialP4BindingResult.Code.APPENDED, result.code());
            assertNotNull(result.binding());
            binding = result.binding();
            projectId = result.projectId();
        }

        assertNotEquals(oldBinding.bindingIdentity(), binding.bindingIdentity());
        List<EditorialRepository.Chapter> freshChapters = repository.listChapters(projectId);
        long chapterId;
        if (freshChapters.isEmpty()) {
            chapterId = insertChapter(database, projectId, sources);
        } else {
            assertEquals(1, freshChapters.size());
            chapterId = freshChapters.get(0).id;
        }

        EditorialP4ResumeResult resume = new EditorialP4BindingTransactionService(database, storage)
                .resumeProject(projectId, FRESH_SELECTOR, sources);
        assertEquals(EditorialP4ResumeResult.Code.RESTORED, resume.code());
        assertNotNull(resume.binding());
        assertEquals(binding.bindingIdentity(), resume.binding().bindingIdentity());
        assertEquals(1, new EditorialRepository(database).listChapters(projectId).size());
        assertEquals(EditorialSafe4Contract.NORMAL_MODE, binding.sourceMode());
        assertEquals("AVAILABLE", binding.glossaryStatus());
        assertEquals("AVAILABLE", binding.pronounStatus());
        assertEquals("NONE", binding.pairContextStatus());
        assertFalse(binding.executionAllowed());
        assertEquals(EditorialP4Binding.NOT_CERTIFIED, binding.certificationState());
        return new FreshFixture(projectId, chapterId, binding, sources, resume,
                FRESH_SELECTOR, result == null ? "ALREADY_EXISTS_OR_READBACK" : result.code().name());
    }

    private List<EditorialP4InputSource> freshSources(EditorialP4Binding oldBinding,
                                                        EditorialRepository repository,
                                                        EditorialRepository.Chapter chapter) {
        Map<String, EditorialRepository.AssetSnapshot> assets = new HashMap<>();
        for (EditorialRepository.AssetSnapshot asset : repository.chapterAssets(chapter.id)) {
            assets.put(asset.role.name(), asset);
        }
        ArrayList<EditorialP4InputSource> sources = new ArrayList<>();
        for (var identity : oldBinding.inputs()) {
            EditorialRepository.AssetSnapshot asset = assets.get(identity.role());
            assertNotNull("missing source asset " + identity.role(), asset);
            byte[] bytes = asset.content.getBytes(StandardCharsets.UTF_8);
            assertEquals(identity.byteLength(), bytes.length);
            assertEquals(identity.sha256(), EditorialCanonicalJson.sha256Hex(bytes));
            sources.add(new EditorialP4InputSource(identity.role(), freshSourceReference(identity.role()),
                    bytes, identity.encoding(), identity.schemaStatus(), identity.ordinal()));
        }
        assertEquals(4, sources.size());
        return List.copyOf(sources);
    }

    private static String freshSourceReference(String role) {
        return "p5e-fresh://mercedes-vol5/20260911/001/" + role.toLowerCase(java.util.Locale.ROOT);
    }

    private EditorialP4SetupRequest setupRequest(String packId, String packVersion,
                                                  List<EditorialP4InputSource> sources,
                                                  EditorialP4Binding oldBinding) {
        return new EditorialP4SetupRequest(FRESH_SELECTOR, "MERCEDES FRESH PILOT",
                "VOL 5 20260911", packId, packVersion, FRESH_PROJECT_KEY, FRESH_SCOPE_KEY,
                sources, EditorialSafe4Contract.NORMAL_MODE,
                oldBinding.glossaryStatus(), oldBinding.pronounStatus(),
                oldBinding.pairContextStatus(),
                "USER_CONFIRMED_NORMAL_P5E_FRESH_PILOT", "EDITORIAL_PILOT",
                "L1_SOURCE_PREFLIGHT", "manifest-attestation-v1", EditorialLineageNodeKind.ROOT,
                null, FRESH_CREATED_AT);
    }

    private IsolatedFixture createIsolatedFixture(FreshFixture current) throws Exception {
        String name = "p5e-fresh-isolated-" + UUID.randomUUID() + ".db";
        Path root = context.getCacheDir().toPath().resolve("p5e-fresh-isolated-" + UUID.randomUUID());
        TranslationRepository isolatedDatabase = new TranslationRepository(context, name);
        EditorialPackStorageLayout isolatedStorage = new EditorialPackStorageLayout(root);
        try {
            PackFixture pack = readPack(CANONICAL_ASSET);
            assertEquals(CANONICAL_ZIP_SHA256, EditorialCanonicalJson.sha256Hex(pack.zipBytes));
            EditorialPackImportResult imported = new EditorialPackImportService(isolatedDatabase,
                    isolatedStorage, new EditorialEngineProfileResolver(
                    BundledEditorialEngineContractProfileRegistry.load()))
                    .importZip(new ByteArrayInputStream(pack.zipBytes));
            assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, imported.state());
            String isolatedEvaluationId = imported.importId() + ":compatibility:v1";
            EditorialPackCompatibilityEvaluation isolatedEvaluation =
                    new EditorialPackCompatibilityEvaluationDao(isolatedDatabase)
                            .findByEvaluationId(isolatedEvaluationId)
                            .orElseThrow(() -> new AssertionError("isolated importer evaluation missing"));
            assertEquals(imported.importId(), isolatedEvaluation.importId());
            EditorialP4BindingResult setup = new EditorialP4BindingTransactionService(
                    isolatedDatabase, isolatedStorage).createSetup(setupRequest(
                    pack.manifest.packId(), pack.manifest.version(), current.sources,
                    new EditorialP4BindingDao(database).findByAttemptRequestSelector(OLD_SELECTOR)
                            .orElseThrow()));
            assertEquals(EditorialP4BindingResult.Code.APPENDED, setup.code());
            assertNotEquals(current.binding.bindingIdentity(), setup.binding().bindingIdentity());
            assertEquals(isolatedEvaluationId, setup.binding().compatibilityEvaluationId());
            long chapterId = insertChapter(isolatedDatabase, setup.projectId(), current.sources);
            EditorialP4ResumeResult resume = new EditorialP4BindingTransactionService(
                    isolatedDatabase, isolatedStorage).resumeProject(setup.projectId(),
                    FRESH_SELECTOR, current.sources);
            assertEquals(EditorialP4ResumeResult.Code.RESTORED, resume.code());
            return new IsolatedFixture(isolatedDatabase, isolatedStorage, root, name,
                    setup.projectId(), chapterId, setup.binding(), current.sources, pack.manifest);
        } catch (Throwable failure) {
            isolatedDatabase.close();
            context.deleteDatabase(name);
            deleteTree(root);
            throw failure;
        }
    }

    private EditorialP5PilotProvider.Request parserRequest(IsolatedFixture fixture) {
        EditorialP5PilotRequest request = pilotRequest(fixture);
        Map<String, byte[]> visible = new LinkedHashMap<>();
        visible.put(EditorialSafe4Contract.RAW,
                request.source(EditorialSafe4Contract.RAW).bytes());
        visible.put(EditorialSafe4Contract.GLOSSARY,
                request.source(EditorialSafe4Contract.GLOSSARY).bytes());
        EditorialP5PilotProvider.Request.Context context = new EditorialP5PilotProvider.Request.Context(
                fixture.binding.bindingIdentity(), fixture.binding.runDeclarationIdentity(),
                request.manifestFingerprint(), request.bundleIdentity(), request.predecessorIdentity(),
                request.stableAnchors(), request.populationIds());
        return new EditorialP5PilotProvider.Request(request.attemptIdentity(),
                EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC, "FAKE_PROVIDER", "fake/model",
                "L1_RAW_DISCOVERY", "b".repeat(64), visible, request.authority(),
                EditorialP5RawWireContract.SCHEMA_VERSION, FRESH_CHAPTER_KEY, "", context);
    }

    private EditorialP5RawWireResponse validWire(EditorialP5PilotProvider.Request request) {
        EditorialLedgerValidator.Entry finding = new EditorialLedgerValidator.Entry(
                request.context().populationIds().get(0), "PROCESSED", List.of("fake-evidence"), true);
        Map<String, String> gates = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) gates.put(gate, "PASS");
        return new EditorialP5RawWireResponse(EditorialP5RawWireContract.SCHEMA_VERSION,
                request.attemptIdentity(), request.requestEnvelopeHash(), List.of(finding), gates,
                List.of("fake-evidence"), List.of(), 0,
                EditorialStopDecision.continueWithoutStop("L1", "COVERAGE", "FAKE_LOCAL_VALIDATED"),
                true);
    }

    private EditorialP5PilotRequest pilotRequest(IsolatedFixture fixture) {
        Map<EditorialPackFileRole, byte[]> authority = new java.util.EnumMap<>(EditorialPackFileRole.class);
        for (EditorialPackManifest.FileEntry file : fixture.manifest.fileEntries()) {
            try {
                authority.put(file.role(), Files.readAllBytes(
                        fixture.storage.immutableEntry(fixture.binding.canonicalPackHash(), file.path())));
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
                new EditorialP5PilotRequest.PackAuthority(authority), FRESH_CHAPTER_KEY,
                EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY, sources,
                fixture.binding.runDeclarationIdentity(), List.of("chapter:" + FRESH_CHAPTER_KEY),
                List.of("population:" + FRESH_CHAPTER_KEY), true,
                EditorialP5RawWireContract.OUTPUT_TOKEN_CAP);
    }

    private static String schemaId(String role) {
        return EditorialSafe4Contract.GLOSSARY.equals(role) ? "safe4.full.glossary.v1"
                : EditorialSafe4Contract.PRONOUN.equals(role) ? "safe4.full.pronoun.v1" : "text.v1";
    }

    private long insertChapter(TranslationRepository target, long projectId,
                               List<EditorialP4InputSource> sources) {
        SQLiteDatabase db = target.editorialWritableDatabase();
        db.beginTransaction();
        try {
            EditorialP4InputSource raw = sources.stream()
                    .filter(source -> EditorialSafe4Contract.RAW.equals(source.role()))
                    .findFirst().orElseThrow();
            ContentValues chapter = new ContentValues();
            chapter.put("project_id", projectId);
            chapter.put("chapter_key", FRESH_CHAPTER_KEY);
            chapter.put("title", "Mercedes VOL5 fresh pilot chapter 001");
            chapter.put("state", EditorialSafe4Workflow.ChapterState.SAFE4_BLOCKED.name());
            chapter.put("raw_hash", EditorialCanonicalJson.sha256Hex(raw.bytes()));
            chapter.put("created_at", FRESH_CREATED_AT + 1);
            chapter.put("updated_at", FRESH_CREATED_AT + 1);
            long chapterId = db.insertOrThrow("editorial_chapters", null, chapter);
            for (EditorialP4InputSource source : sources) {
                byte[] bytes = source.bytes();
                ContentValues asset = new ContentValues();
                asset.put("chapter_id", chapterId);
                asset.put("role", source.role());
                asset.put("source_uri", source.sourceReference());
                asset.put("display_name", source.role().toLowerCase(java.util.Locale.ROOT) + ".txt");
                asset.put("sha256", EditorialCanonicalJson.sha256Hex(bytes));
                asset.put("size_bytes", bytes.length);
                asset.put("content", new String(bytes, StandardCharsets.UTF_8));
                asset.put("created_at", FRESH_CREATED_AT + 1);
                db.insertOrThrow("editorial_assets", null, asset);
            }
            db.setTransactionSuccessful();
            return chapterId;
        } finally {
            db.endTransaction();
        }
    }

    private EditorialP5PilotAuthorization authorization(EditorialP4Binding binding, String id,
                                                          EditorialP5PilotRequest.Phase phase) {
        return new EditorialP5PilotAuthorization(id, binding.bindingIdentity(),
                binding.runDeclarationIdentity(), binding.canonicalPackHash(),
                binding.canonicalProfileHash(), binding.compatibilityEvaluationId(),
                FRESH_CHAPTER_KEY, phase.name(), "FAKE_PROVIDER", "fake/model",
                "fake-account-fingerprint", 1, 0, 0, 200_000,
                EditorialP5RawWireContract.OUTPUT_TOKEN_CAP, 200_000, BigDecimal.ONE,
                60_000L, true, false, false, "HASH_ONLY", "TEST_STOP_AUTHORITY",
                0L, Long.MAX_VALUE, true);
    }

    private static String canonical(Map<String, Object> value) {
        return EditorialCanonicalJson.canonicalize(value);
    }

    private static void assertThrows(String label, ThrowingAction action) throws Exception {
        try {
            action.run();
            fail(label + " must be rejected");
        } catch (JSONException | IllegalArgumentException expected) {
            // Typed parser rejection is the assertion.
        }
    }

    private static void assertNullReconcile(EditorialP5CExactBindingExecution.Result result) {
        assertEquals(null, result.reconcileResult());
    }

    private long projectId(EditorialP4Binding binding) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT project_row_id FROM editorial_p4_bindings WHERE binding_identity=?",
                new String[]{binding.bindingIdentity()})) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private String projectSemanticKey(long projectId) {
        // The service stores the semantic key in the revision identity. The
        // project name is also read back to ensure this is the expected fresh
        // row without exposing source contents.
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT series_name,volume_name FROM editorial_projects WHERE id=?",
                new String[]{String.valueOf(projectId)})) {
            assertTrue(cursor.moveToFirst());
            assertEquals("MERCEDES FRESH PILOT", cursor.getString(0));
            assertEquals("VOL 5 20260911", cursor.getString(1));
            return FRESH_PROJECT_KEY;
        }
    }

    private static long count(TranslationRepository database, String table) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private void logFixture(String label, FreshFixture fixture) {
        LogProxy.i(label + " project=" + prefix(Long.toString(fixture.projectId))
                + " binding=" + prefix(fixture.binding.bindingIdentity())
                + " run=" + prefix(fixture.binding.runDeclarationIdentity())
                + " raw=" + prefix(rawHash(fixture.sources)));
    }

    private static String rawHash(List<EditorialP4InputSource> sources) {
        return sources.stream().filter(source -> EditorialSafe4Contract.RAW.equals(source.role()))
                .findFirst().map(source -> EditorialCanonicalJson.sha256Hex(source.bytes())).orElse("");
    }

    private static String prefix(String value) {
        return value == null ? "" : value.substring(0, Math.min(12, value.length()));
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

    private record PackFixture(byte[] zipBytes, EditorialPackManifest manifest) {
        PackFixture {
            zipBytes = zipBytes.clone();
        }
    }

    private record FreshFixture(long projectId, long chapterId, EditorialP4Binding binding,
                                List<EditorialP4InputSource> sources, EditorialP4ResumeResult resume,
                                String bindingSelector, String setupOutcome) {
    }

    private record IsolatedFixture(TranslationRepository database,
                                   EditorialPackStorageLayout storage, Path root, String databaseName,
                                   long projectId, long chapterId, EditorialP4Binding binding,
                                   List<EditorialP4InputSource> sources,
                                   EditorialPackManifest manifest) implements AutoCloseable {
        @Override public void close() throws Exception {
            database.close();
            ApplicationProvider.getApplicationContext().deleteDatabase(databaseName);
            deleteTree(root);
        }
    }

    private final class CompactProvider implements EditorialP5PilotProvider {
        private final boolean noCall;
        private final boolean invalidOutput;
        private int calls;
        private String attemptIdentity = "";
        private final List<String> payloads = new ArrayList<>();

        private CompactProvider(boolean noCall) {
            this(noCall, false);
        }

        private CompactProvider(boolean noCall, boolean invalidOutput) {
            this.noCall = noCall;
            this.invalidOutput = invalidOutput;
        }

        @Override public Response call(Request request) throws Exception {
            if (noCall) throw new AssertionError("replay must not call provider");
            calls++;
            attemptIdentity = request.attemptIdentity();
            if (invalidOutput) {
                return new Response("fake-invalid-response", new byte[]{'{', '}'}, "stop", true,
                        128, 800, 0, 928, new BigDecimal("0.01"), null, false, true, true);
            }
            if (request.context() == null) throw new AssertionError("fake request context missing");
            if (request.visibleSources().containsKey(EditorialSafe4Contract.DRAFT)
                    || request.visibleSources().containsKey(EditorialSafe4Contract.PRONOUN)) {
                throw new AssertionError("RAW fake request leaked hidden source");
            }
            EditorialP5RawWireResponse wire = validWire(request);
            String payload = new String(wire.wireBytes(), StandardCharsets.UTF_8);
            payloads.add(payload);
            EditorialP5L1Output output = OpenRouterEditorialP5PilotProvider.parseOutput(payload, request);
            return new Response("fake-compact-response", wire.wireBytes(), "stop", true,
                    128, 800, 0, 928, new BigDecimal("0.01"), output, true, true, true);
        }
    }

    @FunctionalInterface private interface ThrowingAction {
        void run() throws Exception;
    }

    /** Avoid retaining a log dependency in assertions; only redacted prefixes are logged. */
    private static final class LogProxy {
        private static void i(String message) {
            android.util.Log.i("P5E_FRESH_LOCAL", message);
        }
    }
}
