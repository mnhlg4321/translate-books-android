package com.ml.tblandroidtxt;

public class PromptBuilder {
    public static final String INPUT_IN = "<INPUT>";
    public static final String INPUT_OUT = "</INPUT>";
    public static final String TRANS_IN = "<TRANSLATION>";
    public static final String TRANS_OUT = "</TRANSLATION>";

    public static PromptPair translationPrompt(Chunk chunk, String previousTranslation, AppSettings s) {
        if (s != null && !"full".equalsIgnoreCase(s.optimizationPreset)) {
            return PromptPlan.translation(chunk, previousTranslation, s).prompt;
        }
        String custom = "";
        if (s.translationInstructions != null && !s.translationInstructions.trim().isEmpty()) {
            custom = "# ⚠️ MANDATORY STYLE INSTRUCTIONS - ABSOLUTE PRIORITY ⚠️\n\n"
                    + "These instructions override all other guidelines.\n\n"
                    + s.translationInstructions.trim() + "\n\n";
        }
        String contextualRules = buildContextualRules(chunk, s);
        String system = "You are a professional " + s.targetLanguage + " translator and writer.\n\n"
                + custom
                + contextualRules
                + "# TRANSLATION PRINCIPLES\n\n"
                + "Translate " + s.sourceLanguage + " to " + s.targetLanguage + ". Output only the translation.\n\n"
                + "PRIORITY ORDER:\n"
                + "1. Preserve exact names.\n"
                + "2. Match original tone and formality.\n"
                + "3. Use natural " + s.targetLanguage + " phrasing, never word-for-word.\n"
                + "4. Fix grammar/spelling errors in output.\n"
                + "5. Translate idioms to " + s.targetLanguage + " equivalents.\n\n"
                + "LAYOUT PRESERVATION:\n"
                + "- Keep the exact text layout, spacing, line breaks, and indentation.\n"
                + "- WRITE YOUR TRANSLATION IN " + s.targetLanguage.toUpperCase() + ".\n\n"
                + outputFormat();

        String prev = "";
        if (previousTranslation != null && !previousTranslation.trim().isEmpty()) {
            prev = "# CONTEXT - Previous Paragraph\n\nFor consistency and natural flow, here's what came immediately before:\n\n"
                    + previousTranslation.trim() + "\n\n";
        }
        String surrounding = "";
        if (!chunk.contextBefore.isEmpty() || !chunk.contextAfter.isEmpty()) {
            surrounding = "# SURROUNDING CONTEXT - DO NOT TRANSLATE THIS SECTION\n\n"
                    + "Before:\n" + chunk.contextBefore + "\n\nAfter:\n" + chunk.contextAfter + "\n\n";
        }
        String user = prev + surrounding
                + "# TEXT TO TRANSLATE\n\n"
                + INPUT_IN + "\n" + chunk.mainContent + "\n" + INPUT_OUT + "\n\n"
                + "REMINDER: Output ONLY your translation in this exact format:\n"
                + TRANS_IN + "\nyour translation here\n" + TRANS_OUT + "\n\n"
                + "Start with " + TRANS_IN + " and end with " + TRANS_OUT + ". Nothing before or after.\n\n"
                + "Provide your translation now:";
        return new PromptPair(system.trim(), user.trim());
    }

    public static PromptPair refinementPrompt(String source, String draft, AppSettings s) {
        if (s != null && !"full".equalsIgnoreCase(s.optimizationPreset)) {
            return PromptPlan.refinement(source, draft, s).prompt;
        }
        String custom = "";
        if (s.refinementInstructions != null && !s.refinementInstructions.trim().isEmpty()) {
            custom = "# MANDATORY REFINEMENT INSTRUCTIONS\n\n" + s.refinementInstructions.trim() + "\n\n";
        }
        String contextualRules = buildContextualRules(source, s);
        String system = "You are a professional literary editor for " + s.targetLanguage + " translations.\n\n"
                + custom
                + contextualRules
                + "Edit the draft translation by checking it against the source. Fix mistranslations, omissions, wrong speaker/subject, unnatural phrasing, inconsistent terms, and broken formatting. Do not add new plot details. Output only the final edited translation.\n\n"
                + outputFormat();
        String user = "# SOURCE TEXT\n" + INPUT_IN + "\n" + source + "\n" + INPUT_OUT + "\n\n"
                + "# DRAFT TRANSLATION\n" + draft + "\n\n"
                + "Return the edited translation only:\n" + TRANS_IN + "\nfinal edited translation here\n" + TRANS_OUT;
        return new PromptPair(system.trim(), user.trim());
    }


    private static String buildContextualRules(String sourceText, AppSettings s) {
        if (s == null) return "";
        PromptContextBuilder.ContextBlock block = PromptContextBuilder.build(s.glossaryText, s.pronounText, sourceText, s);
        String body = block.asPromptText();
        if (body.trim().isEmpty()) return "";
        return "# CONTEXTUAL TERM AND PRONOUN LOCKS\n\n"
                + "Only the glossary/pronoun entries relevant to this chunk are injected below. Treat them as mandatory locks.\n\n"
                + body;
    }

    private static String buildContextualRules(Chunk chunk, AppSettings s) {
        if (s == null || chunk == null) return "";
        PromptContextBuilder.ContextBlock block = PromptContextBuilder.buildWithRuleContext(
                s.glossaryText, s.pronounText, chunk.mainContent,
                chunk.contextBefore + "\n" + chunk.mainContent,
                chunk.paragraphStart, chunk.paragraphEnd, s);
        String body = block.asPromptText();
        if (body.trim().isEmpty()) return "";
        return "# CONTEXTUAL TERM AND PRONOUN LOCKS\n\n"
                + "Only the glossary/pronoun entries relevant to this chunk are injected below. Treat them as mandatory locks.\n\n"
                + body;
    }

    private static String outputFormat() {
        return "# OUTPUT FORMAT\n\n"
                + "CRITICAL OUTPUT RULES:\n"
                + "1. Translate ONLY the text between \"" + INPUT_IN + "\" and \"" + INPUT_OUT + "\" tags.\n"
                + "2. Your response MUST start with " + TRANS_IN + ".\n"
                + "3. Your response MUST end with " + TRANS_OUT + ".\n"
                + "4. Include NOTHING before " + TRANS_IN + " and NOTHING after " + TRANS_OUT + ".\n"
                + "5. Do NOT add explanations, comments, notes, or greetings.\n\n"
                + "CORRECT format:\n" + TRANS_IN + "\nYour translated text here\n" + TRANS_OUT + "\n";
    }

    public static boolean hasTranslationTags(String raw) {
        if (raw == null) return false;
        String cleaned = stripThinking(raw);
        int a = cleaned.indexOf(TRANS_IN);
        int b = cleaned.lastIndexOf(TRANS_OUT);
        return a >= 0 && b > a;
    }

    public static String extractTranslation(String raw) {
        if (raw == null) return "";
        String cleaned = stripThinking(raw).trim();
        int a = cleaned.indexOf(TRANS_IN);
        int b = cleaned.lastIndexOf(TRANS_OUT);
        if (a >= 0 && b > a) {
            return cleaned.substring(a + TRANS_IN.length(), b).trim();
        }
        return cleaned.trim();
    }

    public static String extractTranslationStrict(String raw) {
        String cleaned = stripCodeFence(stripThinking(raw).trim());
        int a = cleaned.indexOf(TRANS_IN);
        int b = cleaned.lastIndexOf(TRANS_OUT);
        if (a >= 0 && b > a) {
            String out = cleaned.substring(a + TRANS_IN.length(), b).trim();
            validateTranslationOutput(out);
            return out;
        }

        // Some OpenAI-compatible models occasionally ignore wrapper tags but still return only
        // the translation. Accept that safe plain-text case to avoid failing every chunk, while
        // still rejecting prompt echoes, refusals, or meta commentary.
        validateTranslationOutput(cleaned);
        if (looksLikePromptEcho(cleaned)) {
            throw new RuntimeException("Model response missing " + TRANS_IN + " tags and looks like prompt/meta output. Preview: " + preview(cleaned));
        }
        return cleaned.trim();
    }

    public static void validateTranslationOutput(String out) {
        String v = out == null ? "" : out.trim();
        if (v.isEmpty()) throw new RuntimeException("Empty translation after tag parsing");
        String l = v.toLowerCase();
        if (l.contains("yêu cầu có vẻ kỳ lạ")
                || l.contains("yeu cau co ve ky la")
                || l.contains("request seems unusual")
                || l.contains("your request seems")
                || l.contains("the request seems")
                || l.contains("i can't translate")
                || l.contains("i cannot translate")
                || l.contains("as an ai")
                || l.contains("tôi không thể dịch")
                || l.contains("toi khong the dich")
                || l.contains("mình không thể dịch")
                || l.contains("minh khong the dich")) {
            throw new RuntimeException("Model returned meta/commentary instead of translation. Preview: " + preview(v));
        }
    }

    private static boolean looksLikePromptEcho(String s) {
        String l = (s == null ? "" : s).toLowerCase();
        return l.contains(INPUT_IN.toLowerCase())
                || l.contains(INPUT_OUT.toLowerCase())
                || l.contains("# text to translate")
                || l.contains("# output format")
                || l.contains("correct format:")
                || l.contains("provide your translation now")
                || l.contains("your translation here")
                || l.contains("final edited translation here");
    }

    private static String stripCodeFence(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNl >= 0 && lastFence > firstNl) s = s.substring(firstNl + 1, lastFence).trim();
        }
        return s;
    }

    private static String stripThinking(String raw) {
        return raw == null ? "" : raw.replaceAll("(?is)<think>.*?</think>", "");
    }

    private static String preview(String s) {
        if (s == null) return "";
        String one = s.replace('\n', ' ').replace('\r', ' ').trim();
        return one.length() <= 180 ? one : one.substring(0, 180) + "...";
    }
}
