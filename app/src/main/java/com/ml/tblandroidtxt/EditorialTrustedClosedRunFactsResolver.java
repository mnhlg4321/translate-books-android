package com.ml.tblandroidtxt;

import java.util.Optional;

/** Trust boundary for closing and resolving a run; never populated from caller assertions. */
@FunctionalInterface
public interface EditorialTrustedClosedRunFactsResolver {
    Optional<EditorialTrustedClosedRunFacts> resolve(
            EditorialPackCompatibilityEvaluation evaluation,
            String packContractVersion,
            String packSchemaVersion);
}
