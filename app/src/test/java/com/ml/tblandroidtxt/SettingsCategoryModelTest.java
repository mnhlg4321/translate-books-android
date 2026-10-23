package com.ml.tblandroidtxt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SettingsCategoryModelTest {
    @Test public void fiveCategoriesAndVietnameseLabelsAreStable() {
        assertArrayEquals(new String[]{"General", "Provider", "Biên tập", "Appearance", "Advanced"}, SettingsCategoryModel.ids());
        assertEquals("Dịch", SettingsCategoryModel.label("General"));
        assertEquals("Provider & chi phí", SettingsCategoryModel.label("Provider"));
        assertEquals("Nâng cao & thông tin", SettingsCategoryModel.label("About"));
    }

    @Test public void oldIdsMapWithoutChangingEditorialOrProvider() {
        assertEquals("General", SettingsCategoryModel.normalize("Translation"));
        assertEquals("General", SettingsCategoryModel.normalize("Prompt"));
        assertEquals("Advanced", SettingsCategoryModel.normalize("Performance"));
        assertEquals("Advanced", SettingsCategoryModel.normalize("Logging"));
        assertEquals("Biên tập", SettingsCategoryModel.normalize("Biên tập"));
        assertEquals("General", SettingsCategoryModel.normalize("unknown"));
    }
}
