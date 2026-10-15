package com.ml.tblandroidtxt.editorial.api.fix;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.PairText;
import com.ml.tblandroidtxt.editorial.api.RawAlignedNormalizer;
import com.ml.tblandroidtxt.editorial.api.chunk.LineUnits;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Applies a parsed answer only to its declared line or insertion point; every other source character is copied unchanged. */
public final class TargetedFixApplier {
    public enum Status { FIXED, KEPT, REJECTED, NO_ANSWER }
    /**
     * {@code proposed}: the model's line when a check refused it ({@link #offerable}), with the RAW's marks restored as an accepted
     * answer would get them; empty otherwise. It is kept so that a person can still apply it: a check without the speaker cannot
     * tell every right answer from a wrong one (Kumo ch 013 line 151, 2026-10-15). {@code citedRow}: the row the answer named.
     */
    public record Outcome(FixPoint point, Status status, String before, String after, String reason, String proposed, int citedRow) {
        public Outcome {
            proposed = proposed == null ? "" : proposed;
        }

        public Outcome(FixPoint point, Status status, String before, String after, String reason) {
            this(point, status, before, after, reason, "", TargetedFixParser.Answer.NOT_CITED);
        }
    }

    /**
     * A refused answer a person may still apply: a content lock or a length/symbol check. A malformed answer (several lines, a tag or
     * an explanation, new Japanese, nothing) is never offered.
     */
    public static boolean offerable(String reason) {
        if (reason == null) return false;
        if (reason.startsWith(AddressEditGuard.PREFIX)) return true;
        switch (reason) {
            case GLOSSARY_TARGET_REMOVED: case "PROTECTED_SYMBOL_CHANGE": case "LINE_LENGTH_RATIO": return true;
            default: return false;
        }
    }
    /** {@code punctuationLines}: lines whose marks were restored from the RAW ({@link RawPunctuation}). */
    public record Result(String text, List<Outcome> outcomes, int punctuationLines) {
        public Result { outcomes = List.copyOf(outcomes); }

        public Result(String text, List<Outcome> outcomes) { this(text, outcomes, 0); }
    }

    private record Span(int start, int contentEnd, int end, String ending) { }
    private record Edit(int start, int end, String replacement, int id, boolean insertion) { }

    /**
     * A malformed answer (symbols, Japanese, length, tags, several lines) for one point keeps that DRAFT line and lets the other
     * points of the chunk apply; only more than this many malformed answers in one response block the chunk, because the response
     * as a whole is then suspect (owner decision 2026-10-13, plan C2'). Anchoring errors always block.
     */
    private static final int MAX_POINT_LOCAL_FORMAT_REJECTIONS = 1;

    /**
     * A content lock like the ADDRESS lock: the answer removes a glossary target that the DRAFT line already uses for a source term
     * of its RAW. The line stays as the DRAFT and the chunk is not blocked (chapter 017 heading "Guardian", chapter 008 "Zombie Leader").
     */
    static final String GLOSSARY_TARGET_REMOVED = "GLOSSARY_TARGET_REMOVED";

    private TargetedFixApplier() { }

    public static Result apply(String draft, List<FixPoint> points, TargetedFixParser.Parsed parsed,
                               List<EditInputs.GlossaryEntry> glossary) {
        return apply(draft, points, parsed, glossary, List.of());
    }

    /**
     * As {@link #apply(String, List, TargetedFixParser.Parsed, List)}, then restores the RAW's marks on the listed lines: on the
     * accepted answer where a point changed the line, on the DRAFT line otherwise. The model's answer is still checked against
     * the DRAFT's own marks first, so a model never decides the brackets.
     */
    public static Result apply(String draft, List<FixPoint> points, TargetedFixParser.Parsed parsed,
                               List<EditInputs.GlossaryEntry> glossary, List<FixPointFinder.PunctuationRepair> punctuation) {
        String source = draft == null ? "" : draft;
        List<Span> spans = spans(source);
        LineUnits units = LineUnits.parse(source);
        List<Outcome> outcomes = new ArrayList<>();
        List<Edit> edits = new ArrayList<>();
        List<Integer> formatRejections = new ArrayList<>();
        java.util.Map<Integer, String> marks = new java.util.HashMap<>();
        for (FixPointFinder.PunctuationRepair repair : punctuation == null ? List.<FixPointFinder.PunctuationRepair>of() : punctuation) {
            marks.put(repair.draftLineIndex(), repair.rawGroup());
        }
        java.util.Set<Integer> replacedLines = new java.util.HashSet<>();
        int punctuationLines = 0;
        for (FixPoint point : points == null ? List.<FixPoint>of() : points) {
            TargetedFixParser.Answer answer = parsed == null ? null : parsed.answers().get(point.id());
            String before = point.draftText();
            if (answer == null) {
                outcomes.add(new Outcome(point, Status.NO_ANSWER, before, before, "ID_NOT_RETURNED"));
                continue;
            }
            if (answer.keep() || answer.text().isEmpty()) {
                outcomes.add(new Outcome(point, point.insertion() ? Status.NO_ANSWER : Status.KEPT, before, before,
                        point.insertion() ? "EMPTY_INSERTION" : "MODEL_KEEP"));
                continue;
            }
            String candidate = PairText.trim(answer.text());
            String normalized = preserveDraftTerminator(before, candidate);
            boolean restoredTerminator = !normalized.equals(candidate);
            candidate = normalized;
            String reject = rejection(point, candidate, answer.row(), glossary);
            if (reject != null) {
                // The ADDRESS lock only says the rows cannot justify this answer: the line stays as the DRAFT and the other points
                // of the chunk still apply. A malformed answer is decided after the loop (MAX_POINT_LOCAL_FORMAT_REJECTIONS).
                Status status = reject.startsWith(AddressEditGuard.PREFIX) || reject.equals(GLOSSARY_TARGET_REMOVED) ? Status.KEPT : Status.REJECTED;
                if (status == Status.REJECTED) formatRejections.add(outcomes.size());
                String proposed = "";
                if (!point.insertion() && offerable(reject)) {
                    String raw = marks.get(point.draftLineIndex());
                    proposed = raw == null ? candidate : RawPunctuation.restore(candidate, raw);
                }
                outcomes.add(new Outcome(point, status, before, before, reject, proposed, answer.row()));
                continue;
            }
            if (candidate.equals(before)) {
                outcomes.add(new Outcome(point, Status.KEPT, before, before, "UNCHANGED"));
                continue;
            }
            if (point.insertion()) {
                int offset;
                String eol;
                if (point.draftLineIndex() < 0) {
                    int firstUnit = units.size() == 0 ? -1 : 0;
                    offset = firstUnit < 0 ? source.length() : spans.get(units.units().get(firstUnit).physicalLine() - 1).start();
                    eol = nearestEnding(spans);
                } else if (point.draftLineIndex() >= units.size()) {
                    outcomes.add(new Outcome(point, Status.REJECTED, before, before, "INSERT_ANCHOR_OUT_OF_RANGE"));
                    continue;
                } else {
                    int physical = units.units().get(point.draftLineIndex()).physicalLine() - 1;
                    Span anchor = spans.get(physical);
                    offset = anchor.end();
                    eol = anchor.ending().isEmpty() ? nearestEnding(spans) : anchor.ending();
                }
                boolean previousHasNoEnding = point.draftLineIndex() >= 0
                        && spans.get(units.units().get(point.draftLineIndex()).physicalLine() - 1).ending().isEmpty();
                String insertion = previousHasNoEnding ? eol + candidate + eol : candidate + eol;
                edits.add(new Edit(offset, offset, insertion, point.id(), true));
                outcomes.add(new Outcome(point, Status.FIXED, "", candidate, "INSERTED"));
            } else {
                if (point.draftLineIndex() >= units.size()) {
                    outcomes.add(new Outcome(point, Status.REJECTED, before, before, "DRAFT_LINE_OUT_OF_RANGE"));
                    continue;
                }
                int physical = units.units().get(point.draftLineIndex()).physicalLine() - 1;
                Span target = spans.get(physical);
                String raw = marks.get(point.draftLineIndex());
                if (raw != null) {
                    String restored = RawPunctuation.restore(candidate, raw);
                    if (!restored.equals(candidate)) punctuationLines++;
                    candidate = restored;
                }
                replacedLines.add(point.draftLineIndex());
                edits.add(new Edit(target.start(), target.contentEnd(), candidate, point.id(), false));
                outcomes.add(new Outcome(point, Status.FIXED, before, candidate,
                        restoredTerminator ? "REPLACED_WITH_DRAFT_TERMINATOR" : "REPLACED"));
            }
        }
        // a rejected point added no edit, so turning it into KEPT changes no text
        if (!formatRejections.isEmpty() && formatRejections.size() <= MAX_POINT_LOCAL_FORMAT_REJECTIONS) {
            for (int at : formatRejections) {
                Outcome rejected = outcomes.get(at);
                outcomes.set(at, new Outcome(rejected.point(), Status.KEPT, rejected.before(), rejected.after(), rejected.reason(),
                        rejected.proposed(), rejected.citedRow()));
            }
        }
        // lines no answer replaced keep their DRAFT words and only take the RAW's marks
        for (java.util.Map.Entry<Integer, String> mark : marks.entrySet()) {
            int line = mark.getKey();
            if (replacedLines.contains(line) || line < 0 || line >= units.size()) continue;
            Span target = spans.get(units.units().get(line).physicalLine() - 1);
            String original = source.substring(target.start(), target.contentEnd());
            String restored = RawPunctuation.restore(original, mark.getValue());
            if (restored.equals(original)) continue;
            edits.add(new Edit(target.start(), target.contentEnd(), restored, 0, false));
            punctuationLines++;
        }
        edits.sort(Comparator.comparingInt(Edit::start).reversed()
                .thenComparing(Edit::insertion) // replacements first at a shared boundary
                .thenComparing(Comparator.comparingInt(Edit::id).reversed()));
        StringBuilder output = new StringBuilder(source);
        for (Edit edit : edits) output.replace(edit.start(), edit.end(), edit.replacement());
        return new Result(output.toString(), outcomes, punctuationLines);
    }

    /**
     * A person applies a refused proposal to a stored chunk result: the line at {@code draftLineIndex}, moved down by the insertions
     * placed before it, is replaced by {@code proposed}. Every other character is kept.
     *
     * @param insertionsBefore applied MISSING insertions whose anchor is before this line (anchor index lower than this line)
     * @param draftLine the DRAFT line the point was found on; the result line must still carry the same words, so a result that
     *                  changed (another edit, a different chunk text) is refused rather than overwritten
     * @return the new chunk text, or null when the line is not where it should be
     */
    public static String applyProposal(String chunkText, int draftLineIndex, int insertionsBefore, String draftLine, String proposed) {
        if (chunkText == null || proposed == null || proposed.isBlank() || draftLineIndex < 0 || insertionsBefore < 0) return null;
        if (proposed.indexOf('\n') >= 0 || proposed.indexOf('\r') >= 0) return null;
        LineUnits units = LineUnits.parse(chunkText);
        int at = draftLineIndex + insertionsBefore;
        if (at >= units.size()) return null;
        Span span = spans(chunkText).get(units.units().get(at).physicalLine() - 1);
        String current = chunkText.substring(span.start(), span.contentEnd());
        if (!wording(current).equals(wording(draftLine))) return null;
        return chunkText.substring(0, span.start()) + PairText.trim(proposed) + chunkText.substring(span.contentEnd());
    }

    /** Letters and digits only: marks restored from the RAW change punctuation, never the words. */
    private static String wording(String text) {
        StringBuilder out = new StringBuilder();
        String nfc = java.text.Normalizer.normalize(text == null ? "" : text, java.text.Normalizer.Form.NFC);
        nfc.codePoints().filter(Character::isLetterOrDigit).forEach(out::appendCodePoint);
        return out.toString();
    }

    private static String rejection(FixPoint point, String candidate, int citedRow, List<EditInputs.GlossaryEntry> glossary) {
        if (candidate.isEmpty()) return "EMPTY_CANDIDATE";
        if (candidate.indexOf('\n') >= 0 || candidate.indexOf('\r') >= 0) return "MULTILINE_CANDIDATE";
        if (candidate.indexOf('<') >= 0 || candidate.indexOf('>') >= 0 || candidate.contains("```")
                || candidate.matches("(?i)^\\s*(here is|here's|below is|ghi chú|notes?)\\b.*")) return "META_OR_TAG";
        boolean sourceScript = RawAlignedNormalizer.containsKanaOrHan(candidate);
        String before = point.draftText();
        if (sourceScript && !RawAlignedNormalizer.containsKanaOrHan(before) && !authorizedGlossarySource(candidate, glossary)) return "NEW_KANA_OR_HAN";
        if (!point.insertion()) {
            if (!protectedSymbols(before, point).equals(protectedSymbols(candidate, point))) return "PROTECTED_SYMBOL_CHANGE";
            if (removesGlossaryTarget(point, before, candidate, glossary)) return GLOSSARY_TARGET_REMOVED;
            String address = AddressEditGuard.rejection(point, candidate, citedRow);
            if (address != null) return address;
            int oldLetters = PairText.letters(before);
            int newLetters = PairText.letters(candidate);
            if (oldLetters == 0 || 2L * newLetters < oldLetters || (long) newLetters > 2L * oldLetters) return "LINE_LENGTH_RATIO";
        }
        return null;
    }

    /** A generated Japanese full stop must not replace the existing target-language
     * full stop immediately before the same dialogue close. No words are changed. */
    private static String preserveDraftTerminator(String before, String candidate) {
        for (char close : new char[] {'」', '』', '”', '’', '"', '\''}) {
            if (before.endsWith("." + close) && candidate.endsWith("。" + close) && before.indexOf('。') < 0
                    && candidate.indexOf('。') == candidate.length() - 2) {
                return candidate.substring(0, candidate.length() - 2) + "." + close;
            }
        }
        return candidate;
    }

    /** Preserve dialogue, frames and special punctuation without inventing replacements.
     * Kana-only ruby brackets may disappear with a KANA repair; ordinary brackets stay protected. */
    private static String protectedSymbols(String text, FixPoint point) {
        if (point.types().contains(FixPoint.Type.KANA)) text = text.replaceAll("《[\\p{IsHiragana}\\p{IsKatakana}ー]+》", "");
        String protectedSet = "\"'「」『』“”‘’«»【】〔〕〈〉《》［］｛｝（）[]{}()◆◇＊…—─!?！？。";
        StringBuilder out = new StringBuilder();
        text.codePoints().filter(c -> protectedSet.indexOf(c) >= 0
                || Character.getType(c) == Character.OTHER_SYMBOL
                || Character.getType(c) == Character.MATH_SYMBOL
                || Character.getType(c) == Character.CURRENCY_SYMBOL).forEach(out::appendCodePoint);
        return out.toString();
    }

    /**
     * True when the DRAFT line uses a glossary target whose source is in the point's RAW and the answer drops that target. Not for
     * ADDRESS points: a pronoun row may turn a surname into its call name, and the ADDRESS lock already limits what may change there.
     */
    private static boolean removesGlossaryTarget(FixPoint point, String before, String candidate, List<EditInputs.GlossaryEntry> glossary) {
        if (glossary == null || point.types().contains(FixPoint.Type.ADDRESS)) return false;
        String raw = java.text.Normalizer.normalize(point.rawText(), java.text.Normalizer.Form.NFC);
        String draft = java.text.Normalizer.normalize(before, java.text.Normalizer.Form.NFC).toLowerCase(java.util.Locale.ROOT);
        String answer = java.text.Normalizer.normalize(candidate, java.text.Normalizer.Form.NFC).toLowerCase(java.util.Locale.ROOT);
        for (EditInputs.GlossaryEntry term : glossary) {
            String source = java.text.Normalizer.normalize(term.source().trim(), java.text.Normalizer.Form.NFC);
            String target = java.text.Normalizer.normalize(term.target().trim(), java.text.Normalizer.Form.NFC).toLowerCase(java.util.Locale.ROOT);
            if (source.isEmpty() || target.isEmpty() || !raw.contains(source)) continue;
            if (draft.contains(target) && !answer.contains(target)) return true;
        }
        return false;
    }

    private static boolean authorizedGlossarySource(String candidate, List<EditInputs.GlossaryEntry> glossary) {
        if (glossary == null) return false;
        for (EditInputs.GlossaryEntry term : glossary) {
            String source = term.source().trim();
            if (!source.isEmpty() && candidate.contains(source)) return true;
        }
        return false;
    }

    private static List<Span> spans(String text) {
        List<Span> out = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int contentEnd = start;
            while (contentEnd < text.length() && text.charAt(contentEnd) != '\n' && text.charAt(contentEnd) != '\r') contentEnd++;
            int end = contentEnd;
            if (end < text.length()) {
                if (text.charAt(end) == '\r' && end + 1 < text.length() && text.charAt(end + 1) == '\n') end += 2;
                else end++;
            }
            out.add(new Span(start, contentEnd, end, text.substring(contentEnd, end)));
            start = end;
        }
        if (out.isEmpty()) out.add(new Span(0, 0, 0, ""));
        return out;
    }

    private static String nearestEnding(List<Span> spans) {
        for (Span span : spans) if (!span.ending().isEmpty()) return span.ending();
        return "\n";
    }
}
