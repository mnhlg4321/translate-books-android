package com.ml.tblandroidtxt;

/**
 * Whether a stored job input hash still describes a file's text. The preparation plan stores the hash of the normalized text
 * (line endings turned into "\n"); jobs from before the plan stored the hash of the raw text. Resume has to accept both, otherwise a
 * file with Windows line endings (CRLF) is reported as "changed" although not one character differs.
 */
final class InputIdentity {
    private InputIdentity() {}

    enum Match { RAW, NORMALIZED, NONE }

    static Match match(String storedHash, String text) {
        if (storedHash == null || storedHash.isEmpty()) return Match.RAW;
        String t = text == null ? "" : text;
        if (storedHash.equalsIgnoreCase(HashUtil.sha256(t))) return Match.RAW;
        if (storedHash.equalsIgnoreCase(HashUtil.sha256(Chunker.normalizeSource(t)))) return Match.NORMALIZED;
        return Match.NONE;
    }

    static boolean matches(String storedHash, String text) { return match(storedHash, text) != Match.NONE; }
}
