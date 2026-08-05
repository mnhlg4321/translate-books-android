package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure-JVM RUN_CONTEXT_CLOSED preparation seam.
 *
 * <p>This service constructs and validates an immutable record only. It has no
 * DAO, SQLite, Android, importer, filesystem, network or composition-root
 * dependency. A ready result is suitable for a later append phase; it does
 * not mean that a record exists in storage.</p>
 */
public final class EditorialLineageRuntimeService {
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private final EditorialLineageContextResolver resolver;
    private final EditorialLineageValidator validator;

    public EditorialLineageRuntimeService(EditorialLineageContextResolver resolver) {
        this(resolver, new EditorialLineageValidator());
    }

    public EditorialLineageRuntimeService(EditorialLineageContextResolver resolver,
                                          EditorialLineageValidator validator) {
        if (resolver == null) throw new IllegalArgumentException("resolver-required");
        if (validator == null) throw new IllegalArgumentException("validator-required");
        this.resolver = resolver;
        this.validator = validator;
    }

    /** Resolves, validates and prepares; it never appends or persists. */
    public EditorialLineageCreationResult validateAndPrepare(
            EditorialLineageCreationRequest request) {
        if (request == null || request.callerSelection() == null) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.LINEAGE_CONTEXT_REQUIRED);
        }

        EditorialLineageCallerSelection selection = request.callerSelection();
        EditorialLineageAuthoritativeContext context;
        try {
            context = resolver.resolve(selection);
        } catch (RuntimeException ignored) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.RESOLVER_FAILURE);
        }
        if (context == null) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.LINEAGE_CONTEXT_REQUIRED);
        }

        EditorialLineageCreationResult assertionResult = checkAssertions(request, context);
        if (assertionResult != null) return assertionResult;

        EditorialLineageCreationResult basicResult = checkBasicContext(context, selection);
        if (basicResult != null) return basicResult;

        EditorialLineageCreationResult manifestResult = checkManifest(context);
        if (manifestResult != null) return manifestResult;

        EditorialLineageCreationResult trustResult = checkTrustAnchor(context);
        if (trustResult != null) return trustResult;

        EditorialLineageCreationResult compatibilityResult = checkCompatibility(context);
        if (compatibilityResult != null) return compatibilityResult;

        EditorialLineageNodeKind nodeKind = selection.requestedNodeKind();
        EditorialLineageParentReference parentReference = null;
        if (nodeKind == EditorialLineageNodeKind.ROOT) {
            if (!blank(selection.selectedParentRecordIdentity())
                    || !context.parentCandidates().isEmpty()) {
                return validationFailure(EditorialLineageValidationCode.ROOT_HAS_PARENT);
            }
        } else if (nodeKind == EditorialLineageNodeKind.CHILD) {
            if (blank(selection.selectedParentRecordIdentity())) {
                return validationFailure(EditorialLineageValidationCode.MISSING_PARENT);
            }
            List<EditorialLineageRecord> matches = new ArrayList<>();
            for (EditorialLineageRecord candidate : context.parentCandidates()) {
                if (candidate != null && selection.selectedParentRecordIdentity()
                        .equals(candidate.recordIdentity())) {
                    matches.add(candidate);
                }
            }
            if (matches.isEmpty()) {
                return validationFailure(EditorialLineageValidationCode.ORPHAN_LINEAGE);
            }
            if (matches.size() > 1) {
                return validationFailure(EditorialLineageValidationCode.AMBIGUOUS_PARENT);
            }
            EditorialLineageRecord parent = matches.get(0);
            parentReference = new EditorialLineageParentReference(
                    parent.recordIdentity(), parent.recordFingerprint());
        }

        EditorialLineageIdentity identity = new EditorialLineageIdentity(
                context.canonicalPackHash(),
                context.trustContext().trustedProfileId(),
                context.trustContext().trustedProfileVersion(),
                context.trustContext().canonicalProfileHash(),
                context.trustContext().machineContractFingerprint(),
                context.contractVersion(),
                context.schemaVersion(),
                context.projectIdentity(),
                context.inputScopeIdentity(),
                context.runEvaluationIdentity(),
                context.inputManifest(),
                nodeKind);
        EditorialLineageRecord record;
        try {
            record = EditorialLineageRecord.create(identity, parentReference);
        } catch (RuntimeException ignored) {
            return validationFailure(EditorialLineageValidationCode.INVALID_IDENTITY_ENCODING);
        }

        EditorialLineageValidationResult validation;
        try {
            validation = validator.validate(record, context.validationContext());
        } catch (RuntimeException ignored) {
            return validationFailure(EditorialLineageValidationCode.INVALID_IDENTITY_ENCODING);
        }
        if (validation == null || !validation.isValid()) {
            if (validation == null || validation.failureCodes().isEmpty()) {
                return validationFailure(EditorialLineageValidationCode.INVALID_IDENTITY_ENCODING);
            }
            return EditorialLineageCreationResult.validationFailure(validation.failureCodes());
        }
        return EditorialLineageCreationResult.ready(record);
    }

    private EditorialLineageCreationResult checkBasicContext(
            EditorialLineageAuthoritativeContext context,
            EditorialLineageCallerSelection selection) {
        if (selection.requestedNodeKind() == null
                || blank(context.canonicalPackHash())
                || isPlaceholder(context.canonicalPackHash())
                || !HASH.matcher(safe(context.canonicalPackHash())).matches()
                || blank(context.contractVersion())
                || isPlaceholder(context.contractVersion())
                || blank(context.schemaVersion())
                || isPlaceholder(context.schemaVersion())
                || context.validationContext() == null) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.LINEAGE_CONTEXT_REQUIRED);
        }
        if (context.validationContext().forkAllowed()) {
            return validationFailure(EditorialLineageValidationCode.FORK_NOT_ALLOWED);
        }
        if (blank(selection.projectSelector()) || isPlaceholder(selection.projectSelector())) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.PROJECT_IDENTITY_REQUIRED);
        }
        if (blank(selection.inputScopeSelector()) || isPlaceholder(selection.inputScopeSelector())) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.INPUT_SCOPE_REQUIRED);
        }
        if (blank(selection.runEvaluationSelector()) || isPlaceholder(selection.runEvaluationSelector())) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.RUN_EVALUATION_IDENTITY_REQUIRED);
        }
        if (blank(context.projectIdentity()) || isPlaceholder(context.projectIdentity())) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.PROJECT_IDENTITY_REQUIRED);
        }
        if (blank(context.inputScopeIdentity()) || isPlaceholder(context.inputScopeIdentity())) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.INPUT_SCOPE_REQUIRED);
        }
        if (blank(context.runEvaluationIdentity()) || isPlaceholder(context.runEvaluationIdentity())
                || context.runContextState() != EditorialLineageRunContextState.CLOSED) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.RUN_EVALUATION_IDENTITY_REQUIRED);
        }
        return null;
    }

    private EditorialLineageCreationResult checkManifest(
            EditorialLineageAuthoritativeContext context) {
        EditorialLineageInputManifest manifest = context.inputManifest();
        if (manifest == null || blank(manifest.manifestVersion())
                || context.requiredInputRoles().isEmpty()
                || manifest.entries().isEmpty()) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.INPUT_MANIFEST_INCOMPLETE);
        }
        Set<String> roleOrdinals = new HashSet<>();
        Set<String> roles = new HashSet<>();
        for (EditorialLineageInputEntry entry : manifest.entries()) {
            if (entry == null || blank(entry.role()) || isPlaceholder(entry.role())
                    || !HASH.matcher(safe(entry.inputHash())).matches()
                    || entry.ordinal() < 0 || entry.byteCount() < 0 || entry.itemCount() < 0
                    || !roleOrdinals.add(entry.role() + "\u0000" + entry.ordinal())) {
                return EditorialLineageCreationResult.failure(
                        EditorialLineageCreationCode.INPUT_MANIFEST_INCOMPLETE);
            }
            roles.add(entry.role());
        }
        for (String requiredRole : context.requiredInputRoles()) {
            if (blank(requiredRole) || isPlaceholder(requiredRole) || !roles.contains(requiredRole)) {
                return EditorialLineageCreationResult.failure(
                        EditorialLineageCreationCode.INPUT_MANIFEST_INCOMPLETE);
            }
        }
        return null;
    }

    private EditorialLineageCreationResult checkTrustAnchor(
            EditorialLineageAuthoritativeContext context) {
        EditorialLineageTrustContext trust = context.trustContext();
        if (trust == null || blank(trust.trustedProfileId())
                || blank(trust.trustedProfileVersion())
                || !HASH.matcher(safe(trust.canonicalProfileHash())).matches()
                || !HASH.matcher(safe(trust.machineContractFingerprint())).matches()
                || isPlaceholder(trust.trustedProfileId())
                || isPlaceholder(trust.trustedProfileVersion())) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.TRUSTED_PROFILE_CONTEXT_MISMATCH);
        }
        java.util.Optional<EditorialLineageTrustContext> expected =
                context.validationContext().expectedTrustContext();
        if (expected != null && expected.isPresent() && !trust.equals(expected.get())) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.TRUSTED_PROFILE_CONTEXT_MISMATCH);
        }
        return null;
    }

    private EditorialLineageCreationResult checkCompatibility(
            EditorialLineageAuthoritativeContext context) {
        EditorialLineageCompatibilityEvidence evidence = context.compatibilityEvidence();
        if (evidence == null || blank(evidence.evidenceIdentity())
                || isPlaceholder(evidence.evidenceIdentity())
                || evidence.status() != EditorialLineageCompatibilityStatus.COMPATIBLE) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.COMPATIBILITY_CONTEXT_MISMATCH);
        }
        return null;
    }

    private EditorialLineageCreationResult checkAssertions(
            EditorialLineageCreationRequest request,
            EditorialLineageAuthoritativeContext context) {
        EditorialLineageCallerAssertions assertions = request.callerAssertions();
        EditorialLineageTrustContext trust = context.trustContext();
        EditorialLineageCompatibilityEvidence compatibility = context.compatibilityEvidence();
        String manifestFingerprint = context.inputManifest() == null ? null
                : EditorialLineageCanonicalizer.inputManifestFingerprint(context.inputManifest());
        if (!matches(assertions.canonicalPackHashAssertion(), context.canonicalPackHash())
                || !matches(assertions.trustedProfileIdAssertion(), trust == null ? null : trust.trustedProfileId())
                || !matches(assertions.trustedProfileVersionAssertion(), trust == null ? null : trust.trustedProfileVersion())
                || !matches(assertions.canonicalProfileHashAssertion(), trust == null ? null : trust.canonicalProfileHash())
                || !matches(assertions.machineContractFingerprintAssertion(), trust == null ? null : trust.machineContractFingerprint())
                || !matches(assertions.contractVersionAssertion(), context.contractVersion())
                || !matches(assertions.schemaVersionAssertion(), context.schemaVersion())
                || !matches(assertions.compatibilityEvidenceIdentityAssertion(), compatibility == null ? null : compatibility.evidenceIdentity())
                || !matches(assertions.compatibilityStatusAssertion(), compatibility == null ? null : compatibility.status())
                || !matches(assertions.inputManifestFingerprintAssertion(), manifestFingerprint)) {
            return EditorialLineageCreationResult.failure(
                    EditorialLineageCreationCode.CALLER_ASSERTION_MISMATCH);
        }
        return null;
    }

    private static EditorialLineageCreationResult validationFailure(
            EditorialLineageValidationCode code) {
        return EditorialLineageCreationResult.validationFailure(List.of(code));
    }

    private static boolean matches(Object assertion, Object authoritative) {
        return assertion == null || assertion.equals(authoritative);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isPlaceholder(String value) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("unknown") || normalized.equals("placeholder")
                || normalized.equals("empty") || normalized.equals("null")
                || normalized.equals("n/a") || normalized.equals("na");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
