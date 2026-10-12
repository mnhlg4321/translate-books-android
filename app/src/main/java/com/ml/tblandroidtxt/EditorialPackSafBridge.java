package com.ml.tblandroidtxt;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

/** Thin SAF bridge; it never parses a ZIP, reads content or persists an external URI. */
public final class EditorialPackSafBridge {
    public static final int REQUEST_CODE = 3107;
    private static final String ZIP_MIME = "application/zip";
    private static final String GENERIC_MIME = "application/octet-stream";

    private EditorialPackSafBridge() { }

    public static boolean isRequest(int requestCode) { return requestCode == REQUEST_CODE; }

    public static void launch(Activity activity) {
        if (activity == null) throw new IllegalArgumentException("Activity is required");
        try {
            activity.startActivityForResult(openIntent(ZIP_MIME), REQUEST_CODE);
        } catch (ActivityNotFoundException noZipProvider) {
            activity.startActivityForResult(openIntent(GENERIC_MIME), REQUEST_CODE);
        }
    }

    public static Intent openIntent(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType == null || mimeType.isBlank() ? ZIP_MIME : mimeType);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{ZIP_MIME, GENERIC_MIME});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    /** Rejects ClipData so a provider cannot silently turn the single-file picker into multi-select. */
    public static Uri selectedUri(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null || data.getClipData() != null) return null;
        return data.getData();
    }
}
