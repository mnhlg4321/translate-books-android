package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public final class EditorialL3ReferenceNormalizationTest {
    @Test public void carriedAndProbeReferenceListsAreNormalizedAndCountedForReceipt() {
        String attempt = "l3-normalization-test";
        byte[] raw = "synthetic source line".getBytes(StandardCharsets.UTF_8);
        EditorialRawInventory.Inventory inventory = EditorialRawInventory.build(raw);
        Map<String, Object> carried = row("index", BigDecimal.ZERO, "status", "FIXED",
                "changeIds", list("C1", "C1"), "preserveIds", list(), "evidenceQuote", "", "reason", "");
        Map<String, Object> probe = row("probeId", "P1", "kind", "COVERAGE", "rawUnits", list("L1", "L1"),
                "viStart", BigDecimal.ONE, "viEnd", BigDecimal.ONE, "scope", "synthetic scope",
                "contrast", "synthetic contrast", "rawQuote", "source", "viQuote", "target",
                "verdict", "NO_DEFECT", "action", "NONE");
        Map<String, Object> disposition = row("disposition", "CONTINUE", "reasonCode", "OK", "stopClass", "NONE");
        Map<String, Object> wire = row("wireSchemaVersion", EditorialL3Ledger.RECONCILE_WIRE_V3,
                "attemptIdentity", attempt, "resolutions", list(), "carriedResolutions", list(carried),
                "changes", list(), "preserved", list(), "probes", list(probe), "disposition", disposition);

        EditorialL3Ledger.ReconcileWire parsed = EditorialL3Ledger.parseReconcile(
                canonical(wire), attempt, List.of(), 1, inventory, "synthetic target".getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of("C1"), parsed.carried().get(0).changeIds());
        assertEquals(List.of(inventory.units().get(0).id()), parsed.probes().get(0).rawUnits());
        assertEquals(2, parsed.duplicateReferencesRemoved());
        assertEquals(BigDecimal.valueOf(2), EditorialL3Ledger.normalizationEvidence(parsed).get("duplicateReferencesRemoved"));
    }

    private static Map<String, Object> row(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }

    private static List<Object> list(Object... values) { return new ArrayList<>(List.of(values)); }

    private static byte[] canonical(Map<String, Object> value) {
        return EditorialCanonicalJson.canonicalize(value).getBytes(StandardCharsets.UTF_8);
    }
}
