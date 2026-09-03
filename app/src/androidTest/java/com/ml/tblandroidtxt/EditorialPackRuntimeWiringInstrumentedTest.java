package com.ml.tblandroidtxt;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluationContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluator;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityProvenance;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfileCanonicalizer;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.TrustedEditorialEngineProfileCatalog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EditorialPackRuntimeWiringInstrumentedTest {
    private Context context;
    private String databaseName;
    private TranslationRepository repository;
    private EditorialPackStorageLayout storage;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-g2c-b2-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
        storage = new EditorialPackStorageLayout(context.getCacheDir().toPath().resolve(UUID.randomUUID().toString()));
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void productionProfileIsNonExecutableAndPersistsTrustedProvenance() throws Exception {
        EditorialEngineContractProfile profile = bootstrapOnlyRegistry().list().get(0);
        EditorialPackAndroidFixture.Fixture fixture = EditorialPackAndroidFixture.valid(
                "g2c.b2.nonexec", "1.0.0", "runtime\n".getBytes(StandardCharsets.UTF_8));
        EditorialPackImportService service = productionService();

        EditorialPackImportResult result = service.importPack(entries(fixture));

        assertEquals(EditorialPackImportState.STORED_BLOCKED, result.state());
        assertEquals(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, result.compatibilityClass());
        assertFalse(result.readyForCertification());
        assertEquals(List.of("context.test.v1", "ledger.test.v1"), new ArrayList<>(result.missingCapabilities()));
        assertTrue(result.blockedReason().startsWith("PROFILE_NON_EXECUTABLE"));
        assertEquals(1, countRows("editorial_packs"));
        assertEquals(1, countRows("editorial_pack_compatibility_results"));
        assertEquals(1, countRows("editorial_pack_compatibility_evaluations"));

        EditorialPackCompatibilityEvaluation evidence = new EditorialPackCompatibilityEvaluationDao(repository)
                .listByPackHash(fixture.manifest().canonicalPackHash()).get(0);
        assertEquals(profile.engineProfileId(), evidence.trustedProfileId().orElseThrow());
        assertEquals(profile.engineProfileVersion(), evidence.trustedProfileVersion().orElseThrow());
        assertEquals(profile.canonicalProfileHash(), evidence.canonicalProfileHash().orElseThrow());
        assertEquals(profile.machineContractFingerprint(), evidence.machineContractFingerprint());
        assertEquals("PROFILE_NON_EXECUTABLE", evidence.reasonCode());
        assertEquals(List.of("context.test.v1", "ledger.test.v1"), new ArrayList<>(result.missingCapabilities()));
        EditorialCompatibilityEvaluationContext expectedContext = new EditorialCompatibilityEvaluationContext(
                fixture.manifest().canonicalPackHash(), profile.engineProfileId(), profile.engineProfileVersion(),
                profile.canonicalProfileHash(), profile.engineVersion(), profile.machineContractFingerprint(),
                EditorialCompatibilityEvaluator.EVALUATOR_CONTRACT_VERSION,
                EditorialCompatibilityProvenance.adapterSetFingerprint(profile),
                EditorialCompatibilityProvenance.capabilityFingerprint(profile));
        assertEquals(expectedContext.fingerprint(), evidence.contextFingerprint().orElseThrow());
        assertFalse(EditorialSafe4Pack.executionEnabled());
    }

    @Test public void duplicateImportDoesNotResolveOrAppendEvaluation() {
        EditorialEngineContractProfileRegistry source = bootstrapOnlyRegistry();
        AtomicInteger resolverCalls = new AtomicInteger();
        EditorialEngineProfileResolver resolver = new EditorialEngineProfileResolver(new EditorialEngineContractProfileRegistry() {
            @Override public List<EditorialEngineContractProfile> list() { resolverCalls.incrementAndGet(); return source.list(); }
            @Override public Optional<EditorialEngineContractProfile> findByCanonicalHash(String hash) { return source.findByCanonicalHash(hash); }
            @Override public Optional<EditorialEngineContractProfile> findByIdentity(String id, String version) { return source.findByIdentity(id, version); }
        });
        EditorialPackAndroidFixture.Fixture fixture = EditorialPackAndroidFixture.valid(
                "g2c.b2.duplicate", "1.0.0", "duplicate\n".getBytes(StandardCharsets.UTF_8));
        EditorialPackImportService service = new EditorialPackImportService(repository, storage, resolver);

        EditorialPackImportResult first = service.importPack(entries(fixture));
        EditorialPackImportResult second = service.importPack(entries(fixture));

        assertEquals(EditorialPackImportState.STORED_BLOCKED, first.state());
        assertTrue("second=" + second, second.alreadyExisted());
        assertEquals(EditorialPackImportState.STORED_BLOCKED, second.state());
        assertEquals(1, resolverCalls.get());
        assertEquals(1, countRows("editorial_packs"));
        assertEquals(1, countRows("editorial_pack_compatibility_evaluations"));
    }

    @Test public void v15PersistenceFailureCannotCreatePackOrReadyState() {
        repository.editorialWritableDatabase().execSQL(
                "CREATE TRIGGER test_b2_v15_failure BEFORE INSERT ON editorial_pack_compatibility_evaluations "
                        + "BEGIN SELECT RAISE(ABORT, 'test evidence failure'); END");
        EditorialPackAndroidFixture.Fixture fixture = EditorialPackAndroidFixture.valid(
                "g2c.b2.persistence-failure", "1.0.0", "failure\n".getBytes(StandardCharsets.UTF_8));

        EditorialPackImportResult result = productionService().importPack(entries(fixture));

        assertEquals(EditorialPackImportState.STORED_BLOCKED, result.state());
        assertFalse(result.readyForCertification());
        assertEquals(0, countRows("editorial_packs"));
        assertEquals(0, countRows("editorial_pack_compatibility_results"));
        assertEquals(0, countRows("editorial_pack_compatibility_evaluations"));
    }

    @Test public void restartReadsPersistedEvidenceWithoutReevaluation() {
        EditorialPackAndroidFixture.Fixture fixture = EditorialPackAndroidFixture.valid(
                "g2c.b2.restart", "1.0.0", "restart\n".getBytes(StandardCharsets.UTF_8));
        EditorialPackImportResult result = productionService().importPack(entries(fixture));
        repository.close();
        repository = new TranslationRepository(context, databaseName);

        List<EditorialPackCompatibilityEvaluation> evidence = new EditorialPackCompatibilityEvaluationDao(repository)
                .listByPackHash(fixture.manifest().canonicalPackHash());
        assertEquals(1, evidence.size());
        assertEquals(result.canonicalPackHash(), evidence.get(0).canonicalPackHash());
        assertEquals("PROFILE_NON_EXECUTABLE", evidence.get(0).reasonCode());
        assertEquals(1, countRows("editorial_packs"));
    }

    @Test public void testOnlyUnsupportedContractIsBlockedWithoutFallback() {
        EditorialEngineContractProfile source = bootstrapOnlyRegistry().list().get(0);
        EditorialEngineContractProfile testOnlyProfile = validExecutableProfile(source);
        EditorialPackAndroidFixture.Fixture base = EditorialPackAndroidFixture.valid(
                "g2c.b2.unsupported", "1.0.0", "unsupported\n".getBytes(StandardCharsets.UTF_8));
        LinkedHashMap<String, Object> declarations = new LinkedHashMap<>(base.manifest().declarations());
        declarations.put("contractVersion", "contract.not-supported.v1");
        declarations.put("canonicalPackHash", "0".repeat(64));
        declarations.put("canonicalPackHash", canonicalHashWithout(declarations));
        byte[] manifestBytes = EditorialCanonicalJson.canonicalize(declarations).getBytes(StandardCharsets.UTF_8);
        EditorialPackAndroidFixture.Fixture unsupported = new EditorialPackAndroidFixture.Fixture(
                manifestBytes, base.dataFiles(), EditorialPackManifest.parse(manifestBytes));
        EditorialEngineContractProfileRegistry testOnlyRegistry = new EditorialEngineContractProfileRegistry() {
            @Override public List<EditorialEngineContractProfile> list() { return List.of(testOnlyProfile); }
            @Override public Optional<EditorialEngineContractProfile> findByCanonicalHash(String hash) { return Optional.of(testOnlyProfile).filter(p -> p.canonicalProfileHash().equals(hash)); }
            @Override public Optional<EditorialEngineContractProfile> findByIdentity(String id, String version) { return Optional.of(testOnlyProfile).filter(p -> p.engineProfileId().equals(id) && p.engineProfileVersion().equals(version)); }
        };

        EditorialPackImportResult result = new EditorialPackImportService(repository, storage,
                new EditorialEngineProfileResolver(testOnlyRegistry)).importPack(entries(unsupported));

        assertEquals(EditorialPackImportState.STORED_BLOCKED, result.state());
        assertEquals(EditorialPackCompatibilityClass.BLOCKED, result.compatibilityClass());
        assertTrue(result.blockedReason().startsWith("UNSUPPORTED_CONTRACT"));
        assertEquals(1, countRows("editorial_packs"));
        assertEquals(1, countRows("editorial_pack_compatibility_results"));
        assertEquals(0, countRows("editorial_pack_compatibility_evaluations"));
    }

    private EditorialPackImportService productionService() {
        return new EditorialPackImportService(repository, storage,
                new EditorialEngineProfileResolver(bootstrapOnlyRegistry()));
    }

    /**
     * These legacy wiring tests exercise the historical bootstrap profile's
     * non-executable behavior. Keep that fixture independent of the qualified
     * SAFE4 v2 profile now present in the real production catalog.
     */
    private static EditorialEngineContractProfileRegistry bootstrapOnlyRegistry() {
        EditorialEngineContractProfile bootstrap = BundledEditorialEngineContractProfileRegistry.load()
                .findByIdentity(TrustedEditorialEngineProfileCatalog.PRODUCTION_PROFILE_ID,
                        TrustedEditorialEngineProfileCatalog.PRODUCTION_PROFILE_VERSION).orElseThrow();
        return new EditorialEngineContractProfileRegistry() {
            @Override public List<EditorialEngineContractProfile> list() { return List.of(bootstrap); }
            @Override public Optional<EditorialEngineContractProfile> findByCanonicalHash(String hash) {
                return Optional.of(bootstrap).filter(profile -> profile.canonicalProfileHash().equals(hash));
            }
            @Override public Optional<EditorialEngineContractProfile> findByIdentity(String id, String version) {
                return Optional.of(bootstrap).filter(profile -> profile.engineProfileId().equals(id)
                        && profile.engineProfileVersion().equals(version));
            }
        };
    }

    private static EditorialEngineContractProfile validExecutableProfile(EditorialEngineContractProfile source) {
        EditorialEngineContractProfile draft = new EditorialEngineContractProfile(
                source.profileFormat(), source.profileFormatVersion(), "test-only.profile", "1.0.0", source.engineVersion(),
                "contract.test.v1", "contract.test.v1", List.of("evidence.test.v1"), List.of(),
                new EditorialEngineContractProfile.PhaseGraph(List.of("L1"), List.of()),
                java.util.Map.of("L1", new EditorialEngineContractProfile.ContextAllowList(List.of(), List.of())),
                source.evidenceSchemaFingerprints(), source.gateDefinitionFingerprints(), source.releaseArtifactFingerprints(),
                source.implementedCapabilities(), source.explicitlyMissingCapabilities(), source.capabilityEvidence(),
                source.bundledAdapterIds(), source.adapterDescriptors(), "0".repeat(64), "0".repeat(64),
                source.createdAt(), source.buildSourceCommit(), source.deprecationPolicy());
        String machine = EditorialEngineContractProfileCanonicalizer.machineContractFingerprint(draft);
        EditorialEngineContractProfile withMachine = new EditorialEngineContractProfile(
                draft.profileFormat(), draft.profileFormatVersion(), draft.engineProfileId(), draft.engineProfileVersion(), draft.engineVersion(),
                draft.minimumSupportedContractVersion(), draft.maximumSupportedContractVersion(), draft.supportedSchemaVersions(),
                draft.supportedInputRoles(), draft.supportedPhaseGraph(), draft.contextAllowListByPhase(),
                draft.evidenceSchemaFingerprints(), draft.gateDefinitionFingerprints(), draft.releaseArtifactFingerprints(),
                draft.implementedCapabilities(), draft.explicitlyMissingCapabilities(), draft.capabilityEvidence(),
                draft.bundledAdapterIds(), draft.adapterDescriptors(), machine, "0".repeat(64), draft.createdAt(), draft.buildSourceCommit(), draft.deprecationPolicy());
        String hash = EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(withMachine);
        return new EditorialEngineContractProfile(
                withMachine.profileFormat(), withMachine.profileFormatVersion(), withMachine.engineProfileId(), withMachine.engineProfileVersion(), withMachine.engineVersion(),
                withMachine.minimumSupportedContractVersion(), withMachine.maximumSupportedContractVersion(), withMachine.supportedSchemaVersions(),
                withMachine.supportedInputRoles(), withMachine.supportedPhaseGraph(), withMachine.contextAllowListByPhase(),
                withMachine.evidenceSchemaFingerprints(), withMachine.gateDefinitionFingerprints(), withMachine.releaseArtifactFingerprints(),
                withMachine.implementedCapabilities(), withMachine.explicitlyMissingCapabilities(), withMachine.capabilityEvidence(),
                withMachine.bundledAdapterIds(), withMachine.adapterDescriptors(), machine, hash, withMachine.createdAt(), withMachine.buildSourceCommit(), withMachine.deprecationPolicy());
    }

    private static String canonicalHashWithout(java.util.Map<String, Object> root) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(root);
        copy.remove("canonicalPackHash");
        byte[] canonical = EditorialCanonicalJson.canonicalize(copy).getBytes(StandardCharsets.UTF_8);
        byte[] domain = "EDITORIAL_PACK_CANONICAL_HASH_V1\n".getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[domain.length + canonical.length];
        System.arraycopy(domain, 0, payload, 0, domain.length);
        System.arraycopy(canonical, 0, payload, domain.length, canonical.length);
        return EditorialCanonicalJson.sha256Hex(payload);
    }

    private static List<EditorialPackImportEntry> entries(EditorialPackAndroidFixture.Fixture fixture) {
        ArrayList<EditorialPackImportEntry> result = new ArrayList<>();
        result.add(new EditorialPackImportEntry("editorial-pack.json",
                new ByteArrayInputStream(fixture.manifestBytes()), fixture.manifestBytes().length, -1L, false));
        for (Map.Entry<String, byte[]> entry : fixture.dataFiles().entrySet()) {
            result.add(new EditorialPackImportEntry(entry.getKey(), new ByteArrayInputStream(entry.getValue()),
                    entry.getValue().length, -1L, false));
        }
        return result;
    }

    private int countRows(String table) {
        try (android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }
}
