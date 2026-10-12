package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Resolves explicit Glossary/Pronoun source status without inferring NONE. */
public final class EditorialSourceStatusResolver {
    public enum PronounStatus { AVAILABLE, NONE, LEGACY_REJECTED }

    public enum Decision { ACCEPTED, AMBIGUOUS, QUARANTINED, INVALID }

    public static final class PronounCandidate {
        private final String sourceId;
        private final byte[] bytes;
        private final boolean authoritative;
        private final boolean legacy;

        public PronounCandidate(String sourceId, byte[] bytes, boolean authoritative, boolean legacy) {
            this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
            this.bytes = bytes == null ? null : bytes.clone();
            this.authoritative = authoritative;
            this.legacy = legacy;
        }

        public String sourceId() { return sourceId; }
        public byte[] bytes() { return bytes == null ? null : bytes.clone(); }
        public boolean authoritative() { return authoritative; }
        public boolean legacy() { return legacy; }
    }

    public record Selection(PronounStatus status, String provenance, List<PronounCandidate> candidates) {
        public Selection {
            if (status == null) throw new NullPointerException("status");
            candidates = List.copyOf(candidates == null ? List.of() : candidates);
        }

        public static Selection available(String provenance, PronounCandidate candidate) {
            return new Selection(PronounStatus.AVAILABLE, provenance, List.of(candidate));
        }

        public static Selection none(String provenance) {
            return new Selection(PronounStatus.NONE, provenance, List.of());
        }

        public static Selection legacyRejected(String provenance, List<PronounCandidate> candidates) {
            return new Selection(PronounStatus.LEGACY_REJECTED, provenance, candidates);
        }
    }

    public record Result(Decision decision, PronounStatus status, String provenance,
                         List<PronounCandidate> authoritativeCandidates,
                         List<PronounCandidate> quarantinedCandidates, String reasonCode) {
        public Result {
            Objects.requireNonNull(decision, "decision");
            Objects.requireNonNull(status, "status");
            provenance = provenance == null ? "" : provenance;
            authoritativeCandidates = List.copyOf(authoritativeCandidates == null ? List.of() : authoritativeCandidates);
            quarantinedCandidates = List.copyOf(quarantinedCandidates == null ? List.of() : quarantinedCandidates);
            Objects.requireNonNull(reasonCode, "reasonCode");
        }

        public boolean accepted() {
            return decision == Decision.ACCEPTED || decision == Decision.QUARANTINED;
        }

        public boolean hasExactlyOneAuthoritativeSource() {
            return authoritativeCandidates.size() == 1;
        }
    }

    public Result resolve(Selection selection) {
        if (selection == null || selection.status() == null || selection.provenance() == null
                || selection.provenance().isBlank()) {
            return invalid(PronounStatus.NONE, "PRONOUN_STATUS_EXPLICIT_REQUIRED");
        }
        List<PronounCandidate> authoritative = new ArrayList<>();
        List<PronounCandidate> quarantined = new ArrayList<>();
        for (PronounCandidate candidate : selection.candidates()) {
            if (candidate == null || candidate.bytes() == null || !candidate.authoritative() || candidate.legacy()) {
                if (candidate != null) quarantined.add(candidate);
            } else {
                authoritative.add(candidate);
            }
        }

        return switch (selection.status()) {
            case AVAILABLE -> {
                if (authoritative.size() > 1) {
                    yield new Result(Decision.AMBIGUOUS, PronounStatus.AVAILABLE, selection.provenance(),
                            authoritative, quarantined, "PRONOUN_MULTIPLE_AUTHORITATIVE_SOURCES");
                }
                if (authoritative.isEmpty()) {
                    yield new Result(Decision.INVALID, PronounStatus.AVAILABLE, selection.provenance(),
                            List.of(), quarantined, "PRONOUN_AVAILABLE_SOURCE_MISSING");
                }
                yield new Result(quarantined.isEmpty() ? Decision.ACCEPTED : Decision.QUARANTINED,
                        PronounStatus.AVAILABLE, selection.provenance(), authoritative, quarantined,
                        quarantined.isEmpty() ? "PRONOUN_AVAILABLE_ACCEPTED" : "PRONOUN_NON_AUTHORITATIVE_QUARANTINED");
            }
            case NONE -> {
                if (!authoritative.isEmpty()) {
                    yield new Result(Decision.INVALID, PronounStatus.NONE, selection.provenance(),
                            authoritative, quarantined, "PRONOUN_NONE_HAS_AUTHORITATIVE_SOURCE");
                }
                yield new Result(quarantined.isEmpty() ? Decision.ACCEPTED : Decision.QUARANTINED,
                        PronounStatus.NONE, selection.provenance(), List.of(), quarantined,
                        quarantined.isEmpty() ? "PRONOUN_NONE_EXPLICIT" : "PRONOUN_NONE_QUARANTINED");
            }
            case LEGACY_REJECTED -> new Result(Decision.QUARANTINED, PronounStatus.LEGACY_REJECTED,
                    selection.provenance(), List.of(), selection.candidates(), "PRONOUN_LEGACY_REJECTED_QUARANTINED");
        };
    }

    private static Result invalid(PronounStatus status, String reason) {
        return new Result(Decision.INVALID, status, "", List.of(), List.of(), reason);
    }
}
