package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
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
 * OpenRouter adapter for one L2_EDIT call on the qualified fresh route. It only
 * renders the prompt and transports bytes: the model returns line-anchored change
 * rows, never the edited text, and {@link EditorialL2Execution} decides validity,
 * reconstruction and commit. No retry, repair or fallback happens here.
 */
public final class OpenRouterEditorialL2Provider implements EditorialL2Execution.Provider {
    static final String WIRE_FORMAT_RULES =
            "Wire rules enforced by the app (any violation rejects the whole response):\n"
            + "- Edit only by whole DRAFT lines. line is the number shown as L<n>| in the DRAFT block; "
            + "before is that line's exact text without the L<n>| prefix; after is the full replacement line without line breaks.\n"
            + "- changeId and errorId: 1-48 ASCII characters matching [A-Za-z0-9][A-Za-z0-9._:/-]* (for example C001, E001). "
            + "Every change has its own errorId opened before the edit and at most one CLOSED change per line.\n"
            + "- dialogue is true when the line contains spoken words. A dialogue change needs speakerProof "
            + "{speaker, listener, anchorBefore, anchorAfter} grounded in RAW; without that proof keep the draft line "
            + "(add a preserved row instead of a change). The app reverts any dialogue change without complete proof.\n"
            + "- preserved rows: preserveId, line, exact before text, evidenceLimit (why the draft is kept).\n"
            + "- status is CLOSED for an applied change or REVERTED for a change you opened and withdrew.\n"
            + "- disposition.disposition is CONTINUE, PRESERVE_DRAFT or STOP; stopClass is NONE unless STOP, "
            + "then CONTENT_BLOCKED (proven conflict that cannot be fixed, kept or reverted safely) or INPUT_REQUIRED. "
            + "reasonCode: at most 32 characters, starts with a letter or digit, then letters, digits, spaces and . _ : / ; ( ) -.\n"
            + "- Never return the whole chapter, hashes or identities other than the attemptIdentity echo.\n";

    private final AppSettings settings;

    public OpenRouterEditorialL2Provider(AppSettings settings) {
        if (settings == null) throw new IllegalArgumentException("OpenRouter settings are required");
        this.settings = settings.copy();
    }

    @Override public Response call(Request request) throws Exception {
        if (request == null || !EditorialL2Execution.PHASE.equals(request.phase())
                || !EditorialL2Execution.WIRE_SCHEMA_VERSION.equals(request.outputSchemaId())) {
            throw new IllegalStateException("P6_L2_REQUEST_INVALID");
        }
        if (!EditorialP5EFreshRawRoutingPolicy.matches(settings)) {
            throw new IllegalStateException("P6_L2_ROUTE_SETTINGS_MISMATCH");
        }
        if (settings.apiKey == null || settings.apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OPENROUTER_CONFIGURATION_INCOMPLETE");
        }
        OpenAICompatibleClient.ChatResult result = OpenAICompatibleClient.chatWithUsage(settings,
                buildPrompt(request), request.maximumOutputTokens(), request.attemptIdentity(), null,
                false, OpenAICompatibleClient.monotonicDeadlineNanosFromNowMillis(
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

    /** Pure rendering; visible roles come only from the app-side phase projection. */
    static PromptPair buildPrompt(Request request) {
        StringBuilder system = new StringBuilder();
        system.append("You are an untrusted SAFE4 L2 editor. The app is the authority.\n")
                .append("Return exactly one JSON object and no Markdown or commentary.\n")
                .append("Read RAW first and independently, then the DRAFT and REPORT_L1. Fix coverage, meaning, ")
                .append("titles and glossary before relations, then local naturalness. ")
                .append("When the evidence does not clearly prove a better target, keep the draft line.\n")
                .append(WIRE_FORMAT_RULES)
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
                    .append(EditorialSafe4Contract.DRAFT.equals(role) ? numbered(text) : text)
                    .append("\n--- END ").append(role).append(" ---\n");
        }
        user.append("\nReturn only this object:\n")
                .append("{\"wireSchemaVersion\":\"").append(EditorialL2Execution.WIRE_SCHEMA_VERSION)
                .append("\",\"attemptIdentity\":\"<exact echo>\",")
                .append("\"changes\":[{\"changeId\":\"C001\",\"errorId\":\"E001\",\"line\":1,\"before\":\"...\",")
                .append("\"after\":\"...\",\"reason\":\"...\",\"dialogue\":false,\"status\":\"CLOSED\"}],")
                .append("\"preserved\":[{\"preserveId\":\"P001\",\"line\":1,\"before\":\"...\",\"evidenceLimit\":\"...\"}],")
                .append("\"disposition\":{\"disposition\":\"CONTINUE|PRESERVE_DRAFT|STOP\",\"reasonCode\":\"...\",")
                .append("\"stopClass\":\"NONE|CONTENT_BLOCKED|INPUT_REQUIRED\"}}\n");
        return new PromptPair(system.toString(), user.toString());
    }

    /** DRAFT lines numbered exactly as the app splits them (LF or CRLF; last line may be unterminated). */
    static String numbered(String text) {
        String[] lines = text.split("\\r?\\n", -1);
        StringBuilder out = new StringBuilder(text.length() + lines.length * 8);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) out.append('\n');
            out.append('L').append(i + 1).append('|').append(lines[i]);
        }
        return out.toString();
    }

    private static String authority(Request request, EditorialPackFileRole role) {
        byte[] bytes = request.authority() == null ? null : request.authority().bytes(role);
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }
}
