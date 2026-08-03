package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Strict, deterministic JSON parsing/canonicalization used by pack manifests. */
public final class EditorialCanonicalJson {
    private EditorialCanonicalJson() {}

    public static Object parse(byte[] utf8Bytes) {
        if (utf8Bytes == null) throw new IllegalArgumentException("JSON bytes are null");
        if (utf8Bytes.length >= 3 && (utf8Bytes[0] & 0xff) == 0xef
                && (utf8Bytes[1] & 0xff) == 0xbb && (utf8Bytes[2] & 0xff) == 0xbf) {
            throw new IllegalArgumentException("UTF-8 BOM is forbidden");
        }
        final String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(utf8Bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Manifest is not strict UTF-8", e);
        }
        return new Parser(text).parseDocument();
    }

    public static Map<String, Object> parseObject(byte[] utf8Bytes) {
        Object value = parse(utf8Bytes);
        if (!(value instanceof Map)) throw new IllegalArgumentException("Manifest root must be an object");
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) value;
        return result;
    }

    /** Canonical form: sorted object keys, normalized numbers and deterministic escaping. */
    public static String canonicalize(Object value) {
        StringBuilder out = new StringBuilder();
        writeCanonical(value, out);
        return out.toString();
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder out = new StringBuilder(digest.length * 2);
            for (byte b : digest) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    static Map<String, Object> freezeObject(Map<String, Object> source) {
        TreeMap<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            sorted.put(entry.getKey(), freeze(entry.getValue()));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    static Object freeze(Object value) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) value;
            return freezeObject(map);
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) value;
            ArrayList<Object> frozen = new ArrayList<>(list.size());
            for (Object item : list) frozen.add(freeze(item));
            return Collections.unmodifiableList(frozen);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map)) throw new IllegalArgumentException(path + " must be an object");
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    static List<Object> array(Object value, String path) {
        if (!(value instanceof List)) throw new IllegalArgumentException(path + " must be an array");
        return (List<Object>) value;
    }

    static String string(Object value, String path) {
        if (!(value instanceof String)) throw new IllegalArgumentException(path + " must be a string");
        return (String) value;
    }

    static boolean bool(Object value, String path) {
        if (!(value instanceof Boolean)) throw new IllegalArgumentException(path + " must be boolean");
        return (Boolean) value;
    }

    static long integer(Object value, String path) {
        if (!(value instanceof BigDecimal)) throw new IllegalArgumentException(path + " must be an integer");
        BigDecimal decimal = (BigDecimal) value;
        try {
            return decimal.longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(path + " must be an integer in range", e);
        }
    }

    private static void writeCanonical(Object value, StringBuilder out) {
        if (value == null) { out.append("null"); return; }
        if (value instanceof String) { writeString((String) value, out); return; }
        if (value instanceof Boolean) { out.append((Boolean) value ? "true" : "false"); return; }
        if (value instanceof BigDecimal) {
            BigDecimal decimal = ((BigDecimal) value).stripTrailingZeros();
            out.append(decimal.signum() == 0 ? "0" : decimal.toPlainString());
            return;
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) value;
            out.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> entry : new TreeMap<>(map).entrySet()) {
                if (!first) out.append(',');
                first = false;
                writeString(entry.getKey(), out);
                out.append(':');
                writeCanonical(entry.getValue(), out);
            }
            out.append('}');
            return;
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) value;
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) out.append(',');
                writeCanonical(list.get(i), out);
            }
            out.append(']');
            return;
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    private static void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String text;
        private int index;

        Parser(String text) { this.text = text; }

        Object parseDocument() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) fail("Trailing data");
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) fail("Unexpected end of JSON");
            char c = text.charAt(index);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' && consume("true")) return Boolean.TRUE;
            if (c == 'f' && consume("false")) return Boolean.FALSE;
            if (c == 'n' && consume("null")) return null;
            if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
            fail("Unexpected character '" + c + "'");
            return null;
        }

        private Map<String, Object> parseObject() {
            expect('{');
            LinkedHashMap<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) { index++; return object; }
            while (true) {
                skipWhitespace();
                if (!peek('"')) fail("Object key must be a string");
                String key = parseString();
                if (object.containsKey(key)) fail("Duplicate object key: " + key);
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();
                if (peek('}')) { index++; return object; }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            ArrayList<Object> array = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) { index++; return array; }
            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (peek(']')) { index++; return array; }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') return out.toString();
                if (c < 0x20) fail("Unescaped control character in string");
                if (c != '\\') { out.append(c); continue; }
                if (index >= text.length()) fail("Unterminated escape");
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> out.append(parseUnicodeEscape());
                    default -> fail("Invalid string escape");
                }
            }
            fail("Unterminated string");
            return null;
        }

        private char parseUnicodeEscape() {
            if (index + 4 > text.length()) fail("Incomplete unicode escape");
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(text.charAt(index++), 16);
                if (digit < 0) fail("Invalid unicode escape");
                value = (value << 4) | digit;
            }
            return (char) value;
        }

        private BigDecimal parseNumber() {
            int start = index;
            if (peek('-')) index++;
            if (index >= text.length()) fail("Invalid number");
            if (peek('0')) {
                index++;
                if (index < text.length() && Character.isDigit(text.charAt(index))) fail("Leading zero in number");
            } else {
                if (!Character.isDigit(text.charAt(index))) fail("Invalid number");
                while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
            }
            if (peek('.')) {
                index++;
                int fractionStart = index;
                while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
                if (fractionStart == index) fail("Fraction has no digits");
            }
            if (peek('e') || peek('E')) {
                index++;
                if (peek('+') || peek('-')) index++;
                int exponentStart = index;
                while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
                if (exponentStart == index) fail("Exponent has no digits");
            }
            try {
                return new BigDecimal(text.substring(start, index));
            } catch (NumberFormatException e) {
                fail("Invalid number");
                return BigDecimal.ZERO;
            }
        }

        private boolean consume(String expected) {
            if (text.regionMatches(index, expected, 0, expected.length())) {
                index += expected.length();
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (index < text.length()) {
                char c = text.charAt(index);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') index++;
                else break;
            }
        }

        private boolean peek(char expected) { return index < text.length() && text.charAt(index) == expected; }

        private void expect(char expected) {
            if (!peek(expected)) fail("Expected '" + expected + "'");
            index++;
        }

        private void fail(String message) { throw new IllegalArgumentException(message + " at byte/char " + index); }
    }
}
