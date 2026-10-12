package com.ml.tblandroidtxt;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Immutable identity and capability gate for the bundled V5-SAFE.4 workflow pack. */
public final class EditorialSafe4Pack {
    public static final String VERSION = "V5-SAFE.4";
    public static final String EVIDENCE_SCHEMA = "editorial-v5-safe4-evidence-1";
    public static final String ASSET_ROOT = "editorial/v5-safe4/";

    public static final String PROJECT_FILE = "PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4.txt";
    public static final String PROMPT_FILE = "PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4.txt";
    public static final String WORKFLOW_FILE = "WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4.txt";

    public static final String PROJECT_SHA256 = "C57100C45F16FC5A27E56AE17ABE919BE89DA55082A66D060DB504746ED8B717";
    public static final String PROMPT_SHA256 = "0B4C02573F46A91528A63262D3E52C655A5C7E31F2ABBBFB01759D38E94F8E81";
    public static final String WORKFLOW_SHA256 = "7A434ADE77239DB33AF5A677456D0310AC11DC8391565FC4888236536FF96B5E";

    public enum Capability {
        PACK_INTEGRITY,
        EXACT_MANIFEST_LINEAGE,
        EXHAUSTIVE_LEDGER_SCHEMA,
        DERIVED_EVIDENCE_GATES,
        PRONOUN_STATUS_AND_PAIR_CONTEXT,
        L1_RAW_FIRST_BARRIER,
        L2_CHANGE_COVERAGE,
        L3_TWO_ADVERSARIAL_PASSES,
        SAFE4_RELEASE_ARTIFACTS,
        GOLDEN_REPLAY_G1_TO_G10
    }

    private static final EnumSet<Capability> REQUIRED = EnumSet.allOf(Capability.class);
    private static final EnumSet<Capability> IMPLEMENTED = EnumSet.of(Capability.PACK_INTEGRITY);

    /** Stable pack identity; individual source hashes remain available for byte-level verification. */
    public static final String PACK_HASH = HashUtil.sha256(
            VERSION + "\n" + PROJECT_FILE + ":" + PROJECT_SHA256 + "\n"
                    + PROMPT_FILE + ":" + PROMPT_SHA256 + "\n"
                    + WORKFLOW_FILE + ":" + WORKFLOW_SHA256 + "\n"
                    + "schema:" + EVIDENCE_SCHEMA);

    public static Set<Capability> requiredCapabilities() {
        return Collections.unmodifiableSet(EnumSet.copyOf(REQUIRED));
    }

    public static Set<Capability> implementedCapabilities() {
        return Collections.unmodifiableSet(EnumSet.copyOf(IMPLEMENTED));
    }

    public static Set<Capability> missingCapabilities() {
        EnumSet<Capability> missing = EnumSet.copyOf(REQUIRED);
        missing.removeAll(IMPLEMENTED);
        return Collections.unmodifiableSet(missing);
    }

    /** Fail-closed until every SAFE.4 capability and Golden Replay has concrete evidence. */
    public static boolean executionEnabled() {
        return IMPLEMENTED.containsAll(REQUIRED);
    }

    public static String blockedReason() {
        return executionEnabled() ? "" : "SAFE 4 engine is locked until schema, derived gates, lineage and Golden Replay G1-G10 are complete";
    }

    private EditorialSafe4Pack() { }
}
