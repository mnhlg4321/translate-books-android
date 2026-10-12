package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Complete immutable scope snapshot and its derived v1 identity/fingerprints. */
public final class EditorialInputScopeSnapshot {
    public static final String MANIFEST_FINGERPRINT_DOMAIN =
            "EDITORIAL_INPUT_SCOPE_MANIFEST_FINGERPRINT_V1";
    public static final String IDENTITY_DOMAIN = "EDITORIAL_INPUT_SCOPE_IDENTITY_V1";

    private final String projectRevisionIdentity;
    private final String scopeCanonicalVersion;
    private final String canonicalScopeKey;
    private final EditorialRequiredInputRoleContract requiredRoleContract;
    private final String manifestVersion;
    private final List<EditorialInputScopeSnapshotEntry> entries;
    private final String manifestCanonical;
    private final String manifestFingerprint;
    private final String canonicalProjection;
    private final String scopeSnapshotIdentity;

    public EditorialInputScopeSnapshot(
            String projectRevisionIdentity,
            String scopeCanonicalVersion,
            String canonicalScopeKey,
            EditorialRequiredInputRoleContract requiredRoleContract,
            String manifestVersion,
            List<EditorialInputScopeSnapshotEntry> entries) {
        this.projectRevisionIdentity = EditorialIdentityText.sha256(projectRevisionIdentity,
                "project revision identity");
        this.scopeCanonicalVersion = EditorialIdentityText.nonEmpty(scopeCanonicalVersion,
                "scope canonical version");
        this.canonicalScopeKey = EditorialScopeKeyCanonicalizer.normalize(canonicalScopeKey);
        this.requiredRoleContract = Objects.requireNonNull(requiredRoleContract,
                "required-role contract");
        this.manifestVersion = EditorialIdentityText.nonEmpty(manifestVersion, "manifest version");
        this.entries = canonicalEntries(entries, requiredRoleContract);
        this.manifestCanonical = buildManifestCanonical();
        this.manifestFingerprint = EditorialIdentityText.hash(
                MANIFEST_FINGERPRINT_DOMAIN, manifestCanonical);
        this.canonicalProjection = buildCanonicalProjection();
        this.scopeSnapshotIdentity = EditorialIdentityText.hash(
                IDENTITY_DOMAIN, canonicalProjection);
    }

    public String projectRevisionIdentity() { return projectRevisionIdentity; }
    public String scopeCanonicalVersion() { return scopeCanonicalVersion; }
    public String canonicalScopeKey() { return canonicalScopeKey; }
    public EditorialRequiredInputRoleContract requiredRoleContract() { return requiredRoleContract; }
    public String requiredRolesCanonical() { return requiredRoleContract.canonical(); }
    public String requiredRolesFingerprint() { return requiredRoleContract.fingerprint(); }
    public String manifestVersion() { return manifestVersion; }
    public List<EditorialInputScopeSnapshotEntry> entries() { return entries; }
    public String manifestCanonical() { return manifestCanonical; }
    public String manifestFingerprint() { return manifestFingerprint; }
    public String canonicalProjection() { return canonicalProjection; }
    public String scopeSnapshotIdentity() { return scopeSnapshotIdentity; }

    private static List<EditorialInputScopeSnapshotEntry> canonicalEntries(
            List<EditorialInputScopeSnapshotEntry> source,
            EditorialRequiredInputRoleContract roleContract) {
        if (source == null || source.isEmpty()) throw new IllegalArgumentException("input manifest is empty");
        ArrayList<EditorialInputScopeSnapshotEntry> ordered = new ArrayList<>(source);
        Set<String> keys = new HashSet<>();
        Set<String> presentRoles = new HashSet<>();
        for (EditorialInputScopeSnapshotEntry entry : ordered) {
            String role = EditorialIdentityText.nonEmpty(entry.role(), "input role");
            if (!roleContract.contains(role)) {
                throw new IllegalArgumentException("input role is not in reviewed contract: " + role);
            }
            if (entry.ordinal() == null || entry.byteCount() == null || entry.itemCount() == null) {
                throw new IllegalArgumentException("input ordinal/byte count/item count are required");
            }
            if (entry.ordinal() < 0 || entry.byteCount() < 0 || entry.itemCount() < 0) {
                throw new IllegalArgumentException("input ordinal/count cannot be negative");
            }
            EditorialIdentityText.sha256(entry.inputSha256(), "input SHA-256");
            String key = role + "\u0000" + entry.ordinal();
            if (!keys.add(key)) throw new IllegalArgumentException("duplicate input role/ordinal");
            presentRoles.add(role);
        }
        if (!presentRoles.containsAll(roleContract.requiredRoleSet())) {
            throw new IllegalArgumentException("input manifest is missing a required role");
        }
        ordered.sort(Comparator.comparing(EditorialInputScopeSnapshotEntry::role)
                .thenComparingLong(e -> e.ordinal()));
        return List.copyOf(ordered);
    }

    private String buildManifestCanonical() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("manifestVersion", manifestVersion);
        List<Object> canonicalEntries = new ArrayList<>();
        for (EditorialInputScopeSnapshotEntry entry : entries) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", entry.role());
            item.put("ordinal", BigDecimal.valueOf(entry.ordinal()));
            item.put("inputSha256", entry.inputSha256());
            item.put("byteCount", BigDecimal.valueOf(entry.byteCount()));
            item.put("itemCount", BigDecimal.valueOf(entry.itemCount()));
            canonicalEntries.add(item);
        }
        projection.put("entries", canonicalEntries);
        return EditorialCanonicalJson.canonicalize(projection);
    }

    private String buildCanonicalProjection() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("scopeCanonicalVersion", scopeCanonicalVersion);
        projection.put("projectRevisionIdentity", projectRevisionIdentity);
        projection.put("canonicalScopeKey", canonicalScopeKey);
        projection.put("requiredRolesCanonical", requiredRoleContract.canonical());
        projection.put("requiredRolesFingerprint", requiredRoleContract.fingerprint());
        projection.put("manifestVersion", manifestVersion);
        projection.put("manifestFingerprint", manifestFingerprint);
        return EditorialCanonicalJson.canonicalize(projection);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialInputScopeSnapshot that)) return false;
        return scopeSnapshotIdentity.equals(that.scopeSnapshotIdentity)
                && canonicalProjection.equals(that.canonicalProjection)
                && entries.equals(that.entries);
    }

    @Override public int hashCode() {
        return Objects.hash(scopeSnapshotIdentity, canonicalProjection, entries);
    }
}
