package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution.Store.Claim;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Durable L2_EDIT store (schema v25): claim, atomic commit, recovery and CHECK enforcement. */
@RunWith(AndroidJUnit4.class)
public final class EditorialPhaseArtifactStoreInstrumentedTest {
    private static final String CHAPTER = "chapter-001";
    private static final String ATTEMPT = hex('a');
    private static final String PREDECESSOR = hex('b');
    private static final String BUNDLE = hex('c');

    private Context context;
    private String databaseName;
    private TranslationRepository database;
    private EditorialPhaseArtifactStore store;

    private static String hex(char c) {
        return String.valueOf(c).repeat(64);
    }

    private static String sha(byte[] bytes) {
        return EditorialCanonicalJson.sha256Hex(bytes);
    }

    private static EditorialL2Execution.Committed committed(String attempt, byte[] text, byte[] map) {
        return new EditorialL2Execution.Committed(attempt, PREDECESSOR, BUNDLE, text, sha(text), map, sha(map));
    }

    private int rowCount(String where) {
        try (Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM editorial_phase_artifacts WHERE " + where, null)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "editorial-phase-artifacts-" + UUID.randomUUID() + ".db";
        database = new TranslationRepository(context, databaseName);
        store = new EditorialPhaseArtifactStore(database, CHAPTER);
    }

    @After public void tearDown() {
        if (database != null) database.close();
        context.deleteDatabase(databaseName);
    }

    @Test public void claimIsAcquiredOnceThenInFlight() {
        assertEquals(Claim.ACQUIRED, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
        assertEquals(Claim.IN_FLIGHT, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
        assertFalse(store.findCommitted(ATTEMPT).isPresent());
    }

    @Test public void commitThenFindCommittedReturnsIdenticalPairAndClaimIsAlreadyCommitted() {
        byte[] text = "vi-l2 text àạ".getBytes(StandardCharsets.UTF_8);
        byte[] map = "{\"changes\":[]}".getBytes(StandardCharsets.UTF_8);
        assertEquals(Claim.ACQUIRED, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
        store.commit(committed(ATTEMPT, text, map));

        Optional<EditorialL2Execution.Committed> found = store.findCommitted(ATTEMPT);
        assertTrue(found.isPresent());
        assertArrayEquals(text, found.get().viL2Bytes());
        assertArrayEquals(map, found.get().changeMapBytes());
        assertEquals(sha(text), found.get().viL2Sha256());
        assertEquals(sha(map), found.get().changeMapSha256());
        assertEquals(PREDECESSOR, found.get().predecessorIdentity());
        assertEquals(BUNDLE, found.get().bundleIdentity());
        assertEquals(Claim.ALREADY_COMMITTED, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
    }

    @Test public void commitWithMismatchedHashThrowsAndLeavesNoCommittedRow() {
        byte[] text = "text".getBytes(StandardCharsets.UTF_8);
        byte[] map = "map".getBytes(StandardCharsets.UTF_8);
        assertEquals(Claim.ACQUIRED, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
        EditorialL2Execution.Committed bad = new EditorialL2Execution.Committed(ATTEMPT, PREDECESSOR,
                BUNDLE, text, hex('f'), map, sha(map));
        try {
            store.commit(bad);
            fail("expected hash mismatch");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        assertEquals(0, rowCount("status='COMMITTED'"));
        assertFalse(store.findCommitted(ATTEMPT).isPresent());
        assertEquals(Claim.IN_FLIGHT, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
    }

    @Test public void recoveryRequiredIsReportedAndCommitIsRejected() {
        assertEquals(Claim.ACQUIRED, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
        store.markRecoveryRequired(ATTEMPT, "RETRY_L2_PROVIDER_CALL_FAILED");
        assertEquals(Claim.RECOVERY_REQUIRED, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
        try {
            store.commit(committed(ATTEMPT, "t".getBytes(StandardCharsets.UTF_8),
                    "m".getBytes(StandardCharsets.UTF_8)));
            fail("commit must be rejected after recovery is required");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertEquals(0, rowCount("status='COMMITTED'"));
        assertFalse(store.findCommitted(ATTEMPT).isPresent());
    }

    @Test public void claimWithDifferentPredecessorForSameAttemptThrows() {
        assertEquals(Claim.ACQUIRED, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
        try {
            store.claim(ATTEMPT, hex('d'), BUNDLE);
            fail("changed predecessor must be rejected");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertEquals(1, rowCount("attempt_identity='" + ATTEMPT + "'"));
    }

    @Test public void committedPairSurvivesReopeningTheDatabase() {
        byte[] text = "persisted text".getBytes(StandardCharsets.UTF_8);
        byte[] map = "persisted map".getBytes(StandardCharsets.UTF_8);
        assertEquals(Claim.ACQUIRED, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
        store.commit(committed(ATTEMPT, text, map));
        database.close();

        database = new TranslationRepository(context, databaseName);
        store = new EditorialPhaseArtifactStore(database, CHAPTER);
        Optional<EditorialL2Execution.Committed> found = store.findCommitted(ATTEMPT);
        assertTrue(found.isPresent());
        assertArrayEquals(text, found.get().viL2Bytes());
        assertArrayEquals(map, found.get().changeMapBytes());
        assertEquals(sha(text), found.get().viL2Sha256());
        assertEquals(sha(map), found.get().changeMapSha256());
        assertEquals(Claim.ALREADY_COMMITTED, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
    }

    @Test public void directCommittedRowWithNullBlobIsRejectedByCheckConstraint() {
        ContentValues values = new ContentValues();
        values.put("attempt_identity", ATTEMPT);
        values.put("phase", "L2_EDIT");
        values.put("chapter_key", CHAPTER);
        values.put("predecessor_identity", PREDECESSOR);
        values.put("bundle_identity", BUNDLE);
        values.put("status", "COMMITTED");
        values.putNull("text_bytes");
        values.put("text_sha256", hex('e'));
        values.put("evidence_bytes", new byte[]{1});
        values.put("evidence_sha256", hex('e'));
        values.put("created_at", 1L);
        values.put("updated_at", 1L);
        try {
            database.editorialWritableDatabase().insertOrThrow("editorial_phase_artifacts", null, values);
            fail("CHECK constraint must reject COMMITTED with NULL text_bytes");
        } catch (SQLiteConstraintException expected) {
            // expected
        }
        assertEquals(0, rowCount("1=1"));
    }

    @Test public void claimedRowCannotBeUpdatedToCommittedWithNullTextBytes() {
        assertEquals(Claim.ACQUIRED, store.claim(ATTEMPT, PREDECESSOR, BUNDLE));
        try {
            database.editorialWritableDatabase().execSQL(
                    "UPDATE editorial_phase_artifacts SET status='COMMITTED' WHERE attempt_identity='"
                            + ATTEMPT + "'");
            fail("CHECK constraint must reject COMMITTED with NULL blobs");
        } catch (SQLiteConstraintException expected) {
            // expected
        }
        assertEquals(1, rowCount("status='CLAIMED'"));
    }
}
