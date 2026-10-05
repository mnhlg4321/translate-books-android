package com.ml.tblandroidtxt.editorial.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the edit answer. EDITED is the first top-level tag, trimmed at both ends and otherwise byte for byte as sent. NOTES
 * is optional and line based: a broken line is skipped and counted, never an error. Nothing here throws on model text.
 */
public final class EditResponseParser {
    public enum Status { OK, TRUNCATED, FORMAT, WRONG_PAIR }

    /** One decision the editor flagged: a quote from its text, a free kind label and a short reason. */
    public record Note(String quote, String kind, String reason) { }

    public record Parsed(Status status, String edited, List<Note> notes, int notesDropped, String wrongPairEvidence,
                         boolean textBeforeTag) {
        public Parsed {
            edited = edited == null ? "" : edited;
            notes = notes == null ? List.of() : List.copyOf(notes);
            wrongPairEvidence = wrongPairEvidence == null ? "" : wrongPairEvidence;
        }
    }

    private EditResponseParser() { }

    public static Parsed parse(String content, String finishReason) {
        String text = content == null ? "" : content;
        boolean length = "length".equalsIgnoreCase(finishReason == null ? "" : finishReason.trim());
        int open = text.indexOf(EditPromptBuilder.EDITED_OPEN);
        int wrongOpen = text.indexOf(EditPromptBuilder.WRONG_PAIR_OPEN);
        if (wrongOpen >= 0 && (open < 0 || wrongOpen < open)) {
            String evidence = section(text, wrongOpen, EditPromptBuilder.WRONG_PAIR_OPEN, EditPromptBuilder.WRONG_PAIR_CLOSE);
            if (evidence != null && !evidence.isBlank()) {
                return new Parsed(Status.WRONG_PAIR, "", List.of(), 0, evidence.trim(), false);
            }
        }
        if (open < 0) {
            return new Parsed(length ? Status.TRUNCATED : Status.FORMAT, "", List.of(), 0, "", false);
        }
        int close = matchingClose(text, open);
        if (close < 0) {
            return new Parsed(length ? Status.TRUNCATED : Status.FORMAT, "", List.of(), 0, "", !text.substring(0, open).isBlank());
        }
        String edited = text.substring(open + EditPromptBuilder.EDITED_OPEN.length(), close).strip();
        if (edited.isEmpty()) return new Parsed(Status.FORMAT, "", List.of(), 0, "", !text.substring(0, open).isBlank());
        boolean before = !text.substring(0, open).isBlank();
        String rest = text.substring(close + EditPromptBuilder.EDITED_CLOSE.length());
        List<Note> notes = new ArrayList<>();
        int dropped = 0;
        int notesOpen = rest.indexOf(EditPromptBuilder.NOTES_OPEN);
        if (notesOpen >= 0) {
            String body = section(rest, notesOpen, EditPromptBuilder.NOTES_OPEN, EditPromptBuilder.NOTES_CLOSE);
            if (body == null) body = rest.substring(notesOpen + EditPromptBuilder.NOTES_OPEN.length());
            for (String line : body.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
                String row = line.strip();
                if (row.isEmpty()) continue;
                Note note = note(row);
                if (note == null) dropped++;
                else notes.add(note);
            }
        }
        return new Parsed(Status.OK, edited, notes, dropped, "", before);
    }

    /** The close tag that balances the first open tag; a nested open tag inside the text does not end it early. */
    private static int matchingClose(String text, int open) {
        int depth = 0;
        int index = open;
        while (index < text.length()) {
            int nextOpen = text.indexOf(EditPromptBuilder.EDITED_OPEN, index);
            int nextClose = text.indexOf(EditPromptBuilder.EDITED_CLOSE, index);
            if (nextClose < 0) return -1;
            if (nextOpen >= 0 && nextOpen < nextClose) {
                depth++;
                index = nextOpen + EditPromptBuilder.EDITED_OPEN.length();
            } else {
                depth--;
                if (depth == 0) return nextClose;
                index = nextClose + EditPromptBuilder.EDITED_CLOSE.length();
            }
        }
        return -1;
    }

    private static String section(String text, int open, String openTag, String closeTag) {
        int close = text.indexOf(closeTag, open + openTag.length());
        if (close < 0) return null;
        return text.substring(open + openTag.length(), close);
    }

    private static Note note(String row) {
        String[] parts = row.split("\\s\\|\\s", 3);
        if (parts.length < 3) return null;
        String quote = parts[0].strip();
        String kind = parts[1].strip();
        String reason = parts[2].strip();
        if (quote.isEmpty() || kind.isEmpty()) return null;
        return new Note(quote, kind, reason);
    }
}
