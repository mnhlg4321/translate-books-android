package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class EditorialPackManifestTest {
    @Test public void validManifestExposesImmutableContractIdentity() {
        EditorialPackManifest manifest = EditorialPackFixtures.valid().manifest();
        assertEquals("com.example.editorial.safe4", manifest.packId());
        assertEquals("contract.test.v1", manifest.contractVersion());
        assertEquals(EditorialPronounPolicy.NONE, manifest.pronounPolicy());
        assertEquals(3, manifest.fileEntries().size());
        assertThrows(UnsupportedOperationException.class, () -> manifest.requiredCapabilities().add("evil"));
    }

    @Test public void missingFieldAndDuplicateRoleAreRejected() {
        EditorialPackFixtures.Fixture fixture = EditorialPackFixtures.valid();
        String missing = new String(fixture.manifestBytes(), StandardCharsets.UTF_8).replace(",\"createdAt\":\"2026-08-03T00:00:00Z\"", "");
        assertThrows(IllegalArgumentException.class, () -> EditorialPackManifest.parse(missing.getBytes(StandardCharsets.UTF_8)));
        String duplicateRole = new String(fixture.manifestBytes(), StandardCharsets.UTF_8).replaceFirst("PROJECT_INSTRUCTION", "WORKFLOW");
        assertThrows(IllegalArgumentException.class, () -> EditorialPackManifest.parse(duplicateRole.getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void oneByteOrPromptHashChangeCreatesNewPackIdentity() {
        EditorialPackFixtures.Fixture first = EditorialPackFixtures.valid();
        EditorialPackFixtures.Fixture second = EditorialPackFixtures.create("com.example.editorial.safe4", "5.0.4", "prompt changed\n", "DATA_COMPATIBLE");
        assertNotEquals(first.manifest().canonicalPackHash(), second.manifest().canonicalPackHash());
        assertNotEquals(first.manifest().fileEntries().get(1).sha256(), second.manifest().fileEntries().get(1).sha256());
    }

    @Test public void dbe214PromptIsNotCode86CanonicalPrompt() {
        assertNotEquals(EditorialPackFixtures.CODE86_PROMPT_SHA, EditorialPackFixtures.DBE214_PROMPT_SHA);
        assertNotEquals(EditorialPackFixtures.CODE86_PROMPT_SHA, EditorialPackFixtures.CURRENT_EXTERNAL_PROMPT_SHA);
        assertNotEquals(EditorialPackFixtures.DBE214_PROMPT_SHA, EditorialPackFixtures.CURRENT_EXTERNAL_PROMPT_SHA);
        EditorialPackFixtures.Fixture canonical = EditorialPackFixtures.valid();
        EditorialPackFixtures.Fixture candidate = EditorialPackFixtures.create(canonical.manifest().packId(), canonical.manifest().version(), "DBE214 candidate bytes\n", "DATA_COMPATIBLE");
        assertNotEquals(canonical.manifest().canonicalPackHash(), candidate.manifest().canonicalPackHash());
    }
}
