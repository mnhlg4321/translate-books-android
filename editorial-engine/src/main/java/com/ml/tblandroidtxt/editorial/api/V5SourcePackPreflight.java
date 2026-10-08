package com.ml.tblandroidtxt.editorial.api;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offline, fail-closed checks for the four original attachments required by the V5 4.1.3 full-chat pack. */
public final class V5SourcePackPreflight {
    public static final List<String> REQUIRED_NAMES = List.of("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv");
    public static final String GLOSSARY_HEADER = "source,target,category,note,priority";
    public static final String PRONOUN_HEADER = "from,speaker,target,self,call,scope,note";

    public record Result(String code, List<String> fileNames) {
        public Result { fileNames = List.copyOf(fileNames); }
        public boolean valid() { return code.isEmpty(); }
    }

    private V5SourcePackPreflight() { }

    /** Decodes source bytes without replacement characters or newline conversion. */
    public static String decodeUtf8(byte[] bytes) {
        if (bytes == null) throw new IllegalArgumentException("V5_SOURCE_BYTES_MISSING");
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("V5_SOURCE_UTF8_INVALID", invalid);
        }
    }

    /** Empty code means every required role was supplied exactly once and passes the pack's source checks. */
    public static Result check(List<EditInputs.OriginalSourceFile> files) {
        if (files == null || files.size() != REQUIRED_NAMES.size()) return result("V5_SOURCE_FILE_SET_INVALID", files);
        Map<String, String> byName = new LinkedHashMap<>();
        for (int i = 0; i < files.size(); i++) {
            EditInputs.OriginalSourceFile file = files.get(i);
            if (file == null || !REQUIRED_NAMES.get(i).equals(file.name()) || byName.put(file.name(), file.content()) != null) {
                return result("V5_SOURCE_FILE_SET_INVALID", files);
            }
            if (file.content().isBlank() || !validUtf8String(file.content())) return result("V5_SOURCE_EMPTY_OR_ENCODING_INVALID", files);
        }
        if (!byName.keySet().containsAll(REQUIRED_NAMES)) return result("V5_SOURCE_FILE_SET_INVALID", files);
        if (!validGlossary(byName.get("GLOSSARY.csv"))) {
            return result("V5_GLOSSARY_SCHEMA_INVALID", files);
        }
        if (!validPronoun(byName.get("PRONOUN.csv"))) return result("V5_PRONOUN_SCHEMA_INVALID", files);
        return result("", files);
    }

    public static Result check(EditInputs inputs) {
        if (inputs == null) return result("V5_SOURCE_FILE_SET_INVALID", List.of());
        Result files = check(inputs.originalSourceFiles());
        if (!files.valid()) return files;
        Map<String, String> byName = new LinkedHashMap<>();
        for (EditInputs.OriginalSourceFile file : inputs.originalSourceFiles()) byName.put(file.name(), file.content());
        if (!inputs.raw().equals(byName.get("RAW.txt")) || !inputs.draft().equals(byName.get("DRAFT.txt"))) {
            return result("V5_SOURCE_TEXT_MISMATCH", inputs.originalSourceFiles());
        }
        return files;
    }

    public static void requireValid(List<EditInputs.OriginalSourceFile> files) {
        Result result = check(files);
        if (!result.valid()) throw new IllegalArgumentException(result.code());
    }

    public static void requireValid(EditInputs inputs) {
        Result result = check(inputs);
        if (!result.valid()) throw new IllegalArgumentException(result.code());
    }

    private static boolean validGlossary(String csv) {
        String[] lines = csv.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        if (!GLOSSARY_HEADER.equals(stripBom(lines[0]).trim())) return false;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank() || lines[i].stripLeading().startsWith("#")) continue;
            if (ReferenceFilter.cells(lines[i]).size() != 5) return false;
        }
        return true;
    }

    private static boolean validPronoun(String csv) {
        String[] lines = csv.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<String> useful = new ArrayList<>();
        for (String line : lines) if (!line.isBlank() && !line.stripLeading().startsWith("#")) useful.add(line);
        if (useful.isEmpty()) return false;
        String first = stripBom(useful.get(0)).trim();
        if (PRONOUN_HEADER.equalsIgnoreCase(first)) {
            for (int i = 1; i < useful.size(); i++) if (ReferenceFilter.cells(useful.get(i)).size() != 7) return false;
            return true;
        }
        boolean legacyHeader = "from,target,note".equalsIgnoreCase(first) || "source,target,note".equalsIgnoreCase(first);
        if (legacyHeader) {
            for (int i = 1; i < useful.size(); i++) if (ReferenceFilter.cells(useful.get(i)).size() != 3) return false;
            return true;
        }
        // The legacy form is headerless and has exactly three CSV cells on every non-comment row.
        for (String line : useful) {
            if (ReferenceFilter.cells(line).size() != 3) return false;
        }
        return true;
    }

    private static String firstCsvLine(String csv) {
        int end = csv.indexOf('\n');
        String line = end < 0 ? csv : csv.substring(0, end);
        if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
        return stripBom(line);
    }

    private static String stripBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private static boolean validUtf8String(String value) {
        try {
            StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(java.nio.CharBuffer.wrap(value));
            return true;
        } catch (CharacterCodingException invalid) { return false; }
    }

    private static Result result(String code, List<EditInputs.OriginalSourceFile> files) {
        List<String> names = new ArrayList<>();
        if (files != null) for (EditInputs.OriginalSourceFile file : files) if (file != null) names.add(file.name());
        return new Result(code, names);
    }
}
