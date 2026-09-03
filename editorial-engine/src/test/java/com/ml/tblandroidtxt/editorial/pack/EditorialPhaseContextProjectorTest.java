package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditorialPhaseContextProjectorTest {
    private final EditorialPhaseContextProjector projector = new EditorialPhaseContextProjector();

    @Test public void hiddenAssetsRemainInFullBundleButAreNotMissingInRawDiscovery() {
        EditorialPhaseContextProjector.Bundle bundle = bundle(true, true);
        EditorialPhaseContextProjector.FullBundleValidation full = projector.validateFullBundle(bundle);
        EditorialPhaseContextProjector.PhaseProjection projection = projector.project(bundle, "L1_RAW_DISCOVERY");
        assertTrue(full.valid());
        assertEquals(full.bundleIdentity(), projection.bundleIdentity());
        assertTrue(projection.isVisible(EditorialSafe4Contract.RAW));
        assertTrue(projection.isVisible(EditorialSafe4Contract.GLOSSARY));
        assertFalse(projection.isVisible(EditorialSafe4Contract.DRAFT));
        assertTrue(projection.hiddenRoles().contains(EditorialSafe4Contract.DRAFT));
        assertTrue(projection.hiddenRoles().contains(EditorialSafe4Contract.PRONOUN));
    }

    @Test public void pairContextIsOptionalAndDoesNotBecomeAuthority() {
        EditorialPhaseContextProjector.Bundle withoutPair = bundle(false, true);
        EditorialPhaseContextProjector.Bundle withPair = bundle(true, true);
        assertTrue(projector.validateFullBundle(withoutPair).valid());
        assertTrue(projector.validateFullBundle(withPair).valid());
        assertFalse(withPair.assets().stream().filter(a -> EditorialSafe4Contract.PAIR_CONTEXT.equals(a.role()))
                .findFirst().orElseThrow().authoritative());
        assertTrue(projector.project(withoutPair, "L1_RECONCILE").pairContextOptional());
        assertEquals(EditorialSourceStatusResolver.PronounStatus.AVAILABLE,
                projector.project(withoutPair, "L1_RAW_DISCOVERY").pronounStatus());
    }

    @Test public void fullBundleRejectsDuplicateRoleAndSourceId() {
        List<EditorialPhaseContextProjector.BundleAsset> assets = new ArrayList<>(bundle(false, true).assets());
        assets.add(new EditorialPhaseContextProjector.BundleAsset(
                EditorialSafe4Contract.RAW, "raw", bytes("duplicate"), true, "text.v1"));
        EditorialPhaseContextProjector.FullBundleValidation result = projector.validateFullBundle(
                new EditorialPhaseContextProjector.Bundle(assets, availablePronoun()));
        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(value -> value.startsWith("BUNDLE_DUPLICATE_SOURCE_ID")));
    }

    @Test public void noneStatusIsRetainedWhenPronounIsHidden() {
        EditorialPhaseContextProjector.Bundle bundle = bundle(false, false);
        assertTrue(projector.validateFullBundle(bundle).valid());
        EditorialPhaseContextProjector.PhaseProjection projection = projector.project(bundle, "L1_SOURCE_PREFLIGHT");
        assertEquals(EditorialSourceStatusResolver.PronounStatus.NONE, projection.pronounStatus());
        assertFalse(projection.isVisible(EditorialSafe4Contract.PAIR_CONTEXT));
    }

    private static EditorialPhaseContextProjector.Bundle bundle(boolean pair, boolean pronounAvailable) {
        List<EditorialPhaseContextProjector.BundleAsset> assets = new ArrayList<>();
        assets.add(asset(EditorialSafe4Contract.RAW, "raw", "raw"));
        assets.add(asset(EditorialSafe4Contract.DRAFT, "draft", "draft"));
        assets.add(asset(EditorialSafe4Contract.GLOSSARY, "glossary", "term\ttarget"));
        if (pronounAvailable) assets.add(asset(EditorialSafe4Contract.PRONOUN, "pronoun", "from\ttarget"));
        if (pair) assets.add(new EditorialPhaseContextProjector.BundleAsset(
                EditorialSafe4Contract.PAIR_CONTEXT, "pair", bytes("optional"), false, "pair.v1"));
        EditorialSourceStatusResolver.Result status = pronounAvailable ? availablePronoun()
                : new EditorialSourceStatusResolver().resolve(
                EditorialSourceStatusResolver.Selection.none("user-explicit-none"));
        return new EditorialPhaseContextProjector.Bundle(assets, status);
    }

    private static EditorialSourceStatusResolver.Result availablePronoun() {
        return new EditorialSourceStatusResolver().resolve(
                EditorialSourceStatusResolver.Selection.available("user-selected", new EditorialSourceStatusResolver.PronounCandidate(
                        "pronoun", bytes("from\ttarget"), true, false)));
    }

    private static EditorialPhaseContextProjector.BundleAsset asset(String role, String id, String text) {
        return new EditorialPhaseContextProjector.BundleAsset(role, id, bytes(text), true, "text.v1");
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
