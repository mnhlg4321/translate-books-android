package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
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
