package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Host characterization for the frozen P2 manifest and its single-cause negatives. */
public class EditorialP2ReferencePackTest {
    private static final String MANIFEST_RESOURCE = "/editorial-p2/editorial-pack.json";
    private static final String MANIFEST_SHA256 = "3e88503e312db8da351ca574820c98216ab6fd3fa233e35aedb0db379e50013a";
    private static final String CANONICAL_PACK_HASH = "497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d";

    @Test public void canonicalManifestFreezesAllPackManifestV1Declarations() throws IOException {
        byte[] bytes = manifestBytes();
        EditorialPackManifest manifest = EditorialPackManifest.parse(bytes);

        assertEquals(8285, bytes.length);
        assertEquals(MANIFEST_SHA256, EditorialCanonicalJson.sha256Hex(bytes));
        assertEquals(manifest.canonicalJson(), new String(bytes, StandardCharsets.UTF_8));
        assertEquals(CANONICAL_PACK_HASH, manifest.canonicalPackHash());
        assertEquals(manifest.canonicalPackHash(), manifest.calculatedCanonicalPackHash());
        assertEquals("com.ml.tblandroidtxt.editorial-pack", manifest.manifestFormat());
        assertEquals(1, manifest.manifestVersion());
        assertEquals("com.ml.tblandroidtxt.editorial.safe4.full", manifest.packId());
        assertEquals("4.1.3", manifest.version());
        assertEquals("safe4.full.three-pass.v1", manifest.contractVersion());
        assertEquals("safe4.full.receipt.v1", manifest.schemaVersion());
        assertEquals("4.17.0", manifest.minimumEngineVersion());
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, manifest.declaredCompatibilityClass());
        assertEquals(EditorialPronounPolicy.NONE, manifest.pronounPolicy());

        assertEquals(11, manifest.requiredCapabilities().size());
        assertTrue(manifest.requiredCapabilities().containsAll(Set.of(
                "pack.integrity.sha256.v1", "source.preflight.safe4-full.v1",
                "bundle.phase-visibility.safe4-full.v1", "status.glossary-pronoun.safe4-full.v1",
                "ledger.exhaustive.safe4-full.v1", "preserve.draft.safe4-full.v1",
                "stop.typed.safe4-full.v1", "diff.change-coverage.v1",
                "qa.l3-two-adversarial.v1", "release.safe4-full.v1",
                "replay.safe4.g1-g24.v1")));
        assertEquals(3, manifest.fileEntries().size());
        assertEquals(24, list(manifest.declarations().get("goldenReplayCases")).size());
        assertEquals(9, list(manifest.declarations().get("gateDefinitions")).size());
        assertEquals(7, map(manifest.declarations().get("phaseGraph")).get("phases") instanceof List<?> ?
                list(map(manifest.declarations().get("phaseGraph")).get("phases")).size() : -1);

        Map<String, Object> pairContext = map(manifest.declarations().get("pairContextPolicy"));
        assertEquals(Boolean.TRUE, pairContext.get("optional"));
        Map<String, Object> release = map(manifest.declarations().get("releaseArtifacts"));
        assertTrue(list(release.get("forbiddenArtifacts")).contains("EXECUTABLE_CODE"));
        assertTrue(list(release.get("forbiddenArtifacts")).contains("REMOTE_SCRIPT"));
        assertFalse(manifest.declarations().containsKey("execution"));
        assertFalse(manifest.declarations().containsKey("certificationState"));
    }

    @Test public void independentManifestNegativeFixturesAreTypedAndFailClosed() throws IOException {
        EditorialPackManifest base = EditorialPackManifest.parse(manifestBytes());
        EditorialPackIntegrityValidator validator = new EditorialPackIntegrityValidator();

        assertCode(validator, variant(base, root -> root.remove("phaseGraph"), true),
                EditorialPackValidationCode.MISSING_MANIFEST_FIELD);
        assertCode(validator, variant(base, root -> root.put("executablePayload", "script"), true),
                EditorialPackValidationCode.UNKNOWN_MANIFEST_FIELD);
        assertCode(validator, variant(base, root -> {
            List<Object> files = list(root.get("fileRoles"));
            map(files.get(1)).put("role", "PROJECT_INSTRUCTION");
        }, true), EditorialPackValidationCode.DUPLICATE_FILE_ROLE);
        assertCode(validator, variant(base, root -> {
            List<Object> files = list(root.get("fileRoles"));
            map(files.get(1)).put("path", "nested/prompt.txt");
        }, true), EditorialPackValidationCode.INVALID_MANIFEST_FIELD);
        assertCode(validator, addBom(manifestBytes()), EditorialPackValidationCode.BOM_FORBIDDEN);
        assertCode(validator, variant(base, root -> root.put("canonicalPackHash", "0".repeat(64)), false),
                EditorialPackValidationCode.CANONICAL_HASH_MISMATCH);

        byte[] lengthMutation = variant(base, root -> {
            for (Object value : list(root.get("fileRoles"))) {
                Map<String, Object> file = map(value);
                if ("prompt.txt".equals(file.get("path"))) {
                    file.put("byteLength", BigDecimal.valueOf(((Number) file.get("byteLength")).longValue() + 1));
                }
            }
        }, true);
        EditorialPackManifest lengthManifest = EditorialPackManifest.parse(lengthMutation);
        assertEquals(8853, lengthManifest.fileEntries().stream()
                .filter(file -> "prompt.txt".equals(file.path())).findFirst().orElseThrow().byteLength());

        byte[] hashMutation = variant(base, root -> {
            for (Object value : list(root.get("fileRoles"))) {
                Map<String, Object> file = map(value);
                if ("prompt.txt".equals(file.get("path"))) file.put("sha256", "0".repeat(64));
            }
        }, true);
        EditorialPackManifest hashManifest = EditorialPackManifest.parse(hashMutation);
        assertEquals("0".repeat(64), hashManifest.fileEntries().stream()
                .filter(file -> "prompt.txt".equals(file.path())).findFirst().orElseThrow().sha256());
    }

    private static void assertCode(EditorialPackIntegrityValidator validator, byte[] manifestBytes,
                                   EditorialPackValidationCode expected) {
        EditorialPackIntegrityResult result = validator.validate(manifestBytes, Map.of());
        assertEquals(expected, result.primaryCode());
        assertFalse(result.valid());
    }

    private static byte[] manifestBytes() throws IOException {
        try (InputStream input = EditorialP2ReferencePackTest.class.getResourceAsStream(MANIFEST_RESOURCE)) {
            if (input == null) throw new IOException("Missing P2 manifest resource: " + MANIFEST_RESOURCE);
            return input.readAllBytes();
        }
    }

    private static byte[] addBom(byte[] bytes) {
        byte[] result = new byte[bytes.length + 3];
        result[0] = (byte) 0xef;
        result[1] = (byte) 0xbb;
        result[2] = (byte) 0xbf;
        System.arraycopy(bytes, 0, result, 3, bytes.length);
        return result;
    }

    private static byte[] variant(EditorialPackManifest original, Consumer<Map<String, Object>> mutation,
                                  boolean recalculateHash) {
        Map<String, Object> root = mutableMap(original.declarations());
        mutation.accept(root);
        if (recalculateHash) {
            root.put("canonicalPackHash", "0".repeat(64));
            root.put("canonicalPackHash", EditorialPackFixtures.canonicalHashWithout(root));
        }
        return EditorialCanonicalJson.canonicalize(root).getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) { return (Map<String, Object>) value; }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) { return (List<Object>) value; }

    private static Map<String, Object> mutableMap(Map<String, Object> original) {
        return map(mutable(original));
    }

    private static Object mutable(Object value) {
        if (value instanceof Map<?, ?> source) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) copy.put((String) entry.getKey(), mutable(entry.getValue()));
            return copy;
        }
        if (value instanceof List<?> source) {
            ArrayList<Object> copy = new ArrayList<>(source.size());
            for (Object item : source) copy.add(mutable(item));
            return copy;
        }
        return value;
    }
}
