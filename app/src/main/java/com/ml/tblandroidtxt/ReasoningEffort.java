package com.ml.tblandroidtxt;

import java.util.Locale;

/**
 * The reasoning effort sent with translation requests. A reasoning model spends part of the output budget on thinking, so a long chapter
 * chunk can be cut before the answer ends; "low" keeps the thinking short. "auto" sends "low" only to model ids that are known to reason,
 * "off" never sends the parameter, and low/medium/high are sent as chosen.
 */
final class ReasoningEffort {
    static final String AUTO = "auto", OFF = "off", LOW = "low", MEDIUM = "medium", HIGH = "high";

    private ReasoningEffort() {}

    static String normalize(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        switch (v) {
            case OFF: case LOW: case MEDIUM: case HIGH: return v;
            default: return AUTO;
        }
    }

    /** True for model ids that reason before answering (OpenAI gpt-5 / o-series, DeepSeek R1 / reasoner, "thinking" variants). */
    static boolean looksLikeReasoningModel(String model) {
        String m = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        if (m.isEmpty()) return false;
        return m.contains("gpt-5") || m.matches(".*(^|/)o[1-9](-.*)?$") || m.contains("deepseek-r1") || m.contains("-reasoner")
                || m.contains("thinking") || m.contains("/r1");
    }

    /** The value to put in {@code reasoning_effort}, or "" to leave the parameter out. */
    static String forTranslation(AppSettings s) {
        String choice = normalize(s == null ? "" : s.reasoningEffort);
        if (OFF.equals(choice)) return "";
        if (AUTO.equals(choice)) return looksLikeReasoningModel(s == null ? "" : s.model) ? LOW : "";
        return choice;
    }
}
