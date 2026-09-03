package com.ml.tblandroidtxt.editorial.pack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Code-owned capability allow-list boundary for profile validation.
 *
 * <p>This is not a bundled profile and is not a certification evidence store.
 * The production catalog knows the SAFE4 capability IDs so missing capability
 * declarations can be checked and confirms only the capabilities whose
 * production owner and deterministic evidence descriptor are code-owned below.
 * Test-only catalogs must be explicitly constructed and may contain only
 * {@code test.*} identifiers.</p>
 */
public final class EditorialEngineContractCapabilityEvidenceCatalog {
    public static final String PACK_INTEGRITY_SHA256_V1 = "pack.integrity.sha256.v1";
    public static final String SOURCE_PREFLIGHT_SAFE4_FULL_V1 = "source.preflight.safe4-full.v1";
    public static final String BUNDLE_PHASE_VISIBILITY_SAFE4_FULL_V1 = "bundle.phase-visibility.safe4-full.v1";
    public static final String STATUS_GLOSSARY_PRONOUN_SAFE4_FULL_V1 = "status.glossary-pronoun.safe4-full.v1";
    public static final String LEDGER_EXHAUSTIVE_SAFE4_FULL_V1 = "ledger.exhaustive.safe4-full.v1";
    public static final String PRESERVE_DRAFT_SAFE4_FULL_V1 = "preserve.draft.safe4-full.v1";
    public static final String STOP_TYPED_SAFE4_FULL_V1 = "stop.typed.safe4-full.v1";
    public static final String DIFF_CHANGE_COVERAGE_V1 = "diff.change-coverage.v1";
    public static final String QA_L3_TWO_ADVERSARIAL_V1 = "qa.l3-two-adversarial.v1";
    public static final String RELEASE_SAFE4_FULL_V1 = "release.safe4-full.v1";
    public static final String REPLAY_SAFE4_G1_G24_V1 = "replay.safe4.g1-g24.v1";
    public static final Set<String> KNOWN_SAFE4_CAPABILITIES = Set.of(
            PACK_INTEGRITY_SHA256_V1,
            SOURCE_PREFLIGHT_SAFE4_FULL_V1,
            BUNDLE_PHASE_VISIBILITY_SAFE4_FULL_V1,
            STATUS_GLOSSARY_PRONOUN_SAFE4_FULL_V1,
            LEDGER_EXHAUSTIVE_SAFE4_FULL_V1,
            PRESERVE_DRAFT_SAFE4_FULL_V1,
            STOP_TYPED_SAFE4_FULL_V1,
            DIFF_CHANGE_COVERAGE_V1,
            QA_L3_TWO_ADVERSARIAL_V1,
            RELEASE_SAFE4_FULL_V1,
            REPLAY_SAFE4_G1_G24_V1,
            "lineage.exact-parent.v1",
            "ledger.exhaustive.safe4.v1",
            "gate.derived.safe4.v1",
            "context.pronoun-pair.safe4.v1",
            "barrier.l1-raw-first.v1",
            "release.safe4.v1",
            "replay.safe4.g1-g10.v1");

    private final Set<String> recognizedCapabilities;
    private final Set<String> implementationEvidenceCapabilities;
    private final Map<String, EvidenceDescriptor> evidenceDescriptors;

    private EditorialEngineContractCapabilityEvidenceCatalog(Set<String> recognizedCapabilities,
                                                             Set<String> implementationEvidenceCapabilities,
                                                             Map<String, EvidenceDescriptor> evidenceDescriptors) {
        this.recognizedCapabilities = Collections.unmodifiableSet(new LinkedHashSet<>(recognizedCapabilities));
        this.implementationEvidenceCapabilities = Collections.unmodifiableSet(new LinkedHashSet<>(implementationEvidenceCapabilities));
        this.evidenceDescriptors = Collections.unmodifiableMap(new LinkedHashMap<>(evidenceDescriptors));
    }

    public static EditorialEngineContractCapabilityEvidenceCatalog production() {
        Map<String, EvidenceDescriptor> descriptors = p3bDescriptors();
        return new EditorialEngineContractCapabilityEvidenceCatalog(
                KNOWN_SAFE4_CAPABILITIES, descriptors.keySet(), descriptors);
    }

    public static EditorialEngineContractCapabilityEvidenceCatalog testOnly(Set<String> capabilityIds) {
        if (capabilityIds == null || capabilityIds.isEmpty()) throw new IllegalArgumentException("Test capability catalog is empty");
        for (String capabilityId : capabilityIds) {
            if (capabilityId == null || !capabilityId.startsWith("test.")) {
                throw new IllegalArgumentException("Test catalog accepts only test.* capabilities");
            }
        }
        return new EditorialEngineContractCapabilityEvidenceCatalog(capabilityIds, capabilityIds, Map.of());
    }

    public boolean recognizes(String capabilityId) {
        return recognizedCapabilities.contains(capabilityId);
    }

    public boolean confirmsImplemented(String capabilityId) {
        return implementationEvidenceCapabilities.contains(capabilityId);
    }

    public Set<String> recognizedCapabilities() {
        return recognizedCapabilities;
    }

    public java.util.Optional<EvidenceDescriptor> evidenceDescriptor(String capabilityId) {
        return java.util.Optional.ofNullable(evidenceDescriptors.get(capabilityId));
    }

    public record EvidenceDescriptor(String capabilityId, String productionOwner,
                                     String positiveTest, String negativeTest,
                                     String evidenceFingerprint) {
        public EvidenceDescriptor {
            if (capabilityId == null || productionOwner == null || positiveTest == null
                    || negativeTest == null || evidenceFingerprint == null) {
                throw new NullPointerException("Evidence descriptor fields are required");
            }
        }
    }

    private static Map<String, EvidenceDescriptor> p3bDescriptors() {
        LinkedHashMap<String, EvidenceDescriptor> result = new LinkedHashMap<>();
        descriptor(result, EditorialSafe4Contract.PACK_INTEGRITY_SHA256_V1,
                "EditorialPackIntegrityValidator", "EditorialP2ReferencePackImportInstrumentedTest",
                "EditorialPackIntegrityValidatorTest");
        descriptor(result, EditorialSafe4Contract.SOURCE_PREFLIGHT_SAFE4_FULL_V1,
                "EditorialSourcePreflight", "EditorialSourcePreflightTest.P01-P09",
                "EditorialSourcePreflightTest.P05-P07");
        descriptor(result, EditorialSafe4Contract.BUNDLE_PHASE_VISIBILITY_SAFE4_FULL_V1,
                "EditorialPhaseContextProjector", "EditorialPhaseContextProjectorTest",
                "EditorialPhaseContextProjectorTest.fullBundleRejectsDuplicateRoleAndSourceId");
        descriptor(result, EditorialSafe4Contract.STATUS_GLOSSARY_PRONOUN_SAFE4_FULL_V1,
                "EditorialSourceStatusResolver", "EditorialPhaseContextProjectorTest.noneStatusIsRetainedWhenPronounIsHidden",
                "EditorialPhaseContextProjectorTest.fullBundleRejectsDuplicateRoleAndSourceId");
        descriptor(result, EditorialSafe4Contract.LEDGER_EXHAUSTIVE_SAFE4_FULL_V1,
                "EditorialLedgerValidator", "EditorialReceiptAndCoverageValidatorTest.ledgerIsExhaustiveAndDoesNotTrustModelPass",
                "EditorialReceiptAndCoverageValidatorTest.ledgerIsExhaustiveAndDoesNotTrustModelPass");
        descriptor(result, EditorialSafe4Contract.PRESERVE_DRAFT_SAFE4_FULL_V1,
                "EditorialStopDecision + EditorialReceiptValidator", "EditorialReceiptAndCoverageValidatorTest.preserveDraftCannotBecomeCanonOrPropagation",
                "EditorialReceiptAndCoverageValidatorTest.preserveDraftCannotBecomeCanonOrPropagation");
        descriptor(result, EditorialSafe4Contract.STOP_TYPED_SAFE4_FULL_V1,
                "EditorialStopDecision", "EditorialStopDecisionTest.inputMissingIsTypedAndHasRecovery",
                "EditorialStopDecisionTest.schemaFailureIsRepairNotContentConflict");
        descriptor(result, EditorialSafe4Contract.DIFF_CHANGE_COVERAGE_V1,
                "EditorialDiffValidator", "EditorialReceiptAndCoverageValidatorTest.actualDiffMustMatchDeclaredErrorMapping",
                "EditorialReceiptAndCoverageValidatorTest.actualDiffMustMatchDeclaredErrorMapping");
        descriptor(result, EditorialSafe4Contract.QA_L3_TWO_ADVERSARIAL_V1,
                "EditorialQaValidator", "EditorialReceiptAndCoverageValidatorTest.qaRequiresTwoIndependentPassesAndReleaseIsFailClosed",
                "EditorialReceiptAndCoverageValidatorTest.qaRequiresTwoIndependentPassesAndReleaseIsFailClosed");
        descriptor(result, EditorialSafe4Contract.RELEASE_SAFE4_FULL_V1,
                "EditorialReleaseValidator", "EditorialReceiptAndCoverageValidatorTest.qaRequiresTwoIndependentPassesAndReleaseIsFailClosed",
                "EditorialReceiptAndCoverageValidatorTest.qaRequiresTwoIndependentPassesAndReleaseIsFailClosed");
        descriptor(result, EditorialSafe4Contract.REPLAY_SAFE4_G1_G24_V1,
                "EditorialSafe4ReplayRunner", "EditorialSafe4ReplayRunnerTest.G1ThroughG24ExecuteWithDeterministicExpectedDecisions",
                "EditorialSafe4ReplayRunnerTest.replayCaseRequiresDecisionReasonGateAndEquation");
        return result;
    }

    private static void descriptor(Map<String, EvidenceDescriptor> result, String capabilityId,
                                   String owner, String positiveTest, String negativeTest) {
        result.put(capabilityId, new EvidenceDescriptor(capabilityId, owner, positiveTest, negativeTest,
                EditorialSafe4Contract.evidenceFingerprint(capabilityId, owner, positiveTest, negativeTest)));
    }
}
