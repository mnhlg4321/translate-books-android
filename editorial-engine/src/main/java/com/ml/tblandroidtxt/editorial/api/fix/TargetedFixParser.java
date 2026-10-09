package com.ml.tblandroidtxt.editorial.api.fix;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tolerant, bounded line parser. Bad envelope decoration is ignored; it never invents a point id. */
public final class TargetedFixParser {
    public record Answer(String text, boolean keep) { }
    public record Parsed(Map<Integer, Answer> answers, int ignoredLines) {
        public Parsed { answers = Map.copyOf(answers); }
    }

    private static final Pattern BRACKET = Pattern.compile("^\\[(\\d+)]\\s*(.*)$");
    private static final Pattern NUMBER_DOT = Pattern.compile("^(\\d+)[.)]\\s*(.*)$");
    private static final Pattern NUMBER_PARENS = Pattern.compile("^\\((\\d+)\\)\\s*(.*)$");
    private static final int MAX_RESPONSE_CHARS = 100_000;

    private TargetedFixParser() { }

    public static Parsed parse(String response, List<FixPoint> points) {
        Set<Integer> expected = new java.util.HashSet<>();
        if (points != null) for (FixPoint point : points) expected.add(point.id());
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
            String value = unquote(matcher.group(2).trim());
            boolean keep = "=".equals(value);
            answers.put(id, new Answer(keep ? "" : value, keep));
        }
        return new Parsed(answers, ignored);
    }

    private static String unquote(String value) {
        if (value.length() < 2) return value;
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        boolean pair = (first == '"' && last == '"') || (first == '“' && last == '”')
                || (first == '「' && last == '」') || (first == '『' && last == '』')
                || (first == '‘' && last == '’') || (first == '\'' && last == '\'');
        return pair ? value.substring(1, value.length() - 1).trim() : value;
    }
}
