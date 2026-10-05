package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;

/** Sends one request of the edit/check flow. Implementations never throw for a provider problem: they return a failure. */
public interface EditorialApiProvider {
    /** @param model empty = the model of the app settings */
    EditorialApiFlow.StepResponse call(EditorialApiFlow.Request request, String model, int maxOutputTokens, long timeoutMillis);

    /** Cancels the call in flight, if any. */
    void cancel();
}
