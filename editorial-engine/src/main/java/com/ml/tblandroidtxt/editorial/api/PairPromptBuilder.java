package com.ml.tblandroidtxt.editorial.api;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The edit request for one pair (or for the whole chapter as a pseudo-pair, the control arm). What the model must return is
 * marked once; the neighbouring text is labelled reference-only and is never part of the answer. References are projected
 * for the main range only (see {@link ReferenceProjector}); a row that applies to part of the range says where, and the RAW
 * paragraphs then carry the labels the row refers to. The model is never asked to count, hash or number anything. Pure.
 */
public final class PairPromptBuilder {
    /** The pair cannot be turned into a request (a missing DRAFT side, or paragraph numbers that do not add up). */
    public static final class PromptException extends RuntimeException {
        public final String code;

        public PromptException(String code, String detail) {
            super(code + ": " + detail);
            this.code = code;
        }
    }

    private PairPromptBuilder() { }

    public static ApiPrompt buildPair(PairMap map, PairMap.Entry entry, String targetLanguage, List<EditInputs.GlossaryEntry> glossary,
                                      String pronounCsv, Set<String> cueFields) {
        String draft = map.draftText(entry);
        if (draft == null) throw new PromptException("PAIR_MISSING_DRAFT", entry.pairId());
        int[] r = map.rawRange(entry);
        int[] d = map.draftRange(entry);
        String rawMain = map.raw.text.substring(r[0], r[1]);
        ReferenceProjector.Params params = new ReferenceProjector.Params(map.raw.chapterId, map.rawParagraphStart(entry), map.rawParagraphEnd(entry),
                rawMain, map.raw.contextBefore(r[0], PairContract.CONTEXT_CHARS), map.raw.contextAfter(r[1], PairContract.CONTEXT_CHARS), cueFields);
        return build(false, targetLanguage, params, glossary, pronounCsv, rawMain, draft,
                params.contextBefore(), params.contextAfter(), map.draft.contextBefore(d[0], PairContract.CONTEXT_CHARS),
                map.draft.contextAfter(d[1], PairContract.CONTEXT_CHARS));
    }

    /** The whole chapter as one pseudo-pair: same projection policy, main = every paragraph, no context. */
    public static ApiPrompt buildWhole(PairMap map, String targetLanguage, List<EditInputs.GlossaryEntry> glossary, String pronounCsv,
                                       Set<String> cueFields) {
        List<DocManifest.Unit> ru = map.raw.units();
        List<DocManifest.Unit> du = map.draft.units();
        if (ru.isEmpty() || du.isEmpty()) throw new PromptException("WHOLE_EMPTY", map.mapRevision);
        for (PairMap.Entry e : map.entries()) if (e.missingDraft()) throw new PromptException("PAIR_MISSING_DRAFT", e.pairId());
        String rawMain = map.raw.text.substring(ru.get(0).start(), ru.get(ru.size() - 1).end());
        String draft = map.draft.text.substring(du.get(0).start(), du.get(du.size() - 1).end());
        ReferenceProjector.Params params = new ReferenceProjector.Params(map.raw.chapterId, 1, Math.max(1, map.raw.paragraphCount()), rawMain, "", "", cueFields);
        return build(true, targetLanguage, params, glossary, pronounCsv, rawMain, draft, "", "", "", "");
    }

    private static ApiPrompt build(boolean whole, String targetLanguage, ReferenceProjector.Params params, List<EditInputs.GlossaryEntry> glossary,
                                   String pronounCsv, String rawMain, String draftMain, String rawBefore, String rawAfter,
                                   String draftBefore, String draftAfter) {
        ReferenceProjector.Projection projection = ReferenceProjector.project(params, glossary, pronounCsv);
        StringBuilder system = new StringBuilder(QualityCore.editPrompt(targetLanguage));
        system.append('\n').append(whole ? WHOLE_CONTRACT : PART_CONTRACT);
        List<String> rows = ReferenceProjector.renderPronounRows(projection);
        if (projection.glossary().isEmpty() && rows.isEmpty()) {
            system.append('\n').append(EditPromptBuilder.NO_REFERENCE).append('\n');
        } else {
            if (!projection.glossary().isEmpty()) {
                StringBuilder g = new StringBuilder();
                EditPromptBuilder.appendReferences(g, projection.glossary(), List.of());
                system.append(g);
            }
            if (!rows.isEmpty()) {
                system.append("\n# PRONOUNS (from | speaker | target | self | call | note)\n");
                if (projection.hasPartial()) {
                    system.append("A row followed by [áp dụng đoạn ...] applies only to the RAW paragraphs it names; the RAW paragraphs carry the labels ")
                            .append(PairContract.LABEL_OPEN).append("nnn").append(PairContract.LABEL_CLOSE).append(" (never copy a label).\n");
                }
                for (String row : rows) system.append(row).append('\n');
                List<String> conflicts = ReferenceProjector.renderConflicts(projection);
                if (!conflicts.isEmpty()) {
                    system.append("Rows for the same speaker and target disagree on some paragraphs (")
                            .append(String.join("; ", conflicts))
                            .append("). Do not choose between them: keep the form of address already used in the DRAFT there.\n");
                }
            }
        }
        system.append('\n').append(EditPromptBuilder.OUTPUT_CONTRACT);

        StringBuilder user = new StringBuilder();
        if (!whole && !rawBefore.isBlank()) user.append("# RAW CONTEXT BEFORE (REFERENCE ONLY - do not return, do not edit)\n").append(rawBefore).append("\n\n");
        user.append(whole ? "# RAW\n" : "# RAW (this part)\n");
        user.append(projection.hasPartial() ? labelled(rawMain, params.mainParaStart(), params.mainParaEnd()) : rawMain).append("\n\n");
        if (!whole && !rawAfter.isBlank()) user.append("# RAW CONTEXT AFTER (REFERENCE ONLY - do not return, do not edit)\n").append(rawAfter).append("\n\n");
        if (!whole && !draftBefore.isBlank()) user.append("# DRAFT CONTEXT BEFORE (REFERENCE ONLY - do not return, do not edit)\n").append(draftBefore).append("\n\n");
        user.append(whole ? "# DRAFT\n" : "# DRAFT (this part - return the edited version of exactly this text)\n").append(draftMain).append('\n');
        if (!whole && !draftAfter.isBlank()) user.append("\n# DRAFT CONTEXT AFTER (REFERENCE ONLY - do not return, do not edit)\n").append(draftAfter).append('\n');
        return new ApiPrompt(EditorialApiContract.Step.EDIT, system.toString(), user.toString(), projection.glossary().size(), rows.size(),
                QualityCore.editSha256());
    }

    /** Puts the whole-chapter paragraph label in front of every paragraph of the main text; the count must match the manifest. */
    static String labelled(String main, int firstParagraph, int lastParagraph) {
        StringBuilder out = new StringBuilder();
        int paragraph = firstParagraph - 1;
        boolean inParagraph = false;
        String[] lines = main.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i > 0) out.append('\n');
            if (PairText.isBlank(line)) { inParagraph = false; out.append(line); continue; }
            if (!inParagraph) { paragraph++; inParagraph = true; out.append(PairContract.paragraphLabel(paragraph)).append(' '); }
            out.append(line);
        }
        if (paragraph != lastParagraph) {
            throw new PromptException("LABEL_COUNT_MISMATCH", String.format(Locale.ROOT, "%d..%d labelled to %d", firstParagraph, lastParagraph, paragraph));
        }
        return out.toString();
    }

    private static final String PART_CONTRACT =
            "CHUNK CONTRACT\n"
            + "You edit ONE part of a longer chapter. Return only the edited text of the part marked DRAFT (this part), between the tags below. "
            + "Text marked REFERENCE ONLY is neighbouring context: never return it, never edit it, never copy it into your answer. "
            + "Keep the line breaks and any marker line (such as a line with only a symbol) of the DRAFT part where they are.\n";

    private static final String WHOLE_CONTRACT =
            "CHAPTER CONTRACT\n"
            + "You edit the whole chapter in one answer. Keep the line breaks and any marker line (such as a line with only a symbol) of the DRAFT where they are.\n";
}
