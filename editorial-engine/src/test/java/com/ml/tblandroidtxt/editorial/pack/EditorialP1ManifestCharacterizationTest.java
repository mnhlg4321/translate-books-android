package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * P1 characterization of the existing Pack Manifest v1 against the exact
 * test-only V5-SAFE.4.1.3 fixture. These assertions document the current
 * boundary; they do not expand the manifest model.
 */
public class EditorialP1ManifestCharacterizationTest {
    private static final String FIXTURE_RESOURCE = "/editorial-p1/editorial-pack.json";

    @Test public void exactFourEntryFixtureManifestCarriesCurrentPackIdentity() throws IOException {
        byte[] bytes = fixtureBytes();
        EditorialPackManifest manifest = EditorialPackManifest.parse(bytes);

        assertEquals(8285, bytes.length);
        assertEquals("com.ml.tblandroidtxt.editorial.safe4.full", manifest.packId());
        assertEquals("4.1.3", manifest.version());
        assertEquals("safe4.full.three-pass.v1", manifest.contractVersion());
        assertEquals("safe4.full.receipt.v1", manifest.schemaVersion());
        assertEquals("4.17.0", manifest.minimumEngineVersion());
        assertEquals("497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d",
                manifest.canonicalPackHash());
        assertEquals(manifest.canonicalJson(), new String(bytes, StandardCharsets.UTF_8));
        assertEquals(manifest.canonicalPackHash(), manifest.calculatedCanonicalPackHash());

        assertEquals(3, manifest.fileEntries().size());
        assertFile(manifest, EditorialPackFileRole.PROJECT_INSTRUCTION, "project.txt", 9485,
                "1727ae173f2cfd530eb818cae69e0d3fadc59c35e3b5b6d478704a02091a26ad");
        assertFile(manifest, EditorialPackFileRole.TURN_PROMPT, "prompt.txt", 8852,
                "d25757d1a6bddd5962a3b178b9ef850727573ae0c34867ec8f4b8450c7cd754f");
        assertFile(manifest, EditorialPackFileRole.WORKFLOW, "workflow.txt", 34917,
                "5db6b4f6509313f106499113537d2880bc6d2ff663859239dafb285557505730");
    }

    @Test public void manifestCarriesDeclarationsButNotExecutionSemantics() throws IOException {
        EditorialPackManifest manifest = EditorialPackManifest.parse(fixtureBytes());
        Map<String, Object> declarations = manifest.declarations();

        // The current model can carry the declaration-shaped portions.
        for (String field : Set.of("inputRoles", "pronounPolicy", "pairContextPolicy", "phaseGraph",
                "contextAllowList", "evidenceSchemas", "gateDefinitions", "releaseArtifacts",
                "goldenReplayCases", "migrationPolicy")) {
            assertTrue("missing declaration field " + field, declarations.containsKey(field));
        }
        assertEquals(11, manifest.requiredCapabilities().size());
        assertEquals(4, list(declarations, "inputRoles").size());
        assertEquals(7, map(declarations, "phaseGraph").get("phases") instanceof List
                ? list(map(declarations, "phaseGraph"), "phases").size() : -1);
        assertEquals(9, list(declarations, "gateDefinitions").size());
        assertEquals(24, list(declarations, "goldenReplayCases").size());

        // No current root field expresses these runtime obligations.
        for (String field : Set.of("optionalCapabilities", "sourceModes", "alternateModePolicy",
                "fullBundleValidation", "phaseVisibility", "receiptRules", "stopReasonCodes",
                "recoveryActions", "preserveDraftPolicy")) {
            assertFalse("unexpected runtime field " + field, declarations.containsKey(field));
        }

        // A declaration of nine gates/G1-G24 is accepted, but only the shallow
        // descriptor shape is validated by the current parser.
        assertNotNull(map(declarations, "releaseArtifacts").get("artifacts"));
        assertNotNull(map(declarations, "pronounPolicy").get("defaultStatus"));
        assertTrue(map(declarations, "pairContextPolicy").containsKey("optional"));
    }

    private static void assertFile(EditorialPackManifest manifest, EditorialPackFileRole role,
                                   String path, long length, String sha256) {
        EditorialPackManifest.FileEntry actual = manifest.fileEntries().stream()
                .filter(file -> file.role() == role).findFirst().orElseThrow();
        assertEquals(path, actual.path());
        assertEquals(length, actual.byteLength());
        assertEquals(sha256, actual.sha256());
        assertEquals("text/plain", actual.mediaType());
        assertEquals("UTF-8", actual.charset());
        assertEquals("FORBIDDEN", actual.bom());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map<String, Object> source, String key) {
        return (Map<String, Object>) source.get(key);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Map<String, Object> source, String key) {
        return (List<Object>) source.get(key);
    }

    private static byte[] fixtureBytes() throws IOException {
        try (InputStream input = EditorialP1ManifestCharacterizationTest.class
                .getResourceAsStream(FIXTURE_RESOURCE)) {
            if (input == null) throw new IOException("P1 manifest fixture is missing: " + FIXTURE_RESOURCE);
            return input.readAllBytes();
        }
    }
}
