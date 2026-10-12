package com.ml.tblandroidtxt;

import android.database.Cursor;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackIntegrityResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackIntegrityValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistryMetadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-only SQLite/storage adapter. It has no install, update, delete or certification operation. */
public final class SqliteEditorialPackRegistry implements EditorialPackRegistry {
    private final TranslationRepository database;
    private final EditorialPackStorageLayout storage;
    private final EditorialPackIntegrityValidator integrityValidator = new EditorialPackIntegrityValidator();

    public SqliteEditorialPackRegistry(TranslationRepository database, EditorialPackStorageLayout storage) {
        if (database == null || storage == null) throw new IllegalArgumentException("Registry dependencies are required");
        this.database = database;
        this.storage = storage;
    }

    @Override public Optional<EditorialPackManifest> findByHash(String canonicalPackHash) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT manifest_canonical_json,canonical_pack_hash,state,storage_key,blocked_reason,created_at,validated_at,engine_version_used FROM editorial_packs WHERE canonical_pack_hash=?", new String[]{canonicalPackHash})) {
            return cursor.moveToFirst() ? parseRecord(cursor) : Optional.empty();
        }
    }

    @Override public Optional<EditorialPackManifest> findByIdentity(String packId, String version) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT manifest_canonical_json,canonical_pack_hash,state,storage_key,blocked_reason,created_at,validated_at,engine_version_used FROM editorial_packs WHERE pack_id=? AND version=?", new String[]{packId, version})) {
            return cursor.moveToFirst() ? parseRecord(cursor) : Optional.empty();
        }
    }

    @Override public List<EditorialPackManifest> list() {
        ArrayList<EditorialPackManifest> result = new ArrayList<>();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT manifest_canonical_json,canonical_pack_hash,state,storage_key,blocked_reason,created_at,validated_at,engine_version_used FROM editorial_packs ORDER BY pack_id,version,canonical_pack_hash", null)) {
            while (cursor.moveToNext()) parseRecord(cursor).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    private Optional<EditorialPackManifest> parseRecord(Cursor cursor) {
        String canonicalJson = cursor.getString(0);
        String hash = cursor.getString(1);
        try {
            EditorialPackManifest manifest = EditorialPackManifest.parse(canonicalJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (!hash.equals(manifest.canonicalPackHash()) || !hash.equals(manifest.calculatedCanonicalPackHash())) return Optional.empty();
            EditorialPackRegistryMetadata.IntegrityState integrityState;
            String integrityReason = "";
            if (!storage.hasImmutableMarker(hash)) {
                integrityState = EditorialPackRegistryMetadata.IntegrityState.MISSING_STORAGE;
                integrityReason = "Immutable storage marker is missing";
            } else {
                Map<String, byte[]> dataFiles = new LinkedHashMap<>();
                for (EditorialPackManifest.FileEntry file : manifest.fileEntries()) {
                    if (!Files.isRegularFile(storage.immutableEntry(hash, file.path()), LinkOption.NOFOLLOW_LINKS)) {
                        integrityState = EditorialPackRegistryMetadata.IntegrityState.INVALID;
                        integrityReason = "Stored file is missing or is a symlink: " + file.path();
                        return Optional.of(manifest.withRegistryMetadata(metadata(cursor, integrityState, integrityReason, latestCompatibility(hash))));
                    }
                    dataFiles.put(file.path(), Files.readAllBytes(storage.immutableEntry(hash, file.path())));
                }
                EditorialPackIntegrityResult integrity = integrityValidator.validate(
                        canonicalJson.getBytes(StandardCharsets.UTF_8), dataFiles);
                integrityState = integrity.valid()
                        ? EditorialPackRegistryMetadata.IntegrityState.VALID
                        : EditorialPackRegistryMetadata.IntegrityState.INVALID;
                integrityReason = integrity.valid() ? "" : issueText(integrity.issues());
            }
            return Optional.of(manifest.withRegistryMetadata(metadata(cursor, integrityState, integrityReason, latestCompatibility(hash))));
        } catch (RuntimeException | IOException e) {
            EditorialPackManifest errorManifest = manifestWithError(canonicalJson, cursor, e);
            return errorManifest == null ? Optional.empty() : Optional.of(errorManifest);
        }
    }

    private EditorialPackManifest manifestWithError(String canonicalJson, Cursor cursor, Exception error) {
        try {
            EditorialPackManifest manifest = EditorialPackManifest.parse(canonicalJson.getBytes(StandardCharsets.UTF_8));
            return manifest.withRegistryMetadata(metadata(cursor, EditorialPackRegistryMetadata.IntegrityState.UNAVAILABLE,
                    error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(), null));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private EditorialPackRegistryMetadata metadata(Cursor cursor, EditorialPackRegistryMetadata.IntegrityState integrityState,
                                                   String integrityReason,
                                                   EditorialPackRegistryMetadata.CompatibilitySnapshot compatibility) {
        EditorialPackRegistryMetadata.StorageState state;
        try { state = EditorialPackRegistryMetadata.StorageState.valueOf(cursor.getString(2)); }
        catch (RuntimeException e) { state = EditorialPackRegistryMetadata.StorageState.UNKNOWN; }
        return new EditorialPackRegistryMetadata(state, cursor.getString(3), cursor.getString(4),
                cursor.getLong(5), cursor.getLong(6), cursor.getString(7), integrityState, integrityReason, compatibility);
    }

    private EditorialPackRegistryMetadata.CompatibilitySnapshot latestCompatibility(String hash) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT compatibility_class,required_class,machine_contract_fingerprint,engine_version_used,blocked_reason,evaluated_at FROM editorial_pack_compatibility_results WHERE canonical_pack_hash=? ORDER BY evaluated_at DESC,id DESC LIMIT 1", new String[]{hash})) {
            if (!cursor.moveToFirst()) return null;
            EditorialPackCompatibilityClass classification = parseClass(cursor.getString(0));
            EditorialPackCompatibilityClass required = parseClass(cursor.getString(1));
            String reason = safe(cursor.getString(4));
            return new EditorialPackRegistryMetadata.CompatibilitySnapshot(classification, required, cursor.getString(2), cursor.getString(3), reason, missingFromReason(reason), cursor.getLong(5));
        }
    }

    private static EditorialPackCompatibilityClass parseClass(String value) {
        try { return EditorialPackCompatibilityClass.valueOf(value); }
        catch (RuntimeException e) { return EditorialPackCompatibilityClass.BLOCKED; }
    }

    private static java.util.Set<String> missingFromReason(String reason) {
        int start = reason.lastIndexOf("[");
        int end = reason.lastIndexOf("]");
        if (start < 0 || end <= start) {
            int marker = reason.indexOf("Missing capabilities:");
            if (marker < 0) return java.util.Set.of();
            start = marker + "Missing capabilities:".length();
            end = reason.length();
        } else {
            start++;
        }
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        for (String value : reason.substring(start, end).split("[,;]")) if (!value.trim().isEmpty()) values.add(value.trim());
        return values;
    }

    private static String issueText(List<EditorialPackIntegrityResult.Issue> issues) {
        StringBuilder out = new StringBuilder();
        for (EditorialPackIntegrityResult.Issue issue : issues) { if (out.length() > 0) out.append("; "); out.append(issue.code()).append(": ").append(issue.message()); }
        return out.toString();
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
