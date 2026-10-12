package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5RawWireContract;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class EditorialP5EReconcileEntryTest {
    private static final class Spec {
        String binding = EditorialP5EFreshRawLiveRunner.BINDING_IDENTITY;
        String run = EditorialP5EFreshRawLiveRunner.RUN_DECLARATION_IDENTITY;
        String pack = EditorialP5EFreshRawLiveRunner.CANONICAL_PACK_HASH;
        String profile = EditorialP5EFreshRawLiveRunner.CANONICAL_PROFILE_HASH;
        String evaluation = EditorialP5EFreshRawLiveRunner.COMPATIBILITY_EVALUATION_ID;
        String chapter = EditorialP5EFreshRawLiveRunner.CHAPTER_KEY;
        String phase = "L1_RECONCILE";
        String provider = EditorialP5EFreshRawRoutingPolicy.PROVIDER;
        String model = EditorialP5EFreshRawRoutingPolicy.MODEL;
        int primary = 1;
        int repair = 0;
        int retries = 0;
        int outputTokens = EditorialP5RawWireContract.OUTPUT_TOKEN_CAP;
        boolean chapterToProvider = true;
        boolean storeResponse = false;
        boolean storeRequest = false;
        boolean singleUse = true;

        EditorialP5PilotAuthorization build() {
            return new EditorialP5PilotAuthorization("auth", binding, run, pack, profile,
                    evaluation, chapter, phase, provider, model, "endpoint-account",
                    primary, repair, retries, 50000, outputTokens, 60000,
                    BigDecimal.valueOf(0.10), 300000L, chapterToProvider, storeResponse,
                    storeRequest, "HASH_ONLY", "USER", 1L, 2L, singleUse);
        }
    }

    private static boolean matches(Spec spec) {
        return EditorialP5EFreshRawLiveRunner.reconcileAuthorizationMatches(spec.build());
    }

    @Test public void validAuthorizationMatches() {
        assertTrue(matches(new Spec()));
    }

    @Test public void eachSingleDeviationIsRejected() {
        Spec s;
        s = new Spec(); s.phase = "L1_RAW_DISCOVERY"; assertFalse("phase RAW", matches(s));
        s = new Spec(); s.singleUse = false; assertFalse("singleUse", matches(s));
        s = new Spec(); s.chapterToProvider = false; assertFalse("chapterToProvider", matches(s));
        s = new Spec(); s.primary = 2; assertFalse("primary calls", matches(s));
        s = new Spec(); s.repair = 1; assertFalse("repair", matches(s));
        s = new Spec(); s.retries = 1; assertFalse("retries", matches(s));
        s = new Spec(); s.outputTokens = EditorialP5RawWireContract.OUTPUT_TOKEN_CAP + 1;
        assertFalse("output cap high", matches(s));
        s = new Spec(); s.outputTokens = EditorialP5RawWireContract.OUTPUT_TOKEN_CAP - 1;
        assertFalse("output cap low", matches(s));
        s = new Spec(); s.storeResponse = true; assertFalse("response storage", matches(s));
        s = new Spec(); s.storeRequest = true; assertFalse("request storage", matches(s));
        s = new Spec(); s.provider = "other"; assertFalse("provider", matches(s));
        s = new Spec(); s.model = "other/model"; assertFalse("model", matches(s));
        s = new Spec(); s.binding = "x".repeat(64); assertFalse("binding", matches(s));
        s = new Spec(); s.run = "x".repeat(64); assertFalse("run declaration", matches(s));
        s = new Spec(); s.pack = "x".repeat(64); assertFalse("pack hash", matches(s));
        s = new Spec(); s.profile = "x".repeat(64); assertFalse("profile hash", matches(s));
        s = new Spec(); s.evaluation = "other"; assertFalse("evaluation", matches(s));
        s = new Spec(); s.chapter = "002"; assertFalse("chapter", matches(s));
    }
}
