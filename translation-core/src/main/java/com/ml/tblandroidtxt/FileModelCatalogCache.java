package com.ml.tblandroidtxt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Model catalog cache in a plain file; the Android app keeps its own copy of this logic over files/. */
public final class FileModelCatalogCache implements ModelCatalog.CacheStore {
    private final File file;

    public FileModelCatalogCache(File file) {
        this.file = file;
    }

    @Override public String load() throws Exception {
        if (!file.exists()) return null;
        byte[] bytes = new byte[(int)Math.min(Integer.MAX_VALUE, file.length())];
        int count;
        try (FileInputStream input = new FileInputStream(file)) { count = input.read(bytes); }
        if (count <= 0) return null;
        return new String(bytes, 0, count, StandardCharsets.UTF_8);
    }

    @Override public void save(String rootJson) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(rootJson.getBytes(StandardCharsets.UTF_8));
        }
    }
}
