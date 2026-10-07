package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.SourceCheck;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Fixture runner for EDITORIAL_API_V1 (modes API_V1_QUICK, API_V1_THOROUGH and V5_CHAT). It reads the same private fixture payload
 * the P6 runner reads (RAW, DRAFT, GLOSSARY, PRONOUN pushed by scripts/p6/run_group.ps1), runs the real run service and
 * writes the same output layout (final.txt, structural.json, run-metadata.json, spend-ledger.jsonl, prompts, responses) so
 * score_run.py and verify_fixture_run.py judge it unchanged. Without p6_fixture_live=YES the provider is a local fake and
 * nothing leaves the device (the dry run N5 starts from).
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialApiV1FixtureRunnerInstrumentedTest {
    private static final List<String> SOURCE_NAMES = List.of("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv");
    /** The reservation basis P6 pinned (USD per million tokens: 0.25 in, 1.20 out). */
    private static final EditorialApiRunService.Pricing PINNED = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.00000025"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };

    @Test public void runFixture() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        Assume.assumeTrue("API_V1 fixture runner is opt-in", "YES".equalsIgnoreCase(args.getString("p6_fixture_run", ""))
                && (args.getString("p6_mode", "").startsWith("API_V1_") || "V5_CHAT".equals(args.getString("p6_mode", ""))));
        String runId = uuid(args.getString("p6_run_id", ""));
        String fixtureId = args.getString("p6_fixture_id", "");
        String modeText = args.getString("p6_mode", "");
        if (!fixtureId.matches("fx-[ah][0-9]{2}")) throw new IllegalArgumentException("P6_FIXTURE_ID_INVALID");
        EditorialApiContract.Mode mode;
        if ("API_V1_QUICK".equals(modeText)) mode = EditorialApiContract.Mode.QUICK;
        else if ("API_V1_THOROUGH".equals(modeText)) mode = EditorialApiContract.Mode.THOROUGH;
        else if ("V5_CHAT".equals(modeText)) mode = EditorialApiContract.Mode.V5_CHAT;
        else throw new IllegalArgumentException("P6_FIXTURE_MODE_INVALID");
        boolean live = "YES".equalsIgnoreCase(args.getString("p6_fixture_live", ""));
        Context context = ApplicationProvider.getApplicationContext();

        AppSettings settings = null;
        if (live) {
            settings = SettingsStore.load(context).copy();
            String preflight = EditorialP6FixtureLivePreflight.check(settings, args.getString("p6_expected_endpoint_account_fingerprint", ""));
            if (!EditorialP6FixtureLivePreflight.MATCH.equals(preflight)) throw new IllegalStateException(preflight);
        }
        Path externalRoot = context.getExternalFilesDir(null).toPath().toAbsolutePath().normalize();
        Path inputRoot = context.getFilesDir().toPath().toAbsolutePath().normalize().resolve("p6-fixtures");
        Path runInput = inputRoot.resolve(runId).normalize();
        Path fixtureRoot = runInput.resolve(fixtureId).normalize();
        Path outputRoot = externalRoot.resolve("p6-fixture-results").resolve(runId).resolve(fixtureId).normalize();
        if (!fixtureRoot.startsWith(runInput) || !runInput.startsWith(inputRoot) || !outputRoot.startsWith(externalRoot)
                || fixtureRoot.toString().contains("6.FINAL") || Files.exists(outputRoot)) {
            throw new IllegalStateException("P6_FIXTURE_PATH_REFUSED");
        }
        Map<String, Object> runtime = EditorialCanonicalJson.parseObject(Files.readAllBytes(runInput.resolve(fixtureId + ".runtime.json")));
        @SuppressWarnings("unchecked") Map<String, Object> files = (Map<String, Object>) runtime.get("files");
        if (!files.keySet().equals(Set.copyOf(SOURCE_NAMES))) throw new IllegalStateException("P6_RUNTIME_FILE_SET_INVALID");
        Map<String, String> text = new LinkedHashMap<>();
        for (String name : SOURCE_NAMES) {
            byte[] bytes = Files.readAllBytes(fixtureRoot.resolve(name).normalize());
            @SuppressWarnings("unchecked") Map<String, Object> meta = (Map<String, Object>) files.get(name);
            if (!EditorialCanonicalJson.sha256Hex(bytes).equals(meta.get("sha256"))) throw new IllegalStateException("P6_FIXTURE_SOURCE_HASH_MISMATCH");
            text.put(name, new String(bytes, StandardCharsets.UTF_8));
        }

        String groupId = args.getString("p6_group_id", live ? "" : "P6-OFFLINE-" + runId);
        if (groupId == null || !groupId.matches("[A-Za-z0-9._-]{3,100}")) throw new IllegalArgumentException("P6_SPEND_GROUP_INVALID");
        BigDecimal groupMaximum = new BigDecimal(args.getString("p6_group_maximum_usd", "1.00"));
        if (!EditorialP6GroupSpendLedger.isValidRunnerGroupCap(groupMaximum)) throw new IllegalArgumentException("P6_SPEND_GROUP_CAP_INVALID");
        BigDecimal chapterCap = new BigDecimal(args.getString("p6_chapter_cap_usd", "0.10"));
        Path ledgerPath = externalRoot.resolve("p6-spend-ledger-groups").resolve(groupId + ".jsonl").normalize();
        if (!ledgerPath.startsWith(externalRoot)) throw new IllegalArgumentException("P6_SPEND_GROUP_PATH_REFUSED");
        EditorialP6GroupSpendLedger ledger = new EditorialP6GroupSpendLedger(ledgerPath, groupId, groupMaximum);

        List<EditInputs.GlossaryEntry> entries = new ArrayList<>();
        for (GlossaryStore.Term term : GlossaryStore.parseTerms("GLOSSARY.csv", text.get("GLOSSARY.csv"))) {
            entries.add(new EditInputs.GlossaryEntry(term.source, term.target, term.category, term.note));
        }
        String targetLanguage = "Vietnamese";
        EditorialApiSources sources = new EditorialApiSources(text.get("RAW.txt"), text.get("DRAFT.txt"),
                EditorialApiSources.glossaryAsText(entries), entries, text.get("PRONOUN.csv"), targetLanguage);

        Files.createDirectories(outputRoot);
        Capture capture = new Capture(outputRoot);
        String modelOverride = args.getString("p6_model_override", "").trim();
        String model = live ? (modelOverride.isEmpty() ? settings.model : modelOverride) : "fake-model";
        String reasoning = args.getString("p6_reasoning_effort", mode == EditorialApiContract.Mode.V5_CHAT ? "medium" : "minimal");
        EditorialApiProvider delegate;
        Map<String, String> packHashes = new LinkedHashMap<>();
        if (live && mode == EditorialApiContract.Mode.V5_CHAT) {
            String project = asset(context, "editorial/v5-safe4-full-chatgpt/PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4_1_3_FULL.txt");
            String prompt = asset(context, "editorial/v5-safe4-full-chatgpt/PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4_1_3_FULL.txt");
            String workflow = asset(context, "editorial/v5-safe4-full-chatgpt/WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4_1_3_FULL.txt");
            packHashes.put("project", EditorialCanonicalJson.sha256Hex(project.getBytes(StandardCharsets.UTF_8)));
            packHashes.put("prompt", EditorialCanonicalJson.sha256Hex(prompt.getBytes(StandardCharsets.UTF_8)));
            packHashes.put("workflow", EditorialCanonicalJson.sha256Hex(workflow.getBytes(StandardCharsets.UTF_8)));
            delegate = new V5ChatEditorialApiProvider(settings, project, prompt, workflow, reasoning, capture::recordV5);
        } else if (live) {
            delegate = new OpenRouterEditorialApiProvider(settings, reasoning);
        } else {
            delegate = fake(sources.draft);
        }
        EditorialApiRunService.Pricing pricing = live && "openai/gpt-5.6-sol".equalsIgnoreCase(model) ? SOL : PINNED;
        EditorialApiLedgerProvider provider = new EditorialApiLedgerProvider(delegate, ledger, pricing, runId + "|" + fixtureId, model, capture::record);

        List<SourceCheck.Problem> problems = SourceCheck.check(sources.raw, sources.draft, sources.hasGlossary(), sources.hasPronoun(),
                sources.glossaryText.length() + sources.pronounText.length());
        InMemoryEditorialApiStore store = new InMemoryEditorialApiStore();
        EditorialApiCombo combo = new EditorialApiCombo();
        combo.name = fixtureId;
        store.insertCombo(combo);
        EditorialApiRunService service = new EditorialApiRunService(store, provider, pricing, 900_000L);
        EditorialApiRun run;
        String blocked = "";
        if (SourceCheck.blocked(problems)) {
            // refuse before anything is sent; the draft stays the final text
            run = service.prepare(combo, sources, model, mode);
            run.state = EditorialApiContract.RunState.RETRY_REQUIRED;
            run.error = "SOURCE_BLOCKED";
            blocked = problems.toString();
            store.updateRun(run);
        } else {
            run = service.prepare(combo, sources, model, mode);
            run = service.execute(run.id, chapterCap, targetLanguage, null);
        }

        boolean valid = run.state == EditorialApiContract.RunState.FINAL_OK || run.state == EditorialApiContract.RunState.FINAL_NOTES;
        byte[] finalBytes = (valid ? run.finalText : sources.draft).getBytes(StandardCharsets.UTF_8);
        Files.write(outputRoot.resolve("final.txt"), finalBytes);

        JSONObject guards = new JSONObject(run.guardsJson);
        JSONArray flagArray = guards.optJSONArray("flags");
        List<String> flagCodes = new ArrayList<>();
        if (flagArray != null) for (int i = 0; i < flagArray.length(); i++) flagCodes.add(flagArray.getJSONObject(i).optString("code"));
        String route = firstRoute(run);

        Map<String, Object> structural = new LinkedHashMap<>();
        structural.put("valid", valid);
        structural.put("measuredRefusal", false);
        structural.put("providerKind", live ? "LIVE" : "FAKE_OFFLINE");
        structural.put("reasonCode", run.state.name());
        structural.put("stage", "API_V1");
        structural.put("providerCalls", BigDecimal.valueOf(run.calls));
        structural.put("actualProviderCalls", BigDecimal.valueOf(live ? provider.physicalCalls() : 0));
        structural.put("inputTokens", BigDecimal.valueOf(run.inputTokens));
        structural.put("outputTokens", BigDecimal.valueOf(run.outputTokens));
        structural.put("usd", run.usd.toPlainString());
        List<String> stops = new ArrayList<>();
        if (!valid) {
            stops.add("P6_API_V1_" + run.state.name());
            if (!run.error.isEmpty()) stops.add(run.error);
            if (!blocked.isEmpty()) stops.add(blocked);
            if (!run.wrongPairEvidence.isEmpty()) stops.add("WRONG_PAIR");
        }
        structural.put("stops", stops);
        Files.write(outputRoot.resolve("structural.json"), EditorialCanonicalJson.canonicalize(structural).getBytes(StandardCharsets.UTF_8));

        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        Map<String, Object> apiV1 = new LinkedHashMap<>();
        apiV1.put("mode", mode.name());
        apiV1.put("state", run.state.name());
        apiV1.put("rewriteRatio", BigDecimal.valueOf(guards.optDouble("rewriteRatio", 0.0)));
        apiV1.put("guardFlags", new ArrayList<Object>(flagCodes));
        apiV1.put("calls", BigDecimal.valueOf(run.calls));
        apiV1.put("glossaryEntries", BigDecimal.valueOf(run.glossaryEntries));
        apiV1.put("pronounRows", BigDecimal.valueOf(run.pronounRows));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fixtureId", fixtureId);
        metadata.put("mode", modeText);
        metadata.put("providerKind", live ? "LIVE" : "FAKE_OFFLINE");
        metadata.put("groupId", groupId);
        metadata.put("groupMaximumUsd", groupMaximum.toPlainString());
        metadata.put("chapterCapUsd", chapterCap.toPlainString());
        metadata.put("actualProviderCalls", BigDecimal.valueOf(live ? provider.physicalCalls() : 0));
        metadata.put("fakeProviderCalls", BigDecimal.valueOf(live ? 0 : run.calls));
        metadata.put("inputTokens", BigDecimal.valueOf(run.inputTokens));
        metadata.put("outputTokens", BigDecimal.valueOf(run.outputTokens));
        metadata.put("usd", run.usd.toPlainString());
        metadata.put("knownCostCalls", BigDecimal.valueOf(run.costKnown ? run.calls : 0));
        metadata.put("unknownCostCalls", BigDecimal.valueOf(run.costKnown ? 0 : run.calls));
        metadata.put("costOverrunCalls", BigDecimal.valueOf(provider.overruns().size()));
        metadata.put("finishReasons", new ArrayList<Object>(capture.finishReasons));
        metadata.put("finalSha256", EditorialCanonicalJson.sha256Hex(finalBytes));
        metadata.put("model", run.model);
        metadata.put("route", route);
        metadata.put("contractRevision", run.contractRevision);
        metadata.put("qualityCoreSha256", run.qualityCoreSha256);
        metadata.put("reasoningEffort", reasoning);
        if (!packHashes.isEmpty()) metadata.put("v5PackSha256", new LinkedHashMap<>(packHashes));
        metadata.put("sourceCommit", args.getString("p6_source_commit", ""));
        metadata.put("apkVersionName", packageInfo.versionName == null ? "" : packageInfo.versionName);
        metadata.put("apkVersionCode", BigDecimal.valueOf(packageInfo.getLongVersionCode()));
        metadata.put("apiV1", apiV1);
        Files.write(outputRoot.resolve("run-metadata.json"), EditorialCanonicalJson.canonicalize(metadata).getBytes(StandardCharsets.UTF_8));

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("steps", new JSONArray(run.stepsJson).toString());
        detail.put("notes", new JSONArray(run.notesJson).toString());
        detail.put("issues", new JSONArray(run.issuesJson).toString());
        detail.put("guards", run.guardsJson);
        detail.put("wrongPairEvidence", run.wrongPairEvidence);
        Files.write(outputRoot.resolve("api-run.json"), EditorialCanonicalJson.canonicalize(detail).getBytes(StandardCharsets.UTF_8));
        Files.copy(ledgerPath, outputRoot.resolve("spend-ledger.jsonl"));
        EditorialP6GroupSpendLedger.Snapshot snapshot = ledger.inspect();
        if (!provider.overruns().isEmpty()) {
            // never clamped into the ledger: the exact amounts are the evidence and the group stops
            StringBuilder overrun = new StringBuilder();
            for (EditorialApiLedgerProvider.Overrun o : provider.overruns()) {
                overrun.append(o.callId()).append(" reserved=").append(o.reserved().toPlainString())
                        .append(" actual=").append(o.actual().toPlainString()).append("\n");
            }
            Files.write(outputRoot.resolve("cost-overrun.txt"), overrun.toString().getBytes(StandardCharsets.UTF_8));
        }
        assertTrue("a call was billed above its reservation (see cost-overrun.txt)", provider.overruns().isEmpty());
        assertTrue("no spend reservation may stay UNKNOWN", snapshot.pendingCalls() == 0);
    }

    /** Dry run: the edit returns the draft unchanged, the check passes; every answer costs a fixed fake amount. */
    private static EditorialApiProvider fake(String draft) {
        return new FakeEditorialApiProvider((request, index) -> request.step() == EditorialApiContract.Step.EDIT
                ? FakeEditorialApiProvider.edited(draft) : FakeEditorialApiProvider.pass());
    }

    private static final EditorialApiRunService.Pricing SOL = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.000002"); }
        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.000010"); }
    };

    private static String asset(Context context, String path) throws IOException {
        try (java.io.InputStream input = context.getAssets().open(path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String firstRoute(EditorialApiRun run) throws Exception {
        JSONArray steps = new JSONArray(run.stepsJson);
        for (int i = 0; i < steps.length(); i++) {
            String route = steps.getJSONObject(i).optString("route", "");
            if (!route.isEmpty()) return route;
        }
        return "";
    }

    private static String uuid(String value) {
        if (value == null || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new IllegalArgumentException("P6_RUN_ID_INVALID");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    /** Prompts and raw answers of every request, in order, outside Git like the P6 captures. */
    private static final class Capture {
        private final Path root;
        private int sequence;
        final List<String> finishReasons = new ArrayList<>();

        Capture(Path root) { this.root = root; }

        synchronized void record(EditorialApiFlow.Request request, EditorialApiFlow.StepResponse response) throws IOException {
            String step = request.step().name();
            Files.createDirectories(root.resolve("prompts"));
            Files.createDirectories(root.resolve("responses"));
            int number = ++sequence;
            byte[] prompt = ("SYSTEM\n" + request.prompt().system() + "\nUSER\n" + request.prompt().user()).getBytes(StandardCharsets.UTF_8);
            Files.write(root.resolve("prompts").resolve(String.format(Locale.ROOT, "%03d-%s.txt", number, step)), prompt);
            Files.write(root.resolve("responses").resolve(String.format(Locale.ROOT, "%03d-%s.json", number, step)),
                    response.content().getBytes(StandardCharsets.UTF_8));
            finishReasons.add(response.finishReason());
        }

        synchronized void recordV5(int turn, List<OpenAICompatibleClient.ChatMessage> messages,
                                   OpenAICompatibleClient.ChatResult result, String error) throws IOException {
            Path root = this.root.resolve("v5-chat");
            Files.createDirectories(root);
            StringBuilder prompt = new StringBuilder();
            for (OpenAICompatibleClient.ChatMessage message : messages) {
                prompt.append(message.role()).append("\n").append(message.content()).append("\n\n");
            }
            Files.write(root.resolve(String.format(Locale.ROOT, "%02d-request.txt", turn)), prompt.toString().getBytes(StandardCharsets.UTF_8));
            String responseText = result == null ? "" : result.content;
            Files.write(root.resolve(String.format(Locale.ROOT, "%02d-response.txt", turn)), responseText.getBytes(StandardCharsets.UTF_8));
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("turn", BigDecimal.valueOf(turn));
            meta.put("error", error == null ? "" : error);
            meta.put("finishReason", result == null ? "" : result.finishReason);
            meta.put("inputTokens", BigDecimal.valueOf(result == null ? 0 : result.promptTokens));
            meta.put("outputTokens", BigDecimal.valueOf(result == null ? 0 : result.completionTokens));
            meta.put("model", result == null ? "" : result.responseModel);
            meta.put("route", result == null ? "" : result.responseProvider);
            Files.write(root.resolve(String.format(Locale.ROOT, "%02d-metadata.json", turn)), EditorialCanonicalJson.canonicalize(meta).getBytes(StandardCharsets.UTF_8));
        }
    }

}
