package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EditorialSafe4TrustedProfileFactoryTest {
    private static final String COMMIT = "b5589bf5f2b3b60942841950c5877e6f8281f7ff";
    private static final String CREATED = "2026-09-03T00:00:00+07:00";

    @Test public void factoryCreatesFullyEvidencedExecutableContractShape() {
        EditorialEngineContractProfile profile = EditorialSafe4TrustedProfileFactory.create(COMMIT, CREATED);
        EditorialEngineContractProfileValidationResult result =
                new EditorialEngineContractProfileValidator().validate(profile);
        assertTrue(result.issues().toString(), result.isValid());
        assertEquals(EditorialSafe4Contract.CONTRACT_VERSION, profile.minimumSupportedContractVersion());
        assertEquals(EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION, profile.supportedSchemaVersions().get(0));
        assertEquals(EditorialSafe4Contract.PHASES, profile.supportedPhaseGraph().phases());
        assertEquals(EditorialSafe4Contract.IMPLEMENTED_CAPABILITY_IDS.size(), profile.capabilityEvidence().size());
        assertEquals(0, profile.explicitlyMissingCapabilities().size());
        assertEquals("ACTIVE", profile.deprecationPolicy().state());
        assertTrue(!profile.deprecationPolicy().automaticProjectRebind());
    }

    @Test(expected = IllegalArgumentException.class)
    public void sourceCommitMustLookLikeExistingImmutableEvidenceReference() {
        EditorialSafe4TrustedProfileFactory.create("not-a-commit", CREATED);
    }
}
