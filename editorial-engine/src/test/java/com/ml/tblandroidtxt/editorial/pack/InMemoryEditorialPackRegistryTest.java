package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class InMemoryEditorialPackRegistryTest {
    @Test public void registryIsReadOnlyAndSupportsMultipleVersions() {
        EditorialPackManifest first = EditorialPackFixtures.valid().manifest();
        EditorialPackManifest second = EditorialPackFixtures.create("com.example.editorial.safe4", "5.0.5", "new prompt\n", "DATA_COMPATIBLE").manifest();
        EditorialPackRegistry registry = new InMemoryEditorialPackRegistry(List.of(first, second));
        assertEquals(first, registry.findByIdentity(first.packId(), first.version()).orElseThrow());
        assertEquals(second, registry.findByIdentity(second.packId(), second.version()).orElseThrow());
        List<EditorialPackManifest> listed = registry.list();
        assertThrows(UnsupportedOperationException.class, () -> listed.add(first));
        assertTrue(registry.findByHash(first.canonicalPackHash()).isPresent());
    }

    @Test public void sameIdentityWithNewHashIsRejected() {
        EditorialPackManifest canonical = EditorialPackFixtures.valid().manifest();
        EditorialPackManifest changed = EditorialPackFixtures.create(canonical.packId(), canonical.version(), "DBE214 candidate prompt\n", "DATA_COMPATIBLE").manifest();
        assertThrows(IllegalArgumentException.class, () -> new InMemoryEditorialPackRegistry(List.of(canonical, changed)));
    }

    @Test public void registryDoesNotExposeMutableManifestDeclarations() {
        EditorialPackManifest manifest = EditorialPackFixtures.valid().manifest();
        EditorialPackManifest stored = new InMemoryEditorialPackRegistry(List.of(manifest)).list().get(0);
        assertThrows(UnsupportedOperationException.class, () -> stored.declarations().put("execution", "java"));
        assertEquals(manifest.canonicalPackHash(), stored.canonicalPackHash());
    }
}
