package com.ml.tblandroidtxt.editorial.pack;

import java.util.Map;
import java.util.Set;

/**
 * Revision of the L1 ledger contract. A report written before the ledger existed carries no revision and is
 * {@link #LEGACY_V1}: it stays readable and keeps its original identities, but it is never an eligible
 * predecessor for a ledger-contract chain. Every attempt identity of a ledger chain binds the revision, so a
 * changed wire or report schema can never be answered with an older COMMITTED artifact.
 */
public final class EditorialContractRevision {
    public static final String LEGACY_V1 = "LEGACY_CONTRACT_V1";
    public static final String L1_LEDGER_V2 = "L1_LEDGER_V2";
    public static final String L1_LEDGER_V3 = "L1_LEDGER_V3";
    public static final String L1_LEDGER_V4 = "L1_LEDGER_V4";
    public static final String L1_LEDGER_V5 = "L1_LEDGER_V5";
    public static final String L1_LEDGER_V6 = "L1_LEDGER_V6";
    public static final String CURRENT_LEDGER = L1_LEDGER_V6;
    public static final String REPORT_SCHEMA_V2 = "safe4.full.report-l1.v2";
    private static final Set<String> KNOWN = Set.of(LEGACY_V1, L1_LEDGER_V2, L1_LEDGER_V3, L1_LEDGER_V4, L1_LEDGER_V5, L1_LEDGER_V6);

    private EditorialContractRevision() { }

    public static boolean known(String revision) { return revision != null && KNOWN.contains(revision); }

    public static boolean isLedger(String revision) {
        return L1_LEDGER_V2.equals(revision) || L1_LEDGER_V3.equals(revision) || L1_LEDGER_V4.equals(revision)
                || L1_LEDGER_V5.equals(revision) || L1_LEDGER_V6.equals(revision);
    }

    /** Revision recorded in a parsed REPORT_L1; a missing or unknown value means the legacy contract. */
    public static String ofReport(Map<String, Object> report) {
        if (report == null) return LEGACY_V1;
        Object value = report.get("contractRevision");
        if (value instanceof String && known((String) value)) return (String) value;
        return LEGACY_V1;
    }

    /** Revision of persisted report bytes; unparsable bytes are treated as legacy and never as eligible. */
    public static String ofReportBytes(byte[] reportBytes) {
        if (reportBytes == null) return LEGACY_V1;
        try {
            return ofReport(EditorialCanonicalJson.parseObject(reportBytes));
        } catch (RuntimeException invalid) {
            return LEGACY_V1;
        }
    }

    /** A predecessor is eligible only when its revision equals the revision the chain runs under. */
    public static boolean eligiblePredecessor(String reportRevision, String chainRevision) {
        return known(chainRevision) && chainRevision.equals(reportRevision);
    }

    /**
     * Text added to an attempt-identity input. Empty for the legacy contract so that every identity computed
     * before this revision existed is reproduced bit for bit.
     */
    public static String identitySuffix(String revision) {
        return isLedger(revision) ? "\ncontractRevision=" + revision : "";
    }
}
