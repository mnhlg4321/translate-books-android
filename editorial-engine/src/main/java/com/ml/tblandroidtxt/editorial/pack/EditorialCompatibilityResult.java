package com.ml.tblandroidtxt.editorial.pack;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class EditorialCompatibilityResult {
    private final EditorialPackCompatibilityClass classification;
    private final EditorialPackCompatibilityClass requiredClass;
    private final boolean blocked;
    private final List<EditorialPackIntegrityResult.Issue> issues;
    private final Set<String> missingCapabilities;
    private final String machineContractFingerprint;

    EditorialCompatibilityResult(EditorialPackCompatibilityClass classification,
                                  EditorialPackCompatibilityClass requiredClass, boolean blocked,
                                  List<EditorialPackIntegrityResult.Issue> issues, Set<String> missingCapabilities,
                                  String machineContractFingerprint) {
        this.classification = classification;
        this.requiredClass = requiredClass;
        this.blocked = blocked;
        this.issues = Collections.unmodifiableList(List.copyOf(issues));
        this.missingCapabilities = Collections.unmodifiableSet(Set.copyOf(missingCapabilities));
        this.machineContractFingerprint = machineContractFingerprint == null ? "" : machineContractFingerprint;
    }

    public EditorialPackCompatibilityClass classification() { return classification; }
    public EditorialPackCompatibilityClass requiredClass() { return requiredClass; }
    public boolean blocked() { return blocked; }
    public boolean usableWithoutApk() { return !blocked && (classification == EditorialPackCompatibilityClass.DATA_COMPATIBLE || classification == EditorialPackCompatibilityClass.ADAPTER_REQUIRED); }
    public List<EditorialPackIntegrityResult.Issue> issues() { return issues; }
    public Set<String> missingCapabilities() { return missingCapabilities; }
    public String machineContractFingerprint() { return machineContractFingerprint; }
    public EditorialPackValidationCode primaryCode() { return issues.isEmpty() ? EditorialPackValidationCode.VALID : issues.get(0).code(); }
}
