package com.ml.tblandroidtxt.editorial.api.chunk;

import com.ml.tblandroidtxt.editorial.api.EditInputs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * CS-1 alignment anchors. A glossary term anchors a RAW line when its source form occurs in the line and a DRAFT line when its
 * target form (case-insensitive) occurs in it; digits anchor both sides after full-width digits are brought to ASCII. CJK
 * numerals are not digits. An anchor shared by the two lines of a pair lowers the pair's cost, a missing one raises it.
 */
public final class Anchors {
    public record Term(String source, String targetLower) { }


    private Anchors() { }

    /** Glossary entries with a source and a target; the target is stripped and lower-cased. */
    public static List<Term> terms(List<EditInputs.GlossaryEntry> entries) {
        List<Term> out = new ArrayList<>();
        if (entries == null) return out;
        for (EditInputs.GlossaryEntry e : entries) {
            String source = LineUnits.strip(e.source());
            String target = LineUnits.strip(e.target());
            if (!source.isEmpty() && !target.isEmpty()) out.add(new Term(source, target.toLowerCase(Locale.ROOT)));
        }
        return out;
    }

    public static Set<String> targets(List<Term> terms) {
        Set<String> out = new HashSet<>();
        for (Term t : terms) out.add(t.targetLower());
        return out;
    }

    public static Set<String> raw(String text, List<Term> terms) {
        Set<String> out = new HashSet<>();
        for (Term t : terms) if (text.contains(t.source())) out.add(t.targetLower());
        out.addAll(numbers(text));
        return out;
    }

    public static Set<String> draft(String text, List<Term> terms) {
        String low = text.toLowerCase(Locale.ROOT);
        Set<String> out = new HashSet<>();
        for (Term t : terms) if (low.contains(t.targetLower())) out.add(t.targetLower());
        out.addAll(numbers(text));
        return out;
    }

    public static Set<String> numbers(String text) {
        StringBuilder ascii = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            ascii.append(c >= '０' && c <= '９' ? (char) ('0' + (c - '０')) : c);
        }
        // runs of decimal digits (Unicode Nd, like the reference tooling); plain code, because Android regex lacks UNICODE_CHARACTER_CLASS
        Set<String> out = new HashSet<>();
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < ascii.length(); ) {
            int cp = ascii.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isDigit(cp)) run.appendCodePoint(cp);
            else if (run.length() > 0) { out.add(run.toString()); run.setLength(0); }
        }
        if (run.length() > 0) out.add(run.toString());
        return out;
    }
}
