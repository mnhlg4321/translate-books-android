package com.ml.tblandroidtxt.editorial.api.fix;

import java.util.ArrayList;
import java.util.List;

/**
 * Owner convention (2026-10-13, plan P2): a DRAFT line follows its RAW for the brackets 〝〟 「」 『』, the colon ： and the ellipsis ……
 * Deterministic and model-free; a mark is only replaced when the line and its RAW hold the same number of them, so nothing is ever
 * inserted at a guessed position, and no letter changes.
 */
public final class RawPunctuation {
    private static final String RAW_OPEN = "「『〝";
    private static final String RAW_CLOSE = "」』〟";

    private RawPunctuation() { }

    /** @return the DRAFT line with the RAW's marks, or the line unchanged when the marks cannot be matched one to one */
    public static String restore(String draftLine, String rawGroup) {
        if (draftLine == null || rawGroup == null || draftLine.isEmpty()) return draftLine;
        String out = ellipses(colons(quotes(draftLine, rawGroup), rawGroup), rawGroup);
        return wording(out).equals(wording(draftLine)) ? out : draftLine;
    }

    /** The letters and digits of a line in order: what a mark repair may never change. */
    private static String wording(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().filter(Character::isLetterOrDigit).forEach(out::appendCodePoint);
        return out.toString();
    }

    /** “…” or "…" pairs take the RAW bracket of the same position, when both hold the same number of pairs and the line has no RAW bracket yet. */
    private static String quotes(String line, String raw) {
        List<Integer> opens = new ArrayList<>();
        for (int i = 0; i < raw.length(); i++) if (RAW_OPEN.indexOf(raw.charAt(i)) >= 0) opens.add(RAW_OPEN.indexOf(raw.charAt(i)));
        if (opens.isEmpty()) return line;
        for (int i = 0; i < line.length(); i++) if (RAW_OPEN.indexOf(line.charAt(i)) >= 0 || RAW_CLOSE.indexOf(line.charAt(i)) >= 0) return line;
        String curly = pairs(line, '“', '”', opens);
        if (curly != null) return curly;
        String straight = pairs(line, '"', '"', opens);
        return straight != null ? straight : line;
    }

    private static String pairs(String line, char open, char close, List<Integer> opens) {
        StringBuilder out = new StringBuilder(line.length());
        int pair = 0;
        boolean inside = false;
        boolean any = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            boolean isOpen = open == close ? c == open && !inside : c == open;
            boolean isClose = open == close ? c == close && inside : c == close;
            if (isOpen) {
                if (inside || pair >= opens.size()) return null;
                out.append(RAW_OPEN.charAt(opens.get(pair)));
                inside = true;
                any = true;
            } else if (isClose) {
                if (!inside) return null;
                out.append(RAW_CLOSE.charAt(opens.get(pair)));
                inside = false;
                pair++;
            } else {
                out.append(c);
            }
        }
        return any && !inside && pair == opens.size() ? out.toString() : null;
    }

    private static String colons(String line, String raw) {
        int want = count(raw, "：");
        if (want == 0 || count(line, ":") != want) return line;
        return line.replace(": ", "：").replace(":", "：");
    }

    private static String ellipses(String line, String raw) {
        int want = count(raw, "……");
        if (want == 0 || count(line, "...") != want) return line;
        return line.replace("...", "……");
    }

    private static int count(String text, String mark) {
        int n = 0;
        for (int at = text.indexOf(mark); at >= 0; at = text.indexOf(mark, at + mark.length())) n++;
        return n;
    }
}
