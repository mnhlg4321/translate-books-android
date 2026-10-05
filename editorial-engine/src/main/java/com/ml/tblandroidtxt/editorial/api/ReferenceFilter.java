package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Which reference lines reach the prompt (plan section 3.1). No count limit: the edit call sees every glossary entry whose
 * source occurs in RAW and every pronoun row that names a word of RAW. Pronoun rows are never invented from the glossary.
 */
public final class ReferenceFilter {
    /** Pronoun columns of the current format; a three-column legacy row only has {@code from}, {@code target}, {@code note}. */
    public static final int PRONOUN_COLUMNS = 7;

    private ReferenceFilter() { }

    public static List<EditInputs.GlossaryEntry> glossary(String raw, List<EditInputs.GlossaryEntry> entries) {
        List<EditInputs.GlossaryEntry> kept = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (EditInputs.GlossaryEntry entry : entries) {
            String source = entry.source().trim();
            if (source.isEmpty() || entry.target().isBlank() || !raw.contains(source)) continue;
            if (seen.add(source + "\u0000" + entry.target().trim())) kept.add(entry);
        }
        return kept;
    }

    /** Pronoun rows as they were written (header and blank lines removed) that mention RAW. */
    public static List<String> pronounRows(String raw, String csv) {
        List<String> kept = new ArrayList<>();
        if (csv == null) return kept;
        for (String line : csv.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String row = line.trim();
            if (row.isEmpty() || row.startsWith("#")) continue;
            List<String> cells = cells(row);
            if (isHeader(cells)) continue;
            if (mentions(raw, cells)) kept.add(row);
        }
        return kept;
    }

    private static boolean isHeader(List<String> cells) {
        if (cells.isEmpty()) return true;
        String first = cells.get(0).trim().toLowerCase(java.util.Locale.ROOT);
        return first.equals("from") || first.equals("source") || first.equals("nguồn");
    }

    private static boolean mentions(String raw, List<String> cells) {
        List<String> keys = new ArrayList<>();
        if (cells.size() >= PRONOUN_COLUMNS) {
            keys.add(cells.get(0));
            keys.add(cells.get(1));
            keys.add(cells.get(2));
        } else if (!cells.isEmpty()) {
            keys.add(cells.get(0));
        }
        boolean anyKey = false;
        for (String key : keys) {
            String trimmed = key.trim();
            if (trimmed.isEmpty() || trimmed.equals("*") || trimmed.equals("-")) continue;
            anyKey = true;
            if (raw.contains(trimmed)) return true;
        }
        // a row that names nothing in particular applies everywhere
        return !anyKey;
    }

    /** Minimal CSV cell split: commas separate, a double-quoted cell may contain commas and doubled quotes. */
    static List<String> cells(String row) {
        List<String> out = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < row.length() && row.charAt(i + 1) == '"') { cell.append('"'); i++; }
                    else quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"' && cell.length() == 0) {
                quoted = true;
            } else if (c == ',') {
                out.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        out.add(cell.toString());
        return out;
    }
}
