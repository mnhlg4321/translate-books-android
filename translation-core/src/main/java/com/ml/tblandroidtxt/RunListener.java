package com.ml.tblandroidtxt;

/** Receives what the run loop used to broadcast and show in the foreground notification. */
public interface RunListener {
    void onSnapshot(RunSnapshot snapshot);

    void onNotification(String text, int done, int total);

    /** The run is over: release it, drop the foreground notification and stop the service (the old tail sequence). */
    void onRunEnded();

    /** The run ended because it was cancelled; the app keeps the former finishCancelled body. */
    void onCancelledEnd(String message);
}
