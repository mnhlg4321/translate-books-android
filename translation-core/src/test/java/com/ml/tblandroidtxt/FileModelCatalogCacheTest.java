package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class FileModelCatalogCacheTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void missingFileLoadsAsNull() throws Exception {
        assertNull(new FileModelCatalogCache(new File(tmp.getRoot(), "none.json")).load());
    }

    @Test public void saveThenLoadRoundTripsUtf8() throws Exception {
        FileModelCatalogCache cache = new FileModelCatalogCache(new File(tmp.getRoot(), "catalog.json"));
        String text = "{\"retrieved_at\":1,\"response\":\"tiếng Việt 日本語\"}";
        cache.save(text);
        assertEquals(text, cache.load());
        cache.save("{}");
        assertEquals("{}", cache.load());
    }
}
