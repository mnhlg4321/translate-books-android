package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluator;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackIntegrityResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackIntegrityValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.nio.file.Paths;
import java.util.UUID;

/** Android persistence/import boundary. It snapshots streams once and never enables execution. */
public final class EditorialPackImportService {
    public static final int MAX_ENTRY_COUNT = 4;
    public static final long MAX_FILE_BYTES = 4L * 1024L * 1024L;
    public static final long MAX_TOTAL_BYTES = 12L * 1024L * 1024L;
    public static final double MAX_COMPRESSION_RATIO = 100.0d;

    private static final String MANIFEST_PATH = "editorial-pack.json";
    private final TranslationRepository database;
    private final EditorialPackStorageLayout storage;
    private final EditorialPackIntegrityValidator integrityValidator;
    private final EditorialCompatibilityEvaluator compatibilityEvaluator;
    private final EditorialEngineProfile engineProfile;

    public EditorialPackImportService(Context context, TranslationRepository database, EditorialEngineProfile engineProfile) {
        if (context == null || database == null || engineProfile == null) throw new IllegalArgumentException("Import service dependencies are required");
        this.database = database;
        this.storage = new EditorialPackStorageLayout(context.getFilesDir().toPath());
        this.integrityValidator = new EditorialPackIntegrityValidator();
        this.compatibilityEvaluator = new EditorialCompatibilityEvaluator();
        this.engineProfile = engineProfile;
    }

    /** Package-private seam for Android recovery tests; production callers use private app storage. */
    EditorialPackImportService(TranslationRepository database, EditorialPackStorageLayout storage, EditorialEngineProfile engineProfile) {
        if (database == null || storage == null || engineProfile == null) throw new IllegalArgumentException("Import service dependencies are required");
        this.database = database;
        this.storage = storage;
        this.integrityValidator = new EditorialPackIntegrityValidator();
        this.compatibilityEvaluator = new EditorialCompatibilityEvaluator();
        this.engineProfile = engineProfile;
    }

    public EditorialPackStorageLayout storageLayout() { return storage; }

    public EditorialPackImportResult importPack(Collection<EditorialPackImportEntry> entries) {
        String importId = UUID.randomUUID().toString();
        Path staging = storage.stagingDirectory(importId);
        boolean importRow = false;
        try {
            storage.createRoots();
            Files.createDirectory(staging);
            storage.writeOwnerMarker(importId);
            importRow = insertImportRow(importId, staging);
            Snapshot snapshot = snapshot(importId, entries);
            updateImport(importId, EditorialPackImportState.SNAPSHOTTED, "", snapshot.packId, snapshot.version, "", false, null);
            EditorialPackIntegrityResult integrity = integrityValidator.validate(snapshot.manifestBytes, snapshot.dataFiles);
            if (!integrity.valid()) {
                String reason = issueText(integrity.issues());
                return blockImport(importId, EditorialPackImportError.INTEGRITY_INVALID, reason, "", "", "", null, false);
            }
            EditorialPackManifest manifest = integrity.manifest();
            updateImport(importId, EditorialPackImportState.INTEGRITY_VALIDATED, "", manifest.packId(), manifest.version(), manifest.canonicalPackHash(), false, null);
            EditorialCompatibilityResult compatibility = compatibilityEvaluator.evaluate(manifest, engineProfile);
            String compatibilityReason = issueText(compatibility.issues());
            updateImport(importId, EditorialPackImportState.COMPATIBILITY_EVALUATED, compatibilityReason, manifest.packId(), manifest.version(), manifest.canonicalPackHash(), false, null);

            ExistingPack existing = existingPack(manifest);
            if (existing != null) {
                if (!existing.canonicalPackHash.equals(manifest.canonicalPackHash())) {
                    return blockImport(importId, EditorialPackImportError.IDENTITY_COLLISION,
                            "Same packId/version is already bound to another canonical hash", manifest.packId(), manifest.version(), manifest.canonicalPackHash(), compatibility.classification(), false);
                }
                if (!storage.hasImmutableMarker(existing.canonicalPackHash)) {
                    return blockImport(importId, EditorialPackImportError.EXISTING_STORAGE_MISSING,
                            "Existing registry row has no complete immutable storage; overwrite is forbidden", manifest.packId(), manifest.version(), manifest.canonicalPackHash(), compatibility.classification(), false);
                }
                updateImport(importId, existing.state, compatibilityReason, manifest.packId(), manifest.version(), manifest.canonicalPackHash(), false, existing.id);
                storage.deleteOwnedStaging(importId);
                return result(importId, existing.state, EditorialPackImportError.NONE, compatibilityReason, manifest, compatibility.classification(), existing.storageKey);
            }
            ExistingHash existingHash = existingHash(manifest.canonicalPackHash());
            if (existingHash != null) {
                return blockImport(importId, EditorialPackImportError.IDENTITY_COLLISION,
                        "Canonical hash is already stored under a different identity", manifest.packId(), manifest.version(), manifest.canonicalPackHash(), compatibility.classification(), false);
            }

            String storageKey = moveToImmutable(importId, manifest.canonicalPackHash());
            try {
                return storeFinal(importId, manifest, compatibility, storageKey, compatibilityReason);
            } catch (SQLiteConstraintException e) {
                return blockImport(importId, EditorialPackImportError.IDENTITY_COLLISION, "Registry identity collision; immutable storage was retained for recovery",
                        manifest.packId(), manifest.version(), manifest.canonicalPackHash(), compatibility.classification(), true);
            } catch (RuntimeException e) {
                return blockImport(importId, EditorialPackImportError.DATABASE_WRITE_FAILED,
                        "Registry transaction failed; immutable storage was retained for recovery: " + safeMessage(e),
                        manifest.packId(), manifest.version(), manifest.canonicalPackHash(), compatibility.classification(), true);
            }
        } catch (ImportFailure e) {
            if (importRow) safelyBlockImport(importId, e.error, e.getMessage());
            return new EditorialPackImportResult(importId, importRow ? EditorialPackImportState.STORED_BLOCKED : EditorialPackImportState.STAGING,
                    e.error, e.getMessage(), "", "", "", null, "");
        } catch (IOException | RuntimeException e) {
            if (importRow) safelyBlockImport(importId, EditorialPackImportError.SNAPSHOT_WRITE_FAILED, safeMessage(e));
            return new EditorialPackImportResult(importId, importRow ? EditorialPackImportState.STORED_BLOCKED : EditorialPackImportState.STAGING,
                    EditorialPackImportError.SNAPSHOT_WRITE_FAILED, safeMessage(e), "", "", "", null, "");
        }
    }

    /** Cleans only importer-owned staging directories and resumes a storage-moved DB transaction when provable. */
    public EditorialPackRecoveryReport recoverOrphanedImports() {
        int cleaned = 0;
        int finalized = 0;
        int blocked = 0;
        int retainedUnknownStorage = 0;
        try {
            storage.createRoots();
            try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                    "SELECT import_id,state,canonical_pack_hash,staging_path,storage_key FROM editorial_pack_imports ORDER BY created_at",
                    null)) {
                while (cursor.moveToNext()) {
                    String importId = cursor.getString(0);
                    EditorialPackImportState state = EditorialPackImportState.valueOf(cursor.getString(1));
                    boolean finalState = state == EditorialPackImportState.STORED_BLOCKED || state == EditorialPackImportState.STORED_READY_FOR_CERTIFICATION;
                    String hash = safe(cursor.getString(2));
                    Path staging = Paths.get(safe(cursor.getString(3))).toAbsolutePath().normalize();
                    if (!finalState && !hash.isEmpty() && storage.hasImmutableMarker(hash)) {
                        if (finalizeMovedStorage(importId, hash)) finalized++; else { safelyBlockImport(importId, EditorialPackImportError.RECOVERY_BLOCKED, "Storage exists but recovery validation/finalization failed"); blocked++; }
                    } else if (storage.isOwnedStagingDirectory(staging, importId)) {
                        if (!finalState) safelyBlockImport(importId, EditorialPackImportError.RECOVERY_BLOCKED, "Orphaned importer staging was cleaned before any pack row existed");
                        try { storage.deleteOwnedStaging(importId); } catch (IOException ignored) {}
                        cleaned++;
                    } else if (!finalState) {
                        safelyBlockImport(importId, EditorialPackImportError.RECOVERY_BLOCKED, "Import has no provable staging or complete storage");
                        blocked++;
                    }
                }
            }
            try (var paths = Files.list(storage.stagingRoot())) {
                var iterator = paths.iterator();
                while (iterator.hasNext()) {
                    Path path = iterator.next();
                    String importId = path.getFileName().toString();
                    if (!EditorialPackStorageLayout.isImportId(importId) || !storage.isOwnedStagingDirectory(path, importId)) continue;
                    if (!importRowExists(importId)) {
                        try { storage.deleteOwnedStaging(importId); cleaned++; } catch (IOException ignored) {}
                    }
                }
            }
            try (var paths = Files.list(storage.immutableRoot())) {
                var iterator = paths.iterator();
                while (iterator.hasNext()) {
                    Path path = iterator.next();
                    String hash = path.getFileName().toString();
                    if (EditorialPackStorageLayout.isCanonicalHash(hash) && storage.hasImmutableMarker(hash) && existingHash(hash) == null) retainedUnknownStorage++;
                }
            }
        } catch (Exception e) {
            blocked++;
        }
        return new EditorialPackRecoveryReport(cleaned, finalized, blocked, retainedUnknownStorage);
    }

    private Snapshot snapshot(String importId, Collection<EditorialPackImportEntry> entries) throws IOException, ImportFailure {
        if (entries == null) throw new ImportFailure(EditorialPackImportError.NULL_INPUT, "Pack entries are required");
        if (entries.isEmpty() || entries.size() > MAX_ENTRY_COUNT) throw new ImportFailure(EditorialPackImportError.ENTRY_COUNT_LIMIT, "Pack must contain exactly four root entries");
        Map<String, EditorialPackImportEntry> byCollisionKey = new HashMap<>();
        Map<String, byte[]> bytesByPath = new LinkedHashMap<>();
        long total = 0;
        String packId = "";
        String version = "";
        for (EditorialPackImportEntry entry : entries) {
            if (entry == null) throw new ImportFailure(EditorialPackImportError.NULL_INPUT, "Pack entry is null");
            if (entry.symbolicLink()) throw new ImportFailure(EditorialPackImportError.SYMLINK_FORBIDDEN, "Symbolic-link entries are forbidden");
            final String path;
            try { path = EditorialPackStorageLayout.normalizeEntryPath(entry.path()); }
            catch (IllegalArgumentException e) { throw new ImportFailure(EditorialPackImportError.INVALID_ENTRY_PATH, e.getMessage()); }
            String collisionKey = path.toLowerCase(Locale.ROOT);
            if (byCollisionKey.put(collisionKey, entry) != null) throw new ImportFailure(EditorialPackImportError.DUPLICATE_NORMALIZED_PATH, "Duplicate normalized entry path: " + path);
            if (entry.compressedLength() == 0 && entry.declaredLength() > 0) throw new ImportFailure(EditorialPackImportError.COMPRESSION_RATIO_LIMIT, "Compressed length is zero for a non-empty entry");
            SnapshotFile snapshotFile = readOnce(importId, path, entry, total);
            total += snapshotFile.bytes.length;
            if (entry.declaredLength() >= 0 && entry.declaredLength() != snapshotFile.bytes.length) throw new ImportFailure(EditorialPackImportError.TRUNCATED_STREAM, "Declared stream length differs for " + path);
            if (entry.compressedLength() > 0 && snapshotFile.bytes.length / (double) entry.compressedLength() > MAX_COMPRESSION_RATIO) throw new ImportFailure(EditorialPackImportError.COMPRESSION_RATIO_LIMIT, "Compression ratio exceeds limit for " + path);
            bytesByPath.put(path, snapshotFile.bytes);
            if (MANIFEST_PATH.equals(path)) {
                try {
                    EditorialPackManifest manifest = EditorialPackManifest.parse(snapshotFile.bytes);
                    packId = manifest.packId();
                    version = manifest.version();
                } catch (IllegalArgumentException ignored) {
                    // The integrity validator owns the typed manifest failure; snapshot still remains exact.
                }
            }
        }
        if (!bytesByPath.containsKey(MANIFEST_PATH)) throw new ImportFailure(EditorialPackImportError.INTEGRITY_INVALID, "editorial-pack.json is missing");
        storage.writeSnapshotMarker(importId);
        LinkedHashMap<String, byte[]> dataFiles = new LinkedHashMap<>(bytesByPath);
        byte[] manifestBytes = dataFiles.remove(MANIFEST_PATH);
        return new Snapshot(manifestBytes, dataFiles, packId, version, total);
    }

    private SnapshotFile readOnce(String importId, String normalizedPath, EditorialPackImportEntry entry, long currentTotal) throws IOException, ImportFailure {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
        byte[] buffer = new byte[8192];
        long size = 0;
        try (InputStream input = entry.stream(); FileChannel output = FileChannel.open(storage.stagingEntry(importId, normalizedPath), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (read == 0) continue;
                size += read;
                if (size > MAX_FILE_BYTES || currentTotal + size > MAX_TOTAL_BYTES) throw new ImportFailure(size > MAX_FILE_BYTES ? EditorialPackImportError.ENTRY_SIZE_LIMIT : EditorialPackImportError.TOTAL_SIZE_LIMIT, "Pack size limit exceeded");
                digest.update(buffer, 0, read);
                bytes.write(buffer, 0, read);
                ByteBuffer pending = ByteBuffer.wrap(buffer, 0, read);
                while (pending.hasRemaining()) output.write(pending);
            }
            output.force(true);
        } catch (ImportFailure e) { throw e; } catch (IOException e) { throw new ImportFailure(EditorialPackImportError.SNAPSHOT_WRITE_FAILED, "Could not snapshot " + normalizedPath + ": " + e.getMessage()); }
        byte[] snapshot = bytes.toByteArray();
        String sha256 = hex(digest.digest());
        if (!EditorialPackStorageLayout.isCanonicalHash(sha256)) throw new AssertionError("SHA-256 digest is invalid");
        return new SnapshotFile(snapshot, sha256, size);
    }

    private String moveToImmutable(String importId, String hash) throws IOException, ImportFailure {
        storage.requireHash(hash);
        Path source = storage.stagingDirectory(importId);
        Path destination = storage.immutableDirectory(hash);
        if (Files.exists(destination)) {
            if (storage.hasImmutableMarker(hash)) return hash;
            throw new ImportFailure(EditorialPackImportError.STORAGE_COLLISION, "Immutable hash directory exists without a complete marker");
        }
        storage.writeStorageMarkerInDirectory(source, hash, importId);
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            throw new ImportFailure(EditorialPackImportError.STORAGE_MOVE_FAILED, "Atomic move is not supported by private storage");
        } catch (IOException e) {
            throw new ImportFailure(EditorialPackImportError.STORAGE_MOVE_FAILED, "Could not move immutable pack storage: " + e.getMessage());
        }
        return hash;
    }

    private EditorialPackImportResult storeFinal(String importId, EditorialPackManifest manifest, EditorialCompatibilityResult compatibility,
                                                 String storageKey, String compatibilityReason) {
        SQLiteDatabase db = database.editorialWritableDatabase();
        long now = System.currentTimeMillis();
        boolean blocked = compatibility.blocked() || compatibility.classification() == EditorialPackCompatibilityClass.BLOCKED
                || compatibility.classification() == EditorialPackCompatibilityClass.INVALID;
        EditorialPackImportState finalState = blocked ? EditorialPackImportState.STORED_BLOCKED : EditorialPackImportState.STORED_READY_FOR_CERTIFICATION;
        db.beginTransaction();
        try {
            ContentValues pack = new ContentValues();
            pack.put("pack_id", manifest.packId()); pack.put("version", manifest.version()); pack.put("canonical_pack_hash", manifest.canonicalPackHash());
            pack.put("contract_version", manifest.contractVersion()); pack.put("schema_version", manifest.schemaVersion()); pack.put("minimum_engine_version", manifest.minimumEngineVersion());
            pack.put("compatibility_class", compatibility.classification().name()); pack.put("state", finalState.name()); pack.put("storage_key", storageKey);
            pack.put("manifest_canonical_json", manifest.canonicalJson()); pack.put("created_at", now); pack.put("validated_at", now);
            pack.put("engine_version_used", engineProfile.engineVersion()); pack.put("blocked_reason", compatibilityReason);
            long packRowId = db.insertOrThrow("editorial_packs", null, pack);
            for (EditorialPackManifest.FileEntry file : manifest.fileEntries()) {
                ContentValues row = new ContentValues(); row.put("pack_row_id", packRowId); row.put("declared_role", file.role().name()); row.put("normalized_relative_path", file.path());
                row.put("byte_length", file.byteLength()); row.put("sha256", file.sha256()); row.put("storage_key", storageKey + "/" + file.path());
                db.insertOrThrow("editorial_pack_files", null, row);
            }
            ContentValues compatibilityRow = new ContentValues(); compatibilityRow.put("import_id", importId); compatibilityRow.put("pack_row_id", packRowId);
            compatibilityRow.put("canonical_pack_hash", manifest.canonicalPackHash()); compatibilityRow.put("engine_version_used", engineProfile.engineVersion());
            compatibilityRow.put("machine_contract_fingerprint", compatibility.machineContractFingerprint()); compatibilityRow.put("compatibility_class", compatibility.classification().name());
            compatibilityRow.put("required_class", compatibility.requiredClass().name()); compatibilityRow.put("blocked_reason", compatibilityReason); compatibilityRow.put("evaluated_at", now);
            db.insertOrThrow("editorial_pack_compatibility_results", null, compatibilityRow);
            ContentValues importUpdate = new ContentValues(); importUpdate.put("state", finalState.name()); importUpdate.put("pack_id", manifest.packId()); importUpdate.put("pack_version", manifest.version());
            importUpdate.put("canonical_pack_hash", manifest.canonicalPackHash()); importUpdate.put("storage_key", storageKey); importUpdate.put("storage_moved", 1); importUpdate.put("pack_row_id", packRowId); importUpdate.put("blocked_reason", compatibilityReason); importUpdate.put("updated_at", now);
            if (db.update("editorial_pack_imports", importUpdate, "import_id=?", new String[]{importId}) != 1) throw new IllegalStateException("Import row disappeared before final commit");
            db.setTransactionSuccessful();
            return result(importId, finalState, EditorialPackImportError.NONE, compatibilityReason, manifest, compatibility.classification(), storageKey);
        } finally { db.endTransaction(); }
    }

    private boolean finalizeMovedStorage(String importId, String hash) {
        try {
            Path directory = storage.immutableDirectory(hash);
            byte[] manifestBytes = Files.readAllBytes(directory.resolve(MANIFEST_PATH));
            EditorialPackManifest manifest = EditorialPackManifest.parse(manifestBytes);
            Map<String, byte[]> data = new LinkedHashMap<>();
            for (EditorialPackManifest.FileEntry file : manifest.fileEntries()) data.put(file.path(), Files.readAllBytes(directory.resolve(file.path())));
            EditorialPackIntegrityResult integrity = integrityValidator.validate(manifestBytes, data);
            if (!integrity.valid() || !hash.equals(manifest.canonicalPackHash())) return false;
            EditorialCompatibilityResult compatibility = compatibilityEvaluator.evaluate(manifest, engineProfile);
            if (existingPack(manifest) != null) {
                updateImport(importId, existingPack(manifest).state, issueText(compatibility.issues()), manifest.packId(), manifest.version(), hash, true, existingPack(manifest).id);
                return true;
            }
            storeFinal(importId, manifest, compatibility, hash, issueText(compatibility.issues()));
            return true;
        } catch (Exception e) { return false; }
    }

    private ExistingPack existingPack(EditorialPackManifest manifest) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery("SELECT id,canonical_pack_hash,state,storage_key FROM editorial_packs WHERE pack_id=? AND version=?", new String[]{manifest.packId(), manifest.version()})) {
            return cursor.moveToFirst() ? new ExistingPack(cursor.getLong(0), cursor.getString(1), EditorialPackImportState.valueOf(cursor.getString(2)), cursor.getString(3)) : null;
        }
    }

    private ExistingHash existingHash(String hash) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery("SELECT id FROM editorial_packs WHERE canonical_pack_hash=?", new String[]{hash})) {
            return cursor.moveToFirst() ? new ExistingHash(cursor.getLong(0)) : null;
        }
    }

    private boolean insertImportRow(String importId, Path staging) {
        ContentValues values = new ContentValues(); values.put("import_id", importId); values.put("state", EditorialPackImportState.STAGING.name()); values.put("staging_path", staging.toString());
        values.put("created_at", System.currentTimeMillis()); values.put("updated_at", System.currentTimeMillis());
        database.editorialWritableDatabase().insertOrThrow("editorial_pack_imports", null, values);
        return true;
    }

    private void updateImport(String importId, EditorialPackImportState state, String reason, String packId, String version, String hash, boolean moved, Long packRowId) {
        ContentValues values = new ContentValues(); values.put("state", state.name()); values.put("blocked_reason", reason == null ? "" : reason); values.put("pack_id", packId == null ? "" : packId); values.put("pack_version", version == null ? "" : version); values.put("canonical_pack_hash", hash == null ? "" : hash); values.put("storage_moved", moved ? 1 : 0); values.put("updated_at", System.currentTimeMillis());
        if (packRowId == null) values.putNull("pack_row_id"); else values.put("pack_row_id", packRowId);
        database.editorialWritableDatabase().update("editorial_pack_imports", values, "import_id=?", new String[]{importId});
    }

    private EditorialPackImportResult blockImport(String importId, EditorialPackImportError error, String reason, String packId, String version, String hash,
                                                   EditorialPackCompatibilityClass compatibility, boolean retainStorage) {
        safelyBlockImport(importId, error, reason);
        if (!retainStorage) try { storage.deleteOwnedStaging(importId); } catch (IOException ignored) {}
        return new EditorialPackImportResult(importId, EditorialPackImportState.STORED_BLOCKED, error, reason, packId, version, hash, compatibility, retainStorage ? hash : "");
    }

    private void safelyBlockImport(String importId, EditorialPackImportError error, String reason) {
        try {
            ContentValues values = new ContentValues();
            values.put("state", EditorialPackImportState.STORED_BLOCKED.name());
            values.put("blocked_reason", error.name() + ": " + reason);
            values.put("updated_at", System.currentTimeMillis());
            database.editorialWritableDatabase().update("editorial_pack_imports", values, "import_id=?", new String[]{importId});
        } catch (Exception ignored) {}
    }

    private boolean importRowExists(String importId) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery("SELECT 1 FROM editorial_pack_imports WHERE import_id=?", new String[]{importId})) { return cursor.moveToFirst(); }
    }

    private EditorialPackImportResult result(String importId, EditorialPackImportState state, EditorialPackImportError error, String reason,
                                             EditorialPackManifest manifest, EditorialPackCompatibilityClass compatibility, String storageKey) {
        return new EditorialPackImportResult(importId, state, error, reason, manifest.packId(), manifest.version(), manifest.canonicalPackHash(), compatibility, storageKey);
    }

    private static String issueText(List<EditorialPackIntegrityResult.Issue> issues) {
        if (issues == null || issues.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (EditorialPackIntegrityResult.Issue issue : issues) { if (out.length() > 0) out.append("; "); out.append(issue.code()).append(": ").append(issue.message()); }
        return out.toString();
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2); for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 0xff)); return out.toString();
    }

    private static String safe(String value) { return value == null ? "" : value; }
    private static String safeMessage(Throwable error) { return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(); }

    public record EditorialPackRecoveryReport(int cleanedStaging, int finalizedStorage, int blockedImports, int retainedUnknownStorage) {}
    private record Snapshot(byte[] manifestBytes, Map<String, byte[]> dataFiles, String packId, String version, long totalBytes) {}
    private record SnapshotFile(byte[] bytes, String sha256, long byteLength) {}
    private record ExistingPack(long id, String canonicalPackHash, EditorialPackImportState state, String storageKey) {}
    private record ExistingHash(long id) {}
    private static final class ImportFailure extends Exception {
        final EditorialPackImportError error;
        ImportFailure(EditorialPackImportError error, String message) { super(message); this.error = error; }
    }
}
