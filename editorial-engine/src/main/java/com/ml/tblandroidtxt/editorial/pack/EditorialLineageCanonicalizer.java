package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canonical semantic projections and SHA-256 domain-separated lineage hashes. */
public final class EditorialLineageCanonicalizer {
    public static final String INPUT_MANIFEST_DOMAIN = "EDITORIAL_LINEAGE_INPUT_MANIFEST_V1\n";
    public static final String IDENTITY_DOMAIN = "EDITORIAL_LINEAGE_IDENTITY_V1\n";
    public static final String RECORD_FINGERPRINT_DOMAIN = "EDITORIAL_LINEAGE_RECORD_FINGERPRINT_V1\n";

    private EditorialLineageCanonicalizer() { }

    public static String canonicalInputManifestJson(EditorialLineageInputManifest manifest) {
        if (manifest == null) throw new IllegalArgumentException("Input manifest is required");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("manifestVersion", manifest.manifestVersion());
        root.put("entries", canonicalEntries(manifest.entries()));
        return EditorialCanonicalJson.canonicalize(root);
    }

    public static String canonicalIdentityJson(EditorialLineageIdentity identity) {
        if (identity == null) throw new IllegalArgumentException("Lineage identity is required");
        return EditorialCanonicalJson.canonicalize(identityMap(identity));
    }

    public static String canonicalParentReferenceJson(EditorialLineageParentReference parent) {
        if (parent == null) return "null";
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("parentRecordIdentity", parent.parentRecordIdentity());
        root.put("parentRecordFingerprint", parent.parentRecordFingerprint());
        return EditorialCanonicalJson.canonicalize(root);
    }

    public static String canonicalRecordPayloadJson(EditorialLineageIdentity identity,
                                                     EditorialLineageParentReference parent,
                                                     String recordIdentity) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("identity", identityMap(identity));
        root.put("nodeKind", identity.nodeKind().name());
        root.put("parent", parentMap(parent));
        root.put("recordIdentity", recordIdentity);
        return EditorialCanonicalJson.canonicalize(root);
    }

    public static String inputManifestFingerprint(EditorialLineageInputManifest manifest) {
        return hash(INPUT_MANIFEST_DOMAIN, canonicalInputManifestJson(manifest));
    }

    /** Record identity intentionally excludes the parent so reparenting is detectable. */
    public static String recordIdentity(EditorialLineageIdentity identity) {
        return hash(IDENTITY_DOMAIN, canonicalIdentityJson(identity));
    }

    /** Record fingerprint binds the stable node identity to its exact parent reference. */
    public static String recordFingerprint(EditorialLineageIdentity identity,
                                           EditorialLineageParentReference parent,
                                           String recordIdentity) {
        return hash(RECORD_FINGERPRINT_DOMAIN,
                canonicalRecordPayloadJson(identity, parent, recordIdentity));
    }

    public static byte[] strictUtf8(String canonicalText) {
        if (canonicalText == null) throw new IllegalArgumentException("Canonical text is required");
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(canonicalText));
            byte[] result = new byte[encoded.remaining()];
            encoded.get(result);
            if (result.length >= 3 && (result[0] & 0xff) == 0xef
                    && (result[1] & 0xff) == 0xbb && (result[2] & 0xff) == 0xbf) {
                throw new IllegalArgumentException("UTF-8 BOM is forbidden");
            }
            return result;
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Lineage canonical text is not strict UTF-8", e);
        }
    }

    private static Map<String, Object> identityMap(EditorialLineageIdentity identity) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("lineageContract", EditorialLineageIdentity.LINEAGE_CONTRACT);
        root.put("canonicalPackHash", identity.canonicalPackHash());
        root.put("trustedProfileId", identity.trustedProfileId());
        root.put("trustedProfileVersion", identity.trustedProfileVersion());
        root.put("canonicalProfileHash", identity.canonicalProfileHash());
        root.put("machineContractFingerprint", identity.machineContractFingerprint());
        root.put("contractVersion", identity.contractVersion());
        root.put("schemaVersion", identity.schemaVersion());
        root.put("projectIdentity", identity.projectIdentity());
        root.put("inputScopeIdentity", identity.inputScopeIdentity());
        root.put("runEvaluationIdentity", identity.runEvaluationIdentity());
        root.put("inputManifest", inputManifestMap(identity.inputManifest()));
        root.put("nodeKind", identity.nodeKind().name());
        return root;
    }

    private static Map<String, Object> inputManifestMap(EditorialLineageInputManifest manifest) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("manifestVersion", manifest.manifestVersion());
        root.put("entries", canonicalEntries(manifest.entries()));
        return root;
    }

    private static List<Object> canonicalEntries(List<EditorialLineageInputEntry> entries) {
        ArrayList<EditorialLineageInputEntry> ordered = new ArrayList<>(entries);
        ordered.sort(Comparator.comparing(EditorialLineageInputEntry::role)
                .thenComparingInt(EditorialLineageInputEntry::ordinal)
                .thenComparing(EditorialLineageInputEntry::inputHash)
                .thenComparingLong(EditorialLineageInputEntry::byteCount)
                .thenComparingLong(EditorialLineageInputEntry::itemCount));
        ArrayList<Object> result = new ArrayList<>(ordered.size());
        for (EditorialLineageInputEntry entry : ordered) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", entry.role());
            item.put("ordinal", BigDecimal.valueOf(entry.ordinal()));
            item.put("inputHash", entry.inputHash());
            item.put("byteCount", BigDecimal.valueOf(entry.byteCount()));
            item.put("itemCount", BigDecimal.valueOf(entry.itemCount()));
            result.add(item);
        }
        return result;
    }

    private static Object parentMap(EditorialLineageParentReference parent) {
        if (parent == null) return null;
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("parentRecordIdentity", parent.parentRecordIdentity());
        root.put("parentRecordFingerprint", parent.parentRecordFingerprint());
        return root;
    }

    private static String hash(String domain, String canonicalJson) {
        byte[] domainBytes = strictUtf8(domain);
        byte[] jsonBytes = strictUtf8(canonicalJson);
        byte[] payload = new byte[domainBytes.length + jsonBytes.length];
        System.arraycopy(domainBytes, 0, payload, 0, domainBytes.length);
        System.arraycopy(jsonBytes, 0, payload, domainBytes.length, jsonBytes.length);
        return EditorialCanonicalJson.sha256Hex(payload);
    }
}
