package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.V5FinalExtractor;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * V5_CHAT sends the three owner-pack turns as three requests sharing one
 * OpenAI-compatible message history. Reports are retained by the runner but
 * are never parsed; only the final turn's &lt;FINAL&gt; section becomes EDITED.
 */
public final class V5ChatEditorialApiProvider implements EditorialApiProvider {
    public interface TurnRecorder {
        void record(int turn, List<OpenAICompatibleClient.ChatMessage> messages,
                    OpenAICompatibleClient.ChatResult result, String error) throws IOException;
    }

    private static final String FINAL_INSTRUCTION =
            "Sau toàn bộ báo cáo, đặt toàn văn bản cuối giữa <FINAL> và </FINAL>.";

    private final AppSettings settings;
    private final String projectInstruction;
    private final String promptFile;
    private final String workflow;
    private final String reasoningEffort;
    private final TurnRecorder recorder;
    private final OpenAICompatibleClient.CallControl control = new OpenAICompatibleClient.CallControl();
    private volatile int physicalCalls;
    private volatile BigDecimal lastWorstCase = BigDecimal.ZERO;

    public V5ChatEditorialApiProvider(AppSettings settings, String projectInstruction,
                                      String promptFile, String workflow, String reasoningEffort,
                                      TurnRecorder recorder) {
        if (settings == null) throw new IllegalArgumentException("settings are required");
        this.settings = settings.copy();
        this.projectInstruction = required(projectInstruction, "project instruction");
        this.promptFile = required(promptFile, "turn prompt");
        this.workflow = required(workflow, "workflow");
        this.reasoningEffort = reasoningEffort == null || reasoningEffort.isBlank() ? "medium" : reasoningEffort.trim();
        this.recorder = recorder;
        validatePromptSections(promptFile);
    }

    public int physicalCalls() { return physicalCalls; }
    public BigDecimal lastWorstCase() { return lastWorstCase; }

    /** Conservative aggregate reservation for all three requests in one chat. */
    public BigDecimal worstCase(EditorialApiRunService.Pricing pricing, String model,
                                int maxOutputTokens, EditorialApiFlow.Request request) {
        List<String> turns = turnTexts(request);
        long input = 0;
        long output = 0;
        List<OpenAICompatibleClient.ChatMessage> history = new ArrayList<>();
        history.add(new OpenAICompatibleClient.ChatMessage("system", projectInstruction));
        for (String turn : turns) {
            history.add(new OpenAICompatibleClient.ChatMessage("user", turn));
            for (OpenAICompatibleClient.ChatMessage message : history) input += approxTokens(message.content());
            history.add(new OpenAICompatibleClient.ChatMessage("assistant", "x".repeat(Math.max(1, maxOutputTokens * 3))));
            output += maxOutputTokens;
        }
        BigDecimal result = pricing.inputPerToken(model).multiply(BigDecimal.valueOf(input))
                .add(pricing.outputPerToken(model).multiply(BigDecimal.valueOf(output)));
        lastWorstCase = result;
        return result;
    }

    @Override public void cancel() { control.cancel("EDITORIAL_API_USER_CANCEL"); }

    @Override public EditorialApiFlow.StepResponse call(EditorialApiFlow.Request request, String model,
                                                        int maxOutputTokens, long timeoutMillis) {
        physicalCalls = 0;
        try {
            AppSettings s = settings.copy();
            if (model != null && !model.trim().isEmpty()) s.model = model.trim();
            if (s.apiKey == null || s.apiKey.trim().isEmpty()) return EditorialApiFlow.StepResponse.failure("API_KEY_MISSING");
            List<String> turns = turnTexts(request);
            List<OpenAICompatibleClient.ChatMessage> history = new ArrayList<>();
            history.add(new OpenAICompatibleClient.ChatMessage("system", projectInstruction));
            long input = 0, output = 0;
            BigDecimal cost = BigDecimal.ZERO;
            boolean known = true;
            String servedModel = s.model;
            String route = "";
            for (int i = 0; i < turns.size(); i++) {
                int turn = i + 1;
                history.add(new OpenAICompatibleClient.ChatMessage("user", turns.get(i)));
                long deadline = timeoutMillis > 0
                        ? OpenAICompatibleClient.monotonicDeadlineNanosFromNowMillis(timeoutMillis) : 0L;
                OpenAICompatibleClient.ChatResult result;
                try {
                    result = OpenAICompatibleClient.chatWithUsage(s, history, maxOutputTokens,
                            "editorial-api-v5-chat-l" + turn, null, false, deadline, control,
                            null, null, reasoningEffort);
                } catch (Exception failure) {
                    boolean wasDispatched = dispatched(failure);
                    if (wasDispatched) physicalCalls++;
                    record(turn, history, null, describe(failure));
                    return aggregateFailure(input, output, cost, wasDispatched ? false : known, servedModel, route,
                            "V5_TURN_" + turn + "_" + (wasDispatched ? "UNKNOWN" : "FAILED"));
                }
                physicalCalls++;
                input += Math.max(0, result.promptTokens);
                output += Math.max(0, result.completionTokens);
                BigDecimal turnCost = cost(result, s.model);
                boolean turnKnown = turnCost != null;
                if (turnKnown) cost = cost.add(turnCost); else known = false;
                if (!turnKnown) cost = cost.add(BigDecimal.ZERO);
                if (!result.responseModel.isBlank()) servedModel = result.responseModel;
                if (!result.responseProvider.isBlank()) route = result.responseProvider;
                record(turn, history, result, "");
                String finish = result.finishReason == null ? "" : result.finishReason;
                if ("length".equalsIgnoreCase(finish)) {
                    return new EditorialApiFlow.StepResponse("", finish, input, output, cost, known,
                            servedModel, route, "V5_TRUNCATED_L" + turn);
                }
                history.add(new OpenAICompatibleClient.ChatMessage("assistant", result.content));
            }
            String finalText = V5FinalExtractor.extract(history.get(history.size() - 1).content());
            if (finalText.isBlank()) {
                return new EditorialApiFlow.StepResponse("", "stop", input, output, cost, known,
                        servedModel, route, "V5_FINAL_MISSING");
            }
            String wrapped = "<EDITED>" + finalText + "</EDITED>";
            return new EditorialApiFlow.StepResponse(wrapped, "stop", input, output, cost, known,
                    servedModel, route, "");
        } catch (RuntimeException failure) {
            return EditorialApiFlow.StepResponse.failure("V5_LOCAL_" + failure.getClass().getSimpleName());
        }
    }

    private EditorialApiFlow.StepResponse aggregateFailure(long input, long output, BigDecimal cost,
                                                            boolean known, String model, String route, String error) {
        return new EditorialApiFlow.StepResponse("", "", input, output, cost, known, model, route, error);
    }

    private void record(int turn, List<OpenAICompatibleClient.ChatMessage> messages,
                        OpenAICompatibleClient.ChatResult result, String error) {
        if (recorder != null) {
            try { recorder.record(turn, List.copyOf(messages), result, error); }
            catch (IOException failure) { throw new IllegalStateException("V5_CAPTURE_FAILED", failure); }
        }
    }

    private List<String> turnTexts(EditorialApiFlow.Request request) {
        int l1 = promptFile.indexOf("LƯỢT 1");
        int l2 = promptFile.indexOf("LƯỢT 2");
        int l3 = promptFile.indexOf("LƯỢT 3");
        String first = promptFile.substring(Math.max(0, l1), l2);
        String second = promptFile.substring(l2, l3);
        String third = promptFile.substring(l3).strip() + "\n" + FINAL_INSTRUCTION;
        String source = "WORKFLOW\n" + workflow + "\nSOURCE INPUTS\n"
                + request.prompt().user() + "\nREFERENCE AND APP CONTEXT\n" + request.prompt().system();
        return List.of("PROJECT INSTRUCTION\n" + projectInstruction + "\n\n" + first + "\n\n" + source,
                second, third);
    }

    private static void validatePromptSections(String prompt) {
        if (prompt.indexOf("LƯỢT 1") < 0 || prompt.indexOf("LƯỢT 2") < 0 || prompt.indexOf("LƯỢT 3") < 0) {
            throw new IllegalArgumentException("V5_PROMPT_SECTIONS_INVALID");
        }
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("V5_" + label.toUpperCase(Locale.ROOT).replace(' ', '_') + "_MISSING");
        return value;
    }

    private static long approxTokens(String value) { return ((long) value.length() / 3L) + 1L; }

    private static BigDecimal cost(OpenAICompatibleClient.ChatResult result, String model) {
        if (result.providerCostReported && Double.isFinite(result.providerCost) && result.providerCost >= 0) {
            return BigDecimal.valueOf(result.providerCost);
        }
        if (model != null && model.equalsIgnoreCase("openai/gpt-5.6-sol")) {
            return BigDecimal.valueOf(result.promptTokens).multiply(new BigDecimal("0.000002"))
                    .add(BigDecimal.valueOf(result.completionTokens).multiply(new BigDecimal("0.000010")));
        }
        return null;
    }

    private static boolean dispatched(Exception failure) {
        return !OpenRouterEditorialApiProvider.notDispatched(failure);
    }

    private static String describe(Exception failure) {
        return OpenRouterEditorialApiProvider.describe(failure);
    }
}
