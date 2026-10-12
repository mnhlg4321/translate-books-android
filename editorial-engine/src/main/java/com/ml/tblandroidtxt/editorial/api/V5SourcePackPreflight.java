package com.ml.tblandroidtxt.editorial.api;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline, fail-closed checks for the four original attachments required by the V5 4.1.3 full-chat pack. Files are judged by
 * their ROLE (RAW, DRAFT, GLOSSARY, PRONOUN), each exactly once; the name is only required to be the owner's real file name,
 * because the pack derives the chapter ID and series from it and names its outputs after them.
 */
public final class V5SourcePackPreflight {
    public static final List<String> ROLES = List.of("RAW", "DRAFT", "GLOSSARY", "PRONOUN");
    /** The placeholder names older runs used; they carry no chapter or series, so they are refused as original names. */
    public static final List<String> GENERIC_NAMES = List.of("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv");
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

    /** The files in the pack's canonical role order; files with an unknown role keep their relative order at the end. */
    public static List<EditInputs.OriginalSourceFile> inRoleOrder(List<EditInputs.OriginalSourceFile> files) {
        List<EditInputs.OriginalSourceFile> out = new ArrayList<>();
        if (files == null) return out;
        for (String role : ROLES) for (EditInputs.OriginalSourceFile file : files) if (file != null && role.equals(file.role())) out.add(file);
        for (EditInputs.OriginalSourceFile file : files) if (file != null && !ROLES.contains(file.role())) out.add(file);
        return out;
    }

    /** Empty code means every role was supplied exactly once and passes the pack's source checks. */
    public static Result check(List<EditInputs.OriginalSourceFile> files) {
        if (files == null || files.size() != ROLES.size()) return result("V5_SOURCE_FILE_SET_INVALID", files);
        Map<String, EditInputs.OriginalSourceFile> byRole = new LinkedHashMap<>();
        for (EditInputs.OriginalSourceFile file : files) {
            if (file == null || !ROLES.contains(file.role()) || byRole.put(file.role(), file) != null) {
                return result("V5_SOURCE_FILE_SET_INVALID", files);
            }
        }
        if (!byRole.keySet().containsAll(ROLES)) return result("V5_SOURCE_FILE_SET_INVALID", files);
        for (EditInputs.OriginalSourceFile file : files) {
            if (!originalName(file.name())) return result("V5_SOURCE_NAME_INVALID", files);
        }
        for (String role : ROLES) {
            String content = byRole.get(role).content();
            if (content.isBlank() || !validUtf8String(content)) return result("V5_SOURCE_EMPTY_OR_ENCODING_INVALID", files);
        }
        if (!validGlossary(byRole.get("GLOSSARY").content())) return result("V5_GLOSSARY_SCHEMA_INVALID", files);
        if (!validPronoun(byRole.get("PRONOUN").content())) return result("V5_PRONOUN_SCHEMA_INVALID", files);
        return result("", files);
    }

    /** Files and chain identity together: both must be present before a request is built. */
    public static Result check(List<EditInputs.OriginalSourceFile> files, V5SourceIdentity identity) {
        Result set = check(files);
        if (!set.valid()) return set;
        String problem = (identity == null ? V5SourceIdentity.NONE : identity).problem();
        return problem.isEmpty() ? set : new Result(problem, set.fileNames());
    }

    public static Result check(EditInputs inputs) {
        if (inputs == null) return result("V5_SOURCE_FILE_SET_INVALID", List.of());
        Result files = check(inputs.originalSourceFiles(), inputs.identity());
        if (!files.valid()) return files;
        Map<String, String> byRole = new LinkedHashMap<>();
        for (EditInputs.OriginalSourceFile file : inputs.originalSourceFiles()) byRole.put(file.role(), file.content());
        if (!inputs.raw().equals(byRole.get("RAW")) || !inputs.draft().equals(byRole.get("DRAFT"))) {
            return result("V5_SOURCE_TEXT_MISMATCH", inputs.originalSourceFiles());
        }
        return files;
    }

    public static void requireValid(List<EditInputs.OriginalSourceFile> files, V5SourceIdentity identity) {
        Result result = check(files, identity);
        if (!result.valid()) throw new IllegalArgumentException(result.code());
    }

    public static void requireValid(EditInputs inputs) {
        Result result = check(inputs);
        if (!result.valid()) throw new IllegalArgumentException(result.code());
    }

    /** A real file name: not blank, no path or control characters, and not one of the generic placeholders. */
    static boolean originalName(String name) {
        if (name == null || name.isBlank() || !name.equals(name.trim()) || GENERIC_NAMES.contains(name)) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x20 || c == 0x7F || c == '/' || c == '\\') return false;
        }
        return true;
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

    private static String stripBom(String value) {
        return value != null && value.startsWith("﻿") ? value.substring(1) : value;
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
