package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The wording and decisions of the Biên tập screens, without any view. */
public final class EditorialApiPresenterTest {
    private static final BigDecimal IN = new BigDecimal("0.0000003");
    private static final BigDecimal OUT = new BigDecimal("0.0000015");
    private static final String PRONOUN_CSV = "from,speaker,target,self,call,scope,note\nA,A,B,tôi,anh,all,\n";

    private static EditorialApiCombo combo() {
        EditorialApiCombo combo = new EditorialApiCombo();
        combo.id = 7;
        combo.name = "raw001+draft001";
        combo.rawUri = "content://x/raw";
        combo.rawName = "raw001.txt";
        combo.draftUri = "content://x/draft";
        combo.draftName = "draft001.txt";
        combo.settingsJson = new EditorialApiCombo.Settings().toJson();
        return combo;
    }

    private static EditorialApiSources sources(String raw, String draft, boolean glossary, boolean pronoun) {
        List<EditInputs.GlossaryEntry> entries = glossary ? List.of(new EditInputs.GlossaryEntry("勇者", "Dũng giả", "", "")) : List.of();
        return new EditorialApiSources(raw, draft, EditorialApiSources.glossaryAsText(entries), entries, pronoun ? PRONOUN_CSV : "", "Vietnamese");
    }

    private static EditorialApiRun run(EditorialApiContract.RunState state) {
        EditorialApiRun run = new EditorialApiRun();
        run.id = 3;
        run.state = state;
        run.model = "fake-model";
        run.rawText = "原文";
        run.draftText = "Bản nháp\nDòng hai";
        run.finalText = "Bản cuối\nDòng hai";
        run.rawSha256 = HashUtil.sha256(run.rawText);
        run.draftSha256 = HashUtil.sha256(run.draftText);
        return run;
    }

    @Test public void everyRunStateHasAPlainStatusAndTheInterruptedOneAsksToRunAgain() {
        assertEquals("Chưa chạy", EditorialApiPresenter.statusLabel(null));
        assertEquals("Xong", EditorialApiPresenter.statusLabel(run(EditorialApiContract.RunState.FINAL_OK)));
        assertEquals("Xong — có mục cần xem", EditorialApiPresenter.statusLabel(run(EditorialApiContract.RunState.FINAL_NOTES)));
        assertEquals("Đã hủy", EditorialApiPresenter.statusLabel(run(EditorialApiContract.RunState.CANCELLED)));
        assertTrue(EditorialApiPresenter.statusLabel(run(EditorialApiContract.RunState.WRONG_PAIR)).contains("khác chương"));
        assertTrue(EditorialApiPresenter.statusLabel(run(EditorialApiContract.RunState.RETRY_REQUIRED)).contains("chạy lại"));
        // a RUNNING row that nothing in this process executes was cut off
        assertTrue(EditorialApiPresenter.statusLabel(run(EditorialApiContract.RunState.RUNNING)).contains("gián đoạn"));
    }

    @Test public void theListRowOffersTheResultOnlyForAFinishedRun() {
        assertFalse(EditorialApiPresenter.row(combo(), null).hasResult);
        assertTrue(EditorialApiPresenter.row(combo(), run(EditorialApiContract.RunState.FINAL_OK)).hasResult);
        assertFalse(EditorialApiPresenter.row(combo(), run(EditorialApiContract.RunState.RUNNING)).hasResult);
    }

    @Test public void bothFilesAreRequiredButReferencesAreNot() {
        EditorialApiCombo combo = combo();
        assertEquals("", EditorialApiPresenter.missingForConfirmation(combo));
        combo.draftUri = "";
        assertEquals("Chọn file DRAFT.", EditorialApiPresenter.missingForConfirmation(combo));
        combo.rawUri = "";
        assertEquals("Chọn file RAW và file DRAFT.", EditorialApiPresenter.missingForConfirmation(combo));
    }

    @Test public void theDefaultNameJoinsTheChosenParts() {
        assertEquals("raw001+draft001+glossary001+pronoun001", EditorialApiPresenter.suggestedName(combo(), "glossary001.csv", "pronoun001"));
        assertEquals("raw001+draft001", EditorialApiPresenter.suggestedName(combo(), "", ""));
    }

    @Test public void theConfirmationShowsNamesFirstLinesCharCountsAndTheReferences() {
        EditorialApiSources s = sources("一行目\n\n二行目\n三行目\n四行目", "Dòng một\nDòng hai\nDòng ba\nDòng bốn", true, true);
        EditorialApiPresenter.Confirmation c = EditorialApiPresenter.confirmation(combo(), s, "glossary001", "pronoun001", "fake-model", IN, OUT);
        assertEquals("raw001.txt", c.rawName);
        assertEquals("draft001.txt", c.draftName);
        assertEquals(s.raw.length(), c.rawChars);
        assertEquals("一行目\n二行目\n三行目", c.rawHead);
        assertEquals(3, c.draftHead.split("\n").length);
        assertEquals("Glossary: glossary001 — 1 mục", c.glossaryLine);
        assertEquals("Pronoun: pronoun001", c.pronounLine);
        assertTrue(c.warnings.isEmpty());
        assertTrue(c.canRun());
        assertTrue(c.costLine, c.costLine.startsWith("Ước tính chi phí: khoảng USD "));
        assertTrue(c.costLine, c.costLine.contains("tối đa USD 0.1 "));
    }

    @Test public void missingReferencesWarnButDoNotBlock() {
        EditorialApiSources s = sources("原文です", "Văn bản", false, false);
        EditorialApiPresenter.Confirmation c = EditorialApiPresenter.confirmation(combo(), s, "", "", "fake-model", IN, OUT);
        assertEquals("Không dùng Glossary", c.glossaryLine);
        assertEquals("Không dùng Pronoun", c.pronounLine);
        assertEquals(2, c.warnings.size());
        assertTrue(c.canRun());
    }

    @Test public void aModelWithoutAKnownPriceWarnsThatTheCapMayStopTheRun() {
        EditorialApiSources s = sources("原文です", "Văn bản", true, true);
        EditorialApiPresenter.Confirmation known = EditorialApiPresenter.confirmation(combo(), s, "g", "p", "m", IN, OUT, true);
        EditorialApiPresenter.Confirmation unknown = EditorialApiPresenter.confirmation(combo(), s, "g", "p", "m", IN, OUT, false);
        assertTrue(known.warnings.isEmpty());
        assertEquals(1, unknown.warnings.size());
        assertTrue(unknown.warnings.get(0).contains("chạm trần"));
        assertTrue(unknown.canRun());
    }

    @Test public void anOddLengthRatioWarnsOnceAndDoesNotBlock() {
        EditorialApiSources s = sources("あ".repeat(400), "Ngắn", true, true);
        EditorialApiPresenter.Confirmation c = EditorialApiPresenter.confirmation(combo(), s, "g", "p", "fake-model", IN, OUT);
        assertEquals(1, c.warnings.size());
        assertTrue(c.warnings.get(0).contains("khác thường"));
        assertTrue(c.canRun());
    }

    @Test public void emptySourcesATooLongChapterAndAMissingModelBlock() {
        assertFalse(EditorialApiPresenter.confirmation(combo(), sources("", "Văn bản", true, true), "g", "p", "m", IN, OUT).canRun());
        assertFalse(EditorialApiPresenter.confirmation(combo(), sources("原文", "  ", true, true), "g", "p", "m", IN, OUT).canRun());
        EditorialApiPresenter.Confirmation tooLong = EditorialApiPresenter.confirmation(combo(),
                sources("あ".repeat(60_000), "Việt ".repeat(12_000), true, true), "g", "p", "m", IN, OUT);
        assertFalse(tooLong.canRun());
        assertTrue(tooLong.blockers.get(0).contains("quá dài"));
        EditorialApiPresenter.Confirmation noModel = EditorialApiPresenter.confirmation(combo(), sources("原文", "Văn bản", true, true), "g", "p", "", IN, OUT);
        assertFalse(noModel.canRun());
        assertTrue(noModel.blockers.get(0).contains("Cài đặt"));
    }

    @Test public void thoroughCostsMoreThanQuickAndBothStayBelowTheCapForANormalChapter() {
        EditorialApiSources s = sources("あ".repeat(3000), "Việt ".repeat(1200), true, true);
        EditorialApiCombo.Settings quick = new EditorialApiCombo.Settings();
        EditorialApiCombo.Settings thorough = new EditorialApiCombo.Settings();
        thorough.mode = EditorialApiContract.Mode.THOROUGH;
        String q = EditorialApiPresenter.costLine(s, quick, IN, OUT);
        String t = EditorialApiPresenter.costLine(s, thorough, IN, OUT);
        double quickUsd = Double.parseDouble(q.substring(q.indexOf("USD ") + 4, q.indexOf(" (")));
        double thoroughUsd = Double.parseDouble(t.substring(t.indexOf("USD ") + 4, t.indexOf(" (")));
        assertTrue(quickUsd > 0.0);
        assertTrue(thoroughUsd > quickUsd);
        assertTrue(thoroughUsd < 0.10);
    }

    @Test public void progressNamesTheStepAndMarksARetry() {
        assertEquals("Đang biên tập…", EditorialApiPresenter.progressLine(EditorialApiContract.Step.EDIT, 1));
        assertEquals("Đang kiểm…", EditorialApiPresenter.progressLine(EditorialApiContract.Step.CHECK, 1));
        assertEquals("Đang kiểm lại…", EditorialApiPresenter.progressLine(EditorialApiContract.Step.RECHECK, 1));
        assertEquals("Đang kiểm (thử lại)…", EditorialApiPresenter.progressLine(EditorialApiContract.Step.CHECK, 2));
    }

    @Test public void aFinalResultCanBeExportedAndComparedAndAWrongPairCannot() {
        EditorialApiPresenter.Result ok = EditorialApiPresenter.result(run(EditorialApiContract.RunState.FINAL_OK), List.of());
        assertTrue(ok.canExport && ok.canCompare);
        assertEquals("Đã biên tập xong", ok.headline);
        EditorialApiRun wrong = run(EditorialApiContract.RunState.WRONG_PAIR);
        wrong.wrongPairEvidence = "RAW nói về A, DRAFT nói về B";
        EditorialApiPresenter.Result r = EditorialApiPresenter.result(wrong, List.of());
        assertFalse(r.canExport);
        assertTrue(r.headline.contains("không cùng một chương"));
        assertEquals("RAW nói về A, DRAFT nói về B", r.detail);
        assertFalse(EditorialApiPresenter.result(run(EditorialApiContract.RunState.CANCELLED), List.of()).canExport);
    }

    @Test public void changedSourcesAskToRunAgainInPlainWords() {
        EditorialApiPresenter.Result r = EditorialApiPresenter.result(run(EditorialApiContract.RunState.FINAL_OK), List.of("RAW", "GLOSSARY"));
        assertEquals(List.of("file RAW", "Glossary"), r.staleParts);
        assertEquals("Nguồn đã thay đổi sau lần chạy này (file RAW, Glossary). Chạy lại?", EditorialApiPresenter.staleMessage(r.staleParts));
        assertEquals("", EditorialApiPresenter.staleMessage(List.of()));
    }

    @Test public void reviewItemsAndFlagsReadAsPlainWords() {
        EditorialApiRun run = run(EditorialApiContract.RunState.FINAL_NOTES);
        run.issuesJson = "[{\"step\":\"CHECK\",\"status\":\"NEEDS_REVIEW\",\"reason\":\"NO_MATCH\",\"kind\":\"NUMBER\","
                + "\"editedQuote\":\"ba người\",\"rawQuote\":\"二人\",\"fix\":\"hai người\"},"
                + "{\"step\":\"CHECK\",\"status\":\"APPLIED\",\"reason\":\"\",\"kind\":\"MEANING\",\"editedQuote\":\"x\",\"rawQuote\":\"y\",\"fix\":\"z\"}]";
        run.guardsJson = "{\"flags\":[{\"code\":\"REWRITE_WARN\",\"detail\":\"40% of the draft changed\"},{\"code\":\"GLOSSARY_WARN\",\"detail\":\"勇者 -> Dũng giả\"}]}";
        EditorialApiPresenter.Result r = EditorialApiPresenter.result(run, List.of());
        assertEquals(1, r.review.size());
        assertEquals("Sai số", r.review.get(0).kind);
        assertEquals("hai người", r.review.get(0).fix);
        assertEquals("Bản cuối thay đổi nhiều so với DRAFT (40% of the draft changed)", r.flags.get(0));
        assertEquals("Chưa thấy trong bản cuối: 勇者 -> Dũng giả", r.flags.get(1));
    }

    @Test public void theTechnicalDetailsKeepModelHashesAndCostOutOfTheMainView() {
        EditorialApiRun run = run(EditorialApiContract.RunState.FINAL_OK);
        run.qualityCoreSha256 = "a".repeat(64);
        run.calls = 2;
        run.usd = new BigDecimal("0.0123");
        String technical = EditorialApiPresenter.technical(run);
        assertTrue(technical.contains("fake-model"));
        assertTrue(technical.contains("a".repeat(64)));
        assertTrue(technical.contains("RAW sha256: " + run.rawSha256));
        assertTrue(technical.contains("USD 0.0123"));
        EditorialApiPresenter.Result r = EditorialApiPresenter.result(run, List.of());
        assertFalse(r.headline.contains("sha256"));
        assertFalse(r.detail.contains("sha256"));
    }

    @Test public void theComparisonListsOnlyChangedPassagesAndSaysSoWhenThereAreNone() {
        List<String[]> rows = EditorialApiPresenter.diffRows("Bản nháp\nDòng hai", "Bản cuối\nDòng hai");
        assertEquals(1, rows.size());
        assertEquals("Bản nháp", rows.get(0)[1]);
        assertEquals("Bản cuối", rows.get(0)[2]);
        assertEquals("Bản cuối giống hệt DRAFT.", EditorialApiPresenter.diffSummary("a\nb", "a\nb"));
        assertEquals("1 chỗ khác DRAFT.", EditorialApiPresenter.diffSummary("a\nb", "a\nc"));
    }

    @Test public void theTabIsCalledBienTap() {
        assertEquals("Biên tập", EditorialApiPresenter.TAB_TITLE);
    }
}
