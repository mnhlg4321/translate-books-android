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
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;
import com.ml.tblandroidtxt.editorial.pack.EditorialStopDecision;

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
import java.util.EnumMap;
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
import static org.junit.Assert.assertTrue;

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
        return new EditorialP5PilotAuthorization(id, binding.bindingIdentity(),
                binding.runDeclarationIdentity(), binding.canonicalPackHash(),
                binding.canonicalProfileHash(), binding.compatibilityEvaluationId(), CHAPTER_KEY,
                phase, "FAKE_PROVIDER", "fake/model", "fake-account-fingerprint", 1, 1, 0,
                200_000, 2_000, 200_000, BigDecimal.ONE, 60_000L, true, false, false,
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
}
