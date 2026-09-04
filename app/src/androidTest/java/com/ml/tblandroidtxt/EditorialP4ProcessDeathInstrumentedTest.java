package com.ml.tblandroidtxt;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Two-invocation device proof for durable P4 resume. Run a_prepare, stop the
 * target process from the host, then run b_resume; the full suite also runs
 * both in deterministic order as a persistence regression.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public final class EditorialP4ProcessDeathInstrumentedTest {
    private static final String ASSET = "editorial-p2/v5-safe-4.1.3-full-canonical.zip";
    private static final String DATABASE_NAME = "editorial-p4-process-death-proof.db";
    private static final String SELECTOR = "p4-process-death-selector";
    private static final String CANONICAL_PACK_HASH =
            "497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d";
    private static final String STORAGE_DIRECTORY = "editorial-p4-process-death-proof";

    private Context context;
    private TranslationRepository database;
    private Path storageRoot;
    private EditorialPackStorageLayout storage;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        storageRoot = context.getCacheDir().toPath().resolve(STORAGE_DIRECTORY);
        database = null;
        storage = new EditorialPackStorageLayout(storageRoot);
    }

    @After public void tearDown() {
        if (database != null) database.close();
    }

    @Test public void a_prepareExactBindingForHostProcessStop() throws Exception {
        context.deleteDatabase(DATABASE_NAME);
        deleteTree(storageRoot);
        database = new TranslationRepository(context, DATABASE_NAME);
        PackFixture fixture = fixture();
        EditorialPackImportResult imported = importZip(fixture.zipBytes);
        assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION, imported.state());
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, imported.compatibilityClass());
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                .resolve(fixture.manifest.packId(), fixture.manifest.version()).orElseThrow();

        EditorialP4SetupRequest request = request(candidate);
        EditorialP4BindingResult created = new EditorialP4BindingTransactionService(database, storage)
                .createSetup(request);
        assertEquals(EditorialP4BindingResult.Code.APPENDED, created.code());
        assertNotNull(created.binding());
        assertEquals(CANONICAL_PACK_HASH, created.binding().canonicalPackHash());
        assertFalse(created.binding().executionAllowed());
    }

    @Test public void b_resumeExactBindingAfterHostProcessStop() {
        database = new TranslationRepository(context, DATABASE_NAME);
        EditorialP4ResumeResult resumed = new EditorialP4BindingTransactionService(database, storage)
                .resumeBySelector(SELECTOR, sources());
        assertEquals(EditorialP4ResumeResult.Code.RESTORED, resumed.code());
        assertNotNull(resumed.binding());
        assertEquals(CANONICAL_PACK_HASH, resumed.binding().canonicalPackHash());
        assertEquals("4.1.3", resumed.binding().packVersion());
        assertEquals("AVAILABLE", resumed.binding().pronounStatus());
        assertFalse(resumed.executionAllowed());

        database.close();
        database = null;
        context.deleteDatabase(DATABASE_NAME);
        try {
            deleteTree(storageRoot);
        } catch (IOException error) {
            throw new AssertionError("process-death proof cleanup failed", error);
        }
    }

    private EditorialPackImportResult importZip(byte[] bytes) {
        return new EditorialPackImportService(database, storage,
                new EditorialEngineProfileResolver(BundledEditorialEngineContractProfileRegistry.load()))
                .importZip(new ByteArrayInputStream(bytes));
    }

    private EditorialP4SetupRequest request(EditorialPackSelectionCandidate candidate) {
        return new EditorialP4SetupRequest(SELECTOR, "Process death", "4.1.3",
                candidate.packId(), candidate.packVersion(), "semantic/process-death",
                "scope/process-death", sources(), EditorialSafe4Contract.NORMAL_MODE, "AVAILABLE", "AVAILABLE", "NONE",
                "USER_CONFIRMED_NORMAL", "EDITORIAL_SETUP", "L1_SOURCE_PREFLIGHT",
                "manifest-attestation-v1", EditorialLineageNodeKind.ROOT, null, 2000L);
    }

    private List<EditorialP4InputSource> sources() {
        return List.of(
                source("RAW", "raw", "raw process-death"),
                source("DRAFT", "draft", "draft process-death"),
                source("GLOSSARY", "glossary", "glossary process-death"),
                source("PRONOUN", "pronoun", "pronoun process-death"));
    }

    private EditorialP4InputSource source(String role, String reference, String value) {
        return new EditorialP4InputSource(role, "content://p4/process-death/" + reference,
                value.getBytes(StandardCharsets.UTF_8), "UTF-8", "VALID", 0L);
    }

    private PackFixture fixture() throws IOException {
        byte[] zipBytes;
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open(ASSET)) {
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
        return new PackFixture(zipBytes, EditorialPackManifest.parse(manifestBytes));
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException error) {
                    throw new RuntimeException(error);
                }
            });
        }
    }

    private static final class PackFixture {
        final byte[] zipBytes;
        final EditorialPackManifest manifest;

        PackFixture(byte[] zipBytes, EditorialPackManifest manifest) {
            this.zipBytes = zipBytes;
            this.manifest = manifest;
        }
    }
}
