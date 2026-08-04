package com.ml.tblandroidtxt.editorial.pack;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Code-owned capability allow-list boundary for profile validation.
 *
 * <p>This is not a bundled profile and is not a certification evidence store.
 * The production catalog knows the SAFE4 capability IDs so missing capability
 * declarations can be checked, but confirms implementation evidence only for
 * the pack-integrity capability. Test-only catalogs must be explicitly
 * constructed and may contain only {@code test.*} identifiers.</p>
 */
public final class EditorialEngineContractCapabilityEvidenceCatalog {
    public static final String PACK_INTEGRITY_SHA256_V1 = "pack.integrity.sha256.v1";
    public static final Set<String> KNOWN_SAFE4_CAPABILITIES = Set.of(
            PACK_INTEGRITY_SHA256_V1,
            "lineage.exact-parent.v1",
            "ledger.exhaustive.safe4.v1",
            "gate.derived.safe4.v1",
            "context.pronoun-pair.safe4.v1",
            "barrier.l1-raw-first.v1",
            "diff.change-coverage.v1",
            "qa.l3-two-adversarial.v1",
            "release.safe4.v1",
            "replay.safe4.g1-g10.v1");

    private final Set<String> recognizedCapabilities;
    private final Set<String> implementationEvidenceCapabilities;

    private EditorialEngineContractCapabilityEvidenceCatalog(Set<String> recognizedCapabilities,
                                                             Set<String> implementationEvidenceCapabilities) {
        this.recognizedCapabilities = Collections.unmodifiableSet(new LinkedHashSet<>(recognizedCapabilities));
        this.implementationEvidenceCapabilities = Collections.unmodifiableSet(new LinkedHashSet<>(implementationEvidenceCapabilities));
    }

    public static EditorialEngineContractCapabilityEvidenceCatalog production() {
        return new EditorialEngineContractCapabilityEvidenceCatalog(
                KNOWN_SAFE4_CAPABILITIES, Set.of(PACK_INTEGRITY_SHA256_V1));
    }

    public static EditorialEngineContractCapabilityEvidenceCatalog testOnly(Set<String> capabilityIds) {
        if (capabilityIds == null || capabilityIds.isEmpty()) throw new IllegalArgumentException("Test capability catalog is empty");
        for (String capabilityId : capabilityIds) {
            if (capabilityId == null || !capabilityId.startsWith("test.")) {
                throw new IllegalArgumentException("Test catalog accepts only test.* capabilities");
            }
        }
        return new EditorialEngineContractCapabilityEvidenceCatalog(capabilityIds, capabilityIds);
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
}
