package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * App-computed identity facts for one source in a P4 frozen input scope.
 * The reference is descriptive only; byte length and SHA-256 are authoritative.
 */
public record EditorialP4SourceIdentity(
        String role,
        String sourceReference,
        long byteLength,
        String sha256,
        String encoding,
        String schemaStatus,
        long ordinal) {
    public EditorialP4SourceIdentity {
        role = EditorialIdentityText.nonEmpty(role, "source role");
        sourceReference = EditorialIdentityText.nonEmpty(sourceReference, "source reference");
        if (byteLength < 0) throw new IllegalArgumentException("source byte length cannot be negative");
        sha256 = EditorialIdentityText.sha256(sha256, "source SHA-256");
        encoding = EditorialIdentityText.nonEmpty(encoding, "source encoding");
        schemaStatus = EditorialIdentityText.nonEmpty(schemaStatus, "source schema status");
        if (ordinal < 0) throw new IllegalArgumentException("source ordinal cannot be negative");
    }

    public Map<String, Object> canonicalMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", role);
        result.put("ordinal", BigDecimal.valueOf(ordinal));
        result.put("sourceReference", sourceReference);
        result.put("byteLength", BigDecimal.valueOf(byteLength));
        result.put("sha256", sha256);
        result.put("encoding", encoding);
        result.put("schemaStatus", schemaStatus);
        return result;
    }
}
