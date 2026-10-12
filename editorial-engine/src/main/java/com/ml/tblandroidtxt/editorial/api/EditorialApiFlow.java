package com.ml.tblandroidtxt.editorial.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract.Mode;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract.RunState;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract.Step;

/**
 * The run as a pure state machine: no I/O, no clock, no provider. The app asks {@link #nextRequest()} what to send, sends it,
 * and hands the answer to {@link #accept}. QUICK = E; THOROUGH = E, C, and C again only when the app applied fixes. A step
 * that fails technically (transport error, truncation, unreadable answer) is repeated once; nothing else is ever repeated, so
 * a run is at most three steps and six requests. A wrong pair stops at once. An irregular answer never stops the run.
 */
public final class EditorialApiFlow {
    public record Config(Mode mode, EditGuards.Config guards) {
        public static Config of(Mode mode) { return new Config(mode, EditGuards.Config.defaults()); }
    }

    public record Request(Step step, int attempt, ApiPrompt prompt, boolean json) { }

    /** The provider's answer to one request; {@code error} is non-empty when no usable answer came back. */
    public record StepResponse(String content, String finishReason, long inputTokens, long outputTokens, BigDecimal cost,
                               boolean costKnown, String model, String route, String error) {
        public StepResponse {
            content = content == null ? "" : content;
            finishReason = finishReason == null ? "" : finishReason;
            cost = cost == null ? BigDecimal.ZERO : cost;
            model = model == null ? "" : model;
            route = route == null ? "" : route;
            error = error == null ? "" : error;
        }

        public static StepResponse failure(String error) {
            return new StepResponse("", "", 0, 0, BigDecimal.ZERO, true, "", "", error == null || error.isBlank() ? "ERROR" : error);
        }

        /**
         * The request may have reached the provider and may have been charged, but no usable answer or price came back
         * (timeout or a lost connection after sending, a 5xx, an unreadable body). The charge is unknown and the step must
         * not be repeated automatically.
         */
        public static StepResponse unknownOutcome(String error) {
            return new StepResponse("", "", 0, 0, BigDecimal.ZERO, false, "", "", error == null || error.isBlank() ? "UNKNOWN_OUTCOME" : error);
        }

        /** An error with an unknown charge: whether the provider billed anything cannot be told. */
        public boolean outcomeUnknown() { return !error.isEmpty() && !costKnown; }
    }

    public record StepRecord(Step step, int attempt, String finishReason, long inputTokens, long outputTokens, BigDecimal cost,
                             boolean costKnown, String model, String route, String result) { }

    public enum IssueStatus { APPLIED, NEEDS_REVIEW }

    /** One reported problem with what the app did about it. */
    public record IssueRecord(Step step, CheckResponseParser.Issue issue, IssueStatus status, String reason) { }

    public record Outcome(RunState state, String editedText, String finalText, List<EditResponseParser.Note> notes, int notesDropped,
                          List<IssueRecord> issues, EditGuards.Report guards, String wrongPairEvidence, List<StepRecord> steps,
                          int checkCountersTotal, boolean checkUnavailable) {
        public Outcome {
            notes = List.copyOf(notes);
            issues = List.copyOf(issues);
            steps = List.copyOf(steps);
        }

        public int calls() { return steps.size(); }

        public long inputTokens() { long n = 0; for (StepRecord s : steps) n += s.inputTokens(); return n; }

        public long outputTokens() { long n = 0; for (StepRecord s : steps) n += s.outputTokens(); return n; }

        public BigDecimal cost() { BigDecimal n = BigDecimal.ZERO; for (StepRecord s : steps) n = n.add(s.cost()); return n; }

        public boolean costKnown() { for (StepRecord s : steps) if (!s.costKnown()) return false; return true; }
    }

    private final EditInputs inputs;
    private final Config config;
    private final RawAlignedNormalizer.Result draftNormalization;
    private final List<StepRecord> steps = new ArrayList<>();
    private final List<IssueRecord> issues = new ArrayList<>();
    private Step step = Step.EDIT;
    private int attempt = 1;
    private Request pending;
    private Outcome outcome;
    private String edited = "";
    private List<EditResponseParser.Note> notes = List.of();
    private int notesDropped;
    private EditGuards.Report guards;
    private int checkCounters;
    private boolean checkUnavailable;

    public EditorialApiFlow(EditInputs inputs, Config config) {
        this.inputs = inputs;
        this.config = config;
        this.draftNormalization = RawAlignedNormalizer.normalize(inputs.raw(), inputs.draft(), inputs.glossary());
    }

    private String baseDraft() { return draftNormalization.text(); }

    public boolean done() { return outcome != null; }

    /** The steps recorded so far (also while the run is still going), for progress that survives a restart. */
    public List<StepRecord> stepsSoFar() { return List.copyOf(steps); }

    public String editedSoFar() { return edited; }

    public Outcome outcome() { return outcome; }

    /** The request to send now, or {@code null} when the run is finished. Asking twice returns the same request. */
    public Request nextRequest() {
        if (outcome != null) return null;
        if (pending == null) pending = build();
        return pending;
    }

    /** Stops the run on the user's request; the draft stays the final text and nothing the model returned is kept as final. */
    public void cancel() {
        if (outcome == null) finish(RunState.CANCELLED, baseDraft(), "");
    }

    /** A cancellation after dispatch still retains the returned usage; the content is not accepted. */
    public void cancel(StepResponse response) {
        if (outcome != null || pending == null) throw new IllegalStateException("no request is waiting for an answer");
        record(pending, response, "CANCELLED");
        cancel();
    }

    /**
     * The app decided not to send the waiting request (for example the cost cap would be exceeded). No call is counted as
     * made: the step is recorded with {@code reason}, an edit that was not sent leaves the draft with RETRY_REQUIRED, and a
     * check that was not sent leaves the edit standing with the check marked unavailable.
     */
    public void skipStep(String reason) {
        if (outcome != null || pending == null) throw new IllegalStateException("no request is waiting");
        Request request = pending;
        pending = null;
        steps.add(new StepRecord(request.step(), request.attempt(), "", 0, 0, BigDecimal.ZERO, true, "", "", reason));
        if (request.step() == Step.EDIT) {
            finish(RunState.RETRY_REQUIRED, baseDraft(), "");
        } else {
            checkUnavailable = true;
            finish(RunState.FINAL_NOTES, edited, "");
        }
    }

    public void accept(StepResponse response) {
        if (outcome != null || pending == null) throw new IllegalStateException("no request is waiting for an answer");
        Request request = pending;
        pending = null;
        switch (request.step()) {
            case EDIT -> acceptEdit(response, request);
            case CHECK, RECHECK -> acceptCheck(response, request);
            default -> throw new IllegalStateException();
        }
    }

    private Request build() {
        return switch (step) {
            case EDIT -> new Request(Step.EDIT, attempt, EditPromptBuilder.build(inputs, draftNormalization), false);
            case CHECK, RECHECK -> new Request(step, attempt, CheckPromptBuilder.build(step, inputs.raw(), baseDraft(), edited,
                    guards == null ? List.of() : guards.flags(), inputs.targetLanguage(),
                    ReferenceFilter.glossary(inputs.raw(), inputs.glossary()), ReferenceFilter.pronounRows(inputs.raw(), inputs.pronounCsv())), true);
        };
    }

    private void acceptEdit(StepResponse response, Request request) {
        EditResponseParser.Parsed parsed = response.error().isEmpty()
                ? EditResponseParser.parse(response.content(), response.finishReason()) : null;
        if (parsed != null && parsed.status() == EditResponseParser.Status.WRONG_PAIR) {
            record(request, response, "WRONG_PAIR");
            wrongPair(parsed.wrongPairEvidence());
            return;
        }
        EditGuards.Report report = null;
        String technical = response.error();
        if (parsed != null) {
            if (parsed.status() == EditResponseParser.Status.OK) {
                report = EditGuards.check(inputs.raw(), baseDraft(), parsed.edited(), inputs.glossary(), config.guards());
                if (!report.separable()) technical = "META_UNSEPARABLE";
                else technical = sizeBlock(baseDraft(), report.cleaned(), response.finishReason());
            } else {
                technical = parsed.status().name();
            }
        }
        if (!technical.isEmpty()) {
            record(request, response, technical);
            // a request whose outcome is unknown may already be charged: never send it again on our own
            // V5_CHAT has already dispatched its own three-turn chain.  A missing
            // FINAL or truncated turn must never resend that chain implicitly.
            if (config.mode() != Mode.V5_CHAT && !response.outcomeUnknown()
                    && attempt <= EditorialApiContract.MAX_TECHNICAL_RETRIES_PER_STEP) { attempt++; return; }
            finish(RunState.RETRY_REQUIRED, baseDraft(), "");
            return;
        }
        record(request, response, "OK");
        edited = report.cleaned();
        guards = report;
        notes = parsed.notes();
        notesDropped = parsed.notesDropped();
        if (config.mode() == Mode.QUICK || config.mode() == Mode.V5_CHAT) {
            boolean clean = report.flags().isEmpty() && notes.isEmpty();
            finish(clean ? RunState.FINAL_OK : RunState.FINAL_NOTES, edited, "");
            return;
        }
        step = Step.CHECK;
        attempt = 1;
    }

    /**
     * N5 B/fx-a04: a well-formed EDITED with finish {@code stop} held 37 of 192 lines and became the final text. The
     * letter thresholds of the pair gate (lost or doubled text) make such an answer a technical failure, never a final.
     */
    static String sizeBlock(String draft, String edited, String finishReason) {
        StructuralGate.Result size = StructuralGate.check(new StructuralGate.Input(draft, edited, EditResponseParser.Status.OK,
                finishReason, null, null, 0));
        for (String code : List.of("CANDIDATE_EMPTY", "CHARS_LOSS", "CHARS_GROWTH")) if (size.has(code)) return code;
        return "";
    }

    private void acceptCheck(StepResponse response, Request request) {
        CheckResponseParser.Parsed parsed = response.error().isEmpty() ? CheckResponseParser.parse(response.content()) : null;
        boolean truncated = "length".equalsIgnoreCase(response.finishReason());
        if (parsed == null || parsed.status() != CheckResponseParser.Status.OK || truncated) {
            record(request, response, parsed == null ? response.error() : truncated ? "TRUNCATED" : "UNREADABLE");
            if (!response.outcomeUnknown() && attempt <= EditorialApiContract.MAX_TECHNICAL_RETRIES_PER_STEP) { attempt++; return; }
            // the edit is intact; only the independent check is missing, which the person is told
            checkUnavailable = true;
            finish(RunState.FINAL_NOTES, edited, "");
            return;
        }
        record(request, response, parsed.verdict());
        checkCounters += parsed.counters().total();
        // Tolerating malformed bookkeeping must not turn a lost finding or absent verdict into a clean check.
        // Keep the candidate and surface a review flag; do not add a retry or invent missing semantic content.
        if (parsed.counters().issuesDropped() > 0 || (parsed.issues().isEmpty()
                && (parsed.counters().missingKeys() > 0 || parsed.counters().unknownKinds() > 0
                || !"PASS".equals(parsed.verdict())))) {
            checkUnavailable = true;
        }
        if ("WRONG_PAIR".equals(parsed.verdict())) {
            wrongPair(parsed.wrongPairEvidence());
            return;
        }
        if (request.step() == Step.RECHECK) {
            for (CheckResponseParser.Issue issue : parsed.issues()) {
                issues.add(new IssueRecord(Step.RECHECK, issue, IssueStatus.NEEDS_REVIEW, "REPORTED_AFTER_FIXES"));
            }
            finish(checkUnavailable || issues.stream().anyMatch(i -> i.status() == IssueStatus.NEEDS_REVIEW) || !notes.isEmpty()
                    ? RunState.FINAL_NOTES : RunState.FINAL_OK, edited, "");
            return;
        }
        FixApplier.Result applied = FixApplier.apply(edited, parsed.issues());
        for (FixApplier.Applied a : applied.applied()) issues.add(new IssueRecord(Step.CHECK, a.issue(), IssueStatus.APPLIED, ""));
        for (FixApplier.Review r : applied.review()) {
            issues.add(new IssueRecord(Step.CHECK, r.issue(), IssueStatus.NEEDS_REVIEW, r.reason().name()));
        }
        if (!applied.applied().isEmpty()) {
            edited = applied.text();
            step = Step.RECHECK;
            attempt = 1;
            return;
        }
        boolean review = issues.stream().anyMatch(i -> i.status() == IssueStatus.NEEDS_REVIEW);
        finish(checkUnavailable || review || !notes.isEmpty() ? RunState.FINAL_NOTES : RunState.FINAL_OK, edited, "");
    }

    private void wrongPair(String evidence) {
        finish(RunState.WRONG_PAIR, baseDraft(), evidence);
    }

    private void record(Request request, StepResponse response, String result) {
        steps.add(new StepRecord(request.step(), request.attempt(), response.finishReason(), response.inputTokens(),
                response.outputTokens(), response.cost(), response.costKnown(), response.model(), response.route(), result));
    }

    private void finish(RunState state, String finalText, String wrongPairEvidence) {
        outcome = new Outcome(state, edited, finalText, notes, notesDropped, issues, guards, wrongPairEvidence, steps,
                checkCounters, checkUnavailable);
        pending = null;
    }
}
