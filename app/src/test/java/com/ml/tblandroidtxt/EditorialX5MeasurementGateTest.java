package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.PairText;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class EditorialX5MeasurementGateTest {
    private JSONObject suite() throws Exception {
        JSONArray records = new JSONArray();
        for (int i = 1; i <= 74; i++) records.put(new JSONObject().put("chapter", "006").put("key", i + ":1")
                .put("group", "CLEAN").put("layer", "CLEAN_KEPT")
                .put("current", new JSONObject().put("status", "KEPT").put("afterSha256", PairText.sha256("Cau"))));
        return new JSONObject().put("records", records);
    }
    private String gate(String after, String status) throws Exception {
        return new JSONObject().put("fixPoints", new JSONArray().put(new JSONObject().put("id", 1)
                .put("after", after).put("status", status))).toString();
    }
    @Test public void unchangedCleanPassesAndOtherTextStops() throws Exception {
        EditorialX5MeasurementGate g = new EditorialX5MeasurementGate(suite().toString());
        assertEquals("", g.check(1, gate("Cau", "KEPT")));
        assertEquals("X5_CLEAN_REGRESSION:1:1", g.check(1, gate("Wrong", "FIXED")));
    }
    @Test public void onlyPinnedDaAlternativeIsAccepted() throws Exception {
        JSONObject s = suite();
        s.getJSONArray("records").getJSONObject(0).put("optionalDa", new JSONObject().put("allowedAfterSha256",
                new JSONArray().put(PairText.sha256("Rina"))));
        EditorialX5MeasurementGate g = new EditorialX5MeasurementGate(s.toString());
        assertEquals("", g.check(1, gate("Rina", "FIXED")));
        assertTrue(g.check(1, gate("Ken", "FIXED")).startsWith("X5_CLEAN_REGRESSION"));
    }
    @Test public void oldWrongFixCannotPassAgain() throws Exception {
        JSONObject s = suite();
        s.getJSONArray("records").getJSONObject(0).put("group", "KNOWN").put("layer", "S4_WRONG_PASSED");
        EditorialX5MeasurementGate g = new EditorialX5MeasurementGate(s.toString());
        assertEquals("X5_OLD_WRONG_FIX_PASSED:1:1", g.check(1, gate("Wrong", "FIXED")));
        assertEquals("", g.check(1, gate("Cau", "REJECTED")));
    }
    @Test public void mappingAndMissingPointsStop() throws Exception {
        EditorialX5MeasurementGate g = new EditorialX5MeasurementGate(suite().toString());
        assertEquals("X5_POINT_MISSING:1:1", g.check(1, "{}"));
        assertEquals("X5_POINT_MAPPING_CHANGED", g.check(75, gate("Cau", "KEPT")));
        assertEquals("", g.check(75, "{}"));
    }
    @Test public void badInputCountOrChapterIsRefused() throws Exception {
        try {new EditorialX5MeasurementGate("{\"records\":[]}"); fail();} catch(IllegalArgumentException expected) { }
        JSONObject s = suite();s.getJSONArray("records").getJSONObject(0).put("chapter", "008");
        try {new EditorialX5MeasurementGate(s.toString()); fail();} catch(IllegalArgumentException expected) { }
    }
}
