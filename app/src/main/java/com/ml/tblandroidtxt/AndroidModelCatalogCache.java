package com.ml.tblandroidtxt;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Model catalog cache kept in files/openrouter_model_catalog_v42.json. */
final class AndroidModelCatalogCache implements ModelCatalog.CacheStore {
    private static final String CACHE_FILE = "openrouter_model_catalog_v42.json";
    private final Context appContext;

    AndroidModelCatalogCache(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override public String load() throws Exception {
        File file = new File(appContext.getFilesDir(), CACHE_FILE);
        if (!file.exists()) return null;
        byte[] bytes = new byte[(int)Math.min(Integer.MAX_VALUE, file.length())];
        int count;
        try (FileInputStream input = new FileInputStream(file)) { count = input.read(bytes); }
        if (count <= 0) return null;
        return new String(bytes, 0, count, StandardCharsets.UTF_8);
    }

    @Override public void save(String rootJson) throws Exception {
        File file = new File(appContext.getFilesDir(), CACHE_FILE);
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(rootJson.getBytes(StandardCharsets.UTF_8));
        }
    }
}
