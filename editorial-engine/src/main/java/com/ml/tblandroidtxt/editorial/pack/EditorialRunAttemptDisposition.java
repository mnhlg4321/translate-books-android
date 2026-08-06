package com.ml.tblandroidtxt.editorial.pack;

/** Explicit caller intent; retry never allocates a new authoritative attempt. */
public enum EditorialRunAttemptDisposition {
    RETRY_SAME_ATTEMPT,
    NEW_AUTHORIZED_ATTEMPT
}
