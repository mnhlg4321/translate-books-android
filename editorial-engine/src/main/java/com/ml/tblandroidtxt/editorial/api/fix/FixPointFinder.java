package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.AddressChecklist;
import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairContract;
import com.ml.tblandroidtxt.editorial.api.PairText;
import com.ml.tblandroidtxt.editorial.api.RawAlignedNormalizer;
import com.ml.tblandroidtxt.editorial.api.ReferenceProjector;
import com.ml.tblandroidtxt.editorial.api.chunk.Anchors;
import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan;
import com.ml.tblandroidtxt.editorial.api.chunk.LineAligner;
import com.ml.tblandroidtxt.editorial.api.chunk.LineUnits;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

/** Detects only deterministic repair candidates whose RAW/DRAFT relationship is supplied by the pair or CS-1 plan. */
public final class FixPointFinder {
    public record Result(List<FixPoint> points, int ambiguousGlossarySplits, int alignedRawLines, int alignedDraftLines) {
        public Result { points = List.copyOf(points); }
    }

    private static final class Target {
        final int draftIndex;
        final int rawOrder;
        final String draft;
        final StringBuilder raw = new StringBuilder();
        final Set<FixPoint.Type> types = new LinkedHashSet<>();
        final LinkedHashSet<String> rules = new LinkedHashSet<>();

        Target(int draftIndex, int rawOrder, String draft) {
            this.draftIndex = draftIndex;
            this.rawOrder = rawOrder;
            this.draft = draft;
        }

        void add(FixPoint.Type type, String source, String rule) {
            types.add(type);
            if (!source.isBlank()) {
                if (raw.length() > 0 && !raw.toString().contains(source)) raw.append('\n');
                if (!raw.toString().contains(source)) raw.append(source);
            }
            if (rule != null && !rule.isBlank()) rules.add(rule);
        }
    }

    private record Missing(int rawOrder, int afterDraftIndex, String raw, String rule) { }

    private FixPointFinder() { }

    public static Result find(PairMap map, PairMap.Entry entry, List<EditInputs.GlossaryEntry> glossary, String pronounCsv,
                              Set<String> cueFields, ChunkPlan plan) {
        if (map == null || entry == null || entry.missingDraft()) throw new IllegalArgumentException("FIX_PAIR_MISSING_DRAFT");
        String rawMain = map.rawText(entry);
        String draftMain = map.draftText(entry);
        LineUnits raw = LineUnits.parse(rawMain);
        LineUnits draft = LineUnits.parse(draftMain);
        int[] rr = map.rawRange(entry);
        int[] dr = map.draftRange(entry);
        ReferenceProjector.Params params = new ReferenceProjector.Params(map.raw.chapterId, map.rawParagraphStart(entry), map.rawParagraphEnd(entry),
                rawMain, map.raw.contextBeforeLines(rr[0], PairContract.CONTEXT_CHARS, 2),
                map.raw.contextAfterLines(rr[1], PairContract.CONTEXT_CHARS, 2), cueFields);
        ReferenceProjector.Projection projection = ReferenceProjector.project(params, glossary, pronounCsv);
        List<LineAligner.Bead> beads = beads(raw, draft, glossary, entry, plan);
        List<Integer> rawParagraphs = paragraphs(rawMain, map.rawParagraphStart(entry));
        TreeMap<Integer, Target> targets = new TreeMap<>();
        List<Missing> missing = new ArrayList<>();
        int ambiguousSplits = 0;

        for (LineAligner.Bead bead : beads) {
            int rawCount = bead.rawCount();
            int draftCount = bead.draftCount();
            if (rawCount == 0 || draftCount == 0) {
                if (rawCount > 0) {
                    for (int ri = bead.rawStart(); ri < bead.rawEnd(); ri++) {
                        String rawLine = raw.units().get(ri).text();
                        missing.add(new Missing(ri, bead.draftStart() - 1, rawLine,
                                "MISSING: dịch đầy đủ dòng RAW vào vị trí sau dòng DRAFT " + bead.draftStart() + "; không bỏ ý, không thêm nội dung."));
                    }
                }
                continue;
            }
            List<String> rawLines = new ArrayList<>();
            List<String> draftLines = new ArrayList<>();
            for (int i = bead.rawStart(); i < bead.rawEnd(); i++) rawLines.add(raw.units().get(i).text());
            for (int i = bead.draftStart(); i < bead.draftEnd(); i++) draftLines.add(draft.units().get(i).text());
            String rawGroup = String.join("\n", rawLines);
            String draftGroup = String.join("\n", draftLines);

            List<EditInputs.GlossaryEntry> missingTerms = new ArrayList<>();
            for (EditInputs.GlossaryEntry term : projection.glossary()) {
                if (containsSource(rawLines, term.source()) && !draftGroup.toLowerCase(Locale.ROOT).contains(term.target().trim().toLowerCase(Locale.ROOT))) {
                    missingTerms.add(term);
                }
            }
            // A split RAW sentence can map to several DRAFT lines. With no target anchor, assigning its missing term to
            // an arbitrary half would be guesswork, so keep that detector out of the repair set and expose the count.
            if (!missingTerms.isEmpty() && draftCount > 1) ambiguousSplits += missingTerms.size();

            for (int di = bead.draftStart(); di < bead.draftEnd(); di++) {
                String draftLine = draft.units().get(di).text();
                Target target = null;
                boolean hasAlignedRaw = rawCount > 0;
                if (hasAlignedRaw) target = targets.computeIfAbsent(di, k -> new Target(k, bead.rawStart(), draftLine));

                if (hasAlignedRaw && RawAlignedNormalizer.containsKanaOrHan(draftLine) && !authorizedSourceToken(draftLine, projection.glossary())) {
                    target.add(FixPoint.Type.KANA, rawGroup,
                            "KANA: перевод должен быть на целевом языке; сопоставить с RAW и сохранить имена/тừ nguồn được cho phép trong Glossary.");
                }
                if (hasAlignedRaw && !missingTerms.isEmpty() && draftCount == 1) {
                    for (EditInputs.GlossaryEntry term : missingTerms) {
                        target.add(FixPoint.Type.GLOSSARY, rawGroup,
                                "GLOSSARY: khi đúng ngữ cảnh RAW dùng thuật ngữ nguồn '" + term.source().trim() + "' thì DRAFT phải dùng dạng đích '"
                                        + term.target().trim() + "'; kiểm ngữ cảnh, không thay máy móc.");
                    }
                }
                if (hasAlignedRaw && isDialogue(draftLine)) {
                    int[] candidateParagraphs = new int[rawCount];
                    for (int i = 0; i < rawCount; i++) candidateParagraphs[i] = rawParagraphs.get(bead.rawStart() + i);
                    for (ReferenceProjector.Row row : projection.pronouns()) {
                        if (row.call().isBlank() || row.self().isBlank()) continue;
                        boolean scopedDialogue = false;
                        for (int i = 0; i < rawCount; i++) {
                            int para = candidateParagraphs[i];
                            if (row.covers(para) && containsDialogue(rawLines.get(i)) && !conflicted(projection, para)) scopedDialogue = true;
                        }
                        if (!scopedDialogue) continue;
                        Set<String> expected = Set.of(row.call().trim().toLowerCase(Locale.ROOT), row.self().trim().toLowerCase(Locale.ROOT));
                        for (String token : AddressChecklist.unexpectedWords(draftLine, expected)) {
                            target.add(FixPoint.Type.ADDRESS, rawGroup,
                                    "ADDRESS: Pronoun row " + row.from() + " | " + row.speaker() + " → " + row.target()
                                            + " | self=" + row.self() + " | call=" + row.call() + " | " + row.note()
                                            + "; dạng xưng hô ngoài self/call: " + token
                                            + "; áp dụng trong scope của hàng; chỉ sửa nếu RAW xác nhận đúng speaker/target, nếu mơ hồ dùng [n] =.");
                        }
                    }
                }
            }
        }

        List<Object> ordered = new ArrayList<>();
        ordered.addAll(targets.values());
        ordered.addAll(missing);
        ordered.sort(Comparator.comparingInt((Object o) -> o instanceof Target t ? t.rawOrder : ((Missing) o).rawOrder)
                .thenComparingInt(o -> o instanceof Target t ? t.draftIndex : ((Missing) o).afterDraftIndex));
        List<FixPoint> points = new ArrayList<>();
        int id = 1;
        for (Object value : ordered) {
            if (value instanceof Target t) {
                if (t.types.isEmpty()) continue;
                points.add(new FixPoint(id++, t.types, t.draftIndex, t.draft, t.raw.toString(), new ArrayList<>(t.rules)));
            } else {
                Missing m = (Missing) value;
                points.add(new FixPoint(id++, Set.of(FixPoint.Type.MISSING), m.afterDraftIndex, "", m.raw, List.of(m.rule)));
            }
        }
        return new Result(points, ambiguousSplits, raw.size(), draft.size());
    }

    private static List<LineAligner.Bead> beads(LineUnits raw, LineUnits draft, List<EditInputs.GlossaryEntry> glossary,
                                                 PairMap.Entry entry, ChunkPlan plan) {
        if (plan == null) return LineAligner.align(raw.texts(), draft.texts(), Anchors.terms(glossary)).beads();
        int chunkIndex = entry.displayOrdinal() - 1;
        if (chunkIndex < 0 || chunkIndex >= plan.chunks.size()) throw new IllegalArgumentException("FIX_ALIGNMENT_PLAN_MISMATCH");
        ChunkPlan.Chunk chunk = plan.chunks.get(chunkIndex);
        if (chunk.rawTo() - chunk.rawFrom() != raw.size() || chunk.draftTo() - chunk.draftFrom() != draft.size()) {
            throw new IllegalArgumentException("FIX_ALIGNMENT_PLAN_MISMATCH");
        }
        List<LineAligner.Bead> out = new ArrayList<>();
        for (LineAligner.Bead b : ChunkPlan.decodeBeads(plan.beads)) {
            if (b.rawStart() < chunk.rawFrom() || b.rawEnd() > chunk.rawTo() || b.draftStart() < chunk.draftFrom() || b.draftEnd() > chunk.draftTo()) continue;
            out.add(new LineAligner.Bead(b.rawStart() - chunk.rawFrom(), b.rawEnd() - chunk.rawFrom(),
                    b.draftStart() - chunk.draftFrom(), b.draftEnd() - chunk.draftFrom()));
        }
        int r = 0;
        int d = 0;
        for (LineAligner.Bead b : out) {
            if (b.rawStart() != r || b.draftStart() != d) throw new IllegalArgumentException("FIX_ALIGNMENT_PLAN_MISMATCH");
            r = b.rawEnd();
            d = b.draftEnd();
        }
        if (r != raw.size() || d != draft.size()) throw new IllegalArgumentException("FIX_ALIGNMENT_PLAN_MISMATCH");
        return out;
    }

    private static boolean containsSource(List<String> lines, String source) {
        String needle = source == null ? "" : source.trim();
        if (needle.isEmpty()) return false;
        for (String line : lines) if (line.contains(needle)) return true;
        return false;
    }

    private static boolean authorizedSourceToken(String line, List<EditInputs.GlossaryEntry> glossary) {
        for (EditInputs.GlossaryEntry term : glossary) {
            String source = term.source().trim();
            if (!source.isEmpty() && line.contains(source)) return true;
        }
        return false;
    }

    private static boolean containsDialogue(String line) { return line.indexOf('「') >= 0 || line.indexOf('『') >= 0; }

    private static boolean isDialogue(String line) {
        return line.indexOf('「') >= 0 || line.indexOf('『') >= 0 || line.indexOf('」') >= 0 || line.indexOf('』') >= 0;
    }

    private static boolean conflicted(ReferenceProjector.Projection projection, int paragraph) {
        for (ReferenceProjector.Conflict c : projection.conflicts()) for (int[] r : c.overlap()) if (paragraph >= r[0] && paragraph <= r[1]) return true;
        return false;
    }

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
}
