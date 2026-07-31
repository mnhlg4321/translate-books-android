package com.ml.tblandroidtxt;

/** Versioned JSON contracts returned by editorial runners before text export is rendered. */
public final class EditorialEvidenceSchema {
    public static final String VERSION = "editorial-v5-evidence-1";

    public static String l1ReportSchema() {
        return "{\"schema\":\"" + VERSION + "\",\"run\":\"L1\",\"chapterId\":\"string\","
                + "\"scenes\":[{\"id\":\"string\",\"rawStart\":\"string\",\"rawEnd\":\"string\","
                + "\"coverage\":\"ALIGNED|MISSING|EXTRA|MISALIGNED\",\"status\":\"OPEN|CLOSED\"}],"
                + "\"issues\":[{\"id\":\"string\",\"severity\":\"CRITICAL|MAJOR|MINOR\",\"category\":\"COVERAGE|FIDELITY|REFERENCE|VOICE|LOGIC|ACTION|STRUCTURE|CONTINUITY\","
                + "\"rawAnchor\":\"string\",\"draftAnchor\":\"string\",\"currentMeaning\":\"string\","
                + "\"rawEvidence\":\"string\",\"deviationImpact\":\"string\",\"l2Scope\":\"string\"}],"
                + "\"gates\":{\"coverage\":\"OPEN|CLOSED\",\"fidelity\":\"OPEN|CLOSED\","
                + "\"referenceVoice\":\"OPEN|CLOSED\",\"continuityStructure\":\"OPEN|CLOSED\"}}";
    }

    public static String l2OutputSchema() {
        return "{\"schema\":\"" + VERSION + "\",\"run\":\"L2\",\"chapterId\":\"string\","
                + "\"output\":\"full VI_L2 text\",\"outputComplete\":true,\"globalChanges\":[{\"id\":\"string\",\"before\":\"string\","
                + "\"after\":\"string\",\"reason\":\"string\",\"scope\":\"string\",\"status\":\"OPEN|CLOSED\"}],"
                + "\"gates\":{\"coverage\":\"OPEN|CLOSED\",\"fidelity\":\"OPEN|CLOSED\","
                + "\"referenceVoice\":\"OPEN|CLOSED\",\"continuityStructure\":\"OPEN|CLOSED\",\"change\":\"OPEN|CLOSED\"}}";
    }

    public static String l3OutputSchema() {
        return "{\"schema\":\"" + VERSION + "\",\"run\":\"L3\",\"chapterId\":\"string\","
                + "\"output\":\"full FINAL_QA text\",\"outputComplete\":true,\"changeSet\":[{\"id\":\"string\",\"position\":\"string\","
                + "\"before\":\"string\",\"after\":\"string\",\"reason\":\"string\",\"scope\":\"string\",\"status\":\"OPEN|CLOSED\"}],"
                + "\"crossSceneVoiceAudit\":\"OPEN|CLOSED\",\"finalReadThrough\":\"OPEN|CLOSED\","
                + "\"gates\":{\"coverage\":\"OPEN|CLOSED\",\"fidelity\":\"OPEN|CLOSED\","
                + "\"referenceVoice\":\"OPEN|CLOSED\",\"continuityStructure\":\"OPEN|CLOSED\",\"change\":\"OPEN|CLOSED\"}}";
    }

    private EditorialEvidenceSchema() {}
}
