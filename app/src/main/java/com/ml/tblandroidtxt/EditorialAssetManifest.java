package com.ml.tblandroidtxt;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Snapshot checks used before an editorial run starts; URIs are persisted later by the repository. */
public final class EditorialAssetManifest {
    public static class Asset {
        public final EditorialWorkflowV5.AssetRole role;
        public final String sha256;

        public Asset(EditorialWorkflowV5.AssetRole role, String sha256) {
            this.role = role;
            this.sha256 = sha256 == null ? "" : sha256;
        }
    }

    public static String validateUniqueRequired(List<Asset> assets, Set<EditorialWorkflowV5.AssetRole> required) {
        if (assets == null) return "Missing asset manifest";
        Set<EditorialWorkflowV5.AssetRole> seen = EnumSet.noneOf(EditorialWorkflowV5.AssetRole.class);
        for (Asset asset : assets) {
            if (asset == null || asset.role == null || asset.sha256.trim().isEmpty()) return "Asset role or checksum is missing";
            if (!seen.add(asset.role)) return "Ambiguous duplicate asset role: " + asset.role;
        }
        for (EditorialWorkflowV5.AssetRole role : required) if (!seen.contains(role)) return "Missing required asset: " + role;
        return null;
    }

    public static boolean unchanged(List<Asset> snapshot, List<Asset> current) {
        if (snapshot == null || current == null || snapshot.size() != current.size()) return false;
        HashSet<String> before = new HashSet<>();
        HashSet<String> after = new HashSet<>();
        for (Asset asset : snapshot) if (asset != null) before.add(asset.role + ":" + asset.sha256);
        for (Asset asset : current) if (asset != null) after.add(asset.role + ":" + asset.sha256);
        return before.equals(after);
    }

    private EditorialAssetManifest() {}
}
