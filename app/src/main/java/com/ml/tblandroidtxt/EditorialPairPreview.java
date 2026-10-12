package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairMaps;
import com.ml.tblandroidtxt.editorial.api.PairPromptBuilder;
import com.ml.tblandroidtxt.editorial.api.PairText;
import com.ml.tblandroidtxt.editorial.api.ReferenceProjector;
import com.ml.tblandroidtxt.editorial.api.SourceCheck;
import com.ml.tblandroidtxt.editorial.api.ApiPrompt;
import com.ml.tblandroidtxt.editorial.api.DocManifest;
import com.ml.tblandroidtxt.editorial.api.fix.FixPoint;
import com.ml.tblandroidtxt.editorial.api.fix.FixPointFinder;
import com.ml.tblandroidtxt.editorial.api.fix.TargetedFixPrompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What the person sees before any money is spent: the pairs, which ones are missing, which cannot run and why, and what the
 * references reduce to for each pair. {@code blockers} are typed codes that stop a run; {@code warnings} never do. Pure.
 */
public final class EditorialPairPreview {
    public static final class Row {
        public int ordinal;
        public String pairId = "";
        public boolean missing;
        public boolean tooLong;
        public int rawChars;
        public int draftChars;
        public int glossaryEntries;
        public int pronounRows;
        public int partialRows;
        public int conflicts;
        public int estimatedInputTokens;
        public int estimatedOutputTokens;
        public int fixPointCount;
        public int addressPoints;
        public int glossaryPoints;
        public int kanaPoints;
        public int missingPoints;
        public int ambiguousGlossarySplits;
        public List<FixPoint> fixPoints = List.of();
        public String rawHead = "";
        public String draftHead = "";
    }

    public final PairMap map;
    public final List<Row> rows = new ArrayList<>();
    public final List<String> blockers = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();
    public int missingCount;
    public int fixPointCount;
    public int chunksWithFixPoints;

    private EditorialPairPreview(PairMap map) { this.map = map; }

    public boolean runnable() { return blockers.isEmpty() && map != null; }

    public static EditorialPairPreview of(EditorialPairSource source, List<EditInputs.GlossaryEntry> glossary, String pronounText, Set<String> cueFields) {
        List<String> blockers = new ArrayList<>();
        for (String issue : source.lineageIssues) blockers.add("SOURCE_" + issue);
        if (source.rawRows.isEmpty() || source.draftRows.size() != source.rawRows.size()) {
            EditorialPairPreview empty = new EditorialPairPreview(null);
            if (blockers.isEmpty()) blockers.add("SOURCE_NO_ROWS");
            empty.blockers.addAll(blockers);
            return empty;
        }
        PairPromptBuilder.ContextPolicy context = source.plan == null ? PairPromptBuilder.ContextPolicy.DEFAULT
                : new PairPromptBuilder.ContextPolicy(source.plan.limits.contextChars(), PairPromptBuilder.ContextPolicy.DEFAULT.minLines());
        PairMap map = PairMaps.fromJobRows(source.chapterId, "raw:" + source.ref, "draft:" + source.ref, source.rawRows, source.draftRows);
        EditorialPairPreview preview = new EditorialPairPreview(map);
        preview.blockers.addAll(blockers);
        for (DocManifest.Issue issue : map.verify()) {
            if (issue.code().equals("MISSING_DRAFT")) continue; // reported once, below, as the missing pairs
            preview.blockers.add("MAP_" + issue.code());
        }
        if (source.plan != null) {
            if (source.plan.warned()) preview.warnings.add("CHAPTER_WARN");
            if (source.plan.uncertainChunks > 0) preview.warnings.add("UNCERTAIN_CHUNKS:" + source.plan.uncertainChunks);
        }
        boolean hasGlossary = glossary != null && !glossary.isEmpty();
        boolean hasPronoun = pronounText != null && !PairText.isBlank(pronounText);
        if (!hasGlossary) preview.warnings.add("NO_GLOSSARY");
        if (!hasPronoun) preview.warnings.add("NO_PRONOUN");
        int conflicts = 0;
        int dropped = 0;
        for (PairMap.Entry e : map.entries()) {
            Row row = new Row();
            row.ordinal = e.displayOrdinal();
            row.pairId = e.pairId();
            row.missing = e.missingDraft();
            String rawText = map.rawText(e);
            String draftText = map.draftText(e);
            row.rawChars = PairText.letters(rawText);
            row.draftChars = draftText == null ? 0 : PairText.letters(draftText);
            row.rawHead = head(rawText);
            row.draftHead = draftText == null ? "" : head(draftText);
            if (row.missing) {
                preview.missingCount++;
            } else {
                try {
                    FixPointFinder.Result fixes = FixPointFinder.find(map, e, glossary == null ? List.of() : glossary, pronounText,
                            cueFields, source.plan);
                    row.fixPoints = fixes.points();
                    row.fixPointCount = row.fixPoints.size();
                    row.ambiguousGlossarySplits = fixes.ambiguousGlossarySplits();
                    for (FixPoint point : row.fixPoints) {
                        if (point.types().contains(FixPoint.Type.ADDRESS)) row.addressPoints++;
                        if (point.types().contains(FixPoint.Type.GLOSSARY)) row.glossaryPoints++;
                        if (point.types().contains(FixPoint.Type.KANA)) row.kanaPoints++;
                        if (point.types().contains(FixPoint.Type.MISSING)) row.missingPoints++;
                    }
                    row.fixPointCount = row.fixPoints.size();
                    preview.fixPointCount += row.fixPointCount;
                    if (row.fixPointCount > 0) {
                        preview.chunksWithFixPoints++;
                        int[] r = map.rawRange(e);
                        int[] d = map.draftRange(e);
                        ApiPrompt prompt = TargetedFixPrompt.build("Vietnamese",
                                map.raw.contextBeforeLines(r[0], context.chars(), context.minLines()), rawText,
                                map.raw.contextAfterLines(r[1], context.chars(), context.minLines()),
                                map.draft.contextBeforeLines(d[0], context.chars(), context.minLines()), draftText,
                                map.draft.contextAfterLines(d[1], context.chars(), context.minLines()), row.fixPoints);
                        row.estimatedInputTokens = (int) Math.min(Integer.MAX_VALUE, prompt.estimatedInputTokens());
                        row.estimatedOutputTokens = TargetedFixPrompt.maxOutputTokens(row.fixPoints);
                        row.tooLong = prompt.estimatedInputTokens() > SourceCheck.MAX_ESTIMATED_TOKENS;
                    }
                    int[] r = map.rawRange(e);
                    ReferenceProjector.Projection pr = ReferenceProjector.project(new ReferenceProjector.Params(map.raw.chapterId,
                            map.rawParagraphStart(e), map.rawParagraphEnd(e), rawText, map.raw.contextBeforeLines(r[0], context.chars(), context.minLines()),
                            map.raw.contextAfterLines(r[1], context.chars(), context.minLines()), cueFields),
                            glossary == null ? List.of() : glossary, pronounText);
                    for (ReferenceProjector.Row p : pr.pronouns()) if (p.partial()) row.partialRows++;
                    row.conflicts = pr.conflicts().size();
                    conflicts += row.conflicts;
                    dropped += pr.count("SCOPE_INVALID");
                    row.glossaryEntries = pr.glossary().size();
                    row.pronounRows = pr.pronouns().size();
                } catch (PairPromptBuilder.PromptException | IllegalArgumentException e2) {
                    String code = e2 instanceof PairPromptBuilder.PromptException ? ((PairPromptBuilder.PromptException) e2).code : "FIX_ALIGNMENT_PLAN_MISMATCH";
                    preview.blockers.add("PAIR_" + code + ":" + row.ordinal);
                }
            }
            preview.rows.add(row);
        }
        if (preview.missingCount > 0) preview.blockers.add("MISSING_PAIRS:" + preview.missingCount);
        List<Integer> long_ = new ArrayList<>();
        for (Row r : preview.rows) if (r.tooLong) long_.add(r.ordinal);
        if (!long_.isEmpty()) preview.blockers.add("TOO_LONG:" + long_);
        if (conflicts > 0) preview.warnings.add("REFERENCE_CONFLICT:" + conflicts);
        if (dropped > 0) preview.warnings.add("SCOPE_INVALID:" + dropped);
        int ambiguous = 0;
        for (Row row : preview.rows) ambiguous += row.ambiguousGlossarySplits;
        if (ambiguous > 0) preview.warnings.add("AMBIGUOUS_GLOSSARY_SPLIT:" + ambiguous);
        return preview;
    }

    private static String head(String text) {
        if (text == null) return "";
        String t = PairText.trim(text);
        int cut = Math.min(t.length(), 80);
        String first = t.substring(0, cut).replace('\n', ' ');
        return t.length() > cut ? first + "…" : first;
    }
}
