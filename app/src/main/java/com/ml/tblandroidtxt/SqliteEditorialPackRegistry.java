package com.ml.tblandroidtxt;

import android.database.Cursor;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackIntegrityResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackIntegrityValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
                "SELECT manifest_canonical_json,canonical_pack_hash FROM editorial_packs WHERE canonical_pack_hash=?", new String[]{canonicalPackHash})) {
            return cursor.moveToFirst() ? parseIfComplete(cursor.getString(0), cursor.getString(1)) : Optional.empty();
        }
    }

    @Override public Optional<EditorialPackManifest> findByIdentity(String packId, String version) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT manifest_canonical_json,canonical_pack_hash FROM editorial_packs WHERE pack_id=? AND version=?", new String[]{packId, version})) {
            return cursor.moveToFirst() ? parseIfComplete(cursor.getString(0), cursor.getString(1)) : Optional.empty();
        }
    }

    @Override public List<EditorialPackManifest> list() {
        ArrayList<EditorialPackManifest> result = new ArrayList<>();
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT manifest_canonical_json,canonical_pack_hash FROM editorial_packs ORDER BY pack_id,version,canonical_pack_hash", null)) {
            while (cursor.moveToNext()) parseIfComplete(cursor.getString(0), cursor.getString(1)).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    private Optional<EditorialPackManifest> parseIfComplete(String canonicalJson, String hash) {
        try {
            if (!storage.hasImmutableMarker(hash)) return Optional.empty();
            EditorialPackManifest manifest = EditorialPackManifest.parse(canonicalJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (!hash.equals(manifest.canonicalPackHash()) || !hash.equals(manifest.calculatedCanonicalPackHash())) return Optional.empty();
            Map<String, byte[]> dataFiles = new LinkedHashMap<>();
            for (EditorialPackManifest.FileEntry file : manifest.fileEntries()) {
                if (!Files.isRegularFile(storage.immutableEntry(hash, file.path()))) return Optional.empty();
                dataFiles.put(file.path(), Files.readAllBytes(storage.immutableEntry(hash, file.path())));
            }
            EditorialPackIntegrityResult integrity = integrityValidator.validate(
                    canonicalJson.getBytes(StandardCharsets.UTF_8), dataFiles);
            if (!integrity.valid() || !hash.equals(integrity.manifest().canonicalPackHash())) return Optional.empty();
            return Optional.of(manifest);
        } catch (RuntimeException | IOException e) {
            return Optional.empty();
        }
    }
}
