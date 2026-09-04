package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * P4 entry characterization. These assertions deliberately describe the
 * binding facts that the v18 identity objects must expose before persistence
 * and UI selection can be implemented.
 */
public final class EditorialP4BindingCharacterizationTest {
    private static final String PACK = "a".repeat(64);
    private static final String PROFILE = "b".repeat(64);
    private static final String MACHINE = "c".repeat(64);
    private static final String EVALUATION = "evaluation-p4-1";
    private static final String RAW = "d".repeat(64);
    private static final String DRAFT = "e".repeat(64);
    private static final String GLOSSARY = "f".repeat(64);
    private static final String PRONOUN = "1".repeat(64);

    @Test public void projectRevisionMustExposeExplicitPackProfileAndEvaluationFacts() {
        EditorialProjectRevision revision = new EditorialProjectRevision(
                "project-projection-v1", "series/volume-p4", "project-definition-v1", "editorial",
                PROFILE, MACHINE);

        assertTrue(revision.canonicalProjection().contains("canonicalPackHash"));
        assertTrue(revision.canonicalProjection().contains("trustedProfileId"));
        assertTrue(revision.canonicalProjection().contains("compatibilityEvaluationId"));
    }

    @Test public void inputScopeEntriesMustRetainSourceReferenceEncodingAndSchemaStatus() {
        EditorialProjectRevision revision = new EditorialProjectRevision(
                "project-projection-v1", "series/volume-p4-scope", "project-definition-v1", "editorial",
                PROFILE, MACHINE);
        EditorialRequiredInputRoleContract roles = new EditorialRequiredInputRoleContract(
                "roles-v1", new LinkedHashSet<>(List.of("RAW", "DRAFT", "GLOSSARY", "PRONOUN")));
        EditorialInputScopeSnapshot snapshot = new EditorialInputScopeSnapshot(
                revision.revisionIdentity(), "scope-v1", "chapter/001", roles, "manifest-v1", List.of(
                new EditorialInputScopeSnapshotEntry("RAW", 0L, RAW, 10L, 1L),
                new EditorialInputScopeSnapshotEntry("DRAFT", 0L, DRAFT, 20L, 2L),
                new EditorialInputScopeSnapshotEntry("GLOSSARY", 0L, GLOSSARY, 30L, 3L),
                new EditorialInputScopeSnapshotEntry("PRONOUN", 0L, PRONOUN, 40L, 4L)));

        assertTrue(snapshot.manifestCanonical().contains("sourceReference"));
        assertTrue(snapshot.manifestCanonical().contains("encoding"));
        assertTrue(snapshot.manifestCanonical().contains("schemaStatus"));
    }
}
