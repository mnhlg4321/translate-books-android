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
    private static final String RUN = "f".repeat(64);

    @Test public void eventIdentityAndFingerprintAreDeterministic() {
        EditorialRunClosureEvent first = EditorialRunClosureEvent.allocate(draft(
                EditorialLineageNodeKind.ROOT, null, "attestation-v1", ATTESTATION), 10L);
        EditorialRunClosureEvent replay = EditorialRunClosureEvent.allocate(draft(
                EditorialLineageNodeKind.ROOT, null, "attestation-v1", ATTESTATION), 99L);
        assertEquals(first.closureEventIdentity(), replay.closureEventIdentity());
        assertEquals(first.closureEventFingerprint(), replay.closureEventFingerprint());
        assertNotEquals(first.appendedAt(), replay.appendedAt());
    }

    @Test public void eventIdentityIncludesRootChildParentOrdinalAndEligibility() {
        EditorialRunClosureEvent root = EditorialRunClosureEvent.allocate(draft(
                EditorialLineageNodeKind.ROOT, null, "attestation-v1", ATTESTATION), 1L);
        EditorialRunClosureEvent child = EditorialRunClosureEvent.allocate(draft(
                EditorialLineageNodeKind.CHILD, PARENT, "attestation-v1", ATTESTATION), 1L);
        EditorialRunClosureEvent nextAttempt = EditorialRunClosureEvent.allocate(
                draft(EditorialLineageNodeKind.ROOT, null, "attestation-v1", ATTESTATION, 2L), 1L);
        assertNotEquals(root.closureEventIdentity(), child.closureEventIdentity());
        assertNotEquals(root.closureEventIdentity(), nextAttempt.closureEventIdentity());
        assertNull(root.parentRecordIdentity());
        assertEquals(PARENT, child.parentRecordIdentity());
    }

    @Test public void manifestReferenceAndAttestationFingerprintOnlyChangeFingerprint() {
        EditorialRunClosureEvent first = EditorialRunClosureEvent.allocate(draft(
                EditorialLineageNodeKind.ROOT, null, "attestation-v1", ATTESTATION), 1L);
        EditorialRunClosureEvent changed = EditorialRunClosureEvent.allocate(draft(
                EditorialLineageNodeKind.ROOT, null, "attestation-v2", "1".repeat(64)), 1L);
        assertEquals(first.closureEventIdentity(), changed.closureEventIdentity());
        assertNotEquals(first.closureEventFingerprint(), changed.closureEventFingerprint());
    }

    @Test public void timestampDoesNotParticipateInHashAndStoredHashesAreVerified() {
        EditorialRunClosureEvent value = EditorialRunClosureEvent.allocate(draft(
                EditorialLineageNodeKind.ROOT, null, "attestation-v1", ATTESTATION), 44L);
        assertEquals(value.closureEventIdentity(), EditorialRunClosureEvent.fromStored(
                value.closureEventIdentity(), value.closureEventFingerprint(), value.draft(),
                88L).closureEventIdentity());
        assertThrows(() -> EditorialRunClosureEvent.fromStored(
                "0".repeat(64), value.closureEventFingerprint(), value.draft(), value.appendedAt()));
    }

    @Test public void rootAndChildStructuralRulesAreFailClosed() {
        assertThrows(() -> draft(EditorialLineageNodeKind.ROOT, PARENT, "attestation-v1", ATTESTATION));
        assertThrows(() -> draft(EditorialLineageNodeKind.CHILD, null, "attestation-v1", ATTESTATION));
        assertThrows(() -> draft(EditorialLineageNodeKind.ROOT, null, "file://bad", ATTESTATION));
        assertThrows(() -> draft(EditorialLineageNodeKind.ROOT, null, "attestation-v1", "bad"));
    }

    private static EditorialRunClosureEventDraft draft(
            EditorialLineageNodeKind nodeKind, String parent, String reference, String fingerprint) {
        return draft(nodeKind, parent, reference, fingerprint, 1L);
    }

    private static EditorialRunClosureEventDraft draft(
            EditorialLineageNodeKind nodeKind, String parent, String reference,
            String fingerprint, long ordinal) {
        return new EditorialRunClosureEventDraft(RUN, PROJECT, SCOPE, "evaluation-1",
                "translate", "phase-1", MANIFEST, reference, nodeKind, parent, ordinal,
                EditorialClosureEventEligibility.ELIGIBLE, "closure-attestation-v1", fingerprint);
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
