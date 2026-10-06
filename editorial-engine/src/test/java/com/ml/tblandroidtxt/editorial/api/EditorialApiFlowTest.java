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

    @Test public void incompleteCheckCannotBecomeACleanFinalOrTriggerMoreCalls() {
        for (String answer : List.of("{}", "{\"verdict\":\"ISSUES\",\"issues\":[]}",
                "{\"verdict\":\"PASS\",\"issues\":[{\"edited_quote\":\"\",\"kind\":\"OMISSION\"}]}")) {
            for (boolean recheck : List.of(false, true)) {
                EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
                List<EditorialApiFlow.StepResponse> answers = new ArrayList<>();
                answers.add(ok(edited(DRAFT), "stop"));
                if (recheck) answers.add(ok(issues("năm mươi", "năm trăm"), "stop"));
                answers.add(ok(answer, "stop"));
                drive(flow, answers);
                assertEquals(answer, RunState.FINAL_NOTES, flow.outcome().state());
                assertTrue(flow.outcome().checkUnavailable());
                assertEquals(recheck ? 3 : 2, flow.outcome().calls());
            }
        }
    }

    @Test public void checkAndRecheckSeeTheSameFilteredReferenceAsTheEditButNeverItsNotes() {
        EditInputs withReference = new EditInputs(RAW, DRAFT, "Vietnamese",
                List.of(new EditInputs.GlossaryEntry("太郎", "Taro", "name", ""), new EditInputs.GlossaryEntry("無関係", "KHONGDUNG", "", "")),
                "太郎,太郎,花子,tôi,cô,all,\n次郎,次郎,三郎,KHONGDUNGXH,b,c,d\n");
        EditorialApiFlow flow = new EditorialApiFlow(withReference, EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<EditorialApiFlow.Request> requests = new ArrayList<>();
        List<String> script = List.of("<EDITED>" + DRAFT + "</EDITED><NOTES>năm mươi | NUMBER | NOTEMARK</NOTES>",
                issues("năm mươi", "năm trăm"), PASS);
        EditorialApiFlow.Request request;
        int next = 0;
        while ((request = flow.nextRequest()) != null) {
            requests.add(request);
            flow.accept(ok(script.get(next++), "stop"));
        }
        assertEquals(List.of(Step.EDIT, Step.CHECK, Step.RECHECK), requests.stream().map(EditorialApiFlow.Request::step).toList());
        ApiPrompt edit = requests.get(0).prompt();
        assertEquals(1, edit.glossaryEntries());
        assertEquals(1, edit.pronounRows());
        for (int i = 1; i <= 2; i++) {
            ApiPrompt check = requests.get(i).prompt();
            assertEquals(requests.get(i).step().name(), 1, check.glossaryEntries());
            assertEquals(1, check.pronounRows());
            assertTrue(check.system().contains("# GLOSSARY"));
            assertTrue(check.system().contains("太郎 | Taro | name"));
            assertTrue(check.system().contains("# PRONOUNS"));
            assertTrue(check.system().contains("太郎,太郎,花子,tôi,cô,all"));
            assertFalse(check.system().contains("KHONGDUNG"));
            assertFalse(check.user().contains("KHONGDUNG"));
            assertFalse(check.system().contains("NOTEMARK") || check.user().contains("NOTEMARK"));
        }
    }

    @Test public void anEditWhoseOutcomeIsUnknownIsNeverSentAgainOnItsOwn() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(flow, List.of(EditorialApiFlow.StepResponse.unknownOutcome("SocketTimeoutException")));
        assertEquals(List.of(Step.EDIT), asked);
        assertEquals(RunState.RETRY_REQUIRED, flow.outcome().state());
        assertEquals(DRAFT, flow.outcome().finalText());
        assertEquals(1, flow.outcome().calls());
        assertFalse(flow.outcome().costKnown());
    }

    @Test public void aDefiniteFailureBeforeDispatchIsStillRetriedOnceAndCostsNothing() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.QUICK));
        List<Step> asked = drive(flow, List.of(EditorialApiFlow.StepResponse.failure("HTTP_429"), ok(edited(EDITED), "stop")));
        assertEquals(List.of(Step.EDIT, Step.EDIT), asked);
        assertEquals(RunState.FINAL_OK, flow.outcome().state());
        assertTrue(flow.outcome().costKnown());
    }

    @Test public void aCheckOrRecheckWhoseOutcomeIsUnknownKeepsTheEditAndStops() {
        EditorialApiFlow check = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(check, List.of(ok(edited(EDITED), "stop"), EditorialApiFlow.StepResponse.unknownOutcome("HTTP_503")));
        assertEquals(List.of(Step.EDIT, Step.CHECK), asked);
        assertEquals(RunState.FINAL_NOTES, check.outcome().state());
        assertTrue(check.outcome().checkUnavailable());
        assertEquals(EDITED, check.outcome().finalText());
        assertFalse(check.outcome().costKnown());

        EditorialApiFlow recheck = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        asked = drive(recheck, List.of(ok(edited(DRAFT), "stop"), ok(issues("năm mươi", "năm trăm"), "stop"),
                EditorialApiFlow.StepResponse.unknownOutcome("SocketTimeoutException")));
        assertEquals(List.of(Step.EDIT, Step.CHECK, Step.RECHECK), asked);
        assertEquals(RunState.FINAL_NOTES, recheck.outcome().state());
        assertTrue(recheck.outcome().checkUnavailable());
        assertTrue(recheck.outcome().finalText().contains("năm trăm"));
    }

    @Test public void aSuccessfulAnswerWithoutAPriceContinuesButTheRunIsMarkedAsCostUnknown() {
        EditorialApiFlow.StepResponse noPrice = new EditorialApiFlow.StepResponse(edited(EDITED), "stop", 100, 50, BigDecimal.ZERO, false, "m", "r", "");
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        List<Step> asked = drive(flow, List.of(noPrice, ok(PASS, "stop")));
        assertEquals(List.of(Step.EDIT, Step.CHECK), asked);
        assertEquals(RunState.FINAL_OK, flow.outcome().state());
        assertFalse(flow.outcome().costKnown());
    }

    @Test public void checkWithoutReferenceCarriesNoReferenceSectionAndZeroCounts() {
        EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        flow.nextRequest();
        flow.accept(ok(edited(DRAFT), "stop"));
        ApiPrompt check = flow.nextRequest().prompt();
        assertEquals(0, check.glossaryEntries());
        assertEquals(0, check.pronounRows());
        assertFalse(check.system().contains("AUTHORITATIVE REFERENCE"));
        assertFalse(check.system().contains("# GLOSSARY"));
    }

    @Test public void aReferenceThatMatchesNothingInRawIsNotShownToTheCheck() {
        EditInputs unmatched = new EditInputs(RAW, DRAFT, "Vietnamese", List.of(new EditInputs.GlossaryEntry("無関係", "x", "", "")), "");
        EditorialApiFlow flow = new EditorialApiFlow(unmatched, EditorialApiFlow.Config.of(Mode.THOROUGH));
        flow.nextRequest();
        flow.accept(ok(edited(DRAFT), "stop"));
        ApiPrompt check = flow.nextRequest().prompt();
        assertEquals(0, check.glossaryEntries());
        assertFalse(check.system().contains("AUTHORITATIVE REFERENCE"));
    }

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

    @Test public void skippedStepsEndTheRunWithoutCountingACall() {
        EditorialApiFlow edit = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        assertEquals(Step.EDIT, edit.nextRequest().step());
        edit.skipStep("COST_CAP");
        assertEquals(RunState.RETRY_REQUIRED, edit.outcome().state());
        assertEquals(DRAFT, edit.outcome().finalText());
        assertEquals(0, edit.outcome().inputTokens());
        EditorialApiFlow check = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(Mode.THOROUGH));
        check.nextRequest();
        check.accept(ok(edited(EDITED), "stop"));
        assertEquals(Step.CHECK, check.nextRequest().step());
        check.skipStep("COST_CAP");
        assertEquals(RunState.FINAL_NOTES, check.outcome().state());
        assertTrue(check.outcome().checkUnavailable());
        assertEquals(EDITED, check.outcome().finalText());
        assertEquals("COST_CAP", check.outcome().steps().get(1).result());
    }
}
