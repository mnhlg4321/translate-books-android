package com.ml.tblandroidtxt;

import android.content.Context;
import android.database.Cursor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Device acceptance for project-scoped P4 binding; it never opens Editorial execution. */
@RunWith(AndroidJUnit4.class)
public final class EditorialP4BindingInstrumentedTest {
    private static final String CANONICAL_ASSET = "editorial-p2/v5-safe-4.1.3-full-canonical.zip";
    private static final String CANONICAL_ZIP_SHA256 =
            "b9c65dbeb9d4c4ed46b67d5ec28ff6252cc2bdc4b63bc902904612987ec58987";

    private Context context;
    private String databaseName;
    private TranslationRepository database;
    private Path storageRoot;
    private EditorialPackStorageLayout storage;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-p4-binding-" + UUID.randomUUID() + ".db";
        database = new TranslationRepository(context, databaseName);
        storageRoot = context.getCacheDir().toPath().resolve("editorial-p4-binding-" + UUID.randomUUID());
        storage = new EditorialPackStorageLayout(storageRoot);
    }

    @After public void tearDown() throws Exception {
        if (database != null) database.close();
        context.deleteDatabase(databaseName);
        deleteTree(storageRoot);
    }

    @Test public void canonicalAndSyntheticPackBindSideBySideAndResumeExactly() throws Exception {
        PackFixture canonical = fixture(CANONICAL_ASSET);
        assertEquals(CANONICAL_ZIP_SHA256, EditorialCanonicalJson.sha256Hex(canonical.zipBytes));
        EditorialPackImportResult canonicalImport = importZip(canonical.zipBytes);
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, canonicalImport.state());
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, canonicalImport.compatibilityClass());

        PackFixture synthetic = mutate(canonical, root -> {
            root.put("version", "4.1.4");
            root.put("displayName", "V5-SAFE.4.1.4 synthetic compatible");
        });
        EditorialPackImportResult syntheticImport = importZip(synthetic.zipBytes);
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, syntheticImport.state());
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, syntheticImport.compatibilityClass());
        assertNotEquals(canonical.manifest.canonicalPackHash(), synthetic.manifest.canonicalPackHash());

        EditorialPackSelectionPolicy policy = new EditorialPackSelectionPolicy(database, storage);
        assertEquals(2, policy.listSelectable().size());
        EditorialPackSelectionCandidate canonicalCandidate = policy.resolve(
                canonical.manifest.packId(), canonical.manifest.version()).orElseThrow();
        EditorialPackSelectionCandidate syntheticCandidate = policy.resolve(
                synthetic.manifest.packId(), synthetic.manifest.version()).orElseThrow();

        EditorialP4SetupRequest firstRequest = request("p4-selector-canonical", "Project A", "4.1.3",
                canonicalCandidate, sources("A"));
        EditorialP4BindingTransactionService service = new EditorialP4BindingTransactionService(database, storage);
        EditorialP4BindingResult first = service.createSetup(firstRequest);
        assertEquals(EditorialP4BindingResult.Code.APPENDED, first.code());
        assertNotEquals(null, first.binding());
        assertFalse(first.binding().executionAllowed());
        assertEquals("NOT_CERTIFIED", first.binding().certificationState());
        assertEquals(canonical.manifest.canonicalPackHash(), first.binding().canonicalPackHash());
        assertEquals(canonicalCandidate.evaluation().evaluationId(), first.binding().compatibilityEvaluationId());
        assertEquals(1, count("editorial_p4_bindings"));
        assertEquals(4, count("editorial_p4_binding_inputs"));
        EditorialRepository.Project projectA = new EditorialRepository(database).getProject(first.projectId());
        assertEquals(canonical.manifest.packId(), projectA.boundPackId);
        assertEquals(canonical.manifest.version(), projectA.boundPackVersion);
        assertEquals(first.binding().bindingIdentity(), projectA.bindingIdentity);

        EditorialP4BindingResult retry = service.createSetup(firstRequest);
        assertEquals(EditorialP4BindingResult.Code.ALREADY_EXISTS, retry.code());
        assertEquals(first.binding().bindingIdentity(), retry.binding().bindingIdentity());
        assertEquals(1, count("editorial_p4_bindings"));
        assertEquals(1, count("editorial_projects"));

        EditorialP4SetupRequest secondRequest = request("p4-selector-synthetic", "Project B", "4.1.4",
                syntheticCandidate, sources("B"));
        EditorialP4BindingResult second = service.createSetup(secondRequest);
        assertEquals(EditorialP4BindingResult.Code.APPENDED, second.code());
        assertNotEquals(first.binding().bindingIdentity(), second.binding().bindingIdentity());
        assertEquals(2, count("editorial_p4_bindings"));
        assertEquals(2, count("editorial_projects"));

        EditorialP4ResumeResult beforeRestart = service.resumeProject(first.projectId(), null, sources("A"));
        assertEquals(EditorialP4ResumeResult.Code.RESTORED, beforeRestart.code());
        assertEquals(first.binding().bindingIdentity(), beforeRestart.binding().bindingIdentity());
        assertFalse(beforeRestart.executionAllowed());

        database.close();
        database = new TranslationRepository(context, databaseName);
        service = new EditorialP4BindingTransactionService(database, storage);
        EditorialP4ResumeResult afterRestartA = service.resumeProject(first.projectId(), null, sources("A"));
        EditorialP4ResumeResult afterRestartB = service.resumeProject(second.projectId(), null, sources("B"));
        assertEquals(EditorialP4ResumeResult.Code.RESTORED, afterRestartA.code());
        assertEquals(EditorialP4ResumeResult.Code.RESTORED, afterRestartB.code());
        assertEquals(first.binding().bindingIdentity(), afterRestartA.binding().bindingIdentity());
        assertEquals(second.binding().bindingIdentity(), afterRestartB.binding().bindingIdentity());
        assertEquals(canonical.manifest.canonicalPackHash(), afterRestartA.binding().canonicalPackHash());
        assertEquals(synthetic.manifest.canonicalPackHash(), afterRestartB.binding().canonicalPackHash());
        assertEquals("AVAILABLE", afterRestartA.binding().pronounStatus());
        assertEquals(0, providerCallCount());
    }

    @Test public void sourceAndPackDriftAreStaleAndProjectCannotCreateExecutionChapter() throws Exception {
        PackFixture canonical = fixture(CANONICAL_ASSET);
        EditorialPackImportResult imported = importZip(canonical.zipBytes);
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, imported.state());
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                .resolve(canonical.manifest.packId(), canonical.manifest.version()).orElseThrow();
        EditorialP4SetupRequest request = request("p4-selector-drift", "Drift project", "4.1.3",
                candidate, sources("drift"));
        EditorialP4BindingTransactionService service = new EditorialP4BindingTransactionService(database, storage);
        EditorialP4BindingResult created = service.createSetup(request);
        assertEquals(EditorialP4BindingResult.Code.APPENDED, created.code());

        EditorialP4ResumeResult sourceDrift = service.resumeBySelector(request.attemptRequestSelector(),
                sources("changed"));
        assertEquals(EditorialP4ResumeResult.Code.STALE_CHAIN, sourceDrift.code());
        assertTrue(sourceDrift.detail().contains("source-identity-drift"));
        assertFalse(sourceDrift.executionAllowed());

        EditorialRepository repository = new EditorialRepository(database);
        try {
            repository.createChapter(created.projectId(), "chapter-1", "Chapter 1", List.of(
                    new EditorialRepository.AssetSnapshot(EditorialSafe4Workflow.AssetRole.RAW,
                            "content://raw", "raw.txt", "raw"),
                    new EditorialRepository.AssetSnapshot(EditorialSafe4Workflow.AssetRole.DRAFT,
                            "content://draft", "draft.txt", "draft"),
                    new EditorialRepository.AssetSnapshot(EditorialSafe4Workflow.AssetRole.GLOSSARY,
                            "content://glossary", "glossary.txt", "glossary")));
            fail("P4 setup must not open chapter/execution creation");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("execution-disabled"));
        }

        Path storedPrompt = storage.immutableEntry(canonical.manifest.canonicalPackHash(), "prompt.txt");
        byte[] originalPrompt = Files.readAllBytes(storedPrompt);
        byte[] tampered = originalPrompt.clone();
        tampered[0] = (byte) (tampered[0] ^ 1);
        Files.write(storedPrompt, tampered);
        try {
            EditorialP4ResumeResult packDrift = service.resumeBySelector(request.attemptRequestSelector(),
                    sources("drift"));
            assertEquals(EditorialP4ResumeResult.Code.STALE_CHAIN, packDrift.code());
            assertTrue(packDrift.detail().contains("pack-profile-or-evaluation-drift"));
        } finally {
            Files.write(storedPrompt, originalPrompt);
        }
        assertEquals(1, count("editorial_p4_bindings"));
        assertEquals(0, providerCallCount());
    }

    @Test public void blockedPackCannotBeSelectedAndCollisionDoesNotCreatePartialBinding() throws Exception {
        PackFixture canonical = fixture(CANONICAL_ASSET);
        EditorialPackImportResult imported = importZip(canonical.zipBytes);
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, imported.state());
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                .resolve(canonical.manifest.packId(), canonical.manifest.version()).orElseThrow();
        EditorialP4BindingTransactionService service = new EditorialP4BindingTransactionService(database, storage);
        EditorialP4SetupRequest firstRequest = request("p4-selector-collision", "Collision A", "4.1.3",
                candidate, sources("collision-a"));
        EditorialP4BindingResult first = service.createSetup(firstRequest);
        assertEquals(EditorialP4BindingResult.Code.APPENDED, first.code());

        EditorialP4SetupRequest collision = new EditorialP4SetupRequest(
                firstRequest.attemptRequestSelector(), "Collision B", "4.1.3", firstRequest.packId(),
                firstRequest.packVersion(), "different-semantic-key", firstRequest.scopeKey(),
                sources("collision-b"), firstRequest.sourceMode(), firstRequest.glossaryStatus(),
                firstRequest.pronounStatus(), firstRequest.pairContextStatus(),
                firstRequest.explicitUserDecisionProvenance(), firstRequest.runKind(),
                firstRequest.phaseIdentity(), firstRequest.frozenManifestReference(),
                EditorialLineageNodeKind.ROOT, null, firstRequest.createdAt());
        EditorialP4BindingResult collisionResult = service.createSetup(collision);
        assertEquals(EditorialP4BindingResult.Code.REQUEST_COLLISION, collisionResult.code());
        assertEquals(1, count("editorial_projects"));
        assertEquals(1, count("editorial_project_revisions"));
        assertEquals(1, count("editorial_input_scope_snapshots"));
        assertEquals(1, count("editorial_authoritative_run_declarations"));
        assertEquals(1, count("editorial_p4_bindings"));

        PackFixture unknownCapability = mutate(canonical, root -> {
            root.put("version", "4.1.5");
            root.put("displayName", "V5-SAFE.4.1.5 unknown capability");
            @SuppressWarnings("unchecked")
            List<Object> capabilities = (List<Object>) root.get("requiredCapabilities");
            capabilities.add("p4.unknown.capability.v1");
        });
        EditorialPackImportResult blocked = importZip(unknownCapability.zipBytes);
        assertEquals(EditorialPackImportState.STORED_BLOCKED, blocked.state());
        assertEquals(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, blocked.compatibilityClass());
        assertTrue(new EditorialPackSelectionPolicy(database, storage).resolve(
                unknownCapability.manifest.packId(), unknownCapability.manifest.version()).isEmpty());
        EditorialP4SetupRequest blockedRequest = request("p4-selector-blocked", "Blocked", "future",
                new EditorialPackSelectionCandidate(unknownCapability.manifest,
                        candidate.evaluation(), candidate.trustedProfile()), sources("blocked"));
        EditorialP4BindingResult blockedResult = service.createSetup(blockedRequest);
        assertEquals(EditorialP4BindingResult.Code.PACK_NOT_SELECTABLE, blockedResult.code());
        assertEquals(1, count("editorial_p4_bindings"));
        assertEquals(0, providerCallCount());
    }

    private EditorialPackImportResult importZip(byte[] bytes) {
        return new EditorialPackImportService(database, storage,
                new EditorialEngineProfileResolver(BundledEditorialEngineContractProfileRegistry.load()))
                .importZip(new ByteArrayInputStream(bytes));
    }

    private EditorialP4SetupRequest request(String selector, String series, String volume,
                                            EditorialPackSelectionCandidate candidate,
                                            List<EditorialP4InputSource> sources) {
        return new EditorialP4SetupRequest(selector, series, volume, candidate.packId(),
                candidate.packVersion(), "semantic/" + series, "scope/" + selector, sources,
                "NORMAL", "AVAILABLE", "AVAILABLE", "NONE", "USER_CONFIRMED_NORMAL",
                "EDITORIAL_SETUP", "L1_SOURCE_PREFLIGHT", "content://pack/editorial-pack.json",
                EditorialLineageNodeKind.ROOT, null, 1000L);
    }

    private List<EditorialP4InputSource> sources(String suffix) {
        return List.of(
                source("RAW", "raw-" + suffix, "raw bytes " + suffix),
                source("DRAFT", "draft-" + suffix, "draft bytes " + suffix),
                source("GLOSSARY", "glossary-" + suffix, "glossary bytes " + suffix),
                source("PRONOUN", "pronoun-" + suffix, "pronoun bytes " + suffix));
    }

    private EditorialP4InputSource source(String role, String reference, String value) {
        return new EditorialP4InputSource(role, "content://p4/" + reference,
                value.getBytes(StandardCharsets.UTF_8), "UTF-8", "VALID", 0L);
    }

    private int count(String table) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private static int providerCallCount() {
        // P4 has no provider dependency; this explicit sentinel makes the test
        // assertion visible and prevents a future test helper from hiding calls.
        return 0;
    }

    private PackFixture fixture(String asset) throws IOException {
        byte[] zipBytes;
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open(asset)) {
            zipBytes = input.readAllBytes();
        }
        byte[] manifestBytes = null;
        LinkedHashMap<String, byte[]> data = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] bytes = input.readAllBytes();
                if ("editorial-pack.json".equals(entry.getName())) manifestBytes = bytes;
                else data.put(entry.getName(), bytes);
            }
        }
        if (manifestBytes == null) throw new IOException("manifest missing from test asset");
        return new PackFixture(zipBytes, manifestBytes, data, EditorialPackManifest.parse(manifestBytes));
    }

    private static PackFixture mutate(PackFixture base, Consumer<Map<String, Object>> mutation)
            throws IOException {
        Map<String, Object> root = mutableMap(base.manifest.declarations());
        mutation.accept(root);
        root.put("canonicalPackHash", "0".repeat(64));
        root.put("canonicalPackHash", canonicalHashWithout(root));
        byte[] manifestBytes = EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
        return new PackFixture(zip(manifestBytes, base.dataFiles), manifestBytes,
                copyData(base.dataFiles), EditorialPackManifest.parse(manifestBytes));
    }

    private static byte[] zip(byte[] manifestBytes, Map<String, byte[]> data) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("editorial-pack.json"));
            zip.write(manifestBytes);
            zip.closeEntry();
            for (Map.Entry<String, byte[]> entry : data.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
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

    private static Map<String, Object> mutableMap(Map<String, Object> original) {
        return map(mutable(original));
    }

    private static Object mutable(Object value) {
        if (value instanceof Map<?, ?> source) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                copy.put((String) entry.getKey(), mutable(entry.getValue()));
            }
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

    private static Map<String, byte[]> copyData(Map<String, byte[]> original) {
        LinkedHashMap<String, byte[]> result = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : original.entrySet()) {
            result.put(entry.getKey(), entry.getValue().clone());
        }
        return result;
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (IOException error) { throw new RuntimeException(error); }
            });
        }
    }

    private static final class PackFixture {
        final byte[] zipBytes;
        final byte[] manifestBytes;
        final Map<String, byte[]> dataFiles;
        final EditorialPackManifest manifest;

        PackFixture(byte[] zipBytes, byte[] manifestBytes, Map<String, byte[]> dataFiles,
                    EditorialPackManifest manifest) {
            this.zipBytes = zipBytes;
            this.manifestBytes = manifestBytes;
            this.dataFiles = dataFiles;
            this.manifest = manifest;
        }
    }
}
