package com.ml.tblandroidtxt;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * D4 hermetic integration evidence for the real Mercedes Vol. 4 projections.
 *
 * The fixtures are classpath resources copied byte-for-byte from the released
 * CH001/CH004 inputs.  No test in this class reads the original D: drive.
 */
public class TranslationProfileIntegrationTest {
    private static final String P3_HEADER = "from,speaker,target,self,call,scope,note";
    private static final String GLOSSARY_HEADER = "source,target,category,note,priority";
    private static final String ARROW = "\u2192";

    private static final String CH001_RAW_SHA256 = "FE4CE02301E9EE5FD457A7A744C982B23EE3C95100CFDC5CBDF908B3979F40AF";
    private static final String CH001_GLOSSARY_SHA256 = "459FBF1037BC52FA756843BFF7364759D0460388A38F51D16662D8751ACC5E8C";
    private static final String CH001_PRONOUN_SHA256 = "1F71CDCBC8760022E171FB8303E58A312D1913EE157CF783350E62100D9829FB";
    private static final String CH004_RAW_SHA256 = "36FB90029B19360590344A6147E8EE53131BBAAF336196D7E7DC1D35B4BA3A4A";
    private static final String CH004_GLOSSARY_SHA256 = "5583CFCBDE99E3F3AC32191F9306D5377BD37A008D5EC7B8F47741B04E6DA332";
    private static final String CH004_PRONOUN_SHA256 = "B4A6A41CC31ABCDD48C246031368C68F31B5CF2517F9E23B7132B642C64F4648";

    @Test public void realMercedesFixturesAreByteStableAndParseTogether() throws Exception {
        assertFixture("ch001", "raw.txt", CH001_RAW_SHA256, false);
        assertFixture("ch001", "glossary.csv", CH001_GLOSSARY_SHA256, true);
        assertFixture("ch001", "pronoun.csv", CH001_PRONOUN_SHA256, true);
        assertFixture("ch004", "raw.txt", CH004_RAW_SHA256, false);
        assertFixture("ch004", "glossary.csv", CH004_GLOSSARY_SHA256, true);
        assertFixture("ch004", "pronoun.csv", CH004_PRONOUN_SHA256, true);

        assertChapterFixture("ch001", 53, 15, 1);
        assertChapterFixture("ch004", 154, 36, 6);
    }

    @Test public void ch001TranslationPromptCombinesGlossaryAndPronoun() {
        String raw = fixtureText("ch001", "raw.txt");
        String glossary = fixtureText("ch001", "glossary.csv");
        String pronoun = fixtureText("ch001", "pronoun.csv");
        Chunk chunk = findChunk(chunks(raw), "\u884c\u304f\u305e", 50);

        AppSettings settings = settings(glossary, pronoun);
        PromptPlan plan = PromptPlan.translation(chunk, "", settings);
        PromptContextBuilder.PronounRule rule = findPronoun(
                PromptContextBuilder.validate(glossary, pronoun), "\u884c\u304f\u305e");
        String expected = "Mercedes " + ARROW + " @GROUP_1: omit pronouns | " + rule.note;

        assertTrue("p050 must overlap the P3 scope", chunk.paragraphStart <= 50 && chunk.paragraphEnd >= 50);
        assertTrue("Glossary and P3 must share one PromptPlan", plan.glossaryLockCount > 0);
        assertEquals(1, plan.pronounLockCount);
        assertEquals(expected, plan.pronoun.trim());
        assertEquals(1, count(plan.pronoun, expected));
        assertTrue(plan.glossary.contains("Kuro") || plan.glossary.contains("Benkei")
                || plan.glossary.contains("Mercedes") || plan.glossary.contains("dungeon"));
        assertFalse(plan.pronoun.contains("行くぞ " + ARROW + " Mercedes: Basil"));
        assertFalse(plan.pronoun.contains("(omit)"));
        assertTrue(plan.prompt.system.contains(plan.glossary.trim()));
        assertTrue(plan.prompt.system.contains(plan.pronoun.trim()));
        assertEquals(1, count(plan.prompt.system, expected));
        System.out.println("D4_LOCK ch001 chunk=" + chunk.index + " range=p" + chunk.paragraphStart + "-p"
                + chunk.paragraphEnd + " glossaryLocks=" + plan.glossaryLockCount
                + " pronounLocks=" + plan.pronounLockCount + " line=" + expected);
        System.out.println("D4_GLOSSARY ch001=" + plan.glossary.replace("\n", " || "));
    }

    @Test public void ch004ScopedSelfAndCallReachPrompt() {
        String raw = fixtureText("ch004", "raw.txt");
        String glossary = fixtureText("ch004", "glossary.csv");
        String pronoun = fixtureText("ch004", "pronoun.csv");
        Chunk chunk = findChunk(chunks(raw), "\u79c1", 52);
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate(glossary, pronoun);
        PromptContextBuilder.PronounRule rule = findPronoun(report, "\u79c1");
        AppSettings settings = settings(glossary, pronoun);
        PromptPlan plan = PromptPlan.translation(chunk, "", settings);
        String expected = "Mercedes " + ARROW + " Basil: ta/ng\u01b0\u01a1i | " + rule.note;

        assertEquals("Mercedes", rule.speaker);
        assertEquals("Basil", rule.target);
        assertEquals("ta", rule.self);
        assertEquals("ng\u01b0\u01a1i", rule.call);
        assertEquals("p052-p153", rule.scope);
        assertTrue(chunk.paragraphStart <= 52 && chunk.paragraphEnd >= 52);
        assertEquals(1, count(plan.pronoun, expected));
        assertTrue(plan.pronoun.contains(expected));
        assertFalse(plan.pronoun.contains("\u79c1 " + ARROW + " Mercedes: Basil"));
        System.out.println("D4_LOCK ch004 chunk=" + chunk.index + " range=p" + chunk.paragraphStart + "-p"
                + chunk.paragraphEnd + " glossaryLocks=" + plan.glossaryLockCount
                + " pronounLocks=" + plan.pronounLockCount + " line=" + expected);
        System.out.println("D4_GLOSSARY ch004=" + plan.glossary.replace("\n", " || "));
    }

    @Test public void ch004ScopeDoesNotLeak() {
        String scoped = P3_HEADER + "\n"
                + "\u79c1,Mercedes,Basil,ta,ng\u01b0\u01a1i,p052-p153,scoped\n";
        assertEquals(1, buildAt(scoped, "\u79c1", 52, 52).pronounCount);
        assertEquals(1, buildAt(scoped, "\u79c1", 153, 153).pronounCount);
        assertEquals(0, buildAt(scoped, "\u79c1", 51, 51).pronounCount);
        assertEquals(0, buildAt(scoped, "\u79c1", 154, 154).pronounCount);
        assertEquals(0, buildAt(scoped, "Mercedes Basil", 52, 52).pronounCount);

        String p153 = P3_HEADER + "\n"
                + "\u304a\u524d\u9054,Mercedes,@GROUP_1,ta,\u5404\u3005,p153,group\n";
        assertEquals(1, buildAt(p153, "\u304a\u524d\u9054", 153, 153).pronounCount);
        assertEquals(0, buildAt(p153, "\u304a\u524d\u9054", 152, 152).pronounCount);

        String malformed = P3_HEADER + "\n"
                + "Bad,Mercedes,Basil,ta,ng\u01b0\u01a1i,CH004:p153-p052,malformed\n";
        PromptContextBuilder.ParseReport malformedReport = PromptContextBuilder.validate("", malformed);
        assertEquals(1, malformedReport.explicitPronouns.size());
        assertTrue(malformedReport.malformedPronounRows.toString().contains("invalid scope"));
        assertEquals(0, buildAt(malformed, "Bad", 100, 100).pronounCount);

        String star = P3_HEADER + "\n"
                + "\u6211\u3005,Basil,Mercedes,ch\u00fang t\u00f4i,ti\u1ec3u th\u01b0,*,star\n";
        assertEquals(1, buildAt(star, "\u6211\u3005", 1, 1).pronounCount);
        assertEquals(0, buildAt(star, "Mercedes", 1, 1).pronounCount);
    }

    @Test public void unmatchedRowsAndMetadataNeverReachPrompt() {
        String raw = fixtureText("ch004", "raw.txt");
        String glossary = fixtureText("ch004", "glossary.csv");
        String pronoun = fixtureText("ch004", "pronoun.csv");
        Chunk chunk = findChunk(chunks(raw), "\u79c1", 52);
        AppSettings base = settings(glossary, pronoun);
        PromptPlan baseline = PromptPlan.translation(chunk, "", base);

        StringBuilder extraGlossary = new StringBuilder(glossary);
        StringBuilder extraPronoun = new StringBuilder(pronoun);
        for (int i = 0; i < 250; i++) {
            extraGlossary.append("UNMATCHED_GLOSSARY_").append(i)
                    .append(",Unused").append(i).append(",term,")
                    .append("file/path priority scope CH004:p050,high\n");
            extraPronoun.append("UNMATCHED_CUE_").append(i)
                    .append(",FileSpeaker,FileTarget,ta,ng\u01b0\u01a1i,CH004:p050,")
                    .append("file/path priority\n");
        }
        AppSettings withUnmatched = settings(extraGlossary.toString(), extraPronoun.toString());
        PromptPlan expanded = PromptPlan.translation(chunk, "", withUnmatched);

        assertEquals(baseline.glossaryLockCount, expanded.glossaryLockCount);
        assertEquals(baseline.pronounLockCount, expanded.pronounLockCount);
        assertEquals(baseline.glossaryTokens, expanded.glossaryTokens);
        assertEquals(baseline.pronounTokens, expanded.pronounTokens);
        assertEquals(baseline.totalInputTokens, expanded.totalInputTokens);
        assertFalse(expanded.glossary.contains("source,target"));
        assertFalse(expanded.glossary.contains("priority"));
        assertFalse(expanded.glossary.contains("CH004:p050"));
        assertFalse(expanded.pronoun.contains("from,speaker"));
        assertFalse(expanded.pronoun.contains("CH004:p050"));
        assertFalse(expanded.pronoun.contains("FileSpeaker"));
        assertFalse(expanded.pronoun.contains("UNMATCHED_CUE"));
        assertFalse(expanded.pronoun.contains("file/path"));
        assertEquals(1, count(expanded.prompt.system, expanded.pronoun.trim()));
    }

    @Test public void previewBalancedFullAndRuntimeUseSameLocks() {
        String raw = fixtureText("ch001", "raw.txt");
        String glossary = fixtureText("ch001", "glossary.csv");
        String pronoun = fixtureText("ch001", "pronoun.csv");
        Chunk chunk = findChunk(chunks(raw), "\u884c\u304f\u305e", 50);
        AppSettings balanced = settings(glossary, pronoun);
        PromptPlan balancedPlan = PromptPlan.translation(chunk, "", balanced);

        String preview = PromptContextBuilder.preview(glossary, pronoun, chunk.mainContent,
                chunk.contextBefore + "\n" + chunk.mainContent,
                chunk.paragraphStart, chunk.paragraphEnd, balanced);
        for (String line : lines(balancedPlan.glossary)) {
            assertTrue(preview.contains(line));
            assertEquals(1, count(preview, line));
        }
        for (String line : lines(balancedPlan.pronoun)) {
            assertTrue(preview.contains(line));
            assertEquals(1, count(preview, line));
        }

        AppSettings full = balanced.copy();
        full.optimizationPreset = "full";
        PromptPlan fullPlan = PromptPlan.forTranslation(chunk, "", full);
        PromptPair fullPair = PromptBuilder.translationPrompt(chunk, "", full);
        assertEquals(fullPair.system, fullPlan.prompt.system);
        assertEquals(fullPair.user, fullPlan.prompt.user);
        assertEquals(balancedPlan.glossaryLockCount, fullPlan.glossaryLockCount);
        assertEquals(balancedPlan.pronounLockCount, fullPlan.pronounLockCount);
        assertEquals(balancedPlan.glossary, fullPlan.glossary);
        assertEquals(balancedPlan.pronoun, fullPlan.pronoun);
        for (String line : lines(fullPlan.glossary + fullPlan.pronoun)) {
            assertEquals(1, count(fullPlan.prompt.system, line));
        }

        CostEstimator.Estimate estimate = CostEstimator.estimatePreparedChunks(
                Arrays.asList(chunk), balanced);
        assertEquals(balancedPlan.glossaryTokens, estimate.breakdownGlossary);
        assertEquals(balancedPlan.pronounTokens, estimate.breakdownPronoun);
    }

    @Test public void settingsSnapshotRoundTripKeepsBothProfiles() {
        AppSettings settings = settings(fixtureText("ch004", "glossary.csv"),
                fixtureText("ch004", "pronoun.csv"));
        settings.optimizationPreset = "full";
        settings.refineAfter = true;
        settings.glossaryInjectLimit = 17;
        settings.pronounInjectLimit = 13;
        settings.translationInstructions = "Keep the established Mercedes voice.";
        settings.refinementInstructions = "Preserve scoped locks.";
        settings.selectedGlossaryId = "mercedes-ch004";
        settings.selectedPronounId = "mercedes-p3";
        String json = SettingsStore.toJson(settings);
        AppSettings restored = SettingsStore.fromJson(json);

        assertEquals(settings.glossaryText, restored.glossaryText);
        assertEquals(settings.pronounText, restored.pronounText);
        assertEquals(settings.optimizationPreset, restored.optimizationPreset);
        assertEquals(settings.refineAfter, restored.refineAfter);
        assertEquals(settings.glossaryInjectLimit, restored.glossaryInjectLimit);
        assertEquals(settings.pronounInjectLimit, restored.pronounInjectLimit);
        assertEquals(HashUtil.settingsHash(settings), HashUtil.settingsHash(restored));

        List<Chunk> chunks = chunks(fixtureText("ch004", "raw.txt"));
        PromptPlan beforeTranslation = PromptPlan.forTranslation(chunks.get(0), "", settings);
        PromptPlan afterTranslation = PromptPlan.forTranslation(chunks.get(0), "", restored);
        PromptPlan beforeRefinement = refinementPlan(chunks.get(0), "draft", settings);
        PromptPlan afterRefinement = refinementPlan(chunks.get(0), "draft", restored);
        assertPlanLocksEqual(beforeTranslation, afterTranslation);
        assertPlanLocksEqual(beforeRefinement, afterRefinement);

        CostEstimator.Estimate before = CostEstimator.estimatePreparedChunks(chunks, settings);
        CostEstimator.Estimate after = CostEstimator.estimatePreparedChunks(chunks, restored);
        assertEstimateEqual(before, after);
    }

    @Test public void reconstructedChunksReproduceRangesAndLocks() {
        String raw = fixtureText("ch004", "raw.txt");
        AppSettings settings = settings(fixtureText("ch004", "glossary.csv"),
                fixtureText("ch004", "pronoun.csv"));
        settings.refineAfter = true;
        List<Chunk> original = chunks(raw);
        ArrayList<Chunk> reconstructed = new ArrayList<>();
        for (Chunk c : original) {
            reconstructed.add(new Chunk(c.index, c.startOffset, c.endOffset,
                    c.contextBefore, c.mainContent, c.contextAfter, c.parentStableId));
        }
        Chunker.assignParagraphRanges(reconstructed);

        assertEquals(original.size(), reconstructed.size());
        for (int i = 0; i < original.size(); i++) {
            Chunk before = original.get(i);
            Chunk after = reconstructed.get(i);
            assertEquals(before.index, after.index);
            assertEquals(before.startOffset, after.startOffset);
            assertEquals(before.endOffset, after.endOffset);
            assertEquals(before.mainContent, after.mainContent);
            assertEquals(before.contextBefore, after.contextBefore);
            assertEquals(before.contextAfter, after.contextAfter);
            assertEquals(before.paragraphStart, after.paragraphStart);
            assertEquals(before.paragraphEnd, after.paragraphEnd);
            assertEquals(before.stableId, after.stableId);
            assertPlanLocksEqual(PromptPlan.forTranslation(before, "", settings),
                    PromptPlan.forTranslation(after, "", settings));
            assertPlanLocksEqual(refinementPlan(before, "draft", settings),
                    refinementPlan(after, "draft", settings));
        }
    }

    @Test public void refineAfterKeepsScopedPronounLocks() {
        String raw = fixtureText("ch004", "raw.txt");
        AppSettings settings = settings(fixtureText("ch004", "glossary.csv"),
                fixtureText("ch004", "pronoun.csv"));
        settings.refineAfter = true;
        Chunk chunk = findChunk(chunks(raw), "\u79c1", 52);
        String expected = "Mercedes " + ARROW + " Basil: ta/ng\u01b0\u01a1i | "
                + findPronoun(PromptContextBuilder.validate(settings.glossaryText, settings.pronounText), "\u79c1").note;

        PromptPlan translation = PromptPlan.translation(chunk, "", settings);
        PromptPlan refinement = refinementPlan(chunk, "draft", settings);
        assertEquals(1, count(translation.pronoun, expected));
        assertEquals(1, count(refinement.pronoun, expected));
        assertEquals(translation.pronounLockCount, refinement.pronounLockCount);
        assertTrue(refinement.prompt.system.contains(expected));

        AppSettings full = settings.copy();
        full.optimizationPreset = "full";
        PromptPlan fullTranslation = PromptPlan.forTranslation(chunk, "", full);
        PromptPlan fullRefinement = refinementPlan(chunk, "draft", full);
        assertEquals(1, count(fullTranslation.prompt.system, expected));
        assertEquals(1, count(fullRefinement.prompt.system, expected));

        CostEstimator.Estimate estimate = CostEstimator.estimatePreparedChunks(
                Arrays.asList(chunk), settings);
        assertEquals(translation.pronounTokens + refinement.pronounTokens,
                estimate.breakdownPronoun);
        assertEquals(translation.glossaryTokens + refinement.glossaryTokens,
                estimate.breakdownGlossary);

        assertNotNull(method(TranslationEngine.class, "refineWithRetry", long.class, Chunk.class,
                String.class, AppSettings.class, TranslationEngine.CancelChecker.class,
                TranslationEngine.UsageSink.class, TranslationEngine.EventSink.class));
        assertNotNull(declaredMethod(TranslatorService.class, "refineWithRetry", Chunk.class,
                String.class, AppSettings.class));
        assertNotNull(method(TranslationEngine.class, "refineWithRetry", String.class,
                String.class, AppSettings.class, TranslationEngine.CancelChecker.class,
                TranslationEngine.UsageSink.class, TranslationEngine.EventSink.class));
        System.out.println("D4_REFINE ch004 chunk=" + chunk.index + " range=p" + chunk.paragraphStart + "-p"
                + chunk.paragraphEnd + " translationPronounTokens=" + translation.pronounTokens
                + " refinementPronounTokens=" + refinement.pronounTokens
                + " estimatorPronounTokens=" + estimate.breakdownPronoun);
    }

    @Test public void compactCostContainsOnlySemanticPayload() {
        assertCostScenario("ch001", 53);
        assertCostScenario("ch004", 154);
    }

    @Test public void wholeChapterDryPreparationNeedsNoExternalWorkflow() {
        assertDryPreparation("ch001", 53);
        assertDryPreparation("ch004", 154);
    }

    private static void assertFixture(String chapter, String name, String expectedHash, boolean bomExpected) {
        byte[] bytes = fixtureBytes(chapter, name);
        assertEquals(expectedHash, HashUtil.sha256(bytes).toUpperCase(Locale.ROOT));
        if (bomExpected) {
            assertTrue(name + " must retain UTF-8 BOM", bytes.length >= 3);
            assertEquals(0xEF, bytes[0] & 0xFF);
            assertEquals(0xBB, bytes[1] & 0xFF);
            assertEquals(0xBF, bytes[2] & 0xFF);
        }
    }

    private static void assertChapterFixture(String chapter, int expectedParagraphs,
                                              int expectedGlossaryRows, int expectedPronounRows) {
        String raw = fixtureText(chapter, "raw.txt");
        String glossary = fixtureText(chapter, "glossary.csv");
        String pronoun = fixtureText(chapter, "pronoun.csv");
        assertEquals(expectedParagraphs, paragraphCount(raw));
        PromptContextBuilder.ParseReport parsed = PromptContextBuilder.validate(glossary, pronoun);
        assertEquals(expectedGlossaryRows, parsed.glossaryTerms.size());
        assertEquals(expectedPronounRows, parsed.explicitPronouns.size());
        assertTrue("No malformed Glossary rows: " + parsed.malformedGlossaryRows,
                parsed.malformedGlossaryRows.isEmpty());
        assertTrue("No malformed P3 rows: " + parsed.malformedPronounRows,
                parsed.malformedPronounRows.isEmpty());
        assertTrue("Glossary CSV header must retain BOM", glossary.startsWith("\uFEFF"));
        assertTrue("Pronoun CSV header must retain BOM", pronoun.startsWith("\uFEFF"));
        assertFalse(parsed.glossaryTerms.toString().contains("source,target"));
        assertFalse(parsed.explicitPronouns.toString().contains("from,speaker,target"));
        assertFalse(hasField(GlossaryStore.Term.class, "priority"));
        assertFalse(hasField(PromptContextBuilder.TermEntry.class, "priority"));
        assertTrue(glossary.contains("\u30e1\u30eb\u30bb\u30c7\u30b9")
                || glossary.contains("\u30af\u30ed"));
        assertTrue(glossary.contains("Mercedes") || glossary.contains("\u3067"));
        for (PromptContextBuilder.TermEntry entry : parsed.glossaryTerms) {
            assertNotNull(entry.note);
            assertFalse(entry.source.equals("source"));
        }
    }

    private static void assertCostScenario(String chapter, int expectedParagraphs) {
        String raw = fixtureText(chapter, "raw.txt");
        String glossary = fixtureText(chapter, "glossary.csv");
        String pronoun = fixtureText(chapter, "pronoun.csv");
        List<Chunk> chunks = chunks(raw);

        AppSettings noProfile = settings("", "");
        AppSettings noNote = settings(glossaryWithoutNotes(glossary), pronounWithoutNotes(pronoun));
        AppSettings fullNote = settings(glossary, pronoun);
        AppSettings noPriority = settings(glossaryWithoutPriority(glossary), pronoun);
        AppSettings unmatched = settings(glossary + unmatchedGlossaryRows(200),
                pronoun + unmatchedPronounRows(200));

        CostEstimator.Estimate empty = CostEstimator.estimatePreparedChunks(chunks, noProfile);
        CostEstimator.Estimate withoutNote = CostEstimator.estimatePreparedChunks(chunks, noNote);
        CostEstimator.Estimate full = CostEstimator.estimatePreparedChunks(chunks, fullNote);
        CostEstimator.Estimate priorityRemoved = CostEstimator.estimatePreparedChunks(chunks, noPriority);
        CostEstimator.Estimate extraUnmatched = CostEstimator.estimatePreparedChunks(chunks, unmatched);

        assertEquals(expectedParagraphs, lastParagraph(chunks));
        assertEquals(empty.sourceTokens, withoutNote.sourceTokens);
        assertEquals(empty.sourceTokens, full.sourceTokens);
        assertEquals(empty.outputTokensLow, full.outputTokensLow);
        assertEquals(empty.outputTokensHigh, full.outputTokensHigh);
        assertTrue(withoutNote.inputTokensLow > empty.inputTokensLow);
        assertTrue(full.inputTokensLow > withoutNote.inputTokensLow);
        assertTrue(full.breakdownGlossary >= withoutNote.breakdownGlossary);
        assertTrue(full.breakdownPronoun >= withoutNote.breakdownPronoun);
        assertEquals(full.inputTokensLow, priorityRemoved.inputTokensLow);
        assertEquals(full.inputTokensHigh, priorityRemoved.inputTokensHigh);
        assertEquals(full.breakdownGlossary, priorityRemoved.breakdownGlossary);
        assertEquals(full.breakdownPronoun, priorityRemoved.breakdownPronoun);
        assertEquals(full.inputTokensLow, extraUnmatched.inputTokensLow);
        assertEquals(full.inputTokensHigh, extraUnmatched.inputTokensHigh);
        assertEquals(full.breakdownGlossary, extraUnmatched.breakdownGlossary);
        assertEquals(full.breakdownPronoun, extraUnmatched.breakdownPronoun);

        int expectedGlossaryTokens = 0;
        int expectedPronounTokens = 0;
        int matchedGlossary = 0;
        int matchedPronoun = 0;
        for (Chunk chunk : chunks) {
            PromptPlan plan = PromptPlan.translation(chunk, "", fullNote);
            PromptPlan noNotePlan = PromptPlan.translation(chunk, "", noNote);
            assertEquals(stripNotes(noNotePlan.glossary), stripNotes(plan.glossary));
            assertEquals(stripNotes(noNotePlan.pronoun), stripNotes(plan.pronoun));
            assertFalse(plan.glossary.contains("priority"));
            assertFalse(plan.pronoun.contains("scope="));
            expectedGlossaryTokens += plan.glossaryTokens;
            expectedPronounTokens += plan.pronounTokens;
            matchedGlossary += plan.glossaryLockCount;
            matchedPronoun += plan.pronounLockCount;
        }
        assertEquals(expectedGlossaryTokens, full.breakdownGlossary);
        assertEquals(expectedPronounTokens, full.breakdownPronoun);

        System.out.println("D4_COST chapter=" + chapter
                + " chunks=" + full.chunks
                + " matchedGlossaryRows=" + matchedGlossary
                + " matchedPronounRows=" + matchedPronoun
                + " glossaryTokens=" + full.breakdownGlossary
                + " pronounTokens=" + full.breakdownPronoun
                + " inputLow=" + full.inputTokensLow
                + " inputHigh=" + full.inputTokensHigh
                + " deltaNoProfile=" + (full.inputTokensLow - empty.inputTokensLow)
                + " deltaNoNote=" + (full.inputTokensLow - withoutNote.inputTokensLow));
        printCostScenario(chapter, "no-profile", chunks, noProfile, empty);
        printCostScenario(chapter, "no-note", chunks, noNote, withoutNote);
        printCostScenario(chapter, "full-note", chunks, fullNote, full);
    }

    private static void assertDryPreparation(String chapter, int expectedParagraphs) {
        String raw = fixtureText(chapter, "raw.txt");
        AppSettings settings = settings(fixtureText(chapter, "glossary.csv"),
                fixtureText(chapter, "pronoun.csv"));
        settings.refineAfter = true;
        List<Chunk> chunks = chunks(raw);
        assertTrue(Chunker.verifyCoverage(Chunker.normalizeSource(raw), chunks).valid);
        assertEquals(expectedParagraphs, lastParagraph(chunks));
        int previousEnd = 0;
        for (Chunk chunk : chunks) {
            assertTrue(chunk.paragraphStart >= 1);
            assertTrue(chunk.paragraphEnd >= chunk.paragraphStart);
            assertTrue(chunk.paragraphStart <= previousEnd + 1);
            previousEnd = Math.max(previousEnd, chunk.paragraphEnd);
            assertNotNull(PromptPlan.translation(chunk, "", settings));
            assertNotNull(PromptPlan.forTranslation(chunk, "", full(settings)));
            assertNotNull(refinementPlan(chunk, "draft", settings));
            assertNotNull(refinementPlan(chunk, "draft", full(settings)));
        }
        assertEquals(expectedParagraphs, previousEnd);
        CostEstimator.Estimate estimate = CostEstimator.estimatePreparedChunks(chunks, settings);
        assertEquals(chunks.size(), estimate.chunks);
        assertTrue(estimate.inputTokensLow > 0);
        System.out.println("D4_DRY chapter=" + chapter + " paragraphs=" + expectedParagraphs
                + " chunks=" + chunks.size() + " inputLow=" + estimate.inputTokensLow
                + " inputHigh=" + estimate.inputTokensHigh);
    }

    private static void printCostScenario(String chapter, String scenario, List<Chunk> chunks,
                                          AppSettings settings, CostEstimator.Estimate estimate) {
        int glossaryRows = 0;
        int pronounRows = 0;
        for (Chunk chunk : chunks) {
            PromptPlan plan = PromptPlan.translation(chunk, "", settings);
            glossaryRows += plan.glossaryLockCount;
            pronounRows += plan.pronounLockCount;
        }
        System.out.println("D4_COST_SCENARIO chapter=" + chapter + " scenario=" + scenario
                + " chunks=" + estimate.chunks + " matchedGlossaryRows=" + glossaryRows
                + " matchedPronounRows=" + pronounRows + " glossaryTokens=" + estimate.breakdownGlossary
                + " pronounTokens=" + estimate.breakdownPronoun + " inputLow=" + estimate.inputTokensLow
                + " inputHigh=" + estimate.inputTokensHigh);
    }

    private static PromptContextBuilder.ContextBlock buildAt(String pronoun, String chunk,
                                                              int start, int end) {
        AppSettings settings = new AppSettings();
        return PromptContextBuilder.build("", pronoun, chunk, start, end, settings);
    }

    private static PromptContextBuilder.PronounRule findPronoun(
            PromptContextBuilder.ParseReport report, String from) {
        for (PromptContextBuilder.PronounRule rule : report.explicitPronouns) {
            if (from.equals(rule.from)) return rule;
        }
        fail("Missing P3 rule for cue " + from);
        return null;
    }

    private static Chunk findChunk(List<Chunk> chunks, String cue, int paragraph) {
        for (Chunk chunk : chunks) {
            if (chunk.mainContent.contains(cue)
                    && chunk.paragraphStart <= paragraph && chunk.paragraphEnd >= paragraph) return chunk;
        }
        StringBuilder ranges = new StringBuilder();
        for (Chunk chunk : chunks) ranges.append('[').append(chunk.paragraphStart)
                .append('-').append(chunk.paragraphEnd).append(']');
        fail("Missing real chunk for cue " + cue + " paragraph " + paragraph + " ranges=" + ranges);
        return null;
    }

    private static List<Chunk> chunks(String raw) {
        AppSettings settings = new AppSettings();
        settings.contextOverlapEnabled = false;
        return Chunker.chunkText(raw, settings);
    }

    private static AppSettings settings(String glossary, String pronoun) {
        AppSettings settings = new AppSettings();
        settings.glossaryText = glossary == null ? "" : glossary;
        settings.pronounText = pronoun == null ? "" : pronoun;
        settings.contextOverlapEnabled = false;
        return settings;
    }

    private static AppSettings full(AppSettings original) {
        AppSettings full = original.copy();
        full.optimizationPreset = "full";
        return full;
    }

    private static PromptPlan refinementPlan(Chunk chunk, String draft, AppSettings settings) {
        try {
            Method method = PromptPlan.class.getMethod("forRefinement", Chunk.class,
                    String.class, AppSettings.class);
            return (PromptPlan) method.invoke(null, chunk, draft, settings);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            fail("Range-aware refinement failed: " + cause);
        } catch (Exception e) {
            fail("Missing range-aware PromptPlan.forRefinement(Chunk, String, AppSettings): " + e);
        }
        return null;
    }

    private static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException missing) {
            fail("Missing method " + type.getSimpleName() + "." + name);
            return null;
        }
    }

    private static Method declaredMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException missing) {
            fail("Missing declared method " + type.getSimpleName() + "." + name);
            return null;
        }
    }

    private static void assertPlanLocksEqual(PromptPlan before, PromptPlan after) {
        assertEquals(before.glossary, after.glossary);
        assertEquals(before.pronoun, after.pronoun);
        assertEquals(before.glossaryLockCount, after.glossaryLockCount);
        assertEquals(before.pronounLockCount, after.pronounLockCount);
        assertEquals(before.glossaryTokens, after.glossaryTokens);
        assertEquals(before.pronounTokens, after.pronounTokens);
        assertEquals(before.totalInputTokens, after.totalInputTokens);
    }

    private static void assertEstimateEqual(CostEstimator.Estimate before,
                                             CostEstimator.Estimate after) {
        assertEquals(before.sourceTokens, after.sourceTokens);
        assertEquals(before.chunks, after.chunks);
        assertEquals(before.inputTokensLow, after.inputTokensLow);
        assertEquals(before.inputTokensHigh, after.inputTokensHigh);
        assertEquals(before.outputTokensLow, after.outputTokensLow);
        assertEquals(before.outputTokensHigh, after.outputTokensHigh);
        assertEquals(before.totalTokensLow, after.totalTokensLow);
        assertEquals(before.totalTokensHigh, after.totalTokensHigh);
        assertEquals(before.breakdownSource, after.breakdownSource);
        assertEquals(before.breakdownInstruction, after.breakdownInstruction);
        assertEquals(before.breakdownGlossary, after.breakdownGlossary);
        assertEquals(before.breakdownPronoun, after.breakdownPronoun);
        assertEquals(before.breakdownContext, after.breakdownContext);
    }

    private static String glossaryWithoutNotes(String raw) {
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate(raw, "");
        StringBuilder out = new StringBuilder("source,target,category,note\n");
        for (PromptContextBuilder.TermEntry entry : report.glossaryTerms) {
            appendCsv(out, entry.source, entry.target, entry.category, "");
        }
        return out.toString();
    }

    private static String glossaryWithoutPriority(String raw) {
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate(raw, "");
        StringBuilder out = new StringBuilder("source,target,category,note\n");
        for (PromptContextBuilder.TermEntry entry : report.glossaryTerms) {
            appendCsv(out, entry.source, entry.target, entry.category, entry.note);
        }
        return out.toString();
    }

    private static String pronounWithoutNotes(String raw) {
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate("", raw);
        StringBuilder out = new StringBuilder(P3_HEADER).append('\n');
        for (PromptContextBuilder.PronounRule rule : report.explicitPronouns) {
            appendCsv(out, rule.from, rule.speaker, rule.target, rule.self,
                    rule.call, rule.scope, "");
        }
        return out.toString();
    }

    private static void appendCsv(StringBuilder out, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            String value = values[i] == null ? "" : values[i];
            if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
                out.append('"').append(value.replace("\"", "\"\"")).append('"');
            } else out.append(value);
        }
        out.append('\n');
    }

    private static String unmatchedGlossaryRows(int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            out.append("NO_MATCH_GLOSSARY_").append(i).append(",Unused").append(i)
                    .append(",term,Unmatched note ").append(i).append(",low\n");
        }
        return out.toString();
    }

    private static String unmatchedPronounRows(int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            out.append("NO_MATCH_PRONOUN_").append(i)
                    .append(",Speaker,Target,ta,ng\u01b0\u01a1i,p001,Unmatched note ")
                    .append(i).append('\n');
        }
        return out.toString();
    }

    private static String stripNotes(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : lines(text)) {
            int separator = line.indexOf(" | ");
            if (separator >= 0) line = line.substring(0, separator);
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static List<String> lines(String text) {
        ArrayList<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\\r?\\n")) if (!line.trim().isEmpty()) out.add(line.trim());
        return out;
    }

    private static int lastParagraph(List<Chunk> chunks) {
        int last = 0;
        for (Chunk chunk : chunks) last = Math.max(last, chunk.paragraphEnd);
        return last;
    }

    private static int paragraphCount(String raw) {
        String normalized = Chunker.normalizeSource(raw);
        int count = 0;
        boolean inParagraph = false;
        for (String line : normalized.split("\\n", -1)) {
            if (line.trim().isEmpty()) {
                inParagraph = false;
            } else if (!inParagraph) {
                count++;
                inParagraph = true;
            }
        }
        return count;
    }

    private static int count(String text, String needle) {
        if (text == null || needle == null || needle.isEmpty()) return 0;
        int count = 0;
        int from = 0;
        while (true) {
            int at = text.indexOf(needle, from);
            if (at < 0) return count;
            count++;
            from = at + Math.max(1, needle.length());
        }
    }

    private static boolean hasField(Class<?> type, String name) {
        try {
            type.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException absent) {
            return false;
        }
    }

    private static String fixtureText(String chapter, String name) {
        return new String(fixtureBytes(chapter, name), StandardCharsets.UTF_8);
    }

    private static byte[] fixtureBytes(String chapter, String name) {
        String resource = "fixtures/v417/d4/mercedes-vol4/" + chapter + "/" + name;
        InputStream in = TranslationProfileIntegrationTest.class.getClassLoader()
                .getResourceAsStream(resource);
        assertNotNull("Missing hermetic fixture " + resource, in);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
            return out.toByteArray();
        } catch (Exception e) {
            fail("Cannot read fixture " + resource + ": " + e);
            return new byte[0];
        } finally {
            try { in.close(); } catch (Exception ignored) {}
        }
    }
}
