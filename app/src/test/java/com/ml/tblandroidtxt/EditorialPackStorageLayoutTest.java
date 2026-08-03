package com.ml.tblandroidtxt;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class EditorialPackStorageLayoutTest {
    @Test public void normalizesUnicodeButRejectsTraversalAndAlternateSeparators() throws Exception {
        Path root = Files.createTempDirectory("editorial-storage");
        EditorialPackStorageLayout layout = new EditorialPackStorageLayout(root);
        assertEquals("é.txt", EditorialPackStorageLayout.normalizeEntryPath("e\u0301.txt"));
        assertThrows(IllegalArgumentException.class, () -> EditorialPackStorageLayout.normalizeEntryPath("../prompt.txt"));
        assertThrows(IllegalArgumentException.class, () -> EditorialPackStorageLayout.normalizeEntryPath("..\\prompt.txt"));
        assertThrows(IllegalArgumentException.class, () -> EditorialPackStorageLayout.normalizeEntryPath("C:\\prompt.txt"));
        assertThrows(IllegalArgumentException.class, () -> EditorialPackStorageLayout.normalizeEntryPath("/prompt.txt"));
        assertThrows(IllegalArgumentException.class, () -> EditorialPackStorageLayout.normalizeEntryPath("prompt..txt"));
        assertTrue(layout.root().startsWith(root.toAbsolutePath().normalize()));
    }

    @Test public void contentAddressedDirectoriesRequireLowercaseSha256AndImportIds() throws Exception {
        Path root = Files.createTempDirectory("editorial-storage");
        EditorialPackStorageLayout layout = new EditorialPackStorageLayout(root);
        String hash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String id = "123e4567-e89b-12d3-a456-426614174000";
        layout.createRoots();
        assertEquals(layout.immutableRoot().resolve(hash), layout.immutableDirectory(hash));
        assertEquals(layout.stagingRoot().resolve(id), layout.stagingDirectory(id));
        assertThrows(IllegalArgumentException.class, () -> layout.immutableDirectory(hash.toUpperCase()));
        assertThrows(IllegalArgumentException.class, () -> layout.stagingDirectory("not-an-import"));
    }
}
