package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Fail-closed pure-JVM validator for exact-parent lineage records. */
public final class EditorialLineageValidator {
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:/@+\\-]{0,255}");

    public EditorialLineageValidationResult validate(EditorialLineageRecord record,
                                                      EditorialLineageValidationContext context) {
        if (record == null) return invalid(EditorialLineageValidationCode.INPUT_NULL, "record", "record-required");
        if (context == null) return invalid(EditorialLineageValidationCode.CONTEXT_NULL, "context", "context-required");
        ArrayList<EditorialLineageValidationIssue> issues = new ArrayList<>();
        validateNode(record, context, new HashSet<>(), true, issues);
        return issues.isEmpty() ? EditorialLineageValidationResult.valid()
                : EditorialLineageValidationResult.invalid(issues);
    }

    private void validateNode(EditorialLineageRecord record,
                              EditorialLineageValidationContext context,
                              Set<String> visiting,
                              boolean candidate,
                              List<EditorialLineageValidationIssue> issues) {
        if (record == null) {
            add(issues, EditorialLineageValidationCode.ORPHAN_LINEAGE, "parent", "parent-record-null");
            return;
        }
        String visitKey = record.recordIdentity();
        if (!visiting.add(visitKey)) {
            add(issues, EditorialLineageValidationCode.CYCLE_DETECTED, "parent", "record-cycle");
            return;
        }

        validateFields(record, issues);
        validateDeclaredFingerprint(record, issues);
        if (candidate) {
            validateTrustContext(record, context, issues);
            validateDuplicateAndReparent(record, context, issues);
        }

        EditorialLineageParentReference parentReference = record.parentReference();
        if (record.nodeKind() == EditorialLineageNodeKind.ROOT) {
            if (parentReference != null) {
                add(issues, EditorialLineageValidationCode.ROOT_HAS_PARENT, "parent", "root-parent-forbidden");
            }
        } else if (parentReference == null) {
            add(issues, EditorialLineageValidationCode.MISSING_PARENT, "parent", "child-parent-required");
        } else {
            validateParent(record, parentReference, context, visiting, issues);
        }
        visiting.remove(visitKey);
    }

    private void validateFields(EditorialLineageRecord record,
                                List<EditorialLineageValidationIssue> issues) {
        EditorialLineageIdentity identity = record.identity();
        validateHash(identity.canonicalPackHash(), "identity.canonicalPackHash", issues);
        validateToken(identity.trustedProfileId(), "identity.trustedProfileId", issues);
        validateToken(identity.trustedProfileVersion(), "identity.trustedProfileVersion", issues);
        validateHash(identity.canonicalProfileHash(), "identity.canonicalProfileHash", issues);
        validateHash(identity.machineContractFingerprint(), "identity.machineContractFingerprint", issues);
        validateToken(identity.contractVersion(), "identity.contractVersion", issues);
        validateToken(identity.schemaVersion(), "identity.schemaVersion", issues);
        validateToken(identity.projectIdentity(), "identity.projectIdentity", issues);
        validateToken(identity.inputScopeIdentity(), "identity.inputScopeIdentity", issues);
        validateToken(identity.runEvaluationIdentity(), "identity.runEvaluationIdentity", issues);
        validateToken(identity.inputManifest().manifestVersion(), "identity.inputManifest.manifestVersion", issues);
        if (identity.inputManifest().entries().isEmpty()) {
            add(issues, EditorialLineageValidationCode.INVALID_MANIFEST,
                    "identity.inputManifest.entries", "manifest-empty");
        }
        Set<String> entryKeys = new HashSet<>();
        for (int i = 0; i < identity.inputManifest().entries().size(); i++) {
            EditorialLineageInputEntry entry = identity.inputManifest().entries().get(i);
            String path = "identity.inputManifest.entries[" + i + "]";
            validateToken(entry.role(), path + ".role", issues);
            validateHash(entry.inputHash(), path + ".inputHash", issues);
            if (entry.ordinal() < 0 || entry.byteCount() < 0 || entry.itemCount() < 0) {
                add(issues, EditorialLineageValidationCode.INVALID_MANIFEST, path, "negative-count");
            }
            if (!entryKeys.add(entry.role() + "\u0000" + entry.ordinal())) {
                add(issues, EditorialLineageValidationCode.INVALID_MANIFEST, path, "duplicate-role-ordinal");
            }
        }
        validateHash(record.inputManifestFingerprint(), "fingerprint.inputManifestFingerprint", issues);
        validateHash(record.recordIdentity(), "fingerprint.recordIdentity", issues);
        validateHash(record.recordFingerprint(), "fingerprint.recordFingerprint", issues);
        if (record.parentReference() != null) {
            validateHash(record.parentReference().parentRecordIdentity(), "parent.parentRecordIdentity", issues);
            validateHash(record.parentReference().parentRecordFingerprint(), "parent.parentRecordFingerprint", issues);
        }
    }

    private void validateDeclaredFingerprint(EditorialLineageRecord record,
                                             List<EditorialLineageValidationIssue> issues) {
        try {
            EditorialLineageFingerprint computed = record.computedFingerprint();
            if (!computed.inputManifestFingerprint().equals(record.inputManifestFingerprint())) {
                add(issues, EditorialLineageValidationCode.IDENTITY_FINGERPRINT_MISMATCH,
                        "fingerprint.inputManifestFingerprint", "computed-mismatch");
            }
            if (!computed.recordIdentity().equals(record.recordIdentity())) {
                add(issues, EditorialLineageValidationCode.IDENTITY_FINGERPRINT_MISMATCH,
                        "fingerprint.recordIdentity", "computed-mismatch");
            }
            if (!computed.recordFingerprint().equals(record.recordFingerprint())) {
                add(issues, EditorialLineageValidationCode.IDENTITY_FINGERPRINT_MISMATCH,
                        "fingerprint.recordFingerprint", "computed-mismatch");
            }
        } catch (RuntimeException e) {
            add(issues, EditorialLineageValidationCode.INVALID_IDENTITY_ENCODING,
                    "record", "canonicalization-failed");
        }
    }

    private void validateTrustContext(EditorialLineageRecord record,
                                     EditorialLineageValidationContext context,
                                     List<EditorialLineageValidationIssue> issues) {
        Optional<EditorialLineageTrustContext> expected = context.expectedTrustContext();
        if (expected == null || expected.isEmpty()) return;
        EditorialLineageTrustContext trust = expected.get();
        EditorialLineageIdentity identity = record.identity();
        if (!trust.trustedProfileId().equals(identity.trustedProfileId())
                || !trust.trustedProfileVersion().equals(identity.trustedProfileVersion())
                || !trust.canonicalProfileHash().equals(identity.canonicalProfileHash())) {
            add(issues, EditorialLineageValidationCode.CROSS_PROFILE,
                    "identity.profile", "trusted-profile-mismatch");
        }
        if (!trust.machineContractFingerprint().equals(identity.machineContractFingerprint())) {
            add(issues, EditorialLineageValidationCode.STALE_PROFILE,
                    "identity.machineContractFingerprint", "machine-fingerprint-mismatch");
        }
    }

    private void validateDuplicateAndReparent(EditorialLineageRecord record,
                                              EditorialLineageValidationContext context,
                                              List<EditorialLineageValidationIssue> issues) {
        List<EditorialLineageRecord> sameIdentity = safeRecords(
                context.findByRecordIdentity(record.recordIdentity()));
        if (sameIdentity.size() > 1 || !sameIdentity.isEmpty()) {
            add(issues, EditorialLineageValidationCode.DUPLICATE_LINEAGE,
                    "recordIdentity", "identity-already-indexed");
            for (EditorialLineageRecord existing : sameIdentity) {
                if (!existing.fingerprint().equals(record.fingerprint())) {
                    add(issues, EditorialLineageValidationCode.IDENTITY_FINGERPRINT_MISMATCH,
                            "recordFingerprint", "same-identity-different-fingerprint");
                    break;
                }
            }
        }

        List<EditorialLineageRecord> sameRun = safeRecords(
                context.findByRunEvaluationIdentity(record.identity().runEvaluationIdentity()));
        for (EditorialLineageRecord existing : sameRun) {
            if (existing.recordIdentity().equals(record.recordIdentity())) {
                if (!Objects.equals(existing.parentReference(), record.parentReference())) {
                    add(issues, EditorialLineageValidationCode.REPARENT_ATTEMPT,
                            "parent", "run-identity-reparented");
                    add(issues, EditorialLineageValidationCode.PARENT_MISMATCH,
                            "parent", "run-identity-parent-mismatch");
                }
                continue;
            }
            if (!sameLineageContext(existing.identity(), record.identity())) {
                addContextMismatch(existing.identity(), record.identity(), issues);
            } else if (!existing.identity().inputManifest().equals(record.identity().inputManifest())) {
                add(issues, EditorialLineageValidationCode.MANIFEST_MISMATCH,
                        "identity.inputManifest", "same-run-input-manifest-changed");
            } else if (!Objects.equals(existing.parentReference(), record.parentReference())) {
                add(issues, EditorialLineageValidationCode.REPARENT_ATTEMPT,
                        "parent", "stable-node-parent-changed");
            } else {
                add(issues, EditorialLineageValidationCode.LINEAGE_IDENTITY_REVIEW_REQUIRED,
                        "runEvaluationIdentity", "tuple-does-not-separate-records");
            }
        }
    }

    private void validateParent(EditorialLineageRecord child,
                                EditorialLineageParentReference reference,
                                EditorialLineageValidationContext context,
                                Set<String> visiting,
                                List<EditorialLineageValidationIssue> issues) {
        if (reference.parentRecordIdentity().equals(child.recordIdentity())) {
            add(issues, EditorialLineageValidationCode.SELF_PARENT, "parent", "self-parent");
            return;
        }
        List<EditorialLineageRecord> candidates = safeRecords(
                context.findByRecordIdentity(reference.parentRecordIdentity()));
        if (candidates.isEmpty()) {
            add(issues, EditorialLineageValidationCode.MISSING_PARENT, "parent", "parent-not-indexed");
            return;
        }
        if (candidates.size() > 1) {
            add(issues, EditorialLineageValidationCode.AMBIGUOUS_PARENT,
                    "parent", "multiple-parent-records");
            return;
        }
        EditorialLineageRecord parent = candidates.get(0);
        if (!reference.parentRecordFingerprint().equals(parent.recordFingerprint())) {
            add(issues, EditorialLineageValidationCode.PARENT_MISMATCH,
                    "parent.parentRecordFingerprint", "parent-fingerprint-mismatch");
        }
        addContextMismatch(parent.identity(), child.identity(), issues);

        EditorialLineageValidationResult parentResult = validateStoredParent(parent, context, visiting);
        if (!parentResult.isValid()) {
            for (EditorialLineageValidationIssue issue : parentResult.issues()) {
                if (issue.code() == EditorialLineageValidationCode.CYCLE_DETECTED
                        || issue.code() == EditorialLineageValidationCode.SELF_PARENT) {
                    add(issues, issue.code(), "parent." + issue.path(), issue.detail());
                }
            }
            add(issues, EditorialLineageValidationCode.ORPHAN_LINEAGE,
                    "parent", "parent-chain-invalid");
        }

        if (!context.forkAllowed()) {
            for (EditorialLineageRecord existingChild : safeRecords(
                    context.findChildrenByParentIdentity(parent.recordIdentity()))) {
                if (!existingChild.recordIdentity().equals(child.recordIdentity())) {
                    add(issues, EditorialLineageValidationCode.FORK_NOT_ALLOWED,
                            "parent", "multiple-children-forbidden");
                    break;
                }
            }
        }
    }

    private EditorialLineageValidationResult validateStoredParent(
            EditorialLineageRecord parent,
            EditorialLineageValidationContext context,
            Set<String> visiting) {
        ArrayList<EditorialLineageValidationIssue> issues = new ArrayList<>();
        validateNode(parent, context, visiting, false, issues);
        return issues.isEmpty() ? EditorialLineageValidationResult.valid()
                : EditorialLineageValidationResult.invalid(issues);
    }

    private static void addContextMismatch(EditorialLineageIdentity parent,
                                           EditorialLineageIdentity child,
                                           List<EditorialLineageValidationIssue> issues) {
        if (!parent.canonicalPackHash().equals(child.canonicalPackHash())) {
            add(issues, EditorialLineageValidationCode.CROSS_PACK, "parent.canonicalPackHash", "pack-mismatch");
        }
        if (!parent.trustedProfileId().equals(child.trustedProfileId())
                || !parent.trustedProfileVersion().equals(child.trustedProfileVersion())
                || !parent.canonicalProfileHash().equals(child.canonicalProfileHash())) {
            add(issues, EditorialLineageValidationCode.CROSS_PROFILE, "parent.profile", "profile-mismatch");
        }
        if (!parent.machineContractFingerprint().equals(child.machineContractFingerprint())
                && parent.trustedProfileId().equals(child.trustedProfileId())
                && parent.trustedProfileVersion().equals(child.trustedProfileVersion())
                && parent.canonicalProfileHash().equals(child.canonicalProfileHash())) {
            add(issues, EditorialLineageValidationCode.STALE_PROFILE,
                    "parent.machineContractFingerprint", "machine-fingerprint-mismatch");
        }
        if (!parent.contractVersion().equals(child.contractVersion())
                || !parent.schemaVersion().equals(child.schemaVersion())) {
            add(issues, EditorialLineageValidationCode.MANIFEST_MISMATCH,
                    "parent.contractSchema", "contract-schema-mismatch");
        }
        if (!parent.projectIdentity().equals(child.projectIdentity())) {
            add(issues, EditorialLineageValidationCode.CROSS_PROJECT, "parent.projectIdentity", "project-mismatch");
        }
        if (!parent.inputScopeIdentity().equals(child.inputScopeIdentity())) {
            add(issues, EditorialLineageValidationCode.CROSS_INPUT_SCOPE,
                    "parent.inputScopeIdentity", "input-scope-mismatch");
        }
        if (!parent.inputManifest().equals(child.inputManifest())) {
            add(issues, EditorialLineageValidationCode.MANIFEST_MISMATCH,
                    "parent.inputManifest", "source-input-manifest-mismatch");
        }
    }

    private static boolean sameLineageContext(EditorialLineageIdentity left,
                                               EditorialLineageIdentity right) {
        return left.canonicalPackHash().equals(right.canonicalPackHash())
                && left.trustedProfileId().equals(right.trustedProfileId())
                && left.trustedProfileVersion().equals(right.trustedProfileVersion())
                && left.canonicalProfileHash().equals(right.canonicalProfileHash())
                && left.machineContractFingerprint().equals(right.machineContractFingerprint())
                && left.contractVersion().equals(right.contractVersion())
                && left.schemaVersion().equals(right.schemaVersion())
                && left.projectIdentity().equals(right.projectIdentity())
                && left.inputScopeIdentity().equals(right.inputScopeIdentity());
    }

    private static void validateToken(String value, String path,
                                      List<EditorialLineageValidationIssue> issues) {
        if (value == null || !TOKEN.matcher(value).matches() || containsBom(value)) {
            add(issues, EditorialLineageValidationCode.INVALID_IDENTITY_ENCODING, path, "token-invalid");
        }
        try {
            EditorialLineageCanonicalizer.strictUtf8(value);
        } catch (RuntimeException e) {
            add(issues, EditorialLineageValidationCode.INVALID_IDENTITY_ENCODING, path, "utf8-invalid");
        }
    }

    private static void validateHash(String value, String path,
                                     List<EditorialLineageValidationIssue> issues) {
        if (value == null || !HASH.matcher(value).matches()) {
            add(issues, EditorialLineageValidationCode.INVALID_HASH, path, "sha256-lowercase-required");
        }
    }

    private static boolean containsBom(String value) {
        return value != null && value.indexOf('\ufeff') >= 0;
    }

    private static List<EditorialLineageRecord> safeRecords(List<EditorialLineageRecord> records) {
        if (records == null) return List.of();
        return List.copyOf(records);
    }

    private static EditorialLineageValidationResult invalid(EditorialLineageValidationCode code,
                                                            String path,
                                                            String detail) {
        return EditorialLineageValidationResult.invalid(List.of(
                new EditorialLineageValidationIssue(code, path, detail)));
    }

    private static void add(List<EditorialLineageValidationIssue> issues,
                            EditorialLineageValidationCode code,
                            String path,
                            String detail) {
        issues.add(new EditorialLineageValidationIssue(code, path, detail));
    }
}
