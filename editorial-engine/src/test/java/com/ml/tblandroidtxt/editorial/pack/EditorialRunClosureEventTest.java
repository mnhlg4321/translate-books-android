package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class EditorialRunClosureEventTest {
    private static final String PROJECT = "a".repeat(64);
    private static final String SCOPE = "b".repeat(64);
    private static final String MANIFEST = "c".repeat(64);
    private static final String PARENT = "d".repeat(64);
    private static final String ATTESTATION = "e".repeat(64);
    @Test public void eventIdentityAndFingerprintAreDeterministic() {
        EditorialAuthoritativeRunDeclaration declaration = declaration(
                "closure-request-root", EditorialLineageNodeKind.ROOT, null, 1L, "manifest-v1");
        EditorialRunClosureEventDraft request = draft(declaration,
                EditorialLineageNodeKind.ROOT, null);
        EditorialRunClosureEvent first = EditorialRunClosureEvent.allocate(
                declaration, request, "attestation-v1", ATTESTATION, 10L);
        EditorialRunClosureEvent replay = EditorialRunClosureEvent.allocate(
                declaration, request, "attestation-v1", ATTESTATION, 99L);
        assertEquals(first.closureEventIdentity(), replay.closureEventIdentity());
        assertEquals(first.closureEventFingerprint(), replay.closureEventFingerprint());
        assertNotEquals(first.appendedAt(), replay.appendedAt());
    }

    @Test public void eventIdentityIncludesRootChildParentOrdinalAndEligibility() {
        EditorialAuthoritativeRunDeclaration rootDeclaration = declaration(
                "closure-root", EditorialLineageNodeKind.ROOT, null, 1L, "manifest-v1");
        EditorialAuthoritativeRunDeclaration childDeclaration = declaration(
                "closure-child", EditorialLineageNodeKind.CHILD, PARENT, 1L, "manifest-v1");
        EditorialAuthoritativeRunDeclaration nextDeclaration = declaration(
                "closure-next", EditorialLineageNodeKind.ROOT, null, 2L, "manifest-v1");
        EditorialRunClosureEvent root = event(rootDeclaration, EditorialLineageNodeKind.ROOT, null);
        EditorialRunClosureEvent child = event(childDeclaration, EditorialLineageNodeKind.CHILD, PARENT);
        EditorialRunClosureEvent nextAttempt = event(nextDeclaration, EditorialLineageNodeKind.ROOT, null);
        assertNotEquals(root.closureEventIdentity(), child.closureEventIdentity());
        assertNotEquals(root.closureEventIdentity(), nextAttempt.closureEventIdentity());
        assertNull(root.parentRecordIdentity());
        assertEquals(PARENT, child.parentRecordIdentity());
    }

    @Test public void manifestReferenceAndAttestationFingerprintOnlyChangeFingerprint() {
        EditorialAuthoritativeRunDeclaration firstDeclaration = declaration(
                "closure-reference", EditorialLineageNodeKind.ROOT, null, 1L, "manifest-v1");
        EditorialAuthoritativeRunDeclaration changedDeclaration = declaration(
                "closure-reference", EditorialLineageNodeKind.ROOT, null, 1L, "manifest-v2");
        EditorialRunClosureEvent first = event(firstDeclaration, EditorialLineageNodeKind.ROOT, null,
                "attestation-v1", ATTESTATION);
        EditorialRunClosureEvent changed = event(changedDeclaration, EditorialLineageNodeKind.ROOT, null,
                "attestation-v1", "1".repeat(64));
        assertEquals(first.closureEventIdentity(), changed.closureEventIdentity());
        assertNotEquals(first.closureEventFingerprint(), changed.closureEventFingerprint());
    }

    @Test public void timestampDoesNotParticipateInHashAndStoredHashesAreVerified() {
        EditorialAuthoritativeRunDeclaration declaration = declaration(
                "closure-stored", EditorialLineageNodeKind.ROOT, null, 1L, "manifest-v1");
        EditorialRunClosureEventDraft request = draft(declaration,
                EditorialLineageNodeKind.ROOT, null);
        EditorialRunClosureEvent value = EditorialRunClosureEvent.allocate(
                declaration, request, "attestation-v1", ATTESTATION, 44L);
        assertEquals(value.closureEventIdentity(), EditorialRunClosureEvent.fromStored(
                value.closureEventIdentity(), value.closureEventFingerprint(), declaration,
                request, value.closureAttestationVersion(), value.closureAttestationFingerprint(),
                88L).closureEventIdentity());
        assertThrows(() -> EditorialRunClosureEvent.fromStored(
                "0".repeat(64), value.closureEventFingerprint(), declaration, request,
                value.closureAttestationVersion(), value.closureAttestationFingerprint(),
                value.appendedAt()));
    }

    @Test public void rootAndChildStructuralRulesAreFailClosed() {
        assertThrows(() -> new EditorialRunClosureEventDraft(
                "closure-request", EditorialLineageNodeKind.ROOT, PARENT,
                EditorialClosureEventEligibility.ELIGIBLE));
        assertThrows(() -> new EditorialRunClosureEventDraft(
                "closure-request", EditorialLineageNodeKind.CHILD, null,
                EditorialClosureEventEligibility.ELIGIBLE));
        assertThrows(() -> new EditorialRunClosureEventDraft(
                "123", EditorialLineageNodeKind.ROOT, null,
                EditorialClosureEventEligibility.ELIGIBLE));
        assertThrows(() -> new EditorialRunClosureEventDraft(
                "closure-request", EditorialLineageNodeKind.ROOT, null, null));
    }

    private static EditorialRunClosureEvent event(
            EditorialAuthoritativeRunDeclaration declaration,
            EditorialLineageNodeKind nodeKind, String parent) {
        return event(declaration, nodeKind, parent, "attestation-v1", ATTESTATION);
    }

    private static EditorialRunClosureEvent event(
            EditorialAuthoritativeRunDeclaration declaration,
            EditorialLineageNodeKind nodeKind, String parent,
            String attestationVersion, String attestationFingerprint) {
        return EditorialRunClosureEvent.allocate(declaration,
                draft(declaration, nodeKind, parent), attestationVersion,
                attestationFingerprint, 1L);
    }

    private static EditorialRunClosureEventDraft draft(
            EditorialAuthoritativeRunDeclaration declaration,
            EditorialLineageNodeKind nodeKind, String parent) {
        return new EditorialRunClosureEventDraft(declaration.attemptRequestSelector(), nodeKind,
                parent, EditorialClosureEventEligibility.ELIGIBLE);
    }

    private static EditorialAuthoritativeRunDeclaration declaration(
            String selector, EditorialLineageNodeKind nodeKind, String parent,
            long ordinal, String manifestReference) {
        EditorialAuthoritativeRunDeclarationDraft draft =
                new EditorialAuthoritativeRunDeclarationDraft(selector, PROJECT, SCOPE,
                        "evaluation-1", "translate", "phase-1", MANIFEST, manifestReference,
                        nodeKind, parent);
        return EditorialAuthoritativeRunDeclaration.allocate(draft, ordinal, 7L);
    }

    private static void assertThrows(Runnable action) {
        try {
            action.run();
            fail("expected fail-closed validation");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage() != null && !expected.getMessage().isEmpty());
        }
    }
}
