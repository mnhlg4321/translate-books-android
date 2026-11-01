package com.ml.tblandroidtxt;

import org.junit.Test;

import static org.junit.Assert.*;

public class TranslationConfigStateTest {
    @Test public void allThreeConfigsShowNotUsedWhenEmpty() {
        assertEquals(TranslationConfigState.Status.NOT_USED,
                TranslationConfigState.glossary("", 0, 0).status);
        assertEquals(TranslationConfigState.Status.NOT_USED,
                TranslationConfigState.pronoun("", 0, 0).status);
        assertEquals("Không sử dụng",
                TranslationConfigState.instruction("", false, false, false, "").statusLabel());
    }

    @Test public void selectedGlossaryAndPronounExposeCountsAndWarnings() {
        TranslationConfigState.Item glossary = TranslationConfigState.glossary("names.csv", 42, 1);
        TranslationConfigState.Item pronoun = TranslationConfigState.pronoun("pronouns.csv", 7, 0);
        assertEquals(TranslationConfigState.Status.WARNING, glossary.status);
        assertTrue(glossary.displayText().contains("42 mục"));
        assertEquals(TranslationConfigState.Status.VALID, pronoun.status);
        assertTrue(pronoun.displayText().contains("7 quy tắc"));
    }

    @Test public void translationOnlyInstructionIsValid() {
        TranslationConfigState.Item item = TranslationConfigState.instruction(
                "novel.yaml", true, false, false, "");
        assertTrue(item.isValidForStart());
        assertTrue(item.displayText().contains("translation"));
    }

    @Test public void refinementOnlyInstructionExplainsDisabledRefinement() {
        TranslationConfigState.Item item = TranslationConfigState.instruction(
                "refine.yaml", false, true, false, "");
        assertEquals(TranslationConfigState.Status.VALID, item.status);
        assertTrue(item.displayText().contains("refinement chưa bật"));
    }

    @Test public void unreadableOrUnrecognizedInstructionIsInvalid() {
        assertEquals(TranslationConfigState.Status.INVALID,
                TranslationConfigState.instruction("missing.yaml", true, false, false, "Mất quyền đọc").status);
        assertEquals(TranslationConfigState.Status.INVALID,
                TranslationConfigState.instruction("", false, false, false, "Mất quyền đọc").status);
        assertEquals(TranslationConfigState.Status.INVALID,
                TranslationConfigState.instruction("empty.yaml", false, false, false, "").status);
    }

    @Test public void activeJobSnapshotWinsOverEditableSettings() {
        AppSettings editable = new AppSettings();
        editable.instructionName = "next-job.yaml";
        AppSettings activeJob = new AppSettings();
        activeJob.instructionName = "running-job.yaml";
        assertSame(activeJob, TranslationConfigState.selectSettings(editable, activeJob, true));
        assertSame(editable, TranslationConfigState.selectSettings(editable, activeJob, false));
    }

    @Test public void yamlParserCoversTranslationRefinementAndInvalidInput() {
        CustomInstructions translation = YamlInstructionParser.parse("a.yaml", "translation: |-\n  Translate naturally");
        CustomInstructions refinement = YamlInstructionParser.parse("b.yaml", "refinement: |-\n  Polish the draft");
        CustomInstructions invalid = YamlInstructionParser.parse("c.yaml", "unrelated: true");
        assertTrue(translation.hasTranslation());
        assertTrue(refinement.hasRefinement());
        assertFalse(invalid.hasTranslation());
        assertFalse(invalid.hasRefinement());
    }

    @Test public void nothingChosenSaysNotUsedOnlyOnce() {
        assertEquals("Không sử dụng", TranslationConfigState.glossary("", 0, 0).displayText());
        assertEquals("Không sử dụng", TranslationConfigState.pronoun("", 0, 0).displayText());
        assertTrue(TranslationConfigState.glossary("x.csv", 3, 0).displayText().contains("\n"));
    }

    @Test public void compactSummaryShowsSelectedNamesCountsAndYamlStatusWithoutEditLabel() {
        TranslationConfigState state = new TranslationConfigState(
                TranslationConfigState.glossary("001_GLOSSARY_KUMO_VOL13", 51, 0),
                TranslationConfigState.pronoun("001_PRONOUN_KUMO_VOL13", 28, 0),
                TranslationConfigState.instruction("001_PROFILE.yaml", true, false, false, ""),
                false, 0);
        assertEquals("Glossary: 001_GLOSSARY_KUMO_VOL13 (51) · Pronoun: 001_PRONOUN_KUMO_VOL13 (28) · YAML: 001_PROFILE.yaml ✓",
                state.compactSummary());
        assertFalse(state.compactSummary().contains("· Sửa"));
    }

    @Test public void compactSummaryNamesUnusedItemsAndKeepsChapterNumberWhenShortened() {
        TranslationConfigState unused = new TranslationConfigState(
                TranslationConfigState.glossary("", 0, 0),
                TranslationConfigState.pronoun("", 0, 0),
                TranslationConfigState.instruction("", false, false, false, ""), false, 0);
        assertEquals("Glossary: Không dùng · Pronoun: Không dùng · YAML: Không dùng", unused.compactSummary());

        TranslationConfigState longName = new TranslationConfigState(
                TranslationConfigState.glossary("QA_001_GLOSSARY_FOR_A_LONG_COLLECTION_NAME", 51, 0),
                TranslationConfigState.pronoun("", 0, 0),
                TranslationConfigState.instruction("", false, false, false, ""), false, 0);
        String summary = longName.compactSummary();
        assertTrue(summary.contains("001"));
        assertTrue(summary.contains("(51)"));
        assertTrue(summary.length() < 100);
    }

    @Test public void compactSummaryKeepsChapterSetConfigurationNote() {
        TranslationConfigState state = new TranslationConfigState(
                TranslationConfigState.glossary("", 0, 0),
                TranslationConfigState.pronoun("", 0, 0),
                TranslationConfigState.instruction("", false, false, false, ""), false, 0);
        assertTrue(state.compactSummary(true).contains("theo từng chương của tập"));
    }
}
