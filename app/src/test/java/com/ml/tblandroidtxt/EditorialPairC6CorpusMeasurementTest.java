package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.AddressChecklist;
import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairText;
import com.ml.tblandroidtxt.editorial.api.RawAlignedNormalizer;
import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan;
import com.ml.tblandroidtxt.editorial.api.chunk.LineAligner;
import com.ml.tblandroidtxt.editorial.api.chunk.LineUnits;
import com.ml.tblandroidtxt.editorial.api.fix.FixPoint;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Opt-in, private-corpus measurement. It reads book files but emits counts only. */
public final class EditorialPairC6CorpusMeasurementTest {
    private static final String[] CHAPTERS = {"004", "005", "006", "007", "008", "011", "014", "017"};
    private static final FixPoint.Type[] EDIT_TYPES = {FixPoint.Type.ADDRESS, FixPoint.Type.GLOSSARY, FixPoint.Type.KANA};

    private enum FinalState { SAME, CHANGED, AMBIGUOUS }

    private record PrivateChapter006(Path draftPath, String raw, String draft, String glossaryText, String pronoun,
                                     List<EditInputs.GlossaryEntry> glossary, EditorialPairSourceLoader.FilesLoad load,
                                     EditorialPairPreview preview) { }

    private record CorrectionTarget(int rowIndex, int pointId, int physicalLine, String newLine) { }

    private record FakeExecution(FakeEditorialApiProvider provider, EditorialPairModels.PairRun run,
                                 EditorialPairRunService.ExportPlan output, EditorialPairPreview preview) { }

    private static final EditorialApiRunService.Pricing FREE_FAKE_PRICING = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return BigDecimal.ZERO; }
        @Override public BigDecimal outputPerToken(String model) { return BigDecimal.ZERO; }
    };

    private static final class Counts {
        final EnumMap<FixPoint.Type, Integer> points = zeroes();
        final EnumMap<FixPoint.Type, Integer> ownerChanged = zeroes();
        final EnumMap<FixPoint.Type, Integer> covered = zeroes();
        final EnumMap<FixPoint.Type, Integer> finalUnchanged = zeroes();
        final EnumMap<FixPoint.Type, Integer> pointComparable = zeroes();
        final EnumMap<FixPoint.Type, Integer> pointAmbiguous = zeroes();
        int chunks;
        int calledChunks;
        int inputTokens;
        int outputTokens;
        int ownerEditAmbiguous;
        int missingRawBeads;
    }

    @Test public void measurePrivateCorpusWhenConfigured() throws Exception {
        String inputRoot = System.getenv("C6_INPUT_ROOT");
        String finalRoot = System.getenv("C6_FINAL_ROOT");
        if (inputRoot == null || inputRoot.isBlank() || finalRoot == null || finalRoot.isBlank()) return;

        Path inputs = Path.of(inputRoot);
        Path finals = Path.of(finalRoot);
        assertTrue("C6 input root must exist", Files.isDirectory(inputs));
        assertTrue("C6 FINAL root must exist", Files.isDirectory(finals));

        AppSettings settings = new AppSettings();
        settings.provider = "openrouter";
        settings.model = "openai/gpt-5.6-luna";
        settings.optimizationPreset = "balanced";
        settings.chunkMode = "token";
        settings.maxTokensPerChunk = 450;
        settings.softLimitRatio = 0.8f;
        settings.maxOutputTokens = 4096;
        settings.contextChars = 400;

        Counts all = new Counts();
        for (String chapter : CHAPTERS) {
            Path inputDir = inputs.resolve(chapter);
            assertTrue("missing chapter input: " + chapter, Files.isDirectory(inputDir));
            String raw = readUtf8(required(inputDir, chapter + "_RAW_JAKUAKU_MONSTER_VOL1.txt"));
            Path draftPath = findOne(inputDir, p -> p.getFileName().toString().startsWith(chapter + "_")
                    && (p.getFileName().toString().endsWith("_DRAFT.txt") || p.getFileName().toString().endsWith("_translated.txt")));
            Path glossaryPath = required(inputDir, chapter + "_JAKUAKU_MONSTER_VOL1_chapter_glossary.csv");
            Path pronounPath = required(inputDir, chapter + "_PRONOUN_JAKUAKU_MONSTER_VOL1.csv");
            Path finalPath = required(finals, chapter + "_FINAL_QA_JAKUAKU_MONSTER_VOL1.txt");
            String draft = readUtf8(draftPath);
            String glossaryCsv = readUtf8(glossaryPath);
            List<GlossaryStore.Term> parsedTerms = GlossaryStore.parseTerms(glossaryPath.getFileName().toString(), glossaryCsv);
            List<EditInputs.GlossaryEntry> glossary = parsedTerms.stream()
                    .map(t -> new EditInputs.GlossaryEntry(t.source, t.target, t.category, t.note)).toList();
            String glossaryText = EditorialApiSources.glossaryAsText(glossary);
            String pronoun = readUtf8(pronounPath);
            String finalText = readUtf8(finalPath);

            EditorialPairSourceLoader.References refs = new EditorialPairSourceLoader.References(
                    glossaryText, glossaryPath.getFileName().toString(), pronoun, pronounPath.getFileName().toString());
            EditorialPairSourceLoader.FilesLoad load = EditorialPairSourceLoader.planFiles(chapter, raw, draft, refs, settings);
            assertNotNull("plan missing: " + chapter, load.plan);
            assertFalse("blocked source plan: " + chapter, load.plan.blocked());
            EditorialPairPreview preview = EditorialPairPreview.of(load.source, glossary, pronoun,
                    EditorialPairSnapshot.cues("from,speaker,target"));
            assertTrue("preview blocked for " + chapter + ": " + String.join(",", preview.blockers), preview.runnable());

            measureChapter(chapter, preview, load.plan, raw, draft, glossary, finalText, all);
        }

        printSummary(all);
        assertEquals("all eight requested chapters measured", 8, CHAPTERS.length);
        assertTrue("all planned chunks are counted", all.chunks > 0);
    }

    @Test public void fakeChapter006KeepsEveryPointByteExactAndEditsOnlyOneSelectedLineWhenConfigured() throws Exception {
        String inputRoot = System.getenv("C6_INPUT_ROOT");
        if (!"YES".equalsIgnoreCase(System.getenv("C6_FAKE_006")) || inputRoot == null || inputRoot.isBlank()) return;
        PrivateChapter006 chapter = loadPrivate006(Path.of(inputRoot));
        CorrectionTarget target = firstSafeTarget(chapter);
        assertNotNull("006 needs one eligible non-insertion point for edit-isolation test", target);

        FakeExecution noOp = executeFake(chapter, null);
        assertTrue("no-op run must be complete", noOp.output != null && noOp.output.complete);
        assertEquals("only flagged chunks may call the fake provider", chapter.preview.chunksWithFixPoints, noOp.provider.requests.size());
        assertEquals("fake responses settle at zero", 0, noOp.run.usd.compareTo(BigDecimal.ZERO));
        byte[] sourceBytes = Files.readAllBytes(chapter.draftPath);
        assertTrue("no-op fake output must equal source DRAFT byte for byte", java.util.Arrays.equals(sourceBytes,
                noOp.output.text.getBytes(StandardCharsets.UTF_8)));
        assertNoPointChunksWereDispatched(chapter.preview, noOp.provider.requests.size());
        assertEachCallSendsTheOutputFloor(noOp.provider);

        FakeExecution oneEdit = executeFake(chapter, target);
        assertTrue("one-edit run must produce an exportable candidate", oneEdit.output != null && oneEdit.output.complete);
        assertEquals("one-edit run still calls only flagged chunks", chapter.preview.chunksWithFixPoints, oneEdit.provider.requests.size());
        assertEquals("one-edit fake responses settle at zero", 0, oneEdit.run.usd.compareTo(BigDecimal.ZERO));
        String expected = replacePhysicalLine(chapter.draft, target.physicalLine, target.newLine);
        assertTrue("only the selected fix-point line may change", java.util.Arrays.equals(expected.getBytes(StandardCharsets.UTF_8),
                oneEdit.output.text.getBytes(StandardCharsets.UTF_8)));
        assertNoPointChunksWereDispatched(chapter.preview, oneEdit.provider.requests.size());
        assertEachCallSendsTheOutputFloor(oneEdit.provider);
        System.out.printf(java.util.Locale.ROOT,
                "C6 fake006 noOpCalls=%d oneEditCalls=%d points=%d noPointChunks=%d byteExact=true oneTargetLine=true usd=0%n",
                noOp.provider.requests.size(), oneEdit.provider.requests.size(), chapter.preview.fixPointCount,
                chapter.preview.rows.size() - chapter.preview.chunksWithFixPoints);
    }

    /** C6.6: a reasoning model must get at least the shared 4096-token output ceiling on every targeted call. */
    private static void assertEachCallSendsTheOutputFloor(FakeEditorialApiProvider provider) {
        assertEquals("one max_tokens value per call", provider.requests.size(), provider.maxOutputTokens.size());
        for (int maxTokens : provider.maxOutputTokens) {
            assertTrue("max_tokens " + maxTokens + " is below the 4096 floor", maxTokens >= 4096);
        }
    }

    private static PrivateChapter006 loadPrivate006(Path inputRoot) throws Exception {
        Path inputDir = inputRoot.resolve("006");
        String raw = readUtf8(required(inputDir, "006_RAW_JAKUAKU_MONSTER_VOL1.txt"));
        Path draftPath = required(inputDir, "006_JAKUAKU_MONSTER_VOL1_DRAFT.txt");
        String draft = readUtf8(draftPath);
        Path glossaryPath = required(inputDir, "006_JAKUAKU_MONSTER_VOL1_chapter_glossary.csv");
        List<GlossaryStore.Term> terms = GlossaryStore.parseTerms(glossaryPath.getFileName().toString(), readUtf8(glossaryPath));
        List<EditInputs.GlossaryEntry> glossary = terms.stream()
                .map(t -> new EditInputs.GlossaryEntry(t.source, t.target, t.category, t.note)).toList();
        String glossaryText = EditorialApiSources.glossaryAsText(glossary);
        Path pronounPath = required(inputDir, "006_PRONOUN_JAKUAKU_MONSTER_VOL1.csv");
        String pronoun = readUtf8(pronounPath);
        AppSettings settings = c6Settings();
        EditorialPairSourceLoader.References refs = new EditorialPairSourceLoader.References(
                glossaryText, glossaryPath.getFileName().toString(), pronoun, pronounPath.getFileName().toString());
        EditorialPairSourceLoader.FilesLoad load = EditorialPairSourceLoader.planFiles(
                "006 private fake fixture", raw, draft, refs, settings);
        assertNotNull("006 plan missing", load.plan);
        assertFalse("006 plan is blocked", load.plan.blocked());
        EditorialPairPreview preview = EditorialPairPreview.of(load.source, glossary, pronoun,
                EditorialPairSnapshot.cues("from,speaker,target"));
        assertTrue("006 preview is blocked", preview.runnable());
        return new PrivateChapter006(draftPath, raw, draft, glossaryText, pronoun, glossary, load, preview);
    }

    private static AppSettings c6Settings() {
        AppSettings settings = new AppSettings();
        settings.provider = "openrouter";
        settings.model = "openai/gpt-5.6-luna";
        settings.optimizationPreset = "balanced";
        settings.chunkMode = "token";
        settings.maxTokensPerChunk = 450;
        settings.softLimitRatio = 0.8f;
        settings.maxOutputTokens = 4096;
        settings.contextChars = 400;
        return settings;
    }

    private static FakeExecution executeFake(PrivateChapter006 chapter, CorrectionTarget target) throws Exception {
        List<Integer> activeRows = new ArrayList<>();
        for (int i = 0; i < chapter.preview.rows.size(); i++) {
            if (chapter.preview.rows.get(i).fixPointCount > 0) activeRows.add(i);
        }
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, callIndex) -> {
            if (callIndex < 0 || callIndex >= activeRows.size()) throw new IllegalStateException("C6_FAKE_CALL_INDEX_INVALID");
            int rowIndex = activeRows.get(callIndex);
            int selectedId = target != null && target.rowIndex == rowIndex ? target.pointId : -1;
            String answer = target == null ? EditorialPairTestData.targetedAnswer(request, callIndex, (i, d) -> "=")
                    : targetedAnswerForPoint(request, selectedId, target.newLine);
            return new com.ml.tblandroidtxt.editorial.api.EditorialApiFlow.StepResponse(answer, "stop", 0, 0,
                    BigDecimal.ZERO, true, "fake-model", "fake-route", "");
        });
        EditorialPairRunService service = new EditorialPairRunService(new InMemoryPairRunStore(), fake, FREE_FAKE_PRICING, 60_000L);
        EditorialPairModels.PairRun prepared = service.prepare(1, chapter.load.source, chapter.glossaryText, chapter.pronoun,
                null, "fake-model", "Vietnamese", new BigDecimal("1.00"), EditorialPairModels.ARM_CHUNK);
        EditorialPairModels.PairRun run = service.execute(prepared.id, null);
        return new FakeExecution(fake, run, service.exportPlan(run.id), chapter.preview);
    }

    private static String targetedAnswerForPoint(com.ml.tblandroidtxt.editorial.api.EditorialApiFlow.Request request,
                                                  int selectedId, String replacement) {
        String user = request.prompt().user();
        int start = user.indexOf("# NUMBERED FIX POINTS\n");
        if (start < 0) throw new IllegalStateException("C6_FAKE_TARGETED_PROMPT_MISSING");
        String[] lines = user.substring(start).split("\n");
        Pattern header = Pattern.compile("^\\[(\\d+)] TYPES: .*$");
        StringBuilder answer = new StringBuilder();
        int returned = 0;
        for (int i = 1; i < lines.length; i++) {
            Matcher id = header.matcher(lines[i]);
            if (!id.matches()) continue;
            int numericId = Integer.parseInt(id.group(1));
            if (answer.length() > 0) answer.append('\n');
            answer.append('[').append(numericId).append("] ").append(numericId == selectedId ? replacement : "=");
            returned++;
        }
        if (returned == 0) throw new IllegalStateException("C6_FAKE_NO_TARGET_LINES");
        return answer.toString();
    }

    private static CorrectionTarget firstSafeTarget(PrivateChapter006 chapter) {
        LineUnits draft = LineUnits.parse(chapter.draft);
        for (int rowIndex = 0; rowIndex < chapter.preview.rows.size(); rowIndex++) {
            EditorialPairPreview.Row row = chapter.preview.rows.get(rowIndex);
            int globalOffset = chapter.load.plan.chunks.get(rowIndex).draftFrom();
            for (FixPoint point : row.fixPoints) {
                if (point.insertion() || point.draftText().isBlank() || PairText.letters(point.draftText()) == 0) continue;
                // A punctuation-only fake response proves target isolation without asserting semantic quality.
                String replacement = point.draftText() + "!";
                int globalUnit = globalOffset + point.draftLineIndex();
                if (globalUnit < 0 || globalUnit >= draft.size()) continue;
                return new CorrectionTarget(rowIndex, point.id(), draft.units().get(globalUnit).physicalLine(), replacement);
            }
        }
        return null;
    }

    private static String replacePhysicalLine(String text, int physicalLine, String replacement) {
        int line = 1;
        int start = 0;
        while (line < physicalLine && start < text.length()) {
            char c = text.charAt(start++);
            if (c == '\r') {
                if (start < text.length() && text.charAt(start) == '\n') start++;
                line++;
            } else if (c == '\n') line++;
        }
        if (line != physicalLine) throw new IllegalStateException("C6_FAKE_TARGET_LINE_OUT_OF_RANGE");
        int end = start;
        while (end < text.length() && text.charAt(end) != '\r' && text.charAt(end) != '\n') end++;
        return text.substring(0, start) + replacement + text.substring(end);
    }

    private static void assertNoPointChunksWereDispatched(EditorialPairPreview preview, int calls) {
        assertEquals("fake dispatch count equals flagged chunk count", preview.chunksWithFixPoints, calls);
    }

    private static void measureChapter(String chapter, EditorialPairPreview preview, ChunkPlan plan, String rawText, String draftText,
                                       List<EditInputs.GlossaryEntry> glossary, String finalText, Counts all) {
        Counts chapterCounts = new Counts();
        LineUnits draftUnits = LineUnits.parse(draftText);
        List<String> draftLines = draftUnits.texts();
        List<String> finalLines = LineUnits.parse(finalText).texts();
        FinalState[] finalState = new FinalState[draftLines.size()];
        java.util.Arrays.fill(finalState, FinalState.AMBIGUOUS);
        Map<String, Integer> draftFrequency = frequencies(draftLines);
        Map<String, Integer> finalFrequency = frequencies(finalLines);
        for (int i = 0; i < draftLines.size(); i++) {
            int inDraft = draftFrequency.get(draftLines.get(i));
            int inFinal = finalFrequency.getOrDefault(draftLines.get(i), 0);
            if (inFinal >= inDraft) finalState[i] = FinalState.SAME;
            else if (inDraft == 1) finalState[i] = FinalState.CHANGED;
            else chapterCounts.ownerEditAmbiguous++;
        }

        Map<Integer, EnumSet<FixPoint.Type>> expectedTypes = expectedKinds(rawText, draftText, plan, glossary);
        Map<Integer, EnumSet<FixPoint.Type>> detectedTypes = new HashMap<>();
        assertEquals("preview rows match CS-1 chunks", plan.chunks.size(), preview.rows.size());
        for (int entryIndex = 0; entryIndex < preview.map.entries().size(); entryIndex++) {
            PairMap.Entry entry = preview.map.entries().get(entryIndex);
            EditorialPairPreview.Row row = preview.rows.get(entryIndex);
            int localOffset = plan.chunks.get(entryIndex).draftFrom();
            for (FixPoint point : row.fixPoints) {
                for (FixPoint.Type type : point.types()) {
                    chapterCounts.points.merge(type, 1, Integer::sum);
                    if (!point.insertion()) detectedTypes.computeIfAbsent(localOffset + point.draftLineIndex(), ignored -> EnumSet.noneOf(FixPoint.Type.class)).add(type);
                }
            }
            chapterCounts.chunks++;
            if (row.fixPointCount > 0) {
                chapterCounts.calledChunks++;
                chapterCounts.inputTokens += row.estimatedInputTokens;
                chapterCounts.outputTokens += row.estimatedOutputTokens;
            }
        }

        for (int i = 0; i < draftLines.size(); i++) {
            if (finalState[i] == FinalState.AMBIGUOUS) continue;
            EnumSet<FixPoint.Type> candidates = expectedTypes.getOrDefault(i, EnumSet.noneOf(FixPoint.Type.class));
            if (finalState[i] == FinalState.CHANGED) {
                for (FixPoint.Type type : EDIT_TYPES) if (candidates.contains(type)) {
                    chapterCounts.ownerChanged.merge(type, 1, Integer::sum);
                    if (detectedTypes.getOrDefault(i, EnumSet.noneOf(FixPoint.Type.class)).contains(type)) {
                        chapterCounts.covered.merge(type, 1, Integer::sum);
                    }
                }
            }
        }
        for (Map.Entry<Integer, EnumSet<FixPoint.Type>> e : detectedTypes.entrySet()) {
            int index = e.getKey();
            for (FixPoint.Type type : e.getValue()) {
                if (index < 0 || index >= finalState.length || finalState[index] == FinalState.AMBIGUOUS) {
                    chapterCounts.pointAmbiguous.merge(type, 1, Integer::sum);
                } else {
                    chapterCounts.pointComparable.merge(type, 1, Integer::sum);
                    if (finalState[index] == FinalState.SAME) chapterCounts.finalUnchanged.merge(type, 1, Integer::sum);
                }
            }
        }

        for (LineAligner.Bead bead : ChunkPlan.decodeBeads(plan.beads)) {
            if (bead.rawCount() > 0 && bead.draftCount() == 0) chapterCounts.missingRawBeads++;
        }
        merge(all, chapterCounts);
        BigDecimal upperUsd = BigDecimal.valueOf(chapterCounts.inputTokens).multiply(new BigDecimal("0.000005"))
                .add(BigDecimal.valueOf(chapterCounts.outputTokens).multiply(new BigDecimal("0.000025"))).setScale(6, RoundingMode.CEILING);
        System.out.printf(java.util.Locale.ROOT,
                "C6 chapter=%s chunks=%d calls=%d points=%d address=%d glossary=%d kana=%d missing=%d in=%d out=%d conservativeUpperUsd=%s lunaReserveUsd=%s ownerChangedAmbiguous=%d missingRawBeads=%d%n",
                chapter, chapterCounts.chunks, chapterCounts.calledChunks, total(chapterCounts.points),
                chapterCounts.points.get(FixPoint.Type.ADDRESS), chapterCounts.points.get(FixPoint.Type.GLOSSARY),
                chapterCounts.points.get(FixPoint.Type.KANA), chapterCounts.points.get(FixPoint.Type.MISSING),
                chapterCounts.inputTokens, chapterCounts.outputTokens, upperUsd.toPlainString(),
                lunaReserveUsd(chapterCounts.inputTokens, chapterCounts.outputTokens).toPlainString(),
                chapterCounts.ownerEditAmbiguous, chapterCounts.missingRawBeads);
    }

    private static Map<Integer, EnumSet<FixPoint.Type>> expectedKinds(String rawText, String draftText, ChunkPlan plan,
                                                                       List<EditInputs.GlossaryEntry> glossary) {
        Map<Integer, EnumSet<FixPoint.Type>> out = new HashMap<>();
        LineUnits rawUnits = LineUnits.parse(rawText);
        LineUnits draftUnits = LineUnits.parse(draftText);
        List<LineAligner.Bead> beads = ChunkPlan.decodeBeads(plan.beads);
        for (LineAligner.Bead bead : beads) {
            if (bead.rawCount() == 0 || bead.draftCount() == 0) continue;
            StringBuilder rawGroup = new StringBuilder();
            for (int i = bead.rawStart(); i < bead.rawEnd(); i++) {
                if (rawGroup.length() > 0) rawGroup.append('\n');
                rawGroup.append(rawUnits.units().get(i).text());
            }
            for (int d = bead.draftStart(); d < bead.draftEnd(); d++) {
                String draft = draftUnits.units().get(d).text();
                EnumSet<FixPoint.Type> types = EnumSet.noneOf(FixPoint.Type.class);
                if (!AddressChecklist.unexpectedWords(draft, Set.of()).isEmpty()) types.add(FixPoint.Type.ADDRESS);
                if (RawAlignedNormalizer.containsKanaOrHan(draft)
                        && !containsAuthorizedSource(draft, glossary)) types.add(FixPoint.Type.KANA);
                for (EditInputs.GlossaryEntry term : glossary) {
                    if (!term.source().isBlank() && !term.target().isBlank() && rawGroup.toString().contains(term.source().trim())
                            && !draft.toLowerCase(java.util.Locale.ROOT).contains(term.target().trim().toLowerCase(java.util.Locale.ROOT))) {
                        types.add(FixPoint.Type.GLOSSARY);
                    }
                }
                if (!types.isEmpty()) out.put(d, types);
            }
        }
        return out;
    }

    private static boolean containsAuthorizedSource(String line, List<EditInputs.GlossaryEntry> glossary) {
        for (EditInputs.GlossaryEntry term : glossary) if (!term.source().isBlank() && line.contains(term.source().trim())) return true;
        return false;
    }

    private static Map<String, Integer> frequencies(List<String> lines) {
        Map<String, Integer> out = new HashMap<>();
        for (String line : lines) out.merge(line, 1, Integer::sum);
        return out;
    }

    private static EnumMap<FixPoint.Type, Integer> zeroes() {
        EnumMap<FixPoint.Type, Integer> out = new EnumMap<>(FixPoint.Type.class);
        for (FixPoint.Type type : FixPoint.Type.values()) out.put(type, 0);
        return out;
    }

    private static void merge(Counts total, Counts row) {
        for (FixPoint.Type type : FixPoint.Type.values()) {
            total.points.merge(type, row.points.get(type), Integer::sum);
            total.ownerChanged.merge(type, row.ownerChanged.get(type), Integer::sum);
            total.covered.merge(type, row.covered.get(type), Integer::sum);
            total.finalUnchanged.merge(type, row.finalUnchanged.get(type), Integer::sum);
            total.pointComparable.merge(type, row.pointComparable.get(type), Integer::sum);
            total.pointAmbiguous.merge(type, row.pointAmbiguous.get(type), Integer::sum);
        }
        total.chunks += row.chunks;
        total.calledChunks += row.calledChunks;
        total.inputTokens += row.inputTokens;
        total.outputTokens += row.outputTokens;
        total.ownerEditAmbiguous += row.ownerEditAmbiguous;
        total.missingRawBeads += row.missingRawBeads;
    }

    /**
     * The live C6 reservation for openai/gpt-5.6-luna, at the runner's pinned LUNA prices (USD 0.20 / 1.20 per M). The
     * conservativeUpperUsd column is the app fallback for a model of unknown price (USD 5 / 25 per M), not a luna quote.
     */
    static BigDecimal lunaReserveUsd(long inputTokens, long outputTokens) {
        return BigDecimal.valueOf(inputTokens).multiply(new BigDecimal("0.0000002"))
                .add(BigDecimal.valueOf(outputTokens).multiply(new BigDecimal("0.0000012"))).setScale(6, RoundingMode.CEILING);
    }

    private static void printSummary(Counts c) {
        BigDecimal estimateUsd = BigDecimal.valueOf(c.inputTokens).multiply(new BigDecimal("0.000005"))
                .add(BigDecimal.valueOf(c.outputTokens).multiply(new BigDecimal("0.000025"))).setScale(6, RoundingMode.CEILING);
        System.out.printf(java.util.Locale.ROOT,
                "C6 TOTAL chapters=%d chunks=%d calls=%d points=%d address=%d glossary=%d kana=%d missing=%d inputTokens=%d outputReserve=%d conservativeUpperUsd=%s lunaReserveUsd=%s%n",
                CHAPTERS.length, c.chunks, c.calledChunks, total(c.points), c.points.get(FixPoint.Type.ADDRESS),
                c.points.get(FixPoint.Type.GLOSSARY), c.points.get(FixPoint.Type.KANA), c.points.get(FixPoint.Type.MISSING),
                c.inputTokens, c.outputTokens, estimateUsd.toPlainString(), lunaReserveUsd(c.inputTokens, c.outputTokens).toPlainString());
        for (FixPoint.Type type : EDIT_TYPES) {
            int owner = c.ownerChanged.get(type);
            int covered = c.covered.get(type);
            int points = c.points.get(type);
            int comparable = c.pointComparable.get(type);
            int sameFinal = c.finalUnchanged.get(type);
            System.out.printf(java.util.Locale.ROOT,
                    "C6 TYPE=%s points=%d ownerChanged=%d covered=%d coverage=%s%% finalUnchanged=%d/%d ambiguousPoints=%d%n",
                    type.name(), points, owner, covered, percent(covered, owner), sameFinal, comparable, c.pointAmbiguous.get(type));
        }
        System.out.printf(java.util.Locale.ROOT, "C6 TYPE=MISSING points=%d rawOnlyAlignmentBeads=%d noise=not-applicable%n",
                c.points.get(FixPoint.Type.MISSING), c.missingRawBeads);
    }

    private static String percent(int n, int d) {
        if (d == 0) return "NA";
        return BigDecimal.valueOf(n).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(d), 1, RoundingMode.HALF_UP).toPlainString();
    }

    private static int total(Map<FixPoint.Type, Integer> values) { return values.values().stream().mapToInt(Integer::intValue).sum(); }

    private interface PathFilter { boolean accept(Path path); }

    private static Path required(Path dir, String name) {
        Path path = dir.resolve(name);
        assertTrue("missing private input file: " + name, Files.isRegularFile(path));
        return path;
    }

    private static Path findOne(Path dir, PathFilter filter) throws Exception {
        List<Path> found;
        try (var paths = Files.list(dir)) { found = paths.filter(filter::accept).toList(); }
        assertEquals("expected one DRAFT file in chapter directory", 1, found.size());
        return found.get(0);
    }

    private static String readUtf8(Path path) throws Exception {
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }
}
