package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

/** v4.22 S0.8: the exported profile has no API key; importing a keyless file keeps the key saved on the device. */
public class ProfileExportKeyTest {
    private static final String KEY = "sk-or-v1-SYNTHETIC-KEY-1234567890";

    @Test public void exportedProfileHasNoApiKeyButKeepsTheOtherSettings() throws Exception {
        AppSettings s = new AppSettings();
        s.apiKey = KEY; s.model = "synthetic/model"; s.provider = "openrouter";
        String exported = SettingsStore.toExportJson(s);
        assertFalse(exported.contains(KEY));
        JSONObject o = new JSONObject(exported);
        assertFalse(o.has("apiKey"));
        JSONObject full = new JSONObject(SettingsStore.toJson(s));
        assertEquals(full.length() - 1, o.length());
        assertEquals("synthetic/model", o.getString("model"));
        assertTrue(SettingsStore.toJson(s).contains(KEY));
    }

    @Test public void keylessImportKeepsTheSavedKey() throws Exception {
        AppSettings current = new AppSettings();
        current.apiKey = KEY;
        AppSettings source = new AppSettings();
        source.model = "other/model";
        AppSettings imported = SettingsStore.fromImportedProfile(SettingsStore.toExportJson(source), current);
        assertEquals(KEY, imported.apiKey);
        assertEquals("other/model", imported.model);
    }

    @Test public void olderProfileWithAKeyStillImportsIt() throws Exception {
        AppSettings current = new AppSettings();
        current.apiKey = KEY;
        AppSettings source = new AppSettings();
        source.apiKey = "sk-from-file";
        AppSettings imported = SettingsStore.fromImportedProfile(SettingsStore.toJson(source), current);
        assertEquals("sk-from-file", imported.apiKey);
    }
}
