package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable P4 project/run binding. It is setup metadata only: the contract
 * deliberately fixes executionAllowed=false and certificationState=NOT_CERTIFIED.
 */
public final class EditorialP4Binding {
    public static final String CONTRACT_VERSION = "editorial-p4-binding-v1";
    public static final String IDENTITY_DOMAIN = "EDITORIAL_P4_BINDING_IDENTITY_V1";
    public static final String FINGERPRINT_DOMAIN = "EDITORIAL_P4_BINDING_FINGERPRINT_V1";
    public static final String NOT_CERTIFIED = "NOT_CERTIFIED";

    private final String projectRevisionIdentity;
    private final String inputScopeSnapshotIdentity;
    private final String runDeclarationIdentity;
    private final String packId;
    private final String packVersion;
    private final String canonicalPackHash;
    private final String manifestFingerprint;
    private final String trustedProfileId;
    private final String trustedProfileVersion;
    private final String canonicalProfileHash;
    private final String machineContractFingerprint;
    private final String compatibilityEvaluationId;
    private final String compatibilityOutcome;
    private final String evaluationContextFingerprint;
    private final String contractVersion;
    private final String schemaVersion;
    private final String phaseGraphFingerprint;
    private final String contextAllowListFingerprint;
    private final String sourceMode;
    private final String glossaryStatus;
    private final String pronounStatus;
    private final String pairContextStatus;
    private final String explicitUserDecisionProvenance;
    private final String inputManifestFingerprint;
    private final long runAttemptOrdinal;
    private final String runKind;
    private final String phaseIdentity;
    private final List<EditorialP4SourceIdentity> inputs;
    private final String canonicalProjection;
    private final String bindingIdentity;
    private final String bindingFingerprint;

    public EditorialP4Binding(
            String projectRevisionIdentity,
            String inputScopeSnapshotIdentity,
            String runDeclarationIdentity,
            String packId,
            String packVersion,
            String canonicalPackHash,
            String manifestFingerprint,
            String trustedProfileId,
            String trustedProfileVersion,
            String canonicalProfileHash,
            String machineContractFingerprint,
            String compatibilityEvaluationId,
            String compatibilityOutcome,
            String evaluationContextFingerprint,
            String contractVersion,
            String schemaVersion,
            String phaseGraphFingerprint,
            String contextAllowListFingerprint,
            String sourceMode,
            String glossaryStatus,
            String pronounStatus,
            String pairContextStatus,
            String explicitUserDecisionProvenance,
            String inputManifestFingerprint,
            long runAttemptOrdinal,
            String runKind,
            String phaseIdentity,
            List<EditorialP4SourceIdentity> inputs) {
        this.projectRevisionIdentity = EditorialIdentityText.sha256(projectRevisionIdentity,
                "project revision identity");
        this.inputScopeSnapshotIdentity = EditorialIdentityText.sha256(inputScopeSnapshotIdentity,
                "input scope snapshot identity");
        this.runDeclarationIdentity = EditorialIdentityText.sha256(runDeclarationIdentity,
                "run declaration identity");
        this.packId = EditorialIdentityText.nonEmpty(packId, "pack id");
        this.packVersion = EditorialIdentityText.nonEmpty(packVersion, "pack version");
        this.canonicalPackHash = EditorialIdentityText.sha256(canonicalPackHash, "canonical pack hash");
        this.manifestFingerprint = EditorialIdentityText.sha256(manifestFingerprint,
                "manifest fingerprint");
        this.trustedProfileId = EditorialIdentityText.nonEmpty(trustedProfileId,
                "trusted profile id");
        this.trustedProfileVersion = EditorialIdentityText.nonEmpty(trustedProfileVersion,
                "trusted profile version");
        this.canonicalProfileHash = EditorialIdentityText.sha256(canonicalProfileHash,
                "canonical profile hash");
        this.machineContractFingerprint = EditorialIdentityText.sha256(machineContractFingerprint,
                "machine contract fingerprint");
        this.compatibilityEvaluationId = EditorialIdentityText.nonEmpty(compatibilityEvaluationId,
                "compatibility evaluation id");
        this.compatibilityOutcome = EditorialIdentityText.nonEmpty(compatibilityOutcome,
                "compatibility outcome");
        if (!EditorialPackCompatibilityClass.DATA_COMPATIBLE.name().equals(this.compatibilityOutcome)) {
            throw new IllegalArgumentException("P4 binding requires DATA_COMPATIBLE compatibility");
        }
        this.evaluationContextFingerprint = EditorialIdentityText.sha256(evaluationContextFingerprint,
                "evaluation context fingerprint");
        this.contractVersion = EditorialIdentityText.nonEmpty(contractVersion, "contract version");
        this.schemaVersion = EditorialIdentityText.nonEmpty(schemaVersion, "schema version");
        this.phaseGraphFingerprint = EditorialIdentityText.sha256(phaseGraphFingerprint,
                "phase graph fingerprint");
        this.contextAllowListFingerprint = EditorialIdentityText.sha256(contextAllowListFingerprint,
                "context allow-list fingerprint");
        this.sourceMode = EditorialIdentityText.nonEmpty(sourceMode, "source mode");
        this.glossaryStatus = EditorialIdentityText.nonEmpty(glossaryStatus, "glossary status");
        this.pronounStatus = EditorialIdentityText.nonEmpty(pronounStatus, "pronoun status");
        this.pairContextStatus = EditorialIdentityText.nonEmpty(pairContextStatus,
                "pair context status");
        this.explicitUserDecisionProvenance = EditorialIdentityText.nonEmpty(
                explicitUserDecisionProvenance, "explicit user decision provenance");
        this.inputManifestFingerprint = EditorialIdentityText.sha256(inputManifestFingerprint,
                "input manifest fingerprint");
        if (runAttemptOrdinal < 0) throw new IllegalArgumentException("run attempt ordinal cannot be negative");
        this.runAttemptOrdinal = runAttemptOrdinal;
        this.runKind = EditorialIdentityText.nonEmpty(runKind, "run kind");
        this.phaseIdentity = EditorialIdentityText.nonEmpty(phaseIdentity, "phase identity");
        this.inputs = canonicalInputs(inputs);
        if (this.inputs.isEmpty()) throw new IllegalArgumentException("P4 input identities are required");
        this.canonicalProjection = buildCanonicalProjection();
        this.bindingIdentity = EditorialIdentityText.hash(IDENTITY_DOMAIN, canonicalProjection);
        this.bindingFingerprint = EditorialIdentityText.hash(FINGERPRINT_DOMAIN, canonicalProjection);
    }

    public static EditorialP4Binding fromStored(
            String declaredIdentity,
            String declaredFingerprint,
            String projectRevisionIdentity,
            String inputScopeSnapshotIdentity,
            String runDeclarationIdentity,
            String packId,
            String packVersion,
            String canonicalPackHash,
            String manifestFingerprint,
            String trustedProfileId,
            String trustedProfileVersion,
            String canonicalProfileHash,
            String machineContractFingerprint,
            String compatibilityEvaluationId,
            String compatibilityOutcome,
            String evaluationContextFingerprint,
            String contractVersion,
            String schemaVersion,
            String phaseGraphFingerprint,
            String contextAllowListFingerprint,
            String sourceMode,
            String glossaryStatus,
            String pronounStatus,
            String pairContextStatus,
            String explicitUserDecisionProvenance,
            String inputManifestFingerprint,
            long runAttemptOrdinal,
            String runKind,
            String phaseIdentity,
            List<EditorialP4SourceIdentity> inputs) {
        EditorialP4Binding result = new EditorialP4Binding(projectRevisionIdentity,
                inputScopeSnapshotIdentity, runDeclarationIdentity, packId, packVersion,
                canonicalPackHash, manifestFingerprint, trustedProfileId, trustedProfileVersion,
                canonicalProfileHash, machineContractFingerprint, compatibilityEvaluationId,
                compatibilityOutcome, evaluationContextFingerprint, contractVersion, schemaVersion,
                phaseGraphFingerprint, contextAllowListFingerprint, sourceMode, glossaryStatus,
                pronounStatus, pairContextStatus, explicitUserDecisionProvenance,
                inputManifestFingerprint, runAttemptOrdinal, runKind, phaseIdentity, inputs);
        if (!result.bindingIdentity.equals(declaredIdentity)
                || !result.bindingFingerprint.equals(declaredFingerprint)) {
            throw new IllegalArgumentException("stored P4 binding identity/fingerprint mismatch");
        }
        return result;
    }

    public String projectRevisionIdentity() { return projectRevisionIdentity; }
    public String inputScopeSnapshotIdentity() { return inputScopeSnapshotIdentity; }
    public String runDeclarationIdentity() { return runDeclarationIdentity; }
    public String packId() { return packId; }
    public String packVersion() { return packVersion; }
    public String canonicalPackHash() { return canonicalPackHash; }
    public String manifestFingerprint() { return manifestFingerprint; }
    public String trustedProfileId() { return trustedProfileId; }
    public String trustedProfileVersion() { return trustedProfileVersion; }
    public String canonicalProfileHash() { return canonicalProfileHash; }
    public String machineContractFingerprint() { return machineContractFingerprint; }
    public String compatibilityEvaluationId() { return compatibilityEvaluationId; }
    public String compatibilityOutcome() { return compatibilityOutcome; }
    public String evaluationContextFingerprint() { return evaluationContextFingerprint; }
    public String contractVersion() { return contractVersion; }
    public String schemaVersion() { return schemaVersion; }
    public String phaseGraphFingerprint() { return phaseGraphFingerprint; }
    public String contextAllowListFingerprint() { return contextAllowListFingerprint; }
    public String sourceMode() { return sourceMode; }
    public String glossaryStatus() { return glossaryStatus; }
    public String pronounStatus() { return pronounStatus; }
    public String pairContextStatus() { return pairContextStatus; }
    public String explicitUserDecisionProvenance() { return explicitUserDecisionProvenance; }
    public String inputManifestFingerprint() { return inputManifestFingerprint; }
    public long runAttemptOrdinal() { return runAttemptOrdinal; }
    public String runKind() { return runKind; }
    public String phaseIdentity() { return phaseIdentity; }
    public List<EditorialP4SourceIdentity> inputs() { return inputs; }
    public String canonicalProjection() { return canonicalProjection; }
    public String bindingIdentity() { return bindingIdentity; }
    public String bindingFingerprint() { return bindingFingerprint; }
    public boolean executionAllowed() { return false; }
    public String certificationState() { return NOT_CERTIFIED; }

    private static List<EditorialP4SourceIdentity> canonicalInputs(
            List<EditorialP4SourceIdentity> source) {
        if (source == null) return List.of();
        ArrayList<EditorialP4SourceIdentity> ordered = new ArrayList<>(source);
        Set<String> keys = new HashSet<>();
        for (EditorialP4SourceIdentity input : ordered) {
            if (input == null) throw new IllegalArgumentException("null P4 input identity");
            if (!keys.add(input.role() + "\u0000" + input.ordinal())) {
                throw new IllegalArgumentException("duplicate P4 input role/ordinal");
            }
        }
        ordered.sort(Comparator.comparing(EditorialP4SourceIdentity::role)
                .thenComparingLong(EditorialP4SourceIdentity::ordinal));
        return List.copyOf(ordered);
    }

    private String buildCanonicalProjection() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("bindingContractVersion", CONTRACT_VERSION);
        root.put("projectRevisionIdentity", projectRevisionIdentity);
        root.put("inputScopeSnapshotIdentity", inputScopeSnapshotIdentity);
        root.put("runDeclarationIdentity", runDeclarationIdentity);
        root.put("packId", packId);
        root.put("packVersion", packVersion);
        root.put("canonicalPackHash", canonicalPackHash);
        root.put("manifestFingerprint", manifestFingerprint);
        root.put("trustedProfileId", trustedProfileId);
        root.put("trustedProfileVersion", trustedProfileVersion);
        root.put("canonicalProfileHash", canonicalProfileHash);
        root.put("machineContractFingerprint", machineContractFingerprint);
        root.put("compatibilityEvaluationId", compatibilityEvaluationId);
        root.put("compatibilityOutcome", compatibilityOutcome);
        root.put("evaluationContextFingerprint", evaluationContextFingerprint);
        root.put("contractVersion", contractVersion);
        root.put("schemaVersion", schemaVersion);
        root.put("phaseGraphFingerprint", phaseGraphFingerprint);
        root.put("contextAllowListFingerprint", contextAllowListFingerprint);
        root.put("sourceMode", sourceMode);
        root.put("glossaryStatus", glossaryStatus);
        root.put("pronounStatus", pronounStatus);
        root.put("pairContextStatus", pairContextStatus);
        root.put("explicitUserDecisionProvenance", explicitUserDecisionProvenance);
        root.put("inputManifestFingerprint", inputManifestFingerprint);
        ArrayList<Object> inputValues = new ArrayList<>();
        for (EditorialP4SourceIdentity input : inputs) inputValues.add(input.canonicalMap());
        root.put("inputs", inputValues);
        root.put("runAttemptOrdinal", BigDecimal.valueOf(runAttemptOrdinal));
        root.put("runKind", runKind);
        root.put("phaseIdentity", phaseIdentity);
        root.put("executionAllowed", false);
        root.put("certificationState", NOT_CERTIFIED);
        return EditorialCanonicalJson.canonicalize(root);
    }
}
