package com.ml.tblandroidtxt.editorial.api.chunk;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CS-1 step S1: pairs the files of a library by what the file name says about the chapter, never by the chapter number of
 * the story. In the owner's web-novel set a RAW is named {@code 0043-042　<title>.txt}: four digits are the position of the
 * file, three digits the chapter number in the story (two settings files sit between, so the two drift apart), while DRAFT,
 * FINAL, glossary and pronoun files are numbered by the position of the file ({@code 043_<title>_translated.txt}). So:
 * a RAW and a DRAFT with the same non-empty title are a pair; files without a title (the light-novel set is named by number
 * only) are paired by position; the glossary and pronoun of a DRAFT are the files with the same position. A person always
 * confirms the pairs; the names are only a suggestion.
 */
public final class SourceTitle {
    private static final Pattern EXTENSION = Pattern.compile("\\.[^./\\\\]*$");
    private static final Pattern NUMBER_PREFIX = Pattern.compile("^(\\d{3,4})(?:-\\d{3})?");
    private static final Pattern COPY_SUFFIX = Pattern.compile("\\(\\d+\\)$");
    private static final Pattern SUFFIX = Pattern.compile("(?:_v\\d+|_translated|_FINAL_QA.*|_chapter_glossary|(?:_[A-Z][A-Z0-9]*)+)$");
    private static final Pattern ROLE_WORD = Pattern.compile("^(?:RAW|DRAFT|PRONOUN|GLOSSARY)(?:_|$)");
    private static final Pattern POSITION = Pattern.compile("^(\\d{3,4})");

    public record Pair(int rawIndex, int draftIndex, String method) { }

    private SourceTitle() { }

    /** The chapter title a file name carries, or {@code ""} when it carries none (only a number, a role and a series). */
    public static String of(String fileName) {
        String s = Normalizer.normalize(fileName == null ? "" : fileName, Normalizer.Form.NFKC);
        s = EXTENSION.matcher(s).replaceFirst("");
        s = NUMBER_PREFIX.matcher(s).replaceFirst("");
        s = COPY_SUFFIX.matcher(s).replaceFirst("");
        for (int i = 0; i < 4; i++) {
            Matcher m = SUFFIX.matcher(s);
            if (!m.find()) break;
            s = s.substring(0, m.start());
        }
        s = s.replaceFirst("^[\\s_\\-]+", "");
        s = ROLE_WORD.matcher(s).replaceFirst("");
        return s.replaceAll("\\s+", " ").trim();
    }

    /** Position of the file in its set: the leading number of the name, or {@code -1}. */
    public static int position(String fileName) {
        Matcher m = POSITION.matcher(fileName == null ? "" : fileName);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /**
     * Pairs RAW names with DRAFT names: equal non-empty titles that are unique on both sides first, then the files left over
     * (including those without a title) by position when as many are left on each side.
     */
    public static List<Pair> pair(List<String> rawNames, List<String> draftNames) {
        List<Pair> out = new ArrayList<>();
        boolean[] rawUsed = new boolean[rawNames.size()];
        boolean[] draftUsed = new boolean[draftNames.size()];
        Map<String, List<Integer>> rawByTitle = byTitle(rawNames);
        Map<String, List<Integer>> draftByTitle = byTitle(draftNames);
        for (Map.Entry<String, List<Integer>> e : rawByTitle.entrySet()) {
            if (e.getKey().isEmpty()) continue;
            List<Integer> d = draftByTitle.get(e.getKey());
            if (e.getValue().size() == 1 && d != null && d.size() == 1) {
                out.add(new Pair(e.getValue().get(0), d.get(0), "TITLE"));
                rawUsed[e.getValue().get(0)] = true;
                draftUsed[d.get(0)] = true;
            }
        }
        List<Integer> rawLeft = leftover(rawNames, rawUsed);
        List<Integer> draftLeft = leftover(draftNames, draftUsed);
        if (rawLeft.size() == draftLeft.size()) {
            for (int i = 0; i < rawLeft.size(); i++) out.add(new Pair(rawLeft.get(i), draftLeft.get(i), "ORDER"));
        }
        out.sort(Comparator.comparingInt(Pair::rawIndex));
        return out;
    }

    /** For every DRAFT name the index of the name in {@code others} (glossary, pronoun, FINAL) with the same position, or -1. */
    public static int[] attach(List<String> draftNames, List<String> others) {
        Map<Integer, Integer> byPosition = new HashMap<>();
        for (int i = 0; i < others.size(); i++) {
            int p = position(others.get(i));
            if (p >= 0) byPosition.putIfAbsent(p, i);
        }
        int[] out = new int[draftNames.size()];
        for (int i = 0; i < out.length; i++) out[i] = byPosition.getOrDefault(position(draftNames.get(i)), -1);
        return out;
    }

    private static Map<String, List<Integer>> byTitle(List<String> names) {
        Map<String, List<Integer>> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) m.computeIfAbsent(of(names.get(i)), k -> new ArrayList<>()).add(i);
        return m;
    }

    private static List<Integer> leftover(List<String> names, boolean[] used) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) if (!used[i]) out.add(i);
        out.sort(Comparator.<Integer>comparingInt(i -> position(names.get(i))).thenComparing(i -> names.get(i)));
        return out;
    }
}
