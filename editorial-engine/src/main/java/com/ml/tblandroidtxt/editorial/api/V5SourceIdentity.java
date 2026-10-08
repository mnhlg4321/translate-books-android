package com.ml.tblandroidtxt.editorial.api;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The chain identity of one V5 source pack: the chapter {@code ID} and the {@code SERIES}. The pack names its outputs
 * {@code [ID]_FINAL_QA_[SERIES].txt}, so a run without both is refused before anything is sent. A library that already knows
 * the two values passes them explicitly; the file-based runner derives them from the original file names.
 */
public record V5SourceIdentity(String chainId, String series) {
    public static final V5SourceIdentity NONE = new V5SourceIdentity("", "");
    private static final Pattern ID = Pattern.compile("[0-9A-Za-z][0-9A-Za-z._-]{0,31}");
    private static final Pattern SERIES = Pattern.compile("[0-9A-Za-z][0-9A-Za-z._-]{0,119}");
    private static final Pattern NAME = Pattern.compile("^([0-9]{3,6})_(.+)$");
    /** Role words that belong to a file name but not to the series, per role. */
    private static final Map<String, List<String>> ROLE_WORDS = Map.of(
            "RAW", List.of("RAW"),
            "DRAFT", List.of("RAW", "DRAFT", "TRANSLATED"),
            "GLOSSARY", List.of("CHAPTER", "GLOSSARY"),
            "PRONOUN", List.of("PRONOUN"));

    public V5SourceIdentity {
        chainId = chainId == null ? "" : chainId.trim();
        series = series == null ? "" : series.trim();
    }

    public boolean complete() { return !chainId.isEmpty() && !series.isEmpty(); }

    /** Empty when both parts are present and well formed; otherwise the typed stop code. */
    public String problem() {
        if (!complete()) return "V5_IDENTITY_MISSING";
        if (!ID.matcher(chainId).matches() || !SERIES.matcher(series).matches()) return "V5_IDENTITY_INVALID";
        return "";
    }

    public record Derivation(String code, V5SourceIdentity identity) {
        public boolean valid() { return code.isEmpty(); }
    }

    /**
     * Reads {@code NNN_<series words>.<ext>} from each original file name, removes the role words of that file, and requires
     * every file to agree. Names that do not follow the pattern give {@code V5_IDENTITY_MISSING}; files that disagree give
     * {@code V5_IDENTITY_MISMATCH}.
     */
    public static Derivation derive(List<EditInputs.OriginalSourceFile> files) {
        if (files == null || files.isEmpty()) return new Derivation("V5_IDENTITY_MISSING", NONE);
        V5SourceIdentity first = null;
        for (EditInputs.OriginalSourceFile file : files) {
            V5SourceIdentity one = fromName(file.role(), file.name());
            if (!one.complete()) return new Derivation("V5_IDENTITY_MISSING", NONE);
            if (first == null) first = one;
            else if (!first.equals(one)) return new Derivation("V5_IDENTITY_MISMATCH", NONE);
        }
        String problem = first.problem();
        return new Derivation(problem, problem.isEmpty() ? first : NONE);
    }

    static V5SourceIdentity fromName(String role, String name) {
        String stem = name == null ? "" : name;
        int dot = stem.lastIndexOf('.');
        if (dot > 0) stem = stem.substring(0, dot);
        Matcher m = NAME.matcher(stem);
        if (!m.matches()) return NONE;
        List<String> drop = ROLE_WORDS.getOrDefault(role == null ? "" : role, List.of());
        StringBuilder series = new StringBuilder();
        for (String word : m.group(2).split("_")) {
            if (word.isEmpty() || drop.contains(word.toUpperCase(Locale.ROOT))) continue;
            if (series.length() > 0) series.append('_');
            series.append(word);
        }
        return new V5SourceIdentity(m.group(1), series.toString());
    }
}
