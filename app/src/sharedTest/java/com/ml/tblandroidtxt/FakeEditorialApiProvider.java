package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Scripted provider for tests: never touches the network. A script is a list of answers consumed in order, or a function
 * from the request; every request is recorded. Shared by the JVM tests and the instrumented tests.
 */
public final class FakeEditorialApiProvider implements EditorialApiProvider {
    public interface Responder {
        EditorialApiFlow.StepResponse answer(EditorialApiFlow.Request request, int index);
    }

    public final List<EditorialApiFlow.Request> requests = new ArrayList<>();
    public final List<String> models = new ArrayList<>();
    public final List<Integer> maxOutputTokens = new ArrayList<>();
    private final Responder responder;
    public volatile boolean cancelled;
    public volatile Runnable beforeAnswer;

    public FakeEditorialApiProvider(Responder responder) { this.responder = responder; }

    public static FakeEditorialApiProvider scripted(EditorialApiFlow.StepResponse... answers) {
        List<EditorialApiFlow.StepResponse> list = List.of(answers);
        return new FakeEditorialApiProvider((request, index) -> index < list.size() ? list.get(index)
                : EditorialApiFlow.StepResponse.failure("SCRIPT_EXHAUSTED"));
    }

    @Override public synchronized EditorialApiFlow.StepResponse call(EditorialApiFlow.Request request, String model, int maxOutput, long timeoutMillis) {
        requests.add(request);
        models.add(model);
        maxOutputTokens.add(maxOutput);
        Runnable hook = beforeAnswer;
        if (hook != null) hook.run();
        return responder.answer(request, requests.size() - 1);
    }

    @Override public void cancel() { cancelled = true; }

    // ---- ready-made answers ----

    public static EditorialApiFlow.StepResponse text(String content, String finish) {
        return new EditorialApiFlow.StepResponse(content, finish, 1500, 400, new BigDecimal("0.004"), true, "fake-model", "fake-route", "");
    }

    public static EditorialApiFlow.StepResponse edited(String text) { return text("<EDITED>" + text + "</EDITED>", "stop"); }

    public static EditorialApiFlow.StepResponse editedWithNotes(String text, String notes) {
        return text("<EDITED>" + text + "</EDITED><NOTES>" + notes + "</NOTES>", "stop");
    }

    public static EditorialApiFlow.StepResponse truncated() { return text("<EDITED>nửa chừng", "length"); }

    public static EditorialApiFlow.StepResponse wrongPair(String evidence) { return text("<WRONG_PAIR>" + evidence + "</WRONG_PAIR>", "stop"); }

    public static EditorialApiFlow.StepResponse pass() {
        return text("{\"verdict\":\"PASS\",\"wrong_pair_evidence\":\"\",\"issues\":[]}", "stop");
    }

    public static EditorialApiFlow.StepResponse issue(String editedQuote, String fix) {
        return text("{\"verdict\":\"ISSUES\",\"wrong_pair_evidence\":\"\",\"issues\":[{\"edited_quote\":\"" + editedQuote
                + "\",\"raw_quote\":\"\",\"kind\":\"NUMBER\",\"fix\":\"" + fix + "\"}]}", "stop");
    }

    public static EditorialApiFlow.StepResponse checkWrongPair(String evidence) {
        return text("{\"verdict\":\"WRONG_PAIR\",\"wrong_pair_evidence\":\"" + evidence + "\",\"issues\":[]}", "stop");
    }

    /** A provider that answers by mode: edit returns {@code edited}; the check (and re-check) answer {@code pass()}. */
    public static FakeEditorialApiProvider editsAndPasses(String edited) {
        return new FakeEditorialApiProvider((request, index) ->
                request.step() == EditorialApiContract.Step.EDIT ? edited(edited) : pass());
    }
}
