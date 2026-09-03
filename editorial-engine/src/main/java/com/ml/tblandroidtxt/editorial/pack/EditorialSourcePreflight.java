package com.ml.tblandroidtxt.editorial.pack;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic, provider-free source preflight for the SAFE4 full contract. */
public final class EditorialSourcePreflight {
    public enum Mode { NORMAL_FOUR_SOURCE, ALTERNATE_EXPLICIT }

    public enum Stage {
        EXACT_INVENTORY,
        BYTE_ACCESS,
        FORMAT_ENCODING,
        SOURCE_SCHEMA,
        BYTE_IDENTITY
    }

    public enum Outcome { PASS, PRESERVE_DRAFT, INPUT_REQUIRED, RETRY_REQUIRED }

    /** A handle may exist while its bytes are unavailable; that is a retry, not absence. */
    public static final class SourceInput {
        private final String role;
        private final String sourceId;
        private final byte[] bytes;
        private final boolean handlePresent;
        private final long declaredByteLength;
        private final String declaredSha256;
        private final String schemaId;

        public SourceInput(String role, String sourceId, byte[] bytes, boolean handlePresent,
                           long declaredByteLength, String declaredSha256, String schemaId) {
            this.role = Objects.requireNonNull(role, "role");
            this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
            this.bytes = bytes == null ? null : bytes.clone();
            this.handlePresent = handlePresent;
            this.declaredByteLength = declaredByteLength;
            this.declaredSha256 = declaredSha256;
            this.schemaId = schemaId;
        }

        public static SourceInput readable(String role, String sourceId, byte[] bytes, String schemaId) {
            if (bytes == null) throw new IllegalArgumentException("Readable source bytes are required");
            return new SourceInput(role, sourceId, bytes, true, bytes.length,
                    EditorialCanonicalJson.sha256Hex(bytes), schemaId);
        }

        public static SourceInput unreadable(String role, String sourceId, String schemaId) {
            return new SourceInput(role, sourceId, null, true, -1L, null, schemaId);
        }

        public String role() { return role; }
        public String sourceId() { return sourceId; }
        public byte[] bytes() { return bytes == null ? null : bytes.clone(); }
        public boolean handlePresent() { return handlePresent; }
        public long declaredByteLength() { return declaredByteLength; }
        public String declaredSha256() { return declaredSha256; }
        public String schemaId() { return schemaId; }
    }

    public record Request(Mode mode, boolean explicitAlternateSelection,
                          Map<String, SourceInput> inputs, boolean evidenceContentSufficient) {
        public Request {
            Objects.requireNonNull(mode, "mode");
            if (inputs == null) throw new NullPointerException("inputs");
            LinkedHashMap<String, SourceInput> copy = new LinkedHashMap<>();
            for (Map.Entry<String, SourceInput> entry : inputs.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) throw new NullPointerException("input entry");
                copy.put(entry.getKey(), entry.getValue());
            }
            inputs = Collections.unmodifiableMap(copy);
        }

        public static Request normal(Map<String, SourceInput> inputs) {
            return new Request(Mode.NORMAL_FOUR_SOURCE, false, inputs, true);
        }

        public static Request normal(Map<String, SourceInput> inputs, boolean evidenceContentSufficient) {
            return new Request(Mode.NORMAL_FOUR_SOURCE, false, inputs, evidenceContentSufficient);
        }

        public static Request alternateExplicit(Map<String, SourceInput> inputs) {
            return new Request(Mode.ALTERNATE_EXPLICIT, true, inputs, true);
        }
    }

    public record Result(Outcome outcome, String reasonCode, Stage failedStage,
                         List<String> checkedRoles, Map<String, String> sourceFingerprints,
                         boolean downstreamNotEvaluated, int semanticRawReads, int providerCalls) {
        public Result {
            Objects.requireNonNull(outcome, "outcome");
            Objects.requireNonNull(reasonCode, "reasonCode");
            checkedRoles = List.copyOf(checkedRoles == null ? List.of() : checkedRoles);
            sourceFingerprints = Collections.unmodifiableMap(new LinkedHashMap<>(
                    sourceFingerprints == null ? Map.of() : sourceFingerprints));
            if (semanticRawReads != 0 || providerCalls != 0) {
                throw new IllegalArgumentException("Preflight cannot read semantic RAW or call a provider");
            }
        }

        public boolean passed() { return outcome == Outcome.PASS || outcome == Outcome.PRESERVE_DRAFT; }
    }

    public Result evaluate(Request request) {
        if (request == null) return failure(Outcome.INPUT_REQUIRED, "INPUT_MODE_NOT_SELECTED", Stage.EXACT_INVENTORY, List.of());
        List<String> checked = new ArrayList<>();
        if (request.mode() == Mode.ALTERNATE_EXPLICIT && !request.explicitAlternateSelection()) {
            return failure(Outcome.INPUT_REQUIRED, "INPUT_ALTERNATE_SELECTION_REQUIRED", Stage.EXACT_INVENTORY, checked);
        }

        // Exact inventory is deliberately role-based and independent of filename/version.
        List<String> required = request.mode() == Mode.NORMAL_FOUR_SOURCE
                ? EditorialSafe4Contract.REQUIRED_SOURCE_ROLES
                : List.of(EditorialSafe4Contract.RAW, EditorialSafe4Contract.DRAFT,
                EditorialSafe4Contract.GLOSSARY);
        for (String role : required) {
            checked.add(role);
            if (!request.inputs().containsKey(role)) {
                String reason = switch (role) {
                    case EditorialSafe4Contract.RAW -> "INPUT_RAW_FILE_MISSING";
                    case EditorialSafe4Contract.DRAFT -> "INPUT_DRAFT_FILE_MISSING";
                    case EditorialSafe4Contract.GLOSSARY -> "INPUT_GLOSSARY_REQUIRED";
                    case EditorialSafe4Contract.PRONOUN -> "INPUT_PRONOUN_FILE_MISSING";
                    default -> "INPUT_SOURCE_ROLE_MISSING";
                };
                return failure(Outcome.INPUT_REQUIRED, reason, Stage.EXACT_INVENTORY, checked);
            }
        }

        for (String role : required) {
            SourceInput input = request.inputs().get(role);
            if (!input.handlePresent() || input.bytes() == null) {
                return failure(Outcome.RETRY_REQUIRED, "RETRY_SOURCE_BYTES_UNAVAILABLE", Stage.BYTE_ACCESS, checked);
            }
        }

        LinkedHashMap<String, String> fingerprints = new LinkedHashMap<>();
        for (String role : required) {
            SourceInput input = request.inputs().get(role);
            byte[] bytes = input.bytes();
            if (!strictUtf8WithoutBom(bytes)) {
                return failure(Outcome.INPUT_REQUIRED, "INPUT_SOURCE_ENCODING_INVALID", Stage.FORMAT_ENCODING, checked);
            }
            if (!validSchema(input)) {
                String reason = EditorialSafe4Contract.GLOSSARY.equals(role)
                        ? "INPUT_GLOSSARY_SCHEMA_INVALID"
                        : EditorialSafe4Contract.PRONOUN.equals(role)
                        ? "INPUT_PRONOUN_SCHEMA_INVALID"
                        : "INPUT_SOURCE_SCHEMA_INVALID";
                return failure(Outcome.INPUT_REQUIRED, reason, Stage.SOURCE_SCHEMA, checked);
            }
            String actualHash = EditorialCanonicalJson.sha256Hex(bytes);
            if (input.declaredByteLength() >= 0 && input.declaredByteLength() != bytes.length
                    || input.declaredSha256() != null && !input.declaredSha256().equalsIgnoreCase(actualHash)) {
                return failure(Outcome.INPUT_REQUIRED, "INPUT_SOURCE_BYTE_IDENTITY_MISMATCH", Stage.BYTE_IDENTITY, checked);
            }
            fingerprints.put(role, actualHash);
        }

        // Pair Context is optional and never enters the required source loop.
        Outcome outcome = request.evidenceContentSufficient() ? Outcome.PASS : Outcome.PRESERVE_DRAFT;
        String reason = request.evidenceContentSufficient() ? "PREFLIGHT_PASS" : "PRESERVE_DRAFT_EVIDENCE_INSUFFICIENT";
        return new Result(outcome, reason, null, checked, fingerprints, false, 0, 0);
    }

    private static boolean validSchema(SourceInput input) {
        if (input.schemaId() == null || input.schemaId().isBlank()) return false;
        return switch (input.role()) {
            case EditorialSafe4Contract.GLOSSARY -> "safe4.full.glossary.v1".equals(input.schemaId());
            case EditorialSafe4Contract.PRONOUN -> "safe4.full.pronoun.v1".equals(input.schemaId());
            default -> true;
        };
    }

    private static boolean strictUtf8WithoutBom(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xef
                && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf) return false;
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private static Result failure(Outcome outcome, String reason, Stage stage, List<String> checked) {
        return new Result(outcome, reason, stage, checked, Map.of(), true, 0, 0);
    }
}
