package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.AddressChecklist;
import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairText;
import com.ml.tblandroidtxt.editorial.api.ReferenceProjector;
import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan;
import com.ml.tblandroidtxt.editorial.api.chunk.LineAligner;
import com.ml.tblandroidtxt.editorial.api.chunk.LineUnits;
import com.ml.tblandroidtxt.editorial.api.fix.FixPoint;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Opt-in, private-corpus, offline, 0 USD (set DIAG_INPUT_ROOT = the q2-inputs folder and DIAG_OUT = a private JSON-lines file;
 * DIAG_CHAPTERS = optional comma list). For every DRAFT line of every chapter it records the facts that decide whether the
 * address finder flags the line: the aligned RAW paragraphs, whether they carry a quotation, the pronoun rows that cover them,
 * whether a row is complete (both self and call), whether the paragraph is in a row conflict, the address words the line uses and
 * the types the real preview flagged. It also re-derives "the finder should flag this line" from those facts and counts every
 * disagreement with the real preview, so the explanation of an unflagged line is checked against the shipped finder. The output
 * carries book text and stays outside the repository.
 */
public final class EditorialPairPronounMissDiagnosisTest {
    private static final String[] ALL = {"004", "005", "006", "007", "008", "011", "014", "017"};

    @Test public void dumpFinderFactsPerDraftLineWhenConfigured() throws Exception {
        String inputRoot = System.getenv("DIAG_INPUT_ROOT");
        String outPath = System.getenv("DIAG_OUT");
        if (inputRoot == null || inputRoot.isBlank() || outPath == null || outPath.isBlank()) return;
        String[] chapters = System.getenv("DIAG_CHAPTERS") == null || System.getenv("DIAG_CHAPTERS").isBlank()
                ? ALL : System.getenv("DIAG_CHAPTERS").split(",");
        StringBuilder out = new StringBuilder();
        int mismatches = 0;
        int lines = 0;
        for (String chapter : chapters) {
            chapter = chapter.trim();
            Path dir = Path.of(inputRoot).resolve(chapter);
            assertTrue("missing chapter input: " + chapter, Files.isDirectory(dir));
            String raw = read(required(dir, chapter + "_RAW_JAKUAKU_MONSTER_VOL1.txt"));
            Path draftPath = findOne(dir, chapter);
            String draft = read(draftPath);
            Path glossaryPath = required(dir, chapter + "_JAKUAKU_MONSTER_VOL1_chapter_glossary.csv");
            Path pronounPath = required(dir, chapter + "_PRONOUN_JAKUAKU_MONSTER_VOL1.csv");
            List<EditInputs.GlossaryEntry> glossary = GlossaryStore.parseTerms(glossaryPath.getFileName().toString(), read(glossaryPath)).stream()
                    .map(t -> new EditInputs.GlossaryEntry(t.source, t.target, t.category, t.note)).toList();
            String glossaryText = EditorialApiSources.glossaryAsText(glossary);
            String pronoun = read(pronounPath);
            EditorialPairSourceLoader.References refs = new EditorialPairSourceLoader.References(
                    glossaryText, glossaryPath.getFileName().toString(), pronoun, pronounPath.getFileName().toString());
            EditorialPairSourceLoader.FilesLoad load = EditorialPairSourceLoader.planFiles(chapter, raw, draft, refs, settings());
            assertNotNull("plan missing: " + chapter, load.plan);
            assertFalse("blocked plan: " + chapter, load.plan.blocked());
            EditorialPairPreview preview = EditorialPairPreview.of(load.source, glossary, pronoun, EditorialPairSnapshot.cues("from,speaker,target"));
            assertTrue("preview blocked for " + chapter, preview.runnable());
            ChunkPlan plan = load.plan;
            PairMap map = preview.map;

            // flagged types per global DRAFT line, exactly as the shipped finder produced them
            Map<Integer, Set<FixPoint.Type>> flagged = new HashMap<>();
            for (int e = 0; e < map.entries().size(); e++) {
                int offset = plan.chunks.get(e).draftFrom();
                for (FixPoint p : preview.rows.get(e).fixPoints) {
                    if (p.insertion()) continue;
                    flagged.computeIfAbsent(offset + p.draftLineIndex(), k -> new LinkedHashSet<>()).addAll(p.types());
                }
            }

            LineUnits rawUnits = LineUnits.parse(raw);
            LineUnits draftUnits = LineUnits.parse(draft);
            List<Integer> rawParagraphs = paragraphs(raw, 1);
            int lastParagraph = rawParagraphs.isEmpty() ? 1 : rawParagraphs.get(rawParagraphs.size() - 1);
            ReferenceProjector.Projection table = ReferenceProjector.project(new ReferenceProjector.Params(map.raw.chapterId, 1, lastParagraph,
                    PairText.normalize(raw), "", "", Set.of("none")), glossary, pronoun);

            int rowsTotal = table.pronouns().size();
            int rowsIncomplete = 0;
            for (ReferenceProjector.Row r : table.pronouns()) if (r.call().isBlank() || r.self().isBlank()) rowsIncomplete++;
            out.append("{\"meta\":true,\"ch\":").append(js(chapter)).append(",\"draftLines\":").append(draftUnits.size())
                    .append(",\"rawLines\":").append(rawUnits.size()).append(",\"rows\":").append(rowsTotal)
                    .append(",\"rowsIncomplete\":").append(rowsIncomplete).append(",\"chunks\":").append(plan.chunks.size())
                    .append(",\"conflicts\":").append(table.conflicts().size()).append("}\n");

            // the shipped finder uses, per chunk, the rows chosen by the chunk's cue words and the conflicts of those rows only
            List<LineAligner.Bead> globalBeads = ChunkPlan.decodeBeads(plan.beads);
            for (int e = 0; e < map.entries().size(); e++) {
                PairMap.Entry entry = map.entries().get(e);
                ChunkPlan.Chunk chunk = plan.chunks.get(e);
                if (entry.missingDraft()) continue;
                String rawMain = map.rawText(entry);
                int[] rr = map.rawRange(entry);
                ReferenceProjector.Projection projection = ReferenceProjector.project(new ReferenceProjector.Params(map.raw.chapterId,
                        map.rawParagraphStart(entry), map.rawParagraphEnd(entry), rawMain,
                        map.raw.contextBeforeLines(rr[0], com.ml.tblandroidtxt.editorial.api.PairContract.CONTEXT_CHARS, 2),
                        map.raw.contextAfterLines(rr[1], com.ml.tblandroidtxt.editorial.api.PairContract.CONTEXT_CHARS, 2),
                        EditorialPairSnapshot.cues("from,speaker,target")), glossary, pronoun);
                Set<String> projectedKeys = new LinkedHashSet<>();
                for (ReferenceProjector.Row r : projection.pronouns()) projectedKeys.add(r.key());
                for (LineAligner.Bead bead : globalBeads) {
                    if (bead.draftCount() == 0 || bead.draftStart() < chunk.draftFrom() || bead.draftEnd() > chunk.draftTo()) continue;
                    List<String> rawLines = new ArrayList<>();
                    List<Integer> paras = new ArrayList<>();
                    for (int i = bead.rawStart(); i < bead.rawEnd(); i++) {
                        rawLines.add(rawUnits.units().get(i).text());
                        paras.add(rawParagraphs.get(i));
                    }
                    boolean dialogue = false;
                    for (String l : rawLines) if (l.indexOf('「') >= 0 || l.indexOf('『') >= 0) dialogue = true;
                    for (int d = bead.draftStart(); d < bead.draftEnd(); d++) {
                        String text = draftUnits.units().get(d).text();
                        lines++;
                        boolean conflicted = false;
                        for (int p : paras) conflicted |= conflicted(projection, p);
                        List<String> rowJson = new ArrayList<>();
                        boolean shouldFlag = false;
                        for (ReferenceProjector.Row row : table.pronouns()) {
                            boolean covers = false;
                            boolean coversDialogue = false;
                            for (int i = 0; i < paras.size(); i++) {
                                if (row.covers(paras.get(i))) {
                                    covers = true;
                                    if (rawLines.get(i).indexOf('「') >= 0 || rawLines.get(i).indexOf('『') >= 0) coversDialogue = true;
                                }
                            }
                            if (!covers) continue;
                            boolean projected = projectedKeys.contains(row.key());
                            boolean complete = !row.call().isBlank() && !row.self().isBlank();
                            Set<String> allowed = new LinkedHashSet<>(List.of(row.call().trim().toLowerCase(Locale.ROOT), row.self().trim().toLowerCase(Locale.ROOT)));
                            Set<String> unexpected = new TreeSet<>(AddressChecklist.unexpectedWords(text, allowed));
                            if (projected && complete && coversDialogue && !unexpected.isEmpty()) {
                                boolean paraConflict = false;
                                for (int i = 0; i < paras.size(); i++) if (row.covers(paras.get(i)) && conflicted(projection, paras.get(i))) paraConflict = true;
                                if (!paraConflict) shouldFlag = true;
                            }
                            rowJson.add("{\"from\":" + js(row.from()) + ",\"speaker\":" + js(row.speaker()) + ",\"target\":" + js(row.target())
                                    + ",\"self\":" + js(row.self()) + ",\"call\":" + js(row.call()) + ",\"complete\":" + complete
                                    + ",\"projected\":" + projected + ",\"coversDialogue\":" + coversDialogue + ",\"unexpected\":" + jsArr(unexpected) + "}");
                        }
                        boolean realFlag = flagged.getOrDefault(d, Set.of()).contains(FixPoint.Type.ADDRESS);
                        if (realFlag != shouldFlag) mismatches++;
                        out.append("{\"ch\":").append(js(chapter)).append(",\"chunk\":").append(e + 1).append(",\"d\":").append(d)
                                .append(",\"raw\":[").append(bead.rawStart()).append(',').append(bead.rawEnd()).append("],\"paras\":").append(paras.toString().replace(" ", ""))
                                .append(",\"dialogue\":").append(dialogue).append(",\"conflicted\":").append(conflicted)
                                .append(",\"lex\":").append(jsArr(new TreeSet<>(AddressChecklist.unexpectedWords(text, Set.of()))))
                                .append(",\"flagged\":").append(jsArr(typeNames(flagged.getOrDefault(d, Set.of()))))
                                .append(",\"shouldFlagAddress\":").append(shouldFlag)
                                .append(",\"rows\":[").append(String.join(",", rowJson)).append("]")
                                .append(",\"text\":").append(js(text)).append(",\"rawText\":").append(js(String.join("\n", rawLines))).append("}\n");
                    }
                }
            }
        }
        Files.write(Path.of(outPath), out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("DIAG lines=" + lines + " finderMismatches=" + mismatches + " out=" + outPath);
        assertEquals("the re-derived rule must agree with the shipped finder on every line", 0, mismatches);
    }

    private static boolean conflicted(ReferenceProjector.Projection projection, int paragraph) {
        for (ReferenceProjector.Conflict c : projection.conflicts()) for (int[] r : c.overlap()) if (paragraph >= r[0] && paragraph <= r[1]) return true;
        return false;
    }

    /** Same paragraph numbering as the finder: a paragraph starts after a blank gap between two lines that carry letters. */
    private static List<Integer> paragraphs(String text, int first) {
        List<Integer> out = new ArrayList<>();
        int paragraph = Math.max(1, first);
        boolean sawNonBlank = false;
        boolean inGap = false;
        for (String line : PairText.normalize(text).split("\n", -1)) {
            if (PairText.isBlank(line)) {
                if (sawNonBlank) inGap = true;
                continue;
            }
            if (inGap) { paragraph++; inGap = false; }
            out.add(paragraph);
            sawNonBlank = true;
        }
        return out;
    }

    private static List<String> typeNames(Set<FixPoint.Type> types) {
        List<String> out = new ArrayList<>();
        for (FixPoint.Type t : types) out.add(t.name());
        return out;
    }

    private static AppSettings settings() {
        AppSettings s = new AppSettings();
        s.provider = "openrouter";
        s.model = "openai/gpt-5.6-luna";
        s.optimizationPreset = "balanced";
        s.chunkMode = "token";
        s.maxTokensPerChunk = 450;
        s.softLimitRatio = 0.8f;
        s.maxOutputTokens = 4096;
        s.contextChars = 400;
        return s;
    }

    private static String js(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c)); else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static String jsArr(Iterable<String> values) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String v : values) {
            if (!first) sb.append(',');
            first = false;
            sb.append(js(v));
        }
        return sb.append(']').toString();
    }

    private static Path required(Path dir, String name) {
        Path path = dir.resolve(name);
        assertTrue("missing private input file: " + name, Files.isRegularFile(path));
        return path;
    }

    private static Path findOne(Path dir, String chapter) throws Exception {
        List<Path> found;
        try (var paths = Files.list(dir)) {
            found = paths.filter(p -> p.getFileName().toString().startsWith(chapter + "_")
                    && (p.getFileName().toString().endsWith("_DRAFT.txt") || p.getFileName().toString().endsWith("_translated.txt"))).toList();
        }
        assertEquals("expected one DRAFT file in chapter directory " + chapter, 1, found.size());
        return found.get(0);
    }

    private static String read(Path path) throws Exception {
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return text.startsWith("﻿") ? text.substring(1) : text;
    }
}
