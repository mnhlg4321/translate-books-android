package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageCanonicalizer;
import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable, selector-bearing RUN_CONTEXT_CLOSED evidence.
 *
 * <p>The declared identity and fingerprint are checked against the canonical
 * projection. The projection includes explicit ROOT/CHILD intent and the
 * exact parent selector, so a retry cannot silently reparent or change node
 * kind.</p>
 */
public final class EditorialRunClosureEvent {
    public static final String IDENTITY_DOMAIN = "EDITORIAL_CLOSURE_EVENT_IDENTITY_V1";
    public static final String FINGERPRINT_DOMAIN = "EDITORIAL_CLOSURE_EVENT_FINGERPRINT_V1";
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    private final String eventIdentity;
    private final String eventFingerprint;
    private final String projectRevisionSelector;
    private final String inputScopeSelector;
    private final String compatibilityEvaluationSelector;
    private final String runKind;
    private final String phaseIdentity;
    private final Long sourceRunRowIdSelector;
    private final EditorialLineageNodeKind nodeKind;
    private final String parentRecordIdentity;
    private final String frozenManifestFingerprint;
    private final String frozenManifestReference;
    private final EditorialClosureEventEligibility eligibility;
    private final String canonicalProjection;

    private EditorialRunClosureEvent(String eventIdentity,
                                     String eventFingerprint,
                                     String projectRevisionSelector,
                                     String inputScopeSelector,
                                     String compatibilityEvaluationSelector,
                                     String runKind,
                                     String phaseIdentity,
                                     Long sourceRunRowIdSelector,
                                     EditorialLineageNodeKind nodeKind,
                                     String parentRecordIdentity,
                                     String frozenManifestFingerprint,
                                     String frozenManifestReference,
                                     EditorialClosureEventEligibility eligibility) {
        this.projectRevisionSelector = required(projectRevisionSelector, "project-revision-selector");
        this.inputScopeSelector = required(inputScopeSelector, "input-scope-selector");
        this.compatibilityEvaluationSelector = required(
                compatibilityEvaluationSelector, "compatibility-evaluation-selector");
        this.runKind = required(runKind, "run-kind");
        this.phaseIdentity = required(phaseIdentity, "phase-identity");
        if (sourceRunRowIdSelector != null && sourceRunRowIdSelector < 0) {
            throw new IllegalArgumentException("source-run-row-selector-invalid");
        }
        this.sourceRunRowIdSelector = sourceRunRowIdSelector;
        this.nodeKind = Objects.requireNonNull(nodeKind, "node-kind");
        if (nodeKind == EditorialLineageNodeKind.ROOT && !blank(parentRecordIdentity)) {
            throw new IllegalArgumentException("root-parent-forbidden");
        }
        if (nodeKind == EditorialLineageNodeKind.CHILD && blank(parentRecordIdentity)) {
            throw new IllegalArgumentException("child-parent-required");
        }
        this.parentRecordIdentity = blank(parentRecordIdentity) ? null : required(parentRecordIdentity, "parent");
        this.frozenManifestFingerprint = checkedHash(frozenManifestFingerprint, "frozen-manifest-fingerprint");
        this.frozenManifestReference = required(frozenManifestReference, "frozen-manifest-reference");
        this.eligibility = Objects.requireNonNull(eligibility, "eligibility");
        this.canonicalProjection = buildCanonicalProjection();
        this.eventIdentity = requiredHash(eventIdentity, "event-identity");
        this.eventFingerprint = requiredHash(eventFingerprint, "event-fingerprint");
    }

    /** Creates a canonical event for injected test-only or future authoritative producers. */
    public static EditorialRunClosureEvent create(
            String projectRevisionSelector,
            String inputScopeSelector,
            String compatibilityEvaluationSelector,
            String runKind,
            String phaseIdentity,
            Long sourceRunRowIdSelector,
            EditorialLineageNodeKind nodeKind,
            String parentRecordIdentity,
            String frozenManifestFingerprint,
            String frozenManifestReference,
            EditorialClosureEventEligibility eligibility) {
        EditorialRunClosureEvent draft = new EditorialRunClosureEvent(
                zeroHash(), zeroHash(), projectRevisionSelector, inputScopeSelector,
                compatibilityEvaluationSelector, runKind, phaseIdentity, sourceRunRowIdSelector,
                nodeKind, parentRecordIdentity, frozenManifestFingerprint, frozenManifestReference,
                eligibility);
        return new EditorialRunClosureEvent(
                draft.computedIdentity(), draft.computedFingerprint(), projectRevisionSelector,
                inputScopeSelector, compatibilityEvaluationSelector, runKind, phaseIdentity,
                sourceRunRowIdSelector, nodeKind, parentRecordIdentity, frozenManifestFingerprint,
                frozenManifestReference, eligibility);
    }

    /** Read-only seam for collision/tamper tests; semantic fields are still structurally validated. */
    public static EditorialRunClosureEvent fromDeclared(
            String declaredEventIdentity,
            String declaredEventFingerprint,
            String projectRevisionSelector,
            String inputScopeSelector,
            String compatibilityEvaluationSelector,
            String runKind,
            String phaseIdentity,
            Long sourceRunRowIdSelector,
            EditorialLineageNodeKind nodeKind,
            String parentRecordIdentity,
            String frozenManifestFingerprint,
            String frozenManifestReference,
            EditorialClosureEventEligibility eligibility) {
        return new EditorialRunClosureEvent(declaredEventIdentity, declaredEventFingerprint,
                projectRevisionSelector, inputScopeSelector, compatibilityEvaluationSelector,
                runKind, phaseIdentity, sourceRunRowIdSelector, nodeKind, parentRecordIdentity,
                frozenManifestFingerprint, frozenManifestReference, eligibility);
    }

    public String eventIdentity() { return eventIdentity; }
    public String eventFingerprint() { return eventFingerprint; }
    public String projectRevisionSelector() { return projectRevisionSelector; }
    public String inputScopeSelector() { return inputScopeSelector; }
    public String compatibilityEvaluationSelector() { return compatibilityEvaluationSelector; }
    public String runKind() { return runKind; }
    public String phaseIdentity() { return phaseIdentity; }
    public Long sourceRunRowIdSelector() { return sourceRunRowIdSelector; }
    public EditorialLineageNodeKind nodeKind() { return nodeKind; }
    public String parentRecordIdentity() { return parentRecordIdentity; }
    public String frozenManifestFingerprint() { return frozenManifestFingerprint; }
    public String frozenManifestReference() { return frozenManifestReference; }
    public EditorialClosureEventEligibility eligibility() { return eligibility; }
    public String canonicalProjection() { return canonicalProjection; }

    public String computedIdentity() { return hash(IDENTITY_DOMAIN, canonicalProjection); }
    public String computedFingerprint() { return hash(FINGERPRINT_DOMAIN, canonicalProjection); }
    public boolean isCanonical() {
        return eventIdentity.equals(computedIdentity()) && eventFingerprint.equals(computedFingerprint());
    }

    private String buildCanonicalProjection() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("closureEventContractVersion", "editorial-run-closure-event-v1");
        projection.put("projectRevisionSelector", projectRevisionSelector);
        projection.put("inputScopeSelector", inputScopeSelector);
        projection.put("compatibilityEvaluationSelector", compatibilityEvaluationSelector);
        projection.put("runKind", runKind);
        projection.put("phaseIdentity", phaseIdentity);
        projection.put("sourceRunRowIdSelector", sourceRunRowIdSelector == null
                ? null : BigDecimal.valueOf(sourceRunRowIdSelector));
        projection.put("nodeKind", nodeKind.name());
        projection.put("parentRecordIdentity", parentRecordIdentity);
        projection.put("frozenManifestFingerprint", frozenManifestFingerprint);
        projection.put("frozenManifestReference", frozenManifestReference);
        projection.put("eligibility", eligibility.name());
        String result = EditorialCanonicalJson.canonicalize(projection);
        EditorialLineageCanonicalizer.strictUtf8(result);
        return result;
    }

    private static String required(String value, String label) {
        if (blank(value)) throw new IllegalArgumentException(label + "-required");
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                throw new IllegalArgumentException(label + "-control-character");
            }
        }
        return value;
    }

    private static String checkedHash(String value, String label) {
        String checked = required(value, label);
        if (!HASH.matcher(checked).matches()) throw new IllegalArgumentException(label + "-invalid");
        return checked;
    }

    private static String requiredHash(String value, String label) {
        return checkedHash(value, label);
    }

    private static String zeroHash() { return "0".repeat(64); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static String hash(String domain, String projection) {
        byte[] domainBytes = EditorialLineageCanonicalizer.strictUtf8(domain + "\n");
        byte[] projectionBytes = EditorialLineageCanonicalizer.strictUtf8(projection);
        byte[] payload = new byte[domainBytes.length + projectionBytes.length];
        System.arraycopy(domainBytes, 0, payload, 0, domainBytes.length);
        System.arraycopy(projectionBytes, 0, payload, domainBytes.length, projectionBytes.length);
        return EditorialCanonicalJson.sha256Hex(payload);
    }
}
