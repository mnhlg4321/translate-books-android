package com.ml.tblandroidtxt;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;

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

/** Android proof that canonical and control transports share identity and differ only at the importer boundary. */
@RunWith(AndroidJUnit4.class)
public class EditorialP2ReferencePackImportInstrumentedTest {
    private static final String CANONICAL_ASSET = "editorial-p2/v5-safe-4.1.3-full-canonical.zip";
    private static final String CONTROL_ASSET = "editorial-p2/v5-safe-4.1.3-full-java-control.zip";
    private static final String CANONICAL_ZIP_SHA256 = "b9c65dbeb9d4c4ed46b67d5ec28ff6252cc2bdc4b63bc902904612987ec58987";
    private static final String CONTROL_ZIP_SHA256 = "44f99423292ada15680220165af50430532d847e155f93c1b15d9f173d4609a5";
    private static final List<String> ENTRY_NAMES = List.of(
            "editorial-pack.json", "project.txt", "prompt.txt", "workflow.txt");

    private Context context;
    private String databaseName;
    private TranslationRepository repository;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-p2-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void canonicalAndJavaControlShareIdentityButHaveDifferentTransport() throws Exception {
        PackFixture canonical = fixture(CANONICAL_ASSET);
        PackFixture control = fixture(CONTROL_ASSET);

        assertEquals(ENTRY_NAMES, canonical.entryNames());
        assertEquals(ENTRY_NAMES, control.entryNames());
        assertEquals(CANONICAL_ZIP_SHA256, EditorialCanonicalJson.sha256Hex(canonical.zipBytes()));
        assertEquals(CONTROL_ZIP_SHA256, EditorialCanonicalJson.sha256Hex(control.zipBytes()));
        assertNotEquals(EditorialCanonicalJson.sha256Hex(canonical.zipBytes()),
                EditorialCanonicalJson.sha256Hex(control.zipBytes()));
        assertFalse(containsBytes(canonical.zipBytes(), new byte[]{0x50, 0x4b, 0x07, 0x08}));
        assertTrue(containsBytes(control.zipBytes(), new byte[]{0x50, 0x4b, 0x07, 0x08}));

        assertArrayEquals(canonical.manifestBytes(), control.manifestBytes());
        assertEquals(canonical.manifest().canonicalPackHash(), control.manifest().canonicalPackHash());
        assertEquals(canonical.manifest().canonicalPackHash(), canonical.manifest().calculatedCanonicalPackHash());
        assertEquals(canonical.manifest().machineContractFingerprint(), control.manifest().machineContractFingerprint());
        for (String name : List.of("project.txt", "prompt.txt", "workflow.txt")) {
            assertArrayEquals(canonical.dataFiles().get(name), control.dataFiles().get(name));
        }
    }

    @Test public void canonicalImportRetainsKnownGapWhileJavaControlPassesAndIsIdempotent() throws Exception {
        PackFixture canonical = fixture(CANONICAL_ASSET);
        EditorialPackStorageLayout canonicalStorage = new EditorialPackStorageLayout(
                context.getCacheDir().toPath().resolve("editorial-p2-canonical-" + UUID.randomUUID()));
        EditorialPackImportResult canonicalResult = new EditorialPackImportService(
                repository, canonicalStorage, profile(canonical.manifest()))
                .importZip(new ByteArrayInputStream(canonical.zipBytes()));

        assertEquals(EditorialPackImportState.STAGING, canonicalResult.state());
        assertEquals(EditorialPackImportError.TRUNCATED_STREAM, canonicalResult.error());
        assertTrue(canonicalResult.blockedReason().contains("central directory/end record is incomplete"));
        assertEquals(0, countRows("editorial_packs"));
        assertEquals(0, countRows("editorial_pack_imports"));
        assertEquals(0, countDirectories(canonicalStorage.stagingRoot()));

        PackFixture control = fixture(CONTROL_ASSET);
        EditorialPackStorageLayout controlStorage = new EditorialPackStorageLayout(
                context.getCacheDir().toPath().resolve("editorial-p2-control-" + UUID.randomUUID()));
        EditorialPackImportService controlService = new EditorialPackImportService(
                repository, controlStorage, profile(control.manifest()));
        EditorialPackImportResult first = controlService.importZip(new ByteArrayInputStream(control.zipBytes()));

        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, first.state());
        assertEquals(EditorialPackImportError.NONE, first.error());
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, first.compatibilityClass());
        assertTrue(first.readyForCertification());
        for (Map.Entry<String, byte[]> entry : control.dataFiles().entrySet()) {
            assertArrayEquals(entry.getValue(), Files.readAllBytes(
                    controlStorage.immutableEntry(first.canonicalPackHash(), entry.getKey())));
        }
        assertTrue(controlStorage.hasImmutableMarker(first.canonicalPackHash()));

        EditorialPackImportResult second = controlService.importZip(new ByteArrayInputStream(control.zipBytes()));
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, second.state());
        assertTrue(second.alreadyExisted());
        assertEquals(1, countRows("editorial_packs"));
        assertEquals(0, countRowsWhere("editorial_packs", "state='CERTIFIED'"));
    }

    @Test public void independentNegativeFixturesRemainTypedAndFailClosed() throws Exception {
        PackFixture base = fixture(CONTROL_ASSET);

        Map<String, byte[]> driftData = copyData(base.dataFiles());
        driftData.get("prompt.txt")[0] = (byte) (driftData.get("prompt.txt")[0] ^ 1);
        assertError("one-byte authority drift", importZip(base, rezip(base.manifestBytes(), driftData)),
                EditorialPackImportError.INTEGRITY_INVALID);

        PackFixture wrongLength = withManifestMutation(base, root -> {
            for (Object value : list(root.get("fileRoles"))) {
                Map<String, Object> file = map(value);
                if ("prompt.txt".equals(file.get("path"))) {
                    file.put("byteLength", BigDecimal.valueOf(((Number) file.get("byteLength")).longValue() + 1));
                }
            }
        }, true);
        assertError("wrong byte length", importZip(base, wrongLength.zipBytes()),
                EditorialPackImportError.INTEGRITY_INVALID);

        PackFixture wrongFileHash = withManifestMutation(base, root -> {
            for (Object value : list(root.get("fileRoles"))) {
                Map<String, Object> file = map(value);
                if ("prompt.txt".equals(file.get("path"))) file.put("sha256", "0".repeat(64));
            }
        }, true);
        assertError("wrong per-file hash", importZip(base, wrongFileHash.zipBytes()),
                EditorialPackImportError.INTEGRITY_INVALID);

        PackFixture wrongCanonicalHash = withManifestMutation(base,
                root -> root.put("canonicalPackHash", "0".repeat(64)), false);
        assertError("wrong canonical pack hash", importZip(base, wrongCanonicalHash.zipBytes()),
                EditorialPackImportError.INTEGRITY_INVALID);

        assertError("missing manifest", importZip(base, zip(List.of(
                        payload("project.txt", base.dataFiles().get("project.txt")),
                        payload("prompt.txt", base.dataFiles().get("prompt.txt")),
                        payload("workflow.txt", base.dataFiles().get("workflow.txt"))))),
                EditorialPackImportError.INTEGRITY_INVALID);
        assertError("missing authority entry", importZip(base, zip(List.of(
                        payload("editorial-pack.json", base.manifestBytes()),
                        payload("project.txt", base.dataFiles().get("project.txt")),
                        payload("prompt.txt", base.dataFiles().get("prompt.txt"))))),
                EditorialPackImportError.INTEGRITY_INVALID);

        ArrayList<ZipPayload> extra = new ArrayList<>(payloads(base));
        extra.add(payload("extra.txt", new byte[]{1}));
        assertError("extra entry", importZip(base, zip(extra)), EditorialPackImportError.ENTRY_COUNT_LIMIT);

        assertError("nested path", importZip(base, zip(List.of(
                        payload("editorial-pack.json", base.manifestBytes()),
                        payload("nested/project.txt", base.dataFiles().get("project.txt")),
                        payload("prompt.txt", base.dataFiles().get("prompt.txt")),
                        payload("workflow.txt", base.dataFiles().get("workflow.txt"))))),
                EditorialPackImportError.INVALID_ENTRY_PATH);
        assertError("path traversal", importZip(base, zip(List.of(
                        payload("../editorial-pack.json", base.manifestBytes()),
                        payload("project.txt", base.dataFiles().get("project.txt")),
                        payload("prompt.txt", base.dataFiles().get("prompt.txt")),
                        payload("workflow.txt", base.dataFiles().get("workflow.txt"))))),
                EditorialPackImportError.INVALID_ENTRY_PATH);
        assertError("duplicate normalized path", importZip(base, zip(List.of(
                        payload("editorial-pack.json", base.manifestBytes()),
                        payload("project.txt", base.dataFiles().get("project.txt")),
                        payload("prompt.txt", base.dataFiles().get("prompt.txt")),
                        payload("PROMPT.TXT", base.dataFiles().get("prompt.txt"))))),
                EditorialPackImportError.DUPLICATE_NORMALIZED_PATH);

        byte[] duplicateRoleManifest = mutatedManifestBytes(base, root -> {
            List<Object> files = list(root.get("fileRoles"));
            map(files.get(1)).put("role", "PROJECT_INSTRUCTION");
        }, true);
        assertError("duplicate role", importZip(base, zip(payloads(duplicateRoleManifest, base.dataFiles()))),
                EditorialPackImportError.INTEGRITY_INVALID);

        EditorialPackImportService symlinkService = service(base, "symlink");
        EditorialPackImportResult symlink = symlinkService.importPack(List.of(
                new EditorialPackImportEntry("editorial-pack.json",
                        new ByteArrayInputStream(base.manifestBytes()), -1L, true)));
        assertEquals(EditorialPackImportError.SYMLINK_FORBIDDEN, symlink.error());

        byte[] bomManifest = new byte[base.manifestBytes().length + 3];
        bomManifest[0] = (byte) 0xef;
        bomManifest[1] = (byte) 0xbb;
        bomManifest[2] = (byte) 0xbf;
        System.arraycopy(base.manifestBytes(), 0, bomManifest, 3, base.manifestBytes().length);
        assertError("BOM", importZip(base, rezip(bomManifest, base.dataFiles())),
                EditorialPackImportError.INTEGRITY_INVALID);

        PackFixture wrongContract = withManifestMutation(base,
                root -> root.put("contractVersion", "contract.not-supported.v1"), true);
        EditorialPackImportResult contractResult = service(base, "wrong-contract")
                .importPack(entries(wrongContract));
        assertEquals(EditorialPackImportState.STORED_BLOCKED, contractResult.state());
        assertEquals(EditorialPackImportError.NONE, contractResult.error());
        assertEquals(EditorialPackCompatibilityClass.BLOCKED, contractResult.compatibilityClass());

        PackFixture unknownCapability = withManifestMutation(base, root -> {
            list(root.get("requiredCapabilities")).add("future.required.capability.v1");
        }, true);
        EditorialPackImportResult upgrade = new EditorialPackImportService(
                repository,
                new EditorialPackStorageLayout(context.getCacheDir().toPath().resolve("unknown-cap-" + UUID.randomUUID())),
                new EditorialEngineProfileResolver(BundledEditorialEngineContractProfileRegistry.load()))
                .importPack(entries(unknownCapability));
        assertEquals(EditorialPackImportState.STORED_BLOCKED, upgrade.state());
        assertEquals(EditorialPackImportError.NONE, upgrade.error());
        assertEquals(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, upgrade.compatibilityClass());
        assertTrue(upgrade.missingCapabilities().contains("future.required.capability.v1"));
        assertTrue(upgrade.blockedReason().contains("future.required.capability.v1"));

        byte[] truncated = Arrays.copyOf(base.zipBytes(), base.zipBytes().length - 1);
        assertError("truncated ZIP", importZip(base, truncated), EditorialPackImportError.TRUNCATED_STREAM);

        byte[] oversized = new byte[(int) EditorialPackImportService.MAX_FILE_BYTES + 1];
        assertError("entry size limit", importZip(base, zip(List.of(payload("editorial-pack.json", oversized)))),
                EditorialPackImportError.ENTRY_SIZE_LIMIT);

        byte[] repetitive = new byte[20_000];
        Arrays.fill(repetitive, (byte) 'A');
        assertError("compression ratio limit", importZip(base, zip(List.of(payload("editorial-pack.json", repetitive)))),
                EditorialPackImportError.COMPRESSION_RATIO_LIMIT);
    }

    private EditorialPackImportResult importZip(PackFixture profileFixture, byte[] bytes) {
        return service(profileFixture, "negative").importZip(new ByteArrayInputStream(bytes));
    }

    private static void assertError(String label, EditorialPackImportResult result, EditorialPackImportError expected) {
        assertEquals(label, expected, result.error());
    }

    private EditorialPackImportService service(PackFixture fixture, String suffix) {
        return new EditorialPackImportService(repository,
                new EditorialPackStorageLayout(context.getCacheDir().toPath().resolve(
                        "editorial-p2-" + suffix + "-" + UUID.randomUUID())), profile(fixture.manifest()));
    }

    private static EditorialEngineProfile profile(EditorialPackManifest manifest) {
        return new EditorialEngineProfile("4.17.0", manifest.requiredCapabilities(),
                List.of(new EditorialEngineProfile.ContractSupport(
                        manifest.contractVersion(), manifest.schemaVersion(),
                        manifest.machineContractFingerprint(), Set.of())), List.of());
    }

    private PackFixture fixture(String asset) throws IOException {
        byte[] zipBytes;
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open(asset)) {
            zipBytes = input.readAllBytes();
        }
        byte[] manifestBytes = null;
        LinkedHashMap<String, byte[]> data = new LinkedHashMap<>();
        ArrayList<String> names = new ArrayList<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                names.add(entry.getName());
                byte[] bytes = input.readAllBytes();
                if ("editorial-pack.json".equals(entry.getName())) manifestBytes = bytes;
                else data.put(entry.getName(), bytes);
            }
        }
        if (manifestBytes == null) throw new IOException("P2 fixture has no manifest: " + asset);
        return new PackFixture(zipBytes, manifestBytes, data,
                EditorialPackManifest.parse(manifestBytes), names);
    }

    private static PackFixture withManifestMutation(PackFixture base,
                                                     Consumer<Map<String, Object>> mutation,
                                                     boolean recalculateHash) throws IOException {
        byte[] manifestBytes = mutatedManifestBytes(base, mutation, recalculateHash);
        return rezipFixture(manifestBytes, base.dataFiles());
    }

    private static byte[] mutatedManifestBytes(PackFixture base,
                                                Consumer<Map<String, Object>> mutation,
                                                boolean recalculateHash) {
        Map<String, Object> root = mutableMap(base.manifest().declarations());
        mutation.accept(root);
        if (recalculateHash) {
            root.put("canonicalPackHash", "0".repeat(64));
            root.put("canonicalPackHash", canonicalHashWithout(root));
        }
        return EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
    }

    private static PackFixture rezipFixture(byte[] manifestBytes, Map<String, byte[]> data) throws IOException {
        return new PackFixture(zip(payloads(manifestBytes, data)), manifestBytes.clone(), copyData(data),
                EditorialPackManifest.parse(manifestBytes), ENTRY_NAMES);
    }

    private static byte[] rezip(byte[] manifestBytes, Map<String, byte[]> data) throws IOException {
        return zip(payloads(manifestBytes, data));
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
        return payloads(fixture.manifestBytes(), fixture.dataFiles());
    }

    private static List<ZipPayload> payloads(byte[] manifestBytes, Map<String, byte[]> data) {
        ArrayList<ZipPayload> result = new ArrayList<>();
        result.add(payload("editorial-pack.json", manifestBytes));
        for (Map.Entry<String, byte[]> entry : data.entrySet()) result.add(payload(entry.getKey(), entry.getValue()));
        return result;
    }

    private static ZipPayload payload(String name, byte[] bytes) {
        return new ZipPayload(name, bytes == null ? new byte[0] : bytes.clone());
    }

    private static byte[] zip(List<ZipPayload> payloads) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (ZipPayload payload : payloads) {
                zip.putNextEntry(new ZipEntry(payload.name()));
                zip.write(payload.bytes());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
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

    private static Map<String, byte[]> copyData(Map<String, byte[]> original) {
        LinkedHashMap<String, byte[]> copy = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : original.entrySet()) copy.put(entry.getKey(), entry.getValue().clone());
        return copy;
    }

    private static Map<String, Object> mutableMap(Map<String, Object> original) {
        return map(mutable(original));
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) { return (Map<String, Object>) value; }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) { return (List<Object>) value; }

    private static boolean containsBytes(byte[] bytes, byte[] needle) {
        for (int start = 0; start <= bytes.length - needle.length; start++) {
            boolean matches = true;
            for (int offset = 0; offset < needle.length; offset++) {
                if (bytes[start + offset] != needle[offset]) { matches = false; break; }
            }
            if (matches) return true;
        }
        return false;
    }

    private static int countDirectories(Path root) throws IOException {
        if (!Files.isDirectory(root)) return 0;
        try (java.util.stream.Stream<Path> paths = Files.list(root)) {
            return (int) paths.filter(Files::isDirectory).count();
        }
    }

    private int countRows(String table) {
        try (android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private int countRowsWhere(String table, String where) {
        try (android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + table + " WHERE " + where, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private record ZipPayload(String name, byte[] bytes) { }

    private record PackFixture(byte[] zipBytes, byte[] manifestBytes, Map<String, byte[]> dataFiles,
                               EditorialPackManifest manifest, List<String> entryNames) {
        PackFixture {
            zipBytes = zipBytes.clone();
            manifestBytes = manifestBytes.clone();
            dataFiles = java.util.Collections.unmodifiableMap(copyData(dataFiles));
            entryNames = List.copyOf(entryNames);
        }
    }
}
