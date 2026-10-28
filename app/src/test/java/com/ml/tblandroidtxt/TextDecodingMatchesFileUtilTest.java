package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** The core TextDecoding (used by the PC app) must decode exactly like FileUtil.decodeText on the phone, byte set by byte set. */
public class TextDecodingMatchesFileUtilTest {
    private static final String JA = "日本語のテスト。「こんにちは」";

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static void same(String label, byte[] data) throws Exception {
        Method m = FileUtil.class.getDeclaredMethod("decodeText", byte[].class);
        m.setAccessible(true);
        FileUtil.TextReadResult phone = (FileUtil.TextReadResult) m.invoke(null, (Object) data);
        TextDecoding.Result core = TextDecoding.decode(data);
        assertEquals(label + " text", phone.text, core.text);
        assertEquals(label + " encoding", phone.encoding, core.encoding);
        assertEquals(label + " bytes", phone.bytes, core.bytes);
    }

    @Test public void everyEncodingBranchMatches() throws Exception {
        same("utf8", JA.getBytes(StandardCharsets.UTF_8));
        same("utf8 bom", concat(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, JA.getBytes(StandardCharsets.UTF_8)));
        same("utf16le bom", concat(new byte[]{(byte) 0xFF, (byte) 0xFE}, JA.getBytes(StandardCharsets.UTF_16LE)));
        same("utf16be bom", concat(new byte[]{(byte) 0xFE, (byte) 0xFF}, JA.getBytes(StandardCharsets.UTF_16BE)));
        same("shift-jis", JA.getBytes(Charset.forName("windows-31j")));
        same("invalid utf8 and invalid sjis", new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0x80, 0x41, (byte) 0xFD, (byte) 0xFE});
        same("empty", new byte[0]);
        same("ascii", "plain text\nline two\n".getBytes(StandardCharsets.US_ASCII));
    }
}
