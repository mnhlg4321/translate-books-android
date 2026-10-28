package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Fixed byte vectors for the text decoding that the phone (FileUtil) and the PC both use. The bytes are written out here, so the test
 * no longer compares the code with a copy of itself.
 */
public class TextDecodingVectorsTest {
    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) out[i] = (byte) values[i];
        return out;
    }

    private static void check(String label, byte[] data, String text, String encoding) throws Exception {
        TextDecoding.Result r = TextDecoding.decode(data);
        assertEquals(label + " text", text, r.text);
        assertEquals(label + " encoding", encoding, r.encoding);
        assertEquals(label + " bytes", data.length, r.bytes);
    }

    @Test public void utf8() throws Exception {
        check("utf-8", bytes(0xE6, 0x97, 0xA5, 0xE6, 0x9C, 0xAC, 0xE8, 0xAA, 0x9E), "日本語", "UTF-8");
        check("ascii", bytes('a', 'b', '\n'), "ab\n", "UTF-8");
        check("empty", bytes(), "", "UTF-8");
    }

    @Test public void bomsAreRemovedAndNamed() throws Exception {
        check("utf-8 bom", bytes(0xEF, 0xBB, 0xBF, 0xE6, 0x97, 0xA5), "日", "UTF-8 BOM");
        check("utf-16le bom", bytes(0xFF, 0xFE, 0xE5, 0x65, 0x2C, 0x67), "日本", "UTF-16LE BOM");
        check("utf-16be bom", bytes(0xFE, 0xFF, 0x65, 0xE5, 0x67, 0x2C), "日本", "UTF-16BE BOM");
    }

    @Test public void shiftJisIsUsedOnlyWhenStrictUtf8Fails() throws Exception {
        check("shift-jis", bytes(0x93, 0xFA, 0x96, 0x7B, 0x8C, 0xEA), "日本語", "Windows-31J/Shift-JIS");
        check("shift-jis with ascii", bytes('A', 0x93, 0xFA, '\n'), "A日\n", "Windows-31J/Shift-JIS");
    }

    @Test public void bytesThatFitNoEncodingFallBackToLenientUtf8() throws Exception {
        TextDecoding.Result r = TextDecoding.decode(bytes(0x81, 0x20, 0xFF, 0xFF));
        assertEquals("UTF-8 fallback", r.encoding);
        assertEquals(4, r.bytes);
        assertEquals("replacement characters instead of an exception", "� ��", r.text);
    }

    @Test public void nullIsEmpty() throws Exception {
        TextDecoding.Result r = TextDecoding.decode(null);
        assertEquals("", r.text);
        assertEquals(0, r.bytes);
    }
}
