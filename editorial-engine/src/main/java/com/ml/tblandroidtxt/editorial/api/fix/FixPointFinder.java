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
    public record Result(List<FixPoint> points, int ambiguousGlossarySplits, int alignedRawLines, int alignedDraftLines,
                         List<PunctuationRepair> punctuation) {
        public Result {
            points = List.copyOf(points);
            punctuation = punctuation == null ? List.of() : List.copyOf(punctuation);
        }

        public Result(List<FixPoint> points, int ambiguousGlossarySplits, int alignedRawLines, int alignedDraftLines) {
            this(points, ambiguousGlossarySplits, alignedRawLines, alignedDraftLines, List.of());
        }
    }

    /** A DRAFT line (index among the chunk's non-blank lines) whose marks {@link RawPunctuation} restores from its aligned RAW lines. */
    public record PunctuationRepair(int draftLineIndex, String rawGroup) { }

    /** Precedes the DRAFT address word that an ADDRESS rule found outside a row's self/call; the prompt reads it back. */
    static final String ADDRESS_OUTSIDE_MARKER = "; dạng xưng hô ngoài self/call: ";

    private static final class Target {
        final int draftIndex;
        final int rawOrder;
        final String draft;
        final StringBuilder raw = new StringBuilder();
        final Set<FixPoint.Type> types = new LinkedHashSet<>();
        final LinkedHashSet<String> rules = new LinkedHashSet<>();
        final LinkedHashSet<AddressRow> addressRows = new LinkedHashSet<>();
        String rawContext = "";
        List<String> sceneEntities = List.of();

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
        // The edit lock reads every row of the chapter table in scope, not only the rows whose cue is in this chunk: a speaker whose
        // row is not shown to the model can still be the one who speaks. A cue field that names no column keeps every row (GLOBAL).
        ReferenceProjector.Projection tableRows = ReferenceProjector.project(new ReferenceProjector.Params(map.raw.chapterId,
                map.rawParagraphStart(entry), map.rawParagraphEnd(entry), rawMain, "", "", Set.of("none")), glossary, pronounCsv);
        // a pronoun row names how a speaker addresses someone in the RAW ("白いの"); where that wording contains a glossary source, the row
        // decides that stretch and the glossary does not ask for the term
        List<String> rowFroms = new ArrayList<>();
        for (ReferenceProjector.Row row : tableRows.pronouns()) if (!row.from().isBlank()) rowFroms.add(row.from().trim());
        List<LineAligner.Bead> beads = beads(raw, draft, glossary, entry, plan);
        List<Integer> rawParagraphs = paragraphs(rawMain, map.rawParagraphStart(entry));
        TreeMap<Integer, Target> targets = new TreeMap<>();
        List<Missing> missing = new ArrayList<>();
        List<PunctuationRepair> punctuation = new ArrayList<>();
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
                if (containsUncoveredSource(rawLines, term.source(), projection.glossary(), rowFroms)
                        && !draftGroup.toLowerCase(Locale.ROOT).contains(term.target().trim().toLowerCase(Locale.ROOT))
                        && !namedByThirdPersonPronoun(term, draftGroup)) {
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
                // one DRAFT line for its RAW group: the marks can be matched position by position (P2)
                if (hasAlignedRaw && draftCount == 1 && !RawPunctuation.restore(draftLine, rawGroup).equals(draftLine)) {
                    punctuation.add(new PunctuationRepair(di, rawGroup));
                }

                if (hasAlignedRaw && untranslatedScript(draftLine, rawGroup) && !authorizedSourceToken(draftLine, projection.glossary())) {
                    target.add(FixPoint.Type.KANA, rawGroup,
                            "KANA: DRAFT còn chữ Nhật chưa dịch; chỉ dịch phần đó sang ngôn ngữ đích theo RAW, giữ tên và từ nguồn mà Glossary cho phép, không đổi phần đã dịch.");
                }
                if (hasAlignedRaw && !missingTerms.isEmpty() && draftCount == 1) {
                    for (EditInputs.GlossaryEntry term : missingTerms) {
                        target.add(FixPoint.Type.GLOSSARY, rawGroup,
                                "GLOSSARY: khi đúng ngữ cảnh RAW dùng thuật ngữ nguồn '" + term.source().trim() + "' thì DRAFT phải dùng dạng đích '"
                                        + term.target().trim() + "'; kiểm ngữ cảnh, không thay máy móc.");
                    }
                }
                // Scope comes from the aligned RAW quotation and pronoun row. Do not require
                // the DRAFT to preserve Japanese quote marks: their omission must not hide
                // a pronoun that is already outside the applicable row.
                if (hasAlignedRaw) {
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
                        Set<String> expected = AddressChecklist.cellForms(row.self(), row.call());
                        for (String token : AddressChecklist.unexpectedWords(draftLine, expected)) {
                            target.add(FixPoint.Type.ADDRESS, rawGroup,
                                    "ADDRESS: Pronoun row " + row.from() + " | " + row.speaker() + " → " + row.target()
                                            + " | self=" + row.self() + " | call=" + row.call() + " | " + row.note()
                                            + ADDRESS_OUTSIDE_MARKER + token
                                            + "; áp dụng trong scope của hàng; chỉ sửa nếu RAW xác nhận đúng speaker/target, nếu mơ hồ dùng [n] =.");
                        }
                    }
                    if (target.types.contains(FixPoint.Type.ADDRESS)) {
                        // the rows of the table in scope for this line are what the edit lock later checks the model's answer against
                        // the RAW lines right before and after the aligned lines show who is in the scene
                        StringBuilder neighbours = new StringBuilder();
                        if (bead.rawStart() > 0) neighbours.append(raw.units().get(bead.rawStart() - 1).text());
                        if (bead.rawEnd() < raw.size()) {
                            if (neighbours.length() > 0) neighbours.append('\n');
                            neighbours.append(raw.units().get(bead.rawEnd()).text());
                        }
                        target.rawContext = neighbours.toString();
                        for (ReferenceProjector.Row row : tableRows.pronouns()) {
                            for (int p : candidateParagraphs) {
                                if (row.covers(p)) { target.addressRows.add(AddressRow.of(row)); break; }
                            }
                        }
                        target.sceneEntities = sceneEntities(target.addressRows, glossary, target.rawContext);
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
                points.add(new FixPoint(id++, t.types, t.draftIndex, t.draft, t.raw.toString(), new ArrayList<>(t.rules), new ArrayList<>(t.addressRows), t.rawContext, t.sceneEntities));
            } else {
                Missing m = (Missing) value;
                points.add(new FixPoint(id++, Set.of(FixPoint.Type.MISSING), m.afterDraftIndex, "", m.raw, List.of(m.rule)));
            }
        }
        return new Result(points, ambiguousSplits, raw.size(), draft.size(), punctuation);
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

    /**
     * True when the source stands on its own in some RAW line. An occurrence inside a longer glossary source (リーダー in ゾンビリーダー)
     * belongs to that longer entry, whose own target is checked separately, so it never makes the shorter entry's target "missing".
     */
    private static boolean containsUncoveredSource(List<String> lines, String source, List<EditInputs.GlossaryEntry> glossary,
                                                   List<String> rowFroms) {
        String needle = source == null ? "" : source.trim();
        if (needle.isEmpty()) return false;
        for (String line : lines) {
            for (int at = line.indexOf(needle); at >= 0; at = line.indexOf(needle, at + 1)) {
                if (!coveredByLongerSource(line, at, needle, glossary) && !coveredByRowFrom(line, at, needle, rowFroms)
                        && !partOfLongerWord(line, at, needle)) return true;
            }
        }
        return false;
    }

    /**
     * The occurrence lies inside a pronoun row's RAW address wording that is longer than the glossary source (白 in 白いの) and inside
     * a quotation: a row only describes how someone speaks, so the same wording in narration (日屋見さん in a sentence about her) is
     * still checked against the glossary.
     */
    private static boolean coveredByRowFrom(String line, int at, String needle, List<String> rowFroms) {
        if (!insideQuotation(line, at)) return false;
        for (String longer : rowFroms) {
            if (longer.length() <= needle.length() || !longer.contains(needle)) continue;
            for (int from = Math.max(0, at - longer.length() + needle.length()); from <= at; from++) {
                if (line.startsWith(longer, from) && from + longer.length() >= at + needle.length()) return true;
            }
        }
        return false;
    }

    /** True when the position lies between an opening 「/『 and its closing mark on the same line. */
    private static boolean insideQuotation(String line, int at) {
        int depth = 0;
        for (int i = 0; i < at && i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '「' || c == '『') depth++;
            else if ((c == '」' || c == '』') && depth > 0) depth--;
        }
        return depth > 0;
    }

    private static final String[] NAME_ENDINGS = {"ちゃん", "くん", "さん", "さま", "様", "殿", "君", "先輩", "先生"};
    private static final String NAME_PARTICLES = "はがをにのへともでやかねよなわぞぜ";

    /**
     * A one-character Han source (白) is ambiguous: as a name it stands alone, followed by a particle, an honorific or punctuation;
     * inside 白い悪魔, 純白 or 白魔法 it is part of a longer word (a colour, an adjective stem, a compound) and not the name.
     * Longer sources are unaffected.
     */
    private static boolean partOfLongerWord(String line, int at, String needle) {
        if (needle.codePointCount(0, needle.length()) != 1 || !isHan(needle.codePointAt(0))) return false;
        int end = at + needle.length();
        if (at > 0 && isHan(line.codePointBefore(at))) return true;
        if (end >= line.length()) return false;
        for (String ending : NAME_ENDINGS) if (line.startsWith(ending, end)) return false;
        int after = line.codePointAt(end);
        if (isHan(after) || Character.UnicodeScript.of(after) == Character.UnicodeScript.KATAKANA) return true;
        if (Character.UnicodeScript.of(after) == Character.UnicodeScript.HIRAGANA) return NAME_PARTICLES.indexOf(after) < 0;
        return false;
    }

    private static boolean isHan(int codePoint) { return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN; }

    /** A capitalised word that does not start a sentence or a quotation (Fuyu in "Khi đó, Fuyu sẽ nói"). */
    private static boolean hasMidSentenceProperName(String text) {
        String starters = ".!?。！？…「『“\"‘'—:";
        int lastNonSpace = -1;
        boolean atWordStart = true;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isLetter(cp)) {
                if (atWordStart && Character.isUpperCase(cp) && lastNonSpace >= 0 && starters.indexOf(lastNonSpace) < 0) return true;
                atWordStart = false;
            } else {
                atWordStart = true;
            }
            if (!Character.isWhitespace(cp)) lastNonSpace = cp;
        }
        return false;
    }

    private static final Set<String> THIRD_PERSON_WORDS = Set.of("hắn", "gã", "lão", "ả", "nàng", "chàng");
    private static final Set<String> THIRD_PERSON_HEADS = Set.of("anh", "cô", "chị", "ông", "bà", "ngài", "em", "cậu", "bác", "chú");
    private static final Set<String> THIRD_PERSON_TAILS = Set.of("ta", "ấy", "kia");

    /**
     * A character entry asks for the name, but a DRAFT line that already refers to that person with a third-person pronoun (hắn,
     * anh ta, cô ấy, lão) is correct as it is; forcing the name would only change a pronoun into a name. When the line also carries
     * another proper name in mid-sentence, the pronoun may refer to that other person and the name is still checked.
     */
    private static boolean namedByThirdPersonPronoun(EditInputs.GlossaryEntry term, String draftGroup) {
        String category = term.category() == null ? "" : term.category().trim();
        if (!"character".equalsIgnoreCase(category)) return false;
        if (hasMidSentenceProperName(draftGroup)) return false;
        List<String> tokens = AddressChecklist.tokens(draftGroup);
        for (int i = 0; i < tokens.size(); i++) {
            if (THIRD_PERSON_WORDS.contains(tokens.get(i))) return true;
            if (i + 1 < tokens.size() && THIRD_PERSON_HEADS.contains(tokens.get(i)) && THIRD_PERSON_TAILS.contains(tokens.get(i + 1))) return true;
        }
        return false;
    }

    private static boolean coveredByLongerSource(String line, int at, String needle, List<EditInputs.GlossaryEntry> glossary) {
        for (EditInputs.GlossaryEntry other : glossary) {
            String longer = other.source().trim();
            if (longer.length() <= needle.length() || !longer.contains(needle)) continue;
            for (int from = Math.max(0, at - longer.length() + needle.length()); from <= at; from++) {
                if (line.startsWith(longer, from) && from + longer.length() >= at + needle.length()) return true;
            }
        }
        return false;
    }

    /**
     * Entities of the pronoun rows that the glossary names in the RAW lines next to the aligned lines: a glossary entry whose target
     * shares a name word with the entity (島川 → Shimakawa for the speaker Shimakawa) and whose source is in those lines puts that
     * entity in the scene, even when no pronoun row is aimed at it.
     */
    private static List<String> sceneEntities(Set<AddressRow> rows, List<EditInputs.GlossaryEntry> glossary, String neighbours) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (neighbours.isEmpty() || glossary == null) return List.of();
        for (EditInputs.GlossaryEntry entry : glossary) {
            String source = entry.source() == null ? "" : entry.source().trim();
            if (source.isEmpty() || !neighbours.contains(source)) continue;
            Set<String> targetWords = new LinkedHashSet<>(AddressChecklist.tokens(entry.target()));
            for (AddressRow row : rows) {
                for (String entity : new String[] {row.speaker(), row.target()}) {
                    if (entity.isEmpty() || entity.startsWith("*") || entity.startsWith("@")) continue;
                    for (String word : AddressChecklist.tokens(entity)) {
                        if (targetWords.contains(word)) out.add(entity.trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        return new ArrayList<>(out);
    }

    /** Han numerals the book keeps in its chapter headings (○一七); see {@link #untranslatedScript}. */
    private static final String HAN_NUMERALS = "〇一二三四五六七八九十百千";

    /**
     * Kana or Han left in the DRAFT line, except a run of Han numerals that stands verbatim in the aligned RAW: the chapter heading
     * "○一七" is copied on purpose, and flagging it made the model rewrite the whole heading (v4.20 E, chapter 017).
     */
    private static boolean untranslatedScript(String draftLine, String rawGroup) {
        StringBuilder rest = new StringBuilder();
        for (int i = 0; i < draftLine.length(); ) {
            int cp = draftLine.codePointAt(i);
            if (HAN_NUMERALS.indexOf(cp) < 0) {
                rest.appendCodePoint(cp);
                i += Character.charCount(cp);
                continue;
            }
            int start = i;
            while (i < draftLine.length() && HAN_NUMERALS.indexOf(draftLine.codePointAt(i)) >= 0) i += Character.charCount(draftLine.codePointAt(i));
            String run = draftLine.substring(start, i);
            if (!rawGroup.contains(run)) rest.append(run);
        }
        return RawAlignedNormalizer.containsKanaOrHan(rest.toString());
    }

    private static boolean authorizedSourceToken(String line, List<EditInputs.GlossaryEntry> glossary) {
        for (EditInputs.GlossaryEntry term : glossary) {
            String source = term.source().trim();
            if (!source.isEmpty() && line.contains(source)) return true;
        }
        return false;
    }

    private static boolean containsDialogue(String line) { return line.indexOf('「') >= 0 || line.indexOf('『') >= 0; }

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
