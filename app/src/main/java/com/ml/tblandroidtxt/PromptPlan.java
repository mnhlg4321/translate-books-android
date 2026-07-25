package com.ml.tblandroidtxt;

/** Exact, inspectable prompt breakdown used by requests, estimates and telemetry. */
public final class PromptPlan {
    public final PromptPair prompt;
    public final String phase;
    public final String source;
    public final String systemBase;
    public final String instruction;
    public final String glossary;
    public final String pronoun;
    public final String history;
    public final String surroundingContext;
    public final String outputContract;
    public final int rawTokens, systemTokens, instructionTokens, glossaryTokens;
    public final int pronounTokens, historyTokens, contextTokens, totalInputTokens;
    public final int glossaryLockCount, pronounLockCount;

    private PromptPlan(String phase, String source, String systemBase, String instruction,
                       String glossary, String pronoun, String history, String surroundingContext,
                       String outputContract, PromptPair prompt, int glossaryLockCount, int pronounLockCount) {
        this.phase = phase; this.source = safe(source); this.systemBase = safe(systemBase);
        this.instruction = safe(instruction); this.glossary = safe(glossary); this.pronoun = safe(pronoun);
        this.history = safe(history); this.surroundingContext = safe(surroundingContext);
        this.outputContract = safe(outputContract); this.prompt = prompt;
        this.glossaryLockCount = Math.max(0, glossaryLockCount);
        this.pronounLockCount = Math.max(0, pronounLockCount);
        rawTokens = tokens(this.source); systemTokens = tokens(this.systemBase) + tokens(this.outputContract);
        instructionTokens = tokens(this.instruction); glossaryTokens = tokens(this.glossary);
        pronounTokens = tokens(this.pronoun); historyTokens = tokens(this.history);
        contextTokens = tokens(this.surroundingContext);
        totalInputTokens = tokens(prompt.system) + tokens(prompt.user);
    }

    public static PromptPlan translation(Chunk chunk, String previous, AppSettings s) {
        AppSettings settings = s == null ? new AppSettings() : s;
        String ruleContext = safe(chunk.contextBefore) + "\n" + chunk.mainContent;
        PromptContextBuilder.ContextBlock locks = PromptContextBuilder.buildWithRuleContext(
                settings.glossaryText, settings.pronounText, chunk.mainContent, ruleContext, settings);
        String base = "You are a professional " + settings.targetLanguage + " translator and writer.\n\n"
                + "Translate " + settings.sourceLanguage + " to " + settings.targetLanguage + ". Preserve names, meaning, tone, layout, spacing, line breaks and indentation. "
                + "Use natural " + settings.targetLanguage + " phrasing, translate idioms appropriately, and do not add or omit information.";
        String instruction = safe(settings.translationInstructions).trim();
        String history = trimContext(previous, settings, true);
        String before = trimSideContext(chunk.contextBefore, settings, true);
        String after = trimSideContext(chunk.contextAfter, settings, false);
        String surrounding = (before.isEmpty() ? "" : "Before:\n" + before) + (after.isEmpty() ? "" : (before.isEmpty() ? "" : "\n\n") + "After:\n" + after);
        String contract = "Return only the translation between <TRANSLATION> and </TRANSLATION>; no notes or commentary.";
        StringBuilder system = new StringBuilder(base);
        if (!instruction.isEmpty()) system.append("\n\n# GLOBAL INSTRUCTION\n").append(instruction);
        if (!locks.glossary.trim().isEmpty()) system.append("\n\n# GLOSSARY LOCKS\n").append(locks.glossary.trim());
        if (!locks.pronouns.trim().isEmpty()) system.append("\n\n# PRONOUN LOCKS\n").append(locks.pronouns.trim());
        system.append("\n\n# OUTPUT CONTRACT\n").append(contract);
        StringBuilder user = new StringBuilder();
        if (!history.isEmpty()) user.append("# PREVIOUS TRANSLATION CONTEXT (do not translate)\n").append(history).append("\n\n");
        if (!surrounding.isEmpty()) user.append("# SURROUNDING SOURCE CONTEXT (do not translate)\n").append(surrounding).append("\n\n");
        user.append("# SOURCE\n<INPUT>\n").append(chunk.mainContent).append("\n</INPUT>\n\n<TRANSLATION>\n");
        PromptPair pair = new PromptPair(system.toString().trim(), user.toString().trim());
        return new PromptPlan("translate", chunk.mainContent, base, instruction, locks.glossary,
                locks.pronouns, history, surrounding, contract, pair, locks.glossaryCount, locks.pronounCount);
    }

    public static PromptPlan forTranslation(Chunk chunk, String previous, AppSettings s) {
        if (s != null && "full".equalsIgnoreCase(s.optimizationPreset)) {
            PromptPair legacy = PromptBuilder.translationPrompt(chunk, previous, s);
            PromptContextBuilder.ContextBlock locks = PromptContextBuilder.build(s.glossaryText, s.pronounText, chunk.mainContent, s);
            return new PromptPlan("translate", chunk.mainContent, legacy.system, s.translationInstructions,
                    locks.glossary, locks.pronouns, safe(previous), safe(chunk.contextBefore) + safe(chunk.contextAfter), "",
                    legacy, locks.glossaryCount, locks.pronounCount);
        }
        return translation(chunk, previous, s);
    }

    public static PromptPlan refinement(String source, String draft, AppSettings s) {
        AppSettings settings = s == null ? new AppSettings() : s;
        PromptContextBuilder.ContextBlock locks = PromptContextBuilder.build(settings.glossaryText, settings.pronounText, source, settings);
        String base = "You are a professional literary editor for " + settings.targetLanguage + ". Compare the draft with the source; fix mistranslations, omissions, speaker, terminology, fluency and formatting without adding information.";
        String instruction = safe(settings.refinementInstructions).trim();
        String contract = "Return only the final edited translation between <TRANSLATION> and </TRANSLATION>.";
        StringBuilder system = new StringBuilder(base);
        if (!instruction.isEmpty()) system.append("\n\n# REFINEMENT INSTRUCTION\n").append(instruction);
        if (!locks.glossary.trim().isEmpty()) system.append("\n\n# GLOSSARY LOCKS\n").append(locks.glossary.trim());
        if (!locks.pronouns.trim().isEmpty()) system.append("\n\n# PRONOUN LOCKS\n").append(locks.pronouns.trim());
        system.append("\n\n# OUTPUT CONTRACT\n").append(contract);
        String user = "# SOURCE\n<INPUT>\n" + safe(source) + "\n</INPUT>\n\n# DRAFT\n" + safe(draft) + "\n\n<TRANSLATION>\n";
        PromptPair pair = new PromptPair(system.toString().trim(), user.trim());
        return new PromptPlan("refine", source, base, instruction, locks.glossary, locks.pronouns, draft, "", contract,
                pair, locks.glossaryCount, locks.pronounCount);
    }

    public static PromptPlan forRefinement(String source, String draft, AppSettings s) {
        if (s != null && "full".equalsIgnoreCase(s.optimizationPreset)) {
            PromptPair legacy = PromptBuilder.refinementPrompt(source, draft, s);
            PromptContextBuilder.ContextBlock locks = PromptContextBuilder.build(s.glossaryText, s.pronounText, source, s);
            return new PromptPlan("refine", source, legacy.system, s.refinementInstructions,
                    locks.glossary, locks.pronouns, draft, "", "", legacy, locks.glossaryCount, locks.pronounCount);
        }
        return refinement(source, draft, s);
    }

    public int adaptiveMaxOutputTokens(AppSettings s) {
        int configured = Math.max(128, s == null ? 4096 : s.maxOutputTokens);
        String preset = s == null ? "balanced" : safe(s.optimizationPreset).toLowerCase();
        double ratio = preset.contains("quality") ? 1.8 : preset.contains("economy") ? 1.4 : 1.6;
        int reserve = Math.max(256, (int)Math.ceil(rawTokens * ratio) + 96);
        return Math.min(configured, reserve);
    }

    private static String trimContext(String value, AppSettings s, boolean history) {
        String v = safe(value).trim();
        int chars = Math.max(0, s == null ? 400 : s.contextChars);
        String preset = s == null ? "balanced" : safe(s.optimizationPreset).toLowerCase();
        double multiplier = preset.contains("quality") ? 1.5 : preset.contains("economy") ? 0.6 : 1.0;
        int max = Math.max(0, (int)(chars * multiplier));
        if (max == 0 || v.isEmpty()) return "";
        if (v.length() <= max) return v;
        return history ? v.substring(v.length() - max) : v.substring(0, max);
    }

    private static String trimSideContext(String value, AppSettings s, boolean before) {
        String v = safe(value).trim(); if (v.isEmpty()) return "";
        int total = Math.max(0, s == null ? 400 : s.contextChars);
        String preset = s == null ? "balanced" : safe(s.optimizationPreset).toLowerCase();
        double multiplier = preset.contains("quality") ? 1.5 : preset.contains("economy") ? 0.6 : 1.0;
        int max = Math.max(0, (int)(total * multiplier / 2.0));
        if (max == 0 || v.length() <= max) return max == 0 ? "" : v;
        return before ? v.substring(v.length() - max) : v.substring(0, max);
    }

    private static int tokens(String value) { return Chunker.approxTokens(safe(value)); }
    private static String safe(String value) { return value == null ? "" : value; }
}
