package com.ml.tblandroidtxt.editorial.pack;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates the staged bytes of one pack. It never loads or executes code. */
public final class EditorialPackIntegrityValidator {
    public EditorialPackIntegrityResult validate(byte[] manifestBytes, Map<String, byte[]> dataFiles) {
        if (manifestBytes == null || dataFiles == null) {
            return invalid(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.NULL_INPUT, "", "Manifest and data files are required"));
        }
        EditorialPackManifest manifest;
        try {
            manifest = EditorialPackManifest.parse(manifestBytes);
        } catch (IllegalArgumentException e) {
            return invalid(new EditorialPackIntegrityResult.Issue(classifyManifestError(e), "editorial-pack.json", safeMessage(e)));
        }

        ArrayList<EditorialPackIntegrityResult.Issue> issues = new ArrayList<>();
        byte[] canonicalManifest = manifest.canonicalJson().getBytes(StandardCharsets.UTF_8);
        if (!java.util.Arrays.equals(manifestBytes, canonicalManifest)) {
            issues.add(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.INVALID_MANIFEST_FIELD,
                    "editorial-pack.json", "Manifest bytes are not the required canonical JSON representation"));
        }
        if (!manifest.canonicalPackHash().equals(manifest.calculatedCanonicalPackHash())) {
            issues.add(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.CANONICAL_HASH_MISMATCH,
                    "canonicalPackHash", "Declared canonical pack hash does not match the canonical manifest"));
        }
        if (dataFiles.size() != 3) {
            issues.add(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.FILE_COUNT_MISMATCH,
                    "fileRoles", "Pack must contain exactly three data files"));
        }

        Set<String> expectedPaths = new HashSet<>();
        for (EditorialPackManifest.FileEntry entry : manifest.fileEntries()) {
            expectedPaths.add(entry.path());
            byte[] bytes = dataFiles.get(entry.path());
            if (bytes == null) {
                issues.add(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.MISSING_FILE,
                        entry.path(), "Manifest file role is missing from staged data"));
                continue;
            }
            if (bytes.length != entry.byteLength()) {
                issues.add(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.FILE_LENGTH_MISMATCH,
                        entry.path(), "Raw byte length does not match manifest"));
            }
            String actual = EditorialCanonicalJson.sha256Hex(bytes);
            if (!actual.equals(entry.sha256())) {
                issues.add(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.FILE_HASH_MISMATCH,
                        entry.path(), "Raw SHA-256 does not match manifest"));
            }
            if (hasUtf8Bom(bytes)) {
                issues.add(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.BOM_FORBIDDEN,
                        entry.path(), "Pack data files must not contain a UTF-8 BOM"));
            } else {
                try {
                    StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
                } catch (CharacterCodingException e) {
                    issues.add(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.INVALID_UTF8,
                            entry.path(), "Pack data file is not strict UTF-8"));
                }
            }
        }
        for (String path : dataFiles.keySet()) {
            if (!expectedPaths.contains(path)) {
                issues.add(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.EXTRA_FILE,
                        path, "Data file is not declared by a manifest role"));
            }
        }
        return new EditorialPackIntegrityResult(issues.isEmpty(), manifest, issues);
    }

    private static EditorialPackIntegrityResult invalid(EditorialPackIntegrityResult.Issue issue) {
        return new EditorialPackIntegrityResult(false, null, List.of(issue));
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xff) == 0xef && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf;
    }

    private static EditorialPackValidationCode classifyManifestError(Throwable error) {
        String text = safeMessage(error).toLowerCase(java.util.Locale.ROOT);
        if (text.contains("bom")) return EditorialPackValidationCode.BOM_FORBIDDEN;
        if (text.contains("duplicate object key")) return EditorialPackValidationCode.DUPLICATE_JSON_KEY;
        if (text.contains("utf-8")) return EditorialPackValidationCode.INVALID_UTF8;
        if (text.contains("missing manifest field")) return EditorialPackValidationCode.MISSING_MANIFEST_FIELD;
        if (text.contains("unknown manifest field")) return EditorialPackValidationCode.UNKNOWN_MANIFEST_FIELD;
        if (text.contains("duplicate file role")) return EditorialPackValidationCode.DUPLICATE_FILE_ROLE;
        if (text.contains("duplicate file path")) return EditorialPackValidationCode.DUPLICATE_FILE_PATH;
        if (text.contains("file role")) return EditorialPackValidationCode.INVALID_FILE_ROLE;
        return EditorialPackValidationCode.INVALID_MANIFEST_FIELD;
    }

    private static String safeMessage(Throwable error) {
        return error == null || error.getMessage() == null ? "Invalid pack manifest" : error.getMessage();
    }
}
