package com.ml.tblandroidtxt.editorial.api.fix;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tolerant, bounded line parser. Bad envelope decoration is ignored; it never invents a point id. */
public final class TargetedFixParser {
    /**
     * One answer. {@code row} is the pronoun row an ADDRESS answer cites ({@code [id] R<n> <line>}): 1-based index into the point's
     * address rows, {@link #NOT_CITED} when the answer names no row, {@link #INVALID_ROW} when it names a row the point does not have.
     */
    public record Answer(String text, boolean keep, int row) {
        public static final int NOT_CITED = 0;
        public static final int INVALID_ROW = -1;

        public Answer(String text, boolean keep) { this(text, keep, NOT_CITED); }
    }
    public record Parsed(Map<Integer, Answer> answers, int ignoredLines) {
        public Parsed { answers = Map.copyOf(answers); }
    }

    private static final Pattern BRACKET = Pattern.compile("^\\[(\\d+)]\\s*(.*)$");
    private static final Pattern NUMBER_DOT = Pattern.compile("^(\\d+)[.)]\\s*(.*)$");
    private static final Pattern NUMBER_PARENS = Pattern.compile("^\\((\\d+)\\)\\s*(.*)$");
    private static final Pattern ROW_CITATION = Pattern.compile("^[Rr](\\d{1,3})(?:\\s+(.*))?$");
    private static final int MAX_RESPONSE_CHARS = 100_000;

    private TargetedFixParser() { }

    public static Parsed parse(String response, List<FixPoint> points) {
        Set<Integer> expected = new java.util.HashSet<>();
        Map<Integer, Integer> rowCounts = new java.util.HashMap<>();
        if (points != null) for (FixPoint point : points) {
            expected.add(point.id());
            if (point.types().contains(FixPoint.Type.ADDRESS) && !point.addressRows().isEmpty()) rowCounts.put(point.id(), point.addressRows().size());
        }
        String text = response == null ? "" : response;
        if (text.length() > MAX_RESPONSE_CHARS) text = text.substring(0, MAX_RESPONSE_CHARS);
        text = text.replace("<EDITED>", "").replace("</EDITED>", "").replace("<EDIT>", "").replace("</EDIT>", "");
        Map<Integer, Answer> answers = new LinkedHashMap<>();
        int ignored = 0;
        for (String rawLine : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            String line = rawLine.trim();
            if (line.startsWith("```") || line.equals("**") || line.equals("---")) continue;
            if (line.startsWith("**") && line.endsWith("**") && line.length() >= 4) line = line.substring(2, line.length() - 2).trim();
            Matcher matcher = BRACKET.matcher(line);
            if (!matcher.matches()) matcher = NUMBER_DOT.matcher(line);
            if (!matcher.matches()) matcher = NUMBER_PARENS.matcher(line);
            if (!matcher.matches()) {
                if (!line.isEmpty()) ignored++;
                continue;
            }
            int id;
            try { id = Integer.parseInt(matcher.group(1)); }
            catch (NumberFormatException invalid) { ignored++; continue; }
            if (!expected.contains(id) || answers.containsKey(id)) { ignored++; continue; }
            // Everything after the id is book content. Quote marks are not an envelope.
            String value = matcher.group(2).trim();
            int row = Answer.NOT_CITED;
            // only a point with numbered pronoun rows can cite one; elsewhere a leading "R2" is book text
            Matcher citation = rowCounts.containsKey(id) ? ROW_CITATION.matcher(value) : null;
            if (citation != null && citation.matches()) {
                int cited = Integer.parseInt(citation.group(1));
                row = cited >= 1 && cited <= rowCounts.get(id) ? cited : Answer.INVALID_ROW;
                value = citation.group(2) == null ? "" : citation.group(2).trim();
            }
            boolean keep = "=".equals(value);
            answers.put(id, new Answer(keep ? "" : value, keep, row));
        }
        return new Parsed(answers, ignored);
    }

}
