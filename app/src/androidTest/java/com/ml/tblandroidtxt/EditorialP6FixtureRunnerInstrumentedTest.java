package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialContractRevision;
import com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead;
import com.ml.tblandroidtxt.editorial.pack.EditorialL1Ledger;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialL3Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialRawInventory;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Opt-in emulator harness. It reads only four fixture sources plus a label-free runtime manifest. */
@RunWith(AndroidJUnit4.class)
public final class EditorialP6FixtureRunnerInstrumentedTest {
    private static final String CANONICAL_ASSET = "editorial-p2/v5-safe-4.1.3-full-canonical.zip";
    private static final List<String> SOURCE_NAMES = List.of("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv");
    private static final int FAKE_PROVIDER_CALL_LIMIT = 8;

    private Context context;
    private TranslationRepository database;
    private String databaseName;
    private Path storageRoot;
    private EditorialPackStorageLayout storage;

    @Test public void runFixture() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        Assume.assumeTrue("P6 fixture runner is opt-in", "YES".equalsIgnoreCase(args.getString("p6_fixture_run", "")));
        String runId = uuid(args.getString("p6_run_id", ""));
        String fixtureId = args.getString("p6_fixture_id", "");
        String mode = args.getString("p6_mode", "CHAIN");
        if (!fixtureId.matches("fx-[ah][0-9]{2}")) throw new IllegalArgumentException("P6_FIXTURE_ID_INVALID");
        if (!Set.of("L1_ONLY", "L2_ONLY", "L3_ONLY", "CHAIN").contains(mode)) {
            throw new IllegalArgumentException("P6_FIXTURE_MODE_INVALID");
        }

        context = ApplicationProvider.getApplicationContext();
        Path externalRoot = context.getExternalFilesDir(null).toPath().toAbsolutePath().normalize();
        Path fixtureInputRoot = java.nio.file.Paths.get("/data/local/tmp/p6-fixtures").toAbsolutePath().normalize();
        Path runInputRoot = fixtureInputRoot.resolve(runId).normalize();
        Path fixtureRoot = runInputRoot.resolve(fixtureId).normalize();
        Path outputRoot = externalRoot.resolve("p6-fixture-results").resolve(runId).resolve(fixtureId).normalize();
        if (!fixtureRoot.startsWith(runInputRoot) || !runInputRoot.startsWith(fixtureInputRoot)
                || !outputRoot.startsWith(externalRoot)
                || fixtureRoot.toString().contains("6.FINAL") || Files.exists(outputRoot)) {
            throw new IllegalStateException("P6_FIXTURE_PATH_REFUSED");
        }
        Map<String, Object> runtime = EditorialCanonicalJson.parseObject(readPushedFile(
                runInputRoot.resolve(fixtureId + ".runtime.json")));
        if (!fixtureId.equals(string(runtime, "fixtureId"))) throw new IllegalStateException("P6_RUNTIME_ID_MISMATCH");
        String chapter = string(runtime, "chapter");
        Map<String, Object> manifestFiles = object(runtime.get("files"));
        if (!manifestFiles.keySet().equals(Set.copyOf(SOURCE_NAMES))) throw new IllegalStateException("P6_RUNTIME_FILE_SET_INVALID");
        Map<String, byte[]> input = new LinkedHashMap<>();
        for (String name : SOURCE_NAMES) {
            Path source = fixtureRoot.resolve(name).normalize();
            if (!source.startsWith(fixtureRoot)) throw new IllegalStateException("P6_FIXTURE_SOURCE_PATH_REFUSED");
            byte[] bytes = readPushedFile(source);
            Map<String, Object> metadata = object(manifestFiles.get(name));
            if (bytes.length != integer(metadata, "bytes")
                    || !EditorialCanonicalJson.sha256Hex(bytes).equals(string(metadata, "sha256"))) {
                throw new IllegalStateException("P6_FIXTURE_SOURCE_HASH_MISMATCH");
            }
            input.put(name, bytes);
        }

        Files.createDirectories(outputRoot);
        Path reportPath = outputRoot.resolve("report-l1.json");
        Path promptPath = outputRoot.resolve("l2-edit-prompt.txt");
        EditorialChainBudgets budgets = EditorialChainBudgets.fullLedgerRecommended();
        String groupId = EditorialCanonicalJson.sha256Hex((runId + "|" + fixtureId + "|" + mode + "|P6-OFFLINE-GROUP")
                .getBytes(StandardCharsets.UTF_8));
        EditorialP6GroupSpendLedger spend = new EditorialP6GroupSpendLedger(
                outputRoot.resolve("spend-ledger.jsonl"), groupId, budgets.chainMaximumCost());
        String opaqueChapterKey = "p6-fixture-" + EditorialCanonicalJson.sha256Hex((runId + "|" + fixtureId)
                .getBytes(StandardCharsets.UTF_8)).substring(0, 16);
        FixtureSetup fixture = createSetup(fixtureId, chapter, input, runId, opaqueChapterKey);
        FakeL1 l1 = new FakeL1();
        FakeL2 l2 = new FakeL2(promptPath);
        FakeL3 l3 = new FakeL3();
        EditorialP5PilotProvider budgetedL1 = new EditorialP6BudgetedL1Provider(l1, spend, budgets);
        EditorialL2Execution.Provider budgetedL2 = new EditorialP6BudgetedPhaseProvider(l2, spend, budgets, true);
        EditorialL2Execution.Provider budgetedL3 = new EditorialP6BudgetedPhaseProvider(l3, spend, budgets, false);
        int fakeCalls;
        int measuredCalls;
        byte[] finalBytes;
        String reason;
        boolean valid;
        String stage;

        if ("CHAIN".equals(mode)) {
            EditorialP6L1Authorizations.Pair consent = EditorialP6L1Authorizations.create(
                    fixture.binding(), fixture.chapterKey(), "offline-fake-provider", budgets, System.currentTimeMillis());
            EditorialChapterFinalCoordinator.Result result = new EditorialChapterFinalCoordinator(database, storage)
                    .runFromL1(fixture.projectId(), fixture.selector(), fixture.chapterKey(), budgets,
                            consent.raw(), consent.reconcile(), budgetedL1, budgetedL2, budgetedL3);
            reason = result.reasonCode();
            valid = result.finalReady() && result.finalArtifact() != null;
            finalBytes = valid ? result.finalArtifact().viL2Bytes() : input.get("DRAFT.txt");
            measuredCalls = result.providerCalls();
            fakeCalls = l1.calls + l2.calls + l3.calls;
            stage = "CHAIN";
        } else {
            EditorialP5CExactBindingExecution l1Execution = EditorialP5CExactBindingExecution.forContract(
                    database, storage, EditorialContractRevision.L1_LEDGER_V2);
            EditorialP6L1Authorizations.Pair consent = EditorialP6L1Authorizations.create(
                    fixture.binding(), fixture.chapterKey(), "offline-fake-provider", budgets, System.currentTimeMillis());
            EditorialP5CExactBindingExecution.Result raw = l1Execution.executeRaw(
                    fixture.projectId(), fixture.selector(), fixture.chapterKey(), consent.raw(), budgetedL1);
            EditorialP5CExactBindingExecution.Result reconcile = raw.accepted()
                    ? l1Execution.executeReconcile(fixture.projectId(), fixture.selector(), fixture.chapterKey(),
                    consent.reconcile(), budgetedL1) : raw;
            if (!reconcile.accepted()) throw new IllegalStateException("P6_L1_PREDECESSOR_FAILED:" + reconcile.reasonCode());
            EditorialP5CExactBindingExecution.CommittedL1 committedL1 = l1Execution.committedL1(
                    fixture.projectId(), fixture.selector(), fixture.chapterKey()).orElseThrow(
                    () -> new IllegalStateException("P6_REPORT_L1_READBACK_FAILED"));
            Files.write(reportPath, committedL1.reportL1Bytes());
            if ("L1_ONLY".equals(mode)) {
                finalBytes = input.get("DRAFT.txt");
                reason = "L1_REPORT_COMMITTED";
                valid = true;
                measuredCalls = raw.providerCalls() + reconcile.providerCalls();
                stage = "L1";
            } else {
                EditorialL2Execution.Request l2Request = new EditorialL2Execution.Request(
                        committedL1.context(), committedL1.reportL1AttemptIdentity(), committedL1.reportL1Bytes(), Set.of());
                EditorialL2Execution.Result l2Result = new EditorialL2Execution().execute(l2Request,
                        budgets.discovery(), budgets.edit(), budgets.finalRead(), budgetedL2,
                        new EditorialPhaseArtifactStore(database, fixture.chapterKey(), EditorialPhaseArtifactStore.L2_PHASE));
                if (!l2Result.accepted() || l2Result.committed() == null) {
                    throw new IllegalStateException("P6_L2_PREDECESSOR_FAILED:" + l2Result.reasonCode());
                }
                if ("L2_ONLY".equals(mode)) {
                    finalBytes = l2Result.committed().viL2Bytes();
                    reason = "L2_COMMITTED";
                    valid = true;
                    measuredCalls = raw.providerCalls() + reconcile.providerCalls() + l2Result.providerCalls();
                    stage = "L2";
                } else {
                    EditorialL3Execution.Request l3Request = new EditorialL3Execution.Request(
                            committedL1.context(), committedL1.reportL1AttemptIdentity(), committedL1.reportL1Bytes(),
                            l2Result.committed(), Set.of());
                    EditorialL3Execution.Result l3Result = new EditorialL3Execution().execute(l3Request,
                            budgets.reaudit(), budgets.reconcile(), budgets.finalRead(), budgetedL3,
                            new EditorialPhaseArtifactStore(database, fixture.chapterKey(), EditorialPhaseArtifactStore.L3_PHASE));
                    if (!l3Result.accepted() || l3Result.committed() == null) {
                        throw new IllegalStateException("P6_L3_PREDECESSOR_FAILED:" + l3Result.reasonCode());
                    }
                    finalBytes = l3Result.committed().viL2Bytes();
                    reason = "L3_FINAL_COMMITTED";
                    valid = true;
                    measuredCalls = raw.providerCalls() + reconcile.providerCalls()
                            + l2Result.providerCalls() + l3Result.providerCalls();
                    stage = "L3";
                }
            }
            fakeCalls = l1.calls + l2.calls + l3.calls;
        }

        Files.write(outputRoot.resolve("final.txt"), finalBytes);
        Map<String, Object> structural = new LinkedHashMap<>();
        structural.put("valid", valid);
        structural.put("reasonCode", reason);
        structural.put("stage", stage);
        structural.put("providerCalls", measuredCalls);
        structural.put("stops", valid ? List.of() : List.of(reason));
        Files.write(outputRoot.resolve("structural.json"), EditorialCanonicalJson.canonicalize(structural).getBytes(StandardCharsets.UTF_8));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fixtureId", fixtureId);
        metadata.put("mode", mode);
        metadata.put("providerKind", "FAKE_OFFLINE");
        metadata.put("actualProviderCalls", BigDecimal.ZERO);
        metadata.put("fakeProviderCalls", BigDecimal.valueOf(fakeCalls));
        metadata.put("finalSha256", EditorialCanonicalJson.sha256Hex(finalBytes));
        Files.write(outputRoot.resolve("run-metadata.json"), EditorialCanonicalJson.canonicalize(metadata).getBytes(StandardCharsets.UTF_8));
        if (!Files.exists(reportPath)) throw new IllegalStateException("P6_REPORT_L1_EVIDENCE_MISSING");
        if (!Files.exists(promptPath)) Files.write(promptPath, new byte[0]);
        assertTrue("offline fake provider must be called only through the production stage boundary", fakeCalls <= FAKE_PROVIDER_CALL_LIMIT);
        assertTrue("production artifacts must validate: " + reason, valid);
    }

    @After public void tearDown() throws Exception {
        if (database != null) database.close();
        if (context != null && databaseName != null) context.deleteDatabase(databaseName);
        if (storageRoot != null) deleteTree(storageRoot);
    }

    private FixtureSetup createSetup(String fixtureId, String chapter, Map<String, byte[]> input, String runId,
                                    String chapterKey) throws Exception {
        databaseName = "p6-fixture-" + runId.substring(0, 8) + "-" + fixtureId + ".db";
        database = new TranslationRepository(context, databaseName);
        storageRoot = context.getCacheDir().toPath().resolve("p6-fixture-pack-" + UUID.randomUUID());
        storage = new EditorialPackStorageLayout(storageRoot);
        byte[] zip = readAssetZip(CANONICAL_ASSET);
        EditorialPackImportResult imported = new EditorialPackImportService(database, storage,
                new com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver(
                        BundledEditorialEngineContractProfileRegistry.load()))
                .importZip(new ByteArrayInputStream(zip));
        if (imported.state() != EditorialPackImportState.STORED_READY_FOR_CERTIFICATION) {
            throw new IllegalStateException("P6_CANONICAL_PACK_IMPORT_FAILED");
        }
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                .resolve(imported.packId(), imported.version()).orElseThrow(
                        () -> new IllegalStateException("P6_CANONICAL_PACK_NOT_SELECTABLE"));
        List<EditorialP4InputSource> sources = new ArrayList<>();
        long ordinal = 0L;
        for (String name : SOURCE_NAMES) {
            String role = name.substring(0, name.lastIndexOf('.'));
            sources.add(new EditorialP4InputSource(role, "content://p6-fixture/" + fixtureId + "/" + role.toLowerCase(),
                    input.get(name), "UTF-8", "VALID", ordinal++));
        }
        String selector = "p6-fixture-" + runId.substring(0, 8) + "-" + fixtureId;
        EditorialP4BindingResult result = new EditorialP4BindingTransactionService(database, storage).createSetup(
                new EditorialP4SetupRequest(selector, "P6 offline fixtures", "MERCEDES VOL 5",
                        candidate.packId(), candidate.packVersion(), "p6-fixture:" + runId + ":" + fixtureId,
                        "p6-fixture-scope:" + fixtureId, sources, EditorialSafe4Contract.NORMAL_MODE,
                        "AVAILABLE", "AVAILABLE", "NONE", "USER_CONFIRMED_NORMAL", "EDITORIAL_PILOT",
                        "L1_SOURCE_PREFLIGHT", "p6-fixture-manifest-v1", EditorialLineageNodeKind.ROOT,
                        null, System.currentTimeMillis()));
        if (result.code() != EditorialP4BindingResult.Code.APPENDED || result.binding() == null) {
            throw new IllegalStateException("P6_CREATE_SETUP_FAILED:" + result.code());
        }
        insertChapter(result.projectId(), chapterKey, fixtureId, chapter, sources);
        return new FixtureSetup(result.projectId(), chapterKey, selector, result.binding(), sources,
                candidate.manifest());
    }

    private void insertChapter(long projectId, String chapterKey, String fixtureId, String chapterName,
                               List<EditorialP4InputSource> sources) {
        SQLiteDatabase db = database.editorialWritableDatabase();
        long now = System.currentTimeMillis();
        db.beginTransaction();
        try {
            ContentValues chapter = new ContentValues();
            chapter.put("project_id", projectId);
            chapter.put("chapter_key", chapterKey);
            chapter.put("title", "Offline fixture " + fixtureId + " / " + chapterName);
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
        } finally {
            db.endTransaction();
        }
    }

    private static byte[] readAssetZip(String asset) throws IOException {
        try (InputStream source = ApplicationProvider.<Context>getApplicationContext().getAssets().open(asset);
             ZipInputStream zip = new ZipInputStream(source)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    while (true) {
                        int count = zip.read(buffer);
                        if (count < 0) break;
                        output.write(buffer, 0, count);
                    }
                }
                zip.closeEntry();
            }
            return output.toByteArray();
        }
    }

    private static Map<String, Object> rawWire(String attempt, EditorialRawInventory.Inventory inventory) {
        return map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE, "attemptIdentity", attempt,
                "coverage", coverage(inventory), "candidates", List.of());
    }

    private static Map<String, Object> reconcileWire(String attempt, EditorialRawInventory.Inventory inventory) {
        return map("wireSchemaVersion", EditorialL1Ledger.RECONCILE_WIRE, "attemptIdentity", attempt,
                "coverage", coverage(inventory), "resolutions", List.of(), "findings", List.of(),
                "speakerRecords", List.of(), "protectedSpans", List.of(),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OFFLINE_FAKE", "stopClass", "NONE"));
    }

    private static List<Object> coverage(EditorialRawInventory.Inventory inventory) {
        List<EditorialRawInventory.Unit> units = inventory.units();
        if (units.isEmpty()) throw new IllegalStateException("P6_FIXTURE_RAW_EMPTY");
        return List.of(map("from", units.get(0).id(), "to", units.get(units.size() - 1).id(), "status", "PROCESSED"));
    }

    private static byte[] l2Wire(EditorialL2Execution.Provider.Request request) {
        if (EditorialL2Execution.DISCOVERY_PHASE.equals(request.phase())) {
            EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(request.visibleSources().get(EditorialSafe4Contract.RAW));
            String unit = inventory.units().get(0).id();
            return json(map("wireSchemaVersion", EditorialL2Execution.DISCOVERY_WIRE_V2,
                    "attemptIdentity", request.attemptIdentity(), "coverage", coverage(inventory),
                    "candidates", List.of(map("candidateId", "c1", "ledger", "UNIT", "unitId", unit,
                            "note", "neutral offline structural candidate"))));
        }
        if (EditorialFinalRead.L2_PHASE.equals(request.phase())) return finalReadWire(request);
        return json(map("wireSchemaVersion", EditorialL2Execution.WIRE_SCHEMA_VERSION_V2,
                "attemptIdentity", request.attemptIdentity(),
                "resolutions", List.of(map("candidateId", "c1", "status", "PROCESSED")),
                "findingResolutions", List.of(), "changes", List.of(), "preserved", List.of(),
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OFFLINE_FAKE", "stopClass", "NONE")));
    }

    private static byte[] l3Wire(EditorialL2Execution.Provider.Request request) {
        EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(request.visibleSources().get(EditorialSafe4Contract.RAW));
        if (EditorialL3Execution.REAUDIT_PHASE.equals(request.phase())) {
            String vi = new String(request.visibleSources().get("VI_L2"), StandardCharsets.UTF_8);
            int line = firstNonEmptyLine(EditorialFinalRead.lines(vi.getBytes(StandardCharsets.UTF_8)));
            return json(map("wireSchemaVersion", EditorialL3Execution.REAUDIT_WIRE_V2,
                    "attemptIdentity", request.attemptIdentity(), "coverage", coverage(inventory),
                    "candidates", List.of(map("candidateId", "c1", "ledger", "UNIT",
                            "unitId", inventory.units().get(0).id(), "viLine", BigDecimal.valueOf(line),
                            "status", "PROCESSED", "note", "neutral offline structural candidate"))));
        }
        if (EditorialFinalRead.L3_PHASE.equals(request.phase())) return finalReadWire(request);
        List<String> viLines = EditorialFinalRead.lines(request.visibleSources().get("VI_L2"));
        List<EditorialRawInventory.Unit> units = inventory.units();
        List<Integer> viNumbers = new ArrayList<>();
        for (int i = 0; i < viLines.size() && viNumbers.size() < 6; i++) {
            if (!viLines.get(i).isBlank()) viNumbers.add(i + 1);
        }
        if (units.size() < 6 || viNumbers.size() < 6) throw new IllegalStateException("P6_FIXTURE_TOO_SHORT_FOR_L3_PROBES");
        List<Object> probes = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            EditorialRawInventory.Unit unit = units.get(i);
            String rawQuote = snippet(unit.text());
            String viQuote = snippet(viLines.get(viNumbers.get(i) - 1));
            probes.add(map("probeId", "P" + (i + 1), "kind", i < 3 ? "COVERAGE" : "REGRESSION",
                    "rawUnits", List.of(unit.id()), "viStart", BigDecimal.valueOf(viNumbers.get(i)),
                    "viEnd", BigDecimal.valueOf(viNumbers.get(i)), "scope", "neutral offline coverage sample",
                    "contrast", "compare source and translation anchors", "rawQuote", rawQuote, "viQuote", viQuote,
                    "verdict", "NO_DEFECT", "action", "NONE"));
        }
        return json(map("wireSchemaVersion", EditorialL3Execution.RECONCILE_WIRE_V2,
                "attemptIdentity", request.attemptIdentity(),
                "resolutions", List.of(map("candidateId", "c1", "status", "PROCESSED")),
                "carriedResolutions", List.of(), "changes", List.of(), "preserved", List.of(), "probes", probes,
                "disposition", map("disposition", "CONTINUE", "reasonCode", "OFFLINE_FAKE", "stopClass", "NONE")));
    }

    private static byte[] finalReadWire(EditorialL2Execution.Provider.Request request) {
        byte[] target = request.visibleSources().get(EditorialFinalRead.TARGET_ROLE);
        List<String> lines = EditorialFinalRead.lines(target);
        List<Object> tails = new ArrayList<>();
        for (Integer number : EditorialFinalRead.probeLines(target)) {
            String value = lines.get(number - 1);
            tails.add(map("line", BigDecimal.valueOf(number), "tail",
                    value.length() <= EditorialFinalRead.TAIL_LENGTH ? value : value.substring(value.length() - EditorialFinalRead.TAIL_LENGTH)));
        }
        return json(map("wireSchemaVersion", EditorialFinalRead.WIRE, "attemptIdentity", request.attemptIdentity(),
                "readSha256", EditorialCanonicalJson.sha256Hex(target), "probeTails", tails,
                "verdict", "CLEAN", "defects", List.of()));
    }

    private static int firstNonEmptyLine(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) if (!lines.get(i).isBlank()) return i + 1;
        throw new IllegalStateException("P6_FIXTURE_TRANSLATION_EMPTY");
    }

    private static String snippet(String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("P6_FIXTURE_PROBE_TEXT_EMPTY");
        return value.length() <= EditorialL1Ledger.MAX_QUOTE ? value : value.substring(0, EditorialL1Ledger.MAX_QUOTE);
    }

    private static byte[] json(Map<String, Object> value) {
        return EditorialCanonicalJson.canonicalize(value).getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }

    private static String uuid(String value) {
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equals(value.toLowerCase(java.util.Locale.ROOT))) throw new IllegalArgumentException();
            return parsed.toString();
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("P6_RUN_ID_INVALID");
        }
    }

    private static byte[] readPushedFile(Path path) throws IOException {
        String quotedPath = "'" + path.toString().replace("'", "'\\''") + "'";
        ParcelFileDescriptor[] pipes = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommandRwe("cat " + quotedPath);
        if (pipes == null || pipes.length != 3) throw new IOException("P6_FIXTURE_SHELL_BRIDGE_UNAVAILABLE");
        try {
            pipes[1].close();
            byte[] output = readDescriptor(pipes[0]);
            byte[] errors = readDescriptor(pipes[2]);
            if (errors.length != 0) {
                throw new IOException("P6_FIXTURE_SHELL_READ_FAILED: "
                        + new String(errors, StandardCharsets.UTF_8).trim());
            }
            if (output.length == 0) throw new IOException("P6_FIXTURE_SHELL_READ_EMPTY");
            return output;
        } finally {
            for (ParcelFileDescriptor pipe : pipes) {
                if (pipe != null) try { pipe.close(); } catch (IOException ignored) { }
            }
        }
    }

    private static byte[] readDescriptor(ParcelFileDescriptor descriptor) throws IOException {
        try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }

    @SuppressWarnings("unchecked") private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("P6_RUNTIME_OBJECT_INVALID");
        return (Map<String, Object>) value;
    }

    private static String string(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (!(raw instanceof String text) || text.isBlank()) throw new IllegalArgumentException("P6_RUNTIME_FIELD_INVALID");
        return text;
    }

    private static long integer(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (!(raw instanceof BigDecimal number)) throw new IllegalArgumentException("P6_RUNTIME_FIELD_INVALID");
        return number.longValueExact();
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private record FixtureSetup(long projectId, String chapterKey, String selector, EditorialP4Binding binding,
                                List<EditorialP4InputSource> sources, com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest manifest) { }

    private static final class FakeL1 implements EditorialP5PilotProvider {
        int calls;
        @Override public Response call(Request request) {
            calls++;
            byte[] raw = request.visibleSources().get(EditorialSafe4Contract.RAW);
            EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(raw);
            Map<String, Object> wire = "L1_RAW_DISCOVERY".equals(request.phase())
                    ? rawWire(request.attemptIdentity(), inventory) : reconcileWire(request.attemptIdentity(), inventory);
            return new Response("offline-fake-" + request.phase(), json(wire), "stop", true,
                    0, 0, 0, BigDecimal.ZERO, null, true);
        }
    }

    private static final class FakeL2 implements EditorialL2Execution.Provider {
        final Path promptPath;
        int calls;
        FakeL2(Path promptPath) { this.promptPath = promptPath; }
        @Override public Response call(Request request) throws Exception {
            calls++;
            PromptPair prompt = OpenRouterEditorialL2Provider.buildPrompt(request);
            if (EditorialL2Execution.PHASE.equals(request.phase())) {
                Files.write(promptPath, ("SYSTEM\n" + prompt.system + "\nUSER\n" + prompt.user).getBytes(StandardCharsets.UTF_8));
            }
            return new Response(l2Wire(request), "stop", true, 0, 0, BigDecimal.ZERO, true);
        }
    }

    private static final class FakeL3 implements EditorialL2Execution.Provider {
        int calls;
        @Override public Response call(Request request) {
            calls++;
            return new Response(l3Wire(request), "stop", true, 0, 0, BigDecimal.ZERO, true);
        }
    }
}
