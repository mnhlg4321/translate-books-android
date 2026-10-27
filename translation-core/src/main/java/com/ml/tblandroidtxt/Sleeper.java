package com.ml.tblandroidtxt;

/** Sleep port for retry backoff; tests use an implementation that returns at once. */
public interface Sleeper {
    void sleep(long millis) throws InterruptedException;

    Sleeper THREAD = Thread::sleep;
}
