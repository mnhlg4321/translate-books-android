package com.ml.tblandroidtxt;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EditorialPackZipImportInstrumentedTest {
    @Test public void safPickerIsSingleReadOnlyZipWithGenericFallback() {
        Intent intent = EditorialPackSafBridge.openIntent("application/zip");
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.getAction());
        assertEquals("application/zip", intent.getType());
        assertFalse(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, true));
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == 0);
        assertEquals(2, intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES).length);
    }

    @Test public void multiSelectionAndCancelNeverBecomeASelectedZip() {
        Intent multi = new Intent();
        ClipData clip = new ClipData("packs", new String[]{"application/zip"}, new ClipData.Item(Uri.parse("content://pack/one.zip")));
        clip.addItem(new ClipData.Item(Uri.parse("content://pack/two.zip")));
        multi.setClipData(clip);
        multi.setData(Uri.parse("content://pack/one.zip"));
        assertNull(EditorialPackSafBridge.selectedUri(android.app.Activity.RESULT_OK, multi));
        assertNull(EditorialPackSafBridge.selectedUri(android.app.Activity.RESULT_CANCELED, null));
    }

    @Test public void editorialTabExposesOnlyZipImportActionAndReadOnlyManagement() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View page = new EditorialPageFactory(activity).build();
                String text = allText(page);
                assertNotNull(page);
                assertTrue(text.contains("Import Editorial Pack ZIP"));
                assertTrue(text.contains("READ ONLY"));
                assertFalse(text.contains("Certify"));
                assertFalse(text.contains("Activate"));
                assertFalse(text.contains("Delete"));
                assertFalse(text.contains("Replace"));
            });
        }
    }

    @Test public void cancelledSafResultDoesNotChangePackRowCount() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                int before = countPacks(activity);
                activity.onActivityResult(EditorialPackSafBridge.REQUEST_CODE, android.app.Activity.RESULT_CANCELED, null);
                assertEquals(before, countPacks(activity));
            });
        }
    }

    private static int countPacks(MainActivity activity) {
        try (TranslationRepository repository = new TranslationRepository(activity);
             android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery("SELECT COUNT(*) FROM editorial_packs", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        }
    }

    private static String allText(View view) {
        StringBuilder out = new StringBuilder();
        if (view instanceof TextView) out.append(((TextView) view).getText()).append('\n');
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) out.append(allText(((ViewGroup) view).getChildAt(i)));
        return out.toString();
    }
}
