package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluator;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluationContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluationResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityProvenance;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityReasonCode;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackIntegrityResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackIntegrityValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackValidationCode;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
    private final EditorialEngineProfileResolver trustedResolver;
    private final EditorialPackCompatibilityEvaluationDao evaluationDao;

    public EditorialPackImportService(Context context, TranslationRepository database, EditorialEngineProfile engineProfile) {
        if (context == null || database == null || engineProfile == null) throw new IllegalArgumentException("Import service dependencies are required");
        this.database = database;
        this.storage = new EditorialPackStorageLayout(context.getFilesDir().toPath());
        this.integrityValidator = new EditorialPackIntegrityValidator();
        this.compatibilityEvaluator = new EditorialCompatibilityEvaluator();
        this.engineProfile = engineProfile;
        this.trustedResolver = null;
        this.evaluationDao = new EditorialPackCompatibilityEvaluationDao(database);
    }

    /** Production composition-root constructor. Trusted profiles come only from the bundled registry. */
    public EditorialPackImportService(Context context, TranslationRepository database, EditorialEngineProfileResolver trustedResolver) {
        if (context == null || database == null || trustedResolver == null) throw new IllegalArgumentException("Import service dependencies are required");
        this.database = database;
        this.storage = new EditorialPackStorageLayout(context.getFilesDir().toPath());
        this.integrityValidator = new EditorialPackIntegrityValidator();
        this.compatibilityEvaluator = new EditorialCompatibilityEvaluator();
        this.engineProfile = null;
        this.trustedResolver = trustedResolver;
        this.evaluationDao = new EditorialPackCompatibilityEvaluationDao(database);
    }

    /** Package-private seam for Android recovery tests; production callers use private app storage. */
    EditorialPackImportService(TranslationRepository database, EditorialPackStorageLayout storage, EditorialEngineProfile engineProfile) {
        if (database == null || storage == null || engineProfile == null) throw new IllegalArgumentException("Import service dependencies are required");
        this.database = database;
        this.storage = storage;
        this.integrityValidator = new EditorialPackIntegrityValidator();
        this.compatibilityEvaluator = new EditorialCompatibilityEvaluator();
        this.engineProfile = engineProfile;
        this.trustedResolver = null;
        this.evaluationDao = new EditorialPackCompatibilityEvaluationDao(database);
    }

    /** Package-private production seam for Android integration tests. */
    EditorialPackImportService(TranslationRepository database, EditorialPackStorageLayout storage, EditorialEngineProfileResolver trustedResolver) {
        if (database == null || storage == null || trustedResolver == null) throw new IllegalArgumentException("Import service dependencies are required");
        this.database = database;
        this.storage = storage;
        this.integrityValidator = new EditorialPackIntegrityValidator();
        this.compatibilityEvaluator = new EditorialCompatibilityEvaluator();
        this.engineProfile = null;
        this.trustedResolver = trustedResolver;
        this.evaluationDao = new EditorialPackCompatibilityEvaluationDao(database);
    }

    public EditorialPackStorageLayout storageLayout() { return storage; }

    public EditorialPackImportResult importPack(Collection<EditorialPackImportEntry> entries) {
        return importPack(entries, state -> { });
    }

    /** Imports already separated source entries and reports durable state transitions. */
    public EditorialPackImportResult importPack(Collection<EditorialPackImportEntry> entries,
                                                EditorialPackImportProgressListener listener) {
        EditorialPackImportProgressListener progress = listener == null ? state -> { } : listener;
        String importId = UUID.randomUUID().toString();
        Path staging = storage.stagingDirectory(importId);
        boolean importRow = false;
        try {
            storage.createRoots();
            Files.createDirectory(staging);
            storage.writeOwnerMarker(importId);
            importRow = insertImportRow(importId, staging);
            Snapshot snapshot = snapshot(importId, entries);
            notifyState(progress, EditorialPackImportState.SNAPSHOTTED);
            updateImport(importId, EditorialPackImportState.SNAPSHOTTED, "", snapshot.packId, snapshot.version, "", false, null);
            EditorialPackIntegrityResult integrity = integrityValidator.validate(snapshot.manifestBytes, snapshot.dataFiles);
            if (!integrity.valid()) {
                String reason = issueText(integrity.issues());
                notifyState(progress, EditorialPackImportState.STORED_BLOCKED);
                return blockImport(importId, EditorialPackImportError.INTEGRITY_INVALID, reason, "", "", "", null, false);
            }
            EditorialPackManifest manifest = integrity.manifest();
            notifyState(progress, EditorialPackImportState.INTEGRITY_VALIDATED);
            // Duplicate identity/hash checks precede evaluation. Re-import is a read-only lookup,
            // never a new resolver call or a new compatibility history entry.
            ExistingPack existing = existingPack(manifest);
            if (existing != null) {
                if (!existing.canonicalPackHash.equals(manifest.canonicalPackHash())) {
                    return blockImport(importId, EditorialPackImportError.IDENTITY_COLLISION,
                            "Same packId/version is already bound to another canonical hash", manifest.packId(), manifest.version(), manifest.canonicalPackHash(), null, false);
                }
                if (!storage.hasImmutableMarker(existing.canonicalPackHash)) {
                    return blockImport(importId, EditorialPackImportError.EXISTING_STORAGE_MISSING,
                            "Existing registry row has no complete immutable storage; overwrite is forbidden", manifest.packId(), manifest.version(), manifest.canonicalPackHash(), null, false);
                }
                PersistedCompatibility persisted = latestCompatibility(manifest.canonicalPackHash());
                CompatibilityDecision existingDecision = persisted == null
                        ? CompatibilityDecision.blocked(EditorialCompatibilityReasonCode.BLOCKED, "Existing pack has no compatibility row")
                        : CompatibilityDecision.persisted(persisted);
                String existingReason = persisted == null ? "PACK_ALREADY_EXISTS" : persisted.reasonCode.name();
                // v14 keeps a unique non-empty import hash index. A duplicate import
                // row therefore retains an empty hash and points at the immutable
                // existing pack row instead of overwriting/claiming its hash.
                updateImport(importId, existing.state, existingReason, manifest.packId(), manifest.version(), "", false, existing.id);
                storage.deleteOwnedStaging(importId);
                notifyState(progress, existing.state);
                return result(importId, existing.state, EditorialPackImportError.NONE, existingReason, manifest, existingDecision, existing.storageKey, true);
            }
            ExistingHash existingHash = existingHash(manifest.canonicalPackHash());
            if (existingHash != null) {
                return blockImport(importId, EditorialPackImportError.IDENTITY_COLLISION,
                        "Canonical hash is already stored under a different identity", manifest.packId(), manifest.version(), manifest.canonicalPackHash(), null, false);
            }

            updateImport(importId, EditorialPackImportState.INTEGRITY_VALIDATED, "", manifest.packId(), manifest.version(), manifest.canonicalPackHash(), false, null);
            CompatibilityDecision compatibility = evaluate(manifest);
            String compatibilityReason = compatibilityReason(compatibility);
            notifyState(progress, EditorialPackImportState.COMPATIBILITY_EVALUATED);
            updateImport(importId, EditorialPackImportState.COMPATIBILITY_EVALUATED, compatibilityReason, manifest.packId(), manifest.version(), manifest.canonicalPackHash(), false, null);

            String storageKey = moveToImmutable(importId, manifest.canonicalPackHash());
            try {
                EditorialPackImportResult finalResult = storeFinal(importId, manifest, compatibility, storageKey, compatibilityReason);
                notifyState(progress, finalResult.state());
                return finalResult;
            } catch (SQLiteConstraintException e) {
                notifyState(progress, EditorialPackImportState.STORED_BLOCKED);
                return blockImport(importId, EditorialPackImportError.IDENTITY_COLLISION, "Registry identity collision; immutable storage was retained for recovery",
                        manifest.packId(), manifest.version(), manifest.canonicalPackHash(), compatibility.outcome, true);
            } catch (RuntimeException e) {
                notifyState(progress, EditorialPackImportState.STORED_BLOCKED);
                return blockImport(importId, EditorialPackImportError.DATABASE_WRITE_FAILED,
                        "COMPATIBILITY_PERSISTENCE_FAILURE: Registry transaction failed; immutable storage was retained for recovery: " + safeMessage(e),
                        manifest.packId(), manifest.version(), manifest.canonicalPackHash(), compatibility.outcome, true);
            }
        } catch (ImportFailure e) {
            if (importRow) safelyBlockImport(importId, e.error, e.getMessage());
            if (importRow) notifyState(progress, EditorialPackImportState.STORED_BLOCKED);
            return new EditorialPackImportResult(importId, importRow ? EditorialPackImportState.STORED_BLOCKED : EditorialPackImportState.STAGING,
                    e.error, e.getMessage(), "", "", "", null, "");
        } catch (IOException | RuntimeException e) {
            if (importRow) safelyBlockImport(importId, EditorialPackImportError.SNAPSHOT_WRITE_FAILED, safeMessage(e));
            if (importRow) notifyState(progress, EditorialPackImportState.STORED_BLOCKED);
            return new EditorialPackImportResult(importId, importRow ? EditorialPackImportState.STORED_BLOCKED : EditorialPackImportState.STAGING,
                    EditorialPackImportError.SNAPSHOT_WRITE_FAILED, safeMessage(e), "", "", "", null, "");
        }
    }

    /**
     * Imports one selected ZIP stream.  The stream is consumed exactly once at
     * this headless boundary; UI code never parses, previews, hashes or reopens
     * the URI.  ZIP entries are bounded in memory before the normal private
     * staging snapshot/validation pipeline is invoked.
     */
    public EditorialPackImportResult importZip(InputStream zipStream) {
        return importZip(zipStream, state -> { });
    }

    public EditorialPackImportResult importZip(InputStream zipStream,
                                               EditorialPackImportProgressListener listener) {
        String importId = UUID.randomUUID().toString();
        if (zipStream == null) {
            return new EditorialPackImportResult(importId, EditorialPackImportState.STAGING,
                    EditorialPackImportError.NULL_INPUT, "ZIP stream is required", "", "", "", null, "");
        }
        ArrayList<EditorialPackImportEntry> entries = new ArrayList<>();
        long total = 0L;
        ZipStructureProbe structureProbe = new ZipStructureProbe(zipStream);
        try (ZipInputStream zip = new ZipInputStream(structureProbe)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    return zipFailure(importId, EditorialPackImportError.INVALID_ENTRY_PATH, "ZIP directory entries are forbidden");
                }
                if (zipEntryIsSymbolicLink(entry)) {
                    return zipFailure(importId, EditorialPackImportError.SYMLINK_FORBIDDEN, "Symbolic-link ZIP entries are forbidden");
                }
                final String path;
                try {
                    path = EditorialPackStorageLayout.normalizeEntryPath(entry.getName());
                } catch (IllegalArgumentException e) {
                    return zipFailure(importId, EditorialPackImportError.INVALID_ENTRY_PATH, e.getMessage());
                }
                if (entries.size() >= MAX_ENTRY_COUNT) {
                    return zipFailure(importId, EditorialPackImportError.ENTRY_COUNT_LIMIT, "Pack must contain exactly four root entries");
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                long size = 0L;
                while ((read = zip.read(buffer)) != -1) {
                    if (read == 0) continue;
                    size += read;
                    if (size > MAX_FILE_BYTES || total + size > MAX_TOTAL_BYTES) {
                        return zipFailure(importId, size > MAX_FILE_BYTES ? EditorialPackImportError.ENTRY_SIZE_LIMIT : EditorialPackImportError.TOTAL_SIZE_LIMIT,
                                "Pack size limit exceeded");
                    }
                    bytes.write(buffer, 0, read);
                }
                total += size;
                long compressed = entry.getCompressedSize();
                if (compressed == 0L && size > 0L) {
                    return zipFailure(importId, EditorialPackImportError.COMPRESSION_RATIO_LIMIT,
                            "Compressed length is zero for a non-empty entry");
                }
                if (compressed > 0L && size / (double) compressed > MAX_COMPRESSION_RATIO) {
                    return zipFailure(importId, EditorialPackImportError.COMPRESSION_RATIO_LIMIT,
                            "Compression ratio exceeds limit for " + path);
                }
                entries.add(new EditorialPackImportEntry(path,
                        new ByteArrayInputStream(bytes.toByteArray()), size, compressed, false));
                zip.closeEntry();
            }
            if (!structureProbe.hasCompleteEndOfCentralDirectory()) {
                return zipFailure(importId, EditorialPackImportError.TRUNCATED_STREAM, "ZIP central directory/end record is incomplete");
            }
            if (entries.isEmpty()) return zipFailure(importId, EditorialPackImportError.ENTRY_COUNT_LIMIT, "ZIP contains no entries");
        } catch (IOException e) {
            if (structureProbe.hasInvalidLocalEntryPath()) {
                return zipFailure(importId, EditorialPackImportError.INVALID_ENTRY_PATH, "ZIP entry path is not a root-level relative path");
            }
            return zipFailure(importId, EditorialPackImportError.TRUNCATED_STREAM, "Could not read ZIP stream: " + safeMessage(e));
        }
        return importPack(entries, listener);
    }

    /**
     * Keeps only bounded byte metadata while the selected ZIP stream is consumed once.
     * Android's ZipInputStream may return null for a short malformed stream instead of
     * throwing; the end-record check restores deterministic truncation classification.
     */
    private static final class ZipStructureProbe extends FilterInputStream {
        private static final int END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50;
        private static final int END_OF_CENTRAL_DIRECTORY_BYTES = 22;
        private static final int MAX_END_RECORD_WINDOW = 65_557;
        private final byte[] tail = new byte[MAX_END_RECORD_WINDOW];
        private int tailStart;
        private int tailSize;

        ZipStructureProbe(InputStream input) { super(input); }

        @Override public int read() throws IOException {
            int value = super.read();
            if (value >= 0) append(value);
            return value;
        }

        @Override public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = super.read(buffer, offset, length);
            if (count > 0) for (int i = 0; i < count; i++) append(buffer[offset + i] & 0xff);
            return count;
        }

        boolean hasCompleteEndOfCentralDirectory() {
            if (tailSize < END_OF_CENTRAL_DIRECTORY_BYTES) return false;
            for (int offset = 0; offset <= tailSize - END_OF_CENTRAL_DIRECTORY_BYTES; offset++) {
                if (readTail(offset) != 0x50 || readTail(offset + 1) != 0x4b
                        || readTail(offset + 2) != 0x05 || readTail(offset + 3) != 0x06) continue;
                int commentLength = readTail(offset + 20) | (readTail(offset + 21) << 8);
                if (offset + END_OF_CENTRAL_DIRECTORY_BYTES + commentLength == tailSize) return true;
            }
            return false;
        }

        boolean hasInvalidLocalEntryPath() {
            for (int offset = tailSize - 30; offset >= 0; offset--) {
                if (readTail(offset) != 0x50 || readTail(offset + 1) != 0x4b
                        || readTail(offset + 2) != 0x03 || readTail(offset + 3) != 0x04) continue;
                int nameLength = readTail(offset + 26) | (readTail(offset + 27) << 8);
                if (offset + 30 + nameLength > tailSize) continue;
                byte[] nameBytes = new byte[nameLength];
                for (int i = 0; i < nameLength; i++) nameBytes[i] = (byte) readTail(offset + 30 + i);
                try {
                    EditorialPackStorageLayout.normalizeEntryPath(new String(nameBytes, StandardCharsets.UTF_8));
                } catch (IllegalArgumentException invalidPath) {
                    return true;
                }
            }
            return false;
        }

        private void append(int value) {
            if (tailSize < tail.length) {
                tail[(tailStart + tailSize) % tail.length] = (byte) value;
                tailSize++;
            } else {
                tail[tailStart] = (byte) value;
                tailStart = (tailStart + 1) % tail.length;
            }
        }

        private int readTail(int offset) {
            return tail[(tailStart + offset) % tail.length] & 0xff;
        }
    }

    private EditorialPackImportResult zipFailure(String importId, EditorialPackImportError error, String reason) {
        return new EditorialPackImportResult(importId, EditorialPackImportState.STAGING, error, reason, "", "", "", null, "");
    }

    private static void notifyState(EditorialPackImportProgressListener listener, EditorialPackImportState state) {
        try { listener.onState(state); } catch (RuntimeException ignored) { }
    }

    /** Android's ZipEntry has no public Unix-mode API; inspect it only when the runtime exposes it. */
    private static boolean zipEntryIsSymbolicLink(ZipEntry entry) {
        try {
            Field field = ZipEntry.class.getDeclaredField("extraAttributes");
            field.setAccessible(true);
            int attributes = field.getInt(entry);
            int unixMode = (attributes >>> 16) & 0xF000;
            return unixMode == 0xA000;
        } catch (Throwable ignored) {
            return false;
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

    private EditorialPackImportResult storeFinal(String importId, EditorialPackManifest manifest, CompatibilityDecision compatibility,
                                                 String storageKey, String compatibilityReason) {
        SQLiteDatabase db = database.editorialWritableDatabase();
        long now = System.currentTimeMillis();
        boolean blocked = compatibility.blocked || compatibility.outcome == EditorialPackCompatibilityClass.BLOCKED
                || compatibility.outcome == EditorialPackCompatibilityClass.INVALID;
        // The bundled production profile is intentionally non-executable in B2. This
        // defensive gate prevents a future resolver regression from creating a ready row.
        if (trustedResolver != null && compatibility.trustedProfile != null
                && !EditorialEngineProfileResolver.isExecutableContractProfile(compatibility.trustedProfile)
                && compatibility.outcome == EditorialPackCompatibilityClass.DATA_COMPATIBLE) {
            blocked = true;
        }
        EditorialPackImportState finalState = blocked ? EditorialPackImportState.STORED_BLOCKED : EditorialPackImportState.STORED_READY_FOR_CERTIFICATION;
        db.beginTransaction();
        try {
            ContentValues pack = new ContentValues();
            pack.put("pack_id", manifest.packId()); pack.put("version", manifest.version()); pack.put("canonical_pack_hash", manifest.canonicalPackHash());
            pack.put("contract_version", manifest.contractVersion()); pack.put("schema_version", manifest.schemaVersion()); pack.put("minimum_engine_version", manifest.minimumEngineVersion());
            pack.put("compatibility_class", compatibility.outcome.name()); pack.put("state", finalState.name()); pack.put("storage_key", storageKey);
            pack.put("manifest_canonical_json", manifest.canonicalJson()); pack.put("created_at", now); pack.put("validated_at", now);
            pack.put("engine_version_used", engineVersionUsed(compatibility)); pack.put("blocked_reason", compatibilityReason);
            long packRowId = db.insertOrThrow("editorial_packs", null, pack);
            for (EditorialPackManifest.FileEntry file : manifest.fileEntries()) {
                ContentValues row = new ContentValues(); row.put("pack_row_id", packRowId); row.put("declared_role", file.role().name()); row.put("normalized_relative_path", file.path());
                row.put("byte_length", file.byteLength()); row.put("sha256", file.sha256()); row.put("storage_key", storageKey + "/" + file.path());
                db.insertOrThrow("editorial_pack_files", null, row);
            }
            ContentValues compatibilityRow = new ContentValues(); compatibilityRow.put("import_id", importId); compatibilityRow.put("pack_row_id", packRowId);
            compatibilityRow.put("canonical_pack_hash", manifest.canonicalPackHash()); compatibilityRow.put("engine_version_used", engineVersionUsed(compatibility));
            compatibilityRow.put("machine_contract_fingerprint", trustedMachineFingerprint(compatibility)); compatibilityRow.put("compatibility_class", compatibility.outcome.name());
            compatibilityRow.put("required_class", compatibility.requiredClass.name()); compatibilityRow.put("blocked_reason", compatibilityReason); compatibilityRow.put("evaluated_at", now);
            long compatibilityResultId = db.insertOrThrow("editorial_pack_compatibility_results", null, compatibilityRow);
            appendTrustedEvidenceIfAvailable(importId, packRowId, compatibilityResultId, manifest, compatibility, compatibilityReason, now);
            ContentValues importUpdate = new ContentValues(); importUpdate.put("state", finalState.name()); importUpdate.put("pack_id", manifest.packId()); importUpdate.put("pack_version", manifest.version());
            importUpdate.put("canonical_pack_hash", manifest.canonicalPackHash()); importUpdate.put("storage_key", storageKey); importUpdate.put("storage_moved", 1); importUpdate.put("pack_row_id", packRowId); importUpdate.put("blocked_reason", compatibilityReason); importUpdate.put("updated_at", now);
            if (db.update("editorial_pack_imports", importUpdate, "import_id=?", new String[]{importId}) != 1) throw new IllegalStateException("Import row disappeared before final commit");
            db.setTransactionSuccessful();
            return result(importId, finalState, EditorialPackImportError.NONE, compatibilityReason, manifest, compatibility, storageKey);
        } finally { db.endTransaction(); }
    }

    private void appendTrustedEvidenceIfAvailable(String importId, long packRowId, long compatibilityResultId,
                                                  EditorialPackManifest manifest, CompatibilityDecision decision,
                                                  String blockerDetails, long evaluatedAt) {
        EditorialEngineContractProfile profile = decision.trustedProfile;
        if (profile == null) return;
        EditorialCompatibilityEvaluationContext context = new EditorialCompatibilityEvaluationContext(
                manifest.canonicalPackHash(), profile.engineProfileId(), profile.engineProfileVersion(),
                profile.canonicalProfileHash(), profile.engineVersion(), profile.machineContractFingerprint(),
                EditorialCompatibilityEvaluator.EVALUATOR_CONTRACT_VERSION,
                EditorialCompatibilityProvenance.adapterSetFingerprint(profile),
                EditorialCompatibilityProvenance.capabilityFingerprint(profile));
        EditorialPackCompatibilityEvaluation evaluation = EditorialPackCompatibilityEvaluation.trusted(
                importId + ":compatibility:v1", importId, packRowId, compatibilityResultId, context,
                decision.outcome, decision.reasonCode.name(), blockerDetails, evaluatedAt);
        // INSERT-only and inside the pack transaction: provenance failure rolls back
        // the pack/files/v14 row together with this v15 evidence row.
        evaluationDao.append(evaluation);
    }

    private CompatibilityDecision evaluate(EditorialPackManifest manifest) {
        if (trustedResolver != null) {
            try {
                EditorialCompatibilityEvaluationResult resolved = trustedResolver.resolve(manifest);
                if (resolved == null) return CompatibilityDecision.blocked(
                        EditorialCompatibilityReasonCode.COMPATIBILITY_EVALUATION_FAILURE,
                        "Compatibility resolver returned no result");
                CompatibilityDecision decision = CompatibilityDecision.trusted(resolved);
                if (decision.trustedProfile != null
                        && !EditorialEngineProfileResolver.isExecutableContractProfile(decision.trustedProfile)
                        && decision.outcome == EditorialPackCompatibilityClass.DATA_COMPATIBLE) {
                    return decision.withGate(EditorialCompatibilityReasonCode.PROFILE_NON_EXECUTABLE,
                            "Trusted profile is not executable; ready state is forbidden in B2");
                }
                return decision;
            } catch (RuntimeException error) {
                return CompatibilityDecision.blocked(EditorialCompatibilityReasonCode.COMPATIBILITY_EVALUATION_FAILURE,
                        "Compatibility evaluation failed closed");
            }
        }
        EditorialCompatibilityResult legacy = compatibilityEvaluator.evaluate(manifest, engineProfile);
        return CompatibilityDecision.legacy(legacy);
    }

    private static String compatibilityReason(CompatibilityDecision decision) {
        StringBuilder out = new StringBuilder(decision.reasonCode.name());
        for (EditorialPackIntegrityResult.Issue issue : decision.issues) {
            out.append("; ").append(issue.code()).append(": ").append(issue.message());
        }
        if (!decision.missingCapabilities.isEmpty()) {
            out.append("; Missing capabilities: ").append(String.join(", ", decision.missingCapabilities));
        }
        return out.toString();
    }

    private String engineVersionUsed(CompatibilityDecision decision) {
        if (decision.trustedProfile != null) return decision.trustedProfile.engineVersion();
        return engineProfile == null ? AppBuildInfo.VERSION_NAME : engineProfile.engineVersion();
    }

    private static String trustedMachineFingerprint(CompatibilityDecision decision) {
        return decision.trustedProfile == null ? "" : decision.trustedProfile.machineContractFingerprint();
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
            ExistingPack existing = existingPack(manifest);
            if (existing != null) {
                updateImport(importId, existing.state, "PACK_ALREADY_EXISTS", manifest.packId(), manifest.version(), hash, true, existing.id);
                return true;
            }
            CompatibilityDecision compatibility = evaluate(manifest);
            String reason = compatibilityReason(compatibility);
            storeFinal(importId, manifest, compatibility, hash, reason);
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

    private PersistedCompatibility latestCompatibility(String hash) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT compatibility_class,required_class,machine_contract_fingerprint,blocked_reason FROM editorial_pack_compatibility_results WHERE canonical_pack_hash=? ORDER BY evaluated_at DESC,id DESC LIMIT 1",
                new String[]{hash})) {
            if (!cursor.moveToFirst()) return null;
            EditorialPackCompatibilityClass outcome = parseCompatibilityClass(cursor.getString(0));
            EditorialPackCompatibilityClass required = parseCompatibilityClass(cursor.getString(1));
            String reason = safe(cursor.getString(3));
            return new PersistedCompatibility(outcome, required, reasonCodeFrom(reason),
                    cursor.getString(2), missingFromReason(reason));
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
                                             EditorialPackManifest manifest, CompatibilityDecision compatibility, String storageKey) {
        return result(importId, state, error, reason, manifest, compatibility, storageKey, false);
    }

    private EditorialPackImportResult result(String importId, EditorialPackImportState state, EditorialPackImportError error, String reason,
                                             EditorialPackManifest manifest, CompatibilityDecision compatibility, String storageKey,
                                             boolean alreadyExisted) {
        return new EditorialPackImportResult(importId, state, error, reason, manifest.packId(), manifest.version(), manifest.canonicalPackHash(),
                compatibility == null ? null : compatibility.outcome, storageKey,
                compatibility == null ? Set.of() : compatibility.missingCapabilities, alreadyExisted);
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

    private static EditorialPackCompatibilityClass parseCompatibilityClass(String value) {
        try { return EditorialPackCompatibilityClass.valueOf(value); }
        catch (RuntimeException error) { return EditorialPackCompatibilityClass.BLOCKED; }
    }

    private static Set<String> missingFromReason(String reason) {
        int start = reason.indexOf("Missing capabilities:");
        if (start < 0) return Set.of();
        String values = reason.substring(start + "Missing capabilities:".length());
        Set<String> result = new java.util.TreeSet<>();
        for (String value : values.split("[;,]")) if (!value.trim().isEmpty()) result.add(value.trim());
        return Set.copyOf(result);
    }

    private static EditorialCompatibilityReasonCode reasonCodeFrom(String reason) {
        if (reason != null) {
            int end = reason.indexOf(':');
            String candidate = (end < 0 ? reason : reason.substring(0, end)).trim();
            try { return EditorialCompatibilityReasonCode.valueOf(candidate); }
            catch (IllegalArgumentException ignored) { }
        }
        return EditorialCompatibilityReasonCode.BLOCKED;
    }

    private static final class CompatibilityDecision {
        final EditorialPackCompatibilityClass outcome;
        final EditorialPackCompatibilityClass requiredClass;
        final boolean blocked;
        final List<EditorialPackIntegrityResult.Issue> issues;
        final Set<String> missingCapabilities;
        final String machineContractFingerprint;
        final EditorialCompatibilityReasonCode reasonCode;
        final EditorialEngineContractProfile trustedProfile;

        CompatibilityDecision(EditorialPackCompatibilityClass outcome, EditorialPackCompatibilityClass requiredClass,
                              boolean blocked, List<EditorialPackIntegrityResult.Issue> issues,
                              Set<String> missingCapabilities, String machineContractFingerprint,
                              EditorialCompatibilityReasonCode reasonCode, EditorialEngineContractProfile trustedProfile) {
            this.outcome = outcome;
            this.requiredClass = requiredClass;
            this.blocked = blocked;
            this.issues = List.copyOf(issues == null ? List.of() : issues);
            this.missingCapabilities = java.util.Collections.unmodifiableSet(new java.util.TreeSet<>(missingCapabilities == null ? Set.of() : missingCapabilities));
            this.machineContractFingerprint = machineContractFingerprint == null ? "" : machineContractFingerprint;
            this.reasonCode = reasonCode == null ? EditorialCompatibilityReasonCode.BLOCKED : reasonCode;
            this.trustedProfile = trustedProfile;
        }

        static CompatibilityDecision trusted(EditorialCompatibilityEvaluationResult result) {
            return new CompatibilityDecision(result.outcome(), result.requiredClass(), result.blocked(), result.issues(),
                    result.missingCapabilities(), result.trustedProfile().map(EditorialEngineContractProfile::machineContractFingerprint).orElse(""),
                    result.reasonCode(), result.trustedProfile().orElse(null));
        }

        static CompatibilityDecision legacy(EditorialCompatibilityResult result) {
            EditorialCompatibilityReasonCode reason = result.missingCapabilities().isEmpty()
                    ? switch (result.classification()) {
                        case ADAPTER_REQUIRED -> EditorialCompatibilityReasonCode.ADAPTER_REQUIRED;
                        case ENGINE_UPGRADE_REQUIRED -> EditorialCompatibilityReasonCode.ENGINE_UPGRADE_REQUIRED;
                        case INVALID -> EditorialCompatibilityReasonCode.INVALID_PACK;
                        case DATA_COMPATIBLE -> EditorialCompatibilityReasonCode.DATA_COMPATIBLE;
                        default -> EditorialCompatibilityReasonCode.BLOCKED;
                    }
                    : EditorialCompatibilityReasonCode.MISSING_ENGINE_CAPABILITY;
            return new CompatibilityDecision(result.classification(), result.requiredClass(), result.blocked(), result.issues(),
                    result.missingCapabilities(), result.machineContractFingerprint(), reason, null);
        }

        static CompatibilityDecision persisted(PersistedCompatibility persisted) {
            return new CompatibilityDecision(persisted.outcome, persisted.requiredClass,
                    persisted.outcome != EditorialPackCompatibilityClass.DATA_COMPATIBLE,
                    List.of(), persisted.missingCapabilities, persisted.machineContractFingerprint,
                    persisted.reasonCode, null);
        }

        static CompatibilityDecision blocked(EditorialCompatibilityReasonCode reason, String message) {
            return new CompatibilityDecision(EditorialPackCompatibilityClass.BLOCKED, EditorialPackCompatibilityClass.BLOCKED,
                    true, List.of(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.UNSUPPORTED_CONTRACT_SCHEMA,
                    "trustedProfile", message)), Set.of(), "", reason, null);
        }

        CompatibilityDecision withGate(EditorialCompatibilityReasonCode reason, String message) {
            return new CompatibilityDecision(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED,
                    EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, true,
                    List.of(new EditorialPackIntegrityResult.Issue(EditorialPackValidationCode.ENGINE_UPGRADE_REQUIRED,
                            "trustedProfile", message)), missingCapabilities, machineContractFingerprint, reason, trustedProfile);
        }
    }

    private record PersistedCompatibility(EditorialPackCompatibilityClass outcome,
                                          EditorialPackCompatibilityClass requiredClass,
                                          EditorialCompatibilityReasonCode reasonCode,
                                          String machineContractFingerprint,
                                          Set<String> missingCapabilities) {}

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
