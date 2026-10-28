package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/**
 * The open Pronoun profile on the PC. The text is edited as plain text and saved as it is ({@link PronounStoreCore#upsert}); the table
 * is only a read-only view of how the shared parser understands that text, so a 3-column legacy file stays 3-column and no column is
 * ever split and glued back.
 */
public final class DesktopPronounEditor {
    public static final String[] COLUMNS_7 = {"from", "speaker", "target", "self", "call", "scope", "note"};
    public static final String[] COLUMNS_3 = {"from", "target", "details"};

    private final PronounStoreCore.Profile draft;
    private String baseline;

    private DesktopPronounEditor(PronounStoreCore.Profile draft) {
        this.draft = draft;
        this.baseline = signature();
    }

    public static DesktopPronounEditor open(PronounStoreCore.Profile stored) {
        PronounStoreCore.Profile copy = new PronounStoreCore.Profile();
        copy.id = stored.id; copy.name = stored.name; copy.uri = stored.uri; copy.text = stored.text;
        copy.createdAt = stored.createdAt; copy.updatedAt = stored.updatedAt;
        return new DesktopPronounEditor(copy);
    }

    public String id() { return draft.id; }
    public String name() { return draft.name; }
    public String text() { return draft.text; }
    public void setName(String v) { draft.name = v == null ? "" : v.trim(); }
    public void setText(String v) { draft.text = v == null ? "" : v; }
    public boolean dirty() { return !signature().equals(baseline); }

    /** True when every parsed rule is a legacy 3-column rule (and there is at least one). */
    public boolean legacyOnly() {
        List<PromptContextBuilder.PronounRule> rules = rules();
        if (rules.isEmpty()) return false;
        for (PromptContextBuilder.PronounRule r : rules) if (r.p3) return false;
        return true;
    }

    public String[] columns() { return legacyOnly() ? COLUMNS_3 : COLUMNS_7; }

    /** One row per rule the parser found, in file order, in the columns of {@link #columns()}. */
    public List<String[]> rows() {
        boolean legacy = legacyOnly();
        List<String[]> out = new ArrayList<>();
        for (PromptContextBuilder.PronounRule r : rules()) {
            if (legacy) out.add(new String[]{r.sourceName, r.targetName, r.details});
            else if (r.p3) out.add(new String[]{r.from, r.speaker, r.target, r.self, r.call, r.scope, r.note});
            else out.add(new String[]{r.sourceName, "", r.targetName, "", "", "", r.details});
        }
        return out;
    }

    private List<PromptContextBuilder.PronounRule> rules() {
        return PromptContextBuilder.validate("", draft.text).explicitPronouns;
    }

    /** Number of malformed rows ("Cảnh báo: n" in the editor). */
    public int warningCount() { return PromptContextBuilder.validate("", draft.text).malformedPronounRows.size(); }

    public int ruleCount() { return rules().size(); }

    /** Line under the title: "n quy tắc Pronoun • Hợp lệ" or the number of warnings. */
    public String statusLine() {
        int w = warningCount();
        return ruleCount() + " quy tắc Pronoun • " + (w == 0 ? "Hợp lệ" : "Cảnh báo: " + w);
    }

    public boolean save(DesktopRuntime runtime) {
        if (draft.name.trim().isEmpty()) draft.name = "Pronoun";
        if (!runtime.savePronoun(draft)) return false;
        baseline = signature();
        return true;
    }

    /** "Kiểm tra dữ liệu": the parser's report for the chosen Glossary and this profile, then the priority rule, as on the phone. */
    public String healthText(String glossaryText) {
        PromptContextBuilder.ParseReport r = PromptContextBuilder.validate(glossaryText == null ? "" : glossaryText, draft.text);
        return r.summaryText() + "\n\n" + DesktopGlossaryEditor.PRIORITY_RULE;
    }

    /** The warning count a list card shows: the number of conflicts, as on the phone. */
    public static int conflictCount(PronounStoreCore.Profile p) {
        return PromptContextBuilder.validate("", p.text).conflicts.size();
    }

    private String signature() { return draft.name + '\u0001' + draft.text; }
}
