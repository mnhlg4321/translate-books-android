package com.ml.tblandroidtxt.editorial.pack;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Judges a persisted QA receipt against the FINAL bytes it claims to describe. A receipt of the ledger contract
 * may only claim what an operation recorded in the same file backs: a read of FINAL needs a final-read
 * operation on the exact FINAL hash with verified probes, every probe is anchored, and a static
 * {@code finalReadOrder} marker is refused. A receipt from before the ledger contract carries no revision; it
 * is reported as legacy and its marker is not treated as evidence of any read.
 */
public final class EditorialQaReceiptValidator {
    public record Result(boolean valid, boolean legacy, List<String> issues) {
        public Result {
            issues = List.copyOf(issues);
        }
    }

    private EditorialQaReceiptValidator() { }

    public static Result validate(byte[] receiptBytes, byte[] finalBytes) {
        Map<String, Object> receipt;
        try {
            receipt = EditorialCanonicalJson.parseObject(receiptBytes);
        } catch (RuntimeException invalid) {
            return new Result(false, false, List.of("RECEIPT_UNPARSABLE"));
        }
        if (!(receipt.get("contractRevision") instanceof String)) {
            // Legacy receipt: readable, never evidence of a read (its finalReadOrder is a fixed label).
            return new Result(true, true, List.of());
        }
        List<String> issues = new ArrayList<>();
        try {
            check(receipt, finalBytes, issues);
        } catch (RuntimeException malformed) {
            issues.add("RECEIPT_MALFORMED");
        }
        return new Result(issues.isEmpty(), false, issues);
    }

    @SuppressWarnings("unchecked")
    private static void check(Map<String, Object> receipt, byte[] finalBytes, List<String> issues) {
        if (!EditorialContractRevision.isLedger((String) receipt.get("contractRevision"))) issues.add("RECEIPT_REVISION_UNKNOWN");
        if (!EditorialL3Execution.QA_RECEIPT_SCHEMA_V2.equals(receipt.get("schemaVersion"))) issues.add("RECEIPT_SCHEMA_INVALID");
        if (!"QA_RECEIPT".equals(receipt.get("artifactType"))) issues.add("RECEIPT_ARTIFACT_TYPE_INVALID");
        String finalSha = EditorialCanonicalJson.sha256Hex(finalBytes);
        if (!finalSha.equals(receipt.get("finalSha256"))) issues.add("RECEIPT_FINAL_HASH_MISMATCH");
        if (!(receipt.get("finalByteCount") instanceof BigDecimal) || ((BigDecimal) receipt.get("finalByteCount")).intValue() != finalBytes.length) {
            issues.add("RECEIPT_FINAL_LENGTH_MISMATCH");
        }
        if (receipt.containsKey("finalReadOrder")) issues.add("RECEIPT_STATIC_FINAL_READ_MARKER");
        if (!"NONE".equals(receipt.get("stopReceipt"))) issues.add("RECEIPT_STOP_PRESENT");

        Object readValue = receipt.get("finalRead");
        Object opsValue = receipt.get("operations");
        if (!(readValue instanceof Map) || !(opsValue instanceof List)) {
            issues.add("RECEIPT_FINAL_READ_MISSING");
        } else {
            Map<String, Object> read = (Map<String, Object>) readValue;
            if (!EditorialFinalRead.L3_PHASE.equals(read.get("phase"))) issues.add("RECEIPT_FINAL_READ_PHASE_INVALID");
            if (!finalSha.equals(read.get("targetSha256"))) issues.add("RECEIPT_FINAL_READ_TARGET_MISMATCH");
            if (!Boolean.TRUE.equals(read.get("probesVerified"))) issues.add("RECEIPT_FINAL_READ_PROBES_UNVERIFIED");
            if (!"CLEAN".equals(read.get("verdict")) || !((List<Object>) read.get("defects")).isEmpty()) {
                issues.add("RECEIPT_FINAL_READ_NOT_CLEAN");
            }
            List<Object> ops = (List<Object>) opsValue;
            boolean readOp = false;
            int expectedOrder = 1;
            for (Object value : ops) {
                Map<String, Object> op = (Map<String, Object>) value;
                if (((BigDecimal) op.get("order")).intValue() != expectedOrder++) issues.add("RECEIPT_OPERATION_ORDER_INVALID");
                if (EditorialFinalRead.L3_PHASE.equals(op.get("phase")) && finalSha.equals(op.get("targetSha256"))) readOp = true;
            }
            if (!readOp) issues.add("RECEIPT_FINAL_READ_WITHOUT_OPERATION");
            if (ops.size() != 3 || !EditorialL3Execution.REAUDIT_PHASE.equals(((Map<String, Object>) ops.get(0)).get("phase"))
                    || !EditorialL3Execution.RECONCILE_PHASE.equals(((Map<String, Object>) ops.get(1)).get("phase"))) {
                issues.add("RECEIPT_OPERATIONS_INCOMPLETE");
            }
        }

        Object probeValue = receipt.get("probes");
        int coverage = 0;
        int regression = 0;
        if (!(probeValue instanceof List)) {
            issues.add("RECEIPT_PROBES_MISSING");
        } else {
            for (Object value : (List<Object>) probeValue) {
                Map<String, Object> probe = (Map<String, Object>) value;
                if ("COVERAGE".equals(probe.get("kind"))) coverage++; else if ("REGRESSION".equals(probe.get("kind"))) regression++;
                List<Object> units = (List<Object>) probe.get("rawUnits");
                if (units == null || units.isEmpty()) issues.add("RECEIPT_PROBE_UNANCHORED");
                else if (((BigDecimal) probe.get("viStart")).intValue() < 1) issues.add("RECEIPT_PROBE_UNANCHORED");
                for (String key : new String[] {"scope", "contrast", "rawQuote", "viQuote"}) {
                    if (!(probe.get(key) instanceof String) || ((String) probe.get(key)).isBlank()) issues.add("RECEIPT_PROBE_INCOMPLETE");
                }
                boolean none = "NO_DEFECT".equals(probe.get("verdict"));
                if (none != "NONE".equals(probe.get("action")) && !"CONFLICT".equals(probe.get("verdict"))) issues.add("RECEIPT_PROBE_ACTION_INVALID");
            }
            if (coverage < EditorialL3Ledger.MIN_PROBES_PER_KIND || regression < EditorialL3Ledger.MIN_PROBES_PER_KIND) {
                issues.add("RECEIPT_PROBES_TOO_FEW");
            }
        }

        Object releaseValue = receipt.get("releaseNumbers");
        if (!(releaseValue instanceof Map)) {
            issues.add("RECEIPT_RELEASE_NUMBERS_MISSING");
        } else {
            Map<String, Object> release = (Map<String, Object>) releaseValue;
            for (String key : new String[] {"unprocessedRawUnits", "unprocessedCandidates", "provenUnresolvedConflicts",
                    "unaccountedChangedAnchors", "protectedSpanRegressions"}) {
                if (!(release.get(key) instanceof BigDecimal) || ((BigDecimal) release.get(key)).signum() != 0) {
                    issues.add("RECEIPT_RELEASE_NUMBER_NOT_ZERO:" + key);
                }
            }
        }
        Object carriedValue = receipt.get("carriedDefects");
        if (carriedValue instanceof Map) {
            for (Object value : (List<Object>) ((Map<String, Object>) carriedValue).get("defects")) {
                if ("UNRESOLVED".equals(((Map<String, Object>) value).get("status"))) issues.add("RECEIPT_CARRIED_DEFECT_UNRESOLVED");
            }
        } else {
            issues.add("RECEIPT_CARRIED_DEFECTS_MISSING");
        }
    }

    /** Fixed UTF-8 view for tests and diagnostics. */
    static String text(byte[] bytes) { return new String(bytes, StandardCharsets.UTF_8); }
}
