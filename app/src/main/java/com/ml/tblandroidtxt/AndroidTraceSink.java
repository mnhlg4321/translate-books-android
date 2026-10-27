package com.ml.tblandroidtxt;

import android.content.Context;

/** Routes engine traces to DebugTraceStore with the application context. */
final class AndroidTraceSink implements TraceSink {
    private final Context appContext;

    AndroidTraceSink(Context context) {
        this.appContext = context == null ? null : context.getApplicationContext();
    }

    @Override public void request(String phase, int chunkIndexOneBased, PromptPair prompt, AppSettings settings) {
        DebugTraceStore.request(appContext, phase, chunkIndexOneBased, prompt, settings);
    }

    @Override public void response(String phase, int chunkIndexOneBased, OpenAICompatibleClient.ChatResult result, String extracted) {
        DebugTraceStore.response(appContext, phase, chunkIndexOneBased, result, extracted);
    }

    @Override public void error(String phase, int chunkIndexOneBased, Throwable error) {
        DebugTraceStore.error(appContext, phase, chunkIndexOneBased, error);
    }
}
