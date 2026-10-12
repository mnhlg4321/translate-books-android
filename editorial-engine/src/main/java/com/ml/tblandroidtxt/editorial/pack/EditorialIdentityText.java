package com.ml.tblandroidtxt.editorial.pack;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.regex.Pattern;

/** Shared strict text, hash and domain-separation rules for v17 identities. */
final class EditorialIdentityText {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private EditorialIdentityText() { }

    static String nonEmpty(String value, String field) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        strictUtf8(value, field);
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                throw new IllegalArgumentException(field + " contains a control character");
            }
        }
        return value;
    }

    static String optionalText(String value, String field) {
        if (value == null || value.isEmpty()) return null;
        return nonEmpty(value, field);
    }

    static String sha256(String value, String field) {
        if (!SHA256.matcher(value == null ? "" : value).matches()) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }

    static String scopeKey(String value) {
        if (value == null) throw new IllegalArgumentException("scope key is required");
        strictUtf8(value, "scope key");
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
        int start = 0;
        int end = normalized.length();
        while (start < end && isAsciiWhitespace(normalized.charAt(start))) start++;
        while (end > start && isAsciiWhitespace(normalized.charAt(end - 1))) end--;
        normalized = normalized.substring(start, end);
        if (normalized.isEmpty()) throw new IllegalArgumentException("scope key is empty");
        strictUtf8(normalized, "scope key");
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isISOControl(normalized.charAt(i))) {
                throw new IllegalArgumentException("scope key contains a control character");
            }
        }
        return normalized;
    }

    static byte[] strictUtf8(String value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required");
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xef
                    && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf) {
                throw new IllegalArgumentException(field + " must not start with UTF-8 BOM");
            }
            return bytes;
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException(field + " is not strict UTF-8", e);
        }
    }

    static String hash(String domain, String canonicalProjection) {
        byte[] domainBytes = strictUtf8(domain, "identity domain");
        byte[] projectionBytes = strictUtf8(canonicalProjection, "canonical projection");
        byte[] payload = new byte[domainBytes.length + 1 + projectionBytes.length];
        System.arraycopy(domainBytes, 0, payload, 0, domainBytes.length);
        payload[domainBytes.length] = '\n';
        System.arraycopy(projectionBytes, 0, payload, domainBytes.length + 1, projectionBytes.length);
        return EditorialCanonicalJson.sha256Hex(payload);
    }

    private static boolean isAsciiWhitespace(char value) {
        return value == ' ' || value == '\t' || value == '\n'
                || value == '\u000b' || value == '\f' || value == '\r';
    }
}
