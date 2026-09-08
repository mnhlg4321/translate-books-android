package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4SourceIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Explicit P5D device path for the separately authorized VOL5/001 RAW call.
 * Source bytes are staged into the debuggable validation package by the host;
 * this test never logs source text, a request body or a provider response.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5DVol5RawPilotInstrumentedTest {
    private static final String CANONICAL_ASSET =
            "editorial-p2/v5-safe-4.1.3-full-canonical.zip";
    private static final String PACK_ID = "com.ml.tblandroidtxt.editorial.safe4.full";
    private static final String PACK_VERSION = "4.1.3";
    private static final String SELECTOR = "p5d-raw-mercedes-vol5-001";
    private static final String CHAPTER_KEY = "001";
    private static final String PROVIDER = "openrouter";
    private static final String MODEL = "openai/gpt-5.6-luna";
    private static final long PILOT_WINDOW_MILLIS = 5 * 60 * 1000L;

    @Test public void preparePersistedVol5Chapter001Binding() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        try (TranslationRepository database = new TranslationRepository(context)) {
            EditorialPackStorageLayout storage = new EditorialPackStorageLayout(
                    context.getFilesDir().toPath());
            EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                    .resolve(PACK_ID, PACK_VERSION).orElse(null);
            if (candidate == null) {
                byte[] packBytes;
                try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                        .getAssets().open(CANONICAL_ASSET)) {
                    packBytes = input.readAllBytes();
                }
                EditorialPackImportResult imported = new EditorialPackImportService(database, storage,
                        new EditorialEngineProfileResolver(
                                BundledEditorialEngineContractProfileRegistry.load()))
                        .importZip(new ByteArrayInputStream(packBytes));
                assertEquals(EditorialPackImportState.STORED_READY_FOR_CERTIFICATION,
                        imported.state());
                candidate = new EditorialPackSelectionPolicy(database, storage)
                        .resolve(PACK_ID, PACK_VERSION).orElse(null);
            }
            Assume.assumeTrue("canonical 4.1.3 pack is not imported in validation package",
                    candidate != null);
            if (candidate == null) return;
            List<EditorialP4InputSource> sources = readSources(context);
            EditorialP4SetupRequest request = new EditorialP4SetupRequest(
                    SELECTOR, "MERCEDES", "VOL 5", candidate.packId(), candidate.packVersion(),
                    "p5d/mercedes/vol5", "p5d/mercedes/vol5/001", sources,
                    EditorialSafe4Contract.NORMAL_MODE, "AVAILABLE", "AVAILABLE", "NONE",
                    "USER_CONFIRMED_NORMAL_P5D_RAW_VOL5", "EDITORIAL_PILOT",
                    "L1_SOURCE_PREFLIGHT", "manifest-attestation-v1", EditorialLineageNodeKind.ROOT,
                    null, System.currentTimeMillis());
            EditorialP4BindingResult result = new EditorialP4BindingTransactionService(database, storage)
                    .createSetup(request);
            assertTrue("VOL5 P4 setup was not accepted: " + result.detail(), result.accepted());
            assertNotNull(result.binding());
            long projectId = result.projectId();
            if (listChapterIds(database, projectId).isEmpty()) insertChapter(database, projectId, sources);

            EditorialP4ResumeResult resumed = new EditorialP4BindingTransactionService(database, storage)
                    .resumeProject(projectId, SELECTOR, sources);
            assertEquals(EditorialP4ResumeResult.Code.RESTORED, resumed.code());
            assertEquals(result.binding().bindingIdentity(), resumed.binding().bindingIdentity());
            logBinding(result.binding(), projectId, sources);
        }
    }

    @Test public void authorizedVol5RawRunsOnlyWhenExplicitlyOptedIn() throws Exception {
        String optIn = InstrumentationRegistry.getArguments().getString("p5d_raw_live", "");
        Assume.assumeTrue("new VOL5 RAW call is explicit opt-in", "YES".equalsIgnoreCase(optIn));

        Context context = ApplicationProvider.getApplicationContext();
        AppSettings settings = SettingsStore.load(context);
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: provider must be OpenRouter",
                PROVIDER.equalsIgnoreCase(nullToEmpty(settings.provider)));
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: OpenRouter endpoint required",
                AppSettings.defaultBaseUrl(PROVIDER).equals(AppSettings.normalizeEndpoint(settings.baseUrl)));
        assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: OpenRouter API key is absent after validation restore",
                settings.apiKey != null && !settings.apiKey.trim().isEmpty());

        settings.provider = PROVIDER;
        settings.baseUrl = AppSettings.defaultBaseUrl(PROVIDER);
        settings.model = MODEL;
        settings.timeoutSeconds = 300;

        try (TranslationRepository database = new TranslationRepository(context)) {
            EditorialP4Binding binding = new EditorialP4BindingDao(database)
                    .findByAttemptRequestSelector(SELECTOR).orElseThrow(
                            () -> new AssertionError("VOL5 P4 binding is not persisted"));
            long projectId = projectId(database, binding.bindingIdentity());
            assertTrue("VOL5 persisted project id is required", projectId > 0);
            String endpointAccountFingerprint = EditorialCanonicalJson.sha256Hex(
                    (settings.baseUrl + "|" + EditorialCanonicalJson.sha256Hex(
                            settings.apiKey.getBytes(StandardCharsets.UTF_8)))
                            .getBytes(StandardCharsets.UTF_8));
            long issuedAt = System.currentTimeMillis();
            long expiresAt = issuedAt + PILOT_WINDOW_MILLIS;
            EditorialP5PilotAuthorization raw = new EditorialP5PilotAuthorization(
                    "p5d-raw-vol5-001-" + issuedAt,
                    binding.bindingIdentity(), binding.runDeclarationIdentity(),
                    binding.canonicalPackHash(), binding.canonicalProfileHash(),
                    binding.compatibilityEvaluationId(), CHAPTER_KEY, "L1_RAW_DISCOVERY",
                    PROVIDER, MODEL, endpointAccountFingerprint,
                    1, 1, 0, 100_000, 2_048, 100_000, BigDecimal.valueOf(0.10),
                    PILOT_WINDOW_MILLIS, true, false, false, "HASH_ONLY",
                    "USER_AUTHORIZED_NEW_RAW_VOL5;RECONCILE_NOT_AUTHORIZED",
                    issuedAt, expiresAt, true);

            Log.i("P5D_RAW_PREFLIGHT", "selector=" + SELECTOR
                    + " projectId=" + projectId
                    + " bindingIdentity=" + binding.bindingIdentity()
                    + " runDeclarationIdentity=" + binding.runDeclarationIdentity()
                    + " compatibilityEvaluationId=" + binding.compatibilityEvaluationId()
                    + " canonicalPackHash=" + binding.canonicalPackHash()
                    + " canonicalProfileHash=" + binding.canonicalProfileHash()
                    + " provider=" + PROVIDER + " model=" + MODEL
                    + " endpointAccountFingerprint=" + endpointAccountFingerprint
                    + " primaryCalls=1 schemaRepairCalls=1 networkRetries=0"
                    + " maxTotalCost=0.10 maxExecutionTimeMs=" + PILOT_WINDOW_MILLIS);
            for (EditorialP4SourceIdentity input : binding.inputs()) {
                Log.i("P5D_RAW_PREFLIGHT", "source role=" + input.role()
                        + " length=" + input.byteLength() + " sha256=" + input.sha256());
            }

            OpenRouterEditorialP5PilotProvider provider =
                    new OpenRouterEditorialP5PilotProvider(settings, 2_048);
            assertTrue("LIVE_AUTHORIZATION_INCOMPLETE: provider configuration is incomplete",
                    provider.configured());
            EditorialP5CExactBindingExecution.Result result =
                    new EditorialP5CExactBindingExecution(database,
                            new EditorialPackStorageLayout(context.getFilesDir().toPath()))
                            .executeRaw(projectId, SELECTOR, CHAPTER_KEY, raw, provider);
            logResult(result);
            assertNotNull(result);
            assertTrue("RAW result exceeded the authorized primary plus repair calls",
                    result.providerCalls() <= 2);
            org.junit.Assert.assertNull("RECONCILE must not be called by RAW-only authorization",
                    result.reconcileResult());
            assertTrue("execution must remain disabled", !result.executionAllowed());
            assertEquals("NOT_CERTIFIED", result.certificationState());
            if (result.accepted()) {
                assertNotNull(result.rawResult());
                assertTrue(result.rawResult().outcome() == EditorialP5PilotResult.Outcome.COMMITTED
                        || result.rawResult().outcome() == EditorialP5PilotResult.Outcome.ALREADY_COMMITTED);
            }
        }
    }

    private static List<EditorialP4InputSource> readSources(Context context) throws Exception {
        return List.of(
                source(context, "RAW", "raw", "001_RAW_MERCEDES_VOL5.TXT"),
                source(context, "DRAFT", "draft", "001_DRAFT_MERCEDES_VOL5.TXT"),
                source(context, "GLOSSARY", "glossary", "001_GLOSSARY_MERCEDES_VOL5.CSV"),
                source(context, "PRONOUN", "pronoun", "001_PRONOUN_MERCEDES_VOL5.CSV"));
    }

    private static EditorialP4InputSource source(Context context, String role, String id,
                                                  String originalName) throws Exception {
        byte[] bytes;
        try (InputStream input = context.openFileInput("p5d-vol5-" + id + ".bin")) {
            bytes = stripUtf8Bom(input.readAllBytes());
        }
        if (bytes.length == 0) throw new AssertionError("empty VOL5 source: " + originalName);
        return new EditorialP4InputSource(role, "p5d-real://mercedes-vol5/001/" + id,
                bytes, "UTF-8", "VALID", 0L);
    }

    private static byte[] stripUtf8Bom(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xef && (bytes[1] & 0xff) == 0xbb
                && (bytes[2] & 0xff) == 0xbf) return Arrays.copyOfRange(bytes, 3, bytes.length);
        return bytes;
    }

    private static void insertChapter(TranslationRepository database, long projectId,
                                      List<EditorialP4InputSource> sources) {
        android.database.sqlite.SQLiteDatabase db = database.editorialWritableDatabase();
        long now = System.currentTimeMillis();
        db.beginTransaction();
        try {
            ContentValues chapter = new ContentValues();
            chapter.put("project_id", projectId);
            chapter.put("chapter_key", CHAPTER_KEY);
            chapter.put("title", "MERCEDES VOL 5 • 001");
            chapter.put("state", EditorialSafe4Workflow.ChapterState.SAFE4_BLOCKED.name());
            chapter.put("raw_hash", EditorialCanonicalJson.sha256Hex(sources.get(0).bytes()));
            chapter.put("created_at", now);
            chapter.put("updated_at", now);
            long chapterId = db.insertOrThrow("editorial_chapters", null, chapter);
            for (EditorialP4InputSource source : sources) {
                ContentValues asset = new ContentValues();
                asset.put("chapter_id", chapterId);
                asset.put("role", source.role());
                asset.put("source_uri", source.sourceReference());
                asset.put("display_name", source.role().toLowerCase() + ".source");
                asset.put("sha256", EditorialCanonicalJson.sha256Hex(source.bytes()));
                asset.put("size_bytes", source.bytes().length);
                asset.put("content", new String(source.bytes(), StandardCharsets.UTF_8));
                asset.put("created_at", now);
                db.insertOrThrow("editorial_assets", null, asset);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static List<Long> listChapterIds(TranslationRepository database, long projectId) {
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        try (android.database.Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT id FROM editorial_chapters WHERE project_id=? AND chapter_key=?",
                new String[]{String.valueOf(projectId), CHAPTER_KEY})) {
            while (cursor.moveToNext()) ids.add(cursor.getLong(0));
        }
        return ids;
    }

    private static long projectId(TranslationRepository database, String bindingIdentity) {
        try (android.database.Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT project_row_id FROM editorial_p4_bindings WHERE binding_identity=?",
                new String[]{bindingIdentity})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    private static void logBinding(EditorialP4Binding binding, long projectId,
                                   List<EditorialP4InputSource> sources) {
        Log.i("P5D_VOL5_BINDING", "projectId=" + projectId + " selector=" + SELECTOR
                + " bindingIdentity=" + binding.bindingIdentity()
                + " runDeclarationIdentity=" + binding.runDeclarationIdentity()
                + " canonicalPackHash=" + binding.canonicalPackHash()
                + " canonicalProfileHash=" + binding.canonicalProfileHash()
                + " sourceManifestFingerprint=" + binding.inputManifestFingerprint());
        for (EditorialP4InputSource source : sources) {
            Log.i("P5D_VOL5_SOURCE", "role=" + source.role() + " length=" + source.bytes().length
                    + " sha256=" + EditorialCanonicalJson.sha256Hex(source.bytes()));
        }
    }

    private static void logResult(EditorialP5CExactBindingExecution.Result result) {
        Log.i("P5D_RAW_RESULT", "status=" + result.status() + " reason=" + result.reasonCode()
                + " providerCalls=" + result.providerCalls()
                + " executionAllowed=" + result.executionAllowed()
                + " certificationState=" + result.certificationState());
        EditorialP5PilotResult raw = result.rawResult();
        if (raw == null) {
            Log.i("P5D_RAW_METRICS", "result=NOT_AVAILABLE");
            return;
        }
        EditorialP5PilotResult.Metrics metrics = raw.metrics();
        Log.i("P5D_RAW_METRICS", "outcome=" + raw.outcome() + " reason=" + raw.reasonCode()
                + " primaryCalls=" + metrics.primaryCalls()
                + " repairCalls=" + metrics.repairCalls()
                + " networkRetries=" + metrics.networkRetries()
                + " inputTokens=" + metrics.inputTokens()
                + " outputTokens=" + metrics.outputTokens()
                + " totalTokens=" + metrics.totalTokens()
                + " actualReportedCost=" + metrics.actualReportedCost()
                + " finishReason=" + metrics.finishReason()
                + " truncated=" + metrics.truncated()
                + " schemaValid=" + metrics.schemaValidationPassed()
                + " receiptValid=" + metrics.receiptValidationPassed()
                + " latencyMs=" + metrics.latencyMillis());
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
