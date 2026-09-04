package com.ml.tblandroidtxt;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

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
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> assertTrue(allText(new EditorialPackManagementPageFactory(activity).build()).contains("persistent imported packs")));
            scenario.recreate();
            scenario.onActivity(activity -> assertTrue(allText(new EditorialPackManagementPageFactory(activity).build()).contains("READ ONLY")));
        }
    }

    @Test public void openingAndRefreshingPageDoesNotChangePackRowCount() {
        TranslationRepository beforeRepo = new TranslationRepository(ApplicationProvider.getApplicationContext());
        int before = countPacks(beforeRepo);
        beforeRepo.close();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                new EditorialPackManagementPageFactory(activity).build();
                new EditorialPackManagementPageFactory(activity).build();
            });
        }
        TranslationRepository afterRepo = new TranslationRepository(ApplicationProvider.getApplicationContext());
        assertEquals(before, countPacks(afterRepo));
        afterRepo.close();
    }

    private static int countPacks(TranslationRepository repository) {
        try (android.database.Cursor cursor = repository.editorialReadableDatabase().rawQuery("SELECT COUNT(*) FROM editorial_packs", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        }
    }

    private static String allText(View view) {
        StringBuilder out = new StringBuilder();
        if (view instanceof TextView) out.append(((TextView) view).getText()).append('\n');
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) out.append(allText(((ViewGroup) view).getChildAt(i)));
        return out.toString();
    }
}
