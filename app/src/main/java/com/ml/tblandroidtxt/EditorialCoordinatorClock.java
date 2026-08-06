package com.ml.tblandroidtxt;

/** Trusted application clock; callers cannot provide audit timestamps. */
@FunctionalInterface
public interface EditorialCoordinatorClock {
    long now();
}
