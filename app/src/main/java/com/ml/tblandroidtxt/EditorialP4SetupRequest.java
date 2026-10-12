package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind;

import java.util.List;

/** Explicit user-selected P4 setup request; it contains no execution command. */
public record EditorialP4SetupRequest(
        String attemptRequestSelector,
        String seriesName,
        String volumeName,
        String packId,
        String packVersion,
        String projectSemanticKey,
        String scopeKey,
        List<EditorialP4InputSource> sources,
        String sourceMode,
        String glossaryStatus,
        String pronounStatus,
        String pairContextStatus,
        String explicitUserDecisionProvenance,
        String runKind,
        String phaseIdentity,
        String frozenManifestReference,
        EditorialLineageNodeKind nodeKind,
        String parentRecordIdentity,
        long createdAt) {
    public EditorialP4SetupRequest {
        sources = List.copyOf(sources == null ? List.of() : sources);
    }
}
