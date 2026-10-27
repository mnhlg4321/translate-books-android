package com.ml.tblandroidtxt;

import android.content.Context;
import android.util.Log;

/** Run log into the app log store and logcat. */
final class AndroidRuntimeLog implements RuntimeLog {
    private final Context context;

    AndroidRuntimeLog(Context context) { this.context = context; }

    @Override public void append(String message) { LogStore.append(context, message); }

    @Override public void error(String tag, String message, Throwable error) { Log.e(tag, message, error); }

    @Override public String stackTrace(Throwable error) { return Log.getStackTraceString(error); }
}
