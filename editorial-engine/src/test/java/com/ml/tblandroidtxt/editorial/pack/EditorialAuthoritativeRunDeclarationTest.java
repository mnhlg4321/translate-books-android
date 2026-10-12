package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class EditorialAuthoritativeRunDeclarationTest {
    private static final String PROJECT = "a".repeat(64);
    private static final String SCOPE = "b".repeat(64);
    private static final String MANIFEST = "c".repeat(64);
    private static final String PARENT = "d".repeat(64);

    @Test public void sameSemanticDeclarationIsDeterministicAndTimestampIndependent() {
        EditorialAuthoritativeRunDeclarationDraft draft = draft("request-001", "phase-1", null);
        EditorialAuthoritativeRunDeclaration first =
                EditorialAuthoritativeRunDeclaration.allocate(draft, 4L, 10L);
        EditorialAuthoritativeRunDeclaration replay =
                EditorialAuthoritativeRunDeclaration.allocate(draft, 4L, 99L);

        assertEquals(first.declarationIdentity(), replay.declarationIdentity());
        assertEquals(first.declarationFingerprint(), replay.declarationFingerprint());
        assertEquals(first.identityProjection(), replay.identityProjection());
        assertEquals(first.fingerprintProjection(), replay.fingerprintProjection());
        assertNotEquals(first.createdAt(), replay.createdAt());
    }

    @Test public void semanticChangeChangesIdentityAndFingerprint() {
        EditorialAuthoritativeRunDeclaration first = EditorialAuthoritativeRunDeclaration.allocate(
                draft("request-001", "phase-1", null), 0L, 1L);
        EditorialAuthoritativeRunDeclaration changed = EditorialAuthoritativeRunDeclaration.allocate(
                draft("request-001", "phase-2", null), 0L, 1L);
        assertNotEquals(first.declarationIdentity(), changed.declarationIdentity());
        assertNotEquals(first.declarationFingerprint(), changed.declarationFingerprint());
    }

    @Test public void manifestReferenceChangesFingerprintButNotSemanticIdentity() {
        EditorialAuthoritativeRunDeclaration first = EditorialAuthoritativeRunDeclaration.allocate(
                draft("request-001", "phase-1", null), 0L, 1L);
        EditorialAuthoritativeRunDeclaration changedReference = EditorialAuthoritativeRunDeclaration.allocate(
                new EditorialAuthoritativeRunDeclarationDraft("request-001", PROJECT, SCOPE,
                        "evaluation-1", "translate", "phase-1", MANIFEST,
                        "attestation-v2", EditorialLineageNodeKind.ROOT, null), 0L, 1L);
        assertEquals(first.declarationIdentity(), changedReference.declarationIdentity());
        assertNotEquals(first.declarationFingerprint(), changedReference.declarationFingerprint());
    }

    @Test public void rootAndChildRequireExactIntentAndParent() {
        EditorialAuthoritativeRunDeclaration root = EditorialAuthoritativeRunDeclaration.allocate(
                draft("request-root", "phase-1", null), 0L, 1L);
        EditorialAuthoritativeRunDeclaration child = EditorialAuthoritativeRunDeclaration.allocate(
                draft("request-child", "phase-1", PARENT), 1L, 1L);
        assertEquals(EditorialLineageNodeKind.ROOT, root.nodeKind());
        assertNull(root.parentRecordIdentity());
        assertEquals(EditorialLineageNodeKind.CHILD, child.nodeKind());
        assertEquals(PARENT, child.parentRecordIdentity());
        assertNotEquals(root.declarationIdentity(), child.declarationIdentity());
        assertThrows(() -> new EditorialAuthoritativeRunDeclarationDraft(
                "bad-root", PROJECT, SCOPE, "evaluation-1", "translate", "phase-1", MANIFEST,
                "attestation-v1", EditorialLineageNodeKind.ROOT, PARENT));
        assertThrows(() -> new EditorialAuthoritativeRunDeclarationDraft(
                "bad-child", PROJECT, SCOPE, "evaluation-1", "translate", "phase-1", MANIFEST,
                "attestation-v1", EditorialLineageNodeKind.CHILD, null));
    }

    @Test public void selectorStructuralSafetyIsFailClosed() {
        assertThrows(() -> draft("", "phase-1", null));
        assertThrows(() -> draft("1234567890", "phase-1", null));
        assertThrows(() -> draft("550e8400-e29b-41d4-a716-446655440000", "phase-1", null));
        assertThrows(() -> draft("file://selector", "phase-1", null));
        assertThrows(() -> draft("request\u0001", "phase-1", null));
        assertThrows(() -> draft(" request", "phase-1", null));
    }

    @Test public void storedHashesAreRecomputedNotTrusted() {
        EditorialAuthoritativeRunDeclaration value = EditorialAuthoritativeRunDeclaration.allocate(
                draft("request-001", "phase-1", null), 0L, 1L);
        assertEquals(value.declarationIdentity(), EditorialAuthoritativeRunDeclaration.fromStored(
                value.declarationIdentity(), value.declarationFingerprint(), value.draft(),
                value.runAttemptOrdinal(), value.createdAt()).declarationIdentity());
        assertThrows(() -> EditorialAuthoritativeRunDeclaration.fromStored(
                "0".repeat(64), value.declarationFingerprint(), value.draft(),
                value.runAttemptOrdinal(), value.createdAt()));
    }

    private static EditorialAuthoritativeRunDeclarationDraft draft(
            String request, String phase, String parent) {
        return new EditorialAuthoritativeRunDeclarationDraft(request, PROJECT, SCOPE,
                "evaluation-1", "translate", phase, MANIFEST, "attestation-v1",
                parent == null ? EditorialLineageNodeKind.ROOT : EditorialLineageNodeKind.CHILD,
                parent);
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
