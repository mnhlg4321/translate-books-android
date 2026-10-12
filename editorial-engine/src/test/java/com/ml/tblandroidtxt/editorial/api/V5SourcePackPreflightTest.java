package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Synthetic pack checks; private book text is deliberately not a test fixture. */
public final class V5SourcePackPreflightTest {
    private static final String RAW = "raw source\r\n";
    private static final String DRAFT = "draft text\n";
    private static final String GLOSSARY = "source,target,category,note,priority\nterm,thuật ngữ,term,,1\n";
    private static final String PRONOUN = "﻿from,speaker,target,self,call,scope,note\n";
    static final String RAW_NAME = "007_RAW_SAMPLE_SERIES_VOL1.txt";
    static final String DRAFT_NAME = "007_SAMPLE_SERIES_VOL1_DRAFT.txt";
    static final String GLOSSARY_NAME = "007_SAMPLE_SERIES_VOL1_chapter_glossary.csv";
    static final String PRONOUN_NAME = "007_PRONOUN_SAMPLE_SERIES_VOL1.csv";
    static final V5SourceIdentity IDENTITY = new V5SourceIdentity("007", "SAMPLE_SERIES_VOL1");

    static List<EditInputs.OriginalSourceFile> full(String glossary, String pronoun) {
        return List.of(new EditInputs.OriginalSourceFile("RAW", RAW_NAME, RAW),
                new EditInputs.OriginalSourceFile("DRAFT", DRAFT_NAME, DRAFT),
                new EditInputs.OriginalSourceFile("GLOSSARY", GLOSSARY_NAME, glossary),
                new EditInputs.OriginalSourceFile("PRONOUN", PRONOUN_NAME, pronoun));
    }

    @Test public void acceptsTheFourRolesWithPackHeadersAndRetainsTheirOriginalNamesAndText() {
        List<EditInputs.OriginalSourceFile> files = full(GLOSSARY, PRONOUN);
        V5SourcePackPreflight.Result result = V5SourcePackPreflight.check(files, IDENTITY);
        assertTrue(result.code(), result.valid());
        assertEquals(List.of(RAW_NAME, DRAFT_NAME, GLOSSARY_NAME, PRONOUN_NAME), result.fileNames());
        assertEquals(RAW, files.get(0).content());
        assertTrue(V5SourcePackPreflight.decodeUtf8("﻿x\r\n".getBytes(StandardCharsets.UTF_8)).contains("\r\n"));
    }

    @Test public void filesAreJudgedByRoleNotByNameOrOrder() {
        List<EditInputs.OriginalSourceFile> reversed = new ArrayList<>(full(GLOSSARY, PRONOUN));
        java.util.Collections.reverse(reversed);
        assertTrue(V5SourcePackPreflight.check(reversed, IDENTITY).valid());
        assertEquals(List.of("RAW", "DRAFT", "GLOSSARY", "PRONOUN"),
                V5SourcePackPreflight.inRoleOrder(reversed).stream().map(EditInputs.OriginalSourceFile::role).toList());
        // a GLOSSARY role whose bytes are a pronoun table fails by the glossary schema, whatever it is called
        List<EditInputs.OriginalSourceFile> swapped = new ArrayList<>(full(GLOSSARY, PRONOUN));
        swapped.set(2, new EditInputs.OriginalSourceFile("GLOSSARY", GLOSSARY_NAME, PRONOUN));
        assertEquals("V5_GLOSSARY_SCHEMA_INVALID", V5SourcePackPreflight.check(swapped, IDENTITY).code());
    }

    @Test public void refusesMissingDuplicateEmptyOrUnknownRoleFiles() {
        assertEquals("V5_SOURCE_FILE_SET_INVALID", V5SourcePackPreflight.check(full(GLOSSARY, PRONOUN).subList(0, 3), IDENTITY).code());
        List<EditInputs.OriginalSourceFile> duplicate = new ArrayList<>(full(GLOSSARY, PRONOUN));
        duplicate.set(3, new EditInputs.OriginalSourceFile("GLOSSARY", PRONOUN_NAME, PRONOUN));
        assertEquals("V5_SOURCE_FILE_SET_INVALID", V5SourcePackPreflight.check(duplicate, IDENTITY).code());
        List<EditInputs.OriginalSourceFile> unknown = new ArrayList<>(full(GLOSSARY, PRONOUN));
        unknown.set(3, new EditInputs.OriginalSourceFile("NOTES", PRONOUN_NAME, PRONOUN));
        assertEquals("V5_SOURCE_FILE_SET_INVALID", V5SourcePackPreflight.check(unknown, IDENTITY).code());
        List<EditInputs.OriginalSourceFile> empty = new ArrayList<>(full(GLOSSARY, PRONOUN));
        empty.set(1, new EditInputs.OriginalSourceFile("DRAFT", DRAFT_NAME, " \r\n"));
        assertEquals("V5_SOURCE_EMPTY_OR_ENCODING_INVALID", V5SourcePackPreflight.check(empty, IDENTITY).code());
    }

    @Test public void aNameMustBeTheOwnersRealFileNameNotAPlaceholderOrAPath() {
        for (String bad : new String[] {"", "  ", "RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv", "dir/007_RAW_S.txt", "a\\b.txt", "x\ny.txt", " 007_RAW_S.txt"}) {
            List<EditInputs.OriginalSourceFile> files = new ArrayList<>(full(GLOSSARY, PRONOUN));
            files.set(0, new EditInputs.OriginalSourceFile("RAW", bad, RAW));
            assertEquals("name [" + bad + "]", "V5_SOURCE_NAME_INVALID", V5SourcePackPreflight.check(files, IDENTITY).code());
        }
    }

    @Test public void theChainNeedsBothIdAndSeriesBeforeAnythingIsSent() {
        List<EditInputs.OriginalSourceFile> files = full(GLOSSARY, PRONOUN);
        assertEquals("V5_IDENTITY_MISSING", V5SourcePackPreflight.check(files, V5SourceIdentity.NONE).code());
        assertEquals("V5_IDENTITY_MISSING", V5SourcePackPreflight.check(files, new V5SourceIdentity("007", " ")).code());
        assertEquals("V5_IDENTITY_MISSING", V5SourcePackPreflight.check(files, new V5SourceIdentity("", "S")).code());
        assertEquals("V5_IDENTITY_MISSING", V5SourcePackPreflight.check(files, null).code());
        assertEquals("V5_IDENTITY_INVALID", V5SourcePackPreflight.check(files, new V5SourceIdentity("0 07", "S")).code());
        assertEquals("V5_IDENTITY_INVALID", V5SourcePackPreflight.check(files, new V5SourceIdentity("007", "S/../x")).code());
        // a file-set problem is reported first
        assertEquals("V5_SOURCE_FILE_SET_INVALID", V5SourcePackPreflight.check(files.subList(0, 2), V5SourceIdentity.NONE).code());
        EditInputs inputs = new EditInputs(RAW, DRAFT, "Vietnamese", List.of(), PRONOUN, files, V5SourceIdentity.NONE);
        assertEquals("V5_IDENTITY_MISSING", V5SourcePackPreflight.check(inputs).code());
        assertTrue(V5SourcePackPreflight.check(new EditInputs(RAW, DRAFT, "Vietnamese", List.of(), PRONOUN, files, IDENTITY)).valid());
        assertEquals("V5_SOURCE_TEXT_MISMATCH",
                V5SourcePackPreflight.check(new EditInputs("other", DRAFT, "Vietnamese", List.of(), PRONOUN, files, IDENTITY)).code());
    }

    @Test public void enforcesFiveColumnGlossaryAndSevenColumnOrLegacyPronoun() {
        assertEquals("V5_GLOSSARY_SCHEMA_INVALID", V5SourcePackPreflight.check(full("source,target,category,note\na,b,c,d", PRONOUN), IDENTITY).code());
        assertEquals("V5_GLOSSARY_SCHEMA_INVALID", V5SourcePackPreflight.check(full(GLOSSARY + "bad,row\n", PRONOUN), IDENTITY).code());
        assertEquals("V5_PRONOUN_SCHEMA_INVALID", V5SourcePackPreflight.check(full(GLOSSARY, "from,speaker,target,self,call,note\na,b,c,d,e,f"), IDENTITY).code());
        assertEquals("V5_PRONOUN_SCHEMA_INVALID", V5SourcePackPreflight.check(full(GLOSSARY, PRONOUN + "bad,row\n"), IDENTITY).code());
        assertTrue(V5SourcePackPreflight.check(full(GLOSSARY, "花子,em,legacy note\n太郎,tôi,another note\n"), IDENTITY).valid());
        assertTrue(V5SourcePackPreflight.check(full(GLOSSARY, "from,target,note\n花子,em,legacy note\n"), IDENTITY).valid());
    }

    @Test public void strictUtf8DecoderRejectsMalformedBytes() {
        try {
            V5SourcePackPreflight.decodeUtf8(new byte[] {(byte) 0xc3, 0x28});
            throw new AssertionError("malformed UTF-8 was accepted");
        } catch (IllegalArgumentException expected) {
            assertEquals("V5_SOURCE_UTF8_INVALID", expected.getMessage());
        }
    }

    // ---- identity from the original names

    @Test public void identityIsReadFromTheOwnersFileNamesWhateverTheirPattern() {
        // the owner's four real patterns, including the two DRAFT variants and the translated one
        V5SourceIdentity.Derivation d = V5SourceIdentity.derive(full(GLOSSARY, PRONOUN));
        assertTrue(d.code(), d.valid());
        assertEquals(IDENTITY, d.identity());
        String[][] drafts = {{"002_RAW_SAMPLE_SERIES_VOL1_DRAFT.txt"}, {"002_SAMPLE_SERIES_VOL1_translated.txt"}, {"002_SAMPLE_SERIES_VOL1_DRAFT.txt"}};
        for (String[] draft : drafts) {
            List<EditInputs.OriginalSourceFile> files = List.of(
                    new EditInputs.OriginalSourceFile("RAW", "002_RAW_SAMPLE_SERIES_VOL1.txt", RAW),
                    new EditInputs.OriginalSourceFile("DRAFT", draft[0], DRAFT),
                    new EditInputs.OriginalSourceFile("GLOSSARY", "002_SAMPLE_SERIES_VOL1_chapter_glossary.csv", GLOSSARY),
                    new EditInputs.OriginalSourceFile("PRONOUN", "002_PRONOUN_SAMPLE_SERIES_VOL1.csv", PRONOUN));
            V5SourceIdentity.Derivation each = V5SourceIdentity.derive(files);
            assertTrue(draft[0] + " " + each.code(), each.valid());
            assertEquals(new V5SourceIdentity("002", "SAMPLE_SERIES_VOL1"), each.identity());
        }
    }

    @Test public void identityMissingOrDisagreeingNamesStopBeforeAProviderIsInvolved() {
        List<EditInputs.OriginalSourceFile> generic = List.of(
                new EditInputs.OriginalSourceFile("RAW", "RAW.txt", RAW), new EditInputs.OriginalSourceFile("DRAFT", "DRAFT.txt", DRAFT),
                new EditInputs.OriginalSourceFile("GLOSSARY", "GLOSSARY.csv", GLOSSARY), new EditInputs.OriginalSourceFile("PRONOUN", "PRONOUN.csv", PRONOUN));
        assertEquals("V5_IDENTITY_MISSING", V5SourceIdentity.derive(generic).code());
        assertEquals("V5_IDENTITY_MISSING", V5SourceIdentity.derive(List.of()).code());
        List<EditInputs.OriginalSourceFile> otherChapter = new ArrayList<>(full(GLOSSARY, PRONOUN));
        otherChapter.set(1, new EditInputs.OriginalSourceFile("DRAFT", "008_SAMPLE_SERIES_VOL1_DRAFT.txt", DRAFT));
        assertEquals("V5_IDENTITY_MISMATCH", V5SourceIdentity.derive(otherChapter).code());
        List<EditInputs.OriginalSourceFile> otherSeries = new ArrayList<>(full(GLOSSARY, PRONOUN));
        otherSeries.set(3, new EditInputs.OriginalSourceFile("PRONOUN", "007_PRONOUN_OTHER_BOOK.csv", PRONOUN));
        assertEquals("V5_IDENTITY_MISMATCH", V5SourceIdentity.derive(otherSeries).code());
        assertFalse(V5SourceIdentity.NONE.complete());
    }
}
