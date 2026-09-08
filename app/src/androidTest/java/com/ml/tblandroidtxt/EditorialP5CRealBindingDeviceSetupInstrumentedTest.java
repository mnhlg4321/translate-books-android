package com.ml.tblandroidtxt;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import org.junit.Test;
import org.junit.Assume;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Device-only setup for the authorized P5C pilot. The source bytes are
 * supplied outside Git under /sdcard/Download/p5c-real and are converted with
 * the same BOM handling as the existing app file import before app-owned
 * identities are calculated. This test deliberately does not call a provider.
 */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5CRealBindingDeviceSetupInstrumentedTest {
    private static final String SELECTOR = "p5c-real-mercedes-vol4-001";
    private static final String CHAPTER_KEY = "001";
    private static final String SOURCE_ROOT = "/sdcard/Download/p5c-real";

    @Test public void createOrVerifyPersistentExactBindingForChapter001() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        try (TranslationRepository database = new TranslationRepository(context)) {
            Path storageRoot = context.getFilesDir().toPath();
            EditorialPackStorageLayout storage = new EditorialPackStorageLayout(storageRoot);
            Optional<EditorialPackSelectionCandidate> candidate = new EditorialPackSelectionPolicy(database, storage)
                    .resolve("com.ml.tblandroidtxt.editorial.safe4.full", "4.1.3");
            Assume.assumeTrue("validation package has no persisted P5C pack after baseline restore",
                    candidate.isPresent());

            List<EditorialP4InputSource> sources = readSources(context);
            EditorialP4SetupRequest request = new EditorialP4SetupRequest(
                    SELECTOR, "MERCEDES", "VOL 4", candidate.get().packId(), candidate.get().packVersion(),
                    "p5c/mercedes/vol4", "p5c/mercedes/vol4/001", sources,
                    EditorialSafe4Contract.NORMAL_MODE, "AVAILABLE", "AVAILABLE", "NONE",
                    "USER_CONFIRMED_NORMAL_P5C", "EDITORIAL_PILOT", "L1_SOURCE_PREFLIGHT",
                    "manifest-attestation-v1", EditorialLineageNodeKind.ROOT, null,
                    System.currentTimeMillis());

            EditorialP4BindingResult result = new EditorialP4BindingTransactionService(database, storage)
                    .createSetup(request);
            assertTrue("P4 setup was not accepted: " + result.detail(), result.accepted());
            assertNotNull(result.binding());

            long projectId = result.projectId();
            EditorialP4Binding binding = result.binding();
            if (listChapterIds(database, projectId).isEmpty()) {
                insertChapter(database, projectId, sources);
            }

            EditorialP4ResumeResult resumed = new EditorialP4BindingTransactionService(database, storage)
                    .resumeProject(projectId, SELECTOR, sources);
            assertEquals(EditorialP4ResumeResult.Code.RESTORED, resumed.code());
            assertEquals(binding.bindingIdentity(), resumed.binding().bindingIdentity());
            assertEquals(binding.canonicalPackHash(), resumed.binding().canonicalPackHash());
            assertEquals(binding.inputManifestFingerprint(), resumed.binding().inputManifestFingerprint());
            assertEquals(1, listChapterIds(database, projectId).size());
            Log.i("P5C_REAL_BINDING", "projectId=" + projectId
                    + " selector=" + SELECTOR
                    + " bindingIdentity=" + binding.bindingIdentity()
                    + " runDeclarationIdentity=" + binding.runDeclarationIdentity()
                    + " compatibilityEvaluationId=" + binding.compatibilityEvaluationId()
                    + " canonicalPackHash=" + binding.canonicalPackHash()
                    + " canonicalProfileHash=" + binding.canonicalProfileHash()
                    + " inputManifestFingerprint=" + binding.inputManifestFingerprint());
            for (com.ml.tblandroidtxt.editorial.pack.EditorialP4SourceIdentity input : binding.inputs()) {
                Log.i("P5C_REAL_SOURCE", "role=" + input.role()
                        + " length=" + input.byteLength() + " sha256=" + input.sha256());
            }
        }
    }

    private static List<EditorialP4InputSource> readSources(Context context) throws Exception {
        return List.of(
                source(context, "RAW", "raw", "001_RAW_1_4.txt"),
                source(context, "DRAFT", "draft", "001_DRAFT_MERCEDES_VOL4.txt"),
                source(context, "GLOSSARY", "glossary", "001_CHAPTER_GLOSSARY_FINAL_MERCEDES_VOL4.csv"),
                source(context, "PRONOUN", "pronoun", "001_PRONOUN.csv"));
    }

    private static EditorialP4InputSource source(Context context, String role, String id, String name)
            throws Exception {
        byte[] bytes;
        try (InputStream input = context.openFileInput("p5c-real-" + id + ".bin")) {
            bytes = input.readAllBytes();
        }
        bytes = stripUtf8Bom(bytes);
        if (bytes.length == 0) throw new AssertionError("empty real source: " + name);
        return new EditorialP4InputSource(role, "p5c-real://mercedes-vol4/001/" + id,
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
            android.content.ContentValues chapter = new android.content.ContentValues();
            chapter.put("project_id", projectId);
            chapter.put("chapter_key", CHAPTER_KEY);
            chapter.put("title", "MERCEDES VOL 4 • 001");
            chapter.put("state", EditorialSafe4Workflow.ChapterState.SAFE4_BLOCKED.name());
            chapter.put("raw_hash", EditorialCanonicalJson.sha256Hex(sources.get(0).bytes()));
            chapter.put("created_at", now);
            chapter.put("updated_at", now);
            long chapterId = db.insertOrThrow("editorial_chapters", null, chapter);
            for (EditorialP4InputSource source : sources) {
                android.content.ContentValues asset = new android.content.ContentValues();
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
        ArrayList<Long> ids = new ArrayList<>();
        try (android.database.Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT id FROM editorial_chapters WHERE project_id=? AND chapter_key=?",
                new String[]{String.valueOf(projectId), CHAPTER_KEY})) {
            while (cursor.moveToNext()) ids.add(cursor.getLong(0));
        }
        return ids;
    }
}
