package com.ml.tblandroidtxt;

import android.content.Context;

/** Routes ObservabilityLog lines into the app log store. */
final class AndroidObservabilitySink implements ObservabilityLog.Sink {
    private final Context appContext;

    AndroidObservabilitySink(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override public void append(String line) {
        LogStore.append(appContext, line);
    }
}
