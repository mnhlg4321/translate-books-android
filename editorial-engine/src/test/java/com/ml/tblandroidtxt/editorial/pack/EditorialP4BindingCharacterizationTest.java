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

    @Test public void P4BindingExposesExplicitPackProfileAndEvaluationFacts() {
        EditorialP4Binding binding = binding();

        assertTrue(binding.canonicalProjection().contains("canonicalPackHash"));
        assertTrue(binding.canonicalProjection().contains("trustedProfileId"));
        assertTrue(binding.canonicalProjection().contains("compatibilityEvaluationId"));
        assertTrue(binding.canonicalProjection().contains("\"executionAllowed\":false"));
    }

    @Test public void P4BindingInputIdentitiesRetainSourceReferenceEncodingAndSchemaStatus() {
        String projection = binding().canonicalProjection();
        assertTrue(projection.contains("sourceReference"));
        assertTrue(projection.contains("encoding"));
        assertTrue(projection.contains("schemaStatus"));
    }

    private static EditorialP4Binding binding() {
        return new EditorialP4Binding(
                "2".repeat(64), "3".repeat(64), "4".repeat(64),
                "com.example.pack", "4.1.3", "5".repeat(64), "6".repeat(64),
                "com.example.profile", "2.0.0", "7".repeat(64), "8".repeat(64),
                "evaluation-p4", "DATA_COMPATIBLE", "9".repeat(64),
                "safe4.full.three-pass.v1", "safe4.full.receipt.v1", "a".repeat(64),
                "b".repeat(64), EditorialSafe4Contract.NORMAL_MODE, "AVAILABLE", "NONE", "NONE", "user-confirmed-normal",
                "c".repeat(64), 0L, "EDITORIAL_SETUP", "L1_SOURCE_PREFLIGHT",
                List.of(
                        new EditorialP4SourceIdentity("RAW", "content://raw", 10L, RAW,
                                "UTF-8", "VALID", 0L),
                        new EditorialP4SourceIdentity("DRAFT", "content://draft", 20L, DRAFT,
                                "UTF-8", "VALID", 0L),
                        new EditorialP4SourceIdentity("GLOSSARY", "content://glossary", 30L, GLOSSARY,
                                "UTF-8", "VALID", 0L)));
    }
}
