package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialL3Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialLedgerValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5L1Output;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * One-chapter pipeline on a real database: committed L1 -> L2_EDIT -> L3 ->
 * FINAL -> verified .txt export. All providers are local fakes; nothing leaves
 * the device.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialChapterFinalCoordinatorInstrumentedTest {
    private static final String CANONICAL_ASSET = "editorial-p2/v5-safe-4.1.3-full-canonical.zip";
    private static final String CHAPTER_KEY = "final-coordinator-chapter-1";
    private static final String SELECTOR = "final-coordinator-binding-selector";
    private static final String DRAFT_LINE = "original draft bytes";
    private static final String EDITED_LINE = "edited draft bytes";
    private static final EditorialL2Execution.Budget BUDGET =
            new EditorialL2Execution.Budget(1_000_000, 4_000, new BigDecimal("1.00"), 60_000L);

    private Context context;
    private String databaseName;
    private TranslationRepository database;
    private Path storageRoot;
    private EditorialPackStorageLayout storage;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "final-coordinator-" + UUID.randomUUID() + ".db";
        database = new TranslationRepository(context, databaseName);
        storageRoot = context.getCacheDir().toPath().resolve("final-coordinator-" + UUID.randomUUID());
        storage = new EditorialPackStorageLayout(storageRoot);
    }

    @After public void tearDown() throws Exception {
        if (database != null) database.close();
        context.deleteDatabase(databaseName);
        deleteTree(storageRoot);
    }

    @Test public void runToFinalBeforeL1StopsWithoutProviderCalls() throws Exception {
        BindingFixture fixture = createBoundChapter();
        ScriptedProvider l2 = new ScriptedProvider();
        ScriptedProvider l3 = new ScriptedProvider();
        EditorialChapterFinalCoordinator.Result result = new EditorialChapterFinalCoordinator(database, storage)
                .runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY, BUDGET, l2, BUDGET, l3);
        assertEquals(EditorialChapterFinalCoordinator.Stage.L1_INCOMPLETE, result.stage());
        assertEquals("INPUT_REPORT_L1_NOT_COMMITTED", result.reasonCode());
        assertFalse(result.finalReady());
        assertNull(result.finalArtifact());
        assertEquals(0, result.providerCalls());
        assertEquals(0, l2.calls + l3.calls);
    }

    @Test public void l1ThroughFinalExportsAndResumesIdempotently() throws Exception {
        BindingFixture fixture = createBoundChapter();
        commitL1(fixture);

        ScriptedProvider l2 = new ScriptedProvider();
        ScriptedProvider l3 = new ScriptedProvider();
        EditorialChapterFinalCoordinator coordinator = new EditorialChapterFinalCoordinator(database, storage);
        EditorialChapterFinalCoordinator.Result first = coordinator.runToFinal(fixture.projectId, SELECTOR,
                CHAPTER_KEY, BUDGET, l2, BUDGET, l3);

        assertEquals(first.reasonCode(), EditorialChapterFinalCoordinator.Stage.FINAL, first.stage());
        assertTrue(first.finalReady());
        assertEquals("L3_FINAL_COMMITTED", first.reasonCode());
        assertEquals(3, first.providerCalls());
        assertEquals(1, l2.calls);
        assertEquals(2, l3.calls);
        EditorialL2Execution.Committed fin = first.finalArtifact();
        assertNotNull(fin);
        assertEquals(EDITED_LINE, new String(fin.viL2Bytes(), StandardCharsets.UTF_8));

        Path target = context.getCacheDir().toPath().resolve("final-export-" + UUID.randomUUID() + ".txt");
        try {
            EditorialChapterFinalCoordinator.ExportResult export = EditorialChapterFinalCoordinator.exportTxt(fin,
                    () -> Files.newOutputStream(target), () -> Files.newInputStream(target));
            assertTrue(export.reasonCode(), export.verified());
            assertEquals(fin.viL2Sha256(), export.sha256());
            assertEquals(fin.viL2Bytes().length, export.byteCount());
            assertArrayEquals(fin.viL2Bytes(), Files.readAllBytes(target));
        } finally {
            Files.deleteIfExists(target);
        }

        // Same repository: resume is idempotent and dispatches nothing.
        ScriptedProvider noL2 = new ScriptedProvider();
        ScriptedProvider noL3 = new ScriptedProvider();
        EditorialChapterFinalCoordinator.Result second = coordinator.runToFinal(fixture.projectId, SELECTOR,
                CHAPTER_KEY, BUDGET, noL2, BUDGET, noL3);
        assertEquals(second.reasonCode(), EditorialChapterFinalCoordinator.Stage.FINAL, second.stage());
        assertTrue(second.finalReady());
        assertEquals(0, second.providerCalls());
        assertEquals(0, noL2.calls + noL3.calls);
        assertEquals(fin.viL2Sha256(), second.finalArtifact().viL2Sha256());

        // Fresh repository over the same database file: FINAL is read back from the durable store.
        database.close();
        database = new TranslationRepository(context, databaseName);
        ScriptedProvider afterL2 = new ScriptedProvider();
        ScriptedProvider afterL3 = new ScriptedProvider();
        EditorialChapterFinalCoordinator.Result third = new EditorialChapterFinalCoordinator(database, storage)
                .runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY, BUDGET, afterL2, BUDGET, afterL3);
        assertEquals(third.reasonCode(), EditorialChapterFinalCoordinator.Stage.FINAL, third.stage());
        assertEquals(0, third.providerCalls());
        assertEquals(0, afterL2.calls + afterL3.calls);
        assertEquals(fin.viL2Sha256(), third.finalArtifact().viL2Sha256());
        EditorialL2Execution.Committed stored = new EditorialPhaseArtifactStore(database, CHAPTER_KEY,
                EditorialPhaseArtifactStore.L3_PHASE).findCommitted(fin.attemptIdentity()).orElseThrow();
        assertEquals(fin.viL2Sha256(), stored.viL2Sha256());
        assertArrayEquals(fin.viL2Bytes(), stored.viL2Bytes());
    }

    @Test public void inspectReportsEachStageFromDurableRowsWithoutDispatching() throws Exception {
        BindingFixture fixture = createBoundChapter();
        EditorialChapterFinalCoordinator coordinator = new EditorialChapterFinalCoordinator(database, storage);

        EditorialChapterFinalCoordinator.Inspection beforeL1 = coordinator.inspect(fixture.projectId, SELECTOR, CHAPTER_KEY);
        assertEquals(EditorialChapterProgress.Stage.L1_INCOMPLETE, beforeL1.progress().stage());
        assertNull(beforeL1.finalArtifact());

        commitL1(fixture);
        EditorialChapterFinalCoordinator.Inspection afterL1 = coordinator.inspect(fixture.projectId, SELECTOR, CHAPTER_KEY);
        assertEquals(EditorialChapterProgress.Stage.L2, afterL1.progress().stage());
        assertEquals(EditorialChapterProgress.NextAction.RUN_STAGE_WITH_AUTHORIZATION, afterL1.progress().next());
        assertFalse(afterL1.progress().finalReady());

        EditorialChapterFinalCoordinator.Result run = coordinator.runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY,
                BUDGET, new ScriptedProvider(), BUDGET, new ScriptedProvider());
        assertTrue(run.reasonCode(), run.finalReady());

        // Fresh repository over the same file: progress and FINAL are read from durable rows only.
        database.close();
        database = new TranslationRepository(context, databaseName);
        EditorialChapterFinalCoordinator.Inspection done = new EditorialChapterFinalCoordinator(database, storage)
                .inspect(fixture.projectId, SELECTOR, CHAPTER_KEY);
        assertEquals(EditorialChapterProgress.Stage.FINAL, done.progress().stage());
        assertEquals(EditorialChapterProgress.NextAction.VIEW_AND_EXPORT, done.progress().next());
        assertNotNull(done.finalArtifact());
        assertEquals(run.finalArtifact().viL2Sha256(), done.finalArtifact().viL2Sha256());
        assertArrayEquals(run.finalArtifact().viL2Bytes(), done.finalArtifact().viL2Bytes());
    }

    @Test public void interruptedL3IsUnknownStateAndResumeNeverRedispatches() throws Exception {
        BindingFixture fixture = createBoundChapter();
        commitL1(fixture);
        EditorialChapterFinalCoordinator coordinator = new EditorialChapterFinalCoordinator(database, storage);

        // An Error escapes the execution boundary like a process death: the L3 row stays CLAIMED.
        EditorialL2Execution.Provider dying = request -> {
            if (!EditorialL2Execution.PHASE.equals(request.phase())) throw new AssertionError("simulated process death");
            return new ScriptedProvider().call(request);
        };
        try {
            coordinator.runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY, BUDGET, dying, BUDGET, dying);
            org.junit.Assert.fail("the simulated death must escape");
        } catch (AssertionError expected) {
            assertEquals("simulated process death", expected.getMessage());
        }

        EditorialChapterFinalCoordinator.Inspection interrupted = coordinator.inspect(fixture.projectId, SELECTOR, CHAPTER_KEY);
        assertEquals(EditorialChapterProgress.Stage.L3, interrupted.progress().stage());
        assertEquals(EditorialChapterProgress.StageState.IN_FLIGHT_UNKNOWN, interrupted.progress().l3());
        assertEquals(EditorialChapterProgress.NextAction.OWNER_RECOVERY_DECISION, interrupted.progress().next());
        assertNull(interrupted.finalArtifact());

        ScriptedProvider l2 = new ScriptedProvider();
        ScriptedProvider l3 = new ScriptedProvider();
        EditorialChapterFinalCoordinator.Result resumed = coordinator.runToFinal(fixture.projectId, SELECTOR,
                CHAPTER_KEY, BUDGET, l2, BUDGET, l3);
        assertFalse(resumed.finalReady());
        assertEquals(EditorialChapterFinalCoordinator.Stage.L3, resumed.stage());
        assertEquals(0, l2.calls + l3.calls);
    }

    @Test public void failedProviderCallLeavesRecoveryRequiredWithTheTypedReason() throws Exception {
        BindingFixture fixture = createBoundChapter();
        commitL1(fixture);
        EditorialChapterFinalCoordinator coordinator = new EditorialChapterFinalCoordinator(database, storage);
        EditorialL2Execution.Provider failing = request -> { throw new IOException("transport down"); };
        EditorialChapterFinalCoordinator.Result result = coordinator.runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY,
                BUDGET, failing, BUDGET, new ScriptedProvider());
        assertFalse(result.finalReady());
        assertEquals(EditorialChapterFinalCoordinator.Stage.L2, result.stage());

        EditorialChapterProgress.Progress progress = coordinator.inspect(fixture.projectId, SELECTOR, CHAPTER_KEY).progress();
        assertEquals(EditorialChapterProgress.StageState.RECOVERY_REQUIRED, progress.l2());
        assertEquals("RETRY_L2_PROVIDER_CALL_FAILED", progress.reasonCode());
        assertEquals(EditorialChapterProgress.StopClass.RETRY_REQUIRED, progress.stopClass());
        assertEquals(EditorialChapterProgress.NextAction.OWNER_RECOVERY_DECISION, progress.next());
    }

    @Test public void failedExportKeepsTheStoredFinalUntouched() throws Exception {
        BindingFixture fixture = createBoundChapter();
        commitL1(fixture);
        EditorialChapterFinalCoordinator coordinator = new EditorialChapterFinalCoordinator(database, storage);
        EditorialL2Execution.Committed fin = coordinator.runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY,
                BUDGET, new ScriptedProvider(), BUDGET, new ScriptedProvider()).finalArtifact();
        assertNotNull(fin);

        EditorialChapterFinalCoordinator.ExportResult writeFailure = EditorialChapterFinalCoordinator.exportTxt(fin,
                () -> { throw new IOException("destination refused"); }, () -> new ByteArrayInputStream(new byte[0]));
        assertFalse(writeFailure.verified());
        assertEquals("EXPORT_WRITE_FAILED", writeFailure.reasonCode());

        EditorialChapterFinalCoordinator.ExportResult mismatch = EditorialChapterFinalCoordinator.exportTxt(fin,
                java.io.OutputStream::nullOutputStream, () -> new ByteArrayInputStream("different".getBytes(StandardCharsets.UTF_8)));
        assertFalse(mismatch.verified());
        assertEquals("EXPORT_READBACK_MISMATCH", mismatch.reasonCode());

        EditorialChapterFinalCoordinator.Inspection after = coordinator.inspect(fixture.projectId, SELECTOR, CHAPTER_KEY);
        assertEquals(EditorialChapterProgress.Stage.FINAL, after.progress().stage());
        assertArrayEquals(fin.viL2Bytes(), after.finalArtifact().viL2Bytes());
        assertEquals(fin.viL2Sha256(), after.finalArtifact().viL2Sha256());
    }

    private void commitL1(BindingFixture fixture) throws Exception {
        EditorialP5PilotRequest rawRequest = request(fixture, EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                fixture.binding.runDeclarationIdentity());
        EditorialP5PilotRequest reconcileRequest = request(fixture, EditorialP5PilotRequest.Phase.L1_RECONCILE,
                rawRequest.attemptIdentity());
        FakeL1Provider provider = new FakeL1Provider(Map.of(
                "L1_RAW_DISCOVERY", l1Output(rawRequest), "L1_RECONCILE", l1Output(reconcileRequest)));
        EditorialP5CExactBindingExecution.Result result = new EditorialP5CExactBindingExecution(database, storage)
                .execute(fixture.projectId, SELECTOR, CHAPTER_KEY,
                        authorization(fixture.binding, "auth-raw", "L1_RAW_DISCOVERY"),
                        authorization(fixture.binding, "auth-reconcile", "L1_RECONCILE"), provider);
        assertEquals(result.reasonCode(), EditorialP5CExactBindingExecution.Status.COMMITTED, result.status());
        assertEquals(2, provider.calls);
    }

    // ---- fake L2 / L3 provider: scripted wire JSON echoing the received attempt identity ----

    private static final class ScriptedProvider implements EditorialL2Execution.Provider {
        int calls;

        @Override public Response call(Request request) {
            calls++;
            String wire;
            switch (request.phase()) {
                case EditorialL2Execution.PHASE -> wire = l2Wire(request.attemptIdentity());
                case EditorialL3Execution.REAUDIT_PHASE -> wire = reauditWire(request.attemptIdentity());
                case EditorialL3Execution.RECONCILE_PHASE -> wire = reconcileWire(request.attemptIdentity());
                default -> throw new AssertionError("unexpected phase " + request.phase());
            }
            return new Response(wire.getBytes(StandardCharsets.UTF_8), "stop", true, 10, 10,
                    new BigDecimal("0.01"), true);
        }
    }

    private static String l2Wire(String attempt) {
        return "{\"wireSchemaVersion\":\"" + EditorialL2Execution.WIRE_SCHEMA_VERSION + "\","
                + "\"attemptIdentity\":\"" + attempt + "\","
                + "\"changes\":[{\"changeId\":\"c1\",\"errorId\":\"e1\",\"line\":1,"
                + "\"before\":\"" + DRAFT_LINE + "\",\"after\":\"" + EDITED_LINE + "\","
                + "\"reason\":\"fix wording\",\"dialogue\":false,\"status\":\"CLOSED\"}],"
                + "\"preserved\":[],"
                + "\"disposition\":{\"disposition\":\"CONTINUE\",\"reasonCode\":\"L2_OK\",\"stopClass\":\"NONE\"}}";
    }

    private static String reauditWire(String attempt) {
        return "{\"wireSchemaVersion\":\"" + EditorialL3Execution.REAUDIT_WIRE + "\","
                + "\"attemptIdentity\":\"" + attempt + "\","
                + "\"candidates\":[{\"candidateId\":\"u1\",\"ledger\":\"UNIT\",\"line\":1,\"status\":\"PROCESSED\"}]}";
    }

    private static String reconcileWire(String attempt) {
        return "{\"wireSchemaVersion\":\"" + EditorialL3Execution.RECONCILE_WIRE + "\","
                + "\"attemptIdentity\":\"" + attempt + "\","
                + "\"resolutions\":[],\"changes\":[],\"preserved\":[],"
                + "\"adversarialCoverage\":[{\"probeId\":\"cov1\",\"finding\":\"none\",\"verdict\":\"NO_DEFECT\"}],"
                + "\"adversarialRegression\":[{\"probeId\":\"reg1\",\"finding\":\"none\",\"verdict\":\"NO_DEFECT\"}],"
                + "\"disposition\":{\"disposition\":\"CONTINUE\",\"reasonCode\":\"L3_OK\",\"stopClass\":\"NONE\"}}";
    }

    // ---- fixture (mirrors EditorialP5CExactBindingFakeE2EInstrumentedTest) ----

    private BindingFixture createBoundChapter() throws Exception {
        PackFixture pack = readPack(CANONICAL_ASSET);
        EditorialPackImportResult imported = new EditorialPackImportService(database, storage,
                new EditorialEngineProfileResolver(BundledEditorialEngineContractProfileRegistry.load()))
                .importZip(new ByteArrayInputStream(pack.zipBytes));
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, imported.state());
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                .resolve(pack.manifest.packId(), pack.manifest.version()).orElseThrow();
        List<EditorialP4InputSource> sources = sourceInputs();
        EditorialP4BindingResult bindingResult = new EditorialP4BindingTransactionService(database, storage)
                .createSetup(new EditorialP4SetupRequest(SELECTOR, "Final coordinator", "4.1.3",
                        candidate.packId(), candidate.packVersion(), "final/fake", "final/scope",
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
            chapter.put("title", "Final coordinator chapter");
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
                input("DRAFT", "draft", DRAFT_LINE),
                input("GLOSSARY", "glossary", "term\ttarget\tPOS\tnote"),
                input("PRONOUN", "pronoun", "from\ttarget\tgender\tnumber"));
    }

    private static EditorialP4InputSource input(String role, String id, String text) {
        return new EditorialP4InputSource(role, "content://final/" + id,
                text.getBytes(StandardCharsets.UTF_8), "UTF-8", "VALID", 0L);
    }

    private EditorialP5PilotRequest request(BindingFixture fixture, EditorialP5PilotRequest.Phase phase,
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

    private static EditorialP5PilotAuthorization authorization(EditorialP4Binding binding, String id, String phase) {
        return new EditorialP5PilotAuthorization(id, binding.bindingIdentity(),
                binding.runDeclarationIdentity(), binding.canonicalPackHash(),
                binding.canonicalProfileHash(), binding.compatibilityEvaluationId(), CHAPTER_KEY,
                phase, "FAKE_PROVIDER", "fake/model", "fake-account-fingerprint", 1, 1, 0,
                200_000, 2_000, 200_000, BigDecimal.ONE, 60_000L, true, false, false,
                "HASH_ONLY", "TEST_STOP_AUTHORITY", 0L, Long.MAX_VALUE, true);
    }

    private static EditorialP5L1Output l1Output(EditorialP5PilotRequest request) {
        EditorialLedgerValidator.Request ledger = new EditorialLedgerValidator.Request(
                request.populationIds(), List.of(new EditorialLedgerValidator.Entry(
                        request.populationIds().get(0), "PROCESSED", List.of("fake-evidence"), false)));
        Map<String, String> gates = new LinkedHashMap<>();
        for (String gate : EditorialSafe4Contract.GATE_IDS) gates.put(gate, "PASS");
        String rawText = request.phase().equals(EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY.name())
                ? new String(request.source(EditorialSafe4Contract.RAW).bytes(), StandardCharsets.UTF_8)
                : new String(request.source(EditorialSafe4Contract.DRAFT).bytes(), StandardCharsets.UTF_8);
        return new EditorialP5L1Output("safe4.full.report-l1.v1",
                EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION, request.binding().bindingIdentity(),
                request.manifestFingerprint(), CHAPTER_KEY, "L1", request.bundleIdentity(),
                request.predecessorIdentity(), request.stableAnchors(), ledger, gates,
                List.of(), List.of(), rawText, rawText, 0,
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
                                  EditorialPackManifest manifest, List<EditorialP4InputSource> sources) { }

    private static final class FakeL1Provider implements EditorialP5PilotProvider {
        private final Map<String, EditorialP5L1Output> outputs;
        int calls;

        FakeL1Provider(Map<String, EditorialP5L1Output> outputs) {
            this.outputs = new HashMap<>(outputs);
        }

        @Override public Response call(Request request) {
            calls++;
            EditorialP5L1Output output = outputs.get(request.phase());
            if (output == null) throw new AssertionError("unexpected fake-provider call");
            return new Response("fake-response-" + calls,
                    ("redacted-response-" + calls).getBytes(StandardCharsets.UTF_8), "stop",
                    true, 40, 20, 60, BigDecimal.ZERO, output, true);
        }
    }
}
