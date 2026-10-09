package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import com.ml.tblandroidtxt.editorial.api.PairStates.RunState;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** What the person reads about a pair run: why it cannot run, the three separate statuses, warnings and the export label. */
public final class EditorialPairPresenterTest {
    private static EditorialPairPreview preview(EditorialPairSource source, String glossary, String pronoun) {
        return EditorialPairPreview.of(source, EditorialPairSnapshot.glossaryFrom(glossary), pronoun, null);
    }

    private static EditorialPairSource job(List<String> draft) {
        List<String> raw = new ArrayList<>();
        for (int i = 0; i < draft.size(); i++) raw.add(EditorialPairRunServiceTest.rawRow(i + 1));
        return new EditorialPairSource("JOB", "7", "truyen.txt", "", raw, draft, List.of());
    }

    @Test public void everyBlockerIsAnExplanationAndTwoFilesGetTheMappingReason() {
        EditorialPairPreview files = preview(EditorialPairSource.unmappedFiles("a.txt + b.txt"), "", "");
        assertFalse(files.runnable());
        String text = EditorialPairPresenter.blockers(files).get(0);
        assertTrue(text, text.contains("chưa có liên kết đoạn"));
        assertTrue(text.contains("job Dịch"));
        assertEquals("Đoạn 3 của job không nối liền với đoạn trước (RAW bị hở hoặc trùng).", EditorialPairPresenter.blocker("SOURCE_ROW_OFFSET_GAP:3"));
        assertTrue(EditorialPairPresenter.blocker("MISSING_PAIRS:2").startsWith("Còn 2 đoạn chưa có bản dịch hoàn tất"));
        assertTrue(EditorialPairPresenter.blocker("TOO_LONG:[4, 5]").contains("4, 5"));
        assertTrue(EditorialPairPresenter.blocker("MAP_RAW_COVERAGE_GAP").contains("COVERAGE_GAP"));
        assertTrue(EditorialPairPresenter.blocker("SOMETHING_NEW").contains("SOMETHING_NEW"));
    }

    @Test public void aMissingTranslationIsListedNotHiddenAndStopsTheRun() {
        List<String> draft = new ArrayList<>();
        draft.add(EditorialPairRunServiceTest.draftRow(1));
        draft.add(null);
        draft.add(EditorialPairRunServiceTest.draftRow(3));
        EditorialPairPreview p = preview(job(draft), "", "");
        assertEquals(3, p.rows.size());
        assertEquals("Đoạn 2 • RAW " + p.rows.get(1).rawChars + " chữ • DRAFT 0 chữ • Thiếu bản dịch", EditorialPairPresenter.previewRow(p.rows.get(1)));
        assertEquals("3 đoạn, 1 thiếu bản dịch • chưa thể chạy", EditorialPairPresenter.previewTotals(p));
        assertTrue(EditorialPairPresenter.blockers(p).get(0).contains("Còn 1 đoạn chưa có bản dịch"));
    }

    @Test public void aRunnablePreviewListsTheReferencesAndOnlyWarnsAboutTheirAbsence() {
        List<String> draft = List.of(EditorialPairRunServiceTest.draftRow(1), EditorialPairRunServiceTest.draftRow(2));
        EditorialPairPreview none = preview(job(draft), "", "");
        assertTrue(none.runnable());
        List<String> warnings = EditorialPairPresenter.warnings(none);
        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).startsWith("Không dùng Glossary"));
        assertTrue(warnings.get(1).startsWith("Không dùng Pronoun"));
        assertEquals("2 đoạn", EditorialPairPresenter.previewTotals(none));
        EditorialPairPreview with = preview(job(draft), "花子\tHanako\tname\t\n", "from,speaker,target,self,call,scope,note\n花子,花子,太郎,tôi,anh,*,\n");
        assertTrue(with.warnings.isEmpty());
        assertEquals(1, with.rows.get(0).glossaryEntries);
        assertEquals(1, with.rows.get(0).pronounRows);
    }

    @Test public void aReferenceConflictAndABadScopeAreWarningsInPlainWords() {
        List<String> draft = List.of(EditorialPairRunServiceTest.draftRow(1));
        String pronoun = "from,speaker,target,self,call,scope,note\n"
                + "花子,花子,太郎,tôi,anh,*,a\n花子,花子,太郎,em,anh,*,b\n花子,花子,太郎,x,y,p000,c\n";
        EditorialPairPreview p = preview(job(draft), "", pronoun);
        assertTrue(p.warnings.toString(), p.warnings.contains("REFERENCE_CONFLICT:1"));
        assertTrue(p.warnings.contains("SCOPE_INVALID:1"));
        assertTrue(EditorialPairPresenter.warning("REFERENCE_CONFLICT:1").contains("không tự chọn"));
        assertTrue(EditorialPairPresenter.warning("SCOPE_INVALID:1").contains("bị bỏ qua"));
        assertTrue(p.runnable());
    }

    @Test public void theCostLineNamesThePairCountAndTheCapAndIsHonestThatTheCapIsPerCall() {
        EditorialPairPreview p = preview(job(List.of(EditorialPairRunServiceTest.draftRow(1), EditorialPairRunServiceTest.draftRow(2))), "", "");
        String line = EditorialPairPresenter.costLine(p, new BigDecimal("0.00000025"), new BigDecimal("0.0000012"), new BigDecimal("0.05"));
        assertTrue(line, line.startsWith("Ước tính chi phí: khoảng USD "));
        assertTrue(line.contains("2 lượt gọi"));
        assertTrue(line.contains("trần USD 0.05"));
        assertTrue(line.contains("giữ chỗ chi phí trước khi gửi"));
    }

    // ---- three separate statuses

    private static PairRun run(RunState state) {
        PairRun r = new PairRun();
        r.id = 99;
        r.state = state;
        r.model = "m";
        return r;
    }

    private static PairItem item(int ordinal, PairState state) {
        PairItem i = new PairItem();
        i.ordinal = ordinal;
        i.state = state;
        return i;
    }

    @Test public void structureMeaningAndSavingAreThreeSeparateLinesAndNothingSaysPassed() {
        List<PairItem> items = List.of(item(1, PairState.ACCEPTED), item(2, PairState.WARN_REVIEW), item(3, PairState.STRUCTURE_BLOCKED), item(4, PairState.UNKNOWN));
        PairRun run = run(RunState.UNKNOWN);
        List<String> lines = EditorialPairPresenter.statusLines(run, items);
        assertEquals("Cấu trúc: 1 đoạn đạt, 1 có cảnh báo, 1 bị chặn, 1 không rõ, trên tổng 4 đoạn.", lines.get(0));
        assertEquals("Nghĩa: chưa được chấm. Ghép thành công không có nghĩa là bản dịch đúng.", lines.get(1));
        assertTrue(lines.get(2).startsWith("Lưu và xuất: tiến độ đã lưu"));
        run.costKnown = false;
        run.costOverrun = true;
        List<String> withCost = EditorialPairPresenter.statusLines(run, items);
        assertEquals(5, withCost.size());
        assertTrue(withCost.get(3).contains("chưa rõ chi phí"));
        for (String l : withCost) assertFalse(l, l.contains("đã đạt") || l.contains("không còn lỗi"));
    }

    @Test public void headlinesNeverPromiseAResultAndShowTheWarningCount() {
        PairRun ok = run(RunState.FINAL_ELIGIBLE);
        assertEquals("Đã ghép đủ các đoạn", EditorialPairPresenter.runHeadline(ok, false, false));
        ok.warnings = 3;
        assertEquals("Đã ghép đủ các đoạn, có 3 cảnh báo cấu trúc", EditorialPairPresenter.runHeadline(ok, false, false));
        assertEquals("Đang biên tập từng đoạn…", EditorialPairPresenter.runHeadline(ok, true, false));
        assertEquals("Lần chạy bị gián đoạn", EditorialPairPresenter.runHeadline(run(RunState.RUNNING), false, true));
        assertTrue(EditorialPairPresenter.runHeadline(run(RunState.FINAL_BLOCKED), false, false).contains("chưa ghép được bản đủ"));
        assertTrue(EditorialPairPresenter.runHeadline(run(RunState.UNKNOWN), false, false).contains("không rõ kết quả"));
        for (RunState s : RunState.values()) {
            String text = EditorialPairPresenter.runHeadline(run(s), false, false);
            assertFalse(s + " " + text, text.contains("đạt") || text.contains("hoàn tất"));
        }
    }

    @Test public void everyPairStateHasAPlainLabelAndAnUnknownPairSaysItIsNotResent() {
        for (PairState s : PairState.values()) assertFalse(s.name(), EditorialPairPresenter.pairState(s).isEmpty());
        assertTrue(EditorialPairPresenter.pairState(PairState.UNKNOWN).contains("không gửi lại"));
        assertTrue(EditorialPairPresenter.pairState(PairState.WARN_REVIEW).contains("cần xem"));
        assertTrue(EditorialPairPresenter.pairState(PairState.ACCEPTED).contains("qua kiểm cấu trúc"));
    }

    @Test public void aRunLeftSentByAProcessThatIsGoneIsReportedAsInterrupted() {
        PairRun run = run(RunState.RUNNING);
        assertTrue(EditorialPairPresenter.interrupted(run, List.of(item(1, PairState.E_SENT))));
        assertTrue(EditorialPairPresenter.interrupted(run, List.of(item(1, PairState.E_RESERVED))));
        assertFalse(EditorialPairPresenter.interrupted(run, List.of(item(1, PairState.ACCEPTED), item(2, PairState.IMPORTED))));
    }

    @Test public void gateCodesReadAsPlainWordsAndBlockVersusWarningIsShown() {
        PairItem i = item(1, PairState.STRUCTURE_BLOCKED);
        i.gateJson = "{\"status\":\"BLOCK\",\"codes\":[{\"gate\":\"SIZE\",\"code\":\"CHARS_LOSS\",\"severity\":\"BLOCK\",\"detail\":\"200 of 1000 letters\"},"
                + "{\"gate\":\"SIZE\",\"code\":\"LINE_DELTA\",\"severity\":\"WARN\",\"detail\":\"192 -> 37 lines\"}]}";
        List<String> lines = EditorialPairPresenter.gateLines(i);
        assertEquals("Chặn: Mất nhiều chữ so với DRAFT (200 of 1000 letters)", lines.get(0));
        assertEquals("Cảnh báo: Số dòng thay đổi (192 -> 37 lines)", lines.get(1));
        assertTrue(EditorialPairPresenter.gateLines(item(2, PairState.ACCEPTED)).isEmpty());
        assertEquals("Chỉ xuống dòng lại, chữ giữ nguyên", EditorialPairPresenter.gateCode("REFLOW_ONLY"));
    }

    @Test public void exportFileNamesCarryTheKindOfTheResult() {
        EditorialPairRunService.ExportPlan finalPlan = new EditorialPairRunService.ExportPlan("t", "l", true, "_bien-tap");
        EditorialPairRunService.ExportPlan tmp = new EditorialPairRunService.ExportPlan("t", "l", false, "_tam");
        assertEquals("truyen_bien-tap.txt", EditorialPairPresenter.exportName("truyen", finalPlan));
        assertEquals("truyen_tam.txt", EditorialPairPresenter.exportName("truyen", tmp));
        assertTrue(EditorialPairPresenter.exportName("", tmp).startsWith("editorial"));
    }
}
