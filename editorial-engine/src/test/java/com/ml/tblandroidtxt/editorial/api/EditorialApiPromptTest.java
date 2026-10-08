package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Quality Core, reference filtering and the edit/check prompts. Synthetic text only. */
public final class EditorialApiPromptTest {
    private static final String RAW = "太郎は赤い扉を開けた。\n花子が言った。「五百メートル先です」\n";
    private static final String DRAFT = "Taro mở cánh cửa đỏ.\nHanako nói: \"Cách năm mươi mét.\"\n";

    private static EditInputs inputs(List<EditInputs.GlossaryEntry> glossary, String pronouns) {
        return new EditInputs(RAW, DRAFT, "Vietnamese", glossary, pronouns);
    }

    @Test public void qualityCoreHasTheEightRulesAndNoLedgerVocabulary() {
        String text = QualityCore.editPrompt("Vietnamese") + QualityCore.checkPrompt("Vietnamese");
        for (int rule = 1; rule <= 8; rule++) assertTrue("rule " + rule, QualityCore.rules().contains(rule + ". "));
        String lower = text.toLowerCase(Locale.ROOT);
        assertFalse(lower.contains("changing as little as possible"));
        assertTrue(lower.contains("preserve lines that are already correct"));
        assertTrue(lower.contains("do not change phrasing that is already correct in meaning, voice and naturalness"));
        assertTrue(lower.contains("preserve capitalization of status labels and terms"));
        for (String banned : new String[] {"ledger", "sha-256", "sha256", "hash", "unit ref", "manifest", "receipt", "error id", "change id", "binding"}) {
            assertFalse("Quality Core mentions " + banned, lower.contains(banned));
        }
        assertTrue(text.contains("「」『』"));
    }

    @Test public void qualityCoreTextIsPinnedByDigest() {
        // changing a rule changes the digest recorded in every run; update the pin together with the rule
        assertEquals(64, QualityCore.editSha256().length());
        assertEquals(64, QualityCore.checkSha256().length());
        assertFalse(QualityCore.editSha256().equals(QualityCore.checkSha256()));
        assertEquals(QualityCore.editSha256(), QualityCore.editSha256());
        assertEquals(EXPECTED_RULES_SHA256, QualityCore.rulesSha256());
    }

    /** SHA-256 of the quality rules text (UTF-8). */
    static final String EXPECTED_RULES_SHA256 = "3b56e8c1794a29af3b7615d89cb678f66b5b96ca0c67d3ee6f9fade873bd4a53";

    @Test public void glossaryIsFilteredByRawOccurrenceWithoutALimit() {
        java.util.ArrayList<EditInputs.GlossaryEntry> many = new java.util.ArrayList<>();
        many.add(new EditInputs.GlossaryEntry("太郎", "Taro", "name", ""));
        many.add(new EditInputs.GlossaryEntry("花子", "Hanako", "name", "girl"));
        many.add(new EditInputs.GlossaryEntry("存在しない語", "Không có", "term", ""));
        for (int i = 0; i < 300; i++) many.add(new EditInputs.GlossaryEntry("赤い扉", "cánh cửa đỏ" + (i == 0 ? "" : i), "term", ""));
        List<EditInputs.GlossaryEntry> kept = ReferenceFilter.glossary(RAW, many);
        // 太郎, 花子 and the 300 distinct targets of the repeated source; the entry absent from RAW is dropped
        assertEquals(302, kept.size());
        ApiPrompt prompt = EditPromptBuilder.build(inputs(many, ""));
        assertEquals(302, prompt.glossaryEntries());
        assertTrue(prompt.system().contains("太郎 | Taro"));
        assertFalse(prompt.system().contains("存在しない語"));
    }

    @Test public void pronounRowsKeepBothFormatsAndDropRowsThatNameNothingInRaw() {
        String csv = "from,speaker,target,self,call,scope,note\n"
                + "太郎,太郎,花子,tôi,em,scene 1,close\n"
                + "次郎,次郎,三郎,anh,em,scene 2,absent\n"
                + "花子,花子,\"太郎, kun\",em,anh,scene 1,quoted comma\n"
                + "# a comment\n\n"
                + "花子,chị,legacy three columns\n";
        List<String> rows = ReferenceFilter.pronounRows(RAW, csv);
        assertEquals(3, rows.size());
        assertTrue(rows.get(0).startsWith("太郎,"));
        assertTrue(rows.get(1).contains("\"太郎, kun\""));
        assertEquals("花子,chị,legacy three columns", rows.get(2));
        ApiPrompt prompt = EditPromptBuilder.build(inputs(List.of(), csv));
        assertEquals(3, prompt.pronounRows());
        assertTrue(prompt.system().contains("# PRONOUNS"));
        assertFalse(prompt.system().contains("次郎"));
    }

    @Test public void withoutReferenceTheEditPromptSaysSoAndKeepsTheContract() {
        ApiPrompt prompt = EditPromptBuilder.build(inputs(List.of(), ""));
        assertTrue(prompt.system().contains("No glossary and no pronoun rows are supplied"));
        assertFalse(prompt.system().contains("# GLOSSARY"));
        assertFalse(prompt.system().contains("# PRONOUNS"));
        for (String tag : new String[] {"<EDITED>", "</EDITED>", "<NOTES>", "<WRONG_PAIR>"}) assertTrue(tag, prompt.system().contains(tag));
        assertTrue(prompt.user().startsWith("# RAW\n" + RAW));
        assertTrue(prompt.user().contains("# DRAFT\n" + DRAFT));
        assertEquals(QualityCore.editSha256(), prompt.qualityCoreSha256());
        // a reference that matches nothing in RAW reads as no reference
        ApiPrompt unmatched = EditPromptBuilder.build(inputs(List.of(new EditInputs.GlossaryEntry("無関係", "x", "", "")), "次郎,次郎,三郎,a,b,c,d"));
        assertTrue(unmatched.system().contains("No glossary and no pronoun rows are supplied"));
    }

    @Test public void checkPromptSeesRawFirstThenEditedAndTheChangedPassagesButNeverTheNotes() {
        String edited = "Taro mở cánh cửa đỏ.\nHanako nói: \"Cách năm trăm mét.\"\n";
        List<EditGuards.Flag> flags = List.of(new EditGuards.Flag(EditGuards.Code.SYMBOL_WARN, "「 draft 0, edited 1"));
        ApiPrompt prompt = CheckPromptBuilder.build(EditorialApiContract.Step.CHECK, RAW, DRAFT, edited, flags, "Vietnamese");
        String user = prompt.user();
        assertTrue(user.indexOf("# RAW") < user.indexOf("# EDITED"));
        assertTrue(user.indexOf("# EDITED") < user.indexOf("# CHANGED PASSAGES"));
        assertTrue(user.contains("before: Hanako nói: \"Cách năm mươi mét.\""));
        assertTrue(user.contains("after: Hanako nói: \"Cách năm trăm mét.\""));
        assertTrue(user.contains("SYMBOL_WARN"));
        assertFalse(user.contains("<NOTES>"));
        assertFalse(user.contains("# DRAFT"));
        assertTrue(prompt.system().contains("ANSWER FORMAT"));
        assertTrue(CheckPromptBuilder.build(EditorialApiContract.Step.CHECK, RAW, DRAFT, DRAFT, List.of(), "Vietnamese").user()
                .contains("(none: the edited text equals the draft)"));
    }

    @Test public void q1GuardRevertsAddedHanAndNormalizesFullwidthQuestion() {
        String raw = "原文 ?";
        String draft = "Bản dịch ?";
        EditGuards.Report report = EditGuards.check(raw, draft, "Bản dịch？ 三", List.of(), EditGuards.Config.defaults());
        assertEquals("Bản dịch ?", report.cleaned());
        assertTrue(report.has(EditGuards.Code.CONTENT_LEAK));
        assertTrue(report.has(EditGuards.Code.NORMALIZATION_APPLIED));
    }

    @Test public void q1EditPromptContainsDetectionsButNeverFinalReference() {
        ApiPrompt prompt = EditPromptBuilder.build(new EditInputs("日本語 ?", "Bản dịch ? あ", "Vietnamese", List.of(), ""));
        assertTrue(prompt.system().contains("APP DETECTIONS"));
        assertTrue(prompt.system().contains("UNTRANSLATED"));
        assertFalse(prompt.system().contains("FINAL"));
    }
}
