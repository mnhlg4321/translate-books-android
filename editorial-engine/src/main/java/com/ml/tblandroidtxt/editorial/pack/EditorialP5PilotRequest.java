package com.ml.tblandroidtxt.editorial.pack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact, app-owned input envelope for one P5 L1 attempt. */
public final class EditorialP5PilotRequest {
    private static final String REQUEST_IDENTITY_DOMAIN = "EDITORIAL_P5_L1_REQUEST_IDENTITY_V1\n";
    private static final String ATTEMPT_IDENTITY_DOMAIN = "EDITORIAL_P5_L1_ATTEMPT_IDENTITY_V1\n";
    private static final String LEDGER_ATTEMPT_IDENTITY_DOMAIN = "EDITORIAL_P5_L1_ATTEMPT_IDENTITY_V2\n";

    public enum Phase {
        L1_RAW_DISCOVERY,
        L1_RECONCILE
    }

    /** Source bytes are app-read values, never model-declared identities. */
    public record SourceBytes(String role, String sourceReference, byte[] bytes,
                              String encoding, String schemaId, String schemaStatus,
                              long ordinal) {
        public SourceBytes {
            role = text(role, "source role");
            sourceReference = text(sourceReference, "source reference");
            bytes = bytes == null ? null : bytes.clone();
            encoding = text(encoding, "source encoding");
            schemaId = text(schemaId, "source schema id");
            schemaStatus = text(schemaStatus, "source schema status");
            if (ordinal < 0) throw new IllegalArgumentException("source ordinal cannot be negative");
        }

        @Override public byte[] bytes() { return bytes == null ? null : bytes.clone(); }

        public String actualSha256() {
            return bytes == null ? "" : EditorialCanonicalJson.sha256Hex(bytes);
        }

        public long actualByteLength() { return bytes == null ? -1L : bytes.length; }
    }

    /** Pack payload used to build a request; all accessors return defensive copies. */
    public static final class PackAuthority {
        private final Map<EditorialPackFileRole, byte[]> bytesByRole;

        public PackAuthority(Map<EditorialPackFileRole, byte[]> values) {
            LinkedHashMap<EditorialPackFileRole, byte[]> copy = new LinkedHashMap<>();
            if (values != null) {
                for (Map.Entry<EditorialPackFileRole, byte[]> entry : values.entrySet()) {
                    if (entry.getKey() == null) throw new IllegalArgumentException("null authority role");
                    copy.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().clone());
                }
            }
            bytesByRole = Map.copyOf(copy);
        }

        public byte[] bytes(EditorialPackFileRole role) {
            byte[] value = bytesByRole.get(role);
            return value == null ? null : value.clone();
        }

        public Map<EditorialPackFileRole, byte[]> entries() {
            LinkedHashMap<EditorialPackFileRole, byte[]> copy = new LinkedHashMap<>();
            for (Map.Entry<EditorialPackFileRole, byte[]> entry : bytesByRole.entrySet()) {
                copy.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().clone());
            }
            return Map.copyOf(copy);
        }
    }

    private final EditorialP4Binding binding;
    private final EditorialPackManifest manifest;
    private final PackAuthority authority;
    private final String chapterKey;
    private final String phase;
    private final List<SourceBytes> sources;
    private final String predecessorIdentity;
    private final List<String> stableAnchors;
    private final List<String> populationIds;
    private final boolean evidenceContentSufficient;
    private final int requestedOutputTokens;
    private final String contractRevision;
    private final byte[] predecessorReport;

    public EditorialP5PilotRequest(
            EditorialP4Binding binding,
            EditorialPackManifest manifest,
            PackAuthority authority,
            String chapterKey,
            Phase phase,
            List<SourceBytes> sources,
            String predecessorIdentity,
            List<String> stableAnchors,
            List<String> populationIds,
            boolean evidenceContentSufficient,
            int requestedOutputTokens) {
        this(binding, manifest, authority, chapterKey, phase == null ? null : phase.name(),
                sources, predecessorIdentity, stableAnchors, populationIds,
                evidenceContentSufficient, requestedOutputTokens);
    }

    private EditorialP5PilotRequest(
            EditorialP4Binding binding,
            EditorialPackManifest manifest,
            PackAuthority authority,
            String chapterKey,
            String phase,
            List<SourceBytes> sources,
            String predecessorIdentity,
            List<String> stableAnchors,
            List<String> populationIds,
            boolean evidenceContentSufficient,
            int requestedOutputTokens) {
        this(binding, manifest, authority, chapterKey, phase, sources, predecessorIdentity, stableAnchors,
                populationIds, evidenceContentSufficient, requestedOutputTokens,
                EditorialContractRevision.LEGACY_V1, null);
    }

    private EditorialP5PilotRequest(
            EditorialP4Binding binding,
            EditorialPackManifest manifest,
            PackAuthority authority,
            String chapterKey,
            String phase,
            List<SourceBytes> sources,
            String predecessorIdentity,
            List<String> stableAnchors,
            List<String> populationIds,
            boolean evidenceContentSufficient,
            int requestedOutputTokens,
            String contractRevision,
            byte[] predecessorReport) {
        if (!EditorialContractRevision.known(contractRevision)) {
            throw new IllegalArgumentException("unknown contract revision");
        }
        this.contractRevision = contractRevision;
        this.predecessorReport = predecessorReport == null ? null : predecessorReport.clone();
        this.binding = Objects.requireNonNull(binding, "binding");
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.authority = Objects.requireNonNull(authority, "authority");
        this.chapterKey = text(chapterKey, "chapter key");
        this.phase = text(phase, "phase");
        ArrayList<SourceBytes> sourceCopy = new ArrayList<>();
        if (sources != null) {
            for (SourceBytes source : sources) {
                if (source == null) throw new IllegalArgumentException("null source");
                sourceCopy.add(source);
            }
        }
        this.sources = List.copyOf(sourceCopy);
        this.predecessorIdentity = text(predecessorIdentity, "predecessor identity");
        this.stableAnchors = List.copyOf(stableAnchors == null ? List.of() : stableAnchors);
        this.populationIds = List.copyOf(populationIds == null ? List.of() : populationIds);
        this.evidenceContentSufficient = evidenceContentSufficient;
        if (requestedOutputTokens < 0) throw new IllegalArgumentException("requested output tokens cannot be negative");
        this.requestedOutputTokens = requestedOutputTokens;
    }

    public EditorialP4Binding binding() { return binding; }
    public EditorialPackManifest manifest() { return manifest; }
    public PackAuthority authority() { return authority; }
    public String chapterKey() { return chapterKey; }
    public String phase() { return phase; }
    public List<SourceBytes> sources() { return sources; }
    public String predecessorIdentity() { return predecessorIdentity; }
    public List<String> stableAnchors() { return stableAnchors; }
    public List<String> populationIds() { return populationIds; }
    public boolean evidenceContentSufficient() { return evidenceContentSufficient; }
    public int requestedOutputTokens() { return requestedOutputTokens; }

    /** Contract revision this attempt runs under; the legacy value reproduces every pre-ledger identity. */
    public String contractRevision() { return contractRevision; }

    /**
     * Persisted REPORT_L1 of the RAW-phase predecessor for a ledger-contract RECONCILE; null otherwise. It is
     * app-owned evidence (its hash is already part of the predecessor attempt) and not a source role.
     */
    public byte[] predecessorReport() { return predecessorReport == null ? null : predecessorReport.clone(); }

    public String manifestFingerprint() {
        return EditorialCanonicalJson.sha256Hex(manifest.canonicalJson().getBytes(StandardCharsets.UTF_8));
    }

    /** Identity of all currently supplied bundle bytes, including explicit pronoun provenance. */
    public String bundleIdentity() {
        return bundle().identity();
    }

    /** Stable identity for the exact project/run/chapter/phase attempt. */
    public String attemptIdentity() {
        String value = binding.bindingIdentity() + "\n" + binding.runDeclarationIdentity() + "\n"
                + binding.canonicalPackHash() + "\n" + binding.canonicalProfileHash() + "\n"
                + binding.compatibilityEvaluationId() + "\n" + chapterKey + "\n" + phase + "\n"
                + predecessorIdentity + "\n" + bundleIdentity();
        if (EditorialContractRevision.isLedger(contractRevision)) {
            return EditorialCanonicalJson.sha256Hex((LEDGER_ATTEMPT_IDENTITY_DOMAIN + value
                    + EditorialContractRevision.identitySuffix(contractRevision))
                    .getBytes(StandardCharsets.UTF_8));
        }
        return EditorialCanonicalJson.sha256Hex((ATTEMPT_IDENTITY_DOMAIN + value)
                .getBytes(StandardCharsets.UTF_8));
    }

    /** Stable identity for the request facts; visible projection is hashed by the executor separately. */
    public String requestIdentity() {
        ArrayList<String> descriptors = new ArrayList<>();
        for (SourceBytes source : sources) descriptors.add(sourceDescriptor(source));
        descriptors.sort(Comparator.naturalOrder());
        String value = binding.bindingIdentity() + "\n" + manifestFingerprint() + "\n"
                + chapterKey + "\n" + phase + "\n" + predecessorIdentity + "\n"
                + String.join("\n", stableAnchors) + "\n" + String.join("\n", populationIds)
                + "\n" + String.join("\n", descriptors)
                + EditorialContractRevision.identitySuffix(contractRevision);
        return EditorialCanonicalJson.sha256Hex((REQUEST_IDENTITY_DOMAIN + value)
                .getBytes(StandardCharsets.UTF_8));
    }

    /** The same facts under another contract revision; the attempt and request identities change with it. */
    public EditorialP5PilotRequest withContractRevision(String revision) {
        return new EditorialP5PilotRequest(binding, manifest, authority, chapterKey, phase, sources,
                predecessorIdentity, stableAnchors, populationIds, evidenceContentSufficient,
                requestedOutputTokens, revision, predecessorReport);
    }

    /** Attaches the RAW-phase REPORT_L1 bytes a ledger-contract RECONCILE resolves. */
    public EditorialP5PilotRequest withPredecessorReport(byte[] report) {
        return new EditorialP5PilotRequest(binding, manifest, authority, chapterKey, phase, sources,
                predecessorIdentity, stableAnchors, populationIds, evidenceContentSufficient,
                requestedOutputTokens, contractRevision, report);
    }

    /** The same facts with another predecessor attempt identity (the RAW attempt of a RECONCILE). */
    public EditorialP5PilotRequest withPredecessorIdentity(String value) {
        return new EditorialP5PilotRequest(binding, manifest, authority, chapterKey, phase, sources,
                value, stableAnchors, populationIds, evidenceContentSufficient,
                requestedOutputTokens, contractRevision, predecessorReport);
    }

    public EditorialP5PilotRequest withPhase(String value) {
        return copy(value, sources, evidenceContentSufficient);
    }

    public EditorialP5PilotRequest withSourceBytes(String role, byte[] value) {
        ArrayList<SourceBytes> updated = new ArrayList<>();
        boolean replaced = false;
        for (SourceBytes source : sources) {
            if (!replaced && source.role().equals(role)) {
                updated.add(new SourceBytes(source.role(), source.sourceReference(), value,
                        source.encoding(), source.schemaId(), source.schemaStatus(), source.ordinal()));
                replaced = true;
            } else {
                updated.add(source);
            }
        }
        return copy(phase, updated, evidenceContentSufficient);
    }

    public EditorialP5PilotRequest withoutSource(String role) {
        ArrayList<SourceBytes> updated = new ArrayList<>();
        for (SourceBytes source : sources) if (!source.role().equals(role)) updated.add(source);
        return copy(phase, updated, evidenceContentSufficient);
    }

    public EditorialP5PilotRequest withEvidenceContentSufficient(boolean value) {
        return copy(phase, sources, value);
    }

    public SourceBytes source(String role) {
        for (SourceBytes source : sources) if (source.role().equals(role)) return source;
        return null;
    }

    private EditorialP5PilotRequest copy(String phase, List<SourceBytes> sources, boolean evidenceSufficient) {
        return new EditorialP5PilotRequest(binding, manifest, authority, chapterKey, phase,
                sources, predecessorIdentity, stableAnchors, populationIds,
                evidenceSufficient, requestedOutputTokens, contractRevision, predecessorReport);
    }

    EditorialPhaseContextProjector.Bundle bundleForExecution() {
        EditorialSourceStatusResolver.Result status = pronounStatus();
        ArrayList<EditorialPhaseContextProjector.BundleAsset> assets = new ArrayList<>();
        for (SourceBytes source : sources) {
            boolean authoritative = !EditorialSafe4Contract.PRONOUN.equals(source.role())
                    && !EditorialSafe4Contract.PAIR_CONTEXT.equals(source.role());
            if (EditorialSafe4Contract.PRONOUN.equals(source.role())) {
                authoritative = status.status() == EditorialSourceStatusResolver.PronounStatus.AVAILABLE;
            }
            assets.add(new EditorialPhaseContextProjector.BundleAsset(source.role(),
                    source.sourceReference(), source.bytes(), authoritative, source.schemaId()));
        }
        return new EditorialPhaseContextProjector.Bundle(assets, status);
    }

    private EditorialPhaseContextProjector.Bundle bundle() {
        return bundleForExecution();
    }

    private EditorialSourceStatusResolver.Result pronounStatus() {
        EditorialSourceStatusResolver resolver = new EditorialSourceStatusResolver();
        EditorialSourceStatusResolver.PronounStatus status;
        try {
            status = EditorialSourceStatusResolver.PronounStatus.valueOf(binding.pronounStatus());
        } catch (RuntimeException error) {
            status = EditorialSourceStatusResolver.PronounStatus.NONE;
        }
        String provenance = binding.explicitUserDecisionProvenance();
        if (status == EditorialSourceStatusResolver.PronounStatus.AVAILABLE) {
            ArrayList<EditorialSourceStatusResolver.PronounCandidate> candidates = new ArrayList<>();
            for (SourceBytes source : sources) {
                if (EditorialSafe4Contract.PRONOUN.equals(source.role())) {
                    candidates.add(new EditorialSourceStatusResolver.PronounCandidate(
                            source.sourceReference(), source.bytes(), true, false));
                }
            }
            return resolver.resolve(new EditorialSourceStatusResolver.Selection(
                    status, provenance, candidates));
        }
        if (status == EditorialSourceStatusResolver.PronounStatus.LEGACY_REJECTED) {
            ArrayList<EditorialSourceStatusResolver.PronounCandidate> candidates = new ArrayList<>();
            for (SourceBytes source : sources) {
                if (EditorialSafe4Contract.PRONOUN.equals(source.role())) {
                    candidates.add(new EditorialSourceStatusResolver.PronounCandidate(
                            source.sourceReference(), source.bytes(), false, true));
                }
            }
            return resolver.resolve(new EditorialSourceStatusResolver.Selection(
                    status, provenance, candidates));
        }
        return resolver.resolve(new EditorialSourceStatusResolver.Selection(
                EditorialSourceStatusResolver.PronounStatus.NONE, provenance, List.of()));
    }

    private static String sourceDescriptor(SourceBytes source) {
        return source.role() + "\u0000" + source.ordinal() + "\u0000" + source.sourceReference()
                + "\u0000" + source.actualByteLength() + "\u0000" + source.actualSha256()
                + "\u0000" + source.encoding() + "\u0000" + source.schemaId()
                + "\u0000" + source.schemaStatus();
    }

    private static String text(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }
}
