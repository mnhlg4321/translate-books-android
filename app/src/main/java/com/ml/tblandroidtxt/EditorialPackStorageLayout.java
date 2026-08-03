package com.ml.tblandroidtxt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Private, content-addressed storage boundary for imported packs. */
public final class EditorialPackStorageLayout {
    private static final Pattern IMPORT_ID = Pattern.compile("[0-9a-fA-F-]{36}");
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private final Path root;
    private final Path stagingRoot;
    private final Path immutableRoot;

    public EditorialPackStorageLayout(Path appPrivateFilesRoot) {
        if (appPrivateFilesRoot == null) throw new IllegalArgumentException("Private storage root is required");
        this.root = appPrivateFilesRoot.resolve("editorial-packs").toAbsolutePath().normalize();
        this.stagingRoot = root.resolve("staging");
        this.immutableRoot = root.resolve("immutable");
        if (!stagingRoot.startsWith(root) || !immutableRoot.startsWith(root)) throw new IllegalArgumentException("Invalid storage layout");
    }

    public Path root() { return root; }
    public Path stagingRoot() { return stagingRoot; }
    public Path immutableRoot() { return immutableRoot; }
    public Path stagingDirectory(String importId) { requireImportId(importId); return stagingRoot.resolve(importId); }
    public Path immutableDirectory(String canonicalPackHash) { requireHash(canonicalPackHash); return immutableRoot.resolve(canonicalPackHash); }
    public Path stagingEntry(String importId, String normalizedPath) { return safeChild(stagingDirectory(importId), normalizedPath); }
    public Path immutableEntry(String canonicalPackHash, String normalizedPath) { return safeChild(immutableDirectory(canonicalPackHash), normalizedPath); }
    public Path ownerMarker(String importId) { return stagingDirectory(importId).resolve(".editorial-import-owner"); }
    public Path snapshotMarker(String importId) { return stagingDirectory(importId).resolve(".snapshot-complete"); }
    public Path immutableMarker(String canonicalPackHash) { return immutableDirectory(canonicalPackHash).resolve(".storage-complete"); }

    public void createRoots() throws IOException {
        Files.createDirectories(stagingRoot);
        Files.createDirectories(immutableRoot);
    }

    public void writeOwnerMarker(String importId) throws IOException {
        forceText(ownerMarker(importId), "EDITORIAL_IMPORT_OWNER_V1\n" + importId + "\n");
    }

    public void writeSnapshotMarker(String importId) throws IOException {
        forceText(snapshotMarker(importId), "EDITORIAL_SNAPSHOT_COMPLETE_V1\n" + importId + "\n");
    }

    public void writeImmutableMarker(String canonicalPackHash, String importId) throws IOException {
        forceText(immutableMarker(canonicalPackHash), "EDITORIAL_STORAGE_COMPLETE_V1\n" + canonicalPackHash + "\n" + importId + "\n");
    }

    public void writeStorageMarkerInDirectory(Path directory, String canonicalPackHash, String importId) throws IOException {
        requireHash(canonicalPackHash);
        if (directory == null || !directory.toAbsolutePath().normalize().startsWith(immutableRoot.getParent().toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Storage directory is outside the private pack root");
        }
        forceText(directory.resolve(".storage-complete"), "EDITORIAL_STORAGE_COMPLETE_V1\n" + canonicalPackHash + "\n" + importId + "\n");
    }

    public boolean hasSnapshotMarker(String importId) {
        return Files.isDirectory(stagingDirectory(importId), LinkOption.NOFOLLOW_LINKS)
                && Files.isRegularFile(snapshotMarker(importId), LinkOption.NOFOLLOW_LINKS);
    }
    public boolean hasImmutableMarker(String canonicalPackHash) {
        return Files.isDirectory(immutableDirectory(canonicalPackHash), LinkOption.NOFOLLOW_LINKS)
                && Files.isRegularFile(immutableMarker(canonicalPackHash), LinkOption.NOFOLLOW_LINKS);
    }

    public static String normalizeEntryPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) throw new IllegalArgumentException("Entry path is blank");
        String normalized = Normalizer.normalize(rawPath, Normalizer.Form.NFC);
        if (normalized.indexOf('\0') >= 0 || normalized.startsWith("/") || normalized.startsWith("\\")
                || normalized.contains("/") || normalized.contains("\\") || normalized.contains(":")
                || normalized.indexOf('\u2215') >= 0 || normalized.indexOf('\u2044') >= 0
                || normalized.indexOf('\uff0f') >= 0 || normalized.indexOf('\uff3c') >= 0
                || normalized.equals(".") || normalized.equals("..") || normalized.contains("..")) {
            throw new IllegalArgumentException("Entry path is not a root-level relative path");
        }
        return normalized;
    }

    public static String normalizedCollisionKey(String rawPath) {
        return normalizeEntryPath(rawPath).toLowerCase(Locale.ROOT);
    }

    public static boolean isImportId(String value) { return value != null && IMPORT_ID.matcher(value).matches(); }
    public static boolean isCanonicalHash(String value) { return value != null && HASH.matcher(value).matches(); }

    public boolean isOwnedStagingDirectory(Path candidate, String importId) {
        if (!isImportId(importId) || candidate == null) return false;
        Path normalized = candidate.toAbsolutePath().normalize();
        return normalized.equals(stagingDirectory(importId))
                && Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)
                && Files.isRegularFile(ownerMarker(importId), LinkOption.NOFOLLOW_LINKS);
    }

    public void deleteOwnedStaging(String importId) throws IOException {
        Path directory = stagingDirectory(importId);
        if (!isOwnedStagingDirectory(directory, importId)) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException e) { throw new StorageDeleteException(e); }
            });
        } catch (StorageDeleteException e) {
            throw e.io;
        }
    }

    public void deleteOwnedStagingDirectory(Path directory, String importId) throws IOException {
        if (!isOwnedStagingDirectory(directory, importId)) return;
        deleteOwnedStaging(importId);
    }

    public void requireHash(String hash) {
        if (!isCanonicalHash(hash)) throw new IllegalArgumentException("Canonical pack hash must be lowercase SHA-256");
    }

    private static void requireImportId(String importId) {
        if (!isImportId(importId)) throw new IllegalArgumentException("Invalid import ID");
    }

    private static Path safeChild(Path parent, String normalizedPath) {
        String safe = normalizeEntryPath(normalizedPath);
        Path child = parent.resolve(safe).normalize();
        if (!child.startsWith(parent.toAbsolutePath().normalize())) throw new IllegalArgumentException("Entry escapes storage root");
        return child;
    }

    private static void forceText(Path path, String text) throws IOException {
        Files.write(path, text.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try (var channel = java.nio.channels.FileChannel.open(path, StandardOpenOption.WRITE)) { channel.force(true); }
    }

    private static final class StorageDeleteException extends RuntimeException {
        final IOException io;
        StorageDeleteException(IOException io) { this.io = io; }
    }
}
