package com.ml.tblandroidtxt;

import android.content.Context;
import android.net.Uri;

/** DocumentIO over FileUtil/SAF; output resolution is delegated to the service, which owns deferred-output handling. */
final class AndroidDocumentIO implements DocumentIO {
    private final Context context;
    private final TranslatorService service;

    AndroidDocumentIO(TranslatorService service) {
        this.context = service;
        this.service = service;
    }

    @Override public String readText(String ref) throws Exception { return FileUtil.readText(context, Uri.parse(ref)); }

    @Override public String displayName(String ref) { return FileUtil.displayName(context, Uri.parse(ref)); }

    @Override public void writeTextVerified(String ref, String text) throws Exception { FileUtil.writeTextVerified(context, Uri.parse(ref), text); }

    @Override public void writeText(String ref, String text) throws Exception { FileUtil.writeText(context, Uri.parse(ref), text); }

    @Override public String resolveOutput(long jobId, String ref) throws Exception {
        return service.materializeOutput(jobId, ref == null ? null : Uri.parse(ref)).toString();
    }
}
