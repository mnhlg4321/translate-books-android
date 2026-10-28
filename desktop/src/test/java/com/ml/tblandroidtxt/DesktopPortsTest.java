package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DesktopPortsTest {
    private static final String KEY = "TEST_KEY_NOT_SECRET";
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void keyValueStoreRoundTripsAcrossInstancesAndCommitReportsSuccess() {
        Path prefs = tmp.getRoot().toPath().resolve("prefs");
        JsonFileKeyValueStore a = new JsonFileKeyValueStore(prefs, "demo");
        assertTrue(a.edit().putString("s", "giá trị").putBoolean("b", true).putLong("n", 7L).commit());
        JsonFileKeyValueStore b = new JsonFileKeyValueStore(prefs, "demo");
        assertEquals("giá trị", b.getString("s", ""));
        assertTrue(b.getBoolean("b", false));
        assertEquals(7L, ((Number) b.getAll().get("n")).longValue());
        assertTrue(b.edit().remove("s").commit());
        assertEquals("d", new JsonFileKeyValueStore(prefs, "demo").getString("s", "d"));
        assertFalse("no temp file is left behind", Files.exists(prefs.resolve("demo.json.tmp")));
    }

    @Test public void aWriteThatCannotFinishLeavesTheOldFileIntact() throws Exception {
        Path prefs = tmp.getRoot().toPath().resolve("prefs");
        JsonFileKeyValueStore a = new JsonFileKeyValueStore(prefs, "demo");
        a.edit().putString("s", "old").commit();
        // A directory where the temp file must go makes the write fail before the move: the committed file must survive.
        Files.createDirectory(prefs.resolve("demo.json.tmp"));
        assertFalse(a.edit().putString("s", "new").commit());
        assertEquals("old", new JsonFileKeyValueStore(prefs, "demo").getString("s", ""));
    }

    @Test public void documentIoReadsBomAndShiftJisLikeThePhoneAndWritesUtf8WithoutBom() throws Exception {
        Path dir = tmp.getRoot().toPath();
        String text = "日本語のテスト。";
        Files.write(dir.resolve("utf8bom.txt"), concat(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, text.getBytes(StandardCharsets.UTF_8)));
        Files.write(dir.resolve("sjis.txt"), text.getBytes(Charset.forName("windows-31j")));
        Files.write(dir.resolve("utf16.txt"), concat(new byte[]{(byte) 0xFF, (byte) 0xFE}, text.getBytes(StandardCharsets.UTF_16LE)));
        PathDocumentIO io = new PathDocumentIO(dir);
        for (String name : new String[]{"utf8bom.txt", "sjis.txt", "utf16.txt"}) {
            assertEquals(name, text, io.readText(PathDocumentIO.refOf(dir.resolve(name))));
        }
        String out = PathDocumentIO.refOf(dir.resolve("out").resolve("o.txt"));
        io.writeTextVerified(out, "Xin chào\n");
        byte[] written = Files.readAllBytes(dir.resolve("out").resolve("o.txt"));
        assertEquals("Xin chào\n", new String(written, StandardCharsets.UTF_8));
        assertTrue("no BOM", written.length < 3 || (written[0] & 0xFF) != 0xEF);
    }

    @Test public void dpapiEncryptsAndDecryptsTheKeyAndNeverStoresItInPlainText() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        DpapiSecrets secrets = new DpapiSecrets(tmp.getRoot().toPath().resolve("secrets"));
        assertFalse(secrets.hasKey());
        secrets.save(KEY);
        assertEquals(KEY, secrets.load());
        assertEquals("••••" + KEY.substring(KEY.length() - 4), secrets.masked());
        byte[] raw = Files.readAllBytes(tmp.getRoot().toPath().resolve("secrets").resolve("api_key.bin"));
        assertFalse("the file is encrypted", new String(raw, StandardCharsets.ISO_8859_1).contains("NOT_SECRET"));
        assertEquals(KEY, secrets.withCurrentSecrets(new AppSettings()).apiKey);
    }

    @Test public void theKeyNeverReachesPrefsLogsOrTheDatabase() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        Path data = tmp.getRoot().toPath().resolve("data");
        DesktopRuntime runtime = DesktopRuntime.open(data, null);
        try {
            runtime.secrets().save(KEY);
            AppSettings s = runtime.loadSettings();
            s.apiKey = KEY;
            s.model = "fixture-model";
            runtime.saveSettings(s);
            assertEquals("the loaded settings never carry the key", "", runtime.loadSettings().apiKey);
            runtime.settingsForRun(s);
            FileRuntimeLog log = new FileRuntimeLog(data.resolve("logs"));
            log.append("run settings prepared");
        } finally {
            runtime.close();
        }
        byte[] needle = KEY.getBytes(StandardCharsets.UTF_8);
        java.util.List<Path> files;
        try (Stream<Path> walk = Files.walk(data)) { files = walk.filter(Files::isRegularFile).collect(Collectors.toList()); }
        assertFalse(files.isEmpty());
        for (Path f : files) {
            if (f.getFileName().toString().equals("app.lock")) continue;
            assertFalse(f + " contains the key", indexOf(Files.readAllBytes(f), needle) >= 0);
        }
    }

    @Test public void keyShapeIsCheckedBeforeSaving() {
        String good = "sk-or-v1-" + "0123456789abcdef".repeat(4);
        assertEquals(73, good.length());
        assertEquals(null, DpapiSecrets.validate("openrouter", good));
        assertEquals(null, DpapiSecrets.validate("openrouter", "  " + good + "\n"));
        String doubled = DpapiSecrets.validate("openrouter", good + good);
        assertTrue(doubled, doubled.contains("hai lần"));
        assertFalse("the message never repeats the key", doubled.contains("0123456789abcdef"));
        assertTrue(DpapiSecrets.validate("openrouter", good.substring(0, 60)).contains("73"));
        assertTrue(DpapiSecrets.validate("openrouter", "sk-or-v1-" + "G".repeat(64)) != null);
        assertTrue(DpapiSecrets.validate("openrouter", "").contains("trống"));
        assertTrue(DpapiSecrets.validate("openrouter", good.substring(0, 20) + " " + good.substring(20)).contains("khoảng trắng"));
        assertEquals(null, DpapiSecrets.validate("openai", "sk-" + "a".repeat(40)));
        assertEquals(null, DpapiSecrets.validate("custom", "token-" + "x".repeat(20)));
        assertTrue(DpapiSecrets.validate("custom", "short") != null);
        assertTrue(DpapiSecrets.validate("custom", "x".repeat(300)) != null);
        assertTrue(DpapiSecrets.validate("openai", "sk-" + "a".repeat(20) + "sk-" + "b".repeat(20)).contains("hai lần"));
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static int indexOf(byte[] hay, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= hay.length; i++) {
            for (int j = 0; j < needle.length; j++) if (hay[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }
}
