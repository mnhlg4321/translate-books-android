package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract.Mode;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract.RunState;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract.Step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** The run as a state machine: calls per mode, technical retries, wrong pair, and the P6-shaped irregularities. */
public final class EditorialApiFlowTest {
    private static final String RAW = "太郎は赤い扉を開けた。\n花子が言った。「五百メートル先です」";
    private static final String DRAFT = "Taro mở cánh cửa đỏ.\nHanako nói: 「Cách năm mươi mét.」";
    private static final String EDITED = "Taro mở cánh cửa đỏ.\nHanako nói: 「Cách năm trăm mét.」";

    private static EditInputs inputs() { return new EditInputs(RAW, DRAFT, "Vietnamese", List.of(), ""); }

    private static EditorialApiFlow.StepResponse ok(String content, String finish) {
        return new EditorialApiFlow.StepResponse(content, finish, 1000, 200, new BigDecimal("0.01"), true, "model-x", "route-y", "");
    }

    private static String edited(String text) { return "<EDITED>" + text + "</EDITED>"; }

    private static final String PASS = "{\"verdict\":\"PASS\",\"wrong_pair_evidence\":\"\",\"issues\":[]}";

    private static String issues(String quote, String fix) {
        return "{\"verdict\":\"ISSUES\",\"wrong_pair_evidence\":\"\",\"issues\":[{\"edited_quote\":\"" + quote
                + "\",\"raw_quote\":\"\",\"kind\":\"NUMBER\",\"fix\":\"" + fix + "\"}]}";
    }

    /** Drives a flow with scripted answers (one per request) and returns the steps asked, in order. */
    private static List<Step> drive(EditorialApiFlow flow, List<EditorialApiFlow.StepResponse> answers) {
        List<Step> asked = new ArrayList<>();
        int next = 0;
        EditorialApiFlow.Request request;
        while ((request = flow.nextRequest()) != null) {
            assertTrue("script exhausted at " + request.step(), next < answers.size());
            asked.add(request.step());
            flow.accept(answers.get(next++));
            assertTrue("never more than six requests", asked.size() <= 6);
        }
        assertEquals("every scripted answer was used", answers.size(), next);
        return asked;
    }

    @Test public void quickIsOneCall() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.QUICK));
        List<Step> asked = drive(flow, List.of(ok(edited(EDITED), "stop")));
        assertEquals(List.of(Step.EDIT), asked);
        assertEquals(RunState.FINAL_OK, flow.outcome().state());
        assertEquals(EDITED, flow.outcome().finalText());
        assertEquals(1, flow.outcome().calls());
        assertEquals(0, new BigDecimal("0.01").compareTo(flow.outcome().cost()));
        assertEquals(1000, flow.outcome().inputTokens());
        assertNull(flow.nextRequest());
    }

    @Test public void quickWithNotesOrFlagsIsFinalNotes() {
        EditorialApiFlow notes = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.QUICK));
        drive(notes, List.of(ok(edited(EDITED) + "<NOTES>năm trăm | NUMBER | RAW nói 500</NOTES>", "stop")));
        assertEquals(RunState.FINAL_NOTES, notes.outcome().state());
        assertEquals(1, notes.outcome().notes().size());
        EditorialApiFlow flagged = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.QUICK));
        drive(flagged, List.of(ok(edited("Taro mở cánh cửa đỏ.\nHanako nói: 「Cách năm trăm mét."), "stop")));
        assertEquals(RunState.FINAL_NOTES, flagged.outcome().state());
        assertTrue(flagged.outcome().guards().has(EditGuards.Code.SYMBOL_WARN));
    }

    @Test public void thoroughPassIsTwoCalls() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(flow, List.of(ok(edited(EDITED), "stop"), ok(PASS, "stop")));
        assertEquals(List.of(Step.EDIT, Step.CHECK), asked);
        assertEquals(RunState.FINAL_OK, flow.outcome().state());
        assertEquals(2, flow.outcome().calls());
    }

    @Test public void thoroughWithAnAppliedFixIsThreeCallsAndTheFixIsInTheFinalText() {
        String slip = "Taro mở cánh cửa đỏ.\nHanako nói: 「Cách năm mươi mét.」";
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(flow, List.of(ok(edited(slip), "stop"), ok(issues("năm mươi", "năm trăm"), "stop"), ok(PASS, "stop")));
        assertEquals(List.of(Step.EDIT, Step.CHECK, Step.RECHECK), asked);
        assertEquals(RunState.FINAL_OK, flow.outcome().state());
        assertEquals(EDITED, flow.outcome().finalText());
        assertEquals(appliedCount(flow), 1);
        assertEquals(3, flow.outcome().calls());
    }

    private static int appliedCount(EditorialApiFlow flow) {
        int n = 0;
        for (EditorialApiFlow.IssueRecord r : flow.outcome().issues()) if (r.status() == EditorialApiFlow.IssueStatus.APPLIED) n++;
        return n;
    }

    @Test public void recheckThatStillReportsAnIssueEndsWithNotesAndNeverASixthCall() {
        String slip = "Taro mở cánh cửa đỏ.\nHanako nói: 「Cách năm mươi mét.」";
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(flow, List.of(ok(edited(slip), "stop"), ok(issues("năm mươi", "năm trăm"), "stop"),
                ok(issues("Taro", "Tarō"), "stop")));
        assertEquals(3, asked.size());
        assertEquals(RunState.FINAL_NOTES, flow.outcome().state());
        assertTrue(flow.outcome().issues().stream().anyMatch(i -> i.step() == Step.RECHECK
                && i.status() == EditorialApiFlow.IssueStatus.NEEDS_REVIEW));
    }

    @Test public void issueWithUnmatchedQuoteIsNeedsReviewAndRunsOneCheckOnly() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(flow, List.of(ok(edited(EDITED), "stop"), ok(issues("câu không tồn tại", "x"), "stop")));
        assertEquals(List.of(Step.EDIT, Step.CHECK), asked);
        assertEquals(RunState.FINAL_NOTES, flow.outcome().state());
        assertEquals(FixApplierReason.NO_MATCH, flow.outcome().issues().get(0).reason());
    }

    private static final class FixApplierReason {
        static final String NO_MATCH = "NO_MATCH";
    }

    @Test public void technicalFailureOfTheEditIsRetriedOnceThenRetryRequiredKeepingTheDraft() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(flow, List.of(ok("<EDITED>nửa chừng", "length"), ok("không có thẻ", "stop")));
        assertEquals(List.of(Step.EDIT, Step.EDIT), asked);
        assertEquals(RunState.RETRY_REQUIRED, flow.outcome().state());
        assertEquals(DRAFT, flow.outcome().finalText());
        assertEquals(2, flow.outcome().calls());
    }

    @Test public void theRetryCanSucceed() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.QUICK));
        List<Step> asked = drive(flow, List.of(EditorialApiFlow.StepResponse.failure("HTTP_503"), ok(edited(EDITED), "stop")));
        assertEquals(List.of(Step.EDIT, Step.EDIT), asked);
        assertEquals(RunState.FINAL_OK, flow.outcome().state());
        assertEquals(EDITED, flow.outcome().finalText());
        assertEquals("HTTP_503", flow.outcome().steps().get(0).result());
    }

    @Test public void unseparableExplanationInsideTheTextCountsAsATechnicalFailure() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.QUICK));
        List<Step> asked = drive(flow, List.of(ok(edited("Taro mở cánh cửa đỏ.\nGhi chú: giữa chừng\nHanako nói."), "stop"),
                ok(edited(EDITED), "stop")));
        assertEquals(2, asked.size());
        assertEquals(RunState.FINAL_OK, flow.outcome().state());
    }

    @Test public void anUnreadableCheckIsRetriedOnceThenTheEditStandsWithNotes() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(flow, List.of(ok(edited(EDITED), "stop"), ok("không phải JSON", "stop"), ok("{\"verdict\":", "stop")));
        assertEquals(List.of(Step.EDIT, Step.CHECK, Step.CHECK), asked);
        assertEquals(RunState.FINAL_NOTES, flow.outcome().state());
        assertTrue(flow.outcome().checkUnavailable());
        assertEquals(EDITED, flow.outcome().finalText());
    }

    @Test public void wrongPairFromTheEditStopsAtOnce() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(flow, List.of(ok("<WRONG_PAIR>RAW: cuộc thi; DRAFT: bữa tiệc</WRONG_PAIR>", "stop")));
        assertEquals(List.of(Step.EDIT), asked);
        assertEquals(RunState.WRONG_PAIR, flow.outcome().state());
        assertEquals(DRAFT, flow.outcome().finalText());
        assertTrue(flow.outcome().wrongPairEvidence().contains("cuộc thi"));
    }

    @Test public void wrongPairFromTheCheckStopsBeforeAnyFix() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(flow, List.of(ok(edited(EDITED), "stop"),
                ok("{\"verdict\":\"WRONG_PAIR\",\"wrong_pair_evidence\":\"khác chương\",\"issues\":[]}", "stop")));
        assertEquals(2, asked.size());
        assertEquals(RunState.WRONG_PAIR, flow.outcome().state());
        assertEquals(DRAFT, flow.outcome().finalText());
    }

    @Test public void cancelKeepsTheDraftAndEndsTheRun() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        assertEquals(Step.EDIT, flow.nextRequest().step());
        flow.cancel();
        assertEquals(RunState.CANCELLED, flow.outcome().state());
        assertEquals(DRAFT, flow.outcome().finalText());
        assertNull(flow.nextRequest());
    }

    @Test public void theRequestIsStableUntilAnswered() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.QUICK));
        assertTrue(flow.nextRequest() == flow.nextRequest());
        assertFalse(flow.nextRequest().json());
    }

    @Test public void worstCaseNeverExceedsSixRequestsWhateverTheModelSays() {
        // every answer is a technical failure: E twice, then done
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        int count = 0;
        while (flow.nextRequest() != null) { flow.accept(EditorialApiFlow.StepResponse.failure("X")); count++; assertTrue(count <= 6); }
        assertEquals(2, count);
        // E ok, every check unreadable: E, C, C
        EditorialApiFlow second = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        second.nextRequest();
        second.accept(ok(edited(EDITED), "stop"));
        int more = 0;
        while (second.nextRequest() != null) { second.accept(EditorialApiFlow.StepResponse.failure("X")); more++; }
        assertEquals(2, more);
    }
}
