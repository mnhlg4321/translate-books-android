package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public final class EditorialP5PilotProviderContextTest {
    @Test public void providerRequestCarriesExactAppOwnedIdentityContext() {
        EditorialP5PilotProvider.Request.Context context =
                new EditorialP5PilotProvider.Request.Context(
                        "binding", "run", "manifest", "bundle", "predecessor",
                        List.of("anchor"), List.of("population"));
        EditorialP5PilotProvider.Request request = new EditorialP5PilotProvider.Request(
                "a".repeat(64), EditorialP5PilotProvider.CallKind.PRIMARY_SEMANTIC,
                "openrouter", "google/gemini-2.5-flash", "L1_RAW_DISCOVERY",
                "e".repeat(64), Map.of("RAW", "raw".getBytes()),
                new EditorialP5PilotRequest.PackAuthority(Map.of()),
                "safe4.full.report-l1.v1", "001", "", context);
        assertEquals("binding", request.context().bindingIdentity());
        assertEquals(List.of("anchor"), request.context().stableAnchors());
        assertEquals(List.of("population"), request.context().populationIds());
    }
}
