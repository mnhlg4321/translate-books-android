package com.ml.tblandroidtxt.editorial.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;

/** Text helpers of the chunk-pair contract: one normalization, one notion of "letters", one hash. Pure. */
public final class PairText {
    private PairText() { }

    /** CRLF and CR become LF, then NFC (normalization revision {@link PairContract#NORMALIZATION_REVISION}). */
    public static String normalize(String text) {
        String t = text == null ? "" : text;
        t = t.replace("\r\n", "\n").replace('\r', '\n');
        return Normalizer.normalize(t, Normalizer.Form.NFC);
    }

    public static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || codePoint == 0xFEFF;
    }

    /** Number of code points that are not whitespace: the "chữ" of the size gate. Independent of line breaks. */
    public static int letters(String text) {
        if (text == null) return 0;
        int n = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (!isWhitespace(cp)) n++;
            i += Character.charCount(cp);
        }
        return n;
    }

    /** The text with every whitespace code point removed, in order. */
    public static String lettersOnly(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (!isWhitespace(cp)) out.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    public static int leadingWhitespace(String text) {
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            if (!isWhitespace(cp)) break;
            i += Character.charCount(cp);
        }
        return i;
    }

    public static int trailingWhitespaceStart(String text) {
        int end = text.length();
        while (end > 0) {
            int cp = text.codePointBefore(end);
            if (!isWhitespace(cp)) break;
            end -= Character.charCount(cp);
        }
        return end;
    }

    public static String trim(String text) {
        String t = text == null ? "" : text;
        int a = leadingWhitespace(t);
        if (a == t.length()) return "";
        return t.substring(a, trailingWhitespaceStart(t));
    }

    /** Lines that contain at least one non-whitespace code point. */
    public static int nonBlankLines(String text) {
        if (text == null || text.isEmpty()) return 0;
        int n = 0;
        for (String line : text.split("\n", -1)) if (letters(line) > 0) n++;
        return n;
    }

    public static boolean isBlank(String text) { return letters(text) == 0; }
}
