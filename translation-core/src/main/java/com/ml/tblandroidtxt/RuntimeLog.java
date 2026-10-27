package com.ml.tblandroidtxt;

/** Run log port: the app log store plus platform error logging. */
public interface RuntimeLog {
    void append(String message);

    void error(String tag, String message, Throwable error);

    String stackTrace(Throwable error);
}
