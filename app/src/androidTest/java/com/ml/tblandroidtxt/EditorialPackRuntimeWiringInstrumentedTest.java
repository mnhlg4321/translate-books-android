package com.ml.tblandroidtxt;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluationContext;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityEvaluator;
import com.ml.tblandroidtxt.editorial.pack.EditorialCompatibilityProvenance;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        EditorialEngineContractProfile profile = BundledEditorialEngineContractProfileRegistry.load().list().get(0);
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
        EditorialEngineContractProfileRegistry source = BundledEditorialEngineContractProfileRegistry.load();
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
        assertTrue(second.alreadyExisted());
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

    private EditorialPackImportService productionService() {
        return new EditorialPackImportService(repository, storage,
                new EditorialEngineProfileResolver(BundledEditorialEngineContractProfileRegistry.load()));
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
