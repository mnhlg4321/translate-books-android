package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

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
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialRawInventory;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
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
    private boolean preserveL1Database;
    private boolean preserveL1Storage;

    @Test public void responseCaptureRetainsExactContentBytes() throws Exception {
        Path root = Files.createTempDirectory(contextFilesRoot(), "p6-response-capture-test-");
        byte[] content = "{\"model\":\"text-only\"}".getBytes(StandardCharsets.UTF_8);
        try {
            ResponseCapture capture = new ResponseCapture(root);
            capture.record("L1_RECONCILE", content);
            Path saved = root.resolve("001-L1_RECONCILE.json");
            assertArrayEquals(content, Files.readAllBytes(saved));
        } finally {
            deleteTree(root);
        }
    }

    private Path contextFilesRoot() {
        return ApplicationProvider.getApplicationContext().getCacheDir().toPath();
    }

    @Test public void runFixture() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        Assume.assumeTrue("P6 fixture runner is opt-in", "YES".equalsIgnoreCase(args.getString("p6_fixture_run", "")));
        String runId = uuid(args.getString("p6_run_id", ""));
        String fixtureId = args.getString("p6_fixture_id", "");
        String mode = args.getString("p6_mode", "CHAIN");
        if (!fixtureId.matches("fx-[ah][0-9]{2}")) throw new IllegalArgumentException("P6_FIXTURE_ID_INVALID");
        if (!Set.of("L1_ONLY", "L2_ONLY", "L3_ONLY", "L1_THEN_L2", "CHAIN").contains(mode)) {
            throw new IllegalArgumentException("P6_FIXTURE_MODE_INVALID");
        }
        String l1SourceRunId = args.getString("p6_l1_source_run_id", "");
        if (l1SourceRunId != null && !l1SourceRunId.isBlank()) l1SourceRunId = uuid(l1SourceRunId);
        boolean reuseL1State = "L1_THEN_L2".equals(mode) && l1SourceRunId != null && !l1SourceRunId.isBlank();
        if ("L1_THEN_L2".equals(mode)
                && "YES".equalsIgnoreCase(args.getString("p6_fixture_live", "")) && !reuseL1State) {
            throw new IllegalArgumentException("P6_L1_THEN_L2_LIVE_REQUIRES_G1_STATE");
        }
        if (!"L1_THEN_L2".equals(mode) && l1SourceRunId != null && !l1SourceRunId.isBlank()) {
            throw new IllegalArgumentException("P6_L1_SOURCE_MODE_MISMATCH");
        }

        context = ApplicationProvider.getApplicationContext();
        boolean liveMode = "YES".equalsIgnoreCase(args.getString("p6_fixture_live", ""));
        boolean fakeInvalidL1 = "YES".equalsIgnoreCase(args.getString("p6_fake_invalid_l1", ""));
        if (fakeInvalidL1 && liveMode) {
            throw new IllegalArgumentException("P6_FAKE_INVALID_L1_LIVE_FORBIDDEN");
        }
        if (fakeInvalidL1 && !"L1_ONLY".equals(mode)) {
            throw new IllegalArgumentException("P6_FAKE_INVALID_L1_MODE_INVALID");
        }
        if (liveMode && "L2_ONLY".equals(mode)) {
            throw new IllegalArgumentException("P6_LIVE_L2_ONLY_REQUIRES_REUSED_G1_L1_STATE");
        }
        AppSettings liveSettings = null;
        if (liveMode) {
            liveSettings = SettingsStore.load(context).copy();
            String preflight = EditorialP6FixtureLivePreflight.check(liveSettings,
                    args.getString("p6_expected_endpoint_account_fingerprint", ""));
            if (!EditorialP6FixtureLivePreflight.MATCH.equals(preflight)) {
                throw new IllegalStateException(preflight);
            }
        }
        Path externalRoot = context.getExternalFilesDir(null).toPath().toAbsolutePath().normalize();
        Path appFilesRoot = context.getFilesDir().toPath().toAbsolutePath().normalize();
        Path fixtureInputRoot = appFilesRoot.resolve("p6-fixtures").normalize();
        Path runInputRoot = fixtureInputRoot.resolve(runId).normalize();
        Path fixtureRoot = runInputRoot.resolve(fixtureId).normalize();
        Path outputRoot = externalRoot.resolve("p6-fixture-results").resolve(runId).resolve(fixtureId).normalize();
        if (!fixtureRoot.startsWith(runInputRoot) || !runInputRoot.startsWith(fixtureInputRoot)
                || !outputRoot.startsWith(externalRoot)
                || fixtureRoot.toString().contains("6.FINAL") || Files.exists(outputRoot)) {
            throw new IllegalStateException("P6_FIXTURE_PATH_REFUSED");
        }
        Map<String, Object> runtime = EditorialCanonicalJson.parseObject(Files.readAllBytes(
                runInputRoot.resolve(fixtureId + ".runtime.json")));
        if (!fixtureId.equals(string(runtime, "fixtureId"))) throw new IllegalStateException("P6_RUNTIME_ID_MISMATCH");
        String chapter = string(runtime, "chapter");
        Map<String, Object> manifestFiles = object(runtime.get("files"));
        if (!manifestFiles.keySet().equals(Set.copyOf(SOURCE_NAMES))) throw new IllegalStateException("P6_RUNTIME_FILE_SET_INVALID");
        Map<String, byte[]> input = new LinkedHashMap<>();
        for (String name : SOURCE_NAMES) {
            Path source = fixtureRoot.resolve(name).normalize();
            if (!source.startsWith(fixtureRoot)) throw new IllegalStateException("P6_FIXTURE_SOURCE_PATH_REFUSED");
            byte[] bytes = Files.readAllBytes(source);
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
        PromptCapture promptCapture = new PromptCapture(outputRoot.resolve("prompts"), promptPath);
        ResponseCapture responseCapture = new ResponseCapture(outputRoot.resolve("responses"));
        ProviderMetrics metrics = new ProviderMetrics();
        EditorialChainBudgets budgets = EditorialChainBudgets.fullLedgerRecommended();
        String groupId = args.getString("p6_group_id", liveMode ? "" : "P6-OFFLINE-" + runId);
        if (liveMode && (groupId == null || groupId.isBlank())) throw new IllegalArgumentException("P6_LIVE_GROUP_ID_REQUIRED");
        if (!groupId.matches("[A-Za-z0-9._-]{3,100}")) throw new IllegalArgumentException("P6_SPEND_GROUP_INVALID");
        String capText = args.getString("p6_group_maximum_usd", liveMode ? "" : budgets.chainMaximumCost().toPlainString());
        if (liveMode && (capText == null || capText.isBlank())) throw new IllegalArgumentException("P6_LIVE_GROUP_CAP_REQUIRED");
        BigDecimal groupMaximum;
        try {
            groupMaximum = new BigDecimal(capText);
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("P6_SPEND_GROUP_CAP_INVALID");
        }
        if (groupMaximum.signum() <= 0 || groupMaximum.compareTo(new BigDecimal("1.00")) > 0) {
            throw new IllegalArgumentException("P6_SPEND_GROUP_CAP_INVALID");
        }
        Path groupLedgerPath = externalRoot.resolve("p6-spend-ledger-groups")
                .resolve(groupId + ".jsonl").normalize();
        if (!groupLedgerPath.startsWith(externalRoot)) throw new IllegalArgumentException("P6_SPEND_GROUP_PATH_REFUSED");
        EditorialP6GroupSpendLedger spend = new EditorialP6GroupSpendLedger(
                groupLedgerPath, groupId, groupMaximum);
        String databaseRunId = reuseL1State ? l1SourceRunId : runId;
        String opaqueChapterKey = "p6-fixture-" + EditorialCanonicalJson.sha256Hex((databaseRunId + "|" + fixtureId)
                .getBytes(StandardCharsets.UTF_8)).substring(0, 16);
        FixtureSetup fixture = reuseL1State
                ? openExistingL1Setup(fixtureId, chapter, input, databaseRunId, opaqueChapterKey)
                : createSetup(fixtureId, chapter, input, databaseRunId, opaqueChapterKey);
        FakeL1 l1 = new FakeL1(fakeInvalidL1);
        FakeL2 l2 = new FakeL2();
        FakeL3 l3 = new FakeL3();
        List<EditorialP4InputSource> reloadedSources = currentAssetSources(fixture);
        EditorialP4ResumeResult sourcePreflight = new EditorialP4BindingTransactionService(database, storage)
                .resumeProject(fixture.projectId(), fixture.selector(), reloadedSources);
        if (!"RESTORED".equals(sourcePreflight.code().name())) {
            throw new IllegalStateException("P6_FIXTURE_P4_PREFLIGHT_FAILED:" + sourcePreflight.code()
                    + ":" + sourcePreflight.detail() + ":fields="
                    + String.join(",", sourceIdentityDrifts(fixture, reloadedSources))
                    + ":lengths=" + sourceIdentityLengths(fixture, reloadedSources)
                    + ":sha256=" + sourceIdentityHashes(fixture, reloadedSources));
        }
        EditorialP5PilotProvider l1Delegate = l1;
        EditorialL2Execution.Provider l2Delegate = l2;
        EditorialL2Execution.Provider l3Delegate = l3;
        boolean liveL1 = liveMode && ("L1_ONLY".equals(mode) || "CHAIN".equals(mode));
        boolean liveL2 = liveMode && ("L2_ONLY".equals(mode) || "L1_THEN_L2".equals(mode) || "CHAIN".equals(mode));
        boolean liveL3 = liveMode && ("L3_ONLY".equals(mode) || "CHAIN".equals(mode));
        if (liveL1) {
            l1Delegate = new OpenRouterEditorialP6L1Provider(
                    OpenRouterEditorialP5PilotProvider.withFreshRawLifecyclePersistence(
                            liveSettings, budgets.l1Raw().maximumOutputTokens(), database),
                    OpenRouterEditorialP5PilotProvider.withFreshReconcileLifecyclePersistence(
                            liveSettings, budgets.l1Reconcile().maximumOutputTokens(), database));
        }
        if (liveL2) {
            l2Delegate = new OpenRouterEditorialL2Provider(liveSettings);
        }
        if (liveL3) {
            l3Delegate = new OpenRouterEditorialL3Provider(liveSettings);
        }
        EditorialP5PilotProvider budgetedL1 = new EditorialP6BudgetedL1Provider(
                new PromptRecordingL1Provider(l1Delegate, promptCapture, responseCapture, liveL1 ? metrics : null), spend, budgets);
        EditorialL2Execution.Provider budgetedL2 = new EditorialP6BudgetedPhaseProvider(
                new PromptRecordingPhaseProvider(l2Delegate, promptCapture, responseCapture, liveL2 ? metrics : null, true),
                spend, budgets, true);
        EditorialL2Execution.Provider budgetedL3 = new EditorialP6BudgetedPhaseProvider(
                new PromptRecordingPhaseProvider(l3Delegate, promptCapture, responseCapture, liveL3 ? metrics : null, false),
                spend, budgets, false);
        int fakeCalls;
        int measuredCalls;
        byte[] finalBytes;
        String reason;
        boolean valid;
        String stage;
        List<String> stops = new ArrayList<>();
        boolean reportRequired = true;

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
            EditorialP5CExactBindingExecution l1Execution = EditorialP5CExactBindingExecution.forContract(
                    database, storage, EditorialContractRevision.CURRENT_LEDGER);
            EditorialP5CExactBindingExecution.CommittedL1 committedL1 = l1Execution.committedL1(
                    fixture.projectId(), fixture.selector(), fixture.chapterKey()).orElse(null);
            if (committedL1 == null) throw new IllegalStateException("P6_REPORT_L1_READBACK_FAILED:"
                    + result.stage() + ":" + result.reasonCode() + ":providerCalls="
                    + result.providerCalls() + ":fakeCalls=" + fakeCalls);
            Files.write(reportPath, committedL1.reportL1Bytes());
            stage = "CHAIN";
        } else {
          try {
            EditorialP5CExactBindingExecution l1Execution = EditorialP5CExactBindingExecution.forContract(
                    database, storage, EditorialContractRevision.CURRENT_LEDGER);
            EditorialP5CExactBindingExecution.CommittedL1 committedL1;
            int l1Calls = 0;
            if (reuseL1State) {
                committedL1 = l1Execution.committedL1(fixture.projectId(), fixture.selector(), fixture.chapterKey())
                        .orElseThrow(() -> new IllegalStateException("P6_REUSED_REPORT_L1_MISSING"));
            } else {
                EditorialP6L1Authorizations.Pair consent = EditorialP6L1Authorizations.create(
                        fixture.binding(), fixture.chapterKey(), "offline-fake-provider", budgets, System.currentTimeMillis());
                EditorialP5CExactBindingExecution.Result raw = l1Execution.executeRaw(
                        fixture.projectId(), fixture.selector(), fixture.chapterKey(), consent.raw(), budgetedL1);
                EditorialP5CExactBindingExecution.Result reconcile = raw.accepted()
                        ? l1Execution.executeReconcile(fixture.projectId(), fixture.selector(), fixture.chapterKey(),
                        consent.reconcile(), budgetedL1) : raw;
                if (!reconcile.accepted()) {
                    int calls = raw.providerCalls() + (reconcile == raw ? 0 : reconcile.providerCalls());
                    throw new StageStop("L1", "P6_L1_PREDECESSOR_FAILED:" + reconcile.reasonCode(),
                            stopDetails(reconcile), calls);
                }
                committedL1 = l1Execution.committedL1(fixture.projectId(), fixture.selector(), fixture.chapterKey())
                        .orElseThrow(() -> new IllegalStateException("P6_REPORT_L1_READBACK_FAILED"));
                l1Calls = raw.providerCalls() + reconcile.providerCalls();
            }
            Files.write(reportPath, committedL1.reportL1Bytes());
            if ("L1_ONLY".equals(mode)) {
                finalBytes = input.get("DRAFT.txt");
                reason = "L1_REPORT_COMMITTED";
                valid = true;
                measuredCalls = l1Calls;
                stage = "L1";
            } else {
                EditorialL2Execution.Request l2Request = new EditorialL2Execution.Request(
                        committedL1.context(), committedL1.reportL1AttemptIdentity(), committedL1.reportL1Bytes(), Set.of());
                EditorialL2Execution.Result l2Result = new EditorialL2Execution().execute(l2Request,
                        budgets.discovery(), budgets.edit(), budgets.finalRead(), budgetedL2,
                        new EditorialPhaseArtifactStore(database, fixture.chapterKey(), EditorialPhaseArtifactStore.L2_PHASE));
                if (!l2Result.accepted() || l2Result.committed() == null) {
                    throw new StageStop("L2", "P6_L2_PREDECESSOR_FAILED:" + l2Result.reasonCode(),
                            l2Result.issues(), l1Calls + l2Result.providerCalls());
                }
                if ("L2_ONLY".equals(mode) || "L1_THEN_L2".equals(mode)) {
                    finalBytes = l2Result.committed().viL2Bytes();
                    reason = "L2_COMMITTED";
                    valid = true;
                    measuredCalls = l1Calls + l2Result.providerCalls();
                    stage = "L2";
                } else {
                    EditorialL3Execution.Request l3Request = new EditorialL3Execution.Request(
                            committedL1.context(), committedL1.reportL1AttemptIdentity(), committedL1.reportL1Bytes(),
                            l2Result.committed(), Set.of());
                    EditorialL3Execution.Result l3Result = new EditorialL3Execution().execute(l3Request,
                            budgets.reaudit(), budgets.reconcile(), budgets.finalRead(), budgetedL3,
                            new EditorialPhaseArtifactStore(database, fixture.chapterKey(), EditorialPhaseArtifactStore.L3_PHASE));
                    if (!l3Result.accepted() || l3Result.committed() == null) {
                        throw new StageStop("L3", "P6_L3_PREDECESSOR_FAILED:" + l3Result.reasonCode(),
                                l3Result.issues(), l1Calls + l2Result.providerCalls() + l3Result.providerCalls());
                    }
                    finalBytes = l3Result.committed().viL2Bytes();
                    reason = "L3_FINAL_COMMITTED";
                    valid = true;
                    measuredCalls = l1Calls + l2Result.providerCalls() + l3Result.providerCalls();
                    stage = "L3";
                }
            }
          } catch (StageStop stopped) {
            // A typed stop is a structural result, not a crash: keep the reason and the
            // engine's safe detail so the run can be diagnosed without retaining model output.
            finalBytes = input.get("DRAFT.txt");
            reason = stopped.reason;
            valid = false;
            stage = stopped.stage;
            measuredCalls = stopped.calls;
            stops.add(stopped.reason);
            stops.addAll(stopped.details);
            reportRequired = !"L1".equals(stopped.stage);
          }
            fakeCalls = l1.calls + l2.calls + l3.calls;
        }

        Files.write(outputRoot.resolve("final.txt"), finalBytes);
        Map<String, Object> structural = new LinkedHashMap<>();
        structural.put("valid", valid);
        structural.put("providerKind", liveMode ? "LIVE" : "FAKE_OFFLINE");
        structural.put("reasonCode", reason);
        structural.put("stage", stage);
        structural.put("providerCalls", BigDecimal.valueOf(measuredCalls));
        structural.put("actualProviderCalls", BigDecimal.valueOf(metrics.calls));
        structural.put("inputTokens", BigDecimal.valueOf(metrics.inputTokens));
        structural.put("outputTokens", BigDecimal.valueOf(metrics.outputTokens));
        structural.put("usd", metrics.usd.toPlainString());
        structural.put("stops", valid ? List.of() : (stops.isEmpty() ? List.of(reason) : List.copyOf(stops)));
        Files.write(outputRoot.resolve("structural.json"), EditorialCanonicalJson.canonicalize(structural).getBytes(StandardCharsets.UTF_8));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fixtureId", fixtureId);
        metadata.put("mode", mode);
        metadata.put("providerKind", liveMode ? "LIVE" : "FAKE_OFFLINE");
        metadata.put("l1ReusedFromPriorGroup", reuseL1State);
        metadata.put("l1SourceRunId", reuseL1State ? l1SourceRunId : "");
        metadata.put("l1ThenL2SameDatabase", "L1_THEN_L2".equals(mode));
        metadata.put("groupId", groupId);
        metadata.put("groupMaximumUsd", groupMaximum.toPlainString());
        metadata.put("actualProviderCalls", BigDecimal.valueOf(metrics.calls));
        metadata.put("inputTokens", BigDecimal.valueOf(metrics.inputTokens));
        metadata.put("outputTokens", BigDecimal.valueOf(metrics.outputTokens));
        metadata.put("usd", metrics.usd.toPlainString());
        metadata.put("knownCostCalls", BigDecimal.valueOf(metrics.knownCostCalls));
        metadata.put("unknownCostCalls", BigDecimal.valueOf(metrics.calls - metrics.knownCostCalls));
        metadata.put("reportedCostCalls", BigDecimal.valueOf(metrics.reportedCostCalls));
        metadata.put("finishReasons", List.copyOf(metrics.finishReasons));
        metadata.put("fakeProviderCalls", BigDecimal.valueOf(liveMode ? 0 : fakeCalls));
        metadata.put("testOnlyInvalidL1", fakeInvalidL1);
        metadata.put("finalSha256", EditorialCanonicalJson.sha256Hex(finalBytes));
        Files.write(outputRoot.resolve("run-metadata.json"), EditorialCanonicalJson.canonicalize(metadata).getBytes(StandardCharsets.UTF_8));
        Files.copy(groupLedgerPath, outputRoot.resolve("spend-ledger.jsonl"));
        if (reportRequired && !Files.exists(reportPath)) throw new IllegalStateException("P6_REPORT_L1_EVIDENCE_MISSING");
        spend.inspect();
        assertTrue("offline fake provider must be called only through the production stage boundary", fakeCalls <= FAKE_PROVIDER_CALL_LIMIT);
        if (fakeInvalidL1) {
            assertTrue("test-only invalid L1 must produce a typed structural stop", !valid);
            assertTrue("test-only invalid L1 must stop at L1", "L1".equals(stage));
            assertTrue("test-only invalid L1 must preserve the engine detail code",
                    stops.contains("L1_COVERAGE_GAP"));
            assertTrue("test-only invalid L1 must not call a provider", metrics.calls == 0);
        } else {
            assertTrue("production artifacts must validate: " + reason
                    + (l2.failure.isEmpty() ? "" : ":fakeL2=" + l2.failure), valid);
        }
        if ("L1_ONLY".equals(mode) && "YES".equalsIgnoreCase(args.getString("p6_keep_l1_state", ""))) {
            preserveL1Database = true;
            preserveL1Storage = true;
        }
    }

    @Test public void liveModeRejectsMismatchedFingerprintBeforeAnyDispatch() {
        Bundle args = InstrumentationRegistry.getArguments();
        Assume.assumeTrue("zero-call live preflight is opt-in",
                "YES".equalsIgnoreCase(args.getString("p6_fixture_live", ""))
                        && "YES".equalsIgnoreCase(args.getString("p6_live_preflight_only", "")));
        String expected = args.getString("p6_expected_endpoint_account_fingerprint", "");
        Assume.assumeTrue("expected value must be a non-zero SHA-256",
                expected != null && expected.matches("[0-9a-fA-F]{64}") && !expected.matches("0{64}"));
        AppSettings settings = SettingsStore.load(ApplicationProvider.getApplicationContext()).copy();
        assertEquals(EditorialP6FixtureLivePreflight.FINGERPRINT_MISMATCH,
                EditorialP6FixtureLivePreflight.check(settings, expected));
        Bundle status = new Bundle();
        status.putString("p6.live.preflight.result", "FINGERPRINT_MISMATCH");
        status.putInt("p6.live.preflight.providerCalls", 0);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, status);
    }

    @After public void tearDown() throws Exception {
        if (database != null) database.close();
        if (context != null && databaseName != null && !preserveL1Database) context.deleteDatabase(databaseName);
        if (storageRoot != null && !preserveL1Storage) deleteTree(storageRoot);
    }

    private FixtureSetup createSetup(String fixtureId, String chapter, Map<String, byte[]> input, String runId,
                                    String chapterKey) throws Exception {
        databaseName = databaseName(runId, fixtureId);
        database = new TranslationRepository(context, databaseName);
        storageRoot = context.getFilesDir().toPath().resolve("p6-fixture-pack-state")
                .resolve(runId.substring(0, 8) + "-" + fixtureId);
        if (Files.exists(storageRoot)) throw new IllegalStateException("P6_FIXTURE_STORAGE_ALREADY_EXISTS");
        storage = new EditorialPackStorageLayout(storageRoot);
        byte[] zip = readAssetZip(CANONICAL_ASSET);
        EditorialPackImportResult imported = new EditorialPackImportService(database, storage,
                new com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver(
                        BundledEditorialEngineContractProfileRegistry.load()))
                .importZip(new ByteArrayInputStream(zip));
        if (imported.state() != EditorialPackImportState.STORED_READY_FOR_CERTIFICATION) {
            throw new IllegalStateException("P6_CANONICAL_PACK_IMPORT_FAILED:" + imported.state()
                    + ":" + imported.error() + ":" + imported.blockedReason());
        }
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                .resolve(imported.packId(), imported.version()).orElseThrow(
                        () -> new IllegalStateException("P6_CANONICAL_PACK_NOT_SELECTABLE"));
        List<EditorialP4InputSource> sources = new ArrayList<>();
        long ordinal = 0L;
        for (String name : SOURCE_NAMES) {
            String role = name.substring(0, name.lastIndexOf('.'));
            sources.add(new EditorialP4InputSource(role, "content://p6-fixture/" + fixtureId + "/" + role.toLowerCase(),
                    appTextBytes(input.get(name)), "UTF-8", "VALID", ordinal++));
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

    private FixtureSetup openExistingL1Setup(String fixtureId, String chapter, Map<String, byte[]> input,
                                             String sourceRunId, String chapterKey) throws Exception {
        databaseName = databaseName(sourceRunId, fixtureId);
        storageRoot = context.getFilesDir().toPath().resolve("p6-fixture-pack-state")
                .resolve(sourceRunId.substring(0, 8) + "-" + fixtureId);
        if (!Files.isDirectory(storageRoot) || !context.getDatabasePath(databaseName).isFile()) {
            throw new IllegalStateException("P6_REUSED_L1_STATE_MISSING");
        }
        preserveL1Database = true;
        preserveL1Storage = true;
        database = new TranslationRepository(context, databaseName);
        storage = new EditorialPackStorageLayout(storageRoot);
        String selector = "p6-fixture-" + sourceRunId.substring(0, 8) + "-" + fixtureId;
        EditorialP4Binding binding = new EditorialP4BindingDao(database).findByAttemptRequestSelector(selector)
                .orElseThrow(() -> new IllegalStateException("P6_REUSED_L1_BINDING_MISSING"));
        long projectId;
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT project_row_id FROM editorial_p4_bindings WHERE attempt_request_selector=?",
                new String[]{selector})) {
            if (!cursor.moveToFirst()) throw new IllegalStateException("P6_REUSED_L1_PROJECT_MISSING");
            projectId = cursor.getLong(0);
        }
        EditorialPackManifest manifest = new EditorialPackSelectionPolicy(database, storage)
                .resolve(binding.packId(), binding.packVersion())
                .orElseThrow(() -> new IllegalStateException("P6_REUSED_L1_PACK_MISSING")).manifest();
        FixtureSetup fixture = new FixtureSetup(projectId, chapterKey, selector, binding, List.of(), manifest);
        List<EditorialP4InputSource> storedSources = currentAssetSources(fixture);
        assertFixtureInputsMatch(input, storedSources);
        if (!"RESTORED".equals(new EditorialP4BindingTransactionService(database, storage)
                .resumeProject(projectId, selector, storedSources).code().name())) {
            throw new IllegalStateException("P6_REUSED_L1_SOURCE_PREFLIGHT_FAILED");
        }
        if (!new EditorialRepository(database).listChapters(projectId).stream()
                .anyMatch(existing -> chapterKey.equals(existing.chapterKey))) {
            throw new IllegalStateException("P6_REUSED_L1_CHAPTER_MISSING");
        }
        return fixture;
    }

    private static void assertFixtureInputsMatch(Map<String, byte[]> input, List<EditorialP4InputSource> stored)
            throws CharacterCodingException {
        Map<String, EditorialP4InputSource> byRole = new LinkedHashMap<>();
        for (EditorialP4InputSource source : stored) byRole.put(source.role(), source);
        for (String name : SOURCE_NAMES) {
            String role = name.substring(0, name.lastIndexOf('.'));
            EditorialP4InputSource source = byRole.get(role);
            if (source == null || !Arrays.equals(appTextBytes(input.get(name)), source.bytes())) {
                throw new IllegalStateException("P6_REUSED_L1_SOURCE_MISMATCH");
            }
        }
    }

    private static String databaseName(String runId, String fixtureId) {
        return "p6-fixture-" + runId.substring(0, 8) + "-" + fixtureId + ".db";
    }

    /** Match FileUtil's UTF-8 text import: decode strictly and consume a leading UTF-8 BOM. */
    private static byte[] appTextBytes(byte[] fixtureBytes) throws CharacterCodingException {
        int offset = fixtureBytes.length >= 3 && (fixtureBytes[0] & 0xff) == 0xef
                && (fixtureBytes[1] & 0xff) == 0xbb && (fixtureBytes[2] & 0xff) == 0xbf ? 3 : 0;
        String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(fixtureBytes, offset, fixtureBytes.length - offset))
                .toString();
        return text.getBytes(StandardCharsets.UTF_8);
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
        try (InputStream source = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(asset)) {
            return source.readAllBytes();
        }
    }

    private List<EditorialP4InputSource> currentAssetSources(FixtureSetup fixture) {
        EditorialRepository repository = new EditorialRepository(database);
        EditorialRepository.Chapter chapter = repository.listChapters(fixture.projectId()).stream()
                .filter(value -> fixture.chapterKey().equals(value.chapterKey)).findFirst().orElseThrow(
                        () -> new IllegalStateException("P6_FIXTURE_CHAPTER_READBACK_FAILED"));
        Map<String, EditorialRepository.AssetSnapshot> assets = new LinkedHashMap<>();
        for (EditorialRepository.AssetSnapshot asset : repository.chapterAssets(chapter.id)) {
            assets.put(asset.role.name(), asset);
        }
        List<EditorialP4InputSource> sources = new ArrayList<>();
        for (var identity : fixture.binding().inputs()) {
            EditorialRepository.AssetSnapshot asset = assets.get(identity.role());
            if (asset == null) throw new IllegalStateException("P6_FIXTURE_SOURCE_ASSET_MISSING:" + identity.role());
            sources.add(new EditorialP4InputSource(identity.role(), asset.sourceUri,
                    asset.content.getBytes(StandardCharsets.UTF_8), identity.encoding(),
                    identity.schemaStatus(), identity.ordinal()));
        }
        return List.copyOf(sources);
    }

    private static List<String> sourceIdentityDrifts(FixtureSetup fixture,
                                                      List<EditorialP4InputSource> current) {
        List<String> drift = new ArrayList<>();
        for (int i = 0; i < fixture.binding().inputs().size(); i++) {
            var identity = fixture.binding().inputs().get(i);
            EditorialP4InputSource actual = current.get(i);
            if (!identity.sourceReference().equals(actual.sourceReference())) drift.add(identity.role() + ":reference");
            if (identity.byteLength() != actual.bytes().length) drift.add(identity.role() + ":length");
            if (!identity.sha256().equals(EditorialCanonicalJson.sha256Hex(actual.bytes()))) drift.add(identity.role() + ":sha256");
        }
        return List.copyOf(drift);
    }

    private static String sourceIdentityLengths(FixtureSetup fixture,
                                                List<EditorialP4InputSource> current) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < fixture.binding().inputs().size(); i++) {
            var identity = fixture.binding().inputs().get(i);
            EditorialP4InputSource actual = current.get(i);
            if (identity.byteLength() != actual.bytes().length) {
                values.add(identity.role() + ":" + identity.byteLength() + "/" + actual.bytes().length);
            }
        }
        return values.isEmpty() ? "none" : String.join(",", values);
    }

    private static String sourceIdentityHashes(FixtureSetup fixture,
                                               List<EditorialP4InputSource> current) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < fixture.binding().inputs().size(); i++) {
            var identity = fixture.binding().inputs().get(i);
            EditorialP4InputSource actual = current.get(i);
            String actualHash = EditorialCanonicalJson.sha256Hex(actual.bytes());
            if (!identity.sha256().equals(actualHash)) {
                values.add(identity.role() + ":" + identity.sha256().substring(0, 12)
                        + "/" + actualHash.substring(0, 12));
            }
        }
        return values.isEmpty() ? "none" : String.join(",", values);
    }

    private static Map<String, Object> rawWire(String attempt, EditorialRawInventory.Inventory inventory) {
        return map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE, "attemptIdentity", attempt,
                "coverage", coverage(inventory), "candidates", List.of());
    }

    private static Map<String, Object> invalidRawWire(String attempt, EditorialRawInventory.Inventory inventory) {
        EditorialRawInventory.Unit first = inventory.units().get(0);
        return map("wireSchemaVersion", EditorialL1Ledger.RAW_WIRE, "attemptIdentity", attempt,
                "coverage", List.of(map("from", first.id(), "to", first.id(), "status", "PROCESSED")),
                "candidates", List.of());
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
            return json(map("wireSchemaVersion", EditorialL2Execution.DISCOVERY_WIRE_V3,
                    "attemptIdentity", request.attemptIdentity(), "coverage", coverage(inventory),
                    "candidates", List.of(map("candidateId", "c1", "ledger", "UNIT", "unitId", unit,
                            "note", "neutral offline structural candidate"))));
        }
        if (EditorialFinalRead.L2_PHASE.equals(request.phase())) return finalReadWire(request);
        return json(map("wireSchemaVersion", EditorialL2Execution.WIRE_SCHEMA_VERSION_V3,
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
            return json(map("wireSchemaVersion", EditorialL3Execution.REAUDIT_WIRE_V3,
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
        return json(map("wireSchemaVersion", EditorialL3Execution.RECONCILE_WIRE_V3,
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
        return EditorialCanonicalJson.canonicalize(value.get("wireSchemaVersion") instanceof String && ((String) value.get("wireSchemaVersion")).endsWith(".v3")
                ? com.ml.tblandroidtxt.editorial.pack.EditorialUnitReference.wireView(value) : value).getBytes(StandardCharsets.UTF_8);
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

    /** A stage that ended in a typed stop; carries only app-generated safe codes, never model text. */
    private static final class StageStop extends RuntimeException {
        final String stage;
        final String reason;
        final List<String> details;
        final int calls;

        StageStop(String stage, String reason, List<String> details, int calls) {
            super(reason);
            this.stage = stage;
            this.reason = reason;
            this.details = details == null ? List.of() : List.copyOf(details);
            this.calls = calls;
        }
    }

    private static List<String> stopDetails(EditorialP5CExactBindingExecution.Result result) {
        List<String> details = new ArrayList<>();
        for (EditorialP5PilotResult phase : new EditorialP5PilotResult[]{result.rawResult(), result.reconcileResult()}) {
            if (phase != null && phase.stopReceipt() != null) {
                details.add("phase=" + phase.stopReceipt().phase());
                details.addAll(phase.stopReceipt().evidenceRefs());
            }
        }
        return details;
    }

    private static final class PromptCapture {
        private final Path root;
        private final Path legacyL2EditPath;
        private int sequence;

        PromptCapture(Path root, Path legacyL2EditPath) {
            this.root = root;
            this.legacyL2EditPath = legacyL2EditPath;
        }

        synchronized void record(String phase, PromptPair prompt) throws IOException {
            if (phase == null || !phase.matches("[A-Z0-9_]{1,48}") || prompt == null) {
                throw new IllegalArgumentException("P6_PROMPT_CAPTURE_INVALID");
            }
            Files.createDirectories(root);
            byte[] bytes = ("SYSTEM\n" + prompt.system + "\nUSER\n" + prompt.user)
                    .getBytes(StandardCharsets.UTF_8);
            Path path = root.resolve(String.format(Locale.ROOT, "%03d-%s.txt", ++sequence, phase));
            Files.write(path, bytes, java.nio.file.StandardOpenOption.CREATE_NEW,
                    java.nio.file.StandardOpenOption.WRITE);
            if ("L2_EDIT".equals(phase)) {
                Files.write(legacyL2EditPath, bytes, java.nio.file.StandardOpenOption.CREATE_NEW,
                        java.nio.file.StandardOpenOption.WRITE);
            }
        }
    }

    /** Fixture-only archive of model content bytes, written before the engine validates them. */
    private static final class ResponseCapture {
        private final Path root;
        private int sequence;

        ResponseCapture(Path root) { this.root = root; }

        synchronized void record(String phase, byte[] content) throws IOException {
            if (phase == null || !phase.matches("[A-Z0-9_]{1,48}") || content == null
                    || content.length > 65_536) {
                throw new IllegalArgumentException("P6_RESPONSE_CAPTURE_INVALID");
            }
            Files.createDirectories(root);
            Path path = root.resolve(String.format(Locale.ROOT, "%03d-%s.json", ++sequence, phase));
            Files.write(path, content, java.nio.file.StandardOpenOption.CREATE_NEW,
                    java.nio.file.StandardOpenOption.WRITE);
        }
    }

    private static final class ProviderMetrics {
        int calls;
        int knownCostCalls;
        int reportedCostCalls;
        long inputTokens;
        long outputTokens;
        BigDecimal usd = BigDecimal.ZERO;
        final List<String> finishReasons = new ArrayList<>();

        synchronized void started() { calls++; }

        synchronized void response(long input, long output, BigDecimal cost, boolean known,
                                   boolean reported, String finishReason) {
            inputTokens += input;
            outputTokens += output;
            if (known) {
                knownCostCalls++;
                usd = usd.add(cost);
            }
            if (reported) reportedCostCalls++;
            String safe = finishReason != null && finishReason.matches("[A-Za-z0-9_-]{1,32}")
                    ? finishReason : "OTHER";
            finishReasons.add(safe);
        }
    }

    private static final class PromptRecordingL1Provider implements EditorialP5PilotProvider {
        private final EditorialP5PilotProvider delegate;
        private final PromptCapture prompts;
        private final ResponseCapture responses;
        private final ProviderMetrics metrics;

        PromptRecordingL1Provider(EditorialP5PilotProvider delegate, PromptCapture prompts,
                                  ResponseCapture responses, ProviderMetrics metrics) {
            this.delegate = delegate;
            this.prompts = prompts;
            this.responses = responses;
            this.metrics = metrics;
        }

        @Override public void beginAttempt(long maximumExecutionTimeMillis) {
            delegate.beginAttempt(maximumExecutionTimeMillis);
        }

        @Override public Response call(Request request) throws Exception {
            prompts.record(request.phase(), OpenRouterEditorialP5PilotProvider.buildLedgerPrompt(request));
            if (metrics != null) metrics.started();
            Response response = delegate.call(request);
            responses.record(request.phase(), response.responseBytes());
            if (metrics != null) metrics.response(response.inputTokens(), response.outputTokens(),
                    response.reportedCost(), response.costKnown(), response.costReported(), response.finishReason());
            return response;
        }
    }

    private static final class PromptRecordingPhaseProvider implements EditorialL2Execution.Provider {
        private final EditorialL2Execution.Provider delegate;
        private final PromptCapture prompts;
        private final ResponseCapture responses;
        private final ProviderMetrics metrics;
        private final boolean l2;

        PromptRecordingPhaseProvider(EditorialL2Execution.Provider delegate, PromptCapture prompts,
                                     ResponseCapture responses, ProviderMetrics metrics, boolean l2) {
            this.delegate = delegate;
            this.prompts = prompts;
            this.responses = responses;
            this.metrics = metrics;
            this.l2 = l2;
        }

        @Override public Response call(Request request) throws Exception {
            PromptPair prompt = l2 ? OpenRouterEditorialL2Provider.buildPrompt(request)
                    : OpenRouterEditorialL3Provider.buildPrompt(request);
            prompts.record(request.phase(), prompt);
            if (metrics != null) metrics.started();
            Response response = delegate.call(request);
            responses.record(request.phase(), response.responseBytes());
            if (metrics != null) metrics.response(response.inputTokens(), response.outputTokens(),
                    response.cost(), response.costKnown(), false, response.finishReason());
            return response;
        }
    }

    private record FixtureSetup(long projectId, String chapterKey, String selector, EditorialP4Binding binding,
                                List<EditorialP4InputSource> sources, com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest manifest) { }

    private static final class FakeL1 implements EditorialP5PilotProvider {
        private final boolean invalidRawCoverage;
        int calls;

        FakeL1(boolean invalidRawCoverage) {
            this.invalidRawCoverage = invalidRawCoverage;
        }

        @Override public Response call(Request request) {
            calls++;
            byte[] raw = request.visibleSources().get(EditorialSafe4Contract.RAW);
            EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(raw);
            Map<String, Object> wire = "L1_RAW_DISCOVERY".equals(request.phase())
                    ? (invalidRawCoverage ? invalidRawWire(request.attemptIdentity(), inventory)
                    : rawWire(request.attemptIdentity(), inventory))
                    : reconcileWire(request.attemptIdentity(), inventory);
            return new Response("offline-fake-" + request.phase(), json(wire), "stop", true,
                    0, 0, 0, BigDecimal.ZERO, null, true);
        }
    }

    private static final class FakeL2 implements EditorialL2Execution.Provider {
        int calls;
        String failure = "";
        @Override public Response call(Request request) throws Exception {
            calls++;
            try {
                return new Response(l2Wire(request), "stop", true, 0, 0, BigDecimal.ZERO, true);
            } catch (Exception error) {
                StackTraceElement[] trace = error.getStackTrace();
                if (trace.length > 0) {
                    StackTraceElement at = trace[0];
                    failure = error.getClass().getSimpleName() + "@" + at.getClassName()
                            + "#" + at.getMethodName() + ":" + at.getLineNumber();
                } else {
                    failure = error.getClass().getSimpleName();
                }
                throw error;
            }
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
