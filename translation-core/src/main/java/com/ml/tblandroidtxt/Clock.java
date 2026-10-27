package com.ml.tblandroidtxt;

/** Wall-clock port for the run loop and the engine, so JVM tests can drive time. */
public interface Clock {
    long now();

    Clock SYSTEM = System::currentTimeMillis;
}
