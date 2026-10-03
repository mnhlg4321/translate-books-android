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
    private static final EditorialChainBudgets CHAIN = EditorialChainBudgets.d3Recommended();

    private Context context;
    private String databaseName;
    private TranslationRepository database;
    private Path storageRoot;
    private EditorialPackStorageLayout storage;
    private String rawText = "raw chapter bytes";
    private String draftText = DRAFT_LINE;

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
                .runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY, CHAIN, l2, l3);
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
                CHAPTER_KEY, CHAIN, l2, l3);

        assertEquals(first.reasonCode(), EditorialChapterFinalCoordinator.Stage.FINAL, first.stage());
        assertTrue(first.finalReady());
        assertEquals("L3_FINAL_COMMITTED", first.reasonCode());
        assertEquals(4, first.providerCalls());
        assertEquals(2, l2.calls);
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
                CHAPTER_KEY, CHAIN, noL2, noL3);
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
                .runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY, CHAIN, afterL2, afterL3);
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
        assertEquals(com.ml.tblandroidtxt.editorial.pack.EditorialContractRevision.LEGACY_V1,
                afterL1.l1ContractRevision());
        assertFalse(afterL1.progress().finalReady());

        ScriptedProvider legacyL2 = new ScriptedProvider();
        ScriptedProvider legacyL3 = new ScriptedProvider();
        EditorialChapterFinalCoordinator.Result run = coordinator.runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY,
                CHAIN, legacyL2, legacyL3);
        assertTrue(run.reasonCode(), run.finalReady());
        assertEquals(4, run.providerCalls());
        assertEquals(2, legacyL2.calls);
        assertEquals(2, legacyL3.calls);

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
            if (EditorialL3Execution.REAUDIT_PHASE.equals(request.phase())
                    || EditorialL3Execution.RECONCILE_PHASE.equals(request.phase())) {
                throw new AssertionError("simulated process death");
            }
            return new ScriptedProvider().call(request);
        };
        try {
            coordinator.runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY, CHAIN, dying, dying);
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
                CHAPTER_KEY, CHAIN, l2, l3);
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
                CHAIN, failing, new ScriptedProvider());
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
                CHAIN, new ScriptedProvider(), new ScriptedProvider()).finalArtifact();
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

    /**
     * EMULATOR ONLY, opt-in ({@code p6_seed_emulator_ui=L1|FINAL}): seeds the app's real database and files
     * directory with one P4-bound chapter (L1 committed, or the full fake chain) so the chapter-card UI can be
     * looked at by hand. It refuses, before writing anything, when the database already holds any P4 binding,
     * so it can never touch pilot data. No provider or network is involved.
     */
    @Test public void seedMainDatabaseForEmulatorUiSmoke() throws Exception {
        String mode = InstrumentationRegistry.getArguments().getString("p6_seed_emulator_ui");
        org.junit.Assume.assumeTrue("L1".equals(mode) || "FINAL".equals(mode));
        database.close();
        database = new TranslationRepository(context);
        try (android.database.Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM editorial_p4_bindings", null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals("refusing to seed a database that already has P4 bindings", 0L, cursor.getLong(0));
        }
        storage = new EditorialPackStorageLayout(context.getFilesDir().toPath());
        BindingFixture fixture = createBoundChapter();
        commitL1(fixture);
        if ("FINAL".equals(mode)) {
            EditorialChapterFinalCoordinator.Result result = new EditorialChapterFinalCoordinator(database, storage)
                    .runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY, CHAIN, new ScriptedProvider(), new ScriptedProvider());
            assertTrue(result.reasonCode(), result.finalReady());
        }
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
                case EditorialL2Execution.DISCOVERY_PHASE -> wire = discoveryWire(request.attemptIdentity());
                case EditorialL2Execution.PHASE -> wire = l2Wire(request.attemptIdentity());
                case EditorialL3Execution.REAUDIT_PHASE -> wire = reauditWire(request.attemptIdentity());
                case EditorialL3Execution.RECONCILE_PHASE -> wire = reconcileWire(request.attemptIdentity());
                default -> throw new AssertionError("unexpected phase " + request.phase());
            }
            return new Response(wire.getBytes(StandardCharsets.UTF_8), "stop", true, 10, 10,
                    new BigDecimal("0.01"), true);
        }
    }

    private static String discoveryWire(String attempt) {
        return "{\"wireSchemaVersion\":\"" + EditorialL2Execution.DISCOVERY_WIRE + "\","
                + "\"attemptIdentity\":\"" + attempt + "\","
                + "\"candidates\":[{\"candidateId\":\"d1\",\"ledger\":\"UNIT\",\"line\":1}]}";
    }

    private static String l2Wire(String attempt) {
        return "{\"wireSchemaVersion\":\"" + EditorialL2Execution.WIRE_SCHEMA_VERSION + "\","
                + "\"attemptIdentity\":\"" + attempt + "\","
                + "\"resolutions\":[{\"candidateId\":\"d1\",\"status\":\"PROCESSED\"}],"
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

    // ---- ledger contract through the coordinator entry point ----

    private static final String LEDGER_RAW = String.join("\n", "王は城に入った。", "騎士が言った。", "「踏破した。」", "「今回は無理だ。」",
            "彼女は笑った。", "「踏破だ。」", "雨が降る。", "彼は歩いた。", "「踏破完了。」", "空は暗い。");
    private static final String LEDGER_DRAFT = String.join("\n", "Vua vao thanh.", "Hiep si noi.", "\"Da chinh phuc.\"",
            "\"今回 khong the.\"", "Co ay cuoi.", "\"Chinh phuc roi.\"", "Troi mua.", "Anh di.", "\"Chinh phuc xong.\"", "Troi toi.");

    private static final class LedgerWires {
        static String unit(int line) {
            return com.ml.tblandroidtxt.editorial.pack.EditorialRawInventory.build(
                    LEDGER_RAW.getBytes(StandardCharsets.UTF_8)).units().get(line - 1).id();
        }

        static Map<String, Object> m(Object... kv) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < kv.length; i += 2) map.put((String) kv[i], kv[i + 1]);
            return map;
        }

        static List<Object> l(Object... v) { return new ArrayList<>(List.of(v)); }

        static byte[] json(Map<String, Object> map) {
            return EditorialCanonicalJson.canonicalize(map).getBytes(StandardCharsets.UTF_8);
        }

        static List<Object> coverage() { return l(m("from", unit(1), "to", unit(10), "status", "PROCESSED")); }

        static byte[] rawWire(String attempt) {
            return json(m("wireSchemaVersion", com.ml.tblandroidtxt.editorial.pack.EditorialL1Ledger.RAW_WIRE,
                    "attemptIdentity", attempt, "coverage", coverage(), "candidates", l()));
        }

        static Map<String, Object> finding(String id, String type, int rawLine, Map<String, Object> draft, String rawQuote,
                                           String draftQuote, List<Object> occurrences) {
            return m("errorId", id, "type", type, "severity", "MAJOR", "rawUnits", l(unit(rawLine)), "draft", draft,
                    "rawQuote", rawQuote, "draftQuote", draftQuote, "observation", "obs", "expectedMeaning", "exp",
                    "evidenceRefs", l(), "candidateIds", l(), "occurrenceUnits", occurrences, "disposition", "OPEN",
                    "evidenceLimit", "");
        }

        static byte[] reconcileWire(String attempt) {
            Map<String, Object> lines3 = m("kind", "LINES", "start", BigDecimal.valueOf(3), "end", BigDecimal.valueOf(3), "after", BigDecimal.ZERO);
            Map<String, Object> lines4 = m("kind", "LINES", "start", BigDecimal.valueOf(4), "end", BigDecimal.valueOf(4), "after", BigDecimal.ZERO);
            Map<String, Object> missing = m("kind", "MISSING", "start", BigDecimal.ZERO, "end", BigDecimal.ZERO, "after", BigDecimal.valueOf(6));
            return json(m("wireSchemaVersion", com.ml.tblandroidtxt.editorial.pack.EditorialL1Ledger.RECONCILE_WIRE,
                    "attemptIdentity", attempt, "coverage", coverage(), "resolutions", l(),
                    "findings", l(finding("e1", "MEANING", 3, lines3, "踏破", "chinh phuc", l(unit(6), unit(9))),
                            finding("e2", "UNTRANSLATED", 4, lines4, "今回", "今回", l()),
                            finding("e3", "OMISSION", 7, missing, "雨が降る", "", l())),
                    "speakerRecords", l(),
                    "protectedSpans", l(m("spanId", "p1", "start", BigDecimal.valueOf(7), "end", BigDecimal.valueOf(7),
                            "source", "PRONOUN_ROW", "reason", "profile row")),
                    "disposition", m("disposition", "CONTINUE", "reasonCode", "L1_OK", "stopClass", "NONE")));
        }

        static Map<String, Object> change(String id, String errorId, String op, int line, String before, String after) {
            Map<String, Object> map = m("changeId", id, "errorId", errorId, "line", BigDecimal.valueOf(line), "before", before,
                    "after", after, "reason", "fix", "dialogue", Boolean.FALSE, "status", "CLOSED");
            if (op != null) map.put("op", op);
            return map;
        }

        static Map<String, Object> resolution(String errorId, List<Object> changeIds, List<Object> occurrences) {
            return m("errorId", errorId, "status", "FIXED", "changeIds", changeIds, "preserveIds", l(),
                    "occurrences", occurrences, "evidenceQuote", "", "reason", "");
        }

        static byte[] editWire(String attempt) {
            return json(m("wireSchemaVersion", EditorialL2Execution.WIRE_SCHEMA_VERSION_V2, "attemptIdentity", attempt,
                    "resolutions", l(m("candidateId", "U001", "status", "PROCESSED")),
                    "findingResolutions", l(
                            resolution("e1", l("C1", "C2", "C3"), l(m("unitId", unit(6), "ref", "C2"), m("unitId", unit(9), "ref", "C3"))),
                            resolution("e2", l("C4"), l()), resolution("e3", l("C5"), l())),
                    "changes", l(change("C1", "e1", null, 3, "\"Da chinh phuc.\"", "\"Da vuot qua.\""),
                            change("C2", "e1", null, 6, "\"Chinh phuc roi.\"", "\"Vuot qua roi.\""),
                            change("C3", "e1", null, 9, "\"Chinh phuc xong.\"", "\"Vuot qua xong.\""),
                            change("C4", "e2", null, 4, "\"今回 khong the.\"", "\"Lan nay khong the.\""),
                            change("C5", "e3", "INSERT_AFTER", 6, "\"Chinh phuc roi.\"", "Troi bat dau mua.")),
                    "preserved", l(),
                    "disposition", m("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE")));
        }

        static byte[] readWire(EditorialL2Execution.Provider.Request request) {
            byte[] target = request.visibleSources().get(com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead.TARGET_ROLE);
            List<String> lines = com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead.lines(target);
            List<Object> tails = l();
            for (Integer line : com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead.probeLines(target)) {
                String text = lines.get(line - 1);
                tails.add(m("line", BigDecimal.valueOf(line), "tail", text.length() <= 12 ? text : text.substring(text.length() - 12)));
            }
            return json(m("wireSchemaVersion", com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead.WIRE,
                    "attemptIdentity", request.attemptIdentity(), "readSha256", EditorialCanonicalJson.sha256Hex(target),
                    "probeTails", tails, "verdict", "CLEAN", "defects", l()));
        }

        static Map<String, Object> probe(String id, String kind, int rawLine, int viLine, String rawQuote, String viQuote) {
            return m("probeId", id, "kind", kind, "rawUnits", l(unit(rawLine)), "viStart", BigDecimal.valueOf(viLine),
                    "viEnd", BigDecimal.valueOf(viLine), "scope", "checked this unit", "contrast", "compared with the glossary",
                    "rawQuote", rawQuote, "viQuote", viQuote, "verdict", "NO_DEFECT", "action", "NONE");
        }

        static byte[] l3Wire(EditorialL2Execution.Provider.Request request) {
            if (EditorialL3Execution.REAUDIT_PHASE.equals(request.phase())) {
                return json(m("wireSchemaVersion", EditorialL3Execution.REAUDIT_WIRE_V2, "attemptIdentity", request.attemptIdentity(),
                        "coverage", coverage(),
                        "candidates", l(m("candidateId", "u1", "ledger", "TG", "unitId", unit(3), "viLine", BigDecimal.valueOf(3),
                                "status", "PROCESSED", "note", "contrast"))));
            }
            if (com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead.L3_PHASE.equals(request.phase())) return readWire(request);
            return json(m("wireSchemaVersion", EditorialL3Execution.RECONCILE_WIRE_V2, "attemptIdentity", request.attemptIdentity(),
                    "resolutions", l(m("candidateId", "u1", "status", "PROCESSED")), "carriedResolutions", l(),
                    "changes", l(change("Q1", "L3-1", null, 8, "Troi mua.", "Troi mua to.")),
                    "preserved", l(),
                    "probes", l(probe("P1", "COVERAGE", 1, 1, "王は城", "Vua vao"), probe("P2", "COVERAGE", 2, 2, "騎士が", "Hiep si"),
                            probe("P3", "COVERAGE", 3, 3, "踏破", "vuot qua"), probe("P4", "REGRESSION", 4, 4, "今回", "Lan nay"),
                            probe("P5", "REGRESSION", 6, 6, "踏破", "Vuot qua roi"), probe("P6", "REGRESSION", 7, 7, "雨が降る", "bat dau mua")),
                    "disposition", m("disposition", "CONTINUE", "reasonCode", "L3_OK", "stopClass", "NONE")));
        }
    }

    private void commitLedgerL1(BindingFixture fixture) throws Exception {
        EditorialP5PilotProvider l1Provider = request -> {
            byte[] wire = "L1_RAW_DISCOVERY".equals(request.phase()) ? LedgerWires.rawWire(request.attemptIdentity())
                    : LedgerWires.reconcileWire(request.attemptIdentity());
            return new EditorialP5PilotProvider.Response("fake-ledger-" + request.phase(), wire, "stop", true, 40, 20, 60,
                    BigDecimal.ZERO, null, true);
        };
        EditorialP5CExactBindingExecution.Result result = EditorialP5CExactBindingExecution.forContract(database, storage,
                com.ml.tblandroidtxt.editorial.pack.EditorialContractRevision.L1_LEDGER_V2).execute(
                fixture.projectId, SELECTOR, CHAPTER_KEY,
                authorization(fixture.binding, "auth-raw-v2", "L1_RAW_DISCOVERY"),
                authorization(fixture.binding, "auth-reconcile-v2", "L1_RECONCILE"), l1Provider);
        assertEquals(result.reasonCode(), EditorialP5CExactBindingExecution.Status.COMMITTED, result.status());
    }

    private static final class LedgerL2 implements EditorialL2Execution.Provider {
        int calls;
        final List<String> phases = new ArrayList<>();

        @Override public Response call(Request request) {
            calls++;
            phases.add(request.phase());
            byte[] body;
            switch (request.phase()) {
                case EditorialL2Execution.DISCOVERY_PHASE -> body = LedgerWires.json(LedgerWires.m(
                        "wireSchemaVersion", EditorialL2Execution.DISCOVERY_WIRE_V2, "attemptIdentity", request.attemptIdentity(),
                        "coverage", LedgerWires.coverage(),
                        "candidates", LedgerWires.l(LedgerWires.m("candidateId", "U001", "ledger", "UNIT", "unitId", LedgerWires.unit(1), "note", "n"))));
                case EditorialL2Execution.PHASE -> body = LedgerWires.editWire(request.attemptIdentity());
                default -> body = LedgerWires.readWire(request);
            }
            return new Response(body, "stop", true, 100, 50, new BigDecimal("0.01"), true);
        }
    }

    private static final class LedgerL3 implements EditorialL2Execution.Provider {
        int calls;

        @Override public Response call(Request request) {
            calls++;
            return new Response(LedgerWires.l3Wire(request), "stop", true, 100, 50, new BigDecimal("0.01"), true);
        }
    }

    @Test public void ledgerChainRunsFromTheCoordinatorWithProtectedSpansStructureAndRestart() throws Exception {
        rawText = LEDGER_RAW;
        draftText = LEDGER_DRAFT;
        BindingFixture fixture = createBoundChapter();
        commitLedgerL1(fixture);

        LedgerL2 l2 = new LedgerL2();
        LedgerL3 l3 = new LedgerL3();
        EditorialChapterFinalCoordinator coordinator = new EditorialChapterFinalCoordinator(database, storage);
        assertEquals(com.ml.tblandroidtxt.editorial.pack.EditorialContractRevision.L1_LEDGER_V2,
                coordinator.inspect(fixture.projectId, SELECTOR, CHAPTER_KEY).l1ContractRevision());
        EditorialChapterFinalCoordinator.Result first = coordinator.runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY,
                EditorialChainBudgets.ledgerRecommended(), l2, l3);
        assertEquals(first.reasonCode(), EditorialChapterFinalCoordinator.Stage.FINAL, first.stage());
        assertTrue(first.finalReady());
        assertEquals(6, first.providerCalls());
        assertEquals(List.of(EditorialL2Execution.DISCOVERY_PHASE, EditorialL2Execution.PHASE,
                com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead.L2_PHASE), l2.phases);
        String fin = new String(first.finalArtifact().viL2Bytes(), StandardCharsets.UTF_8);
        String[] lines = fin.split("\n", -1);
        assertEquals("\"Da vuot qua.\"", lines[2]);
        assertEquals("Troi bat dau mua.", lines[6]);
        // the report protected DRAFT line 7: it sits at line 8 after the insert and the L3 edit of it was reverted
        assertEquals("Troi mua.", lines[7]);
        assertEquals(11, lines.length);

        // restart: a new repository over the same file resumes without a call and exposes the same FINAL
        database.close();
        database = new TranslationRepository(context, databaseName);
        coordinator = new EditorialChapterFinalCoordinator(database, storage);
        LedgerL2 noL2 = new LedgerL2();
        LedgerL3 noL3 = new LedgerL3();
        EditorialChapterFinalCoordinator.Result again = coordinator.runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY,
                EditorialChainBudgets.ledgerRecommended(), noL2, noL3);
        assertEquals(again.reasonCode(), EditorialChapterFinalCoordinator.Stage.FINAL, again.stage());
        assertEquals(0, noL2.calls + noL3.calls);
        EditorialChapterFinalCoordinator.Inspection inspection = coordinator.inspect(fixture.projectId, SELECTOR, CHAPTER_KEY);
        assertTrue(inspection.progress().finalReady());
        assertEquals("VERIFIED", inspection.receiptStatus());
        Path target = context.getCacheDir().toPath().resolve("ledger-export-" + UUID.randomUUID() + ".txt");
        try {
            EditorialChapterFinalCoordinator.ExportResult export = EditorialChapterFinalCoordinator.exportTxt(
                    inspection.finalArtifact(), () -> Files.newOutputStream(target), () -> Files.newInputStream(target));
            assertTrue(export.reasonCode(), export.verified());
        } finally {
            Files.deleteIfExists(target);
        }
        // a FINAL whose receipt does not back its claims is refused by export
        EditorialL2Execution.Committed forged = new EditorialL2Execution.Committed(
                inspection.finalArtifact().attemptIdentity(), inspection.finalArtifact().predecessorIdentity(),
                inspection.finalArtifact().bundleIdentity(), inspection.finalArtifact().viL2Bytes(),
                inspection.finalArtifact().viL2Sha256(),
                new String(inspection.finalArtifact().changeMapBytes(), StandardCharsets.UTF_8)
                        .replace("\"operations\"", "\"operationsX\"").getBytes(StandardCharsets.UTF_8),
                EditorialCanonicalJson.sha256Hex(new String(inspection.finalArtifact().changeMapBytes(), StandardCharsets.UTF_8)
                        .replace("\"operations\"", "\"operationsX\"").getBytes(StandardCharsets.UTF_8)));
        assertEquals("EXPORT_RECEIPT_INVALID", EditorialChapterFinalCoordinator.exportTxt(forged,
                () -> new java.io.ByteArrayOutputStream(), () -> new java.io.ByteArrayInputStream(new byte[0])).reasonCode());
        assertArrayEquals(first.finalArtifact().viL2Bytes(), inspection.finalArtifact().viL2Bytes());
    }

    @Test public void ledgerChainWithoutAFinalReadBudgetStopsBeforeAnyCall() throws Exception {
        rawText = LEDGER_RAW;
        draftText = LEDGER_DRAFT;
        BindingFixture fixture = createBoundChapter();
        commitLedgerL1(fixture);
        LedgerL2 l2 = new LedgerL2();
        LedgerL3 l3 = new LedgerL3();
        EditorialChapterFinalCoordinator.Result result = new EditorialChapterFinalCoordinator(database, storage)
                .runToFinal(fixture.projectId, SELECTOR, CHAPTER_KEY, EditorialChainBudgets.d3Recommended(), l2, l3);
        assertEquals(EditorialChapterFinalCoordinator.Stage.L2, result.stage());
        assertEquals("L2_FINAL_READ_BUDGET_REQUIRED", result.reasonCode());
        assertEquals(0, l2.calls + l3.calls);
    }

    @Test public void fullLedgerChainStartsAtL1AndReopensWithoutRepeatingAnyStage() throws Exception {
        rawText = LEDGER_RAW;
        draftText = LEDGER_DRAFT;
        BindingFixture fixture = createBoundChapter();
        AtomicL1 l1 = new AtomicL1();
        LedgerL2 l2 = new LedgerL2();
        LedgerL3 l3 = new LedgerL3();
        EditorialChapterFinalCoordinator coordinator = new EditorialChapterFinalCoordinator(database, storage);
        EditorialChainBudgets budgets = EditorialChainBudgets.fullLedgerRecommended();
        EditorialChapterFinalCoordinator.Result result = coordinator.runFromL1(fixture.projectId, SELECTOR,
                CHAPTER_KEY, budgets, fullAuthorization(fixture.binding, "raw-first", "L1_RAW_DISCOVERY", budgets.l1Raw()),
                fullAuthorization(fixture.binding, "reconcile-first", "L1_RECONCILE", budgets.l1Reconcile()),
                l1, l2, l3);

        assertEquals(result.reasonCode(), EditorialChapterFinalCoordinator.Stage.FINAL, result.stage());
        assertTrue(result.finalReady());
        assertEquals(8, result.providerCalls());
        assertEquals(2, l1.calls);
        assertEquals(List.of("L1_RAW_DISCOVERY", "L1_RECONCILE"), l1.phases);
        assertEquals(3, l2.calls);
        assertEquals(3, l3.calls);

        database.close();
        database = new TranslationRepository(context, databaseName);
        EditorialChapterFinalCoordinator reopened = new EditorialChapterFinalCoordinator(database, storage);
        AtomicL1 noL1 = new AtomicL1();
        noL1.refuse = true;
        LedgerL2 noL2 = new LedgerL2();
        LedgerL3 noL3 = new LedgerL3();
        EditorialChapterFinalCoordinator.Result again = reopened.runFromL1(fixture.projectId, SELECTOR,
                CHAPTER_KEY, budgets, fullAuthorization(fixture.binding, "raw-resume", "L1_RAW_DISCOVERY", budgets.l1Raw()),
                fullAuthorization(fixture.binding, "reconcile-resume", "L1_RECONCILE", budgets.l1Reconcile()),
                noL1, noL2, noL3);
        assertEquals(EditorialChapterFinalCoordinator.Stage.FINAL, again.stage());
        assertEquals(0, again.providerCalls());
        assertEquals(0, noL1.calls + noL2.calls + noL3.calls);
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
                input("RAW", "raw", rawText),
                input("DRAFT", "draft", draftText),
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

    private static EditorialP5PilotAuthorization fullAuthorization(EditorialP4Binding binding, String id,
                                                                    String phase, EditorialL2Execution.Budget budget) {
        int inputTokens = (budget.maximumInputBytes() + 1) / 2;
        return new EditorialP5PilotAuthorization(id, binding.bindingIdentity(), binding.runDeclarationIdentity(),
                binding.canonicalPackHash(), binding.canonicalProfileHash(), binding.compatibilityEvaluationId(),
                CHAPTER_KEY, phase, "FAKE_PROVIDER", "fake/model", "fake-account-fingerprint", 1, 0, 0,
                inputTokens, budget.maximumOutputTokens(), inputTokens + budget.maximumOutputTokens(),
                budget.maximumCost(), budget.maximumExecutionTimeMillis(), true, false, false,
                "HASH_ONLY", "TEST_STOP_AUTHORITY", 0L, Long.MAX_VALUE, true);
    }

    private static final class AtomicL1 implements EditorialP5PilotProvider {
        int calls;
        boolean refuse;
        final List<String> phases = new ArrayList<>();

        @Override public Response call(Request request) {
            calls++;
            phases.add(request.phase());
            if (refuse) throw new AssertionError("committed L1 stages must not be called again");
            byte[] wire = "L1_RAW_DISCOVERY".equals(request.phase())
                    ? LedgerWires.rawWire(request.attemptIdentity())
                    : LedgerWires.reconcileWire(request.attemptIdentity());
            return new Response("fake-" + request.phase(), wire, "stop", true,
                    40, 20, 60, new BigDecimal("0.01"), null, true);
        }
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
