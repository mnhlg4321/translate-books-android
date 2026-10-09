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

    /** How much reference-only text surrounds a pair: at least {@code chars} characters and {@code minLines} non-blank lines, in whole lines. */
    public record ContextPolicy(int chars, int minLines) {
        public static final ContextPolicy DEFAULT = new ContextPolicy(PairContract.CONTEXT_CHARS, 2);
    }

    public static ApiPrompt buildPair(PairMap map, PairMap.Entry entry, String targetLanguage, List<EditInputs.GlossaryEntry> glossary,
                                      String pronounCsv, Set<String> cueFields) {
        return buildPair(map, entry, targetLanguage, glossary, pronounCsv, cueFields, ContextPolicy.DEFAULT);
    }

    public static ApiPrompt buildPair(PairMap map, PairMap.Entry entry, String targetLanguage, List<EditInputs.GlossaryEntry> glossary,
                                      String pronounCsv, Set<String> cueFields, ContextPolicy context) {
        String draft = map.draftText(entry);
        if (draft == null) throw new PromptException("PAIR_MISSING_DRAFT", entry.pairId());
        int[] r = map.rawRange(entry);
        int[] d = map.draftRange(entry);
        String rawMain = map.raw.text.substring(r[0], r[1]);
        ReferenceProjector.Params params = new ReferenceProjector.Params(map.raw.chapterId, map.rawParagraphStart(entry), map.rawParagraphEnd(entry),
                rawMain, map.raw.contextBeforeLines(r[0], context.chars(), context.minLines()),
                map.raw.contextAfterLines(r[1], context.chars(), context.minLines()), cueFields);
        return build(false, targetLanguage, params, glossary, pronounCsv, rawMain, draft,
                params.contextBefore(), params.contextAfter(), map.draft.contextBeforeLines(d[0], context.chars(), context.minLines()),
                map.draft.contextAfterLines(d[1], context.chars(), context.minLines()));
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
        StringBuilder system = new StringBuilder(QualityCore.chunkEditPrompt(targetLanguage));
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
                if (!whole) {
                    List<String> check = AddressChecklist.build(projection, rawMain, draftMain, params.mainParaStart());
                    if (!check.isEmpty()) {
                        system.append("\n# ADDRESS CHECK (computed by the app from the rows above; it says where to look, not what is correct)\n");
                        for (String line : check) system.append(line).append('\n');
                        system.append("For each listed word, read RAW to decide who speaks and who is addressed. Only when RAW shows that the speaker and the "
                                + "addressee are the row's speaker and target, use the row's call for that address (and self for the speaker's own first person). "
                                + "Keep the word where RAW shows another speaker or addressee, or where it is unclear.\n");
                    }
                }
            }
        }
        system.append('\n').append(CHUNK_OUTPUT_CONTRACT);

        StringBuilder user = new StringBuilder();
        if (!whole && !rawBefore.isBlank()) user.append("# RAW CONTEXT BEFORE (REFERENCE ONLY - do not return, do not edit)\n").append(rawBefore).append("\n\n");
        user.append(whole ? "# RAW\n" : "# RAW (this part)\n");
        user.append(projection.hasPartial() ? labelled(rawMain, params.mainParaStart(), params.mainParaEnd()) : rawMain).append("\n\n");
        if (!whole && !rawAfter.isBlank()) user.append("# RAW CONTEXT AFTER (REFERENCE ONLY - do not return, do not edit)\n").append(rawAfter).append("\n\n");
        if (!whole && !draftBefore.isBlank()) user.append("# DRAFT CONTEXT BEFORE (REFERENCE ONLY - do not return, do not edit)\n").append(draftBefore).append("\n\n");
        user.append(whole ? "# DRAFT\n" : "# DRAFT (this part - return the edited version of exactly this text)\n").append(draftMain).append('\n');
        if (!whole && !draftAfter.isBlank()) user.append("\n# DRAFT CONTEXT AFTER (REFERENCE ONLY - do not return, do not edit)\n").append(draftAfter).append('\n');
        return new ApiPrompt(EditorialApiContract.Step.EDIT, system.toString(), user.toString(), projection.glossary().size(), rows.size(),
                QualityCore.chunkEditSha256());
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
            + "Preserve scene markers and the DRAFT layout unless a RAW-supported content correction requires changing the layout.\n";

    private static final String WHOLE_CONTRACT =
            "CHAPTER CONTRACT\n"
            + "You edit the whole chapter in one answer. Preserve scene markers and the DRAFT layout unless a RAW-supported content correction requires changing the layout.\n";

    private static final String CHUNK_OUTPUT_CONTRACT =
            "OUTPUT CONTRACT\n"
            + "Return only one of these forms. For a matching pair, return the complete corrected text of exactly the supplied DRAFT between "
            + EditPromptBuilder.EDITED_OPEN + " and " + EditPromptBuilder.EDITED_CLOSE + ". Do not shorten it or include reports or commentary. "
            + "Write the closing tag exactly as " + EditPromptBuilder.EDITED_CLOSE + ". "
            + "The answer is text in the target language only: never copy a RAW sentence into it; where a DRAFT line is wrong, rewrite that DRAFT line in the target language.\n"
            + "If RAW and DRAFT clearly describe different chapters, return " + EditPromptBuilder.WRONG_PAIR_OPEN
            + "brief evidence quoting each text" + EditPromptBuilder.WRONG_PAIR_CLOSE + " instead. A missing sentence or wrong number is a defect to fix, not a wrong pair.\n";
}
