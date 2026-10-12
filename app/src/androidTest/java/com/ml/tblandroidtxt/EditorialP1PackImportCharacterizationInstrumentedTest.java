package com.ml.tblandroidtxt;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluator;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackIntegrityResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackIntegrityValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistryMetadata;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * P1 characterization against the real test-only 4.1.3 ZIP asset. It proves
 * the existing importer/storage boundary and records current compatibility
 * behavior without changing production code.
 */
@RunWith(AndroidJUnit4.class)
public class EditorialP1PackImportCharacterizationInstrumentedTest {
    private static final String FIXTURE_ASSET = "editorial-p1/v5-safe-4.1.3-full.zip";
    private Context context;
    private String databaseName;
    private TranslationRepository repository;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-p1-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void fixtureHasExactlyFourRootEntriesAndExactAuthorityBytes() throws Exception {
        PackFixture fixture = fixture();
        assertEquals(Set.of("editorial-pack.json", "project.txt", "prompt.txt", "workflow.txt"),
                new HashSet<>(fixture.entryNames()));
        assertEquals(4, fixture.entryNames().size());
        assertFalse(fixture.entryNames().stream().anyMatch(name -> name.contains("/") || name.contains("\\")));
        assertEquals(fixture.manifest().canonicalJson(), new String(fixture.manifestBytes(), StandardCharsets.UTF_8));
        assertEquals(fixture.manifest().canonicalPackHash(), fixture.manifest().calculatedCanonicalPackHash());

        assertAuthority(fixture, EditorialPackFileRole.PROJECT_INSTRUCTION, "project.txt", 9485,
                "1727ae173f2cfd530eb818cae69e0d3fadc59c35e3b5b6d478704a02091a26ad");
        assertAuthority(fixture, EditorialPackFileRole.TURN_PROMPT, "prompt.txt", 8852,
                "d25757d1a6bddd5962a3b178b9ef850727573ae0c34867ec8f4b8450c7cd754f");
        assertAuthority(fixture, EditorialPackFileRole.WORKFLOW, "workflow.txt", 34917,
                "5db6b4f6509313f106499113537d2880bc6d2ff663859239dafb285557505730");
    }

    @Test public void valid413ImportIsIdempotentAndImmutableReadbackIsExact() throws Exception {
        PackFixture fixture = fixture();
        EditorialPackImportService service = service(fixture);

        CountingInputStream source = new CountingInputStream(fixture.zipBytes());
        EditorialPackImportResult first = service.importZip(source);
        assertTrue("canonical ZIP must contain an EOCD record",
                findEndOfCentralDirectory(fixture.zipBytes()) >= 0);
        assertEquals("canonical ZIP must be drained before EOCD validation",
                fixture.zipBytes().length, source.bytesRead());
        assertEquals("error=" + first.error() + " reason=" + first.blockedReason(),
                EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, first.state());
        assertEquals(EditorialPackImportError.NONE, first.error());
        assertEquals("error=" + first.error() + " reason=" + first.blockedReason(),
                EditorialPackCompatibilityClass.DATA_COMPATIBLE, first.compatibilityClass());
        assertTrue(first.readyForCertification());

        for (Map.Entry<String, byte[]> file : fixture.dataFiles().entrySet()) {
            assertArrayEquals(file.getValue(), Files.readAllBytes(
                    service.storageLayout().immutableEntry(first.canonicalPackHash(), file.getKey())));
        }
        assertTrue(service.storageLayout().hasImmutableMarker(first.canonicalPackHash()));

        EditorialPackImportResult second = importAsset(service);
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, second.state());
        assertTrue(second.alreadyExisted());
        assertEquals(first.canonicalPackHash(), second.canonicalPackHash());
        assertEquals(1, countRows("editorial_packs"));

        EditorialPackRegistryMetadata metadata = new SqliteEditorialPackRegistry(repository, service.storageLayout())
                .findByHash(first.canonicalPackHash()).orElseThrow().registryMetadata().orElseThrow();
        assertEquals(EditorialPackRegistryMetadata.IntegrityState.VALID, metadata.integrityState());
    }

    @Test public void synthetic414IsSideBySideAndDoesNotRebindOrActivateCurrentProject() throws Exception {
        PackFixture base = fixture();
        PackFixture future = synthetic414(base);
        EditorialPackImportService service = service(base);

        EditorialRepository projects = new EditorialRepository(repository);
        EditorialRepository.Project project = new EditorialRepository.Project();
        project.seriesName = "P1 current project";
        project.volumeName = "Pinned before pack update";
        long projectId = projects.createProject(project);
        EditorialRepository.Project before = projects.getProject(projectId);

        EditorialPackImportResult first = importAsset(service);
        EditorialPackImportResult second = service.importZip(new ByteArrayInputStream(future.zipBytes()));

        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, first.compatibilityClass());
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, second.compatibilityClass());
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, second.state());
        assertNotEquals(first.canonicalPackHash(), second.canonicalPackHash());
        assertNotEquals(first.version(), second.version());
        assertEquals(2, new SqliteEditorialPackRegistry(repository, service.storageLayout()).list().size());
        assertEquals(0, countRowsWhere("editorial_packs", "state='CERTIFIED'"));
        assertEquals(0, countRows("editorial_runs"));

        EditorialRepository.Project after = projects.getProject(projectId);
        assertEquals(before.workflowVersion, after.workflowVersion);
        assertEquals(before.workflowHash, after.workflowHash);
        assertEquals(before.seriesName, after.seriesName);
        assertEquals(before.volumeName, after.volumeName);
    }

    @Test public void unknownRequiredCapabilityIsRejectedAsEngineUpgradeWithCapabilityEvidence() throws Exception {
        PackFixture unknown = withManifestMutation(fixture(), root -> {
            @SuppressWarnings("unchecked")
            List<Object> capabilities = (List<Object>) root.get("requiredCapabilities");
            capabilities.add("future.required.capability.v1");
        }, null);
        EditorialPackStorageLayout storage = new EditorialPackStorageLayout(
                context.getCacheDir().toPath().resolve("editorial-p1-unknown-" + UUID.randomUUID()));
        EditorialPackImportService service = new EditorialPackImportService(repository, storage,
                new EditorialEngineProfileResolver(BundledEditorialEngineContractProfileRegistry.load()));

        EditorialPackImportResult result = service.importZip(new ByteArrayInputStream(unknown.zipBytes()));

        assertEquals(EditorialPackImportError.NONE, result.error());
        assertEquals(EditorialPackImportState.STORED_BLOCKED, result.state());
        assertEquals(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, result.compatibilityClass());
        assertFalse(result.readyForCertification());
        assertTrue(result.missingCapabilities().contains("future.required.capability.v1"));
        assertTrue(result.blockedReason().contains("future.required.capability.v1"));
    }

    @Test public void integrityAndZipBoundariesRejectRequiredNegativeFixtures() throws Exception {
        PackFixture fixture = fixture();
        EditorialPackImportService service = service(fixture);

        byte[] missingManifest = zip(List.of(
                payload("project.txt", fixture.dataFiles().get("project.txt")),
                payload("prompt.txt", fixture.dataFiles().get("prompt.txt")),
                payload("workflow.txt", fixture.dataFiles().get("workflow.txt"))));
        assertEquals(EditorialPackImportError.INTEGRITY_INVALID,
                service.importZip(new ByteArrayInputStream(missingManifest)).error());

        ArrayList<ZipPayload> extra = new ArrayList<>(payloads(fixture));
        extra.add(payload("extra.txt", new byte[]{1}));
        assertEquals(EditorialPackImportError.ENTRY_COUNT_LIMIT,
                service.importZip(new ByteArrayInputStream(zip(extra))).error());

        byte[] duplicate = zip(List.of(
                payload("editorial-pack.json", fixture.manifestBytes()),
                payload("project.txt", fixture.dataFiles().get("project.txt")),
                payload("prompt.txt", fixture.dataFiles().get("prompt.txt")),
                payload("PROMPT.TXT", fixture.dataFiles().get("prompt.txt"))));
        assertEquals(EditorialPackImportError.DUPLICATE_NORMALIZED_PATH,
                service.importZip(new ByteArrayInputStream(duplicate)).error());

        byte[] traversal = zip(List.of(
                payload("../editorial-pack.json", fixture.manifestBytes()),
                payload("project.txt", fixture.dataFiles().get("project.txt")),
                payload("prompt.txt", fixture.dataFiles().get("prompt.txt")),
                payload("workflow.txt", fixture.dataFiles().get("workflow.txt"))));
        assertEquals(EditorialPackImportError.INVALID_ENTRY_PATH,
                service.importZip(new ByteArrayInputStream(traversal)).error());

        EditorialPackImportResult symlink = service.importPack(List.of(
                new EditorialPackImportEntry("editorial-pack.json",
                        new ByteArrayInputStream(fixture.manifestBytes()), -1L, true)));
        assertEquals(EditorialPackImportError.SYMLINK_FORBIDDEN, symlink.error());

        Map<String, byte[]> wrong = new LinkedHashMap<>(fixture.dataFiles());
        wrong.put("prompt.txt", "tampered\n".getBytes(StandardCharsets.UTF_8));
        EditorialPackIntegrityResult wrongResult = new EditorialPackIntegrityValidator()
                .validate(fixture.manifestBytes(), wrong);
        assertTrue(wrongResult.issues().stream().anyMatch(issue ->
                issue.code() == com.ml.tblandroidtxt.editorial.pack.EditorialPackValidationCode.FILE_LENGTH_MISMATCH));
        assertTrue(wrongResult.issues().stream().anyMatch(issue ->
                issue.code() == com.ml.tblandroidtxt.editorial.pack.EditorialPackValidationCode.FILE_HASH_MISMATCH));
    }

    @Test public void sizeCompressionAndTruncatedStreamFailuresAreTyped() throws Exception {
        PackFixture fixture = fixture();
        EditorialPackImportService service = service(fixture);

        byte[] oversized = new byte[(int) EditorialPackImportService.MAX_FILE_BYTES + 1];
        assertEquals(EditorialPackImportError.ENTRY_SIZE_LIMIT,
                service.importZip(new ByteArrayInputStream(zip(List.of(payload("editorial-pack.json", oversized))))).error());

        byte[] repetitive = new byte[20_000];
        Arrays.fill(repetitive, (byte) 'A');
        assertEquals(EditorialPackImportError.COMPRESSION_RATIO_LIMIT,
                service.importZip(new ByteArrayInputStream(zip(List.of(payload("editorial-pack.json", repetitive))))).error());

        byte[] validZip = fixture.zipBytes();
        byte[] truncated = Arrays.copyOf(validZip, validZip.length - 1);
        assertEquals(EditorialPackImportError.TRUNCATED_STREAM,
                service.importZip(new ByteArrayInputStream(truncated)).error());
    }

    @Test public void interruptedSnapshotIsRecoverableWithoutPackRow() throws Exception {
        PackFixture fixture = fixture();
        EditorialPackImportService service = service(fixture);
        List<EditorialPackImportEntry> entries = entries(fixture);
        entries.set(2, new EditorialPackImportEntry("prompt.txt",
                new ByteArrayInputStream(fixture.dataFiles().get("prompt.txt")), 999L, -1L, false));

        EditorialPackImportResult result = service.importPack(entries);
        assertEquals(EditorialPackImportError.TRUNCATED_STREAM, result.error());
        EditorialPackImportService.EditorialPackRecoveryReport recovery = service.recoverOrphanedImports();
        assertTrue(recovery.cleanedStaging() >= 1);
        assertEquals(0, countDirectories(service.storageLayout().stagingRoot()));
        assertEquals(0, countRows("editorial_packs"));
    }

    private EditorialPackImportService service(PackFixture fixture) {
        return new EditorialPackImportService(repository,
                new EditorialPackStorageLayout(context.getCacheDir().toPath().resolve("editorial-p1-" + UUID.randomUUID())),
                profile(fixture.manifest()));
    }

    private EditorialPackImportResult importAsset(EditorialPackImportService service) throws IOException {
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open(FIXTURE_ASSET)) {
            return service.importZip(input);
        }
    }

    private static EditorialEngineProfile profile(EditorialPackManifest manifest) {
        return new EditorialEngineProfile("4.17.0", manifest.requiredCapabilities(),
                List.of(new EditorialEngineProfile.ContractSupport(
                        manifest.contractVersion(), manifest.schemaVersion(),
                        manifest.machineContractFingerprint(), Set.of())), List.of());
    }

    private PackFixture fixture() throws IOException {
        byte[] zipBytes;
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open(FIXTURE_ASSET)) {
            zipBytes = input.readAllBytes();
        }
        LinkedHashMap<String, byte[]> data = new LinkedHashMap<>();
        ArrayList<String> names = new ArrayList<>();
        byte[] manifestBytes = null;
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                names.add(entry.getName());
                byte[] bytes = input.readAllBytes();
                if ("editorial-pack.json".equals(entry.getName())) manifestBytes = bytes;
                else data.put(entry.getName(), bytes);
            }
        }
        if (manifestBytes == null) throw new IOException("P1 fixture has no manifest");
        return new PackFixture(zipBytes, manifestBytes, data,
                EditorialPackManifest.parse(manifestBytes), names);
    }

    private static PackFixture synthetic414(PackFixture base) {
        byte[] changedPrompt = "Synthetic 4.1.4 authority bytes\n".getBytes(StandardCharsets.UTF_8);
        return withManifestMutation(base, root -> {
            root.put("packId", "com.ml.tblandroidtxt.editorial.safe4.full.next");
            root.put("version", "4.1.4");
            root.put("displayName", "Biên tập V5-SAFE.4.1.4-FULL (synthetic)");
            root.put("createdAt", "2026-09-03T00:01:00+07:00");
            @SuppressWarnings("unchecked")
            List<Object> files = (List<Object>) root.get("fileRoles");
            for (Object value : files) {
                @SuppressWarnings("unchecked")
                Map<String, Object> file = (Map<String, Object>) value;
                if ("prompt.txt".equals(file.get("path"))) {
                    file.put("byteLength", BigDecimal.valueOf(changedPrompt.length));
                    file.put("sha256", EditorialCanonicalJson.sha256Hex(changedPrompt));
                }
            }
        }, changedPrompt);
    }

    private static PackFixture withManifestMutation(PackFixture base,
                                                     Consumer<Map<String, Object>> mutation,
                                                     byte[] replacementPrompt) {
        Map<String, Object> root = mutableMap(base.manifest().declarations());
        mutation.accept(root);
        root.put("canonicalPackHash", "0".repeat(64));
        root.put("canonicalPackHash", canonicalHashWithout(root));
        byte[] manifestBytes = EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
        LinkedHashMap<String, byte[]> data = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : base.dataFiles().entrySet()) data.put(entry.getKey(), entry.getValue().clone());
        if (replacementPrompt != null) data.put("prompt.txt", replacementPrompt.clone());
        ArrayList<ZipPayload> payloads = new ArrayList<>();
        payloads.add(payload("editorial-pack.json", manifestBytes));
        payloads.add(payload("project.txt", data.get("project.txt")));
        payloads.add(payload("prompt.txt", data.get("prompt.txt")));
        payloads.add(payload("workflow.txt", data.get("workflow.txt")));
        try {
            return new PackFixture(zip(payloads), manifestBytes, data,
                    EditorialPackManifest.parse(manifestBytes),
                    List.of("editorial-pack.json", "project.txt", "prompt.txt", "workflow.txt"));
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }

    private static List<EditorialPackImportEntry> entries(PackFixture fixture) {
        ArrayList<EditorialPackImportEntry> result = new ArrayList<>();
        result.add(new EditorialPackImportEntry("editorial-pack.json",
                new ByteArrayInputStream(fixture.manifestBytes()), fixture.manifestBytes().length, -1L, false));
        for (Map.Entry<String, byte[]> entry : fixture.dataFiles().entrySet()) {
            result.add(new EditorialPackImportEntry(entry.getKey(),
                    new ByteArrayInputStream(entry.getValue()), entry.getValue().length, -1L, false));
        }
        return result;
    }

    private static List<ZipPayload> payloads(PackFixture fixture) {
        ArrayList<ZipPayload> result = new ArrayList<>();
        result.add(payload("editorial-pack.json", fixture.manifestBytes()));
        for (Map.Entry<String, byte[]> entry : fixture.dataFiles().entrySet()) result.add(payload(entry.getKey(), entry.getValue()));
        return result;
    }

    private static ZipPayload payload(String name, byte[] bytes) {
        return new ZipPayload(name, bytes == null ? new byte[0] : bytes.clone());
    }

    private static byte[] zip(List<ZipPayload> payloads) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(bytes)) {
            for (ZipPayload payload : payloads) {
                output.putNextEntry(new ZipEntry(payload.name()));
                output.write(payload.bytes());
                output.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static String canonicalHashWithout(Map<String, Object> root) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(root);
        copy.remove("canonicalPackHash");
        byte[] canonical = EditorialCanonicalJson.canonicalize(copy).getBytes(StandardCharsets.UTF_8);
        byte[] domain = "EDITORIAL_PACK_CANONICAL_HASH_V1\n".getBytes(StandardCharsets.UTF_8);
        byte[] payload = Arrays.copyOf(domain, domain.length + canonical.length);
        System.arraycopy(canonical, 0, payload, domain.length, canonical.length);
        return EditorialCanonicalJson.sha256Hex(payload);
    }

    private static Map<String, Object> mutableMap(Map<String, Object> original) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) mutable(original);
        return result;
    }

    private static Object mutable(Object value) {
        if (value instanceof Map<?, ?> source) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) copy.put((String) entry.getKey(), mutable(entry.getValue()));
            return copy;
        }
        if (value instanceof List<?> source) {
            ArrayList<Object> copy = new ArrayList<>(source.size());
            for (Object item : source) copy.add(mutable(item));
            return copy;
        }
        return value;
    }

    private static void assertAuthority(PackFixture fixture, EditorialPackFileRole role,
                                         String path, long length, String sha256) {
        EditorialPackManifest.FileEntry file = fixture.manifest().fileEntries().stream()
                .filter(entry -> entry.role() == role).findFirst().orElseThrow();
        assertEquals(path, file.path());
        assertEquals(length, fixture.dataFiles().get(path).length);
        assertEquals(length, file.byteLength());
        assertEquals(sha256, file.sha256());
        assertEquals(sha256, EditorialCanonicalJson.sha256Hex(fixture.dataFiles().get(path)));
        byte[] bytes = fixture.dataFiles().get(path);
        assertFalse(bytes.length >= 3 && (bytes[0] & 0xff) == 0xef
                && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf);
    }

    private int countRows(String table) {
        try (android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + table, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        }
    }

    private int countRowsWhere(String table, String where) {
        try (android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + table + " WHERE " + where, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        }
    }

    private static int countDirectories(Path root) throws IOException {
        if (!Files.isDirectory(root)) return 0;
        try (java.util.stream.Stream<Path> paths = Files.list(root)) {
            return (int) paths.filter(Files::isDirectory).count();
        }
    }

    private static int findEndOfCentralDirectory(byte[] bytes) {
        for (int i = 0; i + 3 < bytes.length; i++) {
            if ((bytes[i] & 0xff) == 0x50 && (bytes[i + 1] & 0xff) == 0x4b
                    && (bytes[i + 2] & 0xff) == 0x05 && (bytes[i + 3] & 0xff) == 0x06) return i;
        }
        return -1;
    }

    private record ZipPayload(String name, byte[] bytes) { }
    private record PackFixture(byte[] zipBytes, byte[] manifestBytes, Map<String, byte[]> dataFiles,
                               EditorialPackManifest manifest, List<String> entryNames) {
        PackFixture {
            zipBytes = zipBytes.clone();
            manifestBytes = manifestBytes.clone();
            LinkedHashMap<String, byte[]> copy = new LinkedHashMap<>();
            for (Map.Entry<String, byte[]> entry : dataFiles.entrySet()) copy.put(entry.getKey(), entry.getValue().clone());
            dataFiles = java.util.Collections.unmodifiableMap(copy);
            entryNames = List.copyOf(entryNames);
        }
    }

    private static final class CountingInputStream extends ByteArrayInputStream {
        private int bytesRead;

        CountingInputStream(byte[] bytes) { super(bytes); }

        @Override public int read() {
            int value = super.read();
            if (value >= 0) bytesRead++;
            return value;
        }

        @Override public int read(byte[] buffer, int offset, int length) {
            int count = super.read(buffer, offset, length);
            if (count > 0) bytesRead += count;
            return count;
        }

        int bytesRead() { return bytesRead; }
    }
}
