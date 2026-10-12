package com.ml.tblandroidtxt.editorial.pack;

import java.util.List;
import java.util.Optional;

/** Read-only access to profiles that have already been trusted by the engine. */
public interface EditorialEngineContractProfileRegistry {
    List<EditorialEngineContractProfile> list();

    Optional<EditorialEngineContractProfile> findByCanonicalHash(String canonicalProfileHash);

    Optional<EditorialEngineContractProfile> findByIdentity(String engineProfileId, String engineProfileVersion);
}
