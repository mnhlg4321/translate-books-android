package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.QualityCore;
import com.ml.tblandroidtxt.editorial.api.V5SourceIdentity;
import com.ml.tblandroidtxt.editorial.api.V5SourcePackPreflight;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs {@link EditorialApiFlow} for one stored run: one chapter, one writer. The run row is written after every step, the
 * cost cap is checked before every request, a user cancel ends the run with the draft kept, and a restart never resends
 * anything - a run still marked RUNNING after a restart is shown as interrupted and the person chooses to run again.
 */
public final class EditorialApiRunService {
    /** USD per token for a model; unknown models use the conservative fallback so the cap still protects. */
    public interface Pricing {
        BigDecimal inputPerToken(String model);

        BigDecimal outputPerToken(String model);
    }

    public interface Listener {
        void onStep(EditorialApiRun run, EditorialApiContract.Step step, int attempt);

        void onFinished(EditorialApiRun run);
    }

    /** USD 5 / 25 per million tokens: above any model this app offers, so an unknown price can never under-reserve. */
    public static final Pricing CONSERVATIVE = new Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.000005"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.000025"); }
    };

    public static final int CHECK_MAX_OUTPUT_TOKENS = 4096;

    private static final Set<Long> ACTIVE = ConcurrentHashMap.newKeySet();

    private final EditorialApiStore store;
    private final EditorialApiProvider provider;
    private final Pricing pricing;
    private final long timeoutMillis;
    private volatile boolean cancelled;

    public EditorialApiRunService(EditorialApiStore store, EditorialApiProvider provider, Pricing pricing, long timeoutMillis) {
        this.store = store;
        this.provider = provider;
        this.pricing = pricing == null ? CONSERVATIVE : pricing;
        this.timeoutMillis = timeoutMillis;
    }

    /** A run row marked RUNNING that this process is not executing was cut off (app killed, device restarted). */
    public static boolean isInterrupted(EditorialApiRun run) {
        return run != null && run.state == EditorialApiContract.RunState.RUNNING && !ACTIVE.contains(run.id);
    }

    public static boolean isActive(long runId) { return ACTIVE.contains(runId); }

    /** Snapshots the sources into a new RUNNING run. Nothing is sent. */
    public EditorialApiRun prepare(EditorialApiCombo combo, EditorialApiSources sources, String model,
                                   EditorialApiContract.Mode mode) {
        EditInputs inputs = sources.inputs();
        if (mode == EditorialApiContract.Mode.V5_CHAT) V5SourcePackPreflight.requireValid(inputs);
        EditorialApiRun run = new EditorialApiRun();
        run.comboId = combo.id;
        run.model = model == null ? "" : model;
        run.mode = mode;
        run.state = EditorialApiContract.RunState.RUNNING;
        run.qualityCoreSha256 = QualityCore.rulesSha256();
        run.rawText = sources.raw;
        run.rawSha256 = sources.rawSha256();
        run.draftText = sources.draft;
        run.draftSha256 = sources.draftSha256();
        run.glossaryText = sources.glossaryText;
        run.glossarySha256 = sources.glossarySha256();
        run.pronounText = sources.pronounText;
        run.pronounSha256 = sources.pronounSha256();
        run.originalSourceFilesJson = mode == EditorialApiContract.Mode.V5_CHAT
                ? sourceFilesJson(sources.originalSourceFiles, sources.identity) : "[]";
        run.finalText = sources.draft;
        run.glossaryEntries = com.ml.tblandroidtxt.editorial.api.ReferenceFilter.glossary(inputs.raw(), inputs.glossary()).size();
        run.pronounRows = com.ml.tblandroidtxt.editorial.api.ReferenceFilter.pronounRows(inputs.raw(), inputs.pronounCsv()).size();
        store.insertRun(run);
        return run;
    }

    public void cancel() {
        cancelled = true;
        provider.cancel();
    }

    /** Runs to the end on the calling (background) thread and returns the stored final run. */
    public EditorialApiRun execute(long runId, BigDecimal capUsd, String targetLanguage, Listener listener) {
        EditorialApiRun run = store.getRun(runId);
        if (run == null) throw new IllegalArgumentException("run " + runId);
        if (run.finished()) return run;
        if (!ACTIVE.add(runId)) throw new IllegalStateException("run " + runId + " is already executing");
        cancelled = false;
        try {
            EditorialApiFlow flow = new EditorialApiFlow(inputsOf(run, targetLanguage), EditorialApiFlow.Config.of(run.mode));
            try {
                BigDecimal unknownReserve = BigDecimal.ZERO; // worst case of every call whose price never came back
                boolean costBoundBroken = false; // a call billed more than its own worst case: stop sending, the price model is wrong
                EditorialApiFlow.Request request;
                while ((request = flow.nextRequest()) != null) {
                    if (cancelled) { flow.cancel(); break; }
                    BigDecimal spent = unknownReserve;
                    for (EditorialApiFlow.StepRecord s : flow.stepsSoFar()) spent = spent.add(s.cost());
                    int maxOut = run.mode == EditorialApiContract.Mode.V5_CHAT ? V5ChatEditorialApiProvider.MIN_OUTPUT_TOKENS
                            : request.step() == EditorialApiContract.Step.EDIT
                            ? OpenRouterEditorialApiProvider.editMaxOutputTokens(run.draftText.length()) : CHECK_MAX_OUTPUT_TOKENS;
                    BigDecimal worst = worstCase(request, maxOut, run.model);
                    if (costBoundBroken) {
                        flow.skipStep("COST_BOUND_EXCEEDED");
                    } else if (spent.add(worst).compareTo(capUsd) > 0) {
                        flow.skipStep("COST_CAP");
                    } else {
                        if (listener != null) listener.onStep(run, request.step(), request.attempt());
                        EditorialApiFlow.StepResponse response = provider.call(request, run.model, maxOut, timeoutMillis);
                        if (!response.costKnown()) unknownReserve = unknownReserve.add(worst);
                        if (response.costKnown() && response.cost().compareTo(worst) > 0) costBoundBroken = true;
                        if (cancelled) { flow.cancel(response); break; }
                        flow.accept(response);
                    }
                    EditorialApiRunCodec.applyProgress(run, flow.stepsSoFar(), flow.editedSoFar());
                    store.updateRun(run);
                }
                EditorialApiFlow.Outcome outcome = flow.outcome();
                EditorialApiRunCodec.applyOutcome(run, outcome);
                if (outcome.state() == EditorialApiContract.RunState.RETRY_REQUIRED) {
                    run.error = lastResult(outcome);
                }
            } catch (RuntimeException unexpected) {
                // never leave a run stuck in RUNNING: the draft stays the final text and the cause is recorded
                run.state = EditorialApiContract.RunState.RETRY_REQUIRED;
                run.finalText = run.draftText;
                run.error = "INTERNAL:" + unexpected.getClass().getSimpleName();
            }
            store.updateRun(run);
            if (listener != null) listener.onFinished(run);
            return run;
        } finally {
            ACTIVE.remove(runId);
        }
    }

    BigDecimal worstCase(EditorialApiFlow.Request request, int maxOutputTokens, String model) {
        return pricing.inputPerToken(model).multiply(BigDecimal.valueOf(request.prompt().estimatedInputTokens()))
                .add(pricing.outputPerToken(model).multiply(BigDecimal.valueOf(maxOutputTokens)));
    }

    private static String lastResult(EditorialApiFlow.Outcome outcome) {
        List<EditorialApiFlow.StepRecord> steps = outcome.steps();
        return steps.isEmpty() ? "" : steps.get(steps.size() - 1).result();
    }

    static EditInputs inputsOf(EditorialApiRun run, String targetLanguage) {
        List<EditInputs.GlossaryEntry> entries = new ArrayList<>();
        for (String line : run.glossaryText.split("\n", -1)) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            entries.add(new EditInputs.GlossaryEntry(parts.length > 0 ? parts[0] : "", parts.length > 1 ? parts[1] : "",
                    parts.length > 2 ? parts[2] : "", parts.length > 3 ? parts[3] : ""));
        }
        SourceSnapshot snapshot = sourceSnapshotFromJson(run.originalSourceFilesJson);
        return new EditInputs(run.rawText, run.draftText, targetLanguage, entries, run.pronounText, snapshot.files, snapshot.identity);
    }

    /** The stored V5 attachments: every file with its role and original name, and the chain identity. */
    private static String sourceFilesJson(List<EditInputs.OriginalSourceFile> files, V5SourceIdentity identity) {
        JSONArray array = new JSONArray();
        try {
            for (EditInputs.OriginalSourceFile file : files) {
                array.put(new JSONObject().put("role", file.role()).put("name", file.name()).put("content", file.content()));
            }
            V5SourceIdentity id = identity == null ? V5SourceIdentity.NONE : identity;
            return new JSONObject().put("chainId", id.chainId()).put("series", id.series()).put("files", array).toString();
        } catch (org.json.JSONException impossible) { throw new IllegalStateException("V5_SOURCE_SNAPSHOT_INVALID", impossible); }
    }

    private static final class SourceSnapshot {
        final List<EditInputs.OriginalSourceFile> files;
        final V5SourceIdentity identity;

        SourceSnapshot(List<EditInputs.OriginalSourceFile> files, V5SourceIdentity identity) {
            this.files = files;
            this.identity = identity;
        }
    }

    /**
     * Reads both stored shapes. The older shape is a bare array of {@code name}/{@code content} items written before roles and
     * identity existed; its role is recovered from the placeholder name it used and its identity is empty, so it can still be
     * shown but a V5 run on it is refused by the identity check.
     */
    private static SourceSnapshot sourceSnapshotFromJson(String json) {
        List<EditInputs.OriginalSourceFile> files = new ArrayList<>();
        V5SourceIdentity identity = V5SourceIdentity.NONE;
        try {
            String text = json == null || json.isBlank() ? "[]" : json.trim();
            JSONArray array;
            if (text.startsWith("[")) {
                array = new JSONArray(text);
            } else {
                JSONObject object = new JSONObject(text);
                identity = new V5SourceIdentity(object.optString("chainId", ""), object.optString("series", ""));
                array = object.optJSONArray("files");
                if (array == null) array = new JSONArray();
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String name = item.optString("name", "");
                String role = item.optString("role", "");
                if (role.isEmpty()) role = legacyRole(name);
                files.add(new EditInputs.OriginalSourceFile(role, name, item.optString("content", "")));
            }
        } catch (org.json.JSONException invalid) { throw new IllegalStateException("V5_SOURCE_SNAPSHOT_INVALID", invalid); }
        return new SourceSnapshot(List.copyOf(files), identity);
    }

    private static String legacyRole(String name) {
        switch (name) {
            case "RAW.txt": return "RAW";
            case "DRAFT.txt": return "DRAFT";
            case "GLOSSARY.csv": return "GLOSSARY";
            case "PRONOUN.csv": return "PRONOUN";
            default: return "";
        }
    }
}
