package com.ml.tblandroidtxt;

/** Read-only port for one explicit immutable closure event selector. */
@FunctionalInterface
public interface EditorialRunClosureEventResolver {
    EditorialClosureEventResolutionResult resolve(String closureEventSelector);
}
