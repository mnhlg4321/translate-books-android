package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;

/** Structural validator only; linguistic fidelity remains evidence reviewed by the workflow. */
public final class EditorialEvidenceValidator {
    public static String validateL1Report(String rawJson) {
        try {
            JSONObject root = new JSONObject(rawJson == null ? "" : rawJson);
            if (!EditorialEvidenceSchema.VERSION.equals(root.optString("schema"))) return "Unsupported evidence schema";
            if (!"L1".equals(root.optString("run"))) return "Evidence run must be L1";
            if (blank(root.optString("chapterId"))) return "Missing chapterId";
            JSONArray scenes = root.optJSONArray("scenes");
            if (scenes == null || scenes.length() == 0) return "L1 report requires at least one scene";
            for (int i = 0; i < scenes.length(); i++) {
                JSONObject scene = scenes.optJSONObject(i);
                if (scene == null || blank(scene.optString("id")) || blank(scene.optString("rawStart"))
                        || blank(scene.optString("rawEnd"))) return "Scene " + (i + 1) + " is missing raw anchors";
                if (!oneOf(scene.optString("coverage"), "ALIGNED", "MISSING", "EXTRA", "MISALIGNED")) return "Scene " + (i + 1) + " has invalid coverage";
                if (!"CLOSED".equals(scene.optString("status"))) return "Scene " + (i + 1) + " is not closed";
            }
            JSONArray issues = root.optJSONArray("issues");
            if (issues == null) return "Missing issues ledger";
            for (int i = 0; i < issues.length(); i++) {
                JSONObject issue = issues.optJSONObject(i);
                if (issue == null || blank(issue.optString("id")) || !oneOf(issue.optString("severity"), "CRITICAL", "MAJOR", "MINOR")
                        || blank(issue.optString("rawAnchor")) || blank(issue.optString("draftAnchor"))
                        || blank(issue.optString("currentMeaning")) || blank(issue.optString("rawEvidence"))
                        || blank(issue.optString("deviationImpact")) || blank(issue.optString("l2Scope"))) {
                    return "Issue " + (i + 1) + " is missing required L1 evidence";
                }
            }
            JSONObject gates = root.optJSONObject("gates");
            if (gates == null) return "Missing L1 gates";
            String[] names = {"coverage", "fidelity", "referenceVoice", "continuityStructure"};
            for (String name : names) if (!"CLOSED".equals(gates.optString(name))) return "L1 gate is not closed: " + name;
            return null;
        } catch (Exception e) {
            return "Invalid L1 JSON";
        }
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static boolean oneOf(String value, String... allowed) { for (String item : allowed) if (item.equals(value)) return true; return false; }
    private EditorialEvidenceValidator() {}
}
