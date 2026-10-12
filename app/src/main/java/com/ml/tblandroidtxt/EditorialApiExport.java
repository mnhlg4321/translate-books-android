package com.ml.tblandroidtxt;

import android.content.Context;
import android.net.Uri;

/** Exports the final text of a run as UTF-8 TXT and proves it by reading the file back. */
final class EditorialApiExport {
    private EditorialApiExport() { }

    /** @return the SHA-256 of the file read back, equal to the SHA-256 of {@code finalText} */
    static String exportFinal(Context context, Uri destination, EditorialApiRun run) throws Exception {
        if (run == null || run.finalText == null || run.finalText.isEmpty()) throw new IllegalArgumentException("no final text to export");
        FileUtil.writeTextVerified(context, destination, run.finalText);
        String readBack = HashUtil.sha256(FileUtil.readText(context, destination));
        String expected = HashUtil.sha256(run.finalText);
        if (!expected.equals(readBack)) throw new IllegalStateException("EXPORT_READBACK_MISMATCH");
        return readBack;
    }

    static String suggestedFileName(EditorialApiCombo combo) {
        String base = combo == null || combo.name.isEmpty() ? "editorial" : combo.name;
        return FileUtil.sanitizeOutputName(base + "_final.txt");
    }
}
