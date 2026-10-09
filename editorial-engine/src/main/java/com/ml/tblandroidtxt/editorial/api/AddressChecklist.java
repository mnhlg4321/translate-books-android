package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Where to look for a wrong form of address in one chunk, computed by the app from what is certain: the pronoun rows that
 * name a {@code call}, the paragraph ranges they are scoped to, which RAW paragraphs carry a quotation, and which address
 * words the DRAFT paragraph in the same position uses. It never decides who speaks or who is addressed and never says what to
 * write: the model reads RAW for that. Paragraphs are matched by position, so a chunk whose RAW and DRAFT paragraph counts
 * differ gets no checklist (fail closed). The text carries paragraph labels and word counts only, no book text. Pure.
 */
public final class AddressChecklist {
    /** Vietnamese words that can be a form of address or a first-person word; polysemous ones are the reason this only lists. */
    static final Set<String> WORDS = Set.of("cậu", "bạn", "anh", "chị", "em", "cô", "ông", "bà", "mày", "ngươi", "ngài", "chú", "bác",
            "thầy", "tớ", "tao", "ta", "tui", "tôi");

    private AddressChecklist() { }

    /** @return the checklist lines for the system prompt, or an empty list when there is nothing to check */
    public static List<String> build(ReferenceProjector.Projection projection, String rawMain, String draftMain, int firstParagraph) {
        List<String> rawBlocks = paragraphs(rawMain);
        List<String> draftBlocks = paragraphs(draftMain);
        List<String> out = new ArrayList<>();
        if (rawBlocks.isEmpty()) return out;
        if (rawBlocks.size() != draftBlocks.size()) {
            // the DRAFT may run two paragraphs together without a blank line; when every RAW paragraph is one line, line n pairs with line n
            List<String> rawLines = lines(rawMain);
            List<String> draftLines = lines(draftMain);
            if (rawLines.size() != rawBlocks.size() || rawLines.size() != draftLines.size()) return out;
            draftBlocks = draftLines;
        }
        for (ReferenceProjector.Row row : projection.pronouns()) {
            if (row.call().isBlank()) continue;
            Set<String> own = Set.of(row.call().trim().toLowerCase(Locale.ROOT), row.self().trim().toLowerCase(Locale.ROOT));
            List<String> quoted = new ArrayList<>();
            Map<String, Map<String, Integer>> found = new LinkedHashMap<>();
            for (int i = 0; i < rawBlocks.size(); i++) {
                int paragraph = firstParagraph + i;
                if (!row.covers(paragraph) || !hasQuotation(rawBlocks.get(i))) continue;
                String label = String.format(Locale.ROOT, "P%03d", paragraph);
                quoted.add(label);
                Map<String, Integer> words = new LinkedHashMap<>();
                for (String token : tokens(draftBlocks.get(i))) {
                    if (WORDS.contains(token) && !own.contains(token)) words.merge(token, 1, Integer::sum);
                }
                if (!words.isEmpty()) found.put(label, words);
            }
            if (found.isEmpty()) continue;
            StringBuilder sb = new StringBuilder();
            sb.append("Row ").append(row.from()).append(" (").append(row.speaker()).append(" -> ").append(row.target())
                    .append("; self \"").append(row.self()).append("\", call \"").append(row.call()).append("\"): quoted RAW paragraphs in scope ")
                    .append(String.join(", ", quoted)).append("; DRAFT address words other than the row's self/call: ");
            boolean first = true;
            for (Map.Entry<String, Map<String, Integer>> e : found.entrySet()) {
                if (!first) sb.append("; ");
                first = false;
                sb.append(e.getKey()).append(' ');
                boolean firstWord = true;
                for (Map.Entry<String, Integer> w : e.getValue().entrySet()) {
                    if (!firstWord) sb.append(", ");
                    firstWord = false;
                    sb.append('"').append(w.getKey()).append('"');
                    if (w.getValue() > 1) sb.append(" x").append(w.getValue());
                }
            }
            out.add(sb.toString());
        }
        return out;
    }

    private static List<String> lines(String text) {
        List<String> out = new ArrayList<>();
        for (String line : paragraphLines(text)) if (!PairText.isBlank(line)) out.add(line);
        return out;
    }

    private static String[] paragraphLines(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    /** Blocks of non-blank lines separated by blank lines, the same paragraphs the RAW labels count. */
    static List<String> paragraphs(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : paragraphLines(text)) {
            if (PairText.isBlank(line)) {
                if (current.length() > 0) { out.add(current.toString()); current.setLength(0); }
            } else {
                if (current.length() > 0) current.append('\n');
                current.append(line);
            }
        }
        if (current.length() > 0) out.add(current.toString());
        return out;
    }

    private static boolean hasQuotation(String rawBlock) {
        return rawBlock.indexOf('「') >= 0 || rawBlock.indexOf('『') >= 0;
    }

    /** Lower-cased words of letters; no regex class, so it behaves the same on Android. */
    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        String nfc = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC);
        for (int i = 0; i < nfc.length(); ) {
            int cp = nfc.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isLetter(cp)) {
                word.appendCodePoint(Character.toLowerCase(cp));
            } else if (word.length() > 0) {
                out.add(word.toString());
                word.setLength(0);
            }
        }
        if (word.length() > 0) out.add(word.toString());
        return out;
    }
}
