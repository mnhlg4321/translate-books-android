package com.ml.tblandroidtxt;

/**
 * A failure that belongs to one chapter only (its final integrity check failed, or a chunk cannot be split any further). A chapter-set
 * run marks that chapter's job as failed and goes on with the next chapter. Any other failure (cost limit, delivery unknown, authentication,
 * quota, network) stays a plain exception and stops the whole queue.
 */
public final class ChapterLocalException extends IllegalStateException {
    public ChapterLocalException(String message) { super(message); }

    public ChapterLocalException(String message, Throwable cause) { super(message, cause); }
}
