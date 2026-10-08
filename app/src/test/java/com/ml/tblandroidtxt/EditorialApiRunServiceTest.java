package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.V5SourceIdentity;
import com.ml.tblandroidtxt.editorial.api.V5SourcePackPreflight;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract.Mode;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract.RunState;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Run service over the in-memory store and the scripted provider: every path of the state machine, the cap, cancel, restart. */
public final class EditorialApiRunServiceTest {
    private static final String RAW = "太郎は赤い扉を開けた。\n花子が言った。「五百メートル先です」\n";
    private static final String DRAFT = "Taro mở cánh cửa đỏ.\nHanako nói: 「Cách năm mươi mét.」\n";
    private static final String EDITED = "Taro mở cánh cửa đỏ.\nHanako nói: 「Cách năm trăm mét.」\n";
    private static final BigDecimal CAP = new BigDecimal("0.50");

    private final InMemoryEditorialApiStore store = new InMemoryEditorialApiStore();

    private EditorialApiCombo combo() {
        EditorialApiCombo combo = new EditorialApiCombo();
        combo.name = "raw001+draft001";
        combo.rawUri = "content://raw";
        combo.draftUri = "content://draft";
        store.insertCombo(combo);
        return combo;
    }

    private static EditorialApiSources sources() {
        return new EditorialApiSources(RAW, DRAFT, "", List.of(), "", "Vietnamese");
    }

    private EditorialApiRunService service(EditorialApiProvider provider) {
        return new EditorialApiRunService(store, provider, EditorialApiRunService.CONSERVATIVE, 60_000L);
    }

    @Test public void quickRunStoresASnapshotThenTheFinalText() {
        EditorialApiCombo combo = combo();
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.scripted(FakeEditorialApiProvider.edited(EDITED));
        EditorialApiRunService service = service(provider);
        EditorialApiRun prepared = service.prepare(combo, sources(), "model-a", Mode.QUICK);
        assertEquals(RunState.RUNNING, prepared.state);
        assertEquals(HashUtil.sha256(RAW), store.getRun(prepared.id).rawSha256);
        assertEquals(DRAFT, store.getRun(prepared.id).finalText);
        assertEquals(0, provider.requests.size());

        EditorialApiRun run = service.execute(prepared.id, CAP, "Vietnamese", null);
        assertEquals(RunState.FINAL_OK, run.state);
        assertEquals(EDITED.strip(), run.finalText.strip());
        assertEquals(1, run.calls);
        assertEquals("model-a", provider.models.get(0));
        assertEquals(0, new BigDecimal("0.004").compareTo(run.usd));
        assertEquals(run.finalText, store.getRun(prepared.id).finalText);
        assertEquals(RunState.FINAL_OK, store.latestRun(combo.id).state);
        assertTrue(store.runWrites >= 3);
    }

    @Test public void v5RequiresAndPersistsTheExactValidatedFourSourceAttachments() {
        EditorialApiCombo combo = combo();
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.scripted(FakeEditorialApiProvider.edited(DRAFT));
        EditorialApiRunService service = service(provider);
        EditorialApiSources missing = sources();
        try {
            service.prepare(combo, missing, "model-a", Mode.V5_CHAT);
            throw new AssertionError("V5 accepted missing original attachments");
        } catch (IllegalArgumentException expected) {
            assertEquals("V5_SOURCE_FILE_SET_INVALID", expected.getMessage());
        }
        assertTrue(provider.requests.isEmpty());

        List<EditInputs.OriginalSourceFile> original = List.of(
                new EditInputs.OriginalSourceFile("RAW", "007_RAW_SAMPLE_SERIES_VOL1.txt", "original RAW\r\n"),
                new EditInputs.OriginalSourceFile("DRAFT", "007_SAMPLE_SERIES_VOL1_DRAFT.txt", "original DRAFT\n"),
                new EditInputs.OriginalSourceFile("GLOSSARY", "007_SAMPLE_SERIES_VOL1_chapter_glossary.csv", "source,target,category,note,priority\nterm,thuật ngữ,term,,1\n"),
                new EditInputs.OriginalSourceFile("PRONOUN", "007_PRONOUN_SAMPLE_SERIES_VOL1.csv", "from,speaker,target,self,call,scope,note\n花子,花子,太郎,em,anh,*,legacy\n"));
        V5SourceIdentity identity = new V5SourceIdentity("007", "SAMPLE_SERIES_VOL1");
        // without a chain identity nothing is stored and nothing is sent
        EditorialApiSources noIdentity = new EditorialApiSources("original RAW\r\n", "original DRAFT\n", "filtered", List.of(), "filtered", "Vietnamese", original);
        try {
            service.prepare(combo(), noIdentity, "model-a", Mode.V5_CHAT);
            throw new AssertionError("V5 accepted a missing chain identity");
        } catch (IllegalArgumentException expected) {
            assertEquals("V5_IDENTITY_MISSING", expected.getMessage());
        }
        assertEquals(0, store.runWrites);
        EditorialApiSources valid = new EditorialApiSources("original RAW\r\n", "original DRAFT\n", "filtered", List.of(), "filtered", "Vietnamese", original, identity);
        EditorialApiRun run = service.prepare(combo(), valid, "model-a", Mode.V5_CHAT);
        EditInputs restored = EditorialApiRunService.inputsOf(store.getRun(run.id), "Vietnamese");
        assertEquals(original, restored.originalSourceFiles());
        assertEquals("roles and original names survive the stored snapshot", "007_RAW_SAMPLE_SERIES_VOL1.txt", restored.originalSourceFiles().get(0).name());
        assertEquals("RAW", restored.originalSourceFiles().get(0).role());
        assertEquals(identity, restored.identity());
        assertTrue(run.originalSourceFilesJson.contains("original RAW\\r\\n"));
        assertTrue(V5SourcePackPreflight.check(restored).valid());
        assertTrue(provider.requests.isEmpty());
    }

    @Test public void aV5SnapshotStoredBeforeRolesAndIdentityStillReadsButIsRefusedAsV5Input() {
        // the bare array older builds wrote: name and content only, placeholder names
        String legacy = "[{\"name\":\"RAW.txt\",\"content\":\"r\"},{\"name\":\"DRAFT.txt\",\"content\":\"d\"},"
                + "{\"name\":\"GLOSSARY.csv\",\"content\":\"source,target,category,note,priority\\n\"},{\"name\":\"PRONOUN.csv\",\"content\":\"from,speaker,target,self,call,scope,note\\n\"}]";
        EditorialApiRun run = new EditorialApiRun();
        run.rawText = "r";
        run.draftText = "d";
        run.originalSourceFilesJson = legacy;
        EditInputs restored = EditorialApiRunService.inputsOf(run, "Vietnamese");
        assertEquals(List.of("RAW", "DRAFT", "GLOSSARY", "PRONOUN"), restored.originalSourceFiles().stream().map(EditInputs.OriginalSourceFile::role).toList());
        assertEquals(V5SourceIdentity.NONE, restored.identity());
        assertEquals("a placeholder-named snapshot has no identity and is refused as V5 input", "V5_SOURCE_NAME_INVALID", V5SourcePackPreflight.check(restored).code());
    }

    @Test public void thoroughWithAFixIsThreeStoredCalls() {
        EditorialApiCombo combo = combo();
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.scripted(FakeEditorialApiProvider.edited(DRAFT.strip()),
                FakeEditorialApiProvider.issue("năm mươi", "năm trăm"), FakeEditorialApiProvider.pass());
        EditorialApiRunService service = service(provider);
        EditorialApiRun prepared = service.prepare(combo, sources(), "m", Mode.THOROUGH);
        EditorialApiRun run = service.execute(prepared.id, CAP, "Vietnamese", null);
        assertEquals(RunState.FINAL_OK, run.state);
        assertTrue(run.finalText.contains("năm trăm"));
        assertEquals(3, run.calls);
        assertEquals(3, provider.requests.size());
        assertTrue(EditorialApiRunCodec.reviewItems(run).isEmpty());
        assertTrue(run.issuesJson.contains("APPLIED"));
    }

    @Test public void wrongPairKeepsTheDraftAndTheEvidence() {
        EditorialApiRunService service = service(FakeEditorialApiProvider.scripted(FakeEditorialApiProvider.wrongPair("RAW: thi đấu; DRAFT: tiệc")));
        EditorialApiRun prepared = service.prepare(combo(), sources(), "m", Mode.THOROUGH);
        EditorialApiRun run = service.execute(prepared.id, CAP, "Vietnamese", null);
        assertEquals(RunState.WRONG_PAIR, run.state);
        assertEquals(DRAFT, run.finalText);
        assertTrue(run.wrongPairEvidence.contains("thi đấu"));
    }

    @Test public void technicalFailureIsRetriedOnceThenRetryRequiredWithTheErrorRecorded() {
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.scripted(FakeEditorialApiProvider.truncated(),
                EditorialApiFlow.StepResponse.failure("HTTP_503"));
        EditorialApiRunService service = service(provider);
        EditorialApiRun prepared = service.prepare(combo(), sources(), "m", Mode.QUICK);
        EditorialApiRun run = service.execute(prepared.id, CAP, "Vietnamese", null);
        assertEquals(RunState.RETRY_REQUIRED, run.state);
        assertEquals(DRAFT, run.finalText);
        assertEquals(2, run.calls);
        assertEquals("HTTP_503", run.error);
    }

    @Test public void costCapBlocksBeforeAnythingIsSent() {
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.scripted(FakeEditorialApiProvider.edited(EDITED));
        EditorialApiRunService service = service(provider);
        EditorialApiRun prepared = service.prepare(combo(), sources(), "m", Mode.QUICK);
        EditorialApiRun run = service.execute(prepared.id, new BigDecimal("0.001"), "Vietnamese", null);
        assertEquals(0, provider.requests.size());
        assertEquals(RunState.RETRY_REQUIRED, run.state);
        assertEquals(0, run.calls);
        assertEquals("COST_CAP", run.error);
        assertEquals(DRAFT, run.finalText);
        assertEquals(0, BigDecimal.ZERO.compareTo(run.usd));
    }

    @Test public void costCapCanStopOnlyTheCheckAndKeepTheEdit() {
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.scripted(FakeEditorialApiProvider.edited(EDITED.strip()),
                FakeEditorialApiProvider.pass());
        EditorialApiRunService service = service(provider);
        EditorialApiRun prepared = service.prepare(combo(), sources(), "m", Mode.THOROUGH);
        // worst case of the edit call is about USD 0.108 and the check about USD 0.113 once the edit has cost USD 0.004
        BigDecimal cap = new BigDecimal("0.110");
        EditorialApiRun run = service.execute(prepared.id, cap, "Vietnamese", null);
        assertEquals(1, provider.requests.size());
        assertEquals(RunState.FINAL_NOTES, run.state);
        assertEquals(EDITED.strip(), run.finalText);
        assertEquals(1, run.calls);
        assertTrue(run.guardsJson.contains("\"checkUnavailable\":true"));
    }

    @Test public void anUnknownOutcomeIsNotRepeatedAndTheRunSaysTheChargeIsUnknown() {
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.scripted(EditorialApiFlow.StepResponse.unknownOutcome("SocketTimeoutException: read timed out"),
                FakeEditorialApiProvider.edited(EDITED.strip()));
        EditorialApiRunService service = service(provider);
        EditorialApiRun run = service.execute(service.prepare(combo(), sources(), "m", Mode.QUICK).id, CAP, "Vietnamese", null);
        assertEquals(1, provider.requests.size());
        assertEquals(RunState.RETRY_REQUIRED, run.state);
        assertFalse(run.costKnown);
        assertEquals(1, run.calls);
        assertEquals(DRAFT, run.finalText);
        assertTrue(run.error, run.error.startsWith("SocketTimeoutException"));
        assertFalse(EditorialApiRunService.isInterrupted(run));
        assertTrue(EditorialApiPresenter.result(run, List.of()).detail.contains("chưa rõ số tiền"));
        // the stored row says the same after a reopen
        assertFalse(store.getRun(run.id).costKnown);
    }

    @Test public void aDefiniteRejectionBeforeDispatchStillGetsItsOneRetryAndKeepsTheCostKnown() {
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.scripted(EditorialApiFlow.StepResponse.failure("ApiHttpException: 429"),
                FakeEditorialApiProvider.edited(EDITED.strip()));
        EditorialApiRunService service = service(provider);
        EditorialApiRun run = service.execute(service.prepare(combo(), sources(), "m", Mode.QUICK).id, CAP, "Vietnamese", null);
        assertEquals(2, provider.requests.size());
        assertEquals(RunState.FINAL_OK, run.state);
        assertTrue(run.costKnown);
    }

    @Test public void anAnswerWithoutAPriceStaysReservedAtItsWorstCaseWhenCheckingTheCap() {
        EditorialApiFlow.StepResponse noPrice = new EditorialApiFlow.StepResponse("<EDITED>" + EDITED.strip() + "</EDITED>", "stop", 1500, 400,
                BigDecimal.ZERO, false, "m", "r", "");
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.scripted(noPrice, FakeEditorialApiProvider.pass());
        EditorialApiRunService service = service(provider);
        // edit worst case is about USD 0.108 and the check about USD 0.113: 0.20 admits the edit, not the check on top of an unpriced edit
        EditorialApiRun run = service.execute(service.prepare(combo(), sources(), "m", Mode.THOROUGH).id, new BigDecimal("0.20"), "Vietnamese", null);
        assertEquals(1, provider.requests.size());
        assertEquals(RunState.FINAL_NOTES, run.state);
        assertFalse(run.costKnown);
        assertTrue(run.guardsJson.contains("\"checkUnavailable\":true"));
        assertTrue(run.stepsJson.contains("COST_CAP"));
    }

    @Test public void aCallBilledAboveItsOwnWorstCaseStopsFurtherDispatchAndKeepsTheRealCharge() {
        EditorialApiFlow.StepResponse expensive = new EditorialApiFlow.StepResponse("<EDITED>" + EDITED.strip() + "</EDITED>", "stop", 1500, 400,
                new BigDecimal("5.0"), true, "m", "r", "");
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.scripted(expensive, FakeEditorialApiProvider.pass());
        EditorialApiRunService service = service(provider);
        EditorialApiRun run = service.execute(service.prepare(combo(), sources(), "m", Mode.THOROUGH).id, new BigDecimal("100"), "Vietnamese", null);
        assertEquals(1, provider.requests.size());
        assertEquals(RunState.FINAL_NOTES, run.state);
        assertEquals(0, new BigDecimal("5.0").compareTo(run.usd));
        assertTrue(run.stepsJson.contains("COST_BOUND_EXCEEDED"));
        assertEquals(EDITED.strip(), run.finalText);
    }

    @Test public void cancelDuringACallEndsTheRunWithTheDraft() {
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.editsAndPasses(EDITED.strip());
        EditorialApiRunService service = service(provider);
        provider.beforeAnswer = service::cancel;
        EditorialApiRun prepared = service.prepare(combo(), sources(), "m", Mode.THOROUGH);
        EditorialApiRun run = service.execute(prepared.id, CAP, "Vietnamese", null);
        assertEquals(RunState.CANCELLED, run.state);
        assertEquals(DRAFT, run.finalText);
        assertTrue(provider.cancelled);
        assertEquals(1, provider.requests.size());
        assertEquals(1, run.calls);
        assertEquals(0, new BigDecimal("0.004").compareTo(run.usd));
        assertTrue(run.inputTokens > 0);
        assertTrue(run.stepsJson.contains("CANCELLED"));
        assertEquals(run.usd, store.getRun(prepared.id).usd);
    }

    @Test public void aRunLeftRunningByARestartIsInterruptedAndNeverResentByOpeningIt() {
        EditorialApiCombo combo = combo();
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.editsAndPasses(EDITED.strip());
        EditorialApiRun prepared = service(provider).prepare(combo, sources(), "m", Mode.QUICK);
        // the process dies here: only the stored row remains
        EditorialApiRun reopened = store.latestRun(combo.id);
        assertTrue(EditorialApiRunService.isInterrupted(reopened));
        assertFalse(EditorialApiRunService.isActive(reopened.id));
        assertEquals(DRAFT, reopened.finalText);
        assertEquals(0, provider.requests.size());
        // running again is an explicit second run, never an automatic resend
        EditorialApiRun second = service(provider).prepare(combo, sources(), "m", Mode.QUICK);
        assertTrue(second.id > prepared.id);
        EditorialApiRun done = service(provider).execute(second.id, CAP, "Vietnamese", null);
        assertEquals(RunState.FINAL_OK, done.state);
        assertFalse(EditorialApiRunService.isInterrupted(done));
        // the first row is still interrupted history; executing a finished run sends nothing
        int before = provider.requests.size();
        service(provider).execute(second.id, CAP, "Vietnamese", null);
        assertEquals(before, provider.requests.size());
    }

    @Test public void anUnexpectedExceptionNeverLeavesTheRunRunning() {
        FakeEditorialApiProvider provider = new FakeEditorialApiProvider((request, index) -> { throw new IllegalStateException("boom"); });
        EditorialApiRunService service = service(provider);
        EditorialApiRun prepared = service.prepare(combo(), sources(), "m", Mode.QUICK);
        EditorialApiRun run = service.execute(prepared.id, CAP, "Vietnamese", null);
        assertEquals(RunState.RETRY_REQUIRED, run.state);
        assertEquals("INTERNAL:IllegalStateException", run.error);
        assertEquals(DRAFT, run.finalText);
        assertEquals(RunState.RETRY_REQUIRED, store.getRun(prepared.id).state);
        assertFalse(EditorialApiRunService.isActive(prepared.id));
    }

    @Test public void listenerSeesEveryStepAndTheEnd() {
        FakeEditorialApiProvider provider = FakeEditorialApiProvider.editsAndPasses(EDITED.strip());
        EditorialApiRunService service = service(provider);
        EditorialApiRun prepared = service.prepare(combo(), sources(), "m", Mode.THOROUGH);
        StringBuilder log = new StringBuilder();
        service.execute(prepared.id, CAP, "Vietnamese", new EditorialApiRunService.Listener() {
            @Override public void onStep(EditorialApiRun run, EditorialApiContract.Step step, int attempt) { log.append(step).append(attempt).append(' '); }

            @Override public void onFinished(EditorialApiRun run) { log.append(run.state); }
        });
        assertEquals("EDIT1 CHECK1 FINAL_OK", log.toString());
    }

    @Test public void snapshotComparisonNamesTheSourcesThatChanged() {
        EditorialApiSources base = sources();
        EditorialApiRun run = service(FakeEditorialApiProvider.scripted()).prepare(combo(), base, "m", Mode.QUICK);
        assertTrue(EditorialApiSources.changedParts(run, base).isEmpty());
        EditorialApiSources edited = new EditorialApiSources(RAW, DRAFT + "thêm", "", List.of(), "p", "Vietnamese");
        assertEquals(List.of("DRAFT", "PRONOUN"), EditorialApiSources.changedParts(run, edited));
    }

    @Test public void theSnapshotRebuildsTheSameInputsIncludingGlossary() {
        List<EditInputs.GlossaryEntry> glossary = List.of(new EditInputs.GlossaryEntry("太郎", "Taro", "name", "main"),
                new EditInputs.GlossaryEntry("花子", "Hanako", "", ""));
        EditorialApiSources withGlossary = new EditorialApiSources(RAW, DRAFT, EditorialApiSources.glossaryAsText(glossary), glossary,
                "太郎,太郎,花子,tôi,em,scene,note", "Vietnamese");
        EditorialApiRun run = service(FakeEditorialApiProvider.scripted()).prepare(combo(), withGlossary, "m", Mode.QUICK);
        assertEquals(2, run.glossaryEntries);
        assertEquals(1, run.pronounRows);
        EditInputs rebuilt = EditorialApiRunService.inputsOf(store.getRun(run.id), "Vietnamese");
        assertEquals(glossary, rebuilt.glossary());
        assertEquals(withGlossary.pronounText, rebuilt.pronounCsv());
        assertNotNull(run.qualityCoreSha256);
        assertEquals(64, run.qualityCoreSha256.length());
        assertEquals(EditorialApiContract.CONTRACT_REVISION, run.contractRevision);
    }

    @Test public void comboCrudAndSettingsRoundTripAndDeleteRemovesItsRuns() {
        EditorialApiCombo combo = combo();
        EditorialApiCombo.Settings settings = new EditorialApiCombo.Settings();
        settings.mode = Mode.THOROUGH;
        settings.model = "x/y";
        settings.maxUsdPerChapter = new BigDecimal("0.25");
        combo.settingsJson = settings.toJson();
        store.updateCombo(combo);
        EditorialApiCombo.Settings read = store.getCombo(combo.id).settings();
        assertEquals(Mode.THOROUGH, read.mode);
        assertEquals("x/y", read.model);
        assertEquals(0, new BigDecimal("0.25").compareTo(read.maxUsdPerChapter));
        EditorialApiCombo.Settings broken = EditorialApiCombo.Settings.fromJson("{not json");
        assertEquals(Mode.QUICK, broken.mode);
        assertEquals(0, EditorialApiCombo.DEFAULT_MAX_USD.compareTo(broken.maxUsdPerChapter));
        EditorialApiRun run = service(FakeEditorialApiProvider.scripted()).prepare(combo, sources(), "m", Mode.QUICK);
        store.deleteCombo(combo.id);
        assertEquals(null, store.getCombo(combo.id));
        assertEquals(null, store.getRun(run.id));
        assertEquals("raw001+draft001+glossary001+pronoun001",
                EditorialApiCombo.suggestName("raw001.txt", "draft001.txt", "glossary001.csv", "pronoun001.csv"));
        assertEquals("raw+draft", EditorialApiCombo.suggestName("", null, "", null));
    }
}
