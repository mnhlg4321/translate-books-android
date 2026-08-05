package com.ml.tblandroidtxt.editorial.pack;

import java.util.Objects;

/** Immutable root/child lineage record with no persistence or runtime behavior. */
public final class EditorialLineageRecord {
    private final EditorialLineageIdentity identity;
    private final EditorialLineageParentReference parentReference;
    private final EditorialLineageFingerprint fingerprint;

    private EditorialLineageRecord(EditorialLineageIdentity identity,
                                   EditorialLineageParentReference parentReference,
                                   EditorialLineageFingerprint fingerprint) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.parentReference = parentReference;
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
    }

    public static EditorialLineageRecord create(EditorialLineageIdentity identity,
                                                EditorialLineageParentReference parentReference) {
        return new EditorialLineageRecord(identity, parentReference,
                EditorialLineageFingerprint.derive(identity, parentReference));
    }

    /** Read-only construction seam for validating serialized/tampered declarations. */
    public static EditorialLineageRecord fromDeclared(
            EditorialLineageIdentity identity,
            EditorialLineageParentReference parentReference,
            EditorialLineageFingerprint declaredFingerprint) {
        return new EditorialLineageRecord(identity, parentReference, declaredFingerprint);
    }

    public EditorialLineageIdentity identity() { return identity; }
    public EditorialLineageNodeKind nodeKind() { return identity.nodeKind(); }
    public EditorialLineageParentReference parentReference() { return parentReference; }
    public EditorialLineageFingerprint fingerprint() { return fingerprint; }
    public String recordIdentity() { return fingerprint.recordIdentity(); }
    public String recordFingerprint() { return fingerprint.recordFingerprint(); }
    public String inputManifestFingerprint() { return fingerprint.inputManifestFingerprint(); }

    public EditorialLineageFingerprint computedFingerprint() {
        return EditorialLineageFingerprint.derive(identity, parentReference);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialLineageRecord that)) return false;
        return identity.equals(that.identity)
                && Objects.equals(parentReference, that.parentReference)
                && fingerprint.equals(that.fingerprint);
    }

    @Override public int hashCode() {
        return Objects.hash(identity, parentReference, fingerprint);
    }
}
