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
    public record Outcome(FixPoint point, Status status, String before, String after, String reason) { }
    public record Result(String text, List<Outcome> outcomes) {
        public Result { outcomes = List.copyOf(outcomes); }
    }

    private record Span(int start, int contentEnd, int end, String ending) { }
    private record Edit(int start, int end, String replacement, int id, boolean insertion) { }

    private TargetedFixApplier() { }

    public static Result apply(String draft, List<FixPoint> points, TargetedFixParser.Parsed parsed,
                               List<EditInputs.GlossaryEntry> glossary) {
        String source = draft == null ? "" : draft;
        List<Span> spans = spans(source);
        LineUnits units = LineUnits.parse(source);
        List<Outcome> outcomes = new ArrayList<>();
        List<Edit> edits = new ArrayList<>();
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
            String reject = rejection(point, candidate, glossary);
            if (reject != null) {
                outcomes.add(new Outcome(point, Status.REJECTED, before, before, reject));
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
                edits.add(new Edit(target.start(), target.contentEnd(), candidate, point.id(), false));
                outcomes.add(new Outcome(point, Status.FIXED, before, candidate,
                        restoredTerminator ? "REPLACED_WITH_DRAFT_TERMINATOR" : "REPLACED"));
            }
        }
        edits.sort(Comparator.comparingInt(Edit::start).reversed()
                .thenComparing(Edit::insertion) // replacements first at a shared boundary
                .thenComparing(Comparator.comparingInt(Edit::id).reversed()));
        StringBuilder output = new StringBuilder(source);
        for (Edit edit : edits) output.replace(edit.start(), edit.end(), edit.replacement());
        return new Result(output.toString(), outcomes);
    }

    private static String rejection(FixPoint point, String candidate, List<EditInputs.GlossaryEntry> glossary) {
        if (candidate.isEmpty()) return "EMPTY_CANDIDATE";
        if (candidate.indexOf('\n') >= 0 || candidate.indexOf('\r') >= 0) return "MULTILINE_CANDIDATE";
        if (candidate.indexOf('<') >= 0 || candidate.indexOf('>') >= 0 || candidate.contains("```")
                || candidate.matches("(?i)^\\s*(here is|here's|below is|ghi chú|notes?)\\b.*")) return "META_OR_TAG";
        boolean sourceScript = RawAlignedNormalizer.containsKanaOrHan(candidate);
        String before = point.draftText();
        if (sourceScript && !RawAlignedNormalizer.containsKanaOrHan(before) && !authorizedGlossarySource(candidate, glossary)) return "NEW_KANA_OR_HAN";
        if (!point.insertion()) {
            if (!protectedSymbols(before, point).equals(protectedSymbols(candidate, point))) return "PROTECTED_SYMBOL_CHANGE";
            String address = AddressEditGuard.rejection(point, candidate);
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
