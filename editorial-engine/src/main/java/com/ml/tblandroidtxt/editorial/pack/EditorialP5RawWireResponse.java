package com.ml.tblandroidtxt.editorial.pack;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Typed model-facing response for {@link EditorialP5RawWireContract}. */
public final class EditorialP5RawWireResponse {
    private final String wireSchemaVersion;
    private final String attemptIdentity;
    private final String requestEnvelopeHash;
    private final List<EditorialLedgerValidator.Entry> findings;
    private final Map<String, String> gateObservations;
    private final List<String> evidenceRefs;
    private final List<String> preservedInventory;
    private final int declaredChangesCount;
    private final EditorialStopDecision.Decision disposition;
    private final boolean modelDeclaredPass;

    public EditorialP5RawWireResponse(String wireSchemaVersion, String attemptIdentity,
                                       String requestEnvelopeHash,
                                       List<EditorialLedgerValidator.Entry> findings,
                                       Map<String, String> gateObservations,
                                       List<String> evidenceRefs,
                                       List<String> preservedInventory,
                                       int declaredChangesCount,
                                       EditorialStopDecision.Decision disposition,
                                       boolean modelDeclaredPass) {
        this.wireSchemaVersion = wireSchemaVersion == null ? "" : wireSchemaVersion;
        this.attemptIdentity = attemptIdentity == null ? "" : attemptIdentity;
        this.requestEnvelopeHash = requestEnvelopeHash == null ? "" : requestEnvelopeHash;
        this.findings = List.copyOf(findings == null ? List.of() : findings);
        this.gateObservations = Map.copyOf(gateObservations == null ? Map.of() : gateObservations);
        this.evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
        this.preservedInventory = List.copyOf(preservedInventory == null ? List.of() : preservedInventory);
        this.declaredChangesCount = declaredChangesCount;
        this.disposition = disposition;
        this.modelDeclaredPass = modelDeclaredPass;
        List<String> issues = EditorialP5RawWireContract.validate(this);
        if (!issues.isEmpty()) throw new IllegalArgumentException(String.join(",", issues));
    }

    public String wireSchemaVersion() { return wireSchemaVersion; }
    public String attemptIdentity() { return attemptIdentity; }
    public String requestEnvelopeHash() { return requestEnvelopeHash; }
    public List<EditorialLedgerValidator.Entry> findings() { return findings; }
    public Map<String, String> gateObservations() { return gateObservations; }
    public List<String> evidenceRefs() { return evidenceRefs; }
    public List<String> preservedInventory() { return preservedInventory; }
    public int declaredChangesCount() { return declaredChangesCount; }
    public EditorialStopDecision.Decision disposition() { return disposition; }
    public boolean modelDeclaredPass() { return modelDeclaredPass; }

    /** Canonical redacted wire representation used only for local size evidence/tests. */
    public byte[] wireBytes() {
        return EditorialCanonicalJson.canonicalize(wireMap()).getBytes(StandardCharsets.UTF_8);
    }

    public Map<String, Object> wireMap() {
        ArrayList<Object> entries = new ArrayList<>();
        for (EditorialLedgerValidator.Entry entry : findings) {
            entries.add(Map.of("itemId", entry.itemId(), "disposition", entry.disposition(),
                    "evidenceRefs", entry.evidenceRefs(), "modelDeclaredPass", entry.modelDeclaredPass()));
        }
        Map<String, Object> dispositionMap = new LinkedHashMap<>();
        dispositionMap.put("disposition", disposition.disposition().name());
        dispositionMap.put("reasonCode", disposition.reasonCode());
        dispositionMap.put("phase", disposition.phase());
        dispositionMap.put("blockingGate", disposition.blockingGate());
        dispositionMap.put("evidenceRefs", disposition.evidenceRefs());
        dispositionMap.put("affectedScope", disposition.affectedScope());
        dispositionMap.put("recoveryAction", disposition.recoveryAction());
        dispositionMap.put("resumeFrom", disposition.resumeFrom());
        dispositionMap.put("stopClass", disposition.stopReceipt() == null
                ? "NONE" : disposition.stopReceipt().stopClass().name());
        dispositionMap.put("retryable", disposition.stopReceipt() != null
                && disposition.stopReceipt().retryable());
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("wireSchemaVersion", wireSchemaVersion);
        root.put("attemptIdentity", attemptIdentity);
        root.put("requestEnvelopeHash", requestEnvelopeHash);
        root.put("findings", entries);
        root.put("gateObservations", new LinkedHashMap<>(gateObservations));
        root.put("evidenceRefs", evidenceRefs);
        root.put("preservedInventory", preservedInventory);
        root.put("declaredChanges", List.of());
        root.put("disposition", dispositionMap);
        root.put("modelDeclaredPass", modelDeclaredPass);
        return root;
    }

    /**
     * Materializes the unchanged exact RAW bytes into the existing final
     * typed artifact. No model identity or source text is used for this step.
     */
    public EditorialP5L1Output materialize(EditorialP5PilotProvider.Request request) {
        if (request == null || request.context() == null) {
            throw new IllegalArgumentException("RAW materialization request context is missing");
        }
        if (!"L1_RAW_DISCOVERY".equals(request.phase())) {
            throw new IllegalArgumentException("RAW materialization requires L1_RAW_DISCOVERY");
        }
        if (!attemptIdentity.equals(request.attemptIdentity())) {
            throw new IllegalArgumentException("RAW wire attempt identity mismatch");
        }
        if (!request.requestEnvelopeHash().equals(requestEnvelopeHash)) {
            throw new IllegalArgumentException("RAW wire request envelope mismatch");
        }
        byte[] rawBytes = request.visibleSources().get(EditorialSafe4Contract.RAW);
        if (rawBytes == null) throw new IllegalArgumentException("RAW source bytes are missing");
        String rawText = strictUtf8(rawBytes);
        if (!Arrays.equals(rawBytes, rawText.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("RAW source bytes are not stable UTF-8");
        }
        EditorialLedgerValidator.Result ledgerValidation = new EditorialLedgerValidator().validate(
                new EditorialLedgerValidator.Request(request.context().populationIds(), findings));
        if (!ledgerValidation.valid()) {
            throw new IllegalArgumentException("RAW_WIRE_LEDGER_INVALID:" + ledgerValidation.issues());
        }
        return new EditorialP5L1Output(
                EditorialP5RawWireContract.FINAL_REPORT_SCHEMA,
                EditorialSafe4Contract.RECEIPT_SCHEMA_VERSION,
                request.context().bindingIdentity(), request.context().manifestFingerprint(),
                request.chapterKey(), EditorialP5RawWireContract.FINAL_PHASE,
                request.context().bundleIdentity(), request.context().predecessorIdentity(),
                request.context().stableAnchors(),
                new EditorialLedgerValidator.Request(request.context().populationIds(), findings),
                gateObservations, preservedInventory, List.of(), rawText, rawText, 0,
                disposition, Set.copyOf(evidenceRefs), modelDeclaredPass);
    }

    private static String strictUtf8(byte[] bytes) {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return decoded.toString();
        } catch (CharacterCodingException error) {
            throw new IllegalArgumentException("RAW source is not strict UTF-8", error);
        }
    }
}
