package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairReservation;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.ApiPrompt;
import com.ml.tblandroidtxt.editorial.api.ChunkMerge;
import com.ml.tblandroidtxt.editorial.api.EditResponseParser;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.PairContract;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairPromptBuilder;
import com.ml.tblandroidtxt.editorial.api.PairStates;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import com.ml.tblandroidtxt.editorial.api.PairText;
import com.ml.tblandroidtxt.editorial.api.StructuralGate;
import com.ml.tblandroidtxt.editorial.api.fix.FixPoint;
import com.ml.tblandroidtxt.editorial.api.fix.FixPointFinder;
import com.ml.tblandroidtxt.editorial.api.fix.TargetedFixApplier;
import com.ml.tblandroidtxt.editorial.api.fix.TargetedFixParser;
import com.ml.tblandroidtxt.editorial.api.fix.TargetedFixPrompt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the chunk-pair edit on a frozen snapshot: at most one request per flagged pair (arm C) or one for the whole chapter (arm W), each one
 * reserved in the ledger before it is dispatched and journalled before it is sent. Nothing is sent twice: a pair that may have
 * reached the provider becomes UNKNOWN and stays so, a pair already received is never asked again, and a restart reads the
 * journal instead of guessing. The model only returns text; hashes, ids, counts, gates and receipts are the app's.
 */
public final class EditorialPairRunService {
    public static final String WHOLE_PAIR_ID = "WHOLE";

    public interface Listener {
        void onPair(PairRun run, PairItem item);

        void onFinished(PairRun run);
    }

    /** What the person can export now, and how it must be labelled. */
    public static final class ExportPlan {
        public final String text;
        public final String label;
        public final boolean complete;
        public final String fileSuffix;

        ExportPlan(String text, String label, boolean complete, String fileSuffix) {
            this.text = text;
            this.label = label;
            this.complete = complete;
            this.fileSuffix = fileSuffix;
        }
    }

    public static final class PrepareException extends Exception {
        public final List<String> blockers;

        PrepareException(List<String> blockers) {
            super("PAIR_RUN_NOT_RUNNABLE " + blockers);
            this.blockers = List.copyOf(blockers);
        }
    }

    private static final Set<Long> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final int MAX_ATTEMPTS = 2;

    private final EditorialPairRunStore store;
    private final EditorialApiProvider provider;
    private final EditorialApiRunService.Pricing pricing;
    private final long timeoutMillis;
    private volatile boolean cancelled;

    public EditorialPairRunService(EditorialPairRunStore store, EditorialApiProvider provider, EditorialApiRunService.Pricing pricing, long timeoutMillis) {
        this.store = store;
        this.provider = provider;
        this.pricing = pricing == null ? EditorialApiRunService.CONSERVATIVE : pricing;
        this.timeoutMillis = timeoutMillis;
    }

    public static boolean isActive(long runId) { return ACTIVE.contains(runId); }

    public void cancel() {
        cancelled = true;
        provider.cancel();
    }

    // ---- prepare

    /** Freezes the source and the references into a run. Refuses (with typed blockers) anything that cannot run. */
    public PairRun prepare(long comboId, EditorialPairSource source, String glossaryText, String pronounText, String cueFields, String model,
                           String targetLanguage, BigDecimal capUsd, String arm) throws PrepareException {
        Set<String> cues = EditorialPairSnapshot.cues(cueFields);
        EditorialPairPreview preview = EditorialPairPreview.of(source, EditorialPairSnapshot.glossaryFrom(glossaryText), pronounText, cues);
        if (!preview.runnable()) throw new PrepareException(preview.blockers);
        PairRun run = new PairRun();
        run.comboId = comboId;
        run.arm = arm;
        run.model = model == null ? "" : model;
        run.targetLanguage = targetLanguage == null || targetLanguage.isBlank() ? "Vietnamese" : targetLanguage;
        run.sourceKind = source.kind;
        run.sourceRef = source.ref;
        run.sourceLabel = source.label;
        run.chapterId = source.chapterId;
        run.rawRowsJson = EditorialPairSnapshot.rowsJson(source.rawRows);
        run.draftRowsJson = EditorialPairSnapshot.rowsJson(source.draftRows);
        run.chunkPlanJson = source.plan == null ? "" : source.plan.toJson();
        run.glossaryText = glossaryText == null ? "" : glossaryText;
        run.glossarySha256 = run.glossaryText.isEmpty() ? "" : HashUtil.sha256(run.glossaryText);
        run.pronounText = pronounText == null ? "" : pronounText;
        run.pronounSha256 = PairText.isBlank(run.pronounText) ? "" : HashUtil.sha256(run.pronounText);
        run.cueFields = cueFields == null || cueFields.isEmpty() ? "from,speaker,target" : cueFields;
        run.mapRevision = preview.map.mapRevision;
        run.mapHash = preview.map.mapHash();
        run.capUsd = capUsd;
        List<PairItem> items = new ArrayList<>();
        if (EditorialPairModels.ARM_WHOLE.equals(arm)) {
            items.add(newItem(WHOLE_PAIR_ID, 1));
        } else {
            for (PairMap.Entry e : preview.map.entries()) items.add(newItem(e.pairId(), e.displayOrdinal()));
        }
        run.state = PairStates.RunState.PREPARED;
        store.insertRun(run, items);
        return store.getRun(run.id);
    }

    private static PairItem newItem(String pairId, int ordinal) {
        PairItem i = new PairItem();
        i.pairId = pairId;
        i.ordinal = ordinal;
        i.state = PairState.IMPORTED;
        return i;
    }

    // ---- recovery

    /** True while some pair is in a state that only a restart-time decision can settle. */
    public boolean needsRecovery(long runId) {
        if (isActive(runId)) return false;
        Map<String, Boolean> open = openPairs(runId);
        for (PairItem i : store.items(runId)) {
            if (PairStates.recover(i.state, open.containsKey(i.pairId)) != PairStates.Recovery.NONE) return true;
        }
        return false;
    }

    private Map<String, Boolean> openPairs(long runId) {
        Map<String, Boolean> open = new HashMap<>();
        for (PairReservation r : store.openReservations(runId)) open.put(r.pairId, Boolean.TRUE);
        return open;
    }

    /** Reads the journal state of every pair after the process died and acts only as {@link PairStates#recover} says. Never sends. */
    public void recover(long runId) {
        if (isActive(runId)) return;
        recoverNow(runId);
    }

    private void recoverNow(long runId) {
        Map<String, Boolean> open = openPairs(runId);
        for (PairItem i : store.items(runId)) {
            PairStates.Recovery action = PairStates.recover(i.state, open.containsKey(i.pairId));
            if (action == PairStates.Recovery.SETTLE_ZERO_NOT_DISPATCHED) {
                store.revertToImported(runId, i.pairId, "NOT_DISPATCHED");
            } else if (action == PairStates.Recovery.MARK_UNKNOWN) {
                store.commitUnknown(runId, i.pairId, i.state == PairState.E_SENT ? "PROCESS_DIED_AFTER_SEND" : "RESERVATION_WITHOUT_RESULT");
            }
        }
        finish(runId);
    }

    // ---- execute

    public PairRun execute(long runId, Listener listener) {
        return run(runId, listener, null, MAX_ATTEMPTS);
    }

    /**
     * Single-chunk experiment: sends exactly one request for {@code pairId} (no retry after a failure) and leaves every other pair
     * unsent. The frozen snapshot is the whole chapter, so the request is the one the full run would build for that pair. A pair
     * that is not an unsent IMPORTED pair is refused before anything is reserved or sent. The product flow never calls this.
     */
    public PairRun executeOnly(long runId, String pairId, Listener listener) {
        if (store.getRun(runId) == null) throw new IllegalArgumentException("run " + runId);
        PairItem target = null;
        for (PairItem i : store.items(runId)) if (i.pairId.equals(pairId)) target = i;
        if (target == null) throw new IllegalArgumentException("PAIR_NOT_IN_RUN: " + pairId);
        if (target.state != PairState.IMPORTED) throw new IllegalStateException("PAIR_NOT_UNSENT: " + pairId + " is " + target.state);
        return run(runId, listener, pairId, 1);
    }

    private PairRun run(long runId, Listener listener, String only, int maxAttempts) {
        PairRun run = store.getRun(runId);
        if (run == null) throw new IllegalArgumentException("run " + runId);
        // Never resume an old, partially sent run using a different prompt contract.
        // Historical candidates remain readable/exportable through exportPlan.
        if (!PairContract.REVISION.equals(run.contractRevision)) {
            throw new IllegalStateException("PAIR_CONTRACT_CHANGED: tạo lượt chạy mới; kết quả cũ vẫn được giữ");
        }
        if (!ACTIVE.add(runId)) throw new IllegalStateException("run " + runId + " is already executing");
        cancelled = false;
        try {
            recoverNow(runId);
            run = store.getRun(runId);
            run.started = true;
            run.paused = false;
            store.updateRun(run);
            EditorialPairSnapshot snapshot = EditorialPairSnapshot.of(run);
            for (PairItem item : store.items(runId)) {
                if (cancelled) {
                    PairRun paused = store.getRun(runId);
                    paused.paused = true;
                    store.updateRun(paused);
                    break;
                }
                if (item.state != PairState.IMPORTED) continue;
                if (only != null && !only.equals(item.pairId)) continue;
                boolean stop = dispatch(run, snapshot, item, listener, maxAttempts);
                run = refresh(runId);
                if (stop) break;
            }
            return finish(runId);
        } finally {
            ACTIVE.remove(runId);
        }
    }

    /** @return true when the run must stop (unknown outcome, reservation refused, overrun, wrong pair). */
    private boolean dispatch(PairRun run, EditorialPairSnapshot snapshot, PairItem item, Listener listener, int maxAttempts) {
        boolean whole = WHOLE_PAIR_ID.equals(item.pairId);
        PairMap.Entry entry = whole ? null : snapshot.map.entry(item.pairId);
        String draftRange = whole ? wholeDraft(snapshot.map) : snapshot.map.draftText(entry);
        FixPointFinder.Result fixes = null;
        boolean targeted = !whole && EditorialPairModels.ARM_CHUNK.equals(run.arm);
        if (targeted) {
            fixes = FixPointFinder.find(snapshot.map, entry, snapshot.glossary, snapshot.pronounText, snapshot.cueFields, snapshot.chunkPlan);
            if (fixes.points().isEmpty()) {
                store.commitNoFixPoints(run.id, item.pairId, draftRange, noFixPointsJson(fixes));
                if (listener != null) listener.onPair(run, store.item(run.id, item.pairId));
                return false;
            }
        }
        ApiPrompt prompt = targeted ? targetedPrompt(snapshot, entry, fixes.points(), run.targetLanguage) : whole
                ? PairPromptBuilder.buildWhole(snapshot.map, run.targetLanguage, snapshot.glossary, snapshot.pronounText, snapshot.cueFields)
                : PairPromptBuilder.buildPair(snapshot.map, entry, run.targetLanguage, snapshot.glossary, snapshot.pronounText, snapshot.cueFields, snapshot.context);
        int maxOut = targeted ? TargetedFixPrompt.maxOutputTokens(fixes.points()) : OpenRouterEditorialApiProvider.editMaxOutputTokens(draftRange.length());
        BigDecimal worst = pricing.inputPerToken(run.model).multiply(BigDecimal.valueOf(prompt.estimatedInputTokens()))
                .add(pricing.outputPerToken(run.model).multiply(BigDecimal.valueOf(maxOut)));
        int calls = 0;
        long inTokens = 0;
        long outTokens = 0;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String requestId = PairText.sha256(run.id + "|" + item.pairId + "|" + run.contractRevision + "|" + prompt.qualityCoreSha256() + "|g" + item.generation + "|" + attempt).substring(0, 32);
            PairReservation reservation = new PairReservation();
            reservation.callId = requestId;
            reservation.runId = run.id;
            reservation.pairId = item.pairId;
            reservation.worstUsd = worst;
            // 1-2: reserve in the ledger; a refusal means nothing is sent
            if (!store.reserve(reservation, run.capUsd)) {
                if (attempt == 1) {
                    store.setItemState(run.id, item.pairId, PairState.IMPORTED, PairState.RESERVE_FAILED, 0, requestId, "RESERVE_FAILED");
                } else {
                    failNotBilled(run, item, calls, inTokens, outTokens, "RESERVE_FAILED_ON_RETRY", requestId);
                }
                return true;
            }
            // 3-4: journal reserved, then sent, right before the call
            if (attempt == 1) {
                store.setItemState(run.id, item.pairId, PairState.IMPORTED, PairState.E_RESERVED, attempt, requestId, "RESERVED");
                store.setItemState(run.id, item.pairId, PairState.E_RESERVED, PairState.E_SENT, attempt, requestId, "SENT");
            } else {
                store.bumpAttempt(run.id, item.pairId, attempt, requestId);
            }
            if (listener != null) listener.onPair(run, item);
            EditorialApiFlow.StepResponse response = provider.call(new EditorialApiFlow.Request(EditorialApiContract.Step.EDIT, attempt, prompt, false),
                    run.model, maxOut, timeoutMillis);
            calls++;
            inTokens += response.inputTokens();
            outTokens += response.outputTokens();
            if (response.outcomeUnknown()) {
                store.commitUnknown(run.id, item.pairId, response.error());
                return true;
            }
            if (!response.error().isEmpty()) {
                // proven not billed (pre-dispatch failure): close the reservation at zero, one more attempt, then give up on the pair
                store.settleZero(requestId, "FAILED_BEFORE_DISPATCH");
                if (attempt < maxAttempts) continue;
                failNotBilled(run, item, calls, inTokens, outTokens, response.error(), requestId);
                return true;
            }
            return targeted
                    ? receiveTargeted(run, item, draftRange, fixes.points(), snapshot.glossary, response, requestId, worst, calls, inTokens, outTokens, attempt)
                    : receive(run, item, prompt, draftRange, response, requestId, worst, calls, inTokens, outTokens, attempt);
        }
        return false;
    }

    private static ApiPrompt targetedPrompt(EditorialPairSnapshot snapshot, PairMap.Entry entry, List<FixPoint> points, String language) {
        int[] r = snapshot.map.rawRange(entry);
        int[] d = snapshot.map.draftRange(entry);
        return TargetedFixPrompt.build(language,
                snapshot.map.raw.contextBeforeLines(r[0], snapshot.context.chars(), snapshot.context.minLines()), snapshot.map.rawText(entry),
                snapshot.map.raw.contextAfterLines(r[1], snapshot.context.chars(), snapshot.context.minLines()),
                snapshot.map.draft.contextBeforeLines(d[0], snapshot.context.chars(), snapshot.context.minLines()), snapshot.map.draftText(entry),
                snapshot.map.draft.contextAfterLines(d[1], snapshot.context.chars(), snapshot.context.minLines()), points);
    }

    private boolean receiveTargeted(PairRun run, PairItem item, String draftRange, List<FixPoint> points,
                                    List<com.ml.tblandroidtxt.editorial.api.EditInputs.GlossaryEntry> glossary,
                                    EditorialApiFlow.StepResponse response, String requestId, BigDecimal worst,
                                    int calls, long inTokens, long outTokens, int attempt) {
        TargetedFixParser.Parsed parsed = TargetedFixParser.parse(response.content(), points);
        TargetedFixApplier.Result applied = TargetedFixApplier.apply(draftRange, points, parsed, glossary);
        boolean complete = applied.outcomes().size() == points.size();
        boolean missing = false;
        boolean rejected = false;
        List<String> invalid = new ArrayList<>();
        for (TargetedFixApplier.Outcome outcome : applied.outcomes()) {
            if (outcome.status() == TargetedFixApplier.Status.NO_ANSWER) missing = true;
            if (outcome.status() == TargetedFixApplier.Status.REJECTED) {
                rejected = true;
                invalid.add("id=" + outcome.point().id() + ":" + outcome.reason());
            }
        }
        EditResponseParser.Status parseStatus = complete && !missing && !rejected
                ? EditResponseParser.Status.OK : EditResponseParser.Status.FORMAT;
        StructuralGate.Result gate = StructuralGate.check(new StructuralGate.Input(draftRange, applied.text(), parseStatus,
                response.finishReason(), requestId, requestId, 0));
        if (missing) gate = withCode(gate, new StructuralGate.Code("TARGETED", "TARGETED_ANSWER_MISSING", StructuralGate.Severity.BLOCK,
                "one or more expected point ids were not returned"));
        if (rejected) gate = withCode(gate, new StructuralGate.Code("TARGETED", "TARGETED_LINE_REJECTED", StructuralGate.Severity.BLOCK,
                String.join(",", invalid)));
        if (parsed.ignoredLines() > 0) gate = withCode(gate, new StructuralGate.Code("TARGETED", "TARGETED_IGNORED_LINES", StructuralGate.Severity.WARN,
                parsed.ignoredLines() + " unrecognized response lines"));

        PairItem done = new PairItem();
        done.pairId = item.pairId;
        done.state = PairStates.afterReceive(gate.status());
        done.attempt = attempt;
        done.requestId = requestId;
        done.candidateText = applied.text();
        done.responseText = response.content();
        done.responseHash = PairText.sha256(response.content());
        done.gateJson = gateJson(gate, applied.outcomes(), parsed.ignoredLines());
        done.calls = calls;
        done.inputTokens = inTokens;
        done.outputTokens = outTokens;
        done.error = rejected ? "TARGETED_LINE_REJECTED" : missing ? "TARGETED_ANSWER_MISSING" : "";
        BigDecimal settled;
        String reason;
        boolean overrun = false;
        if (response.costKnown()) {
            done.usd = response.cost();
            done.costKnown = true;
            settled = response.cost();
            reason = "ACTUAL";
            overrun = response.cost().compareTo(worst) > 0;
        } else {
            done.usd = BigDecimal.ZERO;
            done.costKnown = false;
            settled = worst;
            reason = "PRICE_UNKNOWN_RESERVED_AT_WORST";
        }
        store.commitReceived(run.id, done, requestId, settled, reason);
        if (overrun) {
            PairRun r = store.getRun(run.id);
            r.costOverrun = true;
            r.error = "COST_BOUND_EXCEEDED";
            store.updateRun(r);
            return true;
        }
        return done.state == PairState.STRUCTURE_BLOCKED;
    }

    private static StructuralGate.Result withCode(StructuralGate.Result gate, StructuralGate.Code extra) {
        List<StructuralGate.Code> codes = new ArrayList<>(gate.codes());
        codes.add(extra);
        StructuralGate.Status status = extra.severity() == StructuralGate.Severity.BLOCK ? StructuralGate.Status.BLOCK
                : gate.status() == StructuralGate.Status.PASS ? StructuralGate.Status.WARN : gate.status();
        return new StructuralGate.Result(status, codes, gate.candidate(), gate.draftLetters(), gate.candidateLetters(), gate.draftLines(),
                gate.candidateLines(), gate.verbatim(), gate.reflowOnly(), gate.internalLayoutFingerprint());
    }

    private static String noFixPointsJson(FixPointFinder.Result result) {
        try {
            return new JSONObject().put("status", "NO_FIX_POINTS").put("contract", PairContract.REVISION)
                    .put("ambiguousGlossarySplits", result.ambiguousGlossarySplits()).put("fixPoints", new JSONArray()).toString();
        } catch (JSONException impossible) { throw new IllegalStateException(impossible); }
    }

    private boolean receive(PairRun run, PairItem item, ApiPrompt prompt, String draftRange, EditorialApiFlow.StepResponse response, String requestId,
                            BigDecimal worst, int calls, long inTokens, long outTokens, int attempt) {
        EditResponseParser.Parsed parsed = EditResponseParser.parse(response.content(), response.finishReason(), true);
        String body = EditResponseParser.rawEditedBody(response.content(), true);
        StructuralGate.Result gate = StructuralGate.check(new StructuralGate.Input(draftRange, body, parsed.status(), response.finishReason(),
                requestId, requestId, 0));
        if (EditResponseParser.closeRepaired(response.content())) gate = StructuralGate.withRepairedClose(gate);
        PairItem done = new PairItem();
        done.pairId = item.pairId;
        done.state = PairStates.afterReceive(gate.status());
        done.attempt = attempt;
        done.requestId = requestId;
        done.candidateText = gate.candidate();
        done.responseText = response.content();
        done.responseHash = PairText.sha256(response.content());
        done.gateJson = gateJson(gate);
        done.calls = calls;
        done.inputTokens = inTokens;
        done.outputTokens = outTokens;
        done.error = parsed.status() == EditResponseParser.Status.WRONG_PAIR ? "WRONG_PAIR: " + parsed.wrongPairEvidence() : "";
        BigDecimal settled;
        String reason;
        boolean overrun = false;
        if (response.costKnown()) {
            done.usd = response.cost();
            done.costKnown = true;
            settled = response.cost();
            reason = "ACTUAL";
            overrun = response.cost().compareTo(worst) > 0;
        } else {
            // no price came back: the reservation closes at its worst case, never at zero
            done.usd = BigDecimal.ZERO;
            done.costKnown = false;
            settled = worst;
            reason = "PRICE_UNKNOWN_RESERVED_AT_WORST";
        }
        store.commitReceived(run.id, done, requestId, settled, reason);
        if (overrun) {
            PairRun r = store.getRun(run.id);
            r.costOverrun = true;
            r.error = "COST_BOUND_EXCEEDED";
            store.updateRun(r);
            return true;
        }
        // Stop the batch at the first unusable candidate; do not spend on the remaining
        // pairs before the caller has inspected this failure. No repair call is sent.
        return done.state == PairState.STRUCTURE_BLOCKED;
    }

    private void failNotBilled(PairRun run, PairItem item, int calls, long inTokens, long outTokens, String error, String requestId) {
        PairItem done = new PairItem();
        done.pairId = item.pairId;
        done.state = PairState.STRUCTURE_BLOCKED;
        done.attempt = MAX_ATTEMPTS;
        done.requestId = requestId;
        done.gateJson = "{\"status\":\"BLOCK\",\"codes\":[{\"gate\":\"ENVELOPE\",\"code\":\"PROVIDER_FAILURE\",\"severity\":\"BLOCK\",\"detail\":"
                + JSONObject.quote(error) + "}]}";
        done.calls = calls;
        done.inputTokens = inTokens;
        done.outputTokens = outTokens;
        done.error = error;
        store.commitFailedNotBilled(run.id, done);
    }

    private static String wholeDraft(PairMap map) {
        List<com.ml.tblandroidtxt.editorial.api.DocManifest.Unit> du = map.draft.units();
        return map.draft.text.substring(du.get(0).start(), du.get(du.size() - 1).end());
    }

    static String gateJson(StructuralGate.Result gate) {
        try {
            JSONObject o = new JSONObject();
            JSONArray codes = new JSONArray();
            for (StructuralGate.Code c : gate.codes()) {
                codes.put(new JSONObject().put("gate", c.gate()).put("code", c.code()).put("severity", c.severity().name()).put("detail", c.detail()));
            }
            o.put("status", gate.status().name()).put("codes", codes).put("draftLetters", gate.draftLetters()).put("candidateLetters", gate.candidateLetters())
                    .put("draftLines", gate.draftLines()).put("candidateLines", gate.candidateLines()).put("verbatim", gate.verbatim())
                    .put("reflowOnly", gate.reflowOnly()).put("layout", gate.internalLayoutFingerprint()).put("contract", PairContract.REVISION);
            return o.toString();
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String gateJson(StructuralGate.Result gate, List<TargetedFixApplier.Outcome> outcomes, int ignoredLines) {
        try {
            JSONObject root = new JSONObject(gateJson(gate));
            root.put("ignoredResponseLines", ignoredLines);
            JSONArray points = new JSONArray();
            for (TargetedFixApplier.Outcome outcome : outcomes) {
                FixPoint point = outcome.point();
                JSONArray types = new JSONArray();
                for (FixPoint.Type type : FixPoint.Type.values()) if (point.types().contains(type)) types.put(type.name());
                points.put(new JSONObject().put("id", point.id()).put("types", types).put("status", outcome.status().name())
                        .put("reason", outcome.reason()).put("draftLineIndex", point.draftLineIndex())
                        .put("raw", point.rawText()).put("before", outcome.before()).put("after", outcome.after())
                        .put("rules", new JSONArray(point.rules())));
            }
            root.put("fixPoints", points);
            return root.toString();
        } catch (JSONException impossible) { throw new IllegalStateException(impossible); }
    }

    // ---- run totals, final state, merge

    private PairRun refresh(long runId) {
        PairRun run = store.getRun(runId);
        List<PairItem> items = store.items(runId);
        int calls = 0;
        long in = 0;
        long out = 0;
        BigDecimal usd = BigDecimal.ZERO;
        boolean known = true;
        for (PairItem i : items) {
            calls += i.calls;
            in += i.inputTokens;
            out += i.outputTokens;
            usd = usd.add(i.usd);
            known &= i.costKnown;
        }
        run.calls = calls;
        run.inputTokens = in;
        run.outputTokens = out;
        run.usd = usd;
        run.costKnown = known;
        List<PairState> states = new ArrayList<>();
        for (PairItem i : items) states.add(i.state);
        run.warnings = PairStates.warnings(states);
        run.state = PairStates.derive(states, run.started, isActive(runId), run.paused);
        store.updateRun(run);
        return run;
    }

    private PairRun finish(long runId) {
        PairRun run = refresh(runId);
        List<PairState> states = new ArrayList<>();
        for (PairItem i : store.items(runId)) states.add(i.state);
        run.state = PairStates.derive(states, run.started, false, run.paused);
        if (run.state == PairStates.RunState.FINAL_ELIGIBLE) {
            ChunkMerge.Result merged = merge(run, false);
            if (merged.ok()) {
                run.mergedText = merged.text();
                run.mergeReceiptJson = receiptJson(merged.receipt());
            } else {
                run.state = PairStates.RunState.FINAL_BLOCKED;
                run.error = "MERGE: " + merged.errors().get(0).code();
            }
        } else {
            run.mergedText = "";
            run.mergeReceiptJson = "";
        }
        store.updateRun(run);
        return store.getRun(runId);
    }

    private ChunkMerge.Result merge(PairRun run, boolean provisional) {
        EditorialPairSnapshot snapshot = EditorialPairSnapshot.of(run);
        Map<String, String> accepted = new HashMap<>();
        for (PairItem i : store.items(run.id)) if (PairStates.mergeable(i.state)) accepted.put(i.pairId, i.candidateText);
        if (EditorialPairModels.ARM_WHOLE.equals(run.arm)) return mergeWhole(snapshot.map, accepted.get(WHOLE_PAIR_ID), provisional);
        return ChunkMerge.merge(snapshot.map, accepted, provisional);
    }

    /** The whole-chapter candidate replaces everything between the first and the last unit; the edges are the app's. */
    private static ChunkMerge.Result mergeWhole(PairMap map, String candidate, boolean provisional) {
        List<com.ml.tblandroidtxt.editorial.api.DocManifest.Unit> du = map.draft.units();
        String lead = map.draft.text.substring(0, du.get(0).start());
        String trail = map.draft.text.substring(du.get(du.size() - 1).end());
        String body = candidate;
        List<String> fallback = new ArrayList<>();
        if (body == null) {
            if (!provisional) return new ChunkMerge.Result(false, "", null, List.of(new ChunkMerge.Error("MERGE_PAIR_NOT_ACCEPTED", WHOLE_PAIR_ID)));
            body = wholeDraft(map);
            fallback.add(WHOLE_PAIR_ID);
        }
        String text = lead + body + trail;
        ChunkMerge.BoundaryPlan plan = new ChunkMerge.BoundaryPlan(lead, List.of(), trail);
        ChunkMerge.Receipt receipt = new ChunkMerge.Receipt(PairContract.REVISION, map.mapRevision, map.mapHash(), map.raw.coverageHash(),
                map.draft.coverageHash(), plan.hash(), plan.edgeHash(), List.of(WHOLE_PAIR_ID), fallback, PairText.sha256(text));
        return new ChunkMerge.Result(true, text, receipt, List.of());
    }

    static String receiptJson(ChunkMerge.Receipt r) {
        try {
            return new JSONObject().put("contract", r.contractRevision()).put("mapRevision", r.mapRevision()).put("mapHash", r.mapHash())
                    .put("rawCoverageHash", r.rawCoverageHash()).put("draftCoverageHash", r.draftCoverageHash()).put("boundaryPlanHash", r.boundaryPlanHash())
                    .put("edgePlanHash", r.edgePlanHash()).put("pairs", new JSONArray(r.orderedPairIds())).put("fallbackPairs", new JSONArray(r.fallbackPairIds()))
                    .put("outputSha256", r.outputSha256()).toString();
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    // ---- decisions and export

    /** A person's decision on one warned pair, recorded with the hash of the candidate it applies to. */
    public void resolveWarning(long runId, String pairId, boolean accept) {
        PairItem item = store.item(runId, pairId);
        if (item == null || item.state != PairState.WARN_REVIEW) throw new IllegalStateException("NOT_A_WARNING");
        store.resolveWarning(runId, pairId, accept ? PairState.ACCEPTED : PairState.REJECTED, PairText.sha256(item.candidateText));
        finish(runId);
    }

    /**
     * The text the person may export now. A finished run is the merge of every pair; anything else is a provisional copy in which
     * the pairs without an accepted candidate keep their own DRAFT text, labelled as such and never as a result of the model.
     */
    public ExportPlan exportPlan(long runId) {
        PairRun run = store.getRun(runId);
        if (run == null) return null;
        List<PairItem> items = store.items(runId);
        if (run.state == PairStates.RunState.FINAL_ELIGIBLE) {
            ChunkMerge.Result merged = merge(run, false);
            if (merged.ok()) {
                String label = run.warnings > 0
                        ? "Bản ghép đủ các đoạn, có " + run.warnings + " cảnh báo cấu trúc; chưa được chấm nghĩa"
                        : "Bản ghép đủ các đoạn; chưa được chấm nghĩa";
                return new ExportPlan(merged.text(), label, true, run.warnings > 0 ? "_bien-tap-canh-bao" : "_bien-tap");
            }
        }
        ChunkMerge.Result provisional = merge(run, true);
        if (!provisional.ok()) return null;
        int accepted = 0;
        for (PairItem i : items) if (PairStates.mergeable(i.state)) accepted++;
        return new ExportPlan(provisional.text(), "Bản tạm: " + accepted + "/" + items.size() + " đoạn đã biên tập, các đoạn còn lại giữ nguyên DRAFT",
                false, "_tam");
    }

    public PairRun get(long runId) { return store.getRun(runId); }

    public List<PairItem> items(long runId) { return store.items(runId); }
}
