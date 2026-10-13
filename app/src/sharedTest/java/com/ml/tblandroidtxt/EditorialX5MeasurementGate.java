package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.PairText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/** Test-only measurement stop gate; does not change finder, prompt, applier or production guard. */
public final class EditorialX5MeasurementGate {
    private final Map<String, JSONObject> records = new HashMap<>();

    public EditorialX5MeasurementGate(String json) throws Exception {
        JSONArray input = new JSONObject(json).getJSONArray("records");
        for (int i = 0; i < input.length(); i++) {
            JSONObject record = input.getJSONObject(i);
            if (!"006".equals(record.getString("chapter"))) throw new IllegalArgumentException("X5_CHAPTER_MISMATCH");
            String key = record.getString("key");
            if (records.put(key, record) != null) throw new IllegalArgumentException("X5_DUPLICATE_POINT");
        }
        if (records.size() != 74) throw new IllegalArgumentException("X5_POINT_COUNT_MISMATCH");
    }

    /** Returns a text-free stop code after one chunk, before another call is allowed. */
    public String check(int ordinal, String gateJson) throws Exception {
        JSONObject gate = new JSONObject(gateJson);
        JSONArray points = gate.optJSONArray("fixPoints");
        Map<String, JSONObject> observed = new HashMap<>();
        if (points != null) for (int i = 0; i < points.length(); i++) {
            JSONObject point = points.getJSONObject(i);
            String key = ordinal + ":" + point.getInt("id");
            JSONObject record = records.get(key);
            if (record == null || observed.put(key, point) != null) return "X5_POINT_MAPPING_CHANGED";
            if ("CLEAN".equals(record.getString("group"))) {
                String after = PairText.sha256(point.getString("after"));
                boolean accepted = after.equals(record.getJSONObject("current").getString("afterSha256"));
                JSONObject optional = record.optJSONObject("optionalDa");
                if (optional != null) {
                    JSONArray allowed = optional.getJSONArray("allowedAfterSha256");
                    for (int j = 0; j < allowed.length(); j++) accepted |= after.equals(allowed.getString(j));
                }
                if (!accepted) return "X5_CLEAN_REGRESSION:" + key;
            } else if ("S4_WRONG_PASSED".equals(record.getString("layer")) && "FIXED".equals(point.getString("status"))) {
                return "X5_OLD_WRONG_FIX_PASSED:" + key;
            }
        }
        for (Map.Entry<String, JSONObject> e : records.entrySet()) {
            if (e.getKey().startsWith(ordinal + ":") && !"NO_POINT".equals(e.getValue().getJSONObject("current").getString("status"))
                    && !observed.containsKey(e.getKey())) return "X5_POINT_MISSING:" + e.getKey();
        }
        return "";
    }
}
