package com.ml.tblandroidtxt;

/** Privacy-safe request/response trace port (hashes and sizes only, never content). */
public interface TraceSink {
    void request(String phase, int chunkIndexOneBased, PromptPair prompt, AppSettings settings);

    void response(String phase, int chunkIndexOneBased, OpenAICompatibleClient.ChatResult result, String extracted);

    void error(String phase, int chunkIndexOneBased, Throwable error);

    /** No tracing, as when the engine used to be built without a Context. */
    TraceSink NONE = new TraceSink() {
        @Override public void request(String phase, int chunkIndexOneBased, PromptPair prompt, AppSettings settings) {}
        @Override public void response(String phase, int chunkIndexOneBased, OpenAICompatibleClient.ChatResult result, String extracted) {}
        @Override public void error(String phase, int chunkIndexOneBased, Throwable error) {}
    };
}
