package com.ml.tblandroidtxt.editorial.pack;

import java.util.regex.Pattern;

/** Structural-only validation; A2 never creates or interprets an attempt selector. */
final class EditorialAttemptRequestSelector {
    private static final Pattern UUID_TEXT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    private EditorialAttemptRequestSelector() { }

    static String validate(String value) {
        String checked = EditorialIdentityText.nonEmpty(value, "attempt request selector");
        if (!checked.equals(trimAscii(checked))) {
            throw new IllegalArgumentException("attempt request selector has outer whitespace");
        }
        if (checked.contains("/") || checked.contains("\\") || checked.contains("://")) {
            throw new IllegalArgumentException("attempt request selector cannot be a path or URI");
        }
        if (checked.matches("[0-9]+") || UUID_TEXT.matcher(checked).matches()) {
            throw new IllegalArgumentException("attempt request selector cannot be a row/timestamp/UUID substitute");
        }
        return checked;
    }

    private static String trimAscii(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isAsciiWhitespace(value.charAt(start))) start++;
        while (end > start && isAsciiWhitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean isAsciiWhitespace(char value) {
        return value == ' ' || value == '\t' || value == '\n'
                || value == '\u000b' || value == '\f' || value == '\r';
    }
}
