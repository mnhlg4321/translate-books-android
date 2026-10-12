package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable registry for validated test/runtime descriptors; it never changes project or pack state. */
public final class InMemoryEditorialPackRegistry implements EditorialPackRegistry {
    private final Map<String, EditorialPackManifest> byHash;
    private final Map<String, EditorialPackManifest> byIdentity;

    public InMemoryEditorialPackRegistry(Collection<EditorialPackManifest> manifests) {
        HashMap<String, EditorialPackManifest> hashes = new HashMap<>();
        HashMap<String, EditorialPackManifest> identities = new HashMap<>();
        if (manifests != null) for (EditorialPackManifest manifest : manifests) {
            if (manifest == null) throw new IllegalArgumentException("Registry cannot contain null manifest");
            EditorialPackManifest oldHash = hashes.putIfAbsent(manifest.canonicalPackHash(), manifest);
            if (oldHash != null && !oldHash.canonicalJson().equals(manifest.canonicalJson())) throw new IllegalArgumentException("Duplicate pack hash with different manifest");
            String identity = identity(manifest.packId(), manifest.version());
            EditorialPackManifest oldIdentity = identities.putIfAbsent(identity, manifest);
            if (oldIdentity != null && !oldIdentity.canonicalPackHash().equals(manifest.canonicalPackHash())) {
                throw new IllegalArgumentException("Same packId/version has multiple canonical hashes");
            }
        }
        this.byHash = Map.copyOf(hashes);
        this.byIdentity = Map.copyOf(identities);
    }

    @Override public Optional<EditorialPackManifest> findByHash(String canonicalPackHash) {
        return Optional.ofNullable(byHash.get(canonicalPackHash));
    }

    @Override public Optional<EditorialPackManifest> findByIdentity(String packId, String version) {
        return Optional.ofNullable(byIdentity.get(identity(packId, version)));
    }

    @Override public List<EditorialPackManifest> list() {
        ArrayList<EditorialPackManifest> result = new ArrayList<>(byHash.values());
        result.sort(Comparator.comparing(EditorialPackManifest::packId).thenComparing(EditorialPackManifest::version).thenComparing(EditorialPackManifest::canonicalPackHash));
        return List.copyOf(result);
    }

    private static String identity(String packId, String version) {
        if (packId == null || version == null) throw new IllegalArgumentException("Pack identity is required");
        return packId + "\u0000" + version;
    }
}
