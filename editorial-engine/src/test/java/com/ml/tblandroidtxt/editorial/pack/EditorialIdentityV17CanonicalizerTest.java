package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EditorialIdentityV17CanonicalizerTest {
    private static final String HASH_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String HASH_C = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    @Test public void projectProjectionIsStableAndDoesNotUseLocalFields() {
        EditorialProjectRevision first = project(" series/001 ", "novel");
        EditorialProjectRevision second = project(" series/001 ", "novel");

        assertEquals(first.canonicalProjection(), second.canonicalProjection());
        assertEquals(first.revisionIdentity(), second.revisionIdentity());
        assertEquals(first.projectDefinitionFingerprint(), second.projectDefinitionFingerprint());
        assertFalse(first.canonicalProjection().contains("outputUri"));
        assertFalse(first.canonicalProjection().contains("createdAt"));
        assertFalse(first.canonicalProjection().contains("rowId"));
        assertFalse(first.canonicalProjection().contains("series_name"));
    }

    @Test public void oneSemanticByteChangesProjectIdentityAndFingerprint() {
        EditorialProjectRevision left = project("series/001", "novel");
        EditorialProjectRevision right = project("series/002", "novel");

        assertNotEquals(left.revisionIdentity(), right.revisionIdentity());
        assertNotEquals(left.projectDefinitionFingerprint(), right.projectDefinitionFingerprint());
    }

    @Test public void scopeKeyUsesNfcAsciiTrimAndPreservesMeaning() {
        assertEquals("Café/001", EditorialScopeKeyCanonicalizer.normalize(" \u0043afe\u0301/001\t"));
        assertEquals("001", EditorialScopeKeyCanonicalizer.normalize("001"));
        assertEquals("001", EditorialScopeKeyCanonicalizer.normalize(" 001 "));
        assertEquals("Series/001", EditorialScopeKeyCanonicalizer.normalize("Series/001"));
        assertNotEquals(EditorialScopeKeyCanonicalizer.normalize("Series/001"),
                EditorialScopeKeyCanonicalizer.normalize("series/001"));
        assertNotEquals(EditorialScopeKeyCanonicalizer.normalize("Series/001"),
                EditorialScopeKeyCanonicalizer.normalize("Series:001"));
    }

    @Test public void scopeKeyRejectsEmptyControlAndMalformedUtf8() {
        assertThrows(() -> EditorialScopeKeyCanonicalizer.normalize(" \t\r\n "));
        assertThrows(() -> EditorialScopeKeyCanonicalizer.normalize("Series\u0000/001"));
        assertThrows(() -> EditorialScopeKeyCanonicalizer.normalize("\ud800"));
    }

    @Test public void roleAndManifestOrderingDoesNotChangeScopeIdentity() {
        EditorialRequiredInputRoleContract roles = roleContract("RAW", "DRAFT", "GLOSSARY");
        EditorialInputScopeSnapshot first = scope(roles, List.of(
                entry("DRAFT", 0, HASH_B, 20, 2),
                entry("RAW", 0, HASH_A, 10, 1),
                entry("GLOSSARY", 0, HASH_C, 5, 1)));
        EditorialInputScopeSnapshot second = scope(roles, List.of(
                entry("GLOSSARY", 0, HASH_C, 5, 1),
                entry("RAW", 0, HASH_A, 10, 1),
                entry("DRAFT", 0, HASH_B, 20, 2)));

        assertEquals(first.scopeSnapshotIdentity(), second.scopeSnapshotIdentity());
        assertEquals(first.manifestFingerprint(), second.manifestFingerprint());
        assertEquals(first.entries(), second.entries());
    }

    @Test public void scopeRejectsUnknownRoleDuplicateAndUnknownCount() {
        EditorialRequiredInputRoleContract roles = roleContract("RAW", "DRAFT");
        assertThrows(() -> scope(roles, List.of(
                entry("RAW", 0, HASH_A, 1, 1), entry("OTHER", 0, HASH_B, 1, 1))));
        assertThrows(() -> scope(roles, List.of(
                entry("RAW", 0, HASH_A, 1, 1), entry("RAW", 0, HASH_A, 1, 1),
                entry("DRAFT", 0, HASH_B, 1, 1))));
        assertThrows(() -> scope(roles, List.of(
                new EditorialInputScopeSnapshotEntry("RAW", 0L, HASH_A, 1L, null),
                entry("DRAFT", 0, HASH_B, 1, 1))));
        EditorialInputScopeSnapshot explicitZero = scope(roles, List.of(
                entry("RAW", 0, HASH_A, 1, 0), entry("DRAFT", 0, HASH_B, 1, 1)));
        assertEquals(0L, explicitZero.entries().get(1).itemCount().longValue());
    }

    @Test public void scopeOneByteManifestChangeInvalidatesIdentity() {
        EditorialRequiredInputRoleContract roles = roleContract("RAW", "DRAFT");
        EditorialInputScopeSnapshot left = scope(roles, List.of(
                entry("RAW", 0, HASH_A, 10, 1), entry("DRAFT", 0, HASH_B, 20, 2)));
        EditorialInputScopeSnapshot right = scope(roles, List.of(
                entry("RAW", 0, HASH_A, 10, 1), entry("DRAFT", 0, HASH_C, 20, 2)));

        assertNotEquals(left.manifestFingerprint(), right.manifestFingerprint());
        assertNotEquals(left.scopeSnapshotIdentity(), right.scopeSnapshotIdentity());
    }

    @Test public void closedRunUsesOnlyAllocatedOrdinalAndTrustedDataCompatible() {
        EditorialClosedRunContextDraft draft = closedDraft(
                EditorialClosedRunCompatibilityOutcome.DATA_COMPATIBLE);
        EditorialClosedRunContext first = EditorialClosedRunContext.allocate(draft, 0);
        EditorialClosedRunContext second = EditorialClosedRunContext.allocate(draft, 1);

        assertEquals(first.closedRunIdentity(), EditorialClosedRunContext.allocate(draft, 0).closedRunIdentity());
        assertNotEquals(first.closedRunIdentity(), second.closedRunIdentity());
        assertFalse(first.canonicalProjection().contains("closedAt"));
        assertFalse(first.canonicalProjection().contains("sourceRunRowId"));
        assertThrows(() -> EditorialClosedRunContext.allocate(
                closedDraft(EditorialClosedRunCompatibilityOutcome.BLOCKED), 0));
        assertThrows(() -> EditorialClosedRunContext.allocate(
                closedDraft(EditorialClosedRunCompatibilityOutcome.LEGACY_UNATTESTED), 0));
        assertThrows(() -> EditorialClosedRunContext.allocate(
                closedDraft(EditorialClosedRunCompatibilityOutcome.ENGINE_UPGRADE_REQUIRED), 0));
        assertThrows(() -> EditorialClosedRunContext.allocate(
                closedDraft(EditorialClosedRunCompatibilityOutcome.ADAPTER_REQUIRED), 0));
    }

    @Test public void bindingIsContentAddressedAndDomainSeparated() {
        EditorialRunLineageBinding first = new EditorialRunLineageBinding("binding-v1", HASH_A, HASH_B, HASH_C);
        EditorialRunLineageBinding same = new EditorialRunLineageBinding("binding-v1", HASH_A, HASH_B, HASH_C);
        EditorialRunLineageBinding changed = new EditorialRunLineageBinding("binding-v1", HASH_A, HASH_B, HASH_A);

        assertEquals(first.bindingIdentity(), same.bindingIdentity());
        assertEquals(first.bindingFingerprint(), same.bindingFingerprint());
        assertNotEquals(first.bindingIdentity(), changed.bindingIdentity());
        assertNotEquals(first.bindingFingerprint(), changed.bindingFingerprint());
        assertNotEquals(first.bindingIdentity(), first.bindingFingerprint());
    }

    @Test public void identityDomainsAreExactAndDifferent() {
        assertEquals("EDITORIAL_PROJECT_REVISION_IDENTITY_V1", EditorialProjectRevision.IDENTITY_DOMAIN);
        assertEquals("EDITORIAL_INPUT_SCOPE_IDENTITY_V1", EditorialInputScopeSnapshot.IDENTITY_DOMAIN);
        assertEquals("EDITORIAL_CLOSED_RUN_IDENTITY_V1", EditorialClosedRunContext.IDENTITY_DOMAIN);
        assertEquals("EDITORIAL_RUN_LINEAGE_BINDING_IDENTITY_V1", EditorialRunLineageBinding.IDENTITY_DOMAIN);
        assertNotEquals(EditorialProjectRevision.IDENTITY_DOMAIN, EditorialInputScopeSnapshot.IDENTITY_DOMAIN);
    }

    private static EditorialProjectRevision project(String key, String type) {
        return new EditorialProjectRevision("project-projection-v1", key, "project-definition-v1",
                type, HASH_A, HASH_B);
    }

    private static EditorialRequiredInputRoleContract roleContract(String... roles) {
        return new EditorialRequiredInputRoleContract("input-roles-v1", new LinkedHashSet<>(List.of(roles)));
    }

    private static EditorialInputScopeSnapshot scope(
            EditorialRequiredInputRoleContract roles,
            List<EditorialInputScopeSnapshotEntry> entries) {
        return new EditorialInputScopeSnapshot(HASH_A, "scope-v1", "  Series/001  ", roles,
                "manifest-v1", entries);
    }

    private static EditorialInputScopeSnapshotEntry entry(
            String role, long ordinal, String hash, long bytes, long items) {
        return new EditorialInputScopeSnapshotEntry(role, ordinal, hash, bytes, items);
    }

    private static EditorialClosedRunContextDraft closedDraft(
            EditorialClosedRunCompatibilityOutcome outcome) {
        EditorialClosedRunContractFacts facts = new EditorialClosedRunContractFacts(
                "pack-contract", "pack-contract-v1", "pack-schema", "pack-schema-v1",
                "evaluator-v1", HASH_A, HASH_B, HASH_C, "engine-v1");
        return new EditorialClosedRunContextDraft("closed-run-v1", HASH_A, HASH_B, HASH_C,
                "evaluation-1", outcome, "trusted-profile", "1", HASH_A, HASH_B, facts,
                "ROOT", "phase-1", HASH_C);
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
