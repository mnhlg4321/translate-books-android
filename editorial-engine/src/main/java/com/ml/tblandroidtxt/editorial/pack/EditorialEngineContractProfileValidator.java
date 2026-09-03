package com.ml.tblandroidtxt.editorial.pack;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Fail-closed structural and semantic validator for contract profiles. */
public final class EditorialEngineContractProfileValidator {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final Pattern VERSION = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._+/-]{0,127}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern COMMIT = Pattern.compile("[0-9a-f]{40,64}");
    private static final Set<String> CARDINALITIES = Set.of("ONE", "OPTIONAL", "MANY");
    private static final Set<String> DEPRECATION_STATES = Set.of("ACTIVE", "DEPRECATED", "REVOKED");

    private final EditorialEngineContractProfileParser parser;
    private final EditorialEngineContractCapabilityEvidenceCatalog capabilityCatalog;

    public EditorialEngineContractProfileValidator() {
        this(EditorialEngineContractCapabilityEvidenceCatalog.production());
    }

    public EditorialEngineContractProfileValidator(EditorialEngineContractCapabilityEvidenceCatalog capabilityCatalog) {
        this.parser = new EditorialEngineContractProfileParser();
        if (capabilityCatalog == null) throw new NullPointerException("capabilityCatalog");
        this.capabilityCatalog = capabilityCatalog;
    }

    /** Validates raw UTF-8 profile bytes without exposing parser contents in errors. */
    public EditorialEngineContractProfileValidationResult validate(byte[] profileBytes) {
        if (profileBytes == null) {
            return invalid(EditorialEngineContractProfileValidationResult.Code.INPUT_NULL, "profile", "Profile bytes are null");
        }
        if (profileBytes.length > EditorialEngineContractProfileParser.MAX_PROFILE_BYTES) {
            return invalid(EditorialEngineContractProfileValidationResult.Code.SIZE_LIMIT, "profile", "Profile exceeds maximum byte length");
        }
        final EditorialEngineContractProfile profile;
        try {
            profile = parser.parse(profileBytes);
        } catch (EditorialEngineContractProfileParser.ParseException e) {
            return invalid(parserCode(e.code()), "profile", "Profile rejected during parsing");
        } catch (IllegalArgumentException e) {
            return invalid(EditorialEngineContractProfileValidationResult.Code.PARSE_ERROR, "profile", "Profile rejected during parsing");
        }
        return validate(profile);
    }

    public EditorialEngineContractProfileValidationResult validate(EditorialEngineContractProfile profile) {
        if (profile == null) {
            return invalid(EditorialEngineContractProfileValidationResult.Code.INPUT_NULL, "profile", "Profile is null");
        }
        java.util.ArrayList<EditorialEngineContractProfileValidationResult.Issue> issues = new java.util.ArrayList<>();
        validateLimits(profile, issues);
        validateIdentity(profile, issues);
        validateContractShape(profile, issues);
        validatePhaseGraph(profile, issues);
        validateContexts(profile, issues);
        validateFingerprints(profile.evidenceSchemaFingerprints(), "evidenceSchemaFingerprints", issues);
        validateFingerprints(profile.gateDefinitionFingerprints(), "gateDefinitionFingerprints", issues);
        validateFingerprints(profile.releaseArtifactFingerprints(), "releaseArtifactFingerprints", issues);
        validateCapabilities(profile, issues);
        validateAdapters(profile, issues);
        validateDeprecation(profile, issues);
        validateSafe4Shape(profile, issues);

        if (issues.stream().anyMatch(issue -> issue.code() == EditorialEngineContractProfileValidationResult.Code.COLLECTION_LIMIT
                || issue.code() == EditorialEngineContractProfileValidationResult.Code.STRING_LIMIT)) {
            return EditorialEngineContractProfileValidationResult.invalid(issues);
        }

        String calculatedMachineFingerprint = EditorialEngineContractProfileCanonicalizer.machineContractFingerprint(profile);
        if (!calculatedMachineFingerprint.equals(profile.machineContractFingerprint())) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.MACHINE_FINGERPRINT_MISMATCH,
                    "machineContractFingerprint", "Machine-contract fingerprint does not match the semantic projection");
        }
        String calculatedProfileHash = EditorialEngineContractProfileCanonicalizer.canonicalProfileHash(profile);
        if (!calculatedProfileHash.equals(profile.canonicalProfileHash())) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.CANONICAL_HASH_MISMATCH,
                    "canonicalProfileHash", "Canonical profile hash does not match the profile without its hash field");
        }
        return issues.isEmpty()
                ? EditorialEngineContractProfileValidationResult.valid(profile)
                : EditorialEngineContractProfileValidationResult.invalid(issues);
    }

    private static void validateLimits(EditorialEngineContractProfile profile,
                                       List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        if (profile.supportedSchemaVersions().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                || profile.supportedInputRoles().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                || profile.supportedPhaseGraph().phases().size() > EditorialEngineContractProfileParser.MAX_PHASE_COUNT
                || profile.supportedPhaseGraph().edges().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                || profile.contextAllowListByPhase().size() > EditorialEngineContractProfileParser.MAX_CONTEXT_COUNT
                || profile.evidenceSchemaFingerprints().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                || profile.gateDefinitionFingerprints().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                || profile.releaseArtifactFingerprints().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                || profile.implementedCapabilities().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                || profile.explicitlyMissingCapabilities().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                || profile.capabilityEvidence().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                || profile.bundledAdapterIds().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                || profile.adapterDescriptors().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.COLLECTION_LIMIT,
                    "profile", "Profile collection exceeds maximum item count");
        }
        if (profile.createdAt().length() > EditorialEngineContractProfileParser.MAX_STRING_LENGTH
                || profile.machineContractFingerprint().length() > EditorialEngineContractProfileParser.MAX_STRING_LENGTH
                || profile.canonicalProfileHash().length() > EditorialEngineContractProfileParser.MAX_STRING_LENGTH
                || profile.buildSourceCommit().length() > EditorialEngineContractProfileParser.MAX_STRING_LENGTH) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.STRING_LIMIT,
                    "profile", "Profile string exceeds maximum length");
        }
        for (Map.Entry<String, EditorialEngineContractProfile.ContextAllowList> entry : profile.contextAllowListByPhase().entrySet()) {
            if (entry.getKey().length() > EditorialEngineContractProfileParser.MAX_STRING_LENGTH
                    || entry.getValue().requiredRoles().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE
                    || entry.getValue().allowedRoles().size() > EditorialEngineContractProfileParser.MAX_COLLECTION_SIZE) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.COLLECTION_LIMIT,
                        "contextAllowListByPhase", "Context collection exceeds maximum item count");
            }
        }
    }

    private static void validateIdentity(EditorialEngineContractProfile profile,
                                         List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        if (!EditorialEngineContractProfile.PROFILE_FORMAT.equals(profile.profileFormat())) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_FORMAT,
                    "profileFormat", "Unsupported profile format");
        }
        if (profile.profileFormatVersion() != EditorialEngineContractProfile.PROFILE_FORMAT_VERSION) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_VERSION,
                    "profileFormatVersion", "Unsupported profile format version");
        }
        identifier(profile.engineProfileId(), "engineProfileId", issues);
        version(profile.engineProfileVersion(), "engineProfileVersion", issues);
        version(profile.engineVersion(), "engineVersion", issues);
        if (profile.minimumSupportedContractVersion() == null
                ^ profile.maximumSupportedContractVersion() == null) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.CONTRACT_SCHEMA_MISMATCH,
                    "supportedContractVersions", "Contract version bounds must both be null or both be present");
        }
        if (profile.minimumSupportedContractVersion() != null) {
            version(profile.minimumSupportedContractVersion(), "minimumSupportedContractVersion", issues);
            version(profile.maximumSupportedContractVersion(), "maximumSupportedContractVersion", issues);
            if (compareContractVersions(profile.minimumSupportedContractVersion(), profile.maximumSupportedContractVersion()) > 0) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.CONTRACT_SCHEMA_MISMATCH,
                        "supportedContractVersions", "Minimum contract version is greater than maximum contract version");
            }
        }
        sha256(profile.machineContractFingerprint(), "machineContractFingerprint", issues);
        sha256(profile.canonicalProfileHash(), "canonicalProfileHash", issues);
        commit(profile.buildSourceCommit(), "buildSourceCommit", issues);
        if (profile.createdAt().isBlank()) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_VERSION,
                    "createdAt", "createdAt is blank");
        } else {
            try {
                OffsetDateTime.parse(profile.createdAt());
            } catch (DateTimeParseException e) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_VERSION,
                        "createdAt", "createdAt must be an ISO offset date-time");
            }
        }
    }

    private static void validateContractShape(EditorialEngineContractProfile profile,
                                              List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        uniqueStrings(profile.supportedSchemaVersions(), "supportedSchemaVersions", issues, false);
        Set<String> roles = new HashSet<>();
        for (int i = 0; i < profile.supportedInputRoles().size(); i++) {
            EditorialEngineContractProfile.InputRole role = profile.supportedInputRoles().get(i);
            String path = "supportedInputRoles[" + i + "]";
            identifier(role.role(), path + ".role", issues);
            version(role.cardinality(), path + ".cardinality", issues);
            if (!CARDINALITIES.contains(role.cardinality())) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_DESCRIPTOR,
                        path + ".cardinality", "Unsupported input-role cardinality");
            }
            if (!roles.add(role.role())) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE,
                        path + ".role", "Duplicate input role");
            }
        }
        for (int i = 0; i < profile.supportedSchemaVersions().size(); i++) {
            version(profile.supportedSchemaVersions().get(i), "supportedSchemaVersions[" + i + "]", issues);
        }

        boolean hasContractBounds = profile.minimumSupportedContractVersion() != null;
        if (hasContractBounds) {
            if (profile.supportedSchemaVersions().isEmpty() || profile.supportedPhaseGraph().phases().isEmpty()) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.CONTRACT_SCHEMA_MISMATCH,
                        "supportedContractVersions", "Executable contract support requires schema and phase descriptors");
            }
        } else {
            if (!profile.supportedSchemaVersions().isEmpty()
                    || !profile.supportedInputRoles().isEmpty()
                    || !profile.supportedPhaseGraph().phases().isEmpty()
                    || !profile.supportedPhaseGraph().edges().isEmpty()
                    || !profile.contextAllowListByPhase().isEmpty()
                    || !profile.evidenceSchemaFingerprints().isEmpty()
                    || !profile.gateDefinitionFingerprints().isEmpty()
                    || !profile.releaseArtifactFingerprints().isEmpty()
                    || !profile.bundledAdapterIds().isEmpty()
                    || !profile.adapterDescriptors().isEmpty()) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.CONTRACT_SCHEMA_MISMATCH,
                        "supportedContractVersions", "A no-contract profile cannot declare executable contract facts");
            }
        }
    }

    private static void validatePhaseGraph(EditorialEngineContractProfile profile,
                                           List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        Set<String> phases = new HashSet<>();
        for (int i = 0; i < profile.supportedPhaseGraph().phases().size(); i++) {
            String phase = profile.supportedPhaseGraph().phases().get(i);
            if (!phases.add(phase)) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE,
                        "supportedPhaseGraph.phases[" + i + "]", "Duplicate phase");
            }
            identifier(phase, "supportedPhaseGraph.phases[" + i + "]", issues);
        }
        Set<String> edges = new HashSet<>();
        for (int i = 0; i < profile.supportedPhaseGraph().edges().size(); i++) {
            EditorialEngineContractProfile.PhaseEdge edge = profile.supportedPhaseGraph().edges().get(i);
            String path = "supportedPhaseGraph.edges[" + i + "]";
            identifier(edge.from(), path + ".from", issues);
            identifier(edge.to(), path + ".to", issues);
            if (!phases.contains(edge.from()) || !phases.contains(edge.to())) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.DANGLING_PHASE,
                        path, "Phase edge references a phase outside the graph");
            }
            if (!edges.add(edge.from() + "\u0000" + edge.to())) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE,
                        path, "Duplicate phase edge");
            }
        }
    }

    private static void validateContexts(EditorialEngineContractProfile profile,
                                         List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        Set<String> phases = new HashSet<>(profile.supportedPhaseGraph().phases());
        if (!profile.contextAllowListByPhase().keySet().equals(phases)) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.INCOMPLETE_CONTEXT,
                    "contextAllowListByPhase", "Context allow-list must cover exactly every supported phase");
        }
        Set<String> roles = new HashSet<>();
        for (EditorialEngineContractProfile.InputRole role : profile.supportedInputRoles()) roles.add(role.role());
        for (Map.Entry<String, EditorialEngineContractProfile.ContextAllowList> entry : profile.contextAllowListByPhase().entrySet()) {
            String phasePath = "contextAllowListByPhase." + entry.getKey();
            Set<String> allowed = new HashSet<>();
            for (String role : entry.getValue().allowedRoles()) {
                if (!allowed.add(role)) add(issues, EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE, phasePath, "Duplicate allowed context role");
                if (!roles.contains(role)) add(issues, EditorialEngineContractProfileValidationResult.Code.INCOMPLETE_CONTEXT, phasePath, "Context role is not a supported input role");
            }
            Set<String> required = new HashSet<>();
            for (String role : entry.getValue().requiredRoles()) {
                if (!required.add(role)) add(issues, EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE, phasePath, "Duplicate required context role");
                if (!roles.contains(role) || !allowed.contains(role)) add(issues, EditorialEngineContractProfileValidationResult.Code.INCOMPLETE_CONTEXT, phasePath, "Required context role is not allowed");
            }
        }
    }

    private static void validateFingerprints(List<EditorialEngineContractProfile.FingerprintDescriptor> values,
                                             String path,
                                             List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < values.size(); i++) {
            EditorialEngineContractProfile.FingerprintDescriptor item = values.get(i);
            String itemPath = path + "[" + i + "]";
            identifier(item.id(), itemPath + ".id", issues);
            sha256(item.fingerprint(), itemPath + ".fingerprint", issues);
            if (!ids.add(item.id())) add(issues, EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE, itemPath + ".id", "Duplicate descriptor ID");
        }
    }

    private void validateCapabilities(EditorialEngineContractProfile profile,
                                      List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        uniqueStrings(profile.implementedCapabilities(), "implementedCapabilities", issues, true);
        uniqueStrings(profile.explicitlyMissingCapabilities(), "explicitlyMissingCapabilities", issues, true);
        Set<String> implemented = new HashSet<>(profile.implementedCapabilities());
        Set<String> missing = new HashSet<>(profile.explicitlyMissingCapabilities());
        for (String capability : implemented) {
            if (missing.contains(capability)) add(issues, EditorialEngineContractProfileValidationResult.Code.CAPABILITY_OVERLAP,
                    "implementedCapabilities", "Capability appears in both implemented and missing sets");
            if (!capabilityCatalog.confirmsImplemented(capability)) add(issues, EditorialEngineContractProfileValidationResult.Code.UNKNOWN_CAPABILITY,
                    "implementedCapabilities", "Capability is not recognized by the trusted implementation/evidence catalog");
        }
        for (String capability : missing) {
            if (!capabilityCatalog.recognizes(capability)) add(issues, EditorialEngineContractProfileValidationResult.Code.UNKNOWN_CAPABILITY,
                    "explicitlyMissingCapabilities", "Capability is not recognized by the trusted implementation/evidence catalog");
        }

        Set<String> evidenceCapabilities = new HashSet<>();
        for (int i = 0; i < profile.capabilityEvidence().size(); i++) {
            EditorialEngineContractProfile.CapabilityEvidence evidence = profile.capabilityEvidence().get(i);
            String path = "capabilityEvidence[" + i + "]";
            identifier(evidence.capabilityId(), path + ".capabilityId", issues);
            commit(evidence.sourceCommit(), path + ".sourceCommit", issues);
            sha256(evidence.evidenceFingerprint(), path + ".evidenceFingerprint", issues);
            identifier(evidence.evidenceClass(), path + ".evidenceClass", issues);
            if (!implemented.contains(evidence.capabilityId()) || !capabilityCatalog.confirmsImplemented(evidence.capabilityId())) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_CAPABILITY_EVIDENCE,
                        path, "Evidence must describe a capability recognized as implemented");
            }
            if ("p3b-contract".equals(evidence.evidenceClass())) {
                EditorialEngineContractCapabilityEvidenceCatalog.EvidenceDescriptor descriptor =
                        capabilityCatalog.evidenceDescriptor(evidence.capabilityId()).orElse(null);
                if (descriptor == null || !descriptor.evidenceFingerprint().equals(evidence.evidenceFingerprint())) {
                    add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_CAPABILITY_EVIDENCE,
                            path, "P3B evidence fingerprint does not match the code-owned descriptor");
                }
            }
            if (!evidenceCapabilities.add(evidence.capabilityId())) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE,
                        path + ".capabilityId", "Duplicate capability evidence");
            }
        }
        for (String capability : implemented) {
            if (!evidenceCapabilities.contains(capability)) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.MISSING_CAPABILITY_EVIDENCE,
                        "capabilityEvidence", "Every implemented capability requires a trusted evidence record");
            }
        }
    }

    private void validateAdapters(EditorialEngineContractProfile profile,
                                  List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        Set<String> bundledIds = new HashSet<>();
        for (String adapterId : profile.bundledAdapterIds()) {
            identifier(adapterId, "bundledAdapterIds", issues);
            if (!bundledIds.add(adapterId)) add(issues, EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE,
                    "bundledAdapterIds", "Duplicate bundled adapter ID");
        }
        Set<String> descriptorIds = new HashSet<>();
        for (int i = 0; i < profile.adapterDescriptors().size(); i++) {
            EditorialEngineContractProfile.AdapterDescriptor adapter = profile.adapterDescriptors().get(i);
            String path = "adapterDescriptors[" + i + "]";
            identifier(adapter.adapterId(), path + ".adapterId", issues);
            version(adapter.adapterVersion(), path + ".adapterVersion", issues);
            version(adapter.sourceContractVersion(), path + ".sourceContractVersion", issues);
            version(adapter.sourceSchemaVersion(), path + ".sourceSchemaVersion", issues);
            sha256(adapter.sourceMachineContractFingerprint(), path + ".sourceMachineContractFingerprint", issues);
            if (!descriptorIds.add(adapter.adapterId())) add(issues, EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE,
                    path + ".adapterId", "Duplicate adapter ID");
            if (!bundledIds.contains(adapter.adapterId())) add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_ADAPTER,
                    path + ".adapterId", "Adapter descriptor is not listed as bundled");
            for (String capability : adapter.requiredCapabilities()) {
                if (!capabilityCatalog.recognizes(capability)) add(issues, EditorialEngineContractProfileValidationResult.Code.UNKNOWN_CAPABILITY,
                        path + ".requiredCapabilities", "Adapter requires an unknown capability");
                if (!profile.implementedCapabilities().contains(capability)) add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_ADAPTER,
                        path + ".requiredCapabilities", "Adapter requires a capability not implemented by the profile");
            }
        }
        if (!bundledIds.equals(descriptorIds)) add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_ADAPTER,
                "adapterDescriptors", "Bundled adapter IDs and adapter descriptors must match exactly");
    }

    private static void validateSafe4Shape(EditorialEngineContractProfile profile,
                                           List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        if (!EditorialSafe4Contract.CONTRACT_VERSION.equals(profile.minimumSupportedContractVersion())
                || !EditorialSafe4Contract.CONTRACT_VERSION.equals(profile.maximumSupportedContractVersion())) return;
        try {
            EditorialEngineContractProfile expected = EditorialSafe4TrustedProfileFactory.create(
                    profile.buildSourceCommit(), profile.createdAt());
            String actual = EditorialEngineContractProfileCanonicalizer.canonicalJsonWithoutProfileHash(profile);
            String canonicalExpected = EditorialEngineContractProfileCanonicalizer.canonicalJsonWithoutProfileHash(expected);
            if (!canonicalExpected.equals(actual)) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_DESCRIPTOR,
                        "safe4", "SAFE4 profile shape does not match the code-owned contract descriptor");
            }
        } catch (RuntimeException e) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_DESCRIPTOR,
                    "safe4", "SAFE4 profile cannot be reconstructed from its declared evidence inputs");
        }
    }

    private static void validateDeprecation(EditorialEngineContractProfile profile,
                                            List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        EditorialEngineContractProfile.DeprecationPolicy policy = profile.deprecationPolicy();
        if (!DEPRECATION_STATES.contains(policy.state())) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_DEPRECATION,
                    "deprecationPolicy.state", "Unknown deprecation state");
        }
        if ((policy.replacementProfileId() == null) != (policy.replacementVersion() == null)) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_DEPRECATION,
                    "deprecationPolicy", "Replacement profile ID and version must be both null or both present");
        }
        if (policy.replacementProfileId() != null) {
            identifier(policy.replacementProfileId(), "deprecationPolicy.replacementProfileId", issues);
            version(policy.replacementVersion(), "deprecationPolicy.replacementVersion", issues);
            if (policy.replacementProfileId().equals(profile.engineProfileId())
                    && policy.replacementVersion().equals(profile.engineProfileVersion())) {
                add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_DEPRECATION,
                        "deprecationPolicy", "Replacement profile cannot reference itself");
            }
        }
        if (policy.automaticReplacement() || policy.automaticProjectRebind()) {
            add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_DEPRECATION,
                    "deprecationPolicy", "Automatic replacement and project rebind are forbidden");
        }
    }

    private static void uniqueStrings(List<String> values, String path,
                                      List<EditorialEngineContractProfileValidationResult.Issue> issues,
                                      boolean capabilities) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < values.size(); i++) {
            String item = values.get(i);
            if (item == null || item.isBlank()) add(issues, EditorialEngineContractProfileValidationResult.Code.INVALID_ID,
                    path + "[" + i + "]", "Value is blank");
            if (!seen.add(item)) add(issues, EditorialEngineContractProfileValidationResult.Code.DUPLICATE_VALUE,
                    path + "[" + i + "]", "Duplicate value");
            if (capabilities) identifier(item, path + "[" + i + "]", issues);
        }
    }

    private static void identifier(String value, String path,
                                   List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) add(issues,
                EditorialEngineContractProfileValidationResult.Code.INVALID_ID, path, "Identifier format is invalid");
    }

    private static void version(String value, String path,
                                List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        if (value == null || !VERSION.matcher(value).matches()) add(issues,
                EditorialEngineContractProfileValidationResult.Code.INVALID_VERSION, path, "Version format is invalid");
    }

    private static void sha256(String value, String path,
                               List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        if (value == null || !SHA256.matcher(value).matches()) add(issues,
                EditorialEngineContractProfileValidationResult.Code.INVALID_HASH, path, "Fingerprint must be lowercase SHA-256 hex");
    }

    private static void commit(String value, String path,
                               List<EditorialEngineContractProfileValidationResult.Issue> issues) {
        if (value == null || !COMMIT.matcher(value).matches()) add(issues,
                EditorialEngineContractProfileValidationResult.Code.INVALID_FINGERPRINT, path, "Source commit must be full lowercase Git hex");
    }

    private static int compareContractVersions(String left, String right) {
        String[] leftParts = left.split("[._-]");
        String[] rightParts = right.split("[._-]");
        int count = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < count; i++) {
            String a = i < leftParts.length ? leftParts[i] : "";
            String b = i < rightParts.length ? rightParts[i] : "";
            boolean numericA = a.matches("[0-9]+");
            boolean numericB = b.matches("[0-9]+");
            int comparison;
            if (numericA && numericB) comparison = compareNumericStrings(a, b);
            else comparison = a.compareTo(b);
            if (comparison != 0) return comparison;
        }
        return 0;
    }

    private static int compareNumericStrings(String left, String right) {
        String a = left.replaceFirst("^0+(?!$)", "");
        String b = right.replaceFirst("^0+(?!$)", "");
        if (a.length() != b.length()) return Integer.compare(a.length(), b.length());
        return a.compareTo(b);
    }

    private static EditorialEngineContractProfileValidationResult.Code parserCode(
            EditorialEngineContractProfileParser.Code code) {
        return switch (code) {
            case INPUT_NULL -> EditorialEngineContractProfileValidationResult.Code.INPUT_NULL;
            case SIZE_LIMIT -> EditorialEngineContractProfileValidationResult.Code.SIZE_LIMIT;
            case UNKNOWN_FIELD -> EditorialEngineContractProfileValidationResult.Code.UNKNOWN_FIELD;
            case MISSING_FIELD -> EditorialEngineContractProfileValidationResult.Code.MISSING_FIELD;
            case WRONG_TYPE -> EditorialEngineContractProfileValidationResult.Code.WRONG_TYPE;
            case COLLECTION_LIMIT -> EditorialEngineContractProfileValidationResult.Code.COLLECTION_LIMIT;
            case STRING_LIMIT -> EditorialEngineContractProfileValidationResult.Code.STRING_LIMIT;
            case PARSE_ERROR -> EditorialEngineContractProfileValidationResult.Code.PARSE_ERROR;
        };
    }

    private static EditorialEngineContractProfileValidationResult invalid(
            EditorialEngineContractProfileValidationResult.Code code, String path, String message) {
        return EditorialEngineContractProfileValidationResult.invalid(List.of(
                new EditorialEngineContractProfileValidationResult.Issue(code, path, message)));
    }

    private static void add(List<EditorialEngineContractProfileValidationResult.Issue> issues,
                            EditorialEngineContractProfileValidationResult.Code code,
                            String path, String message) {
        issues.add(new EditorialEngineContractProfileValidationResult.Issue(code, path, message));
    }

}
