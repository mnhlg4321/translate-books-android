package com.ml.tblandroidtxt.editorial.pack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public final class EditorialLineageRuntimeServiceTest {
    private static final String PACK = "a".repeat(64);
    private static final String OTHER_PACK = "f".repeat(64);
    private static final String PROFILE_HASH = "b".repeat(64);
    private static final String MACHINE = "c".repeat(64);
    private static final String OTHER_MACHINE = "d".repeat(64);
    private static final String RAW_HASH = "e".repeat(64);
    private static final String DRAFT_HASH = "1".repeat(64);

    @Test public void validRootIsReadyToAppendAndDoesNotPersist() {
        EditorialLineageCreationResult result = service(context(
                PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE),
                baseManifest())).validateAndPrepare(new EditorialLineageCreationRequest(
                selection(EditorialLineageNodeKind.ROOT, null)));

        assertEquals(EditorialLineageCreationCode.READY_TO_APPEND, result.code());
        assertTrue(result.isReadyToAppend());
        assertTrue(result.record().isPresent());
        assertEquals(EditorialLineageNodeKind.ROOT, result.record().get().nodeKind());
        assertTrue(result.record().get().parentReference() == null);
    }

    @Test public void validChildUsesOnlyResolverParentFingerprint() {
        EditorialLineageRecord parent = root(PACK, "project-1", "scope-1", "run-parent", baseManifest());
        EditorialLineageAuthoritativeContext context = context(
                PACK, "project-1", "scope-1", "run-child", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(parent), List.of(parent), trust(MACHINE),
                baseManifest());

        EditorialLineageCreationResult result = service(context).validateAndPrepare(
                new EditorialLineageCreationRequest(selection(EditorialLineageNodeKind.CHILD,
                        parent.recordIdentity())));

        assertEquals(EditorialLineageCreationCode.READY_TO_APPEND, result.code());
        EditorialLineageLineageAssertions.assertParent(result.record().get(), parent);
    }

    @Test public void sameRequestAndContextProduceTheSameRecord() {
        EditorialLineageAuthoritativeContext context = context(
                PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE),
                baseManifest());
        EditorialLineageCreationRequest request = new EditorialLineageCreationRequest(
                selection(EditorialLineageNodeKind.ROOT, null));

        EditorialLineageRecord first = service(context).validateAndPrepare(request).record().get();
        EditorialLineageRecord second = service(context).validateAndPrepare(request).record().get();

        assertEquals(first, second);
        assertEquals(first.recordIdentity(), second.recordIdentity());
        assertEquals(first.recordFingerprint(), second.recordFingerprint());
    }

    @Test public void callerRequestHasSelectionsAndAssertionsButNoAuthoritativeParentFingerprint() {
        Set<String> selectionFields = fieldNames(EditorialLineageCallerSelection.class);
        Set<String> requestFields = fieldNames(EditorialLineageCreationRequest.class);
        assertFalse(selectionFields.contains("canonicalPackHash"));
        assertFalse(selectionFields.contains("machineContractFingerprint"));
        assertFalse(selectionFields.contains("parentRecordFingerprint"));
        assertFalse(requestFields.contains("canonicalPackHash"));
        assertFalse(requestFields.contains("machineContractFingerprint"));
        assertFalse(requestFields.contains("parentRecordFingerprint"));
        assertTrue(fieldNames(EditorialLineageCallerAssertions.class)
                .stream().anyMatch(name -> name.endsWith("Assertion")));
    }

    @Test public void missingProjectFailsClosed() {
        assertCode(context(PACK, null, "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest()),
                selection(EditorialLineageNodeKind.ROOT, null), EditorialLineageCreationCode.PROJECT_IDENTITY_REQUIRED);
    }

    @Test public void missingScopeFailsClosed() {
        assertCode(context(PACK, "project-1", null, "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest()),
                selection(EditorialLineageNodeKind.ROOT, null), EditorialLineageCreationCode.INPUT_SCOPE_REQUIRED);
    }

    @Test public void missingOrUnclearRunIdentityFailsClosed() {
        assertCode(context(PACK, "project-1", "scope-1", null, EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest()),
                selection(EditorialLineageNodeKind.ROOT, null), EditorialLineageCreationCode.RUN_EVALUATION_IDENTITY_REQUIRED);
        assertCode(context(PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.OPEN,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest()),
                selection(EditorialLineageNodeKind.ROOT, null), EditorialLineageCreationCode.RUN_EVALUATION_IDENTITY_REQUIRED);
    }

    @Test public void missingCallerSelectionIsRejectedBeforeAuthoritativePreparation() {
        EditorialLineageCallerSelection selection = new EditorialLineageCallerSelection(
                "", "scope-selector", "run-selector", EditorialLineageNodeKind.ROOT, null);
        assertCode(context(PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest()),
                selection, EditorialLineageCreationCode.PROJECT_IDENTITY_REQUIRED);
        selection = new EditorialLineageCallerSelection(
                "project-selector", "", "run-selector", EditorialLineageNodeKind.ROOT, null);
        assertCode(context(PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest()),
                selection, EditorialLineageCreationCode.INPUT_SCOPE_REQUIRED);
    }

    @Test public void incompleteManifestFailsClosed() {
        EditorialLineageInputManifest manifest = new EditorialLineageInputManifest("input-manifest-v1",
                List.of(new EditorialLineageInputEntry("RAW", 0, RAW_HASH, 10, 1)));
        assertCode(context(PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), manifest),
                selection(EditorialLineageNodeKind.ROOT, null), EditorialLineageCreationCode.INPUT_MANIFEST_INCOMPLETE);
    }

    @Test public void blockedCompatibilityCannotProduceReadyRecord() {
        assertCode(context(PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.BLOCKED, List.of(), List.of(), trust(MACHINE), baseManifest()),
                selection(EditorialLineageNodeKind.ROOT, null), EditorialLineageCreationCode.COMPATIBILITY_CONTEXT_MISMATCH);
    }

    @Test public void profileOrMachineTrustMismatchFailsClosed() {
        EditorialLineageTrustContext authoritative = trust(MACHINE);
        EditorialLineageTrustContext expected = trust(OTHER_MACHINE);
        EditorialLineageAuthoritativeContext context = context(
                PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), authoritative,
                baseManifest(), expected);
        assertCode(context, selection(EditorialLineageNodeKind.ROOT, null),
                EditorialLineageCreationCode.TRUSTED_PROFILE_CONTEXT_MISMATCH);
    }

    @Test public void callerAssertionMismatchIsNotUsedAsAuthoritativeFact() {
        EditorialLineageCallerAssertions assertions = new EditorialLineageCallerAssertions(
                "9".repeat(64), null, null, null, null, null, null, null, null, null);
        EditorialLineageCreationRequest request = new EditorialLineageCreationRequest(
                selection(EditorialLineageNodeKind.ROOT, null), assertions);
        EditorialLineageAuthoritativeContext context = context(
                PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest());

        EditorialLineageCreationResult result = service(context).validateAndPrepare(request);
        assertEquals(EditorialLineageCreationCode.CALLER_ASSERTION_MISMATCH, result.code());
        assertFalse(result.record().isPresent());
    }

    @Test public void childWithoutParentSelectionIsValidationFailure() {
        EditorialLineageCreationResult result = service(context(
                PACK, "project-1", "scope-1", "run-child", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest()))
                .validateAndPrepare(new EditorialLineageCreationRequest(
                        selection(EditorialLineageNodeKind.CHILD, null)));
        assertValidationCode(result, EditorialLineageValidationCode.MISSING_PARENT);
    }

    @Test public void orphanAndAmbiguousParentsFailClosed() {
        EditorialLineageRecord parent = root(PACK, "project-1", "scope-1", "run-parent", baseManifest());
        EditorialLineageCreationResult orphan = service(context(
                PACK, "project-1", "scope-1", "run-child", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest()))
                .validateAndPrepare(new EditorialLineageCreationRequest(
                        selection(EditorialLineageNodeKind.CHILD, parent.recordIdentity())));
        assertValidationCode(orphan, EditorialLineageValidationCode.ORPHAN_LINEAGE);

        EditorialLineageCreationResult ambiguous = service(context(
                PACK, "project-1", "scope-1", "run-child", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(parent), List.of(parent, parent), trust(MACHINE), baseManifest()))
                .validateAndPrepare(new EditorialLineageCreationRequest(
                        selection(EditorialLineageNodeKind.CHILD, parent.recordIdentity())));
        assertValidationCode(ambiguous, EditorialLineageValidationCode.AMBIGUOUS_PARENT);
    }

    @Test public void reparentAndDuplicateAreReturnedAsExistingValidatorCodes() {
        EditorialLineageRecord existing = root(PACK, "project-1", "scope-1", "run-root", baseManifest());
        EditorialLineageCreationResult duplicate = service(context(
                PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(existing), List.of(), trust(MACHINE), baseManifest()))
                .validateAndPrepare(new EditorialLineageCreationRequest(
                        selection(EditorialLineageNodeKind.ROOT, null)));
        assertValidationCode(duplicate, EditorialLineageValidationCode.DUPLICATE_LINEAGE);

        EditorialLineageRecord otherParent = root(PACK, "project-1", "scope-1", "run-other-parent", baseManifest());
        EditorialLineageIdentity childIdentity = identity(PACK, "project-1", "scope-1", "run-child",
                EditorialLineageNodeKind.CHILD, baseManifest());
        EditorialLineageRecord existingChild = EditorialLineageRecord.create(childIdentity,
                new EditorialLineageParentReference(existing.recordIdentity(), existing.recordFingerprint()));
        EditorialLineageAuthoritativeContext reparentContext = context(
                PACK, "project-1", "scope-1", "run-child", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE,
                List.of(existingChild, otherParent), List.of(otherParent), trust(MACHINE), baseManifest());
        EditorialLineageCreationResult reparent = service(reparentContext).validateAndPrepare(
                new EditorialLineageCreationRequest(selection(EditorialLineageNodeKind.CHILD,
                        otherParent.recordIdentity())));
        assertValidationCode(reparent, EditorialLineageValidationCode.REPARENT_ATTEMPT);
    }

    @Test public void crossPackParentFailsClosed() {
        EditorialLineageRecord parent = root(OTHER_PACK, "project-1", "scope-1", "run-parent", baseManifest());
        EditorialLineageCreationResult result = service(context(
                PACK, "project-1", "scope-1", "run-child", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(parent), List.of(parent), trust(MACHINE), baseManifest()))
                .validateAndPrepare(new EditorialLineageCreationRequest(
                        selection(EditorialLineageNodeKind.CHILD, parent.recordIdentity())));
        assertValidationCode(result, EditorialLineageValidationCode.CROSS_PACK);
    }

    @Test public void crossProfileProjectAndScopeParentCodesRemainLossless() {
        EditorialLineageRecord crossProfile = rootWithIdentity(new EditorialLineageIdentity(
                PACK, "other-profile", "1", PROFILE_HASH, MACHINE, "contract-v1", "schema-v16",
                "project-1", "scope-1", "run-parent", baseManifest(), EditorialLineageNodeKind.ROOT));
        EditorialLineageCreationResult profileResult = service(context(
                PACK, "project-1", "scope-1", "run-child", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(crossProfile), List.of(crossProfile), trust(MACHINE), baseManifest()))
                .validateAndPrepare(new EditorialLineageCreationRequest(
                        selection(EditorialLineageNodeKind.CHILD, crossProfile.recordIdentity())));
        assertValidationCode(profileResult, EditorialLineageValidationCode.CROSS_PROFILE);

        EditorialLineageRecord crossProject = root(PACK, "project-2", "scope-1", "run-parent", baseManifest());
        EditorialLineageCreationResult projectResult = service(context(
                PACK, "project-1", "scope-1", "run-child", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(crossProject), List.of(crossProject), trust(MACHINE), baseManifest()))
                .validateAndPrepare(new EditorialLineageCreationRequest(
                        selection(EditorialLineageNodeKind.CHILD, crossProject.recordIdentity())));
        assertValidationCode(projectResult, EditorialLineageValidationCode.CROSS_PROJECT);

        EditorialLineageRecord crossScope = root(PACK, "project-1", "scope-2", "run-parent", baseManifest());
        EditorialLineageCreationResult scopeResult = service(context(
                PACK, "project-1", "scope-1", "run-child", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(crossScope), List.of(crossScope), trust(MACHINE), baseManifest()))
                .validateAndPrepare(new EditorialLineageCreationRequest(
                        selection(EditorialLineageNodeKind.CHILD, crossScope.recordIdentity())));
        assertValidationCode(scopeResult, EditorialLineageValidationCode.CROSS_INPUT_SCOPE);
    }

    @Test public void resolverNullAndResolverThrowAreStableFailures() {
        EditorialLineageCreationRequest request = new EditorialLineageCreationRequest(
                selection(EditorialLineageNodeKind.ROOT, null));
        assertEquals(EditorialLineageCreationCode.LINEAGE_CONTEXT_REQUIRED,
                new EditorialLineageRuntimeService(selection -> null)
                        .validateAndPrepare(request).code());
        assertEquals(EditorialLineageCreationCode.RESOLVER_FAILURE,
                new EditorialLineageRuntimeService(selection -> { throw new IllegalStateException("ignored"); })
                        .validateAndPrepare(request).code());
    }

    @Test public void rootParentAndUnknownNodeKindFailWithoutRecord() {
        EditorialLineageRecord parent = root(PACK, "project-1", "scope-1", "run-parent", baseManifest());
        EditorialLineageCreationResult rootParent = service(context(
                PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(parent), List.of(), trust(MACHINE), baseManifest()))
                .validateAndPrepare(new EditorialLineageCreationRequest(
                        selection(EditorialLineageNodeKind.ROOT, parent.recordIdentity())));
        assertValidationCode(rootParent, EditorialLineageValidationCode.ROOT_HAS_PARENT);
        assertFalse(rootParent.record().isPresent());

        EditorialLineageCallerSelection missingKind = new EditorialLineageCallerSelection(
                "project-selector", "scope-selector", "run-selector", null, null);
        EditorialLineageCreationResult missingKindResult = service(context(
                PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest()))
                .validateAndPrepare(new EditorialLineageCreationRequest(missingKind));
        assertEquals(EditorialLineageCreationCode.LINEAGE_CONTEXT_REQUIRED, missingKindResult.code());
        assertFalse(missingKindResult.record().isPresent());
    }

    @Test public void resultAndAuthoritativeCollectionsAreDefensive() {
        ArrayList<EditorialLineageRecord> parents = new ArrayList<>();
        EditorialLineageAuthoritativeContext context = context(
                PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), parents, trust(MACHINE), baseManifest());
        parents.add(root(PACK, "project-1", "scope-1", "late-parent", baseManifest()));
        assertTrue(context.parentCandidates().isEmpty());
        try {
            context.requiredInputRoles().add("MUTATE");
            fail("required roles must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        EditorialLineageCreationResult result = service(context).validateAndPrepare(
                new EditorialLineageCreationRequest(selection(EditorialLineageNodeKind.ROOT, null)));
        try {
            result.validationCodes().add(EditorialLineageValidationCode.INVALID_HASH);
            fail("result codes must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        assertNotNull(result.record().get().identity());
    }

    @Test public void localeDoesNotChangePreparedRecord() {
        EditorialLineageAuthoritativeContext context = context(
                PACK, "project-1", "scope-1", "run-root", EditorialLineageRunContextState.CLOSED,
                EditorialLineageCompatibilityStatus.COMPATIBLE, List.of(), List.of(), trust(MACHINE), baseManifest());
        EditorialLineageCreationRequest request = new EditorialLineageCreationRequest(
                selection(EditorialLineageNodeKind.ROOT, null));
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            EditorialLineageRecord first = service(context).validateAndPrepare(request).record().get();
            Locale.setDefault(Locale.US);
            EditorialLineageRecord second = service(context).validateAndPrepare(request).record().get();
            assertEquals(first, second);
        } finally {
            Locale.setDefault(old);
        }
    }

    private static EditorialLineageRuntimeService service(EditorialLineageAuthoritativeContext context) {
        return new EditorialLineageRuntimeService(selection -> context);
    }

    private static void assertCode(EditorialLineageAuthoritativeContext context,
                                   EditorialLineageCallerSelection selection,
                                   EditorialLineageCreationCode expected) {
        EditorialLineageCreationResult result = service(context).validateAndPrepare(
                new EditorialLineageCreationRequest(selection));
        assertEquals(expected, result.code());
        assertFalse(result.record().isPresent());
    }

    private static void assertValidationCode(EditorialLineageCreationResult result,
                                             EditorialLineageValidationCode expected) {
        assertEquals(EditorialLineageCreationCode.VALIDATION_FAILED, result.code());
        assertTrue(result.validationCodes().contains(expected));
        assertFalse(result.record().isPresent());
    }

    private static EditorialLineageCallerSelection selection(EditorialLineageNodeKind nodeKind,
                                                              String parentIdentity) {
        return new EditorialLineageCallerSelection(
                "project-selector", "scope-selector", "run-selector", nodeKind, parentIdentity);
    }

    private static EditorialLineageAuthoritativeContext context(
            String pack,
            String project,
            String scope,
            String run,
            EditorialLineageRunContextState runState,
            EditorialLineageCompatibilityStatus compatibilityStatus,
            List<EditorialLineageRecord> validationRecords,
            List<EditorialLineageRecord> parentCandidates,
            EditorialLineageTrustContext trust,
            EditorialLineageInputManifest manifest) {
        return context(pack, project, scope, run, runState, compatibilityStatus,
                validationRecords, parentCandidates, trust, manifest, trust);
    }

    private static EditorialLineageAuthoritativeContext context(
            String pack,
            String project,
            String scope,
            String run,
            EditorialLineageRunContextState runState,
            EditorialLineageCompatibilityStatus compatibilityStatus,
            List<EditorialLineageRecord> validationRecords,
            List<EditorialLineageRecord> parentCandidates,
            EditorialLineageTrustContext trust,
            EditorialLineageInputManifest manifest,
            EditorialLineageTrustContext expectedTrust) {
        return new EditorialLineageAuthoritativeContext(
                pack, trust, "contract-v1", "schema-v16",
                new EditorialLineageCompatibilityEvidence("evaluation-1", compatibilityStatus),
                project, scope, run, runState, manifest, Set.of("RAW", "DRAFT"),
                new InMemoryValidationContext(validationRecords, expectedTrust), parentCandidates);
    }

    private static EditorialLineageRecord root(String pack, String project, String scope,
                                               String run, EditorialLineageInputManifest manifest) {
        return EditorialLineageRecord.create(
                identity(pack, project, scope, run, EditorialLineageNodeKind.ROOT, manifest), null);
    }

    private static EditorialLineageRecord rootWithIdentity(EditorialLineageIdentity identity) {
        return EditorialLineageRecord.create(identity, null);
    }

    private static EditorialLineageIdentity identity(String pack, String project, String scope,
                                                      String run, EditorialLineageNodeKind kind,
                                                      EditorialLineageInputManifest manifest) {
        return new EditorialLineageIdentity(pack, "profile", "1", PROFILE_HASH, MACHINE,
                "contract-v1", "schema-v16", project, scope, run, manifest, kind);
    }

    private static EditorialLineageInputManifest baseManifest() {
        return new EditorialLineageInputManifest("input-manifest-v1", List.of(
                new EditorialLineageInputEntry("DRAFT", 0, DRAFT_HASH, 20, 2),
                new EditorialLineageInputEntry("RAW", 0, RAW_HASH, 30, 3)));
    }

    private static EditorialLineageTrustContext trust(String machine) {
        return new EditorialLineageTrustContext("profile", "1", PROFILE_HASH, machine);
    }

    private static Set<String> fieldNames(Class<?> type) {
        Set<String> names = new HashSet<>();
        for (Field field : type.getDeclaredFields()) names.add(field.getName());
        return names;
    }

    private static final class InMemoryValidationContext implements EditorialLineageValidationContext {
        private final List<EditorialLineageRecord> records;
        private final EditorialLineageTrustContext expectedTrust;

        private InMemoryValidationContext(List<EditorialLineageRecord> records,
                                          EditorialLineageTrustContext expectedTrust) {
            this.records = List.copyOf(records == null ? List.of() : records);
            this.expectedTrust = expectedTrust;
        }

        @Override public List<EditorialLineageRecord> findByRecordIdentity(String identity) {
            ArrayList<EditorialLineageRecord> result = new ArrayList<>();
            for (EditorialLineageRecord record : records) {
                if (record.recordIdentity().equals(identity)) result.add(record);
            }
            return List.copyOf(result);
        }

        @Override public List<EditorialLineageRecord> findByRunEvaluationIdentity(String run) {
            ArrayList<EditorialLineageRecord> result = new ArrayList<>();
            for (EditorialLineageRecord record : records) {
                if (record.identity().runEvaluationIdentity().equals(run)) result.add(record);
            }
            return List.copyOf(result);
        }

        @Override public List<EditorialLineageRecord> findChildrenByParentIdentity(String parent) {
            ArrayList<EditorialLineageRecord> result = new ArrayList<>();
            for (EditorialLineageRecord record : records) {
                if (record.parentReference() != null
                        && record.parentReference().parentRecordIdentity().equals(parent)) result.add(record);
            }
            return List.copyOf(result);
        }

        @Override public Optional<EditorialLineageTrustContext> expectedTrustContext() {
            return Optional.ofNullable(expectedTrust);
        }
    }

    private static final class EditorialLineageLineageAssertions {
        private static void assertParent(EditorialLineageRecord child, EditorialLineageRecord parent) {
            assertNotNull(child);
            assertNotNull(child.parentReference());
            assertEquals(parent.recordIdentity(), child.parentReference().parentRecordIdentity());
            assertEquals(parent.recordFingerprint(), child.parentReference().parentRecordFingerprint());
        }
    }
}
