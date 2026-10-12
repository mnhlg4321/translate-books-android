package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditorialPackIntegrityValidatorTest {
    private final EditorialPackIntegrityValidator validator = new EditorialPackIntegrityValidator();

    @Test public void validManifestAndExactlyThreeFilesPass() {
        EditorialPackFixtures.Fixture fixture = EditorialPackFixtures.valid();
        EditorialPackIntegrityResult result = validator.validate(fixture.manifestBytes(), fixture.dataFiles());
        assertTrue(result.valid());
        assertEquals(EditorialPackValidationCode.VALID, result.primaryCode());
    }

    @Test public void missingRoleExtraFileAndWrongHashFailClosed() {
        EditorialPackFixtures.Fixture fixture = EditorialPackFixtures.valid();
        Map<String, byte[]> missing = new HashMap<>(fixture.dataFiles());
        missing.remove("workflow.txt");
        EditorialPackIntegrityResult missingResult = validator.validate(fixture.manifestBytes(), missing);
        assertFalse(missingResult.valid());
        assertTrue(missingResult.issues().stream().anyMatch(i -> i.code() == EditorialPackValidationCode.MISSING_FILE));

        Map<String, byte[]> extra = new HashMap<>(fixture.dataFiles());
        extra.put("extra.txt", "extra".getBytes(StandardCharsets.UTF_8));
        assertTrue(validator.validate(fixture.manifestBytes(), extra).issues().stream().anyMatch(i -> i.code() == EditorialPackValidationCode.EXTRA_FILE));

        Map<String, byte[]> wrong = new HashMap<>(fixture.dataFiles());
        wrong.put("prompt.txt", "tampered\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(validator.validate(fixture.manifestBytes(), wrong).issues().stream().anyMatch(i -> i.code() == EditorialPackValidationCode.FILE_HASH_MISMATCH));
    }

    @Test public void canonicalHashAndManifestCanonicalityAreRequired() {
        EditorialPackFixtures.Fixture fixture = EditorialPackFixtures.valid();
        String wrongHash = new String(fixture.manifestBytes(), StandardCharsets.UTF_8).replace(fixture.manifest().canonicalPackHash(), "0000000000000000000000000000000000000000000000000000000000000000");
        EditorialPackIntegrityResult wrong = validator.validate(wrongHash.getBytes(StandardCharsets.UTF_8), fixture.dataFiles());
        assertTrue(wrong.issues().stream().anyMatch(i -> i.code() == EditorialPackValidationCode.CANONICAL_HASH_MISMATCH));

        String reordered = "{\"version\":\"5.0.4\",\"manifestFormat\":\"com.ml.tblandroidtxt.editorial-pack\"}";
        assertFalse(validator.validate(reordered.getBytes(StandardCharsets.UTF_8), fixture.dataFiles()).valid());
    }

    @Test public void oneByteChangeChangesFileAndPackHash() {
        EditorialPackFixtures.Fixture first = EditorialPackFixtures.valid();
        byte[] changed = first.dataFiles().get("prompt.txt").clone();
        changed[0] = (byte) (changed[0] ^ 1);
        EditorialPackFixtures.Fixture second = EditorialPackFixtures.create("com.example.editorial.safe4", "5.0.4", changed, "DATA_COMPATIBLE");
        assertFalse(first.manifest().canonicalPackHash().equals(second.manifest().canonicalPackHash()));
        assertFalse(first.manifest().fileEntries().get(1).sha256().equals(second.manifest().fileEntries().get(1).sha256()));
    }
}
