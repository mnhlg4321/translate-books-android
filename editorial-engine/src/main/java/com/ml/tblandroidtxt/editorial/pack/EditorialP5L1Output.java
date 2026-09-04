package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Typed model result accepted by P5 only after local validation. */
public final class EditorialP5L1Output {
    private final String reportSchemaVersion;
    private final String receiptSchemaVersion;
    private final String bindingIdentity;
    private final String manifestFingerprint;
    private final String chapterKey;
    private final String phase;
    private final String bundleIdentity;
    private final String predecessorIdentity;
    private final List<String> stableAnchors;
    private final EditorialLedgerValidator.Request ledger;
    private final Map<String, String> gates;
    private final List<String> preservedInventory;
    private final List<EditorialDiffValidator.DeclaredChange> declaredChanges;
    private final String beforeText;
    private final String afterText;
    private final int releaseAttemptCount;
    private final EditorialStopDecision.Decision disposition;
    private final Set<String> evidenceRefs;
    private final boolean modelDeclaredPass;

    public EditorialP5L1Output(
            String reportSchemaVersion,
            String receiptSchemaVersion,
            String bindingIdentity,
            String manifestFingerprint,
            String chapterKey,
            String phase,
            String bundleIdentity,
            String predecessorIdentity,
            List<String> stableAnchors,
            EditorialLedgerValidator.Request ledger,
            Map<String, String> gates,
            List<String> preservedInventory,
            List<EditorialDiffValidator.DeclaredChange> declaredChanges,
            String beforeText,
            String afterText,
            int releaseAttemptCount,
            EditorialStopDecision.Decision disposition,
            Set<String> evidenceRefs,
            boolean modelDeclaredPass) {
        this.reportSchemaVersion = text(reportSchemaVersion, "report schema version");
        this.receiptSchemaVersion = text(receiptSchemaVersion, "receipt schema version");
        this.bindingIdentity = text(bindingIdentity, "binding identity");
        this.manifestFingerprint = text(manifestFingerprint, "manifest fingerprint");
        this.chapterKey = text(chapterKey, "chapter key");
        this.phase = text(phase, "phase");
        this.bundleIdentity = text(bundleIdentity, "bundle identity");
        this.predecessorIdentity = text(predecessorIdentity, "predecessor identity");
        this.stableAnchors = List.copyOf(stableAnchors == null ? List.of() : stableAnchors);
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.gates = Map.copyOf(gates == null ? Map.of() : gates);
        this.preservedInventory = List.copyOf(preservedInventory == null ? List.of() : preservedInventory);
        this.declaredChanges = List.copyOf(declaredChanges == null ? List.of() : declaredChanges);
        this.beforeText = beforeText;
        this.afterText = afterText;
        if (releaseAttemptCount < 0) throw new IllegalArgumentException("release attempt cannot be negative");
        this.releaseAttemptCount = releaseAttemptCount;
        this.disposition = Objects.requireNonNull(disposition, "disposition");
        this.evidenceRefs = Set.copyOf(evidenceRefs == null ? Set.of() : evidenceRefs);
        this.modelDeclaredPass = modelDeclaredPass;
    }

    public String reportSchemaVersion() { return reportSchemaVersion; }
    public String receiptSchemaVersion() { return receiptSchemaVersion; }
    public String bindingIdentity() { return bindingIdentity; }
    public String manifestFingerprint() { return manifestFingerprint; }
    public String chapterKey() { return chapterKey; }
    public String phase() { return phase; }
    public String bundleIdentity() { return bundleIdentity; }
    public String predecessorIdentity() { return predecessorIdentity; }
    public List<String> stableAnchors() { return stableAnchors; }
    public EditorialLedgerValidator.Request ledger() { return ledger; }
    public Map<String, String> gates() { return gates; }
    public List<String> preservedInventory() { return preservedInventory; }
    public List<EditorialDiffValidator.DeclaredChange> declaredChanges() { return declaredChanges; }
    public String beforeText() { return beforeText; }
    public String afterText() { return afterText; }
    public int releaseAttemptCount() { return releaseAttemptCount; }
    public EditorialStopDecision.Decision disposition() { return disposition; }
    public Set<String> evidenceRefs() { return evidenceRefs; }
    public boolean modelDeclaredPass() { return modelDeclaredPass; }

    /** Semantic fingerprint excludes transport/schema-validity flags and model self-certification. */
    public String semanticFingerprint() {
        return EditorialCanonicalJson.sha256Hex(EditorialCanonicalJson.canonicalize(semanticMap())
                .getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> semanticMap() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("bindingIdentity", bindingIdentity);
        root.put("manifestFingerprint", manifestFingerprint);
        root.put("chapterKey", chapterKey);
        root.put("phase", phase);
        root.put("bundleIdentity", bundleIdentity);
        root.put("predecessorIdentity", predecessorIdentity);
        root.put("stableAnchors", stableAnchors);
        root.put("ledger", ledgerMap(ledger));
        root.put("gates", new TreeMap<>(gates));
        root.put("preservedInventory", preservedInventory);
        List<Object> changes = new ArrayList<>();
        for (EditorialDiffValidator.DeclaredChange change : declaredChanges) {
            changes.add(Map.of("lineNumber", BigDecimal.valueOf(change.lineNumber()), "beforeHash", change.beforeHash(),
                    "afterHash", change.afterHash(), "errorId", change.errorId()));
        }
        root.put("declaredChanges", changes);
        root.put("beforeText", beforeText);
        root.put("afterText", afterText);
        root.put("releaseAttemptCount", BigDecimal.valueOf(releaseAttemptCount));
        root.put("disposition", decisionMap(disposition));
        root.put("evidenceRefs", evidenceRefs.stream().sorted().toList());
        return root;
    }

    private static Map<String, Object> ledgerMap(EditorialLedgerValidator.Request value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("populationIds", value.populationIds());
        List<Object> entries = new ArrayList<>();
        for (EditorialLedgerValidator.Entry entry : value.entries()) {
            entries.add(Map.of("itemId", entry.itemId(), "disposition", entry.disposition(),
                    "evidenceRefs", entry.evidenceRefs(), "modelDeclaredPass", entry.modelDeclaredPass()));
        }
        result.put("entries", entries);
        return result;
    }

    private static Map<String, Object> decisionMap(EditorialStopDecision.Decision value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("disposition", value.disposition().name());
        result.put("reasonCode", value.reasonCode());
        result.put("phase", value.phase());
        result.put("blockingGate", value.blockingGate());
        result.put("evidenceRefs", value.evidenceRefs());
        result.put("affectedScope", value.affectedScope());
        result.put("recoveryAction", value.recoveryAction());
        result.put("resumeFrom", value.resumeFrom());
        if (value.stopReceipt() != null) result.put("retryable", value.stopReceipt().retryable());
        return result;
    }

    private static String text(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }
}
