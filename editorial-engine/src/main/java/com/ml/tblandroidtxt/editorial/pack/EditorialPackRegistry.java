package com.ml.tblandroidtxt.editorial.pack;

import java.util.List;
import java.util.Optional;

/** Read-only G2-A registry contract. It has no persistence or certification operation. */
public interface EditorialPackRegistry {
    Optional<EditorialPackManifest> findByHash(String canonicalPackHash);
    Optional<EditorialPackManifest> findByIdentity(String packId, String version);
    List<EditorialPackManifest> list();
}
