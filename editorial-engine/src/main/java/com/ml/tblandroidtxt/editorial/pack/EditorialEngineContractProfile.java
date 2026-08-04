package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable declarative description of the machine contract implemented by an
 * editorial engine.
 *
 * <p>This is the G2-C0A data model only. {@link EditorialEngineProfile} is the
 * older evaluator-facts adapter used by the current compatibility seam. C0A
 * does not wire either class into Android or create a runtime registry. A
 * reviewed conversion from this model to the evaluator adapter is a later
 * C0C concern.</p>
 */
public final class EditorialEngineContractProfile {
    public static final String PROFILE_FORMAT = "com.ml.tblandroidtxt.editorial-engine-profile";
    public static final int PROFILE_FORMAT_VERSION = 1;

    private final String profileFormat;
    private final int profileFormatVersion;
    private final String engineProfileId;
    private final String engineProfileVersion;
    private final String engineVersion;
    private final String minimumSupportedContractVersion;
    private final String maximumSupportedContractVersion;
    private final List<String> supportedSchemaVersions;
    private final List<InputRole> supportedInputRoles;
    private final PhaseGraph supportedPhaseGraph;
    private final Map<String, ContextAllowList> contextAllowListByPhase;
    private final List<FingerprintDescriptor> evidenceSchemaFingerprints;
    private final List<FingerprintDescriptor> gateDefinitionFingerprints;
    private final List<FingerprintDescriptor> releaseArtifactFingerprints;
    private final List<String> implementedCapabilities;
    private final List<String> explicitlyMissingCapabilities;
    private final List<CapabilityEvidence> capabilityEvidence;
    private final List<String> bundledAdapterIds;
    private final List<AdapterDescriptor> adapterDescriptors;
    private final String machineContractFingerprint;
    private final String canonicalProfileHash;
    private final String createdAt;
    private final String buildSourceCommit;
    private final DeprecationPolicy deprecationPolicy;

    public EditorialEngineContractProfile(
            String profileFormat,
            int profileFormatVersion,
            String engineProfileId,
            String engineProfileVersion,
            String engineVersion,
            String minimumSupportedContractVersion,
            String maximumSupportedContractVersion,
            List<String> supportedSchemaVersions,
            List<InputRole> supportedInputRoles,
            PhaseGraph supportedPhaseGraph,
            Map<String, ContextAllowList> contextAllowListByPhase,
            List<FingerprintDescriptor> evidenceSchemaFingerprints,
            List<FingerprintDescriptor> gateDefinitionFingerprints,
            List<FingerprintDescriptor> releaseArtifactFingerprints,
            List<String> implementedCapabilities,
            List<String> explicitlyMissingCapabilities,
            List<CapabilityEvidence> capabilityEvidence,
            List<String> bundledAdapterIds,
            List<AdapterDescriptor> adapterDescriptors,
            String machineContractFingerprint,
            String canonicalProfileHash,
            String createdAt,
            String buildSourceCommit,
            DeprecationPolicy deprecationPolicy) {
        this.profileFormat = Objects.requireNonNull(profileFormat, "profileFormat");
        this.profileFormatVersion = profileFormatVersion;
        this.engineProfileId = Objects.requireNonNull(engineProfileId, "engineProfileId");
        this.engineProfileVersion = Objects.requireNonNull(engineProfileVersion, "engineProfileVersion");
        this.engineVersion = Objects.requireNonNull(engineVersion, "engineVersion");
        this.minimumSupportedContractVersion = minimumSupportedContractVersion;
        this.maximumSupportedContractVersion = maximumSupportedContractVersion;
        this.supportedSchemaVersions = immutableList(supportedSchemaVersions, "supportedSchemaVersions");
        this.supportedInputRoles = immutableList(supportedInputRoles, "supportedInputRoles");
        this.supportedPhaseGraph = Objects.requireNonNull(supportedPhaseGraph, "supportedPhaseGraph");
        this.contextAllowListByPhase = immutableMap(contextAllowListByPhase, "contextAllowListByPhase");
        this.evidenceSchemaFingerprints = immutableList(evidenceSchemaFingerprints, "evidenceSchemaFingerprints");
        this.gateDefinitionFingerprints = immutableList(gateDefinitionFingerprints, "gateDefinitionFingerprints");
        this.releaseArtifactFingerprints = immutableList(releaseArtifactFingerprints, "releaseArtifactFingerprints");
        this.implementedCapabilities = immutableList(implementedCapabilities, "implementedCapabilities");
        this.explicitlyMissingCapabilities = immutableList(explicitlyMissingCapabilities, "explicitlyMissingCapabilities");
        this.capabilityEvidence = immutableList(capabilityEvidence, "capabilityEvidence");
        this.bundledAdapterIds = immutableList(bundledAdapterIds, "bundledAdapterIds");
        this.adapterDescriptors = immutableList(adapterDescriptors, "adapterDescriptors");
        this.machineContractFingerprint = Objects.requireNonNull(machineContractFingerprint, "machineContractFingerprint");
        this.canonicalProfileHash = Objects.requireNonNull(canonicalProfileHash, "canonicalProfileHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.buildSourceCommit = Objects.requireNonNull(buildSourceCommit, "buildSourceCommit");
        this.deprecationPolicy = Objects.requireNonNull(deprecationPolicy, "deprecationPolicy");
    }

    private static <T> List<T> immutableList(List<T> source, String name) {
        return Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(source, name)));
    }

    private static <T> Map<String, T> immutableMap(Map<String, T> source, String name) {
        Objects.requireNonNull(source, name);
        LinkedHashMap<String, T> copy = new LinkedHashMap<>();
        for (Map.Entry<String, T> entry : source.entrySet()) {
            copy.put(Objects.requireNonNull(entry.getKey(), name + " key"), Objects.requireNonNull(entry.getValue(), name + " value"));
        }
        return Collections.unmodifiableMap(copy);
    }

    public String profileFormat() { return profileFormat; }
    public int profileFormatVersion() { return profileFormatVersion; }
    public String engineProfileId() { return engineProfileId; }
    public String engineProfileVersion() { return engineProfileVersion; }
    public String engineVersion() { return engineVersion; }
    public String minimumSupportedContractVersion() { return minimumSupportedContractVersion; }
    public String maximumSupportedContractVersion() { return maximumSupportedContractVersion; }
    public List<String> supportedSchemaVersions() { return supportedSchemaVersions; }
    public List<InputRole> supportedInputRoles() { return supportedInputRoles; }
    public PhaseGraph supportedPhaseGraph() { return supportedPhaseGraph; }
    public Map<String, ContextAllowList> contextAllowListByPhase() { return contextAllowListByPhase; }
    public List<FingerprintDescriptor> evidenceSchemaFingerprints() { return evidenceSchemaFingerprints; }
    public List<FingerprintDescriptor> gateDefinitionFingerprints() { return gateDefinitionFingerprints; }
    public List<FingerprintDescriptor> releaseArtifactFingerprints() { return releaseArtifactFingerprints; }
    public List<String> implementedCapabilities() { return implementedCapabilities; }
    public List<String> explicitlyMissingCapabilities() { return explicitlyMissingCapabilities; }
    public List<CapabilityEvidence> capabilityEvidence() { return capabilityEvidence; }
    public List<String> bundledAdapterIds() { return bundledAdapterIds; }
    public List<AdapterDescriptor> adapterDescriptors() { return adapterDescriptors; }
    public String machineContractFingerprint() { return machineContractFingerprint; }
    public String canonicalProfileHash() { return canonicalProfileHash; }
    public String createdAt() { return createdAt; }
    public String buildSourceCommit() { return buildSourceCommit; }
    public DeprecationPolicy deprecationPolicy() { return deprecationPolicy; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialEngineContractProfile that)) return false;
        return profileFormatVersion == that.profileFormatVersion
                && profileFormat.equals(that.profileFormat)
                && engineProfileId.equals(that.engineProfileId)
                && engineProfileVersion.equals(that.engineProfileVersion)
                && engineVersion.equals(that.engineVersion)
                && Objects.equals(minimumSupportedContractVersion, that.minimumSupportedContractVersion)
                && Objects.equals(maximumSupportedContractVersion, that.maximumSupportedContractVersion)
                && supportedSchemaVersions.equals(that.supportedSchemaVersions)
                && supportedInputRoles.equals(that.supportedInputRoles)
                && supportedPhaseGraph.equals(that.supportedPhaseGraph)
                && contextAllowListByPhase.equals(that.contextAllowListByPhase)
                && evidenceSchemaFingerprints.equals(that.evidenceSchemaFingerprints)
                && gateDefinitionFingerprints.equals(that.gateDefinitionFingerprints)
                && releaseArtifactFingerprints.equals(that.releaseArtifactFingerprints)
                && implementedCapabilities.equals(that.implementedCapabilities)
                && explicitlyMissingCapabilities.equals(that.explicitlyMissingCapabilities)
                && capabilityEvidence.equals(that.capabilityEvidence)
                && bundledAdapterIds.equals(that.bundledAdapterIds)
                && adapterDescriptors.equals(that.adapterDescriptors)
                && machineContractFingerprint.equals(that.machineContractFingerprint)
                && canonicalProfileHash.equals(that.canonicalProfileHash)
                && createdAt.equals(that.createdAt)
                && buildSourceCommit.equals(that.buildSourceCommit)
                && deprecationPolicy.equals(that.deprecationPolicy);
    }

    @Override public int hashCode() {
        return Objects.hash(profileFormat, profileFormatVersion, engineProfileId, engineProfileVersion, engineVersion,
                minimumSupportedContractVersion, maximumSupportedContractVersion, supportedSchemaVersions,
                supportedInputRoles, supportedPhaseGraph, contextAllowListByPhase, evidenceSchemaFingerprints,
                gateDefinitionFingerprints, releaseArtifactFingerprints, implementedCapabilities,
                explicitlyMissingCapabilities, capabilityEvidence, bundledAdapterIds, adapterDescriptors,
                machineContractFingerprint, canonicalProfileHash, createdAt, buildSourceCommit, deprecationPolicy);
    }

    public record InputRole(String role, String cardinality, boolean required) {
        public InputRole {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(cardinality, "cardinality");
        }
    }

    public record PhaseGraph(List<String> phases, List<PhaseEdge> edges) {
        public PhaseGraph {
            phases = immutableList(phases, "phases");
            edges = immutableList(edges, "edges");
        }
    }

    public record PhaseEdge(String from, String to) {
        public PhaseEdge {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
        }
    }

    public record ContextAllowList(List<String> requiredRoles, List<String> allowedRoles) {
        public ContextAllowList {
            requiredRoles = immutableList(requiredRoles, "requiredRoles");
            allowedRoles = immutableList(allowedRoles, "allowedRoles");
        }
    }

    public record FingerprintDescriptor(String id, String fingerprint) {
        public FingerprintDescriptor {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(fingerprint, "fingerprint");
        }
    }

    public record CapabilityEvidence(String capabilityId, String sourceCommit,
                                     String evidenceFingerprint, String evidenceClass) {
        public CapabilityEvidence {
            Objects.requireNonNull(capabilityId, "capabilityId");
            Objects.requireNonNull(sourceCommit, "sourceCommit");
            Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint");
            Objects.requireNonNull(evidenceClass, "evidenceClass");
        }
    }

    public record AdapterDescriptor(String adapterId, String adapterVersion,
                                    String sourceContractVersion, String sourceSchemaVersion,
                                    String sourceMachineContractFingerprint,
                                    List<String> requiredCapabilities) {
        public AdapterDescriptor {
            Objects.requireNonNull(adapterId, "adapterId");
            Objects.requireNonNull(adapterVersion, "adapterVersion");
            Objects.requireNonNull(sourceContractVersion, "sourceContractVersion");
            Objects.requireNonNull(sourceSchemaVersion, "sourceSchemaVersion");
            Objects.requireNonNull(sourceMachineContractFingerprint, "sourceMachineContractFingerprint");
            requiredCapabilities = immutableList(requiredCapabilities, "requiredCapabilities");
        }
    }

    public record DeprecationPolicy(String state, String replacementProfileId,
                                    String replacementVersion, boolean automaticReplacement,
                                    boolean automaticProjectRebind) {
        public DeprecationPolicy {
            Objects.requireNonNull(state, "state");
        }
    }
}
