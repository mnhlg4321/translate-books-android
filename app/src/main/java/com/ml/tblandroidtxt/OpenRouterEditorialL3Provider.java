package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialL3Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * OpenRouter adapter for the two L3 calls on the qualified fresh route. The
 * blind re-audit only ever receives what the app projection made visible
 * (RAW, GLOSSARY, VI_L2); reconcile additionally receives the app-owned
 * candidate list. Rendering and transport only; {@link EditorialL3Execution}
 * owns validity, release numbers and commit. No retry, repair or fallback.
 */
public final class OpenRouterEditorialL3Provider implements EditorialL2Execution.Provider {
    static final String CANDIDATES_ROLE = "L3_REAUDIT_CANDIDATES";

    static final String REAUDIT_RULES =
            "Re-audit rules enforced by the app (any violation rejects the whole response):\n"
            + "- You see only RAW, GLOSSARY and VI_L2. Read RAW first, build the expected model, then read the whole VI_L2.\n"
            + "- candidates lists every raw unit (ledger UNIT) and every title/glossary (TG), semantic (SR) and relation (RC) candidate.\n"
            + "- line is the VI_L2 line shown as L<n>|, or 0 when the candidate has no VI_L2 line (for example missing content).\n"
            + "- status: PROCESSED (VI_L2 renders RAW correctly), PRESERVED (kept with an evidence limit), "
            + "UNPROCESSED (not yet checked or needs a fix), CONFLICT (proven unresolved conflict).\n"
            + "- candidateId: 1-48 ASCII characters matching [A-Za-z0-9][A-Za-z0-9._:/-]*, unique.\n"
            + "- Never return chapter text, hashes or identities other than the attemptIdentity echo.\n";

    static final String RECONCILE_RULES =
            "Reconcile rules enforced by the app (any violation rejects the whole response):\n"
            + "- Now open DRAFT, REPORT_L1 and CHANGE_MAP_L2 and reconcile against the candidate list in "
            + CANDIDATES_ROLE + ". resolutions may update any candidateId from that list; unknown IDs are rejected.\n"
            + "- QA edits are whole VI_L2 lines: line is the L<n>| number, before is that exact line, after the full replacement "
            + "without line breaks; every change has its own QA errorId and changeId; dialogue changes need speakerProof "
            + "{speaker, listener, anchorBefore, anchorAfter} grounded in RAW, otherwise keep the line with a preserved row.\n"
            + "- adversarialCoverage: probes that try to refute coverage (missed raw unit, title/glossary, wrong sense, "
            + "relation/listener, speaker, unaccounted change). adversarialRegression: probes that treat every actual change "
            + "as suspect (unnecessary change, lost subject/listener/voice, honorific/attribution, symbols, numbers, line loss). "
            + "Each probe: probeId, finding, verdict NO_DEFECT|FIXED|PRESERVED|CONFLICT. Both lists must be non-empty.\n"
            + "- disposition as in L2: CONTINUE or PRESERVE_DRAFT with stopClass NONE, or STOP with CONTENT_BLOCKED/INPUT_REQUIRED; "
            + "reasonCode at most 32 characters, starts with a letter or digit, then letters, digits, spaces and . _ : / ; ( ) -.\n"
            + "- The app computes the release numbers itself; a PASS claim is not evidence.\n";

    private final AppSettings settings;

    public OpenRouterEditorialL3Provider(AppSettings settings) {
        if (settings == null) throw new IllegalArgumentException("OpenRouter settings are required");
        this.settings = settings.copy();
    }

    @Override public Response call(Request request) throws Exception {
        if (request == null || !validPhase(request)) throw new IllegalStateException("P6_L3_REQUEST_INVALID");
        if (!EditorialP5EFreshRawRoutingPolicy.matches(settings)) {
            throw new IllegalStateException("P6_L3_ROUTE_SETTINGS_MISMATCH");
        }
        if (settings.apiKey == null || settings.apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OPENROUTER_CONFIGURATION_INCOMPLETE");
        }
        OpenAICompatibleClient.ChatResult result = OpenAICompatibleClient.chatWithUsage(settings,
                buildPrompt(request), request.maximumOutputTokens(), request.attemptIdentity() + ":" + request.phase(),
                null, false, OpenAICompatibleClient.monotonicDeadlineNanosFromNowMillis(
                        request.maximumExecutionTimeMillis()),
                new OpenAICompatibleClient.CallControl(),
                new JSONObject().put("type", "json_object"),
                EditorialP5EFreshRawRoutingPolicy.providerPreferences(),
                EditorialP5RawWireContract.REASONING_POLICY);
        BigDecimal cost = BigDecimal.ZERO;
        boolean costKnown = false;
        if (result.providerCostReported && Double.isFinite(result.providerCost) && result.providerCost >= 0) {
            cost = BigDecimal.valueOf(result.providerCost);
            costKnown = true;
        } else {
            double estimate = ModelCatalog.usageCost(ModelCatalog.findModelInfo(
                    EditorialP5EFreshRawRoutingPolicy.PROVIDER, EditorialP5EFreshRawRoutingPolicy.MODEL),
                    result.promptTokens, result.cachedPromptTokens, result.completionTokens);
            if (Double.isFinite(estimate) && estimate >= 0) {
                cost = BigDecimal.valueOf(estimate);
                costKnown = true;
            }
        }
        return new Response(result.content.getBytes(StandardCharsets.UTF_8), result.finishReason,
                true, result.promptTokens, result.completionTokens, cost, costKnown);
    }

    private static boolean validPhase(Request request) {
        boolean reaudit = EditorialL3Execution.REAUDIT_PHASE.equals(request.phase())
                && EditorialL3Execution.REAUDIT_WIRE.equals(request.outputSchemaId())
                && !request.visibleSources().containsKey(CANDIDATES_ROLE);
        boolean reconcile = EditorialL3Execution.RECONCILE_PHASE.equals(request.phase())
                && EditorialL3Execution.RECONCILE_WIRE.equals(request.outputSchemaId())
                && request.visibleSources().containsKey(CANDIDATES_ROLE);
        return reaudit || reconcile;
    }

    static PromptPair buildPrompt(Request request) {
        boolean reaudit = EditorialL3Execution.REAUDIT_PHASE.equals(request.phase());
        StringBuilder system = new StringBuilder();
        system.append("You are an untrusted SAFE4 L3 QA assistant. The app is the authority.\n")
                .append("Return exactly one JSON object and no Markdown or commentary.\n")
                .append(reaudit ? REAUDIT_RULES : RECONCILE_RULES)
                .append("\n[PROJECT_INSTRUCTION]\n").append(authority(request, EditorialPackFileRole.PROJECT_INSTRUCTION))
                .append("\n[/PROJECT_INSTRUCTION]\n[TURN_PROMPT]\n").append(authority(request, EditorialPackFileRole.TURN_PROMPT))
                .append("\n[/TURN_PROMPT]\n[WORKFLOW]\n").append(authority(request, EditorialPackFileRole.WORKFLOW))
                .append("\n[/WORKFLOW]");

        Map<String, Object> envelope = new TreeMap<>();
        envelope.put("phaseActivation", request.phase());
        envelope.put("wireSchemaVersion", request.outputSchemaId());
        envelope.put("attemptIdentity", request.attemptIdentity());
        envelope.put("chapterKey", request.chapterKey());

        StringBuilder user = new StringBuilder();
        user.append("APP-OWNED REQUEST ENVELOPE (only attemptIdentity may be echoed):\n")
                .append(EditorialCanonicalJson.canonicalize(envelope))
                .append("\n\nVISIBLE SOURCE BLOCKS. Do not infer or request hidden roles:\n");
        List<String> roles = new ArrayList<>(request.visibleSources().keySet());
        Collections.sort(roles);
        for (String role : roles) {
            String text = new String(request.visibleSources().get(role), StandardCharsets.UTF_8);
            user.append("\n--- ").append(role).append(" ---\n")
                    .append("VI_L2".equals(role) ? OpenRouterEditorialL2Provider.numbered(text) : text)
                    .append("\n--- END ").append(role).append(" ---\n");
        }
        user.append("\nReturn only this object:\n");
        if (reaudit) {
            user.append("{\"wireSchemaVersion\":\"").append(EditorialL3Execution.REAUDIT_WIRE)
                    .append("\",\"attemptIdentity\":\"<exact echo>\",\"candidates\":[{\"candidateId\":\"U001\",")
                    .append("\"ledger\":\"UNIT|TG|SR|RC\",\"line\":1,\"status\":\"PROCESSED|PRESERVED|UNPROCESSED|CONFLICT\"}]}\n");
        } else {
            user.append("{\"wireSchemaVersion\":\"").append(EditorialL3Execution.RECONCILE_WIRE)
                    .append("\",\"attemptIdentity\":\"<exact echo>\",")
                    .append("\"resolutions\":[{\"candidateId\":\"U001\",\"status\":\"PROCESSED|PRESERVED|UNPROCESSED|CONFLICT\"}],")
                    .append("\"changes\":[{\"changeId\":\"QC001\",\"errorId\":\"QE001\",\"line\":1,\"before\":\"...\",")
                    .append("\"after\":\"...\",\"reason\":\"...\",\"dialogue\":false,\"status\":\"CLOSED\"}],")
                    .append("\"preserved\":[{\"preserveId\":\"QP001\",\"line\":1,\"before\":\"...\",\"evidenceLimit\":\"...\"}],")
                    .append("\"adversarialCoverage\":[{\"probeId\":\"AC001\",\"finding\":\"...\",\"verdict\":\"NO_DEFECT|FIXED|PRESERVED|CONFLICT\"}],")
                    .append("\"adversarialRegression\":[{\"probeId\":\"AR001\",\"finding\":\"...\",\"verdict\":\"NO_DEFECT|FIXED|PRESERVED|CONFLICT\"}],")
                    .append("\"disposition\":{\"disposition\":\"CONTINUE|PRESERVE_DRAFT|STOP\",\"reasonCode\":\"...\",")
                    .append("\"stopClass\":\"NONE|CONTENT_BLOCKED|INPUT_REQUIRED\"}}\n");
        }
        return new PromptPair(system.toString(), user.toString());
    }

    private static String authority(Request request, EditorialPackFileRole role) {
        byte[] bytes = request.authority() == null ? null : request.authority().bytes(role);
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }
}
