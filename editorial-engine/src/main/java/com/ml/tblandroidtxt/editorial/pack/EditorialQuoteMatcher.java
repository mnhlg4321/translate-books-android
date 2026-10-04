package com.ml.tblandroidtxt.editorial.pack;

import java.text.Normalizer;

/** Bounded substring checks for quotes tied to a specific source anchor. */
final class EditorialQuoteMatcher {
    private EditorialQuoteMatcher() { }

    static boolean contains(String anchor, String quote) {
        if (anchor == null || quote == null) return false;
        return nfcTrim(anchor).contains(nfcTrim(quote));
    }

    /** RAW may render ruby readings as base text followed by 《reading》. */
    static boolean containsRaw(String anchor, String quote) {
        return anchor != null && quote != null
                && contains(withoutRubyReadings(anchor), withoutRubyReadings(quote));
    }

    private static String nfcTrim(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
        int start = 0;
        int end = normalized.length();
        while (start < end) {
            int cp = normalized.codePointAt(start);
            if (!Character.isWhitespace(cp)) break;
            start += Character.charCount(cp);
        }
        while (end > start) {
            int cp = normalized.codePointBefore(end);
            if (!Character.isWhitespace(cp)) break;
            end -= Character.charCount(cp);
        }
        return normalized.substring(start, end);
    }

    /** Remove closed ruby-reading annotations only; malformed open annotations remain literal source text. */
    private static String withoutRubyReadings(String source) {
        StringBuilder result = new StringBuilder(source.length());
        int position = 0;
        while (position < source.length()) {
            int open = source.indexOf('《', position);
            if (open < 0) {
                result.append(source, position, source.length());
                break;
            }
            int close = source.indexOf('》', open + 1);
            if (close < 0) {
                result.append(source, position, source.length());
                break;
            }
            result.append(source, position, open);
            position = close + 1;
        }
        return result.toString();
    }
}
