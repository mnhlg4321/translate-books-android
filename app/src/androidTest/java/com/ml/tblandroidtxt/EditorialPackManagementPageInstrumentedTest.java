package com.ml.tblandroidtxt;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EditorialPackManagementPageInstrumentedTest {
    @Test public void pageBuildsReadOnlySurfaceWithoutMutationActions() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View page = new EditorialPackManagementPageFactory(activity).build();
                assertNotNull(page);
                String text = allText(page);
                assertTrue(text.contains("READ ONLY"));
                assertTrue(!text.contains("Import") && !text.contains("Certify") && !text.contains("Activate")
                        && !text.contains("Delete") && !text.contains("Replace"));
            });
        }
    }

    @Test public void detailRebuildAndActivityRecreationDoNotExposeMutationControls() {
        try (ActivityScenario<MainActivity> first = ActivityScenario.launch(MainActivity.class)) {
            first.onActivity(activity -> assertTrue(allText(new EditorialPackManagementPageFactory(activity).build()).contains("persistent imported packs")));
        }
        try (ActivityScenario<MainActivity> second = ActivityScenario.launch(MainActivity.class)) {
            second.onActivity(activity -> assertTrue(allText(new EditorialPackManagementPageFactory(activity).build()).contains("READ ONLY")));
        }
    }

    private static String allText(View view) {
        StringBuilder out = new StringBuilder();
        if (view instanceof TextView) out.append(((TextView) view).getText()).append('\n');
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) out.append(allText(((ViewGroup) view).getChildAt(i)));
        return out.toString();
    }
}
