package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable result of trusted-profile selection followed by the G2-A
 * compatibility evaluator. It carries profile identity separately from the
 * evaluator facts and never represents certification, activation or execution.
 */
public final class EditorialCompatibilityEvaluationResult {
    private final EditorialPackCompatibilityClass outcome;
    private final EditorialPackCompatibilityClass requiredClass;
    private final EditorialCompatibilityReasonCode reasonCode;
    private final boolean blocked;
    private final List<EditorialPackIntegrityResult.Issue> issues;
    private final Set<String> missingCapabilities;
    private final String canonicalPackHash;
    private final EditorialEngineContractProfile trustedProfile;
    private final EditorialEngineProfile evaluatorProfile;

    EditorialCompatibilityEvaluationResult(EditorialPackCompatibilityClass outcome,
                                            EditorialPackCompatibilityClass requiredClass,
                                            EditorialCompatibilityReasonCode reasonCode,
                                            boolean blocked,
                                            List<EditorialPackIntegrityResult.Issue> issues,
                                            Set<String> missingCapabilities,
                                            String canonicalPackHash,
                                            EditorialEngineContractProfile trustedProfile,
                                            EditorialEngineProfile evaluatorProfile) {
        this.outcome = outcome;
        this.requiredClass = requiredClass;
        this.reasonCode = reasonCode;
        this.blocked = blocked;
        this.issues = immutableIssues(issues);
        this.missingCapabilities = immutableSortedSet(missingCapabilities);
        this.canonicalPackHash = canonicalPackHash == null ? "" : canonicalPackHash;
        this.trustedProfile = trustedProfile;
        this.evaluatorProfile = evaluatorProfile;
    }

    public EditorialPackCompatibilityClass outcome() { return outcome; }
    public EditorialPackCompatibilityClass requiredClass() { return requiredClass; }
    public EditorialCompatibilityReasonCode reasonCode() { return reasonCode; }
    public boolean blocked() { return blocked; }
    public boolean dataCompatible() {
        return !blocked && outcome == EditorialPackCompatibilityClass.DATA_COMPATIBLE;
    }
    public List<EditorialPackIntegrityResult.Issue> issues() { return issues; }
    public Set<String> missingCapabilities() { return missingCapabilities; }
    public String canonicalPackHash() { return canonicalPackHash; }
    public Optional<EditorialEngineContractProfile> trustedProfile() { return Optional.ofNullable(trustedProfile); }
    public Optional<EditorialEngineProfile> evaluatorProfile() { return Optional.ofNullable(evaluatorProfile); }

    private static List<EditorialPackIntegrityResult.Issue> immutableIssues(
            List<EditorialPackIntegrityResult.Issue> source) {
        return Collections.unmodifiableList(new ArrayList<>(source == null ? List.of() : source));
    }

    private static Set<String> immutableSortedSet(Set<String> source) {
        TreeSet<String> sorted = new TreeSet<>();
        if (source != null) sorted.addAll(source);
        return Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
    }
}
