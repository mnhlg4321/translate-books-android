package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The host manifest counts and hashes the exact bytes it attaches; synthetic text only. */
public final class V5HostSourceManifestTest {
    private static final String RAW = "\uFEFF第一行です。\r\n\r\n「二行目」と言った。\r\n";
    private static final String DRAFT = "Dòng một.\n\n\"Dòng hai\" nói.";
    private static final String GLOSSARY = "source,target,category,note,priority\n勇者,Dũng giả,role,,1\n";
    private static final String PRONOUN = "from,speaker,target,self,call,scope,note\n";

    private static List<EditInputs.OriginalSourceFile> files() {
        return List.of(
                new EditInputs.OriginalSourceFile("PRONOUN", V5SourcePackPreflightTest.PRONOUN_NAME, PRONOUN),
                new EditInputs.OriginalSourceFile("RAW", V5SourcePackPreflightTest.RAW_NAME, RAW),
                new EditInputs.OriginalSourceFile("GLOSSARY", V5SourcePackPreflightTest.GLOSSARY_NAME, GLOSSARY),
                new EditInputs.OriginalSourceFile("DRAFT", V5SourcePackPreflightTest.DRAFT_NAME, DRAFT));
    }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void entriesAreInRoleOrderWithBytesCharsLinesAndHashesOfTheExactContent() throws Exception {
        List<V5HostSourceManifest.Entry> entries = V5HostSourceManifest.entries(files());
        assertEquals(List.of("RAW", "DRAFT", "GLOSSARY", "PRONOUN"), entries.stream().map(V5HostSourceManifest.Entry::role).toList());
        V5HostSourceManifest.Entry raw = entries.get(0);
        assertEquals(V5SourcePackPreflightTest.RAW_NAME, raw.name());
        assertEquals(RAW.getBytes(StandardCharsets.UTF_8).length, raw.bytes());
        assertEquals(RAW.codePointCount(0, RAW.length()), raw.chars());
        assertEquals("three CRLF-terminated lines, one blank", 3, raw.lines());
        assertEquals(2, raw.nonblankLines());
        assertEquals(sha(RAW), raw.sha256());
        V5HostSourceManifest.Entry draft = entries.get(1);
        assertEquals("a last line without a terminator still counts", 3, draft.lines());
        assertEquals(sha(DRAFT), draft.sha256());
        assertEquals(sha(GLOSSARY), entries.get(2).sha256());
        assertEquals(sha(PRONOUN), entries.get(3).sha256());
    }

    @Test public void theBlockCarriesIdSeriesVersionAndOneLinePerFileWithOriginalNames() {
        String block = V5HostSourceManifest.render(V5SourcePackPreflightTest.IDENTITY, files());
        assertTrue(block.startsWith(V5HostSourceManifest.BEGIN + "\n"));
        assertTrue(block.endsWith(V5HostSourceManifest.END));
        assertTrue(block.contains("\nID=007\n"));
        assertTrue(block.contains("\nSERIES=SAMPLE_SERIES_VOL1\n"));
        assertTrue(block.contains("\nVERSION=V5-SAFE.4.1.3-FULL\n"));
        assertTrue(block.contains("\nFILE_COUNT=4\n"));
        String[] lines = block.split("\n");
        int fileLines = 0;
        for (String line : lines) if (line.startsWith("FILE role=")) fileLines++;
        assertEquals(4, fileLines);
        for (String name : new String[] {V5SourcePackPreflightTest.RAW_NAME, V5SourcePackPreflightTest.DRAFT_NAME,
                V5SourcePackPreflightTest.GLOSSARY_NAME, V5SourcePackPreflightTest.PRONOUN_NAME}) {
            assertTrue(name, block.contains("name=\"" + name + "\""));
        }
        assertFalse("the generic placeholder names must not appear", block.contains("RAW.txt") || block.contains("GLOSSARY.csv"));
        assertEquals(4, block.split("bytes_readable=yes", -1).length - 1);
        // roles appear in canonical order
        assertTrue(block.indexOf("FILE role=RAW") < block.indexOf("FILE role=DRAFT"));
        assertTrue(block.indexOf("FILE role=DRAFT") < block.indexOf("FILE role=GLOSSARY"));
        assertTrue(block.indexOf("FILE role=GLOSSARY") < block.indexOf("FILE role=PRONOUN"));
    }

    @Test public void anchorsAreTheFirstAndLastNonBlankLinesEscapedAndBounded() {
        String block = V5HostSourceManifest.render(V5SourcePackPreflightTest.IDENTITY, files());
        assertTrue(block, block.contains("first_anchor=\"\\uFEFF第一行です。\""));
        assertTrue(block, block.contains("last_anchor=\"「二行目」と言った。\""));
        String longLine = "あ".repeat(100);
        List<EditInputs.OriginalSourceFile> withLong = List.of(
                new EditInputs.OriginalSourceFile("RAW", V5SourcePackPreflightTest.RAW_NAME, "\n\n" + longLine + "\n  \n"),
                new EditInputs.OriginalSourceFile("DRAFT", V5SourcePackPreflightTest.DRAFT_NAME, "q\"uote\\"),
                new EditInputs.OriginalSourceFile("GLOSSARY", V5SourcePackPreflightTest.GLOSSARY_NAME, GLOSSARY),
                new EditInputs.OriginalSourceFile("PRONOUN", V5SourcePackPreflightTest.PRONOUN_NAME, PRONOUN));
        String out = V5HostSourceManifest.render(V5SourcePackPreflightTest.IDENTITY, withLong);
        assertTrue(out, out.contains("first_anchor=\"" + "あ".repeat(40) + "\u2026\""));
        assertTrue(out, out.contains("first_anchor=\"q\\\"uote\\\\\""));
    }

    @Test public void theRedactedFormHasEveryCountAndHashButNoBookText() {
        List<String> lines = V5HostSourceManifest.redactedLines(V5SourcePackPreflightTest.IDENTITY, files());
        String joined = String.join("\n", lines);
        assertTrue(joined.contains("ID=007") && joined.contains("SERIES=SAMPLE_SERIES_VOL1") && joined.contains("VERSION=V5-SAFE.4.1.3-FULL"));
        assertEquals(7, lines.size());
        assertFalse(joined.contains("第一行") || joined.contains("Dòng") || joined.contains("勇者"));
        assertFalse(joined.contains("anchor"));
    }

    @Test public void emptyAndSingleLineFilesAreCountedWithoutSurprise() {
        List<EditInputs.OriginalSourceFile> odd = List.of(
                new EditInputs.OriginalSourceFile("RAW", V5SourcePackPreflightTest.RAW_NAME, "x"),
                new EditInputs.OriginalSourceFile("DRAFT", V5SourcePackPreflightTest.DRAFT_NAME, "a\rb\r"),
                new EditInputs.OriginalSourceFile("GLOSSARY", V5SourcePackPreflightTest.GLOSSARY_NAME, ""),
                new EditInputs.OriginalSourceFile("PRONOUN", V5SourcePackPreflightTest.PRONOUN_NAME, "\n"));
        List<V5HostSourceManifest.Entry> e = V5HostSourceManifest.entries(odd);
        assertEquals(1, e.get(0).lines());
        assertEquals("lone CR terminates a line", 2, e.get(1).lines());
        assertEquals(0, e.get(2).lines());
        assertEquals(0, e.get(2).nonblankLines());
        assertEquals(1, e.get(3).lines());
        assertEquals(0, e.get(3).nonblankLines());
    }
}
