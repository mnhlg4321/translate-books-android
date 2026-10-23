package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SettingsRecommendationModelTest {
    @Test public void onlyChangedSafeRuntimeFieldsAreListed() {
        AppSettings s = new AppSettings();
        s.provider = "custom"; s.model = "private-model"; s.apiKey = "secret"; s.costLimitUsd = 0.42;
        s.maxOutputTokens = 1024; s.maxAttempts = 3; s.timeoutSeconds = 300; s.contextChars = 400; s.softLimitRatio = 0.8f;
        assertEquals(1, SettingsRecommendationModel.diffs(s).size());
        assertEquals("maxOutputTokens", SettingsRecommendationModel.diffs(s).get(0).key);
        assertFalse(SettingsRecommendationModel.diffs(s).get(0).current.contains("secret"));
    }

    @Test public void applyAndUndoCanUseAFullCopyWithoutTouchingSecretsOrMoney() {
        AppSettings before = new AppSettings();
        before.apiKey = "secret"; before.costLimitUsd = 0.2; before.maxOutputTokens = 100;
        AppSettings after = SettingsRecommendationModel.apply(before);
        assertEquals(8192, after.maxOutputTokens);
        assertEquals("secret", after.apiKey);
        assertEquals(0.2, after.costLimitUsd, 0);
        assertTrue(SettingsRecommendationModel.diffs(before).size() > 0);
        assertEquals(100, before.maxOutputTokens);
    }
}
