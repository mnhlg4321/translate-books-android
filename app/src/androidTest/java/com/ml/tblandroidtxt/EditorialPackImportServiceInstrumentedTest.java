package com.ml.tblandroidtxt;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EditorialPackImportServiceInstrumentedTest {
    private Context context;
    private String databaseName;
    private TranslationRepository repository;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-g2b1-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void freshDatabaseCreatesPackTablesAndImmutableConstraints() {
        SQLiteDatabase db = repository.editorialWritableDatabase();
        assertTable(db, "editorial_packs");
        assertTable(db, "editorial_pack_files");
        assertTable(db, "editorial_pack_imports");
        assertTable(db, "editorial_pack_compatibility_results");
        assertIndex(db, "sqlite_autoindex_editorial_packs_1");
        assertIndex(db, "idx_editorial_pack_import_hash");
        try { db.execSQL("INSERT INTO editorial_packs(pack_id,version,canonical_pack_hash,contract_version,schema_version,minimum_engine_version,compatibility_class,state,storage_key,manifest_canonical_json,created_at,validated_at,engine_version_used) VALUES('x','1','" + "a".repeat(64) + "','c','s','1.0.0','DATA_COMPATIBLE','CERTIFIED','x','{}',1,1,'1.0.0')"); fail("invalid state must be rejected"); }
        catch (SQLiteException expected) { }
    }

    @Test public void upgradeFromVersion13RetainsLegacyRows() throws Exception {
        repository.close();
        Path path = context.getDatabasePath(databaseName).toPath();
        Files.createDirectories(path.getParent());
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path.toFile(), null);
        old.execSQL("CREATE TABLE legacy_keep(id INTEGER PRIMARY KEY, value TEXT NOT NULL)");
        old.execSQL("INSERT INTO legacy_keep(id,value) VALUES(7,'keep')");
        old.setVersion(13);
        old.close();
        repository = new TranslationRepository(context, databaseName);
        try (android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery("SELECT value FROM legacy_keep WHERE id=7", null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals("keep", cursor.getString(0));
        }
        assertTable(repository.editorialReadableDatabase(), "editorial_packs");
    }

    @Test public void validPackIsSnapshottedStoredAndRepeatIsIdempotent() throws Exception {
        EditorialPackAndroidFixture.Fixture fixture = EditorialPackAndroidFixture.valid("g2b1.valid", "1.0.0", "prompt\n".getBytes(StandardCharsets.UTF_8));
        EditorialPackImportService service = service(fixture);
        EditorialPackImportResult first = service.importPack(entries(fixture));
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, first.state());
        assertTrue(first.readyForCertification());
        assertTrue(Files.isRegularFile(service.storageLayout().immutableMarker(fixture.manifest().canonicalPackHash())));
        EditorialPackImportResult second = service.importPack(entries(fixture));
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, second.state());
        assertEquals(first.canonicalPackHash(), second.canonicalPackHash());
        try (android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery("SELECT COUNT(*) FROM editorial_packs", null)) {
            assertTrue(cursor.moveToFirst()); assertEquals(1, cursor.getInt(0));
        }
        SqliteEditorialPackRegistry registry = new SqliteEditorialPackRegistry(repository, service.storageLayout());
        assertTrue(registry.findByHash(first.canonicalPackHash()).isPresent());
    }

    @Test public void sameIdentityWithChangedPromptIsBlockedAndGetsNewHash() {
        EditorialPackAndroidFixture.Fixture firstFixture = EditorialPackAndroidFixture.valid("g2b1.collision", "1.0.0", "prompt-a\n".getBytes(StandardCharsets.UTF_8));
        EditorialPackAndroidFixture.Fixture secondFixture = EditorialPackAndroidFixture.valid("g2b1.collision", "1.0.0", "prompt-b\n".getBytes(StandardCharsets.UTF_8));
        EditorialPackImportService service = service(firstFixture);
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, service.importPack(entries(firstFixture)).state());
        EditorialPackImportResult collision = service.importPack(entries(secondFixture));
        assertEquals(EditorialPackImportState.STORED_BLOCKED, collision.state());
        assertEquals(EditorialPackImportError.IDENTITY_COLLISION, collision.error());
        assertNotEquals(firstFixture.manifest().canonicalPackHash(), secondFixture.manifest().canonicalPackHash());
    }

    @Test public void sourceMutationAfterSnapshotCannotChangeStoredBytes() throws Exception {
        EditorialPackAndroidFixture.Fixture fixture = EditorialPackAndroidFixture.valid("g2b1.snapshot", "1.0.0", "stable\n".getBytes(StandardCharsets.UTF_8));
        List<EditorialPackImportEntry> entries = entries(fixture);
        byte[] source = fixture.dataFiles().get("prompt.txt");
        entries.set(2, new EditorialPackImportEntry("prompt.txt", new MutatingInputStream(source, "changed-after-eof\n".getBytes(StandardCharsets.UTF_8))));
        EditorialPackImportService service = service(fixture);
        EditorialPackImportResult result = service.importPack(entries);
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, result.state());
        assertArrayEquals(source, Files.readAllBytes(service.storageLayout().immutableEntry(result.canonicalPackHash(), "prompt.txt")));
    }

    @Test public void securityAndFilesystemFailuresNeverCreateAvailablePack() throws Exception {
        EditorialPackAndroidFixture.Fixture fixture = EditorialPackAndroidFixture.valid("g2b1.security", "1.0.0", "prompt\n".getBytes(StandardCharsets.UTF_8));
        EditorialPackImportService service = service(fixture);
        EditorialPackImportResult traversal = service.importPack(List.of(new EditorialPackImportEntry("../editorial-pack.json", new ByteArrayInputStream(fixture.manifestBytes()))));
        assertEquals(EditorialPackImportError.INVALID_ENTRY_PATH, traversal.error());
        List<EditorialPackImportEntry> extra = entries(fixture);
        extra.add(new EditorialPackImportEntry("extra.txt", new ByteArrayInputStream(new byte[]{1})));
        assertEquals(EditorialPackImportError.ENTRY_COUNT_LIMIT, service.importPack(extra).error());
        EditorialPackImportResult symlink = service.importPack(List.of(new EditorialPackImportEntry("editorial-pack.json", new ByteArrayInputStream(fixture.manifestBytes()), -1L, true)));
        assertEquals(EditorialPackImportError.SYMLINK_FORBIDDEN, symlink.error());
        List<EditorialPackImportEntry> duplicate = entries(fixture);
        duplicate.set(3, new EditorialPackImportEntry("PROMPT.TXT", new ByteArrayInputStream(fixture.dataFiles().get("workflow.txt"))));
        assertEquals(EditorialPackImportError.DUPLICATE_NORMALIZED_PATH, service.importPack(duplicate).error());
        List<EditorialPackImportEntry> wrongHash = entries(fixture);
        wrongHash.set(2, new EditorialPackImportEntry("prompt.txt", new ByteArrayInputStream("different\n".getBytes(StandardCharsets.UTF_8))));
        assertEquals(EditorialPackImportError.INTEGRITY_INVALID, service.importPack(wrongHash).error());
        List<EditorialPackImportEntry> truncated = entries(fixture);
        truncated.set(2, new EditorialPackImportEntry("prompt.txt", new ByteArrayInputStream(fixture.dataFiles().get("prompt.txt")), 999L, -1L, false));
        assertEquals(EditorialPackImportError.TRUNCATED_STREAM, service.importPack(truncated).error());
        Files.createDirectories(service.storageLayout().immutableRoot().resolve(fixture.manifest().canonicalPackHash()));
        EditorialPackImportResult collision = service.importPack(entries(fixture));
        assertEquals(EditorialPackImportError.STORAGE_COLLISION, collision.error());
        assertFalse(new SqliteEditorialPackRegistry(repository, service.storageLayout()).findByHash(fixture.manifest().canonicalPackHash()).isPresent());
    }

    @Test public void orphanedStagingIsCleanedWithoutCreatingPack() throws Exception {
        EditorialPackAndroidFixture.Fixture fixture = EditorialPackAndroidFixture.valid("g2b1.orphan", "1.0.0", "prompt\n".getBytes(StandardCharsets.UTF_8));
        EditorialPackImportService service = service(fixture);
        EditorialPackImportResult result = service.importPack(withDeclaredLengthMismatch(fixture));
        assertEquals(EditorialPackImportState.STORED_BLOCKED, result.state());
        EditorialPackImportService.EditorialPackRecoveryReport report = service.recoverOrphanedImports();
        assertTrue(report.cleanedStaging() >= 1);
        assertEquals(0, countDirectories(service.storageLayout().stagingRoot()));
        assertEquals(0, countRows(repository, "editorial_packs"));
    }

    @Test public void databaseFailureLeavesNoAvailablePack() {
        EditorialPackAndroidFixture.Fixture fixture = EditorialPackAndroidFixture.valid("g2b1.db-failure", "1.0.0", "prompt\n".getBytes(StandardCharsets.UTF_8));
        EditorialPackImportService service = new EditorialPackImportService(new FailingRepository(context, databaseName + "-fail"),
                new EditorialPackStorageLayout(context.getCacheDir().toPath().resolve(UUID.randomUUID().toString())), profile(fixture));
        EditorialPackImportResult result = service.importPack(entries(fixture));
        assertEquals(EditorialPackImportState.STAGING, result.state());
        assertEquals(EditorialPackImportError.SNAPSHOT_WRITE_FAILED, result.error());
    }

    private EditorialPackImportService service(EditorialPackAndroidFixture.Fixture fixture) {
        return new EditorialPackImportService(repository,
                new EditorialPackStorageLayout(context.getCacheDir().toPath().resolve(UUID.randomUUID().toString())), profile(fixture));
    }

    private EditorialEngineProfile profile(EditorialPackAndroidFixture.Fixture fixture) {
        return new EditorialEngineProfile("1.0.0", Set.of("context.test.v1", "ledger.test.v1"),
                List.of(new EditorialEngineProfile.ContractSupport("contract.test.v1", "evidence.test.v1",
                        fixture.manifest().machineContractFingerprint(), Set.of("context.test.v1", "ledger.test.v1"))), List.of());
    }

    private static List<EditorialPackImportEntry> entries(EditorialPackAndroidFixture.Fixture fixture) {
        ArrayList<EditorialPackImportEntry> result = new ArrayList<>();
        result.add(new EditorialPackImportEntry("editorial-pack.json", new ByteArrayInputStream(fixture.manifestBytes()), fixture.manifestBytes().length, -1L, false));
        for (Map.Entry<String, byte[]> entry : fixture.dataFiles().entrySet()) result.add(new EditorialPackImportEntry(entry.getKey(), new ByteArrayInputStream(entry.getValue()), entry.getValue().length, -1L, false));
        return result;
    }

    private static List<EditorialPackImportEntry> withDeclaredLengthMismatch(EditorialPackAndroidFixture.Fixture fixture) {
        List<EditorialPackImportEntry> result = entries(fixture);
        result.set(2, new EditorialPackImportEntry("prompt.txt", new ByteArrayInputStream(fixture.dataFiles().get("prompt.txt")), 999L, -1L, false));
        return result;
    }

    private static int countDirectories(Path root) throws IOException {
        if (!Files.isDirectory(root)) return 0;
        try (java.util.stream.Stream<Path> paths = Files.list(root)) { return (int) paths.filter(Files::isDirectory).count(); }
    }

    private static int countRows(TranslationRepository repository, String table) {
        try (android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        }
    }

    private static void assertTable(SQLiteDatabase db, String name) {
        try (android.database.Cursor cursor = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[]{name})) { assertTrue(name, cursor.moveToFirst()); }
    }

    private static void assertIndex(SQLiteDatabase db, String name) {
        try (android.database.Cursor cursor = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='index' AND name=?", new String[]{name})) { assertTrue(name, cursor.moveToFirst()); }
    }

    private static final class MutatingInputStream extends InputStream {
        private final byte[] initial;
        private final byte[] replacement;
        private int index;
        private boolean replaced;
        MutatingInputStream(byte[] initial, byte[] replacement) { this.initial = initial.clone(); this.replacement = replacement.clone(); }
        @Override public int read() { if (index < initial.length) return initial[index++]; replaced = true; return -1; }
        @Override public int read(byte[] buffer, int offset, int length) { if (index >= initial.length) { replaced = true; return -1; } int count = Math.min(length, initial.length - index); System.arraycopy(initial, index, buffer, offset, count); index += count; return count; }
    }

    private static final class FailingRepository extends TranslationRepository {
        FailingRepository(Context context, String name) { super(context, name); }
        @Override android.database.sqlite.SQLiteDatabase editorialWritableDatabase() { throw new SQLiteException("simulated database failure"); }
        @Override android.database.sqlite.SQLiteDatabase editorialReadableDatabase() { throw new SQLiteException("simulated database failure"); }
    }
}
