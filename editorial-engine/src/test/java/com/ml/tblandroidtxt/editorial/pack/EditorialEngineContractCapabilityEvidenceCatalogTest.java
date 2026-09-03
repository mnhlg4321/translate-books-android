package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EditorialEngineContractCapabilityEvidenceCatalogTest {
    @Test public void productionCatalogConfirmsOnlyCodeOwnedP3BCapabilities() {
        EditorialEngineContractCapabilityEvidenceCatalog catalog =
                EditorialEngineContractCapabilityEvidenceCatalog.production();
        assertEquals(EditorialSafe4Contract.IMPLEMENTED_CAPABILITY_IDS.size(),
                EditorialSafe4Contract.IMPLEMENTED_CAPABILITY_IDS.stream()
                        .filter(catalog::confirmsImplemented).count());
        for (String capability : EditorialSafe4Contract.IMPLEMENTED_CAPABILITY_IDS) {
            EditorialEngineContractCapabilityEvidenceCatalog.EvidenceDescriptor descriptor =
                    catalog.evidenceDescriptor(capability).orElseThrow();
            assertEquals(capability, descriptor.capabilityId());
            assertTrue(descriptor.evidenceFingerprint().matches("[0-9a-f]{64}"));
        }
        assertTrue(catalog.recognizes("lineage.exact-parent.v1"));
        assertTrue(!catalog.confirmsImplemented("lineage.exact-parent.v1"));
    }
}
