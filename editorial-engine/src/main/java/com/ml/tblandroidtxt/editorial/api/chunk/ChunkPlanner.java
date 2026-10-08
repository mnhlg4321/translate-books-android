package com.ml.tblandroidtxt.editorial.api.chunk;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * CS-1 end to end, offline: read units (S2), align (S3), judge the chapter (S4), cut (S5), check each chunk (S6) and return
 * the plan (S8) together with the exact chunk texts. The chunk texts are slices of the two source texts and are checked to
 * concatenate back to each source byte for byte (S9) before a plan is returned.
 */
public final class ChunkPlanner {
    /** Everything the plan depends on besides the four texts. */
    public record Settings(String mode, int soft, int hard, int maxOutputTokens, double outputFactor,
                           ToIntFunction<String> measureRaw, ToIntFunction<String> measureDraft, double seriesRatio) { }

    public record Planned(ChunkPlan plan, LineUnits raw, LineUnits draft) {
        public List<String> rawRows() { return rows(plan, raw, true); }

        public List<String> draftRows() { return rows(plan, draft, false); }

        private static List<String> rows(ChunkPlan plan, LineUnits units, boolean isRaw) {
            List<String> out = new ArrayList<>();
            for (ChunkPlan.Chunk c : plan.chunks) out.add(isRaw ? units.slice(c.rawFrom(), c.rawTo()) : units.slice(c.draftFrom(), c.draftTo()));
            return out;
        }
    }

    private ChunkPlanner() { }

    public static Planned plan(String rawText, String draftText, List<EditInputs.GlossaryEntry> glossary, String glossaryText, String pronounText,
                               Settings s) {
        LineUnits raw = LineUnits.parse(rawText);
        LineUnits draft = LineUnits.parse(draftText);
        List<Anchors.Term> terms = Anchors.terms(glossary);
        String rawSha = PairText.sha256(rawText == null ? "" : rawText);
        String draftSha = PairText.sha256(draftText == null ? "" : draftText);
        String glossarySha = glossaryText == null || glossaryText.isEmpty() ? "" : PairText.sha256(glossaryText);
        String pronounSha = pronounText == null || pronounText.isBlank() ? "" : PairText.sha256(pronounText);
        ChunkPlan.Limits limits = new ChunkPlan.Limits(s.mode(), s.soft(), s.hard(), s.maxOutputTokens());
        if (raw.size() == 0 || draft.size() == 0) {
            ChapterVerdict.Result empty = ChapterVerdict.evaluate(raw.texts(), draft.texts(), emptyAlignment(), terms, s.seriesRatio());
            ChunkPlan plan = new ChunkPlan(rawSha, draftSha, glossarySha, pronounSha, limits, s.seriesRatio(), raw.size(), draft.size(), List.of(), List.of(),
                    empty.level().name(), reasons(empty, false, 0), 0, 0, 0, 0, 0);
            return new Planned(plan, raw, draft);
        }
        List<String> rawLines = raw.texts();
        List<String> draftLines = draft.texts();
        LineAligner.Alignment alignment = LineAligner.align(rawLines, draftLines, terms);
        ChapterVerdict.Result verdict = ChapterVerdict.evaluate(rawLines, draftLines, alignment, terms, s.seriesRatio());
        ChunkCutter.Limits cutLimits = new ChunkCutter.Limits(s.soft(), s.hard(), s.maxOutputTokens(), s.outputFactor(), s.measureRaw(), s.measureDraft());
        List<ChunkCutter.Cut> cuts = ChunkCutter.cut(alignment, rawLines, draftLines, cutLimits);
        ChunkChecks.Result checked = ChunkChecks.apply(alignment, rawLines, draftLines, terms, cuts);
        List<LineAligner.Bead> beads = alignment.beads();
        List<ChunkPlan.Chunk> chunks = new ArrayList<>();
        for (ChunkChecks.Checked c : checked.chunks()) {
            List<String> flags = new ArrayList<>(c.flags());
            for (String f : c.failures()) flags.add("CHECK_" + f);
            chunks.add(new ChunkPlan.Chunk(beads.get(c.beadFrom()).rawStart(), beads.get(c.beadTo()).rawEnd(),
                    beads.get(c.beadFrom()).draftStart(), beads.get(c.beadTo()).draftEnd(), Collections.unmodifiableList(flags), c.uncertain()));
        }
        ChunkPlan plan = new ChunkPlan(rawSha, draftSha, glossarySha, pronounSha, limits, s.seriesRatio(), raw.size(), draft.size(),
                ChunkPlan.encodeBeads(beads), chunks, level(verdict, checked.raisesWarn()), reasons(verdict, checked.raisesWarn(), checked.failureShare()),
                verdict.splitGroups(), verdict.mergeGroups(), verdict.rawOnly(), verdict.draftOnly(), checked.initialFailures());
        Planned planned = new Planned(plan, raw, draft);
        if (!String.join("", planned.rawRows()).equals(raw.source()) || !String.join("", planned.draftRows()).equals(draft.source())) {
            throw new IllegalStateException("CHUNK_REASSEMBLY_MISMATCH");
        }
        return planned;
    }

    private static String level(ChapterVerdict.Result verdict, boolean raiseWarn) {
        if (verdict.level() == ChapterVerdict.Level.OK && raiseWarn) return ChapterVerdict.Level.WARN.name();
        return verdict.level().name();
    }

    private static List<ChunkPlan.Reason> reasons(ChapterVerdict.Result verdict, boolean raiseWarn, double failureShare) {
        List<ChunkPlan.Reason> out = new ArrayList<>();
        for (ChapterVerdict.Reason r : verdict.reasons()) out.add(new ChunkPlan.Reason(r.level().name(), r.code(), r.value()));
        if (raiseWarn) out.add(new ChunkPlan.Reason(ChapterVerdict.Level.WARN.name(), "CHUNK_CHECKS", failureShare));
        return out;
    }

    private static LineAligner.Alignment emptyAlignment() {
        return new LineAligner.Alignment(List.of(), List.of(), List.of(), 0);
    }
}
