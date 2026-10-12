package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Reviewed, caller-supplied role contract; it is never inferred from UI schema. */
public final class EditorialRequiredInputRoleContract {
    public static final String FINGERPRINT_DOMAIN =
            "EDITORIAL_INPUT_SCOPE_REQUIRED_ROLES_FINGERPRINT_V1";

    private final String contractVersion;
    private final List<String> requiredRoles;
    private final String canonical;
    private final String fingerprint;

    public EditorialRequiredInputRoleContract(String contractVersion, Set<String> requiredRoles) {
        this.contractVersion = EditorialIdentityText.nonEmpty(contractVersion,
                "required-role contract version");
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            throw new IllegalArgumentException("required roles are required");
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String role : requiredRoles) {
            String canonicalRole = EditorialIdentityText.nonEmpty(role, "required role");
            if (!unique.add(canonicalRole)) throw new IllegalArgumentException("duplicate required role");
        }
        ArrayList<String> ordered = new ArrayList<>(unique);
        ordered.sort(Comparator.naturalOrder());
        this.requiredRoles = List.copyOf(ordered);
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("contractVersion", this.contractVersion);
        projection.put("requiredRoles", this.requiredRoles);
        this.canonical = EditorialCanonicalJson.canonicalize(projection);
        this.fingerprint = EditorialIdentityText.hash(FINGERPRINT_DOMAIN, canonical);
    }

    public String contractVersion() { return contractVersion; }
    public List<String> requiredRoles() { return requiredRoles; }
    public Set<String> requiredRoleSet() { return Set.copyOf(requiredRoles); }
    public String canonical() { return canonical; }
    public String fingerprint() { return fingerprint; }

    public boolean contains(String role) { return requiredRoles.contains(role); }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EditorialRequiredInputRoleContract that)) return false;
        return canonical.equals(that.canonical);
    }

    @Override public int hashCode() { return Objects.hash(canonical); }
}
