package com.ml.tblandroidtxt;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Decodes TXT bytes: BOM first (UTF-8, UTF-16LE/BE), then strict UTF-8, then windows-31j / Shift_JIS, last a lenient UTF-8.
 * The phone's FileUtil.decodeText delegates here, and the PC reads files through it; TextDecodingVectorsTest pins the behaviour
 * with fixed byte vectors.
 */
public final class TextDecoding {
    private TextDecoding() {}

    public static final class Result {
        public final String text;
        public final String encoding;
        public final int bytes;

        Result(String text, String encoding, int bytes) {
            this.text = text == null ? "" : text;
            this.encoding = encoding == null ? "unknown" : encoding;
            this.bytes = bytes;
        }
    }

    public static Result decode(byte[] data) throws Exception {
        if (data == null) data = new byte[0];
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            return new Result(new String(data, 3, data.length - 3, StandardCharsets.UTF_8), "UTF-8 BOM", data.length);
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
            return new Result(new String(data, 2, data.length - 2, Charset.forName("UTF-16LE")), "UTF-16LE BOM", data.length);
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
            return new Result(new String(data, 2, data.length - 2, Charset.forName("UTF-16BE")), "UTF-16BE BOM", data.length);
        }
        try {
            return new Result(strictDecode(data, StandardCharsets.UTF_8), "UTF-8", data.length);
        } catch (CharacterCodingException ignored) {
            try {
                return new Result(strictDecode(data, Charset.forName("windows-31j")), "Windows-31J/Shift-JIS", data.length);
            } catch (Exception ignored2) {
                try {
                    return new Result(strictDecode(data, Charset.forName("Shift_JIS")), "Shift_JIS", data.length);
                } catch (Exception ignored3) {
                    return new Result(new String(data, StandardCharsets.UTF_8), "UTF-8 fallback", data.length);
                }
            }
        }
    }

    private static String strictDecode(byte[] data, Charset charset) throws CharacterCodingException {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer decoded = decoder.decode(ByteBuffer.wrap(data));
        return decoded.toString();
    }
}
