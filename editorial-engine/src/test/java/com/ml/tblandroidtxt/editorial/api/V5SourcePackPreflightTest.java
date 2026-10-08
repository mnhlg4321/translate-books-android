package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Synthetic pack checks; private book text is deliberately not a test fixture. */
public final class V5SourcePackPreflightTest {
    private static final String RAW = "raw source\r\n";
    private static final String DRAFT = "draft text\n";
    private static final String GLOSSARY = "source,target,category,note,priority\nterm,thuật ngữ,term,,1\n";
    private static final String PRONOUN = "\uFEFFfrom,speaker,target,self,call,scope,note\n";

    private static List<EditInputs.OriginalSourceFile> full(String glossary, String pronoun) {
        return List.of(new EditInputs.OriginalSourceFile("RAW.txt", RAW),
                new EditInputs.OriginalSourceFile("DRAFT.txt", DRAFT),
                new EditInputs.OriginalSourceFile("GLOSSARY.csv", glossary),
                new EditInputs.OriginalSourceFile("PRONOUN.csv", pronoun));
    }

    @Test public void acceptsTheExactFourFilesWithPackHeadersAndRetainsTheirOriginalText() {
        List<EditInputs.OriginalSourceFile> files = full(GLOSSARY, PRONOUN);
        V5SourcePackPreflight.Result result = V5SourcePackPreflight.check(files);
        assertTrue(result.code(), result.valid());
        assertEquals(V5SourcePackPreflight.REQUIRED_NAMES, result.fileNames());
        assertEquals(RAW, files.get(0).content());
        assertTrue(V5SourcePackPreflight.decodeUtf8("\uFEFFx\r\n".getBytes(StandardCharsets.UTF_8)).contains("\r\n"));
    }

    @Test public void refusesMissingDuplicateEmptyOrWronglyNamedFiles() {
        assertEquals("V5_SOURCE_FILE_SET_INVALID", V5SourcePackPreflight.check(full(GLOSSARY, PRONOUN).subList(0, 3)).code());
        List<EditInputs.OriginalSourceFile> duplicate = new ArrayList<>(full(GLOSSARY, PRONOUN));
        duplicate.set(3, new EditInputs.OriginalSourceFile("GLOSSARY.csv", PRONOUN));
        assertEquals("V5_SOURCE_FILE_SET_INVALID", V5SourcePackPreflight.check(duplicate).code());
        List<EditInputs.OriginalSourceFile> empty = new ArrayList<>(full(GLOSSARY, PRONOUN));
        empty.set(1, new EditInputs.OriginalSourceFile("DRAFT.txt", " \r\n"));
        assertEquals("V5_SOURCE_EMPTY_OR_ENCODING_INVALID", V5SourcePackPreflight.check(empty).code());
    }

    @Test public void enforcesFiveColumnGlossaryAndSevenColumnOrLegacyPronoun() {
        assertEquals("V5_GLOSSARY_SCHEMA_INVALID", V5SourcePackPreflight.check(full("source,target,category,note\na,b,c,d", PRONOUN)).code());
        assertEquals("V5_GLOSSARY_SCHEMA_INVALID", V5SourcePackPreflight.check(full(GLOSSARY + "bad,row\n", PRONOUN)).code());
        assertEquals("V5_PRONOUN_SCHEMA_INVALID", V5SourcePackPreflight.check(full(GLOSSARY, "from,speaker,target,self,call,note\na,b,c,d,e,f")).code());
        assertEquals("V5_PRONOUN_SCHEMA_INVALID", V5SourcePackPreflight.check(full(GLOSSARY, PRONOUN + "bad,row\n")).code());
        assertTrue(V5SourcePackPreflight.check(full(GLOSSARY, "花子,em,legacy note\n太郎,tôi,another note\n")).valid());
        assertTrue(V5SourcePackPreflight.check(full(GLOSSARY, "from,target,note\n花子,em,legacy note\n")).valid());
    }

    @Test public void strictUtf8DecoderRejectsMalformedBytes() {
        try {
            V5SourcePackPreflight.decodeUtf8(new byte[] {(byte) 0xc3, 0x28});
            throw new AssertionError("malformed UTF-8 was accepted");
        } catch (IllegalArgumentException expected) {
            assertEquals("V5_SOURCE_UTF8_INVALID", expected.getMessage());
        }
    }
}
