package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EditorialCompatibilityEvaluationContextTest {
    private static final String A = "a".repeat(64);
    private static final String B = "b".repeat(64);
    private static final String C = "c".repeat(64);
    private static final String D = "d".repeat(64);
    private static final String E = "e".repeat(64);

    @Test public void contextFingerprintIsDeterministicAndTimestampIndependent() {
        EditorialCompatibilityEvaluationContext first = context();
        EditorialCompatibilityEvaluationContext second = context();

        assertEquals(first.canonicalJson(), second.canonicalJson());
        assertEquals(first.fingerprint(), second.fingerprint());
        assertFalse(first.canonicalJson().contains("evaluatedAt"));
        assertFalse(first.canonicalJson().contains("prompt"));
        assertFalse(first.canonicalJson().contains("C:\\Users"));
    }

    @Test public void canonicalJsonDoesNotDependOnConstructionOrderOrWhitespace() {
        EditorialCompatibilityEvaluationContext context = new EditorialCompatibilityEvaluationContext(
                A, "profile.id", "1.0.0", B, "4.16-dev.30", C,
                "editorial-compatibility-v1", D, E);
        assertEquals("{" +
                "\"adapterSetFingerprint\":\"" + D + "\"," +
                "\"canonicalPackHash\":\"" + A + "\"," +
                "\"canonicalProfileHash\":\"" + B + "\"," +
                "\"capabilityFingerprint\":\"" + E + "\"," +
                "\"engineVersion\":\"4.16-dev.30\"," +
                "\"evaluatorContractVersion\":\"editorial-compatibility-v1\"," +
                "\"machineContractFingerprint\":\"" + C + "\"," +
                "\"trustedProfileId\":\"profile.id\"," +
                "\"trustedProfileVersion\":\"1.0.0\"}", context.canonicalJson());
    }

    @Test public void contextRejectsNonSemanticOrMalformedHashes() {
        try { new EditorialCompatibilityEvaluationContext("not-a-hash", "id", "1", B, "engine", C, "eval", D, E); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        try { new EditorialCompatibilityEvaluationContext(A, "id", "1", B, "engine", C, "eval", D, "secret"); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
    }

    private static EditorialCompatibilityEvaluationContext context() {
        return new EditorialCompatibilityEvaluationContext(
                A, "profile.id", "1.0.0", B, "4.16-dev.30", C,
                "editorial-compatibility-v1", D, E);
    }
}
