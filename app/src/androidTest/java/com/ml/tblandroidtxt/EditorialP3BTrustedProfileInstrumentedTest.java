package com.ml.tblandroidtxt;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.TrustedEditorialEngineProfileCatalog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Device proof that the bundled v2 profile is trusted without opening execution. */
@RunWith(AndroidJUnit4.class)
public class EditorialP3BTrustedProfileInstrumentedTest {
    private static final String CANONICAL_ASSET = "editorial-p2/v5-safe-4.1.3-full-canonical.zip";

    private Context context;
    private String databaseName;
    private TranslationRepository repository;
    private EditorialPackStorageLayout storage;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-p3b-profile-" + UUID.randomUUID() + ".db";
        repository = new TranslationRepository(context, databaseName);
        storage = new EditorialPackStorageLayout(context.getCacheDir().toPath().resolve(UUID.randomUUID().toString()));
    }

    @After public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(databaseName);
    }

    @Test
    public void canonicalSafe4UsesBundledTrustedProfileButExecutionRemainsDisabled() throws Exception {
        BundledEditorialEngineContractProfileRegistry registry =
                BundledEditorialEngineContractProfileRegistry.load();
        EditorialEngineContractProfile profile = registry.findByIdentity(
                TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_ID,
                TrustedEditorialEngineProfileCatalog.SAFE4_PROFILE_VERSION).orElseThrow();
        byte[] zip;
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open(CANONICAL_ASSET)) {
            zip = input.readAllBytes();
        }

        EditorialPackImportResult result = new EditorialPackImportService(repository, storage,
                new EditorialEngineProfileResolver(registry)).importZip(new ByteArrayInputStream(zip));

        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, result.state());
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, result.compatibilityClass());
        assertTrue(result.readyForCertification());
        EditorialPackCompatibilityEvaluation evidence = new EditorialPackCompatibilityEvaluationDao(repository)
                .listByPackHash(result.canonicalPackHash()).get(0);
        assertEquals(profile.engineProfileId(), evidence.trustedProfileId().orElseThrow());
        assertEquals(profile.engineProfileVersion(), evidence.trustedProfileVersion().orElseThrow());
        assertEquals(profile.canonicalProfileHash(), evidence.canonicalProfileHash().orElseThrow());
        assertFalse(EditorialSafe4Pack.executionEnabled());
        assertEquals(0, countRowsWhere("editorial_packs", "state='CERTIFIED'"));
    }

    private int countRowsWhere(String table, String where) {
        try (android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + table + " WHERE " + where, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }
}
