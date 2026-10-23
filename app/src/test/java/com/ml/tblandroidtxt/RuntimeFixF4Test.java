package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

/** F4: persisted runtime snapshots are secret-free while the local settings profile keeps its key. */
public class RuntimeFixF4Test {
    @Test public void persistedJsonOmitsApiKeyButRuntimeJsonKeepsIt() throws Exception {
        AppSettings settings = new AppSettings();
        settings.apiKey = "synthetic-fixture-key";

        JSONObject runtime = new JSONObject(SettingsStore.toJson(settings));
        JSONObject persisted = new JSONObject(SettingsStore.toPersistedJson(settings));

        assertEquals("synthetic-fixture-key", runtime.getString("apiKey"));
        assertFalse(persisted.has("apiKey"));
    }

    @Test public void persistedSecretRemovalIsIdempotentAndDoesNotChangeSettingsHashInput() throws Exception {
        String source = "{\"provider\":\"fixture\",\"apiKey\":\"synthetic-fixture-key\",\"model\":\"fixture-model\"}";
        String once = SettingsStore.stripPersistedSecrets(source);
        String twice = SettingsStore.stripPersistedSecrets(once);

        assertFalse(new JSONObject(once).has("apiKey"));
        assertEquals(once, twice);
        assertEquals("fixture-model", new JSONObject(once).getString("model"));
    }

    @Test public void missingSnapshotKeyIsRestoredOnlyFromCurrentSettings() {
        AppSettings current = new AppSettings();
        current.apiKey = "synthetic-current-key";
        AppSettings snapshot = SettingsStore.fromJson("{\"provider\":\"fixture\"}");

        AppSettings hydrated = SettingsStore.withCurrentSecrets(current, snapshot);

        assertEquals("synthetic-current-key", hydrated.apiKey);
        assertEquals("fixture", hydrated.provider);
        assertTrue(snapshot.apiKey == null || snapshot.apiKey.isEmpty());
    }
}
