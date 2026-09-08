package com.ml.tblandroidtxt;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.Assume;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Redacted readback of the one live attempt after provider timeout. */
@RunWith(AndroidJUnit4.class)
public final class EditorialP5CLiveRecoveryInspectionInstrumentedTest {
    @Test public void timedOutLiveAttemptIsRecoveryOnly() {
        Context context = ApplicationProvider.getApplicationContext();
        try (TranslationRepository database = new TranslationRepository(context);
             Cursor cursor = database.editorialReadableDatabase().rawQuery(
                     "SELECT status,response_identity,report_bytes,receipt_bytes,"
                             + "recovery_reason_code,phase FROM editorial_p5c_attempts "
                             + "WHERE provider=? AND model=? AND chapter_key=?",
                     new String[]{"openrouter", "openai/gpt-5.6-luna", "001"})) {
            int rows = 0;
            while (cursor.moveToNext()) {
                rows++;
                String status = cursor.getString(0);
                String responseIdentity = cursor.getString(1);
                int reportBytes = cursor.isNull(2) ? 0 : cursor.getBlob(2).length;
                int receiptBytes = cursor.isNull(3) ? 0 : cursor.getBlob(3).length;
                String recovery = cursor.getString(4);
                String phase = cursor.getString(5);
                Log.i("P5C_LIVE_RECOVERY", "status=" + status
                        + " responseIdentityPresent=" + (responseIdentity != null
                        && !responseIdentity.isBlank())
                        + " reportBytes=" + reportBytes
                        + " receiptBytes=" + receiptBytes
                        + " recoveryReason=" + recovery
                        + " phase=" + phase);
                assertEquals("RECOVERY_REQUIRED", status);
                assertEquals(0, reportBytes);
                assertEquals(0, receiptBytes);
                assertEquals("RETRY_PROVIDER_CALL_FAILED", recovery);
                assertTrue(responseIdentity == null || responseIdentity.isBlank());
                assertEquals("L1_RAW_DISCOVERY", phase);
            }
            Assume.assumeTrue("historical P5C runtime row is absent after required baseline restore",
                    rows > 0);
            assertEquals("one live raw attempt must be present", 1, rows);
        }
    }
}
