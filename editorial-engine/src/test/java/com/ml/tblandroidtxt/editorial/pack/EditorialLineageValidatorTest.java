package com.ml.tblandroidtxt.editorial.pack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

public final class EditorialLineageValidatorTest {
    private static final String PACK = "a".repeat(64);
    private static final String PROFILE_HASH = "b".repeat(64);
    private static final String MACHINE = "c".repeat(64);
    private static final String ROOT_INPUT_HASH = "d".repeat(64);
    private static final String DRAFT_INPUT_HASH = "e".repeat(64);
    private static final EditorialLineageValidator VALIDATOR = new EditorialLineageValidator();

    @Test public void validRootAndChildUseExactParent() {
        EditorialLineageRecord root = root("run-root");
        EditorialLineageRecord child = child("run-child", root, baseManifest());

        assertValid(root, TestContext.of());
        assertValid(child, TestContext.of(root));
        assertEquals(root.recordIdentity(), child.parentReference().parentRecordIdentity());
        assertEquals(root.recordFingerprint(), child.parentReference().parentRecordFingerprint());
    }

    @Test public void canonicalizationIsDeterministicAndIgnoresFieldOrderWhitespaceAndLocale() {
        EditorialLineageInputManifest first = new EditorialLineageInputManifest("input-manifest-v1", List.of(
                new EditorialLineageInputEntry("DRAFT", 0, DRAFT_INPUT_HASH, 100, 1),
                new EditorialLineageInputEntry("RAW", 0, ROOT_INPUT_HASH, 100, 1)));
        EditorialLineageInputManifest second = new EditorialLineageInputManifest("input-manifest-v1", List.of(
                new EditorialLineageInputEntry("RAW", 0, ROOT_INPUT_HASH, 100, 1),
                new EditorialLineageInputEntry("DRAFT", 0, DRAFT_INPUT_HASH, 100, 1)));
        EditorialLineageIdentity firstIdentity = identity("run-deterministic", EditorialLineageNodeKind.ROOT, first);
        EditorialLineageIdentity secondIdentity = identity("run-deterministic", EditorialLineageNodeKind.ROOT, second);
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(EditorialLineageCanonicalizer.canonicalInputManifestJson(first),
                    EditorialLineageCanonicalizer.canonicalInputManifestJson(second));
            assertEquals(EditorialLineageCanonicalizer.recordIdentity(firstIdentity),
                    EditorialLineageCanonicalizer.recordIdentity(secondIdentity));
            assertEquals(EditorialLineageCanonicalizer.recordIdentity(firstIdentity),
                    EditorialLineageCanonicalizer.recordIdentity(firstIdentity));
        } finally {
            Locale.setDefault(original);
        }
        assertFalse(EditorialLineageCanonicalizer.canonicalIdentityJson(firstIdentity).contains("\ufeff"));
    }

    @Test public void recordIdentityExcludesParentButRecordFingerprintBindsParent() {
        EditorialLineageRecord firstParent = root("run-parent-1");
        EditorialLineageRecord secondParent = root("run-parent-2");
        EditorialLineageIdentity childIdentity = identity("run-child", EditorialLineageNodeKind.CHILD, baseManifest());
        EditorialLineageRecord first = EditorialLineageRecord.create(childIdentity,
                new EditorialLineageParentReference(firstParent.recordIdentity(), firstParent.recordFingerprint()));
        EditorialLineageRecord second = EditorialLineageRecord.create(childIdentity,
                new EditorialLineageParentReference(secondParent.recordIdentity(), secondParent.recordFingerprint()));

        assertEquals(first.recordIdentity(), second.recordIdentity());
        assertNotEquals(first.recordFingerprint(), second.recordFingerprint());
    }

    @Test public void oneByteParentMutationFailsClosed() {
        EditorialLineageRecord root = root("run-root");
        EditorialLineageRecord child = EditorialLineageRecord.create(
                identity("run-child", EditorialLineageNodeKind.CHILD, baseManifest()),
                new EditorialLineageParentReference(root.recordIdentity(), mutate(root.recordFingerprint())));

        assertHasCode(child, TestContext.of(root), EditorialLineageValidationCode.PARENT_MISMATCH);
    }

    @Test public void crossPackProfileProjectAndInputScopeFailClosed() {
        EditorialLineageRecord root = root("run-root");
        assertHasCode(childWith(root, identity("run-cross-pack", EditorialLineageNodeKind.CHILD,
                        baseManifest(), mutate(PACK), root.identity().trustedProfileId(),
                        root.identity().trustedProfileVersion(), root.identity().canonicalProfileHash(),
                        root.identity().machineContractFingerprint(), root.identity().projectIdentity(),
                        root.identity().inputScopeIdentity())), TestContext.of(root),
                EditorialLineageValidationCode.CROSS_PACK);
        assertHasCode(childWith(root, identity("run-cross-profile", EditorialLineageNodeKind.CHILD,
                        baseManifest(), PACK, "profile-other", "v1", "f".repeat(64), MACHINE,
                        "project-1", "chapter-001")), TestContext.of(root),
                EditorialLineageValidationCode.CROSS_PROFILE);
        assertHasCode(childWith(root, identity("run-cross-project", EditorialLineageNodeKind.CHILD,
                        baseManifest(), PACK, "profile-safe4", "v1", PROFILE_HASH, MACHINE,
                        "project-other", "chapter-001")), TestContext.of(root),
                EditorialLineageValidationCode.CROSS_PROJECT);
        assertHasCode(childWith(root, identity("run-cross-scope", EditorialLineageNodeKind.CHILD,
                        baseManifest(), PACK, "profile-safe4", "v1", PROFILE_HASH, MACHINE,
                        "project-1", "chapter-002")), TestContext.of(root),
                EditorialLineageValidationCode.CROSS_INPUT_SCOPE);
    }

    @Test public void changedManifestAndSameRunInputMutationFailClosed() {
        EditorialLineageRecord root = root("run-root");
        EditorialLineageRecord changedChild = child("run-child", root, changedManifest());
        assertHasCode(changedChild, TestContext.of(root), EditorialLineageValidationCode.MANIFEST_MISMATCH);

        EditorialLineageRecord original = child("run-same", root, baseManifest());
        EditorialLineageRecord changed = child("run-same", root, changedManifest());
        assertHasCode(changed, TestContext.of(root, original), EditorialLineageValidationCode.MANIFEST_MISMATCH);
    }

    @Test public void missingParentRootParentAndChildWithoutParentAreRejected() {
        EditorialLineageIdentity childIdentity = identity("run-child", EditorialLineageNodeKind.CHILD, baseManifest());
        EditorialLineageRecord missing = EditorialLineageRecord.create(childIdentity,
                new EditorialLineageParentReference("f".repeat(64), "f".repeat(64)));
        assertHasCode(missing, TestContext.of(), EditorialLineageValidationCode.MISSING_PARENT);

        EditorialLineageRecord root = root("run-root");
        EditorialLineageRecord rootWithParent = EditorialLineageRecord.create(
                identity("run-root-parent", EditorialLineageNodeKind.ROOT, baseManifest()),
                new EditorialLineageParentReference(root.recordIdentity(), root.recordFingerprint()));
        assertHasCode(rootWithParent, TestContext.of(root), EditorialLineageValidationCode.ROOT_HAS_PARENT);

        EditorialLineageRecord childWithoutParent = EditorialLineageRecord.create(childIdentity, null);
        assertHasCode(childWithoutParent, TestContext.of(), EditorialLineageValidationCode.MISSING_PARENT);
    }

    @Test public void reparentDuplicateAndDifferentFingerprintFailClosed() {
        EditorialLineageRecord firstParent = root("run-parent-1");
        EditorialLineageRecord secondParent = root("run-parent-2");
        EditorialLineageIdentity identity = identity("run-reparent", EditorialLineageNodeKind.CHILD, baseManifest());
        EditorialLineageRecord original = childWith(firstParent, identity);
        EditorialLineageRecord reparented = childWith(secondParent, identity);
        EditorialLineageValidationResult result = VALIDATOR.validate(reparented, TestContext.of(firstParent, secondParent, original));
        assertEquals(List.of(EditorialLineageValidationCode.PARENT_MISMATCH,
                        EditorialLineageValidationCode.DUPLICATE_LINEAGE,
                        EditorialLineageValidationCode.REPARENT_ATTEMPT,
                        EditorialLineageValidationCode.IDENTITY_FINGERPRINT_MISMATCH), result.failureCodes());
        assertHasCode(result, EditorialLineageValidationCode.DUPLICATE_LINEAGE);
        assertHasCode(result, EditorialLineageValidationCode.REPARENT_ATTEMPT);
        assertHasCode(result, EditorialLineageValidationCode.PARENT_MISMATCH);

        EditorialLineageFingerprint wrongFingerprint = new EditorialLineageFingerprint(
                original.inputManifestFingerprint(), original.recordIdentity(), mutate(original.recordFingerprint()));
        EditorialLineageRecord duplicateDifferentBytes = EditorialLineageRecord.fromDeclared(
                original.identity(), original.parentReference(), wrongFingerprint);
        EditorialLineageValidationResult duplicateResult = VALIDATOR.validate(
                original, TestContext.of(firstParent, duplicateDifferentBytes));
        assertHasCode(duplicateResult, EditorialLineageValidationCode.DUPLICATE_LINEAGE);
        assertHasCode(duplicateResult, EditorialLineageValidationCode.IDENTITY_FINGERPRINT_MISMATCH);
    }

    @Test public void orphanAmbiguousForkAndCycleFailClosed() {
        EditorialLineageRecord root = root("run-root");
        EditorialLineageRecord orphanParent = EditorialLineageRecord.create(
                identity("run-orphan-parent", EditorialLineageNodeKind.CHILD, baseManifest()), null);
        EditorialLineageRecord orphanChild = childWith(root,
                identity("run-orphan-child", EditorialLineageNodeKind.CHILD, baseManifest()));
        orphanChild = EditorialLineageRecord.create(orphanChild.identity(),
                new EditorialLineageParentReference(orphanParent.recordIdentity(), orphanParent.recordFingerprint()));
        assertHasCode(orphanChild, TestContext.of(root, orphanParent), EditorialLineageValidationCode.ORPHAN_LINEAGE);

        EditorialLineageRecord ambiguousRoot = root("run-ambiguous");
        EditorialLineageRecord ambiguousChild = child("run-ambiguous-child", ambiguousRoot, baseManifest());
        assertHasCode(ambiguousChild, TestContext.of(ambiguousRoot, ambiguousRoot),
                EditorialLineageValidationCode.AMBIGUOUS_PARENT);

        EditorialLineageRecord existingChild = child("run-existing-child", root, baseManifest());
        EditorialLineageRecord fork = child("run-fork-child", root, baseManifest());
        assertHasCode(fork, TestContext.of(root, existingChild), EditorialLineageValidationCode.FORK_NOT_ALLOWED);

        EditorialLineageIdentity cycleAIdentity = identity("run-cycle-a", EditorialLineageNodeKind.CHILD, baseManifest());
        EditorialLineageIdentity cycleBIdentity = identity("run-cycle-b", EditorialLineageNodeKind.CHILD, baseManifest());
        EditorialLineageRecord cycleA = EditorialLineageRecord.fromDeclared(cycleAIdentity,
                new EditorialLineageParentReference(EditorialLineageCanonicalizer.recordIdentity(cycleBIdentity),
                        "f".repeat(64)),
                new EditorialLineageFingerprint(
                        EditorialLineageCanonicalizer.inputManifestFingerprint(baseManifest()),
                        EditorialLineageCanonicalizer.recordIdentity(cycleAIdentity), "f".repeat(64)));
        EditorialLineageRecord cycleB = EditorialLineageRecord.fromDeclared(cycleBIdentity,
                new EditorialLineageParentReference(EditorialLineageCanonicalizer.recordIdentity(cycleAIdentity),
                        "f".repeat(64)),
                new EditorialLineageFingerprint(
                        EditorialLineageCanonicalizer.inputManifestFingerprint(baseManifest()),
                        EditorialLineageCanonicalizer.recordIdentity(cycleBIdentity), "f".repeat(64)));
        EditorialLineageRecord cycleCandidate = EditorialLineageRecord.create(
                identity("run-cycle-candidate", EditorialLineageNodeKind.CHILD, baseManifest()),
                new EditorialLineageParentReference(cycleA.recordIdentity(), cycleA.recordFingerprint()));
        assertHasCode(cycleCandidate, TestContext.of(cycleA, cycleB), EditorialLineageValidationCode.CYCLE_DETECTED);
        assertHasCode(cycleCandidate, TestContext.of(cycleA, cycleB), EditorialLineageValidationCode.ORPHAN_LINEAGE);

        EditorialLineageRecord self = EditorialLineageRecord.create(
                identity("run-self", EditorialLineageNodeKind.CHILD, baseManifest()), null);
        self = EditorialLineageRecord.create(self.identity(),
                new EditorialLineageParentReference(self.recordIdentity(), self.recordFingerprint()));
        assertHasCode(self, TestContext.of(self), EditorialLineageValidationCode.SELF_PARENT);
    }

    @Test public void staleProfileAndInvalidHashFailClosed() {
        EditorialLineageRecord root = root("run-root");
        TestContext context = TestContext.of(root).withTrust(new EditorialLineageTrustContext(
                "profile-safe4", "v1", PROFILE_HASH, "d".repeat(64)));
        assertHasCode(root, context, EditorialLineageValidationCode.STALE_PROFILE);

        EditorialLineageRecord invalidHash = EditorialLineageRecord.create(
                identity("run-invalid", EditorialLineageNodeKind.ROOT, baseManifest(), "Z".repeat(64),
                        "profile-safe4", "v1", PROFILE_HASH, MACHINE, "project-1", "chapter-001"), null);
        assertHasCode(invalidHash, TestContext.of(), EditorialLineageValidationCode.INVALID_HASH);
    }

    @Test public void declaredMutationAndStableRepeatedValidationAreDeterministic() {
        EditorialLineageRecord root = root("run-root");
        EditorialLineageFingerprint wrong = new EditorialLineageFingerprint(
                mutate(root.inputManifestFingerprint()), root.recordIdentity(), root.recordFingerprint());
        EditorialLineageRecord tampered = EditorialLineageRecord.fromDeclared(root.identity(), null, wrong);
        EditorialLineageValidationResult first = VALIDATOR.validate(tampered, TestContext.of());
        EditorialLineageValidationResult second = VALIDATOR.validate(tampered, TestContext.of());
        assertEquals(first.issues(), second.issues());
        assertHasCode(first, EditorialLineageValidationCode.IDENTITY_FINGERPRINT_MISMATCH);
    }

    @Test public void strictUtf8RejectsBomAndMalformedText() {
        try {
            EditorialLineageCanonicalizer.strictUtf8("\ufeffcanonical");
            fail("BOM must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            EditorialLineageCanonicalizer.strictUtf8("\ud800");
            fail("malformed UTF-16 input must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test public void modelAndValidationResultsDefensivelyCopyCollections() {
        ArrayList<EditorialLineageInputEntry> mutableEntries = new ArrayList<>();
        mutableEntries.add(new EditorialLineageInputEntry("RAW", 0, ROOT_INPUT_HASH, 1, 1));
        EditorialLineageInputManifest manifest = new EditorialLineageInputManifest("input-manifest-v1", mutableEntries);
        mutableEntries.clear();
        assertEquals(1, manifest.entries().size());
        try {
            manifest.entries().add(new EditorialLineageInputEntry("DRAFT", 0, DRAFT_INPUT_HASH, 1, 1));
            fail("manifest entries must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        EditorialLineageValidationResult result = VALIDATOR.validate(
                child("run-child", root("run-root"), baseManifest()), TestContext.of());
        try {
            result.issues().clear();
            fail("validation issues must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test public void fixedFixtureManifestHasExpectedMachineCodes() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/editorial/lineage/root-child-v1/manifest.json")) {
            if (input == null) fail("fixture manifest missing");
            String fixture = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String canonicalFixture = fixture.trim();
            Object parsed = EditorialCanonicalJson.parseObject(canonicalFixture.getBytes(StandardCharsets.UTF_8));
            assertEquals(canonicalFixture, EditorialCanonicalJson.canonicalize(parsed));
            assertTrue(fixture.contains("\"fixtureSetId\":\"lineage/root-child-v1\""));
            assertTrue(fixture.contains("\"caseId\":\"ambiguous-parent\""));
            assertTrue(fixture.contains("\"expectedCode\":\"CYCLE_DETECTED\""));
        }
    }

    private static EditorialLineageRecord root(String runIdentity) {
        return EditorialLineageRecord.create(identity(runIdentity, EditorialLineageNodeKind.ROOT, baseManifest()), null);
    }

    private static EditorialLineageRecord child(String runIdentity,
                                                 EditorialLineageRecord parent,
                                                 EditorialLineageInputManifest manifest) {
        return childWith(parent, identity(runIdentity, EditorialLineageNodeKind.CHILD, manifest));
    }

    private static EditorialLineageRecord childWith(EditorialLineageRecord parent,
                                                    EditorialLineageIdentity identity) {
        return EditorialLineageRecord.create(identity,
                new EditorialLineageParentReference(parent.recordIdentity(), parent.recordFingerprint()));
    }

    private static EditorialLineageIdentity identity(String runIdentity,
                                                      EditorialLineageNodeKind kind,
                                                      EditorialLineageInputManifest manifest) {
        return identity(runIdentity, kind, manifest, PACK, "profile-safe4", "v1", PROFILE_HASH,
                MACHINE, "project-1", "chapter-001");
    }

    private static EditorialLineageIdentity identity(String runIdentity,
                                                      EditorialLineageNodeKind kind,
                                                      EditorialLineageInputManifest manifest,
                                                      String pack,
                                                      String profileId,
                                                      String profileVersion,
                                                      String profileHash,
                                                      String machine,
                                                      String project,
                                                      String inputScope) {
        return new EditorialLineageIdentity(pack, profileId, profileVersion, profileHash, machine,
                "editorial.contract.v1", "editorial.schema.v1", project, inputScope,
                runIdentity, manifest, kind);
    }

    private static EditorialLineageInputManifest baseManifest() {
        return new EditorialLineageInputManifest("input-manifest-v1", List.of(
                new EditorialLineageInputEntry("RAW", 0, ROOT_INPUT_HASH, 100, 1),
                new EditorialLineageInputEntry("DRAFT", 0, DRAFT_INPUT_HASH, 100, 1)));
    }

    private static EditorialLineageInputManifest changedManifest() {
        return new EditorialLineageInputManifest("input-manifest-v1", List.of(
                new EditorialLineageInputEntry("RAW", 0, mutate(ROOT_INPUT_HASH), 100, 1),
                new EditorialLineageInputEntry("DRAFT", 0, DRAFT_INPUT_HASH, 100, 1)));
    }

    private static String mutate(String value) {
        char replacement = value.charAt(0) == 'a' ? 'b' : 'a';
        return replacement + value.substring(1);
    }

    private static void assertValid(EditorialLineageRecord record, EditorialLineageValidationContext context) {
        EditorialLineageValidationResult result = VALIDATOR.validate(record, context);
        assertTrue(result.issues().toString(), result.isValid());
        assertEquals(EditorialLineageValidationCode.VALID, result.code());
    }

    private static void assertHasCode(EditorialLineageRecord record,
                                      EditorialLineageValidationContext context,
                                      EditorialLineageValidationCode expected) {
        assertHasCode(VALIDATOR.validate(record, context), expected);
    }

    private static void assertHasCode(EditorialLineageValidationResult result,
                                      EditorialLineageValidationCode expected) {
        assertTrue("expected " + expected + " in " + result.failureCodes(),
                result.failureCodes().contains(expected));
    }

    private static final class TestContext implements EditorialLineageValidationContext {
        private final Map<String, List<EditorialLineageRecord>> byIdentity;
        private final Map<String, List<EditorialLineageRecord>> byRun;
        private final Map<String, List<EditorialLineageRecord>> byParent;
        private final Optional<EditorialLineageTrustContext> trust;

        private TestContext(List<EditorialLineageRecord> records,
                            Optional<EditorialLineageTrustContext> trust) {
            this.byIdentity = new HashMap<>();
            this.byRun = new HashMap<>();
            this.byParent = new HashMap<>();
            this.trust = trust;
            for (EditorialLineageRecord record : records) {
                byIdentity.computeIfAbsent(record.recordIdentity(), unused -> new ArrayList<>()).add(record);
                byRun.computeIfAbsent(record.identity().runEvaluationIdentity(), unused -> new ArrayList<>()).add(record);
                if (record.parentReference() != null) {
                    byParent.computeIfAbsent(record.parentReference().parentRecordIdentity(),
                            unused -> new ArrayList<>()).add(record);
                }
            }
        }

        static TestContext of(EditorialLineageRecord... records) {
            return new TestContext(List.of(records), Optional.empty());
        }

        TestContext withTrust(EditorialLineageTrustContext trustContext) {
            return new TestContext(allRecords(), Optional.of(trustContext));
        }

        private List<EditorialLineageRecord> allRecords() {
            ArrayList<EditorialLineageRecord> records = new ArrayList<>();
            byIdentity.values().forEach(records::addAll);
            return records;
        }

        @Override public List<EditorialLineageRecord> findByRecordIdentity(String recordIdentity) {
            return byIdentity.getOrDefault(recordIdentity, List.of());
        }

        @Override public List<EditorialLineageRecord> findByRunEvaluationIdentity(String runEvaluationIdentity) {
            return byRun.getOrDefault(runEvaluationIdentity, List.of());
        }

        @Override public List<EditorialLineageRecord> findChildrenByParentIdentity(String parentRecordIdentity) {
            return byParent.getOrDefault(parentRecordIdentity, List.of());
        }

        @Override public Optional<EditorialLineageTrustContext> expectedTrustContext() {
            return trust;
        }
    }
}
