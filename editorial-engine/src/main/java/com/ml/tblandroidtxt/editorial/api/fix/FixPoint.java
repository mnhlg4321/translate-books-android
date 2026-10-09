package com.ml.tblandroidtxt.editorial.api.fix;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** One app-detected, RAW-anchored target. Line indices count non-blank DRAFT lines from zero. */
public record FixPoint(int id, Set<Type> types, int draftLineIndex, String draftText, String rawText, List<String> rules) {
    public enum Type { ADDRESS, GLOSSARY, KANA, MISSING }

    public FixPoint {
        if (id < 1) throw new IllegalArgumentException("FIX_POINT_ID_INVALID");
        types = types == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(types));
        draftText = draftText == null ? "" : draftText;
        rawText = rawText == null ? "" : rawText;
        rules = rules == null ? List.of() : List.copyOf(rules);
        if (types.isEmpty() || rules.isEmpty()) throw new IllegalArgumentException("FIX_POINT_WITHOUT_RULE");
        if (draftLineIndex < -1) throw new IllegalArgumentException("FIX_POINT_LINE_INVALID");
        if (types.contains(Type.MISSING)) {
            if (types.size() != 1 || !draftText.isEmpty() || rawText.isBlank()) throw new IllegalArgumentException("MISSING_POINT_INVALID");
        } else if (draftLineIndex < 0 || draftText.isBlank() || rawText.isBlank()) {
            throw new IllegalArgumentException("FIX_POINT_ANCHOR_INVALID");
        }
    }

    public boolean insertion() { return types.contains(Type.MISSING); }

    public String typesText() {
        StringBuilder out = new StringBuilder();
        for (Type type : Type.values()) if (types.contains(type)) {
            if (out.length() > 0) out.append(", ");
            out.append(type.name());
        }
        return out.toString();
    }
}
