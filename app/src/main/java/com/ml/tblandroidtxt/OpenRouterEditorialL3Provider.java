package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialFinalRead;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialL3Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

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

    static final String CARRIED_ROLE = "L3_CARRIED_DEFECTS";

    static final String REAUDIT_RULES_V2 =
            "Re-audit rules enforced by the app (any violation rejects the whole response):\n"
            + "- You see only RAW, GLOSSARY and VI_L2. RAW units are shown as <unitId>|<text>; VI_L2 lines as L<n>|. "
            + "Build the expected model from RAW first, then read the whole VI_L2.\n"
            + "- coverage: ordered, contiguous, non-overlapping ranges {from,to,status} over the unit ids from the first to the last unit, "
            + "no gap and no overlap; PROCESSED for units you read, PRESERVED only for units you could not assess.\n"
            + "- candidates are sparse. ledger: TG glossary or title term, SR relationship or address, RC recurring or contrasting concept, "
            + "PAIR named pair, SPEAKER unclear speaker, UNIT any other trap. unitId is copied exactly; viLine is the VI_L2 line where that unit is "
            + "rendered, 0 when it is missing; status PROCESSED (VI_L2 renders it correctly), PRESERVED (kept with an evidence limit), "
            + "UNPROCESSED (needs a fix or is not yet checked), CONFLICT (proven unresolved conflict); note at most 80 characters.\n"
            + "- candidateId: 1-48 ASCII characters matching [A-Za-z0-9][A-Za-z0-9._:/-]*, unique.\n"
            + "- Never return chapter text, hashes or identities other than the attemptIdentity echo.\n";

    static final String RECONCILE_RULES_V2 =
            "Reconcile rules enforced by the app (any violation rejects the whole response):\n"
            + "- You now also see DRAFT, REPORT_L1, CHANGE_MAP_L2, " + CANDIDATES_ROLE + " (the candidates of your blind pass) and " + CARRIED_ROLE
            + " (defects the reader of VI_L2 reported; each has an index, a VI_L2 line, a quote and a type).\n"
            + "- resolutions may update any candidateId from " + CANDIDATES_ROLE + "; unknown ids are rejected.\n"
            + "- carriedResolutions has EXACTLY one row per entry of " + CARRIED_ROLE + ", by index (an empty list when there are none): status FIXED "
            + "(changeIds are CLOSED changes touching that VI_L2 line or its neighbours), REJECTED (evidenceQuote is an exact substring of that VI_L2 line and "
            + "reason says why the line is right), PRESERVED (preserveIds name a preserved row on that very line), UNRESOLVED (the app stops the chapter).\n"
            + "- QA edits use VI_L2 lines: line is the L<n>| number. op is REPLACE (default; before is the exact line, after the full replacement without line breaks), "
            + "INSERT_AFTER (new line after VI_L2 line n; 0 inserts at the top; before is the exact text of line n, empty for 0), DELETE (after is empty) or "
            + "MERGE_WITH_NEXT (lines n and n+1 become the single line in after). Every change has its own QA errorId and changeId; dialogue changes need "
            + "speakerProof {speaker, listener, anchorBefore, anchorAfter} grounded in RAW, otherwise keep the line with a preserved row.\n"
            + "- probes: at least " + 3 + " COVERAGE and " + 3 + " REGRESSION, each anchored: rawUnits (1-6 exact unit ids), viStart..viEnd (VI_L2 lines), "
            + "scope (what you checked), contrast (the counter-check or control you compared against), rawQuote (exact substring of one rawUnit, at most 80 "
            + "characters), viQuote (exact substring of those VI_L2 lines, at most 80 characters), verdict NO_DEFECT (action NONE), DEFECT_FOUND "
            + "(action CHANGE:<changeId> of a CLOSED change on or next to those lines), PRESERVED (action PRESERVE:<preserveId> of a row on those lines) or "
            + "CONFLICT (action NONE; the app stops the chapter). No two probes share the same anchors and together they cover at least min(6, units) "
            + "different RAW units. COVERAGE probes try to refute coverage (missed unit, glossary or title, wrong sense, relation or listener, speaker); "
            + "REGRESSION probes treat every actual change as suspect (unneeded change, lost subject or voice, honorific, symbols, numbers, line loss).\n"
            + "- disposition as in L2: CONTINUE or PRESERVE_DRAFT with stopClass NONE, or STOP with CONTENT_BLOCKED/INPUT_REQUIRED; reasonCode at most 32 "
            + "characters, starts with a letter or digit, then letters, digits, spaces and . _ : / ; ( ) -.\n"
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
                && (EditorialL3Execution.REAUDIT_WIRE.equals(request.outputSchemaId())
                || EditorialL3Execution.REAUDIT_WIRE_V2.equals(request.outputSchemaId()))
                && !request.visibleSources().containsKey(CANDIDATES_ROLE);
        boolean reconcile = EditorialL3Execution.RECONCILE_PHASE.equals(request.phase())
                && (EditorialL3Execution.RECONCILE_WIRE.equals(request.outputSchemaId())
                || EditorialL3Execution.RECONCILE_WIRE_V2.equals(request.outputSchemaId()))
                && request.visibleSources().containsKey(CANDIDATES_ROLE);
        boolean read = EditorialFinalRead.L3_PHASE.equals(request.phase())
                && EditorialFinalRead.WIRE.equals(request.outputSchemaId())
                && request.visibleSources().containsKey(EditorialFinalRead.TARGET_ROLE)
                && request.visibleSources().containsKey(EditorialFinalRead.PROBES_ROLE)
                && request.visibleSources().containsKey(EditorialSafe4Contract.RAW);
        return reaudit || reconcile || read;
    }

    static PromptPair buildPrompt(Request request) {
        boolean reaudit = EditorialL3Execution.REAUDIT_PHASE.equals(request.phase());
        boolean read = EditorialFinalRead.L3_PHASE.equals(request.phase());
        boolean v2 = EditorialL3Execution.REAUDIT_WIRE_V2.equals(request.outputSchemaId())
                || EditorialL3Execution.RECONCILE_WIRE_V2.equals(request.outputSchemaId());
        StringBuilder system = new StringBuilder();
        system.append(read ? "You are an untrusted SAFE4 final reader. The app is the authority.\n"
                        : "You are an untrusted SAFE4 L3 QA assistant. The app is the authority.\n")
                .append("Return exactly one JSON object and no Markdown or commentary.\n")
                .append(read ? OpenRouterEditorialL2Provider.FINAL_READ_RULES
                        : reaudit ? (v2 ? REAUDIT_RULES_V2 : REAUDIT_RULES) : (v2 ? RECONCILE_RULES_V2 : RECONCILE_RULES))
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
                    .append(v2 && EditorialSafe4Contract.RAW.equals(role) && !read
                            ? OpenRouterEditorialP5PilotProvider.renderUnits(request.visibleSources().get(role))
                            : "VI_L2".equals(role) || EditorialFinalRead.TARGET_ROLE.equals(role)
                            || (EditorialSafe4Contract.DRAFT.equals(role) && v2)
                            ? OpenRouterEditorialL2Provider.numbered(text) : text)
                    .append("\n--- END ").append(role).append(" ---\n");
        }
        user.append("\nReturn only this object:\n");
        if (read) {
            user.append("{\"wireSchemaVersion\":\"").append(EditorialFinalRead.WIRE)
                    .append("\",\"attemptIdentity\":\"<exact echo>\",\"readSha256\":\"<targetSha256 copied>\",")
                    .append("\"probeTails\":[{\"line\":1,\"tail\":\"...\"}],\"verdict\":\"CLEAN|DEFECTS\",")
                    .append("\"defects\":[{\"line\":1,\"quote\":\"...\",\"type\":\"MEANING\",\"note\":\"...\"}]}\n");
        } else if (v2 && reaudit) {
            user.append("{\"wireSchemaVersion\":\"").append(EditorialL3Execution.REAUDIT_WIRE_V2)
                    .append("\",\"attemptIdentity\":\"<exact echo>\",")
                    .append("\"coverage\":[{\"from\":\"<first unitId>\",\"to\":\"<last unitId>\",\"status\":\"PROCESSED|PRESERVED\"}],")
                    .append("\"candidates\":[{\"candidateId\":\"c1\",\"ledger\":\"UNIT|TG|SR|RC|PAIR|SPEAKER\",\"unitId\":\"<unitId>\",")
                    .append("\"viLine\":1,\"status\":\"PROCESSED|PRESERVED|UNPROCESSED|CONFLICT\",\"note\":\"...\"}]}\n");
        } else if (v2) {
            user.append("{\"wireSchemaVersion\":\"").append(EditorialL3Execution.RECONCILE_WIRE_V2)
                    .append("\",\"attemptIdentity\":\"<exact echo>\",")
                    .append("\"resolutions\":[{\"candidateId\":\"c1\",\"status\":\"PROCESSED|PRESERVED|UNPROCESSED|CONFLICT\"}],")
                    .append("\"carriedResolutions\":[{\"index\":0,\"status\":\"FIXED|REJECTED|PRESERVED|UNRESOLVED\",\"changeIds\":[],")
                    .append("\"preserveIds\":[],\"evidenceQuote\":\"\",\"reason\":\"\"}],")
                    .append("\"changes\":[{\"changeId\":\"QC001\",\"errorId\":\"QE001\",\"op\":\"REPLACE|INSERT_AFTER|DELETE|MERGE_WITH_NEXT\",")
                    .append("\"line\":1,\"before\":\"...\",\"after\":\"...\",\"reason\":\"...\",\"dialogue\":false,\"status\":\"CLOSED\"}],")
                    .append("\"preserved\":[{\"preserveId\":\"QP001\",\"line\":1,\"before\":\"...\",\"evidenceLimit\":\"...\"}],")
                    .append("\"probes\":[{\"probeId\":\"P001\",\"kind\":\"COVERAGE|REGRESSION\",\"rawUnits\":[\"<unitId>\"],\"viStart\":1,\"viEnd\":1,")
                    .append("\"scope\":\"...\",\"contrast\":\"...\",\"rawQuote\":\"...\",\"viQuote\":\"...\",")
                    .append("\"verdict\":\"NO_DEFECT|DEFECT_FOUND|PRESERVED|CONFLICT\",\"action\":\"NONE|CHANGE:<changeId>|PRESERVE:<preserveId>\"}],")
                    .append("\"disposition\":{\"disposition\":\"CONTINUE|PRESERVE_DRAFT|STOP\",\"reasonCode\":\"...\",")
                    .append("\"stopClass\":\"NONE|CONTENT_BLOCKED|INPUT_REQUIRED\"}}\n");
        } else if (reaudit) {
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
