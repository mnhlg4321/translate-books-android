package com.ml.tblandroidtxt.editorial.pack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Validates a complete source/evidence bundle and projects only the current phase. */
public final class EditorialPhaseContextProjector {
    private static final String IDENTITY_DOMAIN = "EDITORIAL_SAFE4_FULL_BUNDLE_IDENTITY_V1\n";
    private static final Set<String> KNOWN_ROLES = Set.of(
            EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
            EditorialSafe4Contract.GLOSSARY, EditorialSafe4Contract.PRONOUN,
            EditorialSafe4Contract.PAIR_CONTEXT, "REPORT_L1", "VI_L2", "CHANGE_MAP_L2");

    public enum ValidationCode { VALID, INVALID }

    public static final class BundleAsset {
        private final String role;
        private final String sourceId;
        private final byte[] bytes;
        private final boolean authoritative;
        private final String schemaId;

        public BundleAsset(String role, String sourceId, byte[] bytes,
                           boolean authoritative, String schemaId) {
            this.role = Objects.requireNonNull(role, "role");
            this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
            this.bytes = bytes == null ? null : bytes.clone();
            this.authoritative = authoritative;
            this.schemaId = schemaId == null ? "" : schemaId;
        }

        public String role() { return role; }
        public String sourceId() { return sourceId; }
        public byte[] bytes() { return bytes == null ? null : bytes.clone(); }
        public boolean authoritative() { return authoritative; }
        public String schemaId() { return schemaId; }
        public String sha256() { return bytes == null ? "" : EditorialCanonicalJson.sha256Hex(bytes); }
    }

    public record Bundle(List<BundleAsset> assets, EditorialSourceStatusResolver.Result pronounStatus) {
        public Bundle {
            assets = List.copyOf(assets == null ? List.of() : assets);
            Objects.requireNonNull(pronounStatus, "pronounStatus");
        }

        public String identity() {
            List<String> descriptors = new ArrayList<>();
            for (BundleAsset asset : assets) {
                descriptors.add(asset.role() + "\u0000" + asset.sourceId() + "\u0000"
                        + asset.sha256() + "\u0000" + (asset.bytes() == null ? -1 : asset.bytes().length)
                        + "\u0000" + asset.authoritative() + "\u0000" + asset.schemaId());
            }
            descriptors.sort(Comparator.naturalOrder());
            String status = pronounStatus.status().name() + "\u0000" + pronounStatus.provenance();
            return EditorialCanonicalJson.sha256Hex((IDENTITY_DOMAIN + status + "\n"
                    + String.join("\n", descriptors)).getBytes(StandardCharsets.UTF_8));
        }
    }

    public record FullBundleValidation(ValidationCode code, String bundleIdentity,
                                       List<String> issues) {
        public FullBundleValidation {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(bundleIdentity, "bundleIdentity");
            issues = List.copyOf(issues == null ? List.of() : issues);
        }

        public boolean valid() { return code == ValidationCode.VALID; }
    }

    public record PhaseProjection(String phase, String bundleIdentity,
                                  Map<String, BundleAsset> visibleAssets,
                                  Set<String> hiddenRoles,
                                  List<String> requiredRoles,
                                  EditorialSourceStatusResolver.PronounStatus pronounStatus,
                                  boolean pairContextOptional) {
        public PhaseProjection {
            Objects.requireNonNull(phase, "phase");
            Objects.requireNonNull(bundleIdentity, "bundleIdentity");
            visibleAssets = Collections.unmodifiableMap(new LinkedHashMap<>(visibleAssets));
            hiddenRoles = Set.copyOf(hiddenRoles);
            requiredRoles = List.copyOf(requiredRoles);
            Objects.requireNonNull(pronounStatus, "pronounStatus");
        }

        public boolean isVisible(String role) { return visibleAssets.containsKey(role); }
    }

    public FullBundleValidation validateFullBundle(Bundle bundle) {
        if (bundle == null) return invalid("BUNDLE_REQUIRED", "");
        List<String> issues = new ArrayList<>();
        Map<String, Integer> counts = new HashMap<>();
        Set<String> sourceIds = new HashSet<>();
        for (BundleAsset asset : bundle.assets()) {
            if (asset == null) {
                issues.add("BUNDLE_NULL_ASSET");
                continue;
            }
            if (!KNOWN_ROLES.contains(asset.role())) issues.add("BUNDLE_UNKNOWN_ROLE:" + asset.role());
            if (asset.bytes() == null) issues.add("BUNDLE_BYTES_MISSING:" + asset.role());
            if (!sourceIds.add(asset.sourceId())) issues.add("BUNDLE_DUPLICATE_SOURCE_ID:" + asset.sourceId());
            counts.merge(asset.role(), 1, Integer::sum);
            if (asset.schemaId().isBlank()) issues.add("BUNDLE_SCHEMA_MISSING:" + asset.role());
        }
        for (String required : List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                EditorialSafe4Contract.GLOSSARY)) {
            if (counts.getOrDefault(required, 0) != 1) issues.add("BUNDLE_REQUIRED_ROLE:" + required);
        }
        int pronounCount = counts.getOrDefault(EditorialSafe4Contract.PRONOUN, 0);
        if (bundle.pronounStatus().status() == EditorialSourceStatusResolver.PronounStatus.AVAILABLE) {
            if (pronounCount != 1 || !bundle.pronounStatus().hasExactlyOneAuthoritativeSource()) {
                issues.add("BUNDLE_PRONOUN_STATUS_MISMATCH");
            }
        } else if (pronounCount > 0 && !bundle.pronounStatus().quarantinedCandidates().isEmpty()) {
            // A quarantined legacy/non-authoritative candidate may remain recorded, but never authoritative.
            for (BundleAsset asset : bundle.assets()) {
                if (EditorialSafe4Contract.PRONOUN.equals(asset.role()) && asset.authoritative()) {
                    issues.add("BUNDLE_NON_AVAILABLE_PRONOUN_AUTHORITATIVE");
                }
            }
        }
        return issues.isEmpty()
                ? new FullBundleValidation(ValidationCode.VALID, bundle.identity(), List.of())
                : new FullBundleValidation(ValidationCode.INVALID, bundle.identity(), issues);
    }

    public PhaseProjection project(Bundle bundle, String phase) {
        FullBundleValidation validation = validateFullBundle(bundle);
        if (!validation.valid()) throw new IllegalArgumentException("Full bundle is invalid: " + validation.issues());
        if (!EditorialSafe4Contract.PHASES.contains(phase)) throw new IllegalArgumentException("Unknown SAFE4 phase");
        List<String> required = requiredFor(phase, bundle.pronounStatus().status());
        Set<String> allowed = allowedFor(phase);
        Map<String, BundleAsset> visible = new TreeMap<>();
        Set<String> hidden = new HashSet<>();
        for (BundleAsset asset : bundle.assets()) {
            if (allowed.contains(asset.role())) visible.put(asset.role(), asset);
            else hidden.add(asset.role());
        }
        for (String role : required) {
            if (!visible.containsKey(role)) throw new IllegalArgumentException("Required phase role is absent: " + role);
        }
        for (BundleAsset asset : bundle.assets()) {
            if (!visible.containsKey(asset.role())) hidden.add(asset.role());
        }
        return new PhaseProjection(phase, validation.bundleIdentity(), visible, hidden, required,
                bundle.pronounStatus().status(), true);
    }

    private static List<String> requiredFor(String phase, EditorialSourceStatusResolver.PronounStatus pronounStatus) {
        String encoded = EditorialSafe4Contract.phaseRequiredRoles().get(phase);
        if (encoded == null) throw new IllegalArgumentException("Unknown SAFE4 phase");
        List<String> result = new ArrayList<>(List.of(encoded.split("\\u0000", -1)));
        // Explicit NONE/LEGACY_REJECTED is a source decision, not an absent-file inference.
        // Such a decision removes PRONOUN from the phase requirement while retaining the pinned status.
        if (pronounStatus != EditorialSourceStatusResolver.PronounStatus.AVAILABLE) {
            result.remove(EditorialSafe4Contract.PRONOUN);
        }
        return List.copyOf(result);
    }

    private static Set<String> allowedFor(String phase) {
        return switch (phase) {
            case "L1_SOURCE_PREFLIGHT" -> Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                    EditorialSafe4Contract.GLOSSARY, EditorialSafe4Contract.PRONOUN);
            case "L1_RAW_DISCOVERY", "L2_RAW_DISCOVERY" -> Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY);
            case "L1_RECONCILE" -> Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                    EditorialSafe4Contract.GLOSSARY, EditorialSafe4Contract.PRONOUN, EditorialSafe4Contract.PAIR_CONTEXT);
            case "L2_EDIT" -> Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                    EditorialSafe4Contract.GLOSSARY, "REPORT_L1", EditorialSafe4Contract.PRONOUN,
                    EditorialSafe4Contract.PAIR_CONTEXT);
            case "L3_RAW_FIRST_REAUDIT" -> Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.GLOSSARY, "VI_L2");
            case "L3_RECONCILE" -> Set.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                    EditorialSafe4Contract.GLOSSARY, "REPORT_L1", "VI_L2", "CHANGE_MAP_L2",
                    EditorialSafe4Contract.PRONOUN, EditorialSafe4Contract.PAIR_CONTEXT);
            default -> throw new IllegalArgumentException("Unknown SAFE4 phase");
        };
    }

    private static FullBundleValidation invalid(String issue, String identity) {
        return new FullBundleValidation(ValidationCode.INVALID, identity, List.of(issue));
    }
}
